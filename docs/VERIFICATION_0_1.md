# Milestone 1 verification — 22 October 2026

## Verified build

- Java 17, Gradle 8.14.3, Android SDK 36.
- `:androidApp:assembleDebug`: passed.
- `:androidApp:assembleDebugAndroidTest`: passed.
- `:shared:domain:jvmTest`: 5 passed (word search, combined filtering, stable order,
  minor-unit money formatting and invalid sample data).
- `:shared:data:jvmTest`: 1 passed (real Room/SQLite database, idempotent save,
  close/reopen persistence, remove and rejection of unknown product IDs).
- `:shared:presentation:jvmTest`: 3 passed (onboarding/search/save state, failed
  writes without false success, load failure and retry).
- `:androidApp:lintDebug`: passed with 0 errors and 27 non-blocking warnings. Warnings
  concern pinned dependency/tool upgrades, target SDK currency and backup policy.
  The app intentionally disables backup, includes extraction rules and targets API 36.
  Resolve release requirements when preparing a store release; this is a debug MVP slice.

The lint analyzer was updated separately from AGP because the default analyzer
could not read newer Kotlin metadata in transitive dependencies. Final lint runs
with analyzer 32.4.1, while AGP stays at 8.13.2. No lint issues were suppressed to
hide the original API compatibility error; it was fixed using a values-v27 style.

## Emulator verification

Device: **Pixel_9a**, Android 16 / API 36, x86_64, emulator-5554.

`:androidApp:connectedDebugAndroidTest`: 1 comprehensive UI test passed. It covers:

1. First-launch simulator disclosure and acceptance.
2. Search for headphones and exclusion of unrelated products.
3. Product details and selecting the Sand sample colour.
4. Saving to the wishlist and displaying it in the wishlist tab.
5. Activity recreation with the saved product still present.
6. Removing the favourite and displaying the empty wishlist state.
7. Search with no results, resetting filters and filtering the Home category.

The final APK is reinstalled after device tests because the test runner cleans up
its temporary installation. Screenshots in `docs/screenshots` capture the actual
emulator, not design mockups. Visual inspection covered onboarding, bundled fonts,
product images and storefront layout. Duplicate top system insets were corrected.

An additional ADB smoke test (`python3 tools/smoke_restart.py`) passed after
reinstallation: it saved the headphones, force-stopped the application process,
relaunched the app, confirmed onboarding remained accepted and found the saved
product in the wishlist. Parcel was left installed and open on Discover, with
the sample headphones saved.

## Scope and limits

Android and JVM shared targets compile. iOS is not yet configured or tested.
Only the API 36 emulator has device coverage; API 23 and physical devices still
need checks. Dark theme, large-font/TalkBack testing, release signing and broader
release QA remain later milestones. Cart, simulated checkout, order progression,
notifications and unboxing are not implemented in this increment.

No external payment SDK or API credentials exist in the project. Browsing,
onboarding and wishlist operate locally without an INTERNET permission.
