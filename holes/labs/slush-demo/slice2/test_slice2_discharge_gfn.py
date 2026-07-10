from __future__ import annotations

from pathlib import Path

import slice2_discharge_gfn as s2


def test_parse_mission_scopes_reads_applied_and_try_candidates():
    scopes = s2.parse_mission_scopes()
    rec = scopes["M-IRC-stability"]
    assert rec.applied == ("loop-failure-signals",)
    assert rec.try_candidates[0][0] == "single-line-transport"
    assert isinstance(rec.try_candidates[0][1], float)


def test_build_cases_does_not_add_answers_to_candidate_pool():
    scopes = {
        "M-held": s2.MissionScope(
            mission="M-held",
            applied=("answer-pattern",),
            try_candidates=(("seed-pattern", 0.9),),
        )
    }
    labelled = {"M-held": {"mission": "M-held", "applied": {"answer-pattern"}, "y": 1.0, "L": 50.0}}
    neighbours = {"seed-pattern": {"neighbour-pattern"}}
    cases = s2.build_cases(scopes, labelled, neighbours)
    assert len(cases) == 1
    assert "answer-pattern" not in cases[0].pool
    assert set(cases[0].pool) == {"seed-pattern", "neighbour-pattern"}


def test_evaluate_config_is_deterministic_on_tiny_cases(monkeypatch):
    cases = [
        s2.EvalCase("M-a", {"p1"}, ["p1"], {"p1": 1.0}, ["p1", "p2"]),
        s2.EvalCase("M-b", {"p2"}, ["p2"], {"p2": 1.0}, ["p1", "p2"]),
    ]
    labelled = {
        "M-a": {"mission": "M-a", "applied": {"p1"}, "y": 1.0, "L": 50.0},
        "M-b": {"mission": "M-b", "applied": {"p2"}, "y": 0.0, "L": 20.0},
    }
    neighbours = {"p1": {"p2"}, "p2": {"p1"}}

    def fake_fit(train):
        bonus = {}
        for rec in train:
            for p in rec["applied"]:
                bonus[p] = 1.0 if rec["y"] == 1.0 else -1.0
        return {}, bonus

    monkeypatch.setattr(s2.par, "fit_credits", fake_fit)
    kwargs = dict(seed=7, steps=4, k_samples=3, beta=1.0, eig_lambda=0.0, max_len=2)
    r1 = s2.evaluate_config(cases, labelled, neighbours, shuffle_labels=False, **kwargs)
    r2 = s2.evaluate_config(cases, labelled, neighbours, shuffle_labels=False, **kwargs)
    assert r1["mean"] == r2["mean"]
    assert r1["rows"] == r2["rows"]


def test_write_findings(tmp_path: Path):
    result = {
        "config": {
            "variants": [{"steps": 1, "k": 1}],
            "beta": 1.0,
            "eig_lambda": 0.0,
            "max_len": 1,
            "seeds": [1],
            "slice1_recall": 0.25,
        },
        "n_cases": 1,
        "headline": {
            "gfn": 0.5,
            "retrieval": 0.25,
            "popularity": 0.0,
            "random": 0.1,
            "shuffle_null_gfn": 0.0,
            "slice1": 0.25,
            "reachable": 1.0,
        },
        "real_runs": [
            {
                "shuffle_labels": False,
                "seed": 1,
                "steps": 1,
                "k_samples": 1,
                "mean": {
                    "gfn_recall": 0.5,
                    "retrieval_recall": 0.25,
                    "popularity_recall": 0.0,
                    "random_expected_recall": 0.1,
                },
            }
        ],
        "shuffle_runs": [
            {
                "shuffle_labels": True,
                "seed": 1,
                "steps": 1,
                "k_samples": 1,
                "mean": {
                    "gfn_recall": 0.0,
                    "retrieval_recall": 0.25,
                    "popularity_recall": 0.0,
                    "random_expected_recall": 0.1,
                },
            }
        ],
    }
    out = tmp_path / "findings.md"
    s2.write_findings(result, out)
    text = out.read_text()
    assert "Slice-2a" in text
    assert "discharge GFN" in text
    assert "label-shuffle null" in text
