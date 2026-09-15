import json,pathlib,subprocess
R=pathlib.Path(__file__).resolve().parent; M=pathlib.Path('/home/joe/code/mathlib4')
modules=['MachineModelSpec','MachineBeliefDistribution','MachineForwardModelWitness','MachineParameters','MachinePredictiveOutcome','MachinePreferenceDistribution','Row16ForwardModelAxiomCheck','FloatCarriedRowCorrespondence']
def run(label,*command):
 p=subprocess.run(['python3',str(R/'run.py'),label,str(M),*command]); return p.returncode
assert run('lean-version','lean','--version')==0
assert run('lake-version','lake','--version')==0
assert run('source-commit','git','show','--stat','--oneline','HEAD')==0
marker=R/'canonical-build.marker'
assert run('canonical-marker','touch',str(marker))==0
code=run('canonical-build','env','LEAN_NUM_THREADS=1','lake','build',*['DarkTower.WarMachine.'+m for m in modules])
assert run('canonical-written','find','.lake','-type','f','-newer',str(marker))==0
written=(R/'logs/canonical-written.stdout').read_text().splitlines()
unexpected=[p for p in written if not any('/DarkTower/WarMachine/'+m+'.' in p for m in modules)]
(R/'canonical-cache.json').write_text(json.dumps({'modules':modules,'files-written':written,'unexpected-files':unexpected,'exit':code},indent=2)+'\n')
assert code==0,code
assert not unexpected,unexpected
print('Canonical cache aligned; files written:',len(written))
