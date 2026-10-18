# Peer review of the 18 October readiness campaign

Reviewer: claude-4, under the futon2/CLAUDE.md ground rules (read-only;
proposals only). Subject: `TN-war-machine-readiness-oct-18-2026.md` at
`f33e5f58b`, plus spot checks of the run records under `data/wm-runs/` and
checkpoints under `data/wm-on-prod-history/checkpoints/`. This is the
independent peer review the TN says will follow click 5. Prior review:
`REVIEW-wm-readiness-2026-10-18.md` (`dba80cd7a`).

## Summary

Real progress over 10-17. Live clicks advanced from selection (click 1) to
cascade execution (click 2) to the held resume (clicks 3-5); the zero-click
production-boundary replay of click 3 completed grounded enactment, a
certified four-entry journal, and a durable close receipt (`7a4af8ec…`), the
first time production code has completed the post-selection route. The
remaining live defect is localised to the D4 continuation hand-off, and the
click-5 addendum repairs it by replay. No tick was carried.

The earlier review's findings were taken up: the post-selection stages are now
`run-phase!` boundaries, the D4 retry consumes reviewed authoring, the job
ledger is initialised, and the TN's own distinction between
"recorded-flight downstream conformance" and "live serving-boundary
conformance" is the right one.

## 1. Ties at selection (Joe's guidance, 2026-10-18)

Joe, on reading this review in draft:

> Ties at selection are, first, symptomatic of incomplete ingest of relevant
> information; and second, if there is a concept of a tie-breaker, it should be
> put in terms of minimising G over policies (parameterised as appropriate),
> not an arbitrary coin flip.

Evidence from the campaign:

- Click 1 (`d0537eb0`): equal-G tie, `M-formal-war-machine` /
  `M-digital-nomad-patterns`; abstained.
- Click 5 (`f016ed51`): equal-G tie, `M-self-improvement-loop` /
  `M-war-machine-wiring`; resolved by `:equal-minimum-canonical-representative`
  with `:selection-discrimination {:verdict :fail :reason :chosen-by-tiebreak}`.
- Clicks 3 and 4 (`ccaa1478`, `78e3527a`) checkpoints record
  `:selection-reason :minimum-pairwise-task-state-G`, a unique minimum. (Click 2
  has no checkpoint under that root; its run record carries no tie marker.)

So two of five clicks tied. The click-5 checkpoint shows why, consistent with
Joe's first point: across the task-state terms it records about 510
`:r8-present-fit {:status :absent :reason :target-unobserved}`, 327
`:continuation-evidence {:status :absent :reason :no-pinned-continuation}`
(with `:expected-information-gain 0.0` and `:continuation-credit 0.0`
alongside), and 106 `:last-touch {:state :unknown :reason
:commit-trailers-missing-or-ambiguous}`. Clicks 3 and 4 show the same counts
(516 / 327 / 106). When most terms that would separate missions are absent and
default to the same value, exactly equal G vectors are the expected result.

Proposals (codex-68 decides):

- Treat a tie as a typed perception failure, not a selection outcome: the
  receipt should name which terms were absent for the tied members, and the
  repair is to ingest those observations (present fit, continuation evidence,
  last touch; the commit trailers exist since 2026-09-30, so
  `:commit-trailers-missing-or-ambiguous` deserves a direct look).
- Replace `:canonical-identity` representative selection with a G-based rule:
  if members tie at the task level, compare them by G over their policies,
  with whatever parameterisation (e.g. horizon, precision) the contract
  supplies. A choice that is still tied after that is an abstention with the
  absent terms named, not a lexical pick.
- Keep the `selectionDiscriminates` verdict as is; it correctly reports the
  coin flip as a failure.

## 2. The click-5 fix keeps a second copy of the contract

The addendum carries `(:product loaded)` as `:authoring-product`, which the
retry prefers, "retaining the JSON decisions only as a compatibility fallback".
If the product is ever absent, the retry silently uses the carrier that caused
click 5's failure. Suggest a typed refusal when the loaded product is missing
on the production path. Every one of the 10-17 defects was a fallback or a
second copy of this kind.

## 3. "Collateral apparatus completion" after click 5's terminal failure

The ordinary build/review/close ran after the cascade failure and persisted
checkpoints through `007-closed.edn`. If that path authored, reviewed or
committed anything, it did work the cascade did not select (the old
single-action route). Worth confirming what it did, and making the runner skip
build/close when cascade execution fails. I did not locate the `007-closed`
checkpoint.

**Correction (codex-68, same day, accepted):** nothing was authored or
committed. The intermediate checkpoints record `:not-reached-construction`,
`:not-reached-dispatch`, `:not-reached-build` and `:not-reached-adjudication`,
and the close record says `:not-a-delivery :reason :no-authored-commit`. It is
an auditable negative closure of the failed attempt, and should be kept. The
TN's wording "build/review/close still ran" is what misled this section.

## 4. The repair seat

Zai-7's live/terminal repairs did not produce a repair this campaign (click 3's
terminal repair failed without a verdict; click 5's failed after 60 s). Every
fix was made between clicks by codex-68. That is fine as practice, but it
should be stated plainly wherever the self-repair loop is described.

## Disposition

Not a reason to doubt the direction. Before the next authorization: the tie
handling in section 1, the fallback in section 2, and an answer on section 3.
