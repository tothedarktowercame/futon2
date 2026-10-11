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

The arrows are partial functions on element names.  Square-leg
`PMap.wellFormed` checks endpoints and functionality, not preservation of the
theories' axioms; axiom preservation is currently required only for gluings.

Auxiliary flags have exact, deliberately limited semantics.  Either flag drops
the on-the-nose commutation requirement for the whole square; each flagged
triangle is omitted from the active consistency join.  With both flags set,
consistency is vacuous.  Coverage is different: auxiliary flags do not exempt
either cone leg, and the images of `b1` and `b2` **together** must cover `B`.
Lean proves that consistency plus this joint finite coverage corresponds to
the model's semantic 3/2-pushout predicate.  That predicate is a lax/maximal
mediator construction for cones carrying the same auxiliary flags, not the
ordinary unique-mediator pushout readers may expect.

Several squares form a policy only when explicit gluings identify objects in
one square with objects in another.  A gluing is not an ordering hint: it needs
the two square identities, the two object roles, and a total, surjective,
functional graph whose transpose is also functional and which preserves
axioms in both directions.  Lean's current cascade verdict means a connected
graph of individually well-formed pairwise gluings.  It does **not** yet prove
that isomorphisms compose coherently around cycles or that duplicate gluings
agree; any colimit or whole-cascade pasting claim needs that additional law.

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
   required commutation, and consistency.  Lean's `ObjectReceipt.valid` checks
   only the digest string's shape; correspondence with captured source bytes is
   a runtime boundary outside that Lean predicate.
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
root.  The measured flag matrix now has executable Lean and Clojure witnesses:
an inconsistent, noncommuting but jointly covered square refuses with neither
flag, passes with aux1 only, passes with aux2 only, and passes with both flags.
With both flags, the active-route consistency join is empty and therefore
vacuous.  Coverage is still joint and mandatory: the same both-aux receipt
refuses when identity 32 is removed from the union of the two cone-leg images.
A separate partial-route witness is consistent and covered but refuses solely
for noncommutation.  These witnesses document the current semantics; they do
not endorse or revise it.

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
from precedence or coincident names.  `Policy.wellFormed` checks each gluing
separately and does not yet enforce diagram coherence.  `Policy.components`
also traverses all supplied gluings, including malformed ones; only
`components_cover` is proved, not exact-once component membership or an
equivalence with ordinary graph connectivity.  A runtime diagnostic must not
present those raw components as certified connectivity without first filtering
or validating the gluings.

The executable malformed-merge witness makes that distinction concrete: an
empty, non-total gluing between two nonempty `G` objects merges the raw graph
to `[[0, 1]]`, while `Policy.wellFormed = false` and `Policy.verdict` is
`.bag [[0, 1]]`, never `.cascade`.  The Lean API documentation therefore calls
`Policy.components` **raw-input components**.  Repository tracing found no
futon2 three-halves policy/component renderer or Morning Brief consumer; the
only runtime implementation currently validates one square.  If such a report
is added, it must label these as supplied/raw components, or expose admitted
connectivity only after the policy well-formedness gate.

#### Cycle coherence (Lean model; runtime carrier still missing)

Pairwise validity is not diagram coherence.  The smallest executable witness
uses three identical two-element objects: the `0→1` and `1→2` gluings are
identity maps, while the supplied `0→2` gluing swaps the two elements.  Every
gluing is individually well formed, the raw graph is connected, and the
existing verdict is `.cascade`, but the composite transport is not the direct
transport.  `GluingTriangleCertificate.valid` is an additive check for an
explicitly supplied triangle; it rejects this witness and accepts the same
policy with an identity direct edge.  It does not strengthen
`Policy.wellFormed` or redefine `Policy.verdict`.

| Lean evidence/check | Runtime evidence today | Exact missing producer field |
|---|---|---|
| Stable indices into `Policy.gluings` | No multi-square policy receipt | Stable `:gluing-id` on every admitted gluing |
| Triangle `(first, second, direct)` | None | `:coherence-triangles` entries naming the three gluing IDs |
| Matching square indices and endpoint roles | No cross-square endpoint carrier | Source-pinned `:from`/`:to` square IDs and roles for each gluing |
| `PMap.comp first.iso second.iso = direct.iso` | No transport graph carrier | The complete finite `:iso` graph for each gluing, retained exactly |
| Coverage of all cycles | None; not proved in Lean by this slice | A source-pinned complete fundamental-cycle basis (for example a spanning forest plus one triangle/path certificate per non-tree edge) |

A caller-supplied list of successful triangles may omit the bad cycle.  Before
coherent connectivity can justify pasting or any colimit claim, Lean still
needs (1) a theorem that the supplied certificates cover a cycle basis of the
admitted gluing graph and (2) path independence derived from coherence on that
basis.  Runtime correspondingly needs an independently produced, source-pinned
complete basis, not a selector-authored subset.

### Pasting

Lean defines **vertical** `pastedDiamond` and proves one preservation theorem,
`prop8`.  It requires on-the-nose shared-object/map equalities
`d2.G = d1.I1`, `d2.I2 = d1.B`, and `d2.a2 = d1.b1`; a general gluing supplies
only an isomorphism.  Runtime therefore needs either identity-restricted
gluings or a new theorem transporting pushouts along those isomorphisms.
`prop8` also requires the second square to commute on the nose.  Since an
auxiliary flag can admit a square without that commutation check, such a square
cannot automatically serve in the second position.  The gluing carrier is
necessary but not sufficient for pasting.

### Application order

Lean deliberately separates `Policy.applicationOrder` from construction order.
Runtime currently uses the same `:precedence` vector for construction, model
rollout, G scoring, and enactment.  The meaning of an application-order triple
and its correspondence to observed execution still need specification.  See
`APPLICATION-ORDER-AUDIT-2026-10-11.md`.

Lean's derived `Policy.constructionOrder` is itself narrower than all gluings:
it emits an edge only when exactly one endpoint role is `B`.  `B`–`B` and
non-`B`–non-`B` gluings add no construction edge.

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
