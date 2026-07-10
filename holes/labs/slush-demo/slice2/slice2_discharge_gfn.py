#!/usr/bin/env python3
"""Slice-2a discharge-trained pattern GFlowNet recovery experiment.

Standalone lab script.  It trains a small trajectory-balance-style policy over
pattern sets with reward

    log R(S) = beta * sum_{p in S} bonus(p) + eig_lambda * EIG(S)

where ``bonus`` is fit on the leave-one-out TRAIN split by
``pattern_aliveness_reward.fit_credits``.  Held-out recovery never adds the
held-out answers to the candidate pool: candidates are retrieval hits plus their
one-hop phylogeny neighbours.
"""
from __future__ import annotations

import argparse
import json
import math
import random
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Sequence, Set, Tuple

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

HERE = Path(__file__).resolve().parent
SLUSH_DIR = HERE.parent
sys.path.insert(0, str(SLUSH_DIR))

import pattern_aliveness_reward as par  # noqa: E402
import slush_proxy as sp  # noqa: E402

SCOPES_PATH = Path("/home/joe/code/futon6/data/mission-pattern-scopes.edn")
PHYLOGENY_PATH = Path("/home/joe/code/futon6/data/pattern-phylogeny-edges.json")
FINDINGS_PATH = SLUSH_DIR / "findings" / "slice2_recovery_findings.md"
RESULTS_PATH = SLUSH_DIR / "findings" / "slice2_recovery_results.json"

SLICE1_RECALL = 0.25
DEFAULT_SEEDS = (20260710, 20260711, 20260712)


@dataclass(frozen=True)
class MissionScope:
    mission: str
    applied: Tuple[str, ...]
    try_candidates: Tuple[Tuple[str, float], ...]


@dataclass
class EvalCase:
    mission: str
    target: Set[str]
    seeds: List[str]
    cos: Dict[str, float]
    pool: List[str]


class PatternPolicy(nn.Module):
    def __init__(self, n_patterns: int, dim: int = 32):
        super().__init__()
        self.emb = nn.Embedding(n_patterns + 1, dim)
        self.scorer = nn.Sequential(
            nn.Linear(dim * 2, 64),
            nn.ReLU(),
            nn.Linear(64, 1),
        )
        self.log_z = nn.Parameter(torch.zeros(1))

    def state_vec(self, selected: Sequence[int]) -> torch.Tensor:
        if not selected:
            return torch.zeros(self.emb.embedding_dim)
        return self.emb(torch.tensor(selected, dtype=torch.long)).mean(0)

    def logits(self, selected: Sequence[int], options: Sequence[int]) -> torch.Tensor:
        state = self.state_vec(selected)
        feats = torch.stack(
            [torch.cat([self.emb(torch.tensor(opt, dtype=torch.long)), state]) for opt in options]
        )
        return self.scorer(feats).squeeze(1)


def parse_mission_scopes(path: Path = SCOPES_PATH) -> Dict[str, MissionScope]:
    text = path.read_text()
    scopes: Dict[str, MissionScope] = {}
    rx = re.compile(
        r'\{:mission\s+"([^"]+)"\s+:applied\s+\[([^\]]*)\]\s+'
        r':try-candidates\s+\[([^\]]*)\]\}',
        re.DOTALL,
    )
    for m in rx.finditer(text):
        mission = sp.normalize_mission_id(m.group(1))
        applied = tuple(sp.pattern_stem(p) for p in re.findall(r'"([^"]+)"', m.group(2)))
        tries = tuple(
            (sp.pattern_stem(p), float(c))
            for p, c in re.findall(r'\{:pattern\s+"([^"]+)"\s+:cos\s+([-0-9.]+)\}', m.group(3))
        )
        scopes[mission] = MissionScope(mission=mission, applied=applied, try_candidates=tries)
    return scopes


def load_neighbours(path: Path = PHYLOGENY_PATH) -> Dict[str, Set[str]]:
    data = json.loads(path.read_text())
    neigh: Dict[str, Set[str]] = defaultdict(set)
    for key in ("co_app", "descent"):
        for edge in data.get(key, []):
            a, b = sp.pattern_stem(edge[0]), sp.pattern_stem(edge[1])
            neigh[a].add(b)
            neigh[b].add(a)
    return dict(neigh)


