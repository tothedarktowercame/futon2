"""Pin imported sources, independently decode doubles, generate concrete Lean controls."""
import csv, hashlib, json, pathlib, re, subprocess, sys
from fractions import Fraction
R=pathlib.Path(__file__).resolve().parent
M=pathlib.Path('/home/joe/code/mathlib4')
F=pathlib.Path('/home/joe/code/futon2')
SUBJECT='480a666ad27c18477b4b9df86e11862be3930d50'
OLD='4750f9fa17eb09bb3d70c0ae12e4256cf3dcf5ec'
def git(repo,*args): return subprocess.check_output(['git','-C',str(repo),*args])
def sha(b): return hashlib.sha256(b).hexdigest()
def blob(commit,path): return git(M,'show',f'{commit}:{path}')
def check_identity(path, candidate, expected):
    if sha(candidate) != sha(expected):
        raise ValueError(f'stale-source-hash: {path}: candidate={sha(candidate)}, pinned={sha(expected)}')
entry='DarkTower.WarMachine.MachineBeliefDistribution'
if '--stale' in sys.argv:
    for name in ['MachineBeliefDistribution','Holes']:
        path=f'DarkTower/WarMachine/{name}.lean'
        try:
            check_identity(path,blob(OLD,path),blob(SUBJECT,path))
        except ValueError as error:
            print('REJECT',error)
        else:
            raise AssertionError('stale control unexpectedly accepted')
    sys.exit(1)
# Traverse source import closure; Lean/Std are toolchain identities rather than repo sources.
pending=[entry]; records={}; external=set()
while pending:
    module=pending.pop()
    if module in records or module in external: continue
    path=module.replace('.','/')+'.lean'
    if not (M/path).exists():
        external.add(module); continue
    data=blob(SUBJECT,path)
    check_identity(path,(M/path).read_bytes(),data)
    # Remove block comments before extracting imports (including nested comments).
    text=data.decode(); clean=[]; depth=0; i=0
    while i<len(text):
        if text[i:i+2]=='/-': depth+=1; i+=2
        elif depth and text[i:i+2]=='-/': depth-=1; i+=2
        else:
            if not depth: clean.append(text[i])
            i+=1
    imports=[]
    for line in ''.join(clean).splitlines():
        match=re.match(r'\s*(?:public\s+|private\s+)?import\s+(.+)',line)
        if match: imports.extend(x for x in match[1].split('--')[0].split() if x != 'all')
    records[module]={'path':path,'sha256':sha(data),'imports':imports}
    pending.extend(imports)
assert not ({'DarkTower.WarMachine.CascadeEFE','DarkTower.WarMachine.CascadeEFEPolicies','DarkTower.WarMachine.GOverCascades'} & records.keys())
runtime={}
for path in ['src/futon2/aif/machine_belief.clj','src/futon2/aif/machine_model.clj']:
    data=git(F,'show',f'9aad9adf:{path}')
    assert data==(F/path).read_bytes()
    runtime[path]=sha(data)
manifest={'subject':SUBJECT,'canonical-head':git(M,'rev-parse','HEAD').decode().strip(),
          'toolchain':blob(SUBJECT,'lean-toolchain').decode().strip(),
          'source-closure':records,'external-imports':sorted(external),'runtime-source':runtime,
          'historical-object-type':git(M,'cat-file','-t',OLD).decode().strip(),
          'historical-to-repair-commits':int(git(M,'rev-list','--count',f'{OLD}..{SUBJECT}'))}
assert git(M,'merge-base',OLD,SUBJECT).decode().strip()==OLD
for path in ['lean-toolchain','lakefile.lean','lake-manifest.json']:
    assert blob(SUBJECT,path)==(M/path).read_bytes()
