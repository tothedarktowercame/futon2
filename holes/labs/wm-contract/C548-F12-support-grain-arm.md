# C548 — `:F12` slice 10: run the support-grain arm, the last unrun arm of the choice

Item `:F12` (`worklist.edn`), slice 10. C547 §6 named a free hand inside the pay
arm and registered it as `:not-run` with `:buys :unmeasured :costs :unmeasured`:
arm A pins `admittedBy` to the *support* of a `P → Option Rule` input, never
eliminating `Rule`, so a `Set P` input might buy the same thing. This slice runs
it, in one new Lean module, and rewrites that arm's three registry fields with
what was measured.

**No ruling is taken and none is implied.** `:status` is still
`:observed-not-decided`, there is no `:ruling` key, and the registry compares
EQUAL as a value to HEAD outside `:choices :organise-third-origin`. `Holes.lean`
and `holes-contract.json` are untouched — `git log -1` on both still reads
`61c4825dc3` and `50fa53469a`, so C542's contract pin does not move.

New file: `mathlib4/DarkTower/WarMachine/F12SupportArm.lean` (311 lines, 37
declarations), committed at mathlib4 `60cab98ab3`. No existing file in any repo
was modified.

## 1. The arm

`SupportOrganiseType` (`F12SupportArm.lean:24`) is arm A's signature
(`F12AttributionArm.lean:48-49`) with the rule-valued argument replaced by its
support, and `ConformantOrganiseSupport` (`F12SupportArm.lean:28`) is arm A's
predicate with `oattr` replaced by equality with that argument
(`F12SupportArm.lean:38`). Natural witness at `F12SupportArm.lean:41`, conformant
at `F12SupportArm.lean:52`; the mirror that files admissions under
`addedByOrganise` is refuted at recorded vertex 11 (`F12SupportArm.lean:74`),
the counterpart of `attributingMirrorNotConformant`
(`F12AttributionArm.lean:119`).

## 2. What it buys: the same determination, and the two arms are inter-derivable

`supportThirdOriginDetermined` (`F12SupportArm.lean:82`) — any two conformant
functions agree on `admittedBy` at every input. That alone would only say the two
arms each determine *something*; the translation is what makes them the same
determination:

- forward, `attributingOfSupport_conformant` (`F12SupportArm.lean:137`): a
  conformant support-grain function, precomposed with support erasure, is
  conformant at arm A's signature;
- back, `supportOfAttributingAt_conformant` (`F12SupportArm.lean:263`): a
  conformant arm-A function, precomposed with an indicator at a named rule, is
  conformant at the support signature.

The reverse direction as first written held only at `Rule := Unit`
(`F12SupportArm.lean:154`) — the one rule type at which arm A's input carries no
rule, so the case where the arms trivially coincide. The general version is the
review's addition, instantiated at the single rule id any recorded run exhibits
(`F12SupportArm.lean:277`).

Recorded execution: handed the support of the transcribed provenance map, every
conformant function returns the nine admissions (`F12SupportArm.lean:240`). The
version shipped before review (`F12SupportArm.lean:101`) hands the arm
`d1Admitted` itself, so it recovers the recorded answer from an input that
already is it; both are kept, and the difference between them is the point.

## 3. What it costs

**Not an argument.** The arm is cheaper in *grain*, not in argument count: the
declaration at `Holes.lean:861` still gains a fourth input it does not have, and
the price statement carries over unchanged — without the clause, two functions
conformant on the four laws agree on nodes and edges and still disagree on
admission at vertex 11 (`F12SupportArm.lean:114`). That agreement is on an
inhabited edge relation, `18 → 19` (`F12SupportArm.lean:227`); slice 7 and slice 8
each had to add that non-vacuity for the same reason, and this arm shipped
without it.

**What the smaller input loses is not visible in the field the choice is about.**
For any conformant arm-A function at any rule type, equal supports force equal
`admittedBy` (`F12SupportArm.lean:167`). Non-vacuously on the recorded run: a
counterfactual provenance labelling the same nine vertices with `haltOnBudget` —
a rule the recorded run never emits (`zaifProvenanceNeverTheStopRule`,
`F12AttributionArm.lean:41`) — has the same support (`F12SupportArm.lean:179`),
differs at vertex 11 (`F12SupportArm.lean:183`), and no conformant function tells
the two apart on `admittedBy` while both yield the nine admissions
(`F12SupportArm.lean:186`).

**Stated against overstating it.** The rule grain is not inert at the interface,
only in `admittedBy`. `organiseAttributingStopMarked` (`F12SupportArm.lean:196`)
reads the rule and files the stop-labelled subset under `addedByOrganise`; it is
conformant (`F12SupportArm.lean:205`), returns the same nodes as the natural
witness at every input (`F12SupportArm.lean:292`, via the containment at
`F12SupportArm.lean:285`), and differs from it only on the counterfactual input,
never on the recorded one (`F12SupportArm.lean:216`).

## 4. The measurement, recomputed rather than cited

