# AUD-D2 findings

Verdict: I_absent_is_loud over default scope | repos futon2=082da13, futon3=e3e5ef4, futon0=7a568e8, futon3a=58ea67a | helpers total=56 loud=38 silent=17 declared-optional=1 refused=0 | silent call sites=45 silent+absent-now=7

## Helpers

| helper | class | file:line | reason |
| --- | --- | --- | --- |
| futon0.futonzero.rollout-ledger/read-edn | loud | futon0/scripts/futon0/futonzero/rollout_ledger.clj:33 | missing/unparseable path propagates as an exception |
| futon0.report.joe-hud/read-edn-file | silent | futon0/scripts/futon0/report/joe_hud.clj:338 | absence/parse failure collapses to nil/empty/default |
| futon0.rhythm.affect/load-json-file | loud | futon0/scripts/futon0/rhythm/affect.clj:332 | missing/unparseable path propagates as an exception |
| futon0.rhythm.affect/load-jsonl-file | loud | futon0/scripts/futon0/rhythm/affect.clj:339 | missing/unparseable path propagates as an exception |
| futon0.vitality.scanner/read-json-file | silent | futon0/scripts/futon0/vitality/scanner.clj:38 | absence/parse failure collapses to nil/empty/default |
| checks.absent-is-loud-lint/read-forms | loud | futon2/checks/absent_is_loud_lint.clj:45 | missing/unparseable path propagates as an exception |
| checks.r2-channel-contract/read-clojure-forms | loud | futon2/checks/r2_channel_contract.clj:41 | missing/unparseable path propagates as an exception |
| checks.r2-channel-contract/definition-line | loud | futon2/checks/r2_channel_contract.clj:57 | missing/unparseable path propagates as an exception |
| checks.r2-channel-contract/read-edn-forms | loud | futon2/checks/r2_channel_contract.clj:97 | missing/unparseable path propagates as an exception |
| checks.r2-channel-contract/docstring-finding | loud | futon2/checks/r2_channel_contract.clj:157 | missing/unparseable path propagates as an exception |
| checks.r8-f-contract/read-edn-forms | loud | futon2/checks/r8_f_contract.clj:24 | missing/unparseable path propagates as an exception |
| fold-author/read-edn-file | loud | futon2/scripts/fold_author.clj:96 | missing/unparseable path propagates as an exception |
| fold-author/prose-for | loud | futon2/scripts/fold_author.clj:118 | missing/unparseable path propagates as an exception |
| fold-author/read-answer | loud | futon2/scripts/fold_author.clj:183 | missing/unparseable path propagates as an exception |
| futon2.report.war-machine/read-edn-file | silent | futon2/scripts/futon2/report/war_machine.clj:482 | absence/parse failure collapses to nil/empty/default |
| futon2.report.war-machine/read-json-file | silent | futon2/scripts/futon2/report/war_machine.clj:488 | absence/parse failure collapses to nil/empty/default |
| futon2.report.war-machine/read-strategy-cascade | loud | futon2/scripts/futon2/report/war_machine.clj:1029 | missing/unparseable path propagates as an exception |
| futon2.report.war-machine/safe-slurp-json | silent | futon2/scripts/futon2/report/war_machine.clj:3451 | absence/parse failure collapses to nil/empty/default |
| reference-regression/run-constructor | loud | futon2/scripts/reference_regression.clj:71 | missing/unparseable path propagates as an exception |
| reference-regression/deposit-has-seal-field? | loud | futon2/scripts/reference_regression.clj:333 | missing/unparseable path propagates as an exception |
| ants.aif.experiment-schema/read-registration | loud | futon2/src/ants/aif/experiment_schema.clj:17 | missing/unparseable path propagates as an exception |
| ants.compare/count-cyberants | loud | futon2/src/ants/compare.clj:72 | missing/unparseable path propagates as an exception |
| ants.compare-replay/read-edn | loud | futon2/src/ants/compare_replay.clj:29 | missing/unparseable path propagates as an exception |
| ants.cyber/load-cyber-edn | silent | futon2/src/ants/cyber.clj:258 | absence/parse failure collapses to nil/empty/default |
| ants.tournament/load-cyberants | silent | futon2/src/ants/tournament.clj:75 | absence/parse failure collapses to nil/empty/default |
| futon2.aif.a4a-substrate/read-edn-file | loud | futon2/src/futon2/aif/a4a_substrate.clj:140 | missing/unparseable path propagates as an exception |
| futon2.aif.actuator-a3/advance-mission-doc! | loud | futon2/src/futon2/aif/actuator_a3.clj:696 | missing/unparseable path propagates as an exception |
| futon2.aif.anticipation/load-anticipations | silent | futon2/src/futon2/aif/anticipation.clj:27 | absence/parse failure collapses to nil/empty/default |
| futon2.aif.arguing-worlds/read-freeze | loud | futon2/src/futon2/aif/arguing_worlds.clj:235 | missing/unparseable path propagates as an exception |
| futon2.aif.belief/section-ids-from-stack-annotations | loud | futon2/src/futon2/aif/belief.clj:472 | missing/unparseable path propagates as an exception |
| futon2.aif.belief/classify-entity-tags-from-stack-annotations | silent | futon2/src/futon2/aif/belief.clj:555 | absence/parse failure collapses to nil/empty/default |
| futon2.aif.belief/classify-entity-repos-from-stack-annotations | silent | futon2/src/futon2/aif/belief.clj:761 | absence/parse failure collapses to nil/empty/default |
| futon2.aif.belief/classify-entity-ticks-from-stack-annotations | silent | futon2/src/futon2/aif/belief.clj:794 | absence/parse failure collapses to nil/empty/default |
| futon2.aif.code-build-match/load-clean | loud | futon2/src/futon2/aif/code_build_match.clj:56 | missing/unparseable path propagates as an exception |
| futon2.aif.fold-escrow/load-deposit | loud | futon2/src/futon2/aif/fold_escrow.clj:167 | throws or returns tagged missing/unreadable state |
| futon2.aif.full-loop-cohort/read-edn | loud | futon2/src/futon2/aif/full_loop_cohort.clj:35 | missing/unparseable path propagates as an exception |
| futon2.aif.full-loop-cohort/activate! | loud | futon2/src/futon2/aif/full_loop_cohort.clj:131 | throws or returns tagged missing/unreadable state |
| futon2.aif.lane-futility/read-trace-file | loud | futon2/src/futon2/aif/lane_futility.clj:28 | missing/unparseable path propagates as an exception |
| futon2.aif.mission-registry/mission-doc->entry | loud | futon2/src/futon2/aif/mission_registry.clj:164 | missing/unparseable path propagates as an exception |
| futon2.aif.rollout/load-move-set | loud | futon2/src/futon2/aif/rollout.clj:10 | missing/unparseable path propagates as an exception |
| futon2.aif.sorry-registry/load-sorrys | loud | futon2/src/futon2/aif/sorry_registry.clj:42 | missing/unparseable path propagates as an exception |
| futon2.aif.tripwire/read-record | loud | futon2/src/futon2/aif/tripwire.clj:112 | missing/unparseable path propagates as an exception |
| futon2.aif.tripwire-calibration/read-edn-lines | silent | futon2/src/futon2/aif/tripwire_calibration.clj:25 | absence/parse failure collapses to nil/empty/default |
| futon2.aif2.tension/read-curvature-signal | declared-optional | futon2/src/futon2/aif2/tension.clj:198 | name/docstring declares optional/graceful read |
| find-snatch/parse-pattern-file | loud | futon3/checks/find_snatch.clj:47 | throws or returns tagged missing/unreadable state |
| checks.library-graph-lint/parse-pattern | loud | futon3/checks/library_graph_lint.clj:80 | missing/unparseable path propagates as an exception |
| checks.library-graph-lint/read-index-cache | silent | futon3/checks/library_graph_lint.clj:242 | absence/parse failure collapses to nil/empty/default |
| checks.library-graph-lint/read-edn-or | silent | futon3/checks/library_graph_lint.clj:304 | absence/parse failure collapses to nil/empty/default |
| playout-snatch/parse-why | loud | futon3/checks/playout_snatch.clj:259 | missing/unparseable path propagates as an exception |
| checks.spider-fleet/read-edn | silent | futon3/checks/spider_fleet.clj:20 | absence/parse failure collapses to nil/empty/default |
| checks.spider-runner/read-edn | silent | futon3/checks/spider_runner.clj:38 | absence/parse failure collapses to nil/empty/default |
| checks.spider-runner/response-prompt | loud | futon3/checks/spider_runner.clj:130 | missing/unparseable path propagates as an exception |
| checks.spider-runner/apply-output! | loud | futon3/checks/spider_runner.clj:278 | missing/unparseable path propagates as an exception |
| checks.spider-runner/process-turn! | silent | futon3/checks/spider_runner.clj:332 | absence/parse failure collapses to nil/empty/default |
| futon.notions/load-pattern-index | loud | futon3a/src/futon/notions.clj:30 | missing/unparseable path propagates as an exception |
| sidecar.store/load-store-from-audit-log | loud | futon3a/src/sidecar/store.clj:75 | missing/unparseable path propagates as an exception |

