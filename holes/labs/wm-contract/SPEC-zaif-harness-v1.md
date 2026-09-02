# SPEC: zaif harness v1 — the historical mission × ChipWits, as R-node unit tests

Claude (claude-1) with Joe, 2026-09-02. Sources mashed up, as asked:
`futon2/holes/M-zaif-harness.md` (2026-07-11, the four-arm controller spec),
`p4ng/app-zaif.tex` (the PLoP paper's transplant appendix — dependency-ordered
route map, every row but the live A/B landed),
`NOTE-chipwits-iconography.md` (this directory), zaif v0
(`futon3c/src/futon3c/agents/zaif_controller.clj`, `zaif_inputs.clj`), and the
equation registry's node table. Joe's framing: unit tests per R node that
exercise each harness component individually — tools one at a time, and the
reporting workflow through the WM edge facilities already built — toward a new
edition of the harness whose agents become the WM's crew.

## The map: R node → zaif harness component → ChipWits icon → unit test

| R | WM meaning | zaif component | icon | the unit test |
|---|---|---|---|---|
| R1 | belief state μ | controller state between turns: task belief + operator-C belief (M-zaif-harness "holds (task belief, operator-C belief, precision state)") | the robot's registers | state persists across two planted turns and is reconstructible from typed records alone — no narrated memory |
| R2 | observation o | every typed input: tool results, operator turns, deliberate memories (app-zaif: "memories as typed R2 observations"), declared marks (the operator-side twin, precision 1 by construction) | FEEL / LOOK / SNIFF chips | **one test per tool**: each of the zai runner's read-only tools returns the typed envelope or a reason-bearing "none" — never an untyped absence. The six memory tools first; each new tool adds its R2 test before it ships |
| R3 | belief update | the fold of R2 into belief | the wire into the register | a planted observation moves belief the documented direction; an empty turn leaves it unchanged with the absence typed |
| R4 | forward model Q(o\|π) | per-arm predicted outcome from the arm's declared yield/cost model — v0's documented constants | reading the chip's label | the arithmetic asserted from constants alone (v0's own stated design: "documented in data so tests can assert the arithmetic without tuning against outcomes") |
| R5 | G = risk + ambiguity | G per arm, parameterized by the clocked mission (app-zaif commitment 1: strategy received, not computed — mission preferences enter as the risk term's targets) | comparing chips before wiring | planted mission C ⇒ G ranks the four arms as the constants dictate; the ambiguity term's contribution isolated per arm (feeds U4's discrimination question) |
| R6 | policy posterior Q(π) | arm selection: retrieve / act / ask / yield, with recall warrants entering here (app-zaif) | the board choosing the next chip | posterior over arms under the flagged score; arms OPAQUE at the seam (U5) so the test survives arms → cascades |
| R7 | channel precision Π | per-channel trust: declared marks (precision 1) vs lexical guesses (measured 0.42/0.19, disqualified as instrument) vs tool results | how hard the chip's input is believed | a declared mark outweighs a lexical guess in the same update, by the recorded precisions — "declare, don't guess; and when you must guess, measure the guesser first" as an assertion |
| R8 | F_π | per-arm mismatch against current belief | the chip that doesn't fit the room | computed from records; replay delta 0 (U1 property applied to zaif's field) |
| R9 | independent witness | proposal ≠ witness (app-zaif: "R9 keeps proposal separate from R16 witness"); checked handoffs adjudicated externally, never by the worker's report | the Debug panel is not the robot | a verdict event authored by the worker seat is REFUSED as a type error; only the adjudicator's rerun mints it |
| R13 | depth T | per-turn horizon (T=1 within a turn; the mission supplies the longer horizon) | one chip per cycle | pinned: the controller never plans past the turn; the pin names where mission-horizon planning would enter |
| R14 | temperature τ | the precision fold over the claimed-vs-verified verdict stream (PZ3: fold ported, has run over the live corpus) | FUEL gauge honesty | a planted verdict stream moves τ the documented direction; an empty stream holds τ with the absence typed (the WM's own never-fires lesson, as a test) |
| R16 | action u | the executed arm: tool invocation, the ask, the yield | the chip FIRING | **one test per tool**, second half: execution produces the R16 witness record with provenance (queries as provenance, Z2's design); yield's witness is the turn handed back |
| R17 | learning | v0: constants fixed BY DESIGN — the honest stub. The learning path is receipt-fed ranking (memory seam) and the Z3 A/B (attention-cost constants tested against each other, no silent tuning) | the Workshop between missions | pins that live constants do not drift; names R17 stubbed; the A/B's two constants recorded as the only sanctioned change vector |

## The reporting workflow test (through the WM edge facilities)

The Ledger library (Z1, "shipped, test-covered") and transcript persistence
already give named, replayable queries over the event record. The test Joe
asked for: drive one full zaif decision, then require that EVERY claim its
report makes is re-derivable from the edge facilities alone — arm chosen and
G terms from the decision evidence, tool calls from R16 witnesses, belief
moves from R2/R3 records, mission attribution from the Z1 queries, status
from the mission-status oracle (never the doc header). A report line with no
backing query is the failure. This is the Debug panel property made a gate:
the run is explained in the language it was recorded in, or not at all.

## What "v1" adds over v0 (the edition, named)

v0 is the Z2 row: four arms, fixed constants, opt-in, replay-calibrated, no
live decisions. v1 = v0 + (a) the per-node test harness above, (b) the R7
channel-precision table as data (marks/guesses/tools), (c) the R14 fold wired
to zaif's own verdict stream (PZ3's adapter, consumed rather than just
ported), (d) the reporting gate. Explicitly NOT in v1: cascade-grain arms
(that is LA2/LA3's delivery; the U5 opaque-candidate seam is the socket it
plugs into), live A/B (Z3 stays preregistration-gated), any constant tuning.

## Proposed rows (stubs; Joe's nod mints them)

- :U7 :class :I — the per-tool R2/R16 test pairs (six memory tools first;
  envelope-or-typed-none on read, witness-with-provenance on fire).
- :U8 :class :I — the reporting gate: one decision, every report claim
  re-derived from Z1 queries and typed records; a claim with no backing
  query fails the suite.
- :U9 :class :RUN — the R7 precision table asserted from the recorded
  probe/mark data (0.42/0.19 vs 1.0-by-construction), as the worked example
  of "measure the guesser first".
- U6 (already minted) remains the end-to-end fitting; U7-U9 are its
  components tested one at a time, which is the ChipWits way: test the chip,
  then wire the board.
