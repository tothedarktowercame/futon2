# OBS-D — where the truth/observation pairs for measuring A actually are, if anywhere

Packet 17 of `PROOF-2-STRATEGY-draft-2026-09-24.md` (row 17 / OBS-D;
falsifier: a missing observation counted as a measured false; an
accepted-increment verdict relabelled as an observation of a token).
Author: kimi-1, 2026-09-24. Discovery/specification only: no code, click,
data write, bundle, or Lean file changed. All file:line anchors are at
futon2 `b03017a51d2330dc01a5c06a81c7da00652f0b04`; the Lean anchor is
mathlib4 `DarkTower/WarMachine/TokenObservation.lean` as checked out at
packet time. All record citations name the durable file; none of the
quoted values was recomputed.

Terminology used throughout, from the dispatch: for one occurrence and
one token, the REFERENCE TRUTH is whether the token was really produced;
the OBSERVATION is what the locator/check reported. A measured A
(`AdjudicationRates.falsePos` / `.falseNeg`, TokenObservation.lean:31–37)
needs pairs of these on the same occurrence, with denominators.

## 1. Inventory: every place a token outcome is recorded at close

The close record is `007-closed.edn` under
`data/wm-full-loop-machinery-<N>/<cohort>/attempt-00X/`; its judgment is
at `[:payload :judgment]` (run_ending_classification.clj:44–47 reads it
the same way). Token-outcome-bearing entries found:

### 1.1 `:token-outcome-comparison` — a prediction-vs-observation verdict (conflates, but not in the direction the theorem needs)

Key path: `[:payload :judgment :route-attestation :token-outcome-comparison]`
(observed present in 9 of the 10 closes surveyed; the tenth carries
`{:status :absent :reason :comparison-not-supplied}` — machinery-75
attempt-002, whose judgment also carries `:failure-kind :agent-unavailable`).

Value form (schema `:wm/token-outcome-comparison-v1`, produced by
`compare-outcomes`, token_outcome.clj:48–82): per wanted token,
`{:token [<target> <kw>] :predicted <exact rational> :observed <boolean
| {:status :missing :kind …}> :verdict <keyword> :measurement <row>}`.
Observed live: token `["T-repair-occ-444fb018…" :restoration-accepted]`,
`:predicted 1` (machinery-72 attempt-001) or `175/256` (machinery-76
attempt-002), `:observed false|true`, verdicts
`:predicted-not-observed` (8 closes) / `:predicted-and-observed`
(1 close, machinery-76 attempt-002).

The `:measurement` row embeds the locator report:
`:result {:observed <boolean> :check :C4 :evidence {:repo :sha
:resolved-sha :path :decl :file-present}}` (machinery-76 attempt-002,
007-closed.edn, row quoted verbatim in §2.1). The measurement rows
originate outside the close, in the enactment record
`:wm/d-task-enactment-v1` at
`futon3c/data/wm-d-task-enactment/action-<id>.edn`, keyed
`:after-token-evidence`; the close copies the row and names its source at
`:measurement-source {:path … :sha256 …}`.

Classification: the `:observed` boolean is an OBSERVATION (the C4
locator's report, observation_checks.clj:62–69 — a grep-class check for
the declaration line in a file at a resolved sha). The `:verdict`
compares that observation against the model's frozen PREDICTION
(`:positive-marginal-support`, token_outcome.clj:9–46), not against a
reference truth. There is no truth field anywhere in the comparison.
The `:predicted` leg is the scorer's own output; the comparison
therefore measures prediction-vs-locator agreement, i.e. success
frequency of the policy's predictions, not observation accuracy (the
honest sentence, §4.3).

### 1.2 `:accepted-increment` — a verdict built FROM observations, not an observation (the row's second falsifier made concrete)

Key path: `[:payload :judgment :accepted-increment]` (present in the
accepted close machinery-76 attempt-002: `:accepted? true`; the
predicate is accepted_increment.clj:36–134).

Its conjunct (b) (`:produced-token-results`, accepted_increment.clj:71–88)
re-reads each declared produced token TRUE at the after-revision through
the same C3/C4 locator checks (`locator-observed-true?`,
accepted_increment.clj:19–27). So the accepted-increment verdict is a
compound judgment whose per-token inputs are locator observations —
but what is recorded under `:accepted-increment` is the verdict and its
evidence summary, not a fresh observation act, and certainly not a
truth. Treating `:accepted? true` (or its per-token results) as "the
token was observed" relabels a verdict as an observation; treating it
as "the token was really produced" relabels it as a truth. Both are
the named falsifier.

