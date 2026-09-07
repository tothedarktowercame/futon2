# C547 — `:F12` slice 9: the choice at the sorry, registered with its arms

Item `:F12` (`worklist.edn`), slice 9. No Lean, no new checker: this slice
registers the choice point that slices 5–8 built the arms for, in
`aif-equations.edn :choices` under `:organise-third-origin`, and states what each
arm was measured to cost. `Holes.lean` and `holes-contract.json` are untouched
(`git log -1` on both still reads `61c4825dc3` and `50fa53469a`).

**No ruling is taken here and none is implied.** The entry carries `:status
:observed-not-decided` and no `:ruling` key. Every claim below was re-checked
against the files today, not copied from C543–C546; where a number comes from a
run record, the record was regenerated first and compared.

## 1. Why registration is the slice

C545 §10 and C546 §8 both end at the same place: the sorry at `Holes.lean:861`
is discharged or amended only by choosing, and each called the choice a ruling.
This ledger's own checker says what happens before a choice reaches Joe
(`worklist_check.bb:17-45`, transcribing his rule of 2026-09-01): a choice the
theory does not determine is a **choice point in `:choices`, with the arms named
and the measurement that separates them**, and the branches are built and run.
Slices 5–8 built and ran them. The registration had not been written, so the
arms sat in five prose documents and the registry that the machine and the
reviewer read said nothing about the choice at all.

That is the whole of this slice. It adds no theorem and settles nothing.

## 2. The choice

`organise : Cascade Policy → Set P → Repository P → Cascade P := sorry`
(`Holes.lean:861`). O1 gives a cascade node three origins, and the third —
`admittedBy`, added by the `:LA2` field amendment (`Holes.lean:833-845`) — has no
input in that signature that determines it. Either the signature gains one, or
the declaration records that the attribution is free.

## 3. Arm A — pay for the argument (run, slice 8)

`AttributingOrganiseType` (`F12AttributionArm.lean:48-49`) is the slice-7
signature with one input added, `P → Option Rule`; `oattr`
(`F12AttributionArm.lean:63-64`) pins `admittedBy` to that input's support.

*Buys:* `attributingThirdOriginDetermined` (`F12AttributionArm.lean:128`) — any
two conformant functions agree on `admittedBy` at every input;
`attributingRecordedAdmissions` (`F12AttributionArm.lean:138`) — every conformant
function returns the nine recorded admissions.

*Costs:* a fourth argument the declaration does not have. The determination is a
projection of the new argument, not a property the interface gained:
`attributingSplitSurvivesWithoutOattr` (`F12AttributionArm.lean:208`) has two
functions conformant on the four laws at the **enlarged** argument list still
disagreeing on admission, so it is the clause and not the argument that
determines. It also overturns a result: `attributingMirrorNotConformant`
(`F12AttributionArm.lean:119`) refutes the mirror that
`organiseAdmittingMirrorConformant` (`F12AdmittingArm.lean:98`) proved conformant.

## 4. Arm B — record that the attribution is free (run, slices 6 and 7)

Two sub-arms, by carrier, and they cost differently.

*With an `admittedBy` field* (`ArmOneCascade`, `F12D1Arms.lean:74-82`):
conformance leaves the attribution undetermined —
`organiseAdmittingMirrorConformant` (`F12AdmittingArm.lean:98`) files all nine
recorded admissions under `addedByOrganise`, leaves `admittedBy` empty, and
satisfies all four clauses.

*Without one* (`Cascade` as declared, `Holes.lean:29-34`):
`admittedIndistinguishableFromAdded` (`F12Conformance.lean:123`) proves every
recorded admission is **forced** under `addedByOrganise` — the misattribution the
`:LA2` amendment added the field to prevent.

*The cheaper escape is refuted.* `organise` already takes the policy-grain
cascade, so reading the attribution off it would cost no argument;
`attributionNotFixedByAnyTemperament` (`F12AttributionArm.lean:230`) proves the
two slice-7 witnesses disagree for **every** temperament.

## 5. The measurement that separates the arms

Recomputed today by re-running `f12_attribution_check.bb` (PASS; artifact
byte-identical to the committed one): across the nine cascade records under
`futon3:checks`, 213 admissions, seven records carrying provenance, two carrying
none (`construct-cascade.edn`, `snatch-cascade.edn`), and **one distinct rule id**
in the whole corpus (`:widen-the-cascade-only-on-evidence`).

So no recorded run distinguishes an emitting-rule attribution from a boolean
admitted-or-not flag. Arm A buys determination on data that cannot exercise the
grain it buys it at.

## 6. A free hand inside arm A, not yet run

The grain of the attribution input is a second choice inside the first, and
nothing measured so far fixes it:

- `oattr` reads only the support, `{p | (prov p).isSome}`
  (`F12AttributionArm.lean:63-64`);
- `attributingThirdOriginDetermined` (`F12AttributionArm.lean:128`) discharges by
  `rw [hf.oattr, hg.oattr]` and never eliminates `Rule`;
- the field it pins is a `Set P`, and the amendment says why in the source:
  "which policy-grain rule admitted each one is recorded on the Clojure side and
  is not what O1 quantifies over" (`Holes.lean:833-845`);
- the corpus has one rule id (§5).

That is not a proof that a `Set P` input buys the same determination — it is an
**unrun arm**, and the cheapest one left. It is named in the registry entry as
such, and it is the candidate for slice 10.

## 7. What this slice does not settle, and what is not registered

D1's carrier question, D2 (which denominator O4's acting order is read over), D3
(whether an O4 witness may be reached by an encoding written after the law) and
the O3 field question (`Holes.lean:1034`) are all untouched here. D2's three
denominators were run in slice 3 and disagree — O4 false over the 29 primary
rounds, true over the 49 paired and the 102 transcript rounds
(`runs/F12-organise/02-o4-reachability.edn`, re-read today) — so D2 is a choice
point whose arms exist; it is **not** registered by this slice, because its probe
is labelled `:not-a-witness` (no constructor selected both patterns, so there is
no cascade for the row to be of) and because the gate it would be read through
does not pass at HEAD: `require-pass!` aborts with
`:rule-does-not-encode-an-authored-then` for three of four rules
(`futon3c:scripts/zaif_cascade_gate.clj:579-582`), a table this row does not own.
Registering a choice point whose measurement cannot currently be taken would put
a question to Joe dressed as a result.

## 8. Gates

`negative_controls.sh` and `pointer_check.bb` — see the ledger row. No Lean
touched, so no `lake build`; no new script, so no clj-kondo or check-parens
subject beyond what the ledger row records. No `gen_aif_dag.bb` (TN §9a).
