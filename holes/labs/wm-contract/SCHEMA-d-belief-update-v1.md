# D receipt consumed by Q — v1, frozen 2026-09-20

Owner approval for this freeze: claude-12's 2a acceptance, following
`c300c67b` / `8d1afde8`. Refusal policy authority: `1bc98c04`.
Producer: D; consumer: Q. This document fixes their interface before 2b
consumption code. It does not claim that the producer is wired.

## Join and common fields

D supplies `{:cascade-belief q0 :belief-update-receipt receipt}` to
`efe/rank-actions`. Q receives the same receipt and q0 through the evaluator
input. The decision retains that receipt at
`[:selection-certificate :belief-update-receipt]`. Q must not recondition it.

Every active receipt has:

```clojure
{:schema :wm/token-belief-update-v1
 :status status                         ; :value / :refused / :missing / :invalid
 :occurrence-id occurrence-id           ; non-nil actual occurrence identity
 :tau tau                               ; positive observation step; nil if missing
 :observation observation
 :z-semantics :per-step-redraw}
```

For every consumable receipt, `:continuation-belief` is the exact distribution
passed as q0. Beliefs use exact nonnegative integer/ratio masses summing to 1
on token sets. Stored beliefs omit zero masses; equality is Clojure `=`.
Q may check extensional equality as well, but it must not alter the receipt.
The producer retains full calculation rows separately, including zeros.

Received observation:

```clojure
{:status :observed :occurrence-id occurrence-id :tau tau
 :present #{...} :absent #{...}}
```

The sets are disjoint, their union is nonempty, and both are within the token
universe. Unknown/unmeasured tokens enter neither set. Context fields exactly
match the enclosing receipt. Q's scoring entry index is 0; it must not replace
the actual observation's positive tau with 0.

Received updates also carry `:pre-belief`, `:predicted-belief`,
`:observation-probability`, `:model`, `:carrier`, `:predecessor`, and
`:observation-authority`. The model and predecessor are the ones D actually
used, not a candidate's future. The carrier records support plus transition
closure, token universe and measured counts. Provenance fields do not grant
admission by their presence: D validates the underlying records.

## Successful conditioning

`:status :value` has `:posterior` and `:continuation-belief`, both equal to q0,
positive `:observation-probability`, and `:policy :conditioned-posterior`.
`:calculation` retains the shared exact calculation receipt.

Conditioning that changes nothing is still a value. D may cite
`BeliefConditionedRollout.exactUpdate_pointMass_vacuous` under `:conformance`
only after checking point predicted belief and possible received observation.
Q determines vacuity by comparing predicted belief with posterior, retaining
the original receipt. It must distinguish a vacuous update from no update.

## Impossible received observation

`:status :refused`, `:kind :belief-update-refused`,
`:observation-probability 0`, no `:posterior`. `:refused-trajectory` retains
the exact refused calculation (pre-belief, B-pushed belief, observation and
zero evidence). It is never relabeled missing or invalid.

Two policies are admitted:

1. `:policy :refused-observation-discarded` requires
   `[:observation-authority :channel] = :judgement` and
   `:continuation-belief = :predicted-belief = q0`.
2. `:policy :refused-reinitialized-from-observation` requires
   `[:observation-authority :channel] = :checkable`,
   `[:observation-authority :contract] = :wm/observation-contract-v1`, and
   the following explicit reinitialization:

   ```clojure
   :reinitialization
   {:authority :contract-admitted-checkable-observation
    :occurrence-id occurrence-id :tau tau
    :observation observation
    :initialization-receipt fact-derived-receipt
    :belief continuation-belief}
   :finding {:kind :model-misfit
             :reason :checkable-observation-has-zero-predictive-probability}
   ```

   Reinitialization's context and observation equal the received event;
   its belief equals the initialization receipt's `:value`, continuation,
   and q0. D establishes contract admission and the initialization derivation.
   Q checks these recorded equalities and policy/authority fields; it does
   not infer checkability or recompute the initialization.

Neither policy is a theorem: both are declared runner continuation policies.
The mathematical trajectory still records `none`. A Q event must preserve
the refusal, even if reinitialization changes the consumed belief.

## No observation and invalid input

`:status :missing` means no received observation, exclusively. Its observation
is `{:status :missing :reason :no-observation :occurrence-id ... :tau nil}`.
It has no posterior, probability, calculation, or refused trajectory. If an
executed transition is admitted, D records pre-belief, predicted-belief,
`:policy :no-observation-prediction-only`, and continuation = predicted = q0.
Q retains missing-input metadata and emits no observation-update event.

`:status :invalid` identifies malformed input or unverifiable domain/authority
with `:kind` and `:detail`. It is non-consumable: Q returns typed invalid input,
never a successful update, impossible-observation refusal, or missing value.

## Initialization and carry-admission refusals are separate

Missing/unestablished/contradictory enacted predecessor is a recorded
`:carry-no-predecessor`; changed token universe is `:carry-domain-changed`.
These live under decision `[:selection-certificate :token-belief-input
:carry-admission]`, with the fresh initialization receipt and consumed value
retained alongside. They cause fresh initialization, as ruled, and produce
no active update receipt: no exact update ran. An existing observation is
not relabeled missing to explain a failed carry admission.

The 2a `:token-belief-stage` schema remains historical staged evidence; it is
never passed to Q as a `:wm/token-belief-update-v1` receipt.
