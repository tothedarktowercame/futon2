# Declared step-indexed C, review against 1a3a7680

Implementation c322c22f; main merge 7d1d7e85.

Both live declarations supply :c-schedule with :placement
{:value :terminal :status :declared} and :elsewhere
{:value :uniform-over-non-ruled-zero :status :declared}. Loader and assembly
retain it; live-c supplies it; the judge's joint merge and efe/rank-actions
retain it. Differing family schedules refuse, rather than selecting one.
Legacy absent schedules remain explicitly defaulted every-step at the loader.

The evaluator selects the terminal preference at tau=T and the declared
uniform distribution over allowed outcomes earlier. C remains exactly zero
on every ruled-zero outcome. Each consumed step retains a serializable full
distribution: universe, exact additive log weights, and zeroed outcome sets.
These are the members used by the risk calculation, not rollout-support-only
probes. No powerset is materialized for scoring. The verdict compares those
distributions, including equivalence when zero exclusions constrain support;
form labels alone cannot determine its answer. Missing step data stays missing.

Controls actually returned:
- terminal versus uniform earlier: :non-degenerate, :varies-across-horizon,
  :C-steps-count 2, even with a deliberately wrong :constant-spec form label.
- constant preference at both steps: :degenerate, :constant-across-horizon,
  :C-steps-count 2, even with a :step-indexed label.
- one allowed outcome after zero exclusions: correctly constant despite
  differing weight representations; excluded outcome log probability -Inf.
- loaded declaration -> assembly -> live-c -> judge merge -> efe/rank-actions
  -> consumed C verdict: :varies-across-horizon, count 2. This control initially
  exposed efe dropping :c-schedule while reconstructing spec; now fixed.

## Nine rows, specification 1a3a7680

| Row | Review |
|---|---|
| 1 | Pass: nonempty EoI wants remain absent; loader/assembly controls pass. |
| 2 | Pass under recorded relaxation: each entry names community, with explicit not-consumed status. No ownership wiring claimed. |
| 3 | Not exercised: one effective spec, no cross-owner zero sets. Multi-owner enforcement remains queued. |
| 4 | Pass: C6 reference-resolution control rerun, non-resolving witness refuses :unknown-sha; no true observation. |
| 5 | Pass: zero exclusions retained at every step; exclusion control passes. Existing no-overlap fallback remains labelled :derived-no-overlap. Existing-record validity unchanged. |
| 6 | Pass: declaration scales retained; lam=2 and invalid lam=0 controls rerun by witness-reference namespace. |
| 7 | Pass at the due step: terminal coverage preference preserved. Earlier neutrality is declared under row 9, not disguised preference for stalling. |
| 8 | Single-owner conservative case only; no claim of implemented multi-owner scoring. |
| 9 | Pass: declared terminal/elsewhere schedule reaches actual evaluator and consumed per-step distributions determine verdict. Both required controls pass. |

No WM click: production acceptance in-domain/count/verdict remains deferred.

Validation: preference-family 3 tests/20 assertions (repeated by registry on
merged main); decomposition replay 6/78; bounded observation route 7/132;
witness-reference rows 1-8 controls 3/27, all pass. Bounded coupled F remains
5.413103575948838. clj-kondo 0 errors/0 warnings; check-parens OK.
wm_run_validity output byte-identical to the preceding witness-reference
baseline, VALID 4/5 with unchanged F-not-consumed flag.

Scope limitation: the nonzero-rate factorized evaluator does not implement
this C family and refuses :c-family-unsupported-with-rates rather than ignore
the schedule. Production zero-rate sparse scoring and bounded enumeration
support the family. No A rates, D derivation, F calculation or Q dynamics were
changed. Only C inputs/consumption/records were changed in the scoring paths.

Warrant test-registry-e1771affb8abf09606a9ed8a4dd8262aa0c77ffdfb99a1ade962255ccdc093a6
Subject WM-step-indexed-C-family. HTTP current-validity check true.

Reload in order: futon2.aif.cascade-model-manifest,
futon2.aif.cascade-observation-scoring, futon2.aif.g-term-decomposition,
futon2.aif.live-c, futon2.aif.cascade-sources, futon2.aif.cascade-problems,
futon2.aif.efe, futon2.report.war-machine. Earlier C6 observation-checks reload
is also needed if still pending. No serving reload performed.
