# L5-4L-1 — Lean: `SelectionGrounding.lean`, the non-façade check (claude-2 → codex-11, 2026-11-01)

## Why

`DarkTower/WarMachine/Requirements.lean` (darktower `221609fe66`) checks the *shape* of a War
Machine run: nine requirements over facts a projector asserts. Its positive selection fixture
`selectionGood` proves the shape is satisfiable, but with made-up ids. Joe, 2026-11-01: real
conformance "must come from something else, showing that the field was considered and
prioritised". This module adds carriers from which Lean recomputes the selection and checks the
record against its own facts. The semantics to mirror already exist in
`DarkTower/WarMachine/R20Step.lean` lines 91–120 (`SelectionStep`, `applySelectionStep`,
`applySelectionCascade` over `GiraffeFacts`); they are `noncomputable` there, so you write a
computable copy over concrete types and may (optionally) prove it agrees.

A hand-built real-data example of the shape you are formalising:
`futon2/holes/labs/wm-contract/witnesses/outer-selection-conformant-2026-11-01.edn` (three
cascades applied to the 2026-11-01 field; per-cascade `:applied {:steps … :pick … :giraffe-facts …}`,
`:relation`, `:score`). Read it first; the README beside it explains the numbers.

## Module `DarkTower/WarMachine/SelectionGrounding.lean` (new file; imports Requirements)

```lean
abbrev Id := Requirements.Id          -- ℕ, 0 = recorded absence

structure FieldFact where
  ready : Bool
  cone : ℕ
  unblocks : ℕ
  pin : Id                             -- digest of the Giraffe pass row it came from
  deriving DecidableEq, Repr

inductive FieldEntry where
  | fact (task : Id) (f : FieldFact)
  | excluded (task : Id) (reason pin : Id)   -- typed exclusion, in the field, not dropped
  deriving DecidableEq, Repr

inductive Step where
  | readyGate | byCone | byUnblocks        -- recomputable, R20Step semantics
  | named (step : Id)                      -- a README step Lean cannot recompute
  deriving DecidableEq, Repr

inductive Relation where
  | focus | associated | usefulElsewhere | unrelated | unknown
  deriving DecidableEq, Repr

structure Terms where
  risk : ℚ
  ambiguity : ℚ
  novelty : ℚ
  deriving DecidableEq, Repr

structure GroundedPolicy where
  cascade : Id
  steps : List Step
  pick : Option Id                         -- none = this cascade picks nothing
  reach : ℚ
  relation : Relation                      -- of the pick to the retained focus
  terms : Terms
  g : ℚ
  deriving DecidableEq, Repr

structure SelectionGrounding where
  enumerated : Finset Id
  field : List FieldEntry
  policies : List GroundedPolicy
  abstention : Terms × ℚ                   -- stopped kernel's terms and G
  chosen : Id                              -- cascade id
  deriving DecidableEq, Repr
```

Semantics, computable:
- `applyStep (field : List (Id × FieldFact)) : Step → Option (List (Id × FieldFact))`:
  `readyGate` filters `ready`; `byCone` is a *stable* sort by `cone` descending; `byUnblocks`
  stable by `unblocks` descending (match R20Step lines 109–115: `insertionSort` with `≤` on the
  flipped arguments); `named _` gives `none` (not recomputable).
- `recomputedPick (g : SelectionGrounding) (p : GroundedPolicy) : Option (Option Id)`: fold the
  steps over the `fact` entries of the field (exclusions are not candidates); `none` if any step
  is `named`; otherwise `some (head?.map Prod.fst)`.

Requirements (each `: SelectionGrounding → Bool`, then an `Outcome`-style result where
"unverifiable" is possible):
- **G1 fieldConsidered**: the field's tasks, as a list, are exactly `enumerated` with no
  duplicates; every `fact` has `pin ≠ 0`; every `excluded` has `reason ≠ 0 ∧ pin ≠ 0`.
- **G2 prioritised**: for every policy, `recomputedPick g p = some p.pick`. If any policy has a
  `named` step, the result is *unverifiable* (use a three-valued result type or
  `Option Bool`), never true.
- **G3 scoredFromTerms**: for every policy `p.g = p.terms.risk + p.terms.ambiguity − p.terms.novelty`,
  `0 ≤ p.reach ≤ 1`, and (`p.pick = some _ → p.relation ≠ .unknown`); abstention's G likewise
  equals its terms' sum.
- **G4 agreesWithRunFacts (r : Requirements.RunFacts)**: `g.chosen` is `r.currentDecision`'s
  cascade (`.pick _ c`), `g.policies.map (fun p => (p.cascade, p.g)) = r.policyG` as finsets of
  pairs, `g.abstention.2 = r.abstentionG`, `g.enumerated = r.enumeratedTasks`.

`grounded (g) (r) : Bool` = G1 ∧ G2 = true ∧ G3 ∧ G4.

## Fixtures and theorems (all by `native_decide` or `decide`)

Positive, small: a field of five tasks (four facts, one typed exclusion), three policies with
steps `[readyGate, byCone, byUnblocks]`, `[readyGate, byUnblocks, byCone]` and `[readyGate]`,
picks equal to the recomputed heads, terms with exact rationals summing to G (use the witness's
numbers: 29949/10000 for the chosen, 34841/10000 for the others, abstention 47875/10000, with
terms you split so the sums hold), relation `.associated` on each pick, chosen = the minimum.
A matching `RunFacts` for G4: build it as `{ Requirements.selectionGood with … }` with the same
ids and G values. Theorem `small_witness_grounded`.

Negatives, each changing one thing, each with a theorem naming the one requirement that fails:
- `taskMissingFromField` (G1), `duplicateTaskInField` (G1), `unpinnedFact` (G1),
  `exclusionWithoutReason` (G1);
- `menuOrderPick` (G2): a policy's recorded pick is a lower-cone task than the recomputed head;
- `pickFromEmptyGate` (G2): `pick = some t` where the gate leaves nothing;
- `namedStepUnverifiable`: G2 is unverifiable, not true and not false;
- `gNotFromTerms` (G3), `reachAboveOne` (G3), `pickOnUnknownRelation` (G3);
- `policyGDisagrees` (G4): the grounding's G differs from `RunFacts.policyG`.

Optional, if cheap: `applyStep_agrees_with_R20Step` for the three named steps.

## Gates (read these; two have bitten before)

- Worktree: `git -C /home/joe/code/mathlib4 worktree add ~/worktrees/mathlib4-l5-4l-1 -b codex11/l5-4l-1-selection-grounding 221609fe66`.
  **Never** `~/worktrees/mathlib4/<name>`: that path is a symlink into the primary checkout.
- Cache: in the worktree run `lake exe cache get`, or `cp -a /home/joe/code/mathlib4/.lake .lake`
  (12 GB, slow). **Never** symlink `.lake` or any part of it to the primary's: that rewrote
  29,315 primary oleans on 2026-11-01.
- `lake build DarkTower.WarMachine.SelectionGrounding` green; zero `sorry`; `#print axioms` on the
  positive theorem. Log to `~/notes/l5-4l-1/build.log`, give its sha256.
- Add the module to whatever imports list makes `lake build DarkTower` include it (look at how
  `R20Step` or `CloseOutcomeImage` is listed).
- Commit on the branch; do not merge; do not touch `Requirements.lean`.
- Bell claude-2 (`python3 /home/joe/code/futon3c/scripts/agency_send.py --from codex-11 --to claude-2 --kind bell`)
  with: sha, build log sha, the theorem list, and anything you could not state.
