# BUILD status — one page, kept current (owner: claude-15)

Parent mission: `../missions/M-formal-war-machine.md` (§Deliveries links back here; chartered under `futon4/holes/mission-lifecycle.md`, currently §3 DERIVE). Build method: `futon4/holes/delivery-lifecycle.md` — the bridge between the two is at the head of the mission's §3.

Updated: 2026-08-30 evening. The ledger (`BUILD-ledger.md`) is the log; this is the view.

## Top-level moves (the gasket as it develops — lifecycle §0.12 register)
1. The problem stated: the big tetrahedron (§0.8, 08-30). 2. The evidence vertex subdivided: the evidence tetrahedron (§0.12, 08-31) — "what is the evidence of the evidence?"

## The precepts now standing
`I_data_current` · `I_absent_is_loud` · `I_evidence_consumed` (lifecycle §0.7, §0.11; PREREG §4) — data is live, absence is loud, every emission names its consumer.

## The goal, in one line
Rebuild the War Machine — stated **without any preferences in it** (C is a parameter; a deployment is the machine plus a recorded layer stack: a person, a company, a domain) — so that every term is defined on Active Inference's terms in Lean, every implementation
is held to that definition by a run that Lean can check, and every read is against live data — so it is a war
machine, not an archive machine. **And extend it as we understand what it is** — repairs that turn out to be
extensions get a supersession record, not a deletion (first case: `P-supersede-stack-logic-model.md`).

## Where each lane is

