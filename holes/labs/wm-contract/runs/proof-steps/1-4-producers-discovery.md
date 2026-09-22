# Proof ⟨1⟩4 discovery: the four producers (read-only)

zai-1, 2026-09-22. Sources read: `full_loop_runner.clj` :3697-3755,
`d_predecessor_task_authority.clj` (:283 produce!, :295 complete!,
:320-350 signed-observations), `token_outcome.clj` (:55-90 compare),
`run_ending_classification.clj` (:63-108 classify*, :149-156 verify-close),
`route_attestation.clj` (:125 receipt, :134 retain!), `focus_receipt.clj`
(:98 classify-target, :140+ build), `cascade_habit_reinforcement.clj`,
`learning_trial_ledger.clj`, `attempt_learning.clj` (:15-21
supported-contract?), the r4-1/r4-2 close records.

## 1. Post-build measurement of the wanted tokens

**Exists.** The measuring chain is real and production-shaped:
- `d_predecessor_task_authority/produce!` (:283) — claim → verify →
  write-claim!; `complete!` (:295) is the runner's entry, called at
  `full_loop_runner.clj:3700` (inside the :3697-3755 close-assembly
  block) with the artifact binding and files from the build checkpoint.
- `signed-observations` (:320-350) resolves each wanted token against its
  declared locator (C3/C4) at the AFTER revision, producing the
  after-token evidence; `token_outcome/compare` (:55-90) then compares
  predicted vs observed with EXACTLY the failure vocabulary the step
  needs (`:measurement-unavailable`, `:artifact-revision-mismatch`,
  `:observation-missing`).
- **Why r4-2's trial came back :missing (r4-3 STAGES table: "0 of 2
  (:missing)"):** the r4-2 close is a REVISION-REFUSAL close — the author
  refused with `:artifact-binding-scope-conflict`, so there is no reviewed
  after-revision to measure against; `signed-observations` requires the
  dispatch/claim inputs (`complete!`'s route even names
  `:binding-not-retained` / `:incomplete`). With no accepted artifact
  revision, every wanted token's row is absent →
  `:measurement-unavailable` → `:observation-missing`. The producer is not
  broken; the close it was measuring never produced an artifact to
  measure.
- **Exercisable read-only on r4-1/r4-2?** r4-1: YES — machinery-69's
  attempt-002 retained a grounded after-revision (its run record carries
  10 true / 15 false measured observations). r4-2: only the refusal path
  (which is itself worth pinning: measured-false vs missing distinction).

## 2. Run-ending attestation and focus relation

**Exists, with one wiring gap.**
- The attestation producer is `route_attestation.clj`: `receipt` (:125)
  builds `:wm/route-attestation-v1` from the dispatch events and the
  artifact binding; `retain!` (:134) writes it. `classify*`
  (`run_ending_classification.clj:63-108`) consumes it: an increment is
  `qualifying` (:59-64) only when the route attestation carries a
  matched `:increment` criterion row with `:present` attestation status.
- The focus relation: **classify-target IS now the producer's classifier**
  — `focus_receipt.clj:140+ build`'s per-candidate rows go through
  `classify-target` (:143/:164, with the relation-context and
  classifications overrides from build 2's handoff A/B). So the close
  side and the scoring side share one relation producer as ruled.
- The gap: classify* requires BOTH an attested increment AND a focus
  row; the focus receipt's rows come from
  `[:selection-certificate :candidates]` — present when the decision
  carried the new wiring, absent on historical closes. For a live ⟨1⟩5
  click both will be produced; on r4-1/r4-2 (historical) the class is
  :unknown with `:attested-increment` / `:commit-facets-v1-relation`
  reasons — exercisable read-only as the refusal semantics, not as a
  positive attestation.

## 3. Accepted-increment predicate

**Partially exists; acceptance is the weakest of the four.**
- What exists: `verify-close` (`run_ending_classification.clj:149-156`)
  checks the CLOSE RECORD's integrity only — the recorded classification
  receipt exists, its projection matches the close judgment, and the
  digest pins it. It verifies the RECORD, not the WORK.
- The acceptance meaning inside classify* is `qualifying-increments`
  (:59-64): an increment criterion row in the route attestation, matched
  by the artifact binding (route_attestation's `match-binding` :41),
  attested `:present`. The route attestation binds the dispatch's
  declared criterion to the observed after-revision — that IS the
  "reviewed commits bound to it" half, because the attestation's source
  events (:34) name the binding's commit.
- What does NOT establish acceptance today: (a) no function asserts
  "the TARGET'S EXISTING ACCEPTANCE" (the mission's stated acceptance
  criteria) was met for this occurrence — the route attestation asserts
  the dispatch's criterion, which the author's prompt derived from the
  cascade, not from the mission's acceptance section; (b) ticket review
  ("mark this ticket DONE only when its scoped work is accepted",
  ticket.md) has no mechanical predicate at all; (c) `verify-close`
  passed on r4-2's failure close (click-3's known defect) — it validates
  recording, and says so. **The missing piece is a producer that reads
  the target's own acceptance declaration and binds the reviewed commits
  to it — the scope-B3 style predicate — which does not exist.**

## 4. B update after an accepted close, keyed by occurrence id

**Does not exist.** Confirmed again: `learning_trial_ledger.clj` has no
production reader; `attempt_learning.clj:15-21 supported-contract?` still
pins `:mode :record-only`, `:authority :declared`; the single trial in
attempts.edn is `:consumption :not-authorized`; the contract amendment
authorising production consumption (⟨1⟩3's ⟨2⟩1 ruling path) is written
into the proof plan but the RESOURCE file
`resources/wm/attempt-learning-contract.edn` is still the record-only
version. Habit reinforcement (`cascade_habit_reinforcement.clj`)
updates the habit prior from selection events, not from measured
outcomes. Nothing anywhere writes "B updated once, keyed by occurrence
id, after an accepted close".

## Smallest build order

1. **B-update contract + reader** (producer 4) — smallest and unblocks
   ⟨1⟩8: amend the contract resource (Joe's plan signature is the
   authority), add the production reader + the exactly-once
   occurrence-keyed write. Testable read-only against attempts.edn.
2. **Accepted-increment predicate** (producer 3) — the real gap; reads
   the target's acceptance declaration (mission completion criteria /
   ticket acceptance section), binds the reviewed commits, refuses typed
   when the declaration cannot be checked. Exercise the negative paths
   read-only on r4-1 (whose increment IS attested) and r4-2 (refusal).
3. **Measurement + attestation on the reference target** (producers 1-2)
   — exist; wire them into the reference ticket's close path at ⟨1⟩6-7
   and pin their r4-1 read-only behaviour now (positive measurement,
   attestation semantics) while building 2.

The live ⟨1⟩5 click cannot precede 1-3 of this order; 1-2 can be
exercised read-only on the real closes today.
