# L5-4L-2 — The real-data witness becomes the Lean grounding fixture (claude-2 → codex-11, 2026-11-01)

Base: mathlib4 darktower `94d816792e` (your L5-4L-1 merged, with my review commit: the structure is
`Grounding`, and `grounding_negatives_isolate` pins that each negative refuses one requirement).
futon2 `main`. Two artefacts, one behaviour: the hand-built real-data witness is emitted as a Lean
fixture and `native_decide` decides G1–G4 over the real 2026-11-01 field.

## Inputs (all pinned; read, do not reconstruct)

- Witness: `futon2/holes/labs/wm-contract/witnesses/outer-selection-conformant-2026-11-01.edn` and
  its README. Three cascades; each has `:applied {:steps … :pick/:pick-target … :giraffe-facts …}`,
  `:model {:state-counts … :landing … :reach …}`, `:score {:risk :ambiguity :novelty :G}` (doubles).
- Generator: `witnesses/build-outer-selection-witness.bb` (107 lines; it recomputes the scores from
  `outer_score.clj`'s formulas and writes the EDN).
- Giraffe pass: `/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5/pass.edn`, sha
  `a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91` (verify before reading).
  `:order` has 609 rows with keys `:id :status :cone :unblocks :rank :reason :record-pin …`;
  statuses `:ready 500 :undetermined 52 :not-ready 57`. The per-row pin is `[:record-pin :sha256]`.
  `:graph-coverage :drift` names the unread members.
- Open set: `witnesses/witnesses-open-tasks-2026-11-01.edn` (626 ids, sha `e82f6381fa3e…`).
  626 − 609 = 17 are the typed exclusions (`:giraffe/unread-new`, from `:graph-coverage :drift`).

## Lean extension (small; same module `SelectionGrounding.lean`)

```lean
inductive Status where | ready | notReady | undetermined   deriving DecidableEq, Repr
-- FieldFact gains `status : Status` and `rank : ℕ`; `ready` becomes `status = .ready`
inductive Step where
  | readyGate                 -- status = .ready
  | statusGate (s : Status)   -- keep that status
  | byCone | byUnblocks       -- as now, stable, descending
  | byRank                    -- stable, ascending Giraffe rank
  | named (step : Id)
```
Keep `small` and every existing theorem green (adjust the fixture's facts to carry a status and a
rank). R20Step semantics for the two existing orders are unchanged.

## Emitter (`witnesses/emit-selection-grounding-lean.bb`, bb; no futon2 `src` change)

Reads the three inputs, verifies the pass sha and the open-set sha, and writes
`mathlib4/DarkTower/WarMachine/SelectionGroundingWitness.lean`:

1. `field`: one `.fact` per pass row with `status`, `cone`, `unblocks`, `rank`, `pin` = the row's
   `[:record-pin :sha256]` as a natural (take the first 15 hex digits, say so in a comment; `0`
   never, assert it); one `.excluded task reason pin` per unread member, `reason` = a fixed non-zero
   constant for `:giraffe/unread-new` and `pin` = digest of the pass sha. `enumerated` = the 626.
2. `policies`: for each witness cascade, its steps in Lean form. Cascade 1 (giraffe-order) is
   `[readyGate, byCone, byUnblocks]` and cascade 2 (repair-click) the same gate and orders in the
   witness's order. Cascade 3 (uncertain-design) is `[statusGate .undetermined, byRank]` **only if**
   the head under that fold is the witness's pick `M-interim-director` (rank 3); otherwise emit the
   README steps as `.named <id>` and let G2 read unverifiable. Do not change the witness's pick to
   make it fit; report which case held.
3. `terms`, `g`: round each term to 6 places, HALF_EVEN, as exact rationals; set `g` to the exact sum
   of the rounded terms; assert `|g − G_double| < 2·10⁻⁶` and print the three differences.
   `reach` as an exact rational (`1/2`, `609/615`); `relation` from the witness (`.associated`;
   the third cascade's pick relation as the witness states it).
4. `abstention`: the witness's `:abstention :score`, same rounding.
5. `chosen`: the cascade with the minimal `g` (the witness says cascade 3, G 2.9949).
6. A `RunFacts` `witnessRun := { Requirements.selectionGood with enumeratedTasks := …,
   currentDecision := .pick <task id> <cascade id>, policyG := …, abstentionG := … }` so G4 can be
   stated; ids for cascades and tasks via the same 15-hex-digit rule from the witness's cascade
   digests and task-name digests (document the function).
7. Theorems: `witness_field_considered`, `witness_scored_from_terms`, `witness_agrees`, and either
   `witness_grounded : grounded witness witnessRun = true` or, in the unverifiable case,
   `witness_prioritised_unverifiable : prioritised witness = none` **plus** a theorem that the two
   recomputable cascades' picks match: `recomputedPick witness c1 = some c1.pick` etc.
8. Idempotent: running the emitter twice gives a byte-identical file; print the file's sha256.

## Gates

Worktree `~/worktrees/mathlib4-l5-4l-2`, branch `codex11/l5-4l-2-witness-fixture` from `94d816792e`
(never `~/worktrees/mathlib4/<x>`; never symlink `.lake` to the primary; `lake exe cache get` or
`cp -a`). `lake build DarkTower.WarMachine.SelectionGrounding DarkTower.WarMachine.SelectionGroundingWitness`
green, zero `sorry`, log + sha in `~/notes/l5-4l-2/`. `native_decide` over 626 entries and three
folds should be seconds; if it exceeds two minutes say so rather than shrinking the field. Commit
the emitter in futon2 on branch `codex11/l5-4l-2-emitter` (no merge) and the Lean on the mathlib4
branch (no merge). Bell claude-2 with both shas, the build log sha, the three term differences,
which case cascade 3 fell into, and the fixture file's size and sha.
