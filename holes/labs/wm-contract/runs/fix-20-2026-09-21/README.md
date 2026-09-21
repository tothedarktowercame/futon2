# Fix-20: namespace load-time source identity

Branch `fix/narrative-20`, base `e9659878`. No serving JVM evaluation, reload,
click or canonical checkout edit was performed.

## Behavior

`futon2.aif.load-identity/register!` samples source bytes as a namespace loads,
records their SHA-256, resolved URL/path and capture time in a `defonce`
registry. The same namespace's registration is overwritten on reload; other
registrations survive. Each participant calls it immediately after its `ns`
form. Absolute `*file*` paths support actual `load-file`; classpath-relative
names resolve through the current loader. Failed reads remain unavailable.

The declared scope is the 17 afternoon-probe namespaces (including runner and
war-machine), plus EFE, close-loop and close-retention: **20 namespaces**.
`required-sources` lists their canonical paths explicitly. This is a scoped
report, not a claim to cover every transitive futon2 dependency.

The runner now compares registered digests with fresh canonical disk reads.
Every registered namespace and every required namespace appears under
`:namespaces` in `:runner/source` and the forwarded `:loaded-code-identity`.
Per-namespace status is `:current`, `:stale`, `:unregistered` or `:unavailable`.
An extra registration with no declared canonical path is unavailable; its
worktree/resource path is never substituted for canonical disk.

Compatibility: runner-owned stale maps to existing `:runner/source-check
:drift` and the existing `:stale-runner-source` refusal. Other namespaces'
staleness is **recorded only**, and unregistered/unavailable remains non-refusing.
The runner's existing `:runner/sha256` remains the captured digest when current.
Existing record persistence already carries the whole report; its test now
also checks a per-namespace unregistered verdict survives serialization.

## Capture boundary

This is source identity sampled during load, not a bytecode hash. The compiler
can have read bytes before the registration call; an edit between those reads
and registration is a residual race. Failed/partial compilation after the
registration call, `load-string` with unrelated source metadata, later Var
mutation and preserved `defonce` application state are not certified by a
matching source hash. The namespace and runner docstrings say so. No new
loaded-byte certainty is claimed for namespaces not registered in this scope.

After landing, the owner must load the new identity dependency and reload each
participating namespace from canonical paths to populate its entry. Merely
loading the registry cannot certify already-loaded namespaces; the report
honestly shows them unregistered. This branch performs none of those serving
operations.

## Negative case and acceptance

Before runner edits, `runner-load-identity-test` ran against base runner code:
1 test / 3 assertions / 3 failures / 0 errors (`before.log`). Exact assertion:

```
expected: (= :drift (:runner/source-check r))
  actual: (not (= :drift :current))
```

The registry contains an older digest while the injected canonical read equals
the actual current classloader resource. The old runner compares those two
current reads and misses the stale capture; the new runner consults the capture.

`load-identity-test` also performs the real sequence in its own process:
load a temporary namespace that registers itself; edit its file; verify the
loaded value is still old and status is stale; show the old disk-to-disk
comparison still returns equality; reload the file; verify the new value and
current status. Unregistered namespaces and unknown canonical paths have
separate controls. No model/application stubs replace the actual file loader.

`runner-load-identity-test` verifies the runner's aggregate report, explicitly
checks another namespace's staleness does not refuse, and loads all 20
participants, comparing each captured digest with its actual source resource.

## Gates and remaining owner run

Fresh standalone commands from this worktree:

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.runner-load-identity-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.full-loop-runner-test
```

- load-identity: **3 tests / 12 assertions**, zero failures/errors.
- runner-load-identity: **3 / 45**, zero failures/errors.
- Existing source-guard tests selected with `clojure.test/test-vars`: **3 / 12**,
  zero failures/errors. Includes actual early refusal and durable record checks.
- Full runner namespace: **184 tests / 576 assertions / 0 failures / 86 errors**.
  Every error is `Serving runner source drifts from the canonical checkout`.
  This isolated branch's source correctly differs from canonical main. The
  guard was not disabled and the canonical checkout was not changed to turn
  the run green. The owner requested to run the runner-level tests on main;
  **that full canonical run remains required after landing**. These errors
  are not reported as a passing suite or hidden behind a warrant.

The three selected existing Vars were:
`runner-source-drift-compares-actual-bytes`,
`drifting-runner-refuses-before-consuming-the-attempt`, and
`run-record-carries-the-runner-source-identity`, in
`futon2.aif.full-loop-runner-test`. `existing-source-tests.log` is their fresh
result. Their old current-resource mocks were migrated to captured-digest
registry entries, preserving equal-byte, unavailable and refusal assertions.

Static gates over every changed/added Clojure file: clj-kondo **0 errors,
0 warnings** (two existing informational str messages); check-parens **OK**.
`git diff --check` clean. Commands:

```sh
clj-kondo --lint $(git diff --name-only e9659878 -- '*.clj')
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- $(git diff --name-only e9659878 -- '*.clj')
```

The committed configs request warrants only for the two new complete green
namespaces, never for the failing full runner run. Full logs are retained.
