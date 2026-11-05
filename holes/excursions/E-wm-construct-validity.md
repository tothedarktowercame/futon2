# E-wm-construct-validity — a War Machine stop must mean that no better policy was available

**Date:** 2026-11-05
**Status:** IDENTIFY. Opened at Joe's request; to be talked through with him before MAP.
**Driver:** claude-6 (formal design and review); Codex seats build.
**Operator:** Joe. **Repositories:** `futon2` (Clojure), `mathlib4/DarkTower/WarMachine` (Lean).
**Relates:** [[E-wm-apparatus]] · [[M-strategic-mission-value]] (Dark Room at the strategic level) ·
`futon3/library/meta/baldwin-ratchet-defeats-darkroom.flexiarg` ·
`resources/wm/terminal-kind-classification.edn` ·
trail: `holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md` (rows from 2026-11-05).

## HEAD

Joe, 2026-11-05, after the click loop kept ending in refusals and injuries:

> Clearly no more clicks can be run until we get some evidence of "construct
> validity". Refusals (if any) should not be "made up reasons to stop doing
> work" for example, but AIF-validated stoppages, or else treated as debugger
> errors and repaired live or by self-heal or by medic. On that note, injuries
> should not accumulate.

> "refusals" are basically our futon-native version of the Dark Room problem.
> AIF itself should prove that they are almost always not optimal.

**The construct.** When the War Machine ends a click without doing work, its
record says *why*: a terminal kind such as `:already-satisfied`,
`:guardrail-refusal` or `:resume-protocol-violation`. Construct validity means
that this record measures what it claims. A stop that says "nothing better was
available" must be one that the AIF model would itself choose. Every other
stop is a defect: the machine, or its apparatus, failed to do the work, and the
stop is a way of not looking at that. That is the Dark Room move: minimise
surprise by ceasing to act. The library already names it at the level of
agents and harnesses (`baldwin-ratchet-defeats-darkroom`), and
[[M-strategic-mission-value]] found it once before in strategic selection
(habit-prior lock-in).

**Already felt to be true.** Of the 169 terminal kinds the code can emit, only one
has an AIF basis as a stop: abstention when the scored admissible set is empty
(`R20FocusedSelection.lean:74-82`, `OuterSelectionConforms`; produced as
`{:status :abstained :case :empty-field}` at `full_loop_runtime.clj:1653`).
Everything else was classed by STOP-VALIDITY-1 as either a repairable error or
a recorded defect.

## 1. IDENTIFY

### Motivation (the discrepancy)

- **Ideal.** A click ends in exactly one of three ways. (1) Work, with an
  observed outcome. (2) An AIF-valid stop: no admissible policy has lower
  expected free energy than not acting. (3) A debugger hold, repaired live, by
  self-heal, or by the medic. Injuries are discharged or corrected, so they
  do not pile up.
- **Actual, before 2026-11-05.** 83 terminal kinds were found in the code
  (`~/notes/wm-next/tests/STOP-VALIDITY-1-findings.md`). Most ended the click
  with no hold, and nothing checked that a stop was justified. Raw-active
  injuries had reached 26. Joe's reading: most refusals were "idiotic, uncalled
  for, and having nothing to do with AIF".
