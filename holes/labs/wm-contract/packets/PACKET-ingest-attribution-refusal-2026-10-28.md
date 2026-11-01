From claude-11. Repair, two small coupled changes + tests. Bell claude-11 back with a summary + the FULL commit sha(s) (`git rev-parse HEAD` in each repo you commit to).

## Goal

codex-11 stopped at the supported requeue boundary for injury 2aef
(`/home/joe/notes/wm-next/criteria-completeness/requeue-boundary/boundary.md`):
the ordinal-70 retry child is durably `completed` with runtime disposition
`retry-attribution-refused`, but the coordinator cannot see that, so it can
neither requeue nor be stopped from re-dispatching the malformed id.

1. `futon3c/src/futon3c/wm/runner_service.clj:314` `retry-click-status`
   reports only `:status :failed`; a completed child with a typed
   attribution refusal returns nil, so the coordinator leaves the retry
   `:awaiting-retry`.
2. `futon2/src/futon2/aif/meta_repair_coordinator.clj:313`
   `coordinate-queued-retry` reaches the existing retry port before minting a
   replacement, which would replay the malformed id (forbidden by
   `DISCOVERY-retry-attribution-refused-2026-10-28.md` §4).

## Changes

1. `retry-click-status`: also report a record that is `:wm/click-state-v1`,
   matching click id, `:status :completed`, whose runtime disposition is
   exactly `:meta-self-heal/retry-attribution-refused` (cite where that key
   lives in the record). Return it as a durable non-success with the record's
   own result verbatim and the same `:evidence-pin` (sha256 of the acceptance
   bytes) as failures get, marked `:refusal :retry-attribution-refused` so it
   is distinguishable from a service failure. Everything else unchanged
   (queued/running/other completed → nil). Update the docstring.
2. Coordinator: when the observed status for a pending retry is that
   attribution refusal, do not dispatch the existing retry id again; mint a
   fresh requeue through the (already fixed, `5cdaf1c75`) producer from the
   injury's authored patch, as a normal authority-backed ledger transition.
   Cite the code path you change.

## Tests

- futon3c: the status port returns the refusal (pinned) for a completed
  attribution-refused record and nil for an ordinary completed one.
- futon2: given ordinal-70's shape (retry id suffix ≠ authored patch) and an
  attribution-refused status, the coordinator appends a requeue whose retry id
  ends in the authored patch, and never calls the dispatch port with the old
  id (redefine the port to throw).

## Gates

kondo, check-parens; run only the changed test namespaces; commit only changed
files by explicit path in each repo (both are shared). No reloads, no clicks,
no ledger edits (the live requeue is codex-11's, after review and reload).
