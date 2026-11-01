# L4c-2b — The alert checks both runs of a click and conforms only if both do (claude-2 → claude-3, 2026-11-01)

Plan slice L4, last part. Base: futon2 `main` at `900548ec7` (L4c-2a merged); mathlib4 `55878e243a`.
Scope: `scripts/wm_run_alert.py` and `test/scripts/wm_run_alert_test.py`. Projector and checker
unchanged. No `data/` writes; acceptance on copies.

## Principle (plan: one loop, two runs per click, each a RunFacts)

A click records a selection run and a work run. Today the alert evaluates the work run only.
After this slice it evaluates both, prints both, writes both into the verdict file, and the
click conforms only when every requirement holds on both runs.

## Readers that must keep working (checked by claude-2, 2026-11-01)

- `scripts/wm_click.sh:790-809`: needs the verdict file to exist at `<record>.requirements.edn`
  and uses the alert's exit status. Unchanged contract.
- `futon3c/src/futon3c/wm/runner_service.clj` (`POST /api/alpha/wm/click/child-verdict`,
  ~line 440-500): reads `":run-record"`, `":conforms"`, `":verdicts"` from the file and stores
  `:verdicts` as given; it compares `:run-record` to the child's record path. So those three keys
  keep their names and shapes: `":run-record"` the record path; `":conforms"` a boolean;
  `":verdicts"` a flat `{":Q1" ":holds" …}` map. The `":verdicts"` map stays the **work** run's.
- `scripts/wm_click_record_child_verdict.py` only forwards the path.
- Tests under `test/scripts/wm_click_*_test.py` fake the file with those keys; keep them passing.

## Behaviour

1. Export twice: `clojure -M -m wm-run-facts --run <record>` (work) and the same with
   `--kind selection`. Check each with `wm_check_run_facts.clj` into its own `.lean`
   (`RunCheckWork.lean`, `RunCheckSelection.lean`, definitions `work` and `selection`), run
   Lean on each, parse each with the existing regex against the Lean constructor list.
2. **Printed block.** First line `ALERT FOR JOE` or `CONFORMS`. Then a line `work run:` and the
   per-requirement lines as today (every requirement when conforming, the bad ones when not);
   then `selection run:` and its lines. The existing test regexes `(?m)^Q7 violated:` must still
   match, so the Q lines stay at column 0 under their section header.
3. **Verdict file.** Keep `":run-record"`, `":checked-at"`, `":conforms"` (true iff every
   requirement holds on both runs), `":verdicts"` (work run, flat), `":critical-parameters"`
   (work run), `":not-recomputable"` (work run), `":module-shas"`, `":exporter-sha"`,
   `":evidence-publication"` as they are. Add:
   - `":selection-verdicts"` flat `{":Q1" … ":Q11" …}`,
   - `":selection-critical-parameters"`,
   - `":selection-not-recomputable"`,
   - `":policy-g-scale"` `1000000` read from either export's `policyGScale` (they agree; if they
     differ, that is itself a line in the block and `:conforms` is false),
   - `":runs"` `[":work" ":selection"]` so a reader can see the file is two-run.
4. **Exit status.** As today: 1 when the block is an alert, 2 when NOT CHECKED (either export,
   check or Lean step failing), 3 for post-check failures. The second export failing is NOT
   CHECKED for the whole click: never fall back to a one-run verdict.
5. **`numbers()`** gets the selection run's lines too. For Q2/Q11 the `.nr` reasons already
   print; for Q7 print `pathAbsenceCount` and the four path provenances
   (`receipt`, `pickRelation`, `pickReach`, `learningApplied`) so the operator sees *which*
   absence Lean refused. For Q1 the existing enumerated/open line.
6. **Evidence payload** (`futon1b`): `"conforms"` is the two-run value; `"block"` the full block.
   Nothing else changes.

## Tests (one per behaviour, in `wm_run_alert_test.py`)

- Both exports are requested: the fake `run` sees two `wm-run-facts` calls, the second with
  `--kind selection`.
- Both conform → `CONFORMS`, `":conforms" true`, exit 0.
- Work conforms, selection has Q7 violated → `ALERT FOR JOE`, `":conforms" false`,
  `":verdicts"` all holds, `":selection-verdicts"` has `":Q7" ":violated"`, exit 1.
- Selection export fails (non-zero) → `requirements NOT CHECKED`, exit 2, no verdict file.
- A requirement missing from the selection Lean output → `not-checked` in
  `":selection-verdicts"`, alert, conforms false.
- The three readers' keys are present and shaped as before (one assertion each).
- Update `test_click20_alerts_and_exits_nonzero` and the fixture at line ~23 for two exports.

## Acceptance

On copies of ac5bc477, 376d76ff, d5b9b513 under `~/notes/l4c2b/`:
```
python3 scripts/wm_run_alert.py --run-record <copy> --no-evidence --alerts-log ~/notes/l4c2b/alerts.log
```
Paste the three blocks verbatim and each verdict file's `":conforms"`, `":verdicts"` and
`":selection-verdicts"` lines. Expected for ac5bc477: work run Q7, Q9 violated, Q11
unverifiable; selection run Q7, Q9 violated, Q2/Q4/Q5/Q6/Q11 unverifiable; exit 1. Then run
the four `test/scripts/wm_click_*_test.py` and report they still pass.

## Gates

`python3 test/scripts/wm_run_alert_test.py` and the four click tests; `python3 -m pyflakes
scripts/wm_run_alert.py` if available. Branch `claude3/l4c2b-alert-two-runs` from `main` in
`~/worktrees/futon2-l4c2b`. Do not merge. Bell claude-2 with sha, test output, three blocks,
and anything that does not fit.
