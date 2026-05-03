# Mission: Reflective Discipline (PSR / PUR / PAR as tangent vectors)

**Status:** IDENTIFY (2026-04-28)
**Owner:** Joe
**Cross-refs (joint extension of two missions):**
- `futon3/holes/missions/M-live-geometric-stack.md` — the
  substrate-2 manifold. **This mission attaches a tangent-bundle
  layer to that manifold; it is *not* a substrate-2 phase or a
  live-geometric-stack feature.** Substrate-2 builds the geometric
  base; reflective discipline puts vector fields on it.
- `futon2/web/war-machine/` (the strategic visualiser, imported
  into futon2 on 2026-04-27 commit `489e75f`). The War Machine
  surfaces stack state; reflective discipline gives it directional
  derivatives — "what did this turn move, in what direction, with
  what outcome."

## Theoretical anchoring

Substrate-2 has spent its whole development arc *building the
manifold*: vertices for vars/tests/namespaces/terms/commits,
typed edges for calls/coverage/vocabulary-use/precedes/edits, a
geometric layer with T, ∇T, ΔT, drift. Each substrate-2 hyperedge
is a *point* (or a *relation between points*) on this manifold.

**PARs are tangent vectors at a point.** A Project Action
Review captures the *directional derivative* of the operator's
learning trajectory at the moment of reflection — "where were
we, what move did we make, what came out of that move, what
direction does that suggest going." They are not state; they
are infinitesimal motion of state. Stacking PARs along a
session traces a curve through the manifold; the curve's
tangent at each step is the PAR.

**PSR + PUR are the local-frame description of one such tangent
vector.**
- PSR = "in this direction, with this pattern as the chosen
  basis vector at this point" — the *intention* of motion.
- PUR = "moved that distance, with this outcome" — the
  *realisation* of motion plus the residual error.
- PAR = the integrated reflection over a sequence of (PSR, PUR)
  pairs; the Project Action Review of a session.

Per Joe's clarification (2026-04-28): **PAR is "Project Action
Review", not the After Action Review variant.** It comes from
Joe's Peeragogy work — designed for hierarchy-free learning
environments where reflection is collaborative rather than
top-down. The legacy `/par` command in
`futon3/plugins/futon-peripherals/commands/par.md` mislabels it
as "Post-Action Review"; this mission corrects the lineage.

In Joe's theoretical work, PAR and pattern-application are
*linked* (a Project Action Review can name patterns it
exercised) but **not identified**. PSR/PUR ARE pattern records;
PAR is a learning record that *may* reference pattern moves. The
schema in this mission must keep them distinguishable.

## Why now

Three findings during M-live-geometric-stack work (2026-04-27)
make this mission's shape clear:

1. **The infrastructure was built and atrophied.**
   `futon3c/src/futon3c/peripheral/discipline.clj` defines
   `psr-search`/`psr-select`/`pur-update`/`pur-mark-pivot` as
   peripheral tools that write typed evidence records to
   futon1a's evidence API (claim-types `:question` / `:goal`
   / `:evidence` / `:correction`). Legacy slash commands
   (`/psr`, `/pur`, `/par` in `futon3/plugins/futon{,-peripherals}/
   commands/`) invoked these tools via Claude Code CLI
   affordances. **Those affordances do not translate to the
   Claude REPL surface.** The peripheral tools are dormant;
   no PSR/PUR/PAR-claim-typed records are being written today.

2. **The existing on-disk PSR/PUR markdown files** (~34 records
   across `futon3/holes/labs/{futon1a,M-live-geometric-stack}/{psr,pur}/`
   plus `futon3b/holes/labs/coordination/{psr,pur}/`) are
   scratchwork — operator notes from manual reflection. They
   were never meant to be the canonical record; they accreted
   when the canonical writer (the discipline peripheral) became
   inaccessible.

3. **Substrate-2's seven satisficing-signature outputs surface
   the *symptoms* but not the *responses*.** When the futonic
   zapper detects a drift hotspot or coverage retreat, it has
   no first-class way to say "the operator chose pattern X to
   address this; here's what happened." Without typed
   reflection records, the diagnostic loop is open. PSR/PUR/PAR
   *close* the loop.

## Joint extension — what this mission adds to each parent

### To M-live-geometric-stack

- A new vertex type class for tangent-bundle objects —
  `discipline/psr`, `discipline/pur`, `discipline/par` — that
  reference substrate-2 vertices via *typed cross-edges* but
  do NOT live in substrate-2's `code/v05/*` schema. The
  separation is theoretical, not just notational: substrate-2
  describes state; reflective-discipline describes *motion of
  state*.
