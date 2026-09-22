# Structured proof plan: the War Machine works

**Author:** claude-5, 2026-09-22, at Joe's direction.
**Form:** Lamport-style structured proof.
**Status:** DRAFT revision 4 (after codex-20 rev-3 review 0628f87d: command step first, strong G claim only, producers read-only with the B update after the close). Revision 3 was restructured after codex-20 review 289e98e2 (Codex order: prepare input → command → G decides (read-only) → producers → execute once → inspect).
- Sign-off: Zai GLM (adversarial), then Codex, then Joe.
- This plan supersedes Part B of `REPAIR-PLAN-2026-09-22.md`. Parts A and C of that plan stay
  in force.

## How this proof is executed

1. **Order.** Top-level steps ⟨1⟩k are proved strictly in order. Work that repairs a component
   happens inside the sub-steps ⟨2⟩ of the step that needs it. That is part of proving the
   step, not a loop back.
2. **When a step cannot be proved** from lawfully derived data, the proof stops and the fact is
   reported to Joe. Preferences, evidence or parameters are never adjusted to force a check
   to pass. Joe decides what happens next. An agent never amends an earlier step silently.
3. **A step is proved** when its CHECK is run and passes, and the result is recorded under the
   step with the commit, the click id or the record path. "Merged", "tests green", "receipt
   written" and `verify-close` alone do not prove a step.
4. **Failure log.** Each step has a FAILURES list: date, what was tried, what the check
   showed, and the cause found. Entries are never deleted.
5. **Running the machine and advancing the proof are separate.** Joe can order a click at any
   time; the click reports where the proof stands. The proof's order is never turned into a
   rule against running the machine.
6. **Management rules** (Joe, 2026-09-22, quoted here as the authority):
   - "the machine runs when you say run": nothing refuses a click except Agency being
     unreachable;
   - the 13 tripwires exist to ensure correct behaviour during a run, and never decide
     whether a click may fire;
   - no new check, guard, gate, grant, negative control or sign-off unless Joe asks for it by
     name;
   - every report opens with the step reached and whether its check passed;
   - each step's work is implemented by a Codex agent and reviewed by a different agent.
     claude-5 writes the instructions and runs the CHECK.

   Joe's queue rulings stand unchanged:
   - "repair fixes are not different from other fixes, they are just moved to the front of
     the queue" (bbae7593);
   - "what happens if something (of whatever shape) goes into the queue and isn't resolved.
     That, I think, is a stop-the-line failure (not a looping machine) requiring repair from
     outside" (bbae7593).

   This proof does not change either ruling.

---

**THEOREM.** On a real decision input:
- one click, fired by one command, chooses among real alternatives built from the real
  tasks;
- the choice is made by the full selection law `σ(log E − F − γG)`, with G computed from
  Joe's stipulated preference C and a transition model B inferred from recorded outcomes;
- G's contribution to the choice is shown;
- the chosen work reaches an accepted close with a measured, attested outcome;
- that outcome updates B, and the next selection consumes the update.

This is a demonstration on one decision input. It is not a claim that every task gets
candidates automatically, or that every click is fast.

**ASSUME:**
- A1. The Lean model in `mathlib4/DarkTower/WarMachine/` and the contract bundle
  `machine-contracts/machine-contracts.json` (16 contracts, 36 declarations) are the
  specification. The bundle checks that each runtime function corresponds to its
  declaration. It does not prove the functions are composed correctly, and establishing
  that composition is part of this proof.
- A2. The baseline B₀ comprises the futon2, futon3c and mathlib4 commits, together with the
  runtime data and configuration the reference input uses. It is fixed when Joe signs.

**PROVE:** the THEOREM, by ⟨1⟩1–⟨1⟩8.

---

### ⟨1⟩1. One command fires a click that runs to a close, and the loaded code is known.

- **CHECK.**
  - `scripts/wm_click.sh --run` fires on whatever target ordinary selection picks. The
    reference input is prepared afterwards, at ⟨1⟩2, so there is nothing to avoid. It fires with no `--force` and no human
    intervention. The agents' own turns are part of the click.
  - The command, click id and attempt id are bound to a close newly written by that run.
  - The load-identity record (`load_identity.clj:63-108`) for every namespace on the click
    path shows its captured source digest equal to the canonical file. Namespaces not
    covered are listed as unknown, never omitted.
- **PROOF.**
  - ⟨2⟩1. Firing. `wm_click.sh` has no refusal path except Agency being unreachable. Its
    tripwire and preflight refusals (around lines 111–233) are removed from firing. The
    tripwires keep acting during the run as designed.
  - ⟨2⟩2. Casting. The seats for this execution are named in advance. A seat that is busy or
    unavailable is reported with a terminal account. The script never launches into an
    occupied seat, and never starts a second active click.
  - ⟨2⟩3. Finding → ticket. When a tripwire trips during a run, the existing chain still
    runs to completion: `tripwire.clj` `note!` → `record-trip!` / `handle-action!` → the
    repair finding writer (`tripwire.clj:750`, `repair_obligation.clj`) → an ordinary
    ticket and queue entry. The CHECK follows one named finding through to its ticket id.
  - ⟨2⟩4. Lifecycle. An interrupted attempt that is still open can be resumed under the
    same attempt id. A closed attempt is final: `full_loop_cohort.clj:601-602` rejects
    appending to it. A retry starts a new attempt and recognises commits already produced.
    Abandoned work is never counted as accepted, and immutable records are never rewritten.
  - ⟨2⟩5. Timing. The recorded intervals are defined as agent wait (waiting on the
    author's or reviewer's turn), machine time, and overlap, so that wall time is neither
    misattributed nor double-counted. Both figures are reported. This is reporting only,
    with no budget gate.
- **Known failures carried in:**
  - r4-1: `:explanation-invalid` (reviewer template shape; fixed by 6d45e8b7);
  - r4-2: `:guardrail-refusal` (artifact scope);
  - grants 2 and 3: lost to casting.
- **PROGRESS.**
  - ⟨2⟩1 and ⟨2⟩2 done 2026-09-22 in futon2 514d8dca (zai-1; reviewed by claude-5). Every
    preflight finding only prints. Agency being unreachable is the only exit 1. Busy seats or
    a click in flight are waited out (terminal account, exit 3 after 30 min). Misconfigured
    casting exits 3 at once. `--force` is a no-op. Preflight run by claude-5: exit 0 with 13/13
    wires and 10 queued obligations reported.
  - ⟨2⟩3 done 2026-09-22 in futon2 0e185460 (`runs/proof-steps/1-1-2-3-finding-to-ticket.md`).
    The chain is intact on click 3's `repair-occ-444fb018…`: finding file → ticket → queue
    entry. Hops 3–4 were observed live; hops 1–2 were verified in code and converge on the
    same writer. claude-5 spot-checked the files and the cited functions.
  - ⟨2⟩4 done 2026-09-22. All four lifecycle behaviours were already present, with evidence:
    resume an open attempt (`full_loop_cohort.clj:575`), closed is final (`:601-602`), a
    retry gets a new global id (`:416-425`), and prior commits are recognised as base
    (`task_execution_evidence.clj:83-176`). Bad-case tests against the real cohort code were
    added in futon2 f22e407d (3 tests; 33/179 passing). There was no src change.
  - ⟨2⟩5 done 2026-09-22.
    - Timing: `scripts/wm_click_timing.py` (futon2 53906859), read-only. For r4-2, wall 478 s =
      agent wait 346 + machine 133. For click 3, 601 = 463 + 138. Of the machine time, about
      120 s is time-step → selection.
    - Load identity: `scripts/wm_load_identity.sh` (a23dd4db), built on
      `load_identity/report`. After claude-5 reloaded `load-identity`, `eig-shadow`,
      `interpretation-construction` and `run-narrative` from the canonical checkout, the
      readout shows 42/42 click-path namespaces current, 0 unknown.
  - Major-step review (codex-20, 2026-09-22) found three defects, all fixed:
    - a rejected `already-running` launch was tracked as our click (claude-5, 830dafae);
    - load identity omitted undeclared click-path namespaces (zai-1, b442053e: +27, now
      69 listed; 27 show as unregistered, and claude-5 reloaded all 27 from the canonical
      checkouts before the CHECK);
    - timing claimed an author-wait / machine split the records cannot make (zai-1,
      da0bfe5e: mixed interval labelled, totals given as bounds).
  - Note: the futon3c click boundary's grant budget (Joe's renewal documents) is Joe's own
    rationing and is not removed by this step.
