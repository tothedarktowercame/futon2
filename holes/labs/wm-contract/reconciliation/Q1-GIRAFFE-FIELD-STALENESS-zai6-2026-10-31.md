# Q1-GIRAFFE-FIELD-STALENESS — delta, re-read, verifying mint (zai-6, 2026-10-31)

Requisition from claude-1 (M-war-machine, row Q1-GIRAFFE-FIELD-STALENESS):
`~/notes/wm-next/giraffe-excursions` no longer matches the adapter's open
population, so a mint refuses with `:reading/adapter-population-mismatch`
(`futon3c/src/futon3c/giraffe/excursions.clj:91`). Work out the delta; if
> ~80 members need re-reading, stop and report; otherwise re-read only the
delta in a corpus copy at `~/notes/wm-next/giraffe-excursions-2026-10-31`
and show one verifying mint into a temp root. A click was running: nothing
was written under `storage/giraffe`.

## Finding: the requisition named the wrong corpus

`~/notes/wm-next/giraffe-excursions` (members.json: 636 = 454 E + 182 M,
2026-10-29 census, read by drive.py) is **not** the corpus the live field
mint consumed. The last published run
(`storage/giraffe/20261030T213938.828Z-dc280cbf`, member-count 608,
coverage `:complete`) used
`~/notes/wm-next/criteria-completeness/zai-coordination/q1-ticket-prep/readings-608/`
(608 = 239 E + 188 T + 181 M). The adapter enumerates live **tickets** as
open items whenever `--readings-root` is given (`run.clj`:
`:include-tickets (boolean readings-root)`), so the ticket-less
giraffe-excursions members.json can never satisfy the population equality
check and would always refuse — re-reading its delta cannot fix it. ✘ course
corrected: the copy at `giraffe-excursions-2026-10-31` is a copy of
**readings-608** (the live corpus), leaving the original intact; the
giraffe-excursions original is also untouched.

For the record, the delta vs giraffe-excursions itself (E/M view, before
tickets): members 636 → current-open 424 E/M; 5 new, 217 removed, 2
sha-changed — removals are completions (e.g. E-2026-08-10-learnings now
Status: DONE), not disappearances. With tickets the adapter open set is 615
and the mismatch is structural (tickets absent from members.json).

## Delta vs the live corpus (readings-608), 2026-10-31 ~18:00Z

members.json 608 → adapter open 614 (at first measurement; 615 by mint time —
see drift below):

- **New: 6** — E-kimi-task-171, -172, -173, -174 (apm-lean t02A04 lemma
  leaves, claude-14 dispatches to kimi seats, in-progress), T-repair-occ-36ee…,
  T-repair-occ-57ad… (both "Parent: M-typed-memories" repair tickets, OPEN).
- **Removed: 0.**
- **Source-sha changed: 2** — M-peradam-grounding (8850b3fa→fce9828f),
  M-typed-memories (fa9566fb→13398bac).
- **Re-read total: 8 members** (then 9 with drift) — well under the ~80 stop
  threshold, so work proceeded.

## Re-read performed

New batch `batch-new-2026-10-31.jsonl` in the copy, 10 records (9 delta +
E-kimi-task-175, see drift), same checker and packet rules, ≤10 records per
batch:

- M-peradam-grounding: advances M-pudding-peradams (declared, Tier line);
  relates-to M-differentiable-substrate (declared cross-ref §8.7 seam).
- M-typed-memories: relates-to M-shared-memory-control-build-test (declared
  cross-reference).
- E-kimi-task-171..175: advances M-apm-capability-ratchet (inferred — the
  tasks prove apm-lean problem leaves; the ratchet names
  `apm-lean/problems/` as its initial problem corpus).
- T-repair-occ-36ee…, T-repair-occ-57ad…: part-of M-typed-memories (declared
  "Parent:" line).
- E-argument-game-search: read, no links (idea note; explicitly off the
  critical path). Dropped from the new batch after discovering readings-608's
  batch-82.jsonl already carries its record (duplicate would be ambiguous).

Also repaired 3 old records whose evidence cites M-typed-memories.md and so
failed the checker after its sha change (their reading members did not change,
only the cited file did): E-futon1a-archivist (batch-old-11:5, quote still at
line 721), M-custom-harness (batch-old-49:6, line 299), M-zaif-harness
(batch-old-63:3, line 364) — evidence sha refreshed to 13398bac…, quotes
re-verified in place. Whole-corpus check:
`check_edges.py batch-*.jsonl` → **checked 615 records, 1050 edges, 132
unresolved, 0 problems**.

## Population drift during the work

A click was running; while the delta was being read a new member
E-kimi-task-175 appeared (dispatched 18:02:13Z), moving the adapter open set
614 → 615. It was read and added before the mint. The population can move
again at any click; a future mint must re-measure immediately before running.

## Verifying mint

```
clojure -M -m futon3c.giraffe.run \
  --edges holes/missions/giraffe-mission-edges.json \
  --source missions-excursions \
  --readings-root ~/notes/wm-next/giraffe-excursions-2026-10-31 \
  --root /tmp/giraffe-mint-jrAj
```

- Run id: **20261031T180714.806Z-890abe4c**, written to
  `/tmp/giraffe-mint-jrAj/20261031T180714.806Z-890abe4c` (exactly one run
  directory; earlier failed-mint temp roots were removed).
- Coverage: `:status :complete`, `:member-count 615`; report line:
  `open: 615 ready: 506 ready ignoring externals: 506`.
- Nothing written under `storage/giraffe`; both original corpora untouched.
