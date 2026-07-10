#!/usr/bin/env python3
"""Slice-2a v2: mission-conditioned discharge GFlowNet recovery.

V1 was an honest null because the reward was global:

    log R(S) = beta * sum bonus(p)

V2 makes the reward mission-conditioned:

    log R(S | m) = sum_p alpha * rel(m, p) + beta * bonus(p)

``bonus`` is the validated TRAIN-only alive-vs-mess log-odds from
``pattern_aliveness_reward.fit_credits(train)[1]``. ``rel`` is propagated from a
mission's cosine seed patterns through the phylogeny graph so targets can be
reachable even when ``applied ∩ try_candidates = ∅``.
"""
from __future__ import annotations

import argparse
import json
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
FINDINGS_PATH = SLUSH_DIR / "findings" / "slice2_v2_recovery_findings.md"
RESULTS_PATH = SLUSH_DIR / "findings" / "slice2_v2_recovery_results.json"

DEFAULT_SEEDS = (20260710, 20260711)


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
    rel: Dict[str, float]
    pool: List[str]


class PatternPolicy(nn.Module):
    def __init__(self, n_patterns: int, dim: int = 32):
        super().__init__()
        self.emb = nn.Embedding(n_patterns + 1, dim)
        self.scorer = nn.Linear(dim * 2 + 2, 1)
        with torch.no_grad():
            self.scorer.weight.zero_()
            self.scorer.bias.zero_()
            self.scorer.weight[0, -2] = 1.0
            self.scorer.weight[0, -1] = 1.0
        self.log_z = nn.Parameter(torch.zeros(1))

    def state_vec(self, selected: Sequence[int]) -> torch.Tensor:
        if not selected:
            return torch.zeros(self.emb.embedding_dim)
        return self.emb(torch.tensor(selected, dtype=torch.long)).mean(0)

    def logits(
        self,
        selected: Sequence[int],
        options: Sequence[int],
        features: Dict[int, Tuple[float, float]],
    ) -> torch.Tensor:
        state = self.state_vec(selected)
        feats = []
        for opt in options:
            rel_x, bonus_x = features.get(opt, (0.0, 0.0))
            scalar = torch.tensor([rel_x, bonus_x], dtype=torch.float32)
            feats.append(torch.cat([self.emb(torch.tensor(opt, dtype=torch.long)), state, scalar]))
        return self.scorer(torch.stack(feats)).squeeze(1)


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
        scopes[mission] = MissionScope(mission, applied, tries)
    return scopes


def load_weighted_graph(path: Path = PHYLOGENY_PATH) -> Tuple[Dict[str, Dict[str, float]], List[str]]:
    data = json.loads(path.read_text())
    graph: Dict[str, Dict[str, float]] = defaultdict(dict)
    for edge in data.get("co_app", []):
        a, b = sp.pattern_stem(edge[0]), sp.pattern_stem(edge[1])
        w = float(edge[2]) if len(edge) > 2 else 1.0
        graph[a][b] = max(graph[a].get(b, 0.0), w)
        graph[b][a] = max(graph[b].get(a, 0.0), w)
    for edge in data.get("descent", []):
        a, b = sp.pattern_stem(edge[0]), sp.pattern_stem(edge[1])
        graph[a][b] = max(graph[a].get(b, 0.0), 0.5)
        graph[b][a] = max(graph[b].get(a, 0.0), 0.5)
    library = sorted(sp.pattern_stem(p) for p in data.get("patterns", []))
    return dict(graph), library


def labelled_by_mission() -> Dict[str, Dict]:
    return {sp.normalize_mission_id(m["mission"]): m for m in par.labelled_missions()}


def propagated_relevance(
    seeds: Sequence[Tuple[str, float]],
    graph: Dict[str, Dict[str, float]],
    *,
    hops: int,
    decay: float,
) -> Dict[str, float]:
    scores: Dict[str, float] = defaultdict(float)
    frontier: Dict[str, float] = {p: max(0.0, cos) for p, cos in seeds}
    for p, v in frontier.items():
        scores[p] += v
    for hop in range(1, hops + 1):
        nxt: Dict[str, float] = defaultdict(float)
        for p, score in frontier.items():
            for q, w in graph.get(p, {}).items():
                nxt[q] += score * float(w) * (decay ** hop)
        for p, v in nxt.items():
            scores[p] += v
        frontier = nxt
    if not scores:
        return {}
    mx = max(scores.values()) or 1.0
    return {p: float(v / mx) for p, v in scores.items() if v > 0.0}


