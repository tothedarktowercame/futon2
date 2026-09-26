# E-kimi-task-45 — EFE carpet global controls: band floor, status filter, layer toggles (futon6)

**Requisition:** completed — 2026-09-26T03:37:29Z, job invoke-1790393400027-24673-cc380f3a, state done

Clocked in by claude-12 for kimi-4 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet — global controls for the EFE carpet (futon6)

Mission: futon3c/holes/missions/M-the-perfect-crime.md (the EFE field page is its view).
Reviewer: claude-12. Asked for by Joe, 2026-09-26: "a global controller for the carpet
that would turn off N of the level sets, or provide other global controls … quite a lot
of missions are in level set 0 which may be basically useless for me now".

## Goal

Add a small control panel to the EFE field page (`scripts/mission_efe_field.py`, which
writes `data/mission-efe-field.html`) that hides or shows missions and layers in the
browser, with no page reload and no server.

## Facts you need (measured by claude-12 at futon6 174a456)

- Each mission is a hub `(x, y, m, n)` in `hubs`. Its level-set band is the band of the
  metric field at the hub's grid cell:
  `band = min(NB-1, int(grid[round(y/STEP)][round(x/STEP)] / fmax * NB))`, NB = 7.
  Today: band 0 has 153 of 324 hubs, band 1 91, band 2 46, bands 3–6 34.
- A mission's status text is `ACT[m]["status_line"]` (from mission-activity.json); it is
  free text or None.
- Per-mission SVG pieces are drawn in separate strings: `hub_svg` (~line 331),
  `activity_svg` (mission-doc ring, `churn_ring`), `code_churn_svg` (code ring,
  `code_churn_ring`), `scope_svg` (scope dots; `scope_pts` tuples do not currently carry
  the mission name, so add it), and `hubline_svg`. Stars and the live overlay are drawn
  elsewhere.

## What to build

1. Two pure functions in a new module `scripts/efe_carpet_controls.py` (standard library,
   no reads at import):
   - `hub_band(grid, fmax, nb, step, x, y) -> int`.
   - `status_class(status_line) -> "done" | "open" | "unknown"`: "done" when the text
     (case-insensitive) starts with or contains, as a word, archived, complete,
     completed, superseded, closed, retired; "unknown" for None/empty; else "open".
     Keep the word list as a module constant with a comment.
2. In `mission_efe_field.py`, every per-mission element above (hub, both rings, the
   mission's scope dots and hub lines) carries `data-m="<mission>"`,
   `data-band="<0..6>"` and `data-status="<done|open|unknown>"`. Put the attributes on a
   per-mission wrapping `<g>` where the draw order allows it, otherwise on each element.
   Do not change draw order.
3. A fixed control panel (top-left corner, not covering the header text) with:
   - **Band floor**: a range input 0..6, "hide missions below band N", showing the count
     of missions shown/hidden;
   - **Status**: checkboxes for done / open / unknown (all checked by default), each
     with its count;
   - **Layers**: checkboxes for the mission-doc ring, the code ring, scope dots, and the
     momentum lasso.
   Hidden means `display:none`. Defaults show everything, so the page looks as it does
   today until someone touches a control.
4. A legend line naming what the band measures: "band = density of scopes around the
   mission (its scopes weighted by determined / frontier / vacuous, blurred with its
   neighbours) — not a judgement of value".

## Constraints

- Work on master in /home/joe/code/futon6; commit with explicit paths only
  (`git commit -- <paths>`); no stash, amend or branch switch.
- Leave the unrelated uncommitted files alone (mission_scope_detect.py,
  mission_efe_scope_dump.py, data/mission-wholeness.edn, tests/test_mission_scope_detect.py,
  render_scope_view.py, scope_view/). `data/` is not committed.
- Do not run `scripts/mission_activity.py` without `--no-v05` (that pulls futon1b for
  hours; a background run of it is going — leave it alone).
- No host paths in code. Do not publish the page.
- Inline JS: every string literal must survive Python f-string rendering (a literal
  newline inside a JS string broke the page on 2026-09-25). Build the page and run
  `node --check` on the extracted inline scripts.

## Acceptance

1. `tests/test_efe_carpet_controls.py` (unittest, importlib pattern as in
   `tests/test_efe_tornhill_ring.py`): `hub_band` on a small synthetic grid (a point at
   the max is band NB-1, a point at 0 is band 0, a boundary value); `status_class` on
   "archived", "COMPLETE (SUPERSEDED by …)", "IDENTIFY (2026-04-…)", None, and a status
   that merely mentions "incomplete" (must be "open", not "done").
2. Construct the bad case: temporarily make `status_class` match substrings instead of
   words; confirm the "incomplete" test fails; restore. Say so in your reply.
3. Build the page (`python3 scripts/mission_efe_field.py`). Report: the count of
   elements carrying `data-band`, the per-band and per-status mission counts shown in the
   panel, and that every hub has exactly one band. `node --check` passes on the inline
   script.
4. `git grep -n '/home/joe' -- scripts tests` shows no new hits.

## Reply

Bell claude-12 back with a summary and the commit shas: the counts above, the test
result line, and the bad case you tried.
