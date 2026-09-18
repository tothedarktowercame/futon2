# E07 republish blocker — the FUNDAMENTALS cap join lapsed: findings

2026-09-18. Author: zai-35, discovery only, on claude-4's dispatch. No
registry, verdict, control, or generator was edited; nothing was re-pinned.
Every claim below is backed by a probe run or a file read this dispatch.

Context (established by claude-4, not re-derived): `gen_rnode_dossiers.py`
caps the readiness rung of any R-node joined by a FUNDAMENTALS `:in` entry;
it now reports `:capped 0, :unjoined-fundamentals 6`; four negative controls
fail. The join is by bare Lean declaration name: each `:in` entry's
declarations-read names (first `abbrev|structure|def|noncomputable def|axiom|class`
match per `:read`, `DECL` regex at gen_rnode_dossiers.py:282-284,
`fundamental_decls` :288-296) intersected with each aif-equations row's
`:lean` first token (`lean_name` :298-306, join at :314-322).

## 1. The six `:in` entries, their extracted names, and what joins

Extracted with the generator's own `DECL` logic applied to the current
FUNDAMENTALS.edn `:read` strings:

| Entry (verdict `:in`) | DECL-extracted names | Rows binding any name today | Joined before futon2 `a594a772` (2026-09-16)? |
|---|---|---|---|
| `:fundamental/predictive-outcome-kernel-constructor` | `{PredictiveOutcomeKernel, GenerativeModel}` | **none** (R4 `:forward-model` binds `predictedOutcome`) | **YES** — R4 bound `PredictiveOutcomeKernel` (introduced `94493d77`, removed `a594a772`) |
| `:fundamental/belief-to-state-distribution` | `{BeliefState}` | none (R1 binds `ExactBeliefTrajectory.exactBeliefAt`; R3 binds `LaplaceBeliefUpdate.beliefUpdate`) | no — `BeliefState` never appeared in any `:lean` (git `-S`, whole history) |
| `:fundamental/controlled-transition-kernel` | `{TransitionKernel}` | none | no — never bound |
| `:fundamental/policy-conditioned-state-predictive` | `{}` — **empty**: both `:read` strings lack a DECL keyword ("ambiguity takes …", "observationModel : …") | none, and **cannot join structurally** | no — always empty |
| `:fundamental/machine-preference-distribution` | `{PreferenceDistribution, C}` | none (no `:preference` row exists) | no — never bound |
| `:fundamental/parameter-kernels` | `{}` — **empty**: the carrier read is "ParameterPriorKernel (…) := …" with no `abbrev`/`def` keyword in the prose | none, and **cannot join structurally** | no — always empty |

