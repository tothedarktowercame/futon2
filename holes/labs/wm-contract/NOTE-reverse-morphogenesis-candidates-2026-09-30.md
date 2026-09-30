# NOTE: reverse morphogenesis — candidate completed missions and their operator-turn record, 2026-09-30

Discovery only (zai-1, for claude-1). No runner/library code changed, no
clicks fired, nothing loaded into the serving JVM, no seats dispatched.
Evidence-store access was bounded and sequential: one 20-page walk of
`GET :7073/api/alpha/evidence/?author=joe` (limit 1000, `before`-paged)
plus two bounded `text-search` calls. Method and every limitation is
stated inline; where a number is an upper bound from an unreliable join,
both numbers are given.

Purpose (Joe, 2026-09-30): for a COMPLETED mission, work out from the
record what pattern cascade actually gave rise to its completion —
"reverse morphogenesis". See NOTE-g-as-fold-2026-09-30.md.

---

## 1. When the store began recording operator turns, and how a turn ties to a mission

- **First dated operator-turn entry**: `2026-02-18T15:34:37.711770692Z`
  (`evidence/at`; `chat-turn`, role `user`, author `joe`, session
  `s-debug`, text "Can you summarize what changed under the hood?"). The
  walk paged author=joe back to this entry; 18,798 operator turns
  (`event "chat-turn"`, `role "user"`) are recorded in total.
- **Joins, reliable vs guess**:
  - RELIABLE: the entry body's **`mission-id` / `clocked-mission`**
    (`clocked-target` beside it), written by the chat/turn boundary when
    the operator's session had a mission clocked. 7,190 of the 18,798
    turns carry it; the earliest with it is `2026-06-03T12:40:25Z`
    (mission `M-foo`). Commit entries (`event "turn-commits"`) carry the
    same join.
  - RELIABLE, later only: **`(session-id, turn-id)`** between store
    entries and the 象 analysis files — on the "typed" surface (late
    2026-09 onward) the store entry's `evidence/session-id` equals the
    analysis file's `session_id`. **`turn-id` ALONE IS A GUESS**: turn ids
    are session-scoped (`claude-8-turn-525`), so ids collide across
    sessions; joining on id alone overcounts analyses per mission badly
    (e.g. M-wm-policies: 57 by id-only, 1 by (session,turn)).
  - GUESS: text matching, and the agent's clocked mission for turns the
    operator wrote in an unclocked session.
  - 11,608 turns carry no mission join at all (early period + unclocked
    sessions); for those, only the text or the reply thread can tie them.

## 2. Shortlist: six COMPLETED missions with the most recorded operator turns

Completion rule as given: the status line says closed/complete/finished;
phase words do not count. Counts are operator turns carrying that
`mission-id`. "analysed" = a 象 analysis exists joined by
**(session-id, turn-id)** (strict); the looser id-only number is given in
brackets as an UPPER BOUND known to overcount. Worktree copies were
ignored (canonical repos only).

