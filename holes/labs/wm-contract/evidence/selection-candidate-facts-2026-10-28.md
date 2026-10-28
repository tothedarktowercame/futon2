# selection-candidate-facts-2026-10-28

Facts the record gives about each candidate at the moment of choice, for the 32
events of `selection-testset-2026-10-28.tsv` with candidates recorded. Per the
2026-10-28 packet, readiness facts are NOT here: dependencies are Giraffe need
edges in `selection-candidate-edges-2026-10-28.json` (items with state groups,
edges, rejected readings), and readiness/unblocks/cone are to be computed by
`futon3c.giraffe.pass` from those — claude-11 runs it.

## Coverage

- All 32 events covered; all 93 candidates appear in both the edges JSON
  (`items`) and the facts TSV (checked programmatically: 0 mismatches).
- 106 fact rows total; 16 `NONE-RECORDED` (candidate has no recorded fact of
  the allowed kinds — its recorded content is a dependency, now an edge, or a
  readiness statement, now computed).

## Per fact-kind: events where EVERY candidate of the event has that kind

Only these can drive a gate or an ordering on that event:

| fact-kind | uniform events | count |
|---|---|---|
| cost | K5.2, K6.3, K6.5 | 3 |
| value-or-leverage | K5.3, K5.4, K6.5, K8.1, K8.3, K8.4, K9.5 | 7 |
| chance-of-success | — | 0 |
| nearly-done | — (K4.4 gives it for P7 only; P8's fact is value) | 0 |
| date | — (K19.4's timebox is a cost row) | 0 |
| operator-asked | — (K12.4: assurance only) | 0 |
| relation-to-focus | — (K12.4: the three excluded items only; K13.4: UI polish only) | 0 |
| other:* | (several, but each names a different thing — not gate material) | — |

So on this test set, cost and value-or-leverage are the only fact kinds that
can gate/order whole events; everything else arrives asymmetrically and can at
most break ties or explain a pick.

## @guard lines that read each fact kind (verbatim, from the pattern files)

- **cost** — `take-the-cheap-win`: "@guard needs: a cost estimate per candidate (hour / day / week, or seat-tokens) and its risk." (its "and its risk" is the chance-of-success reading); also `spend-what-the-seats-have`: "@guard needs: current usage per provider and seat (Claude, Codex, Zai, Kimi), and which seats are reserved or contended." (the K1.2/K1.6 `other:usage-*` rows).
- **value-or-leverage** — `take-the-hard-one-to-learn`: "@guard needs: a statement of what the hard candidate would teach, or of which other items rest on it."; `prepare-the-ground`: "@guard needs: a named later build item, and what the candidate would change about it (the candidate's cone: everything downstream of it, not only what it frees at once)."; `work-that-pays`: "@guard needs: a paying or prospective customer, committed money, or a pitch, and the artefact the buyer would see."
- **nearly-done** — `finish-what-is-nearly-done`: "@guard needs: per candidate, how much of it is resolved (checked exit criteria, open holes left) and what remains."
- **date** — `the-date-decides`: "@guard needs: an external dated event (paper due, workshop, contract end, runway floor) and which candidates it bears on."
- **operator-asked** — `the-operator-asked`: "@guard needs: the operator's words, dated, with where they were said."
- **relation-to-focus** — closest is `end-in-something-shown`: "@guard needs: for each candidate, the capability it would demonstrate and how someone could verify it." (the K12.4 rows are scope-relation, which is what that gate needs to see the demo's boundary)
- **chance-of-success** — read only through `take-the-cheap-win`'s "and its risk", and `bound-the-bet`: "@guard needs: an uncertain candidate, and a stop rule that can be checked (a time limit, a kill clause, an artefact due by a date)."
- Readiness kinds (can-start-now / blocked-by / unblocks) are NOT fact rows; they are what `only-what-can-start-now` ("@guard needs: each candidate's needs edges … and whether each need is met.") and `what-others-wait-on-first` ("@guard needs: ready candidates (after only-what-can-start-now) and, for each, the items its completion would make ready (Giraffe one-step unblocks).") read off the edges JSON.

## Events where the chosen candidate is not distinguishable by recorded facts

- **K3.6** (diagramprover phases): all three candidates `NONE-RECORDED`, all held on structurally identical external-resource edges. The record holds three parks, not a pick.
- **K4.3** (coupling probes vs constraint): all three `NONE-RECORDED`; the only content is the dependency edges (probes → constraint waits). Distinguishing facts would have to come from the probes' own status lines, which are not part of this choice record.
- **K7.4** (giraffe stages 1–4): all four `NONE-RECORDED`; stages differ only in their edges (3 and 4 need the WM), i.e. only by computed readiness.
- **K15.4** (D5a / E6): both `NONE-RECORDED`, both "fully unblocked", same state. The record selects both; no fact separates them.

(K4.6 and K4.7 have uniform facts but the record's "choice" is a set, not a pick: one candidate, or three equally-rooted items.)

## Problems met

- The cancelled first packet's TSV draft (which used can-start-now/blocked-by/unblocks fact rows) was discarded and rewritten; nothing of it was committed.
- Candidate ids are sometimes seats (K1.2, K1.6) rather than work items; their state groups are placeholders for the pass input shape and their facts are `other:usage-*`, which `spend-what-the-seats-have` reads.
- K3.6/K3.5-class events cite the Cascade Live catalogue (as-of pinned); the graph was not re-queried, per the discovery-only rule.
- Probe states (K4.3) are read from status lines current at 2026-10-28; the record does not date their transitions (noted in that event's `rejected`).