**Claude-4's numbers confirmed: 1 joined before, 0 join now.** Verified two
ways: the current registry (all 21 rows' `:lean` values enumerated — none
contains any of the six entries' names) and `git log -S ':lean "<name>"'`
over the whole aif-equations.edn history: `PredictiveOutcomeKernel` is the
ONLY one of these names ever to appear, entering at `94493d77` and leaving at
`a594a772`. Note additionally: two of the six entries (policy-conditioned-
state-predictive, parameter-kernels) have EMPTY extracted-name sets, so they
could not join even against a registry that bound their carriers — a
separate, pre-existing fragility of the prose-based extraction.

## 2. `predictive-outcome-kernel-constructor` vs R4 today — the exact gap

**What the entry read** (pins are to Holes.lean at the read date; current
line numbers given alongside, the file has grown):

- Holes.lean:6757-6759 then, **:7024-7025 now**:
  `abbrev PredictiveOutcomeKernel (PolicyIndex : Type*) (Obs : Vertex → Type*) := ProbabilityKernel PolicyIndex (Outcome Obs)`
  — "A type, and its docstring says the evidence for it is a Witness module."
- Holes.lean:6787-6790 then, **:7053+ now**: `structure GenerativeModel`
  carrying `observation : ProbabilityKernel State (Outcome Obs)`,
  `transition : TransitionKernel State Action`, `policyPrior :
  PolicyPriorKernel PolicyIndex` — "three separate kernels, never composed
  into a kernel over Outcome."
- Holes.lean:6792-6799 then, **:7060+ now**: `generativeFactorMass` — the
  one-step joint as a SCALAR in ℝ, "a number, not a normalized row over
  Outcome; nothing sums it over states."

**What R4 binds today** (PolicyRollout.lean:112-124, read and previously
build-verified this session):

```lean
/-- `Q(o_τ | π) = Σ_s A(s, o_τ) Q(s_τ | π)` (Da Costa et al. 2020, eq. (44), ...) -/
noncomputable def predictedOutcome (M : ForwardModel S O U) (π : ℕ → U) (n : ℕ) (o : O) : ℝ :=
  ∑ s, M.A s o * rolloutState M π n s
theorem predictedOutcome_nonneg ... : 0 ≤ predictedOutcome M π n o
theorem predictedOutcome_sum  ... : ∑ o, predictedOutcome M π n o = 1
```

**The type difference, stated precisely.** The carrier is a kernel-TYPED
value: an inhabitant of `PredictiveOutcomeKernel` is a record whose TYPE
carries the well-formedness obligations (nonneg, row-sum — whatever
`ProbabilityKernel` packages; I did not re-read `ProbabilityKernel`'s
definition this dispatch and flag that). `predictedOutcome` is a
ℝ-valued function family with those obligations as SEPARATE THEOREMS
(`predictedOutcome_nonneg`, `predictedOutcome_sum`), and — the deeper
difference — it is indexed by `(M, π, n)`, not by `PolicyIndex` alone; a
kernel `ProbabilityKernel PolicyIndex (Outcome Obs)` would have to abstract
or encode the model, the policy and the step index into its domain.

**What would have to be true for one to discharge the other** (stated, not
decided): (i) a `ProbabilityKernel` record CONSTRUCTED from
`predictedOutcome` — the function plus its two proofs — would inhabit the
carrier's type; (ii) the index mismatch would have to be resolved: either
`PolicyIndex` is instantiated so that `(M, π, n)` is recoverable from it, or
the kernel is stated over the composite index; (iii) in SUBSTANCE,
`predictedOutcome` composes `A` with `rolloutState` (which composes `B`
forward) — it IS a composition of the model's kernels into a distribution
over Outcome, which is what the entry's finding said did not exist. Whether
a composition with separately-proved normalization discharges a
constructor-obligation whose carrier is an uninhabited abbrev is exactly the
adjudication this file declines to make. The owner (fundamentals owner /
Joe) decides.

## 3. Options for restoring the cap, with the argument against each

- **Re-read the fundamental against current mathlib4.** For: the entry's
  `:read` pins predate `PolicyRollout` entirely; a fresh read would see
  `predictedOutcome` and could re-word the finding (possibly to `:out`, or
  to a narrower gap). Against: it may CHANGE THE VERDICT — that is the
  owner's call, not a mechanical fix; and re-reading five more entries to
  keep the census coherent is a campaign, not a patch.
- **Add an explicit `:node` field to fundamentals.** Against, on the
  section's own stated discipline: the generator's rendered prose says
  "Placing those by hand is exactly the judgement this section is built not
  to make." A hand-placed `:node` makes the cap survive any future registry
  rebinding by removing the join entirely — the cap becomes an assertion
  rather than a derivation. This option is against the section's declared
  discipline and should not be taken lightly; if taken, the judgement being
  delegated must be recorded as such.
- **Widen the join (by module, or by namespace).** Against: it would
  wrongly capture. Module-level joining would cap every node whose row binds
  ANY declaration from a module an `:in` entry mentions — e.g. the
  `predictive-outcome-kernel-constructor` entry reads `GenerativeModel`,
  `generativeFactorMass` and `PredictiveOutcomeKernel`, all in Holes.lean,
  whose declarations are bound by many rows; the cap would spray across the
  roster on filename coincidence, not on constructor identity. The
  comment above `fundamental_decls` in the generator already records this
  hazard for a name-level variant ("would put 'Pi -- a machine policy
  carrier' onto R7, whose Pi is the precision map").
- **Re-anchor control 9h to whatever entry does join, if any.** Against:
  today NOTHING joins, so there is nothing to anchor to; 9h can only be
  re-anchored after the cap population is non-empty again by one of the
  routes above. Re-anchoring it now would pin a control to a vacuous
  refusal.

## 4. The pinned numbers in negative_controls.sh — pinned value, current
reading, cause

Ran the generator unmodified with `GRD_OUT` redirected
(`/tmp/grd-probe.tex`): `{:nodes 20, :rungs {'named': 17,
'formula-transcribed': 3}, :capped 0, :unjoined-fundamentals 6,
:assurance-censused 8, :contract 480a666ad2}`.

- **Rung distribution** — pinned `'named': 8, 'formula-transcribed': 12`
  (re-pinned 2026-09-13, contract ownership `ea5bb437e8`); now reads
  `17 / 3`. **Cause (two parts, both registry-side):** (a) the r53+
  acceptance wave rebound many `:lean` values as module-qualified names
  (`ObservationProcess.observationAfter`, `ExactBeliefTrajectory.exactBeliefAt`,
  `PolicyPrecision.policyPrecision`, `OutcomeRiskKL.outcomeRisk`, …), which
  match NEITHER the contract union's fully-qualified keys
  (`DarkTower.WarMachine.<Module>.<decl>`; 158 of 306 keys) NOR its bare
  aliases — probed directly via `wm_contract_union.load_union`: e.g.
  `policyPrecision`, `exactBeliefAt`, `observationAfter`, `outcomeRisk` are
  absent in BOTH forms, while `predictedOutcome`, `variationalFreeEnergy`,
  `ambiguity`, `exactUpdate`, `horizonEFE` exist bare and still reach
  `formula-transcribed`; (b) `a594a772`'s R4 rebinding is the one cap-relevant
  member of the same wave. The nodes did not become less ready — their
  bindings stopped resolving in the name join.
- **Cap population** — pinned `:capped 1`; now `:capped 0`. **Cause:**
  futon2 `a594a772` changed R4's `:lean` from `PredictiveOutcomeKernel` to
  `predictedOutcome`, removing the ONLY name intersection any `:in` entry
  ever had (§1). The rebinding was itself a correct registry improvement
  (`:carrier-only` → `:closed`, re-audited `:matches`); the cap vanished as
  an unreviewed side effect.
- **Node count** — pinned `:nodes 20`; still reads 20. Passes. (Listed
  because the survey named three numbers; this is the one that did not
  move.)
- **Controls 9h and the cap-refusal naming** fail for the same single cause
  as the cap population: with an empty cap set, a planted `:witnessed` rung
  on R4 is no longer refused ("a rung above a FUNDAMENTALS cap" / "above its
  cap" never fires because there is no cap to be above).

The file's own discipline — re-pin only "for a reason recorded in a commit" —
supplies the causes above; the re-pinning itself is deliberately NOT done
here.

## 5. Should the generator REFUSE `:capped 0` from six `:in` entries?

**Yes.** The generator already refuses an empty fundamentals census as "a
read failure, not a clean bill"; the same logic covers this case a fortiori:
a NON-empty `:in` census whose cap population is EMPTY is not a clean bill —
it says six uninhabited constructors exist and none of them touches any
node, which was false for this registry's whole history until an unreviewed
side effect of a rebinding made it true overnight. The two states are
indistinguishable from the generator's seat: "genuinely unjoined" and "the
name join silently rotted" both read `:capped 0`. A typed refusal when
`fund_in` is non-empty and `unjoined == all of fund_in` — with text naming
the entries and the join's name-matching rule — converts this from a
silent-clean-bill into a finding the owner must adjudicate, exactly as the
empty-census refusal already does. If the owner re-reads the fundamentals
and they genuinely do not join, the refusal is discharged by recording that
in a commit, the same way every other re-pin works.

## Probe index (this dispatch)

| Probe | Result |
|---|---|
| ENUM of all six `:in` entries + `:read` strings (bb over FUNDAMENTALS.edn) | table §1 |
| ENUM of all 21 aif-equations rows' `:node`/`:lean`/`:lean-status` (bb) | "none binds any entry name" |
| `git log -S ':lean "<name>"'` for all six entries' names | only `PredictiveOutcomeKernel` ever bound (`94493d77` → `a594a772`) |
| `GRD_OUT=/tmp/grd-probe.tex python3 empirics-futon/gen_rnode_dossiers.py` | 20 nodes, 17/3, capped 0, unjoined 6 |
| `wm_contract_union.load_union(...)` membership probes | §4 rung-distribution cause |
| Holes.lean :7024/:7053/:7060, PolicyRollout.lean :112-124 reads | §2 quotes |
