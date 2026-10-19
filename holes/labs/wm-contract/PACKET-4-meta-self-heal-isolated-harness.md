# Packet 4 — isolated META self-heal transaction harness

Date: 2026-10-19

This is an offline finite harness, not live serving integration.

## Rerun

```sh
tmp=$(mktemp -d)
clojure -M:test -m futon2.aif.meta-self-heal-harness run "$tmp"
```

The harness selects the pinned `bd1f106d...` construction failure from
`test/fixtures/meta-self-heal/campaign-2026-10-19.edn`, writes its recorded
shape to a temporary file, and ingests it through the locked durable injury
store.  Arbitration observes the derived active set and admits only the
dedicated coordinator.

## Offline transport evidence

- `fake/wm-author` initializes and modifies a throwaway Git repository and
  returns the SHA of a real clean commit.
- `fake/wm-reviewer` independently runs `git rev-parse` and `git show`, hashes
  the changed file, and seals its review against that exact commit.
- `fake/wm-repair-reviewer` is represented by the distinct work-order seat;
  the enact adapter verifies repository HEAD/cleanliness and seals the exact
  integration commit and retry-click identity.
- Retry produces a new sealed, repair/patch/click-attributed outcome record.

The successful trace is:

```text
campaign record -> open -> claimed -> authored -> reviewed -> enacted
                -> attributable retry -> discharged
```

The repair id and real Git patch SHA are invariant from their introduction to
discharge.  Arbitration reports ordinary work ineligible before repair and
eligible only after discharge.

## Restart and duplicate-effect control

Coordinator checkpoints are available after claim, author, review, and enact.
At each checkpoint the harness writes the sealed ledger and transaction as
one-form EDN, reloads the ledger from disk, and invokes a fresh JVM through
the `verify-checkpoint` CLI.  It then resumes the retained transaction.
Observed effect census is exactly one call each to author, review, enact, and
retry; no checkpoint causes duplicate dispatch.

## Failure matrix

| Injection | Result | Last durable state |
|---|---|---|
| malformed detection record | typed ingest refusal | no ledger created |
| reused claim session | preparation refusal | open |
| author dispatch/result failure | held | claimed |
| reviewer rejection | held | authored |
| reviewer patch substitution | typed refusal | authored |
| enact/reload failure | held | reviewed |
| retry failure | reopened | reopened |
| retry success without repair attribution | typed refusal | enacted |

Every case with an ingested injury remains exclusively routed to repair;
ordinary work eligibility stays false.  The harness and coordinator import no
ordinary loop-node, pattern fold, policy/G selector, family selector, or
D-task authority.

## Campaign comparison

The real pinned campaign record ends at one `:construction` /
`:fold-output-invalid` terminal failure and contains no repair id, patch,
review, enactment, or attributable retry.  The harness preserves that cause
shape but intentionally adds the six sealed ledger states.  It does not claim
byte/schema equality with the old run-record trace.

## Remaining gap

No actual Agency transport, serving arbitration, reload endpoint, production
ledger, or click is exercised.  Packet 5/live wiring must adapt these ports to
the serving process and persist each checkpoint atomically.  This harness
proves transaction mechanics and restart shape only.
