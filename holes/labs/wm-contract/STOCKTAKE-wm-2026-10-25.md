# STOCKTAKE — the War Machine at 2026-10-25T23:39Z, before any further click

Written by claude-9 at Joe's request ("before we run any more clicks it is
worth doing a stocktake"; "worth snapshotting the War Machine as uninjured
and potentially ready to run clean"). Each line is marked **[V]** when I read
the source or the live JVM myself this sitting, or **[R]** when I relay a named
record I read but did not re-execute. Nothing here was dispatched; no click,
reload, ledger write or data move was made.

## 1. Snapshot (pinned)

| what | value | how read |
|---|---|---|
| futon2 HEAD | `9f81724b5` (Add read-only mission-wants-view), branch master | [V] `git rev-parse` |
| futon3c HEAD | `c9ad3c3f4` (GET /api/alpha/wm/mission-wants), clean. At my first read (23:33Z) HEAD was `78d8336a6` with that route uncommitted in `http.clj`; claude-11's lane committed it at 23:35Z | [V] `git status` twice |
| mathlib4 (darktower) HEAD | `ddf999ac97` (Add mission observation admission contract), `~/worktrees/mathlib4`, clean | [V] |
| serving JVM drift | `{:drift [] :registered 109}`: every registered namespace's captured sha equals the file on disk | [V] `proof-eval.sh -f ~/notes/b4-reload/drift.clj` |
| debugger | attached, `stopped` = `[]` (no suspended run frames) | [V] proof-eval on `futon2.aif.wm.debugger` |
| injury ledger file | `data/wm-meta-injuries-live-2026-10-22/ledger.edn`, sha256 `5862fd76…2b1fc`, 44 entries (29 transitions, 15 observations), 7 injury ids: 04e14314, 41a00c62, 8467a7d5, 94091b05, 9ccf92a2, a9b900a4, cfbcbad0 | [V] `sha256sum`, text count |
| effective active injuries | **0** (raw-active 7, effective-ids `#{}`, covered? true) | [R] codex-11's independent plain-JVM check `~/notes/wm-next/offline-repair/authority/8467-adoption/live/coordinator-check.edn`, whose source pin is the sha above; the JVM `load-ledger` without a medic verifier reads `:refused`, as designed, so the effective count needs the authority verifier that only the composition root builds |
| medic adoption records | one per injury: `authority/{04,41,8467,940,9cc,a9,cfb}-adoption` under `~/notes/wm-next/offline-repair/` | [V] directory listing |
| ordinary click budget | allocated 173, consumed 173, **available 0** (authorization AUTH-ordinary-click-budget-renewal-7, sha 326826ac) | [V] proof-eval `ordinary-click-budget/availability` |
| last budget-charged click | `wm-click-0487f6a6…`, 2026-10-24T20:39Z, caller codex-11 | [V] `data/wm-ordinary-clicks/consumption.jsonl` |
| Lean holes in `DarkTower/WarMachine` | 5 `sorry`, all in `Holes.lean` and all marked PERMANENT EXTERNAL ATTESTATION (Lean cannot prove an event): `preferenceStackLiveRecorded`, `wmRunsOnce`, `enactedEqualsSelectedWhenRankOneGated`, `policyPrecisionIsGammaFromBeta`, `policyPosteriorImportsPolicyF`. `SelfHealRoute.lean` has none (the grep hit is the word in a docstring) | [V] grep for `sorry` as a term |
| Agency | no running job except 象-1 turn capture; claude-11's three dispatches today (zai-1 meta-latest, zai-2 mission-wants-view, zai-5 Morning Brief wants) all delivered | [V] `/api/alpha/invoke/jobs` |

Git tags `wm-uninjured-2026-10-25` mark the three commits above (futon2,
futon3c, mathlib4 darktower).

## 2. What "uninjured" means and does not mean

- **Means** [R, codex-11 21:45Z review]: every one of the seven historical
  injury occurrences has an exact medic correction appended to the ledger, raw
  history preserved. The next click's `production-readiness` will see no active
  injury, so it is not pre-empted into the self-heal route.
- **Does not mean**: that any of the seven defects was fixed *by the machine*.
  All seven were cleared from outside (offline repair + medic disposition,
  2026-10-24/25). codex-11's review says so in its own words: "not task
  success, learning credit or a claim that every possible machine defect is
  fixed".
