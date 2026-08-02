# Featuregrid — maturity across the AIF systems, and which way each lesson flows

**Opened:** 2026-08-02. **Owner:** claude-4. **Operator:** Joe.
**Parent:** `M-aif-stack.md`. **Sibling:** `E-aif-ants-epistemics.md`.

Joe: *"let's develop a featuregrid somewhere that can explain how mature the
different systems are and how they can learn from each other. (E.g., we're making
a model 'of Zaif' but I'm not sure right now what the corresponding thing is for
a Zaif sensorium.)"*

## Evidential status — read this before trusting a cell

Cells are marked with how they are known, because a grid of confident-looking
guesses is the exact failure this programme exists to catch:

- **M** — measured or proved this week (hard)
- **R** — established by reading the source (hard)
- **I** — inferred from architecture, not verified (soft — treat as a hypothesis)
- **?** — genuinely unknown

## Maturity ladder

The levels came out of this week's findings; **inert** is the one we had to
discover we needed.

| level | meaning |
|---|---|
| `absent` | not present |
| `half-built` | present but with no consumer, or a consumer with no producer |
| `inert` | connected, computed, canonically named, and **causally dead** |
| `live` | demonstrably changes behaviour |
| `measured` | live, and its contribution has been quantified |

## The grid

| feature | ants | Zaif runners | War Machine | AIF² | memory retrieval (V3) |
|---|---|---|---|---|---|
| **sensorium** | 14 channels, but neighbourhood ones are **means** — directional info aggregated away `M` | see mapping below `I` | mission/pattern state `I` | claims + attacks `I` | query + candidate set, **v1.2 receipts live** `R` |
| **action palette** | 4 macro-actions; movement resolved downstream by the engine `R` | **actions minted as needed** — the most mature palette in the stack `I` | recommend-a-mission `I` | assert / attack / concede `I` | query ladder: 3-term → pairs → singles `R` |
| **generative model** | real forward model (`forward-predict`) predicting next observations `R` | **none evident** — no model of "what the repo looks like after this edit" `I` | ? | *candidate*: the argument corpus itself `I` | ? |
| **policy selection** | argmax over scalar EFE; **risk owns 27.54 of a 28.10 margin** `M` | ? `?` | central × strategic × doable, then rank `R` | attack survival — **does not scalarise** `I` | rank + top-k `R` |
| **epistemic term** | **inert** — ambiguity spread exactly 0; directed-EIG max 3.3% of margin `M` | ? `?` | absent `I` | attack-driven by construction `I` | breadth escalation, hand-coded schedule `R` |
| **preferences (C)** | C-vectors per mode `R` | **not represented** — envelope is an external hazard, not a preference `M` | mission value `R` | ? | ? |
| **witness** | external: yield, starvation `M` | job outcome `I` | **R16 witness does not feed belief** `R` | ? | use/ignore receipts `R` |
| **conatus / survival** | risk term — measured: sparse starvation 0.133 vs 0.633 without `M` | **absent** — died 3× to an unmodelled 30-min cap `M` | ? | ? | ? |
| **learning** | none within a run `R` | ? | reliability posteriors `I` | ? | that is the whole question `R` |

## Joe's question: what is a Zaif runner's sensorium?

The ant's 14 channels map over with unexpected completeness. This is the part of
the grid most worth arguing with.

| ant channel | runner correspondent |
|---|---|
| `:food` local food density | closable work at hand — a task with a clear next step |
| `:pher` local pheromone | traces left by prior agents *here* — commits, notes, prior attempts |
| `:food-trace` **neighbour mean** | queue depth — *"there is work nearby"* **without which work** |
| `:pher-trace` neighbour mean | how much prior activity in this area, aggregated |
| `:home-prox` | proximity to a committable state |
| `:enemy-prox` | a conflicting change, a held lock, an opposing failing test |
| `:h` hunger | **context / budget / deadline remaining** — the conatus channel |
| `:ingest` | recent rate of progress |
| `:trail-grad` | is this area getting more or less attention |
| `:novelty` | have I been here already this session |
| `:cargo` | **uncommitted work in progress** |
| `:reserve-home` | shared quota remaining |

