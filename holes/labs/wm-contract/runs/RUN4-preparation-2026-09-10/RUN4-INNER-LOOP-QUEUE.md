# RUN4 inner-loop candidate queue — real eligible work, 2026-09-10

Zai-2, preparation only, under Joe's direction (relayed by Codex-17): use the
inner loop to build remaining outer-loop/system items that are not blocked,
instead of treating the four illustrative probes as mandatory. Read-only
inspection; this note is the only artifact. No worklist/registry/frontier/data
changes, no live ledger, no worker execution, no Codex-16 dispatch, no Claude.

## Sources inspected (live, today)

- `p4ng/empirics-futon/issue-board.edn`, `issue-board-next-actions.edn`,
  `issue-board-blockages.edn` (as-of 2026-09-10).
- `futon2/holes/labs/wm-contract/worklist.edn` (live; unfinished rows:
  :RUN4 :RUN13 :F10 :F12 blocked, :F11 :U88 open, :U80 :U83 :U84 blocked,
  all listed above).
- Mission carrier: `futon2/src/futon2/aif/mission_registry.clj`
  (`*/holes/missions/M-*.md`, `Status:` line; `:advance-mission` =
  engage an already-open mission's holes, `war_machine.clj:1495-1498`).
- `SESSION-IAD-feedback-interpretation-2026-09-10.md`,
  `runs/LEARNING-design-proposed-2026-09-09.md`,
  `runs/RUN4-preparation-2026-09-10/C-WIRING-REVIEW.md`,
  blockage pointers (`MORNING-BRIEF-reduction-2026-09-07.md:73`,
  `runs/U84-trace-reason-census-2026-09-10/README.md`).

## Ordered candidate queue (max 3)

### 1. U88 — implement contextual preferences and institutions (local interpretation)

- Source: worklist `:U88`, status **:open**, owner `:joe`, class `:I`, deps
  `[]`; issue-board `wm-ticket/U88`, column **:ready-for-lane**.
- Binding: **no existing mission doc** binds it (checked `*/holes/missions/`);
  draft an ordinary development mission (text below) outside the discovery
  roots — place under this run directory until the coordinator adopts it.
  Action type stays ordinary: `:advance-mission` once a mission doc is open.
- Bounded deliverable: turn the PROPOSED interpretation in
  `SESSION-IAD-feedback-interpretation-2026-09-10.md` into tested behavior —
  participants/positions/actions/information/control as data plus predicates,
  one worked local example, unit tests. **No institutional selector, no
  preference masses, no adoption, no live wiring.**
- Objective tests: unit tests for each predicate arm; a negative test that an
  unrecognized configuration refuses typed (not nil); a test that no rule
  revision path exists yet (participation-in-revision is design, not code).
- Staffing: worker Zai (Clojure), reviewer Codex (not Codex-16/-10). Owner
  acceptance stays Joe's (`:owner :joe` is the decision owner, not a blocker;
  Joe's new direction authorizes building, review/select is Codex-17's).
- Consumable post-launch: yes; also useful before it (pure code + tests).

### 2. wm-choice/learning — finish the learning design for review

- Source: issue-board `wm-choice/learning`, **:ready-for-lane**, expected
  source status `:observed-not-decided` (not a worklist row; the last unruled
  model choice). Basis `runs/Item25-model-choice-chain-2026-09-09/README.md`,
  proposed design `runs/LEARNING-design-proposed-2026-09-09.md`.
- Binding: none exists; same ordinary-mission draft route.
- Bounded deliverable: complete the proposed evidence/update/next-consumer
  design (R-A origin/review/outcome witness, R-B join, R-C failure reaching
  the next learner, R-D selection reason) to review-ready: each element names
  its producer, its persisted record, and the consumer that reads it. **No
  offline-only reopening, no inferred adoption, no registry writes.**
- Objective tests: a checklist each element must satisfy (producer exists /
  record shape pinned / consumer named); any element lacking a producer is
  listed as a gap, not defaulted.
- Staffing: worker Codex (design), reviewer Zai (cross-check against
  TN-APM-memory-first-transfer-probe R-A..R-D vocabulary).