def labelled_by_mission() -> Dict[str, Dict]:
    return {sp.normalize_mission_id(m["mission"]): m for m in par.labelled_missions()}


def build_cases(
    scopes: Dict[str, MissionScope],
    labelled: Dict[str, Dict],
    neighbours: Dict[str, Set[str]],
) -> List[EvalCase]:
    cases: List[EvalCase] = []
    for mission in sorted(set(scopes) & set(labelled)):
        scope = scopes[mission]
        target = set(scope.applied)
        if not target or not scope.try_candidates:
            continue
        seeds = [p for p, _ in scope.try_candidates]
        cos = {p: c for p, c in scope.try_candidates}
        pool = set(seeds)
        for p in seeds:
            pool.update(neighbours.get(p, set()))
        cases.append(
            EvalCase(
                mission=mission,
                target=target,
                seeds=seeds,
                cos=cos,
                pool=sorted(pool),
            )
        )
    return cases


def admissible_indices(
    selected: Sequence[int],
    case: EvalCase,
    pattern_to_idx: Dict[str, int],
    idx_to_pattern: Sequence[str],
    neighbours: Dict[str, Set[str]],
) -> List[int]:
    selected_set = set(selected)
    pool = [pattern_to_idx[p] for p in case.pool if p in pattern_to_idx]
    if not selected:
        seed_opts = [pattern_to_idx[p] for p in case.seeds if p in pattern_to_idx]
        return [i for i in seed_opts if i not in selected_set]
    selected_patterns = [idx_to_pattern[i] for i in selected]
    neigh = set(case.seeds)
    for p in selected_patterns:
        neigh.update(neighbours.get(p, set()))
    allowed = [pattern_to_idx[p] for p in case.pool if p in neigh and p in pattern_to_idx]
    return [i for i in allowed if i not in selected_set] or [i for i in pool if i not in selected_set]


def sample_trajectory(
    policy: PatternPolicy,
    case: EvalCase,
    pattern_to_idx: Dict[str, int],
    idx_to_pattern: Sequence[str],
    neighbours: Dict[str, Set[str]],
    max_len: int,
    rng: random.Random,
    greedy: bool = False,
) -> Tuple[List[str], torch.Tensor]:
    stop_idx = len(idx_to_pattern)
    selected: List[int] = []
    log_pf = torch.zeros(1)
    for _ in range(max_len + 1):
        add_opts = admissible_indices(selected, case, pattern_to_idx, idx_to_pattern, neighbours)
        opts = add_opts + [stop_idx]
        logits = policy.logits(selected, opts)
        lp = F.log_softmax(logits, dim=0)
        if greedy:
            choice = int(torch.argmax(lp))
        else:
            choice = int(torch.distributions.Categorical(logits=logits).sample())
        log_pf = log_pf + lp[choice]
        picked = opts[choice]
        if picked == stop_idx:
            break
        selected.append(picked)
        if len(selected) >= max_len:
            break
    return [idx_to_pattern[i] for i in selected], log_pf


def log_reward(
    selected: Iterable[str],
    bonus: Dict[str, float],
    beta: float,
    eig_lambda: float,
) -> float:
    score = beta * sum(bonus.get(p, 0.0) for p in selected)
    if eig_lambda:
        score += eig_lambda * sum(
            sp.CONSTELLATION_EIG.get(sp.PATTERN_TO_CONSTELLATION.get(p), 0.0)
            for p in selected
            if p in sp.PATTERN_TO_CONSTELLATION
        )
    return float(max(-30.0, min(30.0, score)))


