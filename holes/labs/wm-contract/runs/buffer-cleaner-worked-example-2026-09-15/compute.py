"""Constructed comparison. No Emacs access. Natural logs; exact costs via Fraction."""
import hashlib
import itertools
import json
import math
from fractions import Fraction as F
from pathlib import Path

ROOT = Path(__file__).resolve().parent
P = json.loads((ROOT / 'parameters.json').read_text())

def frac(x):
    return F(str(x))

def entropy(ps):
    return -sum(p * math.log(p) for p in ps if p > 0)

def kl(q, c):
    assert all(x > 0 for x in c)
    return sum(x * math.log(x / y) for x, y in zip(q, c) if x > 0)

def compute(name, wiring):
    expected, actual = F(0), F(0)
    coords, killed = [], []
    for b in P['buffers']:
        kill = b['kind'] in wiring['categories'] and (
            b['kind'] != 'file' or b['age_hours'] > wiring['file_age_hours'])
        if kill:
            killed.append(b['id'])
        p = frac(b['p_needed']) if kill else F(0)
        c = frac(b['recovery_cost'])
        expected += p * c
        actual += c if kill and b['revisited'] else 0
        z = 1 + math.exp(-float(c))
        pref = [1 / z, math.exp(-float(c)) / z]
        q = [float(1-p), float(p)]
        assert math.isclose(sum(pref), 1) and sum(q) == 1
        coords.append(dict(id=b['id'], killed=kill, q=q, C=pref,
                           risk=kl(q, pref), ambiguity=entropy(q),
                           expected_cost=str(p*c)))
    # Independent direct JOINT evaluation, not just sum of coordinate scores.
    choices = [[(i, p) for i, p in enumerate(r['q']) if p > 0] for r in coords]
    joint_mass, joint_risk, joint_h = 0., 0., 0.
    for outcome in itertools.product(*choices):
        q = math.prod(p for _, p in outcome)
        c = math.prod(r['C'][i] for r, (i, _) in zip(coords, outcome))
        joint_mass += q
        joint_risk += q * math.log(q/c)
        joint_h -= q * math.log(q)
    risk = sum(r['risk'] for r in coords)
    h = sum(r['ambiguity'] for r in coords)
    g = risk + h
    baseline = sum(math.log1p(math.exp(-b['recovery_cost'])) for b in P['buffers'])
    assert math.isclose(joint_mass, 1, abs_tol=1e-12)
    assert math.isclose(joint_risk, risk, abs_tol=1e-12)
    assert math.isclose(joint_h, h, abs_tol=1e-12)
    assert math.isclose(g, float(expected)+baseline, abs_tol=1e-12)
    assert risk >= -1e-12 and h >= 0
    fuel = frac(P['scan_cost_per_buffer'])*len(coords) + frac(P['kill_cost'])*len(killed)
    remaining = len(coords)-len(killed)
    return dict(wiring=name, killed=killed, remaining=remaining,
                eligible=remaining <= P['remaining_threshold'],
                risk=risk, ambiguity=h, G_B=g, G_A_illustration=g,
                A_status='deterministic-bookkeeping-projection; full-A-undetermined',
                expected_recovery_cost=str(expected), realized_recovery_cost=str(actual),
                fuel=str(fuel), predicted_A_total=g+float(fuel), predicted_B_total=g+float(fuel),
                oracle_realized_total=str(actual+fuel), constant_preference_baseline=baseline,
                coordinates=coords, direct_joint_mass=joint_mass,
                direct_joint_risk=joint_risk, direct_joint_ambiguity=joint_h)

rows = [compute(n,w) for n,w in P['wirings'].items()]
assert len(P['buffers']) == 24
assert [len(r['killed']) for r in rows] == [16,8]
assert all(r['eligible'] for r in rows)
# Zero-revisit control: Q=delta_0, so H=0 and KL=log Z, not an extra noise charge.
for c in [.5, 1, 2, 5]:
    z=1+math.exp(-c)
    assert math.isclose(kl([1,0],[1/z,math.exp(-c)/z]),math.log(z),abs_tol=1e-12)
oracle = min((r for r in rows if r['eligible']),key=lambda r:F(r['oracle_realized_total']))['wiring']
for horn in ['A','B']:
    pick=min((r for r in rows if r['eligible']), key=lambda r:r[f'predicted_{horn}_total'])['wiring']
    for r in rows:
        r[f'{horn}_selected'] = r['wiring']==pick
        r['oracle_selected'] = r['wiring']==oracle
        r[f'{horn}_pick_agrees_with_constructed_oracle'] = pick==oracle
out=dict(schema='buffer-computation-v1',data_status='constructed-not-measured',
         preregistration_commit='a508aa24',full_A_status='not-defined-by-available-ruling',
         comparison_status='A-projection-and-B-not-distinguishable-on-this-field',rows=rows)
(ROOT/'results.json').write_text(json.dumps(out,indent=2,allow_nan=False)+'\n')
print('PASS: direct joint/marginal KL and entropy, normalization, exact-cost and zero-revisit controls')
for r in rows:
    print(json.dumps({k:v for k,v in r.items() if k not in ['coordinates','killed']}))
