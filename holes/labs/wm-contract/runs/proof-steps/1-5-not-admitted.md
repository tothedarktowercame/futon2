# ⟨1⟩5 diagnosis: why the front ticket entry was NOT admitted

zai-1, 2026-09-22. Read-only: the run record
`data/wm-runs/tick-run-record-2026-09-22-1790110142.edn`,
`ticket_queue.clj:75-115`, `policy.clj:355-374`,
`cascade_proposals.clj:130-175` (record-supply),
`repair_proposals.clj:13/:70-82` (target-id, supply), and the tick's
wiring in `war_machine.clj` :6571-6585.

## What "admitted" means for the tick

The tick calls `policy/select-action-cascades` with `:ticket-queue`
(war_machine.clj:6425), which computes
`queue-plan (ticket-queue/plan ticket-queue candidates ticket-queue-refusals)`
(policy.clj:358-359). A front entry is **:admitted** only when
`supported?` holds for at least one of the tick's **candidates**
(ticket_queue.clj:95-96: `admitted` = the targets of the ranked entries that
pass `supported?` — non-empty precedence, not `:zero-support`, finite :g).
`:not-admitted` is decided by the TARGET'S ABSENCE from that set, with the
refusals carried from the tick's assembly.

## The specific condition the reference ticket failed, from the record

The run record's `:ticket-queue :entries` carries the answer verbatim:

```
{:ticket "T-repair-occ-444fb018…" :status :not-admitted
 :refusals [{:target "T-repair-occ-444fb018…"
             :kind :universe-not-admitted
             :reason :repair-closure-observation-unavailable
             :missing :locators}
            {:target "T-repair-occ-444fb018…"
             :stage :proposal-supply
             :reason :repair-closure-observation-unavailable
             :missing-evidence [:produced-resolution-evidence] …}]}
```

So the ticket was **removed from the tick's candidate family BEFORE
selection** — the ranked `candidates` handed to `ticket-queue/plan` contained
no entry for the ticket's target, hence no `supported?` check ever ran for
it (the recorded refusals are the assembly's, not support failures; note
there is NO `:no-policy-support` and NO `:not-in-admitted-candidates` in the
record — both default kinds were overridden by the assembly refusals).

The removal is `cascade-proposals/record-supply`
(`cascade_proposals.clj:136-141`): `repair-targets` = the targets of the
repair scan's **open finding ids** plus repair-origin proposals;
`withheld` = the assembled problems whose target is in that set; those are
removed from `:problems` and recorded as the two refusals above. The
ticket's own finding — `repair-occ-444fb018…` — IS in the run's
`:repair-scan :open-finding-ids` (verified in the record), and
`repair_proposals/target-id` (:13) maps finding-id → `"T-" + id`, which is
exactly the ticket's target string. **The ticket was withheld because its
own finding is open — which is necessarily true until the repair it fronts
is discharged.** The withhold guard exists to stop UNRESOLVED repair
obligations from being selected as ordinary work; its condition
("repair-closure observation unavailable") cannot be satisfied by a ticket
whose finding is open by definition.

## Why my pre-flight saw both candidates admitted — the divergence

The two probes call DIFFERENT code paths around the same loader:

- **My pre-flight** called `cs/load-declared` → `cp/assemble` directly
  (proof-eval): the declaration loader and admission gate — the ticket's
  source loads, its facts observe, both candidates construct. It never
  consulted the repair store.
- **The tick** (war_machine.clj :6571+) additionally runs
  `cascade-proposals/load-supply` (repair-root scan) → `record-supply`,
  which REMOVES repair-target problems from the assembly BEFORE ranking —
  and `policy.clj:358`'s `ticket-queue/plan` sees only what survives.

So the divergence is exactly: **pre-flight = loader+assemble; tick =
loader+assemble+supply-withhold.** The 1-5 pre-flight was accurate about the
loader and wrong about the tick for one reason: it never exercised the
repair-scan withhold. This is also why click 3 (the pre-proof STAGES note)
said the ticket "has no cascade source, so the next selection will record it
as not admitted and fall through" — the same withhold, from the same cause,
observed then and not connected at pre-flight time. That miss is mine.

## One-line answer

**The entry was not admitted because the tick's proposal-supply step
withheld the ticket's target as an open repair obligation
(`:repair-closure-observation-unavailable`, cascade_proposals.clj:136-141) —
a condition that is necessarily true for any finding-ticket until its repair
closes, and which no admission check downstream can overcome because the
target never reaches the ranked candidates.**
