# DRAFT FOR JOE'S REVIEW: strawman holes for M-aif-policy-conditioned-eig

This proposal does not alter the mission. Each item is independently
accept/edit/strike-able.

## QUESTIONS

1. **query** — The mission says to begin with one narrow policy family whose
   outcomes already feed a Dirichlet count, or define the evidence event first
   if none exists (`holes/missions/M-aif-policy-conditioned-eig.md:127`). The Q
   audit likewise puts naming one policy and enumerable outcome domain first
   (`holes/labs/wm-contract/Q-interface-completeness.edn:185`); which recorded
   acquisition/review action and evidence event should be the first instance?
2. **challenge** — Observation closure explicitly includes no-result, failure,
   timeout, conflicting evidence, and missing evidence
   (`holes/missions/M-aif-policy-conditioned-eig.md:81`). Should the first
   machine-Q constructor be rejected unless those outcomes are structural rows
   of its alphabet and its pinned non-toy instance proves normalization, as the
   current missing-constructor finding requires
   (`holes/labs/wm-contract/Q-interface-completeness.edn:42`)?
3. **query** — The completion contract requires simulated and realised updates
   to call the same implementation
   (`holes/missions/M-aif-policy-conditioned-eig.md:88`). Which existing
   A4a/BMR update entry point is authoritative enough to own both paths, and
   what persisted observation can serve as the equivalence fixture?
4. **suggest** — The Q audit finds that R13→R4 carries only an integer horizon
   and that typed Q delivery into R5 is still missing
   (`holes/labs/wm-contract/Q-interface-completeness.edn:89`,
   `holes/labs/wm-contract/Q-interface-completeness.edn:98`). Would you approve
   specifying the policy/cascade carrier and the normalized-Q delivery receipt
   before adding either edge, so a drawn route cannot be mistaken for the
   absent generative contract?

## STRAWMAN HOLES

- [ ] **Preregister the first experiment domain.** Name one concrete policy
  family, versioned `theta` domain, finite evidence alphabet, corpus split, and
  primary calibration metric. **Acceptance evidence:** a reviewed,
  machine-readable declaration includes ordinary evidence plus no-result,
  failure, timeout, conflict, and missing evidence, with no catch-all silently
  discarded.
- [ ] **Construct one non-toy machine `Q(o|pi)`.** Compose pinned generative
  model, belief, and policy inputs into the predictive-outcome carrier rather
  than hand-writing a point mass or copying the observed outcome. **Acceptance
  evidence:** an executable fixture constructs at least two policy rows, proves
  each row finite and normalized, and exhibits a policy-conditioned difference.
- [ ] **Unify simulated and realised posterior updates.** Route hypothetical
  evidence and its later observed counterpart through one A4a/BMR updater with
  only the observation source changed. **Acceptance evidence:** a pinned
  observation fixture yields equal posterior state and provenance on both paths,
  while an unknown outcome fails closed.
- [ ] **Specify typed policy-to-Q and Q-to-consumer delivery.** Define the
  R13→R4 policy payload and R4→R5 predictive-kernel payload with model/source
  pins, outcome-domain identity, normalization receipt, and executable
  falsifiers. **Acceptance evidence:** the same constructed Q is accepted by
  both risk and EIG fixtures, and action-grain Gaussian telemetry or `Q(pi)` is
  rejected at the boundary.
- [ ] **Collect a default-off EIG shadow and calibration packet.** Persist prior
  entropy, expected posterior entropy, EIG, degeneracy reasons, and later
  realised information gain without affecting selection. **Acceptance
  evidence:** held-out log-loss/Brier results and predicted-versus-realised
  entropy reductions are reported alongside winner/abstain/scale effects, and
  feature-off selection traces remain byte-identical.

## BASIS

I read the mission's missing generative join, completion criteria, and staged dark-to-live plan (`holes/missions/M-aif-policy-conditioned-eig.md:77`, `holes/missions/M-aif-policy-conditioned-eig.md:119`, `holes/missions/M-aif-policy-conditioned-eig.md:137`). I also read the Q-interface audit's missing constructor and typed-edge work order (`holes/labs/wm-contract/Q-interface-completeness.edn:42`, `holes/labs/wm-contract/Q-interface-completeness.edn:185`) and checked the registry task pattern at `src/futon2/aif/mission_registry.clj:34`.
