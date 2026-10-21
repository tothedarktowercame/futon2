# Blueprint 4: close, ingest and discharge (phases 31-34, T, L)

Part of [00-TARGET](00-TARGET.md). Status: the classification and ledger
steps have run many times; a successful close on this route has never
happened.

## Leaves

### 4.1 Close runs the repaired terminal receipt (phase 31)

`close!` → `close-core!`; the run result goes through
`terminal-receipt/attach` (`full_loop_runner.clj:1389`). This is where the
terminal-receipt half of `b463d85cc` runs. Its acceptance test,
`test/futon2/aif/meta_loop_node_injury_acceptance_test.clj`, pins the injured
record `f9abc990` by sha256.

### 4.2 The close outcome is one T counts as success

T classifies a record `:successful-task-outcome` when it has no failure kind
and its outcome is in `#{:success :closed :grounded-change :cohort-complete}`
(`meta_terminal_injury.clj:15`, `:121-130`). To read: which outcome a
reading-policy click closes with when phases 22-30 pass. This depends on
blueprint 3, leaf 3.3.

### 4.3 Ingest writes the discharge (phases 33-34, L)

The run record is deposited and ingested into
`data/wm-meta-injuries/ledger.edn`. A successful child appends `:discharged`
for `d755c886`; anything else appends `:retry-inconclusive` and a requeue.
Witnessed for the inconclusive path seven times (ordinals 18-36).

## Open questions

- **The loop-node half of the repair.** On this route the loop-node receipt
  is `:incomplete` with four refused nodes (child 7). It does not stop the
  click, so a discharge from this route would not show that the receipt now
  reads retained evidence. That half would only be tested by a click on the
  family-selection route the injury came from. Joe to say whether
  `:discharged` from this route is enough.
- **Test runs writing the live ledger.** Ordinals 37-44 have
  `:cause-record-pin` paths under `/tmp/wm-*-test-*`. If a test JVM can
  append to the ledger, it can also append the transition a retry child is
  waiting for. This bears on whether a later `:discharged` entry can be
  trusted, so by futon2/CLAUDE.md rule 5 it should be understood before the
  next click.
