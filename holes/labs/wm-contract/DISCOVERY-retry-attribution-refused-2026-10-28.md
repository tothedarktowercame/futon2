# Discovery: `retry-attribution-refused`, 2026-10-28

This is a read-only account of the two retained refusals.  No click, reload,
ledger write, or source change was made for this discovery.

## 1. The attribution boundary

The refusal is raised by `attributable-retry-readiness` at
`src/futon2/aif/meta_self_heal_production.clj:1394-1422`, before the retry is
allowed into the ordinary runner (`src/futon2/aif/full_loop_runtime.clj:803-816`).
The check loads and verifies the durable injury ledger, computes the effective
active injuries, and looks up both:

* the effective transition for the supplied `:injury-id`; and
* the last `:authored` transition with the same injury, repair, and attempt.

It returns `:ready` only when the ledger loaded, effective state computed, the
injury is currently `:enacted`, an authored transition exists, and all four
attribution values agree:

```clojure
[:injury-id :repair-id :retry-click] ; effective enacted entry = supplied retry
:patch-sha                         ; authored entry = supplied retry
```

The refusal at lines 1420-1422 reports the enacted/authored values as
`:expected` and the retry payload as `:supplied`.  This comparison is the
intended protection: an unrelated repository commit must not be allowed to
run as if it were the reviewed repair.

The server preserves the retry payload when it turns a queued retry into a
child (`futon3c/src/futon3c/wm/runner_service.clj:1315-1329`) and launches that
child only after the parent is durably closed (`runner_service.clj:1363-1373`).
Retry identities themselves include the supplied patch SHA
(`futon3c/src/futon3c/wm/meta_self_heal_adapters.clj:767-779`).

## 2. The two refusals

### 00:07Z retry

Evidence:

* acceptance:
  `futon3c/data/wm-click-acceptances/click-wm-retry-repair_2f6826382458c5b36338189a8623f41abb120f92-2-patch-87c10683a0788ae6c8c9df185fab0b9649586842.edn`
* binding:
  `futon3c/data/wm-click-run-bindings/click-run-binding-wm-retry-repair_2f6826382458c5b36338189a8623f41abb120f92-2-patch-87c10683a0788ae6c8c9df185fab0b9649586842.edn`
  (SHA-256 `af2ef6102d85ebcf82ac9e249bb352ad47fadde46527a768867a41ae3aa70286`)

The child supplied:

* injury `injury/2aef7e0c0f48a851a781b41642bf30e52e3fa246`;
* repair `repair/2f6826382458c5b36338189a8623f41abb120f92`;
* attempt `2`;
* retry id
  `wm-retry-repair/2f6826382458c5b36338189a8623f41abb120f92-2-patch-87c10683a0788ae6c8c9df185fab0b9649586842`;
* patch `87c10683a0788ae6c8c9df185fab0b9649586842`.

The verified ledger expected the same injury and repair, but its effective
entry was ordinal 70, whose retry id is
`wm-requeue-2-2-repair/2f6826382458c5b36338189a8623f41abb120f92-patch-87c10683a0788ae6c8c9df185fab0b9649586842`,
and the matching authored transition, ordinal 53, has patch
`02e9d5e7e11e92479d586ad078ce960902c36fa1`.  Thus both `:retry-click` and
`:patch-sha` failed.  The run binding retains that expected map; the supplied
map is independently fixed by the accepted child id/payload.  No run record
was produced.

### Repair iteration 1 child

Evidence:

* `/home/joe/notes/wm-next/criteria-completeness/repair-iteration-1/receipt.md`;
* `owned-click.json` in that directory (the bound child payload);
* `child-binding.edn` there, copied from
  `futon3c/data/wm-click-run-bindings/click-run-binding-wm-requeue-2-2-repair_2f6826382458c5b36338189a8623f41abb120f92-patch-87c10683a0788ae6c8c9df185fab0b9649586842.edn`
  (SHA-256 `691f367c992f70d3568ed8180826bf6c5d6dea8e942956ff1e0237d5919828bb`).