### 1.3 `:run-ending-classification` — a record-only class, neither truth nor observation

Key path: `[:payload :judgment :run-ending-classification]`, receipt
schema `:wm/run-ending-classification-receipt-v1`
(run_ending_classification.clj:104–132). It classifies the ending from
a fixed projection of the close (`:close-judgment-keys`,
run_ending_classification.clj:22–26: `:outcome :grounded?
:artifact-only? :failure-kind :occurrence :route-attestation
:route-attestation-ref`) plus route-attestation increments and focus
facets. It reads existing entries; it observes nothing and adjudicates
no token.

### 1.4 `:occurrence` and `:outcome` — identities and run-level outcomes, no per-token values

`:occurrence` (`:wm/action-transition-occurrence-v2`) carries run /
cohort / attempt / transition / action identities
(run_ending_classification.clj:49–63 selects exactly these). The
close-level `:outcome` is a run outcome keyword (e.g.
`:agent-unavailable` in machinery-75 attempt-002), not a token truth.

### 1.5 Outside the close but token-outcome-bearing: the held-out calibration snapshot

`resources/wm/eig/held-out-calibration.edn` (schema
`:wm/eig-held-out-calibration-v1`, produced by
held_out_calibration.clj). Each scored row carries `:predicted
<rational>` and `:realised <boolean>`, where `:realised` is a
RE-OBSERVATION of the same token by the same C4 locator class at the
run's own artifact sha (`observe-realised`, held_out_calibration.clj
"the realised outcome is OBSERVED, never asserted … the artifact sha
pins the observation to what the run left behind"). Live rows: two
contributing rows (run-ids `2026-09-23-1790184736`,
`2026-09-23-1790187227`), both `:realised false`; a third row excluded
`:outside-declared-window` (`2026-09-23-1790189901`).

This gives, for two runs, TWO observations of the same token (the
close-time measurement row and the calibration-time re-observation) by
the same checker at the same revision — test–retest material for the
locator's self-agreement, not a truth/observation pair: neither leg is
independent of the locator being measured.

## 2. Does any occurrence on record carry BOTH a truth and an observation of the same token?

**No.** Surveyed: all ten `007-closed.edn` of machinery-72..76
(attempts 001/002 each), their embedded comparison/prediction/
accepted-increment/classification entries, the enactment record
`futon3c/data/wm-d-task-enactment/action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe.edn`
(read for machinery-76 attempt-002), and the calibration snapshot.
Nowhere is there a value that answers "was the token really produced"
by a channel independent of the locator that produced `:observed`.

The closest pair on record, quoted from machinery-76 attempt-002
(`007-closed.edn`, `[:payload :judgment :route-attestation
:token-outcome-comparison :tokens 0]`):

- prediction: `:predicted 175/256` (exact rational, from the frozen
  rollout over `:initial-belief` point mass);
- observation: `:observed true` via `:check :C4`, evidence
  `{:repo "futon2" :sha "97e17e10…" :resolved-sha "97e17e10…"
  :path "holes/tickets/T-repair-occ-444fb018….md"
  :decl "**Status:** DONE" :file-present true}`;
- truth: **absent**. The file's declaration line is what the locator
  reads; nothing on the record independently establishes that the
  restoration was really accepted (that the DONE line is correct), nor
  is there even a second, distinct mechanical checker whose
  disagreement could define an error event.

What I cannot show on the record: any adjudication of the locator
itself. The check evidence (`:resolved-sha`, `:path`, `:decl`,
`:file-present`) is sufficient for a LATER re-execution to adjudicate
the C4 report (grep the same file at the same sha), but no such
adjudication is recorded anywhere in the surveyed closes, and a
re-execution by the same check is a retest, not a reference truth.

## 3. Population and denominators an estimator could use today

