# Review of ccc0c703: historical evidence admission is incomplete

Independent existing projection suite: 2 tests / 7 assertions pass.
New retained historical-reader-review.clj uses the real terminal test's
canonical admission/click/binding fixture and actual shared reader. It replaces
only the evidence family with a skeletal historical projection and matching
digests. No repair, verification, historical admission or cohort store exists.
The binding still says grounded-change; the run record still contains task
pin evidence and no historical transition.

The public read-bundle! nevertheless returns:

    {:schema :wm/run4-historical-admission-bundle-v1,
     :classification {:task-result :unknown,
                      :repair-status :awaiting-validation,
                      :infrastructure :safe,
                      :production-successor-required? true}}

Projection lacks repair ID, verification ID/source/artifact, execution identity,
cohort identity, enacted action, requested-pin status and full series/trial/casting.
Digest equality proves bytes match the supplied reference, not that their contents
satisfy the historical admission contract. The reader must validate exact schemas,
all identities and the actual canonical store admission/verification/cohort close.
Require binding outcome/run/attempt/path to agree as well as click ID. Source
fields and immutable artifacts need rereading; matching internally generated
claims alone is not an authoritative store join. Safe infrastructure cannot be
inferred from two repair-state labels.

Producer corrections are not accepted end-to-end until an actual materialized
async fixture produces a routed run, immutable store transition and cohort close
and the strict reader consumes those exact artifacts. Reuse a real selection
route instrumentation path; missing route is not license to invent one. This
negative reader test is a separate prerequisite, not the missing positive gate.

The controller still awaits terminal evidence; no successor advancement was
established by this slice. Keep historical observation separate from task success.

Run from futon3c with test alias paths:
clojure -Sdeps '{:aliases {:review {:extra-paths ["test"]}}}' -M:review ../futon2/holes/labs/wm-contract/runs/RUN4-repair057-revalidation-2026-09-11/historical-reader-review.clj

No live evidence, capacity, attempts, credentials or services changed.
