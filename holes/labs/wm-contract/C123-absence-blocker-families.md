# C123 — remaining absence blocker families

Date: 2026-08-31. Result: 11 sites collapse into four migration families,
but the largest family is not one already-authorised behavioural change.

## Census

Authority: `checks/absence-coercion-dispositions.edn` after C118 and C120.
Canonical command: `bb -cp . checks/preemptive_absence_coercion_lint.clj`.
Before and after this design pass: **11** live blocked rows, all in futon2.
The disposition file still covers all 18 C12 rows exactly:
`{:fix-now 6, :exempt-with-reason 1, :blocked 11}`.

## Family 1 — observation support consumers (6 sites)

Sites:

- `observation.clj:152` — numeric vector projection;
- `free_energy.clj:23` and `:50` — channel gap and its caller;
- `free_energy.clj:61-63` — epistemic diagnostic;
- `free_energy.clj:138-143` — mode inference;
- `policy.clj:144-145` — fallback policy's sorry-pressure read.

One carrier change can reach all six: make the observation consumer boundary
accept a tagged channel reading (`:observed` with value, or reason-bearing
`:absent`) rather than independently calling `get`/keyword lookup with zero.
The carrier itself already exists as `observation-status` and
`observation-envelope`; this family therefore does not need another generic
wrapper.

It does **not**, however, collapse into one behavioural switch. Its consumers
ask four different questions:

1. vectorisation: whether an absent coordinate is refused or represented by a
   mask/value pair;
2. pragmatic and epistemic scoring: C98 decided support-aware omission, but
   deliberately left the live switch unauthorised;
3. mode inference: whether incomplete mode features yield an unknown mode or a
   partial classifier;
4. fallback policy: whether unknown sorry pressure permits learning/no-op,
   requires abstention, or defers to another selector.

Thus a shared API migration can reduce implementation duplication, but marking
all six fixed requires three still-unmade runtime decisions plus the separately
staged C98 scoring switch. I did not make or hide those decisions here.

## Family 2 — prediction measurement record (2 sites)

Sites:

- `free_energy.clj:98-100` — observed/mean/variance defaults inside prediction
  error computation;
- `adapters/fulab.clj:81` — missing prediction error becomes zero while
  computing tau.

One versioned `PredictionMeasurement` record can unblock both:
`{:status :present :observed o :mean μ :variance σ² :error ε}` versus
`{:status :absent|:malformed :reason ...}`. C120 versioned the downstream
precision records, but these are upstream producer/adapter boundaries. The
remaining decision is whether a missing measurement makes tau unavailable,
retains a declared legacy variant, or refuses the adapter call. That is not a
numeric default decision and was not made here.

## Family 3 — rollout move score (2 sites)

Sites:

- `rollout.clj:129` — prior fallback exponentiates missing `:score` as zero;
- `rollout.clj:158` — missing step delta falls through to missing score, then
  zero.

One typed `MoveScore` producer can unblock both: a move carries a scored
variant with its score/prior/delta provenance, or an explicit `:unscored`
variant. `renormalize-priors` and `move-cost` must then refuse or exclude the
same unscored variant rather than invent separate defaults. The unresolved
choice is whether unscored moves are excluded from rollout support or abort the
rollout; changing that changes search and is not authorised in this pass.

## Family 4 — belief aggregation validity (1 site)

Site: `belief.clj:1040-1052`. This consumes a map of prediction-error records
and independently defaults missing weighted error, precision, or the entire
annotation-health entry. C120 distinguishes legacy from malformed records at
the precision producer, but aggregation still accepts an unvalidated
collection. One `ValidatedPredictionErrors` boundary would unblock this site;
its decision is whether malformed/partial collections refuse belief update or
aggregate only present channels. This is independent of the scoring switch.

## Conclusion

The 11 names are not 11 unrelated migrations: they form **4 implementation
families with site counts 6 / 2 / 2 / 1**. Nor are they four mechanical fixes.
The common carrier for the largest family already exists; what remains is
consumer policy. Treating that as one API patch would merely move the silent
defaults behind a shared function.

No family was changed because the largest requires explicitly unauthorised
scoring behaviour plus two additional decisions. The next useful operator
decisions are:

1. incomplete mode features: unknown mode or partial classification;
2. unknown sorry pressure in fallback selection: abstain or continue;
3. unscored rollout moves: exclude or refuse the rollout;
4. malformed prediction measurements/collections: refuse, or retain a named
   legacy variant.

## Gates

No code, scoring, policy, selection, or actuation changed. Lint remains 11.
The current full gates remain green: futon2 1,037 tests / 6,203 assertions and
futon3 248 tests / 1,518 assertions, zero failures/errors. This classification
unit scores 5/7 for automation: the census, sites, consumers, and bounded
families are pinned; the behavioural acceptances and decisions are expressly
refused rather than inferred.
