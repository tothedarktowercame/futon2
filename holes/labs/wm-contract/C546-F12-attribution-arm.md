# C546 — `:F12` slice 8: the attribution arm, run and priced

Basis: mathlib4 `0251291563` (`F12AttributionArm.lean`, dispatched to codex-1) +
`4930505dc1` (review repairs and additions); futon2 (this commit)
(`f12_attribution_check.bb`, `runs/F12-organise/06-attribution-arm.edn`).

`Holes.lean` and `holes-contract.json` are untouched: `git log -1` on both still
reads `61c4825dc3` and `50fa53469a`, so C542's contract pin does not move. The
sorry at `Holes.lean:861` is neither discharged nor amended by this slice.

## 1. What this slice is

C545 §10 left the row with two exits and called both rulings: either the O-laws
gain a clause that pins O1's third origin to something outside them, or the sorry
is amended to say the attribution is free. **The first exit is runnable, and this
slice runs it**, which is what this ledger's own checker (transcribing Joe's rule
of 2026-09-01, `worklist_check.bb:17-45`) asks of a choice before it reaches him.

The something outside the laws is recorded. `futon3:checks/zaif-cascade.edn`
carries, for the transcribed run `:widen-to-a-budget`, a per-pattern
`[:cascade :provenance]` map whose entries are `:found` or
`[:admitted-by <rule-id>]`, written by the `:admit` arm of `apply-edit`
(`futon3:checks/find_organise.clj:412`, the `assoc-in` at
`futon3:checks/find_organise.clj:422`). Recomputed here rather than read off:
twenty entries, nine admissions at transcription indices 11–19 — exactly
`d1Admitted` (`F12D1Arms.lean:19`) — and eleven `:found` at 0–10, exactly
`d1Selected` (`F12D1Arms.lean:16`). The record file's last change is futon3
`1b8b1d1`, which is the transcription basis, and it is unchanged at futon3 HEAD
and clean in the working tree, so unlike slice 2's library read this basis needs
no archive.

## 2. The determination, and what it costs

`AttributingOrganiseType` (`F12AttributionArm.lean:48`) is slice 7's
`AdmittingOrganiseType` (`F12AdmittingArm.lean:17`) with one argument inserted
before the codomain and nothing else changed — the checker reconstructs the
insertion from both files rather than taking the sentence on trust.
`ConformantOrganiseAttributing` (`F12AttributionArm.lean:52`) is slice 7's
predicate plus exactly one clause, `oattr`, pinning `admittedBy` to the support
of that argument; the checker compares the four inherited clauses one by one with
the new argument normalised away, and they are unmoved.

With the clause present the third origin is determined:
`attributingThirdOriginDetermined` (`F12AttributionArm.lean:128`) says any two
conformant functions agree on `admittedBy` at every input, and
`attributingRecordedAdmissions` (`F12AttributionArm.lean:138`) says every
conformant function returns exactly the nine recorded admissions from the
transcribed provenance. **Neither is a discovery.** `oattr` makes the field a
projection of an argument, so determination is bought by enlarging the argument
list at `Holes.lean:861`, and the two theorems are the receipt for the purchase
rather than evidence that the interface got stronger. The slice's content is the
price, which is why the price is stated as a machine-checked signature comparison
and not as a remark.

The thing the clause overturns is slice 7's result, and it is overturned at the
recorded input: `attributingMirrorNotConformant` (`F12AttributionArm.lean:119`)
refutes the mirror that `organiseAdmittingMirrorConformant`
(`F12AdmittingArm.lean:98`) proved conformant, at recorded vertex 11.

## 3. What the review added: the clause, and nothing else, does the work

