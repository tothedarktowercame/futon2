# WM click runner: producers of inputs the click requires

## Reviewed update — 2026-10-19, codex-1

P24 P11 (`5874be666` + `89e689716`): selected observation can produce one trace
at normal construction or early close within the same runner invocation. An early
row identifies actual phase/stage, without manufacturing construction completion.
Attempt state is cached even on failure; no retry loop or retired R8 scalar added.

P4 P02b: no defect in square need extraction was reproduced. With no outer
choice, generator target-local assembly has zero targets; its inner field cannot
nominate square work. A potential preparation request from the retained outer
field is a distinct producer, not evidence of a selected action or a P4 repair.

P11 P04 (`96bb168cb`): provider emits per-policy score receipts and measured
menu/scored/score-absent frontier, repinning its response after adding them.
Declaration contains only computed rows; all-unscorable produces an explicit
no-policy-scores declaration, not invented numeric authority. This producer
change alone does not establish selection continuation.

P30 update (P01, `ac4ca5bab` + `72352574b`): selected-action completion with
absent prospective carry emits `:status :unavailable`,
`:kind :prospective-carry-unavailable` and the absent receipt. It does not invoke
predecessor claim production on that path. Present-carry completion continues
through the existing verifier with compatible ID/universe context keys.

P4 clarification from P32 (`71c83c4e8`): occurrence-square authoring at `3352dc1b`
accepts a first missing-square family failure without a chosen action. The static
model represents first-family availability, not all-target coverage. Whether the
pre-choice route delivers that family carrier remains a separate interface check.

P24 update (P00, `b4359d675`, reviewed worker `6fc4fadef`): written run records emit existing abstained frontier and loop-node measurements from the sorry-cell certificate even without Q2 construction. No default counts or selected action are manufactured. Worker regression passed for both Q2 cases; absent upstream data remains absent.

The original audit below remains a historical reading at its stated revision.

futon2 commit read: `1de600e2c` (HEAD when written; reading began at `f76041cc7`, and the only
change between them is one line in `src/futon2/aif/wm/family_selection.clj` plus a test).
Date: 2026-10-19. Method: static reading (grep/sed) only; nothing was executed.
Scope: `src/futon2/aif/**`, `scripts/wm_click.sh`, `scripts/wm_click_repair_loop.py`. Three files
outside that scope are cited where the producer lives there:
`scripts/futon2/report/war_machine.clj` (the selection judge), `futon3c/src/futon3c/wm/*` and
`futon3c/scripts/mined_pattern_graph.py`.

Abbreviations. `runner` = `src/futon2/aif/full_loop_runner.clj`; `runtime` =
`src/futon2/aif/full_loop_runtime.clj`; `wm.clj` = `scripts/futon2/report/war_machine.clj`;
`mlos` = `src/futon2/aif/meta_live_outer_selector.clj`; `fsel` = `src/futon2/aif/wm/family_selection.clj`.

## Click timeline used for the "when" column

Outside the runner: `wm_click.sh --run` (reads budget at line 68-73, fires `POST /api/alpha/wm/click`
at line 326). Inside `runner/run-opportunity!` (runner:7839): `previous-run/start!` (7858),
`discharge-receipt/catch-up!` (7869), then `run-opportunity-core!` (5242), whose phases are, in order:
:agent-readiness (5279), :code-state (5286), :ration-admission (6266), :substrate-preflight (6269),
:preference-refresh (6275), :stop-line-memory (6292), :selection (6374),
:selected-target-reading (6421) with :selected-target-reading-redecision, :occurrence-square-authoring
(6457) with :occurrence-square-redecision, interpretation ask (6526) and :selection-redecision (6549),
then construction/dispatch/build/review/close, then after the core returns: `persist-run-record!` (8003),
:meta-injury-deposit (8011), :meta-terminal-injury-ingest (8022), :outer-learning-publication (8035).
Before any of that, `runtime/run-opportunity!` (runtime:401) calls `runtime/production-defaults`
(runtime:226), which builds several inputs once per click (rows P12, P13, P14, P15).

## 1. Producers

Row ids are P1.. so the other aspects can cite them.

