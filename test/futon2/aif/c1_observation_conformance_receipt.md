# C1 observation conformance receipt

Candidate base HEAD: `f74a84037be49bac3bd0f61f6b4d67c941a8fc20`.
Worktree: `/home/joe/worktrees/futon2/c1-observation`.
Environment: Linux 6.8.0-31-generic x86_64; Clojure CLI 1.12.5.1664.

## Law exercised

`cascade-model-manifest/pattern-kernel` applies the action's `:produces` set
to the current token state, yielding its successor distribution.
`predict-observations` applies `observation-distribution` to every successor
and marginalises it.  With rates `{:true {:false-neg 1/10 :false-pos 1/10}}`,
the empty state yields `Q(#{:true}) = 1/10`; the state `#{:true}` yields
`Q(#{:true}) = 9/10`.  `outcome-risk` is then invoked on each resulting
action-conditioned observation distribution with full-support preference.

## Required bad case, not committed

Temporary edit: the `:flip` fixture emitted `#{}` rather than `#{:true}`.

Command:

```sh
clojure -X:test :nses '[futon2.aif.c1-observation-conformance-test]'
```

Result: red — 1 test, 7 assertions, 3 failures.  The failing checks were the
flip successor `{#{:true} 1}`, its `9/10` observation probability, and the
required difference from keep.  The temporary edit was reverted.

## Final gate

```sh
clj-kondo --lint test/futon2/aif/c1_observation_conformance_test.clj
emacs --batch -l /home/joe/code/futon4/dev/check-parens.el \
  test/futon2/aif/c1_observation_conformance_test.clj
clojure -X:test :nses '[futon2.aif.c1-observation-conformance-test]'
```

Output:

```text
linting took 7ms, errors: 0, warnings: 0
OK

Running tests in #{"test"}

Testing futon2.aif.c1-observation-conformance-test

Ran 1 tests containing 7 assertions.
0 failures, 0 errors.
```
