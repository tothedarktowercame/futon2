# Criteria-completeness campaign: 50 ordinary clicks, 2026-10-27 to 2026-10-30

Authority: `E-wm-criteria-completeness.md` (Joe, 2026-10-27: 50 clicks on Zai
seats; success → next click, failure → self-heal, failed self-heal → repair
from outside, altering the algorithm where warranted). Budget 178→228
allocated; 227 consumed; **1 credit remains**. Per-click table: `clicks.tsv`
(50 rows; sources are run bindings in `futon3c/data/wm-click-run-bindings/`,
run records in `futon2/data/wm-runs/`, and operator notes under
`~/notes/wm-next/criteria-completeness/`). Compiled 2026-10-30 by claude-11.

## Where the clicks went

| Route | Clicks | Outcome |
|---|---|---|
| Ordinary selection | 11 | 2 useful work; 8 stopped at a defect later repaired (5 via an injury, 3 by direct fix); 1 guardrail refusal (fc2e27e3) |
| Self-heal (repair coordinator) | 35 | 8 discharged an injury (with the medic clicks below); 4 authored and enacted a patch then retried; 6 retries stopped at a further defect; 5 held; 16 refused before any repair was attempted |
| Medic | 4 | discharges of outside-repaired injuries (counted with the 8 above) |

So 39 of 50 clicks were spent on the machine repairing itself. This is the
imbalance Joe flagged on 2026-10-29 ("if we spend all our time and energy on
repair and never do useful work…"). The rule adopted then: one self-heal
attempt; if it fails, fix the upstream cause directly and log the self-heal
failure as a separate (immune-loop) defect, run in parallel with ordinary
clicks.

## Useful work delivered

- **907fac83**, M-reachable-from-boot: want `h38267af9ec50` reached
  (futon3c `48b920a4`, issue #65 closed).
- **3589464e**, M-peradam-grounding: 2 of 5 selected wants reached
  (`:O-peradam-role` record; credit-assignment rule), futon7 `8b46822b`.
- Partial: b4e9ea0e's author commit futon3c `b7ca069d5` landed, but that run
  ended `build-failed` at the reviewer falsifier.

The campaign's success criterion ("useful task execution and successor
consumption without corrective intervention") is **met twice, not yet as a
streak**: neither useful click was followed by an uncorrected successor.

## Defects exposed and what was done

40 distinct defects, each tied to the click that exposed it (`clicks.tsv`).
18 were on the ordinary path, concentrated in cascade resume (answer-hole id
shapes, hot-load failure, resume protocol, author answer/refusal validity,
reviewer falsifier without selected-want identity, T3/T5 tripwires, selector
choosing a parked mission, readiness prompt over provider limit, invalid
reading → abstention). 22 were in self-heal/medic itself (acceptance tests
green-before-repair or uncompilable, acceptance runs escaping store isolation,
ledger/transaction divergence, coordinator transition refusals, medic authority
rejecting a valid ledger, launcher ownership mismatch so requirements were
never checked on self-heal clicks).

Machine changes made in response (futon2 unless noted; all reviewed):
out-of-band fix route with Lean model (`307144249`, futon3c `c139b401e`);
acceptance isolation (`ad632e153`); medic routes for open-injury discharge,
cause reattribution and ordinary-author-refusal misclassification
(`8b8344609`, `ae623faf0`, `09660685`); scoped acceptance (`b94329b50`);
answer delivery and author delivery gate (`b9f9cbc7d`, `198c07458`);
selector exclusion of unknown-term members (`4690a7e3d`); occurrence order
(`41173944d`); file-delivered and identity-checked readings (`28ca3794e`,
`034e90684`). Injuries discharged in the campaign: 9c622038, ad038c48,
2aef7e0c, c5606345, e43a348c, 1e3f6a62, a46779bc, f7e97cc6 — each with the
true fixing commit on record. e43a348c's upstream cause remains open
(deficit E4-E43-CAUSE-SCOPE).

Last change of the campaign (2026-10-30): reading-family selection now takes
β from the Lean R20 spec — prior `descend hesp-level q` from the carried
valence, posterior by `iteratePrecision` (16 fixed iterations, SPM `Ni`) —
replacing the closed Oct-17 β campaign gate (`6a36b6006`, `41350ce41`, merge
`5e8d9e2ed`). The outer Makeup pin was migrated without resetting learned
state (`e39a41110`, merge `b7a7c95f7`; live state migrated, backup `outer-state.edn.pre-r20-240ae86b`, `read-state` durable with learned fields equal). The held click fc2e27e3 then ran from its held phase to
close: β prior = posterior = 1.25 (one policy), identity change recorded.

## Last click so far (fc2e27e3) and the open question it leaves

fc2e27e3 (M-typed-memories) reached author and review and closed
`:guardrail-refusal` / `:reviewed-claim-refused` (ledger entry 128). The author
answered in 27 s by claiming an existing artifact: futon3c `7aac5ca07`, an
inbox-zero commit adding `E-argument-game-search.md`, unrelated to the
mission. The reviewer rejected the claim. The guardrail worked; the author's
behaviour is the defect.

## Pre-registration: click 51 (the last credit), amended 2026-10-30

Changes made before click 51, all reviewed, none spending a click:
lever 0 (existing-artifact claims must cite the selected target; futon2
`582451e3b`, Lean `d7e21d0ee`); Q6 refusal reuses click-local reselection
(`f9d28dc19`, Lean `b5c7da28f4`); outer learning consumes completed runs
exactly once (`93da58236`). Before that last fix outer learning had never
taken a step (`:click-count 0`; pending frozen on `f74df9cc` since 2026-10-29,
deficit OUTER-LEARNING-SKIPPED-RUNS).

A no-charge preview on the live code predicts:

- **Outer learning**: consumes f74df9cc once as `notYetEvaluated` (C decays
  only, observed gain `:several`); click-count 0 → 1; consumption receipt
  written; pending replaced by click 51's selection.
- **Selection**: outer selection picks M-typed-memories; Q6
  `:repeat-choice-after-refusal` (digest `c1801780…`, unchanged since click 50)
  empties its one-policy family; click-local reselection excludes it for this
  click and selects **M-peradam-grounding**.
- **Reading**: its retained reading is stale, so the click refreshes the
  reading before decision.
- **β**: R20 `:r20-beta-from-valence`, 16 iterations.
- **Success** = a want of M-peradam-grounding reached with a new commit by the
  author in this run, accepted by the reviewer (3 of its 5 wants were open
  after click 3589464e).
- **If the author claims an existing artifact**: lever 0 refuses it before
  review unless the commit cites M-peradam-grounding.
- **Any other stop**: one self-heal attempt at most; on failure, fix upstream
  directly and log the self-heal failure for the immune loop.
- After click 51 the budget is exhausted; a further campaign needs Joe's grant.
