# Refusal totality: the self-heal route's refusal sites against SelfHealRoute rev 3

claude-2, 2026-10-19/20, packet M1 from claude-5. Reading only: no Lean edits, no
runtime edits, no clicks.

Read at: futon2 `e5c5208c9` (`src/futon2/aif/meta_self_heal_production.clj` = **P**,
`full_loop_runtime.clj` = **R**, `meta_repair_coordinator.clj` = **C**,
`scripts/wm_route_check.py` = the checker); futon3c `840251d7c`
(`src/futon3c/wm/meta_self_heal_adapters.clj` = **A**, `runner_service.clj` = **S**);
mathlib4 `a84278e893` (`DarkTower/WarMachine/SelfHealRoute.lean`).

Two corrections to the packet's source list. `full_loop_runtime.clj` is in futon2
(`src/futon2/aif/`), not futon3c. `producer-invoke!` and `one-edn-map` are in
`meta_self_heal_adapters.clj`, not `runner_service.clj`.

## How a refusal reaches the record (read this before the table)

The runtime order is `run-opportunity!` (R:455-500). Before that, the click endpoint runs
a readiness preflight:

0. **Preflight** (S:336 `meta-self-heal-preflight!`, called at S:1099 before a click id
   exists). It runs `production-readiness` with `:meta-self-heal/readiness-route
   :ordinary`. A refusal throws HTTP 503 and writes no binding, so no route check runs.
1. **Diagnosis**: `prepare-diagnosis` (P:769), then `produce-diagnosis!` (P:149).
2. **Proposal and admission**: `prepare-acceptance` (P:666), then `produce-acceptance!`
   (P:496), then `admit-acceptance!` (P:564). Admission is the bounded run and then the
   review.
3. **Readiness**: `production-readiness` (P:797), or `attributable-retry-readiness`
   (P:896) on the retry path.
4. **Route**: `route!` (P:918). It checks the route terms, then prepare, then the
   bindings, then the checkpoint loop over `coordinator/coordinate` (C:216).

Steps 1 and 2 never refuse the click. Their result is an *observation*
(`:diagnosis-observation`, `:acceptance-observation`). The only way it affects the run
is that `effective-opts` leaves `:diagnosis-evidence-pin` or `:acceptance-test` unset
(R:470-478). `route!` then refuses `:production-config-required` with that absent term.
So every diagnosis and proposal reason below reaches the disposition as
`[:acceptance-test]` (or `[:diagnosis-evidence]`) at `routeRefused`, and can be seen only
inside the observation. Rev 2 made the same point: refusals flow through readiness and
`route!`.

The disposition that the binding keeps is S:636 `select-keys`: `:status :reason :route
:ordinary-selection-called? :absent-terms :before-ration-spend? :injury-id :repair-id
:attempt :expected :actual :defects :prepare`. It does not keep `:phase`,
`:ledger-write`, `:transaction-write`, or either observation. The checker's prefix rules
read `[:meta-self-heal/readiness :acceptance-observation]` from the *run record*. A refused
click has no run record, so for every refused click today the prefix is `:not-recorded`
(gap at position 0). This matches all of today's sidecars.

Column 5 gives the rev 3 state the refusal belongs to by the Lean docstrings. "chk"
means `wm_route_check.py` projects it today, "–" means it does not. Column 4 says
whether a production click supplies the deciding input: **sup** (supplied), **abs**
(absent), **harness** (supplied by the harness only), **n/a** (decided by an outside
answer, a file or the disk, not by configuration).

## Table

