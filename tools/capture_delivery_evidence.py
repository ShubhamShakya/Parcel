"""Capture delivery and unboxing on a development emulator; places one local simulated order.
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
run('shell','settings','put','global','animator_duration_scale','1')
run('shell','am','force-stop','com.parcel.simulator')
run('shell','am','start','-W','-n','com.parcel.simulator/com.parcel.android.MainActivity')
accept=find('I understand · Explore Parcel')
if accept is not None: tap(accept)
from simulator_login import login_if_needed
login_if_needed(run, find, click)
shot('playful-home.png')
click('Everyday headphones',scroll=True)
click('Add to cart',scroll=True)
click('Got it')
run('shell','input','keyevent','4')
click('Cart (',prefix=True)
click('Checkout')
click('Continue to checkout',scroll=True)
click('Instant checkout · no payment required',scroll=True)
for _ in range(5):
    check=next((n for n in nodes() if n.get('class')=='android.widget.CheckBox'),None)
    if check is not None:
        if check.get('checked')!='true': tap(check)
        break
    run('shell','input','swipe','540','1800','540','900','300')
else: raise AssertionError('Checkout acknowledgement not found')
click('Place order',scroll=True)
shot('order-tracking.png')
click('Your delivery OTP',scroll=True)
shot('delivery-otp.png')
click('Deliver it instantly ✨',scroll=True)
click('Unbox your parcel ✨',scroll=True)
shot('unboxing-ready.png')
button=find('Open my parcel')
assert button is not None
recorder=subprocess.Popen(ADB+['shell','screenrecord','--time-limit','7','/sdcard/parcel-unboxing.mp4'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
time.sleep(.4)
tap(button)
time.sleep(2)
shot('unboxing-open.png')
recorder.wait(timeout=15)
(OUT/'unboxing.mp4').write_bytes(subprocess.check_output(ADB+['exec-out','cat','/sdcard/parcel-unboxing.mp4']))
click('Give them a home on your shelf',scroll=True)
click('Space 1')
shot('shelf-picker.png')
click('Everyday headphones')
shot('purchased-shelf.png')
print('PASS: local order tracked, instantly delivered, unboxed and placed on shelf; screenshots and unboxing video captured.')