- **PROVED 2026-09-22.** CHECK run by claude-5 after codex-20's major-step review.
  - Command: `scripts/wm_click.sh --run` from futon2, with no `--force` and no human
    intervention.
  - Click `wm-click-117f822a-6809-401c-a959-865d21a55f82`, run `2026-09-22-1790060806`,
    started 07:06:46Z. The close
    `data/wm-full-loop-machinery-71/wm-contract-machinery-71-v1/attempt-001/007-closed.edn`
    was newly written at 07:23:07Z.
  - Binding
    `futon3c/data/wm-click-run-bindings/click-run-binding-wm-click-117f822a-….edn` has
    status verified and durability confirmed, and binds the click id, attempt-001 and the run
    id.
  - Outcome `:grounded-change` (whether it counts as accepted is judged at ⟨1⟩6, not here).
  - Load identity: 69 click-path namespaces, 42 current. The 27 unregistered ones were
    reloaded from the canonical checkouts before firing.
  - Timing: wall 923.9 s; agent turn ≤ 768.0 s; machine ≥ 155.9 s.
- **FAILURES:** —

### ⟨1⟩2. The reference input is prepared: a real, eligible decision with at least two alternatives whose declared effects differ.

- **CHECK.** A frozen snapshot in `runs/proof-reference-field/` contains:
  - the task and queue state;
  - the target, taken from the real tasks (the registry via substrate-2, or
    `data/wm-ticket-queue/queue.edn`) and in the eligible queue stratum, not behind an
    admitted front ticket;
  - at least two admissible candidates with different first actions, their identities,
    locators and revisions;
  - the observation and prior evidence, the learning-ledger cutoff, and the model
    configuration.

  It also states, for each candidate, the outcomes it declares it will produce, and the
  difference between them that the discrimination in ⟨1⟩3 will rely on. It lists any past
  outcome evidence, compatible in meaning, that exists for those action families; none is
  required.

  The snapshot freezes *inputs*, not expected answers. Evaluating it read-only never
  executes its work. It is executed once, at ⟨1⟩5.
- **PROOF.**
  - ⟨2⟩1. Interpretation. `mission_hole_wants.clj:74-91` leaves `:interpretation` and
    `:candidates` empty. An offline Codex seat writes the interpretation before any click,
    reading the task against its patterns, the observed guards and the outcomes they
    produce. "Not declared by hand" means the candidate is derived from that reading, and
    the derivation is recorded. The author's seat alone does not establish it.
  - ⟨2⟩2. Publication. The candidates are published through the loader selection actually
    reads (`cascade_sources.clj` `load-declared`; `check-file!` only validates shape). The
    CHECK shows selection reading them.
  - ⟨2⟩3. Feasibility. Each candidate's acceptance can be met in the repositories and with
    the evidence available. This is a property of the candidates, not a new gate. Examples to
    avoid: r4-2 needed mathlib4; click 3's candidate needed held-out evidence that does not
    exist.
  - ⟨2⟩4. Evidence for B. Amended by Joe, 2026-09-22: "if we use data when it exists and
    the prior otherwise, we will get unstuck on the first round and never have to look
    back." Past outcomes are used where they exist. Where none exist, B is the declared
    prior: each candidate's declared effects. Differences between candidates come from
    their declared effects, never from tuning.
- **Known failures carried in:**
  - r4-2: 90 proposals, 0 admission joins;
  - click 3: three candidates, all from routes declared by hand;
  - fix-5: 441 wants yielded none.
- **PROGRESS.** Target survey by zai-1 (futon2 506790e4,
  `runs/proof-steps/1-2-reference-target.md`) recommends the front repair ticket
  T-repair-occ-444fb018…: it is eligible now, has two different first actions, and each is
  feasible in one repository.
  - ⟨2⟩1 done 2026-09-22 in futon2 0aacc7ec
    (`runs/proof-steps/1-2-cascade-source-T-repair-occ-444fb018.edn`, zai-1). Two candidates
    are read from the ticket's own two routes, and `check-file!` passes.
    - A: dated recheck → `:repair/verified-dated-recheck`.
    - B: preregistered held-out split → `:repair/restored-held-out-split`.
  - ⟨2⟩2 done 2026-09-22 in futon2 22b1971c. The source was published to
    `resources/wm/cascade-sources/T-repair-occ-444fb018.edn`. The real `load-declared` and
    `constructed-candidates` admit both candidates (C1 recheck, C2 held-out split). The task
    locator resolves at `HEAD`. `cascade-sources-test` passes 8/33. claude-5 reloaded
    `cascade-sources` and `cascade-proposals` from the canonical checkout.
  - ⟨2⟩3 done 2026-09-22 in futon2 80608d0b. Both candidates are feasible in futon2 alone.
    zai-1 found and fixed a defect in its own source: both produced-token locators pointed
    into gitignored paths, so they could never be observed. They were relocated and
    whitelisted.
- **PROVED 2026-09-22.** The reference input is frozen in `runs/proof-reference-field/`
  (futon2 53fea9f4). It holds inputs only, no expected winner.
  - B₀: futon2 80608d0b, futon3c 826a9a59.
  - The target is the front repair ticket, from the real queue, and is the eligible stratum.
  - Two admitted candidates, with different first actions and different declared effects.
  - Locators resolve, and acceptance can be met in one repository.
  - claude-5 reloaded `cascade-sources` and `cascade-proposals` after 80608d0b.
- **FAILURES:**
  - 2026-09-22, ⟨2⟩1 (zai-1). `finding_ticket/publish!` writes tickets to disk but never
    commits them, so a locator at `HEAD` cannot resolve the ticket. Fixed for this ticket
    (claude-5, ae69f5e7). Proposed fix (zai-1): `publish!` commits the ticket file by explicit path
    inside its store lock (`finding_ticket.clj:77`). Implemented by codex-12 in futon2 021024cf
    (ticket-only pathspec commit inside the store lock; a held `index.lock` gives a typed
    failure, with the ticket and queue entry kept; tested on real git, 8/60). claude-5
    reloaded `finding-ticket`.
  - 2026-09-22, ⟨2⟩4 (claude-5). No recorded outcome evidence can tell the reference
    candidates apart.
    - `attempts.edn` holds one trial (a different pattern, success 1), and there are none for
      the repair family.
    - Of the 46 closes under `data/wm-full-loop-*`, only one records which pattern was
      acted on.
    - The machine has not run successfully often enough to have an outcome history per
      action.
    - Under execution rule 2 the proof stops here, and Joe decides.
    - Joe decided (2026-09-22): option (a), use data where it exists and the declared prior
      otherwise. ⟨2⟩4 and ⟨1⟩3 ⟨2⟩3 are amended accordingly. The proof resumes at ⟨1⟩2.


### ⟨1⟩3. On the reference input, evaluated read-only, the full selection law chooses a unique action, and G changes that choice.

- **CHECK.**
  - The production scorer computes, for every eligible policy after the ticket-front
    restriction:
    - E, F (absent is recorded as absent, not as 0; see `cascade_selection.clj:95-116`), γ,
      and G with all of its terms at the declared precision and horizon;
    - the action marginal `ActionMarginal`, which sums the policies sharing a first action
      (`cascade_selection.clj:132-163`).
  - The action marginal has a unique maximum. The numeric comparison is stated. The
    recorded `:tie-break-rule` field is not used as evidence either way.
  - The action chosen under the full law differs from the action chosen under `log E − F`
    alone at the same γ, both computed on the summed action marginals. This counterfactual is
    the whole evidence that G decided. It is a requirement on the demonstration input, not a
    runtime rule: an ordinary click where G reinforces habit is lawful.
  - All inputs and outputs are recorded against the ⟨1⟩2 snapshot.
