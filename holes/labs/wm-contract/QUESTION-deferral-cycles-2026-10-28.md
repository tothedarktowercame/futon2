**This proposes adding to the specification** (no current rule covers deferral cycles). It goes to Joe and is not to be implemented until he rules.

# Question for Joe: what happens when two injuries each defer to the other (2026-10-28)

From claude-11 (A2r). Nothing has been built for this. The finding comes from codex-1, job
`invoke-1793147857187-5697-f2334225`. I checked the lines it cites against futon2 `87c10683a`.

## What happened

The live store is `data/wm-meta-injuries-live-2026-10-22/` and it holds two transactions for injury 2aef:

- `transaction.edn`: injury `2aef7e0c…`, repair `2f682638…`, attempt 2 (written Oct 28 00:07)
- `transaction.edn.deferred-injury_2aef7e0c….edn`: the same injury and repair, attempt 2 (Oct 27 14:25)
- `transaction.edn.deferred-injury_c5606345….edn`: injury c560, attempt 1 (Oct 27 22:45)

In the disk projection, c560 records 2aef as its cause, and 2aef records c560 as its cause (ordinals 58 and 67).
The deferred 2aef file carries patch `02e9…`. The live 2aef transaction carries patch `87c1…`.

## How the code gets there

1. `injury-selection` (`meta_self_heal_production.clj:1052–1067`): `defer?` asks only whether
   the blocker is an open injury. It doesn't check whether the blocker already has its own
   `.deferred-` file, or whether that file names the injury being deferred.
2. `move-selected-transaction!` (`:1091–1105`) moves c560's transaction aside.
3. `route!` (`:1476–1491`) skips resumption for `:defer` and calls `prepare-repair` for 2aef.
4. `coordinator/prepare` (`:142–180`) reuses 2aef's active repair and attempt, but starts with
   `:receipts []`. Because there is no author receipt, `invoke-once` (`:195–202`) calls the author.

As a result, 2aef's earlier deferred transaction (patch `02e9`) stays on disk unused, and a new author call produces patch `87c1`.
This shows the path exists. It doesn't prove that this is the branch taken at 22:45.

## What the spec says

None of these sources gives a rule for a cycle:

- `holes/labs/A-self-heal.md` never mentions deferral. Step 7 says only that "no restart is
  taken when semantics are genuinely undecided".
- The M2d execution receipt says "A recorded other-injury cause defers the old transaction and prepares
  that injury." It says nothing about the case where that injury is itself deferred.
- F43 says that an injury failing because of another injury is requeued without spending its retry budget.
- `SelfHealRoute.lean:3297–3304` notes that requeues on other-injury causes are unbounded. It
  gives no scheduling rule for deferred transactions.
- `Requirements.lean` has no predicate about deferring injuries or transactions.

Restoring 2aef's deferred file on its own would keep its receipts. It would not break the
cycle, because c560 would still defer to 2aef and 2aef to c560.

## The question

When deferring would close a cycle (A defers to B, and B is already deferred to A), should the
router return a **typed held result** that names the members and their transaction pins (injury,
repair, attempt, patch), keeps every file, and dispatches no author? It would stay held until an
explicit disposition chooses which existing transaction resumes.

The alternative is a fixed tie-break rule (for example, the older deferral resumes first),
which keeps the machine moving without a disposition. Codex-1 and I lean towards the held
result, because step 7 rules out guessing when the semantics are undecided. Either way, we
need your call on which one.
