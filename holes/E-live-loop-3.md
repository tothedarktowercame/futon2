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
