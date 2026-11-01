#!/usr/bin/env python3
"""Install debug-signed copies of a release APK and its independent test harness."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import tempfile


def run(*args, capture=False):
    return subprocess.run(args, check=True, text=True, stdout=subprocess.PIPE if capture else None).stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('app_apk', type=Path)
    parser.add_argument('test_apk', type=Path)
    parser.add_argument('--sdk', type=Path)
    parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL'))
    args = parser.parse_args()
    sdk = args.sdk or os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        properties = Path(__file__).resolve().parent.parent / 'local.properties'
        if properties.exists():
            for line in properties.read_text().splitlines():
                if line.startswith('sdk.dir='):
                    sdk = line.split('=', 1)[1]
    if not sdk:
        parser.error('Supply --sdk or set ANDROID_HOME')
    sdk = Path(sdk)
    adb = str(sdk / 'platform-tools' / 'adb')
    if not args.serial:
        devices = [line.split()[0] for line in run(adb, 'devices', capture=True).splitlines()[1:]
                   if len(line.split()) == 2 and line.split()[1] == 'device']
        if len(devices) != 1:
            parser.error('Connect exactly one dedicated test device or pass --serial')
        args.serial = devices[0]
    versions = [p for p in (sdk / 'build-tools').iterdir()
                if re.fullmatch(r'\d+\.\d+\.\d+', p.name) and (p / 'apksigner').exists()]
    apksigner = str(max(versions, key=lambda p: tuple(map(int, p.name.split('.')))) / 'apksigner')
    key = Path.home() / '.android' / 'debug.keystore'
    if not key.exists():
        parser.error('Build a debug APK first to create ~/.android/debug.keystore')
    with tempfile.TemporaryDirectory(prefix='keepass-release-tests-') as directory:
        for source, name in ((args.app_apk, 'app'), (args.test_apk, 'tests')):
            signed = str(Path(directory) / (name + '.apk'))
            run(apksigner, 'sign', '--ks', str(key), '--ks-key-alias', 'androiddebugkey',
                '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', signed, str(source))
            run(adb, '-s', args.serial, 'install', '-r', '-t', signed)
        output = run(adb, '-s', args.serial, 'shell', 'am', 'instrument', '-w', '-r',
                     'com.keepassdroid.release.tests/androidx.test.runner.AndroidJUnitRunner', capture=True)
        print(output)
        # `am instrument` and some AGP versions can exit zero even after a crash.
        match = re.search(r'OK \((\d+) tests?\)', output)
        if not match or int(match.group(1)) == 0 or 'FAILURES!!!' in output or 'INSTRUMENTATION_FAILED' in output:
            raise SystemExit('Release instrumentation did not complete successfully')


if __name__ == '__main__':
    main()
