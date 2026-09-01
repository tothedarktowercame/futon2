# C376 — adversarial review of PositiveLeanWitnessReceipt/v1

Date: 2026-09-01. Representative mutation target:
`softmax-positive-receipt.edn`. All mutations were in-memory; no receipt, Lean
source, fixture, registry, or generated witness was edited.

## Verdict

The receipt reliably detects textual change inside every declaration slice it
is actually given, and an absent elaboration result rejects. It does **not**
establish that the required slices or adapter exist, that adapter fields name
anything in Lean, that semantic dependencies are complete, or that elaboration
succeeded with an allowed axiom set. The schema therefore overclaims the four
properties which distinguish it from a declaration hash.

## Counterexamples

Each mutation below returned `:pass? true` against the live toolchain:

| Mutation to a valid Softmax receipt | Result |
|---|---|
| `:source-basis []` | pass |
| remove the entire `Holes.lean` semantic-dependency entry | pass |
| `[:adapter :mappings] []` | pass |
| remove `:adapter` entirely | pass |
| change a mapping's `:lean-field` to `"unrelatedField"` | pass |

The adapter validator compares only `:expected` with the EDN fixture path.
`:kind`, `:lean-declaration`, and every `:lean-field` are uninterpreted labels.
An empty or absent mapping passes through vacuous `every?`. Thus the explicit
fixture-to-Lean adapter is recorded but not bound.

The source-basis validator recomputes only the supplied list and compares it to
itself. It has no required theorem slice, nonempty witness slice, or dependency
closure. Removing a semantic dependency or every declaration therefore creates
no discrepancy. A changed instance, notation, coercion, or imported definition
outside the author-selected slices can alter meaning without appearing in the
receipt. Locality works for unrelated edits, but “related” is an unaudited
author assertion.

## Weakening scope

When the theorem declaration remains in a populated source basis, all proposed
subtle weakenings—stronger hypothesis, generalized conclusion, concrete former
variable, or narrowed implication antecedent—change the declaration bytes and
are rejected as source drift. The weakness is not in comparing a named slice;
it is that the schema permits the theorem slice and dependency slices to be
removed. A weakened live theorem can be re-receipted with an empty basis and
still pass.

## Elaboration and axiom result defect

`validate` tests equality between the recorded and live result; it never
requires `:exit 0`, a vector of axioms, or an allowed axiom policy. The current
tree contains **30** PositiveLeanWitnessReceipt files:

- 19 record exit 0;
- **11 record exit 1**;
- **12 record `:axioms nil`**.

A reproducibly failing `#print axioms` invocation therefore validates. One exit
0 receipt also has nil axioms, so nil does not distinguish axiom-free output
from parser/failure absence. An absent `:result` was correctly rejected, but
absence is stricter than a recorded failure.

Likewise, a transitive `sorryAx` would be visible in successful `#print axioms`
output, but the schema merely records and reproduces the vector. It contains no
rule rejecting `sorryAx` or requiring justification for nonstandard axioms. A
receipt recorded after such a dependency entered would accept it. Pinning a
named dependency catches later drift; it does not make the initially recorded
axiom set acceptable.

## What remains supported

- The baseline Softmax receipt validates on the current toolchain.
- Any textual mutation inside a retained named declaration changes its digest.
- Fixture byte digest and schema are checked.
- Fixture-path expected values are checked for every nonempty supplied mapping.
- Toolchain/version identities are live-compared.
- Removing `:result` rejects as `:elaboration-or-axiom-drift`.

These are useful component checks, but they do not presently justify calling
the carrier a complete positive-proof receipt. This delivery reports only and
does not repair or regenerate the population.
