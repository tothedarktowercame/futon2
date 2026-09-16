import pathlib, tempfile, json, hashlib, shutil, subprocess
repo=pathlib.Path('/home/joe/code/futon2')
r=repo/'holes/labs/wm-contract/runs/wm-09-predecessor-history-2026-09-16'
census=pathlib.Path('/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/revision-21-inputs/archive-census.json')
rows=json.loads(census.read_text())['files']; assert len(rows)==24
base=repo/'data/wm-full-loop'
# Only the three counterpart identities specifically compared in K9, not a new corpus census.
for n in [1,2,24]:
 p=base/f'wm-outer-loop-40-v1/attempt-{n:03}/007-closed.edn'
 rows.append({'path':str(p),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'scope':'named nonarchive counterpart, fresh byte pin'})
with tempfile.TemporaryDirectory(prefix='wm09-k9-structure-') as tmp:
 tmp=pathlib.Path(tmp); root=tmp/'copy'; verified=[]
 for row in rows:
  close=pathlib.Path(row['path']); assert hashlib.sha256(close.read_bytes()).hexdigest()==row['sha256']
  dest=root/close.parent.relative_to(base)
  for f in close.parent.rglob('*'): assert not f.is_symlink()
  shutil.copytree(close.parent,dest)
  for f in close.parent.rglob('*'):
   if f.is_file():
    c=dest/f.relative_to(close.parent); digest=hashlib.sha256(f.read_bytes()).hexdigest()
    assert digest==hashlib.sha256(c.read_bytes()).hexdigest()
    verified.append({'original':str(f),'copy':str(c),'sha256':digest})
 source=tmp/'source.clj';source.write_bytes(subprocess.check_output(['git','show','96d5b5e579c79cd0151bcfe9de820c9de4710feb:src/futon2/aif/receipt_construction.clj'],cwd=repo))
 with (r/'k9r2-archive-structure.log').open('w') as log:
  p=subprocess.run(['clojure','-M:test',str(r/'k9r2-archive-structure.clj'),str(root),str(source),str(r/'K9R2-ARCHIVE-STRUCTURE.edn')],cwd=repo,stdout=log,stderr=subprocess.STDOUT)
 for row in verified: assert hashlib.sha256(pathlib.Path(row['original']).read_bytes()).hexdigest()==row['sha256']
 (r/'K9R2-ARCHIVE-COPY.json').write_text(json.dumps({'source':'96d5b5e579c79cd0151bcfe9de820c9de4710feb','verified_files':verified,'exit':p.returncode,'production_bytes_unchanged':True,'rebindings':0,'scope':'structure only; admission not attempted; no manifest rewriting','temporary_copy_removed_on_exit':True},indent=2)+'\n')
 assert p.returncode==0
