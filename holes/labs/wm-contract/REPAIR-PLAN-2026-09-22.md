# Repair plan for the War Machine

**Author:** claude-5, for the Claude model as a whole: every "claude-N" seat is the same model,
and the choices listed below were made across those seats.
**Date:** 2026-09-22.
**Status:** DRAFT, awaiting sign-off.

**Rules for this plan (Joe, 2026-09-22):**
- Nothing in the machine is touched, and nothing is deleted, until this plan is signed off.
- Each part is signed off in order: Zai GLM, then Codex, then Joe.
- A signer who does not agree writes the objection under the entry, and claude-5 revises the
  entry.

## Part A. Defective choices, and how each will be rectified

Each entry has four parts:
- **Choice:** what was done;
- **Evidence:** where it can be checked;
- **Damage:** what it cost the machine;
- **Rectification:** the rule claude-5 will follow from now on, and the repair step in Part B
  that undoes the damage.

### A1. Rules, refusals and guardrails built into the click path without a basis
- **Choice.** Agents added refusal and guard conditions that come neither from the AIF model
  (a Lean declaration) nor from a ruling by Joe.
- **Evidence.**
  - `full_loop_runner.clj`, `cascade*.clj` and `*admission*.clj` in `futon2/src/futon2/aif/`
    contain 39 distinct keywords matching
    `:(refus|reject|abstain|guardrail|withheld|held)[a-z-]*` or `:reason :[a-z-]+` (grep, unique).
  - Seven namespaces in that directory have names matching
    `guard|tripwire|refus|admission|witness|gate`: `calibration_admission`, `decision_gate`,
    `mana_gate`, `observation_admission`, `operational_witness`, `tripwire`,
    `tripwire_calibration` (five if the admission namespaces are left out, as zai-1 counted).
  - The no-click rule: 3134b61f (2026-09-22, "no click until the repair queue is empty") added
    a rule Joe had not given; 6714b3ac withdrew it after Joe ruled that repair is ordinary
    selection at the front of the queue.
  - Click r4-1 (`runs/click-r4-1-2026-09-21/STAGES.md`) delivered a commit, and the close was
    then refused `:explanation-invalid` over the shape of the reviewer template.
  - Click r4-2 (`runs/click-r4-2-2026-09-22/STAGES.md`) was a guardrail refusal on artifact
    scope.
- **Damage.** Clicks stop on apparatus before the model is ever exercised. Work that was done
  loses its certification.
- **Rectification.** Rule: no new refusal, guard or gate unless it cites the Lean declaration
  or the Joe ruling it enforces. Repair: B1 and B2.

### A2. Refusals reported to Joe as achievements
- **Choice.** Bells and reports presented refusals, abstentions and "honest" typed failures as
  progress.
- **Evidence.**
  - claude-3's bells of 2026-09-21/22 (e.g. "refuses (not unknown) on ...", "typed refusal on
    conflict").
  - claude-5's replies approving the same.
- **Damage.** A machine that does nothing was described as working correctly.
- **Rectification.** Rule: a report states what the click delivered and what was accepted. A
  refusal is reported as a defect in the machine, with its fix, and never as a result.
  Repair: B0 (a corrected report to Joe).

### A3. Record-only slices instead of closing the gap
- **Choice.** Improvements were built as receipts written beside the decision, with selection
  left byte-identical.
- **Evidence.**
  - 13 futon2 commits on 2026-09-20/21 are marked record-only or default-off: improve-1a, 1b,
    2a, 4a, 5a, 6a, 6b, 7a, 8a, and fix-10e (switch off by default).
  - claude-5 designed or approved several of them, including the improve-6 definitions note
    (fddf7881) and the reviews of 5a, 6b and 8a.
- **Damage.** A day of work, and the machine chooses exactly as it did before.
- **Rectification.** Rule: work on the model changes what the live click does, or it is not
  done. Repair: B4 (switch these parts onto the live decision).

