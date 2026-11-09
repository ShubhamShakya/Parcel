# Play Store listing text

Paste these into Play Console → Main store listing. Character counts are given because
Console rejects anything over the limit; counts below are for the text as written here.

Every factual claim is checked against the code: 110 products (104 clothing + 6 others)
across five categories, five delivery stages, a twelve-space shelf, and
`ParcelFeatures.GOOGLE_ADS_ENABLED = false`. **If you enable advertising, change the
"no advertising and no analytics" line in the full description** — it is scoped to "this
release" so it is true today, but it would become false.

---

## Contact details (Console fields, not part of the description)

Play Console asks for these separately under Store settings → Store listing contact details.
Use the same address as the privacy policy so the listing is consistent:

- **Support email:** `supportparcel@gmail.com` (required)
- **Privacy policy URL:** the hosted address of `docs/privacy-policy/index.html`

---

## App name (30 character limit)

```
Parcel — Shopping Simulator
```

27 characters.

---

## Short description (80 character limit)

```
All the joy of shopping. None of the spending. A simulator — nothing is real.
```

77 characters. This reuses the in-app tagline, so the listing matches the feature
graphic and the onboarding screen.

Alternatives, if you prefer a plainer line:

| Text | Count |
| --- | --- |
| `A shopping simulator: browse, cart, track and unbox. No real money, ever.` | 73 |
| `Window-shop, check out and unbox parcels. Simulated — no real money is spent.` | 77 |
| `Browse, cart and unbox parcels. A simulator: nothing is bought or shipped.` | 74 |

---

## Full description (4000 character limit)

```
Parcel is a shopping simulator. Browse, save, add to cart, check out, track the delivery and open the parcel — all without spending a thing. Nothing you order is real, and nothing is ever shipped.

WHAT YOU CAN DO

• Browse more than 100 items across clothing, tech, home, style and outdoors
• Filter clothing by size, colour, fabric, fit, pattern, occasion and season
• Save favourites to a wishlist that waits for you
• Build a cart, change quantities, and pick coupons that really do change the total
• See the full price breakdown at checkout: subtotal, discount, tax and fees
• Follow an order through five delivery stages, or deliver it instantly with one tap
• Open your parcel — the lid lifts, the confetti flies
• Give every unboxed find a place on your twelve-space shelf

EVERYTHING HERE IS SIMULATED

Products, prices, ratings, reviews, offers, taxes, fees, addresses and orders are all invented for demonstration. No real money is charged. Nothing is purchased, reserved or delivered. Parcel is not a shop, and is not affiliated with any retailer or brand.

WORKS OFFLINE

Browsing, your wishlist, cart, saved addresses and the whole checkout work with no connection at all.

STAYS ON YOUR PHONE

Your profile and shopping history are saved only on your device. There is no account server. This release contains no advertising and no analytics. Uninstall the app and everything goes with it.

THE SMALL DETAILS

Every product illustration was drawn for this app. A smiling parcel keeps you company while you shop. Light and dark themes follow your phone's own setting.

Parcel is for anyone who enjoys window shopping, who wants to try the shape of an online order without the cost, or who simply likes opening parcels.
```

1,732 characters.

### Why it is written this way

- **The disclosure leads.** Play's metadata policy expects a listing to represent the app
  honestly, and a shopping app that is not a shop needs saying so immediately. Play also
  collapses the description after roughly three lines, so "simulator" has to appear in the
  first sentence rather than in a disclaimer at the bottom.
- **"Not affiliated with any retailer or brand"** is deliberate. It heads off the reading
  that this is a storefront for someone else's catalogue.
- **No superlatives, no keyword stuffing, no fake testimonials, no emoji.** All of these
  draw metadata rejections.
- **Claims stay checkable.** "More than 100 items" against 110; "five delivery stages" and
  "twelve-space shelf" against the code.
