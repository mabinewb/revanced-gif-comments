"""Package JVM classes, Android DEX and extension as a ReVanced RVP bundle."""
from pathlib import Path
import hashlib
import json
import os
import time
import zipfile

root = Path(__file__).resolve().parents[1]
build = Path(os.environ.get('GIF_BUILD_DIR', root / 'build')).resolve()
out = build / 'gif-comments-0.1.0.rvp'
manifest = '\r\n'.join([
    'Manifest-Version: 1.0',
    'Name: GIF Comments',
    'Description: Experimental Tenor comments for YouTube 20.40.45',
    'Version: 0.1.0',
    f'Timestamp: {int(time.time() * 1000)}',
    'Source: https://github.com/mabinewb/revanced-gif-comments',
    'Author: mabinewb',
    'Website: https://github.com/mabinewb/revanced-gif-comments',
    'License: GNU General Public License v3.0',
    '', '',
])
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as archive:
    archive.writestr('META-INF/MANIFEST.MF', manifest)
    for file in (build / 'patch-classes').rglob('*'):
        if file.is_file():
            archive.write(file, file.relative_to(build / 'patch-classes').as_posix())
    archive.write(build / 'patch-dex/classes.dex', 'classes.dex')
    archive.write(build / 'extension-dex/classes.dex', 'gif-comments-extension.dex')
digest = hashlib.sha256(out.read_bytes()).hexdigest()
(build / 'SHA256SUMS.txt').write_text(f'{digest}  {out.name}\n', encoding='ascii')
print(json.dumps({'file': out.name, 'sha256': digest, 'bytes': out.stat().st_size}))
