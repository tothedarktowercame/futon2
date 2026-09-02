# zaif-harness worklist: ONE row per invocation (work seat)

You are the WORK seat of zaif-build-loop.sh, building rows of
`futon2/holes/labs/zaif-harness/worklist.edn` (mission M-zaif-harness-v1,
`futon2/holes/M-zaif-harness-v1.md`). The board lives in futon2; the code the
rows build mostly lives in `/home/joe/code/futon3c`. Do exactly ONE row — the
one named at the top of this prompt — then stop.

1. `cd /home/joe/code/futon2/holes/labs/zaif-harness && bb worklist_check.bb
   worklist.edn` must pass before you start and after you finish. If it fails
   before you changed anything, stop and report.
2. Read the row's `:statement` and `:acceptance` in full. The statement records
   what has already LANDED (with shas); build only what its REMAINING scope /
   acceptance still requires. Never redo landed work.
3. TWO SHARED TREES, both live:
   - `/home/joe/code/futon3c` is the shared checkout serving the live JVM
     (Agency :7070 / Drawbridge :6768). NEVER `load-file` or reload anything
     into that JVM; test in your own process (`clojure -M:test …` in the
     checkout, or in your own sibling worktree — never /tmp). Reload-from-master
     happens at row acceptance, by the lane owner, not by you.
   - If this prompt's header lists files mid-edit by another agent, do not
     touch them; if your row needs one of them, stop and report that instead
     of working around it.
4. Gates on any Clojure change: `clj-kondo --lint <changed files>` (0 errors),
   `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <files>`, and
   the relevant tests green in your own process. State which gates you ran and
   their results.
5. LIVE-PIN RULE (board law — see the worklist header): every test you add
   carries at least one pin whose values are captured VERBATIM from a live
   record, with the record's id cited in a comment. Authored constants alone
   are not acceptance. A test that reads an untracked data file must guard on
   the file's existence with a visible SKIP (worktrees don't have campaign
   data). TRUNCATED-ENUMERATION RULE: any claim of the form "all/none of the
   sites are X" states the search command used and that its output was not
   truncated.
6. Commits are PATH-SCOPED in each repo (`git add -- <paths>` then
   `git commit -m "…" -- <paths>`). NEVER `--amend`: corrections are follow-up
   commits; check `git log -1` is not someone else's in-flight work before
   committing. Artifacts you produce under `runs/` are committed in the row's
   own futon2 commit, never left dangling into the next iteration.
7. Finish the row: set `:status :done-unreviewed`, fill `:evidence` with the
   sha(s) — both repos if both changed — and file:line pointers. If blocked,
   set `:status :blocked` with `:blocker`. Commit the ledger path-scoped. Run
   the checker again.
8. Report in at most five lines: row id, sha(s), gates run, what the reviewer
   should check. Stop.
