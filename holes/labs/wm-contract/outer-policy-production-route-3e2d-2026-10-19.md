# Outer Cascade Live policy production route — Packet 3E2d

## Production completion — 2026-10-19

The historical configuration status below is superseded by commit
`af0fa4729`. Production now supplies `outer-policy-provider-runtime/production-provider`
by default. It adapts the existing outer-learning authority, pinned outcome
manifest, prior concentration, preference transform and controller terms to
each sealed menu. New policy digests need no observed outcomes: their posterior
starts at the declared prior. Target-level resource and cadence estimates remain
target-level estimates; task-outcome updates require the matching policy digest.
Unit objective precision (`gamma = 1`) is explicitly declared as calibration in
the authority file, not inferred from observations.

At Joe's instruction, the unique-minimum requirement no longer blocks equal
policies from obtaining their first observation. Equal objective minima use a
uniform draw; the receipt retains the draw and candidate population, and the
selected carrier verifier replays it. This does not change unequal objectives
or admit malformed evidence. Empty-history selection, carrier verification and
downstream consumption pass together; existing outcome updates are tested
separately. A production adapter probe consumed 29 pinned historical records.

Base: `9946f92c15da443de3b6002245e0808d677d5fa7`

No click was launched. The shared main worktree was not edited.

## Production call path

`full-loop-runtime/production-defaults` transports one explicit evidence provider
into `meta-live-outer-selector/selector`. On the ordinary (non-injury) branch,
`select-live` now executes exactly this route:

1. standing/map/ownership filtering;
2. `family-selection/full-eligible-target-field` over every admitted target;
3. `family-selection/sealed-pre-choice-menu` over the complete field;
4. `outer-policy-evidence-provider/resolve-provider`;
5. `outer-policy-evidence/join` and its total verifier in the objective boundary;
6. `outer-policy-objective/evaluate` once;
7. `selected-outer-cascade-policy/seal` and `verify`;
8. the canonical outer receipt/action, retaining the sealed menu, evidence,
   objective, selected carrier, and Q2/Q4/Q8 recomputation projection.

The downstream `cascade-decision` consume-without-selection branch receives the
same selected carrier. Tests replace its legacy field and family selectors with
call counters and observe zero calls. The machine-injury algorithm branch remains
separate and unchanged.

## Explicit evidence authority

The provider response is `:wm/outer-policy-evidence-provider-v1`, content-pinned
over its declaration, durable terminal records, and authority metadata. A
provider may be supplied directly as a pure function/map or loaded from an
explicit configured EDN path. The response supplies no numerical defaults. Its
declaration must continue to satisfy the integrated evidence verifier: exact
outcome vocabulary, positive Dirichlet alpha, probability preference and pinned
authority, positive gamma, and pinned resource/cadence/continuation `ln E`
components (including explicit zero).

Absent provider configuration refuses with
`:outer-policy-evidence-provider-required` and enumerates the absent authority
terms and every menu digest. Incomplete declarations, altered provider pins, and
invalid evidence refuse before objective selection. Apparatus/unclassified
terminal records remain in each evidence row's exclusion audit and do not change
task-policy alpha.

The objective equations remain the integrated authority, in nats:

`G = KL(q || C) + E_q[H(A)] - EIG(Dirichlet posterior)`

`objective = gamma * G - (lnE_resource + lnE_cadence + lnE_continuation)`

Selection is the unique strict objective minimum. Exact equality produces the
decided `:no-unique-policy-objective-minimum` abstention, names all tied policy
and cascade identities, and records `:strict-policy-discrimination` as absent.
Identity is never a tie breaker.

## Focused evidence

- Core production/evidence/objective gate: 65 tests, 331 assertions, 0 failures,
  0 errors.
- Downstream family/identity/executable/report gate: 69 tests, 333 assertions,
  0 failures, 0 errors.
- Static checks: changed-file clj-kondo, Emacs check-parens, and `git diff --check`.
- A focused post-commit registry warrant is recorded separately by the registry.

Production-shaped controls cover absent/refused/incomplete providers, implicit
authority rejection, apparatus exclusion, provider seal tampering, exact ties,
strict unique selection, byte-identical target/policy/digest/canonical-record
transport, and zero legacy downstream selection calls.

## Honest configuration status

The serving defaults do not currently name an evidence authority file or
provider. They therefore fail closed at the explicit provider boundary and are
not claimed live-ready. Deployment must configure
`:outer-policy-evidence-provider` or
`:outer-policy-evidence-provider-path` with a declaration covering the complete
dynamic menu. No authority was synthesized from policy identity and no live
click was used to manufacture one.
