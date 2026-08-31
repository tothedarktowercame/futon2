# C102 — P-R8 §solved 1 hole census

Date: 2026-08-31

## Result

The two holes split as **1 dischargeable / 1 blocked / 0 false**.  Neither has
the degenerate-universal defect: `wmTraceR8` is data, and
`r8CensusWmTrace` is a proposition about that one data object rather than a
universal over an arbitrary carrier or function.

| Declaration | Claim | Existing evidence | Classification |
|---|---|---|---|
| `wmTraceR8` | The source constant is the 792-form R8 snapshot transcribed as `List R8TickLit`. | `R8-D3-report.edn` pins 53 files / 792 forms by `c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`; `R8-D3-report.lean` contains the corresponding 792-row `wmTraceR8Generated`. | **Dischargeable, non-trivial:** install the exact pinned literal (or a lossless generated source module) as `wmTraceR8`, then check its digest. A disposition-only compression is not the same object: `r8EraBoundary` also reads every row's date, shape, stored-F, and selection-gain fields. |
| `r8CensusWmTrace` | `r8Census wmTraceR8 = (755, 32, 5)` for that pinned snapshot. | `generatedCensus` proves the triple by `native_decide`, but only for `R8GeneratedFixture.wmTraceR8Generated`. The report supplies the tick ids in all three dispositions. | **Blocked on `wmTraceR8`:** after the exact source object is installed, the same `native_decide` proof is available. Until then, using `generatedCensus` would assert an unproved source/generated equality. |

## Snapshot versus live growth

The triple `(755, 32, 5)` is an exact property of the immutable content pin
above.  It is not “the corpus now”.  C35/C44's live R8 gate instead classifies
growth after its watermark as `:append-only-growth`.  New trace forms therefore
do not make this pinned census false, and must not cause its literal to be
silently refreshed.  The pin and the live delta are different objects with
different consumers.

## Proof-about-a-copy finding

The old evidence establishes:

```lean
r8Census R8GeneratedFixture.wmTraceR8Generated = (755, 32, 5)
```

It does **not** establish the contract claim about `Holes.wmTraceR8`; no equality
between those constants is stated.  This is the same proof-about-a-copy defect
found in C99.  Rebinding is legitimate only after the pinned generated literal
becomes the source object, or after a checked equality to that source is added.

No declarations were discharged in this census pass: installing and validating
the exact 792-row source is real adapter work, not a trivial proof move.
