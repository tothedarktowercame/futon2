# C-g′-wiring-discovery — G′ wiring part A: discovery of the canonical G path (read-only)

**Excursion:** E-kimi-task-437. **Author:** kimi-1. **Date:** 2026-11-25.
**Scope:** READ-ONLY. No code edited. All paths relative to `/home/joe/code/futon2`.
**Reference port (reviewed, out of path):** `futon2.aif.want-free-energy` (c6e9a7fd0), readback ad2c42c77, registry fe7410bd7, review d8235c2cc.

## 1. Where G's terms are computed, which helper, rational enclosures?

`compare-at-horizon` (`src/futon2/aif/cascade_coapply_g.clj:98`) does no arithmetic itself. Per candidate `score-one` (line 56) calls `scorer/rank-cascade-actions` (line 68–70, opts forced `:g-normalization :raw-cumulative :parameter-information-mode :none :require-finite-risk? true :prediction-only? true`).

- **Risk and ambiguity per step:** `cascade_observation_scoring.clj` `score-candidate` (line ~201): at each τ it rolls the token transition (`m/rollout-evaluation`) then queries `om/query` `{:op :score :belief next-q :tau τ :preference …}` (line 213). For the `:class-emission` model the score branch is `observation_model.clj:293–320`: `risk` = `m/outcome-risk` on the class predictive vs the class preference (`cascade_model_manifest.clj:600`, Lean `OutcomeRiskKL.outcomeRisk` — **double** `Math/log` Gibbs sum, `:infinite` if q(o)>0 ∧ c(o)=0); `ambiguity` is exactly `0.0` there (deterministic emission, line 311). (The generic branch at `observation_model.clj:378–406` computes double risk + belief-weighted row entropy.)
- **Novelty:** computed *inside* `cascade-coapply-g` itself, not the scorer: `beta-novelty` (line 14, `1/(a+b+2)` exact rational arithmetic on count pairs) and `expected-novelty` (line 47, weighted over the pre-step belief). `score-one` (lines 74–86) subtracts it per step: `g = (:G-efe e) − novelty`, and rewrites each certificate step's `:g`.
- **Helper verdict:** arithmetic is **its own** — `cascade-g/step-g` and `cascade-g/log-bounds` are **not used** anywhere on this path (`cascade_g.clj` is a separate rational-enclosure canonical G consumed by nothing in `compare-at-horizon`). Risk/ambiguity are IEEE doubles via `Math/log`; only the novelty term is exact rational. So the enclosure discipline G′ needs must be *added*, not inherited.

## 2. Where C comes from on each path; can C be 0?

Both lanes use the class-emission channel's own schedule: `cascade_shape_g.clj:41–63` `class-emission-model` builds `:class-preference {τ (if (= τ H) terminal {:ending/not-yet-evaluated 1})}`.

- **Selection lane:** `selection_canonical_g.clj:187–189` builds `schedule` — pre-horizon unit mass on `:ending/not-yet-evaluated`, horizon = `shape/terminal-class-preference` (`cascade_shape_g.clj:34–39`: `{:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100}`, exact ratios, all > 0). `compare-at-horizon` requires the shared model preference to equal `:authorized-class-preference` (line ~128–131).
- **Execution family lane:** `wm/family_canonical_g.clj:70–72` — terminal is `preference-registry/terminal-distribution!` of `:factorized-preference` when supplied, else the same `shape/terminal-class-preference`; the authorized schedule is built identically (lines 89–91) and each model rebuilt with that terminal (line 88).
- **Shape:** map class → probability, validated by `class-preference!` (`observation_model.clj:261–273`): each value a probability in [0,1], **total exactly 1** (a 0 total is explicitly refused); keys within `:class-universe` `[:focused :related :unrelated :stop-the-line :ending/not-yet-evaluated]`.
- **Zero mass:** allowed by validation. If prediction puts mass on a zero-C class, `outcome-risk` returns `:infinite` and `score-candidate` (line ~216–220) throws `:inadmissible-risk` because `score-one` sets `:require-finite-risk? true`; the candidate becomes a typed refusal (test-pinned, `cascade_coapply_g_test.clj:100–102`). So today G refuses rather than scoring; G′'s want term needs c ∈ (0,1] on every predicted outcome — same domain, so the refusal sites are reusable: where G refuses `:infinite-risk`, G′'s want term would be +∞ and must refuse identically. Note the terminal and `:ending/not-yet-evaluated` masses are all positive in every built schedule.