| # | reason | file:line | phase | deciding input (supplied?) | rev 3 state | witness |
|---|---|---|---|---|---|---|
| 1 | `:production-config-required` (preflight) | P:842 via S:336 | 0 preflight | `:repair-ports`, `:transaction-path` (sup since ddb3fcb79). The four self-heal port terms are waived under `:ordinary` | **none**: HTTP 503, no binding | unwitnessed today |
| 2 | `:ledger-unreadable` (preflight) | P:832 via S:336 | 0 preflight | ledger file loads (n/a) | **none**: HTTP 503, no binding | unwitnessed |
| 3 | `:no-durable-injury-ledger` | P:780 | 1 diagnosis | ledger file present (n/a) | `noActiveInjury` (–) | unwitnessed |
| 4 | `:diagnosis-ledger-unavailable` | P:783 | 1 diagnosis | ledger `:loaded` (n/a) | `ledgerUnreadable` via readiness (chk, suffix only); see order error 2 | unwitnessed |
| 5 | `:diagnosis-active-set-unavailable` | P:786 | 1 diagnosis | `ledger/active-injuries` computes (n/a) | **none**: readiness is then ready with `active-injury? false`, and `route!` returns nil, so the click runs **ordinary work** (see Noticed) | unwitnessed |
| 6 | `:no-active-injury` | P:789 | 1 diagnosis | active set empty (n/a) | `noActiveInjury` → `ordinarySelection` (chk via `ordinary-selection-called?`) | today's ordinary clicks |
| 7 | `:diagnosis-source-pin-unavailable` | P:163 | 1 diagnosis | ledger cause pin (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 8 | `:diagnosis-port-unavailable` | P:167 | 1 diagnosis | `:meta-diagnosis-fn` (sup, A `ports`) | `diagnosisRefused` (–) | unwitnessed |
| 9 | `:diagnosis-source-pin-mismatch` | P:46 | 1 diagnosis | cause record bytes match pin (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 10 | `:diagnosis-source-unreadable` | P:50, P:53 | 1 diagnosis | cause record parses (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 11 | `:diagnosis-port-threw` | P:184 (sub-reason of #14) | 1 diagnosis | port throws | `diagnosisRefused` (–) | unwitnessed |
| 12 | `:diagnosis-port-returned-nil` | P:187 | 1 diagnosis | port returns nil | `diagnosisRefused` (–). **Unreachable in production**: `producer-invoke!` always returns a map | – |
| 13 | `:diagnosis-port-malformed` | P:191 | 1 diagnosis | port returns non-map | `diagnosisRefused` (–). **Unreachable in production**, as #12 | – |
| 14 | `:diagnosis-unavailable` (`:diagnosis-reason` is one of #15, #49, #50, #51 or the seat's own) | P:195 | 1 diagnosis | author seat answers `:succeeded` (n/a) | `diagnosisRefused` (–) | earlier clicks (F9) |
| 15 | `:diagnosis-not-succeeded` | P:199 (default sub-reason of #14) | 1 diagnosis | seat gave no reason | `diagnosisRefused` (–) | unwitnessed |
| 16 | `:diagnosis-response-missing` | P:206 | 1 diagnosis | `:diagnosis` map in answer (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 17 | `:diagnosis-production-functions-absent` (at diagnosis) | P:212 | 1 diagnosis | answer names the 3 roles with resolvable vars (n/a) | `diagnosisRefused` (–) | F11 era, fca1c921 |
| 18 | `:diagnosis-path-conflict` | P:76, P:86 | 1 diagnosis (persist) | receipt path free (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 19 | `:diagnosis-persist-failed` | P:89 | 1 diagnosis (persist) | disk write (n/a) | `diagnosisRefused` (–) | unwitnessed |
| 20 | `:acceptance-no-diagnosis-required` | P:682 | 2 proposal | diagnosis `:not-required` | `noActiveInjury` path (–) | with #6 |
| 21 | `:acceptance-diagnosis-unavailable` (prepare-acceptance `:else`) | P:685 | 2 proposal | diagnosis not `:available`/`:not-required`. **Includes `:present`**: a caller-supplied pin with no supplied test lands here | `acceptanceUnavailable` (chk, prefix). For `:present` the chk prefix `diagnosisRefused` is wrong | unwitnessed today |
| 22 | `:acceptance-retry-not-prepared` | R:479 | 2 (retry path) | `:meta-repair-retry` in opts | **none**: retry path unmodelled | unwitnessed |
| 23 | `:acceptance-diagnosis-pin-unavailable` | P:249 | 2 proposal (receipt read) | pin shape | `proposalRefused` (–) | unwitnessed |
| 24 | `:acceptance-diagnosis-pin-mismatch` | P:256 | 2 proposal | receipt bytes match pin | `proposalRefused` (–) | unwitnessed |
| 25 | `:acceptance-diagnosis-unreadable` | P:266, P:268, P:282 | 2 proposal | receipt parses, schema | `proposalRefused` (–) | unwitnessed |
| 26 | `:acceptance-diagnosis-unavailable` (receipt read) | P:270, P:272 | 2 proposal | receipt `:status :available`, `:diagnosis` map | `proposalRefused`. **The checker projects this word to `diagnosisRefused → acceptanceUnavailable`, which is wrong here: the diagnosis was `:available`** | unwitnessed |
| 27 | `:acceptance-diagnosis-identity-mismatch` | P:274 | 2 proposal | receipt injury/source match | `proposalRefused` (–) | unwitnessed |
| 28 | `:diagnosis-production-functions-absent` (at proposal) | P:278 | 2 proposal | pinned receipt verified (F11) | `proposalRefused` (–). Same word as #17, other phase | unwitnessed |
| 29 | `:acceptance-port-unavailable` | P:518 | 2 proposal | `:meta-acceptance-fn` (sup) | `proposalRefused` (–) | unwitnessed |
| 30 | `:acceptance-request-function-unresolvable` | P:526 | 2 proposal | diagnosis vars resolve in JVM (n/a) | `proposalRefused` (–) | unwitnessed |
| 31 | `:acceptance-port-threw` | P:447 (sub-reason of #34) | 2 proposal | port throws | `proposalRefused` (chk via #34) | unwitnessed |
| 32 | `:acceptance-port-returned-nil` | P:450 | 2 proposal | – | `proposalRefused` (–). **Unreachable in production** | – |
| 33 | `:acceptance-port-malformed` | P:453 | 2 proposal | – | `proposalRefused` (–). **Unreachable in production** | – |
| 34 | `:acceptance-unavailable` (`:acceptance-reason` = #49/#50/#51 or the author's own refusal) | P:460 | 2 proposal | author seat answers `:proposed` (n/a) | `proposalRefused` (chk) | **8df4e8d2** (reason #51); fca1c921 (author's own refusal, then #50) |
| 35 | `:acceptance-proposal-source-mismatch` (at proposal) | P:467 | 2 proposal | test source namespace/path agree | `proposalRefused` (–) | unwitnessed |
| 36 | `:acceptance-prose-or-malformed` | P:470 | 2 proposal | `concrete-acceptance?` | `proposalRefused` (–) | unwitnessed |
| 37 | `:acceptance-path-conflict` (proposal receipt) | P:346, P:355 | 2 proposal (persist) | path free | `proposalRefused` (–) | unwitnessed |
| 38 | `:acceptance-persist-failed` (proposal receipt) | P:358 | 2 proposal (persist) | disk | `proposalRefused` (–) | unwitnessed |
| 39 | `:acceptance-proposal-pin-unavailable` / `-pin-mismatch` / `-unreadable` / `-not-proposed` / `-review-state-invalid` / `-identity-mismatch` / `-source-mismatch` / `-not-concrete` | P:536, 540, 547/549/562, 551, 553, 555, 557, 559 | 2 admission (re-read of the receipt just written) | receipt on disk unchanged (n/a) | `admissionRefused` (–). Reachable only if the file changes between write and read | – |
| 40 | `:acceptance-run-port-unavailable` | P:580 | 2 admission | `:meta-acceptance-run-fn` (sup since F8) | `admissionRefused` (–) | earlier (F8 era) |
| 41 | `:acceptance-review-port-unavailable` | P:583 | 2 admission | `:meta-acceptance-review-fn` (sup) | `admissionRefused` (–) | unwitnessed |
| 42 | `:acceptance-run-refused` (`:acceptance-result` = #43-#46 or a timeout `:errored`) | P:618 | 2 bounded run | run returns `:ran` with outcome, exit, summary, pin | `admissionRefused` (–) | unwitnessed. Click 4's uncompilable test was **not** this: the run was valid (`:errored`) and went to review |
| 43 | `:acceptance-run-threw` | P:589 (sub of #42) | 2 bounded run | runner throws | `admissionRefused` (–) | unwitnessed |
| 44 | `:acceptance-source-path-refused` | A:390, A:392 (sub of #42) | 2 bounded run | proposed source path new and resolvable | `admissionRefused` (–) | unwitnessed |
| 45 | `:acceptance-command-not-admissible` | A:395, A:398 (sub of #42) | 2 bounded run | argv shape, namespace agrees | `admissionRefused` (–) | unwitnessed |
| 46 | `:acceptance-unit-launch-failed` (and the un-namespaced `:acceptance-test-id-invalid`, A:393) | A:359 (sub of #42) | 2 bounded run | `systemd-run` exit 0 | `admissionRefused` (–) | unwitnessed |
| 47 | `:acceptance-test-passes-before-repair` | P:622 | 2 bounded run | test fails before repair (n/a) | `admissionRefused` (–) | off-click replay 5 (F16) |
| 48 | `:acceptance-review-refused`, two conditions | P:628 | 2 review | (a) `:review-status :refused`: the reviewer answered. (b) `:review-status :failed`: the transport or seat failed (#49-#51, `:acceptance-review-threw` P:613), so there was **no answer** | (a) `reviewRefused` (chk). (b) by the docstring this is `admissionRefused` ("without a reviewer answer"), but **the checker projects it to `reviewRefused`**: its rule requires only `review-status` non-nil | (a) **1a6de51f** (`:acceptance-test-did-not-fail-for-injury-reason`), 6cdf2a7c; (b) **36b47627, fdc08aea, 78180753** (429) |
| 49 | `:producer-seat-unavailable` | A:227 | 1/2 any producer call | seat registered (sup) | sub-reason of #14 / #34 / #48b | unwitnessed |
| 50 | `:producer-invoke-failed` | A:233 | 1/2 any producer call | transport (n/a) | sub-reason of #14 / #34 / #48b | 36b47627, fdc08aea, 78180753, fca1c921 |
| 51 | `:producer-response-malformed` | A:237 | 1/2 any producer call | answer is one EDN map with a keyword key (n/a) | sub-reason of #14 / #34 / #48b | **8df4e8d2** |
| 52 | `:acceptance-review-malformed` | P:626 | 2 review | – | `admissionRefused` (–). **Unreachable in production** | – |
| 53 | `:acceptance-review-identity-mismatch` | P:638 | 2 review | reviewer's echo matches the request | **none**: the reviewer did answer (not `admissionRefused`) and did not accept (not `reviewAccepted`) | unwitnessed |
| 54 | `:acceptance-path-conflict` / `:acceptance-persist-failed` (admission receipt) | P:346/355/358, via P:650 | 2 review (persist, after acceptance) | disk | **none**: comes after an accepted review; no edge from `reviewAccepted` to a refusal | unwitnessed |
| 55 | `:ledger-unreadable` (runtime) | P:832 | 3 readiness | ledger `:loaded` | `ledgerUnreadable` (chk) | unwitnessed |
| 56 | `:production-config-required`, port terms (runtime) | P:842 | 3 readiness | `:transaction-path`, `:repair-ports`, run port; diagnosis/proposal/review ports when no pin/test (all sup since ddb3fcb79) | `readinessRefused` (chk) | earlier (F4a/F8 era) |
| 57 | `:retry-attribution-refused` | P:914 | 3 readiness (retry path) | retry matches the last enacted entry | **none**: retry path unmodelled | unwitnessed |
| 58 | `:production-config-required`, route terms | P:937 | 4 route | `:diagnosis-evidence-pin` and `:acceptance-test` (supplied **only** when #14 and #48 succeed), `:transaction-path`, ports | `routeRefused` (chk) | **36b47627, fdc08aea, 78180753, 1a6de51f, 8df4e8d2** (all `[:acceptance-test]`) |
| 59 | `:preparation-refused` (`:prepare` holds `:coordinator/preparation-refused` C:163 or `-malformed` C:168) | P:951 | 4 route (prepare) | the ten work-order fields (sup since ddb3fcb79) | `preparationRefused` (chk) | unwitnessed under this word; c78d0f27 under the masked word |
| 60 | `:diagnosis-transaction-mismatch` | P:866 | 4 route (binding) | resumed/prepared transaction's pins equal the observation | `bindingRefused` (chk) | **c78d0f27** (masked form, before df2209a33) |
| 61 | `:acceptance-transaction-unadmitted` | P:880 | 4 route (binding) | acceptance observation `:accepted`/`:present` | **none**. **Unreachable in production**: `:acceptance-test` is set only from those two statuses (R:476), and #58 refuses without it | – |
| 62 | `:acceptance-transaction-mismatch` | P:892 | 4 route (binding) | resumed transaction's test equals the admitted one | `bindingRefused` (chk) | unwitnessed. Reachable on a resumed `transaction.edn` |
| 63 | `:checkpoint-persist-failed` (carries `:phase :ledger-write :transaction-write`, none of which the binding keeps) | P:972 | 4 route (coordination loop) | `store/persist-ledger!` and `persist-transaction!` succeed | **none** | unwitnessed |
| 64 | `:transaction-invalid` | P:691 (inside #63's `:transaction-write`) | 4 route (coordination) | coordinator's transaction verifies | **none** (masked by #63) | unwitnessed |
| 65 | `:transaction-persist-failed` | P:703 (inside #63) | 4 route (coordination) | disk | **none** (masked by #63) | unwitnessed |
| 66 | coordinator refusals inside the loop: `:coordinator/input-refused` C:237, `-resume-state-refused` C:257, `-port-receipt-refused` C:188, `-port-threw` C:194, `-malformed` C:379/381 | C, through P:963 | 4 route (coordination) | coordinator inputs, the author, review and enact ports, receipts | **none**. These results carry no `:ledger`, so `persist-ledger!` refuses `:store/invalid-ledger` and `route!` reports **#63** instead (a second masking, like c78d0f27) | unwitnessed |
| 67 | coordinator refusals that carry a ledger: `:coordinator/author-payload-refused` C:272, `-review-patch-substitution` C:292, `-enact-payload-refused` C:318, `-retry-attribution-refused` C:345, `-ledger-transition-refused` C:201 | C | 4 route (coordination) | the author, review and enact payloads | **none**. They persist, the loop carries on through the remaining phases, and the final `coordinate` is the refusal. `route!`'s return (P:983) has `:status` but **no `:reason`**, so the disposition is `:refused` with no reason | unwitnessed |
| 68 | held results `:author-failed` / `:review-rejected` / `:enact-failed` | C:268, C:290, C:313 | 4 route (coordination) | the repair author's patch, the repair reviewer's verdict, and enactment | **none**. The disposition is `:status :held` with no reason; the checker's `status != refused` → `disposition-not-projectable` | unwitnessed |
| 69 | success outcomes `:discharged`, or a reopened state | C:364, C:375 | 4 route (record) | the retry result | `routeEnacted → enacted` covers only "enacted". No discharged/reopened split | unwitnessed |

69 rows. Rows 39, 66 and 67 group sibling reasons that share one condition shape and one
outcome; counted separately, that is 82 sites.

`none` rows: **17**: #1, 2, 5, 22, 53, 54, 57, 61, 63, 64, 65, 66, 67, 68, and three
that are mis-placed rather than absent, counted here because the checker's projection
contradicts the docstring: #21 (`:present` case), #26, #48b. (#69 is partial; not
counted.)

Unreachable in production (kept in the table, not counted as `none`): #12, 13, 32, 33,
39, 52, 61.

## Unmodelled classes

1. **The author/reviewer boundary**: #14, 16, 17, 28, 30, 34-36, 42-48. Lean has the
   states (`diagnosisRefused`, `proposalRefused`, `admissionRefused`, `reviewRefused`) but
   no vocabulary for *why*. Today's five clicks all stopped here, with the same disposition
   `routeRefused [:acceptance-test]` and different causes. A route check can never
   tell them apart, because the cause sits in the acceptance observation, which a refused
   click's binding does not keep. **This needs a new inductive**, as `ReadinessTerm` did
   in rev 2: a `ProposalDefect` (transport, unreadable answer, own refusal, prose,
   source mismatch, unresolvable function) and an `AdmissionDefect` (run refused,
   passes before repair, no reviewer answer, reviewer refused on the merits). Each would
   be attached to the existing states. Placing today's two clicks: click 4's uncompilable
   test is `reviewRefused` on the merits (the run was valid). Click 5's unparseable answer
   is `proposalRefused` / transport-class `:producer-response-malformed`.
2. **The transport/seat class**: #49-51, at three producer calls. **No new state.** Its
   rule in the checker is wrong for review (#48b), and it needs the defect term from
   class 1.
3. **Coordination after a transaction exists**: #63-69. This is where the next click
   stops if a proposal is admitted. Rev 3 has the single edge `routeEnacted → enacted`.
   The runtime has held, refused (with or without a ledger), persist-failed, enacted,
   discharged and reopened. **This needs a new inductive** (`CoordinationOutcome`) and at
   least the states `coordinationHeld` and `coordinationRefused`. The two masking paths
   (#66 reported as #63, and #67 or #68 reported without a reason) should be fixed in
   the runtime before the states are written, or the table will model the mask.
4. **Persist/IO**: #18, 19, 37, 38, 54, 63-65. Before an accepted review these fold
   into the existing refused states, so **no new state** is needed. After acceptance (#54)
   and in the loop (#63) **one new state** is needed, `persistRefused`, or they fall
   under class 3.
5. **The retry path**: #22, 57. This is the known hole (rev 1 docstring: "The Lean route
   table does not cover it"). It needs its own small table, **one new state pair**
   (`retryReady`, `retryRefused`) off the start state.
6. **Preflight refusals**: #1, 2. These are outside the click record (HTTP 503). **No
   state needed**, but the table should say that `readinessRefused` and
   `ledgerUnreadable` can also be reached before any click id exists, and so are never
   route-checked.
7. **Fallthrough to ordinary work with an injury not ruled out**: #5. **One edge**, or
   (better) a runtime change. See Noticed.

## Order errors

1. **The ledger-unreadable prefix asserts an injury the runtime has not found.** Rev 3
   allows `ledgerUnreadable` only after `activeInjury → … → acceptanceUnavailable`. In the
   runtime, `diagnosis-ledger-unavailable` (#4) is decided *before* any active set
   exists, so the path should begin at a state that does not claim an active injury.
   The checker escapes this only because it projects `ledgerUnreadable` as a suffix.
2. **Readiness runs twice; rev 3 models only the second run.** The preflight (S:1099)
   runs `production-readiness` before diagnosis, under `:ordinary`. Rev 3's
   `noActiveInjury → ordinarySelection` skips readiness, while the runtime runs readiness
   (both times) first, on every click. No refusal is lost, because the only possible
   one (an unreadable ledger) contradicts "no active injury". The edge order is still not
   the runtime's.
3. **`route!` re-reads the ledger** (P:923-925) rather than using the reading that the
   diagnosis and readiness used (`diagnosis-context`, P:755). Rev 3's single
   `activeInjury` start covers two reads that can disagree. If the injury closes between
   them, `route!` returns nil and the click runs ordinary work after a diagnosis and
   proposal were spent.
4. **The checkpoint loop does not stop on a non-checkpoint result** (P:963-981). It
   checks only that the persists succeeded, so a held or refused phase is carried
   through the later phases, and a final `coordinate` runs regardless. Rev 3's
   `routeEnacted → enacted` assumes the loop ends in enactment.
5. **Placement, not edge order (the checker, not the table):** #26 and #21 (`:present`)
   are projected one phase too early, and #48b one state too late. The cause is that one
   keyword is shared by two conditions: `:acceptance-diagnosis-unavailable` at P:685 and
   P:270/272, `:acceptance-review-refused` with `:review-status` `:refused` or `:failed`,
   and `:diagnosis-production-functions-absent` at P:212 and P:278. The c78d0f27
   lesson again: a reason's name is not its condition.

## Noticed (one line each, not proposals)

- #5: when the active set cannot be computed, a loaded ledger runs ordinary work. `route!` should refuse in that case instead of returning nil.
- #66: in the checkpoint loop, a coordinator refusal without `:ledger` is reported as `:checkpoint-persist-failed`. Persist only results that carry a ledger, and return the coordinator's refusal as it is.
- #67/68: `route!`'s final map (P:983) drops `:reason` and `:phase`. Add them, and add `:phase` to S:636's select-keys.
- #48: the checker rule for `reviewRefused` should require `:review-status :refused`. A `:failed` status belongs at `admissionRefused`.
- Refused clicks have no run record, so the prefix gap at position 0 is permanent for every refused click. The binding could keep the two observations' `:status`/`:reason` pairs (S:636).
- `:acceptance-test-id-invalid` (A:393) is the only un-namespaced reason on the route.
- Unconfirmed: the preflight's `:meta-self-heal/readiness-route :ordinary` is returned in `configured` and bound as the click opts (S:1099). If it survives to `run-opportunity!`, the runtime readiness also waives the four self-heal port terms, and #56 can refuse only on `:transaction-path` or `:repair-ports`. I did not trace it into the worker.
