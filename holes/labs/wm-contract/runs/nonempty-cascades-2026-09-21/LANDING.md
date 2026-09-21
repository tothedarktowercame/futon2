# Nonempty executable cascade families

Author: codex-8. Requesting owner: claude-12.
Authorization: invoke-1789961013061-22833-8d700d74.
Independent review: pending owner review.

Assembly no longer prepends an empty order. Each constructed candidate owns
its candidate ID, order and construction receipt in one map. Candidate IDs
retain their source proposal index even when an earlier proposal is declined.
The old parallel construction-receipts vector is removed.

Every executable candidate passes the same admission: nonempty order,
construction receipt, interpreted pattern maps, and nonempty interpretation
receipt maps for every referenced pattern. Declines retain target, stage,
reason and missing evidence. Targets with no admitted proposal are removed
before diagnostic lanes or joint scoring, with a target-level decline.
An all-declined family returns a typed abstention with
:no-acting-cascade-candidate and its per-target refusals.

The model's candidate-space API still supplies its empty diagnostic baseline.
That mathematical contract is unchanged. At the production decision boundary,
per-target baseline candidates appear only under :null-comparison, labeled
:per-target-diagnostic-baseline and :used-for-joint-selection? false.
They are not in executable :candidates or the joint scoring family.

## Acceptance quartet

1. Real-candidate test: three admitted real candidates, no empty sibling.
   Reversing explicit order/receipt pairs preserves each receipt's own order.
   Exported lane candidates are nonempty; diagnostic nulls are separately labeled.
2. No-admission tests: missing receipts, receipts for unrelated patterns, empty
   orders, or no paired constructions all produce zero executable candidates
   and a target-level decline. Malformed source proposals are also recorded
   at construction admission, without renumbering surviving candidates.
3. Every-target-declines tests: typed :abstained result, reason
   :no-acting-cascade-candidate, no posterior and no lanes. Existing selection
   certificate all-empty-roster control remains unchanged and passing.
4. Full-loop runner: see pre/post test receipts for final counts.

clj-kondo and check-parens logs accompany this receipt. One intermediate runner
pass predates the diagnostic-output label and is retained separately; it is
not the final-source warrant. Source hashes and exact registered results are
recorded in precommit.json and postcommit.json.

Part (b), generated interpretation admission, is not implemented.
No live tick, shared-JVM load, or repair-store closing was performed.
COMMITTED IS NOT LOADED: the serving JVM still needs the owning deployment
workflow to load the committed canonical namespaces before the next tick.

## Landing and warrant scope

Implementation commit: c155d690cc1b27f42094ea55a2159bed82c136d7.
All five final postcommit registrations passed. Runner: 180/998/0/0.
The canonical assembly test passed but its warrant was refused because
decision_gate.clj was concurrently dirty. The refusal is preserved.
All five warrants were therefore issued and checked against the clean
pinned checkout /home/joe/code/futon2-nonempty-warrant at c155d690,
retained for review. They do not warrant newer canonical edits.

- assembly: test-registry-67896344fec255792d10f36756f40a751d3f3cac3b2de7ec776308d731bf4a6e
- decision: test-registry-2c762e6b9dea1d5474d87c2c3b59ff23b8ce3c035f494fbdbb59880d52f454af
- construction: test-registry-8db35310204ad0dbdb643c94b0428bf7eee15ccc06699ef4d1dbbf30738415a9
- selection: test-registry-a6684487fe71b0c48b5ce47aeb97291f684d7e9289a89f000be0358c06bec441
- runner: test-registry-97f6993558b5b8ebf058a9bfd142eb63f79ee175e5c9489125880ee71409b330

Exact hashes, roles, scope, and results: postcommit.json.

## Canonical re-registration — 2026-09-21

Authorization: invoke-1789963377231-22865-21f8ad82, claude-12.
All five namespaces were freshly run and registered on canonical
/home/joe/code/futon2 at 6ace66a8620855de70451db043d1fde2fbdbef43.
All five subsequent canonical warrant checks returned true.
These current-source warrants replace the need to rely on the earlier pinned
checkout for this packet. The historical pinned evidence remains unchanged.
The current war_machine includes the subsequent proposal-supply recording;
canonical hashes, rather than an assertion of equality to c155d690, bind these runs.

| Scope | Tests | Assertions | Failures | Errors |
|---|---:|---:|---:|---:|
| assembly | 6 | 30 | 0 | 0 |
| decision | 13 | 91 | 0 | 0 |
| construction | 8 | 34 | 0 | 0 |
| selection | 9 | 109 | 0 | 0 |
| runner | 180 | 998 | 0 | 0 |

- assembly: test-registry-0fb2869496c9806cf014061cef5de5b78c597a625e166bd9c14674fb066ef2a2
- decision: test-registry-f08d640acf6072bbf8944a265a7265e8f40e82893218db0b2401de4fa2d99f5f
- construction: test-registry-35d6cf4678d662a5115da35f8f742af799abaf789b9fc93ab0f644cd4bbbb984
- selection: test-registry-fc4d8cecd6dfadc0b20e7abcc7611fa9b3c9654d79f247e10a66843b754f8333
- runner: test-registry-53b55b6994d70fe4bfa31ff55bc1094ff3a9d78b97f3a0dbcfc5caed897ed1ac

Full records: canonical-reregistration/receipt.json and checks.json, with fresh
logs and dependency closures alongside. Source files were not changed. No live
reload or tick occurred. COMMITTED IS NOT LOADED. Item 4 repair-store writes
remain held pending the commissioned Agency-job review verdict.