def build_cases(
    scopes: Dict[str, MissionScope],
    labelled: Dict[str, Dict],
    graph: Dict[str, Dict[str, float]],
    library: Sequence[str],
    *,
    hops: int,
    decay: float,
    pool_top_n: int,
) -> List[EvalCase]:
    cases: List[EvalCase] = []
    for mission in sorted(set(scopes) & set(labelled)):
        scope = scopes[mission]
        target = set(scope.applied)
        if not target or not scope.try_candidates:
            continue
        seeds = [p for p, _ in scope.try_candidates]
        cos = {p: c for p, c in scope.try_candidates}
        rel = propagated_relevance(scope.try_candidates, graph, hops=hops, decay=decay)
        ranked_rel = [p for p, _ in sorted(rel.items(), key=lambda kv: (-kv[1], kv[0]))]
        pool = set(seeds)
        pool.update(ranked_rel[:pool_top_n])
        for p in seeds:
            pool.update(graph.get(p, {}).keys())
        pool = {p for p in pool if p in library}
        cases.append(EvalCase(mission, target, seeds, cos, rel, sorted(pool)))
    return cases


def admissible_indices(
    selected: Sequence[int],
    case: EvalCase,
    pattern_to_idx: Dict[str, int],
    idx_to_pattern: Sequence[str],
    graph: Dict[str, Dict[str, float]],
) -> List[int]:
    selected_set = set(selected)
    if not selected:
        opts = [pattern_to_idx[p] for p in case.seeds if p in pattern_to_idx]
        if opts:
            return [i for i in opts if i not in selected_set]
    selected_patterns = [idx_to_pattern[i] for i in selected]
    allowed = set(case.seeds)
    for p in selected_patterns:
        allowed.update(graph.get(p, {}).keys())
    opts = [pattern_to_idx[p] for p in case.pool if p in allowed and p in pattern_to_idx]
    opts = [i for i in opts if i not in selected_set]
    if opts:
        return opts
    return [pattern_to_idx[p] for p in case.pool if p in pattern_to_idx and pattern_to_idx[p] not in selected_set]


def feature_map(
    case: EvalCase,
    bonus: Dict[str, float],
    pattern_to_idx: Dict[str, int],
    *,
    alpha: float,
    beta: float,
) -> Dict[int, Tuple[float, float]]:
    return {
        pattern_to_idx[p]: (alpha * case.rel.get(p, 0.0), beta * bonus.get(p, 0.0))
        for p in case.pool
        if p in pattern_to_idx
    }


def sample_trajectory(
    policy: PatternPolicy,
    case: EvalCase,
    pattern_to_idx: Dict[str, int],
    idx_to_pattern: Sequence[str],
    graph: Dict[str, Dict[str, float]],
    features: Dict[int, Tuple[float, float]],
    max_len: int,
    greedy: bool = False,
) -> Tuple[List[str], torch.Tensor]:
    stop_idx = len(idx_to_pattern)
    selected: List[int] = []
    log_pf = torch.zeros(1)
    for _ in range(max_len + 1):
        add_opts = admissible_indices(selected, case, pattern_to_idx, idx_to_pattern, graph)
        opts = add_opts + [stop_idx]
        logits = policy.logits(selected, opts, features)
        lp = F.log_softmax(logits, dim=0)
        choice = int(torch.argmax(lp)) if greedy else int(torch.distributions.Categorical(logits=logits).sample())
        log_pf = log_pf + lp[choice]
        picked = opts[choice]
        if picked == stop_idx:
            break
        selected.append(picked)
        if len(selected) >= max_len:
            break
    return [idx_to_pattern[i] for i in selected], log_pf


def log_reward(selected: Iterable[str], case: EvalCase, bonus: Dict[str, float], alpha: float, beta: float) -> float:
    score = sum(alpha * case.rel.get(p, 0.0) + beta * bonus.get(p, 0.0) for p in selected)
    return float(max(-30.0, min(30.0, score)))


