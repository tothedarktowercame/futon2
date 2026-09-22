# Structured proof plan: the War Machine works

**Author:** claude-5, 2026-09-22, at Joe's direction.
**Form:** Lamport-style structured proof.
**Status:** DRAFT.
- Sign-off: Zai GLM (adversarial), then Codex, then Joe.
- This plan supersedes Part B of `REPAIR-PLAN-2026-09-22.md`. Parts A and C of that plan stay
  in force.

## How this proof is executed

1. **Order.** Steps are proved strictly in order. Step ⟨1⟩k is not started until ⟨1⟩(k−1) is
   proved.
2. **No back loops.**
   - Every step is checked on the **reference field** fixed in ⟨1⟩2.
   - Each step's criterion is written to be strong enough that the next step can use it.
   - When a step fails, the failure is recorded under that step, and the work stays at that
     step until its criterion holds. The work does not move to another component.
   - If a failure shows that an earlier step's criterion was too weak, the proof stops. Joe
     decides whether to amend that criterion. An agent never does this silently.
3. **A step is proved** when its CHECK is run and passes, and the result is recorded under the
   step with the commit, the click id or the record path. "Merged", "tests green" and
   "receipt written" do not prove a step.
4. **Failure log.** Each step has a FAILURES list. Each entry records:
   - the date;
   - what was tried;
   - what the check showed;
   - the cause found.

   Entries are never deleted.
5. **Management rules** (Part C of the repair plan, as Joe gave them on 2026-09-22):
   - The machine runs when Joe says run. Checks before a click report; they do not block,
     unless running is impossible.
   - No new check, guard, gate, grant, negative control or sign-off is added unless Joe asks
     for it by name.
   - Every report opens with the step reached and whether its check passed.
   - Work is implemented by a Codex agent. A different agent reviews it. claude-5 writes each
     step's instructions and runs its check.

---

**THEOREM.** One click, fired by one command:
- chooses among real alternatives built from the real tasks;
- makes that choice by expected free energy G, computed from preferences C and a transition
  model B that are built from real data;
- delivers the chosen work to an accepted close;
- feeds the measured outcome back, so that the next selection's B has changed.

**ASSUME:**
- A1. The Lean model in `mathlib4/DarkTower/WarMachine/` and the contract bundle
  `machine-contracts/machine-contracts.json` (16 contracts, 36 declarations) are the
  specification. Their theorems are not re-proved here.
- A2. The codebase at a baseline commit B₀ of futon2, futon3c and mathlib4, fixed when Joe
  signs.
- A3. Joe has decided each change made after the plan was drafted (16d4482c, 05:14Z):
  - `d1e9e96b`: EIG shadow code;
  - `8f97757b`: withdrawal of the two-layer calibration candidate;
  - ticket `T-repair-occ-444fb018…`: now at the front of the queue.

  For each: keep or revert. The decisions are recorded here before ⟨1⟩1.

**PROVE:** the THEOREM, by ⟨1⟩1–⟨1⟩10.

---

### ⟨1⟩1. A click can be fired by one command, and it runs to a close.

- **CHECK.**
  - `scripts/wm_click.sh --run` (or its replacement) reaches a written `007-closed.edn`, with
    no `--force` and no manual steps between the command and the close.
  - The time is recorded as two figures: machine compute and agent wait.
- **PROOF.**
  - ⟨2⟩1. Casting: author, reviewer and repair seats are named once (the wm-author and
    wm-reviewer lanes) and are always registered. CHECK: preflight finds them without
    intervention.
  - ⟨2⟩2. Preflight reports and does not refuse. Its 13 tripwires and seat checks print their
    findings, and the click fires unless Agency is down. CHECK: a click fires while a tripwire
    reports.
  - ⟨2⟩3. The close accepts well-formed work. The reviewer's standing decision is produced in
    the shape the close reads (fixed on 2026-09-21 by 6d45e8b7). CHECK: a close is not refused
    `:explanation-invalid` for work the reviewer approved.
- **Known failures carried in:**
  - r4-1: `:explanation-invalid` (reviewer template shape);
  - r4-2: `:guardrail-refusal` (artifact scope);
  - grants 2 and 3: lost to casting (comments in `wm_click.sh`).
- **FAILURES:** —

### ⟨1⟩2. The reference field: a real target with at least two admissible candidates that begin with different actions.

- **CHECK.** One click's candidate receipt shows all of the following:
  - a target taken from the real tasks (the registry via substrate-2, or
    `data/wm-ticket-queue/queue.edn`), not a route declared by hand;
  - at least two admitted candidates for it;
  - different first actions for those candidates;
  - locators that resolve, and acceptance that can be met within the artifact scope.

  That target and candidate set are frozen as the **reference field** in
  `runs/proof-reference-field/`. Every later step is checked on it.
