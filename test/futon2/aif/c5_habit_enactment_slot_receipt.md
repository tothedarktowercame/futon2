# C5-habit-3 execution receipt

Candidate base: `2847245b8dc1634bea3ff3e4019c4f0bd29e3892`.
Worktree: `/home/joe/worktrees/futon2/c5-habit-3`.
Environment: isolated `clojure -X:test` process; no shared-JVM load, endpoint,
or click.

## Negative control

I temporarily replaced terminal's typed absence with
`(:precedence (:action decision))`.  The new namespace then failed three
assertions: it expected the typed absence in the sealed terminal and
predecessor, and it rejected equality between selected precedence and
`:enactment`.  The exact red output is retained at
`/home/joe/notes/C5/c5-habit-3-bad-precedence.log`.  The source was restored
before the candidate gate.

## Focused gate

Command:

```
clojure -X:test :nses '[futon2.aif.c5-habit-enactment-slot-test futon2.aif.c5-focus-carry-test futon2.aif.wm.q6-repeat-after-refusal-test futon2.aif.affective-level-test]'
```

The command's complete log is
`/home/joe/notes/C5/c5-habit-3-green.log`.  The new C5 habit namespace and
C5 focus/affective namespaces completed without reported failures.  The
pre-existing Q6 integration namespace finished `25 tests`, `173 assertions`,
`9 failures`, all asserting old `:reading-policy-execution-semantics-owed`
outcomes while the current runner reports `:cascade-enactment-open`; none
mentions `:enactment`.  This is a baseline/integration blocker, not masked by
this receipt.

Static gates after the restored candidate: `clj-kondo --lint` reported 0
errors/0 warnings; `check-parens.el` reported `OK`; `git diff --check` was
clean.  The final candidate-only confirmation is retained at
`/home/joe/notes/C5/c5-habit-3-final-new-namespace.log`: `3 tests`, `12
assertions`, `0 failures`, `0 errors`.
