# C435 — derived gate repository scope

The workspace gate now pins Futon3c in addition to Futon2, Mathlib4, p4ng, and
Futon3. The reload/click/certificate rehearsal therefore cannot affect the gate
without its repository appearing in `PROVENANCE`, `PROVENANCE-FINISH`, `BASIS`,
and the latest-run receipt.

Repository scope is derived from every positive and control command's execution
`:dir` (defaulting to Futon2) plus absolute argv paths beneath
`/home/joe/code/`. The derived set is reconciled with the provenance map before
any command runs. Current result: five reached, five pinned, zero unpinned and
zero unresolved. A synthetic command rooted in an unpinned sibling is rejected
by the gate control.

This derivation claims the gate's declared process-entry repository surface. It
does not infer arbitrary repositories opened internally by child programs; a
child with such a dependency must expose it in command metadata or its own
receipt. That limit is narrower than the defect repaired here and is stated
rather than silently promoted to transitive dependency discovery.

C431's receipt and runbook now say five repositories. No full gate was run in
this focused packet. Clj-kondo and inventory are clean.
