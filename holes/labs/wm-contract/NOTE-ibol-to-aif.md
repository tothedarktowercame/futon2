# NOTE: IBOL → AIF — the ChipWits action vocabulary transplanted to the real loop

Joe with zai-7 (zai), 2026-09-12. Companion to `NOTE-chipwits-iconography.md`
(the static analogy) and `SPEC-zaif-harness-v1.md` (the test map). This note is
the *high-level design pass Joe asked for*: read the 1984 operator vocabulary
(`docs/ChipWits_Mac_Manual.pdf`, ch. I) and ask what each action *is* when the
maze is replaced by the real scenario — an agent in a known-to-be-interactive
loop over the futon1b store, a latent counterpart, and a mission. Implementation
detail deliberately set aside (per Joe, this session).

## The substitutions (maze → real world)

| ChipWits | Ours |
|---|---|
| tile | one turn |
| room / Environment | the clocked mission |
| distance (Range Finder) | expected remaining cost / precision of the target |
| wall, door | invariant, refusal (workarounds-forbidden) |
| bomb | an irreversible act that destroys the actor's standing (bad commit, wrong bell to a stakeholder) |
| pie, coffee (fuel) | operator attention — restored by the *yield* arm, not by pickup |
| damage meter | the correction ledger (γ hits taken) |
| score | realized mission outcome — what G was supposed to predict |

## The four load-bearing transplant facts

These are things the *actual manual* says that the design should keep, not
charm:

1. **Every chip has two wires.** LOOK/FEEL/SMELL branch true/false on the Thing;
   there is no "maybe." Transplant: every observation verb returns
   `thing | typed-none` and both wires are wired to authored continuations.
   The false wire is a first-class path, not an error — the
   `an-unmodelled-response-stops-the-line` discipline, made physical.
2. **Sensors are banded by range, and range is priced.** FEEL (one tile, cheap,
   always safe), LOOK (line of sight, writes *distance* to the Range Finder),
   SMELL (whole room, untargeted). zaif v0 has one undifferentiated retrieve
   arm; the design move is a **three-verb observation vocabulary** with a
   precision row per verb (R7), so the controller can choose *how far to look*
   the way ChipWits chose FEEL vs LOOK vs SMELL.
3. **The Move Stack is egress planning.** ChipWits SAVE MOVEs so it can retrace
   its steps out of the maze. Transplant: every act chip auto-saves its inverse
   (the undo path of a live edit session); *yield* is pop-the-Move-Stack —
   return to the last state the operator vouched for. The plan trace is not
   narration, it is a re-walkable path (replay-delta-0, U1's property).
4. **KEYPRESS is the always-checked chip.** The player can step in mid-run and
   redirect flow. Transplant: the operator-interrupt test sits ahead of every
   non-trivial act — a pending operator turn or autoclock ambiguity (mission X
   or Y?) branches true and pre-empts the plan. This is the constitutional
   form of "known-to-be-interactive is a premise."

## The operator table (IBOL verb → AIF verb)

- **FEEL thing** — cheap adjacent check: current transcript tail, last tool
  result, shelf contents. Cost ~0; precision = channel's.
- **LOOK thing** — named-target query: a Z1 library XTQL form bound to a target
  record shape. Writes the Range Finder: the target's *estimated remaining
  cost-to-go / uncertainty* (posting statistics, IDF-ish), so later chips can
  compare against it.
- **SMELL thing** — untargeted room scan: the coordination stream, recent
  events. Candidate generator only; membership is decided by the store (P2's
  ruling, restated as a chip).
- **PICK UP** — acquire into working memory (open the tin): the observed Thing
  becomes shelf state. No arguments: it picks up whatever FEEL/LOOK certified
  is adjacent — you cannot pick up what you have not observed adjacent (no
  confabulated acquisition).
- **ZAP** — the irreversible act: commit, bell out, publish. Comes with the
  manual's own hazard table: zapping a bomb destroys you (invariant violation);
  zapping pie wastes the fuel pickup would have gained (a destructive act that
  forecloses the gentle one — i.e., when act-dominates-ask flattens
  relationships). ZAP is the only chip that can *damage*: each damage event is
  a γ hit on the damage meter.
- **MOVE fwd/back/turn45** — the act arm's pragmatic step; back and rotate are
  first-class (retreat and reorient are cheap in the loop, unlike the maze).
- **SAVE MOVE / COMPARE MOVE** — write to / test the egress stack (fact 3).
  COMPARE MOVE with the empty-square argument is the *am I done / out of
  plan?* test — the typed-empty path.
- **SAVE THING / SAVE NUM / POP** — shelf management: bounded slots, visible
  contents (the Memory panel).
- **SING** — self-report. Charming; not evidence (unchanged from the
  iconography note). SING Fuel/Range/Damage is the *only* sanctioned SING:
  singing a meter's value is a typed read, singing prose is not.
- **KEYPRESS** — operator interrupt (fact 4).
- **COIN FLIP** — exploration at G-ties: when two arms' G difference is within
  the ledgers' resolution, flip; record the flip. (v0's tie-break sort order is
  a hidden coin — make it a visible one.)
