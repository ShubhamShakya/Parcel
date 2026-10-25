"""Loopback-only PayU sandbox reference service (Python standard library).
Never use for live payments or expose this development server directly to the internet.
See docs/PAYU_SANDBOX.md. Merchant salt is read from the environment, never the client.
"""
from decimal import Decimal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import time
from urllib.parse import parse_qs, urlencode
from urllib.request import Request, urlopen

CATALOG = json.loads(Path(__file__).with_name('payu_catalog.json').read_text())
OFFERS = {
 'EVERYDAY10': (49900, 10, 0, 50000, False),
 'BIGFINDS15': (199900, 15, 0, 75000, False),
 'HELLO200': (149900, 0, 20000, 20000, False),
 'SMALLJOY100': (99900, 0, 10000, 10000, False),
 'FEEFREE': (79900, 0, 0, 0, True),
}

def price(lines, offer):
    if not 1 <= len(lines) <= sum(len(p["variants"]) for p in CATALOG.values()):
        raise ValueError('Invalid cart size')
    seen = set()
    subtotal = 0
    for line in lines:
        p = CATALOG.get(line['productId'])
        key = (line['productId'], line['variant'])
        if not p or key in seen or line['variant'] not in p['variants'] or type(line['quantity']) is not int or not 1 <= line['quantity'] <= 10:
            raise ValueError('Invalid product, variant or quantity')
        if line['title'] != p['title'] or line['unitPrice'] != {'minorUnits': p['price'], 'currency': 'INR'}:
            raise ValueError('Catalog snapshot mismatch')
        seen.add(key)
        subtotal += p['price'] * line['quantity']
    if offer is not None and offer not in OFFERS:
        raise ValueError('Unknown offer')
    rule = OFFERS.get(offer)
    eligible = rule is not None and subtotal >= rule[0]
    discount = min(subtotal, rule[3], (subtotal*rule[1]+50)//100 if rule[1] else rule[2]) if eligible else 0
    tax = ((subtotal-discount)*18+50)//100
    fee = 0 if eligible and rule[4] else 2900
    def money(n): return {'minorUnits': n, 'currency': 'INR'}
    return dict(subtotal=money(subtotal), discount=money(discount), tax=money(tax), convenienceFee=money(fee),
                total=money(subtotal-discount+tax+fee), offerCode=offer, offerEligible=eligible)


def verify_status(checkout, transaction_id, response):
    """Only independently fetched PayU data for this exact transaction can verify it."""
    details = response.get('transaction_details', {}).get(transaction_id)
    if not isinstance(details, dict): return 'pending'
    if details.get('txnid') != transaction_id: return 'pending'
    expected = Decimal(checkout['totals']['total']['minorUnits']) / 100
    try:
        # PayU Verify Payment returns transaction_amount; amt is present on some SDK responses.
        actual = Decimal(str(details.get('transaction_amount', details.get('amt', 'NaN'))))
        if not actual.is_finite() or actual != expected: return 'pending'
        if details.get('additional_charges') and Decimal(str(details['additional_charges'])) != 0: return 'pending'
    except Exception: return 'pending'
    if details.get('productinfo') not in (None, 'Parcel simulated order'): return 'pending'
    if details.get('status') == 'success' and details.get('unmappedstatus') == 'captured' and details.get('mihpayid'):
        return 'verified'
    if details.get('unmappedstatus') in ('usercancelled', 'userCancelled'): return 'cancelled'
    if details.get('status') == 'failure' and details.get('unmappedstatus') in ('failed', 'bounced', 'dropped'):
        return 'failed'
    return 'pending'


def generate_sdk_hash(sdk, session, key, salt):
    name, text = sdk.get('hashName', ''), sdk.get('hashString', '')
    if not isinstance(text, str) or len(text) > 16000 or sdk.get('postSalt'):
        raise ValueError('Unsupported hash request')
    payment_prefix = '|'.join([key, session['transactionId'], session['amount'], session['productInfo'], session['firstName'], session['email']])
    if name == 'payment_source':
        # No UDF/SI/split-payment values are used by this app.
        if text != payment_prefix + '|' * 11: raise ValueError('Payment hash is not bound to this checkout')
    else:
        allowed = {'vas_for_mobile_sdk', 'payment_related_details_for_mobile_sdk', 'get_checkout_details', 'getBinInfo', 'validateVPA', 'get_sdk_configuration'}
        if name not in allowed or not text.startswith(key + '|' + name + '|'):
            raise ValueError('Hash command is not allowed')
        if name == 'payment_related_details_for_mobile_sdk' and session['email'] not in text:
            raise ValueError('Hash credential does not match this session')
    if sdk.get('hashType') == 'V2':
        value = hmac.new(salt.encode(), text.encode(), hashlib.sha256).hexdigest()
    elif sdk.get('hashType') in (None, '', 'V1'):
        value = hashlib.sha512((text+salt).encode()).hexdigest()
    else:
        raise ValueError('Unsupported hash version')
    return {'hashName': name, 'hash': value}


class SandboxStore:
    def __init__(self, path, key, salt, callback_base):
        self.path, self.key, self.salt = str(path), key, salt
        self.callback_base = callback_base.rstrip('/')
        if not key or not salt or not self.callback_base.startswith('https://'):
            raise ValueError('Test key, salt and HTTPS callback base are required')
        with self.connect() as db:
            db.execute('CREATE TABLE IF NOT EXISTS attempts (id TEXT PRIMARY KEY, payload TEXT NOT NULL, session TEXT NOT NULL, created INTEGER NOT NULL)')
        os.chmod(self.path, 0o600)
    def connect(self):
        db = sqlite3.connect(self.path, timeout=15)
        db.row_factory = sqlite3.Row
        return db
    def create(self, checkout):
        if checkout.get('mode') != 'payu' or not re.fullmatch('[a-f0-9]{24}', checkout.get('id', '')):
            raise ValueError('Invalid sandbox checkout')
        expected = price(checkout['lines'], checkout['totals'].get('offerCode'))
        if checkout['totals'] != expected: raise ValueError('Client totals do not match server pricing')
        # Do not forward demo addresses to PayU or retain them: no actual delivery takes place.
        frozen = {'id': checkout['id'], 'mode': 'payu', 'lines': checkout['lines'], 'totals': expected}
        payload = json.dumps(frozen, sort_keys=True)
        with self.connect() as db:
            db.execute('BEGIN IMMEDIATE')
            row = db.execute('SELECT * FROM attempts WHERE id=?', (checkout['id'],)).fetchone()
            if row:
                if row['payload'] != payload: raise ValueError('Idempotency key reused with different items')
                return json.loads(row['session'])
            session = dict(environment='test', checkoutId=checkout['id'], transactionId=checkout['id'], key=self.key,
                amount=f"{Decimal(expected['total']['minorUnits'])/100:.2f}", currency='INR', productInfo='Parcel simulated order',
                firstName='Demo', email='parcel-demo@example.invalid', phone='9999999999',
                surl=self.callback_base+'/v1/payu/callback/success', furl=self.callback_base+'/v1/payu/callback/failure',
                sessionToken=secrets.token_urlsafe(32))
            db.execute('INSERT INTO attempts VALUES(?,?,?,?)', (checkout['id'],payload,json.dumps(session),int(time.time())))
            return session
    def load(self, checkout_id, token=None):
        with self.connect() as db: row = db.execute('SELECT * FROM attempts WHERE id=?', (checkout_id,)).fetchone()
        if not row: raise ValueError('Unknown checkout')
        session = json.loads(row['session'])
        if token is not None and not hmac.compare_digest(token, session['sessionToken']): raise PermissionError('Invalid session')
        return json.loads(row['payload']), session
    def verify(self, checkout, session):
        txn = session['transactionId']
        text = f'{self.key}|verify_payment|{txn}|{self.salt}'
        body = urlencode(dict(key=self.key, command='verify_payment', var1=txn, hash=hashlib.sha512(text.encode()).hexdigest())).encode()
        # The endpoint is deliberately fixed to sandbox. There is no production option.
        request = Request('https://test.payu.in/merchant/postservice?form=2', data=body, headers={'Content-Type':'application/x-www-form-urlencoded'})
        with urlopen(request, timeout=20) as response:
            raw = response.read(128000)
        data = json.loads(raw)
        return dict(environment='test', checkoutId=checkout['id'], status=verify_status(checkout,txn,data),
                    amountMinor=checkout['totals']['total']['minorUnits'], currency='INR')
    def callback(self, form):
        checkout, session = self.load(form.get('txnid', ''))
        # Validate the PayU reverse hash before passing a result into the native bridge.
        fields = [self.salt, form.get('status', '')] + ['']*5 + [form.get(f'udf{i}', '') for i in range(5,0,-1)] + [session['email'],session['firstName'],session['productInfo'],session['amount'],session['transactionId'],self.key]
        text = '|'.join(fields)
        additional = form.get('additionalCharges', form.get('additional_charges'))
        if additional: text = additional+'|'+text
        if not hmac.compare_digest(hashlib.sha512(text.encode()).hexdigest(), form.get('hash','').lower()):
            raise ValueError('Invalid callback signature')
        result = self.verify(checkout, session)
        bridge = 'onSuccess' if result['status']=='verified' else 'onFailure'
        # Only our own result is interpolated; posted HTML/card data is never reflected.
        encoded = json.dumps(json.dumps(result)).replace('<','\\u003c')
        return f'<!doctype html><meta name="viewport" content="width=device-width"><p>Returning to Parcel sandbox…</p><script>PayU.{bridge}({encoded});</script>'


def handler(store):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args): pass # Never log payment/hash requests or session tokens.
        def do_POST(self):
            try:
                size = int(self.headers.get('Content-Length','0'))
                if not 0 < size <= 64000: raise ValueError('Invalid request size')
                raw = self.rfile.read(size)
                if self.path.startswith('/v1/payu/callback/'):
                    form = {k:v[0] for k,v in parse_qs(raw.decode(), keep_blank_values=True).items()}
                    body = store.callback(form).encode(); content_type='text/html; charset=utf-8'
                else:
                    data = json.loads(raw)
                    if self.path == '/v1/payu/checkouts': result=store.create(data)
                    elif self.path in ('/v1/payu/hash','/v1/payu/verify'):
                        auth = self.headers.get('Authorization','')
                        if not auth.startswith('Bearer '): raise PermissionError('Session required')
                        checkout,session=store.load(data['checkoutId'], auth[7:])
                        result = generate_sdk_hash(data['sdk'],session,store.key,store.salt) if self.path.endswith('/hash') else store.verify(checkout,session)
                    else: raise ValueError('Unknown endpoint')
                    body=json.dumps(result).encode(); content_type='application/json'
                self.send_response(200);self.send_header('Content-Type',content_type)
                self.send_header('Cache-Control','no-store');self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
            except PermissionError:
                self.send_error(403,'Invalid sandbox session')
            except (ValueError,KeyError,TypeError):
                self.send_error(400,'Invalid sandbox request')
            except Exception:
                self.send_error(503,'Sandbox verification unavailable; retain the pending checkout')
    return Handler

if __name__ == '__main__':
    if os.environ.get('PAYU_TEST_MODE') != 'true': raise SystemExit('Set PAYU_TEST_MODE=true; this tool supports sandbox only.')
    store=SandboxStore(os.environ.get('PAYU_DB','/tmp/parcel-payu-sandbox.db'),os.environ.get('PAYU_TEST_KEY',''),os.environ.get('PAYU_TEST_SALT',''),os.environ.get('PAYU_CALLBACK_BASE',''))
    print('PayU SANDBOX reference service on 127.0.0.1:8787. No live-payment mode. Ctrl+C to stop.')
    ThreadingHTTPServer(('127.0.0.1',8787),handler(store)).serve_forever()
