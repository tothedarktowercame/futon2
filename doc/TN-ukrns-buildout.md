# TN-ukrns-buildout

**Date:** 2026-05-05
**Status:** design sketch; build deferred until npt WP v8 lands publicly. No code change in futon2 yet.
**Cross-refs:**
- `npt/working-paper/UKRN_WP_draft_v8.md` §5 "Trajectories as cyber-ant decisions" — the existing landscape-only simulation
- `npt/working-paper/notebooks/ukrn_cyberant_aif.clj` — the AIF cyber-ant for the bootstrap-phase landscape
- `npt/working-paper/assumptions-v2.edn` — population-model parameters (10 institutional profiles, 16 NPT factors, 5 levers, 5 patterns)
- `futon2/doc/stack-baseline.md` — the AIF observe → perceive → affect → policy loop being reused
- `futon2/doc/aif-engine.md` — engine internals
- `futon2/web/war-machine/src/war_machine/client/hex.cljs` — pointy-top hex layout (already wired)
- `futon2/web/war-machine/src/war_machine/client/tick.cljs` — tick controller
- `futon2/web/war-machine/src/war_machine/client/aif_join.cljs` — AIF wiring on the client
- `https://www.ukrn.org/training-schema/` — the schema the nest is built from (~50 topics across seven research-stage categories)
- `futon3/library/ants/baseline-cyber-ant.flexiarg`, `white-space-scout.flexiarg`, `cargo-return-discipline.flexiarg` — pattern operators that the topic-cyber-ants would specialise

## Why this note exists

The npt working paper currently models *one* dynamical system: UK HEIs traversing a (D, A) phase space under decisions made by a single cyber-ant. The simulation in §5 covers the bootstrap-phase question — *what happens to ten institutional profiles when UKRN-S applies its five levers under various tension schedules*. It does not cover the question that comes next: *how does UKRN-S's training portfolio itself get built out, and how does that build-out interact with the landscape*?

This note sketches the second system — the schema build-out as a nest of topic-cyber-ants — and proposes coupling the two for a War Machine demo. It is an idea note for a Bristol-workshop or public-talk artefact, not a WP integration.

## The two coupled systems

### Landscape (the existing model)

Ten HEI profiles, each with 16 NPT factor scores, traversing (D, A) space under decisions made by a single cyber-ant. The cyber-ant's actions are tension positions on six axes; its priors are over preferred (D, A) outcomes; its diagnostic vocabulary is the WP's five institution-level patterns plus three programme-level patterns. Implemented in `notebooks/ukrn_cyberant_aif.clj`.

### Nest (the proposed extension)

The UKRN training schema as a hex tiling. Each schema topic is a tile. The schema's seven research-stage categories — Planning, Conducting, Analysing, Disseminating, Evaluating, Incentive Structure, Other — are the regions. Tile state runs on a small ladder: *unbuilt* → *scoped* → *operational*. UKRN-S as queen decides which tile to advance next.

A topic-cyber-ant inhabits each operational tile. It has:

- A *prior preference* specific to its topic (e.g., for the FAIR-data topic, the prior is high data-management-plan quality across disciplines).
- *Observation channels* relevant to its topic (per-discipline adoption rates, repository presence, existing community-of-practice signals).
- A subset of the WP's patterns active for its topic. Different topics activate different patterns: the foundational ECR-track topic activates Confident Adaptation; the FAIR-data topic activates Shared Maintenance; the Co-production topic (currently tagged "no ORP coverage") activates white-space-scout because it is genuinely new territory.
- An *action repertoire* in the topic's own register: commission a training session, partner with a repository, draft discipline-specific guidance, advocate for a funder mandate.

## Coupling

The two systems are not independent. They speak to each other in both directions.

### Nest → landscape

A topic that has not been built has no operational lever in the landscape model. The landscape's cyber-ant in §5 acts on whatever lever set the nest currently affords. Currently §5 assumes all five levers (publish-scaffold, delivery-spine, CoP-infrastructure, evaluation-integration, ORCA-support) are always active. In the coupled view, levers come *online* as topic-cyber-ants are instantiated. The bootstrap phase corresponds to the first ~5 tiles being lit; the rest of the schema is the post-bootstrap rollout.

### Landscape → nest

The landscape's binding-constraint signal tells the queen which topic to build next. A population in which several profiles have `relational-integ` binding tells the queen to invest in a CoP-infrastructure topic before, say, a publication-strategies topic. The selection rule is exactly the *Diagnose-before-prescribing* pattern from the WP, applied at the schema level: target binding constraints, not the loudest demand signal.

The result is a control loop: landscape signals what is binding; nest builds the lever; landscape moves under the new lever; new binding constraints emerge; nest builds the next lever. This is the *patterns-as-attractors* claim at one scale up — the patterns are not only attractors at the institution/system level, they are also the diagnostic the nest uses to decide what to build.

## The War Machine as visualisation substrate

The War Machine already has the right substrate:

- Pointy-top hex layout (`hex.cljs`) with `hex->pixel`, `hex-neighbours`, `hex-ring-coords`, and existing assignments laying out pattern languages as hex regions.
- A tick controller (`tick.cljs`) and AIF stack joiner (`aif_join.cljs`).
- Sprite-emitting renderer (`sprites.cljs`) and HUD (`hud.cljs`).

What would need to be added or extended:

- A *nest-region layout*: the seven research-stage categories as hex regions, with topic tiles within each. Re-uses the same idea as the existing pattern-language layout — same template, different data.
- A *landscape panel*, either as an outer ring around the nest or as a separate sub-panel. Ten HEI tiles, each colored by current mode (multiplied / absorbed / mismatch).
- *Pheromone-trail edges* between nest tiles and HEI tiles. When a nest topic comes online, the trail to the HEI(s) whose binding constraint that topic addresses is drawn. This makes the coupling visible.
- *Diachronic playback*. Joe noted this is weaker in the current War Machine than ideal. Two options:
  - *Step controls* (cheaper) — render state on each tick; user steps with arrow keys; trace text updates on each step. Probably suffices for live demo.
  - *Timeline scrubber* (richer) — store all tick states; user drags a slider; both panels update synchronously. Small but real addition.

## Implementation sketch

The work splits cleanly into a backend pass and a frontend pass, in that order.

### Backend (Clojure, in npt or futon2)

1. *Schema input*: the public schema scraped or transcribed into EDN — `topic-id`, `category`, `tags` (e.g., `:beyond-orp-scope`, `:priority`, `:no-orp-coverage`).
2. *Topic-cyber-ant template*: a function that, given a topic record, instantiates an AIF agent with topic-appropriate priors, observation channels, and active patterns. Specialises the existing patterns from `futon3/library/ants/` (white-space-scout, cargo-return-discipline, hunger-precision-coupling) per topic.
3. *Queen logic*: a meta-agent that picks which tile to advance next. Inputs: current landscape binding-constraint signal, current nest state, finite resource budget (training-manager FTE, presenter pool). Output: a tile to move from unbuilt → scoped or scoped → operational.
4. *Coupling layer*: when a tile becomes operational, the corresponding lever is enabled in the landscape model. When the landscape evaluates its tensions and observes binding constraints, those signals feed the queen.
5. *Tick controller*: ticks both systems forward together. Landscape ticks in (D, A) space; nest ticks in tile-state space. Trace records both.

### Frontend (CLJS, War Machine)

6. *Nest renderer*: lay out research-stage categories as hex regions; topic tiles colored by state (grey / amber / green).
7. *Landscape renderer*: HEI panel colored by mode.
8. *Coupling visuals*: pheromone trails between nest and landscape on lever-activation events.
9. *Step controls or scrubber*: as above.

## What the demo gives us

- Schema-coverage progression visible — the nest fills in tile by tile under the queen's decisions
- HEI population evolution visible — the landscape shifts mode under available levers
- The two coupled — the demo shows that bootstrap is a *first phase* of nest-build-out, not the whole story
- Patterns-as-attractors visible at two scales simultaneously — same diagnostic vocabulary, different substrate
- Sequencing claims made visible — the order in which topics come online matters; the demo lets viewers test rollout policies
- Demand-pull vs supply-push policies become testable — different queen logics produce different rollout shapes

## Open questions / deferred decisions

- *Schema subset for the initial demo*: 50 topics is a lot. A subset of ~12–15 covering all seven categories would be enough to show the pattern. Selection should include one or two from the *beyond-ORP-scope* set (Publication strategies, Publication copyright/licensing, Peer-review guidelines) since these are where white-space-scout dynamics matter most.
- *Granularity of tile-state*: three states (unbuilt / scoped / operational) is the simplest; richer might be needed if topics have sub-states. Defer until backend prototype reveals what tracking is useful.
- *Real survey data as landscape input*: the BORS 25 survey and the Open Research Training Programme Curriculum are candidate landscape signals — they tell the queen what HEIs are asking for. Incorporating them is a separate task, but the demo can use synthetic signals for the first cut.
- *Scrubber vs step controls*: depends on the audience. Step is fine for live demo; scrubber lets a self-paced viewer explore. Step first; scrubber if it earns its keep.
- *Where this lives*: Bristol workshop demo and a futon-track paper are both plausible. They are not mutually exclusive — the demo seeds the paper.

## What this note does not commit to

- It does not propose adding any of this to the WP. The WP keeps its single-system simulation in §5; the coupled-systems demo is a separate piece.
- It does not decide whether the queen logic should be the same AIF-cyber-ant template as the topic-cyber-ants (recursive — UKRN-S as a cyber-ant whose actions are tile-state updates) or a simpler heuristic policy. Both are plausible; the recursive option is more elegant but more work.
- It does not specify the data format for trace records. That belongs in the backend implementation pass.

## When to take this up

After WP v8 lands publicly. The WP makes the simpler one-system claim; the demo is the substantive next step. A reasonable scope for a first build is one to two weeks for the backend (Clojure, on the existing AIF stack) plus another one to two weeks for the War Machine front-end, given a CLJS contributor.