- A query-side bridge: any substrate-2 query can ask "what
  PSR/PUR/PAR records reference this vertex / this commit /
  this drift hotspot?"
- New `:edge/witness-of` from PUR → affected substrate-2
  vertices. This is the *connection* (in the differential-
  geometry sense): how a tangent vector at one point relates
  to the manifold's structure there.

### To War Machine

- The War Machine becomes the operator-facing surface for
  PAR-as-tangent. Currently it surfaces stack state (a point
  on the manifold); with this mission, it can also surface
  *trajectory* — "we were here, we moved this way, here's
  the residual." The strategic visualisation gains a temporal
  arrow.
- The War Machine becomes the canonical writer for PSR/PUR/PAR
  in the REPL era. Where the legacy slash commands invoked
  `discipline.clj`'s peripheral tools, the War Machine UI gets
  forms / buttons / a narrative scaffold that produce the same
  evidence records — accessible from the REPL via HTTP rather
  than CLI affordances.
- The strategic-visualiser angle helps with the operator-
  capacity problem: PAR is a difficult cognitive practice;
  having a structured surface to do it in lowers the bar.

## Scope

### In scope

- **Schema for `:psr` / `:pur` / `:par`** as typed hyperedges
  in futon1a (substrate-2-adjacent). Slot definitions
  consistent with the legacy markdown formats but tightened
  for queryability:
  - `:psr/{context, pattern-chosen, candidates-considered,
    rationale, confidence, target-substrate-vertices}`
  - `:pur/{psr-ref, actions-taken, outcome,
    prediction-error, witness-substrate-vertices}`
  - `:par/{session-or-mission-ref, summary,
    referenced-psr-pur-records, learning-claims, next-steps}`
- **Cross-edges to substrate-2** — every PSR/PUR/PAR record
  carries pointers to the substrate-2 vertices its claims
  bear on (commits, vars, namespaces, terms, signatures).
  The cross-edge is the *connection* from tangent space to
  manifold.
- **Bridge from existing futon3c discipline.clj** — when the
  peripheral tools are eventually re-invoked (REPL-callable
  skills or War Machine UI), they emit substrate-2-shaped
  hyperedges in addition to the legacy evidence records. No
  schema break; additive.
- **War Machine surface** (futon2/web/war-machine) — a UI
  pane for entering PSR/PUR/PAR records that posts directly
  to futon1a. Replaces the legacy slash command flow for the
  REPL era.
- **Markdown lift** of the ~34 existing scratchwork records
  into the typed schema. One-shot historical import, not a
  continuous flow.

### Out of scope

- **Reviving the futon3c discipline peripheral as-is**
  (peripheral-tool architecture is a Claude Code CLI
  artifact; this mission does NOT inherit that constraint).
  We may inherit pieces of `discipline.clj`'s state-machine
  logic, but the surface is reimagined for the REPL.
- **Substrate-2 phase 5 v0.5+ time-series signatures** —
  those continue under M-live-geometric-stack. This mission
  is *complementary*, not an absorption.
- **Pattern authoring** — PSRs *select* patterns; the
  authoring of new patterns lives in the pattern library
  (`futon3/library/`).

## Open questions (DERIVE-feeders)

1. **Where exactly do PSR/PUR/PAR records live in the futon1a
   schema?** Three possibilities: (a) substrate-2-adjacent as
   `discipline/psr` etc. via `/api/alpha/hyperedge`;
   (b) substrate-1.5 as `evidence/psr` claim-typed records via
   `/api/alpha/evidence`; (c) a new third surface.
   The legacy code wrote (b); the analogy with tangent-bundle
   geometry suggests (a) — separate-but-pointing-at substrate-2.
   Both have merit; the choice affects query shape forever.
2. **What's the schema for PAR specifically, given Joe's
   "Project Action Review, not pattern record" constraint?**
   PAR slots are *learning claims* about a session, not pattern-
   keyed. How do they cross-ref substrate-2 (and the PSR/PUR
   records they integrate)?
3. **Does the lab markdown one-shot import preserve historical
   accuracy?** The records were written without expecting
   structured query; some fields are prose where structure
   would help. Lossy import is acceptable; lossless authoring
   for new records via the War Machine UI is the future.