| id | input (as the consumer names it) | producing function | file:line | when it runs | where it lands | all targets or selected only? | notes |
|---|---|---|---|---|---|---|---|
| P1 | target reading (`reading-registry/current-reading`; failure kinds `:no-current-target-reading`, `:stale-target-reading`) written before any outer choice | `reading-preparation/prepare!` -> `selected-target-reading/repair-need!` -> `registry/publish!` | `target_reading_preparation.clj:18`; `selected_target_reading.clj:304` (call to publish at `:493`); `target_reading_registry.clj:135` | in a click, phase :selected-target-reading (runner:6421), only when the first :selection returned a pre-choice field (`prechoice-field`, `selected_target_reading.clj:200`: reason `:incomplete-policy-menu-coverage` or `:no-canonically-bound-policy`, no chosen target). Wired as `:selected-target-reading-fn` at `runtime:307`, `prepare!` call at `runtime:318`; step wrapper `runner/selected-target-reading-step` (runner:2132) | `/home/joe/code/storage/wm-target-readings/targets/<sha256 of target-id>/<excerpt-digest>.edn` (`target_reading_registry.clj:15`, `record-file`, atomic write `:97`). Request files: `/home/joe/code/storage/wm-target-reading-requests/<sha256>/request.json` (`selected_target_reading.clj:14,292`). The step also puts a `:selected-target-reading-request` record on the judgement and selection certificate (runner:2132) | only the targets that appear in `prechoice-needs` (`selected_target_reading.clj:210`), and at most `(min 3 (max 1 (count needs)))` per click with a 30-minute deadline (`runtime:318-321`). Not all eligible targets | Needs the designated author seat (Agency) to answer; `designated-agent-answer-fn` returns `:not-contacted` when the seat is not idle/restored (`runtime:57-77`). `prepare!` stops at the first refused need (`target_reading_preparation.clj:31-34`). After publication the runner re-runs the judge once (redecision). Order: the producer runs AFTER the first outer-choice attempt, so the first attempt always sees the pre-click store |
| P2 | target reading for the already-chosen target | `selected-target-reading/repair!` -> `repair-need!` | `selected_target_reading.clj:516` | same phase, same wiring, taken when `prechoice-field` is nil (`runtime:325-327`) | same store as P1 | selected target only | `selected-need` (`selected_target_reading.clj:222`) refuses to use another target's failure |
| P3 | excerpt digest (`reading-registry/excerpt-digest`) | `reading-registry/excerpt-digest` (pure function of the source file) | `target_reading_registry.clj:79`; consumed at `fsel:167` | every decision, for every declared target source: outer pre-choice field and inner field | not stored on its own; copied into the reading record as `:excerpt-digest` (P1) and into the failure as `:expected-digest` | all targets with a readable HEAD source | Returns a typed absence `:target-head-template-only` when the HEAD holds only the template sentence. The request builder is the external `scripts/wm_task_reading.py` (`selected_target_reading.clj:17`); `publish!` refuses a request whose `excerpt_sha256` differs (`target_reading_registry.clj:135`) |
| P4 | occurrence squares (`square-registry/current`, kind `:occurrence-squares-not-authored`) | `square-authoring/repair!` -> `square-registry/publish!` | `occurrence_square_authoring.clj:69` (publish call `:148`); `occurrence_square_registry.clj:87` | in a click, phase :occurrence-square-authoring (runner:6457), via `occurrence-square-authoring-step` (runner:2159); wired at `runtime:328` | `/home/joe/code/storage/wm-occurrence-squares/<value-digest of target>/<analysis-digest>.edn` (`occurrence_square_registry.clj:10,13,87`) | selected target only: `selected-need` (`occurrence_square_authoring.clj:27`) reads the inner decision's `:target-policy-field` families | CONSUMED for every target in the outer pre-choice field because `runtime:368` sets `:require-occurrence-squares? true` and `fsel:180-185` fails the family when squares are absent. See "No producer found" item 1 |
| P5 | pattern graph file (`:pattern-graph-path`, `mined-pattern-graph.json`) | `build_graph` / `main` in `futon3c/scripts/mined_pattern_graph.py` | `futon3c/scripts/mined_pattern_graph.py:241` (default `--out`) | outside clicks, operator script; no caller found in futon2 or futon3c (searched `mined_pattern_graph` in `futon2/{src,scripts}`, `futon3c/{src,scripts}`, `futon3/scripts`) | `/home/joe/code/storage/operator-turns/mined-pattern-graph.json` (path from `runtime:37`) | n/a (one graph) | File on disk dated Oct 1 23:18 |
| P6 | pattern graph pin (`<graph>.pin.edn`, `graph-pin/load-pinned`) | `graph-pin/pin!` | `pattern_graph_pin.clj:111` | no caller found; see "No producer found" item 2 | `/home/joe/code/storage/operator-turns/mined-pattern-graph.json.pin.edn` (dated Oct 16 03:52) | n/a | `load-pinned` (`:124`) refuses `:graph-pin-missing` / `:graph-pin-mismatch`; `fsel:136-157` turns that into a failure on every target family |
| P7 | pattern graph diff (proposal that feeds the next graph rebuild) | `pattern-graph-diff/publish!` | `wm/pattern_graph_diff.clj:107`; call at runner:5830 | end of a click, in the close path, when `selected-action` exists and `:pattern-graph-diff-dir` is set (`runtime:367` defaults `:pattern-graph-diff-dir` to `runtime:39`) | `/home/joe/code/futon2/data/wm-pattern-graph-diffs/<run-id>.pattern-graph-diff.json` | selected target only | Not read by the click itself; `mined_pattern_graph.py` takes `--diffs` that were "inspected, manually applied" (docstring of `build_graph` args at line ~241) |
| P8 | pattern library manifest (`q2-construction/library-manifest`) | `q2-construction/library-manifest` | `q2_construction.clj:69`; call at `wm/cascade_decision.clj:1385` | every decision (:selection, redecisions), inside the inner construction census for the selected task | in the decision's `:selection-certificate :q2-construction`, then the run record; no file | the whole library is hashed once per decision; census is for the selected task | Library files are `futon3/library/**/*.flexiarg` (`occurrence_square_registry.clj:11`), hand-authored. Typed absence `:library-root-absent` / `:library-empty` |
| P9 | policy field (`:policy-field`, `:wm/pre-choice-policy-field-v1`) | `family-selection/full-eligible-target-field` | `fsel:200` (calls `families-for-field` `:136`); call at `mlos:602` | in a click, phase :selection (runner:6374) and again in every redecision; inside `select-live` (`mlos:519`) | `:policy-field` on the outer receipt (`mlos:730`), then judgement and run record | all on-map eligible targets (`mlos` `on-map`) | Needs P1 (readings), P4 (squares), P5+P6 (graph and pin) per target. A family lacking any is `:failed`, not `:computed`, and is kept out of the menu (P10) |
| P10 | sealed pre-choice menu (`:wm/sealed-outer-policy-menu-v1`, `:menu-pin`) | `family-selection/sealed-pre-choice-menu` | `fsel:333`; call at `mlos:613` | same place as P9 | `[:policy :sealed-menu]` on the outer receipt (`mlos` receipt block near 700-730) | only families with `:status :computed`; `:frontier` counts eligible/assessed/reading-absent (`fsel:302`) | Status `:refused` with reason `:no-canonically-bound-policy` when no entry binds, which is what triggers P1 |
| P11 | outer policy evidence provider and its declaration (`:wm/outer-policy-evidence-declaration-v1`) | `policy-provider-runtime/production-provider` -> `provide` | `outer_policy_provider_runtime.clj:67` (`provide` `:24`); wired at `runtime:253`; resolved at `mlos:615` | every outer selection in :selection, after the menu is sealed | declaration and records on the receipt (`:evidence-provider-source-pin`, `:policy-evidence`) | one declaration scoring every menu entry (`provide` scores `(:entries menu)`) | Alternative sources: `:outer-policy-evidence-provider` opt, or a static file via `provider/load-static` (`outer_policy_evidence_provider.clj:57`, `runtime` provider selection near `:250`). Refuses `:learning-authority-unavailable` unless `[:belief :status]` is `:produced` (`:28`). Calls `learning/load-shadow` itself (`:68`), a second read of P12/P13 inputs besides `mlos:807` |
| P12 | outer learning authority (`resources/wm/outer-learning-authority.edn`) | none in code (hand edit, committed) | read at `outer_learning_runtime.clj:19-25`; path `:10` | n/a | file, last changed by commit `af0fa4729` | n/a | Listed here for the dependency graph; see "No producer found", hand-authored |
| P13 | outer learning manifest (`data/wm-learning/run-record-manifest.edn`) | `outer-learning-manifest/record!` | `outer_learning_manifest.clj:41`; call at runner:8035 | end of every click that wrote a run record, phase :outer-learning-publication; active only when `:outer-learning-manifest-path` is set, which `runtime:369-370` does in `run-ordinary-opportunity!` | `/home/joe/code/futon2/data/wm-learning/run-record-manifest.edn` (atomic append, one entry per run record: path and sha256) | the click's own record only | Merged with the authority's `:run-records` in `load-shadow` (`outer_learning_runtime.clj:27-34`); a path whose bytes changed refuses (`outer_learning_manifest.clj`, `:outer-learning-record-drift`). Each click's evidence is therefore seen from the NEXT click on |
| P14 | meta field observation (`:meta-field-observation`, `:meta-field-source-pin`) | `meta-field/observe` | `meta_field_observation.clj:211`; call at `runtime:241` | start of the click in `production-defaults`, before the runner starts; only if an injury authority loaded (`runtime:231-233`) | opts, then `selection-judge` (`runtime:183`) -> `wm.clj` `select-outer-task` (`wm.clj:1540`) -> `mlos:784` verify | whole field (missions, tickets, excursions, algorithms) | When `injury-authority` is nil no observation is built and the selector runs without algorithm rows. Reloads the registries itself when no `:registry-snapshot` is passed (`meta_field_observation.clj:220-224`), separate from the :preference-refresh phase |
| P15 | ordinary click budget / availability (`:ordinary-click-availability`) | `futon3c.wm.ordinary-click-budget/availability` | `futon3c/src/futon3c/wm/ordinary_click_budget.clj:146`; call at `runtime:234-238` | start of the click in `production-defaults`, BEFORE the ration is consumed in :ration-admission (runner:6266, hook set at `futon3c/src/futon3c/wm/runner_service.clj:942`) | opts, then P14 | n/a | `runtime:236-238` swallows any Throwable and yields nil, so a broken budget read silently drops click availability from the META observation. The count `allocated` is a hand-edited constant (`ordinary_click_budget.clj:43`). `consume!` runs in `futon3c/src/futon3c/transport/http.clj:10122`. `wm_click.sh:68-73` reads the same figure over HTTP before firing |
| P16 | META injury authority (`data/wm-meta-injuries/HEAD.edn`; `injury-store/load-authority`) | `meta-injury-store/deposit!` | `meta_injury_store.clj:169` (reader `:198`); call at runner:8011 | end of every click that wrote a run record, phase :meta-injury-deposit | `data/wm-meta-injuries/<run-id>.edn` and `data/wm-meta-injuries/HEAD.edn` (overwritten each click) | the click's own record | Marked "legacy" in its docstring (`:154`); still the only authority `runtime:231` reads. HEAD.edn names only the most recent run. File present, dated Oct 19 07:39 |
| P17 | META injury ledger (`ledger.edn`) | `meta-injury-store/ingest-record-file!` | `meta_injury_store.clj:136`; call at runner:8022 | end of every click that wrote a run record, phase :meta-terminal-injury-ingest | `/home/joe/code/futon2/data/wm-meta-injuries/ledger.edn` (path `meta_self_heal_production.clj:11`, set into opts at `runtime` `run-opportunity!`) | the click's own record | Also written by the repair route: `meta-self-heal/route!` persists ledger and `transaction.edn` per checkpoint (`meta_self_heal_production.clj:161,174`). Read before the ordinary run by `production-readiness` (`:77`) |
| P18 | mission substrate freshness (substrate-2 mission entities) | `mission-registry/refresh-mission-substrate!` -> `upsert-mission-record!` | `mission_registry.clj:579` (upsert `:409`); wired as `:refresh-fn` at `runtime:302-306` | in a click, phase :preference-refresh (runner:6275, fn chosen at runner:6277) | substrate-2 entities `mission|<id>` via `substrate/put-doc!` | every mission file found by `load-missions-from-files` | Throws `:mission-substrate-freshness-unestablished` when the scan is empty, any upsert errors, or the stored sha differs; with `:refresh-required? true` (`runtime` defaults) a non-:ok refresh stops the click (runner:6283-6290) |
| P19 | C-vector (live preference) | `c-vector/maybe-refresh!` | `c_vector.clj:311`; same `:refresh-fn` (`runtime:304`) | same phase as P18 | in-memory atom `c-state` | whole corpus | Re-derives only when `stale?` |
| P20 | outer task population (`:tasks` given to the selector) | inline in `generate-war-machine` | `wm.clj:6633-6644` (`outer-task-population`); selector call `wm.clj:6654` | in a click, inside the :selection judge | passed straight to `select-outer-task`; not retained as a file | all open missions, live excursions, live tickets (filtered to the flight target for flights) | Tickets include those published by `finding-ticket/publish!` (P23). `mlos:827` `live-registry-tasks` is a separate reader used by previews |
| P21 | pipeline cascade snapshot (`fetch-snapshot`; summary, graph, agency) | `meta-live/fetch-pipeline-snapshot` | `mlos:75`; call at `mlos:532` | every outer selection, inside :selection and each redecision (fetched again each time) | `:summary-source`, `:graph-source`, `:agency-source` pins on the receipt | whole pipeline map | Producers are `futon3c.logic.cascade-real-live/cascade-real-summary` (`futon3c/src/futon3c/logic/cascade_real_live.clj:257`) and `cascade-real-graph`, called in-JVM when present (`mlos:85-95`), else HTTP `/api/alpha/cascade-real`. Targets not on the map are excluded as `:pipeline/not-on-current-map` |
| P22 | stop-line queue / repair obligations (`repair/open-obligations`) | `repair/record-system-failure!` and `repair/record-review-failure!` | `repair_obligation.clj:549`, `:504`; open-set reader `:948`; calls at runner:5468 and 6151 (system), 7606 and 7784 (review) | end of a non-success click (close path) and in the close-fallback handler; read by :stop-line-memory (runner:6292) | `/home/joe/code/futon2/data/wm-repair-obligations/findings/*.edn` (`repair_obligation.clj:25`) | the failing attempt only | `repair-discharge/finalize-run!` (`repair_discharge.clj:200`, call at runner:6059) transitions obligations at close. `wm_click.sh:183-224` counts open obligations by evaluating in the JVM and only reports them |
| P23 | repair ticket for a finding (becomes a ticket task in the registry) | `finding-ticket/publish!` | call at `repair_obligation.clj:542` and `:622` | inside P22 | a ticket document committed in a repo (`finding_ticket.clj:61,82`) and queue entry via `ticket-queue/enqueue!` (`ticket_queue.clj:56`) | the failing attempt only | `ticket-queue/read-declaration` (`ticket_queue.clj:44`) reads `/home/joe/code/futon2/data/wm-ticket-queue/queue.edn` or the resource default (used at `wm.clj:6690`). The caller chain from `finding-ticket/publish!` to `enqueue!` was not traced (see Uncertain) |
| P24 | run record (`data/wm-runs/tick-run-record-<run-id>.edn`) | `persist-run-record!` -> `run-record-io/write-run-record-stream!` | runner:1010 (write at 1267); `run_record_io.clj:8`; call at runner:8003 | after `run-opportunity-core!` returns, every click that reaches it | `/home/joe/code/futon2/data/wm-runs/` (`default-run-record-dir`, runner:138) | the click | Consumers: P13, P16, P17, `wm_click.sh:378` (requires the file). The Q2, Q9, Q10 certificates and the Q6 stream entry (`previous-run/close!`, runner:1269) are built inside the same function |
| P25 | terminal receipt (`:terminal-receipt`) | `terminal-receipt/attach` | `wm/terminal_receipt.clj:201`; call at runner:1266 | inside P24 | key on the run record | the click | |
| P26 | requirements verdict (`<run-record>.requirements.edn`) | `scripts/wm_run_alert.py` | `scripts/wm_run_alert.py:59` (file name built there); call at `wm_click.sh:384` | after a `wm_click.sh --run` click completes; the script exits 1 if the file is absent (`wm_click.sh:378-390`) | `<run-record path>.requirements.edn` beside the run record | the click | Not produced when the click is fired by another route than `wm_click.sh` |
| P27 | Q6 predecessor / run stream (`previous-run/*predecessor*`) | `previous-run/start!` (predecessor) and `previous-run/close!` (append) | `previous_run.clj:145`, `:153`; calls at runner:7858 and 1269 | start and end of each click | `:started` and `:closed` events appended to the stream file in the run-record dir (`previous_run.clj:50,78`) | the click | Bound for the decision at runner:7890; read at `wm/cascade_decision.clj:1242` |
| P28 | cohort attempt: start event, checkpoints, close | `cohort/start-attempt!`, `append-checkpoint!`, `close-attempt!` | `full_loop_cohort.clj:569`, `:649`, `:704`; calls at runner:5324, 5396-5422, 5985 | each click: start before selection, checkpoints at each of the five required checkpoints (runner `required-checkpoints`, line 148) and at close | `/home/joe/code/futon2/data/wm-full-loop/<cohort>/<attempt>/NNN-*.edn` (`full_loop_cohort.clj:26`) | the click | `start-attempt!` throws "cohort is not activated" unless `<cohort>/activation.edn` exists (`:610-612`); that file is produced only by operator action, see P29 |
| P29 | cohort activation (`activation.edn`) | `cohort/activate!` | `full_loop_cohort.clj:362` | outside clicks: operator CLI `full_loop_cli.clj:660` ("activate") and `scripts/wm_full_loop_cohort.clj:22`; also `:955` (successor creation) | `data/wm-full-loop/<cohort>/activation.edn` | n/a | Default cohort definition `/home/joe/code/futon2/holes/labs/M-aif-full-loop-46/cohort.edn` (`full_loop_cohort.clj:22`) is hand-authored. Activation files for cohorts 40 to 46 exist on disk |
| P30 | retained token outcome, kernel example, run-ending classification (cohort `retained/` files) | `retain-token-outcome!`, `retain-kernel-example!`, `retain-run-ending!` | runner:4607, 4662, 4683; calls at 5645, 5675, 5900 | close path of a click that selected an action with a D-task result | `data/wm-full-loop/<cohort>/<attempt>/retained/{token-outcome,surprises,kernel-example,run-ending-classification}.edn` | selected action only | |
| P31 | learning trial ledger (read by `novelty/read-inputs`) | `learning-ledger/record!` | `learning_trial_ledger.clj:26`; call at runner:4637 (inside `retain-token-outcome!`) | close path, as P30 | `/home/joe/code/futon2/data/wm-learning-trials` (`learning_trial_ledger.clj:14`) | selected action only | Consumed as `:novelty-inputs` (`wm/cascade_decision.clj:1262`, reader `parameter_novelty.clj:67`) on the next decision |
| P32 | cascade feedback store (`:cascade-feedback-metadata`, pattern evidence prior) | `cascade-feedback/record!` | `cascade_feedback.clj:187`; call at runner:5814 | close path, when `:cascade-feedback-path` is supplied (runtime sets the default at `run-ordinary-opportunity!`) | `/home/joe/code/futon2/data/wm-pattern-feedback/events.edn` (`cascade_feedback.clj:19`) | selected action only | Metadata is loaded ONCE at click start (`runtime` `run-ordinary-opportunity!`, `cascade-feedback/load-construction-metadata`), so a click never sees its own record |
| P33 | observation labels (`:observation-labels-path`, rates view) | `observation-label-wire/record-declaration-reads!` | `observation_label_wire.clj:30`; call at runner:1117 (inside `persist-run-record!`) | after the click | `/home/joe/code/futon2/data/wm-observation-labels/labels.edn` (path at runner:335) | declarations the click read | Read in the next decision by `observation-label-view` (`wm/cascade_decision.clj:455-460`). The comment at runner:1116 says "these labels are for the next decision" |
| P34 | machine interpretation of a refused target (`wm-interpretations/<target>.edn`) | `click-ask/click-ask-fn` -> `want-interpretation/publish!` | `wm/click_ask.clj:57`; `want_interpretation.clj:492`; called at runner:6526 | in a click, after :selection when the decision refuses a target `:no-admitted-interpretation`, at most once; never for flights (runner:6508) | `/home/joe/code/futon2/data/wm-interpretations/<target>.edn` (`want_interpretation.clj:359`) | one refused target per click | After publication the runner re-runs the decision once (runner:6549) |
| P35 | declared cascade sources, beta by context (`:declared-betas`, `:beta-by-context`) | `cascade-sources/load-declared` (reader of hand-authored files) with `mission-hole-wants/merge-into-sources` adding every open mission and ticket under context `:WM` | `cascade_sources.clj:255-294`; `wm.clj:6663-6671` | in a click, inside :selection judge | in memory (`assembled :declared-betas`, `cascade_problems.clj:401`) | every target whose context has a beta | Beta values come from `resources/wm/cascade-sources/*.edn` (hand-authored). `declared-beta` (`wm/cascade_decision.clj:1147`) refuses `:beta-not-declared` if any computed target lacks one |
| P36 | reading precision campaign / declaration (`precision/declaration-for`) | `ensure-reading-precision-campaign!` -> `reading-precision/register-campaign!`; `declaration-for` issues on demand | `runtime:46`, called at `runtime:229`; `reading_policy_precision.clj:231,259` | start of the click, in `production-defaults` | in-memory atoms `!campaign`, `!declarations` of `reading_policy_precision.clj:33-34` | one beta, bounded by `:identity-limit` identities | The campaign file `resources/wm/reading-precision-beta/wm-readiness-2026-10-17-campaign.edn` is hand-authored (`runtime:41`). State is lost on JVM restart and re-registered at the next click |
| P37 | enactment fold (E, `:enactment-fold`) and conditioning steps (`:conditioning-steps`) | `enactment-fold-source/fold-from-flights`, `conditioning-steps` read flight records written by `flight-driver/run-flight!` | readers `enactment_fold_source.clj:63,75`; writer `flight_driver.clj:241` (`write!`); read call at `wm.clj:6832-6845` | the WRITER runs outside clicks, as the operator CLI `clojure -M -m futon2.aif.flight-driver` (`flight_driver.clj:4,317`). The reader runs each :selection | `/home/joe/code/futon2/data/wm-interpretations/flights/<flight-id>.edn` (and `flights/enactments`, `flight_driver.clj:234`) | flown targets only | An ordinary click writes nothing here. Flight records on disk: 7 files. Ties into P38 |
| P38 | policy history pin and prefix census (`:policy-history-pin`, `history-census`) | `policy-prefix-admission/history-census` and `census-pin` (pure, over P37) | `policy_prefix_admission.clj:137,198`; call at `wm/cascade_decision.clj:1204-1207` | every decision in :selection | `[:selection-certificate :policy-history-pin]` and run record | every policy in the inner field | Computed, not stored. With no flights the dir status is `:absent` and the "certified zero-history carrier" bound to this pin is used (comment at `cascade_decision.clj:86`) |
| P39 | cascade revision proposals (`:cascade-revision-proposals-fn`) | `cascade-revision-producer/make-proposals-fn` | `cascade_revision_producer.clj:115`; wired at `runtime:294-296` | only in a click's revision loop after a reviewer "request changes" (runner:3630) | evidence under the attempt dir `<attempt-evidence-dir>/cascade-revision` | selected target | Needs the `selected-judgment` atom set by `:judge-fn` (`runtime:278-292`) |
| P40 | cascade proposals supply (`cascade-proposals/load-supply`) | `cascade-proposals/generate-retrieval!` (CLI `-main`) and `repairs/supply` | `cascade_proposals.clj:50,125`; read at `wm.clj:6686` (`load-supply`, `cascade_proposals.clj:115`) | the retrieval writer is a by-hand CLI; the repair-store part is recomputed on each read | `/home/joe/code/futon2/data/wm-cascade-proposals/<uuid>/proposal.edn` (`cascade_proposals.clj:16`) | per target | One bundle directory exists on disk |
| P41 | repair loop ledger, logs and debugger pages | `record`, `log`, `debugger_page`, held-stop `write_text` | `wm_click_repair_loop.py:100,93,483,383`; per-click log `:364` | outside clicks, by the loop (`run`, `:758`), per click | `data/wm-click-loop/{ledger.jsonl,loop.log,click-NNN-*.log,debugger/*.md}` | the loop's own clicks | Not read by the click. Read by the repair seat and the operator |
| P42 | reloaded namespaces in the serving JVM | `reload_changed` (`require :reload` through `proof-eval.sh`) | `wm_click_repair_loop.py:230-257` | outside clicks, before each click and after each repair, only when no click is running | the serving JVM | namespaces under `futon2/src` and `futon3c/src/futon3c/wm` committed since the last reload | A file with uncommitted edits is skipped and logged (`:240-244`) |
| P43 | fresh cast sessions and repair seat registration | `reset_cast_sessions`, `ensure_seat` | `wm_click_repair_loop.py:125,499` | outside clicks, in `fire` before dispatch (`:348-358`) and before repair | Agency (`POST /api/alpha/agents/<seat>/reset-session` and `/agents/restore`) | the three cast seats | `fire` refuses the click when the outer-policy preflight (`:413`) finds no provider configured |
| P44 | ordinary click launch (the `POST` that starts a click) | `wm_click.sh` fire block | `wm_click.sh:326` | outside clicks, operator or the repair loop (`wm_click_repair_loop.py:348` runs `wm_click.sh --run`) | futon3c runner service, returns `click-id`; the run id is minted at `wm_click.sh` (`/proc/sys/kernel/random/uuid`) | n/a | `wm_click.sh` also writes a throwaway `/tmp/wm_click_wires.clj` (`:183`), which it only reads back |

