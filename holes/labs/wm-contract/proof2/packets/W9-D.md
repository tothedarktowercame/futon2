# W9-D — the co-application kernel at C1's containment order, the honest closer for [R2 R6] (DISCOVERY)

PROOF-2a ⟨2⟩1d. Discovery only: no Lean, mathlib4, or p4ng tree was written.
Inputs read at mathlib4 8d98c63f97 (HEAD), p4ng bc4e22c (committed DAG
outputs), futon2 registry at HEAD (`git show HEAD:holes/labs/wm-contract/
aif-equations.edn`; the worktree copy carries another lane's uncommitted edit
and was not used). Predictions in §3 were TRIALLED against scratch copies
under `/tmp/w9d/` (`LEAN_ROOT=/tmp/w9d/m4/` `AIF_EQ=/tmp/w9d/eq.edn`
`AIF_OUT=/tmp/w9d/out/`, the W11-D harness: real `.ilean` files copied in and
touched newer than sources; nothing ran Lean). The baseline scratch reproduced
the committed counts exactly (30 imported / 6 term-present / 3 absent /
1 consumer-unplaced of 40, 2 shared-module-only) and the committed edge set
byte-for-byte at edge grain before any trial edit.

## 1. The rows and the declarations they name

Registry line numbers from `git show HEAD:…/aif-equations.edn`; Lean line
numbers at mathlib4 8d98c63f97.

**`:containment-order`** (registry :608; defines `:r`, node R6, theory-defined,
`:imports [:interp]`)
- `:lean "Proof2.ContainmentOrder.containmentOrder"`, `:lean-status :closed`,
  `:lean-term {:interp "pat"}`.
- `DarkTower/WarMachine/Proof2/ContainmentOrder.lean:68` —
  `def containmentOrder (pat : ι → InterpretedPattern V) (a b : ι) : Prop`.
  `pat` is a **free binder** (C1's `:lean-term-note`: without it the join
  cannot see `:interp` at R6). Imports: `CascadeOrder`, `CascadeTransition`
  (ContainmentOrder.lean:1-2). Also hosts `Stratification` (:88) and
  `acyclicDescent_of_stratification` (:111) — the smallest sufficient
  condition for acyclicity, C1's finding that the row states none.
- Nothing anywhere instantiates `containmentOrder`: a grep over `DarkTower/`
  finds the name only inside its own module. It is a construction with no
  caller.

**`:co-application-kernel`** (registry :631; defines `:B-co`, node R4,
theory-defined, `:imports [:r :interp]`)
- `:lean "Proof2.CoApplicationKernel.coApplyKernel"`, `:lean-status :closed`,
  `:lean-term {:interp "pat"}`.
- `DarkTower/WarMachine/Proof2/CoApplicationKernel.lean:99-100` —
  `noncomputable def coApplyKernel (pat : ι → InterpretedPattern V)
  (r : ι → ι → Prop) (s s' : Finset V) : ℝ`. Both `pat` and **`r` are free
  binders**. Imports: `CascadeTransition`, `CascadeOrder` (:1-2). Also hosts
  `enabledFrontier` (:92), `frontierConflict` (:108), `hasRestrictedMeets`
  (:46), `coApplyKernel_eq_cascadeKernel_of_chain` (:175).
- The module does **not** import `ContainmentOrder`. The only instantiations
  of `coApplyKernel` in the tree are at OTHER relations:
  `ChainOrder.lean:204` applies it at `listChain` (the precedence-list chain)
  and `CascadeCoapplication.lean:159` at an arbitrary `IsChainOrder` relation.
  No module applies it at `containmentOrder pat`. That is the whole of W9's
  Lean-side gap, in one sentence.

