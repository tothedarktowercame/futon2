# U11a reading map — actand candidates and replay data

Discovery only, 2026-09-03. No source or live-JVM state was changed. Paths
below are relative to `/home/joe/code` unless stated otherwise.

## A. Portfolio pragmatic value (arm grain)

`futon3c/src/futon3c/portfolio/policy.clj:49-74` defines
`pragmatic-value(action, observation, mu-sens, adjacent-missions)`. In the
cited range it computes one scalar expected-progress heuristic for each of
`:work-on`, `:review`, and `:consolidate`: `:work-on` combines absolute
gap/stall prediction errors, adjacent-mission count, and observed gap count
(`:54-63`); `:review` combines review-age prediction error and observation
(`:64-68`); `:consolidate` combines spinoff-pressure prediction error,
observed pressure, and inverse coverage (`:69-74`). The remaining cases
continue below the U11 citation (`:75-89`).

Inputs are the action keyword; observation and `mu-sens` maps containing
`:gap-count`, `:stall-count`, `:review-age`, `:spinoff-pressure`, and
`:coverage-pct`; and adjacent mission maps whose `:adjacent?` flags are
counted (`:52-74`). Output is a number, not a density or evidence record.
`expected-free-energy` calls it at `:155-180`, where the scalar becomes the
`:terms :pragmatic` member of an action/G record. Its grain is one portfolio
action evaluated against a portfolio observation/belief snapshot (the U11
adapter calls this `:arm-session`), not an entire mission course.

Replay consequence: none of the 114 calibration rows or 56 decision
snapshots contains these five channels or `mu-sens`/adjacent missions. The
mission tick R2 fixtures contain 14 differently named WM observation channels
(for example `:mission-health` and `:support-coverage`), while R1/R3 contain
the matching WM belief maps; those records can replay the *shape* of an
observation-versus-belief computation but cannot call arm A verbatim without
a declared channel mapping. Sample live-derived fixture pin: run id
`0a18c4f7-758e-400a-8223-9c52edf07450`, whose tracked R2 fixture records
`:mission-health 0.023376623376623377` and `:support-coverage 0.6` at
`futon2/holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures/0a18c4f7-R2.edn`.
For the calibration half, the exhaustive command
`bb -e '(let [xs (clojure.edn/read-string (slurp "holes/labs/M-zaif-harness/calibration-sessions.edn")) ks #{:gap-count :stall-count :review-age :spinoff-pressure :coverage-pct :mu-sens :adjacent-missions}] (prn {:rows (count xs) :hits (count (filter #(some % ks) xs))}))'`
printed `{:rows 114, :hits 0}`; its one-map output was not truncated. For the
56-decision half, the bounded search and complete result counts are stated in
the inventory below.

## B. Mission-head pragmatic value (mission-action grain)

`futon3c/src/futon3c/aif/mission_head.clj:135-144` defines
`pragmatic-value(action, channels)`. It returns a scalar from
`:phase-progress`, `:prediction-divergence`, `:gate-readiness`, or
`:obligation-satisfaction`, with a constant for `:save-state`, according to
the six mission actions. `epistemic-value` is an action lookup at `:146-155`;
`effort-cost` is another action lookup at `:157-166`; `compute-g` combines the
three under lambdas at `:168-176`. Inputs are one mission-action keyword, the
four-channel map, and (for G) the lambdas. Outputs are a pragmatic scalar and,
from `compute-g`, a scalar G. `select-mission-action` exposes the public
record at `:184-210`: action, policies containing G/three terms/probability,
tau, and abstention. The grain is an action within one mission head.

Replay consequence: the calibration and decision records do not carry the
four mission-head channels. The three WM tick R2 fixtures also use a distinct
14-channel vocabulary, so they do not replay this function verbatim. Their
S4 fixtures do supply mission identity/clock lineage; R2 supplies measured WM
channels; R1/R3 supply beliefs. A future adapter therefore has real
mission-grain inputs to map, but the mapping must be declared rather than
renaming `:mission-health` into one of these inputs. Sample pin: run id
`801976e7-01c6-4e39-aada-27f620f7c2f1` records mission id
`M-zaif-harness-v1`, endpoint `futon2-d/mission/zaif-harness-v1`, and witness
rule `selection-decision` in the tracked `801976e7-S4.edn` fixture.

## C. Cascade-catalog playout record (mission grain; not built)

