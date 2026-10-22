# Packet: record the habit fold each click holds, and whether it was consumed (2026-10-22)

From claude-2. Recording change only (Joe approved recording changes 2026-10-22).
Selection must not change. This is step 1 of section 4 in
`holes/labs/wm-contract/NOTE-r20-habit-counts-2026-10-22.md` (codex-11, e6480b6e3);
read sections 1 and 3 of that note first.

## Why
Live clicks take the sealed outer-policy route
(`src/futon2/aif/wm/cascade_decision.clj:1332`), which never calls
`cascade-habit-store/attach-state`, so `:habit-reads` is `{:status :absent
:reason :never-read}`. The report still builds an enactment fold
(`scripts/futon2/report/war_machine.clj:6832`–6838) and the click record keeps
nothing of it. R20 needs the counts a click held to be on the record, with an
honest statement that the live prior did not use them.

## Goal (one behaviour)
The tick run record gains a top-level `:habit-fold-snapshot`, next to
`:habit-reads` (`src/futon2/aif/full_loop_runner.clj:1211`):

    {:schema :wm/habit-fold-snapshot-v1
     :status :present
     :counts ... :samples ... :alpha ...        ; from the fold, unchanged
     :folded-from ...                            ; paths + hashes the fold already carries
     :snapshot-edn <pr-str of the above> :sha256 <sha of snapshot-edn>
     :consumed? <boolean>
     :consumed-by <vector of :habit-reads occurrence indices, empty when not consumed>
     :ln-e-source <keyword naming where the live ln E came from>}

- The snapshot must be **the fold value the selection was handed** (the
  `:enactment-fold` the run's opts carried), not a re-fold done at record time.
  If that value does not reach the runner, record
  `{:schema :wm/habit-fold-snapshot-v1 :status :absent :reason :fold-not-passed-to-runner}`
  and say in your bell where it stops. Do not add a second fold call to fill it.
- `:consumed?` is derived, not declared: true exactly when `:habit-reads` has an
  occurrence whose snapshot hash matches this snapshot. On today's route it
  will be false. Do not hard-code false.
- `:ln-e-source`: on the sealed outer route, `:outer-controller-inputs`
  (resource/cadence/continuation, `outer_policy_provider_runtime.clj:36`–43);
  `:habit-fold` only when consumed. Take this from the route actually taken
  (the selection certificate's `:selection-route`), not from a constant.
- Do **not** put the snapshot into `:habit-reads :occurrences`; the note explains
  why that would read as consumption.

## Validation
Extend `validate-record*` in `src/futon2/aif/scoring_input_receipts.clj` so that:
- a `:present` snapshot whose `:sha256` does not match its `:snapshot-edn` fails
  (`:habit-fold-snapshot-hash-mismatch`);
- `:consumed? true` with no matching `:habit-reads` occurrence fails
  (`:habit-fold-consumption-unwitnessed`), and so does `:consumed? false` with one;
- a record with no `:habit-fold-snapshot` key at all fails
  (`:habit-fold-snapshot-not-observed`) — the same pattern as
  `:habit-receipts-not-observed`. Check old fixture records still in tests and
  update fixtures, not the rule.

## Acceptance tests (in `test/futon2/aif/scoring_input_receipts_test.clj`)
1. A record built the way the runner builds it, with an empty habit log and a
   fold with nonzero counts: snapshot present, `:consumed? false`,
   `:ln-e-source :outer-controller-inputs`, validates clean.
2. Bad cases, each caught with its own error: flipped `:consumed? true` on (1);
   one count edited after hashing; snapshot key removed.
3. A consumed case: a habit-reads occurrence produced by `attach-state` on the
   same fold gives `:consumed? true` and validates.

## Do not
- change selection, the objective, ln E, or any route;
- write any count/receipt file (steps 2–4 of the note are later packets);
- touch `meta_live_outer_selector.clj` or menu scoring (codex-4 is working there on
  packet A1).

## Gates
clj-kondo on changed files; `emacs --batch -l ~/code/futon4/dev/check-parens.el`
on changed Clojure; `clojure -M:test` for `scoring-input-receipts-test` and any
namespace whose fixtures you touched (set `FUTON3C_INVOKE_JOBS_FILE` to a temp
path if a test touches the invoke ledger). Rebase onto current master before
committing. No live clicks.

If you think this field would mislead a reader of the record, or that step 1
should not land without step 2, say so and stop; that is a useful answer.

Bell claude-2 back with a summary + commit shas.