## 3. Where GNormalization is applied; what reads it

- Definition/application: `cascade_observation_scoring.clj` — `g-normalization-laws` (lines ~48–57), `g-normalization-declaration` (~59–64), `normalize-g-terms` (~66–78): under `:per-step-capacity-and-pattern`, `risk/(H·ln|O|)`, `ambiguity/H`, `information-gain/pattern-count`. `score-candidate` applies it at lines 234–241 (declaration recorded in the certificate at :g-terms/:normalization-declaration, :raw retains unnormalized sums).
- **The canonical path never normalizes:** `score-one` forces `:raw-cumulative` (`cascade_coapply_g.clj:69`) and `compare-at-horizon` refuses any other law (lines 141–145, `:conflicting-score-law`).
- Readers of `:per-step-capacity-and-pattern` are *other* paths: `cascade_shape_g.clj:333,353` (progress/class arranged scorer), `inner_abstention.clj:23`; Requirements-facing checks `q4_preference.clj:122,263` and `reading_policy_preference.clj:140` read the declaration. Part B should mirror this: G′'s want-term/H normalisation belongs as a declared term in `:g-terms`, not silently inside the sum; with the current `:raw-cumulative` forcing, H-division of the want term must be decided against `WantFreeEnergy`'s step indexing (the reference port already sums per-step then has total-g′; the Lean witness is the arbiter).

## 4. Consumers of the returned G value

Returned fields per scored entry: `:G-efe` = `:G-cascade` = `:controller-score` (raw risk+ambiguity−novelty), per-step `:g`, `:rank`, `:g-tie`, certificate `:g-terms` (`:risk :ambiguity :expected-information-gain`, :raw, :normalization-declaration), `:parameter-novelty`.

Selection lane (`outer_select.clj`):
- line 505 `selection-g/score` → entries read via `:G-cascade` at line ~523 (`canonical-g` fn): sets record `:G`, `:terms {:risk :ambiguity :novelty}`, keeps old pick-only G as `:pick-only-terms`; refusal → `:G :inadmissible` + `:canonical-refusal`.
- Abstention: `:stopping` entry's G at line ~532; must be finite (`:abstention-score-inadmissible`, line ~558).
- Finity gate: `:cascade-score-inadmissible` over all `:G`s (line ~556).
- Pick: strict minimum over prediction classes, exact tie → `refuse! :tie-at-minimum` (`outer_select.clj:610`; tie plumbing `cascade_selection.clj:305–311`); chosen minimum must be strictly below abstention G (`:pick-not-below-abstention`, `outer_select.clj:619–622`).
- Requirements in code: Q11 parameter posterior (`selection-g/predicted-parameters`, consumed at outer_select ~529–534), Q12 fired patterns (`fired-patterns`); Q2/comparisonFloor is the stopping comparison above.
- Trace/evidence fields: `:cascade-scores`, `:cascades`, `:abstention`, `:why`/`selection-why`, argument frame.

Execution lane (`wm/family_selection.clj` + `policy.clj`):
- `family-canonical-g/rescore` (line 426) rewrites each score via `replace-score` (`family_canonical_g.clj:41–61`): `:g`, `:controller-score`, `:risk :ambiguity :information-gain`, `:g-normalization`, `:legacy-g` kept aside; ranked union re-sorted by `:G-efe` with re-derived ranks (`family_selection.clj:434–438`).
- `policy/select-action-cascades` (policy.clj:399–…): softmax posterior σ(lnE − F − G/β) over `:controller-score`, strict-minimum over prediction classes, `:tie-at-minimum`, empty-cascade exclusion; decision gate kinds `decision_gate.clj:60–80` (`:tie-at-minimum`, `:no-admissible-class`, …).
- `family-canonical-g/certify-score` (line 138): recomputes `compare-at-horizon` and checks `fields {:g :controller-score :risk :ambiguity :information-gain}` equality and `(dissoc entry :rank :g-tie)` equality against the recorded `(:scorer-result record)` — any change to the returned entry shape or values is caught here.

