import hashlib
import hmac
import json
from pathlib import Path
import tempfile
import unittest
from payu_sandbox_server import CATALOG, price, SandboxStore, generate_sdk_hash, verify_status

class SandboxTests(unittest.TestCase):
    def checkout(self):
        product = CATALOG['sound-one']
        lines=[dict(productId='sound-one',title=product['title'],variant='Sand',quantity=2,unitPrice=dict(minorUnits=product['price'],currency='INR'))]
        return dict(id='a'*24,mode='payu',lines=lines,totals=price(lines,'EVERYDAY10'))
    def test_totals_are_authoritative(self):
        checkout=self.checkout()
        self.assertEqual(checkout['totals']['discount']['minorUnits'],50000)
        self.assertEqual(checkout['totals']['tax']['minorUnits'],116964)
        self.assertEqual(checkout['totals']['total']['minorUnits'],769664)
        checkout['lines'][0]['unitPrice']['minorUnits']=1
        with self.assertRaises(ValueError): price(checkout['lines'],None)
    def test_session_is_persistent_idempotent_and_authenticated(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'attempts.db'
            store=SandboxStore(path,'test-key','test-salt','https://sandbox.example.invalid')
            checkout=self.checkout(); session=store.create(checkout)
            self.assertEqual(session,store.create(checkout))
            reopened=SandboxStore(path,'test-key','test-salt','https://sandbox.example.invalid')
            self.assertEqual(session,reopened.load(checkout['id'],session['sessionToken'])[1])
            with self.assertRaises(PermissionError): reopened.load(checkout['id'],'wrong-token')
            changed=json.loads(json.dumps(checkout)); changed['lines'][0]['quantity']=1
            changed['totals']=price(changed['lines'],'EVERYDAY10')
            with self.assertRaises(ValueError): store.create(changed)
    def test_payment_hash_is_bound_to_the_frozen_amount(self):
        checkout=self.checkout()
        with tempfile.TemporaryDirectory() as directory:
            store=SandboxStore(Path(directory)/'test.db','test-key','test-salt','https://sandbox.example.invalid')
            session=store.create(checkout)
            text='|'.join([session[k] for k in ['key','transactionId','amount','productInfo','firstName','email']])+'|'*11
            value=generate_sdk_hash(dict(hashName='payment_source',hashString=text),session,'test-key','test-salt')
            self.assertEqual(hashlib.sha512((text+'test-salt').encode()).hexdigest(),value['hash'])
            with self.assertRaises(ValueError): generate_sdk_hash(dict(hashName='payment_source',hashString=text.replace(session['amount'],'0.01')),session,'test-key','test-salt')
            with self.assertRaises(ValueError): generate_sdk_hash(dict(hashName='verify_payment',hashString='test-key|verify_payment|other|'),session,'test-key','test-salt')
    def test_v2_hash_and_wrong_merchant_are_handled(self):
        session=dict(email='parcel-demo@example.invalid',transactionId='a'*24,amount='1.00',productInfo='Parcel simulated order',firstName='Demo')
        text='test-key|getBinInfo|512345|'
        result=generate_sdk_hash(dict(hashName='getBinInfo',hashString=text,hashType='V2'),session,'test-key','test-salt')
        self.assertEqual(hmac.new(b'test-salt',text.encode(),hashlib.sha256).hexdigest(),result['hash'])
        with self.assertRaises(ValueError): generate_sdk_hash(dict(hashName='getBinInfo',hashString='other|getBinInfo|512345|'),session,'test-key','test-salt')
    def test_sdk_success_or_mismatched_amount_cannot_verify_an_order(self):
        checkout=self.checkout(); txn=checkout['id']; amount=checkout['totals']['total']['minorUnits']/100
        details=dict(txnid=txn,transaction_amount=str(amount),status='success',unmappedstatus='captured',mihpayid='sandbox-123')
        self.assertEqual('verified',verify_status(checkout,txn,dict(transaction_details={txn:details})))
        details['transaction_amount']='0.01'
        self.assertEqual('pending',verify_status(checkout,txn,dict(transaction_details={txn:details})))
        self.assertEqual('pending',verify_status(checkout,txn,dict(status='success')))
        details['transaction_amount']=str(amount);details['unmappedstatus']='auth'
        self.assertEqual('pending',verify_status(checkout,txn,dict(transaction_details={txn:details})))
    def test_callback_rejects_invalid_signature_without_reflecting_input(self):
        with tempfile.TemporaryDirectory() as directory:
            store=SandboxStore(Path(directory)/'test.db','test-key','test-salt','https://sandbox.example.invalid')
            session=store.create(self.checkout())
            with self.assertRaises(ValueError): store.callback(dict(txnid=session['transactionId'],hash='tampered',status='success'))

if __name__=='__main__': unittest.main()
