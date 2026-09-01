# C288 — the final two glossary terms share one carrier blocker

Date: 2026-09-01

The final terms were assessed in dependency order: pattern-language/cascade,
then policy `pi`, because the latter is defined as the former when scored.
Neither is honestly bindable to the current Lean carrier.

The paper says a cascade is a semilattice with two composition axes:
sequential dependency (`BV.seq`) and cross-cutting co-application (`BV.copar`).
Lean's `Cascade` has one acyclic `edges` relation and a precedence list; it has
no co-application relation and no meet law. `CascadeOrder.lean` states the gap
literally: it models "no probability, overlap, wiring, or fold behaviour," and
its accepting fixture proves only acyclic descent. A fixture for that carrier
could therefore pass while omitting the overlap axis the glossary says
dominates. That would be the partial-witness defect C270 and C282 excluded.

`cascadeGrainPi` is an abbreviation for this same `Cascade`, so policy `pi`
inherits the blocker. The type system already distinguishes cascade-grain `pi`
from `DecisionRule` and `ControlPolicy`; the notation collision is structurally
closed, but the intended semantic object is under-modelled and cannot be bound.

Unblocking both terms requires one carrier-family repair: represent sequential
and co-application relations distinctly, require the recorded meet/semilattice
law, and emit an identity-preserving fixture from the serialized cascade whose
same nodes and both relation families are checked in Lean. This pass does not
invent those missing serialized semantics or weaken the glossary to the
acyclic-edge carrier.

Coverage therefore remains 31/33. No Q pin or model-area refresh is required,
because no Lean declaration or binding changed.
