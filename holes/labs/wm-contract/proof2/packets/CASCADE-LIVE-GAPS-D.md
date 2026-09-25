# CASCADE-LIVE-GAPS-D — the data Cascade Live lacks for the outer cascade, sized

claude-12, 2026-09-25, on claude-8's requisition. Read-only: GETs against :7070
and futon1b :7073 only, nothing loaded into a JVM, nothing under `data/`.
Follows OUTER-CASCADE-D (futon2 `595de935`, `f36820a4`). Sizing only: each
change below is a packet for the file's owner, not a proposal adopted here.

Read at futon3c `34c7d59f` (`logic/cascade_real_live.clj` last changed
`d0741b04`; `substrate/client.clj`; `agency/clock_lineage.clj`,
`clock_decision.clj`) and futon2 `9df277b1` (`aif/target_field.clj`). Store
walks: futon1b `GET /api/alpha/hyperedges?type=<t>&limit=1000`, paged by
`after`, one run each, 2026-09-25. Owners: `GET /api/alpha/cascade-real`
`owners` (O1 claude-2, O2 claude-1, O3 claude-4, O4 claude-10, O5 claude-4);
the composer file names none, so the dimension owner is named per gap.

**Answers up front.**
1. Excursion nodes exist in the store on 13 clock edges only; a composer regex
   gives them lineage. Patterns for E-/T- need the ingest extended (store write).
2. The pattern section fails at ~5 s per 100-row page. A per-type deadline or a
   cached read with as-of keeps the regenerate-from-live standard.
3. Of 88 clock edges, 29 name their caller; 38 + 14 need it written at clock
   time (`clock_decision.clj`, `clock_lineage.clj`), not reconstructed.
4. Sorrys (`sorrys.edn`, 4 open) and tensions (a 06-03 artefact, 4 nodes) are
   not in the store: typed absences before the first flight.
5. Requisition state: the outer cascade reads it from the field only.
6. Cost: ~20 s → ~75 s under 2(a), unchanged under 2(b); the rest < 1 s.

## 1. Excursion and ticket nodes

**Needed.** For the 282 excursions and tickets of 354: the facts missions
get (cited patterns, cluster/basin, held, lineage).

**Today.** The join takes the first endpoint matching `-d/mission/`
(`cascade_real_live.clj:273`, `mission-ep`). Endpoint kinds, from the store walks:
`code/v05/mined-move` (O1) 177 rows, 354 mission ends, 6.85 s;
`cascade/cluster-member` (O4) 117, all mission, 3.80 s; `held/on-mission` 124,
all mission, 3.44 s; `cascade/hole-target` (O5) 0, 1.92 s; `mission-scope/pattern`
1,173, all mission, 55.9 s; `clock/clocked-on` (O3) 88: 75 mission, **13
excursion**, 5.05 s.

So excursion nodes exist in the store, named `<repo>-d/excursion/<id without
E->` (e.g. `futon2-d/excursion/kimi-task-28`, `futon2-d/excursion/cascade-real`),
on clock edges only. No edge type has a ticket endpoint.

**Do excursions cite patterns?** Measured by text, not by edge: a library id
`<ns>/<name>` (1,395 in `futon3/library`) appearing in the target's own file.
Missions: 26 of 59 cite at least one (170 citations). Excursions: 44 of 265
(143), 3 of them `E-kimi-task-N`. Tickets: 2 of 30 (7). None of these
excursion or ticket citations are edges: `mission_scope_ingest.clj` builds
binders from `futon6/data/mission-scope-trees` keyed on `M-` stems (`:301`,
`:734`–`:864`).

**Smallest change.**
- Composer only: an `entity-ep` matching `-d/(mission|excursion|ticket)/`
  beside `mission-ep`, used by `lineage-section` (`:357`) and the summary's O3
  claims (`:65`). This gives the 13 excursion lineage rows; O1, O4 and held
  give nothing for E-/T- because no edge has such an end.
- `doc-files` (`:275`) matches `[ME]-*.md`; tickets need `T-` added and a
  `:kind "ticket"` in `doc-row` (`:303`).
