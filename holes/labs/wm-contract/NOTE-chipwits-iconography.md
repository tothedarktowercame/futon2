# NOTE: ChipWits as an iconic language for zaif agents and their harnesses

Claude (claude-1) with Joe, 2026-09-02, for fun — and because the analogy is
tighter than it has any right to be. Source material:
`~/code/chipwits-forth` (Doug Sharp's original FORTH source and disk images,
open-sourced for the 40th anniversary; manuals under `docs/`, screenshots
under `mac/docs/` and `c64/docs/`). Joe's provenance claim for zaif is already
on record at `P-validated-R5.md:704-716`: "program the robot from a finite
board of operation chips, then watch it run." This note just cashes it out,
icon by icon.

ChipWits' programming language was called **IBOL — Icon-Based Operating
Language**. The name alone is the thesis: the policy is made of icons you can
point at.

| ChipWits (1984) | Ours (2026) |
|---|---|
| The ChipWit robot | The zaif agent — it has no goals of its own; it runs what was wired |
| An IBOL chip (FEEL, LOOK, SNIFF, MOVE, TURN, ZAP, PICKUP…) | A pattern: one guarded operator. Sensor chips are `find`/retrieve arms; actuator chips are act arms |
| The chip board / panel, wired with FLOW | The cascade — a DAG over patterns with authored edges; WIRE is the authored edge, FLOW is precedence |
| The board is FINITE | The stopping rule made physical (P-validated-R5's own phrase) — a bounded cascade you can read at a glance |
| The **Workshop** | The constructor (LA3): the cascade is assembled BEFORE the run and cannot be rewired mid-run — inspectable, citable, refusable, exactly the property §1c demands of a policy |
| The **Warehouse** (saved robots) | The library + registry: policies as artifacts you store, name, and re-deploy |
| **Environments** (Greedville, mazes) | Missions/domains: Snatch, Ants, zaif tasks, ALFWorld — same robot, different room |
| FUEL (and coffee to restore it) | Token budget and `:operator-attention-cost` — zaif v0's constants literally price these |
| PIE / COFFEE / OILCAN | The preference vector C: outcomes the field rewards |
| BOUNCER | Typed adverse observations — the refusal/hazard class AC1-AC7 made recordable |
| The Cycles counter | Ticks |
| The **Memory** panel's chip slots | The shelf / working memory — bounded slots, visible contents |
| The **Debug** panel, current chip highlighted, single-step | The trace + per-node assertions: U6's whole design is ChipWits' debug panel for the WM — a regression names the chip that broke |
| Score | Realized mission outcome — what G was supposed to predict |
| SING | The agent's self-report. Charming; not evidence. The audit reads the board, not the song |

Two things the analogy teaches, beyond charm:

1. **The player is the constructor, not the pilot.** ChipWits' entire game is
   that you cannot drive the robot — you author its policy in the Workshop,
   then watch it meet the room. That is the crew model: zaif agents as crew
   members whose competence IS their board. Improving a crew member means
   reworking the board (authored edges; O5's learning moves weights, never
   adds an edge without an author), not whispering to the robot mid-run.

2. **The debug panel is the honesty mechanism.** ChipWits shows you which chip
   fired, every cycle, in the same icons you programmed with. Our equivalent —
   trace records carrying `:tau-source`, per-term attribution, per-node
   assertions — is the same move: the run is explained in the language the
   policy was written in, or it is not explained at all.

Iconography to lift, if we ever draw the zaif harness: chips-in-slots for the
shelf, wire-between-chips for authored edges, the finite board outline for the
budget/stopping rule, the highlighted chip for the current trace position, and
the Workshop/Warehouse/Environments triptych for constructor/library/missions.

Row link: U6 (worklist.edn) builds the debug panel; M-zaif-harness.md carries
the mission.
