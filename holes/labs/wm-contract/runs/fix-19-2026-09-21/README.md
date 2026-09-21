# Fix-19: read-only serving-code inventory

No reload or WM click was performed. All serving evals used core/JDK reads:
namespace lookup/intern-name enumeration, process properties and dereferencing
the existing tripwire baseline. No serving `require`, `def`, `intern`,
`load-file`, application invocation, or atom mutation.

## Result at 2026-09-21T15:09:48.890957332Z

Canonical head: `66b379c29dbea036da73f4d5a458f210a37a7f35`.
Selected base: `4354668b6f8327d26d449c013bd273297ab8e7db`, the last first-parent
commit before 13:00 UTC, selected for the owner's requested afternoon changes.
**This is a scope boundary, not a claimed last-loaded commit.** None could be
established read-only. The tool requires `--since` rather than inventing one.

17 namespaces changed in this range; every disk sha256 is in `probe.edn`.
Ten namespaces are loaded; seven are not loaded:

- futon2.aif.cascade-habit-reinforcement
- futon2.aif.cascade-plan
- futon2.aif.job-text-retention
- futon2.aif.scan-report
- futon2.aif.token-outcome
- futon2.aif.interpretation-construction
- futon2.aif.run-narrative

The loaded `futon2.report.war-machine` has no intern named
`candidate-want-progress`, whereas disk defines it (fix-16). That is concrete
structural evidence the serving namespace is behind the current definition.
Missing namespaces are not by themselves proof a caller is stale (some code
loads lazily). For all 17, exact loaded-byte freshness is **unknown**, not
falsely current. The report retains intern sets so the owner can inspect
specific changes without executing application code.

## Why the existing mechanisms cannot certify loaded bytes

`full_loop_runner.clj`'s `runner-source-drift` calls `resource-bytes` on every
check. That function opens the classloader resource *then*, not when the
namespace was compiled. Its comparison with canonical disk therefore compares
two contemporary file reads when the resource resolves to the canonical file.
It can say `:current` while old functions are loaded. Its docstring overstates
what it establishes. `:runner/source` / `:loaded-code-identity` carry this
same report, not a whole-JVM loaded git commit. This issue is reported here;
production guard changes are outside this prep assignment.

Tripwire's `defonce composition-baseline` covers only five namespaces. The
actual serving baseline contains four entries, all with nil source hashes
(the runner was not present at capture). It reads relative disk paths and
records class names, not bytecode hashes. It cannot establish current source
identity after partial reloads. Repair evaluators have a narrow captured digest
map, but none of the 17 affected namespaces has that mechanism.

There is no general retained load-time source digest in these namespaces.
Metadata `:file`/`:line`, class names, resource bytes, git HEAD and namespace
presence cannot retrospectively supply one. No new registry was installed in
the serving JVM to disguise that limitation.

## Tool and reproduction

From the fix-19 worktree (the tool itself runs locally):

```sh
clojure -M -m wm-loaded-code-probe --repo /home/joe/code/futon2 --since 4354668b6f8327d26d449c013bd273297ab8e7db
```

It reads tracked Clojure namespaces under `src/` and `scripts/futon2/`, unions
paths touched in the commit range with tracked uncommitted changes, and
orders selected namespaces through their transitive static `:require`/`:use`
dependencies (including unchanged intermediates). Cycles refuse. Deleted or
unreadable-as-namespace paths are listed as `:unrepresented-paths`; this run
has none. It does not pretend to infer dynamic dependencies or reload
unchanged dependents with captured roots/macros. The afternoon range is not
a guarantee that earlier code was loaded. HEAD and selected disk digests are
checked again after the live read; concurrent source changes require retry.

## Owner's reload and verification

`owner-reload.clj` contains the exact ordered `(require 'ns :reload)` forms.
**Prepared only; not executed.** First it checks every selected classpath
resource resolves to `/home/joe/code/futon2/{src,scripts}/...` and matches the
recorded digest. If more fixes land, it refuses: rerun the probe and regenerate
the plan for the new snapshot. No worktree source is loaded.

The owner can run from `/home/joe/code/futon3c`:

```sh
scripts/proof-eval.sh -f /home/joe/code/futon2-fix-19/holes/labs/wm-contract/runs/fix-19-2026-09-21/owner-reload.clj
scripts/proof-eval.sh -f /home/joe/code/futon2-fix-19/holes/labs/wm-contract/runs/fix-19-2026-09-21/post-reload.clj
```

`-f` reads an instruction form on the client; these forms never `load-file`
a worktree. `require` resolves only canonical resources, as checked above.
The second form is read-only; expect `:all-listed-namespaces-loaded? true`
and `:fix-16-helper-present? true`. Retain the successful reload response as
execution evidence. This is structural verification plus a witnessed reload,
not retrospective proof of every loaded function or refreshed `defonce` state.

## Gates

5 tests / 8 assertions, zero failures/errors. Tests cover the false-current
case (presence plus a disk hash must still return unknown), transitive order
through unchanged dependencies, cycle refusal, both prefix require shapes,
explicit base refusal and generated read-only form syntax/forbidden operations.
This is a new prep tool, not a production fix; there is no predecessor tool
on main against which to claim a failing regression assertion.

```sh
clojure -M:test -m cognitect.test-runner -n wm-loaded-code-probe-test
clj-kondo --lint scripts/wm_loaded_code_probe.clj test/wm_loaded_code_probe_test.clj holes/labs/wm-contract/runs/fix-19-2026-09-21/owner-reload.clj holes/labs/wm-contract/runs/fix-19-2026-09-21/post-reload.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- scripts/wm_loaded_code_probe.clj test/wm_loaded_code_probe_test.clj holes/labs/wm-contract/runs/fix-19-2026-09-21/owner-reload.clj holes/labs/wm-contract/runs/fix-19-2026-09-21/post-reload.clj
```

Lint: zero errors/warnings; parens: OK. Generated reload forms were only
parsed/linted, never evaluated. `tests.log` and registry evidence are retained.

Registry warrant: `test-registry-d9c6f2fb6e40582b24099c685c9daeb58684cf7aeff7cdfe44b2c5ebab63a1b0`. Committed implementation
`a8229020`; stable execution, matched postcheck, 5 tests / 8 assertions, zero failures/errors. This warrant runs only local unit tests,
not the owner reload artifact or any serving mutation.
