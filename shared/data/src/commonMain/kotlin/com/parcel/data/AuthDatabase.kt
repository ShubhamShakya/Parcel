package com.parcel.data

import androidx.room.*
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.parcel.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Clock

@Entity(tableName = "shopper_profiles")
data class ShopperEntity(@PrimaryKey val phone: String, val firstName: String, val lastName: String)
@Entity(tableName = "auth_session")
data class AuthSessionEntity(@PrimaryKey val id: Int = 1, val phone: String)
@Entity(tableName = "otp_challenge")
data class OtpEntity(@PrimaryKey val id: Int = 1, val phone: String, val countryIso: String, val number: String,
    val resendAt: Long, val verified: Boolean = false)
@Dao
interface AuthDao {
    @Query("SELECT p.* FROM shopper_profiles p INNER JOIN auth_session s ON s.phone = p.phone WHERE s.id = 1") fun observeProfile(): Flow<ShopperEntity?>
    @Query("SELECT * FROM otp_challenge WHERE id = 1") fun observeChallenge(): Flow<OtpEntity?>
    @Query("SELECT * FROM shopper_profiles WHERE phone = :phone") suspend fun profile(phone: String): ShopperEntity?
    @Query("SELECT * FROM otp_challenge WHERE id = 1") suspend fun challenge(): OtpEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProfile(profile: ShopperEntity)
    @Upsert suspend fun putSession(session: AuthSessionEntity)
    @Upsert suspend fun putChallenge(challenge: OtpEntity)
    @Query("DELETE FROM otp_challenge") suspend fun clearChallenge()
    @Query("DELETE FROM auth_session") suspend fun clearSession()
}
class RoomAuthRepository(private val db: ParcelDatabase, private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : AuthRepository {
    private val dao = db.authDao()
    override fun observe() = combine(dao.observeProfile(), dao.observeChallenge()) { profile, challenge ->
        AuthSnapshot(profile?.let { ShopperProfile(it.phone, it.firstName, it.lastName) },
            challenge?.let { OtpChallenge(it.phone, it.countryIso, it.number, it.resendAt, it.verified) })
    }
    private suspend fun <T> transaction(block: suspend () -> T): T = db.useWriterConnection { it.immediateTransaction { block() } }
    override suspend fun requestOtp(countryIso: String, number: String) = transaction {
        val phone = canonicalPhone(countryIso, number)
        val previous = dao.challenge()
        // Changing screens or requesting the same number again cannot bypass the countdown.
        if (previous?.phone != phone || previous.verified || now() >= previous.resendAt) {
            dao.putChallenge(OtpEntity(phone = phone, countryIso = countryIso, number = number, resendAt = now() + OTP_RESEND_DELAY_MS))
        }
    }
    override suspend fun resendOtp() = transaction {
        val challenge = dao.challenge() ?: error("Enter your phone number first.")
        check(!challenge.verified) { "This number is already verified." }
        check(now() >= challenge.resendAt) { "Please wait before resending your OTP." }
        dao.putChallenge(challenge.copy(resendAt = now() + OTP_RESEND_DELAY_MS))
    }
    override suspend fun verifyOtp(code: String) = transaction {
        require(otpError(code) == null) { otpError(code).orEmpty() }
        val challenge = dao.challenge() ?: error("Enter your phone number first.")
        // Deliberately permissive only in this local simulator. This is not SMS authentication.
        if (dao.profile(challenge.phone) != null) {
            dao.putSession(AuthSessionEntity(phone = challenge.phone)); dao.clearChallenge()
        } else dao.putChallenge(challenge.copy(verified = true))
    }
    override suspend fun register(firstName: String, lastName: String) = transaction {
        require(nameError(firstName, lastName) == null) { nameError(firstName, lastName).orEmpty() }
        val challenge = dao.challenge() ?: error("Verify your phone number first.")
        check(challenge.verified) { "Verify your phone number first." }
        if (dao.profile(challenge.phone) == null) dao.insertProfile(ShopperEntity(challenge.phone, firstName.trim(), lastName.trim()))
        dao.putSession(AuthSessionEntity(phone = challenge.phone)); dao.clearChallenge()
    }
    override suspend fun changePhone() = transaction { dao.clearChallenge() }
    override suspend fun signOut() = transaction { dao.clearChallenge(); dao.clearSession() }
}
