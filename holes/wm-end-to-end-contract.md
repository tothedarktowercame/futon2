# The War Machine end-to-end contract — a definitive statement (proposed)

**Why this exists (Joe, 2026-07-08):** there has never been a final statement of *what the
end-to-end "from the top" pipeline is* or *what state the WM maintains between runs* — the current
picture is scattered across path-dependent fragments (the pilot contract `M-pilot-appearance.md`, the
`aif-cascade-loop-live.html` ring, the `flight-pipeline-cards-iv.html` ledger) and the newer actuator
(A3–A7) that *took over the tail* of the older pipeline. This doc reconciles all of them into one
statement. It is **proposed, for ratification** — not decreed; where it supersedes a fragment it says so.

Synthesized from a code-grounded reconciliation of the four sources + the newest build (2026-07-08).

---

## 1. The role is ONE, not three (the merge)

The apparent three roles — **whole-loop driver** (the Pilot), **fold-turn author** (the out-of-process
escrow writer), **follow-mode observer** (the operator) — are *one role fractured by path-dependent
implementation*:

- The **author** was split out-of-process so the hot loop stays LLM-free and mana-accounted, and the
  fold is *replayed* (hash-gated) rather than re-invoked.
- **Follow mode** is just the operator's lens on the driver.
- The **Pilot** is the driver.

**The coherent unified role:** *one agent drives `READ→EVAL→PRINT→LOOP`, and wherever the loop needs a
fold it does not already have, that same agent authors it inline — becoming the author for that step —
observably.* The out-of-process escrow *flight* collapses into **a step within the run** ("author a
missing fold"). This is exactly what gap **G1** (the escrow-writer) becomes in the unified role: not a
separate flight, but the pipeline's authoring step. The mana/hash-gating discipline is preserved as
the *guard on that step*, not as a reason to keep it a separate role.

The Pilot's cycle vocabulary (`repl.spec.edn`: R/E/P/L as the differential operator `v·∇`, a γ-frame
per step, PRINT tools gated on a `:consent-gate-event-id`, real writes operator-armed, the pilot may
*refuse* a WM-I4-violating write) is **current** and is the driver altitude. The pilot's *storyboard
manifestation* (the four-reader `.cljc` scene list, the kāya/vedanā/citta/dhammā interpretive lattice)
is **historical-only** — a prototype that stayed a skeleton; it is not the pipeline.

---

## 2. The end-to-end ring (the definitive step sequence)

The pipeline is a single **belly-hubbed ring, driven from above by the Pilot's R/E/P/L**. Forward arc:

```
 wake → scan/judge (drift-fenced) → build ψ (seat-by-state) → condense cascade (ΔF)
   → fold → deposit v2 (escrow-replay, CT/CLean-typed) → price ΔG
   → act-gate (ΔF>0 ∧ ΔG<0) → ENACT → REALIZE → γ (R14) → τ_eff = τ/γ  ─┐
   └────────────────────────── the return leg re-weights the next softmax ─┘
```

The **belly (R19)** is the hub, not a station: ~455 live preferences whose predictive risk re-ranks
every candidate the gate sees.

