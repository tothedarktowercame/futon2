#!/usr/bin/env python3
"""fold-GFN reward harness (laptop). Given a mission + a SELECTED pattern-subset (a candidate wiring
method-spine), construct a minimal CLean wiring, gate it 0-sorry, and score structural meaningfulness
vs the empirical A-small wiring. reward = 0sorry_floor * (0.4 + 0.6*struct_sim).
This is the STEEP reward (M1) the GFN needs. Deterministic constructor = a mini impl-#1."""
import json,subprocess,tempfile,os,re,sys
sys.path.insert(0,"/home/joe/code/futon6/scripts")
import numpy as np
from clean_structure_embed import load_clean, structure_vector

CLEANDIR="/home/joe/code/futon6/holes/clean"
def slug(p): return re.sub(r'[^a-z0-9]+','-',p.lower()).strip('-')

def construct(mission, patterns):
    """patterns (ordered) -> a minimal linear CLean wiring dict (each pattern = a box; linear chain)."""
    boxes=[]; wires=[]; seq=[]
    prev=None
    for i,p in enumerate(patterns):
        bid=f"s{i+1}"; meth=slug(p.split('/')[-1]); seq.append(meth)
        prod=f"prod-{i+1}"
        b={"id":":"+bid,"method":":"+meth,"text":f"apply {p}"}
        if prev: b["consumes"]=[":"+prev]
        if i==len(patterns)-1: b["discharges"]={"to":":"+slug(mission)+"-goal"}
        else: b["produces"]=":"+prod
        # give the middle box a typed hole (so it's not vacuous)
        if i==1: b["hole"]={"kind":":sorry","discharge":":sorryProof","satiety":":payoff","wanted":"the load-bearing step"}
        boxes.append(b)
        if prev: wires.append({"from":":"+f"s{i}","to":":"+bid,"carries":":"+prev})
        prev=prod
    return {":clean/proof":slug(mission),":clean/title":mission,
            ":clean/seq":[":"+s for s in seq],
            ":clean/boxes":boxes,":clean/wires":wires,
            ":clean/copar":[{":reading":":informal",":is":":clean/seq"},{":reading":":formal",":is":[":clean/boxes",":clean/wires"]}],
            ":clean/shape":{":macro":":gfn-candidate",":holes-at":[":s2"],":discharges-at":[f":s{len(patterns)}"]}}

def to_edn(d):
    def e(v):
        if isinstance(v,dict): return "{"+" ".join(f"{e(k)} {e(val)}" for k,val in v.items())+"}"
        if isinstance(v,list): return "["+" ".join(e(x) for x in v)+"]"
        if isinstance(v,str): return v if v.startswith(":") else '"'+v.replace('"','\\"')+'"'
        return str(v)
    return e(d)

def gate_and_score(mission, patterns, empirical_leaf=None):
    d=construct(mission,patterns); pid=slug(mission)
    f=os.path.join(CLEANDIR,pid+".clean.edn"); open(f,"w").write(to_edn(d))
    try:
        r=subprocess.run(["/home/joe/code/futon6/.venv/bin/python",
            "/home/joe/code/futon6/scripts/clean_to_lean.py","--clean-dir",CLEANDIR,
            "--mode","standalone","--only",pid,"--out","/tmp/gfn_c.lean"],
            capture_output=True,text=True,timeout=60)
        lean=subprocess.run(["lean","/tmp/gfn_c.lean"],capture_output=True,text=True,timeout=90)
        zero_sorry = 1.0 if (lean.returncode==0 and lean.stdout.strip()=="" ) else 0.0
    finally:
        if os.path.exists(f): os.remove(f)
    sim=0.0
    if empirical_leaf and zero_sorry>0:
        try:
            va,_,_=structure_vector(load_clean(os.path.join(CLEANDIR,empirical_leaf+".clean.edn")))
            vb,_,_=structure_vector(d)
            va,vb=np.array(va),np.array(vb); sim=float(va@vb/((np.linalg.norm(va)*np.linalg.norm(vb))+1e-9))
        except Exception as ex: sim=0.0
    reward = zero_sorry*(0.4+0.6*max(sim,0))
    return {"0sorry":zero_sorry,"struct_sim":round(sim,3),"reward":round(reward,3),"nboxes":len(patterns)}

if __name__=="__main__":
    # smoke: autoclock-in's real cascade patterns -> reward vs its empirical A-small wiring
    pats=["invariant-coherence/shape-first-identify","storage/durability-throughput-gate",
          "coordination/session-durability-check","iching/hexagram-18-gu",
          "invariant-coherence/state-snapshot-witness","sidecar/per-id-audit-timeline-linkage"]
    print("full cascade   ->",gate_and_score("autoclock-in-gfn",pats,empirical_leaf="autoclock-in"))
    print("subset (3)     ->",gate_and_score("autoclock-in-gfn",pats[:3],empirical_leaf="autoclock-in"))
    print("scrambled (2)  ->",gate_and_score("autoclock-in-gfn",pats[3:5],empirical_leaf="autoclock-in"))
