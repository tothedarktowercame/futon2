# L4b: where the record keeps each Lean carrier (claude-3, 2026-11-01)

Discovery for L4c. No code changed. Read: the 11-01 record copy at
`~/notes/l4a/claude2/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn`, and
the 28 run records dated 10-29 to 11-01 that carry `:outer/selection` (the era-2 set). Census
scripts and their output are in `~/notes/l4b/` (`census.clj`, `census2.clj`, `census3.clj`,
`census*.edn`). Base: mathlib4 `55878e243a`, futon2 main `641719d4c`. Paths abbreviated
`SC` = `[:decision :selection-certificate]`, `OS` = `[:outer/selection]`.

Population of the 28: 25 have scored candidates (13 with 2 or more policies, 12 with one); 3
have none (22539d3d, f74df9cc, 4961d278). Where a producer is named from a grep and I did not
read the code, it says "grep suggests".

## What L4c and Joe need to know first

**1. On work runs the chosen policy is usually not the G minimum.** In 12 of the 13 multi-policy
runs, `[:decision :chosen :id]` has a higher G than another compared policy. Only ac5bc477 chose
the minimum. Example, 376d76ff: chosen `policy-7d91…` G 1.6584, while `policy-34c8…` has G 1.4034
and the highest posterior (0.392 against 0.304).

The cause is the law, not an error in the record. The posterior is taken over policies
(`softmax(log E − γG)`, `:law :sigma-log-E-minus-gamma-G`), and then summed over each policy's
first action (`[:decision :selection-law :action-marginal]`). The winning action is enacted.
`policy.clj:609` says so: `:per-policy-argmax`, "the posterior's mode, which the summed
marginal can overrule". Several same-G variants sharing a first action outvote a lower-G policy
that stands alone. The record's `[:decision :selection-law :policy-comparison]` reads
`:decided-by :G` and names the argmax as winner in those same runs, which is not what was enacted.

So Q11's "chosen G strictly below every other compared G" will read **violated** on those 12 once
`policyG` projects. In 2 of them (e71c7380, f4b95abf) the action comparison itself tied exactly at
0.3964 and `:tie-break-rule :action-name-ascending` decided it: selection by name order on the
inner path. Joe needs to rule on whether Q11 or the action marginal is right. This is the same
"policy, not action, is what G is over" question as `Joe: "G is defined over policies"` (Q3's
text). I am not proposing an answer.

**2. Q11's `comparedPolicies = constructedCascades` cannot hold on two record shapes.**
- The projector puts them in different id spaces (row 7), so it fails on every run until L4c fixes
  that.
- After that fix it still fails in the 2 records where Q6 removed a refused policy before the
  comparison (d5b9b513, c5590f95: 5 constructed, 4 compared, `SC :q6 :status :excluded`). The
  deleted Q8 allowed this through `requirementExcluded ⊆ q6Excludable`. Q11 has no such allowance.

**3. No inner stopped-kernel G exists.** See rows 4 and 6. Q11 stays unverifiable on work runs
until L5.

**4. Every inner G comes from a synthetic model.** In all 25, each `SC :scoring i` has
`:scope :synthetic-bounded-replay` and `[:observation-model :provenance]` reads
`{:status :synthetic :calibrated false, :source "cascade_shape_g arrangement model"}`. The numbers
are exact-enumeration results over a model nobody has calibrated. This does not block projection,
but a conforming Q11 on these values would certify a comparison on an uncalibrated model.

**5. The selection path's Q2 cannot be filled from the record.** No task-selection library pin or
per-cascade prediction exists on any era-2 record (rows 9, 11). L5 has to produce them; L4c can
only type them absent.

## Work run

