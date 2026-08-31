# C86 — exact recorded ablation

Date: 2026-08-31

The complete g1/snatcher-dominant five-policy score table is recorded as exact dyadic rationals obtained from the IEEE-754 binary64 values. This claims only the recorded computation, not ideal real-valued scores.

The declaration was strengthened while being narrowed: both minimizer sets must exist and be disjoint. The prior existential form could be satisfied by choosing two different members of one tied minimizer set, so it did not actually express movement. Over the pinned table, G has minimizers `{grim, probe-one-token}` and pragmatic risk has the unique minimizer `{always-abstain}`; Lean proves both facts and their disjointness.

The negative control replaces every pragmatic score with its G score. The minimizer sets then coincide, and the checker rejects the table. Thus the claim is not true by construction.
