"""Sign a development capture into a fictional local profile when the login gate is shown."""
def login_if_needed(run, find, click):
    if find('Phone number') is None:
        return
    click('Phone number')
    run('shell', 'input', 'text', '9000000000')
    run('shell', 'input', 'keyevent', '4')
    click('Continue with phone', scroll=True)
    click('4-character verification code')
    run('shell', 'input', 'text', '1234')
    run('shell', 'input', 'keyevent', '4')
    click('Verify & continue', scroll=True)
    if find('First name') is not None:
        click('First name'); run('shell', 'input', 'text', 'Demo')
        click('Last name'); run('shell', 'input', 'text', 'Shopper')
        run('shell', 'input', 'keyevent', '4')
        click('Create my profile', scroll=True)
