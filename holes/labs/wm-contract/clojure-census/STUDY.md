# Testimony study — planned vs built vs specified

Opened 2026-09-18 by claude-12, under Joe's design the same day: in
parallel with each census dispatch D1–D7 (zai testifies to what IS BUILT),
claude-4 testifies to what it PLANS TO BUILD for the same term cluster,
and both testimonies are compared against the Lean specification.

## The three witnesses

| witness | testifies to | artifact |
|---|---|---|
| zai (per dispatch) | what is built | `D<N>-<slug>.edn` (PLAN.md schema) |
| claude-4 (per dispatch) | what it plans to build / claims built | `T<N>-claude4.edn` (schema below) |
| Lean | the specification | census modules + carriers (frozen core) |

## Testimony schema (claude-4, per cluster)

```edn
{:testimony :T1
 :terms
 [{:term :G
   :planned  [{:var "intended name" :wm-item "..." :status :planned | :in-progress
               :inputs ["..."] :depends-on ["..."]}]
   :claims-built [{:var "..." :wm-item "..." :note "..."}]
   :no-plans? false}]
 :grounding ["DAG/commission artifacts read, by path"]}
```

## Comparison, per term (filled by claude-12 after both land)

- **P↔S**: does the plan's shape match the Lean signature? (a plan that
  cannot fill the spec's inputs is divergent before it is built)
