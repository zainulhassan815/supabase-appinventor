#!/usr/bin/env python3
import json
import os
import shutil
import sys
import zipfile

USAGE = 'usage: app.py <project-dir> <aix> <scenario.yail> <project-url> <publishable-key>'
PACKAGE = 'dev.dreamerslab.supabase.e2e.Screen1'
EXTENSION = 'dev.dreamerslab.supabase'
RUNTIME = 'com.google.appinventor.components.runtime'
BUILT_IN_VERSIONS = {'Label': '6', 'File': '4'}
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets')


def components(url, key):
    return [
        ('Supabase', f'{EXTENSION}.Supabase', {'ProjectUrl': url, 'PublishableKey': key}),
        ('SupabaseDatabase', f'{EXTENSION}.SupabaseDatabase', {'Client': 'Supabase1'}),
        ('SupabaseStorage', f'{EXTENSION}.SupabaseStorage', {'Client': 'Supabase1', 'Bucket': 'uploads'}),
        ('SupabaseChannel', f'{EXTENSION}.SupabaseChannel', {'Client': 'Supabase1', 'Topic': 'e2e', 'PresenceKey': 'tester'}),
        ('File', f'{RUNTIME}.File', {}),
        ('Label', f'{RUNTIME}.Label', {'Text': ''}),
    ]


def versions(aix, target):
    with zipfile.ZipFile(aix) as zf:
        zf.extractall(target)
        descriptor = next(name for name in zf.namelist() if name.endswith('/components.json'))
        return {c['name']: c['version'] for c in json.loads(zf.read(descriptor))}


def form_json(instances, versions):
    return {
        'authURL': ['ai2.appinventor.mit.edu'],
        'YaVersion': '237',
        'Source': 'Form',
        'Properties': {
            '$Name': 'Screen1', '$Type': 'Form', '$Version': '32',
            'AppName': 'SupabaseE2E', 'Title': 'Supabase E2E', 'Uuid': '0',
            '$Components': [{
                '$Name': f'{name}1', '$Type': name, '$Version': versions.get(name) or BUILT_IN_VERSIONS[name], 'Uuid': str(i + 1),
                **properties,
            } for i, (name, _, properties) in enumerate(instances)],
        },
    }


def property_yail(component, name, value):
    if name == 'Client':
        return f"(set-and-coerce-property! '{component} '{name} (get-component {value}) 'component)"
    return f"(set-and-coerce-property! '{component} '{name} {json.dumps(value)} 'text)"


def yail(instances, scenario):
    adds = '\n'.join(
        f'(add-component Screen1 {type_name} {name}1 '
        + ' '.join(property_yail(f'{name}1', key, value) for key, value in properties.items()) + ')'
        for name, type_name, properties in instances)
    return f'''#|
$Source $Yail
|#

(define-form {PACKAGE} Screen1 #f)
(require <com.google.youngandroid.runtime>)
(do-after-form-creation (set-and-coerce-property! 'Screen1 'AppName "SupabaseE2E" 'text))
{adds}
(def (p$report line)
  (set-and-coerce-property! 'Label1 'Text
    (string-append (coerce-to-string (get-property 'Label1 'Text)) (coerce-to-string line) "\\n") 'text))
{scenario}
(init-runtime)
'''


def main(project, aix, scenario, url, key):
    instances = components(url, key)
    found = versions(aix, os.path.join(project, 'assets', 'external_comps'))
    for asset in os.listdir(ASSETS):
        shutil.copy(os.path.join(ASSETS, asset), os.path.join(project, 'assets', asset))
    source = os.path.join(project, 'src', *PACKAGE.split('.')[:-1])
    os.makedirs(source, exist_ok=True)
    os.makedirs(os.path.join(project, 'youngandroidproject'), exist_ok=True)
    with open(os.path.join(project, 'youngandroidproject', 'project.properties'), 'w') as f:
        f.write(f'main={PACKAGE}\nname=SupabaseE2E\nassets=../assets\nsource=../src\nbuild=../build\n'
                'versioncode=1\nversionname=1.0\nuseslocation=False\naname=SupabaseE2E\nsizing=Responsive\n'
                'showlistsasjson=True\ntheme=AppTheme.Light.DarkActionBar\n')
    with open(os.path.join(source, 'Screen1.scm'), 'w') as f:
        f.write(f'#|\n$JSON\n{json.dumps(form_json(instances, found))}\n|#\n')
    with open(os.path.join(source, 'Screen1.bky'), 'w') as f:
        f.write('<xml xmlns="http://www.w3.org/1999/xhtml"></xml>\n')
    with open(scenario) as f, open(os.path.join(source, 'Screen1.yail'), 'w') as out:
        out.write(yail(instances, f.read()))


if __name__ == '__main__':
    if len(sys.argv) != 6:
        sys.exit(USAGE)
    main(*sys.argv[1:])
