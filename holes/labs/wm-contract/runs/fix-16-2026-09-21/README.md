# Fix-16: refuse candidates that predict no new target want

Codex-11; `fix/narrative-16`, based on main `60a139d8`.

Admission now calls the same `cascade-model-manifest/rollout` used by the
constructor and scorer, with `cascade-policy/token-interpretation` and the
declared horizon. It runs after the existing nonempty-order and receipt
checks, before lane construction or joint scoring. No selector changes.

For the current fresh-fact point-mass D, it asks whether any wanted token
absent initially has positive probability of being present at the terminal
step. Patterns have the existing add-only semantics, so this also detects
whether a wanted token was newly produced anywhere within the horizon.
Producing an unrelated prerequisite alone is insufficient. The initializer
uses the target's own true facts; this is the local projection of the current
scoring D, which does not consume prospective belief carry. The recorded-run
fixture asserts that recombining those facts exactly reproduces recorded D.

A rejected candidate has `:stage :candidate-admission`,
`:reason :no-new-wanted-token`, its target/candidate id, and evidence:

- declared horizon and wanted tokens;
- raw initial facts (including unknown values) and initial true state;
- initial wanted tokens and the terminal belief projected onto wanted tokens;
- the empty new-wanted-token set and the rollout function/semantics used.

This is model prediction, not a claim that implementation outcomes were
observed. Missing horizon or a model refusal is separately typed as
`:candidate-prediction-refused`; it is not mislabeled as evidence of no
progress. There is no default horizon.

The existing candidate declines flow into `:dropped-candidates` and
`:cascade-problems :dropped-candidates`, which the tick retains. If every
candidate is refused, the existing target refusal is `:no-constructed-candidate`
and the existing decision is `:abstained` / `:no-acting-cascade-candidate`.
No fallback candidate, selection, or lane is fabricated.

The existing common-horizon/common-beta validation was extracted into one
helper and is also called before admission. Otherwise a newly dropped target
could conceal an incompatible declared family. A regression pins that case.

## Recorded and constructed cases

`test/fixtures/new-wanted-token/1789964661.edn` projects the frozen run's
candidate maps, interpreted patterns, receipts, target facts, terminal C's
wanted tokens, T=2 and beta into the existing admission input shape. It
retains the original D; the test checks exact equality with the real
`scoring-input-receipts/initial-belief` result. The rollout is real, not stubbed.

| Case | Result |
|---|---|
| Recorded AIF candidate | admitted |
| Recorded F11 candidate | admitted |
| Recorded external-F2 candidate | refused, `:no-new-wanted-token` |
| One newly satisfied wanted token (another remains unmet) | admitted |
| Step 1 produces a prerequisite, step 2 produces the want; T=1 | refused |
| Same candidate, T=2 | admitted |
| F2 is the only submitted target | existing abstention, with retained drop evidence |

For F2, `:initial-wanted-tokens` is `#{:route-a-rehearsal-reported}` and
`:terminal-wanted-belief` is `{#{:route-a-rehearsal-reported} 1}`.

The beyond-horizon refusal is the right call under the commissioned rule:
the declared comparison cannot credit a wanted outcome it does not predict
within its horizon. This does not assert permanent impossibility. Increasing
the declared common horizon can admit it, as the T=2 control demonstrates.

The existing target-separation test was updated for the new behavior: a
guard-blocked target is now declined rather than retained at a worse score.
It still checks that A's true fact cannot enable B's guard, and adds the
positive control where B has its own true fact: both candidates then enter
the posterior and their identically named patterns remain separate actions.

## Current declared sources

Read-only census at `2026-09-21T14:15:43.122656191Z`, using the reference
run's declared horizon 2, the canonical declaration files, and their real
observation checks. No tick, scan, selector, or actuation was run. Full source
hashes, observed facts/check evidence, and declines are in `source-census.edn`.

| Declared target | Before this admission check | After |
|---|---:|---:|
| M-aif-policy-conditioned-eig | 1 candidate | 1 |
| M-f11-find-production-successor | 1 candidate | 1 |
| M-wm-08-external-f2 | 1 candidate | 0; entire target refused |
| M-expressions-of-interest | already refused by assembly | unchanged |

EOI's existing refusal is `:universe-not-admitted` / `:locators`, for
`:change-authored-and-bound`, `:obligation-resolved-through-the-account`, and
`:premise-refused-before-work`. It is not a new fix-16 refusal.

Reproduce from this worktree:

```sh
clojure -M holes/labs/wm-contract/runs/fix-16-2026-09-21/source_census.clj /home/joe/code/futon2/resources/wm/cascade-sources 2
```

