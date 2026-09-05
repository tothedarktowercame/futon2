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
pointers, and suffixes resolving to more than one file.  The review added one
more: `:error/collision-not-recomputed`, for a declaration naming a fold that
is not a collision at all.  Resolution searches
the futon2, p4ng, and mathlib4 repositories, excludes `.git` and `target`, and
matches the registry's relative path as a suffix rather than selecting a bare
filename by root order.

Section 13 of `p4ng/empirics-futon/negative_controls.sh` plants every refusal.
Its capital-Pi control removes only the `pi` declaration and requires all three
members in the diagnostic.  A separate plant introduces a previously unknown
`mu` collision.  Another gives differently spelled rows one runtime key.  The
two accepting controls reverse member order and check the committed registry.

## Independent count disagreement

CORRECTED BY THE REVIEW; the paragraph as delivered is kept below the
correction because the number it reports is what the review found wrong.  The
checker now reports 31 rows, five case-fold groups, **86 pointer claims over 67
distinct locations**.  As delivered it reported "77 distinct pointers" for a
list it never deduplicated: the 70 column claims stand over 60 locations,
because eight locations are cited by more than one row (`sec-glossary.tex:15`
by four; `MachinePrecision.lean:101`, `sec-glossary.tex:31`, `:39`, `:62`,
`bmr.clj:108-134`, `efe.clj:472-727`, `free_energy.clj:203-278` by two each),
so ten claims were counted a second time inside a number whose own label said
they were not.  Claims and locations are different quantities; both are now
reported as themselves, and section 13m pins both.  What is checked for
resolution is the 67 locations.

As delivered:

> The checker finds 31 rows, five case-fold groups, and 77 distinct pointer
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

The seven added in-string locations are unchanged by the correction; only the
label and the claim total moved.

## Review repairs (the reviewing seat, not re-dispatched)

1. The pointer count above, and the ACCEPT line and section 13m that carried
   it.
2. `:error/collision-not-recomputed`.  The member-mismatch check was guarded on
   the fold having a recomputed group, so a `:collisions` entry for a fold with
   one member -- or none -- fell through every check and was accepted.  Every
   commissioned control plants a collision the registry fails to declare; this
   is the same defect reflected, and nothing pointed that way.  Control 13n.
3. A control for `:error/ambiguous-pointer`, which the delivery implemented and
   no control exercised.  Control 13o; futon2 carries two `efe.clj`, and three
   rows point into the long one.
4. `pointer_check.bb` now scans `symbol-concordance.edn` (the slice-1 evidence's
   first item for this slice), with `AIF_EXTRA` so the extras list is reachable
   from a negative control at all -- every existing pointer control drives
   `AIF_EQ`, which switches the extras off, so nothing in that list had ever
   been exercised on a planted defect.  Controls 4e4, 4e5 and the file-count pin
   in 4f.
5. The plant suite's own summary line was dropped by `System/exit` before `*out*`
   was flushed, so a run with findings and a run without ended on the same last
   line; it now flushes and exits non-zero on a finding.

## What was deliberately not done

`pointer_check.bb`'s extension regex is NOT widened to `.tex`.
`slice2-tex-pointer-blast-radius.txt` measures what that would cost the shared
gate: admitting `.tex` and `.md` pulls 240 further pointers into scope from
`worklist.edn` alone, 22 from `aif-equations.edn` and 2 from
`control-map-edges.edn`, resolved against a bare-filename roots allowlist whose
own comments record it falling behind nine times.  The nine non-`clj|lean|edn`
pointers in the concordance are checked by `symbol_concordance_check.bb`, which
resolves by relative-path suffix against three repository roots and refuses an
ambiguous match, so they are not unchecked -- they are checked by the registry's
own gate rather than by the shared one.  Widening the shared gate is its own
item.

This account is not a notation ruling and changes no registry or ledger.
