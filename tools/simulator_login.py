"""Sign a development capture into a fictional local profile when the login gate is shown.

`adb shell input` is lossy against Compose text fields on an emulator: a single
`input text "9000000000"` can deliver five of its ten digits, and a batch of
`keyevent 67` backspaces can deliver a third of them. Nothing reports the loss, so
the capture fails several steps later with a confusing error. Everything here
therefore types in small pieces and verifies the result before moving on.
"""
import re
import time


def _type(run, text, per_key=0.12):
    """Type one character at a time, which survives the dropped-input problem."""
    for character in text:
        run('shell', 'input', 'text', character)
        time.sleep(per_key)


def _tap(run, node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds', '')))
    run('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(.6)


def _fill(run, find, label, value, attempts=4):
    """Focus the field labelled `label`, type `value`, and confirm it is exactly that.

    Backspaces are sent in repeated bursts rather than one batch: a burst loses
    events, so a field holding a previous half-entry needs several passes before it
    is actually empty.
    """
    for _ in range(attempts):
        node = find(label)
        if node is None:
            return False
        _tap(run, node)
        for _ in range(4):
            run('shell', 'input', 'keyevent', *(['67'] * 30))
            time.sleep(.3)
        _type(run, value)
        time.sleep(.7)
        if find(value) is not None:          # the field's own text, exactly
            return True
    raise AssertionError(
        f'Could not enter {value!r} into {label!r}; the device kept dropping input.')


def login_if_needed(run, find, click):
    if find('Phone number') is None:
        return
    _fill(run, find, 'Phone number', '9000000000')
    run('shell', 'input', 'keyevent', '4')
    click('Continue with phone', scroll=True)
    click('4-digit verification code')
    _type(run, '1234')
    run('shell', 'input', 'keyevent', '4')
    click('Verify & continue', scroll=True)
    if find('First name') is not None:
        click('First name'); _type(run, 'Demo')
        click('Last name'); _type(run, 'Shopper')
        run('shell', 'input', 'keyevent', '4')
        click('Create my profile', scroll=True)
