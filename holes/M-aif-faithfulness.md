# Mission: AIF faithfulness — retire the badge debt, node by node

**Date:** 2026-07-03
**Status:** ACTIVE (2026-07-04: Joe ratified the §3 scope boundary and took ALL
five bucket-1 verdicts — see §2.1 verdict ledger; Week-0 wave dispatched same day)
**Owner:** claude-12 (acting, per Joe's 2026-07-04 go-ahead; framed by Joe + claude-7)
**Parent audit:** `holes/E-r18-faithfulness-audit.md` + `data/r18-badges.edn` — the
16-quantity verdict table this mission exists to discharge. M-evaluate-policies
(closed-candidate 2026-07-03) already burned down the R5 lane's *audit* and built
its two heaviest repairs dark; this mission inherits the remainder across ALL nodes.
**Sources:** `holes/aif-wiring-explainer.html` (R18B badge data, per-quantity
claims/computes/repair) · `holes/E-KL-refinements.md` (items 1–5 all landed
2026-07-03) · `holes/M-evaluate-policies.md` §8 D1–D8, §12 (E6 shadow evidence),
IHTB-2 ordering constraint (§MAP) · `holes/E-close-the-loop.md` (R16-EXEC-REACH,
explicitly OUT of scope here) · `src/futon2/aif/` (~5.9k lines, the code under repair).

## 0. HEAD — big gaps, small gaps, or switches?

**Operator-voice anchor.** Joe, 2026-07-03, verbatim: *"We have *identified* a lot
of gaps (across R1, R6, R5, R7, and R14, as well as R16). But what I don't know yet
is if they are big gaps, small gaps, switches I need to decide on or what. […] Are
we talking days of work? weeks? months?"*

**Second anchor (same day) — the baseline requirement.** Joe, verbatim: *"with the
majority of boxes green, with these major faithfulness questions outstanding, I
think this mission should also include in the IDENTIFY phase not just the 'gap
between what's implemented and what would be faithful' but a very clear description
of what the War Machine is *right now*. […] If we're going to bring new features on
line as we work through the mission, it would be useful to have clarity about which
version of the WM is running at which tick, for example, and to have a clear
history of what it is actually achieving over time. Right now I don't have that
visibility."* — So IDENTIFY carries TWO deliverables: the gap inventory (§1.1) AND
the as-is baseline (§1.0); and the mission gains a **Bucket 0** (§2.0): tick
provenance + an achievement ledger, sequenced BEFORE any behaviour-changing repair
lands, because a flip you can't attribute to a tick range is a flip you can't audit.

**The scoping answer (claude-7, same day):** weeks, not months — **iff** the scope
boundary in §3 holds. The audit already did the expensive epistemics: every named
quantity is badged, every badge carries a named repair, and the two
highest-behavioural-impact repairs (`:risk-mode :kl`, `:ambiguity-mode
:gaussian-entropy`) are **already built dark with E6 winner-flip evidence in
hand**. What remains sorts cleanly into four buckets: operator switches (§2.1),
small mechanical repairs (§2.2), real builds with templates (§2.3), and a
structural tail that belongs to *other* missions (§3). Estimate for buckets 1–3:
**2–4 weeks of parallelizable belled work + ~5 operator decisions**, with a chunk
of the R5 lane already in flight (§4).

**What "done" means here.** Not "everything `:derived-from-FEP`" — the adversarial
rule means that badge is *earned per declared model*, and some quantities' honest
endpoint is a rename, a relocation, or a deletion rather than a canonicalization.
Done = **no quantity wears a name it hasn't paid for**: every badge is
`:principled-approximation` or better, OR the quantity has been renamed/relocated
/deleted per its D8 disposition, and the explainer + `data/r18-badges.edn` reflect
reality. This is the audit's own green path (E-r18-faithfulness-audit §Green path),
promoted to a mission.

## 1. IDENTIFY

### 1.0 The baseline — what the War Machine IS, right now (2026-07-03)

Verified against the live system this session (crontab, trace files, code), not
recited from docs. This subsection is the seed of the baseline deliverable (B-0b);
corrections land here as the mission's own ground truth.

**What runs, on what clock.**
- **Hourly, on the hour** (cron `0 * * * *`): the scheduled judgement tick —
  `clojure -M:wm-scheduled` (`scripts/wm_scheduled_run.clj`), a one-shot JVM
  (never the serving JVM; the no-live-pilot-edit discipline). One tick = observe
  the 13–14 channel schema → belief update → K=3 rollout over candidate policies →
  EFE ranking → softmax/abstain selection → act-gate over the cascade lane
  (rollout → classical fold → escrowed LLM fold) → on first `:pass`, artifact-only
  enactment via the deterministic executor (`futon2.aif.enact`) → trace write.
  Logs: `logs/wm-scheduled.log`.
- **Daily 04:00 UTC**: the outer loop (`-M:wm-outer-loop`) — R12 hyperparameter
  take-up. Logs: `logs/wm-outer-loop.log`.
- **Daily 04:30 UTC**: shared-corpus embedding refresh (futon3a
  `index_patterns.sh`).

**What is recorded.** Per-tick EDN records appended to
`data/wm-trace/wm-trace-YYYY-MM-DD.edn` — 42 daily files, continuous since
**2026-05-18**. A record carries: `:tick` (epoch-ms) + `:timestamp` · the full
observation vector (per-channel value/preferred/gap/in-range?) · belief before/
after · `:free-energy` decomposition · `:ranked-actions` with ALL EFE summands
persisted per candidate (post-2d6533e whitelist; `:risk-mode` stamped per action)
· `:decision` + `:mode` · `:act-gate-verdicts` (per-mission ΔF/ΔG/verdict/source)
· `:enactment` audit · `:realized-outcome` when the executor reproduces ·
`:precision-state` / `:policy-precision` (γ) carried across ticks. Enactment
flights also log to `p4ng/sequel-notebook.org`.

**What is NOT recorded — the named visibility gaps (Joe's ask, verbatim above):**
- **V-1 (version-at-tick):** no git sha, no code-version, no loaded-config stamp
  in the tick record (checked: trace schema + `wm_scheduled_run.clj`). Today,
  "which WM produced this tick" is only recoverable by correlating tick
  timestamps against futon2 commit times by hand. With dark-flag flips coming
  (D-1a/D-1b/B-2d), this becomes the audit chain's missing link. → **B-0a**.
- **V-2 (achievement history):** no consolidated readout of what the WM has
  actually DONE over its 6+ weeks of ticks — decisions taken vs. abstains,
  act-gate passes, enactments, γ samples accrued, channel trajectories. The raw
  trace has it all; nothing renders it. (`wm_trace_census.bb` is the near-template:
  it already sweeps the corpus per-term.) → **B-0c**.
- **V-3 (as-is description):** the closest thing to "what is the WM right now" is
  the explainer + completeness doc, both of which describe the *architecture*
  against the *ideal* — there is no short operational document a reader can trust
  for "what happens each hour, where does it land, what has it achieved." →
  **B-0b** (this subsection is its seed).

### 1.1 The gap inventory, by node

From the R18 badge data (16 quantities; 0 `:derived-from-FEP` at audit, 7
`:principled-approximation`, 9 `:analogical`). Per node as Joe listed them:

| Node | Quantities (badge) | The gap, one line |
|---|---|---|
| **R1** belief | `belief-update` (PA) | exact categorical-Bayes structure, but a hand-set `(1+w)` likelihood — no A-matrix |
| **R5** evaluate | `G-total, G-risk, G-ambiguity, G-info, G-gap, G-survival, G-structural-pressure, G-graph-pragmatic` (1 PA, 7 A) | the blend called "EFE"; canonical repairs for risk+ambiguity BUILT DARK; epistemic pole measured inert (0% flips/674); heuristic terms need D8 dispositions executed |
| **R6** select | `effective-temperature-softmax` (PA) | softmax(−G/τ) faithful; the adaptive τ_spread layer is a non-canonical extra on top of γ |
| **R7** precision | `channel-precision` (PA) | 1/max(var,ε) leg canonical; the summed-in need-component is an affect modulation wearing precision's name |
| **R14** γ | `policy-precision-gamma` (PA) + `policy-performance-ratio` (A, honestly named) | γ = clamp(2^(gain·perf)) from realized outcomes — a substitution for, not an approximation of, the E[G]-over-policies β-update |
| **R16** fold-eval | `coverage-delta-g` (PA), `F-free-energy` (PA, futon3a) | rollout accumulator genuine; per-move g = −coverage and accuracy = coverage-sum are proxies for real ΔF / log-likelihood |
| **R19** belly | `G-goal-outcome` (A) | the audit's sharpest fault (#11): a non-KL docstring-labelled "CANONICAL … KL"; W1 repair in flight (claude-4) |

(R19 wasn't in Joe's list but `G-goal-outcome` rides the WANT→G edge into R5's
G-total, so it's in scope; its *supply* side — the PROOF join — is not, per §3.)

## 2. The buckets — what kind of work each gap is

### 2.0 Bucket 0 — VISIBILITY. Prerequisite; lands before any behaviour change.

The second operator anchor's deliverables. Small builds, but sequenced FIRST:
every later flip/repair is only auditable against them.

- **B-0a Tick provenance stamp** (V-1). The scheduled runner stamps each tick
  record with: futon2 git sha (+ dirty flag), the resolved mode/flag set
  (`:risk-mode`, `:ambiguity-mode`, kl-channel-weights, live-wire switch, …), and
  a monotonic schema version for the trace record itself. ~1 day incl. tests;
  purely additive to the trace (append-only discipline preserved). Acceptance:
  given any tick from the file, `(wm-version-of tick)` answers "which code, which
  config" with no human correlation step.
- **B-0b State-of-the-WM baseline doc** (V-3). One short document (seeded from
  §1.0): what runs on what clock, what a tick does end-to-end, what's recorded
  where, what the current mode flags are, what "green" currently means per node.
  Kept current BY THE MISSION — each landed repair edits it in the same parcel
  (the §2.4 gate gains this as a check). ~1 day to write; marginal cost per
  parcel thereafter.
- **B-0c Achievement ledger** (V-2). A census-style script
  (`wm_trace_census.bb` is the template) sweeping `data/wm-trace/` into a
  per-day/per-week readout: ticks run · decisions vs. abstains · act-gate
  passes/fails (with missions) · enactments + realized-outcomes · γ sample count
  and value · channel-gap trajectories. Output: an EDN artifact + a rendered
  table (org or HTML) Joe can actually read. Once B-0a lands, the ledger also
  segments by WM version — "what did each version of the WM achieve." 1–2 days.
  Re-run at mission close = the before/after exhibit for the whole mission.

### 2.1 Bucket 1 — SWITCHES. Zero build; Joe decides. (hours)

The decision queue, each with its evidence already gathered:

- **D-1a `:risk-mode :kl` flip** (R5 G-risk). TRUE KL per channel
  (truncated+renormalised, quadrature-verified, `0f8d5c6`), reviewed-PASS,
  **explicitly UNBLOCKED** per E-KL-refinements. Evidence: E6 — winner-changing
  (ρ .841, sd ×22); T-calibration (`22b0024`) — the ×20 dispersion gap is
  STRUCTURAL, not a T-artifact, so flipping is a genuine behaviour choice.
  Canonical flip config = UNIFORM channel weights; `:pragmatic-parity` is a
  comparability preset only.
- **D-1b `:ambiguity-mode :gaussian-entropy` flip** (R5 G-ambiguity). Built dark
  (`2d6533e`); E6 shadow: sd ×554, flips the live winner alone. Joe-gated on
  `e6-shadow.edn` evidence. Note D-1a and D-1b interact — decide jointly or
  sequence deliberately.
- **D-1c G-survival disposition** (R5). Docs already relabel it "homeostatic
  pressure inside the pragmatic lane" (keys unchanged, API). Ratify: fold into
  risk once C is strong (D8: risk-KL absorbs it) vs. keep as named pressure term.
  It flips 47.9% — the dominant term — so timing matters.
- **D-1d G-structural-pressure relocation** (R5). Documented EXOGENOUS WEIGHT;
  canonical seat = the habit prior ln E(π) (R12), not a G-summand. Ratify the
  move. Flips 44.5% today.
- **D-1e W1-lane KL flip** (R19→R5 `G-goal-outcome`). Bernoulli round-trip landed
  dark (claude-4 `eb06565`, reviewed-PASS claude-5); flip named as ALSO waiting on
  T-calibration (KL lane ≈ ×9.6 hinge at T=0.1). Decide with D-1a.

**Mission deliverable for this bucket:** one decision memo per switch (evidence,
options, recommendation, blast radius), put in front of Joe; record verdicts here.

**VERDICT LEDGER (Joe, 2026-07-04, decisions taken in one sitting via claude-12):**
- **D-1a: DONE before this ledger** — `:risk-mode :kl` production-flipped
  2026-07-04 07:33 (`cd0d25d`; record = M-evaluate-policies §15; E-KL-refinements
  CLOSED). First live tick 08:00 same day. NOTE: the flip landed BEFORE B-0a,
  contra §5's stamp-before-flip rule — boundary recoverable via commit time +
  per-action `:risk-mode` in the trace, but B-0a is now urgent-before-any-further-flip.
- **D-1b: DONE** — `:ambiguity-mode :gaussian-entropy` live via the D5c flip
  (`8ae1090`); both rank lanes confirmed against `war_machine.clj`
  (`arena-ambiguity-mode` / `arena-risk-mode`, 2026-07-04).
- **D-1c: RATIFIED — fold G-survival into risk-KL (the D8 endpoint), execution
  GATED on C9 burn-in + W1 flip evidence.** The dominant term (47.9% flips) does
  not move during an unattributed window. Supporting evidence: the IHTB-2 pre-flip
  sim shows post-flip G-core already lands ≈ canonical −E[ln C].
- **D-1d: RATIFIED — relocate G-structural-pressure to the habit prior ln E(π)
  (R12).** Build dark behind a mode flag (D5a pattern); the flip is Joe's, after
  B-0a stamps the trace.
- **D-1e: FLIP NOW** (Joe chose to ride the same before/after boundary as
  D-1a/D-1b rather than wait for B-0a). Belled to claude-4 (named W1 driver)
  2026-07-04. **EXECUTED same day (`fb15d66`) — reviewed-PASS (claude-12,
  2026-07-04).** Review checked: full 9-file diff read; clj-kondo re-run
  independently (0/0); witness + regulator-sweep test nses re-run (26 tests /
  115 assertions / 0 fail); check-parens re-run (OK); explainer regen re-run —
  byte-identical to committed; badge correctly NOT raised (`:repair-built`
  only). Process note: the commit's `wm-baseline.md` hunk swept in claude-10's
  in-progress B-0c section (both parcels touch that file) — content looks
  right but B-0c's numbers are verified in ITS review, not this one.
  Remaining: live-tick stamp (`:goal-outcome-mode :kl` in the 08:00+ trace),
  then reviewer-side badge raise after burn-in.
  **LIVE-TICK EVIDENCE LANDED (2026-07-04 08:03):** the first tick under all
  three flips wrote 109 scored candidates each stamped `:risk-mode :kl` AND
  `:goal-outcome-mode :kl`, `:wm-version` provenance present (sha `7015ce9`,
  schema v2, `:ambiguity-mode :gaussian-entropy` in the stamp) — and the
  decision flipped in the predicted direction: `advance-mission
  M-first-flights` at G=+4.5967 (positive totals, the IHTB-2 sim's
  +1.68..+6.67 window) vs. the hinge era's negative-G `address-sorry`.
  Badge raises for G-risk / G-ambiguity / G-goal-outcome now wait ONLY on the
  C9 burn-in census (Joe's wording: live-tick evidence + burn-in).

### 2.2 Bucket 2 — small mechanical repairs. 1–3 days each; bell-able as-is.

- **B-2a G-total struct split** (R5, `efe.clj:449`). Expose canonical
  G = risk + ambiguity first-class; demote the 6 augmentation terms to a named
  multi-objective layer. The audit: "a rename + a struct split, no new math."
  `:G-core` already emitted + persisted (D2/I3 live, 0 violations) — this is
  finishing the relabel in the code's own shape.
- **B-2b G-graph-pragmatic split** (R5, `efe.clj:118`). The `1000·mask` is a
  domain restriction (Π_feasible), not a value; ascent = pragmatic proxy. Split
  per D8/§3.5.
- **B-2c R7 need-term gating** (`precision.clj:92`). Pull the affect/need
  component out of the precision sum into a separate, named salience channel.
- **B-2d R6 τ-layer separation** (`policy.clj:35`). Let γ carry sharpness alone;
  make τ_spread = range(G)/k a separately-justified calibration layer.
  ⚠ Half-bucket-1: this changes live selection behaviour — build it dark behind a
  mode flag like D5a did, flip is Joe's.

### 2.3 Bucket 3 — real builds. ~2–5 days each; most have templates.

- **B-3a R1 A-matrix** (`belief.clj`). Derive the `(1+w)` likelihood from an
  explicit observation model A over the 13–14 channels × 7-status state space.
  The one genuinely new *design* in the mission (where does A come from: hand-set
  v0 → learned later?). Raises the last un-touched PA toward canonical; also the
  prerequisite for R3's `gm: A` label being honest.
- **B-3b R14 γ β-update** (`policy_precision.clj:123`). Fold prospective
  G-spread-over-policies into γ's prior; keep the realized-perf loop as the
  correction term (repair per audit #6). NOTE the data gate: γ only moves off 1.0
  when outcome VARIANCE arrives, which is R16-EXEC-REACH's cadence (out of scope,
  §3) — build the estimator now, expect it to idle until reach grows.
- **B-3c G-info → real EIG or delete** (R5, `efe.clj:256`). Template exists (the
  futon3c portfolio surface computes a real posterior-entropy EIG). **SEQUENCED
  AFTER cascade-row handling per IHTB-2** (M-evaluate-policies §MAP): deletion
  before that hands wins to 0.0 placeholders. Measured 0% flips/674, so no urgency
  — do it last.
- **B-3d G-gap → real expected-uncertainty-reduction** (R5, `efe.clj:176`).
  Currently 6.0 × table lookup; saturation flagged (E-possible-world-regulator).
  Same epistemic-pole family as B-3c; sequence together.
- **B-3e W1 predictive KL completion** (R19, `c_vector.clj:406`). IN FLIGHT —
  claude-4 is the named driver; dark twin landed; E7 (post-merge re-census) is the
  named follow-on. This mission TRACKS it, doesn't re-dispatch it.

### 2.4 What each repair must carry (the gate, uniform)

Per the coding-handoff protocol: built dark behind a flag where behaviour could
change · clj-kondo + check-parens + the relevant witness tests · in-source HONESTY
block updated · `data/r18-badges.edn` re-badged by the *reviewer*, not the author
(the E-KL-refinements precedent: "badge candidate, re-audit is the owner's step —
not claimed") · explainer R18B block regenerated (`scripts/r18_badges_to_js.bb`) ·
**baseline doc (B-0b) updated in the same parcel** — a repair that changes what a
tick does without updating "what the WM is right now" fails review.

## 3. NOT IN SCOPE — the structural tail (named, so it's a boundary, not a hole)

These gate how much *data* flows through the loop, not whether the labels are
honest. Each is already tracked elsewhere; pulling any of them in is what would
turn this mission from weeks into months:

- **R16-EXEC-REACH** (executor rule-table reach) → E-close-the-loop. Gates γ's
  sample variance (B-3b's data), not γ's estimator.
- **coverage-ΔG's per-move g from real ΔF** + **cascade-F accuracy as a real
  log-likelihood** (`fold_eval.clj:30`, `cascade_construct.py:214`) → the fold
  evaluation lane (E-close-the-loop / the bake-off). These two PAs keep their
  amber badges until that lane matures; this mission only keeps their HONESTY
  blocks current.
- **The (have→want) PROOF-join corpus** → E-have-want-pairs / M-populate-substrate-2.
  R19's supply side; `G-goal-outcome`'s *evaluation* repair (B-3e/D-1e) does not
  wait on it.
- **R15** (nested/hierarchical prior) and **R17** (structure learning port) —
  completeness gaps, not faithfulness gaps; different mission.
- **Post-flip regulator dynamic-range re-exam** (added 2026-07-04, ratified with
  the scope): the `cd0d25d` suite finding — gap-weight (and the regulator sweeps
  generally) are calibrated in hinge-era units and no longer dominate under
  nats-scale KL risk → E-possible-world-regulator, not this mission.

## 4. In-flight registry — do not double-bell

| Who | What | Where |
|---|---|---|
| claude-5 | E-KL-refinements owner; reviews on the KL lane; R5 badge work (Joe, 2026-07-03) | E-KL-refinements (CLOSED with `cd0d25d`) |
| claude-10 | KL items 1–3 builds (truncation `0f8d5c6`, calibration `22b0024`); **B-0c achievement ledger DONE `61c9b06`, reviewed-PASS claude-12 2026-07-04** (determinism re-verified run1≡run2; independent count re-derivation matched: 41 files / 682 ticks / all decided / 36 enactments; one stale header comment about :risk-mode persistence fixed by reviewer) | this doc §2.0 |
| claude-4 | W1 driver; Bernoulli round-trip `eb06565`; **D-1e flip DONE `fb15d66`, reviewed-PASS claude-12 2026-07-04** (live-tick stamp pending); E7 re-census follow-on | this doc §2.1 / E-C-vector-live §11–12 |
| claude-9 | **B-0a tick provenance stamp DONE `da9fec1`, reviewed-PASS claude-12 2026-07-04** (kondo/check-parens/trace-test 17/45/0 re-run independently; LIVE ACCEPTANCE MET: the 2026-07-04 08:00 tick record carries `:wm-version` with `:git-sha` = exact HEAD `7015ce9`, all three modes, `:trace-schema-version 2`. Note: `:git-dirty? true` fires on others' tracked doc churn — honest but noisy; revisit only if it drowns signal. Re-dispatch history: codex-1 bell FAILED, Codex quota exhausted until 2026-07-18. **Follow-up `515a593` also reviewed-PASS** (stamp now computed at JVM start — right fix for the shared-tree mid-run-commit hazard; nit: its commit message misreads the first tick, whose 7015ce9 stamp was in fact accurate. Its 08:09 manual verification run wrote a record into the production trace → trace-hygiene norm recorded in wm-baseline.md; record left in place, fully self-identifying)) | this doc §2.0 |
| claude-12 | mission owner; B-0b baseline doc (`holes/wm-baseline.md`, seeded 2026-07-04); reviews all parcels (author ≠ reviewer) | this doc |
| claude-5 | **B-2a+B-2b DONE `ac60874`, reviewed-PASS claude-12 2026-07-04.** `:G-core` + named `:augmentation-terms`/`:G-augmentation` (signs as-they-enter); graph mask/value split additive, `:G-total` expression untouched; golden byte-identity captured pre-split at `c11e162` under BOTH option eras; schema v3 + baseline updated in-parcel. Review: diff read; kondo 0/0 + check-parens re-run; witness ns 3× deterministic; **full suite re-run by reviewer 449/2994/0** (claude-5's flagged one-off transient did not reproduce — 4 witness runs + 2 full-suite runs clean between us; treated as environmental until seen again). **Follow-on: D-1d dark build belled to claude-5 same day.** | this doc §2.2 |
| claude-6 | **B-2c DONE `b36e614`, reviewed-PASS claude-12 2026-07-04.** Dark behind `:salience-mode` (default `:summed` = v0.13, golden-witnessed against pre-parcel output; `:separate` = canonical Π + named `:salience` key). Review: diff read; kondo 0/0, check-parens OK re-run; precision suites re-run green; golden verified present. Note: its arena resolver rode in B-2d's commit (serialized war_machine.clj edits — acceptable, coherent). Flip needs an R7 shadow study (queued). | this doc §2.2 |
| claude-3 | **B-2d DONE `7fbaf8b`, reviewed-PASS claude-12 2026-07-04.** Dark behind `:tau-mode` (default `:spread` byte-identical, witnessed at both effective-temperature and select-action level; `:gamma-only` = canonical). Correct opt-IN hatch polarity; both new modes stamped into `:wm-version` via the same resolver fns the behaviour uses. Review: diff read; kondo/check-parens re-run OK; policy+precision+policy-precision suites re-run 73/211/0. Flip decided jointly with B-3b (shared sharpness seam); needs E6-style shadow (queued). | this doc §2.2 |
| claude-7 | **B-3a DONE `3d2683a`, reviewed-PASS claude-12 2026-07-04.** Review: full diff + design note read; kondo re-run (0 errors; the 1 warning verified pre-existing in the parent commit); check-parens OK; belief+forward-model suites re-run 76/1389/0 — matches claim. Legacy-reduction theorem (A-identity + κ(w)=log₂(1+w) ≡ (1+w), 1e-12) is the parcel's keystone; default `:legacy` byte-identical; channel block is read-only unification, prediction path untouched. **Flip prerequisite (before a D-memo goes to Joe): an E6-style shadow — posteriors/decisions under `:a-matrix` vs `:legacy` over the live corpus.** Badge stays PA until then (reviewer-side). | this doc §2.3 / holes/E-r1-a-matrix-design.md |

| claude-10 | **B-3a-shadow** — E6-style `:a-matrix` vs `:legacy` shadow study, the flip-memo prerequisite (belled 2026-07-04, job `invoke-…-577c2324`; read-only, artifacts to holes/labs/) | this doc §2.3 |

Coordination rule inherited from M-evaluate-policies §10: parcels on Agency,
author ≠ reviewer throughout, verdicts recorded in this doc.

**D-1d dark build: BELLED** to claude-5 2026-07-04 (job `invoke-…-82f10c9c`)
immediately after B-2a/B-2b landed — the `efe.clj` seam is free and the
augmentation naming makes the relocation surgical. C9 burn-in census waits for a few days of post-flip
ticks (1 real tick so far); it gates the three badge raises AND D-1c execution.
Flip-evidence shadow studies for B-2c (`:salience-mode`) and B-2d (`:tau-mode`,
jointly with B-3b) queue behind the in-flight parcels — one dark-mode shadow
harness can likely serve all three (A-matrix shadow is the template).

**Standing authorization (Joe, 2026-07-04, verbatim intent):** "happy for you to
continue driving it through with claude helpers" — claude-12 drives dispatch +
review without per-wave check-ins; operator gates (flips, badge-raise timing,
mission close) remain Joe's.

## 5. Sequencing sketch

1. **Week 0 (now):** Joe ratifies scope (§3 boundary). **B-0a (provenance stamp)
   bells out immediately** — it must be in the trace BEFORE any flip so the
   before/after boundary is machine-readable. B-0b/B-0c in the same wave.
   Joe takes the D-1a/D-1b/D-1e flip decisions (one sitting; the evidence is
   assembled — and B-0c's ledger gives the "what is it achieving today" baseline
   to judge flips against). D-1c/D-1d verdicts recorded.
2. **Wave 1 (parallel bells, after B-0a lands):** B-2a/B-2b/B-2c (mechanical,
   independent) + B-3a (A-matrix, the long-lead design) + B-2d dark-build.
3. **Wave 2:** B-3b (γ β-update) once B-2d's τ question is settled (they share
   the selection-sharpness seam) · cascade-row handling, THEN B-3c/B-3d
   (IHTB-2 order).
4. **Continuous:** track B-3e/E7; re-badge + regenerate the explainer as each
   repair lands; keep the §2.1 verdict ledger current.
5. **Close:** re-run the full 16-quantity audit against the badge criteria
   (adversarial rule intact); every row `:principled-approximation`+ or
   dispositioned; R18 node flips partial → done. Mission-close is Joe's call.

## 6. Estimate (the answer to the HEAD question, on the record)

- Bucket 0: **~3–4 days total** (provenance stamp 1, baseline doc 1, ledger 1–2),
  front-loaded; the ledger re-run at close is the mission's own exhibit.
- Bucket 1: **hours** (decision memos: ~1 day of prep, already mostly written by
  M-evaluate-policies).
- Bucket 2: **4 items × 1–3 days**, fully parallelizable.
- Bucket 3: **5 items × 2–5 days**, 1 in flight, 2 sequenced late by IHTB-2,
  parallelizable across 2–3 agents.
- **Total: 2–4 weeks at current belled-work cadence, ~5 operator decisions.**
  The months-scale work is all behind the §3 boundary, by construction.
