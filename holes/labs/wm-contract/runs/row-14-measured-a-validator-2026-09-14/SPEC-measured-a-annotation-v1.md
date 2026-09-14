# Measured-A categorical annotation validator v1

Scope: pure structural validation of an independently adjudicated annotation.
This does not mint labels, authenticate production authority, attach labels to
closes, count coverage, or construct an A matrix.  The accepted test record is
an explicitly synthetic mechanism fixture.

## Record and authority boundary

The candidate record has exactly these top-level fields:

`{:schema :annotation/id :subject :label :rubric :conditioning :evidence
:provenance :created-at}`.

`:subject` binds entity, run, cohort, attempt, closed checkpoint, Status,
state time, and model revision. `:rubric` binds rubric id/version and the
status-specific criterion id. `:conditioning` binds the post-action-at-close
point, transition/action ids and time, close disposition/time, and immutable
evidence cutoff. `:evidence` is a nonempty ordered vector of role, literal
bytes, recomputed SHA-256, observation time, one supported Status, and no
conflicts. `:provenance` records independent adjudication, source-record shape,
and contemporaneous or explicit retrospective provenance. Creation time is
separate from evidence time and independent review time.

Configured authority is a separate argument, never a candidate field. It has
exactly `{:schema :scope :rubric :observer :reviewer :acceptance}`. Its rubric
must cover the exact seven-state machine support. Observer origin, distinct
reviewer authorization, and acceptance artifact retain literal source bytes
whose SHA-256 is recomputed. Acceptance binds the fixed-order subject:
annotation, entity/run/cohort/attempt/checkpoint/state/time, rubric and
criterion, cutoff, and every evidence role/hash/time/support/conflict tuple.
No default authority exists.

Temporal law: `action-at <= state-at <= closed-at <= evidence-cutoff <=
annotation-created-at <= reviewed-at`. Retrospective records retain the same
frozen cutoff plus an explicit reason; they are not presented as contemporary.
Close disposition and model posterior may be retained context but may never be
the label source.

## Status rubric skeleton

The support is the production latent support from `futon2.aif.belief/status-set`:
`spawned, refined, strengthened, addressed, falsified, foreclosed, reopened`.
The following semantic boundaries are grounded in the existing vocabulary;
the operational evidence criteria are intentionally **OPEN for owner review**.

| Status | Semantic boundary | Operational content |
|---|---|---|
| spawned | entity newly enters the tracked status domain | OPEN |
| refined | entity content/specification becomes more precise | OPEN |
| strengthened | current live standing/support becomes stronger | OPEN |
| addressed | the tracked concern has been substantively addressed | OPEN |
| falsified | retained evidence defeats the tracked proposition | OPEN |
| foreclosed | the tracked possibility is closed and not live | OPEN |
| reopened | a formerly closed event becomes live again; distinct from merely having strong current standing | OPEN; must require both prior closure and renewed-live evidence |

Until an independently owned rubric fills these OPEN criteria, no production
annotation can qualify. A keyword choice, reused interest event, renamed
argmax, model posterior, or target close disposition is never qualifying
observation. Ambiguous, conflicting, or insufficient evidence refuses.

## Required controls

The fixture suite retains refusals for missing authority, borrowed acceptance,
candidate-forged identity, rewritten label under unchanged acceptance,
incomplete/mismatched rubric, ambiguous/conflicting/insufficient evidence,
posterior-derived labels, source-byte mutation, invalid temporal order, and
retrospective provenance omission. Fixture authority has `:scope
:isolated-test`; it establishes only the validator mechanism.
