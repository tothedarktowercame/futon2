# SPEC: chip-boards v0 — a simple agent on reusable chips

Joe with zai-7 (zai), 2026-09-12, turning the IBOL→AIF discussions
(`NOTE-ibol-to-aif.md`, `NOTE-chipwits-iconography.md`) into an
implementation specification. Goal: the smallest build that lets us author a
board as data, run a simple agent on a simple task, and read back everything
it did in the language it was written in. Reuses zaif v0's substrate
(`zaif_controller.clj`, `zaif_inputs.clj`, Z1 ledger library); does not
replace it — the G controller and the board coexist, the board being the
*cascade* side (the panel) the controller sits between.

## 1. Board as data (the Warehouse record)

```clojure
{:board/id      "b-ledger-keeper-0"
 :board/version 1
 :entry         :ck/entry
 :chips
 [{:chip/id     :ck/entry
   :verb       :keypress            ; pending operator turn / autoclock ambiguity?
   :wires      {:true :ck/hand-back :false :ck/look-unbacked}}
  {:chip/id    :ck/look-unbacked
   :verb       :look
   :args       {:thing :report-claim-without-backing-query   ; a Z1 query name
                :scope :mission}
   :writes     :range-finder                          ; evidential distance
   :wires      {:true :ck/feel-detail :false :ck/yield}}
  ...]
 :constants    {:fuel-budget 4000 :ask-attention-cost 0.65}   ; declared, not tuned
 :provenance   {:author "joe+zai-7" :clocked-mission "M-..."
                :assembled-at ... :board/parent nil}}
```

Rules (invariants, not conventions):

- **Finite**: `:chips` is a vector, every wire names a chip id in it or the
  terminal `:yield`/`:done`. A board that cannot be read at a glance fails
  review.
- **Two-wire**: every observation verb (`:feel :look :smell :keypress
  :compare-move`) has exactly `:true`/`:false` wires. No maybe, no nil wire.
- **Authored edges, witnessed rewiring**: wiring exists as data. Mid-run
  rewiring is ALLOWED — that is the AIF upgrade over 1984 hardware — but
  only as a **board-transition event**: a diff record in the trace
  (which chip/edge appeared or changed, under which authorship). Self-
  authored edges take effect at proposal precision (low); only an operator
  or adjudicated outcome ratifies to precision 1 (the R9/R7 asymmetry
  applied to structure). A rewiring the trace cannot show is drift and
  fails review.
- **Board swaps are events**: running under a board is recorded
  (`:provenance` + a fired-chip trace), so "which board governed this
  decision" is always answerable (auto-clock witness pattern).
- **Constants are declared**: board-local, versioned with the board, drift-
  pinned (R17 rule extends to boards).

## 2. The chip library (verbs as pure functions)

Every chip is `(fn [state args] {:branch :true|:false :effects [...] :state' ...})`
— pure, no I/O inside; effects are commands the executor performs and
witnesses. State is a plain map:

```clojure
{:shelf         {}          ; thing/num slots, bounded, visible
 :move-stack    []          ; egress: saved traversals/inverses
 :meters        {:fuel n :damage n}
 :range-finder  nil}
```

v0 verb set (each ships with its R2/R16 test pair — U7 discipline):

| verb | reads | effect | notes |
|---|---|---|---|
| `:feel` | local: current turn tail, last tool result, shelf | none | free, always safe |
| `:look` | named Z1 query + target Thing | effect records observation | writes evidential distance (hops × precision decay) to `:range-finder` |
| `:smell` | coordination stream / recent events | none | candidate generator only |
| `:pick-up` | shelf adjacency | shelf write | only after feel/look certified adjacency |
| `:zap` | hazard table | irreversible-effect command (commit/bell/publish) | checks board's hazard table first; damage on γ hit |
| `:move` | edge type | traversal effect | auto-pushes inverse to `:move-stack` |
| `:save-move` / `:compare-move` | move-stack | stack write / branch | empty-square arg = "out of plan?" test |
| `:keypress` | pending operator turn, autoclock ambiguity | branch only | pre-emption point |
| `:yield` (terminal) | — | hands the turn back | pops move-stack to last vouched node |
| `:coin-flip` | — | records a flip | only at G-ties |
| `:sing` | meters only | typed self-report | meter values only, never prose |

