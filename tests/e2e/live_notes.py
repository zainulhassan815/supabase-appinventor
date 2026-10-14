#!/usr/bin/env python3
import json
import os
import shlex
import subprocess
import sys
import time
import urllib.request
import xml.etree.ElementTree as ET

ADB = os.environ.get('ADB', os.path.expanduser('~/Android/sdk/platform-tools/adb'))
PASSWORD = 'secret123'


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, text=True, check=True).stdout


def nodes():
    try:
        adb('shell', 'uiautomator', 'dump', '/sdcard/demo.xml')
        return list(ET.fromstring(adb('shell', 'cat', '/sdcard/demo.xml')).iter('node'))
    except subprocess.CalledProcessError:
        return []


def wait(description, predicate, timeout=60):
    start = time.time()
    deadline = start + timeout
    while time.time() < deadline:
        found = predicate(nodes())
        if found is not None and found is not False:
            print(f'ok   {description} ({time.time() - start:.1f}s)')
            return found
        time.sleep(1)
    raise SystemExit(f'FAIL {description}')


def with_text(text):
    return lambda found: next((n for n in found if n.get('text') == text), None)


def without_text(text):
    return lambda found: all(n.get('text') != text for n in found)


FIELDS = ['Email', 'Password', 'New note']


def field(name):
    def find(found):
        fields = [n for n in found if n.get('class') == 'android.widget.EditText']
        return fields[FIELDS.index(name)] if len(fields) == len(FIELDS) else None
    return find


def empty_field(name):
    return lambda found: (node := field(name)(found)) is not None and node.get('text') in ('', name)


def listed(text):
    return lambda found: next((n for n in found if n.get('class') == 'android.widget.TextView'
                               and n.get('text') == text), None)


def not_listed(text):
    return lambda found: listed(text)(found) is None


def tap(node):
    left, top, right, bottom = (int(v) for v in node.get('bounds').replace('][', ',').strip('[]').split(','))
    adb('shell', 'input', 'tap', str((left + right) // 2), str((top + bottom) // 2))


def leave_field():
    if 'mInputShown=true' in adb('shell', 'dumpsys', 'input_method'):
        adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    adb('shell', 'input', 'keyevent', 'KEYCODE_DPAD_DOWN')


def type_into(name, text):
    node = wait(f'field {name}', field(name))
    tap(node)
    existing = '' if node.get('text') == name else node.get('text')
    adb('shell', 'input', 'keyevent', 'KEYCODE_MOVE_END', *['KEYCODE_DEL'] * len(existing))
    adb('shell', 'input', 'text', shlex.quote(text.replace(' ', '%s')))
    leave_field()


def press(text):
    tap(wait(f'button {text}', with_text(text)))


def add_note(text):
    type_into('New note', text)
    press('Add')
    wait(f'note "{text}" listed', listed(text))


class Laptop:
    def __init__(self, url, key, email):
        self.url, self.key = url, key
        self.token = self.call('POST', '/auth/v1/token?grant_type=password',
                               {'email': email, 'password': PASSWORD})['access_token']

    def call(self, method, path, body=None):
        headers = {'apikey': self.key, 'Content-Type': 'application/json', 'Prefer': 'return=representation'}
        if hasattr(self, 'token'):
            headers['Authorization'] = f'Bearer {self.token}'
        request = urllib.request.Request(self.url + path, method=method, headers=headers,
                                         data=json.dumps(body).encode() if body is not None else None)
        with urllib.request.urlopen(request) as response:
            text = response.read().decode()
            return json.loads(text) if text else None

    def add_note(self, text):
        return self.call('POST', '/rest/v1/notes', {'body': text})[0]['id']

    def delete_note(self, note_id):
        self.call('DELETE', f'/rest/v1/notes?id=eq.{note_id}')

    def broadcast_typing(self):
        self.call('POST', '/realtime/v1/api/broadcast', {'messages': [
            {'topic': 'notes', 'event': 'typing', 'payload': {'who': 'laptop'}, 'private': True}]})


def main(package, url, key, shots):
    email = f'demo{int(time.time())}@example.com'
    wait('app starts signed out', with_text('Signed out'))

    type_into('Email', email)
    type_into('Password', PASSWORD)
    press('Sign up')
    wait('signed in after sign up', with_text(f'Signed in as {email}'))
    wait('presence shows this phone online', with_text('1 online'))

    add_note('Buy milk')
    wait('note box cleared after adding', empty_field('New note'))
    add_note('Call mom today')

    laptop = Laptop(url, key, email)
    laptop_note = laptop.add_note('From the laptop')
    wait('laptop insert arrives through realtime', listed('From the laptop'))
    laptop.broadcast_typing()
    wait('laptop broadcast arrives', with_text('laptop is typing...'))

    press('Stats')
    wait('edge function counts notes and words', with_text('3 notes, 8 words'))

    press('Attach photo')
    wait('photo uploaded and shown from a signed URL', with_text('Photo uploaded'))
    time.sleep(2)
    with open(os.path.join(shots, 'photo.png'), 'wb') as f:
        f.write(subprocess.run([ADB, 'exec-out', 'screencap', '-p'], capture_output=True, check=True).stdout)

    tap(wait('note "Buy milk" to tap', listed('Buy milk')))
    wait('tapped note deleted', not_listed('Buy milk'))
    laptop.delete_note(laptop_note)
    wait('laptop delete arrives through realtime', not_listed('From the laptop'))

    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'monkey', '-p', package, '-c', 'android.intent.category.LAUNCHER', '1')
    wait('session restored after restart', with_text(f'Signed in as {email}'))
    wait('notes reloaded after restart', listed('Call mom today'))

    press('Sign out')
    wait('signed out', with_text('Signed out'))
    wait('notes cleared after sign out', not_listed('Call mom today'))
    print('DEMO PASS')


if __name__ == '__main__':
    main(*sys.argv[1:])
