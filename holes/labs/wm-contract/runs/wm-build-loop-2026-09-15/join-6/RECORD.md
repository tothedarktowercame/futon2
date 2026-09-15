# join-6: verify and assign the RUN4 regression report

- **Author:** claude-2, 2026-09-15.
- **Reviewer:** codex-28.
- **Clearance:** p4ng `08b535d`
  (`wm-walkthroughs/build-loop/decisions/CHECKLIST-WORK-CLEARANCE-2026-09-15.md`,
  item 1).
- **Scope:** verification and assignment only. No fix was made or commissioned.
- **Claims:** join-6 has no checkbox. This record claims nothing for R10, WM
  or completion.

## The item

The checklist entry reads:

> claude-20 reports 11 baseline failures in `futon3c.wm.run4-http-boundary-test`
> (403 instead of expected 200 among the findings). Verify and assign.

## Verification

- **Run.**
  - Command (from `command.txt`): `clojure -X:test :nses '[futon3c.wm.run4-http-boundary-test]'`.
  - It ran in its own short-lived process from `/home/joe/code/futon3c`,
    never in the shared JVM.
- **Source identity** (`source-identity.txt`):
  - futon3c `master` at `ca8f1a45`, with no changes under `src/` or `test/`.
  - futon2 at `bf1c8837`, with no changes under `src/`.
  - SHA-256 hashes of the test file and the handler, admission, service and
    c-fold sources are recorded in the same file.
- **Result.**
  - Exit 1. 4 tests, 27 assertions, **11 failures**, 0 errors.
  - Output is in `test.out` / `test.err` / `test.exit`, with times in
    `started-at.txt` and `finished-at.txt`.
- **Comparison with the dated report.** The count and split match
  claude-20's report:
  - 7 failures in `valid-run4-propagates-exact-server-derived-options`: two
    403s where 200 was expected, `click!` never called, and none of the
    derived options present.
  - 4 failures in `concurrent-duplicates-corruption-and-write-failure-never-double-click`:
    ten 403s where 200 was expected, zero clicks, and 403 where 409 and 500
    were expected.
- **Passing tests.** `run4-refusals-occur-before-click-creation` and
  `legacy-and-single-flight-responses-are-preserved` both pass.

## Cause: the test fixture is stale in two respects

Both causes come from later, intended contract changes. The test file was last
changed in `eac0f12e` (2026-09-10 17:50Z), before either of them.

1. **The `:cohort?` runner option was removed.**
   - What happens: the fixture's pinned config supplies
     `:runner-options {:cohort? false}`. Commit `e06be3a7` (2026-09-11 01:59Z,
     "Bind RUN4 preparation to explicit execution cohort") removed `:cohort?`
     from `runner-option-keys` in `run4_pinned_run_config.clj`. It updated
     that validator's own test, but not this one.
   - Evidence: `probe-cause.out` shows `load!` on the fixture's exact config
     text refusing with `{:error :run4-pinned-run-config-refused :reason
     :unsupported-config-shape}`. The HTTP response is 403
     `run4-pinned-config-invalid` (`probe-403.out`).
2. **An effective-environment attestation was added.**
   - What happens: with only the `:cohort?` key removed, the valid request
     then gets 500 `run4-effective-environment-refused`, and `click!` is
     still not called (`probe-cause.out`). The attestation came in with
     `801f67b1`, `f70dda23`, `f82b13fe` and `72a4c364` (2026-09-10,
     18:36–19:06Z). `run4_trusted_entry` calls it through the dynamic seam
     `*attest-effective-environment*`. It compares the pinned `"1"`
     requirements with the process environment. The fixture neither binds
     the seam nor runs with the serving flags set.
   - Evidence:
     - With both causes bypassed (`:runner-options {}` and a success-shaped
       stub bound to the seam), the valid request returns 200. `click!` is
       called exactly once and the admission is recorded as
       `click-recorded` (`probe-two-layers.out`).
     - The real test namespace, loaded from a temporary copy that differs
       only in the `:cohort?` line (`layer1-removed.diff`, 2 lines) and run
       with the seam bound, passes **4 tests / 27 assertions, 0 failures**
       (`probe-full-suite.out`).
   - So all 11 observed failures trace to these two causes. The probes are
     diagnostics only; the repository test is unchanged.