Out of v0: `:sub-panel` (cascade composition — arrives with U5/LA2, the seam
is already opaque), learned anything (R17 stub, drift-pinned).

## 3. The executor (small)

Between turns: load clocked board → run from `:entry` until a terminal or
fuel exhaustion → every fired chip appends a trace record (chip id, branch
taken, range-finder value, effects with receipts, board version) as
evidence — the debug panel, in the board's own vocabulary. Mid-run rewiring
arrives as a board-transition event (see §1): the executor records the diff
and continues under the new version. The zaif G controller may *select* a
board at clock-in; structure learning — the agent authoring transitions —
is the sanctioned R17 path: proposals at low precision, never self-ratified
(SING is not evidence; neither is the Workshop bragging about its own
rewiring job).

## 3½. Hierarchy criterion (Joe, 2026-09-12 — a key criterion for the project)

The bridge between the per-agent harness and the system that moderates
behavior across the crew is hierarchical AIF, and the WM does not yet have
that level sorted — this project is depending on it working, so it is a
criterion, not a hope.

Levels (each an existing artifact except the last):

| L | unit | timescale | "G" there | artifact |
|---|---|---|---|---|
| 0 | chip | one action | none — declared arithmetic | verb + R2/R16 pair |
| 1 | board/cascade | one turn | fixed constants | board EDN, versioned |
| 2 | agent loop (zaif) | between turns | G(mission), four arms | controller + clock |
| 3 | mission/clock | session | :wants as partial preferences | M-…, autoclock witness |
| 4 | crew moderator / WM strategic | crew-wide | strategic G: which mission, which crew | **the missing level** |

Bridge rules:

1. **Adjacent levels speak only in typed records.** Downward = selection and
   parameterization; upward = evidence (traces, meters, proposals). Each
   level *authors* the level below; none *drives* it ("constructor, not
   pilot", generalized).
2. **Precision asymmetry up the whole stack.** A level may lower its own
   precisions; only the level above ratifies upward. Board transitions are
   ratified at L3/L4; mission changes by the operator/WM.
3. **Slower levels pin faster ones.** Constants ⊳ boards ⊳ missions ⊳ crew
   composition, in decreasing rate of change. The fast level never
   recomputes what the slow level settled.
4. **L4's job is ratification and cross-agent ledgers**: γ per board
   *version* across the crew (do these boards earn their fuel?), ratifying
   self-authored transitions, mission arbitration when clocks disagree.
   The APM three-runs experiment is L4 evidence gathering: same maze,
   different L1 artifacts, compared by query.

Collapse test (the failure this criterion prevents): a trace where a level-N
actor fires a level-(N−2) effect is a type error — the moderator must not
pilot chips (monolith; inspectability dies) and a board must not pick
missions (strategy computed, not received — M-zaif-harness boundary 1).

## 4. Shortlist of first tasks (each a small board)

**0. Inbox-zero as the resident base-case board** (Joe's observation,
2026-09-12: "inbox zero is the zero theorem for this system"). Not a new
build so much as a transcription: the standing loop (daily check, manifest
sweep, per-path LOOK, typed refusal records) already has two-wire semantics
(`:inbox-zero/refusal` was minted by C446 after a silent-refusal failure)
and a named fixed point ("clean" — a definition that grows a clause per
measured failure, i.e. a precision ledger for a predicate). Its history is
the spec's test suite pre-encountered: C446 = the false wire with no
continuation; the U59 sweeper-commits-under-a-live-edit incident = the §3½
collapse test observed in the wild. Base-case claim: every other board's
audit property (claims re-derivable, replay-delta-0, green-tests-mean-it)
presupposes record coherence — clean, not behind, no stale base — and this
loop is what establishes that precondition. The 355-commit stale-base
incident is the proof: good-faith verified work, void base. Hazard table
from day one: committing an in-flight edit is a bomb (FEEL for an active
turn before ZAP).

Then, ranked by how much of the design each exercises while avoiding
hazards:

1. **Ledger-keeper** (recommended first). Each turn: LOOK for report claims
   without a backing Z1 query; if found, FEEL the detail, SING the meter
   reading (typed), YIELD. Pure read, no ZAP, exercises look/feel/two-wire/
   range-finder/yield — and is U8's reporting gate as a resident crew member.
2. **Greeter/triage.** On turn arrival: KEYPRESS check, SMELL the coordination
   stream, branch to hand-back or act. Minimal board (~5 chips); proves the
   executor and trace discipline.
3. **Backtracker.** A small edit excursion: MOVE out along edges with
   auto-saved inverses, then YIELD = pop the move-stack home. Proves egress
   and the "yield returns to last vouched node" property; replay-delta-0 test.
4. **Stakeholder-greeter (Rebecca-lite).** The per-role demonstrator: high
   declared attention cost, ask only when C-uncertainty (from the R7 channel
   table) exceeds it, ZAP forbidden without review (hazard table makes
   publish-without-review a bomb). Depends on 1–3 landing first.
5. **Chip self-test board.** A board whose job is running the other boards'
   per-chip test pairs — U7 as a resident board rather than a suite.

## 5. Acceptance

- A board is authored as one EDN file, loaded, run one turn, and every trace
  claim re-derivable from Z1 queries + trace records (the U8 property, held
  from day one).
- Each verb ships with its R2 (envelope-or-typed-none on read) and R16
  (witness-with-provenance on fire) test pair before use in a shipped board.
- No unwitnessed rewiring (test: a board diff without a corresponding
  transition event in the trace fails; mid-run *rewiring with* a typed
  transition event succeeds).
- The **hierarchy criterion holds**: adjacent levels communicate only in
  typed records; the collapse test (an L-N actor firing an L-(N−2) effect)
  fails as a type error.
- Constants pinned: byte-identical board version + identical inputs ⇒
  identical trace (determinism holds per version; coin flips recorded).
- **Cross-run egress**: the move-stack persists across runs, so a later run
  can yield back to an earlier run's vouched state *under the board version
  it ran under* (bitemporal boards; run-the-old-version-as-of).

Explicitly not in v0: sub-panels/cascade grain, constant tuning (structure
learning via board-transition events is allowed from day one; *numeric*
learning stays R17's named stub), any default-on deployment (boards are
opt-in, J-gated like everything else), stakeholder channel calibration
beyond declared values.

Row links: U7 (per-tool pairs), U8 (reporting gate), U9 (R7 channel table),
U5 (the sub-panel seam), R13/R17 pins, M-zaif-harness-v1 (edition).

## The recursion ruling (Joe, 2026-09-12): every chip can be an expansion board

Like the ChipWits SUB-PANELs (A–G, entered by the SUB-PANEL chip,
returned from by BOOMERANG), any chip may itself be a board — its own
wiring diagram — so chip ⊂ board is recursive: board = chips + wires,
chip = (verb | board). Fully recursive, with three laws that keep it
from being a soup:

1. **Opacity at the parent**: a chip-that-is-a-board exposes only its
   interface — two wires, args, effects, fuel price. The parent cannot
   see its internals, so the parent's tests survive the child's
   rewiring. (This IS the U5 opaque-candidate seam, now with a
   referent.)
2. **Finiteness at every level**: each board, however nested, is a
   finite panel readable at a glance. Recursion composes; it never
   licenses an infinite board.
3. **BOOMERANG = return-with-value**: sub-board completion is a typed
   return to the parent's next wire — sub-boards never escape upward
   except through their interface.

The recursion is what makes modules compose: the cartographer's 10-turn
module (a board proposal) can be wired into a larger board as ONE chip,
whose provenance batch names the turns it was read from. Nesting depth
is itself a meter (a stack-like fuel cost is the natural pricing).