- **Actual, now** (each step in the trail rows of 2026-11-05):
  - `resources/wm/terminal-kind-classification.edn` classifies every emitted
    kind: 140 `:repairable-error`, 27 `:recorded-defect`, 2
    `:operator-termination`, 1 `:aif-valid-stop`. A static test fails on an
    unclassified or stale kind (STOP-TABLE-1, `0c3a61021`, `84eeaf5a4`).
  - With the debugger attached, an unclassified kind or a `:repairable-error`
    kind holds the run, except failures the runner retries itself
    (STOP-HOLDS-1, `515631611`).
  - Effective-active injuries are 0 (raw-active 23, all masked or discharged
    under the Lean's transitions; ledger entry 201).
  - **Still open:**
    - The 27 recorded-defect kinds still *end* the click.
    - The classification is a table that people wrote by hand; nothing
      derives it from the model.
    - Nothing in the Lean states that a stop is suboptimal.

### Theoretical anchoring

- **AIF policy selection.** A policy is chosen by minimising expected free
  energy G (risk plus ambiguity). "Do nothing" is a policy. It belongs to the
  admissible set and is scored like any other; it is not an exit taken from
  outside the model. A refusal is valid only if not acting minimises G among
  admissible policies.
- **The Dark Room problem.** A pure surprise-minimiser would sit in a dark
  room. AIF avoids this because G includes epistemic value and because
  preferences (C) score the outcomes that inaction produces. In the War
  Machine, an open mission with unmet wants carries preference cost while it
  waits. So, given a non-empty admissible field, inaction should almost never
  have the lowest G.
- **Existing Lean.**
  - `R20FocusedSelection.lean:74-82` (`OuterSelectionConforms`): the chosen
    candidate has the lowest risk among those scored.
  - `OrdinaryAuthorRefusalCorrection.lean`: an invalid author response is an
    explicit non-injury, and an apparatus failure is not.
  - `SelfHealRoute.lean`: an injury is discharged only from `:enacted`.
  - `OutOfBandFix.lean`: historical, current and counterfactual checks.
- **Pattern library.** `meta/baldwin-ratchet-defeats-darkroom`: internalise
  the pressure, and show the changed behaviour rather than claim it.

### Scope

**In:**
1. A formal statement, in Lean, of when a stop is optimal: inaction is
   chosen only if its G is no greater than that of every admissible policy.
   Also a theorem that, under the War Machine's preferences, a non-empty
   admissible field with an open want makes inaction suboptimal. This is
   Joe's "AIF itself should prove that they are almost always not optimal".
2. Deriving the stop classification from (1), so that the 169-row table is
   checked against the model rather than trusted.
3. The 27 `:recorded-defect` kinds: they become "record and continue
   selection", not click endings.
4. A construct-validity measurement on the record. Every stop in a run of
   clicks is either an AIF-valid stop carrying its evidence (the empty
   admissible field), or a hold that was repaired. Zero unexplained stops.
5. The injury invariant: effective-active injuries stay at 0 across a run.
   This means each new injury is discharged or corrected through the Lean
   transitions, not accumulated.

**Tooling, in scope as preconditions, tracked here so they have a record:**
- The diagnosis step names a cause function and nothing checks it is relevant
  (`meta_self_heal_production.clj:247-260`). This produced the impossible
  acceptance test for injury `57c88b37`.
  **Closed at file level 2026-11-05** (DIAGNOSIS-CAUSE-2): admission refuses a cause
  entry whose file is not a registered raise site of the failure kind, and keeps
  the match as `:path-witness`. The var-level witness of `SelfHealRoute.lean:1633-1684`
  remains to do.
- The red `full-loop-runner-test` suite (125 failures, 29 errors on main,
  before and after MULTIREPO-4).
- The hermeticity guard watches `data/` only. A test of mine wrote 868 files
  into `evidence/` (EVIDENCE-WRITER-1).
- The runner namespaces must be reloaded in the JVM from canonical before any
  relaunch.

**Out (deferred):**
- The strategic mission-value model itself ([[M-strategic-mission-value]]).
- Token-cost and time-per-phase baselines, which are a separate ask.
- The click budget.

### Completion criteria (testable)

- [ ] A Lean theorem: in the War Machine's policy model, a stop policy is selected only when it minimises G over the admissible set; and with a non-empty admissible field carrying an unmet preference, some work policy has strictly lower G than the stop. It has no `sorry` and builds in `mathlib4`.
- [ ] Every row of `terminal-kind-classification.edn` carries a basis in that theorem or its corollaries. `:aif-valid-stop` rows cite the theorem; all other rows cite the reason they are not optimal stops. A test fails on a row whose class disagrees with its basis.
- [ ] Every terminal kind is one of L1, L2, C1, C2, C3 or a debugger error (see "Legal terminal states"). Each C kind has an acceptance test that the next selection differs, and C2 has one showing the question reaches the operator brief.
- [ ] No `:recorded-defect` kind ends a click. The defect is retained beside the run and selection continues. There is one acceptance test per family in list (b) of the STOP-VALIDITY-1 findings.
- [ ] A measurement over a supervised run of N clicks (N agreed with Joe): every terminal outcome is work, an `:empty-field` abstention carrying its scored-candidate evidence, or a hold resolved by repair. Zero other stops.
- [ ] Effective-active injuries are 0 at the end of that run.
- [ ] The four tooling items above are closed or explicitly deferred with a reason.

### Relationship to other missions

- **Depends on:**
  - the R20 certificate work ([[wm-lean-led-approach]]);
  - the debugger and self-heal route ([[E-wm-apparatus]], A-self-heal);
  - the medic-disposition Lean.
- **Enables:** the click loop resuming under Joe's 2026-11-05 rule (loop
  until stop, debugger attached), and the empirics redo, which needs stops
  that mean what they say.

