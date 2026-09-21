# Mission/campaign/excursion census — 2026-09-21

642 documents: **71 closed-witnessed**, **45 closed-unwitnessed**, **359 open**, **167 undetermined**. **307 retirement candidates**, for Joe to review; no mission was edited or retired by this census.

## Scope and reproducibility

Canonical repositories only: `futon0`, `futon1`, `futon1a`, `futon1b`, `futon2`, `futon3`, `futon3a`, `futon3b`, `futon3c`, `futon4`, `futon5`, `futon5a`, `futon6`, `futon7`, `futon7a`, `p4ng`. Recursive `holes/**/[MCE]-*.md`; filenames define the document ID (full stem, including companion suffixes). Hidden-directory components, archive-named directories, and symlink files are excluded. Worktree copies are not roots. Frozen mission copies inside canonical run/authority directories remain in scope (the repeated U88 ID below is an example); they are not treated as separate canonical repositories. Untracked files are included if present; absent Git history is explicitly blank. No stores queried.

Run `python3 holes/labs/wm-contract/runs/mission-census-2026-09-21/census.py --root /home/joe/code --self-check` from futon2. Outputs are written beside the script. Standard library only. The cutoff for ages is the UTC calendar date 2026-09-21; Git committer timestamps are retained with their original offset and normalized to UTC dates for age arithmetic, not replaced by filesystem mtimes. Git history follows the current path, not renames. A commit touching a file is evidence of a touch, not necessarily substantive mission progress.

## Classification method and limits

A field parser routes the first recognized status/lifecycle/phase field in the first 80 lines (outside fenced code). CSV retains all such header fields, all explicit lifecycle/phase fields throughout the file, and explicitly labelled owners throughout the file, with source line numbers. Owners are not inferred from authors or committers. Some later owner/phase fields refer to subwork; their text and lines are retained rather than collapsed into a false single authority. Phase information embedded in a status is retained as explicitly labelled status-phase mentions; this does not choose a current phase from a phase history.

Closure words only select documents for inspection. Every initial closure-word route and every additional archived/heading-style terminal route exposed by the original baseline was inspected against its criteria, checklists, scope and closing records. Additional whole-document closure/retirement records found in body searches were inspected separately. Nonterminal headers default to open; missing recognizable headers default to undetermined. This is not a claim that an undetermined document has no deliverable or no status anywhere in its prose.

The inspection heuristic for “all obligations met” is: find the document’s own bounded scope and final criteria; require a nonempty all-met checklist, explicit all-criteria outcome table, or a closing record that explicitly disposes every scoped obligation, plus a named commit/receipt/file. Phase completion, passing tests alone, unchecked acceptance criteria, and a bare operator close are insufficient. Recorded scope exclusions and revised exits are honored when explicit. Retirement instead requires an explicit replacement/stand-down with its reason. No closure is inferred automatically from checkbox totals. Those totals include historical and subphase checklists and are diagnostic only.

An undated archive stamp plus an incompatible active header is undetermined unless a later clear closure resolves it; archive without a reason otherwise routes to closed-unwitnessed. Manual decisions, evidence lines, quotations and source SHA-256 hashes are embedded in census.py. If a reviewed source changes, rerunning returns undetermined until that judgment is refreshed. Newly encountered terminal wording also returns undetermined rather than manufacturing closure. Evidence means the document records an identifiable witness; no tests, Lean proofs, live demos, store receipts or historical commits were independently re-executed. Some witness files may have moved. The classifications assess the documentation, not current runtime fitness.

The required sample check read the **20 shortest documents by UTF-8 file size among the initial 121 closure-word routes in full**, not just regex snippets. This is a purposive, reproducible sample, not a random accuracy estimate. Other routed documents were read at their criteria, closure and relevant evidence sections. The sample found prospective criteria and phase-only completion, which were not counted as witnessed closure.

## Counts by repository

| Repo | closed-witnessed | closed-unwitnessed | open | undetermined | Total |
|---|---:|---:|---:|---:|---:|
| futon0 | 0 | 2 | 17 | 14 | 33 |
| futon1 | 0 | 0 | 0 | 0 | 0 |
| futon1a | 0 | 0 | 0 | 0 | 0 |
| futon1b | 0 | 0 | 2 | 2 | 4 |
| futon2 | 13 | 1 | 72 | 32 | 118 |
| futon3 | 12 | 16 | 15 | 7 | 50 |
| futon3a | 3 | 1 | 2 | 1 | 7 |
| futon3b | 0 | 1 | 0 | 0 | 1 |
| futon3c | 32 | 20 | 134 | 68 | 254 |
| futon4 | 4 | 2 | 18 | 4 | 28 |
| futon5 | 1 | 2 | 14 | 0 | 17 |
| futon5a | 1 | 0 | 25 | 16 | 42 |
| futon6 | 3 | 0 | 31 | 20 | 54 |
| futon7 | 2 | 0 | 29 | 3 | 34 |
| futon7a | 0 | 0 | 0 | 0 | 0 |
| p4ng | 0 | 0 | 0 | 0 | 0 |
| **Total** | 71 | 45 | 359 | 167 | 642 |

## Reconciliation with 642 documents / 493 not marked closed