Codex's control C2 planted the deletion of `oattr` and watched
`attributingMirrorNotConformant` stop elaborating. A planted deletion is not
something a later reader can cite — the same shape as slice 5's unused haves,
slice 6's undischarged predicate and slice 7's `rfl`-provable agreement — so the
deletion is committed as a predicate. `ConformantOrganiseAttributingSansAttr`
(`F12AttributionArm.lean:146`) is `ConformantOrganiseAttributing` minus `oattr`,
checked clause by clause to differ in exactly that;
`attributingSplitSurvivesWithoutOattr` (`F12AttributionArm.lean:208`) proves both
witnesses satisfy it and disagree on admission at recorded vertex 11. So slice 7's
underdetermination survives the enlarged argument list untouched, and it is the
added clause rather than anything else about the new signature that determines the
origin.

Two supports for reading that as a split rather than a coincidence:
`attributingAgreeOnNodes` (`F12AttributionArm.lean:181`) and
`attributingAgreeOnEdges` (`F12AttributionArm.lean:188`) show the two witnesses
differ only in attribution, and `attributingZaifEdgeNonVacuous`
(`F12AttributionArm.lean:195`) shows the relation they agree on is inhabited —
the recorded fast-forward `18 → 19`, the single entry of
`runs/F12-organise/01-zaif-transcription.edn` `:fast-forward :over-nodes`. Slice
7's first spelling of the same agreement was provable by `rfl` with nothing saying
the relation was non-empty; that is why this one is committed rather than assumed.

## 4. The cheaper move, refuted: read it off the argument already there

`organise`'s type already takes a `Cascade Policy` — the policy-grain cascade —
and the emitting rule id is one of its nodes, so the obvious way to avoid paying
for a new argument is to read the attribution off the temperament. It does not
work, and `attributionNotFixedByAnyTemperament` (`F12AttributionArm.lean:230`)
states why in the general form: for EVERY `t : Cascade ZaifPolicyRule` the two
slice-7 witnesses disagree on `admittedBy`. The proof never reads `t`'s nodes,
edges or precedence, and that insensitivity is the claim — `Cascade Policy`
carries a set of rule ids and nothing mapping a rule to the patterns it admitted,
and that map is a run fact. `attributionNotFixedByTemperament`
(`F12AttributionArm.lean:239`) is the recorded instance of it. The checker
corroborates from the other side: no key of the record's `:temperaments` map has a
pattern-keyed value (`:pattern-keyed-maps []` in the artifact).

**A defect in this slice's own dispatch packet, found in review.** The packet told
codex that the record's `:temperaments` `:shared-nodes` is the policy cascade's
node set. It is not: `differ-only-in-the-stop`
(`futon3:checks/construct_cascade.clj:338`, the `:shared-nodes` line at
`futon3:checks/construct_cascade.clj:348`) removes the stop rule before comparing
the two temperaments, so the summary reports one node where the temperament that
ran has two — `budgeted-temperament` at `futon3:checks/construct_cascade.clj:324`
has `[:halt-on-budget :widen-the-cascade-only-on-evidence]` with precedence
`{:halt-on-budget 1, :widen-the-cascade-only-on-evidence 2}`. The dispatched
`zaifPolicyCascade` was therefore a weaker input than the recorded one. Repaired:
`ZaifPolicyRule` (`F12AttributionArm.lean:17`) has both constructors,
`zaifPolicyCascade` (`F12AttributionArm.lean:222`) carries both nodes and the
recorded precedence order, and `zaifProvenanceNeverTheStopRule`
(`F12AttributionArm.lean:41`) records that the run's provenance names one of the
two and never the other. The checker now reads the temperament literal out of its
own source and compares node count and precedence length, so the summary cannot be
mistaken for the thing again.

The repair does not change any theorem's truth, and saying so is part of the
result: `attributionNotFixedByAnyTemperament` holds of every temperament,
including the singleton the packet's error would have produced. What the repair
buys is that the recorded instance is now the recorded one.

## 5. The limit: the rule grain is exercised nowhere

