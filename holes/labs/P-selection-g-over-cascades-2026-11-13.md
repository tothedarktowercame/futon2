SPEC CHANGE PROPOSAL: selection-run G becomes the canonical cascade G (`GOverCascades.cascadeGrainG`); the pick-only `R20Step.outerModel` leaves the selection path.

# Selection G over cascades (2026-11-13)

Author: Claude (session 0a6558e5, coordinating the Lucy WM ladder after codex-6).
For: Joe's assessment. Nothing below is built yet.

## 1. Finding

Joe (2026-11-13): "it is G over policies, so a different cascade (including a
different pattern) should have a different G even if it chooses the same
ticket" — and, on the evidence, "both the code and the Lean model are dead
wrong". This is the recurrence recorded in futon2 `holes/M-G-over-cascades.md`
§2 and in the memory note on the single-action attractor.

Evidence (Lucy sandbox, futon2 `2d5b7da79`, mathlib4 as pinned there):

- `src/futon2/aif/outer_select.clj:427` scores each cascade as
  `(assoc score-input :cascade [] :field [candidate])`: the cascade is applied
  once to find its pick and then dropped. Risk, ambiguity and novelty come from
  the pick's model.
- `DarkTower/WarMachine/R20Step.lean:423` `outerModel` takes the cascade but
  uses it only through `cascadePick`; its docstring: the kernels "read only
  whole-task facts: readiness status, unblocks, reach, and target relation".
- Consequence on the ladder field 08/09/10 (loop runs 2 and 3,
  wm-click-ab37afb9, -9790344a, -2612ffd1, -9c9892c4): cascades picking 08 and
  09 merge into one prediction class; Q2's floor of two compared policies is
  not met; selection abstains `:insufficient-comparison` with a class at G 4.55
  against stopping at 14.97.
- The canonical definition exists and is unused here:
  `GOverCascades.cascadeGrainG = CascadeCoApplyGNovelty.scoreCascadeCoN`
  (da Costa et al. 2020 eq. 42: risk + ambiguity over one horizon's epochs,
  minus the expected novelty of the patterns that can fire; transition =
  co-application over the containment order). Clojure
  `cascade_coapply_g.clj` implements it; its callers are the work and repair
  cascades, not `outer_select` / `outer_score`.
- Why selection never reached it: the canonical candidate needs an
  `InterpretedPattern` (consumes / produces / forbids / θ) per pattern. The
  generic text interpreter (`cascade_model_manifest/interpret-pattern`) refuses
  the task-selection prose (it rejects "or", "not", etc.), and no explicit
  interpretation was ever written. Selection had nothing to feed the real G.

## 2. Change

### 2.1 Interpretations of the executable task-selection patterns

Tokens are facts about the candidate a cascade selects. Consumed and forbidden
tokens are read from each pattern's `@guard` and IF/HOWEVER; produced tokens
are the pattern's `@predicts`, stated as an established fact. θ = 1 is the
documented default until counts exist (see §4).

| pattern | consumes | forbids | produces (from `@predicts`) |
|---|---|---|---|
| only-what-can-start-now | `ready` | `unmet-need` | `can-begin` ("every selected task can begin when it is dispatched") |
| what-others-wait-on-first | `ready`, `unblocks` | — | `ready-count-rises` ("the number of ready items rises by the predicted count") |
| prepare-the-ground | `cone`, `named-later-item` | — | `later-item-reshaped` ("the later item … uses or is reshaped by this work") |
| find-out-before-building | `undetermined` | — | `design-decided` ("the answer changes or confirms the build's design") |
| serves-a-way | `ready`, `placed-work` | `non-work-placement` | `reason-names-way` ("the selected task's reason names the way it serves") |

Initial tokens come from the selected candidate's whole-task facts, the same
facts the executable steps already read: `ready` / `undetermined` (status),
`unblocks` (≥ 1), `cone` (≥ 1), `placed-work` (A1 placement with a way and
action work), `unmet-need` (Giraffe prerequisite unmet). `named-later-item` has
no fact source today, so prepare-the-ground's guard cannot hold yet; that is a
recorded gap, not a default.

### 2.2 Observation

`Candidate.observation : Finset V → EndingClass`. Proposal: the ending is the
selected task's relation class to focus (`R15StrategicTarget.outcomeClass`, as
today) once the selection has established that the task can begin
(`can-begin ∈ s`); before that it is `notYetEvaluated`; with no selected task it
is `stopTheLine`. So endings keep their present meaning, and the cascade enters
G through its trajectory: which tokens its patterns establish, at which epoch
of the common horizon, and how surely (θ, novelty). Two cascades with the same
pick but different patterns then have different G, which is what Joe's ruling
requires; a cascade that predicts more (e.g. serves-a-way also establishing
`reason-names-way`) is not rewarded for length alone, because only the
observation, not the count of tokens, carries preference.

Open (for Joe): whether a pattern's own `@predicts` token should also be
observable, i.e. carry preference, rather than only shaping when the ending
resolves. If yes, the observation needs a want signature for selection (the
work run's analogue is the mission's outstanding clauses). §2.2 as written
does not add one.

### 2.3 Lean

- `R20Step`: selection-run G := `cascadeGrainG` over the candidate of §2.1-2.2;
  `outerModel` is kept only as the record of the retired pick-only form.
- `Requirements`: a new check that fails while selection G is composition-blind.
  Stated on `RunFacts`: for two compared selection policies with the same pick
  and different pattern sets, their recorded G differ unless their trajectories
  are proved equal (a witness, not a tolerance). The present code fails it on
  the ladder field.

### 2.4 Clojure (after Lean is agreed)

`outer_select`: build the §2.1 candidate per cascade and score it with
`cascade-coapply-g`; prediction classes then form from the cascade's predicted
trajectory, not the pick's facts. First test: field 08/09/10 with the four
menu cascades must give two classes, each with a G.

## 3. What this does not change

The executable steps (which ticket a cascade picks), the requirements' floor
of two compared policies, the ending classes and their preferences, and the
focus/relation producers.

## 4. Gaps recorded, not filled

- θ and novelty counts for task-selection patterns: none recorded. Checked in
  Lean: `CascadeCoApplyGNovelty.CandidateN` requires `counts_pos` (every pattern's
  Beta pair strictly positive), so there is no zero-novelty case. Proposal: the
  explicit uniform prior Beta(1, 1) per pattern, recorded as a documented prior
  (not inferred), and θ = 1 by documented default. Counts stay fixed: Joe,
  2026-11-13, "We don't need it to self-improve". Novelty then still depends on
  which patterns can fire, so it is composition-sensitive as well.
- `named-later-item` (prepare-the-ground) has no fact source.
