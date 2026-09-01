# C446 — inbox zero is running, refusing, and not reporting the refusal

**Date:** 2026-09-01. **Owner:** `claude-20`. Prompted by Joe during the O24 sitting:
*"if the Inbox Zero service is not working, and we're building up a big backlog of
uncommitted files, that's complicated and annoying and difficult to work with."*

I expected to find the feature disabled. It is not.

## What is actually running

Read from the serving JVM's own process environment (pid 3191708,
`futon3c-zone.service`):

```
FUTON3C_INBOX_ZERO_ENABLED=true
FUTON3C_INBOX_ZERO_SWEEPER=true
FUTON3C_INBOX_ZERO_PROMOTION=execute
FUTON3C_INBOX_ZERO_SWEEPER_INTERVAL_MS=1800000     # 30 min
FUTON3C_INBOX_ZERO_STATE_PATH=/home/joe/code/storage/inbox-zero/state.edn
```

`bootstrap.clj:553` defaults `FUTON3C_INBOX_ZERO_ENABLED` to false. It is
explicitly true here. Promotion is in `execute` mode, which is the mode that
actually commits. **This is not a deployment gap.**

## The observing half is alive; the acting half has produced nothing since 08-26

| Artifact | Last written | Reading |
|---|---|---|
| `state.edn` (125.7 MB) | **2026-09-01 07:13** | the watcher is recording edits now |
| `witnesses/` | 2026-09-01 02:47 | witness capture is live |
| `escalation-ledger.edn` | **2026-08-26 09:08** | nothing escalated in six days |
| `attribution-proposals.edn` | **does not exist** | the sweeper has never recorded a proposal here |

The escalation ledger's final three entries are the same refusal:

```clojure
{:record/type :inbox-zero/refusal
 :refusal/reason :no-session-id
 :refusal/agent-id "f41-guide"
 :refusal/first-at #inst "2026-08-26T09:08:28.541-00:00"
 :refusal/note "no plan computed; subsequent identical refusals are counted in-process only"}
```

## The defect

**Promotion refuses because it cannot resolve a session id, and the ledger
records only the first instance of each refusal.** Repeats are counted in
process memory and written nowhere. So the ledger going quiet on 08-26 is not
evidence that refusals stopped — it is evidence that they stopped being *new*.

There is no endpoint to ask. `GET /api/alpha/health`, `/daemons` and
`/followups` all answer `Unknown endpoint` on the serving JVM, so the sweeper's
liveness cannot be queried from outside the process at all.

**From the outside, a sweeper that is working and a sweeper that has refused
every path for six days look identical.** That is the shape this campaign has
been finding all week — a check that cannot fail, an absence coerced into
success — and `futon0/README-inbox-zero.md` had already named it in this exact
context: *"a clean tree and an unswept tree look identical from inside, and a
sweep that never runs reports nothing rather than reporting failure."*

## Current dirt (corrects an earlier count)

I had been saying "futon3c 6 files, futon3 1". Measured now:

- **futon3** — 3 paths: `resources/sigils/patterns-index.tsv` modified, two
  untracked `library/math-formalization/*.flexiarg`.
- **futon3c** — 10 paths: four modified `emacs/*.el`, six untracked
  `holes/labs/M-apm-demonstration/pattern-library-*-scribe-*.md`.
- futon2, p4ng, mathlib4 clean.

The six APM scribe files are the population the sweeper is designed for and did
not act on.

## What the repair is, and what it is not

The repair is **not** "make it commit everything". The sweeper is deliberately
built never to mint a claim it cannot attribute, and that is correct.

The first repair is to **make the refusal visible and countable** — every
refusal, not every first refusal, with a queryable surface. Until that exists,
any fix to the `:no-session-id` path cannot be told from no fix at all, because
the only signal available is a ledger that stops writing when the failure
becomes routine.

Second, and only then: resolve why promotion cannot obtain a session id for
these seats.

## One thing worth noting in passing

`state.edn` is 125.7 MB of EDN, rewritten as a whole map. That is the same shape
as register decision O13 (invoke-jobs ledger, 134.6 MB, full-map rewrite per
mutation). Two facilities have independently arrived at the same storage
ceiling; whatever is decided for O13 probably applies here.
