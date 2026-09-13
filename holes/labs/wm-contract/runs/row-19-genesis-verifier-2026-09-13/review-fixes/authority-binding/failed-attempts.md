# Superseded development attempts

These were pre-commit development runs and are not gates.

1. First focused invocation exited 1: with-redefs received Var forms (ClassCastException; /tmp/clojure-16292260734532487897.edn). Repaired with with-redefs-fn.
2. Next exited 1: runner named the superseded test Var (/tmp/clojure-10001740129885734169.edn). Runner updated.
3. Next exited 1 with 5 passes/8 errors: provenance validation called seq on a keyword. Repaired to require non-nil origin.
4. Next exited 1 with 12 passes/1 error: planted resolver had arity zero. Corrected to accept its reference.
5. Final pre-commit exploratory invocation passed 15/15. Receipt-bearing run then executed once at committed tree 03f34fa0b252a127581b380892024847b0ae6298.
