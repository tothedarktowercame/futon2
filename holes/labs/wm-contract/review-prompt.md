# wm-edge worklist: REVIEW one row per invocation

You are the second reader for `futon2/holes/labs/wm-contract/worklist.edn`. Do exactly ONE
review, then stop. You must not be the author of what you review: the row's `:evidence` names
the commit; if `git log -1 --format=%an <sha>` or the row text says the work was yours, pick
another row or stop and say so.

1. `cd ~/code/futon2 && git status --porcelain holes/labs/wm-contract/` must show nothing but
   untracked receipts; if ledger or registry files are modified, stop and report (someone is
   mid-edit; a read is against a commit).
2. Pick the FIRST row with `:status :done-unreviewed`.
3. Read the diff of every sha in its `:evidence` (`git show <sha>`). Re-run the verify step the
   row's `:acceptance` implies: tests it names, `bb worklist_check.bb`,
   `bb ~/code/p4ng/empirics-futon/pointer_check.bb`, `bash ~/code/p4ng/empirics-futon/negative_controls.sh`
   if a registry or generator changed, the run's pre-flight or script if it is a RUN row. Capture
   exit codes -- never pipe a gate into tail/grep and read the pipe's status.
4. EITHER set `:status :done :reviewed-by "<your seat name>" :reviewed-at "<UTC>" :review "<what you
   checked, with the numbers you reproduced>"` -- if the row touched a registry entry add
   `:covers-key [<key-path>]` (content-addressed, e.g. `[:equations {:id :precision}]`) and
   `:review-covers "<registry sha>"`; if it touched none, `:covers-key :none` --
   OR leave it `:done-unreviewed` and add `:review-finding "<what is wrong, with a pointer>"` and
   set the row `:status :open` so the work loop repairs it.
5. Small findings you can fix in one edit: fix them, say so in `:review`.
6. `bb worklist_check.bb` must exit 0. Commit the ledger (and any fix) with the row id in the
   message. Report in three lines. Stop.
