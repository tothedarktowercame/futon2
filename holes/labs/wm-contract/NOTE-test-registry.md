# NOTE: the test registry — inbox-zero's next analogue (Joe, 2026-09-14)

Joe's observation: under author-is-not-reviewer, the author (say Codex)
writes and runs tests, then the reviewer (say Claude) "reruns the
tests" — strictly a waste. The rerun costs full execution AND buys
none of the judgment that justifies having a separate reviewer.

## The mapping (it is inbox-zero, exactly)

| inbox-zero | test registry |
|---|---|
| dirty tree | test run with no registered warrant |
| commit + push | registry entry (sha-chained, tamper-evident) |
| certificate (board/inputs/verbs digests + replay) | run record: code sha, test sha, command, env fingerprint, results, log, timestamp, author |
| verify-trace replays instead of re-executing | reviewer VERIFIES THE RECORD instead of duplicating the run |
| compensating window | selective re-execution when warrants fail |

## The registry entry (the warrant a test run must carry)

{:code-sha <the code under test> :test-sha <the tests themselves>
 :command "clojure -M:test -n …" :env-fingerprint <toolchain/jvm/deps
 hashes> :results {:tests N :assertions M :failures 0 :duration-ms …}
 :log <pinned artifact> :ran-at … :author "codex-N"}

Integrity: entries are append-only and sha-chained (the RUN4 packet
discipline); a results claim without matching shas does not register.

## What the reviewer does instead of rerunning (the real review)

1. RECORD CHECK (cheap, mechanical): do the shas match the diff under
   review? Does the env fingerprint match the review environment?
   Fresh log, complete result envelope, typed-none for absent parts.
2. TEST ADEQUACY (the judgment only a reviewer provides): are these
   the right tests? Do they pin the behavior the change claims? Would
   they catch the likely bug? THIS is what author-is-not-reviewer is
   for — and rerunning never supplied it.
3. SELECTIVE RE-EXECUTION (priced): sample spot-checks; full rerun
   only on warrant failure — stale sha, env mismatch, first-ever run
   of a new test, security- or invariant-relevant paths, or a results
   claim the record cannot support. Audit sampling, not audit
   duplication.

## Why this is safe (the existing precedents)

- The chip-board certificates already established the principle: a
  replayable record IS the verification for deterministic executions;
  codex-17's witness loop then proved replay beats rerun for catching
  tampering (witness defect 2 — the certificate compares payloads).
- WORK-REMAINING row 3 (click path records both output-validator
  verdicts inside the construction record) is this same shape landing
  in the WM; the registry generalizes it to all test evidence.
- R9 is PRESERVED, not weakened: proposal ≠ witness still holds — the
  witness's act becomes record-verification + adequacy judgment +
  selective execution, which is MORE witness-work than a blind rerun,
  not less. What disappears is the duplication, not the independence.

## AIF reading (one line each)

- The run is R2 evidence; the registry is its warrant (◈); the
  reviewer's precision about it (R7) is high when shas match and env
  agrees, decaying with staleness — re-execution is the precision-
  restoring act, performed when precision demands it, not always.
- Ostrom: rerun-everything is monitoring priced above defection gains;
  the registry prices policing at a sha comparison, with sampling for
  depth.

## Open questions for Joe

1. Registry scope: futon3c tests first (the hot path), or all repos?
2. Does the env fingerprint include the JVM/deps (it must, for
   "0 failures" to mean anything across machines)?
3. Sampling rate for routine reviews (default proposal: spot-check one
   test per review; full rerun stays mandatory for pre-push and
   invariant-relevant lanes)?

## RULINGS (Joe, 2026-09-14)

1. Scope: wherever tests and handoffs are run — not repo-limited.
2. Env fingerprint: as proposed — toolchain, JVM, dependency hashes
   included, so results are comparable across machines.
3. Sampling: spot-check one test per review by default; adjust when
   evidence says otherwise (the threshold rule again: declared, then
   re-priced on measurement — full rerun stays mandatory pre-push and
   on invariant-relevant lanes).
