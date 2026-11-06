# T-formal-patterns-unblock — make M-formal-patterns' next items concretely selectable

**Requisition:** in-progress — dispatched 2026-11-06T06:22:56Z to a Kimi seat (claude-6, for Joe)

**Asked by:** Joe, 2026-11-06. **Owner:** claude-6 (review). **Worker:** Kimi seat.

## Definition (Joe, 2026-11-06)

"do what's needed to get this unblocked, including relaying clear questions to
Joe about what is needed (not 'his permission' but in technical terms, what are
the concrete blockers)".

"The resulting state should be that the next items here are concretely
available for selection by the WM, not that those items are done. The task
should no longer be classed as 'undetermined' at the end, and it should be
re-rated by Giraffe."

## Starting state (claude-6, 2026-11-06)

- Mission: `futon5/holes/M-formal-patterns.md` (sha256 457cc259… at 05:24Z;
  since changed by WM clicks). Header `Status: OPEN`; freeze section at
  "## ❄ FROZEN 2026-08-04 — no new slices until the reboot register closes".
- Register: `futon5/TN-baldwin-reboot.md` §10 "The locked register". As of
  today: N1 CLOSED, N2 DONE, N4 DONE, H1 DONE, H3 DONE, H4 DONE; N2b
  look-ahead (gate: Joe); N3 re-scoped, ledger backlog remains (gate: Joe);
  H2 open, folded into N2b; H5 partly discharged.
- Giraffe run `storage/giraffe/20261106T052455.977Z-f7d748d5`: status
  `undetermined`, reason `dependency-reading`, pass-rank 276,
  priority band unknown. Its reading (corpus
  `~/notes/wm-next/giraffe-reads-2026-11-04/corpus/batch-old-52.jsonl` line 2)
  has one unresolved `depends-on` edge to `TN-baldwin-reboot.md`: "A TN- tech
  note, not an M/E/T task document, so it does not resolve as a link target".
- The WM selected the mission through its find-out-before-building class
  (best rank among 9 undetermined items). WM commits in futon5 today:
  b0b8e37 (H1 remainder), ac8a057 (N3 backlog), 84409a9 (S4 Peeragogy
  translation — a mission slice made while the freeze notice stands).
- WM mission registry (`futon2/src/futon2/aif/mission_registry.clj` ~330-356):
  only an explicit `**Gate:**` line naming an operator decision removes a whole
  mission from selection; status prose does not.

## Outcome required

1. Questions for Joe, each stating a concrete technical blocker (what decision,
   between which options, what each option changes in the code/paper), in the
   section below, and nothing phrased as a request for permission.
2. The mission file is self-contained about the freeze: it defines slice, N, H,
   and states the register status it depends on, so a reader (and Giraffe)
   does not need the TN to know what is blocked.
3. Items that are not blocked on Joe are concrete, unchecked acceptance items
   with locators, so the WM can select them. Items blocked on Joe point at the
   question below by number. The items themselves are not done here.
4. A fresh Giraffe run no longer rates M-formal-patterns `undetermined`.

## Questions for Joe

(to be filled by the worker)

## Result

(to be filled by the worker: commits, Giraffe run id and the new row)
