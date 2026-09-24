# PROOF-2 F discovery — why F is never supplied

Date: 2026-09-24

Scope: read-only discovery over source, history, and existing run records. No code, data, click, or JVM state was changed.

## Verdict

No run record under `data/wm-runs/` contains a selection-certificate candidate with both numeric F and `:f-status :attached` or `:computed`. The absence is intentional and currently correct: commit `acc4f3c4640b4092710cceaf9d88f50acaeb0663` disabled the old prospective F calculation on production clicks and installed a staged observed-prefix interface that always emits `:not-supplied` until D conditioning supplies and admits a real policy/execution/observation prefix. The three named runs contain no admitted observation updates. Before that commit, the machine did calculate prospective F, but identity A made it non-finite for effectful policies, so it was recorded as `:computed-not-attached`, not consumed. This is therefore a missing data-production-and-admission path plus its consumer wiring, not a finite value sitting elsewhere in the record and accidentally omitted.

## 1. What an admitted policy prefix is

The reason is set in `src/futon2/aif/policy_prefix_evidence.clj:56–70`, repeated in `src/futon2/aif/efe.clj:1269–1275`, and propagated by `src/futon2/aif/policy.clj:235–258`. In this code, a policy prefix is an ordered, nonempty sequence of already observed steps for one exact policy. `evaluate-synthetic` defines its required shape: a policy identity; a validated observation model; a normalized prior; `:z-semantics :per-step-redraw`; and steps numbered consecutively by `:tau`, each carrying a transition row over every supported prior state, an occurrence-identified context, and the subsequently observed event. Each step predicts through the transition, conditions the prediction through `observation-model/query`, retains the prior/prediction/posterior, and adds that observation's F. An **admitted** prefix would be the same history whose policy→execution→observation identities D has accepted for production consumption. No ledger currently supplies it: the production caller invokes `policy-prefix/production-ranked` with only ranked candidates and a small conditioning-status summary, and `production-ranked` explicitly refuses synthetic receipts, removes any `:f`, and unconditionally emits `:status :not-supplied`. It is empty when D has admitted no ordered observation update for that policy; the present receipt names the pending dependency as `:d-conditioning-consumption-and-policy-prefix-admission`.

## 2. Data absence or wiring absence?

It is both, in dependency order, but not “qualifying data already exists and one argument was forgotten.” The authoritative place where a supplied prefix would have to appear is per candidate at:

`[:decision :selection-certificate :candidates i :f-prefix]`

Its admitted observation inputs would be joined from:

`[:decision :selection-certificate :token-belief-input :observation-updates]`

and reflected at:

`[:decision :selection-certificate :token-belief-stage :observation-updates]`.

The three records say:

| Run | policies | token-belief input | observation updates | stage | F prefix |
|---|---:|---|---:|---|---|
| `2026-09-21-1790033693` | 2 | `:conditioning-status :not-run`, reason `:observation-initialization-disabled` | 0 | `:awaiting-observation-admission` | `:not-supplied`, `:no-admitted-policy-prefix` |
| `2026-09-22-1790053967` | 3 | `:conditioning-status :not-run`, reason `:observation-initialization-disabled` | 0 | `:awaiting-observation-admission` | `:not-supplied`, `:no-admitted-policy-prefix` |
| `2026-09-23-1790131591` | 1 | `:conditioning-status :not-run`, reason `:observation-initialization-disabled` | 0 | `:awaiting-observation-admission` | `:not-supplied`, `:no-admitted-policy-prefix` |

Each record does contain an `:initial-belief-receipt` with `:status :present` and origin `:assembled-target-facts`, but that is a point-mass starting belief, not a policy/execution/observation history. The records also contain inspected prior action material and ordinary locator observations, but the token-belief input explicitly refuses them for conditioning; they are not an admitted ordered prefix. Thus the immediate absence is data/admission: zero qualifying prefix steps. The architectural absence is also wiring: even if a caller constructed qualifying history elsewhere, `production-ranked` has no parameter for it and always emits `:not-supplied`. Both must be repaired in that order.

## 3. What `cascade-free-energy/policy-free-energy` needs

`src/futon2/aif/cascade_free_energy.clj:105` validates five inputs before calculating a candidate:

1. nonempty normalized initial belief `:q0`;
2. nonempty `:candidates`, each with id and precedence vector;
3. positive integer horizon `:tau`;
4. set-valued `:observed-tokens`;
5. nonempty complete adjudication `:rates`.

It also checks the pinned Lean source, rolls `q0` through each candidate's transition kernel to `tau`, evaluates `TokenObservation.tokenLikelihood rates state observation`, sums `P(o|π)`, and returns `−log P(o|π)`; zero evidence becomes `##Inf` and typed model refusals propagate.