def train_policy(
    train_cases: Sequence[EvalCase],
    bonus: Dict[str, float],
    neighbours: Dict[str, Set[str]],
    idx_to_pattern: Sequence[str],
    pattern_to_idx: Dict[str, int],
    *,
    seed: int,
    steps: int,
    beta: float,
    eig_lambda: float,
    max_len: int,
) -> PatternPolicy:
    torch.manual_seed(seed)
    rng = random.Random(seed)
    policy = PatternPolicy(len(idx_to_pattern))
    opt = torch.optim.Adam(policy.parameters(), lr=3e-3)
    usable = [c for c in train_cases if c.pool]
    for _ in range(steps):
        case = rng.choice(usable)
        selected, log_pf = sample_trajectory(
            policy, case, pattern_to_idx, idx_to_pattern, neighbours, max_len, rng
        )
        target_log_r = log_reward(selected, bonus, beta, eig_lambda)
        loss = (policy.log_z + log_pf - target_log_r) ** 2
        opt.zero_grad()
        loss.backward()
        opt.step()
    return policy


def ranked_by_gfn(
    policy: PatternPolicy,
    case: EvalCase,
    neighbours: Dict[str, Set[str]],
    idx_to_pattern: Sequence[str],
    pattern_to_idx: Dict[str, int],
    *,
    seed: int,
    k_samples: int,
    max_len: int,
) -> List[str]:
    rng = random.Random(seed)
    counts: Counter[str] = Counter()
    for _ in range(k_samples):
        selected, _ = sample_trajectory(
            policy, case, pattern_to_idx, idx_to_pattern, neighbours, max_len, rng
        )
        counts.update(set(selected))
    return [
        p
        for p, _ in sorted(
            counts.items(),
            key=lambda kv: (-kv[1], -case.cos.get(kv[0], 0.0), kv[0]),
        )
    ]


def recall_at_budget(ranked: Sequence[str], target: Set[str], budget: int) -> float:
    if not target:
        return 0.0
    picked = set(ranked[:budget])
    return len(picked & target) / len(target)


def random_expected_recall(case: EvalCase, budget: int) -> float:
    reachable = len(set(case.pool) & case.target)
    if not case.pool or not case.target:
        return 0.0
    return (min(budget, len(case.pool)) / len(case.pool)) * (reachable / len(case.target))


def popularity_rank(train_labelled: Sequence[Dict]) -> List[str]:
    counts: Counter[str] = Counter()
    for m in train_labelled:
        counts.update(m["applied"])
    return [p for p, _ in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))]


def shuffled_train(train_labelled: Sequence[Dict], seed: int) -> List[Dict]:
    rng = np.random.default_rng(seed)
    perm = rng.permutation(len(train_labelled))
    return [
        dict(m, y=train_labelled[int(perm[i])]["y"], L=train_labelled[int(perm[i])]["L"])
        for i, m in enumerate(train_labelled)
    ]


def evaluate_config(
    cases: Sequence[EvalCase],
    labelled: Dict[str, Dict],
    neighbours: Dict[str, Set[str]],
    *,
    seed: int,
    steps: int,
    k_samples: int,
    beta: float,
    eig_lambda: float,
    max_len: int,
    shuffle_labels: bool = False,
) -> Dict:
    all_patterns = sorted({p for c in cases for p in c.pool})
    pattern_to_idx = {p: i for i, p in enumerate(all_patterns)}
    rows = []
    for held_idx, held in enumerate(cases):
        train_labelled = [m for mid, m in sorted(labelled.items()) if mid != held.mission]
        if shuffle_labels:
            train_labelled = shuffled_train(train_labelled, seed + 1009 * held_idx)
        _, bonus = par.fit_credits(train_labelled)
        train_cases = [c for c in cases if c.mission != held.mission]
        policy = train_policy(
            train_cases,
            bonus,
            neighbours,
            all_patterns,
            pattern_to_idx,
            seed=seed + held_idx,
            steps=steps,
            beta=beta,
            eig_lambda=eig_lambda,
            max_len=max_len,
        )
        budget = max(1, len(held.target))
        gfn_rank = ranked_by_gfn(
            policy,
            held,
            neighbours,
            all_patterns,
            pattern_to_idx,
            seed=seed + 7919 + held_idx,
            k_samples=k_samples,
            max_len=max_len,
        )
        retrieval_rank = [
            p for p, _ in sorted(held.cos.items(), key=lambda kv: (-kv[1], kv[0]))
        ] + [p for p in held.pool if p not in held.cos]
        pop_rank_all = popularity_rank(train_labelled)
        pop_rank = [p for p in pop_rank_all if p in held.pool]
        rows.append(
            {
                "mission": held.mission,
                "target_n": len(held.target),
                "pool_n": len(held.pool),
                "reachable_n": len(set(held.pool) & held.target),
                "budget": budget,
                "gfn_recall": recall_at_budget(gfn_rank, held.target, budget),
                "retrieval_recall": recall_at_budget(retrieval_rank, held.target, budget),
                "popularity_recall": recall_at_budget(pop_rank, held.target, budget),
                "random_expected_recall": random_expected_recall(held, budget),
            }
        )
    return summarize_rows(rows, seed, steps, k_samples, beta, eig_lambda, shuffle_labels)


