# E-outer-loop — the War Machine no longer chooses its own work

Date: 2026-09-24
Parent: the PROOF-2 plan (`labs/wm-contract/PROOF-2-STRATEGY-draft-2026-09-24.md`).
Sibling: `E-cascade-real.md` (the machine does not build its cascades). This
excursion takes the other passive step: the machine does not choose what to
work on.
Owner: claude-10. Driver: Joe.
Status: OPEN — the old loop and its removal are written down (O1–O8);
investigations running.

## Joe's framing (dictated 2026-09-24, voice transcript)

> "The war machine can't choose its own work ... it's allowed to do work on
> something in a queue whereas before, we had an outer loop whereby the war
> machine would choose its own work. And one of the considerations that it
> would choose that work based on is feasibility."

> "You said at one time it could, and now it can't. I mean, that is a
> critical finding for an active inference system."

## What the outer loop was (code at `5d55e7a0^`, 2026-09-17)

Read from `git show 5d55e7a0^:scripts/futon2/report/war_machine.clj`
(line numbers refer to that revision).

1. **Proposing (:6808).** Every tick, `action-proposer/compose-proposers`
   ran six proposers over the whole substrate:
   - `ap/bootstrap-proposer`: `:no-op`, plus one `:learn-action-class` per
     action class with no concrete actions (docstring cites Joe
     2026-05-17: "when no concrete actions exist for an action class, the
     highest-priority recommendation is to enable the class itself");
   - `pattern-registry/pattern-enumerator-proposer`;
   - `mission-registry/mission-enumerator-proposer`;
   - `mission-registry/ticket-enumerator-proposer`;
   - `sorry-registry/sorry-enumerator-proposer` (open Lean `sorry`s);
   - `tension/tension-proposer` (high-curvature substrate nodes, M-aif2).
2. **Valuing.** The proposals were enriched with structural pressure,
   mission value from recent trace records, the interest-network posterior,
   and a task-belief ladder; time pressure came from the anticipation
   snapshot.
3. **Feasibility (:997, :7217).** `arena-graph-feasibility-mode` defaulted to
   `:policy-support`: "graph applicability is a policy support condition,
   not a value penalty" (typed-residual remediation, 2026-07-13). Infeasible
   proposals were excluded from `:admissible-actions` — "the decision is
   drawn from this executable support ... the same Π_feasible domain" —
   and the exclusions were recorded as `:policy-support-exclusions`.
4. **Choosing.** `efe/rank-actions` scored the admissible set by G and
   `policy/select-action` chose, with a `default-mode-select` fallback.

A cascade lane (`:cascade-policies`) and a horizon gap-scan (open-mission
classes with thin pattern coverage) ran beside it as advisory rows.

## What removed it

`5d55e7a0` (2026-09-17), "H5b: judge enacts the gated cascade decision or
abstains; flat decision path deleted (Joe 2026-09-17)". The commit message
lists as deleted: "compose-proposers candidates, channel ranking,
select-action/default-mode-select, flat authorize, advisory cascade rows,
flat carries" and the structural-pressure, gap-view, star-map, ROI and
mission-fold helpers. The comment it left (`war_machine.clj:7025–7035` at
HEAD) gives the reason as "a cascade is a policy, and G is computed over
policies".

## Defects

**O1. Nothing proposes work.** The judge calls none of the six proposers.
What survives is the mission and ticket *enumeration*:
`cascade_problems/substrate-targets` lists the same ids the mission and
ticket enumerators produce, "as TARGET IDENTITIES, not the flat
:advance-mission / :advance-ticket actions those proposers construct". So the
machine still sees its missions and tickets; it no longer proposes anything
to do about them. All six proposers still exist in `src/` and are not called
from the judge.

**O2. The move from actions to cascades dropped the proposing step, not
just the flat ranking.** The cited reason — G over policies, a cascade is a
policy — says what the machine should choose *among*. It does not say the
machine should stop generating the set. The replacement has a target list
(substrate missions, declared targets, the proposal supply, the ticket
queue: `war_machine.clj:7072–7080`) but nothing that turns a target into a
policy (E-cascade-real D3–D4). I1 (`0b06df90`) located the ruling text in
`p4ng/wm-walkthroughs/build-loop/closure/FOCUS.md` Priority 0: three rulings,
all about the decision, none about proposing.

**O3. Feasibility changed from a support condition to a refusal.** Before,
infeasible proposals were excluded and the rest competed, so the machine
always chose among what it could do. Now a target without a declared
candidate is refused, and if every target is refused the tick abstains.
Nothing ranks feasible alternatives.

**O4. The "learn" move is gone.** The bootstrap proposer turned "cannot act
here" into a proposal to enable that kind of action. The cascade path
records an `:environmental-hold` and abstains (E-cascade-real D7).

**O5. The value signals are dead code.** `enrich-candidates-with-mission-value`
(`war_machine.clj:2366`) and `apply-task-belief-ladder` (:2536) are still
defined in the judge and never called; the interest-network enrichment and
time pressure are no longer applied. Whatever the machine had learned about
which work was worth doing no longer reaches a decision.

**O6. Sorrys, patterns and tensions left the machine's view.** The target
list is missions, tickets, declared targets and the proposal supply. Open
Lean `sorry`s, patterns and tension nodes, which were proposal sources
before 09-17, are not on it.

**O8. Nothing was ever planned to choose which targets to work on.** I1 Q4:
the H-series planned H7f, feeding constructed families for *given* targets
into the sources (never landed). No H-series row plans a replacement for
what the old proposers did, which was decide which missions, tickets,
sorrys and patterns were worth acting on.

**O7. No record says what was lost.** H5b's message lists deleted code, not
the capability — choosing among missions, tickets, patterns and sorrys by
feasibility and value — that went with it.

Not yet established: whether the old loop chose *well*. The pre-H5b comment
at the task-belief ladder mentions a plateau of "55 candidates at one
:mission-value-factor", so it may have chosen among near-ties. I3 below asks
what the records show.

## Investigations

| id | question | who | job | status |
|---|---|---|---|---|
| I1 | (shared with E-cascade-real) history of the pre-H5b proposers and ranking; primary text of the 2026-09-17 instruction H5b cites | kimi-2 | invoke-1790256229096-23668-ed1a1a26 | done: `0b06df90` |
| I3 | Records: over the tick records before 2026-09-17, what did the outer loop propose and choose — counts by action type and proposer, feasibility exclusions, near-ties — and did the chosen work get done? | kimi-5 | invoke-1790256941064-23670-b64b5491 (park-cd984e66) | running |

Read-only: no clicks, no writes under `data/`, no shared-JVM loads.
