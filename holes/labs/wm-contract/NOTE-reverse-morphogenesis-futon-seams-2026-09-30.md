# NOTE: reverse morphogenesis, M-futon-seams — the cascade actually followed (RM2, 2026-09-30)

zai-1 for claude-1 (PROOF-2b). Read-only: the reconstruction script is
`futon2/scripts/wm_reverse_morphogenesis.clj` (clj-kondo and check-parens
clean; no network, writes only `--out`); `futon2.aif.analysis-cascade`
(7319cb3c4) is used read-only, unmodified. No WM clicks, nothing loaded
into the futon3c JVM, no runner code touched. Evidence-store access for
this job: ZERO new queries — the caches come from the RM1 walk
(`/tmp/wm-ops.json`, `/tmp/wm-commits.json`). Exactly ONE Agency job was
dispatched (item 4). G was not computed, per instruction.

Subject: **M-futon-seams**, COMPLETE 2026-09-24; 283 operator turns
(09-24 → 09-28); **144 analysed by 象** under the strict (session-id,
turn-id) join.

## 1. The 144 analysed turns through analysis->cascades (:overlap)

Input shape verified: the analysis JSON maps feed
`analysis->cascades` directly (sentences → fragments → `pattern_refs`,
each ref validated: `:status "candidate"`, id, rationale, sha256).

- **15 turns** construct at least one cascade (each exactly one reading
  in :overlap mode — no fragment carries two validated refs).
- **129 turns cite no pattern at all**: counted here, not dropped. At
  fragment grain `analysis->cascades` reports **269
  :fragment-without-validated-pattern-ref failures** across the 144 —
  the analyses read intents/targets but stamp no validator-passed
  pattern for those fragments. That zero-pattern majority is the single
  biggest gap in the record and the reason the followed cascade below
  rests on 15 of 144 turns.

## 2. The followed cascade

Assembled by the script: nodes = patterns cited across the turns;
within-turn edges from the cascades (fragment order + roles); across-turn
edges `:next-in-session` between consecutive analysed turns of one
session (earlier → later).

