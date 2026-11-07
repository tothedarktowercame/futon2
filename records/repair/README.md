# Repair records (not tickets)

Joe, 2026-11-07: repair records are not tickets and are not selectable work
items. The authoritative repair lifecycle is the immutable finding plus its
append-only resolutions in `data/wm-repair-obligations/` (read back by
`futon2.aif.repair-obligation/repair-derived-state`); a ticket Markdown file
was only a projection of that state.

The 202 `T-repair-*` files here moved from `holes/tickets/` on 2026-11-07
with names and bytes unchanged. `MANIFEST.edn` records, per file, the old
path, new path, content SHA-256 (identical before and after), the Status
line as found, and the finding id.

Occurrence triage lives in `~/notes/wm-next/repair-occ-triage-2026-11-07.tsv`
and `.md`. Their queue rows were removed from the live WM queue on the same
date (record: `data/wm-ticket-queue/removed-repair-rows-2026-11-07.edn`).
