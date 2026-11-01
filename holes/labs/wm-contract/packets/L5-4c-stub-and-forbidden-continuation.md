# L5-4c — Producer: remove the deleted selector's stub; reselection forbids instead of filtering (claude-2 → codex-11, 2026-11-01)

Base: futon2 `main` at `3c0d41bf8` (L5-4b merged). Authority: plan L5-4; Joe's ruling 3 (a refused
policy stays compared, the refusal forbids choosing it) applied to the outer run; Lean Q7 (the
`:outer-task-selection` stub is a typed absence on every record: L3, L4c-2a) and Q6/Q11 (the
forbidden set is read from the record). Two behaviours, both small, both yours to land in one
packet because they touch the same runtime file.

## 1. Remove the stub

`full_loop_runner.clj:1227` writes `{:schema :wm/outer-task-selection-v1 :status :absent :reason
:receipt-not-retained}` on every record: the deleted selector's receipt key, never populated since
2026-10-28. `construction_receipt_lean_adapter.clj:558` reads it for `:chosen`, `:action`,
`:outer-learning`. Remove the writer. Make the adapter read `:outer/selection` (chosen cascade
`:cascade-id`, selected target `[:selection :selected :target]`, learning `:outer/learning`), or,
if its consumers want the old three keys, derive them from `:outer/selection` and say so in a
docstring; a record with no `:outer/selection` gives the adapter a typed absence naming that key.
List every other reader of `:outer-task-selection` (grep `src/`, `scripts/`, `test/`, and futon3c
`src/`) and change or exempt each. The projector's selection export (`wm_run_facts.clj`, L4c-2a)
counts the stub in `pathAbsenceCount`; once the stub is gone that count must drop by one on a new
record and stay as is on old records (pin both).

## 2. Reading reselection forbids; it does not filter

`full_loop_runtime.clj` reading-reselection port: after a reader refusal it currently returns the
typed no-reselection result you left in L5-4b. Finish it: pass the refused target to
`outer-select/select` as `:forbidden #{target}` with the refusal's kind and pin; `select` keeps the
task in the field, scores and records it in `:cascades`, and takes the minimum over cascades
whose pick is not forbidden; if every cascade's pick is forbidden, refuse `:no-admissible-pick`
naming them. Record `:forbidden [{:target :reason :pin}]` in `:outer/selection`. The field
partition assertion from L5-4b must still hold (nothing removed).

## Tests (one live pin each)

- A new record carries no `:outer-task-selection` key; the adapter reads the chosen cascade and
  target from `:outer/selection`; an old record still projects its stub as an absence.
- The selection export's `pathAbsenceCount` on the 11-01 fixture is unchanged (old record) and on
  a freshly produced section is one less.
- Reselection after a refusal: the refused target is in `:field-facts` and `:cascades` picks, is
  listed under `:forbidden`, and the chosen cascade's pick differs from it; when every cascade
  picks the forbidden target, `:no-admissible-pick`.
- The 12 outer namespaces, `futon2.aif.wm-run-facts-test`, `wm-check-run-facts-test`, and the
  adapter's test namespace, compared with `main` as before; canonical `data/` count before and
  after.

## Gates

kondo, parens, overrides to scratch, no reload, no merge. Branch
`codex11/l5-4c-stub-and-forbidden` from `main` in `~/worktrees/futon2-l5-4c`. Bell claude-2 with
sha, the reader table for the stub key, test counts vs main, data/ counts, and anything that does
not fit. Note for the record, not for this packet: the live makeup's cascades sort undetermined
rows by an unavailable cone and now refuse; the menu's source (makeup file versus pinned library)
is the next slice, L5-4d.
