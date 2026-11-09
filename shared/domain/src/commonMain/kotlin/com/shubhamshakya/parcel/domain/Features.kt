package com.shubhamshakya.parcel.domain

/**
 * Build-wide switches for integrations that are prepared but not yet released.
 * Both are off: neither has been configured with real credentials or verified end to end.
 */
object ParcelFeatures {
    /** PayU CheckoutPro sandbox. Also needs a configured backend URL; see docs/PAYU_SANDBOX.md. */
    const val PAYU_ENABLED = false
    /** Google AdMob native and banner slots. Also needs release ad units; see docs/ADMOB.md. */
    const val GOOGLE_ADS_ENABLED = false
}
