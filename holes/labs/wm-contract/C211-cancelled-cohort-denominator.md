# C211 — cancelled attempts outside cohort 46's denominator

Date: 2026-08-31

## Change

Cancellation remains a valid recorded terminal outcome, but no longer consumes
cohort 46's preregistered denominator or stopping window. The immutable dossier
is retained under semantic stratum `:post-preregistration/cancelled`, with its
attempt identity and ordinal intact (`full_loop_cohort.clj:371-400`). The live
cohort counters and `:outcomes` are derived only from non-cancelled attempts.

Admission uses that same denominator (`full_loop_cohort.clj:258-266`). A
cancelled dossier therefore cannot make the runner report that cohort 46 is
complete. Physical ordinals remain monotone over every recorded dossier, so
the history does not reuse an identity merely because an attempt left the
cohort's analytic world.

The preregistration was not amended. Its SHA-256 remains
`e9031b3c66173bf10eb91cc38d43a7b750b1f3c24007c9340d506101e0c8f1ff`.

## Existing evidence

A filesystem census found **62** current (non-archive) full-loop attempt
directories, **58** with closed checkpoints, and **0** closed as `:cancelled`.
Thus no existing cohort result is diluted by cancellation. The packet's “72”
does not describe the current attempt-directory population; 72 is the referent
population reported by the drift detector. No published cohort correction is
needed.

Canonical census:

```sh
find data/wm-full-loop -maxdepth 2 -type d -name 'attempt-*'
find data/wm-full-loop -path '*/attempt-*/*closed.edn' -print0 \
  | xargs -0 rg -l ':outcome\s+:cancelled'
```

## Control

`cancelled-attempt-is-recorded-outside-preregistered-denominator` constructs a
complete attempt, closes it as `:cancelled`, and establishes all four claims:

- cohort `:attempt-count` remains 0 and `:remaining` remains 40;
- `:recorded-attempt-count` is 1;
- the cancelled attempt id appears under
  `:post-preregistration/cancelled`;
- the next eligible attempt is admitted, retains a distinct ordinal, and makes
  the cohort denominator 1.

Invocation:

```sh
clojure -X:test :nses '[futon2.aif.full-loop-cohort-test]'
```

Result: 10 tests, 39 assertions, 0 failures, 0 errors.

