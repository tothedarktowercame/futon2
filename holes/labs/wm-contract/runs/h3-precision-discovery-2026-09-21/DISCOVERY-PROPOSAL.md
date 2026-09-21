# H3: policy precision discovery and proposed runtime wiring

Date: 2026-09-21. Status: discovery and proposal only; STOP for review.
No runtime implementation, serving-JVM reload, or test-suite execution is claimed.

## Finding

The packet's temperature-mode pointers describe an older path. The current cascade selector already computes policy probabilities using `G/beta`. Its beta is declared, not learned. Closing H3 requires authenticated observation-to-policy evidence, a corrected existing updater, and persistent beta consumption; changing a temperature-mode flag would not close it.

Inspection baseline: futon2 `181ba4d114baa46dd371a3a2150fcf9ea5c2df53`. `source-pins.json` records source hashes and the separate Mathlib and p4ng revisions. Line references below describe that snapshot. `probe.edn` contains a fresh tooling-JVM numerical probe and a byte-hashed snapshot of today's trace. These are discovery evidence, not warrants.

## 1. Current computation and joins

| Source | Observed behavior |
|---|---|
| `src/futon2/aif/cascade_selection.clj`, `selection-posterior` (~52) | Scores the finite-G policy support as `E * exp(-F-G/beta)`, requiring positive beta. |
| `src/futon2/aif/policy.clj`, `select-action-cascades` (~228) | Requires caller beta; records it as `:declared` in the selection law/certificate. Retains the policy posterior separately from the action marginal. |
| `scripts/futon2/report/war_machine.clj`, `cascade-decision-admitted` (~6011–6240) | Builds one joint candidate family across targets, requires a single horizon and beta across assembled problems, then passes that declared beta to the selector. |
| `cascade_sources.clj` (~161), `cascade_problems.clj`, `beta-for` | Declaration supplies context beta; no predecessor learning join here. |
| `policy_precision.clj`, `distributions` (~34), `converge-beta` (~95) | Implements reciprocal precision and an iterative rate update, including bisection. Supports habit placement in both policy distributions. |
| `policy_precision.clj`, `carry-beta` (~497) | Older carry interface aligns F against ranked controller scores. It is not an authenticated frozen-cascade-menu update. |
| `policy_precision.clj`, `cascade-beta-update` (~638) | Existing cascade wrapper, but no production caller found in `src/` or `scripts/`. It must be repaired rather than paralleled. |
| `war_machine.clj`, `beta-dark-carry` (~575), `tau-mode-of` (~854) | Older helpers remain; source search found no live caller of the dark-carry helper. `policy/effective-temperature` no longer resolves. These do not establish today's default selection law. |
| `trace.clj` (~603) | Persists precision state if supplied by judge output; current selection does not supply learned state. |

The trace snapshot in `probe.edn` contains three records, all with `:cascade-selection-posterior`, declared beta 1, and no precision state. The last record is run `2026-09-21-1789964661`, timestamp `2026-09-21T04:26:36.942700495Z`; its tau-source is absent. This is a census of that byte snapshot, not a claim about all historical runs or loaded server code.

### A demonstrated updater defect

In a fresh tooling JVM, with G `[0,1]`, F `[0,2]`, prior beta 1 and bisection:

- `converge-beta` converges to `:beta-posterior 0.81206788662324`.
- `cascade-beta-update` retains beta 1, `:beta-source :held-absent`, even though its nested solve reports convergence.

The wrapper checks `(:beta solve)` (~706); the delegated finite-F solver returns `:beta-posterior`. Existing equal-G controls do not expose this because unchanged beta is their expected answer. The wrapper's separate infinite-F branch also omits habit E, unlike the production selector. Nonfinite F needs stronger classification: positive infinity is not the same input as NaN or negative infinity. Fixing only the result-key mismatch would leave those semantic differences.

Reproduction expressions, after requiring `futon2.aif.policy-precision` as `pp`:

```clojure
(pp/converge-beta 1.0 [0.0 1.0] [0.0 2.0] {:solver :bisect})
(:WM (pp/cascade-beta-update
       {} :WM
       [{:id :low :precedence [:p/low] :g 0.0}
        {:id :high :precedence [:p/high] :g 1.0}]
       {:low 0.0 :high 2.0} {:solver :bisect}))
```

Probe execution: `clojure -M:test /tmp/h3-probe.clj`, exit 0; tooling JVM only. The retained output is `probe.edn`; the temporary script additionally hashed and parsed one snapshot of the trace and checked namespace resolution. No suite was run and no passing-test claim follows from this probe.