`f12_attribution_check.bb` re-run this slice: PASS, and
`runs/F12-organise/06-attribution-arm.edn` regenerated byte-identically (no git
diff). Over the nine cascade records under `futon3:checks`: 213 admissions
(`:corpus-admissions 213`), seven records carrying provenance, two carrying none,
and `:distinct-rule-ids-in-the-corpus 1` —
`:widen-the-cascade-only-on-evidence`. So the corpus cannot exercise the grain
arm A buys its determination at, which is the same finding slice 9 recorded and
is now the reason the instantiation at `F12SupportArm.lean:277` is at that rule
and not at an arbitrary one.

## 5. Review — what was checked, and what it found

The module was written by codex-1 (job `invoke-1788747663187-13512-1dbe105d`,
dispatched `--from wm-build-work`) and reviewed here against the diff and a
re-run of the gates. Three findings, all fixed in review rather than re-dispatched:

- **the edge agreement was on an unwitnessed relation.** `F12SupportArm.lean:114`
  asserted the two witnesses agree on `edges` with nothing anywhere saying the
  relation is inhabited — the defect slice 7's and slice 8's reviews each caught
  in their own arm. Fixed by `supportZaifEdgeNonVacuous`
  (`F12SupportArm.lean:227`).
- **the recorded-input claim was fed its own answer.** `F12SupportArm.lean:101`
  hands the arm `d1Admitted` and concludes `d1Admitted`, where arm A's
  counterpart routes through the transcription. Fixed by the routed version at
  `F12SupportArm.lean:240`; the original is kept, since which one you are reading
  is exactly what a reader of this arm needs to notice.
- **the reverse transfer held only at `Unit`.** As written
  (`F12SupportArm.lean:154`) the "same determination" claim was proved in the one
  case where the two arms are the same by construction. Generalised at
  `F12SupportArm.lean:263` and instantiated at the recorded rule
  (`F12SupportArm.lean:277`).

Also corrected: the docstring at `F12SupportArm.lean:204` gave containment of the
stop-labelled set in the support as the reason O1 holds; the proof is `rfl` and
does not use it. The containment is now proved where it is actually needed
(`F12SupportArm.lean:285`, for the node agreement).

Codex reported four controls, each plant verified present and each failing as
required, and reported one thing this review confirms: it did not commit, because
`lake build DarkTower` is red. **That redness is pre-existing and outside this
row.** Verified by moving the new file aside and rebuilding at HEAD:
`DarkTower/MemoryArmPreregistration.lean:268:18` and `:295:19` fail with `type
expected`, in a file last touched by `1bc1734308`, an APM/Baldwin lane. The gate
this row's slices have actually been clearing is the module target — slice 8
reported 2708 jobs, this slice's module target reports 2709.

Four further controls run in review against the added theorems, each plant
verified present in the file first and each moving the verdict:

| plant | result |
|---|---|
| non-vacuity aimed at the unauthored pair `0 1` | `F12SupportArm.lean:236` application type mismatch |
| routed recorded admissions concluding `d1Nodes` | `F12SupportArm.lean:245` type mismatch |
| indicator inverted (`none` on the support) | `F12SupportArm.lean:253` unsolved goals |
| node agreement taking the left injection instead of the containment | `F12SupportArm.lean:301` application type mismatch |

## 6. Gates

`lake build DarkTower.WarMachine.F12SupportArm`: 2709 jobs, success, no warning
from the new module (the `sorry` warnings replayed are `Holes.lean`'s own, at
lines 153, 264, 861, 7240, 7263, 7274, 7802, 7805, 7808, 7811). `#print axioms`
over all 37 declarations, the list generated from the file rather than by hand:
0 `sorryAx`; 17 depend on no axioms, 20 on `propext`/`Classical.choice`/`Quot.sound`.
0 occurrences of `sorry`, `axiom` or `native_decide` in the file.
`negative_controls.sh` PASS (133 negative, 53 positive). `pointer_check.bb`: 2578
pointers in 6 files, 0 unresolved. One valid pointer is deliberately NOT in
pointer-syntax in the ledger row: `mathlib4/DarkTower/` is not on `pointer_check.bb`'s
roots allowlist (`p4ng/empirics-futon/pointer_check.bb:53`, which carries
`mathlib4/DarkTower/WarMachine/` but not its parent), so a `file:line` citation of
`MemoryArmPreregistration.lean` reports as unresolved although the file exists and the
lines are right. Appending that root is a p4ng edit this row does not own; the exact
columns are in §5 above, and this paragraph is here so the next slice that needs a
pointer into `DarkTower/` appends the root rather than dodging it again. No Clojure and
no Lisp touched, so no clj-kondo or check-parens subject. No `gen_aif_dag.bb`
(TN §9a). No machine run, so no run-lock.

## 7. What this does not settle

Every arm of `:organise-third-origin` now carries a measured cost, so the choice
is Joe's and no slice has anything left to run on it. Nothing else about `:F12`
moved: D1 (which carrier the four O-laws are stated of), D2 (which denominator
O4's acting order is read over, and whose probe is `:not-a-witness` with the gate
red at HEAD — C547 §7), D3, and the O3 field question (`Holes.lean:1034`) are
untouched, O4 is still not stateable at the arm carrier, and the sorry at
`Holes.lean:861` is neither discharged nor amended.
