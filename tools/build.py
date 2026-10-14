"""Build/test the RVP without GitHub Packages credentials or an Android SDK.

Requires Python 3.10+ and Java 21 on PATH. Downloads hash-pinned official tools.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import urllib.request
import zipfile

PLATFORM='morphe' if '--morphe' in sys.argv else 'revanced'
ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get('GIF_CACHE_DIR', ROOT / 'tools/cache')).resolve()
BUILD = Path(os.environ.get('GIF_BUILD_DIR', ROOT / 'build')).resolve()


def run(*args):
    print('Running:', Path(str(args[0])).name, flush=True)
    subprocess.run([str(arg) for arg in args], cwd=ROOT, check=True)


def jar(source, target):
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as out:
        for path in sorted(source.rglob('*')):
            if path.is_file():
                out.write(path, path.relative_to(source).as_posix())


def main():
    java = shutil.which('java')
    if not java:
        raise SystemExit('Java 21 must be available on PATH')
    CACHE.mkdir(parents=True, exist_ok=True)
    BUILD.mkdir(parents=True, exist_ok=True)
    dependencies = json.loads((ROOT / 'tools/dependencies.json').read_text(encoding='utf-8'))
    files = {}
    for name, dependency in dependencies.items():
        if name=='morphe-cli' and PLATFORM!='morphe': continue
        path = CACHE / dependency['file']
        if not path.exists():
            print('Downloading:', name, flush=True)
            request = urllib.request.Request(dependency['url'], headers={'User-Agent': 'revanced-gif-comments-build'})
            with urllib.request.urlopen(request, timeout=60) as response, path.open('wb') as out:
                shutil.copyfileobj(response, out)
        if hashlib.sha256(path.read_bytes()).hexdigest() != dependency['sha256']:
            raise SystemExit(f'Hash mismatch: {path.name}. Remove the failed download and retry.')
        files[name] = path
    if PLATFORM=='morphe': files['cli']=files['morphe-cli']
    kotlin = CACHE / 'kotlin/kotlinc'
    if not (kotlin / 'lib/kotlin-compiler.jar').exists():
        with zipfile.ZipFile(files['kotlin']) as archive:
            archive.extractall(CACHE / 'kotlin')
    platform=CACHE/'android-platform'
    if not (platform/'android-9/android.jar').exists():
        with zipfile.ZipFile(files['android']) as archive:
            archive.extractall(platform)
    files['android']=platform/'android-9/android.jar'
    for name in ['patch-classes', 'extension-classes', 'test-classes', 'patch-dex', 'extension-dex']:
        directory=(BUILD/name).resolve()
        if directory.parent!=BUILD:
            raise SystemExit('Unsafe build output directory')
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    run(java, '-cp', str(kotlin / 'lib/*'), 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-kotlin-home', kotlin, '-Xcontext-parameters', '-Xskip-prerelease-check', '-Xlambdas=class',
        '-jvm-target', '17', '-cp', files['cli'], '-d', BUILD / 'patch-classes',
        ROOT / ('src/main/kotlin/MorpheGifCommentsPatch.kt' if PLATFORM=='morphe' else 'src/main/kotlin/GifCommentsPatch.kt'))
    run(java, '-jar', files['ecj'], '-8', '-encoding', 'UTF-8', '-cp', files['android'], '-d', BUILD / 'extension-classes',
        *sorted((ROOT / 'src/extension/java').rglob('*.java')))
    testcp = os.pathsep.join(map(str, [files['protobuf'], files['json'], BUILD / 'extension-classes']))
    run(java, '-jar', files['ecj'], '-8', '-encoding', 'UTF-8', '-cp', testcp,
        '-d', BUILD / 'test-classes', *sorted((ROOT/'src/test/java').glob('*.java')))
    run(java, '-cp', testcp + os.pathsep + str(BUILD / 'test-classes'), 'GifCommentsTest')
    run(java, '-cp', testcp + os.pathsep + str(BUILD / 'test-classes'), 'TenorClientTest')
    run(java, '-cp', testcp + os.pathsep + str(BUILD / 'test-classes'), 'GifTransportTest')
    run(java, '-cp', testcp + os.pathsep + str(BUILD / 'test-classes'), 'GifLibraryTest')
    jar(BUILD / 'patch-classes', BUILD / 'patch.jar')
    jar(BUILD / 'extension-classes', BUILD / 'extension.jar')
    run(java, '-cp', files['r8'], 'com.android.tools.r8.D8', '--min-api', '28', '--classpath', files['cli'],
        '--output', BUILD / 'patch-dex', BUILD / 'patch.jar')
    run(java, '-cp', files['r8'], 'com.android.tools.r8.D8', '--min-api', '28', '--lib', files['android'],
        '--output', BUILD / 'extension-dex', BUILD / 'extension.jar')
    run(sys.executable, ROOT / 'tools/package_bundle.py', *(['--morphe'] if PLATFORM=='morphe' else []))
    version=(ROOT/'VERSION').read_text().strip()
    if PLATFORM=='morphe':
        run(java,'-jar',files['cli'],'list-patches','--patches',BUILD/('morphe-gif-comments-'+version+'.mpp'),'-p','-v')
    else:
        run(java, '-jar', files['cli'], 'list-patches', '-p', BUILD / ('gif-comments-'+version+'.rvp'), '-b', '--packages', '--versions')



if __name__ == '__main__':
    main()
