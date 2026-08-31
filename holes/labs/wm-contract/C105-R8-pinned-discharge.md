# C105 — pinned R8 source discharge

Date: 2026-08-31

`wmTraceR8` is now the exact 792-row literal emitted in
`R8-D3-report.lean`: no rows, dates, presence fields, or free-energy shapes are
compressed.  Its immutable source identity is 53 files / 792 forms at content
pin `c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`,
using `:sha256-over-newline-joined-sorted-form-sha256`.  This is the corpus at
that pin, not “the corpus now”; later append-only growth belongs to the C44
watermark delta.

`r8CensusWmTrace` now applies `native_decide` to that same source constant and
proves its snapshot census is `(755, 32, 5)`.  The previous generated theorem
proved the generated sibling only; installing the identical literal removes
that proof-about-a-copy gap.

`checks/r8_pinned_snapshot_witness.clj` checks both the recorded pin/census and
literal equality between the source and generator output.  Its two independent
controls mutate the pin and the stored-F census count respectively.
