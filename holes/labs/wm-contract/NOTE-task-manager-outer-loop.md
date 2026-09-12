# NOTE: the task manager — the outer loop as a crew member (spec sketch)

Joe with zai-7 (zai), 2026-09-12. Joe's framing: before the cartographer,
specify the task manager. It shares idioms with inbox-zero (keeping
things fresh) but stages work rather than clearing it: look across the
mission landscape (the Cascade Live register), find high-priority items,
push them to the inner loop. It is almost the War Machine's OUTER LOOP —
which exists as design (M-zaif-harness-v1 §INSTANTIATE's S rows: map the
mission selector, step one selection manually, selection→clocking) but
is not currently being run. The task manager managing the Cascade Live
page IS the outer loop in crew-member form, and its own staged work is
the continuation of the WM buildout once the inner loop is sorted.

## From the chip-boards perspective

The task manager is the third binding of the coherence-keeper schema —
and the first one that also SELECTS:

- disagreement predicate: an issue is stageable when (fresh) ∧ (owned or
  ownership-proposed) ∧ (no live conflict) ∧ (prerequisites done) —
  exactly the issue board's own column lattice (:ready-for-lane is this
  predicate already computed; the task manager keeps it FRESH, which is
  the inbox-zero idiom: 324 rows are stale by the register's own
  freshness criterion today).
- transition: stage → push to a lane via CLOCKING (M-autoclock-in is the
  existing mechanism; the auto-clock witness is the provenance of which
  work was pushed when).
- hazard table: pushing work an operator has not prioritized; clocking
  an agent onto a mission mid-turn (mid-session re-clock rule: in-flight
  acts complete under the old mission's G).

## What it computes and what it receives (the constitutional line)

- It COMPUTES urgency signals — measurable quantities: staleness age,
  downstream blockage count (how many rows name this as prerequisite),
  age-of-oldest-ready-item, disagreement density on a subject. These are
  facts, not preferences.
- It RECEIVES priority. Ranking "high-priority" is a preference over
  outcomes — Joe's or the WM strategic loop's, never the task manager's.
  It surfaces the urgency table and the operator (or L4) picks; the pick
  is a declared mark, precision 1. This is M-zaif-harness boundary 1
  (strategy received, not computed) applied to scheduling.
- It NEVER pilots: pushing to a lane is a bell/clock event (an L3 act
  with provenance), not an edit into another agent's board.

## Relationship to the crew built so far

- inbox-zero keeps the REPOS coherent; the cascade-verifier keeps the
  REGISTER coherent; the task manager keeps the WORK QUEUE coherent —
  third keeper, plus the selector horn.
- It consumes the verifier's output (fresh rows) and produces the input
  of the N3 executors (staged, conflict-free, prioritized items). It is
  the hinge between N1 and N3 in the needs hierarchy.
- The zaif-harness worklist's S rows are its own build steps: S1 map the
  old mission selector (this note is S1's input), S2 one manual
  selection (the task manager's first cycle, done by hand), S4
  selection→clocking (the push mechanism).

## Coordination

zai-5 is working the War Machine build. The outer loop is the WM's; the
task manager must be legible to it, not parallel to it. Open questions
for zai-5: (a) does the WM strategic loop already name the priority
function the task manager should receive? (b) is clocking the sanctioned
push primitive, or does the WM want a different lane-intake event?
(c) where does the outer loop's G live — mission-parameterized like the
inner loop's?

Build order: the verifier's freshness work first (it is the task
manager's eyes), then S2 manual selection, then the staging board.
Cartographer parked until this lands (Joe's sequencing).

## ANSWERED: the WM build's rulings (zai-5, 2026-09-12, from RUN4 live work)

(a) **Receive `ranked-actions`; never derive a ranking.** The strategic
loop's judgment already emits `ranked-candidates` + `selection-reasons`
(fed by `futon3c.aif.intrinsic-values` posteriors, rehydrated at JVM
start). That shape IS the machine's canonical "preferences over missions
right now." If priority must ever be injected rather than received, the
sanctioned channel is the RUN4 operator-selection pin structure
(`:operator-selected` mode with authority-ref) — not a new one.

(b) **Bells are push, clocking is context, packets are the gate.** Three
distinct channels: the bell (`agency_send --kind request`) is the
sanctioned push primitive (the WM loop dispatches author/reviewer turns
this way); clocking (M-autoclock-in) sets mission targeting and
dispatches nothing; RUN4 lanes take operator-provisioned packets only —
frozen pins, sha-chained, attempt-each-once. A task manager pushing into
a pinned lane goes through packet minting, never direct dispatch. The
admission apparatus exists precisely to refuse unsanctioned intake.

(c) **The outer loop gets its own small G — register-liveness facts,
inbox-zero-shaped.** Mission work keeps the per-cohort grounded-success
contract. The outer loop's success predicate: "no fact in the register
is older than its own criterion, and every stageable item has been
surfed with its urgency facts" — all observable, nothing adjudicated.
Division of labor confirmed: freshness is the VERIFIER's job to measure,
the task manager's to surface and act on.

**Wiring caution (learned by a burned cohort attempt):** RUN4 selection
windows require cast actors IDLE. Surface-to-lane handoffs schedule for
idle, never fire-and-hope.

Revised constitutional line, in one sentence: the task manager receives
`ranked-actions`, pushes via bells, attributes via the clock, gates
pinned lanes through packet minting, and its own success is a freshness
predicate — five verbs, none of them pilot.

## Automatability as a triage dimension (Joe, 2026-09-12)

Fourth question after fresh/owned/unblocked: HOW AUTOMATABLE is this?
Don't feed the WM non-automatable tasks. Three-valued, and the middle
value is the load-bearing one:

- **automatable-now**: a cascade exists that spans turns of this shape —
  go ahead once a working lane takes it (the "just go" bucket in the
  current tracker).
- **needs-operator**: the task's shape includes a judgment only the
  operator can make (rulings, preferences, acceptance) — route to ASK,
  never to a lane. Some tracker items are permanently this bucket.
- **not-yet**: no spanning cascade today. Operator turns were, by
  definition, non-automatable AT THE TIME THEY WERE DONE — but that
  verdict decays as the library grows.

Two consequences:

1. **The predicate is a query, not a judgment**: "is there a cascade
   spanning turns of this shape?" is askable on the association map
   (cartographer t3/t6). This is the crossover between task manager and
   cartographer: the task manager consumes the map's spanning-structure
   read as its triage input, and the map's growth rate IS the
   automatability frontier moving — yesterday's not-yet becoming
   today's automatable-now, measured rather than felt.
2. **Automatability verdicts are fresh-stamped like everything else**
   (the R14 idiom): a "not-yet" from an old library version is stale the
   moment relevant patterns or cascades land. The task manager re-tests
   the not-yet bucket on map growth, not on a timer.