| # | Carrier | EDN path | Shape, exactness | In 28 | Producer |
|---|---|---|---|---|---|
| 1a | `targets` | `SC :q2-construction :entries 0 :task` | string | 26 (0 entries in 22539d3d, f74df9cc) | `q2_construction.clj:45` |
| 1b | `action` | **no source** | Q2 and Q11 do not read it; a stable constant or the task id serves | none | none |
| 1c | `reading` | `… :entries 0 :reading` = `{:excerpt-digest :analysis-digest}` | two shas, no single digest; projector digests the map | 26 | `q2_construction.clj:47` |
| 1d | `library` | `SC :q2-construction :library :sha256` | sha over the pattern→sha map | 26 | `q2_construction.clj:27` (`library-manifest`) |
| 1e | `graph` | `… :entries 0 :graph-digest` **or** `SC :q2-construction :graph :pin :sha256` | two different shas in all 26 | 26 | `target_policy_family.clj:210` (a `pr-str` hash); `:pin` from the pattern-graph pin |
| 1f | `kernel` | `… :entries 0 :kernel` = `{:retraction {…} :transition-kernel kw}` | map, no digest; projector digests it | 26 | `q2_construction.clj:49` |
| 1g | `slice` | `… :entries 0 :slice` = `[{:pattern :source-sha256}]` | exact shas | 26 | `q2_construction.clj:51` |
| 1h | `pool` | **no key.** Derive: patterns in `… :entries 0 :policies * 3 :nodes` | | 26 | derived |
| 1i | `policyCount`, `policies` | `… :entries 0 :policies` (policy keys) | vectors `[:wm/reading-policy-v1 target id {…}]` | 26 | `q2_construction.clj:52` |
| 1j | `sliceFromWholeLibrary` | computed | see below | 26 | projector |
| 2 | `constructorPatternCount` | `count` of 1h | int | 26 | derived |

Notes on row 1:
- The projector reads `SC :target-construction`, a key no era-2 record has
  (`wm_run_facts.clj:351`). That is the only reason `targetConstruction` is NR today.
- `sliceFromWholeLibrary` inputs are all present: `… :entries 0 :slice *` against
  `SC :q2-construction :library :patterns` (pattern id → sha, 1491 to 1527 entries). Checked: all
  26 hold, every slice pattern at its cited sha.
- `pool = slice` holds in all 26 by the derivation in 1h, so Q2 can pass on it. The slice is
  already what the policies use: seed patterns that failed (`:failures`, `:void-pattern-citation`
  and others, 22 of 26 have at least one) are left out of the slice.
- `4961d278` has `:status :failed`, an empty slice and no policies. It is `:abstained`.
- `libraryPatternCount` is read from the live snapshot, not the record. In these records the pin
  at the time is `SC :q2-construction :library :count`, which also equals
  `[:world-at-selection :pattern-library :file-count]` in all 28. L4c should take the record's.
- Packet: "`:library` pinned, count 1527" holds for 15 of 28; the count ranges 1491 to 1527.

### Row 3: `policyG`

The G the law consumed is `SC :candidates i :g`. Agreement checked, by policy id:

| Path | Agrees with `:candidates i :g` |
|---|---|
| `SC :scoring i :g` (map indexed 0..n) | all 25 |
| `SC :target-policy-families * :policies * :G` | 23 of 25: the other two are the Q6-excluded records, which list a fifth policy |
| `SC :preference-receipts * :g` | 23 of 25, the same two records |
| `SC :parameter-novelty * :terms :serving-G` | all 25 |

Confirmed on ac5bc477 and only there: `ln(0.36249/0.31876) = 0.1286 = 0.8 × (1.7966 − 1.6359)`, so
`:posterior` uses γ = `[:decision :selection-law :gamma]` on these G values.

- Every `:g` is a double, 25 of 25 records. The `:scoring` step predictions are exact, see row 5.
- G composes as `risk + fit-ambiguity − information-gain`, each normalized. On ac5bc477 policy 0:
  risk 1.6784 = progress 1.5735 + class-emission 0.1049; ambiguity 0.4251 (`:fit-ambiguity`);
  information 0.3069; total 1.7966. `SC :preference-receipts * :normalization :components` holds
  the split.
- `[:decision :g-term-decomposition]` and `SC :g-term-decomposition` read `:status :missing` in all
  28. Not a source.
- `SC :parameter-novelty * :terms :novelty-consumed-in-G?` is false: novelty is recorded and not in
  G on this path.

### Row 4: `abstentionG`: absent

`[:decision :abstention]` is `{:status :not-abstained}` (25), `:absent` (2), `:abstained` with
`:targets` (1, 4961d278); writer `full_loop_runner.clj:885-898`. `:abstained` comes from
`wm/family_selection.clj:451,527`, which refuses when no family formed. It scores nothing.
Greps for `stopped` and `abstain` across `policy.clj`, `wm/cascade_decision.clj` and
`wm/family_selection.clj` find refusal routes only, no stopped model. The stopped kernel exists
only on the outer path (`outer_score.clj:88`). **So no stopped-kernel G is computed on the inner
path.** That is an L5 requirement, and Q11 stays unverifiable on work runs until then.

### Row 5: `policyPrediction`

