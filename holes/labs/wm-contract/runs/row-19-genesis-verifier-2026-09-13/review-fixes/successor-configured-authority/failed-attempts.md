# Superseded attempts

Pre-commit development runs found and repaired a misplaced successor-test parenthesis, three zero-arity planted resolvers, and then passed 22/22. At committed tree `04343e0e`, tests passed 22/22, induced failure exited 1, and parens passed, but kondo exited 2 with one unused `config` binding. Follow-up `5a1df387` removed it; all final gates below were rerun. The first production-root probe lacked the repository's dev classpath and exited 1 (`/tmp/clojure-10785659690913689524.edn`); the corrected read-only probe is retained separately.
