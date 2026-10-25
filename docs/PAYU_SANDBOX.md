# PayU sandbox setup

Parcel 0.2 integrates the official Android CheckoutPro SDK 3.3.7 through an
Android-specific `SandboxPaymentGateway`. **It always sets `isProduction=false`.**
The default APK has no backend URL, so PayU checkout is visibly unavailable.
Local demo checkout is a separate, explicitly selected offline payment method.

This integration is prepared, compiled and covered by helper/checkout tests.
**An actual PayU card/UPI transaction has not been executed:** merchant test
credentials and an HTTPS callback host have not been supplied. Do not describe
this as an end-to-end certified PayU integration until the checks below pass.

## What is included

- Native Android CheckoutPro launch; the SDK supplies UPI/card entry screens.
- Ktor HTTP client to obtain a frozen sandbox checkout session, request hashes,
  and verify results. No salt or test key is compiled into the APK.
- A runnable **Python standard-library reference server** in
  `tools/payu_sandbox_server.py`, for local development only. It is not a hosted
  production backend and does not change the KMP app stack.
- SQLite-backed, idempotent sessions; authoritative mock-catalog pricing;
  bearer-scoped hash/verification requests; SDK SHA-512/V2 HMAC-SHA256 handling;
  amount-bound payment hashes; validated callback bridge pages; server-side
  PayU Verify Payment calls to the fixed `https://test.payu.in` endpoint.
- Persistent pending checkout snapshots in Room. Interrupted/uncertain payments
  retain the cart and can be reconciled after restart. Success callbacks alone
  cannot create a shopping order.

## 1. Configure your own test merchant account

Enable UPI and cards for the test account in PayU's dashboard. Use **test** key
and salt only. PayU keys do not have a reliably enforceable `test_` prefix, so the
SDK/environment and fixed test API host enforce the sandbox boundary.

Create a locally ignored `.env` at the project root (replace placeholders locally):

```dotenv
PAYU_TEST_MODE=true
PAYU_TEST_KEY=your_test_merchant_key
PAYU_TEST_SALT=your_test_merchant_salt
PAYU_CALLBACK_BASE=https://your-own-https-callback-host
PAYU_DB=/tmp/parcel-payu-sandbox.db
```

Do not put the salt in chat, Kotlin, Gradle properties, BuildConfig or Git. The
server keeps only mock cart/session data; it does not store card details, VPAs,
SDK response bodies or demo shipping addresses. Avoid request-body logging in any
reverse proxy as well. The SQLite session file has owner-only permissions.

## 2. Run the local reference service

Python 3.10+ is sufficient; no packages need installing.

```sh
set -a
source .env
set +a
python3 tools/payu_sandbox_server.py
```

It binds **127.0.0.1:8787**. Do not expose its general REST endpoints to the public
internet: this is an isolated development service with capability-based checkout
sessions, not account authentication, deployment hardening or multi-user isolation.
Provide an HTTPS reverse proxy/tunnel **only for `/v1/payu/callback/*`** to that
service. Set `PAYU_CALLBACK_BASE` to that HTTPS host. PayU redirects POST payment
results to these URLs; the response invokes the native `PayU.onSuccess` or
`PayU.onFailure` JavaScript bridge after validation and independent verification.

An unknown or tampered SDK hash fails closed. The starter permits payment_source,
vas_for_mobile_sdk, payment_related_details_for_mobile_sdk, get_checkout_details,
getBinInfo, validateVPA and get_sdk_configuration. It does not implement saved
cards, SI/recurring payments, split payments, MCP or unrelated merchant commands.
If a test account/SDK requests an additional hash shape, validate and explicitly
support that command on the server before enabling that feature. Never turn the
hash route into an unrestricted signing oracle.

## 3. Point the Android debug build to the service

For the Android emulator, `10.0.2.2` addresses the Mac host:

```sh
./gradlew :androidApp:installDebug -PpayuSandboxBaseUrl=http://10.0.2.2:8787
```

The URL is the only mobile configuration. The server returns the public merchant
key and session fields. Debug cleartext traffic is permitted only to `10.0.2.2`
and `localhost`. Non-local service URLs must use HTTPS. Release builds reject
HTTP gateway configuration at runtime. There is no live-payment switch.

To return to the normal, unconfigured build:

