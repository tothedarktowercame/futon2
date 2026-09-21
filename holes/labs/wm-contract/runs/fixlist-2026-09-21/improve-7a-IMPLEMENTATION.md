# improve-7a — record-only focus receipt

Branch `fix/narrative-improve-7a`, base `e59a5571`, implementation `c7a68fa9`.
No scoring, preference, habit, belief or selection-law changes. No clicks or
shared-JVM evaluation. The selection certificate gains `:focus-receipt`; the
selection narrative gains one sentence naming focus and all candidate classes.

`focus-receipt` recomputes commit-facets-v1 from declared, pinned commit paths,
with fractional credit, a bounded UTC-day window and time-qualified facet edges.
The resource `resources/wm/focus/commit-facets-v1.json` freezes eight repository
heads and 331 commit/path rows from the discovery. It was generated read-only
with `git diff-tree --root --no-commit-id --name-only -r <pinned-sha>` per row.
Tests read this resource and compressed frozen run fixtures, never live Git for
discovery. The serving-path integration fixture creates its own temporary Git
repository for its existing token-observation checks.

The resource is a bounded **retrospective history snapshot**, not a live Git
collector. No commit after the receipt's as-of time contributes credit. The
snapshot's coverage end and the effective analysis end are recorded separately;
each window expires 24 hours after its coverage end. After expiry, discovery
and candidate classification become typed unknown. Refreshing discovery input
is explicit future work; this slice does not silently reuse it forever.
Candidate relations from the discovery are attributed retrospective readings;
they do not certify historical applicability. The WM/APM bridge cites Q3 at
`40762056` and becomes eligible at its commit time, Sept 21 17:31:44 UTC (not
at midnight or during the 04:24 run). The confirmed global split cites
`2181ac4d`; early historical receipts mark it a retrospective ruling.

Observed replay counts:

| Input as-of | Commits | WM credit | Other credit | Focus | WM/APM bridge |
|---|---:|---:|---:|---|---|
| Sept 20 23:59:59Z | 165 | 151 | 14 | WM | ineligible |
| Sept 21 18:00:00Z | 166 | 157 | 9 | WM | eligible |

Previous focus is explicitly absent on the serving path, which does not yet
supply an authoritative continuation. The pure API retains a supplied prior
focus; completion and transition consumption stay typed absent/held. The
same-focus facet relation works in both directions. Missing embedding nodes
remain absent; no retrieval proximity authorizes a relation. Candidates without
a declared relation are unknown, never irrelevant. F2 is a focus target but
its unchanged prediction does not become a known-failure outcome.

The receipt records .55/.35/.05/.05 as a **global long-run estimate**, an explicit
attested-increment/known-typed-failure outcome-domain map, and held
unrepresented-class mass, local C, attestation and predictive kernel. It builds
no token-powerset C. Frozen replays retain every pre-existing certificate field's
printed bytes, candidate scores, G decomposition and selection law/posterior.
Adding/removing a hash-map entry can change container print order; the test
therefore checks whole-decision equality and each old field's bytes separately.
The validator rejects planted class, focus and mass changes. The new namespace
registers with load identity and is listed in required sources.

## Validation

All commands ran in the isolated worktree, Java 21 / Clojure 1.11.1:

`clojure -M:test -m cognitect.test-runner -n <namespace>`:

| Namespace | Tests | Assertions | Result |
|---|---:|---:|---|
| futon2.aif.focus-receipt-test | 5 | 73 | 0 failures/errors |
| futon2.aif.run-narrative-test | 26 | 122 | 0 failures/errors |
| futon2.aif.scoring-input-receipts-test | 3 | 44 | 0 failures/errors |
| futon2.aif.load-identity-test | 3 | 12 | 0 failures/errors |
| futon2.report.war-machine-test | 89 | 511 | 0 failures/errors |
| futon2.aif.full-loop-runner-test | 191 | 1115 | 3 pre-existing failures, 0 errors |

The new grounded runner test uses `run-feature-card-attempt`, asserts
`closed-execution`, retains the receipt in the selection checkpoint and keeps
selection-law bytes unchanged. It passed. The runner namespace's three failures
were reproduced in detached base worktree `futon2-improve-7a-base` at `e59a5571`:
`attempt-limb-evidence-follows-checkpoints-in-manifest` expects 16 entries but
gets 17; `selected-runner-action-is-minted-once-and-returned-from-writer` has
two old evidence-list assertions omitting `retained/surprises.edn`. These were
not changed in this task. Reproduction after requiring that namespace:

```clojure
(clojure.test/test-vars
  (map #(ns-resolve 'futon2.aif.full-loop-runner-test %)
       '[attempt-limb-evidence-follows-checkpoints-in-manifest
         selected-runner-action-is-minted-once-and-returned-from-writer]))
```

Baseline failure for this feature: run the real serving two-tick fixture on
`e59a5571`, before the namespace exists, and assert the new receipt:

```clojure
(futon2.aif.token-observation-initialization-test/with-two-ticks
  (fn [{:keys [second]}]
    (clojure.test/is
      (= :wm/focus-receipt-v1
         (get-in second [:selection-certificate :focus-receipt :schema])))))
;; FAIL: actual (not (= :wm/focus-receipt-v1 nil))
```

Loading the new test namespace against base also fails with “Could not locate
futon2/aif/focus_receipt… on classpath”; the assertion above establishes the
missing serving behavior independently of that namespace-load failure.

`clj-kondo --lint` on all seven changed Clojure files: 0 errors / 0 warnings
(one existing informational `str` diagnostic in war_machine.clj).
`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
'(arxana-check-parens-cli)' -- --no-defaults <the same seven files>`: OK.
`git diff --check`: clean. Full command outputs are in
`/tmp/improve-7a-{focus,narrative,receipts,load,wm,runner-gate,base-runner-controls,baseline-assertion,lint,parens}.log`.

Scoped registry warrant:
`test-registry-e4b8614af812e88a804d26270c6dc2009be6753092b926762c128f4b49c2c4c9`.
Fresh `check`: `:warrant? true`, `:outside-closure []`. It covers the new focus
namespace test (5/73), serving attachment, reader/validator/narrative/load-identity
sources, declaration resource and both frozen fixtures; it is not a warrant
that the entire runner namespace is green. Registry commands from futon3c's own
CLI process: `clojure -M -m futon3c.test-registry run /tmp/improve-7a-registry.edn`
and `check /tmp/improve-7a-registry-check.edn`. The registered command uses
`clojure -M:test -n futon2.aif.focus-receipt-test`, the registry's accepted
explicit-namespace form; the direct gate command above was also run.
