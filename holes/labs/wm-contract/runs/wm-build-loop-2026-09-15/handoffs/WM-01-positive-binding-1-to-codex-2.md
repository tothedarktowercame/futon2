# WM-01-positive-binding-1: restore the repaired predictive positive receipt

From claude-3 to codex-2. **Author: codex-2. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-positive-binding-1.md` (sha256 `20fc0354eb852cc30399ba7ecd182298a4e451838570f74d94369aca4cfbebec`). **Read it first; it governs.** DAG node `WM-01-bindings`, cascade K6. This packet adds facts I verified; it does not widen the dispatch.

## Read this first: your receipt is one of 27 drifted pins, and you may only fix one

I ran `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`. **Exit 1: "27 declaration(s) moved without a re-emitted receipt", across 14 receipts.** Yours is one line in that list.

| Receipt | Drifted declarations |
|---|---|
| ambiguity | `predictedState`, `observationModel` |
| expected-free-energy | `Q`, `Cdist` |
| expected-information-gain | `Q`, `prior`, `posterior` |
| generative-model | `observation`, `transition`, `policyPrior`, Holes `ProbabilityKernel` |
| observation-kernel | `reference` |
| parameter-posterior-kernel | `posterior` |
| parameter-prior-kernel | `prior` |
| policy-prior-kernel | `reference` |
| precision | `weightedReference`, `swappedReference`, `precisionAndErrorAreNotInterchangeable`, Holes `variationalFreeEnergy` |
| **predictive-outcome-kernel** | **`predictive`** ← yours |
| predictive-outcome-risk | `predictive`, `preference`, `positivePreference` |
| preference-distribution | `fair` |
| transition-kernel | `controlled` |
| variational-free-energy | `constantGaussianReference`, Holes `variationalFreeEnergy` |

**Attribution:** I computed the slice hashes at `480a666ad2^`, `480a666ad2`, and the two earlier Row-16 Holes commits. Every sampled declaration flips exactly at **`480a666ad2`** (the repair). The mechanism explains the rest: the repair added `support_nodup` and `mass_eq_zero_of_not_mem` to `ProbabilityKernel`, so every witness declaration constructing a kernel changed its slice text, as did `variationalFreeEnergy` through the precision change. The two Row-16 `Holes.lean` commits are older than the repair and are not implicated.

Consequences you must carry into the receipt:
- Restoring the predictive receipt **cannot** green the U71 gate: 26 drifts remain in receipts you may not touch. Report that as the residual; do not imply the gate is fixed.
- `positive-receipt-known-stale.edn` currently holds `:entries []`, so every mismatch reads as `UNATTESTED-DRIFT`. That file is **outside your scope**: do not register the others as known-stale. Registering drift instead of re-attesting it is exactly the laundering the dispatch forbids.
- Per C584, this gate runs from `worklist_check.bb:231-241`, which every build seat invokes, so the tree is currently refused by it. That is pre-existing, not caused by you.

## The one deliverable

A freshly verified, independently reviewed successor `PositiveLeanWitnessReceipt/v1` for `holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn`, replacing the drifted source basis with re-recorded evidence. **Not a hash edit, and not the old timestamp or result reused as new execution evidence.**

## The mechanism that already exists — use it, don't reinvent

`checks.positive-proof-receipt/basis-record` (`checks/positive_proof_receipt.clj:175`) re-records exactly the fields that must be freshly measured:
- `:source-basis` via `live-source-basis` (declaration-slice hashes),
- `:toolchain` via `live-toolchain` (Lean version, `lean-toolchain` and `lake-manifest` hashes),
- `:result` via `elaborate` (which writes a temp file importing the module and running `#print axioms <theorem>` under `lake env lean` in the receipt's declared `cwd`),
- `:fixture` sha256.

There is **no CLI**, so call it from a small authored control under your run directory. The precedent is `runs/FoldC-two-axis-healing-2026-09-09.md`, which re-attested through `basis-record` and compared the prior derivation before replacing it. Do the same: record the old derivation, the new one, and the diff between them.

`validate` reports five distinct failures. Only `:positive-source-drift` is currently firing for your receipt; codex-4's diagnostic showed `:receipt-shape-valid? true`, `:fixture-adapter-matches? true`, `:toolchain-matches? true`, `:elaboration-matches? true`. Keep it that way:
- `receipt-shape-valid?` demands at least two `:source-basis` entries, the theorem's own name among the declarations, the exact `dependency-boundary` and `correspondence-boundary` maps unchanged, `:kind :edn-fields-to-lean-declaration/v1`, the adapter's `:lean-declaration` present in the basis, and **every mapping's `lean-field` string occurring literally in the retained slice text** with a matching `:expected-shape`.
- `:result` must record exit 0 with an axiom vector inside {propext, Classical.choice, Quot.sound}, and must equal the live elaboration.

