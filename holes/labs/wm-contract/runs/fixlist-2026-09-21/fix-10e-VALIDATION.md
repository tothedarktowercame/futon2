# fix-10e — admitted observations initialize the next selection

Branch `fix/narrative-10e`, base `3185d4fb`. Implementation: `77fea6af`.

The serving declaration → assembly → `token-belief-predecessor/input-receipt`
→ `joint-q0` path now consumes explicitly enabled, independently admitted
D-task v2 observations. The four source declarations beside beta explicitly
set `:token-initialization :enabled false`. Absence also defaults OFF. The
policy appears in declaration-read occurrences (including when selection
abstains), assembled problems, and the selection stage/input receipts. No
runner option silently enables it.

## Declared policy and authority

```edn
{:schema :wm/token-initialization-policy-v1 :enabled false
 :placement :next-selection :unknown :fresh-initialization
 :temporal-order :same-revision-only :historical :declared-revision-only}
```

Enable the target's declaration before the producing attempt. Full declaration
identity is pinned: changing that declaration between attempts refuses its old
observations as `:token-meaning-changed`; this includes changing its switch.
There is no attempt to reinterpret yesterday's bytes under today's declaration.
Consequently, the first tick after OFF → ON initializes fresh; consumption
starts after an attempt under that enabled, unchanged declaration.

The new exact-action-file reader runs the unchanged execution verifier before
exposing the v2 projection. The old reader, positive-only authority and precision
carry admission remain separate. Consumption additionally joins previous-trace
occurrence, carry occurrence, full token universe, declaration digest, locator,
schedule and check evidence. Every token records updated / not-updated / refused,
with Boolean value only for an actual observation. Source occurrence, artifact
revision, meaning/evidence digests and the entire signed projection are retained.

Temporal rules in this first version are deliberately narrow:

- A HEAD-located token uses the previous attempt's artifact observation. If a
  fresh check resolves another revision, refuse that carry as
  `:stale-or-unordered-observation` and retain fresh initialization. This does
  **not** infer Git ancestry or say that a later read proves a newer artifact.
- Same-revision contradictory observations refuse. Missing fresh evidence does
  not erase an admitted, revision-bound previous observation.
- A pinned historical locator uses its separate declared-revision observation,
  never the artifact-revision answer. Thus an earlier task-stated claim cannot
  be overwritten by checking a different revision.
- Missing/unknown D observations get no update; their fresh initializer remains.
  Cross-revision posterior carry and an ancestry-based supersession policy are
  not authorized by this version. No tau is invented from an unscheduled check;
  this policy sets the next initial condition, not a within-rollout transition.

The new stage is `:wm/token-belief-stage-v2`; the consuming input is
`:wm/token-belief-input-v3`. Prospective carry v2 retains identity/domain but no
belief that could be mistaken for a predicted posterior. Historical stage v1
and input v1/v2 replay under their old no-update equalities. New input replay
reconstructs each update and the resulting consumed distribution; scoring
receipts compare actual incoming beliefs with that validated distribution.
G, entity-status belief, habit reinforcement and grounding labels are unchanged.
The participating initialization namespaces are added to load-identity's scope.

## Acceptance and controls

The hermetic two-tick fixture uses the real updater declaration's interpreted
singleton and actual mission text in a temporary Git repository. The unrelated
build leaves the checkbox unchecked. Actual rollout predicts updater probability
1 at tick 1; verified D evidence observes false; actual tick-2 selection q0
excludes the updater and records `[:updated false]`. A missing historical
revision records `:observation-missing` with no update. A separate true artifact
check changes q0 from fresh initialization, so this test distinguishes the ON
and OFF paths rather than succeeding just because fresh initialization already
had the updater false. All node evaluation traces consume the resulting q0.

