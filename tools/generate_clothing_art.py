"""Original code-drawn clothing artwork in Parcel's established illustration style.
Requires Pillow for regeneration only. No external images or runtime downloads.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFilter
from generate_clothing_catalog import PALETTE, ROOT
OUT=ROOT/'shared/presentation/src/commonMain/composeResources/files/products'
OUT.mkdir(parents=True,exist_ok=True)
def draw_garment(d,r,color):
    shape=r['shape'];typ=r['typeId'];outline='#374B50';seam='#F0E8DC'
    if shape=='pants':
        short=typ in {'shorts','training-shorts','lounge-shorts','culottes'}
        y=345 if short else 434
        if typ=='skirts':
            d.polygon([(242,103),(398,103),(456,421),(182,421)],fill=color)
            for x in range(235,430,36):d.line((320,130,x,403),fill=seam,width=2)
        else:
            d.polygon([(228,103),(410,103),(432,y),(338,y),(318,243),(298,y),(206,y)],fill=color)
            d.line((319,134,318,243),fill=outline,width=3)
            d.line((232,122,405,122),fill=seam,width=3)
            d.arc((223,124,288,199),0,90,fill=outline,width=3);d.arc((351,124,413,199),90,180,fill=outline,width=3)
            if typ=='cargo-pants':
                d.rounded_rectangle((214,239,273,297),radius=4,outline=seam,width=3)
                d.rounded_rectangle((369,239,423,297),radius=4,outline=seam,width=3)
        d.rounded_rectangle((239,102,400,124),radius=3,fill=color,outline=outline,width=2)
    elif shape in {'shawl','saree'}:
        d.polygon([(194,91),(400,74),(451,416),(235,443)],fill=color)
        for x in range(215,410,22):d.line((x,100,x+29,422),fill=seam,width=2)
        d.line((212,122,410,105),fill='#C3A46D',width=13)
        d.line((235,408,443,382),fill='#C3A46D',width=13)
        if shape=='saree':d.polygon([(345,80),(408,75),(432,418),(368,431)],fill='#D6B77D')
        for x in range(244,440,15):d.line((x,425,x+3,446),fill=outline,width=2)
    elif shape=='briefs':
        d.rounded_rectangle((195,176,447,210),radius=6,fill=outline)
        d.polygon([(197,208),(446,208),(393,347),(250,347)],fill=color)
        d.line((223,224,265,324),fill=seam,width=3);d.line((418,224,377,324),fill=seam,width=3)
        if typ in {'boxers','trunks'}:d.rectangle((209,207,431,345),fill=color);d.line((320,245,320,350),fill=outline,width=4)
    elif shape=='bra':
        d.line((244,131,220,267),fill=color,width=22);d.line((395,131,420,267),fill=color,width=22)
        d.ellipse((193,205,320,329),fill=color,outline=outline,width=2)
        d.ellipse((320,205,448,329),fill=color,outline=outline,width=2)
        d.rounded_rectangle((200,303,443,338),radius=9,fill=color)
        d.line((314,265,328,265),fill=outline,width=5)
    else:
        long= r['sleeve']!='Short sleeve'
        dress=shape=='dress'
        outfit=shape=='outfit'
        bottom=398 if dress else 321 if outfit else 414
        points=[(247,108),(282,91),(358,91),(393,108),(474,227 if long else 180),(420,253 if long else 214),(389,179),(407 if dress else 393,bottom),(233 if dress else 247,bottom),(251,179),(220,253 if long else 214),(166,227 if long else 180)]
        d.polygon(points,fill=color)
        if r['neckline']=='V-neck':d.polygon([(281,93),(320,147),(359,93)],fill='#EFEDE5')
        else:d.ellipse((282,76,358,124),fill='#EFEDE5')
        if r['neckline']=='Collared':
            d.polygon([(281,91),(317,125),(297,142),(270,111)],fill='#DFD5C3')
            d.polygon([(359,91),(323,125),(343,142),(370,111)],fill='#DFD5C3')
        if typ=='hoodies':
            d.arc((277,64,363,153),175,365,fill=outline,width=9)
            d.line((309,123,304,193),fill=seam,width=4);d.line((331,123,336,193),fill=seam,width=4)
            d.rounded_rectangle((276,270,364,323),radius=8,outline=outline,width=3)
        if typ in {'padded-jackets','fleece-jackets','sports-jackets','coats','cardigans','shirts','formal-shirts','overshirts','sherwanis','nehru-jackets','robes'}:
            d.line((320,126,320,bottom-10),fill=outline,width=3)
            for y in range(160,bottom-10,40):d.ellipse((315,y,325,y+10),fill='#E7D9B6')
        if typ=='padded-jackets':
            for y in range(171,bottom-10,34):d.line((251,y,390,y),fill=outline,width=2)
        if r['pattern']=='Striped':
            for y in range(162,bottom-10,25):d.line((252,y,388,y),fill=seam,width=3)
        d.line((249,bottom-10,392,bottom-10),fill=seam,width=2)
        if outfit:
            d.polygon([(260,333),(310,333),(300,443),(246,443)],fill=color)
            d.polygon([(331,333),(381,333),(394,443),(340,443)],fill=color)
            d.line((261,345,380,345),fill=seam,width=3)
        if typ in {'tank-tops','camisoles','vests','undershirts','slips','swimwear','sports-bras'}:
            d.rectangle((166,125,248,266),fill='#EDEDE5');d.rectangle((393,125,476,266),fill='#EDEDE5')
for r in json.loads((ROOT/'tools/clothing_products.json').read_text()):
    for i in range(8):
        frame=0 if i==0 else i-1
        bg=['#ECEEE7','#EFE8DF','#E5ECEA'][frame%3]
        im=Image.new('RGB',(640,512),bg)
        layer=Image.new('RGBA',im.size);d=ImageDraw.Draw(layer)
        d.ellipse((196,427,449,458),fill=(39,51,45,30));layer=layer.filter(ImageFilter.GaussianBlur(12));im.paste(layer,(0,0),layer)
        d=ImageDraw.Draw(im)
        if frame in (1,4):d.ellipse((-100,-150,480,370),fill='#F8F8F3')
        draw_garment(d,r,PALETTE[r['colors'][frame%len(r['colors'])]])
        if frame in (1,4):im=im.rotate(5 if frame==1 else -5,resample=Image.Resampling.BICUBIC,fillcolor=bg)
        if frame in (3,5):im=im.crop((180,85,461,350) if frame==3 else (215,220,420,420)).resize((640,512),Image.Resampling.LANCZOS)
        path=r['id'] if i==0 else f"{r['id']}_{frame}"
        im.save(OUT/f'{path}.png',optimize=True)
print('Generated offline clothing cards and seven gallery compositions per product.')
