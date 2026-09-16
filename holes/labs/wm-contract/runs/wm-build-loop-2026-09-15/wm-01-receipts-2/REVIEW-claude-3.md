# WM-01-receipts-2: claude-3 review

**Reviewer verdict: ACCEPT twelve receipts; CONCUR that variational-free-energy is blocked.** Author codex-2; commits `4ecc3f3f` … `5c02af15` (13 checkpoints in manifest order).

The U71 re-attestation gate is now at **2 drifts, exit 1**, down from 26, both in the blocked receipt. It is not green. WM-01 is not checked, and the R4 admission stays blocked.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-receipts-2.md` (sha256 `16746e12…`); frozen manifest sha256 `d976a45e…`; elected in codex-28 job `invoke-1789517403069-21299-87f5197d`.
- Author job: `invoke-1789517672325-21301-fa28b682`. Handoff: `../handoffs/WM-01-receipts-2-to-codex-2.md` (sha256 `baca940f…`).

## What I checked

1. **Scope.** All 13 of codex-2's commits touch only manifest receipts and this run directory. The one out-of-scope path in the range (`handoffs/WM-01-positive-binding-1-to-codex-2.md`) belongs to the automatic inbox-zero commit `1db4eeed`, not to codex-2. The accepted predictive receipt was not touched; `positive-receipt-known-stale.edn` still reads `:entries []`; mathlib4 is clean.
2. **Freshly recorded, not hand-edited, for all 13.** I recomputed `checks.positive-proof-receipt/basis-record` against every committed receipt:
   - **12 receipts:** `validate` gives `:pass? true`, `:failures []`, and **zero fields differ** from the fresh recomputation.
   - **variational-free-energy:** `:pass? false`, `[:positive-source-drift]`, with `:source-basis` the only differing field — consistent with its canonical predecessor having been restored.
3. **VFE predecessor restored byte-for-byte.** The committed file and the pre-campaign version both hash `fe6f51a9…`. The candidate successor is retained only in the run directory.
4. **Attribution.** Before dispatch I computed all 26 drifted slices at `480a666ad2^`, `480a666ad2` and HEAD: 26/26 flip at the repair, live equals HEAD, and every pinned value equals the pre-repair slice. codex-2's per-slice `attribution.json` records the same result for every slice, not a sample.
5. **Semantic dispositions.** I read all 13. Each says `preserved` and names the specific change. I checked three against the actual repair diff and each reading is exact:
   - ambiguity: only `support_nodup` and `mass_eq_zero_of_not_mem` added; masses unchanged, since each type has a single constructor;
   - observation-kernel: only the two proof fields added; `mass := fun _ _ => 1 / 2` unchanged;
   - parameter-prior: `mass` changed from a constant `1` to a `match` giving `1` on `inspect/cautious` and `repair/bold` and `0` elsewhere.
6. **My own mutation controls** on precision, generative-model and transition-kernel, on isolated values: each baseline passes, and zeroing a pin in the first or the second source-basis entry yields exactly `[:positive-source-drift]`. Canonical receipts were unchanged by my runs.
7. **Sampled wrappers, serially:** precision `--negative-swap` and `--negative-type`, generative-model `--negative`, transition-kernel `--negative-uncontrolled` and `--negative-beta` — all exit 0 with their own "negative-control PASS" messages.
8. **Shared slice.** Holes `variationalFreeEnergy` is pinned at the live `b1caf4f3d2a2` in the re-attested precision receipt, and at the old `6a2cf76f8016` in the blocked VFE receipt. codex-2 records that no Lean command result was reused across the two subjects.
9. **Final gate, run by me:** exit 1, "2 declaration(s) moved without a re-emitted receipt", both in `variational-free-energy-positive-receipt.edn` (`constantGaussianReference` and Holes `variationalFreeEnergy`).

## The VFE block, reproduced

The wrapper's `--negative-weakened-positive` mode rewrites the positive theorem's right-hand side to `variationalFreeEnergy (fun _ => gaussianReference.precision) …`. `GaussianReference.precision` is declared `ℝ`, while the repaired `variationalFreeEnergy` takes a `PrecisionMap` of `NonnegativeReal`. I elaborated exactly that construction myself: **exit 1**, `gaussianReference.precision has type ℝ` against the expected `NonnegativeReal`.

The positive theorem is fine — it wraps the field as `⟨gaussianReference.precision, by norm_num [gaussianReference]⟩`. The **control** is stale relative to the repair: it can no longer build the weakened variant it exists to reject. Fixing it means editing `checks/variational_free_energy_witness.clj`, which this dispatch forbids. The block is correct, and nothing was reworded to get around it.

## Findings

- **F1. codex-2's wrapper counts for VFE describe its candidate state, not the committed tree.** The receipt reports 34 wrapper invocations with "33 exit 0; one exit 2", i.e. only the weakened mode failing. Against what is committed, **all four VFE modes fail**: positive exits 1, and `--negative-value`, `--negative-type` and `--negative-weakened-positive` each exit 2. Every mode computes the receipt report first, and the restored predecessor is drifted, so `positive?` is false in all of them. codex-2's runs were taken against the candidate successor before restoration. This strengthens the block rather than weakening it, but the matrix's VFE gate results are not reproducible from the committed files.
- **F2. The pre-repair witnesses were weaker than their fixtures.** Parameter-prior, parameter-posterior, transition-kernel, preference-distribution and predictive-outcome-risk previously returned mass one (or one half) *off* their declared supports. The old receipts therefore attested kernels whose mass functions did not match the fixtures' singleton or binary descriptions. The repair makes the witnesses say what the fixtures always said, which is why "preserved" is right.
- **F3. "mutation slipped" misdescribes the failure again.** Every VFE negative prints it, though what failed is the positive-side receipt validation. Same wrapper convention I noted for the predictive receipt; not this packet's to change.

## What unblocks VFE

One bounded packet: correct the weakened-positive replacement in `checks/variational_free_energy_witness.clj` so it builds a genuine weakening under the repaired types (for instance by constructing the `NonnegativeReal` as the positive theorem does), then run that wrapper's three negative modes per futon2 `AGENTS.md`, then install the retained candidate successor and re-run the gate. The weakening must stay a real weakening; the point of the control is that it is rejected.

## Limits

- I recomputed all 13 derivations and read all 13 dispositions, but checked only three dispositions against source diffs, ran my own mutation controls on three receipts, and re-ran wrappers for three receipts.
- Adapter and dependency-closure boundaries remain declared-not-derived; the added carrier pins are specific dependencies, not a complete transitive closure.
- These are hand-derived fixtures, not observations or runtime Q.
- WM-01 is **not** checked; R4 remains blocked.
