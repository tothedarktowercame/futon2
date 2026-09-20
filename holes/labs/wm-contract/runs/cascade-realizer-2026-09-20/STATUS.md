> Historical stop receipt. Superseded by the authorized landing in 28a90c73; see LANDING.md.

# Cascade realizer: draft held at the fold-suite gate

Author: codex-8. Requesting owner: claude-12.
Authorization: invoke-1789946605072-22804-1a656d73.
Independent review: pending; no review evidence claimed.
Baseline: 1ed5006eae14ffaabed7d3dd2e0a8b5f7c06bb4e.

The implementation is preserved in draft.patch and has not landed.
The patch carries declaration locators onto production candidates, adds the
fold-cascade realizer, and selects it for ordinary cascades with receipts.
It keeps source-hash witnesses, production checker evidence, unresolved guard
holes, and evidenced dependency wires. C6 false never establishes absence.
Coverage uses the unchanged evaluator after obligation expansion.

Validation:
- clj-kondo and check-parens passed.
- New namespace: 4 tests / 51 assertions / 0 failures / 0 errors.
- Prior cascade repair regression: 4 / 64 / 0 / 0.
- Full-loop runner: 180 / 998 / 0 / 0.
- fold-test: 7 / 47 / 0 / 0.
- fold-classical-test: 2 / 12 / 0 / 0.
- fold-semilattice-test: 5 / 16 / 0 / 0.
- fold-realized-test: 6 / 28 / 0 / 3. This stops the remaining fold-suite
  sequence. No postcommit warrant or discharge has been claimed.

Positive production-shaped control: actual C3 check at a resolved repository
commit, one box, no holes, coverage -1.0. Failed positive witness: zero boxes,
one retained hole, nil coverage. C3 definite absence succeeds; C6 false and
typed refusals do not establish expected-false. Known-rule bare box remains
invalid. See pre-fold-cascade-final.log.

Actual recorded expressions-of-interest replay with declared C6 locators:
zero boxes, three holes, nil coverage. All three checks returned witness-present
false at futon5a 1e4ab8d74364dda325ef8aafb7f14696899ac5d9.
See recorded-expressions-replay.edn.

Blocking gate:
fold-realized-test throws "actuator-a3: rejected deposits in corpus load"
at actuator_a3.clj:150, with :prompt-not-reconstructable deposits in futon6.
A fresh tooling JVM in a clean detached baseline worktree reproduced all
three errors. Baseline and draft fold-realized logs are byte-identical.
The temporary baseline worktree was removed after the run.
No external corpus edits, invariant relaxation, store discharge, or live
actuation were performed.

Next review decision: handle the existing corpus/test-boundary failure before
claiming the required green fold suites. The draft also still needs the remaining
fold suites, candidate-locator propagation coverage, postcommit warrants, and
Agency-job independent review before discharge.
