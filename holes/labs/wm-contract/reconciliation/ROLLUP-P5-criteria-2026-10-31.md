# P5 criteria rollup — all ordinary clicks of 2026-10-31 — zai-6, 2026-10-31

Owner: zai-6, evidence lane, read-only. This is the one-page count Joe reads.
Seven ordinary clicks (P5 sequence), best status across runs per criterion,
with the run that shows it. Full matrices exist for 4955b6e6, ebbbe42a,
d5b9b513, adc46631, 0be4fc7d; 0ba3370c and e71c7380 are covered from their
records and the plan checkpoints (no standalone matrix was requested for
them; their facts below are cited from the run records where used).

Runs (chronological): 4955b6e6 (already-satisfied, wants unmeasured) ·
0ba3370c (duplicate-guard refused, fallback :build-failed, injury a67d3e49) ·
ebbbe42a (:grounded-progress, 1/4 wants by checkbox) · e71c7380 (author
already-satisfied claim refused :claim-mission-text-not-an-artifact) ·
d5b9b513 (:grounded-progress, 0/6 wants, substance 1/6) · adc46631
(:incomplete :answer-reviewer-mismatch, injury c309d796) · 0be4fc7d
(:grounded-progress after revision round, revision door refused).

Legend: **observed** = clean witness · **observed w/ limits** = witnessed but
a stated boundary remains · **not-exercised** = the run(s) never reached that
machinery (not a defect) · **unknown** = no run could establish it ·
**violated** = a run shows the criterion broken.

| Criterion | Best status today | Best witness run | Notes |
|---|---|---|---|
| A1 budget/credit | **observed** | all 7 | every issuance has a consumption line with authorization sha `326826ac`, caller claude-1 |
| A2 serving envelope | **unknown** | — | no run produced a heap/time envelope receipt; r20 apparatus rows `:not-recorded` (adc46631) |
| A3 effective injuries | **observed** (effective: none) | 0be4fc7d | serving authority-aware read: none active all day (claude-1); two injuries minted (a67d3e49 ord 133, c309d796 ord 138) both medic-discharged; raw ledger still reads open — read effective, not raw |
| A4 pre-dispatch census | **observed** | 0be4fc7d | fully-populated effective-run-identity (target/manifest/policy/check-set/mission-source/base-head) |
| B1 population consistency | **observed w/ limits** | adc46631, 0be4fc7d | clean 617/617 and 618/618 on the two post-refresh clicks; earlier gap 610/611→610/614 (excursion-lane lag) closed by per-click re-mint, but equality holds only at selection time — drift is unending (Q1-GIRAFFE row, still open) |
| B2 failure retention | **observed w/ limits** | adc46631 | only failing ordinary run; failure-kind terminal receipt retained — but `:detail {:absent :no-error-data}`: cause evidence not retained |
| B3 selection scoring | **observed w/ limits** | 0be4fc7d | g-terms + normalization + accumulation retained every run; flat tie/cascade-scores block never present today; no recomputation ever claimed |
| B4 frozen prediction | **observed w/ limits** | adc46631 | landed correctly once (stop-the-line/none, and the run did stop); wrong on both success terminals (outcome set can't name `:grounded-progress`/`:changed`) |
| C1 construction identity | **observed** | d5b9b513, 0be4fc7d | patch/requisition/digest chain complete and `:claim-matches? true`; ebbbe42a's decisions-mismatch not reproduced after it |
| C2 durable checkpoint | **unknown** | — | no run proved pre-send durability vs end-of-run write |
| C3 descriptor contract | **observed** | all grounded runs | full-length shas throughout (patch commit, source-digest, requisition, grounded commit) |
| C4 review limb | **observed w/ limits** | **0be4fc7d** | the day's best: reviewer requested changes → revision → re-review approval with substantive note. Limit: no `:review-gate` map retained anywhere today |
| D1 want measurement | **observed w/ limits** | ebbbe42a, d5b9b513 | before/after measured, but the only instrument is the mission checkbox (C4 declaration form): ebbbe42a reached 1/4 by checkbox; d5b9b513 did real work (1/6 in substance) that measured false; 0be4fc7d has mission-state but no per-want outcomes block |
| D2 executable checks + learning | **observed** | ebbbe42a, d5b9b513 | replayed test receipts (ebbbe42a 4fd00fa63; d5b9b513 6 tests, byte-identical rerun) + publication receipts; 0be4fc7d verified with supporting artifact |
| D3 truthful classification | **observed** | all 7 | no run overclaimed: honest refusals, honest zero-want grounds; standing naming tension on `:grounded-progress` (want-delta terminal field queued) |
| D4 retention completeness | **observed w/ limits** | d5b9b513 | several permanent gaps every run: outer-task-selection receipt not retained; authority-aware injury join absent; q6 focus `:focus-not-retained`; habit store retired; r20 rows not recorded; frontier retention phase-dependent (0be4fc7d lost what adc46631 kept) |
| E1 ending receipt | **observed** | all 7 | both limbs exercised across the day: success receipts (ebbbe42a/d5b9b513/0be4fc7d) and failure receipt (adc46631), each with terminal publication + q6 terminal |
| E2 predecessor visibility | **observed** | all after the first | every run from 0ba3370c on saw its predecessor's ending (q6 :predecessor :present) |
| E3 no repeat on unchanged inputs | **observed** | 0ba3370c (refusal limb), 0be4fc7d (comparison limb) | duplicate-effective-run guard refused a real repeat (0ba3370c) before author contact; effective-run-comparison :no-duplicate with full identity (0be4fc7d); adc46631 shows the degradation: comparison :not-reached after a failed run |
| E4 identical inputs across a pair | **violated** (early), avoided late | violated: 4955b6e6, ebbbe42a, d5b9b513 | three violations, each narrowest-of-day so far: same-input pairs with serving composition drift (4955b6e6), a refused-predecessor component (ebbbe42a), the run's own grounded commit moving base-head (d5b9b513). Not violated after: pairs differ by target (adc46631) or by deliberate field re-mint + base-head (0be4fc7d) |

## The count

- **Observed clean (10):** A1, A3, A4, C1, C3, D2, D3, E1, E2, E3.
- **Observed with limits (7):** B1, B2, B3, B4, C4, D1, D4 — each has a
  stated boundary above; none blocks the next click.
- **Unknown (2):** A2, C2 — never established by any run today; both are
  machinery-receipt questions (envelope, pre-send durability), not run-lane
  questions.
- **Violated (1):** E4 — early-day only; the late-day pairs avoid it by
  construction (target change or deliberate re-mint), which is a workaround,
  not a fix.
- **Not-exercised** is per-run, not a day-status: by end of day every limb
  except the failure-to-duplicate-after-failure path has been exercised at
  least once.

## What today proved / what it didn't

Proved: the guardrails fire on real defects (duplicate, claim-mission-text,
answer-reviewer-mismatch), the failure limb retains its receipt, the revision
loop changes artifacts, Q1 holds when the field is re-minted per click, and
no run ever overclaimed. Didn't: A2/C2 receipts, C4 gate map, an instrument
that sees substance rather than checkboxes (D1), tie recomputation (B3),
prediction of success terminals (B4), and E4 avoidance remains a consequence
of motion, not a mechanism.
