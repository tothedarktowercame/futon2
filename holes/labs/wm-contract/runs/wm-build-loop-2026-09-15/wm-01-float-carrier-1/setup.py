import hashlib,json,pathlib,subprocess,tempfile,tarfile,io
R=pathlib.Path(__file__).resolve().parent
M=pathlib.Path('/home/joe/code/mathlib4')
def git(*args): return subprocess.check_output(['git','-C',str(M),*args])
assert not git('status','--porcelain')
assert git('branch','--show-current').decode().strip()=='darktower'
files=['MachineModelSpec','MachineForwardModelWitness']
record={'head':git('rev-parse','HEAD').decode().strip(),'before':{n:hashlib.sha256((M/f'DarkTower/WarMachine/{n}.lean').read_bytes()).hexdigest() for n in files}}
# Bounded DarkTower graph, including transitive dependents and wrapper controls.
graph={str(p.relative_to(M).with_suffix('')).replace('/','.'): [line.split()[1] for line in p.read_text().splitlines() if line.startswith('import ')] for p in (M/'DarkTower').rglob('*.lean')}
def closure(module):
    seen=set(); todo=[module]
    while todo:
        item=todo.pop()
        if item in seen: continue
        seen.add(item); todo.extend(graph.get(item,[]))
    return seen
spec='DarkTower.WarMachine.MachineModelSpec'
record['dependents']=sorted(m for m in graph if m!=spec and spec in closure(m))
record['predictive-wrapper-closures']={m:sorted(closure('DarkTower.WarMachine.'+m)) for m in ['PredictiveOutcomeKernelWitness','PredictiveOutcomeKernelUnconditionalNegative','PredictiveOutcomeKernelSoftmaxNegative']}
for c in record['predictive-wrapper-closures'].values(): assert spec not in c
# Retain the actual names from the wrapper for reviewer cross-check.
record['wrapper-text']=(pathlib.Path('/home/joe/code/futon2/checks/predictive_outcome_kernel_witness.clj')).read_text()
W=pathlib.Path(tempfile.mkdtemp(prefix='wm-float-carrier-'))
(R/'workspace.txt').write_text(str(W)+'\n')
with tarfile.open(fileobj=io.BytesIO(git('archive','HEAD'))) as t: t.extractall(W,filter='data')
subprocess.run(['cp','-a','--reflink=auto',str(M/'.lake'),str(W/'.lake')],check=True)
assert (M/'.lake/build/lib/lean/DarkTower/WarMachine/MachineModelSpec.olean').stat().st_ino != (W/'.lake/build/lib/lean/DarkTower/WarMachine/MachineModelSpec.olean').stat().st_ino
for p in (W/'.lake').rglob('*'):
    if p.is_symlink(): assert p.resolve().is_relative_to(W),str(p)
record['workspace']=str(W)
(R/'preflight.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