def train_policy(
    train_cases: Sequence[EvalCase],
    bonus: Dict[str, float],
    graph: Dict[str, Dict[str, float]],
    idx_to_pattern: Sequence[str],
    pattern_to_idx: Dict[str, int],
    *,
    seed: int,
    steps: int,
    alpha: float,
    beta: float,
    max_len: int,
) -> PatternPolicy:
    torch.manual_seed(seed)
    rng = random.Random(seed)
    policy = PatternPolicy(len(idx_to_pattern))
    opt = torch.optim.Adam(policy.parameters(), lr=3e-3)
    usable = [c for c in train_cases if c.pool]
    for _ in range(steps):
        case = rng.choice(usable)
        features = feature_map(case, bonus, pattern_to_idx, alpha=alpha, beta=beta)
        selected, log_pf = sample_trajectory(
            policy, case, pattern_to_idx, idx_to_pattern, graph, features, max_len
        )
        loss = (policy.log_z + log_pf - log_reward(selected, case, bonus, alpha, beta)) ** 2
        opt.zero_grad()
        loss.backward()
        opt.step()
    return policy


def ranked_by_gfn(
    policy: PatternPolicy,
    case: EvalCase,
    bonus: Dict[str, float],
    graph: Dict[str, Dict[str, float]],
    idx_to_pattern: Sequence[str],
    pattern_to_idx: Dict[str, int],
    *,
    alpha: float,
    beta: float,
    seed: int,
    k_samples: int,
    max_len: int,
) -> List[str]:
    torch.manual_seed(seed)
    counts: Counter[str] = Counter()
    features = feature_map(case, bonus, pattern_to_idx, alpha=alpha, beta=beta)
    for _ in range(k_samples):
        selected, _ = sample_trajectory(
            policy, case, pattern_to_idx, idx_to_pattern, graph, features, max_len
        )
        counts.update(set(selected))
    return [p for p, _ in sorted(counts.items(), key=lambda kv: (-kv[1], -case.rel.get(kv[0], 0.0), kv[0]))]


def recall_at_budget(ranked: Sequence[str], target: Set[str], budget: int) -> float:
    return len(set(ranked[:budget]) & target) / len(target) if target else 0.0


def random_expected_recall(case: EvalCase, budget: int) -> float:
    if not case.pool or not case.target:
        return 0.0
    return (min(budget, len(case.pool)) / len(case.pool)) * (len(set(case.pool) & case.target) / len(case.target))


def popularity_rank(train_labelled: Sequence[Dict], pool: Set[str]) -> List[str]:
    counts: Counter[str] = Counter()
    for m in train_labelled:
        counts.update(m["applied"])
    return [p for p, _ in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])) if p in pool]


def shuffled_train(train_labelled: Sequence[Dict], seed: int) -> List[Dict]:
    rng = np.random.default_rng(seed)
    perm = rng.permutation(len(train_labelled))
    return [
        dict(m, y=train_labelled[int(perm[i])]["y"], L=train_labelled[int(perm[i])]["L"])
        for i, m in enumerate(train_labelled)
    ]


def mean(xs: Iterable[float]) -> float:
    vals = list(xs)
    return float(np.mean(vals)) if vals else 0.0


