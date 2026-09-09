# FoldC: what survives the separation — 2026-09-09

The old proof survives; its identification with the current runtime does not.
Keep the old witness and receipt unchanged as an ordered-fold example. Treat
the probability/prediction → scalar-risk calculation as a distinct evidence era.
This is a claim distinction, not permission to bypass the failing runtime check.
Joe's authority is recorded verbatim in the session file's follow-up section.

## What the two artifacts say

| Question | Old ordered-fold witness | Separated calculation |
|---|---|---|
| Input | An outcome-indexed real function, plus an ordered list of layers | A prediction Q, conditional K, and separately supplied preference distribution C |
| Operation | Pointwise left fold with each layer's chosen binary operation | Compose prediction kernels; evaluate KL against C; add the resulting weighted scalar to risk |
| Concrete numbers | Base 3; add one then double gives 8, reverse gives 7 | Grounded-change-only prediction against C(grounded-change)=1/2 gives ln 2 ≈ 0.6931471805599453 |
| Proven claim | Empty-list identity and a counterexample to order independence | Constant K absorbs normalized upstream prediction; positive Q at preferred zero is inadmissible; the stated logarithm arithmetic |
| Limit | Declaration witness, no run identity; no probability-normalization theorem | No theorem connecting the Clojure execution to Lean; no learned observation bridge; no full stochastic-category instance |

Old definitions and proofs: mathlib4:DarkTower/WarMachine/FoldCWitness.lean:8-36.
The general carrier at mathlib4:DarkTower/WarMachine/Holes.lean:7319-7327
permits arbitrary real-valued layers and binary composition. The value 3 is
not the seed's probability 1/2 and must not be silently substituted for it.
Old fixture: futon2:holes/labs/wm-contract/fold-c-reference.edn:1.
Old certificate: futon2:holes/labs/wm-contract/fold-c-positive-receipt.edn:1.

New definitions and proofs: mathlib4:DarkTower/WarMachine/PreferenceRiskSeparation.lean:14-69
(commit b76719cd23). Runtime adapter:
futon2:src/futon2/aif/disposition_risk.clj:59-99 (commit 02b317f5).
KL implementation: futon2:src/futon2/aif/disposition_risk.clj:116-133.
Deciding risk sum: futon2:src/futon2/aif/efe.clj:702-713.

## Analogy, and why it is not equality

Both describe a calculation assembled from named stages. The old example shows
why arbitrary transformations must retain their order: (3+1)×2 differs from
3×2+1. The new pipeline also has typed stages: prediction precedes comparison
with preferences. It is not a list of add-one/multiply-two preference updates.

There is a limited algebraic embedding: for a *fixed* policy and fixed computed
risk contributions, addition of those scalars can be represented by a left fold
on real numbers. In exact real arithmetic these additive contributions commute;
that does not contradict the old counterexample involving multiplication.
Floating-point summation need not be bitwise order independent. Such an embedding
is an analogy, not a correspondence proof or a licence to populate
PreferenceLayer.prefers with an invented outcome-independent KL value. No
mapping of the old 3/8/7 to the new ln 2 is supplied by the runtime or the ruling.

The finite-kernel proof gives the useful prediction-side relation instead:
for constant K, sum_o Q(o|p) K(d|o) = K(d), since sum_o Q(o|p)=1.
Thus this explicit adapter cannot discriminate policies by their observation
predictions. It does not close futon2:holes/E-C-realization.md:74-88.

## Fresh execution evidence

Executed from the canonical checkouts on 2026-09-09; no live JVM loads or runs.

1. From futon2, `bb -cp . -e` requiring `checks.positive-proof-receipt` and
   validating the EDN in `holes/labs/wm-contract/fold-c-positive-receipt.edn`:
   exit **0**, `:pass? true`, `:failures []`; shape, elaboration, source basis,
   fixture adapter, toolchain and elaboration match all true. An initial
   diagnostic command had an extra closing parenthesis and exited 1 *after*
   printing a successful validation. The corrected command was run separately;
   only its exit 0 supports this report.
2. `bb -cp . checks/fold_c_witness.clj`: exit **1**, verbatim:

   ```text
   fold-c-witness: FAIL runtime-folded=#{:ruled-outcome-c} lean-folded=#{} exit-convention=0-pass/1-fail/2-mutation-slipped
   ```

   This is an actual baseline mismatch. The checker compares layer-ID sets at
   futon2:checks/fold_c_witness.clj:53-69; runtime declares the layer at
   futon2:src/futon2/aif/ruled_outcome_c.clj:54-65. The old witness's line 13
   claim about the *current* runtime is stale even though its theorem is true.
   The receipt itself explicitly limits correspondence to declared, not
   derived, mappings; validating it cannot establish the missing runtime claim.
3. `lake env lean DarkTower/WarMachine/PreferenceRiskSeparation.lean`: exit
   **0**. All eight printed declarations use only `propext`, `Classical.choice`,
   and `Quot.sound`; no `sorryAx`. The ln 2 theorem is an arithmetic identity,
   not yet a theorem that `scalarKL` evaluated on a concrete twelve-member
   kernel returns ln 2. Keep that distinction in any new certificate.
4. Read the recorded cohort with `checks.disposition-kernel/read-kernel`, wrap
   it with `constant-checkpoint-kernel`, then call `disposition-risk` against
   `ruled-outcome-c/seeded-c` with `{:mission-health 0.5}` as ignored input:
   exit **0**, result **0.6931471805599453**, exactly equal to `Math/log 2`.
   Source `holes/labs/M-aif-full-loop-46/ledger.edn`, SHA256
   `1f195e907fe9ab77b576a9973a058ada6e9c61510ef70309a8803744b13f8044`;
   sample size 3; metadata says `:ignores-channel-observations? true` and
   `:observation-model-bridge :open`. This is a finite recorded-cohort probe,
   not a new live run.

## Certificate succession and the remaining pre-go-live item

Preserve the old receipt, fixture and negative controls as evidence about the
old mathematical example. Do not re-attest them as if matching layer names
would prove today's scalar-risk calculation. The old receipt remains
independently checkable; the composite runtime gate remains red.

The successor needs a separately named claim and its own source-pinned receipt:
instantiate the finite prediction/preference distributions with all twelve
named outcomes and explicit zeros; prove the concrete `scalarKL` result and
zero refusal; bind that instance to the adapter's source cohort and seed;
check the runtime scalar boundary and opt-out behavior. The existing adapter
and separation proofs are reusable inputs to that work, not a completed
replacement certificate. Negative controls must independently detect a changed
seed, invalid support, a false observation-conditioning claim, and a mismatch
at the scalar-risk boundary. The checker must distinguish a failed baseline
from an escaped mutation (its current negative-mode message conflates them at
futon2:checks/fold_c_witness.clj:84-93).

The reviewed successor should then replace the *runtime comparison obligation*
explicitly, retaining the old ordered-fold check under its historical scope.
This note changes neither gate nor registry. Find-marker retirement and contract
re-pinning remain parked while that replacement is implemented and reviewed.
The comparison is complete; successor certification is the next bounded task.
