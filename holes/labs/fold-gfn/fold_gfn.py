#!/usr/bin/env python3
"""Minimal trajectory-balance GFN over the fold's spine-selection, with the STEEP empirical-match reward.
Task: given a mission's cascade (candidate patterns), SELECT the subset that = the empirical wiring spine.
Reward = exp(beta*F1(selected, target)). Tests whether a steep reward lets the GFN learn (vs the flat-reward
M1 wall). torch/CPU, futon6/.venv. Leave-one-mission-out eval."""
import json,sys,math,random
sys.path.insert(0,"/home/joe/code/futon6/scripts")
import urllib.request, numpy as np, torch, torch.nn as nn, torch.nn.functional as F
from clean_structure_embed import load_clean, kw
random.seed(0); torch.manual_seed(0)
g=json.loads(urllib.request.urlopen("http://localhost:7070/api/alpha/cascade-real/graph").read())
from collections import defaultdict
casc=defaultdict(list)
for e in g["patterns"]["edges"]: casc[e["mission"].split("/")[-1]].append(e["pattern"].split("/")[-1])
LEAVES="autoclock-in stepper-calibration patterns-done-right state-snapshot-witness agency-rebuild invariant-queue-unstuck single-entry-point f6-ingest pattern-ingest a-sorry-enterprise".split()
data=[]
for lf in LEAVES:
    d=load_clean(f"/home/joe/code/futon6/holes/clean/{lf}.clean.edn")
    seq=set(kw(x) for x in d.get(kw(":clean/seq"),d.get("clean/seq",[])))
    cand=list(dict.fromkeys(casc.get(lf,[])))
    tgt=set(c for c in cand if c in seq)
    if len(cand)>=4 and tgt: data.append((lf,cand,tgt))
vocab=sorted({p for _,c,_ in data for p in c}); vi={p:i for i,p in enumerate(vocab)}; V=len(vocab)
print(f"missions {len(data)} · pattern vocab {V}")
D=32
emb=nn.Embedding(V+1,D)  # +1 = STOP
pf=nn.Sequential(nn.Linear(D*2,64),nn.ReLU(),nn.Linear(64,1))  # score(candidate | state)
logZ=nn.Parameter(torch.zeros(1))
opt=torch.optim.Adam(list(emb.parameters())+list(pf.parameters())+[logZ],lr=3e-3)
def state_vec(sel): 
    if not sel: return torch.zeros(D)
    return emb(torch.tensor([vi[p] for p in sel])).mean(0)
def f1(sel,tgt):
    s=set(sel); 
    if not s: return 0.0
    p=len(s&tgt)/len(s); r=len(s&tgt)/len(tgt); return 0 if p+r==0 else 2*p*r/(p+r)
def sample(cand, greedy=False):
    sel=[]; logpf=torch.zeros(1); remaining=list(cand)
    for _ in range(len(cand)+1):
        sv=state_vec(sel); opts=remaining+["STOP"]
        feats=torch.stack([torch.cat([emb(torch.tensor(vi.get(o,V))),sv]) for o in opts])
        logits=pf(feats).squeeze(1); lp=F.log_softmax(logits,0)
        a=int(torch.argmax(lp)) if greedy else int(torch.distributions.Categorical(logits=logits).sample())
        logpf=logpf+lp[a]
        if opts[a]=="STOP": break
        sel.append(opts[a]); remaining.remove(opts[a])
    return sel,logpf
def train(train_data, beta=6.0, steps=800):
    hist=[]
    for t in range(steps):
        lf,cand,tgt=random.choice(train_data)
        sel,logpf=sample(cand); R=math.exp(beta*f1(sel,tgt))
        loss=(logZ+logpf-math.log(R))**2
        opt.zero_grad(); loss.backward(); opt.step()
        if t%100==0:
            m=np.mean([f1(sample(c,greedy=False)[0],tg) for _,c,tg in train_data for _ in range(3)])
            hist.append((t,round(float(m),3)))
    return hist
# leave-one-out eval
print("training on all (reward-climb check):")
for t,m in train(data): print(f"  step {t:4}  sampled-F1 {m}")
print("\nleave-one-mission-out greedy spine recovery:")
recs=[]
for held in range(len(data)):
    # reinit
    torch.manual_seed(held); 
    for p in emb.parameters(): nn.init.normal_(p,0,0.1)
    for p in pf.parameters():
        if p.dim()>1: nn.init.xavier_uniform_(p)
        else: nn.init.zeros_(p)
    with torch.no_grad(): logZ.zero_()
    tr=[d for i,d in enumerate(data) if i!=held]; train(tr,steps=500)
    lf,cand,tgt=data[held]; sel,_=sample(cand,greedy=True); r=f1(sel,tgt); recs.append(r)
    print(f"  {lf:24} F1={r:.2f}  picked {sorted(sel)[:4]}... vs target {sorted(tgt)[:3]}...")
print(f"\nMEAN leave-one-out spine-recovery F1: {np.mean(recs):.2f}  (random-subset baseline ~{np.mean([f1(random.sample(c,len(t)),t) for _,c,t in data]):.2f})")
