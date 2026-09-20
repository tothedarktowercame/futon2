# D 2c authority boundary requiring a scope ruling

Build preparation found a dependency not covered in the earlier discovery.
The task-grain, revision-pair and recovery rulings are understood and unchanged.
No production producer/verifier/reader code has been written in this step.

D 2b currently calls `machine-enactment-correspondence/verify-correspondence`
from `token_belief_predecessor.clj:41`. The proposed production extension meets
an additional existing invariant:

- `machine_enactment_correspondence.clj:102` re-resolves E2a before verifying
  witnesses. Lines 137–163 bind the witnesses to the complete, ordered approved
  portfolio and require selected occurrence membership and exact action bytes.
- `machine_portfolio_restriction.clj:110` resolves E1 from independently pinned
  ranked support, membership, costs, utilities and budgets. It does not accept
  a runner's selected action as substitute authority.
- `machine_budget_authority.clj:123–127` explicitly refuses production because
  the externally owned E1 source configuration is not installed. E2b is not
  the only unavailable production authority in this chain.
- That chain's candidate occurrence is an R6 domain identifier. The existing
  runner's close-retention occurrence is a minted action/transition identifier.
  Both should be retained and explicitly bound; equality of action values
  alone is not proof that they identify the same approved occurrence.

## Executed compatibility probe

`authority_probe.clj` re-resolves the existing independently pinned isolated
E1/E2b fixture, without mocks or changing source. The positive isolated control
returns `:exact-occurrence-and-action`. The same configuration in production
refuses `:e2b/production-authority-unavailable`. Resolving E2a with production
E1 separately refuses `:r6-r11/production-authority-unavailable`.

The probe also invokes the real occurrence minter using the identical selected
action value. Its minted action ID is distinct from the candidate ID; no join
between these identities is asserted. `probe.log` retains these results.
Lint and parentheses pass; `execution.json` records commands, exits and hashes.
This was one exploratory probe, not a rerun of the test suite or a production
admission demonstration. No fixture is promoted to production authority.

## Structural decision requested

The four-condition task verifier can be implemented as a specifically scoped
D predecessor authority. It must then be named as such, with E2b/E1 portfolio
verification still unavailable and no claim that it discharges that broader
proof. Alternatively, extending production E2b must preserve its E2a domain
checks and therefore needs a production E1 authority source and explicit
candidate-to-minted-occurrence join as part of the build.

Please specify which scope is intended. The four-condition correspondence rule
settles task enactment but does not state whether D's existing E2b dependency
is to be replaced by this narrower authority or fulfilled through E1/E2a.
I have not inferred that existing approved-domain checks may be omitted.

The workspace AGENTS.md requires a stop rather than bypassing or weakening an
invariant. This is that boundary: replacing D's verifier without a scope ruling
could silently remove the portfolio requirement, while claiming production E2b
from task evidence would overstate what was checked. All three prior rulings
remain implementable once that relation is explicit.
