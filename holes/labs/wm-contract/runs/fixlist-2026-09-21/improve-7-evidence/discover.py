"""Read-only declared diagnostic rule; pins inputs, does not query live services."""
import collections, datetime, hashlib, json, pathlib, re, subprocess
import numpy as np
ROOT=pathlib.Path('/home/joe/code')
REPOS=['futon0','futon2','futon3c','futon4','futon5a','futon6','mathlib4','apm-lean']
def git(repo,*args): return subprocess.check_output(['git','-C',str(ROOT/repo),*args],text=True)
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
# Attribution features are declared, not learned topic names. One commit may
# touch multiple facets; count fractional credit, no merges or line-count weight.
def facets(paths):
 out=set()
 for p in paths:
  if re.search(r'(^|/)(wm-contract|WarMachine|aif)(/|$)|(^|/)(war_machine|wm_|M-war-machine|M-wm-|M-G-wm|M-aif-policy)',p):out.add('WM')
  if re.search(r'(^|/)(apm|apm-lean)(/|$)|(^|/)(M-apm-|apm_|apm-|countdown_manifest)|^problems/',p):out.add('APM')
 return out or {'other/unattributed'}
heads=json.loads((pathlib.Path(__file__).parent/'heads.json').read_text())
activity={}
for day,end in [('2026-09-20','2026-09-21T00:00:00Z'),('2026-09-21','2026-09-21T17:31:44Z')]:
 counts=collections.Counter(); missions=collections.Counter(); rows=[]
 for repo in REPOS:
  data=git(repo,'log',heads[repo],'--no-merges','--since='+day+'T00:00:00Z','--until='+end,'--format=COMMIT %H %cI','--name-only')
  for block in data.split('COMMIT ')[1:]:
   ls=block.splitlines(); header=ls[0].split(); paths=[p for p in ls[1:] if p]; fs=facets(paths)
   for f in fs:counts[f]+=1/len(fs)
   ms=sorted(set(m for p in paths for m in re.findall(r'(?:^|/)(M-[^/]+)\.md$',p)))
   missions.update(ms); rows.append({'repo':repo,'sha':header[0],'at':header[1],'facets':sorted(fs),'missions':ms})
 activity[day]={'until':end,'commits':len(rows),'facet_credit':dict(counts),'top_mission_touches':missions.most_common(12),'rows':rows,'focus':max(counts,key=counts.get)}
folder=ROOT/'futon6/data/mission-structure-embed';j=json.loads((folder/'mission-embed.json').read_text()); a=np.load(folder/'structure-embeddings.npy');a=a/np.linalg.norm(a,axis=1,keepdims=True);stems=j['stems']
queries=['war-machine','apm-solutions','expressions-of-interest','action-cost-modelling','aif4iad','futon-forward-model','futonzero-mvp','futonzero-generative','self-documenting-stack','canon-fingerprint-store']
embedding={}
for q in queries:
 if q not in stems:embedding[q]={'status':'absent'};continue
 i=stems.index(q);scores=a@a[i];indices=sorted(range(len(stems)),key=lambda k:(-float(scores[k]),stems[k]));indices=[k for k in indices if k!=i][:5]
 embedding[q]={'wm_cosine':float(scores[stems.index('war-machine')]),'top5':[(stems[k],float(scores[k])) for k in indices]}
paths=[folder/'mission-embed.json',folder/'structure-embeddings.npy',ROOT/'futon6/data/mission-carpet-pos-embed.json',ROOT/'futon6/data/mission-wholeness.edn',ROOT/'futon6/data/fold-embed/manifest.json']
result={'rule':'commit-facets-v1; 24h, no merges, fractional commit credit, WM/APM unmerged unless explicit semantic bridge','heads':heads,'activity':activity,'embedding':embedding,'structure_shape':list(a.shape),'pins':{str(p):{'sha256':digest(p),'mtime':datetime.datetime.fromtimestamp(p.stat().st_mtime,datetime.timezone.utc).isoformat()} for p in paths}}
print(json.dumps(result,indent=2))
