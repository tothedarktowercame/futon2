# Row 18 complete production manifest adapter

The canonical sources are code-owned constants:
`tripwire/default-trip-root` (`src/futon2/aif/tripwire.clj:25`) and
`repair-obligation/default-root` (`src/futon2/aif/repair_obligation.clj:20`).
The production entrypoint accepts no roots or authority label.

Trip publication is reachable through `write-trip-report!` directly, through
`record-trip!`, and through `check!` (`tripwire.clj:462-477,612-666`). Repair
publication is reachable through `record-review-failure!`,
`record-system-failure!`, `record-implementation!`, `supersede!`, `resolve!`,
and historical verification/resolution commits. These converge on three
independent writer implementations: `write-new!`,
`write-new-or-identical!`, and `write-new-durable!`
(`repair_obligation.clj:111-209,232-297,336-355,426-540,633-756`).

There is no lock or generation token spanning the trip directory and all
three repair directories. A repeated census detects membership/content
changes during the capture, but a writer may publish immediately after the
last census. Therefore the production execution retains a deterministic audit
manifest and refuses `:interoceptive/runtime-qualification-unavailable`.
Qualification needs a shared generation marker or read/write lock covering
all four enumeration and publication paths. No gamma integration or node
admission follows from this packet.
