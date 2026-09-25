# MAP-WIRING-D — what exists that M-wm-wiring will use to wire its components

claude-11, 2026-09-25, on claude-8's requisition. This is the MAP phase's
discovery for futon3c `holes/missions/M-wm-wiring.md` (`a9eb3ee7`; owner
claude-10). It answers `futon4/holes/mission-lifecycle.md` §2 MAP (`:95-114`,
`4139c74`). Read-only: nothing built or run, no diagram drawn, nothing
written under `data/`.

Heads read: futon2 `9681d434`, futon3c `1f592a06`, futon3 library (shas per
file), futon5 `17c2eb2`, futon4 `4139c74`.

**Answers up front.**
- **The lifecycle already names a checker for this.** It is futon3c
  `futon3c.diagramprover.wiring`, listed at `mission-lifecycle.md:375-392`. It
  takes an ownership map: boxes, each with a source site, the fields it reads
  and the fields it writes. It reports `written-never-read`,
  `read-never-written`, `multiply-written`, and **conformance** between the
  declared reads and writes and the source file each box names. A worked map
  of a futon2 War Machine path already exists
  (`holes/labs/M-diagramprover/wm-wiring.edn`). That is the form recommended
  in Q4.
- **futon5 has one useful piece.** Most of its "wiring" is cellular-automaton
  rule wiring, not wiring software components. The exception is
  `src/futon5/ct/mission.clj` (`aeeee96`): a port and component diagram with
  validators, including the I4 exogeneity check.
- **M-futon-seams' token-flow form diagrams a target's cascade.** It shows the
  work the flight is about, not the machine that flies it.
- **The two components this mission names that have no caller at HEAD** are
  `grain_gate.clj` and `enactment_habit.clj`. Row 0 (the enactment writer)
  does not exist.

## Q1. What already exists that the mission will use

