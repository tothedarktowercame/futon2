# C220 — Prepared clean production-runner reload

Date: 2026-08-31
Status: prepared, not performed

Run `make runner-reload-preflight` in canonical `/home/joe/code/futon2`.
It checks every condition without contacting the Drawbridge admin route:

1. the source resolves exactly to canonical
   `/home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj`;
2. the canonical branch is `main` (this repository's current name for the
   historical “master” branch);
3. `HEAD` is readable and the whole repository is clean;
4. a throwaway tooling JVM can require `futon2.aif.full-loop-runner`;
5. a clean, stable, green bounded Futon2 receipt tested that exact commit.

Only when all five pass does it print this executable command:

```sh
cd /home/joe/code/futon3c && clojure -M:dev-admin load-file /home/joe/code/futon2/src/futon2/aif/full_loop_runner.clj
```

The command uses the canonical admin client. In the serving JVM it delegates
canonical Futon2 loads to `load-file-recorded!`; it does not start a second
serving JVM and cannot load a worktree copy. Joe, not this procedure, decides
whether to execute it.

After a successful reload, `make run-readiness` should change
`serving-runner-code` from `UNAVAILABLE/not-recorded-in-this-process-image` to
an `available` identity. It passes only when that identity is clean and stable
and its Git commit equals the bounded Futon2 receipt's tested commit. Remaining
readiness blockers remain independently visible; a recorded identity does not
force the overall verdict green.

The throwaway `futon3c.wm.code-identity-test` exercises the recording path
without touching production. It demonstrates: clean canonical source records
commit/content/clean/stable identity; dirty canonical source is refused and
records nothing; noncanonical source is refused and records nothing.