## Silent Helper Call Sites

| file:line | helper | path expression | guard form | exists now | resolved path |
| --- | --- | --- | --- | --- | --- |
| futon0/scripts/futon0/report/joe_hud.clj:427 | futon0.report.joe-hud/read-edn-file | (str futon5a-root "/data/alignment.edn") | none | absent | /home/joe/code/futon5a/data/alignment.edn |
| futon0/scripts/futon0/report/joe_hud.clj:428 | futon0.report.joe-hud/read-edn-file | (str futon5a-root "/data/stack-logic-model.edn") | none | absent | /home/joe/code/futon5a/data/stack-logic-model.edn |
| futon0/scripts/futon0/report/joe_hud.clj:429 | futon0.report.joe-hud/read-edn-file | (str futon5a-root "/data/jsdq-terminal-vocabulary.edn") | none | absent | /home/joe/code/futon5a/data/jsdq-terminal-vocabulary.edn |
| futon0/scripts/futon0/vitality/scanner.clj:237 | futon0.vitality.scanner/read-json-file | path | when-let | dynamic/refused | dynamic path |
| futon0/scripts/futon0/vitality/scanner.clj:428 | futon0.vitality.scanner/read-json-file | config | or | dynamic/refused | dynamic path |
| futon0/scripts/futon0/vitality/scanner.clj:436 | futon0.vitality.scanner/read-json-file | output-path | none | dynamic/refused | dynamic path |
| futon2/scripts/futon2/report/war_machine.clj:498 | futon2.report.war-machine/read-edn-file | capability-star-map-path | if | present | /home/joe/code/futon0/holes/missions/M-capability-star-map.graph.edn |
| futon2/scripts/futon2/report/war_machine.clj:536 | futon2.report.war-machine/read-edn-file | mission-domain-ratified-path | if | present | /home/joe/code/futon6/data/mission-domain-ratified.edn |
| futon2/scripts/futon2/report/war_machine.clj:547 | futon2.report.war-machine/read-edn-file | mission-fold-view-path | if | present | /home/joe/code/futon6/data/mission-fold-view.edn |
| futon2/scripts/futon2/report/war_machine.clj:572 | futon2.report.war-machine/read-json-file | forward-model-centrality-path | or | present | /home/joe/code/futon7/holes/M-futon-forward-model.centrality.json |
| futon2/scripts/futon2/report/war_machine.clj:592 | futon2.report.war-machine/read-edn-file | forward-model-roi-results-path | if | present | /home/joe/code/futon7/holes/M-futon-forward-model.roi-results.edn |
| futon2/scripts/futon2/report/war_machine.clj:2075 | futon2.report.war-machine/read-edn-file | (str futon5a-root "/data/alignment.edn") | when-let | absent | /home/joe/code/futon5a/data/alignment.edn |
| futon2/scripts/futon2/report/war_machine.clj:2089 | futon2.report.war-machine/read-edn-file | (str futon5a-root "/data/stack-logic-model.edn") | when-let | absent | /home/joe/code/futon5a/data/stack-logic-model.edn |
| futon2/scripts/futon2/report/war_machine.clj:2150 | futon2.report.war-machine/read-edn-file | (str futon5a-root "/data/stack-logic-model.edn") | when-let | absent | /home/joe/code/futon5a/data/stack-logic-model.edn |
| futon2/scripts/futon2/report/war_machine.clj:2211 | futon2.report.war-machine/read-edn-file | (str futon5a-root "/data/stack-logic-model.edn") | none | absent | /home/joe/code/futon5a/data/stack-logic-model.edn |
| futon2/scripts/futon2/report/war_machine.clj:3516 | futon2.report.war-machine/safe-slurp-json | mark2-state-path | none | present | /home/joe/code/storage/mark2/state.json |
| futon2/scripts/futon2/report/war_machine.clj:3530 | futon2.report.war-machine/safe-slurp-json | f | none | dynamic/refused | dynamic path |
| futon2/scripts/futon2/report/war_machine.clj:3682 | futon2.report.war-machine/safe-slurp-json | f | if | dynamic/refused | dynamic path |
| futon2/scripts/futon2/report/war_machine.clj:3850 | futon2.report.war-machine/read-edn-file | invariant-model-path | when-let | present | /home/joe/code/futon4/futon-stack-invariant-model.edn |
| futon2/scripts/futon2/report/war_machine.clj:4223 | futon2.aif.belief/classify-entity-tags-from-stack-annotations | unresolved | none | dynamic/refused | dynamic path |
| futon2/scripts/futon2/report/war_machine.clj:4224 | futon2.aif.belief/classify-entity-repos-from-stack-annotations | unresolved | none | dynamic/refused | dynamic path |
| futon2/scripts/futon2/report/war_machine.clj:4225 | futon2.aif.belief/classify-entity-ticks-from-stack-annotations | unresolved | none | dynamic/refused | dynamic path |
| futon2/src/ants/cyber.clj:284 | ants.cyber/load-cyber-edn | (:config-path cyber) | if-let | dynamic/refused | dynamic path |
| futon2/src/ants/tournament.clj:222 | ants.tournament/load-cyberants | hex-path | none | dynamic/refused | dynamic path |
| futon2/src/ants/tournament.clj:223 | ants.tournament/load-cyberants | sigil-path | none | dynamic/refused | dynamic path |
| futon2/src/futon2/aif/anticipation.clj:31 | futon2.aif.anticipation/load-anticipations | default-events-path | none | present | /home/joe/code/calendar/events.edn |
| futon2/src/futon2/aif/anticipation.clj:139 | futon2.aif.anticipation/load-anticipations | path | none | dynamic/refused | dynamic path |
| futon2/src/futon2/aif/belief.clj:571 | futon2.aif.belief/classify-entity-tags-from-stack-annotations | default-stack-annotations-path | none | present | /home/joe/code/futon5a/holes/stack-annotations.edn |
| futon2/src/futon2/aif/belief.clj:767 | futon2.aif.belief/classify-entity-repos-from-stack-annotations | default-stack-annotations-path | none | present | /home/joe/code/futon5a/holes/stack-annotations.edn |
| futon2/src/futon2/aif/belief.clj:800 | futon2.aif.belief/classify-entity-ticks-from-stack-annotations | default-stack-annotations-path | none | present | /home/joe/code/futon5a/holes/stack-annotations.edn |
| futon2/src/futon2/aif/tripwire_calibration.clj:37 | futon2.aif.tripwire-calibration/read-edn-lines | phase-log | none | dynamic/refused | dynamic path |
| futon3/checks/library_graph_lint.clj:259 | checks.library-graph-lint/read-index-cache | path | or | dynamic/refused | dynamic path |
| futon3/checks/library_graph_lint.clj:320 | checks.library-graph-lint/read-edn-or | baseline | none | dynamic/refused | dynamic path |
| futon3/checks/library_graph_lint.clj:321 | checks.library-graph-lint/read-edn-or | attestations | none | dynamic/refused | dynamic path |
| futon3/checks/library_graph_lint.clj:323 | checks.library-graph-lint/read-edn-or | evidence-records | when | dynamic/refused | dynamic path |
| futon3/checks/spider_fleet.clj:40 | checks.spider-fleet/read-edn | (fs/path dir ".spider/state.edn") | none | dynamic/refused | dynamic path |
| futon3/checks/spider_fleet.clj:42 | checks.spider-fleet/read-edn | %1 | try | dynamic/refused | dynamic path |
| futon3/checks/spider_fleet.clj:43 | checks.spider-fleet/read-edn | (fs/path dir "attestations.edn") | none | dynamic/refused | dynamic path |
| futon3/checks/spider_fleet.clj:44 | checks.spider-fleet/read-edn | (fs/path dir ".spider/absences.edn") | none | dynamic/refused | dynamic path |
| futon3/checks/spider_fleet.clj:45 | checks.spider-fleet/read-edn | (fs/path dir ".spider/seat-failures.edn") | none | dynamic/refused | dynamic path |
| futon3/checks/spider_runner.clj:102 | checks.spider-runner/read-edn | report | none | dynamic/refused | dynamic path |
| futon3/checks/spider_runner.clj:275 | checks.spider-runner/read-edn | path | none | dynamic/refused | dynamic path |
| futon3/checks/spider_runner.clj:295 | checks.spider-runner/read-edn | (str (fs/path (:checkpoints paths) (format "checkpoint-%03d.edn" (dec n)))) | when | dynamic/refused | dynamic path |
| futon3/checks/spider_runner.clj:470 | checks.spider-runner/read-edn | (:state paths) | none | dynamic/refused | dynamic path |
| futon3/checks/spider_runner.clj:486 | checks.spider-runner/process-turn! | paths | none | dynamic/refused | dynamic path |

## Refusals

| helper | file:line | reason |
|---|---|---|
| none | n/a | none |

Positive control: {:files ["futon2/checks/fixtures/absent_is_loud/positive.clj"], :violations 2}
Negative control: {:files ["futon2/checks/fixtures/absent_is_loud/negative.clj"], :violations 0}
