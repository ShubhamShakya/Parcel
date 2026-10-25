# Haat-inspired redesign verification

- Debug APK builds successfully with the new theme, bundled Albert Sans font, home sections, product cards, fixed shopping action bars, delivery map and account shortcuts.
- All five Android workflows pass across the final suite and focused browsing rerun: authentication, checkout, clothing, delivery/unboxing/shelf and browsing/wishlist/recreation.
- Ten shared presentation tests pass.
- Android lint: 0 errors, 43 warnings.
- Product options were split into their own lazy-list section to keep size/colour selection reachable above fixed action bars. Tests now explicitly scroll to the appropriate section before tapping.
- Installed successfully on Pixel_9a. Visually inspected home, featured sections, account, product details, cart, checkout and tracking screenshots in `docs/screenshots/haat-*.png`. Completed one local simulated checkout during the walkthrough; no PayU or real payment was submitted.

# Login and offer UI refinement verification

- Updated Android debug APK built successfully.
- Four of five emulator workflows passed, including authentication with OTP boxes and logout cancellation/confirmation.
- The checkout test failed because its compiled version still expected the removed offers caption. Its source now checks offer-dot semantics and automatic advancement; this revised test has not yet run.
- The final test/lint rerun was blocked by automatic permission review timing out twice. A sandboxed attempt could not access the Gradle wrapper lock outside the workspace. Final lint and reinstall remain pending.
- Earlier verification results below describe the preceding revision.

# Parcel 0.5.0 phone login verification

- Android debug APK 0.5.0 builds and is installed on Pixel_9a (Android 16/API 36).
- 45 shared tests pass: 22 domain, 13 data/persistence, 10 presentation.
- Five emulator workflows pass: authentication, browsing, checkout, clothing and delivery/shelf.
- Android lint passes with 0 errors / 43 non-blocking warnings.

New coverage checks 9/16-digit boundaries, invalid phone characters, dial-code identity,
exactly four non-space OTP characters, mandatory trimmed names and the 30-second resend
boundary. Database tests close/reopen during verified registration and after login,
then confirm returning users retain their names and bypass registration. A v3→v4
migration preserves pre-existing shelf and wishlist data; earlier migration tests
continue to pass. ViewModel tests cover request failure, repeated taps, retry and logout.

The authentication device test selects the UK dial code, rejects too-short/long phone
numbers, recreates the activity during the OTP wait and checks the deadline is unchanged.
It waits for Resend OTP to appear, resends, verifies an arbitrary code, registers a name,
recreates the activity with the saved session, logs out and logs back in using a different
four-character code without seeing registration again.

The final device run explicitly used `ANDROID_SERIAL=emulator-5554`. An initial run
selected a connected I2404 phone and could not access its Compose UI; it is not counted
as a supported-device pass. Emulator startup initially exceeded ddmlib's two-second
property-fetch timeout. Host graphics with Vulkan disabled and the invocation-only
`-Dddmlib.getprop.timeout.sec=30` setting allowed the final run to complete successfully.
No timeouts or test failures were suppressed in the application.

Visual evidence: `phone-login.png`, `country-picker.png`, `otp-countdown.png`,
`otp-resend.png`, `phone-registration.png` and `phone-profile.png` in `docs/screenshots`.
`tools/capture_auth_evidence.py` uses a fictional number and leaves the app logged out.

Authentication is local simulator behavior: no SMS is sent, any four non-space
characters verify, and profiles are stored only on this device. Existing shopping data
is device-wide and shared when switching profiles. This is not a real account-security
boundary. See PHONE_LOGIN.md for the flow and integration boundaries.

---

# Parcel 0.4.0 delivery, unboxing and shelf verification

- Android debug APK 0.4.0 builds and is installed on Pixel_9a (Android 16/API 36).
- 37 shared tests pass: 19 domain, 10 data/persistence, 8 presentation.
- Four emulator workflows pass, including the new delivery → unboxing → shelf flow.
- Six sandbox reference-server tests pass; no PayU network transaction is performed.
- Lint passes with zero errors. Existing dependency/platform warnings remain.