The packet's `[… :candidates * :certificate :node-evaluations]` does not exist. The real paths:
- Progress prediction: `SC :scoring i :steps * :prediction`, e.g. `{[0 false] 1/2, [1 false] 1/2}`.
- Class prediction: `SC :candidates i :class-emission :certificate :steps * :prediction`, e.g.
  `#:ending{:not-yet-evaluated 1N}`.
- Node evaluations: `SC :node-evaluation-traces * :evaluations`, and
  `:class-emission :certificate :node-evaluations`.

Every progress prediction value is an exact ratio, in all 25 (grep suggests the writer is
`cascade_observation_scoring.clj:248`).

**Proposed digest:** sha over `[(mapv :prediction progress-steps), (mapv :prediction class-steps)]`
in step order, with sorted keys. This is the object the law scored: raw risk equals the sum of
step risks (36.38 on ac5bc477), and the class risk adds through the normalized component. It is
exact, so it digests stably. I did not include the fit-ambiguity input, which G also depends on.
Including it would mark policies apart that predict the same outcomes.

Distinct predictions, using `:scoring` alone: every multi-policy run has exactly 2 distinct
sequences, whether n is 3, 4 or 5. So Q11's "not all equal" passes on all 13, and the same-G
variants from point 1 predict identically. Those duplicates are what outvotes the minimum.

### Row 6: `abstentionPrediction`: absent

Same as row 4. On the outer path the stopped distribution is `{[:stop-the-line :none] 1.0}`; there
is no inner equivalent.

### Row 7: decision ids against compared ids

| Field | Today's projected value | Id space |
|---|---|---|
| `comparedPolicies` | `pr-str` of `SC :q4 :compared` (`wm_run_facts.clj:109`) | policy key text |
| `currentDecision`, `previousDecision` | `pr-str` of `[:q6 … :choice :policy-key]` (`wm_run_facts.clj:391`) | policy key text, **same as compared** |
| `constructedCascades` | `action-identity/digest` of `:id` (`wm_run_facts.clj:447`) | a digest, **different** |
| `[:decision :chosen :id]` | `"E-…/policy-78fb…"` | short id, joins `:q4 :policies * :policy-id` |

`SC :q2-construction :entries 0 :policies` equals `SC :q4 :compared` as sets in every run with candidates
except the two Q6-excluded records.

Consequence: the L2 ruling "the cascade digest is the policy id" is available: `SC :candidates i
:id :cascade-digest` is a sha, present and distinct within the run in all 25. But the Q6 carrier
holds only `:policy-key`, `:policy-id` and `:target`, with no digest. If L4c moves compared and
current to the digest and leaves previous as a key, `previous ≠ current` is always true and **Q6
goes vacuous**. Either hash the policy key on both sides (a function of the record alone) or add
the digest to the Q6 carrier in L5. I recommend hashing the policy key: same digest space for
everything, no producer change, and the same value for previous and current. It is not
`:cascade-digest`, so this would amend the L2 ruling's wording.

## Selection run

| # | Carrier | EDN path | Shape, exactness | In 28 | Producer |
|---|---|---|---|---|---|
| 8 | `enumeratedTasks` | `OS :outer-field/admitted` ∪ targets of `OS :outer-field/excluded` | strings; no duplicates in any of the 28 | 26 non-empty | `full_loop_runtime.clj:1272,1339`, `outer_observation.clj` |
| 9a | `comparedPolicies` | `OS :tied-cascades` is **not** the menu: it lists only those at the minimum. The menu is only in `outer-makeup.edn :cascades` | keywords | indexes in all 28 | `outer_select.clj:195` |
| 9b | `policyG` | `OS :cascade-scores` (position = makeup menu position) | doubles, sum only | 28 | `outer_select.clj:160` |
| 9c | chosen | `OS :cascade-id`, `:cascade-index` | keyword, int | 28 | `outer_select.clj:159` |
| 9d | `:cascade-picks` | `OS :cascade-picks` | target or `:no-pick` | **3 only** (f4b95abf, ac5bc477, c5590f95) | `outer_select.clj:164` |
| 9e | `policyPrediction` | **one only**: `OS :prediction {:outcomes :q :landing :status}` | doubles, 15-entry map | 28 | `outer_select.clj:188` |
| 10 | `abstentionG` | not separate | see below | none | `outer_score.clj:88` |
| 12a | `receipt` | digest of `OS` | computed by projector | 28 | |
| 12b | `pickRelation` | `OS :selected-relation`; `:cascade-pick-relations` in 3 | keyword | 28 | `outer_select.clj:174` |
| 12c | `pickReach` | `OS :selection :selected :reach` | `1/2` in all 28 | 28 | `outer_observation.clj:119` (`:default-reach`) |
| 12d | `consumedRuns` | `OS :outer/learning :receipt :run-id`, `… :receipt :ending` | one per record | 12 | `outer_state.clj:238` |
| 12e | `learningApplied` | `OS :outer/learning :status` | `:applied` 12, `:deferred` 15, absent 1 | 27 | `full_loop_runtime.clj:1239-1248` |

