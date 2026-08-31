# C209 — Morning Brief unanswered-item disposition

Date: 2026-08-31

## Census

The immutable store contains 72 items, all schema 1, written from 2026-07-14
through 2026-07-27. Outcomes are:

| Outcome | Count |
|---|---:|
| `:grounded-change` | 21 |
| `:build-failed` | 29 |
| `:agent-unavailable` | 8 |
| `:incomplete` | 8 |
| `:substrate-unavailable` | 3 |
| `:agent-job-stalled` | 2 |
| `:agent-job-timeout` | 1 |

The achievement tiers recorded are 15 `:fully-grounded`, 14
`:partial-authored`, 24 `:none`, and 19 items predating the tier field.

The objective-based QA vocabulary and its `:substantive-achievement` belief
projection landed in `cf7e538` on 2026-07-16. Nineteen items (including six
grounded changes) predate it; 53 items postdate it. The later
`:feature-verdict` objective landed in `2439ddf` on 2026-07-18; 37 items
predate that addition and 35 postdate it.

Only 15 items carry the achievement entity id required by
`belief-event-for`. Those 15 are grounded, post-objective-vocabulary items.
The other six grounded changes predate the objective vocabulary and carry no
belief target. Therefore:

- 72 items have unanswered `:substantive-achievement` questions;
- 15 unanswered questions actually block an otherwise constructible
  independent belief event;
- 57 are QA/audit debt but cannot feed belief even if answered under today's
  implementation.

Four review records exist, all `:feature-verdict`; there has never been a
`:substantive-achievement` review or Morning Brief belief event.

## Retrospective evidence hazard

A review written today gets today's `:reviewed-at`. Its `:belief-event` has an
event id, entity id, type, weight, source, and objective, but no occurrence
time or reference to the historical attempt time. `unseen-belief-events`
offers it to the next update as newly unseen evidence. Thus a verdict about a
July attempt would enter belief as current evidence, not as dated historical
evidence. Retrospective QA is technically possible for the 15 targeted items,
but its temporal meaning is unsound unless the belief carrier first gains an
authored occurrence time and an explicit historical-evidence policy.

## Proposed categories

- `:historical-unanswered`: retained immutably, excluded from
  `DECISION-DUE`, and never synthesized into a review. Its missing independent
  evidence remains explicit.
- `:live-pending`: remains in `DECISION-DUE` until its applicable objectives
  are answered. A substantive answer feeds belief only when the item names an
  achievement entity.

Recommended boundary: classify all 72 existing items as
`:historical-unanswered`, and begin `:live-pending` with Joe's next explicitly
operator-triggered run. This is an observable epoch boundary rather than a
guessed age cutoff: the historical store ended on July 27, predates the current
v20 production-path baseline, and has no sound temporal carrier for delayed
belief evidence.

Alternatives for Joe:

1. Use the objective-vocabulary landing (`cf7e538`) as the boundary: 19
   historical, 53 live. This creates 53 decisions now; only 15 can feed belief,
   and those 15 would enter as falsely current evidence.
2. Select only the 15 target-bearing grounded items for retrospective QA and
   classify 57 historical. This recovers possible judgments but still requires
   a temporal-evidence policy before they may feed belief.

## Consequence of the recommended boundary

The machine permanently receives no independent substantive-achievement
evidence for the 21 historical grounded changes, including the 15 for which a
belief event could otherwise be constructed. No historical item or review is
deleted or rewritten; the absence remains part of the evidence record. In
exchange, Joe's next run begins with a truthful, bounded decision queue and no
July judgment is misrepresented as current evidence.

No disposition was applied in this delivery.