New shared coverage checks exact 4/12/22/24-hour status boundaries, clock rollback,
instant delivery and unboxing idempotency, early-unboxing rejection, stable per-unit
identities and exact paid-total allocation. Database tests close/reopen between order
creation and next-day delivery, then verify the same OTP, unboxing and shelf ownership.
They also check invalid slots, unowned items, moving/replacing/clearing shelf items,
and v2→v3 migration preserving receipts and cart. The existing v1 migration still passes.

The new emulator workflow creates an order, checks its OTP, delivers it instantly,
recreates the activity, unboxes it and places an item. Another recreation preserves
the shelf. Moving and clearing a slot return the unit to the picker without duplication.
Existing checkout, browsing and clothing workflows remain passing.

Visual evidence: `docs/screenshots/order-tracking.png`, `delivery-otp.png`,
`unboxing-ready.png`, `unboxing-open.png`, `shelf-picker.png`, `purchased-shelf.png`
and `unboxing.mp4`. The capture helper enables normal animator playback on the
development emulator. `tools/capture_delivery_evidence.py` creates one local simulated
order to capture this flow; it does not invoke PayU.

Delivery is defined as 24 hours after confirmation. The foreground ticker refreshes
at most every 30 seconds, and reopening catches up after time away. There are no
background delivery notifications yet. Physical-device, minimum-API, iOS, broader
accessibility and signed release validation remain outstanding.

---

# Parcel 0.3.0 clothing verification

Final debug build and checks pass on Pixel_9a, Android 16 / API 36.
The final APK is installed on emulator-5554 after device-test cleanup.

| Check | Result |
| --- | --- |
| Shared domain tests | 15 passed |
| Shared data/persistence tests | 7 passed |
| Shared presentation tests | 8 passed |
| Android emulator UI tests | 3 passed |
| Sandbox reference-server tests | 6 passed |
| Android lint | 0 errors / 43 non-blocking warnings |

Clothing coverage verifies department/type intersections, gender search, winter
selection without duplicated IDs, attribute/price filters, valid colour/size
combinations, cart-to-checkout persistence, all taxonomy types represented and
832 bundled clothing image files. All 110 catalog products match the sandbox
fixture's IDs, titles, prices and variants.

Clothing type/filter sheets open fully so their controls remain reachable.

The new device flow checks men's shirts, winter hoodies, size-required add-to-cart,
women's dresses, combined facets, clearing filters and grouped types including
one-size sarees. The two existing browsing and checkout flows also pass.
Screenshots under `docs/screenshots/clothing-*.png` show the installed UI.

PayU credentials were not supplied; these checks do not make a PayU transaction.
The release limitations recorded below still apply.

---

# Parcel 0.2.1 storefront polish

Removes sample/virtual badges, product/gallery watermarks and repeated simulation
banners from browsing. Product descriptions, pricing, review labels, offers, cart
and order headings use natural shopping language. Onboarding, checkout and About
still explain simulation; PayU remains sandbox-only. No pricing, persistence or
payment-verification rules changed.

Both existing emulator workflows pass with the revised copy. The 0.2.1 APK builds
and lint reports zero errors; screenshots are refreshed from the installed build. The underlying 0.2 business-rule coverage below
remains applicable.

# Parcel 0.2 verification

## Final results

| Check | Result |
| --- | --- |
| Android debug APK | Passed, version 0.2.1 |
| Shared domain tests | 10 passed |
| Shared Room/persistence tests | 5 passed |
| Shared ViewModel tests | 7 passed |
| Sandbox reference-server tests | 6 passed |
| Android emulator UI tests | 2 passed |
| Android lint | Passed, 0 errors / 43 non-blocking warnings |

Toolchain: Java 17, Gradle 8.14.3, Kotlin 2.3.20, AGP 8.13.2, Android SDK 36.
Device: **Pixel_9a, Android 16 / API 36, x86_64, emulator-5554**.

## Shared behavior covered

- Case-insensitive multiword search, category intersection and stable result order.
- Minor-unit money formatting and invalid sample-data rejection.
- Randomized 5–7-image selections and 3–5-offer decks, with no duplicates; seeded
  draws exercise every permitted size.
- Percentage/fixed discounts, eligibility minimums, discount caps, fee waivers,
  sample tax rounding, zero-value empty carts and non-stacking offers.
- Demo-address validation, creation, update, selection, selected-address deletion,
  deleting all addresses and avoiding unexpected reseeding.
