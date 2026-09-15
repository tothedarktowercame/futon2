import hashlib,json,pathlib,shutil,subprocess
R=pathlib.Path(__file__).resolve().parent; M=pathlib.Path('/home/joe/code/mathlib4'); F=pathlib.Path('/home/joe/code/futon2')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
W=pathlib.Path((R/'workspace.txt').read_text().strip())
changed=json.loads((R/'changed-sources.json').read_text())
for path,digest in changed.items():
 assert sha(M/path)==digest==sha(W/path)
 assert 'sorry' not in (M/path).read_text()
runtime=F/'src/futon2/aif/machine_model.clj'
assert sha(runtime)=='462aae432397e21209c238685d1aa39d040f15b330e2baa5f771d4858eb66b77'
for name in ['Positive','Axioms']:
 assert 'sorryAx' not in (R/f'logs/{name}.stdout').read_text()
for name,goal in [('Duplicate','support_nodup'),('Hidden','mass_eq_zero_of_not_mem.true'),('OutsideBound','nearNormalised'),('RetainedConversion','exact_total_one_premise')]:
 assert 'case '+goal in (R/f'logs/{name}.stdout').read_text()
 assert json.loads((R/f'logs/{name}.json').read_text())['exit']==1
old='02bc5c00a8dc7d5dde2e861d647d805f5023f36eda46a12f4d3ea102d407e34c'
record={'mathlib-commit':subprocess.check_output(['git','-C',str(M),'rev-parse','HEAD']).decode().strip(),
 'runtime-sha256':sha(runtime),'registry-sha256-read-only':sha(F/'checks/witness-registry.edn'),
 'successor-binding':{'id':'R4-forward-model-float-carried-production-pins-v1',
  'old-artifact-sha256':old,'new-artifact-sha256':changed['DarkTower/WarMachine/MachineForwardModelWitness.lean'],
  'old-Holes-sha256':'4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b',
  'current-Holes-sha256':sha(M/'DarkTower/WarMachine/Holes.lean'),
  'new-MachineModelSpec-sha256':changed['DarkTower/WarMachine/MachineModelSpec.lean'],
  'declarations':['MachineModelSpec.FloatCarriedRow','MachineModelSpec.FloatCarriedRow.support_ne_nil',
   'MachineForwardModelWitness.advanceTwiceRow','MachineForwardModelWitness.cascadeRow',
   'MachineForwardModelWitness.productionPinnedFloatCarried'],
  'scope':'Formal witness successor only: new support laws discharged, existing coordinates and theorem statements unchanged. No renewed production observation/composition correspondence acceptance; registry update requires codex-28 decision.'},
 'binding-1':{'commit':'daa263c8de977f3a88052f66a59fab38ee65d419','subject':'480a666ad27c18477b4b9df86e11862be3930d50',
  'reused-files':{n:sha(R.parent/'wm-01-binding-1'/n) for n in ['Positive.lean','independent-arithmetic.json']},
  'scope':'Historical source pins remain valid for 480a666ad2; they do not describe this new source.'}}
shutil.rmtree(W)
record['cleanup']={'removed':str(W),'exists-after':W.exists()}
(R/'successor-binding.json').write_text(json.dumps(record,indent=2)+'\n')
print(json.dumps(record,indent=2))
