plugins { alias(libs.plugins.kotlin.serialization); alias(libs.plugins.kotlin.multiplatform); alias(libs.plugins.android.library) }
kotlin {
    androidTarget(); jvm(); jvmToolchain(17)
    sourceSets {
        commonMain.dependencies { api(libs.coroutines.core); api(libs.serialization.json) }
        commonTest.dependencies { implementation(kotlin("test")); implementation(libs.coroutines.test) }
    }
}
android { namespace = "com.parcel.domain"; compileSdk = 36; defaultConfig { minSdk = 23 } }