- Room v1→v2 migration preserves the original wishlist and onboarding acceptance.
- Cart variant quantity persistence, frozen checkout after database close/reopen,
  wrong-amount rejection, mutation locking, transactional receipt/cart changes,
  repeat-finalization idempotency and receipt persistence.
- Mobile/server mock-catalog parity for product IDs, prices, titles and variants.
- Catalog load retry, persistence-error reporting, onboarding and save state.
- Unconfigured PayU does not create a draft; pending verification preserves the
  cart; matching verified reconciliation completes; another checkout's verification
  cannot finalize; duplicate concurrent submits are ignored.

## Emulator UI tests

The original browsing flow covers onboarding, search, details, variants, wishlist
add/remove, recreation, empty results and category filters.

The new flow covers:

1. Swiping the offer carousel through a full deck and confirming wraparound, then
   swiping backwards and selecting an offer.
2. A product gallery with 5–7 images, switching thumbnails and preserving image
   count/current page across activity recreation.
3. Adding a selected variant to the cart, increasing quantity and retaining it
   across activity recreation.
4. Editing a Connaught Place demo address, deleting another and adding a new one.
5. Checkout displays an automatically applied offer, sample tax and convenience fee.
6. Selecting unconfigured PayU leaves the payment button disabled.
7. Explicitly choosing local demo payment creates a saved simulated receipt.

The APK is reinstalled after the runner cleans up its test installation. Screenshots
are captured from the running app under `docs/screenshots`, including the gallery,
cart, addresses and checkout breakdown. The screenshot helper does not submit an
order or any payment.

## PayU validation boundaries

The real CheckoutPro 3.3.7 dependency and Android callback adapter compile. Native
payment activities explicitly use PayU's AppCompat theme. The SDK always uses test
mode; backend HTTP is allowed only on debug loopback addresses, and callback URLs
must use HTTPS. Merchant salt never enters the mobile build.

The six Python tests cover authoritative totals, persistent idempotent sessions,
invalid bearer tokens, amount-bound payment hashes, V2 hashes, rejected merchant/
command mismatches, rejected callback signatures and independent verified/captured
payment matching. They execute with synthetic keys and **do not call PayU**.

No merchant credentials or public callback host were supplied. Therefore **actual
card/UPI success, failure, OTP, cancellation, intent simulator and process-death
flows through PayU are not yet verified**. The default APK correctly displays
setup required. Follow PAYU_SANDBOX.md before claiming end-to-end gateway support.

## Remaining release work

Only API 36 has emulator coverage; API 23 and physical devices need validation.
iOS, TalkBack/large-font checks, release signing/minification and broader
release QA remain outstanding. The historical 0.2/0.3 builds only had confirmation receipts. Version 0.4 adds
shipping progression and unboxing; notifications remain a future increment.

Lint warnings concern pinned dependency/tool upgrades, target-SDK currency, API-23
network-config/backup attributes and PayU SDK consumer ProGuard rules. No error was
suppressed to get a passing result. A full clean dex build can also emit Kotlin
metadata rewrite warnings from the older AGP/D8 toolchain; debug builds and the
emulator tests pass. Align the release toolchain and verify minification before
store distribution. The Python service is a loopback development reference, not a
publicly hardened, authenticated multi-user payment backend.

## Companion screens · 0.6.0 · 25 October 2026

- Added companion account, notification, product-tool, payment-preview, delivery interaction, return-request and help flows; full scope and integration limits are in `HAAT_COMPANION.md`.
- Final shared checks: **51 tests passed** (25 domain, 16 data, 10 presentation).
- Full Android workflow suite: **6 tests passed** on Pixel 9a / API 36 (`emulator-5554`). The companion test covers inbox read state, editing the existing profile, persisted offline chat and activity recreation.
- Re-ran the companion UI test after final load-error/draft-preservation changes and version bump: **1 passed**. Installed `0.6.0`, version code 7, on the emulator.
- Room 4 → 5 migration test preserves the signed-in profile and saved products. Reopen tests cover profile-specific companion state; return tests cover delivery gating, invalid exchange sizes, duplicate requests and the ten-day deadline.
- Lint at the full regression checkpoint: **0 errors, 43 warnings**. Final follow-up changed load-state handling, chat draft clearing, test isolation, version text and birthday validation/tests; lint was not repeated for those small follow-ups.
- Notification **inbox** is implemented. System delivery notifications remain future work. PayU end-to-end testing remains blocked on sandbox credentials/backend configuration as described above.
- Captured and visually reviewed seven emulator screenshots: account, edit profile, inbox, coupons, demo payment presets, help and offline support. Saved under `docs/screenshots/companion-*.png`; no clipping or overlapping controls observed at the emulator’s default display/font settings.

