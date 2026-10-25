"""Capture phone login and registration on a development emulator using a fictional profile.
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
shot('phone-login.png')
click('🇮🇳 India (+91)  ▾')
shot('country-picker.png')
run('shell','input','keyevent','4')
click('Phone number')
run('shell','input','text','9'+str(int(time.time()))[-9:])
run('shell','input','keyevent','4')
click('Continue with phone',scroll=True)
shot('otp-countdown.png')
for _ in range(15):
    resend=find('Resend OTP')
    if resend is not None: break
    time.sleep(1)
else: raise AssertionError('Resend did not appear after countdown')
shot('otp-resend.png')
tap(resend)
click('4-character verification code')
run('shell','input','text','1234')
run('shell','input','keyevent','4')
click('Verify & continue',scroll=True)
shot('phone-registration.png')
click('First name')
run('shell','input','text','Mira')
click('Last name')
run('shell','input','text','Shah')
run('shell','input','keyevent','4')
click('Create my profile',scroll=True)
click('Profile')
shot('phone-profile.png')
click('Log out',scroll=True)
shot('logout-confirmation.png')
click('Log out')
print('PASS: phone registration, animated countdown, resend and profile captured; app left at phone login.')
