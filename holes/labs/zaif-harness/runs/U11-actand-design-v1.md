# U11 — actand-indexed world-model source for :task-belief (design v1)

Date: 2026-09-03. Author: claude-2 (design lead per the 2026-09-02 joint-pass
agreement; claude-1 reviews with wm-side constraints). Status: REVIEWED by
claude-1 (bellback invoke-1788432100206, pass with conditions); amendments
below marked [A1]-[A3] are those conditions applied. Registry entry lands
wm-side as their U33. Inputs: `runs/U11-reading-map-v1.md` (U11a, reviewed),
`wm-contract/DESIGN-c-vector.md` §5–§7, D8a/D8b (the task-belief seam),
Z1's promised table (M-zaif-harness.md:187-194).

## 1. The object — one statement, two indices

**Q_actand : Grain → Actand → Action → Density over declared observables.**

IF zaif needs an arm-grain forward model (what happens to THIS correction
target under THIS arm) and the WM carries a mission-grain `Q(o|pi)` hole
(DESIGN-c-vector §3), HOWEVER building two separate forward models would
recreate the two-C mistake one node over (§5's diagnosis: one function stated
too widely, repaired as a grain-indexed family), THEN Q_actand is declared
once as the family above with exactly two indices today —
`:arm-session` (zaif: actand = the session's correction target; actions = the
registry arms; observables = the declared outcome vocabulary of the gold
judgments) and `:mission` (wm: actand = the clocked mission; actions = tick
candidates; observables = the declared tick channels) — BECAUSE §5 already
establishes the pattern (`C_int`/`C_mis`) and the acceptance bar is that the
zaif source and the WM hole are *the same object at two indices*, checkable
by their sharing the refusal constructor and provenance shape below.

A (grain, actand, action) triple with no typed source is
**`:q-actand/no-typed-source`** — a typed constructor, not an empty density
and not a uniform prior.

[A1] (claude-1 review condition) Shared constructors and provenance shape are
implementation sharing; the identity claim becomes CONTRACT sharing through
**one record schema (`q-actand-record`) and one validator test that both
grain instantiations must pass** — a divergence in either lane fails the one
shared test instead of drifting silently. This is part of U11b's acceptance:
the schema + validator land with the table, the `:arm-session` instantiation
passes it immediately, and the `:mission` instantiation cites the same test
when it lands.

## 2. Provenance discipline (the D8b seam is the enforcement point)

IF every density row must be traceable to records, HOWEVER an untyped or
unprovenanced value entering `:task-belief` would silently become a prior,
THEN every Q_actand query result carries
`{:provenance {:source <registry-key> :query <query-id> :record-ids [...]}}`
and enters the controller ONLY through the existing seam —
`zaif_inputs.clj:79-91 task-belief-from`, which already refuses
value-without-provenance as `:d8/unprovenanced-task-belief` (verified live
2026-09-03) — BECAUSE D8b built exactly this gate and the design's job is to
feed it, not to bypass it. A predicted outcome with no typed source is a
refusal, never a prior.

## 3. The three arms (proposed registry entry, verbatim)

Proposed for `wm-contract/aif-equations.edn :choices` — **:status :open**;
this registers the choice set and its decision procedure, not a ruling (Joe's
2026-09-01 rule: the comparison is designed into the row, not ruled in
advance). Registry lives wm-side, so landing this entry is part of claude-1's
review of this draft:

```edn
:task-belief-actand-source
{:observed :no-actand-source-wired  :status :open
 :arms {:A {:site "futon3c/src/futon3c/portfolio/policy.clj:49-74"
            :grain :arm-session
            :note "portfolio pragmatic-value; scalar heuristic over 5 declared channels + mu-sens + adjacent-missions"}
        :B {:site "futon3c/src/futon3c/aif/mission_head.clj:135-176"
            :grain :mission
            :note "mission-head pragmatic/epistemic/effort lookups; scalar over 4 declared channels"}
        :C {:site "S6 cascade-catalog playout (NOT BUILT; futon4/holes/mission-lifecycle-wm-alignment.md:143-189)"
            :grain :mission
            :note "accreted query over PSR/PUR, discharges, :shown lists, clock lineage; named so the registry records it"}}
 :decision-procedure "U11 §6 replay comparison; DISCRIMINATION headline; baselines named there"
 :statement "Three candidate sources for Q_actand; scalar arms A/B are :scalar-awaiting-density (DESIGN-c-vector §6) until declared as log-density images; no arm is ruled on before the comparison runs."}
```

## 4. Adapters — channel mapping is declared, missing input refuses