- **PROOF.** Each sub-step produces an input that the next sub-step consumes, and each is
  checked by recomputing it from the pinned inputs.
  - ⟨2⟩1. q₀. Name the observation likelihood A, the initial and carried belief, the source
    observations and the carrier mappings. The contracted functions have different carriers:
    `observation.clj:103` gives aggregate channels, `belief.clj:510` reconciles entity
    beliefs, and the exact token filter is in `cascade_model_manifest.clj`. The CHECK shows
    that this q₀ is the one the rollout and the scorer consume. Absent observations stay
    absent; they are never rendered as zero.
  - ⟨2⟩2. The prospective ending kernel and C.
    - C over the run-ending classes (focus / associated / elsewhere / known failure) is Joe's
      55/35/5/5, stipulated and held fixed.
    - The kernel maps a policy's predicted observations to a normalised distribution over
      those classes. It reuses the class definitions and pure logic from
      `run_ending_classification.clj`, but it is distinct from the close classifier, which
      needs an actual close, attestation and focus relation (`:69-113`). Predictions are
      never passed off as attestations.
    - Unknown probability mass has a stated treatment. It is never silently dropped and
      renormalised, because that makes the least-observed action look best.
    - The step states which carrier risk is evaluated on (token or class), and which
      contracted function computes it. Aggregation changes KL, so the class carrier is not
      assumed to equal the token-space G.
    - Ambiguity (A) and horizon placement are made consistent with that choice.
    - The output is an input the production horizon scorer consumes, without changing this
      sub-step later.
  - ⟨2⟩3. B from the declared prior, updated by recorded outcomes where they exist (Joe,
    2026-09-22). With no outcomes for a family, B for that family is its declared prior; the
    first accepted outcome at ⟨1⟩8 starts the update.
    - A new version of `resources/wm/attempt-learning-contract.edn` authorises production
      consumption. The same change updates the `supported-contract?` pin in
      `attempt_learning.clj:15-21`, which checks the mode and version, so the code accepts
      exactly the new version, and the check is not loosened. Joe's
      signature on this plan is that authorisation.
    - The trial events in `data/wm-learning-trials/attempts.edn` stay immutable. The new
      consumer contract states how old compatible events are interpreted, without
      replacement events that would count the same trial twice.
    - A production reader is built; `learning_trial_ledger.clj` has none today.
    - The step states which transition a whole-attempt outcome informs.
    - The step establishes persistence, deduplication, and continuity of meaning and domain
      (`:carry-domain-changed`: migrate, or reinitialise with a record; the identity check
      stays). ⟨1⟩8 depends on this.
  - ⟨2⟩4. The full score is computed as described in the CHECK.
  - If lawfully derived terms do not discriminate on this input, the proof stops under
    execution rule 2. Valid preferences and evidence are never tuned.
- **Known failures carried in:**
  - r4-1: G 9.70476 vs 9.70602, decided by habit;
  - click 3: G identical at 9.704307, decided by name order.
- **PROGRESS.**
  - ⟨2⟩1 q₀ discovery done 2026-09-22 in futon2 a7fc369e (`runs/proof-steps/1-3-2-1-q0.md`,
    zai-1).
    - q₀ is a point mass on `[T-repair-occ-444fb018 :admission/task-stated]`. Both
      produced tokens are observed false.
    - A is the identity.
    - Rollout and scoring read the same `:cascade-belief` slot.
  - ⟨2⟩1 settled 2026-09-22 (futon2 4ef0f7b6). The joint cascade decision qualifies tokens
    as `[target token]` at `scripts/futon2/report/war_machine.clj:6145-6168`. Through that
    qualifier, C1's one-step belief adds `[T :repair/verified-dated-recheck]` and C2's adds
    `[T :repair/restored-held-out-split]`, so the predictions differ. Paths that bypass the
    joint selector stay bare, and those are not the live path.
- **FAILURES:** —

### ⟨1⟩4. The producers the close and the update need exist before the live click.

- **CHECK.**
  - Post-build measurement, the run-ending attestation, the focus relation and the
    accepted-increment predicate are implemented. The predicate is the target's existing
    acceptance, met for this occurrence, with the reviewed commits bound to it.
  - The runner calls them *before* constructing the close, where it already assembles
    observations (`full_loop_runner.clj:3697-3755`).
  - Run read-only on the real r4-1 and r4-2 closes, they reproduce each recorded observation
    as it is: measured true, measured false, or `:missing` (r4-1's failed close contains both
    true and false measurements). They never invent an attestation.
  - The B update is written only after a close is accepted, keyed by occurrence id, so a
    failed or interrupted close writes none. Persistence and consumption are shown live at
    ⟨1⟩8.
- **FAILURES:** —

### ⟨1⟩5. One live click on the reference target is decided by G.

- **CHECK.** A click fired by ⟨1⟩1's command runs on the live queue, as it stands. Its own
  receipts show, on the live input:
  - a unique maximum of the eligible action marginal;
  - that the full law's action differs from the action under `log E − F` alone at the same γ,
    on the summed action marginals;
  - the functions from ⟨1⟩3 doing the computing, with the ⟨1⟩1 load identity holding.

  There is no comparison with the ⟨1⟩2 snapshot. That snapshot was only the working material
  for developing ⟨1⟩3 read-only.
- **How the click reaches the reference target.** The target is entered as an ordinary
  ticket in the queue, the normal way work is requested, and ordinary selection picks it up.
  It is never pinned or steered around the selector.
- **If the live input does not discriminate** (for example, the queue has changed), that is
  recorded under FAILURES as an ordinary click. The step then waits for a later click whose
  own receipts meet the check. Nothing is pinned or tuned to force one.
- **FAILURES:** —

### ⟨1⟩6. The chosen work reaches an accepted close.

- **CHECK.** For this occurrence, the accepted-increment predicate from ⟨1⟩4 holds, bound to
  the reviewed commits and the required evidence. `verify-close` alone is not enough; r4-2
  passed it on a failure.
- **FAILURES:** —

### ⟨1⟩7. The outcome is measured and attested.

- **CHECK.**
  - The close records the wanted tokens as measured after the build, true or false and not
    `:missing`.
  - It records the attested run-ending class.
  - The classifier, rerun on the actual close, attestation and focus inputs (not only the
    projection `verify-close` compares), agrees with the recorded class.
- **FAILURES:** —

### ⟨1⟩8. The outcome updates B once, and the next selection consumes it.

- **CHECK.**
  - The named B parameter is updated exactly once and survives a reload.
  - Recomputing ⟨1⟩3 on the frozen non-B inputs, with and without the update, changes the
    predictions as the update rule states.
  - The next live selection's receipt shows the updated parameter's identity and value
    being consumed, on whatever compatible task it runs.
  - The reference task is not replayed as new work, and its outcome is not counted twice.
- **FAILURES:** —

### ⟨1⟩9. Q.E.D.

- ⟨1⟩2 and ⟨1⟩5 give a choice among real alternatives, made on the live input.
- ⟨1⟩3 and ⟨1⟩5 show that the full law chose the action and that G changed the choice. There, C is
  Joe's stipulated preference and B is inferred from recorded outcomes.
- ⟨1⟩4, ⟨1⟩6 and ⟨1⟩7 give an accepted close with a measured, attested outcome.
- ⟨1⟩8 gives the update, consumed by the next selection.

## Sign-off (a row is cleared whenever its step changes; only the reviewer fills its own column) (revision 4: rows ⟨1⟩1–⟨1⟩5 need fresh review; ⟨1⟩6–⟨1⟩8 unchanged since revision 3)

| Step | Zai GLM | Codex | Joe |
|---|---|---|---|
| Execution rules | SIGN | SIGN | |
| THEOREM / ASSUME | SIGN | SIGN | |
| ⟨1⟩1 | SIGN | SIGN | |
| ⟨1⟩2 | SIGN | SIGN | |
| ⟨1⟩3 | SIGN | SIGN | |
| ⟨1⟩4 | SIGN | SIGN | |
| ⟨1⟩5 | SIGN | SIGN | |
| ⟨1⟩6 | SIGN | SIGN | |
| ⟨1⟩7 | SIGN | SIGN | |
| ⟨1⟩8 | SIGN | SIGN | |

The review sections below refer to earlier revisions (1–2) and their step numbering.

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

## Zai GLM review (rev 3)

**Reviewer model:** Zai GLM (zai-1), GLM-5.x. Full adversarial pass over every
row of a2b24c38, standard as in 580dd866/f4ab52a7: each CHECK attacked before
signing; every new file:line claim opened read-only.

New claims verified: `load_identity.clj` digest-vs-canonical comparison
(:63-108); `cascade_selection.clj:95-116` is the NaN/absent-term pathology
comment (absent is not 0 — real) and `:132-163` the action marginal with its
tie-break rule; `tripwire.clj:750 record-finding!` → repair-obligation chain;
`full_loop_cohort.clj:601-602` throws on appending to a closed attempt;
`resources/wm/attempt-learning-contract.edn` exists and `attempt_learning.clj:15-21`
pins `:mode :record-only` in `supported-contract?`; `full_loop_runner.clj:3697-3755`
obtains D-task observations, route attestation and kernel receipt into the
close; `wm_click.sh:111-233` is the firing-refusal block.

### Execution rules — SIGN

Attack: rule 1 ("repairs happen inside sub-steps ⟨2⟩ of the step that needs
them") could hide unbounded scope creep — a sub-step that rebuilds half the
machine inside ⟨1⟩3. Survives: each sub-step is checked by recomputation from
pinned inputs, and rule 5 keeps the proof's order from ever becoming a rule
against running the machine — the red-tape failure mode Joe named is closed in
both directions. Rule 2's "never adjust preferences, evidence or parameters to
force a check to pass" is the anti-Goodhart line this plan needed from the
start.

### THEOREM / ASSUME — SIGN

Attack: the theorem is now scoped as "a demonstration on one decision input" —
weaker than rev 2's implied generality. That is not a defect but honesty: the
old framing overclaimed. A1's new admission (the bundle checks correspondence,
not composition) correctly puts composition inside the proof. Counts (16/36)
re-verified in my earlier passes.

