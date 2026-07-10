#!/usr/bin/env python3
# f1 on the REAL code-graph: does a structural embedding predict held-out CALL edges? (pgvector-Lean replication)
import urllib.request,re,time,numpy as np
t0=time.time()
def pull(t,repo,lim=200000):
    u=f"http://localhost:7071/api/alpha/hyperedges?type={urllib.parse.quote(t)}&repo={repo}&limit={lim}"
    import urllib.parse
    b=urllib.request.urlopen(u).read().decode()
    return re.findall(r':hx/endpoints \[([^\]]*)\]',b)
import urllib.parse
def pull(t,repo,lim=300000):
    u=f"http://localhost:7071/api/alpha/hyperedges?type={urllib.parse.quote(t)}&repo={repo}&limit={lim}"
    b=urllib.request.urlopen(u).read().decode()
    return re.findall(r':hx/endpoints \[([^\]]*)\]',b)
REPO="futon3c-d"
calls=[]
for ep in pull("code/v05/calls",REPO):
    q=re.findall(r'"([^"]+)"',ep)
    if len(q)>=2 and not q[0].startswith("dir:") and not q[1].startswith("dir:"): calls.append((q[0],q[1]))
print(f"futon3c call edges pulled: {len(calls)}  ({time.time()-t0:.1f}s)")
V=sorted({v for e in calls for v in e}); vi={v:k for k,v in enumerate(V)}
n=len(V); print(f"vars (nodes): {n}")
if n<50: print("TOO SMALL — repo filter may not have matched; aborting"); raise SystemExit
A=np.zeros((n,n),np.float32)
for a,b in calls: A[vi[a],vi[b]]=1.0
Asym=np.maximum(A,A.T)
def embed(M,k=64):
    d=M.sum(1,keepdims=True); Dn=1/np.sqrt(np.maximum(d,1)); Mn=M*Dn*Dn.T   # sym-normalized adjacency
    U,S,Vt=np.linalg.svd(Mn,full_matrices=False); k=min(k,len(S)); return U[:,:k]*np.sqrt(S[:k])
rng=np.random.default_rng(3)
pos=list(zip(*np.where(Asym>0))); pos=[(a,b) for a,b in pos if a<b]
held=set(rng.choice(len(pos),size=int(len(pos)*0.2),replace=False).tolist())
Atr=Asym.copy()
for k in held: a,b=pos[k]; Atr[a,b]=Atr[b,a]=0.0
te=time.time(); E=embed(Atr); ems=(time.time()-te)*1000
deg=Atr.sum(1)
def auc(P,N):P=np.array(P);N=np.array(N);return float((P[:,None]>N[None,:]).mean())
held_e=[pos[k] for k in held]
neg=[(rng.integers(n),rng.integers(n)) for _ in range(len(held_e)*5)]; neg=[(a,b) for a,b in neg if a!=b and Asym[a,b]==0][:len(held_e)*3]
emb_sc=lambda a,b: float(E[a]@E[b]); deg_sc=lambda a,b: float(deg[a]*deg[b])
svd_auc=auc([emb_sc(a,b) for a,b in held_e],[emb_sc(a,b) for a,b in neg])
pop_auc=auc([deg_sc(a,b) for a,b in held_e],[deg_sc(a,b) for a,b in neg])
# recall@k: for each held (a,b), rank b among all non-neighbors of a by embedding score
ranks=[]
for a,b in held_e[:400]:
    s=E@E[a]; s[a]=-1e9
    for nb in np.where(Atr[a]>0)[0]: s[nb]=-1e9   # exclude train-known neighbors
    ranks.append(int(np.sum(s>s[b])))
ranks=np.array(ranks)
print(f"embed {ems:.0f}ms · held-out call edges n={len(held_e)}")
print(f"  structural-SVD  AUC={svd_auc:.3f}   recall@10={np.mean(ranks<10):.2f}  recall@50={np.mean(ranks<50):.2f}  MRR={np.mean(1/(ranks+1)):.3f}")
print(f"  degree/pop      AUC={pop_auc:.3f}")
print(f"total wall {time.time()-t0:.1f}s")
