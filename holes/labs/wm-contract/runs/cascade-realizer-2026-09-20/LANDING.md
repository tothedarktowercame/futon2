# Cascade realizer landing

Implementation: 28a90c73. Author: codex-8. Requesting owner: claude-12.
Commissioned independent reviewer: claude-2; Agency-job review pending.
Store writes and discharge remain held until that review arrives.

The selected-cascade path now uses fold-cascade when interpretation receipts
exist. Production candidates retain their declared observation locators.
The realizer calls observation-checks/observe with those locators and retains
resolved revisions, exact checker results, interpretation source hashes, and
per-condition witnesses. C6 false cannot establish a negative guard. Missing
or failed guards retain their output obligations as holes. Dependency wires
name the shared declared output/input and carry the input observation.
Classical and semilattice construction behavior remains intact with route
markers added to their production outputs. Both validators, fold-eval, and
meme.fold are unchanged.

## Demonstration trio

1. Constructed production-shaped positive: the real C3 checker establishes a
   file-presence guard at a resolved futon2 commit. One enriched box, no holes,
   coverage -1.0 through the unchanged evaluator. This is the first successful
   construction demonstrated by the new cascade-specific path; it is a
   construction result, not a claim that the declared output was enacted.
2. Failed positive witness: zero boxes, one retained obligation, nil coverage.
   Additional controls establish C3 definite absence, reject C6 false as
   expected-false evidence, and retain holes for refused checks.
3. Recorded expressions-of-interest action plus its declared C6 locators:
   zero boxes, three holes, nil coverage. Actual witness files were absent at
   futon5a 1e4ab8d74364dda325ef8aafb7f14696899ac5d9.
   See recorded-expressions-replay.edn.

## Validation and attribution

Precommit source hashes and author/owner/reviewer roles are in
landing-precommit.json. The restored implementation matches the draft's tested
source hashes exactly; prior runner 180/998/0/0, realizer 4/51/0/0, and repair
regression 4/64/0/0 evidence was retained. Added locator-propagation coverage:
cascade-decision 11/55/0/0. clj-kondo and check-parens passed.

Other passing fold suites: contract 7/47, classical 2/12, semilattice 5/16,
LLM 8/32, escrow seam 2/13 (all zero failures/errors).

Pre-existing corpus failures remain visible:
- fold-realized: 6 tests, 28 assertions, 0 failures, 3 errors; clean baseline
  1ed5006e gives byte-identical output.
- fold-escrow: 8 tests, 30 assertions, 6 failures, 0 errors; same baseline
  gives identical output except randomized temporary directory names.
  The exact normalization and comparison are recorded in landing-precommit.json.

Both failures concern existing prompt-not-reconstructable futon6 deposits.
Landing proceeds under owner ruling invoke-1789947124666-22806-d267f61b;
no corpus changes, quarantine bypass, or test weakening were made.
Postcommit registrations and current warrant checks accompany this receipt.

All four postcommit warrants passed and checked valid in the canonical checkout:
- realizer: test-registry-b563ff90af46e0592d8b2f7dfb029ac66a342529b7d37550e402776a83850a1d; run 12a5969f-fa94-4799-9c30-17ba31f4ecf9.
- decision: test-registry-873a97f0d0f6a77877c675d536f57ab9985ac33919155a527b40632a3246980e; run 86a5ecb9-a93e-404b-b3e1-e579f850c997.
- repair: test-registry-9a46c3e56a8de3cce6252dea92e0379dc1099e7d8ecebfe4d8aa43847a243ec4; run 8b9bfca2-dcb6-498e-9b2f-c36410d30482.
- runner: test-registry-4788e2b5db9181772016a257f6e79707b07aed09482ba9f559370157527b7fdb; run 85213a15-8e21-4f6c-ba9a-3a7a4223ed8d.

Exact hashes, results and participant roles: landing-postcommit.json.