**Watch the adapter mappings.** The current adapter pins `lean-field` strings `"inductive Policy"` and `"| .inspect => [clear]"`. The repair rewrote `predictive`'s `mass` into a `match` over `⟨.evidence, .clear⟩`, but the `support` arm `| .inspect => [clear]` survives verbatim, so the mapping should still hold. Verify that against the retained slice rather than assuming it; if a pinned `lean-field` no longer occurs, that is a **meaning change**, and the dispatch says stop and report the conflict rather than re-word the mapping to fit.

## What I verified about meaning

- The fixture `holes/labs/wm-contract/predictive-outcome-kernel-reference.edn` is unchanged: sha256 `667873b464525ad9a14be4fbc4cf0d28ba55d379b5d9381bbe506264603eff48`, last touched 2026-09-01. Confirm this yourself; the dispatch requires it.
- `PredictiveOutcomeKernelWitness.lean` (currently sha256 `b38429046b74af1ed580c5fbfddda984d49d81f8705b5568f74a783494c05632`) still declares the same deterministic content: inspect predicts `clear`, repair predicts `fixed`, each a normalized point mass, with `allPolicyRowsNormalised` proved by `predictive.normalised p` exactly as before. The repair added the two support obligations and made `mass` explicitly zero off-support.
- So the claim still means what the fixture says. The new support obligations are additional discharged facts, not a changed subject. State that in your own words, from your own reading.

## Controls to run and retain (command, cwd, exit, output; don't pipe gate output)

1. **The positive theorem and axiom probe** at the repaired sources.
2. **The wrapper's positive mode**: `bb checks/predictive_outcome_kernel_witness.clj` — must exit 0 after your repair.
3. **Both registered negative modes, serially** (about 6.5s and 3.4GB each, per futon2 `AGENTS.md`): `--negative-unconditional`, then `--negative-softmax`. Both must exit 0.
   - Note the convention before you interpret them: a negative fixture is *expected to elaborate*, because its `#guard_msgs` asserts the rejection text. `rejected?` is `(zero? (lean-exit …))`. Both fixtures exit 0 today, so the negatives already reject; they were only failing through the positive side. Exit 2 prints "mutation slipped" even when the positive is what failed, which is misleading — and fixing that message is **not** this packet.
4. **A mutation control on your restored receipt, in an isolated copy**: alter the `predictive` slice hash and require the real validator to report `:positive-source-drift`. `holes/labs/wm-contract/positive_receipt_reattestation_controls.sh` is the established pattern for this (copy receipt and sources to a temp root, observe PASS, plant drift, require `UNATTESTED-DRIFT`). Do not modify that script.
5. **The re-attestation gate** before and after, retaining both: expect 27 drifts before and 26 after, with yours gone.
6. **Confirm unchanged bytes** for the fixture and both negative Lean fixtures.
7. clj-kondo and check-parens (files after `--`) on any Clojure control you author.

## Scope

Only:
- `futon2 holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn`
- receipts and controls under `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-positive-binding-1/`

Not allowed: Lean sources, the wrapper, the fixture, the schema, the validator, the merger test, any other receipt, fragments, the registry, `positive-receipt-known-stale.edn`, checkbox edits, serving actions, clicks, pushes, held P1 work, choosing successor work. Temporary read-only Lean probes are fine; no canonical `.lake` writes unless a genuinely missing dependency forces a separately reported build.

Preserve the predecessor receipt's bytes and identity in your run directory. futon2 is shared (~190 unrelated dirty paths): stage explicit paths only, never `commit -a`, never amend, check `git log -1` first.

## Receipt

Record: the old and new receipt bytes and hashes; the old→new declaration-slice mapping with both hashes; the old and new `basis-record` derivations and their diff; the fixture and negative-fixture bytes confirmed unchanged; every command with cwd and exit; the before/after re-attestation counts; and the limits — that this closes one source-drift gate only, that 26 drifts remain outside scope, that the R4 admission stays blocked, and that the declared-not-derived adapter and dependency-closure boundaries are unchanged (no claim of verifying the Clojure adapter or a full transitive closure).

## Stop and bellback

Stop once the receipt and controls are committed. **Bell claude-3 back** with the commit sha, the old→new slice mapping, the positive and both negative gate results with exits, the mutation-control result, the before/after drift counts, and any meaning conflict. A discovery beyond scope is a question in the bellback, not a task.
