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

### Compatibility ruling: 象 / Minus40°DB (reviewed 2026-10-16)

Claude-16 reviewed this excursion against the live 象 occurrence records,
Minus40°DB, `CASCADE-SPEC-v3`, `ThreeHalvesBlend.lean`, and the current War
Machine constructors.  Verdict: **compatible with the amendments recorded
below, not compatible as originally written**.

The smallest shared definition is:

- a pinned pattern citation resolves either to an **occurrence** with exact
  reading provenance and its four 象 maps, represented by
  `:wm/three-halves-square-v1`, or to a typed void/unread result which is kept
  in the construction census but is not an occurrence;
- a **structural cascade** is occurrences plus their witnessed gluing diagram
  and construction order;
- an **executable cascade** is a refinement of that structural cascade which
  adds evidenced application-order edges, operational clauses, an executable
  frontier, open holes, and an execution journal;
- a checked projection from executable cascade back to the structural cascade
  and its construction census must preserve every occurrence, square, gluing,
  void/unread result, and construction-order fact.  The executable layer may
  add operational evidence; it may not invent missing 象 readings or
  reinterpret descriptive order as application order.

Minus40°DB is not itself a cascade carrier.  It contributes stable entity
identity useful for gluing and its valid-time/recorded-time model for the
journal.  These are compatible enrichments, not reasons to collapse the two
carriers into one.

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
- the 象 reading record id, exact fragment span, source-text digest, analysis
  digest, and the cited pattern's `source_sha256` for every occurrence;
- pinned reading, excerpt, analysis, pattern-library, graph and kernel
  identities;
- the exact scorer and normalization declarations under which its G was
  computed;
- the construction and selection receipts which establish that this is the
  object actually compared and chosen, not a reconstruction made afterwards.

### Cascade structure

- occurrence-level nodes, each binding a distinct citation occurrence—not
  merely a pattern id—to exact fragment/span provenance, pinned pattern bytes,
  and the four maps of its `:wm/three-halves-square-v1` reading, including
  situation sources and auxiliary flags;
- authored/organised additions, distinguished from originally selected nodes;
- four relations kept separate, each with evidence: descriptive reading order
  and its role labels; undirected gluing at a shared object with an isomorphism
  or consistency witness; operational application order with transition
  evidence; and guards;
- precedence/construction order as a property of the reading, not as a
  replacement for the graph and never by itself application order;
- alternatives as unchosen readings or other witnessed colimits, not presumed
  executable branches;
- an explicit account of the seed-graph edges used and any newly authored
  edge, with its authority.

The occurrence key must distinguish two citations of the same pattern.  In
particular, `:blend-squares` must be keyed by occurrence identity rather than
pattern id; otherwise repeated citations collapse and the carrier cannot
represent the live-turn data.

The square source is upstream of G.  A validated target reading identifies
citations and exact spans; a separate occurrence-square authoring registry
binds each citation's four situation maps and sources to that reading digest,
occurrence id and pattern pin.  The reading-family constructor consumes only
validated registry records before constructing the policies compared by G.
It is forbidden to derive maps from role labels, or to recover them from the
runner's later `:action` envelope after selection.  That envelope retains the
selected cascade policy; it is not the grain at which G is evaluated.

The production construction phase is `occurrence-square-authoring`.  It asks
the designated author only for the four situation elements and their evidence
sources.  Code constructs the theories and commuting maps, checks the pinned
pattern bytes and exact written parts, publishes the occurrence-keyed record,
and reruns inner construction under the retained outer choice.  A citation to
a pattern lacking any of IF/HOWEVER/BECAUSE/THEN is recorded as a typed void
and removed before policy construction; it is never padded with invented
content.  A failed authoring exchange is an execution error at this phase, not
an abstention or refusal by G.

### Operational meaning

- for each node or executable group: antecedent/guard, required state or
  evidence, intended operation, expected products/state changes, responsible
  capability, and completion check;
- dependency links showing which products enable which later nodes;
- the currently enabled frontier;
- evidence required before a step can be counted as enacted;
- policy holes and design holes, each with an owner/question, dependencies,
  and a re-entry condition.

Keep three absences distinct:

1. a **void citation**, where one of the four parts matches nothing and no
   occurrence exists;
2. an **unread citation**, where the four maps have not been authored and no
   occurrence may yet be claimed;
3. an **open operational hole**, where a valid structural occurrence exists
   but its enactment clause remains unresolved.

Only the third is a node in an executable cascade.  Neither of the first two
may be smuggled into a policy as a permissive open hole.

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
4. **Cascade law:** application-order edges are acyclic; gluing remains typed
   and undirected and is excluded from that cycle check; construction order
   names every required occurrence exactly once; co-application is retained
   as composition rather than flattened away.  A shared name without a gluing
   isomorphism/consistency witness is only a typed `shared-name-candidate`.
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
9. **象 projection:** forgetting executable additions produces the exact
   `ThreeHalvesBlend.Policy`-shaped structural diagram and construction census
   supplied by 象: the same occurrence identities, squares, gluings,
   void/unread results, and construction order.

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
cascade and respects its guards and application ordering.  Beside this
identity chain, a projection witness must show that the executable carrier
forgets back to the exact `ThreeHalvesBlend.Policy`-shaped structural diagram;
the 象 producer's output must lie in that projection's image.  Negative
witnesses must reject
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
- never infer application order from fragment order, reading order, scope
  nesting, construction order, or an unwitnessed shared name;
- enact a single node only when the cascade says it is sequentially enabled;
- enact a co-application group as a group, with a group completion condition,
  rather than serialising it by accident;
- dispatch through named capabilities with the shared guaranteed-handoff
  mechanism, never through an ambient or stale roster name;
- require evidence of the advertised effect before advancing the journal and
  habit count;
- derive guards and completion checks from the occurrence square when this is
  warranted (I1/I2 elements observed and CHECK on the B element); responsible
  capability and intended operation are attributed authored additions or open
  holes, never defaults;
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

1. Freeze a real reading-family selection fixture, including repeated pattern
   citations, exact spans, four-map squares, typed void/unread/open states,
   gluings, reading order, co-application, application-order evidence,
   receipts, pins and G terms.
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

1. Define node/group operational clauses, the structural-to-executable
   refinement, its forgetful projection, and the execution journal.
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
- two occurrences of the same pattern remain distinct through square lookup,
  scoring, persistence, validation and projection;
- void, unread and open are mutually exclusive and only open enters the
  executable node set;
- descriptive reading order cannot satisfy an application-order obligation;
- an undirected gluing does not create a false directed cycle, while an
  unwitnessed shared name cannot become a gluing;
- projection back to the 象 structural diagram is exact;
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
key squares by pattern id, treat an unread citation as an open node, infer
application order from reading order, promote a shared name to an unwitnessed
gluing, reconstruct after selection, drop an edge, accept a cycle, skip the
completion receipt, classify a compiler failure as refusal, rerun selection
during retry, and lose an open hole.  Each plant must be caught before
authorising a click.

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
