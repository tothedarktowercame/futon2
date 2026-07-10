from __future__ import annotations

from pathlib import Path

import slice2_discharge_gfn as s2


def test_parse_mission_scopes_reads_applied_and_try_candidates():
    scopes = s2.parse_mission_scopes()
    rec = scopes["M-IRC-stability"]
    assert rec.applied == ("loop-failure-signals",)
    assert rec.try_candidates[0][0] == "single-line-transport"
    assert isinstance(rec.try_candidates[0][1], float)


def test_propagated_relevance_reaches_two_hop_patterns_and_normalizes():
    graph = {
        "seed": {"near": 2.0},
        "near": {"target": 3.0},
        "target": {},
    }
    rel = s2.propagated_relevance((("seed", 0.5),), graph, hops=2, decay=0.5)
    assert rel["seed"] > 0
    assert rel["near"] > 0
    assert rel["target"] > 0
    assert max(rel.values()) == 1.0


def test_build_cases_does_not_add_answers_but_can_reach_via_relevance():
    scopes = {
        "M-held": s2.MissionScope(
            mission="M-held",
            applied=("answer-pattern",),
            try_candidates=(("seed-pattern", 0.9),),
        )
    }
    labelled = {"M-held": {"mission": "M-held", "applied": {"answer-pattern"}, "y": 1.0, "L": 50.0}}
    graph = {
        "seed-pattern": {"bridge-pattern": 1.0},
        "bridge-pattern": {"answer-pattern": 1.0},
        "answer-pattern": {},
    }
    cases = s2.build_cases(
        scopes,
        labelled,
        graph,
        ["seed-pattern", "bridge-pattern", "answer-pattern"],
        hops=2,
        decay=1.0,
        pool_top_n=3,
    )
    assert len(cases) == 1
    assert "answer-pattern" in cases[0].pool
    assert cases[0].rel["answer-pattern"] > 0
    assert "answer-pattern" not in cases[0].seeds


def test_evaluate_condition_is_deterministic_on_tiny_cases(monkeypatch):
    cases = [
        s2.EvalCase("M-a", {"p1"}, ["p1"], {"p1": 1.0}, {"p1": 1.0, "p2": 0.5}, ["p1", "p2"]),
        s2.EvalCase("M-b", {"p2"}, ["p2"], {"p2": 1.0}, {"p1": 0.5, "p2": 1.0}, ["p1", "p2"]),
    ]
    labelled = {
        "M-a": {"mission": "M-a", "applied": {"p1"}, "y": 1.0, "L": 50.0},
        "M-b": {"mission": "M-b", "applied": {"p2"}, "y": 0.0, "L": 20.0},
    }
    graph = {"p1": {"p2": 1.0}, "p2": {"p1": 1.0}}

    def fake_fit(train):
        bonus = {}
        for rec in train:
            for p in rec["applied"]:
                bonus[p] = 1.0 if rec["y"] == 1.0 else -1.0
        return {}, bonus

    monkeypatch.setattr(s2.par, "fit_credits", fake_fit)
    kwargs = dict(seed=7, steps=3, k_samples=3, alpha=1.0, beta=0.5, max_len=2)
    r1 = s2.evaluate_condition(cases, labelled, graph, shuffle_labels=False, **kwargs)
    r2 = s2.evaluate_condition(cases, labelled, graph, shuffle_labels=False, **kwargs)
    assert r1["mean"] == r2["mean"]
    assert r1["rows"] == r2["rows"]


def test_write_findings(tmp_path: Path):
    result = {
        "config": {
            "steps": 1,
            "k": 1,
            "max_len": 1,
            "hops": 2,
            "decay": 0.5,
            "pool_top_n": 10,
            "ratios": [{"alpha": 1.0, "beta": 0.0}, {"alpha": 1.0, "beta": 0.5}],
            "seeds": [1],
        },
        "n_cases": 1,
        "headline": {
            "rel_plus_aliveness": 0.5,
            "rel_only": 0.25,
            "aliveness_only": 0.0,
            "popularity": 0.0,
            "random": 0.1,
            "ceiling": 1.0,
            "shuffle_rel_plus_aliveness": 0.25,
            "real_gap_vs_rel_only": 0.25,
            "shuffle_gap_vs_rel_only": 0.0,
            "rel_plus_aliveness_quality": 1.0,
            "rel_only_quality": 0.5,
        },
        "real_runs": [
            {
                "shuffle_labels": False,
                "alpha": 1.0,
                "beta": 0.5,
                "seed": 1,
                "mean": {
                    "gfn_recall": 0.5,
                    "rel_rank_recall": 0.25,
                    "popularity_recall": 0.0,
                    "random_expected_recall": 0.1,
                    "proposal_bonus": 1.0,
                },
            }
        ],
        "shuffle_runs": [
            {
                "shuffle_labels": True,
                "alpha": 1.0,
                "beta": 0.5,
                "seed": 1,
                "mean": {
                    "gfn_recall": 0.25,
                    "rel_rank_recall": 0.25,
                    "popularity_recall": 0.0,
                    "random_expected_recall": 0.1,
                    "proposal_bonus": 0.0,
                },
            }
        ],
    }
    out = tmp_path / "findings.md"
    s2.write_findings(result, out)
    text = out.read_text()
    assert "Slice-2a v2" in text
    assert "rel+aliveness" in text
    assert "shuffle-null" in text
