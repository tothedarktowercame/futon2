# E-implementing-G-over-cascades — from selected composition to an enacted cascade

**Date:** 2026-10-16  
**Status:** OPEN — implementation plan drafted; no claim of implementation  
**Parent:** [[M-G-over-cascades]]  
**Reason for opening:** A live click selected and scored a structurally arranged
policy, then stopped at `:reading-policy-execution-semantics-owed`.  The stop
prevented an old vector-based enactment path from misreading the new value, but
it also exposed an unfinished join: the selected arrangement was never made
into the durable, validated, executable cascade already required by the
mission and by `DarkTower/WarMachine/GOverCascades.lean`.

## 1. Scope and non-negotiable meaning

This excursion does **not** define a cascade as an ordered bundle of pattern
names.  The definition already in effect is:

- `DarkTower.WarMachine.Holes.Cascade`: nodes, authored additions, directed
  edges, an acyclicity witness, and precedence;
- `M-G-over-cascades`: an admissible, governed composition over the real seed
  graph, whose typed connections and interaction terms affect `G(π)`;
- `DarkTower.WarMachine.GOverCascades`: the policy grain scored by G is the
  cascade itself; a composition-blind bag of patterns is non-conformant;
- the War Machine design: the selected cascade guides work, its outcomes are
  witnessed, and those outcomes return as evidence for later selection and
  learning.

The current reading-family value contains useful structural material
(`nodes`, typed `edges`, co-application/descent and `precedence`).  It is an
input to construction, not a second definition of cascade and not by itself an
enactment instruction.

## 2. Required result for the next live click

For the selected target, one click must perform this uninterrupted path:

```text
construct candidate cascades
        ↓
score G over those exact compositions
        ↓
select one cascade identity
        ↓
materialise and save that exact cascade
        ↓
validate it against the ratified cascade definition
        ↓
compile its currently executable frontier into an enactment guide
        ↓
enact and record a witnessed step, or hold the same phase for live repair
```

The click must not translate a construction, validation, or compilation defect
into `no-selection`, abstention, or an ordinary policy refusal.  Those outcomes
would falsely say that the decision law declined to act.  Once a cascade has
been selected, failure to materialise, validate, or follow it is a machine
error.  The debugger must retain the selected target, selected cascade
identity, inputs, and phase; after repair it retries that same phase without
rerunning outer selection or spending another click.

Success for this excursion means that the next click can produce a cascade
which is:

1. **saved** as a durable, content-addressed record;
2. **validated** against the agreed cascade definition and the exact object
   scored and selected;
3. **followed** as the guide to enactment in the War Machine;
4. **open where necessary**, with unresolved design decisions represented as
   explicit holes in the cascade rather than concealed defaults or terminal
   refusals.

## 3. One canonical saved artifact

Introduce one versioned record, provisionally
`:wm/executable-cascade-v1`.  Do not add separate selection, validation, and
flight identities which can drift.  Its content digest is the cascade identity
used throughout scoring, selection, persistence, validation, enactment, the
run record, and later habit/learning evidence.

The record must contain at least:

### Provenance and binding

- schema and content digest;
- run, click, selected target, selected policy and construction occurrence;
- pinned reading, excerpt, analysis, pattern-library, graph and kernel
  identities;
- the exact scorer and normalization declarations under which its G was
  computed;
- the construction and selection receipts which establish that this is the
  object actually compared and chosen, not a reconstruction made afterwards.

### Cascade structure

- occurrence-level nodes, each binding a pattern id to pinned pattern bytes;
- authored/organised additions, distinguished from originally selected nodes;
- typed directed edges, including descent, co-application/overlap, guards and
  any other admitted edge type;
- precedence as a property of the composition, not as a replacement for the
  graph;
- alternatives or branches when the selected composition contains them;
- an explicit account of the seed-graph edges used and any newly authored
  edge, with its authority.

### Operational meaning

- for each node or executable group: antecedent/guard, required state or
  evidence, intended operation, expected products/state changes, responsible
  capability, and completion check;
- dependency links showing which products enable which later nodes;
- the currently enabled frontier;
- evidence required before a step can be counted as enacted;
- policy holes and design holes, each with an owner/question, dependencies,
  and a re-entry condition.

### State without mutation of identity

The immutable cascade definition and its evolving execution journal must be
separate.  The cascade record is append-never/rewrite-never.  A journal refers
to its digest and records frontier changes, attempts, evidence, verified
effects, newly opened holes, and closure.  Learning reads witnessed journal
entries; it must never infer enactment merely because a cascade was selected.