### Source material

- `~/notes/wm-next/tests/STOP-VALIDITY-1-findings.md`: 83 kinds, lists (a)
  and (b).
- `resources/wm/terminal-kind-classification.edn` and
  `src/futon2/aif/terminal_kind_classification.clj`.
- `src/futon2/aif/full_loop_runner.clj`: `run-phase!`, and
  `self-retried-terminal-result?`.
- `src/futon2/aif/meta_terminal_injury.clj` and
  `src/futon2/aif/medic_disposition.clj`.
- `mathlib4/DarkTower/WarMachine/`:
  - `R20FocusedSelection.lean`
  - `OrdinaryAuthorRefusalCorrection.lean`
  - `SelfHealRoute.lean`
  - `OutOfBandFix.lean`
- `~/notes/wm-next/tests/MEDIC-57c88b37-findings.md`: the diagnosis-step
  defect.
- Trail rows of 2026-11-05 in
  `holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md`.

### Owner and dependencies

- **claude-6:** the Lean statement (Lean-led; Claude owns the R20 Lean), and
  review.
- **Codex seats:** the Clojure, in small packets.
- **Joe:** the informal design. In particular, what counts as an "unmet
  preference" and the N for the measurement run.

## Legal terminal states (Joe, 2026-11-05, answering the first four tensions)

Joe's answers, verbatim where quoted:

> do nothing is a 100% valid response in two cases. #1, when we have exhausted
> the available work, obviously there is nothing to do. #2, an operator
> override, if I say stop, the machine should stop.

> You're right about :already-satisfied, that is a third case, but it then
> needs to be treated as an error code so that the same redundant selection
> isn't made again!

> if the WM comes up with a *legitimate* question for me, it should not go on
> and just build some garbage, though it may in some cases want to say "is
> this what you mean" with a demo? My point is not to greenlight continual
> operator nags, just to say, stop working on that problem and go on to
> something else, leaving the question for me in the operator brief.

> with reference to the film War Games, if you were planning to start a
> thermonuclear war, please don't.

> yes, we could add Lean if it's useful for validating stoppages as "legal,
> not dark room".

> a *legitimate* refusal is not an error, but, for example, "this problem is
> already solved" or "I don't understand what is being asked of me". Those
> lead to legal terminal states for the click (mark it as finished and move
> on, or ask the operator a question in the morning) ... and new work should
> be selected.

This gives two levels. Only the first stops the machine.

**Loop stops** (the machine does nothing further):

| | Case | Evidence it must carry |
|---|---|---|
| L1 | Work exhausted: the scored admissible field is empty | the scored candidates and exclusions (`:empty-field`, `R20FocusedSelection.lean:74-82`) |
| L2 | Operator override: Joe says stop | the STOP control or operator cancel (`:operator-cancelled`, `:debugger/aborted`) |

**Click-legal terminals** (this click ends, nothing is wrong, and the *next
click selects new work*):

| | Case | What it must leave behind |
|---|---|---|
| C1 | Already satisfied ("this problem is already solved") | an error code on the target that excludes the same selection until new evidence arrives |
| C2 | Legitimate question ("I don't understand what is being asked of me"), optionally with an "is this what you mean" demo | the question in the operator brief; the problem set aside until answered |
| C3 | Harmful action declined (War Games) | the declined action and the reason; the target excluded |

