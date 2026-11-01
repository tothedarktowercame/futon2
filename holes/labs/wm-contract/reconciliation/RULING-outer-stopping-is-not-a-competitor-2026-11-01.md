# Ruling: on the outer loop, stopping is not a competitor; almost every open task is worth doing and the question is order (Joe, 2026-11-01 ~20:40Z)

Context. Clicks 12–16 refused `:pick-not-below-abstention` on an unchanged field: the best
available class (M-interim-director-proxy-metric-inventory, landing related/one) scored G 3.3896
against the abstention's 3.2782, the gap being an ambiguity charge on the readiness-prior
mixture. The loop paused by repair 9. I put the question: sanctioned outer stop, or model fault?

Joe, verbatim:

> "on the first question for me, the machine is behaving like Barnaby the Scrivener or Marvin
> the depressed robot. if it can literally find nothing better to do we need to teach it to be
> more helpful and more curious. No Ticket? No Excursion? No mission? No open question? No use of
> m40 memory? no code as a crime scene investigations or refactoring? No sense of what the Giraffe
> priorities say? Because the latter should put a priority order on a massive backlog of work.
> Surely you could find a top ten using these resources. We should perhaps spatialize the
> machine's ratings like we did with the old EFE field, but now using more up to date rankings.
> worth doing should be almost all open tasks, the question is order. Barring anything wlse it
> could be given goals related to its own self-improvement and capability growth!"

What this rules, in the terms of the Lean model and the code:

1. **Abstention is not a competitor on the selection kind.** On a non-empty field the outer
   selection picks the strict G minimum among the compared (admissible) policies. The
   abstention's G is still computed and recorded as information, never as a bar the pick must
   clear. `abstainEmptyField` remains the only outer abstention (Lean already says so; Joe's
   earlier ruling: outer abstention only when nothing is left). `abstainOnTask` stays a work-run
   case. The runtime refusal `:pick-not-below-abstention` is an inner (work) refusal only.
2. **A class that predicts exactly stopping is still an order.** `predictsOtherThanStopping`
   is a work-run requirement; on the outer the stopping-equivalent classes (L5-11) are compared
   like any other and the fact that a class predicts stopping is retained as a diagnostic, not a
   stop. When every class predicts stopping on a non-empty field the machine still picks the
   minimum and the record says so: that is the signal for the curiosity programme, not a reason
   to stop.
3. **Ordering is the product.** Giraffe priorities, tickets, excursions, missions, open
   questions, m40 memory, code investigation and refactoring, and the machine's own
   self-improvement are the sources; the outer loop's job is a top-N ordering over them, and a
   spatialised rating view of the field (the old EFE field, with today's rankings) is wanted.
4. **Fallback goals.** If the field were ever truly empty, the machine is to be given goals of
   its own self-improvement and capability growth rather than stop.

Items 1–2 are specified in packet L5-16-L (Lean + outer selector + projector). Items 3–4 start
with discovery packet L5-17-D.

## Refinement (Joe, 2026-11-01 ~20:55Z)

> "So, again, outer stopping should be a competitor, just a provably bad one unless I say 'let's
> pause' or we literally run out pf worthwhile work to do. So I think we should definitely keep it
> in as an option, but just expect that the machine runs at 10,000 feet or so above stopping level"

This supersedes item 1 above as written. Stopping remains in the outer comparison and Lean's
Q11 keeps `chosen.2 < abstentionG` for both kinds (codex-5's Lean half `2c8877d31` is **not
merged**). What changes is the abstention's G on the outer: it carries a ruled **altitude**, a
constant in nats added to its risk, large enough that a certain stop loses to any policy that
predicts progress under any plausible learned C. Lowering the altitude to zero is "let's pause":
the operator's model-level pause control, beside the loop's stop file. An empty field still
abstains (`abstainEmptyField`). Items 2–4 stand.
