# Independent review of R1 — codex-3

**Verdict: reject against the full requested classification contract, pending
resolution of F1 below.** The narrower R1 change correctly handles interrupted
temp writes when valid lock metadata survives. I found no path by which it
adopts a temp or masks damage to a committed chain with intact authority.

Subject: futon2 `e38ea7e5153ce30dc094106a9ab5a904d12d3165`.
Review commission: `invoke-1789505882635-21236-0b88b14e`.
Decision context read: p4ng `b09e3dd`, Q-D–Q-G.
Store source, committed tests and PROTOCOL.md were compared with that exact
subject before and after review; no differences. No store source was changed.

## F1 — Requested temp-only case depends on an unstated valid-lock precondition

The review request says: “Missing genesis with only temp files present should
read as :pending-recovery, not :model-not-established.” With valid existing
`.writer.lock` metadata, R1 does return pending recovery. With literally only
temp files present and no `.writer.lock`, the public reader instead returns:

```edn
{:status :damaged :reason :parse-failure}
```

Reproduction, entirely in temporary directories:

1. Create a directory and an empty `INIT.edn.tmp`; create no other files.
2. Open its handle using a payload validator returning :ok.
3. Call `read-store`.

The same occurs with only `snapshots/snapshot.tmp` and its parent directory.
Both reproductions and the matching valid-lock controls are retained as the
first four cases in `probes.stdout`; executable source is `probes.clj`.
Run from futon2:

```sh
clojure -M:test holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/r1-review/probes.clj
```

**Cause:** `locked` at source lines 68–79 creates `.writer.lock` using CREATE,
but writes its EDN only when that file is the directory's sole entry. A temp
makes the directory non-pristine, leaving the newly created lock empty.
`inspect` at lines 131–134 then strictly parses that empty file before reaching
the missing-genesis/preps branch at lines 143–145.

This is an inherited interaction with my original lock-bootstrap code, not a
new regression introduced by R1. The probe simulates a filesystem state; it
does **not** establish that a real power failure on this ext4 volume creates
that state. Normal initialized transactions retain a valid lock, and the
existing protocol prohibits removing that lock. Those qualifications matter:
R1 is correct if the requested case presupposes surviving valid lock metadata,
but that precondition is absent from the unconditional review case.

**Required resolution:** reconcile the requested missing-genesis/temp-only
classification with the trusted lock-metadata boundary, then retain a control
for absent versus existing malformed lock metadata. Either the governing
contract must explicitly classify absent/malformed coordination metadata as
damage with a specific reason, or the implementation must distinguish newly
created lock metadata from corruption without weakening strict reading or
rewriting an existing damaged record. Do not merely exempt all empty locks or
auto-recover the store. This review makes neither change and authorizes no
recovery. Rejection is limited to this unresolved contract case, not a request
to revert the working temp exclusion.

## Other requested checks

### Diff and branch classifications

- Read the requested `git show e38ea7e5 -- …` diff and the original R1 review.
- Root and snapshots preparations are classified by the `.tmp` suffix and
  retained in `preps`. Every non-temp file is still parsed strictly.
- Missing genesis with valid lock metadata and temp-only preparations:
  **pending-recovery**, including empty/unparsable temp bytes.
- Matching INIT + genesis, no completed initialization marker, no HEAD or
  final snapshots, and `HEAD.edn.tmp`: **initialization-incomplete**, for empty,
  partial and multi-form temp bytes. This is the intended more specific
  initialization state; the temp never establishes an empty HEAD.
- The committed names are fixed (`HEAD.edn`, `genesis.edn`, initialization and
  declaration records, `snapshots/<seq>.edn`). None can be chosen as `.tmp` by
  an operation. Renaming HEAD to HEAD.edn.tmp still reports damaged/missing-head;
  renaming a committed tail to .tmp reports damaged/missing-head-snapshot.
  Missing genesis with surviving records remains a refusing recovery state.

### Precedence and prepared final snapshots

- Empty temp plus invalid committed snapshot/HEAD bytes: damaged/parse-failure.
- Empty temp plus changed declaration: damaged/declaration-hash-mismatch.
- The committed suite also verifies committed hash mismatch beats temp recovery.
- A valid final snapshot beyond HEAD remains pending with no temp, and remains
  pending when an empty HEAD temp is added. The old HEAD stays authoritative.
- A malformed final snapshot beyond HEAD still reports damaged/parse-failure,
  even with an empty temp. R1 exempts preparations by suffix, not every file
  outside the committed prefix. No prepared final snapshot is hidden or adopted.

### Protocol amendment

The two amended commit-matrix rows correctly describe partial snapshot/head
temp writes under the valid-metadata precondition. The dated note accurately
identifies the old behavior. Clarify that the blanket “temp means pending”
wording is subject to (a) damaged non-temp metadata, including F1, and (b) the
more specific initialization-incomplete branch. The existing initialization
section already explains (b); the amendment should not be read to replace it.
The earlier “committed damage always takes precedence” statement remains true
for the intact committed-authority branch examined here.

## Gates and probes

Commands ran directly, without shell pipelines, sequentially in short-lived
processes. Outputs and actual exit statuses are retained in this directory.

| Gate | Exit | Result |
|---|---:|---|
| clj-kondo (store, committed tests, review probes) | 0 | 0 errors, 0 warnings |
| check-parens (same files) | 0 | OK |
| `clojure -X:test :nses '[futon2.aif.work-target-store-test]'` | 0 | 16 tests, 292 assertions; 0 failures/errors |
| Review probes | 0 | 15 diagnostic cases reproduced; see classification above |
| Existing mutation runner, one separate JVM | 0 | 17 outcomes match retained receipt exactly |

`gate-receipts.json` records argv, cwd, timings, source hashes and exit statuses.
`run-review.py` reproduces these runs. The diagnostic probes assert the reported
observations, including F1; their zero exit is not a claim that F1 satisfies the
requested pending-recovery behavior. All filesystem mutations were confined to
isolated temp stores and cleaned up. No shared JVM or production store was used.

## Mutation confirmation

Re-ran the existing `p1b-1-review/run_mutants.clj`, without regenerating or
changing its source files. The final summary is semantically **identical** to
`p1b-1-review/post-fix-mutation-summary.edn`, including each failure count.
`mutation-comparison.edn` retains the independent equality check.

**14 killed, 3 survived; 17/17 behaved as predicted.** The survivors are:

- S05 non-atomic move: tests do not distinguish it on this filesystem. Source
  inspection confirms the actual implementation still uses ATOMIC_MOVE.
- S14 omitted duplicate-operation-ID check: existing tests do not reach the
  necessary consistently rehashed history.
- S15 omitted empty-head envelope check: existing tests do not reach that
  altered-envelope case.

These are test-reach limits, not evidence those checks are unnecessary. R1's
reversion mutant S17 is killed with 13 failing assertions. The original reload
is clean (16 tests / 292 assertions). None of those mutations tests the absent-
lock case F1; the mutation receipt does not resolve it.
