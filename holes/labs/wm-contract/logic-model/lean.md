# Lean logic-model units

## L1 — self-heal route, level 1

Module: `DarkTower.WarMachine.SelfHealRoute`.

This unit transcribes the level-1 injury-versus-ordinary routing table from
`phases.md` under the 2026-10-19 ruling: an active injury prepares diagnosis,
runs admission before readiness, and remains on the dedicated self-heal route;
ordinary selection is available only when there is no active injury. Its
finite-table certificate is decided over the transcribed finite edge set and
its finite state vocabulary. It corresponds to the run-time
`:meta-self-heal/readiness` receipt and the recorded click disposition. It
keeps click 1 as an observed-not-recorded conforming route and certifies the
durable reverted-F2c click-2 fallback as a violation rather than as behaviour.

Still pending: an admission witness carrying its bounded run result, and an
enact witness.  Their states are present in the table but have no record
witness in L1. Finding F7 (click bindings did not retain a refused click's
disposition) is landed in futon3c `baa9d55fa`: from click 3 on, a refused click's
binding carries `:runtime-disposition` and can be a record witness. Click 1's
binding predates it and carries only binding/run-record absence, so click 1
remains an observed-not-recorded witness.

Revision 2 (mathlib4 `darktower` `8a88a93105`, claude-5, 2026-10-19 19:07Z), after claude-2's
L1 reading (job `invoke-1792436329058-17707-36486211`): the first table sent diagnosis and
acceptance refusals straight to `terminalRecord`; the runtime
(`full_loop_runtime.clj:475-505`) runs readiness and then `route!` after every acceptance
observation, and `route!` is the function that refuses with `[:acceptance-test]` (click 4).
Readiness terms and route terms are now separate inductives and are not part of the state,
so `RouteState` is finite; a record's terms are reported beside its path by the producer.
`conformsPath` (starts at an injury state, ends at `terminalRecord`) and `conformsSuffix`
replace bare `conforms` for verdicts; the truncated path `[activeInjury, diagnosisPrepared]`
is pinned as a bad case. `noFallback` is scoped to clicks without `:meta-repair-retry`; the
attributable retry (`full_loop_runtime.clj:492-499`) runs ordinary work under an active
injury and is an open hole, not decided. Click 4 (`wm-click-fca1c921`, binding sha256
`0e3f984b…99e6`) is a record witness of the suffix `[readinessReady, routeRefused,
terminalRecord]`; its prefix is the serving result transcribed in PACKETS.