| source | meaning of "wiring" | gives | live or source | the one thing to reuse |
|---|---|---|---|---|
| futon3c `src/futon3c/diagramprover/wiring.clj` (`c474470f`, 193 l.; test `test/futon3c/diagramprover/wiring_test.clj`, `7635eb83`) | components writing fields that other components read | **a checker** over EDN ownership maps: `ingest` `:13`, `written-never-read` `:36`, `read-never-written` `:54`, `multiply-written` `:72`, `conformance` `:107`, `phase-chain-findings` `:161` | live code, callable in its own process; warrant not looked up (typed: `{:absent :warrant-not-checked}`) | `conformance`: "Compare declared box reads/writes with occurrences in their named sites." An unreadable site is a finding, not an exception (`:118-119`) |
| futon3c `holes/labs/M-diagramprover/wm-wiring.edn` (`15a51bee`, 38 l.) + dated findings | same | a worked map of a futon2 War Machine path: `belief.clj` → `war_machine.clj` → a live-wiring test | record | the box shape `{:box/id :site {:ns …}\|{:file …} :reads [...] :writes [...]}` (`:26-38`); provenance stamps are written mostly for later audit, and a test is declared as their reader (`:14-18`) |
| futon4 `mission-lifecycle.md` BOM "process" row and wiring rule (`:348-424`) | same | the rule for when a diagram is required; the grading of which checkers exist | rule | "a BOM row may only name a level at which a verifier actually exists" (`:367-368`) |
| futon5 `src/futon5/ct/mission.clj` (`aeeee96`, 681 l.; tests in `test/futon5/ct/`) | mission architecture: ports, components, typed edges | **validators**: completeness `:165`, coverage (no dead components) `:183`, no orphan inputs `:201`, type safety `:248`, I3 timescale `:311`, I4 exogeneity `:350`, closure `:386`, `validate` `:419`; composition `:486-554`; mermaid rendering `:626` | live code; futon5 has no CLAUDE.md, so the "source material" flag the requisition recalls is not there (the phrase is at futon3c `CLAUDE.md:193` and is about futon3) | **I4**: "If you can trace a directed path from any Action node to any Preference node that does not pass through the Environment, the diagram has a wireheading vulnerability" (`docs/chapter0-aif-as-wiring-diagram.md:207-209`, `84167e6`) |
| futon5 `data/missions/*-exotype.edn` (e.g. `coordination-exotype.edn`, `c3cffce`, 330 l.) | abstract architectures | a diagram format `{:ports {:input :output} :components :edges}`; a concrete mission "is valid iff it projects onto this diagram" (`:3-6`) | data | `futon2-aif-ants.edn` in the same directory is an earlier futon2 diagram in this form |
| futon5 `docs/ct-wiring-vocabulary.md`, `wiring-design-principles-from-good-bands.md`, `Mission-1-fulab-wiring.md`, `notebook-eoc-detection-and-wiring-design.md`, `experiment-proposal-wiring-param-isolation.md`, `holes/missions/M-fulab-wiring-survey.md`, `data/wiring-rules/`, `data/experiment-wiring-*.edn`, `scripts/cyberant_wiring_compare.clj`, `scripts/hexagram_wiring_pass_workup.clj` | **cellular automata** (rules built from bit primitives) | nothing about wiring components, except as analogy | CA infrastructure | `data/wiring-ladder/README.md:5-10` (`62a6b9b`): "Add one component at a time… Test at each level… Record what each addition contributes." This matches the mission's criterion 3, one row per step |
| futon3c `holes/labs/M-futon-seams/wiring/instance-{4,4b,5,6,7}-wiring.edn` (`7635eb83`); `scripts/wiring_from_cascade.py`, `scripts/wiring_check.py` (`4e5a1350`, 130 l.) | a target's cascade tokens | **token-flow form**: nodes are patterns, ports are `:in`/`:out` tokens, and satiety comes from the enacted outcome (`instance-4-wiring.edn:1-12`); a checker that every edge carries a token its ends have, and that the recorded dangling outputs equal what the ports imply (`wiring_check.py:6-17`) | scripts, run by hand | "the recorded dangling outputs and unfed wants are exactly the ones the ports imply, so the findings cannot drift from the picture" (`:16-17`) |
| futon3 `library/process/built-but-not-wired-invisibility.flexiarg` (`59a31a5`) | components | pattern | library | "keep an inventory of unwired components so invisibility is a listed fact, not an archaeology result" (`:24`); "no per-component test can catch it" (`:27`) |
| `library/test-registry/warrant-only-the-wires.flexiarg` (`21a9199`) | components | pattern | library | "Field contracts between endpoints … are wires too: one contract artifact, its sha pinned by both endpoint warrants" (`:10`); the recorded case is a producer writing `:g-term-decomposition` while its consumer read `:g-terms` (`:28`) |
| `library/workshop/honest-holes-gate-composed-claims.flexiarg` (`4a7f07c`) | composed claims | pattern | library | the one required record is `{:checked-dimensions [...] :holes-found 0}`, scoped to the composition's own dimensions (`:33-39`) |
| `library/workshop/COMPOSITOR.md` (`7c4cc87`) | composition | design note | library | C3 "build the degenerate case first" (`:44-48`); C6 "Watchers watch, gates gate" (`:61`) |
| `library/process/detection-wired-to-single-catastrophic-response.flexiarg` (`59a31a5`) | detection → response | pattern | library | "A single maximal response makes the cost of reporting a fault higher than the cost of hiding it" (`:26`). This is E-outer-loop O3's shape |
| `library/apparatus/` (14 patterns) | apparatus | patterns | library | `done-is-observed-running` (a component exists when the live system shows it acting) and `one-authority-per-question` bear on rows 6-7 (E read from the legacy store) |
| futon2 `holes/E-outer-loop.md` (`aedcc6ae`) | the old loop | a record of what was wired and O1-O8 | record | O3 (`:86`): feasibility turned from support into a refusal, and the tick abstained when everything was refused |
| futon2 `holes/labs/wm-contract/NOTE-task-manager-outer-loop.md` (`8bd9fce5`) | tick → lane | mechanism | record | stageable = fresh ∧ owned ∧ no live conflict ∧ prerequisites done (`:19-24`); staging is clocking, i.e. M-autoclock-in (`:25-27`) |
| futon2 `holes/missions/M-G-wm-wiring.md` (`d9b906f3`, HEAD) | the WM checklist | stage plan | record, never worked | every stage produces "a checkable artifact, not accumulated machinery" (`:20`); CP0-CP6 all unticked (`:77-84`) |
| futon5a `holes/missions/M-war-machine-wiring.md` (`78d98fd`, VERIFY 2026-05-01) | the WM "last mile" | three contracts (manifold-read, head-write, trace-emit; `:68-87`) | record | "Both endpoints now exist; the connector doesn't" (`:39-55`); INSTANTIATE deferred (`:1846-1848`). The prior round stopped at the same gap |

