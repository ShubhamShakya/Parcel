"""ADB smoke check for Parcel's onboarding and wishlist after actual process death.
Run only on a development emulator; this saves the sample headphones to its wishlist.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--serial', default='emulator-5554')
args = parser.parse_args()
sdk = os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk'))
adb = [str(Path(sdk) / 'platform-tools/adb'), '-s', args.serial]
component = 'com.parcel.simulator/com.parcel.android.MainActivity'

def call(*command):
    return subprocess.check_output(adb + list(command), text=True)

def nodes():
    call('shell', 'uiautomator', 'dump', '/sdcard/parcel-smoke.xml')
    return list(ET.fromstring(call('shell', 'cat', '/sdcard/parcel-smoke.xml')).iter('node'))

def find(label):
    for node in nodes():
        if label in (node.get('text'), node.get('content-desc')):
            return node
    return None

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    call('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def screenshot(name):
    folder = Path(__file__).resolve().parents[1] / 'docs/screenshots'
    folder.mkdir(parents=True, exist_ok=True)
    (folder / name).write_bytes(subprocess.check_output(adb + ['exec-out', 'screencap', '-p']))

call('shell', 'am', 'start', '-W', '-n', component)
accept = find('I understand · Explore Parcel')
if accept is not None:
    screenshot('onboarding.png')
    tap(accept)
    time.sleep(1)
def click(label, scroll=False):
    for _ in range(8 if scroll else 2):
        node = find(label)
        if node is not None:
            tap(node)
            return
        if scroll:
            call('shell', 'input', 'swipe', '540', '1800', '540', '700', '300')
        else:
            time.sleep(.5)
    raise AssertionError('Missing UI: ' + label)

from simulator_login import login_if_needed
login_if_needed(call, find, click)
screenshot('home.png')
heart = find('Save Everyday headphones')
if heart is not None:
    tap(heart)
else:
    assert find('Remove Everyday headphones from wishlist') is not None, 'Expected sample headphones on home'
time.sleep(0.5)
# The process is killed, not just the Activity recreated.
call('shell', 'am', 'force-stop', 'com.parcel.simulator')
call('shell', 'am', 'start', '-W', '-n', component)
assert find('I understand · Explore Parcel') is None, 'Onboarding acceptance did not persist'
wishlist = find('Wishlist')
assert wishlist is not None, 'Wishlist tab missing after restart'
tap(wishlist)
assert find('Everyday headphones') is not None, 'Saved product did not survive process death'
screenshot('wishlist-after-restart.png')
discover = find('Discover')
assert discover is not None
tap(discover)
screenshot('home.png')
print('PASS: disclosure acceptance and wishlist survive an actual force-stop/relaunch.')
