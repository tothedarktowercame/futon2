# Mission: War Machine Wiring (last-mile bridge to M-live-geometric-stack)

**Date:** 2026-04-30
**Status:** OPEN — VERIFY (2026-05-01) — Checkpoint 1 lands stepper trial against M-reachable-from-boot trajectory; vocabulary passes structural-distinction test; substrate-2 commit-vertex coverage gap surfaced as V-1 pre-condition; D-7.5 vocabulary refinements identified (catalog-coverage, generalization-vs-addition, implicit-to-typed lift, epistemic-humility primitive)
**Owner:** Joe + Claude
**Repos:** futon5a (mission home), futon3c (head), futon3 (manifold + pair-mission), futon2 (AIF inspiration), futon5 (wiring-diagram pattern)

**Parent endpoints (the two sides of the bridge):**

- `futon3c/holes/missions/M-war-machine.md` — the head: move-selection
  policy, bidding, Tick/Click dispatch, recurring AIF tick
- `futon3c/holes/missions/M-war-machine-tuning.md` — the head's policy
  tuning surface
- `futon3/holes/missions/M-live-geometric-stack.md` — the manifold:
  live (T, ∇, Δ, drift) over the futon-stack substrate
- `futon3/holes/missions/M-pattern-application-diagnostic.md` —
  substrate-2's pair-mission, boundary-growth interface

**Cross-refs (MAP research targets — AIF & wiring-diagram inspiration):**

- `futon2/doc/aif-engine.md` — AIF mechanics
- `futon2/src/ants/clj_ants_aif.clj` (+ tests under `futon2/test/ants/`)
  — AIF ants implementation, the autonomous-foraging reference
- `futon2/holes/M-aif-head.md`, `M-aif4iad.md` — head-specific AIF
  framings
- `futon5/docs/chapter0-aif-as-wiring-diagram.md` — the canonical
  "AIF as wiring diagram" exposition; the visual idiom an autonomous
  WM should emit per mission
- `futon5/data/missions/futon2-aif-ants.edn` — existing capture of
  AIF-ants mission data
- `futon5a/holes/missions/M-stack-stereolithography.md` § Checkpoint 6
  — piano roll as the head's post-hoc trace projected onto
  (substrate × time)
- `futon5a/scripts/piano_roll.py` + `/tmp/piano-roll-v0.{pdf,png}` —
  prototype trace renderer

## 1. IDENTIFY

### The last mile

Until 2026-04 the WM head and the futon-stack manifold were two
implicit halves of the same story. The head was developed in
`M-war-machine` and tuned in `M-war-machine-tuning`, but its
move-selection policy could only see local context (mission queue,
blackboard state, recent operator turns). The manifold side — what's
nearby on the substrate, where the gradient points, where work is
*wanted* — was folklore.

`M-live-geometric-stack` (substrate-2) changed that. With T (tangent
/ connectivity), ∇ (gradient), Δ (laplacian / concentration), and
drift defined live over the substrate, the manifold side is real
infrastructure for the first time. **Both endpoints now exist; the
connector doesn't.** A TM has its tape always-already coupled to its
head; a TM-on-manifold doesn't get that coupling for free. This is
the last mile.

### The piano roll as evidence the framing is real

The piano roll built in M-stack-stereolithography Checkpoint 6 is
literally the head's trajectory on the manifold, projected onto
(substrate × time). Cluster fans are the head dwelling; silences are
the head elsewhere; cross-futon parallel rhythms are parallel head
trajectories. That this reading is *faithful*, not metaphorical, is
the strongest evidence that the TM-on-manifold framing is load-
bearing — and therefore that the wiring step is concrete work, not
philosophy.

### Three contracts the wiring needs

The mission's deliverables — drafted in MAP, sketched in DERIVE,
argued in ARGUE, verified in VERIFY, sized in INSTANTIATE:

1. **Manifold-read contract.** At the head's current (futon × layer
   × family × time) position, what does it query from substrate-2?
   Pressure, mission portfolio in this neighborhood, T, ∇, Δ, drift
   — but expressed as a function signature against substrate-2's
   actual exports.
2. **Head-write contract.** What does the head emit back when it
   acts? Tick / Click / commit / mission-stamp / closure — typed
   enough that the manifold updates its field in response. The act
   of writing should *move* the field, not merely log a fact.
3. **Trace-emit contract.** Turn the post-hoc piano-roll renderer
   into a forward-going live emitter. Each WM cycle becomes one row
   appended to a forward-extending piano roll; the bottom edge is
   the head's *current* position. This is how future WMs "print
   piano rolls" as natural runtime output rather than as audit.

### Open conceptual question

Is it one head (sequential, clean theory, single-trajectory piano
roll) or N heads (matches operator reality, weaker universality,
multi-track piano roll)? Worth answering early in MAP — the choice
shapes all three contracts.

### Speculative destination (motivation, not target)

A WM that forages autonomously on the manifold: reads ∇ to find
steepest pressure, drift to spot unstable regions, T to see what's
within move-distance; produces a `futon5/docs/chapter0`-style wiring
diagram per mission as a side-effect of action; closes invariants
opportunistically per the Krowne tetrahedron's evidence apex; emits
its trajectory as live piano roll; functions as a jolly work
companion that doesn't block on operator prompts. The futon2 AIF
ants are the closest existing analog.

This destination *motivates* the mission. We will not reach it
without first naming the gaps. **There may be many.**

### Scope in

- The three contracts (manifold-read, head-write, trace-emit),
  developed to function-signature level against substrate-2's
  actual exports
- An enumeration of gaps where the endpoints don't yet match the
  contracts — separated into "head doesn't expose this," "manifold
  doesn't expose this," "neither side exposes this"
- A small live-emitter prototype if it falls out cheaply — turn
  `piano_roll.py` into a forward-going emitter
- Mapping the gap-list to AIF-ant inspiration (where futon2's
  patterns transfer cleanly) and to futon5's wiring-diagram idiom
  (where it might or might not apply per-mission)

### Scope out

- Actually wiring the head's policy to consume substrate-2's field
  in production — that's an INSTANTIATE-scale follow-up, not this
  mission's body
- Implementing autonomous foraging
- The per-mission wiring-diagram emitter (chapter-0 pattern as
  per-mission runtime output) — its own follow-up if MAP shows
  the substrate is ready
- Multi-head theory beyond the one-vs-N decision

## 2. MAP (largely research-based)

MAP is research, not implementation. For each contract, produce a
matching sketch between what the endpoints currently expose and
what the contract needs, plus a gap-list. The output of MAP is a
**gaps document**; DERIVE then decides which gaps to close first.

### Primary endpoints (read in full)

1. **`futon3/holes/missions/M-live-geometric-stack.md`** — what does
   substrate-2 actually export today vs. what's still aspirational?
   For each of T / ∇ / Δ / drift, identify: data shape, query
   interface, batch vs. streaming, position-addressing scheme.
2. **`futon3c/holes/missions/M-war-machine.md`** — current
   move-selection inputs. Where would manifold-read quantities
   slot in?
3. **`futon3c/holes/missions/M-war-machine-tuning.md`** — what new
   signal types could the policy absorb if substrate-2 exposed them?
4. **`futon3/holes/missions/M-pattern-application-diagnostic.md`** —
   substrate-2's pair-mission; boundary-growth interface may already
   prefigure parts of the head-write contract.

### AIF & wiring-diagram inspiration (canvass for transferable patterns)

5. **`futon2/doc/aif-engine.md`** — AIF mechanics. What's
   implemented vs. described?
6. **`futon2/src/ants/clj_ants_aif.clj`** + tests — autonomous
   foraging policy. What does an ant read from its environment?
   What does it write? How does it decide where to move next?
   Critical for the manifold-read contract sketch.
7. **`futon2/holes/M-aif-head.md`** + **`M-aif4iad.md`** —
   head-specific AIF framings; may already address parts of the
   manifold-read contract under different names.
8. **`futon5/docs/chapter0-aif-as-wiring-diagram.md`** — the
   canonical AIF-as-wiring-diagram exposition. This is the visual
   idiom we'd want autonomous WMs to produce per mission. Map the
   pattern's preconditions to current mission shape.
9. **`futon5/data/missions/futon2-aif-ants.edn`** — already-captured
   AIF-ants mission data. Inventory what's known so we don't
   re-derive.

### Possible gaps (priors, to verify in MAP)

- Substrate-2 might not yet export ∇ in head-readable form —
  gradient direction at a discrete-manifold position is non-trivial
  to define and may need a coarsening
- The head might not be position-aware — does it know where on the
  manifold it currently *is*, or only what mission it's executing?
- The futon stack might not be a smooth-enough manifold for ∇/Δ to
  be locally well-defined; coarsening to layer × family × time
  might be necessary
- The futon2 AIF ants may use a substrate model that doesn't
  transfer (e.g. continuous arena vs. discrete-mission manifold)
- Futon5's chapter-0 wiring-diagram pattern may presuppose
  declarative mission specs; current missions may not be
  declarative enough to auto-emit the diagram

### MAP success criterion

For each of the three contracts, produce one of:

- (a) "this works today, here's the function signature"
- (b) "this needs X from substrate-2 — file a follow-up to that
  mission"
- (c) "this needs Z from the head — file a follow-up to
  M-war-machine"
- (d) "this is genuinely unsolved at the conceptual level — note
  in DERIVE for design work"

That four-way classification across three contracts is the gaps
document. DERIVE then prioritises.

### Checkpoint 1 - 2026-04-30: MAP research findings + gaps document

Methodology: four parallel research agents read the primary endpoints
and inspiration sources in full, returning structured findings against
the three-contract scaffold. This checkpoint synthesizes their reports.

#### Cross-cutting discoveries

1. **WM-I4 / Hobbes invariant is a wall.** `M-war-machine.md` (line
   827 in the cyberant comparison table) explicitly commits to:
   *"WM does not act. The Hobbes criterion (WM-I4): sovereignty
   preserved, operator decides."* The current WM is `:π-display`
   only — a reading surface that aggregates 16 channels into a single
   stack-scale observation, ranks actions via EFE, and surfaces a
   recommendation. It does not emit move-the-field events. **The
   head-write contract therefore lands directly on this invariant.**
   The mission's working framing (head emits writes that update the
   manifold) is incompatible with the existing WM as deployed.

2. **Substrate-2 is more operational than the IDENTIFY draft assumed.**
   All four fields (T, ∇T, ΔT, drift) are operational today as
   XTDB-backed query functions over `(vertex-id, commit-id)` pairs;
   the multi-watcher streams updates with ~5s latency; the HTTP route
   is `GET /api/alpha/hyperedges?type=...&repo=...&source-file=...`
   served by futon1a at `localhost:7071`. The discipline note in
   M-live-geometric-stack (lines 386-390) explicitly says *"War
   Machine is a consumer, not the owner of geometry"* — anticipating
   this mission.

3. **Single-head architecture confirmed.** The "open conceptual
   question" in IDENTIFY (one head vs N heads) has a concrete prior
   answer from the existing docs: **one head**. The 16-channel
   portfolio observation flattens N missions into a single stack-scale
   summary; that flattening *is* the singularisation. Multi-head is
   open as a future redesign, not as ambiguity in the current system.

