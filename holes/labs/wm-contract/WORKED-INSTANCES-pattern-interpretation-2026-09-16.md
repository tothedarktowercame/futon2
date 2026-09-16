# Worked instances for PROPOSAL-pattern-interpretation (D2–D7)

claude-7, 2026-09-16. Two real cascades interpreted under claude-4's proposal
(`p4ng/wm-walkthroughs/build-loop/closure/PROPOSAL-pattern-interpretation.md`),
tokens f⁺ (established) and f⁻ (observed false), unknown = neither token.
These are illustrations for review, not admitted interpretation records.

## 1. Buffer cleaner (futon3/library/buffer-cleaner, five patterns)

Real state: `buffer-cleaner-adapter/runs/state-packet.json` (134 buffers: 60
temp, 48 other, 13 invoke, 10 http, 1 dired, 1 file, 1 stream; 123 modified, 4
visible, 7 with a process, 1 active agent). Receipts: conservative 0 kills /
134 remain, aggressive 3 kills / 131 remain; both `:execute
:refused-until-gated`, `:revisit-truth :unavailable`, threshold 16.

Cycle-grain facts: packet-observed, exemptions-recorded, staleness-classified,
kill-receipts-emitted, gate-exists (false: `classify.clj:105`
`:execute-not-gated`), kills-executed, settled-report-emitted, clean-enough
(false: 131/134 ≥ 16), revisit-truth-measured (false: "unavailable" is an
observation of absence). q0: all f⁻.

| pattern | guard | effect |
|---|---|---|
| observe-categories | ¬packet-observed | packet-observed |
| exempt-the-in-use | packet-observed ∧ ¬exemptions-recorded | exemptions-recorded |
| classify-staleness | exemptions-recorded ∧ ¬staleness-classified | staleness-classified |
| receipt-then-gate (proposal) | staleness-classified ∧ ¬kill-receipts-emitted | kill-receipts-emitted |
| receipt-then-gate (act) | kill-receipts-emitted ∧ gate-exists ∧ ¬kills-executed | kills-executed |
| yield-settled | kill-receipts-emitted ∧ ¬settled-report-emitted | settled-report-emitted |

Run: observe → exempt → classify → receipt → (act blocked: gate-exists⁻) →
yield → stop. clean-enough never established. Reproduces the recorded run
("no threshold-eligible wiring"); strong-Kleene enactment gives the same order.

Findings:
- F1. An unknown in a ¬ guard stalls the cascade: declaring exemptions-recorded
  `:unknown` stops everything at step 2. Admission rule needed: per-cycle
  progress facts are false at cycle start by construction (observed), unknown
  is for genuinely unobserved facts.
- F2. Real uncertainty (was a killed buffer needed later? priors 0.05–0.40)
  belongs in θ and C, not in an unknown guard fact.
- F3. The want (clean-enough) is a world outcome no pattern's effect produces;
  D7's coverage over produced f⁺ is 0 on every path. Needs a θ-weighted outcome
  effect on the act, or preference scored on observation.
- F4. receipt-then-gate's THEN holds two acts; one guard/effect per pattern
  forces a split whose link to the source pattern the record must carry.
- F5. exempt-the-in-use is per buffer (123 modified); per-buffer grounding is
  ~670 facts / 1,340 tokens and needs quantifiers the guard language lacks.
  Per-item invariants stay in adapters and gates; the cascade universe has a
  declared grain.

## 2. Inbox zero (futon3/library/inbox-zero, from futon0/README-inbox-zero.md)

Real state: futon-sync-check-clean run 2026-09-16 21:01 (journal), repo p4ng:
clause 1 nine tracked generated files 40.7 h; clause 3 291 unpushed, oldest
151 h; upstream configured, not behind; last commit within a minute.

Per-repo facts (16): remote-fetched ⁺; stale-dirt-present ⁺; dirt-classified
⁻; dirt-untracked-generated ⁻ (tracked, `git ls-files`); dirt-tracked-generated
⁺; dirt-real-work ⁻; unpushed-stale ⁺ (151 > 24 h); unpushed-outlier ⁺ (291 >
declared threshold 10, futon3 inbox-zero-lib `promote_push.clj:15`);
sensitivity-screened **unknown** (no screen record found);
turn-end-hook-covers-authors ⁻ (hook is agent-chat only per README; p4ng's
authors are Claude Code / Zai sessions); in-flight-by-other-seat ⁺;
escalated-to-seat ⁻ (no record found); change-routed-to-actor ⁻
(`inbox-zero-delta.bb` logs and notify-sends only); pushed ⁻; noise-ignored ⁻;
work-promoted ⁻.

| pattern | guard | effect | at q0 |
|---|---|---|---|
| measure-against-the-remote | ¬remote-fetched | remote-fetched | blocked (achieved) |
| classify-the-dirt | remote-fetched ∧ stale-dirt-present ∧ ¬dirt-classified | dirt-classified | fires |
| push-the-declared | unpushed-stale ∧ ¬unpushed-outlier ∧ sensitivity-screened ∧ ¬pushed | pushed | blocked |
| escalate-by-who-can-act | unpushed-outlier ∧ ¬escalated-to-seat | escalated-to-seat | fires |
| ignore-by-kind | dirt-classified ∧ dirt-untracked-generated ∧ ¬noise-ignored | noise-ignored | blocked |
| promote-at-turn-end | dirt-classified ∧ dirt-real-work ∧ turn-end-hook-covers-authors ∧ ¬in-flight-by-other-seat ∧ ¬work-promoted | work-promoted | blocked |
| gate-fails-loudly | stale-dirt-present ∧ ¬change-routed-to-actor | change-routed-to-actor | fires |

