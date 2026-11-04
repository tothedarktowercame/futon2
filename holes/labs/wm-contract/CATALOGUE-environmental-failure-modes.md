# Catalogue: environmental failure modes of the War Machine

Started 2026-11-04 by claude-6, from AUDIT-1 (`~/notes/wm-next/repair-ticket-audit-2026-11-04/`,
zai-1). Each row was checked against its ticket and finding.

**What this is.** Joe, 2026-11-04: the environmental subclasses "are more like a catalogue of errors
or failure modes than they are 'tickets' to keep open until they are closed. Broadly it would be nice
to have assurances that they don't / won't recur, but this is detailed debugging work, rather than
obvious items to put in front of the machine as self-heal candidates."

So each entry records a failure mode, when it happened, and the tickets that recorded it. It also
records what we know about whether it can recur:
- **guard**: something now checks for the condition before it can fail a click, with evidence;
- **no guard known**: debugging work, for a person or seat to take up deliberately, not for the
  machine to select.

To check whether a mode has recurred, look for a repair finding with the same failure title and
stage opened after the date of the last occurrence:
`grep -l '<title>' futon2/holes/tickets/T-repair-*` and compare `git log --diff-filter=A` dates.

| Mode (title @ stage) | Occurrences | What happened | Guard status |
|---|---|---|---|
| agent unavailable @ :agent-readiness, "Configured author is unavailable" | 5: 09-23, 10-01, 10-12, 10-15, 10-16 | The author seat was not reachable when the click reached agent readiness. | Partial guard: the launch preflight now checks the cast before firing. Every click log prints `casting wm-author / wm-reviewer / wm-repair-reviewer idle and invoke-ready` (e.g. `futon2/data/wm-click-loop/click-001-1793815472.log`). A seat that drops mid-click is not covered. |
| agent unavailable @ :agent-readiness, "Selected an action, but no cast was given for it" | 3: 09-26, 10-01, 10-01 | The click was launched without a cast. This is a launch-configuration mistake, not the environment. | Guard: `wm_click_repair_loop.py` requires `--author/--reviewer/--repair-reviewer`, and its stop notice says "no cast identity has a default". |
| transport timeout @ :author-wait, "request timed out" | 1: 09-29 | The author's reply did not arrive in time. | No guard known. |
| transport timeout @ :construction, "substrate-2 mission registry unreachable" | 1: 09-30 | The mission registry on substrate-2 (:7071) did not answer during construction. | No guard known. |
| source unavailable @ :close, "Attempt evidence source unavailable" | 1: 10-02 | The attempt's evidence could not be read at close. | No guard known. |
| wm job ledger unavailable @ :ticket-readiness-refresh | 1: 10-27 | WM dispatch lacked its run-local job ledger context. | No guard known; probably wiring, not environment. |
| dispatch failed @ :ticket-readiness-refresh, "Agency dispatch failed" | 1: 10-29 | An Agency dispatch failed during the ticket readiness refresh. | No guard known. |
| hot load failed @ :cascade-resume, "Selected cascade held in open-cascade phase" | 2: 10-29 (two attempts) | Hot-loading the repaired code before resuming a held cascade failed. | No guard known. This looks more like a code defect in the resume path than the environment. |

Tickets (all OPEN as of 2026-11-04; ids are `T-repair-<id>` in `futon2/holes/tickets/`):
- occ-2ce2238e…, occ-2aee52c7…, occ-d4d69c06…, occ-e5c66a19…, occ-140c55d9…, occ-0228a166…,
  occ-11f5a87b…, occ-836a86b2… (agent unavailable, including the no-cast ones)
- occ-d7b9455b… and occ-6005871b… (transport timeout)
- occ-a3438ff0… (source unavailable)
- occ-f291316c… (job ledger)
- occ-c15da9b6… (dispatch failed)
- ea1-a6cf24bb…--attempt-001-hot-load-failed and ea1-60b8bfca…--attempt-001-hot-load-failed

No mode in this table has an OPEN ticket dated after 2026-10-29.
