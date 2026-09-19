# Ruling: selection precedence and external repair (2026-09-19)

Joe, verbatim, on being asked whether clicks should always run cascade
selection with repairs handled by external lanes (Option A) or keep a
bounded stop-line diversion (Option B):

> "Option A was already declared by me in other terms. If the machine
> fails to self-heal using its standard methods, we will repair it from
> outside. I already allocated a budget of 5 runs, to my knowledge only
> one is used so the 2 would come from the existing budget."

## Operative consequences

1. An ordinary click always runs cascade selection. Open repair
   obligations no longer pre-empt the enacted entry; they are recorded in
   the run record as evidence (which open obligations existed at
   selection time), honoring both the guards ruling (nothing halts runs
   during tuning; evidence yes, vetoes no) and the run-certificate
   uniformity ruling (every run carries the same certificate burden —
   possible only when every run performs selection).
2. External repair is the standing mechanism when self-healing fails:
   agent lanes work the obligation queue through the store's own verbs
   (implementation with independent review, validation, resolution, or a
   proof-carrying dismissal), each repair earning a test-registry
   warrant. The 2026-09-19 drain (runner-eligible 20 -> 2) is the worked
   precedent.
3. Two machine clicks for production-shaped validation of the six
   findings at :awaiting-validation are authorized from the existing
   P-0 budget (AUTH-ordinary-click-budget-2026-09-19.md), leaving two.
   They run after the runner change lands, so they perform real
   selection.

Context: FINDING-repair-branch-bypass-2026-09-19.md (the unbounded
precedence, its provenance in cf7e5389/64561ef0, and the coded exit);
CLASSIFY-repair-queue-2026-09-19.md; the drain receipts of 2026-09-19.
