# Consumable build backlog

Prepared at Joe's request, 2026-09-11. Uses the existing `wm/worklist-v1` consumer, not a new scheduler. Nine unfinished canonical rows and RUN3's completed dependency record are retained with their exact acceptance, status and progress. Only F11 is currently selectable. Held rows remain visible; no canonical ledger status is changed.

From `/home/joe/code`:

```sh
bb futon2/holes/labs/wm-contract/runs/WM-BACKLOG-2026-09-11/refresh.clj
bb futon2/holes/labs/wm-contract/build_step.bb next-open --worklist futon2/holes/labs/wm-contract/runs/WM-BACKLOG-2026-09-11/worklist.edn
bb futon2/holes/labs/wm-contract/build_step.bb next-task --worklist futon2/holes/labs/wm-contract/runs/WM-BACKLOG-2026-09-11/worklist.edn
```

`next-task` returns the full selected F11 row, including original acceptance and accumulated progress, for a build worker. Selection now checks dependencies even for rows labelled open; missing or unfinished dependencies refuse selection. Fundamentals retain existing priority. Refresh before each dispatch, because this is a snapshot rather than authority to overwrite the canonical ledger.

This is a **build-input queue**, not an installed RUN4 manifest queue. RUN4 execution additionally requires mission conversion, frozen pins, actual candidate/admissibility and repair-store checks. It must wait for the current pilot/repairs; this queue spends no capacity. Do not run a second autonomous build loop alongside the active coordinator. Runtime stop-lines remain authoritative before any queued item.

Deferred: F12 requires an executable naturalistic example/interpretation; F10/U88 contextual decisions and current pilot evidence; RUN4/RUN13 accepted-run evidence; U80 depends on F12; U84 requires measured reasoned-record count. U83's acceptance is stale relative to its store and must be restated/retired, not executed as 73 invented pending attempts. See each exact row for its basis.
