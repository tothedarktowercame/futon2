# 3/2 categories in the War Machine

This note explains the category-theoretic work now being used to make War
Machine pattern cascades real runtime objects rather than lists of pattern
names.  It is an orientation document, not a claim that the bridge is complete.

## What we are modelling

A policy is a cascade of interpreted design-pattern occurrences.  Each
occurrence is represented by one finite 3/2-blend square:

```text
             a1                 b1
        G --------> I1 ----------------> B
        |                                  ^
     a2 |                                  | b2
        v                                  |
        I2 --------------------------------+
```

The four objects are finite theories:

- `G`: the grounded/source situation;
- `I1` and `I2`: two partial interpretations or input views;
- `B`: the blend produced from them.

The arrows are partial functional maps.  Auxiliary flags can mark a triangle
as intentionally absent from the active consistency check.  A square is useful
only when its objects and maps are well formed, its required routes commute,
its active routes are consistent, and both cone legs cover the blend.  Lean
proves that this executable finite coverage test corresponds to the semantic
3/2-pushout predicate used by the model.

Several squares form a policy only when explicit gluings identify objects in
one square with objects in another.  A gluing is not an ordering hint: it needs
the two square identities, the two object roles, a genuine isomorphism graph,
its inverse, total endpoint coverage, and axiom preservation in both
directions.  Connected, validly glued squares constitute a cascade; disconnected
squares remain a bag.

## Why this is before G

Expected free energy ranks policies.  It must not manufacture the policies it
ranks.  The categorical receipt is therefore an admission boundary before G:

```text
pattern readings
    -> construction relations
    -> per-occurrence 3/2 squares
    -> multi-square gluing/pasting
    -> application and transition correspondence
    -> admitted CascadeEFE policy
    -> G scoring and posterior selection
```

If structural evidence is missing, the composition root records a typed
refusal.  It must not infer a square from a pattern name, infer a gluing from
equal printed labels, or rename the existing precedence vector as categorical
evidence.

## Implemented certificate slices

The implementation is additive.  Each completed slice has positive and
adversarial witnesses.

1. **Token-order construction receipt.** Clojure emits token-supported edges,
   closest-common-origin meets with witnessed paths, and a clean linear
   extension.  Lean's `ConstructionReceipt.valid` checks the same carrier.
   Invalid old-direction meets refuse before scoring.
2. **One source-pinned square per occurrence.** Runtime receipt
   `:wm/three-halves-square-v1` records named `G/I1/I2/B` theories, all four
   map graphs, and auxiliary flags.  Validation checks source/theory/map shape,
   required commutation, and consistency.
3. **Pushout coverage.** Coverage is computed from `B` and the images of `b1`
   and `b2`; no caller supplies a validity Boolean.  Lean theorem
   `valid_projects_semantic_pushout` projects a valid runtime receipt to
   `IsThreeHalvesPushout`.  Runtime reports the concrete uncovered identities
   and refuses the candidate before G.

Primary code and proofs:

- `mathlib4/DarkTower/WarMachine/ThreeHalvesBlend.lean`
- `mathlib4/DarkTower/WarMachine/ThreeHalvesRuntimeCertificate.lean`
- `mathlib4/DarkTower/WarMachine/ThreeHalvesRuntimeWitness.lean`
- `mathlib4/DarkTower/WarMachine/ConstructionReceipt.lean`
- `src/futon2/aif/three_halves_square.clj`
- `src/futon2/aif/interpretation_construction.clj`
- `src/futon2/aif/wm/cascade_decision.clj`

The pinned `ukrns/publication-cadence` fixture is the current positive
runtime-shaped witness.  Removing a cone-leg pair leaves one blend identity
uncovered and is rejected by Lean, the focused validator, and the composition
root.

## What remains open

### Multi-square gluing

Runtime currently stores independent squares keyed by pattern ID.  It records
token support, common-origin paths, and precedence, but no cross-square object
identification or isomorphism.  The missing producer is approximately:

```clojure
{:schema :wm/three-halves-policy-v1
 :squares [{:square-id id :receipt <three-halves-square-v1>} ...]
 :gluings [{:from {:square-id i :role :G|:I1|:I2|:B}
             :to {:square-id j :role :G|:I1|:I2|:B}
             :iso #{[source-name target-name] ...}
             :source {:path path :sha256 digest}
             :reading text} ...]}
```

This evidence must be authored and source-pinned.  It cannot be synthesized
from precedence or coincident names.

### Pasting

Lean defines `pastedDiamond` and proves the conditions under which vertically
pasted pushouts remain pushouts.  Runtime cannot certify pasting before the
shared-object/gluing carrier exists.

### Application order

Lean deliberately separates `Policy.applicationOrder` from construction order.
Runtime currently uses the same `:precedence` vector for construction, model
rollout, G scoring, and enactment.  The meaning of an application-order triple
and its correspondence to observed execution still need specification.  See
`APPLICATION-ORDER-AUDIT-2026-10-11.md`.

### Predictive-kernel adequacy

G consumes a `CascadeEFE.Policy` with guarded transitions and a predictive
kernel.  We still need the theorem and runtime receipt showing how a validated,
glued 3/2-blend policy yields exactly that carrier.  Successful post-hoc scoring
is not a substitute for pre-G admission.

## Working rules

- Read the Lean carrier before changing the runtime representation.
- Extend certificates by adding evidence; do not weaken existing definitions
  to fit current Clojure.
- Keep construction, application, prediction, and enactment identities
  distinct until a certificate proves their correspondence.
- Every acceptance condition needs a falsifier that fails at the composition
  root before G.
- Use a runtime-shaped positive fixture and retain exact source bytes/digests.
- A blocker is a useful result.  Record the missing producer or theorem
  signature instead of inventing categorical data.

The detailed evidence ledger and correspondence tables live in
`PLAN-post-click-51-recovery-2026-10-10.md`.
