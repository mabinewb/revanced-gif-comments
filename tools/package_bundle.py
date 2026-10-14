"""Package JVM classes, Android DEX and extension as a ReVanced RVP bundle."""
from pathlib import Path
import hashlib
import json
import os
import time
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
build = Path(os.environ.get('GIF_BUILD_DIR', root / 'build')).resolve()
version=(root/'VERSION').read_text().strip()
morphe='--morphe' in sys.argv
out = build / (('morphe-gif-comments-'+version+'.mpp') if morphe else ('gif-comments-'+version+'.rvp'))
manifest = '\r\n'.join([
    'Manifest-Version: 1.0',
    'Name: '+('Morphe GIF Comments' if morphe else 'GIF Comments'),
    'Description: Tenor GIF comments for YouTube '+('21.20.405' if morphe else '20.40.45'),
    'Version: '+version,
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
