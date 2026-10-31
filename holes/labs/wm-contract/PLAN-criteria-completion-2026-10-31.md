# Criteria completion: execution plan, owners and decision boundaries

Owner: claude-1 from 2026-10-31 14:29Z, by Joe's transfer from codex-11 ([handoff](HANDOFF-codex-11-to-claude-1-2026-10-31.md)); codex-11 wrote this plan and its coordinator duties below now fall to claude-1. Date: 2026-10-31. This is an execution plan against GRID-wm-criteria-completeness-v2.md, not a replacement specification or evidence that its criteria already pass.

## Lead amendment (claude-1, 2026-10-31)

The criteria, deficit rows and evidence rules stand. Sequence changes:

1. **adf04df0 takes the medic route.** Two diagnosis refusals are treated as its one self-heal try. Joe's standing rule is that a repair made from outside gets a medic stamp and is not reprocessed. `open-out-of-band-route!` runs before diagnosis, and its historical pin is the injury's own cause record, so no supplemental carrier or cause-entry change is needed. The fix is c96bd9871. If the acceptance check refuses, the refusal is reported and the supplemental-carrier amendment in delivery-evidence-review-zai5-2026-10-31.md is the fallback.
2. **Retention is merged (f96f0ce23) and gates nothing.** Future failures carry the pin in the source record. A guarded reload is owed before the next click.
3. **Three lanes.** Live: one click, merge or reload at a time. Source: one deficit row per worktree, merged only between clicks and frozen during P6. Evidence: read-only, may run during clicks. P7 rows no longer wait behind P4.
4. **Queue rule.** Every dispatch names its row and closing condition. A new defect gets a row and an answer to "does it block the next dispatch?"; if not, it waits.
5. **Handoff tests read what the receiver reads.** A test ending at the producer's return value does not establish delivery.
6. **P6 is a check, not a target.** It starts once ordinary clicks have stopped needing fixes.

## Latest execution checkpoint: P4 held

P2 is complete as a scoped evidence account; P3 produced one additional deficit closure (now9closed/15non-closed). Two P4 parent clicks,0930ccf1 and023dde8b, consumed two credits; neither claimed a repair attempt. Latest receipt-backed balance is **12 available**. P4 is held on the producer's `:cause-scope-unretained` refusal. **No automatic third parent; P5/P6 are not released.** [Hold and evidence question](P4-HOLD-after-023dde8b-2026-10-31.md). Earlier starting figures and conditional assignments below are preserved as history.

## Starting position

- Twenty whole-machine criteria A1–E4; 24 subsidiary deficit rows. Eight deficit rows closed, sixteen non-closed. These are different counts.
- Two charged clicks since renewal: 3415 grounded-progress; eaa9 incomplete. Neither establishes newly measured completion of the selected task. Last verified credit balance 14; refresh before any issuance.
- One effective injury adf04df0, attempt 1 previously verified unspent. Descriptor typing fix is integrated; a valid delivery and supported recovery are still owed.
- Cap source review and focused tests: Kimi 象-2, job invoke-1793414052868-8579-a7748c85, 43 tests / 262 assertions passed. Exact cap f358bfe5c merged as 5197d9f6c. Serving activation/readiness has not yet been established to this coordinator. An accidental broad test invocation reported a futon6 quarantine; agent must reconcile its actual side effect before the packet is complete.
- Existing evidence closes ticket-source scope and cross-run repeat prevention, not whole B1/E3 or sustained loop behavior. R28 remains a producer-specific evidence question; no new field requirement is authorized.

## Work order and dependencies

