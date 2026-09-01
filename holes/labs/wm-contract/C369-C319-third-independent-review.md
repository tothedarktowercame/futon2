# C369 — third independent attack on the C319 execution sheet

Date: 2026-09-01. Reviewed the current C319 sheet and current executable
consumers from the document alone. No fence, park, reload, click, restoration,
or production job was attempted.

## Verdict and convergence measurement

**C319 still does not survive independent execution.** The independent finding
counts are **9 (C331) → 8 (C341) → 8 (C369)**. This is not a substantially
falling rate. The mechanisms have improved, but new cross-step assertions are
being added as quickly as old ones are removed. A fourth repair/review pass on
the same broad sheet is not proportionate: shrink the executable claim to a
small state machine whose transitions consume receipts, and keep explanatory
and recovery material outside that executable core.

## Eight current findings

1. **The operator population is internally inconsistent.** The request says
   “five background units,” while the structured attestation and executable
   stop loop name eight systemd units. A reader cannot tell whether “five” means
   families or an actual population; the checker requires the eight-name set.

2. **Restoration authentication is not established before mutation.** Step 0
   tests only that the key is readable. C365 showed a mode-`0644` key is
   accepted, and the sheet checks neither owner nor mode. Moreover, a failed
   `capture` prints JSON and exit 1 but the command block has no `|| exit` or
   manifest-existence assertion. The next step can therefore begin parking
   without durable restoration authority.

3. **The park journal is still not an executable all-target transaction.** The
   terminal watchdog has one literal `record` command. The two coordinators and
   conditionally changed units are followed by prose telling the operator to
   construct and invoke further commands. No aggregate assertion proves that
   every changed target has exactly one successful journal row before the sheet
   advances. A failed record can leave a parked writer outside restoration.

4. **The 300-second receipt age is incompatible with the bounded gate path.**
   Step 2 creates evidence before `make workspace-gate`. The bounded submitter
   may wait up to 2700 seconds for admission, and the gate alone has measured
   about 216 seconds. The inner gate consumes the original receipt near its end;
   it can therefore be stale before use even when the operator follows the
   sheet without delay. Fresh live re-observation does not waive the explicit
   prior-receipt age check.

5. **Fence assessment failure is not a blocking gate verdict.** The capability
   distinguishes a missing receipt as `:status :unavailable`, and checker
   nonzero as an unverified live fence, but `wm_workspace_gate.clj` uses that to
   qualify the event claim rather than add a gate failure. All enumerated checks
   can exit 0 while the fence assessment subprocess failed. C319 asks the reader
   to inspect for `FENCE-CONDITIONAL`; its command-level success does not enforce
   that requirement.

6. **Normal and abort release ordering drops the fence before restoration.**
   Step 9 announces `FENCE-RELEASE` and only afterwards runs restoration. The
   “parked, before reload” abort row does the same. Releasing lane, publisher,
   session, and operator promises permits new writers while inverse operations
   are still running. The emergency path has the safer order—restore, verify,
   then release—but the ordinary path does not.

7. **The emergency retry claim is stale against C365.** The sheet says a retry
   skips completed inverses recorded in the outcome ledger. If an inverse and
   postcondition succeed but the outcome append fails, no completion is
   recorded; retry then rejects because the target is no longer parked. The
   procedure describes this case as resumable when it is presently stuck and
   needs owner intervention.

8. **A 60-minute attestation contradicts the explicitly unbounded middle of
   the procedure.** The sheet says reload and live author/reviewer latency are
   unbounded, but creates an attestation expiring in 60 minutes. The required
   post-click writer observation validates current attestation time. A correct
   but slow run therefore cannot reach the asserted release evidence; no renewal
   transition is defined, and silently extending the attestation would rewrite
   the window's authority after it began.

## Seams that held

- Preflight consumes ID plus receipt and re-runs the evidence subprocess; a name
  alone is not proof.
- The checker has a typed observation-unavailable result, and the explicit
  step-2 shell captures and tests its exit.
- Missing terminal click evidence remains fail-closed with writers parked.
- Coordinator classes agree across the attestation, evidence checker, and
  restoration tool.
- Bounded ID transport remains shell-quoted and missing ID/evidence pairs are
  rejected.
- Post-click evidence is correctly scoped to parked writers rather than falsely
  reasserting clean repositories.

## Focused evidence

- A missing restoration key made `capture` exit 1 and produced no manifest; the
  surrounding documented command sequence supplied no stop condition.
- `fence/assess` over a missing receipt returned `:status :unavailable`,
  `:event-free? :unverified`, and `:distinguishable-cause? false`.
- Current source comparison used `max-receipt-age-seconds = 300`, bounded
  admission timeout `2700`, the sheet's 60-minute attestation, and the recorded
  216-second gate duration.

The measured result supports reducing C319's claim, not another broad rewrite.