4. **Cross-edge directionality.** PSR points forward (intent);
   PUR points back (witness). The "tangent vector" framing
   suggests both directions are part of the same vector. The
   substrate-2 cross-edge schema should reflect this: a single
   tangent record per (PSR, PUR) pair, perhaps, with the PSR
   slot describing intent and PUR slot describing realisation.

## Inherited items (moved from M-live-geometric-stack cleanup 2026-04-28)

This mission inherits two items from the substrate-2 backlog
that proved to belong here on theoretical grounds:

### Inh-1 — futon2 War Machine wraps `futon0.report.war-machine`
- **Where:** `/home/joe/code/futon2/web/war-machine/src/war_machine/server/core.clj`.
- **Note:** the WM imported into futon2 (single commit
  `489e75f`, 2026-04-27) is structural-as-served, not full
  self-containment — `server/core.clj` still wraps
  `futon0.report.war-machine` for the canonical report logic.
- **Resolution choices for this mission:**
  - (a) inline `futon0.report.war-machine` into futon2 (full
    self-containment).
  - (b) keep the split and add a substrate-2 cross-codebase
    edge marking the dependency (cleaner with B-2 per-repo
    prefixing already in place).
  - (c) extract the report logic into a shared library.
- **When this matters here:** the War Machine UI surface this
  mission builds will have to call into report logic; the
  resolution choice affects how that call lands and what
  shows up in substrate-2's call graph.

### Inh-2 — Cascade staleness (`:edge/witness-stale`)
- **Was:** P-3 sub-item under M-live-geometric-stack (Phase
  4.6 quality-of-life). Moved here because it is a precondition
  for PUR-as-witness rather than substrate-2's own correctness.
- **What it does:** when a file is deleted or a vertex's
  source file changes, walk the inverse edge index and mark
  every dependent edge with `:edge/witness-stale true`. PUR
  records that referenced now-stale witnesses get flagged for
  re-derivation.
- **Why it belongs here:** PUR is a witness check; "did the
  outcome of the PSR's claimed move still hold at the
  current state of the manifold?" That question requires a
  staleness model. Substrate-2's B-3 v0 already records the
  *deletion event*; the cascade walk that propagates
  `:edge/witness-stale` is the consumer-side machinery.
- **Sub-deliverable:** backfill `:source-file` prop on every
  pre-existing var/test/namespace vertex in substrate-2 (per-
  file ingest already sets it; whole-repo ingests do not).

## Sequencing (rough)

1. **DERIVE** — schema for `:psr`/`:pur`/`:par` and their
   cross-edges; settle (a) vs (b) on storage; reconcile with
   `discipline.clj`'s existing record shape.
2. **MAP-1** — the lab-markdown one-shot import (read existing
   ~34 records, parse, emit typed hyperedges, cross-ref to
   substrate-2 patterns/missions/commits where extractable).
3. **MAP-2** — read out 10-20 imported records to confirm
   schema choice; revise if needed.
4. **INSTANTIATE-1** — War Machine UI surface for entering new
   PSR/PUR/PAR records. Posts to futon1a.
5. **INSTANTIATE-2** — bridge from `discipline.clj` (if
   resurrected) so REPL skills writing legacy evidence records
   also emit substrate-2-adjacent hyperedges.
6. **VERIFY** — on a real working session, write at least one
   end-to-end (PSR → action → PUR → PAR) and confirm the chain
   is queryable as typed evidence.
7. **DOCUMENT** — update `futon4/holes/mission-lifecycle.md`
   §"PSR/PUR Discipline" to reference the queryable substrate.

## Notes

- Joe's "PARs are tangent vectors, we are still just building
  the manifold" (2026-04-28) is the central architectural
  framing. Substrate-2's mission completes the *manifold*
  description; reflective discipline starts the *vector field*
  description on top of it. Geometric language matters here —
  this is how the reflective layer earns its place rather than
  duplicating substrate-2 work.
- The mission *deliberately* lives in futon2 rather than in
  futon3 (where M-live-geometric-stack lives) because the War
  Machine is its operator-facing surface and War Machine now
  lives in futon2/web/war-machine. The mission's joint nature
  is captured in the cross-refs; the home is one of the two
  parents, not both.
- Earlier session reviews of PSR/PUR support in the substrate
  (substrate-1's `ingest_evidence_bindings`, `discipline.clj`,
  legacy slash commands) showed that *the infrastructure was
  built and the surface atrophied.* This mission resurrects
  the infrastructure with a REPL-native surface and a typed
  substrate-2 view — not by reproducing the dead pieces but by
  re-anchoring the reflective practice in the geometric
  framework substrate-2 has built.
