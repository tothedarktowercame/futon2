# Measured-A capture discovery — codex-26, 2026-09-13

Read-only source inspection, pinned in source-pins.json; no live reload, run,
new close census, or deployment claim. The earlier 82 physical July closes
remain a historical scoped finding, not a fresh coverage measurement.

Current source path: futon3c wm/runner_service.clj run-click! resolves
futon2.aif.full-loop-runner/run-opportunity! after configured-runner-opts.
The CLI also calls run-opportunity!. In full_loop_runner.clj the selection
resets selected-entity-belief with selected-target, (:belief judgement), and
run/id (around 3167). At close, entity-state-at-close reads that retained belief
and labels the unique argmax :derived-unique-argmax-of-mu-post. This is not
independently observed categorical status. outcome-entity-at-close independently
retains selection target identity and refuses disagreement with the belief row.
Both fields enter the closed judgment, then cohort/close-attempt! writes through
the explicitly bound execution-cohort data-root when present, otherwise the
cohort default. full_loop_cohort.clj defaults to data/wm-full-loop; close-attempt!
validates required checkpoints and appends a :closed checkpoint.

row14_measured_a_coverage.clj still reads its pinned census for measured statuses
and emits literal :deployed? false / :reload-status :pending. Those literals
cannot establish current deployment. A new report must distinguish source
capability, loaded service capability, actual retained observed data and authority.

Next bounded packet: inspect configured-runner-opts/cohort bindings and current
read-only service status to resolve active capture roots; enumerate observed-state
owners for the seven statuses and define a justified entity/time join. Pin exact
observed source records or emit absent; latent argmax is not ground truth.
Do not collect failures or run the loop to fill empty cells. This discovery does
not yet establish the active service root or defensible observed-state authority.