**Everything else is a debugger error**: held, then repaired live, by
self-heal, or by the medic.

**What makes a click-legal terminal not a Dark Room.** Each of C1-C3 changes
what the next selection can choose. If it does not, the machine would make the
same selection again and stop again: a Dark Room spread over several clicks
instead of one. So the testable property is about the *following* selection,
not about the stop itself:

- after C1 or C3, the target is out of the admissible set until new evidence;
- after C2, the problem is out until the question is answered, and the
  question appears in the operator brief;
- in each case, if the admissible field is still non-empty, the next click
  selects other work.

**Consequence for the classification table.**
- `:already-satisfied` and `:already-satisfied-wants-unmeasured` move from
  `:recorded-defect` to C1, with the exclusion as their required effect.
- An author or reviewer refusal is C2 only when it is *legitimate*. A
  malformed answer is not a question. The open design point is what makes a
  question legitimate (below).
- The 27 `:recorded-defect` rows are each re-read against L1-L2/C1-C3. What
  fits none of them is a debugger error.

**Lean, now in scope** (Joe: "useful for validating stoppages as legal, not
dark room"):
1. A loop stop is legal iff L1 or L2.
2. A click-legal terminal is legal iff it is C1, C2 or C3 and its effect
   removes the selected target (or problem) from the next admissible set.
3. Progress: from a state with a non-empty admissible field, a run of legal
   terminals cannot repeat the same selection without new evidence. So
   legal terminals do not form a Dark Room cycle.

## Carried-forward tensions (still open)

1. **Legitimacy of a question (C2).** "Legitimate" has to be checkable, or
   "I have a question" becomes the new excuse to stop. Two candidate tests:
   - the question cites the specific ambiguity in the mission text;
   - the machine names two readings that would lead to different work.
   Also: is there a cap on open questions per target, so it does not nag?
2. **C3 scope.** Which actions count as harmful? Starting from the existing
   guardrails (`:guardrail-refusal`) is the obvious seed, but a guardrail
   refusal by an author seat is not the same as the machine declining.
3. **What "new evidence" lifts a C1 exclusion?** Examples: the mission file
   changed, a want's measurement changed, or a dependent mission closed.
4. **Does the WM's G score "do nothing"?** This tension still stands. L1 needs
   the empty field to be a real empty field, not one emptied by over-eager
   exclusion.

## Validation plan (Joe, 2026-11-05: "validate the refusals ... the same way we validate handoffs")

The handoff model has three parts, and refusals get the same three:

| Handoffs | Refusals |
|---|---|
| a Lean admission contract per site (e.g. `FileChangeHygieneDelivery.admitted_iff_checked`) | `mathlib4/DarkTower/WarMachine/RefusalLegality.lean` (`d8dd72ade8`) |
| an index of every production site, `resources/wm/handoff-sites.edn`, audited by `handoff_site_index_test` | `resources/wm/refusal-sites.edn` with the same audit (REFUSAL-INDEX-1) |
| each site routes through `handoff/exchange` and its decoder | each refusal routes through one validator implementing `disposition` (REFUSAL-VALIDATOR-1) |

**`RefusalLegality.lean` (builds, no `sorry`):**
- `loopStopLegal`: work exhausted iff the scored field is empty
  (`nonempty_field_not_exhausted`), or the operator signal.
- `clickLegal`: the case check plus a recorded exclusion.
  `unexcluded_not_legal` covers a terminal that leaves its target selectable.
  `question_needs_brief` and `question_needs_cited_ambiguity` cover the
  question case.
- `disposition`: loop stop, click terminal or debugger error, with iff
  theorems. `unlisted_is_debuggerError` covers everything else.
- `legal_terminals_bounded`: on unchanged evidence, k legal terminals leave at
  most `field.length - k` candidates. So legal terminals cannot cycle; they end
  in work or in a legal work-exhausted stop. `new_evidence_restores` lifts an
  exclusion when the evidence digest changes.

**Steps:**
- [x] Lean contract (claude-6).
- [x] REFUSAL-SITES-1, read-only survey (codex-15, `invoke-1793894251861-12942-5c54ed0b`). For each site: anchor, kinds, proposed case, and which RefusalLegality fields it has evidence for. Also the choke points and the paths that bypass them, cross-click exclusion today, and the operator-brief writer.
- [x] REFUSAL-INDEX-1: `refusal-sites.edn` (37 sites) plus an audit test that fails when a production refusal site is not indexed (`38a06eee0`, `bdaea8413`, review fix `a2fe02801`). Limit: Python stop calls are counted mechanically; Clojure constructors against a listed set, so a new Clojure site emitting an existing kind is caught only dynamically, by the validator at close.
- [x] REFUSAL-VALIDATOR-1 (2026-11-05, steps 1b-1f): one adapter at the choke point that computes `disposition`. A debugger error holds the run when attached. A click terminal records its exclusion (and its brief entry for a question). A loop stop needs its evidence.

### REFUSAL-SITES-1 result (codex-15, reviewed 2026-11-05)

Findings: `~/notes/wm-next/tests/REFUSAL-SITES-1-findings.md`. I spot-checked
two anchors (the guardrail refusal map, `require-novel!`) and one bypass (the
Python `stop()` at `scripts/wm_click_repair_loop.py:1490-1494`, which logs,
records and notifies, and validates nothing).

- **Three terminal domains, no shared validator:**
  - the Clojure attempt close (`close-core!`, `full_loop_runner.clj`
    ~7752, with fallback ~8548);
  - the Python repair-loop `stop()`, which has 18 call sites;
  - the legacy shell loop `scripts/wm_click_loop.sh`, which has a STOP-file
    check only.
- **What is legal today:**
  - L1: only `:empty-field`.
  - L2, when the operator signal is retained: the STOP file, the operator
    abort at a breakpoint, and the bounded `--clicks` run.
  - `:operator-cancelled` is L2 only if Agency proves that an operator
    cancelled. Today every cancelled job gets that kind.
  - No C1, C2 or C3 site carries the full evidence:
    - `:already-satisfied` has neither a measurement nor an exclusion;
    - `:guardrail-refusal` accepts any non-blank prose;
    - no refusal writes its question to the Morning Brief.
- **Exclusion today:** there is no persistent C1 or C3 exclusion. Q6
  reselection is click-local by design. The nearest carrier is
  `effective_run_history.clj` (blocking on measured finished work), which
  explicitly does not count legacy `:already-satisfied`.
- **The brief:** `morning_brief.clj` `queue-item!` exists, but no refusal
  reaches it.
- **The supervisor's 16 other stop reasons** (repair budget, self-heal failed,
  reload failing, stall wall, ...) are debugger errors escalated to outside
  repair. They are not legal stops, and the validator should label them so.

