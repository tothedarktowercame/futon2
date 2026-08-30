# AUD-D3 — make the WM report's file reads loud; delete the three never-produced inputs

Owner: claude-15. Mode: work. Two files, one behaviour (I_absent_is_loud), one acceptance run.

## Context
`I_absent_is_loud` (futon4/holes/delivery-lifecycle.md §0.7; PREREG §4). The lint `futon2/checks/absent_is_loud_lint.clj`
(AUD-D2, `f3e928f`) exits 1 today with 7 silent+absent-now sites, all reads of three files that were planned on
2026-05-03 in `M-war-machine.md` and NEVER produced (no commit anywhere): `futon5a/data/stack-logic-model.edn`,
`alignment.edn`, `jsdq-terminal-vocabulary.edn`. Joe's decision: the stack logic model is SUPERSEDED, not rebuilt —
read `futon2/holes/problems/P-supersede-stack-logic-model.md` first; your commit message must cite it.

## Goal
1. `futon2/scripts/futon2/report/war_machine.clj`: remove the dead branches at :2075 (alignment), :2089 and :2150
   (workstream-nodes / workstream-dependency-edges from the logic model), :2211 (pocketwatch ticks) — and whatever
   downstream code only consumed their output (render sections that would now always be empty). Leave a one-line
   comment at each removal site: `;; superseded — see holes/problems/P-supersede-stack-logic-model.md`.
2. `futon0/scripts/futon0/report/joe_hud.clj:427-429`: same three reads; same treatment.
3. Make `read-edn-file` LOUD in both files: on absence return `{:missing path}`; on parse failure
   `{:unreadable path :cause (ex-message e)}`; never nil. Then walk its remaining call sites (the lint lists them:
   `futon2/holes/labs/wm-contract/AUD-D2-findings.md`) and make each either (a) fail closed, or (b) render the
   `:missing`/`:unreadable` marker into the report output so the reader sees it — no call site may treat the marker
   as data. `safe-slurp-json` (:3451, mark2 cache): same shape.
4. Do NOT touch any other repo, any other helper, or the lint itself. If a call site cannot be classified honestly
   as (a) or (b), REFUSE that site with the reason and leave it — the lint will keep reporting it.

## Acceptance (dry-run before reporting — row 11)
- `bb futon2/checks/absent_is_loud_lint.clj` (bare, not piped — the exit code is the gate): `silent+absent-now=0`
  and `read-edn-file` no longer classed `silent` in either repo. State the full verdict line.
- The WM report still builds: run whatever entry point produces it (`war_machine.clj` main / the script that
  calls it — say which) and confirm exit 0 and that the output contains no section that was previously fed by
  the three files. A `:missing` marker for any OTHER absent input is CORRECT output, not a failure — list them.
- clj-kondo clean on both files; `futon4/dev/check-parens.el` clean.
- Two commits (one per repo), only your paths; messages cite `P-supersede-stack-logic-model.md`.

## Report
Bell claude-15 back with: both shas, the verdict line, the list of `:missing` markers the report now shows, any
refused call sites with reasons, and a one-line diffstat per file.
