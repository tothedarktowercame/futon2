# C530 — F8 refusing symbol-concordance checker

Date: 2026-09-05

`symbol_concordance_check.bb` turns the slice-1 census into a refusal boundary.
It accepts only schema `:wm/symbol-concordance-v1`, a non-empty population with
unique row ids, recomputed lower-case folds, completely and accurately declared
collision memberships, pointer-backed collision declarations, typed column
absences, and resolvable unambiguous pointers.

## Two relations, kept separate

`:relation/case-fold` compares lower-cased `:bare` strings.  It recomputes the
groups and treats stored `:fold` as a checked transcription, not an input.
`:relation/runtime-key` extracts the first keyword-shaped token from
`:runtime/:var` and checks reuse independently of display spelling.  This is
the distinction requested by `C529-F8-symbol-concordance.md:75-84`: two rows
may spell their symbols differently and still overwrite the same runtime key.

Every run prints both relation names and prints the limit
`:reading-collisions-not-detected`.  Policy precision `gamma` and evidence
precision `Pi` are a conceptual collision documented at
`C461-beta-gamma-discovery.md:14-45`, not a case-fold collision.  The checker
does not choose a preferred reading, rewrite the registry, or treat member
order as precedence.

## Refusals

The checker emits the requested typed errors for a foreign schema, empty
population, duplicate ids, drifted stored folds, undeclared case-fold and
runtime-key collisions, inaccurate or unresolved collision members,
collisions without basis pointers, untyped column absence, unresolved
pointers, and suffixes resolving to more than one file.  Resolution searches
the futon2, p4ng, and mathlib4 repositories, excludes `.git` and `target`, and
matches the registry's relative path as a suffix rather than selecting a bare
filename by root order.

Section 13 of `p4ng/empirics-futon/negative_controls.sh` plants every refusal.
Its capital-Pi control removes only the `pi` declaration and requires all three
members in the diagnostic.  A separate plant introduces a previously unknown
`mu` collision.  Another gives differently spelled rows one runtime key.  The
two accepting controls reverse member order and check the committed registry.

## Independent count disagreement

The checker finds 31 rows, five case-fold groups, and 77 distinct pointer
claims: 70 column pointers plus seven additional in-string locations not
duplicated by a column claim.  Those seven are
`MachinePredictionError.lean:129-135`,
`MachinePredictionErrorWitness.lean:152-156`,
`aif-equations.edn:86-91`, `aif-equations.edn:158-160`,
`aif-equations.edn:161-165`,
`C277-model-reduction-free-energy-change-binding.md:5-8`, and
`sec-glossary.tex:47`.  All seven resolve uniquely and within bounds.  The
dispatch supplied 76; subtracting one of these without a stated rule would
weaken the pointer check, so section 13 pins the independently reproduced 77
and leaves the disagreement for review.

This account is not a notation ruling and changes no registry or ledger.
