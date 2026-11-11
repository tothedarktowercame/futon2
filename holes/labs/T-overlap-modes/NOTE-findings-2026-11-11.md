# T-overlap-modes-as-factor-precision — findings (discovery only)

kimi-2, 2026-11-11. Answers the ticket's five questions against the code at
futon2 `main` and the recorded cascade corpora. No code changed.

## Corpora counted

- **C1 — tick-run records**: `futon2/data/wm-runs/tick-run-record-*.edn`,
  636 files, dated 2026-10-20 through 2026-11-07 (each embeds the selected
  `:wm/executable-cascade-v1` record and its candidate arrangements).
- **C2 — machinery selections**:
  `futon2/data/wm-full-loop-machinery-*/*/attempt-*/002-selection.edn`,
  291 files (recorded-at stamps from 2026-10-02 onward).
- **C3 — recorded flights**: `futon2/data/wm-recorded-flights/*/ledger/*.edn`,
  12 ledgers (7 embed a `:wm/structural-cascade-v1`); the click5 flight family
  of 2026-10-19±.

## Q1 — What `:isomorphism` / `:consistency` witnesses assert, who produces them, how often

What they assert: `executable_cascade.clj:171` (`witness-conforming?`) demands
only a *shape*: `:kind` ∈ `#{:isomorphism :consistency}` (l.43), a 64-hex
`:digest`, and `:objects` = exactly the two endpoints. The kinds carry no
further semantics in the runtime — no field says what is isomorphic or what is
consistent, and no code path distinguishes the two kinds' downstream meaning.
The names gesture at the Lean carrier: `ThreeHalvesBlend.lean:813-836`
(`Gluing`, `Gluing.wellFormed`) requires a `PMap` that is a *full* isomorphism
of theories — inverse well-formed, total on both `elems`, and carrying **all**
axioms in both directions. That is the "full blend" shape (one shared object,
nothing dropped); there is no selective/partial witness kind on either side.

Who produces them: **nobody**. Repository-wide, the only occurrences of the
two kinds are the schema definitions themselves (`executable_cascade.clj:43`;
the `eig_shadow.clj:40` / `calibration_cycle.clj:22` "consistency" hits are
unrelated EIG fields). No analysis, reading, or selection code ever attaches a
`:witness` to an `:overlap` edge. Overlap edges are produced with
`:from-fragment :to-fragment :from-roles :to-roles` (shared role-name
evidence; e.g. C2 wm-full-loop-machinery-125: overlaps on roles
`["condition" "contrast" "context"]`), never a witness, so
`classify-edges` (l.184, `:overlap` case l.198) demotes **every** overlap to
`:shared-name-candidate` with `:offered-witness nil`.

Counts: C1 — 2921 `:kind :overlap` edges, 0 witnesses of either kind, 1037
`:gluing []` (empty), **0 non-empty gluings**, 14 non-empty
`:shared-name-candidates` entries (all `:offered-witness nil`). C2 — 2346
overlap edges, 0 witnesses of either kind. C3 — all 7 embedded structural
cascades have `:gluing []` and `:shared-name-candidates []`.

So `:isomorphism`/`:consistency` correspond to nothing recorded. They do not
split full vs selective blends; they are an unexercised schema.

## Q2 — Is any recorded overlap a containment, and how represented?

No. There is no containment mode anywhere: 0 `:containment` occurrences and 0
`:scope-nesting` occurrences across C1–C3. `:scope-nesting` exists only as a
name in the compiler's `descriptive-evidence-kinds`
(`cascade_enactment_compiler.clj:31`) — an evidence kind *refused* for
application-order edges, not a represented relation. The reading-order
relations (`:context-enables`, `:dependency-enables`, …
`executable_cascade.clj:44-47`) are explicitly descriptive and non-operational
and carry no empirical-prior semantics. Absent, not contradicted.

## Q3 — Do the squares carry per-modality precision or shared-factor identity?