4. **Two competing AIF observation surfaces exist.** Path A of
   M-war-machine-tuning forked the system into a 16-channel **portfolio**
   surface (`futon3c.portfolio.observe/observe`, consumed by the
   running cljs UI) and a 12-channel **strategic** surface
   (`futon0/scripts/futon0/report/war_machine.clj` `observe`). The
   wiring contract must pick which surface the manifold-read extends,
   or expose both. This is a decision point, not a gap.

#### Manifold-read contract — classification per component

| Component | Status | Detail |
|---|---|---|
| Position addressing | (a) | `(repo, vertex-id, commit-id)` exists today via futon1a XTDB |
| T at position | (a) | Phase 2 query function operational |
| ∇T at position | (a) | `∇T(e,c) = T(target(e),c) − T(source(e),c)` operational |
| ΔT at position | (a) | sum-over-incident-edges, operational |
| drift at position | (a) | Jaccard over vocabulary fingerprints, operational |
| **Head-facing `argmax-∇T` over neighbours** | **(b)** | components exist; convenience function does not |
| **Real-time push channel** | **(b)** | watcher writes evidence; no subscription channel exposed for an external head |
| **Head's *current position*** | **(c)** | head is not position-aware (see below) |

**The dominant gap is on the head side, not the manifold side.** The
head's current `observe()` returns a 16-channel scalar aggregate over
the *whole stack* with no position parameter. To consume substrate-2's
field, the observation function would need to be re-parameterised by
position: `observe(position) → channels`. Hook point:
`futon3c.portfolio.observe/observe` and `result-summary` in
`futon3c/src/futon3c/portfolio_inference/scheduler.clj`. Plausible
minimal extension: add a `:position` field to each observation
entry; expose `:gradient`, `:laplacian`, `:drift` keys alongside the
existing 16 channels.

**Conceptual gap (c): how does the head *choose* its position?** The
current head identifies "where it is" only by reference to the
currently-active mission — a one-step-removed proxy. A position-aware
head needs either (i) explicit selection-of-where-to-look, or (ii) a
foraging policy that takes ∇T as an input and outputs the next
manifold step (the futon2 ant pattern).

#### Head-write contract — classification per component

The headline finding: **this contract lands directly on WM-I4 / Hobbes.**

| Component | Status | Detail |
|---|---|---|
| Writing structural edges to futon1a (general write API) | (a) | POST to futon1a routes; caveat: upsert REPLACES props, must fetch-merge-upsert (per memory `futon1a-upsert-replaces-props`) |
| Writing `:pattern-application` edges (taxonomy slot) | (a) | endpoint addressable; status `:new` in substrate-2 edge taxonomy |
| **WM emitting any move-the-field event** | **(c)** | blocked by WM-I4; structural change required to unblock |
| **Validating that a write moved the boundary** (closure check) | **(c)** | line-integral evaluator unsolved; named in pair-mission DERIVE feeder #5 |
| Schema for "head-emitted writes" distinct from watcher-emitted writes | **(c)** | not in either doc |

**Three options for resolving the WM-I4 wall (DERIVE-scope decision):**

1. **Reopen WM-I4.** The cleanest framing for autonomous foraging,
   but a non-trivial invariant decision. Joe's prior memory entry
   `project_war_machine` notes "ready for INSTANTIATE, dynamic
   strategic visualiser" — the WM as deployed is a viewer, and
   reopening I4 changes its kind.
2. **Reframe: head-writes are operator-actions mediated by WM
   recommendations.** The WM remains a `:π-display`; what we call
   the "head" in the TM-on-manifold framing is the operator+WM
   composite. Head-writes happen when the operator acts on a WM
   recommendation; the augmentation ledger
   (`M-war-machine-tuning-augmentation.edn`) and the
   `derive-augmentation-from-evidence` checker (M-war-machine.md
   Checkpoint 6) are *already* the de facto detection mechanism.
   This option is closest to the existing system; it weakens
   "autonomous AIF ant" but preserves the Hobbes criterion.
3. **Fork: the portfolio-inference scheduler IS the head, separate
   from WM-as-display.** The scheduler runs `portfolio-step!`
   autonomously every 24 h, emitting `:portfolio/step` evidence.
   That step *is* a typed event with a chosen action; if that
   action keyword is given a writeable interpretation in the
   manifold, the scheduler is the head and WM stays the display.
   This is the cleanest minimal modification.

The choice between (1)/(2)/(3) is the **load-bearing DERIVE decision**.

#### Trace-emit contract — classification per component

| Component | Status | Detail |
|---|---|---|
| Backing store for per-tick state | (a) | `M-war-machine-tuning-snapshots.edn` + `append-snapshot!` exists |
| Structured per-tick emission | (a) | `:portfolio/step` evidence already deposits chosen action + diagnostics |
| Forward-going emitter | (b) | `piano_roll.py` is post-hoc; needs a row-append-per-tick variant |
| `tap>` event-stream pattern from futon2 | (a, transferable) | drop-in for live emission; `(tap> {:type :wm/step …})` matches existing fulab adapter idiom |
| Distinguishing head-trace events from regular `:edits` | **(b)** | needs a new edge type in substrate-2 (proposal: `:head/trace-step` carrying `(commit-id, vertex-from, vertex-to, decision-rationale)`) |

**This is the cheapest contract to land.** All raw materials exist;
the missing piece is a forward-row-append emitter at the same
call-site that does `(scheduler/append-snapshot! ...)` after each
`portfolio-step!`. No new substrate-2 capability is required if the
trace just consumes existing `:portfolio/step` evidence; only if we
want to type-distinguish head-trace from other writes does (b) bite.

#### Per-mission wiring-diagram bridge (separate finding)

The IDENTIFY destination "futon5-style wiring diagrams per mission as
a side-effect of action" is a **(d)-class gap** (genuinely unsolved at
the conceptual level), but with a real path forward surfaced by
Agent 4:

- A futon5 wiring diagram is a **typed directed graph in EDN**, not a
  rendered picture: `:ports/:input`, `:ports/:output`, `:components`
  with `:accepts`/`:produces` type-sets, `:edges` with `:type`,
  validated against I1-I6 invariants by `futon5.ct.mission/validate`.
  Examples: `futon5/data/missions/futon2-aif-ants.edn`,
  `futon3-agent-loop.edn`. **Currently hand-authored as static
  analysis** — explicitly *"NOT a mission spec"* per the file
  preamble.
- The WM produces moves; missions today **do not carry the typed-port
  surface** the diagram pattern requires. Auto-emission would require
  a structured EDN sidecar per mission, populated by the WM.
- **Feasible bridge**: WM emits a *partial* wiring diagram per
  mission, populated incrementally — every Click on a file becomes a
  `C-execute → I-environment` edge, every commit a node, etc. The
  diagram grows as the mission is worked. Validation runs only once
  the diagram crosses a completeness threshold. Pairs naturally with
  the live piano-roll: each piano-roll row carries a diff against the
  current wiring diagram.
- This is its own mission-scope chunk, not part of M-war-machine-wiring.
  Logged here as a DERIVE-scope follow-up candidate.

#### Adoption recommendations from futon2 AIF ants

Agent 3's transferability summary, ratified:

- **Read surface**: adopt the `(world × position) → obs-map`
  function shape from `ants.aif.observe/g-observe` (16 normalized
  scalars, fixed-key map). Field set must be redesigned for stack
  semantics — don't transfer `:food`/`:pher` literally; use
  `:T-here`, `:∇-magnitude`, `:Δ-pressure`, `:drift-velocity`,
  `:neighbour-T-mean`, `:evidence-density`, `:gate-proximity`.
- **Head-write split**: adopt directly. Head emits
  `{:action <keyword> :diagnostics {…} :head-state' …}`; substrate-2
  owns the `case` switch from action keyword to field mutation. The
  action vocabulary itself is domain-specific.
- **Move-selection**: futon2 uses EFE + softmax with
  mode-conditioned action sets. The portfolio-inference loop
  *already* uses EFE-ranked actions over a 5-action repertoire with
  `:π/abstain-threshold 0.55`. The architectural alignment is
  remarkable — porting the futon2 policy machinery is largely
  unification rather than translation.
- **Engine protocol** (`select-pattern`, `update-beliefs`): a
  minimal contract surface; the maximal worked example is the
  five-stage `observe → perceive → affect → policy → default-mode`
  pipeline in `ants.aif.core/aif-step`. **Decision for DERIVE**:
  protocol vs. pipeline as the contract's surface area.
- **Structural limitation flagged**: futon2 ants do parameter
  updating only — no model restructuring. WM on a *changing*
  manifold (substrate-2 evolves as missions complete) needs the
  slow second AIF loop M-aif-head defers. The wiring contracts
  must leave a hook for this.

#### MAP success criterion verdict

For each contract, one of the four classifications:

