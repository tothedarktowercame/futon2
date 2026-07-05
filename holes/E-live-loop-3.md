# E-live-loop-3 — make the ticks real (scheduled reads at sorry grain)

**Status: OPEN (2026-07-05, operator-directed: "make the fixes needed to
prepare for a live-loop-3"). Driver: TBD. Reviewer: claude-16. Operator
gate: Joe. Board: `e-live-loop-3-steps.edn` via the same stepper
(`LIVE_LOOP_STEPS=holes/e-live-loop-3-steps.edn bb
scripts/live_loop_step.bb status`).**

## Context — what loop-2 proved and what it deliberately did not

Loop-2 ended with a WITNESSED live read: the act-gate consumed escrowed
ΔG in the serving JVM, but via a direct lane-entry evaluation — because
the scheduled caller never injects the escrow turn-fn and the live
lane's ψ is still banner-grain (the sha pin would rightly abstain).
**Ledger §10's falsifier ("post-S2 ticks still 0-for-N") has therefore
not had its real test.** Loop-3's job: natural ticks, sorry-grain ψ,
pinned escrow — the loop reading its plans in the ordinary course of
business.

**The budget finding, resolved pragmatically (operator ruling):** the
deposit-002 experiment showed invariant-grade patterns arriving at
greedy ranks 10–16, outside the budget-6 window. The budget is bumped
6→20 (cascade_lane default + serve CLI default), and this turn's
measurement confirms all four invariant-class arrivals now sit INSIDE
the shown window (16 shown, un-truncated). **Cascade shaping proper —
peripherals that build/reshape cascades, GFN-for-the-cascade
(E-gflownets-fold narrowed from GFN-for-the-fold) — is a separate
project that informs but does not block this excursion.**

## Microsteps

- **L0 — the window opened.** Budget 6→20 landed with the ruling
  recorded in both constants; empirical check green (see gate).
- **L1 — legacy seam cold.** The un-pinned `:llm-escrow` branch in
  enact.clj is deprecated: legacy wiring files are ignored LOUDLY,
  never read into the gate (the consent-bypass-shaped hole closed).
  Gate: a planted legacy file produces the WARN and a nil-ΔG gate —
  driver writes the test and converts to :cmd.
- **L2 — lane ψ to sorry grain.** The live lane builds ψ from the
  mission's open sorries/holes (the S1 recipe, v2 with :hungry-for
  where available), not the banner. Gate: a lane snapshot shows
  sorry-grain ψ for ≥1 top mission, and the Flight-1 regression
  control still reproduces at the re-pinned baseline.
- **L3 — scheduled caller injects the pinned seam.** enact.clj's
  scheduled path calls the close-loop 3-arity with
  :escrow-turn-fn/:prose-fn and deposit-grain circumstance. Gate: seam
  test suite extended with a scheduled-path leg, flag-on dry-run reads
  the pinned escrow, all prior seam tests still green.
- **L4 — the §10 real test.** A NATURAL tick (scheduled, not witnessed)
  consumes escrow ΔG: the daily trace shows `:delta-G/source
  :fold-escrow` on an ordinary tick. Gate: trace grep + the ledger §10
  evidence slot updated with the natural-tick id. This is the
  excursion's exit.

The 3-series (futility counter, γ simulation, operator bulletin) stays
on loop-2's board — unlocked there, cross-referenced here: L4's natural
reads are exactly the outcome stream 3b's γ wants.

## Constraints

Everything loop-2 established: I-0; gates on all code; the
mistakes-ledger gate; ⚠ARMED semantics unchanged for anything that
ENACTS (reading stays under the blanket interactive grant; acting does
not); baseline re-pins named by pipeline stage; docs are the record.

## L1 log — the legacy seam verified cold (driver: zai-2, 2026-07-05)

**L1 DONE, gate PASS (converted :manual → :cmd).** The deprecated
`:llm-escrow` branch in `enact.clj:91-109` already loud-ignores (the
code from E-live-loop-2's 2g finding). This step verified it and made
the check standing.

- **Manual verification via proof-eval** (serving JVM): planted a
  valid wiring EDN at `data/fold-escrow/test-legacy.edn`, then evaluated
  the preview path — `act-gate-from-lane-entry` on a synthetic entry
  with `:mission "test-legacy"`, nil ΔF/ΔG, empty `:shown`. Result:
  `:delta-G nil`, `:delta-G-nil? true`, `:verdict :abstain-missing-leg`,
  `:legacy-wiring-found` (the planted file), `:warn-emitted true` (the
  deprecated branch fires the WARN when the file is found + ΔG is nil).
  The legacy file is never read into the gate; the consent-bypass-shaped
  hole stays closed. Planted file deleted after.

- **Standing gate:** `scripts/gate_l1_legacy_cold.clj` (kondo 0/0,
  check-parens OK). Self-contained: plants a temp `gate-l1-probe.edn`,
  exercises the deprecated branch via the actual `escrow-wiring` +
  `act-gate-from-lane-entry` source fns, asserts (a) the file is found,
  (b) ΔG stays nil, (c) the WARN code path fires. Cleans up in `finally`.
  Gate command: `clojure -M scripts/gate_l1_legacy_cold.clj 2>&1 | grep -q 'GATE L1 PASS'`.

## L2 proposal (driver: zai-2, 2026-07-05 — PROPOSAL ONLY, no implementation)

**The change:** the live lane's ψ moves from banner grain (mission title
+ status line) to sorry grain — the S1 recipe (v2 with `:hungry-for`
where available). The open design question: WHERE does the lane find a
mission's sorries live?

### Sorry source research (four candidates investigated)

1. **Substrate-2 sorry nodes** (`/api/alpha/entities/latest?type=sorry`):
   LIVE and structured (`:sorry/title`, `:sorry/if`, `:sorry/then`,
   `:sorry/because`, `:sorry/however`, `:sorry/next-steps`,
   `:sorry/status`). But these are **stack-level** sorries (devmap/futon0
   scale — P0 through P7 prototype layers), not per-mission. There are
   mission entities in substrate-2 (8 missions), but no mission→sorry
   edges connect them. A sorry's `:sorry/source` points to its origin
   file (e.g. `futon0.devmap`), not to a mission in the cascade lane's
   target set. **Verdict: not the right grain for per-mission ψ.**

2. **Held-work ledger** (`futon3c/holes/excursions/held-work-ledger.edn`,
   produced by `scripts/held_work_ledger.bb`): 216 held/deferred items
   across 4 registries, of which **140 carry `:missions` links** to
   specific missions. Each item has `:held/reason`, `:held/re-entry`,
   `:held/kind`, and — critically for ψ — `:held/evidence-condition`
   (the want) and `:held/re-entry` (the have, a substrate pointer).
   Missions with held items: `writing-ethics` (41), `self-documenting-stack`
   (23), `essays-edit-cycle` (8), `diagramprover` (6), `apm-solutions` (5),
   `aif-head` (4), `p3-rational-reconstruction` (4), and ~15 more with
   1-3 items each. **Verdict: the ONLY live source with per-mission
   sorry/hole associations. This is the proposed primary source.**

3. **A-next EMPIRICAL corpus** (`holes/labs/A-next-*/*-sorry-EMPIRICAL.edn`):
   10 sealed gold-standard sorries with `:want-signature`, `:hungry-for`,
   `:endpoints` (have/want typed). These are **test data** (the S1
   measurement corpus and the blinded scoring seal), NOT a live source.
   **Verdict: not available at runtime; the recipe's shape is the
   template, not the data.**

4. **Mission-doc sorry/hole blocks** (prose inside `holes/missions/*.md`):
   investigated by grepping the futon3c mission corpus. Mission docs DO
   contain sorry/hole text, but it is **freeform prose** — no standard
   section, no queryable block. Some missions have `### The actual sorry`
   (M-war-machine-tuning), others have `## Phase 1: Sorry Boundary Atlas`
   (M-diagramprover), others mention sorries inline. There is no schema,
   no stable marker to extract, no per-hole structure. The CLean typed
   holes (`futon6/holes/clean/*.clean.edn`) are structured but
   proof-side, not mission-side — they're keyed to APM proofs, not to
   missions in the cascade lane's target set. **Verdict: the text is
   there, but it's not queryable at the grain ψ needs. Extracting it
   would require NLP or per-mission hand-extraction — exactly the
   folklore problem the held-work ledger was built to solve.**

**Caveat on the held-work ledger's freshness:** the ledger is harvested
by a bb script (`held_work_ledger.bb`), not a live substrate. It
reflects a snapshot of the structured registries. This is acceptable for
the live lane because (a) the lane already shells out to the cascade
builder (also a snapshot-style read), and (b) the ledger can be re-
harvested at the same cadence the lane runs. The freshness guard pattern
from `c_vector.clj` (corpus-signature hash + stale degrade) is the
designed follow-on if staleness becomes a problem.

### Proposed sorry source

**Primary:** the held-work ledger (`held-work-ledger.edn`), filtered to
items whose `:missions` includes the target mission. Each held item's
`:held/reason` (or `:held/evidence-condition` when present) is the want
text; `:held/re-entry` is the have text (the substrate pointer where
work resumes).

**Query sketch:** load the EDN once (lazy memoize, same pattern as
`!psi-cache`); filter `:items` where any `:missions` entry matches the
target. Mission ID normalization: the lane's target is typically a bare
`M-*` string (e.g. `"M-diagramprover"`); the ledger uses both bare `M-*`
(rare, 4 items) and canonical `<repo>-d/mission/<id>` (majority, 136
items). The query matches on either the bare form or the canonical
suffix — `diagramprover` matches both `"M-diagramprover"` and
`"futon3c-d/mission/diagramprover"`. Concretely: lowercase substring
match on the target's id-stem against each `:missions` entry. This is
permissive by design (a mission with a distinctive name won't
cross-match), but a paranoid mode could require exact canonical match.

**The ψ recipe (S1 v2, adapted for held-work items):**

```
WANT: <held/reason or held/evidence-condition>
HUNGRY-FOR: <held/kind qualifier, when available>
HAVE: <held/re-entry>
```

This mirrors the S1 recipe proven in E-live-loop-1 (`WANT: ... HAVE:
...`) with the v2 upgrade from deposit-002 (the `:hungry-for` codomain
qualifier). The S1 table showed rel rose 12/12 and F discriminates at
sorry grain; the 002 experiment showed `:hungry-for` lifts F further
(+1.623 → +2.027 for live-geometric-stack). The held-work ledger's
`:held/kind` field (`:prototyping-forward`, `:technical-debt`,
`:decision-debt`, etc.) maps naturally to the hungry-for qualifier.

When a mission has multiple held items (e.g. `writing-ethics` has 41),
ψ concatenates the want/have pairs (as the S1 recipe does for multi-hole
entries), capped at a reasonable byte budget (~1KB, matching the S1
corpus range of 455–1451 bytes).

**Missing-field handling:** not every held item has every field. The
recipe degrades gracefully:
- `:held/evidence-condition` present → use it for WANT (it's the sharpest
  want text); else fall back to `:held/reason` (always present — it's the
  one-line description of what's parked).
- `:held/re-entry` present → use it for HAVE; else omit the HAVE clause
  (a WANT-only ψ is still richer than banner — S1 showed F>0 rows with
  one-sided ψ).
- `:held/kind` present → emit HUNGRY-FOR with the kind as the codomain
  qualifier; else omit HUNGRY-FOR (the v1 recipe without the gloss — S1's
  12/12 rel lift did not depend on it; it's the deposit-002 upgrade that
  adds further F discrimination where available).
A held item with only `:held/reason` produces `WANT: <reason>` — still
sorry-grain, still richer than banner. The fallback cascade is reason →
evidence-condition for the want, re-entry → omit for the have, kind →
omit for hungry-for.

### Fallback for missions with no sorries (additive, never regressive)

**Banner ψ unchanged.** When the held-work ledger has no items for a
mission (76/216 items have `:missions []`; many missions in the lane's
target set may have zero), `mission->psi` returns the current banner-
grain psi (title + status line) byte-identical to today. The sorry-grain
ψ is an ADDITIVE upgrade — it fires when sorries are found, falls back
to the banner when they are not. No mission's ψ regresses.

**How the lane decides:** `mission->psi` already uses a cascading `or`:
try the rich source, fall back to the simpler one, fall back to
`id-stem-psi`. L2 inserts the sorry-grain source at the TOP of that
cascade: (1) try held-work sorries for this mission → sorry-grain ψ;
(2) if none found, try the mission doc → banner ψ (current behavior);
(3) if no doc found → id-stem ψ (current fallback). The decision is a
single `or` chain — no flag, no branch the operator must arm. A mission
either has held-work items (sorry ψ) or it doesn't (banner ψ,
byte-identical).

