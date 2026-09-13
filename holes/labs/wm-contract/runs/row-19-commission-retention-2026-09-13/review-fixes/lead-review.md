# Independent re-review — codex-26, 2026-09-13

Reviewed futon3c `002c6d27..b12674f3`, futon2 receipt `5712367e`.
Disposition: R2/R3 repaired; R1 durability mechanism needs structural repair
before admission/live reload. No existing test was rerun.

The current HTTP source and focused runner hashes match the receipt:
`c737e5bb32a5470bd15f42ddda7f3db6077b29ad5d6c7a70643665895bdbdd8e`
and `7b835c2ddea540f85aaf7cd7bef721e3ccfc48c94e0bd3b70d15fabbd769864b`.
The focused runner now prints bound counters and exits nonzero on failures;
raw outputs show 8 passing assertions and the induced ninth assertion failing
with exit 1. The parens command now invokes the actual validator. These two
previous receipt defects are closed at the reviewed source pins.

## R1a: the archive is still inside the hot ledger

`:request-commission-archive` accumulates full commissions indefinitely in the
same map held by `!invoke-jobs-ledger`. Every update serializes/persists that
whole map. Moving records out of `:jobs` while retaining their prompts in a
sibling key does not bound the live ledger's memory or rewrite volume.
The archive must be a separately persisted immutable evidence store with
on-demand lookup, not an ever-growing field in the live ledger.

Preserve evidence durably before the hot record is discarded. Fail closed on
archive persistence failure, preserve hot evidence for retry, and commission
retry/idempotence plus >7-day readback after memory reset. Ensure expiry and
ordinary hot writes do not load or rewrite all historical commission bodies.
Retain the existing D13 hot-job and trace-index pruning rules.

## R1b: archived identity must participate in job-id uniqueness

`create-invoke-job!` currently considers only live `:jobs` when accepting a
requested job id. An expired id can therefore be reused despite an archive.
The read API then combines the new hot commission/digest with the old
archived `:job-join` and reports `:source :hot-ledger`. The archive-conflict
check only runs when that new job later expires, too late to protect the
current read.

Prevent archived-id reuse at creation, or define an explicit versioned
identity without conflating generations. A read must return commission and
job evidence from one consistent identity/generation. Commission reused-id
creation and hot/archive disagreement controls; never return a mixed pair.

The archived projection still needs separate trace-authority verification and
the external operator root for genesis, as the author correctly records.
No R9 anchor/admission has been authorized by this re-review.