def mean(xs: Iterable[float]) -> float:
    vals = list(xs)
    return float(np.mean(vals)) if vals else 0.0


def summarize_rows(
    rows: List[Dict],
    seed: int,
    steps: int,
    k_samples: int,
    beta: float,
    eig_lambda: float,
    shuffle_labels: bool,
) -> Dict:
    keys = ("gfn_recall", "retrieval_recall", "popularity_recall", "random_expected_recall")
    return {
        "seed": seed,
        "steps": steps,
        "k_samples": k_samples,
        "beta": beta,
        "eig_lambda": eig_lambda,
        "shuffle_labels": shuffle_labels,
        "n": len(rows),
        "mean": {k: mean(r[k] for r in rows) for k in keys},
        "micro": {
            k: (
                sum(r[k] * r["target_n"] for r in rows) / max(1, sum(r["target_n"] for r in rows))
            )
            for k in keys
        },
        "reachable_mean": mean(r["reachable_n"] / r["target_n"] for r in rows),
        "rows": rows,
    }


def run_suite(args: argparse.Namespace) -> Dict:
    scopes = parse_mission_scopes()
    labelled = labelled_by_mission()
    neighbours = load_neighbours()
    cases = build_cases(scopes, labelled, neighbours)
    real_runs = []
    shuffle_runs = []
    for steps, k_samples in args.variants:
        for seed in args.seeds:
            real_runs.append(
                evaluate_config(
                    cases,
                    labelled,
                    neighbours,
                    seed=seed,
                    steps=steps,
                    k_samples=k_samples,
                    beta=args.beta,
                    eig_lambda=args.eig_lambda,
                    max_len=args.max_len,
                    shuffle_labels=False,
                )
            )
            shuffle_runs.append(
                evaluate_config(
                    cases,
                    labelled,
                    neighbours,
                    seed=seed,
                    steps=steps,
                    k_samples=k_samples,
                    beta=args.beta,
                    eig_lambda=args.eig_lambda,
                    max_len=args.max_len,
                    shuffle_labels=True,
                )
            )
    return {
        "config": {
            "variants": [{"steps": s, "k": k} for s, k in args.variants],
            "beta": args.beta,
            "eig_lambda": args.eig_lambda,
            "max_len": args.max_len,
            "seeds": list(args.seeds),
            "slice1_recall": SLICE1_RECALL,
        },
        "n_cases": len(cases),
        "real_runs": real_runs,
        "shuffle_runs": shuffle_runs,
        "headline": headline(real_runs, shuffle_runs),
    }


def aggregate_runs(runs: Sequence[Dict], metric: str) -> float:
    return mean(run["mean"][metric] for run in runs)


def headline(real_runs: Sequence[Dict], shuffle_runs: Sequence[Dict]) -> Dict:
    return {
        "gfn": aggregate_runs(real_runs, "gfn_recall"),
        "retrieval": aggregate_runs(real_runs, "retrieval_recall"),
        "popularity": aggregate_runs(real_runs, "popularity_recall"),
        "random": aggregate_runs(real_runs, "random_expected_recall"),
        "shuffle_null_gfn": aggregate_runs(shuffle_runs, "gfn_recall"),
        "slice1": SLICE1_RECALL,
        "reachable": mean(run["reachable_mean"] for run in real_runs),
    }