By count, over the surveyed closes (machinery-72..76 = 5 cohorts × 2
attempts = 10 closes; machinery-77 exists on disk but was outside the
dispatch's named range and is not counted):

| Stratum | Count | Identities |
|---|---|---|
| Closes with a token-outcome comparison | 9 | run-ids 2026-09-23-1790131591, -1790136186, -1790161992, -1790184736, -1790187227, -1790189901, -1790193054, -1790196782, -1790199409 |
| …of which comparison rows (1 wanted token each, all `[T-repair-occ-444fb018… :restoration-accepted]`) | 9 | per-run `:tokens 0` rows |
| … verdict `:predicted-not-observed` | 8 | all of the above except -1790199409 |
| … verdict `:predicted-and-observed` | 1 | run-id 2026-09-23-1790199409 (machinery-76 attempt-002) |
| … rows with measured-false observation (`:observed false`, boolean) | 8 | the `:predicted-not-observed` eight |
| … rows with typed-missing observation | 0 | none in these nine (the missing-verdict path, token_outcome.clj:66–72, is unexercised here) |
| Closes with comparison typed absent | 1 | run-id 2026-09-23-eee9f1be-731f-46c2-941d-11b94d30187e (machinery-75 attempt-002, `:failure-kind :agent-unavailable`) |
| Accepted-increment verdicts | 1 | machinery-76 attempt-002, `:accepted? true` |
| Calibration re-observation rows (same token, same C4 class, artifact-pinned) | 2 contributing (+1 window-excluded) | run-ids -1790184736, -1790187227 (excluded: -1790189901) |
| **Truth/observation pairs eligible for an FP/FN estimator** | **0** | — |

So the only denominators available today are (a) prediction-agreement
denominators (9 comparisons, 8/9 predicted-not-observed — success
frequency of predictions, not observation accuracy), and (b) locator
test–retest denominators (2 runs with paired close-time and
calibration-time observations, both agreeing on `false`). Neither
identifies `falsePos`/`falseNeg` of the observation kernel. The
population also has a structural thinness the strategy should note:
every surveyed comparison measures the SAME token of the SAME target,
so even a perfect estimator on these rows would yield rates for one
token, not per-`v` rates for `AdjudicationRates (V → ℝ)`.

## 4. OBS-P: what a measurement producer must emit at the close seam

Since §2's answer is "no pairs exist", clause 1 needs a producer. The
close seam is where §1.1's comparison already lands (`[:payload
:judgment :route-attestation :token-outcome-comparison]`); the pair
carrier belongs beside it so the observation leg reuses the existing
measurement rows rather than a parallel channel.

### 4.1 The pair carrier (per occurrence × per token)

Schema `:wm/token-outcome-pair-v1`, at
`[:payload :judgment :route-attestation :token-outcome-pairs]` (vector),
each entry:

- `:occurrence` — the same identity map the close already carries
  (`:run/id`, `:cohort/id`, `:attempt/id`, `:transition/id`,
  `:action/id`, `:action/value-sha256`; the form
  run_ending_classification.clj:56–62 already validates);
- `:token` — the qualified token;
- `:observation` — `{:observed <boolean> :check :C4 :evidence {…}
  :checked-at <instant>}` copied from the measurement row (as today),
  OR the typed-missing form `{:status :missing :kind
  :measurement-unavailable|:ambiguous-measurement|:artifact-revision-mismatch}`
  exactly as token_outcome.clj:66–72 already distinguishes — **never a
  boolean substitute** (the row's first falsifier is schema-level: only
  booleans enter denominators; missing stays missing);
- `:truth` — `{:truth <boolean> :truth-source <keyword>
  :adjudicator <identity> :evidence {…} :adjudicated-at <instant>}`,
  where `:truth-source` must name a channel independent of the
  `:observation`'s locator — concretely one of:
  `:independent-recheck-different-class` (a second mechanical checker
  of a different class than the observation's), or `:human-adjudication`
  / `:reviewer-adjudication` (a recorded reviewer verdict on the same
  artifact at the same `:resolved-sha`), each with its own evidence
  map — OR `{:status :missing :reason :no-independent-truth-channel}`;
- `:reviewed-revision` — the after-revision sha both legs are pinned
  to (the falsifier "mismatched reviewed revision" is a join check on
  this field against both evidence maps);
- `:pair-sha256`, `:schema-version`.

A pair is **estimable** iff `:observation` is boolean and `:truth` is
boolean; the estimator's population is exactly the estimable pairs,
with `:counts` reporting `{:estimable n :missing-observation m
:missing-truth k}` so the denominators are on the record. The same
`:consumed-at`/`:value-sha256` uniform pair that CERT-S §0's addition
already requires of every term entry applies to the aggregated rates
under `[:decision :selection-certificate :model-inputs <id> :A]`
(CERT-S §1, row A: `:rates`, `:estimator`, `:population`, `:window`,
`:counts`, `:source-row-identities` — the identities of the pair
entries above).

### 4.2 Honest absences this producer must keep

- Observation missing ⇒ `:observation {:status :missing …}` and the
  pair is non-estimable. It must not enter any rate (first falsifier).
- Truth missing ⇒ same, with `:no-independent-truth-channel`; today's
  entire corpus would carry this reason, which is the accurate
  description of §2.
- An accepted-increment verdict is never copied into either leg
  (second falsifier): the pair's `:truth` requires its own adjudication
  event; `:observation` requires its own check report. Conjunct (b) of
  `accepted-increment` MAY supply the observation leg's boolean only by
  re-citing the locator check's own evidence map, marked
  `:observation-source :accepted-increment-conjunct-b` — and then the
  truth leg must still come from elsewhere, or the pair is not
  estimable.

### 4.3 The honest sentence

Success frequency is not observation accuracy: the 8/9
`:predicted-not-observed` rate measures how often the scorer's frozen
prediction agreed with the locator, which convolves the policy's
predictive quality with the locator's error rate, and says nothing
about either alone — false-positive and false-negative rates of token
observation require, per occurrence, a truth and an observation of the
same token, and no such pair exists on any surveyed record.

## 5. Falsifier restated as a concrete bad case

Row falsifier, concrete: machinery-75 attempt-002's close carries
`:token-outcome-comparison {:status :absent :reason
:comparison-not-supplied}`. If an estimator treated that absent
comparison — or any `{:status :missing}` observation under
token_outcome.clj:66–72 — as a measured `false`, it would manufacture
one falseNegative count for token `[T-repair-occ-444fb018…
:restoration-accepted]` out of an occurrence where the agent never ran
(`:failure-kind :agent-unavailable`), silently enlarging the
denominator by a non-observation. Second falsifier, concrete: reading
`[:payload :judgment :accepted-increment :accepted? true]` on
machinery-76 attempt-002 as "token :restoration-accepted observed
true" and pairing it with the comparison's `:observed true` as if they
were independent legs — both trace to the same C4 read of the same
`Status: DONE` line at `97e17e10`, so the "pair" would have
correlation 1 by construction and the estimated rates would be the
locator's self-agreement dressed as accuracy.

## 6. What this packet unblocks

- **OBS-P (row 18)** has its emission target: the pair carrier of §4.1
  at the close seam beside the existing comparison, with the
  estimability rule and the typed absences named.
- **A-S (row 19)** has its population/denominator map: today
  zero eligible pairs; the denominator rule (only boolean/boolean
  pairs count, with `:counts` broken out) is fixed here; and the
  one-token thinness of the current population is flagged for the
  estimator's scope statement.
- **D-D (row 23)** can treat §1.1's comparison as a
  prediction-agreement record only; conditioning admission must not
  inherit it as an observation-accuracy input.

## 7. Missing definition the theorem needs (proposed amendment)

**Amendment proposal (for the Review amendments, not a question):**
Clause 1's P₁ speaks of "the extracted accepted outcome rows" as the
estimator's input. The theorem has no definition of *eligible pair*.
Add to clause 1:

> **Eligible pair.** An occurrence-token pair is eligible for the A
> estimator iff the record carries, for the same occurrence identity
> and token, (i) an observation: a boolean locator report with its
> check class and revision-pinned evidence, and (ii) a reference
> truth: a boolean adjudication whose named channel is independent of
> that locator, revision-pinned to the same after-revision. Typed
> absences of either leg are recorded and counted in `:counts` and
> excluded from the rate. An accepted-increment verdict, a
> run-ending classification, or a prediction–observation comparison
> verdict is neither leg.

Without this, W₁'s "measured-rate side proposition" quantifies over a
set the record cannot present, and any estimator risks the two
falsifiers above as silent defaults.

## Revision 2 — complete close-carrier census and extracted pair identity

Author: codex-1, 2026-09-24. This append corrects the incomplete inventory
and restricted population above; it does not rewrite the original findings.
Sources inspected at futon2 `d1b011ab9dc54ee0740309752bfc110174bc554f`:
`full_loop_runner.clj` (`retain-token-outcome!`, `retain-kernel-example!`,
both close assemblies), `kernel_example.clj` (`align`, `collect`),
`attempt_learning.clj` (`receipt`), `learning_trial.clj` (`receipt`), and
`learning_trial_ledger.clj` (`record!`, `read-trials`, `b-update`).

### R2.1 Two additional carriers

1. `[:payload :judgment :kernel-example]` is an OBSERVATION carrier,
   schema `:wm/aligned-kernel-example-v1`. Its complete token population is
   `[:observation-projection :observations]`, a token-keyed map whose rows
   carry `[:artifact-observation :observed]` and `:measurement :result`
   (check class and evidence). The revision pair is nested at
   `[:observation-projection :revision-pair]`, not at the example root.
   `:tokens` is only the wanted-token projection; counting it alone misses
   the other observed tokens. `:observation-source {:path :sha256}` names
   the enactment file and its raw byte hash. `align` checks occurrence,
   universe, declaration/meaning, artifact revision and measurement joins;
   it emits typed missing observations when execution is not admitted.
   This does not establish reference truth or causal attribution.
2. `[:payload :judgment :learning-trial-receipt]`, also retained inside the
   token comparison, is schema `:wm/learning-trial-receipt-v2`. Its
   `:trials` are selected-cascade/effect/attempt B-learning trials with
   `:after-observation`, retained measurements, deduplication identity,
   admission/held status and ledger `:counted?`. `attempt-learning/receipt`
   reads the same verified observations as the kernel example, and
   `learning-ledger/record!` records eligible trials. Neither trial
   admission, ledger counting, nor the illustrative Beta summary is an
   independent truth adjudication or a new observation. Refuse the receipt
   and trial wrapper as EITHER pair leg with
   `:reason :learning-trial-receipt-not-a-leg`. The underlying locator
   report can instead be cited through its observation carrier and source.

The four earlier categories remain: comparison (observation plus prediction
agreement verdict), accepted-increment (compound verdict), ending
classification (record-only class), occurrence/outcome (identity/run label).
The root comparison and route-attestation comparison are equal in all 13
non-aborted closes; they are copies, not separate measurements. In 75/002,
root comparison, kernel example and learning receipt are nil on the old
record; the route comparison is explicitly absent. The census represents
these legacy nils as `{:status :missing :reason :carrier-not-recorded}`;
it neither rewrites the record nor substitutes a boolean.

### R2.2 Recomputed machinery-70..76 population

Read all **14** files matching
`data/wm-full-loop-machinery-N/wm-contract-machinery-N-v1/attempt-00X/007-closed.edn`,
N=70..76, X=1,2, with a tagged-literal-tolerant EDN reader and an EOF check.
Table cells are **true / false / typed-missing token counts**. Kernel counts
use the complete admitted observation map, not just wanted tokens. When
projection admission fails, the wanted-token missing rows are counted.

| N / attempt | run id (date prefix omitted) | Comparison | Kernel | B trials counted / held |
|---|---|---|---|---|
| 70/001 | 1790037762 (2026-09-22) | 0 / 0 / 2 | 0 / 0 / 2 | 0 / 1 |
| 70/002 | 1790053967 (2026-09-22) | 0 / 0 / 3 | 0 / 0 / 3 | 0 / 1 |
| 71/001 | 1790060806 (2026-09-22) | 2 / 1 / 0 | 3 / 4 / 0 | 1 / 0 |
| 71/002 | 1790110142 (2026-09-22) | 1 / 1 / 0 | 1 / 2 / 0 | 1 / 0 |
| 72/001 | 1790131591 (2026-09-23) | 0 / 1 / 0 | 1 / 5 / 0 | 1 / 3 |
| 72/002 | 1790136186 (2026-09-23) | 0 / 1 / 0 | 2 / 4 / 0 | 1 / 3 |
| 73/001 | 1790161992 (2026-09-23) | 0 / 1 / 0 | 2 / 4 / 0 | 1 / 3 |
| 73/002 | 1790184736 (2026-09-23) | 0 / 1 / 0 | 2 / 4 / 0 | 1 / 3 |
| 74/001 | 1790187227 (2026-09-23) | 0 / 1 / 0 | 2 / 4 / 0 | 1 / 3 |
| 74/002 | 1790189901 (2026-09-23) | 0 / 1 / 0 | 3 / 3 / 0 | 1 / 3 |
| 75/001 | 1790193054 (2026-09-23) | 0 / 1 / 0 | 4 / 2 / 0 | 1 / 3 |
| 75/002 | occurrence missing: agent-unavailable | carrier absent | carrier missing | carrier missing |
| 76/001 | 1790196782 (2026-09-23) | 0 / 1 / 0 | 5 / 1 / 0 | 1 / 1 |
| 76/002 | 1790199409 (2026-09-23) | 1 / 0 / 0 | 6 / 0 / 0 | 1 / 1 |
| Total | 14 closes | 4 / 10 / 5 | 31 / 33 / 5 | 11 / 25 |

Thus 13 compared receipts carry 19 wanted rows, including five missing
measurements (70); one further close has an absent comparison. The 13
recorded kernel examples comprise 11 admitted projections (64 booleans)
and two unavailable projections (five wanted-token typed absences,
`:task-execution-incomplete`). The aborted close is one **carrier-level**
absence, not a fabricated token row. Thirteen learning receipts have 36
trials, 11 counted and 25 held. None supplies independent truth.

The 14 boolean comparison rows are already among the 64 kernel observations
on the same occurrence/token/revision; adding the carrier counts would
count them twice. The kernel adds **50 distinct observed occurrence/token
rows** to the comparison inventory. The held-out calibration snapshot's
two realised-false rows remain same-locator reobservations, not reference
truths. **Eligible truth/observation pairs: 0** in the enlarged inventory;
64 measured occurrence/token observations lack independent truth, five
wanted-token rows lack an admitted observation as well, and one close lacks
a token carrier. False-positive and false-negative truth denominators are
both zero: these absences cannot be estimated as zero error rates.

Concrete 76/002 source: `futon3c/data/wm-d-task-enactment/` +
`action-5c1163d2-3e45-4fea-8919-6e2b41c6acfe.edn`, raw SHA-256
`161d0c1c9afa30e927d4d08a110256820ee3563d227aac7b7c38dc1f921cdc66`.
Its projection records before `a1957b7cb871a752927aa98ee2340ed91c64812f`,
after `97e17e10f2695c481c55ddaebe9026f2d245663f`, and
`:before-evidence :not-measured`. All six observations are C4 true;
the two learning trials are one held, one counted. Neither the unmeasured
before leg nor the reference truth may be inferred from those six truths
of the locator's report.

### R2.3 Pair hash domain and explicit CERT-S/GEN-D amendment

Proposed CERT-S addition (upstream source rows for its A entry) and GEN-D
extraction rule: `:pair-sha256` is **only a canonical extracted-value hash**,
`"sha256:<hex>"`, over this five-key map:

```clojure
{:occurrence <identity projection>
 :token <qualified token or typed absence>
 :revision-pair <recorded revision pair or typed absence per missing revision>
 :observation <complete extracted observation leg including source ref>
 :truth <complete extracted truth leg or typed absence>}
