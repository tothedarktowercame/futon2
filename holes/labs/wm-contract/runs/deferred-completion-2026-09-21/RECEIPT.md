# Dormant deferred completion: rename and disposition

New runner route, dispatch markers, phase reasons, helper names and emitted
refusal kinds use deferred-completion terminology. Selection remains bound to
the exact selected `:incomplete-recoverable` finding; ordinary selection cannot
pick up an ambient job. The existing class name is deliberately retained as
required by the ruling. Legacy `:recovery` claims are accepted only at the
compatibility boundary, normalized on new writes, and categorically refused
along with the new spelling. No admission rule changed. The malformed/missing
artifact protections introduced in 562286d1 remain in place.

The capability is dormant. It preserves a completed job named by a finding
when a runner was lost mid-wait; it does not automatically discover orphan jobs
or constitute a general crash/restart protocol. README documents these limits.

## Disposition enumeration

Read-only census at 2026-09-21T05:11:07.876901306Z, using the real
`repair-obligation/open-obligations` against its production default root:
42 outstanding findings (28 machine-failure, 11 environmental-hold,
3 independent-review-failure); **zero incomplete-recoverable findings** and
therefore **zero referenced jobs awaiting deferred completion**.

The all-findings cross-check found one historical member:

| Finding | Job | Typed disposition | Evidence |
|---|---|---|---|
| repair-attempt-006-late-author-artifact | invoke-1784158547590-592-baa93319 | :retired / :already-resolved | Resolution dated 2026-07-16T09:08:32.774857211Z, successor attempt-007, artifact a707b6fff76da6ca55327f91324a10b79d96231a; live Agency GET now 404 invoke-job-not-found |

Retired here means excluded from this mechanism's eligible set, not a new
WONTFIX or store-status rewrite. Existing effective store status is :resolved.
Raw census, historical finding/resolution and API read are retained beside
DISPOSITION.edn. No findings, jobs or artifacts were cancelled, superseded,
dismissed, mutated or discarded. No broader orphan-job census is claimed.

## Validation

The first test executions used the isolated checkout based on 38b4c95c.
Authority and historical-verification passed; runner and selection suites
refused with :stale-runner-source because the source-coherence guard compares
against the canonical checkout. These failed executions are retained. The
guard was not bypassed or stubbed. The rename was then applied to canonical
files while preserving another lane's discharge work, and the four namespaces
were executed there. Only this lane's patch is staged; the other lane's edits
remain unstaged. Canonical logs carry the final results. No serving reload or
click occurred. All executions have retained logs and exit codes.
The authority regression checks both legacy/new refusal spelling and canonical
new serialization; runner regressions retain malformed-artifact refusal and
assert the new dispatch labels; selection-always and historical-verification
suites check that ordinary execution remains separate.

### Final canonical executions

| Namespace | Tests | Assertions | Exit |
|---|---:|---:|---:|
| full-loop-runner | 180 | 1005 | 0 |
| d-predecessor-task-authority | 8 | 43 | 0 |
| selection-always | 4 | 31 | 0 |
| historical-repair-revalidation | 2 | 19 | 0 |

Canonical clj-kondo: 0 errors / 0 warnings. Check-parens: OK.
The earlier pieces 1–2 warrant checkout has been restored to clean 385f54dc.
Validation here used the canonical working tree including concurrent, unstaged
discharge changes; this commit stages only the rename against HEAD and its
receipts, and does not claim to certify or own the concurrent discharge work.
