from pathlib import Path
import subprocess,json,time,os
out=Path(__file__).resolve().parent; work=Path((out/'workspace.txt').read_text().strip()); manifest=json.loads((out/'source-manifest.json').read_text())
changed=set(manifest['files']); order=[]; seen=set()
def visit(p):
 if p in seen:return
 seen.add(p)
 for line in (work/p).read_text().splitlines():
  line=line.strip()
  if line.startswith(('import ','public import ','private import ')):
   for m in line.split('import ',1)[1].split():
    d=m.replace('.','/')+'.lean'
    if (work/d).is_file():visit(d)
 if p in changed:order.append(p)
for p in sorted(changed):visit(p)
receipts=[]
def run(name,cmd):
 start=time.monotonic()
 with (out/'logs'/f'{name}.stdout').open('w') as a,(out/'logs'/f'{name}.stderr').open('w') as b:
  r=subprocess.run(cmd,cwd=work,stdout=a,stderr=b,env={**os.environ,'LEAN_NUM_THREADS':'1'})
 receipt={'name':name,'argv':cmd,'cwd':str(work),'exit-status':r.returncode,'seconds':round(time.monotonic()-start,3)}
 receipts.append(receipt);(out/'build-receipts.json').write_text(json.dumps(receipts,indent=2)+'\n'); print(json.dumps(receipt),flush=True)
 return r.returncode
for name,cmd in [('lean-version',['lean','--version']),('lake-version',['lake','--version']),('lean-path',['lake','env','printenv','LEAN_PATH'])]:
 if run(name,cmd):raise SystemExit(1)
for p in order:
 target=work/'.lake/build/lib/lean'/p.replace('.lean','.olean');target.parent.mkdir(parents=True,exist_ok=True)
 if run(Path(p).stem,['lake','env','lean','-j','1','-o',str(target),p]):raise SystemExit(1)