The exemplar already carries pieces of that calculation:

- `:q0`: `[:decision :initial-belief-receipt :value]`, repeated under `[:decision :selection-certificate :token-belief-stage :initialization :value]`;
- candidate identity and precedence: the certificate's `:candidates`/`:policies` ids and `[:decision :selection-law ... :action]`;
- a six-token/64-state carrier and continuation belief under `:token-belief-stage`;
- an effective horizon of four inside the class-emission model recorded under candidate `:computed-f` and scoring provenance.

It does **not** carry the admissible inputs needed for PROOF-2 F:

- no admitted observed policy prefix (`:observation-updates []`);
- no measured token observation rates A; scoring provenance is a synthetic, uncalibrated class-emission model, and the old token path used identity-default rates;
- no posterior `q` needed to witness `PolicyVariationalFreeEnergy.variationalFreeEnergy lik prior q` rather than merely its `−log evidence` equality value;
- no complete content-addressed `:params` receipt from `cascade-free-energy` on the production selection.

Most importantly, production sets `:f-prefix-production? true`, so `efe.clj:1125` does not call `cascade-free-energy/policy-free-energy` at all. The exemplar's `:computed-f` is a class-observation-model receipt whose nested F says `:status :not-supplied`; it is not a finite cascade F waiting to be attached.

## 4. From `:computed-not-attached` to `:not-supplied`

The change is commit `acc4f3c4640b4092710cceaf9d88f50acaeb0663`, **“Stage H4 observed-prefix F and typed-neutral production receipts”** (2026-09-21 04:59:12Z). It did three decisive things:

1. production ranking began passing `:f-prefix-production? true`;
2. `efe.clj` stopped calling `cascade-free-energy/policy-free-energy` when that flag is set;
3. `policy-prefix/production-ranked` was inserted before selection and made the production receipt unconditionally `:not-supplied` pending D admission.

Before this commit, F was genuinely computed from the prospective candidate rollout, identity-default adjudication rates, and the chosen observed-token set. It was not a usable finite F that was arbitrarily thrown away. For effectful policies under identity A, the rollout state differed from the observation, `P(o|π) = 0`, and F was `##Inf`; source comments identify the resulting dark-room/NaN failure. Commit `57b3dcf4` had already made such a non-finite value `:computed-not-attached` instead of feeding it to selection. Therefore the earlier state means **computed on inputs that did not qualify for finite consumption**, then correctly withheld. `acc4f3c4` replaced that prospective calculation with the more faithful observed-prefix design, but staged only the refusal side; its production data/admission side remains absent.

## 5. Smallest honest must-build item

**F-prefix production and Lean-bound consumption (depends on clauses 0, 1, and 2; contributes to clauses 3, 4, and 6).**

- After an ordinary candidate from the B4 field is selected and executed, bind the selected policy identity, execution occurrence, measured outcome, measured A version, previous belief, B version, transition row, and exact posterior into one immutable, deduplicated conditioning step.
- On the next ordinary click, D admits the ordered steps belonging to each eligible policy and records them at `[:decision :selection-certificate :token-belief-input :observation-updates]`; no synthetic evaluator or hand-built prefix may authorize them.
- Replace the unconditional `production-ranked` refusal with a consumer of those admitted steps. It writes the full per-policy receipt at `[:decision :selection-certificate :candidates i :f-prefix]`, including prior, transition, observation, A/B versions, posterior q, per-step F, total F, occurrence identities, and content hash; the exact total is attached to `:f`, marked consumed, and copied to `:g-term-decomposition`.
- The generated witness must prove each step's `q = ExactBeliefTrajectory.exactUpdate A B o sPrev` and consumed `F = PolicyVariationalFreeEnergy.variationalFreeEnergy (fun x => A x o) (ExactBeliefTrajectory.predictedState B sPrev) q`; where q is exact, it also proves the equality to `−log (observationProbability A B o sPrev)`. Selection must then witness `PolicySelection.selectionPosterior` on that exact F.
- Bad cases must reject a duplicate/mismatched occurrence, a prefix not joined to the same policy, identity-default or unmeasured A, missing q, and a logged F whose consumed-at hash differs from selection's input.

There is no honest shortcut. Clause 0 is needed to provide ordinary policies rather than hand-admitted arms. Clause 1 is needed because identity A produced the old infinite F and a stipulated/floored A would violate R1. Clause 2 is needed because the Lean quantity names q explicitly and the current record does not retain it. Only after those dependencies produce a real admitted prefix can clause 3 supply F rather than correctly report `:not-supplied`.
