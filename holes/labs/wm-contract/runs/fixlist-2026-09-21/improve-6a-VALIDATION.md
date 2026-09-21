# improve-6a — selection-expectation surprise receipts

Branch `fix/narrative-improve-6a`, base `fddf7881`. D1–D4 read before implementation.

The close path writes a vector of `:wm/surprise-v1` records to
`<attempt>/retained/surprises.edn`, including the empty vector when no observed
mismatch qualifies. It admits that file's digest into the existing close
manifest, and carries `:surprise-ids` in the durable close judgment and result.
The closed narrative prints the IDs. The file does not enter the attempt's
checkpoint file set or the separately enumerated `evidence/` directory.

The expectation is the existing selection-frozen prediction under
`:positive-marginal-support`: positive probability expects observed=true,
zero probability expects observed=false. This is the declared support rule,
not a newly chosen probability or G threshold. Missing observations, matching
positive predictions, neither, repair findings and other incidents emit none.
Missing declaration time or occurrence, and known reversed/equal clocks, cannot
establish the expectation and emit none.

Expectation identity hashes the frozen structured prediction plus token.
Surprise identity hashes occurrence + token + expectation digest using
`action_identity/digest` (canonical v2), never rendered narrative text.
Map ordering and dynamic printer bindings do not change it. A changed model-id
changes the expectation and surprise IDs. No observation time or generated
prose is used to salt the identity.

`:predicted-not-observed` for a declared produced token is `:B-effect`, explicitly
`:causal-attribution :not-established`. A predicted state token not declared as
an effect is `:D-prediction`; an unexpected observed token is `:D-or-external`,
reason `:observed-outside-predicted-support-cause-unattributed`. The source
comparison and evidence remain available; a checkbox mismatch does not prove
pattern causality. Every revision field starts `{:status :none-yet}`.

Time provenance: `:declared-at` is the durable selection checkpoint's
`:recorded-at`. New D-task claims record `:observed-at` immediately after the
artifact measurement pass, not at build completion or receipt rendering. The
historical fixture has no exact measurement timestamp: its surprise keeps
`{:status :not-recorded}` there, with the post-build artifact-observation
placement. The historical ordering comes from the runner's selection-before-
post-build measurement lifecycle; no exact old sample clock is invented.
The reference selection timestamp read from `002-selection.edn` is
`2026-09-21T04:26:36.986447255Z`. This does not upgrade the legacy occurrence to
an admitted learning trial or create a parameter update.

The reference fixture retains real 1789964661 action, declarations, initial D,
and measurements. The real rollout/comparator produces exactly one surprise:
AIF updater `:hole/h6378c65a4012`. Scores/posteriors before and after receipt
computation are byte-identical; the runner fixture also preserves the complete
selection-law bytes. Production theta, Q, G, policy selection and habit code
are unchanged. New namespace `futon2.aif.surprise` registers its load-time identity
and joins the required source census.

The FIXLIST now declares the later-revision trailer convention:

```text
Surprise: <id>
```

No trailer scanner or learning-event classification was built.

## Validation

Outputs are adjacent in `improve-6a/`. Each command ran in its own worktree JVM:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.surprise-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.d-predecessor-task-authority-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.run-narrative-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.attempt-learning-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test
```

Results: surprise **2/21**, D-task **8/43**, narrative **26/122**, load identity
**3/12**, attempt learning **3/33**, all zero failures/errors.
Runner namespace: **188 tests / 591 assertions, 0 failures / 88 errors**, all
canonical runner-source drift. The guard remains enabled. The newly added
retention helper was then executed independently (1 test / 9 assertions,
zero failures/errors), exercising actual artifact observations, retention,
manifest admission, and file SHA256:

```sh
clojure -M:test -e '(require (quote futon2.aif.full-loop-runner-test)) (binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)] (clojure.test/test-vars [(ns-resolve (quote futon2.aif.full-loop-runner-test) (quote surprise-retention-enters-the-real-close-manifest))]) (prn @clojure.test/*report-counters*) (System/exit (if (zero? (+ (:fail @clojure.test/*report-counters*) (:error @clojure.test/*report-counters*))) 0 1)))'
```

The updated grounded `feature-card-close-retains-learning-receipt-without-changing-selection`
test reads `007-closed.edn`, checks one surprise, its IDs and manifest entry,
unchanged selection-law bytes, and successful `closed-execution`. **Owner must
run this on main after merge**, as requested; this worktree cannot certify it.
The helper does not bypass the runner guard; it tests the retention API directly.

Before test: copied only the new surprise test to a detached worktree at
`fddf7881`, leaving production unchanged. It ran **2 tests / 20 assertions,
12 failures / 0 errors**. Example:

```text
The retained updater mismatch must produce exactly one surprise
expected: (= 1 (count rows))
  actual: (not (= 1 0))
```

The final test adds a matching-positive/no-surprise control (21 assertions).
The baseline log is retained without rewriting it as a 21-assertion run.

Kondo on every changed Clojure file: 0 errors, 0 warnings (one pre-existing
single-string `str` informational message in the runner). check-parens: OK.
No serving JVM changes, clicks, production model updates or Lean builds.
