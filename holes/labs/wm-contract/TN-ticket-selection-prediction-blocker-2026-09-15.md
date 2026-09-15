# Ticket selection: prediction contract blocks implementation

Packet: claude-20 bell invoke-1789478716105-21021-46aa2127. Discovery completed; executable implementation is **blocked under the packet’s stop rule**. No registry, proposal, forward-model, scope or runtime code changed. No reload or click.

## Resolution (claude-20, lead, 2026-09-15)

The stop was right; the law is now decided and dispatched (bell invoke-1789479853453-21033-f928800f). A ticket is treated as a mission with one open item. `:advance-ticket` uses the `:advance-mission` law's declared prior (`forward_model.clj:152–168`) on the one channel that will measure tickets: `:mission-health += 0.04·f`, variance 0.015, event `:addressed` weighted by f, with f = `advance-mission-ordinal-factor(1)` (≈0.667, computed by the existing function). There is no `:sorry-count-norm` term, because tickets are not in the sorry census.

This reuses a declared, uncalibrated prior rather than deriving one from mission-health's formula. A derivation would give a ticket completion about 1/total while missions keep their declared 0.04·f, which would make tickets about forty times weaker and never selected. Both laws get calibrated later from recorded attempts.

For the prediction to be true, the War Machine's own triage (`war_machine.clj` `scan-mission-triage`) counts tickets as work items: complete → "complete", inactive → "inactive", not machine-actionable → "not-actionable", live → "open", so tickets never enter the abandoned or blocked penalties. futon3c's shared mission inventory is not changed. Mission-health therefore measures missions plus tickets from this change on.

## Exact missing inputs

The requested analogue, `forward_model.clj:152–168`, predicts `:mission-health += 0.04*f`, `:sorry-count-norm -= 0.05*f`, variances 0.015 and 0.01, and an `:addressed` event weighted by f. `mission-value-factor` at lines 112–128 uses an enriched mission scalar, or the mission remaining-work factor at lines 96–110. The latter silently defaults a missing count to 3; a new action type otherwise receives constant f=1. Neither fallback supplies ticket evidence.

Live enrichment is a `:advance-mission` branch (`war_machine.clj:2700–2729`), reading a mission document endpoint, mission phase, centrality and strategic values. The requested ticket fields (title, Status, optional parent) do not define those values for a ticket. The output channels are specifically mission-triage health and open-sorry count (`observation.clj:21–27,52–59,124–133`). A ticket marked PARTIAL does not entail that advancing it reduces the open-sorry census or improves parent-mission triage by those amounts. Parent identity alone does not establish the causal effect.

**Missing contract:** a ticket-specific mapping from evidence of ticket progress to predicted observed channels and their uncertainty, including the producer of any scale f. Copying mission coefficients/variances, substituting f=1, inheriting parent mission values, or using the existing missing-count default would be unapproved modelling choices. A typed `:addressed` event alone would not provide the numerical observation prediction required for ranking.

This is a missing declared ticket prediction law, not a proof that tickets cannot be modelled. The next decision is which observed ticket-progress quantity the model should predict, how it affects existing observation channels (if any), and what source supplies its uncertainty. Once that is specified, registry/enumerator/document/scope wiring can proceed as the original packet requests. No new authority seat or general evidence subsystem is proposed.

## Ticket census

34 immediate `futon3c/holes/tickets/T-*.md` files: **20 live, 8 complete, 2 inactive, 4 not actionable**. The two T-prefixed discovery notes directly under holes are excluded. This is the actual futon3c directory census, not an implemented cross-repository registry.

Classification follows the packet: leading DONE → complete; SUPERSEDED/DEFERRED/PARKED/ARCHIVED → inactive; WATCH/FINDING/DESIGN CONSTRAINT or a Status saying Joe’s call/awaiting Joe → not actionable; otherwise live. Mid-line “deferred” does not override leading STILL-OPEN. Full status text and source-byte hashes are retained in `runs/ticket-selection-discovery-2026-09-15/census.json`.

### Exact live list