## Validation and existing failures

Environment: isolated `/home/joe/code/futon2-fix-16`, OpenJDK 21.0.11,
Clojure 1.11.1. No shared JVM loads, shared checkout writes, or WM clicks.
Static checks on all four changed/added Clojure files: clj-kondo 0 errors,
0 warnings (one pre-existing informational message); check-parens `OK`.

The final regression namespace on base main has **6 tests / 22 assertions /
15 failures / 0 errors**. On the implementation it has **0 failures/errors**.
An exact bad-case assertion from the retained `before.log` is:

```
FAIL in (recorded-no-op-is-refused)
expected: (= :no-new-wanted-token (:reason drop))
  actual: (not (= :no-new-wanted-token nil))
```

Every namespace below was run separately with
`clojure -M:test -m cognitect.test-runner -n <namespace>`:

| Namespace suffix (under futon2) | Tests/assertions | Failures/errors |
|---|---:|---:|
| report.new-wanted-token-admission-test | 6/22 | 0/0 |
| report.cascade-decision-test | 13/94 | 0/0 |
| report.wm01-bindings-test | 2/19 | 0/0 |
| aif.cascade-proposals-test | 6/36 | 0/0 |
| aif.h4-production-stage-test | 1/18 | 0/0 |
| aif.policy-precision-carry-test | 9/60 | 0/0 |
| aif.repair-proposals-test | 5/31 | 0/0 |
| aif.scoring-input-receipts-test | 3/44 | 0/0 |
| aif.token-belief-predecessor-test | 3/25 | 0/0 |
| report.cascade-habit-accumulation-test | 5/32 | 6/0, also on base |
| aif.token-belief-carry-test | 4/30 | 2/0, also on base |
| aif.uniform-run-record-test | 3/36 | 2/0, also on base |

**The wider checks are not all green.** These ten existing failures were
reproduced with the base `war_machine.clj` loaded into a fresh local test
process. No failing assertion was removed, suppressed, or changed:

- Habit accumulation still expects an empty policy to be selected/counted
  and compares pre-migration snapshots; the failing test stubs out
  `cascade-decision`. Its separate real-decision test passes.
- Carry's two failures compare historical complete selection-law snapshots.
  Its actual initialization, prospective-carry, and serialization checks pass.
- Uniform run-record expects the old `:computed-not-attached` F status and
  an old census verdict; current production uses `:not-supplied`. Both
  failures also occur on base.

The base verification uses `clojure -M:test -e` with the following forms
(substitute the namespace under investigation):

```clojure
(require '[clojure.java.shell :as sh] '[clojure.test :as t]
         'futon2.report.war-machine)
(let [r (sh/sh "git" "show" "60a139d8:scripts/futon2/report/war_machine.clj")]
  (assert (zero? (:exit r)))
  (load-string (:out r)))
(require 'futon2.report.new-wanted-token-admission-test)
(let [r (t/run-tests 'futon2.report.new-wanted-token-admission-test)]
  (shutdown-agents)
  (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))
```

Static commands:

```sh
clj-kondo --lint scripts/futon2/report/war_machine.clj test/futon2/report/new_wanted_token_admission_test.clj test/futon2/report/cascade_decision_test.clj holes/labs/wm-contract/runs/fix-16-2026-09-21/source_census.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- scripts/futon2/report/war_machine.clj test/futon2/report/new_wanted_token_admission_test.clj test/futon2/report/cascade_decision_test.clj holes/labs/wm-contract/runs/fix-16-2026-09-21/source_census.clj
```

Registry configs for the new regression and the updated joint-decision
namespace are retained beside this note. Warrants cover those namespaces;
they do not claim that the three failing namespaces passed.

## Committed registry evidence

Implementation commit: `187296bb`. Both registry runs returned `:warrant? true`,
`:execution/stable? true`, and `:postcheck {:status :matched}`.

- Admission: `test-registry-1bb33fba6d5f8460ed2cebf0f67bd1d79f8a95d14508150e30c86d112b87219b`; 6 tests, 22 assertions, zero failures/errors.
- Joint decision: `test-registry-99fdd47c26fb4dc8ba3969d5f6f8edb41a51f55ca810df6d6c3581497816ebcb`; 13 tests, 94 assertions, zero failures/errors.

The retained check configs use the implementation commit's complete changed-path
list. Each warrant covers its own test namespace and production dependency
closure; report artifacts and the census script are outside those test closures.
The census was separately executed and the script passed both static gates.
