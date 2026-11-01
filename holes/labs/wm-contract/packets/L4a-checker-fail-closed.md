# L4a — The checker checks every Lean requirement, fail-closed (claude-2 → claude-3, 2026-11-01)

Plan: `futon2/holes/labs/wm-contract/PLAN-outer-selection-restoration-claude2-2026-11-01.md`,
slice L4, first part. Lean is settled through L3: darktower head `1971aeaf55` in
`/home/joe/code/mathlib4`, `Requirement` has nine constructors
(`q1 q2 q3 q4 q5 q6 q7 q9 q11`), `PartialRunFacts` has a field per constructor.
This slice is Clojure and Python under `futon2/scripts/`, canonical branch `main`.
No selection code (`src/futon2/aif/outer_*`) is touched; that freeze stands.

## The behaviour

Today, since L1 merged, `wm_check_run_facts.clj` throws
"PartialRunFacts has fields with no generator clause" for `:q2` and `:q11`, so
`wm_run_alert.py` prints "requirements NOT CHECKED" and exits 2 on every run. That is
fail-closed by accident. Two older defects make the same path fail-open the moment the
throw stops:

1. `wm_run_alert.py:6` hard-codes `PARTIAL_REQUIREMENTS=(1,3,4,5,6,7,9)`, so Q2 and Q11
   would be dropped from the verdict even when Lean computes them.
2. `wm_run_alert.py:102` reads `found.get(q,"holds")`: the Lean check prints
   `#eval alert exported`, which lists only non-holding requirements, and any
   requirement Lean did not print reads as holds. A projection whose evidence failed to
   elaborate, or a regex miss, becomes CONFORMS.

After this slice: the checker generates a clause for every `PartialRunFacts` field; the
Lean check prints the outcome of every requirement; the alert takes the requirement list
from Lean, requires every one to be present in the output, and treats absent or
`unverifiable` as not holds. Nothing defaults to holds anywhere.

## Checker (`scripts/wm_check_run_facts.clj`)

- Add `deps` and `evidence` clauses:
  - `:q2` → `["kind" "targetConstruction" "libraryPatternCount" "constructorPatternCount"
    "comparedPolicies" "constructedCascades" "enumeratedTasks" "currentChoice"]`.
    Render `kind := .work` or `.selection` from the fact (string `"work"`/`"selection"`;
    anything else: no constructor, Lean refuses), `targetConstruction` via the existing
    `target-construction` fn (it already renders `sliceFromWholeLibrary` and `action`),
    the two counts, the two id sets, `enumeratedTasks`, `currentChoice`.
  - `:q11` → `["currentChoice" "comparedPolicies" "constructedCascades" "targetConstruction"
    "enumeratedTasks" "kind" "policyG" "abstentionG" "policyPrediction"]`.
    Render `policyG := [(id, n/d), …]` from `[{"policy" …, "g" {"numerator" n, "denominator" d}}]`,
    `abstentionG := n/d`, `policyPrediction := [(id, digest), …]`. Exact rationals only:
    if the export carries a float, that is `.nr` ("G not projected at a declared scale"),
    not a conversion.
  - `:q7` deps become `["pathAbsenceCount" "kind" "selectionPath"]`. For `kind = work`,
    render `selectionPath := none`. For `kind = selection`, render the `SelectionPath`
    literal with `Provenance` constructors from `{"observed" digest} | "defaulted" | "absent"`
    and `pendingRun`/`consumedRuns` as `Option`/`List` of `(id, provenance)`. If the
    export says `selection` without a `selectionPath`, `.nr`.
- `lean-source` prints `#eval outcomes <definition>` (all nine) instead of
  `#eval alert <definition>`. Keep the `partial-run-fact-fields` regex read of the reader;
  it is what ties the generator to Lean.
- The unknown-field throw stays. It must now not fire against the current reader.

## Projector (`scripts/wm_run_facts.clj`)

- Emit `"kind" "work"` for a tick run record. The projector labels the kind; the record
  never does (L1 review: a one-cascade menu labelled work dodges the floor of two).
- Emit `"selectionPath"` as `.nr` text for work runs? No: for a work run the checker
  renders `none` from the kind. Emit nothing.
- `policyG`, `abstentionG`, `policyPrediction`: emit `(nr "per-policy G, abstention G and
  prediction digests not yet projected from the inner record")`. Finding where they live
  in the inner record is L4b, a discovery packet. Do not guess at `:posterior` or `:G`
  here; I looked and the `:G` values in the 11-01 record are not plain numbers.
- Everything else unchanged.

## Alert (`scripts/wm_run_alert.py`)

- Delete `PARTIAL_REQUIREMENTS`. Read the constructor list from
  `Requirements.lean`'s `inductive Requirement where | q1 | …` line (regex, same style as
  the checker reads `PartialRunFacts`), and from the Lean output parse
  `Requirement.qN, … Outcome.X` for every N.
- A requirement in the constructor list but absent from the output: outcome
  `"not-checked"`. `holds` is the only outcome that counts as holds; `unverifiable`,
  `violated`, `not-checked` are all in `bad`. `:conforms` is true only when all nine hold.
- `":verdicts"` and `":critical-parameters"` iterate the constructor list. `numbers(q, f)`
  needs Q2 and Q11 lines; for carriers that are `.nr`, print the `.nr` reason.
- Exit status rules unchanged (nonzero on any alert; 2 on NOT CHECKED; 3 on post-check
  failure).

## Tests to update (one pin per behaviour, captured from a live run)

- `test/wm_check_run_facts_test.clj`: `partial-run-facts-fields-come-from-lean` must see
  nine fields; `unknown-lean-field-fails-before-generation` keeps the throw; add a test that
  the generated Lean for the 11-01 record `ac5bc477` contains `q2 := .known` and
  `q11 := .nr` and `#eval outcomes`.
- `test/scripts/wm_run_alert_test.py:53` fakes the Lean output with `Outcome.holds` for
  q in 1..10; change to the nine real constructors, and add: (a) a fake output missing
  one constructor yields `not-checked`, ALERT, `:conforms false`; (b) `unverifiable` on
  q11 yields ALERT, `:conforms false`.
- `test/futon2/aif/wm_run_facts_test.clj`: `kind` is `"work"`.
- Run `clojure -X:test :nses '[...]'` for the two clj tests and `python3 -m pytest
  test/scripts/wm_run_alert_test.py` (or unittest, whichever the file uses).

## Acceptance

Run the real alert on the 11-01 record from the futon2 checkout:
`python3 scripts/wm_run_alert.py --run-record data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn --no-evidence --alerts-log /tmp/l4a-alerts.log`
and paste the block. Expected: ALERT FOR JOE with Q7 violated (62-style path count or
the typed path), Q9 violated, Q11 unverifiable (G not projected), Q2 whatever Lean says
of the projected construction; exit nonzero. Do not write a `.requirements.edn` next to
the live record: copy the record to `~/notes/l4a/` first and run against the copy.

## Gates

- clj-kondo on the two Clojure scripts; `futon4/dev/check-parens.el` on them.
- futon2 branch `claude3/l4a-checker-fail-closed` from `main`, in a worktree
  `~/worktrees/futon2-l4a` (never `/tmp`); run tests there with the `:test` exec alias
  (`clojure -X:test :nses '[ns]'`), not `-M:test -e`.
- Do not merge. Bell claude-2 with: sha(s), test output, the acceptance block verbatim,
  and anything in the record or scripts that does not fit this packet.
