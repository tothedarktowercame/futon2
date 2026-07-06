# W-candidate-drift-fence / M-first-flights closure evidence — mechanical stop-recommending path

**Task:** C (mechanical stop-recommending path) — determine what source-of-record edits
would stop the WM from recommending M-first-flights, without a name-based special case.
**Author:** zai-6. **Date:** 2026-07-06. **No edits made; analysis only.**

## 1. Why M-first-flights is currently recommendable

Two independent paths feed M-first-flights into the WM's candidate pool:

### Path A: mission-registry status classification (THE primary gate)

`mission_registry.clj` scans mission docs, reads the `Status:` line, and classifies
via `classify-status`. The classifier extracts the FIRST `[A-Z][A-Z-]*` token as the
"leading state token" and matches it against terminal-state prefixes.

M-first-flights' current status line:
```
Status: PHASE A COMPLETE (2026-06-12, operator side-by-side verdict PASS — checkpoint 20)...
```

The first uppercase token is **PHASE**, not COMPLETE. "PHASE" matches no terminal
state, so `classify-status` returns `:unknown`. The bias is toward live (`:unknown`
missions stay in the pool — "a work queue should not silently hide work"). So
`live-mission?` returns true → M-first-flights appears in `open-missions`.

This is NOT a bug — it's the designed behavior. The classifier deliberately ignores
mid-line keywords (the "HEAD complete; IDENTIFY drafted" pattern) to avoid
over-excluding missions that report completed sub-phases.

### Path B: held-work ledger → sorry-grain ψ (secondary, only fires if Path A admits the mission)

`cascade_lane.clj`'s `sorry-grain-psi` reads `held-work-ledger.edn` for items matching
the mission id. The entry `:sorry/first-flights-phase-b-policy-grade-G` has
`:held/status :held` and `:held/missions ["M-first-flights"]`.

`held-items-for` does NOT filter by `:held/status` — it only does mission-id substring
matching. So as long as M-first-flights is in the candidate pool, this sorry produces
the lane's ψ (the best-measured ψ at F -0.024).

BUT: this path is inert if Path A excludes the mission. The cascade-lane only builds ψ
for missions that survived the `open-missions` filter and appear in ranked-actions.

## 2. How to stop recommending M-first-flights without a name-based special case

### The single required edit: change the mission doc Status line

If the leading token of the Status line is a terminal state, the classifier removes
M-first-flights from the live pool — no special-casing. Verified by testing the
classifier logic against all candidate closure statuses:

| Proposed leading token | classify-status | live? | Effect |
|---|---|---|---|
| `PHASE A COMPLETE` (current) | `:unknown` | **true** | stays in pool (the problem) |
| `CLOSED` | `:complete` | false | removed from pool |
| `COMPLETE` | `:complete` | false | removed from pool |
| `SUPERSEDED-AS-MISSION` | `:inactive` | false | removed from pool |
| `SUPERSEDED` | `:inactive` | false | removed from pool |
| `DEFERRED` | `:inactive` | false | removed from pool |
| `ARCHIVED` | `:inactive` | false | removed from pool |
| `PARKED` | `:inactive` | false | removed from pool |
| `DONE` | `:complete` | false | removed from pool |

**Recommended status choice: `SUPERSEDED-AS-MISSION toward M-fold-ansatz`**

Rationale:
- Phase A is genuinely complete; Phase B is the standing obligation, and the
  campaign routes the Phase-B-shaped work to M-fold-ansatz (the fold frontier).
- SUPERSEDED-AS-MISSION is the classifier's designed way to say "this mission's
  work moved elsewhere" (prefix match catches compound forms — Finding-2).
- It classifies as `:inactive`, which removes it from `open-missions` cleanly.
- It carries semantic meaning (superseded, not just done) that the audit trail needs.

### Risks of each status choice

| Status | Risk |
|---|---|
| `COMPLETE` / `DONE` | Misleading: Phase B was never completed; the mission was not "done," its remaining obligation transferred. |
| `CLOSED` | Neutral but uninformative — doesn't say WHERE the work went. |
| `SUPERSEDED-AS-MISSION` | **Best fit** — accurately describes the transfer. Minor risk: a reader unfamiliar with the convention might think the mission failed rather than transferred. |
| `DEFERRED` | Wrong: Phase B is not deferred, it's transferred. DEFERRED implies it may return. |
| `ARCHIVED` | Too final; implies no follow-on. Phase B work continues elsewhere. |
| `PARKED` | Similar to DEFERRED — implies it may be unparked. |

## 3. Recommended source-of-record changes (if closure is approved)

### Change 1 (REQUIRED): mission doc Status line
**File:** `futon3c/holes/missions/M-first-flights.md`, line 3
**Current:** `Status: PHASE A COMPLETE (2026-06-12, ...`
**Proposed:** `Status: SUPERSEDED-AS-MISSION toward M-fold-ansatz — Phase A complete (2026-06-12, operator verdict PASS); Phase B obligation transferred. Full lifecycle ran 2026-06-11/12.`

This is the ONLY change that affects the WM's recommendation. The classifier
reads the leading token `SUPERSEDED-AS-MISSION` → prefix match → `:inactive` →
excluded from `open-missions`.

### Change 2 (RECOMMENDED for hygiene): sorry registry status
**File:** `futon2/resources/sorrys.edn`, entry `:sorry/first-flights-phase-b-policy-grade-G`
**Current:** `:status :open`
**Proposed:** `:status :addressed` (or `:foreclosed`, if the sorry's vocabulary supports it)

This does NOT affect WM recommendations (the sorry only fires if the mission is in
the candidate pool, which Change 1 prevents). But it keeps the sorry registry honest:
the obligation transferred, so the sorry is no longer open.

### Change 3 (OPTIONAL for hygiene): held-work ledger
**File:** `futon3c/holes/excursions/held-work-ledger.edn`, entry for `:sorry/first-flights-phase-b-policy-grade-G`
**Current:** `:status :held`
**Proposed:** `:status :addressed` or remove the entry

Same as Change 2 — inert once Change 1 lands, but keeps the ledger honest.

## 4. Gates to run after edits

1. **`bb scripts/live_loop_step.bb gate 2a`** (or equivalent) — verify the mission
   scan picks up the new status and M-first-flights is excluded from live missions.
2. **Test:** `clojure -M:test -e '(require ...mission-registry-test)...'` — the
   existing test suite has status-classification tests; add one for
   `SUPERSEDED-AS-MISSION` if not already covered (it IS covered: the
   `finding-2-compound-superseded-status-classifies-inactive` test).
3. **Manual verify:** `(mr/open-missions)` should NOT contain `M-first-flights`.
4. **Cascade-lane cache clear:** if the serving JVM has a cached ψ, it would need
   a `clear-all-caches!` call — but only on the next tick (one-shot JVM reads fresh).

## 5. Commands and outputs from this analysis

```
Mission: M-first-flights
  status-class: :unknown
  live?: true
  status-line: PHASE A COMPLETE (2026-06-12, operator side-by-side verdict PASS...)
  open-hole-count: 6
  path: /home/joe/code/futon3c/holes/missions/M-first-flights.md

Classifier test results: all proposed closure statuses (CLOSED, COMPLETE,
SUPERSEDED, SUPERSEDED-AS-MISSION, DEFERRED, ARCHIVED, PARKED, DONE) classify
as terminal (:complete or :inactive) → live?=false → excluded from candidates.

Sorry registry: :sorry/first-flights-phase-b-policy-grade-G has :status :open
Held-work ledger: same entry has :status :held
Both are inert once the mission doc status changes (the cascade-lane only
builds ψ for missions in the live candidate pool).
```
