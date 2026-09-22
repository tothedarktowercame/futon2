# Generic ticket front placement — slice 1

Base `d8351fc0`; branch `fix/ticket-queue-front`. Owner contract: `invoke-1790040390721-23115-872b69b6`. This replaces the superseded repair-admission assignment. Only ordinary ticket queue ordering is implemented here; creation, dispatch and closure slices remain separate. No click, serving-JVM evaluation or live model/task-store mutation was performed.

## Declaration

The carrier is the classpath resource `resources/wm/ticket-queue.edn`. Its complete version-1 schema is:

```clojure
{:schema :wm/ticket-queue-v1
 :placement :front
 :order [:inserted-at :ticket]
 :within-stratum :cascade-selection-posterior
 :entries [{:ticket "T-documentation" :inserted-at "2026-09-22T01:00:00Z"}]}
```

The committed resource has `:entries []`: this landing places no actual ticket at the front. No fallback declaration is invented if the file is missing/malformed. Entries require unique ordinary `T-` identities and parseable Instants; extra fields, duplicate identities, unknown rule versions and invalid dates refuse `:invalid-ticket-queue`. Insertion times compare as Instants (including differing timezone offsets), then by ticket id ascending. One ticket is one stratum. Policy action-marginal selection, including its existing action-name tie-break, decides within that ticket.

A creator can insert an entry into this declaration in its own later authorized transaction; this slice adds no write API, store access or ticket creation. The queue consumer reads only ticket identity, insertion time and ordinary policy support. There is no origin field, native finding key or special prefix interpretation. The existing report path still has its older proposal machinery; removing that is the owner's later ordinary-T-path slice, not an additional dependency of this queue implementation.

## Selection and evidence

`judge` reads one declaration snapshot, includes its identities among ordinary assembly targets, and passes that same declaration through `cascade-decision` to `policy/select-action-cascades`. Direct callers of `cascade-decision` also read the resource, or accept the explicit `:ticket-queue` test/input seam. Missing source admission keeps its ordinary refusal; candidate declines are retained in the existing output and copied into the queue receipt. A missing/inadmissible front entry never blocks later admitted candidates. Infinite G / zero-support F retains the existing exclusion and is recorded as `:no-policy-support` when no ordinary admission refusal applies.

All admitted candidates are scored as before. The full-family candidate quantities, posterior and summed marginal are unchanged by queue order. The earliest supported ticket supplies the eligible stratum; the **same** `cascade-selection/selection-posterior` and `bayes-choice` compute its conditional choice. Finite-score underflow in the full family must not erase eligibility: the conditional normalization avoids that; the full-family mass remains honestly zero when represented as zero. Infinite risk/zero-support F is not rescued.

For a nonempty declaration, both `:selection-certificate :ticket-queue` and `:selection-law :ticket-queue` carry:

- `:schema :wm/ticket-queue-selection-v1`, the full `:declaration`, sorted `:entries` with admission/refusal evidence, `:eligible-targets`, and `:status` (`:front-stratum` or `:no-admitted-front-entry`).
- `:unrestricted-choice`, `:choice` (conditional when a front stratum exists), and `:stratum-posterior` (nil without a front stratum).
- `:decided-by :ticket-queue` when the stratum constrains eligibility, otherwise `:unrestricted-policy`. It identifies the applied domain rule even if unrestricted and constrained choices agree; both choices make that case inspectable.

Existing `:policy-comparison` / `:action-comparison` continue to explain unrestricted posterior odds/counterfactuals and receive `:selection-domain :unrestricted` only for nonempty queue declarations. They are not relabelled as explanations of a different action. `:chosen-action-mass` remains the selected action's **full-family** marginal, not its conditional mass; the latter is `:ticket-queue :choice :mass`. Existing law id remains `:cascade-selection-posterior`, now with the declared eligibility receipt. The conditional rule is not a claim of unrestricted Bayes optimality or a new G term.

The decision gate recomputes the earliest supported ticket from the declaration and recorded candidates, recomputes conditional policy probabilities, checks both choices, requires the same receipt in the certificate and law, and verifies that the enacted action belongs to the declared stratum. It still checks candidate receipts, full posterior normalization, full marginal mass and Bayes optimality within the eligible domain. A forged eligible target is refused. Zero represented full-family mass is admitted only with the checked positive conditional choice.

All-refused families remain ordinary abstentions and retain the queue/refusals in the certificate with nil choices and `:decided-by :abstention`. An empty declaration adds **no fields** to historical decisions. The complete serialized selector output (including the certificate) is checked against bytes captured from unmodified `d8351fc0`; no diagnostic fields are projected away.

The new `futon2.aif.ticket-queue` namespace registers load identity and is included in the canonical required-source map. No full-loop runner changes are needed to extract the choice: the selected value is still an ordinary cascade candidate. Generic ticket repository/dispatch fixes identified in the discovery remain later work; this packet does not claim an executable end-to-end ticket click.

## Tests and baseline failure

`futon2.aif.ticket-queue-test` uses temporary absent habit files, fixed input quantities and clocks, ordinary non-origin-specific ticket names, the real selector/gate, and real ordinary assembly/report decision for the refused-front case. It covers FIFO in either input order, equal-time id order, equivalent offset timestamps, policy choice within a stratum, numerical underflow, malformed declarations, forged gate evidence, no-origin input, and byte identity without queue entries.

Copied the exact new test namespace into a detached `d8351fc0` worktree. No producer source was changed there. Baseline: **8 tests / 43 assertions, 28 failures, 0 errors**. For example:

```text
expected: (= "T-documentation" (get-in front [:action :target]))
actual: (not (= "T-documentation" "M-main"))

expected: (= "T-a" (get-in (select rows opts (apply queue entries)) [:action :target]))
actual: (not (= "T-a" "T-b"))
```

The baseline capture command was run in that detached worktree before implementation:

```sh
clojure -M:test -e '(require (quote futon2.aif.ticket-queue-test)) (futon2.aif.ticket-queue-test/with-inputs (fn [opts] (spit "test/fixtures/ticket-queue/before.edn" (pr-str (pr-str (futon2.aif.policy/select-action-cascades futon2.aif.ticket-queue-test/roster opts))))))'
```

The EDN file contains the serialized decision string itself. Baseline and current tests read exactly those bytes; no store timestamps or generated paths enter this decision.

## Validation

Commands ran in `/home/joe/code/futon2-ticket-queue-front`, standalone CLI JVMs, one namespace per invocation:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.ticket-queue-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.policy-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.decision-gate-test
clojure -M:test -m cognitect.test-runner -n futon2.report.cascade-decision-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.selection-discrimination-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.selection-certificate-test -e slow
```

Results respectively: 8/43, 8/15, 16/408, 13/94, 5/51, 3/12, 8/108 tests/assertions, all passing. The certificate namespace's one slow Lean compiler test was explicitly excluded; no Lean source/math changed. No full-loop runner test was added or run: no runner source changed, and the actual report-to-selector-to-gate integration is tested in this namespace. Owner canonical click validation remains separate from this slice.

`clj-kondo` on all six changed/new Clojure files: 0 errors, 0 warnings (one pre-existing informational `str` message in war_machine). `check-parens` on those files and the queue declaration: OK. `git diff --check` and staged equivalent: clean. Raw outputs are retained alongside this note. A scoped registry warrant will be retained in the follow-up evidence commit; registry evidence is the only requested evidence-store write, never a live task/model store mutation.
