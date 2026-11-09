"""Original Play Store listing artwork. Development dependency: Pillow.

Illustrative brand art only: the parcel friend, bundled product illustrations and
brand shapes. No application UI is depicted, so the images stay valid when screens
change. Regenerate after a brand or palette change with:

    python3 tools/generate_play_store_art.py
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
PANELS = [
    dict(key='01-hero', cy=1080, head='All the joy.\nNone of the spending.', sub='A shopping simulator for Android',
         tint=PEACH, art='friend'),
    dict(key='02-browse', cy=1070, head='Browse\nhandpicked finds', sub='Electronics, home and over 100 clothing pieces',
         tint=LAVENDER, art='trio'),
    dict(key='03-wishlist', cy=1120, head='Save every\nfavourite', sub='A wishlist that waits for you, offline',
         tint=PINK, art='heart'),
    dict(key='04-offers', cy=980, head='Offers that\nactually apply', sub='Coupons, taxes and totals, all simulated',
         tint=PEACH, art='tag'),
    dict(key='05-delivery', cy=1165, head='Follow every\ndelivery', sub='Five stages in four hours, or arrive instantly',
         tint=MINT, art='route'),
    dict(key='06-unbox', cy=1080, head='Open your\nparcel', sub='Lids lift. Confetti flies.',
         tint=PEACH, art='unbox'),
    dict(key='07-shelf', cy=1245, head='A shelf for\nyour finds', sub='Twelve spaces. Collect every one.',
         tint=LAVENDER, art='shelf'),
    dict(key='08-simulated', cy=915, head='No real money.\nEver.', sub='Products, prices and orders are simulated',
         tint=MINT, art='badge'),
]

def draw_art(im, kind, cy):
    d = ImageDraw.Draw(im)
    if kind == 'friend':
        parcel_friend(im, 540, cy, 780)
    elif kind == 'trio':
        product_card(im, 'headphones', 80, cy - 400, 460, 368)
        product_card(im, 'backpack', 540, cy - 230, 460, 368)
        product_card(im, 'lamp', 310, cy + 30, 460, 368)
    elif kind == 'heart':
        parcel_friend(im, 540, cy + 60, 560)
        heart = Image.new('RGBA', im.size, (0, 0, 0, 0))
        hd = ImageDraw.Draw(heart)
        cx, hy, r = 540 * SS, (cy - 300) * SS, 78 * SS
        hd.ellipse((cx - r * 1.05, hy - r * .7, cx + r * .05, hy + r * .45), fill='#EE91AA')
        hd.ellipse((cx - r * .05, hy - r * .7, cx + r * 1.05, hy + r * .45), fill='#EE91AA')
        hd.polygon([(cx - r, hy + r * .05), (cx + r, hy + r * .05), (cx, hy + r * 1.25)], fill='#EE91AA')
        im.paste(Image.alpha_composite(im.convert('RGBA'), heart).convert('RGB'), (0, 0))
    elif kind == 'tag':
        d.rounded_rectangle((250 * SS, (cy - 200) * SS, 830 * SS, (cy + 130) * SS), radius=46 * SS, fill=ORANGE)
        centred(d, '15%', font(700, 132), NAVY, 540, cy - 160)
        centred(d, 'OFF', font(700, 54), NAVY, 540, cy + 10)
        parcel_friend(im, 540, cy + 330, 400)
    elif kind == 'route':
        stops = [(140, cy + 210), (330, cy + 70), (540, cy + 150), (730, cy + 10), (900, cy - 120)]
        for i in range(len(stops) - 1):
            d.line((stops[i][0] * SS, stops[i][1] * SS, stops[i + 1][0] * SS, stops[i + 1][1] * SS),
                   fill='#2E3A57', width=14 * SS)
        for i, (sx, sy) in enumerate(stops):
            done = i < len(stops) - 1
            d.ellipse(((sx - 26) * SS, (sy - 26) * SS, (sx + 26) * SS, (sy + 26) * SS),
                      fill=ORANGE if done else NAVY, outline=ORANGE, width=6 * SS)
        parcel_friend(im, 880, cy - 330, 380)
    elif kind == 'unbox':
        parcel_friend(im, 540, cy + 20, 760, opening=1.0, confetti=True)
    elif kind == 'shelf':
        keys = ['headphones', 'backpack', 'lamp', 'mug', 'tote', 'women-dresses']
        for row in range(2):
            d.rounded_rectangle((110 * SS, (cy - 200 + row * 260) * SS, 970 * SS, (cy - 190 + row * 260) * SS),
                                radius=6 * SS, fill='#49371F')
            for col in range(3):
                key = keys[row * 3 + col]
                if (PRODUCTS / f'{key}.png').exists():
                    product_card(im, key, 130 + col * 290, cy - 390 + row * 260, 250, 190, radius=26)
    elif kind == 'badge':
        d.ellipse((330 * SS, (cy - 210) * SS, 750 * SS, (cy + 210) * SS), outline=ORANGE, width=10 * SS)
        centred(d, '₹0', font(700, 150), ORANGE, 540, cy - 120)
        centred(d, 'spent', font(500, 46), IVORY, 540, cy + 60)
        parcel_friend(im, 540, cy + 370, 400)

def screenshot(panel, w=1080, h=1920):
    im = canvas(w, h)
    glow(im, 180, 300, 300, panel['tint'], 90)
    glow(im, 940, 1500, 340, panel['tint'], 70)
    d = ImageDraw.Draw(im)
    y = centred(d, panel['head'], font(700, 78), IVORY, 540, 150)
    centred(d, panel['sub'], font(400, 38), MUTED, 540, y + 20)
    draw_art(im, panel['art'], panel['cy'])
    d.rounded_rectangle((360 * SS, 1740 * SS, 720 * SS, 1830 * SS), radius=45 * SS, fill=SURFACE)
    parcel_mark(im, 420, 1785, 70)
    centred(d, 'Parcel', font(600, 38), IVORY, 565, 1762)
    save(im, f'phone-{panel["key"]}.png', (w, h))

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
    for panel in PANELS:
        screenshot(panel)
    print(f'Done: {len(list(OUT.glob("*.png")))} files.')