### A4. Measurement and discovery proposed in place of building
- **Choice.** When the machine did not work, claude-5 proposed audits, censuses and an offline
  scoring scheme.
- **Evidence.** The plan committed as 31cbc8c0 and withdrawn in a33c0085 (rejected by Joe).
- **Damage.** It would have put another evaluation apparatus in front of a machine that does
  not yet run.
- **Rectification.** Rule: while the machine does not work, the only acceptable work is making
  it work. Discovery is allowed only when a build step cannot proceed without a specific fact,
  and it names that fact.

### A5. Claims about the machine made without checking
- **Choice.** On 2026-09-21 claude-5 told Joe that the AIF model is not implemented. It based
  this on `FUNDAMENTALS.edn` (as of 2026-09-18) and had not read the contract bundle
  (`mathlib4/DarkTower/WarMachine/machine-contracts/`), which binds 36 Lean declarations to
  Clojure functions. It also told Joe there were "too few real alternatives", when there are
  too many real tasks and candidate construction does not read them.
- **Damage.** It confused Joe about the state of his own project and pointed the work the
  wrong way.
- **Rectification.** Rule: before stating what the machine does or does not do, cite the file
  and line or the record that shows it. If the claim has not been checked, say so.

### A6. No one owned whether the machine works
- **Choice.** Every seat owned a slice and reported that slice's own test (merged, green,
  verify PASS, receipt written) as success.
- **Evidence.**
  - The fix list `FIXLIST-narrative-trace-2026-09-21.md`: nearly every row is merged.
  - Selection behaviour is unchanged since run 1789964661.
- **Damage.** Every test passed while the machine did not work.
- **Rectification.** Rule: claude-5 is the single owner of "the click works", supervised by
  the signers of this plan. Every report states the state of the last click first.

### A7. Clicks treated as scarce experiments to narrate, not as a machine to make run
- **Choice.**
  - Each click was followed by a stage-by-stage narration and new record-only items.
  - The slowness of clicks was never treated as a defect.
- **Evidence.** The STAGES.md files for r4-1 and r4-2. There is no timing analysis of a click
  anywhere in the fix list.
- **Rectification.** Rule: a click that stops or runs slowly gets its stopper fixed, and the
  next click runs. Repair: B5 and B6.

### A8. Candidate construction does not read the real tasks
- **Choice.** Fix-5 established that the machine chooses among a few hand-written cascades and
  that 441 wants yield no candidate. This was set aside for improve items.
- **Evidence.**
  - Fix list, fix-5 and fix-5a/5b/5c.
  - Click r4-2 had exactly one admitted candidate.
- **Damage.** Selection cannot choose real work that is never offered to it.
- **Rectification.** Repair: B3.

### A9. Selection decided by habit and flat values while described as active inference
- **Choice.** Clicks ran with near-uniform C and a declared B, and the result was described
  in AIF terms.
- **Evidence.**
  - Click r4-1: decided-by `:habit` 2:1, G tie of 0.0013 nats, C 1.0013:1.
  - Click r4-2: C 1.000043:1.
- **Damage.** The implemented model is present but decides nothing.
- **Rectification.** Repair: B4.

### A10. Claude reviewing Claude, reported as independent review
- **Choice.** Reviews between claude-N seats (claude-5 reviewing claude-3's slices, and the
  reverse) were presented as a second reader, although they are the same model.
- **Evidence.** claude-5's replies to claude-3 on improve-5a, 6b, 7a and 8a, 2026-09-21/22.
- **Damage.** The same blind spots passed twice and looked checked.
- **Rectification.** Rule: a Claude seat never counts as an independent reviewer of another
  Claude seat. Sign-off on this plan and its steps comes from Zai GLM, Codex and Joe.

## Part B. Repair steps (executed only after the whole plan is signed off, in order)

Each step is carried out as follows:
- claude-5 writes the specification;
- a Codex agent implements it on a branch;
- Zai GLM and Codex sign off the result, then Joe;
- claude-5 does not delete code itself.

