# RUNNER-SUITE-D — which commit broke futon2.aif.full-loop-runner-test: none; the failure tracks the live futon3 checkout

Discovery packet, kimi-5, 2026-09-24. Read-only bisect; no code changed, no
clicks, no shared-JVM loads, no writes under data/. The bisect worktree
(`/home/joe/code/wt-bisect`) was removed after the runs.

## Answer first

**No futon2 commit broke these tests.** `close-retains-token-mismatch-before-manifest-freeze`
fails identically at the commit that ADDED it (`cab63449`), at `cd1f4f84`
(`cabc8e67`'s parent), at `cabc8e67`, and — per claude-10's report confirmed
by claude-8 — at clean `c6d1e542`. Every run gives the same signature:
`admitted-token-initialization-survives-runner-close` and
`close-retains-token-mismatch-before-manifest-freeze` fail with

```
expected: (= :grounded-change (:outcome result))
  actual: (not (= :grounded-change :build-failed))
```

followed by the receipt-shaped assertions collapsing
(`(= 3 (count (:tokens receipt)))` → actual `0`, receipt `nil`). The failure
is not a regression in the named range (cabc8e67, 4a2ba931, 97a84770,
54295ca0, f38e20b2, 95aa28b2, 4dd54848, c6d1e542); it predates all of them
and its presence depends on state outside futon2's git tree: **the bytes of
the live sibling checkout `/home/joe/code/futon3`**.

## Mechanism (proven, not inferred)

1. `cascade-sources/read-receipt-source` (src/futon2/aif/cascade_sources.clj)
   re-reads every interpretation receipt's `:source` file, resolved against
   the hardcoded live root `futon2.aif.observation-checks/repo-root =
   "/home/joe/code"`, and REFUSES the whole load when the file is unreadable
   (`:interpretation-source-unreadable`) or its bytes differ from the pinned
   `:sha256` (`:interpretation-source-hash-mismatch`). The runner calls
   `load-declared` on the author-prompt path
   (`full_loop_runner.clj:1967`) and at judgement assembly
   (`war_machine.clj:7050`, `(or (:cascade-sources-dir judge-opts)
   cascade-sources/default-dir)`); a refusal there surfaces as
   `:outcome :build-failed` (observed ex-data: `:error
   :invalid-cascade-source`, `:failure-kind :close-exception`).
2. The pins point at futon3 library files. The live futon3 checkout moved:
   `library/apparatus/one-authority-per-question.flexiarg` hashed
   `42371c5d…` at futon2's older pins and `b367ff7d…` today (futon3 21a9199,
   "Repair @why and @how…"; futon3 HEAD at measurement 9c248d5,
   2026-09-24T16:11Z). futon2's declared sources were re-pinned in 031d9452
   ("re-pin five drifted cascade sources"), which is why a bare
   `load-declared` SUCCEEDS at `cd1f4f84`+ (verified directly: `:ok 17`).
3. But both failing tests build their decision from
   `futon2.aif.token-outcome-test` fixtures —
   `test/fixtures/new-wanted-token/1789964661.edn`,
   `test/fixtures/occurrence-1789964661.edn` (and
   `eig-source-remaining/updater-only-declaration.edn`) — which carry the
   receipt with `:source {:path
   "futon3/library/apparatus/one-authority-per-question.flexiarg"
   :revision "7fc6a050…" :sha256 "42371c5d…"}` VERBATIM (visible in the
   failing assertion diff at every sha). The close path re-reads that receipt
   through the same gate; live futon3 bytes no longer match the pin; the
   close refuses; the token-outcome comparison receipt is never written;
   every downstream assertion on it fails.
4. Direct reproduction of the gate's dependence on futon3 state: at
   `cab63449` (whose declared sources still pinned `42371c5d…`), a bare
   `(load-declared)` in the worktree refuses today with
   `cascade-sources: interpretation-source-hash-mismatch {:path
   "futon3/library/apparatus/one-authority-per-question.flexiarg", :declared
   42371c5d…, :observed b367ff7d…}`. The same call at the same sha passed on
   2026-09-21/23 when the live futon3 bytes were the pinned ones.

So the tests passed when written because the live futon3 checkout happened
to match every pin they transitively carry, and they fail now because futon3
moved. futon2-side commits only changed WHICH pin is stale (031d9452 re-pinned
the declared sources; nobody re-pinned the test fixtures).

## Verdict: neither candidate shape — a third

The packet asked: test asserting an old close shape (test to update), or the
close dropping/misplacing something a close must carry (defect)? It is
neither. The close behaves as designed: an interpretation receipt whose
document-bound source no longer matches its pin MUST refuse — that is the
pin mechanism working. The defect is a **fixture/environment coupling**: a
test fixture (and, at the close, the decision the test hands the runner)
pins the live bytes of a mutable sibling checkout, so the suite's result is
a function of futon3's HEAD, not of futon2's. This is the AGENTS.md
"fixture looks like the data" family inverted: the fixture IS real data —
pinned so hard to a moving external repo that it cannot outlive the pin.

Historical corroboration that this class recurs: `6e531aae` "Revert fix-10a
merge (7f5ce24b): two runner tests fail on merged main" — the same two-test
signature, re-landed with close-path fixes in `0ff2c6ab`.

What would green look like: either re-pin the three fixtures' receipts to
the current futon3 bytes (`b367ff7d…`, exactly what 031d9452 did for the
declared sources — a treadmill, since futon3 keeps moving), or stop
re-validating fixture receipts against live bytes on the close path (a
design decision: receipts in a recorded decision are already-admitted
evidence, and re-reading their sources against a mutable root re-opens
admission at close). That choice belongs to the runner/close owners; this
packet only names the mechanism.

## f38e20b2 did not touch either failing test

`git show f38e20b2 -- test/futon2/aif/full_loop_runner_test.clj` contains a
single hunk (`@@ -5434,11 +5434,24 @@`); both failing tests live at lines
6060–6160 and are byte-identical across f38e20b2. The "touched the test
file" suspicion is discharged.

## Harness note (relevant to anyone re-running this bisect)

A plain worktree run of this namespace does NOT work: every runner test
errors with `:stale-runner-source` (202 tests, 92 errors), because
`full_loop_runner.clj`'s drift guard compares the loaded source against the
HARDCODED canonical path `/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj`
and refuses on drift. The bisect ran inside a user-namespace bind mount
(`unshare -rm`, bind the worktree over `/home/joe/code/futon2` and
`/home/joe` over `/root` for gitlibs/.m2 resolution) so the canonical path
resolves to worktree bytes; with that, the suite runs clean except the two
tests above. This guard plus the `repo-root` constant means three hardcoded
live-checkout paths already gate this namespace — the futon3 pin drift is
the same coupling, one level down.

Run counts (namespace-level, bind-mounted): `cab63449` single-test 1
failure; `cd1f4f84` full namespace 202 tests, 8 failures, 2 errors;
`cabc8e67` full namespace 202 tests, 8 failures, 2 errors — identical to the
`c6d1e542` report.
