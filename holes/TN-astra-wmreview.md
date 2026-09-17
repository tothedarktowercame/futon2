# War Machine review against futon-2026

2026-09-09 — Astra/Codex, requested by Joe.

Substantial work is landing, but the remaining work is concentrated where
implementation must become useful, witnessed behaviour. More automated
throughput alone will not close those gaps.

This was a read-only review of the local futon-2026 sources, current worklist,
recent rulings, run-readiness records, and relevant production code. No
qualification was rerun and no machine run was started. Counts and status
claims below describe the records inspected, not a fresh qualification of the
running system. Line references are those inspected on this date.

## Progress and its limits

The worklist had **198 done, 8 blocked, and 2 open**. That is progress through
the recorded tasks, not 95% completion of the War Machine. September 8's
bulletin records **346 commits, 19 status moves, and 14 run directories**, but
**zero run-era deposits and zero machine-adopted decisions**. Many commits
regenerate the paper. These measures describe different accomplishments, and
the current reporting makes them too easy to confuse.

Sources: [worklist](labs/wm-contract/worklist.edn),
[September 8 bulletin](labs/wm-contract/bulletins/BULLETIN-2026-09-08.md).

## Outstanding work that matters most

| Work | What exists | What still prevents completion |
|---|---|---|
| **Preferences, C** | Channel preferences compute; an outcome-preference calculation and a discriminating Snatch miniature exist. | Production does not supply the new calculation's inputs. More fundamentally, the fitted kernel consumes checkpoint trajectories, whereas the scorer supplies channel observations. A justified model connecting them is missing. |
| **find** | Joe's approved signature landed in Lean on September 9 (`ae9dc5b58e`). | The worklist remains open; application of the signature is not completion of the implementation and its evidence. |
| **organise** | Formal laws and worked exemplars exist. | The naturalistic exemplar is blocked by library correspondence failures, a cyclic relation, and missing authored edges. These are concrete repair work; bypassing the checks would invalidate the result. |
| **Accepted operation** | Earlier runs have reproducible conformance certificates. | RUN4's recorded readiness remains blocked; RUN13 still requires certificates tied to accepted runs. The convergence register records zero converged quantities. |
| **The paper's external-value claim** | A 32-case corpus and evaluation design exist. | Part III explicitly remains an outline; its 160-cell acceptance grid is unrun. Internal conformance cannot complete this part. |

Evidence:

- C: [worklist F10](labs/wm-contract/worklist.edn), around line 1390;
  [production options](../scripts/futon2/report/war_machine.clj), around line
  6330; [kernel observation definition](../checks/disposition_kernel.clj),
  around line 20. The production options do not supply
  `:ruled-outcome-c-enabled?`, `:seeded-c`, or `:disposition-kernel`. The
  [scorer](../src/futon2/aif/efe.clj), around line 701, passes `next-mean` to
  the disposition calculation. The kernel explicitly records that its source
  ledger does not retain channel-valued observations.
- find and organise: [worklist F11/F12](labs/wm-contract/worklist.edn),
  around lines 1400–1405; [September 9 rulings](labs/wm-contract/RULINGS-walkthrough-2026-09-09.md).
- Accepted operation: [recorded readiness](labs/wm-contract/runs/F2-run4-readiness/READINESS.edn)
  and [convergence register](labs/wm-contract/CONVERGENCE.edn). These are dated
  records; their freshness must be checked before using them to qualify a new run.
- External value: [Part III](../../p4ng/sec-evaluation-outline.tex).
- C's demonstrated scope: [paper C section](../../p4ng/sec-c-vector.tex)
  and [Snatch miniature](../../p4ng/sec-snatch-mini-c-generated.tex).

## Avoidable coordination drag

The mission's advertised one-page current status still leads with August 30
state. U83 is blocked on producing tickets for **73 pending judgments Joe
already resolved**. The September 9 bulletin lists decisions as waiting on Joe
that subsequent rulings released. These records make completed decisions look
like outstanding demands on the operator's attention.

Sources: [build status](problems/BUILD-status.md),
[U83](labs/wm-contract/worklist.edn),
[September 9 bulletin](labs/wm-contract/bulletins/BULLETIN-2026-09-09.md),
and [September 9 rulings](labs/wm-contract/RULINGS-walkthrough-2026-09-09.md).

The distinction matters: the later ruling authorizes work; an older generated
bulletin does not revoke it. Likewise, a task with an obsolete acceptance
needs explicit retirement or supersession, rather than implementation that
manufactures the old state.

## Recommended execution changes

These are recommendations from this review, not new rulings, changes to
acceptance criteria, or authorization to start an unattended cohort.

### 1. Make the next milestone one useful, complete attempt

A real problem enters; find retrieves warranted material; organise constructs
a valid cascade; declared preferences discriminate between meaningful
alternatives; the selected work produces an independently reviewed outcome;
that outcome reaches the next observation and learning step. Require evidence
at each existing boundary.

This demonstrates integration while leaving the broader formal and paper
obligations explicit. It does not make one attempt sufficient evidence for
long-term performance or completion of Part III.

### 2. Hold the already-authorized C/restart excursion next

The preparation exists. Use it to settle the intended problem, process
preferences, the missing human-feedback coordinate, and the framing of the
next attempts. Its output should be a recorded preference specification and
attempt design that builders can implement.

Joe's ruling expressly rejects restarting an automated loop to hunt for
coverage; that remains the constraint. See the
[prepared excursion](E-C-realization.md) and
[pinned Cascade Live breakdown](labs/wm-contract/runs/U88-cascade-live/U88-cascade-live-pin.md).

### 3. Give automated workers bounded prerequisites for that attempt

Finish find; sequence the approved vertex reconciliation; repair the exact
library defects blocking organise; specify and implement
observation/disposition recording and the production model connection.

Each packet should name its consuming call site and the evidence that will
show it was exercised. Dispatch through Agency, with separate author and
reviewer. Preserve the existing invariant and qualification gates; do not
substitute checkpoint trajectories for channel observations merely to make
the connection executable.

### 4. Repair stale task state and prevent fresh attestation debt

Supersede U83's obsolete acceptance, distinguish authorized work from genuinely
unresolved preferences, and make source changes carry the affected
proof-receipt updates and checks in the same reviewed delivery.

The September 7 audit found that re-attestation was trailing implementation;
merely rerunning checks could not repair changed proof-source bindings.
Re-verification is the work, followed by receipt emission and binding through
the proper machinery. See the
[red-component audit](labs/wm-contract/RED-COMPONENTS-2026-09-07.md), especially
its strict-lint and positive-receipt findings. Later rulings take precedence
over that audit's historical accepted-red proposals.

### 5. Measure useful closure per operator hour

Report accepted end-to-end attempts, newly exercised contract obligations,
outcomes actually consumed by learning, and unresolved operator decisions.
Keep commit and publication counts as supporting information.

Likewise, redirect library effort toward the blockers: the fleet's latest
baseline dispatched nothing because its first attestation pass was already
complete. More passes at that same level will not repair missing authored
relations. See the
[fleet baseline](labs/zaif-harness/runs/coverage-2026-09-08.md).

## Proposed next work cycle

Put **the C/restart session and its first complete real attempt at the centre
of the next work cycle**, with the automated lanes explicitly serving it.
That would turn the considerable implementation progress into something Joe
can observe helping choose, complete, and learn from actual work.
