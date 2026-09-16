# WM-01-receipts-2: re-attest the remaining 13 positive receipts

From claude-3 to codex-2. **Author: codex-2. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-receipts-2.md` (sha256 `16746e129484779432fa2835aa8f5aba2e09cfab086238e40fe9ab1c113b8ad4`). Frozen manifest: `WM-01-receipts-2-manifest.json` (sha256 `d976a45eabf8825bb64f5f29fd7a7c3a02d547c110115e41c3c2cf5d5277ab57`). **Read both first; they govern.** DAG node `WM-01-bindings`, cascade K6.

This is the campaign after your WM-01-positive-binding-1 (`027b4210`, reviewed `854775fa`). Reuse the mechanism you proved there.

## Checkpoint discipline: commit per receipt

This packet is 13 receipts, 34 wrapper invocations and ~26 mutation controls. Its real risk is not time but the missing mid-way checkpoint: one bell gives exactly one signal, so a failure at receipt 11 must not lose receipts 1–10.

- Work receipts in **manifest order**.
- **Commit after each receipt** (its successor bytes, its per-receipt matrix entry, its logs). 13 small commits are correct here, not one large one.
- Maintain `matrix.json` as you go, so it always reflects completed work.
- If you run out of room, stop cleanly and bell back with the matrix as it stands, naming the next receipt. Partial, reviewed progress is a good outcome; a wedged all-or-nothing run is not.

## Verified inputs (re-derive them; a mismatch is a finding)

**The manifest's 13 filenames match my own census exactly.** Current gate state: exit 1, **26 drifts across 13 receipts**.

**Attribution is complete, not sampled.** I computed each drifted declaration's slice hash at `480a666ad2^`, `480a666ad2` and HEAD: **26/26 flip exactly at `480a666ad2`**, with the live hash equal to HEAD in every case, and every pinned value equal to the pre-repair slice. The mechanism is uniform: that repair added `support_nodup` and `mass_eq_zero_of_not_mem` to `ProbabilityKernel` and changed `variationalFreeEnergy` to take a `PrecisionMap`, so every witness declaration constructing a kernel, and the precision consumers, changed slice text. The dispatch requires you to establish this yourself for every slice; I expect you to reproduce these results.

| Receipt | Drifted declarations (old → new, 12 chars) |
|---|---|
| ambiguity | `predictedState` `98e655ea9a0c`→`2156ea86d2b8`; `observationModel` `8329b7c2a065`→`dc5954fa0959` |
| expected-free-energy | `Q` `1acd2511dc1e`→`55c76675edf8`; `Cdist` `43bf261efead`→`78821fab3626` |
| expected-information-gain | `Q` `214ca13df036`→`78e9a9f9e500`; `prior` `e476c58e3d21`→`af8143bd06d5`; `posterior` `bf05e61328b9`→`821ea72b35f4` |
| generative-model | `observation` `a53010b3302c`→`f383a02d375c`; `transition` `80fdb22d491d`→`6eacde9ca9b5`; `policyPrior` `d68e4107e5be`→`b1d0b9c2e716`; **Holes `ProbabilityKernel`** `d5940a2d22c5`→`558040993291` |
| observation-kernel | `reference` `16d2e69414a9`→`ea0a7c724cc3` |
| parameter-posterior-kernel | `posterior` `e4df54ba4a9f`→`ce62bb665846` |
| parameter-prior-kernel | `prior` `0d4762f47d3e`→`7f6face7bce1` |
| policy-prior-kernel | `reference` `8452d3f112bc`→`d05bf72e5230` |
| precision | `weightedReference` `542e6e23243c`→`a2310939f460`; `swappedReference` `4af6666616a4`→`1b73a3a18640`; `precisionAndErrorAreNotInterchangeable` `c914e1877b9f`→`0ff217ed61fc`; **Holes `variationalFreeEnergy`** `6a2cf76f8016`→`b1caf4f3d2a2` |
| predictive-outcome-risk | `predictive` `b751e9a43be6`→`1ca78693fd32`; `preference` `8b1f7f0a74a5`→`35b5a4945778`; `positivePreference` `60bfdddbed1f`→`d2b1672f8f9b` |
| preference-distribution | `fair` `130bbde169b7`→`840c489a9d9b` |
| transition-kernel | `controlled` `0b606f6958c7`→`9b94767f97ff` |
| variational-free-energy | `constantGaussianReference` `b3954989c51a`→`6b5ab5c8be55`; **Holes `variationalFreeEnergy`** `6a2cf76f8016`→`b1caf4f3d2a2` |

**Shared slices.** Holes `variationalFreeEnergy` is pinned by **two** receipts (precision and variational-free-energy). The dispatch allows reusing a command result only when the subject and command are genuinely the same; if you reuse anything for that slice, map it explicitly to both receipts. Holes `ProbabilityKernel` appears in generative-model here, and also in the already-accepted predictive receipt, which you must not touch.

## Gates per receipt: the wrapper and its exact registered negative modes

| Receipt | Wrapper | Negative modes |
|---|---|---|
| ambiguity | `ambiguity_witness.clj` | `--negative-control` |
| expected-free-energy | `expected_free_energy_witness.clj` | `--negative` |
| expected-information-gain | `expected_information_gain_witness.clj` | `--negative` |
| generative-model | `generative_model_witness.clj` | `--negative` |
| observation-kernel | `observation_kernel_witness.clj` | `--negative-normalisation`, `--negative-mass` |
| parameter-posterior-kernel | `parameter_posterior_kernel_witness.clj` | `--negative-prior`, `--negative-outcome` |
| parameter-prior-kernel | `parameter_prior_kernel_witness.clj` | `--negative-outcome`, `--negative-habit` |
| policy-prior-kernel | `policy_prior_kernel_witness.clj` | `--negative-control` |
| precision | `precision_witness.clj` | `--negative-swap`, `--negative-type` |
| predictive-outcome-risk | `predictive_outcome_risk_witness.clj` | `--negative-control` |
| preference-distribution | `preference_distribution_witness.clj` | `--negative-conditioning`, `--negative-pragmatic-cost` |
| transition-kernel | `transition_kernel_witness.clj` | `--negative-uncontrolled`, `--negative-beta` |
| variational-free-energy | `variational_free_energy_witness.clj` | `--negative-value`, `--negative-type`, `--negative-weakened-positive` |

That is 13 positives plus 21 negative modes. **Run them serially, one at a time** (about 6.5s and 3.4GB peak each, futon2 `AGENTS.md`): this machine has hit a cgroup memory throttle, so do not parallelize. Confirm each wrapper's own exit convention before interpreting it; in the predictive wrapper a negative fixture is *expected to elaborate* because `#guard_msgs` asserts the rejection, and other wrappers may differ.

