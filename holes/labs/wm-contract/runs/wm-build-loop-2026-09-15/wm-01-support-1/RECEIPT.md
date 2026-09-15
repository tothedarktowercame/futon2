# WM-01-support-1

Author: codex-2; reviewer: claude-3. Handoff:
`invoke-1789511720453-21280-cd784591`. Governing dispatch SHA-256 verified:
`9322deabfbbf54a6b78cc0e312830aa0ebb37635993f010aeaa9b98370d83b2d`.
Baseline: `009156613dd3ef82eb01c91e441a8a36e01966ed` (baseline-head.txt).
Before/after hashes of all six source/test files: before.sha256 / after.sha256.

## Shared contract and consumers

`machine-model/support-refusal-kind` is the single non-throwing definition:
support must be a nonempty vector with distinct identities. The model's
existing `support!` uses it and retains the model validator's exception-to-data
handling. Public `distribution-admission` uses the same definition before
set comparison, then requires exact row-key coverage and unchanged numeric-1
admission. It returns refusals as data. It never repairs support or masses.

The belief reader already calls `distribution-admission` after its canonical
order guard; no change to machine_belief.clj was necessary. The predictor
already calls it for the initial row and every produced row. It now preserves
support-shape refusal kinds and checks that admission succeeded before applying
its canonical support-set guard. Malformed support cannot reach either set
conversion of a scalar or transition arithmetic.

Support order remains in `:support`, named masses in `:values`. Missing keys
and extra keys, including an off-support zero, refuse. No sorting, deduplication,
normalization, fallback, numeric-law change or first-vals lookup change occurred.

## Before/after and refusal kinds

Direct public-boundary cases are captured in before.edn and after.edn:

| Row / support | Baseline | After |
|---|---|---|
| `{:a 1}` / `[:a :a]` | admitted, exact total 1 | `:duplicate-support` |
| `{:a 1}` / nil or `[]` | `:distribution-support-mismatch` | `:missing-support` |
| `{:a 1}` / `(:a)` or `#{:a}` | admitted | `:missing-support` |
| `{:a 1}` / `[:a]` | admitted | admitted unchanged |

Entrypoint distinctions are deliberate:

- Public boundary: malformed shape -> `:missing-support`; duplicate identities
  -> `:duplicate-support`; missing/extra row keys ->
  `:distribution-support-mismatch`. Non-map row keeps `:missing-distribution`.
- Full model validator: existing support kinds and paths remain. Nil model
  `:state-support` is rejected earlier by required-field validation as
  `:missing-field`; empty/list/set support is `:missing-support` and a repeated
  identity is `:duplicate-support`.
- Belief reader: duplicate/nil/empty/set context support keeps the existing
  `:state-support-order-mismatch`. A canonical-order list compares equal to the
  canonical vector in Clojure and previously passed; it now reaches the shared
  shape check and returns `:missing-support`. Row coverage retains
  `:posterior-support-mismatch`.
- Predictor: malformed shape -> `:missing-support`; duplicates ->
  `:duplicate-support`, both before transition execution. Unsupported numbers
  retain `:unsupported-numeric-type`. Row coverage and other numeric failures
  keep `:invalid-mass`, including the numeric-1 extra-key control. This avoids
  changing existing numeric-1 expectations while exposing the newly enforced
  support failures.

No existing test expectation changed. New tests exercise all four entrypoints,
singleton/seven-status success, order preservation versus swapped values,
missing/extra keys, and malformed vector/list/set/scalar cases. A transition spy
records zero calls for every malformed kernel support and one call for an
ordinary successful prediction, whose expected terminal row is checked.

## Gates

Exact commands are in commands.txt. Each ran from /home/joe/code/futon2,
without a pipeline, with stdout/stderr captured to `.log` and its immediate
exit status in `.exit`.

| Gate | Result | Exit |
|---|---|---|
| Baseline boundary namespaces | 19 tests / 375 assertions, no failures/errors | 0 |
| Baseline compatibility namespaces | 87 / 493, no failures/errors | 0 |
| clj-kondo, six authorized files | 0 errors / 0 warnings | 0 |
| check-parens, six authorized files | OK | 0 |
| Final boundary namespaces | 23 / 427, no failures/errors | 0 |
| Final compatibility namespaces | 87 / 493, no failures/errors | 0 |

Files: baseline-tests.*, baseline-compatibility.*, clj-kondo.*,
check-parens.*, tests.*, compatibility.*. The direct before/after probes also
exited 0. No numeric-1 control or admission rule was changed. A search of checks/ for
`machine[_-](model|belief|predictive)` found no registered wrapper dependency;
no witness negative-mode run was applicable.

## Limits

Exact normalization concerns the numeric total only, not empirical adequacy.
This enforces one runtime support law corresponding to the repaired carrier;
it is not full Lean/runtime correspondence or WM-01 completion. No Lean,
transition arithmetic, held-P1, serving reload or click changes were made.
No blocking scope conflict or additional implementation finding remains.
