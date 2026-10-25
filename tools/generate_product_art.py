"""Original bundled product illustrations. Development dependency: Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
OUT = Path(__file__).resolve().parents[1] / 'shared/presentation/src/commonMain/composeResources/files/products'
OUT.mkdir(parents=True, exist_ok=True)
S=2
def art(name, background, draw):
 im=Image.new('RGB',(640,512),background)
 shadow=Image.new('RGBA',im.size);d=ImageDraw.Draw(shadow)
 d.ellipse((155,413,485,450),fill=(40,45,35,26));shadow=shadow.filter(ImageFilter.GaussianBlur(16))
 im.paste(shadow,(0,0),shadow);draw(ImageDraw.Draw(im));im.save(OUT/f'{name}.png')
def headphones(d):
 d.arc((185,80,455,355),180,360,fill='#263E41',width=32)
 d.arc((203,98,437,355),180,360,fill='#516568',width=9)
 for x in (174,403):
  d.rounded_rectangle((x,227,x+66,383),radius=27,fill='#1E3337')
  d.rounded_rectangle((x+10,242,x+52,369),radius=19,fill='#526B6C')
  d.line((x+22,265,x+22,341),fill='#8A9D98',width=3)
def tote(d):
 d.rounded_rectangle((206,81,291,253),radius=38,outline='#A78151',width=15)
 d.rounded_rectangle((348,81,433,253),radius=38,outline='#A78151',width=15)
 d.polygon([(180,192),(460,192),(482,423),(158,423)],fill='#D8BB90')
 d.line((192,204,448,204),fill='#B69A70',width=4)
 d.rounded_rectangle((274,266,366,339),radius=4,fill='#F4E4C7')
 d.text((290,289),'PARCEL',fill='#7B6851',stroke_width=0)
def mug(d):
 d.ellipse((380,187,506,340),outline='#B96E50',width=29)
 d.rounded_rectangle((172,166,413,399),radius=55,fill='#C98262')
 d.ellipse((172,141,413,203),fill='#DDAB8C')
 d.ellipse((189,153,396,189),fill='#794C39')
 d.arc((253,61,294,132),260,90,fill='#CFB6A0',width=5)
 d.arc((319,49,360,124),260,90,fill='#CFB6A0',width=5)
 d.line((201,230,201,341),fill='#D99475',width=8)
def backpack(d):
 d.rounded_rectangle((280,67,360,147),radius=27,outline='#3C5D4A',width=14)
 d.rounded_rectangle((199,106,444,426),radius=69,fill='#557D62')
 d.rounded_rectangle((220,236,423,401),radius=30,fill='#3C604B')
 d.line((238,267,404,267),fill='#D4B982',width=6)
 d.line((394,267,394,290),fill='#D4B982',width=6)
 d.rounded_rectangle((276,157,365,202),radius=7,fill='#E8D7AD')
 d.line((305,178,337,178),fill='#6E7B5C',width=4)
def lamp(d):
 d.ellipse((220,385,448,434),fill='#AE8543')
 d.rounded_rectangle((320,211,336,409),radius=7,fill='#B8893D')
 d.line((329,236,270,137),fill='#B8893D',width=16)
 d.polygon([(234,105),(302,111),(381,232),(183,232)],fill='#D7AB59')
 d.ellipse((183,212,381,245),fill='#FBE7AB')
 d.ellipse((243,226,323,240),fill='#FFF5CE')
def speaker(d):
 d.rounded_rectangle((176,151,465,402),radius=64,fill='#53626D')
 for y in range(177,378,9):
  for x in range(198,444,9):
   if not ((x<216 or x>425) and (y<193 or y>359)): d.ellipse((x,y,x+2,y+2),fill='#94A1A6')
 d.rounded_rectangle((253,130,389,159),radius=10,fill='#364954')
 for x in (276,320,364): d.ellipse((x,137,x+7,144),fill='#C6D2D1')
 d.rounded_rectangle((282,281,363,316),radius=7,fill='#D0BA91')
 d.text((297,292),'P A R C E L',fill='#48525A')
for args in [('headphones','#E6EEEB',headphones),('tote','#F0E9DD',tote),('mug','#F3E6DC',mug),('backpack','#E5ECE2',backpack),('lamp','#F3EDDE',lamp),('speaker','#E7EAED',speaker)]: art(*args)

# Seven original, locally rendered gallery compositions per item. These are sample
# illustrations, not photographs. Each frame is drawn from the primitives above.
class GalleryDraw:
 def __init__(self, drawing, index): self.drawing, self.index = drawing, index
 def __getattr__(self, name):
  fn=getattr(self.drawing,name)
  def render(*args, **kwargs):
   # Alternate finish palettes without depending on external or previously rendered images.
   palettes = [('#263E41','#173E50'),('#557D62','#70846A'),('#C98262','#A8785B'),('#D8BB90','#C6AE8C'),('#D7AB59','#BEB893'),('#53626D','#6F827C')]
   if self.index in (2,4,6):
    for field in ('fill','outline'):
     if field in kwargs:
      for old,new in palettes:
       if kwargs[field]==old: kwargs[field]=new
   return fn(*args,**kwargs)
  return render

products=[('headphones','#E6EEEB',headphones),('tote','#F0E9DD',tote),('mug','#F3E6DC',mug),('backpack','#E5ECE2',backpack),('lamp','#F3EDDE',lamp),('speaker','#E7EAED',speaker)]
for name,bg,draw in products:
 for i in range(7):
  im=Image.new('RGB',(640,512),bg)
  d=ImageDraw.Draw(im)
  if i in (1,3,5):
   d.ellipse((-50,-140,450,380),fill='#FFFFFF')
   d.ellipse((410,300,790,680),fill='#DCE5DB')
  else:
   d.rounded_rectangle((40+i*5,35,600,470),radius=40,outline='#FFFFFF',width=3)
  d.ellipse((155,423,485,450),fill='#D0D7CF')
  draw(GalleryDraw(d,i))
  if i in (1,4):
   # A slight angled view, rendered from this frame's own drawing.
   im=im.rotate(7 if i==1 else -7,resample=Image.Resampling.BICUBIC,fillcolor=bg)
  if i in (3,5):
   box=(125,100,525,420) if i==3 else (160,180,480,436)
   im=im.crop(box).resize((640,512),Image.Resampling.LANCZOS)
  d=ImageDraw.Draw(im)
  im.save(OUT/f'{name}_{i}.png')
