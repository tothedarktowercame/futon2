# P5 criteria rollup — all ordinary clicks of 2026-10-31 — zai-6, 2026-10-31

Owner: zai-6, evidence lane, read-only. This is the one-page count Joe reads.
Eight ordinary clicks (P5 sequence), best status across runs per criterion,
with the run that shows it. Full matrices exist for 4955b6e6, ebbbe42a,
d5b9b513, adc46631, 0be4fc7d, f4b95abf; 0ba3370c and e71c7380 are covered
from their records and the plan checkpoints (no standalone matrix was
requested for them; their facts below are cited from the run records where
used). Revised 2026-10-31 late (zai-6) after click 8: A2 and C2 move out of
unknown, B3's limit is now disclosed in-record, B4 has a new landing shape,
and the counts change accordingly (what each change rests on is stated
per row; the matrix for f4b95abf carries the pins).

Runs (chronological): 4955b6e6 (already-satisfied, wants unmeasured) ·
0ba3370c (duplicate-guard refused, fallback :build-failed, injury a67d3e49) ·
ebbbe42a (:grounded-progress, 1/4 wants by checkbox) · e71c7380 (author
already-satisfied claim refused :claim-mission-text-not-an-artifact) ·
d5b9b513 (:grounded-progress, 0/6 wants, substance 1/6) · adc46631
(:incomplete :answer-reviewer-mismatch, injury c309d796) · 0be4fc7d
(:grounded-progress after revision round, revision door refused) ·
**f4b95abf (:guardrail-refusal :already-satisfied-wants-unmeasured — an
approved already-satisfied claim refused because measurement would not admit
mission text as artifact; first run with all of today's fixes loaded).**

Legend: **observed** = clean witness · **observed w/ limits** = witnessed but
a stated boundary remains · **not-exercised** = the run(s) never reached that
machinery (not a defect) · **unknown** = no run could establish it ·
**violated** = a run shows the criterion broken.

| Criterion | Best status today | Best witness run | Notes |
|---|---|---|---|
| A1 budget/credit | **observed** | all 8 | every issuance has a consumption line with authorization sha `326826ac`, caller claude-1 |
| A2 serving envelope | **observed w/ limits** ← was unknown | **f4b95abf** | rests on `:serving-envelope-v1` captured at run-start-before-author-dispatch: heap :measured (2 954/12 288 MiB, post-explicit-GC pool bean), agency queue + 50 seats :measured from live endpoints, admission-opts :measured (full config map). Limit: `:unknown-components` names four unmeasured things (provider quota/rate-limit, substrate JVM heap, os memory pressure, agency JVM heap) — disclosed, not closed |
| A3 effective injuries | **observed** (effective: none) | 0be4fc7d, f4b95abf | serving authority-aware read: none active all day (claude-1); two injuries minted (a67d3e49 ord 133, c309d796 ord 138) both medic-discharged; raw ledger still reads open — read effective, not raw |
| A4 pre-dispatch census | **observed** | 0be4fc7d | fully-populated effective-run-identity (target/manifest/policy/check-set/mission-source/base-head) |
| B1 population consistency | **observed w/ limits** | adc46631, 0be4fc7d, f4b95abf | clean 617/617, 618/618, 626/626 on the three post-refresh clicks (excursions 246→255 in an hour, all seen); earlier gap 610/611→610/614 closed by per-click re-mint, but equality holds only at selection time — drift is unending (Q1-GIRAFFE row, still open) |
| B2 failure retention | **observed** ← was w/ limits | **f4b95abf** | adc46631 retained the failure but not the cause (`:detail {:absent :no-error-data}`); f4b95abf retains the full detail: author-job, review-job (ids + result-summaries), refusal-reason, claim-citation, `:reviewed-claim-refused? true`. Both failure kinds of the day have receipts |
| B3 selection scoring | **observed w/ limits (limit now disclosed)** | f4b95abf | g-terms + normalization + accumulation retained every run; **f4b95abf adds `:cascade-pick-relations`** — per-pick `{:relation :associated, :relation-known? true}` ×2 and `{:no-pick, relation-known? nil}` ×1, so the TIED-OUTER-CASCADES limit (outer score cannot separate unknown-relation picks) is now stated in the record instead of silent. Limit itself stands: no recomputation, flat tie block still absent |
| B4 frozen prediction | **observed w/ limits** | adc46631, f4b95abf | landed correctly once (adc46631, stop-the-line/none, run stopped); wrong on all three other terminal classes — and f4b95abf brings the first non-degenerate distribution (`[:related :several] 0.375`) yet still cannot score the actual `:refused` terminal. The frozen outcome set has never contained a terminal class that occurred |
| C1 construction identity | **observed** | d5b9b513, 0be4fc7d, f4b95abf | patch/requisition/digest chain complete and `:claim-matches? true`; ebbbe42a's decisions-mismatch not reproduced after it |
| C2 durable checkpoint | **observed w/ limits** ← was unknown | **f4b95abf** (evidence row by kimi-2) | rests on kimi-2's C2-CHECKPOINT-CHRONOLOGY row (stated-evidence closure): construction → `pre-author-mission-baseline.edn` → dispatch ordering retained in ebbbe42a, 0be4fc7d and now f4b95abf (machinery-167 attempt-002: 21:47:30.553 → baseline sha `0a30eacf…` → 21:47:32.006), chronology code-enforced (too-early/too-late/rebound refusals), receiver-verified tests 41/238 under a temp record dir. Limit: continuation reuse of a retained baseline still has no live witness (no `:deferred-author-job` in any record today) — that witness alone keeps the row open |
| C3 descriptor contract | **observed** | all grounded runs | full-length shas throughout (patch commit, source-digest, requisition, grounded commit; f4b95abf's claimed commit `25684350…` also full 40-hex) |
| C4 review limb | **observed** ← was w/ limits | **f4b95abf** | 0be4fc7d showed the full social limb (request-changes → revision → re-review); f4b95abf retains the **`:review-gate` map** (`{:required? false, :executed? true, :tool-events 7, :passed? true}`) that every earlier matrix flagged absent — and shows the intended shape: an *approving* review overridden when measurement refuses |
| D1 want measurement | **observed w/ limits** | ebbbe42a, d5b9b513, f4b95abf | the only instrument remains the mission checkbox (declaration form): ebbbe42a reached 1/4 by checkbox; d5b9b513's real work (1/6 in substance) measured false; f4b95abf refused both directions — mission-text claims rejected (`:claim-mission-text-not-an-artifact`) and per-want outcomes honestly `:refused`. Both failure modes of the checkbox-only instrument have now been caught; an artifact-reading instrument still does not exist |
| D2 executable checks + learning | **observed** | ebbbe42a, d5b9b513 | replayed test receipts (ebbbe42a 4fd00fa63; d5b9b513 6 tests, byte-identical rerun) + publication receipts; 0be4fc7d verified with supporting artifact; f4b95abf correctly wrote learning/scan sidecars but no grounded artifact (not-exercised) |
| D3 truthful classification | **observed** | all 8 | no run overclaimed: honest refusals, honest zero-want grounds, an approved claim refused rather than banked; standing naming tension on `:grounded-progress` (want-delta terminal field queued) |
| D4 retention completeness | **observed w/ limits** | f4b95abf | improved late-day (serving-envelope, pick-relations, review-gate, full failure detail, first 2 r20 rows `:agrees`); remaining gaps: outer-task-selection receipt not retained; authority-aware injury join absent; q6 focus `:focus-not-retained`; habit store retired; frontier retention phase-dependent; failure `:detail :omitted` elides 21 subfields |
| E1 ending receipt | **observed** | all 8 | both limbs exercised: success receipts (ebbbe42a/d5b9b513/0be4fc7d) and failure/guardrail receipts (adc46631, f4b95abf), each with terminal publication + q6 terminal |
| E2 predecessor visibility | **observed** | all after the first | every run from 0ba3370c on saw its predecessor's ending (q6 :predecessor :present) |
| E3 no repeat on unchanged inputs | **observed** | 0ba3370c (refusal limb), 0be4fc7d, f4b95abf (comparison limb) | duplicate-effective-run guard refused a real repeat (0ba3370c) before author contact; effective-run-comparison :no-duplicate with full identity (0be4fc7d, f4b95abf); adc46631 shows the degradation: comparison :not-reached after a failed run |
| E4 identical inputs across a pair | **violated** (early), avoided late | violated: 4955b6e6, ebbbe42a, d5b9b513 | three violations, each narrowest-of-day so far: same-input pairs with serving composition drift (4955b6e6), a refused-predecessor component (ebbbe42a), the run's own grounded commit moving base-head (d5b9b513). Not violated after: pairs differ by target (adc46631), deliberate field re-mint + base-head (0be4fc7d), or policy/manifest change with five intervening runs (f4b95abf vs d5b9b513) |

## The count (revised after click 8)

- **Observed clean (12):** A1, A3, A4, B2, C1, C3, C4, D2, D3, E1, E2, E3.
- **Observed with limits (7):** A2, B1, B3, B4, C2, D1, D4 — each with the
  stated boundary above; none blocks the next click.
- **Unknown (0)** — A2 and C2 left unknown this morning; both now have
  receipts (f4b95abf's `:serving-envelope`; kimi-2's chronology row + this
  run's baseline-before-dispatch) with named limits.
- **Violated (1):** E4 — early-day only; every late-day pair avoids it by
  motion (target change, deliberate re-mint, policy change), which is a
  workaround, not a mechanism.
- **Not-exercised** is per-run, not a day-status: by end of day every limb
  except continuation-reuse-of-a-retained-baseline (C2's open witness) and
  duplicate-after-a-failed-run has been exercised at least once.

## Statuses resting on record shape, not behaviour

Four rows today rest partly on reading a record shape rather than on
behaviour (full list in the f4b95abf matrix): **A2** (the envelope is a
measurement receipt; nothing has yet been refused or shaped by an envelope
value), **C2** (chronology is code-enforced and unit-tested but the
deferred/resume path has never run live), **B1** (Q1 pass is enumeration
shape; the stale-field gate is behaviour but no run exercised a visible
member being handled), **D1's measurement side** (the instrument reads
declaration form; today's behaviour is all *refusals* — no instrument has
ever read an artifact). The behavioural side of C4, B2, E1–E3 is behaviour
witnessed in-run.

## What today proved / what it didn't

Proved: the guardrails fire on real defects (duplicate, claim-mission-text,
answer-reviewer-mismatch, and now an approved already-satisfied claim
refused), the failure limb retains its receipt with full detail, the
revision loop changes artifacts, Q1 holds when the field is re-minted per
click, the serving envelope can be measured and its blind spots named, and
no run ever overclaimed. Didn't: an instrument that sees substance rather
than checkboxes or mission text (D1 — the day's central unresolved gap, now
caught failing in both directions), tie recomputation (B3), prediction of
any terminal class that occurred (B4), continuation reuse live (C2's
witness), and E4 avoidance remains a consequence of motion, not a mechanism.
