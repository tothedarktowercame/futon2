# L3 — Typed absences on the selection path (claude-2 → claude-3, 2026-11-01)

Plan: `futon2/holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md`,
slice L3. L1 (`3a98ab3029`) and L2 (`a1716abc9d` + review fix `11d9dc6852`) are merged;
darktower head is `11d9dc6852`. The review fix added `comparedPolicies = constructedCascades`
to Q11 with the negative `constructedNotCompared` (three cascades built, two compared,
record asserting nothing lacks G, conformed before).

## Goal (one behaviour)

Q7 today is `pathAbsenceCount = 0`: Lean trusts a count the projector supplies. The
selection path's absences are the ones that made 2026-10-31/11-01 a façade, and every one
of them is a typed value in the live record that the count never saw. L3 states them as
typed carriers and makes Q7 refuse each of them on a selection run. The inner path's
count stays for now; its typed replacement is a later slice.

## Primary sources (read these, not my summary)

Run record `futon2/data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn`
(shapes 1, 3 and 4 appear in 11 of 11 era-2 records; shape 2 in 6 of 11):

1. Selection receipt. The retained `:outer/selection` section is present in era-2 records;
   the separate `:outer-task-selection` key is the deleted selector's stub
   (`full_loop_runner.clj:1227`) and reads
   `{:schema :wm/outer-task-selection-v1, :status :absent, :reason :receipt-not-retained}`.
   The carrier is a digest of `:outer/selection`; the stub is not the receipt.
   (Corrected after claude-3's L3 bell, which caught the conflation.)
2. Relation of the chosen target unknown:
   `{:target "E-comprehension-foundation", :relation :unknown, :relation-known? false}`
   (the `:cascade-pick-relations` vector; 6 of 11 runs chose on an unknown relation).
   Known classes are `#{:focus :associated :useful-elsewhere}`, see `known-relation?`
   in `futon2/src/futon2/aif/outer_select.clj:55`.
3. Reach supplied from a parameter file, not observed:
   `:selection {:selected {:target "E-comprehension-foundation", :reach 1/2}}`;
   the value comes from `(:default-reach makeup)` at
   `futon2/src/futon2/aif/outer_observation.clj:119`, and
   `futon2/data/wm-learning/outer-makeup.edn` has `:default-reach 1/2`.
4. Learning reported applied with no classified ending:
   `:outer/learning {:status :applied, :ending-class :not-yet-evaluated, :observed-gain :none, …}`
   beside `:classification-absence {:reason :classification-receipt-absent, …}` for every
   consumed run (12 of 12 consumptions across the era).

Hand witness of the conformant shape, real data:
`futon2/holes/labs/wm-contract/witnesses/outer-selection-conformant-2026-11-01.edn`
(relations from a live `classify-target` run: two picks `:associated`,
E-comprehension-foundation `:unknown`; reach observed as 2/4 from four prior clicks and
609/615 from the Giraffe pass's `:graph-coverage`, not from cone counts; the makeup, Giraffe pass and library manifest shas are in the file).

## Carriers (Requirements.lean, after `pathAbsenceCount`)

```lean
/-- Where a value on the selection path came from.  `observed` carries the digest
of the receipt that measured it this run; a digest of `0` is an absence. -/
inductive Provenance where
  | observed (digest : Id)
  | defaulted   -- supplied by a parameter file, e.g. outer-makeup :default-reach
  | absent      -- not supplied, status absent, or unknown
  deriving DecidableEq, Repr

/-- The selection run's path from field to chosen task. -/
structure SelectionPath where
  /-- The retained `:outer/selection` section. -/
  receipt : Provenance
  /-- Relation of the chosen target to the retained focus (classify-target receipt). -/
  pickRelation : Provenance
  /-- Reach of the chosen target over the field (Giraffe cone / unblocks). -/
  pickReach : Provenance
  /-- Runs consumed for learning, each with its classification receipt. -/
  consumedRuns : List (Id × Provenance)
  /-- `:outer/learning {:status :applied}`. -/
  learningApplied : Bool
  deriving DecidableEq, Repr
```

RunFacts gains `selectionPath : Option SelectionPath`. No default value.

## Q7

```lean
def Provenance.isObserved : Provenance → Bool
  | .observed d => d ≠ 0
  | _ => false

def Q7 (r : RunFacts) : Bool := decide (
  r.pathAbsenceCount = 0 ∧
  (r.kind = .selection ↔ r.selectionPath.isSome) ∧
  (∀ p ∈ r.selectionPath,
    p.receipt.isObserved ∧ p.pickRelation.isObserved ∧ p.pickReach.isObserved ∧
    (p.consumedRuns.map Prod.fst).Nodup ∧
    (p.learningApplied ↔ (p.consumedRuns ≠ [] ∧ ∀ c ∈ p.consumedRuns, c.2.isObserved))))
```

The `↔` on learning is deliberate in both directions: applied with an unclassified
or absent ending is learning from nothing (the live shape); not applied when every
consumed run is classified is a refusal to learn. If you find a run shape where one
direction is wrong, state it in the bell rather than weakening the clause.

`defaulted` is never acceptable on this path: a reach or relation read from a file is
the "default as parameter" tripwire in the plan. There is no `tolerated` constructor.

## Fixtures

- `selectionGood`: `selectionPath := some { receipt := .observed 80001, pickRelation := .observed 80002, pickReach := .observed 80003, consumedRuns := [(80010, .observed 80011), (80012, .observed 80013)], learningApplied := true }`. Still conforms.
- `good`, `workOneCascade`, `click20`, and every work-kind fixture: `selectionPath := none`. `good_conforms`, `work_one_cascade_conforms` still hold; `click20_violations` unchanged.
- Negatives, each `{ selectionGood with selectionPath := some { … } }` changing one thing:
  - `receiptNotRetained` — `receipt := .absent` (every record to date)
  - `relationUnknown` — `pickRelation := .absent` (click 12 and five others)
  - `relationStatedInFile` — `pickRelation := .defaulted`
  - `reachDefaulted` — `pickReach := .defaulted` (every record to date, `:default-reach 1/2`)
  - `observedZeroDigest` — `pickReach := .observed 0`
  - `learningWithoutObservation` — `consumedRuns := [(80010, .absent), (80012, .absent)], learningApplied := true` (the live shape)
  - `learningAppliedNothingConsumed` — `consumedRuns := [], learningApplied := true`
  - `learningRefusedWithObservations` — `learningApplied := false`, consumed runs as in selectionGood
  - `duplicateConsumedRun` — the same run id twice
  - `selectionRunWithoutPath` — `{ selectionGood with selectionPath := none }`
  - `workRunWithSelectionPath` — `{ good with selectionPath := selectionGood.selectionPath }`
- Theorems: `Q7 selectionGood = true`; one `… _not_Q7` per negative; and
  `selection_absence_negatives_fail_only_Q7` stating `violations x = [.q7]` for all of them.

## Reader

`PartialRunFacts.q7` already exists; nothing to add. Confirm `absent_G_carrier_alerts`
and the existing reader theorems still build.

## Gates

- Worktree `~/worktrees/mathlib4-l3-selection-absences`, branch `claude3/l3-selection-absences`
  from darktower `11d9dc6852`. Populate `.lake` with `lake exe cache get` or a real
  `cp -a` of the primary's; never a symlink into `/home/joe/code/mathlib4/.lake`,
  whole or partial.
- `lake build DarkTower.WarMachine.Requirements DarkTower.WarMachine.RequirementsReader`
  green, zero `sorry`; log to `~/notes/l3-selection-absences/build.log` and give its sha256.
- Do not merge. Bell claude-2 with: sha, build log sha, which clauses you did not state
  and why, and anything in the live record that does not fit `Provenance`.

## Decided elsewhere (from your L2 bell; do not fold into L3)

- G precision / float near-ties: L4, with a declared scale carrier and the projector
  rounding to it. Not a margin in Q11.
- One id namespace for cascade digest and policy id: L4 projector; the cascade digest is
  the policy id.
- Abstention as a legitimate outcome (a `Decision` with an abstain case, Q11 then requires
  abstention G strictly below every policy G): L3b, after this lands.
- Whether the inner path produces a stopped-kernel G: L5 requirement.
