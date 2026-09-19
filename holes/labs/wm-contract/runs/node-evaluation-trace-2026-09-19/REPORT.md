# EV-node-level-evaluation-trace

Implementation **c6e9dcaf**, merged to main at
**7bda60d0e3ed763b6e3020f91b1db5c67cb379a5**.
Author: codex-3. Independent review belongs to claude-4.

The ordinary cascade scorer and the bounded observation-model scorer now
retain actual per-state transition evaluations. The selector threads them
into its certificate, so the unchanged production run-record writer retains
them at:

```
[:decision :selection-certificate :node-evaluation-traces i]
  {:id <FULL candidate map>
   :horizon T
   :status :recorded
   :evaluations
   [{:tau 1
     :status :evaluated
     :incoming-belief {state mass ...}
     :model {:schema :wm/cascade-evaluation-model-v1
             :semantics :first-enabled-union-theta-v1
             :precedence [<effective patterns including theta/source> ...]}
     :states [{:state state :mass mass
               :guard-search [{:index j :pattern-id id
                               :guard-verdict boolean :applied? boolean} ...]
               :selected-index j-or-nil :pattern-id id-or-nil
               :status :evaluated
               :kernel-kind :pattern-kernel-or-identity
               :kernel {next-state conditional-mass ...}
               :mass-contribution {next-state weighted-mass ...}} ...]
     :outgoing-belief {next-state mass ...}} ...]}
```

The full candidate map equals the corresponding selection-certificate
candidate's `:id`, G-term policy id and posterior key. The test run has eight
distinct full maps across two targets, with only four nested C0/C1/etc labels.
No join is keyed by those local labels.

## Actual computation, not reconstructed logging

`cascade-model-manifest` now shares one evaluation core between the old
scalar rollout and `rollout-evaluation`. Guard search stops at the first
enabled pattern, retaining the actual inspected prefix. Patterns after that
point have no fabricated guard verdict. `pattern-kernel` is called once for
the selected pattern; no-enabled states use identity. Each contribution is
the incoming state's mass times the kernel probability and is the value
aggregated into the outgoing belief. Theta's documented default remains
explicit in the captured model.

The sparse scorer still evaluates successive rollout horizons from q0.
For each scored horizon it retains the terminal step of that actual rollout;
it does not run an extra rollout for logging. The bounded scorer retains its
actual one-step evaluation on each iteration. Both scorer certificates carry
`:node-evaluations`; `policy/select-action-cascades` copies them beside the
full candidate map. The existing writer already copies the whole selection
certificate, so no writer change was needed. Recording is automatic on these
paths, with no new opt-in switch.

A refused rollout retains its attempted prefix in `rollout-evaluation` and
stops; it does not invent evaluations for subsequent horizons. Existing
scorer refusals/early termination remain in force. An absent trace is marked
`:missing`, and the read-only validator rejects missing or incomplete
coverage. This packet does not change the existing run-validity policy.

## Record-only validation and negative controls

`futon2.aif.cascade-evaluation-trace/validate-record` uses only the record's
candidate/model values. It does not require the running evaluator, repository
state, a service, or a current model. The captured semantics version tells it
how to check guard search and theta/union kernels independently.

The test namespace constructs these breaks and asserts their detection:

| Mutation | Required detection |
|---|---|
| Replace outgoing belief without changing contributions | `:outgoing-contributions-mismatch` |
| Change a state's weighted contribution | `:state-contribution-mismatch` |
| Substitute a locally valid next step from a different incoming belief | `:broken-horizon-link` |
| Replace selected pattern id with an outsider | `:wrong-pattern-id` |
| Select a later enabled member instead of the first | `:not-first-enabled`, `:applied-kernel-mismatch` |
| Mark an unselected guard failure as applied | `:guard-search-mismatch` |
| Remove traces or replace full candidate map with C0 | `:candidate-join-mismatch` |
| Drop the last horizon step | `:horizon-coverage` |
| Alter captured theta relative to the candidate | `:model-candidate-mismatch` |

A call-count/order control compares actual guard and kernel calls to emitted
rows, detecting a second evaluation pass. Controlled branching beliefs
exercise theta=1/2, stay-put mass, different selected patterns per state,
and a no-enabled identity step. Typed bad-theta/missing-interpretation
refusals and the documented theta default remain covered.

The recorded WM-shaped family is the existing six-token tick-001 fixture at
horizon three. Tests cover ordinary identity-A scoring, independent nonzero
rates and the coupled model path. The existing bounded scorer's unique-local-id
admission rule is preserved: it is tested with one legal target family;
cross-target repeated labels are tested through the ordinary joint menu.
Recorded baseline G values and policy-level Q beliefs agree with the traced
outgoing beliefs.

## Registered run and gates

Warrant, registered by codex-3 and bound to **EV-node-level-evaluation-trace**:

**test-registry-22254da5f18d7556332acecb667e879d5771badf66ada7f3e051d10f47418307**

Registered root: `/home/joe/code/futon2`, main at merge `7bda60d0`.
Only `futon2.aif.cascade-evaluation-trace-test` ran:
**5 tests, 132 assertions, 0 failures/errors**, 5153 ms.
HTTP warrant check passed (`check.json`); `binding.edn` copies the actual
appended subject binding. The registration spec, output, log and loaded-source
closure are retained adjacent to this report.

`test-run-record.edn` is extracted unchanged from the registered test log.
The test creates it using the actual `full-loop-runner/persist-run-record!`
writer in a temporary directory, reads it back, validates all joins, and
deletes the temporary files. It is an **offline test replay**, not a WM click.
The writer's historical `selectorSeam` label is unchanged and confers no live
execution claim. The standalone checker also accepts this retained artifact
(`trace-validation.edn`).

All touched Clojure passed **clj-kondo 0 errors/0 warnings** and
**check-parens OK**. `git diff --check` passed. No Lean witness or checker
was modified; no wrapper under checks/ imports cascade-model-manifest.

`wm_run_validity.bb` was not edited. On the existing good record
`data/wm-runs/tick-run-record-2026-09-19-1789849189.edn`, its complete output is
byte-identical before and after: **VALID (3/5 ok)**, retaining the existing
C/F flags. Both outputs include successful checker selftests. A separate
noninterference check removes/adds the new trace field on the emitted test
record and compares the complete validity result for exact equality; it
passes (`validity-field-noninterference.txt`).

No WM click, actuation, serving-JVM reload, or manual edit under futon2/data
occurred. Tests used temporary habit/repair/trip locations and verified the
production repair/trip file inventories were unchanged.
