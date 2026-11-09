# Parcel advertising

## Current placements

- Native cards: Home after featured offers, category lists after eight products, Wishlist below its heading, and Purchased between its heading and shelf.
- Interstitials: eligible transitions into Cart or Profile using their bottom tabs. Tapping the already-selected tab does not trigger an ad. No banners remain; the earlier Profile banner request was superseded by the interstitial/native layout.
- Only a preloaded interstitial can show. No fill, offline, unavailable consent or SDK initialization causes navigation to proceed immediately; a later load never interrupts the new page.
- Interstitials require at least two tab transitions, a 60-second initial grace period and a 120-second cooldown shared by Cart/Profile. An earned/rewarded ad also resets the cooldown. No ads on launch, exit, checkout/payment forms or tracking/unboxing.
- Google controls ad content, impressions and clicks. Native cards label external advertising and preserve AdChoices. Do not click live ads during testing.

## Shelf rewards

- Six free spaces (1–6), twelve total. Spaces 7–12 can each be unlocked permanently on this device by earning one optional rewarded ad.
- Tapping a locked space shows a custom disclosure with the exact reward and “Not now”. Only “Watch ad · unlock 1 space” requests an ad. Cancelling a pending load prevents it from appearing later.
- A space unlocks only from Google's earned-reward callback, never from loading, showing or dismissal. Skipping/closing without earning or an ad failure leaves it locked. Duplicate callbacks for the same space do not grant another space.
- Unlocks use existing Room preference rows, persist across restarts and remain device-wide like the shelf. Existing occupied slots beyond six are retained on upgrade. No database schema change is required.
- Rewards save in a process scope independent of Activity destruction. This is a local simulator entitlement, not a server-verified or cross-device purchase. Clearing app data removes it. Google rewarded inventory may include interactive creatives as well as video.

## Debug/testing

No account or keys are needed for Google demo ads. Debug builds always use demo unit IDs:

- Native: `ca-app-pub-3940256099942544/2247696110`
- Interstitial: `ca-app-pub-3940256099942544/1033173712`
- Rewarded: `ca-app-pub-3940256099942544/5224354917`
- Sample app ID: `ca-app-pub-3940256099942544~3347511713`

Debug requests use G-rated/child-directed treatment and bypass UMP; the confirmed product audience is **adults only**. Live release configuration uses the audience setting and UMP. Testing actual publisher consent messages requires your own App ID, published Privacy & messaging configuration and a configured UMP test device/geography. Demo ads cannot establish live eligibility or revenue.

## Live configuration

Release builds have ads disabled by default. Supply an Android App ID for package `com.parcel.simulator`, four native unit IDs, two interstitial IDs and one rewarded ID. IDs must match their formats; former banner IDs cannot be reused as native/interstitial IDs.

```
./gradlew :androidApp:assembleRelease \
  -PparcelLiveAds=true -PparcelAdsAudience=adults \
  -PadmobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX \
  -PadmobHomeNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobCategoryNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobWishlistNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobPurchasedNativeId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobCartInterstitialId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobProfileInterstitialId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX \
  -PadmobShelfRewardedId=ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX
```

Configure your privacy-policy URL, AdMob Privacy & messaging forms, developer website with app-ads.txt, store listing, app verification/readiness review and Play ads/Data safety declarations before enabling live ads. Do not send passwords or payment credentials.

UMP updates at launch, shows required consent forms, gates requests through canRequestAds, and offers Profile → Ad privacy choices when required. Changing privacy invalidates preloaded interstitials. Mixed-age audiences require a neutral age screen and per-user treatment before enabling live ads; currently rejected by configuration.

SDK: Google Mobile Ads 25.4.0 retains Android API 23 support; UMP 4.0.0. No mediation adapters. Android owns SDK objects; shared code owns shelf rules and persistence. Other targets default to no ads.

## References

- https://developers.google.com/admob/android/native/advanced
- https://developers.google.com/admob/android/interstitial
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy
- https://support.google.com/admob/answer/6201362

Before live release verify consent scenarios with your account, offline/no-fill, callback errors, background/rotation, reward delivery and ad layout using Ad Inspector. Reward persistence/UI tests with controlled callbacks do not replace a real SDK rewarded-ad test.
