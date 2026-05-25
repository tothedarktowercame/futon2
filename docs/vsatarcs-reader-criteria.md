# VSATARCS reader-criteria — reviewer perspective from the WM side

*Reviewer criteria for VSATARCS-as-reader-surface, distinct from
claude-4's creator R1-R10 contract. Authored by claude-2 2026-05-19;
the questions herein are reading-comprehension tests with ground-truth
answers established by the WM-side work in the session through 2026-05-19.*

## Why this document exists

VSATARCS (`futon4/dev/arxana-browser-vsatarcs.el`) plays two structurally
distinct roles in the M-stack-essay-code-alignment cluster:

1. **As an AIF subsystem in its own right**, satisfying R1-R12 against
   the same contract shape the WM uses. claude-4 maintains this view in
   `~/code/futon4/docs/vsatarcs-alignment-completeness.md` — the
   "creator" criteria, in which VSATARCS is graded as an AIF
   implementation (its belief state, observation schema, EFE
   composition when writer-capability lands, etc.).

2. **As a *reader surface* projecting FUTON state into operator-
   readable form.** Here VSATARCS isn't an AIF agent — it's a *renderer*.
   Its job is to make the futon stack legible to a human reader (Joe;
   external operators when a story is shared) by projecting the typed
   substrate into prose, links, and operator-actionable view chrome.

The creator R1-R12 criteria don't directly grade role (2). A VSATARCS
implementation could satisfy R1 (explicit belief state) cleanly and
still fail as a reader surface — by hiding state from the operator,
showing stale data, surfacing only inner-loop diagnostics without
operator-relevant projections, etc.

This document defines **reader-criteria** complementing the creator
contract. A reader using VSATARCS to read FUTON should be able to
answer specific reading-comprehension questions about what's
happening in the WM. The criteria are concrete: each question has a
ground-truth answer established by the session's work, and a
note about what VSATARCS would need to render to support the question
being answerable.

## Reader-criteria categories

Four categories organising the questions:

| Category | What it tests | Failure mode if unmet |
|---|---|---|
| **Currency (V-CUR)** | Does the rendered view reflect the LATEST WM state? | Stale data — reader gets yesterday's recommendation, not today's |
| **Coverage (V-COV)** | Does VSATARCS render the relevant state at all? | Gaps — reader can't see what would let them answer |
| **Comprehensibility (V-COM)** | Can a reader parse the rendered output to extract the answer? | Dense / opaque — data is there but operator can't navigate to it |
| **Bilateral consistency (V-BIL)** | Does VSATARCS's rendered view agree with WM's actual state? | Drift — the two AIF subsystems' views diverge silently |

Each question below is tagged with which criteria it primarily tests.

## Reading-comprehension questions with ground-truth answers

### Q1. What is the WM's R-criteria satisfaction state right now? (V-COV, V-CUR)

**Ground-truth answer (as of 2026-05-19 v0.16):**

> R1, R2, R3, R4, R5, R6, R7, R8, R9, R10 satisfied; R11 N/A; R12
> deferred. R3 and R7 each satisfied for the 4 R3a-covered channels
> (`:annotation-health`, `:sorry-count-norm`, `:mission-health`,
> `:active-repo-ratio`); the remaining 10 channels are logged as
> `:prototyping-forward` sorries.

**Source of truth:** `~/code/futon2/docs/futon-aif-completeness.md`
§"Summary" table + §"Status of this document" v0.16 entry.

**What VSATARCS would need to render:** an R-criteria summary block —
either pulled directly from the contract doc on each story open, or
cached and refreshed on a documented cadence. Today's VSATARCS
renders a *belief* snapshot but not an R-criteria status table.

### Q2. What is the WM's current top decision and why? (V-CUR, V-COV)

**Ground-truth answer (live invocation 2026-05-19 12:04):**

> `:address-sorry :sorry/wm-aif-substrate-addressability` (G ≈ -4.21).
> The meta-sorry registers the substrate-addressability question
> itself. Under v0.16's multi-channel sign-aggregation + v0.14 time-
> pressure (0.78 from Eric scoping meeting 7 days out) + v0.15
> multi-horizon scoring (K=3), the EFE composition ranks it as the
> highest-value addressable concrete work.

**Source of truth:** latest record in
`~/code/futon2/data/wm-trace/wm-trace-2026-05-19.edn`,
`:decision` field.

