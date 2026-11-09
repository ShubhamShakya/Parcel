package com.parcel.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AdPlacement { Home, Category, Wishlist, Purchased }
enum class InterstitialPlacement { Cart, Profile }
interface ParcelAds {
    @Composable fun NativeCard(placement: AdPlacement)
    @Composable fun PrivacyOptions()
    fun enterTab(placement: InterstitialPlacement?, navigate: () -> Unit) = navigate()
    /** Returns cancellation for a pending load. Rewards already earned remain durable. */
    fun watchShelfReward(slot: Int, finished: (String?) -> Unit): () -> Unit {
        finished("Rewarded ads are unavailable on this platform.")
        return {}
    }
}
private object NoAds : ParcelAds {
    @Composable override fun NativeCard(placement: AdPlacement) = Unit
    @Composable override fun PrivacyOptions() = Unit
}
val LocalParcelAds = staticCompositionLocalOf<ParcelAds> { NoAds }

/** No launch ads, at least two tab actions, and a two-minute gap between full-screen ads. */
class InterstitialGate(private val startedAt: Long) {
    private var lastShown: Long? = null
    private var actions = 0
    fun action() { actions++ }
    fun eligible(now: Long): Boolean = actions >= 2 && now - startedAt >= 60_000 &&
        (lastShown == null || now - lastShown!! >= 120_000)
    fun shown(now: Long) { lastShown = now; actions = 0 }
}