The inventory and original baseline reproduce **642 / 149 regex-closed / 493 not regex-closed** exactly. Claude supplied the original rule in Agency job invoke-1790025295764-23074-30db54d1: search the first 1,500 characters for the first `(status|lifecycle)` followed by up to six nonword characters and up to 80 non-newline characters; test that value for complete/completed/closed/done/retired/superseded/abandoned/archived. census.py implements the exact regex and census.csv retains both baseline fields. This matches any text, truncates long fields, counts phase completion and negations, and treats archived as closed regardless of reason. Initial closure routing omitted archived and heading-style status fields; the baseline comparison exposed these and they were separately inspected before final classification.

Under the stricter rule, 116 are classified closed, of which only 71 have a recorded closure witness; 359 are open and 167 undetermined. Thus neither 493 nor all non-witnessed documents should be called an open-mission backlog. This is a document census: a companion MAP/ARGUE report may close while its parent remains open.

| Original regex group | closed-witnessed | closed-unwitnessed | open | undetermined | Total |
|---|---:|---:|---:|---:|---:|
| Marked closed | 60 | 44 | 35 | 10 | 149 |
| Not marked closed | 11 | 1 | 324 | 157 | 493 |

## Age of open documents

| Days since last Git touch | Count |
|---|---:|
| <0 (future timestamp) | 0 |
| 0–6 | 15 |
| 7–29 | 37 |
| 30–59 | 41 |
| 60–89 | 89 |
| 90–179 | 168 |
| 180+ | 9 |
| unknown | 0 |

## Retirement candidates — proposals only

Rule: open and untouched in Git for at least 30 days, or a specifically identified named supersession while not already closed. Partial-scope replacements are marked as such. Age alone does not establish abandonment. Closed-unwitnessed and contradictory-status documents require evidence/status repair and are not silently retired.

