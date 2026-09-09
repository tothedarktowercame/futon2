# C module v1 — first instantiation, 2026-09-09

## Joe's authorisation, verbatim

> So I don't think we need to make a deep dive into metaphysics here to get something working. And I think the idea of a co-structure or a duality is interesting, but something that I would want to get evidence for after we work with this system. Ask them for a while and practice. And I don't think we should block. Progress with getting the system running on a need to fully theorize. All of this stuff in advance. I think we have enough background on C. That we could start to build an implementation. Or instantiation of the Model that we've just worked up in our excursion document. And this won't necessarily be 100% complete. But it should be enough that we don't block further work and improvement. Both of this aspect and other aspects of the War Machine model. So, effectively, I would propose that we think about See as a module and... One that we can version. One. And run and improve. Or extend. Or change, depending on local considerations, because... As we talked about, the preferences related to building code might be different from the preferences related to solving mathematical problems. Or... My preferences might be very different from those of some other client or consumer who wanted to use the same system to deal with their issues and concerns. So it seems pretty clear that an Ostrom style approach would be very useful here because we'd be able to... Build modularly, extend modularly, and change the preferences based on local conditions. I'd suggest that whatever solution we come up with that allows us to... Get this model interoperating with the rest of the existing War Machine implementation. Will be the right one for now. And that we can evaluate other Thank you. Designs later. But the key consideration is that our... Implementation of C should be type correct relative to the lean specification. I think that's pretty much the only key consideration we have at this point. So with a rough design in place, I think we should start to build now relative to that. Type. With the view that we'll extend. Modularly. Later.

“See” refers to C in this context. This explicitly releases the earlier stop
before building; it does not adopt a numerical parameter for the five current
preferences or close the production observation-model gap.

## What is implemented

- `src/futon2/aif/preference_module.clj`: versioned, local-context modules;
  exact finite tagged-outcome distributions; soft-binary family diagnostics;
  explicit parameter instantiation; local KL; versioned assessments; partial
  comparisons; and ranking against one selected finite preference entry.
- `resources/c-modules/current-work-v1.edn`: the five source-grounded paper
  preferences, migrated from the existing JSON pilot, retaining sources,
  paper revision, measurement obligation, phase and next-work proposal.
  They remain candidates with symbolic p. Organisation-tagged binary outcomes
  here are a local diagnostic domain, not a replacement for the canonical
  twelve flight dispositions or a newly ruled global carrier.
- `efe/rank-actions` accepts `:preference-module` and records its assessment
  for each candidate's explicitly supplied `:preference-readings`.
- `efe/rank-local-preference-actions` returns both ordinary WM rankings and
  a distinct local-C-risk ranking for one finite entry, after the existing
  `rank-actions` support partition. Unknown or refused included candidates
  leave the local selection unresolved. The result retains WM exclusion and
  refusal metadata. This does not add new actuator permission.
- `checks/c_module_v1.clj` replays the paper pilot and exercises the WM adapter;
  output and exact source pins live in `runs/C-module-v1/`.

No global default profile is installed. The caller explicitly selects its local
module snapshot. The existing controller score remains its existing score;
local risk is not renamed aggregate EFE. This is an interoperable callable
module, not live deployment or a redesigned production controller.

## Lean boundary

`mathlib4/DarkTower/WarMachine/LocalPreferenceModule.lean` imports the canonical
Holes types. `ExactTable` carries a finite support, rational mass, nonnegativity
and exact normalization. `ExactTable.kernel` embeds it in the real-valued
`ProbabilityKernel Unit O`. `Module.C` has exactly the canonical
`PreferenceDistribution Obs` type, with outcomes in the vertex-tagged sum.

The file proves preservation of a named zero and constructs the soft-binary
family for any rational p satisfying 1/2<p<1, with positivity on its support.
The runtime table uses unique tagged support, exact rational/integer masses,
explicit mass entries including zero, and zero outside its listed domain.
These are at least as restrictive as the finite-table representation used
by the Lean constructor. No clamping, normalization repair or default mass
is performed.