There is no arm-C reader or catalog record to replay. The S6 row says exactly
what the proposed query would assemble at
`futon2/holes/labs/zaif-harness/worklist.edn:83-85`; its port is split into a
reader and action at `futon2/holes/labs/wm-contract/worklist.edn:819-824`.
The design receipt at
`futon4/holes/mission-lifecycle-wm-alignment.md:143-189` specifies the
accreting inputs: PSR/PUR pattern-use records, flight discharges, each tick's
trace decision and `:shown` pattern list, and clocked-on lineage (`:153-161`).
It proposes kin lookup by shared patterns/repos/cross-references (`:162-166`)
and an empirical distribution of outcomes for similar cascades as the
mission-grain Q(o|pi) playout (`:167-173`). It records current carrier facts,
not a completed catalog: PSRs are sparse, `:shown` stays per-tick and
unaggregated, APM histories are rich but local to that domain, and the
phylogeny carrier has two co-application edges (`:174-189`). Output intended
by S6(a)/wm U23 is a typed, read-only mission cascade plus populated/absent
carrier report and kin missions; S6(b)/wm U24 would consume that at mission
grain. Until U23 exists, arm C has no replayable output.

Sample pin for one input carrier, not a claim that the catalog exists: tick
run `801976e7-01c6-4e39-aada-27f620f7c2f1` has the tracked S4 clock-lineage
fixture cited above and tracked decision fixture
`futon2/holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures/801976e7-R6.edn`.

## Replay-data inventory

### 114 calibration sessions

- Location/status: tracked files
  `futon2/holes/labs/M-zaif-harness/calibration-sessions.edn` and its JSON
  rendition. Each EDN row has `:id`, `:at`, `:context`, correction label,
  route, and gold judgment. It can replay the shipped ZAIF hydrator/controller
  inputs derived from context, but contains none of arm A/B's named channels.
- Enumeration command, run from `futon2`:
  `bb -e '(let [xs (clojure.edn/read-string (slurp "holes/labs/M-zaif-harness/calibration-sessions.edn"))] (prn (count xs)))'`
  printed `114`. Output was one integer and was not truncated.
- Verbatim sample record id: `e-0cae94f2-9ca8-4863-9251-44278445a5f7`.

### 56 live ZAIF decisions

- Location/status: campaign data in the durable evidence store exposed by
  `http://127.0.0.1:7073/api/alpha/evidence/text-search`; it is not a tracked
  repository corpus. The bounded snapshot and its query are recorded at
  `futon2/holes/labs/zaif-harness/runs/U8a-report-sources.md:54-79` and the
  task-belief field inventory at
  `futon2/holes/labs/zaif-harness/runs/D8a-task-belief-sources.md:41-72`.
  Bodies carry arm, G terms, gamma, round/pairing and inputs snapshot. All 56
  snapshots in that inventory had posting stats and empty task belief, so
  they replay current decisions but not arm A/B without new typed inputs.
- Enumeration command recorded by that discovery was a GET of
  `/api/alpha/evidence/text-search?tags=zaif&limit=100`, followed by selecting
  entries whose body event is `:zaif-arm-choice`; it produced
  `{:decisions 56, :nonempty-task-belief 0, :posting-stats-populated 56}`.
  The response had at most 100 entries and the endpoint returned the complete
  requested page; the command output was not truncated. This is a dated,
  bounded snapshot, not a claim that the growing store still contains only
  56 decisions.
- Verbatim sample record id: `e-0f2f9aec-6240-40e9-a25a-e45d9452076f`
  (session `zai-476ceb7567124811958ec8d6dabedd1c`).

### Mission-grain tick records

- Original campaign data/status: three untracked files at
  `futon2/holes/labs/wm-contract/tick-run-record-2026-09-02-{0a18c4f7-...,4abad68c-...,801976e7-...}.edn`.
  They carry full tick observations, mu-pre/mu-post, precision, ranking,
  decision, route, and active-mission evidence. They are not safe as an
  unguarded test dependency.
- Tracked replay material/status: 39 EDN fixtures (three run ids times 13
  nodes) plus README under
  `futon2/holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures/`.
  These are harvested fields or typed absences and are the portable replay
  source. R2 has 14 observations, R1/R3 each have 417-entry beliefs, and S4
  carries clocked mission; R16 is typed `:no-enactment-yet`.
- Enumeration commands, run from `futon2`:
  `find holes/labs/wm-contract -maxdepth 1 -type f -name 'tick-run-record-2026-09-02-*.edn' -print | sort`
  printed exactly the three originals; and
  `find holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures -maxdepth 1 -type f -name '*.edn' -print | sort | wc -l`
  printed `39`. Both outputs completed and were not truncated.
- Verbatim sample record/run id:
  `801976e7-01c6-4e39-aada-27f620f7c2f1` (the ZAIF mission clock pin above).

## Reading-map conclusion

Arms A and B are executable scalar heuristics, but no named replay corpus
already supplies their exact channel vocabularies. The tick fixtures provide
the richest real observation/belief/mission material, while calibration and
decision records provide the ZAIF session grain and current zero-act
baseline. U11 must therefore make channel mapping and missing-input refusal
part of each adapter; it cannot describe a vocabulary substitution as replay.
Arm C remains an explicit unbuilt arm whose proposed source is an accreted
query over existing carriers, not an authored prior.
