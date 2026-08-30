# AUD-D5 — the mana-snapshot timer exists (close AUD-D1's class-(ii) row for mana)

Owner: claude-15. Builder: codex-5 (AUD lane). Mode: work. One unit pair + one findings note. No report-code changes.

## Context
AUD-D1 (your table): `~/code/storage/futon0/mana-snapshot.json` is a class-(ii) snapshot read by the WM report,
18+ days stale, produced by `futon0/scripts/mana-snapshot.bb` (last commit f6ae9cca) — and
`futon3c/holes/missions/E-wm-staleness-meta-stop.md:85` expects `systemctl --user status mana-snapshot.timer`,
but no such unit exists. Joe's rule: where no live endpoint exists, the deliverable is the endpoint (here: the timer
that keeps the snapshot current, plus freshness surfaced to the reader).

## Goal
1. `~/.config/systemd/user/mana-snapshot.service` + `mana-snapshot.timer`: run `bb /home/joe/code/futon0/scripts/mana-snapshot.bb`
   (confirm the script's expected argv/cwd by READING it first; state what it needs) on a cadence matching how the
   data changes — read the script and E-wm-staleness-meta-stop.md:80-95 and choose (likely daily or a few hours);
   say why in the findings. `WantedBy=timers.target`, `Persistent=true`.
2. `systemctl --user daemon-reload && systemctl --user enable --now mana-snapshot.timer`; verify with
   `systemctl --user list-timers | grep mana` and one manual `systemctl --user start mana-snapshot.service` run;
   confirm the json's mtime moved and the content parses (`bb -e '(read-string ...)'` or jq per format).
3. The WM reader (`war_machine.clj` mark2/mana region) already records reads via record-input-read!; do NOT edit it.
   Instead state in the findings: the snapshot's `:as-of`/mtime after your run, and whether the report's mana section
   consumes a freshness field (file:line) or ignores age (if it ignores age, say so as a finding — a later packet
   may add a staleness marker; not yours).
4. Unit files are OUTSIDE the repos: copy both unit files' full text into the findings note
   `futon2/holes/labs/wm-contract/AUD-D5-findings.md` (that note IS the versioned record), plus the verification
   outputs. Commit only the findings note.

## Acceptance (row-11)
- `systemctl --user list-timers` shows mana-snapshot with a NEXT time; the service ran once successfully (exit 0);
  the json is fresh (mtime today) and parses.
- Findings note contains: unit texts, cadence rationale, script argv findings, freshness-consumption answer.
- Refuse (with reason) if the script fails on a real dependency (e.g. needs a host that is down) — do not fake a run.

## Report
Bell claude-15 back with: findings-note sha, `list-timers` line, the json mtime before/after, refusals.
