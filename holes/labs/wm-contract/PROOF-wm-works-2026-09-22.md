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
| Execution rules | SIGN | | |
| THEOREM / ASSUME | SIGN | | |
| ⟨1⟩1 | SIGN | | |
| ⟨1⟩2 | SIGN | | |
| ⟨1⟩3 | SIGN | | |
| ⟨1⟩4 | OBJECT | | |
| ⟨1⟩5 | SIGN | | |
| ⟨1⟩6 | SIGN | | |
| ⟨1⟩7 | SIGN | | |
| ⟨1⟩8 | SIGN | | |
| ⟨1⟩9 | SIGN | | |
| ⟨1⟩10 | SIGN | | |

## Zai GLM review

**Reviewer model:** Zai GLM (zai-1), GLM-5.x. Adversarial standard, as in my
580dd866 review of the repair plan: each item signed only after a stated
attempt to break it against the code, records and click histories.

Verification base (read-only): `machine-contracts/machine-contracts.json`
(16 contracts / 36 declarations — both counts reproduce exactly),
`scripts/wm_click.sh`, `cascade_selection.clj:126-129`, the three STAGES
files, `data/wm-learning-trials/attempts.edn`, `data/wm-ticket-queue/queue.edn`,
commits 6d45e8b7 / d1e9e96b / 8f97757b.

### Execution rules — SIGN

Attack: rule 3 ("merged / tests green / receipt written do not prove a step")
could be read as *requiring* a new verification apparatus per step — red tape.
It survives: every CHECK named in ⟨1⟩1–⟨1⟩10 is a read of an artifact a click
already produces (receipt, close file, ledger) or a recorded figure; none adds
a gate. One note for rule 4 (failure logs never deleted): keep them in this
file or a sibling markdown, not a new store.

### THEOREM / ASSUME — SIGN

Attack on A1's counts: I recounted the bundle — 16 contracts, 36 declarations
in the `declarations` arrays. Both exact. A2 defers B₀ to Joe's signature:
fine. A3 carries my freeze findings (d1e9e96b, 8f97757b, the front ticket)
with keep-or-revert to Joe — carried in. One ordering hazard from A3 is in
Missing item 2.

### ⟨1⟩1 — SIGN, one wording fix

Attack: "no manual steps between the command and the close" is ambiguous —
the author and reviewer agents take multiple turns between them; if "manual
steps" included agent turns, the check can never pass. It must mean *no human
intervention and no `--force`*, with agent turns allowed. Say so. The
machine-compute / agent-wait split is carried in (my 580dd866 finding). ⟨2⟩2's
"preflight reports and does not refuse" correctly implements Joe's rule that
checks report and do not block — but see Missing item 5 on which tripwires, if
any, still protect an invariant.

### ⟨1⟩2 — SIGN, one decision owed