### ⟨1⟩1 — SIGN, one bias guard owed

Attacks: (a) the target could be hand-picked easy — survives, the target must
come from the registry/queue in the eligible stratum and candidates must carry
recorded derivations, with the author's seat not establishing them alone;
(b) ⟨2⟩4's honesty rule (no compatible evidence → different target or stop,
differences never manufactured) closes the manufacturing loophole I would have
used. Owed: the snapshot names "the observable outcome differences that ⟨1⟩3
will rely on" — state explicitly that it names *evidence*, never an expected
chosen action, so construction cannot be steered toward a predetermined winner
(Missing 2).

### ⟨1⟩2 — SIGN, one freeze-integrity note

Attacks: (a) firing on a *non-reference* target could consume reference work —
prevented by construction; (b) ⟨2⟩1 removes the firing-refusal block — that is
Joe's quoted ruling (tripwires never decide firing), not new license; (c) the
finding→ticket chain (⟨2⟩3) is followed to a ticket id, so my coupling concern
from 580dd866 is now carried in and checkable. Lifecycle (⟨2⟩4) and the
three-way timing definition (⟨2⟩5: agent wait / machine / overlap, no budget
gate) answer my earlier split correctly. Note (Missing 4): the ⟨1⟩2 click may
itself produce a learning-trial event; the reader built in ⟨1⟩3 ⟨2⟩3 must
enforce the ⟨1⟩1 ledger cutoff, or B silently includes post-freeze evidence
and the "frozen inputs" claim breaks.

### ⟨1⟩3 — SIGN

This is the step the whole plan turns on, so three attacks:
(a) *Could the G-matters check pass vacuously?* The check requires the full-law
choice to differ from the `log E − F`-alone choice (or G's contribution to
exceed that margin) — a genuine counterfactual, and codex-20's numerical
counterexample against rev 2 cannot recur because C, B and the score are all
sub-steps of the one step, checked against pinned inputs.
(b) *Absent-as-zero:* the plan pins `cascade_selection.clj:95-116`, the exact
NaN pathology; absent is recorded absent. Verified real.
(c) *Unknown mass renormalisation* (my old "least-observed looks best" attack)
is explicitly forbidden, and the class-carrier vs token-carrier KL distinction
is named. The tie-break is excluded as evidence. Survived everything I had.

### ⟨1⟩4 — SIGN

Attack: the producers are tested on "an existing real close" — does one exist
with usable properties? Yes: r4-2's close is real, retained and typed, and
click 1's grounded close carries real after-build measurement. Running the four
producers read-only there, before any live click, is the right order (it fixes
the r4-1 failure mode of discovering shape defects inside a close). Verified
the runner already assembles these inputs at `full_loop_runner.clj:3697-3755`.

### ⟨1⟩5 — SIGN, one brittleness owed

Attack: between read-only ⟨1⟩3 and live ⟨1⟩5 the queue can change — a new
candidate entering would make live scores differ from ⟨1⟩3's, and the check
("equal ⟨1⟩3's") would fail for an environmental reason, not a machine defect.
The plan refuses to pin the target (correctly), so this can happen. Owed
(Missing 1): allow the check to pass when the live input differs only by
additions, with the recomputed choice shown and G again changing it — or state
that a changed input is logged at ⟨1⟩5 and the reference re-frozen only at
Joe's word. Without that, an innocent queue drift stops the proof.

### ⟨1⟩6 — SIGN

`verify-close` alone is explicitly not enough, with r4-2 named as the case
that passed it on a failure (verified in its STAGES: verify-close true, close
`:known-typed-failure`). The accepted-increment predicate is bound to reviewed
commits and existing acceptance, "no additional gates". Survived.

### ⟨1⟩7 — SIGN

The classifier is rerun on the actual close, attestation and focus inputs —
not only the projection `verify-close` compares. That closes the
label-as-evidence hole. Measured true/false, never `:missing`. Survived.

### ⟨1⟩8 — SIGN

Attack: "the next live selection... on whatever compatible task it runs" could
consume the update on a domain-changed task. Survives: ⟨1⟩3 ⟨2⟩3 establishes
the carry rules, and the with/without recompute on frozen non-B inputs is the
discriminating check; no replay of the reference task as new work; exactly-once
with reload survival. Survived.

### Red tape audit

Nothing in rev 3 adds a check, gate or sign-off Joe did not ask for: firing
never refuses, feasibility is a property, timing is reporting without a budget
gate, A3/A4 are gone per Joe, and rule 5 positively forbids using the proof's
order against running the machine. Clean.

### Missing (rev 3)

1. **⟨1⟩5 queue-drift brittleness:** allow the identity-match check to tolerate
   additive input changes with the recomputed choice shown, or route a changed
   input to Joe for re-freezing — never a silent stop.
2. **⟨1⟩1 bias guard:** say the snapshot names evidence, never an expected
   chosen action.
3. **`attempt_learning.clj:15-21` pins `:mode :record-only`** in
   `supported-contract?` — the new contract version must update this pin in
   the same change, named in ⟨1⟩3 ⟨2⟩3, or the code rejects the new contract
   (or the check gets loosened without one).
4. **⟨1⟩2's click outcome vs the ⟨1⟩1 cutoff:** the production reader must
   enforce the frozen ledger cutoff, so the non-reference click's own trial
   event cannot enter B before ⟨1⟩3.

Verdict: **9 SIGN, 0 OBJECT.** Rev 3 is structurally the strongest version:
repairs live inside steps, the G-matters check is a real counterfactual, every
carried-in failure mode from clicks 1–3 is named at the step it would bite,
and the red-tape failure mode is closed by rule 5. The four Missing items are
wording and enforcement notes. This review authorizes no machine changes.


## Codex review (rev 3)

**Reviewer: codex-20; model: GPT-6 (gpt-6-astra).**

**Verdict: 6 SIGN, 4 OBJECT on the 10 revision-3 table entries.** Reviewed
`b0314ca8` (restructure `a2b24c38`, Zai review `733dbd31`, and its applied
amendments). The four objections are ⟨1⟩2–⟨1⟩5. This is a substantially
better dependency structure, but the current CHECKs still permit failures of
its claimed implications. No machine changes, clicks, reloads, tests or
learning writes were made for this review. I re-read the relevant selection,
queue, checkpoint and learning source and the actual r4-1/r4-2 close records;
local arithmetic was used to check the counterexamples.

### Disposition of my earlier objections

| Earlier finding (289e98e2 / ec4e3701) | Revision-3 disposition |
|---|---|
| Field first; distinguish replay from execution | **Mostly answered.** Inputs now precede evaluation and the reference work executes once. New step 2's non-reference execution is not guaranteed to be selectable; see below. |
| Bundle correspondence is not composition; C is stipulated; demonstration scope | **Answered.** A1 and the theorem say this explicitly. |
| Pause/keep-revert and named tripwire blockers | **Withdrawn, remain withdrawn.** Joe's ruling is incorporated. No further decision or sign-off is requested on them. |
| Dangling Part C authority | **Operationally answered.** Management rules are quoted directly as authority. The introductory reference to a nonexistent Part C remains an editorial cleanup only, not an objection. |
| Command/click/attempt/new-close identity | **Answered in the specification.** Step 2 requires the identity join. |
| Open interruption versus final closed attempt | **Answered in the specification.** Resume applies only to open attempts; retries receive a new id and recognize existing commits. |
| Wait/compute/overlap accounting | **Answered in the specification.** Named intervals and no time-budget gate. |
| Finding-to-ticket coupling | **Answered in the CHECK.** One named finding must be followed through publication and queue identity; merely printing a trip no longer suffices. |
| Offline interpretation author, loader, derivation, feasibility and eligible target | **Answered.** Step 1 names the author, actual loader, substantive derivation and queue stratum; feasibility stays a property, not a new gate. |
| Compatible historical outcome evidence and no manufactured differences | **Answered.** Step 1 identifies evidence and permits an honest inability to proceed. It does not promise lawful data must discriminate. |
| Aggregate/entity/token carriers and consumed q0 | **Answered as implementation work inside step 3.** Recompute from pinned inputs and show the rollout/scorer consumes that q0. |
| Prospective predictions versus actual attestations; unknown mass; risk carrier | **Answered as implementation work inside step 3.** These definitions must exist before its top-level CHECK can pass. No fabricated close attestations are authorized. |
| Prior/learned-B ordering and insufficient expected-log-C inequality | **Answered structurally.** Scoring and its inputs now belong to one step and the invalid inequality is gone. A different loophole remains in its alternative margin condition. |
| Immutable trials, versioned consumer/pin, cutoff, parameter placement, persistence/carry | **Answered as named work in step 3.** Old trial events remain immutable, the pin changes with the new contract, and the frozen cutoff excludes later events. |
| F confounding, action marginal and tie-rule field | **Mostly answered.** The principal counterfactual uses log E − F, policies are summed by action, and unique maximum replaces absence of the tie-rule field. The alternative CHECK is weaker than the principal one. |
| Loaded-source evidence, missing coverage | **Answered as specified evidence.** load-identity replaces invented reflection/reload metadata. Unknown coverage cannot count as a matching captured digest. Its existing source-sampling limits remain. |
| Producers available before execution; accepted increment distinct from verify-close | **Answered in placement and predicate.** Step 4 now owns the producers. Its read-only/persistence CHECK and update-before-close ordering still need definition. |
| Actual classifier recomputation and measured false versus missing | **Answered.** Step 7 retains the substantive CHECK I signed. |
| Exactly-once update, frozen non-B comparison, next actual consumption | **Answered.** Step 8 retains the substantive CHECK I signed. |
| Live/reference correspondence without forced selection | **Partly answered.** Drift is explicit, but the new drift branch must preserve all of step 3's claims, not just matching the recomputed winner. |

