# META self-heal must be a durable repair transaction, not an ordinary task

**Status:** OPEN — ARCHITECTURAL REPAIR REQUIRED

Parent: A-self-heal

## Observed failure

The 2026-10-19 live proof campaign did not demonstrate one end-to-end
self-repair.  Instead, `A-self-heal` was routed through the ordinary task
pipeline and exposed one incompatible gate per click:

- ordinary R6/R13/R14/CTAU admission;
- ordinary pattern-fold construction;
- loss of the injury after the failed algorithm attempt;
- D-task token-carry authority at author dispatch;
- loss of the author-dispatch injury because it was not on a stage allowlist.

The corresponding local repairs are `97eab2ee8`, `fa43dd757`, `5a5b421bc`,
`42bfb4bb5`, and `95145f093`.  They are evidence of the boundary defect, not
evidence that autonomous repair works.  In particular:

- `97eab2ee8` bypasses ordinary loop-node requirements for an algorithm;
- `42bfb4bb5` supplies an empty D-task token universe, making that ordinary
  authority vacuous for the repair route;
- `5a5b421bc` and `95145f093` recognize injuries through failure-stage lists,
  so a later unlisted failure shape can still erase the repair obligation;
- `fa43dd757` adapts repair work to the ordinary construction fold.

Pinned campaign examples:

- `data/wm-runs/tick-run-record-2026-10-19-bd1f106d-5bdc-4b9d-8350-5d9c9bd6832f.edn`
  selected `A-self-heal` and failed at construction;
- `data/wm-runs/tick-run-record-2026-10-19-be38a3a9-92bc-4084-b7a6-1c4d2fcfe84a.edn`
  selected `A-self-heal` and failed at author dispatch;
- `data/wm-runs/tick-run-record-2026-10-19-e86d38df-e527-4648-bdfe-cce61133330f.edn`
  subsequently returned to ordinary selection and abstained.

## Required design

META owns a dedicated repair transaction.  Ordinary task selection is entered
again only for the attributable retry after repair enactment.

The injury ledger is append-only and gives each cause a stable identity.  Its
allowed transitions are:

```text
open(cause-record-pin, stage, kind, detected-click)
  -> claimed(repair-id, algorithm, fresh author/reviewer sessions)
  -> authored(patch-sha, worktree)
  -> reviewed(verdict, reviewer-sha)
  -> enacted(reload-receipt or integration-commit)
  -> discharged(retry-click, outcome)
     | reopened(retry-click, new-cause-pin, attempt+1)
     | abandoned(reason)
```

The repair work order contains the injury id, pinned failing record, diagnosis
evidence, acceptance test, and named fresh sessions.  Its author/reviewer
dispatch may use Agency transport, but it must not be scored as an ordinary
policy, exercised through ordinary loop nodes, constructed by the pattern
fold, or certified using D-task token belief.

## Invariants

1. A run-record append cannot remove or downgrade an unresolved injury.
2. `open` or `reopened` leaves the active set only through a transition that
   names both a patch SHA and its retry click, or through typed `abandoned`.
3. While any injury is active, META admits only the self-heal transaction;
   `A-self-heal` does not compete with ordinary work on G.
4. The repair id is unchanged across claim, author dispatch, reviewer
   dispatch, enactment/reload, and retry.
5. A successful unrelated click cannot discharge an injury.
6. Every attempt records fresh session identities and an attempt number.
7. Exhausting the configured attempt limit yields typed `abandoned`, never a
   silent abstention.

## Bounded implementation order

1. Implement the pure append-only ledger and total transition validator.
2. Convert terminal machine failures into `open`/`reopened` entries.  Default
   a terminal algorithm/apparatus failure to injury; explicitly enumerate
   non-injury reasons rather than injury stages.
3. Implement the dedicated transaction coordinator and sealed repair work
   order, with no dependency on loop-node, construction-fold, or D-task
   admission.
4. Thread repair identity through dispatch, review, enactment, and retry.
5. After the dedicated path is proved, remove or quarantine the ordinary-route
   bypasses above and invert their tests to require refusal.
6. Separately make click acceptance return a click id before asynchronous
   execution, so a client timeout cannot cause stale `last-result` reuse.
7. Rename or constrain the observation-only launcher so it cannot be mistaken
   for an autonomous repair executor.

## Acceptance evidence before another live click

- Property test: arbitrary run-record appends never reduce the open-injury set
  without a valid discharge transition.
- Table-driven replay of the 2026-10-19 campaign records, proving every machine
  failure opens or reopens the expected injury without stage allowlists.
- An isolated-JVM transaction harness with fake `wm-author`, `wm-reviewer`, and
  `wm-repair-reviewer` seats and a throwaway worktree.
- Injected failures at detection, author dispatch, review, enactment/reload,
  and retry; each must end in the correct durable state.
- One successful harness run reaching `discharged`, followed by ordinary work
  selection, with the same repair id and patch SHA throughout.
- Replay of a real recorded click input and a trace-shape comparison against
  the isolated harness.

No further live click is warranted until these finite gates pass.  The fake
seats prove transaction shape; a later single live click tests agent
competence and production integration.

## Provenance

Requested by Joe after the five-click campaign exhausted its authorization
without autonomous repair.  Architectural diagnosis supplied by `claude-5`
in Agency job `invoke-1792382724781-17058-f65d240b`, after review of the five
commits and `meta_injury_observation.clj`.

## Completion criteria

- Append-only injury ledger with total transition validator — check: futon2 src/futon2/aif/meta_injury_ledger.clj + test deftest arbitrary-nondischarge-appends-never-reduce-active-set (meta_injury_ledger_test.clj:101). **Met.**
- Campaign replay opens/reopens injuries without stage allowlists — check: deftest campaign-record-appends-cannot-erase-injury (meta_injury_ledger_test.clj:119) over the three pinned 10-19 records. **Met.**
- Dedicated repair transaction with sealed work order, discharging/reopening/abandoning correctly — check: meta_repair_coordinator_test.clj deftests at lines 102, 141, 177, 478. **Met.**
- Repair identity held across claim/dispatch/review/enactment/retry — check: deftest complete-valid-transaction-discharges-only-on-attributable-retry (meta_injury_ledger_test.clj:73) and retry-* deftests in the same file. **Met.**
- The five ordinary-route bypasses are removed or quarantined and their tests inverted to require refusal — check: git log for the inversion of 97eab2ee8, fa43dd757, 5a5b421bc, 42bfb4bb5, 95145f093. **Not started.**
- Click acceptance returns a click id before asynchronous execution — check: the acceptance route's response carries a click id on a real click; no stale last-result reuse. **Not started.**
- Real recorded click replayed with trace-shape comparison against the isolated harness — check: a recorded replay test citing a real click input file. **Not started.**