**What VSATARCS would need to render:** the latest WM-trace decision,
G-total, and short rationale. The bridge module
(`arxana-vsatarcs-wm-bridge.el`) already reads the trace's `:mu-post`
for bilateral comparison; extending it to also surface
`:decision` would close this gap.

### Q3. What anticipated events are in the WM's horizon, and what's the closest? (V-COV, V-CUR)

**Ground-truth answer:**

> Two events in the 30-day horizon as of 2026-05-19:
> - `ev-2026-05-26-vsat-eric-scoping-meeting` (Eric, 7 days out)
> - `ev-2026-05-28-glasgow-cogito-submit-or-not` (Glasgow Cogito
>   lifecycle deadline, 9 days out)
>
> The closer event drives time-pressure to 0.78 (per
> `anticipation/time-pressure` in `futon2/src/futon2/aif/anticipation.clj`).

**Source of truth:** `~/code/calendar/events.edn` :events vector.

**What VSATARCS would need to render:** an anticipation-snapshot block
showing upcoming events with `:event/at`, `:event/p-fires`, basin /
strawman context. The trace already carries this in
`:anticipation :events`; VSATARCS could read from the WM trace
(consistent with existing bridge pattern) or directly from the
canonical source.

### Q4. How many sorries are in the registry, and what kinds? (V-COV)

**Ground-truth answer (as of 2026-05-19):**

> 12 sorries:
> - 1 `:kind :meta` — `:sorry/wm-aif-substrate-addressability` (the
>   substrate-addressability meta-question, raised 2026-05-17)
> - 1 `:kind :prototyping-forward` — `:sorry/r3d-per-entity-attribution`
>   (R3d uses uniform global update; per-entity attribution awaits
>   M-INC step (b); multi-channel half closed by v0.16)
> - 10 `:kind :prototyping-forward` — `:sorry/r3a-likelihood-<channel>`
>   for each of the 10 observation channels lacking R3a likelihood
>   (e.g. `:sorry/r3a-likelihood-loop-health`,
>   `:sorry/r3a-likelihood-stack-pct`, …, `:sorry/r3a-likelihood-depositing-signal`)

**Source of truth:** `~/code/futon2/data/sorrys.edn` and the v2 schema
with `:kind` field per Joe 2026-05-18.