**`:interp`** (registry :80; exogenous symbol, node R2, glossary "Structured
observation")
- `:lean "CascadeTransition.InterpretedPattern"` naming
  `DarkTower/WarMachine/CascadeTransition.lean:22`
  (`structure InterpretedPattern`), but gen_lean_dag.bb does not read an
  exogenous `:lean` as a term (G0's limit), so the row contributes the node
  placement only: `:interp`'s source modules are whatever equations place at
  R2 — today exactly `Proof2.ObservationAtMachine` (via `:observe`).
- The row's own note records that the R2-vs-R6 placement is "recorded, not
  decided" and that moving it to R6 removes the edge (theory edges 37 → 35).

Current classes (p4ng bc4e22c, reproduced in scratch):
- **[R2 R6] `interp`: term-present-not-imported**, `:how :parameter`, binder
  `pat` on `containmentOrder`. R6's modules (MachinePolicySet,
  ContainmentOrder, PolicyPosteriorAtMachine) reach no R2 module.
- **[R6 R4] `:r`: imported — but `:via [PolicyPosteriorAtMachine]` only.**
  R4's `RolloutAtMachine` imports the posterior module, so the edge is blue at
  NODE grain while the term's own module (`ContainmentOrder`) is unreached by
  any R4 module. This is exactly the W4 log's "`:r` enters R4 only at node
  grain". The generator's `:via-shared-module-only` guard does not catch it
  because PolicyPosteriorAtMachine is placed at one node; the dishonesty is
  visible only by reading `:via` against the term's source module.
- [R2 R4] `interp`: imported, via RolloutAtMachine → ObservationAtMachine —
  node grain, rides the observation law, not the interpretation.

## 2. "The machine's interpretations" as a Lean term

**No machine-side declaration supplies `pat`.** Every consumer of
`InterpretedPattern` binds the pattern map free: `containmentOrder`
(ContainmentOrder.lean:68), `coApplyKernel` (CoApplicationKernel.lean:99),
`enabledFrontier`, `frontierConflict`, `ChainOrder.listPat` (which builds one
from a LIST argument, still free). None of the `…AtMachine` modules
(ObservationAtMachine, BeliefStepAtMachine, ActionAtMachine,
RolloutAtMachine, PrefixFreeEnergyAtMachine, PolicyPosteriorAtMachine)
mentions `InterpretedPattern` at all. One must be written.

What the code does (so the Lean has something to be honest ABOUT):

- The interpretations arrive **from outside**: `flight_runner.clj:374-405`
  (`ask-fn`) issues a `want-interpretation` request per unproduced want and
  reads the seat's answer through `agency-answer-fn` (`flight_runner.clj:117`,
  boxes `:r3-prompt`/`:r3-flight-ask`); a valid answer is PUBLISHED, anything
  else is a typed `:need` (`:no-criterion`, `:not-answered`, `:declined`,
  `:rejected`, …), never defaulted. The published interpretations live in the
  flight's target view (`:interpretations <target> :patterns`).
- `construction.clj:113-230` (`containment-order`) builds `r` from those
  interpretations: units are pattern applications, descent edges are the
  `[a b]` pairs with `a ≠ b` whose `(:produces a)` meets `b`'s guard needs —
  `containmentOrder` in Lean, exactly (C1 already established this
  correspondence).
- `efe.clj:1006-1065` (`order-use`) reads the order off the action's
  `:construction-receipt` and, on a non-chain order with no refusal and no
  precedence violations, emits `:kernel-step {:co-apply {:units :descent
  :patterns}}` which `cascade_model_manifest.clj:375-399`
  (`co-apply-kernel`) scores. The refusal arms matter: **no order on the
  receipt, a refused order (`:cyclic-containment`), or precedence violations
  all fall back to the list kernel** with the reason recorded in `:meta`.

So in the code the crossing is real data flow: R2 (seat answers, published
interpretations) → R6 (the order built from them) → R4 (the kernel scored
under that order). In Lean none of the three crossings is written: `pat` is
free at R6 and R4, and `r` is free at R4.

## 3. Proposed modules and the trialled edge deltas

Two modules, two packets. The design follows the lesson W10 recorded and
W11-D re-used: the application lives in the module that holds the row's term.

### W9-1 — `Proof2/CoApplicationAtMachine.lean` (the [R6 R4] `:r` closer)

```lean
import DarkTower.WarMachine.Proof2.ContainmentOrder
import DarkTower.WarMachine.Proof2.CoApplicationKernel

noncomputable def machineCoApplyKernel (pat : ι → InterpretedPattern V)
    (s s' : Finset V) : ℝ :=
  coApplyKernel pat (containmentOrder pat) s s'
```