## Q2. What data the mission holds

- **Inventory rows 0-11** with component shas and warrants: M-wm-wiring
  `:21-34`. Every row except 0 names a sha. Rows 1, 2, 5, 6, 7, 8 and 11 name
  a warrant. Rows 3, 4, 9 and 10 name none in the row (typed:
  `{:absent :warrant-not-named-in-row}`; not searched in the registry).
- **The record shape** (click-001, futon3c
  `holes/labs/M-futon-seams/exemplar/`, `4bc95005`):
  - `click-001.edn` top keys: `:schema :computed-by :clause-status :mission
    :authored-at :click :authored-by :offset-unit :decision`;
  - `:decision` has `:target-field`, `:selection-law`, `:selection-certificate`;
  - `:target-field` has `:considered :feasible :exclusions :cost-ordering
    :score-inputs :score-inputs-finding :chosen`;
  - `:selection-law` has only `:target :note`.
  - `click-001-enactment.edn` has `:schema :observations :candidate
    :conformance :observation-model :attempts :click :grain :theta-prior`.
    Its 8 attempts each have `:n :pattern :commit :produced :success
    :evidence`. `:grain` has `:keyed-by :statement :evidence :checked-by`.
  - **Both files are hand-authored.** No code writes either shape.
- **The target field**: fixture `7bd17dfb` (343 feasible, next steps 331
  `:read-criteria` and 12 `:ask-interpretation`), producer `target_field.clj`
  (`3ecf7a16`), which now carries `:eligible` (`:210`).
- **OUTER-CASCADE-D** (`f36820a4`, 250 l.): the step-kind patterns with guards
  and produces (`:59-78`); red tape as a cost term with a proposed field and
  five cases (`:80-89`); construction cost ~20 s (`§6`).
- **The first target**: futon3c `holes/missions/M-autoclock-in.md`
  (`27f735b4`, Status INSTANTIATE-1); its field entry's next step is
  `:read-criteria`.

## Q3. Rows ready to wire, and what each is missing

The flight loop is `flight/run!` (`flight.clj:246-297`, `c523a6dd`). Its
steps are `read-fn` → `click-wants` → `observe-fn` → `ask-fn` → `click-fn` →
`observe-fn` → `record-click`. Its only production caller is
`flight_driver.clj:123-135` (`c523a6dd`; `--run` only). The click is a POST to
`/api/alpha/wm/click` (`flight_runner.clj:255`), so rows 4, 6 and 9 happen
inside the tick in `scripts/futon2/report/war_machine.clj` (`3aa8c479`), not
in the flight.

| # | component at HEAD | call site | field on click-001's shape | status |
|---|---|---|---|---|
| 0 | none (`machine_enactment_correspondence.clj` is a post-event verifier, not a writer) | none: `run!` has no enactment step | enactment file exists by hand; no code writes it | **missing: the component itself** |
| 1 | Lean `TargetGrainG`; field producer `target_field.clj` | `flight/choose-target` (`flight.clj:151-162`): open stop-line, else `:requested`; nothing reads the field to choose | `[:decision :target-field :chosen]` exists by hand | missing: the outer-cascade action set and the mixture draw (no source file found) |
| 2 | `scripts/wm/extract-outcomes.clj` (`1a98d0dc`, no `ns` form); `served_by_reading.clj` **absent** | none: `flight.clj` never reads outcomes | no `:outcomes` key in click-001 | missing: the `ns` (row 2b) and the quote → span join (2a) |
| 3 | `wi/prompt`, `wi/validate-response` | `flight_runner.clj:91`, `:147`, `:161` | `:asks` on the flight record | partly ready: no library root passed (no `library` in `flight_runner.clj`); `:retrieval` handled in `want_interpretation.clj:89-99, 161, 267` |
| 4 | constructor `:order` (`interpretation_construction.clj:36-55`) | kernel reads `:precedence` (`efe.clj:1163`, `war_machine.clj:4621`); nothing reads `:order` | `:selection-certificate :candidate-derivations` | missing: the read |
| 5 | `grain_gate.clj` (`4b69242e`) | **no caller in `src/` or `scripts/`** (grep) | `:grain` on the enactment | missing: the call; depends on row 0 |
| 6 | `observation_rates.clj` (`64f005d4`) | `war_machine.clj:5889`: `(sourced-rates nil nil nil locators …)` | no `:measurement` anywhere in `war_machine.clj` | missing: labels and the field |
| 7 | `enactment_habit.clj` (`531cfaaa`) | **no caller** (grep); selection reads `:cascade-habit-path` (`war_machine.clj:6629`) | none | missing: the call and the read; depends on row 0 |
| 8 | `:universe` honoured (a98f5879) | `target_field.clj` has no overlap or `:g` computation (grep) | none | missing |
| 9 | checker reads `:join-unverifiable` (futon3c 4bc95005) | `:selection-law` carries `:applied` and `:per-policy-argmax` (`war_machine.clj:4619`, `:6650`); no candidate-id key found | click-001 `:selection-law` = `{:target :note}` | missing: the id |
| 10 | observation exists at flight time | none in `flight*.clj` | none | missing |
| 11 | warrants check (futon3c 4e66c56c) | n/a | n/a | done |

