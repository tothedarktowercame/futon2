# WM defect-class audit, 2026-09-19

Six zai agents, one defect class each, discovery only. Joe's instruction: we
kept finding these one at a time as they stopped a run; find the class instead.
Each dimension was generalised from a defect that had actually stopped a run in
the preceding 24 hours.

65 findings, 12 `:confirmed`. Files are the agents' raw output, unedited. The
verdicts below are mine, from reading the code, not relayed.

| file | dimension | agent | n | confirmed |
|---|---|---|---|---|
| d1-absent-value.edn | absent value into a fn assuming presence | zai-9 | 13 | 0 |
| d2-key-list-drift.edn | hand-copied key list vs its source map | zai-10 | 6 | 0 |
| d3-nil-as-identity.edn | nil as an identity in group/dedup keys | zai-11 | 9 | 0 |
| d4-silent-miss.edn | lookup miss becomes a skip, not a refusal | zai-12 | 13 | 5 |
| d5-unbounded-write.edn | durable write with no size bound | zai-13 | 12 | 5 |
| d6-stale-controls.edn | negative control whose plant stopped landing | zai-14 | 12 | 2 |

## Fixed from this audit

- futon2 `353cdc29` — three `pprint` sites in the repair store. Measured on a
  real 19.4 MB finding: pprint 47,306 ms / 19.4 MB, pr-str 212 ms / 12.0 MB.
  223x faster, 38% smaller, and those 47 seconds were held inside the contended
  store lock. (d5, zai-13.)
- futon2 `f8a526f6` — `group-by :mission/repo` unguarded while its sibling 179
  lines earlier guards the same field. `:mission/repo` is present on all 390
  live missions and null on 298 (76%); all 298 were one nil bucket. (d3,
  zai-11.)
- p4ng `6a683aa` — `control_plants_check.py`, built from d6's proposed check.
  0.03s static pass; falsified by breaking a plant and confirming it reports
  CANNOT-LAND.
- futon2 `b6da1420` — T10 (found by hand while the swarm ran, same class as d4).

## Two discriminators that earned their keep

**Does a sibling call site already guard this field?** This is stronger than
arguing that nil could occur: it is the codebase's own admission that it does.
It is what made the `:mission/repo` finding cheap to confirm, and it is what
says the `find_reconciliation.clj:29` finding has no evidence either way —
`:treatment` and `:disposition` appear at lines 25 and 29 and nowhere else.

**Does this code have a production caller?** Cheap, and it re-ranks findings
hard. Apply it carefully: my first pass searched `src/ scripts/ test/` and was
about to record `find_reconciliation.clj` as having no caller. It has one, in
`holes/labs/wm-contract/f11_f2_reconcile.bb`. An absence established by an
under-scoped search is the same defect as `d4`, one level up.

## Method notes, for the next pass

- Requiring `:why` (why the bad case is reachable) and capping at 15 findings
  kept these files readable. A grep dump was declared a failed packet up front.
- `:confirmed` clustered in d4 and d5, the two dimensions where the bad case
  can be demonstrated from data on disk. d1/d2/d3 are code-reading dimensions
  and their agents correctly ranked most findings `:speculative`.
- Distinguish *key absent* from *key present, value nil*. I reported "298
  missions without `mission/repo`" when the key is present on all 390 and the
  value is null on 298 — same number, wrong claim. `(or (:k %) default)` covers
  both; `(contains? m :k)` covers one.
