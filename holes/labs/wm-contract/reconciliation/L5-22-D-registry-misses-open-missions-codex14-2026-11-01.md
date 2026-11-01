# L5-22-D discovery — registry misses and open missions

Read-only discovery on 2026-11-01. No substrate write, refresh, reload, or
production mutation was performed.

## Short answer

The 15 missions are not outside the scanner's roots and their status syntax is
not rejected by the current parser. All 15 have substrate mission entities with
the exact file paths and SHA-256 values. They were absent from the selectable
roster because the substrate records carry stale `:mission/operator-gated?
true` classifications made by the old Status-prose gate rule. The serving
refresh reported all 342 scanned files unchanged before the failing selection,
so it did not repair those semantic records. Current source commit `a1419060a`
removed that rule: only an explicit `Gate:` line may gate a mission.

`M-ukrns-wp` is a different defect: it is a pathless foreign mission entity
created by the deprecated hinge-log bridge, not a vanished mission file.

## 1. What populates the substrate registry, and why the 15 disappear

### Writer, roots, and timing

The corpus writer is
`scripts/futon2/aif/mission_substrate_ingest.clj`. It calls
`mission-registry/load-missions-from-files` and
`upsert-mission-record!`. The same record builder is used by the futon3c
watcher lane. `load-missions-from-files` enumerates primary checkouts directly
under `~/code`; in each it reads both `<repo>/holes/M-*.md` and
`<repo>/holes/missions/M-*.md` (`mission_registry.clj`,
`load-missions-from-repos`). Therefore futon7 top-level `holes/`, futon3c and
futon5 `holes/missions/`, and the other locations in this set are covered.

Every production decision also declares a required refresh. In
`full_loop_runtime.clj`, `production-defaults` installs
`refresh-mission-substrate!` as `:refresh-fn`; in
`full_loop_runner.clj` the `:preference-refresh` phase runs it before selection
and refuses if the required refresh fails. A retained run at 21:07 UTC, before
the 21:26 selection record in this packet, says:

```clojure
{:scanned 342 :created 0 :updated 0 :unchanged 342}
```

Thus this was not simply “the ingest had not run since the files were written.”

### Exact cause

The old parser at commit `a4bd3eaf8` treated Status prose matching phrases such
as “awaiting Joe”, “ratification = Joe”, and “pending ... Joe” as an operator
gate. Commit `a1419060a18573d72f7d808a4abe8aaa6833c1ca`, **Require explicit
mission operator gates** (2026-10-01), deliberately removed that rule:

> Status prose records work state, not permission. Only an explicit Gate line
> can remove a mission from selection.

The current tooling file scan parses all 15 as live. The live substrate query
finds all 15 entities, but all 15 are non-live because their retained props
still have `:mission/operator-gated? true` and put their Status line in
`:mission/operator-gate-lines`. For example, the substrate entry for
`M-descriptive-essay-of-the-stack` has the exact futon7 path and file SHA, but
its ordinary `OPEN ... awaiting Joe` Status line is stored as its gate line.
The same stale shape appears for the other 14.

The counts from one tooling query were:

```clojure
{:file-count 342
 :substrate-count 344
 :file-matches 15
 :substrate-matches 15
 :file-live 15
 :substrate-live 0}
```

So the packet's inference “the substrate registry does not know them at all”
does not hold. The selection record does not expose these classified-away
entities in its open roster/non-open diagnostic, but the live substrate query
does. This is a semantic-ingestion/code-provenance defect, not a root or header
coverage defect. A correct repair must ensure the serving parser is the current
registered source and then rederive stored props; merely rerunning the stale
refresh repeats the wrong classification.

## 2. `M-ukrns-wp`

The substrate entity is `6fb9177c-67a0-41ff-9a20-4f108c5a11b6`, with only:

```clojure
{:scope/role "mission" :mission/relation "relates-to"}
```

