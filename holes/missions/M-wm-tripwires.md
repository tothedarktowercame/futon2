# M-wm-tripwires — invariant tripwires for the live War Machine

- **Status:** DERIVE (drafted 2026-07-16, claude-6 from Joe's directive;
  ratification = Joe). Implementation = codex (runner expert), invariant spec
  + review = claude-6 (adversarial reviewer of record for the full-loop
  runner, four verdict rounds 2026-07-16).
- **Joe's framing:** "a supplementary harness for the war machine made up of
  invariant tripwires. The whole laser-beam-in-the-museum thing. THEN we run
  it live. Net effect: instead of waiting for broken runs to complete, we
  kick it into a debugger *live*."

## Concept

Simulation tests of known invariants pass by construction once the known bugs
are fixed. The tripwire harness instead checks **universal properties** at
runtime, woven around the live run at phase boundaries. A tripped wire does
NOT throw-and-unwind (the state at violation is the prize): it **freezes,
records, parks, and summons**:

1. Capture a durable trip-report EDN: the tripped invariant, its witness
   data, the full in-flight opportunity context (phase, job snapshots,
   selected entry, stop-lines in scope).
2. Open a typed finding (`:repair/class :invariant-tripped`) through the
   existing repair-obligation machinery — the WM's own stop-line discipline
   eats its own dogfood.
3. PARK the opportunity on an investigation join (futon3c park, background
   mode) and bell the owner (claude-6) + surface to Joe. The "debugger" is a
   live agent session opened onto the frozen trip report while the run waits.
4. Trip action is pluggable per wire: `:record` (shadow mode) →
   `:stop-line` → `:park-and-summon` (museum alarm, production default
   after calibration).

## Invariant catalog (v1 — generalized from the 2026-07-13/16 ledgers)

Conservation / accounting:
- T1. Turn conservation: `dispatched-turns` counted by the runner equals the
  number of Agency dispatch calls observable in the job ledger for this
  attempt. Any drift trips.
- T2. Ledger closure: every job-id referenced by the attempt exists in the
  Agency ledger; every referenced commit exists in the target repo.

Stop-line completeness:
- T3. At every exit of `run-opportunity!`: outcome is in the enumerated set,
  AND zero-achievement implies an open/superseded finding exists ON DISK for
  this attempt (re-read, not trusted from memory).

Provenance:
- T4. Every A-matrix belief event traces to a grounding witness naming an
  implementation entity; count of events applied == count of witnesses.
- T5. Review-commit binding: the reviewer job's prompt contains the exact
  commit sha that grounding then grounds. A grounded sha that the reviewer
  job text never saw trips (this wire would have caught the reviewed-vs-
  grounded split adversarially hypothesized in review round 1).

Immutability / lattice:
- T6. Repair files under findings/ implementations/ resolutions/ are
  append-only: content hashes recorded at phase start must be unchanged at
  phase end; new files only. Status transitions follow the lattice
  (open→awaiting-validation→resolved | open→superseded); any other edge trips.

Progress / liveness (cross-run, read from ledgers):
- T7. Wedge detector: the same stop-line selected on N=3 consecutive
  opportunities without a supersession or resolution trips (would have
  caught C1 live).
- T8. Livelock detector: >K=2 findings minted with the same
  (failure-kind, target, failed-commit) trips (would have caught C2 live).
- T9. Wall-clock: any single phase exceeding its budget by 2x trips even if
  the in-band budget logic thinks otherwise (catches M2-class silent
  budget loss).

Composition coherence:
- T10. Live-code == file-code: at run start, loaded namespace sources hash-
  match the repo files for the runner's own namespaces (catches finding-7
  mixed-image states before they act).

Environment alphabet (the boundary):
- T11. Job snapshots must have state in the known alphabet and parseable
  timestamps; unknown state or unparseable time trips (unknown letters of
  the alphabet become tripwires, not silent defaults).

## Calibration gate (before live)

Shadow mode must RETRO-TRIP on the known incident corpus: replayed
attempt-006 (late-author artifact), the C1 terminal-wedge scenario, the C2
rejection livelock, and a finding-7-style mixed-image simulation. A tripwire
suite that does not trip on the known past is a dud laser. Then one live run
with all wires in `:record`, reviewed; then promote to `:park-and-summon`.

## Slices

- S1 (codex): `futon2.aif.tripwire` ns — wire registry, phase-boundary hook
  into `run-opportunity-core!`'s existing `emit-phase!` seam (no semantic
  changes to the runner), trip-report EDN writer, `:record` action. Wires
  T1-T3, T6, T9, T11 + tests.
- S2 (codex): cross-run detectors T7, T8 (pure functions over the findings
  dir + cohort history) + provenance wires T4, T5 + T10. Retro-trip
  calibration harness over the incident corpus + tests.
- S3 (claude-6 review + Joe gate): shadow-mode live run, review trip log,
  promote to `:park-and-summon`, wire the summon path to futon3c
  park/bell (background mode, per E-park-delivery-losses discipline).

Constraints: the harness must be read-only with respect to WM semantics (a
wire that changes behavior when not tripping is a bug); trip actions must be
un-trippable-loops (a trip during trip handling degrades to `:record` +
stderr, never recursion); all wires individually disableable by id.

## AIF reading (Joe + claude-6, 2026-07-16)

Joe: "perhaps the most AIF thing yet — the model has a hand and a belly; the
tripwire system gives it an eye — it notices things those don't."

Formally: the WM's current observation channel is task-level only (job
outcomes, verdicts, witnesses, QA → A). Its own machinery is a HIDDEN state
with no likelihood mapping — pathologies (wedge, livelock, mixed-image) were
observable only through distal task effects, i.e. completed broken runs. The
tripwires are a new sensory modality over the machine's own trajectories —
closer to INTEROCEPTION than vision: each wire is a near-infinite-precision
prior over machine trajectories (P(violation) ≈ 0), a trip is surprisal that
belief-updating cannot explain away, and park-and-summon is the mandated
epistemic action (freeze the observation, recruit a higher-capacity inference
engine, forage before further policy execution). The dud-laser calibration is
identifiability of the new likelihood mapping: known hidden states must
produce distinguishable observations through the channel. In R-contract terms
this is a step past R16 toward the R14–R18 hierarchical self-model frontier.

**S4 candidate (interoceptive γ):** a trip should modulate the system's
confidence in its own machinery — post-trip, until the finding is discharged,
the WM runs at lowered γ (more conservative thresholds, operator confirmation
on grounding). The noticing feeds the feeling: the eye informs the belly.
Connects directly to the learned-γ thesis of the Closing-the-Loop sequel
paper — trips are exactly the error signal a learned meta-precision needs.
