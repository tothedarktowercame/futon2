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