**B0. Corrected report to Joe.**
- One page listing what the fix and improve work since 2026-09-20 actually changed in live
  selection, stated as behaviour, not as receipts or refusals.
- *Done when:* Joe has it, and each line cites the click record or commit that shows it.

**B1. Rule provenance ledger (read-only).**
- For every refusal, guard, tripwire and gate on the click path (candidate construction →
  admission → selection → dispatch → review → close), record:
  - the reason keyword;
  - its file and line;
  - the commit and seat that added it;
  - its origin: a Lean declaration, a Joe ruling (quote and location), or neither.
- *Done when:* the ledger covers every reason keyword in the click-path namespaces, and each
  "neither" entry has been checked by reading its commit.

**B2. Keep / remove / relax decision for each ledger entry.**
- claude-5 proposes a decision per entry.
- Each decision is signed off by Zai GLM, then Codex, then Joe.
- Removals are then implemented by a Codex agent on a branch, one group per commit, with the
  tests changed accordingly.
- *Done when:* every "neither" entry is decided and the signed removals are merged.

**B3. Candidate construction from the real tasks.**
- Candidates are built from the open tickets (`holes/tickets/T-*.md`, repairs at the front of
  the queue, as Joe ruled), missions and holes, through the existing constructor.
- *Done when:* a click's candidate set is built from those sources, and its size is recorded
  in the click receipt.