IF no recorded corpus supplies arm A's or B's exact channel vocabulary
(reading-map conclusion: structural for the 56 decisions, measured 0/114 for
calibration), HOWEVER renaming `:mission-health` into a mission-head channel
would be a vocabulary substitution passed off as replay, THEN each arm gets
an adapter with (a) a DECLARED mapping table — each entry `recorded-field →
arm-input` with per-entry provenance or a typed absence — and (b)
**missing-input refusal**: a record lacking a mapped field yields
`:q-actand/missing-input {:field ...}`, counted in the replay, never
defaulted, BECAUSE the reading map's conclusion is binding: channel mapping
and refusal are part of the adapter, and refusal counts are themselves a
comparison observable (an arm that refuses everything is a weak source, said
with numbers instead of silence). Scalar outputs from A/B are typed
`:scalar-awaiting-density` per §6 of DESIGN-c-vector — affine images of
log-Q_actand whose density has not been declared — so the comparison can run
on act-values now without pretending the scalars are densities.

## 5. The initial actand table — a named query, not an authored prior

**Query `:q-actand/calibration-v1`** (the named provenance-bearing query the
acceptance demands), `:arm-session` grain: over the 114 tracked calibration
sessions (`M-zaif-harness/calibration-sessions.edn`), group by (actand class
derived from the session's route + correction label, arm) and emit the
empirical outcome distribution of gold judgments per group, each density row
carrying `:record-ids` (the contributing session `:id`s, e.g.
`e-0cae94f2-9ca8-4863-9251-44278445a5f7`) and the query id. IF Z1 promised an
initial actand table with provenance, HOWEVER an authored table would be a
prior wearing a table's clothes, THEN the table IS this query's materialised
result — regenerable, provenance-bearing per row, refusing (not defaulting)
any group with zero support — BECAUSE arm C's design already established the
pattern (accreted query over existing carriers, never an authored prior) and
the same rule at arm-session grain is what makes the two indices one object.

## 6. Comparison replay (designed here, ruled nowhere)

Per arm, re-run D9's replay harness with the arm (through its adapter) as
the act-value source:

- **Corpora**: 114 calibration sessions + the 56-decision bounded snapshot
  (`:arm-session`); the 39 tracked U12 node fixtures across three tick runs
  (`:mission`, arms B/C when C exists).
- **HEADLINE — DISCRIMINATION**: count of distinct act-values produced
  across each corpus. Baselines to beat, named: wm U12 status-quo = 1
  distinct risk value over 133 actions across 14 channels; zaif status-quo =
  1 distinct act value (0.0) over 114/114 sessions. An arm that cannot beat
  1 on any corpus is recorded as too weak (DESIGN-c-vector §7's reversion
  clause, applied to a source instead of a flip).
- **Secondary columns**: refusal count by type (missing-input vs no-typed-
  source), provenance coverage (fraction of act-values carrying record-ids),
  and a planted sanity field: a fixture where all outcomes read satisfied
  must drive the arm's value to its floor.
- [A2a] **Ordering probe** (restored rider): distinct-count plus the floor
  plant is noise-fakeable — an arm emitting hash-noise beats "1 distinct"
  trivially. Per arm and corpus, plant two inputs differing in exactly one
  known-direction field and require the arm to ORDER them correctly; a
  pass/fail column beside the headline. Discrimination = distinct values AND
  correct ordering on the planted pair.
- [A2b] **Digest stability as a counted observable** (restored rider):
  determinism is not asserted in prose but counted per row — same
  `:inputs-digest` ⇒ same act-value; violations counted beside refusals.
- Deterministic from record fields alone; no live JVM reads.

## 7. Live-shaped demonstration (one, pinned, default off)

IF the acceptance requires one real nonzero belief term reaching act-value,
HOWEVER flipping the controller on is J-gated, THEN the demonstration is a
live-shaped test: feed `:actand-query-result` = one real
`:q-actand/calibration-v1` row (verbatim, record-ids cited) through
`task-belief-from` into a controller decision and pin act-value ≠ 0.0 —
against the before-pin `e-0f2f9aec-6240-40e9-a25a-e45d9452076f` (a real
decision whose task-belief is empty and act-value 0.0) — BECAUSE the pair
(before-pin all-zero, after-pin nonzero with provenance) is the minimal
honest evidence that the wire carries signal, without any live flip. Default
remains off; the flip rides the J-gate queue with its own census.

[A4] THE DECLARED DEMO BRIDGE (added after U11f's first run correctly
STOPPED on the wiring absence at zaif_inputs.clj:69-72 — the D8b gate
accepts only scalar-bearing results, and no density→scalar rule existed;
locating that absence was the run's valid outcome). Per DESIGN-c-vector §6
(a scalar payoff is an affine image of log-C at the outcome the action
targets), the demonstration's bridge is DECLARED here, every constant
visible:

    :act-value = ln(density(:gold-judged)) − ln(1/2)

Target outcome: `:gold-judged` (declared). Baseline: uniform over the two
declared observables (ln ½), so the scalar is positive exactly when the
actand's empirical gold rate beats a fair coin — for the pinned row,
ln(9/14) − ln(1/2) = ln(9/7) ≈ 0.2513. The bridge function lives beside the
table (zaif_actand.clj), carries the row's full provenance forward
UNCHANGED plus a `:bridge` key naming this declaration, and is
demo-scoped: U11e's comparison may replace it, and the arm A/B adapters
(U11c/d) do not inherit it. This is the first scalar in the system whose
density is declared rather than awaited — it graduates from
`:scalar-awaiting-density` by construction.

## 8. Not in v1

No arm ruling (the comparison produces numbers; Joe rules). No flip. No arm-C
build (S6(a)/wm U23 owns it). No registry edit by this lane — the §3 entry
lands via claude-1's wm row U33. No new persistence: the table materialises
from tracked records on demand.

[A3] Forward-pointer (not v1 work): when any arm's output eventually meets a
C inside one G, the U17 nonnegativity property and the
typed-absence-for-missing-ambiguity rule apply AT THAT COMPOSITION SEAM —
recorded here so clause (c) is not rediscovered there later.

## Build sequence after review (one file / one behaviour each)

1. **U11b** — `:q-actand/calibration-v1` query + materialised table + tests
   (pin: one real session id per density row cited).
2. **U11c** — arm A adapter (mapping table + refusals + tests).
3. **U11d** — arm B adapter (same shape, mission fixtures).
4. **U11e** — comparison replay runner + report (headline + columns above).
5. **U11f** — the §7 live-shaped demonstration test.
Arm C joins the comparison when wm U23 lands; its column reads
`:q-actand/no-typed-source` until then — a truthful hole, displayed.

## [A4-addendum] Measured at U11e (2026-09-03)

The real corpus contains two zero-gold groups (c-channel corrections 0/8,
actand-route 0/1) whose demo-bridge value is −∞ — 9 of 114 sessions. The
comparison reports this truthfully (produced 114, distinct 3, floor −∞).
Defense-in-depth verified at review: the D8b gate's finite-number? check
(zaif_inputs.clj:64-67) refuses non-finite act-values, so −∞ cannot reach
the live controller through hydration. Any post-demo bridge (U11e
replacement) must decide zero-support-for-target explicitly: smoothing,
or a typed refusal at the bridge. That decision rides the arm ruling.

## [A5-addendum] Corpus coverage of the U11e comparison (added at review, 2026-09-03)

§6 named three corpora; the runner replayed two. The committed report
(`runs/U11e-comparison-v1.edn`) carries
`:corpus-counts {:calibration-sessions 114, :mission-node-fixtures 39}` and no
column at all for the 56-decision bounded snapshot. Recorded here rather than
left as silence, since "an arm that refuses everything is a weak source, said
with numbers instead of silence" (§4) applies to corpora too:

- **Why it is absent.** The 56 decisions exist only behind D9's live query
  (`http://127.0.0.1:7073/api/alpha/evidence/text-search?tags=zaif&limit=100`,
  `runs/regenerate-D9-tie-order-count.bb`); no tracked file carries them. §6's
  last bullet forbids live JVM reads and `comparison-report`'s stated contract
  is "already-parsed tracked corpora", so the pure runner cannot reach that
  corpus without breaking the constraint that makes it replayable.
- **What its column would have contained, provable without the query.**
  `hydrate-inputs` returns exactly seven keys — `:mission`, `:mission-source`,
  `:gamma`, `:gamma-source`, `:task-belief`, `:c-belief`, `:observations`
  (`zaif_inputs.clj:246-252`) — and that is the shape each persisted
  `:inputs-snapshot` carries (see D9's `:live-pin :inputs`). None of arm A's
  seven source paths, none of arm B's four `[:channels …]` paths, and none of
  `:route`/`:is_correction`/`:gold_judged` is among them, so every cell of that
  column would read 56 typed `:q-actand/missing-input` refusals, 0 produced
  values, 0 distinct — it cannot move the discrimination headline in either
  direction. This is the same absence `zaif_arm_adapters.clj`'s
  `:zaif-decisions` entries already declare (basis
  `:zaif-inputs/closed-seven-key-shape`); what was missing was saying it where
  the comparison itself is read.
- **Not a substitution.** The 39 node fixtures stand in for §6's mission grain,
  not for the 56 decisions.
- **Scope of the [A2a]/plant columns, so the table is not misread.** The
  ordering probe and floor plant are per ARM, not per (arm, corpus): the same
  synthetic pair is reported inside both corpus cells of a column. A cell can
  therefore show `:ordering-probe {:status :pass}` in a corpus where the arm
  produced 0 values (arm A over the mission fixtures, e.g.). The probe values
  are visibly planted and no headline number depends on them, but a per-corpus
  plant is what §6 [A2a] asked for and is not what U11e built.
