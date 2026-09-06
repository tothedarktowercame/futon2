# N — Zai roles dispatched on the fly, and the custom tools they need

Commissioned by Joe, 2026-09-06 (verbatim): "less in terms of a loop to go
around trying to get full coverage of the whys and hows in the library... more
in terms of an example of the kind of role that we might dispatch a Zaif agent
to do on the fly. And I think it would be nice to start to develop a list of
the custom tools that we would actually want to make available... I think the
Zaif agents are good stand-ins for system stakeholders, and currently those
stand-ins are mainly enacted by operator turns."

This note is a design draft, not a board. Rows mint on a lane board when a
role is actually commissioned.

## 1. Where the library loop stopped (measured 2026-09-06)

- Board: 19/19 rows done; loop ended 2026-09-05 14:09 after 9 iterations.
- futon3 head unchanged since the final census (`50bd5309`), so the L17/L18
  graphs ARE the current state.
- Raw metadata: 855 files carry `@why` (1,420 occurrences); 813 `@how`
  occurrences. @how measured as a thin editorial layer (L19: 21 resolving
  values; most @how edges run between ordinary patterns, adding few roots).
- Served/refused under the L4 baseline reading (down-problems+wr), per
  cascade, from `runs/L18-graph.edn` via `runs/L17-advisory-gate-report.md`:
  alfworld 0/1 refused, snatch 0/12 (both fully served); ants 5/6;
  construct 11/33 (was 31); zaif 24/53 (was 50); open and retrodiction each
  817/1239 (was 1187).
- The honest frontier, unchanged: the remaining refusals are patterns for
  which no committed source states their problems. That is an AUTHORING gap,
  not a linking gap; the loop closed what linking could close.

## 2. The reframe: role dispatch, not coverage grind

Full-graph coverage is the wrong unit. Cascade work consults specific
cascades; the useful assurance is "the cascades we are about to construct
draw on served patterns," which is a per-cascade refusal list shrinking, not
a global percentage rising. That makes the natural work unit: **dispatch a
role against a named cascade's refusal frontier** — e.g. "serve
open-cascade's next 50 refused patterns under down-problems+wr" — with a
before/after delta as the acceptance bar.

The stakeholder framing: a dispatched Zai role stands in for a system
stakeholder (the consumer of a cascade, the author of a problem statement,
the auditor of a census). Today those stands-in are mainly enacted by
operator turns; the closure-over-the-operator question (Box-12 track,
EPIC 2cc70c9c) is exactly whether the machinery around such a dispatch —
commissioned, dispatched, parked, returned, checked, recorded, surfaced —
exists as running code rather than as Joe doing it by hand.

## 3. First exemplar roles (stakeholder stand-ins)

- **library-annotator** — serves one cascade's refusal frontier: finds
  committed evidence stating a refused pattern's problem, authors the
  `@why` (and `@how` where a mechanism source exists), cites file:line.
  Acceptance: gate re-run shows the refusal count down; every new edge
  resolves; no dangling refs.
- **problem-author** — the authoring round the loop could not do: mines
  excursion logs / holes corpus for problem-stating sources and commits
  problem nodes, so annotator roles have something to cite. (Discovery
  split from annotation — two roles, not one.)
- **census-auditor** — re-runs the census + gate, publishes graph, receipt
  and delta; refuses to publish on a dirty tree or unpinned head.
- **cascade-stakeholder** — reads a cascade as its consumer ("does
  zaif-cascade serve the zaif lane's actual work?") and files tensions as
  recorded observations, not edits.

## 4. The custom tools list (the clarifying deliverable)

Grounded in what the 19 loop rows actually did by hand; each tool is a
script with REFUSAL semantics (exists = refuses or records, the Box-12
standard), callable from a role prompt.

1. **gate-readings** — wrap `runs/l13_graph_gate.clj`: given cascade +
   reading, return the named refusal list. Exists, offered-not-wired.
2. **refusal-frontier** — "next N refused patterns for cascade X under
   reading R," with the pattern files' paths. Thin wrapper over (1).
3. **edge-write-with-refusal** — add `@why`/`@how` only if the target node
   exists, the syntax parses, and the cited source resolves to a committed
   file:line; refuses dangling refs (L14 measured unresolvable edges as a
   real class).
4. **evidence-search** — scoped rg over the committed corpus returning
   quotable file:line spans, so annotations are grounded in sources, not
   invented.
5. **census-run** — l1-style sweep producing graph + receipt with repo
   shas pinned; refuses on dirty tree.
6. **coverage-delta** — before/after refusal table per cascade (the
   L12/L14 delta format), the acceptance-bar instrument.
7. **receipt-append** — append-only run receipt (who, what head, what
   command, bare exits); every loop row hand-rolled this.
8. **board-row** — claim/update a worklist row through the lane's
   worklist_check gate, never a raw edit.
9. **lifecycle primitives, scoped** — bell-back with commission-id
   threaded; park with absolute deadline; a discharge event on surfacing.
   These are exactly the cells Box-12 records as absent (R10
   commissioned/dispatched, R16 parked/surfaced): the tool list here IS
   the concrete requirement list for those cells.

## 5. Join with the Box-12 track

The role-dispatch lifecycle and the process-assurance lifecycle are the
same seven stages. Proposal for the zaif-lane review (when claude-2's
seeded board comes back): one row that runs a single end-to-end exemplar —
library-annotator against one cascade's refusal frontier, with all seven
stages present as running code — as the concrete instance the abstract
cells are measured against. One real dispatch that refuses and records
beats six cells argued from prose.

## 6. The operator-turn miner (Joe, 2026-09-06)

Verbatim: "for the zaif loop... we would be mining the operator turns,
looking for new ideas for harness tools and harness protocols that we
could use in future Zaif runner applications."

This closes a loop the note left open: sections 3-4 derived roles and
tools from what the LOOP ROWS did by hand. The richer corpus is what the
OPERATOR does by hand -- and PA11z just gave that corpus a measured
meaning: the `:operator-turn` verdict marks lifecycle stages enacted only
by turns. The miner is the discovery role over that quantity.

- **Role: operator-turn-miner.** Sweep the recorded operator turns and
  extract (a) recurring interventions -- things Joe does repeatedly that a
  tool could refuse-or-record instead (each is a candidate harness tool,
  and each maps onto an `:operator-turn` or `:absent` census cell);
  (b) stated-but-unbuilt protocol ideas -- turns that describe a way of
  working ("park on every dispatch", "decision sheets not counts") before
  any code enforced it. Output: a candidate list with per-item verbatim
  quotes and turn pointers, routed as PROPOSALS, never as board rows --
  what becomes a tool is a lane-owner or operator decision.
- **Corpus, in evidence order:** (1) the curated verbatims already in
  EPIC-run-era.md and the rulings registries (cheap, high-signal);
  (2) the raw session records under ~/.claude/projects/*/ where every
  emacs-repl turn with Caller: joe is preserved per agent; (3) the
  agency job prompts where operator text was forwarded. The miner cites
  turn + date the way the annotator cites file:line.
- **Join with Box-12:** the miner's finding rate should FALL as the
  track closes cells -- interventions it keeps finding are cells the
  track has not reached. That makes it a measuring instrument for the
  same headline number, from the demand side.
- **Runtime:** this one genuinely wants a zai seat (it is reading Joe's
  prose, the role-fidelity case claude-2 named); schedule after the zai
  quota reset 2026-09-08.
