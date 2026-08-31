# C226 — Fulab `:prediction-error` call path

Date: 2026-08-31. Discovery only; `fulab.clj` and the pending absence
decision were not changed.

## Verdict

The value Fulab itself produces is **not canonical signed prediction error**.
It is

```clojure
max(0, text-score(outcome) - 1)
```

where `text-score` is word count for strings, collection cardinality for
collections, and `1` otherwise (`adapters/fulab.clj:12-17,201-205`).  It is a
nonnegative **outcome-size surplus** (an engineering complexity proxy), not
`o - μ`.  It should not be named `:prediction-error`; a literal name for the
quantity present is `:outcome-size-surplus`.

This is therefore a live vocabulary/type defect, not evidence that the
canonical signed epsilon is currently being clamped on a production path.
The adapter exposes an untyped public seam where a future caller *could* pass
signed epsilon and have every negative value mapped to zero, but no such caller
exists in this checkout.

## Complete call path

`compute-tau` has three call sites:

1. `select-pattern` passes its public `context` unchanged
   (`fulab.clj:132-138`).  No repository call site constructs `FulabAdapter` or
   calls `fulab/new-adapter`; the only occurrences are the definitions in this
   file.  Consequently there is no producer contract for
   `context[:prediction-error]` and no observed source value.
2. The pattern-action branch of `update-beliefs` passes the public
   `observation` unchanged (`:176-185`).  Again, no in-repository Fulab caller
   exists, so this field has no source producer.
3. The generic-observation branch computes the outcome-size surplus at `:202`,
   associates it under `:prediction-error` at `:203`, and immediately passes
   it to `compute-tau`.  `compute-tau` clamps it again at `:81`, adds it to the
   uncertainty score, and uses the sum as the inverse-temperature denominator
   (`:82-84`).  The value is also emitted under `:aif :prediction-error` at
   `:205`.

History confirms the intended local relationship rather than an AIF residual:
commit `dc1e0a4d` added both the `compute-tau` read and the association of the
locally computed nonnegative value in one change.  It did not connect
`free-energy/compute-prediction-error`.

## Readers and scope

The exact singular key `:prediction-error` has only these Fulab readers/writers
in `src/`:

- reader: `compute-tau`, `fulab.clj:81`;
- writer into its context: generic update, `:203`;
- output emission: generic update, `:205`.

No other source namespace reads `(:prediction-error context)`, and no source or
test namespace requires `futon2.aif.adapters.fulab`.  The canonical signed
producer uses the plural per-channel structure `:prediction-errors`, whose
members carry `:error` and `:producer-contract :prediction-error/v1`
(`free_energy.clj:155-181`).  There is no call path between that producer and
Fulab.

Thus the name collision is presently confined to a dormant/reference adapter,
but it is semantically hazardous: `:prediction-error` means signed `o - μ` in
the glossary/Lean/free-energy path and nonnegative outcome-size surplus in
Fulab.  The `(or ... 0.0)` absence decision remains Joe's existing decision and
is untouched.  Changing the clamp or temperature behavior is outside this
discovery.

The full workspace gate was intentionally skipped under C222.
