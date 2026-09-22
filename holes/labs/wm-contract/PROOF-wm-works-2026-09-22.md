# Structured proof plan: the War Machine works

**Author:** claude-5, 2026-09-22, at Joe's direction.
**Form:** Lamport-style structured proof.
**Status:** DRAFT rev 2 (after Zai review f4ab52a7): ⟨1⟩4/⟨1⟩5 swapped so C precedes B; the rest amended per Zai's Missing list.
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

**PROVE:** the THEOREM, by ⟨1⟩1–⟨1⟩10.

---

### ⟨1⟩1. A click can be fired by one command, and it runs to a close.

- **CHECK.**
  - `scripts/wm_click.sh --run` (or its replacement) reaches a written `007-closed.edn`, with
    no `--force` and no human intervention between the command and the close. The agents'
    own turns (author, reviewer) are part of the click.
  - The time is recorded as two figures: machine compute and agent wait.
- **PROOF.**
  - ⟨2⟩1. Casting: author, reviewer and repair seats are named once (the wm-author and
    wm-reviewer lanes) and are always registered. CHECK: preflight finds them without
    intervention.
  - ⟨2⟩2. Nothing blocks firing except Agency being down. The 13 tripwires (T1–T13,
    `tripwire.clj:600`) exist to ensure correct behaviour *during* a run (Joe, 2026-09-22);
    they were never meant to decide whether a click may fire. Any place where a tripwire, or
    a preflight check in `wm_click.sh`, refuses or halts the firing of a click is a defect
    removed in this step. Tripwires keep acting during the run as designed. A finding still
    opens a ticket in the ordinary queue, and a ticket never stops a click. CHECK: `wm_click.sh
    --run` has no refusal path other than Agency being unreachable, and a click fires while a
    tripwire reports.
  - ⟨2⟩3. The close accepts well-formed work. The reviewer's standing decision is produced in
    the shape the close reads (fixed on 2026-09-21 by 6d45e8b7). CHECK: a close is not refused
    `:explanation-invalid` for work the reviewer approved.
- **Known failures carried in:**
  - r4-1: `:explanation-invalid` (reviewer template shape);
  - r4-2: `:guardrail-refusal` (artifact scope);
  - grants 2 and 3: lost to casting (comments in `wm_click.sh`);
  - `wm_click.sh` refuses to fire when preflight tripwires would halt (lines ~111-233).
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
    patterns. They are written by an offline authoring agent (a Codex seat) *before* the click,
    because selection needs candidates to exist before it runs. The click-time author builds
    the chosen candidate; it does not invent the alternatives.
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

### ⟨1⟩4. C: a prospective preference over run-ending classes.

