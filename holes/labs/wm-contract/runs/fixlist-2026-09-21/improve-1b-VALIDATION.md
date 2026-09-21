# improve-1b — record-only attempt delivery counts

Branch `fix/narrative-improve-1b`, base `4765cab4`.

The declared contract is `resources/wm/attempt-learning-contract.edn` (beside
the illustrative prior, outside the directory scanned as cascade sources).
It estimates exactly: **this selected cascade delivers this declared effect
when dispatched through this route**. It does not claim pattern causality,
individual firings, or a learned production transition.

## Admission and retained evidence

`attempt-learning/receipt` makes a v2 learning receipt while keeping the v1
prospective API available for historical replay. It calls the existing
`d-predecessor-task-authority/verify-observations-v2` with the expected pins
and read-job port, rather than trusting an externally asserted admitted flag.
The close path passes its actual context and job reader. The D-task source
bytes are still checked against their retained digest before parsing.

An admitted row requires a v2 action occurrence, the same selected cascade,
target and dispatch route, a declared wanted effect, an exact normalized
selection belief in which that effect has zero mass, and positive model
prediction. The latter is recomputed with the existing rollout and compared
with the frozen prediction. Its Boolean endpoint must agree with the freshly
verified v2 observation at the same artifact revision. Occurrence, declaration
meaning, source, signed observation and execution verification remain in the
receipt. Missing/refused evidence is never coerced to false.

The clock is the declared post-build artifact observation, not an invented
horizon tau or pattern firing. The before observation can remain missing:
this contract expressly uses **absence in selection's model**, not a claim
that an independently measured pre-state exists. All existing missing,
unselected, already-present, duplicate, revised-meaning and invalid-prior
holds remain. New mismatched occurrence/action/route/prediction/artifact and
unverified endpoints also hold.

The 1789964661 updater remains **held `:occurrence-v2-required`**. Its retained
occurrence is `:wm/action-transition-occurrence-v1`. The earlier signed v2
projection's ability to verify a legacy occurrence does not upgrade that
occurrence to v2. No historical identity bytes or measurements were changed.
The frozen reference fixture from improve-1a supplies this regression case.

## Ledger and model separation

`learning-trial-ledger/record!` appends admitted rows to
`data/wm-learning-trials/attempts.edn` (root configurable by the runner's
`:learning-trial-ledger-root`, explicitly temporary in tests). One row is one
**cascade/effect/occurrence at attempt grain**, even if multiple patterns mention
that effect. The identity uses canonical action-identity serialization and the
occurrence's action/transition identities; the grain is versioned.

Each append retains the complete trial, contract, Boolean, and a unit success
or failure increment. An OS file lock plus an in-process mutex covers reading,
deduplication and append; writes are synced. Existing bytes are not rewritten.
Repeated identities become `:held :duplicate-replay` with `:counted? false`.
Changed declaration meaning for the same target/cascade-patterns/effect/route
family is held `:revised-meaning`; contradictory replay is also held. A malformed
ledger is not repaired or ignored. Held-only receipts do not create a ledger
file. A close replay can therefore re-retain a duplicate receipt without
counting another trial. Append failure propagates instead of claiming a count.

No planning or parameter consumer reads this ledger. Only its writer reads old
entries for deduplication and meaning checks. A counted row is acknowledged in
the close receipt, also inside the existing manifest-admitted
`<attempt>/retained/token-outcome.edn`; no extra attempt-top-level file is added.
An append precedes close sealing, so a later close failure can leave an auditable
endpoint count; replay deduplicates it. Admission concerns the independently
verified build endpoint, not a claim that cohort close already succeeded.

The numeric Beta(9,1) prior remains explicitly illustrative. Attempt receipts
show a **single-attempt summary**, not a cumulative learned posterior:
positive → Beta(10,1), mean 10/11; negative → Beta(9,2), mean 9/11.
Neither enters rollout. The earlier per-firing sensitivity shadows are retained
and explicitly marked `:illustrative-per-firing-sensitivity-only` with no
attempt-parameter consumption. They are not treated as an attempt-grain model.
Narrative identifies the cascade and prints the record-only illustrative
*delivery mean* for an admitted row; it also handles nil refusal reasons.