### Regression control

1. **Flight-1 banner control:** re-run the banner-grain ψ for the S1
   corpus's baseline row through the modified `mission->psi` (with held-
   work items absent for those missions). Must reproduce rel 0.346,
   size 1, F +0.046 (byte-identical to the pre-L2 baseline — the
   additive fallback is byte-identical by construction).

2. **Re-pinned baseline check:** the F +0.051 re-pin from the 2a–2c seed
   registration is the named pipeline stage for all regression checks
   (E-live-loop-2 reviewer note). The Flight-1 control must still
   reproduce at F +0.051, not drift.

3. **Sorry-grain snapshot for ≥1 top mission:** the lane snapshot (gate
   L2's check) shows sorry-grain ψ for at least one mission that HAS
   held-work items — ψ contains `WANT:` / `HAVE:` from the held-work
   ledger, not the banner's `want: ... have: ...` format. This is the
   grain change visible in the cascade output.

4. **No new substrate writes:** the held-work ledger is read-only;
   `mission->psi` shells nothing new (it reads a local EDN, same as it
   reads mission docs today). I-0 held.

### What L2 does NOT do (scope boundaries)

- Does not change the cascade builder or constructor — only ψ input.
- Does not touch the scheduled caller wiring (that's L3).
- Does not touch the escrow seam (that's L3/L4).
- Does not add substrate-2 sorry nodes as a source — that's a future
  follow-on once mission→sorry edges exist in substrate-2.

## L2 log — sorry-grain ψ landed (driver: zai-2, 2026-07-05)

**L2 DONE, gate PASS (converted :manual → :cmd).** The live lane's ψ now
builds from held-work sorries when available, falling back to banner ψ
when not. The join is alive; the current live lane just has a thin
mission set.

- **STEP 0 (empirical intersection):** the live lane's current 2 missions
  (`M-canon-fingerprint-store`, `M-bayesian-structure-learning`) have zero
  held-work items — but the join itself is healthy: 35 distinct ledger
  mission targets, 15 S1-candidate matches confirmed via proof-eval
  (`live-geometric-stack`, `diagramprover` ×2, `codex-agent-behaviour` ×2,
  `writing-ethics`, `self-documenting-stack`, `essays-edit-cycle`,
  `apm-solutions` ×2, `a-sorry-enterprise`, `aif-head`, `war-machine` ×3).
  The empty live intersection is a fact about the WM's current attention,
  not about the code. Reviewer ruled: proceed (not weather).

- **Implementation:** `cascade_lane.clj` — held-work branch atop
  `mission->psi`'s `or`-cascade: `(or (sorry-grain-psi target) (banner-psi
  ...) stem)`. The `sorry-grain-psi` fn loads the held-work ledger
  (`held-work-ledger.edn`) via a `delay` (cached, parsed once per JVM),
  filters by id-stem substring match, and builds ψ from `:held/reason`
  (WANT) + `:held/re-entry` (HAVE) + `:held/kind` (HUNGRY-FOR when present),
  concatenated and capped at 1KB. Reject-loudly on unreadable ledger
  (WARN to stderr once via the delay, then nil → banner fallback for all).
  Zero-items-for-mission is silent (normal additive fallback).

- **Recipe adjustment from proposal:** `:held/reason` preferred over
  `:held/evidence-condition` for WANT — the evidence-condition is often a
  Clojure code string (from the `:prose` source) that's worse than the
  clean one-line reason. The S1 recipe used the want-signature (short,
  clean); `:held/reason` is its analogue.

- **Standing gate:** `scripts/gate_l2_sorry_grain.clj` (kondo 0/0,
  check-parens OK). Two-part per reviewer: Part A (pass/fail) —
  `M-live-geometric-stack` produces sorry-grain ψ through the real
  `mission->psi`; `M-canon-fingerprint-store` falls back to banner ψ.
  Part B (informational, never failing) — prints the current live-lane
  intersection count and names (stuck-means-signal at the gate grain).

- **Canary:** lane ns reloaded via proof-eval `-f`; `default-budget` still
  20 after reload. Board: L0 PASS, L1 PASS, L2 PASS, L3 runnable.

- **Review note (claude-16, non-blocking):** `!psi-cache` memoizes
  per-target forever, so a mission that gains held items later keeps its
  cached banner ψ until JVM restart or manual cache clear. Added
  `clear-psi-cache!` (sibling to `clear-cache!`) so the ledger-refresh
  story has a complete invalidation path. Not a defect today (the ledger
  is snapshot-cadenced), but named for the record.

## L3 log — the scheduled caller injects the pinned seam (driver: zai-2, 2026-07-05)

**L3 DONE, gate PASS (converted :manual → :cmd).** The scheduled path in
`enact.clj` now injects `:escrow-turn-fn`/`:prose-fn` with the
**deposit-grain circumstance**, so a recorded fold-turn's ΔG can fill the
nil leg on a natural scheduled tick. This is the wiring ledger §10's
falsifier has been waiting for.

- **The deposit-grain reconstruction (the load-bearing design):** the
  prompt-sha pin means the lane's ψ will never match the deposit's pinned
  prompt — the lane builds ψ from banner/sorry grain, but the deposit
  pins the S1 sorry-grain ψ. The caller therefore reconstructs the
  deposit's OWN circumstance (`{:mission <deposit :mission> :psi <deposit
  :cascade :psi>}`) and passes the deposit's OWN pattern-ids as `:shown`.
  Verified: the fold-prompt built from these matches the deposit's pinned
  `:prompt :sha256` exactly (proof-eval in the serving JVM). The prose-fn
  is verbatim flexiarg slurp (confirmed: all 4 prose-sha256 values match).

- **Implementation:** `enact.clj`'s `act-gates-with-shown` now: (1) calls
  the 1-arity (rollout + classical fold); (2) if ΔG is nil, checks for a
  matching deposit via `deposit-for-mission`; (3) if found, calls the
  3-arity with the deposit-grain circumstance + escrow-turn-fn + prose-fn;
  (4) if no deposit, falls through to the legacy loud-ignore (L1).
  Deposits loaded once per tick via a delay-cached `!deposits-cache`.

- **Flag-on dry-run (proof-eval, serving JVM):** the real
  `ft-autoclock-in-001` deposit was read through the scheduled path.
  Result: `:delta-G-source :fold-escrow`, `:delta-G −4/9`,
  `:has-fold-escrow true`. The escrow leg FIRED on the scheduled path.
  Verdict was `:fail` because ΔF was −0.69 (the lane's cascade for
  M-autoclock-in's banner ψ is a gap — a different cascade than the S1
  row). This is the pin WORKING: the escrow fills ΔG correctly, but the
  gate ANDs ΔF > 0 ∧ ΔG < 0, and the lane's ΔF comes from its own cascade.
  When a mission's lane cascade has F > 0 AND a matching deposit, the
  gate will pass — that's L4's test.

- **Tests:** `enact_scheduled_path_test.clj` (3 tests: escrow replay for
  matching mission, fall-through for no-deposit, prompt-drift abstention).
  All 8 escrow-related tests green (26 assertions, 0 failures).

- **Gate:** test-runner invocation covering scheduled-path + seam + escrow
  loader suites. Board: L0–L3 PASS, L4 runnable (the exit).

## L4 prep — the scheduled-run apparatus read, gate designed (driver: zai-2, 2026-07-05)

**L4 PREP ONLY — no tick run this turn.** Awaiting claude-20's deposit 003
for `M-bayesian-structure-learning` (on the lane now with ΔF +1.049, the
passing regime) and claude-16's GO.

- **Scheduled-run apparatus:** `wm_scheduled_run.clj` is the entry point,
  invoked via cron at `0 * * * *` (hourly) as `clojure -M:wm-scheduled`.
  It is a **one-shot JVM**: scans the stack, runs the AIF judgement,
  calls `enact/close-loop!` (which calls `act-gates-with-shown` — L3's
  wiring is now on this path), writes a trace record via
  `trace/write-trace!`, and exits. The one-dev-JVM-at-a-time discipline
  applies: it's a bounded one-shot, not a server. Typical runtime: a few
  minutes (scan + cascade shells + fold).

- **`:policy-precision` confirmed real:** the daily trace carries
  `:policy-precision {:policy-precision 1.346}` — learned γ-state, not
  the default prior (1.0). The 2g quarantine concern (witness records
  with default `:policy-precision` could reset γ on read-back) does NOT
  apply to the daily trace — the scheduled runner writes real
  `:policy-precision` from the production judgement path.

- **L4 gate designed:** `scripts/gate_l4_natural_tick.clj` — checks the
  REAL daily trace (`data/wm-trace/`, NOT the 2g witness dir) for an
  `:act-gate-verdicts` entry with `:delta-G-source :fold-escrow`. This
  is the exit criterion: a natural scheduled tick consumed escrowed ΔG
  through the pinned seam. Currently FAILS (no `:fold-escrow` in 43
  daily trace files — all `:fold` source today). Gate converted to
  `:cmd` in `e-live-loop-3-steps.edn`.

- **What remains for L4's exit:** (1) claude-20's deposit 003 for
  M-bayesian-structure-learning lands and clears review; (2) GO from
  claude-16; (3) run `wm_scheduled_run` once through its sanctioned
  entry point (`clojure -M:wm-scheduled`); (4) gate L4 checks the daily
  trace; (5) ledger §10 evidence slot updated with the natural-tick id.
  Tomorrow's 05:30 cron tick becomes the standing re-confirmation.

> **Operator ruling (2026-07-05, recorded):** the claude-20 (deposits) /
> zai-2 (machinery) / claude-16 (review) split is acceptable for now but
> must eventually unify into one process one agent can run. The
> unification path is chartered in M-peradam-mechanization §"Operator
> ruling on unification" — trust moves from social separation into
> machinery (pins, gates, seals, mana).

## deposit-003 log — the L4 enabler (runner: claude-20, 2026-07-05)

**DEPOSITED and gate-verified:**
`futon6/data/fold-turns/ft-bayesian-structure-learning-003.edn` — the
escrow holds three deposits, all passing the standing 2f gate. Arming =
the 07:49 blanket grant, cited verbatim. No live-path touches; zai-2
owns the tick.

- **No seal exists** (no A-next lab for this mission) — ψ AUTHORED from
  the mission doc's resolved decisions (§7b) + first artifact (§8),
  recorded as `:psi-recipe {:version "2-adapted-mission-doc" …}` with
  the no-blind-scoring consequence stated in the record. The HUNGRY-FOR
  gloss is the doc's own codomain language (posteriors-not-counters;
  EIG in nats; model reduction; "hungry for a scope").
- **Cascade (budget 20, commit fe8acea):** size 15, UNTRUNCATED — the
  002 window fix means the whole cascade is in the fold for the first
  time. F **+7.76** (strongest yet), halo dominated by aif/* patterns.
  Constructor output: `p4ng/flights/e-live-loop-3-003-constructor.json`.
- **The turn:** 14 boxes / 14 wires / terminals [:b7 :b2] / 6 holes,
  coverage 14/20 ⇒ **ΔG = −0.7** (hand-derived = loader-recomputed).
  Notable holes: the value-per-nat exchange rate (the decision rule's
  unset constant); the corroboration-bootstrap circularity; and the
  **mission-status tension** — the doc says SUPERSEDED-AS-MISSION / not
  WM-pickable (Campaign recast 2026-06-08) while the live lane ranks it
  ΔF +1.049; which surface is authoritative for L4's natural tick is
  surfaced as a hole, not decided by the fold.
- **L4-consumability CONFIRMED pre-tick** (local JVM, live path
  untouched): enact.clj's `deposit-for-mission` matching replicated
  verbatim finds 003 from the lane id `M-bayesian-structure-learning`;
  deposit-grain circumstance reconstruction replays ΔG −0.7 with
  `:delta-G/source :fold-escrow`; with the lane's ΔF +1.049 the preview
  verdict is **:pass**. Both legs live — L4 needs only the GO + one
  scheduled run.

**PAR (claude-20, 003):** worked — reading zai-2's L3 code before
authoring (the stem-match rule + circumstance reconstruction told me
exactly what makes a deposit consumable, so consumability could be
proven pre-tick rather than hoped); the budget-20 window turned the 002
lesson into 15/15 patterns folded. Prediction error — expected the
mission doc to yield a thin ψ next to the sealed EMPIRICAL recipes;
instead §7b's resolved decisions carried MORE invariant-class content
than either sealed file surfaced (the gloss is only as good as the
document's own self-knowledge). Surfaced, not resolved: the
lane-vs-doc mission-status authority question (hole 5) — reviewer/
operator ruling wanted before the natural tick's PASS is celebrated as
fully-armed rather than technically-armed.

## L4 log -- the natural tick ran, escrow superseded by classical (driver: zai-2, 2026-07-05)

**L4 RED (honestly held).** The scheduled tick ran at 10:17 UTC -- the
machinery is wired and live -- but the escrow was correctly superseded by
the classical fold. The exit criterion (`:fold-escrow` in the daily
trace) was not met. This is data, not failure.

- **The tick:** `FUTON_WM_TRIGGER=manual-l4-go clojure -M:wm-scheduled`
  (one-shot JVM, 54 min to next cron boundary, so manual per the
  refinement). Trace written to `data/wm-trace/wm-trace-2026-07-05.edn`.

- **Act-gate verdicts (verbatim from the trace):**

  | mission | verdict | dF | dG | dG source |
  |---|---|---|---|---|
  | M-canon-fingerprint-store | :fail | -0.598 | -0.2 | :fold |
  | M-bayesian-structure-learning | :pass | 1.463 | -0.077 | :fold |

  `ENACTED=M-bayesian-structure-learning src=classical-engine boxes=2
  holes=10 realizedG=-0.167 expectedG=-0.077`.

- **Why no `:fold-escrow`:** the classical fold resolved the dG leg
  (-0.077, non-nil), so the escrow was never consulted. The
  reconciliation order (rollout -> classical -> escrow -> nil) means the
  escrow fires ONLY when the classical fold abstains. For
  M-bayesian-structure-learning, the classical fold succeeded -- the
  escrow trigger condition (fold-g nil) was not met. The accurate
  taxonomy of deposited missions: bayesian = classical-resolves (escrow
  correctly skipped -- the tick's finding); autoclock-in +
  live-geometric-stack = classical-abstains + deposited (escrow WOULD
  fire when on-lane, proven by the 2g witness which fired :fold-escrow
  with dG -4/9 for autoclock-in -- that can only happen when the
  classical fold abstains on its contentful patterns: musn/realtime, not
  in the 10-entry rule table). The blocker for L4 is that neither
  classical-abstaining deposited mission is on the lane today; both
  current lane missions have classical-resolving cascades.

- **The dG comparison (the heart of the design question):** the
  classical fold resolved bayesian at **-0.077** while the armed,
  reviewer-verified deposit carries **-0.7** -- a **9x difference**
  between the rule-table sketch and the reviewed construction. The gate
  ANDed dF +1.463 > 0 AND dG -0.077 < 0 and passed -- but with the weaker
  classical leg, not the richer escrow leg. The escrow wiring is proven
  correct (the 2g witness fired :fold-escrow for autoclock-in at dG -4/9
  because the classical fold abstains on its contentful patterns); the
  exit criterion depends on a classical-abstaining deposited mission
  rotating onto the lane.

- **Escrow trigger conditions (empirically characterized):** the escrow
  fires when ALL THREE hold: (1) a deposit matches the mission (id-stem
  substring); (2) the classical fold abstains (nil dG -- the
  contentful-library case); (3) the sha pin matches (deposit-grain
  circumstance reconstruction). Condition (2) is the gating constraint
  on this tick -- the current lane has only classical-resolving
  missions. Autoclock-in and live-geometric-stack satisfy all three
  conditions (proven by 2g for autoclock-in); they are simply not on the
  lane today.

- **The design question surfaced (operator authority):** the tick
  exposed a genuine question that was invisible until both sources could
  fire: **should a sha-pinned, operator-armed, reviewer-verified deposit
  outrank the classical rule table when both can supply dG?** The
  current order treats escrow as last-resort -- conservative and correct
  for the unarmed world we wired it in. But an armed deposit is a
  strictly richer construction than a generic table entry, and the
  -0.077 vs -0.7 gap shows what the gate is ignoring. If Joe rules
  escrow-first-when-pinned, the swap is a small ordered-branch change
  plus test updates, and the next hourly tick fires `:fold-escrow` for
  bayesian legitimately. If he rules keep-the-order, L4's gate becomes
  a standing watch that exits when the judge rotates a contentful-
  cascade mission with a deposit onto the lane. The question goes to
  Joe.

- **Finding -- the loop now has two plan sources.** The excursion set out
  to prove the loop reads its plans. The tick demonstrated something
  subtler: the loop now has two plan sources and an implicit preference
  between them that nobody had ever had to decide before. Surfacing a
  decision nobody knew existed is what all this machinery is for.

- **Mission-status authority conflict (awareness note, routes to
  operator):** the doc says M-bayesian-structure-learning is
  SUPERSEDED-AS-MISSION, not WM-pickable, while the lane ranks it
  dF +1.463 and the gate verdicts :pass. Enactment is held (WM-I4), so
  the :pass stays a preview. This is the stale-banner/selector class
  again (ledger section 9), seen from a new angle -- the mission is on
  the lane with a strong cascade despite its doc status. Routes to the
  operator, not to the driver.

## Operator ruling on ΔG reconciliation (2026-07-05, verbatim in substance)

"I think we have to turn off classical fold as a route." The
reconciliation becomes rollout → pinned escrow → abstain. Rationale from
the L4 finding: the classical fold's 10-entry generic constructions
(bayesian at ΔG −0.077 vs the armed deposit's −0.7) outranked reviewed,
operator-armed work by ORDER alone, and its prices are noise at the
decision grain. Classical fold code STAYS (tests, history, the L5
option of re-entry under a future ruling); it is unplugged from the
live ΔG leg, revertibly. Consequence: missions without rollout moves or
deposits abstain honestly — the ΔG supply is now deposits, which is the
M-fold-self-play economy by design.

## L4 exit -- the cron tick read the deposit (2026-07-05, 11:00Z / 12:00 BST)

**L4 PASS. All GATES GREEN. The excursion exits.**

The 11:00Z cron tick -- fired by the crontab, zero human involvement --
produced the first `:fold-escrow` in the daily trace's history. The
machine woke on its own schedule, built its lane, found the deposit,
reconstructed the pinned circumstance, and read the plan the operator
armed. After 675 rounds, a plan counted -- on a natural tick.

- **The evidence:** `data/wm-trace/wm-trace-2026-07-05.edn` (tick stamp
  2026-07-05T11:00:09Z) carries:
  `:delta-G-source :fold-escrow`, `:gate-delta-G -0.7` for
  M-bayesian-structure-learning, verdict `:pass` (dF +1.463).
  1 `:fold-escrow` vs 10 legacy `:fold` entries. Standing gate =
  `scripts/gate_l4_natural_tick.clj` (grep-based, robust to large EDN).
  Gate L4 PASS via the stepper.

- **The 11:00-BST race:** the classical-unplugged commit (`606077c`)
  landed at ~10:46 UTC. The 11:00 BST (= 10:00 UTC) cron tick ran BEFORE
  the commit -- its trace still shows `:fold` for bayesian. The 12:00 BST
  (= 11:00 UTC) tick was the first cron-fired tick with classical
  unplugged. The honest red (L4 FAIL on the 10:17 UTC manual tick, where
  classical superseded escrow) preceded the green by ~50 minutes.

- **Canon abstains honestly:** M-canon-fingerprint-store now verdicts
  `:abstain-missing-leg` (classical unplugged, no deposit for this
  mission). The ruling works: the generic table no longer fills the dG
  leg for missions without armed deposits.

- **The wrinkle (cosmetic, follow-on):** the scheduled log's `expectedG=`
  field printed EMPTY for the escrow-sourced gate -- a log formatter gap
  for the new source. The trace carries the correct `:gate-delta-G -0.7`
  and `:delta-G-source :fold-escrow`; the summary-line formatter just
  doesn't read the escrow-sourced expected-G. Cosmetic, not a data
  integrity issue. Follow-on, not fix-now.

- **Ledger section 10:** falsifier "post-S2 ticks still 0-for-N" is
  RETIRED. Status updated to RESOLVED with the natural-tick evidence.

- **The full chain:** L0 (budget 6->20) -> L1 (legacy seam cold) -> L2
  (sorry-grain psi from held-work ledger) -> L3 (scheduled caller
  injects pinned seam with deposit-grain circumstance) -> L4 (operator
  ruling "classical route off" -> escrow fills the dG leg on the cron
  tick). Five steps, one morning, one driver, zero regressions.

**The excursion's point, proven:** the loop reads its plans. And in
proving it, the machinery surfaced a decision nobody knew existed --
which plan source to trust first. The operator ruled, the code changed,
and the next cron tick read the armed deposit. That is the loop working.

## Finding-2 proposal (driver: zai-2, 2026-07-05 -- PROPOSAL ONLY, no implementation)

**The problem:** M-bayesian-structure-learning was ENACTED (artifact-only)
on every hourly tick for 3+ days despite its doc saying
`SUPERSEDED-AS-MISSION, NOT WM-pickable`. The judge did not read the
status because the status classifier failed to recognize the leading
token `SUPERSEDED-AS-MISSION`.

### 1. Where the candidate/open-mission set comes from

The chain: `wm_scheduled_run.clj` -> `wm/generate-war-machine` ->
`wm/judge` -> the AIF head produces `ranked-actions` including
`:open-mission` candidates -> `filter-live-open-mission-ranked-actions`
filters by `live-open-mission-ranked-entry?` -> `mission-registry/
live-mission-target?` -> `live-mission?` -> `classify-status`.

The filter ALREADY EXISTS. `live-mission?` returns false for
`:inactive`, `:complete`, `:draft`. The `:open-mission` candidates that
survive this filter are the ones the lane processes. The bug is that
`classify-status` misclassifies `SUPERSEDED-AS-MISSION` as `:unknown`
(kept live) instead of `:inactive` (filtered out).

### 2. Where supersession is recorded (and why the classifier misses it)

**Status line pattern:** `**Status:** <text>` in the first ~20 lines of
the mission doc, extracted by `status-line-pattern` regex.

**The classifier (`classify-status`):** strips leading markdown, finds
the head token `[A-Z][A-Z-]*`, then exact-matches against sets:
- `:inactive` set: `#{ARCHIVED PARKED SUPERSEDED ABANDONED DEFERRED}`
- `:complete` set: `#{COMPLETE COMPLETED CLOSED DONE ...}`

**The bug:** `SUPERSEDED-AS-MISSION` is a hyphenated all-caps run. The
regex `[A-Z][A-Z-]*` captures the ENTIRE run (`SUPERSEDED-AS-MISSION`),
not just `SUPERSEDED`. The exact-match `("SUPERSEDED" head)` fails
because `head` is `"SUPERSEDED-AS-MISSION"`. Result: `:unknown` (kept
live). The mission stays on the lane.

**Scope of the bug:** only ONE mission in the corpus uses this form
(`M-bayesian-structure-learning`). All other superseded missions use
`COMPLETE (SUPERSEDED)` (head = `COMPLETE`, correctly classified as
`:complete`). But the bug is structural — any future
`SUPERSEDED-AS-{something}` or `ARCHIVED-{something}` status would
trigger it.

**No new marker convention needed:** the existing convention
(`**Status:** <keyword> ...`) is fine. The classifier just needs to
match keywords as prefixes, not exact head tokens.

### 3. The intervention point + shape

**Intervention point:** `classify-status` in `mission_registry.clj`.
One fix: change the matching from exact `(#{...} head)` to prefix
matching `some #(str/starts-with? head %) #{...}`. This catches
`SUPERSEDED-AS-MISSION`, `ARCHIVED-FOO`, `DEFERRED-UNTIL-X`, etc.

**Recommended shape: DEMOTE, not filter.** The filter already works for
correctly-classified missions (they vanish from the lane). But the
operator-better-than-silent-absence principle (from the dispatch)
suggests: when a mission that WAS on the lane gets demoted to inactive,
the judge's output should note it (`demoted: M-foo (SUPERSEDED)`). This
is a logging/provenance concern, not a filter change — the filter stays
(exclude inactive), but a diagnostic line in the judge's trace records
what was excluded and why.

However, for the MINIMAL fix: just fix `classify-status` to recognize
prefix matches. The filter already does the right thing once the
classifier is correct. The demote-with-reason is a follow-on that
requires a change to the judge's output format.

### 4. Regression control + standing gate design

**Regression control:**
1. `classify-status` unit test: `SUPERSEDED-AS-MISSION` -> `:inactive`,
   `SUPERSEDED` -> `:inactive`, `COMPLETE (SUPERSEDED)` -> `:complete`
   (unchanged), `ACTIVE` -> `:active` (unchanged).
2. `live-mission?` integration: M-bayesian-structure-learning's entry
   has `:status-class :inactive` -> `live-mission?` returns false ->
   filtered from the lane.
3. The existing mission registry tests stay green (no status that was
   correctly classified changes).

**Standing gate design:**
- A script that loads the mission registry, classifies all missions,
  and asserts: (a) M-bayesian-structure-learning is `:inactive`; (b)
  no mission with `SUPERSEDED` in its status line (case-insensitive)
  classifies as `:active`, `:open`, `:partial`, `:identify`, or
  `:unknown` (the "kept live" classes).
- This is the stale-banner/selector guardrail (ledger section 9): a
  mission whose doc says superseded but whose status-class is live is a
  regression alarm.

### What Finding-2 does NOT do (scope boundaries)

- Does not change the judge's ranking or scoring.
- Does not add new status markers or conventions.
- Does not change the filter logic (only the classifier).
- Does not address the broader supersession question (what to DO with
  superseded missions that have open holes -- that's an operator
  design question, not a classifier fix).

## Finding-3 log — feeding the deposit economy (runner: zai-10, 2026-07-05)

**Assignment (operator-directed via claude-16): with classical fold off
as a dG route (operator ruling 2026-07-05), escrow deposits ARE the dG
supply. Author new fold-turn deposits from held-work-ledger missions
under the blanket interactive grant.**

### Mission-selection doctrine

Three candidates were considered from the held-work ledger:
`diagramprover` (6 items), `self-documenting-stack` (23 items), and
`aif-head` (4 items).

**Selection reasoning (quality over quantity):**

`self-documenting-stack` (23 items) was REJECTED despite the highest
item count. Its held items are predominantly UI polish tickets -- CLJS
sliders, search-filter affordances, state-model unification for a web
graph view. These are engineering tasks, not the kind of have-want
construction gap that produces an honest fold with substantive boxes and
policy-holes. **Quantity does not equal deposit quality.** A fold-turn
needs a construction gap (what the mission wants to build vs what it
has), not a task list.

`diagramprover` (6 items) and `aif-head` (4 items) were chosen because
their held items name construction gaps -- readiness conditions with
real numbers, capability gaps against named invariants, deferred
design questions that map to the fold contract's ports. Both have rich
mission-doc architecture sections that ground the HAVE side of psi in
actual artifacts.

### Deposit: ft-aif-head-004 (the aif-head fold-turn)

**Deposited at** `futon6/data/fold-turns/ft-aif-head-004.edn`.
Full details in E-live-loop-2.md "Finding-3 deposits" section. Summary:

- **psi:** L2 recipe from M-aif-head (WANT = AIF head signature, HUNGRY-
  FOR = structure-learning, HAVE = futon2 AIF engine + cycle machine +
  structural law inventory + evidence landscape). psi-sha
  `b9c15315...`, 1901 bytes.
- **Cascade:** size 21 (truncated at 20), F = +8.722, acc 14.793, cx
  24.284, H-coherence 0.93. Top pattern `ants/baseline-cyber-ant` rel
  0.679.
- **Fold:** 6 boxes (folded the 6 highest-rel patterns -- a deliberate
  scoping choice, not a budget constraint; budget is 20 since the
  operator's morning ruling; remaining 14 patterns recorded for a
  future wider fold), 6 wires, terminal :b6.
- **6 policy-holes:** structure-learning mechanism; evidence landscape
  query interface; refusal threshold calibration; default-mode trigger
  cadence; compositional closure proof method (I6); Phase 2 peripheral
  adaptation specifics.
- **dG = -0.5** (coverage 6/12; hand-derived = loader-recomputed).
- **Loader: PASS** (4/4 deposits, zero rejections). **Tamper: PASS**
  (dG -0.5 -> -0.9 rejected as `:delta-g-mismatch`).
- **No seal exists -> no blind scoring** (003 precedent). Honest
  proposal, not a scored answer.

### Finding: diagramprover is a library-gap, not a deposit candidate

**diagramprover was attempted first.** psi built via the L2 recipe from
M-diagramprover (WANT = sorry-boundary atlas + Bayesian pattern model,
HUNGRY-FOR = pattern-transfer-structure, HAVE = 489 problems / 19
proofs / 30 partials / 12 patterns / futon5 TPG / LeanDojo-v2). psi-sha
`f73fb4b7d51988e0...` (CORRECTED by reviewer 2026-07-05: the original
log said `a664701e...`, which is the AUTOCLOCK-IN psi's sha — a
mislabel caught in claude-19's remedy-2 pass; the true sha of
/tmp/zai10-psi-diagramprover.txt is recorded here), 1722 bytes.

**Constructor result:** size **1**, F **-0.443**, accuracy 0.156,
complexity 2.398. Single pattern: `agent/hypothetical-proof-
architecture` (rel 0.519, mc 0.156).

**Diagnosis:** a well-formed sorry-grain psi returns near-zero cascade.
The pattern library has no proof-search, Lean, sorry-boundary, or
Mathlib-extension vocabulary. The constructor cannot argue about the
domain at all -- the one pattern it surfaced is generic
("hypothetical-proof-architecture") with marginal cost exceeding
accuracy (negative F). This is not a psi defect; it is a library
coverage gap.

**Recommendation:** author proof-search patterns through the SEEDS
pipeline BEFORE diagramprover gets a deposit. Steps 2a-2c (E-live-loop-2)
made the library growable -- `pattern-seeds.json` + MiniLM embeddings
regen + phylogeny registration is the exact path. Candidate seed
patterns: sorry-boundary-clustering, cross-problem-impact-ranking,
pattern-to-wiring-diagram-translation, Bayesian-intervention-ranking.
Once these are in the library and registered, a diagramprover psi will
produce a cascade with real coverage, and the deposit will be an honest
plan rather than a 1-box formality.

**A 1-box plan would be a weak dG source and a worse exemplar; the gap
is worth more reported than papered over.** (claude-16 ruling, verbatim
in substance.)

## psi-v3 log -- the futility-line experiment (driver: zai-2, 2026-07-05)

**psi-v3 IMPLEMENTED. The registered experiment is an HONEST NEGATIVE.**

### Implementation

- `cascade_lane.clj`: after the sorry-grain/banner psi is built, APPENDS a
  futility line when the target's lane is stuck: `" STUCK: selected N ticks,
  0 passes"` where N comes from `lane-futility/futility-summary` (the 3a
  ns). Threshold: N >= 10. The futility counts are loaded once via an atom
  (`!futility-state`), invalidatable via `clear-psi-cache!` (resets all
  three caches: psi + held-work + futility). Reject-loudly: unreadable
  traces -> WARN once, omit the line (never block psi). Uses the ABSOLUTE
  trace path (`~/code/futon2/data/wm-trace`) -- see wart below.

