# L5-11 — A class that predicts exactly what stopping predicts is not a compared policy (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` (≥ this packet). Worktree only. Alert: click 11 (`wm-click-7adb0f36`, run
`4646c7bd`) **selection Q11 violated**, the first time Q11 was computable on a real click.

## Cause (found; verify)

Lean `RunFacts.predictsOtherThanStopping` (`Requirements.lean` ~507) requires every compared
policy's prediction digest to differ from the abstention's. On click 11 the record's class
`2288c727…` (member `:find-out-before-building`, pick M-interim-director-proxy-metric-inventory)
has landing `[:stop-the-line :none]`, G 3.1628312, prediction digest `20c74a30…`: identical to
the abstention's prediction and G. The other class (`:only-what-can-start-now`, `:giraffe-order`
→ E-comprehension-foundation, `[:related :several]`, G 2.8745) was picked and is the strict
minimum. Lean is right: a policy that predicts stopping is the abstention under another name,
and comparing it says nothing (Joe's ruling 1: same-prediction variants are duplicates; the
abstention is the stopping prediction).

## Behaviour

1. In `outer_select` (where `:classes` and the compared set are formed), a class whose
   prediction digest equals the abstention's prediction digest is **not** a compared policy. It
   is retained on the record as `:stopping-equivalent [{:class :members :pick :landing :g-units}]`
   with the reason `:predicts-stopping`, and it never enters `:classes`, `comparedPolicies`,
   `policyG` or `policyPrediction`. Its tasks remain in the field (this is about policies, not
   tasks: ruling 4).
2. If after that no class remains, the decision is `abstainOnTask`/`abstainEmptyField` as today
   (the field was non-empty but no policy predicts other than stopping): record the typed case.
3. The exporter reads `:stopping-equivalent` into a diagnostic only; RunFacts unchanged. A record
   before this change exports as before.
4. Nothing else changes; no new default.

## Tests

- A fixture with two classes where one predicts the abstention landing: compared = the other;
  `:stopping-equivalent` names the folded one; the export's `policyPrediction` digests all differ
  from `abstentionPrediction`; Q11 evaluable (run the checker on the export: Q11 holds when the
  remaining pick is the strict minimum below abstention).
- A fixture where every class predicts stopping: typed abstention, no pick.
- Named, outer, projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex11/l5-11`. Bell claude-2 with
sha, counts, and click 11's selection record exported on the branch (read-only) showing Q11.
