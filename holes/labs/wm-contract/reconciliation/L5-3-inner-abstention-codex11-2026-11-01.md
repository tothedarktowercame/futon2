# L5-3 — stopped policy under each inner policy's retained model

Discovery/prototype only, 2026-11-01. Source inspected at futon2 bb9bcebaa
(main, later than requested 4c59a7b64). No production change, live evaluation,
click or data write. Scratch namespace: ~/notes/l5-3/prototype.clj.
Run: clojure -M /home/joe/notes/l5-3/prototype.clj from futon2. Exit 0.

## Result and population

All **25/25** historically chosen policies strictly beat their own stopping G.
Across **61** compared policies, **0** recorded prediction digests equal the
chosen reference's stopping digest; **0** equal their own stopping digest.
This does not repair or certify the historical selector's chosen policy.
Chosen means [:decision :chosen :id], joined to SC :candidates, not argmax.

Population exactly matches L5-1: tick records dated 2026-10-29 through 11-01
with :outer/selection, nonempty SC :candidates and SC :scoring. Asserted 25.
results.edn retains all 25 source file SHA256s, all 61 per-policy results,
model digests, step predictions, normalization declarations and exact doubles.
Table values below are displayed to nine decimals; comparisons use doubles.

## Prototype A: same reference, no action