- `T-car3-phase2-impl` — `/home/joe/code/futon3c/holes/tickets/T-car3-phase2-impl.md:3`
- `T-car3-queue-routing-design-note` — `/home/joe/code/futon3c/holes/tickets/T-car3-queue-routing-design-note.md:3`
- `T-cx-new-blocks-emacs` — `/home/joe/code/futon3c/holes/tickets/T-cx-new-blocks-emacs.md:3`
- `T-evidence-pinned-to-mutable-prose-26082026` — `/home/joe/code/futon3c/holes/tickets/T-evidence-pinned-to-mutable-prose-26082026.md:3`
- `T-fail-activation-authority-unavailable` — `/home/joe/code/futon3c/holes/tickets/T-fail-activation-authority-unavailable.md:3`
- `T-fail-agent-not-found` — `/home/joe/code/futon3c/holes/tickets/T-fail-agent-not-found.md:3`
- `T-fail-invoke-error` — `/home/joe/code/futon3c/holes/tickets/T-fail-invoke-error.md:3`
- `T-fail-invoke-interrupted` — `/home/joe/code/futon3c/holes/tickets/T-fail-invoke-interrupted.md:3`
- `T-fail-no-execution-evidence` — `/home/joe/code/futon3c/holes/tickets/T-fail-no-execution-evidence.md:3`
- `T-fail-operator-cancelled` — `/home/joe/code/futon3c/holes/tickets/T-fail-operator-cancelled.md:3`
- `T-fail-worker-lost-on-restart` — `/home/joe/code/futon3c/holes/tickets/T-fail-worker-lost-on-restart.md:3`
- `T-fixture-becomes-registry-26082026` — `/home/joe/code/futon3c/holes/tickets/T-fixture-becomes-registry-26082026.md:3`
- `T-jvm-provenance-before-multi-jvm` — `/home/joe/code/futon3c/holes/tickets/T-jvm-provenance-before-multi-jvm.md:9`
- `T-kangaroo-caching-and-compaction` — `/home/joe/code/futon3c/holes/tickets/T-kangaroo-caching-and-compaction.md:3`
- `T-mq7-unaddressable-caller` — `/home/joe/code/futon3c/holes/tickets/T-mq7-unaddressable-caller.md:3`
- `T-strategic-cascade-emits-disconnected-patterns` — `/home/joe/code/futon3c/holes/tickets/T-strategic-cascade-emits-disconnected-patterns.md:9`
- `T-watu-replay-layer` — `/home/joe/code/futon3c/holes/tickets/T-watu-replay-layer.md:7`
- `T-wm-turns-are-not-operator-turns` — `/home/joe/code/futon3c/holes/tickets/T-wm-turns-are-not-operator-turns.md:7`
- `T-wm-wrong-corpus-26082026` — `/home/joe/code/futon3c/holes/tickets/T-wm-wrong-corpus-26082026.md:3`
- `T-zai-chat-transient-timeout` — `/home/joe/code/futon3c/holes/tickets/T-zai-chat-transient-timeout.md:3`

### Exclusions

- `T-agency-desktop-save`: complete — `/home/joe/code/futon3c/holes/tickets/T-agency-desktop-save.md:3`
- `T-car3-queue-routing`: inactive — `/home/joe/code/futon3c/holes/tickets/T-car3-queue-routing.md:3`
- `T-codex-auto-bellback`: complete — `/home/joe/code/futon3c/holes/tickets/T-codex-auto-bellback.md:3`
- `T-devmap-forward-model-calibration`: not-actionable — `/home/joe/code/futon3c/holes/tickets/T-devmap-forward-model-calibration.md:7`
- `T-dispatch-clock-excursion-prefix`: complete — `/home/joe/code/futon3c/holes/tickets/T-dispatch-clock-excursion-prefix.md:3`
- `T-durable-queue-rename-and-tidies`: complete — `/home/joe/code/futon3c/holes/tickets/T-durable-queue-rename-and-tidies.md:3`
- `T-exogenous-evidence-update-rule`: not-actionable — `/home/joe/code/futon3c/holes/tickets/T-exogenous-evidence-update-rule.md:7`
- `T-forward-model-vs-active-work`: not-actionable — `/home/joe/code/futon3c/holes/tickets/T-forward-model-vs-active-work.md:6`
- `T-kangaroo-v1-pouch`: complete — `/home/joe/code/futon3c/holes/tickets/T-kangaroo-v1-pouch.md:3`
- `T-lean-authority-alias-27082026`: complete — `/home/joe/code/futon3c/holes/tickets/T-lean-authority-alias-27082026.md:3`
- `T-matrix-federation-proof`: inactive — `/home/joe/code/futon3c/holes/tickets/T-matrix-federation-proof.md:3`
- `T-mesh-edge-coverage`: complete — `/home/joe/code/futon3c/holes/tickets/T-mesh-edge-coverage.md:3`
- `T-mesh-qa-misrouting`: complete — `/home/joe/code/futon3c/holes/tickets/T-mesh-qa-misrouting.md:3`
- `T-typed-bell-arse-write-async`: not-actionable — `/home/joe/code/futon3c/holes/tickets/T-typed-bell-arse-write-async.md:4`

## Source pins and validation

- `/home/joe/code/futon2/src/futon2/aif/forward_model.clj:96-128,152-168` — SHA-256 `073492ee1276a5f17fcaf994931a430e1f72551ec0d9a19d3c7042a6d74f17d0`
- `/home/joe/code/futon2/src/futon2/aif/observation.clj:21-27,52-59,124-133` — SHA-256 `9c93087e6b8a1ecb327749b58159799125edbaff43cf21a17deb4a2680ed6b57`
- `/home/joe/code/futon2/scripts/futon2/report/war_machine.clj:2700-2729` — SHA-256 `69a066cccb0b67ac56521f53a94783b260a1ea754ed4ecde63dea700af7540ff`
- `/home/joe/code/futon2/src/futon2/aif/mission_registry.clj:152-161,322-350` — SHA-256 `07822ed0667f268350fc3b9d921adce4d0d8c78276463b3bf03e93f5a01b89da`

Executed: enumerated and classified all 34 files, asserted the 34/20 census, retained complete status lines and SHA-256 hashes, and parsed the generated JSON. No implementation tests or machine runs were performed: implementation stopped before introducing a ticket predictor.
