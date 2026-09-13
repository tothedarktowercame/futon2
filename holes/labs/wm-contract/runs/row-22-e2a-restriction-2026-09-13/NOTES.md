# Row 22 E2a portfolio restriction

The public function accepts only the E1 resolver configuration and re-reads all
five independently pinned sources through `resolve-and-map`. Caller-supplied
selected ids, scope, identity, accounting, and actions are not inputs. The E1
response is checked again for exact support coverage, unique and disjoint
selected/rejected occurrences, ordered accounting, rank and action preservation,
and nonempty approval.

The isolated readback retains three full occurrences, including two equal
semantic actions with different occurrence ids. Canonical R11 approves the
first and third occurrences; E2a preserves their R6 order/action bytes and
retains the excluded second occurrence. Replay re-resolves the sources and is
identical.

This is isolated mechanism evidence only. E1 production authority still
refuses, and no selector consumes `:approved-support` yet. The prospective seam
is documented in `SPEC-row22-e2a-runtime-seam-2026-09-13.md`. R6 scoring and
posterior must later operate over exactly this domain; E2b must separately join
the selected occurrence to exact enactment. No action was selected or enacted
by this packet.

Pins:

- source `machine_portfolio_restriction.clj`:
  `316ddd364e2deaea98172558148fd770c2d5d4a930e32fb8e4825f454b6c51ab`
- test `machine_portfolio_restriction_test.clj`:
  `4edfda36abb71ed16295b685fc007a9d7237081a6ec1cdf80be8aad9a99b5ba0`
- seam specification: `46d9a2c27439529b955e953da3efa47d48ee0e1e0d7511ef026526381680b814`
- readback: `e15f3cbb248875aba0a9f662ca5ac995e5c0fabbcbe4af183b094f1925dee8ec`
