# C331 — independent adversarial review of C319

Date: 2026-09-01. Reviewer: `wm-evidence`. Report only. No coordinator/unit was
parked, no bounded job was launched, and no reload, click, certificate, or
restoration was performed.

## Verdict

C319 does **not** yet survive independent execution. Its broad sequence is
sound, and its refusal paths for reload, readiness, click, and certification
remain loud, but the evidence chain contains two unbound declarations, the
pre-fence manifest omits restoration authority for the coordinators, the gate
step does not expose evidence it instructs the coordinator to verify, and two
abort rows are unsafe/incomplete.

The highest-risk silent path is:

> an arbitrary fence identifier can be declared and reported event-free
> without any C321 evidence bearing that identifier.

This was demonstrated, not inferred. In the visibly unfenced live world:

```sh
clojure -M:wm-preflight --writer-fence C331-DRY-RUN
```

exited 0 and printed an event claim with `:writer-fence {:status :held, :id
"C331-DRY-RUN"}` and `:event-free? true`. C316/C319 correctly call this a
declaration rather than lock acquisition, but the execution sheet never
machine-joins that declaration to the C321 JSON. A typo, copied old
attestation, or invented ID remains a successful event-free claim.

## Non-mutating execution results

- The literal manifest commands are callable and observational.
- `python3 checks/writer_fence_evidence.py --self-test` continues to exercise
  verifiable, breach, and indeterminate outcomes.
- `make -n workspace-gate` expands to the bounded runner and named script exit.
- `make runner-reload-preflight` refused current dirty/untested state, withheld
  the reload command, reported inner script exit 1 and Make exit 2.
- `make run-readiness` remained a refusal/current-state probe; it performed no
  click.
- The click observer and certificate entrypoints retain required-argument
  refusals established by C323; their mutating paths were not rerun.

These results support command availability and refusal, not the full production
semantics.

## Findings

### 1. The actual pre-fence coordinator manifest is missing

Step 0 says to capture the actual pre-fence manifest, but records only systemd,
process, and job state. It does not query the three durable coordinator
registry/status records before stopping them. Step 9 then says to resume only
coordinators that the actual manifest says were enabled, while its restoration
loop names all three.

There is therefore no C319-local authority for which coordinators to resume.
The older C309 observation is not an actual window-open manifest. If one were
already disabled, following the displayed loop would enable work that was not
active before the fence.

### 2. Attestation and fence identity are related only by filename

The attestation JSON contains five booleans but no fence ID, issue time,
acknowledger identities, acknowledgement references, or release boundary.
`writer_fence_evidence.py` validates the booleans and does not compare an ID.
C319 gives it a path containing `$FENCE_ID`; renaming or copying a file supplies
the apparent association.

The later WM preflight independently accepts any nonempty declared ID, as the
live `C331-DRY-RUN` control showed. Thus the intended joined packet in C327 is a
human convention, not something C319's commands establish.

### 3. The fence ID is not passed into the bounded gate job

Step 3 invokes:

```sh
FUTON_WRITER_FENCE_ID="$FENCE_ID" make workspace-gate
```

`make workspace-gate` launches `scripts/run_workspace_gate_bounded.py`, which
submits the literal command `bb -cp . checks/wm_workspace_gate.clj` through
`bg.py launch-test`. Neither submitter passes the caller's
`FUTON_WRITER_FENCE_ID` with `systemd-run --setenv` or embeds it in the submitted
command. The transient service receives the user manager's environment, not a
demonstrated copy of this shell-local assignment.

Therefore C319's expected inner line `PASS (FENCE-CONDITIONAL $FENCE_ID)` is not
entailed by the displayed command. It may instead be content-only, or inherit
an unrelated stale manager environment. This is a current-code mismatch, not a
mere documentation omission.

### 4. The displayed gate result omits fields C319 requires the operator to inspect

The bounded runner prints the inner log and an abbreviated receipt containing
ID, inner/outer exits, verdict, resource status, and paths. It omits the outer
receipt's `reason`, start/finish basis, and `repository-basis-stable`. C319 says
to require clean stable outer bases but does not instruct the coordinator to
run `bg.py test-status ID` or open the named receipt.

The inner log supplies the four-repository basis, but the inner gate deliberately
does not fail on basis movement. Consequently the command can return an outer
pass while a non-Futon2 inner basis moved; C327 already established that the
full lines must be inspected. C319 names the requirement but its executable
step does not retrieve all the required evidence.

### 5. “Run one at a time” follows two adjacent launch commands

Step 4 presents both `launch-test` commands in one code block and only
afterwards says “Run them one at a time.” Pasting the block launches both before
the first `test-status` wait. Admission control may serialize or admit both,
but the document's intended sequential evidence order is not what the literal
block does.

### 6. The handoff carries a retired population claim

Step 9 still says “Seventy-five lexical hybrid-window candidates remain
unaudited.” C326 retired that unreconstructable arithmetic remainder and
replaced it with a named 68-program discovery population whose audit status
must be reconciled by name. The operator handoff would reintroduce a count the
record explicitly withdrew.

### 7. Unit/coordinator restoration population is internally inconsistent

Step 0 asks for five background units. Step 9 says “the observed three
coordinators and four units,” then starts four units unconditionally and a
fifth (`apm-closer`) conditionally. The conditionality is correct, but the prose
population is not. Combined with the missing coordinator status manifest, a
reader cannot derive one exact restoration set from the sheet.

## Abort-path findings

### Abort before parking is not “nothing to undo”

By the end of step 0, dispatch is frozen, lanes/publisher/operator sessions have
made no-write promises, an attestation file exists, and a fence ID has been
reserved. The abort table calls this a clean abort with nothing to undo. It
must at least release/close the acknowledgements and mark the attestation/ID
aborted so that the same file cannot later be presented as live authority.

### Abort after click start can resume writers while the click is still active

The row says never click again, preserve evidence, certify if possible, then
restore. It does not require the observer/click to reach a known terminal state
or establish that no production write remains in flight before restoration.
An observer timeout, lost terminal response, or click still executing can
therefore be followed by resuming coordinators/timers. That overlaps the
production write with restored background writers and destroys the fence
boundary the abort is meant to preserve.

The correct terminal condition is not “certificate possible”; it is first
“the one click is terminal, cancelled with a typed terminal outcome, or its
state is unavailable and the background writers remain parked pending owner
resolution.” C319 currently has no unavailable/in-flight branch.

## What held

- Watchdog-before-closer ordering now matches the actual `Restart=always`
  service relationship.
- Reload preflight withholds its command on current failure.
- Readiness and certificate require explicit identities rather than “latest.”
- Post-click policy correctly forbids retrying for a cleaner result.
- Restoration is intended to use the pre-fence population rather than blindly
  enabling everything; the defect is that C319 does not capture enough of that
  population to execute the intention.

## Publication/execution disposition

Do not put the parking request back to Joe from C319 as written. The minimum
blocking repairs are: capture pre-fence coordinator status; bind one fence ID
through attestations, C321 output, bounded gate command/receipt, and preflight;
make the gate receipt inspection executable; split the suite launch/wait
sequence; and add an in-flight/unavailable post-click abort state. The stale 75
claim and restoration count should be corrected at the same time.

