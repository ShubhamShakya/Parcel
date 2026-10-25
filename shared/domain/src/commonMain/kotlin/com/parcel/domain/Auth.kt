package com.parcel.domain

import kotlinx.coroutines.flow.Flow

const val OTP_RESEND_DELAY_MS = 30_000L
data class DialCountry(val iso: String, val name: String, val dialCode: String)
val dialCountries = listOf(
    DialCountry("IN", "India", "+91"), DialCountry("US", "United States", "+1"), DialCountry("CA", "Canada", "+1"),
    DialCountry("GB", "United Kingdom", "+44"), DialCountry("AU", "Australia", "+61"), DialCountry("NZ", "New Zealand", "+64"),
    DialCountry("AE", "United Arab Emirates", "+971"), DialCountry("SA", "Saudi Arabia", "+966"),
    DialCountry("SG", "Singapore", "+65"), DialCountry("MY", "Malaysia", "+60"), DialCountry("NP", "Nepal", "+977"),
    DialCountry("BD", "Bangladesh", "+880"), DialCountry("PK", "Pakistan", "+92"), DialCountry("LK", "Sri Lanka", "+94"),
    DialCountry("DE", "Germany", "+49"), DialCountry("FR", "France", "+33"), DialCountry("IT", "Italy", "+39"),
    DialCountry("ES", "Spain", "+34"), DialCountry("ZA", "South Africa", "+27"), DialCountry("JP", "Japan", "+81"),
    DialCountry("KR", "South Korea", "+82"), DialCountry("CN", "China", "+86"), DialCountry("ID", "Indonesia", "+62"),
    DialCountry("PH", "Philippines", "+63"), DialCountry("BR", "Brazil", "+55"), DialCountry("MX", "Mexico", "+52"),
    DialCountry("TR", "Türkiye", "+90"), DialCountry("NG", "Nigeria", "+234"), DialCountry("KE", "Kenya", "+254"), DialCountry("EG", "Egypt", "+20"),
)
fun phoneError(number: String): String? = when {
    number.any { it !in '0'..'9' } -> "Use digits only, without the country code."
    number.length !in 9..16 -> "Your phone number needs 9–16 digits."
    else -> null
}
fun canonicalPhone(countryIso: String, number: String): String {
    require(phoneError(number) == null) { phoneError(number).orEmpty() }
    return dialCountries.first { it.iso == countryIso }.dialCode + number
}
fun otpError(code: String): String? = if (code.length == 4 && code.none { it.isWhitespace() }) null else "Enter exactly 4 characters."
fun nameError(first: String, last: String): String? = when {
    first.trim().isEmpty() || last.trim().isEmpty() -> "Enter your first and last name."
    first.trim().length > 50 || last.trim().length > 50 -> "Keep each name to 50 characters or fewer."
    else -> null
}
data class ShopperProfile(val phone: String, val firstName: String, val lastName: String)
data class OtpChallenge(val phone: String, val countryIso: String, val number: String, val resendAt: Long, val verified: Boolean = false) {
    fun remainingSeconds(now: Long): Int = ((resendAt - now).coerceIn(0, OTP_RESEND_DELAY_MS) + 999L).div(1000).toInt()
}
data class AuthSnapshot(val profile: ShopperProfile? = null, val challenge: OtpChallenge? = null)
/** Local simulator authentication. No SMS is sent; any four non-whitespace characters verify. */
interface AuthRepository {
    fun observe(): Flow<AuthSnapshot>
    suspend fun requestOtp(countryIso: String, number: String)
    suspend fun resendOtp()
    suspend fun verifyOtp(code: String)
    suspend fun register(firstName: String, lastName: String)
    suspend fun changePhone()
    suspend fun signOut()
}
