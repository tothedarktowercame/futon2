#!/usr/bin/env python3
"""D1 evidence, half 1: a worked KL[Q(o|pi) || C] example over terminal flight
dispositions, recomputed from data/wm-full-loop (not restated from the NOTE).
Deterministic: sorted traversal, no RNG. Writes kl-worked-example.edn."""
import os, re, math, json

ROOT = "/home/joe/code/futon2/data/wm-full-loop"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "kl-worked-example.edn")


def edn_get(text, pattern):
    m = re.search(pattern, text)
    return m.group(1) if m else None


pairs, no_pi, no_outcome = [], [], []
attempt_dirs = []
for dirpath, dirnames, filenames in os.walk(ROOT):
    dirnames.sort()
    if re.search(r"attempt-\d+$", dirpath):
        attempt_dirs.append(dirpath)
attempt_dirs.sort()

for d in attempt_dirs:
    sel, clo = os.path.join(d, "002-selection.edn"), os.path.join(d, "007-closed.edn")
    pi = None
    if os.path.exists(sel):
        t = open(sel).read()
        m = re.search(r":selected-action\s*\{[^{}]*?:type\s+(:[\w-]+)", t)
        pi = m.group(1) if m else None
    if not os.path.exists(clo):
        no_outcome.append(d)
        continue
    t = open(clo).read()
    o = edn_get(t, r":judgment\s*\{\s*:outcome\s+(:[\w-]+)")
    if o is None:
        no_outcome.append(d)
        continue
    if pi is None:
        no_pi.append((d, o))
        continue
    pairs.append((pi, o))

policies = sorted({p for p, _ in pairs})
outcomes = sorted({o for _, o in pairs})

Q = {}
for p in policies:
    obs = [o for pp, o in pairs if pp == p]
    Q[p] = {o: obs.count(o) / len(obs) for o in outcomes}

# Candidate C's, both full-support over the observed alphabet so every KL is finite.
n = len(outcomes)
C_uniform = {o: 1.0 / n for o in outcomes}
# Operator-seeded C (illustrative seed for the preference registry, NOT a ruling):
# mass on grounded change, modest mass on honest negatives, floor elsewhere.
seed = {":grounded-change": 0.50, ":grounded-no-change": 0.15, ":artifact-only": 0.10}
floor_total = 1.0 - sum(v for k, v in seed.items() if k in outcomes)
floor_n = len([o for o in outcomes if o not in seed])
C_seeded = {o: seed.get(o, floor_total / floor_n) for o in outcomes}


def kl(q, c):
    s = 0.0
    for o in outcomes:
        if q[o] > 0:
            s += q[o] * math.log(q[o] / c[o])
    return s


def edn_map(d, fmt=lambda v: f"{v:.6f}"):
    return "{" + " ".join(f"{k} {fmt(v)}" for k, v in sorted(d.items())) + "}"


risks = {name: {p: kl(Q[p], C) for p in policies}
         for name, C in [(":C-uniform", C_uniform), (":C-seeded", C_seeded)]}
rankings = {name: sorted(policies, key=lambda p: r[p]) for name, r in risks.items()}

with open(OUT, "w") as f:
    f.write("{:schema :kl-worked-example-v1\n")
    f.write(" :recomputed-from \"data/wm-full-loop (86 attempt dirs walked, sorted)\"\n")
    f.write(f" :pairs {len(pairs)} :no-recoverable-pi {len(no_pi)} :no-outcome {len(no_outcome)}\n")
    f.write(f" :policies [{' '.join(policies)}]\n")
    f.write(f" :outcomes [{' '.join(outcomes)}]\n")
    f.write(" :q-by-policy {" + "\n   ".join(f"{p} {edn_map(Q[p])}" for p in policies) + "}\n")
    f.write(" :c-candidates {:C-uniform " + edn_map(C_uniform) +
            "\n                :C-seeded " + edn_map(C_seeded) + "}\n")
    f.write(" :risk {" + "\n        ".join(
        f"{name} {edn_map(r)}" for name, r in sorted(risks.items())) + "}\n")
    f.write(" :ranking {" + " ".join(
        f"{name} [{' '.join(rk)}]" for name, rk in sorted(rankings.items())) + "}\n")
    f.write(" :not-claimed \"The seeded C is an illustration of a registry seed, not a ruling; no registry is touched. Attempts without recoverable pi are counted, not dropped silently.\"}\n")

print(f"pairs={len(pairs)} no-pi={len(no_pi)} no-outcome={len(no_outcome)}")
for name in sorted(risks):
    print(name, {p: round(risks[name][p], 4) for p in rankings[name]}, "->", rankings[name])
