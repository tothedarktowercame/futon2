# Ordinary assessed-selection integration reading — 2026-10-19

**Scope.** This is a static integration reading of the ordinary (non-injury)
assessed-selection route in repository `futon2` at
`6d5a3bf7de638d60c4445fcc367d5c8af95ea178`.  It enumerates only the carrier
from outer selection through runner validation, dispatch preparation, and
terminal retention.  It is not a serving-JVM, click, dispatch, trace-write, or
terminal-ingest observation.  In particular, a source path being reachable
does not establish that it was exercised by a live run.

## Result

No executable producer/consumer break was found in the enumerated ordinary
assessed route.  There is one **documentation-model discrepancy**: the
currently retained P07b notes still say runtime selection activation is
pending, but the current production wiring invokes the selector and the runner
passes its carrier to the cascade decision.  That discrepancy is not evidence
of live execution and does not change the explicitly unverified downstream
Q2/Q8 coverage or debugger behavior.

The enumerated stops include typed validation and dispatch-preparation
barriers. This is not an exhaustive audit of runner stops or proof that every
remaining gate complies with the frontier ruling. Their outcomes are retained
as far as the cited source permits; no successful terminal observation was
claimed here.

## Enumerated carrier

1. **P07 (actual assessed basis and executable carrier).**
   `meta-live-outer-selector/select-live` makes an assessed basis only when the
   policy objective is `:partial` and its `:assessed-selection` is `:selected`,
   then seals it through `selected-policy/seal-assessed`
   (`src/futon2/aif/meta_live_outer_selector.clj:657-668`).  The receipt keeps
   the activated carrier, the partial objective/evidence, and the assessed
   basis; it supplies the selected action only if activation succeeded
   (`...meta_live_outer_selector.clj:759-810`).  A partial result is therefore
   not silently represented as a strict optimum.  `seal-assessed` preserves
   the original menu/evidence/partial receipt and marks
   `:basis-kind :assessed-population`
   (`src/futon2/aif/selected_outer_cascade_policy.clj:60-89`).

   Production does supply this selector and a provider: `production-defaults`
   defaults to `meta-live/selector`, wraps it with the resolved evidence
   provider, and installs the retained selector as
   `:outer-task-selection-fn`
   (`src/futon2/aif/full_loop_runtime.clj:310-337`).  This corrects the older
   “no runtime caller activation yet” statement in
   `logic-model/correlation.md:200-205` and
   `logic-model/consumers.md:64-68`; the current source, not those historical
   notes, is controlling evidence for this reading.

2. **Executable handoff.**  The report runner invokes the configured selector,
   checks the receipt schema and chosen/action identity
   (`scripts/futon2/report/war_machine.clj:1540-1572`), then supplies the
   carrier and a matching claimed-selection bit to
   `cascade-decision/select-and-record-cascade!`
   (`...war_machine.clj:6792-6817`).  `cascade-decision` projects that carrier
   through `selected-outer-policy/decision`; a claimed carrier missing from the
   handoff becomes an explicit `:claimed-policy-selection-without-carrier`
   abstention, rather than falling back to an unrecorded re-selection
   (`src/futon2/aif/wm/cascade_decision.clj:1332-1349`).  The projection itself
   re-verifies menu/evidence/objective pins, the assessed receipt and selected
   canonical entry (`src/futon2/aif/selected_outer_cascade_policy.clj:91-155`),
   and only then emits the executable action (`...:158-185`).

3. **P00/P10/P11 runner retention.**  The runner retains the outer receipt on
   the selected or sorry checkpoint (`full_loop_runner.clj:1046-1051`) and does
   so on the selected path before downstream readiness/dispatch work
   (`...full_loop_runner.clj:6965-6993`).  At record construction it validates
   the exact retained receipt without recomputing population, draw, or choice;
   missing, malformed, or identity-inconsistent input becomes a typed
   `:absent` receipt (`...full_loop_runner.clj:1007-1044`), which is then stored
   as `:outer-task-selection` in the terminal record
   (`...full_loop_runner.clj:1130-1155,1250-1268`).  P00's sorry-arm frontier
   handling is visible in the conditional preservation of
   `:reading-frontier` and `:loop-node-exercise`
   (`...full_loop_runner.clj:1222-1249`).  P10's failure field is retained as a
   distinct record component (`...:1261-1264`), rather than replacing the
   decision/outer receipt.

   P11 observes a selected judgement before later validation, publishes at
   most once within the runner invocation's publication guard, and records a failed trace publication separately
   (`...full_loop_runner.clj:5480-5492,5515-5572`).  Thus a later failure is
   not source evidence that selection did not occur; nor is a missing trace a
   selection veto.  This is static source reasoning only.