```

The occurrence projection has exactly `:run/id`, `:cohort/id`,
`:attempt/id`, `:transition/id`, `:action/id`, `:action/value-sha256`;
unrecorded fields carry `{:status :missing :reason :identity-not-recorded}`.
The full action payload, schema/eligibility fields and the hash itself are
outside this byte domain. When a comparison supplies only its after
revision, before is `{:status :missing :reason :before-revision-not-recorded}`;
no predecessor is reconstructed. Missing after is similarly typed.

Canonical bytes follow CERT-S §3: UTF-8, no trailing newline, maps sorted
by recursively canonical printed key, sets sorted by recursively canonical
printed element **with set delimiters preserved**, vectors remain vectors,
exact integers/ratios, doubles as `#wm/double "<Double/toHexString>"`, normal
EDN escaping for strings, no metadata or comments. Use one space between
map entries and between key/value; no commas. Recompute from those five
values to verify. Record/source raw-file SHA-256 remains a separate identity
and is never replaced by the pair hash. The observation source retains its
raw `:path` and `:sha256`; hashing the extracted pair does not verify the
referenced file. This explicitly separates the two domains GEN-D §1.1/§1.3
requires. Historical OBS-P hashes are not silently reinterpreted: the
amended producer uses `:schema-version 2` and declares
`:pair-hash-domain :wm/token-outcome-pair-value-v2`.

Amend §7's proposed W1/P1 eligibility definition and CERT-S's A source-row
rule together: only same-occurrence/token/revision independent
boolean/boolean pairs are eligible. Kernel observation rows are allowed;
learning-trial receipt/trial wrappers, accepted-increment and comparison
verdicts are neither leg. A missing or refused leg excludes the pair.
These are proposed amendments, not a claim that W1 or CERT-S already
contains the missing definitions, and the census does not establish W1.
