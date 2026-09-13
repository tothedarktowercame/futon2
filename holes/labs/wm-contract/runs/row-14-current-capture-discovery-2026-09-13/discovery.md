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

## Follow-up: service last-click and September close

GET /api/alpha/wm/click (raw response and timestamp/hash receipt retained here)
reports running? false and last click wm-click-c9d0581b-2ea4-40c9-b1ed-8031e3a4b5d9.
Its exact binding path is under /home/joe/run4/F11-production-successor-20260912-v3.
The read binding agrees on click ID and attempt-001; it explicitly says run-record
absent and binding unavailable. Enumerating this named directory locates
cohort/run4-f11-production-successor-20260912-v3/attempt-001/007-closed.edn.
That close records 2026-09-12T18:10:03.184032787Z and outcome incomplete;
its payload judgment contains neither outcome-entity nor entity-state-at-close.
Exact binding/close/controller-series hashes are in current-attempt-pins.json.
This is a located matching-attempt artifact, not an authenticated run-ID join:
the service itself reports the stronger binding unavailable.

The same GET reports serving-runner-code availability unavailable, reason
not-recorded-in-this-process-image. Therefore current checkout capture fields
cannot establish serving-code identity or deployment. This endpoint was inspected
before use: runner_service/status reads state and code_identity/status reads its
recorded reload map. No click POST, reload or new run was performed.

configured-runner-opts delegates to full-loop-runner/config; config merges caller
opts over defaults, including cohort? true. Explicit execution-cohort can choose
another data-root. The default July directory is therefore insufficient for a
current all-root coverage claim. This single September close supplies no new
categorical observations. Next work must establish the seven-status observation
authority and enumerate justified capture roots with stable cohort/entity/time
joins, retaining unavailable service identity and incomplete joins honestly.
