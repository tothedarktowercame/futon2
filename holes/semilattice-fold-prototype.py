#!/usr/bin/env python3
"""Prototype: fold a cascade's SEMILATTICE (chosen_semi_lattice) into a wiring.
Patterns -> boxes; descent -> BV.seq wires; co_app -> BV.copar meets.
Contrast: the classical fold ate the flat :shown list and produced 0 boxes."""
import sys, re, json, collections
sys.path.insert(0, "/home/joe/code/futon3a/holes/labs/M-memes-arrows")
from cascade_construct import construct_cascade, chosen_semi_lattice, load_phylogeny, pattern_stem

PSI = ("there is no condensed navigable mental-model of the stack's mission-work as a whole, "
       "and no way to see which capabilities the operator is developing across missions; "
       "mining pipeline, capability vocabulary, B/A splitting, tagging, hypergraph condensation, cross-mission connection")
WANT = "MissionState -> {Wiring, PolicyHoles}"

phy = load_phylogeny()
r = construct_cascade(PSI, epsilon=0.18, phylogeny=phy)
ids = [p for (p,_,_) in r["cascade"]]
sl  = chosen_semi_lattice(ids, phy)
desc, coapp = sl["descent"], sl["co_app"]

def leaf(pid): return re.sub(r"[^a-z0-9]+","-", pattern_stem(pid).split("/")[-1].lower()).strip("-")

# --- fold the semilattice into a wiring (the fold's own :wiring shape) ---
boxes = [{"id": leaf(p), "pattern": pattern_stem(p), "produces": leaf(p)+"-out"} for p in ids]
seq_wires  = [{"from": leaf(a), "to": leaf(b), "type": "wire/seq",   "carries": leaf(a)+"-out"} for (a,b) in desc]
copar_wires= [{"from": leaf(a), "to": leaf(b), "type": "wire/copar", "weight": w} for (a,b,w) in coapp]
touched = {leaf(a) for a,_ in desc} | {leaf(b) for _,b in desc} | {leaf(a) for a,_,_ in coapp} | {leaf(b) for _,b,_ in coapp}
isolated = [b["id"] for b in boxes if b["id"] not in touched]
wiring = {"want-signature": WANT, "boxes": boxes,
          "wires": seq_wires + copar_wires,
          "terminals": {"out": [{"port": "wiring-diagram", "type": "Wiring"}]},
          "policy-holes": [{"isolated-pattern": p} for p in isolated]}

print(f"=== SEMILATTICE-FOLD wiring (F={r['F-free-energy']}, wholeness={r['wholeness']}) ===")
print(f"  boxes           : {len(boxes)}   (one per pattern — a REAL construction)")
print(f"  seq wires  (BV.seq)   : {len(seq_wires)}")
print(f"  copar wires (BV.copar): {len(copar_wires)}")
print(f"  isolated -> policy-holes: {len(isolated)}")
print(f"\n  vs CLASSICAL fold on the flat :shown list  -> boxes=0, policy-holes={len(ids)} (all unfolded)\n")

# --- emit the descent BACKBONE as a DarkTower CLean (validate 0-sorry) ---
# topological order over descent (co_app carried in :clean/shape metadata — the
# renderer-extension the co_app copar needs, exactly the scope-organism deferral).
succ = collections.defaultdict(list); indeg = {leaf(p):0 for p in ids}
for a,b in desc:
    succ[leaf(a)].append(leaf(b)); indeg[leaf(b)] += 1
order, q = [], [n for n in [leaf(p) for p in ids] if indeg[n]==0]
seen=set()
while q:
    n=q.pop(0)
    if n in seen: continue
    seen.add(n); order.append(n)
    for m in succ[n]:
        indeg[m]-=1
        if indeg[m]==0: q.append(m)
for p in ids:  # any left out by a cycle -> append (keeps it total)
    if leaf(p) not in seen: order.append(leaf(p))
pos = {n:i for i,n in enumerate(order)}
pred = collections.defaultdict(list)
for a,b in desc: pred[leaf(b)].append(leaf(a)+"-out")
cboxes=[]
for n in order:
    bx={"id":n,"method":n,"text":f"pattern {n}","produces":n+"-out"}
    if pred[n]: bx["consumes"]=sorted(set(pred[n]))
    cboxes.append(bx)
clean={"clean/proof":"M-learning-loop-cascade",
       "clean/title":"M-learning-loop F-optimal cascade folded as a DarkTower wiring (semilattice backbone)",
       "clean/seq":[b["method"] for b in cboxes],
       "clean/boxes":cboxes,
       "clean/wires":[{"from":a,"to":b,"carries":a+"-out"} for (a,b) in [(leaf(x),leaf(y)) for x,y in desc]],
       "clean/copar":[{"reading":"informal","is":"clean/seq"},{"reading":"formal","is":["clean/boxes","clean/wires"]}],
       "clean/shape":{"macro":"cascade-semilattice-fold","holes-at":set(),"discharges-at":set(),
                      "co-app":[[leaf(a),leaf(b),w] for a,b,w in coapp],
                      "seq-edges":len(desc),"copar-edges":len(coapp)}}
def edn(x):
    if isinstance(x,dict):
        if not x: return "{}"
        return "{"+" ".join(f"{edn(k) if not isinstance(k,str) or '/' in k else ':'+k} {edn(v)}" for k,v in x.items())+"}"
    if isinstance(x,set): return "#{"+" ".join(sorted(edn(v) for v in x))+"}" if x else "#{}"
    if isinstance(x,(list,tuple)): return "["+" ".join(edn(v) for v in x)+"]"
    if isinstance(x,str): return ':'+x if ('/' in x) else '"'+x+'"'
    return str(x)
def edn_top(d): return "{"+" ".join(f":{k} {edn(v)}" for k,v in d.items())+"}"
open("/tmp/cascade-clean/M-learning-loop-cascade.clean.edn","w").close() if False else None
import os; os.makedirs("/tmp/cascade-clean",exist_ok=True)
open("/tmp/cascade-clean/M-learning-loop-cascade.clean.edn","w").write(edn_top(clean)+"\n")
print("  wrote /tmp/cascade-clean/M-learning-loop-cascade.clean.edn  (descent backbone; co_app in :clean/shape)")