- **Does not mean** the self-heal algorithm works. See §4.

## 3. Things in the live stores that a clean click would read (pre-click list)

These were found this sitting. None is in the dossier yet; each breaks no
Lean term, so under the dossier's rule 1 they belong here and in *Parked*
unless the first click trips on one.

1. **13 test-origin run records in the live run store** [V]. `data/wm-runs/`
   holds, dated today: 11 records with target `M-selected` whose paths point
   at `/home/joe/worktrees/futon2` (the runner test fixture), and two
   (`596e3a2d`, target `test-repair`; `71e87573`, `:agent-budget-expired`) whose
   paths point at `/home/joe/worktrees/futon2-mission-observation` (codex-12's
   8467 deferred-observation work, ~19:00Z). The two also deposited
   injury-evidence projections into `data/wm-meta-injuries-live-2026-10-22/`
   (not ledger entries; the ledger does not name them). None consumed budget.
   Readers that take "the newest record" (C5 predecessor focus, Morning Brief
   items, the new `meta-latest` route, `run_narrative`) would see these first.
   Precedent: `data/wm-trace-quarantine/README.edn` (claude-1, Joe approved
   2026-09-28) moved test leaks aside with a one-line reason each. **Proposed:
   quarantine the 13 + 2 the same way before the next click.** Not done: it is
   a live-store move and codex-11 may still be reading them.
2. **An `:active` repair transaction file remains** [V].
   `data/wm-meta-injuries-live-2026-10-22/transaction.edn` (2026-10-24 20:39Z)
   is the 8467 work order, `:status :active`, with three
   `transaction.edn.deferred-injury_*.edn` siblings (04e14314, 94091b05,
   cfbcbad0). `production-readiness` gates on the ledger's active set, not on
   this file, so with effective 0 it should not route to self-heal; but
   `route!` reads the transaction when it does run, and the dossier's parked
   M2d gaps (defer over owned dirt; restoring a deferred transaction needs a
   clean checkout) are about exactly these files. Worth a typed check that a
   click with effective 0 ignores them, before relying on it.
3. **13 `war-machine` parks with no deadline** [V]. `GET /api/alpha/parked`
   lists 13 parks by agent `war-machine`, surface `morning-brief`, mode
   `between-turn`, each awaiting one `repair-occ-…` id, `deadline-ms` nil.
   They are `park-r16-stop-line!` registrations from earlier R16 stop-line
   failures (`full_loop_runner.clj`, ~line 280–310). `data/wm-repair-obligations/findings/`
   holds 275 repair-occurrence findings. Nothing resolves these parks now that
   the injuries are medic-cleared; they are harmless to a click but they are
   the machine's own record of unfinished repairs and should be dispositioned
   (closed with a reason) rather than left to accumulate.
4. **Click budget is 0** [V]. A click needs a fresh grant: raise `allocated` in
   `futon3c/src/futon3c/wm/ordinary_click_budget.clj` with Joe's words, commit,
   reload (per the 10-22/10-23 grants).
5. **futon3c** [V]: claude-11's `http.clj` route was uncommitted at my first
   read and is committed at `c9ad3c3f4`; the serving JVM shows no drift, so
   the route is loaded. futon2 has only untracked probe files under
   `runs/one-click-frontier-2026-10-22/ordinary-trial-5/`.

## 4. The self-heal algorithm has not been shown to heal

Joe's observation holds, and the record says why, in three parts.

- `A-self-heal.md` (approved 2026-10-02) is the *debugger-attached, human-run*
  procedure: one paused click, one bounded repair, reviewer, explicit restart.
  Its `:time-budget-ms` and `:token-budget` are typed holes
  (`:bind-at-launch`), so no concrete policy instance is admissible to G yet.
  Registered in `resources/wm/approved-algorithms.edn`.
- The *dedicated self-heal route* (`meta_self_heal_production.clj`, PACKET-1..7
  of 2026-10-19) is a different thing: the pre-emption branch that takes every
  click while an injury is pending. Its history on injury 04e14314 (dossier
  Archive M2, 2026-10-22/23): diagnosis refused twice by the provider, then
  acceptance admitted, enact done, **the retry did not heal** (selected a
  different target, refused on `:selected-want-accounting`), the next click
  requeued the old repair instead of diagnosing the new injury, and two new
  injuries (cfbcbad0, 94091b05) were opened by the attempts. Every closure
  since came from the medic path.
