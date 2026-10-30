# Proposal: efficiency levers that spend no click (2026-10-30)

**Lever 1 below changes what click 51 would select, so it bears on the
pre-registration in `runs/campaign-50-2026-10-27/REPORT.md`; it goes to
Joe and the campaign's Codex owner before it is built or activated.**
From a Claude session (cloud, session `01DcURqCM4rXHzJMaHCQ8AhG`) at Joe's
question "is that the only a priori improvement you can find?", after the
one change Joe assigned (futon2 branch `claude/claim-cites-selected-target`,
mathlib4 branch of the same name: an existing-artifact claim must cite the
selected target before review). Under the 2026-10-18 ground rules this note
proposes; Codex decides whether anything here gets built. Evidence is the
campaign's `clicks.tsv` and the code as of futon2 `cffe497d`.

The audit this follows is futon3c
`holes/technotes/TN-wm-next-50-clicks-audit-2026-10-30.md`. Its baseline for
the next 50 clicks: about 15 credits to medic and self-heal, about 7 useful
clicks, about 3 clicks lost to an author claiming an unrelated artifact, and
14 to 20 new ordinary-path defects. The levers are ranked by credits saved
per unit of change.

## Lever 1: do not re-pick the task whose run just refused, on unchanged input

**What happens today.** `run-ordinary-opportunity!`
(`src/futon2/aif/full_loop_runtime.clj`, the missing-task branch) builds the
Giraffe observation, resolves pending learning, and calls
`outer-select/select` on the whole admitted field. The only click-local
exclusion is `reading-reselection-port`, which removes a target after its
*reading* is refused. Nothing consults the pending selection's terminal
outcome. `outer_select.clj` has no refusal history (confirmed by grep; the
E3-OUTER-REPEAT discovery of 2026-10-29 found the same). The deficits row
E3-OUTER-REPEAT reads "IMPLEMENTED; LIVE CHANGED-INPUT CONTINUATION
OBSERVED": the observed continuation is the reading reselection at click
49, not a refusal re-pick prevention. GRID v2 E3 names the re-pick after a
refusal on unchanged input as a violation.

**Why it costs a click.** After click 50 (fc2e27e3, M-typed-memories,
`:guardrail-refusal`), the Giraffe run and the makeup are unchanged, so
click 51's outer selection re-picks M-typed-memories. Downstream the inner
decision's Q6 (`previous_run.clj`, `:repeat-choice-after-refusal`) maps
`:guardrail-refusal` to `:refused` and removes the previous policy key when
the selection-input digest is unchanged; with one policy per reading family
the decision then abstains. Either outcome spends the credit: a typed
abstention, or (if the digest moved with the R20 β migration) a repeat of
click 50 with the same author incentive. The R20 β change itself is inert
on one-policy families (`iteratePrecision_constant_F`), so it does not
change this.

**The change.** Before `outer-select/select`, when `(:pending state)` names
a selection whose `:giraffe/run-id` equals the current observation's and
whose `:makeup/sha256` equals the makeup's, read that pending run's record
(`tick-run-record-<run-id>.edn`, `[:terminal-receipt :outcome]`); if it is
`:guardrail-refusal`, remove that target from `:field` for this click and
append `{:target .. :reason :repeat-choice-after-refusal :scope :this-click
:predecessor-run-id ..}` to `:outer-field/excluded`. An absent or unreadable
record is typed and excludes nothing. If the field empties, the click is the
existing typed `{:absent :no-task-selected ...}` with the exclusion retained.
About 40 lines in `full_loop_runtime.clj` beside `pending-run-ending`, plus
a wiring test in the shape of `outer_selection_wiring_test.clj` with a
pending selection and a refused run record. Lean: this is a
`SelectionStep.gate` in `R20Step` applied to the field before the cascade
(`applySelectionCascade_mem` already gives that the pick is from the
reduced field); a small standalone module can state the predicate and the
click-50/51 witness pair, as `AlreadySatisfiedReview.lean` revision 2 does
for the claim check.

**Effect.** Each refusal stops costing a second click. On the audit's
baseline (about 3 refusals in 50) that is about 3 credits, and it removes
the "same task, same author, same incentive" repeat that the click-51
pre-registration itself anticipates.

**Scope note.** The pre-registration predicts ordinary selection on the
unchanged field and plans for the author "again" claiming an unrelated
artifact. Activating this lever before click 51 changes that prediction, so
it needs an amendment to the pre-registration first, or activation only
for the next grant.

## Lever 2: an already-satisfied claim must show executed inspection

