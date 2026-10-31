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