## Default dark appearance · 25 October 2026

- Shared Material 3 theme now always starts dark, independent of Android's appearance setting. Converted explicit light cards, offers, forms, shelf and tracking surfaces; retained product illustration colors.
- Splash/window backgrounds and Android status/navigation bars use the dark palette with light system icons.
- `assembleDebug` and `installDebug` passed on emulator-5554. Cold launch verified while Android reported `Night mode: no`.
- Visually reviewed home, account and edit-profile screenshots in `docs/screenshots/dark-*.png`. The UIAutomator capture helper encountered a stale hierarchy; account/form screenshots were retaken through direct emulator taps and inspected. No business logic changed; the earlier workflow suite was not rerun for this appearance-only change.


## Dialogs, navigation, appearance and delivery · 25 October 2026

- Shared popup shell used for confirmations, notices, address forms and profile-save feedback. Search uses an accessible cross icon. Profile has a saved light/dark switch; dark remains the first-launch default.
- Bottom-tab stack restoration removed and tab transitions made immediate to fix Home returning to Purchased/stale content.
- New delivery schedules randomly select hourly steps or shortened late-night steps ending before local midnight. The selected duration is persisted; instant delivery timestamps every remaining stage while preserving completed timestamps.
- Full workflow checkpoint: **7 Android tests passed**, including profile save popup, authentication, checkout, clothing, shopping, delivery/unboxing and appearance/navigation.
- Final shared checks: **53 tests passed** (26 domain, 17 data, 10 presentation). Includes exact hourly boundaries, shortened late-night boundaries, instant timestamps, saved appearance and delivery-duration persistence across database reopening.
- Final focused emulator regression: **2/2 passed** (AppearanceNavigationTest and DeliveryFlowTest) after the scheduling, map, system-bar and immediate-transition changes. `installDebug` succeeded on **emulator-5554**, Pixel 9a / API 36. No physical device was used.
- Final manual visual check could not run: the emulator disconnected after the successful tests/install, and `adb devices` returned no devices. Automated verification above completed before disconnection.


## AdMob native placements · 25 October 2026

- Added Android Google native test ads to Home after featured products and category lists after eight products. No forced/interstitial/rewarded ads are enabled.
- `assembleDebug`, default ad-free `assembleRelease` (including vital release lint), and presentation JVM tests passed. Follow-up media binding also compiled in the smoke-test build.
- Confirmed build rejects `parcelLiveAds=true` when audience/publisher configuration is missing.
- Online NativeAdsSmokeTest added for both placements, navigation and recreation. Initial execution could not install because the emulator's package service was not ready after startup crashes. Live publisher consent and serving cannot be verified without the publisher configuration described in `ADMOB.md`.
- Retry after boot completion: **NativeAdsSmokeTest passed (1 test, zero failures/skips)**. Both Google demo native cards loaded and were displayed in Compose; navigation away and activity recreation passed. The emulator had system UI ANR dialogs during boot; these are distinct from the passing app assertions. The instrumentation runner removed its app installation on completion; a separate `adb install -r` then succeeded, leaving the debug app installed.
- Owner confirmed adults-only audience; build default updated accordingly. Live ads remain disabled without explicit opt-in and publisher IDs.


## Wishlist, Cart and Purchased banners · 25 October 2026

- Preserved native ads on Home/category lists. Added inline adaptive Google test banners under the headings of Wishlist, Cart and Purchased. Cart banner is in the scroll content, separate from its pinned checkout action. Checkout/payment/tracking/unboxing remain ad-free.
- Banner width follows available content width (requested maximum height 100dp). SDK objects pause/resume with lifecycle, clean up on navigation, and hide on load failure. Each production placement has its own banner unit configuration.
- Presentation JVM tests and Android build passed. `SectionBannerAdsTest` passed on emulator-5554: Google test banners loaded/displayed in all three tabs, Purchased survived activity recreation, and Cart banner disappeared on proceeding to checkout. No ad was clicked.
