# Proof ⟨1⟩3 ⟨2⟩2: class-C scoring in the live joint decision — DESIGN ONLY

zai-1, 2026-09-22. Read-only; no code. Substitution point:
`scripts/futon2/report/war_machine.clj` :6216-6262 — `live-c/cascade-spec`
builds the `:cascade-spec` handed to `efe/rank-actions`; this design replaces
that spec's C with Joe's class preference for the joint family. Brief:
codex-20's diagnosis in PROOF ⟨1⟩3.

## 1. End-state → class mapping

Per candidate, at the common horizon T, over its rollout's terminal belief
(the token-state distribution `(:belief (m/rollout … T))`):

- **Reached** (the candidate's terminal/acceptance token for a target is
  present in a state with mass > 0): the state's class is decided by the
  TARGET's facet, exactly as `run_ending_classification.clj:66-96` decides an
  attested increment's class — facet rows come from the focus receipt
  (`focus_receipt.clj:23-28`), and the facet of a target is computed from its
  commit paths by the `facets` regex (:15-17, now including
  `resources/wm/`). Then:
  - facet == the current focus (discovered focus, e.g. WM) → **focused
    (0.55)**;
  - facet ∈ the facet-graph background (the same-focus edges, e.g. WM↔APM at
    `commit-facets-v1.json :facet-edges`) → **related (0.35)** — this is how
    "related" is decided today: `focus_receipt.clj:47-56` carries
    `:facet-graph {:active [focus] :background [...]}` from the edge list;
  - any other facet → **unrelated (0.05)**.
- **Not reached** (no acceptance token in any positive-mass terminal state —
  obstruction persists, the cascade stopped early, or per Joe's ruling the
  outcome is left unmeasured) → **stop-the-line (0.05)**, per Joe's ruling of
  2026-09-22 (an unmeasured outcome belongs to stop-the-line). This reuses
  the acceptance meanings at `run_ending_classification.clj:66`: a typed
  failure or an attestation-less ending is not an increment; prospectively
  it maps to the stop-line class.

The pushforward: `class-projection Q(s) = Σ_s Q(s)·δ(class(s))` — a linear
map from the token-state distribution to a four-slot class distribution.

## 2. Where the class preference enters

The per-τ preference `Cτ` is consumed at EVERY horizon step, not only the
terminal one: `cascade_observation_scoring.clj:70-76` builds `preference`
per τ (`:probabilities` per step) and `om/query {:op :score :belief next-q
:preference …}` (:88-91) scores each step's belief; `horizon-g`
(`cascade_model_manifest.clj:542`, Lean `PolicyHorizon.horizonEFE`) sums the
steps. The contracted functions: **`preference-distribution`**
(`cascade_model_manifest.clj:491`, Lean `TokenPreference.preference`) builds
C over the outcome space, and **`horizon-g`/`om/query :score`** consume it.
Design: the outcome space for C becomes the four CLASS slots; each step's
belief is projected by the class pushforward of (1) before preference is
applied (a state carrying a mixture of class outcomes scores by its class
mixture — aggregation choice declared on the certificate, per the
"aggregation changes KL" note). The `:cascade-spec` at :6259-6262 carries
`{:want <class slots> :c {:status :class-projection :source "Joe 2026-09-22
ruling + PROOF-wm-works 1.3"} …}` in place of the live-c token weights.

## 3. Ambiguity

Confirmed: A is the identity kernel today (zero adjudication rates —
`efe.clj:1096`, the WIRE-4 comment), so `step-ambiguity`
(`cascade_model_manifest.clj:522`) is 0 at every step, and the risk term is
exactly KL of the class pushforward against C at each scored step, summed
over the horizon: G = Σ_τ KL(proj_τ(Q) ‖ C). No epistemic bonus enters; the
only terms are Joe's preference and the rollout.

## 4. Expected result on the reference input (numbers)

With today's frozen q₀ (evidence absent) and the rewritten source
(`71a0d949`):

- **C1** (recheck; guard fails, target NOT reached) → class distribution
  [0, 0, 0, 1] → G(C1) = ln(1/0.05) = **2.996**.
- **C2** (declare → collect → calibrate → accept; `restoration-accepted`
  reached on the ticket target whose commits facet WM = current focus) →
  [1, 0, 0, 0] → G(C2) = ln(1/0.55) = **0.598**.
- log E − F: tie (no habit history for either family; F not supplied), so
  the score is −G/β: C2 = −0.598 > C1 = −2.996 → **unique maximum at C2**;
  the name-order tie-break would have picked C1. G decides — by Joe's
  55-vs-5 numbers, no u, no entropy effect, no regex defect (the facet fix
  `aec603a7` puts the ticket's commits in WM).

## 5. Blast radius and invariants

**Touches:** every target in the joint family is scored through the same
class spec (mission candidates' acceptance tokens project by their mission's
facet the same way); the certificate's `:c` provenance changes from
`:derived` (live-c) to `:class-projection` with the ruling's id; the
`:no-reachable-want` grain-mismatch branch (:6222-6240) becomes unreachable
for families where every candidate has a class outcome — keep the branch and
its refusal for genuinely classless inputs rather than deleting it; live-c's
derivation can still be computed and recorded for audit without entering the
score.

**Unchanged:** the law σ(ln E − F − γG) exactly as written in
`policy.clj:66-100 selection-scores` and `cascade_selection.clj`
selection-posterior; the ticket-front eligible-stratum rule
(`policy.clj:358-374`, `ticket_queue.clj:89-115`); the declared tie-break
`:action-name-ascending` (`cascade_selection.clj:126-129`, still never used
as evidence); habit E's read and attach; F's attach rules; β's
declared-not-defaulted rule; the qualification scheme at :6145; the
load-identity, habit and E1 gates.
