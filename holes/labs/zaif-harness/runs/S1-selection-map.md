# S1 — the mission-selection path, mapped (facts with pointers)

claude-1, 2026-09-02. Row S1 of `../worklist.edn`. Every stage below carries a
pointer and, where marked PROBE, a command I ran today with its result. Facts
only; repairs and registrations belong to S2/S3.

## Stage 0 — whether any of this runs unattended: NO

`futon0/data/cron-jobs.edn:45-67`: `:wm-scheduled` (the hourly full loop,
`scripts/wm_full_loop_cron.sh`) is `:status :disabled`, operator-disabled at
the 2026-07-15 stop-line; the entry itself records the technical re-enable
gate as CLEAR ("choose bounded duree or a wall-clock cadence explicitly" —
an operator act, deliberately). `:wm-outer-loop` (daily 04:00) is the R12
Beta-hyperparameter loop — NOT the selector (confirming the worklist's
suspicion about `scripts/wm_outer_loop.clj`; its ns docstring says R12
narrow take-up). PROBE: `crontab -l` shows no wm entries installed. Recent
traces (`data/wm-trace/`: 08-30, 08-31, 09-01) are staged/manual runs
(RUN8/RUN9 S2–S4), not cron.

## Stage 1 — candidate enumeration: a file scan with a strict contract

`src/futon2/aif/mission_registry.clj:193-220` `load-missions`: candidates are
exactly `<code-root>/<repo>/holes/missions/M-*.md` (the regex at `:29`),
nothing nested, nothing elsewhere. `open-missions` (`:266`) filters by
`live-mission?` (`:259`): status-class not in `#{:complete :inactive :draft}`.
`src/futon2/aif/actuator_a6.clj:141-144` mints one `{:type :open-mission
:target <id>}` per live mission (`:advance-mission` is the sibling type,
`war_machine.clj:1246-1248`).

**Consequence for this lab**: `M-zaif-harness.md` and `M-zaif-harness-v1.md`
both live in `futon2/holes/`, not `futon2/holes/missions/` — NEITHER IS A
CANDIDATE TODAY. The declared registration channel S3 needs is exactly this
contract: the doc must sit at the scanned path with a live status. (The 09-01
trace's selected mission lives at `futon2/holes/missions/M-aif-policy-
conditioned-eig.md` — the contract observed working.)

A second, parallel index: `war_machine.clj:1286-1310` `mission-doc-index`
reads `code/v05/mission-doc` HYPEREDGES from the substrate for endpoints and
parses `**Gate:** operator-… — text` lines out of the source file as operator
gates. So: the FILE SCAN decides candidacy; the SUBSTRATE INDEX enriches with
endpoint + gates. Two registrations, two places.

## Stage 2 — weighting: the three-factor mission value (step ⑬)

`war_machine.clj:1553-1600` `enrich-candidates-with-mission-value`, "all
substrate reads happen here, at the judge boundary": blends

- **centrality** — normalized from `futon7/holes/M-futon-forward-model.centrality.json` (`:510-511`, cached `:1039-1050`),
- **strategic fit** — cascade role from the strategy cascade file (`FUTON_WM_STRATEGY_CASCADE` overridable),
- **phase doability** — `phase-doability` table `:1405-1415` (head 0.1 …
  instantiate 1.0, document 0.4, complete 0.0, unknown 0.3), reading the
  mission's lifecycle phase,

then completion/operator gates and repeated-non-progress decay
(`previous-selection-non-progress?` `:1417`). Weights overridable via
`FUTON_WM_VALUE_WEIGHTS` (EDN map) — a declared input, on the record.
Also present, separate: `roi-map-for-missions` (`:1087-1101`, name-matching
against an ROI feature map) feeding the report/rollout surface (`:1116-1118`),
and an anamnesis tiebreak (`:1120`).

## Stage 3 — scoring and selection (steps ⑭–⑰)

G_efe = risk + ambiguity per candidate (`efe.clj:808` rank-actions →
`core_efe.clj:94`), plus the named engineering augmentation → controller-score
(⑮); selection at the policy seam with ln E and τ_eff (⑯,
`policy.clj:234-271` strategic-recommendation — chosen is
`(or (first controller-entries) (first ranked-actions))`); abstention when
nothing beats no-op (⑰); decision explanation with per-term contributions (⑱).
The registry's C3/C5 findings apply here verbatim (habit computed, not
applied; first-non-no-op of the G-ordered list).

## Stage 4 — PROBE: the machinery, observed end-to-end on the last real run

`data/wm-trace/wm-trace-2026-09-01.edn` (bb, edn read, last record): decision
`:advance-mission` → `M-aif-policy-conditioned-eig`, `:controller-score
2.9607`, and the whole weight vector visible on the action record:
`:mission-value-factor 0.09`, `:doable 0.3`, `:central 0.0`, `:strategic 0.0`,
`:completion-gate-factor 1.0`, `:operator-gate-factor 1.0`,
`:non-progress-decay 1.0`, rationale "mission substrate: … [identify; advance
open holes]". So: enumerate → weight → score → select runs end to end when
invoked, and the record explains itself per term.

Oddity, recorded not diagnosed: the action carries `:phase nil` (→ doable 0.3
"unknown") while its own rationale text says "[identify …]" — the phase
reaches the rationale but not the typed field on this record. S2's manual
step-through should watch for this.

## Rotted vs running (the row's (d))

- RUNNING when invoked: the full selection chain (stage 4's probe).
- NOT RUNNING unattended: the hourly loop (operator-disabled; gate recorded
  clear) — restart is Joe's explicit act, deliberately.
- ODDITIES: `:phase nil` above; the 09-01 `wm-scheduled.log` tail ends with
  "No construction for selected decision, :target :fire-pattern" — the run
  selected, then found nothing constructed to fire; and `candidate count: 0`
  in `:ranked-actions` on the same record that carries a chosen action (the
  ranked list is elsewhere or elided — S2 to locate it).

## What S2 inherits

Enumerate (stage 1, by hand against the real scan contract) → weight (stage 2,
compute the three factors for each candidate, showing work) → score/select
(stage 3) → compare with what a machine invocation says. And the S3 lever is
now precise: put the mission doc at `futon2/holes/missions/`, live status,
mission-doc hyperedge registered, phase stated where the parser reads it —
then the weights are moved only by declared inputs (phase, gates,
FUTON_WM_VALUE_WEIGHTS on the record, or Joe's mark once S1's successor wires
the declared-mark channel into a weight input).
