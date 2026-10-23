# O3-1 test receipt

Worktree: `/home/joe/worktrees/futon2/o3-outer-canonical-frontier`  
Branch: `o3-outer-canonical-frontier`  
HEAD before commit: `74a2f3207`  
Environment: OpenJDK `21.0.12.1`; Clojure CLI `1.12.5.1664`.

The fixture is a hermetic projection of b2r's retained action shape: the
first two action unit counts (8 and 19), `:no-recorded-trials` theta records,
and frozen-focus class-model provenance.  The 18 MB recorded run is not copied
into this worktree fixture.

## Negative control (red)

Before policy identity was restored from the scorer entry, the focused test
reported two failures: canonical and hypothetical-objective rows both had a
nil `:policy-id`.  The relevant result was:

```
FAIL in (b2r-frontier-is-sealed-diagnostic-and-can-disagree-with-objective)
expected: (not= (:policy-id canonical-first) (:policy-id objective-first))
actual: (not (not= nil nil))
```

## Green run

Command:

```
clojure -X:test :nses '[futon2.aif.outer-canonical-g-frontier-test]'
```

Result:

```
Ran 2 tests containing 14 assertions.
0 failures, 0 errors.
```

The two tests are `b2r-frontier-is-sealed-diagnostic-and-can-disagree-with-objective`
and `missing-class-model-is-a-retained-unscored-refusal`.

Additional gates: `clj-kondo --lint` on the three changed Clojure files: 0
errors, 0 warnings (one pre-existing informational note); check-parens: `OK`;
`git diff --check`: exit 0.  The requested existing-namespace command ran once:
`clojure -X:test :nses '[futon2.aif.outer-policy-objective-test futon2.aif.meta-live-outer-selector-test]'`.
It produced 1 failure in the pre-existing selector control
`explicit-nil-constructor-injections-use-production-defaults`: expected either
`:outer-policy-evidence-provider-required` or `:incomplete-policy-menu-coverage`,
got `:no-canonically-bound-policy`.  O3-1 runs only after a sealed menu and
does not affect that earlier reason.

## Review fixes (claude-7, 2026-10-23)

Test (a) built its disagreement from objective values written into the test
(`G ± 10`), so it could not fail on any change to `produce`, and its
"selection unchanged" check compared a literal with itself.  Run on b5's live
input menu, `produce` scores all four entries and ranks the policy the run drew
(`M-stack-geometry`, from a four-way objective tie) last.  The tests are now
pinned to that menu: `test/fixtures/outer-canonical-g/b5-input-fc464172.edn`
(58 KB; entries projected to the keys `produce` reads, values verbatim,
provenance under `:source`).

Source changes: a scorer exception becomes the typed refusal
`:canonical-score-threw` instead of ending outer selection; the carrier records
each entry's own `:entry-horizon-steps` beside the shared H.

Red control: making the scorer receive every entry's action and preferring the
score over the pre-refusal made `missing-class-model-is-a-retained-unscored-refusal`
fail (1 failure).  Restored.

Green: `clojure -X:test :nses '[futon2.aif.outer-canonical-g-frontier-test]'`
→ `Ran 3 tests containing 29 assertions. 0 failures, 0 errors.`
clj-kondo 0/0, check-parens OK, `git diff --check` clean.

The selector failure `explicit-nil-constructor-injections-use-production-defaults`
also fails at the base commit `74a2f3207` (detached worktree, one var: 1 failure),
so it predates O3-1.
