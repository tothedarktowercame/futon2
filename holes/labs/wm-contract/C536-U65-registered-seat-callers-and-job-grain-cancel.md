# C536 — U65: registered seat callers, and a cancel that ends a job rather than an agent

Row `:U65`, class `:I`, owner `:any`, epic `EPIC-run-era.md`. Seat:
`wm-build-work` (the build loop's Claude work seat; that id is now on the
roster, which is half the item). Author ≠ reviewer: this needs a second read.

The row was minted from the F8 slice-7 coordination incident,
`EPIC-run-era.md:923-941`. Two mechanism facts were recorded there; this closes
both.

## (a) The loop's seats have their own registered identity

**The defect.** `wm-build-loop.sh` runs each work seat as a fresh
`claude -p` (`wm-build-loop.sh:59`), which holds no Agency session and, before
this, no Agency id. Auto-bellback routing requires the CALLER to be on the
roster — `valid-auto-bellback-caller?` is `(and (boolean caller-registered?) …)`
at `futon3c/src/futon3c/transport/http.clj:841-847`, checked through
`auto-bellback-caller-registered?` at `:894-896` — so an unregistered `--from`
gets no reply route at all. On 2026-09-05 the seat did the arithmetic and
borrowed the owner's id (`--from claude-1`); the bail-out bellback then landed
in claude-1's session, which dispatched a continuation nobody had asked for.

**The repair, and what it did NOT need.** No futon3c code change. The server
already carries the mechanism for a pull-only caller: registering with
`delivery-mode inbox` makes `inbox-agent?` true (`http.clj:966`), and
`enqueue-auto-bellback!` then writes the completion bell as JSON under
`~/.claude/agency-inbox/<seat>/` instead of trying to invoke the caller
(`http.clj:1023`, `futon3c/src/futon3c/agency/inbox.clj:24-51`). The
roster had **zero** inbox seats before this. So the repair is registration plus
wiring:

- `wm-build-work` and `wm-build-loop` registered as `type claude`,
  `delivery-mode inbox`. Persisted by `roster_store.clj:79-80` and restored on
  boot (`restore-enabled?`, `roster_store.clj:26-38`).
- `wm-build-loop.sh` `ensure_seats()` (`wm-build-loop.sh:13-33`), run at loop
  start. Idempotent: a duplicate answers 409 and is ignored. It runs every
  start so a roster reset cannot silently return the loop to a borrowed id.
- `wm-inbox-drain.sh` (new): prints every bell waiting in a seat's inbox, then
  ACKs it. The loop drains `wm-build-work` into each work prompt
  (`wm-build-loop.sh:106-113`), so a bellback that arrives while no seat is
  running reaches the NEXT seat rather than the owner's session.
- The ACK matters twice: it is also a pull-only seat's only liveness signal.
  An inbox seat never runs an invoke, so `:agent/last-active` moves only on ack
  (`http.clj:5788-5800`), and the idle reaper reads it.
- `worklist-prompt.md:34-43` now tells the seat which id to dispatch from and
  why, since the borrowed id was a seat DECISION, not a line in a script.

**Live round-trip (acceptance clause 1).** Bell from `wm-build-work` to
codex-17, job `invoke-1788646535570-8054-3ea49f27`, 2026-09-05 22:15:35Z.
codex-17 replied `U65 PROBE OK`; the job's delivery record reads
`{:status "delivered", :surface "inbox", :destination
"/home/joe/.claude/agency-inbox/wm-build-work/auto-bellback-invoke-1788646535570-8054-3ea49f27.json"}`.
`./wm-inbox-drain.sh` printed it and acked it; the file moved to
`consumed/`. No owner id appears anywhere in that path.

## (b) Cancel is now at job grain

**The defect.** `POST /api/alpha/invoke/jobs/:id/cancel` already took a job-id,
and its ledger transition and worker interrupt were already per-job. The
process-tree kill was not: it called `interrupt-agent-process-tree!` with only
the AGENT id, and `futon3c.dev/interrupt-agent-invoke!`
(`futon3c/dev/futon3c/dev.clj:3321-3357`) resolves its control from
`!invoke-controls`, which is keyed by agent with no job recorded
(`dev.clj:3284-3291`). So "cancel my queued duplicate" destroyed whatever
process tree the agent happened to be running. It also called
`reg/mark-agent-idle!` unconditionally, handing a mid-turn seat to the next
queued dispatch.

**The repair** (`futon3c` 80f4ebc9):

- `executing-invoke-job-ids-for-agent` (`http.clj:1487-1513`) — the jobs that
  own an agent's subprocess tree right now. `process-owning-job-states` is
  `#{"running" "overrun"}`: `queued` has started no process and `delivered` is
  an inbox drop, so neither may authorise a kill. `overrun` is included
  deliberately — an overrun turn is exactly the one an operator cancels.
- `handle-cancel-invoke-job` (`http.clj:5874-5962`) kills the tree only when
  the named job is the one, and the ONLY one, the agent is executing.
  Otherwise it ends the job in the ledger and answers
  `:action :refused-not-the-executing-job` with the preserved job-ids. The
  ambiguous case (two executing jobs for one agent, which single-flight should
  make impossible) refuses rather than guessing. `mark-agent-idle!` now fires
  only when no OTHER job of that agent is running.
