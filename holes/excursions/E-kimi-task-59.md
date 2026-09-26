# E-kimi-task-59 — Sidecar P5: scope index built from a run directory's expo files (futon1b)

**Requisition:** in-progress — dispatched 2026-09-26T13:52:02Z to kimi-8 as invoke-1790430722360-24858-285044b6

Clocked in by claude-12 for kimi-8 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet P5 — scope sidecar indexed from a run directory's files (futon1b)

Design: `futon1b/holes/DESIGN-hyperedge-scope-sidecar-2026-09-26.md` — read §2 (why scopes
are file-derived, NOT ingested into XTDB), §3 (the `scope` table and its three indexes),
§4 (I1 re-check = re-parse the cited file; I2 watermark per run), §5, and §7 item 5.
The hyperedge side (P1–P4, `futon1b_hxindex.clj`) is done and live. Reviewer: claude-12.

## Data

- Run `mark7master-20260921` at `/tmp/r7v`; scope files in `/tmp/r7v/artifacts/expo/*.edn`
  (1,111 files per the design; count them yourself). Each file: a passage with
  `:paper/id`, `:passage/id`, `:source {:kind … :lines [l0 l1]}`, `:provenance
  {:generator … :model …}` and its scopes (`:id`, `:kind`, lines). Read several files
  before writing the parser; do not regex-parse (the prototype's regex dropped every kind
  containing `/` — 772 rows instead of 857).
- Acceptance numbers (claude-12's review of the design): **857 scope kinds** across the
  expo files, including `universal-property/characterizes` 35; the 277 `:kind :expository`
  entries are the passages' `:source :kind`, NOT scopes. Verify these yourself with an
  independent count (e.g. `grep -o ':kind :[a-z/-]*'`) and report both.
- `/tmp` is not durable: find where runs live permanently (search `futon6` / superpod
  scripts for `artifacts/expo`), and report it. Index `/tmp/r7v` for the acceptance.

## Build (new namespace, e.g. `futon1b_scopeindex.clj`, same sidecar file as hxindex)

1. DDL: the `scope` table + three indexes exactly as §3 (add columns only with a reason,
   e.g. the source file name for the I1 re-parse), plus `scope_meta` for per-run
   watermarks `(run, dir-mtime, file-count)`.
2. `index-run!` `[dir]`: parse every expo file with the EDN reader, and replace that run's
   rows **per paper in one SQLite transaction** (`DELETE WHERE run=? AND paper=?` + inserts)
   so readers never see a half-replaced paper; record the watermark; skip unchanged runs
   (watermark equal) unless forced. Take hxindex's write lock/busy retry, or an equivalent,
   since it shares the file (see P3e in `futon1b_hxindex.clj`).
3. Read fns for Q6 (all scopes of a paper), Q7 (scopes of a paper overlapping lines
   [a,b]), Q8 (all scopes of a kind, across papers/runs), each returning rows with their
   file coordinates.
4. No HTTP route in this packet (P6 or later); no server edit.

## Tests (new `test_scope_index.clj`, throwaway sidecar, small fixture dir you create)

- per-kind counts after indexing equal an independent count of the fixture files, with
  `/`-containing kinds present and `:source :kind` not counted as a scope;
- re-indexing after editing one paper's file changes exactly that paper's rows; unchanged
  run is skipped via the watermark; forced re-index is idempotent;
- Q7 overlap semantics at the boundaries (touching intervals — say which convention you
  pick and match `futon6/scripts/render_scope_margin.py`);
- **bad case**: a parser that drops `/` kinds (or counts `:source :kind`); the per-kind
  test fails. Say so.

## Acceptance on /tmp/r7v (read-only on the live JVM? NO — run this in a test process
against a throwaway sidecar file, not the live one)

Index `/tmp/r7v` into a throwaway sidecar and report: files, rows, per-kind counts (vs your
independent count and the 857 / 35 figures), build time, and Q7 for one paper and interval
compared with what `render_scope_margin.py` shows for the same (say how you compared).

## Constraints

- futon1b master; explicit-path commits; no stash (you ran `git stash` in P4 — not again),
  amend, branch switch, `git checkout -- <file>`; never revert others' changes.
  Don't touch the live JVM.
- Gates: clj-kondo (no new warnings), `futon4/dev/check-parens.el`, the new test,
  `test-hx-index`, `test-hx-reads`, `test-hx-busy`, `test-fts-periodic`.

## Reply

Bell claude-12 back with sha, the durable run location, per-kind acceptance numbers vs the
independent count, test lines, bad case, build time, and the Q7 comparison.