The attribution input is a map to rule ids, but on the recorded data it carries no
rule-grain information at all. Recomputed over every `*cascade*.edn` under
`futon3:checks`: nine files, seven carrying provenance, 213 admissions, and the
distinct rule-id count over all of them is **one**,
`:widen-the-cascade-only-on-evidence`. Two of the nine
(`futon3:checks/construct-cascade.edn`, `futon3:checks/snatch-cascade.edn`) carry
no provenance map at all, so the alternative library-scale record has no
attribution to pin to.

So no recorded run distinguishes "pinned to the emitting rule" from "pinned to a
single admitted-or-not flag", and this slice's determination result is a result
about the second reading whatever its statement says about the first. The number
is recomputed by the checker rather than written into the prose, so a record that
later carries two rules moves it instead of leaving the caveat stale.

## 6. Controls

Codex's four, each with the plant verified to have landed first and re-runnable
from the report: the natural witness's nodes narrowed to `sel ∪ ∅` breaks its own
O1 and O3; `oattr` deleted from the structure stops
`attributingMirrorNotConformant` elaborating (`Invalid field oattr`); the
temperament comparison read at selected vertex 5 gives `h5 : True`; and the
determination conclusion moved to `addedByOrganise` leaves the rewrite with no
`admittedBy` occurrence to act on.

Five run in review against the Lean, each plant verified present in the planted
copy before the build and each producing an error:

1. `zaifProvenanceNeverTheStopRule` asserting the OTHER constructor is never
   returned — unsolved goals in the `isTrue` branch, so the theorem is about which
   of the two rules the record names.
2. `attributingZaifEdgeNonVacuous` aimed at the unauthored pair `17 → 19` — type
   mismatch on `trivial`, so the non-vacuity depends on the recorded edge.
3. `attributingSplitSurvivesWithoutOattr` read at selected vertex 5 instead of
   admitted vertex 11 — unsolved goals.
4. `organiseAttributingMirrorConformantSansAttr` restated with the full predicate
   — `Fields missing: oattr`, which is control C2 as a committed statement.
5. `attributionNotFixedByAnyTemperament` read at selected vertex 5 — unsolved
   goals.

Six against the checker, each moving the verdict: an admission demoted to `:found`
in a copy of the record (vertex blocks diverge); the temperament literal stripped
of its stop node in a copy of `construct_cascade.clj`
(`:policy-cascade-is-not-the-recorded-temperament`); a review theorem renamed with
a suffix, which is the prefix-match trap slice 7 found
(`:missing-declarations :review-split-survives`); the sans-attr predicate dropping
`o3` as well as `oattr` (`:sans-attr-predicate-drops-more-than-oattr`); a second
rule id planted into the run (`:the-run-names-more-than-one-rule`); and a moved
pin expectation (`:a-pinned-file-moved`).

## 7. Gates

`lake build` 2708 jobs, 0 errors, no warning from the new module. `#print axioms`
over all 23 declarations, generated from the file rather than typed: 0 `sorryAx`,
9 depending on no axiom at all. `clj-kondo` 0/0 and `check-parens` OK on
`f12_attribution_check.bb`; checker byte-identical over two runs;
`negative_controls.sh` PASS; `pointer_check.bb` run on the working ledger before
the ledger commit. No `gen_aif_dag.bb` (TN §9a).

## 8. Where this leaves the row

The exit C545 called a ruling is now a measured option rather than a description.
Pinning the third origin determines it, at the price of a fourth argument the
signature at `Holes.lean:861` does not have, and with no recorded run that
exercises the pin at rule grain. The cheaper alternative — derive the attribution
from the policy cascade the type already takes — is refuted for every temperament.

What remains is the second exit, amending the sorry, and it is still a ruling: the
choice is between paying for the argument and recording in the declaration that
the attribution is free. Both arms of that are now stated with their costs, which
is what the arms of a choice point are supposed to look like before anyone picks
one. D2, D3 and the O3 field question are untouched, and O4 is still not stateable
at this carrier — `ArmOneCascade` has none of the six O4 fields, checked from the
three field lists.