**The mapping earns its keep immediately**: `:food-trace` is a *mean*, which is
what makes the ants' candidates indistinguishable. The runner correspondent —
*"there is work available"* without *which work* — is the same defect one layer
up, and worth checking rather than assuming.

## The axis that reorganises the grid: where do preferences come from?

Joe, 2026-08-02: *"a Zaif runner's sensorium includes my input, or input from
whatever agent is driving them, not just the state of the system itself. WM is
closer to 'just perceive the state of the system and act' because its REPL is
more or less a closed loop."*

This is not a detail about input channels. It changes what kind of agent each
system is.

| | preferences `C` | sensorium | consequence |
|---|---|---|---|
| **ants** | **internal, fixed** — C-vectors per mode, compiled in | world state only | closed loop |
| **War Machine** | **internal** — central × strategic × doable | system state only | closed loop |
| **Zaif runners** | **exogenous and time-varying** — arrive in the brief | world state **+ the driver's instructions** | open loop |

### The runner is not a standard AIF agent, and that may be the finding

In the canonical formulation `C` is part of the generative model: the agent has
preferred outcomes and acts to realise them. **A runner receives its preferences
as an observation, per job, from an agent it cannot fully observe.**

So the runner carries an inference problem the ants and the War Machine do not
have: *infer what is actually wanted from what was actually said.* That is not a
refinement of AIF's machinery, it is a component AIF does not have a slot for —
and it may be why the framework fits the WM more comfortably than it fits the
runners.

It is worth checking against the contract (R1–R12) whether anything constrains
the **provenance** of `C`. My expectation is that it does not, because the
formalism assumes `C` is given.

### A concrete instance from today

My R-a brief asserted that `forward-predict` produced action-dependent variance.
It does not. codex-9 read the source, found the stated preference inconsistent
with the world, **refused the job and said why.**

That is exactly a runner doing `C`-inference correctly: the instruction was the
observation, the observation was wrong, and the agent inferred the real goal well
enough to know the stated one could not serve it. No closed-loop agent faces that
situation, because no closed-loop agent's preferences can be mistaken.

### This reassigns the toy relationship

The ants are a toy of the **runner's embodiment** — act in a world, gather,
survive, leave traces — but of the **War Machine's closure** — fixed internal
preferences, system-state-only sensorium. The analogy is partial and the grid
should say where it holds.

**Design implication, and it is cheap:** an ant that receives a *task* from a
driver each episode — *gather from the north patch*, *defend the trail* — would
be an open-loop ant and a much better toy of a runner. That is a third source of
irreducible uncertainty after adversaries and self-generated structure, and the
only one that already exists in the real system today.

## Which way each lesson flows

This is the point of the grid.

**Runner → ant.**
- *Minted actions.* Runners gain actions as needed; ants have four, fixed. This
  is the palette gap and it is why the ants cannot argue about anything.
- *Caching.* A runner already leaves partial work for another to pick up —
  commits, notes, parked state. The ant cannot drop food except at home. The
  runner has the mechanic the ant needs, which is the next slice.

**Ant → runner.**
- *A generative model.* The ant predicts its next observation before acting.
  No runner does. That is the single largest gap in the grid.
- *Preferences as a first-class object.* Ants have C-vectors; runners have their
  survival envelope as an external hazard, and died three times to it today.

**AIF² → ants (and everything).**
- *Non-scalarised selection.* Risk owns the ants' margin because everything is
  summed into one `G`. An attack cannot be outweighed by inflating an unrelated
  number. This is the constructive answer to the week's central measurement.

**Ants → AIF².**
- *A cheap place to try it.* An ant whose selection is an argument produces
  inspectable reasons at ant scale — which is L1's explanation requirement,
  developed somewhere that costs seconds.

## The discipline this grid exists to enforce

The ants are a **toy model of the runners**, not a separate system. Whatever the
runner layer gains, the ant should gain a toy of, or the toy stops predicting
anything. This week inverted that — an expensive audit of a toy whose diagnosis
was already known — and the grid is the artifact that should make the inversion
visible next time.

**Biggest open cells, in priority order:** the runner's policy selection (`?`),
the runner's epistemic term (`?`), and whether any runner has a generative model
at all. All three are reads, not experiments.
