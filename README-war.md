# War Biology Extensions (Ideas)

This note captures biology-inspired extensions to the Futon2 ant-war simulator.
No code changes are required; it is a design/idea seed for future work.

## Current Model (as of 2026-01-22)

- World state: food, pheromone, home cells, reserves, hunger; no explicit combat.
- “Enemy” is implicit via `:enemy-prox` (distance to the opposing home).
- AIF actions are forage/return/hold/pheromone; classic ants follow similar rules.
- Termination is starvation-driven (queen or all ants), not battlefield outcomes.

Relevant files:
- `futon2/src/ants/war.clj`
- `futon2/src/ants/aif/observe.clj`
- `futon2/src/ants/aif/perceive.clj`
- `futon2/src/ants/aif/policy.clj`

## Biology-Inspired Extensions (Idea Pool)

### 1) Territorial Borders + Ritualized Duels
- Add “border” cells (contested territory) that trigger low-lethality duels.
- Purpose: resolve territory control without mass casualties; battlefields recur.
- Model: cell-level `:control` scalar, duel resolves small shifts.

### 2) Caste Differentiation (Workers vs Soldiers)
- Add `:role` per ant (worker/soldier) with distinct stats/action sets.
- Soldiers: high combat, low foraging; workers: inverse.
- Allows two strategies: “few strong” vs “many weak.”

### 3) Battlefield Complexity (Terrain)
- Add per-cell `:terrain` (open/complex) affecting combat resolution.
- Complex terrain favors strong soldiers; open terrain favors large numbers.
- Provides an environment knob for scenario design.

### 4) Brood Raiding as a Win Condition
- Add brood/pupae resources at homes.
- New action: `:raid` to capture brood and convert into new workers.
- Motivates war beyond food scarcity.

### 5) Chemical Warfare / Infiltration
- Add “odor mask” or “propaganda pheromone” channels.
- Enables sneak entry into enemy home; high risk, high reward.

### 6) Formations + Sacrificial Workers
- Frontline formation gives damage reduction to majors behind it.
- Workers can “shield” to absorb losses before majors engage.

### 7) Corpse Recovery as Resource
- Dead ants create `:corpse` items on cells.
- Retrieval converts to food at home, creating post-battle resource loops.

### 8) Adoption / Colony Fusion
- Post-victory, adopt surviving enemy ants or even merge colonies.
- Mitigates war cost; creates emergent demographic shifts.

### 9) War Triggers Beyond Greed
- Trigger conflicts via overpopulation, border proximity, or reserve pressure.
- War as a strategic response rather than just food scarcity.

### 10) Cyclical Battles (Ceasefire/Night)
- Add a simple day/night tick cycle.
- Conflicts pause at “night,” resume at the same borders at “day.”

### 11) Supercolonies / Alliances
- Expand to 3+ armies with alliance mechanics.
- Supercolonies can split, merge, or coordinate attacks.

## Suggested First Slice (Minimal but Meaningful)

- Add `:role` (worker/soldier) + basic combat resolution on collision.
- Add `:control` scalar per cell to represent territory.
- Add a new observation channel: `:enemy-contact` (ant proximity, not just home).
- Add two new actions: `:attack` and `:defend`.

This slice supports “real war” dynamics while keeping the system small enough
for rapid iteration.

## Notes

- Keep AIF action selection extensible: add combat actions as new candidates
  gated by observation (`:enemy-contact`, `:border-cell?`, etc.).
- Consider tracking war costs explicitly (worker production diverted to soldiers).
- Keep UI minimal at first: render combat events + territory changes.

