# C552 — `:F12` slice 14: what discharging the sorry costs, and what `opaque` really needs

The `:F12` acceptance has one clause no slice had acted on: *discharge or amend
the sorry at `Holes.lean:861`*. Slices 2–4 recorded it as blocked on D1; slices
9–13 registered and ran the three choices the row had accumulated
(`:organise-third-origin`, `:organise-o3-field`, `:organise-carrier`) and each
ended by noting the sorry was still neither discharged nor amended. This slice
takes it, and it needed no ruling to do so: the arms are Lean elaborations
against fixtures this row already built, which is the case
`worklist_check.bb:17-45` sends to the branches rather than to Joe.

**No ruling is taken and no contract moves.** The new registry entry carries
`:status :observed-not-decided` and no `:ruling` key; the registry compares
EQUAL as a value to HEAD outside that one key, checked both ways; `git log -1`
still reads `61c4825dc3` on `Holes.lean` and `50fa53469a` on
`holes-contract.json`. The sorry itself is untouched — the build still reports
it, which is how the check knows.

## 1. The question is not whether a term exists

Slice 6 already built two total functions at exactly `organise`'s type
(`mathlib4/DarkTower/WarMachine/F12Conformance.lean:59` and
`mathlib4/DarkTower/WarMachine/F12Conformance.lean:77`, at the abbrev
`mathlib4/DarkTower/WarMachine/F12Conformance.lean:19` whose docstring states it
is identical to `organise`'s). So the sorry is not there for want of a term, and
`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:28` proves the weaker fact
too: the type is inhabited with no assumptions at all.

What separates the arms is what the declaration SAYS afterwards. Registered as
`:organise-sorry` with six arms, five of them this entry's and all five run; the
sixth is the type amendment, which is D1 and points at `:organise-carrier`
rather than restating its payments under a second key.

## 2. The refusal is now a proved fact rather than a stated one

The `organise` docstring
(`mathlib4/DarkTower/WarMachine/Holes.lean:860`) refuses to implement on the
ground that the recorded O1–O4 instance *does not select one canonical
implementation*. That sentence is now a theorem, under BOTH readings of O3, so
that proving it does not tacitly decide `:organise-o3-field`:

- selected reading — `mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:84`,
  witnesses `organiseSelectedOnly` and `organiseUpClosure`, difference from
  `mathlib4/DarkTower/WarMachine/F12Conformance.lean:107`;
- node-set reading — `mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:93`.
  This one needed a second witness of its own, `organiseNodesUpClosure`
  (`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:62`), because
  `organiseUpClosure` is REFUTED under that reading
  (`mathlib4/DarkTower/WarMachine/F12Conformance.lean:173`).

**The disagreement is located, not merely exhibited.** Both node-reading
witnesses return every recorded selected node and that set is not empty
(`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:111`), so what they differ
about is O1's second origin and nothing else. Each side is inhabited on the
record — vertex 26 organise-added
(`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:120`), the edge `6 → 26`
present in one (`:127`) and absent from the other (`:133`) — without the edge
relation being total (`:140`).

## 3. And no recorded run could settle it

Recomputed into `runs/F12-organise/09-discharge-arm.edn` over the nine cascade
records under `futon3:checks`: **52** O1-shaped cascades, **8** with a non-empty
organise-added set (all eight in `futon3:checks/snatch-cascade.edn`), and
**ZERO** in which a node organise added is a node of the cascade. The two
conformant discharges differ only in what organise puts into the cascade, so no
recorded run distinguishes them. The producer's empty literal is read out too
(`futon3:checks/construct_cascade.clj:412`), so the zero is a fact about the
constructor rather than about which runs happened to be recorded — and
`f12_discharge_check.bb` FAILS if that literal ever becomes computed.

## 4. The packet's premise about `opaque` was refuted

The dispatch packet claimed `opaque` REQUIRES a body, so that sealing the
declaration would be the conformant-discharge arm in disguise. **That is false**,
and the refutation is committed:
`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:271` is an `opaque` with no
body at all, elaborating from the local inhabitance instance at
`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:262`.

What separates the two routes is their AXIOM FOOTPRINT, and it is
machine-readable rather than argued — from the `#print axioms` audit generated
over the file:

| declaration | line | axioms |
|---|---:|---|
| `organiseOpaqueZaif` (body `organiseEmpty`) | 256 | none |
| `organiseOpaqueNoBody` (no body) | 271 | `Classical.choice` |

Either route removes the sorry and neither leaves an O-law provable of the
declaration. So this arm keeps the refusal's content and gives up its evidence,
where the conformant-discharge arm gives up the refusal and keeps the evidence.

## 5. What the temperament argument is worth

The 2026-09-02 TYPE AMENDMENT in the same docstring says the temperament
argument is where the closure-policy datum lives. Both directions are now
measured. A conformant discharge MAY read it — a function branching on the
temperament's precedence is conformant and returns different node sets at two
temperaments on the recorded inputs, under the selected reading
(`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:162`,
`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:188`) and under the node-set
reading (`:204`, `:230`). And the laws do not REQUIRE it to: the selected-only
witness ignores the temperament entirely, by full structure equality
(`mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:240`).

## 6. Three review findings

**(a) The `opaque` correction above** — the packet's, not the seat's. The seat
implemented what it was asked and reported the control faithfully; the claim was
wrong in the packet.

**(b) The R2 floor was missing.** The non-uniqueness results asserted a
difference with nothing under it saying the two witnesses were otherwise
related. Added at `mathlib4/DarkTower/WarMachine/F12DischargeArm.lean:111`. This
is the fifth consecutive slice in which review has had to add exactly this floor
(slices 8, 10, 11, 13 and now 14), which is no longer a slip and is a hole in
what the packets ask for.

**(c) Six docstring pointers were off, and the packet supplied three of them.**
`organiseUpClosure` is at `mathlib4/DarkTower/WarMachine/F12Conformance.lean:77`
and the packet said `:78`; `organiseWitnessesDifferOnZaif` is at `:107` and the
packet said `:108`; `trivialPolicyCascade` is at `:99` and the packet said
`:98`. The seat copied all three. This is the SECOND slice in a row in which a
packet's own pointers landed in the committed file — slice 13's review found the
first (`zaifAdmittedFor` at `:50` for `:48`) and wrote down why: a packet's
pointers are gated by nothing, since `pointer_check.bb` scans the ledger and the
notes and never sees the dispatch text. The other three were the seat's:
`d1Selected` cited at its docstring line rather than
`mathlib4/DarkTower/WarMachine/F12D1Arms.lean:17`, `Cascade` as `29-35` where the
structure ends at `:34`, and `organiseSelectedOnly` as `59-66` where it ends at
`:65`.

All three repaired here rather than re-dispatched (mathlib4 `38a34f286a`).

## 7. Controls

Four against the Lean, each plant read back out of the planted copy before
building, each an independent re-run rather than a report read:

| plant | verified present | exact failure |
|---|---|---|
| up-closure returns `nodes := sel` | `{ nodes := sel` at planted line 64 | O1 unsolved goal `∀ x ∈ sel, Reach repo.standsOn x p → p ∈ sel`; O3 `rfl` fails, `.edges` not defeq to `fastForward .nodes` |
| `organiseOpaqueZaif`'s body removed | declaration reads `opaque organiseOpaqueZaif : OrganiseType Unit Nat` | `failed to synthesize 'Inhabited' or 'Nonempty' instance for OrganiseType Unit ℕ` — and the instance declared LATER in the file does not rescue it |
| the local `Nonempty` instance removed | 0 occurrences of `organiseTypeNonemptyInstance` | the same synthesis failure, now at `organiseOpaqueNoBody` — so the bodiless route rests on the inhabitance and nothing else |
| the R2 floor aimed at `organiseEmpty` | planted line 113 | type mismatch: `x ∈ d1Selected` where `x ∈ (organiseEmpty …).nodes` expected — the floor is not true of everything |

Four against the checker, each moving the verdict:

| plant | result |
|---|---|
| `organiseEmpty` renamed, keeping `organiseEmptyNotConformantSelected` | `:required-declarations-missing ["organiseEmpty"]` — the prefix trap slice 7 found does not fire |
| `:added-by-organise` becomes computed in the producer | `:producer-no-longer-writes-an-empty-literal` |
| the sorry discharged in a planted `Holes.lean` | `:organise-not-declared-as-a-sorry` |
| the bodiless `opaque` removed | `:expected-two-opaque-declarations`, `:expected-exactly-one-bodiless-opaque` |

The last of these is the one that keeps the refuted claim refuted: with only the
body-carrying declaration left, "opaque requires a body" would read as true
again.

## 8. Gates

- `lake build DarkTower.WarMachine.F12DischargeArm`: 2707 jobs, 0 errors, no
  warning from the new module. Re-run in review after the repairs.
- `#print axioms` generated over all **28** declarations: **0** `sorryAx`, **11**
  depending on no axiom.
- `sorry` / `axiom` / `native_decide` in code: 0 / 0 / 0. The token `sorry`
  occurs once in PROSE, in the R5 docstring that corrects how the sorry can be
  removed; the checker counts code and prose separately and fails only on code.
- `f12_discharge_check.bb` PASS, artifact byte-identical over two runs.
- `negative_controls.sh` PASS, `pointer_check.bb` 0 unresolved, both run on the
  working tree before the ledger commit.
- No `gen_aif_dag.bb` (TN §9a). No machine run, so no run-lock.
- `lake build DarkTower` is NOT this row's gate and stays red at HEAD outside
  this row, as slice 10 established by moving its file aside.

## 9. What this does not settle

The entry chooses nothing. What remains open in `:F12` is D2 (probe
`:not-a-witness`, gate red at HEAD, C547 §7), D3, and the four registered choices
— `:organise-third-origin`, `:organise-o3-field`, `:organise-carrier` and now
`:organise-sorry` — every arm of all four now carrying a measured cost. **No
slice has anything left to run on any of them.** The sorry at
`mathlib4/DarkTower/WarMachine/Holes.lean:861` is still neither discharged nor
amended, and what this slice adds is that leaving it is now a choice with a
measured price rather than a default.
