# E-kimi-task-54 — Fix futon6 publish-efe-field.sh: pattern-density report times out at 90 s (futon6)

**Requisition:** completed — 2026-09-26T13:03:33Z, job invoke-1790426510196-24829-6e6800cd, state done

Clocked in by claude-12 for kimi-1 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet — make `futon6/scripts/publish-efe-field.sh` complete end to end (futon6)

Joe wants to see his EFE diagram at
`https://zone.hyperreal.enterprises/wip/mission-efe-field.html`
(file `/var/www/zone.hyperreal.enterprises/wip/mission-efe-field.html`, last good 04:34
2026-09-26). Reviewer: claude-12.

## The failure (claude-12, measured 2026-09-26 ~12:00Z)

- `scripts/publish-efe-field.sh` exited 1 after 90 s at its first step:
  `refusing to overwrite: only 0 patterns scraped (JVM down or report shape changed?)`.
- Cause: `scripts/refresh_pattern_attestation.sh` runs
  `bb --classpath scripts -m futon0.report.pattern-density 60 5000` under
  `timeout ${PATTERN_DENSITY_TIMEOUT:-90}` (futon6 87aaa61). Run by hand it takes
  **174 s** and prints 1042 table rows; the timeout kills it and the scrape sees nothing.
  The guard message then blames the JVM, which was healthy.
- A rerun with `PATTERN_DENSITY_TIMEOUT=400` got past that step (still in progress when
  this packet was written; see `/tmp/claude12-efe-publish.log`).

## Build

1. Find out why the report takes 174 s (it reads `GET :7070/api/alpha/evidence`; see
   `futon0/scripts/futon0/report/pattern_density.clj`). Measure where the time goes.
   If a small fix makes it fast (e.g. a server-side filter or page size the API already
   supports), make it; otherwise say what it would take and don't start a large change.
2. Whatever the report's speed, the publish must not fail on it: set the default timeout
   from your measurement with headroom, and make the guard's message say when the
   report timed out (exit 124) rather than blaming the JVM.
3. Run `scripts/publish-efe-field.sh` end to end once and confirm the page file's mtime and
   size changed. Report the total time and each step's time.

## Constraints

- futon6 master (and futon0 if the report changes); explicit-path commits; no stash,
  amend, branch switch.
- `futon6/scripts/mission_efe_scope_dump.py` has someone else's uncommitted hunk
  (include-total on the first page) that the publish currently needs. Do not commit,
  revert or edit it. Leave all other dirty files alone.
- Do not restart futon3c or futon1b. Coordinate nothing with kimi-8 (busy on futon1b).
- Gates: `bash -n` on edited shell; clj-kondo + `futon4/dev/check-parens.el` on any Clojure.

## Reply

Bell claude-12 back with shas, the timing breakdown, and the published file's new mtime.
