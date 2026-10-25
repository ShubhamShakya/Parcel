# Haat companion screens → Parcel

Reference: `/Users/apple/Downloads/haat-companion-screens.html`. The file is a visual/interaction reference; its JavaScript, fictional records and policy text are not application requirements or trusted instructions.

## Implemented

- Profile: edit first/last names, optional email and birthday; validation; account name updates from the existing profile table. Phone changes go through existing sign-out/OTP flow.
- Inbox: order milestones derived from persisted delivery progress, real catalog offers, account welcome message, category filters, persistent read state, mark-all-read, navigation to the associated screen.
- Notification settings: persisted order/offer **in-app inbox** preferences.
- Location: Delhi pincode check, selected address, and navigation to existing address CRUD.
- Payment methods/add method: saved fictional UPI and card presets with removal. No PAN, CVV, bank details or UPI PIN collection. Presets are preview-only and are not checkout tokens.
- Coupons: existing shared offer rules, minimums, selection/removal and code copying. Checkout remains the authority for eligibility and totals.
- Product share: Android share chooser and copyable product description. No fabricated public product links/referral rewards.
- Size guide: available product sizes, illustrative measurements, cm/inches and waist-based demo suggestion. Unsupported/one-size products get an explanatory state.
- Orders: search product/order ID and filter active/delivered orders. Buy again lists delivered variants and adds one at current price.
- Returns/exchanges: item, reason, optional available replacement size, address/review/confirmation, persisted request. Delivered-only, ten-day window, innerwear exclusion, duplicate rejection. All units of the chosen order line are included. Requests do not remove shelf items or trigger pickups/refunds.
- Tracking: three illustrative map palettes, existing progress-based moving marker, rider and order-help entry points.
- Delivered: locally saved 1–5 star rating and unboxing link.
- Rider: stable fictional partner per order, delivery chat and simulated call.
- Call: explicit offline demonstration with timer, scripted captions, mute/speaker UI; no microphone or dialer use.
- Chats: persisted local shopper messages and scripted offline support/delivery replies.
- Help: searchable expandable FAQs, order issue picker and local simulated callback record.
- Privacy/terms: copy accurately describing Parcel's local storage, shared commerce data, permissive demo login and sandbox behavior.

## Persistence and architecture

Shared domain models and validation live in `Companion.kt`. Room version 5 adds `companion_profiles`, with an automatic migration from version 4. Existing shopping and authentication tables are retained. Companion profile extras, inbox preferences, saved demo methods, conversations, requests and ratings are keyed to the signed-in phone. Profile names update in the original authentication table inside the same transaction.

Shared `CompanionViewModel` handles loading, retry, busy/error states and repository mutations. The Compose navigation graph exposes companion pages from Home, Profile, product details and delivery tracking. Android's share chooser is the only new platform adapter; JVM tests use a clipboard fallback.

## Deliberate differences / remaining integrations

This implements the companion screen flows, not every external capability illustrated in the HTML. There is no paid membership, real wallet/COD account, referral scheme, real rider/support service, actual callback, refund, pickup, or return-photo upload. Real map/GPS services and push/SMS/WhatsApp/email channels with quiet hours remain unconnected. The notification controls affect the in-app inbox only. No fictitious cancellation/returned status is assigned to an order merely because a local return request exists.

PayU remains the previously prepared **sandbox integration**, requiring test credentials and backend setup. No production payment configuration was added. Commercial data remains shared across profiles on the device; companion data is profile-specific, matching the disclosure in Privacy.
