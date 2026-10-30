# Giraffe run

As of: 2026-10-28
Open items: 147
Ready: 140
Ready ignoring externals: 140

Edge sources and counts:
- prose-reading: 41

Rejects by reason:
- rejected-at-reading: 243
- duplicate-id: 2
- no-status-line: 10

## Order of work

The source's own order (tickets index order of open items) beside
Giraffe's order (ready first by unblocks, cone, source order).

| Source order | status | reason | cone | unblocks | Giraffe order | status | reason | cone | unblocks |
|---|---|---|---|---|---|---|---|---|---|
| M-apm-capability-ratchet | not-ready | unmet | 0 | 0  | M-repl-wins-over-cli | ready | ready-by-unblocks | 4 | 2 |
| M-futon-problems | ready | none-declared | 0 | 0  | M-futonzero-prelim-practice | ready | ready-by-unblocks | 1 | 1 |
| M-futonzero-generative | ready | none-declared | 1 | 0  | M-reachable-from-boot | ready | ready-by-unblocks | 1 | 1 |
| M-futonzero-mvp | ready | all-met | 0 | 0  | M-pattern-mining | ready | ready-by-cone | 5 | 0 |
| M-futonzero-prelim-practice | ready | all-met | 1 | 1  | M-superpod-mark2 | ready | ready-by-cone | 4 | 0 |
| M-joe-told-me-about-futon | ready | none-declared | 0 | 0  | M-autoclock-in | ready | ready-by-cone | 3 | 0 |
| M-landscape-positioning | ready | none-declared | 0 | 0  | M-hyperreal-dictionary-planning | ready | ready-by-cone | 3 | 0 |
| M-patterns-done-right | ready | all-met | 0 | 0  | M-象-2000 | ready | ready-by-cone | 2 | 0 |
| M-stack-hud-refactor | ready | none-declared | 0 | 0  | M-essay-corpus-substrate | ready | ready-by-cone | 2 | 0 |
| M-the-futon-stack-Q6-r12-design-choices | ready | none-declared | 0 | 0  | M-trip-journal | ready | ready-by-cone | 2 | 0 |
| M-the-futon-stack | ready | none-declared | 1 | 0  | M-futonzero-generative | ready | ready-by-cone | 1 | 0 |
| M-usage-hacking | ready | none-declared | 0 | 0  | M-the-futon-stack | ready | ready-by-cone | 1 | 0 |
| M-G-wm-wiring | ready | none-declared | 0 | 0  | M-aif-policy-conditioned-eig | ready | ready-by-cone | 1 | 0 |
| M-aif-a-matrix-faithfulness | ready | none-declared | 0 | 0  | M-pattern-retrieval-calibration | ready | ready-by-cone | 1 | 0 |
| M-aif-gap-epistemic-affordance | ready | all-met | 0 | 0  | M-action-cost-modelling | ready | ready-by-cone | 1 | 0 |
| M-aif-policy-conditioned-eig | ready | none-declared | 1 | 0  | M-apm-solutions | ready | ready-by-cone | 1 | 0 |
| M-f11-find-production-successor | ready | none-declared | 0 | 0  | M-codex-sorry-loop | ready | ready-by-cone | 1 | 0 |
| M-formal-war-machine | ready | none-declared | 0 | 0  | M-invariant-queue-extend | ready | ready-by-cone | 1 | 0 |
| M-reflective-discipline | ready | none-declared | 0 | 0  | M-mission-scopes-into-substrate-2 | ready | ready-by-cone | 1 | 0 |
| M-run-produces-its-own-brief | ready | none-declared | 0 | 0  | M-shared-memory-control-build-test | ready | ready-by-cone | 1 | 0 |
| M-u88-contextual-preferences | ready | none-declared | 0 | 0  | M-the-perfect-crime | ready | ready-by-cone | 1 | 0 |
| M-wm-aif-policy-grain-compliance | ready | none-declared | 0 | 0  | M-tpg-coupling-evolution | ready | ready-by-cone | 1 | 0 |
| M-wm-strategic-mission-selection | ready | all-met | 0 | 0  | M-xor-coupling-probe | ready | ready-by-cone | 1 | 0 |
| M-wm-tripwires | ready | none-declared | 0 | 0  | M-expressions-of-interest | ready | ready-by-cone | 1 | 0 |
| M-zaif-harness-v1 | ready | none-declared | 0 | 0  | M-hypergraph-operator | ready | ready-by-cone | 1 | 0 |
| M-emacs-cursor-peripheral | ready | none-declared | 0 | 0  | M-prior-mathematics | ready | ready-by-cone | 1 | 0 |
| M-mission-coherence-patterns | ready | none-declared | 0 | 0  | M-futon-problems | ready | ready-by-source-order | 0 | 0 |
| M-pattern-application-diagnostic | undetermined | cyclic | 9 | 0  | M-futonzero-mvp | ready | ready-by-source-order | 0 | 0 |
| M-pattern-ingest | ready | none-declared | 0 | 0  | M-joe-told-me-about-futon | ready | ready-by-source-order | 0 | 0 |
| M-pattern-mining | ready | none-declared | 5 | 0  | M-landscape-positioning | ready | ready-by-source-order | 0 | 0 |
| M-pattern-retrieval-calibration | ready | all-met | 1 | 0  | M-patterns-done-right | ready | ready-by-source-order | 0 | 0 |
| M-weird-modernism | ready | none-declared | 0 | 0  | M-stack-hud-refactor | ready | ready-by-source-order | 0 | 0 |
| M-pattern-posteriors | ready | none-declared | 0 | 0  | M-the-futon-stack-Q6-r12-design-choices | ready | ready-by-source-order | 0 | 0 |
| M-action-cost-modelling | ready | none-declared | 1 | 0  | M-usage-hacking | ready | ready-by-source-order | 0 | 0 |
| M-agents-queue | ready | none-declared | 0 | 0  | M-G-wm-wiring | ready | ready-by-source-order | 0 | 0 |
| M-aif-over-the-operator | ready | none-declared | 0 | 0  | M-aif-a-matrix-faithfulness | ready | ready-by-source-order | 0 | 0 |
| M-apm-demonstration | ready | none-declared | 0 | 0  | M-aif-gap-epistemic-affordance | ready | ready-by-source-order | 0 | 0 |
| M-apm-solutions | ready | none-declared | 1 | 0  | M-f11-find-production-successor | ready | ready-by-source-order | 0 | 0 |
| M-autoclock-in | ready | none-declared | 3 | 0  | M-formal-war-machine | ready | ready-by-source-order | 0 | 0 |
| M-autonomous-pattern-lifecycle | ready | none-declared | 0 | 0  | M-reflective-discipline | ready | ready-by-source-order | 0 | 0 |
| M-bounded-disposition | ready | all-met | 0 | 0  | M-run-produces-its-own-brief | ready | ready-by-source-order | 0 | 0 |
| M-case-studies | ready | none-declared | 0 | 0  | M-u88-contextual-preferences | ready | ready-by-source-order | 0 | 0 |
| M-chipwitz-corps | ready | all-met | 0 | 0  | M-wm-aif-policy-grain-compliance | ready | ready-by-source-order | 0 | 0 |
| M-codex-sorry-loop | ready | none-declared | 1 | 0  | M-wm-strategic-mission-selection | ready | ready-by-source-order | 0 | 0 |
| M-cyder | ready | none-declared | 0 | 0  | M-wm-tripwires | ready | ready-by-source-order | 0 | 0 |
| M-diagramprover | ready | all-met | 0 | 0  | M-zaif-harness-v1 | ready | ready-by-source-order | 0 | 0 |
| M-dionysus-winddown | ready | none-declared | 0 | 0  | M-emacs-cursor-peripheral | ready | ready-by-source-order | 0 | 0 |
| M-federated-agency-hardening | ready | none-declared | 0 | 0  | M-mission-coherence-patterns | ready | ready-by-source-order | 0 | 0 |
| M-fulab-logic | ready | none-declared | 0 | 0  | M-pattern-ingest | ready | ready-by-source-order | 0 | 0 |
| M-giraffe | ready | none-declared | 0 | 0  | M-weird-modernism | ready | ready-by-source-order | 0 | 0 |
| M-inbox-zero-claim-lifecycle | ready | none-declared | 0 | 0  | M-pattern-posteriors | ready | ready-by-source-order | 0 | 0 |
| M-intent-curvature | ready | none-declared | 0 | 0  | M-agents-queue | ready | ready-by-source-order | 0 | 0 |
| M-invariant-queue-extend | ready | alternative-only | 1 | 0  | M-aif-over-the-operator | ready | ready-by-source-order | 0 | 0 |
| M-invariant-violations | ready | none-declared | 0 | 0  | M-apm-demonstration | ready | ready-by-source-order | 0 | 0 |
| M-kangaroo | ready | none-declared | 0 | 0  | M-autonomous-pattern-lifecycle | ready | ready-by-source-order | 0 | 0 |
| M-latex-wysiwyg | ready | none-declared | 0 | 0  | M-bounded-disposition | ready | ready-by-source-order | 0 | 0 |
| M-memory-retrieval | ready | none-declared | 0 | 0  | M-case-studies | ready | ready-by-source-order | 0 | 0 |
| M-mission-scopes-into-substrate-2 | ready | none-declared | 1 | 0  | M-chipwitz-corps | ready | ready-by-source-order | 0 | 0 |
| M-mission-wiring | ready | none-declared | 0 | 0  | M-cyder | ready | ready-by-source-order | 0 | 0 |
| M-omni-wm-runner | ready | none-declared | 0 | 0  | M-diagramprover | ready | ready-by-source-order | 0 | 0 |
| M-populate-substrate-2 | ready | none-declared | 0 | 0  | M-dionysus-winddown | ready | ready-by-source-order | 0 | 0 |
| M-reachable-from-boot | ready | none-declared | 1 | 1  | M-federated-agency-hardening | ready | ready-by-source-order | 0 | 0 |
| M-repl-wins-over-cli | ready | none-declared | 4 | 2  | M-fulab-logic | ready | ready-by-source-order | 0 | 0 |
| M-shared-memory-control-build-test | ready | none-declared | 1 | 0  | M-giraffe | ready | ready-by-source-order | 0 | 0 |
| M-sigils-reconsidered | ready | none-declared | 0 | 0  | M-inbox-zero-claim-lifecycle | ready | ready-by-source-order | 0 | 0 |
| M-single-entry-point | ready | none-declared | 0 | 0  | M-intent-curvature | ready | ready-by-source-order | 0 | 0 |
| M-sliding-blackboard | ready | none-declared | 0 | 0  | M-invariant-violations | ready | ready-by-source-order | 0 | 0 |
| M-smart-emacs-cursor | ready | none-declared | 0 | 0  | M-kangaroo | ready | ready-by-source-order | 0 | 0 |
| M-stack-inhabitation | not-ready | unmet | 0 | 0  | M-latex-wysiwyg | ready | ready-by-source-order | 0 | 0 |
| M-state-snapshot-witness | not-ready | unmet | 0 | 0  | M-memory-retrieval | ready | ready-by-source-order | 0 | 0 |
| M-the-perfect-crime | ready | all-met | 1 | 0  | M-mission-wiring | ready | ready-by-source-order | 0 | 0 |
| M-turns-first | ready | none-declared | 0 | 0  | M-omni-wm-runner | ready | ready-by-source-order | 0 | 0 |
| M-typed-holes-mathlib-handoff | ready | none-declared | 0 | 0  | M-populate-substrate-2 | ready | ready-by-source-order | 0 | 0 |
| M-typed-memories | ready | none-declared | 0 | 0  | M-sigils-reconsidered | ready | ready-by-source-order | 0 | 0 |
| M-war-machine-pilot | ready | all-met | 0 | 0  | M-single-entry-point | ready | ready-by-source-order | 0 | 0 |
| M-war-machine-tuning | ready | none-declared | 0 | 0  | M-sliding-blackboard | ready | ready-by-source-order | 0 | 0 |
| M-war-machine | ready | none-declared | 0 | 0  | M-smart-emacs-cursor | ready | ready-by-source-order | 0 | 0 |
| M-warrant-limit | ready | none-declared | 0 | 0  | M-turns-first | ready | ready-by-source-order | 0 | 0 |
| M-wm-wiring | ready | all-met | 0 | 0  | M-typed-holes-mathlib-handoff | ready | ready-by-source-order | 0 | 0 |
| M-xenotype-its | ready | all-met | 0 | 0  | M-typed-memories | ready | ready-by-source-order | 0 | 0 |
| M-zai-learning-loop | ready | none-declared | 0 | 0  | M-war-machine-pilot | ready | ready-by-source-order | 0 | 0 |
| M-象-2000 | ready | all-met | 2 | 0  | M-war-machine-tuning | ready | ready-by-source-order | 0 | 0 |
| M-arxana-hardening | ready | none-declared | 0 | 0  | M-war-machine | ready | ready-by-source-order | 0 | 0 |
| M-arxana-roundtrip | ready | none-declared | 0 | 0  | M-warrant-limit | ready | ready-by-source-order | 0 | 0 |
| M-editorial-assistant | ready | all-met | 0 | 0  | M-wm-wiring | ready | ready-by-source-order | 0 | 0 |
| M-essay-corpus-substrate | ready | none-declared | 2 | 0  | M-xenotype-its | ready | ready-by-source-order | 0 | 0 |
| M-essays-diachronic-model | ready | none-declared | 0 | 0  | M-zai-learning-loop | ready | ready-by-source-order | 0 | 0 |
| M-essays-edit-cycle | ready | all-met | 0 | 0  | M-arxana-hardening | ready | ready-by-source-order | 0 | 0 |
| M-essays-retraction-visibility | ready | none-declared | 0 | 0  | M-arxana-roundtrip | ready | ready-by-source-order | 0 | 0 |
| M-futon-enrichment | ready | none-declared | 0 | 0  | M-editorial-assistant | ready | ready-by-source-order | 0 | 0 |
| M-librarian | ready | none-declared | 0 | 0  | M-essays-diachronic-model | ready | ready-by-source-order | 0 | 0 |
| M-or-training-as-learning-system | ready | none-declared | 0 | 0  | M-essays-edit-cycle | ready | ready-by-source-order | 0 | 0 |
| M-or-training-as-learning-system.v1 | ready | none-declared | 0 | 0  | M-essays-retraction-visibility | ready | ready-by-source-order | 0 | 0 |
| M-peeragogy-rewrite | ready | none-declared | 0 | 0  | M-futon-enrichment | ready | ready-by-source-order | 0 | 0 |
| M-simulating-or-training-as-learning-system | ready | none-declared | 0 | 0  | M-librarian | ready | ready-by-source-order | 0 | 0 |
| M-vsatarcs-invariants-integration | ready | none-declared | 0 | 0  | M-or-training-as-learning-system | ready | ready-by-source-order | 0 | 0 |
| M-vsatarcs-invariants-integration.witness-vsatarcs-fresh | ready | none-declared | 0 | 0  | M-or-training-as-learning-system.v1 | ready | ready-by-source-order | 0 | 0 |
| M-vsatarcs-writer | ready | none-declared | 0 | 0  | M-peeragogy-rewrite | ready | ready-by-source-order | 0 | 0 |
| M-war-machine-vsatarcs-interop | ready | none-declared | 0 | 0  | M-simulating-or-training-as-learning-system | ready | ready-by-source-order | 0 | 0 |
| M-web-arxana-missions | ready | all-met | 0 | 0  | M-vsatarcs-invariants-integration | ready | ready-by-source-order | 0 | 0 |
| M-web-arxana-ui-improvements | ready | none-declared | 0 | 0  | M-vsatarcs-invariants-integration.witness-vsatarcs-fresh | ready | ready-by-source-order | 0 | 0 |
| M-webarxana | ready | none-declared | 0 | 0  | M-vsatarcs-writer | ready | ready-by-source-order | 0 | 0 |
| M-aif-tokamak | ready | none-declared | 0 | 0  | M-war-machine-vsatarcs-interop | ready | ready-by-source-order | 0 | 0 |
| M-categorical-code | ready | none-declared | 0 | 0  | M-web-arxana-missions | ready | ready-by-source-order | 0 | 0 |
| M-coupling-as-constraint | not-ready | unmet | 0 | 0  | M-web-arxana-ui-improvements | ready | ready-by-source-order | 0 | 0 |
| M-differentiable-code | ready | none-declared | 0 | 0  | M-webarxana | ready | ready-by-source-order | 0 | 0 |
| M-exotype-xenotype-eoc | ready | none-declared | 0 | 0  | M-aif-tokamak | ready | ready-by-source-order | 0 | 0 |
| M-fulab-wiring-survey | ready | none-declared | 0 | 0  | M-categorical-code | ready | ready-by-source-order | 0 | 0 |
| M-lab-standard | ready | none-declared | 0 | 0  | M-differentiable-code | ready | ready-by-source-order | 0 | 0 |
| M-metaca-search | ready | none-declared | 0 | 0  | M-exotype-xenotype-eoc | ready | ready-by-source-order | 0 | 0 |
| M-propagators | ready | none-declared | 0 | 0  | M-fulab-wiring-survey | ready | ready-by-source-order | 0 | 0 |
| M-sci-reproduction-replay-ledger | ready | none-declared | 0 | 0  | M-lab-standard | ready | ready-by-source-order | 0 | 0 |
| M-tpg-coupling-evolution | ready | none-declared | 1 | 0  | M-metaca-search | ready | ready-by-source-order | 0 | 0 |
| M-xor-coupling-probe | ready | none-declared | 1 | 0  | M-propagators | ready | ready-by-source-order | 0 | 0 |
| M-a-sorry-enterprise | not-ready | unmet | 0 | 0  | M-sci-reproduction-replay-ledger | ready | ready-by-source-order | 0 | 0 |
| M-eoi-outbox-management | ready | all-met | 0 | 0  | M-eoi-outbox-management | ready | ready-by-source-order | 0 | 0 |
| M-expressions-of-interest | ready | none-declared | 1 | 0  | M-landing-practice | ready | ready-by-source-order | 0 | 0 |
| M-hypergraph-operator | ready | all-met | 1 | 0  | M-learning-loop | ready | ready-by-source-order | 0 | 0 |
| M-landing-practice | ready | none-declared | 0 | 0  | M-recommendation-bindings | ready | ready-by-source-order | 0 | 0 |
| M-learning-loop | ready | all-met | 0 | 0  | M-self-improvement-loop | ready | ready-by-source-order | 0 | 0 |
| M-recommendation-bindings | ready | none-declared | 0 | 0  | M-stack-geometry | ready | ready-by-source-order | 0 | 0 |
| M-self-improvement-loop | ready | all-met | 0 | 0  | M-stack-stereolithography | ready | ready-by-source-order | 0 | 0 |
| M-stack-geometry | ready | none-declared | 0 | 0  | M-war-machine-wiring | ready | ready-by-source-order | 0 | 0 |
| M-stack-stereolithography | ready | none-declared | 0 | 0  | M-artificial-stack-exchange | ready | ready-by-source-order | 0 | 0 |
| M-trip-journal | ready | all-met | 2 | 0  | M-canon-fingerprint-store | ready | ready-by-source-order | 0 | 0 |
| M-war-machine-wiring | ready | all-met | 0 | 0  | M-differentiable-math | ready | ready-by-source-order | 0 | 0 |
| M-artificial-stack-exchange | ready | none-declared | 0 | 0  | M-differentiable-substrate | ready | ready-by-source-order | 0 | 0 |
| M-canon-fingerprint-store | ready | none-declared | 0 | 0  | M-distributed-frontiermath | ready | ready-by-source-order | 0 | 0 |
| M-differentiable-math | ready | none-declared | 0 | 0  | M-distributed-proofreaders | ready | ready-by-source-order | 0 | 0 |
| M-differentiable-substrate | ready | all-met | 0 | 0  | M-efe-bge-followon-actions | ready | ready-by-source-order | 0 | 0 |
| M-distributed-frontiermath | ready | none-declared | 0 | 0  | M-metric-harness | ready | ready-by-source-order | 0 | 0 |
| M-distributed-proofreaders | ready | none-declared | 0 | 0  | M-paper-reverse-morphogenesis | ready | ready-by-source-order | 0 | 0 |
| M-efe-bge-followon-actions | ready | none-declared | 0 | 0  | M-pheromone-field | ready | ready-by-source-order | 0 | 0 |
| M-hyperreal-dictionary-planning | ready | all-met | 3 | 0  | M-structure-seed-promotion | ready | ready-by-source-order | 0 | 0 |
| M-metric-harness | ready | none-declared | 0 | 0  | M-symbol-grounding-scaling-plan | ready | ready-by-source-order | 0 | 0 |
| M-paper-reverse-morphogenesis | ready | none-declared | 0 | 0  | M-symbol-grounding | ready | ready-by-source-order | 0 | 0 |
| M-pheromone-field | ready | none-declared | 0 | 0  | M-NA2026-outreach-debrief | ready | ready-by-source-order | 0 | 0 |
| M-prior-mathematics | ready | none-declared | 1 | 0  | M-becoming-nomad | ready | ready-by-source-order | 0 | 0 |
| M-structure-seed-promotion | ready | none-declared | 0 | 0  | M-daily-scan | ready | ready-by-source-order | 0 | 0 |
| M-superpod-mark2 | ready | none-declared | 4 | 0  | M-value-creation-loop | ready | ready-by-source-order | 0 | 0 |
| M-superpod-mark3 | undetermined | cyclic | 9 | 0  | M-pattern-application-diagnostic | undetermined | not-ready-by-source-order | 9 | 0 |
| M-symbol-grounding-scaling-plan | ready | none-declared | 0 | 0  | M-superpod-mark3 | undetermined | not-ready-by-source-order | 9 | 0 |
| M-symbol-grounding | ready | none-declared | 0 | 0  | M-apm-capability-ratchet | not-ready | not-ready-by-source-order | 0 | 0 |
| M-NA2026-outreach-debrief | ready | none-declared | 0 | 0  | M-stack-inhabitation | not-ready | not-ready-by-source-order | 0 | 0 |
| M-becoming-nomad | ready | none-declared | 0 | 0  | M-state-snapshot-witness | not-ready | not-ready-by-source-order | 0 | 0 |
| M-daily-scan | ready | none-declared | 0 | 0  | M-coupling-as-constraint | not-ready | not-ready-by-source-order | 0 | 0 |
| M-value-creation-loop | ready | none-declared | 0 | 0  | M-a-sorry-enterprise | not-ready | not-ready-by-source-order | 0 | 0 |

