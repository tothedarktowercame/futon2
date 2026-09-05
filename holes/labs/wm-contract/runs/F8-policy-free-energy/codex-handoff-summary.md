# F8 leg 1 slice 4 handoff

Commits: mathlib4 `3783d50968e21801198e8394135a770bf75f22f3`;
futon2 **this commit** (its SHA is reported in the dispatch reply because a
commit cannot contain its own hash without changing that hash).

The registry carrier is `machinePolicyFreeEnergy`.

Lean keeps `Real.log` symbolic and proves the residual, variance-branch and
temperature-scaling algebra exactly. The production readback compares complete
floating terms at tolerance `1e-12` and prints measured deltas: positive
two-channel `0.0`; absent-zero floor `1.1102230246251565e-16`; both
`f-pi-vector` candidates `0.0`. Deterministic tolerated/rejected, bare-zero,
negative-variance, and both score-scaling arms match their typed or exact
reference results.

Four production statements are present: the composite carrier; the variance
trichotomy including the absent-zero floor gate; successful totals inhabit
`Real` while failures inhabit `Except`; and the default score holds F_pi fixed
when tau changes while `:by-tau` scales it.

Gates after the last source edit: direct Lean main exit 0, 0 errors, 0 warnings;
direct Lean witness exit 0, 0 errors, 0 warnings; Lake build exit 0, 2709/2709;
axiom audit exit 0 over 21 declarations, 0 `sorryAx`; clj-kondo exit 0, 0
errors, 0 warnings; check-parens exit 0, OK; deterministic readback comparison
exit 0. The final lean-state probe reports 79 modules, 879 declarations,
13,108 source lines, sorry 10 source and axioms 0.

Found and not repaired: the registry row's stale `:code` pointers described in
the dispatch. No registry, worklist, control map, C5xx report, p4ng file,
publish, or generated DAG was changed. No live tick or run lock was taken and
nothing was written under `data/`.
