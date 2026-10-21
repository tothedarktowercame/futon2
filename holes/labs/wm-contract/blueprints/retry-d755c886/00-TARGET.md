# Blueprint 0: validating the d755c886 repair

claude-2, 2026-10-21. Written in the form claude-14 uses for apm-lean
(`apm-lean/holes/labs/blueprints/*/BLUEPRINT.md`): a target, the leaves it is
assembled from, a check for each leaf, and truth checks. This is a reading
aid and a plan, not a change to the specification.

## The target

Injury `d755c886` (`:invalid-close-outcome`, ledger ordinal 4) came from run
`f9abc990` on 2026-10-19. Its repair, futon2 `b463d85cc` (zai-2, 2026-10-20),
changed two things:

- `wm/loop_node_exercise.clj`: the loop-node receipt reads the decision
  evidence the runner retains.
- `wm/terminal_receipt.clj`: an action receipt with no `:click-id` or no
  `:outcome` records a typed absence.

It was enacted at ordinal 16. The ledger moves it to `:discharged` only when
a retry child (an ordinary click queued by the retry port) ends with a record
that T classifies `:successful-task-outcome`: no failure kind, and an outcome
in `#{:success :closed :grounded-change :cohort-complete}`
(`meta_terminal_injury.clj:15`, `:128`). Every other ending is recorded as
`:retry-inconclusive`.

So "the repair is validated" means one ordinary click gets from start to close
without failing, and its close goes through the repaired code.

## Why the retries look like random fixes

Seven children so far. Each stopped at a different stage:

| child | run | stopped at (phase) | injury opened |
|---|---|---|---|
| 1 | `881a8839` | `:selection-world` (13) | `821c15d0` `:no-selection` |
| 2 | `531f4c1f` | `:initialization` (0c) | `75747f06` `:required-checkpoints-missing` |
| 3 | `bcf648e3` | `:cascade-resume` (14.1) | `42fd2c21` `:open-cascade-review-answer-invalid` |
| 4 | `fdbf2abb` | same as 3 | `42fd2c21` again |
| 5 | `35176c9e` | `:cascade-execution` (14) | `a173047e` `:untyped-failure` |
| 6 | `fcf06c3f` | same as 3 | `42fd2c21` again |
| 7 | `752b48a7` | `:construction` (20) | `9c998f2e` `:fold-output-invalid` |

(Phase numbers are from `logic-model/phases.md`; runs and injuries from
`data/wm-meta-injuries/ledger.edn`, ordinals 17-36.)

Two facts explain the pattern:

1. The injured run was not on the route the children take. Run `f9abc990`
   has no mention of `:wm/selected-outer-cascade-policy-v1`; child 7's record
   names it as its `:selection-route`. Every child selected a reading policy
   through the sealed outer selector.
2. No run record from October on that route has closed with
   `:grounded-change` (searched `data/wm-runs/tick-run-record-2026-10-*.edn`).
   A child has to be the first run on that route to get through phases 14 to
   31.

So each retry click is doing two jobs: testing the repair, and being the
first run through a route that has never closed. Each click finds the next
stage on that route that does not work, and each fix moves the stop one stage
further. Nothing in the fixes is random; what has been missing is a map of
the stages still ahead.

## The leaves

Each leaf is one stretch of the phase table. For each one, the blueprint
states what must hold for a reading-policy child, the cheapest check that
does not spend a click, and the status. A check is one of: a reading of the
code, a replay of the last child's recorded inputs through the serving JVM's
own functions (futon2/CLAUDE.md rule 4 allows this to find defects; it
certifies nothing), or a test.

| blueprint | phases | status |
|---|---|---|
| [01 selection to cascade-ready](01-selection-to-cascade-ready.md) | 0a-14.1 | passed live by child 7 |
| [02 construction](02-construction.md) | 15, 19-20 | fixed in `8dd7c2cff`, not merged or loaded |
| [03 author to grounding](03-author-to-grounding.md) | 16-18, 22-30 | never run on this route; not yet read |
| [04 close and discharge](04-close-and-discharge.md) | 31-34, T, L | partly read; two open questions |

The click is the assembly. The proposal is to spend the next retry click only
when every leaf up to close has a check behind it, so that the click tests the
repair rather than finding the next stage.

## Truth checks and open questions

- **Does a reading-policy child test the repair at all?** Half of it, as far
  as I can tell. `terminal-receipt/attach` runs on every close
  (`full_loop_runner.clj:1389`), so the terminal-receipt half is exercised by
  any child that reaches close. The loop-node half is not: in child 7 the
  loop-node receipt is `:incomplete` with all four nodes refused (`:R6`
  `:candidate-space-not-retained`, `:R13`, `:R14`, `:CTAU-CLASS`). Since the
  2026-10-19 frontier ruling, `require-complete!` records this and does not
  stop the click (`loop_node_exercise.clj:134-140`), so it did not stop child 7.
  But a discharge from this route would rest on the terminal-receipt half
  only. Whether that counts as validating `b463d85cc` is a question about
  what the ledger's `:discharged` should mean, so it goes to Joe.
- **Test runs are writing to the live ledger.** Ordinals 37-44 open and
  observe injuries whose `:cause-record-pin` paths are
  `/tmp/wm-run-record-test-*` and `/tmp/wm-total-run-record-test-*`, which
  are test fixtures. A test JVM is appending to
  `data/wm-meta-injuries/ledger.edn`. Not investigated further here.