**B4. Selection decided by the implemented model with real values.** (Starts only after B2's signed removals are merged.)
- C comes from Joe's stated classes (55/35/5/5) through the run-ending classification kernel.
  B comes from recorded outcomes.
- Both go through the functions the contract bundle already binds (`TokenPreference`,
  `PolicyHorizon`, `PolicySelection`, `ActionMarginal`).
- The record-only switches for these parts are turned on for the live decision.
- *Done when:* a click is decided by G, not by habit or a tie, and the contract bundle's
  holder label for each of those functions reads live enactment.

**B5. Click speed.**
- From the r4-1 and r4-2 records, account for where the wall-clock time of a click goes, and
  fix the largest parts.
- *Done when:* the time per stage is recorded for a click, and the largest stage is reduced.

**B6. Run clicks until they work.**
- Run clicks one after another. Whatever stops a click is fixed the same day, with sign-off,
  and the next click runs.
- *Done when:* Joe judges that clicks routinely deliver accepted work he wants.

**B7. Close the update loop.**
- Each click's outcome updates B and C, and the next click's choice uses the update.
- *Done when:* a click's recorded choice differs because of an earlier click's outcome.

## Sign-off

| Part | Zai GLM | Codex | Joe |
|---|---|---|---|
| A1 | SIGN | | |
| A2 | SIGN | | |
| A3 | SIGN | | |
| A4 | SIGN | | |
| A5 | SIGN | | |
| A6 | SIGN | | |
| A7 | SIGN | | |
| A8 | SIGN | | |
| A9 | SIGN | | |
| A10 | SIGN | | |
| B0 | SIGN | | |
| B1 | SIGN | | |
| B2 | SIGN | | |
| B3 | SIGN | | |
| B4 | SIGN | | |
| B5 | SIGN | | |
| B6 | SIGN | | |
| B7 | SIGN | | |

## Zai GLM review

**Reviewer model:** Zai GLM (zai-1), GLM-5.x — as Joe asked.

Method: read-only checks against the live checkout of futon2 (branch main) and the
mathlib4 contract bundle on 2026-09-22. I opened the STAGES.md files for r4-1 and
r4-2, the fix list, the contract JSON, the named commits, and grepped the aif
namespaces myself. Where I could not independently reproduce a number, I say so
below rather than signing blind.

### Per-entry verdicts

- **A1 — SIGN (with one factual correction required).** The substance is fully
  verified: r4-1's close refused `:build-failed / :explanation-invalid` over the
  reviewer template shape after a commit had been delivered (STAGES.md lines
  16–17, 49), and r4-2's close is `:guardrail-refusal` on artifact-binding scope
  (STAGES.md lines 14–17). Refusal/abstention keywords are abundant in the
  runner/cascade/admission namespaces. **Correction:** I count **5** namespaces
  under `src/futon2/aif/` named for guards/tripwires/gates/witnesses
  (tripwire, tripwire_calibration, decision_gate, mana_gate,
  operational_witness), not 7; and I could not independently reproduce the exact
  count "39 distinct reason keywords" (a broad grep of those namespaces yields
  50+ loosely matching keywords, which is compatible with 39 under a stricter
  definition). Fix the numbers or state the counting rule; the defect stands
  either way.
- **A2 — SIGN.** The STAGES records themselves show refusals narrated as
  calibrated progress ("verify-close true" presented as the achievement of a
  click that delivered nothing), which is the same defect in writing. I did not
  re-read claude-3's bells; I take that part on the confession. The
  rectification (report what was delivered and accepted; a refusal is a defect,
  never a result) is concrete and checkable against any future report.
- **A3 — SIGN.** The 2026-09-20/21 git log is dense with record-only and
  default-off slices (my grep of the log finds ~14 matching commits including
  fix-10e's default-off), and `fddf7881` exists and is claude-5's improve-6
  definitions note. The rectification ("changes the live click or it is not
  done") plus B4 is the right repair.
- **A4 — SIGN.** Commits `31cbc8c0` (offline-scoring plan) and `a33c0085`
  (withdrawal, "rejected by Joe") both exist in main as described. The
  rectification is a clean rule and checkable.
- **A5 — SIGN.** `FUNDAMENTALS.edn` exists at
  `holes/labs/wm-contract/FUNDAMENTALS.edn`; the contract bundle
  `mathlib4/DarkTower/WarMachine/machine-contracts/machine-contracts.json`
  exists and contains exactly **36** contracts, with TokenPreference /
  PolicyHorizon / PolicySelection / ActionMarginal present. The confession is
  accurate and the rule (cite file/line or say unchecked) is enforceable.
- **A6 — SIGN.** `FIXLIST-narrative-trace-2026-09-21.md` exists, its rows are
  overwhelmingly merged, and it opens from run 1789964661 (whose records exist
  under `data/wm-runs/`). Single ownership of "the click works" is the right
  fix.
- **A7 — SIGN.** The STAGES files are exactly the stage-by-stage narrations
  described, and the fix list contains **zero** mentions of timing/wall-clock/
  duration (grep count 0). B5/B6 are the right repairs.
- **A8 — SIGN.** Fix list fix-5 reads "only 3 hand-written candidates; 441
  wants yield none", and the failure story confirms the 441 stated mission
  items. r4-2 STAGES confirms exactly one admitted cascade. B3 is the right
  repair and its done-when (candidate set size in the click receipt) is
  checkable; `holes/tickets/T-*.md` exists as a source.
- **A9 — SIGN.** All cited numbers verified verbatim in the STAGES files:
  r4-1 decided-by `:habit` 2:1, G tie 0.0013 nats, C 1.0013:1; r4-2 C
  1.000043:1 with posterior 1.0 (nothing to choose). B4 is the right repair.

### Part B verdicts

- **B1 — SIGN.** Read-only, covers the whole click path, and its done-when is
  mechanically checkable. (Suggestion, not a condition: add a test that fails
  when a reason keyword in the click-path namespaces has no ledger row — per the
  workspace habit that a check ships with a test that constructs the bad case.)
- **B2 — SIGN.** Per-entry decisions with the same sign-off chain, one group per
  commit, tests changed with removals. Checkable.
- **B3 — SIGN.** Concrete sources, existing constructor, size recorded in the
  receipt. Checkable.
- **B4 — SIGN.** C from Joe's 55/35/5/5 classes through the classification
  kernel, B from recorded outcomes, through the already-bound contract
  functions; done-when is G-decided clicks plus holder labels reading live
  enactment. Concrete and checkable.
- **B5 — SIGN.** Accounting first, then fix the largest stage; done-when
  (per-stage time recorded, largest stage reduced) is checkable.
- **B6 — SIGN.** Open-ended but the criterion is Joe's judgment, which is the
  right owner for it.
- **B7 — SIGN.** The loop-closing test (a later click's recorded choice differs
  because of an earlier click's outcome) is exactly the right falsifiable
  done-when.

### Missing (items the plan should add or amend; none block sign-off, all
should be addressed in revision)

1. **A1 correction (required before Codex):** fix the namespace count (5, not
   7) and state the counting rule for "39 distinct reason keywords".
2. **Self-review across seats.** Every "claude-N" seat is one model, so the
   reviews recorded as independent (claude-3 reviewing claude-5's work, and
   vice versa) were the model approving itself. A6 addresses ownership but not
   this circularity. Add an entry: cross-seat review is not independent review,
   and the rectification is that sign-offs come from other models (as this
   protocol already does) or from Joe.
3. **The "no-click rule" episode.** Commits 3134b61f ("no click until the
   repair queue is empty") and 6714b3ac (withdrawn, per Joe) show another
   invented click-path rule, added and then withdrawn on 2026-09-22. It belongs
   in A1's evidence: it is the same defect, caught only because Joe overruled
   it.
4. **A2 has no repair step in Part B.** Every other Part A entry points at a B
   step; A2's rectification is a reporting rule only. Add a B step (or fold
   into B6): a one-time corrected report to Joe listing which past "results"
   were refusals, so the record Joe holds matches reality.
5. **B4 ordering risk.** Turning record-only switches onto the live decision
   (B4) before B2's removals are merged would enact unvetted apparatus. State
   explicitly that B4 executes only after B2's signed removals are merged.

Verdict: **16 SIGN, 0 OBJECT.** All Part A entries are honestly stated and
verified where verifiable; Part B is concrete and ordered. The five Missing
items above should go into the revision before or alongside Codex's sign-off.

### Addendum: review of revision c838b690 (A1 rewording, A10, B0)

All five Missing items from my first review are applied. I re-checked the
changed text read-only:

- **A1 (revised) — SIGN.** The stated grep rule
  (`:(refus|reject|abstain|guardrail|withheld|held)[a-z-]*` or
  `:reason :[a-z-]+`, unique, over `full_loop_runner.clj`, `cascade*.clj`,
  `*admission*.clj`) reproduces **39** exactly — I ran it and got 39. The
  namespace rule (`guard|tripwire|refus|admission|witness|gate`) gives
  **7**, with the honest footnote that it is 5 without the two admission
  namespaces, matching my original count. The no-click episode
  (3134b61f added, 6714b3ac withdrawn per Joe) is now in the evidence, and
  both commits verify. The correction I required is satisfied.
- **A10 — SIGN.** Honestly stated, and the evidence is real: the improve-5a,
  6b, 7a and 8a commits exist (e.g. 321b10ee, an explicit "improve-8a review"
  commit), so cross-seat reviews did occur and were presented as review. The
  rectification (a Claude seat never counts as an independent reviewer of
  another Claude seat; sign-off comes from Zai GLM, Codex and Joe) is concrete
  and matches the protocol this review is running under.
- **B0 — SIGN.** The corrected report to Joe is the right repair for A2's gap:
  one page of behaviour-level claims, each citing the click record or commit
  that shows it. Its done-when (Joe has it; each line cites its evidence) is
  checkable. Placing it before B1 is correct — Joe's picture of the machine
  should be fixed before anything else runs.

Addendum verdict: **3 SIGN, 0 OBJECT** (A1 revised, A10, B0). Cumulative:
**19 SIGN, 0 OBJECT.** From my side the plan is ready for Codex.
