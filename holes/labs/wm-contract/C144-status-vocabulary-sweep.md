# C144 — status classification vocabularies

Date: 2026-08-31

## What contract-lint `:witnessed` meant

The sole declaration was `wmRunConformsToWiring`.  In `contract_lint.clj`, the
judgement is selected only after a hole has evidence, a registry binding, a
readable/current fixture, inspectable acceptance, and a passed run, but its
evidence type returns `:shape-check-not-implemented`.  Thus `:witnessed` meant
**binding passed, evidence shape unchecked**.  It did not mean Lean's
`WITNESSED-INSTANCE OBLIGATION`.

The report now displays that judgement as
`binding-passed-shape-unchecked` and names the declaration.  Lean's separate
label census displays `witnessed-instance-obligation=5`.  The underlying
instrument vocabularies remain legible; the report qualifies them where they
meet.

## Vocabulary sweep

- Contract-lint `:conformant` means a hole's bound fixture passed its implemented
  evidence-shape checker.  Lean has no `CONFORMANT` declaration label.  The
  report calls it `shape-conformant`.
- Contract-lint `:refused-implementation` and Lean
  `DELIBERATE IMPLEMENTATION REFUSAL` currently identify the same three holes,
  but one is a lint judgement and the other an authored source label.  The
  report renders `implementation-refused` versus
  `deliberate-implementation-refusal` so equality is observed, not assumed.
- Contract kind `closed` is rendered as the population count `closed`.  Lint
  judgement / Lean label `closed-by-record` is its provenance grammar; neither
  is a proof-strength claim.  The registry does not use `closed` as a result.
- The witness registry's result vocabulary is `passed` (15 rows), not
  `conformant`, `closed`, `refused`, or `witnessed`.  Its `:witnesses` field is
  an identifier linking evidence to a declaration, not a classification.

No other cross-grammar display collision was found.

## Control

`make status-control`, also run by the workspace gate, now checks both source
independence and display-vocabulary disjointness.  It injects a contract display
term into the Lean display vocabulary and must reject the collision.  This is
the failure mode C144 closes.

