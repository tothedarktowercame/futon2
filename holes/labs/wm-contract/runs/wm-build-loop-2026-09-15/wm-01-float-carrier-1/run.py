"""Run bounded receipt commands without pipes; retain raw outputs and exits."""
import json, pathlib, subprocess, sys
ROOT = pathlib.Path(__file__).resolve().parent
label, cwd, *command = sys.argv[1:]
p = subprocess.run(command, cwd=cwd, capture_output=True)
(ROOT/'logs'/f'{label}.stdout').write_bytes(p.stdout)
(ROOT/'logs'/f'{label}.stderr').write_bytes(p.stderr)
(ROOT/'logs'/f'{label}.json').write_text(json.dumps(dict(command=command,cwd=cwd,exit=p.returncode),indent=2)+'\n')
sys.stdout.buffer.write(p.stdout)
sys.stderr.buffer.write(p.stderr)
print('EXIT', p.returncode)
sys.exit(p.returncode)
