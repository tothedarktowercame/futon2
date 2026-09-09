# RUN4 execution-path audit — draft for Joe, 2026-09-09

Read-only audit by codex-10, job `invoke-1788965944121-16473-77888965`, at
futon2 `e4a50b8193b3268ec42ab783bd91113e313f1980`. No tick, pre-flight,
lock acquisition, deposit, or readiness regeneration was executed. Only this
note is to be committed. Paths below are relative to the futon2 root unless
explicitly absolute. Existing unrelated working-tree changes were preserved.

## Bottom line: not an unconditional launch recipe

RUN4 is an operator-reserved **certificate acceptance**, not a runner's mode
or a registered batch name. Its preregistration currently names the S5 and RE5
certificates, not a new tick count, cadence, seed, flag set, or execution entry
point (`holes/labs/wm-contract/runs/F2-run4-preregistration/00-source.edn:1`).
Which fresh run qualifies is therefore a decision, not a missing command-line
argument we should invent. The controls explicitly reserve that judgment:
`holes/labs/wm-contract/runs/F2-run4-preregistration/04-controls.edn:1` (C6).

The committed meter reports eight green lines and blocked closability, but it
is not a launch permit. RUN4's row reserves the Lean edit to Joe
(`holes/labs/wm-contract/worklist.edn:618`). The later eight-before-any-run
ruling expressly supersedes RUN13's older start-anytime wording
(`holes/labs/wm-contract/EPIC-run-era.md:991`). Joe must resolve that gate and
the current fundamentals/rider state before authorizing the sequence below.

## Operator sequence, once authorized (commands NOT executed in this audit)

Run commands separately and inspect their exit codes; do not pipe gates.
Use a new absolute work directory and unique run-store name chosen by Joe.
The following is the existing **supervised, non-enacting step path**, the one
that actually writes `:pin/accepted-steps`. It is not a scheduled live fold.

1. **Decision:** choose the experiment's flags, scan window, evidence mode,
   number of steps and qualification criteria. No RUN4-specific values were
   found in the preregistration. Do not inherit an unknown shell configuration.
   Capture `git status --short`, `git rev-parse HEAD`, and the relevant
   `FUTON_WM_*` settings without dumping credentials. At audit time `efe.clj`
   was modified and `preference_module.clj` untracked; HEAD alone is not the
   executable input. The runner stamps mode flags and source provenance
   (`scripts/futon2/run_tick_once.clj:285`).
2. From `/home/joe/code/futon2/holes/labs/wm-contract`, run
   `bb run4_readiness.bb` and `bb run4_readiness.bb --summary`.
   They read the manifest, certificates, wiring, contract and closability
   audit; even checking regenerates comparison artifacts in a temporary
   directory (`holes/labs/wm-contract/run4_readiness.bb:265`). No `--emit`
   or `--probe` is part of this audit or an automatic repair. A blocked verdict
   is an answer, not necessarily a nonzero process exit. Inspect all nine
   lines, not just exit 0 (`holes/labs/wm-contract/run4_readiness.bb:432`).
3. From the futon2 root, `clojure -M -m futon2.wm-run-lock status` reads
   lock ownership. Then, **only after launch authorization**,
   `clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj`.
   This runs a real diagnostic tick, intercepts POST and ordinary `spit`, but
   DOES acquire/write/release the live lock; it is not a read-only smoke test
   (`holes/labs/wm-contract/r6_zero_post_preflight.clj:28`). Inspect the
   attempted POST and credential-read counts and lock cleanup, not only exit.
4. Set `RUN4_WORK` to the approved new directory, for example
   `/home/joe/code/futon2/data/wm-step/run4-2026-09-09`; set `RUN4_ID` to the
   approved unique store name. Then run
   `bash holes/labs/wm-contract/wm_step.sh init "$RUN4_WORK" codex-10`.
   This takes the live lock while copying the whole daily trace corpus;
   writes `pin/wm-trace`, `pin/manifest.edn`, `pin/world.edn`, `pin/pin.edn`
   and resets `sandbox/`. It initializes an empty accepted-steps vector
   (`holes/labs/wm-contract/wm_step.sh:151`). No canonical lab-level
   `pin.edn` exists: the pin belongs to this work directory.