Attack: the check could be satisfied by an easy hand-picked target. It
survives: the target must come from the registry or the ticket queue, not a
hand-declared route (click 3's failure mode is named), and the candidates must
have different first actions and resolvable locators. Feasibility as a
property of construction, not a new gate — my finding, carried in correctly.
Owed before build: ⟨2⟩1 says "the step states who writes" the interpretation
(offline agent vs click-time author) — that decision changes the whole shape
of the workflow and is not yet made. Decide it before implementation, not
during.

### ⟨1⟩3 — SIGN

Attack: "receipts name the contracted functions" is close to the label-as-
evidence trap I objected to in the repair plan's rev 1. It survives here
because ⟨1⟩6 and ⟨1⟩7 check *values* from these functions, not names; ⟨1⟩3 is
provenance, not proof of discrimination. Adequate as written.

### ⟨1⟩4 — OBJECT

Two attacks land:

1. **"Predicted outcome distributions that are not equal" is too weak.** Any
   epsilon difference passes; ⟨1⟩6 then needs a G gap large enough to outrank
   habit, and with epsilon-level separation it will fail — sending work back
   to ⟨1⟩4, which the no-back-loop rule forbids. Strengthen the criterion the
   way ⟨1⟩5 already does: separation must exceed the recorded habit difference
   between the candidates (or name a Joe-agreed margin).
2. **The existing learning contract disclaims this step's purpose.** I read
   `data/wm-learning-trials/attempts.edn`: the recorded trial's contract is
   `:mode :record-only`, `:consumption :not-authorized`, and explicitly
   `:does-not-establish #{:individual-pattern-firing :pattern-causality
   :production-parameter-update}`. A builder implementing ⟨2⟩1 (production
   reader of the ledger) will hit a standing, signed disclaimer that forbids
   using this data for production B. The step must name that contract
   amendment (record-only → production update rule, with Joe's sign-off) as
   part of the work; otherwise the check passes only by violating a declared
   contract — exactly the class of defect Part A confesses.

### ⟨1⟩5 — SIGN

Attack: the prospective-mapping design (how `:unknown` is treated,
normalisation) is unspecified, so two builders could produce different C's.
Survives: the check constrains the *outcome* (expected log-preference
separation exceeding habit difference) and holds Joe's 55/35/5/5 fixed, and
the no-powerset rule blocks the known no-op. The design freedom is real but
the criterion is not gameable by it.

### ⟨1⟩6 — SIGN

My tie-break finding (580dd866 Missing 1) is carried in by name and line, and
the check requires the tie-break NOT to be invoked. Attack: "ranks differently
from habit alone" could pass via a tie-break flip on an irrelevant
perturbation — but the check is on the reference field's recorded G values vs
habit, not on perturbations, so it holds. Survived.

### ⟨1⟩7 — SIGN, one runnability fix

Attack: "the live JVM's loaded source matches B₀ plus the merged steps" is
not runnable as written — a person cannot see pass/fail without a command.
Fix: name the command (compare loaded-resource hashes against the canonical
checkout at the recorded commits, or a cold restart from that checkout before
the click). Amendment, not objection; everything else (receipts naming
⟨1⟩3–⟨1⟩6's functions, no tie-break) is checkable.

### ⟨1⟩8 — SIGN

Attack: the check demands an accepted close but says nothing about mid-build
death — timeouts, partial commits, resume, retries counting abandoned work as
accepted. The old repair plan's B8 specified this; the proof plan dropped it.
The FAILURES log records such an event but prescribes no behavior. See
Missing item 3. The check itself is sound and is the first plan line that
makes "accepted increment" the criterion rather than a narration.

### ⟨1⟩9 — SIGN

Attack: "true or false and not `:missing`" could be satisfied by a measurement
that always reads the checkbox, not the work. Survives: the kernel's
attestation (`run_ending_classification.clj:98-113`) and the receipt/kernel
agreement requirement are in the check, and click 1 showed real
after-build measurement is possible on a grounded close.

### ⟨1⟩10 — SIGN

Attack: "the next click's receipts show it consumed the updated B" could pass
if the next click coincidentally chose the same action. Survives: the frozen
reference field recompute (with/without the update, predicted outcome changes
as the update rule says) is the discriminating part; the next-click receipt is
corroboration. Exactly-once, reload survival, and the carry rules are carried
in.

### 580dd866 findings: carried in?

- Tie-break → ⟨1⟩6/⟨1⟩7, by name and line. Carried.
- Agent-wait vs compute split → ⟨1⟩1 CHECK. Carried (as recording; the old
  budget-agreement step is gone — I take that as Joe's red-tape ruling).
- Feasibility not a gate → ⟨1⟩2 ⟨2⟩3. Carried.
- Freeze decisions → ASSUME A3. Carried (ordering hazard in Missing 2).
- Guard→finding→ticket coupling → **NOT carried.** If preflight stops
  refusing (⟨1⟩1 ⟨2⟩2), does the finding path that opened
  T-repair-occ-444fb018 still fire? Unaddressed. Missing item 4.
- Who authors interpretations → named at ⟨1⟩2 ⟨2⟩1 but still undecided.
  Missing item 1.

### Missing

1. **⟨1⟩2 ⟨2⟩1: decide the interpretation author** (offline authoring agent
   vs click-time author) before implementation begins.
2. **A3 ordering hazard:** if Joe *keeps* the front repair ticket
   T-repair-occ-444fb018, it is stop-the-line and has no cascade source, so
   ⟨1⟩1's click fails before the plan starts. Joe's keep/revert must either
   revert it, resolve it from outside, or feed it to ⟨1⟩2's construction —
   decided before ⟨1⟩1, not at it.
3. **⟨1⟩8 has no failure behavior for mid-build death** (timeout, partial
   commit, resume; retries must not double-count or accept abandoned work).
   Add it to ⟨1⟩8 or the execution rules.
4. **Preflight de-blocking vs the finding path:** state whether tripwire
   findings still open repair tickets when they no longer block; if not, the
   machine loses its only proven path from defect to ticket.
5. **Which tripwires, if any, still block.** "The click fires unless Agency
   is down" removes all blocking; if any of the 13 tripwires protects an
   invariant that firing would corrupt (authority, store locks), Joe should
   name it rather than discover it by corruption.
6. **⟨1⟩4's separation margin and contract amendment** (the OBJECT above).
7. **⟨1⟩7's loaded-source check needs its command named.**

Verdict: **11 SIGN, 1 OBJECT (⟨1⟩4)**. The structure is right: strict order,
no back loops, checks on artifacts a click already produces, and the theorem's
four clauses map one-to-one onto ⟨1⟩2/⟨1⟩7, ⟨1⟩4–⟨1⟩6, ⟨1⟩8 and ⟨1⟩9–⟨1⟩10.
The single objection is a required strengthening, not a rejection. This
review authorizes no machine changes.
