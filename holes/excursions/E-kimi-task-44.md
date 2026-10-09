# E-kimi-task-44 — Tornhill packet 1b: EFE code ring reads the Tornhill report (futon6)

**Requisition:** completed — 2026-09-26T03:09:09Z, job invoke-1790391593644-24660-21624235, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition completed, job state done. _(WM status classification by zai-3, high confidence; not yet confirmed by the author.)_

Clocked in by claude-12 for kimi-3 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet 1b — the EFE code ring reads the Tornhill report (futon6)

Source: futon3c/holes/HANDOFF-tornhill-next-2026-09-26.md, packet 1b. Mission:
futon3c/holes/missions/M-the-perfect-crime.md. Reviewer: claude-12. Packet 1a is done
(futon6 0a98504): each mission row in `data/mission-activity.json` now has
`code.files: [[repo, relpath], ...]`.

## Goal

The pink "code churn" ring on the EFE field page (`scripts/mission_efe_field.py`,
`code_churn_ring`, around line 242) now shows commits counted by mission_activity.py's own
git pass. Make it show the file-level Tornhill report instead: for each mission, look up
its `code.files` in the report.

## Inputs

- `data/mission-activity.json` (fresh; regenerate with
  `python3 scripts/mission_activity.py --no-v05`, about 40 s. Do NOT run it without
  `--no-v05`: that pulls ~550k hyperedges from futon1b and takes hours. A background run
  of it is already going; leave it alone).
- The Tornhill report: newest `tornhill-YYYY-MM-DD.json` in
  `Path.home() / ".local/share/futon-audits/tornhill"`, overridable with
  `futon6_config.path("FUTON6_TORNHILL_REPORT", <default>)`. Shape:
  `report["generated"]`, `report["window_days"]`, and
  `report["repos"][repo]["files"]` = list of
  `{path, revs, hotspot, born_in_window, trend_ratio (optional), ...}`. Only code files
  changed in the window appear.
- Optional: `tornhill-chat-YYYY-MM-DD.json` in the same directory: `files` = list of
  `{repo, path, seats: {seat: n}, ...}`.

## What to build

1. A new module `scripts/efe_tornhill_ring.py`, standard library only, with pure
   functions (no file reads at import):
   - `load_report(dir_or_path)` → report dict, or None when absent.
   - `index(report, chat=None)` → `{(repo, path): file_row}` (with a seat set from chat
     when given).
   - `mission_ring(mission_row, idx)` → a dict with `state` one of
     `"no-report" | "no-link" | "unmeasured" | "measured"`, and for measured:
     `revs` (sum), `hotspot` (sum), `top` (up to 3 files by hotspot, each with its trend:
     `trend_ratio`, or `"new"` if `born_in_window`), `n_files_in_report`,
     `n_files` (len of code.files), and `seats` (distinct count, or None without chat).
     "no-link": the row has no `code` or empty `code.files`. "unmeasured": files exist
     but none is in the report.
   - `ring_svg(x, y, r, mission, ring, report_meta, hotspot_max)` → the SVG string.
     Thickness proportional to log1p(hotspot) / log1p(hotspot_max). The `<title>` lists
     the top files with revs, hotspot and trend, the summed revs, "N of M files in the
     report", the seat count when present, and the report filename + its `generated`.
2. In `mission_efe_field.py`, replace `code_churn_ring`'s body with calls into the module.
   States drawn:
   - no report: draw no code rings at all, and put one legend line
     "No Tornhill report — code ring not drawn". Never fall back to the old
     commits_90d computation.
   - no-link: the existing faint dotted grey ring.
   - unmeasured: a dashed pink ring, with a hover saying none of its files changed in the
     report's window (or are not code).
   - measured: solid pink ring.
   Update the page's legend/header text about the code ring so it names the Tornhill
   report and no longer says complexity is pending, and the comment block above the
   function (keep a one-line warrant noting this packet).

## Constraints

- Work on master in /home/joe/code/futon6. Commit with explicit paths only
  (`git commit -- <paths>`). No stash, no amend, no branch switch.
- Unrelated uncommitted files exist (mission_scope_detect.py, mission_efe_scope_dump.py,
  data/mission-wholeness.edn, tests/test_mission_scope_detect.py, render_scope_view.py,
  scope_view/). Leave them alone.
- No host paths in code (`git grep -n '/home/joe' -- scripts tests` no new hits).
- `data/` is gitignored; do not commit it. Do not publish the page.

## Acceptance

1. `tests/test_efe_tornhill_ring.py` (unittest, same importlib pattern as
   `tests/test_mission_activity_files.py`), on a two-file fixture report built in the
   test: one test per state (`no-report`, `no-link`, `unmeasured`, `measured`) asserting
   the state and, for measured, the summed revs and hotspot and the "new" trend marker;
   and `ring_svg` output for measured contains the report filename.
2. Construct the bad case: temporarily make `mission_ring` return "measured" with zeros
   for a mission whose files are not in the report; confirm a test fails; restore. Say
   so in your reply.
3. Regenerate the page (`python3 scripts/mission_efe_field.py`), then write
   `data/mission-efe-tornhill.check.txt`: for 3 measured missions, the hover's summed
   revs versus a sum computed separately from the JSON; for 1 of them, `git log
   --since=90.days --no-merges --full-history --format=%H -- <file> | wc -l` per file in
   its repo against the report's `revs` (small differences from the report's generation
   time are expected; state them). Quote the counts of districts in each state.
4. The page's JS still parses: extract the inline `<script>` and run `node --check` on it.
5. Update the "Next" list in the mission's "Checkpoint 2026-09-26" section (futon3c) to
   say the ring now reads the report, in its own commit in futon3c.

## Reply

Bell claude-12 back with a summary and the commit shas: the state counts, the check-file
result, the test result line, and the bad case you tried.
