# C258 — adversarial review of invoke-ledger atomicity

Date: 2026-08-31

Reviewed futon3c `3be9cc88` using only isolated temporary ledgers. The live
134.6 MB invoke ledger was neither read nor modified.

## Verdict

The same-directory atomic replacement is real, and syntactically corrupt EDN
is now loud. Two material claims do not hold:

1. schema-incomplete maps can still become a fresh empty history at startup;
2. a persistence failure after rename rolls memory back while disk retains the
   new snapshot.

The repair must not be certified as “no empty fallback on every path” or
“persistence failure rolls memory and disk back consistently” until those two
counterexamples are handled.

## Fixture results

### Loud syntax failures

The actual startup helper and direct loader both rejected these isolated files
with `invoke-jobs ledger is unreadable; refusing empty fallback`:

- empty / zero length;
- truncated mid-map;
- truncated mid-key;
- truncated mid-string;
- non-map root;
- two EDN forms.

Startup left the in-memory ledger nil on the corrupt cases. This demonstrates
that the former catch-and-default path is gone for syntax and root-type
failures.

### Valid EDN, incomplete ledger, silent empty substitution

These existing files were accepted:

| fixture | loader result | startup result |
|---|---|---|
| `{}` | default fields merged, zero jobs | persisted as the complete fresh default ledger |
| `{:version 1}` | default fields merged, zero jobs | persisted as the complete fresh default ledger |
| `{:jobs {}}` | default fields merged, zero jobs | persisted as the complete fresh default ledger |

Thus the loader validates “one map,” not the ledger schema. A partial but
syntactically valid map is indistinguishable from an intentionally fresh
ledger and is rewritten into one. The catastrophic absence-to-empty shape is
narrower than before, but it is not eliminated.

Required falsifier: an existing file must carry and validate all required root
fields and their consistency (`:version`, `:next-seq`, `:job-order`,
`:trace->job`, `:jobs`); only a genuinely absent file may create the default.

## Atomic placement

The persistence hook observed the target and temporary snapshot with identical
parents and identical `FileStore` values. The actual `ATOMIC_MOVE` path
completed on the fixture filesystem. This supports same-directory,
same-filesystem atomic replacement on the deployed provider; it does not claim
that every filesystem provider supports atomic moves.

## Writer serialization

Two concurrent same-process mutations were run, with the first sleeping inside
its transition. The final memory and disk ledgers both contained the `slow`
and `fast` jobs and were exactly equal. This supports the stated
**process-wide** writer-lock claim. It is not an inter-process lock and should
not be described as one.

## Rollback boundary counterexample

The existing focused test proves rollback for a failure before replacement.
The commit protocol has a later fallible step: forcing the parent directory
after `Files/move` has already replaced the authority.

An isolated POSIX fixture removed directory read permission at the
`:temp-forced` hook while retaining write and execute permission. The atomic
rename succeeded; opening the directory for the subsequent READ/force failed.
Observed result:

```
failure      invoke-jobs ledger persistence failed
memory-jobs  #{}
disk-jobs    #{renamed-before-dir-force}
memory=disk  false
```

The update caller receives a failure and the in-memory atom rolls back, but the
visible disk authority is already the new ledger. A single rollback policy
cannot cross the rename commit point. Failures before rename may restore
memory; failures after rename require reconciling memory to the visible disk
state or entering a typed unavailable/fatal state. Treating both as the same
exception produces split authority.

## Focused checks

```
clojure -M:test -n futon3c.transport.invoke-ledger-atomicity-test
```

Result: 3 tests, 6 assertions, exit 0.

The independent probe additionally demonstrated the malformed-input matrix,
same-store placement, concurrent serialization, and the post-rename failure
above. No repository-wide suite or gate was run.
