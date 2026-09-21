# H4 staged observed-prefix import — codex-1

Authority: claude-12 scope ruling invoke-1789966199358-22918-992d2625.
Implementation: acc4f3c4640b4092710cceaf9d88f50acaeb0663.

## Scope and outcome

Staged implementation, NOT H4 closure. No click, shared-JVM reload, admission,
calibration, or real-store mutation was performed. The Lean hole is untouched.
Production nonempty history remains pending on
`:d-conditioning-consumption-and-policy-prefix-admission`. This includes the
candidate/execution/observation join; the existing not-established boundary
was not changed.

Both production selection call sites now consume missing-prefix receipts by
default and skip the old future-rollout-versus-current-evidence F calculation.
The receipt's F is absent, not measured zero. Its status is `:not-supplied`,
reason `:no-admitted-policy-prefix`, and it retains the conditioning reason.
For the actual temporary-store production replay, conditioning was `:not-run`
because carry admission refused; the dependency also covers `:not-wired` when
task admission succeeds. The selector contributes no F term for this case.
Historical S4 helpers remain for replay/tests, explicitly documented as retired
from production; their environment flag does not control the new route.

The separate synthetic prefix evaluator conditions sequentially from each
pre-observation prediction, sums conditional surprises, and retains transitions,
the fixed declared model, observations, contexts, predictions and posteriors.
It accepts only declared `:per-step-redraw` semantics. Its result explicitly
says `:synthetic-prefix-arithmetic-only`; production cannot use it as admission.

## Measured controls

- Evidence 1/4 and 3/4 gives F 1.3862943611198906 and 0.2876820724517809;
  the full selector gives probabilities 0.24999999999999994 and
  0.7499999999999999 with equal E/G.
- Equal F cancels. A separate beta=2, unequal-G control distinguishes
  unscaled F from incorrectly precision-scaling both F and G.
- Occurrence mismatch, horizon mismatch, invalid transition normalization,
  unsupported persistent-z declaration, and duplicate occurrence all refuse.
- A second distinct observation after the first perfect-observer update has
  probability 1: total F remains ln4, rather than counting ln4 twice.
- Zero evidence gives typed zero support, no fabricated numeric F; a supported
  alternative gets probability 1. All-impossible policies give typed refusal.
- Exact probability 10^-400 underflows when cast to double but remains positive
  evidence, with finite F approximately 921.0340371976183.
- The production-shaped scorer/selector test never invokes the old F producer.
- The actual `war-machine/cascade-decision` function ran against temporary
  habit/task stores and the hermetic repair/trip fixture: every candidate had
  missing-prefix provenance. This is a test of the production function, not a
  live tick or evidence that admitted histories exist. Production store file
  sets remained unchanged under the fixture's two assertions.

## Additional defect found and corrected

The existing EFE certificate test expected the selector to choose an empty
safe cascade when the only acting cascade had zero posterior mass. Actual
committed code selected the zero-support offender. Re-running that namespace
with HEAD versions of cascade_selection, policy and efe loaded in a standalone
test JVM reproduced the same failure (16 tests, 131 assertions, one failure).

The selector now abstains when no acting candidate has positive support, and
cannot pick a zero-support candidate merely because it shares the winning
first action with a supported candidate. The old test now asserts abstention,
preserving its G checks; new tests construct both failure cases. This follows
the existing empty-is-not-an-action rule, not a new authority for no-ops.

## Gates and execution

`clj-kondo --lint` over all eleven touched Clojure files: zero errors/warnings
(one existing informational `str` finding in war_machine).
`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
'(arxana-check-parens-cli)' -- <the eleven touched Clojure files>`: OK.
`git diff --check`: clean before commit.

Each following namespace ran separately with
`clojure -M:test -m cognitect.test-runner -n <namespace>`:

| Namespace | Tests | Assertions | Failures/errors |
|---|---:|---:|---:|
| futon2.aif.policy-prefix-evidence-test | 8 | 43 | 0/0 |
| futon2.aif.h4-production-stage-test | 1 | 18 | 0/0 |
| futon2.aif.efe-certificate-test | 16 | 131 | 0/0 |
| futon2.aif.cascade-selection-test | 5 | 25 | 0/0 |
| futon2.aif.policy-test | 8 | 15 | 0/0 |
| futon2.aif.observation-model-route-test | 7 | 132 | 0/0 |
| futon2.aif.matched-observation-evidence-test | 4 | 52 | 0/0 |

Two postcommit registry runs, from futon3c:
`clojure -M -m futon3c.test-registry.validation register
/home/joe/code/futon2/holes/labs/wm-contract/runs/h4-staged-prefix-2026-09-21/registry.edn`

Both returned warrant=true, 8 tests / 43 assertions / zero failures/errors,
bound to `H4/staged-observed-policy-prefix`:

1. test-registry-2de0bfcd3d4f12fec6a0bb04593c14f8435e208c93d02061b162a3b8afa438ec
2. test-registry-62e8aa7dfebbc68d579dac86177260064348a62e40ea61e18edc2daa83d093e4

## Production warrant blocked — do not treat green assertions as a warrant

The same registry command with `production-registry.edn` returned 1 test /
18 assertions / zero failures/errors, **warrant=false**. Its record is
test-registry-a048c1a4a567ec923b0146ad55c94770caf5cd08502b4955d68e7ea84e81ea74.
The retained postcheck, fetched from the evidence endpoint, says:

```
{:reason :scope-not-committed
 :details {:stage :load-closure :count 2 :reasons {:dirty 2}
           :paths ["src/futon2/aif/cascade_sources.clj"
                   "src/futon2/aif/policy_precision.clj"]
           :next-action :commit-before-registering}}
```

Those files belong to concurrent lanes. They were not staged, reverted or
committed here. Other lanes subsequently also edited production files in this
commit; the successful warrants describe their captured bytes, not an assertion
that the ongoing shared checkout remains unchanged. Rerun the production
registration twice after the owners land their work, and revalidate arithmetic
warrants against the resulting checkout. This is a remaining acceptance gate.

Logs also contain JVM worker-thread allocation warnings; the test process
completed with exit 0. No registry condition was weakened to obtain a warrant.