- **CHECK.**
  - A mapping from a candidate's predicted observations to the run-ending classes (attested
    increment on focus / associated / elsewhere, known typed failure) is implemented and
    named. It uses the same class definitions as `run_ending_classification.clj`.
  - C over those classes is Joe's 55/35/5/5, held fixed. It is not put on the token powerset
    (the fix list's no-op counterexample).
  - `:unknown` and missing evidence are handled as the mapping states, never counted as a
    class.
  - The mapping is applied to the reference candidates' predicted observations from the
    *declared-prior* rollout (`PolicyRollout.predictedOutcome` with the declared, unlearned
    B), so this check runs before ⟨1⟩5. The values are recorded. They are marked superseded
    when ⟨1⟩5's learned B lands; ⟨1⟩5 records the new ones.
- **PROOF.** ⟨2⟩1. The mapping reuses improve-8's classification kernel, run on predicted
  rather than attested observations.
- **FAILURES:** —

### ⟨1⟩5. B from recorded outcomes: under ⟨1⟩4's C, the candidates' predictions differ by more than habit.

- **CHECK.**
  - B's parameters on the reference field come from recorded outcomes of past actions, with a
    declared prior and update rule. The source is named.
  - `PolicyRollout.predictedOutcome` is computed for each reference candidate.
  - The expected log-preference under ⟨1⟩4's C differs between the candidates by more than
    their recorded log-habit difference. (Zai 1.4: "not equal" was too weak and would have
    sent ⟨1⟩6 back.)
  - The values are recorded.
- **PROOF.**
  - ⟨2⟩1. Amend the learning-trial contract by adding a new version of the pinned entry, not
    by editing it in place. `data/wm-learning-trials/attempts.edn` declares
    `:mode :record-only`, `:consumption :not-authorized`, and `:does-not-establish` a
    production parameter update. This step changes that declaration, so that B may be read
    in production. Joe's signature on this plan is the authorisation, recorded here.
  - ⟨2⟩2. A production reader of the ledger. `learning_trial_ledger.clj` has none today.
  - ⟨2⟩3. Parameter placement: which transition a whole-attempt outcome informs.
  - ⟨2⟩4. The carry across a change in task domain (`:carry-domain-changed`) is migrated or
    explicitly reinitialised. The identity check stays.
- **FAILURES:** —

### ⟨1⟩6. G separates the reference candidates.

- **CHECK.** On the reference field, `PolicyHorizon.horizonEFE` gives G values whose
  difference is large enough that `σ(log E − F − γG)` ranks the candidates differently from
  habit alone. The tie-break `:action-name-ascending` (`cascade_selection.clj:126-129`) is not
  invoked.
- **Also recorded at this step:** the other terms of `σ(log E − F − γG)` on the reference field,
  each with its value and source:
  - E, the habit prior;
  - F, the variational free energy of each policy from past evidence;
  - γ, the precision.

  If F or E outweighs the G difference, the failure is logged here, and the work at this step
  is to trace that term to the data it came from. This step owns E, F and γ.
- **Known failures carried in:**
  - r4-1: G 9.70476 vs 9.70602, decided by habit;
  - click 3: G identical at 9.704307, decided by the tie-break.
- **FAILURES:** —

### ⟨1⟩7. A live click on the reference field is decided by G.

- **CHECK.** A click fired by ⟨1⟩1's command, on the reference field, records a selection
  consistent with ⟨1⟩6's values:
  - the receipts name the functions used in ⟨1⟩3–⟨1⟩6;
  - the tie-break is not invoked;
  - the live JVM's loaded source matches B₀ plus the merged steps. The command reads the JVM
    start time and the reload records through the read-only reflection route, and lists every
    namespace on the click path whose file changed after its last load. It must list none.
- **FAILURES:** —

### ⟨1⟩8. The selected work is delivered and the close accepts it.

- **CHECK.** The ⟨1⟩7 click's close is an accepted increment:
  - the author's commit is reviewed;
  - every required commit and gate is verified;
  - `verify-close` is true.
- **If the build dies mid-way** (timeout, cancellation, partial commit), the attempt is
  recorded as abandoned, never as accepted. A resume continues the same attempt id. A retry
  starts a new attempt and does not duplicate commits. An unchanged deterministic failure is
  not rerun: it is logged here and fixed first.
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
that G is built from C and B drawn from data. ⟨1⟩8 gives the accepted close, and ⟨1⟩9–⟨1⟩10
the update that the next selection consumes.

## Sign-off

| Step | Zai GLM | Codex | Joe |
|---|---|---|---|
| Execution rules | SIGN | OBJECT | |
| THEOREM / ASSUME | SIGN | OBJECT | |
| ⟨1⟩1 | SIGN | OBJECT | |
| ⟨1⟩2 | SIGN | OBJECT | |
| ⟨1⟩3 | SIGN | OBJECT | |
| ⟨1⟩4 | SIGN | OBJECT | |
| ⟨1⟩5 | SIGN | OBJECT | |
| ⟨1⟩6 | SIGN | OBJECT | |
| ⟨1⟩7 | SIGN | OBJECT | |
| ⟨1⟩8 | SIGN | OBJECT | |
| ⟨1⟩9 | SIGN | SIGN | |
| ⟨1⟩10 | SIGN | SIGN | |

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

## Zai GLM review (rev 2)

**Reviewer model:** Zai GLM (zai-1), GLM-5.x. Adversarial, as before: each
changed part attacked against the code and records before signing.

### The swap: C before B — SIGN

Attack on the new order: does C-first reintroduce a back loop between ⟨1⟩3 and
⟨1⟩6? Traced link by link: ⟨1⟩3 supplies the belief state; ⟨1⟩4's C-mapping
needs *predicted observations* for the reference candidates — before ⟨1⟩5's
outcome-updated B exists, these can only come from the current declared-prior
rollout (see Missing 1); ⟨1⟩5 then supplies the outcome-updated B and the
separation check; ⟨1⟩6 consumes both. The weakened joint I objected to
(epsilon "not equal") is gone: ⟨1⟩5's criterion is now exactly the
habit-difference margin, which is what ⟨1⟩6 needs to outrank habit. No back
loop: if ⟨1⟩6 still fails, the cause is F or γ (see Missing 2), which is not
any earlier step's criterion being too weak — it is a component no step owns,
and rule 2 correctly stops the proof for Joe to amend.

### A3 (ticket as ordinary front candidate) — SIGN, with a required Joe line

Attack: A3 asserts the front ticket "does not stop ⟨1⟩1's click", but the fix
list row committed as bbae7593 records Joe's ruling "an unresolved front
ticket stops the line". These conflict. A3's reading (repair is ordinary
selection at the front, from 6714b3ac) is plausible, but the supersession of a
recorded Joe ruling cannot be claude-5's interpretation — Joe must say it in
his signature or A3 must quote his ruling superseding bbae7593. Requiring the
line, not objecting: the keep/revert decision is already Joe's.

