# C387 — report-only exit consumer census

## Scope and conclusion

Exit 3 is not safely portable as a global house convention.  It already means
`DECISION-DUE` at the status-program boundary, while C382 gives it the local
meaning `report-only findings` at two lint boundaries.  That local meaning is
sound only while accompanied by the command declaration that consumes it.

The safe composition is therefore the one C382 currently uses: the workspace
gate invokes a declared report-only command, retains its `:observed-exit 3`,
and normalises only that declared result to gate success.  Exit 3 must not be
exported as though every outer consumer knows its meaning.

## Consumer census

| Consumer | Behaviour for exit 3 today | Assessment |
|---|---|---|
| Direct shell/process caller | Preserves 3 exactly. | Sound only if the caller knows the producing command's vocabulary. |
| `checks/wm_workspace_gate.clj` | Accepts 3 only for entries carrying `:expected-exits #{0 3}`; records both `:observed-exit` and the declared set. Undeclared 3 remains failure. | Sound and reason-preserving. This is the only current consumer of report-only 3. |
| Futon3 bounded-test service (`bounded_test_job.py`) | Any nonzero inner exit becomes outer 125 / `test-failure`; the receipt retains `inner-exit: 3`. | Does not understand report-only. A report-only lint launched directly through it is classified as a failed test. Safe for the workspace gate because the gate has already consumed 3 and exits 0. |
| `scripts/run_workspace_gate_bounded.py` | Returns outer failure first; otherwise returns the receipt's inner exit. | Does not recover report-only semantics. Correct for the composed gate, whose inner exit is already 0. |
| GNU Make recipes | GNU Make itself exits 2 for any failed recipe, including a script exit 3. The repository wrappers print `script-exit=N` before returning. | Numeric meaning is lost at Make's process boundary and survives only in output. This is the known C216 boundary. |
| `scripts/run_readiness.py` | Requires workspace receipt `outer-exit == 0`, `inner-exit == 0`, verdict pass, clean resources, and fresh basis. Other direct subprocesses use `returncode == 0`. | Treats standalone 3 as failure; accepts the composed gate because it is normalised internally. Sound, but not a report-only consumer. |
| `scripts/wm_quiet_run_state.py` | Requires bounded receipt outer 0, pass, clean resources, and stable basis. | Same: report-only 3 must be consumed inside the gate before this state machine. |
| `scripts/wm_status_report.py` | Ordinary component subprocesses are binary zero/nonzero. Its own exit 3 means `DECISION-DUE`, not report-only. | A direct report-only lint would be red. Reusing 3 globally here would create a semantic collision. |
| Workflow report EDN | Records lane/work state and does not interpret subprocess exit codes. | Not an exit-code consumer. |
| Preemptive-repair suite | Models report-only populations in structured data (`:policy`, counts) and exits according to the blocking population. | Already uses the stronger representation; no exit-3 dependency. |

Searches also found no consumer that treats “non-1” or “non-2” as success.
Generic consumers consistently use zero/nonzero; that is conservative, though
it loses the report-only distinction.

## Consequence

No additional consumer needs changing for the current composition.  The two
report-only lints must remain inner constituents of the workspace gate (or be
consumed by another explicitly declared adapter) before crossing the bounded,
Make, readiness, or state-machine boundaries.

If report-only status must cross Make or the bounded service independently,
the semantic carrier must be structured output/receipt data such as
`{:mode :report-only :findings N :instrument-status :pass}`.  C216's
`script-exit=N` line is adequate for human diagnosis but is not a typed
machine-readable verdict.  Adding more globally meaningful nonzero codes will
not survive GNU Make and would collide with the existing status exit 3.

This packet is a census only; no consumer or evidence-owned artifact changed.