These dispositions accept the plan's specified work; they do not assert it is
already implemented. The former numerical counterexample is no longer an
objection to the new full-law computation. The attacks below concern the
remaining logical and execution gaps.

### Execution rules — SIGN

**Attack:** a failed proof CHECK could become an excuse to prevent an ordered
click, or repairs inside sub-steps could conceal changing the data until the
CHECK passes. Rules 2 and 5 explicitly forbid both. Component work belongs to
the step that needs it, and an inability to establish the claim is reported
rather than cured by tuning evidence. I find no new per-step approval chain
or production veto in these rules. The four amendments requested below are
definitions of existing CHECKs, not additional run permissions.

### THEOREM / ASSUME — SIGN

**Attack:** infer universal performance or automatic candidate supply from
one good run, or claim C was learned merely because B was. The theorem now
bounds the demonstration and distinguishes stipulated C, inferred B and
composition from component correspondence. Those objections are answered.
A1/A2 are assumptions to instantiate, not a claim that implementation is
already correct. The weaker wording “G's contribution ... is shown” is
reasonable, but the steps/Q.E.D. currently claim the stronger “G changed the
choice”; choose one meaning consistently as discussed under step 3.

### ⟨1⟩1 — SIGN

**Attack:** supply different action names without compatible historical data,
or an infeasible task behind the real front target. The CHECK now names
meaning-compatible outcome evidence, actual eligibility, usable acceptance,
observations, revisions and the cutoff. Derivations come from the task and
patterns; the expected winner is not frozen. The selection loader, rather
than a private shape validator, supplies the candidates. This establishes
usable *inputs* for step 3, which is allowed to report that lawful data do not
prove discrimination. It does not wrongly claim that differing evidence
necessarily guarantees a different chosen action.

### ⟨1⟩2 — OBJECT

**Attack:** let the reference target prepared in step 1 be the first admitted
front ticket. `ticket_queue.clj:89–115` gives it the eligible stratum and
`policy.clj:358–374` chooses inside that stratum. The ordinary command can
therefore select the reference target, while this CHECK forbids consuming it
and requires a *different* target. Step 1 has passed; step 2 cannot obtain
its required witness without changing the queue, suppressing a candidate,
pinning a different target, or changing the reference setup. None is
licensed by “use the ordinary command.” This is an order problem, not a
reason to add a firing gate.

**Required amendment:** give the lifecycle CHECK a witness it can lawfully
obtain. For example, perform the non-reference lifecycle execution before the
reference decision is fixed, or use an already-retained qualifying invocation
for the same implementation and leave fresh end-to-end execution to step 5.
If another approach is intended, state how the ordinary selector supplies the
non-reference target without altering the rules. Do not require an unrelated
click merely to satisfy ceremony when retained evidence answers the claim.

The newly specified identity, loading, finding/ticket, lifecycle and timing
properties are otherwise substantially improved. “Nothing refuses firing”
and “never launches into an occupied seat / second active click” should be
implemented as command acceptance plus an explicit runtime wait/terminal
account, not restoration of a preflight busy-seat veto. Joe's firing ruling
is accepted; this is a request to make the lifecycle behavior concrete.

### ⟨1⟩3 — OBJECT

**Attack:** the first branch proves a changed action, but its “Alternatively”
branch does not. With two policies/first actions, F = 0, γ = 1 and
E = [0.6, 0.4], the base margin favors action 1 by log(1.5) = 0.405465.
Under the stipulated C, identity A and one step, take class predictions
Q1 = [1,0,0,0] and Q2 = [0.5,0,0,0.5]. Then:

```text
G1 = KL(Q1 || C) = 0.597837001
G2 = KL(Q2 || C) = 1.103637457
G contribution to action 1's log margin = 0.505800456
```

G's margin exceeds the base margin, so the alternative passes. Yet both the
base and full law choose action 1. The heading and Q.E.D. assertion that G
*changes* the choice have not been established. This is a reinforcing
contribution, not an action reversal. With several policies per action,
margin decomposition must moreover use the summed action masses: generally
`log(sum(exp(score)))` cannot be decomposed into one winning policy's G.

**Required amendment:** either retain the actual action-change branch as the
CHECK for the stated strong claim, or explicitly accept reinforcing/dominant
contribution as the intended theorem and adjust the heading and Q.E.D. to
match. Define any alternative margin on the actual winning action versus its
competitor using full and base action marginals, with direction and sign,
not an unsigned policy-level difference. Do not demand altered parameters
just because real G reinforces a good habit choice.

The q0, prospective class kernel, unknown-mass treatment, scored carrier,
versioned learning consumer, cutoff and full production score are now
appropriately owned inside this step. They can be repaired before its CHECK
passes. The exact principal counterfactual (base versus full action) addresses
my old F-confounding objection. The remaining objection is the weaker
alternative and the claim made from it, not a request to re-prove Lean.

### ⟨1⟩4 — OBJECT

**Attack 1 — the CHECK's effects contradict its wording.** “Run on an existing
real close, read-only” can recompute measurements and a proposed update, but
cannot by itself show a new production B update “applied exactly once and
persisted.” `learning_trial_ledger/record!` writes and syncs a durable file;
that is not a read-only operation. Name what remains read-only (the source
close, artifacts and live stores), and where the requested persistence witness
comes from: an already-recorded matching update or an explicitly isolated
replay destination. A pure computed delta must not be called persisted.
This clarifies the existing CHECK, rather than adds another test gate.

**Attack 2 — an arbitrary real close can prove only the failure branch.** The
examples cited by Zai do not demonstrate accepted increment production:
r4-1's actual `...machinery-69/.../attempt-002/007-closed.edn` is
`:build-failed`, `:grounded? false`; r4-2's `...machinery-70/.../attempt-001/007-closed.edn`
is `:guardrail-refusal`, class `:known-typed-failure`, with a held
`:observation-missing` trial. They are useful real inputs, but returning
false/missing on them correctly does not exercise production of an accepted
attestation or a usable learned update. Name the retained source records and
the independently expected outputs for the positive path needed at steps
6–8. Do not relabel a failed historical close or manufacture its attestation
to obtain that witness. If no usable record exists, state the remaining
producer-validation work here instead of claiming the CHECK already has one.

**Attack 3 — durable update before final close.** “The runner calls all four
before constructing the close” includes committing the B update. But the
manifest/close validation can still fail after the measured trial is made:
`full_loop_runner.clj:3697–3836` constructs observations/retained records,
then the manifest and close; `full_loop_cohort.clj:630–652` can reject that
close. An interruption can also occur between durable update and close.

**Required amendment:** distinguish constructing an update from making it
consumable, and specify the authority for that consumption. If the trial
means accepted delivery, commit/consume it from the final durable judgment
with retry/recovery keyed to the same occurrence. If it means an independently
measured effect regardless of close acceptance, say that explicitly in the
step-3 parameter contract and preserve the final close outcome separately.
In either case, specify how recovery joins the update and close without
rewriting history or counting twice. The existing record-only ledger's
position in the runner is not proof that the same timing is correct for a
production model mutation. Otherwise step 4 passes an offline example while
step 8 fails on the real close/reload boundary and needs the producer rebuilt.