4. **P01 dispatch preparation.**  After a selected entry, the runner mints the
   occurrence and builds D-task context from the retained decision
   (`full_loop_runner.clj:7029-7041`).  `d-task/capture` accepts either a
   present prospective carry or a typed absent one (`:prospective-carry-not-retained`
   or `:prospective-carry-incomplete`), while still binding the minted action,
   declaration bytes, and identity correspondence
   (`src/futon2/aif/d_predecessor_task_authority.clj:38-59,84-123`).  This is
   the P01 distinction: absent predecessor learning is a measured absence,
   not a dispatch veto.  Capture can still refuse an invalid occurrence/action,
   carry receipt, or changed declaration.  The runner treats a non-captured
   result as an explicit `:author-dispatch` incomplete failure before author
   dispatch (`full_loop_runner.clj:7345-7355`); that is a real reachable
   validation barrier, but not an inferred consequence of absence.

5. **P33 terminal interpretation.**  Terminal records retain the exact outer
   selection and terminal context before the downstream dataset boundary
   (`full_loop_runner.clj:1254-1268,1308-1317`).  P33's current model row states
   that apparatus-only/unclassified terminal causes are excluded from task
   posterior updates while their counts/identities/reasons remain in the
   non-task census (`logic-model/correlation.md:22-28`; see the relevant
   consumer implementation in `src/futon2/aif/outer_target_belief.clj:81-209`).
   Nothing in this review converts an incomplete route or a missing carrier
   into task failure or success.

## Reachable barriers and absence boundary

The following are the barriers enumerated in this source-limited traversal;
the table does not establish absence of other runner or readiness gates:

| boundary | static condition | representation | interpretation |
| --- | --- | --- | --- |
| evidence provider → outer selection | provider cannot provide a valid response | explicit refusal, including `:outer-policy-evidence-provider-required` (`outer_policy_evidence_provider.clj:37-55`; `meta_live_outer_selector.clj:696-700`) | validation/provenance barrier; not a zero-valued evidence term |
| assessed carrier → cascade | malformed/unsealed/substituted carrier, or claimed selection without one | carrier refusal or typed cascade abstention (`selected_outer_cascade_policy.clj:91-162`; `cascade_decision.clj:1332-1347`) | executable identity barrier; no legacy re-selection fallback |
| retained receipt → terminal record | missing/schema/identity inconsistency | typed `:absent` retained receipt (`full_loop_runner.clj:1007-1044`) | measured retention absence, not evidence that no selection happened |
| D context → dispatch | capture refuses its explicit binding checks | `:incomplete` at `:author-dispatch` (`full_loop_runner.clj:7345-7355`) | dispatch-preparation barrier; typed absent carry alone is accepted |

No source-only claim is made about frequency, whether any barrier occurred in a
live run, trace persistence, agent availability, author dispatch, or terminal
ingestion.

## Reconciliation with the review ledger

- **P07:** current source implements the assessed carrier and production
  handoff; the historical “pending” wording is stale.
- **P01:** current capture contract agrees with the ledger's absent-carry,
  non-veto claim (`logic-model/correlation.md:274-282`).
- **P00/P10/P11:** static source retains selected/sorry evidence, separate
  failure causes, and early terminal trace state.  The ledger's stated limit
  remains correct: these are not serving-route observations.
- **P33:** only retained causal classification may affect a task posterior;
  apparatus and unknown observations remain separately counted.

## Prioritized next packet

**P34 — reconcile P07 runtime-activation documentation (one boundary,
documentation only).**  Scope only the model statements
`logic-model/correlation.md:200-205` and `logic-model/consumers.md:64-68`
against `full_loop_runtime/production-defaults → war_machine/select-outer-task
→ cascade-decision/cascade-decision`.  Replace “runtime selection activation
pending” with source-pinned static wording that says the carrier is wired, but
does **not** claim serving execution, full Q2/Q8 certification, debugger
behavior, or terminal success.  This is priority one because it is a concrete
documentation producer/consumer mismatch; it requires no source rewrite.

No additional executable handoff defect was demonstrated in this traversal.
That negative finding is not approval of all remaining stops; serving-route
measurement remains outstanding. Deferred precision/embedding work and the
repair-obligation queue were intentionally outside this review.