Both new click-path namespaces register with load-identity and are in its
required-source list. Production theta, Q, G, candidate scores and posterior
remain unchanged. The test takes printed actual ranker/selection snapshots
before and after admission plus real ledger writes and asserts byte equality;
the posterior also equals the frozen reference record.

## Tests, baseline and hermeticity

Synthetic tests use actual temporary Git repositories, occurrence minting,
source capture/declaration digests, author/reviewer prompt bindings, the real
producer, signed verifier and model rollout. Only the external job read port
is a retained fixture map. Both an observed created artifact and an observed
missing effect are admitted. This preserves the existing execution verifier's
positive-evidence prerequisite; it does not weaken that verifier to admit an
unestablished all-negative execution.

Assertions cover one success plus one failure increment, byte-identical ledger
on replay, revised-meaning refusal, all earlier held negative classes, missing
job authority, wrong artifact, inconsistent prediction, historical v1 hold,
and unchanged production scores/posteriors. The runner helper test invokes the
actual retaining function with these real signed inputs and checks admitted
counts, retained bytes and replay deduplication. Its five assertions execute
successfully even in the worktree.

Base `4765cab4`, isolated detached worktree
`/home/joe/code/futon2-improve-1b-baseline`: copied the new test namespace only;
fixtures/dependencies already exist there. API resolution is dynamic so the
new acceptance assertions fail rather than stopping at a missing namespace.
Fresh result: **3 tests / 33 assertions, 14 failures, 0 errors**. Examples:

```
expected: (= 2 (count rows))
  actual: (not (= 2 0))
expected: (= #{true false} (set (map :after-observation rows)))
  actual: (not (= #{true false} #{}))
```

Ledger roots in the pure tests and runner helper are temporary; the runner
namespace fixture also binds the default ledger root to its temporary store.
Before/after the test batch, **both canonical learning-ledger directories were
absent** (zero files/counts): `/home/joe/code/{futon2,futon3c}/data/wm-learning-trials`.

The broader canonical data trees were counted as requested: **305061 → 305062
files**, before registry publication. Thus whole-tree equality is NOT claimed.
The newly observed file was
`futon2/data/wm-d-task-enactment/action-ab3ea84b-6fa2-4320-a7bd-a903251b9c03.edn`,
with run `close-retention-success`, action time `2026-09-21T17:07:38.717470801Z`
and repository `/tmp/runner-token-initialization2902752186225040951` — a
canonical D-task test residue, not a learning-ledger write. The phase log and
visibility file also changed concurrently. No shared checkout/data cleanup was
performed. Counts are retained at `/tmp/improve-1b-store-{before,after}.txt`.

## Fresh gates and owner-run boundary

Own worktree, OpenJDK 21.0.11 / Clojure 1.11.1. No live JVM evaluation, clicks,
or Lean builds. Exact command for each namespace (and the base replay):

```
clojure -M:test -m cognitect.test-runner -n <namespace>
```

| Namespace | Tests / assertions | Result |
|---|---:|---|
| futon2.aif.attempt-learning-test | 3 / 33 | pass |
| futon2.aif.run-narrative-test | 23 / 117 | pass |
| futon2.aif.load-identity-test | 3 / 12 | pass |
| futon2.aif.full-loop-runner-test | 187 / 585 | 0 failures, 88 errors, all canonical-source drift |

Logs: `/tmp/improve-1b-{attempt,narrative,load,runner,base}.log`.
The full grounded test follows the owner's corrected fixture: preserve
`:selection-law :applied`, and read the durable `<attempt>/007-closed.edn`.
It checks the v2 receipt, unchanged posterior and `closed-execution`. Its full
close assertions remain **pending canonical execution**: the source guard was
not bypassed. Claimed green helper assertions are distinct from that pending
full-run test.

Static commands:

```sh
files=(src/futon2/aif/{attempt_learning,learning_trial_ledger,full_loop_runner,run_narrative,load_identity}.clj test/futon2/aif/{attempt_learning,full_loop_runner,run_narrative}_test.clj)
clj-kondo --lint "${files[@]}"
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- "${files[@]}"
git diff --check
```

Kondo: 0 errors / 0 warnings (existing informational runner `str` diagnostic).
Parens: `OK`. Diff check clean. Registry publication follows the implementation
commit; its accepted command spelling is `clojure -M:test -n
futon2.aif.attempt-learning-test`, equivalent to the separately run command above.
