# WM-01-support-1: enforce support laws at the shared runtime boundary

From claude-3 to codex-2. **Author: codex-2. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-support-1.md` (sha256 `9322deabfbbf54a6b78cc0e312830aa0ebb37635993f010aeaa9b98370d83b2d`). **Read it first; it governs.** This packet adds the facts I checked; it does not widen the dispatch.

This is the follow-on to your WM-01-numeric-1 work (`ac821857`, `7f546b63`), which codex-28 accepted at source/test scope. Keep every numeric-1 behaviour and control unchanged.

## TODO clause and cascade

WM-01: sound shared probability/model carriers. Cascade K3 requires duplicate-free support and zero mass off support; K5 requires actual consumers to share the contract. The Lean repair `480a666ad2` (independently accepted) has `support_nodup`. The runtime boundary does not yet enforce it. This change fixes that one runtime/Lean mismatch. It does not complete WM-01.

## The gap (I reproduced it at futon2 HEAD `00915661`)

```clojure
(m/distribution-admission {:a 1} [:a :a])
;; => {:ok true, :support [:a :a], :exact-total 1, :exactly-normalized? true, :admission :exact, ...}
```

`distribution-admission` compares `(set (keys row))` with `(set support)`, so it ignores multiplicity. It also never checks the support's shape: `nil`, `[]` and a list reach set equality directly. A Lean-style sum over `[:a :a]` would count the mass twice.

## The one behaviour to build

Make `distribution-admission` enforce the same support contract as the common model:
- the support is a nonempty **vector** of **distinct** identities;
- the row's keys cover the support exactly;
- masses are finite, supported and nonnegative, under numeric-1's unchanged rule.

Keep the support order and the masses. Do not deduplicate, sort, drop keys, normalize or fall back. Return the existing typed refusals (`:missing-support`, `:duplicate-support`, `:distribution-support-mismatch`) as data. Never throw.

The model validator, the belief reader and `predicted-state-plan` must share this one check. Don't build a second validator hierarchy.

## What I checked in the source

- **The common-model contract already exists.** `machine_model.clj:31-33`:

  ```clojure
  (defn- support! [xs path]
    (demand! (and (vector? xs) (seq xs)) :missing-support path)
    (demand! (= (count xs) (count (set xs))) :duplicate-support path))
  ```

  - It throws through `demand!`.
  - `validate` uses it for `:state-support` (line 194) and for actions, policies and hypotheses.
  - `distribution-admission` returns refusal data instead.
  - Sharing it probably means a non-throwing predicate that `support!` and `distribution-admission` both call. Your choice, but one definition, not two copies.
- **Model path.** `kernel!` calls `distribution!` with outputs that `support!` has already validated, so a validated model can't reach the boundary with duplicate support. The gap is at the public function and at consumers handed a hand-built kernel or context.
- **Belief reader.** `machine_belief.clj` refuses `:state-support-order-mismatch` unless the context's `:state-support` equals the canonical seven-status vector, before admission runs. So a duplicate support is already refused there, with that kind. Test the reader entrypoint anyway, and keep that existing refusal where it applies.
- **Predictor.** `predicted-state-plan` takes `support (:state-support kernel)`.
  - Its guard `(not= belief/status-set (set support))` also ignores multiplicity: a kernel support listing all seven statuses plus a repeat passes it.
  - Its `failure` helper maps every refusal except `:unsupported-numeric-type` to `:invalid-mass`. That would report a duplicate support as `:invalid-mass`.
  - An existing numeric-1 test (`predictor-shared-boundary-rejects-initial-and-produced-bad-rows`) expects `:invalid-mass` for an extra row key.
  - Declare the refusal kinds you give at the predictor. If any existing expectation changes, list it in the receipt as a behaviour change, with the reason, and raise it in the bellback.
- **Transitions.** A malformed kernel support must be refused before `transition/apply-belief` runs. Prove it with a test that records whether `apply-belief` was called, for example a `with-redefs` spy.
- **F3 stays as it is.** `policy-refusal` (`machine_predictive.clj:16-21`) already requires exactly one posterior and checks its key against the policy's `:entity/id`. Do not change the first-vals lookup.

## Allowed edits

- `futon2/src/futon2/aif/machine_model.clj`
- `futon2/src/futon2/aif/machine_belief.clj`
- `futon2/src/futon2/aif/machine_predictive.clj`
- `test/futon2/aif/machine_model_test.clj`, `machine_belief_test.clj` and `machine_predictive_test.clj`
- Receipts under `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-support-1/`

Change only the files this boundary needs. Not allowed: held P1 work, migrating other consumers, transition arithmetic, Lean sources, model-law changes, runtime reload, clicks. If you hit a scope conflict, stop and report it before acting.

## Acceptance controls (from the dispatch)

1. The reproduced duplicate support refuses with `:duplicate-support`. Valid singleton and seven-status rows still pass.
2. Nil, empty and non-vector support (a list, a set) refuse per the common-model contract, with no exception. Missing and extra row keys refuse. Permuting a distinct support keeps that order and the same named masses; permuting values is distinguishable.
3. Exercise the public `distribution-admission`, the model validator, and the real belief-reader and `predicted-state-plan` entrypoints.
   - A malformed kernel support fails before `apply-belief` is called.
   - Include a valid, ordinary prediction control, so that refusing every input cannot pass.
4. Every exact, float and decimal admission and every numeric-1 control stays as it is.

## Gates: run them, keep commands, exit statuses and source hashes, and don't pipe gate output

```sh
cd /home/joe/code/futon2
clj-kondo --lint src/futon2/aif/machine_model.clj src/futon2/aif/machine_belief.clj src/futon2/aif/machine_predictive.clj \
  test/futon2/aif/machine_model_test.clj test/futon2/aif/machine_belief_test.clj test/futon2/aif/machine_predictive_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- <the same six files>
clojure -X:test :nses '[futon2.aif.machine-model-test futon2.aif.machine-belief-test futon2.aif.machine-predictive-test]'
clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-q-risk-test futon2.aif.categorical-ambiguity-test futon2.aif.machine-parameters-test futon2.aif.work-target-tick-test futon2.aif.efe-machine-q-test futon2.aif.trace-test]'
```

Record the baseline results at HEAD before you change anything; the numeric-1 baseline was 19 tests / 375 assertions and 87 / 493. futon2 is a shared worktree: stage explicit paths only, never `commit -a`, never amend, and check `git log -1` before committing.

## Receipt: `wm-01-support-1/RECEIPT.md`

It should contain:
- the source sha256s before and after;
- the support contract as implemented, with where each consumer calls it;
- before/after cases, including the reproduced one;
- refusal kinds per entrypoint, and any changed expectations;
- the gate commands and exit statuses;
- limits.

State that exact normalization concerns the numeric total only, not empirical adequacy.

## Stop and bellback

Stop once the change, tests and receipt are committed. **Bell claude-3 back** with:
- the commit shas;
- the contract as implemented;
- before/after evidence;
- gate exit statuses;
- behaviour changes;
- unresolved findings.

Anything you discover beyond this scope is a question in the bellback, not a task.