- **SUB-PANEL / BOOMERANG / LOOP / JUNCTION** — program-flow, not behavior:
  cascade composition (U5's opaque seam), return-with-value (BOOMERANG), the
  horizon pin (LOOP = R13's T=1 within turn; mission supplies longer), and the
  authored edge (JUNCTION = wire).

## The board is the graph (Joe's refinement, 2026-09-12)

Tiles are not merely dropped — they are **replaced by the store's own
geometry** (Joe, this session): the XTDB evidence/hyperedge graph is
traversable like a board.

- **Adjacency is recorded, not imposed.** FEEL's four neighbors become the
  typed hyperedge relations: reply-chain, thread, forks, sorry-overlap
  (Jaccard over `:hx/endpoints` — already a Z1 sketch). The lattice is the
  hyperedge set.
- **Bitemporality beats the frozen maze.** "What was adjacent when the agent
  chose" is an as-of query, so the board the robot ran on is replayable —
  the audit property, restated as geometry.
- **Distance = hops × precision decay.** The Range Finder measures
  *evidential* distance: hyperedge hops to the target, discounted by each
  hop's R7 precision. Two hops through declared marks are closer than one
  hop through a 0.42 lexical guess. This is the retrieve arm's score input
  computed from the board, not tuned.
- **MOVE's arrows become edge types.** Advance = follow an edge of the chosen
  type; back = retreat along the egress stack (unchanged); turn-in-place =
  *change the edge type being traversed* (scan the same node along replies
  vs along sorry-neighborhoods — the robot rotating to a different axis of
  the same graph).
- **Panels are cascades, fully.** Main Panel = the cascade; Sub-Panels A–G =
  sub-cascades (opaque at the U5 seam); BOOMERANG = return-with-value;
  LOOP = the R13 horizon pin; JUNCTION = the authored edge. The finite
  panel *is* the stopping-rule claim: a cascade is a bounded authored board
  you can read at a glance. Workshop discipline = edges change between
  turns with an author, never mid-turn — which is why the controller sits
  between turns.

## What the real loop has that the maze did not (the design's new rows)

- **The room talks back.** No ChipWits Thing ever addressed the robot. Our
  Things include counterparts whose turns arrive *unrequested* — the SMELL
  channel carries speech acts, and "operator said X" is a Thing with its own
  precision row. This is where stakeholder modeling enters at chip grain: a
  counterpart is not a wall to FEEL for; it is a channel whose Thing-vocabulary
  must itself be learned under R7 (measure the guesser first).
- **Fuel is someone else's scarce resource.** In ChipWits the robot burns its
  own fuel; here the ask arm burns the *counterpart's* attention. Coffee (fuel
  restore) = the yield arm: give the turn back and the relationship's budget
  recovers. The fuel meter is per-relationship, not per-robot.

## One harness per role — boards are Warehouse artifacts (Joe, 2026-09-12)

The custom-harness work (M-custom-harness) is the IBOL position exactly: there
is no *the* harness, only a Warehouse of saved boards. A stakeholder-role or
task harness is a differently-authored panel over the same robot:

- **Per-stakeholder boards.** A counterpart-specific panel prices
  `:operator-attention-cost` from that person's actual reply latency, seeds
  its Things-vocabulary from their documents (text sidecar, measured channel
  precision first), and carries its own ZAP hazard table (for an external
  stakeholder, publishing-without-review is the bomb).
- **Per-task boards.** Sensor verbs swap with the domain (a math board's LOOK
  is a Lean query; its FEEL is kondo/paren-check) and FUEL is token budget.
- **Composition is cheap because patterns are policies** (§1c): a board is a
  named bundle of library patterns; minting a harness is
  select-compose-name, and PSR/PUR is the Workshop's assembly log.

Discipline the metaphor insists on: Workshop-before-run. "On the fly" means
fast per-mission composition — never mid-turn rewiring. Board swaps occur at
clock-in or between turns as authored, provenance-carrying events (which board
governed which decision), and the R17 drift-pin extends to boards: no chip
appears or re-wires without an author. A crew member you can reconfigure
invisibly is not inspectable, and inspectability is the crew's whole
competence story.

## Deliberately not transplanted

- The fixed 45°/tile geometry — superseded by the graph-board section above:
  distance is hops × precision decay, not tiles (the geometry that *is*
  kept is the recorded hyperedge adjacency).
- Points-for-zapping (no adversarial Things; hazards are typed, not enemies).
- The finite panel *size* as the stopping rule keeps its meaning (finite board
  = readable policy) but the bound is the mission's, not a screen size.

Row links: U5 (opaque candidates — the SUB-PANEL seam), U7 (per-tool R2/R16 —
two-wire semantics per chip), U9 (R7 precision rows per observation verb),
R13 (LOOP pin), M-zaif-harness-v1 (the edition this design would ride).
