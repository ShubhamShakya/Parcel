package com.parcel.android

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.*
import com.google.android.ump.*
import com.parcel.presentation.*

/** Activity-scoped consent UI; ad views and SDK objects never enter shared state. */
class AndroidParcelAds(private val activity: Activity) : ParcelAds {
    private companion object {
        var initializationStarted = false
        var sdkReady by mutableStateOf(false)
    }
    private val testMode = BuildConfig.DEBUG
    private val enabled = testMode || BuildConfig.LIVE_ADS
    private var ready by mutableStateOf(false)
    private var privacyRequired by mutableStateOf(false)
    private var privacyError by mutableStateOf<String?>(null)
    private var disposed = false
    private val consent by lazy { UserMessagingPlatform.getConsentInformation(activity) }

    fun start() {
        if (!enabled) return
        if (testMode) { initialize(); return }
        val parameters = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(BuildConfig.CHILD_DIRECTED_ADS).build()
        consent.requestConsentInfoUpdate(activity, parameters, {
            if (!disposed) UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { updateConsent() }
        }, { updateConsent() })
    }
    private fun updateConsent() {
        if (disposed) return
        privacyRequired = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        ready = false
        if (consent.canRequestAds()) initialize()
    }
    @Suppress("DEPRECATION")
    private fun initialize() {
        if (disposed) return
        MobileAds.setRequestConfiguration(RequestConfiguration.Builder()
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .setTagForChildDirectedTreatment(if (testMode || BuildConfig.CHILD_DIRECTED_ADS)
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
            .build())
        ready = true
        if (!initializationStarted) {
            initializationStarted = true
            MobileAds.initialize(activity.applicationContext) {
                android.os.Handler(android.os.Looper.getMainLooper()).post { sdkReady = true }
            }
        }
    }
    fun dispose() { disposed = true; ready = false }

    @Composable override fun PrivacyOptions() {
        if (privacyRequired) OutlinedButton(onClick = {
            ready = false
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (!disposed) {
                    if (error != null) privacyError = "Privacy options could not be opened. Please try again."
                    updateConsent()
                }
            }
        }, modifier = Modifier.fillMaxWidth().testTag("ad_privacy_options")) { Text("Ad privacy choices") }
        privacyError?.let { message ->
            ParcelDialog(onDismissRequest = { privacyError = null }, title = { Text("Ad privacy") },
                text = { Text(message) }, confirmButton = { TextButton(onClick = { privacyError = null }) { Text("OK") } })
        }
    }

    @Composable override fun Banner(placement: BannerPlacement) {
        if (!ready || !sdkReady) return
        val unitId = if (testMode) "ca-app-pub-3940256099942544/9214589741" else when (placement) {
            BannerPlacement.Wishlist -> BuildConfig.WISHLIST_BANNER_AD_ID
            BannerPlacement.Cart -> BuildConfig.CART_BANNER_AD_ID
            BannerPlacement.Purchased -> BuildConfig.PURCHASED_BANNER_AD_ID
        }
        ParcelBannerAd(activity, placement, unitId)
    }

    @Composable override fun NativeCard(placement: AdPlacement) {
        if (!ready || !sdkReady) return
        val unitId = if (testMode) "ca-app-pub-3940256099942544/2247696110" else when (placement) {
            AdPlacement.Home -> BuildConfig.HOME_NATIVE_AD_ID
            AdPlacement.Category -> BuildConfig.CATEGORY_NATIVE_AD_ID
        }
        var ad by remember(unitId) { mutableStateOf<NativeAd?>(null) }
        DisposableEffect(unitId) {
            var active = true
            var owned: NativeAd? = null
            AdLoader.Builder(activity, unitId)
                .forNativeAd { loaded ->
                    if (!active || disposed) loaded.destroy() else {
                        owned?.destroy(); owned = loaded; ad = loaded
                    }
                }
                .withNativeAdOptions(NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        // No retries or placeholder: browsing remains usable offline/no-fill.
                        android.util.Log.d("ParcelAds", "${placement.name}: load unavailable (${error.code})")
                    }
                }).build().loadAd(AdRequest.Builder().build())
            onDispose { active = false; owned?.destroy(); owned = null }
        }
        ad?.let { loaded ->
            val background = MaterialTheme.colorScheme.surface.toArgb()
            val foreground = MaterialTheme.colorScheme.onSurface.toArgb()
            key(loaded, background, foreground) {
                AndroidView(modifier = Modifier.fillMaxWidth().testTag("native_ad_${placement.name}"),
                    factory = { context ->
                        val root = NativeAdView(context)
                        fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
                        val content = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(16), dp(16), dp(16), dp(16))
                            setBackgroundColor(background)
                        }
                        fun label(value: String, size: Float) = TextView(context).apply {
                            text = value; textSize = size; setTextColor(foreground)
                        }
                        content.addView(label("Ad · External advertiser", 12f).apply { setPadding(0, 0, dp(32), dp(8)) })
                        val media = MediaView(context).apply { mediaContent = loaded.mediaContent }
                        content.addView(media, LinearLayout.LayoutParams(-1, dp(180)))
                        root.mediaView = media
                        val icon = ImageView(context).apply {
                            setImageDrawable(loaded.icon?.drawable)
                            visibility = if (loaded.icon == null) View.GONE else View.VISIBLE
                            contentDescription = "Advertiser icon"
                        }
                        content.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                        root.iconView = icon
                        val headline = label(loaded.headline.orEmpty(), 18f)
                        content.addView(headline); root.headlineView = headline
                        val body = label(loaded.body.orEmpty(), 14f).apply { visibility = if (loaded.body == null) View.GONE else View.VISIBLE }
                        content.addView(body); root.bodyView = body
                        val advertiser = label(loaded.advertiser.orEmpty(), 12f).apply { visibility = if (loaded.advertiser == null) View.GONE else View.VISIBLE }
                        content.addView(advertiser); root.advertiserView = advertiser
                        val action = Button(context).apply {
                            text = loaded.callToAction.orEmpty()
                            visibility = if (loaded.callToAction == null) View.GONE else View.VISIBLE
                            // Clicks and impressions are registered by the SDK, never manually.
                        }
                        content.addView(action); root.callToActionView = action
                        root.addView(content)
                        root.setNativeAd(loaded)
                        root
                    }, onRelease = { it.destroy() })
            }
        }
    }
}
