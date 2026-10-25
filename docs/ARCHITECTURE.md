# Virtual Shopping Simulator — implementation proposal

Working product name: **Parcel**. Android first, shared Kotlin and Compose, iOS next.

Implementation revision: **0.5.0, local phone login and sign-up**. Includes the agreed visual
identity, randomized offline galleries/offers, persistent cart, editable demo
addresses, local demo payment and prepared PayU sandbox integration. PayU remains
unavailable until the user configures test credentials on a backend; no live
payment path is included. See PAYU_SANDBOX.md for setup and current limitations.

## Module structure and dependency direction

```text
androidApp/                      Android launcher, splash, PayU adapter, DI and device tests
shared/
  domain/src/commonMain/         Immutable models, repository contracts, use cases
  domain/src/commonTest/         Business-rule tests using fake repositories/clocks
  data/src/commonMain/           Mock catalog, Room entities/DAOs, repository adapters, DI
  data/src/androidMain/          Android Room builder, Ktor engine
  presentation/src/commonMain/  Compose design system, navigation, screens, ViewModels
  presentation/src/commonTest/  State and action tests
tools/payu_sandbox_server.py     Optional loopback Python sandbox reference backend
```

`androidApp → presentation + data → domain`. Presentation never imports data. Domain
has no Android, database, HTTP or Compose dependencies. The Android composition
root supplies a database and platform services to Koin. ViewModels expose immutable
StateFlow and accept actions; repositories own persistent state. Shared domain
contains the `SandboxPaymentGateway` interface; Android supplies the CheckoutPro
SDK adapter and Ktor client. Merchant salt/hashing and independent verification
belong to a backend. The optional Python reference service is a development tool,
not part of the KMP binary or a production deployment. Room v4 exports its schema and automatically migrates v1→v2→v3→v4 without erasing
saved items, onboarding, cart or receipts.

Initially all shared modules target Android and JVM (for fast host tests). iOS
source sets, native Room/KSP targets, Darwin Ktor engine, notification adapter,
ComposeUIViewController and an Xcode host are milestone 7. JVM is a test target,
not a shipped desktop application. No claim of iOS support until compiled there.

## Navigation graph

```text
bootstrap → disclosure (first launch) → phone → OTP → names (new number only) → home
bootstrap → home (saved signed-in profile)
profile → logout → phone
home ↔ wishlist ↔ cart ↔ purchased ↔ profile (includes About)
home/wishlist → product/{productId} (5–7-image gallery)
cart → addresses (select/add/edit/delete) → checkout
checkout → local demo confirmation → receipt/{receiptId}
checkout → PayU sandbox SDK → server verification → receipt/{receiptId}
cart → orders → receipt/{receiptId}
receipt/{id} (tracking) → unbox/{id} (delivered only) → purchased (shelf + collection)
[planned] profile → preferences / simulation disclosure
```

Product routes carry IDs, never entire objects. Missing IDs produce a recoverable
not-found state. Disclosure acceptance is persistent and required before browsing.
Onboarding, checkout and About disclose simulation. Checkout repeats the full
no-money/no-delivery disclosure before every order. Back navigation from checkout
must not accidentally place another order.

## Data models

- `Money(minorUnits: Long, currency: String)`: integer arithmetic; INR initially.
- `Category`, `Product`: stable ID, category, title, description, price, comparison
  price, local image key, selectable variants, sample ratings and reviews.
- `Review`: author alias, stars, body; all reviews explicitly synthetic.
- `CatalogRepository`: offline product stream; `SavedItemsRepository`: saved ID stream
  and idempotent setSaved; `PreferencesRepository`: disclosure acceptance stream.
- `CartItem(productId, variant, quantity)`: composite identity; quantity 1–10.
- `OrderLine`: immutable title/variant/quantity/unit-price snapshot.
- `DemoAddress`: three seeded fictional locations near Connaught Place; add/edit/delete
  and selection are persistent. Deleting all addresses never reseeds them unexpectedly.
