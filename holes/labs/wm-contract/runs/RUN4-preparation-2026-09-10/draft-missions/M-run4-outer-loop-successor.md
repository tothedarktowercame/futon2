# Mission: RUN4 outer-loop successor fixture experiment

**Status:** DRAFT — NON-LIVE; independent review and explicit activation required
**Owner:** unassigned Codex/Zai implementation worker; independent reviewer required
**Primary repository:** futon2
**Authority:** RUN4 `SERIES.edn` trial `:outer-loop-aif-replacement` and
`TRIAL-PACKETS.md` Trial 4

## Problem

The existing War Machine build loop has eight observable responsibilities, but
the proposed AIF successor has not yet demonstrated one complete decision →
execution → outcome → next-use cycle.  The trial requires that demonstration
on frozen isolated fixtures before any live-ledger, scheduler, or cutover claim.

This is an ordinary bounded development mission: it asks for reviewed code and
tests in futon2, has a repository and a finite milestone, and may legitimately
remain open across multiple implementation steps.  It is not a new action
class, a live mission activation, or a claim that its model choices are settled.

## First milestone

Build one isolated fixture-scoped outer-loop experiment satisfying Trial 4's
product and preserved-behavior checks:

1. Validate the frozen ledger-shaped fixture before and after work/review.
2. Produce the eligible candidate set under the recorded compatibility rules.
3. Record an AIF selection that actually consumes explicit preference and
   prediction inputs.
4. Hand the selected fixture row through the existing author/reviewer-separated
   inner-loop boundary without touching the live worklist.
5. Record the scripted outcome and reviewed result without coercing missing
   evidence.
6. Persist a Beta update in the isolated store and demonstrate consumption by
   a second selection, with a negative control that detects disconnection.

The milestone does not replace the complete outer loop and does not activate a
scheduler.

## Decisions that remain open

These are mission work, not defaults.  Each must be explicitly specified,
reviewed, and frozen before the experiment can execute:

- [ ] Define the selection equation and its exact relationship to the legacy
      deterministic priority order.
- [ ] Define the outcome-to-Beta-update mapping and the class being updated.
- [ ] Define the learned-state persistence/consumption witness and its negative
      control.
- [ ] Freeze the isolated ledger and outcome fixture bytes and their digests.

No numerical prediction values or probability defaults are supplied by this
draft.

## Preserved boundaries

- Never invoke `wm-build-loop.sh`; its notifier violates the current no-Claude
  constraint.
- Never read or edit the live topology worklist, mission registry, frontier,
  scheduler state, run locks, or production data.
- Preserve invalid-ledger refusal, dependency blocking, no-self-review,
  registry publish hold, no duplicate dispatch, typed unknown outcomes, and
  the exclusions for `:J`, Joe-owned, and `:loop-skip` rows.
- FUNDAMENTALS exclusivity is preserved unless a separately reviewed experiment
  records the old result and explicitly proposes a changed rule.
- Selection differing from the legacy priority is an observation, not by
  itself evidence of improvement.

## Acceptance for the first milestone

- The exact Trial 4 fixture, semantic-contract, outcome, and negative-control
  pins are recorded and fresh.
- Focused tests exercise one full cycle and a second selection over isolated
  stores only.
- The AIF record proves which preference, prediction, and learned-state inputs
  were consumed.
- Author and reviewer are distinct; normal build, review, grounding, and
  delivery checks are retained.
- The report separates task outcome, wiring conformance, and recording
  completeness, and explicitly states that no live execution or cutover
  occurred.

## Activation condition

This file deliberately lives below `holes/labs/.../draft-missions/`, outside
the mission registry's `*/holes/missions/M-*.md` discovery roots.  Moving an
independently reviewed revision into a primary `holes/missions/` directory and
changing its leading status to a live lifecycle state are separate operator
actions.  This draft authorizes neither.