## Per receipt, the required work

1. **Semantic disposition, from reading the source.** `git -C /home/joe/code/mathlib4 show 480a666ad2 -- DarkTower/WarMachine/<Witness>.lean` plus the theorem, fixture and adapter mapping. State whether the added support or precision obligations preserve the claimed mathematical subject. Say explicitly what changed, as you did for the predictive witness (old mass returned one off support; new returns zero).
   - **If the meaning changed** such that the claim or fixture would need rewording, return that receipt as **blocked** with the exact conflict. Do not reword a promise to green a gate. Other valid receipts in the frozen set continue.
2. **Fresh derivation** via `checks.positive-proof-receipt/basis-record` from actual pinned sources and executed theorem/axiom evidence. Preserve the predecessor bytes verbatim and preserve timestamp provenance (record the old `:recorded-at` as history; the successor gets a real new one).
3. **Make a hidden dependency explicit** where an alias would otherwise conceal the repaired carrier or precision type, using the existing source-slice protocol — the `ProbabilityKernel` pin you added last packet is the pattern. Do not claim a complete transitive closure from author-declared slices; the declared-not-derived boundaries stay byte-identical.
4. **Controls.** For each changed pin, and independently for each newly added carrier/precision pin, mutate it in an **isolated copy** and require the intended source-drift refusal. The valid canonical receipt must pass. **Never mutate a canonical receipt.**
5. **Matrix entry:** old and new identities, semantic disposition, commands with exits, axiom results, control outcomes, and what remains.

## End of campaign

Re-run `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`, preserving output and exit. Target: **0 drifts, exit 0**. If any receipt is blocked, the gate will still fail; say so plainly and do not register anything in `positive-receipt-known-stale.edn`. If new unrelated drift appears, identify it without expanding the manifest.

## Scope

Only the 13 manifest receipts under `futon2/holes/labs/wm-contract/`, plus receipts and helper controls under `runs/wm-build-loop-2026-09-15/wm-01-receipts-2/`.

Not allowed: Lean sources, fixtures, wrappers, validator, schema, known-stale, worklist, fragments, the generated registry, the merger suite, the accepted predictive receipt, a new scheduler or framework, checkbox edits, serving actions, clicks, pushes, held P1 work, choosing successor work. Helpers may orchestrate the existing protocol inside your run directory only. Lint and parens any authored Clojure; syntax-check any Python.

futon2 is shared (~190 unrelated dirty paths): stage explicit paths only, never `commit -a`, never amend, and check `git log -1` before each commit.

## Stop and bellback

Stop when the matrix is complete or you must stop cleanly. **Bell claude-3 back** with: the commit shas in order; the per-receipt matrix (old→new pins, semantic disposition, gate exits, control results); any blocked receipt with its exact conflict; the final gate output and exit; and anything unresolved. A discovery beyond scope is a question in the bellback, not a task.
