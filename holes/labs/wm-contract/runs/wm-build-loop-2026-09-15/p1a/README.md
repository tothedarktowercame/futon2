# P1a implementation and gates

Author: codex-2. Request: `invoke-1789502167375-21225-81e8d7ea` from
claude-2, 2026-09-15. Independent review remains with claude-2.

## Implemented contract

`futon2.aif.work-target-belief` provides declaration loading with a fixed
SHA-256, pure candidate admissions, monotone carry/first introduction, and
the domain-aware row-7 reader. D and the interpretation revision come from
the verified declaration, not duplicated literals. Carry also verifies the
supplied declaration envelope against its retained text, so changing its
parsed masses without changing the pinned bytes refuses.

Caller conventions (P1b integration still pending):

- Call `read-declaration` from the futon2 root, or provide a relocated copy's
  path. Its returned envelope is the first argument to `carry-and-introduce`.
- Tick context supplies `:timestamp`. Registry snapshots supply `:read-at`
  and loader-shaped entries enriched with the acquired bytes' `:sha256`.
  The existing registry loaders do not provide hashes. P1a does not read
  registry documents; a missing pin refuses instead of manufacturing one.
- Candidate admission uses the requested action type and registry resolution.
  Registry status is retained context, not an observation or another live
  filter. Exact candidate strings remain the keys, including mission endpoint
  aliases. Duplicate registry IDs refuse as ambiguous.
- A successful carry returns the state directly, with `:ok true`,
  `:belief`, `:lineage`, `:model-context`, and `:target-refusals`. Half-present
  targets remain unchanged and appear under `:target-refusals` with
  `:carry-missing`; other targets can still be carried/introduced. Whole-state
  refusals contain no belief or lineage rows.
- A predecessor with another model context or a claimed observation update
  refuses. No migration or observation update is implemented.
- `target-belief-input` receives row 7's full model context, including selected
  entity and policy scope, and the exact registry target strings. Include any
  admitted endpoint aliases in that set. It preserves row 7's result and
  attaches lineage alongside it. It refuses an entity/context mismatch and
  never narrows a multi-entity policy scope.

No spec conflict required stopping. No tick integration, shared-JVM loading,
observation/model update, prediction, or production-use claim is made.
`machine_belief.clj` is unchanged; its pre/post SHA-256 is recorded below.

## Fixtures and controls

The test contains copies of actual `load-missions` / `load-tickets` entries
for `M-G-wm-wiring` and `T-car3-phase2-impl`, acquired in a separate tooling
process on 2026-09-15. Their document hashes are recorded in the fixtures.
Tests use those copies, not repeated live scans. Candidate-entry wrappers and
bare actions are both exercised.

Controls cover exact ratio initialization; nonuniform carry on departure and
re-admission; later introduction while retaining an earlier target; unusable
predecessors; both half-present cases even outside the candidate set; all
four domain-reader outcomes versus plain row 7; unchanged posterior identity;
policy-scope refusal; action/registry filtering and endpoint identity;
unreadable registry and missing pins; modified declaration bytes/content;
model separation and unsupported update lineage. Public operations include no
observation update.

## Final gate commands and receipts

All commands ran from `/home/joe/code/futon2`, directly with stdout/stderr
redirected to the named logs, without pipelines. Each command's immediate
exit status was captured in its `.exit` file. Final statuses are all **0**.

```sh
clj-kondo --lint src/futon2/aif/work_target_belief.clj test/futon2/aif/work_target_belief_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- --no-defaults src/futon2/aif/work_target_belief.clj test/futon2/aif/work_target_belief_test.clj
clojure -X:test :nses '[futon2.aif.work-target-belief-test futon2.aif.machine-belief-test]'
```

- `clj-kondo.log`, `clj-kondo.exit`: zero errors, zero warnings.
- `check-parens.log`, `check-parens.exit`: OK.
- `tests.log`, `tests.exit`: 11 tests, 105 assertions, zero failures/errors.
- `source-pins.sha256`: tested source, unchanged row 7, declaration.

During development the two added controls introduced one extra closing
parenthesis in the test file; all three gates detected it. It was corrected
before this final complete gate pass. These logs describe the final sources.
