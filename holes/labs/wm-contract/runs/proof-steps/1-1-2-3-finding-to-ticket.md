# Proof ⟨1⟩1 ⟨2⟩3: finding → ticket chain, verified read-only

Instance: `repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`
— the environmental-hold finding from click 3 (run 2026-09-22-1790053967,
attempt-002, failure stage `:revision-wait`), which produced ticket
`T-repair-occ-444fb018…` at the front of `data/wm-ticket-queue/queue.edn`.

## The hops, as verified

| # | Hop | Function (file:line) | Record written (path) | Link id to next hop |
|---|-----|----------------------|----------------------|--------------------|
| 1 | Tripwire witness recorded, run continues | `note!` (`tripwire.clj:979`); called from `observe!` (`tripwire.clj:1095`) | trip report under the trip-report root (via `record-trip!`) | `:trip/id`, `:trip/wire-id` carried in the report |
| 2 | Trip report → repair finding | `record-trip!` (`tripwire.clj:824`) → `handle-action!` (`tripwire.clj:805`; dispatches on `:trip/action`) → `record-finding!` (`tripwire.clj:750`) builds the finding map | in-memory finding map | the finding map handed to `repair/record-system-failure!` |
| 3 | Finding persisted | `record-system-failure!` (`repair_obligation.clj:546`, the default `:tripwire/repair-record-fn`, per `tripwire.clj:771`); writes via `write-new-or-identical!` (`repair_obligation.clj:312`) | `data/wm-repair-obligations/findings/repair-occ-444fb018….edn` (exists; `:repair/id` = the instance id, `:repair/status :open`, `:repair/class :environmental-hold`, `:opened-at 2026-09-22T05:13:31.542488602Z`) | `:repair/id` |
| 4 | Finding → ticket + queue entry | `finding-ticket/publish!` (`finding_ticket.clj:54`), called from `record-system-failure!` (`repair_obligation.clj:588`) under the contended store lock; enqueues via `queue/enqueue!` (`ticket_queue.clj:57`) after `queue/validate!` (`ticket_queue.clj:20`) | `holes/tickets/T-repair-occ-444fb018….md` (exists, links the finding and its SHA-256) and the front entry in `data/wm-ticket-queue/queue.edn` (exists, `:inserted-at 2026-09-22T05:13:31.542488602Z` — equal to the finding's `:opened-at`, as `publish!` copies it) | ticket name = `T-` + finding id; plus the immutable publication receipt `data/wm-repair-obligations/ticket-links/repair-occ-444fb018….edn` (exists) pinning finding-sha256 → ticket path → queue path |

Every path above was opened read-only on 2026-09-22; every named file exists and
every id linkage was checked end to end (finding id → ticket filename → queue
entry `:ticket` → `:inserted-at` = finding `:opened-at`).

## Honest note on this instance's entry point

The tripwire leg (hops 1–2, `note!` → `record-trip!` → `handle-action!` →
`record-finding!`) and the runner's stop-line recording are two entrances that
**converge at hop 3** (`record-system-failure!`). This particular instance
entered at hop 3 from the runner side, not from a tripped wire: its
`:repair/class` is `:environmental-hold` and its stage is `:revision-wait`,
whereas `record-finding!` (`tripwire.clj:762`) types its findings
`:machine-failure` with a `:trip/id`. So the instance proves hops 3–4 live
(finding → ticket → queue, the part the firing-policy change could have
endangered); hops 1–2 are verified in code (`tripwire.clj:979/824/805/750`)
with their convergence at the same writer, and were also exercised live on
2026-09-19/21 by machine-failure findings from tripped wires.

## 514d8dca did not touch any hop

`git show 514d8dca --stat`: one file changed, `scripts/wm_click.sh` (+101/−43).
The commit changed only the shell's decision to fire (findings print, casting
waits, misconfigurations exit 3). No function in `tripwire.clj`,
`repair_obligation.clj`, `finding_ticket.clj` or `ticket_queue.clj` was
modified, and nothing the tripwires do during a run changed.