- **B↔S**: does the built producer match the Lean signature? (from the
  census ledger's :match)
- **P↔B**: does the plan acknowledge what is already built? (duplicate
  risk: planning what exists; facade risk: claiming built what is not)

Discrepancy classes: :plan-diverges-from-spec, :built-diverges-from-spec,
:plan-ignores-built, :claims-built-not-found, :consistent; added in use:
:census-role-error (D1), :aligned-but-dead (D1), :spec-census-error (T2 —
the Lean census itself bound the wrong law and the testimony caught it).
Results table appended below as each pair (Dn, Tn) completes.

## Results

### G cluster — D1 (2c4e022a + review fix f3d0417b) × T1 (2afbc6ed), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :G | no new plans — testimony is claims-built only (the DAG records obligations, not build plans). Pre-registered finding stands: `LF-expected-free-energy` is *accepted* while its own acceptance text leaves the ambiguity argument unbound → **:plan-diverges-from-spec** (at the obligation-record level) | **:matches on the declared reduction domain** (horizon↔T, precedence-fn↔π, :spec↔constant C, pointwise risk with ⊤-iff-zero-C; refuses outside the domain rather than mis-scoring) | all 6 claims-built confirmed by the census, incl. the private `outcome-risk-pointwise` both witnesses flagged independently → **:consistent** |
| :risk | :no-plans (informative) | :not-checked — dedicated OutcomeRiskKL pass queued | :consistent |
| :ambiguity | :no-plans | :matches (within the identity-A known-failing context) | **discrepancy found and resolved**: census said `step-ambiguity` :live; testimony said built-but-not-reached; rerun showed the sole call site is inside `horizon-g` (enumerating reference) → testimony right, census corrected in place (f3d0417b). Class: **:census-role-error** (new). Note: the census hedged in prose while stretching the role field — prose hedges don't survive joins |

### F and Q — D5 (e3dce99a) × T5 (4d2c78bc), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :policy-free-energy (F) | plan exists in effect: T5 identifies `cascade-free-energy/policy-free-energy` as THE producer (returns `{:f {candidate-id F}}` — selection's exact keying; complexity-term 0 declared WITH cited theorem `vfe_posterior_eq`); WM-11 needs-owner | **:diverges — and the distinction matters**: E=1 is a declared neutral WITHIN the law (uniform habit is still a habit); F≡0 is a declared value the law CONTRADICTS (B.2's equality case gives F = −ln P(o|π) ≠ 0). D5 adds the history: F was formally RETIRED (war_machine "the R8 tag survives the retirement of F"); even eps has zero call sites. Live posterior runs σ(ln E − 0 − G/β) | :consistent — T5's "gap is a call site" and D5's "not computed at all" are the same facts, D5 adding the retirement record. Discharging `policyPosteriorImportsPolicyF` = call the existing producer on the tick + pass :f through |
| :belief-state (Q) | R3-cat clarified from theory (filtering done, 4.13 smoothing form open — ledger ab375685) | :matches (two grains: cascade rollout-map + belief.clj entity-map, recorded per-grain) | :consistent |
| :belief-update | no plans beyond R3-cat | **:diverges, scope-stated**: the only live conditioning is channel/entity-grain `update-belief-batch` (no DarkTower carrier); cascade-grain conditioning (`exact-update`, line-for-line ExactBeliefTrajectory) is test-only. Under identity-A the open-loop rollout IS the exact trajectory for every observation the live system can produce — but the surprising-observation case Lean's Option.none exists for is STRUCTURALLY ABSENT | :consistent; T5's dynamic-resolution :caution discharged by owner sweep (no dynamic caller reaches the family) |

**Cross-cutting (D5×T5):** the census's match vocabulary gained a
principled split: *declared-neutral-within-the-law* (E=1, identity-A
as typed refusal domain) vs *declared-value-contradicting-the-law*
(F≡0). Only the second scores :diverges. Fourth path-rot instance
(DAG R1/R3 loci). Two Holes attestations now within reach of existing
producers + the certificate lane: `policyPrecisionIsGammaFromBeta`
(β declared, T2) and `policyPosteriorImportsPolicyF` (F producer built,
T5/D5).

### Preference carriers — D4 (7e85f106) × T4 (6521785d), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :C | no OWNED plans: every C node (WM-13/-delivery, WM-06/-delivery, R19) is needs-owner with no owner field; the mission-layer wiring has no DAG node at all — Joe's stated priority is reflected nowhere in assignments (recorded as coordination fact, not routed as a ruling) | **:matches**, and the canonicity question closes: `log-preference-fn` is canonical BY CONSTRUCTION (log-partition over the universe) ↔ `Preference.IsCanonical`; the live `:spec` case is exactly `Preference.constant`; zeroed outcomes ↔ `preference_eq_zero_iff` | :consistent — T4's three built-and-unused couplings all confirmed by call graph; zai independently caught the `live-c` grep pitfall (war_machine hits were the substring `accumulation-live-config`) |
| :E | :no-plans on the DAG for replacing neutral habit | **:matches** — live E is `Habit.uniform` EXACTLY (nothing writes :habit; log E = 0 in the live posterior) | :consistent |

**The complete C-carrier map (D4's key deliverable):** ONE live C in the
G (the `:spec` declared constant via `log-preference-fn`); `preferences`
C_int live at diagnostics grain only; `c-vector` REFRESHED EVERY TICK
with no live reader (H5b killed its only consumer — live compute, dead
read); `mission-c` SHADOW behind `FUTON_WM_MISSION_C` default-off,
attaching risk_mis post-selection — the mission layer exists as post-hoc
record, structurally unable to move selection; dead: `live-c`,
`ruled-outcome-c` (**F10 answered: computed-fold, NOT enabled-fold** —
fold code exists, its enable flag has no live supplier), `c-fold-config`,
`contextual-preferences`. E carriers: built and declared non-selecting at
two grains (flat: `:habit-prior-applied? false`; cascade:
`attach-log-priors` "without selecting or changing a score");
`strategic-habit/carry` executes on the tick but post-selection,
accumulate default-off.

**Cross-cutting (D4×T4):** the built-but-unwired family and the
computed-but-not-consumed family now dominate the preference layer: the
`:weights` consumer validates input it never receives; `live-c` produces
exactly that input with zero consumers; `:c-fn-pointwise` (step-indexed
Cτ) has NO caller anywhere — T1's finding re-derived from the C side.

### Model quartet — D3 (bfc6f712) × T3 (eae229c7), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :observe (A) | THE INVERSION: asked "is a non-degenerate A planned?", the answer is "it is BUILT and excluded": `token-likelihood` (exact rationals, matches Lean `TokenObservation` line-for-line) is :test-only; the live call site CONSTRUCTS identity rates itself (`efe.clj` zipmap of zero rates over the universe); `horizon-g-sparse` refuses real rates; `observation-rates` (the only real-rates producer) has no live consumer. Pre-registered finding stands: `LF-observe` accepted while its own note says no mixed-rates theorem exists | :matches | :consistent (T3 testified exactly this; θ caveat settled by owner rerun: no live pattern carries :theta) |
| :transitions (B) | fully live AND Lean-aligned — the quartet's one healthy member (cascade-kernel ← rollout ← R4/R5/R6 consumers; theorems correspond) | :matches | :consistent |
| :initial-belief (D) | point mass live-wide (`observed-belief`); non-degenerate `independent-belief` :test-only | :matches | :consistent |

**Cross-cutting (D3×T3):** both degenerate sides of the quartet are what
runs; every degeneracy is DECLARED, not silent — certifiable. `horizon-g`
already computes full G (ambiguity + step-indexed C via :c-fn; "constant
C is the special case, not the definition") but enumerates the powerset;
`horizon-g-sparse` buys scale by assuming away exactly the
non-degeneracy. **The ambiguity known-failing and T1's Cτ hole are one
reduction seen twice; the gap is a SCALABLE non-degenerate evaluation
path, not new modeling.** The built-but-unwired family now has three
members with zero source requirers: `live_c`, `likelihood_precision`
(ζ's Clojure, R7's exact shape), `observation_rates`. This stack's
characteristic failure is not missing code — it is built code nothing
calls.

### Selection cluster — D2 (38a57c8e) × T2 (454fa6d8), compared 2026-09-18

| term | P↔S | B↔S | P↔B |
|---|---|---|---|
| :temperature (β) | no plans; on the live path β is a DECLARED INPUT CHANNEL (per cascade-problem, distinct betas → :incommensurable-family, no default), not a computation. The dead eq.-2.7 solver (`policy-precision/converge-beta`) has **no Lean declaration at all** — a Clojure computation outside the model, though dead. Pre-registered `LF-temperature` "three laws conflated" finding: untangled on the Lean side by the census correction (`AIF.Selection`); DAG node text is claude-4's to fix | **:matches** | :consistent (T2 lists β *consumers* as claims-built; compatible framing) |
| :policy-posterior | **:spec-census-error, found and fixed**: the census had bound the base law; T2 proved production implements the tempered law (γ=1/β on G only, F unscaled); corrected at darktower 99130e5fc3 + re-pin fd8d5f8ee0 BEFORE the D2 comparison ran | **:matches** input-by-input vs `PolicySelection.selectionWeight` (infinite-G → exactly 0 both sides; zero-normalizer ↔ :no-admissible-candidate typed refusal; Clojure-only runtime rules — tie-break, log-sum-exp — noted, uncontradicted) | :consistent. New duplicate found: `cascade_policy/cascade-policy-posterior` + `select-over-cascades`, same law, zero src callers, :test-only |
| :action | no plans | **:matches** (`bayes-choice` over first acting pattern; `authorize` on the admissible set) | :consistent |

**Cross-cutting (D2):** `policy-precision` (R14's converge/carry-β) is
DEAD on the tick — reachable only via `beta-dark-carry`, whose only
callers are tests and lab scripts; the completeness doc's "R14 ✓ LIVE
feed" status has silently regressed or was recorded against a
now-removed path. F is the cluster's one construction gap (`:f` defaults
0; WM-11 needs-owner; Lean side `policyPosteriorImportsPolicyF` sorry,
evidence = a run record carrying per-policy F). E is `Habit.uniform` by
declaration. **Study health note: in two rounds, each witness has been
corrected once by the process** — D1 corrected the built-census, T2
corrected the spec census. No witness is privileged; the reruns are.

**Cross-cutting findings (D1):** `active-horizon-g` is Lean-aligned and
DEAD (test-only island; nothing from the tick reaches it) → new class
**:aligned-but-dead**, the facade-adjacent case: elaboration without
reachability. `efe/compute-efe` dead on the tick (verified: zero call
forms outside its file). Three Clojure kernels of one Lean `outcomeRisk`;
three-plus G computations across grains. The DAG's `sparse-g` field has
file:line rot (497/536 vs actual 505/555) — independent evidence for
name-based addressing.
