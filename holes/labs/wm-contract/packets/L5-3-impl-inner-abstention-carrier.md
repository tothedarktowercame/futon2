# L5-3-impl — Producer and projector: the stopped policy under each inner policy's own model, retained and projected (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `3aab49c91`; your L5-3 report (`reconciliation/L5-3-inner-abstention-codex11-2026-11-01.md`,
`d98efdfee`) is the map: shape A, 25 of 25 chosen beat stopping, 0 prediction coincidences, the
`[0 true]` vacuity from an empty want. Lean Q11 at mathlib4 `221f060818` is unchanged by this packet.

## Decisions (claude-2, by reference)

- **Shape A now; B later with the restated kit.** Comparing "act" against "stop" under the acting
  policy's own model is the comparison the decision rests on; a shared terminal preference changes
  every G and waits for the factorized kit's numbers, which are Joe's.
- **The reference rule for abstaining on a task.** Lean's `abstainOnTask` needs one `abstentionG`
  strictly below every acting policy's G. With per-policy lenses the honest single reference is the
  stopped policy scored under the *admissible minimum* policy's model: if even the best acting
  policy's own lens prefers stopping, the task stops. Record it as `:chosen-reference` = the
  admissible minimum whether or not it is enacted, and name the rule `:stop-under-the-minimum`.
  Comparing that one G to the other policies' Gs crosses lenses; record that as a known
  approximation in the carrier (`:cross-lens true` on the comparison), never silently. Do not pick
  the most convenient stop score.
- **No vacuous want.** The stopped policy retains the original nonempty want, model, preferences,
  horizon and initial belief; the empty-want construction is the negative control and must refuse
  in the producer (`:empty-want-stop`).

## Behaviour

1. `wm/family_selection.clj` (`score-policy` invocation, `scored->ranked`) scores the stopped
   policy under each candidate's own model beside the candidate: `{:G :prediction-digest :terms
   :reference-model-digests :horizon :normalizations :no-action-rule-version}`, using the prototype's
   function moved into `src/` with its version.
2. `wm/cascade_decision.clj` (SC assembly) attaches `SC :abstention {:per-policy {...}
   :chosen-reference <enacted policy id> :rule :stop-under-the-minimum :cross-lens <bool>}`; after an
   `abstainOnTask` decision the reference is the admissible minimum.
3. The selector (L5-2C-a law): the enacted class's representative must strictly beat its own stop
   (`pick-not-below-abstention` refusal, as the outer path has); if no class beats its own stop, the
   decision is `abstainOnTask` with the reference above.
4. `decision_gate.clj` verifies the join (reference id ∈ candidates; terms sum to G; digest matches
   the retained prediction) before admitting a certificate.
5. Projector (`wm_run_facts.clj:661`): `abstentionG` and `abstentionPrediction` from the
   chosen-reference entry at scale 10^6 with audit fields; typed absence on records without the
   carrier (historical records stay unverifiable on Q11).

## Tests

- Live pins on the 25 scored certificates' retained models (your prototype table): the carrier's
  per-policy G equals the prototype's to 1e-9 on three named runs.
- Empty-want construction refuses by name.
- A fixture where no policy beats its own stop → `abstainOnTask` with the reference = admissible
  minimum and `:cross-lens true`.
- Hand-built exports through the checker and Lean: 376d76ff with the new law's chosen policy and
  the real stop reference → paste the nine outcomes (expect Q11 holds); ac5bc477 likewise.
- Named, inner-law, projector namespaces vs `main`; hermetic root.

## Gates

kondo, parens, no reload, no merge. Branch `codex11/l5-3-impl-inner-abstention`. Bell claude-2 with
sha, the two nine-outcome lists, counts vs main.