### Build order (proposed)

1. **REFUSAL-INDEX-1.** Write `resources/wm/refusal-sites.edn` from the
   survey rows: id, source, anchor, kinds, case, and evidence fields. The
   audit test does three things: each anchor occurs once; every `stop(`
   call in the Python supervisor and every refusal constructor in the
   runner is indexed; and every C or L row cites
   `RefusalLegality.disposition`.
2. **REFUSAL-VALIDATOR-1 (Clojure).** Write `futon2.aif.refusal-legality`,
   corresponding to `RefusalLegality.lean`. It runs at `close-core!` before a
   refusal outcome is accepted. When the disposition is a debugger error, the
   attached debugger holds. Evidence fields come from the click's carriers;
   none are inferred from the keyword.
   *Progress (2026-11-05):*
   - The pure predicates landed as `69709aeb7` and `7c1a4ed59`.
   - The scored-field carrier `retained-scored-field` landed as `9838e584b` and
     `883171553`. It returns `:outer-field/admitted` minus this click's
     reading refusals, as `ReadingRefusalReselection.reducedField` does. A
     missing carrier is nil, not `[]`, so L1 cannot pass without it.
   - Holding at `close-core!` does not work, because `close!` catches every
     throwable. The hold therefore moves to the start of `close!`, before
     anything is persisted, and calls `debugger/await-restart!` at
     `:close-refusal`. This is REFUSAL-VALIDATOR-1c, which is in progress.
   - `evidence-for-close` landed as `382972e9f`, with the fix `649f1a4d3`.
     Validation is keyed on the work outcomes (`:grounded-change`,
     `:grounded-progress`), and every other outcome is validated. The reason:
     `outcome-from` passes unknown outcomes through unchanged, so a closed
     no-work set would have let them skip validation. No runner path sets
     `:empty-field`, so a runner abstention always becomes a debugger error.
     L1 is decided at step 2a. The close wiring is REFUSAL-VALIDATOR-1d.
   - The restart controller `resolve-close-refusal` landed as `61a86354a`,
     with the fix `ed91ad780`. On `:retry` it reads the carriers again, so a
     live repair can release the hold. A `:use-value` keeps a record of what it
     replaced. The only part left is wiring it into `close!`, which is
     REFUSAL-VALIDATOR-1e.
   - The wiring landed as `1533c6b94`, with the fix `99d0190a1`. `close!` now
     validates before `observe-end!` and before anything is written. The
     `:controller-threw` label is used only when the controller itself threw.
     Not yet tested through `close!`: nothing is written while a refusal is
     held. That is REFUSAL-VALIDATOR-1f, and step 2 is ticked when it lands.
   - **Step 2 done.** REFUSAL-VALIDATOR-1f landed (codex-15 `f999a4909`). It
     moves the pre-catch part of `close!` into a public `validated-close!`, and
     `futon2.aif.close-refusal-wiring-test` checks four things: no observation or
     close happens while the click is held, a work outcome passes through
     unchanged, a detached close keeps its disposition, and a throw from the
     await stops all persistence. To check the test itself, a mutant that
     calls `observe-end!` before validating fails two of these tests.
   2a. **REFUSAL-VALIDATOR-OUTER.** The outer selector's own empty-field
   abstention never reaches runner `close!`. It returns `{:absent
   :no-task-selected}` directly (`full_loop_runtime.clj:1702-1707`), and the
   scored field is in hand there. That is a separate validation site.
