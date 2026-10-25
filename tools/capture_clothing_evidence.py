"""Capture clothing UI on a development emulator; does not place orders or add cart items.
Run on a fresh development installation after device tests have completed.
"""
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = [str(Path.home()/'Library/Android/sdk/platform-tools/adb'), '-s', 'emulator-5554']
OUT = Path(__file__).resolve().parents[1]/'docs/screenshots'
OUT.mkdir(exist_ok=True)
def run(*args): return subprocess.check_output(ADB+list(args),text=True)
def nodes():
    run('shell','uiautomator','dump','/sdcard/parcel-evidence.xml')
    return list(ET.fromstring(run('shell','cat','/sdcard/parcel-evidence.xml')).iter('node'))
def find(text, prefix=False):
    for node in nodes():
        values=[node.get('text',''),node.get('content-desc','')]
        if any((v.startswith(text) if prefix else v==text) for v in values):
            bounds=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
            if len(bounds)==4 and bounds[2]>bounds[0] and bounds[3]>bounds[1]: return node
    return None
def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
    run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def click(text, prefix=False, scroll=False):
    for _ in range(9 if scroll else 2):
        node=find(text,prefix)
        if node is not None: tap(node); return
        if scroll: run('shell','input','swipe','540','1850','540','700','350')
        else: time.sleep(.5)
    raise AssertionError('Missing UI: '+text)
def shot(name):
    nodes() # Wait for the UI hierarchy to settle before capturing.
    (OUT/name).write_bytes(subprocess.check_output(ADB+['exec-out','screencap','-p']))
run('shell','am','force-stop','com.parcel.simulator')
run('shell','am','start','-W','-n','com.parcel.simulator/com.parcel.android.MainActivity')
accept=find('I understand · Explore Parcel')
if accept is not None: tap(accept)
from simulator_login import login_if_needed
login_if_needed(run, find, click)
click('Clothing')
shot('clothing-men.png')
click('Winterwear')
click('Hoodies')
shot('clothing-winter.png')
import json
hoodie=next(p for p in json.loads(Path(__file__).with_name('clothing_products.json').read_text()) if p['id']=='men-hoodies')
click(hoodie['title'],scroll=True)
click('M',scroll=True)
shot('clothing-sizes.png')
run('shell','input','keyevent','4')
for _ in range(3): run('shell','input','swipe','540','650','540','1900','300')
click("Women's")
shot('clothing-women.png')
click('All types')
shot('clothing-types.png')
run('shell','input','keyevent','4')
click('Filters')
shot('clothing-filters.png')
run('shell','input','keyevent','4')
print('PASS: clothing screens captured; app left in Women’s clothing.')