It has no source, provenance path, SHA, status, or mission document. Searches
of the working trees and `git log --all -- '**/M-ukrns-wp.md'` across the futon
repositories found no file and no deletion commit. The file did not “go”
somewhere in git.

Its origin is the deprecated futon5a
`scripts/hinge_log_bridge.clj`. That bridge created `mission|M-*` entities for
both its owning mission and every cross-mission reference, with only name,
type, and external id. Its connection graph names `M-ukrns-wp`; the script's
header records that historical runs created about eight such foreign mission
entities and was deprecated on 2026-05-24 because those identities competed
with canonical mission ingestion. The 2026-09-17 registry audit independently
identified `M-ukrns-wp` as the sole pathless mission added by that bridge.

The substrate reader currently admits any entity of type `mission` whose
external id begins `M-`; it does not require `mission-doc-ingest` provenance.
Consequently this dormant bridge artifact becomes a live `:unknown` mission.

## 3. The two candidate fixes and Q1 independence

### A. Repair registry ingestion

Use the file census as the independent source and make the substrate ingest
faithfully cover the same declared file contract: both admitted roots, current
header/gate grammar, exact file SHA, and removal or typed quarantine of foreign
pathless entities. The refresh must also prove the loaded parser/source revision
that produced the stored props, not only compare the file SHA. This keeps Q1
meaningful: the census reads files, while the field independently reads the
materialized substrate registry; disagreement detects a stale or incorrectly
classified registry.

### B. Build the field roster from the file census

The field could take its IDs directly from `selection-world/capture-census`
and consult substrate only for pins/metadata. That makes the 15 selectable (or
typed as missing registry pins), and excludes pathless `M-ukrns-wp` if the
census is authoritative. But then Q1's two sets originate in the same file
scan, so `enumeratedTasks = openTasks` no longer checks registry completeness.
It becomes primarily a plumbing/partition check and cannot detect this incident.

Therefore A preserves the intended independent check; B does not.

## 4. Giraffe cone impact

The exact pass used by the failing selection is
`storage/giraffe/20261101T212428.677Z-6ff4449f/pass.edn`, pass SHA
`a6cf439872744816a8f7569f98143cc09dbd1bc7480a46f0843ba0c14ea91494`.
All **15/15** missing-from-field missions have dependency-reading rows and
ranking positions in that pass.

Three of the 15 carry non-empty downstream cones in this exact pass:

- `M-descriptive-essay-of-the-stack`: 8 downstream missions;
- `M-populate-substrate-2`: 1 (`M-fold-ansatz`);
- `M-war-machine-aif-completion`: 11 downstream missions.

Seven of the 15 also occur as members of at least one cone:
`M-fold-ansatz`, `M-populate-substrate-2`, `M-stack-essay-code-alignment`,
`M-stack-morphogenetic-rewrite`, `M-war-machine-aif-completion`,
`M-zaif-harness`, and `M-象-cascade`.

The packet's “cone of 9” example is one higher than the retained pass: this
pass records 8 for `M-descriptive-essay-of-the-stack`. Whichever cone meaning
is intended, the conclusion is unchanged: Giraffe's ranking graph incorporates
tasks the substrate-backed field could not select.

## Evidence consulted

- `data/wm-runs/selection-record-2026-11-01-wm-click-3c866bd4-f98b-4a7b-a8a8-c49087dfd155.edn`
- `storage/giraffe/20261101T212428.677Z-6ff4449f/pass.edn`
- `src/futon2/aif/mission_registry.clj`
- `src/futon2/aif/full_loop_runtime.clj`
- `src/futon2/aif/full_loop_runner.clj`
- `scripts/futon2/aif/mission_substrate_ingest.clj`
- `futon5a/scripts/hinge_log_bridge.clj`
- commits `a4bd3eaf8`, `a1419060a`, `188fdf67a`, and `7a5b54701`
- read-only substrate queries through `futon2.aif.substrate/entities-by-type`
  and `futon2.aif.mission-registry/load-missions`
