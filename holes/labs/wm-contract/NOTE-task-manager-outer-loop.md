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