| # | Step | Status | Note |
|---|------|--------|------|
| 0 | **Pilot R/E/P/L** whole-loop driver (γ-frame/step, consent-gate on PRINT, operator-armed writes) | **current** | the driver altitude; merges the three roles |
| 1 | **Wake / Entry** — cron one-shot JVM; refresh belly (C-vector) | current | |
| 2 | **Scan & judge** — drift-fenced (superseded-mission classifier) + EFE rank | current | |
| 3 | **Build ψ (circumstance)** — seat-by-state (held-work ledger + STUCK line; seated entry pattern); the "magnet" have→want condense | current | supersedes the coarse "magnet ψ" naming |
| 4 | **Condense cascade** — position-aware; a thin ψ triggers **authoring** (→ the inline fold step) | current | |
| 5 | **Fold → deposit v2** — escrow-replay only, SORRY-typed, CT/CLean layer (PIN-1B loader-reconstructs the prompt; PIN-4 type-checks through `clean_to_lean.py → DarkTower`, 0-sorry) | **current** | supersedes the "rollout → classical → escrow, three ΔG sources" fold (rollout/classical legs **retired on theory** — the level error) |
| 6 | **Price the plan (ΔG)** — pinned escrow replay; classical fold OFF | current | |
| 7 | **Act-gate / select** — `ΔF>0 ∧ ΔG<0 ⇒ :pass`; γ per lane; abstain/fail are *recorded verdicts* | current | identical predicate across all sources |
| 8 | **Enact** | **superseded → 8b** | the old "artifact-only, deterministic engine rebuilds what it can, realized-G = its own coverage" is a **mirror** |
| 8b | **A3 substrate actuator (real write)** — extract package from a *reviewed* deposit → `dispatch!` "build this" to a builder agent → builder `submit-tx` writes bound endpoints to substrate-2 → **endpoint-gated dial**: moves only via a live Drawbridge `xtdb.api/q` proof (never builder claims) | **current** | the real-write path; `*live-wire?*` default ON (2026-07-08) |
| 9 | **:realized-outcome via coverage** (`realized-outcome-of`; coverage-vs-coverage; scale ΔG) | **superseded → 9b** | the reproduction mirror |
| 9b | **:realized-outcome grounded (A5)** — `realized-outcome-grounded`: `:realized-G = bound − inhabited` endpoint counts; `:realized-source :substrate-dial`; scale = endpoint-count | **current** | `*gamma-grounded-feed?*` default ON (2026-07-08) — γ eats the world dial |
| 10 | **γ (R14)** — precision over policies; 5-sample burn-in; earned-vs-held prior; now fed by the grounded dial | current | |
| 10b | **A6 status-aware re-rank** — `flip-star-status-on-discharge! → rank-with-star-status → closure-falsifier` | **current** | supersedes plain EFE re-rank once a real discharge lands |
| 11 | **τ_eff = τ/γ** — return leg, re-weights the next softmax | current | |
| 12 | **Trace, observe, learn** — futility board, nag bulletin, co-app mined from deposits → next tick's ψ | current | |
| 13 | **Recommendation responds to work** — return arc at recommendation grain (board→G→recommendation), attributable changes | current | |

**The single evolution to state plainly:** the older sources (ring, cards, simulator) all end the tail
in *artifact-only re-observation over coverage-ΔG* — a mirror. The newest build moves the whole
**enact→realize→γ tail** onto a **grounded substrate write (A3) + grounded realized-outcome
(`bound−inhabited`) + A6 status-aware re-rank**, with live-wire and grounded-γ-feed both **default ON**.
Everything upstream of the act-gate is unchanged; everything downstream is the actuator.

---

## 3. What state the WM maintains between runs (the cross-episode memory)

The genuine cross-run persistence lives in **three physical stores**: the daily EDN **trace**
(`futon2/data/wm-trace/`), the external **XTDB substrate** (via Drawbridge :6768/:7071), and on-disk
**EDN corpora** (futon6 fold-turns + c-vector overlay, futon3c repl-traces). Everything else is an
in-process atom or re-derived per JVM. A new tick reads the previous via
`trace/latest-trace-record` at the top of the judgement build.

| State | Where | Persisted? | Read by next run? |
|-------|-------|-----------|-------------------|
| Per-channel `:precision-state` (8-channel rolling PE precision) | trace | **yes** | **yes** — continues the rolling window |
| γ / `:policy-precision` (`:samples :perf-history :mean-perf :last-outcome-tick`) | trace | **yes** | **yes** — burn-in, bounded history, tick-dedup |
| `:realized-outcome` (`{:policy :expected-G :realized-G :tick}`) | trace | yes (present-only) | **yes** — next tick bends γ |
| Substrate: capabilities, `:capability-star`, `:capability-star-status`, discharges | **XTDB** | **yes (the world)** | **yes** — A4a/A6 read + write it |
| Escrow fold corpus (`ft-*.edn`) | futon6 | yes | replayed (hash-gated) |
| Belly overlay channels | futon6 c-store-overlay.edn | yes (semi-live) | folded into `refresh!` |
| **Belief posterior μ** (`:mu-pre`/`:mu-post`) | trace | **written** | **NO — re-derived** from `stack-annotations.edn` every run |
| Belly C-vector (stated channel) | in-proc atom ← :7071 | re-derived | re-derived live (freshness-guarded) |
| Pilot γ-frames (`data/repl-traces/`) | futon3c | yes (per-run) | **aggregate calibration only**, not control feedback |

