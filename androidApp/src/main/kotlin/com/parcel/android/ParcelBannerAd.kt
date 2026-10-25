package com.parcel.android

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.*
import com.parcel.presentation.BannerPlacement

/** Inline placement: width follows its content area; the SDK controls banner rendering/clicks. */
@Composable internal fun ParcelBannerAd(activity: Activity, placement: BannerPlacement, unitId: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.value.toInt()
        if (width <= 0) return@BoxWithConstraints
        key(unitId, width) {
            var loaded by remember { mutableStateOf(false) }
            val view = remember {
                AdView(activity).apply {
                    adUnitId = unitId
                    setAdSize(AdSize.getInlineAdaptiveBannerAdSize(width, 100))
                }
            }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DisposableEffect(view, lifecycle) {
                var active = true
                view.adListener = object : AdListener() {
                    override fun onAdLoaded() { if (active) loaded = true }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (active) loaded = false
                        android.util.Log.d("ParcelAds", "${placement.name}: banner unavailable (${error.code})")
                    }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> view.resume()
                        Lifecycle.Event.ON_PAUSE -> view.pause()
                        else -> Unit
                    }
                }
                lifecycle.addObserver(observer)
                view.loadAd(AdRequest.Builder().build())
                onDispose {
                    active = false
                    lifecycle.removeObserver(observer)
                    view.adListener = object : AdListener() {}
                    view.destroy()
                }
            }
            if (loaded) Column(Modifier.fillMaxWidth()) {
                Text("Ad · External advertiser", style = MaterialTheme.typography.labelSmall)
                AndroidView(factory = { view }, modifier = Modifier.fillMaxWidth()
                    .wrapContentHeight().testTag("banner_ad_${placement.name}"))
            }
        }
    }
}
