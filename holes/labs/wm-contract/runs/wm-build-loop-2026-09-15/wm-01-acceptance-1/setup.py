from pathlib import Path
import subprocess, json, hashlib, tempfile, tarfile, io, shutil
repo=Path('/home/joe/code/mathlib4'); out=Path(__file__).resolve().parent
subject='480a666ad27c18477b4b9df86e11862be3930d50'
def git(*args): return subprocess.check_output(['git','-C',str(repo),*args])
changed=git('diff-tree','--no-commit-id','--name-only','-r',subject).decode().splitlines()
(out/'repair.diff').write_bytes(git('show',subject))
head=git('rev-parse','HEAD').decode().strip()
assert git('merge-base','--is-ancestor',subject,head)==b''
for name in ['lean-toolchain','lakefile.lean','lake-manifest.json',*changed]:
 assert git('show',subject+':'+name)==(repo/name).read_bytes(),name
# Source transitive closure: collect every module import in subject source.
closure=set(); queue=[p[:-5].replace('/','.') for p in changed]
while queue:
 mod=queue.pop()
 if mod in closure: continue
 closure.add(mod)
 p=mod.replace('.','/')+'.lean'
 try: bs=git('show',subject+':'+p)
 except subprocess.CalledProcessError: continue
 assert bs==(repo/p).read_bytes(),p
 for line in bs.decode().splitlines():
  line=line.strip()
  if line.startswith(('import ','public import ','private import ')):
   for dep in line.split('import ',1)[1].split():
    if dep.startswith(('Mathlib.','DarkTower.')): queue.append(dep)
assert not any(x in closure for x in ['DarkTower.WarMachine.CascadeEFE','DarkTower.WarMachine.CascadeEFEPolicies','DarkTower.WarMachine.GOverCascades'])
manifest={p:hashlib.sha256(git('show',subject+':'+p)).hexdigest() for p in changed}
(out/'source-manifest.json').write_text(json.dumps({'subject':subject,'canonical-head':head,'files':manifest,'import-closure':sorted(closure),'descendant-diff':git('diff','--name-only',subject,head).decode().splitlines()},indent=2)+'\n')
# No canonical Lake execution. Archive source, copy caches/packages to new disk files.
work=Path(tempfile.mkdtemp(prefix='wm01-acc-480a-'))
(out/'workspace.txt').write_text(str(work)+'\n')
archive=git('archive',subject)
with tarfile.open(fileobj=io.BytesIO(archive)) as tar: tar.extractall(work,filter='data')
r=subprocess.run(['cp','-a','--reflink=auto',str(repo/'.lake'),str(work/'.lake')])
assert r.returncode==0
for p in (work/'.lake').rglob('*'):
 if p.is_symlink(): assert str(p.resolve()).startswith(str(work)),str(p)
# Remove copied repair module products to force fresh compilation of all 38.
removed=[]
for src in changed:
 stem=src[:-5]
 for subtree in ['lib/lean','ir']:
  parent=work/'.lake/build'/subtree/Path(stem).parent
  if parent.exists():
   for p in parent.glob(Path(stem).name+'.*'):
    if p.is_file(): p.unlink(); removed.append(str(p.relative_to(work)))
(out/'setup.json').write_text(json.dumps({'workspace':str(work),'cache-copy-exit':r.returncode,'removed-products':removed,'package-link-check':'all symlinks remain inside isolated copy'},indent=2)+'\n')
print(json.dumps({'workspace':str(work),'review-files':len(changed),'closure-modules':len(closure),'removed-products':len(removed)}),flush=True)
