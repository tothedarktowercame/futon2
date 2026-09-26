# E-kimi-task-43 — Tornhill packet 1a: mission_activity.py emits each mission's file list (futon6)

**Requisition:** completed — 2026-09-26T02:49:26Z, job invoke-1790387079827-24611-d415a1ad, state cancelled

Clocked in by claude-12 for kimi-3 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet 1a — mission_activity.py emits each mission's resolved file list (futon6)

Source: futon3c/holes/HANDOFF-tornhill-next-2026-09-26.md, packet 1a. Mission:
futon3c/holes/missions/M-the-perfect-crime.md. Reviewer: claude-12.

## Goal

`futon6/data/mission-activity.json` stores only aggregates for each mission's code:
`code.files_resolved` is a count. The EFE page's next change (packet 1b) needs the files
themselves, to look each one up in a per-file Tornhill report. Add the list.

## Files

- `/home/joe/code/futon6/scripts/mission_activity.py`. `parse_edges()` reads mission→var
  `touches` edges from `data/fold-embed/edges.jsonl`; `resolve_var(var_id, ns_index)` maps
  a var to `(repo, relpath)` or None; `main()` builds `mission_vars` / `mission_files` and
  then each mission's `row["code"]` block (around line 470).
- Add `code.files`: a list of `[repo, relpath]` pairs, sorted and deduplicated, for every
  mission with at least one resolved file. Do not change or remove any existing field.
  Where a mission has `code` but no resolved file, use `"files": []`, matching
  `files_resolved == 0`.
- To make it testable without git or network, factor the var→file resolution for one
  mission into a small function (for example
  `mission_code_files(var_ids, ns_index) -> (sorted_files, n_unresolved)`), and have main()
  call it. Keep the existing behaviour identical.

## Constraints

- Work on master in /home/joe/code/futon6. Do not switch branches, stash, or amend.
  Commit with explicit paths only: `git commit -- <paths>`.
- The working tree has unrelated uncommitted changes (mission_scope_detect.py,
  mission_efe_scope_dump.py, data/mission-wholeness.edn, tests/test_mission_scope_detect.py,
  scripts/render_scope_view.py, scripts/scope_view/). Leave them alone and do not commit
  them.
- No host paths in code or tests: no `/home/joe`; resolve roots through `futon6_config`
  as the script already does.
- `data/` is gitignored run output; do not commit it.
- main() now also pulls futon1b `code/v05` hyperedges from 127.0.0.1:7073, caching them
  under /tmp/v05-cache (commits b309478, 1aa1efe). The first full run may take a while.
  That is expected; do not remove or change that code.

## Acceptance

1. Run `python3 scripts/mission_activity.py` once. For 3 missions that have code, show
   that `len(code.files) == code.files_resolved`, and quote the three pairs of numbers
   in your reply.
2. A new test, `tests/test_mission_activity_files.py` (same importlib pattern as
   `tests/test_mission_scope_detect.py`), uses a synthetic edges file or synthetic var
   list plus a synthetic ns_index:
   - two vars in the same file give one entry in `files`;
   - an unresolvable var is absent from `files` and counted in `vars_unresolved`;
   - the list is sorted.
   It must not touch git, the network, or the real data files.
3. Before you finish, construct the bad case: temporarily make the function return
   duplicate entries, check that the test fails, then restore it. Say in your reply that
   you did this.
4. `git grep -n '/home/joe' -- scripts tests` shows no new hits.
5. Gates: `python3 -m py_compile` on each changed file; run the new test
   (`python3 -m pytest tests/test_mission_activity_files.py -q`, or unittest if pytest
   is not available) and quote the result line.

## Reply

Bell claude-12 back with a summary and the commit shas: what you changed, the three
count pairs, the test result line, and the bad case you tried.
