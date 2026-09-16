"""Run scoped gates serially; preserve each process's actual exit status."""
from pathlib import Path
import json
import subprocess

root = Path('/home/joe/code/futon2')
run = root / 'holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-positive-binding-1'
lean = Path('/home/joe/code/mathlib4')
control = str((run / 'reattest.clj').relative_to(root))
probe = run / 'axioms.lean'
probe.write_text('import DarkTower.WarMachine.PredictiveOutcomeKernelWitness\n#print axioms DarkTower.WarMachine.PredictiveOutcomeKernelWitness.allPolicyRowsNormalised\n')
gates = [
 ('positive-theorem', lean, ['lake', 'env', 'lean', 'DarkTower/WarMachine/PredictiveOutcomeKernelWitness.lean'], 0),
 ('axioms', lean, ['lake', 'env', 'lean', str(probe)], 0),
 ('positive-wrapper', root, ['bb', 'checks/predictive_outcome_kernel_witness.clj'], 0),
 ('negative-unconditional', root, ['bb', 'checks/predictive_outcome_kernel_witness.clj', '--negative-unconditional'], 0),
 ('negative-softmax', root, ['bb', 'checks/predictive_outcome_kernel_witness.clj', '--negative-softmax'], 0),
 ('after-gate', root, ['bb', 'holes/labs/wm-contract/positive_receipt_reattestation_check.bb'], 1),
 ('lint', root, ['clj-kondo', '--lint', control], 0),
 ('parens', root, ['emacs', '-Q', '--batch', '-l', '/home/joe/code/futon4/dev/check-parens.el', '--eval', '(arxana-check-parens-cli)', '--', '--no-defaults', control], 0),
]
results = []
for name, cwd, command, expected in gates:
    with (run / (name + '.log')).open('w') as out:
        result = subprocess.run(command, cwd=cwd, stdout=out, stderr=subprocess.STDOUT)
    (run / (name + '.exit')).write_text(str(result.returncode) + '\n')
    results.append(dict(name=name, cwd=str(cwd), command=command, exit=result.returncode, expected=expected))
    (run / 'commands.json').write_text(json.dumps(results, indent=2) + '\n')
    print(name, 'exit', result.returncode, flush=True)
    if result.returncode != expected:
        raise SystemExit(result.returncode or 1)
