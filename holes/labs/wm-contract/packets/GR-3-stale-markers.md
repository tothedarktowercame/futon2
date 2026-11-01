# GR-3 — Giraffe: surface stale-status markers for operator disposition (claude-2 → codex-10, 2026-11-01)

Authority: Joe, 2026-11-01 ~21:10Z (field confusion is the priority). Repo futon3c (`master`;
worktree `~/worktrees/futon3c/codex10-gr-3`, branch `codex10/gr-3`; never check out a branch in
`/home/joe/code/futon3c`). Mode: work. One behaviour. Mission close stays Joe's call: nothing here
changes status or excludes a task from the field.

## The defect

Giraffe reads `**Status:**` from the header only. In pass `20261101T195517.913Z-c38bc009`, 606 open
files: 4 carry a close-out marker in the body under an OPEN header (`M-interim-director`:
"READY-TO-CLOSE 2026-05-19"; `M-interim-director-proxy-metric-inventory`; `M-vsatarcs-writer`;
`E-mission-mining`), 3 have an expired timebox (`**Timebox:** 2 weeks maximum from 2026-05-14`
and the like). Two of the four sit at ranks 1 and 2 of the field.

## Goal

Each item gets `:stale-markers` (vector, possibly empty) built from the pinned file: `{:kind
:body-closeout :line n :quote "…"}` for `READY-TO-CLOSE`, a `**Status**` line reading CLOSED/DONE in
the body, or a `## … Close-out` heading; `{:kind :timebox-expired :date … :weeks n :expired-on …}`
when `**Date:**` and `**Timebox:**` parse and the window has passed as of `:as-of`. `report.md` gains
a section "Needs operator disposition" listing these items with rank, status, markers and quotes.
`pass.edn` carries the markers on `:order` rows. No status change, no exclusion.

## Tests (live pins from this pass)

1. `M-interim-director` → `:body-closeout` (quote contains "READY-TO-CLOSE") and
   `:timebox-expired` (2026-05-14 + 2 weeks).
2. A file with neither → `[]`.
3. The report section lists exactly the marked items in rank order.

Gates as GR-1. Bell claude-2 with sha, receipt, and the disposition list.