- **Manifold-read**: mostly (a), one (b) (head-facing convenience),
  one (c) (head's current-position concept).
- **Head-write**: surface-level (a) for substrate writes; load-bearing
  (c) on the WM-I4 wall and on closure validation.
- **Trace-emit**: mostly (a)/(b) — cheapest contract, ready to land.
- **Per-mission wiring-diagram emission** (separate): (d), with a
  feasible incremental-emission bridge.

#### Decision points for DERIVE

1. **The WM-I4 question.** Reopen / reframe-as-operator+WM-composite /
   fork-scheduler-as-head. Load-bearing.
2. **Strategic vs portfolio observation surface.** Pick one or both.
3. **Position semantics.** Per-mission proxy (cheap, weak) vs.
   per-vertex addressing in substrate-2 (expensive, strong).
4. **Protocol vs pipeline as the contract surface.** futon2's
   2-method protocol vs. its 5-stage pipeline.
5. **Per-mission wiring diagram**: in scope as a DERIVE-scope
   follow-up, or out of scope and queued as its own mission?

#### Recommended DERIVE first move

**Land the trace-emit contract.** It's the cheapest, validates the
framing without committing to the WM-I4 question, and produces an
artifact (a forward-going piano roll) that Joe can read for evidence
that the wiring is doing useful work. Concretely: turn `piano_roll.py`
into a forward-going emitter consuming `:portfolio/step` evidence
from futon1a, with one row appended per scheduler tick. No new
substrate-2 capability required.

Once the live piano roll is running, the manifold-read contract is the
next-cheapest (extends `observe()` with position keys; substrate-2
queries are already there). The head-write decision can be deferred
until those two are landed and the framing has been stress-tested
against real telemetry.

## 3. DERIVE - first sketch

The MAP gaps document surfaced five decision points. DERIVE settles
each, then sketches the three contracts as concretely as the current
substrate allows.

### D-1. The WM-I4 decision: option 3 — scheduler-as-head

Of the three options on the table, **option 3 (fork: scheduler-as-head)
is the right starting point.** Reasoning:

- The portfolio-inference scheduler in
  `futon3c/src/futon3c/portfolio_inference/scheduler.clj` already runs
  autonomously every 24 h (per Checkpoint 5 of M-stack-stereolithography),
  emits typed `:portfolio/observation`, `:portfolio/belief`,
  `:portfolio/policy`, and `:portfolio/step` evidence per tick.
  **The autonomous loop already exists.** Calling it the head exposes
  what's structurally happening, rather than building a new actor.
- WM-I4 / Hobbes is preserved as currently scoped: the *display* doesn't
  act; the *scheduler* (already not the display) gets named as the
  head. The cljs UI remains a `:π-display`. The Hobbes criterion in
  spirit is "the operator-facing surface doesn't make decisions for
  the operator" — that survives untouched.
- The five-action EFE repertoire that the scheduler already chooses
  among is the head's action vocabulary; we don't have to invent one.
- Option 1 (reopen WM-I4) is the destination but premature; the
  framing should earn that move with evidence first. Option 2
  (operator+WM composite) is hand-wavy — it relabels existing
  operator actions as "head writes" without changing the system.

**Cyborg interoception consistency** (per memory `project_joe_hud_model`):
the slow scheduler-head plays the role of the autonomic loop; the
display-WM is the self-model surface; the operator's Click/Tick is
fast voluntary action. Option 3 fits this anatomy without strain.

#### D-1 addendum: the consent gate (Joe, 2026-04-30)

The right reading of WM-I4 / Hobbes is **not** "WM doesn't act" —
it's "WM doesn't act *unilaterally*; sovereignty is preserved by a
consent step." The consent step is therefore a **first-class
artifact** of the design, not an implicit property of the
architecture.

This makes the option 3 → option 1 migration trivial: option 3 has
operator-mediated consent ("this is what's up next; Joe OKs it; we
get to work"); option 1 replaces the operator at the consent gate
with an **autopen** — a deterministic auto-approval policy. The
wiring contracts, the action vocabulary, the substrate-write paths,
the trace shape are all *unchanged*. Only the consent step's
implementation changes.

Consequences for the design:

- The head-write contract (D-7) routes through a **proposal queue**,
  not directly into substrate-2. The scheduler PROPOSES; the operator
  (or autopen) AUTHORIZES; substrate-2 WRITES.
- The proposal queue is a typed evidence stream
  (`:head/proposed-action`); the consent step is a state transition
  on a proposal record (`:status :pending` → `:authorized` or
  `:rejected`); the substrate-write fires on the transition.
- The cljs UI's existing "next-move tile" is already half of this
  surface — it shows the chosen action. The other half (the
  approve/reject control) is the missing piece on the operator side.
- The autopen is a separate process — not a code change to the head
  — that drains the proposal queue with a deterministic policy
  (e.g. *"authorize when EFE confidence > 0.8 and abstain-threshold
  not triggered"*). Adding it later doesn't touch the head's code.

**This insight upgrades WM-I4 from invariant-by-architectural-fiat
to invariant-by-explicit-consent-gate.** The Hobbes criterion
becomes observable, configurable, and inspectable. It also gives the
augmentation ledger a more principled role: an entry in the ledger
is a record of consent + downstream effect.

### D-2. The observation-surface decision: portfolio (16-channel)

Pick the portfolio surface (`futon3c.portfolio.observe/observe`),
not the strategic one (`futon0/scripts/futon0/report/war_machine.clj
observe`). Reasoning:

- Portfolio is the live surface — running cljs UI consumes it,
  scheduler ticks against it, augmentation ledger correlates against
  it. Strategic is sibling/older.
- All MAP-side machinery (`:portfolio/step` evidence,
  `M-war-machine-tuning-snapshots.edn`, `derive-augmentation-from-evidence`)
  is already keyed to portfolio.
- Strategic stays a viewing alternative; nothing forbids a future
  bridge there too, but DERIVE doesn't need it.

### D-2.5. Inference substrates and the operator-coupled cross-check (added 2026-04-30 per Joe's correction)

The IDENTIFY framing suggested a single, purely-geometric substrate
(T / ∇T / ΔT / drift over substrate-2). That was a useful starting
point but is incomplete in two ways: (a) substrate-2 is *descriptive*,
not *normative* — it shows what is there, not what is needed; and
(b) the operator is not external to the manifold but **coupled with
it**, so operator-stated priorities are themselves signals from a
subsystem doing AIF-style inference about the stack, not decrees the
wiring should ratify by default ("kowtowing to the CEO of Futonland").

DERIVE therefore enumerates **multiple candidate inference substrates**
and treats the choice between them as exploration rather than fait
accompli.

#### Candidate inference substrates

1. **Pure geometric (substrate-2: T, ∇T, ΔT, drift).** Descriptive
   structural state of the codebase manifold. Strengths: existing
   infrastructure, fully operational, well-typed exports. Weaknesses:
   descriptive only — cannot say what *should* happen, only what *is*.
2. **AIF-internal (free-energy F, belief μ, precision π, prediction
   error).** The policy's own computational substrate, what it would
   compute over its observations regardless of geometric reading.
   Already in use by the portfolio-inference scheduler. Strengths:
   principled, in production. Weaknesses: aggregates over the whole
   stack; loses position-locality; doesn't directly read substrate-2.
3. **Normative pull (invariant candidate queue, open mission goals,
   operator-stated priorities).** What the system needs, not what is.
   Strengths: cleanest source of "should." Weaknesses: not
   currently typed as observations; some sources (operator priorities)
   are partially captured in the portfolio aggregate but not as
   typed individual signals.
4. **Operator-coupled (the operator as a coupled subsystem).** The
   operator's recent activity, hinge-log entries, mission-stamp
   choices, stated priorities — all are observations *about the
   operator's coupling with the stack*. The operator is itself a
   subsystem performing AIF-style inference; their preferences are
   signals from that subsystem. Strengths: captures preferences the
   system would otherwise have to invent. Weaknesses: operator-
   coupled inference machinery doesn't yet exist as a typed surface.
5. **Hybrid: every other substrate as observation channels into a
   joint AIF model whose belief μ includes both stack-state and
   operator-state.** Geometric T/∇T/ΔT/drift become observation
   channels; normative pull becomes prior bias; operator-coupled
   signals become observations about a coupled subsystem.
   Strengths: every other substrate is a special case of this one.
   Weaknesses: substantially more pieces; verifies harder.

The v1 implementation should adopt **substrate 5 (hybrid)** as the
target shape, but populate it incrementally — beginning with
substrates 1 and 2 (geometric + AIF-internal), then adding substrates
3 (normative pull) and 4 (operator-coupled) in subsequent increments.
This avoids both single-substrate myopia and over-construction in v1.

#### The operator-coupled cross-check (the kowtow defense)

When the operator marks an invariant or mission as high-priority, the
system should **not** simply ratify and queue an action toward it.
That kind of ratification provides no value beyond what the operator
already had in mind; a system that only kowtows is a yes-machine.

Instead, the system **formally cross-checks** the operator's
recognition against the other substrates:

- **Geometric corroboration**: do the related vertices show high
  T or sharp |∇T|? Does the connectivity around the named target show
  the structural pressure the operator's prioritisation implies?
- **AIF corroboration**: would the policy's own EFE ranking place
  related actions near the top regardless of the operator's input?
  If yes, the operator is reading the same field the policy reads.
- **Operator-internal coherence**: does the operator's recent
  activity (commits, mission-stamps, hinge-log entries) align with
  their stated priority? If yes, the prioritisation is consistent
  with their behaviour; if no, it might be aspirational rather than
  current.

Three outcomes:

- **All substrates corroborate** → high-confidence ratification,
  proposal queued with `:corroboration :high`.
- **Partial corroboration** → suggest with caveats; expose the
  disagreement to the operator. *"You marked X high-priority;
  geometry agrees but AIF policy ranks Y above X."*
- **No corroboration** → flag explicitly: *"Your stated priority
  disagrees with the structural picture and the policy ranking.
  Want to elaborate on what you're seeing?"*

This is the "operator-coupled formal check" that distinguishes the
WM from a yes-machine. It also means the operator gets information
back from the system: when the operator's recognition disagrees with
the cross-check, *somebody is wrong*, and the system should help
locate where.

#### Implications for the contract sketches

D-6 (manifold-read) is re-cast as multi-substrate (see updated sketch).
D-7 (head-write) is augmented with a `:corroboration` field on every
proposal (see updated sketch).

#### Refinement of IDENTIFY's open conceptual question

The "one head vs N heads" question becomes sharper: the head is one,
but it integrates over **multiple inference substrates**. Multi-
substrate ≠ multi-head. A new question opens: **how should the
substrates be weighted in the integration?** Default to equal
weighting; expose substrate weights as policy-tuning parameters
(M-war-machine-tuning territory).

### D-3. The position-semantics decision: layered (mission + vertex)

Position is a **two-level coordinate**, not a single value:

- **Coarse position** — which mission the head is currently working
  on (`:active-mission` from the mission queue). Already addressable.
- **Fine position** — which `(vertex-id, commit-id)` pair on the
  substrate-2 hypergraph the head is currently touching. Resolved
  on-demand from the active mission's last edit / last evidence
  deposit / last Click target.

Both are queryable today; the head doesn't need to *choose* its fine
position in v1. The fine position is *induced* by what the operator
just did (most recent commit, most recent Click). v2+ can introduce
explicit fine-position selection (foraging policy). v1 says: fine
position = whatever the operator was last working on.

This deferral is the key simplifying move — it lets the manifold-read
contract land before the foraging-policy question is settled.

### D-4. The contract-surface decision: pipeline (with protocol-shape preserved)

Adopt the pipeline shape (futon2's `observe → perceive → affect →
policy → default-mode`) as the head's internal architecture; keep the
2-method protocol (`select-pattern`, `update-beliefs`) as the *external*
contract surface for the futon2 engine adapter. The portfolio-inference
scheduler already runs a pipeline; this just gives the pipeline a
named protocol-skin so the futon2 AIF engine can compose with it.

### D-5. The per-mission wiring-diagram decision: deferred

Per-mission wiring-diagram emission is **out of scope for this mission**.
The chapter0 idiom requires structural changes to mission shape that
this mission shouldn't carry. Logged as a follow-up candidate
(`M-war-machine-wiring-diagram-emission` or similar) for future
spin-up after the three core contracts land.

---

### D-6. Manifold-read contract sketch (multi-substrate per D-2.5)

The head's observation function returns a **structured** observation
keyed by inference substrate, rather than a flat merge. This lets
the policy weight substrates independently and lets the cross-check
mechanism (D-2.5) compute corroboration per substrate.

```clojure
(ns futon3c.portfolio.observe-on-manifold)

(defn observe-at
  "Head's perception at a manifold position, returned as a structured
   multi-substrate map (see D-2.5 for substrate enumeration).

   Position is a map:
     {:mission <mission-id>           ; coarse, always required
      :vertex  <hx:type:repo/qname>   ; fine, optional in v1
      :commit  <commit-id>}           ; fine, optional in v1
   "
  [position]
  {:stack-aggregate     (futon3c.portfolio.observe/observe)        ; substrate 2 (AIF-internal aggregate)
   :geometric           (when (:vertex position)                    ; substrate 1
                          (substrate-2-manifold-channels position))
   :normative-pull      (normative-pull-channels position)          ; substrate 3
   :operator-coupled    (operator-coupled-channels position)})      ; substrate 4

(defn substrate-2-manifold-channels
  "Substrate 1 — geometric. Manifold-local readings via futon1a XTDB
   at localhost:7071. Values normalized to [0,1] except
   :grad-direction (a vertex-id)."
  [{:keys [vertex commit]}]
  {:T                (substrate-2/T vertex commit)
   :grad-T-magnitude (substrate-2/grad-T-magnitude vertex commit)
   :grad-direction   (substrate-2/argmax-grad-T-neighbour vertex commit)
   :delta-T          (substrate-2/delta-T vertex commit)
   :drift            (substrate-2/drift-to-recent vertex commit)
   :neighbour-T-mean (substrate-2/neighbour-T-mean vertex commit)})

(defn normative-pull-channels
  "Substrate 3 — what the system needs, not what is. Reads from the
   invariant candidate queue, open missions, operator-set priorities."
  [{:keys [vertex mission]}]
  {:open-candidates-near (candidate-queue/at-position vertex)
   :mission-priority     (mission-store/priority mission)
   :operator-priorities  (operator-priorities/active-list)})

(defn operator-coupled-channels
  "Substrate 4 — operator as a coupled subsystem. Reads operator's
   recent activity as observations about their coupling with the stack."
  [{:keys [vertex mission]}]
  {:recent-commits-near       (operator-activity/recent-commits-near vertex)
   :recent-mission-stamps     (operator-activity/recent-stamps-for mission)
   :recent-hinge-log-entries  (hinge-log/recent-entries-near vertex)
   :stated-priority-coherence (operator-priorities/coherence-with-recent-activity)})
```

**v1 implementation guidance:** populate substrates incrementally.
I2 lands substrates 1 + 2. I3a adds substrate 3 (normative pull) as
typed individual channels rather than relying on the aggregate. I3b
or a follow-up adds substrate 4 (operator-coupled) — this requires
operator-activity reading machinery that doesn't yet exist as a
typed surface and is therefore the most exploratory piece.

The head's policy reads from `observe-at` and chooses an action; the
**cross-check function** (D-2.5) computes corroboration across the
four substrates and attaches it to the proposal:

```clojure
(defn cross-check
  "Returns {:geometric :high|:partial|:low|:absent
            :aif-internal :high|:partial|:low|:absent
            :operator-coupled :high|:partial|:low|:absent
            :overall-confidence <0.0-1.0>}
   Each substrate gets an independent corroboration verdict. Operator-
   coupled is the substrate that handles the kowtow defense: an
   operator-stated priority is corroborated by operator-coupled iff
   their recent behaviour matches the stated priority."
  [observation chosen-action]
  ...)
```

### D-7. Head-write contract sketch (proposal-queue routed)

The chosen-action keyword in `:portfolio/step` becomes the head's
action vocabulary. Substrate-2 owns the case switch from keyword to
field mutation, mirroring futon2's `gather-food` / `deposit-food` /
`pheromone-drop` pattern. **But the head doesn't write directly** —
it deposits a proposal that the operator (or autopen) authorizes;
the substrate-write fires on authorization.

```clojure
(ns futon3c.portfolio.act-on-manifold)

(def head-action-vocabulary
  "Five-action repertoire. Each action either mutates the manifold
   (:writes-edge?) on authorization, or is a no-op (:abstain)."
  {:advance-mission   {:writes-edge? true  :edge-type :mission/status-step}
   :gate-submission   {:writes-edge? true  :edge-type :gate/submission}
   :evidence-deposit  {:writes-edge? true  :edge-type :evidence/deposit}
   :propose-binding   {:writes-edge? true  :edge-type :pattern-application}
   :abstain           {:writes-edge? false}})

(defn propose!
  "Head's per-tick action — deposits a proposal record into the
   consent queue with the top-N candidate actions and an interpretive
   geometric-AIF rationale for each. Does NOT write to the manifold
   directly. The :portfolio/step evidence is still emitted
   (telemetry); the substrate-write happens later, on authorize!"
  [position observation]
  (let [proposal-id (random-uuid)
        ranked-candidates (policy/rank-actions observation)        ; ordered by EFE
        top-N (take 3 ranked-candidates)                           ; configurable
        candidates (mapv (fn [c]
                           {:action (:keyword c)
                            :edge-type (get-in head-action-vocabulary
                                               [(:keyword c) :edge-type])
                            :edge-target (action->target (:keyword c) observation)
                            :diagnostics (:diagnostics c)
                            :corroboration (cross-check observation
                                                        (:keyword c))
                            :rationale (interpret-reading observation c)}) ; D-7.5
                         top-N)
        primary (first candidates)]
    (deposit-portfolio-step! (:action primary) (:diagnostics primary)
                              position)                              ; existing
    (when (some #(get-in head-action-vocabulary
                          [(:action %) :writes-edge?])
                candidates)
      (write-evidence!
        :head/proposed-action
        {:proposal-id proposal-id
         :status :pending
         :proposed-at (now)
         :position position
         :candidates candidates                  ; multi-option per Joe 2026-04-30
         :primary-rank-by :efe
         :reading (structural-summary observation)})) ; one-sentence "what's
                                                       ; happening here" header
    {:proposal-id proposal-id
     :candidates candidates}))

(defn authorize!
  "Operator-or-autopen approves a pending proposal. Triggers the
   substrate write. Idempotent: re-authorizing a non-pending proposal
   is a no-op + a logged warning."
  [proposal-id authorized-by]
  (let [p (read-evidence! :head/proposed-action proposal-id)]
    (when (= :pending (:status p))
      (substrate-2/write-edge!
        {:edge-type (:edge-type p)
         :from      (:vertex (:position p))
         :to        (:edge-target p)
         :commit    (:commit (:position p))
         :props     {:source :wm-head
                     :proposal-id proposal-id
                     :authorized-by authorized-by
                     :authorized-at (now)
                     :diagnostics (:diagnostics p)}})
      (write-evidence!
        :head/proposed-action
        (assoc p :status :authorized
                 :authorized-at (now)
                 :authorized-by authorized-by)))))

(defn reject!
  "Operator-or-autopen declines a pending proposal. No substrate
   write happens; the rejection is logged (which is itself signal —
   rejected proposals feed back into policy tuning)."
  [proposal-id rejected-by reason]
  ...)
```

The autopen is implemented as a separate consumer:

```clojure
(ns futon3c.portfolio.autopen
  "Optional auto-authorizer. NOT enabled by default. When enabled,
   drains the consent queue with a deterministic policy. Toggling
   this off restores Hobbes; toggling on lifts it."
  (...))

(defn autopen-policy-default
  "Authorize when EFE confidence is high AND abstain wasn't triggered."
  [proposal]
  (let [{:keys [G abstain?]} (:diagnostics proposal)]
    (and (not abstain?)
         (> (:confidence G) 0.8))))

(defn run-autopen!
  "Drains the pending queue. Each pass: read pending proposals,
   apply policy, authorize or skip."
  [policy-fn]
  (doseq [p (pending-proposals)]
    (when (policy-fn p)
      (authorize! (:proposal-id p) :autopen))))
```

Five things to note:

1. **The proposal queue is the consent gate.** Hobbes preservation
   = "operator authorizes"; Hobbes lifting = "autopen authorizes."
   No other code path changes between option 3 and option 1.
2. **The `:source :wm-head` prop on every authorized edge**
   distinguishes head-emitted writes from watcher-emitted writes
   (resolves MAP gap (c) by convention).
3. **The `authorized-by` prop is the audit trail.** Every edge
   carries either an operator handle or `:autopen`; the augmentation
   ledger becomes a query against `:authorized-by`.
4. **`:propose-binding`** lands `:pattern-application` edges per
   substrate-2's existing taxonomy slot, but the closure validator
   remains absent — the pair-mission DERIVE feeder #5 gap is
   inherited as-is. v1 authorized writes are structurally valid;
   semantic correctness is verified-later.
5. **Rejected proposals are signal too.** A rejection record feeds
   back into policy tuning (M-war-machine-tuning territory) — the
   head learns operator preferences from explicit rejections faster
   than from passive correlation.

### D-7.5. Interpretive readings — geometric-AIF rationale vocabulary (added 2026-04-30 per Joe)

The existing WM tells the operator what to do but doesn't show its
work in geometric-AIF terms. The wired-up version should generate
**interpretive readings** that combine geometric facts with AIF-flavoured
intent — Joe's example phrasing: *"the curvature in this region of
the stack is extremely high and needs to unfold in order to support
effective motion."*

This requires a **rationale vocabulary** that maps raw geometric +
AIF quantities into legible interpretive language. The vocabulary
is a design artifact of this mission, populated incrementally and
inspectable as code.

#### Two functions, one vocabulary

```clojure
(defn structural-summary
  "One-sentence headline describing what's happening at the position,
   in geometric-AIF terms. Used as the proposal's :reading header."
  [observation]
  ...)

(defn interpret-reading
  "Per-candidate-action rationale: why this action is being suggested
   given the structural picture. Returned as :rationale on each candidate
   in the proposal. Plain-language sentence."
  [observation candidate]
  ...)
```

Both pull from the same vocabulary but operate at different scopes:
`structural-summary` is position-global ("what's happening here");
`interpret-reading` is action-conditioned ("why this action").

#### Vocabulary v0 — starter primitives

The vocabulary builds compositionally. Geometric primitives:

| Quantity | High value reading | Low value reading |
|---|---|---|
| T (tension) | "tense" / "under pressure" | "slack" / "relaxed" |
| ∣∇T∣ (gradient magnitude) | "sharp edge" / "discontinuity" | "smooth" / "uniform" |
| ΔT (laplacian) | "concentration" / "fold" | "flat" / "even" |
| drift | "wandering" / "unstable" | "settled" / "stationary" |
| ΔT/T (rough curvature proxy) | "rough" / "needs unfolding" | "smooth" |

AIF primitives:

| Quantity | Reading |
|---|---|
| free-energy F (high) | "the model isn't matching what's happening here" |
| precision π (low) | "uncertain about this region" |
| prediction error (high) | "surprise" / "unexpected" |
| EFE pragmatic (high for X) | "X would directly reduce free energy" |
| EFE epistemic (high for X) | "X would resolve uncertainty" |

Composite phrasings combine geometric + AIF:

- High T + high prediction error → "tense region the model isn't
  predicting well — needs attention"
- High |∇T| + low precision → "uncertain edge — might be where the
  next move should land"
- High ΔT + high drift → "concentration that's wandering — fold
  about to unstick"
- Joe's example: high curvature + low effective-motion proxy →
  "curvature is extreme; this region needs to unfold to support
  effective motion"

#### Action-conditioned rationale phrasings

For each of the five action types in `head-action-vocabulary`, a
template combines the position's structural picture with what the
action would do:

- `:advance-mission` → "advancing M would smooth the gradient at
  vertex V (∣∇T∣ drops from x to y)"
- `:gate-submission` → "submitting at gate G would resolve the
  prediction error here (F drops from x to y)"
- `:evidence-deposit` → "depositing evidence at vertex V would
  reduce uncertainty about its T value (π rises from x to y)"
- `:propose-binding` → "binding pattern P to vertex V would close
  the open SORRY here (T drops from 1 to 0 if validator confirms)"
- `:abstain` → "no action of high enough EFE confidence to act on;
  surface the structural picture for operator review"

#### Implementation strategy

Templates filled with computed quantities, optionally polished — but
**not LLM-generated in v1.** Reasons: deterministic; auditable;
operator can read the same template twice and see what changed;
vocabulary grows by explicit extension rather than by training.
LLM polish is a follow-up if templates feel too stilted in practice.

#### Where the vocabulary is verified

VERIFY's first concrete task: develop interpretive readings for a
sample of Invariant Queue items, missions, and commit clusters,
and check that they read sensibly. See §5.

### D-8. Trace-emit contract sketch

Two complementary mechanisms; **adopt both**:

**Mechanism A — `tap>` event stream (push, Clojure-side).**

```clojure
(defn deposit-portfolio-step!
  "Existing function. Add tap> emission as a non-blocking
   trace-side-channel. Consumers subscribe with add-tap."
  [chosen-action diagnostics position]
  (write-evidence! :portfolio/step ...)        ; existing
  (tap> {:type :wm/head-step                    ; new
         :ts (System/currentTimeMillis)
         :position position
         :action chosen-action
         :diagnostics diagnostics}))
```

A separate consumer subscribes via `add-tap` and appends one row to
the forward-going piano roll. This is the futon2 `fulab.clj` pattern
ported directly.

**Mechanism B — `:portfolio/step` polling (pull, Python-side).**

Extend `piano_roll.py` with a `--live` mode that polls
`GET /api/alpha/evidence?type=:portfolio/step&since=<last-seen>` from
futon1a, appends new rows to a tall PNG/SVG that grows downward. The
bottom edge of the figure is "now" — the head's current position.

Mechanism A is for in-process consumers (REPL, embedded view);
Mechanism B is for an out-of-process artifact (a desktop poster that
extends in real time).

Both share the row schema:
```clojure
{:ts <epoch-millis>
 :position {:mission <mission-id> :vertex <vertex-id> :commit <commit-id>}
 :action <action-keyword>
 :diagnostics {:G ... :ambiguity ... :info ... :colony ... :survival ...}}
```

Same shape that goes into substrate-2 evidence; same shape the live
emitter renders. **One emission, two consumers.** No new substrate-2
capability required — the optional `:head/trace-step` edge type
flagged in MAP is *not* needed for v1 since `:portfolio/step` evidence
already carries everything the trace needs.

---

### D-9. INSTANTIATE sequencing — four increments, cheapest first

| Increment | Scope | Substrate-2 changes | Effort |
|---|---|---|---|
| **I1: trace-emit live** | `tap>` emission + `piano_roll.py --live` | none | ~1 day |
| **I2: manifold-read extension** | `observe-at` + `argmax-grad-T-neighbour` | one new convenience helper | ~3-5 days |
| **I3a: head propose** | `propose!` + `:head/proposed-action` evidence type + UI authorize/reject control on the existing next-move tile | `:source :wm-head` convention | ~1 week |
| **I3b: head authorize-and-write** | `authorize!` triggers substrate write; rejection feedback into policy tuning | none beyond I3a | ~3-5 days |
| **I4 (optional, post-mission)** | autopen consumer | none | ~1 day, mostly policy tuning |

**I1 first** because it produces a live artifact Joe can read for
evidence that the framing does useful work, *without* committing to
the consent-gate reframe operationally. The `tap>` emission costs
nothing to add; the `--live` mode of `piano_roll.py` reuses the
existing renderer.

**I2 next** because it adds the field input to the head's policy
without changing what the head does. The `observe-at` extension is
purely additive — existing behaviour preserved when fine position
is `nil`.

**I3 split into two increments** to keep the consent-gate testable
in isolation. **I3a** lands proposals + authorize/reject UI without
yet wiring substrate writes — Joe can see what the head *would* do
and approve/reject without consequences. The existing next-move
tile in the cljs UI gets two new buttons. **I3b** activates the
substrate write on authorization; only after I3a's proposal stream
has been observed and the authorize/reject UX is good enough to
trust does the actual writing turn on. Rejected proposals from I3a
become the first dataset for policy tuning.

**I4 (optional, deferred to follow-up)** is the autopen — a separate
consumer that drains the consent queue with a deterministic policy.
Building it later, after I3a has produced a corpus of operator
authorize/reject decisions, lets the autopen's policy be *trained*
against that corpus rather than hand-coded blind.

### D-10. ARGUE-stage prep

The next phase needs to argue:

1. **The consent gate preserves WM-I4 in spirit and in letter.** The
   Hobbes criterion is now *located* — not a property scattered
   across the architecture but a single state transition (`:status
   :pending → :authorized`) in a typed evidence stream. As long as
   the authorizer is the operator, sovereignty is preserved exactly
   as WM-I4 demanded. The autopen substitution is then a
   *configuration choice*, not an architectural break — and it's
   inspectable because every edge carries `:authorized-by`.
   Anticipated objection: "the head proposing actions IS the WM
   acting in spirit." Response: proposing without authorization
   has no manifold effect; the field is unchanged until the consent
   gate fires. The proposal is a recommendation; only the authorized
   edge is an act.
2. **Position-layering (mission + vertex) is the right semantic.**
   Anticipated objection: "the head should choose its position; a
   passive operator-induced position isn't a real foraging head."
   Response: v1 doesn't aspire to autonomous foraging; v1 lets the
   head *observe* the manifold even though the operator selects what
   to look at. v2 introduces selection.
3. **Trace-emit is the right first increment.** Anticipated
   objection: "you can't claim the wiring works until the head
   actually writes." Response: I1 validates the trace-emit half of
   the wiring loop in isolation; if the trace doesn't read sensibly,
   the rest of the wiring shouldn't be built.
4. **Abandoning per-mission wiring-diagram emission for v1 is
   pragmatic, not a retreat.** Anticipated objection: "Joe explicitly
   raised it in the IDENTIFY destination." Response: the IDENTIFY
   destination is a destination, not v1; the mission-shape changes it
   requires are larger than this mission's scope.

## 4. ARGUE - first sketch

### Plain-language argument

The system has two parts that exist but don't talk to each other.

The first is the War Machine: a display that shows the operator the
state of the work in progress — which missions are open, which are
stalled, which way energy is flowing across the codebase. It is a
reading surface, not an actor.

The second is a recently-completed piece of infrastructure that
exposes the structural state of the codebase — where tension is
concentrated, how things drift relative to each other, what's
connected to what. It is descriptive, not prescriptive: it shows
what is there, not what should be done about it. It is a sensing
surface that nothing currently consumes.

The normative side — "what should be done" — comes from a separate
existing surface: the invariant candidate queue, open mission
goals, and operator-set priorities. These say what the system
needs; the structural sensing surface says what the system is.

This mission wires the War Machine to both sides. The War Machine
will start surfacing concrete suggestions of what to work on next,
drawn from the combination: structural pressure (where the gradient
is sharp) read against normative pull (which candidates are pending,
which missions are open). The operator will accept or decline each
suggestion with a single click. Accepted suggestions become recorded
actions. Declined ones become recorded data about what kinds of
advice don't fit. A live-updating record of what the system is
doing accumulates as a side-effect.

There is a subtlety here that distinguishes this design from a
yes-machine. The operator is not external to the system — they work
alongside it, coupled with it. When the operator states a priority,
that statement is a signal *about a deformation they have noticed*,
not a decree the system is obliged to ratify. The wiring therefore
performs a formal cross-check: does the structural picture agree
with the operator's stated priority? Does the policy ranking agree?
Does the operator's recent behaviour match what they say they want?
If everything corroborates, the suggestion moves forward with
confidence. If something disagrees, the system flags the
disagreement back to the operator and asks for elaboration. The
operator receives information from the system, not just the other
way around.

What this mission produces, concretely:

- The War Machine display gains a "do this next" tile with
  accept/decline buttons.
- Accepted suggestions write a tagged record into the same place the
  system already keeps work-records.
- A live trace of suggestions and decisions extends downward in real
  time as a printable artifact.
- The path to gradually automating the accept/decline step is built
  in by design: a single small piece can be replaced when the
  operator wants to step back from approving every move.

### Why this design is necessary, not just useful

Three structural reasons, each of which makes a downstream capability
impossible until this mission lands.

**1. No history of suggestions and decisions exists today.** The
operator looks at the War Machine, decides what to do, and acts —
but nothing records which readings led to which actions. Without
that record, the system cannot ever learn from operator preferences,
cannot ever improve its suggestions, and cannot ever be safely
handed even a small slice of autonomous action because there is no
track record to evaluate. Every other capability that depends on
"the system gets better over time" is impossible until this record
exists.

**2. The recently-completed sensing infrastructure has no consumer.**
A working sensing surface that nothing reads from is not
infrastructure — it is a claim that may or may not hold up. This
mission is the first read against that surface. Until something
reads it, no one knows whether it does what it claims to do, and the
work that built it cannot be validated. Verification of the sensing
infrastructure is not optional; it is the precondition for trusting
any future system that depends on it.

**3. Without a single, observable place where the operator says yes
or no, gradual automation is incoherent.** "Hand over routine
decisions" requires that there be something specific that can be
handed over. With this mission's design, that something specific is
a single small piece of code: the auto-approver. Without the
yes-or-no queue this mission builds, "automation" is hand-waving
over a system that has no place to insert one. The path from "I
approve every move" to "I approve only the hard ones" to "I approve
only the strategic ones" is impossible to walk if there is no
discrete approval step to gradually delegate.

These three reasons compound: each makes the next one possible. The
record makes learning possible; learning makes the auto-approver
trustworthy; the auto-approver makes graduated autonomy feasible.
None of these can begin without the wiring this mission builds.

### Supporting analysis

The remainder of this section defends specific design choices made in
DERIVE. It is supplementary to the plain-language argument above; the
mission's case rests on the plain statement, and the analysis below
is for reviewers wanting detail on individual decisions.

### A-1. Why the consent gate is the right WM-I4 reading

The original WM-I4 was *"WM does not act."* That phrasing is
brittle — any structural change risks violating it, and "doesn't
act" is a property scattered across the architecture rather than
located in one observable place. The consent-gate reading
(*"doesn't act unilaterally; sovereignty is preserved by the
consent step"*) is stronger because:

- It **locates** Hobbes preservation in a single state transition
  (`:pending → :authorized`) on a typed evidence record, which can
  be queried, audited, and tested.
- It makes Hobbes preservation **configurable** — supervised,
  semi-autonomous, and fully autonomous modes are the same
  architecture with different authorizers, not different systems.
- It gives the augmentation ledger a more principled meaning:
  consent record + downstream effect, rather than passive
  correlation.
- It maps cleanly onto cyborg-interoception precedent (memory
  `project_joe_hud_model`): the slow scheduler-head is the
  autonomic loop; the consent gate is the *voluntary motor
  surface* — autonomic activity becomes voluntary action only on
  explicit assent.

**Counter-objection: "operator authorize burden is high."**
Response: bulk-approve patterns + TTL + position-staleness expiry
keep the queue tractable. See A-7.4 for design constraints.

**Counter-objection: "autopen lifts Hobbes by stealth."**
Response: every authorized edge carries `:authorized-by`, making
authorization mode an inspectable property of the manifold itself.
WM display surfaces should show autopen-mode prominently when
active. The configuration is a property of the system, not a
hidden runtime state.

### A-2. Why option 3 over option 1 from the start

Option 3 (scheduler-as-head with operator consent) is the design
that *also* supports option 1 (autonomous foraging). Specifically:
the proposal queue, the action vocabulary, the substrate-2 write
paths, the trace shape, and the policy machinery are all
unchanged between option 3 and option 1. Building option 3 first
therefore costs *zero* on the path to option 1 — the migration is
a single-component swap (operator → autopen at the consent gate).

The argument is symmetric: if option 1 had been built first, it
would have to be retrofitted with a consent gate to recover
operator oversight. Option 3 first is strictly cheaper than option
1 first, given that option 1 is a destination not a target.

### A-3. Why scheduler-as-head, given its slow tick rate

**Objection (not in D-10):** the portfolio-inference scheduler ticks
*daily*. A TM head ticks much more frequently. So calling the daily
scheduler "the head" might be a category error — what we want is a
fast-tick head, and the daily scheduler is the wrong granularity.

**Response:** the head's *belief update* is slow by design (per
M-stack-stereolithography Checkpoint 5's cadence walk-back: daily is
the right cron shape because portfolio-AIF is the slow timescale of
the stack). But the head's *proposal emission* is not bound to
belief-update cadence — `propose!` can be triggered by operator
events (Click, Tick, commit, mission-stamp) as well as by scheduled
ticks. The architecture is:

- **Slow tick** (24h, scheduled): belief update + scheduled
  proposal at the slow timescale (e.g. "this week, advance mission
  X" type proposals).
- **Fast tick** (operator-event-driven): per-operator-action
  proposals at the fast timescale (e.g. "this Click suggests
  evidence-deposit" type proposals).

Both call the same `propose!`; both deposit `:head/proposed-action`
evidence; both go through the same consent gate. The fast tick is
not new infrastructure — it's a hook on existing operator-event
emission.

This unifies the "two timescales, one head" framing implicit in
project memory (`project_joe_hud_model`): autonomic + voluntary as
two aspects of the same agent.

### A-4. Position-layering: passive in v1, active later

DERIVE D-3 chose layered position (mission + vertex), with fine
position induced by recent operator action in v1. The argument:

- v1 is not aspiring to autonomous foraging. The head *observes*
  the manifold even though the operator picks where to look. This
  is sufficient for the manifold-read contract to do work — the
  head's policy gets field-local context (T, ∇T, ΔT, drift)
  alongside its existing 16-channel stack-aggregate.
- v2+ introduces explicit fine-position selection (foraging
  policy) once we know what selection criterion the head should
  use. The futon2 ant pattern (EFE-driven move toward steepest
  pheromone gradient) is the obvious starting point, but ratifying
  it requires evidence the foraging-style policy actually improves
  outcomes — evidence we don't have yet.

**Counter-objection: "passive position isn't a real foraging head;
this isn't a TM-on-manifold, it's a TM-with-operator-hand-on-tape."**
Response: accepted. v1 is exactly that. The TM-on-manifold framing
is a destination this mission moves *toward*, not the v1 the
mission lands. The piano roll already showed that operator-hand-on-
tape produces a real trace; v1 makes that trace ingestible by the
head's policy without committing to autonomous selection.

### A-5. Why trace-emit is the right first increment

Three arguments:

1. **Cheapest.** No new substrate-2 capability; reuses the
   existing piano-roll renderer; the `tap>` emission is a one-line
   addition to `deposit-portfolio-step!`.
2. **Validates the framing in isolation.** If the live piano roll
   doesn't read sensibly — if rows pile up at unhelpful cadence,
   if action keywords don't carry interpretable meaning, if
   diagnostics are illegible — those are findings *before* I2 and
   I3 commit to building anything on the trace shape.
3. **Produces evidence the operator can act on.** The live piano
   roll IS a working artifact: a forward-going record of what the
   head is doing. Even with no manifold-read or head-write, a live
   trace of `:portfolio/step` evidence is more telling than the
   current static cljs UI.

**Counter-objection: "you can't claim the wiring works until the
head actually writes."** Response: I1 doesn't claim the wiring
works — it validates the trace half of the wiring loop. Claims
about the head working come from I3a's authorize/reject corpus,
not from I1.

### A-6. Why per-mission wiring-diagram emission is deferred

The futon5 chapter0 wiring-diagram pattern requires structured
mission specs (typed ports, accepts/produces, edge lists,
timescales) that current missions don't carry. Auto-emission would
require either (a) all missions to be ported into the structured
format first (multi-week effort, out of scope) or (b) the head to
emit *partial* diagrams populated incrementally (Agent 4's
suggestion, which is interesting but its own design problem).

The decision to defer is therefore not retreat but separation of
concerns. Logged as `M-wiring-diagram-emission` (or similar)
follow-up candidate.

### A-7. Surfaced objections deserving work

These are real and need attention before INSTANTIATE.

**A-7.1. The action vocabulary in D-7 is asserted, not sourced.**
The five action keywords (`:advance-mission`, `:gate-submission`,
`:evidence-deposit`, `:propose-binding`, `:abstain`) are inferred
from the MAP-reported "5-action repertoire with abstain-threshold
0.55." MAP did not enumerate the actual five. **VERIFY must
inventory the existing scheduler's action set in
`futon3c.portfolio.policy/expected-free-energy` and adjacent
namespaces** before INSTANTIATE I3a can land.

**A-7.2. Substrate-2 edge taxonomy may not accept the proposed
edge types.** Of the four field-mutating edge types listed in D-7,
only `:pattern-application` is confirmed in substrate-2's existing
taxonomy (Agent 1 finding, status `:new`). The others
(`:mission/status-step`, `:gate/submission`, `:evidence/deposit`)
are inferred. **INSTANTIATE I3a must verify against substrate-2's
actual edge taxonomy and either adopt existing types or coordinate
with substrate-2's owner to add new ones.** The first head-write
is meaningless if it lands on a non-existent edge type.

**A-7.3. Closure validator absence is a noise-amplifier.** Head
proposes `:pattern-application` edges; nothing in v1 verifies the
SORRY actually closed (pair-mission feeder #5 gap, inherited).
Authorized writes can be structurally-valid-but-semantically-wrong,
just-now-detectable. **Mitigation in v1**: head's diagnostics
record the closure-claim alongside the proposal so future
validation is at least retrievable; flag in the augmentation
ledger that pre-validator writes are provisional.

**A-7.4. Proposal staleness / queue backlog.** If propose-rate
exceeds authorize-rate, the queue stales. Open proposals reference
positions that may no longer be current (vertex moved, mission
status changed, commit superseded). **Design constraints v1 must
honour**:

- TTL on proposals (default: 7 days; configurable)
- Position-staleness check: on `authorize!`, refetch the
  proposal's `:vertex` and `:commit`; if either is stale, auto-
  reject with `:reason :position-stale` rather than writing a
  wrong edge
- Consent-queue depth metric exposed on the WM display
- Bulk-reject UI for the operator to clear stale items quickly

**A-7.5. Single-head multi-mission tension.** Per the piano roll,
Joe works multiple missions concurrently. The single-head model
flattens N concurrent missions into "the active mission," but
operator reality is a priority-weighted set. **v1 mitigation**:
`propose!` takes the *most-recently-touched* mission as `:mission`
in `:position`; this is approximate but tractable. **v2+**:
multi-head architecture, deferred per IDENTIFY's open conceptual
question (now closed in v1, reopened in v2).

**A-7.6. Autopen training bias.** Training the autopen on operator
authorize/reject decisions captures operator preferences exactly —
desirable when operator preferences are right, undesirable when
operator has blind spots. The autopen would inherit those blind
spots without ever surfacing them. **Mitigation**: autopen policy
must be **inspectable + editable**, not black-box-trained.
Concretely: the autopen's policy is a Clojure function the operator
reads and modifies, *informed* by the corpus rather than *fitted*
to it. This is a constraint on I4's design.

**A-7.7. Authorization granularity.** The proposal queue authorizes
per-proposal in v1. But operator preferences may be at the
(action-type, target) level: *"authorize gate-submissions to gate
X always; ask me about gate Y."* **v1 design**: per-proposal only.
**v2+ design**: policy-rules for granular auto-authorization,
which is a graceful path to autopen for low-stakes action types
without a full autopen for everything.

### A-8. Vulnerabilities NOT mitigated in v1 — to be honest about

- **Closure validation** (A-7.3) is genuinely deferred. v1 head
  writes `:pattern-application` edges that no validator checks. If
  the head writes wrongly, the noise propagates. The pair-mission
  has the gap; this mission inherits it.
- **Multi-head architecture** (A-7.5) is acknowledged but deferred.
  v1 single-head will produce a piano roll that's *more legible*
  than a multi-head one, which is an aesthetic argument for single-
  head; but the operator-reality argument is for multi-head, and
  v1 doesn't resolve the tension.
- **Autopen-policy inspectability** (A-7.6) is named as a
  *constraint* on I4 but I4 itself is post-mission. There's no v1
  guarantee the eventual autopen will honour it. The mission can
  flag it; it cannot enforce it.
- **Action vocabulary correctness** (A-7.1) is a verification
  pre-condition, not a design property. If the existing scheduler's
  five actions don't match the proposed mapping, INSTANTIATE I3a
  is blocked until reconciled.

These are honestly enumerated rather than papered over. ARGUE's
job is to make these visible to the next reviewer.

### A-9. The case the design handles in v1

Concretely, after I1+I2+I3a+I3b are landed:

- The head emits typed proposals into a consent queue at both slow
  (24h scheduler) and fast (operator-event) tick rates.
- The operator approves or rejects each proposal via the WM
  next-move tile.
- Authorized proposals fire a substrate-2 write tagged with
  `:source :wm-head` and `:authorized-by <operator-handle>`.
- Rejected proposals feed back into policy tuning.
- A live piano roll renders the head's trajectory in real time,
  bottom edge always being "now."
- The augmentation ledger becomes a queryable record of
  (consent-event × downstream-effect) pairs.
- The path to autonomous operation is `add-tap autopen-consumer`
  + a deterministic policy function, with no other code changes.

That's the v1 deliverable. Everything in A-7 / A-8 is a real
limitation of v1 but does not block the v1 deliverable from being
useful.

## 5. VERIFY - first sketch

VERIFY tests two distinct things: (a) that the wiring contracts work
mechanically (the technical pre-conditions that ARGUE A-7 surfaced),
and (b) that the interpretive vocabulary produced by D-7.5 reads
sensibly when applied to real material from the stack.

### V-1. Mechanical pre-conditions (from ARGUE A-7)

Before INSTANTIATE I3a can land, verify:

1. **Action vocabulary correctness** (A-7.1). Inventory the actual
   five-action repertoire used by `futon3c.portfolio.policy/expected-free-energy`
   and adjacent namespaces. Reconcile against the inferred vocabulary
   in D-7 (`:advance-mission`, `:gate-submission`, `:evidence-deposit`,
   `:propose-binding`, `:abstain`). Either confirm the mapping or
   adjust D-7 to match the existing scheduler.
2. **Substrate-2 edge taxonomy** (A-7.2). Verify which of the
   four field-mutating edge types in D-7 are accepted by substrate-2
   today: `:pattern-application` (confirmed), `:mission/status-step`
   (inferred), `:gate/submission` (inferred), `:evidence/deposit`
   (inferred). For each unconfirmed type, either find a substrate-2
   slot it maps to, or coordinate with substrate-2's owner to add
   one.
3. **Operator-activity capture surfaces** (per the operator-coupled
   substrate, D-2.5 and D-6). Inventory what's available today:
   commits (yes, per piano-roll work), mission stamps (yes, per
   `holes/missions/M-*.md` parsing), hinge-log entries (per
   M-trip-journal apparatus — verify capture is live), operator
   priorities ledger (verify it exists). For each missing surface,
   the operator-coupled substrate (substrate 4) is partially blocked.

### V-2. Interpretive vocabulary development (the empirical task)

Joe's framing 2026-04-30: *"the Invariant Queue is a pretty good
source of synthetic examples … missions are also a good source; and
commits and standard static analysis methods can tell us things too."*
The vocabulary in D-7.5 is a design hypothesis that survives or
fails by trial against real material.

Three test corpora, in order of synthetic-to-noisy:

#### V-2.1. Invariant Queue (synthetic, well-typed)

Source: `futon3c/docs/structural-law-inventory.sexp` (798 lines per
M-stack-stereolithography Render v1, also referenced from
`futon4/futon-stack-invariant-model.edn`).

For 5–10 candidate invariants from the queue, produce:

- The geometric reading at the candidate's natural manifold position
  (T, ∇T, ΔT, drift over the affected vertices).
- The AIF reading (would the policy's EFE rank actions toward this
  candidate highly?).
- A composite interpretive sentence using D-7.5's vocabulary.
- An action-conditioned rationale for one or two plausible actions
  (`:propose-binding`, `:advance-mission`).

**Pass condition**: the readings are sensible to Joe as the operator;
distinguish between candidates that genuinely have structural pressure
and ones that are queued aspirationally.

#### V-2.2. Active missions (richer, real-world)

Source: `holes/missions/M-*.md` across all eleven futons.

For 5 active missions, produce:

- Geometric reading at the mission's most-recently-touched vertex.
- AIF reading (urgency, mode, recent EFE history if available).
- Interpretive sentence.
- Action-conditioned rationale for `:advance-mission` and at least
  one alternative.

**Pass condition**: the interpretive reading captures *what the
mission is actually about right now*, not just a generic "this
mission has activity."

#### V-2.3. Commit clusters (raw substrate)

Source: piano-roll cluster groupings (cf.
`futon5a/scripts/piano_roll.py`'s `>` cluster fans). A cluster is a
burst of activity on one futon over a short time window.

For 5 commit clusters from the past 60 days, produce:

- Geometric reading at the affected vertices.
- AIF reading (was this work surprising? did it reduce free energy?).
- Interpretive sentence.

**Pass condition**: the reading distinguishes between clusters of
*productive resolution* (T drops, gradient smooths) and clusters of
*unresolved churn* (T persists or grows despite activity).

#### V-2.4. Before/after differential against a real fix (Joe 2026-04-30 — methodology of choice)

The cleanest VERIFY move available today, and the one that doesn't
rest entirely on operator judgment: **commit a fix that just
landed, then ask the system for its tension reading and "what's
next" signal before the fix and after the fix.** The fix becomes
ground truth. The vocabulary is good if it sees the thing the fix
addressed as a pressure point before, and sees the pressure
dissipate after.

This works today because substrate-2 already addresses positions
by `(vertex, commit)` — we can query T/∇T/ΔT/drift at HEAD~1 and
at HEAD without re-running the watcher. The vocabulary in D-7.5
generates a reading from each query. Side-by-side display makes
the differential legible.

**Procedure:**

1. Pick a meaningful fix that just landed — a single commit that
   addresses a definite thing (a bug fix, a test added, a function
   extracted, an invariant promoted). Commit message preferably
   names what was fixed, in operator language.
2. Identify the affected manifold position(s): the vertex(es) the
   commit edits, the mission it relates to, the surrounding
   neighbourhood.
3. Generate the system's reading at `commit = HEAD~1`:
   - Geometric: T, |∇T|, ΔT, drift over affected vertices
   - Composite interpretive sentence (D-7.5)
   - Top-3 candidate actions with rationales (D-7)
4. Generate the system's reading at `commit = HEAD`: same shape.
5. Compute the differential:
   - Did T drop at the affected vertex?
   - Did |∇T| smooth?
   - Did the "what's next" candidate shift away from this region?
   - Did the interpretive sentence change in a way that reflects
     the resolution?

**Pass conditions** (none requires operator grading):

- **Mechanical**: vocabulary outputs at HEAD~1 vs HEAD show
  geometric quantities changing in the expected direction. T drops
  if the fix added evidence; |∇T| smooths if the fix bridged a
  discontinuity; etc. *Failure mode: vocabulary outputs the same
  reading regardless of computed values — that's the unfalsifiable
  case caught immediately.*
- **Composite-coherence**: the interpretive sentence's claim
  agrees with the underlying numbers. If T dropped from 0.85 to
  0.05 at vertex X but the after-fix reading still calls X "tense,"
  the vocabulary is decoupled from the substrate.
- **Predictive shift**: the top-1 candidate action at HEAD points
  somewhere different from the top-1 candidate at HEAD~1 (assuming
  the fix actually resolved the prior pressure point). If "what's
  next" doesn't move when the structural picture moves, the
  policy is decoupled too.

**Optional operator-coupled validation**: the system's "what's
next" prediction at HEAD names a forward direction. Watch the
operator's subsequent commits — does the operator go where the
system predicted? If yes within k commits (say k=10), the reading
is predictive of operator behaviour. The operator never has to
explicitly grade the system; they just keep working, and the
correlation between predicted-next and actual-next becomes the
validation.

**Why this is the methodology of choice**:

- Real material, not synthetic.
- Differential, so the vocabulary cannot escape into generic
  unfalsifiable readings.
- Mostly automatic — only operator-coupled validation requires
  waiting for the operator to act, and that happens passively.
- Composes with V-2.1, V-2.2, V-2.3: pick a queue item / mission /
  commit cluster that *had a recent fix*, run V-2.4 on it.
- Makes the kowtow defense's working test concrete: if the system
  reads HEAD~1 as "high tension at X" and HEAD as "tension
  dissipated," and the operator's commit message agrees, the
  system is corroborating the operator's action with structural
  evidence — exactly the cross-check we want.
- Runnable today against real history; doesn't require I3a /
  proposal-queue infrastructure to land first.

**First concrete test**: pick a recent fix from any of the active
futons (futon3c and futon6 had the densest recent activity per the
piano roll), run the before/after, see if the vocabulary's outputs
move with the substrate.

### V-3. Cross-substrate corroboration check

Once V-2 is producing readings, verify the cross-check mechanism
(D-2.5):

For 3 cases drawn from V-2.1 / V-2.2 / V-2.3, manually mark a
"high-priority" claim from one substrate (e.g. operator says "this
candidate is high priority"). Compute the corroboration verdict
across the other substrates. Confirm the three outcome modes
(high-confidence ratify / partial with caveats / explicit disagreement
flag) trigger sensibly — including the disagreement case, which is
the kowtow defense's working test.

### V-4. Trace-emit smoke test

Run I1 (forward-going piano roll) for 7 days against live
`:portfolio/step` evidence. Verify:

- Rows append at the expected cadence (one per scheduler tick).
- Cluster fans appear in real time when activity bursts occur.
- The bottom edge of the figure tracks the scheduler's
  `:next-tick-at` (i.e. "now" is visible).
- The trace is legible without the operator having to consult
  evidence records directly.

### V-5. Pass/fail summary

VERIFY passes when:

- V-1.1 + V-1.2 yield green lights (no taxonomy mismatches block I3).
- **V-2.4 (the methodology of choice)** demonstrates that vocabulary
  outputs move with substrate values across a real before/after fix:
  geometric quantities change in the expected direction, composite
  interpretations agree with underlying numbers, and the "what's
  next" candidate shifts.
- V-2.1 / V-2.2 / V-2.3 produce readings Joe finds sensible across
  at least one of the three corpora; the others may need vocabulary
  extension. (Combine with V-2.4 wherever a corpus item had a recent
  fix.)
- V-3 demonstrates the cross-check is doing the work it claims —
  particularly the disagreement-flagging mode.
- V-4 confirms I1's basic mechanic.

VERIFY fails (and DERIVE needs revision) when:

- The vocabulary in D-7.5 produces readings that read as
  hand-wavy / generic / non-distinguishing across very different
  positions.
- The cross-check ratifies everything (kowtow not actually defended).
- Substrate-2's edge taxonomy can't accommodate the head-write
  vocabulary in any reasonable extension.

### Checkpoint 1 - 2026-05-01: V-2.4 stepper trial against the M-reachable-from-boot trajectory

**Trajectory under test** (5 sequential futon3c commits, all 2026-05-01 ~19:50–19:51):

| commit | mission | structural role |
|---|---|---|
| 557efe4 | M-invariant-queue-unstuck | install queue meta-apparatus (boundary, per-turn, ratchet, canary) |
| bac7422 | M-invariant-queue-extend Track 4.1 + tracer reframe | extend apparatus (load-time check, 6 tracks → typed pipeline-tracer evidence) |
| eb02d6d | M-archaeology-control + M-bounded-disposition | introduce subsumption-witness + bounded-disposition shapes (6 sibling invariants) |
| 314ca64 | M-single-locus | introduce cardinality-1 shape (3 sibling invariants) |
| 18b5336 | M-reachable-from-boot **STOP-THE-LINE hot-fix** | open family `layered-error-hierarchy` (I0) outside normal queue cadence after HUD widget showed STUCK |

#### Setup probe — substrate-2 freshness gap (V-1 finding, corrected 2026-05-01)

Initial query (`curl localhost:7071/api/alpha/hyperedges?type=:commit`)
returned empty, which I read as "commit-vertices not indexed for
futon3c." That was a query-syntax error and a hasty conclusion —
the actual type-name is `code/v05/commit`, and substrate-2 does
have commit-vertices for futon3c (598 indexed when re-checked).

The real shape of the gap, after re-probing:

- **Capability**: present. `futon3/scripts/ingest_commits_to_futon1a.clj`
  walks `git log --reverse` and emits `code/v05/commit` vertices +
  `:authored`, `:precedes`, `:edits` edges into futon1a XTDB.
- **Operational**: lagging. The script is **manually triggered**
  (`bb ingest_commits_to_futon1a.clj <repo-root> --label <tag>`).
  No cron, no watcher integration. Latest-indexed commit per repo
  ranges from 4 to 19 days behind today (sampled 2026-05-01).
  futon3c's most recent indexed commit is 2026-04-25 — the
  trajectory commits (2026-05-01) are 6 days past that cliff and
  are therefore invisible to substrate-2.
- **Why this matters here**: V-2.4's mechanical lower bound
  (compute T at `(vertex, commit)` pairs to verify vocabulary
  outputs against substrate values) requires the test-target
  commits to be in substrate-2. They aren't, until ingestion
  catches up.

This is the same anti-pattern M-reachable-from-boot opened a
STOP-THE-LINE family for in `futon3c` 2026-05-01: long-lived state
populated only at operator-driven activation time. Substrate-2's
commit ingestion is a cross-process sibling of that family that
wasn't in M-reachable-from-boot's INSTANTIATE scope.

The closure path is logged as
`futon3/holes/excursions/E-live-means-live.md` (with a Checkpoint
back-pointer at the end of `M-live-geometric-stack.md`). Three
remediation paths there: (1) cron the ingest script for an
immediate patch, (2) watcher-integrate commit ingestion as the
right structural fix, (3) open a sibling under
`reachable-from-boot/` so the freshness gap can't drift silently
again. The trial below proceeds **by-hand on prose evidence**
(commit messages + mission frontmatter + file changes) since
mechanical-mode V-2.4 is blocked until at least (1) lands.

**V-1 pre-condition refined**: not "does substrate-2 cover this
futon" (yes, all 12 watched), but **"is substrate-2's commit-vertex
coverage current as of the commits being tested"** (currently no,
for anything ≤ ~6 days old; tracked under E-live-means-live).

#### Stepper readings (by hand using D-7.5 starter vocabulary)

**State before 557efe4** (HEAD~5):
- T at vertex `evidence-write-surface` is *high*: ~50 parallel write paths through `estore/append*` with no routing authority.
- T at vertex `invariant-apparatus` is *high*: queue meta-apparatus designed (M-invariant-queue-unstuck at MAP→DERIVE) but not embodied.
- |∇T| is *sharp* at the boundary between aspirational and operational invariant families.
- **Composite reading**: *"the invariant queue's apparatus exists in design but not in code; pressure is concentrated at the write-surface where ~50 parallel paths converge with no boundary discipline."*
- **What's next**: top candidate `:propose-binding (build apparatus)` — high pragmatic + epistemic EFE.

**After 557efe4** (queue meta-infrastructure shipped):
- T at `evidence-write-surface` drops sharply (single boundary now via `evidence.boundary/append!`).
- T at `invariant-apparatus` drops (4 operational invariants live).
- |∇T| smooths at the apparatus boundary.
- **Composite reading**: *"queue apparatus now operational; pressure dissipates at the write-surface; pressure shifts to adjacent vertices where check-coverage is incomplete."*
- **What's next**: 4.1 ratchet load-time wiring, OR remaining tracks 4.2/4.3.
- **Differential check**: T dropped where expected ✓; "what's next" shifted to adjacent vertices ✓.

**After bac7422** (4.1 ratchet load-time + tracer reframe):
- T at `ratchet-load-time` drops (boot-time check fires via `ratchet/check-on-load!`).
- *Substantively new structural move*: 4 remaining tracks lifted from prose TODOs into typed `:pipeline-tracer-item` evidence. Pressure that was *implicit in markdown* becomes *explicit in the evidence store*.
- **Composite reading**: *"ratchet now boot-validated; remaining tracks lifted from prose into typed pipeline-tracer evidence — pressure now distributed across 6 tracers rather than concentrated in unstructured intent."*
- **What's next**: any of the 6 tracers (4.2 snapshot-as-evidence, 4.3 arxana-view-columns, 1 substrate-2 lift, 2 war-machine AIF lift, 5 VSATARCS, 3 write-class scoping).
- **Differential check**: pressure-redistribution captured ✓; the prose-to-typed lift is something the vocabulary should name as a structural move (it's a kind of *unfolding* — implicit becomes explicit).

**After eb02d6d** (subsumption-witness + bounded-disposition shapes):
- T at `unrecognized-shape-space` drops; the queue's shape vocabulary expanded from forbidden-pattern to forbidden-pattern + subsumption-witness + bounded-disposition.
- 6 new sibling invariants live in family `archaeology-control`.
- **Composite reading**: *"invariant shape vocabulary expanding along the witness/disposition axis; previously narrow candidates (recover-or-drop, stash-debt-bounded) absorbed into broader bounded-disposition shape — generalization, not addition."*
- **What's next**: more shapes on as-yet-unexplored axes.
- **Differential check**: the *generalization vs. addition* distinction is a vocabulary refinement worth folding into D-7.5.

**After 314ca64** (single-locus, cardinality-1 shape):
- T at `cardinality-axis-unmodeled` drops; shape vocabulary now spans three axes (pattern, evidence, cardinality).
- 12-16 sibling invariants total across 4 shapes.
- **Composite reading**: *"shape vocabulary now spans three structural axes; the queue is generative — new shapes can be hypothesised by axis-introduction rather than only by candidate-promotion."*
- **What's next**: more shapes? More siblings? Or look outside the queue's catalog?
- **Differential check**: at this point the queue is the most operational it has ever been; the system's "what's next" should diversify and may include "stress-test the queue against external pressure."

**After 18b5336** (STOP-THE-LINE M-reachable-from-boot):
- Family `layered-error-hierarchy` opened at **I0** — foundation layer.
- Diagnosis revealed prior data loss ~2 weeks ago (Evidence Landscape lost ~2 weeks of data; pipeline-tracer registry empty at JVM restart) — *previously undetected*.
- Each of M-archaeology-control / M-bounded-disposition / M-single-locus was silently depending on `@!store` being durable and `family-check-fns` being repopulated. **Their live signal had been provisional all along.**
- **Composite reading**: *"the queue's expansion at I3 was structurally sound but silently depending on I0 invariants that hadn't been verified. An external incident (HUD STUCK) exposed pressure outside the queue's own catalog. The queue's expansion was good but insufficient — some invariants come from incident-pressure, not from within the queue's expanding shape vocabulary. This is itself a structural correction to the queue's self-image."*
- **What's next**: complete the four reachable-from-boot siblings; then audit other I0/I1 invariants for similar silent-dependency anti-patterns.
- **Differential check**: this commit is **qualitatively different** from the four prior. Commits 1-4 were queue-self-extending; commit 5 is queue-reaching-outside-itself.

#### What the trial reveals about the vocabulary

1. **The vocabulary needs to distinguish queue-self-extending from queue-reaching-outside-itself.** This is not just "another invariant family added" — it is a phase transition. D-7.5's starter primitives (T, ∇T, ΔT, drift) capture local structural state but don't natively express *trajectory phase*. A vocabulary refinement: introduce a *catalog-coverage* primitive — the fraction of recent invariant work that originated from within the queue's pattern-vocabulary vs. from external incidents. Commits 1-4 score high on catalog-coverage; commit 5 scores zero.

2. **"Generalization vs addition" is a vocabulary distinction worth naming.** eb02d6d absorbed narrow candidates into broader shapes — that's *generalization* of the structural model. This differs from *adding a new shape on a new axis* (314ca64) and from *adding a new family* (18b5336). The vocabulary should have words for these three structural moves.

3. **"Implicit-to-typed" lift is a recurring structural move.** bac7422's tracer-reframe and 18b5336's reachable-from-boot both turn previously-trusted-but-unverified properties into typed-and-checked evidence. The vocabulary should name this move ("lift to typed witness" or similar) as it appears repeatedly.

4. **Substrate-2 commit-vertex coverage is the load-bearing gap for V-2.4 mechanical mode.** Without it, the trial is by-hand on prose evidence — which is rigorous enough to surface the structural narrative but doesn't run the mechanical lower bound (substrate values shifting in expected directions).

5. **The disagreement-flagging mode shows up implicitly.** A v0 vocabulary that read commit 5 as "another invariant family added" (same as commits 2-4) would be unfalsifiable. The reading above explicitly distinguishes commit 5 — that distinction is the kowtow defense's working test in this trial. **Pass.**

6. **Predictive validity has a meta-level finding.** After commit 4 (314ca64), an honest "what's next" prediction from within the queue's catalog would have suggested *more shapes on more axes*. Commit 5 (the STOP-THE-LINE) is an EXTERNAL CORRECTION to that prediction. The system getting this "wrong" (predicting catalog-extension when reality forced catalog-supersession) is itself signal: there's a class of "what's next" the queue cannot predict from within its own pattern vocabulary. **The vocabulary needs a way to flag "I am operating within my catalog; external incidents may correct this"** — i.e. an epistemic-humility primitive.

#### Pass/fail verdict

- **Vocabulary captures local structural state**: PASS (with the V-1 caveat that mechanical quantity verification is deferred).
- **Vocabulary distinguishes structurally-different commits**: PASS — commit 5 reads qualitatively different from commits 1-4.
- **Vocabulary flags its own epistemic limits**: NOT YET — needs catalog-coverage primitive added.
- **Differential narrative is operator-meaningful**: PASS — Joe's framing of "queue self-extending → queue reaching outside itself" is recoverable from the readings.

#### Follow-ups generated by this trial

- **D-7.5 vocabulary v1**: add primitives for *catalog-coverage*, *generalization vs addition vs supersession*, *implicit-to-typed lift*.
- **V-1 pre-condition**: substrate-2 commit-watcher must cover the target futon before V-2.4 mechanical mode can run.
- **VERIFY Checkpoint 2** (next): run the same stepper trial after D-7.5 v1 lands, with commit-vertex coverage in place. Compare hand-composed readings against auto-generated ones; agreement validates the vocabulary as deployable.
- **Open question for VERIFY**: should the system's "what's next" ever be allowed to say *"this question may be unanswerable from within my catalog; external incidents may correct"*? If yes, that's an epistemic-humility output mode worth designing in.

## 6. INSTANTIATE

(deferred)

## Acceptance checklist (2026-09-30)

- [ ] V-1 records a verified action-vocabulary mapping and accepted substrate-2 edge type for every head-write mutation.
- [ ] V-2.4 records a real fix whose before/after geometric quantities, interpretation, and top action move with the substrate values.
- [ ] At least one V-2 corpus produces geometric and AIF readings that Joe accepts as specific to the item read.
- [ ] Three V-3 cases exhibit sensible corroboration, including an explicit disagreement outcome.
- [ ] A seven-day V-4 trace shows scheduler-tick rows, live cluster fans, `:next-tick-at`, and a legible forward edge.
- [ ] INSTANTIATE lands the consent-gated scheduler-as-head bridge after the V-1 preconditions pass.
