# CHANGES-REQUESTED — independent occurrence-identity review

Reviewer: codex-1. Author: codex-24. Review job:
`invoke-1789848531056-22515-dbcfb38d`. Date: 2026-09-19.

Inspected implementation diffs at `0c9499f4e101f60e403217012c4b9cb6d4574fdf`,
`15ec4d6b58eeeac56edc3feb01cebf40da004e67`,
`ab70f0cc6f284fbccdefc4466924517c4e87b211`, and
`84c4c1267988c65970b66b4a87ecf9866d316df3`; registry/wrapper diffs at
`59867021044e402cf147a202b226119473bb5395` and
`988ccb6f704883e0eb5a15401840737223941bd8`, current sources/tests, execution
receipt, and authority note section 5 points 1–2. Tripwire witness identity
was not reviewed as an implementation requirement.

## Current warrant: valid, but coverage insufficient

Executed POST `http://localhost:7070/api/alpha/test-registry/check` with
`:entry-id` `test-registry-97ab21445fa4626c876c9b550ab614d945d851f2bf12dcbd1c476fdded8e4a53`,
`:repo-root` `/home/joe/code/futon2`, and all five changed source/test paths
as `:changed-paths`. At `2026-09-19T20:09:33.992019223Z` it returned
`:warrant? true`, chain length 2, `:outside-closure []`, and recorded results
5 tests / 16 assertions / zero failures/errors. This was the check endpoint,
not a mint-time evidence lookup. No test rerun or repair-store write occurred
in this review. The five-test count includes the aggregate wrapper.

## Blocking findings

1. **Reconstructed retries conflict instead of reusing the original finding.**
   `full_loop_runner.clj:4818` obtains a new `started-at` on every call;
   lines 4880–4887 pass it into reconstructed occurrence identity. The hash
   excludes time (`repair_obligation.clj:420–426`), but the returned occurrence
   includes it. Lines 542–548 embed that occurrence and use its timestamp for
   `:opened-at`; the unchanged publication routine compares exact immutable
   bytes at lines 315–387. Thus two observations with identical origin/event/
   kind and different observation times have the same filename but different
   immutable bytes, producing `:repair-finding-conflict` before evidence append.
   Authority point 2 explicitly requires retaining the original opened-at.

   Executed a pure `clojure -M -e` probe calling `occurrence-identity` twice
   with origin `wm-runner::review-fixed-run`, event `review-fixed-event`, kind
   `:initialization-failed`, and created-at values `2026-09-19T20:00:00Z` and
   `2026-09-19T20:01:00Z`. Result:
   `{:same-occurrence-id true :same-immutable-occurrence false}`.
   No writer was called. The conflict conclusion follows from the inspected
   exact-byte comparison, not a claimed executed store reproduction.

   The initialization test at `full_loop_runner_test.clj:3987–4017` stubs the
   writer and now checks only occurrence IDs, deliberately ignoring the map
   difference. Store tests reuse one precomputed occurrence with a fixed
   timestamp. Neither exercises the composed path that fails. Add a real
   temporary-store initialization retry control at distinct times, retaining
   original finding bytes and appending the later observation.

2. **Occurrence identity is not enforced at the publication boundary.**
   `record-system-failure!` still chooses caller `repair-id` ahead of the
   occurrence-derived ID (`repair_obligation.clj:526–528`). The writer accepts
   the supplied occurrence map without recomputing/verifying its ID or checking
   its typed failure against the finding. Consequently one occurrence can be
   sent with two repair IDs and publish two findings; the later observation
   conflict happens only after publication (lines 548–550). This violates the
   claimed one-finding-per-occurrence contract even though CREATE_NEW remains
   intact. Preserve the legacy path for inputs without occurrences, but refuse
   contradictory IDs/malformed occurrence evidence before publishing anything.
   Add negative controls proving no second finding is created.

3. **Separate observation evidence is not wired through runner callers.**
   The runner supplies occurrences but no `:observation` fields at its new
   publication sites. `occurrence-evidence!` defaults observation ID to the
   originating event ID (`repair_obligation.clj:435–443`). Propagating the same
   occurrence therefore reuses one observation filename, not an additional
   containment observation; reconstructing it with a new timestamp would
   additionally conflict in this record. The two-observation store test passes
   explicit `inner-catch`/`outer-catch` IDs which the runner never supplies.
   Wire stable observation identities/provenance for actual containment and
   retry observations, and test that integration with the real temporary store.

## What is supported

The hash separates origin, event ID, and typed failure. Existing publication
locking and CREATE_NEW are retained. Identical caller-supplied records reuse
one finding, conflicting bytes refuse, and parallel identical publication is
covered. Three distinct job occurrences remain visible to the real T8
`livelock-violations` function. The implementation is forward-only with no
migration of old records in the inspected delta. These are useful properties,
but do not discharge the retry/containment requirements above.

Only this review note was written. No production or temporary repair-store
mutations, click, serving-JVM reload, or restart was performed.
