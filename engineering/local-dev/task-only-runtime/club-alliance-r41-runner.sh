#!/usr/bin/env bash
# Dedicated ClubAllianceManager runtime. Never invokes migrations or seed scripts.
set -euo pipefail
umask 077
private_dir=/Users/aoo/.secrets/aoo-club-alliance-r41
manifest="$private_dir/runtime-manifest.json"
[[ "${R41_DB_HOST:-}" == 127.0.0.1 && "${R41_DB_PORT:-}" == 13369 && "${R41_DB_SCHEMA:-}" == aoo_root_lease_r16 ]] || { echo 'R41: dedicated clone configuration required' >&2; exit 1; }
[[ -f "$manifest" ]] || { echo 'R41: reviewed private runtime manifest is not ready' >&2; exit 1; }
exec python3 - "$manifest" <<'PY'
import hashlib, json, os, pathlib, signal, subprocess, sys, time

root = pathlib.Path('/Users/aoo/.secrets/aoo-club-alliance-r41')
manifest = json.loads(pathlib.Path(sys.argv[1]).read_text())
java = '/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home/bin/java'
jar = pathlib.Path('/Users/aoo/Code/Game/BCG/Aoo/server/work/local-runtime/core-services/classpath/bootstrap-20261006033822-91796/app.jar')
expected = '5b9b8cafc422e23eb49afb5603d46f70d5865a12715273588457fd099388bf50'
if hashlib.sha256(jar.read_bytes()).hexdigest() != expected:
    raise SystemExit('R41: application artifact hash mismatch')
children = []
logs = []

def stop(*_):
    for child in reversed(children):
        if child.poll() is None:
            child.terminate()
    for child in reversed(children):
        try:
            child.wait(timeout=10)
        except subprocess.TimeoutExpired:
            child.kill()
            child.wait()
    for log in logs:
        log.close()

signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))
signal.signal(signal.SIGINT, lambda *_: sys.exit(0))
try:
    apps = manifest.get('apps', [])
    if not apps:
        raise SystemExit('R41: no reviewed application entries')
    seen = set()
    for app in apps:
        name = app['name']
        if name not in {'account', 'version', 'hall', 'gateway', 'social', 'gifting', 'media', 'referral', 'club'} or name in seen:
            raise SystemExit('R41: invalid or duplicate application')
        seen.add(name)
        env_path = root / (name + '.json')
        if env_path.is_symlink() or env_path.stat().st_mode & 0o077:
            raise SystemExit('R41: private application environment permissions required')
        values = json.loads(env_path.read_text())
        if not all(isinstance(k, str) and isinstance(v, str) for k, v in values.items()):
            raise SystemExit('R41: environment must contain strings')
        for key, value in values.items():
            if key.endswith('_DATABASE_URL') and not value.startswith('jdbc:mysql://127.0.0.1:13369/aoo_root_lease_r16'):
                raise SystemExit('R41: database outside dedicated clone')
            if key.endswith('_HTTP_PORT') or key == 'INTERNAL_PORT':
                if not 18670 <= int(value) <= 18689:
                    raise SystemExit('R41: application port outside dedicated range')
        env = {'PATH': '/usr/bin:/bin:/usr/sbin:/sbin', 'HOME': str(root), 'TZ': 'UTC', **values}
        log = (root / (name + '.log')).open('ab')
        logs.append(log)
        entry = ['com.aoo.bcg.gateway.GatewayApplication'] if name == 'gateway' else ['com.aoo.bcg.bootstrap.BootstrapAPP', name]
        child = subprocess.Popen([java, '-Duser.timezone=UTC', '-cp', str(jar)+os.pathsep+str(jar.parent/'lib'/'*'), *entry], cwd=str(jar.parent), env=env, stdout=log, stderr=subprocess.STDOUT)
        children.append(child)
        (root / (name + '.pid')).write_text(str(child.pid) + '\n')
        time.sleep(1)
        if child.poll() is not None:
            raise SystemExit('R41: ' + name + ' exited; inspect private diagnostic log')
    while True:
        for name, child in zip([app['name'] for app in apps], children):
            if child.poll() is not None:
                raise SystemExit('R41: ' + name + ' exited; inspect private diagnostic log')
        time.sleep(1)
finally:
    stop()
PY