### A4 (Joe names blocking tripwires) — SIGN

Verified the count: `tripwire.clj:600` defines exactly 13 wire evaluators
(T1–T13). The assumption correctly bounds Joe's decision to a real, finite
list, and only named tripwires may block — nothing here adds ceremony.

### ⟨1⟩1 — SIGN, one reconciliation

"No human intervention; the agents' own turns are part of the click" fixes my
wording attack. The finding→ticket path stays and never stops a click —
carried in. One inconsistency: ⟨2⟩2 still says "the click fires unless Agency
is down", which contradicts A4's named blocking tripwires. Reconcile: "unless
Agency is down or an A4-named tripwire blocks" (Missing 3).

### ⟨1⟩2 ⟨2⟩1 (offline Codex-authored interpretations) — SIGN

The decision was made and it is the right one: candidates must exist before
selection runs, so the alternatives are authored offline, and the click-time
author builds only the chosen one — which also removes the incentive for the
author to steer selection. Attack on the thin boundary between an
"offline-authored interpretation" and a "hand-declared route": the CHECK still
requires the *target* to come from the registry or queue and the candidates to
have different first actions, and management rules put a different agent on
review. Survived.

### ⟨1⟩4 (now C) — SIGN, two clarifications

The class definitions reuse `run_ending_classification.clj`, C is Joe's
55/35/5/5 held fixed, `:unknown` never counts as a class, no token powerset —
all the constraints I wanted. Clarifications: (a) name the source of the
"current predicted observations" the mapping is applied to at this step —
before ⟨1⟩5 it can only be the declared-prior rollout (Missing 1); (b) state
that ⟨1⟩4's recorded values are superseded when ⟨1⟩5's outcome-updated B lands
(⟨1⟩10's recompute covers it, but say it) (Missing 4).

### ⟨1⟩5 (now B, strengthened) — SIGN, one required line

The separation criterion now exceeds the recorded log-habit difference — the
back loop is removed at the source, and the entry credits the fix to my
objection. ⟨2⟩1 names the attempts.edn contract amendment with Joe's plan
signature as authorisation — my second objection, answered. Required line: the
ledger pins `meaning-sha256` and meanings are versioned; the amendment must
re-version the ledger entry, not edit the declaration in place (Missing 5).

### ⟨1⟩7 (loaded-source command) — SIGN

The command is now runnable and checkable: JVM start time plus reload records,
listing click-path namespaces whose file changed after last load, must list
none. Survived my attack (a stale-namespace check a person can run and see).

### ⟨1⟩8 (build-death handling) — SIGN

Abandoned-never-accepted, resume keeps the attempt id, retry starts a new
attempt without duplicating commits, deterministic failures are not rerun.
Attack: resume-after-death is a new machine capability, possibly substantial —
but it is spec'd as behavior with checkable outcomes, which is what this plan
is for. Survived.

### Missing (rev 2)

1. **⟨1⟩4's predicted observations have no named source** before ⟨1⟩5's B.
   Name it (the declared-prior rollout) so the check is runnable on day one.
2. **No step owns F or γ.** G = σ(log E − F − γG): ⟨1⟩4 owns E's source,
   ⟨1⟩5 owns B — but the risk/free-energy term F and the precision γ have no
   step establishing them from data. If F cancels E at ⟨1⟩6, the proof stops
   with no owning step. Add their provenance to ⟨1⟩3 or ⟨1⟩6's content.
3. **⟨1⟩1 ⟨2⟩2 contradicts A4**: "fires unless Agency is down" vs Joe-named
   blocking tripwires. One clause fixes it.
4. **⟨1⟩4's recorded values are on prior predictions**; mark them superseded
   by ⟨1⟩5's B so a later reader is not misled.
5. **⟨1⟩5 ⟨2⟩1 must re-version the ledger**, not edit the meaning-pinned
   declaration in place.
6. **A3 must carry Joe's explicit supersession of the stop-the-line ruling**
   (bbae7593), not an interpretation of it.

Verdict: **9 SIGN, 0 OBJECT** on the changed parts and the new order. My two
rev-1 objections are both answered at the source. The six Missing items are
wording and ownership fixes; none changes the plan's structure. This review
authorizes no machine changes.


## Codex review

**Reviewer: codex-20; model: GPT-6 (gpt-6-astra).**

**Verdict: 2 SIGN, 10 OBJECT on the 12 table entries. The ⟨1⟩11 conclusion
also does not follow.** Reviewed revision `0ec33988`, then updated against Joe's ruling in
`3cb0e436` (received after the first review), against source and retained
records, including `00042870`, `580dd866`, `f4ab52a7` and `41bafbf3`.
The two SIGNs approve checkable acceptance properties, not the entire ordering
or permission to execute. Zai's cumulative counts are review events, not a
count of distinct proof obligations.

I read the three click narratives, the selection/classification/learning and
checkpoint code, the contract bundle, the reflection implementation, and load
identity code. I evaluated the arithmetic counterexample below locally; I ran
no click, test suite, reload, ingestion or model update. Paths below are
futon2-relative unless another repository is named.

The main issue is that a successful local CHECK need not establish what the
next CHECK needs. Strict execution order does not make that implication true.
The following changes strengthen the CHECKs already requested in this plan;
they are not proposals for new production gates, negative-control suites,
per-step sign-off chains or a new ledger.

### Execution rules — OBJECT

**Attack:** execute the instructions literally. Every step must use the field
fixed in ⟨1⟩2, but ⟨1⟩1 must finish before ⟨1⟩2 starts. Furthermore, ⟨1⟩2
and ⟨1⟩3 ask for a click's receipts before the final live click at ⟨1⟩7.
An early successful click can complete the target and consume its wanted
work. Freezing a copy does not keep that work uncompleted in the live machine.

**Amendment:** prepare and name the reference input before any CHECK that uses
it. Distinguish read-only evaluation of the frozen input from the one actual
execution that changes the world. Freeze inputs, not expected answers or
production state: task/queue snapshot, candidate and action identities,
locators and revisions, observation/prior evidence, ledger cutoff and model
configuration. Define how ⟨1⟩7's live input is shown to correspond to it
without forcing a stale selection or bypassing the real queue. Later replay
is read-only; do not repeat completed work to maintain the fixture. These are
contents of the existing reference-field artifact, not a new approval step.

Joe's rule that he can order a run must remain distinct from whether the
proof has advanced. The proof can stay at a failed step while an explicitly
requested run reports that fact. Do not turn the proof's ordering into the
withdrawn “no click until the repair queue is empty” rule.

### THEOREM / ASSUME — OBJECT

**Attack:** accept all stated assumptions and ask what has actually been
proved. The contract bundle includes model transcriptions and checked runtime
correspondences, not a theorem that these runtime functions are composed
correctly. C = 55/35/5/5 is an owner preference, not a statistic learned from
outcomes; the final paragraph should say so. “By G” also needs the causal
meaning given under ⟨1⟩6, not merely agreement with a ranking.

Joe's subsequent ruling resolves the former pause and firing-authority
questions. His pause applied to claude-5, not the other lanes; I withdraw the
claim that their intervening work required plan-wide freeze reconciliation.
The 13 tripwires are run-time correctness mechanisms, never preflight firing
vetoes. No keep/revert decision or named-blocker list is required by this
review. The removed assumptions are not being reviewed or reinstated.

The file refers to Part C of the repair plan, but the current
`REPAIR-PLAN-2026-09-22.md` has no `## Part C` section. Keep the management
rulings quoted here authoritative and fix the dangling reference; do not
resurrect the superseded Part B's extra sign-offs. B₀ must identify the
runtime data/configuration used by the field as well as repository commits.
A one-field demonstration proves this particular working execution, not
universal success, permanent seat registration, or a speed bound. No new
benchmark or approval gate is requested to enlarge that claim.

### ⟨1⟩1 — OBJECT

**Attack:** a failed click writes a well-formed close. The top CHECK passes,
yet it has not demonstrated ⟨2⟩3's acceptance of valid work. Conversely,
proving acceptance now can require the feasible candidates and measurement
producer deferred to later steps. Checking that `:explanation-invalid` is
absent can pass when no reviewer ever approves anything. A close merely
existing on disk can also be left over from an earlier run.

**Amendment:** separate the already-requested command/lifecycle property from
accepted delivery, establish the field first, and bind command, click id,
attempt id and newly written close. Decide whether this is an existing
historical lifecycle witness or a new execution; do not silently consume the
reference task before ⟨1⟩7. Scope “always registered” to the cast seats for
that execution, with an explicit terminal account of unavailable/busy seats.
Record the runtime handling of a busy seat or an already-active click and
bind this command's result to its own attempt. That is a lifecycle/reporting
requirement, not a preflight veto: do not mistake another run's close for this
command's successful execution. I accept the revised ⟨2⟩2 firing rule; the
remaining objection concerns the CHECK's lifecycle/acceptance implications.


The wait/compute split is named but not defined: author/reviewer turns may
run subprocess computations, and waits can overlap. Specify which recorded
intervals count as agent wait, machine time and overlap so the two figures do
not misattribute or double-count wall time. This is reporting, not a budget gate.

**Coupling attack:** `tripwire.clj:980`'s `note!` records a trip, but ticket
publication depends on the action selected by `record-trip!` / `handle-action!`
and the repair finding writer (`tripwire.clj:750`; `repair_obligation.clj`
publication calls). Printing all wires in the shell does not demonstrate that
chain. In this CHECK's reporting witness, follow the named finding through to
its ordinary ticket and queue identity. Preserve that path when reporting
replaces throwing; do not assert “unchanged” without establishing the caller.

### ⟨1⟩2 — OBJECT

**Attack:** construct two feasible, distinct first actions whose measured
transition predictions are identical. This passes every listed condition and
is exactly sufficient to make ⟨1⟩5 impossible on the now-frozen field.
Alternatively, author novel actions with no compatible historical trials:
⟨1⟩5 cannot obtain a data-informed B without returning to field construction
or waiting for work the plan has not assigned.

**Amendment:** when fixing the field, identify the available, meaning-compatible
past outcome evidence for its action/transition families and the observable
outcome differences the later discrimination argument will use. If those do
not exist, record that fact at field preparation; do not freeze an impossible
claim or manufacture differences by tuning B. This is a property of the
chosen demonstration, not a new admission gate on ordinary candidates.

The offline Codex author is now explicitly assigned, which answers that prior
question. But “not a route declared by hand” conflicts with accepting an agent's
manually written declaration unless the distinction is specified. Require the
existing publication's task-to-pattern reading, observed guards, produced
outcomes and actual construction to establish it; seat identity alone does
not. `cascade_sources/check-file!` is a private declaration-shape validator,
not itself publication or applicability proof (`cascade_sources.clj:106`).
Name the actual loader/publication use and the selection's consumed declaration.
Also identify the real eligible queue stratum: two alternatives for a target
behind an admitted front ticket will not be selectable at ⟨1⟩7.

### ⟨1⟩3 — OBJECT

**Attack:** invoke the three named functions independently, save their outputs,
and let the scorer use another belief. The CHECK can record plausible values
without establishing the required composition. The contract groups have
*different carriers*: `observation/observe` (`observation.clj:103`) produces
aggregate channels from scan data; `belief/reconcile-belief-carry`
(`belief.clj:510`) reconciles entity priors/posteriors; exact token filtering
is in `cascade_model_manifest.clj`. Merely saying all three read “recorded
tokens” does not connect those domains.

**Amendment:** name the actual observation likelihood A, initial/carry belief,
source observations and carrier mappings on this field. The existing CHECK
should recompute the output from those pinned inputs, preserve absent versus
observed values, and establish that this output is the q0 consumed by rollout
and scoring. Include the declared-prior use at this step and how subsequent
learned-B prediction uses it. Missing data rendered numerically as zero must
not become a false observation. The step then supplies a reproducible input,
not just a list of names and printed values.

### ⟨1⟩4 — OBJECT

**Attack:** call the existing classifier on predicted token observations.
`run_ending_classification.clj:69–113` expects a close judgment, occurrence,
route attestation and focus relation; a predicted token set is none of those.
Without them it returns unknown or refuses. Supplying fabricated attestations
to force a success label would falsely turn predictions into evidence.

**Amendment:** reuse the *class definitions* and shared pure classification
logic where appropriate, while explicitly distinguishing prospective predicted
endings from actual close attestations. Define the normalized prospective
kernel and its input carrier. “Unknown never counted as a class” does not
specify what happens to unknown probability mass: silently dropping it and
renormalizing can make the least-observed action look best. State the missing
mass treatment and what the scorer consumes; never synthesize observations.

Also state whether risk is evaluated on the run-ending carrier or token
carrier and which existing contract/function implements that expression.
KL generally changes under aggregation, so a pushforward into four classes
is not automatically the existing token-space G. Define A/ambiguity and
horizon placement consistently with that choice. Before this CHECK passes,
its output must be an input the production-scale horizon scorer can actually
consume, not a mapping that later requires changing ⟨1⟩4 to fit ⟨1⟩6.

### ⟨1⟩5 — OBJECT

**Attack:** its proposed inequality does not imply ⟨1⟩6, even when F = 0 and
γ = 1. A one-step identity-observation example, with class order
[focus, associated, elsewhere, failure], is:

```text
C  = [0.55, 0.35, 0.05, 0.05]
Q1 = [1,    0,    0,    0   ]     E1 = 0.45
Q2 = [0.9,  0,    0,    0.1 ]     E2 = 0.55
E_Q1[log C] - E_Q2[log C] = 0.2397895273
log(E2/E1)                 = 0.2006706955
G1 = KL(Q1 || C)           = 0.5978370008
G2 = KL(Q2 || C)           = 0.5125435546
posterior                 = [0.4289921979, 0.5710078021]
```

The required preference gap exceeds the habit gap, but both G and habit favor
candidate 2. Step 5 passes; step 6's ranking reversal fails. The omitted
predictive entropy causes this counterexample; tracing E/F/γ at step 6 cannot
repair the purported implication. Nonidentity A and multi-step risk introduce
further quantities the current inequality omits.

**Amendment:** let this step establish a justified, consumable B and its actual
predictions, not a mathematically insufficient guarantee about ranking. The
field and subsequent full scoring CHECK must own the real discrimination
claim, using all terms at their declared precision and horizon. Do not adjust
correct data-derived parameters solely to force the desired ranking.

The versioned authorization, production reader and parameter-placement work
are now named. Preserve old ledger records unchanged: `attempts.edn` contains
immutable trial events, whereas `resources/wm/attempt-learning-contract.edn`
is the contract declaration and `attempt_learning.clj:15–21` validates its
version/mode. Specify the new consumer contract and interpretation of old
compatible observations, not replacement “updated” events that double-count
the same trial. Establish persistence, deduplication and meaning/domain
continuity here, because ⟨1⟩10 will depend on them. “Explicitly reinitialised”
must not silently throw away precisely the update ⟨1⟩10 claims survived.

### ⟨1⟩6 — OBJECT

**Attack 1:** a large F difference reverses the habit-only ranking while G has
an arbitrarily small difference. The CHECK passes, but the live action is
not decided by G. Compare with log E − F at the *same* γ and other inputs,
not only with habit; demonstrate the claimed contribution of G through the
existing decomposition/decision evidence. This strengthens the named
“decided by G” CHECK, not a new production veto.

**Attack 2:** per-policy rank does not determine the action. The contracted
`ActionMarginal` sums all policies sharing a first action
(`cascade_selection.clj:132–163`). A field with more than two policies can
rank one policy highest while another action wins the sum. Check the full
eligible action marginal after ticket-front restriction, not just candidate
rank. Keep the actual E/F/γ and their absence semantics; an absent F prefix
is not a measured F = 0 (`cascade_selection.clj:95–116`).

**Attack 3:** `bayes-choice` *always* records `:tie-break-rule
:action-name-ascending`, including strict winners. Absence of that field
cannot mean “tie-break not invoked.” Define this CHECK as a unique maximum
of the computed eligible action masses (with the numeric comparison stated),
not omission of the declared rule. The existing tie-break stays intact.

If a legitimately derived E/F/γ leaves the field nondiscriminating, tracing
its provenance does not guarantee a repair at this step. The earlier field
criterion must not promise separation it has not established, and the proof
must report that limitation rather than modify valid preferences or evidence.

### ⟨1⟩7 — OBJECT

**Attack:** the claimed loaded-source command is not present. The paragraph
describes one but gives no command/endpoint/output field. The read-only
reflection handlers at `futon3c/src/futon3c/transport/http.clj:7346–7396`
call `reflection/core.clj`, whose namespace/Var summaries expose names,
source locations and metadata, not the asserted JVM-start/reload history.
File modification times also do not establish loaded bytes: copying an old
file with preserved mtime or editing after a partial load defeats that test.

**Amendment:** name the existing read interface and captured digest fields
actually used, or assign implementation of the described read capability.
`load_identity.clj:63–108` already compares captured source digests with
canonical files and distinguishes unregistered/unavailable/stale, with stated
limits; reuse that evidence and state its coverage instead of inventing
reflection capabilities. Record the hashes of model declarations and data
consumed too. Establish the source evidence before trusting earlier live
CHECKs, not first at step 7. Reload only from canonical checkouts; this is no
request to restart a shared JVM.

A live result merely consistent with one expected winner can pass while a
hardcoded selector chooses that winner. The same CHECK must join actual
consumed inputs, calculated policy scores, eligible action marginal and chosen
action. It must explain how a live queue/observation snapshot corresponds to
the frozen field; forcibly pinning a target around ordinary selection is not
a valid solution. See the execution-rule objection.

### ⟨1⟩8 — OBJECT

**Attack:** the ⟨1⟩7 click has already run to close under ⟨1⟩1's command.
If the implementation needed to produce measured/attested outcomes is deferred
to ⟨1⟩9, the immutable close cannot acquire that missing evidence later. The
runner currently obtains D-task observations, learning trials and route
attestation *before* constructing the close (`full_loop_runner.clj:3697–3755`).
Their producers must be ready before this live click, even if steps 8/9
inspect the resulting record in order.

`run_ending_classification/verify-close` (`:149–156`) compares the saved close
projection and digest; it does not prove accepted work or even recompute the
class. r4-2 already passed it on a failure. Specify the accepted increment
predicate and bind reviewed commit(s), required evidence and attestation to
this same occurrence. “Every required commit and gate” must use the target's
existing acceptance, not become permission to add unspecified gates.

The new failure/resume paragraph is useful but not yet a coherent lifecycle:
`full_loop_cohort.clj:601–602` rejects appending to an already closed attempt.
If “abandoned” is terminal, it cannot also be resumed with that id. Distinguish
interrupted-but-open resume from closed/abandoned retry, including recognition
of a commit already produced. Put that lifecycle implementation in the command
step before promising unattended execution. No immutable record rewriting.

### ⟨1⟩9 — SIGN

**Attack:** a checkbox reading or `verify-close=true` alone could masquerade
as accepted delivery. This CHECK expressly requires actual post-build token
measurements, a run-ending attestation, and agreement with the classifier;
combined with ⟨1⟩8's accepted increment, these are inspectable properties.
Run the classifier on the actual close/attestation/focus inputs when checking
agreement; do not substitute the projection-only `verify-close`. Preserve the
same occurrence and reviewed revision bindings. A measured false is valid;
a missing observation is not false. The measurement/attestation producers must
be implemented before the live close as noted above. This SIGN is for the
CHECK's acceptance property, not postponing their implementation to this point.

### ⟨1⟩10 — SIGN

**Attack:** the next action can change merely because the completed task leaves
the queue, and a changed B label need not affect predictions. The stated frozen
with/without-update recomputation addresses that confound, while the actual
next selection must separately show consumption. A changed action is not
required, correctly. Exactly-once update and reload survival are checkable.

Use the same frozen non-B inputs in the recomputation and compare the next
selection's *actual consumed parameter identity/value*, not just an available
ledger version. The next live task can differ from the completed reference
task; its receipt must show which compatible learned state it uses. The
meaning and carry contract established at step 5 supplies that connection.
This SIGN does not authorize replaying the reference task as new production
work or counting the same outcome again.

### ⟨1⟩11 — OBJECT (conclusion; not an additional sign-off gate)

The conjunction claimed here does not follow from the current CHECKs:
⟨1⟩2 does not ensure the data/discrimination later required; ⟨1⟩5 does not
imply ⟨1⟩6; ⟨1⟩6 does not isolate G's effect on the action; and the live
close can precede implementation of its required observation/attestation
producers. Correct those implications rather than relying on the “no back
loops” instruction to make them true. C is stipulated by Joe; B is inferred
from outcome evidence. Describe those sources separately in the conclusion.

### Missing / disposition of the earlier findings

1. **Reference-field preparation and live/replay identity — missing.** Freeze
   a real, eligible, evidence-supported decision input before using it. Identify
   which stages only evaluate it and which one executes its work. Completing
   a task or publishing a new front ticket changes the live field; retaining a
   directory cannot erase that fact. Do not weaken the queue or observation
   authorities to keep a demonstration fixed.

2. **The prospective/actual ending distinction — incomplete.** C held fixed,
   prior-rollout sourcing and superseding prior values are carried in, but the
   classifier cannot consume raw predicted observations as an actual attested
   close. The carrier, normalized probability mapping, unknown-mass handling,
   and risk/ambiguity composition still need an implementable definition. This
   is the missing connection from the Lean/runtime components to the scorer.

3. **G versus habit and tie-break — named, not yet proved.** Zai's tie-break
   finding is explicitly carried in. Its proposed margin strengthening is not
   valid (numerical counterexample above), and F can account for a rank change.
   Evaluate the contracted whole score and action marginal, retaining lawful
   tie-breaking for ordinary runs. A discriminating field is a demonstration
   property, not a reason to refuse every future legitimate tie or habit choice.

4. **Wait versus compute; feasibility; interpretation author — carried in
   with limits.** The split is now named; define its accounting rather than
   adding a speed budget/sign-off. Feasibility is correctly a candidate
   construction property, not a new admission veto. Offline Codex authorship
   is decided, but substantive construction and compatible historical outcomes
   remain unproved. The plan demonstrates one field, not automatic supply for
   every task; say that explicitly rather than claim the larger result.

5. **Pause scope and firing authority — resolved by Joe, withdrawn as
   objections.** The pause applied to claude-5 alone. Other lanes' changes do
   not require the keep/revert ceremony previously requested here. Tripwires
   ensure correctness during a run; none may prevent firing. The revised
   ⟨1⟩1 ⟨2⟩2 carries that ruling. No new permission, blocker list or sign-off
   is owed for it. Fix the dangling Part C reference without importing the
   superseded per-step Zai/Codex/Joe approval chain or a new census. B₀ remains
   the plan's stated implementation baseline, not a freeze on other work.

6. **Guard→finding→ticket — asserted, not demonstrated.** Reporting witnesses
   and publishing ordinary tickets are different calls. Establish that the
   changed nonblocking caller still reaches the existing durable finding and
   queue publication for the reporting case the plan already asks to observe.
   New findings can alter which task is selected next; reflect that in the
   live/reference input comparison. This is not an objection to Joe's rule
   that a ticket does not prevent firing.

7. **Measurement, accepted close and learning continuity — implementation
   prerequisites remain late.** Implement the post-build observation and
   attestation producers before the live attempt, then inspect their close at
   steps 8/9. Define the producer/consumer contract version and persistent
   parameter interpretation at step 5. Establish command interruption/resume
   behavior at the command step. Otherwise failures at 8–10 require work on
   steps already marked proved. Do not edit old observations to migrate them.

8. **Loaded-source check — not yet runnable as described.** Supply an actual
   existing command/interface and its digest evidence or identify the small
   implementation owed. JVM start time/file mtimes and namespace metadata
   cannot prove the assertion. Include missing coverage as unknown, not a
   silently empty “none stale” list. Use this evidence when any live numerical
   CHECK first depends on the loaded implementation.

A workable revision can keep Joe's strict order: prepare the real reference
input first; establish the command lifecycle and the input/model contracts;
compute the full score/action evidence; prepare the close/update producers;
then execute once and inspect selection, accepted close, measurements and the
next selection in order. These are dependency corrections to the requested
proof, not additional permissions or operational gates. As written, it is not
ready for my sign-off.


**Update after Joe's ruling (`3cb0e436`).** The obsolete pause/keep-revert and
named-blocker objections above have been withdrawn in place. I accept the
revised firing rule and do not request review of removed A3/A4. The table
remains **2 SIGN / 10 OBJECT**: THEOREM/ASSUME still needs the specification
versus runtime-composition and C-source corrections; ⟨1⟩1 still has the
independent lifecycle/reference/acceptance issues stated above. The numerical
counterexample and the remaining ordering, carrier, loaded-source and close
findings are unchanged. No machine work was performed for this update.
