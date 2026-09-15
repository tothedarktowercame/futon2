"""Review-only gates, separate short-lived JVMs; no shell pipelines or source changes."""
from pathlib import Path
import subprocess, hashlib, json, time
ROOT = Path(__file__).resolve().parents[6]
OUT = Path(__file__).resolve().parent
SRC = 'src/futon2/aif/work_target_store.clj'
TEST = 'test/futon2/aif/work_target_store_test.clj'
PROBE = str(OUT.relative_to(ROOT) / 'probes.clj')
commands = [
 ('clj-kondo', ['clj-kondo', '--lint', SRC, TEST, PROBE]),
 ('check-parens', ['emacs', '-Q', '--batch', '-l', '/home/joe/code/futon4/dev/check-parens.el', '--eval', '(arxana-check-parens-cli)', '--', '--no-defaults', SRC, TEST, PROBE]),
 ('tests', ['clojure', '-X:test', ':nses', '[futon2.aif.work-target-store-test]']),
 ('probes', ['clojure', '-M:test', PROBE]),
 ('mutations', ['clojure', '-M:test', 'holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1b-1-review/run_mutants.clj']),
]
receipts=[]
for name, argv in commands:
 start=time.monotonic()
 with (OUT/(name+'.stdout')).open('w') as out, (OUT/(name+'.stderr')).open('w') as err:
  result=subprocess.run(argv,cwd=ROOT,stdout=out,stderr=err)
 receipt={'name':name,'argv':argv,'cwd':str(ROOT),'exit-status':result.returncode,'elapsed-seconds':round(time.monotonic()-start,3)}
 receipts.append(receipt)
 print(json.dumps(receipt),flush=True)
 if result.returncode:
  print((OUT/(name+'.stdout')).read_text()); print((OUT/(name+'.stderr')).read_text()); break
(OUT/'gate-receipts.json').write_text(json.dumps({'subject':'e38ea7e5153ce30dc094106a9ab5a904d12d3165','source-sha256':{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in [SRC,TEST,PROBE]},'gates':receipts},indent=2)+'\n')
raise SystemExit(next((r['exit-status'] for r in receipts if r['exit-status']),0))
