# C24 — fold-turn quarantine and malformed-EDN audit

Delivered 2026-08-31 by `wm-verbs`.

## Fold-turn diagnosis and disposition

The live `futon6/data/fold-turns` directory contains 18 legacy records. Ten
never reconstructed. Across those ten, every stored per-pattern prose
digest matches the current flexiarg, each stored ψ digest matches its recorded ψ,
and the original `fold-prompt` had not changed. Git history shows no later
edit to the tracked records' mission, ψ, or pattern-id inputs. The prompt digests
therefore did not bind the prompt reconstructible from the recorded fields; this
is not present-day prompt/prose drift and cannot honestly be repaired by refreshing
the digest.

On 2026-10-13, the remaining eight began rejecting after futon2 `86aef779`
changed `fold-prompt` from the legacy answer shape to the enriched proof-output
contract. Reconstructing with the exact `86aef779^` implementation reproduces
all eight stored hashes; current reconstruction reproduces none. Their source
pins did not drift and the corpus is not corrupt: the consumer algorithm changed.
Because replay keys the exact current prompt, refreshing their hashes would bind
old answers to a prompt they never answered. They are quarantined instead.

`fold-turn-quarantine.edn` explicitly excludes all eighteen by the triple
`[:fold-turn/id :prompt-sha256 :reason]`. The files remain untouched. Any changed
id, digest, or rejection reason stops matching quarantine and becomes a loud
rejection again. The loader now reports 0 accepted, 18 quarantined, 0 rejected.
Replay receives no legacy records. `actuator-a3` also consumes
the corpus as read-only structural specimens; it may inspect the raw records
with `:quarantine/status :prompt-not-reconstructable` attached, but cannot feed
them to the replay table.

## Malformed-EDN audit

Count before edits: four test fixtures in `test/**` intentionally rely on EDN
reader failure, including the C34 fixture; none in `checks/*.clj`. All four were
executed directly with `clojure.edn/read-string` and throw:

- `fold_llm_test.clj`: `"}{ not edn"`
- `fold_escrow_test.clj`: an unterminated map
- `pattern_registry_test.clj`: an odd-entry map
- `enact_read_boundary_test.clj`: an unterminated vector

No weak fixture was found, so no mutation was changed. None of C22's six checks
is affected: all six mutate already-parsed structures semantically (authority SHA,
membership verdict, drawn-edge presence, channel presence, absence handling, or
stored-F placement); none depends on parser rejection.

Canonical invocations:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.fold-escrow-test -n futon2.aif.enact-read-boundary-test -n futon2.aif.fold-llm-test -n futon2.aif.pattern-registry-test
bb -cp src:. checks/fold_turn_quarantine_check.clj
bb -cp src:. checks/fold_turn_quarantine_check.clj --negative
```

Exit convention is C16: `0=pass`, `1=ordinary failure`, and
`2=mutation-slipped`.
