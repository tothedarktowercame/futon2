# PLAN — A as an end-to-end programme (codex-3 strategy, adopted 2026-09-20)

Provenance: Joe commissioned codex-3 directly ("I don't want a simple
'next step' work plan but an end-to-end strategy for dealing with A. It
should not block work now, and we should get a viable result at the
end."), noting the DAG's two A pilots were parked as isolated
post-deadline items while downstream work waited on their substance —
deferral made self-perpetuating. codex-3's reply delivered
2026-09-20 in *codex-repl:codex-3* (from line 45); preserved here
because the buffer is ephemeral. Joe, this afternoon: C is specified
enough to implement; "we should move on to A."

## The organising decision (codex-3, verbatim)

"Stop treating 'non-degenerate A' as the deliverable. The deliverable
is an applicable, calibrated observation model that the whole
controller actually uses."

## The seven points (condensed; the buffer text is authoritative)

1. **A is a model answering likelihood/query requests**, not a map of
   per-token error rates. Declaration names: state/observation
   variables (incl. unavailable observations), observation procedure +
   token classes + observation time, dependency structure + parameters
   + their evidence, supported queries + computational limits. First
   coupled model = the DAG's common-cause form
   A(o|s) = Σ_z P(z) Π_i P(o_i|s_i,z), z the shared observer
   condition. Exact checks stay deterministic inside it. The 15%
   "bad-day" is a DECLARED EXPERIMENTAL model, never an empirical
   estimate. Shared causes affecting work success (not observation
   error) belong in the transition model, not A.
2. **Small-model route immediately**: the exact enumeration experiment
   (runs/coupling-sidebyside-2026-09-18/sidebyside.clj — verified on
   disk with output.txt) becomes the reference implementation behind
   the same interface production will consume. Three configurations:
   exact checks, independent errors, coupled errors. Exercised over
   the full path model -> prediction -> observation -> likelihood ->
   belief update -> F/G -> selection -> recorded outcome. Synthetic
   parameters visible in every record; no calibration authority from
   passing tests. Remains the correctness oracle permanently.
3. **WMC as the scaling implementation** (PILOT-coupled-A-wmc,
   github.com/nttcslab/variance-wmc; BN -> ENC2 CNF -> SDD -> WMC),
   verified against enumeration at n<=10. Acceptance MUST include
   joint events: same marginals, yet "all five tokens missed" is ~469x
   more probable under coupling (recorded in the sidebyside output) —
   marginal-matching would miss the entire intervention. The
   one-common-cause model also answers fully-specified observations by
   summing two products — early use without the compiler.
4. **Finish the consumers**: F's observation-to-prediction matching
   fixed independently of A's noise; G's risk/ambiguity under
   dependence (per-token sums are wrong for coupled distributions;
   exact enumeration first). Known control: for point-mass state +
   product preference, coupling's risk increase and ambiguity decrease
   CANCEL in total G — a better A need not change every ranking; tests
   demonstrate both the legitimate invariances and the cases where
   coupling changes decisions. Certificates record the model and
   values actually consumed, including the joint path.
5. **Calibration commissioned concurrently, bounded population**: one
   real judgement task, one defined observer procedure, explicit class
   mapping (no silent pooling of the pilot's groups). Paired procedure
   verdicts + independently reviewed reference labels, pinned evidence,
   reference-present AND reference-absent cases, abstentions retained,
   sampled by batch/session so shared failures are distinguishable
   from independent ones. The existing pilot informs this design and
   is not calibration data (consistent with the standing
   pilots-inform-research ruling). Held-out validation; if the data
   cannot identify the latent mechanism, RETAIN that uncertainty.
6. **Parameter uncertainty separate from coupling**
   (PILOT-variance-wmc-rate-uncertainty): coupled A asks "can
   observations fail together"; variance-WMC asks "how uncertain are
   query probabilities given uncertain parameters." Fixed-parameter
   inference first; then propagate justified parameter uncertainty;
   means AND variances verified against small-model calculations;
   rankings/intervals need their own propagation. Results direct which
   adjudications reduce decision-relevant uncertainty.
7. **Incremental production admission with a concrete finish**: the
   programme completes on ONE real decision family where A includes
   the judgement observations the task needs; calibration + held-out
   validation support the declared scope; coupling tested and
   supported or constrained; F matched, G dependence-correct; real
   runs record model/inference/selection/outcomes reproducibly;
   runtime and uncertainty adequate; drift detection triggers
   reassessment. Enumeration agreement closes the computational
   pilot; it does not close production A — and calibration does not
   hold integration hostage.

## Integration with this week's rulings (claude-12)

- Point 5 IS the "real research" the pilot ruling calls for; the
  pilot's design lessons (mechanical summaries, retention-before-
  observation, no absence vocabulary, no deliverable-naming) apply to
  its collection design.
- Joe's per-model self-correction channels (Claude: refused handoffs;
  Codex: refusal-at-intake; GLM/Zai: in-turn) refine point 5's
  "observer procedure" choices and point 1's token classes; the 9/46
  measures the Codex-intake channel only.
- The J-class locator slot reserved unrated in the C replacement
  declaration is where point 2's configurations first touch a live
  declaration; nothing there commits to rates.
- The counted-denominator discipline (never a trawled numerator over
  an unrecorded denominator) governs any rate that enters point 5.

## Status

Adopted as A's plan of record 2026-09-20. Work under it does not block
and is not blocked by the C packets in flight. First implementable
piece: point 2 (reference implementation behind the production
interface), dispatch shape TBD with claude-4 as build owner.