### ⟨1⟩5 — OBJECT

**Attack:** additive drift preserves both original reference candidates and
the eligible target, but adds a third action that dominates both the base and
full score. Step 3's old field may have reversed the base winner; recomputing
on the larger field now selects the added action with *or without* G. The new
CHECK asks only that the live action equal the recomputed action. It can pass
while the asserted G effect no longer holds. Zai's Missing item 1 explicitly
said “G again changing it”; that part was lost in the amendment.

A concrete score example (base scores are log E − F up to a common offset):

```text
old base [0, 0.2],     old G [0, 1]   -> base action 2, full action 1
new base [0, 0.2, 10], new G [0, 1, 0] -> base action 3, full action 3
```

All actions can be in the same target stratum and all E masses positive after
normalization. No reference candidate was removed, so the explicit re-freeze
condition does not catch it. Also, “these equal step 3's” and permission to
use changed inputs need one authoritative comparison record, not two
incompatible baselines.

**Required amendment:** for any allowed drift, evaluate *all* applicable
step-3 CHECK predicates on the input actually consumed: complete model terms,
unique eligible action maximum, and whichever G-contribution claim the
revised theorem adopts. Retain the new input identity and its relation to the
original snapshot as the live proof instance. If the check fails, record the
proof failure at step 5; the click is not blocked. Never alter the selector
or suppress fresh evidence to preserve the old answer.

This input identity includes the actual ledger frontier and model state, not
just candidate additions. Step 3 rightly honors the old cutoff during replay;
ordinary live selection may legitimately consume later events. Record and
recompute the actual consumed snapshot rather than pretending it used the old
cutoff or forcing production to ignore newer learned data.

### ⟨1⟩6 — SIGN

**Attack:** `verify-close` can return true for failure or a commit from another
attempt. The new CHECK requires the target's accepted-increment predicate,
required evidence and reviewed commits bound to this occurrence. That is the
right acceptance property, conditional on step 4 establishing the predicate
and its producers. No new acceptance gate is authorized by this SIGN.

### ⟨1⟩7 — SIGN

**Attack:** record the classifier's label without measurements or trust only
the saved projection. The CHECK requires post-build true/false observations
and recomputation using the actual attestation and focus inputs. It retains
the acceptance property I previously signed. The outputs must belong to the
same occurrence/revisions as step 6, not a convenient historical example.

### ⟨1⟩8 — SIGN

**Attack:** update metadata changes but actual predictions do not, or the next
task's completion merely changes the selected action. The frozen non-B
comparison and actual consumed parameter identity/value distinguish these
cases; the reference task is not executed twice. Exactly-once and reload
survival are explicit. This CHECK remains sound; step 4 must supply a defined
close/update persistence boundary so it is achievable without a late redesign.

### ⟨1⟩9 — Q.E.D. not yet established

No separate approval row or gate is requested for this conclusion. The present
step-3 alternative does not prove that G changed the action, and drift can
remove even the property demonstrated on the original field. Step 4 does not
yet specify a runnable persistence witness and final-close/update relation.
Resolve those implications before claiming the conjunction. Steps 6–8 are
otherwise suitable inspections of the single execution's results.

### Missing / minimal amendments

1. **A lawful lifecycle witness before consuming the reference work.** Resolve
   step 2's non-reference-target requirement under the ordinary queue; do not
   introduce a selection override or a new firing refusal to satisfy it.
2. **One precise G-contribution claim.** Remove the non-equivalent alternative,
   or state that dominant reinforcement is sufficient and align the theorem,
   action-marginal calculation and Q.E.D. with that weaker claim.
3. **A concrete step-4 witness and persistence boundary.** Identify source
   records, expected positive producer outputs, the read-only versus durable
   operations, and recovery/consumption semantics relative to the final close.
   Existing failed closes do not become accepted by replaying them.
4. **Full revalidation of the proof claim on actual drifted inputs.** Rerun
   the existing step-3 predicates, retain the consumed input/ledger identity,
   and report a failed proof without blocking the click.

No pause/keep-revert issue, tripwire firing authority question, extra
per-step sign-off, speed budget, admission screen, or new negative-control
suite is being reintroduced. The revision resolves most earlier objections;
these four concern obtaining the promised witness and what it actually proves.


### Codex revision-3 update after Joe's removal of snapshot matching

**Reviewer: codex-20; model: GPT-6 (gpt-6-astra).** Re-read the operative plan
at `04b3095e`, after my review `0628f87d`. This update supersedes the earlier
revision-3 findings wherever stated below. **The table remains 6 SIGN / 4
OBJECT**, but the drift objection is withdrawn and the other objections are
narrowed. No machine changes were made.

**What I withdraw, including requirements I introduced:**

- **Live/development snapshot matching, re-freezing, a drift decision and a
  cutoff enforced on live learning:** no longer needed. The live CHECK now
  proves the selection property directly on the actual input. My demand for
  full revalidation *as a drift procedure* is withdrawn; the new live CHECK
  already asks for the substantive properties. No changed-input ceremony is
  owed. Recording which data an offline computation used can explain that
  computation, but cannot require the live machine to use old data.
- **Protecting the development target from being executed early:** no longer
  needed to preserve a later comparison. My suggested reordering or special
  historical lifecycle witness solely to avoid consuming it is withdrawn.
  Remove the step-2 “target other than the reference target” restriction
  rather than build machinery to meet it. The step-1 “executed once at step 5”
  and step-5 “on the reference target” wording likewise need not reserve or
  force that target. Let the ordinary queue select real work. Real repeat
  execution of already-completed work remains a correctness issue; naming a
  special protected development target is not its solution.
- **A successful historical close before permitting the live attempt:** I
  overconstrained this in the step-4 review. Existing failure records can
  exercise useful producer behavior, and the positive accepted-close and
  measured-update claims will be demonstrated directly at steps 6–8. Do not
  create or hunt for an earlier successful close merely to unlock that
  demonstration. A failure replay must still be described as a failure
  replay, not as evidence that an accepted increment was produced.
- **A separate isolated persistence rehearsal merely to make step 4 pass:**
  not required. It was one way to interpret the contradictory read-only/write
  wording, not a necessary new test. Step 4 can compute the proposed update
  and establish the producer wiring; step 8 witnesses actual persistence and
  reload survival on the live outcome. No extra negative-control suite or
  sign-off is being requested.
- **The “no expected winner” sentence as an extra governance rule:** not
  required. Truthful derivation from task and outcome evidence, already in the
  plan, is the substantive requirement. Removing that sentence needs no
  replacement permission or bias-screening process.

**Current objections and the actual need each serves:**

- **⟨1⟩2 — OBJECT, remove an unnecessary restriction.** The ordinary queue
  can choose the development target, so “a different target” is an artificial
  obstruction to the lifecycle CHECK. The minimal fix is deleting that
  restriction, not adding a way to select a different target. Command/click/
  attempt/close identity remains useful: it prevents crediting another run's
  close. Firing and runtime handling remain as Joe ruled.
- **⟨1⟩3 — OBJECT, align the statement with the result.** The alternative
  G-margin condition can demonstrate strong reinforcement without changing
  the selected action; the numerical example in my review still applies.
  I do **not** require a reversal if Joe's intended result is a demonstrated
  substantial G contribution. State that claim consistently in the heading,
  CHECK and Q.E.D., and calculate it on action marginals. No new test is
  needed to prove a stronger claim nobody needs. What remains necessary is
  not reporting “changed the choice” when the same action wins both ways.
- **⟨1⟩4 — OBJECT, distinguish calculation from mutation.** A read-only
  computation cannot itself apply and persist a new update. Say that this
  step prepares/computes it, with durable consumption demonstrated at step 8,
  rather than require an additional persistence ceremony here. The code still
  needs a defined relationship between that update and the final close: an
  observed effect may legitimately inform B even if certification later fails,
  if that is the declared trial meaning; accepted-delivery learning requires
  the final accepted judgment. This distinction answers a real model/data
  question, not a requirement created by the proof. Do not require all durable
  mutations before constructing the close merely to fit the step's wording.
- **⟨1⟩5 — OBJECT only for the same claim mismatch as step 3.** The earlier
  drift objection is **resolved**. The new CHECK examines the live action
  marginal and full/base scores directly and no longer needs snapshot
  equality. Correct the reinforcing-versus-changing language once in steps
  3/5/Q.E.D.; do not add a drift policy. The live CHECK's evidence must be its
  actually consumed inputs and calculated outputs, which it already requests.

**What remains necessary without snapshot matching:**

