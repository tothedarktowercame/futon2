# Give the outer target score its novelty term, so unobserved targets stop tying

**Status:** OPEN
Parent: M-aif-policy-conditioned-eig

## Observed failure

Two of the five 2026-10-18 readiness clicks (1 and 5) stopped or continued on
an exact tie in task-state G between two missions
(`TN-war-machine-readiness-oct-18-2026.md`; review
`REVIEW-wm-readiness-2026-10-18b.md`). Joe's guidance: a tie at selection is
first a symptom of incomplete ingest, and any tie-breaker must be G-based, not
a coin flip.

Reading `src/futon2/aif/outer_target_score.clj` at `b04e2df1c8db`:

- a target with no observations of its own takes the prior `alpha`
  (`(or (get-in belief [:targets target :alpha]) (get-in belief [:prior :alpha]))`),
  so every unobserved target has the same prediction, risk, ambiguity and
  canonical G; the click-5 checkpoint records about 510 `:target-unobserved`
  entries;
- epistemic value is fixed: `eig 0.0` ("no likelihood-parameter posterior
  beyond the categorical Dirichlet is claimed here").

## Scoped task

Compute the expected information gain about each target's Dirichlet
parameters (the novelty term of expected free energy) from its `alpha`, and
subtract it in `canonical-g` in place of the constant `0.0`. A target with
fewer observations then has larger novelty and lower G, so unobserved targets
differ by how much selecting them would teach. The term must be the expected information gain about the Dirichlet
parameters, not posterior spread under another name (R17′ of the PLoP
catalogue: "reserve the words information gain for a calculation that
actually earns them"; the model-uncertainty bonus in `efe.clj` is the
honestly named spread heuristic and is not this term). Record the term under `:terms`
with its kind, as risk and ambiguity are.

Out of scope: the resource/cadence/continuation terms and the [0, 1] clamp
(see `T-outer-target-g-policy-prior.md`).

## Acceptance evidence

- A regression with two targets that share the prior and differ only in
  observation count: before the change their G is equal; after it the less
  observed target has the lower G, by the computed novelty.
- A control with two targets of identical `alpha`: G stays equal (a genuine
  tie remains a tie and is still refused as `:no-unique-task-state-minimum`).
- The Dirichlet novelty formula checked against a closed-form case.
- The outer selector's `:absent-terms` diagnostic still reports what is
  missing.

## Prior design to weigh (July, efe route)

The same failure occurred on 2026-07-19/21 on the efe route (fourteen
never-observed candidates at bit-identical scores; the discrimination gate
refused). The fix then was "Native-Currency Discrimination" (PLoP conference
catalogue): structural zone load enters the preferences C
(`preferences.clj` `c-zone-load-key`, `efe.clj` `:capability-zone-load`) and
per-target predicted-outcome uncertainty replaces a flat ambiguity, so the
core discriminates without a new term. It is compatible with the novelty term;
the outer-objective design should choose between or combine them deliberately.

## Specification note (added 2026-10-18, before any work)

The Lean specification does not currently require this. In
`mathlib4/DarkTower/WarMachine/OuterTaskObjective.lean` (e02dd206d3) the outer
objective "is not cascade-policy G": its channels and aggregation are
caller-declared, and `selected_is_minimum` makes a valid selection "the
deterministic minimum under declared objective value, with stable task
identity as the tie law". Q8's `SelectionCertificate.Valid` accepts a tied
minimum, and `selectionDiscriminates` (Holes.lean) is a verdict outside
`Requirements.conforms`. So this ticket asks for a change the specification
does not demand and partly contradicts (the tie law). Joe's guidance of
2026-10-18 (ties signal incomplete ingest; any tie-breaker must be G-based)
and the revision policy (the code should be an accurate model of AIF) point
the other way. Which governs is Joe's decision; do not start until it is made.

Joe, later on 2026-10-18: the outer loop is supposed to be G over policies,
where policies are cascades drawn from Cascade Live and parameterised by META
items. He was not sure about "G with novelty" as such; note that the inner
cascade G already counts parameter novelty as its information-gain term
(`NOTE-g-over-head-cascades-2026-09-30.md`, `EpistemicValue.lean`), which is
the law this ticket would carry to the outer level. Ties failing is confirmed
for Lean (see `T-q2-q4-q8-recomputable-on-every-run.md`).

## Provenance

Found by claude-4 while annotating R5 (Expected Free Energy Inside an
Auditable Controller) for the plop2026 docbook
(`p4ng/plop-2026-code-annotations.json`), 2026-10-18. Written as a ticket at
Joe's request so the War Machine may select it.
