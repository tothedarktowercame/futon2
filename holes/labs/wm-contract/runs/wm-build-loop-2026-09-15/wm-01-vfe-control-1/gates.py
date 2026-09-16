"""Run the five canonical modes only after fresh canonical validation."""
from pathlib import Path
import json
import subprocess

root = Path('/home/joe/code/futon2')
r = root / 'holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-vfe-control-1'
assert (r / 'control.exit').read_text().strip() == '0'
files = ['checks/variational_free_energy_witness.clj', str((r / 'control.clj').relative_to(root))]
gates = [(mode or 'positive', ['bb', files[0]] + ([mode] if mode else [])) for mode in
         ['', '--negative-value', '--negative-type', '--negative-weakened-positive', '--unrelated-positive-edit']]
gates += [('after-gate', ['bb', 'holes/labs/wm-contract/positive_receipt_reattestation_check.bb']),
          ('lint', ['clj-kondo', '--lint'] + files),
          ('parens', ['emacs', '-Q', '--batch', '-l', '/home/joe/code/futon4/dev/check-parens.el', '--eval', '(arxana-check-parens-cli)', '--', '--no-defaults'] + files)]
commands = []
for name, argv in gates:
    name = name.lstrip('-')
    with (r / (name + '.log')).open('w') as log:
        result = subprocess.run(argv, cwd=root, stdout=log, stderr=subprocess.STDOUT)
    (r / (name + '.exit')).write_text(str(result.returncode) + '\n')
    commands.append(dict(command=argv, cwd=str(root), exit=result.returncode))
    (r / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    print(name, result.returncode, flush=True)
    if result.returncode:
        raise SystemExit(result.returncode)
