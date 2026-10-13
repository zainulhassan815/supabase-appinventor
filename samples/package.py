import glob
import os
import sys
import zipfile

SAMPLE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'live-notes')


def package(aix_path, aia_path):
    with zipfile.ZipFile(aix_path) as aix, zipfile.ZipFile(aia_path, 'w', zipfile.ZIP_DEFLATED) as aia:
        for root, _, files in os.walk(SAMPLE):
            for name in files:
                path = os.path.join(root, name)
                aia.write(path, os.path.relpath(path, SAMPLE))
        for name in aix.namelist():
            if not name.endswith('/'):
                aia.writestr(f'assets/external_comps/{name}', aix.read(name))


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'out'
    package(glob.glob(os.path.join(out, '*.aix'))[0], os.path.join(out, 'LiveNotes.aia'))
