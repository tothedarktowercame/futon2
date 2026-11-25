# C-gprime — G′: plan by the chance the wants are met — aligned Clojure + F8-form readback

**Row:** `:want-free-energy` (`aif-equations.edn`, node **R5**). One packet,
one seat (kimi-1, Agency job `invoke-1795584867616-29022-45ae2c8c`, assigned
by Joe via claude-14, 2026-11-25: "an aligned Clojure implementation so that
we could just get started running this model … the well-established way of
developing aligned Clojure that produces runtime certificates that prove that
the runtime matches the Lean"). Form: C521 (the F8 slice report). The Lean
side already existed (claude-14's worktree); this packet is the Clojure, the
certificate and the registry row. **No wiring into the click**: G′ changes no
`:choices`, and Requirements.lean is untouched.

**Commits.** futon2 `c6e9a7fd0` (namespace + tests) + `ad2c42c77` (readback +
committed output) + `fe7410bd7` (registry row and the three `:references`
entries it cites) + this commit (this report).

## 1. What was in Lean, and what the Clojure ports

`DarkTower/WarMachine/WantFreeEnergy.lean` (mathlib4 worktree
`c14-cascade-outcome`, branch `c14/cascade-outcome-type`):

- `WantEffect O`: wants as an effect, `c(o) ∈ (0,1]` per outcome — **not** a
  distribution (`PreferenceSurpriseUncertain.rankings_disagree` records why
  the distribution reading misleads below reliability 1).
- `chance q w = Σ o, q(o)·c(o)`, the soft chance the wants are met;
  `wantTerm q w = −log chance q w` (Tull, Kleiner & St Clere Smithe 2023,
  Cor. 38 / Prop. 37; Parr & Friston 2019's generalised free energy).
- `stepG' q a w = wantTerm (push q a) w + CascadeEFE.ambiguity q a` — the
  same split as `CascadeEFE.stepG` (risk + ambiguity) with **only the risk
  replaced**; the epistemic term kept by statement (Millidge, Tschantz &
  Buckley 2021). `totalG'` sums steps `t = 1..T`;
  `scoreCascadeCo'` / `scoreCascadeCoN'` score a candidate with G's
  structural refusals and the same novelty subtraction; `perStep` is the
  horizon total divided by H (Joe: "sensible").
- `WantFreeEnergyBenefit.lean`: `mixK`, `chance_mix`, `climb_raises_chance`,
  `delivery_lowers_wantTerm` — delivering work lowers G′ when the wants
  prefer higher rungs.
- `WantFreeEnergyWitness.lean`: the transcribed numerals (below).

The port (`src/futon2/aif/want_free_energy.clj`, ns
`futon2.aif.want-free-energy`) follows `futon2.aif.r1-belief`'s shape
(`b389f2fe2`): plain validated maps, exact ratio arithmetic wherever the Lean
uses real literals, ex-info refusals where the Lean has structure fields or
hypotheses. It **reuses** `r1-belief` (rungs, `climb`, `update-belief`) for
the witness cases and does not reimplement prediction, ambiguity or novelty:
`ambiguity` is stated as `CascadeEFE.ambiguity` and the readback measures the
agreement case against the existing production risk
(`futon2.aif.cascade-g/step-g`, the `CascadeEFE.risk` behind the canonical
cascade G in `cascade-coapply-g`). Novelty in `score-cascade-co-n'` arrives
pre-computed by the canonical novelty code — the namespace does not recount
patterns.

## 2. What the readback measures, and the provenance rule

`holes/labs/wm-contract/want_free_energy_readback.clj`, output committed at
`holes/labs/wm-contract/runs/G-prime-want-free-energy/clojure-readback.txt`
(sha256 `8f5dfbef…0189d0`; reproduces **byte for byte** on a second run).
Following C521 §2's rule: **every `-expected` is transcribed from a named
Lean theorem**, listed in the header:

| theorem | rational | readback |
|---|---|---|
| `chance_nine_in_ten` | 41/50 | exact `==` |
| `chance_certain` | 9/10 | exact `==` |
| `chance_mix_half` | 11/20 | exact `==` |
| `chance_uniform` | 12/25 | exact `==` |
| `chance_uniform_climb` | 57/100 | exact `==` |
| `b17_noTrace_realised` | 3/34 | exact `==` (update runs in `r1-belief`) |

Rational chances compare **exactly** (Clojure ratios). `log` is
transcendental, so, as in C521, Lean keeps it symbolic and the readback
evaluates only `Math/log`: the three want terms (nine-in-ten, certain,
uniform-climb) report **delta 0.0** at tolerance 1e-12.

**The agreement case** (`stepG'_eq_stepG_of_point`) is measured, not
restated: one real-shaped candidate — the five R1 rungs, a point prediction
on `:realised`, a deterministic observation, the uniform want 1/5 per rung
read as the preference distribution (it sums to 1, which the theorem requires
of C) — scored both by `step-g'` and by the existing production
`cascade-g/step-g`. G′ lies **inside the canonical risk's rational
enclosure**; delta from the enclosure midpoint is **2.2e-16** (the enclosure
itself is 1 ulp wide).

`perStep` and the `scoreCascadeCoN'` novelty subtraction are measured on a
two-step candidate: deltas 0.0.

## 3. Tests, planted cases, mutants

`test/futon2/aif/want_free_energy_test.clj`: 13 tests, 40 assertions, 0
failures (`clojure -M:test -n futon2.aif.want-free-energy-test`). Tests are
named after the theorems (`wantTerm_nonneg`, `wantTerm_zero_of_certain`,
`wantTerm_point_eq_risk`, `stepG'_eq_stepG_of_point` — both against the
existing risk function — `wantTerm_prod`, `reliability_helps`, `chance_mix`,
`climb_raises_chance`, `delivery_lowers_wantTerm`, plus the witness numerals
and the cascade scorers). Planted bad cases: a want value **0** (hard want —
deferred), **>1**, and **<0** are refused; a **non-normalised** prediction is
refused, never repaired.

Mutants tried, and what catches each (run against the single test namespace):

- **M1** chance's multiply → add: caught by all five witness-numeral checks,
  both agreement tests, `chance_mix`, `reliability_helps`,
  `climb_raises_chance`, `delivery_lowers_wantTerm`, `wantTerm_prod`,
  `wantTerm_zero_of_certain`, `score-cascade-co`.
- **M2** want-term's sign dropped: caught by `wantTerm_nonneg`,
  `wantTerm_point_eq_risk`, `reliability_helps`, `delivery_lowers_wantTerm`,
  `stepG'_eq_stepG_of_point`.
- **M3** hard want 0 admitted (`pos?` → `not neg?`): caught by
  `want-effect-refusals`.
- **M4** prediction sum-to-1 check dropped: caught by `prediction-refusals`.
- **M5** per-step divide → multiply: caught by `score-cascade-co`.
- **M6** push's q-weight dropped: caught by the kernel validation itself
  (rows no longer sum to 1 → `:invalid-kernel`) in `climb_raises_chance`,
  `delivery_lowers_wantTerm`, `witness-numerals`.
- **M7** novelty added instead of subtracted: caught by `score-cascade-co`.

Baseline after every revert: 0 failures.

## 4. Gates

- `clj-kondo` clean on all three new Clojure files (0 errors, 0 warnings).
- `futon4/dev/check-parens.el`: OK on all three.
- Test gate: only the G′ namespace was run, per the requisition; 13 tests /
  40 assertions / 0 failures.
- Readback reproduces its committed output byte for byte (sha checked twice).
- Nothing was hot-loaded into any serving JVM; the namespace has no
  production callers.

## 5. What is NOT claimed

- **No wiring.** Nothing in the click, selection or runner calls G′. The
  registry row's `:code` names the namespace and says so; `:live-consumer`
  is `nil`.
- **No want-based refusal.** Because every value is positive, every plan has
  a positive chance (`chance_pos`): G′ ranks a want-violating plan low rather
  than refusing it — G's `.inadmissibleRisk` refusal has no counterpart.
  Whether a want may take value 0 (a hard constraint) is **left open**, as in
  the Lean header.
- **Novelty is not recomputed here.** `score-cascade-co-n'` subtracts a
  novelty total supplied by the caller, computed by the same code
  (`cascade-coapply-g`) that computes it for canonical G.
- **The witness wants are illustrative.** `metWant` (9/10, 1/10) and
  `rungWant` (1/10, 1/5, 2/5, 7/10, 1) are the Lean's witness values, not a
  calibrated want for any benefit; `traceProb` remains a placeholder for Joe
  to calibrate (as `r1_belief_examples.edn` already records).
- **Nothing could not be transcribed exactly.** All six rational numerals
  transcribe; only `log` is floating, by the C521 rule, and is reported at
  1e-12 with delta 0.0.

## 6. Wiring (E-kimi-task-438, 2026-11-25): G′ is now the canonical G's pragmatic term

Part A (E-kimi-task-437, futon2 `18fff967a`) discovered the canonical G
path; part B (this section) wired G′ into it under Joe's ruling: run G′,
aligned with Lean, one implementation for selection and execution, no shadow
scorer. Commits: `1056896c6` (stale-test repairs), `b53f7ec88` (the wiring),
`ee81aebf4` (pins, planted case, mutant check), `4145d2ed2` (readback
wiring section), plus this document and the registry row.

**What changed.**

- `observation_model.clj` (class-emission `:score`): computes, from the same
  predicted outcome distribution and the same step C, the EXACT rational
  chance `Σ_o Q(o|π)·c(o)` (C read as a want effect, decision 4), the want
  term `−log chance` as a double (decision 1: doubles and the existing
  strict-minimum/`:tie-at-minimum` ranking are unchanged), and
  `cascade-g/log-bounds`' rational enclosure of the want term for audit.
  Risk is computed in the same pass and reported, never ranked on.
- `cascade_observation_scoring.clj`: under `:g-law :want-free-energy/v1`
  the entry G is `Σ want-term + Σ ambiguity − information-gain`.
  `:g-terms` keeps `:risk` (reported) and gains `:want-term`; `:raw` keeps
  exactly the three `normalize-g-terms` inputs so q4's recompute-equality
  still holds.
- `cascade_coapply_g.clj` (`score-one`): forces the want law, so BOTH lanes
  (selection: `shape/terminal-class-preference` schedule; family:
  preference-registry terminal distribution) score by G′, with the Beta
  novelty subtraction exactly as before. Entries carry `:g-law
  :want-free-energy/v1` and `:g-per-step = G′/H` (decision 3; ranking stays
  on the raw total, the same order at one shared H). Certificate steps
  carry `:chance` (ratio), `:want-term`, `:want-term-enclosure`, `:risk`,
  `:ambiguity`, `:expected-information-gain`, `:g`.
- `family_canonical_g.clj`: the record and the certified receipt carry
  `:want-term` (on the receipt's `:normalization` as a sibling of
  `:terms`/`:raw`); `certify-score`'s recompute-equality pins the want term
  too.
- `q4_preference.clj`: when the receipt declares a want term, the compared
  G recomputes as `want-term + ambiguity − information-gain`; legacy
  receipts (no want term) keep the risk composition.

**Which pins moved.** None of the named numeric pins moved: every fixture
in `cascade_coapply_g_test`, `cascade_pattern_counts_test`,
`family_theta_coherence_test`, `family_selection_canonical_g_test` and the
selection integration tests (`outer_select`, `selection_certificate`,
`selection_discrimination`, `native_currency_discrimination`,
`selection_canonical_g`) whose G value is pinned uses deterministic
(theta = 1) transitions, i.e. point predictions, where
`stepG'_eq_stepG_of_point` gives want-term = risk exactly (verified by
`gprime-point-predictions-keep-the-old-g`). Stochastic fixtures in those
namespaces pin orderings, beliefs and novelty, not G values, and the
orderings are unchanged. Two NEW pins were added, derived from the
certified reference port: chance exactly 1/2 for bern(9/10) under the ruled
C (same shape as `WantFreeEnergyWitness.chance_nine_in_ten`), and the
risk-order/G′-order disagreement fixture (mutant check).

**Baseline repairs (pre-existing reds, not caused by the wiring).**
`cascade_coapply_g_test` was red at HEAD on two tests stale against the
deliberate 2026-10-29 commits `980b78939` (`:f` retention) and `0be247a91`
(explicit `:class-free-energy` history); `family_selection_canonical_g_test`
was red alone because the class-F history index (`a8a4caec6`) only existed
as a side effect of other namespaces in full-suite runs (its fixture now
creates the empty index and removes it again). `class_observation_scoring_test`
and `q4_preference_route_test` have further pre-existing environmental
failures (43 failures / 5 errors), byte-identical with and without this
packet.

**Planted bad case and mutant check.** A C with 0 mass on a predicted
outcome refuses with the existing `:inadmissible-risk` shape — never
`−log 0` (`gprime-hard-want-refuses-never-minus-log-zero`; hard wants stay
deferred). `gprime-ranking-mutant-check-risk-order-disagrees` constructs two
candidates where the KL order and the G′ order genuinely disagree (chance
11/20 vs exactly 1/2) and asserts the G′ winner: ranking on the reported
`:risk` instead of `:G-efe` fails that test.

**Readback.** `want_free_energy_readback.clj` gained a wiring section that
runs the production `compare-at-horizon`: bern(9/10) under C 9/10:1/10
gives chance exactly 41/50 (`chance_nine_in_ten`), the point prediction
gives 9/10 (`chance_certain`), and the agreement case shows the wired
`:G-efe` equal to the old risk-based G recomputed from the reported fields
of the same entry (`stepG'_eq_stepG_of_point`), with the bern contrast
alongside (the delta is exactly the nine-in-ten want term). Output
reproduces byte for byte; sha256
`74ee7f444153f5b420147b6322bfca6ebedcf4cc6dcd7963aaca207c19def1f3`.

**Gates.** clj-kondo clean on the changed files (0 errors; one pre-existing
redundant-let warning in `observation_model.clj`); check-parens OK on every
changed `.clj`; only the named namespaces (plus adjacent consumers) were
run, all green (94 tests / 552 assertions / 0 failures across the nine
named namespaces). Nothing was hot-loaded into the serving JVM; the WM
stays held.

**What is NOT claimed (in addition to §5).**

- **No interval ranking.** Ranking uses the double want term exactly as it
  used the double risk (decision 1); the rational enclosure is audit-only.
- **No hard wants.** c(o) = 0 on a predicted outcome refuses; it is never
  clamped or scored.
- **No re-derivation of C.** The want effect IS each lane's existing C mass
  read as an effect (recorded in the certificate as
  `:c-reading :C-read-as-want-effect`); whether C should instead be derived
  from declared wants is out of scope.
- **Legacy paths unchanged.** Callers of `rank-cascade-actions` without
  `:g-law :want-free-energy/v1` (e.g. `cascade-shape-g/score-policy`'s
  progress channel) keep risk-based G, as do the token-model paths.