Row 8 detail:
- Field size `admitted + excluded` runs 147, 182, 419, 608, 610, 617, 618, 626 across the era. It
  equals `[:world-at-selection :enumerated-tasks :ids]` in 27 of 28. The exception is 9e2103c1
  (enumerator 1, admitted + excluded 26). 22539d3d and f74df9cc are 0 and 0.
- The packet's "17 typed exclusions" is the witness's 609 field. In ac5bc477 there are 81 exclusions,
  and across the era 16 to 81, apart from the two empty records.
- 47 rows in each of 15 records have no `:reason`: they are `{:kind
  :outer-score/graph-term-unavailable …}` rows, merged in from `OS :outer-score/excluded`.
- Reason values seen: `:open` 290, `:inactive` 72, `:operator-gated` 36, `:active` 26, `:identify` 20,
  `:all-policies-already-satisfied` 20, `:target-source-path-absent` 12,
  `:unread-new` 60, `:blocking-effective-run-on-same-giraffe-field` 6, `:repeat-choice-after-refusal` 1,
  and others.
  `:open` and `:active` are registry statuses recorded as exclusion reasons; whether they are
  typed exclusions at all is a question for L5.

Row 9: **only the chosen cascade has a prediction.** `:prediction` is built from `chosen` alone
(`outer_select.clj:151,188`). The three scores have no terms and the other cascades have no
prediction. Q11 needs a digest per cascade, so this is an L5 requirement. `OS :selection` retains
only `:selected`; the producer's `:candidates` does not reach the record.

Row 10: the score of a no-pick cascade is the stopped kernel's, so it sits in `:cascade-scores`
at that cascade's position. That is only visible in the 3 records with `:cascade-picks`, where
position 2 is `:no-pick`. **It is an accident of the menu**: `find-out-before-building` happens to
pick nothing. If every cascade picks, nothing computes abstention. L4c can project position 2 as
abstention G only in those 3 records, and only by reading `:no-pick`. In the 25 without picks
nothing says which position, if any, abstained. L5 must score the stopped kernel separately, as
the hand witness does (`:abstention :score :G 4.7875`).

Row 11, construction against the witness:

| Witness field | In the record? |
|---|---|
| `reading` = Giraffe pass sha | **No.** `OS :giraffe/run-id` and `:giraffe/manifest-sha256` are present; the pass sha is not |
| `graph` = Giraffe input sha | **No.** Recoverable from the run directory named by `:giraffe/run-id`, outside the record |
| `kernel` = makeup sha | Yes: `OS :makeup/sha256` |
| `library` = task-selection manifest sha | **No, and nothing on the path reads the library.** `outer_*.clj` have no library pin; the menu's patterns are names in `outer-makeup.edn`. `[:world-at-selection :pattern-library]` is the whole 1527, not task-selection |
| `slice`, `pool` | **No.** Makeup names 4 patterns; the record names none |
| per-cascade unit shas and edges | **No** |
| `libraryPatternCount` 19 | **No** (20 files in the directory, one a README) |
| `policies`: cascade digests | **No.** Keywords only |
| per-cascade relation, model, terms | relation in 3 records; the rest **no** |
| abstention model and terms | **No** |
| tie disclosure | `OS :tied-cascades` yes, but `:tie?` is defined over distinct picks and read false in all 28 |

Row 12:
- `receipt`: the projector digests `OS` itself. All 28 would be `observed`. The consumption receipt's
  `:selection-sha256` is the digest of the previous run's pending selection, not this one.
- `pickRelation`: `:unknown` in 11, `:associated` in 17. Map `:unknown` to `.absent`. No
  classify-target receipt digest is on the record, so even a known relation has nothing to cite;
  L4c can digest the relation fact, which is weaker than the docstring's "receipt".
- `pickReach`: `.defaulted` in all 28, as a fact about the code (`outer_observation.clj:119`). The
  record shows only the value `1/2`; nothing marks it defaulted, so the projector has to assert it.
