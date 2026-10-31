# Q1-GIRAFFE-FIELD-STALENESS — discovery (zai-7, 2026-10-31)

Discovery only. No source edits, no Giraffe write, no click. Row
`Q1-GIRAFFE-FIELD-STALENESS` in `criteria-completeness-deficits.tsv`.
Everything below was checked live on 2026-10-31 unless pinned otherwise.

## 1. The supported command that produces a verified Giraffe run

`clojure -M -m futon3c.giraffe.run --edges <file> [--only <ids-file>]
[--root <dir>] [--run-id <id>] [--source tickets|missions|missions-excursions]
[--repos <comma-separated repo roots, missions source only>]
[--readings-root <checked-reading-directory>]`

- Namespace: `futon3c/src/futon3c/giraffe/run.clj`, `-main` at line 341
  (docstring pins the CLI). Default root `/home/joe/code/storage/giraffe/`.
- `--edges` is required; `--readings-root` requires `--source
  missions-excursions` and requires `batch-*.jsonl` plus `check_edges.py`
  and `members.json` in that directory (refuses `:giraffe/readings-empty`).
- `-main` composes adapter + pass + `write-run!` (run.clj:216-260) and
  prints the run dir and manifest path. `write-run!` writes
  `input.edn`, `rejected.edn`, `pass.edn`, `work-clusters.json`,
  `report.md`, optional `excluded.edn`, and `manifest.edn` listing each
  file's sha256 plus the code's own `:git-sha`. `verify-run`
  (run.clj:303-340) recomputes every listed sha; the futon2 consumer
  (`full_loop_runtime/find-giraffe-run`, `outer_observation/giraffe-field`)
  serving-resolves `futon3c.giraffe.run/verify-run` and refuses anything
  that does not verify. There is no other supported writer: the run001
  programmatic publishes (`q1-et-admission/publish.clj`) are ad-hoc
  wrappers around the same `write-run!`, not a second entry point.

## 2. What it costs

- **Model calls: none.** The pass is pure computation. The adapter reads
  files and the checked-reading corpus; nothing in `run.clj` invokes a
  model or an API. (Run 213938's own report shows its 97 edges sourced
  `prose-reading` — readings that were produced *earlier*, by reading
  jobs, and merely replayed from the corpus.)
- **Time: bounded by witness, not measured here** (no write was
  authorized for this discovery). Witness: the canonical edit adding
  `--readings-root` was noticed at 2026-10-30T21:35:31Z
  (`zai-coordination/canonical-edit-coordination.txt`), the code was
  committed as futon3c `6b5597d03` at 21:36:42Z, and the run landed at
  21:39:38.828Z — so a full run including JVM startup and corpus read
  fits inside ~3 minutes wall clock. The independent registry scans it
  subsumes cost 127–196 ms each (`holes/labs/wm-contract/runs/
  U37-enumeration-completeness/scan-cost-2026-09-03.txt`).

## 3. What it reads and writes

Reads: the mission repos (`gm/default-repos`, `giraffe/missions.clj:13`,
overridable with `--repos`), `holes/tickets` + `holes/tickets/
tickets-index.edn` when tickets are included (readings scope), the edges
file (pinned in the manifest's `:sources`), and the readings corpus
under `--readings-root`. Writes: exactly one new run directory under the
root (six or seven files, listed above). No canonical store, no ledger,
no outer-state write. Runs are append-only: run-id is a UTC timestamp
plus the first 8 hex of the input sha, so the same input re-mints a new
timestamped dir rather than mutating the old one.

## 4. Safe between clicks?

Yes. The launcher enforces single-flight (no second active click;
`scripts/wm_click.sh` casting wait), and a click reads the field once at
its start (`find-giraffe-run` → observation), so a run written mid-click
cannot change the in-flight click's field. Between clicks it is the
intended lever: a new verified run changes `:giraffe/manifest-sha256`,
which changes every target's effective-run identity, readmits targets
the duplicate guard was blocking (my `duplicate-target-preselection`
exclusion keys on exactly that manifest change), and makes work opened
after the previous run (e.g. ticket `T-repair-ea1-19929445…attempt-002`,
minted 2026-10-31 01:13Z, invisible to the 2026-10-30 21:39Z field)
enumerable again. Two cautions: (a) the manifest's `:git-sha` records
futon3c HEAD, so run it from a clean canonical checkout — an
uncommitted-worktree sha would be unresolvable later; (b) E3 reads the
manifest change between runs as an intervention by design — it is
recorded, not erased.

## 5. How run `20261030T213938.828Z-dc280cbf` was made

Pinned from its own manifest and report plus repo history:
`--source missions-excursions` with a readings corpus (report shows 608
open items across missions/excursions/tickets, 97 `prose-reading`
edges, 243 `rejected-at-reading`), edges file
`/home/joe/code/futon3c/holes/missions/giraffe-mission-edges.json`
(sha `2c728646…`, pinned in `manifest.edn :sources`), written by
futon3c code at `6b5597d03` ("Run Giraffe from checked reading corpus",
codex-5, Agency job invoke-1792440879921-17777-b8d1290e), three minutes
after that commit. Its manifest lists `work-clusters.json` and it
verifies true from futon3c as cwd (checked live 2026-10-31).

## 6. Should it run at a defined point — and the smallest no-timer way

Yes: the field should not be older than the click that consumes it, and
the freshest verified run should not be an accident of who last ran the
command. Smallest no-timer mechanism: **click preparation**. The
launcher already has exactly one per-click moment before firing —
`prepare_sources` inside `scripts/wm_click.sh` (and the equivalent
`wm_click_prepare.py` step). Run the supported writer there, gated on a
staleness predicate so an unchanged population mints nothing: compare
the newest verified run's `input.edn` sha256 against a freshly computed
adapter-input digest; mint a run only when they differ. No timer, no
daemon, no new serving surface — the event is "a click is being
prepared", which is precisely when freshness matters and when
single-flight already guarantees no concurrent consumer. (Alternative,
even smaller but weaker: have `find-giraffe-run`'s empty-miss refusal
name the supported command. That fixes availability, not staleness, and
leaves this row open.)

## 7. Do the retained-run `work-clusters.json` verify failures affect a fresh run?

**No.** The failure is a *format-baseline* question about old
manifests, not a defect in new writes:

- A run written by current code lists `work-clusters.json` in its
  manifest, so it verifies under the current baseline from any cwd
  (run 213938: `verify-run` → true, checked live).
- `verify-run` decides legacy-vs-current from the manifest's own
  `:git-sha` by asking the repo at `System/getProperty "user.dir")`
  (run.clj:283-301). Run `20261028T154151.285Z-b916cd4e` predates
  `work-clusters.json` (its git-sha `ff0734e1c` does not descend from
  `e90fe14bb`): from futon3c as cwd it resolves as legacy and
  **verifies true** (checked live, 2026-10-31). The failures seen in
  futon2 test processes are a cwd artifact: `user.dir` is the futon2
  worktree, the futon3c sha does not resolve, the format is
  `:undecidable`, and the rule fails closed to the current baseline —
  the manifest then looks `missing-from-manifest`. Nothing was
  corrupted.
- Consequence for consumers: `verify-run` is only correct when evaluated
  with a cwd where the manifest shas resolve (the serving JVM does;
  `giraffe-verifier-integrate-one-click.txt` already flags this
  reviewer limitation). The two "pre-existing test defects" I named in
  the Q1 and DUPLICATE-TARGET rows are this cwd artifact, not giraffe
  corruption — worth one row of its own if futon2-side tests keep
  tripping on it.
