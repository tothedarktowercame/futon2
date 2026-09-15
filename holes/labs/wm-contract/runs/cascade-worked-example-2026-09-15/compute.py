"""Synthetic finite cascade computations. No runtime or publication operations."""
import hashlib
import json
import math
from pathlib import Path

HERE = Path(__file__).resolve().parent
PATTERNS = ["SET", "FLIP", "SEAL", "PUBLISH", "ARCHIVE", "VERIFY"]
ORDERS = {"bad4": [0, 1, 2, 3], "good4": [1, 0, 2, 3],
          "good6": [1, 0, 2, 3, 4, 5]}
REQUIRES = {0: [], 1: [], 2: [0, 1], 3: [2], 4: [3], 5: [4]}
T = 6

def guard(p, state):
    _, mask = state
    return not mask & (1 << p) and all(mask & (1 << a) for a in REQUIRES[p])

def transition(order, state):
    x, mask = state
    p = next((p for p in order if guard(p, state)), None)
    if p is None:
        return state, None
    return ((1 if p == 0 else 1-x if p == 1 else x), mask | (1 << p)), p

def entropy(probabilities):
    return -sum(p * math.log(p) for p in probabilities if p > 0)

def kl(q, c):
    if any(p > 0 and c[i] == 0 for i, p in enumerate(q)):
        return None  # riskAdmissible refusal: never smooth the zero.
    return sum(p * math.log(p/c[i]) for i, p in enumerate(q) if p > 0)