- **PROOF.**
  - ⟨2⟩1. Interpretation. `mission_hole_wants.clj:74-91` supplies wants but leaves
    `:interpretation` and `:candidates` empty. Build the interpretation of the target's
    patterns. The step states who writes it: an offline authoring agent, or the author at click
    time.
  - ⟨2⟩2. Construction and publication through `cascade_sources/check-file!`, consumed by the
    next selection.
  - ⟨2⟩3. Feasibility. Each candidate's acceptance can be met in the repositories and with the
    evidence available. This is a property of the candidates built in ⟨2⟩1–⟨2⟩2, not a new
    gate. Examples to avoid: r4-2 needed mathlib4; click 3's candidate needed held-out
    evidence that does not exist.
- **Known failures carried in:**
  - r4-2: 90 proposals, 0 admission joins;
  - click 3: three candidates, all from routes declared by hand;
  - fix-5: 441 wants yielded none.
- **FAILURES:** —

### ⟨1⟩3. Observation and belief on the reference field are computed from data.

- **CHECK.** On the reference field, the click's receipts show the belief state computed by
  the contracted functions (`wm-machine-observe`, `wm-machine-belief-state`, `wm-exact-belief`)
  from the recorded tokens, and the values are recorded.
- **FAILURES:** —

### ⟨1⟩4. B and Q(o|π): the candidates' predicted outcomes differ.

- **CHECK.**
  - On the reference field, B's parameters come from recorded outcomes of past actions, with a
    declared prior and update rule. The source is named.
  - `PolicyRollout.predictedOutcome` gives the reference candidates predicted outcome
    distributions that are **not equal**.
  - The values are recorded.
- **PROOF.**
  - ⟨2⟩1. A production reader of the learning-trial ledger. `learning_trial_ledger.clj` has
    none today.
  - ⟨2⟩2. Parameter placement: which transition a whole-attempt outcome informs.
  - ⟨2⟩3. The carry across a change in task domain (`:carry-domain-changed`) is either
    migrated or explicitly reinitialised. The identity check stays.
- **FAILURES:** —

### ⟨1⟩5. C: preferences separate the reference candidates' predicted outcomes.

- **CHECK.**
  - C is built prospectively from Joe's stated classes (55/35/5/5, held fixed) through a
    mapping from each candidate's predicted observations to run-ending classes. It is not put
    on the token powerset.
  - The expected log-preference of the reference candidates' predicted outcomes differs by
    more than the recorded habit difference between them.
  - The values are recorded.
- **FAILURES:** —

### ⟨1⟩6. G separates the reference candidates.

- **CHECK.** On the reference field, `PolicyHorizon.horizonEFE` gives G values whose
  difference is large enough that `σ(log E − F − γG)` ranks the candidates differently from
  habit alone. The tie-break `:action-name-ascending` (`cascade_selection.clj:126-129`) is not
  invoked.
- **Known failures carried in:**
  - r4-1: G 9.70476 vs 9.70602, decided by habit;
  - click 3: G identical at 9.704307, decided by the tie-break.
- **FAILURES:** —

### ⟨1⟩7. A live click on the reference field is decided by G.

- **CHECK.** A click fired by ⟨1⟩1's command, on the reference field, records a selection
  consistent with ⟨1⟩6's values:
  - the receipts name the functions used in ⟨1⟩3–⟨1⟩6;
  - the tie-break is not invoked;
  - the live JVM's loaded source matches B₀ plus the merged steps.
- **FAILURES:** —

### ⟨1⟩8. The selected work is delivered and the close accepts it.

- **CHECK.** The ⟨1⟩7 click's close is an accepted increment:
  - the author's commit is reviewed;
  - every required commit and gate is verified;
  - `verify-close` is true.
- **FAILURES:** —

### ⟨1⟩9. The outcome is measured and attested.

- **CHECK.**
  - The ⟨1⟩8 close records the wanted tokens as measured after the build, true or false and
    not `:missing`.
  - It records an attested run-ending class from `run_ending_classification.clj`.
  - The receipt and the kernel agree.
- **FAILURES:** —

### ⟨1⟩10. The outcome updates B, and the next selection uses the update.

- **CHECK.**
  - The ⟨1⟩9 outcome updates the named B parameter exactly once, and the update survives a
    reload.
  - On the frozen reference field, recomputing ⟨1⟩4–⟨1⟩6 with and without the update changes
    the predicted outcome, as the update rule says it should.
  - The next click's receipts show that it consumed the updated B.
- **FAILURES:** —

### ⟨1⟩11. Q.E.D.

⟨1⟩2 and ⟨1⟩7 give a choice among real alternatives, made by G. ⟨1⟩4 and ⟨1⟩5 establish
that G is built from B and C drawn from data. ⟨1⟩8 gives the accepted close, and ⟨1⟩9–⟨1⟩10
the update that the next selection consumes.

## Sign-off

| Step | Zai GLM | Codex | Joe |
|---|---|---|---|
| Execution rules | | | |
| THEOREM / ASSUME | | | |
| ⟨1⟩1 | | | |
| ⟨1⟩2 | | | |
| ⟨1⟩3 | | | |
| ⟨1⟩4 | | | |
| ⟨1⟩5 | | | |
| ⟨1⟩6 | | | |
| ⟨1⟩7 | | | |
| ⟨1⟩8 | | | |
| ⟨1⟩9 | | | |
| ⟨1⟩10 | | | |
