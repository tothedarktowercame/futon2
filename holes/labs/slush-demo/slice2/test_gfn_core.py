#!/usr/bin/env python3
"""Tests for the Slice-0 trusted GFN trainer (gfn_core).

Runnable two ways:
    python3 test_gfn_core.py      # no pytest needed
    pytest test_gfn_core.py

The heavier convergence checks use a deliberately tiny problem so the whole
file runs in a few seconds; the full G0a/G0b gate lives in gfn_core.run_gates.
"""
from __future__ import annotations

import math

import numpy as np

import gfn_core as g


def test_logsumexp_matches_reference():
    xs = [0.1, -2.0, 3.5, 3.5]
    ref = math.log(sum(math.exp(x) for x in xs))
    assert abs(g.logsumexp(xs) - ref) < 1e-10
    assert g.logsumexp([]) == -math.inf


def test_log_softmax_normalises():
    lg = np.array([0.2, -1.0, 2.0, 0.0])
    ls = g.log_softmax(lg)
    assert abs(float(np.exp(ls).sum()) - 1.0) < 1e-12


def test_enumerate_sets_count():
    # subsets of size 0..2 over 5 items = C(5,0)+C(5,1)+C(5,2) = 1+5+10
    sets = g.enumerate_sets(5, 2)
    assert len(sets) == 16
    assert frozenset() in sets


def test_enumerate_trajectories_orderings():
    # a set of size k is reached by k! ordered add-templates
    trajs = g.enumerate_trajectories(4, 2)
    by_terminal = {}
    for t in trajs:
        by_terminal.setdefault(t.terminal, 0)
        by_terminal[t.terminal] += 1
    # empty set: 1 template (STOP immediately)
    assert by_terminal[frozenset()] == 1
    # a size-2 set: 2! = 2 orderings
    two = next(s for s in by_terminal if len(s) == 2)
    assert by_terminal[two] == 2
    # every template ends in STOP
    assert all(t.steps[-1][1] == g.STOP for t in trajs)


def test_exact_gibbs_sums_to_one():
    ms = g.make_missions(seed=1, n=5, max_len=2, specs=[("M", 4, 1.0)])
    sets = g.enumerate_sets(5, 2)
    probs, lz = g.exact_gibbs(ms[0].log_reward, sets)
    assert abs(sum(probs.values()) - 1.0) < 1e-10
    assert lz == g.logsumexp([ms[0].log_reward(x) for x in sets])


def test_untrained_marginal_sums_to_one():
    # zero logits -> a valid (uniform-ish) sampler; marginal must still be a
    # probability distribution over terminal sets.
    gfn = g.GFN(n=5, max_len=2, conditional_logz=True)
    sets = g.enumerate_sets(5, 2)
    marg = gfn.terminal_marginal("M", sets)
    assert abs(sum(marg.values()) - 1.0) < 1e-9


def test_missions_have_logz_spread():
    # the F2 precondition: >=3 missions whose true Z differs by >= 3 nats
    n, max_len = 8, 2
    specs = [("A", 3, 0.4), ("B", 6, 0.9), ("C", 10, 1.4)]
    ms = g.make_missions(seed=7, n=n, max_len=max_len, specs=specs)
    sets = g.enumerate_sets(n, max_len)
    lzs = [g.exact_gibbs(m.log_reward, sets)[1] for m in ms]
    assert max(lzs) - min(lzs) >= 3.0


def test_conditional_arm_converges_and_shared_arm_fails():
    # tiny but genuine G0a/G0b in miniature
    n, max_len = 6, 2
    specs = [("A", 3, 0.4), ("B", 6, 1.0), ("C", 9, 1.5)]
    ms = g.make_missions(seed=3, n=n, max_len=max_len, specs=specs)
    sets = g.enumerate_sets(n, max_len)
    trajs = g.enumerate_trajectories(n, max_len)

    cond = g.train_exact(ms, trajs, conditional_logz=True, iters=500, lr=0.05)
    shar = g.train_exact(ms, trajs, conditional_logz=False, iters=500, lr=0.05)

    cond_tv = max(
        g.total_variation(cond.terminal_marginal(m.mid, sets),
                          g.exact_gibbs(m.log_reward, sets)[0])
        for m in ms
    )
    shar_tv = max(
        g.total_variation(shar.terminal_marginal(m.mid, sets),
                          g.exact_gibbs(m.log_reward, sets)[0])
        for m in ms
    )
    # conditional recovers Gibbs; shared logZ is clearly worse (F2)
    assert cond_tv < 0.05, f"conditional TV too high: {cond_tv}"
    assert shar_tv > cond_tv + 0.10, f"shared not distinguishable: {shar_tv} vs {cond_tv}"


def test_conditional_logz_recovers_true_partition():
    n, max_len = 6, 2
    specs = [("A", 4, 0.6), ("B", 8, 1.1)]
    ms = g.make_missions(seed=11, n=n, max_len=max_len, specs=specs)
    sets = g.enumerate_sets(n, max_len)
    trajs = g.enumerate_trajectories(n, max_len)
    cond = g.train_exact(ms, trajs, conditional_logz=True, iters=500, lr=0.05)
    for m in ms:
        true_lz = g.exact_gibbs(m.log_reward, sets)[1]
        assert abs(cond.get_logz(m.mid) - true_lz) < 0.1


def _run_all():
    fns = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for fn in fns:
        fn()
        print(f"ok  {fn.__name__}")
    print(f"\n{len(fns)} tests passed")


if __name__ == "__main__":
    _run_all()