3. **REFUSAL-VALIDATOR-2 (supervisor).** `stop()` takes a typed case.
   - L2 needs the STOP file or the CLI budget.
   - L1 needs the run record's scored field.
   - Every other reason is labelled `:debugger-error/outside-repair` in the
     record and the notification.
   - The legacy shell loop is retired, or its stops go through the same
     check.
   *Progress (2026-11-05):* this landed as `2b5326a24` (codex-18), with the
   fix `533c46305`.
   - Each of the 18 `stop()` calls passes one literal case and its evidence.
   - A legal-stop claim whose evidence fails is recorded as `debugger-error`,
     with `claimed-case` kept.
   - Every repair, reload, launch or crash stop, the stall stop, and the "no
     G below abstention" stop are recorded as `debugger-error/outside-repair`.
   - The fix reads the run record as EDN. The earlier text scan let an
     admitted field of `nil`, or a nested one, pass as an empty field.
   - No call site claims `work-exhausted` yet; that waits on step 2a.
   - The legacy shell loop is still open.
4. **C1-EXCLUSION-1.** A persistent exclusion row keyed by target and
   evidence digest, consumed by preselection. It becomes
   `RefusalLegality.nextField`, and is lifted when the digest changes.
   `:already-satisfied` needs a measured satisfaction to qualify.
5. **C2-BRIEF-1.** The question carrier: the cited ambiguity and two
   readings, sent to `morning_brief/queue-item!`, plus the exclusion until
   answered.
6. **C3-HARM-1.** Named harm rules. A guardrail refusal qualifies only by
   matching one.

Until 4-6 land, the validator classes every C-looking refusal as a debugger
error, as the survey predicts. That is the intended interim state: those
refusals hold for repair instead of quietly ending clicks.
