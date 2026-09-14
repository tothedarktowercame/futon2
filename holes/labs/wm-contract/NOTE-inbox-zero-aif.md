# NOTE: inbox-zero as AIF — the hierarchy's base case, theorized

Joe with zai-7 (zai), 2026-09-12. The exercise Joe asked for: theorize the
inbox-zero loop (`futon0/README-inbox-zero.md`) in the active-inference
vocabulary, so it becomes a *meaningful instance* of the chip-boards
hierarchy (SPEC-chip-boards-v0.md §3½) rather than a script that happens to
run. Node vocabulary follows `SPEC-zaif-harness-v1.md`.

## The loop in one line

A standing Markov blanket around the *record* (the set of repos): each
cycle, observe every host's disagreement with the record, price it, act or
report, and terminate at the named fixed point `clean`. Homeostasis where
the regulated variable is coherence between host state and git state.

## R-node reading

- **R1 (belief μ)** — per-repo coherence belief: dirt ages, ahead/behind,
  upstream presence, worktree census, *and* a belief about live agent turns
  (who is mid-edit right now — the sweeper-vs-edit hazard makes this a
  first-class belief slot, not an afterthought).
- **R2 (observation o)** — one channel per clause of the definition:
  `status --porcelain`, `rev-list` counts, upstream resolution, worktree
  census, manifest membership, and the typed refusal record. Two-wire
  everywhere (C446's lesson): a refused sweep reports `:inbox-zero/refusal`
  or it has not observed.
- **R3 (update)** — the definition itself is the belief, and it revises by
  clause-accretion: each measured failure amends `clean`. Belief revision
  at predicate grain — the loop's R3 is unusually slow and unusually
  well-evidenced (every clause cites an incident).
- **R4 (forward model Q)** — the README's three *measured* costs are the
  yield model, in data: finished work forgotten (14 files, 2026-08-14);
  knowledge paid for twice (the invoke-activity insight re-derived over a
  morning); the dirty tree as unreliable narrator that three agents believed
  (Zone, 2026-08-14). This is what makes the exercise honest: Q is not
  guessed, it is dated.
- **R5 (G)** — per item and per arm: risk (hazard table: committing an
  in-flight edit, pushing a stale base) + ambiguity (unknown ownership of a
  change ⇒ high ambiguity ⇒ the safe arm is report-not-act). The definition's
  "report as information, not failure" clauses are G's ambiguity drain.
- **R6 (policy)** — the four arms, instantiated: **retrieve** (inspect
  content, run tests before committing — "verified passing first"),
  **act** (commit / push / park), **ask** (report to Joe), **yield** (leave
  WIP under 24h — the definition explicitly permits a turn's work; yield is
  a designed-in, not degraded, arm).
- **R7 (channel precision Π)** — two hard-won rows: (a) *fetch-freshness* —
  `futon-sync.clj` computed ahead/behind without fetching, so `=` was a
  precision masquerade: a stale ref read as agreement. Fetch is a precision
  refresh, priced as an observation. (b) *manifest membership = the
  observation frontier* — a repo absent from the manifest (`voxterm`,
  `p4ng`) is not observed-as-null, it is *unobserved*: no channel exists.
  Absence of a sensor ≠ a null reading. The frontier itself is declared
  data and must be audited (the manifest audit was).
- **R8 (mismatch F_π)** — the README's own unification, now formal: dirty /
  behind / stale-base-authoring are one mismatch (host disagrees with
  record) in three signatures — local-not-remote, remote-not-local, and
  new-work-built-on-the-second. The third is F_π compounding: mismatch used
  as the base for new policy.
- **R9 (witness)** — never self-certify: "verified passing first" before
  commit; the codex-10 packet's gates re-run by the reviewer. The sweeper's
  commit message is a receipt, not a verdict.
- **R13 (horizon T)** — T=1 within a sweep; the 24h clauses are the horizon
  in clock time, which is the correct grain for a hygiene loop (its
  planning problem is trivial per item, its *definition* problem is deep).
- **R14 (temperature τ)** — trust in the "clean" verdict. The degrading
  monitor ("worse than no monitor", 2026-08-19) is τ corruption: the
  never-fires lesson — an instrument that cannot fail cannot inform.
- **R16 (action)** — commit/push/park/report with receipts (bulletin lines
  are the witness records).
- **R17 (learning)** — clause-accretion on failure is the resident learning
  rule, and it is receipt-fed: no clause enters the definition without a
  dated incident. This is R17 done right at predicate grain, while numeric
  learning stays pinned.

## Where it sits in the hierarchy — why it is the *meaningful* instance

Inbox-zero is the only current loop that genuinely touches all five levels:

- **L0**: its chips are real one-action verbs (fetch, status, rev-list,
  commit, park) — each with a measurable cost.
- **L1**: the sweep is a board with a named terminal (zero) and a hazard
  table earned by incident.
- **L2**: refusal and deferral decisions are between-cycle policy (which
  arm per item).
- **L3**: the 24h horizon and the manifest are mission-parameterized —
  `clean` is *this* project's cleanliness predicate; another mission could
  pin different clauses.
- **L4**: its output is the crew moderator's first necessary reading:
  per-host coherence state, legible as a trace. And its one recorded
  hierarchy violation (U59: sweeper committed a file under a live edit) is
  the collapse test with a case number.

The operator occupies the top of the stack here in the most literal way:
the definition's clauses were ratified by Joe's rulings ("zero dirt is not
inbox zero", 2026-08-14). Precision raises came from the level above —
never from the loop itself.

## What the formalization buys (predictions, testable)

1. **The clause set should be derivable, not accreted.** If each clause is
   an R7 channel + an R8 mismatch signature, then auditing `clean` = "is
   there a host-record disagreement mode with no channel?" — a generative
   test instead of waiting for the next incident to teach the next clause.
2. **Fetch is a precision-refresh chip**, so its absence should be *typed*:
   a sweep that did not fetch must report readings as
   precision-degraded, not as `=`.
3. **In-flight-turn belief (R1's live-agent slot) must be fed by a channel**
   — currently it is implicit. Prediction: making it explicit (query the
   agency registry before ZAP) eliminates the sweeper-vs-edit class.
4. **The monitor-degradation failure generalizes**: any resident board whose
   instruments cannot fail (no typed refusals recorded over N cycles) has
   τ→0 evidence value. A "no refusals in 30 days" reading is a *finding*,
   not a comfort.

Row links: SPEC-chip-boards-v0 §3½ (hierarchy), §4 task 0; C446, C448,
U59 (case numbers); SPEC-zaif-harness-v1 (node table); README-inbox-zero
(the measured substrate).

## RULING: batch-dispatch cadence (Joe, 2026-09-14)

Inbox-zero cleanup is OPERATOR-DISPATCHED BATCH, not autonomous
continuous: wait until a reasonable number of dirty files accumulate
(~10), then the operator tells the agent to package them into nice
commits and push. Consequences:

- the trigger is a threshold + operator cue (the 🕒 pattern; EX-1
  territory), matching the constellation cadence: pressure builds,
  releases in batches;
- nice commits = grouped by concern/repo, message-described,
  certificate-bearing; push included in the dispatch;
- T2's atomicity requirement is UNCHANGED — operator dispatch changes
  who AUTHORIZES, not the physics; the execution-time in-flight
  recheck stays, in-flight repos are typed-refused out of the batch,
  and the autonomous-mode :atomic-feel-commit-unavailable guard is
  not weakened by dispatch (if the guard refuses during a batch, the
  agent reports and asks);
- the threshold (~10) is a tunable constant, declared not tuned.

## The batch model as integrate-and-fire AIF (Joe, 2026-09-14)

The ruled cadence (threshold ~10, operator dispatch, batch release) is
a textbook integrate-and-fire loop, and that makes it theorizable with
almost no machinery:

- **Integration**: each dirty file is an undischarged prediction
  error — a host-record disagreement (R8) the belief state carries.
  Pressure P(t) = the count (or, weighted, the clause count) of
  undischarged errors. Between dispatches P is non-decreasing absent
  new evidence (the monotonicity theorem T3, restated as dP/dt >= 0
  between events).
- **Firing**: at P >= threshold, the operator's dispatch is the
  trigger — NOT an autonomous crossing (strategy received, not
  computed; the loop does not fire itself). The batch is the action:
  grouped commits discharging the accumulated errors in one policy.
- **Reset**: successful push discharges P to 0 (T4's fixed point);
  in-flight and held-back files are residual P, typed — the neuron
  that doesn't fully reset says so, with reasons.
- **Refractory period**: post-push, the loop is quiet by construction
  (nothing dirty until new edits arrive) — the natural refractory
  phase, requiring no imposed cooldown.

AIF reading of the parts: integration = R3 belief accumulation of
unreconciled evidence; firing = R16 action under operator-scheduled
policy selection (R6's decision arrives from outside the loop);
discharge = precision recovery — committed-and-pushed work carries
warrant, so channel precision (R7) restores; the threshold is a
DECLARED preference constant (the operator's tolerance for visible
dirt, the C of this little loop); and the whole thing is one neuron of
the crew brain, connected to the rest through the cue synapse (🕒).

What the formalism buys: the cadence questions become parameter
questions with types — threshold too low churns (tiny batches, high
operator dispatch cost); too high and the dirty tree's three measured
costs (forgotten work, double payment, unreliable narration) reassert.
The threshold is thus priced against the README's own measured damage
curves, not tuned in the dark. And T1-T4 remain the safety envelope:
the neuron may only fire through the guarded boundary, and every
undischarged unit stays typed.