Row count: 44 rows (P12 and P29 are hand-authored or operator-only entries kept so other aspects can cite the dependency).

## 2. No producer found

What was searched, for every item: callers of the writing function across `futon2/src`, `futon2/scripts`,
`futon3c/src`, `futon3c/scripts` (and `futon3/scripts` for the graph); then the named data directory
on disk.

1. Occurrence squares for any target other than the outer-selected one, and a reachable path for a
   target that has a reading but no squares. Searched: callers of `square-registry/publish!`
   (only `occurrence_square_authoring.clj:148` and tests); callers of `square-authoring/repair!`
   (only the `:occurrence-square-authoring-fn` at `runtime:328`); `prechoice-needs`
   (`selected_target_reading.clj:210`) lists only the two reading failure kinds, never
   `:occurrence-squares-not-authored`. By reading, a circular dependency follows:
   - The outer field requires squares for every target with a reading (`runtime:368`,
     `fsel:180-185`), so a target without squares is not `:computed` and is outside the sealed menu
     (`fsel:333` uses only computed families).
   - Squares are authored only when `selected-need` (`occurrence_square_authoring.clj:27`) finds that
     failure on the INNER field, which exists only for the outer-selected target.
   - So a target whose reading was just prepared by P1 but which has no squares cannot be selected,
     and so is never given squares. It is not executed; confirm with a click or a REPL read of the
     field. On disk: 9 target-reading directories, 8 occurrence-square directories.
