# Review of e39341b5: producer contract changes required

Independent projection namespace: 2 tests / 7 assertions pass.
Retained historical-actual-projection-review.clj invokes the actual cohort-enabled
runner historical test through the real run-opportunity! wrapper, adding requested
pin provenance and a disposable run-record directory. It captures the actual
result and passes it to the real projection. External action execution remains
the existing test port; this is not a real verification-store integration.

Observed: outcome awaiting-validation, projection refuses
:malformed-historical-result. Selection ground contains :run4/requested-pin but
not :run4/enacted-action. The latter is put in the intermediate judgement and
not copied into selection ground; the projection fixture invents that field.

The test supplies no topology route and thus honestly produces no run record.
Separately, source inspection of persist-run-record! (full_loop_runner.clj:280)
shows the production record contains run/click/time/route and optional task pin
and environment attestation, not :attempt-id or :checkpoints. The projection
requires both, so a routed record would still fail its binding check. Correct
the actual writer and reader contract together; do not enrich fixture-only maps.

Identity correction also needed: result :attempt-id is runner execution grain,
yet the projection labels it :controller-attempt/id. Controller admission identity
must come from the authenticated serving context and durable join, separately.
Result also carries no top-level :cohort, so the current projection emits nil;
obtain the actual explicit cohort and captured attempt identity from its producer.

Publication has exists-then-atomic-write race and no safe-ID or canonical-parent
confinement at the click-derived filename. Reuse the reviewed immutable publication
and confinement discipline; atomic replacement alone is not no-clobber admission.
The new dynamic historical projection root is not bound by series service, which
currently binds only ordinary terminal roots. Make it server-owned end-to-end.

Next gate: real materialized async producer plus actual immutable historical store,
cohort close, generated run record, projection and strict reader. No task success,
repair resolution or acceptance inferred. Then chain recording/controller/visibility.
No live capacity, attempt, service, credentials or failed evidence changed.

Reproduction command from futon3c:
clojure -Sdeps '{:aliases {:review {:extra-paths ["test" "../futon2/test"]}}}' -M:review ../futon2/holes/labs/wm-contract/runs/RUN4-repair057-revalidation-2026-09-11/historical-actual-projection-review.clj
