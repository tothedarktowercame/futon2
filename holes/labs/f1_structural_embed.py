#!/usr/bin/env python3
# f1-v0: structural embedding (SVD matrix-factorization) link-prediction over substrate-2.
# Tests whether the (now-rich) substrate-2 structure predicts a mission's neighbours. numpy-only, laptop.
import json, urllib.request, time, numpy as np

t0=time.time()
g=json.load(urllib.request.urlopen("http://localhost:7070/api/alpha/cascade-real/graph"))
# --- build mission x item incidence (items = patterns U clusters U hole-nodes U arrow-target-missions) ---
edges=[]  # (mission, item)
for e in g["patterns"]["edges"]:            edges.append((e["mission"], "pat:"+e["pattern"]))
for e in g["clusters"]:                     edges.append((e["mission"], "clu:"+e["cluster"]))
for e in g["holes"]:                        edges.append((e["target"], "hol:"+e["hole"]))
for e in g["arrows"]:                       edges.append((e["have"],   "arw:"+e["want"]))
miss=sorted({m for m,_ in edges}); item=sorted({i for _,i in edges})
mi={m:k for k,m in enumerate(miss)}; ii={i:k for k,i in enumerate(item)}
A=np.zeros((len(miss),len(item)),dtype=np.float32)
for m,i in edges: A[mi[m],ii[i]]=1.0
print(f"graph: {len(miss)} missions x {len(item)} items, {int(A.sum())} edges  (build {time.time()-t0:.2f}s)")

def embed(M, k=32):
    # SVD structural embedding of a binary incidence (tf-idf-ish column weighting helps)
    w = np.log(1+M.shape[0]/(1+M.sum(0)))           # idf on items
    U,S,Vt = np.linalg.svd(M*w, full_matrices=False)
    k=min(k,len(S)); return U[:,:k]*S[:k], (Vt[:k].T)  # mission-emb, item-emb

# --- (a) bulk held-out mission<->pattern link prediction: AUC vs popularity ---
rng=np.random.default_rng(7)
pat_edges=[(m,i) for m,i in edges if i.startswith("pat:")]
held=set(rng.choice(len(pat_edges), size=max(20,len(pat_edges)//10), replace=False).tolist())
Atr=A.copy()
for k,(m,i) in enumerate(pat_edges):
    if k in held: Atr[mi[m],ii[i]]=0.0
te=time.time(); Um,Vi=embed(Atr); emb_ms=(time.time()-te)*1000
pop=Atr.sum(0)                                      # popularity baseline (item degree)
def auc(scores_pos, scores_neg):
    p=np.array(scores_pos); n=np.array(scores_neg)
    return (p[:,None]>n[None,:]).mean()
pos_e=[pat_edges[k] for k in held]
neg_e=[(miss[rng.integers(len(miss))], item[rng.integers(len(item))]) for _ in range(len(pos_e)*5)]
sc=lambda m,i: float(Um[mi[m]]@Vi[ii[i]])
svd_auc=auc([sc(m,i) for m,i in pos_e],[sc(m,i) for m,i in neg_e])
pop_auc=auc([pop[ii[i]] for m,i in pos_e],[pop[ii[i]] for m,i in neg_e])
print(f"(a) held-out mission<->pattern:  SVD-AUC={svd_auc:.3f}   popularity-AUC={pop_auc:.3f}   (embed {emb_ms:.0f}ms)")

# --- (b) autoclock-in ENDPOINT RECOVERY: from its cascade, rank its non-pattern endpoints ---
M="futon3c-d/mission/autoclock-in"
Um2,Vi2=embed(A)
v=Um2[mi[M]]                                        # its structural mission vector
scores=Vi2@v                                        # score every item
order=np.argsort(-scores)
rank={item[j]:r for r,j in enumerate(order)}
true_ep=[i for m,i in edges if m==M and not i.startswith("pat:")]
print(f"(b) autoclock-in true non-pattern endpoints ({len(true_ep)}), their rank / {len(item)} items:")
for ep in true_ep: print(f"     rank {rank[ep]:>4}  {ep}")
print(f"     (top-20 hit rate of true endpoints: {np.mean([rank[ep]<20 for ep in true_ep]):.2f})")
print(f"total wall: {time.time()-t0:.2f}s")