## Impact on ordinary clicks: ruled out by the code path

- **The failing step runs only for RUN4 clicks.** `handle-wm-click-start` in
  `src/futon3c/transport/http.clj` prepares a RUN4 click only when the request
  names a RUN4 pin: `prepared (when (contains? payload :run4-pin-ref) …)`.
  Both refusal points sit inside that preparation, in `pinned-config/load!`
  and in the `*attest-effective-environment*` call in `prepared-options`.
  An ordinary click carries no pin, so it skips preparation and admission.
  It goes straight to `runner-service/click!`, or to the R10 commissioned
  adapter.
- **The ordinary click target reaches neither check.** A search of every
  source file for what requires them:
  - `run4-effective-environment` is required only by `run4_boot`,
    `run4_trusted_entry` and `run4_terminal_evidence`.
  - `run4-trusted-entry` is required only by `http.clj` (inside the pin
    branch), `run4_series_service` and `run4_report_service`.
  - `runner_service.clj` references neither.
- **Serving startup doesn't enable RUN4 by default.**
  `dev/futon3c/dev/bootstrap.clj` builds the RUN4 handler configuration only
  when `FUTON3C_RUN4_U88_ENABLED` is set, and it defaults to false. When
  disabled, the configuration is identical to the old handler's and nothing
  is read.
- **Test evidence agrees.** `legacy-and-single-flight-responses-are-preserved`,
  which sends an ordinary click without a pin, passes.

## Impact on RUN4 clicks

- **The first cause doesn't reach the retained configurations.** Every one of
  the 14 retained production RUN4 pinned configs lacks `:cohort?`. They are
  the `run-config.edn` files under `futon3c/holes/labs/wm-contract/runs/RUN4-*`
  (U88 codex20 v1 and v2, U88 zai-successor v1 and v4, repair057, repair058,
  repair-ea1 v1 and v2, repair-pinned-selection,
  repair-successor-v2-selection, its zai variant,
  repair-initialization-collision, initialization38690 and
  initialization-close admissions).
- **The second cause depends on the serving environment. For the current
  deployment this is unresolved.**
  - `LOADED-CONSUMERS-2026-09-11.md` records codex-17 running the production
    `attest` and getting all three flags required `"1"`, observed `"1"`,
    effective true. That record also says RUN4 startup remains false.
  - I did not check the current serving environment. This record makes no
    claim about present RUN4 serving behaviour.

## Assignment

- **The confirmed issue.** `futon3c.wm.run4-http-boundary-test` has not been
  updated for two intended RUN4 contract changes. It still supplies the
  removed `:cohort?` runner option. It does not bind the effective-environment
  seam, and it does not supply a valid attested environment. So it currently
  tests nothing past pinned-config validation on the valid, concurrent,
  corruption and write-failure paths.
- **The required fix, not commissioned here.** Update the fixture:
  - drop `:cohort?`;
  - bind `*attest-effective-environment*`, or supply a serving declaration
    and flags;
  - check again that the 409 corruption and 500 write-failure assertions
    exercise the paths they are meant to.
- **Proposed owner: codex-17, pending codex-28's confirmation.**
  - codex-17 ran the RUN4 effective-environment attestation and loaded-consumer
    work (`LOADED-CONSUMERS-2026-09-11.md`).
  - The test's own casting names codex-17 as RUN4 reviewer.
- **The attribution gap, stated rather than hidden.** None of the six causing
  commits carries an agent attribution: `e06be3a7`, `801f67b1`, `f70dda23`,
  `f82b13fe`, `72a4c364` all use the shared git identity with an empty body.
  claude-20's report says "Nobody owns this yet." If codex-28 prefers another
  RUN4 lane owner, the assignment should follow that choice.

## What this discharges

- **Discharged, if codex-28 accepts it:** join-6's verify-and-assign
  obligation. The 11 failures are reproduced, their cause is identified,
  their effect on ordinary clicks is ruled out by the inspected path, and an
  owner is proposed.
- **Not discharged:** it does not fix the test, validate R10 or say anything
  about current RUN4 serving.
