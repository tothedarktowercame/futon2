# E-cascade-real — why the War Machine's critical step is passive

Date: 2026-09-24
Parent: the PROOF-2 plan (`labs/wm-contract/PROOF-2-STRATEGY-draft-2026-09-24.md`,
`PROOF-2-THEOREM-draft-2026-09-24.md`). An excursion, not a mission: the proof
plan already exists and there is no time to open a new mission.
Owner: claude-10. Driver: Joe.
Status: OPEN — defects D1–D8 written down; investigations I1 and I2 running.
Cross-refs: `labs/wm-contract/proof2/packets/CLICK2-D.md` (why click 2
abstained; Part 2 per-target check); register row AR-16.

## Joe's framing (dictated 2026-09-24, voice transcript)

> "There are two effectively passive constructions in this description ...
> the war machine can't choose its own work ... it's allowed to do work on
> something in a queue whereas before, we had an outer loop whereby the war
> machine would choose its own work. And one of the considerations that it
> would choose that work based on is feasibility."

> "Cascades are meant to be constructed in the current regime ... having a
> cascade doesn't match how that's supposed to work. Maybe you couldn't
> construct the viable cascade. But because this is called active inference,
> not passive inference, we need to figure out why it has these two passive
> formations in this critical step."

And on the first draft of the findings: they read "like a list of defects
rather than the list of features" — so they are recorded here as defects.

## What prompted it

Click 2 (wm-click-2b77ec0d, 2026-09-24) abstained. CLICK2-D Part 2 ran the
judge's own admission check offline: all four admitted targets declined
`:no-new-wanted-token`. Clicks 3–10 are held. The machine did not fail; it
ran out of pre-written cascades.

## Defects

Each defect names where it lives and the commit that introduced it. Commits
in this tree are authored as Joseph Corneli with an agent co-author line;
"no ruling found" means none has been located yet, not that none exists (I1
looks).

**D1. Proposing was deleted along with flat ranking.** `5d55e7a0`
(2026-09-17, "H5b ... flat decision path deleted (Joe 2026-09-17)").
Before it, the judge called `action-proposer/compose-proposers` over the
substrate proposers (mission, pattern, portfolio, and the bootstrap
proposer), ranked the proposals with `efe/rank-actions`, and selected. The
cited instruction was that a cascade is a policy and G is computed over
policies. The commit deleted the proposers' call site together with the flat
ranking, and nothing proposes cascades in their place. The proposer
namespaces still exist (`src/futon2/aif/action_proposer.clj`,
`portfolio_action_proposer.clj`); the judge no longer calls them (only a
comment at `scripts/futon2/report/war_machine.clj:7029` mentions them).

**D2. Feasibility is a gate after the fact, not a consideration in choosing
work.** The target list is broad — every substrate mission, plus declared
targets, plus the proposal supply, plus the ticket queue
(`war_machine.clj:7072–7080`) — but a target with no pre-written candidate is
refused. The machine cannot prefer feasible work; it can only drop
infeasible work from what it was handed.

**D3. Cascade construction is placed outside the tick by a docstring.**
`src/futon2/aif/cascade_proposals.clj` (`65e523ef`, 2026-09-21): "Never infer
applicability or token production from signature prose. An agent authors a
complete cascade-source-v1 declaration ... proposals never populate its
executable :candidates ... Retrieval is explicit, outside the tick." The
first sentence guards against a fake constructor (claiming a pattern
produces a token because its prose says so). The rest removes construction
from the machine altogether. No ruling found for the second part.

**D4. The in-machine constructor exists and is not wired.**
`src/futon2/aif/interpretation_construction.clj` (`4328238d`, 2026-09-21,
"Construct bounded candidate families from given interpretations") and
`construction_moves.clj` (`3b01790d`, 2026-09-17, the four
construction-library moves). Their only callers are each other, tests, and
the load-identity digest list. No click has run them on a real target.

**D5. The supply is five hand-written files, and it ran dry.**
`resources/wm/cascade-sources/*.edn`, written 2026-09-22/23. By 2026-09-24
two of the four admitted targets were already complete — one by the 09-23
click's own commit (T-repair-occ-444fb018), one by a reported rehearsal
(M-wm-08-external-f2). The machine's own success exhausts its work supply.
`data/wm-cascade-proposals/` holds one proposal.

**D6. The declarations do not aim at the remaining false wants.**
M-f11-find-production-successor: the false want `:hole/h2045faa0e7cc` is
produced by no declared pattern. M-aif-policy-conditioned-eig: the false want
`:hole/h42fceb4ad48b` is produced by the declared `:aif/two-layer-calibration`,
but the candidate using it was withdrawn in `8f97757b` (2026-09-22) because
the task needs held-out evidence that does not exist. A constructor would
have to find the route, or report what evidence would open it.

**D7. Running out of work produces a hold, not a proposal to learn.** The old
bootstrap proposer turned "no concrete actions for this class" into a
`:learn-action-class` recommendation (docstring cites Joe 2026-05-17). The
cascade path records an `:environmental-hold` finding and abstains. Nothing
proposes the work that would make the next cascade possible.

**D8. The run record does not say why it abstained.** The per-candidate
decline reasons are in the cohort's selection event and the scan markdown,
not in the tick run record (AR-16, as corrected after walkthrough 05).

## Investigations

| id | question | who | job | status |
|---|---|---|---|---|
| I1 | History: what did the pre-H5b proposers propose and on what grounds (feasibility among them)? Is any ruling recorded behind "construction is outside the tick" (D3)? | kimi-2 | invoke-1790256229096-23668-ed1a1a26 (park-08328d54) | running |
| I2 | Constructor: run `interpretation_construction` offline on the four admitted targets and the substrate missions; what does it build, where does it stop, and what would it need in order to run inside the judge (D4, D6)? | kimi-3 | invoke-1790256230680-23669-faccb791 (park-1ce12fa7) | running |

Both read-only: no clicks, no writes under `data/`, no shared-JVM loads.
