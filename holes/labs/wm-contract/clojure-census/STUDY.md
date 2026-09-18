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