- `Offer`, `CheckoutTotals`: one non-stacking offer, eligibility/minimum/caps, integer
  minor-unit discounts, sample 18% tax on discounted goods and a sample ₹29 fee.
- `PendingCheckout`: random checkout ID, payment mode, frozen lines/address/totals and
  creation timestamp. Locks mutations while payment is uncertain; survives restarts.
- `Receipt`: immutable completed simulated order, unique by checkout ID. A transaction
  inserts the receipt, removes purchased cart rows and clears pending state once.
- `CommerceRepository`, `SandboxPaymentGateway`, `PaymentResult`: shared storage and
  payment boundaries; SDK callbacks cannot directly finalize receipts.
- Planned `Order`: UUID, createdAt epoch milliseconds, status schedule, delivery
  estimate, cancellation timestamp, unboxedAt, immutable totals and lines.
- Planned `OrderStatus`: Upcoming, Processing, Shipped, OutForDelivery, Delivered,
  Cancelled. Cancellation is terminal and delivery cannot regress.
- Planned `NotificationPreferences`, `NotificationReceipt(orderId, milestone)`.

## Clothing catalog

`ClothingDetails`, `ClothingType` and `ClothingFilter` live in shared domain code.
The Clothing category contains Men's and Women's departments with eight garment
groups: topwear, bottomwear, outfits, ethnic, activewear, sleepwear, innerwear and
winterwear. Winterwear filters the existing catalog, including suitable garments
from other groups; it does not duplicate products or cart identities.

The taxonomy generator produces 104 products (44 men's, 60 women's), domain type
metadata and the matching sandbox catalog fixture. All products have bundled
artwork and seven gallery compositions. Clothing sizes use standard labels,
waist measurements, bra sizes or One size as appropriate. Separate colour/size
selectors resolve to a validated existing cart variant. The clothing increment required no schema change; delivery now adds the v3 tables. Department/type changes clear incompatible facets.

## Simulation and persistence design

Implemented checkout persists a frozen draft before confirmation, then finalizes
once in a Room transaction. A local demo confirmation works offline. PayU requires
an independently verified test result with the same checkout ID and amount. Failed
terminal results preserve the cart; uncertain results retain the pending lock and
can be reconciled after restart. Gateway details are in PAYU_SANDBOX.md.

Gallery image count is chosen randomly once per product opening, retained across
recomposition/configuration changes. Every product has seven bundled original
sample compositions. The shared offer deck draws 3–5 distinct cards per ViewModel
session. A centered modulo-indexed horizontal pager lets users loop in both
directions. Selected offer persistence is independent of the randomly displayed
deck and remains valid at checkout when its threshold is met.

## Phone login and sign-up

Shared domain models (`DialCountry`, `OtpChallenge`, `ShopperProfile`, `AuthSnapshot`)
and validators sit behind `AuthRepository`. The Room implementation adds v4 tables
for profiles keyed by canonical dial-code + local digits, a singleton session and a
singleton pending OTP challenge. Profile creation and session changes are transactional.
Names are requested only after verification of a new number. Existing numbers log in
without overwriting their stored names. Session, verified registration progress and
resend deadline survive reopen. Country selections sharing a dial code share the same
phone identity; a different dial code produces a different identity.

This is deliberately local simulator authentication, not SMS verification. Any exactly
four non-space characters verify. No generated secret, real OTP sender, server user
directory or network dependency is involved. Phone numbers contain 9–16 ASCII digits
excluding the dial code. The searchable selector currently contains 30 countries.
Both names are required, trimmed and limited to 50 characters each. A future real
login service must replace this permissive repository and add server verification.

`AuthViewModel` owns load/retry and in-flight mutations. The Compose gate precedes
shopping; Profile shows the saved name/number and logout. An animated ring reflects
elapsed wall-clock time until the persisted 30-second resend deadline, at which point
Resend OTP replaces the countdown. Resending is also time-checked in the repository
and clears the visible code for another attempt. Existing commerce data is preserved
and remains device-wide when switching local profiles; it is not account-isolated.

