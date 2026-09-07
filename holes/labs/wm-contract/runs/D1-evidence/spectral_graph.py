#!/usr/bin/env python3
"""Measure Zhou normalized and unnormalized incidence Laplacians."""
import hashlib, json, random
from collections import Counter, deque
from pathlib import Path
import numpy as np

HERE=Path(__file__).parent; IN=HERE/'graph.edn'; OUT=HERE/'spectral.edn'
SEED=54112026; REWIRINGS=200

def matrices(n, edges):
    H=np.zeros((n,len(edges)))
    for j,e in enumerate(edges):
        for i in e: H[i,j]=1
    dv=H.sum(1); de=H.sum(0)
    active=np.flatnonzero(dv>0)
    H=H[active]
    dv=dv[active]
    A=(H/de)@H.T
    N=np.eye(len(active))-(A/np.sqrt(dv)[:,None])/np.sqrt(dv)[None,:]
    U=np.diag(dv)-A
    return N,U,active

def lambda2(M, seed):
    """Second-smallest Ritz value from deterministic 60-step Lanczos."""
    n=len(M)
    if n<2: return 0.0
    rg=np.random.default_rng(seed); q=rg.normal(size=n); q/=np.linalg.norm(q)
    q0=np.zeros(n); beta=0.0; alphas=[]; betas=[]; basis=[]
    for _ in range(min(60,n)):
        basis.append(q.copy()); z=M@q-beta*q0; alpha=float(q@z); z-=alpha*q
        for v in basis: z-=float(v@z)*v
        beta=float(np.linalg.norm(z)); alphas.append(alpha)
        if beta<1e-12: break
        betas.append(beta); q0,q=q,z/beta
    T=np.diag(alphas)
    for i,b in enumerate(betas[:len(alphas)-1]): T[i,i+1]=T[i+1,i]=b
    vals=np.linalg.eigvalsh(T)
    return float(max(0.0,vals[1])) if len(vals)>1 else 0.0

def components(n,edges):
    adj=[set() for _ in range(n)]
    for e in edges:
        for x in e: adj[x].update(e-{x})
    seen=set(); sizes=[]
    for i in range(n):
        if i in seen: continue
        q=[i]; seen.add(i); size=0
        while q:
            x=q.pop(); size+=1
            for y in adj[x]:
                if y not in seen: seen.add(y); q.append(y)
        sizes.append(size)
    return sorted(sizes,reverse=True)

def largest_component(n,edges):
    adj=[set() for _ in range(n)]
    for e in edges:
        for x in e: adj[x].update(e-{x})
    seen=set(); best=set()
    for i in range(n):
        if i in seen or not adj[i]: continue
        comp={i}; stack=[i]; seen.add(i)
        while stack:
            x=stack.pop()
            for y in adj[x]:
                if y not in seen: seen.add(y); comp.add(y); stack.append(y)
        if len(comp)>len(best): best=comp
    return best

def component_matrices(n,edges):
    keep=largest_component(n,edges); order=sorted(keep); ren={v:i for i,v in enumerate(order)}
    induced=[{ren[v] for v in e if v in keep} for e in edges]
    induced=[e for e in induced if e]
    return matrices(len(order),induced)[:2]

def rewire(edges,rng,attempts):
    es=[set(e) for e in edges]
    inc=[(v,j) for j,e in enumerate(es) for v in e]
    for _ in range(attempts):
        a,b=rng.sample(range(len(inc)),2); v,e=inc[a]; w,f=inc[b]
        if e==f or v==w or w in es[e] or v in es[f]: continue
        es[e].remove(v); es[f].remove(w); es[e].add(w); es[f].add(v)
        inc[a]=(w,e); inc[b]=(v,f)
    return es

def stats(xs):
    return {'mean':float(np.mean(xs)),'sd':float(np.std(xs,ddof=1))}

def main():
    g=json.loads(IN.read_text()); ids=[x['id'] for x in g['nodes']]; ix={x:i for i,x in enumerate(ids)}
    edges=[{ix[x] for x in e['members']} for e in g['hyperedges']]
    N,U=component_matrices(len(ids),edges); realn=lambda2(N,SEED); realu=lambda2(U,SEED+1)
    rng=random.Random(SEED); ns=[]; us=[]; attempts=max(1000,20*sum(map(len,edges)))
    for sample in range(REWIRINGS):
        er=rewire(edges,rng,attempts); n,u=component_matrices(len(ids),er)
        ns.append(lambda2(n,SEED+2+2*sample)); us.append(lambda2(u,SEED+3+2*sample))
    sn,su=stats(ns),stats(us)
    types=Counter(x['type'] for x in g['nodes']); inc_types=Counter()
    edge_types=Counter(g['nodes'][ix[e['source']]]['type'] for e in g['hyperedges'])
    for e in g['hyperedges']:
        for x in e['members']: inc_types[g['nodes'][ix[x]]['type']]+=1
    cs=components(len(ids),edges)
    out={'schema':'d1-spectral-v1','seed':SEED,'rewirings':REWIRINGS,'swap-attempts-per-rewiring':attempts,
      'operator':'Zhou incidence operator with unit hyperedge weights: normalized L=I-Dv^-1/2 H De^-1 H^T Dv^-1/2; unnormalized L=Dv-H De^-1 H^T. Lambda_2 is the second-smallest deterministic 60-step Lanczos Ritz value on the largest nontrivial component of each graph; all components and document isolates are reported separately.',
      'null':'Binary incidence configuration model by degree-preserving double-edge swaps, rejecting duplicate incidences; node degrees and hyperedge sizes are fixed.',
      'counts':{'nodes':len(ids),'hyperedges':len(edges),'incidences':sum(map(len,edges)),'reference-isolates':len(g['reference-isolates']),'nodes-by-type':dict(sorted(types.items())),'hyperedges-by-source-type':dict(sorted(edge_types.items())),'incidences-by-member-type':dict(sorted(inc_types.items()))},
      'components':{'count':len(cs),'largest-size':cs[0] if cs else 0,'sizes':cs},
      'normalized':{'lambda-2':realn,'null':sn,'z':(realn-sn['mean'])/sn['sd'] if sn['sd'] else None},
      'unnormalized':{'lambda-2':realu,'null':su,'z':(realu-su['mean'])/su['sd'] if su['sd'] else None}}
    OUT.write_text(json.dumps(out,sort_keys=True,indent=2)+'\n'); print(hashlib.sha256(OUT.read_bytes()).hexdigest(),OUT)
if __name__=='__main__': main()
