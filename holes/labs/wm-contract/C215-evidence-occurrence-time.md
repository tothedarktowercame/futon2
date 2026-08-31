# C215 — evidence occurrence time scope

Date: 2026-08-31  
Owner: `wm-verbs`  
Disposition: confirmed schema-and-uptake gap; no ageing policy implemented

## Result

The unsoundness is broader than retrospective Morning Brief QA.  The live
belief-update carrier has no operational distinction between when evidence
occurred and when it was recorded or consumed.

Morning Brief makes the loss concrete:

- `morning_brief.clj:137-145` projects `substantive-achievement` into an event
  carrying id, entity, type, weight, source and objective, but no occurrence
  time.
- `morning_brief.clj:173-183` records only `:reviewed-at`, the time Joe writes
  the review.  It does not copy an occurrence time from the attempt item into
  the belief event.
- `morning_brief.clj:242-251` returns every unseen event without a freshness
  classification.
- `war_machine.clj:204-225` applies every in-domain unseen event and marks it
  consumed.  There is no time term or stale arm.
- `belief.clj:364-413` documents optional event `:timestamp`, but
  `update-entity-belief` destructures only `:type` and `:weight`; the timestamp
  has no effect on classification or weighting.
- `belief.clj:417-440` reduces the events directly.  It has no occurrence-time
  contract, recording-time contract, watermark, or historical-evidence policy.

Trace schema v20 does not repair this.  `trace.clj:431` records the tick's
current `:timestamp`; it is not the occurrence time of each input event.  The
trace can preserve a late event's application, but cannot say that the event
was late.

Therefore delayed QA, replayed evidence and backfilled observations are all
eligible to enter the next belief update as full-weight unseen evidence.  The
current update is order-insensitive, so this is not an ordering bug; it is an
unrepresented temporal fact and an absent policy for interpreting it.

## Proposed carrier (not implemented)

Every belief event should carry these two typed fields:

```clojure
{:evidence/occurred-at
 {:status :present :value "<ISO-8601>"}
 ;; Historical records that predate the field:
 ;; {:status :absent :reason :predates-field}

 :evidence/recorded-at
 {:status :present :value "<ISO-8601>"}}
```

Missing occurrence time must not default to recording time.  For records that
predate the field, `:predates-field` reuses the trace compatibility vocabulary.
A current-contract event missing the field should be `:malformed`, following
the precision and trace-version pattern.

Adding the carrier is not enough.  Uptake also needs a separately authorised
historical-evidence policy: refuse, hold for operator disposition, decay by a
declared function, or accept at full weight.  This pass does not choose among
those behaviours because each changes belief arithmetic.

## Required falsifier

Construct two otherwise identical events:

1. occurrence and recording times are current;
2. occurrence time is older than recording time.

The second must not reach `update-entity-belief` as indistinguishable current
evidence.  A check passes only if uptake yields a distinct typed result such as
`:historical-held` (or another explicitly authorised policy result).  Merely
storing the old timestamp while applying the same full-weight update fails the
control.

## Scope command

```sh
rg -n ':timestamp|:occurred-at|:recorded-at|:reviewed-at|:queued-at|substantive-achievement|unseen-belief-events|update-belief-batch' \
  src/futon2/aif/morning_brief.clj src/futon2/aif/belief.clj \
  src/futon2/aif/trace.clj scripts/futon2/report/war_machine.clj
```

This establishes the carrier and uptake scope.  It does not claim that every
producer of generic belief events is persisted or backfillable; it establishes
that none can currently communicate staleness to the updater in a way the
updater uses.

## Non-actions

- No historical Morning Brief item was reviewed or dispositioned.
- No occurrence time was inferred from attempt, queue, review, file or tick
  timestamps.
- No missing time was coerced to recording time.
- No belief weight, scoring or selection behaviour changed.
