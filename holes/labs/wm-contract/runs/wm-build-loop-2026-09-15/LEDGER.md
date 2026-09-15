# War Machine build loop — ledger

Lead: claude-2. Specifications and design decisions: codex-28. Implementers:
codex-2, codex-3 and codex-4. Backstop: Joe. Commission:
`invoke-1789501050276-21222-59c12ada` (2026-09-15). Each packet reports its
source, tests, independent review, serving activation and qualifying use
separately. A tick in this ledger is not a checklist tick.

| Packet | Checklist / nodes | Author | Reviewer | Agency job | State | Evidence |
|---|---|---|---|---|---|---|
| P0 reproduce the target-belief gap | WM-02 Q2/Q4, R1 | claude-2 | n/a (read-only check) | none (in-turn) | **done** | `p1-reproducer/` (readback, pins, futon2 head `93837a1e`) |
| P1 work-target belief reaches row 7 | WM-02 Q1–Q9, R1 | — | claude-2 | — | D1–D3 **decided** by codex-28 (`invoke-1789501835834-21224-7484fd75`), pinned at `declarations/wm-work-target-interpretation-v1.edn` (sha256 `055d579d…5e5e`, futon2 `1e36c0d9`); split into P1a/P1b/P1c | `P1-packet.md` (the pre-decision version) |
| P1a pure work-target belief module + domain-aware row-7 adapter | WM-02 Q2/Q4/Q6/Q8, R1 | codex-2 (`998b2b89`) | claude-2 | `invoke-1789502167375-21225-81e8d7ea`; park `park-d74a0875-710f-4996-bcde-7e838429c1a5` | **reviewed: accepted at source/test scope** after reviewer fixes F1 (alias → second belief) and F2 (predecessor vocabulary). codex-28 independently reviewed and accepted F1/F2 (`invoke-1789505103759-21232-326bf551`; p4ng `2922020`, `wm-walkthroughs/build-loop/reviews/P1a-F1-F2.json`). Gates rerun, 10/10 mutants killed. Not integrated, not serving | `p1a/`, `p1a-review/REVIEW.md` |
| P1b-1 durable work-target store (genesis, authoritative head, hash chain, predecessor-validated commit, idempotent retry, recovery states) | WM-02 Q4, E02 | codex-3 (`9ee8bbe0`) | claude-2 | `invoke-1789502848282-21230-6d90b432`; park `park-72aac1ad-e6bc-4ec9-ac2f-de09ae616b50` | **reviewed: accepted at library/isolated-store scope** after reviewer fix R1 (interrupted temp writes had read as damaged history). The fix lacks independent review. Gates rerun; 17/17 mutants as expected (3 predicted survivors = test-reach limits). **Production genesis pending separate rollout authorization** | `p1b-1/`, `p1b-1-review/REVIEW.md` |
| R1 independent review (claude-2's store fix `e38ea7e5`) | WM-02 Q4, E02 | codex-3 (reviewer) | — | `invoke-1789505882635-21236-0b88b14e`; park `park-246bff7a-df78-4a72-a53a-73d90514dc44` | **done: rejected against the full requested contract, pending one case (codex-3 F1).** R1 is correct when valid `.writer.lock` metadata survives: no temp is adopted and no committed damage is masked. But with only temp files and **no** lock file, `read-store` creates an empty lock and reports `:damaged :parse-failure`, not `:pending-recovery`. That behaviour predates R1 (lock bootstrap). Both states refuse. **F1 open, not being fixed: persistence work is held by Joe** | `r1-review/REVIEW.md` (futon2 `4a2a1b20`) |
| P1b-2a pure bridge: full payload validator (all admitted rows), activation-aware store→predecessor adapter, structured injective operation identity, deterministic proposal builder | WM-02 Q2/Q4/Q8, R1, E02 | codex-2 | claude-2 | `invoke-1789505880964-21235-707897db`; park `park-ea1f31b2-c581-41b0-b8ac-40d6cad8adb7` | **active**. Q-D…Q-G decided by codex-28 (`invoke-1789505634687-21234-0c05fba8`; p4ng `b09e3dd`, `build-loop/decisions/P1-QD-QG.md`): established-model failure stops the attempt; no in-attempt recomputation; structured scoped ids; `trace?` confers no write authority; absolute path, inert until genesis | `handoffs/P1b-2a-codex-2.md` (futon2 `cf84c1ba`); receipts to `p1b-2a/` |
| P1b-2b (proposed) strict registry acquisition that hashes the bytes read; `judge` builds a proposal (no commit); `trace-record` clause | WM-02 Q2/Q9, R1 | TBD | claude-2 | not dispatched | held on P1b-2a review | — |
| P1b-2c (proposed) explicit commit authority at audited runner/scheduled entrypoints, one protocol; caller audit of every route to commit; failure-path tests (zero construction after established-model failure, inert pre-activation, trace-only calls cannot write, stable lost-response ids, post-commit failure kept separate); activation expectation input | WM-02 Q2/Q4/Q9, R1, E02 | TBD | claude-2 | not dispatched | held on P1b-2b; the activation/recovery procedure is to be brought for review before rollout | — |
| P1b-2 judge call site (admissions → carry → row-7 adapter), `:work-target-belief` output, trace/checkpoint refs | WM-02 Q2/Q9, R1 | codex-2 (proposed) | claude-2 | not dispatched | held on P1a review and P1b-1 | `P1b-packet.md` |
| P1c Lean production carry model (monotone retention, lineage) + correspondence; the old drop theorem untouched | WM-01 K6, WM-02 Q4 | codex-3 (proposed) | claude-2 | not dispatched | held on P1a (it must model the reviewed function) | none yet |
| P2 dynamics admission separate from A admission | WM-05 C5/C6, WM-03 B7 | TBD | claude-2 | not dispatched | proposed; needs D5 | none yet |
| P3 same B at belief update and prediction | WM-03 B8 | TBD | claude-2 | not dispatched | proposed; needs D4 | none yet |

## Open design questions sent to codex-28

- **D1** What a work target's state means (mission or ticket, on the
  seven-status carrier).
- **D2** Domain authority and declared D.
- **D3** Update authority for work targets before WM-04.
- **D4** Same B at the belief update and at prediction (WM-03 B8).
- **D5** Dynamics admission separate from observation admission.

Full text: this directory's `P1-packet.md` and the bell reply.

## Findings recorded, not yet assigned

- `war_machine.clj` `previous-selection-non-progress?` reads nil target
  belief as "did not move". **Corrected 2026-09-15:** production does not
  reach this. `enrich-candidates-with-mission-value` receives
  `recent-trace-records` (a sequence), so `consecutive-non-progress-count`
  takes the outcome-only `recent-non-progress-count` branch. The belief
  comparison is used only by the one-record compatibility branch, which
  `war_machine_test.clj:948-957` exercises. The first P1 packet's claim that
  this defect affects the controller score was wrong.
- The channel predictors `predict-annotation-health`,
  `predict-mission-health` and `predict-active-repo-ratio` average over
  `(count belief)`. Adding work-target rows to the strategic `:mu-post` map
  would change existing channel predictions inside the update loop. That
  map-placement conflict is open with codex-28 (Q-A).
- `bootstrap-from-stack-annotations` swallows read failures into an empty
  section list.
- The row-7 reader gives the same refusal for an unknown id and for a
  registry target that has no row.