## Waiting on

What each not-ready item still needs, with the sentence the need was read from.

### M-pattern-application-diagnostic (cyclic)
- [:M-pattern-application-diagnostic :M-superpod-mark3]

### M-superpod-mark3 (cyclic)
- [:M-pattern-application-diagnostic :M-superpod-mark3]

### M-apm-capability-ratchet (unmet)
- M-apm-capability-ratchet needs M-futonzero-prelim-practice (closure) -- quote: "the futon0 APM work (`M-apm-capability-ratchet`) does not make sense before the "AI can pass prelims" capability (`M-futonzero-prelim-practice`)" -- futon3c/holes/missions/M-giraffe.md:77

### M-stack-inhabitation (unmet)
- M-stack-inhabitation needs M-repl-wins-over-cli (closure) -- quote: "| M-repl-wins-over-cli | Prerequisite (TESTING) — entry ceremony for the primary surface |" -- futon3c/holes/missions/M-stack-inhabitation.md:125

### M-state-snapshot-witness (unmet)
- M-state-snapshot-witness needs M-reachable-from-boot (closure) -- quote: "- **Foundation:** depends on M-reachable-from-boot (snapshot creation belongs in the bootstrap construction path)." -- futon3c/holes/missions/M-state-snapshot-witness.md:67

### M-coupling-as-constraint (unmet)
- M-coupling-as-constraint needs M-xor-coupling-probe (closure) -- quote: "**Blocked by:** M-xor-coupling-probe, M-tpg-coupling-evolution" -- futon5/holes/missions/M-coupling-as-constraint.md:5
- M-coupling-as-constraint needs M-tpg-coupling-evolution (closure) -- quote: "**Blocked by:** M-xor-coupling-probe, M-tpg-coupling-evolution" -- futon5/holes/missions/M-coupling-as-constraint.md:5

### M-a-sorry-enterprise (unmet)
- M-a-sorry-enterprise needs M-repl-wins-over-cli (closure) -- quote: "| M-repl-wins-over-cli | Prerequisite — evidence pipeline must work |" -- futon5a/holes/missions/M-a-sorry-enterprise.md:154


## Flag: needs-breakdown cycles

- M-pattern-application-diagnostic <-> M-superpod-mark3 (cycle members are withheld from readiness)

## Flag: done-with-open-needs anomalies

- none