# adf04df0 out-of-band evidence — prepared and computed, NOT admitted

Row B5-INJURIES-HOLD-RUN001, medic route (lead amendment §1). Work root:
`~/notes/wm-next/criteria-completeness/run001/adf04df0-oob/` (prepare.clj,
acceptance.clj, artifact.edn, result.edn, out-of-band/ pin, logs/).
This branch records the computation only; nothing here admits the receipt,
writes a ledger, or changes a work order.

## Result: `:status :checked`

Run 2026-10-31T15:12Z via `futon2.aif.out-of-band-fix/compute!` (from the
futon3c classpath with `dev` on the path, since the acceptance runner lives
in `futon3c.wm.meta-self-heal-adapters`).

- **Artifact pin** `8662619015d02dd3d7d5140638a86891858382bbdc994ea8931a9550cb962b5e`
  (`out-of-band/86626190…edn`), schema `:wm/out-of-band-acceptance-v1`,
  injury `injury/adf04df0f31ab8727ac57d9c7f2496d8fa28598c`.
- **Current** (canonical repo, fix present): 1 test / 4 assertions,
  **0 failures, 0 errors**. Run pin `42a530db0839c611e70cb0cb49b579b8dd43dfae3d5aaff58a1ce64ad04b87d1`.
- **Counterfactual** (worktree at HEAD with c96bd9871's src diff reverse-applied):
  1 test / 4 assertions, **2 failures, 0 errors** — the mislabeled
  `:author-delivery-wrong-repository`/`:commit-not-on-canonical-head` makes
  the `:kind`/`:reason` assertions FAIL without any error, exactly the
  required fail-not-error negative control. Run pin `3874799f7e2d39ef8ffbb810f171a96039c1c393820b6c1875b98db9dbf29784`.
- Counterfactual worktree retained at
  `~/worktrees/futon2/out-of-band/4b34de53-236b-42f4-bbbb-d9d8bdea8270`.

The acceptance test drives the real `check-delivery` (3-arity, temp
resource-fn) on the exact historical input — the 9-char abbreviated commit
`8561ecdc7` — read from the committed reconciliation record
`holes/labs/wm-contract/reconciliation/adf04df0-delivery-evidence/9918c345ea60091fd2f22439e57c03666903a0f992e26b9ae6833d90ec4bc386.edn`,
present on both trees (only src/ differs on the counterfactual). Ledger
linkage asserted before retention: the live ledger's ordinal-130 entry pins
the same cause-record-pin (`tick-run-record-2026-10-31-f805ca4b….edn`,
sha256 `ba359b5e…`) and the stated failure-kind/stage.

## Corrections to the dispatch

1. **The dispatch's full fixed-by SHA has a wrong nibble**: it said
   `c96bd9871a7f9af35f35d73528dd67dacc8b52e8`; the commit is
   `c96bd9871a7f9af95f35d73528dd67dacc8b52e8` (`git rev-parse c96bd9871`).
   With the wrong sha compute! correctly refuses `:out-of-band/fix-not-reachable`;
   the artifact carries the corrected sha.
2. Tooling note (not a defect in this row): the acceptance namespace
   initially carried a long multi-line docstring; under the futon2 test
   runner it compiled to a misleading `ClassNotFoundException: clojure.edn`
   at the `(:require …)` form. A one-line docstring with the explanation in
   trailing comments loads cleanly on both trees. Not investigated further —
   named and left.

## Checks

- `clojure -M:test -n futon2.aif.meta-acceptance-injury-adf04df0-delivery-test`
  green in-tree (1/4/0/0) before the compute run.
- clj-kondo on both notes files: only the expected namespace/file-name
  mismatch (the source lives in notes and is materialized at the declared
  path at run time).
- `futon4/dev/check-parens.el` (batch, strategy both): OK.

## Owe / not done

Admission of the checked receipt into the medic route is explicitly NOT
performed (prepare-only per the row). No click, reload, canonical store or
ledger write occurred.
