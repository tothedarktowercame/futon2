# M2y-4e — retain cascade-effect contact absence

Branch `m2y4e-contact-absence`, worktree
`/home/joe/worktrees/futon2/m2y4e`.

Base `e46cdfbeedaf1c12440fb2465f07e807a941b439`; code commit
`c679e8ea91e5501c440eec255a266d8d7921e18f`.

Own short-lived JVM only. No shared-JVM reload, continuation, click, or live
store write.

## Contract

The two `production-cascade-executors` contacts retain the exact existing
`handoff/response-map` result whenever it is non-nil. Only nil is replaced:

- a non-terminal/non-done Agency state becomes a typed absence whose reason is
  that normalized state (including `:not-contacted`);
- a done response with no parseable JSON becomes `:unparseable-answer`;
- seat is always present and Agency job-id is retained when supplied.

`handoff/response-map` itself is unchanged because its other callers retain
their existing semantics.

The absence cannot become a sealed effect receipt. In
`reviewed-build-review-close-executor`, `effect` is `(:effect reviewed)`, hence
nil for the absence map. The acceptance conjunction requires effect status
verified, a grounding map, valid revision and a 64-hex digest before calling
`dispatch/seal-receipt`; those checks fail and the existing
`:reviewed-effect-invalid` receipt retains both answers.

## Live evidence pins

- `/home/joe/notes/M2y/m2y4c/result.edn`:
  `49ddc875f286a6a1a72ef7269b1ee87bf7f50671d4a956afa07043c764a7ee31`.
  Its continuation receipt has occurrence `["agent/trail-enables-return" 1]`,
  `:reviewed-effect-invalid`, and nil author/review answers.
- `/home/joe/code/futon2/data/wm-cascade-continuation-failures/fa8d745780243ce468ac87250024d4c23bab01cbb32f78ef52dfc1435647cd99.edn`:
  `3d5f7940465ac4dce222ce4b4b4bf794c0bbd59bc5a0d1926483d0fae70b41d4`.

These records establish the erased result, not its cause. The test plants the
known designated-seat non-contact path and the independent unparseable-done
path.

## Red

On the base source with the new test copied in:

```text
clojure -X:test :nses '[futon2.aif.m2y4e-contact-absence-test]'
Ran 1 tests containing 8 assertions.
4 failures, 0 errors.
```

Both unavailable-seat answers and both unparseable-done answers were nil. The
good answer assertion passed, pinning the unchanged branch. Full `red.log`
SHA256: `97ce5f2d82a4e8a9dd0853f85ea7d840858a2a42bca57253a676aaa9d1971231`.

## Green and gates

One combined test run:

```text
clojure -X:test :nses '[futon2.aif.m2y4e-contact-absence-test futon2.aif.m2y4d-effect-target-test futon2.aif.wm.cascade-continuation-test]'
Ran 9 tests containing 36 assertions.
0 failures, 0 errors.
```

`green.log` SHA256:
`14ccf02b4c76cdebde524f83a4e5b7e04ac3a24e20b441bab4d4b5a9689399f8`.

`clj-kondo` reports 0 errors and 0 warnings (`kondo.log` SHA256
`70897173e3e79986fea60d09198d97f0d0423fefb0fac20205439521dbf18c34`).
Both changed Clojure files pass check-parens (`parens.log` SHA256
`3df37de95de2d6178e7c17a0bcb38f78927088befb99aeaa7e6aa7df2f9b8a33`).

Final source SHA256s:

- `src/futon2/aif/full_loop_runtime.clj`:
  `c37f64c9bba58ebf599d2708095dc88a73702550038617a8f630a6336e52a30f`
- `test/futon2/aif/m2y4e_contact_absence_test.clj`:
  `ce71bd8ec821863f1b36b3a899c60cd0e003aac5fe8815b1de2946ffe2a024df`

Environment: OpenJDK 21.0.12.1, Clojure CLI 1.12.5.1664, Linux x86_64
6.8.0-31-generic.
