package com.parcel.android

import android.app.Application
import com.parcel.data.*
import com.parcel.domain.SandboxPaymentGateway
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class ParcelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ParcelApplication)
            modules(shoppingDataModule, module {
                single { PayUSandboxGateway(BuildConfig.PAYU_SANDBOX_BASE_URL) }
                single<SandboxPaymentGateway> { get<PayUSandboxGateway>() }
                single<ParcelDatabase> { createAndroidDatabase(this@ParcelApplication) }
            })
        }
    }
}
