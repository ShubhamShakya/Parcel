plugins {
    alias(libs.plugins.kotlin.multiplatform); alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform); alias(libs.plugins.compose.compiler)
}
kotlin {
    androidTarget(); jvm(); jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(compose.runtime); implementation(compose.foundation)
            implementation(compose.ui); implementation(compose.components.resources)
            implementation(libs.material3); implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel); implementation(libs.lifecycle.runtime)
            implementation(libs.koin.compose); implementation(libs.coil.compose)
        }
        commonTest.dependencies { implementation(kotlin("test")); implementation(libs.coroutines.test) }
    }
}
android { namespace = "com.parcel.presentation"; compileSdk = 36; defaultConfig { minSdk = 23 } }
compose.resources { packageOfResClass = "com.parcel.resources" }
