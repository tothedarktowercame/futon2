#!/usr/bin/env python3
# f1-v1: recovery CURVE (step 1) + sorry-node enrichment (step 2). numpy-only, laptop.
import json, urllib.request, time, numpy as np
t0=time.time(); U=urllib.request.urlopen
g=json.load(U("http://localhost:7070/api/alpha/cascade-real/graph"))
edges=[]
for e in g["patterns"]["edges"]: edges.append((e["mission"],"pat:"+e["pattern"]))
for e in g["clusters"]:          edges.append((e["mission"],"clu:"+e["cluster"]))
for e in g["holes"]:             edges.append((e["target"], "hol:"+e["hole"]))
for e in g["arrows"]:            edges.append((e["have"],  "arw:"+e["want"]))
for e in g["held"]:              edges.append((e["mission"],"hld:"+e["held"]))
# --- step 2 enrichment: add sorry nodes linked to missions via :sorry/related-missions ---
try:
    import re
    body=U("http://localhost:7071/api/alpha/hyperedges?type=code%2Fv05%2Fsorry").read().decode()
    for sid,rel in re.findall(r':hx/id "([^"]+)".*?:sorry/related-missions \[([^\]]*)\]', body):
        for mm in re.findall(r'"([^"]+)"', rel):
            # map bare M-* to canonical if present, else keep
            edges.append((mm, "sry:"+sid))
    nsry=len({i for _,i in edges if i.startswith("sry:")})
except Exception as ex: nsry=0; print("sorry-enrich skipped:",ex)
miss=sorted({m for m,_ in edges}); item=sorted({i for _,i in edges})
mi={m:k for k,m in enumerate(miss)}; ii={i:k for k,i in enumerate(item)}
A=np.zeros((len(miss),len(item)),np.float32)
for m,i in edges: A[mi[m],ii[i]]=1.0
NONPAT=[k for k,i in enumerate(item) if not i.startswith("pat:")]
print(f"graph {len(miss)}m x {len(item)}i, {int(A.sum())} edges  (+{nsry} sorry nodes)  build {time.time()-t0:.2f}s")

def embed(M,k=32):
    w=np.log(1+M.shape[0]/(1+M.sum(0))); U_,S,Vt=np.linalg.svd(M*w,full_matrices=False)
    k=min(k,len(S)); return U_[:,:k]*S[:k], Vt[:k].T

rng=np.random.default_rng(11)
# hold out 30% of NON-pattern edges (the endpoints to recover)
np_edges=[(m,i) for m,i in edges if not i.startswith("pat:")]
hidx=set(rng.choice(len(np_edges),size=int(len(np_edges)*0.30),replace=False).tolist())
Atr=A.copy()
for k,(m,i) in enumerate(np_edges):
    if k in hidx: Atr[mi[m],ii[i]]=0.0
te=time.time(); Um,Vi=embed(Atr); ems=(time.time()-te)*1000
nonpat_items=np.array(NONPAT)
pop=Atr.sum(0)
# recovery of each held-out endpoint among all non-pattern items
ranks=[]; pop_ranks=[]
Vnp=Vi[nonpat_items]; popnp=pop[nonpat_items]
for k in hidx:
    m,i=np_edges[k]; s=Vnp@Um[mi[m]]
    order=np.argsort(-s); pos=np.where(nonpat_items[order]==ii[i])[0][0]; ranks.append(pos)
    porder=np.argsort(-popnp); ppos=np.where(nonpat_items[porder]==ii[i])[0][0]; pop_ranks.append(ppos)
ranks=np.array(ranks); pop_ranks=np.array(pop_ranks); N=len(nonpat_items)
print(f"embed {ems:.0f}ms · held-out endpoints n={len(ranks)} over {N} non-pattern items")
for tag,r in [("SVD",ranks),("popularity",pop_ranks)]:
    print(f"  {tag:11s}  recall@20={np.mean(r<20):.2f}  recall@50={np.mean(r<50):.2f}  MRR={np.mean(1/(r+1)):.3f}  median-rank={int(np.median(r))}")
# per-mission curve: recall@20 for missions with >=2 held-out endpoints
from collections import defaultdict
bym=defaultdict(list)
for k,rk in zip(hidx,ranks): bym[np_edges[k][0]].append(rk)
permiss=[np.mean(np.array(v)<20) for v in bym.values() if len(v)>=2]
if permiss: print(f"per-mission recall@20 (n={len(permiss)} missions >=2 endpoints): median={np.median(permiss):.2f} mean={np.mean(permiss):.2f}")
print(f"total wall {time.time()-t0:.2f}s")
