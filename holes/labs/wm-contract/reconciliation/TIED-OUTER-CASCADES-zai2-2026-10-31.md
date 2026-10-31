# TIED-OUTER-CASCADES — discovery report (zai-2, 2026-10-31)

Row: `TIED-OUTER-CASCADES` (B3, discovery only). Trigger: run `adc46631`
selected `:what-others-wait-on-first` from three cascades tied at
3.3781368587563056. Joe's rule: a tie means insufficient sagacity, an
inference defect; find the discriminating evidence, never a tie-break.

Result: the equality is structural, not sagacity-limited competition. Each
cascade's model collapsed to the *same stopped kernel*, so the three scores
are bit-identical because the scoring inputs are bit-identical. The
discriminating evidence already exists in the machine's own retained record;
it is not routed into the score. The machine's own `:tie?` flag already
classifies this as **not a choice tie** (`:tie? false`).

## Why the three scores are exactly equal

`outer-score` depends on a cascade only through `cascade-pick` (the first
surviving candidate). Two independent degeneracies made all three picks score
identically in `adc46631`:

1. **Cascades 1 and 2 picked the same task, and that task's picked kernel
   degenerated to the stopped kernel.** Both `:what-others-wait-on-first`
   (`[:ready-gate :by-cone :by-unblocks]`) and `:only-what-can-start-now`
   (`[:ready-gate]`) left `M-peradam-grounding` first (verified by replay,
   below). But at scoring time `ending-class` returned nil for it, so
   `landing = [:stop-the-line :none]` and `picked-kernel` produces
   `doable == blocked == stopped` rows. The record proves this directly: the
   retained `:prediction :outcomes` puts mass 1.0 on `[:stop-the-line :none]`
   and 0.0 on all fifteen other outcomes. With both kernel rows equal, the
   Fisher-information novelty is exactly 0.0, and push/risk/ambiguity equal
   the no-pick model's, regardless of `q`, `reach`, or gain class.
2. **Cascade 3 picked nothing.** `find-out-before-building` gates on
   `{:gate-status :undetermined}`. `known-term-field` excluded *all 47*
   `:undetermined` rows before scoring (`:graph-term-not-observed`, unknown
   `:cone`/`:unblocks`), so the gate emptied its field: `pick nil` → the
   stopped kernel again.

Same stopped kernel from three routes ⇒ identical doubles. A term not
contributing (ending-class/relation, gain, reach, novelty — all zeroed by the
degenerate landing) **and** a missing observation (graph terms for the whole
undetermined population) each removed a would-be discriminator.

## How often across today's P5 runs

Every retained tick run record of 2026-10-30/31 (10 of 10) carries
`:tied-cascades` with ≥2 entries; 7 of 10 are three-way, 3 of 10 two-way
(cascades 1–2). **`:tie? true` occurs in 0 of 10** — no run today ever had a
genuine choice tie (distinct picks sharing the minimum). In `ebbbe42a`
cascade 3 differed from the tied pair only at the 15th significant digit
(`…482234763` vs `…4822347636`) — floating summation order, not evidence.

| run | tied at | ways | `:tie?` |
|---|---|---|---|
| 3415b671, e5f08d91 (10-30) | 4.957490779577444 / 4.787491742782046 | 3 | false |
| 0ba3370c, 4955b6e6, f805ca4b, adc46631, 0be4fc7d | 3.780…/3.986…/4.313…/3.378…/3.324… | 3 | false |
| d5b9b513, e71c7380, ebbbe42a | 2.900…/2.905…/3.635… | 2 | false |

## The discriminating evidence the machine already holds

- **The relation itself.** The same record retains `:selected-relation
  :associated` for `M-peradam-grounding`. Replayed through the score, an
  `:associated` relation separates the models immediately: with today's
  state the pair scores 2.9163429608506295 while the stopped model scores
  3.3244797210536605 (a 0.408 gap). So the observation exists; at scoring
  time the relation function returned nil/absent while at retention time the
  same evaluation returned `:associated`. That divergence — not missing
  data — is the concrete inference defect behind the tie.
- **The undetermined population's graph terms.** The 47 exclusions are
  retained with `:kind :outer-score/graph-term-knowledge-request`
  (`producer :dependency-reader-reviewer`, `reason :prerequisite-unresolved`).
  Producing them gives cascade 3 a real pick and a distinct score.

## Smallest change (proposed, not implemented — discovery only)

1. **Retain per-cascade picks.** One line in `outer-select/select`: alongside
   `:cascade-scores`, emit the `cascade-pick` target (or `:no-pick`) per
   cascade. Every future `:tied-cascades` then reads as same-pick /
   degenerate-stopped / genuine without a replay. This is evidence
   retention, not a tie-break.
2. **Name the scoring-time relation.** Retain the relation value the score
   consumed (or route scoring through the same focus-receipt evaluation whose
   answer is kept as `:selected-relation`), so the nil-vs-`:associated`
   divergence above is visible in the next record instead of only in the
   kernel shape.

