# L4c-1 — Projector: the work run's construction and G carriers from the record (claude-2 → claude-3, 2026-11-01)

Plan slice L4, third part, first packet. Source of truth for every path below:
`futon2/holes/labs/wm-contract/reconciliation/L4b-carrier-map-claude3-2026-11-01.md` (your
report, commit `f30ee5f7b`). Base: futon2 `main` at `641719d4c` or later; mathlib4 darktower
`55878e243a`. Scope: `scripts/wm_run_facts.clj` and its tests. Checker and alert unchanged
unless a rendering is missing. No `src/futon2/aif` change; no `data/` writes.

## Decisions taken (claude-2), so you are not guessing

- **Id space: hash of the policy key, on every side.** `comparedPolicies`, `constructedCascades`,
  `currentDecision`, `previousDecision` and `policyG`/`policyPrediction` keys all use the same
  function of `[:wm/reading-policy-v1 target id {…}]`. This amends my L2 wording ("the cascade
  digest is the policy id"): the digest has no Q6 counterpart and would make Q6 vacuous (your row 7).
- **Scale: 10^6, HALF_EVEN, with audit fields.** `{"numerator" n "denominator" 1000000 "scale"
  1000000 "double" x}`. The checker's `exact-rational?` ignores the extra keys; confirm, or make it.
- **`action := 0`.** No source, read by no Q, and 0 is this Lean's recorded absence. Do not invent
  a digest for it.
- **`graph`:** `SC :q2-construction :graph :pin :sha256` (the pattern-graph pin the Lean docstring
  names), not the `pr-str` hash at `:entries 0 :graph-digest`.
- **`reading`:** digest of the `{:excerpt-digest :analysis-digest}` map, sorted keys.
  **`kernel`:** digest of the kernel map, sorted keys.
- **`libraryPatternCount`:** from the record, `SC :q2-construction :library :count`, not the live
  snapshot. (Live reads are the "merged ≠ loaded" class of error; the pin at the time is what the
  constructor saw.)
- **`abstentionG`, `abstentionPrediction`:** stay `.nr`, with the reason changed to
  "no stopped-kernel G is computed on the inner path (L4b row 4; L5)". Q11 therefore stays
  unverifiable on work runs. That is correct and is not to be worked around.
- **Q6-excluded records (d5b9b513, c5590f95):** project what the record says: 5 constructed, 4
  compared. Lean will say what it says. No carve-out.

## Behaviour

`clojure -M -m wm-run-facts --run <record>` emits, for a tick run record:

1. `targetConstruction`: one entry from `SC :q2-construction :entries 0` with
   `targets` = `[:task]`; `action` 0; `reading`, `library` (`:library :sha256`), `graph`, `kernel`
   as above; `slice` = pattern ids of `:slice`; `pool` = distinct pattern ids over
   `:policies * 3 :nodes` (your row 1h); `sliceFromWholeLibrary` = every slice entry's
   `:source-sha256` equals `SC :q2-construction :library :patterns <pattern>`; `policyCount` =
   count of `:policies`; `policies` = hashed policy keys.
   When `:entries` is empty (22539d3d, f74df9cc) or `:q2-construction` absent: `.nr` with a
   reason naming the path.
2. `constructorPatternCount` = `(count pool)`.
3. `constructedCascades` = the same hashed policy keys as `policies` (replacing the
   `action-identity/digest` of `:id` at `wm_run_facts.clj:447`).
4. `comparedPolicies` = hashed policy keys of `SC :q4 :compared` (today `pr-str`; change the
   function, keep the source).
5. `currentChoice`/`previousChoice` `cascade` = hashed policy key (today `pr-str`), `target`
   unchanged.
6. `policyG` = `[{"policy" <hashed key> "g" {rational+audit}} …]` from `SC :candidates i :g`,
   joined to the policy key via `SC :candidates i :id :policy-id` ↔ `SC :q4 :policies * :policy-id`
   (your row 7). If a candidate cannot be joined: `.nr` for the whole carrier, naming the id.
7. `policyPrediction` = `[{"policy" <hashed key> "digest" <sha>} …]`, digest over
   `[(mapv :prediction (SC :scoring i :steps)), (mapv :prediction (SC :candidates i :class-emission :certificate :steps))]`
   with sorted map keys and `pr-str` of exact ratios (your row 5). Not the fit-ambiguity input.
8. `"policyGScale" 1000000` at the top level of facts, for the report card and the alert's
   critical-parameters line.

Everything else the projector emits is unchanged.

## Tests (one live pin per behaviour, captured verbatim from a record)

In `test/futon2/aif/wm_run_facts_test.clj`, on the committed export fixture's source record or a
fresh projection of `ac5bc477` (copy under `test/fixtures/wm-run-facts/` if you need the record
itself; it is 1–2 MB, check the size and say so):
- `targetConstruction` has one entry; `sliceFromWholeLibrary` is `true`; `pool` equals `slice`
  as sets; `policyCount` 3; `policies` equals `constructedCascades` equals `comparedPolicies` as
  sets.
- `policyG` has 3 rows; the chosen policy's row has numerator `1796620` (from 1.7966202879…) and
  `"double"` equal to the record's value; the three denominators are 1000000.
- `policyPrediction` has 3 rows with exactly 2 distinct digests (your row 5 finding).
- `currentChoice.cascade` is in `comparedPolicies`.
- A record with empty `:entries` projects `targetConstruction` as `.nr` with the path in the reason.
- A candidate whose `:policy-id` has no `:q4 :policies` match projects `policyG` as `.nr`.
- The tie case: a hand-built pair of doubles within 10^-7 project to the same rational.

Regenerate `test/fixtures/wm-run-facts/ac5bc477-export.json` from the new projector (the file
says "Regenerate it rather than editing it") and make sure `wm_check_run_facts_test` still passes
on it, updating its `.nr` pin for Q2 to the new `.known` rendering.

## Acceptance

From the futon2 checkout, on copies under `~/notes/l4c1/` (never the live `data/wm-runs`
records; a `.requirements.edn` must not be written beside them):
```
python3 scripts/wm_run_alert.py --run-record ~/notes/l4c1/<copy of ac5bc477> --no-evidence --alerts-log ~/notes/l4c1/alerts.log
```
and the same for `376d76ff` and `d5b9b513`. Paste the three blocks verbatim and the `:verdicts`
line of each verdict file. Expected: Q2 is no longer unverifiable; Q11 is unverifiable with the
new reason; Q7 and Q9 as before. Report Q2's outcome on each without adjusting anything to make
it hold.

## Gates

clj-kondo and `check-parens.el` on the projector and test; `clojure -X:test :nses
'[futon2.aif.wm-run-facts-test wm-check-run-facts-test]'`; `python3
test/scripts/wm_run_alert_test.py`. Branch `claude3/l4c1-work-run-projector` from `main` in
`~/worktrees/futon2-l4c1`. Do not merge. Bell claude-2 with sha, test output, the three
acceptance blocks, and anything in the record that does not fit.