| Packet | Lead / independent check | Work and concrete output | Dependencies / finish condition | Criteria | Cost and state |
|---|---|---|---|---|---|
| P0: publish this plan and maintain one queue | codex-11 | This plan; named dispatches; current milestone/blocker account; canonical tracker reconciliation | Done when pushed and linked to Joe; update on material transitions | D4, coordination for all rows | No clicks; current work |
| P1: cap integration and recovery readiness | Kimi 象-2 / its final successor review follows zai-5 predecessor review | Review exact f358; preserve unconditional production caps; merge/push; canonical guarded reload; actual admission opts and effective injury validation; side-effect incident account | Receipt must show source identity, protected hashes, cap=1, attempt unspent, exact command/cost. No hidden auth workaround; access failure returns a precise blocker | A2,A3,C2,D3,E4 | Already assigned job8579. No live POST authorized by that packet |
| P2: twenty-criterion evidence matrix | zai-6 / codex-11 | Machine-readable and readable per-run matrix for 3415 and eaa9; each of 20 rows gets status, exact record path/key/value/hash, expected predicate, intervention, branch explanation; retain Q verdicts separately | Independent of P1. First finish one complete matrix, not a new extractor framework. Then audit R28's correct producer for an existing qualifying witness | All A1–E4, especially A4,B2–B4,C1–C4,D1–D4 | Assign now; read-only evidence, isolated docs branch; zero clicks |
| P3: eliminate stale deficit work | zai-5 / codex-11 | Clause-by-clause evidence audit of six named historical/implemented rows below; propose close / live witness owed / actual source blocker, with pins | Independent of P1/P2. No source repair, no expanded search after one bounded pass; missing evidence is a result | A2,A3,C3,D3,E4 | Assign now; isolated docs branch; zero clicks |
| P4: one bounded injury recovery | zai-4 execution / zai-5 outcome review, codex-11 release | Use the supported ordinary entry into self-heal; at most one persisted author attempt and one retry child; retain parent/child binding, author/review/enact, terminal and ledger delta | P1 genuinely ready; no unresolved test-side-effect or serving-access claim. Freeze expected branch and criterion observations before issuing. If cap is already spent, stop instead of allocating another attempt | A1–A3,C1–C4,D3,D4,E1,E4 | Proposed next execution; one parent credit, child credit-free. Expected balance14→13; no live dispatch by this plan |
| P5: finish the next ordinary task path | zai-4 execution / zai-6 extraction / zai-5 independent outcome check | Let supported selection choose; obtain valid artifact descriptor, contract/baseline, review at exact revision, measured selected wants and truthful terminal. If P4's child already traverses this path, reuse it | P4 disposition permits ordinary operation; snapshot selected completion contract at actual pre-author boundary. No forced winner, fabricated contract or repair-success-as-task-success | A4,B1–B4,C1–C4,D1–D4,E1 | Conditional. At most one additional ordinary credit before reassessment; do not spend just to duplicate child evidence |
| P6: demonstrate a bounded loop | zai-4 execution / zai-6 across-run account / zai-5 review | Predeclare three consecutive ordinary runs under one envelope. Extract successor consumption, target/refusal identities, useful work or explained exhaustion, T12 evaluations, resources, budget and intervention log | P5 yields useful measured behavior and no unresolved path blocker. Freeze sequence before first run. Any corrective code/state intervention ends that sequence; retain failures rather than resetting them silently | E2,E3,E4, with A–D applicable on every run | Conditional checkpoint, up to3 credits, one at a time. Three is a planning sample, not a theorem or perpetual-operation claim |
| P7: complete remaining criterion gaps | codex-11 assigns zai-4 implementation / zai-5 review / zai-6 witness | Work the residual rows in the ordering below, each with first divergence, one bounded patch if needed, discriminating test, serving proof if relevant, then qualifying live evidence | Start only from P2/P3 findings or an observed P4–P6 divergence. No broad machinery project disguised as next-click prerequisite | Explicit row mapping below | Cost depends on witnessed branch; cannot promise all rows close within remaining credits |

P1, P2 and P3 proceed in parallel. P4 depends on P1, not on every unrelated audit. P5 follows recovery; P6 follows a measured task path. P7 is the residual queue, not an automatic detour before every click.

## What each of Joe's twenty criteria will be demonstrated by

