"""Offline scores from the actual classifier export, with declared model limits."""
import json, math, itertools, collections
from fractions import Fraction as F
from pathlib import Path
p=Path(__file__).resolve().parent
x=json.loads((p/'adapter-output.json').read_text())
rows=x['results']
union={r['name']:r for arm in rows for r in arm['classified'] if r['action']=='kill'}
assert len({r['name'] for r in x['packet']['buffers']})==len(x['packet']['buffers'])
ids=sorted(union)
summary=[]
for arm in rows:
 byname={r['name']:r for r in arm['classified']}
 channels=[]
 for name in ids:
  r=byname[name]; parameter=union[name]
  q=float(parameter['prior-needed-later']) if r['action']=='kill' else 0.
  cost=float(parameter['revisit-cost']); assert 0<=q<=1 and cost>=0
  z=1+math.exp(-cost); c=[1/z,math.exp(-cost)/z]; dist=[1-q,q]
  h=-sum(a*math.log(a) for a in dist if a)
  risk=sum(a*math.log(a/b) for a,b in zip(dist,c) if a)
  assert h>=0 and risk>=-1e-12
  if q in (0,1): assert h==0
  channels.append(dict(name=name,kind=parameter['kind'],action=r['action'],Q=dist,C=c,
                       prior_source=parameter['source'],risk=risk,ambiguity=h,cost=cost,
                       expected_cost=str(F(str(q))*F(str(cost)))))
 risk=sum(c['risk'] for c in channels); h=sum(c['ambiguity'] for c in channels)
 mass=jr=jh=0.
 for outcome in itertools.product([0,1],repeat=len(channels)):
  q=math.prod(c['Q'][o] for c,o in zip(channels,outcome))
  cp=math.prod(c['C'][o] for c,o in zip(channels,outcome))
  mass+=q
  if q: jr+=q*math.log(q/cp); jh-=q*math.log(q)
 assert math.isclose(mass,1,abs_tol=1e-12)
 assert math.isclose(jr,risk,abs_tol=1e-12) and math.isclose(jh,h,abs_tol=1e-12)
 exp=sum((F(c['expected_cost']) for c in channels),F(0))
 baseline=sum(math.log1p(math.exp(-c['cost'])) for c in channels)
 assert math.isclose(risk+h,float(exp)+baseline,abs_tol=1e-12)
 fuel=x['kill-args']['fuel']; n=arm['meters']['scanned']; kills=arm['meters']['kills-proposed']
 f=F(str(fuel['per-scan']))*n+F(str(fuel['per-kill']))*kills
 summary.append(dict(wiring=arm['wiring'],channels=channels,meters=arm['meters'],
  G_B=risk+h,G_A_projection=risk+h,risk=risk,ambiguity=h,expected_loss=str(exp),fuel=str(f),
  total=risk+h+float(f),threshold=x['yield-args']['clean-enough-threshold'],
  threshold_eligible=arm['meters']['remaining']<=x['yield-args']['clean-enough-threshold'],
  rule_verdict='unavailable: no observed revisit truth',joint_check='passed'))
ages=[dict(kind=r['kind'],age=r['display-age-seconds']) for r in x['packet']['buffers']]
result=dict(scope='real retained snapshot; constructed probabilities/costs; dry-run only',
 model='shared candidate-union false-kill outcome projection; independent likelihood; fixed current state',
 full_A='unresolved; displayed A projection is not a ruled full-A formula',
 comparison='not distinguishable on this field',
 choice='no threshold-eligible wiring' if not any(r['threshold_eligible'] for r in summary) else 'predicted choice only; truth unavailable',
 arms=summary,age_distribution=ages,kind_counts=dict(collections.Counter(r['kind'] for r in x['packet']['buffers'])),
 threshold_prior='not identifiable from age marginal; use declared conditional table only')
(p/'results.json').write_text(json.dumps(result,indent=2,allow_nan=False)+'\n')
print(json.dumps({**result,'arms':[{k:v for k,v in r.items() if k!='channels'} for r in summary],'age_distribution':'see results.json'},indent=2))
