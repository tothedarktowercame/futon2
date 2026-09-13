# Row 22 E5/E6 semantic integration alternatives

Date: 2026-09-13. Status: read-only design analysis. No mapping is adopted,
implemented, or credited as influence.

## 1. The declarations currently name different quantities

The adopted E5 law preserves `apply-slow-prior` exactly: for a move of class
`k` and slow-mode weight `w_k`, it produces

```
p'_move = p_move * w_k
delta'_move = delta_move - log(w_k).
```

The function describes both outputs as rollout inputs
(`temporal_hierarchy.clj:111-180`). `rollout/validated-priors` normalizes the
positive `:prior` values over the reachable move population; later rollout
costing reads the move score/cost path (`rollout.clj:215-262,294-318,382-430,
455-538`). Thus the two E5 fields have operational meanings inside a tree
search: one changes expansion mass and one changes accumulated path cost.

Live R6 has different operands. `efe/compute-efe` produces the complete
multi-objective `:controller-score` G and `rank-actions` orders it
(`efe.clj:475-590,780-1019`). The selector consumes G, the independently
learned or structural `:habit-prior-bias` as ln E, optional F_pi, and tau:

```
score(pi) = ln E(pi) - G(pi)/tau - F_pi(pi)   ; when F_pi is enabled/unscaled
Q(pi) = softmax(score(pi)).
```

The precise branches and scaling are in `policy.clj:157-241,544-676,790-874`.
The WM resolves the habit source and mode separately before that call
(`war_machine.clj:925-1019,6411-6550`). Neither R6 component reads E5's move
prior or move delta. The missing assumption is therefore not a field name. It
is a mathematical declaration relating a slow-mode distribution over move
classes to a complete policy occurrence's E and/or G.

## 2. Compatible contracts that could be reviewed

### A. Search-only composition

Run `hierarchical-rollout` before R6 and let its returned policy/candidate
support become R6 input. This preserves the existing meaning of both E5 fields:
they affect rollout expansion and path cost, while R6 continues to compute its
own G and habit prior. It is compatible only if the required edge is explicitly
defined as slow state -> rollout construction -> R6 support. It cannot qualify
the currently required fixed-domain score influence, and it cannot be smuggled
in by selecting a convenient rollout field. A changed support is a support
effect, not proof that R6 scores changed on one immutable domain.

### B. Policy-prior composition

Define a new policy-level slow potential from the ordered policy construction,
for example a reviewed aggregation of the constituent move-class weights, and
combine it with the learned habit authority under an explicit product-of-experts
normalization. Only the resulting declared policy prior would yield an
additional log-potential at the selector's single ln-E seam. This is not the
existing move `:prior`, and it must not overwrite or masquerade as learned
habit. The move-cost delta remains rollout-only.

This preserves the selector's meaning if a new declaration proves the
move-to-policy aggregation and combination rule. It changes the policy prior,
so it needs formal review, a named authority, and a compatibility theorem for
the weight-one case. Merely copying `:prior` into `:habit-prior-bias` is not this
contract.

### C. Controller-cost composition

Define a new G contribution from complete-policy slow costs, with an explicit
aggregation of `delta' - delta = -log(w)` and a named sign/unit convention.
Add it exactly once to `:controller-score`, retaining base G and the new term.
The move `:prior` remains rollout-only. This can preserve G's additive-cost
interpretation, but only after proving that the EFE/controller objective is the
authority for this particular policy cost. Copying a move delta into G is not a
valid aggregation and could duplicate costs already represented by risk,
preferences, structural pressure, or rollout construction.

### D. Dual composition

Feeding both a slow prior into ln E and its `-log(w)` penalty into G is not a
neutral transcription. For one weight and otherwise fixed values, the direct
score change would be

```
log(w) - (-log(w))/tau = log(w) + log(w)/tau.
```

The same slow preference is therefore counted twice (and with a tau-dependent
relative strength) unless a new generative-model derivation explicitly calls
for both factors. The fact that rollout uses both for exploration and path cost
does not prove that the one-step R6 posterior should do so again.

## 3. Recommendation requiring authority review

Recommend **B, a new policy-level slow potential at the existing single prior
seam**, while keeping E5's current two move fields and rollout behavior intact.
It most directly expresses the adopted statement that slow state shapes a
forward prior, and it avoids changing canonical G, clip behavior, temperature,
F_pi, or learned-habit evidence. This is a recommendation, not an adopted
mapping.

The review must decide all of the following before implementation:

1. how an occurrence's complete policy construction maps to an ordered sequence
   of move classes;
2. whether its slow potential is a product, sum of log weights, terminal-only
   weight, or another declared aggregation;
3. how that potential combines with learned E (product-of-experts, mixture, or
   replacement) and which normalization/support measure is authoritative;
4. whether unclassified moves contribute identity weight or typed absence;
5. whether the existing E5 cost modulation stays search-only; and
6. the exact weight, positivity, finite-support, and tau assumptions needed for
   the corresponding Lean statement.

Until those questions are ruled, production correspondence must refuse
`:e6/slow-policy-potential-authority-missing`. No fixture label or candidate
field may satisfy it.

## 4. Discriminating formal and isolated checks

The following checks distinguish the alternatives without claiming a real
edge firing:

- **Identity:** all weights one gives byte-identical R6 scores/posterior and
  unchanged support under each proposed adapter.
- **Prior-only:** with fixed G/F_pi/tau and two equal-base policies, changing one
  class weight changes only the named policy-prior term. Learned-habit bytes
  remain fixed and separately visible.
- **Cost-only:** with fixed E/F_pi/tau, a declared policy cost changes only the
  named G contribution; base G remains retained. Sign and tau response must
  match `-G/tau`.
- **Dual-count detector:** compare prior-only, cost-only and dual formulas at
  `tau != 1`; dual must not be accepted as either single-factor contract.
- **Construction sensitivity:** equal semantic actions at different occurrence
  ids but different policy constructions receive their own derived potential;
  swapping ids or construction bytes refuses.
- **Complete-domain conservation:** every input occurrence appears once, in
  order, in base and shaped score/posterior tables. Drop, reorder, deduplication,
  borrowed model/run/tick, missing source, unknown class, invalid weight and
  unsupported aggregation refuse.
- **Float/real separation:** the isolated Clojure witness recomputes the actual
  IEEE functions and reports every delta; Lean states the analytic finite-real
  law and any floating correspondence needs a separately bounded residual.
- **Influence:** two fixed-context arms may demonstrate predicted score and
  selection differences, but remain counterfactual machinery. E6a qualifies
  only after the real retained E5 -> R6 -> E2a/E3/E2b chain changes the approved
  selected and enacted occurrence.

The existing Row 18 field is not a positive check: it remains 147 occurrences,
145 aligned F_pi values plus two typed absences, with `:log-prior-placement
:none` rather than the theorem's `:both`, and it fails the sufficient
`c > 3R/2` uniqueness condition. No alternative above changes that evidence.

## 5. Remaining authority boundary

The smallest next packet is a decision/specification, not source integration:
declare the policy-construction carrier, slow-potential aggregation, combination
with learned habit, and typed unclassified behavior; then state its analytic
law in a new unfrozen module and commission A/B/C/D discrimination in isolated
data. Only after independent review may one new source adapter and its complete
capture be proposed. Runtime wiring, selection, enactment, and E6 admission
remain separate later work.