- Pattern citations for E-/T-: extend the ingest to excursion and ticket files.
  That writes new binder edges to futon1b (store change), and it adds rows to
  the type that already times out (§2). Owner: O4 claude-10 (the backlink is
  O4's, per `mission-pattern-section`'s docstring).

**If not made:** `{:absent :no-excursion-ticket-nodes}` on the 282, as
OUTER-CASCADE-D §5. After it, O1/O4/held stay `{:absent :no-edge-of-kind}`.

**Falsifier.** After the composer change, `lineage` must list exactly the 13
excursion rows the store holds today (or its count then); a nonzero O1, O4 or
held count for any excursion without a store write means the regex matched a
mission node.

## 2. Mission → pattern citations

**Needed.** The target's cited patterns (the O1×O4 fact).

**Today.** `/cascade-real/graph` `section-status."mission-scope/pattern"` =
`failed` on both reads (15:16Z, 15:25:17Z; `:timeout-ms 5000`). The walk pages
at 100 rows (`client.clj:101-110`, set on 08-23 when this type had 971 rows:
limit=100 took 2.6 s then). Today 1,173 rows; my direct walk took 55.9 s for 2
pages of 1,000. With the 08-23 form (~2.5 s fixed plus a per-row term) that
is ~5 s per 100-row page, at the limit: an estimate, not a measured page.

**Options, against s1–s5** (`/cascade-real` `standards`):
- (a) A per-type page deadline in `fetch-edges` (`:34-43`), e.g. 15 s for this
  type. Same live read, so s1 holds. ~12 pages × ~5 s ≈ 60 s on one worker,
  inside `graph-fetch-deadline-ms` 90 s.
- (b) A cached read: the section served from the last completed live walk with
  its own `:as-of-ms`. s1 holds if only the live walk fills the cache and the
  as-of is on the section. Serving ~0 s; refresh ~60 s off the request path.
- (c) "The composer reads the hyperedges directly" is what `fetch-edges`
  already does (`:39-43`); not a separate option.

`client.clj:108-110` records why the 5 s budget was not raised on 08-23: it is
the signal that tells a big page from a slow substrate. Option (a) spends that
signal for one type only; (b) keeps it and moves the wait. A store-side fix
(faster pages for 2.5 KB rows) is futon1b work and would help every type.
Owner: O4 claude-10 (section), futon1b for a store fix.

**If not made:** the outer cascade reads the type directly (as OUTER-CASCADE-D
did), or carries `{:failed-read :section "mission-scope/pattern" :at …}`,
never `0`.

**Falsifier.** After (a) or (b), `counts.patterns` must equal the store's
`mission-scope/pattern` count at that as-of (1,173 today), and the note's
mission count must equal the distinct `-d/mission/` ends (223 today).

## 3. Lineage attribution (`:dispatched-by`)

**Needed.** Each lineage entry carries `:dispatched-by <caller-id | nil>`, so
self-caused lineage can be told apart. Whether to exclude it is not decided;
this sizes the field.

**Today.** `lineage-section` (`:357-371`) emits `{:agent :target :session :at}`
and drops the witness. The witness on the store edge (written by
`clock_lineage.clj` `persist-clock!`, `:138-160`) already says, for all 88
edges (75 on missions, 13 on excursions):
- 38 `rule "dispatch-mission-id"`, `source "invoke-receipt"`: dispatched, caller
  not named on the edge;
- 29 `clock-decision` with `source "inherited"` and `evidence.caller-id`
  (`clock_decision.clj:125`): caller named;
- 14 Kimi seats, `clock-decision` `source 1` with `evidence.targets`: the target
  came from the requisition, the caller is not on the edge;
- 7 own: 5 clock decisions, 1 edit activity, 1 selection decision.

Correction to OUTER-CASCADE-D §5 (`f36820a4`): its split 38/14/14/6 covered
only the mission-ended edges at that read (72 then; 75 now, 38/17/14/6), not
all 88 as its sentence says.

