# C73 — inspectable find F1–F3 rebinding

Date: 2026-08-31

The C65 checker now has zero antecedent-representation mismatches and validates the three bound laws directly. Its independent controls are:

- F1 `--negative-f1`: inject a selected pattern outside the repository; containment rejects it.
- F2 `--negative-f2`: remove the receipt for a selected pattern; receipt completeness rejects it.
- F3 `--negative-f3`: replace a selected pattern's receipt with score-only evidence; non-self-certification rejects it.

The earlier `--negative` control still rejects reintroduced duplicate antecedent text. `findF4Falsifiable` remains unbound because these checks establish only six concrete omitted-pattern observations, not its repaired Lean predicate.

Canonical strict qualification now exits 0 with fresh and inspectable bindings.
