package com.parcel.android

import com.parcel.data.ParcelDatabase
import com.parcel.data.RoomAuthRepository
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/** Keep shopping tests focused on their workflows; AuthFlowTest exercises the login UI. */
suspend fun signInTestShopper() {
    val auth = RoomAuthRepository(GlobalContext.get().get<ParcelDatabase>())
    auth.signOut(); auth.requestOtp("IN", "9000000000"); auth.verifyOtp("1234")
    if (auth.observe().first().profile == null) auth.register("Test", "Shopper")
}
