# Packet 7: live self-heal production seams

This packet repairs the four boundaries exposed by the stopped five-click
campaign. No live click was run.

## Changes

- The serving boundary performs durable META repair readiness before click
  identity allocation and before the ordinary ration callback. Active injury
  plus missing repair authority is a typed refusal, never ordinary fallback.
- Every exact persisted terminal run record is classified and transacted into
  the authoritative locked injury ledger before the runner returns. The legacy
  `HEAD.edn` projection remains observational only.
- Close-wrapper failure retains its complete cause chain and the original
  typed failure. Terminal projection prefers that original kind/stage while
  retaining `invalid-close-outcome/close` as `:wrapped-by` evidence.
- Retry receipts may be `:queued`. The coordinator then persists
  `:enacted/:awaiting-retry` instead of recursively executing and discharging
  under the parent click's single-flight lock. Exact retry run-record ingestion
  alone discharges or reopens; unrelated successful clicks are inert.
- A stale legacy `HEAD.edn` whose temporary source vanished is treated as no
  legacy authority rather than preventing runtime construction.

## Evidence

- Futon2 focused META cohort: 18 tests, 102 assertions, zero failures/errors.
- Futon2 runtime/production/terminal cohort: 27 tests, 131 assertions, zero
  failures/errors before the final queued-retry additions; the final focused
  META cohort above includes those additions.
- Original-failure terminal projection focused test: pass.
- Existing production-shaped `required-loop-node-unexercised` runner control:
  1 test, 10 assertions, zero failures/errors.
- Futon3c runner-service boundary: 30 tests, 205 assertions, zero
  failures/errors, including the no-ration-call negative control.
- `git diff --check`: pass in both repositories.
- `clj-kondo`: zero errors on all changed files; the two reported futon2
  warnings are unchanged pre-existing findings in `full_loop_runner.clj`.

## Acceptance boundary

The changes prove the production composition with injected Agency, enact, and
queued retry adapters. They intentionally do not fabricate deployment
credentials or run a live click. A serving deployment with an active injury
and without those authorities now refuses before spending a ration.