This is a compiled Lean representation and constructor proof paired with
runtime validation of the premises. It is not extraction of Clojure from Lean
or an end-to-end refinement proof of the Clojure implementation. Floating-point
logarithms implement the numeric KL after exact mass validation; they are
checked against reference values, not proved equal to Real.log. Numeric range
failure is explicit. The canonical C hole and its observation-model obligation
are not closed by this type-correct component.

## API and reproducible use

From futon2:

    clojure -M checks/c_module_v1.clj

The example checks pinned source digests before replay. Its paper assessments
refer to the earlier immutable pilot, not the current working-tree paper.
The existing illustrative twelve-outcome seed is also consumed without
changing any mass. Synthetic point predictions supply adapter controls:
grounded-change has risk ln(2); build-failed has risk ln(8), so the former
ranks first under that seed. Predicting cancelled meets an exact zero and
returns refusal. These are explicitly synthetic predictions, not forecasts
from the current channel or checkpoint model and not new preference rulings.

The caller-facing shapes are:

    {:schema :preference-module/v1 :id :local :version 1 :context :a-domain
     :entries [{:id :criterion-id :version 1 :criterion "criterion/v1"
                :kind :finite :distribution {:support [...] :mass {...}}
                :source {:ref "..." :basis "..." :status :candidate}}]}

    {:criterion-id {:version 1 :criterion "criterion/v1"
                    :status :predicted :distribution {:support [...] :mass {...}}
                    :evidence "versioned-forecast-receipt"}}

Tagged outcomes are `[vertex outcome-id]`. `:soft-binary` entries instead name
`:vertex` and `:parameter`; observed Boolean readings yield symbolic risk
expressions. `instantiate` takes an explicit exact p and an authority reference;
it cannot choose the parameter itself. `:mode :accepted` refuses entries whose
source status is not accepted; default candidate use remains labelled in the
entry source. This is a record check, not authentication of a human ruling.

`assess` accepts observed/predicted readings or explicit unknown, inadmissible,
not-applicable states. Missing readings are unknown. IDs, versions, criteria
and evidence references are checked; validating those references' substantive
truth belongs to the supplied evidence adapter/reviewer. Unknowns never become
negative outcomes or probability mass. Finite observations may describe an
empirical distribution; a single witnessed state is its point-mass special case.
Local-risk ranking requires predicted readings, so an observation cannot be
silently reused as a forecast.

## Verification performed

- `lake env lean DarkTower/WarMachine/LocalPreferenceModule.lean` in canonical
  mathlib4: exit 0, no new sorry or axiom declarations.
- Axiom inspection of the embedding and soft-family positivity proof lists only
  propext, Classical.choice and Quot.sound; no sorryAx.
- `futon2.aif.preference-module-test`: 8 tests, 27 assertions, no failures/errors.
  Covers real twelve-outcome seed shape, exact normalization, zero support,
  symbolic-family bounds, version refusal, unknown/trade-off behavior,
  candidate-vs-accepted status, actual WM diagnostic integration and local ranking.
- `futon2.aif.efe-test`: 35 tests, 137 assertions, no failures/errors.
- `futon4/dev/check-parens.el`: source, test, resource and replay artifacts checked.
- clj-kondo: no new errors or warnings; the existing unused private
  `efe/ambiguity` warning is present before and after this change.

The checks exercise the invariants directly rather than building a separate
abstract logic model in this first slice. This is a bounded runtime/Lean
verification, not completion of every VERIFY obligation in the larger design.

The saved replay is byte-identical on a second run after pin validation.

## Extension boundaries

Local modules can change by explicit version/context; unsupported versions,
types, masses and mismatched readings refuse. Adding evidence adapters, more
preferences or a new domain does not require solving institutional duality.
Time-indexed family construction, cross-level mappings, institutional rule
execution, graph closure and independently justified Q remain further slices.
C_tau phase information is retained as metadata here, not claimed as a
constructed Lean PreferenceFamily. Selection among criteria and an aggregate
score require an explicit composition rule; this module does not invent one.

The observation bridge remains sum_o Q(o|policy)P(d|o) on a compatible domain.
Neither our diagnostic observations nor the illustrative point predictions
replace that bridge. Production adoption can now target this explicit input
contract rather than require another preference representation.