## Delivery, unboxing and purchased shelf

`DeliveryProgress` is shared domain state: confirmation time, stable six-digit OTP,
monotonic observed time, optional instant-delivery time and optional unboxed time.
Five stages occur at 0/4/12/22/24 hours after payment confirmation. The confirmation
transaction inserts delivery progress alongside the receipt. Existing v2 receipts
are backfilled with their original creation date; they are never automatically unboxed.

`RoomDeliveryRepository` accepts an injectable clock. Refreshing on foreground entry,
new receipt and every 30 seconds while started catches up from wall-clock time.
The UI does not need to run in the background for elapsed delivery time to count.
Persisted observed time never decreases. Instant delivery and unboxing are idempotent;
opening before delivery is rejected by the repository, not just hidden by the UI.

`delivery_progress` stores per-order state, while `shelf_placements` stores slot 0–11
and a unique purchased-unit identity. Units derive from immutable receipt line and
quantity indices. The order total is allocated proportionally in integer minor units,
including discounts, taxes and fees; the unit amounts sum to the original total.
Unboxing an order unlocks every unit in it. Putting an item away or replacing it does
not remove ownership. A transaction validates ownership, frees the target and previous
slot, and places a unit at most once. Both delivery and shelf survive database reopen.

The shared Compose unboxing uses a finite lid-opening/confetti animation and marks
an order unboxed only after opening completes. An interrupted animation can safely
restart. The Purchased tab shows the shelf and all unboxed units; the picker displays
paid amounts and identifies units already on a shelf. No drag gesture is required.

Local status notifications, cancellation and a full profile remain future work.

## Milestones and acceptance criteria

1. **Foundation + browsing (implemented):** Gradle wrapper; modular Android/KMP
   project; shared Compose storefront and details; bundled product art; search,
   category filters, sample reviews, variants; Room-persisted wishlist and onboarding;
   Koin composition; bundled Manrope fonts, launcher/splash branding; domain/state
   tests; debug APK build. No dormant checkout buttons.
2. **Cart (implemented):** persisted variant/quantity rows, shared integer totals, stock/quantity
   validation, cart UI and restart tests.
3. **Checkout (implemented locally; PayU prepared):** fictional address CRUD,
   explicit disclosure, offers, sample tax/fee totals, atomic idempotent receipts,
   pending recovery and restart tests. Native PayU SDK and local reference service
   are prepared; merchant credentials and end-to-end card/UPI tests remain required.
4. **Orders + tracking (implemented):** next-day schedules, timeline, OTP, instant
   delivery and restart/time-jump/clock-rollback tests. Cancellation remains planned.
5. **Notifications + preferences:** Android scheduler and permission UX, receipt
   deduplication, notification deep links and background verification.
6. **Unboxing + shelf (implemented):** animated opening, persistent completion and a
   12-space shelf. Full profile, broader accessibility checks, dark theme and release
   checks remain planned.
7. **iOS:** native targets and Room builders, Darwin networking, native notifications,
   Xcode host, platform lifecycle tests and device acceptance.

## Design and quality bar

### Theme colors

Use a consistent Material 3 theme with fixed brand colors (no dynamic color override
in the initial release). Light mode is the initial implementation; dark mode is
milestone 6, using the same semantic roles and verified contrast.

| Role | Hex | Usage |
| --- | --- | --- |
| Primary | `#0B5351` | Toolbar, primary buttons, selected navigation |
| On primary | `#FFFFFF` | Text and icons on primary |
| Accent | `#FFB766` | Offers, featured banners, small highlights |
| On accent | `#252B32` | Dark text on orange; no white body text on this accent |
| Background | `#FFFAF4` | Warm cream screen background |
| Surface | `#FFFFFF` | Cards, sheets, search fields |
| Main text | `#20272E` | Headings, descriptions, amounts |
| Secondary text | `#52616B` | Supporting information |
| Error | `#B3261E` | Errors and validation |
| Outline | `#75827F` | Control outlines and boundaries |