| Document | Reason |
|---|---|
| `futon0/holes/C-bayesian-structure-learning-family.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon0/holes/E-post-commit-hooks-for-web.md` | Open; last Git touch 2026-08-22 UTC (30 days before census date). |
| `futon0/holes/M-capability-levels.md` | Open; last Git touch 2026-08-17 UTC (35 days before census date). |
| `futon0/holes/M-what-is-it-who-is-it-for.md` | Open; last Git touch 2026-08-19 UTC (33 days before census date). |
| `futon0/holes/excursions/E-aif-daisyworld.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon0/holes/missions/M-apm-capability-ratchet.md` | Open; last Git touch 2026-07-23 UTC (60 days before census date). |
| `futon0/holes/missions/M-capability-zones.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon0/holes/missions/M-futonzero-generative.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon0/holes/missions/M-futonzero-mvp.md` | Open; last Git touch 2026-03-04 UTC (201 days before census date). Named successor M-futonzero-capability: futon0/holes/missions/M-futonzero-capability.md:10 explicitly supersedes this document. |
| `futon0/holes/missions/M-futonzero-prelim-practice.md` | Open; last Git touch 2026-08-17 UTC (35 days before census date). |
| `futon0/holes/missions/M-patterns-done-right.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon0/holes/missions/M-stack-hud-refactor.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon0/holes/missions/M-the-futon-stack-Q6-r12-design-choices.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon0/holes/missions/M-the-futon-stack.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon0/holes/missions/M-usage-hacking.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon1b/holes/M-evidence-landscape-index.md` | Open; last Git touch 2026-08-17 UTC (35 days before census date). |
| `futon1b/holes/M-xtdb-22x-benchmarking.md` | Open; last Git touch 2026-08-06 UTC (46 days before census date). |
| `futon2/holes/C-futon1b-features.md` | Open; last Git touch 2026-07-12 UTC (71 days before census date). |
| `futon2/holes/E-aif-docs-live.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/E-aif-post-mission-mining.md` | Open; last Git touch 2026-07-03 UTC (80 days before census date). |
| `futon2/holes/E-aif2-partB.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon2/holes/E-cascade-sampler-sampler.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon2/holes/E-close-the-loop.md` | Open; last Git touch 2026-07-02 UTC (81 days before census date). |
| `futon2/holes/E-feature-constellation.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/E-fold-embed-pipeline.md` | Open; last Git touch 2026-07-02 UTC (81 days before census date). |
| `futon2/holes/E-futon1a-to-futon1b-migration-pipeline.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/E-futon1b-operational-switchover.md` | Open; last Git touch 2026-07-11 UTC (72 days before census date). |
| `futon2/holes/E-gflownets-fold.md` | Open; last Git touch 2026-07-03 UTC (80 days before census date). |
| `futon2/holes/E-live-loop-1-s2-escrow-design.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/E-live-loop-1.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/E-live-loop-2.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/E-live-loop-3.md` | Open; last Git touch 2026-07-06 UTC (77 days before census date). |
| `futon2/holes/E-mine-mission-transitions.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/E-r1-a-matrix-design.md` | Open; last Git touch 2026-07-04 UTC (79 days before census date). |
| `futon2/holes/E-rollout-kill-test.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/E-zai-agent-upgrades.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/M-a4a-structure-node.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/M-aif-ants-port.md` | Open; last Git touch 2026-07-14 UTC (69 days before census date). |
| `futon2/holes/M-aif-faithfulness.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon2/holes/M-aif-stack.md` | Open; last Git touch 2026-08-02 UTC (50 days before census date). |
| `futon2/holes/M-aif4iad.md` | Open; last Git touch 2026-03-31 UTC (174 days before census date). |
| `futon2/holes/M-arguing-worlds.md` | Open; last Git touch 2026-06-10 UTC (103 days before census date). |
| `futon2/holes/M-composition-aware-reward.md` | Open; last Git touch 2026-07-11 UTC (72 days before census date). |
| `futon2/holes/M-custom-harness.md` | Open; last Git touch 2026-07-04 UTC (79 days before census date). |
| `futon2/holes/M-digital-nomad-patterns.md` | Open; last Git touch 2026-07-27 UTC (56 days before census date). |
| `futon2/holes/M-evaluate-policies.md` | Open; last Git touch 2026-07-04 UTC (79 days before census date). |
| `futon2/holes/M-experiment-build-match.md` | Open; last Git touch 2026-08-02 UTC (50 days before census date). |
| `futon2/holes/M-feature-acceptance-qa-phase2.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon2/holes/M-feature-acceptance-qa.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon2/holes/M-fold-ansatz.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/M-fold-self-play.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/M-joe-reflection.md` | Open; last Git touch 2026-07-12 UTC (71 days before census date). |
| `futon2/holes/M-legacy-sorry-cleanup.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/M-marks-to-labels.md` | Open; last Git touch 2026-07-12 UTC (71 days before census date). |
| `futon2/holes/M-open-learning-system.md` | Open; last Git touch 2026-03-31 UTC (174 days before census date). |
| `futon2/holes/M-operational-vocabulary.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon2/holes/M-patchboard-viz.md` | Open; last Git touch 2026-07-16 UTC (67 days before census date). |
| `futon2/holes/M-pattern-authority-gate.md` | Open; last Git touch 2026-07-16 UTC (67 days before census date). |
| `futon2/holes/M-peradam-mechanization.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon2/holes/M-post-mining-ingest.md` | Open; last Git touch 2026-06-27 UTC (86 days before census date). |
| `futon2/holes/M-strategic-mission-value.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon2/holes/M-text-sidecar.md` | Open; last Git touch 2026-07-11 UTC (72 days before census date). |
| `futon2/holes/M-wm-capability-claim.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon2/holes/M-wm-substrate-1a-to-1b-port.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon2/holes/M-wm-three-factor-mission-value.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon2/holes/missions/M-aif-a-matrix-faithfulness.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon2/holes/missions/M-aif-gap-epistemic-affordance.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon2/holes/missions/M-reflective-discipline.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon2/holes/missions/M-wm-strategic-mission-selection.md` | Open; last Git touch 2026-07-23 UTC (60 days before census date). |
| `futon2/holes/missions/M-wm-tripwires.md` | Open; last Git touch 2026-07-16 UTC (67 days before census date). |
| `futon3/holes/excursions/E-Ttotal.md` | Open; last Git touch 2026-04-27 UTC (147 days before census date). |
| `futon3/holes/excursions/E-cross-prototype-geometry.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3/holes/excursions/E-live-means-live.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3/holes/excursions/E-old-arxiv-block-detection.md` | Open; last Git touch 2026-04-27 UTC (147 days before census date). |
| `futon3/holes/excursions/E-pattern-peripheral.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon3/holes/excursions/E-substrate-2-directed-edge-id.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3/holes/excursions/E-substrate-2-elisp-projection.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3/holes/excursions/E-substrate-metrics.md` | Open; last Git touch 2026-04-27 UTC (147 days before census date). |
| `futon3/holes/missions/M-emacs-cursor-peripheral.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3/holes/missions/M-mission-coherence-patterns.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3/holes/missions/M-pattern-application-diagnostic.md` | Open; last Git touch 2026-04-27 UTC (147 days before census date). |
| `futon3/holes/missions/M-pattern-ingest.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3/holes/missions/M-pattern-mining.md` | Open; last Git touch 2026-04-27 UTC (147 days before census date). |
| `futon3/holes/missions/M-pattern-retrieval-calibration.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon3/holes/missions/M-weird-modernism.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3a/holes/missions/E-fold-engine.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon3a/holes/missions/M-pattern-posteriors.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon3c/holes/E-codex-resume-after-JVM-restart.md` | Open; last Git touch 2026-07-04 UTC (79 days before census date). |
| `futon3c/holes/E-codex-visibility-after-resume.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon3c/holes/E-efe-education.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon3c/holes/E-efe-trustworthy-over-starmap.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon3c/holes/E-possible-world-regulator.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon3c/holes/E-warranted-play.md` | Open; last Git touch 2026-06-08 UTC (105 days before census date). |
| `futon3c/holes/E-zaif-life-findings.md` | Open; last Git touch 2026-07-23 UTC (60 days before census date). |
| `futon3c/holes/campaigns/C-cascade-real.md` | Open; last Git touch 2026-07-01 UTC (82 days before census date). |
| `futon3c/holes/campaigns/C-falsifiable-missions.md` | Open; last Git touch 2026-06-10 UTC (103 days before census date). |
| `futon3c/holes/campaigns/C-substrate-completion.STANDARD-ARGUE.draft.md` | Open; last Git touch 2026-06-01 UTC (112 days before census date). |
| `futon3c/holes/campaigns/C-substrate-completion.md` | Open; last Git touch 2026-06-03 UTC (110 days before census date). |
| `futon3c/holes/campaigns/C-war-machine-campaign.md` | Open; last Git touch 2026-07-06 UTC (77 days before census date). |
| `futon3c/holes/excursions/E-agency-invariants.md` | Open; last Git touch 2026-06-15 UTC (98 days before census date). |
| `futon3c/holes/excursions/E-agency-ws-cutover.md` | Open; last Git touch 2026-07-01 UTC (82 days before census date). |
| `futon3c/holes/excursions/E-apm-drainer.md` | Open; last Git touch 2026-08-22 UTC (30 days before census date). |
| `futon3c/holes/excursions/E-arse-ct-probe.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon3c/holes/excursions/E-arxana-clock.md` | Open; last Git touch 2026-06-26 UTC (87 days before census date). |
| `futon3c/holes/excursions/E-ashby-variety-stratum.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-causal-coupling-top-down.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-clean-up-substrate-2.md` | Open; last Git touch 2026-06-26 UTC (87 days before census date). |
| `futon3c/holes/excursions/E-crossed-bells.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon3c/holes/excursions/E-deep-research-hardening.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon3c/holes/excursions/E-drainer-stall-announced-jobs.md` | Open; last Git touch 2026-08-22 UTC (30 days before census date). |
| `futon3c/holes/excursions/E-dynamic-queries.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-evidence-flow.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon3c/holes/excursions/E-evidence-flows-everywhere.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon3c/holes/excursions/E-first-flights-typed-grounds-tail-closure.md` | Open; last Git touch 2026-07-06 UTC (77 days before census date). |
| `futon3c/holes/excursions/E-futon-memories.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-held-work-ledger.md` | Open; last Git touch 2026-06-30 UTC (83 days before census date). |
| `futon3c/holes/excursions/E-invariant-stepper.md` | Open; last Git touch 2026-07-02 UTC (81 days before census date). |
| `futon3c/holes/excursions/E-lean-to-clojure-arrow.md` | Open; last Git touch 2026-08-15 UTC (37 days before census date). |
| `futon3c/holes/excursions/E-memory-whitepaper-plan.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). Planning scope Experiment 0 superseded by E-memory-whitepaper-v2-plan.md:7; partial-scope candidate only, not retirement of the full doc. |
| `futon3c/holes/excursions/E-memory-whitepaper-v2-plan.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). Executable programme section 4 superseded by E-memory-whitepaper-v2-programme.md:5; findings record explicitly retained, partial-scope review only. |
| `futon3c/holes/excursions/E-pace-layered-tower.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-per-turn-isolation.md` | Open; last Git touch 2026-06-08 UTC (105 days before census date). |
| `futon3c/holes/excursions/E-pipeline-pipecleaner.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon3c/holes/excursions/E-prove-salingaros-cascade-scorer.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon3c/holes/excursions/E-ratchet-probe.md` | Open; last Git touch 2026-08-03 UTC (49 days before census date). |
| `futon3c/holes/excursions/E-repl-continuations.md` | Open; last Git touch 2026-07-01 UTC (82 days before census date). |
| `futon3c/holes/excursions/E-retrieval-flows.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/excursions/E-substrate-2-timetravel.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon3c/holes/excursions/E-turn-dsl.md` | Open; last Git touch 2026-06-08 UTC (105 days before census date). |
| `futon3c/holes/excursions/E-typed-bells.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon3c/holes/excursions/E-unified-live-surface.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon3c/holes/excursions/E-unsolicited-pouch-turns.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon3c/holes/excursions/E-virtual-patterns.md` | Open; last Git touch 2026-07-01 UTC (82 days before census date). |
| `futon3c/holes/excursions/E-vwm.md` | Open; last Git touch 2026-06-24 UTC (89 days before census date). |
| `futon3c/holes/excursions/E-wm-operator-lane.md` | Open; last Git touch 2026-06-05 UTC (108 days before census date). |
| `futon3c/holes/labs/M-diagramprover/E-book-of-why-complete.md` | Open; last Git touch 2026-08-03 UTC (49 days before census date). |
| `futon3c/holes/labs/M-diagramprover/E-frontier-experiments.md` | Open; last Git touch 2026-08-03 UTC (49 days before census date). |
| `futon3c/holes/missions/E-campaign-spec-grounding.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-cheesemonger.md` | Open; last Git touch 2026-05-29 UTC (115 days before census date). |
| `futon3c/holes/missions/E-g-incorporates-deltaT.md` | Open; last Git touch 2026-05-30 UTC (114 days before census date). |
| `futon3c/holes/missions/E-night-shift.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-pattern-mining.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-pilot-contract-compliance.md` | Open; last Git touch 2026-06-15 UTC (98 days before census date). |
| `futon3c/holes/missions/E-pilot-hop-trigger-wiring.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-pilot-vsatarcs-feed.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-r3d-per-entity-attribution.md` | Open; last Git touch 2026-05-30 UTC (114 days before census date). |
| `futon3c/holes/missions/E-storyteller.md` | Open; last Git touch 2026-05-29 UTC (115 days before census date). |
| `futon3c/holes/missions/E-street-sweeper.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-substrate-2-sorry-typing.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-support-coverage.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/E-ticks-firing-ratio-likelihood.md` | Open; last Git touch 2026-05-30 UTC (114 days before census date). |
| `futon3c/holes/missions/E-wm-live-recommendation.md` | Open; last Git touch 2026-05-25 UTC (119 days before census date). |
| `futon3c/holes/missions/E-wm-metric-redesign.md` | Open; last Git touch 2026-05-25 UTC (119 days before census date). |
| `futon3c/holes/missions/E-wm-staleness-meta-stop.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/M-action-cost-modelling.md` | Open; last Git touch 2026-05-30 UTC (114 days before census date). |
| `futon3c/holes/missions/M-agents-queue.md` | Open; last Git touch 2026-06-15 UTC (98 days before census date). |
| `futon3c/holes/missions/M-apm-demonstration.md` | Open; last Git touch 2026-08-20 UTC (32 days before census date). |
| `futon3c/holes/missions/M-apm-solutions.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-archaeology-control.md` | Open; last Git touch 2026-05-01 UTC (143 days before census date). |
| `futon3c/holes/missions/M-autonomous-pattern-lifecycle.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-bounded-disposition.md` | Open; last Git touch 2026-05-01 UTC (143 days before census date). |
| `futon3c/holes/missions/M-bounded-in-flight-state.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon3c/holes/missions/M-chipwitz-corps.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon3c/holes/missions/M-codex-sorry-loop.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/missions/M-cyder.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-diagramprover.md` | Open; last Git touch 2026-08-15 UTC (37 days before census date). |
| `futon3c/holes/missions/M-dionysus-winddown.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon3c/holes/missions/M-federated-agency-hardening.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon3c/holes/missions/M-fulab-logic.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-intent-curvature.md` | Open; last Git touch 2026-06-03 UTC (110 days before census date). |
| `futon3c/holes/missions/M-invariant-queue-extend.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-invariant-queue-unstuck.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-invariant-violations.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-kangaroo.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon3c/holes/missions/M-latex-wysiwyg.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon3c/holes/missions/M-memory-retrieval.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/missions/M-mission-scopes-into-substrate-2.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon3c/holes/missions/M-mission-wiring.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-populate-substrate-2.D3-slice2-design.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon3c/holes/missions/M-populate-substrate-2.md` | Open; last Git touch 2026-06-26 UTC (87 days before census date). |
| `futon3c/holes/missions/M-reachable-from-boot.md` | Open; last Git touch 2026-05-01 UTC (143 days before census date). |
| `futon3c/holes/missions/M-repl-wins-over-cli.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/M-sigils-reconsidered.md` | Open; last Git touch 2026-08-17 UTC (35 days before census date). |
| `futon3c/holes/missions/M-single-entry-point.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon3c/holes/missions/M-single-locus.md` | Open; last Git touch 2026-05-01 UTC (143 days before census date). |
| `futon3c/holes/missions/M-sliding-blackboard.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-smart-emacs-cursor.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon3c/holes/missions/M-stack-inhabitation.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-state-snapshot-witness.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-structural-law.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-typed-holes-lean-handoffs.md` | Open; last Git touch 2026-06-14 UTC (99 days before census date). |
| `futon3c/holes/missions/M-typed-holes-mathlib-handoff.md` | Open; last Git touch 2026-06-14 UTC (99 days before census date). |
| `futon3c/holes/missions/M-typed-memories.md` | Open; last Git touch 2026-07-23 UTC (60 days before census date). |
| `futon3c/holes/missions/M-war-machine-pilot.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon3c/holes/missions/M-war-machine-tuning.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon3c/holes/missions/M-war-machine.md` | Open; last Git touch 2026-05-04 UTC (140 days before census date). |
| `futon3c/holes/missions/M-xenotype-its.md` | Open; last Git touch 2026-08-01 UTC (51 days before census date). |
| `futon3c/holes/missions/M-zai-learning-loop.md` | Open; last Git touch 2026-07-26 UTC (57 days before census date). |
| `futon3c/holes/qa/M-forum-refactor-qa.md` | Open; last Git touch 2026-02-10 UTC (223 days before census date). |
| `futon4/holes/excursions/E-arxana-workflow-management.md` | Open; last Git touch 2026-08-17 UTC (35 days before census date). |
| `futon4/holes/missions/M-arxana-roundtrip.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon4/holes/missions/M-editorial-assistant.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-essay-corpus-substrate.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-essays-diachronic-model.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-essays-edit-cycle.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-essays-retraction-visibility.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-futon-enrichment.md` | Open; last Git touch 2026-03-07 UTC (198 days before census date). |
| `futon4/holes/missions/M-or-training-as-learning-system.v1.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-peeragogy-rewrite.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon4/holes/missions/M-vsatarcs-invariants-integration.md` | Open; last Git touch 2026-06-05 UTC (108 days before census date). |
| `futon4/holes/missions/M-vsatarcs-invariants-integration.witness-vsatarcs-fresh.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon4/holes/missions/M-vsatarcs-writer.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-war-machine-vsatarcs-interop.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon4/holes/missions/M-web-arxana-missions.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon4/holes/missions/M-web-arxana-ui-improvements.md` | Open; last Git touch 2026-05-30 UTC (114 days before census date). |
| `futon4/holes/missions/M-webarxana.md` | Open; last Git touch 2026-04-14 UTC (160 days before census date). |
| `futon5/holes/M-formal-patterns.md` | Open; last Git touch 2026-08-04 UTC (48 days before census date). |
| `futon5/holes/missions/M-aif-tokamak.md` | Open; last Git touch 2026-07-15 UTC (68 days before census date). |
| `futon5/holes/missions/M-coupling-as-constraint.md` | Open; last Git touch 2026-02-18 UTC (215 days before census date). |
| `futon5/holes/missions/M-differentiable-code.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon5/holes/missions/M-exotype-xenotype-eoc.md` | Open; last Git touch 2026-08-03 UTC (49 days before census date). |
| `futon5/holes/missions/M-fulab-wiring-survey.md` | Open; last Git touch 2026-02-18 UTC (215 days before census date). |
| `futon5/holes/missions/M-lab-standard.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon5/holes/missions/M-metaca-search.md` | Open; last Git touch 2026-07-14 UTC (69 days before census date). |
| `futon5/holes/missions/M-propagators.md` | Open; last Git touch 2026-07-16 UTC (67 days before census date). |
| `futon5/holes/missions/M-sci-reproduction-replay-ledger.md` | Open; last Git touch 2026-07-14 UTC (69 days before census date). |
| `futon5/holes/missions/M-sci-reproduction.md` | Open; last Git touch 2026-07-16 UTC (67 days before census date). |
| `futon5/holes/missions/M-tpg-coupling-evolution.md` | Open; last Git touch 2026-02-18 UTC (215 days before census date). |
| `futon5/holes/missions/M-xor-coupling-probe.md` | Open; last Git touch 2026-02-18 UTC (215 days before census date). |
| `futon5a/holes/excursions/E-btoa-candidate-queue-reweighting-v0.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/excursions/E-btoa-evidence-sample-mapping-v0.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/excursions/E-candidate-queue-xtdb-shape-v0.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/excursions/E-exotype-ct-grounding.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon5a/holes/excursions/E-fix-cx-cr-runners-to-clock-in.md` | Open; last Git touch 2026-06-03 UTC (110 days before census date). |
| `futon5a/holes/excursions/E-half-mil-audit.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon5a/holes/excursions/E-interest-mining.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon5a/holes/excursions/E-jax-demonstrators.md` | Open; last Git touch 2026-06-03 UTC (110 days before census date). |
| `futon5a/holes/excursions/E-the-dark-tower.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon5a/holes/missions/E-EOI-supervision.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon5a/holes/missions/E-trip-journal-hit.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/missions/M-eoi-outbox-management.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon5a/holes/missions/M-landing-practice.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/missions/M-learning-loop.md` | Open; last Git touch 2026-07-22 UTC (61 days before census date). |
| `futon5a/holes/missions/M-recommendation-bindings.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/missions/M-self-improvement-loop.md` | Open; last Git touch 2026-02-25 UTC (208 days before census date). |
| `futon5a/holes/missions/M-stack-stereolithography.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/missions/M-trip-journal.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon5a/holes/missions/M-war-machine-wiring.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon6/holes/E-cold-storage.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon6/holes/E-ground-G.md` | Open; last Git touch 2026-06-10 UTC (103 days before census date). |
| `futon6/holes/E-patch-agent-evidence-leaks.md` | Open; last Git touch 2026-07-10 UTC (73 days before census date). |
| `futon6/holes/excursions/E-iatc-expository-alignment.md` | Open; last Git touch 2026-06-17 UTC (96 days before census date). |
| `futon6/holes/excursions/E-iatc-model.md` | Open; last Git touch 2026-06-18 UTC (95 days before census date). |
| `futon6/holes/excursions/E-mfuton-silver.md` | Open; last Git touch 2026-04-28 UTC (146 days before census date). |
| `futon6/holes/excursions/E-mission-mining.md` | Open; last Git touch 2026-06-13 UTC (100 days before census date). |
| `futon6/holes/excursions/E-sanitize-invalid-EDN.md` | Open; last Git touch 2026-06-18 UTC (95 days before census date). |
| `futon6/holes/excursions/E-scopes-math-to-code.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon6/holes/excursions/E-superpod-ct-nlp-intrinsic-eval.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon6/holes/excursions/E-superpod-mark3.md` | Open; last Git touch 2026-06-18 UTC (95 days before census date). |
| `futon6/holes/missions/E-anatomy-of-a-proof.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon6/holes/missions/E-mealy-style-transducer.md` | Open; last Git touch 2026-06-11 UTC (102 days before census date). |
| `futon6/holes/missions/E-mission-head.md` | Open; last Git touch 2026-06-10 UTC (103 days before census date). |
| `futon6/holes/missions/M-artificial-stack-exchange.md` | Open; last Git touch 2026-02-27 UTC (206 days before census date). |
| `futon6/holes/missions/M-canon-fingerprint-store.md` | Open; last Git touch 2026-06-08 UTC (105 days before census date). |
| `futon6/holes/missions/M-differentiable-math.md` | Open; last Git touch 2026-06-01 UTC (112 days before census date). |
| `futon6/holes/missions/M-differentiable-substrate.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon6/holes/missions/M-distributed-proofreaders.md` | Open; last Git touch 2026-06-13 UTC (100 days before census date). |
| `futon6/holes/missions/M-efe-bge-followon-actions.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon6/holes/missions/M-hyperreal-dictionary-planning.md` | Open; last Git touch 2026-05-03 UTC (141 days before census date). |
| `futon6/holes/missions/M-live-efe-map.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon6/holes/missions/M-metric-harness.md` | Open; last Git touch 2026-06-23 UTC (90 days before census date). |
| `futon6/holes/missions/M-paper-reverse-morphogenesis.md` | Open; last Git touch 2026-04-15 UTC (159 days before census date). |
| `futon6/holes/missions/M-pheromone-field.md` | Open; last Git touch 2026-07-05 UTC (78 days before census date). |
| `futon6/holes/missions/M-prior-mathematics.md` | Open; last Git touch 2026-05-31 UTC (113 days before census date). |
| `futon6/holes/missions/M-structure-seed-promotion.md` | Open; last Git touch 2026-05-21 UTC (123 days before census date). |
| `futon6/holes/missions/M-superpod-mark2.md` | Open; last Git touch 2026-05-21 UTC (123 days before census date). |
| `futon6/holes/missions/M-symbol-grounding-scaling-plan.md` | Open; last Git touch 2026-05-23 UTC (121 days before census date). |
| `futon6/holes/missions/M-symbol-grounding.md` | Open; last Git touch 2026-06-18 UTC (95 days before census date). |
| `futon7/holes/C-greenlit-vsat-features.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon7/holes/C-pudding-prover.md` | Open; last Git touch 2026-07-06 UTC (77 days before census date). |
| `futon7/holes/E-A-B-C-career-mode-economics.md` | Open; last Git touch 2026-05-27 UTC (117 days before census date). |
| `futon7/holes/E-business-exotype-audit.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon7/holes/E-tonic-osint.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon7/holes/M-autonomous-doc-maintenance.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon7/holes/M-buyer-discovery.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon7/holes/M-cold-chain.cadence-consumption-design.md` | Open; last Git touch 2026-06-16 UTC (97 days before census date). |
| `futon7/holes/M-cold-chain.kit-outbox-design.md` | Open; last Git touch 2026-06-16 UTC (97 days before census date). |
| `futon7/holes/M-cold-chain.md` | Open; last Git touch 2026-06-16 UTC (97 days before census date). |
| `futon7/holes/M-daily-scan-multi-axis-queue.md` | Open; last Git touch 2026-05-19 UTC (125 days before census date). |
| `futon7/holes/M-demonstration-foundry.F1-removal-spec.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon7/holes/M-demonstration-foundry.md` | Open; last Git touch 2026-07-13 UTC (70 days before census date). |
| `futon7/holes/M-descriptive-essay-of-the-stack.md` | Open; last Git touch 2026-05-17 UTC (127 days before census date). |
| `futon7/holes/M-futon-forward-model.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon7/holes/M-interim-director-long-tail.md` | Open; last Git touch 2026-05-18 UTC (126 days before census date). |
| `futon7/holes/M-interim-director-proxy-metric-inventory.md` | Open; last Git touch 2026-05-20 UTC (124 days before census date). |
| `futon7/holes/M-interim-director.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon7/holes/M-peradam-grounding.md` | Open; last Git touch 2026-06-10 UTC (103 days before census date). |
| `futon7/holes/M-pudding-peradams.md` | Open; last Git touch 2026-06-09 UTC (104 days before census date). |
| `futon7/holes/M-signal-roll-up.md` | Open; last Git touch 2026-05-19 UTC (125 days before census date). |
| `futon7/holes/M-stack-essay-code-alignment.md` | Open; last Git touch 2026-05-17 UTC (127 days before census date). |
| `futon7/holes/M-stack-morphogenetic-rewrite.md` | Open; last Git touch 2026-05-17 UTC (127 days before census date). |
| `futon7/holes/M-war-machine-aif-completion.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon7/holes/M-war-machine-aif-last-mile.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon7/holes/M-war-machine-frontend-upgrade1.md` | Open; last Git touch 2026-05-26 UTC (118 days before census date). |
| `futon7/holes/missions/M-becoming-nomad.md` | Open; last Git touch 2026-08-14 UTC (38 days before census date). |
| `futon7/holes/missions/M-daily-scan.md` | Open; last Git touch 2026-06-12 UTC (101 days before census date). |
| `futon7/holes/missions/M-value-creation-loop.md` | Open; last Git touch 2026-07-06 UTC (77 days before census date). |

