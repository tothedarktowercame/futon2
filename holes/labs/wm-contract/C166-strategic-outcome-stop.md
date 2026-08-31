# C166 — strategic carrier family: outcome stop

Date: 2026-08-31

## Gate-0 result

The strategic outcome type cannot be settled from the authoritative record, so
this delivery stops before adding carriers.

`M-wm-strategic-mission-selection.md` fixes several surrounding distinctions:

- executable/operator support is a hard relation, not an outcome probability;
- a strategic policy is an outer control-pattern cascade and then a mission or
  short mission cascade;
- selection frequency updates `E_S`, not mission quality;
- tactical results update the *next* strategic state, not the decision that
  selected them; and
- the live `central + strategic + doable` blend is an engineering baseline,
  not canonical strategic AIF.

But S3 names the missing object only as **“probability of useful progress”** and
an outcome model learned from **“independently witnessed mission transitions.”**
S5 repeats **“independently witnessed usefulness.”**  Neither section gives an
outcome vocabulary or equivalence rule.  In particular, it does not decide:

- whether an outcome is a mission-state transition, a delivery disposition, a
  grounded-change category, or a tuple of these;
- whether `grounded-change`, `grounded-no-change`, `build-failed`, and
  `incomplete` are the complete support or merely one cohort's measurement
  taxonomy;
- how a transition becomes “useful” independently of the selecting model; or
- whether outcomes are mission-local or shared across a multi-mission policy.

The existing cohort labels therefore cannot be promoted to `StrategicOutcome`
without a scope decision.  They are observations from particular experimental
epochs, not a declared exhaustive outcome type.  Likewise, reusing the generic
vertex-tagged `Outcome := Sigma Obs` would only relocate the ambiguity: no
strategic observation family has been declared.

## What is settled, but intentionally not built

Once `StrategicOutcome` is fixed, the rest of the family is straightforward:

- `StrategicPolicy` can carry an outer control cascade plus its resulting
  mission/short mission cascade identity;
- `StrategicPredictiveOutcomeKernel` can reuse
  `ProbabilityKernel StrategicPolicy StrategicOutcome`;
- `StrategicPolicyPrior` can reuse `ProbabilityKernel Unit StrategicPolicy`;
- tactical policies can be indexed conditionally by strategic policy; and
- all inherited normalization conditions sum the actual row mass, preserving
  the C49 repair.

No new normalization law or implementation is needed.  The single blocking
decision is the finite strategic outcome support and its independent
usefulness interpretation.  Building the policy/kernel family before that
decision would propagate an invented noun into `Q`, `G_S`, and `E_S`.

No Lean declarations, contract entries, bindings, or paper text change in this
delivery.
