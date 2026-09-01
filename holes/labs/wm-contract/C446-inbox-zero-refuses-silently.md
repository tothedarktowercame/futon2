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

---

## Correction, same day, before dispatching the repair

I checked the service journal before writing a packet, and **the claim above
that "the acting half has produced nothing since 08-26" is wrong.** The
escalation ledger and the missing proposals file are real; the inference I drew
from them was not. The sweeper prints every hundredth refusal and prints every
sweep, and the journal has 1,967 `inbox-zero` lines.

**39 sweeps in the last 36 hours**, on schedule, roughly every 30 minutes.

What the journal actually shows, and it is worse than a stalled feature:

**1. The sweep loop stopped at 2026-09-01 02:03:57 and has not run since.** At
the time of writing it is 07:22 — **5 h 18 m against a 30-minute interval**. The
JVM is alive and logging other subsystems in the same second. Nothing anywhere
reports the stop. `README-inbox-zero.md` predicted this exact failure: *"Nothing
notices when it stops."*

**2. It was never draining.** Every sweep before the stop reported
`attribution sweep left ~1250 path(s) unswept (max-paths=25)`. A cap of 25 paths
per 30 minutes against a backlog of 1,250 is 25 hours of sweeping if nothing new
arrives, and the number held between 1,244 and 1,254 all evening — so it was
not making progress. It fell to 14 unswept at 02:03, the last sweep, which
matches the tree-cleaning commits I made last night rather than any sweeper
progress.

**3. It has still never recorded a proposal.** `attribution-proposals.edn` does
not exist, so of the 25 paths selected per sweep, none reached `:propose` with a
live candidate seat.

**4. The refusal flood suppression is deliberate and documented**, not an
oversight: `turn_promotion.clj:80-95` records having measured *"3,263 such plans
over 30 hours from six sessionless seats"*. It ledgers once per (agent, reason)
per process and prints every hundredth. My criticism of that design was
misplaced. The count exists — as `:occurrence n` in an in-process atom, printed
to the journal. What it is not is durable across restarts or queryable.

**5. One root cause is visible across both halves.** Promotion refuses with
`:no-session-id`, and 2026-08-31 23:31:35 shows `tool-edit witness failed for
claude-2: Session-seat identity fields must be non-blank strings`. Both halves
fail on seats presenting a blank session id.

## What this changes about the repair

The first packet is **discovery, not implementation**: find out why the loop
stopped. That is live, it is the acute fault, and nothing else can be evaluated
while the loop is not running. Making refusals durable and queryable remains
right, but it is now the second question, not the first — and the reason to keep
it is sharper: the count that would have shown this is in an atom that dies with
the process.
