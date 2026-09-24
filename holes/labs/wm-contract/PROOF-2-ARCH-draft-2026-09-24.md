# PROOF-2 — adapting the theorem to the architecture discussed on 2026-09-24

Date: 2026-09-24. Author: claude-10. Status: DRAFT proposal. Nothing in
`PROOF-2-THEOREM-draft-2026-09-24.md` is edited; the amendments are registered
there as AR-32 to AR-36 with status `:proposed`.

Source of the features: `holes/E-cascade-real.md`, `holes/E-outer-loop.md`,
`holes/E-flight-aif.md`, and Joe's remarks in the same session (quoted where
used). Joe (2026-09-24): "The more interesting question is if we could adapt
PROOF-2 to take the architectural features we've talked about into
consideration."

## What PROOF-2 already covers

- **Machine construction, partly.** X₀(b) requires that candidates carrying
  `[:construction-receipt :kind] :hand-admitted` fail P₀, and P₀ requires
  `[:decision :selection-certificate :candidate-derivations]`. So the theorem
  already refuses to credit a hand-written candidate field. It does not say
  what a machine construction must be derived from.
- **Learning and priors.** Clause 5 (Dirichlet B read before every selection)
  and clause 6 (E, C, A, D, F, B certified per click).

## What it does not cover

| feature | where it came from | PROOF-2 today |
|---|---|---|
| F1. The machine chooses its target | E-outer-loop O1–O8 | Theorem starts from a candidate field Π for a target already chosen; target choice is outside every clause. |
| F2. Candidates are constructed in the tick from interpretations | E-cascade-real D3, D4, D11, D15; probe P1 | X₀(b) refuses hand-admitted; nothing names the inputs a machine construction must be reproducible from. |
| F3. A cascade is a semilattice | glossary ¶Policy π; E-cascade-real D18; Joe: "we shouldn't hard-code cascade is a list" | Clause 0 is stated over `CascadeTransition`, whose carrier is `List InterpretedPattern` ("A cascade policy is a precedence list of patterns", `CascadeTransition.lean:11`). `CascadeOrder.lean` has `acyclicDescent`, `IsMeet`, `hasMeets`, unconnected to the kernel. |
| F4. Higher-level patterns condition lower ones | E-cascade-real N1; E-flight-aif W1 (the cascade level) | Only E, a flat prior over policies. |
| F5. A pattern is more than IF→THEN | discussion of rewrite / loss / conditioned-unit readings | `InterpretedPattern` is the rewrite reading only (consumes, forbids, produces). |

## Proposed amendments

### AR-32 — Clause T: the target field and the choice among targets (F1)

**Wₜ.** On each click, the extracted target field `T` lists every target the
tick considered, and splits it into a feasible support `T_f` and exclusions,
each exclusion with a typed reason. The chosen target maximises the selection
posterior over `T_f` (with the same `selectionPosterior` as clause 3, at
target grain), feasibility acting as policy support and not as a value term.
When `T_f` is empty, the record carries, for each excluded target, what would
make it feasible (for a construction failure: the missing interpretation or
the unproduced want, as the constructor's typed findings give it).

**Pₜ.** To be built at `[:decision :target-field]`: considered targets,
`:feasible`, `:exclusions` with reasons, the per-target score, and the chosen
target. Cross-check the chosen target against `[:decision :selection-law]`.

**Xₜ.** (a) Drop one feasible target from `T` while leaving the choice
unchanged; Wₜ must fail. (b) An abstention record with a feasible target
present; Wₜ must fail. (c) An abstention whose exclusions carry no
"what would make it feasible"; Wₜ's empty-support branch must fail.

Note. This is a witness condition, not a gate: under the 2026-09-19 ruling
(nothing halts runs during tuning) a click that fails Wₜ still runs; it is
not credited.

### AR-33 — Clause 0: construction is reproducible from recorded interpretations (F2)

Add to **W₀**: every candidate's construction receipt is
`:machine-constructed`, names the interpretations it used (pattern id, guard,
produces, receipt with author and source sha), and replaying the constructor
on those interpretations and the recorded initial state reproduces the
candidate. Interpretations may be agent-authored; the author is a recorded
field, not a disqualifier (Joe: "I trust agents to make good interpretations
given the chance").

Add to **X₀**: (c) a candidate whose precedence cannot be reproduced from the
recorded interpretations; (d) a construction that refuses a target because one
want has no producer while admission would accept a candidate advancing the
others (E-cascade-real D15). The clause must fail on both.

### AR-34 — Clause 0 carrier: state it over the semilattice (F3)

Restate clause 0 (and the uses of `cascadeKernel` in clauses 4 and 5) over a
descent relation `r` on the candidate's patterns with `acyclicDescent r` and
`hasMeets r` (`CascadeOrder.lean`), plus the co-application edges. A list is
the special case of a chain.

**Missing definition, stated as one.** There is no definition yet of the
transition kernel of a semilattice cascade: when several patterns are enabled
at incomparable positions, which fires, or do they co-apply? `firstEnabled`
answers this only for a list. Until the kernel is defined in Lean, AR-34
cannot be witnessed, and clause 0 on a list should be recorded as the chain
case, not as the general one. Candidate sources for the definition: the
glossary's `BV.seq` / `BV.copar`, `futon3a/holes/labs/M-memes-arrows/cascade_construct.py`
(`chosen_semi_lattice`), and the flight cascades' `:differentiates` /
`:jointly-with` edges (E-flight-aif W1).

Add to **X₀**: (e) flatten the extracted semilattice to a topological order
and recompute. If kernel and score are unchanged on every click, the
semilattice did no work on the record; the certificate says so as a typed
finding.

### AR-35 — a prior over lower patterns conditioned by higher ones (F4)

A term, not yet a clause. The priming reading says a higher-level pattern
changes which lower-level patterns are likely to apply. In the model that is
a prior over the lower level's patterns (or over B) that depends on the
higher level. Whether it is E, a hierarchical prior on B, or something new is
open (E-flight-aif Q2). Needs a Lean definition before it can enter a clause.
Falsifier to carry forward: remove one priming edge; the recorded prior must
change.

### AR-36 — record which reading of a pattern the model uses (F5)

`InterpretedPattern` is the rewrite reading. State in the theorem's preamble
that clauses 0–6 certify the rewrite reading only, so a passing proof is not
read as certifying the forces (HOWEVER) or the conditioning. No new clause.

## Order

AR-36 and AR-33 can be adopted now (a sentence; an extension of existing
W₀/X₀ with carriers the constructor already produces). AR-32 needs one new
record field and is the clause that answers E-outer-loop. AR-34 and AR-35
each need a Lean definition first; per "Lean specifies, code conforms" the
definition comes before any code change to the cascade carrier.
