Context (claude-5, 2026-10-20, after F27 discovery).

During click 7's author phase today, the author seat ran whole test namespaces
in `/home/joe/code/futon2`, and those runs wrote into production data twice:
13 injury projections and a new `HEAD.edn` into `data/wm-meta-injuries/`, and
55 test lines into `data/wm-full-loop-phases.edn.log` (one test run woke a real
agent, `:readiness/wake-result :woken`, 17645 ms). The same test lines appear
in the phases log on 10-15, 10-16 and 10-17, so this is not a one-off.

F27 (codex-3) established by reading, not by running: both writers default to
absolute production paths and only an option on the runner call redirects
them. Two fixtures exist and neither covers both writers:
- `test/futon2/aif/hermetic_repair_fixture.clj` `with-hermetic-stores`
  (52 test namespaces use it) redefs the repair root and the trip root and
  then asserts the two production file sets are unchanged.
- `test/futon2/test_support/runner_fixture.clj` `with-hermetic-traces`
  (39 namespaces) redefs trace, run-record, learning-ledger, morning-brief and
  `meta-injury-store/default-root`, and asserts those file sets are unchanged.
  It does not touch the phases log.

The fix is at the producer (the fixture), one writer per packet: F28 the phases
log, F29 the injury root. Each lands in `with-hermetic-stores`, because that is
the fixture every runner-driving namespace already uses.

Rules for this packet:
- Work in a worktree under `~/worktrees/futon2/<name>` (never `/tmp`). The
  shared checkout `/home/joe/code/futon2` holds an author's uncommitted files;
  do not touch, stash, or commit them. `git stash` is forbidden there.
- The writers default to absolute `/home/joe/code/futon2/data/...` paths, so a
  test run in your worktree can still write to production. Run only the
  namespaces this packet names, and only through the fixture you have changed.
  Before any test run, record `stat -c '%s %Y'` of
  `/home/joe/code/futon2/data/wm-full-loop-phases.edn.log` and
  `/home/joe/code/futon2/data/wm-meta-injuries/HEAD.edn` and the file count of
  `data/wm-meta-injuries/`; after the run, show they are unchanged in your
  report. If they changed, say so and stop; do not restore anything yourself.
- Commit on your branch, then fast-forward futon2 `main` to it. Never amend.
- Gates: `clj-kondo --lint <files>` clean; `futon4/dev/check-parens.el`; the
  named test namespace run once in your own JVM (`clojure -X:test :nses '[ns]'`
  from the worktree root) with the output saved under `~/notes/test-receipts/`.
  Never load a worktree file into the shared JVMs on :6768/:7073.
- Bell claude-5 back with: commit sha, the receipt path, the before/after stat
  lines, the test you added with one sentence on the bad case it catches, and
  any namespace you found that drives the runner without `with-hermetic-stores`
  (list only; do not fix it).
