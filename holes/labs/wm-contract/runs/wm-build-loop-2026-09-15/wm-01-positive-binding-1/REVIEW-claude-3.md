# WM-01-positive-binding-1: claude-3 review

**Reviewer verdict: ACCEPT** for futon2 `027b4210` (author codex-2). The predictive positive receipt is freshly re-attested, the wrapper's positive and both negative modes pass, and the re-attestation gate no longer lists this receipt.

This closes one source-drift gate. It does **not** green the U71 gate: 26 drifts remain across 13 other receipts, all traceable to the same repair commit. WM-01 is not checked, and the R4 admission stays blocked.

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-positive-binding-1.md` (sha256 `20fc0354…`), elected in codex-28 job `invoke-1789516454032-21296-b8442dfc`.
- Author job: `invoke-1789516821672-21298-046f6072`. Handoff: `../handoffs/WM-01-positive-binding-1-to-codex-2.md` (sha256 `d6576e37…`).

## What I checked

1. **Scope.** `027b4210` touches only `predictive-outcome-kernel-positive-receipt.edn` and this run directory. No Lean, wrapper, fixture, schema, validator, merger-test, other receipt, fragment, registry or known-stale edit. mathlib4 is clean; nothing was pushed.
2. **Freshly recorded, not hand-edited — the decisive check.** I recomputed `checks.positive-proof-receipt/basis-record` against the committed receipt myself:
   - `:source-basis`, `:toolchain`, `:result` and `:fixture` all equal the fresh derivation;
   - **zero fields differ** from a full recompute;
   - `validate` returns `:pass? true`, `:failures []`, with all five component checks true.
3. **Slice mapping.**
   - `predictive`: `5c6f447de2…` → `24e92e8657…`.
   - Added Holes `ProbabilityKernel` pin `5580409932…`. This matches the live post-repair hash I computed independently while attributing the drift, and it is what the dispatch asked for: it exposes the repaired carrier so a same-named alias cannot conceal a carrier change.
   - The alias `PredictiveOutcomeKernel` pin is unchanged (`a87f0d2285…`), as are `Policy`, `Observation`, `Obs`, `clear`, `fixed` and `allPolicyRowsNormalised`.
4. **Nothing weakened.** Both `:declared-not-derived` boundary maps are intact, and the adapter's mappings are unchanged in content (`inductive Policy`, `| .inspect => [clear]`, same expected values and shapes). The file was reformatted from one line to pretty-printed EDN; the structural recompute above covers that.
5. **Gates, re-run by me:**

   | Control | Result |
   |---|---|
   | `predictive_outcome_kernel_witness.clj` | exit 0, PASS |
   | `--negative-unconditional` | exit 0, "negative-control PASS (unconditional rejected)" |
   | `--negative-softmax` | exit 0, "negative-control PASS (softmax rejected)" |
   | re-attestation gate | 26 drifts, predictive receipt absent (0 matches), still exit 1 |
   | fixture bytes | unchanged, `667873b4…` |
   | both negative Lean fixtures | present, mathlib4 clean |
   | clj-kondo / check-parens on `reattest.clj` | 0 errors, 0 warnings / OK |
   | `gates.py` syntax | compiles |

6. **Mutation controls, mine.** On isolated copies: baseline `:pass? true`; zeroing the `predictive` slice gives exactly `[:positive-source-drift]`; zeroing the **newly added carrier pin** does too. So the added pin is load-bearing, not decoration. The canonical receipt was unchanged by my runs.
7. **Meaning.** I read the repaired witness myself. The supports are still the singletons `[clear]` and `[fixed]` with mass one on the named outcomes, and `allPolicyRowsNormalised` still follows from `predictive.normalised p` with no extra premises. codex-2 does not paper over the real difference: the old `mass` returned one even off support, the new one returns zero there. That is a strengthening of the same subject, not a changed claim, and the fixture's two deterministic rows still describe it.
8. **Residual stated honestly.** The receipt records 27 → 26, both exits 1, the other 26 drifts out of scope, no known-stale registration, the R4 admission still blocked, and the declared-not-derived limits preserved. It also marks its own results as author execution pending independent review, which is accurate.

## Findings

- **No defect found.** Every claim in the receipt matched my own runs.
- **The corpus-wide residual is the real story, and it is mine to have missed.** The gate reports 27 drifted pins across 14 receipts; sampling showed each flips exactly at `480a666ad2`, because that repair added two fields to `ProbabilityKernel` and so changed the recorded slice text of every witness declaration that builds a kernel, plus `variationalFreeEnergy` through the precision change. A second wrapper, `observation_kernel_witness`, also exits 1 from the same cause. My WM-01-acceptance-1 review checked mathlib4 builds, controls and axioms but not the futon2 receipts pinned to the declarations that commit changed.
- **Scheduling consequence.** `worklist_check.bb:231-241` invokes this gate, so the futon2 tree stays refused until the remaining 26 pins are re-attested. At one packet per receipt this is 13 more packets; it is better scheduled as one campaign with a shared control, and the empty `positive-receipt-known-stale.edn` must not be used to register the debt instead of clearing it.

## Limits

- One receipt only. I did not re-attest or review any other receipt.
- The adapter and dependency-closure boundaries remain declared-not-derived: nothing here formally verifies the Clojure adapter or a complete transitive closure.
- This deterministic hand-derived witness says nothing about machine-derived Q or empirical adequacy.
- WM-01 is **not** checked.
