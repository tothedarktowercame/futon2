# C5-focus execution receipt

Candidate base: `c4a8b8deb6512037c35bc6ce32a1ae1acfc8f077`  
Worktree: `/home/joe/worktrees/futon2/c5-focus`  
Environment: Linux `6.8.0-31-generic` x86_64; Clojure CLI `1.12.5.1664`.

## Path exercised

`previous-run/start!` reads the sealed predecessor terminal; its `:focus` is
decoded by `focus-receipt/predecessor-focus`; `decision-focus-info` supplies
that value to `discover` for a time with no covering window.  The normal
runner now persists the selection receipt in Q6 terminal `:focus`, and the
ordinary selector obtains its predecessor from `previous-run/*predecessor*`.

## Required bad case, then final candidate

Temporary bad edit: in the no-window/unknown-established branch, replace
`(discover inputs decision-as-of previous)` with
`(discover inputs decision-as-of nil)`.

Command:

```sh
clojure -X:test :nses '[futon2.aif.c5-focus-carry-test]'
```

Bad result (captured at `/home/joe/notes/C5-focus/badcase-red.log`): 2 tests,
11 assertions, **4 failures**.  The retained-focus case became `:unknown`,
lost focus `"F"`, lost predecessor id `"one"`, and changed origin from
`:retained-unfinished-focus` to `:commit-facets`.

The edit was reverted.  Final result (captured at
`/home/joe/notes/C5-focus/green-final.log`):

```text
Ran 2 tests containing 11 assertions.
0 failures, 0 errors.
```

The same test covers a resealed terminal with removed `:focus`, a broken
terminal checksum, and a cold start; each remains a typed absence.

## Static gates

```text
clj-kondo --lint <six changed Clojure files>
errors: 0, warnings: 2
```

The two warnings are pre-existing in `full_loop_runner.clj` (redundant `let`,
unused `on-prod-history-checkpoint`); no warning was introduced.  The paren
check reported `OK`; `git diff --check` was clean.

## Existing namespace observations

`futon2.aif.meta-live-outer-selector-test` after the expected focus-receipt
shape update: 33 tests / 152 assertions, one failure in
`explicit-nil-constructor-injections-use-production-defaults`, whose expected
reasons omit observed `:no-canonically-bound-policy`.  This does not exercise
the focus carry.  `futon2.aif.focus-receipt-test` was already red at the real
serving fixture (`receipt-is-attached-by-the-real-serving-selection` gets no
focus receipt); it produced 6 failures before this lane's assertions could
serve as an acceptance gate.  Neither was changed to hide these failures.
