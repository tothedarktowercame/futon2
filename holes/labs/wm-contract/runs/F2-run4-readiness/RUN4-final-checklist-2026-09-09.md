# RUN4 final checklist — 2026-09-09

Assembled by claude-1 at Joe's direction ("we should know what other final
checklist items need to be in place"), from the execution runbook
(RUN4-execution-runbook-2026-09-09.md), the live readiness meter, and the
Item 19–22 rulings. Joe's two confirmations map to existing machinery:

- "Running a version of the designed model, well put together as a graph" =
  the readiness meter's pre-run lines: wiring-pin (wiring hash/commit vs
  live), definitions-intact, regenerates (byte-identical graph/manifest
  regeneration), lean-probe, run-pins. ALL GREEN at contract fcd1261c30 as
  of this writing. Re-run `bb run4_readiness.bb --summary` at start time;
  green lines are the "well put together as a graph" confirmation.
- "A certificate that it ran conformantly according to the wiring" = the
  preregistered conformance certificate (F2-run4-preregistration
  01-assertions.edn: at least one route, no empty route, no unmapped hop,
  no refutation) accepted by Joe over the run's deposits; its acceptance is
  what closes wmRunConformsToWiring (:witnessed-and-held-open, run-gated).

## A. Decisions still needed (Joe), in dependency order

1. **DONE (b9208880, reviewed: READY verified, 11/11 plants)** — :ready gate semantics. run4_readiness.bb:387 demands
   `:readiness :ready`, a value the audit producer cannot emit, so the meter
   that certifies "you are running the designed thing" can never say READY.
   Proposed reading: both RUN4-named holes typed :run-gated with no non-run
   blockers counts as ready-to-run. On Joe's word, claude-1 edits the gate
   with a control.
2. **PARTIALLY RESOLVED, JOE'S WORD REMAINS** — eight-before-any-run vs the four holes: dirichlet BOUND (9f45821c + existing 56b3c44b), C AMENDED at §2a (c75b7323, definition excursion-owned), find FORKED (applied carrier needs new conformance work, 0f74d9b4), organise FORKED (LA2 evidence a0219c3; :library-correspondence control red, two stale THEN citations). Joe rules: run with find/organise open, or commission the new work first. WAS:
   (EPIC-run-era.md:991, Joe 2026-09-06: Box 6's substantive holes gate the
   first run). The current closability audit types 4 holes pre-run-closable.
   Either those close pre-run (lane work, each its own reviewed slice) or
   Joe rules the current state satisfies his gate. This is the one item
   where "anything outstanding from that first check" has real content.
3. **MECHANICALLY DONE (e65b996a, reviewed)** — fold pass-through wired, no default flip; the on/off itself is settled ON by Item 23's reading. WAS: (the seeded disposition fold; caller opts
   :ruled-outcome-c-enabled?/:seeded-c/:disposition-kernel). Item 19a fixed
   C_mis flag-gated; the FOLD flag is a separate decision, still open.
4. **IMPLEMENTED (f71eeade, reviewed); adoption at go/no-go** — 19b recording contract. WAS: (:wm/realized-recording-v1 draft,
   DRAFT-realized-outcome-recording-19b-2026-09-09.md; pins verified). Its
   own reviewed ledger step; determines what the run records.
5. **DONE (00112270, reviewed)** — qualification criteria stated up front (prereg C6 reserves this to
   Joe: the old census includes one ruling-unrealised hop and 19 unfired
   edges — is that coverage qualifying?). Can be ruled at accept time;
   better before.
6. **ASSEMBLED (RUN4-config-2026-09-09.edn, PROPOSED-FOR-REVIEW)** — run config. WAS: flags (accumulation ON per Item 22; E_S fixture stands;
   FUTON_WM_MISSION_C off per 19a), step count, work dir, unique run id
   (runbook step 1).
7. **DONE (00b9afd4, reviewed)** — accept-script red-verdict gate. WAS: wm_step.sh:494,545 advances the pin on exit-zero
   alone; red battery verdicts do not stop it. One word authorizes the
   refuse-on-red repair.

## B. Mechanical work before start (lane)

8. **E forward accumulation: LANDED AND REVIEWED** (futon2 8dd17915;
   63/342/0 re-run by reviewer; registry addendum 48a18537). RUN4's config
   turns :accumulate-strategic-habit? on.
9. **Readiness snapshot re-emit at start** (`bb run4_readiness.bb` +
   committed snapshot refresh) so the committed meter matches the live one
   (runbook risk 1).
10. **DONE** — all re-signs landed (42239777, 2a082e63); 31 signed entries
    verified; ledger quiet.

## C. At-run and post-run (mechanical; no pre-work required)

- Preflight r6_zero_post_preflight (NOTE: takes/releases the live lock);
  wm_step.sh init/step with inspection at every step; accept only after
  the A7 repair (or with manual red-check inspection if A7 is declined);
  RE3 deposits (8 checks + RUN3); Joe's certificate acceptance (closes
  wmRunConformsToWiring for this run's scope).
- **Post-acceptance, does NOT gate the run**: the R5 certificate retake
  producer against an accepted run does not exist (CONVERGENCE.edn rows
  point at one simulation, :accepted-run? false) — new extraction work,
  scoped after the run design is fixed. RUN13's convergence claims wait on
  it; RUN4's banking does not.

## Explicitly NOT on this checklist

find/F11 (Lean statement slice; not on the tick path), organise/F12 (O4
adapter; not on the tick path), U84 (fills as a side effect of the run),
§1b observation-model bridge (19b records both domains; bridging is
post-RUN4 redesign per Item 22's closing sentence).
