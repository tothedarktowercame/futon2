# Successful-close terminal classification

Status: bounded invariant for the terminal-classifier repair of 2026-10-26
(R22/R23 in `~/notes/wm-next/outer-selection/DEFECTS-reading-era.md`). This
is not a new outcome-admission rule, a learning-success rule, or authority to
rewrite an injury ledger. It is the acceptance contract an entry-45 medic
intent pins.

## Invariant

Let `r` be a retained terminal run record with a click id and a run id. `r`
is a successful close only when:

1. `r` carries no failure kind: `[:failure :kind]` is absent and `:failure`
   is absent or an explicit absence (`{:absent :no-failure}`);
2. the terminal outcome `[:terminal-receipt :outcome]` has, under the image
   `close-outcome-image` (`futon2.aif.meta-terminal-injury`, transcribed
   from `DarkTower/WarMachine/CloseOutcomeImage.lean` `image` at mathlib4
   `229c56aabf`), the value `:changed` or `:already-satisfied`.

Then terminal classification is `:non-injury` with the reason
`:successful-task-outcome` (`classify`, futon2 `aa8bf5ed5`; the Lean law is
`CloseOutcomeImage.closeVerdict`, and `click1_code_injury_law_nonInjury`
is the witness that the pre-repair classifier disagreed on click 1's record).

A record with a failure kind present is never rescued by its image. A record
whose outcome has no image, or an image the module calls invalid, keeps the
injury branch. The older set `successful-outcomes`
(`#{:success :closed :cohort-complete}`) stays for the keywords the module
does not name.

## Medic replay

Replaying the repaired classifier on such a record gives the medic replay
value `:successful-close-non-injury` (`futon2.aif.medic-disposition`
`replay-results`, futon2 `5d8b19443`; Lean
`HistoricalMisclassificationCorrection.ReplayResult.successfulCloseNonInjury`
at mathlib4 `129ad1b0ba`). It is distinct from
`:producer-qualified-non-injury`, which names a failure its producer
qualifies. `replayFromClose` ties the two: the close supplies the
successful-close value exactly when the law says non-injury, and never the
producer-qualified value.

## Retry closing law

A successful close is a terminal task outcome. `integrate`
(`futon2.aif.meta-terminal-injury`):

- with no attributable enacted retry, appends the ordinary non-injury
  observation (`:observation`);
- with an attributable enacted retry (an enacted entry whose retry click is
  this record's click), appends `:discharged` for that injury with
  `:outcome :success` and this record as the result pin.

Ledger entry 45 (`injury/9c6220388033cbd1…`, opened at click 1's terminal on
a `:grounded-progress` close with no failure) has no enacted retry history;
its correction is a historical misclassification and asserts nothing about
learning credit or task success beyond the classification above.
