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

## EXPERIMENT VERDICTS (`:B2`, 2026-09-05)

Holes 3–5 only. Holes 1 and 2 — preregister the first experiment domain, and
construct one non-toy machine `Q(o|pi)` — are `:F1`'s plan and were left alone
so the two rows do not do the same work twice; `:F1` slice 3 has already landed
the runtime composition (`src/futon2/aif/machine_q.clj`), and its slice 2 is
waiting on question 1 below, which is Joe's.

Each of the three was given the cheapest experiment that could have refuted it —
refuted meaning *the work it proposes is already done*. None was refuted; all
three are adopted into the mission doc marked machine-proposed, in commit
`af238f4d`.

**Reversal: `git revert af238f4d`.** That commit adds one section to
`holes/missions/M-aif-policy-conditioned-eig.md` and touches nothing else.

The QUESTIONS above are untouched and unanswered.

| Hole | Verdict | The experiment, and what it measured |
|---|---|---|
| Unify simulated and realised posterior updates | **validated, and sharpened** | The refutation is: name the updater both paths would share, and call it twice. The whole public surface of `futon2.aif.a4a` (13 vars) and `futon2.aif.bmr` (6 vars) was enumerated, and none of the 19 is named for an update, an observation, a posterior or a fold. The realised path recomputes concentrations from a corpus (`src/futon2/aif/a4a.clj:85`) rather than updating a state; the only `observe-outcome` in the tree (`src/futon2/aif/selection_gain.clj:163`) folds a scalar performance ratio, not a Dirichlet posterior. So the strawman's phrasing presupposed an object that does not exist: the adopted item leads with minting the updater, which the strawman did not. `runs/B2-strawman/01-consumer-and-updater-probe.edn` `:E3` |
| Specify typed policy-to-Q and Q-to-consumer delivery | **validated, and NARROWED** | This hole's acceptance has two conjuncts and the experiment separated them. The rejection conjunct — action-grain Gaussian telemetry and `Q(pi)` refused at the boundary — is **already discharged** by `:F1` (`src/futon2/aif/machine_q.clj:138`, ten fed objects and their verdicts in `runs/F1-machine-q/01-runtime-seam.edn` `:boundary-refusals`), so adopting the hole verbatim would have asked for work already done. The acceptance conjunct survives: one row of the machine Q that `:F1` pinned (reproducing that record's `:acquisition` row at max abs difference 0.0) is accepted by `epistemic-value/expected-information-gain` as `:predicted-observations`, while `core-efe/risk` — the only runtime risk consumer in the tree, aliased once in `src/ants/aif/efe.clj:20` and nowhere else — throws `ClassCastException` on the map and, given the parallel scalar seqs its signature admits, silently returns 0.0 for the row and 1.170904 for the same row permuted. Nothing refuses it and the outcome identity does not survive. The adopted item asks for the surviving conjunct and names the boundary the risk side still needs. `runs/B2-strawman/01-consumer-and-updater-probe.edn` `:E4` |
| Collect a default-off EIG shadow and calibration packet | **validated** | Each of the five fields the acceptance requires persisted — prior entropy, expected posterior entropy, `:expected-information-gain`, degeneracy reason, realised information gain — occurs 0 times across the 306 `.clj` files of `src/`, `scripts/` and `test/`, and `holes/labs/M-aif-policy-conditioned-eig/`, the directory this mission's section 4 names for the shadow artifact, does not exist. The kernel's only caller remains the hand-built binary latent (`src/futon2/aif/mission_epistemic_value.clj:210`), which is what `Q-interface-completeness.edn` `:Q/to-EIG` already records. `runs/B2-strawman/02-read-and-replay-probes.edn` `:E5` |

The consequence for the field, replayed rather than run: this mission sat at
`:open-hole-count 0`, where `task-belief-ladder/classify` overrides every
support rung with the typed rung-3 `:no-open-holes` refusal
(`src/futon2/aif/task_belief_ladder.clj:262`). At 3 that override no longer
fires — over a field carrying one persisted decision the candidate moves from
factor 0.0 refused to rung 1 at factor 0.375
(`runs/B2-strawman/03-refused-set-replay.edn`).
