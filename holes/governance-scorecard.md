# Governance scorecard — instrumentation for T4.3 and T4.4 (Pudding Prover goals)

**What this is:** the pre-registered instrument for two goals that otherwise
stay vibes: T4.3 (Ostrom-style commons governance of the self-improving
system) and T4.4 (the commodity-hardware RSI claim — the Anthropic hook).
Cards-II discipline: each row names its OBSERVABLE (an existing ledger or
gate wherever possible), its CURRENT STATUS honestly, and what ACHIEVED
would look like as a checkable condition. Statuses re-assessed at each
loop-series close; the falsifiers are as binding as the goals.
Cross-ref: M-demonstration-foundry carries the framing narrative (operator
→ Codex, 2026-07-06); this file carries the numbers. Companion:
`mathlib4/DarkTower/WMPipelineExample.lean` (rules-as-machinery precedent),
`futon3c/holes/GROUND-CONTROL.md` (the nested-delegation layer).

## T4.3 — Ostrom's eight principles, mapped to observables

| # | Principle | Observable (exists today?) | Status 2026-07-06 | ACHIEVED means |
|---|---|---|---|---|
| 1 | Defined boundaries | Agency roster (who) + mana gate-ids (which commons) + capability lists | PARTIAL — roster + one gate live | every consent-gated resource has a gate-id; no un-gated spend path |
| 2 | Congruence with local conditions | award sizing vs actual flight costs (h5, open since deposit-006) | OPEN — awards are round numbers, not priced | award sizes derived from measured cost-per-flight (the mana ledger already records spend-vs-fruit) |
| 3 | Collective-choice arrangements | operator rulings recorded verbatim w/ design consequences (E-live-loop-3, charters) | LIVE for operator; agents propose via checkpoint/proposal-first | a recorded amendment path: agents can PROPOSE rule changes through a named channel and see them ruled on |
| 4 | Monitoring | mana ledger (every spend), gate cadence (~30min standing re-runs), futility board, regression suite | LIVE — the strongest row | unchanged; the falsifier is any consent spend or gate flip found OUTSIDE a ledger |
| 5 | Graduated sanctions | refusal-at-zero (mana), reject-loudly (loader), pin-1b | PARTIAL — one grade exists (refuse); no escalation ladder | a written ladder: refuse → quarantine → deregister, each grade witnessed at least once (synthetic OK) |
| 6 | Conflict resolution | the whistle (crossing-immune sync; E-crossed-bells) + reviewer arbitration | LIVE at agent grain; UNTESTED at rule grain | one real rule-dispute resolved through a recorded mechanism (the cycle-5 warrant discipline is the same idea at measurement grain) |
| 7 | Minimal recognition of rights | consent semantics (mechanical vs semantic), PARs as agent voice, operator-not-sovereign doctrine | PARTIAL — doctrine exists in memory/feedback, not in machinery | agent-side rights named in GROUND-CONTROL (e.g. the right to refuse un-mana'd work; stuck-means-signal as a right, not just advice) |
| 8 | Nested enterprises | GROUND-CONTROL handover (frontier → codex → zai layers); ground-control test 2026-07-06 | LIVE, one level deep, validated by execution | two levels: a ground-control agent commissions ANOTHER ground-control agent for a sub-series, gates green throughout |

**T4.3 ACHIEVED (pre-registered):** all eight rows at LIVE with their
falsifiers standing, plus ONE full loop-series run under agent ground
control where every consent event, sanction, and dispute in the series is
reconstructable from ledgers alone. **Falsifier:** any consent spend, gate
flip, or sanction that cannot be reconstructed from the ledgers — one such
event resets the row to PARTIAL regardless of other evidence.

## T4.4 — the commodity-hardware claim, as accounting

**The claim to instrument:** the self-improvement loop runs on commodity
substrate (laptop + cheap APIs), with frontier models OPTIONAL — not that
frontier models were never used to BUILD it (they were, witnessed), but
that OPERATING and IMPROVING it does not require one.

| Observable | Instrument | Status 2026-07-06 |
|---|---|---|
| Cybernetics substrate | the tick/constructor/gates stack: laptop JVM + CPU MiniLM + cron | LIVE on commodity since inception |
| Plan authoring w/o frontier | ground-control test: 3/3 GLM deposits valid first-pass from written instructions | FIRST POSITIVE DATUM (n=3; transport failures excluded, separate denominator) |
| Review w/o frontier | mechanical gates (pin-1b, 2f, regression suite) replace frontier review for VALIDITY; quality scoring still open (peradam certificates = the designed instrument, awaiting scorer-pool ruling) | PARTIAL — validity mechanized, quality not yet |
| Quality under frontier absence | reference_regression.clj: 8 frozen checks; per-deposit pin table | LIVE — drift is loud; run at each series close |
| Library growth w/o frontier | authoring doctrine + seeds pipeline runnable by GLM agents (zai-8 ran ingestion + edge fix solo, 2026-07-06) | FIRST POSITIVE DATUM (authoring itself still frontier-done both times) |
| **Resource ledger per flight** | flight logs gain a substrate line: which stage ran on laptop / commodity API / frontier API | **TO-BUILD — the missing instrument.** Convention: every flight log ends with `substrate: {stages... :frontier-touches N}`; the T4.4 curve is frontier-touches-per-flight over time |

**T4.4 ACHIEVED (pre-registered):** one full loop-series (charter →
deposits → ticks → verdicts → ledger close) with `frontier-touches: 0` in
every flight log, quality gates green, and the regression suite
byte-stable — then the claim is a witnessed fact and the EOI's forward
case becomes a backward one. **Falsifier:** quality gates degrade under
frontier absence (regression drift, deposit validity rate drops, or gate
pass rates fall) — that would mean the frontier contribution was
load-bearing in a way the instruments must then locate.

## Standing note

The operator's framing (2026-07-06): the current work is the CYBERNETICS;
governance concepts appear embryonically (who do we signal to when stuck —
the nag bulletin; who may spend — the mana gate). Scaling from governing
the loop to governing HOW THE LOOP IS RUN is the T4.3 endgame, and the
GROUND-CONTROL handover is its first rung. This scorecard is re-assessed,
not rewritten: statuses move with dated evidence pointers, in the cards-II
edit discipline.
