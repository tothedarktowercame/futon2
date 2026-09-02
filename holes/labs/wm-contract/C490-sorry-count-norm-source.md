# C490 — I6: `:sorry-count-norm` had no producer, and the refusal was right

Worklist row **I6** (harvested refusal class
`prediction-error-v1--source-field-missing`, PROPOSED-ROWS.md sweep 132).
The row asks one question: is the producer's refusal correct and the CALLER
must change, or is the refusal the defect?

## Answer

**The refusal is correct. The caller is the defect.**

`free_energy.clj:258-263` emits `:status :absent :absent-member :observed`
exactly when `channel-source-status` reports the channel unobserved, and it
carries the envelope's own `:reason`/`:paths` rather than inventing one. On
these three records the reason is `:source-field-missing` and the path is
`[[:graph :summary :total-sorrys]]` — which is `observation.clj:59`, the
requirement `channel-statuses` checks for `:sorry-count-norm`. The producer
said "nobody supplied this", and nobody had.

**Nothing in the tree wrote that path.** `scan-graph`'s `:summary` (at 4f8b918,
`war_machine.clj:2977-2980`; `graph-summary` now) emitted `:total-repos`,
`:active-repos`, `:coupling-edges` and `:ticks-firing` and no sorry count at
all. The channel's earlier source, `sorry-nodes` (`war_machine.clj:2840-2844`),
returns `[]` — superseded by `holes/problems/P-supersede-stack-logic-model.md`
— and when it went, the summary key went with it and nothing replaced it.
Grep confirms the absence rather than assuming it: `total-sorrys` appeared in
`observation.clj` (the two readers) and in five test fixtures, and in no
producer.

## The row's own hypothesis is refuted

I6's `:statement` guessed "the observation source was unavailable in the
`run-tick-once` process — likely a census the on-demand tick does not gather",
i.e. that punch-in ticks differ from full runs. They do not. Tallying
`[:observation-envelope :channels :sorry-count-norm]` over every persisted
trace day:

| trace file | records | `:sorry-count-norm` |
|---|---|---|
| `wm-trace-2026-07-21.edn` | 2 | no envelope key (pre-envelope schema) |
| `wm-trace-2026-08-30.edn` | 7 | no envelope key |
| `wm-trace-2026-08-31.edn` | 5 | 2 `:absent :source-field-missing`, 3 no key |
| `wm-trace-2026-09-01.edn` | 79 | 79 `:absent :source-field-missing` |
| `wm-trace-2026-09-02.edn` | 3 | 3 `:absent :source-field-missing` |

**84 of 84** records that carry an envelope at all report the channel absent
for this one reason — the 79 S1b/S2/S4 replay-corpus ticks of 2026-09-01
included, not just the three punch-in ticks the sweep happened to catch. The
sweep found a permanent gap, not a per-process one. (AC4's row already
recorded the 20/20 figure on the S1b subset; this is the same fact over the
whole persisted corpus.)

Why only three records were harvested: the harvester collects
`:prediction-triple-events`, which `trace.clj:547-548` persists **present-only**
and which AC1 added on 2026-09-02. The older ticks carry the same absence in
`:observation-envelope` and no event, so the event stream's count is a
function of when AC1 landed, not of how often the gap bites.

## The repair

The channel's source was never missing from the machine — only from the path
`observe` reads. `futon2.aif.sorry-registry/open-sorrys`
(`sorry_registry.clj:50-57`, reading `resources/sorrys.edn`) is the live
open-sorry registry, and `judge` already consults it every tick at
`war_machine.clj:5077` to build the `:address-sorry` candidate set. Both
readers of the channel name that population in words:
`observation.clj:27` ("open sorrys / 10 (capped at 1) — from sorry topology")
and `belief.clj:706` ("= open-sorrys / 10").

So the scan now counts it:

- `open-sorry-census` (`war_machine.clj:2926-2946`) returns
  `(count (sorry-registry/open-sorrys))`, or **nil** when that read throws.
  nil rather than 0: a registry nobody could read is not a registry with no
  open sorrys.
