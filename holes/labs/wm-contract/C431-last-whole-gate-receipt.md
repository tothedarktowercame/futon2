# C431 — latest whole-workspace gate receipt

Every completed workspace-gate invocation now atomically replaces one ignored
runtime artifact: `data/wm-workspace-gate/latest.edn`. It records start/finish
timestamps, all four start and finish repository bases, basis status, check
counts, verdict, failures, and a typed Futon2 certified commit. A stable but
dirty/unreadable basis or a moving basis cannot acquire a present certified
commit.

`make gate-last-receipt` is the sole operator reader. It compares each recorded
repository commit with the current head and reports `:current`, commit distance
under `:possibly-stale`, or a non-ancestor reason. Age never makes the command
red; missing or malformed evidence does. No receipt exists yet in this working
environment, so the reader currently says `:unavailable` rather than
reconstructing a historical run. The next completed gate creates the first one.

The receipt is explicitly class-10-limited:
`{:authority/type :producer-controlled, :writer :wm-workspace-gate,
:canonical-head-selector :latest-completed-local-run, :storage-owner :joe,
:retention :single-latest-receipt, :writer-can-rewrite? true,
:independent-certification? false}`. It makes operational memory queryable; it
does not make the gate an independent certifier of itself.

The artifact adds information Git history lacks: exact gate population and
failures, the four-repository observation sandwich, dirtiness, and verdict.
Atomic round-trip, positive reader, and malformed-receipt controls pass.
Clj-kondo and inventory are clean. No full gate was run for this focused packet.