plus: the `Except`-carrying refusal mirror of `order-use` — the co-application
step is ABSENT (typed, carrying `¬ acyclicDescent (containmentOrder pat)`)
when the order is cyclic, never silently the list kernel; on the ok arm
`machineCoApplyKernel_rowsum`/`_isDistribution` by composition with
`coApplyKernel_rowsum`; the chain case by
`coApplyKernel_eq_cascadeKernel_of_chain`; `frontierConflict` at
`containmentOrder pat` unchanged. The acyclicity hypothesis is C1's
`Stratification` (the row states none; the machine's refusal is what the code
does).

Registry edit: `:co-application-kernel` `:lean`
`"Proof2.CoApplicationKernel.coApplyKernel"` →
`"Proof2.CoApplicationAtMachine.machineCoApplyKernel"` (`:lean-at` appended;
the `:lean-term {:interp "pat"}` hint stays valid — the new declaration still
binds `pat`).

**Trialled** (stub module, scratch registry): counts **unchanged**
(30/6/3/1, 2 shared-only, 40 edges, lean-only 49), and the full edge-set diff
against the committed edn is exactly one edge's `:via`:

- **[R6 R4] `:r`: `:via [PolicyPosteriorAtMachine]` →
  `:via [ContainmentOrder, PolicyPosteriorAtMachine]`** (both term entries,
  `:r` and `:pi`, since they share the edge). The class was and stays
  `:imported` — what changes is that the blue is now **earned**: the kernel's
  `r` IS `containmentOrder pat`, and the term's own module carries the edge.
  This is the honest form of the blue W4 logged as node-grain-only.

No regressions: [R2 R4] stays imported via RolloutAtMachine; [R6 R4] `:pi`
unchanged; CoApplicationKernel leaves R4's placed set but remains in the
closure (`modules-unnamed` 199 → 200; ChainOrder still imports it, unplaced
as today).

### W9-2 — `Proof2/ObservedInterpretation.lean` + `Proof2/ContainmentOrderAtMachine.lean` (the [R2 R6] `interp` closer)

W9-1 alone does **not** move [R2 R6] (trialled: still
term-present-not-imported, binder `pat`). Earning that edge needs the R2 side
of §2 written:

```lean
-- Proof2/ObservedInterpretation.lean  (imports CascadeTransition only)
inductive InterpretationAbsence (W : Type*) where  -- the ask step's
  | notAnswered (w : W) | declined (w : W) | ...   -- non-publication outcomes
structure ObservedInterpretation (ι V W : Type*) where
  pat : ι → InterpretedPattern V                   -- the published supply

-- Proof2/ContainmentOrderAtMachine.lean
--   (imports ContainmentOrder, ObservedInterpretation)
def machineContainmentOrder (ob : ObservedInterpretation ι V W) (a b : ι) : Prop :=
  containmentOrder ob.pat a b
```

