package com.parcel.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** SDK objects remain in the platform implementation. Other targets default to no ads. */
enum class AdPlacement { Home, Category }
enum class BannerPlacement { Wishlist, Cart, Purchased }
interface ParcelAds {
    @Composable fun Banner(placement: BannerPlacement)
    @Composable fun NativeCard(placement: AdPlacement)
    @Composable fun PrivacyOptions()
}
private object NoAds : ParcelAds {
    @Composable override fun Banner(placement: BannerPlacement) = Unit
    @Composable override fun NativeCard(placement: AdPlacement) = Unit
    @Composable override fun PrivacyOptions() = Unit
}
val LocalParcelAds = staticCompositionLocalOf<ParcelAds> { NoAds }