- `pendingRun`: when `:applied`, the receipt's `:run-id`. When `:deferred`, the record has only
  `:reason :terminal-ending-class-absent` and **no run id**. `[:q6 :predecessor :previous :run/id]`
  is the nearest carrier, but it is not the same thing: of 12 consumptions, 2 consumed a run other
  than the predecessor (`3415b671` consumed f74df9cc; `4955b6e6` consumed f805ca4b). `outer-state.edn`
  `:pending` is not in the record.
- `consumedRuns`: run id plus `… :receipt :ending :classification-absence`. All 12 are
  `:classification-receipt-absent`, so all 12 project `.absent`. There is a `:completion-pin` and a
  `:record-pin` on each, which are not classification receipts.
- `learningApplied`: `:status :applied`. All 12 report applied with `:ending-class
  :not-yet-evaluated`, which by `outer_state.clj:126-129` performs decay only.

Row 13, a Q6 shape for selection: **partly.**
- `previousOutcome`: `[:q6 :predecessor :previous :outcome :outcome]`, present in 24. It is the work
  run's outcome; a selection run has no outcome of its own.
- `previousDecision`: no selection-level record of the previous cascade. `[:q6 … :choice]` is the
  inner policy. The previous run's own record has `OS :cascade-id`, but the projector reads one
  record, and the docstring at `wm_run_facts.clj` q6-facts forbids searching others.
- `currentInputDigest`: `[:world-at-selection :selection-input-digest]`, 26 of 28. This is the
  digest Q6 already reads, and the Q1-Q2 incident record notes that it covers the field and so
  changes every click.
- Nearest existing carrier: `[:outer-repeat-comparison]` (`outer_refusal_history.clj:88,148`, 27
  records; `:status` `:not-reached` 12, `:inputs-changed` 10, `:no-prior-refusal` 5).
  `:inputs :outer-selection` holds the cascade id, index, Giraffe pins, makeup and focus in 15. It
  compares a run against earlier refusals, and could carry the digest if L5 narrowed it to the
  fields the choice depends on.

## Exactness: proposed rule, for Joe's decision

Smallest gaps between distinct values within one run, across the 28:

| | Smallest | Next |
|---|---|---|
| Inner G | 0.0490 (3415b671) | 0.0553 |
| Outer scores | 4.4 × 10⁻¹⁶ (ebbbe42a) | 0.1336 |

There is one gap of arithmetic noise and nothing else below 0.04. Any scale from 10⁻³ to about
10⁻¹⁴ gives the same verdicts on the 28, so 10⁻⁶ has room on both sides.

**Proposal:**
1. Rational: `BigDecimal` of the double, rounded `HALF_EVEN` to 6 places, written
   `{"numerator" n "denominator" 1000000}`. The checker already accepts that shape.
2. Same scale for `policyG` and `abstentionG` in a run.
3. Record `"scale" 1000000` beside them, and the source double as `"double"`, for audit. Lean reads
   neither.

**Consequence:** two scores within 10⁻⁶ can become one rational, an exact tie, and Q11 refuses the
run. ebbbe42a's third score merges with the other two, which is the intended direction.

**Residual:** rounding can also split a near-tie that straddles a rounding boundary, giving two
rationals 10⁻⁶ apart from values 10⁻¹⁶ apart. The chance is about gap/scale per pair, so 10⁻¹⁰ at
the noise we have seen. Clustering in the projector (values within one scale unit take the same
rational) closes it at the cost of more code. I would not add that for a 10⁻¹⁰ event.

## Answers to the packet's questions, short form

| # | Answer |
|---|---|
| 1 | All present except `action` and a key for `pool`. Projector reads a dead key |
| 2 | Derive from the policies' nodes |
| 3 | `SC :candidates i :g`, doubles. Ids need joining. The law then sums by first action |
| 4 | Not computed on the inner path |
| 5 | `:scoring` steps plus class-emission steps; exact ratios; same object the law scored |
| 6 | Absent |
| 7 | Decision ids match compared; `constructedCascades` does not |
| 8 | Present in 26 |
| 9 | Scores in 28, picks in 3, one prediction, the chosen's |
| 10 | Position of a no-pick cascade, in 3 records only; no separate value |
| 11 | Makeup pin only. Library, slice, cascade digests absent |
| 12 | Receipt computable; relation unknown in 11; reach defaulted in 28; consumed unclassified in 12; pending id absent when deferred |
| 13 | No selection-level previous decision; one digest, too wide |