5. Run `bash holes/labs/wm-contract/wm_step.sh step "$RUN4_WORK" run4`.
   Set `FUTON_WM_STEP_DAYS` explicitly if the approved window differs from 14.
   Default evidence mode uses a cassette; `--live-evidence` and
   `--frozen-evidence` are different experiments, not interchangeable retries.
   Do not use `--allow-pin-drift` to turn a failed pin into a compliant run
   (`holes/labs/wm-contract/wm_step.sh:215`). The command resets from the pin,
   starts the evidence proxy if needed, holds the live lock, then runs
   `clojure -M -m futon2.run-tick-once "$DAYS"` with sandbox trace and receipt
   directories (`holes/labs/wm-contract/wm_step.sh:250`). It writes the step's
   `tick.out`, before/after world, delta, decision records, normalized record,
   rationale, tick receipt, and `step.edn`; use the emitted step directory as
   `RUN4_STEP`, rather than guessing its numbered name.
6. **Pause for Joe's inspection**, below. Do not issue another step before
   deciding: accept supports only the most recent sandbox state. If approved
   as a state transition, run
   `bash holes/labs/wm-contract/wm_step.sh accept "$RUN4_WORK" "$RUN4_STEP" "$RUN4_ID"`.
   It creates `holes/labs/wm-contract/runs/$RUN4_ID`, runs observation and the
   check battery, replaces the pinned corpus, increments generation and appends
   `{:step ... :run-id ... :store ...}` to `pin/pin.edn`
   (`holes/labs/wm-contract/wm_step.sh:485`,
   `holes/labs/wm-contract/wm_step.sh:552`). This is a mutating acceptance;
   it is not a read-only check of whether acceptance would work.
7. Inspect and commit the exact new run-store and generated RE3/RE6/RE7
   artifacts after review, using an explicit path list from `git status`
   (not `git add .`). Then run
   `bash holes/labs/wm-contract/wm_step.sh deposit "$RUN4_WORK" "$RUN4_ID"`.
   The first battery produces receipts whose deposits can be refused because
   they are untracked; the second pass deposits against committed clean bytes.
   RUN3 conformance is intentionally not remeasured when already present
   (`holes/labs/wm-contract/wm_step.sh:404`,
   `holes/labs/wm-contract/wm_step.sh:447`). Review and separately commit the
   resulting ledger changes. This future mutation is not authorized by this audit.
8. Repeat the step/inspect/accept process only for the approved number of
   steps. An additional accepted step is needed to observe the previous
   accepted decision. Separately obtain Joe's RUN4 certificate judgment;
   a step accepted into a pin is not an automatic Lean-hole closure.

All scripts in steps 2–7 were found. Their live execution and external service
availability were NOT tested. Lock path overrides must not divert the run away
from `data/wm-trace/.run-lock`; token nesting/release belongs to the lock API,
not manual deletion (`src/futon2/wm_run_lock.clj:44`,
`src/futon2/wm_run_lock.clj:222`). The lock file was absent when inspected;
that is not a reservation against a later contender.

## Other entry points are not equivalent

`clojure -M -m futon2.report.war-machine 14` prints a report; its main is at
`scripts/futon2/report/war_machine.clj:7123`. Do not mistake this for the
stepper/acceptance workflow. `bash holes/labs/wm-contract/wm_run.sh N 14 AGENT`
runs N one-shot JVM ticks under one outer lock and writes live trace state;
it does not append a step pin's accepted-steps vector
(`holes/labs/wm-contract/wm_run.sh:1`).

