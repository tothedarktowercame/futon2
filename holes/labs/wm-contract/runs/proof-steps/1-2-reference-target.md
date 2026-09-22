# Proof ⟨1⟩2 first handoff: reference-target choice (read-only)

Read 2026-09-22 from: `data/wm-ticket-queue/queue.edn`, `ticket_queue.clj:89-115`
(`plan` — the eligible stratum is the first *admitted* front entry),
`policy.clj:358-374` (queue plan feeds eligible targets), the mission registry
via substrate-2 (`mission_registry.clj:458`), `holes/missions/*.md`,
`data/wm-learning-trials/attempts.edn`, and the retained closes under
`data/wm-full-loop-machinery-69/70/71/`. Nothing frozen, no candidates
written, queue untouched.

## Recorded outcome evidence available (⟨2⟩4), counted

- `attempts.edn` contains **exactly one** recorded trial: selected cascade
  :C1, pattern `apparatus/one-authority-per-question`, token
  `[M-aif-policy-conditioned-eig :hole/h6378c65a4012]` (the shared-updater
  hole), increment `{:success 1, :failure 0}`, meaning-pinned
  (`meaning-sha256`, `declaration-sha256`), `:mode :record-only`,
  `:consumption :not-authorized` (the ⟨1⟩3 contract amendment is the
  precondition for using it).
- One grounded-change close with measured outcomes: machinery-69 attempt-002
  (click 1, run 1789964661, 2026-09-21) — after-build measurement 3 of 3;
  its run record (`data/wm-runs/tick-run-record-2026-09-21-1790033693.edn`)
  holds 10 `:observed true` / 15 `:observed false`.
- **Zero** recorded trials for the `apparatus/done-is-observed-running`
  family (M-f11): r4-2 selected it but closed `:guardrail-refusal` with
  after-build `:missing` — no trial was written.
- **Zero** trials for the repair-ticket family.

So compatible recorded outcome evidence exists for exactly one family:
**M-aif-policy-conditioned-eig / one-authority-per-question**.

## Candidates examined

### 1. T-repair-occ-444fb018… (the front repair ticket) — RECOMMENDED
- **Eligible stratum:** it IS the front entry of the queue (the only one).
  Today it reads `:not-admitted` (no candidates constructed for it;
  `cascade_proposals.clj:136-174` declines repair targets while the
  repair-closure observation is unavailable), so selection falls through.
  ⟨1⟩2's offline authoring (⟨2⟩1) is what makes it the admitted front
  stratum — under Joe's front-of-queue rule this is the natural first target,
  and choosing it does not strand the front ticket.
- **Task:** verify or restore the guardrail-refusal precondition from click 3
  (the held-out split for the EIG calibration route); retain a dated recheck
  if it has cleared. **Two different first actions:** (a) a dated verification
  recheck of the precondition with its source and result — a futon2 record
  task; (b) restoration work: author the preregistered held-out split
  locator and its no-result handling so the precondition exists — futon2 code
  + record. Different first actions, same acceptance.
- **Feasibility (⟨2⟩3):** the ticket's own acceptance is satisfiable either
  way ("verify … restore it if it remains unavailable … if it has cleared,
  retain a dated recheck"), single repository (futon2), evidence that exists
  (the finding, the mission text, the click-3 records). Neither action needs
  a second repository or nonexistent evidence.
- **Outcome evidence:** none for the repair family itself, but the parent
  mission is M-aif-policy-conditioned-eig — the one family WITH a recorded,
  meaning-compatible, successful trial (see above).

### 2. M-aif-policy-conditioned-eig, EIG-packet hole (h42fceb4ac, line 245) — rejected
- Not behind an admitted front ticket (mission stratum), selectable.
- **Two actions:** (a) persist the shadow collector's replay artifacts
  (d1e9e96b's collector exists) — feasible in futon2; (b) produce the
  held-out log-loss/Brier and predicted-vs-realised entropy reductions —
  **infeasible today**: the post-split outcomes do not exist, which is
  exactly why click 3's author correctly refused
  (`:prospective-held-out-evidence-unavailable`). A reference target whose
  natural second action fails ⟨2⟩3 repeats click 3's failure on purpose.
- Outcome evidence: the one trial is for this mission (different hole,
  same pattern family) — the only point in its favour.

### 3. M-f11, ordinary-acceptance hole (line 101) — rejected
- Selectable (mission stratum). Task: complete F11's ordinary acceptance,
  persist runtime validation evidence. Two actions exist (persist the
  applied-find receipt replay; run and persist the full committed 24-pattern
  ordinary gate packet), both single-repo futon2.
- **But:** heavy (the receipt section itself says the full 24-pattern run
  remains outstanding), and the mission's pattern family
  (`done-is-observed-running`) has **zero** recorded trials — ⟨1⟩3's B would
  have no compatible data at all.

### 4. M-f11, repair-024 successor hole (line 102) — rejected
- Two actions: retain the typed failure without resolution (feasible,
  trivial); publish the strict successful successor link (feasible only if a
  successful successor exists — unverified, and the mission text says the
  successor judgment remains outstanding). Weak discrimination between
  actions, and the same zero-trial problem as #3.

### 5. M-G-wm-wiring CP items — rejected
- Eight open holes, but the campaign is explicitly multi-repository (p4ng,
  mathlib4/DarkTower, futon3/library, futon2). Every CP item I read requires
  work outside futon2, failing ⟨2⟩3's one-repository feasibility for both
  actions. Zero recorded trials.

## Recommendation

**T-repair-occ-444fb018…**, the front repair ticket, as the reference target.

Reasons: (1) it is the front of the queue under Joe's own rule, so the
reference input exercises the real stratum mechanics instead of routing
around the front ticket; (2) it has two genuinely different first actions
whose acceptance is satisfiable in one repository with existing evidence —
the ticket's acceptance is written to close on either verification or
restoration; (3) its parent mission carries the only recorded,
meaning-compatible outcome trial, so ⟨1⟩3's B starts from real data;
(4) the alternatives each fail a ⟨1⟩2 precondition on purpose (click-3's
infeasible second action, zero-trial families, or multi-repo scope).

Caveats to carry into ⟨2⟩1: the ticket's candidates must be authored offline
(the live constructor declines repair targets today); the trial's
`:consumption :not-authorized` must be amended (⟨1⟩3 ⟨2⟩3) before B can read
it; and the discrimination in ⟨1⟩3 will lean on the two actions' different
predicted observations, which the interpretation must state explicitly.