The honest content: the machine does not COMPUTE interpretations, it receives
them — the supply is a structure, and a want with no published interpretation
is a typed absence (mirroring `ask-fn`'s `:needs`), never a defaulted pattern.
`machineContainmentOrder` is `containmentOrder` **at the machine's observed
supply**, with absence propagation (no supply for the candidate's patterns →
no order, the code's `:no-order-on-receipt` arm).

Registry edits: re-point `:containment-order` `:lean` →
`"Proof2.ContainmentOrderAtMachine.machineContainmentOrder"`; **add one new
row at R2** (W11-2 precedent) naming
`Proof2.ObservedInterpretation.observedInterpretation` — an equation row, not
an exogenous edit, because placement requires an equation (`:interp` is
exogenous and cannot place a module). The new row is a plan-owner decision;
the trial used `{:id :observed-interpretation :defines :interp-supply
:node :R2 :imports []}`. Also update `:containment-order`'s `:lean-term` hint:
the new declaration binds `ob`, not `pat` (`{:interp "ob.pat"}` or drop the
hint — stale hints only matter while the edge is below `:imported`, but the
record should be true).

**Trialled** (stacked on W9-1): imported 30 → **31**, term-present 6 → **5**,
absent 3, unplaced 1, lean-only 49 unchanged; the full edge-set diff is
exactly:

- **[R2 R6] `interp`: term-present-not-imported → imported,
  `:via [ObservedInterpretation]`** — a genuine use: the R6 module's order is
  built from the R2 module's supply. Nothing else moves.

**The ruling the packet asks for.** [R2 R6] becomes imported by a genuine use
of the interpretation **only under W9-2**. The placement question the
`:interp` note left open is thereby answered in the R2 direction: the code
crosses R2 → R6 as data (seat answers feed `construction/containment-order`),
so the edge should exist and be earned, not deleted by re-placement. The
alternative in the note (move `:interp` to R6, 37 → 35 edges) remains the
plan owner's to take, but taking it now would discard an edge the code
realises.

## 4. Interaction with W11-2 (PrefixFreeEnergyPosterior, in flight at codex-1)

- **Module independence.** W9-1 imports ContainmentOrder + CoApplicationKernel;
  W9-2 imports ContainmentOrder + ObservedInterpretation; W11-2's
  PrefixFreeEnergyPosterior imports PolicyPosteriorAtMachine + the moved
  FPosterior section. No pair imports the other; no cycle risk (the generator
  refuses cycles itself; neither trial tripped it). Landing order is free.
- **The contested edge is [R2 R6].** W11-D §2 found that W11-2's new R6 row
  flips [R2 R6] to imported INCIDENTALLY, because PrefixFreeEnergyPosterior
  imports ObservationAtMachine: `:via [ObservationAtMachine]`. That blue is
  **not earned** — the posterior module uses the observation LAW
  (`machineObservationLaw`), not the interpretation; it is the same artefact
  class as the CascadeOrder blues C1 corrected, blue by node grain while the
  term's genuine carrier is absent. W9's ruling: **refuse that reading**. If
  W11-2 lands first, [R2 R6] will read imported before W9-2 exists; the
  registry note should record that the edge's honest carrier is W9-2's
  ObservedInterpretation and that the intervening blue is the artefact. After
  both land, `:via` is `[ObservationAtMachine, ObservedInterpretation]` and
  the honest one is present.
- **Registry sequencing only.** Both lanes edit `aif-equations.edn` (W11-2
  re-points `:policy-set` + adds an R6 row; W9 re-points
  `:co-application-kernel` and `:containment-order` + adds an R2 row) — the
  hunks are disjoint but the file is shared; the second lane to land rebases,
  and whichever lands second regenerates the DAG once for both.
- W11-1 (`:policy-set` rebind) is orthogonal: MachinePolicySet stays in R6's
  placed set throughout both trials above; nothing here touches [R6 R17].

## 5. Implementation packets

**W9-1** — one module `DarkTower/WarMachine/Proof2/CoApplicationAtMachine.lean`
(`machineCoApplyKernel` + the refusal mirror of `order-use`'s arms +
rowsum/distribution/chain-case theorems; `Stratification` hypothesis for the
ok arm, `twoPatternCycleIsNotAcyclic` as the refusal's reachability case), the
`:co-application-kernel` re-point, `lake build` warrant for the module, then
DAG regeneration. Expected map delta: no count change; [R6 R4] `:via` gains
ContainmentOrder (trialled).

**W9-2** — one module pair `Proof2/ObservedInterpretation.lean` (the supply
structure and its absences; imports `CascadeTransition` only) and
`Proof2/ContainmentOrderAtMachine.lean` (`machineContainmentOrder` + absence
propagation + the agreement theorem with `containmentOrder`), the
`:containment-order` re-point, the new R2 row (plan-owner sign-off),
`:lean-term` hint update, `lake build` warrants for both modules, then DAG
regeneration. Expected map delta: imported 30 → 31, term-present 6 → 5,
exactly [R2 R6] moving (trialled).

W9-1 first if only one is taken: it repairs a blue that is currently
dishonest, while W9-2 creates one. Both are discovery-verified against the
generator; neither touches the code paths (the Clojure already does what the
Lean will now say).

## Appendix — trial harness

Scratch at `/tmp/w9d/`: `m4/` (mathlib4 tree copy + real `.ilean` files touched
newer than sources), `eq.edn` (HEAD registry + the edits above), `out/`
(generator output). Baseline run reproduced p4ng bc4e22c's counts and edge
set before edits; the committed edn was diffed at
`git show bc4e22c:empirics-futon/aif-lean-dag.edn`. Stubs were textual-only
(no `.ilean`), which is safe for NEW modules; the known textual-fallback
hazard (PolicyPosteriorAtMachine's unmatched `end`) does not apply because
every pre-existing module resolved from its `.ilean`. The real p4ng outputs,
the real registry working copy, and the real mathlib4 tree were never
written.