- **A second defect, found while building the control**: cancelling a queued
  job left it in the agent's turn-queue, and `run-invoke-job!` had no terminal
  check — the drainer would later `mark-invoke-job-running!` and resurrect a
  job the operator had stopped. The body is now `run-invoke-job-body!` and the
  wrapper (`http.clj:4446-4467`) refuses a job that reached a terminal state
  while it sat in the queue.

**Live negative control (acceptance clause 2).** Both jobs to codex-17,
2026-09-05 22:17Z, against the reloaded master JVM:

| job | role | state before cancel | state after |
|---|---|---|---|
| `invoke-1788646659258-8057-d977ca80` | J1, running (`sleep 150`) | `running` | `done`, result `U65 NC1 SURVIVED` |
| `invoke-1788646665897-8058-a646e343` | J2, queued duplicate | `queued` | `cancelled`, never run |

The cancel of J2 answered:

    "process-interrupted": false,
    "preserved-job-ids": ["invoke-1788646659258-8057-d977ca80"],
    "process-interrupt": {"action": "refused-not-the-executing-job",
      "message": "Refusing to interrupt codex-17: it is executing
                  invoke-1788646659258-8057-d977ca80, not
                  invoke-1788646665897-8058-a646e343. The named job was ended
                  in the ledger; no process was killed."}

codex-17's status stayed `invoking` across the cancel, the codex process tree
(pids 1732039/1732046) was still alive on every poll, and J1 ran to `done`.
J2 produced no result and never emitted the `U65 NC2 SHOULD NOT HAVE RUN`
line its prompt would have made it print — the resurrection path is shut.

**Unit controls** (`test/futon3c/transport/job_timeout_test.clj`): three added
— `cancel-of-a-queued-job-spares-the-running-job` (asserts, via a redef of
`interrupt-agent-process-tree!`, that NO interrupt is issued and the seat stays
`:invoking`), `cancel-of-the-running-job-still-kills-its-process-tree` (the
authorised case still works — the fix must not be a blanket refusal), and
`a-cancelled-queued-job-is-not-run-when-the-queue-reaches-it`.

## Acceptance clause 3: the borrowed-id pattern

`grep -rn -- "--from" futon2/holes/labs/wm-contract/*.sh worklist-prompt.md`
returns only `--from wm-build-loop` (`wm-build-loop.sh:47`,
`wm-build-watch.sh:6`) and `--from wm-build-work` (`worklist-prompt.md:34`) —
both now registered ids — plus the three comment lines that describe the
incident. The only remaining `--from claude-*` strings under
`holes/labs/wm-contract/` are inside
`claude-15-repl-buffer-snapshot-2026-08-31.txt`, an archived REPL transcript in
which claude-15 used its OWN id. Nothing in the loop borrows an owner id.

## Gates

- clj-kondo: 0 errors, 0 warnings on `http.clj` and `job_timeout_test.clj`
  (one pre-existing `info` at `http.clj:7285`, not in the diff).
- `futon4/dev/check-parens.el`: OK on both.
- Tests: `job-timeout-test` 23 tests / 70 assertions, 8 failures — the SAME 8
  in the same 4 tests (`job-past-cap-becomes-overrun`,
  `overrun-ceiling-finalizes-timeout-and-bells-once`,
  `overrun-late-result-finalizes-done-and-bells-once`,
  `overrun-turn-without-ceiling-still-finalizes-done`) on a stashed baseline,
  so they predate this diff. Run alone and compared before/after:
  `invoke-ledger-atomicity-test` 0F/0E both; `auto-bellback-test` 20F both;
  `http-test` 38F/3E both. (Running the three together is nondeterministic —
  141 failure lines one run, 47 another, on unmodified code — because they
  share on-disk state; that is a pre-existing property of the suite, not a
  finding of this row.)
- `negative_controls.sh`, `pointer_check.bb`: see the row's `:evidence`.
- `gen_aif_dag.bb` NOT run into a publish (TN §9a). No ruling written: neither
  `aif-equations.edn :choices` nor `control-map-edges.edn :decisions` touched.

## One repair outside the row's own work, stated plainly

`negative_controls.sh` section 10j was FAILING before this row started, and for
a reason unrelated to it: the pin reads "171 grandfathered at e8b89211, 5 under
jurisdiction" and the board now has SEVEN (`:C27 :C26 :F8 :F9 :U64 :RUN13
:U65`). `:RUN13` and `:U65` were minted at futon2 `d94cf594` without re-pinning
the p4ng control, whose own comment says "Re-pin when rows are minted"
(`negative_controls.sh:1321-1323`). The gate itself passes on the board — 0
refusals — so only the pinned count was stale. Re-pinned 5 → 7. This row mints
nothing, so the count does not move again.

## Not done, stated

- `zaif-build-loop.sh:36` still bells `--from zaif-build-loop`, an id that is
  NOT registered. Same class of defect, different lab; U65 scopes itself to the
  wm loop ("wm-build-work at minimum"), so it is left alone and named here.
- `futon3c.dev`'s invoke control still records no job-id. The job-grain
  decision is therefore taken in `http.clj` against the invoke-jobs ledger,
  which is the authority the cancel already writes to. Threading a job-id
  through the three `register-invoke-control!` call sites
  (`dev.clj:3662,3912,4586`) would let the control itself refuse a mismatch;
  that is a larger change and was not taken.
- No futon2 `src/` change, so no futon2 test run is claimed. No tick, no run
  lock, nothing under `data/`.
