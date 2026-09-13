# E6b completeness authority and chronology audit

## Independent correction verdict

`14999ca7` is accepted narrowly in the separately committed
`codex23-correction-review.md`. Subject capture store/generation are now joined
to actual capture, inventory, census, and boundary values. Corrected pins and
the retained 10-test/51-assertion gates match; they were not rerun.

## Confirmed join-law defect

The validator orders execution finish before `reviewed-at` and nevertheless
claims the execution retained the review artifact digest. The unmodified
positive fixture has execution finish `01:06`, artifact review `01:07`, and is
accepted. A terminal execution cannot retain an artifact created afterward.
The law must instead require:

`execution.started-at <= review.reviewed-at <= artifact-retained-at
 <= execution.finished-at <= acceptance.accepted-at`.

This is a validator defect, not an external trust premise.

## Missing independently retained origin

A coherent rewrite of job ID, trace ID, review artifact, execution, and
acceptance to nonexistent labels is accepted. The current nine roles merely
agree on strings. None resolves an independently retained Agency/host terminal
job record showing that this reviewer, commission, subject, and artifact were
the actual inputs/output of that job.

This is the smallest missing record/pin: add an externally configured
`:review-origin` role with exact bytes and schema binding authority root and
scope, origin kind/id and trusted-host provenance review, reviewer ID,
commission ID/digest, job and trace IDs, exact subject raw SHA, retained review
artifact raw SHA, artifact-retained-at, terminal status, and finished-at. Its
resolver origin must be configured outside the candidate. The execution,
review, acceptance, and chronology must join it exactly.

The corresponding bounded repair should add this tenth role and correct the
chronology. Controls: review after terminal finish; fake/missing origin;
artifact retained after finish; borrowed commission/subject/artifact; cross-
scope origin; and origin reviewer/job/trace disagreement. Production remains
unconditionally unavailable.

## Deliberately external premises

Writer inventory, acquisition boundary, closed intake, lifecycle reconciliation,
and store ownership are synthetic externally configured inputs in the current
fixture. The validator can check their joins but cannot establish their real
origin. That is deliberate and honestly labelled, not a newly discovered
internal join bug. Real acquisition and independent origin records remain
absent.
