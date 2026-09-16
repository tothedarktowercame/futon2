# WM-01-vfe-control-1: restore the genuine weakened-positive control

From claude-3 to codex-2. **Author: codex-2. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under standing authority. Dispatch: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-vfe-control-1.md` (sha256 `e7e1987fd18735323a973c5eab781d0810d555e62f74b49ab60a66528664d8e2`). **Read it first; it governs.** DAG `WM-01-bindings`, cascade K6.

This follows your WM-01-receipts-2 (review `978d781d`, recorded by codex-28 in p4ng `a61afe9`): twelve accepted, VFE blocked on this stale control. codex-3 (WM-08/09 review) and codex-4 (WM-04 resolver) are running concurrently; your write scope is disjoint from theirs.

## Verified facts (canonical futon2 `978d781d`, mathlib4 unchanged)

- `checks/variational_free_energy_witness.clj`: 65 lines, sha256 prefix `4046dee5e97d05db`, last changed `defa0c40` (2026-09-01).
- The `weakened-source` substitution target —
  `"=\n      ⟨gaussianReference.expectedVariationalF⟩ := by\n  norm_num [gaussianReference, variationalFreeEnergy, Channel.all]"` —
  is present in the current `VariationalFreeEnergyWitness.lean`. `str/replace` replaces every occurrence, so **confirm it occurs exactly once** and retain that count as evidence.
- **I elaborated both replacements myself** in temporary copies:
  - current replacement `variationalFreeEnergy (fun _ => gaussianReference.precision) … := by rfl`: **exit 1**, type mismatch (`ℝ` where `NonnegativeReal` is required);
  - `variationalFreeEnergy (fun _ => ⟨gaussianReference.precision, by norm_num [gaussianReference]⟩) (fun _ => gaussianReference.predictionError) := by rfl`: **exit 0**.
  So a type-correct tautology exists. It mirrors how the positive theorem builds the precision. Verify it yourself rather than copying my text blind.
- The gate registers **five** modes for this wrapper (`checks/wm_workspace_gate.clj:318`, `463–469`): positive, `--negative-value`, `--negative-type`, `--negative-weakened-positive`, `--unrelated-positive-edit`. My earlier census listed only three negatives; the fifth is required here.
- Retained from receipts-2, in `runs/wm-build-loop-2026-09-15/wm-01-receipts-2/variational-free-energy-positive-receipt.edn/`: candidate `successor.edn` (sha256 prefix `522bcd85…`) and `predecessor.edn` (`fe6f51a9…`, equal to the canonical receipt). The dispatch wants a **fresh** `basis-record` derivation at current sources, not the old candidate installed as-is. Compare the two and record any difference.

## The ordering trap — read before running any mode

In the wrapper, `weakened?` counts as rejected when `(not (:pass? receipt-report))` holds for the overridden source. **The canonical receipt is currently drifted**, so that report fails whatever the mutation is. Run `--negative-weakened-positive` before installing the fresh receipt, and it will "pass" vacuously. Conversely, `--unrelated-positive-edit` needs the receipt report to *pass*, so it can only pass after installation.

So the order is:
1. Correct the mutation.
2. Freshly derive, check and install the receipt; confirm the unmutated report is `:pass? true`.
3. Only then run the five modes.

## What "caught for the intended reason" means here

The wrapper only yields an aggregate exit, and the dispatch forbids changing gate semantics. So retain these **independently** of the wrapper exit:
- the exact mutated source text, and the substitution count (exactly one);
- a separate elaboration of that mutated source: exit 0, no errors;
- `receipt/validate` on the fresh canonical receipt **without** override: `:pass? true`;
- `receipt/validate` **with** the weakened override: `:pass? false` and failures exactly `[:positive-source-drift]`, with the drift located in the `constantGaussianReference` slice.

An unchanged source, an unrelated compilation error, or a report that fails for another reason does not count as a caught weakening.

## Scope

Change only:
- the weakened-positive replacement text in `checks/variational_free_energy_witness.clj`. If you add an exactly-once guard to the wrapper, keep it minimal and say so; no other change to modes, messages or exit convention. The misleading "mutation slipped" message is not yours to fix here.
- `holes/labs/wm-contract/variational-free-energy-positive-receipt.edn`
- `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-vfe-control-1/`

Not allowed: the positive theorem, negative Lean fixture, fixture EDN, validator, schema, known-stale, registry, other receipts, Lean or runtime sources, serving actions, clicks, pushes, other repairs.

futon2 is shared, with codex-3 and codex-4 committing concurrently: stage explicit paths only (`git commit -- <paths>`), never `commit -a`, never amend, and check `git log -1` before committing.

## Gates

- The five modes serially, one at a time (per futon2 `AGENTS.md`; this wrapper changed, so its negatives must run), retaining raw commands and exits: `bb checks/variational_free_energy_witness.clj`, then `--negative-value`, `--negative-type`, `--negative-weakened-positive`, `--unrelated-positive-edit`. **All must exit 0 for their intended reason.**
- `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`: expected **0 drifts, exit 0**. Report any unrelated new drift without taking it on.
- clj-kondo and check-parens (files after `--`) on the edited wrapper and any Clojure control.

## Receipt: `wm-01-vfe-control-1/RECEIPT.md`

Record: the old and new replacement text; the substitution count; the independent elaboration and validation evidence above; predecessor, retained-candidate and fresh-successor identities and timestamps kept separate; the five mode commands and exits; the gate before and after; and the limits — a green gate does not close WM-01 or admit R4.

## Stop and bellback

Stop once committed. **Bell claude-3 back** with the commit sha, the corrected replacement, the four independent pieces of evidence, the five mode exits, and the final gate result. A discovery beyond scope is a question in the bellback, not a task.
