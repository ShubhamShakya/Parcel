"""Play Store icon and feature graphic. Development dependency: Pillow.

Illustrative brand art: the parcel friend, the launcher mark and brand shapes.
Regenerate after a brand or palette change with:

    python3 tools/generate_play_store_art.py

This script does NOT produce the store screenshots. Google Play requires those to
show the actual in-app experience, and a listing whose screenshots were drawn
artwork was rejected for exactly that. They are captured from a running device by
tools/capture_play_screenshots.py instead.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/play-store'
FONTS = ROOT / 'shared/presentation/src/commonMain/composeResources/font'
PRODUCTS = ROOT / 'shared/presentation/src/commonMain/composeResources/files/products'
OUT.mkdir(parents=True, exist_ok=True)

# Brand palette, mirroring ParcelTheme's dark scheme and the launcher icon.
NAVY = '#0A0F1C'
SURFACE = '#151C30'
ORANGE = '#F2A541'
IVORY = '#EEF1F7'
MUTED = '#9AA3B8'
TEAL = '#0B5351'
PEACH, LAVENDER, MINT, PINK = '#402D24', '#2A2543', '#18382F', '#3D2635'

SS = 3  # Supersampling factor; every canvas is drawn large and resampled down.
def font(weight, size):
    name = {400: 'manrope_400', 500: 'manrope_500', 600: 'manrope_600', 700: 'manrope_700'}[weight]
    return ImageFont.truetype(str(FONTS / f'{name}.ttf'), size * SS)

def canvas(w, h, colour=NAVY):
    return Image.new('RGB', (w * SS, h * SS), colour)

def save(im, name, size):
    im.resize(size, Image.LANCZOS).save(OUT / name)
    print(f'  {name}  {size[0]}x{size[1]}')

def centred(d, text, fnt, fill, cx, y, spacing=1.18):
    """Draw centred, optionally multi-line text; returns the y below the block."""
    for line in text.split('\n'):
        box = d.textbbox((0, 0), line, font=fnt)
        d.text((cx * SS - (box[2] - box[0]) / 2, y * SS), line, font=fnt, fill=fill)
        y += (box[3] - box[1]) / SS * spacing + 10
    return y

def glow(im, cx, cy, r, colour, alpha=70):
    """A soft pastel blob, used to keep the dark backgrounds from going flat."""
    layer = Image.new('RGBA', im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    rgb = tuple(int(colour[i:i + 2], 16) for i in (1, 3, 5))
    d.ellipse((cx * SS - r * SS, cy * SS - r * SS, cx * SS + r * SS, cy * SS + r * SS), fill=rgb + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(45 * SS))
    im.paste(Image.alpha_composite(im.convert('RGBA'), layer).convert('RGB'), (0, 0))

# --- The parcel friend, ported from ParcelFriend in Playful.kt --------------
def parcel_friend(im, cx, cy, scale, opening=0.0, confetti=False):
    """Draw the mascot centred on (cx, cy); `scale` is the width in design px."""
    u = scale * SS / 240.0
    ox, oy = cx * SS - 120 * u, cy * SS - 120 * u
    def at(x, y): return (ox + x * u, oy + y * u)
    layer = Image.new('RGBA', im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse([at(42, 205), at(198, 220)], fill=(238, 241, 247, 20))
    if confetti:
        for i in range(20):
            angle = i * 2.399
            radius = (25 + opening * 75) * u
            px = ox + 120 * u + math.cos(angle) * radius
            py = oy + 110 * u + math.sin(angle) * radius - math.sin(opening * math.pi) * 30 * u
            piece = Image.new('RGBA', (int(8 * u) + 4, int(13 * u) + 4), (0, 0, 0, 0))
            ImageDraw.Draw(piece).rounded_rectangle(
                (2, 2, 7 * u, 12 * u), radius=3 * u,
                fill=[ORANGE, '#B399E8', '#EE91AA', '#7EA6FF'][i % 4])
            piece = piece.rotate(i * 23 + opening * 130, expand=True, resample=Image.BICUBIC)
            layer.paste(piece, (int(px), int(py)), piece)
    d.rounded_rectangle([at(48, 95), at(192, 205)], radius=15 * u, fill='#E8AD75')
    d.rounded_rectangle([at(54, 95), at(186, 200)], radius=12 * u, fill='#FFDFC9')
    d.rectangle([at(108, 95), at(132, 200)], fill='#FFF4C7')
    for ex in (91, 150):
        d.ellipse([at(ex - 5, 146), at(ex + 5, 156)], fill='#14213D')
    d.ellipse([at(70, 158), at(90, 168)], fill='#FFDCE7')
    d.ellipse([at(152, 158), at(172, 168)], fill='#FFDCE7')
    pts = [(108 + 24 * t / 20, 164 + (179 - 164) * 2 * (t / 20) * (1 - t / 20)) for t in range(21)]
    d.line([at(*p) for p in pts], fill='#14213D', width=max(1, int(3 * u)), joint='curve')
    lid_y = 78 - opening * 40
    lid = Image.new('RGBA', (int(170 * u), int(40 * u)), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lid)
    ld.rounded_rectangle((0, 0, 160 * u, 32 * u), radius=8 * u, fill='#F1BA86')
    ld.rectangle((68 * u, 0, 92 * u, 32 * u), fill='#FFF4C7')
    if opening:
        lid = lid.rotate(opening * 12, expand=True, resample=Image.BICUBIC)
    layer.paste(lid, (int(ox + 40 * u), int(oy + lid_y * u)), lid)
    im.paste(Image.alpha_composite(im.convert('RGBA'), layer).convert('RGB'), (0, 0))

# --- The launcher mark, ported from res/drawable/ic_parcel.xml -------------
def quad(p0, c, p1, t):
    """Point at `t` along a quadratic Bezier, for paths ported from vector drawables."""
    return ((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t ** 2 * p1[0],
            (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t ** 2 * p1[1])

def parcel_mark(im, cx, cy, scale):
    u = scale * SS / 108.0
    ox, oy = cx * SS - 54 * u, cy * SS - 54 * u
    def at(x, y): return (ox + x * u, oy + y * u)
    d = ImageDraw.Draw(im)
    handle = [(43, 42), (43, 34)]
    handle += [quad((43, 34), (43, 23), (54, 23), t / 12) for t in range(13)]
    handle += [quad((54, 23), (65, 23), (65, 34), t / 12) for t in range(13)]
    handle += [(65, 34), (65, 42)]
    d.line([at(*pt) for pt in handle], fill='#FFB766', width=int(5 * u), joint='curve')
    d.polygon([at(32, 39), at(76, 39), at(80, 80), at(28, 80)], fill='#F7F6F2')
    for ex in (44, 64):
        d.ellipse([at(ex - 3, 52), at(ex + 3, 58)], fill=TEAL)
    d.ellipse([at(35, 59), at(45, 65)], fill='#FFDCE7')
    d.ellipse([at(63, 59), at(73, 65)], fill='#FFDCE7')
    pts = [(47 + 14 * t / 20, 65 + 9 * 2 * (t / 20) * (1 - t / 20)) for t in range(21)]
    d.line([at(*p) for p in pts], fill=TEAL, width=max(1, int(2.5 * u)), joint='curve')

def product_card(im, key, x, y, w, h, radius=38):
    """Paste a bundled product illustration into a rounded brand card."""
    art = Image.open(PRODUCTS / f'{key}.png').convert('RGB')
    art = art.resize((w * SS, h * SS), Image.LANCZOS)
    mask = Image.new('L', art.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, art.size[0], art.size[1]), radius=radius * SS, fill=255)
    im.paste(art, (x * SS, y * SS), mask)

# --- Store graphics ---------------------------------------------------------
def feature_graphic():
    im = canvas(1024, 500)
    glow(im, 120, 420, 220, PEACH, 110)
    glow(im, 900, 90, 240, LAVENDER, 90)
    d = ImageDraw.Draw(im)
    parcel_friend(im, 210, 250, 300)
    d.text((400 * SS, 150 * SS), 'Parcel', font=font(700, 86), fill=IVORY)
    d.text((404 * SS, 254 * SS), 'All the joy. None of the spending.', font=font(500, 32), fill=ORANGE)
    d.text((404 * SS, 304 * SS), 'A virtual shopping simulator', font=font(400, 28), fill=MUTED)
    save(im, 'feature-graphic.png', (1024, 500))

def icon(name, background):
    im = canvas(512, 512, background)
    parcel_mark(im, 256, 262, 460)
    save(im, name, (512, 512))

if __name__ == '__main__':
    print(f'Writing Play Store artwork to {OUT}')
    icon('icon-512-teal.png', TEAL)
    icon('icon-512-navy.png', NAVY)
    feature_graphic()
    print('Done. Screenshots come from tools/capture_play_screenshots.py.')
