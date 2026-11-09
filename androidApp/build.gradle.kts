plugins {
    alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}
val payUSandboxUrl = providers.gradleProperty("payuSandboxBaseUrl").orElse("").get()
require(payUSandboxUrl.isEmpty() || payUSandboxUrl.startsWith("https://") || payUSandboxUrl.startsWith("http://10.0.2.2:") || payUSandboxUrl.startsWith("http://localhost:"))
// Live delivery is opt-in and requires explicit publisher/audience configuration.
val liveAds = providers.gradleProperty("parcelLiveAds").orElse("false").get().toBoolean()
val adsAudience = providers.gradleProperty("parcelAdsAudience").orElse("adults").get()
val adsAppId = providers.gradleProperty("admobAppId").orElse("ca-app-pub-3940256099942544~3347511713").get()
val homeAdId = providers.gradleProperty("admobHomeNativeId").orElse("").get()
val categoryAdId = providers.gradleProperty("admobCategoryNativeId").orElse("").get()
val extraAdIds = listOf("WishlistNative", "PurchasedNative", "CartInterstitial", "ProfileInterstitial", "ShelfRewarded")
    .associateWith { providers.gradleProperty("admob${it}Id").orElse("").get() }
if (liveAds) {
    require(adsAudience in listOf("adults", "children")) { "Confirm the intended audience before enabling live ads. Mixed audiences need an age-screen implementation first." }
    require(Regex("ca-app-pub-[0-9]{16}~[0-9]{10}").matches(adsAppId) && !adsAppId.contains("3940256099942544")) { "Set your real admobAppId before enabling live ads." }
    (listOf(homeAdId, categoryAdId) + extraAdIds.values).forEach { require(Regex("ca-app-pub-[0-9]{16}/[0-9]{10}").matches(it) && !it.contains("3940256099942544")) }
}
android {
    namespace = "com.shubhamshakya.parcel.android"; compileSdk = 36
    defaultConfig {
        applicationId = "com.shubhamshakya.parcel"; minSdk = 23; targetSdk = 36
        versionCode = 7; versionName = "0.6.0"
        buildConfigField("String", "PAYU_SANDBOX_BASE_URL", "\"${payUSandboxUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        manifestPlaceholders["admobAppId"] = if (liveAds) adsAppId else "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("boolean", "LIVE_ADS", liveAds.toString())
        buildConfigField("boolean", "CHILD_DIRECTED_ADS", (adsAudience == "children").toString())
        buildConfigField("String", "HOME_NATIVE_AD_ID", "\"${if (liveAds) homeAdId else ""}\"")
        buildConfigField("String", "CATEGORY_NATIVE_AD_ID", "\"${if (liveAds) categoryAdId else ""}\"")
        extraAdIds.forEach { (placement, id) ->
            val field = placement.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase() + "_AD_ID"
            buildConfigField("String", field, "\"${if (liveAds) id else ""}\"")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    buildTypes { release { isMinifyEnabled = false } }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":shared:domain")); implementation(project(":shared:data"))
    implementation(project(":shared:presentation"))
    // 25.4 retains Parcel's Android API 23 support (25.5 raises it to 24).
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation(libs.material3)
    implementation(libs.activity.compose); implementation(libs.koin.android)
    implementation(libs.splashscreen)
    implementation(libs.payu.checkout)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.serialization.json)
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.test.manifest)
}