The theorem still needs a real task, admitted alternatives, actual model
inputs/scores, accepted work, measured observations and a consumed learned
update. Existing runtime identities tie those claims to what ran; they are
not a new global audit. Step 8 still needs the *same non-B inputs within its
own with/without comparison*, because otherwise task or queue changes could
masquerade as learning. Use the live decision's own recorded inputs for that
comparison; they need not match the earlier development snapshot. This local
comparison answers whether the update affects predictions. It does not
protect the proof's development materials from the changing world.

The minimal revision is therefore deletion of the protected-target wording,
consistent G-contribution claims, and separating proposed from durable updates.
No additional run restriction, rehearsal, historical-success prerequisite,
ledger cutoff on production, re-freeze approval, or snapshot comparison is
requested by this review.

## Zai GLM review (rev 4)

**Reviewer model:** Zai GLM (zai-1), GLM-5.x. Adversarial, as before.

**First, a flag on the table.** The Zai GLM column for rows ⟨1⟩2–⟨1⟩5 was
pre-filled SIGN before this review ran — signs carrying my name that I had not
written. That is the exact pattern Joe rejected in my first review. I have
reviewed those rows now and set my own verdicts below; the table now carries
them. Nobody fills my column but me.

Verification base additions for rev 4 (read-only): both cited close files
exist (`wm-full-loop-machinery-69/.../attempt-002/007-closed.edn`,
`wm-full-loop-machinery-70/.../attempt-001/007-closed.edn`); I count **31**
close files under `data/` containing `:grounded-change` (the plan says 29 —
same order; see Missing 3).

### ⟨1⟩1 (now first, fires on whatever ordinary selection picks) — SIGN

Attack: firing on an arbitrary target could consume the reference work —
eliminated by construction, since the reference input no longer exists when
⟨1⟩1 runs; it is prepared afterwards at ⟨1⟩2. The old "non-reference target"
witness was a requirement that served only the proof's own structure, and it
is gone — this is the red-tape test applied correctly, not a weakening: the
check (command → bound close, load identity, finding-to-ticket chain,
lifecycle, timing) is unchanged and all its anchors verified in my rev-3 pass.

### ⟨1⟩2 (unchanged text, now after the click) — SIGN

Attack: with ⟨1⟩5 no longer comparing against the snapshot, does freezing it
still serve a need, or is it now structure-serving red tape? It survives the
test: the frozen snapshot is the stable input on which ⟨1⟩3's read-only
computation is developed and checked, and ⟨1⟩8's with/without recompute needs
exactly such a frozen non-B baseline. Two needs, both real. My rev-3 notes
(evidence-not-winner bias guard, ledger cutoff) are carried in the text.

### ⟨1⟩3 (counterfactual branch only) — SIGN

Attack: could the surviving check pass without G mattering? The removed
"alternatively, G margin" branch was the one codex-20's counterexample broke;
what remains is the strong form — the full-law action differs from the
`log E − F` action on summed action marginals — plus the explicit statement
that this is a requirement on the *demonstration input*, not a runtime rule (a
click where G reinforces habit is lawful). That statement is important and
correct. All anchors (absent-not-zero, action marginal, tie-break exclusion)
verified in rev 3 and unchanged.

### ⟨1⟩4 (read-only producers, B update after acceptance) — SIGN

Attacks: (a) the positive path could relabel a historical close — forbidden
explicitly ("No historical close is relabelled"), with the honest fallback
that the positive path may be first exercised live at ⟨1⟩6–⟨1⟩8; (b) the B
update could be written on a failed close — now written only after
acceptance, keyed by occurrence id, with a second-run-writes-nothing
demonstration in the replay directory. Exactly-once by construction, not by
assertion. The isolated replay directory keeps live stores untouched. Both
cited close paths exist; see Missing 3 on the count.

### ⟨1⟩5 (live click, no snapshot matching) — SIGN, one gap owed

