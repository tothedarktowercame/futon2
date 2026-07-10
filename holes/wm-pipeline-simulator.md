# WM Pipeline Simulator — static readiness audit (before the live trace run)

**Purpose (Joe, 2026-07-08):** before allocating a fresh Zai lane to run the machine
end-to-end and accumulate detailed traces (the paper evidence), *simulate* the pipeline
statically — walk every step, find every point where an LLM/agent turn is required, and
convince ourselves the prompting for each role **exists and is clear enough that the driving
agent won't silently stall**. This doc is that audit. Verified against the code, not assumed.

---

## 0. The load-bearing clarification: THREE roles, TWO loops

Joe's phrasing — "allocate a fresh zai lane, put it in follow mode, run from the top" — maps
onto **three different things** in the stack that share vocabulary. Pinning them apart is the
whole audit, because the trace the paper wants comes from only one of them.

| Role | Who | What it does | Artifact |
|---|---|---|---|
| **Whole-loop DRIVER** (READ→EVAL→PRINT→LOOP) | the **War Machine Pilot** peripheral (an inhabiting agent) | drives the full inference cycle, emits a γ-frame per step | `futon3c/data/repl-traces/<run-id>.edn` — **this is the end-to-end trace** |
| **Fold-turn AUTHOR** (escrow writer) | a commissioned Zai, out-of-process, under mana | authors ONE fold deposit; the live tick later *replays* it as a hash-gated lookup, never an LLM call | `futon6/data/fold-turns/ft-*.edn` |
| **Follow-mode OBSERVER** | Emacs `agent-follow-mode.el` (operator) | polls the invoke-jobs ledger, renders an agent's turns into a buffer | nothing — read-only |

Two loops back these:
- **The WM tick** (`futon2/scripts/wm_scheduled_run.clj`, cron hourly) is **LLM-free**: scan →
  judge (EFE) → act-gate → artifact-only enact → trace → exit. No agent drives it.
- **The Pilot** (`futon3c/README-pilot.md`, `war_machine_pilot.clj` `begin-live-cycle!` /
  `close-live-cycle!`) is the only place an **agent drives the whole loop**, producing the
  γ-frame trace via `futon3c/src/futon3c/aif/repl_trace.clj`.

**Doctrine constraint (`futon3c/holes/GROUND-CONTROL.md` §0):** self-referential WM work is done
*directly* by commissioned agents, **not** through WM enactment — "the machine does not eat
itself." This boundary is **doctrine, not code-enforced** (see G5).

> **The fork for the paper trace:** a detailed end-to-end trace = the **Pilot** (whole-loop
> driver → γ-frames). A fold-authoring follow-mode flight produces *deposits*, not a trace.
> The simulator below is written for the **Pilot** run, with the tick + escrow as the
> automation it sits on.

---

## 1. Ordered pipeline steps ("from the top") — the LLM-free tick

| # | Step | Function / file:line | Agent? |
|---|------|----------------------|--------|
| 1 | Entry / driver (cron one-shot JVM) | `wm-scheduled-run/-main` `scripts/wm_scheduled_run.clj:66` | auto |
| 2 | Refresh belly (live C-vector) | `cv/maybe-refresh!` `:97` | auto |
| 3 | Scan stack + judge → candidates + rank | `wm/generate-war-machine` → `judge` → `efe/rank-actions` `war_machine.clj:4298/3648/3914` | auto (EFE) |
| 4 | Candidate proposal | `action-proposer/propose` `action_proposer.clj:28` | auto |
| 5 | Close-the-loop entry (live-wire gated) | `enact/close-loop!` `enact.clj:235/106` | auto |
| 6 | Build cascade lane (shells minilm cascade) | `cascade-lane/cascade-lane` → `cascade_serve.py` `cascade_lane.clj:339` | auto |
| 7 | Act-gate per entry, reconcile ΔG | `enact/act-gates-with-shown` → `cl/act-gate-from-lane-entry` `enact.clj:132`, `close_loop.clj:55` | auto |
| 7b | **FOLD impl #2 (LLM-turn), REPLAYED from escrow** | `fl/llm-fold` w/ `esc/escrow-turn-fn` `fold_llm.clj:69`, `fold_escrow.clj:196`; deposits `enact.clj:91` | **replay** (turn authored out-of-process) |
| 8 | Verdict (ΔF>0 ∧ ΔG<0 ⇒ :pass) | `cl/preview-verdict` `close_loop.clj:89` | auto |
| 9–13 | Pick :pass → enact (artifact-only) → thread realized-outcome → trace → γ | `enact/enact!` `enact.clj:206`, `fr/with-realized-outcome` `fold_realized.clj:178`, `policy-precision/fold-realized-outcome` | auto |

