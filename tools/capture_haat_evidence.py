"""Capture current Parcel UI on a development emulator; adds one simulated cart item.
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
    all_nodes=nodes()
    all_nodes.sort(key=lambda n: 0 if n.get('text','')==text else 1)
    for node in all_nodes:
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
run('shell','am','start','-W','-n','com.parcel.simulator/com.parcel.android.MainActivity')
accept=find('I understand · Explore Parcel')
if accept is not None: tap(accept)
from simulator_login import login_if_needed
login_if_needed(run, find, click)
shot('haat-home.png')
run('shell','input','swipe','540','1800','540','700','400')
shot('haat-home-sections.png')
click('Profile')
shot('haat-account.png')
click('Home')
# Returning home can restore scroll; move to the beginning before searching.
for _ in range(3): run('shell','input','swipe','540','650','540','1800','250')
click('Search your next favourite')
run('shell','input','text','headphones')
run('shell','input','keyevent','4')
click('Everyday headphones',scroll=True)
shot('haat-product.png')
click('Add to cart')
click('Got it')
run('shell','input','keyevent','4')
click('Cart (',prefix=True)
shot('haat-cart.png')
click('Checkout')
click('Continue to checkout',scroll=True)
shot('haat-checkout.png')
click('Instant checkout · no payment required',scroll=True)
for _ in range(6):
    check=next((n for n in nodes() if n.get('class')=='android.widget.CheckBox'),None)
    if check is not None:
        if check.get('checked')!='true': tap(check)
        break
    run('shell','input','swipe','540','1700','540','750','300')
else: raise AssertionError('Acknowledgement not found')
click('Place order')
shot('haat-tracking.png')
print('PASS: redesigned home, product, cart, checkout, tracking and account captured.')
