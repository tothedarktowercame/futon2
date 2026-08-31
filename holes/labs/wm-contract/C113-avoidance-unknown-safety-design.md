# C113 — avoidance unknown safety design

Date: 2026-08-31. Status: decision boundary located; no behaviour change.

## Present behaviour

The “two sites” are one semantic path:

1. `src/futon2/aif/free_energy.clj:31` — `in-avoided?` coerces a nil
   observation to `0.0` before range membership.
2. `src/futon2/aif/free_energy.clj:70` — `compute-controller-diagnostics`
   supplies `(get obs k 0.0)` to that predicate for every avoided channel.

This is not presently an action guard. The result is `:avoided-active`; its
only production consumer is `scripts/futon2/report/war_machine.clj:4684-4693`,
which maps it into reader-facing `:losses`. Repository-wide search finds no
policy, selection, gate, or actuator consumer of `:avoided-active` or
`:losses`. Therefore the current action-boundary behaviour is **contribute / 
non-blocking**, regardless of the diagnostic.

The diagnostic itself behaves inconsistently under absence because zero is
treated as observed:

- absent `:consulting-pct` and `:active-repo-ratio` falsely violate their
  avoided ranges `[0.0 0.0]` and `[0.0 0.2]`;
- absent `:stack-pct`, `:ticks-firing-ratio`, and `:sorry-count-norm` falsely
  satisfy their guards because zero lies outside their avoided ranges.

Thus the existing behaviour is neither a deliberate fail-open nor fail-closed
policy. It is value-dependent fabrication followed by an informational report.

## Existing vocabulary

R5's local commitment is a static avoided-range diagnostic over observation
channels (`preferences.clj:26-33`), while `free_energy.clj:15-16` retains WM-I4:
priorities are informational, not commands. Nothing there authorises turning a
diagnostic into an actuator veto.

The C-vector's entry status is not a tri-state guard vocabulary. `c-entry`
defaults to `:open` (`c_vector.clj:48-56`), and risk contributes only while
status equals `:open` (`c_vector.clj:94-101`); it defines no `:unknown` state.
The relevant existing type is instead the observation envelope's
`:observed` versus reason-bearing `:absent` variant. Reusing C-entry `:status`
would conflate goal lifecycle with observation availability.

## Required shape before a safety decision

The diagnostic should eventually expose, per avoided channel:

```clojure
{:channel ch
 :guard-status :satisfied|:violated|:unknown
 :observation {:variant :observed :value number}
 ;; or
 :observation {:variant :absent :reason keyword :paths [...]}
 :avoided-range [lo hi]}
```

Unknown must first become representable and persist in the trace. That change
can repair the false diagnostic without changing selection: unknown means
“not evaluated,” never satisfied or violated.

Only a separate proposal to make some avoided ranges **hard safety guards**
creates the abstain-versus-contribute decision. That proposal must name which
channels are safety-critical and its actuator consumer. Under the machine's
current sovereignty commitment, silently promoting all five informational
ranges to vetoes would be a semantic and authority change.

## Decision/refusal

I do **not** choose abstain or contribute here. The current system has no such
guard, so choosing a branch would also—without authorisation—create and arm a
new actuator gate. Joe's decision is:

> Should any R5 avoided-range diagnostic become a hard action guard? If yes,
> which named channels, and should unknown on each named guard force abstention?

Until then, the honest design is staged:

1. typed tri-state diagnostic and trace evidence, still informational;
2. measure unknown populations forward;
3. separately authorise and implement any named hard guard.

## Measurement

Historical impact is unknowable. The pinned corpus has 54 files, 801 records,
and 105,277 candidates, but zero records with observation presence provenance.
Flat zero cannot distinguish measured zero from absent input. The first
post-v18 tick can count unknown diagnostics; no such record exists yet. This is
another instance of why envelopes must be persisted by default.

## Gates

- No source, scoring, selection, or actuation behaviour changed.
- `bb -cp . checks/preemptive_absence_coercion_lint.clj` remains at 15 known
  findings; these two remain blocked on the operator decision above.
- The immediately preceding full gates on this same source state remain green:
  futon2 1,033 tests / 6,189 assertions and futon3 248 tests / 1,518
  assertions, with zero failures/errors. C113 changes documentation only.
- Canonical consumer audit:
  `rg -n 'avoided-active|:losses' src scripts test`.
- Canonical historical census:
  `bb -cp . checks/absence_scoring_counterfactual.clj`.

This design unit scores 5/7 for automation: ports, executable corpus checks,
consumer, pinned reads, and bounded staging are present; acceptance for an
armed guard and the action-boundary decision deliberately remain Joe's.
