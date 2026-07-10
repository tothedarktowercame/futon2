"""gfn_live.py — B2 of SPEC-full-loop-gfn: the GFN slush as the loop's live proposer.

For each (open) mission doc:
  pool     = top-M library patterns by cosine(psi, pattern-embedding);
  reward   = beta * R̂(S|m) from reward_v0 (the LEARNED reward — reliability
             posteriors + coverage feature + complexity prior), memoized per set;
  trainer  = gfn_core.GFN (the Slice-0-validated conditional-logZ TB core),
             sampled trajectories, batched updates, LOW beta by design;
  output   = K distinct proposals + the incumbent construct_cascade proposal,
             shuffled into a BLINDED proposals file (adjudicators must not see
             proposer identity); identities go to a separate sealed sidecar
             keyed by proposal hash.

Run (this box, no JVM):
  cd ~/code/futon3a && .venv/bin/python3 \
    ~/code/futon2/holes/labs/slush-demo/slice2/gfn_live.py M-chipwitz-corps ...
"""
from __future__ import annotations
import argparse, hashlib, json, random, sys
from pathlib import Path

LAB3A = "/home/joe/code/futon3a/holes/labs/M-memes-arrows"
SLICE2 = "/home/joe/code/futon2/holes/labs/slush-demo/slice2"
sys.path.insert(0, LAB3A)
sys.path.insert(0, SLICE2)

import numpy as np
from cascade_rollout import salient
from cascade_construct import EMB, construct_cascade
from offramp_cascade import identify_psi
from aliveness_v3_gate2 import MATCHER, DROP, produces_of
from aliveness_v3_corpus_gate2 import locate_doc
from fold_ground_truth import load_records
from reward_v0 import RewardV0
from gfn_core import GFN

OUT_DIR = Path("/home/joe/code/futon2/holes/labs/slush-demo/findings/proposals")
SEED = 20260710


def build_pool(psi, m=24):
    q = MATCHER.model.encode([psi], normalize_embeddings=True)[0]
    ids = list(EMB)
    mat = np.array([EMB[i] for i in ids])
    mat = mat / np.linalg.norm(mat, axis=1, keepdims=True)
    sims = mat @ q
    order = np.argsort(-sims)[:m]
    return [ids[i] for i in order], [float(sims[i]) for i in order]


class SetReward:
    """Memoized log-reward over pattern-index sets: beta * R̂(S|m)."""

    def __init__(self, pool, want, reward: RewardV0, beta: float):
        self.pool, self.want, self.rw, self.beta = pool, want, reward, beta
        self.produces = {}
        for i, pid in enumerate(pool):
            self.produces[i], _ = produces_of([pid])
        self.cache = {}

    def __call__(self, state) -> float:
        key = frozenset(state)
        if key not in self.cache:
            used = [self.pool[i] for i in sorted(key)]
            prod = set().union(*(self.produces[i] for i in key)) if key else set()
            self.cache[key] = self.beta * self.rw.score(used, self.want, produces=prod)
        return self.cache[key]


def train_and_sample(mission, pool, reward_fn, *, steps, batch, lr, eps,
                     max_len, k, seed):
    gfn = GFN(n=len(pool), max_len=max_len, conditional_logz=True)
    rng = np.random.default_rng(seed)
    for _ in range(steps):
        b = []
        for _ in range(batch):
            trj, terminal, log_r = gfn.sample(mission, reward_fn, rng, eps)
            b.append((mission, trj, terminal, log_r))
        gfn.train_step(b, lr)
    seen, out = set(), []
    for _ in range(k * 8):
        _, terminal, log_r = gfn.sample(mission, reward_fn, rng, 0.0)
        key = frozenset(terminal)
        if key and key not in seen:
            seen.add(key)
            out.append((sorted(pool[i] for i in key), log_r))
        if len(out) >= k:
            break
    out.sort(key=lambda t: -t[1])
    return out


def greedy_proposal(pool, rfn: SetReward, max_len: int):
    """The COUNTERFACTUAL arm (batch-4 pilot, Joe 2026-07-11): greedy argmax of
    the SAME memoized reward the GFN samples from — generated and SEALED but
    not flown; exists so mechanism attribution (sampler vs reward) can be
    studied retrospectively at zero flight cost."""
    state = frozenset()
    cur = rfn(state)
    while len(state) < max_len:
        best, best_gain = None, 0.0
        for i in range(len(pool)):
            if i in state:
                continue
            gain = rfn(frozenset(state | {i})) - cur
            if gain > best_gain:
                best, best_gain = i, gain
        if best is None:
            break
        state = frozenset(state | {best})
        cur += best_gain
    return sorted(pool[i] for i in state), cur


def proposal_hash(mission, patterns):
    return hashlib.sha256((mission + "|" + "|".join(sorted(patterns)))
                          .encode()).hexdigest()[:16]


def main(argv=None):
    p = argparse.ArgumentParser()
    p.add_argument("missions", nargs="+")
    p.add_argument("--k", type=int, default=8)
    p.add_argument("--pool", type=int, default=24)
    p.add_argument("--beta", type=float, default=0.5)
    p.add_argument("--steps", type=int, default=3000)
    p.add_argument("--batch", type=int, default=16)
    p.add_argument("--lr", type=float, default=0.05)
    p.add_argument("--eps", type=float, default=0.10)
    p.add_argument("--max-len", type=int, default=5)
    p.add_argument("--seed", type=int, default=SEED)
    p.add_argument("--tag", default="batch")
    args = p.parse_args(argv)

    reward = RewardV0(load_records())
    blinded, sealed = [], {}
    for mission in args.missions:
        doc = locate_doc(mission)
        if not doc:
            print(f"!! no doc for {mission}, skipping")
            continue
        psi = identify_psi(doc)
        want = salient(psi, DROP)
        pool, sims = build_pool(psi, args.pool)
        rfn = SetReward(pool, want, reward, args.beta)
        gfn_props = train_and_sample(
            mission, pool, rfn, steps=args.steps, batch=args.batch,
            lr=args.lr, eps=args.eps, max_len=args.max_len, k=args.k,
            seed=args.seed)
        inc = construct_cascade(psi)
        inc_pats = [c for c, _, _ in inc["cascade"]]
        entries = [(pats, "gfn", lr_) for pats, lr_ in gfn_props]
        if inc_pats:
            entries.append((inc_pats, "incumbent", None))
        greedy_pats, greedy_lr = greedy_proposal(pool, rfn, args.max_len)
        if greedy_pats:
            entries.append((greedy_pats, "greedy-rhat", greedy_lr))
        for pats, who, lr_ in entries:
            h = proposal_hash(mission, pats)
            blinded.append({"proposal": h, "mission": mission, "patterns": pats})
            sealed[h] = {"proposer": who,
                         **({"log_reward": lr_} if lr_ is not None else {})}
        print(f"== {mission}: pool {len(pool)} (top sim {sims[0]:.2f}), "
              f"{len(gfn_props)} distinct GFN proposals, "
              f"incumbent size {len(inc_pats)}, reward evals {len(rfn.cache)}")

    rng = random.Random(args.seed)
    rng.shuffle(blinded)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    stamp = args.tag
    (OUT_DIR / f"proposals-{stamp}.json").write_text(json.dumps(blinded, indent=1))
    (OUT_DIR / f"proposals-{stamp}.SEALED.json").write_text(json.dumps(sealed, indent=1))
    print(f"\nwrote {len(blinded)} blinded proposals -> proposals-{stamp}.json "
          f"(+ sealed identities; adjudicators read the blinded file ONLY)")


if __name__ == "__main__":
    main()