- Consumable post-launch: directly — this is the outer loop's learning semantics.

### 3. F10 + U83 — record reconciliation of superseded blocked labels

- Sources: worklist `:F10` (:blocked, class :F) and `:U83` (:blocked, class
  :I, deps `[]`); issue-board blockages both typed
  **"superseded … verify/reconcile"**, review-column :needs-verification,
  pointers `C-WIRING-REVIEW.md:1` and `MORNING-BRIEF-reduction-2026-09-07.md:73`.
- Evidence distinguishing stale labels from real dependencies: F10's recorded
  blocker (fold opts / kernel adapter absent) is refuted by the accepted C
  wiring review; its **remaining real obligations** are the observation-model
  gap and the live-run rider, to be carried separately. U83's 73-item queue
  no longer exists and Joe already ruled those attempts; remaining work is at
  most a retirement/supersession record, not a queue.
- Bounded deliverable: two reconciliation records — each stale claim mapped to
  current evidence or explicitly carried forward — plus **proposed** worklist
  corrections for owner acceptance (no worklist edits in the mission itself
  without acceptance).
- Objective tests: every blocker claim in each row is dispositioned
  (refuted-with-pointer / carried-forward-with-owner); no new implementation
  is proposed by either record.
- Staffing: worker Zai, reviewer Codex.
- Consumable post-launch: yes, and helpful before it (cleans the board the
  launcher reads).

## Excluded, with reasons

- `wm-ticket/RUN4` and launch wiring: the launch prerequisite itself; must not
  depend on the unlaunched run.
- `wm-ticket/F12` (+ its maths): Codex-16, busy with Joe's maths.
- `wm-ticket/U80`: Codex-12 (hole census).
- `wm-ticket/U84`: census owned by Codex-17; and its unblock action (run the
  genuine trace producer to accumulate ≥20 reason-bearing records) needs live
  WM ticks — run-gated until launch, correctly excluded from a pre-launch queue.
- Terminal reader / serving-controller integration: Codex-10 owned.
- `wm-defect/3/r16-engine-wiring`: no remaining source repair (accepted at
  `vetting/C498-r16-drawing-repair-acceptance.md`); full-paper PDF release is
  separate.
- `:F11` (open): named-hole readiness work tied to the RUN4 gate vocabulary
  conflict (issue-board design finding 1) — a reviewed contract change, not
  ordinary inner-loop construction; leave to the coordinator/Joe.
- `:RUN13`: no next-action entry; not enough current evidence to queue.

## Draft mission text (candidate 1; ordinary, outside discovery roots)

```
# M-u88-contextual-preferences (DRAFT — not adopted, not in a discovery root)

Status: draft (proposed for adoption by the RUN4 coordinator under Joe's
2026-09-10 direction; adoption opens it to ordinary :advance-mission work)

Objective: implement the contextual preferences/institutions local
interpretation of SESSION-IAD-feedback-interpretation-2026-09-10.md as tested
behavior: participant/position/action/information/control data, typed
predicates, one worked local example. No institutional selector, no preference
masses, no live activation, no adoption claims.

Done means: unit tests green; typed refusals for unrecognized configurations;
the worked example passes; scope exclusions restated in the closing record.
```

(Candidates 2 and 3 get equivalent drafts at adoption time; their objectives
are stated above.)

## Readiness gaps (concrete)

1. No candidate has an existing open mission doc, so none can be bound to
   `:advance-mission` today; each needs coordinator adoption of a draft
   mission first (one file placement + Status line — ordinary, no new action
   type).
2. U88's worklist owner is `:joe`: the implementation can proceed as dev work,
   but acceptance/close of the row needs Joe.
3. Candidate 2's review gate is the model-choice chain record — the reviewer
   must check against `Item25` rulings, and the design must not reopen
   ruled questions (offline-only).
4. Candidates' F10/U83 outputs are proposals only; worklist mutation needs
   owner acceptance, so their missions end at "reconciliation records
   delivered", not "rows changed".
