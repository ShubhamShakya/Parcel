# Phone login — local simulator

The flow is Phone → OTP → First/last name for a new number, or Phone → OTP → Shop
for a returning number. A saved session returns directly to the shop after reopening.
Log out from Profile to use another number. Use fictional numbers for testing.

- Select a country, then enter 9–16 digits without the dial code.
- Enter any four non-space characters, for example `1234`, `0000` or `aB9!`.
- No SMS is sent. The screen explains this before requesting the code.
- Resend OTP appears after 30 seconds. A filling ring and seconds count show the wait.
  The deadline is persisted, so rotation/restart does not restart the wait.
- A new number requires both first and last names. Returning numbers retain their
  original names and skip this step. Names are trimmed and capped at 50 characters.
- The country selector supports 30 countries, searchable by name or dial code.

Room v4 adds profile, session and OTP tables through an automatic v3→v4 migration.
Existing cart, receipts, delivery progression and shelf data are preserved. Profiles
are remembered on this installation only. Shopping data is device-wide and shared
when switching local profiles; this does not create separate customer accounts.

The permissive OTP behavior is intentional for the simulator. It is not a real
phone ownership check. No SMS permissions, credentials, phone-reading permissions
or authentication provider are added. Replace `AuthRepository` with a verified
backend flow before using this as real authentication.

Run device checks against an explicit development emulator to avoid selecting an
unrelated connected device:

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew :androidApp:connectedDebugAndroidTest
```

The authentication device test waits for the actual resend deadline, recreates the
activity mid-countdown, signs up and verifies that the same number later logs in
without asking for names again. Shopping tests seed a fictional local test profile.

## UI refinements

Country choices show flags beside their names and dial codes. Phone length limits are enforced without showing the range in the interface. OTP entry uses four boxes backed by one input for typing, paste and deletion. Logout requires confirmation; Stay here dismisses the dialog without changing the session.

Home offers advance automatically every five seconds, pause during dragging, and show one dot per offer inside the card instead of a numeric offers caption.