def evaluate(order, horizon=T, preferred_one=.99):
    q = {(0, 0): 1.0}
    rows = []
    # O = [(0,0),(0,1),(1,0),(1,1)]; A is deterministic x plus fair coin.
    c = [(1-preferred_one)/2]*2 + [preferred_one/2]*2
    for tau in range(1, horizon+1):
        nxt, applications = {}, []
        for state, mass in q.items():
            dest, p = transition(order, state)
            nxt[dest] = nxt.get(dest, 0) + mass
            applications.append({"from": list(state), "to": list(dest),
                                 "pattern": None if p is None else PATTERNS[p], "mass": mass})
        q = nxt
        joint = [(state, 2*state[0]+coin, mass/2)
                 for state, mass in q.items() for coin in [0, 1]]
        qo = [sum(m for _, o, m in joint if o == i) for i in range(4)]
        risk = kl(qo, c)
        ambiguity = sum(mass*entropy([.5, .5]) for mass in q.values())
        information = sum(m*math.log(m/(q[s]*qo[o])) for s, o, m in joint if m > 0)
        pragmatic = None if risk is None else -sum(m*math.log(c[i]) for i, m in enumerate(qo) if m > 0)
        # Horn A illustration: push forward onto task outcome x, discarding coin.
        task_q = [qo[0]+qo[1], qo[2]+qo[3]]
        a_risk = kl(task_q, [1-preferred_one, preferred_one])
        a_ambiguity = 0.0  # derived: x is a deterministic observation of each state.
        book = None if risk is None else risk+ambiguity
        a = None if a_risk is None else a_risk+a_ambiguity
        assert abs(sum(q.values())-1) < 1e-12
        assert abs(sum(qo)-1) < 1e-12
        if book is not None:
            assert abs(book-(pragmatic-information)) < 1e-12
            assert abs(book-a-math.log(2)) < 1e-12
        rows.append({"tau": tau, "applications": applications,
                     "q": [{"state": list(s), "mass": m} for s, m in q.items()],
                     "J": [{"state": list(s), "observation": [o//2, o%2], "mass": m} for s,o,m in joint],
                     "Q": qo, "C": c, "risk": risk, "ambiguity": ambiguity,
                     "L": pragmatic, "I": information, "G_B": book,
                     "task_Q": task_q, "risk_A": a_risk, "ambiguity_A": a_ambiguity, "G_A": a})
    total = lambda key: None if any(r[key] is None for r in rows) else sum(r[key] for r in rows)
    return {"order": [PATTERNS[p] for p in order], "horizon": horizon,
            "preferred_one": preferred_one, "steps": rows,
            "total_A": total("G_A"), "total_B": total("G_B"),
            "expected_fired": sum(a["mass"] for r in rows for a in r["applications"] if a["pattern"] is not None)}

def probabilities(scores, fuel, coefficient):
    # Spec 3.5 extension; E_i=1, gamma=1, F_i=0 for this demonstration.
    logits = [-s-coefficient*f for s,f in zip(scores,fuel)]
    weights = [math.exp(v-max(logits)) for v in logits]
    z = sum(weights)
    result = [w/z for w in weights]
    assert abs(sum(result)-1) < 1e-12
    return {"E": [1,1,1], "gamma": 1, "F": [0,0,0], "fuel_coefficient": coefficient,
            "logits": logits, "probabilities": dict(zip(ORDERS,result)),
            "status": "illustrative choice distribution; FUEL extension not adopted"}

results = {k: evaluate(v) for k,v in ORDERS.items()}
own = {k: evaluate(v, len(v)) for k,v in ORDERS.items()}
exact = {k: evaluate(v, len(v), 1.0) for k,v in ORDERS.items()}
assert results["bad4"]["total_A"] > results["good4"]["total_A"]
assert results["bad4"]["total_B"] > results["good4"]["total_B"]
assert results["good4"]["total_A"] == results["good6"]["total_A"]
assert exact["good4"]["total_A"] == exact["good6"]["total_A"] == 0
assert exact["bad4"]["total_B"] is None
fuel = [v["expected_fired"] for v in results.values()]
posterior = {horn: {str(lam): probabilities([r['total_'+horn] for r in results.values()], fuel, lam)
                    for lam in [0, .25]} for horn in ['A','B']}
assert all(abs(a-b) < 1e-12 for a,b in zip(posterior['A']['0']['probabilities'].values(), posterior['B']['0']['probabilities'].values()))
sources = {
    'spec': ('holes/labs/wm-contract/SPEC-cascade-policy-semantics-2026-09-15.md', '24-31,35-55,65-79'),
    'efe': ('src/futon2/aif/efe.clj', '40-64,475-500,631-648,687-703'),
    'preferences': ('src/futon2/aif/preferences.clj', '235-264,506-565'),
}
root = HERE.parents[4]
pins = {name: {'path': path, 'lines': lines, 'sha256': hashlib.sha256((root/path).read_bytes()).hexdigest()}
        for name,(path,lines) in sources.items()}
artifact = {'scope': 'pedagogical synthetic computations; draft, no machine correspondence or policy ruling',
    'state': '(x in {0,1}, completed-pattern bitmask)', 'initial': [0,0],
    'patterns': PATTERNS, 'descent_prerequisites': {PATTERNS[k]: [PATTERNS[p] for p in v] for k,v in REQUIRES.items()},
    'outcome_order': [[0,0],[0,1],[1,0],[1,1]],
    'horn_A': 'illustrative task-outcome projection, NOT uniquely implied by full Alexander; removes known irrelevant coin',
    'horn_B': 'book G on full (task outcome, coin) alphabet, from same J',
    'equations': {'prediction': 'spec 2 q,J,Q', 'G_B': 'spec 2; book 4.9/4.10',
                  'decomposition': 'spec 3.3 G=L-I', 'composition': 'spec 3.6',
                  'choice': 'spec 3.5 ln E-gamma G; F=0 here', 'fuel': 'spec 2 pending FUEL proposal'},
    'common_horizon': results, 'own_horizon_diagnostic_not_for_selection': own,
    'exact_alignment_zero_preference_control': exact,
    'choice': posterior, 'old_proxy_components': json.loads((HERE/'proxy.json').read_text()),
    'source_pins': pins}
(HERE/'results.json').write_text(json.dumps(artifact,indent=2,allow_nan=False)+'\n')
print(json.dumps({'common_horizon': {k:{x:r[x] for x in ['total_A','total_B','expected_fired']} for k,r in results.items()}, 'choice': posterior},indent=2))
