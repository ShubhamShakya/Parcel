"""Play Store phone screenshots, captured from the running app.

Google Play requires store screenshots to show the actual in-app experience, so
these are real device captures with a caption composited above them — not drawn
artwork. Development dependency: Pillow, plus an emulator or device on adb.

    ANDROID_SERIAL=emulator-5554 python3 tools/capture_play_screenshots.py

Re-run after a UI change that alters one of the captured screens.
"""
from pathlib import Path
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
OUT = ROOT / 'docs/play-store'
RAW = OUT / 'raw-captures'
FONTS = ROOT / 'shared/presentation/src/commonMain/composeResources/font'
PKG = 'com.shubhamshakya.parcel'
ACTIVITY = f'{PKG}/{PKG}.android.MainActivity'

SERIAL = os.environ.get('ANDROID_SERIAL', 'emulator-5554')
ADB = [str(Path.home() / 'Library/Android/sdk/platform-tools/adb'), '-s', SERIAL]
OUT.mkdir(parents=True, exist_ok=True)
RAW.mkdir(parents=True, exist_ok=True)

NAVY, IVORY, MUTED, ORANGE = '#0A0F1C', '#EEF1F7', '#9AA3B8', '#F2A541'
W, H = 1080, 1920
SS = 2  # Supersample the caption band so text stays crisp.


# --- device helpers ---------------------------------------------------------
def run(*a): return subprocess.check_output(ADB + list(a), text=True)


def nodes():
    run('shell', 'uiautomator', 'dump', '/sdcard/play-shot.xml')
    return list(ET.fromstring(run('shell', 'cat', '/sdcard/play-shot.xml')).iter('node'))


def find(text, prefix=False):
    for n in sorted(nodes(), key=lambda n: 0 if n.get('text', '') == text else 1):
        for v in (n.get('text', ''), n.get('content-desc', '')):
            if v and (v.startswith(text) if prefix else v == text):
                b = list(map(int, re.findall(r'\d+', n.get('bounds', ''))))
                if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
                    return n
    return None


def tap(n):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
    run('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))


def click(text, prefix=False, scroll=False, required=True):
    for _ in range(9 if scroll else 3):
        n = find(text, prefix)
        if n is not None:
            tap(n); time.sleep(1.1); return True
        if scroll:
            run('shell', 'input', 'swipe', '540', '1850', '540', '700', '350'); time.sleep(.4)
        else:
            time.sleep(.6)
    if required:
        raise AssertionError('Missing UI: ' + text)
    return False


def grab(name):
    """Capture the current screen, unscaled, straight from the device."""
    nodes()  # let the hierarchy settle first
    path = RAW / f'{name}.png'
    path.write_bytes(subprocess.check_output(ADB + ['exec-out', 'screencap', '-p']))
    return path


# --- composition ------------------------------------------------------------
def font(weight, size):
    name = {400: 'manrope_400', 500: 'manrope_500', 700: 'manrope_700'}[weight]
    return ImageFont.truetype(str(FONTS / f'{name}.ttf'), size * SS)


def centred(d, text, fnt, fill, cx, y, gap=14):
    for line in text.split('\n'):
        box = d.textbbox((0, 0), line, font=fnt)
        d.text((cx * SS - (box[2] - box[0]) / 2 - box[0], y * SS), line, font=fnt, fill=fill)
        y += (box[3] - box[1]) / SS + gap
    return y


def compose(name, headline, sub, capture):
    """Caption band on top, the real capture below, at Play's 1080x1920."""
    band = Image.new('RGB', (W * SS, H * SS), NAVY)
    d = ImageDraw.Draw(band)
    y = centred(d, headline, font(700, 62), IVORY, W / 2, 86)
    centred(d, sub, font(400, 32), MUTED, W / 2, y + 12)
    d.rounded_rectangle((W / 2 * SS - 46 * SS, 352 * SS, W / 2 * SS + 46 * SS, 358 * SS),
                        radius=3 * SS, fill=ORANGE)
    canvas = band.resize((W, H), Image.LANCZOS)

    shot = Image.open(capture).convert('RGB')
    frame_w = 880
    frame_h = round(shot.height * frame_w / shot.width)
    shot = shot.resize((frame_w, frame_h), Image.LANCZOS)
    top = 410
    if top + frame_h > H - 40:                      # crop the tail, never squash it
        shot = shot.crop((0, 0, frame_w, H - 40 - top))
        frame_h = shot.height
    mask = Image.new('L', (frame_w, frame_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, frame_w, frame_h), radius=28, fill=255)
    canvas.paste(shot, ((W - frame_w) // 2, top), mask)
    d2 = ImageDraw.Draw(canvas)
    d2.rounded_rectangle(((W - frame_w) // 2, top, (W + frame_w) // 2, top + frame_h),
                         radius=28, outline='#263050', width=2)
    canvas.save(OUT / f'phone-{name}.png')
    print(f'  phone-{name}.png  {W}x{H}  <- {capture.name}')


# --- the walk ---------------------------------------------------------------
def main():
    run('shell', 'am', 'force-stop', PKG)
    run('shell', 'am', 'start', '-W', '-n', ACTIVITY)
    time.sleep(3)

    panels = []
    # 1. The disclosure is a real screen and states the simulation in the app's own words.
    if find('I understand · Explore Parcel') is not None:
        panels.append(('08-simulated', 'No real money.\nEver.',
                       'The app says so before you browse', grab('disclosure')))
        click('I understand · Explore Parcel')
        time.sleep(2)

    from simulator_login import login_if_needed
    login_if_needed(run, find, click)
    time.sleep(2)

    click('Home', required=False)
    time.sleep(1.5)
    panels.append(('01-hero', 'All the joy.\nNone of the spending.',
                   'A shopping simulator for Android', grab('home')))

    # Seed a little shopping so the wishlist and cart screenshots show real content
    # rather than their empty states.
    click('Save ', prefix=True, required=False)          # heart on a product card
    time.sleep(.8)
    click('+', required=False)                           # quick-add the same card
    click('Got it', required=False)
    click('+', required=False)                           # a second item
    click('Got it', required=False)
    time.sleep(1)

    click('Clothing', scroll=True, required=False)
    time.sleep(1.5)
    panels.append(('02-browse', 'Browse\nhandpicked finds',
                   'Over 100 pieces, filtered how you like', grab('browse')))

    click('Home', required=False); time.sleep(1.2)
    click('Wishlist', required=False); time.sleep(1.5)
    panels.append(('03-wishlist', 'Save every\nfavourite',
                   'A wishlist that waits for you, offline', grab('wishlist')))

    click('Purchased', prefix=True, required=False); time.sleep(1.8)
    panels.append(('07-shelf', 'A shelf for\nyour finds',
                   'Give every unboxed find a home', grab('shelf')))

    click('Cart (', prefix=True, required=False); time.sleep(1.8)
    panels.append(('04-offers', 'Every rupee\naccounted for',
                   'Subtotal, discount, tax and fees — all simulated', grab('cart')))

    click('Profile', required=False); time.sleep(1.5)
    panels.append(('05-account', 'Your shopping,\nyour way',
                   'Light or dark, following your phone', grab('profile')))

    print(f'Composing {len(panels)} Play screenshots in {OUT}')
    for name, headline, sub, capture in panels:
        compose(name, headline, sub, capture)
    print('Done. Raw captures kept in docs/play-store/raw-captures/.')


if __name__ == '__main__':
    main()
