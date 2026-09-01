# C472 — I5 slice (b): the disposition on the R8 scalar F, and the route answer

Owner: claude-20 (wm-edge worklist seat). Author ≠ reviewer: this needs a second read.
Ledger row: `worklist.edn` `:I5`, slice **(b) the disposition** (futon2 `e25b1ae`). Slice (a) is
`C471-f-scalar-readers.md` (futon2 `b5eebb5`). **Nothing is removed here and no
ruling is written**; this slice says which branch of the acceptance is taken, why,
and what slice (c) has to do. Every claim carries a `file:line` pointer or the
words "not found".

## 1. The disposition: REMOVE

I5's acceptance offers two branches — remove the computation, or record why the
diagnostic is kept with its reader named. **The keep branch is not available**, and
the reason is not a judgement of mine: Joe's J2 ruling forecloses it by name.

`aif-equations.edn:163-168`, `:choices :free-energy-form :ruling :decision`:

> "The current Laplace channel F is a shortcut (J2). **Do not accept it as a
> diagnostic to keep: retire it**, and replace it with theory-aligned free-energy
> quantities that are actually needed and consumed …"

His `:consequence` attached one condition — "it stays in the registry and the figure
until its replacement is realised and consumed" — and RUN9 discharged it: F_π entered
the live policy posterior on three of four S4 ticks (`aif-equations.edn:90-93`
`:discharged-by`; `C470-s4-live-f-pi.md`). So the condition is met and the ruling's
direction stands.

The only material slice (a) produced that could have argued for keeping is that F
has a live reader. It does not survive being stated plainly. The reader is
`checks/r8_f_contract.clj`, whose job is to verify that the field is written
(`:170-181`). **The diagnostic's only reader is the check that the diagnostic is
written.** That is circular, and it is a reason the ruling already anticipated.

Cost argues neither way and should not be cited on either side: 264.28 ns/call
against a 14.22 s tick, 1.9 × 10⁻⁸ of a tick; 45 bytes on a 1,046,162-byte record,
0.0043% (C471 §5). If F goes it goes because J2 says so, not because it is expensive.

## 2. The blocking finding of C471 §6 is answered, and it was overstated

C471 §6 recorded that removing the computation removes the only `:R8` route tag
(`war_machine.clj:4915`, re-verified: the sole `:R8` in the file), contracting the
route from `… R7 → R3 → R8 → R5 …` to `… R7 → R3 → R5 …`, and that
`run3_conformance.bb` classifies `R3→R5` as `:unmapped` — not conformant. It
concluded that slice (b) could not proceed until "what R8 is tagged at afterwards"
was settled. Two things are wrong with that framing.

**(a) R8 does not go dark when F goes.** R8 the box hosts three registry rows, not
one: `:prediction-error` defines `:eps` at R8 (`aif-equations.edn:77-80`), imported
by `:precision` at R7 (`:81-84`) and `:belief-update` at R3 (`:104-107`);
`:policy-free-energy` defines `:F-pi` at R8 (`:95-102`); `:free-energy` defines `:F`
(`:85-94`) and is the retired one. So removal is a retag, not an untagging, and the
retag target already exists: `fe/compute-prediction-error` at
`war_machine.clj:4840`, in the same namespace as the function being removed
(`war_machine.clj:44`, `[futon2.aif.free-energy :as fe]`).

**(b) Two of the three "unmapped" verdicts are a checker gap, not a topology fact.**
`run3_conformance.bb`'s `topology` (`:52-62`) reads `:edges`, `:route-measured-drawn`
and `:decisions :retires`. **It does not read `:decisions :adds`.** But
`gen_live_topology.bb:190-200` does — decision-added edges are drawn, in a distinct
class, and D0's comment at `gen_live_topology.bb:141-147` is precisely about the cost
of recording decisions and never applying them. Measured: **16 decision-added pairs,
none of them in `:edges`, none readable by run3.** Among them `[:R8 :R7]`
(`:r2-r7-superseded-by-eps`, `:grounds :code`) and `[:R8 :R6]`
(`:r8-r6-f-pi-into-posterior`, `:grounds :code`) — the latter is the edge RUN9 added
for F_π, so if the F_π flag were on and F_π were route-tagged, the run would be NOT
CONFORMANT for a reason with nothing to do with F_π. **The published figure draws 16
edges the conformance checker will refuse.** That is a defect in the checker against
the registry's own vocabulary, reported here and not repaired (it is a RUN-class
repair, not this row's).

### What each retag option actually costs

Run through `run3_conformance.bb`'s own classifier (`:116-124`) against the topology
it loads at run time, not reasoned:

| option | resulting hops | classifier says |
|---|---|---|
| **0. drop the tag** | `R7→R3`, `R3→R5` | `:drawn`, **`:unmapped`** |
| **1. retag in place** (at 4915, naming `compute-prediction-error`) | `R7→R3`, `R3→R8`, `R8→R5` | `:drawn`, `:route-measured`, `:drawn` — **unchanged** |
| **2. retag in dependency order** (before the R7 tag at 4911) | `R8→R7`, `R7→R3`, `R3→R5` | **`:unmapped`**(gap), `:drawn`, **`:unmapped`** |
| **3. retag at the F_π site** (`5095-5096`) | `R5→R8`, `R8→R6` | **`:unmapped`**, **`:unmapped`**(gap) |

