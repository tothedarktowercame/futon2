# Witness reference observation and eight-row review

Implementation 131ebd8c; main merge fb267c60. Review baseline: spec 4896fcef.

C6 is a repository-generic observation class. Its locator names repo, sha,
path and optionally require-entry. The EDN witness contains repo, sha and
optionally entry (a repository path). Both references are resolved to commits
and retained. The entry is checked at the resolved referenced commit.
Missing witness -> false; malformed witness -> invalid-witness refusal;
unresolvable reference -> unknown-sha refusal; absent referenced entry -> false.
The class does not establish authorship, event identity, ordering, acceptance
correctness or grounding. Those are not claimed by the check.

All three EoI locators now use C6, retaining their existing witness paths in
futon5a. AUTHORED-CHANGE may name a commit alone; PREMISE-REFUSAL and
CORRECTION-PAR require an entry identifying their committed refusal/acceptance
record. No witness was created by this task. All three remain observed false.

Each :entries value declares :owner :community and :owner-status :not-consumed.
The declaration explicitly says ownership is not consumed by loading,
assembly, projection or scoring. There is no claimed ownership enforcement.

## Eight-row review against 4896fcef

| Row | Result and evidence |
|---|---|
| 1 | Pass: three wants, all currently observed false, no observation refusals; declaration assembles. |
| 2 | Pass under recorded relaxation: each entry names community, explicitly unconsumed. One effective preference spec; no additional owner spec introduced. |
| 3 | Not exercised in this single-spec declaration: no cross-owner zero sets. No claim of implemented multi-owner conflict detection; queued as ruled. |
| 4 | Pass for reference existence: C6 actually resolves the witness reference; refusal/acceptance require an entry. The constructed bad reference refuses, despite the witness file existing in Git. No relational truth claim. |
| 5 | Pass by unchanged implementation review: empty zeroed set here; joint merge preserves zeroed. Judge retains :derived-no-overlap in its no-reachable-want branch. No smoothing added. Existing record-validity output unchanged. |
| 6 | Pass: declared 1/0 consumed. Re-run lam=2 reaches the merged production spec with weight sum 2; lam=0 refuses :invalid-preference-scale. |
| 7 | Pass for this spec: full want coverage scores strictly above empty coverage with equal evidence; witnesses live outside run bookkeeping and are not emitted by the runner. |
| 8 | Single-owner case only, as permitted: community provenance with unchanged scalar interface. Future multi-owner threading/rejection remains queued; not claimed implemented. |

Every want remains in pattern produces. Existing source-pinned pattern
interpretations remain local, with no library edits. The unchanged live-C
projection supplies corpus weight; no preference mass was authored here.
Acceptance tick was NOT fired: in-domain count and C verdict remain unmeasured.

## Controls and gates

Real temporary Git repository: valid witness resolves true. Mutating its sha
from a real commit to "non-resolving-reference" changes the witness and yields
{:status :missing :kind :unknown-sha :data {:repo "fixture"
:sha "non-resolving-reference"}} under :refused, with no result for that token.
Missing file false, missing entry false, missing required entry :no-locator,
non-map assertion-only witness :invalid-witness. Logs retain actual receipts.

New namespace: 3 tests, 27 assertions, zero failures/errors (also registry-run
on merged main). EoI namespace 2/21; orthogonality namespace 2/40, all pass.
clj-kondo 0 errors/0 warnings; check-parens OK. Existing good-record validity
output byte-identical to declared-scales baseline (VALID 4/5, same F flag).

Warrant test-registry-b14d8112d1ccf22df261381e44e07d113a537910a6eac8a59a36db8d614e7313
Subject WM-witness-reference-observation. HTTP check current-validity true.

This patch requires reloads of futon2.aif.observation-checks and
futon2.aif.cascade-problems. Contract/declarations are read per invocation.
If row-6 reloads remain pending, the combined list is live-c,
observation-checks, cascade-sources, cascade-problems, then
futon2.report.war-machine. No serving reload or click performed.
