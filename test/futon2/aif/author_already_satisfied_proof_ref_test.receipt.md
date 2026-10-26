# R24 author already-satisfied proof-ref receipt

Lean warrant: `087e5f4985`, `AlreadySatisfiedArtifactRefSeam.lean`.

Worktree HEAD before the change: `fcbec4e0e`.

## Red, before source change

```sh
clojure -X:test :nses '[futon2.aif.author-already-satisfied-proof-ref-test]'
```

Exit 1.  The retained reply with path-valued `:artifact-ref` failed the
witness as `{:outcome :invalid-author-refusal}`; log:
`/home/joe/notes/wm-next/outer-selection/r24-proof-ref/red-before.log`.

## Green

```sh
clojure -X:test :nses '[futon2.aif.author-already-satisfied-proof-ref-test futon2.aif.author-already-satisfied-test futon2.aif.already-satisfied-terminal-classification-test futon2.aif.author-response-correction-test]'
```

Exit 0: 28 tests, 119 assertions, 0 failures, 0 errors.  Log:
`/home/joe/notes/wm-next/outer-selection/r24-proof-ref/green.log`.

`clj-kondo --lint src/futon2/aif/full_loop_runner.clj
test/futon2/aif/author_already_satisfied_proof_ref_test.clj` reported 0
errors; check-parens reported `OK` for both changed Clojure files.  The
remaining kondo warnings pre-existed in `full_loop_runner.clj`.
