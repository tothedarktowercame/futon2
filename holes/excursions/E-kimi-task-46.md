# E-kimi-task-46 — Design the futon1b hyperedge + superpod-scope sidecar (design note + prototype measurements)

**Requisition:** in-progress — dispatched 2026-09-26T09:34:58Z to kimi-5 as invoke-1790415298714-24746-cc2f6684

Clocked in by claude-12 for kimi-5 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet — design the hyperedge and scope sidecar for futon1b (design + measurement only)

Requested by Joe, 2026-09-26: "requisition the additional sidecar … keeping in mind that we
have even more complex scope requirements with regard to futon6 superpod results" (example:
https://zone.hyperreal.enterprises/wip/mark7-math_9906038-margin.html). Reviewer: claude-12.

This packet produces a design note backed by measurements. It does NOT change futon1b server
code, and does NOT restart or reload any JVM. Implementation packets follow after review.

## What already exists (read these first)

- `futon1b/futon1b_text.clj`: the SQLite sidecar beside XTDB. Tables `ev_fts` (FTS5),
  `ev_attr` (evidence scalars, B-tree indexes), `ev_tags`, `fts_meta` (watermark). Rebuilt
  from the store; candidates are point-hydrated and re-checked against XTDB.
- `futon1b/holes/M-evidence-landscape-index.md` (the mission that built it; its two
  invariants, its scope-out), `futon1b/holes/SPIKE-attribute-index-2026-07-26.md` (its
  "Recommended sequence": hyperedge indexing only with write, replacement and retraction
  coverage plus a rebuild oracle), `futon1b/TN-xtdb-derived-secondary-index.md`.

## Why now (measured by claude-12, 2026-09-26, futon1b :7073)

- `GET /api/alpha/census?type=code/v05/edits` → 551,794; `code/v05/commit` → 18,524.
- Paging `hyperedges?type=code/v05/edits&limit=1000`: ~40 s per page (~6 h for the type).
- `hyperedges?type=code/v05/edits&end=<one commit sha>`: 35 s for 463 rows — a filter to a
  single endpoint costs about the same as a page, i.e. a scan, not a lookup.
- `limit=100`: 18 s. (These overlapped a running bulk pull, so treat them as rough.)
- `futon6/scripts/mission_efe_scope_dump.py` pages every `mission-scope/<binder>` type with
  `include-total=true` on the first page; the EFE publish spent most of 18+ minutes there.
- Writer of v05 edits: `futon3c/src/futon3c/watcher/commit_ingest.clj`
  `ingest-edits-for-commit!` — one edge per var in each changed file, vars read at HEAD.

## The superpod (mark7) scope requirement

A mark7 run writes per-paper artifacts as files. Example on disk: `/tmp/r7v` (run
mark7master-20260921, 8 papers): `artifacts/expo/*.edn` 1,111 files (each a passage with
`:paper/id`, `:passage/id`, `:source {:lines [a b]}`, and `:scopes` with `:kind`,
`:source {:lines}`, `:slot-fill`); `artifacts/graphs` 743 (proof graphs; nodes cite S1
clause units); `artifacts/marks` 12 files, 17 MB (S1 marks placed by source offset);
`expo-candidates` 2,420. Scope kinds seen: connection 693, expository 277,
auxiliary-construction 39, universal-property/characterizes 35, generalisation 16.
Readers today: `futon6/scripts/render_scope_margin.py`, `render_scope_view.py`,
`expository_scope_audit.py`. Planned scale: `futon6/holes/mark7-ct-run-plan-5000.md`
(5,000 papers, all of category theory).

## Deliverable

One note, `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md`, containing:

1. **Query inventory.** The reads the sidecar must answer, each with its caller and its
   current latency (measure; quote the command). At least: hyperedges by type (paged);
   by type + one endpoint; by type + endpoint prefix (e.g. repo); count by type;
   mission-scope per binder (the EFE dump). For superpod scopes: all scopes of one paper;
   scopes overlapping a line/offset interval of a paper; all scopes of a kind across papers;
   join from a proof-graph node to the S1 mark and S4 scopes over the same span.
2. **Where the superpod data should live.** Decide and argue: (a) ingested into futon1b as
   hyperedges and indexed by the sidecar; (b) indexed by the sidecar straight from the run's
   files, with the files as the source of truth; or (c) a per-run SQLite file beside the run.
   Say what each costs at 5,000 papers (estimate rows and bytes from the 8-paper run), what
   provenance each keeps (run id, model, generator), and how a re-run of one paper replaces
   its rows.
3. **Schema.** Tables and indexes for hyperedges (type, endpoint position, endpoint id,
   hx id, and the props the queries above filter on) and for scopes (paper, passage,
   interval, kind, run). Interval queries: say which SQLite mechanism (plain B-tree on
   (paper, start, end), or the R*Tree module) and why.
4. **Maintenance contract.** How rows are written (from the write log? at post time?),
   how replacement and retraction are covered, the watermark, and the rebuild oracle, in
   the terms M-evidence-landscape-index already uses. XTDB stays the truth for anything
   stored in it; candidates are re-checked.
5. **Prototype numbers.** Build a throwaway SQLite file under /tmp (not in any repo):
   - from the cached v05 pages in `/tmp/v05-cache/{commit,edits}/page-*.edn` (19 commit
     pages, 160 edits pages — EDN text; a regex parse is fine), measure lookup of one
     commit's edits by sha and a count by repo;
   - from `/tmp/r7v/artifacts/expo/*.edn`, measure "scopes of paper P overlapping lines
     [a, b]" and "all scopes of kind K".
   Quote build time, file size and query times.
6. **What should not be stored.** Note, with the numbers, whether commit→var edits (551k,
   derivable from git commit→files in ~2 s for all repos) belong in futon1b at all, or
   whether the watcher should write commit→file instead.
7. **Implementation packets.** A numbered list of small follow-on packets (one behaviour
   each, with its acceptance test), in the order you recommend.

## Constraints

- Read-only against futon1b :7073 and the repos. No server code edits, no reload, no
  restart. Keep your own load light: sequential requests, no bulk paging of large types
  (a few pages at most for timing).
- Commit only the note, in futon1b, with an explicit path (`git commit -- <path>`). No
  stash, no amend, no branch switch.
- Say "measured" or "estimated" for every number.

## Reply

Bell claude-12 back with the commit sha, the decision in item 2 in one sentence, the
prototype query times, and the list of implementation packets.
