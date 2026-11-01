# L5-4b — Producer: the outer field is the open tasks, with task facts and pins; reach is a task fact (claude-2 → codex-11, 2026-11-01)

Authority: Joe's ruling 4 (2026-11-01, `reconciliation/RULING-inner-selection-enacted-object-2026-11-01.md`
§Ruling 4): selection is over tasks, never over policies for doing them; feasibility only as typed
task-level facts; a never-attempted task must be selectable. Lean: `SelectionGrounding.lean` G1
(every enumerated task in the field, each with a pinned fact or a typed exclusion) and Q7's typed
path (reach observed, not defaulted). L4b carrier map rows 8 and 12. Base: futon2 `main` at
`30d5585e8` (L5-4a merged). Scope: `src/futon2/aif/outer_observation.clj`, the exclusion producers
it calls, `outer_select.clj` only where the field's shape changes, and their tests. The
`:outer-task-selection` stub and its adapter reader are L5-4c, not here. Clicks held; no reload.

## Behaviour

1. **Remove the policy-derived exclusion.** `outer_observation.clj:131` calls
   `policy-satisfaction/all-satisfied-exclusion` and drops a task from the field when its retained
   inner policies all match a satisfied record (20 exclusions across era-2, reason
   `:all-policies-already-satisfied`). Delete the call. A task is in the field iff it is open in the
   registry; a mission whose wants are met is closed there, and Q1 handles it. Keep
   `policy_satisfaction.clj` for its other callers (`full_loop_runner.clj:1634`) untouched.
2. **Closed set of exclusion reasons, all task-level.** Enumerate the reasons the field producer can
   emit today (L4b row 8 lists them: `:open` 290, `:inactive` 72, `:operator-gated` 36, `:active`
   26, `:identify` 20, `:target-source-path-absent` 12, `:unread-new` 60,
   `:blocking-effective-run-on-same-giraffe-field` 6, `:repeat-choice-after-refusal` 1, and the
   47-per-record `:outer-score/graph-term-unavailable` rows without a reason). For each: say
   where it is produced and whether it is a fact about the task (keep, typed) or about policies or
   bookkeeping (remove). `:open` and `:active` are registry *statuses*; find why they appear as
   exclusion reasons; if those rows are open tasks excluded for being open, that is the
   restricted-domain defect and they must be admitted; if they are annotations merged into the
   excluded list, move them out. Declare the final set in one place (`def task-exclusion-reasons`),
   give every exclusion row `{:target :reason :pin}` with a pin to the fact it rests on (pass row
   record-pin, registry entry sha, source path sha), and refuse an exclusion whose reason is not in
   the set.
3. **Field facts carried.** `:outer/selection` gains `:field-facts`: for every admitted task
   `{:status :cone :unblocks :rank :pin}` from the pass row (`pin` = `[:dependency-reading
   :record-pin :sha256]`), plus `:giraffe/pass-sha256`; for every excluded task the typed row above.
   Assert admitted + excluded = the enumerated open set exactly, by id, and refuse otherwise.
4. **Reach as a task fact.** Replace `:reach (:default-reach makeup)` (`outer_observation.clj:119`)
   with a value computed for *every* field member from that member's pass facts alone, in [0,1],
   recorded as `{:value :source :inputs}` with `:source` naming the facts used. Propose the
   formula from the row's fields (`:status`, `:dependency-reading` with `:edge-count` and checker
   results, `:completion`, `:cone`, `:unblocks`) and state it in a docstring with the reason; it
   is an empirical parameter, not a ruling, so make it one function with its inputs recorded. It
   must not read any inner policy, any prior run on the task, or the makeup. Remove
   `:default-reach` from the read path; if the makeup still carries the key, ignore it and record
   `:default-reach-ignored true`. A member whose facts cannot yield a reach gets
   `{:status :absent :reason …}` and is still in the field (Lean then refuses a pick of it on Q7,
   which is right).
5. **Projector.** `wm_run_facts.clj`'s selection export (L4c-2a) reads `:field-facts` when present:
   `pickReach` becomes `{"observed" <digest of the reach record>}`, and the typed exclusions'
   reasons and pins project. Keep the old behaviour for records without the key.

## Tests (one live pin each)

- The retained 11-01 observation: the field has 626 members, admitted + excluded = the open set,
  no row with reason `:all-policies-already-satisfied`, no reason outside the set, every row pinned.
- A task that had an `:all-policies-already-satisfied` exclusion on the 11-01 record is admitted.
- Reach: every admitted member has a value in [0,1] with a source; a never-attempted task has one;
  an undetermined member's value follows from the stated formula; the makeup's `:default-reach` is
  not read (change it; nothing changes).
- An exclusion with a reason outside the set refuses.
- Projector: `pickReach` is observed on a record with `:field-facts`, defaulted on an old record.
- All 12 outer namespaces plus `futon2.aif.wm-run-facts-test` and `wm-check-run-facts-test`;
  compare failures with `main` as you did for L5-4a and commit the comparison beside it.

## Gates

kondo, parens; the namespaces above; no `data/` writes (count canonical `data/` files before and
after your test runs and report both numbers; claude-3 leaked 24 files this afternoon from
store roots without env overrides, so set every `FUTON_WM_*` override you find to scratch).
Branch `codex11/l5-4b-field-is-tasks` from `main` in `~/worktrees/futon2-l5-4b`. No reload. Do
not merge. Bell claude-2 with sha, the reason table (produced where / kept or removed / why), the
reach formula and its inputs, test counts vs main, the data/ counts, and anything that does not fit.