score-stop-channel calls the existing
cascade_observation_scoring.clj:295 rank-cascade-actions, with an empty vector
precedence but the original candidate's target, retained progress model and
class-emission model, universe, horizon, preferences and acceptance.
The reference initial belief is checked to equal {#{} 1} in every retained
progress certificate's :consumed-g :D. It is not re-estimated from today's data.
Every rollout step applies no operators (scorer :199–220), leaving that belief
unchanged. :prediction-only? true skips conditioning on a fictional observation.
The class channel uses its retained :acceptance as cascade-spec :want;
progress uses its retained :want. Neither model is rebuilt for an empty policy.

Terms use the existing per-step-capacity-and-pattern normalization (:66–81).
Risk = progress risk + class risk; ambiguity = both channel ambiguities plus
zero no-action fit ambiguity; information gain = 0. With no actions there are
no learned transition parameters sampled. Pattern-count floors at 1, but its
only divisor is information gain, which is zero; original horizon and outcome
support remain intact. Class emits unmeasured before horizon and stop-the-line
at horizon, asserted for all 61 references. This is the existing model's class
semantics, not an invented all-horizon stopped emission.

Fit choice is explicit: cascade_shape_g.clj:524–534 defines fit-ambiguity zero
for no nodes; the prototype follows that no-action case. It does not attach the
acting arrangement's fit penalty to stopping. F (observation fit/free energy)
is distinct from G and is not fabricated for a nonexistent observation.
Existing score-policy (:575–587) adds acting node fit-ambiguity to G; the table
uses that retained chosen G. If a future model charges a separate cost for
inaction, that is a new modelling decision, not a recovered term.

## Why [0 true] was wrong, and the counter-control

observation_model.clj:274–277 computes [completed-count (subset? want state)].
A fresh empty arrangement gives empty terminal acceptance
(cascade_shape_g.clj:307–324), so subset?(empty,empty) is true: a vacuous
structural acceptance, NOT evidence that the task's real want is met.
A retains each original NONEMPTY want. All 61 stopped predictions are exactly
{[0 false] 1} at every step. A counter-control deliberately replaces the
reference want with empty and reproduces {[0 true] 1}; controls.edn records it.
Thus A avoids the false success by retaining the original question.
A genuinely satisfied initial state could predict success under no action;
this corpus instead retains empty initial states and nonempty wants.

## Twenty-five-row comparison

| Run | chosen G | stop G under chosen model | Beats | Equal predictions | Stop digest |
|---|---:|---:|---|---:|---|
| tick-run-record-2026-10-29-0d8a61f7-abcb-4599-a1f1-e2387bd42ba2.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-347b9b51-637d-4291-9448-f2dcecedd748.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-376d76ff-75dd-4a2b-a42b-058883554cb6.edn | 1.658428923 | 4.531889162 | true | 0 | ab26331e743a32544f96bd8ede35bbeb3e91d8a542380108711cb8b3c3973a13 |
| tick-run-record-2026-10-29-39a845a3-496a-4bce-888c-e8b0ca2835ab.edn | 2.451686309 | 3.566813854 | true | 0 | ef7a2108771a2536ed898e299b3480fe272cdf50322452debe99bee71172445f |
| tick-run-record-2026-10-29-43ec20a7-c3ba-4726-a43c-a827b2b3e66d.edn | 1.658428923 | 4.531889162 | true | 0 | ab26331e743a32544f96bd8ede35bbeb3e91d8a542380108711cb8b3c3973a13 |
| tick-run-record-2026-10-29-4438ee79-44ec-4982-aba3-4107c477902e.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-5ba921b9-0431-4b85-95a7-6606a01111b8.edn | 1.658428923 | 4.531889162 | true | 0 | ab26331e743a32544f96bd8ede35bbeb3e91d8a542380108711cb8b3c3973a13 |
| tick-run-record-2026-10-29-8bb5e6dd-087a-46b1-beb9-bda595f8cc07.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-9e2103c1-c48f-496f-b8d5-2421fb7f68f0.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-b1a049d7-d4f8-4f63-be69-6872d86e8827.edn | 2.132329626 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-10-29-e0b9aab6-3197-441a-87bd-d49c750387ca.edn | 1.658428923 | 4.531889162 | true | 0 | ab26331e743a32544f96bd8ede35bbeb3e91d8a542380108711cb8b3c3973a13 |
| tick-run-record-2026-10-29-ff4b4d5c-0193-4162-9075-3ff1591b150d.edn | 1.471282505 | 2.573832661 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-30-3415b671-88f6-4662-a2c1-48e3630f6116.edn | 2.341993843 | 2.729834750 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-30-e5f08d91-2fb0-40e2-b367-5c4ed436c3dc.edn | 1.734652708 | 2.695591369 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-0ba3370c-1cc7-4f7d-8537-7edbf6a9ba9c.edn | 1.734652708 | 2.695591369 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-0be4fc7d-70db-4e96-8beb-ea24a1867896.edn | 1.755458928 | 2.555932231 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-4955b6e6-b818-42ea-b00e-7354a71d88e2.edn | 1.734652708 | 2.695591369 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-adc46631-2156-4c84-850f-5abaa3aeb5fd.edn | 1.755458928 | 2.555932231 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-d5b9b513-55ee-484c-81d5-065846b336bc.edn | 1.754970404 | 2.510214485 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-e71c7380-2fc2-41ac-b7f9-c226c27e395e.edn | 1.754970404 | 2.510214485 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-ebbbe42a-9889-4bff-921d-0843ea85a699.edn | 1.734652708 | 2.695591369 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-f4b95abf-6e0f-48d0-bf75-83d5662b43be.edn | 1.754970404 | 2.510214485 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-10-31-f805ca4b-6c35-45cf-accc-f95dd54b0fc8.edn | 1.734652708 | 2.695591369 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |
| tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn | 1.635918623 | 2.863621601 | true | 0 | 23e69f2566814eafb5a6fe2796db25bc69a3bf9d3369339931323d2cbc4d8c6a |
| tick-run-record-2026-11-01-c5590f95-a22e-4f21-a5e2-2cf47f393803.edn | 1.754970404 | 2.510214485 | true | 0 | ec233e589760f8bc4e0d9bd6512a786f27bceab23c09da0e72c9c2d4bd4ce9dd |

## Proposed retained carrier and readers

SC :abstention {:per-policy {policy-id {:G :prediction-digest :terms
:reference-model-digests :horizon :normalizations}}
:chosen-reference policy-id}. Retain full progress/class predictions, or
content-address them alongside the models, for independent digest replay.
Version the no-action/fit rule. Reference an arrangement's exact model even
when it belongs to a prediction class: class equality alone does not prove
the models used to score stopping are identical.

Producer integration points (not changed):
- cascade_shape_g.clj:274 score-arranged / :548 score-policy own the models.
- wm/family_selection.clj:45 scored->ranked and :319 score-policy invocation
  carry the per-policy stop entry with the acting candidate.
- wm/cascade_decision.clj:1294–1404 assembles/augments SC; attach all entries
  and the actual enacted policy reference after selection.
- scripts/wm_run_facts.clj:427 candidate-prediction-row uses the same
  cascade-selection/prediction-digest as this prototype (progress + class
  step distributions); :661 currently emits the work-run stopped-kernel
  absence. Read chosen-reference here for abstentionG/abstentionPrediction,
  preserving typed absence for historical records.
- scripts/wm_check_run_facts.clj already renders these scalar/digest fields;
  Lean Requirements Q11 consumes them. Those interfaces need no new meaning
  for a chosen-policy comparison; tests/projector carrier validation do change.
- decision_gate.clj should verify the chosen-reference join and terms before
  admitting a future certificate; trace/report consumers must preserve it.

A does NOT solve the abstain-on-task branch's demand for one stopping G below
every acting policy: all those policies have different lenses. Choosing a
reference after a decision to stop needs its own explicit rule. Do not select
the most convenient stopping score or silently treat all per-policy stop
scores as interchangeable. No Lean Q11 closure claimed by this experiment.

## Shape B: shared terminal preference

Build a shared task-outcome space for completion, then conditional relevance,
and conditional gain, with retained authorization/calibration and terminal
receipt semantics from FactorizedPreferenceSemantics / OuterFactorizedPreference.
Score every acting policy and stopping against that same C; stopping predicts
not-completed, not vacuous success. A shared terminal term could supplement
progress terms, but changes G and potentially the selected ordering.
A finite weighted sum does not automatically enforce ordinal completion-first
priority; the combination law and scale must also be declared and checked.
The current flat kit is not the factorized certificate. No new weights chosen.

A is cheap and gives the minimal chosen-vs-stop comparison now (two bounded
no-action rollouts per existing policy). B addresses global comparability but
requires new shared terminal predictions, calibration and a composition law.
The 25 wins are model-relative results, not an empirical validation of either.

## Evidence and limitations

Scratch artifacts contain only generated analysis; canonical data is read.
One initial prototype run refused its class-channel options because that model
uses :acceptance, not :want; final code supplies the retained acceptance and
reruns all 61 references successfully. No success was inferred from that failure.
No production tests run: no production code changed. This is a prototype over
sealed historical inputs and current scorer code, not a live admission witness.

Artifact SHA256 pins (~/notes/l5-3/):

- prototype.clj: 91460418ba21c386622adde2488c01b9fd3572ff43ffdcecc14aef391c6dfb1b
- results.edn: ff793e2424f1a8d32a4504cc0e9181e6b4517e1c422eced86c5252cc6f0d2f99
- run.log: b7198a2fecf6de0ac9eb139a961e4249df07565671a04ed657468295643f5c2e
- controls.edn: 1a1c3dbaa185b87493ae422d09ce822f75b9d77f5eb4679946a7c369c8aeacf1
