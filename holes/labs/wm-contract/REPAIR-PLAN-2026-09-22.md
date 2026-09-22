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
  - The runner, cascade and admission namespaces in `futon2/src/futon2/aif/` contain 39
    distinct refusal/abstention reason keywords.
  - Seven namespaces there are named for guards, tripwires, gates or witnesses.
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

## Part B. Repair steps (executed only after the whole plan is signed off, in order)

Each step is carried out as follows:
- claude-5 writes the specification;
- a Codex agent implements it on a branch;
- Zai GLM and Codex sign off the result, then Joe;
- claude-5 does not delete code itself.

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

**B4. Selection decided by the implemented model with real values.**
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
| A1 | | | |
| A2 | | | |
| A3 | | | |
| A4 | | | |
| A5 | | | |
| A6 | | | |
| A7 | | | |
| A8 | | | |
| A9 | | | |
| B1 | | | |
| B2 | | | |
| B3 | | | |
| B4 | | | |
| B5 | | | |
| B6 | | | |
| B7 | | | |