## 5. Tests that pin compare-at-horizon's G numerically (must be re-derived from Lean, not edited to pass)

- `test/futon2/aif/cascade_coapply_g_test.clj` — direct numeric pins: exact novelty (3/4, 1/2+1/4, 3/8), `close?` G = risk+ambiguity−3/4 (line 50), G-eq-per-step-sum (line 94), ordering (`<` between :G-efe's, lines 70–71, 135–137), `:inadmissible-risk` refusal pins (100–102, 139–141), tie/rank pins.
- `test/futon2/aif/cascade_pattern_counts_test.clj` — counts feeding the novelty term.
- `test/futon2/aif/wm/family_theta_coherence_test.clj` — coherence gate + rescored values.
- `test/futon2/aif/wm/family_selection_canonical_g_test.clj` — sortedness by :G-efe (line 43), `:g` == scorer-result :G-efe == selection-candidate :g (48), `(< (:G-efe new) (:G-efe old))` (126), certificate round-trip into select-action-cascades (170).
- Selection-lane integration: `outer_select_test.clj`, `selection_certificate_test.clj`, `selection_discrimination_test.clj`, `native_currency_discrimination_test.clj`, `ranked_candidates_test.clj`, `policy_prefix_evidence_test.clj`, `observation_model_route_test.clj` (all pin choices, ties, abstention that ride on G values); `clean_click_composition_test.clj`, `full_loop_runner_test.clj`, `runner_source_restart_test.clj`, `q1_preparation_runner_test.clj` (end-to-end).

## 6. Proposed minimal change for part B

In `cascade-coapply-g/score-one` (single code path; no second scorer):

1. Keep calling `rank-cascade-actions` as today; keep its double risk/ambiguity as **reported fields** (`:risk-efe :ambiguity` under a renamed key or `:legacy-g` like the family lane already does).
2. Compute the pragmatic replacement with cascade-g's enclosure discipline: per step, want-term `−log c(o)` over the class predictive with exact ratio masses, `ln` via `cascade-g/log-bounds` (rational atanh-24 enclosure, `[lo hi]` intervals), summed with `plus`/`minus` discipline; a zero c on positive predicted mass refuses with the existing `:inadmissible-risk` shape. Per-step want-term/H normalisation applied as a declared `:g-terms` entry, H = `:horizon-steps`.
3. Novelty: already exact rational (`beta-novelty`); subtract with interval minus. Ambiguity: exactly 0 on the class path, but keep the term structurally (sum the enclosure) so the law is want + ambiguity − novelty.
4. Return `:G-efe`/`:controller-score` = G′ (interval midpoint? — *decision for review*: rank on the enclosure's lower bound, or record `{:g-prime [lo hi]}` and rank on `lo` with tie = overlapping intervals → `:tie-at-minimum`; recommend: rank on lower bound, record both bounds in the certificate, matching how certificates already record raw + normalized).
5. Certificate: `:g-terms` gains `:want-term [lo hi]`, keeps old `:risk` as `:efe-risk`; `:composition :want-plus-ambiguity-minus-frontier-novelty`; `certify-score` field set extended accordingly (fields check at `family_canonical_g.clj:170–176`).
6. Readback extension: `holes/labs/wm-contract/want_free_energy_readback.clj` gains a fixture whose class schedule and beliefs match `WantFreeEnergyWitness`; assert compare-at-horizon's G′ enclosure contains each witness value (width ≤ 2·tail bound), committed output + sha256 next to the existing readback, registry row updated.

Open questions for claude-14 review: (a) ranking convention on the enclosure (lower bound vs midpoint; tie = overlap); (b) whether `:G-efe` is renamed or keeps its name with a new `:g-law` tag (selection records already carry `:g-law :selection-cascade-grain-g`); (c) want-term/H division: per-step then sum, or sum then divide — must match the Lean witness.
