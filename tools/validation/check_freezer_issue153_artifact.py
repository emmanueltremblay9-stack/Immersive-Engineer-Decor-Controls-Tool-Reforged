"""Verify qualification-only edits did not alter production classes or artwork."""
import hashlib
import json
from pathlib import Path
import sys
import zipfile

def verify(jar, baseline):
    expected = json.loads(Path(baseline).read_text())
    with zipfile.ZipFile(jar) as archive:
        actual = {n: hashlib.sha256(archive.read(n)).hexdigest() for n in expected['entries']}
    if actual != expected['entries']:
        raise ValueError('Freezer production class/asset byte hashes differ from baseline')
    print(f'PASS {len(actual)} scoped Freezer class/asset hashes; baseline JAR {expected["jar_sha256"]}')

if __name__ == '__main__': verify(sys.argv[1], sys.argv[2])