Option 3 is out: F_π is flag-gated default-off (`war_machine.clj:123-155`), so the
default path would carry no R8 hop at all.

**Option 1 buys conformance at a price that has to be said out loud.** The R7 and R3
tags are already emitted retroactively — `update-precision-state` runs at
`war_machine.clj:4845` and `apply-arena-belief-events` at `:4893`, both inside the
loop, and their tags are emitted at `:4911-4912` after the loop returns — so a
retroactive R8 tag is the existing convention, not a new fiction. But ε runs at
`:4840`, *before* both. So option 1 would place a tag naming ε after the tags for the
two things ε feeds, and the registry's `:via` for the route-measured `R3→R8`
(`control-map-edges.edn:120-124`) would then read
`futon2.aif.free-energy/compute-prediction-error` under a `:basis` — "belief-update
prediction errors feed the present-fit free-energy calculation" — that is backwards
for ε. D6 (`control-map-edges.edn:186`) already records this class of mismatch: "the
tag ORDER R7, R3, R8 records where the tags were placed, not where the values
flowed." Option 1 preserves a documented inaccuracy; it does not introduce a new one,
but it does move it onto a quantity where it is the wrong way round.

**Option 2 is the honest order and needs one thing written.** `R8→R7` is a checker
gap only. `R3→R5` is not: **no decision adds it** (checked — the `:adds` of all 16
decisions contain no `[:R3 :R5]`), and there is no theory edge, because the registry
routes the belief `R3 → R1 → R4 → R5` (`:belief-state :imports [:mu-next]` at R1,
`:forward-model :imports [:mu …]` at R4, `:risk`/`:ambiguity` → `:expected-free-energy`
at R5). The code within a tick does not go through R1: `wm-state` carries this tick's
updated belief (`war_machine.clj:4921-4929`) straight into `efe/rank-actions`
(`:4999`). So `R3→R5` is a real within-tick code flow that the registry's cross-tick
R1 indirection does not describe.

**Neither R8 nor F is what makes `R3→R5` unmapped.** It is unmapped today and would
be unmapped whatever R8 is tagged at; the F computation has merely been standing
between the two tags and hiding it. That is the sentence C471 §6 was reaching for and
did not have: removing F does not break conformance so much as **stop the F
computation from concealing a gap between the drawing and the route**.

## 3. Reader census: three additions to C471 §3, all benign

C471's census was correct that **nothing joins on the record key** but its list of
readers was incomplete. Re-run at HEAD over futon2 and p4ng:

- **`checks/variational_free_energy_witness.clj` — a live gate check C471 did not
  name.** Inventory `checks/wm_workspace_gate.clj:232`, invoked at `:317`, four
  negative controls at `:461-468`, listed in `checks/mutable-verdict-claims.edn:66`,
  registered at `checks/witness-registry.edn:510-537`. It reads **nothing from a
  trace record**: it mutation-tests the Lean binding
  (`mathlib4/DarkTower/WarMachine/VariationalFreeEnergyWitness.lean`, `:8`) against a
  hand-derived fixture (`variational-free-energy-reference.edn`, `:7`). So it is
  untouched by removing the Clojure computation — which is why naming it matters: it
  looks like a fifth blocker and is not one. The Lean binding is `:lean-status
  :closed` and Joe's, as I5's acceptance already says.
- **`scripts/generate_variable_situation_accounting.bb:20`** pins the *Lean* name
  `variationalFreeEnergy` in a `:scores` set, and `:72` matches the English phrase.
  Neither is a record-key reader.
- **Prose that would become false**: `data/r18-badges.edn:218` and
  `holes/aif-wiring-explainer.html:196` both carry "A separate per-tick
  variational-free-energy field now reports the declared Gaussian prediction-error
  objective," inside the `cascade-score` entry. Not readers; claims about the field's
  existence, and slice (c) owns them.

## 4. The r8 contract blocker is three conjuncts, not one

C471 §3a said removal puts new records in `era-violations` because the contract is
stamped unconditionally. Re-read at HEAD, `record-conforms?`
(`checks/r8_f_contract.clj:168-181`) requires all three of:

```
(= stored? gain?)        ; F present iff :selection-gain present
(= stored? controller?)  ; F present iff shape = :controller-map
(= stored? current?)     ; F present iff era = :stored-f-controller
```

`r8-producer-contract` is `:r8/stored-f-controller-v1` (`src/futon2/aif/trace.clj:244-247`)
and is stamped unconditionally at `:473`. So bumping the contract constant alone is
not enough: an F-less record also fails `(= stored? gain?)` and `(= stored? controller?)`
while selection-gain and the controller-map shape are still written. **Slice (c) must
extend the era boundary with a new era, not bump a version string.** The pinned census
`{:missing-F-computable 755 :stored-F 32 :insufficient-inputs 5}`
(`checks/r8_f_contract.clj:16-18`) moves with it.

