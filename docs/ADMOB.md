# Parcel AdMob integration

## Placements and scope

- Home: one native card immediately after featured offers, before the main product grid.
- Category/clothing lists: one full-width native card after eight products, when there are at least nine results and no search query.
- Wishlist, Cart and Purchased: one inline adaptive banner below the screen heading, separated from item/shelf controls. The Cart banner scrolls with content, away from the fixed Checkout button.
- Search results, checkout, authentication, tracking and unboxing remain ad-free. Optional product-detail, rewarded and interstitial formats were not part of the recommended initial rollout.
- Each card labels external advertising, renders SDK assets inside NativeAdView and retains AdChoices. SDK handles impressions and clicks. No reward or real-shopping offer is implied.
- Ad requests run when a placement enters composition, not on recomposition. SDK initialization is process-scoped. Leaving the placement disposes the ad; a late load callback destroys its result. No-fill/network failures hide the card and do not block browsing. There is no automatic refresh timer.
- Shared presentation exposes ParcelAds via a composition local. Android owns SDK objects; unsupported platforms default to no ads. No ad SDK enters domain/data modules.

Banners use content width with a maximum requested height of 100dp. They pause/resume with the activity and are destroyed on leaving their placement. Load failures hide them; any SDK refresh follows AdMob settings. Native Home/category placements are unchanged.

## Test ads

Normal debug builds always use Google's native test unit `ca-app-pub-3940256099942544/2247696110` and sample application ID `ca-app-pub-3940256099942544~3347511713` (unless an explicit live configuration supplies the manifest app ID; the debug ad unit still remains the Google test unit).

The three banners use Google’s demo unit `ca-app-pub-3940256099942544/9214589741` in debug builds.

No account, API key, payment details or backend is needed to test Google creatives. A connected emulator/device with internet access is needed. Debug mode bypasses UMP and applies G-rated/child-directed test requests. A test creative should visibly identify itself as a test ad.

Testing real consent messages requires your own AdMob App ID, a published Privacy & messaging configuration, and a separately configured UMP test device/geography. The demo configuration does not verify the publisher's consent flow. Do not use production creatives for development or click your live ads.

## Live configuration

The owner confirmed **adults only** on 25 October 2026; this is the configured default audience.

Release builds are ad-free by default. Enable explicitly only after audience and publisher setup are complete:

```
./gradlew :androidApp:assembleRelease \
  -PparcelLiveAds=true \
  -PparcelAdsAudience=adults \
  -PadmobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX \
  -PadmobHomeNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobCategoryNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobWishlistBannerId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobCartBannerId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobPurchasedBannerId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```

Use `children` only for a confirmed child-directed audience; this configures child-directed requests and under-age consent treatment. An app serving both adults and children requires a neutral age screen and per-user treatment before enabling live ads; `mixed`/unknown audiences are deliberately rejected. All requests currently have maximum content rating G.

Live mode updates UMP consent at launch, shows the required form, checks canRequestAds, and exposes Profile → Ad privacy choices when UMP requires it. Ads are removed while privacy choices are being changed. Consent failure without valid consent leaves ads disabled. Google's consent form is not replaced with Parcel's custom dialog.

Required from the publisher:
1. Android AdMob App ID for package `com.parcel.simulator`.
2. Two native ad unit IDs (Home and Category) and three **Banner** ad unit IDs (Wishlist, Cart, Purchased). Native IDs cannot be used for banner placements.
3. Confirmed target age groups/countries; cute styling is not an audience declaration.
4. Privacy-policy URL and configured AdMob Privacy & messaging forms.
5. Developer website hosting `/app-ads.txt`, linked from the Play listing; complete AdMob app verification/readiness review.
6. Updated Play Console ads/Data safety/target audience declarations and release signing setup.

IDs are configuration, not secrets. Never send Google passwords, service-account keys or payment credentials.

## SDK and checks

Uses Google Mobile Ads 25.4.0 to retain existing Android API 23 support; 25.5 raises the minimum to 24. UMP is 4.0.0. No mediation adapters are configured. Recheck supported SDK versions and policies before store release.

Official references:
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/native/advanced
- https://developers.google.com/admob/android/test-ads
- https://developers.google.com/admob/android/privacy
- https://support.google.com/admob/answer/6223431
- https://support.google.com/admob/answer/14538460

Before live release, verify the native validator/Ad Inspector, consent required/not required/denied/error cases with your account, privacy reopening, offline/no-fill, light/dark, large fonts, rotation/backgrounding, and no extra requests on recomposition. Demo ads cannot prove live eligibility, fill or revenue.
