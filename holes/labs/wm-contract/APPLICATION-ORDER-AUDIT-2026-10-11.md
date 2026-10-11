# War Machine application-order audit — 2026-10-11

## Result

The runtime does not carry an application order distinct from construction
precedence.  No sound pre-G application-order certificate can be added until
the specification defines that carrier and its relation to observed execution.

Lean now has an **additive parametric certificate** for that future carrier.
It does not authorize the current runtime precedence or change policy
admission.  `ApplicationOrderCertificate.valid` binds the policy's derived
construction edges and declared application triples exactly, decodes the
triples through an externally registered law, requires every square index
exactly once, and applies that law's relation.  The relation is not hard-coded
to construction-order equality, so a future independently authorized
application law can differ.

## Evidence

- `DarkTower/WarMachine/ThreeHalvesBlend.lean` defines
  `Policy.applicationOrder : List (Nat × Nat × Nat)` and proves
  `applicationOrder_independent`: changing it affects neither the structural
  cascade verdict nor the construction order.  It does not yet define
  well-formedness, execution meaning, or correspondence for those triples.
- `src/futon2/aif/interpretation_construction.clj` constructs one
  `:precedence` vector and uses it to build the candidate and model rollout.
- `src/futon2/aif/wm/cascade_decision.clj` uses that same `:precedence` as the
  policy family supplied to G scoring.
- `src/futon2/aif/flight_runner.clj` executes the chosen `:precedence` in vector
  order.
- `src/futon2/aif/cascade_evaluation_trace.clj` declares the evaluated model as
  `:first-enabled-union-theta-v1` with that same `:precedence`.
- Repository search found no runtime `:application-order` or
  `applicationOrder` producer/validator.

Thus the present runtime collapses at least three notions: construction linear
extension, model firing precedence, and enactment sequence.  Copying
`:precedence` into a newly named field would not establish the independent
carrier required by Lean.

## Minimal next specification

Define the meaning of one application-order row `(before, after, witness)`:

1. what the two square/pattern identities denote;
2. what the witness identifies (shared object, produced/consumed token, or
   observed transition evidence);
3. whether the relation is a partial order, total schedule, or dynamic
   refinement constraint;
4. how it constrains model firing and actual enactment; and
5. the typed refusal when construction order, application order, predicted
   firing, and observed execution diverge.

Only then can runtime emit a source-pinned `:wm/application-order-v1` receipt
and Lean state its validity and selected-to-enacted correspondence.  This audit
does not alter production behavior or infer application semantics from the
existing precedence vector.

## Exact Lean/runtime correspondence still required

| Lean carrier | Required runtime field | Status |
|---|---|---|
| `Policy.constructionOrder` snapshot | `:construction-order` (ordered square-ID edge pairs) | Not produced independently; current `:precedence` is overloaded |
| `Policy.applicationOrder` snapshot | `:declared-application-order` (retained triples) | Missing |
| Authorized decoder output `applicationSequence` | `:application-sequence` (ordered stable square IDs) | Missing |
| Exact square population/no duplicates | `:square-population` joined bijectively to policy square IDs | Missing policy-level runtime carrier |
| Law identity/version | `:order-law {:identity ... :version ...}` | Missing external registry/configuration |
| Law authority/receipt | `:order-law {:authority ... :receipt ...}` joined to a retained registry entry | Missing |
| Selector authority and separation | `:selector-authority` nonzero and distinct from the law authority | Missing |
| Registered decoder and relation | Registry entry specifying triple decoding and the construction/application relation | Missing; must not be selector-authored |
| Registry implementation provenance | Source/implementation digest covering the decoder and relation | Missing |

The minimal receipt shape is therefore:

```clojure
{:schema :wm/application-order-v1
 :policy-id stable-id
 :square-population [square-id ...]
 :construction-order [[from-id to-id] ...]
 :declared-application-order [[a b witness] ...]
 :application-sequence [square-id ...]
 :order-law {:identity nonzero-id :version positive-nat
             :authority external-id :receipt nonzero-id}
 :selector-authority distinct-nonzero-id
 :source-pin {:path path :sha256 digest}}
```

The external registry entry must supply the decoder and relation selected by
the four law fields, carry a source/implementation pin for those functions,
and be retained outside selector control.  Lean requires the selector authority
to be nonzero and different from the matched law authority.  Same membership is not ordered correspondence: the Lean
witness `[0,2,1,3]` contains exactly the same squares as `[0,1,2,3]` but is
rejected by the registered exact-order law.  Separate witnesses reject a
duplicate, omission, unknown index, and substituted law identity.

An adversarial law with the same identity/version/authority/receipt but a
changed acceptance function does not validate a certificate against the fixed
registry.  If the caller replaces the trusted registry with that adversarial
implementation, the same certificate can validate: Lean treats the registry
as a trust premise and does not cryptographically authenticate functions.
Therefore the runtime registry boundary must verify and retain the external
source/implementation digest; matching labels alone are insufficient.

The first integration boundary is policy construction in
`src/futon2/aif/interpretation_construction.clj`, before its single
`:precedence` vector is reused by rollout and G.  An independently supplied
and source-pinned application-order receipt must enter there as immutable
evidence; validation must refuse before cascade decision/G.  Enactment in
`src/futon2/aif/flight_runner.clj` may consume it only after the distinct
selected-to-enacted correspondence is specified and checked.
