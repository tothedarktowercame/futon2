# Row 22 E3: bounded pre-enact independence verifier

The pure boundary resolves three separately pinned one-form UTF-8 EDN records:
the pending construction, its R9 independence verdict, and the executed review.
The pending record fixes the exact model/run/cohort/tick/event and the complete
subject: occurrence id, action, construction, ordered field pins, producer,
claim, artifact, and producer trace. Verdict and review must reproduce that
subject and context exactly. Field pins are a nonempty ordered set of distinct
named SHA-256 sources; no caller-supplied one-label placeholder is sufficient
for production. The review embeds the complete input to the canonical
`r9-checker/check-independence`; E3 executes that checker and requires the
retained canonical admission to equal its result. Thus job identities, complete
commission digest, artifact, verification receipt, trace index, executed review,
chronology, and bootstrap anchor are checked by the existing authority rather
than replaced by new verdict booleans. The review identity must differ from the
producer and its completed time must strictly precede pending authorization.
Self, unknown, missing, stale, borrowed, malformed, and retroactive evidence
refuse.

This is isolated mechanism evidence. Fixture configuration and hashes are not
production authority. Production refuses until independently owned pending
event authority and serving R9 retention/genesis records exist. R6 scoring and
posterior proof remains external. The prospective consumer seam is immediately
after exact occurrence selection and construction, and before any `enact!`
call; this packet does not wire that seam or authorize an action.
