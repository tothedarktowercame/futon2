# Narrative fix-4: checkpoint candidate ranking

Branch: `fix/narrative-4`, based on `main` at `4354668b` (no `master` ref).
Only this worktree was edited. No live loading, clicks, or merges.

`full-loop-runner/ranked-candidates` joins certificate quantities by the full
candidate map, not the repeated `:C1` label. It records target, cascade id, G,
habit, F, F status, posterior, action and rank; sorts all candidates descending
by posterior; and uses the declared first-action-name tie rule. Equal first
actions have a deterministic printed-candidate secondary display order. This
ranks policies, not the action marginal that determines enaction. Missing
certificate quantities stay nil with `:f-status :not-recorded`; an unknown
rule for a tied posterior is refused rather than invented.

The diagnostic now explicitly consumes posterior. Its epsilon (1e-6), top-k
(5) and finite/distinctness predicate are unchanged. It receives the sorted
ranking, so top-k now means the five highest posterior masses. Telemetry keys
are `:posterior-values`, `:valid-posterior-count`, `:distinct-posterior`.
The reference run still yields `:passes? true`; fix-7 owns near-tie attribution.

## Consumers

Search covered futon2 `src/`, `scripts/`, `test/`, futon3c `src/`, and p4ng Python
files for `:G-efe`, `:controller-score`, and `:ranked-candidates`.

- `full_loop_cli.clj`: `candidate-label` reads G-efe; retained as an honest alias
  of G. `selected-rank`, `feature-acceptance`, and `inspection-lines` keep their
  action/rank/vector inputs. The pure regression exercises the label function;
  the CLI namespace passes unchanged.
- `full_loop_runner.clj`: the checkpoint and Morning Brief selection-review
  carry the complete ranking; the diagnostic caller now receives posterior
  explicitly. Ranked entries no longer contain controller-score. The decision's
  separate controller-score fields retain their existing meaning.
- `full_loop_runner_test.clj`: two direct old-score diagnostic tests moved and
  updated in the new pure namespace. No integration guard was mocked.
- `native_currency_discrimination_test.clj`: the EFE field now goes through the
  real policy selector and ranking builder before posterior discrimination.
- `selection_always_test.clj` inspects count/verdict only; no field changes.
  `full_loop_cohort_test.clj` requires the ranking key only.
- `cascade_prior.clj` and futon3c `dynamic_queries`, `dynamic_queries_rung4`,
  `strategic_cascade`, `strategic_outcomes` use separately produced rankings;
  they do not consume this selection checkpoint. No p4ng Python matches.

## Regression evidence

The 5.8 KB fixture projects only needed decision paths from the recorded run;
full candidate keys retain kind/id/target/precedence, including the real pattern
maps. All three IDs remain :C1. The test reverses posterior insertion order.

Before/after was established without running an opportunity: temporarily bind
`runner/ranked-candidates` to the exact old inline builder plus checkpoint
truncation, then run `futon2.aif.ranked-candidates-test`. Old implementation:

```clojure
(fn [judgement]
  (vec (take 10
             (map-indexed
              (fn [i [candidate p]]
                {:rank (inc i) :action candidate
                 :controller-score p :G-efe p})
              (get-in judgement [:decision :selection-law :posterior])))))
```

`old-builder.log`: 4 tests / 34 assertions, 20 failures, 0 errors. In particular:

```
expected: (= (mapv :g cs) (mapv :G rows))
actual: (not (= [15.170069253169533 15.171332462691225 15.171375235022659] [nil nil nil]))
```

Additional assertions reject posterior under G-efe and controller-score,
check exact posterior values and F status, and retain 12 tied candidates in
first-action order. Later tests also cover absent certificates and unknown tie
rules.

## Validation

Environment: OpenJDK 21.0.11, Clojure CLI 1.12.5.1664, project Clojure 1.11.1.
Working directory: `/home/joe/code/futon2-narrative-4`.

Commands:

```
clj-kondo --lint src/futon2/aif/full_loop_runner.clj test/futon2/aif/ranked_candidates_test.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/native_currency_discrimination_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/full_loop_runner.clj test/futon2/aif/ranked_candidates_test.clj test/futon2/aif/full_loop_runner_test.clj test/futon2/aif/native_currency_discrimination_test.clj test/fixtures/narrative-trace/ranking-1789964661.edn
clojure -M:test -m cognitect.test-runner -n futon2.aif.ranked-candidates-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.native-currency-discrimination-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-cli-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test
```

- Kondo: 0 errors, 0 warnings (one pre-existing informational str notice).
- Parens: OK. Git diff whitespace check: clean.
- Ranking: 6 tests / 36 assertions, 0 failures, 0 errors (`ranking.log`).
- Native currency: 3 / 10, 0 failures, 0 errors (`native.log`).
- CLI: 12 / 58, 0 failures, 0 errors (`cli.log`).
- Runner integration: 179 / 562, 0 failures, **83 errors**, all exactly
  `Serving runner source drifts from the canonical checkout`. Fresh output is
  `/tmp/narrative-4-runner.log`. The guard is unchanged and was not bypassed.
  This is not a passing integration warrant; canonical revalidation is needed
  after owner review/merge, as discussed in the handoff.
