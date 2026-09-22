# Proof ⟨1⟩3 ⟨2⟩2: prospective ending kernel and C — discovery (read-only)

zai-1, 2026-09-22. Sources read: `run_ending_classification.clj` (classify*
:63-108; declaration `resources/wm/run-ending-classification-v1.edn`),
`focus_receipt.clj` (:12-56 discover, facets regex :15-17), `live_c.clj`
(:104-230 read-sources/derive-live-c), `cascade_model_manifest.clj`
(preference-distribution :491, horizon-g :542 = Lean `PolicyHorizon.horizonEFE`,
with-pattern-theta :271, q-support-outside-c-universe refusal ~:782),
`war_machine.clj` joint decision (:6105-6180), the frozen reference input.

## Answers

**1. Class assignment — YES, both candidates land in the SAME class with
probability 1, so class-space C alone cannot pass the ⟨1⟩3 counterfactual.**
Mechanics: an increment's class comes from the focus-receipt's facet rows
(`classify*` :85-96, `:facet-map` in the declaration). Facets are computed
from the commit's PATHS by the regex at `focus_receipt.clj:15-17`
(`(^|/)wm_` requires an underscore; `war_machine`, `M-wm-`, … likewise).
Both candidates' predicted commits are `resources/wm/rechecks/…edn` (C1) and
`resources/wm/eig/held-out-split.edn` (C2) — NEITHER matches the WM facet
regex (`wm/rechecks` has no underscore after wm), so both facet as
`other/unattributed`, both land in the same class (`elsewhere-useful` if the
current focus is WM), and under the point-mass declared prior both succeed
deterministically. (Discovery note, not fixed: `resources/wm/…` paths
faceting as non-WM looks like a regex gap — flag for Joe, no change here.)
Symmetry is total: each candidate produces exactly one of the two wanted
tokens (coverage 1/2 each), same class, θ = documented default 1, no
recorded rates for either family, equal (zero) habit history.

**2. What can differ in the declared prior.** The only runtime carrier of a
non-point-mass rollout is pattern θ (`with-pattern-theta` :271-280; kernel
:282-300: θ to the produced state, 1−θ stay). Lawful θ values are recorded
success rates (NONE exist for either family — addendum 1 of the q0 file) or
the documented default 1. **Any per-candidate θ < 1 would be an invented
number.** The remaining lawful difference is structural, not numeric: C2's
declared effect explicitly produces NO measured outcomes (a precondition
artefact whose own acceptance names no-result/failure/timeout classes),
while C1's recheck produces a measured disposition. That is a declared,
checkable difference in each action's PREDICTED EVIDENCE OBLIGATIONS, not a
tuned probability. The contracted risk path accepts it: `horizon-g`/step
scoring consume a per-τ preference map via `om/query`
(`cascade_observation_scoring.clj:70-76,88-91`); nothing forces the
preference to be a point mass.

**3. Carrier today.** The live joint decision computes TOKEN-space G:
`war_machine.clj` joint decision derives C via `live-c/derive-live-c`
(mission-id tokens: `:alive/*`, `:closed/*`, `:star/*` — weights from
wholeness L, status, star map — NOT Joe's 55/35/5/5) and scores through
`efe/rank-actions` → `cascade_observation_scoring/score-candidate` →
`om/query {:op :score …}` with `cascade_model_manifest/horizon-g`
(Lean `PolicyHorizon.horizonEFE`). Class-space risk is computed by NO live
caller today; the class kernel (`run_ending_classification.clj`) is
record-only and runs at close, not at selection. Note both produced
`:repair/*` tokens appear in NO live-c want set, so today's token C is also
flat between C1 and C2.

**4. Unknown mass.** Today the scorer refuses rather than renormalising:
a belief state carrying tokens outside C's universe is the typed refusal
`:q-support-outside-c-universe` (`cascade_model_manifest.clj` ~:782-790,
the WM-06 domain-meeting rule). That is the right spine for the kernel:
unknown mass must be an EXPLICIT outcome in the preference map with a stated
value, never dropped-and-renormalised (which would flatter the
least-observed action — the exact trap ⟨2⟩2 names).

## Smallest design that discriminates without inventing numbers

1. **Prospective kernel** (new, pure, record-only-adjacent): for each
   candidate, map its PREDICTED post-build evidence to a class distribution
   `{class p, :unknown u}` where `u` is the declared share of outcomes the
   action's acceptance explicitly leaves unmeasured — computed from the
   candidate's own declared structure (C2's no-result classes ⇒ u > 0; C1's
   measured disposition ⇒ u = 0). Numbers come from the declarations, not
   from tuning.
2. **C = Joe's 55/35/5/5, fixed, over the four classes**, plus ONE extra
   explicit outcome `:unknown` whose preference value **Joe sets once, as a
   ruling** (a single global number, e.g. "unknown counts as elsewhere" or
   "unknown is slightly worse than elsewhere"). This is a Joe decision of
   the same kind as 55/35/5/5 — not a per-candidate parameter.
3. **Risk through the contracted functions:** the kernel's per-τ preference
   map feeds the existing `om/query :score` / `horizon-g` path in place of
   the live-c token weights (both are contracted carriers; aggregation
   choice — class carrier — declared on the decision, per the plan's
   "aggregation changes KL" note).
4. Everything else unchanged (habit E, F, β, the tie-break, the law).

**Effect:** C1 scores class c with u=0; C2 scores the same class c with
u>0 ⇒ different expected log-preference ⇒ different G ⇒ the counterfactual
passes WITHOUT any tuned number. **If Joe declines to rule a value for
:unknown, then no design exists that discriminates these two candidates
without inventing numbers** — the declared effects are perfectly symmetric
(same class, same coverage, same θ, no recorded rates) — and under execution
rule 2 the proof stops for Joe's decision, exactly as it did at ⟨1⟩2.

