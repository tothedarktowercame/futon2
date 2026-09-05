# library-loop: REVIEW one row

You are the Codex review seat for `futon2/holes/labs/library-loop/worklist.edn`.
Review exactly the first row named above. Author and reviewer must differ.
Read every commit named in `:evidence`, verify the diff against the statement
and acceptance, and rerun the relevant parser/reproducibility checks in an
isolated sibling worktree when the library commit is under review. Do not use
or reload a live JVM.

Check that commits are path-scoped, no other agent's edits were swept in,
discovery rows contain no implementation, enumeration claims include their
complete search, and annotations/patterns have the required source pointers.
On pass, set `:status :done` and add `:reviewed-by`, UTC `:reviewed-at`, and a
specific `:review`; on a substantive failure reopen it with `:review-finding`.
Small corrections may be follow-up commits, never amendments. Run the board
checker and commit only the lane paths. Report the result in three lines.
