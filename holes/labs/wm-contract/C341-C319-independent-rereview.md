# C341 — independent re-review of the rewritten C319 execution sheet

Date: 2026-09-01. Reviewer: `wm-evidence`. Reviewed C319 commit `58455f5`.
This is an assessment only; C319 and its implementations were not changed.

## Verdict

**Not yet executable from the document alone.** The coordinator-class seam is
now consistent between C319 and `writer_fence_evidence.py`, and an unavailable
or in-flight click fails closed by keeping writers parked. However, eight
remaining defects include two unsupported fence claims and an unrecoverable
partial-parking path.

## Findings

1. **The pre-fence manifest is not persisted.** C319:48-74 calls observation
   commands without redirecting or serialising their output, yet C319:318-345
   and the emergency path at 351-362 require the captured activation intent to
   decide which writers may be restored. If the coordinator session disappears,
   the restoration journal contains actions but not the pre-state that licenses
   them. The emergency procedure therefore cannot be executed from its durable
   files.

2. **The restoration journal is specified but not constructed by the command
   block.** C319:76-78 and 126-128 say to append after each successful park,
   but C319:89-108 contains no append commands and does not distinguish a unit
   that was already inactive from one actually changed by `systemctl stop`.
   `stop` can succeed for an already-inactive unit; blindly recording it would
   later start a writer that was not active before the fence.

3. **The two advertised waits are only single observations.** C319:100-107
   invokes `systemctl is-active` once, ignores its nonzero/active meanings, and
   proceeds. It then explicitly stops `apm-closer.service`; this can terminate
   the work that C319:112-114 says must finish. There is no polling condition,
   timeout, or abort branch. This is the clearest silent-failure step.

4. **The attestation population under-records the prose population.** C319:23-25
   requires acknowledgements from every session with write authority, but the
   structured content at 39-40 records only the four named WM lanes. Additional
   sessions can acknowledge in prose and remain absent from the receipt. The
   checker proves the fixed list is nonempty, not that it equals the actual
   write-authority population.

5. **A syntactically valid but unverified fence ID still reaches the bounded
   gate and becomes `:event-free? true`.** C319:179 supplies only
   `FUTON_WRITER_FENCE_ID`. `run_workspace_gate_bounded.py:38-47` validates its
   character shape, then injects it into the systemd command. The raw gate at
   `checks/wm_workspace_gate.clj:85-96` treats any nonempty ID as `:status
   :held` and `:event-free? true`; it does not consume the C335 evidence receipt.
   The sheet relies on operator continuity between steps 2 and 3, but the gate
   receipt itself cannot establish that join. A malformed ID rejects; an
   arbitrary well-formed ID does not.

6. **Three bounded-job commands contain unresolved IDs and no terminal polling
   command.** C319:200-212 and 249-252 say `FUTON2_JOB_ID`, `FUTON3_JOB_ID`, and
   `JOB_ID`, but do not show how to extract those IDs from `launch-test` or poll
   until a terminal receipt exists. A single `test-status` can legitimately
   report an in-flight job. The prose says to wait, but the executable sheet
   does not implement the transition.

7. **The click-never-reports branch is safe but not runnable.** The observer has
   a bounded status poll and can emit `click-status-unavailable`; C319:374 then
   correctly refuses release or restoration while a click may be active. This
   cannot be defeated into unsafe restoration. But the sheet provides no
   independent command that establishes “typed terminal outcome and no click
   write in flight,” so recovery after an unavailable observer is an indefinite
   escalation, not an executable abort path. This is fail-closed rather than
   silent success, but Joe cannot finish the procedure from this sheet.

8. **The final fence-through-release claim lacks a post-click observation.** The
   last required evidence rerun is before the click at C319:261. C319:304-316
   later claims the fence was held through release and no breach was observed,
   but there is no post-click fence-bundle command before that statement. The
   certificate observes the run and resource status; it is not a replacement
   for the writer-population observation. The claim exceeds the recorded
   interval.

## Cross-lane seams that held

- C319's three coordinator classes exactly match
  `writer_fence_evidence.py`'s current intended-state vocabulary: one terminal
  coordinator with watchdog stopped, and two durably stopped coordinators.
- Preflight requires both ID and receipt and independently reruns the evidence
  bundle; receipt filename alone does not certify the fence.
- A missing terminal click outcome does not authorize release. The current
  abort text keeps writers parked and escalates rather than treating absence as
  completion.
- The bounded wrapper quotes and transports IDs matching its declared lexical
  grammar; the unresolved problem is semantic binding, not shell transport.

## Inventory

`bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn
(g/inventory-result))"` is the delivery inventory command. Its result is
`{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
