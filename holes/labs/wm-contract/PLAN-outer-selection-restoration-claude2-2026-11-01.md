# Plan: restore task selection to the inner loop's criteria (held by claude-2, 2026-11-01)

Joe, 2026-11-01: "move to fully lean led validation, requiring lean validated conformant run
receipts throughout. Delete the bad special casing in the outer loop lean and replace it
with the same AIF criteria as the inner loop, just applied to task selection. make any
cheating, facades, special cases, ties, restricted domains of operation, empty cascades, and
any other such nonsense subject to tripwires." Precedence was never a licence to downgrade
criteria; it should have raised them.

Evidence: INCIDENT-outer-selection-{Q1-Q2,Q3,Q4}-claude2-2026-11-01.md in reconciliation/.

Roles. claude-2 holds this plan, reviews each slice on bell-back, and builds the witnesses
by hand. claude-3 implements the Lean slices from packets in packets/. No ordinary click
until a run's selection RunFacts and inner RunFacts both conform (Joe). zai-3's
outer-facade-prohibited branch is superseded and is not merged.

## Principle

One loop, one set of requirements. A click has two runs of the same kind: a selection run
(policies are task-selection cascades from futon3/library/task-selection, field is the
open tasks) and a work run (policies are tactical cascades for the selected task). Each
is a `RunFacts`; each must `conform`. Lean states the requirement first; the projector
then reads the record into it; Clojure then changes to produce a record that conforms.
Agreement between a Lean model and the code it models is not validation.

## Slices (Lean first, one packet each, review between)

**L1. The selection run as a RunFacts (Requirements.lean).** Restore the deleted Q2
clauses for the selection kind: exactly one `TargetConstruction` for the selection, with
non-zero pins (reading = Giraffe pass, library = task-selection library manifest, graph =
Giraffe graph, kernel = makeup), `pool = slice`, `sliceFromWholeLibrary = true`,
`slice.card ≤ libraryPatternCount`, `0 < policyCount`, `policies = constructed`,
`comparedPolicies ⊆ policies`, and at least two distinct compared cascades. Q1 for the
selection: enumerated = open. Negative witnesses decided by `native_decide`: a hand-listed
menu (library pin 0), three patterns not drawn as a slice, a one-cascade menu, the actual
outer-makeup shape. `good` still conforms. Packet: packets/L1-selection-runfacts.md.

**L2. Strict minimum and distinct predictions.** Carrier for per-policy G and for the
abstention G; Q4 gains: chosen G strictly below every other compared policy's and below
abstention's; compared policies' predicted outcome distributions not all equal; an exact
tie is a violation, not a disclosure. Template: the deleted
`OuterCascadePolicyObjective.SelectionCertificate.Valid` and `exact_tie_invalid`
(`git show 3a09d2a80f^:DarkTower/WarMachine/OuterCascadePolicyObjective.lean`).

**L3. Absences on the selection path.** Q7 counts the selection receipt (typed absent in
every record to date), an unknown relation on the selected pick, and a defaulted reach.
Learning counts as applied only with a classified ending; otherwise it is a refusal.

**L4. Projector and alert (Clojure, scripts/).** `wm_run_facts.clj` reads `:outer/selection`
into the selection RunFacts; both RunFacts go to Lean; `wm_run_alert.py` drops
`PARTIAL_REQUIREMENTS` and the default-to-holds; `unverifiable` fails.

**L5. Selection code (Clojure, src/futon2/aif/outer_*).** Menu built from the pinned
library, not from `outer-makeup.edn`; reach observed or typed absent; relation unknown
typed absent; a tie refuses; the no-pick cascade is scored as abstention and compared; a
classification receipt is produced for every consumed run.

## Tripwires (each a decided negative witness in Lean and a refusal in Clojure)

façade: menu not drawn from the pinned library, or any zero pin · tie: equal G without a
declared tie law · restricted domain: enumerated ≠ open · empty cascade: a no-pick
policy scoring equal to a pick · default as parameter: reach or relation supplied from a
file · learning without observation: "applied" with classification-receipt-absent ·
fixture in production: a run record whose enumerator is a fixture written to data/wm-runs
· fail-open checker: a Q missing from Lean's output read as holds.

