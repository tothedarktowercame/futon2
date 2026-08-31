# C207 — Quiescent-tree readiness boundary

Date: 2026-08-31

`READY` is intentionally not reachable during active writes to the shared
checkout. The operational window is:

1. pause lane writes and reach a clean committed tree;
2. run the bounded futon2 suite (C162 measured about 105 seconds);
3. run `make run-readiness`;
4. execute and finish the printed operator command before writes resume.

Quiescence must cover the suite and operator run. The receipt producer observes
the repository before and after the suite, so a mid-suite commit produces an
unstable basis. A post-suite commit produces a current-tree mismatch. Readiness
itself acquires no lock, and runtime code may open repository resources after
startup, so a post-readiness commit is a time-of-check/time-of-use hazard rather
than something only the next readiness invocation needs to notice.

The dirty rule cannot honestly be narrowed to “tested paths” today. No complete
dependency manifest covers Clojure sources, generated files, classpath
resources, sibling inputs, and dynamic file reads. Repository-wide cleanliness
is the executable conservative boundary. A future isolated worktree at the
tested commit could permit development elsewhere without weakening identity;
that is a separate design and is not implemented here.
