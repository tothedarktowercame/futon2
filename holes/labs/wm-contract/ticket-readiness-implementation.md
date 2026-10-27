# Ticket readiness implementation boundary

This branch implements a generic, durable assessment carrier. Readiness is a
computed assessment and is not a work-pool lifecycle state. Canonical WM ticket
inventory remains the only population authority.

One runner phase may call `ticket-readiness/refresh-one!` before selection.
It contacts at most one fair candidate, retains the assessment and source pin
atomically, and exposes only validated `:suitable` rows to the existing
pipeline membership and scoring path. Preview does not execute the runner and
therefore cannot publish.

Contract preparation and implementation are different increments. A C3 path
existing is not qualification: the contract must contain bound positive and
negative cases and a separately pinned, independently authored accepted review.
Implementation additionally requires an executed, exact-revision, contract-
matched semantic-red record. Generic C3 and C8 are unchanged.

Explicit operator, ownership, inactive, preparation-dependency, source-drift,
root-dedup, and relevant implementation dependency outcomes remain typed.
Completing an increment while the ticket remains open returns `:completed`
with `:next-disposition :assess-remaining-increments`; it does not infer whole
ticket completion.

## Deliberate remaining composition seam

The production composition must still supply `:ticket-readiness-refresh` with
the canonical inventory reader, `prepare-proposal!` wrapper, designated answer
port, durable manifest path, and response validator. This branch does not pick
a designated seat or infer Giraffe dependency completeness. Consequently the
code is not activation-ready and B1 is not closed. Independent review must
either approve that exact composition or require a follow-up; there is no
permissive default and no hardcoded H1 route.