## Witnesses (claude-2, by hand)

1. A conformant `:outer/selection` section on the real 2026-11-01 field: Giraffe run
   20261101T044121.422Z-730407c5 (609 items), registry open tasks (626) reconciled item by
   item, at least three real selection cascades from the README with library pins, each
   applied to the field, each with a distinct pick and prediction, per-policy G with
   terms, abstention G, a strict minimum.
2. A Lean `RunFacts` instance for the selection kind that `conforms`, and one negative
   instance per tripwire.

## Status, 2026-11-01 07:35Z (claude-2)

| Slice | State | Where |
|---|---|---|
| L1 selection RunFacts, Q2 restored | merged | mathlib4 `3a98ab3029` |
| L2 strict minimum (Q11) + review fix compared = constructed | merged | `a1716abc9d`, `11d9dc6852` |
| L3 typed selection path in Q7 + review fix pendingRun | merged | `719a18641e`, `1971aeaf55` |
| L3b abstention as a typed Decision (Joe's two cases) | merged | `05d3760cea`, `55878e243a` |
| L4a checker checks every Q, alert fail-closed | merged | futon2 `c97a0e7d6`, `40c15a173` |
| Decision rendering in the checker | merged | futon2 `641719d4c` |
| L4b carrier map (discovery) | committed | futon2 `f30ee5f7b` |
| L4c-1 work-run construction + G carriers + review fix (Q3 id space) | merged | futon2 `56b3df01c`, `0d2232fe3`, `c3c5279b6` |
| L4c-2a selection-run export | merged | futon2 `76228b319`, `900548ec7` |
| L4c-2b alert over both runs, conforms iff both | merged | futon2 `6b6c9b4d8`, `e3f906706` |

Live effect: every click now produces two Lean-read RunFacts and a two-run verdict; on all
era-2 records the click alerts (work: Q7/Q9 violated or Q1; selection: Q7, Q9 violated; the
rest unverifiable with a typed reason naming the L5 producer gap).

L5 is blocked on three rulings (Joe): (1) inner selection law, policy argmin vs first-action
marginal (12 of 13 multi-policy runs enact a non-minimal policy); (2) repeated identical-input
abstention on a task (Q6 extension, field exclusion, or selection-run violation; I recommend
the third); (3) a policy excluded by Q6 before comparison fails Q3 and Q11 (d5b9b513, c5590f95);
a typed exclusion carrier, or the refused policy stays compared and Q6 forbids choosing it.
Clicks remain held; no Zai dispatches; no selection code changed.

## L5 slices (after Joe's three rulings, 2026-11-01 11:48Z; record:
reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md)

- **L5-1 discovery (inner law).** Code path from posterior to enacted policy; tests pinning the
  first-action marginal; how "extensionally distinct" is computed today; where Q6 removes a
  policy before comparison; what a stopped-kernel G needs on the inner path.
- **L5-2 inner law.** Enact the strict G minimiser over extensionally distinct cascades; an
  exact tie refuses by name; the Q6-refused policy stays compared and Q6 forbids choosing it;
  `:decided-by` says what ran.
