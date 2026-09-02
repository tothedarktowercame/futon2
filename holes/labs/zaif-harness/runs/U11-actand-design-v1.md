# U11 design draft — the actand source, one forward-model family at two grains

Author: claude-2 (zaif-harness lane), 2026-09-02 ~18:25Z, per the joint-pass
agreement (claude-2 leads, claude-1 reviews with wm-side constraints; bellback
on invoke-1788372741639). **Rev 2, ~18:35Z**: claude-1's review
(invoke-1788373305219) applied in full — scalar-bridge framing (§4), declared
clamp + typed ambiguity in the adapter record (§2), U12 node-fixtures as the
reading-map source (§2), discrimination trio (§3). Status: REVIEWED design;
nothing here is built, flipped, or ruled. Inputs read: D8a (c8eec02),
DESIGN-c-vector.md §3/§5, wm-contract U12's measured result, and both
candidate computations in source.

## 0. What this designs

One object, two consumers. zaif's `:task-belief` hole
(`:d8/task-belief-actand-source-absent`, D8a) and the WM's Q(o_k|π) hole
(DESIGN-c-vector.md §3, "the honest hole, named not papered") are the same
missing thing at two grains. The design refuses to build it twice:

    Q_actand : Grain → Action → Density over declared observables

with `Grain ∈ {:arm-session, :mission}` for now. Every emitted prediction
carries its grain and its source, so it can always say **whose prediction it
is and at which grain** — the two-C lesson applied one node over (claude-1's
constraint 1, adopted as the design's spine).

## 1. The three arms, as they actually are in code

Honesty first: **neither runnable arm is a density.** Both are scalar
expected-progress heuristics with hand-set weights. The design treats them as
degenerate Q's (point predictions of an improvement observable) and says so in
their provenance, rather than dressing them as distributions.

- **Arm A `:portfolio-policy`** — `futon3c/src/futon3c/portfolio/policy.clj:49-74`.
  `pragmatic-value(action, observation, mu-sens, adjacent-missions)`: channel
  errors (gap-count, stall-count, review-age, spinoff-pressure, coverage-pct)
  between observation and belief, weighted per action
  (`:work-on :review :consolidate :upvote :acquire-patterns`). Action-conditioned,
  channel-fed, belief-relative — the closest existing thing to "how much does
  acting advance the task, given what we believe vs see."
- **Arm B `:mission-head-channels`** — `futon3c/src/futon3c/aif/mission_head.clj:135-176`.
  `pragmatic-value(action, channels)`: phase-progress, prediction-divergence,
  gate-readiness, obligation-satisfaction per action
  (`:advance-phase :revise-approach …`). Mission-shaped channels, no belief
  term (channels are taken as read).
- **Arm C `:cascade-catalog-playout`** — wm-side, S6 §3: precedent as the
  playout record; predicted outcomes drawn from typed records of kin missions'
  actual courses. **Not built.** Named here so the registry records all three
  (claude-1's constraint 2). It is the only arm whose output is naturally a
  density and whose provenance is naturally a citation ("prediction from
  precedent M-x's record"); when S6(b) exists, it enters the same comparison
  harness below unchanged.

D8a's warning is a design constraint, not a footnote: *"copying either action
vocabulary into the four-arm controller would be a new model, not hydration."*
So the arms stay where they live; what crosses the seam is an **adapter
record**, not their vocabulary.

## 2. The adapter into `:task-belief` (consumes D8b's seam)

The zaif `:act` arm needs E[task progress | act-now] for the current session.
Mapping declared per arm, in data: zaif `:act` ↦ Arm A `:work-on` /
Arm B `:advance-phase` (the "do the work this turn" action in each
vocabulary). The adapter evaluates the arm against the session's actual
channel readings and emits exactly what D8b's acceptance seam propagates:

    {:act-value  <double>            ; the arm's scalar
     :ambiguity  :not-modeled-degenerate-q  ; TYPED, never numeric 0.0 -- a
                                            ; degenerate Q has zero entropy, so
                                            ; the ambiguity half of G is
                                            ; structurally absent for arms A/B;
                                            ; a numeric 0.0 would rebuild the
                                            ; U4 problem (a term always zero
                                            ; with no way to tell if meaningful)
     :source     {:query   :q-actand/v1
                  :arm     :portfolio-policy | :mission-head-channels | :cascade-catalog-playout
                  :grain   :arm-session
                  :transform :clamp-0-1     ; the clamp is a DECLARED transform;
                                            ; silent, it would turn section 6's
                                            ; "affine image" into "clamped
                                            ; affine image" unannounced
                  :inputs-digest <sha of the channel readings used>
                  :at      <instant>}}

    Digest stability is an ASSERTION, not just provenance: same
    :inputs-digest => same :act-value, replay-checked.

No source ⇒ D8b's typed absence stands. Numeric without provenance ⇒ D8b's
refusal (`:d8/unprovenanced-task-belief`). A predicted outcome with no typed
source is a refusal, not a prior (claude-1's constraint 3 — already enforced
by the seam; the design just declines to weaken it).

Open input the adapter needs and must not invent: **which channel readings
exist at zaif session grain.** Arm A wants gap/stall/review channels; a zai
session has posting-stats, turn-round records, and (post-D10) a clocked
mission whose registry/C_mis fields carry obligation-shaped readings. The
design pass deliberately leaves the reading-map as the first build question,
because it is answerable from records, not from taste. Concretely (claude-1
review): answer it against `wm-contract/runs/U12-c-mis-falsifier/node-fixtures/`
— 40 per-node fixtures (run-id × node, typed absences included, R7 located as
`:precision-state` covering 8/14 channels) — plus U10's matrix; do not
re-extract the tick records.

## 3. The comparison run — designed into the row (Joe's 2026-09-01 rule)

Both zaif arms are runnable over recorded data, so no advance ruling: build
both behind declared inputs, replay, decide on numbers, Joe sees results and
can veto. One replay harness, three measurements:

1. **Discrimination** (headline): count of distinct `:act-value`s and their
   spread over the replayed corpus. Baselines to beat, named
   (claude-1's constraint 1, U12's negative result): wm status-quo forward
   model = **exactly 1 distinct risk value (4.5399e-5) over 133 actions
   across all 14 channels**; zaif shipped = **1 distinct act value (0.0) on
   114/114 sessions**. Distinct-count is necessary, not sufficient — a
   continuous-channel arm produces distinct values from noise alone. So the
   headline is a trio (claude-1 review): (a) beats both constant baselines;
   (b) a DIRECTION probe in U12's style — two planted sessions differing
   only in one gap channel (maximal vs minimal), the arm must order them
   correctly; (c) digest stability — same `:inputs-digest` ⇒ same value, as
   an assertion. "Beats constant + ordered plants + digest-stable" is a
   discrimination claim noise cannot fake.
2. **D9 re-run per arm**: tie-settled vs score-settled counts over the same
   recorded corpus (D9's harness, already specified on its row).
3. **Provenance completeness**: every emitted value resolves to its
   `:inputs-digest`; any row that doesn't is a harness bug, counted as such.

Corpus: the 114 calibration sessions (`calibration-sessions.edn`) + the 56
live decisions for `:arm-session` grain; the three tick records (S7 seed
corpus) for `:mission` grain. All replay, no live flips; default off,
flip J-gated.

## 4. Decisions in DERIVE form

- IF the WM needs Q at mission grain (§3) and zaif needs it at arm grain,
  HOWEVER two forward models built tonight would each be unable to say whose
  prediction it is, THEN one grain-indexed family with per-grain
  instantiations and grain in every record's provenance, BECAUSE the two-C
  design already paid for this lesson and wrote it down (§5's grain-indexed
  family is the template).
- IF the theory does not pick between arms A, B and (eventually) C, HOWEVER
  A and B are runnable over recorded data tonight, THEN a registry `:choices`
  entry names all three arms and the comparison replay decides on
  discrimination + D9 numbers, BECAUSE runnable arms get built and run, not
  ruled on (Joe 2026-09-01; the shared checker enforces it).
- IF the arms are scalar heuristics, HOWEVER the family's type says Density,
  THEN v1 records them as declared point-predictions (degenerate densities)
  with `:arm` and `:transform` in provenance, BECAUSE for a point-prediction
  Q, KL(Q‖C) collapses to −log C(o*) — which is DESIGN-c-vector.md §6's
  scalar bridge verbatim ("a scalar payoff is an affine image of log-C at
  the outcome the action targets"). This is not a weakening of the §5 type
  but its instantiation, and it gives the tally's `:c-cost-vs-distribution`
  row (closed `:partial` because no source yet carries
  `:scalar-awaiting-density`) its first genuine repair path: these scalars
  become typed point-predictions whose log-C evaluation IS the payoff.
  RIDER, inherited by name: the moment any arm's output meets a C in one G,
  wm-contract U17's ≥0 property applies at this seam (−log C can go
  negative in-band against a range C — U12 clause (c)'s negative-term
  problem); the composition must not happen unguarded. (Both points:
  claude-1 review, invoke-1788373305219.)
- IF a session lacks the channel readings an arm wants, HOWEVER inventing
  readings would smuggle a model in as data, THEN the adapter emits the D8b
  typed absence for that session and the replay counts coverage, BECAUSE
  absence-counting is itself one of the comparison's results (an arm that
  only fires on 10% of sessions loses to one that fires honestly on 80%).

## 5. Registry `:choices` entry (draft, to be ported on landing)

    {:choice :q-actand-arm
     :ruling "Joe 2026-09-01 choice-point discipline; joint-pass claude-2/claude-1 2026-09-02"
     :arms [{:arm :portfolio-policy      :status :runnable :source "futon3c portfolio/policy.clj:49-74"}
            {:arm :mission-head-channels :status :runnable :source "futon3c aif/mission_head.clj:135-176"}
            {:arm :cascade-catalog-playout :status :unbuilt :source "wm-contract S6 §3 (precedent as playout record)"}]
     :measurement "discrimination (distinct act-values; baselines: wm-U12 1/133, zaif 1/114) + D9 tie counts + coverage; replay-only"
     :decided :not-yet}

## 6. What this draft does NOT do

No code, no flip, no arm choice, no new channel semantics, no claim that any
arm's numbers mean progress until the replay says they discriminate. Next
acts after claude-1's review: (a) the reading-map question to U10/S7 output;
(b) one build packet per arm adapter (small, separate); (c) the replay
harness packet; (d) numbers to Joe.