**Path B (A3/A6 substrate actuator — invoked separately, the real-write path):** A3 runner →
extract build package from a *reviewed* deposit → **dispatch "build this" to builder Zai** (`a3/dispatch!`
`actuator_a3.clj:525`) → verify Drawbridge proofs → A6 re-rank. `wm_scheduled_run` does **not** call A3.

---

## 2. LLM/agent-role points — does the prompting exist?

| Role | Prompt / spec location | Clear? |
|------|------------------------|--------|
| **Fold turn (7b)** — construct a wiring fitted to the mission | `fold-llm/fold-prompt` `fold_llm.clj:30-49` (inputs + **exact output EDN schema** `{:boxes :wires :terminals :policy-holes}`); procedure `E-live-loop-1-s2-escrow-design.md §D`; record schema §A | **YES** — inputs, schema, acceptance all explicit |
| **A3 builder turn (B3)** — execute reviewed blueprint into real substrate | `a3/build-prompt` `actuator_a3.clj:501-523` (acceptance = substrate write; `:hx/endpoints` schema; evidence shapes); spec `M-actuator-spec-A3-executor.md:21-30` | **YES** — schema-complete |
| **Escrow-author** — assemble/arm/write `ft-*.edn` | GROUND-CONTROL §3 `:180-235`; frozen template `overnight_zai_flight.sh:20-36`; validation `fold_escrow.clj:124-165` (Pins 1/1B/2/3, loud-reject) | **PARTIAL** — spec + validation airtight, but **no writer script** (G1) |
| **Reviewer** — author `:endpoint-bindings`/`:box-bindings` for a deposit | output baked as code maps `actuator_a3.clj:23,62`; `M-actuator-spec-A3-executor.md:3`, `-partial-review.md` | **UNDERSPEC** — output format concrete, **no prompt/checklist** for HOW/when-correct (G2) |
| **Pilot (whole-loop driver)** — READ/EVAL/PRINT/LOOP each step | `futon3c/holes/specs/repl.spec.edn` (V1–V6); `README-pilot.md` "Running a cycle" `:36-54`; backend tools require `:consent-gate-event-id` | **YES for the cycle**; stall risks are the automation it calls (G1/G4) |
| Candidate/cascade gen, judge, verdict, enact | pure automation | n/a — no agent turn |

**Headline:** every role that actually PRODUCES something on a wired path — the fold turn, the
A3 builder, the escrow-author, the Pilot cycle — **has a clear, schema-complete prompt.** The
readiness risks are not missing prompts; they are missing *automation and preflight* around them.

---

## 3. GAPS — where a run would silently stall

