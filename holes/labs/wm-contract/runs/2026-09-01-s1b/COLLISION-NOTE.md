# S1b boundary note (written 2026-09-01T16:48:01Z)

The per-date trace file data/wm-trace/wm-trace-2026-09-01.edn is shared by every run on the day,
and trace records carry NO run id (receipts do, since RUN10). Before S1b proper, the file holds:
- records 0-19: S1 (15:2x UTC, routeless, sha 5006200);
- records 20-29: claude-20's re-run on 6917ba0, started 16:33 UTC, killed after a collision with
  claude-1's first attempt; its last record is 16:46:14;
- record 30 (if present, ~16:47:05): tick 1 of claude-1's FIRST attempt on 6917ba0, which overlapped
  claude-20's last tick and was therefore discarded (tick 2 killed, exit 143).
S1b proper: every record with :timestamp >= 2026-09-01T16:48:01Z (epoch 1788281281).
Verified before relaunch: zero run-tick-once JVMs alive (pgrep on the java command line).
ticks.csv gives each tick's [start,end] epoch window; RUN3 selects records by this range, not by
"the tail" of the file. Row to open: trace records carry the run id; a run lock so two agents cannot
tick concurrently.