| mission | file | status line (quoted, truncated) | turns | first→last turn | analysed strict [loose] | to analyse | commits in window |
|---|---|---|---|---|---|---|---|
| **M-futon-seams** (WM-related: PROOF-2 worked example) | futon3c/holes/missions/M-futon-seams.md | `**Status:** **COMPLETE (2026-09-24)** — all eight phase exits met, each verdict in the section and checked against holes/labs/M-futon-seams/lifecycle.edn …` | 283 | 2026-09-24 → 2026-09-28 | **144** [166] | 139 | 192 turn-commits recorded (futon2-heavy: the PROOF-2a commit stream, e.g. `4ddd207c9` "PROOF-2a: M-futon-seams complete (futon3c 3f5f44dd), eight of eight"; method: one `text-search q=M-futon-seams&limit=1000`, first 1000 results) |
| **M-wm-policies** (WM) | futon2/holes/M-wm-policies.md | `**Status:** CLOSED — **CLOSED (Joe, 2026-06-24).** All 4 completion criteria MET + live-verified; … DOCUMENT done` | 201 | 2026-06-09 → 2026-06-24 | **1** [57] | ~200 | not pulled (June-era; see §4) |
| **M-capability-star-map** (WM-adjacent: closed via WM pilot cycle #1) | futon0/holes/missions/M-capability-star-map.md | `**Status:** CLOSED — CLOSED 2026-06-10 — operator close via WM pilot cycle #1 (pilot claude-3); caveats spun out …` | 129 | 2026-06-07 → 2026-09-26 (tail after closure) | **7** [130] | ~122 | not pulled |
| **M-typed-holes** (WM: the typed-holes contract) | futon3c/holes/missions/M-typed-holes.md | `**Status:** CLOSED — **CLOSED (2026-06-15, Joe).** Ran the full lifecycle` | 107 | 2026-06-14 → 2026-06-17 | **1** [123] | ~106 | not pulled |
| **M-points-de-fuite** | futon2/holes/M-points-de-fuite.md | `**Status:** CLOSED — IDENTIFY → MAP (§2) → DERIVE prototyped … ARGUE/VERIFY basic-pass` | 70 | 2026-07-02 → 2026-07-27 | **0** [44] | 70 | not pulled |
| **M-agency-hardening** | futon3c/holes/missions/M-agency-hardening.md | `**Status:** CLOSED — CLOSED 2026-06-12 (Joe's call) — but see …` | 45 | 2026-06-10 → 2026-08-06 | **0** [0] | 44 | not pulled |

WM-related in the six: M-futon-seams, M-wm-policies, M-typed-holes (+
M-capability-star-map closed by a WM pilot) — satisfies "at least 3".

Notable EXCLUSION **(superseded by the addendum below)**: M-apm-demonstration fails the mission-grain rule (status reads OPEN), but its HEAD phase is complete and it becomes the TOP entry once any grain counts.

### ADDENDUM (claude-1, same day): the completed unit, not the completed mission

The closed/complete-the-whole-file rule is Joe's rule for what is still in
the FIELD. For reverse morphogenesis the unit is COMPLETED WORK at any
grain — a closed mission, a completed PHASE with a recorded exit, an
accepted/ticked criterion, a closed ticket or excursion. The six
completed units with the most recorded operator turns (turns = operator
turns carrying that mission-id; analysed = strict (session,turn) join;
dates = first→last recorded turn):

| unit | grain | the line that shows it is complete | turns | analysed | to analyse |
|---|---|---|---|---|---|
| **M-apm-demonstration** (futon3c) | phase — HEAD complete | `**Status:** OPEN — HEAD complete; IDENTIFY draft pending operator acceptance (2026-08-14)` — the HEAD exit is recorded; only IDENTIFY pends | **1,519** (08-14 → 08-27) | 336 | 1,183 |
| **M-象-2000** (futon3c) | phase — VERIFY passed | `**Status:** OPEN — INSTANTIATE (VERIFY passed, Joe 2026-09-27; builder codex-4)` | 289 (09-26 → 09-30) | 137 | 152 |
| **M-futon-seams** (futon3c) | mission | `**Status:** **COMPLETE (2026-09-24)** — all eight phase exits met …` | 283 (09-24 → 09-28) | **144** | 139 |
| **M-wm-wiring** (futon3c) — WM | phase — five exits met | `**Status:** OPEN — HEAD, IDENTIFY, MAP, DERIVE, ARGUE met (Joe's read, 2026-09-25 ~23:55Z: "it looks good to me"); VERIFY entered …` | 250 (09-25 → 09-27) | 61 | 189 |
| **M-wm-policies** (futon2) — WM | mission | `**Status:** CLOSED — **CLOSED (Joe, 2026-06-24).** All 4 completion criteria MET + live-verified …` | 201 (06-09 → 06-24) | 1 | ~200 |
| **M-learning-loop** (futon5a) | phase/criteria — criteria 3–4 accepted | `**Status:** OPEN — INSTANTIATE (pipeline live; … **criteria 3–4 ACCEPTED by operator 2026-07-22** — see checkpoint; …)` | 210 (07-06 → 07-22) | 1 | 209 |

War Machine related in the widened six: **M-wm-wiring, M-wm-policies,
M-futon-seams** (the PROOF-2 worked example) — three, as required. The
best-recorded WM unit at phase grain is **M-wm-wiring** (250 turns, five
recorded phase exits); the best-analysed unit overall is still
**M-futon-seams** (144), with M-象-2000 (137) and M-apm-demonstration
(336 analysed — the largest analysed set of all) close behind.

Grain-3 units (a single ticked acceptance item with its ticking commit in
the record) were NOT separately enumerated: joining ticked boxes to their
commits needs a per-file git walk plus the turn-commits pull, which this
pass did not spend queries on — the closest verified examples are
M-learning-loop's accepted criteria (quoted above) and M-futon-seams's
eight phase-exit verdicts against lifecycle.edn. Closed tickets/excursions
were not enumerated either (no ticket/excursion entries carried a
mission-id join in the walk). Both remain open for a follow-up pass.

This addendum also changes the §4 price tag: the six completed UNITS need
**~2,070 turns analysed** (1,183 + 152 + 139 + 189 + 200 + 209), roughly
69–173 seat-hours at 2–5 minutes each — dominated by M-apm-demonstration,
which is also already one-fifth analysed.

The 象 analysis corpus: 3,769 batch analyses
(/home/joe/code/storage/operator-turns/batches, 2026-08-22→2026-09-21,
older validator) + 838 live analysed turns
(~/.emacs-graph/session-turn-analysis) = 4,607; 3,657 of the batch
analyses carry a `pattern_refs` field.

## 3. The best candidate: M-futon-seams — the analysed turn sequence

144 of its 283 turns have analyses by the strict join — far ahead of the
others (the June missions predate the analysis surface almost entirely).
Of the analysed turns, 18 live ones cite pattern ids in `pattern_refs`
(all `:status "candidate"`). In order:

| date | turn | patterns cited |
|---|---|---|
| 09-23 | claude-1-turn-37 | cascade-construction/hand-over-when-acting-is-worth-more |
| 09-24 | claude-1-turn-44 | agent/scope-before-action |
| 09-24 | claude-1-turn-47 | stack-coherence/ready-blocked-triage |
| 09-24 | claude-1-turn-51 | agency/self-attribution |
| 09-27 | claude-8-turn-56 | futon-theory/single-source-of-truth |
| 09-27 | claude-8-turn-72 | apparatus/every-wait-has-a-deadline |
| 09-27 | claude-8-turn-82 | test-registry/rerun-when-the-warrant-fails |
| 09-27 | claude-8-turn-97 | hygiene/exempt-the-in-use |
| 09-27 | claude-8-turn-99 | test-registry/rerun-when-the-warrant-fails |
| 09-27 | claude-8-turn-108 | peripherals/hot-reload-as-default-fix-path; collaboration-coherence/determined-fork-proto-psr |
| 09-27 | claude-8-turn-113 | test-registry/rerun-when-the-warrant-fails |
| 09-28 | claude-8-turn-173 | futon-theory/honest-map-over-flattering-counter |
| 09-28 | claude-8-turn-193 | measurement/warrant-travels-with-the-number |
| 09-28 | claude-8-turn-198 | test-registry/rerun-when-the-warrant-fails |
| 09-28 | claude-8-turn-208 | peeragogy/par |

Shape visible even at this grain: the closing days run a
**warrant/test-registry cascade** (rerun-when-the-warrant-fails 4×,
warrant-travels-with-the-number, honest-map-over-flattering-counter,
single-source-of-truth) — the completion is driven by making evidence
cheap and checkable — preceded by governance patterns
(scope-before-action, ready-blocked-triage, self-attribution,
every-wait-has-a-deadline) and closed by the operator-side handover
(hand-over-when-acting-is-worth-more, 09-23, and par, 09-28). That is
exactly the "cascade actually followed" Joe wants reconstructed. The
remaining ~126 analysed turns cite intents/targets but no pattern ids
(their `pattern_refs` are empty), so the full cascade needs the
intent/target sequence, not just refs.

`futon2.aif.analysis-cascade/analysis->cascades` (futon2 7319cb3c4,
"aif: derive overlapping cascades from task readings") is the right tool
to derive the overlapping cascades from these analyses; it was NOT run
for this note (kept read-only, no JVM work beyond the queries above).

## 4. What is missing to do this properly for all six

- **Turns to analyse** (strict-join gap, at ~2–5 min each of seat time):
  M-futon-seams 139, M-wm-policies ~200, M-capability-star-map ~122,
  M-typed-holes ~106, M-points-de-fuite 70, M-agency-hardening 44 —
  **~680 turns total** (roughly 23–57 seat-hours). The June missions are
  nearly unanalysed because the analysis surface only exists from
  2026-08-22 (batch) and fully from the late-September typed surface.
- **Unreliable joins**: turn-id alone collides across sessions (the
  bracketed loose numbers above are overcounts, not undercounts);
  11,608 operator turns carry no mission-id (early or unclocked), so
  pre-June and unclocked work on even these six missions is invisible to
  the reliable join and would need text/-thread recovery — a guess.
- **Work predating the store / the clocked join**: M-wm-policies,
  M-typed-holes, M-agency-hardening, M-capability-star-map completed in
  June 2026; their turns are recorded (June dates present) but their
  analyses are missing and their commits were not pulled here — the
  `turn-commits` entries are reachable only via full-text search (one
  bounded query per mission, first 1000 results; a complete pull needs a
  filtered author/event walk, which I did not spend queries on).
- **Pattern coverage**: only a minority of analyses carry pattern ids
  (all `:status "candidate"`, none promoted); the cascade shape for the
  June missions will have to be built from intents/targets until those
  turns are analysed and pattern-matched.
- **Not determined**: whether `analysis->cascades` accepts these JSON
  analyses directly or needs a conversion (not read past its name);
  whether M-apm-demonstration's OPEN status will flip soon (it would
  become the best candidate by an order of magnitude); the exact date the
  typed session-id surface became uniform (observed working from
  ~2026-09-23 for seams, not verified earlier).
