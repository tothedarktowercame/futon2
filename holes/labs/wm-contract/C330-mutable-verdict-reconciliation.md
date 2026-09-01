# C330 — reconciliation of the named mutable-verdict population

Date: 2026-09-01

The complete `:mutable-verdict-population/v1` population was reconciled by
name. Its 68 members split as **61 content-shaped, 6 event-shaped, and 1
neither**. There are no unexplained members.

## Batch results

The audit proceeded in three lexical batches so an event-shaped member could
be surfaced without waiting for the tail:

| Batch | Members | Content | Event | Neither | Event-shaped findings |
|---|---:|---:|---:|---:|---|
| A | 23 | 22 | 1 | 0 | `contract_authority_current.clj` — repository-authority interval; already made fence-conditional by C326 and feeds the workspace gate. |
| B | 23 | 21 | 1 | 1 | `lane_registry_check.clj` — Agency job-state interval; `mutable_read_set.clj` is the shared observation library, not a verdict. |
| C | 22 | 18 | 4 | 0 | `quiescence_check.py`, `wm_click_resource_observer.clj`, `wm_workspace_gate.clj`, and `writer_fence_evidence.py`. |

No member was classified by default. Earlier C293/C315/C320/C322 findings were
reconciled by path; previously unmatched names were read and classified before
entering the registry.

## Executable declarations

`checks/mutable-verdict-claims.edn` is the authoritative declaration carrier.
It names every content claim, gives each event claim a typed interval kind, and
records the observation library under `:neither`.

`bb -cp . scripts/check_mutable_verdict_claims.bb` consumes both the declarations and
the live versioned population. It rejects:

- an undeclared population member;
- a stale declaration;
- a member in more than one class;
- a registry/population schema mismatch.

The checker is now a workspace-gate constituent, so the declarations are not
dead metadata. Positive verification reports:

```clojure
{:population 68 :content-shaped 61 :event-shaped 6 :neither 1
 :undeclared () :stale () :overlap () :pass? true}
```

Its negative control removes one content declaration and is rejected as
`:undeclared-member`.

## Operator-path impact

- `contract_authority_current.clj` and `wm_workspace_gate.clj` directly shape
  the workspace-gate receipt. C326 made the former fence-conditional; the gate
  already reports four-repository basis movement.
- `quiescence_check.py` and `writer_fence_evidence.py` are direct drain
  evidence. Their interval semantics are intentional and must not be rewritten
  as content currency.
- `lane_registry_check.clj` contributes to quiescence through a moving Agency
  population; instantaneous multi-job truth still needs the service revision
  token designed in C301.
- `wm_click_resource_observer.clj` measures the operator run's resource delta;
  its before/after observations are the claim rather than a hybrid defect.

## Evidence qualification

C327's open-ended clause can now be retired for this versioned population.
The honest replacement is specific:

> All 68 named mutable-input programs are classified: 61 content claims, six
> interval/event claims, and one non-verdict library. Content declarations are
> complete at population v1. Event claims retain their named fence, movement,
> revision-token, or before/after qualifications; this reconciliation does not
> turn those conditional guarantees into instantaneous truth.

Future matching programs cannot silently enlarge the population: the claims
checker fails until each new name is classified.