- **G1 — No escrow-writer automation (the #1 stall).** The fold prompt + record schema are
  fully specified and validation is airtight, but *producing* a new deposit is a manual 4-step
  procedure with **no assembler script** (search found only verifiers `gate_2f_deposit.clj`,
  `gate_2g_provenance.clj`). For a mission with no deposit, `deposit-for-mission → nil`
  (`enact.clj:100`) ⇒ the fold **silently abstains** (ΔG nil → gate abstains). A Pilot run over
  a fresh mission hits a dead fold with no error. *Close:* an armed authoring path (build the
  `fold-prompt`, capture the agent's EDN, compute ΔG, write the arming-complete `ft-*.edn`).
- **G2 — Reviewer role has an output but no prompt.** `reviewed-*-bindings` are hand-authored
  per mission; a new mission has none; there is no checklist for choosing endpoint types /
  abstract-vs-bindable boxes / acceptance. A driving agent asked to review a fresh deposit
  stalls on "what do I output, and when is it right?" *Close:* a reviewer prompt + acceptance
  spec (the `M-actuator-spec-A3-partial-review.md` is the seed).
- **G3 — A3 builder arming is a hard-coded default, not a run gate.** B3 performs the real
  substrate write but the arming/authorization lives implicitly (`builder "zai-3"` default,
  `actuator_a3.clj:527`); WM-I4 is referenced (`enact.clj:15`) but not wired into the A3
  dispatch. *Close:* a machine-checked arming gate at B3 (the fold deposit's `:arming` pin is
  the model).
- **G4 — No readiness preflight (the most likely "looks stuck").** Path A only enacts when
  FOUR toggles align: `FUTON_WM_LIVE_WIRE≠0` (`wm_scheduled_run.clj:30`) ∧ `*escrow-replay?*`
  (`close_loop.clj:32`) ∧ a sha-matching deposit exists ∧ `*classical-fold-dG?*`=false
  (`close_loop.clj:41`). No single preflight reports armed/not — a run can no-op (γ holds) with
  no surfaced reason. *Close:* a one-shot preflight that prints the four toggles + deposit
  presence for the target mission before the run.
- **G5 — Driver-vs-author boundary is doctrine, not code.** Nothing mechanically stops a
  dispatched agent from misreading the Pilot README and attempting WM enactment on
  self-referential work that GROUND-CONTROL §0 says must be done directly. *Close:* state the
  role explicitly in the dispatch bell; long-term, a code guard.

---

## 4. Failure modes (both named in doctrine, neither mechanically prevented)

- **"Announced is not sent":** caught **downstream**, not at emission — the escrow loader
  rejects fabricated/tampered deposits (`fold_escrow.clj` Pins), the mana-gate ledger records
  every spend, gates re-run. An agent *can* claim a deposit it never wrote; the claim just fails
  to become evidence (no `ft-*.edn` ⇒ nothing loads ⇒ gate 2f red). Mitigation for the run:
  write artifacts as you produce them; review ≠ author.
- **"Silent turns":** turns are **inspectable** (every text/tool_use event → the invoke-jobs
  ledger `http.clj:362/3086`, rendered by `agent-follow-mode.el`) but **not forced** — no
  invariant that a turn emits an artifact. The mana-gate forces the *first* act to be a
  consume-or-refuse; the Pilot's PRINT tools require a consent-gate-event-id; but a no-op turn
  is *visible*, not *rejected*. ("Turn-native has no in-flight brakes" — a wedged tool can't be
  cancelled without a JVM restart; the authors flag forced-emission/bounded turns as open.)

---

## 5. Readiness verdict + preflight checklist

**Verdict:** the LLM *prompting* exists and is clear for every producing role. The machine is
**not run-ready for a fresh-mission Pilot trace** until G1 (escrow-writer) and G4 (preflight) are
closed — those are the two that turn a stall into a *silent* stall. G2 (reviewer prompt) is needed
only if the run reaches Path B (real substrate writes); a deliberation-only Pilot trace can defer it.

**Before the live run, confirm (the preflight):**
1. The four Path-A toggles are set as intended for the target mission (G4).
2. The target mission(s) have armed `ft-*.edn` deposits present + loader-accepted (G1) — OR the
   run is explicitly scoped to missions that already have deposits.
3. The dispatch bell states the role (Pilot whole-loop driver vs escrow-author) explicitly (G5).
4. Operator follow-mode is attached so no turn is invisible.
5. Trace sink (`data/repl-traces/<run-id>.edn`) is writable and the V1–V4 spec-verifier is ready.

**Recommended sequence:** close G1 (a small escrow-writer script) + add the G4 preflight →
dry-run the Pilot over ONE mission that already has a deposit (no new authoring needed) → confirm
a clean γ-frame trace end-to-end → then open the aperture to fresh missions.
