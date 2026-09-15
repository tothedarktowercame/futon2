from pathlib import Path
import subprocess,json,time,os
out=Path(__file__).resolve().parent;work=Path((out/'workspace.txt').read_text().strip());rows=[]
for f in sorted(out.glob('Axioms-*.lean')):
 cmd=['lake','env','lean','-j','1',str(f)];start=time.monotonic()
 with (out/'logs'/f'{f.stem}.stdout').open('w') as a,(out/'logs'/f'{f.stem}.stderr').open('w') as b:
  r=subprocess.run(cmd,cwd=work,stdout=a,stderr=b,env={**os.environ,'LEAN_NUM_THREADS':'1'})
 row={'file':f.name,'argv':cmd,'cwd':str(work),'exit-status':r.returncode,'seconds':round(time.monotonic()-start,3)};rows.append(row)
 (out/'axiom-receipts.json').write_text(json.dumps(rows,indent=2)+'\n');print(json.dumps(row),flush=True)
 if r.returncode:break
