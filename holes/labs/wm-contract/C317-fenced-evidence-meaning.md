# C317 — what drain verdicts mean under a writer fence

Date: 2026-09-01. Owner: `wm-organization`. Assessment only; no checker was
converted or repaired.

## Finding

C315's **9/14 (64%) reachable hybrid-window rate is not a measured error
rate**. It does not mean 64% of verdicts are wrong. It means that, without a
common snapshot, movement detector, or held writer fence, 64% of the sampled
applicable programs can combine mutable observations that never coexisted.
That is enough to reject an unconditional reading of a bare `PASS`.

During the quiet run, exposed checks are operationally sound **only
conditionally**: the C305 fence must begin before their first observation,
cover every writer and mutable authority they read, and remain held until the
last evidence is persisted. C292 and the basis-aware bounded receipts observe
consequences of that fence; they do not prove the human/operational premise.

The 75 unexamined C315 candidates impose a second qualification. The fence
covers the five source repositories, lane/Agency dispatch, ordinary and
bounded jobs, the paper publisher, and the scheduled writers classified in
C309. It cannot yet assert that every unaudited program has no dynamic read
outside that set. Therefore the drain supports “all declared fenced inputs
were stable,” not “all possible inputs in every executable were frozen.”

## What invalidates the conditional claim

Any one of these makes the affected evidence unsound or unavailable:

- an acknowledged writer resumes, an unregistered agent writes, or the
  coordinator dispatches after `FENCE-HELD`;
- a timer, embedded coordinator, generator, operator shell, or job changes a
  fenced repository/population and escapes the checkpoints, including ABA;
- a check reads a mutable service, corpus, configuration, clock-sensitive
  default, or external repository outside the declared fence;
- a new ordinary/bounded job starts, even if it finishes between list calls;
- the fence begins after a check's first read or is released before its final
  report/certificate write;
- the production click writes anything outside its exact authorised binding,
  run-record, trace/store, observer-receipt, and certificate set.

When one occurs, do not reinterpret the same outputs as a weaker global pass.
Preserve them, classify the breach, and restart or return unavailable under
C305's phase-specific rules.

## Wrong verdict versus unattributable evidence

Both outcomes exist; they require different language.

**A verdict can be wrong.** Dynamic population checks can omit a newly added
violation and return a false clean result, retain a repaired finding and return
a false failure, or compare an edge/declaration against the wrong version of
its endpoint/registry. C308 established these reachable cases for the
preemptive corpus, control map, contract lint, portability lint, R2 corpus,
absence lint, Q interface, and lane registry. A held complete fence removes
that race during the drain; without it, these are not merely provenance gaps.

**Evidence can instead remain locally true but unattributable.** A finding
about bytes actually read may be correct while its total, population, HEAD, or
“current workspace” label names no state. A single captured-file or immutable
Git-object witness usually retains this narrower truth. It cannot be promoted
to a workspace claim unless its captured identity is recorded and belongs to
the fenced basis.

**Movement-aware checks should refuse rather than lie.** The workspace gate,
C292, and bounded receipts report moved/unavailable bases. The click observer
reports an interval/delta rather than pretending its endpoints are one state.
These results are attributable as observations of movement, but are not a
passing workspace verdict. Undetected ABA remains conditional on the fence.

## Text to accompany Joe's evidence

Use this statement with the quiet-run handoff; a bare `PASS` is insufficient:

> These verdicts were produced under the C305 writer fence held from
> `<FENCE-HELD UTC>` through `<FENCE-RELEASE UTC>`. The attached manifest names
> every acknowledged writer and parked coordinator/timer; C292 reported five
> clean repositories, four idle lanes, and zero ordinary/bounded jobs at each
> checkpoint. Gate and suite receipts record stable start/finish content bases;
> no fence breach was observed. The production phase allowed only the exact
> click/run-bound operational outputs named in the certificate. The claim is
> conditional on that declared read/write boundary. Seventy-five lexical
> hybrid-window candidates remain unaudited, so this evidence does not claim
> that an undeclared external mutable input is impossible.

The handoff must include the fence interval, acknowledgement/park manifest,
checkpoint results, four/five-repository bases as applicable, terminal job
receipts, exact authorised production outputs, and any limitation. Omitting
the conditionality turns procedural evidence back into the unconditional
claim the hybrid-window audit disproved.