Controls cover wrong occurrence, wrong predecessor, wrong domain, stale revision,
receipt tampering, invalid policy, OFF not reading signed authority, explicit
OFF in real declarations, and historical receipt replay. The runner test uses
the already passing grounded retention fixture and asserts `closed-execution`
and preservation of the actual selection input. It does not write extra files
in the attempt root.

On base main, copying only the new test namespace into an isolated detached
worktree and running its namespace fails before assertions:

```
Could not locate futon2/aif/token_initialization_policy__init.class,
futon2/aif/token_initialization_policy.clj or
futon2/aif/token_initialization_policy.cljc on classpath.
```

This is the honest baseline failure: the policy/consumption namespace does not
exist on base main. `/tmp/fix-10e-baseline.log` retains fresh output.

## Gates

All tests use the worktree's own JVM, without shared JVM loads or WM clicks.
Exact namespace command:

```
clojure -M:test -m cognitect.test-runner -n <namespace>
```

| Namespace | Tests | Assertions | Result |
|---|---:|---:|---|
| futon2.aif.token-observation-initialization-test | 6 | 50 | pass |
| futon2.aif.d-predecessor-task-authority-test | 8 | 43 | pass |
| futon2.aif.d-task-observations-test | 6 | 53 | pass |
| futon2.aif.token-belief-carry-test | 4 | 39 | pass |
| futon2.aif.token-belief-predecessor-test | 3 | 25 | pass |
| futon2.aif.scoring-input-receipts-test | 3 | 44 | pass |
| futon2.aif.cascade-sources-test | 8 | 33 | pass |
| futon2.aif.cascade-problems-test | 6 | 30 | pass |
| futon2.report.cascade-decision-test | 13 | 94 | pass |
| futon2.aif.load-identity-test | 3 | 12 | pass |
| futon2.aif.full-loop-runner-test | 185 | 1061 | pass |

Fresh logs: `/tmp/fix-10e-<namespace>.log`; final frozen-code runner replay:
`/tmp/fix-10e-futon2.aif.full-loop-runner-test-final.log`.
Syntax commands from the worktree root:

```bash
files=(
  scripts/futon2/report/war_machine.clj
  src/futon2/aif/cascade_problems.clj
  src/futon2/aif/cascade_sources.clj
  src/futon2/aif/d_predecessor_task_authority.clj
  src/futon2/aif/load_identity.clj
  src/futon2/aif/scoring_input_receipts.clj
  src/futon2/aif/token_belief_carry.clj
  src/futon2/aif/token_belief_predecessor.clj
  src/futon2/aif/token_initialization_policy.clj
  test/futon2/aif/d_predecessor_task_authority_test.clj
  test/futon2/aif/full_loop_runner_test.clj
  test/futon2/aif/token_observation_initialization_test.clj
)
clj-kondo --lint "${files[@]}"
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el \
  --eval '(arxana-check-parens-cli)' -- "${files[@]}"
```

Kondo: 0 errors, 0 warnings (one existing informational `str` diagnostic).

Result `OK`; `git diff --check` clean. Canonical store file counts before/after
the hermetic test batch were unchanged: futon2/data 4072/4072, futon3c/data
300984/300984. These counts precede the requested registry evidence publication.

The reference D record also replays with no caller print bindings:
`clojure -M /tmp/fix-10b-reference-replay.clj` — v1 admitted/updater unknown,
v2 admitted/updater observed false. This is retained-job-snapshot replay, not
fresh Agency job retrieval. No historical record/digest was modified.

Registry warrant for the six-test actual-selection acceptance:
`test-registry-74194b7339582b1d167ab11501f046e6bfc7d8aa230386f150f355dd2638cd55`.
Run: `clojure -M -m futon3c.test-registry run /tmp/fix-10e-registry.edn` from
futon3c; result `:warrant? true`, 6 tests / 50 assertions / exit 0. Scoped check:
`clojure -M -m futon3c.test-registry check /tmp/fix-10e-registry-check.edn`.
The fresh scoped check also returned `:warrant? true`. This warrant covers
the named acceptance, not every runner test.
