# C515 — :F7, the cascade-shaped π: the falsifier fired

**Row:** `:F7` — inhabit the policy carrier, cascade-shaped π.
**Bar (the row's own `:acceptance`):** a committed decision record offering ≥ 2
distinct constructed-and-scored cascades for one target, the falsifier firing
for real and not in a fixture; `FUNDAMENTALS.edn`'s
`:fundamental/machine-policy-carrier` moving with the commit cited; a
single-cascade decision NOT satisfying the check; no live tick required.

## 1. What the falsifier said, and what had been done with it

`FUNDAMENTALS.edn:272` (pre-change) named this entry's falsifier:

> A runtime decision in which two or more cascades for one selected mission are
> constructed and scored, or a Lean value of `cascadeGrainPi` built from machine
> data.

That is a statement about runs. Until `:F1` slice 2 it had only been argued from
code reads — `cascade_prior.clj:1-12` saying the namespace is dark,
`forward_model.clj:25-32` showing the machine ranks single actions.
`f1_policy_family_census.bb` then ran it against the corpus and it did **not**
fire: `runs/F1-machine-q/02-policy-family-census.edn:55-58` records 234 of 889
decisions carrying an `:apply-cascade` candidate,
`:most-distinct-cascades-for-one-target-in-one-record 1`,
`:records-with-two-or-more-cascades-for-one-target 0`. The probe was not blind —
it saw the object — and no decision offered a *choice* between two of them.

This row made it fire.

## 2. What was built, and what was already there

Two pieces of the machine already existed and neither had ever met a recorded
decision:

- `futon2.report.cascade-lane/cascade-policy-menu-for`
  (`scripts/futon2/report/cascade_lane.clj:102-144`) varies the
  coverage-saturation threshold around the production incumbent 0.15 and admits
  only complete, untruncated, distinct policies at the constructor's pool
  ceiling. Before this row its only callers were its own 2-arity delegation
  (`:112`) and `test/futon2/report/cascade_lane_policy_menu_test.clj:40,60` —
  and that test supplies the cascades with `with-redefs`, so the frontier had
  never been run against the real Python constructor in any committed artifact.
- `futon2.aif.cascade-prior/shadow-rank` (`src/futon2/aif/cascade_prior.clj:163-207`)
  ranks a non-degenerate same-mission menu, refusing fewer than two policies
  (`:176-179`) and a mixed-mission menu (`:138-141`).

What this row adds is the third piece: a producer that joins them to a decision
the machine actually made.

`holes/labs/wm-contract/f7_cascade_policy_decision.clj:101-160` reads one
recorded decision out of `data/wm-trace` **by `:run/id`**, takes the mission the
machine committed to, builds |ψ⟩ with the live lane's own
`mission->psi` (`cascade_lane.clj:306` — not a ψ invented for the occasion),
constructs the menu, scores it, and writes the record. It writes nothing under
`data/`, takes no run lock, is not a tick, and sets `:enacted? false`.

## 3. The record

`runs/F7-cascade-policy/f7-cascade-policy-decision.edn` — recorded run
`c149f9de-669c-4817-9b0e-ed4aad77db79` (`data/wm-trace/wm-trace-2026-09-04.edn`,
`2026-09-04T07:50:47.952039102Z`), committed target `M-expressions-of-interest`,
`:decision-type :advance-mission`.

- `:candidate-count 2`, `:policy-choice? true` (`:161-162`).
- The two policies differ in **both** pattern membership and wiring, which is
  the diversity the grain-compliance mission asks for
  (`M-wm-aif-policy-grain-compliance.md:109`): 23 patterns with a 7-edge
  `:descent` and 2 `:co_app` edges at ε = 0.10, against 1 pattern with an empty
  semilattice at ε = 0.20 (`:5-137`). ε = 0.15, the production incumbent,
  produced the same identity as 0.20 and collapsed into it — the menu does not
  pad.
- Both carry a finite `:cascade-score` (−6.901 and 0.019) and both are ranked
  with normalized weights (`:186-265`).
- `:governed-by :cascade-score`, and that is the honest reading rather than a
  result: the cascade prior is at cold start (`:samples 0`), so ln E(π) is
  uniform over the menu and the habit leg has nothing to say yet.

## 4. The check, and the negative control the row asked for

`f7_cascade_choice_check.bb` decides whether a record exhibits the falsifier.
Counting the candidates is not the check: the interesting failure is a **false
two**, and every refusal below is a way of carrying two entries without offering
a choice of policy — `:error/single-cascade`, `:error/duplicate-policy-identity`,
`:error/forged-policy-key`, `:error/mixed-mission`, `:error/truncated-candidate`,
`:error/unscored-candidate`, `:error/unranked`, `:error/ranking-population`,
`:error/unnormalized-weights`, `:error/claims-enactment`,
`:error/basis-does-not-match-record`, `:error/basis-run-not-found`,
`:error/not-a-cascade-policy-decision`.

**The negative control is not a plant.** `--single` runs the same pipeline over
the same recorded decision at the production incumbent ε = 0.15 only, and gets
exactly one cascade: `runs/F7-cascade-policy/f7-single-cascade-decision.edn:52`
`:candidate-count 1`, `:34` `:menu-status :no-policy-choice`, `:77`
`:selection nil`. That is the shape of all 889 recorded decisions, and the check
refuses it.

`negative_controls.sh` section 12 runs 13 negative and 2 positive controls over
these records (suite 75/43 → 88/45). Three are mutation-tested, and two of the
three revealed that the refusal is **over-determined**, which is worth stating
rather than glossing:

| mutation | control that reddens | what it showed |
|---|---|---|
| the `< 2` candidate refusal neutered | 12e | the one-cascade record still refuses, as `:error/unranked` — `shadow-rank` refuses fewer than two by design, so a one-cascade decision has no ranking to record. Fails closed twice. |
| the recomputed-key comparison neutered | 12g | a hand-edited `:policy-key` is accepted. Single point of failure; the control is load-bearing here. |
| the basis comparison neutered | 12c | the planted target still refuses, as `:error/mixed-mission`. Fails closed twice. |

Control 12o is the transcription control. The checker carries a copy of
`cascade-prior/policy-key` so it runs under `bb` without the futon2 classpath —
the same trade `f1_policy_family_census.bb` makes for `habit-prior/policy-key` —
and a copy that had drifted would still refuse and still accept, on the wrong
identities. So 12o loads the real namespace once and compares its keys with the
record's.

## 5. The census entry moved, and exactly how far

`:fundamental/machine-policy-carrier` moves `:in` → `:out` with
`:criterion-leg-that-fails :lean`. The criterion is a conjunction — uninhabited
in **both** Lean and the runtime — and the runtime side is now inhabited by a
producer, a scorer and an actual record. That is the standard
`:out/realized-outcome-channel` closed its runtime leg by
(`FUNDAMENTALS.edn`, `:out/realized-outcome-channel` `:runtime :basis`), and not
a schema. Census counts: 11 entries, 7 `:in` / 4 `:out` → **6 `:in` / 5 `:out`**.

**What the move does not say.** The entry's `:what-is-still-open` carries four
limits and they are the substance of the review:

1. Nothing selects or enacts a cascade. The production lane still constructs one
   cascade per target; `cascade-policy-menu-for` has no caller outside this
   producer and its test; and the advisory lane that would carry cascades into a
   tick is off at `scripts/futon2/run_tick_once.clj:255`
   (`:include-advisory-lanes? false`). The criterion this census applies is that
   a machine **value** of the carrier exists, not that it is on the live path.
2. The habit leg is cold: no cascade-prior state has ever been persisted.
3. One decision, one mission. What fraction of targets yield two or more
   complete distinct policies is not measured.
4. The Lean leg is untouched — which is why the entry is `:out` on a failing
   Lean leg rather than closed.

One sentence in the old entry deserves its own note. `cascade_prior.clj:1-12`
was the strongest evidence for `:uninhabited`: *"Live use is valid only after
the War Machine exposes two or more admissible cascades for the same selected
mission and supplies an honest cascade-level score."* It is the same sentence,
and it now reads the other way, because it states a **condition** and both of its
clauses are met by the record. What follows is that live use would now be valid,
not that it has been taken.

## 6. Pointer corrections found on the way

- `C475-cascade-order-disposition.md:96,185` cites `run_tick_once.clj:211` for
  `:include-advisory-lanes? false`. It is at `scripts/futon2/run_tick_once.clj:255`.
  Reported here; C475 is a committed report and is not rewritten.
- `pointer_check.bb`'s hand-maintained bare-filename root list had no entry for
  `runs/F7-cascade-policy/` or `runs/F1-machine-q/`. Appended (not inserted), the
  ninth occurrence of that defect class; its own header records the previous eight.

## 7. What this row did not do

- **No ruling.** `aif-equations.edn :choices` and `control-map-edges.edn
  :decisions` are untouched. The `FUNDAMENTALS.edn` move is a census verdict that
  follows mechanically from the stated criterion plus the record, and its
  `:census-correction` carries the before, the after and the why.
- **No `src/` change in futon2**, and none in `scripts/` either: the constructor
  and the scorer are used as they stand. So no test run is claimed. The one
  existing test of the frontier (`cascade_lane_policy_menu_test.clj`) is
  unchanged and still fixture-based; this row's evidence is the record, not that
  test.
- **No live tick, no run lock, nothing written under `data/`.** `data/wm-trace`
  was read only.
- `gen_aif_dag.bb` not run; nothing regenerated into a publish (TN §9a).
- `:F1` slice 2 is **not** unblocked by this row's own hand. The carrier now
  exists at the declared grain, which is what `02-policy-family-census.edn`
  `:conclusion` said slice 2 was waiting for; whether slice 2 proceeds on it is
  that row's call and a reviewer's.
- The row's `:statement` records a reversal: one line from Joe ruling the action
  grain admissible as π supersedes this row. Nothing here forecloses it — the
  action-grain reading is untouched, and this record is additive.
