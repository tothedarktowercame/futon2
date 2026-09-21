# Fix-7: policy odds and action neutralization diagnostics

Codex-11; branch `fix/narrative-7`; base main `d618a33d`.
Owner ruling: compare the top two acting policies separately from the action
marginal. No runner edits, clicks, reloads, or shared-checkout changes.

`policy/select-action-cascades` now adds `:policy-comparison` and
`:action-comparison` under `:selection-law`. The existing posterior, marginal,
chosen action, and selected representative are untouched. The helper runs
after the actual choice. Trace persistence retains these fields already.

The policy record compares the top two positive-mass acting policies, ordered
by posterior and then printed identity. It carries winner/runner identities,
F values and statuses, and `:contributions {:habit :free-energy :G :total}`.
The contributions are log E difference, minus consumed F difference, and
minus G difference divided by beta. `:decided-by` names the largest positive
contribution; equal positive contributions use the recorded order
`[:habit :free-energy :G]`. Zero total is `:tie-break`. A singleton is
`:no-competing-policy`, with no fabricated numerical margin.

The optional selector option, declared alongside `:beta`, is:

```clojure
{:beta 1 :near-tie-threshold {:value 0.01 :status :declared}}
```

The declaration is retained beside beta in the selection law. `:near-tie?`
tests `abs(total) <= value`. There is **no default**: existing production
callers that supply only beta record `:threshold-undeclared`. A malformed
declaration records `:threshold-invalid`; diagnostics do not become new
selection gates. This patch does not authorize or install a live threshold.

The action record retains the actual winner and marginal mass, the next
positive-mass action (or `:no-competing-action`), and three `:flips` entries.
Each calls the existing `selection-posterior` and `bayes-choice`, using the
same candidates and action mapping. Finite habit is made uniform, consumed F
is set to zero, or finite G is made common. Zero-support F and non-finite G
exclusions remain excluded: neutralizing a finite scoring term must not turn
a support refusal into an admissible policy. This is recorded as
`:neutralization-support :preserve-exclusions`. Each flip records the new
winner/mass. `:decided-by` is the set of flipping terms, `:robust` if empty,
or `:tie-break` when the actual marginal winners tie exactly.

F semantics are the existing selector's: legacy computed-but-unattached
infinite F consumes neutral zero and retains `:computed-not-attached` plus
the raw computed receipt; missing prefix F stays nil/`:not-supplied` and
contributes no term; zero-support prefix F excludes; attached non-finite F
still refuses. There is no new F fallback.

## Recorded results

Fixtures under `test/fixtures/narrative-discrimination/` are projections of
the two named run records: their exact candidate maps, recorded selection
law, and the `:joint-selection` habit-read snapshot. Tests write that snapshot
to a temporary file and use the **real habit-store reader and real selector**,
without stubs. Both posterior maps and action-marginal maps compare exactly
with the recorded maps. Selected targets are also pinned.

| Run | Acting-policy result | Action result |
|---|---|---|
| 1789964661 | AIF vs F11; G contribution 443/350694 ≈ 0.001263209522; `:G`; near tie at declared 0.01 | `#{:G}`; G neutralization ties all three actions and the declared action-name tie-break picks F11 (`:apparatus/done-is-observed-running`) |
| 1789952479 | EOI C1 vs C2; G contribution exactly 0.0; habit contribution ln(5); `:habit` | `:robust`; none of habit/F/G alone flips the EOI action |

The latter result is not habit deciding the action: with E neutralized,
the two EOI policies still pool into the same first-pattern action. Its new
mass is 0.08357281417382964, versus the single external-F2 policy's mass.
With G neutralized it retains approximately 0.2 mass. Originally its mass
was 0.20050142011022512 and F2's was 0.0333124408287407. Habit distinguishes
EOI C1/C2 as policies but is not necessary for this pooled action to win.

Additional tests cover a 2-nat separated case, exact ties, no declared
threshold, invalid threshold, singleton/pooled-single actions, F signs at
beta=2, missing prefix F, and preserving zero-support exclusions.

## Validation

