# Bounded proof review — codex-26, 2026-09-13

Reviewed Lean 168bd082c572ed6014bc741d0a8b66f580a8c3fc, proposal
25e5e9af and receipt cfa2deaa. All six receipt pins match actual bytes
(lead-bounded-pins.json). Retained compiler output reports exit 0 and standard
axioms without sorryAx for the two named theorems. No proof rerun.

The expectation range bound and IVT root-order argument are valid at their
stated assumptions. Baseline uniqueness is not required; new-root uniqueness
is used to identify all other positive roots with the located larger root.
The unrestricted affine example is correctly limited to its unbounded domain.

Accept this conditional mathematical result, not production correspondence or
adoption. One needed refinement: the theorem currently assumes Continuous delta
and a bound on all real inputs. The policy field is only defined for positive
beta. Restate/prove the ordering theorem using ContinuousOn delta (Ioi 0) and
a bound for positive beta; its IVT interval is entirely positive. Connect the
locally restated fixed-point/reciprocal definitions explicitly to the proposal
when assembling the application. Global positive-root uniqueness and exact-real
to floating solver correspondence remain separate obligations, not inferred
from this proof or a numerical sign scan. No admission or live integration.
