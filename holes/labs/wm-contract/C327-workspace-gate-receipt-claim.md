# C327 — what a workspace-gate receipt supports

Date: 2026-09-01. Reviewer: `wm-evidence`. Assessment only; no gate, receipt,
or readiness code was changed.

## Verdict

A bounded receipt whose outer verdict is `pass` is **not by itself evidence
that “the tree is sound.”** Under a continuously held and separately evidenced
writer fence, a clean/stable inner basis, zero inner failures, and clean outer
resource evidence, the joined packet supports a narrower and useful claim:

> On the named clean repository basis, during the interval covered by writer
> fence `FENCE_ID`, every enumerated workspace-gate command and control exited
> according to its declared acceptance, the inner four-repository basis did not
> move, and the bounded runner observed no resource-limit failure. For controls
> that rely on a separate positive baseline, soundness is composite across the
> positive and negative invocations, not a property of the negative invocation
> alone.

This is the sentence safe to quote to Joe. It does not assert exhaustive
repository correctness, logical soundness of every checker, correctness of
manual exclusions, or event-freedom outside the named fence interval.

## Evidence chain

The chain has four distinct authorities:

1. **C321 writer-fence bundle.** `FENCE-VERIFIABLE` records named coordinator,
   unit, writable-handle, C292, and attestation evidence. It explicitly retains
   the unverifiable residue rather than claiming universal writer absence.
2. **Inner workspace gate.** It records start/finish provenance for Futon2,
   mathlib4, p4ng, and Futon3; enumerates its classified `.clj` inventory; runs
   its positive commands and negative controls; and reports every nonzero exit.
3. **Bounded runner.** It records the command's inner exit, resource evidence,
   and start/finish Git basis for its working directory, then distinguishes test
   failure, resource failure, and repository-basis movement.
4. **Drain/readiness procedure.** C319 requires the same fence ID, clean and
   stable bases, zero inner failures, clean outer resource status, and repeated
   C321 evidence around the gate. This procedure is what joins the first three;
   no individual file currently proves the whole join.

The outer receipt alone is weaker than this chain. The bounded runner samples
only its submitted working directory (`futon2`). The inner gate samples four
repositories, but intentionally exits 0 when its checks pass even if
`:basis-status` is `:moved` (`checks/wm_workspace_gate.clj:421-436`). Therefore
mathlib4, p4ng, or Futon3 movement can coexist with outer `pass` unless the
reader inspects the inner basis line or the drain procedure rejects it.

Likewise, stable does not imply clean: both layers record dirtiness but do not
make a stable dirty tree a command failure. C319's clean-basis requirement is
an additional acceptance condition. Finally,
`live-artifact-format-boundaries` is deliberately report-only: a nonzero
finding count can coexist with check exit 0. The current count may be zero, but
the receipt semantics permit findings.

## With and without the writer fence

### Without a held fence

A green inner exit asserts only that the sequentially executed programs
returned their accepted exits while the gate observed its endpoint provenance.
It does not establish that every program read one coexisting state. C308's
eight resistant checks can all produce wrong answers across hybrid windows.
C320 found the same reachable fixture/Lean window in 17 gate participants.
The outer runner's Futon2 sandwich cannot close other-repository, trace,
Agency, or generator windows.

C326 repaired `contract_authority_current` itself: it now brackets mathlib/Holes
reads and reports `PASS-CONTENT-ONLY (event-free unverified)` without a fence,
or a fence-conditional event claim under a named fence. That fixes C322's
specific event checker but does not snapshot the other hybrid constituents.
Supplying `FUTON_WRITER_FENCE_ID` is a declaration; the gate does not verify
that an independently observed C321 bundle with that ID exists.

Thus an unfenced green result means **accepted exits over live sequential
observations**, not repository soundness and not even a guaranteed
content-coherent snapshot.

### Under a held fence

The C305/C321 fence operationally closes the known hybrid windows if it covers
repository writers, trace producers, Agency transitions, generators,
publishers, operator processes, and all sessions with write authority for the
whole gate. Under that condition:

- C308's dynamic populations cannot change while enumerated;
- C320's fixture and live Lean inputs coexist on the fenced basis;
- C326's fence-conditional contract event claim has the named external
  authority it assumes;
- positive and negative invocations see the same baseline content, subject to
  their individual process reads and the fixed fence.

This is operational coverage, not independent snapshot mechanics. If the
fence is breached or C321 becomes indeterminate, no earlier constituent pass
survives as an interval claim.

## Constituents capable of wrong answers

The known wrong-answer population includes:

- all eight C308 checks across dynamic/mixed content or Agency windows;
  `contract_lint` and `q_interface_completeness_check` directly participate in
  the gate, while the others affect adjacent verification/fence claims;
- the 17 C320 fixture/Lean gate participants, whose separately read fixture and
  Lean states can form a basis that never coexisted;
- `contract_authority_current` when interpreted as event-free without the C326
  fence declaration (it now labels this limitation rather than silently
  claiming it);
- the C325 predicate negative controls when quoted alone. In the gate, their
  corresponding positive invocations make the composite reject a malformed
  baseline; their individual `negative-control PASS` message remains stronger
  than the standalone evidence.

C322 also established that the mutable-verdict audit is not complete by a
durable authoritative population. C326 replaces the lost arithmetic remainder
with a named 68-program discovery population, but does not audit all 68. The
receipt therefore cannot claim that every mutable verifier in the repository
has sound interval mechanics.

## Composite versus constituent soundness

The receipt currently reports a flat vector of command names and exits. It does
not type relationships such as:

- `ambiguity` positive + `c157-perturbed-entropy` negative form one composite;
- a guarded Lean fixture is reason-preserving on its own;
- a report-only lint may have findings despite exit 0;
- a manual exclusion was not executed;
- a command is fence-conditional, content-only, or independently
  snapshot-consistent.

Consequently the receipt proves aggregation of exit conventions, not a uniform
notion of constituent soundness. A reader must not promote every row to “this
check independently proved its prose claim.”

## Recommended receipt language

The operator-facing summary should say:

> `WORKSPACE-GATE PASS — FENCE-CONDITIONAL FENCE_ID. Enumerated checks and
> controls accepted on the recorded clean, stable repository basis; bounded
> resource status clean. Predicate negative controls are accepted only as
> positive+negative composites. Report-only findings: N. Manual exclusions and
> unaudited mutable-verdict programs are not certified by this receipt.`

If the fence evidence is absent or indeterminate, replace that with:

> `WORKSPACE-GATE CONTENT RUN ONLY — accepted exits observed, but a coherent
> cross-repository/event-free basis is unverified; do not use as operator-run
> readiness evidence.`

The packet handed to Joe must include, or point immutably to:

1. the C321 `FENCE-VERIFIABLE` output and fence ID;
2. the complete inner log, especially `PROVENANCE`, `PROVENANCE-FINISH`,
   `BASIS`, `SUMMARY`, individual rows, and report-only finding count;
3. the outer bounded receipt with inner/outer exits and resource status;
4. the repeated post-gate C321 output showing the same fence remained held.

Quoting only `outer verdict=pass`, `inner exit=0`, or “the tree is sound” drops
conditions material to the claim and is not supported.