No. `three_halves_square.clj`: a square is four pinned objects
(`:theory {:elems :names :axioms}` per role, l.101-113 validation), four
partial maps, and two `:auxiliary` booleans (l.75-76, schema l.63-71). Nothing
per-modality; the word `precision` does not occur (the 1426 `:precision` hits
in C1 are the tick machinery's `:precision-model`, unrelated to overlap). The
`:auxiliary` flags are the nearest thing to "selective": an auxiliary triangle
is exempt from the commutation requirement (`validate`, l.147-153; Lean
`Receipt.requiredCommutes`, `ThreeHalvesRuntimeCertificate.lean:44-46`). But
they are per-square, per-triangle exemptions from an equality check — not a
likelihood precision, not attached to a gluing, and not indexed by modality.
Shared-factor identity is likewise absent: gluing is between whole typed-role
*objects* of two squares (Lean `Gluing` names `role_i`/`role_j`,
`ThreeHalvesBlend.lean:813-818`), with no field naming a factor. Absent.

## Q4 — Does the 3/2 treatment state an interchange law, and does its failure separate containment from blending?

The 2026-10-11 checkpoint (the SUT gate, `holes/labs/wm-contract/
SUT-GATE-2026-10-11.md`, item 5) requires: source-pinned square IDs,
typed-role gluing endpoints, explicit isomorphism graphs, and
inverse/coverage/axiom-carriage checks — i.e. gluing and coherence, no
interchange law. The Lean development matches: consistency is "the join of
the composites is well-formed" (`ThreeHalvesBlend.lean`, `Cone.consistent`);
the 3/2-pushout is decided as joint coverage (`pushout_iff_coverage`, l.427 —
per the 2026-10-06 discovery note the universal property *is* coverage on
this carrier); pasting is Proposition 8 (`prop8`, l.711); coherence across
gluings is the explicitly supplied `GluingTriangleCertificate` (l.845-862:
`comp f.iso g.iso = d.iso`), with l.868-869 recording that cycle-basis
completeness is still open. The string "interchange" appears nowhere in the
ThreeHalves/Cascade modules. And since containment does not exist as a
represented mode (Q2), no law — stated or failed — is doing any
containment/blend separation. Absent.

## Q5 — Contradicted vs absent

**Contradicted:** only one piece, mildly. The ticket's reading of a
shared-name candidate as "a low-precision hypothesis for structure learning
(BMR) to confirm or reject" is not what the code's candidate is: it is a
*gate outcome* — an overlap whose witness failed conformity
(`executable_cascade.clj:198-204`), retained with the offending witness
attached. There is no confirm/reject path back to `:gluing` (the validator
`validate-cascade` even *refuses* a candidate whose offered witness conforms,
l.~490 `bad-shared`), and no precision value. The mechanism rhymes with the
reading; the semantics are refusal-quarantine, not Bayesian model reduction.

**Absent (not contradicted):** the full/selective blend distinction (no
witnesses at all, Q1); containment / outer-state-as-empirical-prior (Q2);
per-modality precision and shared-factor identity on the squares (Q3); the
interchange law (Q4).

## Verdict

**Does not match.** The active-inference reading is almost entirely absent
from the code rather than contradicted by it: the overlap machinery records
*one* undifferentiated relation (shared role names, demoted to quarantined
candidates), where the reading wants four modes with precision structure.

## What a faithful representation would need (PROPOSAL for Joe's review — no code change made)

Per overlap/gluing edge in the structural layer
(`:wm/structural-cascade-v1`, alongside the existing `:witness`):

1. `:mode` ∈ `:full-blend | :selective-blend | :containment |
   :shared-name-candidate` — declared, never defaulted.
2. For blends: `:shared-factor` naming the identified hidden-state factor
   (e.g. the digest of the glued object pair, or an explicit factor id), and
   `:precision` as a per-modality map `{modality → precision}` — full blend
   is the all-modalities-high special case, selective the general case; this
   subsumes and generalises the square's `:auxiliary` flags, which would move
   from per-triangle booleans to per-modality likelihood precisions.
3. For containment: a directed `:outer → :inner` pair plus the prior-binding
   (which outer state component supplies the empirical prior for which inner
   prior), replacing the current undirected-only edge shape.
4. For candidates: a precision scalar (or prior odds) and an explicit
   confirm/reject hook — the BMR (R17) receipt id that promoted the candidate
   to `:gluing` or retired it — turning the quarantine list into a structure-
   learning frontier.
5. On the Lean side: a stated interchange (or its failure mode) separating
   containment from blending, and completion of the gluing-cycle coherence
   basis already flagged open at `ThreeHalvesBlend.lean:868-869`.

Witness kinds `:isomorphism` (full) and `:consistency` (selective) *could*
carry modes 1–2, but as Q1 shows they are currently an unexercised schema
with no producer; the proposal needs a producer before it needs new fields.
