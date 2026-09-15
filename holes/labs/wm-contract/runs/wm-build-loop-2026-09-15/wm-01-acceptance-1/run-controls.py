from pathlib import Path
import subprocess,json,time,os
out=Path(__file__).resolve().parent; work=Path((out/'workspace.txt').read_text().strip());rows=[]
for src in [out/'Valid.lean',*sorted(out.glob('Invalid*.lean'))]:
 cmd=['lake','env','lean','-j','1',str(src)];start=time.monotonic()
 with (out/'logs'/f'{src.stem}.stdout').open('w') as a,(out/'logs'/f'{src.stem}.stderr').open('w') as b:
  r=subprocess.run(cmd,cwd=work,stdout=a,stderr=b,env={**os.environ,'LEAN_NUM_THREADS':'1'})
 row={'file':src.name,'argv':cmd,'cwd':str(work),'exit-status':r.returncode,'expected-exit':0 if src.name=='Valid.lean' else 1,'seconds':round(time.monotonic()-start,3)};rows.append(row)
 (out/'control-receipts.json').write_text(json.dumps(rows,indent=2)+'\n');print(json.dumps(row),flush=True)
