package com.parcel.android

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.*
import com.google.android.gms.ads.rewarded.*
import com.parcel.domain.DeliveryRepository
import com.parcel.presentation.InterstitialGate
import com.parcel.presentation.InterstitialPlacement
import kotlinx.coroutines.*

internal class FullScreenAds(
    private val activity: Activity,
    private val delivery: DeliveryRepository,
    private val allowed: () -> Boolean,
) {
    private companion object {
        val gate = InterstitialGate(SystemClock.elapsedRealtime())
        // Earned entitlements must finish saving even if the Activity is recreated.
        val rewardScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    private data class Cached(val ad: InterstitialAd, val loadedAt: Long)
    private val cache = mutableMapOf<InterstitialPlacement, Cached>()
    private val loading = mutableSetOf<InterstitialPlacement>()
    private var disposed = false
    private var showing = false
    private var generation = 0
    fun invalidate() { generation++; cache.clear(); loading.clear() }
    private fun foreground() = !disposed && !activity.isFinishing && !activity.isDestroyed &&
        (activity as LifecycleOwner).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    fun dispose() { disposed = true; cache.clear() }
    fun preload() {
        if (!allowed() || disposed) return
        InterstitialPlacement.entries.forEach { placement ->
            if (cache[placement]?.let { SystemClock.elapsedRealtime() - it.loadedAt >= 3_600_000 } == true) cache.remove(placement)
            if (placement !in cache && loading.add(placement)) {
                val id = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/1033173712" else when (placement) {
                    InterstitialPlacement.Cart -> BuildConfig.CART_INTERSTITIAL_AD_ID
                    InterstitialPlacement.Profile -> BuildConfig.PROFILE_INTERSTITIAL_AD_ID
                }
                val requestGeneration = generation
                InterstitialAd.load(activity, id, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (requestGeneration != generation) return
                        loading.remove(placement)
                        if (!disposed && allowed()) cache[placement] = Cached(ad, SystemClock.elapsedRealtime())
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) { if (requestGeneration == generation) loading.remove(placement) }
                })
            }
        }
    }
    fun enter(placement: InterstitialPlacement?, navigate: () -> Unit) {
        if (showing) return
        gate.action()
        val now = SystemClock.elapsedRealtime()
        val cached = placement?.let { cache[it] }
        if (!allowed() || !foreground() || !gate.eligible(now) || cached == null || now - cached.loadedAt >= 3_600_000) {
            navigate(); preload(); return
        }
        cache.remove(placement)
        showing = true
        // Commit the destination in this tap before showing the cached ad, so recreation retains it.
        navigate()
        var continued = false
        fun finish() {
            if (continued) return
            continued = true; showing = false
            if (!disposed) preload()
        }
        cached.ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { gate.shown(SystemClock.elapsedRealtime()) }
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
        }
        try { cached.ad.show(activity) } catch (_: Exception) { finish() }
    }
    fun reward(slot: Int, finished: (String?) -> Unit): () -> Unit {
        if (!allowed() || !foreground() || showing) {
            finished("An ad isn’t available right now. Please try again later."); return {}
        }
        showing = true
        var active = true
        var earned = false
        var completed = false
        var displayed = false
        fun finish(message: String?) {
            if (completed) return
            completed = true; showing = false
            if (active && !disposed) finished(message)
        }
        val id = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/5224354917" else BuildConfig.SHELF_REWARDED_AD_ID
        RewardedAd.load(activity, id, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(error: LoadAdError) = finish("No ad is available right now. Your six free spaces are still available.")
            override fun onAdLoaded(ad: RewardedAd) {
                if (!active || !foreground() || !allowed()) { finish("Ad cancelled. No space was unlocked."); return }
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        displayed = true
                        gate.shown(SystemClock.elapsedRealtime())
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) = finish("The ad could not open. Please try again.")
                    override fun onAdDismissedFullScreenContent() {
                        if (!earned) finish("Ad closed before the reward was earned. This space is still locked.")
                    }
                }
                try {
                    ad.show(activity) {
                        if (!earned) {
                            earned = true
                            rewardScope.launch {
                                val error = try { delivery.unlockSlot(slot); null }
                                    catch (_: Exception) { "Your reward could not be saved. Please try again." }
                                withContext(Dispatchers.Main) { finish(error) }
                            }
                        }
                    }
                } catch (_: Exception) { finish("The ad could not open. Please try again.") }
            }
        })
        return { active = false; if (!displayed) { completed = true; showing = false } }
    }
}
