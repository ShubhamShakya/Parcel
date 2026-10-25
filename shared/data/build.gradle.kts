plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.multiplatform); alias(libs.plugins.android.library)
    alias(libs.plugins.ksp); alias(libs.plugins.room)
}
kotlin {
    androidTarget(); jvm(); jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
            implementation(libs.room.runtime); implementation(libs.sqlite.bundled)
            implementation(libs.koin.core); implementation(libs.ktor.client.core)
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        commonTest.dependencies { implementation(kotlin("test")); implementation(libs.coroutines.test) }
    }
}
android { namespace = "com.parcel.data"; compileSdk = 36; defaultConfig { minSdk = 23 } }
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
}
room { schemaDirectory("$projectDir/schemas") }
