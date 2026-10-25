"""Generate the shared offline clothing catalog and sandbox pricing fixture.
The checked-in taxonomy is the source for type availability; IDs stay stable.
Run before generate_clothing_art.py when changing the taxonomy.
"""
import json
import hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
GROUPS=json.loads((ROOT/'tools/clothing_taxonomy.json').read_text())
PALETTE={'Navy':'#344C66','Olive':'#66765B','Sand':'#C6AC87','Slate':'#76828B','Burgundy':'#855368','Teal':'#40817D','Ivory':'#E4DAC6','Rose':'#C78D8E'}
BOTTOMS={'jeans','trousers','chinos','cargo-pants','shorts','track-pants','joggers','jeggings','leggings','palazzos','culottes','skirts','training-shorts','pyjamas','lounge-pants','lounge-shorts','churidars','thermal-bottoms','workout-leggings'}
ONE_SIZE={'sarees','dupattas','shawls','dhotis'}
TITLES={'shirts':'Oxford casual shirt','formal-shirts':'Easy-iron formal shirt','tshirts':'Everyday crew-neck T-shirt','polos':'Pique polo shirt','jeans':'Straight-fit jeans','pyjamas':'Cotton sleep pyjamas','hoodies':'Everyday fleece hoodie','padded-jackets':'Quilted puffer jacket','sweaters':'Textured knit sweater','coats':'Tailored wool-blend coat','dresses':'Everyday midi dress','kurtas':'Textured everyday kurta','sarees':'Woven drape saree','sports-jackets':'Warm-up sports jacket','thermal-tops':'Warm-layer thermal top','thermal-bottoms':'Warm-layer thermal bottoms','thermal-sets':'Warm-layer thermal set'}

def catalog():
 rows=[]
 for group,types in GROUPS.items():
  for type_id,label,depts in types:
   for code,department in [('M','Men'),('W','Women')]:
    if code not in depts: continue
    i=int.from_bytes(hashlib.sha256(f'{department.lower()}-{type_id}'.encode()).digest()[:4], 'big')
    colors=list(PALETTE)[i%8:]+list(PALETTE)[:i%8];colors=colors[:3]
    winter=group=='Winterwear' or type_id in {'overshirts','sports-jackets','tracksuits'}
    fabric='Cotton'
    if group=='Winterwear':fabric='Wool blend' if type_id in {'sweaters','cardigans','coats','shawls','shrugs'} else 'Cotton fleece'
    if type_id in {'padded-jackets','sports-jackets','tracksuits'}:fabric='Polyester'
    if type_id=='fleece-jackets':fabric='Fleece'
    if type_id in {'jeans','jeggings'}:fabric='Denim'
    if group=='Activewear':fabric='Polyester blend' if type_id!='swimwear' else 'Nylon blend'
    if group=='Ethnic':fabric='Cotton blend' if i%2==0 else 'Viscose'
    if type_id in {'bras','sports-bras','shapewear','leggings','workout-leggings'}:fabric='Stretch cotton'
    sizes=['XS','S','M','L','XL','XXL']
    if type_id in ONE_SIZE:sizes=['One size']
    elif type_id in {'jeans','trousers','chinos','cargo-pants'}:sizes=['28','30','32','34','36'] if code=='M' else ['26','28','30','32','34']
    elif type_id=='bras':sizes=['32B','34B','36B','36C','38C']
    fit='Relaxed' if type_id in {'hoodies','overshirts','pyjamas','palazzos','lounge-pants','lounge-sets','robes'} else 'Regular'
    if type_id in {'jeans','trousers'}:fit='Straight'
    if type_id in {'leggings','jeggings','thermal-tops','thermal-bottoms','shapewear'}:fit='Slim'
    pattern='Striped' if type_id in {'shirts','night-suits','pyjamas','nightshirts'} else 'Woven' if type_id in {'sarees','shawls','dupattas'} else 'Solid'
    occasion='Festive' if group=='Ethnic' else 'Workout' if group=='Activewear' else 'Home' if group in {'Sleepwear','Innerwear'} else 'Work' if type_id in {'formal-shirts','suits','trousers','blouses'} else 'Casual'
    season='Winter' if winter else 'Summer' if type_id in {'shorts','tshirts','tank-tops','crop-tops','playsuits','training-shorts','lounge-shorts'} else 'All season'
    sleeve=None if type_id in BOTTOMS or group=='Innerwear' or type_id in {'sarees','dupattas','shawls'} else 'Sleeveless' if type_id in {'tank-tops','camisoles','sports-bras','slips','swimwear'} else 'Short sleeve' if type_id in {'tshirts','polos','workout-tshirts','workout-tops','crop-tops','tops','saree-blouses'} else 'Long sleeve'
    neckline=None if sleeve is None else 'Hooded' if type_id=='hoodies' else 'Collared' if type_id in {'shirts','formal-shirts','overshirts','polos','coats','nightshirts'} else 'V-neck' if type_id in {'cardigans','kurtas','kurtis','dresses'} else 'Crew neck'
    rise='High rise' if code=='W' and type_id in BOTTOMS else 'Mid rise' if type_id in BOTTOMS else None
    price=(799+(i%8)*200)*100
    if type_id in {'suits','sherwanis','lehengas','coats'}:price=499900
    elif type_id in {'padded-jackets','sarees','salwar-suits','kurta-sets'}:price=249900
    elif group=='Innerwear':price=49900+(i%3)*10000
    title=TITLES.get(type_id, label if label.endswith('sets') else label.rstrip('s'))
    title=("Men's " if code=='M' else "Women's ")+title[0].lower()+title[1:]
    shape='pants' if type_id in BOTTOMS else 'shawl' if type_id in {'shawls','dupattas'} else 'saree' if type_id=='sarees' else 'briefs' if type_id in {'briefs','trunks','boxers','shapewear'} else 'bra' if type_id in {'bras','sports-bras'} else 'dress' if type_id in {'dresses','nightdresses','slips','lehengas','kurtas','kurtis','salwar-suits','kurta-sets','sherwanis','robes'} else 'outfit' if group=='Outfits' or type_id in {'thermal-sets','night-suits','lounge-sets','kurta-pyjama-sets'} else 'shirt'
    rows.append(dict(id=f'{department.lower()}-{type_id}',department=department,typeId=type_id,typeLabel=label,group=group,title=title,colors=colors,sizes=sizes,fabric=fabric,fit=fit,pattern=pattern,occasion=occasion,season=season,winterWear=winter,sleeve=sleeve,neckline=neckline,rise=rise,price=price,shape=shape))
 return rows

