#!/usr/bin/env python3
"""Validate and report Play's R8 optimization metadata in release bundles."""
import argparse
import json
import sys
import zipfile


def inspect_bundle(path):
    with zipfile.ZipFile(path) as bundle:
        metadata = json.loads(bundle.read('BUNDLE-METADATA/com.android.tools/r8.json'))
        mapping = bundle.getinfo('BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map')
        if mapping.file_size == 0:
            raise ValueError('Empty deobfuscation mapping')
        dex_bytes = sum(info.file_size for info in bundle.infolist() if info.filename.endswith('.dex'))
    scores = {
        'obfuscation': 100 - metadata['stats']['noObfuscationPercentage'],
        'optimization': 100 - metadata['stats']['noOptimizationPercentage'],
        'shrinking': 100 - metadata['stats']['noShrinkingPercentage'],
    }
    print(json.dumps({'bundle': path, 'dex_bytes': dex_bytes, 'percentages': scores}, indent=2))
    if any(not 25 <= score <= 100 for score in scores.values()):
        raise ValueError('An R8 optimization category is below 25% or has an invalid value')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('bundles', nargs='+')
    args = parser.parse_args()
    failed = False
    for path in args.bundles:
        try:
            inspect_bundle(path)
        except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
            print(f'{path}: {error}', file=sys.stderr)
            failed = True
    return int(failed)


if __name__ == '__main__':
    sys.exit(main())