2. Pattern graph pin (`mined-pattern-graph.json.pin.edn`). `graph-pin/pin!` (`pattern_graph_pin.clj:111`)
   has no caller in `futon2/{src,scripts}` or `futon3c/{src,scripts}`; the only references to the
   namespace are its consumers (`target_policy_family`, `pattern_retraction`, `pattern_graph_view`,
   `family_selection`). `mined_pattern_graph.py` rewrites the graph (`:241`) without re-pinning, and
   `load-pinned` refuses a pin whose sha differs. The pin on disk is dated Oct 16, newer than the graph (Oct 1).
   Any rebuild of the graph makes every family fail `:graph-pin-mismatch` until someone pins by hand.
3. META self-heal route inputs `:diagnosis-evidence-pin` and `:acceptance-test`.
   `meta-self-heal/production-readiness` (`meta_self_heal_production.clj:77`) and `route!` (`:133`)
   require both when an active injury exists. The only producer found is the test harness
   (`meta_self_heal_harness.clj:136`). The ports (`:agency-invoke-fn`, `:repair-enact-fn`,
   `:repair-retry-fn`) are supplied by `futon3c/src/futon3c/wm/runner_service.clj:70-89` through
   `meta_self_heal_adapters.clj`; the two evidence terms are not. Searched `diagnosis-evidence` and
   `acceptance-test` in `futon2/{src,scripts}` and `futon3c/{src,scripts}`. Effect: with an active
   injury the route reports `:meta-self-heal/production-config-required`.
