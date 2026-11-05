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

## Carried-forward tensions (for the talk-through)

1. **"Lean is correct and never changes" against a new theorem.** I read
   Joe's rule as forbidding edits that bend existing Lean to fit the Clojure,
   not new statements. Criterion 1 adds a file and changes none. To confirm.
2. **"Almost always".** In which cases is a stop optimal, besides an empty
   admissible field? Two candidates:
   - Operator termination: a STOP file, or budget exhaustion. Here the stop
     comes from outside the model, so it is not the machine's choice.
   - A want genuinely already satisfied: `:already-satisfied` produced by
     the selection step itself. It is currently a recorded defect. Is it
     instead a sign that the admissible set was mis-scored?
3. **Does the WM's G actually score "do nothing"?** If inaction is not an
   explicit policy in the generative model, the theorem has nothing to bind
   to. Then the first delta is adding the null policy to the admissible set,
   with its predicted consequences.
4. **Refusals by other agents.** An author or reviewer seat refusing is the
   environment acting, not the machine choosing. The Lean already treats an
   invalid author response as a non-injury. The question is whether the
   *click* should then end (a stop) or continue, by re-asking, re-selecting
   or recording.
