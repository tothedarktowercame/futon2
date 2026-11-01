# L5-4L-3 — Lean: task-level exclusion reasons; grounding agreement at the class grain; the outer factorized certificate (claude-2 → codex-11, 2026-11-01)

Base: mathlib4 darktower `d3e0d9724f`. Three small additions in one Lean packet, each with its
fixtures and theorems; `Requirements.lean`, `SelectionGrounding.lean` and a new
`OuterFactorizedPreference.lean` (or a section of `FactorizedPreferenceSemantics.lean` if it fits).
Authority: Joe's ruling 4 (record `RULING-inner-selection-enacted-object-2026-11-01.md` §Ruling 4),
`DECISIONS-by-reference-claude2-2026-11-01.md` §5, and the producers now merged in futon2
(L5-4b closed reason set in `outer_observation.clj` `task-exclusion-reasons`; L5-4d-5 classes).

## A. Closed set of exclusion reasons (SelectionGrounding.lean, G1)

`FieldEntry.excluded task reason pin` carries an opaque `reason : Id`. Make it a typed reason:
```lean
inductive ExclusionReason where
  | unreadNew | targetSourcePathAbsent | targetSourceConflict | targetItemLineAbsent | readingSourceChanged
  deriving DecidableEq, Repr
```
(the five in futon2's `task-exclusion-reasons`; nothing policy-derived, nothing that is a registry
status). G1 then needs no "reason ≠ 0"; a projector that meets an unknown reason has no constructor
to render and Lean refuses to elaborate. Fixtures: the small positive with `.unreadNew`; a docstring
stating the invariant: field facts and exclusion reasons are facts about tasks, never about policies
for doing them, and naming `all-policies-already-satisfied` as the excluded example. Update the
witness emitter's expectation in a comment only (the emitter is futon2; I will regenerate the
fixture after you land this and bell me the constructor name for `giraffe/unread-new`).

## B. Grounding agreement at the class grain (SelectionGrounding.lean, G4)

Producers now compare classes (same prediction digest and G units = one policy). `GroundedPolicy`
gains `class : Id`; G4 compares the *set* of distinct `(class, g)` pairs of the grounding's policies
with `RunFacts.policyG` as finsets, requires that members of one class have equal `g` and equal
prediction (assert on the grounding: same class → same `pick` is *not* required, same `g` is), and
takes `g.chosen` as a class id matched against `RunFacts.currentDecision`'s cascade. Fixtures: the
small positive with two members in one class (two step orders, same pick, same G) and G4 holding
against a `policyG` of two rows; a negative where two members of one class carry different `g`.

## C. The outer factorized certificate (new, small)

The outer preference is a product over (ending, gain): ending ∈ {focused, related, unrelated,
stop-the-line, not-yet-evaluated}, gain ∈ {none, one, several} (`outer_score.clj`, kits
55/35/5/5/0 and 1/2/3). The Lean law in `FactorizedPreferenceSemantics.lean` is completion first,
relevance conditional on completion, unmeasured not a failure class. State the outer certificate in
that order:
- completion: the selected work ran to a terminal receipt (ending ∉ {stop-the-line}) vs stopped
  (stop-the-line) vs unmeasured (not-yet-evaluated);
- relevance, conditional on completion: focused > related > unrelated (ordinal law from the
  module's `RelevanceCalibration`);
- gain as a second conditional factor: several > one > none.
Define `OuterOutcome → FactorizedPreferenceSemantics.Outcome` (completion, relevance) and show the
current kit's order is *not* the law's: the kit ranks stop-the-line (5) above not-yet-evaluated (0)
and does not separate completion from relevance ordinally (a theorem exhibiting the two outcomes
the kit orders against the law). Then state the certificate the producer must emit:
`{calibration (relevance order with authority), selector authority, factorization identity/version,
completion-then-relevance law, gain as conditional}`, reusing the module's `Certificate.Valid`.
Do not choose kit numbers: the order is the law, the numbers are Joe's (record that in the docstring).

## Gates

Worktree `~/worktrees/mathlib4-l5-4l-3`, branch `codex11/l5-4l-3-exclusions-classes-certificate`
from `d3e0d9724f`; `.lake` by cache or real copy, never a symlink; build Requirements,
RequirementsReader, CloseOutcomeImage, SelectionGrounding, SelectionGroundingWitness and the new
module green (the witness fixture will need `.unreadNew` and a class id per policy: edit the
generated file by hand for this slice and say so; I regenerate it after); zero sorry; log + sha in
`~/notes/l5-4l-3/`. Do not merge. Bell claude-2 with sha, build log sha, theorem list, and the two
kit-versus-law outcomes.