Model (patterns as written): classify, escalate, route; then stops (push
needs the seat; tracked generated dirt has no acting pattern). Enactment
today: nothing but a log line. The difference names three implementation
defects:
- D-a. Escalation is triggered only from turn-end promotion, so the enacted
  guard carries turn-end-hook-covers-authors; for p4ng it never fires.
- D-b. No pattern acts on tracked generated output (now
  inbox-zero/generated-by-role: intermediate / rebuildable view /
  deliverable / evidence, declared once per kind).
- D-c. The in-flight exemption never lifts on a continuously active repo, so
  exempt dirt ages past 24 h unreported.
And one model-consistent unknown: futon3 (9 unpushed, under threshold, 32 h)
has no screen record, so sensitivity-screened is unknown and push cannot
fire — matching the gate, where futon3's commits are still unpushed.

Findings for the proposal:
- F6. Threshold-derived facts are admissible only with the declared threshold
  and its source; without it they are unknown and block both branches.
- F7. Interpreting a pattern and comparing its guard to the implementation's
  actual trigger is diagnostic: prediction ≠ enactment names the defect.
- F8. How a fact is named (change-reported vs change-routed-to-actor) decides
  whether the model sees a gap; fact meanings need review at admission.
- F3 recurs: the want repo-clean is an outcome no pattern produces.

## 3. Test registry (futon3/library/test-registry, a feature not yet in the loop)

Patterns: futon3 `dc53a0a`. Registry CLI exists (futon3c `test_registry.clj`,
one round trip 2026-09-14, no caller since). Real handoff: WM-03 P10 part 2,
futon2 `cb5f8b1288`, reviewed by claude-4 with "86 tests, 9011 assertions"
rerun across three namespaces. Grain: one (handoff, namespace) pair.

Facts per namespace n (real value; counterfactual with the feature wired):
author-ran-tests (unknown: prose only / ⁺); warrant-registered (⁻ / ⁺);
handoff-carries-warrant (⁻ / ⁺); tests-changed (from git: cascade_model_manifest_test
⁺ 34→35 deftests, receipt_construction_test ⁺ modified, shadow_cascade_g_test
⁻ unchanged); lane-mandatory (unknown: no lane declared / ⁻ routine);
warrant-checked, spot-checked, full-rerun-done, adequacy-recorded,
saving-metered (all ⁻ at q0). warrant-valid is an observation made by the
check (P5), not a produced token: add-only effects (D5) cannot produce
"refused", so the check's outcome enters by observation.

| pattern | guard | effect |
|---|---|---|
| register-the-run | author-ran-tests ∧ ¬warrant-registered | warrant-registered |
| warrant-rides-the-handoff | warrant-registered ∧ ¬handoff-carries-warrant | handoff-carries-warrant |
| bind-warrant-to-the-diff | handoff-carries-warrant ∧ ¬warrant-checked | warrant-checked (validity observed) |
| spot-check-at-the-declared-rate | warrant-valid ∧ ¬tests-changed ∧ ¬lane-mandatory ∧ ¬spot-checked | spot-checked |
| rerun-when-the-warrant-fails | (¬warrant-registered ∨ (warrant-checked ∧ ¬warrant-valid) ∨ tests-changed ∨ lane-mandatory) ∧ ¬full-rerun-done | full-rerun-done |
| judge-adequacy | (warrant-valid ∨ full-rerun-done) ∧ ¬adequacy-recorded | adequacy-recorded |
| meter-the-saving | (spot-checked ∨ full-rerun-done) ∧ ¬saving-metered | saving-metered |

rerun-when-the-warrant-fails expands (D4) to four siblings, one per disjunct.

Today's q0: register blocked (author-ran-tests unknown), handoff blocked;
sibling 1 (¬warrant-registered) fires in all three namespaces → full rerun ×3 →
adequacy → meter. Reproduces what happened (three full reruns), except
saving-metered, which the real review did not record.

Counterfactual q0 (warranted handoff, routine lane declared):
cascade_model_manifest_test and receipt_construction_test → sibling 3
(tests-changed) → full rerun; shadow_cascade_g_test → check → spot-check (one
test instead of three). On this real delivery the feature saves 2 of 64
deftests.

Findings:
- F9. The saving on a first review of a changed delivery is small, because
  deliveries change tests and the first-run lane applies. The larger waste in
  the 2026-09-16 record is repeated verification of unchanged content by
  successive agents (owner, reviewer, contract emitter each rebuilding or
  rerunning the same base: e.g. CascadeTransition/ExactBeliefTrajectory/
  MachineContracts rebuilt by claude-4 and again by claude-7). The registry's
  first target should be that case.
- F10. An undeclared lane is unknown and blocks the spot-check sibling, which
  is the safe direction; admission requires the handoff to declare the lane.
- F11. A check's pass/fail is an observation (P5), not an effect; add-only
  effects cannot express refusal. Consistent with claude-4's F3 answer.