def evaluate_condition(
    cases: Sequence[EvalCase],
    labelled: Dict[str, Dict],
    graph: Dict[str, Dict[str, float]],
    *,
    seed: int,
    steps: int,
    k_samples: int,
    alpha: float,
    beta: float,
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
        policy = train_policy(
            [c for c in cases if c.mission != held.mission],
            bonus,
            graph,
            all_patterns,
            pattern_to_idx,
            seed=seed + held_idx,
            steps=steps,
            alpha=alpha,
            beta=beta,
            max_len=max_len,
        )
        budget = max(1, len(held.target))
        gfn_rank = ranked_by_gfn(
            policy,
            held,
            bonus,
            graph,
            all_patterns,
            pattern_to_idx,
            alpha=alpha,
            beta=beta,
            seed=seed + 7919 + held_idx,
            k_samples=k_samples,
            max_len=max_len,
        )
        pool = set(held.pool)
        rel_rank = [p for p, _ in sorted(held.rel.items(), key=lambda kv: (-kv[1], kv[0])) if p in pool]
        pop_rank = popularity_rank(train_labelled, pool)
        rows.append(
            {
                "mission": held.mission,
                "target_n": len(held.target),
                "pool_n": len(held.pool),
                "reachable_n": len(pool & held.target),
                "budget": budget,
                "gfn_recall": recall_at_budget(gfn_rank, held.target, budget),
                "rel_rank_recall": recall_at_budget(rel_rank, held.target, budget),
                "popularity_recall": recall_at_budget(pop_rank, held.target, budget),
                "random_expected_recall": random_expected_recall(held, budget),
                "proposal_bonus": sum(bonus.get(p, 0.0) for p in gfn_rank[:budget]),
            }
        )
    keys = ("gfn_recall", "rel_rank_recall", "popularity_recall", "random_expected_recall", "proposal_bonus")
    return {
        "seed": seed,
        "steps": steps,
        "k_samples": k_samples,
        "alpha": alpha,
        "beta": beta,
        "shuffle_labels": shuffle_labels,
        "n": len(rows),
        "mean": {k: mean(r[k] for r in rows) for k in keys},
        "reachable_mean": mean(r["reachable_n"] / r["target_n"] for r in rows),
        "rows": rows,
    }


def condition_name(alpha: float, beta: float) -> str:
    if alpha > 0 and beta > 0:
        return "rel+aliveness"
    if alpha > 0:
        return "rel-only"
    return "aliveness-only"


def aggregate(runs: Sequence[Dict], alpha: float | None = None, beta: float | None = None, metric: str = "gfn_recall") -> float:
    filt = [
        r for r in runs
        if (alpha is None or abs(r["alpha"] - alpha) < 1e-12)
        and (beta is None or abs(r["beta"] - beta) < 1e-12)
    ]
    return mean(r["mean"][metric] for r in filt)


def run_suite(args: argparse.Namespace) -> Dict:
    scopes = parse_mission_scopes()
    labelled = labelled_by_mission()
    graph, library = load_weighted_graph()
    cases = build_cases(
        scopes,
        labelled,
        graph,
        library,
        hops=args.hops,
        decay=args.decay,
        pool_top_n=args.pool_top_n,
    )
    real_runs = []
    shuffle_runs = []
    for alpha, beta in args.ratios:
        for seed in args.seeds:
            real_runs.append(
                evaluate_condition(
                    cases,
                    labelled,
                    graph,
                    seed=seed,
                    steps=args.steps,
                    k_samples=args.k,
                    alpha=alpha,
                    beta=beta,
                    max_len=args.max_len,
                    shuffle_labels=False,
                )
            )
            if alpha > 0 and beta > 0:
                shuffle_runs.append(
                    evaluate_condition(
                        cases,
                        labelled,
                        graph,
                        seed=seed,
                        steps=args.steps,
                        k_samples=args.k,
                        alpha=alpha,
                        beta=beta,
                        max_len=args.max_len,
                        shuffle_labels=True,
                    )
                )
    return {
        "config": {
            "steps": args.steps,
            "k": args.k,
            "max_len": args.max_len,
            "hops": args.hops,
            "decay": args.decay,
            "pool_top_n": args.pool_top_n,
            "ratios": [{"alpha": a, "beta": b} for a, b in args.ratios],
            "seeds": list(args.seeds),
        },
        "n_cases": len(cases),
        "real_runs": real_runs,
        "shuffle_runs": shuffle_runs,
        "headline": headline(real_runs, shuffle_runs),
    }


def headline(real_runs: Sequence[Dict], shuffle_runs: Sequence[Dict]) -> Dict:
    rel_only = aggregate(real_runs, alpha=1.0, beta=0.0)
    alive_only = aggregate(real_runs, alpha=0.0, beta=1.0)
    rel_alive_runs = [r for r in real_runs if r["alpha"] > 0 and r["beta"] > 0]
    rel_alive = mean(r["mean"]["gfn_recall"] for r in rel_alive_runs)
    rel_alive_quality = mean(r["mean"]["proposal_bonus"] for r in rel_alive_runs)
    rel_only_quality = aggregate(real_runs, alpha=1.0, beta=0.0, metric="proposal_bonus")
    shuffle_rel_alive = mean(r["mean"]["gfn_recall"] for r in shuffle_runs)
    return {
        "rel_plus_aliveness": rel_alive,
        "rel_only": rel_only,
        "aliveness_only": alive_only,
        "popularity": aggregate(real_runs, metric="popularity_recall"),
        "random": aggregate(real_runs, metric="random_expected_recall"),
        "ceiling": mean(r["reachable_mean"] for r in real_runs),
        "shuffle_rel_plus_aliveness": shuffle_rel_alive,
        "real_gap_vs_rel_only": rel_alive - rel_only,
        "shuffle_gap_vs_rel_only": shuffle_rel_alive - rel_only,
        "rel_plus_aliveness_quality": rel_alive_quality,
        "rel_only_quality": rel_only_quality,
    }


def pct(x: float) -> str:
    return f"{100.0 * x:.1f}%"


def write_findings(result: Dict, path: Path = FINDINGS_PATH) -> None:
    h = result["headline"]
    ratio_label = ", ".join(
        f"{r['alpha']}:{r['beta']}" for r in result["config"]["ratios"]
    )
    lines = [
        "# Slice-2a v2 Mission-Conditioned Recovery",
        "",
        "Standalone exploratory DERIVE lab. V2 reward is mission-conditioned: "
        "`alpha * rel(mission,p) + beta * bonus(p)`. `bonus` is fit TRAIN-only; "
        "`rel` is 1-2/3-hop phylogeny propagation from cosine seed patterns. "
        "Held-out `:applied` is never inserted into the pool.",
        "",
        "## Headline",
        "",
        f"- N held-out missions: {result['n_cases']}",
        f"- Seeds: {', '.join(map(str, result['config']['seeds']))}",
        f"- Ratios alpha:beta: {ratio_label}",
        f"- steps/K/max_len: {result['config']['steps']} / {result['config']['k']} / {result['config']['max_len']}",
        f"- hops/decay/pool_top_n: {result['config']['hops']} / {result['config']['decay']} / {result['config']['pool_top_n']}",
        "",
        "| condition | mean recall@|applied| |",
        "|---|---:|",
        f"| rel+aliveness | {pct(h['rel_plus_aliveness'])} |",
        f"| rel-only | {pct(h['rel_only'])} |",
        f"| aliveness-only | {pct(h['aliveness_only'])} |",
        f"| popularity-only | {pct(h['popularity'])} |",
        f"| random expected | {pct(h['random'])} |",
        f"| reachability ceiling | {pct(h['ceiling'])} |",
        f"| shuffle-null rel+aliveness | {pct(h['shuffle_rel_plus_aliveness'])} |",
        "",
        "## Marginal Aliveness Readout",
        "",
        f"- Real rel+alive minus rel-only recovery gap: {pct(h['real_gap_vs_rel_only'])}",
        f"- Shuffle-null rel+alive minus rel-only recovery gap: {pct(h['shuffle_gap_vs_rel_only'])}",
        f"- Proposal quality (sum TRAIN bonus): rel+alive {h['rel_plus_aliveness_quality']:.3f} vs rel-only {h['rel_only_quality']:.3f}",
        "",
        "## Ratio / Seed Table",
        "",
        "| labels | alpha | beta | seed | recall | rel-rank | popularity | random | proposal bonus |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for run in result["real_runs"] + result["shuffle_runs"]:
        label = "shuffle-null" if run["shuffle_labels"] else condition_name(run["alpha"], run["beta"])
        m = run["mean"]
        lines.append(
            f"| {label} | {run['alpha']} | {run['beta']} | {run['seed']} | "
            f"{pct(m['gfn_recall'])} | {pct(m['rel_rank_recall'])} | "
            f"{pct(m['popularity_recall'])} | {pct(m['random_expected_recall'])} | "
            f"{m['proposal_bonus']:.3f} |"
        )
    verdict = "PASS" if (
        h["rel_plus_aliveness"] > h["rel_only"]
        and h["rel_plus_aliveness"] > h["popularity"]
        and abs(h["shuffle_gap_vs_rel_only"]) < abs(h["real_gap_vs_rel_only"])
    ) else "HONEST NULL / NO PASS"
    lines += [
        "",
        "## Verdict",
        "",
        f"{verdict}. Success requires rel+aliveness > rel-only and popularity, "
        "stable across ratios/seeds, and the rel+alive minus rel-only gap destroyed by label shuffle.",
        "",
        "Full machine-readable rows are in `slice2_v2_recovery_results.json`.",
        "",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines))


def parse_ratios(raw: Sequence[str]) -> List[Tuple[float, float]]:
    out = []
    for item in raw:
        a, b = str(item).split(":", 1)
        out.append((float(a), float(b)))
    return out


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--steps", type=int, default=12)
    p.add_argument("--k", type=int, default=24)
    p.add_argument("--max-len", type=int, default=5)
    p.add_argument("--hops", type=int, default=2)
    p.add_argument("--decay", type=float, default=0.35)
    p.add_argument("--pool-top-n", type=int, default=180)
    p.add_argument("--ratios", nargs="+", default=["1:0", "1:0.5", "1:1", "0:1"])
    p.add_argument("--seeds", type=int, nargs="+", default=list(DEFAULT_SEEDS))
    p.add_argument("--write-findings", action="store_true")
    args = p.parse_args(argv)
    args.ratios = parse_ratios(args.ratios)
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