## Addendum (2026-09-22, after Joe's ruling 27f5b8d8): what G does under C = 55/35/5/5, unmeasured → stop-the-line

Joe's C over [focus, related, unrelated, stop-line] = [.55, .35, .05, .05].
Risk = KL(Q‖C) = Σ q ln(q/c) (lower G better); log E − F is a tie (no habit
history), resolved by name order to :C1 (`:apparatus/…` sorts before
`:aif/…`). Focus today: the discovered facet from
`resources/wm/focus/commit-facets-v1.json` (two windows, 165/166 commits,
WM↔APM same-focus edge) — with the commit mass under `src/futon2/aif/` (the
regex matches `/aif/`), **focus = WM**.

### Case (1) — code as it is (both candidates' commits facet `other/unattributed` → class unrelated, 5%)

| | G |
|---|---|
| C1 (point mass at unrelated) | ln(1/.05) = **2.996** |
| C2, u=0.10 | 2.671 |
| C2, u=0.25 | 2.433 |
| C2, u=0.50 | 2.303 |
| C2, u=0.75 | 2.433 |

**G picks C2 for EVERY u > 0** — mathematically forced, not a coincidence:
unrelated and stop-line carry the SAME c = .05, and KL is convex, so any
mixture of two equal-c slots beats a point mass in one of them:
G(C2) = ln 20 − H(u) < ln 20. Two consequences: (i) Joe's ruling is inert
here — moving C2's unmeasured share between two 5% slots changes nothing
(claude-5's point (a), confirmed exactly); (ii) G changing the choice (C1 by
tie-break → C2 by G) would PASS the counterfactual — but it passes **because
of the facet-regex gap**: the WM repair is classed unrelated, and the win
comes from entropy, not from Joe's preference. That is a defect deciding the
demonstration, not the model.

### Case (2) — regex fixed (resources/wm/ … matches WM → both candidates class focus, 55%)

| | G |
|---|---|
| C1 (point mass at focus) | ln(1/.55) = **0.598** |
| C2, u=0.10 | 0.513 — **C2 wins** |
| C2, u=0.20 | 0.577 — C2 wins |
| C2, u=0.25 | 0.635 — **C1 wins** |
| C2, u=0.50 | 1.104 — C1 wins |
| C2, u=0.75 | 1.834 — C1 wins |

Crossover at **u\* ≈ 0.219**. The direction genuinely depends on u.

### Is u declared anywhere? NO — plainly.

The source file declares no number; the parent mission's C2 names the
no-result/failure/timeout classes with no shares; the split itself is C2's
own future work product and does not exist at selection time. **Any specific
u at selection time is invented.** The only non-invented route is a
STRUCTURAL derivation Joe would have to bless — e.g. u = (declared
unmeasured outcome classes) / (all declared outcome classes) of the
candidate's acceptance (C2 declares {split-present, no-result, failure,
timeout} → u = 3/4; C1 declares {recheck-present} → u = 0). Note this
scheme's value (0.75) sits WELL past the crossover, so it would pick C1 —
but the scheme itself is a modeling choice that flips winners, which is
exactly the kind of decision rule 2 reserves for Joe.

### Verdict

- As the code is: G picks C2 for any u > 0, differing from the tie-break's
  C1 — but on the strength of a regex defect, with Joe's ruling inert.
- With the regex fixed: the winner depends on u, and u is nowhere declared;
  every concrete u is invented unless Joe rules a structural derivation.
- **Either way the ⟨1⟩3 counterfactual cannot be computed lawfully today.**
  The regex fix is necessary (Case 2 is the honest world) but not
  sufficient: Joe must either rule a structural u-scheme or accept a
  different reference input.

## Addendum 2 (2026-09-22): under Joe's amended check (tie → unique maximum counts as G deciding)

With log E − F a tie and F not supplied, the full law σ(ln E − F − γG)
reduces to ranking by −G: the unique maximum is simply the strictly lower G.

- **Case 1 (code as-is, both unrelated at 5%): PASSES the amended check for
  any u > 0, with a unique maximum at C2 — and the DIRECTION is
  u-INDEPENDENT** (G(C2) = ln 20 − H(u) < ln 20 = G(C1) for every u ∈ (0,1),
  minimum at u = 1/2). So no invented u is needed here — only u > 0, which
  is structurally declared (C2's acceptance names unmeasured outcome
  classes; the EXISTENCE of unmeasured mass is declared, its magnitude never
  enters). But the demonstration is hollow in a specific way: the choice is
  made by −H(Q) over two equal-preference slots — Joe's 55/35/5/5 and his
  stop-line ruling are both inert — and it rides on the facet-regex gap that
  classes a WM repair as unrelated.
- **Case 2 (regex fixed, both focus at 55%): FAILS "no invented u" as it
  stands.** The winner flips at u\* ≈ 0.219, so G depends on u's VALUE,
  which is nowhere declared. The structural scheme (u = unmeasured-declared
  classes / all declared classes → 3/4) would give a unique maximum at C1 —
  agreeing with the name-order pick, which the amended check now accepts —
  but the scheme itself is Joe's to rule.

**Revised verdict:** with the amended check, Case 1 technically passes
without any invented number (u's existence is declared; its value is
irrelevant to the direction) — but it decides via a regex defect plus
entropy between two equal slots, with Joe's entire preference structure
inert. Case 2 is the honest world and needs one Joe ruling: bless the
structural u-scheme (unique maximum at C1, direction then value-independent
at 3/4 ≫ u\*) or choose a different reference input. I recommend presenting
exactly this trade to Joe rather than banking the Case 1 pass.
