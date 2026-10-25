# Parcel — virtual shopping simulator

Android-first Kotlin Multiplatform shopping without spending. **Products, prices,
reviews, offers, taxes, fees, addresses and orders are simulated. No physical items
are purchased or shipped.** Use only fictional address data and test payment inputs.

## Appearance

Parcel starts in dark mode by default. Profile → Appearance switches between light
and dark, with the choice saved on this device. Shared popups and system bars follow
the selected appearance.

## Haat-inspired UI refresh

Sage/navy/amber styling with bundled Albert Sans, illustrated home shortcuts, clothing
entry tiles, sorting, product discount cards, fixed purchase/checkout bars, an illustrative
order map driven by simulated progress, and working account shortcuts. See
[redesign scope](docs/HAAT_REDESIGN.md) and [verification](docs/VERIFICATION.md).

## Companion screens in 0.6.0

Profile editing, notification inbox/preferences, Delhi location selection, demo payment
presets, coupons, product sharing and size guidance, order search/filtering, buy again,
simulated returns, delivery ratings, rider interactions, and offline help/chat now have
working screens. Room migration 4 → 5 preserves existing data and adds profile-specific
companion state. See [screen coverage and integration limits](docs/HAAT_COMPANION.md).

## Existing shopping features

- Phone login/sign-up with a searchable country-code selector and 9–16 local digits.
  Any four non-space OTP characters verify in this offline simulator; no SMS is sent.
- First/last names for new numbers, direct login for existing numbers, saved sessions,
  logout and an animated 30-second resend countdown that survives app restarts.
  Profiles are local; shopping data remains shared on this installation.
- Order confirmation includes a five-stage timeline, a persistent six-digit delivery
  OTP, a four-hour delivery estimate and a Deliver instantly button.
- Shared delivery progression: confirmed → shipped (1h) → nearby hub (2h) → out for
  delivery (3h) → delivered (4h). Reopening catches up; saved progress cannot regress.
- Animated parcel opening with a lifting lid, smiling face and confetti. Only delivered
  orders can be unboxed; repeat taps never duplicate the purchased items.
- Purchased tab with a 12-space shelf and an unboxed collection. Tap a space to choose
  from a bottom-sheet grid showing individual purchases and paid amounts; move, replace
  or put away items. Quantities produce separate collectible units.
- Pastel surfaces, rounded cards, a bouncing parcel friend and matching smiling
  launcher/splash icons.
- Clothing → Men's / Women's, with 104 products (44 men’s, 60 women’s), grouped
  garment types and a winterwear collection. Filter by size, colour, fabric, fit,
  pattern, occasion, season, price and relevant sleeve/neckline/rise attributes.
- Clothing details have separate colour and size selectors; selected combinations
  persist through cart and checkout.
- Compose storefront, offline bundled images, Manrope and matching launcher/splash icon.
- Product galleries randomly contain 5–7 distinct sample illustrations per opening.
- A looping offer carousel randomly selects 3–5 cards per shop session. Tapping a
  card saves one offer; eligible discounts apply automatically at checkout.
- Search, category filters, details, variants and a persistent wishlist.
- Persistent cart with variant quantities, remove controls and a 10-per-variant limit.
- Three fictional addresses near Connaught Place, Delhi, plus add/edit/delete/select.
- Checkout with item subtotal, offer discount, tax (18% on
  discounted merchandise), ₹29 convenience fee, simulated delivery and total.
- Explicit local demo payment, immutable saved receipts and simulated order history.
- PayU CheckoutPro Android sandbox adapter supporting account-enabled UPI/card flows,
  Ktor backend calls, pending-payment recovery and independent verification. **PayU
  is unavailable in the default build until configured; no live credentials or
  gateway transaction tests have been supplied.** See the setup guide below.
- Room v1→v2→v3→v4 automatic migrations preserve wishlist, onboarding, cart and receipts.

Browsing, saved items, cart, demo addresses and local checkout work offline. The
PayU SDK adds network permissions, and PayU sandbox checkout requires internet and
the configured reference/backend service. Tracking, unboxing and shelf placement work offline. Local notifications,
full profile and iOS remain future milestones. This debug increment is not a completed production release.

The storefront uses normal shopping language: prices, reviews, offers, cart and
orders. Repetitive sample/virtual badges and image watermarks are removed. Clear
simulation disclosure remains in onboarding, checkout and About; payment processing
is still test-only.

## Structure

- `androidApp`: Android composition root, splash, PayU SDK adapter and device tests.
- `shared:domain`: models, repository/payment contracts, gallery/offer selection, pricing, delivery schedules and purchased-unit allocation.
- `shared:data`: mock catalog and shared Room entities/DAO/repositories.
- `shared:presentation`: shared Compose screens/navigation, theme and MVVM state.
- `tools/payu_sandbox_server.py`: optional loopback-only Python reference service.

Shared modules target Android/JVM. JVM is for host tests; iOS targets/host are deferred.
The Python helper is not part of the KMP binary and is not a production deployment.

## Build and run

Requires Java 17 and Android SDK 36, plus an emulator/device on API 23+. Open the
root in Android Studio and select `androidApp`. Configure ignored `local.properties`
with `sdk.dir=/absolute/path/to/Android/sdk` or use `ANDROID_HOME`.

```sh
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Checks

```sh
./gradlew :shared:domain:jvmTest :shared:data:jvmTest :shared:presentation:jvmTest
ANDROID_SERIAL=emulator-5554 ./gradlew :androidApp:connectedDebugAndroidTest :androidApp:lintDebug
python3 -m unittest discover -s tools -p 'test_payu*.py' -v
```

Device tests reset this development app's onboarding/cart/address/wishlist data.
Use an emulator without data you need to preserve. Schemas are versioned in
`shared/data/schemas`; no destructive migration fallback is enabled.

Clothing taxonomy lives in `tools/clothing_taxonomy.json`. Regenerate the shared
catalog and matching sandbox fixture with `python3 tools/generate_clothing_catalog.py`,
then regenerate offline clothing artwork with `python3 tools/generate_clothing_art.py`.
Stable IDs and attribute seeds preserve existing cart variant identities.

Original product illustrations come from `tools/generate_product_art.py` (Pillow
is needed only to regenerate them). Manrope's SIL Open Font License is bundled
beside the local font files.

[Phone login](docs/PHONE_LOGIN.md) · [PayU sandbox setup](docs/PAYU_SANDBOX.md) · [Architecture](docs/ARCHITECTURE.md) ·
[Verification](docs/VERIFICATION.md)

## Advertising

Android debug builds include Google native test ads on Home and category lists, plus inline adaptive banner test ads in Wishlist, Cart and Purchased. Release ads are disabled by default. See [AdMob setup](docs/ADMOB.md) for IDs, consent, audience configuration and release requirements.
