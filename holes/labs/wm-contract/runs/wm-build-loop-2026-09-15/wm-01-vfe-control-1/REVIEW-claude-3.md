# WM-01-vfe-control-1: claude-3 review

**Reviewer verdict: ACCEPT** for futon2 `c7a6f24f` (author codex-2). The weakened-positive control is type-correct again and is caught for the intended reason. The variational-free-energy positive receipt is freshly re-attested, and the U71 re-attestation gate is **green: 33 receipts, 0 drift, exit 0**.

A green receipt gate does not close WM-01 and does not admit R4.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-vfe-control-1.md` (sha256 `e7e1987f…`), elected by codex-28 after `invoke-1789563134297-21305-d50ea53d`.
- Author job: `invoke-1789563298158-21308-2fafd353`. Handoff: `../handoffs/WM-01-vfe-control-1-to-codex-2.md` (sha256 `2d6439cc…`).

## What I checked

1. **Scope.** `c7a6f24f` touches only `checks/variational_free_energy_witness.clj`, `variational-free-energy-positive-receipt.edn` and this run directory. No Lean, fixture, validator, schema, known-stale or other-receipt edit. Not pushed.
2. **The wrapper diff changes exactly one thing:** the weakened replacement's precision argument, from `gaussianReference.precision` to `⟨gaussianReference.precision, by norm_num [gaussianReference]⟩`. No guard was added; modes, messages and exit convention are unchanged.
3. **The mutation, re-derived by me from the committed wrapper's own string literals:**
   - the substitution target occurs **exactly once** in the current `VariationalFreeEnergyWitness.lean`;
   - the mutated source elaborates in `/tmp`: **exit 0, no errors**.
4. **The receipt, recomputed by me:**
   - `basis-record` against the installed receipt: **zero differing fields**;
   - `validate` without override: `:pass? true`, `:failures []`;
   - `validate` with the weakened override: `:pass? false`, **exactly `[:positive-source-drift]`**;
   - the only declaration whose slice changes under the mutation is **`constantGaussianReference`**, matching codex-2's `drift-location.edn` (`6b5ab5c8…` → `2709d50a…`).
5. **Identities kept separate.** Canonical predecessor `fe6f51a9…` (at `c7a6f24f~1`); retained receipts-2 candidate recorded `2026-09-16T00:26:24Z`; installed fresh successor `5678a915…`, recorded `2026-09-16T12:56:48Z`. `candidate-to-fresh-diff.edn` shows the timestamp as the substantive difference.
6. **The five modes, re-run by me serially after confirming the receipt passes:**

   | Mode | Exit | Message |
   |---|---|---|
   | positive | 0 | `PASS` |
   | `--negative-value` | 0 | `negative-control PASS (perturbed F rejected)` |
   | `--negative-type` | 0 | `negative-control PASS (expected F type rejected)` |
   | `--negative-weakened-positive` | 0 | `negative-control PASS (weakened positive rejected)`, receipt report `:pass? false` |
   | `--unrelated-positive-edit` | 0 | `unrelated-edit PASS (declaration basis stable)`, receipt report `:pass? true` |

7. **Gate, run by me:** `PASS -- 33 receipts; 0 known stale; 0 un-attested drift`, exit 0. `positive-receipt-known-stale.edn` untouched.
8. **clj-kondo** 0 errors, 0 warnings; **check-parens** OK.

## Findings (none blocking)

- **F1. The weakened mode still accepts any receipt failure as a catch.** Its `rejected?` requires `(not (:pass? receipt-report))`, not specifically `:positive-source-drift`. It is caught for the right reason *now*: I confirmed the failure is exactly source drift in `constantGaussianReference`. But if the canonical receipt goes stale again, this mode will pass without testing anything, exactly as it would have before this packet. Asserting the specific failure would close that; it is a wrapper-semantics change outside this dispatch.
- **F2. "mutation slipped" still misdescribes positive-side failures**, as recorded in earlier reviews.

## Campaign state after this packet

All 14 receipts that the 480a666ad2 repair left drifted are now re-attested and reviewed: predictive (`027b4210`), the twelve in receipts-2 (`978d781d`), and variational-free-energy (this packet). The gate that `worklist_check.bb` runs for every seat now passes.

## Limits

- WM-01 is **not** checked. Remaining: subject-bound verification and review for the proposed R4 identity; global registry publication after the R17 pin; the other proof and consumer bindings; shared State/Action/Outcome and model-revision semantics; numeric semantics and production correspondence.
- R4 stays blocked.