The sagacity repair itself is already requested elsewhere: produce the
undetermined graph terms (the retained knowledge-producer requests) and cover
the focus-receipt relation; with either in place the three scores separate on
real evidence.

## Reproduction (read-only; no canonical store touched)

Replayed in a `clojure -M -e` probe with `FUTON_WM_RUN_RECORD_DIR=/tmp/…` and
futon3c's src on the classpath (the Giraffe verifier lives there), against
the pinned field `storage/giraffe/20261031T185451.759Z-afdc9a60` (read-only)
and the pinned makeup sha `f3125835…`:

- known-term field: 552 admitted (501 `:ready`, 51 `:not-ready`), 47
  `:undetermined` excluded — cascade 3's gate empties, `pick nil`.
- `cascade-pick` per cascade: `:what-others-wait-on-first →
  "M-peradam-grounding"`, `:only-what-can-start-now → "M-peradam-grounding"`,
  `:find-out-before-building → nil`.
- With a relation marking the pick `:associated`, `select` returns
  `[2.9163429608506295 2.9163429608506295 3.3244797210536605]`,
  `:tie? false`, `:tied-cascades [:what-others-wait-on-first
  :only-what-can-start-now]` — the tie collapses to same-pick and cascade 3
  separates, exactly as predicted.
- Note: exact re-derivation of 3.378… would need the outer state *as of*
  scoring time; today's state has since absorbed later runs' learning, which
  is why the replayed stopped score is 3.324… (the value 0be4fc7d later
  recorded). The record's own `:prediction` is the pinned proof of the
  degenerate kernel at adc46631.

## Closure condition

Row condition: "Selection reaches a unique minimum on real evidence, or the
tie is explained." The tie is explained (mechanism, frequency, and separating
evidence all demonstrated from retained data). No live witness is owed for
the explanation; the two one-line retention changes above are the proposed
smallest follow-ups, owned by whoever takes the row forward.

## Implementation addendum (zai-2, 2026-10-31, same row)

**Correction to the discovery report above.** The ":selected-relation
:associated" I quoted for adc46631 belongs to the record's *learning*
section (the pending f74df9cc consumption). The `:outer/selection` map's own
`:selected-relation` is **`:unknown`** — there is no scoring-time vs
retention-time divergence. The scoring relation and the retained relation
agree: the relation class of the picked target was unknown. The cause is
upstream of the scorer: `focus-receipt/classify-target` returned
`{:class :unknown, :reason :embedding-node-not-retained}` for
M-peradam-grounding (stated relation absent, `:no-stated-path-to-a-classified-target`,
and the futon6 mission-embed corpus — pinned 2026-06-25 — does not contain
the newer missions). Measured over the pinned field at the record's decision
time: **501/501 ready targets classify :unknown**.

**Fix (at the cause, not a tie-break).** `known-term-field` now admits the
relation term on the same rule as the graph terms: a candidate whose relation
class is not `:focus`/`:associated`/`:useful-elsewhere` is excluded before
scoring with `:kind :outer-score/relation-unavailable` and a knowledge
request to `:focus-receipt-classify-target`, mirroring the graph-term
requests. A pick therefore always has a known ending class, so its kernel can
never silently degenerate to the stop-the-line kernel. `select` also retains
`:cascade-picks` (per-cascade pick target or `:no-pick`) beside
`:cascade-scores`, and an emptied field whose exclusions are relation-only
refuses `:outer-score/no-known-relation-members` instead of the graph-members
kind. `outer_score.clj` is untouched.

**Terms now contributing vs still zero.** With a relation-known pick the
landing is a valued outcome class (`[:related :several]` in the test), push
mass leaves `[:stop-the-line :none]` (0.625), risk/ambiguity see that mass,
and Fisher novelty is strictly positive (it was identically 0.0 in every
degenerate tie). Still zero by design: preference mass on
`:not-yet-evaluated` (c-kit 0; not terminal), and all non-stopped outcomes
for a gate-emptied cascade (spec'd no-pick fallback).

**Live consequence (owed witness).** On the current real field every ready
target classifies :unknown, so the next live click will refuse
`:outer-score/no-known-relation-members` with 501+ relation knowledge
requests naming `:focus-receipt-classify-target`. That refusal is the honest
surface Joe's rule demands; unblocking ordinary clicks needs the relation
evidence (refresh the futon6 mission embedding corpus or declare relations),
not a scoring workaround.

**Tests.** New namespace `futon2.aif.outer-relation-admission-test` (4
tests, 20 assertions): the adc46631 bad case (all-:unknown field from the
retained record's values) now refuses typed; the separating case (real
retained :associated for M-pattern-retrieval-calibration, as runs
d5b9b513/e71c7380 actually classified) picks it, retains picks, separates
scores and shows positive novelty; mixed-field admission and request
retention. The bad case was watched failing open on pre-fix code: three-way
score tie, live pick scored with `[:stop-the-line :none]` mass 1.0.
`reading-format-reselect-test`'s frozen-field expectations encoded the old
degenerate behaviour (first ready candidate with :unknown relation selected)
and were updated to the relation-known picks the fix yields
(M-pattern-retrieval-calibration, replacement
M-interim-director-proxy-metric-inventory) — 4 tests, 37 assertions green.
Pre-existing failures outside this row, named and left:
`outer-select-test`'s `real-giraffe-selection-demonstration` (pinned 2026-10-28
Giraffe run no longer verifies) and `outer-selection-wiring-test` (7
assertions, fixture targets M-selected/M-unknown absent from the live
registry; identical on origin/main), plus `m2l-outer-selection-ask-test`
(identical failure on origin/main).

