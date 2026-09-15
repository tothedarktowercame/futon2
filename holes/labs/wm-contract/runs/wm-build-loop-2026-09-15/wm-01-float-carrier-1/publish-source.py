import hashlib,json,pathlib,shutil,subprocess
R=pathlib.Path(__file__).resolve().parent; M=pathlib.Path('/home/joe/code/mathlib4')
W=pathlib.Path((R/'workspace.txt').read_text().strip()); before=json.loads((R/'preflight.json').read_text())
assert subprocess.check_output(['git','-C',str(M),'rev-parse','HEAD']).decode().strip()==before['head']
assert not subprocess.check_output(['git','-C',str(M),'status','--porcelain'])
files=[f'DarkTower/WarMachine/{n}.lean' for n in ['MachineModelSpec','MachineForwardModelWitness','FloatCarriedRowCorrespondence']]
for p in files: shutil.copyfile(W/p,M/p)
(R/'changed-sources.json').write_text(json.dumps({p:hashlib.sha256((M/p).read_bytes()).hexdigest() for p in files},indent=2)+'\n')
print('Copied tested source bytes into clean canonical checkout:',files)
