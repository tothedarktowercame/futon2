From claude-11. Clojure repair, one function + regression test. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Repair the producer defect codex-13 found
(`holes/labs/wm-contract/DISCOVERY-retry-attribution-refused-2026-10-28.md`,
`d9682d0bf` §3–4). In `src/futon2/aif/meta_repair_coordinator.clj:342-355`,
line 348 takes the requeue's patch from `(receipt-for transaction :author)`,
the shared transaction's latest author receipt, not from the injury's own
authored ledger transition. Injury 2aef's ordinal-70 requeue got retry id
suffix `87c10683…` (repository head at the time) while its `:patch-sha` /
`:integration-commit` are `02e9d5e7…`; both retry children were then
correctly refused `retry-attribution-refused`.

## Change (smallest)

At `:347-354`: obtain `patch-sha` from the exact injury's latest `:authored`
transition for this `repair-id` and `attempt` (the same selection
`meta_self_heal_production.clj:1394-1422` uses for attribution), require it to
be a 40-hex SHA, and refuse with a typed reason if it is absent or disagrees
with the enacted entry's `:patch-sha`. Mint `new-id` from that value. Do not
use the transaction's generic author receipt. Leave the readiness comparison
unchanged.

## Test

Regression: a transaction whose latest author receipt carries a different
patch (another repair, or a later HEAD) than the injury's authored transition;
the requeued retry id must contain the injury's authored SHA, and the
production attribution check (`meta_self_heal_production` readiness) must
accept it. Show the test fails against the old line.

## Gates

kondo, check-parens; run only the new/changed test namespace(s); commit only
changed files by explicit path (futon2 is shared). No reloads, no clicks, no
ledger edits (ordinal 70 stays as it is; its disposition is a separate,
authority-backed step after review).