**Ready vs missing** (the lifecycle's two columns):
- **ready (no new code):** row 11; row 3's prompt and validation path; the
  field producer (row 1's input); the flight loop and its read and ask steps.
- **missing (the work):**
  - row 0, the one new component;
  - calls from the flight for rows 5 and 7, both after row 0;
  - reads in the tick for rows 4, 6 and 9;
  - row 1's action set and draw;
  - row 2's `ns` and join;
  - row 8's overlap;
  - row 10's tick.

## Q4. Which diagram form to draw

**Recommendation: the diagramprover ownership map** (Q1, row 1), checked by
`futon3c.diagramprover.wiring`. Reasons, from the lifecycle's own selection
principle ("prefer the level at which the check can consume the live
artifact", `mission-lifecycle.md:360-363`):
- **Conformance reads the live source.** A box that declares it reads
  `:measurement` from `war_machine.clj` gets a finding while
  `war_machine.clj` does not contain it. The token-flow and ct/mission forms
  are checked only against themselves.
- **Its unit is the mission's unit.** Rows are components that write fields
  of one record that other components read. That is boxes and fields, which is
  trigger 2 of `:405`.
- **It has already run on futon2.** `wm-wiring.edn` works on the War Machine
  path, across repos, with no phase chain (`:19-20`).

The token-flow form stays in use for the target's own cascade: M-autoclock-in's
wants, as its tokens. It is not the machine's diagram. From futon5, borrow
the **I4 check** as a question to put to the map: no path from a box the tick
writes to the preference fields (C, `:want`) that does not pass through an
observation. The "no outer-loop self-caused lineage" question left open in
tension 10 is an instance of this. ct/mission.clj's completeness and coverage
checks overlap with `read-never-written` and `written-never-read`.

**For a flight of M-autoclock-in, the map would have:**
- **boxes** (the components, each with its site):
  - `target-field` (`target_field.clj`), `choose-target` (`flight.clj`);
  - `read-step` (`flight_runner.clj`, read-fn), `outcome-extractor` (row 2),
    `ask-step` (`flight_runner.clj`, ask-fn), `observe` (`flight_runner.clj`);
  - `tick-R5` (`war_machine.clj`: rates, kernel order, selection law);
  - `enactment-writer` (row 0, no site yet), `grain-gate` (`grain_gate.clj`),
    `habit-fold` (`enactment_habit.clj`);
  - `record-click` (`flight.clj`), `publish-observe` (row 10);
  - the W-checkers (futon3c `proof2a_check.clj`) as declared readers.
- **fields** (the reads and writes): the click record's paths.
  - `[:decision :target-field]`, `[:decision :selection-law]`
  - `:measurement`, `:order`/`:precedence`, `:outcomes`
  - the enactment's `:attempts`, `:grain`, `:conformance`
  - E's store, `:universe`
- **wires**: implied by the shared fields. A field written by one box and read
  by another is a wire.

**What the no-orphan checks would catch today:**
- `read-never-written` on `:grain` and the enactment fields. Rows 5 and 7 read
  them, and row 0, their writer, has no site.
- Conformance findings for the declared reads the code does not make:
  - `tick-R5` reading `:measurement` and `:order`;
  - `selection-law` writing a candidate id;
  - `habit-fold`'s output read by selection. Selection reads
    `:cascade-habit-path` instead, the legacy store (tension 6).
- **The library pattern's own case.** `grain_gate.clj` and
  `enactment_habit.clj` are built and have no caller. They show up only if the
  map declares the intended caller (the flight) with its real site. A box left
  out of the map is invisible to every check. So the inventory table has to be
  the list of boxes (see Q5).

## Q5. What the method cannot yet check

- **A component absent from the map.** Every check runs over declared boxes.
  Keeping the map complete against the inventory is by hand. The checker is
  "detect-level … over authored, conformance-kept declarations"
  (`mission-lifecycle.md:389-392`).
- **Conformance is textual.** `field-occurs?` (`wiring.clj:101`) finds a
  field's name in the site's text. A field mentioned in a comment passes. A
  field built from a variable fails.
- **Red tape as a measured quantity.** OUTER-CASCADE-D's
  `{:ask :followed-by :step-kind :beyond-ask? :basis}` field is proposed, not
  built (`:80-89`). No rate exists (`{:absent :no-red-tape-rate}`).
- **Criterion 4a, the refusals a flight can reach.** No enumerator exists.
  Typed refusals are raised by `ex-info` across `war_machine.clj` (7,820
  lines), and nothing lists which ones are reachable from `/wm/click` for a
  given target.
- **The outer cascade's rates before any flight.** Q(o | step kind) is typed
  absent until `measured-cell` has counts (row 1). The first flights produce
  the first counts.
- **Row 1's selection at target grain.** G is undefined on all targets, and
  the mixture law's no-data branch is E-only (H-G-TARGET-PRIOR-D §4). The
  check can confirm the draw is on the record. It cannot confirm that the
  choice is good.
- **Cross-process fields.** The flight and the tick share only the POSTed
  flight EDN and the run record read back (`flight_runner.clj:255-270`). A map
  can declare this seam, but conformance checks one site text per box, not
  the HTTP payload.

## Surprises (recorded before DERIVE)

1. **The verifier the lifecycle names is not in the mission's source list.**
   Its sources point at futon5 and M-futon-seams. The lifecycle's process-row
   checker, and a worked War Machine map, already exist in futon3c.
2. **futon5's "wiring" corpus is mostly cellular automata.** One file of
   validators applies to components.
3. **Rows 5 and 7 have no caller anywhere at HEAD.** Row 0, which both depend
   on, has no code, and click-001's enactment and decision files, the target
   shape, are hand-authored.
4. **futon5a M-war-machine-wiring (VERIFY, 2026-05-01) stopped at the same gap
   in an earlier round**: "Both endpoints now exist; the connector doesn't."
   futon2 M-G-wm-wiring (HEAD) was never worked. DERIVE should read both, for
   what they deferred.

## Wiring-diagram triggers (`mission-lifecycle.md:398-424`)

All four hold for M-wm-wiring:
1. **A machine.** The flight loop and the tick.
2. **More than one party writes fields another reads.** For example,
   `war_machine.clj` writes the run record and `flight.clj` reads it; the
   read, ask and interpretation steps publish and the click reads what they
   publish.
3. **Fields cross process, repo and language boundaries.** The flight → HTTP
   → the serving JVM (`flight_runner.clj:255`). futon2 records are checked by
   futon3c checkers. The clauses are stated in mathlib4 Lean.
4. **The claims rest on evidence the built thing produces.** Completion
   criterion 1 is a click record the flight writes.

So by the lifecycle a diagram is required, not optional. The lifecycle's cited
evidence (`:418-423`): M-apm-demonstration "scored 4-for-4 … carried 'futon5
wiring diagram — not yet drawn / not blocking' across five checkpoints, and
accumulated sixteen instances" of the no-orphan defect class. The "not yet
drawn" line is at futon3c `M-apm-demonstration.md:1536, 1567`, and "not
blocking" is at `:1609`. The count of sixteen is the lifecycle's own claim,
citing `retrieval-whitepaper-v3.md` §7a; I did not recount it.