**What the WM genuinely remembers (learned itself, read AND written):** exactly **two precision
scalars** — the per-channel `:precision-state` and **γ** (`:policy-precision`, with samples /
perf-history / burn-in / dedup) — **plus the external world** (the XTDB substrate it acts on and
re-reads). That is the closed learning loop: `enact` writes `:realized-outcome` → next tick's
`fold-realized-outcome` bends γ.

**What it re-derives fresh (no memory):** the **belief posterior** (re-bootstrapped from stack
annotations each run) and the belly stated channel (re-derived from :7071 — correct, since :7071 is
the durable backing).

**Dead memory (written, never read back) — the honest gap:** the **belief posterior μ** is serialized
to the trace every tick but **no reader consumes it**; belief resets to the annotation-derived prior
each run, so **Bayesian belief does not compound across episodes.** The code itself flags this as
unfinished (`war_machine.clj:3696`: "*R8 trace persistence WILL carry belief across calls*" — future
tense). Likewise the pilot's γ-frame `:learning` block is self-labelled "ADVISORY-ONLY … NOT
WM-actionable" — cross-run *calibration evidence*, not cross-run *control memory*.

> **The honest one-liner for the paper:** between runs the War Machine remembers *two precision
> scalars and the world it acts on* — its precision calibration (`:precision-state`, γ) compounds, and
> the substrate it writes persists. Its **belief state does not** — that is re-derived from annotations
> each run (the acknowledged R8 seam). So the cross-episode learning that is real is
> *precision/confidence learning*, not *belief/model-posterior learning*.

---

## 4. Gaps that block a clean end-to-end run (the preflight)

| Gap | What | Closes with |
|-----|------|-------------|
| **G1** | No inline fold-author — a mission with no `ft-*.edn` deposit silently abstains (γ holds, no error) | the "author a missing fold" step of the unified role (build the `fold-prompt`, capture the agent's EDN, compute ΔG, write the armed deposit) |
| **G4** | No readiness preflight — four+ toggles must align or the run no-ops silently | a one-shot preflight printing the arm state + deposit presence for the target mission |
| **G6** (new) | **Belief cross-run persistence** — μ is dead memory (written, never read); belief resets each run | wire `latest-trace-record`'s `:mu-post` back as the next run's prior (the R8 seam), OR state plainly that only precision-learning compounds |
| G2 | Reviewer role (endpoint/box bindings) has an output but no prompt/checklist | a reviewer prompt + acceptance spec (seed: `M-actuator-spec-A3-partial-review.md`) — only bites on Path B (real writes) |
| G3 | A3 builder arming is a hard-coded default, not a run-gate | a machine-checked arming gate at the substrate-write step (model: the deposit `:arming` pin) |
| G5 | Driver-vs-author boundary is doctrine, not code | in the unified role this dissolves — the driver *is* the author; state the role in the dispatch |

**Recommended first end-to-end trace (lowest risk):** close **G1 + G4**; run the unified Pilot over
**one mission that already has a deposit** (no new authoring, no substrate write) → one clean γ-frame
trace; then open the aperture to fresh missions (needs G1's inline author + G2 if Path B). **G6 is
a decision, not a blocker** — either wire belief persistence or state the precision-only-learning
scope; the paper needs it stated either way.

---

## 5. Relationship to the fragments

- `M-pilot-appearance.md` — driver *cycle vocabulary* current; *storyboard manifestation* historical-only.
- `aif-cascade-loop-live.html` (2026-07-02 ring) — the forward arc is current; its **tail is superseded** (artifact-only / coverage-ΔG → grounded).
- `flight-pipeline-cards-iv.html` (10-card ledger) — the **most current fragment** for cards 1–7; its enact/realize tail (cards 8) is superseded by A3/A6/A5.
- `wm-pipeline-simulator.md` — the baseline step-map + the three-role framing this doc **merges into one**.
- `actuator_a3.clj` / `actuator_a6.clj` / `fold_realized.clj` — the superseding grounded path (the current tail).
