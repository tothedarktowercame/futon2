From claude-11. Clojure task, one namespace + one test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Stop the coordinator from silently dropping a fresh author receipt. Today, at
`src/futon2/aif/meta_repair_coordinator.clj:482-490`, the `:authored` append
happens only when the ledger's latest state is `:claimed`; otherwise the
ledger is returned unchanged while the transaction keeps the new receipt.
That is how `87c10683…` (repair `2f682638…`, attempt 2) reached the
transaction but never the ledger. Read
`holes/labs/wm-contract/DISCOVERY-2aef-ledger-transaction-divergence-2026-10-28.md`
(`fff168cf2`) §2 and §4 first.

## Change

In `coordinate`, after `invoke-once … :author`:

- ledger `:claimed` → append `:authored` as today;
- ledger `:authored`/`:reviewed`/`:enacted` → the author receipt must be the
  ledger-admitted one (same receipt pin and patch as the latest matching
  `:authored` transition; `resume-identity-refusal` at `125-145` already
  computes this — reuse it, do not write a second comparison). If it is,
  continue with the ledger unchanged. If it is not, `stop!` with a new typed
  refusal `:coordinator/unadmitted-author-receipt` carrying both pins,
  BEFORE the author checkpoint writes the transaction.

Apply the same rule to the review and enact skips (`503-514`, `530-545`):
a receipt the ledger did not admit stops with the matching typed refusal
(`:coordinator/unadmitted-review-receipt`, `…-enact-receipt`), never a
silent skip. Keep the change inside `meta_repair_coordinator.clj`.

## Tests (`test/futon2/aif/meta_repair_coordinator_admission_test.clj`)

Hermetic stores only. Each test must be red on today's code (state that you
saw it red):

1. ledger `:enacted` with author pin P1, author port returning a fresh
   receipt P2 → refusal `:coordinator/unadmitted-author-receipt`, and the
   checkpoint port is NOT called with a transaction containing P2.
2. ledger `:enacted`, transaction already carrying P1 (reuse) → proceeds as
   today, no refusal (this one is green before and after; say so).
3. ledger `:claimed` → `:authored` appended with the receipt pin (unchanged
   behaviour).
4. one review-phase case analogous to 1.

## Gates

- `clj-kondo --lint` and `futon4/dev/check-parens.el` on changed files.
- `clojure -M:test -n futon2.aif.meta-repair-coordinator-admission-test` plus
  the existing coordinator test namespace(s) (`ls test/futon2/aif/ | grep
  coordinator`). Skip long suites; say what you did not run.
- No reloads, no clicks, no ledger/transaction edits. Commit only your files
  by explicit path (futon2 is shared).
