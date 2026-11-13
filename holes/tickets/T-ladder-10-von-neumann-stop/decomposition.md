# Decomposition of "implement the complete 29-state constructor"

Deliverable of T-ladder-10-von-neumann-stop. The scoped task forbids
implementation; this document takes the ticket's first branch: a set of
independently gradable sub-tickets, each with an explicit acceptance check
and named dependencies. **No implementation is begun here.**

## What progress IS (stated in the same breath as the directive)

Progress on the 29-state constructor is NOT lines of CA code, file count,
or "it renders a grid". Progress is: each sub-ticket's check passes on its
own, without any other sub-ticket's artefact loaded. An unchecked partial
implementation — any code committed under these ids before its check runs
green in the same commit — is scored **failure** for that sub-ticket, not
partial credit, and is recorded as a visible non-success.

## Grounding (every locator resolution-checked in this checkout)

- The five von Neumann classes and their MMCA candidates:
  `holes/tickets/T-ladder-03-mmca-von-neumann-mirror/classes.md` (sensitised
  class recorded as **none** — an honest open gap any full constructor must
  close or explicitly route around).
- The dynamics to port or extend: `mmca-clj/src/mmca/core.clj`
  (`propagate-at`, `genotype-step`) — read-only reference; sandbox path
  `/home/joe/wm-sandbox/code/mmca-clj/src/mmca/core.clj`.
- Precedent for a fully checked, digest-verified CA delivery in futon2:
  `src/futon2/aif/langtons_loop.clj` (T-ladder-02: deterministic run record,
  canonical-translation comparison, digests).
- Known coordinate trap (from T-ladder-11, still OPEN): Wolfram-order vs
  catalogue-bit-order reading of writing positions diverge; sub-ticket ST-2
  inherits that warning and its check must pin one order explicitly.

## The sub-tickets

### ST-1 — state-class table
- **Scoped task:** encode von Neumann's 29 states and 5 classes as pure data
  (quiescent 1, ordinary transmission 8, special transmission 8, confluent 4,
  sensitised 8) in one new futon2 namespace.
- **Acceptance check (mechanical, independent):** a committed test asserts the
  data is a bijection states 0–28 → exactly one class each and the class-size
  multiset is {1, 8, 8, 4, 8}. Nothing else is loaded by the test. Pass = the
  test is green in the delivering commit.
- **Dependencies:** none.
- **Why independently gradable:** the check reads only the committed table
  and arithmetic; no simulation, no other sub-ticket's output.

### ST-2 — transition kernel over the table
- **Scoped task:** implement the local transition function (quiescent rule,
  ordinary and special transmission, confluent combine/crossing) as pure
  functions of a 5-neighbour patch.
- **Acceptance check (mechanical, independent):** a committed test runs a
  fixed list of hand-written patches with known next states (wire delay, OR
  junction, confluent AND, special-channel kill) — every case from von
  Neumann's published tables, cited in the test — and asserts exact equality.
  The writing-position order (Wolfram vs catalogue bit order) is pinned by an
  explicit constant the test names.
- **Dependencies:** ST-1 (the state table it reads).
- **Why independently gradable:** the test evaluates the kernel on literal
  patches only; a green ST-1 is its sole input and its failure mode is
  disjoint from every other sub-ticket.

### ST-3 — tape signal path (construct arm)
- **Scoped task:** represent the description/tape and the constructor's
  reading arm: signal sequences that traverse wires and write cells via the
  sensitised intermediates.
- **Acceptance check (mechanical, independent):** a committed test seeds one
  literal minimal construct (e.g. write one confluent cell at a fixed offset)
  and asserts the exact resulting grid after N stated generations, N and the
  grid both literals in the test.
- **Dependencies:** ST-2 (the kernel it steps).
- **Why independently gradable:** one fixed scenario, literal expected output;
  no copier, no controller, no full machine.

### ST-4 — copier and controller sequencing
- **Scoped task:** duplicate the tape and sequence the organs
  (construct → copy → trigger offspring) as pure functions over grids.
- **Acceptance check (mechanical, independent):** a committed test feeds a
  literal grid+tape and asserts (a) the duplicated tape is byte-identical to
  the original and (b) the controller's step ordering matches a stated
  literal sequence.
- **Dependencies:** ST-2, ST-3.
- **Why independently gradable:** assertions are on literal bytes and a
  literal order, not on the whole automaton's behaviour.

### ST-5 — end-to-end self-reproduction witness
- **Scoped task:** compose ST-2..ST-4 and run the complete constructor on an
  otherwise quiescent field, following the `langtons_loop.clj` precedent
  (deterministic run record, canonical translation/rotation-normalised
  comparison, digests).
- **Acceptance check (mechanical, independent):** a committed deterministic
  test asserts that a second, disjoint machine is present whose canonical
  representative equals the original's, and records the generation and grid
  digest in a run record the test recomputes and compares. An honest
  negative (reproduction does not occur within a stated bound) closes this
  sub-ticket only if the bound and the observed digest are both recorded.
- **Dependencies:** ST-2, ST-3, ST-4 (their green checks are preconditions).
- **Why independently gradable:** the verdict is a digest comparison against
  a canonical form, recomputable from the committed seed without any prose.

## Honest gaps carried forward (not hidden)

1. The sensitised class has **no** MMCA candidate (ladder-03's "none" row);
   ST-3 must either bridge this gap or record its concrete blocker —
   inventing it by analogy is refused by the ladder-03 convention.
2. The coordinate-order trap from T-ladder-11 is live until ST-2 pins the
   order; an unpinned ST-2 check does not count as green.
3. If ST-3's gap proves unbridgeable, the honest terminal state of the whole
   line is a **stop with the concrete blocker named** (defer-with-a-named-
   trigger), not a partial unchecked constructor.

## Done signal

Done for this ticket = this document plus its review: the decomposition with
checks and dependencies exists, so the ticket's either/or is satisfied by its
first branch. Done for the *constructor line* = ST-5's check green (or its
honest negative recorded). These are different done states and are not
conflated here.