## 4. One validator shared by every boundary

Build one production validator for `:wm/executable-cascade-v1`.  The runner,
debugger, exporter, report card, and fixture generators must call or consume
the result of this validator; none may independently recreate part of its
rules.

It must establish:

1. **Identity:** the digest is intact and binds the selected target, selected
   policy, pins, structure, and scoring declaration.
2. **Population:** every referenced pattern and occurrence exists; there are
   no omitted, duplicate, or foreign nodes.
3. **Graph well-formedness:** every edge endpoint is a node; every edge kind is
   admitted and has its required evidence; authored additions are attributed.
4. **Cascade law:** the directed descent relation is acyclic; precedence names
   every required occurrence exactly once and respects directed constraints;
   co-application is retained as composition rather than flattened away.
5. **Admissibility:** guards, consumes/produces declarations, conflicts, and
   enabled-frontier claims agree with the pinned interpretations and current
   state.
6. **G binding:** the nodes, edges, precedence, alternatives, terms,
   normalization, and identity are exactly those supplied to G and to the
   one selector.  Rebuilding a different cascade after selection is invalid.
7. **Operational coverage:** every closed node has a usable enactment clause
   and completion check.  Every node which does not has an explicit open hole;
   absence must never be converted to an invented default.
8. **Journal binding:** every attempt and effect names this cascade digest, an
   admitted enabled node/group, and the evidence that verifies the transition.

The validator returns either a sealed certificate or an error report containing
the exact failed clause and repair inputs.  It does not return an abstention or
policy refusal: it is checking whether the machine faithfully represented and
can operate the policy it already selected.

## 5. Formal boundary

The existing Lean work gives necessary pieces but not the complete production
claim:

- `Holes.Cascade` fixes the structural carrier;
- `GOverCascades` fixes the grain and scoring interface, while explicitly
  leaving `CascadeGrainSeam.owed` for the production seam;
- Q2–Q10 certify censuses and properties of scoring/selection records, not the
  materialisation and enactment of the selected cascade.

The implementation therefore needs a new witness at the seam, not a claim
that the old Q certificates already close it.  The smallest useful formal
statement should bind:

```text
selected identity
  = identity of the Cascade-typed object supplied to G
  = identity of the validated saved cascade
  = identity named by the enactment journal
```

It must also show that the executable frontier is drawn from the validated
cascade and respects its guards and ordering.  Negative witnesses must reject
at least: changed edges after scoring, flattened co-application, foreign or
missing nodes, cyclic descent, a precedence that violates an edge, an
unattributed authored edge, an unbound journal entry, and a claimed enactment
without completion evidence.

Retire `CascadeGrainSeam.owed` only after the production runner emits this
witness from a real selected cascade and the serving-path record verifies it.

## 6. Compilation to an enactment guide

The existing `:cascade-candidate` construction route is evidence that the War
Machine already has a selected-cascade enactment boundary.  It must not simply
be fed the reading policy's `:precedence :co-apply` map as though that were its
old vector.

Add one explicit compiler:

```text
validated executable cascade + current observed state
    → executable frontier + step/group instructions + open holes
```

Rules:

- preserve graph identity and receipts; compilation may not silently choose a
  different order or discard edges;
- enact a single node only when the cascade says it is sequentially enabled;
- enact a co-application group as a group, with a group completion condition,
  rather than serialising it by accident;
- dispatch through named capabilities with the shared guaranteed-handoff
  mechanism, never through an ambient or stale roster name;
- require evidence of the advertised effect before advancing the journal and
  habit count;
- when a design hole is reached, save it in place and invoke the live repair
  loop against the same cascade and phase.  A repair may append an authorised
  design decision or revised cascade version; it may not mutate the selected
  record in place.

“Open” therefore means the cascade is a durable partial design with a known
frontier and explicit holes.  It does not mean that arbitrary missing fields
are tolerated, nor that the machine pretends an unresolved node was enacted.

## 7. Error and live-repair semantics

After selection, use these outcome classes:

- `:cascade-materialisation-error`
- `:cascade-validation-error`
- `:cascade-compilation-error`
- `:cascade-enactment-error`
- `:cascade-evidence-error`