## Addendum 2 (2026-10-31, follow-up requisition): the 501/501 claim is RETRACTED — probe artifact

Claude-1 challenged the "501/501 ready targets classify :unknown" claim. The
challenge is correct. The earlier probe ran without `resources/` on the
Clojure `:paths`, so `focus-receipt/read-inputs` returned `:absent` and every
target classified :unknown for want of any relation rows or embedding pins.
Re-run read-only with the resource on the classpath (`clojure -Sdeps '{:paths
["src" "resources" "/home/joe/code/futon3c/src"]}'`, `FUTON_WM_RUN_RECORD_DIR`
temp), real `classify-target`:

- Newest canonical field (`storage/giraffe/20261031T201627.802Z-2c37678e`,
  594 admitted targets): **121 :associated, 7 :focus, 466 :unknown**
  (reasons: 239 :relation-not-declared [mostly E- targets, no channel],
  100 :repair-target-absent, 79 :embedding-node-not-retained,
  38 :no-parent-relation, 10 :repaired-target-unknown).
- adc46631's own retained admitted list (552 targets): **103 :associated,
  5 :focus, 444 :unknown**.

So the next live click does NOT face a wholesale refusal; the fix's
`:outer-score/no-known-relation-members` refusal applies only to a field with
zero relation-known members, which the real field is not. The tie mechanism
itself stands unchanged: adc46631's picks were relation-unknown targets
(its selected `M-peradam-grounding` is :unknown :embedding-node-not-retained
— its stem is absent from the pinned 198-stem corpus), and every degenerate
kernel scored bit-identically.

**Why d5b9b513/e71c7380 retained `:selected-relation :associated`.** Their
selected target `M-pattern-retrieval-calibration` classifies :associated
through the **embedding-neighbour channel**: its stem IS in the pinned
2026-06-25 `mission-structure-embed` corpus, and the only relations rows
whose stems are also in that embedding (`M-action-cost-modelling`,
`M-aif4iad`, `M-futonzero-generative` — all "associated") supply the row.
No cosine floor is declared (`:floor {:absent :no-floor-declared}`), so even
cosine −0.014 (nearest neighbour `M-action-cost-modelling`) yields
:associated. The deciding input is the **pinned embedding corpus membership**
plus the row∩embedding intersection — nothing changed between 17:14Z and
18:54Z; the two runs simply picked a retained-stem target while adc46631's
degenerate menu picks landed on unretained-stem targets.

## Supply: what refreshes relation evidence

Chain (all read-only except the two writes named):

1. `futon6/scripts/diffsub_scope_dump.py` — bounded per-binder queries
   against the substrate-2 Reads service on **localhost:7071**; writes
   `futon6/data/diffsub-scopes.json`. No model calls; seconds. **This file is
   the stale link: mtime 2026-06-09, 198 missions.** The service is DOWN
   right now (7070/7072/7073 up, 7071 connection refused), so the refresh
   could not be exercised end-to-end this session.
2. `futon6/scripts/mission_structure_embed.py --out DIR` — deterministic
   feature-vector build (numpy, zero model calls; MiniLM text vectors are a
   read-only baseline). Rebuilt to a temp root in **0.25 s**. On the current
   (stale) scope census it reproduces exactly 198 stems, so the classify
   counts are unchanged (121/7/466): refreshing the embedding alone adds
   nothing; the scope census is the bottleneck, and only missions present in
   substrate-2 gain stems at all.
3. `futon2/resources/wm/focus/commit-facets-v1.json` pins both corpus files
   by sha256, re-hashed at read time. **Writing the canonical corpus without
   a same-commit re-pin breaks the next click** (`:embedding-pin-mismatch`),
   so a canonical refresh is one futon2 commit: corpus rebuild + resource
   re-pin, atomically. Between clicks, only temp-root rebuilds are safe.

## Honest interim: unknown-as-ignorance in outer-score

`outer_score.clj` can already express it: the endings vocabulary includes
`:not-yet-evaluated`. Mapping an unknown relation class to a
`:not-yet-evaluated` landing (instead of `ending-class`'s current nil →
`[:stop-the-line :none]`) yields a non-degenerate kernel — real ambiguity,
positive novelty, and `r + ambiguity − novelty` information gain that
separates unknown-relation picks from deliberate stops and from each other.
It is a one-clause policy change in `ending-class` plus tests, not a
structural change; it contradicts no current exclusion (candidates would
score rather than be excluded, so it is an alternative to 0513d1ea8's
exclusion, to be chosen by policy). Untouched here: outer_score.clj is
frozen by other rows this round.