**What happens today.** Rule 7 of the author prompt says "if inspection
shows" the construction is already satisfied. The classifier
(`author-refusal-classification`) reads the card and the commit's
existence; nothing reads whether the author ran anything. Reviewers are
already held to this: `review-execution-gate`
(`task_execution_evidence.clj`) requires `:executed` and `:tool-events > 0`
from the job's `:execution`, which Agency populates for every job
(`futon3c/src/futon3c/transport/http.clj`, `invoke-execution-evidence`).
Click 50's author answered in 27 s.

**The change.** In `review-already-satisfied-claim!`, after the citation
check, read the author job's execution evidence with the same function the
reviewer gate uses. A claim with executed inspection proceeds; a claim
whose evidence affirmatively says no tool ran is refused before dispatch
as its own typed kind, non-injury like the citation refusal.

**Caution that makes this record-only first.** Agency's comment says the
claude lane does not self-report execution telemetry and the fallback
counts tool_use events from an in-memory ledger that is lost on restart. A
restart between the author's answer and the poll would make an honest
inspected claim look uninspected. Which lane the Zai seats report through
is not established in the public record. So: land it as a retained
`:claim-inspection` field on the result first; promote it to a refusal once
one live Zai author job shows positive `:tool-events` for an inspected
answer.

**Effect.** Small on its own now that lever 0 (the citation check) exists;
it catches the uninspected claim that happens to cite the target.

## Lever 3: decide self-heal admission before the ration is spent

**What happens today.** 16 of the campaign's 35 self-heal clicks (clicks 4,
5, 9, 10, 11, 15, 16, 22, 24, 25, 26, 27, 28, 30, 33, 44) were refused
before any repair was attempted, each after its credit was spent.
`meta-self-heal-preflight!` (`futon3c/src/futon3c/wm/runner_service.clj`)
already runs `production-readiness` before the ration and refuses with
`:before-ration-spend? true`, but readiness emits only port terms;
`route!` (`meta_self_heal_production.clj`) is what refuses with
`[:acceptance-test]`, `acceptance-test-passes-before-repair`,
`resume-identity-refused`, `*-transaction-mismatch` and
`ledger-transition-refused` (SelfHealRoute.lean, revision 2 note), and it
runs after the spend.

Of the 16: eight were decidable from durable state alone (4, 5, 9, 11, 15,
27, 28, 30: absent ports or acceptance-test, transaction and ledger
identity mismatches); six needed an acceptance run (10, 16, 22, 24, 25,
26: green-before-repair, uncompilable, unsatisfiable, unreadable), which
the authority already classes as "external diagnostic/test work", not an
ordinary click; two (33, 44) were isolation and scoping defects since fixed.

**The change.** A read-only admission preview: `route!`'s admission
predicates evaluated against the current ledger, transaction and
acceptance test, with no enactment, no ration callback and no click,
surfaced beside `GET /api/alpha/wm/click` or as a script in
`scripts/wm_click_*.py`. The operator issues a self-heal click only when
the preview admits. This is SelfHealRoute.lean's open hole ("should a
bounded run whose test does not compile refuse at admission, before a
reviewer invoke?") and therefore a specification question for Joe before
it is built.

**Effect.** The largest of the three. Under the one-attempt rule the next
50 still pay one self-heal attempt per new injury; if the refusal share
holds (16 of 35), that is roughly 5 to 7 credits saved, and the operator's
time with them.

## Lever 4 (minor): refuse a duplicate issuance

Clicks 4 and 5 were issued 14 s apart and refused identically. An
issue-time check in `futon3c.wm.ordinary-click-budget` that refuses a
self-heal click whose predecessor's refusal terms are unchanged would have
saved one credit. Mostly operator discipline; recorded for completeness.

## Not levers

- **T12** is now `:enabled? true` (`tripwire.clj:106`); its live firing is
  the pending observation, not a change.
- **R20 β.** Provably inert on one-policy families; the lever would be
  multi-policy reading families, which is a modelling task, not an
  efficiency fix.
- **Lever 0, done at Joe's instruction:** the citation check on
  existing-artifact claims, branch `claude/claim-cites-selected-target` in
  futon2 and mathlib4, Lean revision 2 of `AlreadySatisfiedReview.lean`.

## Order

Lever 3 first if Joe rules on the hole, since it saves the most; lever 1
for the next grant with a pre-registration amendment; lever 2 record-only
now, refusing later. None of these spends a click to build or to test:
lever 1 and lever 2 have hermetic tests in the existing shapes, lever 3 is
exercised against the retained ledger and transaction files.