def pct(x: float) -> str:
    return f"{100.0 * x:.1f}%"


def write_findings(result: Dict, path: Path = FINDINGS_PATH) -> None:
    h = result["headline"]
    variant_label = ", ".join(
        f"{v['steps']}/{v['k']}" for v in result["config"]["variants"]
    )
    lines = [
        "# Slice-2a Discharge-GFN Held-Out Recovery",
        "",
        "Standalone exploratory DERIVE lab. Reward uses TRAIN-only "
        "`pattern_aliveness_reward.fit_credits(...)[1]` (`bonus`, alive-vs-mess log-odds). "
        "Held-out `:applied` is never added to the candidate pool.",
        "",
        "## Headline",
        "",
        f"- N held-out missions: {result['n_cases']}",
        f"- Seeds: {', '.join(map(str, result['config']['seeds']))}",
        f"- Step/K variants: {variant_label}",
        f"- Max selected patterns per trajectory: {result['config']['max_len']}",
        f"- Reachable ceiling from retrieval+one-hop pool: {pct(h['reachable'])}",
        "",
        "| condition | mean recall@|applied| |",
        "|---|---:|",
        f"| discharge GFN | {pct(h['gfn'])} |",
        f"| slice-1 reference | {pct(h['slice1'])} |",
        f"| retrieval-prior beam | {pct(h['retrieval'])} |",
        f"| popularity-only | {pct(h['popularity'])} |",
        f"| random expected | {pct(h['random'])} |",
        f"| label-shuffle null GFN | {pct(h['shuffle_null_gfn'])} |",
        "",
        "## Per-Seed Summary",
        "",
        "| labels | steps | K | seed | GFN | retrieval | popularity | random |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for run in result["real_runs"] + result["shuffle_runs"]:
        label = "shuffle-null" if run["shuffle_labels"] else "real"
        m = run["mean"]
        lines.append(
            f"| {label} | {run['steps']} | {run['k_samples']} | {run['seed']} | "
            f"{pct(m['gfn_recall'])} | "
            f"{pct(m['retrieval_recall'])} | {pct(m['popularity_recall'])} | "
            f"{pct(m['random_expected_recall'])} |"
        )
    verdict = "PASS" if (
        h["gfn"] > h["slice1"]
        and h["gfn"] > h["retrieval"]
        and h["gfn"] > h["popularity"]
        and h["shuffle_null_gfn"] < h["gfn"]
    ) else "HONEST NULL / NO PASS"
    lines += [
        "",
        "## Verdict",
        "",
        f"{verdict}. Acceptance requires GFN > 25%, > retrieval, > popularity, "
        "stable across seeds, and destroyed by label shuffle.",
        "",
        "Full machine-readable rows are in `slice2_recovery_results.json`.",
        "",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines))


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument(
        "--variants",
        nargs="+",
        default=["12:12", "12:24", "24:12"],
        help="Step/K variants, e.g. 12:24 24:24.",
    )
    p.add_argument("--beta", type=float, default=4.0)
    p.add_argument("--eig-lambda", type=float, default=0.0)
    p.add_argument("--max-len", type=int, default=5)
    p.add_argument("--seeds", type=int, nargs="+", default=list(DEFAULT_SEEDS))
    p.add_argument("--write-findings", action="store_true")
    args = p.parse_args(argv)
    parsed = []
    for item in args.variants:
        steps_s, k_s = str(item).split(":", 1)
        parsed.append((int(steps_s), int(k_s)))
    args.variants = parsed
    return args


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    result = run_suite(args)
    print(json.dumps({"headline": result["headline"], "config": result["config"]}, indent=2))
    if args.write_findings:
        RESULTS_PATH.parent.mkdir(parents=True, exist_ok=True)
        RESULTS_PATH.write_text(json.dumps(result, indent=2, sort_keys=True))
        write_findings(result)
        print(f"wrote {RESULTS_PATH}")
        print(f"wrote {FINDINGS_PATH}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