- **12 nodes** (distinct patterns), **4 within-turn edges**
  (retroactive-canonicalization —precedes→ work-examples-first;
  progress-is-a-witnessed-state-change —contrasts-with→ itself across
  one turn's fragments; self-attribution —condition-enables→ itself),
  **139 next-in-session edges** over the sessions' analysed runs.
- **Recurring patterns**: `test-registry/rerun-when-the-warrant-fails`
  ×4 (09-27 → 09-28) and `measurement/warrant-travels-with-the-number`
  ×2 (09-25 → 09-28). Everything else cites once.
- **Shape**: shallow and wide, not deep — no turn chains more than two
  cited patterns, so depth comes from recurrence and session succession,
  not within-turn precedence. The spine seen in RM1 **survives the
  strict join and sharpens**: the completion days run a
  **warrant/test-registry family** (rerun-when-the-warrant-fails ×4,
  warrant-travels-with-the-number, honest-map-over-flattering-counter,
  single-source-of-truth — making evidence cheap and checkable) over
  governance/apparatus patterns (self-attribution, every-wait-has-a-
  deadline, exempt-the-in-use, consent-gate, built-but-not-wired-
  invisibility), with futon-theory's truth-family
  (retroactive-canonicalization, work-examples-first,
  progress-is-a-witnessed-state-change) at the opening.
- **Correction to RM1's §3**: the strict join DROPS six patterns the
  loose id-join had matched (scope-before-action, ready-blocked-triage,
  hot-reload-as-default-fix-path, determined-fork-proto-psr,
  hand-over-when-acting-is-worth-more, peeragogy/par) — those were
  same-id turns of OTHER sessions. The strict cascade above is the
  honest one; RM1's spine paragraph overstated the governance tail and
  the closing "handover" node.

## 3. Phase exits in the turn sequence

From the mission's turn-commits (commit cache; phase-exit commits):

| phase exit | commit | when | falls after analysed turn |
|---|---|---|---|
| MAP exit met | futon3c `5fe889bc7c` "MAP worked properly, in order: its exit is met" | 09-24 17:54 | #39 |
| DERIVE exit met | `1afa87d63a` "DERIVE: the grain step has a check now, and its exit is met" | 09-24 | #61 |
| VERIFY met (6/8) | futon2 `c452369dbc` | 09-24 20:25 | #67 |
| eight of eight (mission complete) | futon2 `4ddd207c9` "M-futon-seams complete (futon3c 3f5f44dd), eight of eight" | 09-24 21:51 | immediately after #67 |

Read phase by phase: all recorded phase exits fall inside the first ~68
analysed turns (09-24) — the mission closed on day one of the four-day
turn record; the 09-25→09-28 turns (where the warrant spine recurs) are
post-completion work (PROOF-2a continuation), NOT the completing
cascade. **The completing cascade itself is therefore mostly in the
129 unanalysed turns of 09-24** — an honest limitation: the followed
cascade this note reports is dominated by what followed completion.
HEAD/IDENTIFY/ARGUE/INSTANTIATE/DOCUMENT exit dates were not found as
commit markers (they live in the mission's section verdicts); not
extracted, not guessed.

## 4. The HEAD reading vs the followed cascade (numbers, then one paragraph)

ONE 象 job (象-1, mode brief, job `invoke-1790760531709-29054-56e9aa57`,
request built by `wm_task_reading.py --mission-head`; the file has no
`## HEAD` heading so the opening-before-first-section was read; reply
**passed `session_turn_analysis.py validate`**). The reading: 4
sentences, 8 fragments, 7 pattern refs, **5 distinct patterns**.

- HEAD patterns that appear in the followed cascade: **1 of 5**
  (`futon-theory/retroactive-canonicalization`).
- HEAD patterns NOT in the followed cascade: **4 of 5**
  (`collaboration-coherence/magical-thinking`,
  `futon-theory/mission-lifecycle`, `futon-theory/mission-scoping`,
  `nomad/self-certifying-artifact`).
- Followed-cascade patterns the HEAD reading did not foresee: **11 of
  12** (all but retroactive-canonicalization).

Paragraph: the HEAD reading frames the mission in lifecycle and scoping
vocabulary — what a mission is, how it is bounded, how it certifies
itself — and almost none of that machinery is what the operator's turns
actually ran on the way to completion. What completed the mission, as
far as the analysed record reaches, was a warrant economy: reruns
avoided by warrants, numbers that travel with their warrants, honest
maps over flattering counters, plus apparatus discipline (deadlines on
waits, exemptions for the in-use, self-attribution). If Joe's idea is
that a HEAD reading gives an initial cascade that G-minimising
adjustment moves toward what actually completes, this case says the
distance is large and specific: 11/12 of the followed cascade was
unforeseen, and the one overlap is a bookkeeping pattern, not a working
method. But the caveat of §3 cuts the other way too — most of the
09-24 completing turns are unanalysed, so the followed cascade here is
biased toward post-completion work; the true test of the idea needs the
129 missing analyses, priced in RM1.

## 5. Retraction of the HEAD reading onto the pattern graph

Graph written fresh: `mined_pattern_graph.py --out` — 3,119 typed edges
(why/how/co-cited/rejected-beside/next-in-session), 1,431 patterns,
giant component 620. Pinned: file sha256
`29ace7cfcaeec4ce904eb775e558fd01d125bd21c026d29b014edc3dfa07747d`;
canonical-edge digest inside the retraction result
`700aa658df431ad78817ad9f4e43552320d58ed711c1437126c4c334c728bd04`.

All three k=3 retractions of the HEAD's five seeds are cheap (costs 17,
19, 19; cuts of 6–7 weak edges) and each leaves the seeds attached to a
637-pattern component that contains **12/12 of the followed cascade**.
Reported as the failure mode it is: at k=3 the retractions do not
separate the HEAD reading from the followed cascade at all — the mined
graph's next-in-session/co-cited density fuses both into one giant
component, so "coverage" is total and vacuous. A retraction that
actually priced the distance between the HEAD reading and what
completed the mission would need k=1, seed-scoped subgraphs, or the
co-rejected kind still priced but no longer written.

## Reproduction

    clojure -M scripts/wm_reverse_morphogenesis.clj -- \
      --mission M-futon-seams --ops-cache <RM1 ops walk> \
      --commits-cache <RM1 seams commits> --out <results.json>

Artifacts of this run (not committed, /tmp): `wm-rm-seams.json`
(reconstruction), `wm-head-request.json` / `wm-head-analysis.json`
(request and validated 象 reading), `wm-pattern-graph.json`,
`wm-head-retraction.json`, `wm-head-reply.txt` (the seat's reply).