- The Lean on that route, `SelfHealRoute.lean`, is a transcription of the
  branch (its `noFallback` theorem and the click-2 certified violation), not a
  specification of healing; the dossier's J2 row retires the branch in favour
  of `R15InjuryTier` (an injury is one candidate in the same G comparison),
  and J2 is open: replay blocked on missing modeled inputs (repair class and
  reach), per `~/notes/wm-next/J2/findings.md`.

So the claim "if an injury is noticed and the self-heal route is selected, the
machine can self-heal" has no positive instance. What a positive instance
needs: an injury opened by a real click, selected for repair by G (J2) or by
the branch, diagnosis + repair + acceptance produced by the cast seats, the
ledger reaching `:discharged` on the route's own acceptance evidence, and the
next click running ordinary selection. That is the first thing to watch for
when the machine next runs into a defect.

## 5. Dossier rows as they stand (from DOSSIER-wm-loop.md, unchanged here)

G1: C1 test done · C2 standalone probe only, no record pin · C3 test done ·
C4 open (code computes prefix F; trace the T1 record) · C6 test done, gap
noted · C7 new row, record check at T1 · G done in code · O2 done in Lean and
code · J2 open, replay blocked · **T1 open** (predict, click, compare).
G2: C5 discovery done, carries partial · L1 wired, no click has reached it.
Open choices U1–U4 unchanged. Machinery: R1 driver done except N > 1; M3 cast
refresh done.

## 6. What was validated in Lean, and what runtime validation exists

- Lean [V, module list; R for green builds]: the contract C1–C7, G, O2, J2's
  `R15InjuryTier`, R20 carries, θ novelty, plus this week's medic/authority
  models (ledger recurrence, sweep provenance, reviewer and author correction,
  mission observation admission `ddf999ac97`) and the checked-map closure
  modules with receipts dated 2026-10-24. The five remaining holes are
  attestations the runtime must witness, not proofs left undone.
- Runtime validation [V]: `holes/NOTE-runtime-validation-invariants.md` exists
  (I1–I8, V1–V5, I11, from 2026-09-12). `find_receipt.clj` has a
  `:runtime-validation` mode. No namespace registers runtime invariants against
  a click; the test registry (warrants) covers tests, not runs. This is the
  thing Joe says is "expected", and it is not yet built.
- Rerun-from-a-point [V, docstrings]: two pieces exist. `wm/recorded_flight`
  "restores the selection checkpoint and recorded agent answers, then executes
  the first unreplayed boundary in isolated stores"; `wm/open_cascade_resume`
  is same-click resume at a typed seam error under the debugger. Neither is a
  general snapshot of machine state (belief, θ counts, focus, ledger) that a
  later process could start from; that is what a "rerun from a given point"
  would add.

## 7. Things to look for when the machine runs (collected)

1. The self-heal positive instance (§4).
2. T1's prediction-vs-record diff, field by field (dossier T1); the first
   differing field becomes a row.
3. C2 on a record: does the run record carry the update's inputs and output so
   `machineStep_eq_update` can be checked on it (today: bounded absence).
4. C5 carries across two consecutive clicks: run id, habit, d, β, focus.
5. L1: the executed pattern's θ trial count +1 in the next click.
6. The five Holes.lean attestations: each is a runtime witness the record
   should supply (`wmRunsOnce`, preference stack recorded, enacted = selected
   under rank-one gating, γ from β, policy F imported).
7. Cast refresh receipt per seat (M3) and the click's `:g-law`.
8. Whether the `:active` transaction file and the 13 parks are read or ignored.
9. Wants: the new `mission-wants-view` and the Morning Brief "you said / we
   did" table against what the click actually froze (`:declared-wants`).
10. Click budget consumption and refund behaviour on refusal.

## 8. What I did not do

No clicks, no dispatches, no reloads, no file moves in `data/`. The effective
injury count is relayed from codex-11's check (pinned to the same ledger bytes
I hashed), not recomputed with the medic verifier. No `lake build` was run.
