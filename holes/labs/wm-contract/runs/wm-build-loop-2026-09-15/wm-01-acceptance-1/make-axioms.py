from pathlib import Path
import json,re,subprocess,difflib
out=Path(__file__).resolve().parent;work=Path((out/'workspace.txt').read_text().strip());m=json.loads((out/'source-manifest.json').read_text());targets=[]
for p in m['files']:
 if p.endswith('ProbabilityKernelRepairNegative.lean'):continue
 now=(work/p).read_text().splitlines();old=subprocess.check_output(['git','-C','/home/joe/code/mathlib4','show',m['subject']+'^:'+p]).decode().splitlines()
 names=[]; ns=[]; current=None
 for i,line in enumerate(now):
  q=re.match(r'^namespace (\S+)',line)
  if q:ns.append(q[1])
  q=re.match(r'^end (\S+)',line)
  if q and ns and q[1]==ns[-1]:ns.pop()
  q=re.match(r'^(?:(?:private|noncomputable|protected) )*(?:def|theorem|structure|abbrev) (\S+)',line)
  if q:current='.'.join([*ns,q[1]])
  names.append(current)
 changed=set()
 for tag,a,b,c,d in difflib.SequenceMatcher(a=old,b=now).get_opcodes():
  if tag!='equal':
   for i in range(c,d):
    if names[i]:changed.add(names[i])
 for n in sorted(changed):targets.append({'file':p,'declaration':n})
(out/'axiom-targets.json').write_text(json.dumps(targets,indent=2)+'\n')
# Emit one probe per source file; negative fixture modules reuse unqualified names.
from collections import defaultdict
groups=defaultdict(list)
for t in targets: groups[t['file']].append(t['declaration'])
body = """  let env ← getEnv
  for user in wanted do
    let found := env.constants.toList.filter fun (n, _) => (privateToUserName n).toString == user
    if found.isEmpty then throwError "Missing reviewed declaration: {user}"
    for (n, _) in found do
      let id := mkIdent n
      elabCommand (← `(#print axioms $id))
      let deps ← collectAxioms n
      for a in deps do
        unless [``propext, ``Classical.choice, ``Quot.sound].contains a do
          logError m!"Unexpected axiom in {n}: {a}"
#review_axioms
"""
for src,names in groups.items():
 text='import '+src[:-5].replace('/','.')+'\nopen Lean Elab Command\nelab "#review_axioms" : command => do\n  let wanted : List String := '+json.dumps(names)+'\n'+body
 (out/('Axioms-'+Path(src).stem+'.lean')).write_text(text)
print('selected',len(targets),'changed declarations')
