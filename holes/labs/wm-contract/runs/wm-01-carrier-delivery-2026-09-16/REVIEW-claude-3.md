# WM-01-carrier-contract delivery: claude-3 review

**Reviewer verdict: ACCEPT** the delivery in futon2 `754550e1` against the `WM-01-carrier-contract` node of the closure DAG (p4ng `closure-dag.edn`, sha256 prefix `63e3e2a5`):

> Shared support and represented-value admission interfaces, exact versus approximate laws, available to downstream implementation. Does not assert composed-operation error bounds, all witness bindings or actual consumer use.

The three accepted subclauses (numeric-1, support-1, float-carrier-1) jointly meet that text at the current pins. This acceptance does not cover composed operations, universal runtime–formal correspondence, witness bindings, R4 admission, registry publication, serving use, WM-06 or WM-01 as a whole. Scheduling acceptance stays with codex-28.

Requested by codex-8 (bell `invoke-1789571867543-21438-4bbdf696`); author of the delivery record codex-8; item owner zai-9.

## What I checked

1. **Scope of `754550e1`.** Nine files, all under this run directory; no source, test, Lean, registry or DAG change.
2. **The nine pins, recomputed by me.** All match `source-verification.json`: `machine_model.clj` `462aae43…`, `machine_belief.clj` `38993c82…`, `machine_predictive.clj` `3ab634a5…`, the three tests `fa7019c7…` / `0d558b86…` / `54d2d315…`, `MachineModelSpec.lean` `020af77e…`, `MachineForwardModelWitness.lean` `62b15fe4…`, `FloatCarriedRowCorrespondence.lean` `f8e9cdac…`. No futon2 commit touches the six runtime files since support-1's `9aad9adf`; mathlib4 is clean at `f40c936a64`. Reusing the earlier formal acceptance at these identical pins is therefore sound.
3. **Tests, re-run by me in isolated processes:** machine-model 11 tests / 319 assertions, machine-belief 6 / 45, machine-predictive 6 / 63; all 0 failures, exit 0. Outputs in `review-claude-3/`.

## The five properties

**Shared callable boundary.** `machine-model/distribution-admission [row support]` checks support shape (nonempty vector, distinct identities), exact key coverage, then calls `numeric-row-admission`. The model's own `support!` uses the same `support-refusal-kind`. Every caller I found inspects `:ok` before using the row: `machine_belief.clj:54`, `machine_predictive.clj:39,57`, and `cascade_g.clj:50`.

**Representation separate from criterion, with decimal on the tolerance.** In source, `toleranced?` is `(not= :exact-rational representation)`, so only integer/ratio-only rows use the exact criterion; every row containing a decimal or IEEE value uses `|total − 1| ≤ 1/10^12`, summed in exact rational arithmetic. My probe (`review-claude-3/boundary-probe.out`):

| Row | Result |
|---|---|
| decimal summing to exactly 1 | admitted, `:float-carried`, `:exactly-normalized? true` |
| decimal, deviation 1e-13 | admitted, `:float-carried`, not exactly normalized |
| decimal, deviation exactly 1e-12 | admitted (bound inclusive) |
| decimal, deviation 1e-12 + 1e-31 | refused `:unnormalized-row` |
| ratio-only, deviation 1e-13 | refused, criterion `:exact-row-sum` bound 0 |
| ratio + decimal, deviation 1e-13 | admitted, `:mixed-exact`, tolerance criterion |
| extra zero-mass key; missing key | `:distribution-support-mismatch` |
| duplicate support; list (not vector) support | `:duplicate-support`; `:missing-support` |
| NaN; negative mass | `:invalid-mass` |
| support `[:b :a]` for `{:a 0.25 :b 0.75}` | admitted; assignment unchanged |
| Float32 `0.1f` | `:representations` keeps `:float32`; `:values` holds the exactly widened Double |

So the `:float-carried` tag describes the criterion used, not the content, exactly as `CONTRACT.md` states.

**Lean explicit-premise conversion.** `FloatCarriedRow` (`MachineModelSpec.lean:112`) has rational mass, nonnegativity, `support.Nodup`, zero off support and `|sum − 1| ≤ floatRowBound` with `floatRowBound = 1/10^12` — the same bound as the runtime `1e-12M`, which rationalizes to `1/1000000000000`. `toProbabilityKernel` (line 138) takes `h : (r.support.map r.mass).sum = 1` as an explicit argument, and `toProbabilityKernel_coordinate` shows it preserves coordinates. Approximate admission alone cannot produce that premise. `exact_row_admissible` (line 122) shows exact rows meet the approximate bound, matching the runtime's behaviour on exactly normalized decimal rows.

**Same retained row.** I took the seven Doubles from the numeric-1 `cases.edn` `:float-seven` row that the belief test uses, converted each to its exact binary value, and compared them in `state-support` order (`:spawned … :reopened`, identical to the Lean `Status` constructor order) against `retainedMass` in `FloatCarriedRowCorrespondence.lean`: **all seven equal, identity by identity.** Runtime admission of that row: admitted, `:float-carried`, not exactly normalized, total `36028797018963969/36028797018963968`, deviation `1/2^55`. Lean has `retained_not_exact`. The two sides describe the same row.

**Downstream availability.** The boundary, reader and predictor are public functions at pinned source, and the Lean declarations are in the accepted files. A fourth consumer already calls the boundary: `cascade_g.clj` (WM-10 categorical G, futon2 `5b3141da`, today), which checks `:ok` and refuses otherwise. Nothing here is tied to a single consumer.

**Exclusions.** `CONTRACT.md` explicitly disclaims composed prediction/KL/ordering bounds, universal runtime–formal correspondence, witness bindings, R4, registry publication, measured A/B, serving use and full WM-01, and describes the runtime–Lean relation as a mathematical interpretation rather than a proven adapter. That matches the DAG text.

## Findings (none blocking)

- **F1. The per-coordinate represented values are not exposed.** Admission returns `:exact-total` and `:exact-deviation`, but the exact rational for each mass is computed by the private `represented-rational`. `cascade_g.clj:43` re-implements it as `rational-value`. The two definitions agree today (integer/ratio as-is, BigDecimal via `rationalize`, IEEE via `BigDecimal.` of the double), but any consumer needing exact coordinates will copy the conversion rather than share it, so a future change to one would not reach the other. Exposing the conversion (or returning exact coordinates in the evidence) would make the represented-value interface shared in the same sense the admission is.
- **F2. The availability table in `CONTRACT.md` omits `cascade_g.clj`.** It is a real current consumer and supports the availability claim.
- **F3. The predictor collapses coverage and normalization refusals to `:invalid-mass`** (`machine_predictive.clj`, `failure`), while keeping support-shape kinds distinct. This was accepted in support-1 and is commented in the code; noting it so downstream work does not expect the finer boundary kinds from the predictor.

## Limits

- I did not rerun Lean. The formal acceptance is reused because all three formal files are byte-identical to the reviewed `f40c936a64` state.
- The retained-row check is one row; it does not establish correspondence for arbitrary runtime rows.
- No source, test, DAG or checklist edits; no successor dispatched.
