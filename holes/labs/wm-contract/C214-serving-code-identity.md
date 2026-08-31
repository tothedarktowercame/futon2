# C214 — Serving runner code identity

Date: 2026-08-31

The serving JVM cannot reconstruct the Futon2 code it previously loaded from
the checkout currently on disk. Its pre-C214 runner identity is therefore
`UNAVAILABLE`, not inferred from `HEAD`.

Going forward, Futon3c's canonical `dev-admin load-file` path delegates to
`futon3c.wm.code-identity/load-file-recorded!`. The serving process measures
the canonical Futon2 source before and after loading it and records source
SHA-256, Git commit/tree, repository dirty flag, load time, path, PID, and
stability. A non-canonical path or a changing basis is refused. This mechanism
does not reload anything by itself; the first identity appears only after Joe
approves a future targeted reload of the production runner.

`make run-readiness` asks the serving JVM for that recorded fact and compares
its clean, stable Git commit with the commit in the bounded Futon2 test
receipt. Missing identity is `UNAVAILABLE`; mismatch is `UNVERIFIED`; neither
passes. `make run-readiness-serving-code-control` injects distinct loaded and
tested commits and must reject them (`0=pass, 2=control-slipped`).

Scope is the production entry namespace
`src/futon2/aif/full_loop_runner.clj`. This closes the immediate C210 gap but
does not claim a complete dependency bundle; C210's immutable-input design is
still required to identify every dynamically read dependency.