(R/'source-identities.json').write_text(json.dumps(manifest,indent=2)+'\n')
rows=list(csv.DictReader((R/'coordinates.tsv').open(),delimiter='\t'))
assert [x['state'] for x in rows]==['spawned','refined','strengthened','addressed','falsified','foreclosed','reopened']
values={x['state']:Fraction(float(x['double'])) for x in rows}
assert all(Fraction(float.fromhex(x['hex']))==values[x['state']] for x in rows)
total=sum(values.values())
assert total==Fraction((R/'runtime-total.txt').read_text())
assert total==Fraction(36028797018963969,36028797018963968)
assert 0 < total-1 <= Fraction(1,10**12)
assert sum(Fraction(x['exact']) for x in rows)==1
(R/'independent-arithmetic.json').write_text(json.dumps({'method':'Python Fraction(float(decimal)) cross-checked against float.fromhex; independent of runtime numeric admission',
 'values':{s:str(v) for s,v in values.items()},'total':str(total),'deviation':str(total-1)},indent=2)+'\n')
def term(f):
    f=Fraction(f)
    return f'({f.numerator} : ℝ) / {f.denominator}'
base='''import DarkTower.WarMachine.MachineBeliefDistribution
open DarkTower.WarMachine.Holes
open DarkTower.WarMachine.MachineBeliefState
open DarkTower.WarMachine.MachineBeliefDistribution
namespace Row7Binding
noncomputable def exactPosterior : Posterior
'''
base+=''.join(f"  | .{x['state']} => {term(x['exact'])}\n" for x in rows)
base+='''theorem exact_nonnegative : ∀ s, 0 ≤ exactPosterior s := by
  intro s; cases s <;> norm_num [exactPosterior]
theorem exact_Normalised : Normalised exactPosterior := by
  norm_num [Normalised, Status.all, exactPosterior]
noncomputable def kernel := selectedKernel exactPosterior exact_nonnegative exact_Normalised
def context : EntityContext := ⟨0, "wm-production", "2026-09-12-row-7"⟩
noncomputable def stored : machineBeliefState := fun e => if e = 0 then some exactPosterior else none
theorem selected : stored context.entity = some exactPosterior := by simp [stored, context]
theorem conserved : stored context.entity = some exactPosterior ∧
    ∀ s, kernel.mass () s = exactPosterior s :=
  selectedPosterior_conserved stored context exactPosterior selected exact_nonnegative exact_Normalised
theorem kernel_normalised : (kernel.support () |>.map (kernel.mass ())).sum = 1 :=
  selectedKernel_normalised exactPosterior exact_nonnegative exact_Normalised
'''
for x in rows:
    s=x['state']
    base+=f'''theorem coordinate_{s} : kernel.mass () .{s} = {term(x['exact'])} := by
  exact selectedKernel_coordinate exactPosterior exact_nonnegative exact_Normalised .{s}
'''
base+='noncomputable def prodPosterior : Posterior\n'
base+=''.join(f'  | .{s} => {term(v)}\n' for s,v in values.items())
base+='''theorem production_not_Normalised : ¬ Normalised prodPosterior := by
  norm_num [Normalised, Status.all, prodPosterior]
'''
axioms=['exact_nonnegative','exact_Normalised','selected','conserved','kernel_normalised',*[f"coordinate_{x['state']}" for x in rows],'production_not_Normalised']
positive=base+'\n'+''.join(f'#print axioms {n}\n' for n in axioms)
positive+=''.join(f'#print axioms DarkTower.WarMachine.MachineBeliefDistribution.{n}\n' for n in ['selectedPosterior_conserved','selectedKernel_coordinate','selectedKernel_normalised'])
(R/'Positive.lean').write_text(positive+'end Row7Binding\n')
# A swapped association changes both named equations while retaining total one.
swap=base+f'''theorem swapped_coordinates : kernel.mass () .spawned = {term(rows[1]['exact'])} ∧
    kernel.mass () .refined = {term(rows[0]['exact'])} := by
  constructor
  · refine ?spawned_coordinate_equation
    all_goals norm_num [kernel, selectedKernel, exactPosterior]
  · refine ?refined_coordinate_equation
    all_goals norm_num [kernel, selectedKernel, exactPosterior]
end Row7Binding
'''
(R/'Swapped.lean').write_text(swap)
(R/'FailedNormalisation.lean').write_text(base+'''theorem attempted_production_Normalised : Normalised prodPosterior := by
  refine ?Normalised_premise
  all_goals norm_num [Normalised, Status.all, prodPosterior]
end Row7Binding
''')
print(f'PASS: {len(records)} source modules byte-identical at repaired subject; external imports {sorted(external)}')
print('PASS runtime source pins; exact fixture total 1; production exact total',total)
