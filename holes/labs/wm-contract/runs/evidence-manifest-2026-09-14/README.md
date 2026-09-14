# Close evidence manifest v1 execution receipts

The mechanism, specification, and controls were committed first at
`ac6ef7394071acd38c26ba53d9537405b66fad44`. The first gate attempt is
retained in `clj-kondo.out`, `check-parens.out`, and `tests.out`: parens and
4 tests / 19 assertions passed, while clj-kondo exited 2 for the test helper
shadowing `clojure.core/bytes`. The helper-only repair was committed at
`9b916f1878c9d4c6a5968685a0d18d288d6cb7eb`.

Files named `*-final.*` and the EDN receipts are the authoritative gates for
that final tree. Tests use only an injected in-memory byte map; they perform no
filesystem admission and contact no serving JVM.

Final source pins:

- `src/futon2/aif/evidence_manifest.clj`: `8a87b2ae80fac35ff3657c4147494681bae5fad8647d2bdae27c2b7f1928a8db`
- `test/futon2/aif/evidence_manifest_test.clj`: `a883d953899585c4fbcfc9a99e1656652bbe50aee4fb77c74b9bf91319c7a9e6`
- `holes/labs/wm-contract/SPEC-close-evidence-manifest-v1.md`: `d44b6d810b53217f0085202da6d3325b7e1824e85c5969c1ca40184787aad7e6`