## Duplicate IDs across repositories

2 filename IDs occur in more than one repository. No deduplication was applied. Same-repo companion reports retain distinct full-stem IDs.

| ID | Paths |
|---|---|
| `M-coordination-rewrite` | futon3/holes/missions/M-coordination-rewrite.md \| futon3b/holes/missions/M-coordination-rewrite.md |
| `M-u88-contextual-preferences` | futon2/holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/draft-missions/M-u88-contextual-preferences.md \| futon2/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-U88-codex20-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-U88-codex20-v2-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-U88-zai-successor-2026-09-12/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-U88-zai-successor-2026-09-12-v4/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-initialization-close-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-ea1-admission-2026-09-12/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-ea1-admission-2026-09-12-v2/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-initialization-collision-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-initialization38690-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-pinned-selection-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-successor-v2-selection-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair-successor-v2-selection-zai-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair057-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md \| futon3c/holes/labs/wm-contract/runs/RUN4-repair058-admission-2026-09-11/authority/holes/missions/M-u88-contextual-preferences.md |

## Full-read sample (20)

| Document | Classification | Evidence / decision |
|---|---|---|
| `futon5/holes/missions/M-diagram-composition.md` | closed-witnessed | L10: Declared multi-diagram validation delivered; all eight checks pass standalone and composed, with src/futon5/ct/mission.clj and data/missions inputs named. |
| `futon5/holes/missions/M-pattern-exotype-bridge.md` | closed-unwitnessed | L9: Complete banner plus measured prototype and file list; broad obligation that all 791 patterns be executable in any domain is not marked discharged. |
| `futon5/holes/missions/M-sci-detection-pipeline.md` | closed-unwitnessed | L9: Complete banner, component summary and accuracy result; no explicit all-obligations-met record or retirement reason. |
| `futon5/holes/missions/M-tpg-coupling-evolution.md` | open | L4: MAP with completed production run; next evolution run is still described at 19–21. |
| `futon3c/holes/missions/M-war-machine-first-outing-expectations.md` | closed-unwitnessed | L3: Completed retrospective narrative, but no explicit closure criteria/discharge ledger or source commit/receipt binding the scored run; not counted closed merely because a report exists. |
| `futon3c/holes/missions/M-alfworld-pattern-discovery.md` | closed-witnessed | L15: The sole completion criterion is ten flexiarg patterns; header records 10/10 written and commit 2713661. |
| `futon3c/holes/missions/M-substrate-metric.R2-curvature-full-report.md` | closed-witnessed | L11: Full-current E1 report supplies output JSON command, all 577 bridge computations and final verdict, satisfying the bounded report scope. |
| `futon3/holes/missions/M-understand-fucodex.md` | closed-witnessed | L3: Explicit SUPERSEDED disposition: unified dispatch/peripheral model resolves the old bridge architecture question; retirement branch. |
| `futon3c/holes/missions/M-substrate-metric.OR-sample.md` | closed-witnessed | L11: Bounded sample report supplies its exact script command, eight computed edge results and verdict; next scaling step is outside this sample's stated scope. |
| `futon3c/holes/excursions/E-scope-organism-copar.md` | closed-witnessed | L4: The sole renderer fidelity gap is recorded implemented with clean_to_lean.py and domain-copar output; regression records 26 proofs, zero sorry, byte-identical default path. |
| `futon3c/holes/missions/M-substrate-metric.R2-curvature-report.md` | closed-witnessed | L11: 200-edge sample report records script/output JSON, timing and final R2 verdict for the stated bounded sample. |
| `futon3c/holes/missions/M-typed-holes-example-scope-query.md` | closed-witnessed | L13: Worked-example scope is exercised with scope_query_dogfood.py, named golden graph and four explicit query/answer outputs; full-corpus extension is distinguished from this dogfood. |
| `futon3c/holes/excursions/E-first-flights-transferred-work.md` | closed-witnessed | L42: W1 independent closure receipt b88a81f plus W2 later interactive closure with flight.spec.edn and flight-typed-ground-witness.edn; both bounded transferred obligations disposed. |
| `futon3c/holes/excursions/E-shutdown-agents-killed-the-pools.md` | closed-witnessed | L52: Both closing changes and the actual destructive-state throwaway-JVM verification are recorded, with src/repl/http.clj as the implementation pointer. |
| `futon2/holes/E-evaluate-policies-spikes.md` | closed-witnessed | L21: Both chartered spikes explicitly DONE; regenerated argue-exhibit.pdf and spike-spectral/spectral-comparison.json supply the two output pointers. |
| `futon2/holes/M-peradam-mechanization.md` | open | L3: P1–P3 machinery landed dark, but header awaits two rulings and E1 reward seam remains in the completion criteria. |
| `futon3c/holes/missions/M-operational-readiness.md` | closed-witnessed | L35: Eight issue rows all done with commit/receipt pointers; four gates marked met and checkpoint evidence records API, tests, evidence and unattended loop. |
| `futon3c/holes/missions/M-typed-holes-lean-wave2-design.md` | closed-unwitnessed | L54: Design note says complete but its explicit T5/T6 acceptance clauses remain prospective compile/example/lemma requirements; no discharge record for those clauses in this doc. |
| `futon3/holes/missions/M-make-agency-work-properly.md` | closed-witnessed | L3: Explicit SUPERSEDED disposition with reason: identifier separation and runner defaults moved to futon3c agency; retirement branch. |
| `futon3/holes/missions/M-drawbridge-multi-agent.md` | closed-witnessed | L3: Explicit SUPERSEDED disposition: shared N:1 registry replaces the per-agent JVM design; retirement branch, not a claim that the old checklist shipped. |

## Review queue and validation

177 source-pinned manual judgments; 0 changed reviewed sources; 0 newly routed terminal headers. CSV supplies the evidence and source hash for every row.

Self-checks enforce one row and one group per path, exact source/quotation agreement for reviewed files, terminal-language refusal without manual review, and actual-source regression cases: M-agency-unified-routing (unchecked success criteria), M-portfolio-inference (weekly heartbeat exit still open), M-IRC-stability (witnessed checklists), and M-P7-rational-reconstruction (contradictory statuses). The initial snapshot also matched every source SHA/quotation and 20 Git-log date/hash spot-checks selected with seed 20260921. Markdown/Python only: clj-kondo and Lisp check-parens are not applicable.