`clojure -M -m wm-scheduled-run 14` is the dormant scheduled entry
(`scripts/wm_scheduled_run.clj:66`). It refreshes C, generates judgment,
calls `enact/close-loop!` by default unless `FUTON_WM_LIVE_WIRE=0/false`,
and persists another trace (`scripts/wm_scheduled_run.clj:30`,
`scripts/wm_scheduled_run.clj:94`). Its main has no run-lock wrapper or
accepted-step pin write. The one-shot explicitly stamps `:live-wire? false`,
disables portfolio steps and invariant eval fallback, and adds held advisory
cascades without enactment (`scripts/futon2/run_tick_once.clj:246`,
`scripts/futon2/run_tick_once.clj:277`). Thus the existing stepper cannot
establish an enacted-equals-selected claim by pretending a held cascade was
enacted. The historical dormant-runner finding is
`holes/problems/BUILD-status.md:8`; current producer inspection confirms
the path distinction, not that the old scheduled path still runs successfully.
The preregistration does not mandate either runner; selecting the scheduled
one requires a separately authorized execution/locking/provenance plan.

## What the producer records

The one-shot mints a UUID joined between receipt and trace, captures evidence
store basis and sample, and writes `tick-run-record-DATE-UUID.edn` under the
receipt directory (`scripts/futon2/run_tick_once.clj:217`,
`scripts/futon2/run_tick_once.clj:277`). Daily trace files are appended EDN
forms, not one EDN map: read every form. `trace-record` retains observation
and lossless observation-envelope, pre/post beliefs, prediction errors,
precision, selection gain, decision/rankings and present-only provenance
(`src/futon2/aif/trace.clj:515`). Detailed policy persistence requires
`FUTON_WM_TRACE_POLICY_DETAILS=1` (`src/futon2/aif/trace.clj:69`);
approve it before the run, not after discovering missing evidence.

`:trace/reason` is supplied on the judgment by the producer, but the durable
reason is placed on the TRACE hop of `:wm/route`; do not require a top-level
trace-record key that this writer does not emit. Missing producer reasons get
the explicit `:trace-route-reason-missing` triage value, not genuine routing
evidence (`scripts/futon2/report/war_machine.clj:6792`,
`src/futon2/aif/trace.clj:727`). Appends use the indexed trace writer; rationale
failure is distinct from trace failure (`scripts/futon2/report/war_machine.clj:6800`).

Three different “observations” must stay distinct: the tick's channel vector;
world-before/after census from `wm_step_records.bb`; and the post-accept
comparison of a previous accepted decision against this step. The last writes
`runs/RUN/observation/realized-outcome-PREVIOUS.edn`, joins by run IDs and
records typed absences if there is no prior accepted step, expected score,
later score or hole-count evidence (`holes/labs/wm-contract/wm_step_observe.bb:113`,
`holes/labs/wm-contract/wm_step_observe.bb:182`,
`holes/labs/wm-contract/wm_step_observe.bb:224`). It is not proof of causally
enacted external change. Accept explicitly tolerates observation failure
(`holes/labs/wm-contract/wm_step.sh:532`). The checkpoint-ledger-to-channel
observation bridge required by F10 remains missing; do not conflate it with
this already implemented step-observation mechanism.

## Draft decision sheet for Joe

- **Mechanical evidence integrity:** inspect all nine readiness lines:
  exact awaiting-certificate census, verbatim definitions, wiring hash/commit,
  tracked clean trace pins, byte-identical regeneration, current axiom probe,
  still-open hole/current contract, current ready closability rows, measured
  invalidators (`holes/labs/wm-contract/run4_readiness.bb:142`). These are
  evidence-integrity checks, not proof of live behavior or qualification.
- **Judgment before starting:** resolve the later no-run ruling, C/find/organise
  readiness, F10 rider, chosen runner and flags. No machine-derived threshold
  selecting a qualifying RUN4 run was found. C6 explicitly reports selection
  discrimination without making it a gate. Identical route censuses are not
  independent discriminating experiments (preregistration controls C6).
- **Judgment before pin advance:** inspect `step.edn`, `tick.out`, full appended
  trace, route reasons, actual selection and any enactment evidence, pin drift,
  world drift, version/flags and absent observations. The accept script only
  demands latest-step, exit-zero and nonempty run ID; red battery verdicts do
  not prevent pin advance (`holes/labs/wm-contract/wm_step.sh:494`,
  `holes/labs/wm-contract/wm_step.sh:545`). A shell success is not qualification.
