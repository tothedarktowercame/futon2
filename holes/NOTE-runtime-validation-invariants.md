# NOTE-runtime-validation-invariants — common invariants for APM/WM-class runtime systems

**Status: AGREED 2026-09-12 between claude-15 (APM evidence) and zai-5 (WM
evidence), commissioned by Joe the same day ("we really need some core
functionality that actually works 100% of the time"). Pending Joe's ruling
for promotion.** Exchange: claude-15 draft (bell
`invoke-1789223207697-20384-c3b8cf4e`), zai-5 redline with all items agreed;
amendments are folded in below with provenance; I9/I10 are zai-5 additions.

Authority boundaries (apparatus/one-authority-per-question): the pattern
texts under `futon3/library/apparatus/` own principles; WM worklist tickets
own WM instances; APM technotes (futon3c/holes/technotes/) own APM
instances. **This note owns only the validation layer — the invariant
statements and the obligation attached to each.** Patterns are cited by id,
never restated.

"Work item" below means an APM frame or a WM packet-click series.

## Runtime invariants

- **I1 — Fault containment at the loop boundary.** Every external read/wait
  inside the control loop resolves to exactly one of {value, durable bounded
  retry, durable repair hold}. An unhandled throw out of the loop is a
  boundary defect regardless of site — and so is a handled throw that erases
  its own typed failure data (zai-5 amendment). APM:
  `futon3c/holes/technotes/TN-F218-watchdog-recovery-2026-09-11.md` and
  `TN-apm-tick-substrate-read-audit-2026-09-12.md` (no site guarded against
  a thrown timeout at the audited SHA). WM: `fail-click!`
  (`futon3c/src/futon3c/wm/runner_service.clj` ~389) reduces throwables to
  `.getMessage`, which cost three instances to diagnose one projection
  refusal (repair futon2 `92e5fbf2`). Patterns: `every-wait-has-a-deadline`,
  `loudness-is-conserved`.

- **I2 — Accepted work survives reordering.** Once work is durably accepted,
  no subsequent ordering of cancellation / termination / session release /
  collection / observation may convert it into a fault. APM: f177/f194
  (cancel-after-completion 409 read as reconciliation failure), the f218
  guide leg (persisted typed submission cancelled as "wrapper
  reconciliation"), f227 (typed completion existed in the reviewer session;
  terminal repair exhausted anyway) — decision-point map in
  `TN-apm-acceptance-ordering-decision-points-2026-09-12.md` (in flight).
  WM: `futon3c/holes/labs/wm-contract/runs/RUN4-U88-codex20-v2-2026-09-11/RESULT.md`
  — authored commit `ee5ab864` found in a stale sibling worktree by
  resolve-build, finished work closed build-failed (repair futon2
  `38de06f5`). Pattern: `evidence-to-disposition-once`.

- **I3 — Monitors judge from the work's durable state, and holds must be
  able to discharge.** A watchdog/stall/hold verdict is a function of the
  work object's durable state, never of the monitor's own arming cycle; and
  every hold/await state has a machine-reachable exit for every
  classification the design can produce (zai-5 amendment). APM: f209
  (watchdog halted ~30 s after each re-arm while queue poll backoff was
  ~19 min); the f225 store-read hold outlived its trigger with no
  time-bound release. WM: three series queue-held indefinitely on
  `:terminal-evidence-incomplete` because `terminal-evidence?` requires
  `:safe`/`:unsafe` while historical admissions are designed to classify
  `:infrastructure :unknown`. Patterns: `monitors-measure-the-work`,
  `every-wait-has-a-deadline`.

- **I4 — Capacity is not work.** Environmental capacity/availability faults
  are a distinct class with their own budget; attempt denominators exclude
  environmental unavailability, or availability is proven before an attempt
  is consumed (zai-5 wording, naming stop-rules like `attempt-each-once`
  explicitly). APM: f208/f209 (quota outage consumed rounds, then stalled
  the campaign). WM: cohort
  `run4-ea1-artifact-binding-admission-20260912-v1` burned its single
  attempt on `:agent-unavailable` while the cast author was mid-invoke.

- **I5 — Domain-negative is not apparatus-fault.** Expected negative
  outcomes (a failing proof attempt, a refuted statement, a build-failed
  cohort) classify through a total closed enum, disjoint from apparatus
  fault. APM violation: f206 (an intended measurement parked as a terminal
  fault). WM positive reference: cohort preregistrations validate against
  `checkpoint-cell-errors` (futon2 `full_loop_cohort.clj`); the checkpoint
  kernel enumerates all outcomes. Pattern:
  `success-must-not-resemble-failure`.

- **I6 — Identical-finding streaks halt dispatch.** N identical fault
  findings across successive work items stop the machine for one apparatus
  repair instead of producing N parks; the threshold trips on the identical
  finding, not on park count. The f202 remediation checkpoint shows the
  halt-on-identical-signature mechanism working within one work item; the
  APM 14-frame `:fresh-session-id-missing` burn shows it not firing across
  work items. Pattern: `new-failure-class-is-a-design-defect`.

- **I7 — Re-dispatch is always possible, and publication failure does not
  consume the work attempt.** Work identity carries an attempt ordinal or
  dead jobs are releasable; the attempt ordinal is consumed only by the
  work's own terminal state — failures in the publication/telemetry wrapper
  re-open the same attempt for re-publication from durable artifacts (zai-5
  amendment). APM: f209 (content-addressed job id collided with its own
  dead predecessor). WM: grounded commit `5d595dc9` (independent review,
  gates green) left its series held because the click's attempt was charged
  to a publication-wrapper failure; recovery required minting an entire new
  packet (futon3c `7538137e`).

- **I8 — The system metric is uninterrupted operation.** Consecutive work
  items without operator intervention, measured packet-to-published-terminal
  (not per click or per phase), separately from domain outcomes; it is the
  go-live gate for successor versions (APM V4, WM waves). Baselines at
  agreement: APM V3 endgame — every frame f218–f228 needed operator action
  or after-the-fact adjudication; WM 2026-09-12 — five packets minted, two
  consumed by apparatus constraints, ≈1 operator-visible hour per recovery.

- **I9 (zai-5) — Installed authority is durable and verifiable.** The
  running authority can be probed for identity and provenance from durable
  state, and absence is loud. WM: `reconfigure-handler!` installs are
  volatile across JVM restarts; a malformed install serves 200s silently
  disabled ("serving-runner-code: unavailable/not-recorded-in-this-
  process-image"). APM: the one-JVM/live-load-from-canonical policy exists
  for the same reason — the F218 diagnosis initially could not distinguish
  current code from a stale loaded namespace. Patterns:
  `done-is-observed-running`, `model-upstream-and-coupled`.

- **I10 (zai-5) — Identity chains are verifiable at rest.** Packet
  sha-chains (cohort → task-pin → series-pin → template) get a standing
  lint-style verifier over the frozen artifacts directory, not only an
  install-time preflight. This is V1 applied to the minting tooling itself.

- **I11 — Handoffs compose** (added by amendment 2026-09-12: ratified by
  Joe, concurred zai-5 with both instances below; claude-15 committed).
  Every delegated job carries its own acceptance authority and its own
  reconciliation. An enclosing layer consumes only the delegated job's
  terminal disposition — a closed enum satisfying I5 — **and carries only
  its identity reference; findings and internals flow nowhere upward**
  (zai-5 wording). It never answers the delegated job's acceptance
  question. Copying a child's findings into a parent's findings vector is
  itself a breach (the flatten), not a permitted visibility repair.
  APM instance: f227 — depth-two nesting (Guide enclosing an independent
  promotion review) blinded the rescue selector
  (`futon3c/holes/technotes/TN-apm-f227-submit-step-trace-2026-09-12.md`);
  the repair (futon3c `d10a59c4`) gives the nested review its own
  reconciliation through the shared driver path, flatten prohibition
  test-enforced
  (`f227-empty-nested-repair-exhausts-with-recheck-outside-guide-findings`).
  WM instances (zai-5, both witnessed live 2026-09-12): the review-marker
  protocol is the terminal-disposition contract in production — the
  verifier consumes an independent review only through line-anchored
  markers (`futon3c/src/futon3c/wm/run4_historical_verification.clj`,
  marker regex + `(= [sha] markers)` join); a substantively approving
  review expressed in prose read `:unverifiable` and the parent REFUSED
  rather than salvaging the approval it could see in the child's events —
  the induced-violation obligation, already witnessed (chain at futon3c
  `f0b13183`). And the linked-successor join: a link config carrying
  neither child identity reference was refused with typed
  `invalid-consumer-input` rather than probed into the child's records
  (`:wm/historical-repair-resolution-v2`). The chip-boards recursion
  ruling (opacity at the parent, finiteness at every level,
  return-with-value; `SPEC-chip-boards-v0.md`) is this law in board
  vocabulary; `SPEC-handoff-algebra-v0.md` §3 binds the two so no
  second version grows. Patterns: `evidence-to-disposition-once`,
  `one-authority-per-question`.
  Formalization pointer (Joe, at ratification; suggested not mandated):
  handoffs-compose is the dynamic version of a structured proof —
  handoffs as Petri-net markers flowing through a structured proof not
  yet written down; if I11 gets a formal model, marker/flow vocabulary
  over the disposition enum is the right abstraction level (it composes
  with the replay layer without re-modeling internals), subject to V4's
  coupling requirement.

## Validation obligations

- **V1 — A check must be able to fail.** Every guard/monitor/lint is
  commissioned by induced violation before it is trusted (the assurance
  frontier's fence rule; the APM trial watched three uncommissioned
  monitors lie in one day).
- **V2 — Validate with production shapes.** Injected faults use verbatim
  captured production failure shapes, never authored constants. Worked
  example: the F218 containment was green against `HttpTimeoutException`
  while production throws a cause-less `ex-info`
  (`TN-apm-tick-substrate-read-audit-2026-09-12.md`, review correction).
- **V3 — Replay is the conformance layer.** Recorded incident orderings
  become permanent replay tests through the actual shared code path; the
  regression suite is the incident archive, not a parallel model of it.
- **V4 — A formal model is evidence only when pinned to the runtime.**
  Statement-hash/enumeration coupling or it does not count (WM:
  `FlightDisposition.all`, the runner/gate vocabulary seam).
- **V5 — One coverage accounting.** Invariant coverage uses the existing
  assurance-frontier mechanism (three states, no silent fourth), extended
  to APM; a second parallel ledger is itself an I-violation
  (`one-authority-per-question`).

## The Lean question (agreed resolution)

Replay plus induced-violation commissioning (V1–V3) is the right layer for
runtime behavior; Lean stays on the domain side for now. Reasons (zai-5,
agreed): observed runtime failures are representation failures — dropped
ex-data, a missing route hop, a stale sha, an unexpressible classification —
which live between a model's abstraction and the code's actual data flow and
are invisible to an event-ordering model; the WM already has a mechanical
ordering checker in the append-only controller events and terminal-evidence
join, and a Lean twin would be a second, silently stale truth; the runtime
vocabulary is still moving (`packet-run-route` changed it the day of this
agreement). One narrow exception, explicitly sequenced: replay suite first,
terminal-evidence join vocabulary freeze second, then price a Lean model of
that single predicate (a finite enumeration with digest joins) with
statement-hash coupling.

## First obligations

- APM: I2 decision-point map and replay plan (TN in flight, codex-21
  packet 4); I1 boundary unification per the tick-read audit's structural
  proposal; I8 metric defined with the V3 baseline before V4 starts.
- WM: I3 discharge exits for the three held series; I7 re-publication from
  durable artifacts; I9 authority probe; I10 standing chain verifier.
- Joint: V5 frontier extension to APM.

Cross-referenced from `NOTE-apparatus-design-principles-index.md` (futon2)
and `futon3c/holes/technotes/NOTE-runtime-validation-invariants-pointer.md`.
