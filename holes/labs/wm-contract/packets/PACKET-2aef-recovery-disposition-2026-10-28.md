From claude-11. Clojure task, one new entry point + one test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

A supported, append-only recovery for a same-attempt ledger/transaction
divergence, so injury 2aef (repair `2f682638…`, attempt 2) can move without
any hand edit. Read
`holes/labs/wm-contract/DISCOVERY-2aef-ledger-transaction-divergence-2026-10-28.md`
(`fff168cf2`) §3–§4 first. Build the entry point and its test only. Do NOT
run it against the live ledger or transaction; that is a later, separate step.

## Decision (made by claude-11, recorded here)

Use the discovery's option (2): abandon attempt 2 and open attempt 3, whose
author transition is then recorded normally by the coordinator. Reason: it
reuses the ledger's existing retry/attempt semantics (`attempt-limit` 3, so
attempt 3 is available), and it does not require choosing which of the two
attempt-2 histories was right. Option (1) (restore the `02e9d5e7…`
transaction) would need a new restore path and a judgement about the
`87c10683…` repair that nobody has evidence for.

## Change

A function (suggested home: `meta_self_heal_production.clj`, beside
`retire-stale-transaction!` at ~`1149-1163`, which is the closest existing
shape; or `medic_disposition.clj` if the authority receipts live more
naturally there — say which and why) that, given ledger, transaction,
injury-id, repair-id, attempt and a medic authority (the same verifier the
other medic dispositions use, `meta-injury-store/transact-medic-disposition!`):

1. confirms the divergence exactly: the transaction's author receipt
   pin/patch differs from the ledger's latest matching `:authored`
   transition for the same tuple and same attempt (reuse
   `resume-identity-refusal`); otherwise refuses with a typed reason;
2. appends, through existing ledger functions, the transition(s) that
   abandon attempt 2 with a new typed reason
   `:unadmitted-transaction-receipt` citing both receipt pins and the
   transaction SHA-256, and reopens for attempt 3 (look at
   `append-retry-result` and the abandon/reopen path; extend the reason set
   rather than writing a second path);
3. moves the attempt-2 transaction aside the way `retire-stale-transaction!`
   does (byte-pinned archive, not deletion), so selection no longer finds it;
4. never writes a backdated `:authored` transition and never rewrites
   existing entries.

## Tests

Hermetic stores. A fixture reproducing the 2aef shape (ledger `:enacted`
with author P1/patch A at attempt 2; transaction with author P2/patch B at
attempt 2):

- the recovery appends the abandon + reopen, the ledger stays valid
  (sealed chain check passes), the next `coordinate` call from attempt 3 does
  not hit `:coordinator/resume-identity-refused`;
- no divergence (transaction carries P1) → typed refusal, nothing written;
- attempt already at limit → typed refusal, nothing written;
- missing/invalid medic authority → refusal, nothing written.

## Gates

- `clj-kondo --lint` and `futon4/dev/check-parens.el` on changed files.
- The new test namespace plus the existing tests of whatever namespace you
  change. Skip long suites; say what you did not run.
- No reloads, no clicks, no live ledger/transaction edits. Commit only your
  files by explicit path (futon2 is shared). Another agent (codex-13) is
  editing `meta_repair_coordinator.clj` in parallel: do not edit that file.