Revision 5 (mathlib4 `darktower` `d6f4b88ca3`, claude-2, 2026-10-20, packet M4L): adds the coordination states `coordinationHeld`, `coordinationRefused` and `persistRefused` after `routeEnacted`, so `RouteState` has 23 states (rev 4's "22" was a miscount of 20). Beside the path: `CoordinationOutcome`, placed by `CoordinationOutcome.state`, with the carrier `CoordinationObservation`. Theorems: `coordinationStates_terminate`, `coordinationStops_beforeEnactment`, `routeEnacted_reachesOutcomes`, `noFallback_rev5`, `outcomes_reachable`, `coordination_66_masked`. The table is written against M4a's record shape; the witness leg is typed none.

Revision 6 (mathlib4 `darktower` `494d0f8e6a`, claude-2, 2026-10-20, packet M6L): the route starts at `ledgerRead`, and `activeSetUnavailable` (row #5) ends the click with no path to ordinary work, so `RouteState` has 25 states. Theorems: `noFallback_rev6`, `ledgerRead_decides`, `ledgerUnreadable_withoutInjury` (order error 1 corrected), `fallthrough_5_isViolation` and `fallthrough_5_elided_isViolation`; the latter two state the reason for M6a. Order error 3 is recorded in the `ledgerRead` docstring.

Rev 6 notes (mathlib4 `4128f17061`, claude-2, M7L): `activeSetUnavailable` is reachable only by construction, because both loads run the same pure `validate-ledger`. The rev 4 witnesses read their bounded run from `[:result :runtime-disposition :acceptance-observation :request :result]`.

Revision 7 (mathlib4 `darktower` `2720d17fb8`, claude-2, 2026-10-20, packet M9L): the retry click is a separate start (`retryClick` → `retryReady` / `retryRefused`), and `retryReady → ordinarySelection` is the second of exactly two doors into ordinary work (`ordinaryWork_twoDoors`), so `RouteState` has 28 states. The child click's terminal ingest closes the injury. Holes: an abstained retry discharges as a success, and a retry that writes no run record re-awaits on every later click.

Revision 8 (mathlib4 `darktower` `2687599fb6`, claude-2, 2026-10-20, packet M10L): adds the closing law for a retry click, `RetryClickOutcome.closing`, under which only `successfulTaskOutcome` discharges, and `ClosingStatus.retryInconclusive` for a retry that did no work. `retryClose_todayDischargesAbstention` is the reason for M10a; `retryClose_disagreement` shows the law and today's code disagree on exactly the seven no-work outcomes.

Revision 9 (mathlib4 `darktower` `7b2bbf9b88`, claude-2, 2026-10-20, packet M11L): `closingToday_eq_closing` shows the code now meets the closing law (futon2 `11fe9094a`). The law also gains the re-queue edge, `RetryClickOutcome.next`, with its own re-queue budget, together with `inconclusive_requeues`, `inconclusive_abandonsWhenSpent`, `requeue_consumesNoAttempt` and `requeue_todayAwaitsForever` (the reason for M10b). Realising the edge needs changes to the ledger, the adapter's retry id, the coordinator and the runner.
Review fix `2cda33008c` (claude-5): the hole now says what the runner does with a reused idempotency key (same payload returns the earlier click's status, S:1119-1121; different payload refuses 409, S:1122-1124) and that the ledger's `:abandoned` rule (L:158-161) admits only `:attempt-limit-exhausted`, so `retryBudgetSpent` has no record shape today.

**AIF leg: typed none.** No module under `DarkTower/AIF` or `DarkTower/WarMachine` models
injury, repair or route arbitration (claude-2, 2026-10-19). The earlier sentence "L1 has the
first two" was wrong; L1 has the table and record witnesses.

**L2 (producer):** proposal by claude-2, `L2-route-certificate-producer.md` (futon2
`6e66219cf`): a sidecar `<binding>.route.edn` written from the `result-summary` hook in
futon3c `runner_service.clj`, verdict by evaluating the Lean module (`lake env lean`), gaps
typed, `:not-checked` when Lean returns no verdict. Not built.

## Ownership and the three legs (Joe, 2026-10-19)

claude-5 owns this encoding. Joe's framing: the AIF formalism (DarkTower/WarMachine) is done;
the run-time certificates should be done but may not be; the logic model is the third leg.
Sangha (the logic model, a form of social organisation in Dewey's sense), tathāgata (the AIF
formalism), dharma (the teaching from practice: the run-time certificates). A unit of this
encoding is complete only with all three: the AIF module it rests on (cited, not restated),
the transcribed table, and a producer that certifies real records against the table at run
time, writing into the record's requirements sidecar. L1 has the first two and record
witnesses; its certificate producer is unit L2.
Revision 5 review (mathlib4 `darktower` `91b819caec`, claude-5, 2026-10-20): `CoordinationPhase` gains `retry`. The record's `:phase` is the coordinator's own when it gives one, and `port-receipt-refused` / `port-threw` carry the port's phase; the retry port runs inside the final `coordinate` call, so a record can carry `:retry`. Code conforms on the other side: futon2 `3833e0706` passes `:final` for the post-loop call, which M4a had left nil.

M10b-1 accepted (claude-5, 2026-10-20 05:21Z): futon2 `6308c946a` + review `ca3893720` give L the re-queue shapes (`:retry-limit`, `append-requeue`, `:abandoned :retry-budget-spent`), so the hole's sentence "`retryBudgetSpent` cannot be recorded today" (L:158-161) is dated; rev 10 re-cites L once M10b-3 lands. The live ledger `data/wm-meta-injuries/ledger.edn` was migrated to carry `:retry-limit 3` (backup beside it).
M10b-2 accepted (claude-5, 2026-10-20 05:28Z): futon3c `e4f58cfc4` + `603170b12` give A `retry-click-id` (ordinal 0 = the A:432 literal; positive ordinals `wm-requeue-<n>-<attempt>-<repair-id>`), so the rev 9 sentence "a re-queue at the same attempt would reuse the id" (about L:182) is dated once M10b-3b mints through it. The `:retry-inconclusive` observation (T:222-225) carries no retry click; M10b-3a adds `:injury-id`/`:retry-click` and a `pending-requeue` reader, which rev 10 should cite as the coordinator's detector.
M10b-3a accepted (claude-5, 2026-10-20 05:36Z): futon2 `f579f4ef8` gives T's inconclusive observation its enacted entry (`:injury-id`, `:retry-click`) and L the reader `pending-requeue`; rev 10 cites it as the coordinator's detector for `RetryClickOutcome.next`. The coordinator edge (M10b-3b) and the adapter port (M10b-3c) are in flight; rev 10 waits for both so the `retryBudgetSpent` sentence and the "same attempt would reuse the id" sentence are re-cited together.
M10b-3b, M10b-3c and the production wiring accepted (claude-5, 2026-10-20 05:44Z): futon2 `aa3ec313b` gives C the re-queue edge (`coordinate-queued-retry`: ledger-first `append-requeue`, `:retry-requeue-<n>` receipts, `:abandoned :retry-budget-spent` with no port call), futon3c `f2e5a9fa6` gives A a retry port that queues the coordinator's click, and futon3c `7efc13bd8` + futon2 `493388b7b` carry `retry-click-id` to C and read the result's click from the latest retry receipt. Rev 10 should state that today's code meets `RetryClickOutcome.next` (replacing `requeue_todayAwaitsForever` as rev 9 replaced the closing disagreement with `closingToday_eq_closing`), cite `pending-requeue` (L) as the detector and `coordinate-queued-retry` (C) as the edge, and re-cite the two dated sentences: "`retryBudgetSpent` cannot be recorded today" and "a re-queue at the same attempt would reuse the id".

Revision 10 (mathlib4 `darktower` `92c213cdd6`, claude-2, 2026-10-20, packet M12L): the rev 9 next step is kept as `nextBeforeAa3ec313b` / `resumeBeforeAa3ec313b`, and the new `nextToday` is read from `coordinate-queued-retry` (C:227-267), `pending-requeue` (L:331-344) and `append-requeue` (L:367-404). The code meets `next` on every outcome that writes a `:retry-inconclusive` observation (`nextToday_eq_next_ofObserved`), the abstention among them. It differs on exactly `otherInjury` and `recordUnreadable` (`nextToday_disagreement`, witness `requeue_todayMissesUnobserved`): neither writes the observation (T:222-227, T:233-277), so the coordinator ends `:awaiting-retry` with nothing queued and the retry budget is never spent. No live re-queue has run yet.
Revision 10 accepted (claude-5, 2026-10-20 05:53Z): mathlib4 `darktower` `92c213cdd6`, one file, no `sorry`, build tail in the commit. Read against futon2 `493388b7b` and futon3c `7efc13bd8`: `nextToday` follows `coordinate-queued-retry` (C:247 detector, C:252-258 mint and append, C:261 abandoned with no port call, C:267 recur) and `append-requeue` (L:394-404); `observedInconclusive`'s catch-all is right because `integrate` writes `:retry-inconclusive` for every `:non-injury` classification of the enacted retry click that is not `:successful-task-outcome` (T:208-227), and the injury branch (T:231-277) writes none for the enacted entry. `nextToday_eq_next_ofObserved` is conditional but inhabited (nine constructors; `requeue_abstentionMet` instantiates it); `nextToday_disagreement` is an unconditional iff; `nextToday` is not `next` by alias (the extra branch). `requeue_todayAwaitsForever` is carried by the renamed `nextBeforeAa3ec313b`. The rev 9 bullet re-cites A:414-426, L:151-152, L:174-175, L:394-398 and S:997, all read. The commit message calls the last theorem `requeue_abandonmentMet`; the file says `requeue_abstentionMet`; no amend. The rev 10 hole (no observation for `otherInjury` or a missing record) is real and is the next code packet, M10b-4.
M10b-4 accepted (claude-5, 2026-10-20 06:02Z): futon2 `d0a890f22` + review `b5dd9269c` make T write the attributed `:retry-inconclusive` observation when the retry click's record is a different-id injury that opens (T:253-266) or repeats (T:288-297); the helper is T:185-196. The two refusal arms (T:284-286, T:299-300) still write nothing, because the record is refused, not ingested. Rev 11 (claude-2, in flight): `observedInconclusive` true for the ingested `otherInjury` arms, the refusal arms split out or documented, `nextToday_disagreement` on `recordUnreadable` alone, witness conjuncts and cites updated to `b5dd9269c`.
Revision 11 accepted (claude-5, 2026-10-20 06:04Z): mathlib4 `darktower` `04ca876dc1`, one file, no `sorry`, build tail in the commit. Read against futon2 `b5dd9269c`: `otherInjury` is the ingested arms (T:253-266 open, T:288-297 active, helper T:185-196) and is observed; `otherInjuryRefused` is the retry-click mismatch (T:284-286) and the terminal refusal (T:299-300), unobserved because the record is refused and nothing is written. `nextToday_disagreement`: `[otherInjuryRefused, recordUnreadable]`. `requeue_otherInjuryMet` instantiates the conditional theorem. The explicit `retryInconclusive` arms in both closing functions keep rev 8/9 unchanged. The hole left for the code: a refused record or a missing record awaits forever (C:247-251); whether `integrate` should write the observation before refusing is a code question, not a Lean one.
