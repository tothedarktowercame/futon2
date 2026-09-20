Current delivery status is in [EXECUTION.md](EXECUTION.md). The checkpoint history below is retained for audit.

# Q phase 2 dependency checkpoint — 2026-09-20

Draft only. No Q implementation commit, final execution receipt, registry warrant,
serving reload, or production conditioning claim.

Worktree: `/home/joe/code/futon2-q-conditioned-evaluator`, branch
`codex7/q-conditioned-evaluator`, based on D 2a + receipts `8d1afde8`.
Q edits have been removed from the canonical checkout. D's staged/committed edits
were preserved. The owner is claude-12; author is codex-7.

## Implemented in the draft

- `conditioned_trajectory.clj`: verify the incoming stored-update receipt against
  the belief actually consumed; record its original receipt, observation, prior,
  prediction, continuation, successful-consumption status and exact vacuity.
  The active receipt schema is provisional, awaiting D 2b.
- The two sparse branches consume one lazy sequential predictive producer. It
  receives the manifest's existing `push-forward`; the namespace graph is acyclic
  and no Bayes implementation or finite observation enumeration was added.
- `efe.clj` passes the state's update receipt to sparse scoring. F's evidence
  input, bounded scoring and selection semantics are untouched.
- Q's census distinguishes absent conditioning, vacuous successful conditioning,
  and non-vacuous successful conditioning. Refusal-only stays degenerate;
  malformed evidence and inconsistent vacuity/consumption flags are missing
  evidence, not a green verdict.
- Tests use the real exact adapter and manifest. Symmetric and asymmetric
  one-token mixtures are checked against the per-token A scored by the sparse
  evaluator. They are declared synthetic parameters, not estimated rates.
  These are not demonstrations of coupled multi-token G.

## Validation so far

`development-after-d2a.log` and `.json`: 10 tests, 80 assertions, zero failures
or errors; stable source hashes; fresh tooling JVM in the isolated checkout.
Latest clj-kondo: zero errors/warnings. Latest check-parens.el: OK.

Earlier `pre-commit.log`/`.json`: 7 tests, 68 assertions before scorer-entry and
additional vacuity/early-stop tests and before the acyclic producer refinement.
Earlier `g-term-pre-commit.log`/`.json`: 6 tests, 78 assertions, including the
existing recorded tick replay. These logs describe their exact earlier bytes,
not the final implementation. E's existing nondegenerate test fixture was
updated to supply the required all-habits context introduced by futon2
`3b11c270`; E implementation was not changed.

The initial registry check refused `:missing-entry`; no Q warrant exists yet.
Final validation must follow the final receipt contract and integration.
Claude-12 authorized local pre-commit validation, commit, then registered
post-commit execution; record both roles and byte hashes. Source ruling:
`invoke-1789929602280-22706-fb4c4c74`. D's EXECUTION.md is the precedent.

## Required before landing

Codex-6's answer in `invoke-1789929808236-22711-8d738cdd`:
D 2a landed (`c300c67b`, receipts `8d1afde8`) but the active v1 receipt is NOT
frozen. Staged `:token-belief-stage` is `:not-wired` and must not enter Q as
an update. Channel authority and checkable reinitialization metadata require
the D 2b checkpoint. Do not infer checkability or accept arbitrary recovery.

The latest recorded ruling is `1bc98c04`, PLAN-a-programme-2026-09-20.md:
judgement refusal continues from the predicted belief with
`:refused-observation-discarded`; checkable refusal reinitializes from the
admitted observation with `:refused-reinitialized-from-observation`. Both
remain mathematical refusals. The current draft handles the former and
intentionally rejects the latter until its evidence contract is known.

After the freeze: align intake with the actual D producer, check both refusal
channels using real D receipts (including negative channel-authority cases),
align the likelihood evidence path with that frozen receipt, complete the production entry join,
revalidate, commit, issue and commit the registry receipts and demonstrations,
merge onto main, and bell claude-12 for independent review. Do not represent
this checkpoint as Q completion.

## Vacuity addendum implemented in draft

Request `invoke-1789929459348-22702-f16efd06`: every vacuous event now records
`:vacuity-license` with the exact fully qualified theorem, predicted-state
support, likelihood value at each support state, the positive constant value,
and normalized predicted mass. Point support cites `exactUpdate_pointMass_vacuous`;
a larger constant-likelihood support cites `exactUpdate_vacuous_of_const_likelihood`.
The values come from the exact adapter's retained likelihood map; no Bayes
recomputation or likelihood inferred from unchanged beliefs.

The census categorizes from predicted/posterior equality. It independently
checks the recorded license against the retained likelihoods; a tampered or
missing license is missing evidence. Intake refuses to fabricate a license if
likelihood evidence is absent, nonconstant, or zero. The exact active D receipt
path for those likelihoods remains subject to D 2b's interface freeze.

`vacuity-addendum.log`/`.json`: fresh tooling JVM, 11 tests / 98 assertions,
zero failures/errors, stable recorded hashes. Lint zero errors/warnings;
check-parens OK. Controls show the point-mass license expiring when support
spreads under the same actual A, non-vacuous consumption on the spread fixture,
and a vacuous distributed fixture under constant likelihood. Altered support,
constant value, and absent license/evidence are caught. This remains draft
validation, not a landing warrant or Q completion.

## Landing-report clarifications

Owner reply `invoke-1789930172127-22716-49a42139`: the constructed
flag-contradicting-beliefs control returns **`:status :missing`**, not a generic
rejection or a degenerate verdict. The test asserts that exact status.
The E fixture adjustment conforms to the prior E fix `3b11c270`.
Final integration must retain separate D/Q commits and rerun the targeted
namespace's pre-commit/post-commit sequence if integration changes its scope.
Lean conformance remains limited to the tick-grain observed prefix under the
stated hypotheses; refusal recovery and predictive future extensions are not
claimed as instances of `conditionedTrajectory`.

## Canonical warrant sequencing

Owner ruling `invoke-1789930485459-22720-5316627c`: worktree validation uses
only fresh tooling processes. Never load worktree files into a shared JVM.
The `:post-commit-warrant` execution occurs AFTER Q's follow-up commits land
on canonical `/home/joe/code/futon2`, not on the worktree branch. Registry
configuration names that canonical root and artifact directory. If integration
changes the tested scope, repeat the pre-commit validation for those bytes
before committing, then obtain the canonical registered warrant. Consolidate
the worktree back onto the canonical branch; do not leave a runtime fork.

## Frozen artifact consumed

The dependency was resolved by `5a0f6a3f` (schema and six real-adapter examples).
The examples fix the carrier universe at `[:carrier :universe]`. Q now uses
`:calculation`, `:refused-trajectory`, explicit `:consumed` and the frozen
license fields, verifies both refusal channels, and tests all six examples.
The earlier provisional-shape notes above are historical. See EXECUTION.md
for current behavior, validation roles, and the production-admission limit.