def generate():
 rows=catalog()
 # Regenerate the shared type list as well: adding a JSON type cannot drift the app taxonomy.
 domain_path=ROOT/'shared/domain/src/commonMain/kotlin/com/parcel/domain/Clothing.kt'
 domain=domain_path.read_text()
 entries=[]
 for group,types in GROUPS.items():
  for id,label,depts in types:
   departments=', '.join('ClothingDepartment.'+d for code,d in [('M','Men'),('W','Women')] if code in depts)
   entries.append(f'    ClothingType("{id}", "{label}", ClothingGroup.{group}, setOf({departments})),')
 start=domain.index('val clothingTypes = listOf(')
 end=domain.index('\n)\n', start)+3
 domain_path.write_text(domain[:start]+'val clothingTypes = listOf(\n'+'\n'.join(entries)+'\n)'+domain[end:])
 (ROOT/'tools/clothing_products.json').write_text(json.dumps(rows,indent=2)+'\n')
 def kt(s):return json.dumps(s,ensure_ascii=False)
 def kl(items):return 'listOf('+', '.join(kt(s) for s in items)+')'
 lines=['package com.parcel.data','', 'import com.parcel.domain.*','', '// Generated by tools/generate_clothing_catalog.py. Stable IDs; no runtime network.', 'val clothingProducts: List<Product> = listOf(']
 for r in rows:
  desc=f"{r['fabric']} {r['typeLabel'].lower()} in a {r['fit'].lower()} fit. {r['pattern']} finish, designed for {r['occasion'].lower()} wear. Choose your colour and size below."
  attrs=f"ClothingDetails(ClothingDepartment.{r['department']}, {kt(r['typeId'])}, {kl(r['colors'])}, {kl(r['sizes'])}, {kt(r['fabric'])}, {kt(r['fit'])}, {kt(r['pattern'])}, {kt(r['occasion'])}, {kt(r['season'])}, {str(r['winterWear']).lower()}, {kt(r['sleeve']) if r['sleeve'] else 'null'}, {kt(r['neckline']) if r['neckline'] else 'null'}, {kt(r['rise']) if r['rise'] else 'null'})"
  variants=[f'{color} / {size}' for color in r['colors'] for size in r['sizes']]
  lines.append(f'''    Product({kt(r['id'])}, {kt(r['title'])}, {kt(r['fabric']+' · '+r['fit']+' fit')}, Category.Clothing,
        Money({r['price']}L), Money({r['price']+50000}L), {kt(r['id'])}, {kt(desc)},
        {kl(variants)}, listOf(Review("Aarav", 5, "Comfortable fabric and a thoughtful fit."), Review("Mira", 4, "Easy to style with everyday favourites.")), clothing = {attrs}),''')
 lines+=[')'];(ROOT/'shared/data/src/commonMain/kotlin/com/parcel/data/ClothingCatalog.kt').write_text('\n'.join(lines)+'\n')
 fixture_path=ROOT/'tools/payu_catalog.json';fixture=json.loads(fixture_path.read_text());fixture={k:v for k,v in fixture.items() if not k.startswith(('men-','women-'))}
 for r in rows:fixture[r['id']]=dict(title=r['title'],price=r['price'],variants=[f'{color} / {size}' for color in r['colors'] for size in r['sizes']])
 fixture_path.write_text(json.dumps(fixture,indent=2)+'\n')
 print(f'Generated {len(rows)} clothing products across {len(GROUPS)} groups.')
if __name__=='__main__':generate()