```sh
./gradlew :androidApp:installDebug
```

## 4. Exercise cards and UPI using official test inputs

Add products, select an offer, choose a demo address, review the sample taxes and
fee, select **PayU sandbox · UPI & cards**, acknowledge the simulation notice and
continue. The SDK controls which account-enabled methods appear. Do not enter
real payment/contact details into this simulator.

For cards, use the current official test card/OTP table linked below. For UPI
collect, use PayU's documented sandbox VPA for the applicable test flow. PayU's
newer test guide also provides an **official UPI Intent Simulator APK** for UAT;
follow its repository instructions to install/configure it for intent testing.
Ordinary installed UPI banking apps are not a substitute for that sandbox fixture.
Device, account and SDK support must be checked with your test merchant setup.

Disable gateway-side extra charges for this demonstration, because the app already
includes its sample fee/tax in the signed amount. An amount/additional-charge
mismatch stays pending instead of silently confirming a differently priced order.

## Backend contract (for replacing the reference server)

All requests/responses use JSON except PayU callback form posts. Preserve idempotency
across server restarts and use authenticated application sessions on a deployed
backend. The random checkout ID is not a replacement for production account auth.

- `POST /v1/payu/checkouts`: receive the shared `PendingCheckout` snapshot. Validate
  mode `payu`, catalog/variants/quantities and calculate totals server-side. Reusing
  the checkout ID with different lines/totals returns an error; retrying the same
  snapshot returns the same transaction ID/session. Return `environment: "test"`,
  `checkoutId`, `transactionId`, `key`, decimal `amount`, `currency: "INR"`,
  `productInfo`, fictional `firstName`, `email`, `phone`, HTTPS `surl`/`furl`, and
  an unguessable `sessionToken`.
- `POST /v1/payu/hash`: bearer token plus `{checkoutId, sdk: {...}}`, where SDK
  fields include `hashName`, `hashString`, optional `hashType`. Validate the merchant,
  allowed command and transaction-bound fields before hashing server-side. Return
  `{hashName, hash}`; do not return the salt or log SDK payloads.
- `POST /v1/payu/verify`: bearer token plus `{checkoutId}`. Independently query PayU
  Verify Payment using the stored transaction ID; compare amount, merchant context,
  successful/captured status and transaction association. Return `environment`,
  `checkoutId`, `status` (`verified`, `failed`, `cancelled`, `pending`), `amountMinor`
  and `currency`. Missing, authorized-only or uncertain results stay pending.
- `POST /v1/payu/callback/success` and `/failure`: validate the reverse signature
  and frozen amount, verify independently, then return the safe native bridge page.

A cancelled/failed **verified terminal result** releases the pending lock and
preserves the cart. A setup/connection failure before opening the SDK also preserves
the cart without leaving a pending lock. If the SDK reports cancellation before
transaction initiation, the app can cancel that unused draft. Once a transaction
has been initiated, uncertain results stay pending for server reconciliation; never
fabricate verification just to dismiss the SDK. Resolve uncertain attempts through
the test dashboard/API. Review this policy during merchant testing.

## Validation checklist before declaring gateway support complete

Run card success/failure/OTP flows, account-enabled UPI collect and simulator intent,
SDK cancellation, hash-service outage, mismatched amounts, repeated callbacks,
app process death during checkout and server restart. Verify pending recovery,
cart preservation and exactly one local receipt after verified success. The Android
UI tests exercise local checkout and the unconfigured PayU gate; they do not claim
to perform PayU transactions.

```sh
python3 -m unittest discover -s tools -p 'test_payu*.py' -v
```

References:

- [Android CheckoutPro integration](https://docs.payu.in/docs/integration-steps-android-checkout-pro)
- [Dynamic SDK hashes](https://docs.payu.in/docs/hash-generation-for-checkoutpro-sdk)
- [Verify Payment API](https://docs.payu.in/reference/verify_payment_api)
- [Android callback URLs](https://docs.payu.in/docs/handling-redirect-urls-surlfurl-with-android-sdk)
- [Current cards and UPI test guide](https://docs.payu.in/docs/test-cards-upi-id-and-wallets)
- [Official UPI Intent Simulator](https://github.com/payu-india/PayU-UPI-Intent-Simulator-App)
