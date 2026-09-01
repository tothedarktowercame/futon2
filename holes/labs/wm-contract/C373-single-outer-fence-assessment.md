# C373 — one outer workspace-gate fence assessment

The workspace gate now clears `FUTON_WRITER_FENCE_ID` and
`FUTON_WRITER_FENCE_EVIDENCE` explicitly for both contract-authority
constituents. They report `PASS-CONTENT-ONLY (event-free unverified)`. The
outer gate alone consumes the receipt and qualifies the composite verdict.

This is not caching: no verification result crosses an operator step or is
passed to another consumer. A later step performs a new observation.

## Control

```sh
clojure -M:test -m cognitect.test-runner \
  -n workspace-gate-fence-composition-test
```

Result: 2 tests, 6 assertions, zero failures/errors. The control proves both
authority argv vectors clear the transport variables; absent outer evidence
produces `:event-free? :unverified`; the outer composition has the sole
fence-conditional branch.

An actual inner invocation under a shell containing fabricated fence variables,
but through the gate's `env -u` boundary, exited 0 and printed
`PASS-CONTENT-ONLY (event-free unverified)`.

```sh
clj-kondo --lint checks/wm_workspace_gate.clj \
  test/workspace_gate_fence_composition_test.clj
```

Exit 0, no warnings/errors.

## Window accounting

C290's gate had 78 checks and took 216 seconds: 2.77 seconds/check. C366's
moving-basis measurement had 124 checks and took 312 seconds: 2.52
seconds/check. Wall time grew 44%, but check population grew 59%; average cost
per check fell about 9%. The observed growth is check count, not generalized
slowness, and the fence is only about 2.2 seconds of it after C373.

Against C290's 6 min 24 s preparation estimate, reserve approximately:

- **6 min 28 s** for preflight plus the outer-gate assessment;
- **6 min 31 s** if a separate standalone authority assessment is also wanted.

These are fence-adjusted comparisons to C290, not a prediction that the now
124-check gate will still finish in its historical 216 seconds. Using the
current measured gate population, the observed preparation total is about
**8 min 04 s** (`384 - 216 + 312 + 2.18 + 2.22`), with the qualification that
the 312-second measurement crossed a moving repository basis.
