# Play Store listing artwork

Generated, not captured. Every file here is drawn by
[`tools/generate_play_store_art.py`](../../tools/generate_play_store_art.py) from the brand
palette in `Theme.kt`, the parcel mark in `res/drawable/ic_parcel.xml`, the mascot geometry in
`Playful.kt` and the bundled product illustrations.

```sh
python3 tools/generate_play_store_art.py
```

Pillow is the only dependency, and it is needed only to regenerate.

## Screenshots show the real app

Google Play rejected an earlier version of this listing with:

> The screenshots consist of marketing illustrations or graphics instead of the
> in-app experience.

The screenshots are therefore captured from a running device and composited under a
caption band — real UI, not drawn artwork:

```sh
ANDROID_SERIAL=emulator-5554 python3 tools/capture_play_screenshots.py
```

The script walks the app (disclosure, home, clothing, wishlist, shelf, cart, profile),
seeds a wishlist item and a cart so no screen is captured in its empty state, and
writes both the finished panels and the untouched device captures, the latter in
`raw-captures/` so a caption can be reworded without driving the device again.

Re-run it after a UI change that alters one of those screens. The icon and feature
graphic are not screenshots, so they remain illustrative brand art from
`tools/generate_play_store_art.py`; that script no longer emits screenshots.

## Files

| File | Size | Play slot |
| --- | --- | --- |
| `icon-512-teal.png` | 512×512 | App icon — matches the current launcher background |
| `icon-512-navy.png` | 512×512 | App icon — matches the in-app dark theme |
| `feature-graphic.png` | 1024×500 | Feature graphic |
| `phone-01-hero.png` … `phone-08-simulated.png` | 1080×1920 | Phone screenshots (7; Play allows 2–8) |

All files are 32-bit PNG and well under Play's limits (largest is 262 KB against a 15 MB cap).

### Two icons, one decision

The launcher icon's background is teal `#0B5351`, left from the original identity, while the
app itself now runs the navy/orange scheme of the Parcel-inspired refresh. Both icons are
generated so the mismatch is a choice rather than an accident:

- **`icon-512-teal.png`** matches what users see on their home screen today. Play expects the
  store icon to match the launcher icon, so this is the safer upload.
- **`icon-512-navy.png`** matches the feature graphic and screenshots, giving a consistent
  listing page — but then the launcher icon should be changed to match, in
  `res/values/styles.xml` and `mipmap-anydpi-v26/ic_launcher.xml`.

### Tablets

Only phone screenshots are generated. If the listing is opened to tablets, pass a different
canvas size to `screenshot()`; the layout is driven by the `cy` value per panel rather than
by fixed coordinates.

## Listing text

App name, short description and full description — with character counts and the reasoning
behind the wording — are in [listing.md](listing.md). The privacy policy page Play requires
is at [../privacy-policy/index.html](../privacy-policy/index.html).

## Listing copy

The headline and subtitle on each panel double as short-description source material. The
simulation disclosure on `phone-08-simulated.png` is deliberate: the store listing should
state that products, prices and orders are simulated, matching the in-app disclosure.