## 5. Pointer drift found in passing (reported, not repaired)

RUN9 repaired the `:free-energy` row's pointers. Three neighbours in the same
registry have drifted the same way and still resolve, because `pointer_check`
verifies line ranges and not content:

| row | `:code` claims | what is actually there | current site |
|---|---|---|---|
| `:prediction-error` (`aif-equations.edn:80`) | `war_machine.clj:4368-4379` | an HTTP eval POST block | `:4840` |
| `:belief-update` (`:107`) | `war_machine.clj:4375-4388, 4429-4431` | same HTTP block; a docstring | `:4840`, `:4845`, `:4893` |
| `:precision` (`:84`) | `war_machine.clj:4380-4388` | the same HTTP block | `:4845` |

This matters to the disposition rather than being incidental: `:prediction-error`'s
pointer is the one the retag depends on. Not repaired here — these are registry rows
carrying reader signatures, and repairing them is the supersede dance, not slice (b).

## 6. What slice (c) owes

Not done here. In order:

1. Decide option 1 vs option 2 for the `:R8` retag, with §2's table as the cost.
2. If option 2: a `:decisions` entry adding `[:R3 :R5]` on `:grounds :code` with the
   `war_machine.clj:4921-4929`/`:4999` pointers as basis — a code-backed correction,
   not a preference — and the `R8→R7` checker gap either fixed in
   `run3_conformance.bb` or declared.
3. futon2: delete `compute-variational-free-energy` (`free_energy.clj:184-205`), its
   call (`war_machine.clj:4913-4914`), the judgement key (`:5241`) and the record key
   (`trace.clj:482`); retag `:4915`; extend the r8 era per §4; update the pinned
   census; the three tests in C471 §3d.
4. p4ng: `detect_drift.py:117-119` raises `ValueError` on a vanished form (C471 §4
   measured it), and `app-eqtutorial.tex:74` / `sec-vetting-corrections.tex:13` cite
   the declaration by name.
5. Prose in §3 above.
6. The run that shows the key absent, under the RUN12 lock, and `run3_conformance.bb`
   green on it. **That run is the acceptance bar and it is also the test of §2** — if
   the retag was chosen wrongly, this is where it shows.

mathlib4 needs nothing: `r8CensusWmTrace` and `r8EraBoundary` are `native_decide`
over a frozen 792-row literal (C471 §3c), and the `variationalFreeEnergy` witness of
§3 reads Lean and a fixture, not records.

## 7. The gate was already red at HEAD (repaired, p4ng)

Running step 4 of the worklist protocol before committing: **`negative_controls.sh`
failed at HEAD**, before any edit of mine, and had been failing since slice (a)'s
commit `b5eebb5`. Eight pointers slice (a) wrote into `worklist.edn` reported
"file not found":

`r8_f_contract.clj:58`, `wm_workspace_gate.clj:272`, `witness-registry.edn:834`,
`bayes_factor_threshold_witness.clj:17`,
`model_reduction_free_energy_change_witness.clj:16`, `contract_lint.clj:360`,
`control-map-edges.edn:43`, `R3-decomposition.clean.edn:23`.

**All eight are valid** — each file exists and each line is within its line count
(checked individually). Two defects in `pointer_check.bb`, both repaired:

1. Its `roots` covered `futon2/src`, `futon2/scripts`, `wm-contract/` and mathlib4
   only. `futon2/checks/`, `p4ng/empirics-futon/`, `futon2/data/` and
   `futon6/holes/clean/` were unreachable. Roots **appended**, so no existing
   first-match resolution changes.
2. Its filename stem regex `[A-Za-z0-9_\-]+` excluded dots, so
   `R3-decomposition.clean.edn:23` was looked up as `clean.edn` — a file that exists
   nowhere. Stem widened to admit dots.

Same defect class as RUN11's review fix (p4ng `c320107`, the missing `scripts/`
root): a pointer the checker cannot resolve is not the same thing as a pointer that
does not resolve, and conflating them makes the gate useless in one direction and
noisy in the other. **The check still refuses bad pointers** — controlled on all four
failure modes (file-not-found, end-beyond-file, inverted range, and a dotted name
past its line count all exit 1; a valid dotted name exits 0).

The part worth carrying back to the protocol: slice (a)'s evidence line claims
"pointer_check 361 pointers / 0 unresolved". That was true when it ran and false when
it committed, because **the gate was run before its own pointers were written into
the ledger** — it certified a tree that did not yet contain the evidence it was
certifying. Run `pointer_check` after writing the row, not before.

The p4ng commit is `8012b0c`.

Gates for this slice, after the repair: `negative_controls.sh` PASS (16 negative, 10
positive); `pointer_check` 403 pointers, 0 unresolved. No `src/` change, so no
clj-kondo, no check-parens, no tests; no run, so no RUN12 lock.