- **Mechanical post-accept deposits:** inspect all eight checks plus RUN3,
  separately recording execution errors and red substantive verdicts. RE3
  receipts name schema, check, run/store, recorded authority, deposited verdict
  and live derivation. A live contract check is not evidence that the historical
  run used it (`holes/labs/wm-contract/runs/RE3-check-deposits/contract-pin-2026-09-05-u60.edn:1`).
- **Joe-only certificate decision:** select the qualifying run and accept its
  pinned conformance certificate. The preregistered conformance means at least
  one route, no empty route, no unmapped hop and no refutation; it does NOT
  require every drawn edge to fire or every ruling to be realized. The old
  census explicitly includes one ruling-unrealised hop and 19 unfired edges
  (`holes/labs/wm-contract/runs/F2-run4-preregistration/01-assertions.edn:1`).
  Whether that coverage is qualifying is Joe's judgment. Newly minted certificates change the manifest
  census; regeneration/re-pinning and any Holes/contract edits require their
  own reviewed workflow, not silent reuse of the two old certificates. No
  command was found that turns `wm_step.sh accept` into Joe's RUN4 Lean ruling.
- **Following acceptance, partially mechanical only:** RUN13 wants risk,
  ambiguity and G certificates on the accepted run, then further per-quantity
  certificates. All three current R5 entries point to the SAME simulation
  artifact, with `:accepted-run? false`
  (`holes/labs/wm-contract/CONVERGENCE.edn:61`). The existing
  `clojure -M holes/labs/wm-contract/f3_node_sim.clj OUTDIR` reads declared
  carriers and the F1 receipt, not an accepted-run argument
  (`holes/labs/wm-contract/f3_node_sim.clj:1`). An accepted-run R5 retake
  producer was NOT FOUND in that path; appending a run ID to this simulation
  would not establish shipped-path agreement. New extraction/reference work
  is required before updating convergence claims. Once real certificates exist,
  run `bb holes/labs/wm-contract/convergence_check.bb holes/labs/wm-contract/CONVERGENCE.edn holes/labs/wm-contract/aif-equations.edn "$RUN4_WORK/pin/pin.edn"`.
  Supply the pin explicitly: its default is the old `data/wm-step/w1/pin/pin.edn`
  (`holes/labs/wm-contract/convergence_check.bb:212`). Check N>0 and the
  accepted-run rejection control, not merely a green static ledger.

## Flagged risks / unfinished work

1. **Stale meter snapshot:** READINESS's `:lean-probe` records Holes hash
   `0410f61d…`; the current axiom receipt and actual Holes bytes both hash
   `fc8a6a9906a6d334e838a7c598c220a4ac686d835505a09b0239764ac08d042d`.
   The wiring hash still matches `161d0abf…`. Eight-of-nine is the committed
   snapshot, not a fresh certification; no meter regeneration was performed.
2. **Dormant scheduled path:** no lock/pin workflow in its main, different
   enactment behavior and absent one-shot run-ID injection. No scheduled
   execution was tested. Selecting it changes what is being measured.
3. **F10 live rider blocked:** the current F10 blocker identifies missing
   production `:ruled-outcome-c-enabled?`, `:seeded-c`, `:disposition-kernel`
   opts and a checkpoint-versus-channel carrier mismatch. Inspection of
   `scripts/futon2/report/war_machine.clj:6330` finds no such opts. Implemented
   fold code alone does not put C on this run's scoring path. The boundary is
   documented in `holes/labs/wm-contract/C592-F10-disposition-risk-fold.md:3`;
   a run or acceptance must not be represented as discharging it.
4. **Observation scope:** post-step observations exist but can be absent and
   are not causal enactment receipts. The F10 channel-valued kernel/recording
   bridge and an accepted-run R5 extraction path are unfinished.
5. **Acceptance conflation:** accepted pin, deposited check, qualifying RUN4
   certificate, and converged quantity are four different claims. Scripts do
   not automatically promote one into the next; red checks can accompany pin
   advance. The default convergence pin is not a newly initialized RUN4 pin.
6. **Live inputs untested:** evidence service, cassette proxy port 7099,
   Python cascade subprocess, shared source-tree stability and mode flags
   were not exercised. Advisory construction is not a full fold execution.
   Dirty EFE work makes an unqualified HEAD pin insufficient. No repair was made.