| Criterion | Evidence owner | Required evidence / planned boundary |
|---|---|---|
| A1 objective, scope, credit, acknowledgement | zai-4 captures; zai-6 joins | Before/after ledger, issued objective, accepted binding, exactly one charge per admitted parent; refused launches explicitly uncharged |
| A2 serving envelope | Kimi at recovery handoff, then zai-4 | Actual loaded identities and resources, heap, seats, provider, queues; list unknown/unregistered components rather than asserting complete coverage |
| A3 blockers and incompatible work | zai-4 | Authority-aware effective injury set and actual admission opts, idle/queue/stop state; per-run disposition of known defects |
| A4 completion and baseline | zai-4 retains; zai-5 verifies | Selected token census, locators and before-state at revision before author dispatch; typed missing contract stops task credit |
| B1 declared field | zai-6 | Pin Giraffe manifest and 608 scope; join actual admission/exclusion, statuses and readiness; reuse closed source-scope evidence without claiming larger field coverage |
| B2 knowledge reaches decision | zai-6 | Chosen task's producer, reading response, redecision and lineage. R28 witness must exercise the original selected-target-reading failure path |
| B3 justified choices | zai-6, zai-5 checks | Actual outer cascade scores, G inputs and tie status; separately applicable inner scores/Q obligations. Do not call a tie a unique minimum |
| B4 prospective prediction | zai-6 | Pre-effect prediction distribution and its input pins; outer historical closure does not discharge inner predictions |
| C1 execution identity | zai-6 | Join selected task/action/occurrence to construction and author request |
| C2 checkpoint before effect | zai-5 | Durable baseline/checkpoint before dispatch and supported continuation. Explicitly exclude an exactly-once external-contact guarantee across the known crash window |
| C3 real author artifact | zai-4 captures; zai-5 verifies | Full SHA, correct repository, reachable committed artifact/digest or independently verifiable already-satisfied result; labeling repair is not valid delivery |
| C4 review and outcome | zai-5 | Review the selected artifact and completion contract at the pinned revision; real build/falsifier or qualified no-build outcome; mutation controls checked separately |
| D1 measured original wants | zai-6, zai-5 checks | Before/after per selected token at reviewed revision; no convenient recensus; absent measurements remain unobserved |
| D2 pattern checks and learning | zai-6 | Executable token/pattern observations and actual learning writes, distinct from mission completion and repair outcome |
| D3 consistent close/injury state | zai-5 | Terminal classification, run-stream, predecessor carrier and ledger agree; preserve historical mislabeling instead of rewriting it |
| D4 complete operator account | codex-11 integrates | Per-run20-row matrix, per-Q results, record pins, budget, unresolved requirements, interventions, parent/child accounting; report UNKNOWN explicitly |
| E1 bounded termination | zai-4 | Durations, terminal/managed stop, resources and final service state; safe refusal is not useful completion |
| E2 successor learning | zai-6 | Actual next-state consumption with exactly-once receipt and differing state pins; reuse observed E2 deficit closure, account separately for skipped history |
| E3 useful repetition/exhaustion | zai-6 | Across-run choices, progress and explicit refusal/exhaustion identities; T12 evaluated history rather than silence. Closed repeat deficit alone is insufficient |
| E4 conserved loop without manual repair | zai-5 | Consecutive envelope, budgets, persistent state and jobs conserved; no corrective intervention inside qualifying sequence. Repair-specific E4 cascade deficit remains separate |

## Every remaining deficit has a place in the queue

P3's first bounded audit has these six rows, in order:

1. **C3-AUTHOR-REPOSITORY-HISTORY (C3/B2):** verify the written fdba6f19d/43ec20a7 gate witness; no retrospective terminal ingestion. Close if the exact condition is evidenced.
2. **E4-WORK-ORDER-NIL-RETURN-LIMIT (E4):** establish integration state and a real attempt using the corrected option projection; no replay-only closure.
3. **E4-TERMINAL-INGESTION-IDENTITY (E4/D3):** establish integration and actual subsequent failure identity; distinguish classification from machinery availability.
4. **E4-E43-CAUSE-SCOPE (B5/E4 as historically mapped):** find actual cascade-ready evidence through the corrected producer. B5 is not one of the adopted20 labels; relate findings to B2/C1/E4 without silently rewriting historical taxonomy.
5. **A2-RELATIVE-STORE-ROOTS (A2/E2/E4):** audit reachable roots and the stated historical split; no blanket canonical-root claim from a narrow check.
6. **B5-INJURIES-HOLD-RUN001 (A3/E4):** existing zero-injury disposition is stale for adf04df0. Separate historical clearance from current state. Current completion requires supported effective-clearance plus a subsequent start without injury stop.

Remaining ten, ordered by relation to the chosen task path:

7. **B2-D4-R28 (B2/D4):** P2 searches the affected producer's real failed-redecision evidence; if no qualifying occurrence exists, report live witness owed. Do not intentionally create a live failure merely for closure.
8. **C4-FULLY-QUALIFIED-STORE-DETECTION (C4):** zai-4 authors only after a pinned reachable loophole is confirmed; zai-5 checks fully-qualified and alias negatives plus clean control. Must be resolved before relying on the affected review protection; no assumption that all task execution must stop without reachability evidence.
9. **E4-ACCEPTANCE-PUBLICATION-ISOLATION (A2/B1/E4):** zai-4/zai-5 audit actual acceptance roots and publication deltas. If P4's acceptance path can mutate canonical tickets/stores, this becomes a P4 blocker; otherwise isolate as later work. No blind reverts or quarantine.
10. **E4-F7E97-ACCEPTANCE-SCOPE (E4/D1):** test the real selected-token construction/baseline/after-observation boundary, not fabricated verified outcomes. Prior failed self-heal is retained; no second attempt to improve machinery.
11. **D4-READING-REFUSAL-FINDING-CONTEXT (B2/D4):** carry real target, typed reason, source/job pins into the finding if missing; close on a real relevant refusal. Do not substitute a generic environmental cause.
12. **OUTER-DISPATCH-CLASSIFICATION-ABSENT (E2/D4):** fix is already integrated; verify that fact, then await a qualifying actual dispatch-failure close. Do not repair it again or force failure to earn credit.
13. **D4-MEDIC-ONLY-REPORTING (D4):** inspect P4 if it takes the medic-only branch; otherwise reuse historical actual medic evidence. Correct only a demonstrated false ownership/missing-ordinary claim, and retain applicability explicitly.
14. **E3-T12 (E3):** P6 captures the qualifying history and explicit firing/non-firing decision. No criterion credit for a silent detector or implementation-only test.
15. **OUTER-LEARNING-SKIPPED-RUNS (E2):** separate ordered recovery packet, inputs and exactly-once plan reviewed before any replay. It does not block future correctly accounted successors absent a concrete dependency; no historical state mutation during the live sequence.
16. **E4-SELF-HEAL-CASCADE (E4):** larger residual capability, not smuggled into P4. Inventory S1–S7; reuse existing S2/S3/S4 evidence; remaining S4b qualification/retirement, S4c route choice, S5 step effects, S6/S7 closing evidence each gets one packet in dependency order. Existing G scorer only. Final witness must be a live injury repaired through a G-selected cascade with per-cascade G. This alone still does not prove across-run E4 conservation. Agent zai-4 authors, zai-5 independently reviews, zai-6 extracts live evidence; dispatch only after the inventory shows which slices are actually missing.

## Execution, credit and stop rules

The14 available credits are authorization capacity, not a target to exhaust. Immediate checkpoint ceiling: one recovery parent + at most one ordinary task attempt + up to three ordinary sequence runs = at most5 additional parent charges, leaving at least9 if the starting balance remains14. Reuse child evidence and charge fewer where possible. This is a planning ceiling; freeze and release each actual dispatch separately under Joe's standing authorization, without asking Joe to reauthorize routine steps.

Only one live run at a time. During it, other agents may read retained artifacts and work in isolated worktrees, but no serving reload, canonical mutation or shared live-store test. Tests are narrow and hermetic. No canonical merge until idle is verified. External side effects and repair-attempt consumption are separately accounted from click credits.

If recovery's one attempt fails or its one child is inconclusive, report the exact held state and stop that injury route. No attempt2, no silent raised cap, no automatically minted new injury to evade the rule. One first-divergence packet maps the blocker to criteria and presents the supported next choice. If no supported path remains under existing authority, that is a real operator decision; do not burn the remaining credits.

New defects enter the existing queue with: affected criterion, reachable path, earliest divergence, evidence pin, proposed bounded correction and whether they actually block the next dispatch. Reporting inaccuracies, unrelated backlog and speculative risks do not automatically preempt the task. A material implementation correction needs independent review; docs-only stale dispositions can be corrected directly from evidence.

Do not promise all20 rows will pass on every branch: NOT-EXERCISED must name why, and additional branch coverage remains owed. The final deliverable is a criterion coverage matrix across the declared runs, a deficit disposition register, all interventions and spend, and an honest decision among READY TO ATTEMPT / THIS RUN WORKED / LOOP DEMONSTRATED. Full completion is not established while required rows remain UNKNOWN/VIOLATED or a qualifying sequence requires corrective intervention.

## Progress visibility

codex-11 reports milestones in this form: packet and agent; last concrete artifact/test/run; current step; next completion condition; blocker and cost. Distinguish queued, running, source-approved, merged, loaded, live-witnessed and closed. An accepted dispatch is not completed work. Check active jobs when responding to status questions and during extended work; ask agents for updates at review, merge, live verification and terminal outcome. Do not imply unattended polling or completion deadlines that have not been scheduled.

Outputs live in Git under holes/labs/wm-contract, with exact commit/run/hash references and raw local evidence paths where necessary. Each evidence audit must produce a portable summary; no external reviewer should have to reconstruct its conclusions from a notes-only conversation. Keep the original GRID and closure conditions unchanged unless Joe explicitly revises them.

## Dispatch register

- P1 Kimi 象-2: `invoke-1793414052868-8579-a7748c85`, running at last status read; cap merged as5197d9f6c, serving readiness still pending coordinator receipt.
- P2 zai-6: `invoke-1793414889578-8599-885160fe`, accepted/queued. Two20-row live-run evidence matrices, then bounded R28 witness search; no runtime changes.
- P3 zai-5: `invoke-1793414890003-8600-ffa34c0b`, accepted/queued. Six-row historical evidence audit; no source fixes or tracker mutation.
- P4–P7: assigned owners in the plan, **not yet dispatched**. This distinction prevents a proposed work sequence from being reported as running work.
