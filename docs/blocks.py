import glob
import html
import json
import os
import re
import sys
import zipfile

DOCS = os.path.dirname(os.path.abspath(__file__))
IMAGES = 'images/blocks'

COMPONENTS = [
    ('Supabase', 'Authentication', 'authentication.md'),
    ('SupabaseDatabase', 'Database', 'database.md'),
    ('SupabaseStorage', 'Storage', 'storage.md'),
    ('SupabaseChannel', 'Realtime', 'realtime.md'),
    ('SupabaseFunctions', 'Edge Functions', 'functions.md'),
]


def is_deprecated(entry):
    return str(entry.get('deprecated')).lower() == 'true'


def sentence(text):
    text = text.strip()
    return text if text.endswith(('.', '!', '?')) else text + '.'


def plain(text):
    return html.unescape(re.sub(r'<[^>]+>', '', text)).strip()


def anchor(title):
    return re.sub(r'[^\w\- ]', '', title.lower()).replace(' ', '-')


class Reference:

    def __init__(self, components):
        self.components = {c['name']: c for c in components}
        self.images = []

    def image(self, name, alt):
        self.images.append(name)
        return f'![{alt}]({IMAGES}/{name}.png)'

    def block(self, instance, name, description):
        return f'{self.image(f"{instance}-{name}", name)}\n\n**{name}:** {sentence(description)}'

    def property(self, instance, prop):
        shots = [self.image(f'{instance}-get{prop["name"]}', f'get {prop["name"]}')]
        if prop['rw'] == 'read-write':
            shots.append(self.image(f'{instance}-set{prop["name"]}', f'set {prop["name"]}'))
        return ' '.join(shots) + f'\n\n**{prop["name"]}** ({prop["rw"]}): {sentence(prop["description"])}'

    def dropdowns(self):
        found = {}
        for component in self.components.values():
            for entry in component['events'] + component['methods'] + component['blockProperties']:
                for owner in [entry] + entry.get('params', []):
                    data = owner.get('helper', {}).get('data')
                    if data:
                        found[data['key']] = data
        return [f'{self.image(f"helper-{key}", key)}\n\n**{key}:** ' + ', '.join(o['name'] for o in data['options'])
                for key, data in sorted(found.items())]

    def component(self, name, guide, path):
        component = self.components[name]
        instance = f'{name}1'
        methods = [m for m in component['methods'] if not is_deprecated(m)]
        properties = [p for p in component['blockProperties'] if not is_deprecated(p)]
        lines = ['', f'## {name}', '', f'{sentence(plain(component["helpString"]))} Guide: [{guide}]({path}).']
        for title, entries in [
            ('Functions', [self.block(instance, m['name'], m['description']) for m in methods]),
            ('Events', [self.block(instance, e['name'], e['description']) for e in component['events']]),
            ('Properties', [self.property(instance, p) for p in properties]),
        ]:
            lines += ['', f'### {name}: {title}', '', '\n\n'.join(entries)]
        return lines

    def markdown(self):
        if set(self.components) != {name for name, _, _ in COMPONENTS}:
            sys.exit(f'Components without a section: {sorted(set(self.components) ^ {n for n, _, _ in COMPONENTS})}')
        deprecated = [f'`{name}.{m["name"]}`' for name, _, _ in COMPONENTS
                      for m in self.components[name]['methods'] if is_deprecated(m)]
        lines = [
            '# Blocks',
            '',
            'Every block of the Supabase extension. Each image contains its block, so you can drag it '
            'straight into your workspace.',
            '',
        ]
        lines += [f'- [{name}](#{anchor(name)})' for name, _, _ in COMPONENTS]
        lines += ['- [Dropdowns](#dropdowns)', '- [Deprecated](#deprecated)']
        for name, guide, path in COMPONENTS:
            lines += self.component(name, guide, path)
        lines += ['', '## Dropdowns', '', 'Choices for blocks, properties and the keys of query and search '
                  'dictionaries.', '', '\n\n'.join(self.dropdowns())]
        lines += ['', '## Deprecated', '',
                  'Hidden from the blocks drawer and kept working for existing projects. The guides name the '
                  'blocks that replace them: ' + ', '.join(deprecated) + '.', '']
        return '\n'.join(lines)


def main(aix_path):
    with zipfile.ZipFile(aix_path) as aix:
        components = next(json.loads(aix.read(n)) for n in aix.namelist() if n.endswith('/components.json'))
    reference = Reference(components)
    text = reference.markdown()
    missing = [n for n in reference.images if not os.path.exists(os.path.join(DOCS, IMAGES, f'{n}.png'))]
    if missing:
        sys.exit(f'Missing block images: {", ".join(missing)}')
    with open(os.path.join(DOCS, 'blocks.md'), 'w') as f:
        f.write(text)


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'out'
    main(glob.glob(os.path.join(out, '*.aix'))[0])