Each error record carries the cascade identity, retained phase, failed clause,
relevant immutable inputs, debugger artifact, and retry entrypoint.  The click
repair loop must treat these as resumable errors.  It may dispatch repair and
review, hot-load a verified fix, and retry the held phase.  It must not:

- rerun outer selection;
- select a runner-up cascade;
- relabel the error as `no-selection`, abstention, or ordinary refusal;
- consume another click for the retry;
- discard the selected cascade or its first failed attempt.

A genuine domain-level refusal remains possible *inside* an enacted cascade
when that cascade explicitly specifies a guarded operation and the observed
world falsifies its guard.  That is evidence about the policy/world, not a
substitute for an implementation error at this seam.

## 8. Implementation order

### Packet A — canonical record and validator

1. Freeze a real reading-family selection fixture, including nodes, edges,
   co-application, precedence, receipts, pins and G terms.
2. Define the versioned artifact and content identity.
3. Implement the single validator and its negative controls.
4. Persist the artifact independently of whether enactment later succeeds.

**Exit:** the fixture is saved, reloadable, and either certified or rejected
for the same reason by every consumer.

### Packet B — selection/scoring identity join

1. Make construction emit canonical cascade candidates before G.
2. Score those identities directly.
3. Make selection return the chosen canonical identity.
4. Prove that persistence does not reconstruct or alter the selected object.

**Exit:** edge/order/co-application mutations after scoring are caught, and
the saved digest is the selected digest.

### Packet C — enactment compiler and open holes

1. Define node/group operational clauses and the execution journal.
2. Compile the enabled frontier without flattening composition.
3. Represent unresolved design choices as explicit open holes.
4. Connect compiled steps to the existing selected-cascade/flight boundary.

**Exit:** a real selected cascade produces a concrete first action or a named
open design hole; neither path produces the old execution-semantics refusal.

### Packet D — held-phase live repair

1. Teach the debugger and click loop the five seam error classes.
2. Persist every failed attempt and repair receipt.
3. Retry the retained phase after hot-load, with no reselection and no new
   click expenditure.

**Exit:** induced failures at every seam repair and resume in one click; tests
fail if any becomes abstention/refusal or if selection runs twice.

### Packet E — Lean witness, exporter and live proof

1. Add the identity/frontier witness and negative modes.
2. Export it only from the verified saved record and journal.
3. Run focused runtime and Lean gates.
4. Run one authorised live click with the debugger attached.

**Exit:** the run record contains the saved cascade, validation certificate,
compiled frontier, enactment attempt and evidence under one identity.  The
formal checker agrees.  Only then remove
`:reading-policy-execution-semantics-owed` and retire
`CascadeGrainSeam.owed`.

## 9. Required tests before the next click

At minimum:

- a production-shaped cascade with sequential and co-application structure;
- save/reload byte and identity stability;
- all validator negative cases named in §5;
- the exact selected object, not a reconstruction, reaches persistence;
- one selector call across success, open-hole, error and repaired-retry paths;
- one outer target choice across those paths;
- a co-application group is not flattened into accidental serial order;
- an open design hole survives save/reload and can be resolved by a new,
  attributed version;
- verified effect advances the journal and habit evidence; an unverified
  attempt does neither;
- every post-selection implementation defect is recorded as a resumable error,
  never as refusal, abstention or no-selection;
- serving-JVM integration: hot-loaded production composition, persistence,
  validator, debugger retry and report export are exercised together;
- the Lean positive witness and every negative witness elaborate.

Plant the old behaviour as controls: vector-coerce the co-application map,
reconstruct after selection, drop an edge, accept a cycle, skip the completion
receipt, classify a compiler failure as refusal, rerun selection during retry,
and lose an open hole.  Each plant must be caught before authorising a click.

## 10. Definition of done

This excursion is not complete because a guard has a clearer message or
because a fixture can be scored.  It is complete when one serving-process
click demonstrates all of the following on a real target:

- more than one structurally distinct cascade was concretely constructed when
  the target affords it;
- G was computed over those exact compositions and one was selected once;
- the selected cascade was saved under the same identity;
- the shared validator certified it against the ratified definition;
- its executable frontier guided a real, evidence-checked enactment step;
- any remaining design choice was retained as an explicit open hole;
- the run record, report card and Lean witness agree about the same cascade;
- no seam defect was reported as refusal, abstention or no-selection;
- a planted live seam defect was repaired by retrying the held phase rather
  than by spending another click.

Until then, further ordinary clicks should not be used to rediscover this
known boundary.