- **L5-3 inner abstention.** Stopped-kernel G and prediction on the inner path; abstain on the
  task when stopping is the strict minimum (Joe's second case); the records carry both.
- **L5-4 outer producers.** Task-selection library pin, slice and pool from the pinned library;
  per-cascade predictions and terms; abstention scored separately; reach observed from retained
  records; classify-target receipt digest retained; the `:outer-task-selection` stub removed; a
  selection-level previous decision and a choice-scoped input digest; `:open`/`:active` as
  exclusion reasons resolved (restricted domain or mislabel).
- **L5-5 Lean.** Selection-run violation for re-picking a task that abstained on unchanged
  inputs (ruling 2); a named refusal when stopping beats every cascade on a non-empty field.
- **Then:** Lean prediction before a click; the click hold is lifted only when a click's two runs
  conform on a real field.

## Grounding slices (Joe, 2026-11-01 ~12:55Z: `selectionGood` certifies the shape with a façade;
real conformance must show the field was considered and prioritised; "if we can certify a shape
and also the non-facade qualities, great")

Shape (Q1–Q11) is checked over facts a projector asserts. Grounding adds carriers from which Lean
*recomputes* the selection and checks the record against its own facts: internal consistency in
Lean, pins verified against the pinned files by the projector. A façade then needs forged pinned
files, not just asserted numbers.

- **L5-4L-1 Lean `SelectionGrounding.lean`.** Carriers: the field as one entry per enumerated
  task (Giraffe fact with pin, or typed exclusion with reason and pin); per policy its steps,
  recorded pick, reach, relation class, terms and G; abstention's terms and G. Requirements:
  G1 field considered (every enumerated task once, every pin non-zero); G2 prioritised (the
  recorded pick is the head of the steps applied to the field, R20Step semantics; a step Lean
  cannot recompute makes G2 unverifiable, not holds); G3 scored from terms (G = risk +
  ambiguity − novelty exactly, reach in [0,1], a pick's relation measured); G4 agreement with
  RunFacts (same policy ids and G values). Small hand fixtures plus negatives: a missing task,
  an unpinned fact, a menu-order pick, a G not equal to its terms, a pick on an unknown relation.
- **L5-4L-2 the witness is the fixture.** Extend `witnesses/build-outer-selection-witness.bb`
  to emit `SelectionGroundingWitness.lean` from the pinned Giraffe pass, the registry open set
  and the three README cascades: 626 entries, 3 policies, real G; theorem by `native_decide`.
  This replaces the question of restating `selectionGood` from the witness.
- **L5-4 producers** then make the live `:outer/selection` carry these facts, and the projector
  supplies them, so Lean's G1–G4 run on every click beside Q1–Q11.

## Ruling 4 adjustments (2026-11-01 13:35Z; record: RULING-… §Ruling 4)

- **L5-4b (producers, second).** Remove the `all-satisfied-exclusion` from the outer field (a task is
  in the field iff it is open; lifecycle status is the task fact). Define reach as a task fact for
  every field member from Giraffe/dependency-reading facts; click history is an added observation,
  never required. Typed exclusion reasons only from the task-level set. Remove the
  `:outer-task-selection` stub. Field facts with record pins carried in `:outer/selection`.
- **L5-4L-3 (Lean, small).** Closed set of task-level exclusion reasons; a policy-derived reason
  (e.g. all-policies-already-satisfied) fails; grounding docstring names the invariant.

## Status, 2026-11-01 14:20Z (claude-2): L5 producers so far

| Slice | State | Where |
|---|---|---|
| L5-1 inner law discovery | report | futon2 `e7b5af803` |
| L5-2L Q6 class, Q11 admissible set + checker fields | merged | mathlib4 `221609fe66`; futon2 `518d03631` |
| L5-2C-a / a2 inner law (argmin over classes) | in review (claude-3, running) | branch `claude3/l5-2c-a-inner-argmin` |
| L5-4L-1/2 grounding module + real-field witness fixture | merged | mathlib4 `94d816792e`, `5025ad1263` |
| L5-4a per-cascade scoring, abstention scored, tie refuses | merged | futon2 `30d5585e8` |
| L5-4b field = open tasks, pinned facts, reach prior; policy-derived exclusions gone | merged | futon2 `3c0d41bf8` |
| L5-4c stub removed; reselection forbids | merged | futon2 `fac22f476` |
| L5-4d menu constructed from the pinned library (+ mapping v2 in futon3) | merged | futon2 `743209eca`, `943e595ae`, `5435f5266`; futon3 `56a6e4d` |
| L5-4d-5 classes on the outer path | merged | futon2 (this merge) |

First selection replay on the retained 11-01 field with the live relation context: chosen class
{only-what-can-start-now, giraffe-order} → M-pattern-retrieval-calibration, G 2.768528 < abstention
4.787492; Lean: Q1 holds, Q2 holds, Q3 holds, Q9 violated (offline empty seat set), Q4/Q5/Q6/Q7/Q11
unverifiable (no selection-level previous decision/input digest, no Q4 terms projection, no Q5 carrier,
no learning status on a replay). Relation coverage: 92 of 626 (FINDING-relation-coverage).

Next: L5-2C-b (refused stays compared, inner); L5-4L-3 (closed exclusion set; grounding at class grain);
L5-4e (selection-level Q6 carriers: previous decision + choice-scoped input digest; Q4 terms projection
from the recorded class terms); relation producer coverage (Joe's two questions); L5-5 (re-pick after
abstention). The live makeup migration and the mapping file await Joe's eye.

## Noted 2026-11-01 15:10Z (from L5-2C-b review)

- **Q6 remembers one step.** Lean's `forbidden` derives from `previousDecision` alone, so A refused,
  B chosen and refused on the same inputs makes A admissible again (ping-pong). Follow-up
  **L5-2L-2**: a `refusalHistory : List (Id × Id)` carrier (cascade, input digest) and `forbidden`
  over every entry whose digest equals the current one; producer retains the history per input
  digest; projector emits it.
- `canonical-exclusion` (the `:canonical-coapply` path, not live on era-2) still removes keys and
  writes `:removed`; align or retire with L5-5.
- Click-path Q6 fixtures on `main` produce one candidate and cannot reach a repeat; the pins are at
  selector, carrier and family level on real policies. Fix the fixture separately.

## Noted 2026-11-01 17:05Z (from L5-2C-c review, futon2 `6a3753d94`)

- **Tampered history is invisible to Lean.** The producer counts refusals whose terminal seal is
  broken as `:unsealed` and does not trust them; a broken seal therefore shrinks the history, and
  Lean's consistency clause protects only the latest refusal. Follow-up **L5-2L-3**: a
  `refusalHistoryUnsealed : Nat` carrier; Q6 requires it to be 0 (a corrupted refusal is an absence
  on the path, not a smaller history); projector emits the producer's count. Dispatched to claude-3.
- **A refusal outlives a later success on the same inputs** (L5-2L-2 reading): a one-policy route
  that was refused refuses until its inputs change. Consistent with Joe's Q6 rule; note that it is
  the inputs digest, not time, that frees a task.
- The real lineage on M-pattern-retrieval-calibration has two refusals of the same policy on two
  digests (e71c7380, f4b95abf), not two classes on one digest; the ping-pong is pinned on a fixture.
- Click-path Q6/Q4 fixtures on `main` still produce one candidate (since L5-2C-b); separate repair.

## Status, 2026-11-01 15:13Z (claude-2; first headed 17:55Z, written ahead of the clock; corrected at 16:20Z against commit 1e5228b70)

| Slice | State | Where |
|---|---|---|
| L5-2C-a/a2 inner law: argmin over prediction classes; tests cannot write canonical data | merged | futon2 `c22814975` |
| L5-2C-b refused policy stays compared; class forbidden | merged | `6aad8b90d` |
| L5-2C-c per-task refusal history retained; union forbidden | merged | `6a3753d94` |
| L5-2L-2 Lean refusal history; Q6 forbids every class refused on these inputs | merged | mathlib4 `d3e0d9724f`; checker/projector `25e32d071` |
| L5-2L-3 Lean unsealed history = Q6 absence | returned, review pending | claude-3 |
| L5-4e selection Q4 terms, choice-scoped digest, previous selection | merged | `9b89c7000` |
| L5-4f immutable selection decision chains every click; selection-only alert | merged | `42c6a3953` |
| L5-4g relations for E-/T-; declared embedding floor | merged | `bdd2dcaa4` |
| L5-4h selection Q5 from the phase log (default path) | merged | `dfc979b71` |
| L5-4i embedding coverage discovery: not the lever | report | `6cf534346` |
| L5-4j relation over the pinned Giraffe graph (251 of 626) | merged | `25cc3b806` |
| L5-4L-3 Lean: typed exclusion reasons, class-grain G4, outer factorized certificate | returned, review pending | codex-11 |
| Stale selector tests after the makeup migration | repaired | `18007aa29` |

Correction for the record: the live `data/wm-learning/outer-makeup.edn` lost its `:cascades` key
on `main` at 13:35Z through the merge of L5-4d-2 (`c70b31779` carried the migration as a tracked-
file change). Intended end state; not an operator edit; sha now `db05f30c…`. The selector has not
read that key since L5-4d-2.

Selection run, Lean, on the retained 11-01 field (two-click replay with the live context): Q1–Q4,
Q6, Q11 holds; Q5 holds through the alert from main; Q7 unverifiable on a replay (learning status),
Q9 violated against the offline roster. Work run on the real records: Q7/Q9 violated or Q1, Q11
unverifiable until the inner stopped-kernel G (L5-3).

## Finding 2026-11-01 18:30Z: eight unsealed terminals in the live run stream (claude-2)

With L5-2L-3 wired (mathlib4 `b3fcae050f`; futon2 `68aed91c0`), Q6 requires the stream's count of
unverifiable terminals to be 0. The live stream (`data/wm-runs`, 264 events, 124 closed, 36
refusals) has **8** closed terminals whose seal does not verify. All eight are 2026-10-22 to
2026-10-24 and carry no `:sha256` at all: written before terminals were sealed, not tampered
(`:outcome` invalid ×5, refused ×2, already-satisfied ×1). The producer's `intact?` cannot tell a
never-sealed terminal from a mismatched one, so both count as unsealed, and the next real click
would read Q6 violated on every task until the eight are retired or re-sealed. Clicks are held, so
nothing is blocked today, but the halt is real and the stream is append-only.

Resolution (L5-6, claude-3): the producer distinguishes `:unsigned-legacy` (no seal key, written
before sealing began) from `:tampered` (seal present, digest mismatch); a medic route appends a
*sealed* retirement event for each legacy terminal (run id, reason `:unsigned-legacy`, the
retirement's own digest), after which the terminal is excluded from both the history and the
unsealed count; a tampered terminal cannot be retired this way and keeps halting. The retirement
is recorded in the injury ledger as the medic's act. Lean needs no change: the carrier's count
is "unverifiable after retirement".

## Correction 2026-11-01 19:05Z to the 18:30Z finding (claude-2)

The eight terminals are not unsealed. Each carries a `:sha256` that equals the value digest as the
serving JVM printed it before R18 (`981479974`, 2026-10-26 06:19Z, `*print-namespace-maps*` false);
my probe tested a `:seal` key that never existed and read its absence as "no seal". claude-3's
L5-6 (futon2 `f572dfb3a`, merged) verifies pre-R18 seals under the printer that made them, accepts
that printer only for closes before the R18 commit time, counts only `:tampered` as unsealed, and
exposes `:sealed-before-r18` as its own count (projector `refusalHistorySealedBeforeR18`). Probe on
the live stream with the merged code: 264 events, 36 refusals, unsealed 0, sealed-before-r18 8.
No data was written; claude-3 declined the packet's live retirement for three stated reasons (the
stream reader admits only two event kinds; the medic ledger needs an injury and an authority
chain; futon2/CLAUDE.md bars a Claude session from writing run data without Joe's named task), all
correct, and the need had vanished. The two pre-R18 refusals carry no input digest and so forbid
nothing under any route. The medic route for a genuinely tampered terminal remains undefined and
is not needed today.

## L5-5 order (from claude-3's discovery, futon2 `b4e279b53`, 2026-11-01 19:40Z)

- **L5-5a (now):** the not-exercised records project and alert as what they are. D1 a not-exercised
  selection projects as a typed non-selection, not as an empty-field abstention; D2 the alert prints
  no work block for a run that did not exist; D3 the previous-selection reader skips not-exercised
  records (they are not selections) and records how many it skipped, so the next selection run's Q6
  carriers stay recomputable; D4 authority refusals and arbitration exceptions get their own path
  labels; D5 the direct-task record names the task. A click without an exercised selection does not
  conform, by name.
- **L5-5b (discovery):** the direct task entry as a selection: the operator's instruction is an
  observation with a declared source (who, when, the words) entering G through the relation; one
  added cascade on the same field, winning only by G; the blocker is that same-prediction classes are
  broken by menu order, so an operator-recognition outcome factor (kernel version change) is needed
  first. Urgency low: 0 of 290 acceptances carried a task.
- **L5-5c (self-heal into selection, in order):** publish a ticket when an injury opens with the join
  key on the finding (today 0 of 299 findings carry the pin, so no injury reaches the field); then
  the obligation fact and the `repair-goes-to-the-front` step; row 3 of the README as a compared
  cascade; then arbitration ("an injury on the next candidate's path fires the gate", with "on the
  path" to be defined). The route stays reachable when selection itself is injured, as a typed
  non-selection that alerts.
- **D6, outside:** flights are a third operator-named entry whose runner writes no selection record.

## Status, 2026-11-01 16:06Z (claude-2; first headed 20:35Z, written ahead of the clock; corrected at 16:20Z against commit af7dd0b3f): Q11 reads holds on real work runs

L5-3-impl merged (futon2): the stopped policy is scored under each candidate's own retained model
with the real want (an empty want refuses `:empty-want-stop`); the reference is the enacted policy,
or the admissible minimum when the task abstains, recorded with `:cross-lens`; the selector refuses
a pick that does not strictly beat its own stop; the gate verifies the join; the projector emits
`abstentionG`/`abstentionPrediction` at scale with typed absence on records before the carrier.
Hand-built exports of 376d76ff and ac5bc477 through the merged inner law: Q11 **holds** on both
(chosen 1.403419 vs stop 3.937637; stopped prediction distinct from every policy's). Equality
with stopping is a named refusal, not a weakened Q11.

Also since 17:55Z: L5-2L-3 (Lean, unsealed count; wired), L5-6 (pre-R18 seals verify; the eight
"unsealed" terminals were my probe's error), L5-4L-3 and 3b (typed exclusion reasons, class-grain
G4, outer factorized certificate; witness regenerated by the emitter), L5-4j (relation over the
Giraffe graph, 251 → 212 of 626 at Joe's floor 0.2), L5-5 discovery (bypass paths; L5-5a in flight).

## Status, 2026-11-01 16:20Z (claude-2): L5-5a merged; bypasses now alert as bypasses

- **L5-5a merged** into futon2 main at `979bd09a0` (branch `claude3/l5-5a-not-exercised`, sha
  `a5946ace8`, base `c160b1945`). Review: diff read in full; the warrant shas match. Judgment on
  the five items: D1 every selection carrier on a not-exercised record is typed `.nr` with the bypass
  path, and `enumeratedTasks` stays `.nr` when the record holds no field (an empty list would read
  as the empty-field abstention in Lean); D2 `:workRunStatus` is `not-run` without a tick in the
  completion receipt, and the verdict carries `:selection-status`; D3 `selection-predecessor` walks
  back through `latest` (strictly-before, same-run excluded, so the walk terminates) and records
  `:previous-skipped`, falling to the sealed tick stream only when the decision index runs out behind
  a bypass; D4 the bypass labels are `:direct-task`, `:authority-refusal`, `:arbitration-exception`,
  `:meta-self-heal`; D5 the record names the task and its entry, with `:kind :api` only when the
  issuer provenance source is the HTTP boundary (futon3c `transport/http.clj` sets that source).
- **Tests are the right ones:** the bad cases named in the report (walk removed, label removed,
  `not_exercised` dropped from `conforms`) each fail a named test; the changed-record-behind-a-bypass
  test pins that the walk never passes a record it cannot verify.
- **Re-executed here** (worktree `~/worktrees/futon2-l5-5a`, hermetic root): the two new alert
  unit tests; `not-exercised-selection-test` and `selection-record-test` on the branch (20 tests,
  0 failures); on the merged tip `not-exercised-selection-test`, `wm-run-facts-test`,
  `wm-check-run-facts-test` (95 tests, 310 assertions, 0 failures), because main had changed
  `wm_run_facts.clj` since the base. A scratch authority-refusal decision record run through the
  real alert and Lean prints `work run: not-run` and `selection run: not exercised
  (authority-refusal)`, rc 1, `:conforms false`. Canonical `data/` unchanged at 45737 files.
- **Not run:** the full suite; no warrant minted (dirty tree, not invalid work).
- **Not yet loaded:** the serving JVM still runs pre-merge code for every namespace changed today.
- **Time correction:** two earlier status headings in this plan (17:55Z, 20:35Z) were written ahead
  of the clock; corrected above to their commit times (15:13Z, 16:06Z). The commit messages keep the
  wrong times.

## Status, 2026-11-01 16:42Z (claude-2): L5-5c-1 merged; an opened injury now publishes a repair ticket

- **L5-5c-1 merged** at `dd4e547b6` (codex-11, branch `codex11/l5-5c-1-injury-ticket-join`, sha
  `a68e75fef`, base `af7dd0b3f`; report
  `reconciliation/L5-5c-1-injury-ticket-join-codex11-2026-11-01.md`). Diff read in full. Behaviour:
  after a durable injury append (or a duplicate re-observation) the store publishes one repair
  finding and ticket joined to the injury by `:injury-id`, `:cause-record-pin` and
  `:origin-run/id`. A new finding carries the keys inline; an existing finding joined by injury id,
  exact cause pin or origin run keeps its bytes and gets an immutable attachment under
  `injury-joins/`, pinned to the finding's sha (my in-thread ruling, same append style as
  observations and dispositions). `injury_ticket_join/resolve-join` gives one answer for both forms,
  types a missing join absent, refuses a conflicting one, and reads a replaced finding as
  `:injury-join-finding-sha-mismatch`. Ambiguous matches refuse; failure-kind or target
  coincidence is not a join. The ticket receipt pins the finding sha, not the ticket text, so the
  appended provenance block does not break it. Publication failure after the append returns
  `:injury-ticket-publication-failed` with `:ledger-durable? true` and is retried by re-ingesting.
- **Tests are the right ones:** same injury ingested twice gives one ledger append, one finding,
  one ticket with identical bytes, and the field producer admits the ticket on a real Giraffe
  fixture; attachment and inline resolve equal; a newline appended to the attachment conflicts;
  tampering the finding makes the join and the medic closure read absent; an injected publication
  outage leaves the ledger durable and the retry publishes. Re-executed here (codex-11's worktree,
  hermetic): join test and medic-closure test, 10 tests, 50 assertions, 0 failures. Live store
  checked read-only: all 299 findings carry a `:repair/id` matching their filename, which the
  resolver's file lookup relies on. Canonical `data/` unchanged at 45737.
- **Follow-up fix, mine** (`a8fd0f6d0`): the runner's ingest-sink projection dropped
  `:ledger-durable?` and `:injury-ticket`, so a run record would have read a durable injury as a
  refused ingest and never named the ticket. Projection now keeps both (ticket as status and
  finding id). Kondo warnings unchanged at 7 on that file; no new test for the projection.
- **Premise correction (mine, from claude-3's L5-5 count):** the packet's "9 pinned injuries" came
  from the superseded `data/wm-meta-injuries/ledger.edn` (22 Oct). The live ledger is
  `data/wm-meta-injuries-live-2026-10-22/ledger.edn`: 144 entries, 23 injury ids, 22 open, every
  pinned cause record present. Codex-11's replay script rerun over it (read-only, no ticket
  written; `reconciliation/L5-5c-1-live-ledger-replay-*-claude2-2026-11-01.edn`): 21 would mint
  new inline findings, 1 (injury 7d5e374e, run `ac5bc477` today) would attach to existing finding
  `repair-occ-e14c140e…` by origin run. Would-be relations under the live producer: 7 associated
  via the hand-reviewed M-aif4iad row (targets M-stack-geometry, M-war-machine-wiring,
  M-reachable-from-boot), 7 embedding below floor, 4 embedding node not retained, 4 no parent.
- **Not done, by design:** the 22 existing open injuries get no ticket until re-observed; the
  store publishes at ingest. Whether to publish for them once is a one-time choice for Joe, not a
  backfill the code should do on its own. Obligation fact, repair-goes-to-the-front step and
  arbitration remain L5-5c-2/3.
- **Not yet loaded:** serving JVM.

## Status, 2026-11-01 16:56Z (claude-2): the three readiness items are done

- **Loop alert handling** (`b3cf25ce6`, `2dafeb2dc`): `wm_click_repair_loop.py --continuous`
  reads each click's two-run verdict beside the run record (by pattern, never defaulted; an
  absent verdict is an alert), records it, queues a non-conforming click to
  `data/wm-click-loop/alerts.jsonl` and bells `--owner` once, dispatches no repair seat, treats
  a conforming abstention as a decided click, reloads landed commits before every later click,
  and stops with a bell only when the same signature fails on two consecutive clicks. 17 loop
  tests pass, 4 new (verdict parse and signature; absent verdict; repeated signature pauses after
  two queued alerts and one bell; distinct alerts plus a lawful abstention keep clicking with no
  repair and a reload before each later click).
- **Serving JVM reloaded from main**: 78 click-path namespaces committed since the JVM started,
  16:53Z, via the loop's own reload (ok; ledger event recorded). Drift probe in the serving JVM:
  runner source check `:current`, composition status 0 defects over 115 namespaces. Focus inputs
  as served: floor 0.2, hop bound 3, graph link kinds and bases as declared. The dry-run preflight
  reports the tripwire wire status as not evaluated; that is its documented behaviour, so the
  probe above is the drift check.
- **First-click prediction**: `reconciliation/PREDICTION-first-continuous-click-claude2-2026-11-01.md`.
  Selection run predicted to conform; work run predicted to alert on Q7 and Q9 exactly as
  ac5bc477 did. The loop will pause at the second click unless those land in between.
- **Not run:** the full futon2 suite today; warrants not minted for today's merges.
- **Ready:** the hold on clicks is Joe's to lift; the command is in the prediction file.

## Status, 2026-11-01 17:25Z (claude-2): the hold is lifted; the machine runs

Joe, ~17:20Z: "yes, let's lift the hold. for te budgest, we can record 50 clicks available,
however for the strategy I think we should make continuous repairs as needed not a series of
trials. each repair should be logged and committed so that we have a trail of what has been
done. the old notion of criteria complete is superseded by moving to full lean compliance."

- **Budget:** futon3c `89173aa2e`, allocated 243 → 288, served 50 available after reload.
- **Loop started 17:25Z** as bg job `bg-1793553906494-213` (label `wm-continuous-loop`):
  `wm_click_repair_loop.py run --continuous --clicks 50 --owner claude-2`, cast wm-author /
  wm-reviewer / wm-repair-reviewer. Log `/tmp/futon3c-bg/bg-1793553906494-213.log`; ledger
  `data/wm-click-loop/ledger.jsonl`; alerts queue `data/wm-click-loop/alerts.jsonl`.
- **Strategy from here:** the two-run Lean verdict is the only criterion ("criteria complete" is
  retired). Each alert names a producer gap; the fix is a commit whose message carries the
  alert's signature, noted in this plan under a repair trail below; the loop reloads it before
  the next click. No trial framing, no per-click packets unless a repair is substantial.

## Repair trail (continuous running)

| At | Click / run | Signature | Fix (commit) | Loaded before click |
|---|---|---|---|---|
| 17:30Z | before click 1's verdict; from ac5bc477's work export | work Q7 violated (5 typed absences, all under `[:failure :detail …]`: the run's reviewer falsifier failed, so Q7 reports a real work failure, not an exporter gap; its injury 7d5e374e is the repair route) ; work Q9 violated (verified negative: the factorized-preference producer is deliberately absent from serving) | Q9: discovery packet `packets/L5-7-D-q9-factorized-preference-into-serving.md` → codex-11 | n/a |
| 17:28–17:30Z | clicks 1–2 (`wm-click-218e340d`, `wm-click-92f6e2fb`), loop paused on the repeat | loop read **verdict absent** on both. Real verdict (file beside the selection record): selection Q1 violated "0 enumerated / 645 open tasks", Q2–Q7, Q11 unverifiable, Q9 violated, work not-run. Cause in the record: `:selection-outcome {:status :refused :kind :makeup-changed}`: the durable outer state was pinned to the makeup at `6a36b6006` (sha f3125835…) and the live makeup is the L5-4d-2 merge's (db05f30c…, `:cascades` removed); `outer_state/read-state` refuses before the field is read. L5-4d-2 shipped the migration code and test but never ran it live. | **Repair 1 (data):** the module's own menu-removal migration run once: `migrate-makeup-pin` + `write-makeup-migration!`; receipt `outer-state.edn.makeup-migration-f65f8761….edn`, backup `outer-state.edn.pre-r20-169a7e66`, learning parameters equal, state reads `:durable` under the live makeup (click-count 12). **Repair 2 (loop):** `d0b603043` locates a refused selection's record by click id, the rule wm_click.sh uses for ownership; test pins clicks 1–2's shape. | loop restarted 17:34Z as `bg-1793554471742-217` (48 clicks); state file is read per click, no reload needed |