Environment: isolated `/home/joe/code/futon2-fix-7`, OpenJDK 21.0.11,
Clojure 1.11.1. One test namespace at a time, own process.

The final regression tests were also run with **both selector namespaces
loaded from the base commit** in a fresh process (never a shared JVM):

```clojure
(require '[clojure.java.shell :as sh] '[clojure.test :as t]
         'futon2.aif.policy)
(doseq [path ["src/futon2/aif/cascade_selection.clj" "src/futon2/aif/policy.clj"]]
  (let [r (sh/sh "git" "show" (str "d618a33d:" path))]
    (assert (zero? (:exit r)))
    (load-string (:out r))))
(require 'futon2.aif.selection-discrimination-test)
(t/run-tests 'futon2.aif.selection-discrimination-test)
```

Run with `clojure -M:test -e '<forms above>'`; the harness exits 1 on failures.
The final red run reports **5 tests, 51 assertions, 41 failures, 0 errors**;
the implementation reports **5 tests, 51 assertions, 0 failures, 0 errors**.
An exact failing assertion for the named bad case is:

```
FAIL in (recorded-runs)
1789964661
expected: (true? (:near-tie? p))
  actual: (not (true? nil))
```

Fresh commands for the implementation:

```sh
clj-kondo --lint src/futon2/aif/cascade_selection.clj src/futon2/aif/policy.clj test/futon2/aif/selection_discrimination_test.clj test/futon2/aif/selection_certificate_test.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- src/futon2/aif/cascade_selection.clj src/futon2/aif/policy.clj test/futon2/aif/selection_discrimination_test.clj test/futon2/aif/selection_certificate_test.clj
```

Both static gates passed: 0 errors/0 warnings, `OK`. For each namespace below,
the exact command was `clojure -M:test -m cognitect.test-runner -n <namespace>`.

| Namespace | Tests | Assertions | Failures/errors |
|---|---:|---:|---|
| futon2.aif.selection-discrimination-test | 5 | 51 | 0/0 |
| futon2.aif.cascade-selection-test | 5 | 25 | 0/0 |
| futon2.aif.policy-test | 8 | 15 | 0/0 |
| futon2.aif.selection-certificate-test | 9 | 109 | 0/0 |
| futon2.aif.policy-prefix-evidence-test | 8 | 43 | 0/0 |
| futon2.aif.policy-precision-carry-test | 9 | 60 | 0/0 |
| futon2.aif.trace-test | 40 | 131 | 0/0 |

The certificate namespace includes its existing Lean positive/negative
checker controls, run sequentially in the mathlib4 source checkout. Its
historical byte-comparison fixture predates main's gamma/tau/tau-source
metadata; the test now asserts those fields explicitly and projects away
only this patch's three additive diagnostic fields. Every original decision
field remains compared, including posterior values and selection.

Registry config: [registry.edn](registry.edn). Registry results are recorded
after committing the implementation, as required by the committed-scope gate.

Implementation commit: `b701baa3`. From `/home/joe/code/futon3c`:

```sh
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-fix-7/holes/labs/wm-contract/runs/fix-7-2026-09-21/registry.edn
```

Issued warrant (`registry-result.edn`): `:warrant? true`,
`:execution/stable? true`, `:postcheck {:status :matched}`;
5 tests / 51 assertions / 0 failures / 0 errors / exit 0, 2889 ms.
Run id `14b7083d-8af1-4c52-bdef-97361dc2c751`;
evidence id `test-registry-7c52fcb35134fc5c4f08e3f56635c069d5cef45ab8aaebaf94954b5a2a759219`.
The warrant covers the new recorded-run regression namespace and its fixtures;
the other six namespace runs are separate fresh logs, not claimed as part of
that warrant.

The post-commit read-only check also returned `:warrant? true`, chain length
2, using `clojure -M -m futon3c.test-registry check` with
[registry-check.edn](registry-check.edn). That config supplies the issued
`:entry-id` and the full changed-path list; the run config alone does not
supply the entry id required by `check`. The check output retains its explicit
`:outside-closure` list, including the separately-tested certificate namespace
and documentation artifacts.