Use generous spacing, rounded 16 dp cards, familiar two-column product grids and
restrained motion. Teal is the main identity; orange is an accent. Validate WCAG AA
text contrast, touch target sizes and TalkBack labels during implementation.

### Typography

Use **Manrope**, a clean modern sans-serif, bundled as local font resources in
shared presentation so Android and future iOS have identical offline typography.
Bundle the upstream SIL Open Font License with the font assets.

- Regular 400: descriptions, reviews and supporting text.
- Medium 500: inputs, navigation and labels.
- SemiBold 600: buttons, product names and section headings.
- Bold 700: page titles and prominent prices.

Suggested scale: page title 28 sp, section title 22 sp, product title 16 sp, body
16 sp, labels 14 sp and supporting text 12 sp. Honor system font scaling and avoid
fixed-height text containers. Use tabular numerals for aligned prices where supported.

### App icon and splash

Design an original, minimal **ivory parcel/shopping-bag symbol with a small orange
fold or handle detail on a deep teal background**. Keep the mark bold and readable
at launcher size, with no text inside the icon. This is the proposed design direction;
the implemented mark now has a smiling face for the playful 0.4 theme.

- One vector master supplies the launcher and splash foreground mark.
- Android adaptive launcher: teal background plus safe-zone foreground; also provide
  legacy launcher resources and a monochrome themed-icon layer.
- Splash: the same icon mark centered on the same teal background using Android's
  SplashScreen API and compatibility library. Respect system icon masking and sizing;
  avoid a second splash screen or an artificial startup delay.
- Opening the app transitions to bootstrap, then onboarding or home. The onboarding
  explains that payments and deliveries are simulated.
- Future iOS app-icon exports and launch branding derive from the same master.

Local bundled product illustrations keep the first launch fully offline; Coil 3
renders them and can later render URLs. Browsing uses ordinary product, price and review labels. Simulation disclosure is
concentrated in onboarding, checkout and About; no fabricated live inventory or urgency. Loading, empty, persistence error, retry
and missing product states are required.

## Payment simulation

Users select fictional address details, review all totals and explicitly acknowledge
simulated shopping before confirming. Local demo payment is fully offline. PayU
sandbox is a separate selectable method; setup status is visible and the button is
disabled until configured. There is no silent fallback between payment methods.
Only test inputs belong in the SDK. No real fulfillment or live payment mode exists.

Unit tests cover meaningful filtering, state transitions and persistence failure
handling. Build verification is recorded separately from UI/device verification.
Production release also requires device QA, migration tests, accessibility review,
privacy policy and signing; this increment remains a foundation, not a release.

## Dependency baseline

Pinned Kotlin 2.3.20 / Compose Multiplatform 1.11.1 / AGP 8.13.2 / Gradle 8.14.3,
Material 3 1.9.0 (stable), Room 2.8.4, KSP 2.3.1, Coroutines 1.11.0, Koin 4.2.1,
Ktor 3.5.0, Navigation 2.9.2, Coil 3.5.0, Lifecycle 2.9.6, Kotlin Serialization 1.10.0 and PayU CheckoutPro 3.3.7. Android minimum API 23,
compile and target API 36, Java 17. The baseline was updated to compatible versions
available in the local build environment. The lint analyzer is pinned separately
to 32.4.1 (AGP-version setting 9.4.1) so it can read the dependency graph’s newer
Kotlin metadata; the Android build plugin remains 8.13.2. Verification results
are recorded in VERIFICATION.md.

References: https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html,
https://developer.android.com/build/releases/about-agp,
https://developer.android.com/kotlin/multiplatform/room,
https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html.