The server supplied injury `2aef7e0c...`, repair `2f682638...`, attempt 2,
the ordinal-70 requeue id above, and patch `87c10683...`.  The verified ledger
expected the same injury, repair, and retry id, but authored patch
`02e9d5e7...`.  Only the patch comparison failed.  The receipt's phrase
"patch `87c10683...`" describes the rejected child payload, not the enacted
repair patch.

The two refusals therefore have the same underlying bad patch attribution.
The first also used the superseded initial-retry id after ordinal 70 had
become effective; the later child used the effective retry id and exposed the
patch mismatch alone.

## 3. Cause and the other enacted injury

The refusals are correct.  Patch `87c10683...` was the then-current repository
head recorded by the serving runner; it is not the reviewed repair patch
`02e9d5e7...`.  Letting either child through would falsely attribute arbitrary
later repository state to repair `2f682638...`.

The producer defect is upstream in requeue construction.  In
`src/futon2/aif/meta_repair_coordinator.clj:342-355`, line 348 obtains the
patch for a pending requeue from `(receipt-for transaction :author)`.  The
transaction is shared/resumed state and is not the durable identity of the
injury being requeued.  It supplied `87c10683...` to `retry-click-id`, while
`ledger/append-requeue` correctly retained the repair's patch
`02e9d5e7...`.  The resulting ordinal-70 entry is internally contradictory:
its `:patch-sha` and `:integration-commit` are `02e9d5e7...`, but its
`:retry-click` suffix is `87c10683...`.

The second awaiting injury is not contradictory.  Ordinal 67 records:

* injury `injury/c5606345ac99815bbb5387aa0954822de8054319`;
* repair `repair/ad68c5681da28c85d5635d1a1446cf437c423833`, attempt 1;
* authored/enacted patch `6f1f8543cc4223e782bb8acdab3713eaef7daaef`;
* retry id
  `wm-requeue-1-1-repair/ad68c5681da28c85d5635d1a1446cf437c423833-patch-6f1f8543cc4223e782bb8acdab3713eaef7daaef`.

Its retry id and patch agree.  Neither of the two refusal records above is a
refusal of that c560 retry; both are children for injury 2aef.

## 4. Smallest repair and disposition

The smallest source repair is at
`src/futon2/aif/meta_repair_coordinator.clj:347-354`: derive the patch from
the verified authored transition for the exact `(injury-id, repair-id,
attempt)` being requeued (or from an enacted entry whose patch has first been
proved equal to that authored transition), then mint the retry id from that
value.  Do not use the transaction's generic latest author receipt.  Add a
regression in which another author receipt/current HEAD exists and prove the
requeued id still contains the target repair's authored SHA.  The existing
readiness comparison should remain unchanged.

That source correction alone does not make ordinal 70 truthful.  After the
producer is repaired and reviewed, injury 2aef needs an authority-backed
ledger transition that names a freshly minted, internally consistent retry;
the malformed ordinal-70 child must not be replayed or edited in place.
Injury c560 can retain its already consistent ordinal-67 retry identity, but
it should be issued only through the supported coordinator after the common
producer is repaired, so another transaction cannot contaminate it.

Excursion step 6 explicitly permits this disposition when self-heal refuses:
retain the exact boundary and repair externally under the excursion's
authorization, including the warranted algorithm change, without repeating
the unchanged failing action
(`holes/labs/wm-contract/E-wm-criteria-completeness.md:25`).  Step 7 then
requires the red evidence, discriminating replay, review, serving activation,
and recheck before resumption (`E-wm-criteria-completeness.md:26`).

The ledger inspected for this note was
`data/wm-meta-injuries-live-2026-10-22/ledger.edn`, SHA-256
`1ad7b9a09fb17ba0a55e4160781ce2b56a97004b938314633976dae38681b59d`.