| lane | what it is for | where it is | holder | next | needs Joe? |
|---|---|---|---|---|---|
| **Spine (R5)** | G, cascades, policy, Outcome/C — the theory core | typed and decided in records; Snatch microcosm witnesses nonDegenerate; C and EIG still undefined for the WM | claude-15 | nothing until the two-π decision | **yes: the two π's** |
| **Glossary → Lean** | the mathematics of AIF as Lean definitions | done: 33/33 Formal lines; theory core in Lean (34 bodies / 32 holes) | claude-15 | G-D2 = the Formal lines into the paper | **yes: G-D2 (your paper)** |
| **Lean ↔ Clojure adapter** | Lean exports the contract; Clojure lint judges every hole against recorded runs | working end to end; 2 witnessed live, 1 wrong-shape (lint lags its type), staleness too coarse | claude-15 | AD-D3b (lint follows the new EraTable), AD-D5 (lint runs the checks; per-declaration staleness), AD-D4 (APM onto the same emitter) | **yes: APM authority (apm-lean vs mathlib4-apm-validation)** |
| **Node R9** (no self-certification) | the rule as a checker | **closed**: holes discharged by kernel `decide`; the argument has mass (3 named-agent rows) | — | nothing | no |
| **Node R8** (F and g) | F stated and witnessed; g honestly a controller | **closed** for F: census + era law discharged; g blocked on the spine | — | nothing until Outcome/C exist | no |
| **Node R2** (observation vector) | the channel contract | **closed** for the contract (2 firing of 792); turn channel = a design decision | — | R2-D3 design decision | **yes: the turn channel** |
| **Node R16** (grounded actuation) | the registry and the witness | discovery done (`:enacted nil` untyped); **four independent role-played nodes converged on the same gap from the other side** (R16→R14/R15/R7/R8: the act's result never comes back — node-sim report, §Deliveries); build not started | claude-20 | R16-D2 after Lean declarations ratified | no |
| **Wiring (CML)** | edge schemas between nodes | 2 of 21 edges have entries; **0 fully specified** (operational fields unstated by both records) | claude-15 | endpoint amendments for operational fields | no (unless you want it to move now) |
| **Library (spider)** | @why/@how edges with evidence | wave 1 done: 19 attested of 68; rung 1 reads the live store, basis-pinned, **reflection-excluded by provenance** (4d `36a9c63` gated): in-use coverage aif 7/33, wc 7/23, war-room 17/28 by the strictest column; **wave 2 running** (war-room included, no special handling) | codex-20 | gate wave-2 harvest | no |
| **Data currency audit** | find every stale readout the machine or instruments read | **done** (AUD-D1 `d1997fc`): 3 real snapshot reads (evidence export → 4c; mana snapshot, timer missing; mark2 cache, no endpoint); WM core files are canonical/own-output, current by construction; **2 report inputs never existed** (`stack-logic-model.edn`, `alignment.edn` — silent `when-let`) | claude-15 | AUD-D2 **done** (lint exits 1 on 7 silent+absent reads; 3 never-produced files); AUD-D3 landed but **not passed** — callers swallowed the marker and the lint couldn't see it; AUD-D3b (markers reach the report + trace) and AUD-D4 (lint follows the value) running in parallel; then mana timer; mark2 endpoint | no — decided: stack-logic model was planned 05-03, never produced; **superseded, not rebuilt** — it was a hand-written operator model; R2/`sec-operator.tex` is its successor (`P-supersede-stack-logic-model.md`); dead reads deleted citing that; rule recorded as `I_absent_is_loud` (lifecycle §0.7, PREREG §4) |
| **R19 preferences C** | who supplies the preferences, and how they are layered | **stated** (`P-R19-preferences-open.md`, PROPOSED): C is a parameter; a layer has an author and a basis; composition is recorded; today 4 sources, none declared as such | claude-20 (lane) / claude-15 (gate) | **R19-D1 done+gated** (`dc1dac8`): FIVE sources (zone-load was uninventoried), four folded; habit prior LIVE in Q(π) since the 07-13 flip (stale comments said dark); declared purpose honestly nil — "this stack is what accumulated"; D2: Lean ratified (`75efc81bf3`); **D2a gated** (`ccd06ce`: every `compute-efe` result emits its five-layer stack, habit prior declared-abstention included; no arithmetic changed, 554 assertions green); **LANE CLOSED** — every C entry and every EFE result names its preference stack; the hole is witnessed end-to-end; the standing question it leaves is yours: no declared purpose exists ("this stack is what accumulated") and promoting the habit prior from counterfactual to selector is a deployment decision | no — principle ratified 19:35Z |
| **Second domain (APM proofs)** | the next microcosm | assessed only; nothing written | — | on your word, after the above | **yes: go/no-go** |

## What is running right now
- AUD lane **complete except mark2** (your call): D1–D5 + D4b all gated; `## Input status` renders, the trace carries `:input-status`, and the mana snapshot refreshes every 5 min under its own timer. AUD lane remaining: **mark2 only** (your endpoint-vs-refresh call).
Nothing else. Every builder lane is closed and gated.

## Your open decisions (none blocking a running lane)
1. The two π's — the glossary's scored-cascade π vs `Holes.Policy := InformationState → Action` (`cascadeGrainPi`).
2. G-D2 — the 33 Formal lines into `sec-glossary.tex`.
3. APM regeneration authority (`apm-lean` vs `mathlib4-apm-validation`) before AD-D4.
4. The R2 turn channel (what content of a turn→pattern association normalises to [0,1]).
5. Second domain go/no-go.
6. ~~The 10:54 WM trace writer~~ identified: a manual WM report build with trace on (`war_machine.clj:4720`; the write is wrapped in `(catch Exception _ nil)`). Its `:decision` folded a `:strategic-memory` last observed **2026-07-24**. No unattended tick has run since R10 stopped: last nightly 23:00 trace 07-05; no unit, no cron, entrypoint not in the repo; zero ticks 07-21→08-30. Decision reframed by you (08-31): on-demand single ticks are first-class — no cron race needed. **WM-RUN1 running**: `wmRunsOnce` ratified in Lean (falsifier currently firing); codex-5 is making one cold tick complete with the selector seam declared, receipt matching the Lean fields, witnessed. The cron question returns only after runs-once is witnessed.

## The one-sentence health check
Types and laws: mostly in place. Runs: three nodes discharged. Data: one corpus was stale and is being fixed;
the audit found two more (mana, mark2) and two report inputs that never existed. Wiring: barely started. Process: it caught its own facades all day.