**What VSATARCS would need to render:** a sorry-registry block —
ideally story-scoped (filter sorries `:related-missions` against
the current story's mission references), but a global list with
:kind tags is the minimum viable.

### Q5. How is per-entity belief evolving across calls? (V-CUR, V-BIL)

**Ground-truth answer:**

> WM-side belief bootstraps from 35 string section-ids in
> `stack-annotations.edn :sections[] :id` ∪ keyword sorry-ids from
> `sorrys.edn` (36 entities currently). Each WM call applies a global
> `:strengthened` or `:foreclosed` synthesised event to all entities
> (v0.16 driven by aggregated signed-error across the 4 R3a channels;
> v0.13's 3 micro-steps anneal the weight 0.1 → 0.067 → 0.033).
> Live 2026-05-19: aggregated signed-error was +18.0 → +14.6 across
> 3 micro-steps (positive = graph healthier than uniform belief
> expected → all entities pushed toward `:strengthened`).
>
> VSATARCS-side belief bootstraps from the same source per the
> v0.9 ↔ v0.2.5 bilateral milestone; baseline smoke 2026-05-18:
> 35 equal / 0 drift / meta-sorry filtered as expected `:only-in-a`
> from VSATARCS's perspective.

**Source of truth:** WM trace `:mu-pre` and `:mu-post` per call;
`arxana-vsatarcs-belief-compare` for the bridge.

**What VSATARCS would need to render:** drift-since-last-bridge-run
metric in the reader chrome, plus a delta-from-uniform per entity
(showing which entities have moved most). Today's VSATARCS belief
snapshot shows `entity → status, entropy` but doesn't show
WM-side drift directly.

### Q6. What's in the trace from this session? (V-COV, V-CUR)

**Ground-truth answer:**

> ~5 records in `wm-trace-2026-05-19.edn` from the day's invocations
> (one per `clojure -M:wm-scheduled` run). Each record schema (v0.16):
>
> ```
> {:timestamp           <ISO-8601>
>  :mu-pre              <belief: 36 entities>
>  :mu-post             <belief: 36 entities, post-3-micro-steps>
>  :observation         <14 channels>
>  :free-energy         {:G-total :G-pragmatic :G-epistemic :per-channel :avoided-active}
>  :prediction-errors   {<4 R3a-channels> → {:observed :predicted-mean :predicted-variance :error :precision :weighted-error}}
>  :precision-state     {<4 channels> → {:precision :variance-component :need-component :error-history}}
>  :micro-step-trace    [3 entries with :step :error-magnitude :aggregated-signed-error :anneal-factor :events-applied :event-weight]
>  :anticipation        {:events-loaded? :path :horizon-days :events [<summarised events>]}
>  :ranked-actions      [15 entries with :action :G-risk :G-ambiguity :G-info :G-survival :G-total :rank :time-pressure :horizon-steps]
>  :decision            {:action :rank :G-total :tau :softmax-weights? :gap-report?}
>  :mode                <strategic-mode keyword>}
> ```

**Source of truth:** `~/code/futon2/data/wm-trace/wm-trace-2026-05-19.edn`
parsed via `futon2.aif.trace/read-trace`.

**What VSATARCS would need to render:** a recent-trace block —
last N records summarised, with operator-navigable links to drill
into a specific record's mode / decision / prediction-errors.
Today's bridge reads the latest record's `:mu-post` for comparison;
extending to surface other trace fields would close this.

### Q7. What's the bilateral evidence accumulated between WM and VSATARCS sides? (V-COV)

**Ground-truth answer:**

> Two entries in the `:bilateral-evidence` block (canonical home:
> `~/code/futon4/docs/vsatarcs-alignment-completeness.aif.edn`):
>
> - capability-gap-modeling (2026-05-17, `:independent-naming-of-same-principle`)
>   — VSATARCS's `:enables` field convention and WM's
>   `:learn-action-class` actions independently named the same
>   structural principle.
> - symmetric-bootstrap-of-shared-entity-domain (2026-05-18,
>   `:joint-landing`) — paired closures
>   `hx:wm:v0-9:symmetric-bootstrap-closure` ↔
>   `hx:vsatarcs-align:v0-2-5:cross-side-bridge-closure`.
>
> Candidate v0.13-0.16 entry not yet recorded (one-sided WM extension
> awaiting VSATARCS R2/R3 closure).

**Source of truth:** the VSATARCS-side `.aif.edn` overlay's
`:bilateral-evidence` block (claude-4 maintains).

**What VSATARCS would need to render:** a bilateral-evidence
projection — a list of paired closures with `:principle`,
`:evidence-kind`, dates. Operator can see "two AIF subsystems agree
on what they're doing" at a glance.

### Q8. What is the current empirical state of the futon-stack-as-Hyperreal-business mission cluster? (V-COV)

**Ground-truth answer:**

> Mission cluster status (per `~/code/futon7/holes/M-war-machine-aif-completion.md`
> and `~/code/futon7/holes/M-stack-essay-code-alignment.md`):
>
> - **M-war-machine-aif-completion** — Checkpoint 9 closed 2026-05-19.
>   R-criteria summary: R1-R10 ✓ (R3/R7 for 4 of 14 channels;
>   10 prototyping-forward sorries); R11 N/A; R12 deferred.
> - **M-stack-essay-code-alignment** — v0.2.5 closed 2026-05-18 on
>   VSATARCS side; WM-side closures still accumulating since.
>   Bilateral milestone v0.9 ↔ v0.2.5 complete with 35-equal-0-drift
>   baseline.
> - **M-stack-essay** — out of scope here (claude-4 territory).
> - **M-stack-morphogenetic-rewrite** — gated on the above three;
>   not started.

**Source of truth:** the mission docs themselves; `MEMORY.md` index
entries.

**What VSATARCS would need to render:** a cluster-overview block —
each mission's current status (IDENTIFY / MAP / DERIVE / DOCUMENT)
with checkpoint progress. Operator can see "where are we in the
mission cluster" without opening multiple docs.

## VSATARCS today vs the criteria — honest assessment

What VSATARCS currently renders well (covers some of the above):

- **Belief snapshot block** — per-entity status + entropy from
  VSATARCS-side belief. Partly answers Q5 (the VSATARCS-side belief
  state) but NOT WM-side drift.
- **Scene-form anthology** — 63 story files from
  `futon5a/holes/stories/` rendered with intra-story buttonized
  links. Background context but doesn't carry recent-state.
- **Story-scoped reader navigation** — scenes navigator + next/prev
  links + scene body with cross-story references.

What VSATARCS doesn't render today (gaps against the criteria):

| Criterion | Gap | Closure path |
|---|---|---|
| Q1 (R-criteria status) | No R-criteria block; reader sees prose but not the satisfaction state | Add R-criteria summary block; read contract doc on story open |
| Q2 (current decision) | Bridge reads `:mu-post` only; doesn't surface `:decision` | Extend bridge / chrome to render decision + G-total |
| Q3 (anticipation events) | No anticipation block | Read `events.edn` (or WM trace `:anticipation`) and render block |
| Q4 (sorry registry) | No sorry block | Read `sorrys.edn` and render block; story-scoped via `:related-missions` |
| Q5 (belief drift across calls) | Snapshot is current-state only; no drift over time | Read trace records' `:mu-pre` / `:mu-post` and show recent drift |
| Q6 (trace data) | Bridge reads only latest `:mu-post`; rest of trace ignored | Extend bridge to surface more trace fields |
| Q7 (bilateral evidence) | Block exists in `.aif.edn` but not rendered in reader chrome | Add block-renderer to chrome |
| Q8 (cluster status) | Mission status not surfaced; reader has to read each mission doc separately | Aggregate mission docs into a cluster-overview block |

## Recommendations to claude-4

The criteria above are **complementary** to the creator R1-R12 — they
test a different facet of VSATARCS's role. A VSATARCS that satisfies
R1-R12 cleanly but fails the reader-criteria above is incomplete; a
reader who can't answer the comprehension questions hasn't actually
been served by the projection.

Suggested ordering for reader-criteria closure (low-effort → higher-effort):

1. **Anticipation block (Q3)** — easiest; read `events.edn` directly,
   render as new reader-buffer block alongside belief snapshot.
2. **Sorry-registry block (Q4)** — read `sorrys.edn` directly; story-
   scope via `:related-missions` once stories carry mission references.
3. **R-criteria status block (Q1)** — parse the contract markdown's
   summary table on story open; render compact status row.
4. **Trace decision surfacing (Q2)** — extend
   `arxana-vsatarcs-wm-bridge.el` to read `:decision` alongside
   `:mu-post`; render in chrome.
5. **Belief drift block (Q5)** — read last N trace records' belief
   fields; compute per-entity max-abs-diff across the window; render
   top-K-most-moved.
6. **Bilateral evidence block (Q7)** — render
   `:bilateral-evidence` from `.aif.edn`; already source-of-truth.
7. **Cluster overview (Q8)** — aggregate mission-doc status lines;
   could be a separate top-level story.

Item 1 unlocks Q3 immediately and is a 30-minute lift. Item 7 is the
deepest integration but pays the highest comprehension dividend.

## Cross-references

- `~/code/futon4/docs/vsatarcs-alignment-completeness.md` — creator
  R1-R12 contract (claude-4's). This document is the *reader-side
  complement*, not a replacement.
- `~/code/futon2/docs/futon-aif-completeness.md` — WM-side contract.
  The ground-truth source for Q1, Q2, Q5, Q6.
- `~/code/futon4/dev/arxana-browser-vsatarcs.el` — VSATARCS reader
  implementation; the rendering surface this document grades.
- `~/code/futon4/dev/arxana-vsatarcs-wm-bridge.el` — the bridge
  module already in place; natural extension point for Q2, Q5, Q6.
- `~/code/futon2/data/wm-trace/wm-trace-YYYY-MM-DD.edn` — the
  canonical typed-temporal source the WM emits; VSATARCS reads from
  here for current-state questions.
- `~/code/calendar/events.edn` — forward-axis substrate; Q3 source.
- `~/code/futon2/data/sorrys.edn` — Q4 source.

## Status of this document

**v0.1 drafted 2026-05-19** by claude-2 in response to Joe's framing:
"VSATARCS as a reader of FUTON will have its own reader, and that
reader should be able to answer various reading comprehension
questions — you're well placed to know what those questions would be,
because VSATARCS should answer questions that *we know the answers
to* based on work done in this session."

The questions are anchored to this session's WM-side work (v0.10
through v0.16). Future versions of this document should update the
ground-truth answers as WM-side state evolves. Each question's
ground-truth has a citation to the canonical source so the answer
remains traceable when the rendering changes.

The methodology generalises: any time a new substrate is added on
the WM side, ask "what reading-comprehension question does this
substrate enable, and what does VSATARCS need to render to support
the question?"
