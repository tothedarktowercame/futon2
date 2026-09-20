# WM registry orthogonality

Implementation `157b5ab8`, main merge `7d583fb4`.

Retired the entire live declaration for M-wm-aif-policy-grain-compliance, including
its sole want and produced outcome, as approved. No replacement declaration or
selection change. Retirement here is removal from live cascade declarations;
no mission/repair database was hand-edited.

Removed C1/C2 handlers and dispatch entries, registry resolution and discovery,
in-process check-record!, CLI-JVM fallback, and shared registry-read wrapper.
The deleted handler includes 426ef6f0's scope-drift change. Contract classes,
handler map, clause bodies and cutoff now exclude C1/C2. Assembly admits only
C3/C4/C5. C5 and 78391df4's immutable observation-reference resolution remain.

Load checks all declared locators, including unused ones, for both :class and
:check spellings. C1/C2 throw :invalid-cascade-source with
:reason :removed-observation-check, naming check, field, token, target and path.
The negative controls first load a valid declaration, then add each removed
check and assert the mutation, typed refusal, and zero calls to observe.

New namespace: 2 tests / 40 assertions, no failures/errors. Remaining observation
checks: 6 tests / 18 assertions; cascade sources: 6 tests / 22 assertions, all pass.
Obsolete warrant-handler tests were removed with their API. The loader's older
unknown-fact fixture now uses a C3 missing commit (rather than a C2 missing
warrant) and retains its true/false/unknown assertions. No surviving check was
weakened. clj-kondo 0 errors / 0 warnings; check-parens OK.

The reduced declarations were loaded and written through the test runner's run
record path. This is not a fresh live selection-certificate claim: that remains
deferred to the planned measurement click. Existing good record
`tick-run-record-2026-09-20-1789862860.edn` has identical wm_run_validity output
before/after (saved beside this report).

Registry grep across src, scripts/futon2 and resources/wm returns NO matches
for test-registry, check-record!, with-registry-runs, registry-check-cli,
registry-runs or api/alpha/test-registry/check. C5's check-registry-entry is
retained because it reads a Git contract bundle. Historical standalone
scripts/wm04/evidence_run.clj still names old C1/C2 locators; it is not called
from the run path and the removed handlers are no longer callable through
observation dispatch. Agent workflow registry registration is unchanged.

Reload REQUIRED from canonical main, in order:
1. futon2.aif.observation-checks (rebuilds the C3/C4/C5 dispatch map)
2. futon2.aif.cascade-problems (updates the admitted class set)
3. futon2.aif.cascade-sources (removes registry wrapper and adds load refusal)

The construction namespace change is documentation only. Judge
futon2.report.war-machine is unchanged and uses those Vars; no judge reload is
required by this patch. Declarations are enumerated on load, so the deleted file
is not retained in a namespace cache. Existing unrelated pending reloads are
not discharged by this statement. No reload, click, or manual data edit performed.

Warrant: test-registry-732e073b105e76f7ac7dc03a30c44e3a8e341edfd4eedf6c64fdfecc28ddb5f9
bound to WM-registry-orthogonality.