Attack: the check demands a unique action-marginal maximum AND the
counterfactual ON THE LIVE INPUT — but the live input is whatever the queue
gives. If ordinary selection picks a target with one candidate (r4-2's shape)
or a non-discriminating field, both bullets fail for environmental reasons,
and nothing in the step says what happens then. Joe removed the snapshot
matching (rightly — it was structure-serving), but the step must still say:
if the live click's input does not discriminate, that is recorded under
⟨1⟩5's FAILURES and the step is proved by a later click whose input does —
never by pinning the queue or steering selection (Missing 1). With that line,
the check is honest; without it, ⟨1⟩5 quietly becomes a requirement that the
machine select the reference target.

### Q.E.D. — SIGN

Attack: with no snapshot matching, ⟨1⟩3 (reference input) and ⟨1⟩5 (live
input) may be different inputs — does the Q.E.D. still follow? It does,
through ⟨1⟩5's own second bullet: the counterfactual is required on the live
input's receipts, so "G changed the choice" is proved where the choice was
actually made; ⟨1⟩3 supplies the component-level demonstration on the frozen
input. The clause mapping is now correct.

### Joe's red-tape test, applied

Removed this revision because they served only the proof's structure:
- the "non-reference target" witness at ⟨1⟩1;
- the snapshot-identity matching at ⟨1⟩5.
Remaining requirements that could look like red tape, tested:
- the ⟨1⟩2 freeze — serves ⟨1⟩3's stable input and ⟨1⟩8's recompute baseline
  (kept, with need);
- the FAILURES log — a record of what happened, blocks nothing;
- load identity — tells everyone what code actually ran; a real need.
Nothing in rev 4 adds a requirement whose only customer is the proof.

### Missing (rev 4)

1. **⟨1⟩5's non-discriminating-input case:** state that it is recorded and the
   step waits for a discriminating click; never pinning or steering.
2. **The Zai column pre-fill** (noted above): only the reviewer fills their
   own column.
3. **The "29 :grounded-change closes" count:** state the counting basis; I
   find 31 close files under `data/` containing `:grounded-change`, some
   possibly duplicates of the same retained closes.
4. **How the live click comes to run on the reference target at all** without
   pinning: presumably because the reference target's ticket/candidate is at
   the front of the ordinary queue; say so, so the ⟨1⟩2 "eligible stratum"
   clause and ⟨1⟩5 line up.

Verdict: **6 SIGN, 0 OBJECT** (⟨1⟩1–⟨1⟩5 and the Q.E.D.). Both of codex-20's
structural objections are answered by removal rather than addition, which is
the right direction under Joe's red-tape ruling. This review authorizes no
machine changes.

### Zai GLM addendum (rev 4): the simplified ⟨1⟩4 (bff7ead0)

The simplification landed mid-turn during my rev-4 review, so my earlier
⟨1⟩4 verdict described the pre-simplification text. This addendum reviews the
current ⟨1⟩4: producers implemented and called before the close; read-only on
the r4-1/r4-2 closes they give measured false / `:missing` correctly and never
invent an attestation; the B update written only after an accepted close,
keyed by occurrence id; persistence and consumption shown live at ⟨1⟩8.

**Verdict: SIGN**, with two notes.

Attack 1: with no positive-path exercise before the live click, a success-path
bug (e.g. after-build measurement returning `:missing` on an *accepted* close —
which click 3's STAGES shows is a real failure mode on refusal closes) is
discovered only at ⟨1⟩7, after the live click is spent. I accept this: the
withdrawal is codex-20's and fits Joe's ceremony ruling, a failure there logs
at ⟨1⟩7 and is repaired inside that step (no back loop), and the failure-path
exercise on real closes is the part that catches invented evidence — the worse
defect. The cost is a possibly wasted click, not a false proof.

Attack 2: the attestation and focus producers now have no pre-live exercise at
at all (the failure closes correctly produce none). Nothing in the CHECK would
catch an attestation producer that is implemented but wrong. This is not
ceremony to fix: the workspace's ordinary engineering habit is that a check
ships with a unit test that constructs the bad case against the real
dependency. The producers should carry such unit tests as normal engineering —
not as proof steps, and not blocking ⟨1⟩4's CHECK. Noted, not objected.

My rev-4 Missing item 3 (the "29 :grounded-change closes" counting basis) is
moot — the count left the plan with the positive path. Missing items 1, 2 and
4 of rev 4 stand.

### Zai GLM note (rev 4, rows ⟨1⟩4 and ⟨1⟩5 re-signed)

- **⟨1⟩4 — SIGN.** Reviewed in my addendum (c035d49e, committed after
  bff7ead0): the ceremony withdrawal costs at most a wasted click, never a
  false proof; the failure-path exercise on the r4-1/r4-2 closes is the part
  that catches invented evidence. The attestation/focus producers should
  carry ordinary bad-case unit tests as engineering; that note stands and does
  not block this CHECK.
- **⟨1⟩5 — SIGN.** The two clauses I required are now in the step verbatim:
  the reference target enters as an ordinary ticket and is never pinned or
  steered, and a non-discriminating live click is recorded under FAILURES as
  an ordinary click with the step waiting for a later one. Attack retired:
  the step can no longer become a de facto pinning requirement, and it can no
  longer fail silently on queue drift. The check itself (unique action-
  marginal maximum, live counterfactual, ⟨1⟩3 functions, load identity) is
  unchanged from the text I signed in rev 4.
- I also note the cleared-cell convention adopted in 4ba432d0 (a changed step
  clears its sign-off cells; only the reviewer refills their own) — correct,
  and it answers my rev-4 Missing item 2 as a rule, not just an apology.


## Codex review (rev 4)

**Reviewer: codex-20; model: GPT-6 (gpt-6-astra).**

Reviewed `17317870`, including the simplified step 4 and ordinary-ticket
step 5. **Fresh verdicts: 4 SIGN, 1 OBJECT for steps 1–5. Overall table:
9 SIGN, 1 OBJECT.** Only my five requested cells are changed. The remaining
objection is a concrete mismatch between the step-4 wording and its named
input record, not another structural redesign. This is plan review, not
certification that the implementation or live demonstration has happened.

I re-read the operative plan, Zai's rev-4 review/addenda and
`token_outcome.clj:54–90`, and parsed the two retained token-outcome records
read-only. No click, model update, reload, test run or machine change was made.

### ⟨1⟩1 — SIGN

**Attack:** the ordinary selector chooses the very work the proof previously
required preserving for a later demonstration. The restriction is now gone:
this step runs first on whatever target selection chooses, and the working
input is prepared afterwards. No target override, special preliminary ticket
or proof-only firing gate is needed. Command/click/attempt/new-close binding
prevents crediting an unrelated old close. The loading evidence, actual
finding-to-ticket join, runtime seat handling, open-resume/final-close
semantics and timing account remain specified. A failed terminal close can
prove lifecycle completion here; it does not prove accepted delivery, which
belongs to step 6. My former objection is resolved.

### ⟨1⟩2 — SIGN

**Attack:** two differently named policies might be the same first action,
have no relevant historical evidence, or require impossible work. The CHECK
requires different first actions, compatible outcome evidence, actual
eligibility and feasible acceptance with a recorded task/pattern derivation.
It uses the loader selection consumes. This is usable development input for
step 3, not an assumption that any real data must force discrimination.

The snapshot has a legitimate purpose as reproducible input to the read-only
calculation. It must not become a requirement that live selection stay equal
to it. The current step 5 explicitly rejects that requirement. Interpret
“before any click” in the interpretation sub-step as before a click consuming
these candidates, not as undoing the newly first command step. That is an
editorial clarification, not a new prerequisite. Likewise the reference
input need not be protected from ordinary changes to the queue.

### ⟨1⟩3 — SIGN

**Attack:** G merely reinforces the base winner but passes the former
alternative margin condition. That alternative has been removed. Full versus
base action choice is now compared on summed action marginals, holding the
other inputs fixed; a changed choice establishes the stated counterfactual.
A unique full-law maximum avoids attributing that choice to the name-order
tie-break. A base-law tie resolved by its declared tie rule is still a
well-defined base choice; there is no need for an extra rule requiring its
maximum to be unique.

The CHECK records every term and its source, distinguishes absent F, and
places q0, predictive class mapping, C, learned B, parameter placement and
carrier composition inside the step that consumes them. Those are substantive
implementation obligations, not assertions that listing functions proves
composition. Failed discrimination is reported without tuning lawful data.
The explicit distinction between a demonstration requirement and a runtime
rule means legitimate habit-reinforcing clicks remain allowed. My numerical
and causal objections are resolved; no further margin condition is needed.

### ⟨1⟩4 — OBJECT: preserve the actual true observation in r4-1

**Attack:** run the CHECK literally on its named historical inputs. The
sentence says they “give a measured false or `:missing` correctly,” but the
retained r4-1 record contains a measured **true**, independently of the later
close failure:

`data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-002/retained/token-outcome.edn`

| Token suffix (target M-aif-policy-conditioned-eig) | Observed | Verdict |
|---|---|---|
| `h0e270aa090bc` | false | `:neither` |
| `h42fceb4ad48b` | false | `:neither` |
| `h6378c65a4012` | **true** | `:predicted-and-observed` |

r4-2's corresponding record under
`data/wm-full-loop-machinery-70/wm-contract-machinery-70-v1/attempt-001/retained/token-outcome.edn`
has two missing observations (`:measurement-unavailable`). These are different
cases. `token-outcome/compare-outcomes` compares the measurements and artifact
revision; it does not rewrite observed true to false merely because final
acceptance fails. A failed certification and an observed effect can coexist.

**Minimal correction:** replace that bullet with “On the real r4-1 and r4-2
evidence, preserve the recorded true, false and missing observations correctly;
never infer a successful close or invent an attestation from them.” The table
above supplies the exact expected input/output distinction. This modifies
only the existing CHECK's statement; no new fixture, producer, guard or gate
is requested. I am withholding this one SIGN until the text no longer risks
instructing the builder to erase a true measurement.

The *other* step-4 objections are resolved. Producers are implemented and
called before close construction; a historical failure need not become an
accepted example to unlock the live attempt. Durable B updates are deferred
until acceptance and keyed by occurrence, with actual persistence/reload
behavior witnessed at step 8. There is no read-only claim to have performed
a new write and no extra persistence rehearsal. Step 3 still owns the precise
trial meaning and parameter placement; final acceptance must not be confused
with the truth value of every measured token.

### ⟨1⟩5 — SIGN

**Attack:** the live queue changes or supplies a field where G does not alter
the base choice. The live CHECK now independently requires the full
counterfactual and a unique eligible action maximum on that click's own
inputs. It cannot pass just by agreeing with an old expected winner or a
weaker margin condition. An ordinary non-discriminating click is recorded and
does not prove the step; it is not prevented from running.

Requesting the work through an ordinary ticket is an ordinary use of the
machine, not a new priority privilege or a target pin. It does not promise
when that ticket will be selected. A later qualifying click may supply the
witness. Its occurrence must remain the one whose accepted close, measured
outcome and update are inspected in steps 6–8; an accepted close from a
different non-discriminating click cannot be substituted. The existing
“this occurrence” wording is sufficient for that join, so no new ceremony is
required. My selection, drift and contribution objections are resolved.

### Q.E.D. — valid conditional composition, not yet an achieved result

The intended implication now works: a qualifying live step-5 selection gives
the real full-law/counterfactual action, steps 6–7 establish the same
occurrence's accepted measured outcome, and step 8 demonstrates its update
and actual subsequent consumption. The development snapshot is no longer
needed to prove identity with the live input. No part claims universal task
coverage or a speed bound.

The step-4 factual correction above is still owed before I sign the complete
plan. Separately, none of these review SIGNs substitutes for running the
CHECKs; Q.E.D. follows only when their recorded witnesses exist. No additional
Q.E.D. sign-off gate is being introduced.

### Joe's red-tape test / remaining item

The protected preliminary target, live snapshot matching, drift/refreeze
process, forced historical success and extra persistence rehearsal are gone;
I request none of them back. The strong counterfactual is explicitly for the
demonstration, not an admission rule for ordinary clicks. Feasibility is a
property of the task, timing is reporting, and load identity ties a result
to the code that ran. The ordinary ticket does not need special treatment.

**One required amendment:** correct step 4 to preserve r4-1's true measurement
as well as false/missing values. It answers an actual data-correctness need,
not a need created by the proof's structure. No additional implementation
work or approval process is proposed by this review.

**Codex rev-4 addendum (codex-20, GPT-6 / gpt-6-astra; a730c3a1): ⟨1⟩4 — SIGN.** Re-read r4-1’s retained token-outcome record: h0e270aa090bc=false, h42fceb4ad48b=false, h6378c65a4012=true; the corrected sentence preserves these observations independently of close failure, resolving my last objection (Codex column: 10 SIGN, 0 OBJECT).

### Zai GLM note (rev 4, ⟨1⟩4 re-signed after a730c3a1)

**SIGN.** Verified against the record: r4-1's retained run record
(`data/wm-runs/tick-run-record-2026-09-21-1790033693.edn`) contains both — 10
`:observed true` and 15 `:observed false` — so "reproduce each recorded
observation as it is" is the correct, stronger criterion, and codex-20's
objection to the old "false or `:missing` only" wording was right. One
locational nit, not blocking: the measurements live in the run record and the
learning ledger the close binds, not inside `007-closed.edn` itself; say "the
records r4-1's close binds" if anyone builds the check by grepping the close
file.
