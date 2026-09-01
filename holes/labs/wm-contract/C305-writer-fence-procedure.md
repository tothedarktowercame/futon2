# C305 — writer fence for the quiet operator run

Date: 2026-09-01. Owner: `wm-organization`.

This is the procedural authority behind C292's composite quiescence check. It
does not claim an atomic snapshot across Git, the lane registry/Agency, and
systemd. The coordinator may declare the fence only after every writer below
has either acknowledged suspension or supplied an observable exclusion. The
fence remains in force through reload, click, and certification.

## Writer census and acknowledgements

Record the acknowledgements in the operator log with a UTC timestamp. Do not
write that log into any repository being certified.

| Writer class | Required acknowledgement or observation |
|---|---|
| Four delivery lanes | `wm-nouns`, `wm-verbs`, `wm-organization`, and `wm-evidence` each explicitly acknowledge `NO-WRITE/NO-JOB UNTIL FENCE-RELEASE`; each lane-registry row is `:idle`. Idle alone is insufficient because it is liveness, not a promise not to resume. |
| Coordinator / APM | `claude-20` records `DISPATCH-FROZEN`, sends no new work, and owns the eventual `FENCE-RELEASE`. Any campaign babysitter or APM process must be stopped by its owner or shown not to dispatch/write into the five repositories. |
| Operator | Joe acknowledges that editor/shell activity will not modify the five repositories; only the approved reload and click are allowed after READY. |
| Paper publisher | `claude-1` acknowledges that publication and live-artifact generators are paused until release. A clean p4ng tree is not a promise that another publication will not start. |
| Other agents/sessions | Compare the live roster with the coordinator's dispatch ledger. Every session retaining workspace write authority must acknowledge the fence or be ended. An unregistered/unknown writer makes the fence unavailable. |
| Ordinary and bounded jobs | `bg.py list` and `bg.py test-list` show no active/activating job twice, once before and once after repository sampling. Let jobs finish; do not kill them merely to obtain a clean observation. |
| Timers, services, watchers | Inventory user timers, running services, path units, and writer-like processes. Each receives an operator-recorded disposition: `inactive`, `paused by owner`, or `write-set-excludes-certified-repositories` with the write target named. An unknown write set is a STOP. |
| Build/generator commands | None may run independently. Commands in the checklist are allowed only in their named step, and their tracked outputs must leave the sampled basis unchanged. |

The current substrate census must be refreshed at the window; on 2026-09-01 it
included `mana-snapshot`, Futon1b metaspace sampling, APM watchdog and axiom
audit, and Futon pattern-index timers; several `apm-watch.sh` processes; an
APM campaign babysitter; and the Futon1b/Futon3c serving services. There were
no user path units. This list is evidence of what to inspect, not a permanent
claim that those processes write (or do not write) the repositories.
The executable-by-executable write-set findings and owner dispositions are in
`C309-scheduled-writer-census.md`.

Use these commands for the machine-side census:

```sh
systemctl --user list-timers --all --no-pager
systemctl --user list-units --type=path --all --no-pager
systemctl --user list-units --type=service --state=running --no-pager
ps -eo pid,ppid,lstart,args | rg 'watch|generator|publish|apm|bg.py'
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list
```

## Fence checkpoints

1. Collect every acknowledgement above and record `FENCE-HELD` with UTC time.
2. Run `python3 checks/quiescence_check.py`. Require `QUIESCENT`; preserve its
   start/finish observations as the initial basis.
3. Re-run the same check immediately before the bounded workspace gate, after
   the gate, after both suite receipts are terminal, immediately before Joe's
   reload, after reload/readiness, and immediately before the observer/click.
   The probe costs under one second in the C290 measurement. Never run it while
   a deliberately bounded suite is active.
4. At every checkpoint also confirm that the acknowledgement roster is still
   complete and the coordinator has not dispatched. Machine observations do
   not substitute for the human promises.
5. A changed/unavailable C292 basis, a new active job, a resumed lane, a new
   writer process with an unknown write set, or a missing acknowledgement is
   `FENCE-BREACH`. Close the window immediately.

After the pre-click checkpoint, the operator click is an **authorised write
phase**, not global quiescence. Only the exact click binding, topology-bearing
`TickRunRecord`, trace/store output, external observer receipt, and final
certificate named by that click/run identity are allowed. Compare repository
status with the pre-click basis and name every changed path; any other tracked
or untracked output is a breach. Do not rerun a clean-tree probe and mislabel
the authorised evidence as failure.

## Restart semantics

- **Before reload:** retain failed receipts as evidence, return to drain step
  1, reacquire every acknowledgement, and rerun gate, both suites, preflight,
  and readiness. No earlier passing receipt crosses the breach.
- **After reload but before click:** restart the same verification. The reload
  may be reused only if its recorded Futon2 runner identity still exactly
  matches the newly tested settled commit. If Futon2 or the runner source
  changed, Joe must reload again after a new passing preflight. A change only
  in another repository does not erase the loaded identity, but it still
  invalidates workspace readiness and requires the gate/suite sequence again.
- **During or after click:** do not click again. Preserve the run record,
  binding, envelope, and logs; stop and classify whether the breach preceded,
  overlapped, or followed their timestamps. Certification may proceed only if
  its exact pinned inputs independently establish the claim; otherwise the
  outcome is unavailable/fail, never reconstructed from a later clean state.
- Release with `FENCE-RELEASE` only after the certificate and its evidence are
  saved, or after an aborted run has been explicitly preserved and closed.

## ABA limit

Two equal observations still cannot prove that no write-and-restore occurred.
The fence makes ABA operationally uninteresting only conditionally: no
acknowledged writer is permitted to act, so an ABA would itself be a fence
breach by an unacknowledged writer. The detector has not gained a monotonic
cross-authority token and must not claim otherwise. An Agency revision token
would close only Agency-local ABA; it cannot version Git, the registry file,
or systemd, so the operator run need not wait for it.