**Smallest change, two parts.**
- Composer only: `lineage-section` passes the witness through as
  `:dispatched-by` = `evidence.caller-id` when present. Covers 29 rows now.
- Written at clock time: where the clock decision is made for an invoked job
  (`clock_decision.clj`, and `clock-dispatch!` in `clock_lineage.clj:167`), put
  the job's `caller` and `job-id` in the witness. Reconstructing later fails:
  job records age out (kimi-10, -13, -14 have none), and the `E-kimi-task-N`
  header covers only script dispatches. futon3c agency change; no store schema
  change (props are free-form). Owner: O3 claude-4.

**If not made:** `{:dispatched-by {:absent :not-recorded}}` per entry, with the
29 inherited rows readable today.

**Falsifier.** Every Kimi row written after the change names the `caller` of
the invoke job that started it; a nil there means the write path missed it.

## 4. Tensions and sorrys

**Needed (E-outer-loop, `aedcc6ae`, O6).** The old loop proposed open Lean
sorrys and high-curvature nodes.

**Sources that exist.**
- Sorrys: `futon2/resources/sorrys.edn` (`aif/sorry_registry.clj:40-48`), 22
  entries: 12 addressed, 6 n-a-by-design, 4 open, 1 acknowledged; not in the
  store. Cascade Live's 9 `held` rows from registry `"sorrys"` are other items
  (`held/item/sorry/<id>`, e.g. `first-flights-phase-b-policy-grade-G`).
- Tensions: `futon2/src/futon2/aif2/tension.clj` reads a delivered artefact,
  `futon3c/holes/missions/M-substrate-metric.R2-curvature-full.json` (file
  dated 2026-06-03; `top_propose_candidates` 4 nodes, all `-d/mission/`). No
  recomputation path is wired (`tension.clj:178-182`: "recompute-per-scan is a
  later upgrade").

**Sizing.** Either would be a section reading a file, not the store, unlike
every other section. With 4 open sorrys and a three-month-old artefact naming
4 missions, neither is worth composing before the first flight.

**Carried by the outer cascade:** `{:absent :sorrys-not-in-cascade-live :source
"futon2/resources/sorrys.edn" :open 4}` and `{:absent :no-tension-dimension
:artefact-date "2026-06-03"}`. Owners: sorrys, no owner found; tension, M-aif2
(no seat named).

**Falsifier.** A later sorry section must report `sorrys.edn`'s `:status :open`
count at the same sha; a tension section, a subset of `top_propose_candidates`.

## 5. Requisition state

**From the field only.** `target_field.clj` reads the line at HEAD
(`requisition`, `:162`) and sets eligibility from it (`with-eligibility`); the
outer cascade's problem is the field, so the fact is already on each entry. A
second parser in Cascade Live could disagree with the first and adds nothing.

Cascade Live does show requisitions in one place: `tickets-section`
(`:338-355`) lists recent unclocked `[ME]-*.md` docs by mtime, and at the
15:25Z read 35 of its 40 items were `E-kimi-task-N` (minted that day; the later
commit futon2 `9eddeb42` touched all 38 again). Nothing in the outer cascade
reads that list; if something ever reads it as work, it needs the same field.

**Falsifier.** Delete one `E-kimi-task-N` requisition line in a scratch copy:
the field's `:eligible` must flip to true for that entry, and Cascade Live's
output must not change.

## 6. Cost, by reasoning from the recorded timings

Baseline (OUTER-CASCADE-D §6) about 20 s; the graph runs its walks on 2 workers
(`:476`), so its time is roughly the longest chain. Change 1 in the composer:
under 1 s (`doc-files` already walks every `holes/`, `:282-286`); the ingest
for E-/T- adds rows to the slow type, ~25 ms each, ~150 rows ≈ +4 s. 2(a): the
pattern walk becomes the graph's critical path, build ~20 s → ~75 s. 2(b):
serving unchanged, a ~60 s refresh off the request path. 3, 4, 5: none
measurable (the witness is already in the fetched rows; absences are free; the
field already reads the line). No new runs were made for this section.
