"""Reproduce the existing wrapper's weakened source, without repairing it."""
from pathlib import Path
import hashlib
import json
import subprocess
import tempfile

root = Path('/home/joe/code/futon2')
run = root / 'holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-receipts-2'
out = run / 'variational-free-energy-positive-receipt.edn'
source_path = Path('/home/joe/code/mathlib4/DarkTower/WarMachine/VariationalFreeEnergyWitness.lean')
source = source_path.read_text()
needle = '=\n      ⟨gaussianReference.expectedVariationalF⟩ := by\n  norm_num [gaussianReference, variationalFreeEnergy, Channel.all]'
replacement = '=\n      variationalFreeEnergy (fun _ => gaussianReference.precision)\n        (fun _ => gaussianReference.predictionError) := by\n  rfl'
assert source.count(needle) == 1
mutant = source.replace(needle, replacement)
(out / 'weakened-source.txt').write_text(mutant)
with tempfile.TemporaryDirectory(prefix='vfe-weakened-probe-') as tmp:
    path = Path(tmp) / 'Weakened.lean'
    path.write_text(mutant)
    argv = ['lake', 'env', 'lean', str(path)]
    with (out / 'weakened-probe.log').open('w') as log:
        result = subprocess.run(argv, cwd='/home/joe/code/mathlib4', stdout=log, stderr=subprocess.STDOUT)
    (out / 'weakened-probe.exit').write_text(str(result.returncode) + '\n')
    (out / 'weakened-probe-command.json').write_text(json.dumps(dict(
        command=argv, cwd='/home/joe/code/mathlib4', exit=result.returncode,
        source_sha256=hashlib.sha256(source_path.read_bytes()).hexdigest(),
        wrapper_sha256=hashlib.sha256((root/'checks/variational_free_energy_witness.clj').read_bytes()).hexdigest(),
        mutant_sha256=hashlib.sha256(mutant.encode()).hexdigest()), indent=2) + '\n')
    print('Unmodified wrapper mutation elaboration exit', result.returncode)