## 2. What the update means

For a frozen policy family, retain a fixed carried rate `beta_prior` and solve for positive b:

```
gamma(b) = 1/b
pi0(b) = softmax(log E - G/b)
pi(b)  = softmax(log E - F - G/b)
b = beta_prior + dot(pi(b) - pi0(b), G)
```

The process theory's eq. 2.7 uses expected free energies indexed by policies; observations revise policy evidence. It does not substitute the selected task's realised scalar outcome for the G vector. Its basic prior lacks habit E; the project-specific extension places E in both distributions. Iteration holds the prior rate fixed while finding the posterior rate. Da Costa A.2 introduces precision and refers back to the process theory for its update. Sources: [Friston 2017, eq. 2.7](https://activeinference.github.io/papers/process_theory.pdf), [Da Costa 2020, appendix A.2](https://arxiv.org/html/2001.07203v2#A2).

The local `p4ng/wm-walkthroughs/build-loop/closure/PROPOSAL-policy-precision-learning.md` already records Joe's 2026-09-17 approval of an initial method: habit E in both distributions, shape-one Gamma rate prior beta 1, an observation-driven update, and retained before/after values. Reuse that starting method. Its suggested per-family contexts and observation clock need reconciliation with today's joint menu and whole-task execution grain.

`pi0` is therefore not raw habit E alone, and `pi` is not the chosen action, an action marginal, or a point mass at the enacted policy. The retained selection-time posterior is useful evidence but is not automatically the policy posterior after observing task completion.

## 3. Evidence available, and evidence missing

**Habit snapshot.** `cascade_habit_store.clj/attach-habits` (~51) reads menu weights, with explicit neutral fallback. `record-selection!` (~102) increments the selected representative on selection, not independently verified execution. Freeze the actual consumed E before that mutation; rereading the store later changes the prior. Selection certificates already retain per-candidate habit, F and G and the full policy posterior. They do not yet establish the complete frozen-family-to-outcome join required here.

**G grain.** Retain the exact expected G vector from the old joint comparison, its horizon, candidate identities, model pins and decomposition. Do not recompute it on next tick's changed candidate family. Keep the same admissible finite-G support used by selection; record exclusions instead of evaluating undefined `0 * infinity` terms.

**F timing.** `efe.clj` (~1110–1127) evaluates F at the horizon against `(:evidence spec)` (defaulting to an empty set), not a later task's admitted observation. Around ~1184 only finite computed F enters selection; nonfinite F is retained in evidence but the selector consumes neutral zero. That zero must not become purported observation evidence for precision learning. The cascade updater's infinity-exclusion posterior is also not the selector's present neutralization policy. Any change to selection's F semantics needs explicit review, rather than being hidden in H3.

**Partial observation.** `cascade_free_energy.clj/policy-free-energy` takes an observed token set restricted to its universe and evaluates exact observation likelihood. It has no present/absent/unknown input contract. D task evidence (`d_predecessor_task_authority.clj`, ~177–182) supplies C3/C4 affirmations, empty absent evidence and explicit unknown tokens. Unknown cannot be converted to false by passing only the present set. Either use a reviewed partial-observation likelihood that sums over unknown coordinates, or record an unavailable update until complete evidence exists. This choice determines whether today's producer can actually train beta.

**Execution identity.** The full-loop runner already captures the minted run/cohort/attempt/action/transition chain before dispatch (~3834, ~4114) and retains independently checked task completion via `d-task/complete!` (~3481). But its D-specific receipt explicitly says `:candidate-to-minted-join :not-established`. It certifies executed-with-artifacts, not portfolio approval or a general calibration authority. An explicit capture/binding from the frozen selection receipt into the minted occurrence is needed; equal action values are insufficient. Recovery and historical verification remain distinct refusals. Do not weaken E2b or claim its scope through D's narrower evidence.

**D carry is not an observation oracle.** `token_belief_predecessor.clj` retains `:conditioning-status :not-wired` even on task admission. Its fresh token belief is not a learned posterior that H3 can treat as already conditioned.

**Initialization and persistence.** Existing beta initialization is 1. `coerce-state` silently falls back on malformed state, which is unsuitable for the new provenance boundary. Initial absence, no valid predecessor, malformed state, no observation, impossible observation, and numerical nonconvergence need distinct records. A valid previously carried beta may be retained under a declared hold policy; corrupted state must not masquerade as an initial or learned value. Persistent state also needs an observation-consumption identity so restart/replay cannot learn twice.

## 4. Proposed smallest faithful change-set

Each row is a build increment after review, not work already performed.

| Change | Acceptance controls |
|---|---|
| Repair the existing cascade updater and reuse its numerical core. Standardize the solver result, use frozen E in both distributions in every branch, validate numeric domains/support, retain pi0/pi/dot/residual and failure reasons. Prefer checked bisection; no clamped value advertised as convergence. | Finite-F contrast reproduces the probe's nontrivial update through the wrapper; contrasting F can move beta in either direction. Constant F/equal G holds beta. A constant F offset does not change the answer. Nonuniform E affects both distributions correctly. Positive-infinite F, all-impossible evidence, NaN, negative infinity, excluded G support, and unbracketed/nonconvergent roots have distinct checked outcomes. |
| Freeze the actual joint comparison and bind its receipt to the minted task occurrence at dispatch. Retain E/G, policy identities, context, horizon, source/model pins and the predictive inputs needed to evaluate later evidence. | Reordering candidates preserves results by identity. Duplicate IDs, changed model bytes, mismatched occurrence, changed carrier and a selection self-comparison cannot validate the binding. Habit mutation after selection cannot change the snapshot. |
| At next-tick intake, validate completed-task evidence and compute F over the **previous** frozen menu at the approved observation grain. Keep this retrospectively updated policy posterior explicitly separate from the historical selection-time posterior. | An authenticated constructed completion with informative observations changes the posterior; missing evidence records no update. Unknown tokens never count as absent. Historical/recovered/self-asserted completion is refused. Positive-infinite observation likelihood cases cannot be relabeled finite neutral evidence. |
| Persist the resulting rate and provenance with exactly-once consumption, then supply it to the current joint selector. Validate state rather than using silent coercion. | Write/read/restart retains rate and receipt; replay cannot apply the update twice. Tampering and wrong predecessor are detected. Explicit first initialization uses the declared prior. Typed hold/refusal preserves the last valid rate without a learned claim. |
| Make this the normal selector input and record actual beta, gamma and temperature provenance. Existing declaration beta becomes the reviewed initialization input, not a value overwriting every learned rate. Reconcile obsolete mode documentation. | Certificate beta equals the carried result; gamma equals its reciprocal and tau equals beta. Both initialized and learned branches are truthful. `decision_gate` already permits `:learned`; preserve its checks. A learned-beta control changes policy probabilities through the real selector; an argmax change is not required. |

The intended update location is next-tick intake after verified completion, before the current comparison is selected. The updater evaluates old-menu G/E with the new admitted evidence, then carries one scalar into the new comparison. It must not attach old F to new candidates because their names happen to match.

Required receipt contents: schema/version; context and previous rate authority; old menu and model pins; minted occurrence plus the explicit selection binding; observation identity/tau and present/absent/unknown semantics; G/E/F and support/exclusions; pi0/pi; rate prior/posterior, gamma and tau; residual/bracket/convergence evidence; update/hold/refusal status; consumed observation identity; and the selector's exact consumed rate. Preserve the computation and the consumption equality, not merely a label saying variational.

## 5. Decisions requested at this checkpoint

1. **Context:** use one rate for the present joint WM comparison, rather than silently importing the older proposal's per-family R/WM/E partition. State the policy for context/model changes and whether initialized declaration rates must agree (current assembly requires agreement).
2. **Observation clock:** confirm how a verified whole-task outcome maps to observation tau in the declared cascade model. One task is not automatically the model horizon or one pattern step. The task-grain D ruling is a precedent, not proof of this new calibration correspondence.
3. **Partial evidence:** approve a specified marginal likelihood for measured coordinates, or explicitly accept held updates until complete observations exist. The former requires implementation and mathematical correspondence tests; the latter cannot claim live learning merely because plumbing exists.
4. **Policy evidence:** settle positive-infinite F handling for the retrospectively updated posterior, with the current selection-time neutralization recorded separately. Do not change the F lane implicitly.
5. **Authority and activation:** authorize the narrow completion-evidence consumer and explicit selection-to-minted binding, preserving D/E2b scope limits; confirm default activation after its acceptance gate. The existing approved beta-1 initialization and habit placement can be reused, with explicit malformed/missing/predecessor policies.

A real closure demonstration must contain an authenticated informative observation, a successful nontrivial rate update, durable carry, and a default-path selection consuming that rate. A record initialized at beta 1, a neutral-evidence hold, or a converged solver that the wrapper discards does not demonstrate learning. No such closure is claimed here.

## Review boundary

This artifact records discovery, a reproducible numerical counterexample, and a proposed build plan. No production implementation or mission/Holes closure mark was changed. Stop here for review of the decisions above.
