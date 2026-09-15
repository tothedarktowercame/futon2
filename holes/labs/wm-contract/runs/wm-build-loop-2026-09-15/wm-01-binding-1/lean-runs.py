"""Read-only Lean runs and before/after canonical cache mutation checks."""
import json, os, pathlib, subprocess, sys
R=pathlib.Path(__file__).resolve().parent
M=pathlib.Path('/home/joe/code/mathlib4')
def run(label, command):
    p=subprocess.run([sys.executable,str(R/'run.py'),label,str(M),*command])
    return p.returncode
packages=[]
for p in json.loads((M/'lake-manifest.json').read_text())['packages']:
    d=M/'.lake/packages'/p['name']
    head=subprocess.check_output(['git','-C',str(d),'rev-parse','HEAD']).decode().strip()
    assert head==p['rev'], (p['name'],head,p['rev'])
    dirty=subprocess.check_output(['git','-C',str(d),'diff','HEAD','--','*.lean','*.toml','*.json'])
    assert not dirty, p['name']
    packages.append({'name':p['name'],'revision':head})
(R/'package-identities.json').write_text(json.dumps(packages,indent=2)+'\n')
marker=R/'cache-before.marker'
assert run('marker',['touch',str(marker)])==0
assert run('cache-before',['find','.lake','-newer',str(marker)])==0
assert not (R/'logs/cache-before.stdout').read_bytes()
assert run('lean-version',['lean','--version'])==0
assert run('lake-version',['lake','--version'])==0
os.environ['LEAN_NUM_THREADS']='1'
for label,file,expected in [('positive','Positive.lean',0),('swapped','Swapped.lean',1),('normalisation','FailedNormalisation.lean',1)]:
    code=run(label,['lake','env','lean','-j','1',str(R/file)])
    assert code==expected, (label,code)
assert run('cache-after',['find','.lake','-newer',str(marker)])==0
assert not (R/'logs/cache-after.stdout').read_bytes(), 'canonical cache changed'
print('PASS: expected control exits; zero canonical .lake entries newer than pre-run marker')
