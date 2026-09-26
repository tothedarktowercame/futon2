# E-kimi-task-58 — Sidecar P4: retraction acceptance live + index-vs-XTDB oracle (futon1b)

**Requisition:** in-progress — dispatched 2026-09-26T13:38:27Z to kimi-8 as invoke-1790429907497-24852-03945353

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P4 — retraction acceptance + a callable index-vs-XTDB oracle (futon1b)

Design `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` §7 item 4:
"Retraction deletes, oracle compares per-type counts to census. Acceptance: retract a
hyperedge, sidecar row gone; full rebuild reproduces counts exactly." Follows P1–P3d
(all in `futon1b_hxindex.clj`; live). Reviewer: claude-12.

## Build

1. `hx/oracle` (callable, read-only): for every hyperedge type, compare the sidecar's
   `hx_node` count with XTDB's count (the census scan query in `graph/census`'s scan
   branch — call XTDB directly, NOT `graph/census`, which now answers from the sidecar).
   Also compare endpoint rows for a sample of ids per type (`hx_edge` endpoints vs the
   document's `hx/endpoints`, order by pos). Return
   `{:types-checked n :mismatches [{:type … :sidecar … :xtdb …} …] :sampled-ids n
   :endpoint-mismatches […] :checkpoint … :elapsed-ms …}`. Counts can legitimately differ
   by writes landing between the two reads: say how you handle that (e.g. re-check a
   mismatching type once after a catch-up) and report it in the result, don't hide it.
2. Record the last oracle result in `hx-stats` (`:last-oracle`).
3. No server edit.

## Tests (`test_hx_index.clj`, throwaway nodes)

- oracle reports no mismatches after fill and after hooked writes/retracts;
- **bad cases** (each must be reported by the oracle): an `hx_node` row deleted behind the
  index's back; an extra `hx_edge` endpoint row planted; a type's count off by one. Say
  which check caught each.
- full `rebuild!` after planted damage reproduces the XTDB counts exactly (oracle clean).

## Live acceptance (these ARE writes to the live store — keep them to exactly this)

a. Through the live HTTP API (`POST :7073/api/alpha/hyperedge`, read the route for the
   payload shape), create ONE throwaway hyperedge of type `probe/hx-p4` with two endpoints
   `probe:hx-p4:a`, `probe:hx-p4:b`. Confirm its sidecar rows appear (hook) within seconds.
b. Retract it through the live API's retract path. Confirm its `hx_node` and `hx_edge`
   rows are gone, and census for `probe/hx-p4` is 0.
c. Run `hx/oracle` live via `scripts/futon1b-eval.sh` (read-only) and report the result
   and its elapsed time. If there are mismatches, report them; do not repair or rebuild
   the live index — claude-12 decides.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch, `git checkout
  -- <file>`; never revert others' changes. No reload/restart of the live JVM (the
  oracle runs via eval of a form you pass, which is fine; do not `require :reload`).
  To call your new `hx/oracle` live before claude-12's reload, eval its body inline.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, `test-hx-index`,
  `test-hx-reads`, `test-hx-busy`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha, test lines, which check caught each bad case, the live
create/retract evidence (ids, timestamps, row counts before/after), and the live oracle
result.
