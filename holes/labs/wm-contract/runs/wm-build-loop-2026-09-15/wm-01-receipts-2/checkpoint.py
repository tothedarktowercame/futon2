"""Execute one approved manifest entry, serially, and retain its checkpoint."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys

ROOT = Path('/home/joe/code/futon2')
RUN = ROOT / 'holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-receipts-2'

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def command(name, argv, directory, logs):
    with (logs / (name + '.log')).open('w') as out:
        result = subprocess.run(argv, cwd=directory, stdout=out, stderr=subprocess.STDOUT)
    (logs / (name + '.exit')).write_text(str(result.returncode) + '\n')
    entry = dict(name=name, command=argv, cwd=str(directory), exit=result.returncode)
    path = logs / 'commands.json'
    entries = json.loads(path.read_text()) if path.exists() else []
    path.write_text(json.dumps(entries + [entry], indent=2) + '\n')
    print(name, result.returncode, flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
    return entry

index = int(sys.argv[1])
manifest = json.loads((RUN / 'manifest.json').read_text())
filename = manifest['receipts'][index]
dirpath = RUN / filename
semantic = json.loads((dirpath / 'semantic.json').read_text())
control = str((RUN / 'receipt_control.clj').relative_to(ROOT))
additions = '[' + ' '.join(json.dumps(x) for x in semantic['additions']) + ']'
command('protocol', ['bb', control, 'run', filename, additions], ROOT, dirpath)
for mode in [''] + semantic['negative-modes']:
    command('positive' if not mode else mode.lstrip('-'),
            ['bb', 'checks/' + semantic['wrapper']] + ([mode] if mode else []), ROOT, dirpath)
before = json.loads((dirpath / 'before-hashes.json').read_text())
assert all(sha(path) == digest for path, digest in before.items())
(dirpath / 'after-hashes.json').write_text(json.dumps(before, indent=2) + '\n')
result = json.loads((dirpath / 'result.json').read_text())
result['semantic-disposition'] = semantic
result['commands'] = json.loads((dirpath / 'commands.json').read_text())
result['protocol-commands'] = str((dirpath / 'protocol-commands.json').relative_to(RUN))
result['status'] = 'author-verified; independent review pending'
matrix_path = RUN / 'matrix.json'
matrix = json.loads(matrix_path.read_text()) if matrix_path.exists() else {'entries': []}
assert len(matrix['entries']) == index
matrix['entries'].append(result)
matrix['next-receipt'] = manifest['receipts'][index + 1] if index + 1 < len(manifest['receipts']) else None
matrix_path.write_text(json.dumps(matrix, indent=2) + '\n')
print('CHECKPOINT READY', filename, flush=True)
paths = [str(p.relative_to(ROOT)) for p in RUN.iterdir() if p.is_file()]
paths += [str(dirpath.relative_to(ROOT)), 'holes/labs/wm-contract/' + filename]
subprocess.run(['git', 'log', '-1', '--oneline'], cwd=ROOT, check=True)
subprocess.run(['git', 'add', '--'] + paths, cwd=ROOT, check=True)
subprocess.run(['git', 'diff', '--cached', '--check'], cwd=ROOT, check=True)
subprocess.run(['git', 'commit', '--only', '-m', 'Re-attest ' + filename + ' at repaired carrier', '--'] + paths, cwd=ROOT, check=True)
