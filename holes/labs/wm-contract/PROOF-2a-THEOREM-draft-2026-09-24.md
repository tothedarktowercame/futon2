# PROOF-2a — the PROOF-2 theorem with the 2026-09-24 architectural adaptations

Date: 2026-09-24. Author: claude-10, at Joe's direction ("make a PROOF-2a that
includes our adaptations"). Status: DRAFT.

**Base.** `PROOF-2-THEOREM-draft-2026-09-24.md` at futon2 `cafe94b3`. Every
clause, key path, falsifier and contract entry there carries over unchanged
unless this file restates it. Where this file restates a clause, this file
governs PROOF-2a. The rationale for each change is in
`PROOF-2-ARCH-draft-2026-09-24.md` (register rows AR-32 to AR-36); the
findings behind them are in `holes/E-outer-loop.md`, `holes/E-cascade-real.md`
and `holes/E-flight-aif.md`.

## Scope statement (AR-36)

Clauses T and 0–6 certify the **rewrite reading** of a design pattern: an
`InterpretedPattern` with consumes, forbids and produces over a finite token
state. A passing PROOF-2a does not certify a pattern's forces (its HOWEVER)
or its conditioning of other patterns. Those readings have no clause yet
(AR-35 is the first step toward the conditioning one).

## Theorem

**THEOREM (PROOF-2a).** The implementation is not fake with respect to the
named War Machine Lean model iff, for every click required by clauses T and
0–6, the provenance proposition `P_k` identifies the values actually
consumed, the concrete Lean proposition `W_k` checks on those values, and the
independently constructed bad extract `X_k` makes the same proposition fail.
All eight clauses hold on the single linked sequence `L` of PROOF-2
(ordinary live clicks under A21); fixtures and hand-built inputs establish
none of them.

### Clause T — the machine chooses its target (new; AR-32)

**Wₜ.** On each click the extracted target field `T` lists every target the
tick considered and partitions it into a feasible support `T_f` and
exclusions, each exclusion with a typed reason. The chosen target maximises
`PolicySelection.selectionPosterior` over `T_f` at target grain, with
feasibility acting as policy support (membership in `T_f`) and not as a term
of G. If `T_f` is empty, each excluded target's record states what would make
it feasible; for a construction failure this is the constructor's typed
finding (the unproduced want, or the missing interpretation).

**Pₜ.** To be built at `[:decision :target-field]`: `:considered`,
`:feasible`, `:exclusions` (target, reason, what-would-make-feasible), the
per-target score inputs, and `:chosen`. `:chosen` must equal the target of
`[:decision :selection-law]`. The 09-23 and 09-24 records have no target
field; the target list assembled at `scripts/futon2/report/war_machine.clj`
(substrate targets, declared targets, proposal supply, ticket queue) is the
input the field must record.

**Xₜ.** (a) Remove one feasible target from the extract while keeping the
choice; Wₜ must fail. (b) An abstaining record with a non-empty `T_f`; Wₜ must
fail. (c) An abstaining record whose exclusions omit what-would-make-feasible;
the empty-support branch of Wₜ must fail.

Wₜ is a witness condition, not a gate. Under the 2026-09-19 ruling (nothing
halts runs during tuning) a click that fails it still runs; it is not
credited.

**Missing definition.** Target-grain scoring needs a G per target. The
natural candidate is the best candidate cascade's G for that target, which
makes Clause T depend on clause 0 being satisfiable for every feasible
target. This is stated here, not settled.

### Clause 0 — an ordinary, semantically nontrivial, machine-constructed field exists (restated; AR-33, AR-34)

**W₀.** As in PROOF-2, plus:
- (construction) every candidate's `:construction-receipt` has `:kind
  :machine-constructed` and names the interpretations it used: pattern id,
  guard, produces, and receipt (author, source path, source sha). Replaying
  the constructor on exactly those interpretations and the recorded initial
  state reproduces the candidate. Agent authorship of an interpretation is
  allowed and recorded.
- (carrier) the candidate's pattern structure is extracted as a descent
  relation `r` with `CascadeOrder.acyclicDescent r` and `CascadeOrder.hasMeets r`,
  plus its co-application edges. A precedence list is the chain case of `r`.

**P₀.** As in PROOF-2, plus `[:decision :selection-certificate
:candidate-derivations <id>]` carrying the interpretations used (with their
receipts), the constructor's version identity, and the descent/co-application
edges.

**X₀.** (a), (b) as in PROOF-2, plus:
- (c) a candidate whose structure cannot be reproduced by replaying the
  constructor on its recorded interpretations; W₀ must fail.
- (d) a target refused because one want has no producer, where admission
  would accept a candidate that newly satisfies another want; W₀'s
  construction condition must fail (E-cascade-real D15).
- (e) flatten each candidate's `r` to a topological order and recompute the
  kernel and score. If nothing changes on any click, the record carries a
  typed finding that the semilattice did no work on `L`. This is a finding,
  not a failure.

**Missing definition (blocks the general case).** `CascadeTransition.cascadeKernel`
is defined only for a list, through `firstEnabled`. There is no Lean
definition of the transition kernel of a semilattice cascade: when patterns
at incomparable positions are enabled at once, whether one fires (by what
rule) or they co-apply. Until it exists, W₀'s kernel equalities are checkable
only when `r` is a chain, and a passing clause 0 is recorded as the chain
case. Sources for the definition: glossary ¶Policy π (`BV.seq`, `BV.copar`),
`futon3a/holes/labs/M-memes-arrows/cascade_construct.py` (`chosen_semi_lattice`),
and the flight cascades' `:differentiates` / `:jointly-with` edges.

### Clauses 1–6

Unchanged from PROOF-2, with one reading rule: wherever they use
`cascadeKernel precedence`, read it as the kernel of the candidate's
structure `r`, which is the list kernel when `r` is a chain (AR-34).

## Not yet a clause (AR-35)

A prior over lower-level patterns conditioned by higher-level ones (the
priming reading). Needs a Lean definition first; open whether it is E, a
hierarchical prior on B, or new. Falsifier to carry forward: remove one
priming edge; the recorded prior must change.

## Work this opens, in order

1. **Lean: the semilattice kernel.** Define it (and prove row-stochasticity,
   and agreement with `cascadeKernel` on chains). Unblocks W₀'s general case.
2. **Record: `[:decision :target-field]`.** Unblocks Clause T.
3. **Record: interpretations and edges in `:candidate-derivations`.** With the
   constructor wired in (E-cascade-real D4), unblocks W₀'s construction
   condition.
4. **Constructor: partial-want construction** (D15), so X₀(d) has something
   to pass.
