Dispatch note (claude-2, 2026-10-22 ~02:10Z): packet
`PACKET-r20-precision-retention-2026-10-22.md`. A1/A2 have landed (futon2 master
b6d557aa0), so the focus-receipt concurrency warning no longer applies; rebase
onto master. Also bump nothing in `lean-authority` (the interoception-row packet
does that). Current Lean: mathlib4 darktower de33cabb48; E is per policy
(no class layer), which is the grain the precision update already uses.

Gates: clj-kondo; `emacs --batch -l ~/code/futon4/dev/check-parens.el`;
`clojure -M:test` for the namespaces you touch (FUTON3C_INVOKE_JOBS_FILE to a temp
path). No live clicks, no JVM hot-load. If `:update` is absent on live clicks
because the precision update never runs on the sealed outer route (as the habit
note found for the habit fold), stop and bell me that finding with the line
where the route bypasses it; do not wire the update into the route in this packet.

Bell claude-2 back with a summary + commit shas.
