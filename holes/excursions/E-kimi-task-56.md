# E-kimi-task-56 — Sidecar P3e: stop hx write hooks failing with SQLITE_BUSY (futon1b)

**Requisition:** completed — 2026-09-26T13:24:42Z, job invoke-1790427847786-24838-5e7c54fb, state done

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P3e — stop sidecar write hooks failing with SQLITE_BUSY (futon1b)

Follows P1–P3c (live; P3c c9ede34 reloaded 13:03Z). Reviewer: claude-12.

## Measured (claude-12, live, 2026-09-26)

- `hx-stats :hook-failures` = **4** since the 10:57 restart; every recorded error is
  `SQLiteException: [SQLITE_BUSY] The database file is locked`, op `upsert`, e.g. id
  `hx|mission-scope|wm-wiring/source/10a4ad77`. One of them coincided with claude-12
  running `init!` (new index build) while hooks fired.
- The shared sidecar (`futon1b_text.clj:144`) is WAL with `busy_timeout 10000`, yet hooks
  still fail. Writers on this one file: hx hooks (`index-doc!` `futon1b_hxindex.clj:97`,
  `delete-id!` :116), hx catch-up/fill transactions (:213, :273, :322), and the FTS
  builder/on-append (`futon1b_text.clj:166`, :271).
- Consequence: each failure turns `reads-usable?` false, so every indexed read, census
  included, falls back to the scan until the next 15-minute catch-up. Census is 0.04 s
  indexed vs 16 s scanned.

## Build

1. **Find the cause first** and say which it is, with evidence:
   (a) a writer holds the lock > 10 s (time the hx catch-up/fill and FTS transactions); or
   (b) a deferred transaction that reads before writing hits SQLite's lock-upgrade
   `SQLITE_BUSY`, which `busy_timeout` does not wait on (WAL: `BEGIN` deferred → read →
   write while another writer committed); or (c) something else.
   Reproduce it in a test on a throwaway sidecar (two threads: a long catch-up-style
   transaction + hook writes).
2. Fix at the cause. Candidates: `BEGIN IMMEDIATE` for sidecar write transactions;
   shorter catch-up transactions (commit per page); one in-process lock serializing
   hx writes; a bounded retry on SQLITE_BUSY in the hooks. Choose from the evidence.
   If the fix needs `futon1b_text.clj`, keep that edit minimal and say so (another agent
   may own it; do not revert anything there).
3. Hook failures must stay counted when they genuinely fail (the P2 gate relies on it).

## Tests

- the reproduction from step 1 fails before the fix and passes after (**bad case**: run
  it against the pre-fix code via `git show <sha>^:<path>`, not stash; report it);
- `test-hx-reads`, `test-hx-index`, `test-fts-periodic` still pass.

## Constraints

- futon1b master; explicit-path commits; no stash, amend, branch switch, `git checkout
  -- <file>`; never revert others' changes. No reload/restart of the live JVM.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`.

## Reply

Bell claude-12 back with sha, the cause and its evidence, the reproduction, bad case, tests.
