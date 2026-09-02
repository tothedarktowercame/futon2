# zaif-harness worklist: REVIEW one row per invocation (review seat)

You are the second reader for `futon2/holes/labs/zaif-harness/worklist.edn`.
Do exactly ONE review — the first row listed at the top of this prompt — then
stop. You must not review your own authorship: the row's `:evidence` names
commits; if `git log -1 --format=%an <sha>` or the row text says the work was
yours, stop and say so.

1. `cd /home/joe/code/futon2 && git status --porcelain holes/labs/zaif-harness/`
   must show nothing but untracked receipts; modified tracked files mean
   someone is mid-edit — stop and report.
2. Read the diff of every sha in `:evidence` (`git show <sha>` in the repo it
   names — futon2 and/or futon3c).
3. Re-run the gates the row's `:acceptance` implies in an ISOLATED sibling
   worktree of the repo (e.g. `git worktree add /home/joe/code/futon3c-review-loop
   <sha>` — a sibling path, never /tmp), NEVER against the live JVM
   (:7070/:6768) and never by reloading into it. Run the named tests there;
   capture exit codes directly (do not pipe a gate into tail/grep and read the
   pipe's status). Remove the worktree afterwards
   (`git worktree remove /home/joe/code/futon3c-review-loop`).
4. Check the board's rules held: every new test cites a live-record pin (which
   record id?); tests reading untracked data files guard existence with a
   visible SKIP; "all/none" enumeration claims carry their untruncated search
   command; commits are path-scoped; no `--amend` over foreign HEAD.
5. EITHER set `:status :done :reviewed-by "zaif-build-loop/<your seat>"
   :reviewed-at "<UTC>" :review "<what you checked, with the numbers you
   reproduced>" :covers-key :none` (unless a registry key genuinely applies) —
   OR leave the work standing and set `:status :open` with
   `:review-finding "<what is wrong, with a pointer>"` so the work seat
   repairs it next iteration. Small findings fixable in one edit: fix them
   yourself and say so in `:review`.
6. `bb worklist_check.bb worklist.edn` must pass. Commit the ledger (and any
   fix) path-scoped with the row id in the message — never `--amend`. Report
   in three lines. Stop.