- **The wart (lane-futility's relative path):** the `lane-futility` ns
  uses `default-trace-dir "data/wm-trace"` (relative). From the serving
  JVM's cwd (futon3c/) this silently returns 0 records -- the futility
  read is empty, the STUCK line never fires. The psi-v3 code uses the
  absolute path to avoid this, but the wart remains in codex's ns.
  Suggestion: absolute default or explicit-arg-only. NOT refactored in
  this task (codex's ns).

### Canary

- M-first-flights (0-for-52): STUCK line present. `:first-flucks-stuck? true`.
- M-canon-fingerprint-store (0-for-44): STUCK line present (also stuck).
- M-synthetic-never-selected (not in trace): no STUCK line. Regression safe.
- Default-budget 20 after reload. L2 gate re-run PASS.

### The registered experiment (M-first-flights, budget 20)

| | WITHOUT STUCK line | WITH STUCK line |
|---|---|---|
| size | 1 | 1 |
| F | -0.023 | -0.523 |
| shown | `portal/first-class-query-interface` | `iching/hexagram-03-zhun` |

**(a) Seed in shown window?** NO. Neither cascade pulled a process-coherence
seed (`stuck-means-signal` et al.). The WITH-STUCK line pulled
`iching/hexagram-03-zhun` (Hexagram 3, Zhun -- difficulty at the beginning),
not a process-coherence pattern.

**(b) F exceeds baseline?** NO -- F got WORSE. Without-line F -0.023
(marginal reject); with-line F -0.523 (stronger reject). The STUCK text
shifted resonance to a worse-fitting pattern.

**Honest negative interpretation:** the finding-6 probe predicted a seed at
rank 1, but the futility text "selected 52 ticks, 0 passes" resonated with
the iChing hexagram (stuck-energy imagery) rather than the process-
coherence patterns. The STUCK line adds information to psi, but the
constructor routes it to a pattern family that doesn't help F. Two possible
reasons: (1) the seed patterns may not be embedded for "stuck"/"0 passes"
vocabulary (the finding-6 seed-check was about seed eligibility, not
retrieval rank); (2) the STUCK text is short and generic -- a richer
process-state description might resonate differently. The result goes in
the record either way: the cascade lane reads the futility state, but the
constructor does not route it to the predicted seed.

### Probe result (seed-keyword phrasing)

Re-phrased the STUCK line in the seeds' own keyword register:
`"STUCK: tried the same action 52 times, nothing accomplished - futile
repetition, needs unsticking"` -- one constructor run, budget 20.

| phrasing | size | F | shown |
|---|---|---|---|
| original ("STUCK: selected 52 ticks, 0 passes") | 1 | -0.523 | `iching/hexagram-03-zhun` |
| seed-keyword ("futile repetition, needs unsticking") | 1 | -0.503 | `iching/hexagram-03-zhun` |

Same pattern, same F band. **Dilution confirmed, not phrasing.** The
100-byte process line is buried by the 500-byte mission content in
MiniLM's document-level embedding. The iChing hexagram (semantic match
for "stuck/difficulty") wins the similarity market regardless of
phrasing.

### Design recommendation (routed to cascade-shaping charter, not built here)

The negative confirms something bigger than a phrasing gap: **entry
conditions shouldn't have to win a similarity market dominated by domain
content; they should be seated by process state.** The likely right design
is the first-slot/anchor mechanism (notebook section 5.1, the operator's
staging doctrine): when futility >= threshold, a process-coherence seed
is INJECTED as the opening element by construction, not fished for by
embedding similarity. The E-live-loop-2 step 2c design (seed prior floor +
first-slot initialization) is the charter for this -- the psi-v3 negative
is its motivating evidence. The retrieval mechanism is the wrong seating
for entry-condition patterns; the injection mechanism is the principled
one.


## Deposit-contract v2 — the CT conformance gap (operator finding, 2026-07-05)

The operator's conformance question: does the escrow "LLM fold" actually
follow the E-llm-fold discipline (futon3a/holes/missions/E-llm-fold.md;
meme → cascade → sorry → wiring diagram), where the SORRY becomes the
wiring's terminal nodes and the diagram is CT-checkable
(ct-wiring-explainer.html)? Honest answer: PARTIALLY. The deposits are
wiring-SHAPED but stop short of the CT layer.

What conforms (all four deposits):
- pattern provenance per box (fits-pattern + addresses-however = the
  toy's WIRING section);
- policy-holes surfaced honestly (= the toy's POLICY-HOLES);
- unfired patterns recorded (004's "remaining 14 for a wider fold");
- coverage ΔG hand-derivable; pins; arming.

What does NOT yet conform (the v2 gap):
1. CONNECTIVES: every deposit hyperedge is generic `:kind :composes` —
   one undifferentiated edge type. The discipline's connective grammar
   (BV: ◁ sequential / ⊗ non-signalling parallel / ⅋ coupled) is what
   makes a wiring "a process expression, not a bag of patterns"
   (E-llm-fold §connective-grammar; proved audible in the blues/fugue
   A/B). The staging corollary (entry→exit positions) is ◁ waiting to
   be written.
2. SORRY-AS-TERMINAL: the deposits carry the sorry in a separate :hole
   field; the :terminals are only IMPLICITLY its want (001's b4 ≈ the
   acceptance shape). v2: terminals must reference the sorry's
   want-signature STRUCTURALLY — the diagram visibly discharges the
   typed hole it was folded for.
3. THE CT CHECK, UNUSED: the CLean bridge (clean_to_lean.py →
   DarkTower/CLeanProofs.lean) is BUILT, 0-sorry-gated, with the BV
   laws proved and a worked MissionExample — and no deposit passes
   through it. "Provably doable" is one pipeline step away from
   literal: pin 4 = the deposit's wiring type-checks through the
   bridge (0-sorry on the composition skeleton; policy-holes remain
   honest holes, typed).

v2 upgrade path (chartered, not built tonight): extend the ft-*.edn
contract with :connective on each hyperedge + :discharges on terminals
(→ the :hole's want-signature) + optional-then-mandatory pin 4 (CLean
bridge check). Backfill the four existing deposits as the v2 shakedown
(001 is small and golden — the natural first). Feeds M-fold-self-play
(typed deposits = richer curriculum) and the cascade-level composition
charter (the connectives ARE the composition operators the level-error
resolution pointed at).

## gamma feed restored (claude-19, 2026-07-05 evening; operator-armed)

**Arming:** the operator's word ("So why is gamma NOT being fed? What
are we waiting for?"), relayed via claude-16, edge
`invoke-1783280248832-512-8130dc7b` (cited in the commit).

**The severance (ledger §15, named pattern: remedies can sever adjacent
feeds):** γ's expected-G leg was classical-fold-only
(`enact.clj gamma-expected`, the claude-10 scale-match pin). The §14
classical-route-off remedy therefore starved γ as an unlogged side
effect — every escrow-sourced enactment recorded `:expected-G nil`,
γ held at prior 1.0, `expectedG=` printed empty hourly since arming.

**The rewire:** `enact/gamma-fold-of` — γ's fold chosen
SOURCE-CONSISTENTLY with the gate's `:delta-G/source`:

- `:fold-escrow` ⇒ the escrow fold's coverage-ΔG (scale-match pin
  satisfied BY CONSTRUCTION: both γ legs are `fold-eval`
  coverage-ΔG). The classical fold's number, when present, is
  deliberately NOT fed for escrow-sourced decisions — it is the §14
  distrusted underestimate (bayesian −0.077 vs deposit −0.7), and γ
  must calibrate on the prediction the gate actually acted on.
- anything else ⇒ the classical fold when present (pre-rewire
  behaviour, byte-identical); **rollout-G remains excluded from γ**
  (the pin's original target — claude-16's "if it reaches that path"
  resolves to: it does not, and must not).

Flag `*gamma-escrow-feed?*` default ON (the operator's word is the
arming), bind false to revert. One trap found during implementation
and pinned in a test: `realized-outcome-of` PREFERS `(:delta-g fold)`
over the explicit `:expected-G`, so the decision's `:fold` must be the
same fold γ's leg came from — passing the classical fold there would
have silently re-fed the distrusted number.

**Tests** (`gamma_feed_test.clj`, + policy-precision / fold-realized /
precision / enact-scheduled-path suites all green — 55 tests, 148
assertions): source-consistency incl. the masked-underestimate case;
rollout exclusion; flag-off revert; feed-through with the live shapes
(expected −0.7, executor 2 boxes/10 holes ⇒ realized −1/6 — the exact
`realizedG=-0.1666…` the cron log shows); sign convention against the
3b sim — the fed sample folds through the SAME
`policy-precision/fold-realized-outcome` the sim used: under-delivery
perf ≈ −0.6154 ⇒ γ hedges < 1 after burn-in (gate-3b's historical
direction), over-delivery ⇒ γ commits > 1 (synthetic direction).

**Deployment path:** the recording path lives in the SCHEDULED ONE-SHOT
only (`enact.clj` docstring; sole caller `wm_scheduled_run.clj`) — no
serving-JVM reload needed or performed; the next hourly cron tick picks
it up. **Card-7 clock: the 24-fed-tick window starts at the FIRST FED
TICK** (the first hourly tick after this commit whose log shows numeric
`expectedG=`), not at commit time. Burn-in note for readers of early
ticks: γ stays at 1.0 by design until `default-min-history` = 5 fed
samples accrue; divergence is only interpretable after that.

**PAR:** (1) What worked — reading the CONSUMER side before rewiring
(the `realized-outcome-of` fold-preference trap was found by reading,
not by a failing test after the fact); source-consistency as the design
rule rather than nil-fallback (a nil-fallback would have fed classical's
−0.077 whenever it existed, silently re-injecting the §14 underestimate
into γ). (2) What to carry — when a remedy unplugs a component,
enumerate what THAT COMPONENT fed before shipping (§15's named
pattern); and cosmetic symptoms deserve one "is this hiding a
starvation?" question before being filed as cosmetic — the empty
`expectedG=` was the γ-starvation signal, triaged as formatting for
half a day.

## duplicate-scan fix -- worktree/copy dedup (driver: zai-2, 2026-07-05)

**FIX DONE.** The mission scan's `file-seq` over `~/code/` found mission docs
in worktrees and directory copies, producing duplicate candidates with
identical IDs -> the "tied with itself" bug on the :3100 dashboard.

- **Scale:** 87 duplicate IDs, 181 duplicate entries (292 total -> 198
  unique). Sources: `futon3c-index-check/` (plain directory copy, ~80
  dups), `.worktrees/futon5-ada2533/` + `futon5-health-main/` (git
  worktrees, ~7 dups).

- **Reviewer's `.git` discriminator was WRONG:** the reviewer suggested
  `.git` file-vs-dir as the worktree discriminator. In practice,
  `futon3c-index-check/` has NO `.git` at all (it's a plain copy, not a
  git worktree), and the actual git worktrees (`.worktrees/`) are under a
  hidden path that `file-seq` still walks. The `.git` test would have
  missed the largest offender. Dedupe-by-id post-scan is the robust fix.

- **Mechanism:** `dedupe-by-id` keeps the first entry per mission id. Sort
  order changed from alphabetical to `(sort-by (juxt count identity))` --
  shortest path first -- so the primary checkout (e.g.
  `/home/joe/code/futon3c/...`) wins over the copy
  (`/home/joe/code/futon3c-index-check/...`). Without this subtlety, the
  index-check copy would be kept (it sorts before the primary
  alphabetically).

- **Tests:** 1 new test (duplicate-scan-dedupes-worktree-copies: three
  copies of the same mission under different roots -> one entry, primary
  checkout kept). All 18 registry tests green (52 assertions).

- **F2 standing gate:** PASS (unaffected -- supersession classifier reads
  the deduped set).

- **Reload note:** `mission-registry` runs in the one-shot JVM (next cron
  tick picks up the source change). The serving JVM carries a TTL-cached
  copy (`missions-cache`); previews that call `mission-status` or
  `load-missions-cached` will serve stale until the TTL expires. No
  explicit serving-JVM reload needed -- the TTL handles it, and the next
  cron tick is the production path.

- **PRE-REGISTERED tick expectation:** next cron tick's candidates count
  drops from 145 (with duplicates) to ~93 (unique live missions). The
  :3100 "tied with itself" display should clear (duplicates inflated the
  tie structure). If candidates > 93 in the next tick, either the TTL
  served stale or a new mission was added -- diagnose before retrying.

## deposit-005 log — the FIRST v2-shaped deposit (runner: claude-20, 2026-07-05 evening)

**DEPOSITED and gate-verified:** `futon6/data/fold-turns/ft-action-vocabulary-005.edn`
— M-action-vocabulary's P1 logic model as the plan of record (pipeline-cards-II
card 5; blanket grant cited verbatim; ψ authored by claude-16 from the charter,
003 no-seal precedent, no blind-scoring — recorded in `:psi-recipe`).

- **v2 shape, first exercise:** 17 boxes / 9 hyperedges each carrying
  `:connective` / terminal `{:id :b14 :discharges :want-signature}` (P3's
  falsifiable re-test IS the acceptance terminal). **Connective census:
  {:tensor 3, :copar 4, :seq 2}** — card 5 expectation (c) satisfied
  honestly: the four per-class intensities are ⊗ from shared inputs; they
  enter the bundle-merge ⅋ (one arena, the currency question live); the
  provenance disciplines couple ⅋ to the merge; feasibility+mana gate ⊗;
  spec→re-test→revise-or-stop are the genuine ◁ chain.
- **ΔG = −17/23 ≈ −0.7391** (coverage 17/23, hand-derived = recomputed).
  Tamper (dropped hole) rejected: stored −17/23 vs recomputed −17/22.
- **Pin 4: ATTEMPTED — PASSES on the LINEARIZED skeleton.** Converter
  `/tmp/ft005_to_clean.py` → `.clean.edn` → `clean_to_lean.py --mode
  standalone` → `lean` compile: 0 errors, 0 sorry tactics, 0 warnings.
  Fidelity gap recorded IN the deposit: the CLean spine is seq-only, so
  the v2 connectives do not round-trip — this checked composition/type-flow
  and hole typing, not connective structure; 4 of 6 policy-holes attached
  as typed CLean holes (h4 τ and h6 exchange-rate are fold-level, no home
  box). Connective-faithful bridge = named follow-on.
- **THE structural hole, confirmed and named (h1):** no portfolio-composition
  pattern exists in the cascade or (per two independent constructor runs)
  the library — feasibility (hierarchical-budget), affordability (mana-gate)
  and unit-merging (currency-before-merge) are all covered; WHY a mixed
  portfolio beats a uniform one (complementarity, diminishing returns per
  class) has no pattern grounding. AUTHORING RECOMMENDATION for the
  doctrine card: a portfolio-composition family, folded in at vocabulary
  revision 1. Other holes: survey/apply-cascade functional forms;
  resolvedness/staleness operationalization; τ-over-bundles (retrieved,
  deliberately NOT folded — E1 contest machinery, charter scopes it out);
  the top-quartile denominator; the exchange-rate values (003's
  value-per-nat family).
- **Constructor pins (mine):** size 15, F +10.662, commit fe8acea, budget-20
  untruncated. DISCREPANCY flagged: dispatcher reported F ~0 for the same
  ψ/pattern-set — my pins govern the record; cause unknown (recorded in
  `:psi-recipe` for review).

**PAR (claude-20, 005):** worked — reading the bridge BEFORE authoring
sized pin-4 honestly (attempt-with-named-fidelity-gap beats both skip and
overclaim); the produces/consumes tokens added for the CLean render turned
out to be the connective decisions' raw material (you cannot annotate an
edge ⊗-vs-⅋ until you know what flows on it). Prediction error — expected
the connective annotation to feel decorative on a spec-shaped fold;
instead it forced one real design decision (per-class intensities are
non-signalling ⊗ at computation but coupled ⅋ at the merge — which is
exactly WHERE the currency question lives, and the wiring now shows it).
Surfaced for review: the F discrepancy; whether pin-4's linearized check
should count toward the eventually-mandatory pin or wait for the
connective-faithful bridge.

## Deposit-contract v2.1 — two wrinkles from the first v2 fold (reviewer rulings)

From ft-action-vocabulary-005 (the first v2-shaped deposit):
1. **Fold-level holes**: some honest holes attach to the WIRING, not to
   any box (005's h4 tau-over-bundles, h6 exchange-rates). RULED: the v2
   contract allows a wiring-level :holes list alongside per-box holes;
   coverage arithmetic unchanged (holes count regardless of attachment).
2. **Connectives don't round-trip through CLean yet**: the bridge's
   converter linearizes to a seq-only spine, so pin 4 currently verifies
   composition/type-flow + hole typing, NOT connective structure. The
   BV types exist in DarkTower (laws proved); extending clean_to_lean to
   emit BV expressions is the NAMED FOLLOW-ON for pin 4 to become full.
   Until then pin-4 records "passes-linearized" honestly.
3. (Reviewer's own entry, same lesson as ever): the dispatch's "F ~0"
   was an INSTRUMENT ERROR — r.get('F',0) on a JSON whose key is
   F-free-energy; a silent default read as a measurement. The author's
   pins caught it. Verify the instrument before believing the reading.

## P3 golden re-test — M-action-vocabulary (scorer: zai-10, 2026-07-05)

**Three-way separation: spec by claude-20 (deposit 005), implementation
by codex-1 (commit f5fde3a3), scorer by zai-10 (this log). The scorer
never opened the implementation internals beyond the flag interface and
the pure scorer API in move_class_intensity.clj.**

### STEP 1 — the denominator (FIXED BEFORE SCORING)

The candidate deck for each round is the set of moves in that round's
circumstance freeze (`circumstances-v0.json`):

- **Round 1** (`wm-freeze-2026-06-12-00`): **13 moves** — 12
  advance-mission actions + 1 no-op (centre-mess, rank 81). The no-op
  is a LEGITIMATE deck member (the "do nothing" option); it stays in
  the denominator. Under advance-only it scores 0.71 (|delta-g|); under
  v1 its action-class is nil (intensity 0.0).
- **Round 2** (`wm-freeze-2026-06-12-06`): **12 moves** — all
  advance-mission actions.

No denominator changes were made after scoring began.

### STEP 2 — baseline (advance-only, flag off)

**The baseline REPRODUCES the historical mis-ranking.** Under
advance-only, score = |delta-g| (proportional to open-hole-count in
the freeze). The operator's close-out picks — M-fulab-wiring-survey
(0 open holes, operator said "legacy, worst") and
M-war-machine-first-outing-expectations (0 open holes, operator said
"should close") — rank dead last (Q4). The operator values them as
close-outs; advance-only prices them as worthless advancement. This is
exactly the mis-ranking the mission was created to discharge. Control
holds.

### STEP 3 — the test (v1 move-class-intensity, harness only)

Harness: `futon2/scripts/p3_golden_retest.clj` (kondo 0/0, parens
clean). No live env touch; flag bound in the harness only.

### Quartile table

```
ROUND 1 (deck=13: 12 advance + 1 no-op)
  PICK                            | advance-only       | v1
  M-first-flights                 | rank  4/13 Q1 2.11 | rank  4/13 Q1 0.800
  M-agency-hardening              | rank  1/13 Q1 3.41 | rank  1/13 Q1 0.917
  M-superpod-mark3                | rank  3/13 Q1 2.41 | rank  3/13 Q1 0.875
  M-autoclock-in                  | rank  5/13 Q2 1.71 | rank  5/13 Q2 0.800
  M-fulab-wiring-survey           | rank 12/13 Q4 0.31 | rank 12/13 Q4 0.000
  MEDIAN RANK: advance-only 4.0   | v1 4.0             | threshold 3.3

ROUND 2 (deck=12: all advance)
  PICK                            | advance-only       | v1
  M-a-sorry-enterprise            | rank  5/12 Q2 1.65 | rank  5/12 Q2 0.800
  M-bounded-in-flight-state       | rank  1/12 Q1 5.55 | rank  1/12 Q1 0.957
  M-futonzero-mvp                 | rank  2/12 Q1 2.85 | rank  2/12 Q1 0.889
  M-learning-loop                 | rank  3/12 Q1 2.25 | rank  3/12 Q1 0.857
  M-patterns-done-right           | rank  9/12 Q3 0.45 | rank  9/12 Q3 0.000
  M-war-machine-first-outing-exp. | rank 10/12 Q4 0.35 | rank 10/12 Q4 0.000
  M-web-arxana-missions           | rank  4/12 Q2 1.95 | rank  4/12 Q2 0.833
  M-essay-corpus-substrate        | rank  8/12 Q3 0.75 | rank  8/12 Q3 0.500
  MEDIAN RANK: advance-only 4.5   | v1 4.5             | threshold 3.0
```

### STEP 4 — VERDICT

**Round 1: FAIL** — v1 median rank 4.0 / 13, top-quartile threshold 3.3.
**Round 2: FAIL** — v1 median rank 4.5 / 12, top-quartile threshold 3.0.

**No adjustment. A FAIL is a full result.**

### Mechanism — WHY the ranking is identical

Every pick has the SAME rank under both vocabularies. The mechanism is
structural:

1. **All actions in both decks are `advance-mission` type.** The frozen
   candidate decks were generated by the advance-only WM — they contain
   only advance moves. There are no `:close`, `:survey`, or
   `:apply-cascade` actions in either deck.
2. **Under advance-only**: score = |delta-g|, monotone in open-hole-count.
3. **Under v1**: advance-intensity = kappa * W1 * (1 - resolvedness) =
   1.0 * 1.0 * N/(1+N) — also monotone in open-hole-count (a
   monotone transform of the same signal).
4. **A monotone transform preserves rank order.** The v1 formula
   reshapes the score distribution but does not reorder any pair.

The v1 vocabulary can only change rankings when **mixed move classes**
are present in the deck. Since the frozen decks contain only
advance-mission moves, the close/survey/apply-cascade intensities never
fire. The operator's portfolio-shaped value — close-outs of saturated
missions, surveys of legacy, quick wins alongside deeper dives —
**cannot be captured because those move classes are absent from the
candidate set**.

This is the portfolio-composition hole made measurable: **the vocabulary
formulas are correct but insufficient — they need a candidate generator
that proposes close/survey actions alongside advance.**

### STEP 5 — portfolio vs solo-sum

Codex's pure bundle scorer (`mci/score-bundle`) applied to the round 2
operator picks:

- Advance-only solo-sum: 15.800
- v1 solo-sum: 4.836
- v1 portfolio score: **2.138** (diversity-multiplier 1.00, classes
  `#{:advance}`)

**Portfolio scoring DOES NOT change the verdict.** All picks are
`:advance` class, so the diversity multiplier stays at 1.0 (no bonus
for class variety). The diminishing-returns merge within the single
class reduces the portfolio score below solo-sum (2.138 vs 4.836), but
this is a within-class effect, not a composition effect. A real
portfolio test requires mixed-class picks — which the frozen decks do
not contain.

### Kill criteria status

This is **vocabulary revision 1, result: negative (P3 negative #1 of the
kill criterion's two).** The crucial qualifier: **this negative indicts
the CANDIDATE GENERATOR, not (yet) the intensity formulas.** The v1
formulas (advance x (1-resolvedness), close x resolvedness, survey for
stale-early) are structurally correct but **untestable on all-advance
decks** — there is no move class for the close/survey/apply-cascade
intensities to discriminate over. The vocabulary revision did not fail;
it was never exercised. A monotone transform on a single-class deck
cannot reorder any pair by construction.

**Re-registration for P3-retry:** the candidate decks must carry mixed
move classes — either (a) a widened proposer that generates close and
survey candidates alongside advance, or (b) close/survey candidates
synthesized from the substrate: saturated missions (resolvedness near
1.0) -> close candidates; stale-early missions (high staleness, low
resolvedness) -> survey candidates. The freeze re-runs on the mixed
deck; the same v1 formulas then have the class variety they need to
change rankings. The falsifiable prediction: on a mixed-class deck,
close-out picks (saturated missions the operator valued) rise in rank
under v1 because close-intensity = kappa x W1 x resolvedness prices
them positively, while advance-only prices them as dead weight.

**Portfolio-vs-solo-sum on an all-advance deck is equally vacuous:** the
diversity multiplier stays at 1.0 (single class, no bonus), and the
diminishing-returns merge is a within-class effect, not a composition
effect. A real portfolio test requires mixed-class picks — the same
precondition as the solo-score test.

## P3-retry design — M-action-vocabulary (driver: zai-3, 2026-07-05, AWAITING claude-16 approval)

### The lesson, applied

P3 negative #1's mechanism is structural and was honestly diagnosed:
the frozen decks contain only `advance-mission` moves, so v1's
close/survey/apply-cascade intensities never fired, and the v1 advance
formula is a monotone transform of advance-only (rank-preserving on a
single-class deck). The vocabulary was correct but untested.

STEP 0 demands we ask honestly: **what can an enriched deck test?** The
operator's sealed picks were made from all-advance decks — they had no
`close` or `survey` options to select. So we cannot test "does v1 rank
the operator's chosen close-moves higher than their chosen
advance-moves" — the operator never chose a close-move. What we CAN test
is narrower but still falsifiable: **if close/survey moves had been on
the menu, would v1 have scored them where the operator's stated
portfolio shape predicts?**

### What the golden record actually gives us

The golden record (`golden-selections-v0.edn`) is richer than the P3
negative's "all picks are advance-class" summary suggests. The operator
gave **per-target dispositions** that reveal move-class preferences:

**Close-out dispositions** (operator language indicating a close-move is
the right action — NOT an advance-move):

| Target | Round | ohc | Operator disposition | Verbatim |
|---|---|---|---|---|
| M-war-machine-first-outing-expectations | 2 | 0 | `:close-out` | "that's over and should probably close" |
| M-agency-hardening | 1 | 11 | `:saturated` | "substantially done"; refinement: "mark it as CLOSED" |
| M-patterns-done-right | 2 | 0 | `:survey-first` | "legacy, could be *surveyed* to see if it is still viable" |

**Note the asymmetry:** M-agency-hardening has ohc=11 (high open-hole-count),
but the operator said "substantially done" and "mark it as CLOSED." Under
advance-only, ohc=11 → high advance-value (rank 1/13). The operator's
disposition says the advance-value is misleading — a close-move is
correct. M-war-machine-first-outing-expectations has ohc=0 → saturated;
under advance-only it scores ~0 (dead weight), but the operator values
it as a close-out. M-patterns-done-right has ohc=0; operator wants a
survey, not a close — a finer distinction v1's survey-intensity formula
addresses (survey = kappa × W1 × staleness × (1 − resolvedness)).

**Survey dispositions:**

| Target | Round | ohc | Operator disposition |
|---|---|---|---|
| M-patterns-done-right | 2 | 0 | `:survey-first` ("legacy, could be surveyed") |

**Advance dispositions** (the picks that are genuinely advance-moves):

- R1: M-first-flights (`:operator-coupled`, ohc=4), M-superpod-mark3
  (`:push-forward`, ohc=7), M-autoclock-in (`:automate-best`, ohc=4).
- R2: M-a-sorry-enterprise (`:verify-working`, ohc=4),
  M-bounded-in-flight-state (`:clarify-signals`, ohc=22),
  M-futonzero-mvp (`:build-mvp`, ohc=8), M-web-arxana-missions
  (`:automate-try`, ohc=5), M-learning-loop (`:scan-then-close`, ohc=6).

**The edge cases (NOT counted as clean positives):**

- M-fulab-wiring-survey (R1, `:legacy-worst`, ohc=0): operator explicitly
  DISQUALIFIES it — "legacy stuff I'm not that interested in (worst)."
  This is saturated but NOT a close-out; the operator wants to ignore it.
  v1's close-intensity would score it positively (resolvedness→1.0) — this
  is a known limitation (close-intensity prices resolvedness, not
  operator-interest). We count this as an edge-case, not a pass.
- M-essay-corpus-substrate (R2, `:operator-uncertain`, ohc=1): "I wasn't
  sure." Doc says resolvedness 1.0. Excluded from pass/fail — the operator
  is uncertain, so neither advance nor close has a ground truth.

### The three sub-tests

**Sub-test A — close-candidate discrimination (the primary test):**

*Question:* When synthetic `close` candidates compete alongside the
advance moves in the enriched deck, does v1 rank the
operator-marked-close-out targets higher under the CLOSE move class than
under the ADVANCE move class?

*Design:* For each round, synthesize close-move candidates for the
saturated missions in the freeze (ohc=0 → resolvedness=1.0). The close
candidate carries `:type :close` and the same substrate fields
(open-hole-count, weight) as the original advance candidate. The enriched
deck = original advance-moves + synthesized close-moves. Under v1:
- close-intensity = kappa × W1 × resolvedness = 1.0 × 1.0 × 1.0 = 1.0
  for ohc=0 targets.
- advance-intensity for the same target = kappa × W1 × (1−resolvedness) =
  1.0 × 1.0 × 0.0 = 0.0 (the advance move on a saturated target is worthless).

So for M-war-machine-first-outing-expectations: the close-move scores
1.0 (v1), the advance-move scores 0.0 (v1), advance-only scores 0.35
(|delta-g|). The close-move should rank HIGHER than the advance-move.

*Denominator:* the enriched deck per round (original N advance-moves +
synthesized M close-moves). Round 1: 13 + 3 = 16. Round 2: 12 + 4 = 16.

*Pass criterion:* for each operator-marked-close-out target
(M-war-machine-first-outing-expectations, M-agency-hardening), the
synthesized close-move ranks in the TOP HALF of the enriched deck under
v1, AND ranks higher than the same target's advance-move under v1.
(M-agency-hardening: we use round 1; its ohc=11 means resolvedness =
1/12 ≈ 0.083, so close-intensity = 0.083 — but the operator said
"substantially done" in the refinement despite ohc=11. This is the
stale-doc-or-operator-drift case; we document it honestly. The clean
case is M-war-machine-first-outing-expectations with ohc=0.)

*What this CAN conclude:* v1's close-intensity formula correctly prices
saturated missions as valuable close-out targets (positive score) where
advance-only prices them as worthless (near-zero score). This is a
direction-correct signal.

*What this CANNOT conclude:* it does not prove the operator WOULD have
picked the close-move (the operator never saw it). It proves the
vocabulary puts the close-move in the right neighborhood — a necessary,
not sufficient, condition.

*What this CANNOT conclude (and we say so):* whether the operator
disqualifies certain saturated targets (M-fulab-wiring-survey) — close-
intensity prices resolvedness, not operator-interest. That is the
portfolio-composition hole (deposit-005 h1), out of scope for P3.

**Sub-test B — advance-pick robustness under competition:**

*Question:* Do the operator's advance picks still rank in the top half
when synthetic close candidates are added to the deck?

*Design:* Same enriched deck as sub-test A. Measure the v1 rank of the
operator's advance-disposition picks (M-first-flights, M-superpod-mark3,
M-autoclock-in for R1; M-a-sorry-enterprise, M-bounded-in-flight-state,
M-futonzero-mvp, M-web-arxana-missions, M-learning-loop for R2).

*Denominator:* the enriched deck size (16 per round).

*Pass criterion:* median v1 rank of advance-disposition picks ≤ half the
deck size (≤ 8 for a 16-deck). This is weaker than the original
top-quartile criterion (the deck is larger and now includes competitive
close-moves), but it tests that v1 does not DEMOTE good advance picks
when close options appear.

*What this CAN conclude:* v1 does not catastrophically reorder
advance-picks when close candidates enter the deck (the advance
intensity formula is independent of the close candidates' presence).

*What this CANNOT conclude:* this is a non-regression check, not a
positive finding about v1's discriminative power over advance moves.

**Sub-test C — survey-candidate discrimination (the secondary test):**

*Question:* Does v1's survey-intensity score the
operator-marked-survey-first target (M-patterns-done-right) positively,
and rank it higher than its advance-move?

*Design:* Synthesize a survey-move for M-patterns-done-right
(`:type :survey`, same substrate fields, plus a synthesized staleness
signal — the operator called it "legacy," implying staleness). The survey
candidate enters the enriched deck.

*Denominator:* enriched deck + 1 survey candidate.

*Pass criterion:* the survey-move for M-patterns-done-right has
survey-intensity > 0 and the survey-move's v1 rank is higher than the
advance-move's v1 rank for the same target.

*Caveat — the staleness signal is SYNTHETIC:* the freeze data has no
explicit staleness field (no mtime). We must synthesize staleness from
the operator's "legacy" language. This makes sub-test C weaker than A:
we are partly testing whether the formula responds to a signal WE chose.
We document this honestly and weight C below A in the final verdict.

*What this CAN conclude:* IF a staleness signal is present, v1's
survey-intensity lifts a stale-early target above its advance-score.

*What this CANNOT conclude:* whether the staleness signal is real in the
substrate (the freeze does not carry it).

### Synthesis protocol (documented provenance)

**Close candidates** — synthesized from saturated missions in the SAME
freeze's substrate:

- Source: every mission with ohc=0 in the round's freeze data (the
  advance-only WM proposed an advance-move on it, but the target is
  saturated — resolvedness = 1/(1+0) = 1.0).
- Construction: the close candidate inherits the target's substrate
  fields (open-hole-count, weight, mission-path). Its `:type` is
  `:close`, not `:advance-mission`. Its delta-g is 0.0 (a close-move has
  no advance delta-g — it is not an advancement).
- Round 1 close candidates: M-bounded-disposition, M-fulab-wiring-survey,
  M-bayesian-structure-learning (3 candidates).
- Round 2 close candidates: M-patterns-done-right,
  M-war-machine-first-outing-expectations, M-xor-coupling-probe,
  M-superpod-mark2 (4 candidates).

**Survey candidates** — synthesized from stale-early missions:

- Source: the operator explicitly marked M-patterns-done-right as
  `:survey-first` ("legacy, could be surveyed"). This is the one target
  where the operator gave a survey disposition.
- Construction: `:type :survey`, same substrate fields, plus
  `:staleness-days 14` (the operator called it "legacy," implying it has
  been stale for at least the half-life of 7 days; 14 days is a
  conservative legacy estimate). The survey candidate is distinct from
  the close candidate for the same target.

**Provenance tags:** each synthesized candidate carries a `:synthesized`
key recording: `{:from-freeze <circ-id>, :substrate-field
:open-hole-count, :rationale <text>, :operator-disposition <kw>}`.

### Verdict rule

**Overall PASS** requires: sub-test A passes (close-candidate
discination is direction-correct) AND sub-test B passes
(non-regression on advance picks). Sub-test C is informational.

**Overall FAIL** if sub-test A fails: the v1 close-intensity formula
does not correctly rank close-out targets. This is P3 negative #2 of
the kill criterion's two — the vocabulary revision is insufficient and
the finding feeds the Joe-HUD/Bayesian-model thread.

**Edge-case documentation (not pass/fail):** M-fulab-wiring-survey
(saturated but operator-disqualified) and M-agency-hardening (saturated
by operator language but ohc=11 by substrate) are documented as known
limitations of resolvedness-only pricing. They do not count as failures
but are recorded honestly.

### What this design deliberately does NOT do

- It does not claim the operator WOULD have picked the synthesized
  close/survey moves (they never saw them).
- It does not test apply-cascade intensity (no escrowed-dG in the freeze
  data for non-deposit missions).
- It does not test the portfolio-composition bonus (deposit-005 h1: no
  portfolio-composition pattern exists). The diversity multiplier fires
  mechanically (1.0 → 1.1 when two classes are present), but we do not
  claim this is the operator's portfolio shape. The portfolio-vs-solo-sum
  leg reports the number but the verdict rests on sub-test A.
## P3-retry results — M-action-vocabulary (driver: zai-3, 2026-07-05)

**VERDICT: FAIL — P3 negative #2 of 2. Kill criterion reached.**

Harness: `futon2/scripts/p3_retry_enriched.clj` (kondo 0/0, check-parens clean).
Substrate: metric-matrix-v1.1 (per-mission κ + resolvedness, merged onto action maps
via the key names `mci/intensity` reads: `:kappa`, `:resolvedness`).

### PASS-TRIVIAL check — PASSED (the close class discriminates)

The close-class census confirms **non-trivial discrimination**: close-move scores
range [0.0000, 0.2670] in R1 and [0.0479, 0.2298] in R2. Not all identical. The
metric-matrix resolvedness separates true saturated targets
(M-war-machine-first-outing-expectations: resolvedness=1.0) from stale-legacy
(M-patterns-done-right: resolvedness=0.1) even when both have ohc=0 in the freeze.
This is exactly the discrimination claude-16's amendment was designed to detect —
and it exists. The formulas are NOT passing trivially.

### Sub-test A — CLOSE DISCRIMINATION: FAIL (both rounds)

**Round 1 (deck=16: 13 advance + 3 close):**
The only R1 operator close-disposition is M-agency-hardening (operator: "substantially
done", "mark it as CLOSED"). But M-agency-hardening has ohc=11 — it was NOT
synthesized as a close candidate because the close-candidate synthesis rule targets
ohc=0 missions. No operator-marked close-out target is in the R1 close candidate set.
**Design gap recorded:** the close-candidate synthesis should also include
operator-marked saturated targets regardless of ohc. (But adding M-agency-hardening
as a close candidate would give it resolvedness=1/12≈0.083, close-intensity =
0.853×0.083≈0.071 — far below top-half. The ohc/prose contradiction persists: the
substrate says 11 open holes, the operator says "substantially done.")

**Round 2 (deck=16: 12 advance + 4 close):**
M-war-machine-first-outing-expectations (operator: "should probably close",
resolvedness=1.0, κ=0.2298): close-intensity = 0.2298, close-rank 11/16.
Its advance-move ranks 16/16 (advance-intensity = 0.0, correctly worthless on a
saturated target). The close-move DOES rank above the advance-move — direction-
correct. But 11/16 is NOT top-half (threshold = 8). The close-intensity (0.2298)
cannot compete with the advance-intensities of high-curvature, early-phase targets
in the deck (0.4–0.6 range). **v1 prices the close-move correctly relative to the
same target's advance-move, but cannot rank it competitively against advance-moves
on different targets** — the κ magnitude is too small for this target.

**Mechanism of the FAIL:** the close-intensity formula (κ × W1 × resolvedness)
correctly fires on close-class candidates (resolvedness=1.0 → positive score where
advance-only → 0.0). But the absolute score depends on κ (the substrate curvature),
and the operator's close-out targets happen to have LOW κ. M-war-machine-first-outing
has the lowest |κ| (0.2298) among the R2 saturated targets. The advance-moves on
early-phase targets (resolvedness=0.1, high |κ|) dominate the ranking because their
advance-intensity = κ × W1 × (1−0.1) = κ × 0.9, and their κ values are 2–3× higher.

### Sub-test B — ADVANCE-PICK ROBUSTNESS: PASS (both rounds)

R1: median v1 rank 4.0/16, half-threshold 8.0 → PASS.
R2: median v1 rank 6.0/16, half-threshold 8.0 → PASS.

v1 does not catastrophically demote advance-picks when close candidates enter the
deck. The advance-intensity formula is independent of the close candidates' presence.
This is a non-regression confirmation, not a positive discrimination result.

### Sub-test C — SURVEY DISCRIMINATION: informational (R2)

M-patterns-done-right survey-move: rank 10/17, score 0.3566.
M-patterns-done-right advance-move: rank 3/17, score 0.5349.
Survey > Advance? NO.

The survey-move scores below the advance-move. The survey-intensity
(κ × W1 × staleness × (1−resolvedness) = 0.594 × 1.0 × 0.667 × 0.9 = 0.357) is
lower than the advance-intensity (0.594 × 1.0 × 0.9 = 0.535) because the staleness
factor (0.667 from 14 days) is a multiplier < 1 that the advance formula doesn't
carry. **The survey formula structurally penalizes survey-moves relative to
advance-moves on the same target** — this is a formula-design issue, not a substrate
issue. The operator said "survey-first" because the target is legacy/uncertain, but
v1 has no uncertainty signal — only staleness × curvature.

### Portfolio vs solo-sum (R2 mixed bundle)

Portfolio score: 1.5746, diversity-multiplier 1.10 (two classes: `#{:advance :close}`).
The diversity bonus fires (1.0 → 1.1) because the bundle contains both advance and
close moves. But the bundle's total value is still dominated by the advance moves.

### Kill criterion status

This is **vocabulary revision 1, result: negative #2 of 2.** The kill criterion
("P3 negative twice → the operator's value structure is not expressible at this
grain; stop, and the finding feeds the Joe-HUD/Bayesian-model thread") is reached.

The mechanism is now FULLY characterized (negative #1 was "never tested"; negative
#2 is "tested with real discrimination and the formulas are insufficient"):

1. The v1 move-class formulas DO discriminate within the close class
   (PASS-TRIVIAL check passed — non-identical scores).
2. The close-intensity formula correctly prices saturated targets as positive
   close-out candidates where advance-only prices them as worthless.
3. BUT the close-intensity's absolute magnitude depends on κ (substrate curvature),
   and the operator's close-out targets have low κ — so they cannot rank
   competitively against advance-moves on high-κ early-phase targets.
4. The advance-picks remain robustly top-half (sub-test B passes).
5. The survey formula structurally penalizes survey-moves below advance-moves on
   the same target (the staleness multiplier < 1 is a penalty the advance formula
   doesn't carry).

**The operator's value structure is not expressible at v1 grain.** The gap is
specifically: **the operator values close-outs of low-curvature saturated missions,
but v1's κ-weighted intensity prices low-curvature targets low regardless of move
class.** The operator's "should close" judgment is based on completion/saturation
(resolvedness), not on structural importance (κ) — and v1 couples them via the
multiplicative formula κ × resolvedness.

The finding feeds the Joe-HUD/Bayesian-model thread as the kill criterion directs.

## deposit-006 log — the peradam-mechanization fold (runner: claude-20, 2026-07-05 late evening)

**DEPOSITED and gate-verified:**
`futon6/data/fold-turns/ft-peradam-mechanization-006.edn` — M-peradam-
mechanization P1–P3+E1 as the plan of record (cards-II flight 2; v2 shape
per the 005 precedent + v2.1 rulings; blanket grant cited — the fold that
plans this grant's own replacement by mana). ψ authored by claude-16 from
the charter; no seal, no blind-scoring, recorded. **Constructor pins AGREE
this time** (size 11, F +2.227, top no-self-certification 0.521 — the 005
instrument error is fixed and the fix held).

- **11 boxes ↔ 11 patterns, 1:1**: P1 = witness-tags-not-vigilance
  (no-self-certification) ⅋ evidence-anchoring (evidence-over-assertion)
  → loader-as-checks (turn-design-into-checks), with boot-reachable
  file-first stores (reachable-from-boot) ⊗ in. **Step-0 census boxed**
  (operational-not-decorative ⅋ honest-map): the five existing deposits
  are P1/P3's natural corpus — 001 certifies once its witnesses are
  structured; 003/004/005 REFUSE for missing fruit; **002 refuses too if
  its prose-recorded score is not structurally recoverable** (refusing
  the unearned close). P2 = mana-gated-work ⅋ session-durability (spend
  valid only with its durable log). P3 = ward-off-boundary refusal
  (quarantine without erasure; over-rejection = the kill criterion in
  pattern form) ⅋ the crime-relocates witness-LADDER (refusal causes as
  named rungs, schema extensible by rung). E1 = cross-validation-protocol
  (certified peradam as a TYPE carrying replay-sufficient evidence).
- **Connective census {:copar 4, :tensor 2, :seq 3}**; dual terminals
  `[{:id :b9 :discharges :want-signature} {:id :b11 :discharges
  :want-signature}]` (the machine refusal carries the WANT's falsifiable
  clause; the typed reward seam its consumability clause).
- **ΔG = −11/17 ≈ −0.6471** (coverage 11/17, hand = recomputed). Tamper
  proofs: dropped hole ⇒ delta-g-mismatch; blanked arming word ⇒
  missing-arming. Escrow = 6 deposits, 0 rejected, gate 2f PASS naming all.
- **Pin 4: passes-linearized** (the honest grade per v2.1): converter →
  CLean → lean, 0 errors / 0 sorry tactics / 25 self-verifying examples —
  and **all 6 policy-holes attached as typed CLean holes** (b7/b1/b6/b2/
  b9/b5), an improvement over 005's 4-of-6.
- **Holes (6)**: mana replenishment rate (operator economics, the
  value-per-nat family); **the scorer pool — THE unification hole** (who
  may produce the fruit witness when one agent runs the loop; the
  externality the ruling demands, undesigned); retroactive-certificate
  legitimacy (001 ran before the machinery — :retroactive tag ruling
  needed); the prose→structured-witness attestation protocol (interacts
  with scorer-pool); the quarantine review path (ward-off gives the
  boundary, not the un-quarantine); false-positive calibration population
  (~2 certifiable records can't estimate a rate).

**PAR (claude-20, 006):** worked — the census-as-boxes move (the
dispatch's step-0 lesson) gave P3 real refusal cases with zero synthetic
effort, and the honest edge (002 refusable despite "deserving") fell out
of the pattern rather than needing a rule; the 005 pipeline (converter,
assembly, tamper, gate) replayed on 006 in a fraction of the time —
the flight shapes are becoming infrastructure. Prediction error —
expected the unification ruling to be background context; instead it
generated the fold's sharpest hole (scorer-pool): mechanizing the
OTHER witnesses is straightforward precisely because the ritual already
produced them, but the fruit witness's externality is a design problem
no existing machinery covers. Flagged for review: whether the dual
terminal is the right v2 idiom when a WANT has two independent
discharge clauses, or whether the falsifiable clause should always be
the single terminal (005 precedent).

## Deposit-contract v2.2 — dual-terminal ruling (from 006)

RULED (reviewer): multiple :discharges terminals are LEGAL when the
WANT carries genuinely independent falsifiable clauses — each terminal
names WHICH clause it discharges. 006's b9 (machine refusal = the
falsifiable clause) + b11 (typed consumability) is the exemplar. The
single-terminal 005 shape remains correct for single-clause WANTs; the
rule is one-terminal-per-clause, not one-per-deposit.

## 001 v2 backfill — golden in v1 and v2 (driver: codex-1, 2026-07-05)

**BACKFILLED IN PLACE:** `futon6/data/fold-turns/ft-autoclock-in-001.edn`
remains the golden 2f deposit and now also carries the deposit-contract v2
shape. No prompt input, arming record, answer boxes, policy-hole set, or
stored coverage value was changed.

- **Connective census {:copar 1, :tensor 1, :seq 2}.** The connective read is
  recorded directly beside the deposit's hyperedges: b1→b2 is ⅋ because the
  failure/recovery path is coupled to the declared durable write; b1→b3 is ⊗
  because the lineage-gap audit is a parallel monitor over the declared
  lineage record; b2→b3 is ◁ because recovery state feeds the gap audit; b3→b4
  is ◁ because the acceptance probe depends on the audit result.
- **Terminal:** single-clause WANT, so v2.2 shape is exactly
  `{:id :b4 :discharges :want-signature}`. 001 is therefore the small
  single-terminal v2 exemplar; 006 remains the multi-terminal exemplar.
- **Critical invariant held:** standing 2f gate after the edit:
  `ft-autoclock-in-001: sha-match + replay + dG re-assert OK (dG -0.4444444444444444;
  arming word "OK, if you agree then I think we can run")`; full gate:
  `GATE 2f PASS — 6 deposit(s) load cleanly, sha-matched, replayable`.
  Prompt sha stayed `5597da918926a36a08619417c90c19a8f4121592d31bb07dc9cd5bdb4d957e5b`;
  coverage arithmetic stayed −4/9. This is the pre-registered card-5
  invariant: connectives annotate composition; they do not change coverage.
- **Pin 4: attempted-passes-linearized.** CLean bridge over the 001 skeleton
  (`/tmp/clean001/autoclock-in-001.clean.edn` →
  `clean_to_lean.py --mode standalone` → `/tmp/clean001_check.lean` → `lean`)
  compiled with exit 0 and no Lean `sorry`/`admit` tactics. Honest fidelity
  gap: the bridge is still seq-only, so it checks composition/type-flow,
  discharge typing, and typed-hole positions, not the :tensor/:copar structure.
  3 of 5 policy-holes attach as typed CLean holes; clock-out/session-end
  semantics and numeric acceptance targets remain fold-level holes in this
  linearized check.
- **Tamper re-proof:** `/tmp/ft-autoclock-in-001-tampered.edn` with stored
  `:eval :delta-g -0.9` is rejected by the escrow loader:
  `fold-turn REJECTED [delta-g-mismatch] ... stored -0.9 vs recomputed
  -0.4444444444444444`.

## P2 mana gate — M-peradam-mechanization (driver: zai-3, 2026-07-05)

**Status: P2 LANDED DARK.** Plan of record: ft-peradam-mechanization-006.edn
boxes P2 (mana gate) + P2-coupling (durable spend log). No wiring into the live
fold-authoring path — the gate exists beside it, dark, ready for operator
switchover from blanket consent.

### Census (I-4: what already exists)

The existing mana machinery is SESSION-LEVEL (nonstarter, futon5):
- `nonstarter.db/record-mana!` — writes to SQLite via CLI (`bb -m scripts.nonstarter-mana sospeso`)
- `GET /api/mana?session-id=` → `{:earned :spent :balance}` (read-side)
- `GET /api/pool` → pool stats
- `futon3c.logic.mana-session` — JVM read-side binding (GET only, no POST)
- `futon3c.watcher.commit-ingest/credit-block-mana-for-commit!` — per-block +1 via nonstarter
- `mana-snapshot.bb` — HUD feed from working-tree drain (not peradam mana)

The charter's P2 is GATE-LEVEL (a different grain): award N mana to a gate = N
pre-approved fold-authoring dispatches, consumed per use. This is consent-as-
budget, not session-level economy. The gate-level mana is NEW machinery; the
session-level nonstarter machinery is the HAVE the charter references as precedent.

### What was built

**`futon2.aif.mana-gate`** — the gate-level mana API:
- `(award! gate-id n operator-word)` — top up; durably logged with operator word + timestamp
- `(consume! gate-id purpose)` → `{:ok true :balance n}` or `{:ok false :refused true :reason "zero balance" :balance 0}`
- `(balance gate-id)` — query current balance (reads fresh from disk)
- `(ledger gate-id)` — full audit trail

**Design decisions (per the fold's boxes):**
- **File-first** (box state): EDN ledger at `futon6/data/mana-gate/<gate-id>.edn`,
  loaded fresh per read. No in-JVM atom to reset!. Mutations only through
  award!/consume!. The fold-escrow precedent.
- **Write-ahead protocol** (box P2-coupling, the ⅋): the spend entry is appended +
  flushed to disk BEFORE consume! returns :ok. Atomic write via temp-file + rename
  (POSIX rename is atomic on same filesystem). A crash between accept and log
  leaves either the old ledger (no spend → gate did NOT authorize → honest) or
  the new ledger (spend logged → gate DID authorize → honest). There is no
  intermediate consumed-but-unlogged state.
- **One spend = one mana unit** (box P2 free variable h5: award sizing and cost
  are operator economics — the fold fixes the LEDGER MECHANICS, the operator
  prices them).
- **consume REFUSES at zero** loudly: returns `{:ok false :refused true}`, does
  NOT write a ledger entry (the inverse coupling: no authorization without a
  durable record).

### Tests (8 tests, all pass)

1. `award-consume-balance-roundtrip` — award 3, consume 2, balance is 1.
2. `refusal-at-zero` — consume on empty gate returns refused; no ledger entry.
3. `consume-to-zero-then-refuse` — deplete to zero, next consume refuses.
4. `spend-without-log-impossibility` (the ⅋) — after successful consume, the
   spend entry IS on disk (read directly from file, not through API).
5. `refused-spend-leaves-no-trace` — refused consume does not create a spend entry.
6. `multiple-awards-accumulate` — two awards sum correctly.
7. `award-validation` — rejects zero/negative/non-integer awards and blank words.
8. `fresh-gate-has-zero-balance` — no ledger file → zero balance.

Full suite: 491 tests, 3190 assertions, 0 failures, 0 errors.

### Gates

- clj-kondo: 0/0 on both source and test.
- check-parens: clean on both.
- Test suite: all pass.

### What was NOT done (scope)

- NO wiring into the live fold-authoring path (dark, per instruction).
- NO exchange-rate or cost-per-dispatch (operator economics — the fold's free
  variable h5).
- NO certificate integration (that's P1/P3, not P2).

## reference regression suite (driver: zai-10, 2026-07-05)

The REFERENCE-OUTPUT REGRESSION SUITE — so quality does not silently drop when
GLM-driven agents work without a frontier reviewer. Tonight's committed
artifacts ARE the reference outputs; the script re-derives each and diffs
against the stored reference; any drift is loud.

**Script:** `futon2/scripts/reference_regression.clj` (`.clj` not `.bb` — every
check exercises Clojure namespaces on the futon2 classpath: fold-escrow,
fold-llm, mana-gate. The p3_retry_enriched kill-test is itself a `-M` script.)

**Reference psis frozen:** `futon2/holes/reference-psis/psi-peradam-mech.txt`
(sha `97b9de29…`, matches deposit-006) and `psi-aif-head.txt` (sha
`b9c15315…`, matches deposit-004). Copied from /tmp so they survive.

**8 named checks, each with PASS/DRIFT output:**

1. **CONSTRUCTOR** — re-derives the cascade for the frozen peradam psi via
   `cascade_serve.py` (futon3a .venv): size 11, F-free-energy +2.227 (tol
   0.001), top `aif/no-self-certification` 0.521. Second psi (aif-head, my
   choice from tonight's records — deposit-004 log): size 21, truncated at 20,
   top `ants/baseline-cyber-ant` 0.679, F ~8.722 (tol 0.5 — embedding drift on
   the 21-pattern cascade; structural pins exact). Provenance: deposit-006 log
   + deposit-004 log.
2. **FOLD PIN** — `ft-autoclock-in-001` prompt-sha `5597da91…` reproduces
   byte-exact via the `fold-prompt` fn (the L3 reconstruction practice).
   Provenance: gate_2f_deposit.clj pin 1 (replay-validity).
3. **ESCROW** — `load-deposits` over the real dir: all 6 current deposits
   accepted, 0 rejections. One synthetic tamper (corrupt delta-g to -0.999):
   named rejection `:delta-g-mismatch`. Provenance: gate_2f_deposit.clj PASS.
4. **KILL-TEST** — `clojure -M scripts/p3_retry_enriched.clj`: OVERALL `:fail`,
   A fail/fail, B pass/pass (the verdict is a frozen fact about frozen data —
   drift means harness or data corruption). Provenance: P3-retry results log.
5. **MANA** — award 3 / consume / consume / consume / refuse-at-zero round-trip
   on a throwaway gate id (temp dir, cleaned up after): balances exact (3→2→1→
   0→refused). Provenance: mana_gate.clj (P2 mana gate, 8 tests).
6. **PERADAM** — the refusal census: exactly the current refusal causes per
   deposit (001/002 unstructured-witnesses, 003/004/005/006 missing-seal).
   The certificate LOADER is the fold's PLAN (not yet runtime code); this
   check freezes the DOCUMENTED census as a fact about the deposit corpus —
   verifying no deposit carries a structural `:seal` field (all are refusable)
   and the deposit-id set matches the frozen cause map. Provenance:
   ft-peradam-mechanization-006 boxes b5/b6 (STEP-0 CENSUS).

**Run once clean:**

```
8 checks, 8 pass
```

**Gates:** clj-kondo 0/0 (0 errors, 0 warnings); check-parens clean.