4. Enactment fold and conditioning steps from an ordinary click. Only `flight-driver` writes
   `data/wm-interpretations/flights/` (P37). The legacy habit file
   `data/wm-habit/cascade-prior.edn` has no writer on purpose (runner:6093-6120, typed absence
   `:legacy-habit-store-retired`), while `cascade_habit_store/read-state` and `policy.clj:451` still
   read it. Searched callers of `record-selection!` and `record-reinforcement!` (none live).
5. Hand-authored inputs with no code writer (each read, none produced): `resources/wm/outer-learning-authority.edn`
   (P12), `resources/wm/approved-algorithms.edn`, `resources/wm/reading-precision-beta/*.edn` (P36),
   `resources/wm/cascade-sources/*.edn` (P35), `resources/wm/ticket-queue.edn`, the cohort
   preregistration `holes/labs/M-aif-full-loop-46/cohort.edn` (P29), `futon3/library/**/*.flexiarg`.
   The ordinary-click allocation is a source constant (`ordinary_click_budget.clj:43`).
6. Repair obligation dismissal and resolution. `dismiss-wontfix!`, `dismiss-condition-cleared!`,
   `dismiss-unexecuted!`, `dismiss-echo!`, `dismiss-fixture-pollution!` (`repair_obligation.clj:978-1198`)
   and `record-historical-verification!` have no caller in `futon2/{src,scripts}`; `commit-historical-resolution!`
   is called only from `futon3c/src/futon3c/wm/run4_historical_successor.clj:87`. Open obligations
   leave the queue only by these routes or by `repair-discharge/finalize-run!`.
