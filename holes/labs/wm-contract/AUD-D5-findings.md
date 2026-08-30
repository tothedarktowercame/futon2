# AUD-D5 findings

- `observed` Script contract: `mana-snapshot.bb` is invoked as `bb scripts/mana-snapshot.bb [--out PATH]`; `--out` is optional and defaults to `/home/joe/code/storage/futon0/mana-snapshot.json` ([/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:5), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:32), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:210)).
- `observed` Script cwd/dependency basis: the script resolves its manifest from `*file*`, so no repo-relative argv is required; I set `WorkingDirectory=/home/joe/code/futon0` for operator clarity. HTTP reads from futon3c/nonstarter are optional and degrade to `[]`/`nil` rather than failing the run ([/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:28), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:107), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:118), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:133), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:139)).
- `observed` Cadence rationale: `E-wm-staleness-meta-stop.md` requires `stat ~/code/storage/futon0/mana-snapshot.json` to show mtime `< 10 minutes ago` and `systemctl --user status mana-snapshot.timer` active at inspection, so a 5-minute user timer is the smallest straightforward cadence with headroom under that gate ([/home/joe/code/futon3c/holes/missions/E-wm-staleness-meta-stop.md](/home/joe/code/futon3c/holes/missions/E-wm-staleness-meta-stop.md:84)).
- `observed` WM freshness consumption answer: the WM reader does consume age, not just content. It computes `:snapshot-age-minutes`, sets `:stale? (> age-min 60.0)`, and renders `Snapshot: <age> min ago` with `⚠ stale` when applicable ([/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2568), [/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2572), [/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2615), [/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2617), [/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2998), [/home/joe/code/futon2/scripts/futon2/report/war_machine.clj](/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:3002)).

## Installed units

`observed` `~/.config/systemd/user/mana-snapshot.service`

```ini
[Unit]
Description=Refresh mana snapshot for War Machine metabolic-balance input

[Service]
Type=oneshot
WorkingDirectory=/home/joe/code/futon0
ExecStart=/usr/bin/env bb /home/joe/code/futon0/scripts/mana-snapshot.bb
```

`observed` `~/.config/systemd/user/mana-snapshot.timer`

```ini
[Unit]
Description=Refresh mana snapshot on a bounded freshness cadence

[Timer]
OnBootSec=2min
OnUnitActiveSec=5min
Persistent=true
Unit=mana-snapshot.service

[Install]
WantedBy=timers.target
```

## Verification

- `observed` Before install/run, `stat -c '%y' /home/joe/code/storage/futon0/mana-snapshot.json` reported `2026-08-12 12:07:42.834125812 +0000`.
- `observed` `systemctl --user status mana-snapshot.timer --no-pager` before install reported `Unit mana-snapshot.timer could not be found.`
- `observed` After `systemctl --user daemon-reload && systemctl --user enable --now mana-snapshot.timer && systemctl --user start mana-snapshot.service`, `systemctl --user list-timers --all --no-pager | rg 'mana-snapshot\.timer'` reported:

```text
Sun 2026-08-30 19:18:47 UTC 3min 50s Sun 2026-08-30 19:13:47 UTC 1min 9s ago mana-snapshot.timer             mana-snapshot.service
```

- `observed` `systemctl --user status mana-snapshot.service --no-pager -n 20` reported successful execution with `ExecStart=/usr/bin/env bb /home/joe/code/futon0/scripts/mana-snapshot.bb (code=exited, status=0/SUCCESS)` and stdout `wrote /home/joe/code/storage/futon0/mana-snapshot.json — max-tier=silent P=0.75 across 17 repos`.
- `observed` After the run, `stat -c '%y' /home/joe/code/storage/futon0/mana-snapshot.json` reported `2026-08-30 19:13:47.722591056 +0000`.
- `observed` Parse check: `jq -r '.\"generated-at\", (.\"per-repo\"|length), (.sessions|length), .\"max-tier\", (.\"max-pressure\"|tostring)' /home/joe/code/storage/futon0/mana-snapshot.json` returned:

```text
2026-08-30T19:13:47.722840193Z
17
165
silent
0.75
```

- `observed` Freshness basis after the successful run: JSON `:generated-at` is `2026-08-30T19:13:47.722840193Z`, and filesystem mtime is `2026-08-30 19:13:47.722591056 +0000`.

## Outcome

- `observed` Acceptance met on August 30, 2026: the user timer exists, is enabled, has a next trigger, the service ran once successfully, the JSON mtime moved to today, and the content parses.
- `observed` No refusal: the script completed against live dependencies as written because its remote reads are explicitly optional ([/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:107), [/home/joe/code/futon0/scripts/mana-snapshot.bb](/home/joe/code/futon0/scripts/mana-snapshot.bb:119)).
