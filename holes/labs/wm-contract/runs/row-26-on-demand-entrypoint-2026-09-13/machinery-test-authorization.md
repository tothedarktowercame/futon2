# Machinery-test run authorization — claude-15, 2026-09-13

Authority chain:
1. Joe (operator, emacs-repl, 2026-09-13): leadership returned to claude-15;
   "we need the minimum of glue code to make the system run"; finish the
   commissioned build.
2. RULINGS-walkthrough-2026-09-12.md Item 5 Operationalized 1 and 3: runs
   during the build are machinery tests recorded as such; the on-demand
   outer-loop entry point is mandated. Settlement recorded at 1d84fd66.
3. Entrypoint review (claude-15, this file's commit): futon3c 51269db9 and
   futon2 402632e6 diffs read in full; tests independently re-run
   (2 tests / 7 assertions, 0 failures; clj-kondo 0/0); POST and GET
   /api/alpha/wm/click verified present on futon3c master at lines
   8956/8962; serving JVM reloaded from master via restore-http-routes.sh
   (probe 200) so the :run-id passthrough is live.

This run is a BUILD-PHASE MACHINERY TEST. It is not qualifying, not a
candidate, not offered for acceptance, and closes no row by itself. Its
records are evidence CANDIDATES for rows 13/14/15/22/23 packets.

Known non-qualifying limitations, declared up front, not invented away:
- FUTON_WM_FPI_DARK / FUTON_WM_FPI_POSTERIOR are not set in the serving
  JVM environment (no restart performed; restarts are Joe's call), so
  F_pi coverage in this run's records is whatever production computes
  without them.
- Effective horizon is whatever production resolves; if it resolves nil
  the R13->R4 depth arm stays unexercised in this run.
Both are recorded limitations of THIS machinery test, per the settlement
("missing effective configuration is a refusal or explicit non-qualifying
limitation, not an invented setting").

Roles: author claude-15 (operator client caller), reviewer codex-22,
repair-reviewer codex-24 — distinct identities per the client's own
validation.
