# C116 — three actionable source proofs

Date: 2026-08-31

- `r9TwoRunCensus` is proved by simplification over the compact source tables.
  Compression is sound here because the theorem reads only table length and
  verdict fields, all determined by the source constructors.
- `r9WmPerRowDeclarations` is proved by simplification over the same source
  `wmVerdictsDeclared`; it reads declaration-source tags and therefore unfolds
  `declaredVerdictRow` rather than using a generated sibling.
- `r8EraBoundary` is proved by `native_decide` over the full, uncompressed
  792-row `wmTraceR8`, because the theorem reads each row's field presence,
  free-energy shape, and date.

Controls are declaration-specific: removing a ledger row falsifies the two-run
census; changing O7's declaration source while preserving rows and verdicts
falsifies only the per-row declaration law; and inserting stored F into a
pre-boundary row falsifies the era law.