- `graph-summary` (`war_machine.clj:2948-2967`) assembles the summary and adds
  `:total-sorrys` **present-only** — a nil census omits the key, so
  `channel-statuses` keeps reporting `:absent :source-field-missing` with its
  paths instead of reading a fabricated zero. A registry that reads and holds
  no open sorrys is a measured 0 and lands in the map.
- `scan-graph` (`war_machine.clj:3020-3021`) calls both.

The pressure and the candidates now count one population. Before this change
rule 1 of `default-mode-select` (`policy.clj:342`) compared a sorry pressure
sourced from nowhere against a candidate list sourced from the registry.

## Measured, both directions

Live registry: 22 sorrys, statuses `{:addressed 12, :n-a-by-design 6, :open 3,
:acknowledged-v1-in-force 1}` → **3 open → 0.3**. On one scan summary with and
without the census, everything else identical:

| | census absent | census present |
|---|---|---|
| `observation-status` | `:absent :source-field-missing` | `:observed` |
| `:sorry-count-norm` | 0.0 (coerced) | 0.3 (read) |
| `per-channel` | `:status :absent`, no `:gap` | `:present :gap 0.0 :in-range? true` |
| `avoidance-by-channel` | `:unknown` | `:satisfied` |
| `score-support :pragmatic :absent` | 11 channels | 10 channels |
| `policy/sorry-pressure-record` | `:unknown`, no `:value` | `:present :value 0.3` |
| `channel-prediction-error` | `:absent`, no `:observed` | `:present :observed 0.3` |

What moves on a live tick, stated precisely:

- **`g-pragmatic` does not move today.** The preferred range is `[0.0 0.3]`
  (`preferences.clj:21`) and 0.3 sits at its top, so the gap is 0.0 and the
  weighted term (weight 0.10, `preferences.clj:58`) is 0.0. It **will** move
  the moment a fourth sorry opens: 0.4 → gap 0.1 → +0.01 to `g-pragmatic`.
  That is the channel doing its job, not a regression.
- **One `:avoidance-unknown` loss disappears**, because the avoided range
  `[0.8 1.0]` (`preferences.clj:32`) is now decidable and `:satisfied`.
  `:loss-count` on live ticks drops by one.
- **The belief update gains a channel.** `:sorry-count-norm` is in
  `belief/channels-with-likelihood` (`belief.clj:933`) with a likelihood model
  at `belief.clj:705`, so its triple now enters `raw-errors`, the precision
  state and the R3d driver at `war_machine.clj:5217-5221` instead of being
  omitted. This is the substance of the repair: a declared channel with a
  likelihood model was contributing nothing to inference.
- **AC4's fallback selector stops abstaining for this reason** — but does not
  change its choice. 0.3 fails rule 1's strict `> 0.3`
  (`policy.clj:342`), so the fallback still falls through to rule 3, which is
  where it went under the pre-AC4 substituted zero as well. The repair changes
  what is *known*, not what is *chosen*, at the current registry contents.

## Not done here, and why

- **`:nodes :sorrys` is left empty.** `sorry-nodes` is still the superseded
  stub, so `war_machine_visual.clj:1421` still renders 0 sorrys. Populating it
  places new nodes in the rendered graph — a different acceptance. The
  divergence (summary says 3, node list says 0) is named in `graph-summary`'s
  docstring so the two are not read as one population.
- **The registry is read twice per tick** — once in `scan-graph`, once at
  `war_machine.clj:5077` for candidates. Folding them into one read means
  restructuring `judge`'s state assembly, which is outside this acceptance.
- **`sorrys.edn` stays hand-curated.** `sorry_registry.clj:20-24` says so
  itself. This row wires the existing v1 substrate to the observation path; it
  does not make the census ingested.
- **No ruling and no registry write.** `aif-equations.edn` and
  `control-map-edges.edn` are untouched; `:covers-key :none`. Replay and tests
  only — no live run, no run lock taken, nothing written under `data/`.
- **Figure not regenerated** (TN §9a).