7. A scheduled or per-click producer for the pattern graph (P5) and for flights (P37): both exist as
   operator commands; nothing in the click path or in `wm_click_repair_loop.py` calls them.

## 3. Uncertain

- P1 reading order. `prepare!` publishes at most 3 readings per click. How many targets are eligible
  at HEAD, and so how many clicks the reading frontier needs, was not counted (would need a read of
  the registry). The 3 comes from `runtime:320`.
- P23 ticket chain. `finding-ticket/publish!` is called from `record-system-failure!` (`repair_obligation.clj:542,622`),
  but I did not read `finding_ticket.clj:95-135` far enough to confirm that it calls
  `ticket-queue/enqueue!`; the "queue entry" part of that row is inferred from `ticket_queue.clj:56` existing.
- P18 watcher lane. The docstring of `upsert-mission-record!` (`mission_registry.clj:409`) says a "watcher
  lane" calls it per land; no such caller was found in futon2. It may live in futon3c. Not searched there.
- P37 who runs flights, and when. Callers of `flight-driver/-main` were not found in `scripts/`;
  `scripts/manual_overnight_flights.clj` runs "flights" but I did not trace whether it uses `run-flight!`.
- P14 `meta-field/observe` versus the registry snapshot used by `select-live`: `observe` loads its own
  registries unless given a snapshot (`meta_field_observation.clj:220`), while the selector builds
  tasks again in `wm.clj:6633`. Whether the two can differ within a click, and what `field/verify`
  does when they do, was not traced.
- P11 production provider reads `learning/load-shadow` a second time (`outer_policy_provider_runtime.clj:68`)
  after `mlos:807` already did; a manifest append between the two reads could change the dataset
  within one selection. Not examined.
- P15 `availability` is read at `runtime:234` before the ration is consumed (:ration-admission,
  runner:6266). I did not check whether the META observation is meant to see the pre-consumption figure.
- Inputs seen but not traced to a writer: tripwire trips (`tripwire.clj:29`), `/run/futon2` interoceptive lock
  (`interoceptive_store_lock.clj:12`), a4a substrate files (`a4a_substrate.clj:14-20`), the C-vector corpus
  behind `c_vector/stale?`, `data/wm-cascade-continuation-closes` (`runtime:43`), execution journal and
  executable cascade roots (`wm/cascade_decision.clj:1450,1453`), and the `run4_*` families in
  futon3c.
