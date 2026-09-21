# improve-1: learning cascade effect reliability

Discovery, 2026-09-21, branch `fix/narrative-improve-1`, base `5a6b77d9`.
No production edits, clicks, serving-JVM evaluation, or Lean builds.

**B is the first defensible learning target. The Bernoulli transition already
exists; the missing parts are trial admission, parameter accumulation, and
routing a versioned posterior mean into the next model.** The current records
provide one selected-effect failure suitable for an explicitly limited
end-to-end attempt calibration, but **zero complete observed, scheduled
per-firing transition tuples**. Treating that one failure as a per-step B trial
requires an additional execution/clock contract, not just a Beta accumulator.

There is a second finding: at today's C scale, the proposed reliability update
can change G substantially, but in the wrong direction for a simple completion
objective. More failure uncertainty reduces outcome KL through entropy. In the
counterfactual below a failure **increases** the updater's selection probability;
a success makes F11 win. This is the actual scorer's behavior, not numerical
roundoff or an added bonus. Slice 1 should therefore retain evidence and shadow
predictions only.

## 1. Actual model and carriers

This describes current canonical source and the frozen reference run, not a
claim about which code is loaded in the serving JVM. The task forbids that
probe. Source references are relative to futon2 unless otherwise stated.

| Component | Carrier and implementation | Supplied versus derived |
|---|---|---|
| State S | Subsets of the finite, target-qualified token universe; sparse map `{token-set exact-mass}`. Reference universe has 11 tokens. | Domain comes from common candidate/spec/D union, never one universe per candidate. |
| Outcome O | Subsets of that same token universe. A token is a checkable declaration/object fact, not mission success or flight disposition. | Measurement meanings/locators are declared. |
| Action U / policy | Ordered vector of interpreted patterns at each model step. Live cascade repeats its precedence over declared T=2. | Candidate construction supplies guards, outputs and precedence. |
| B | `cascade_model_manifest.clj` `first-enabled`, `pattern-kernel`, `cascade-kernel`, `push-forward`, `rollout`. First enabled, incomplete pattern goes to `s ∪ produces` with theta, otherwise stays at s. No enabled pattern means identity. | Structure is interpreted/supplied. Missing theta explicitly becomes 1, `:theta-source :documented-default`; rational theta in [0,1] is already supported. No learned count is consulted. |
| A | `token-likelihood`: product of per-token false-negative / false-positive Bernoulli channels. `A(o|s)` is identity when both rates are zero for every token. | Rates supplied in options, otherwise zero. All reference rates are zero. No reliability estimate is inferred from prediction mismatch. |
| D | Exact initial distribution passed as `:cascade-belief`; reference is a point mass. | Derived from initialization/declared token evidence, optionally fix-10e admitted previous observations under the explicit switch. This changes D, not B or A. |
| Q | Repeated push-forward of D through B, then A. | Computed. Future rollout is predictive; no future real observations are manufactured. |
| G | `efe/rank-cascade-actions` → `horizon-g-sparse[-cert]`: sum of outcome KL risk and observation ambiguity. | Derived from the common A/B/D, horizon, and C. Identity-A path handles sparse correlated mixtures exactly. |
| F | `cascade_free_energy.clj/policy-free-energy`: same rollout and token likelihood, `-log Σ_s Q_tau(s|pi) A(o|s)` at an actual supplied tau. | Derived when observation placement exists; missing prefix is not an invented F observation. Replay keeps the historical consumed F=0 and uniform habit, beta=1. |

With nonzero A, the optimized scorer additionally checks factorization; a
correlated theta mixture cannot silently enter its independent-token formula.
A multi-output pattern's theta describes the **whole output bundle**, not
independent token successes. The two newly productive reference patterns each
have one output, so no bundle ambiguity affects the calculation here.

`MachineParameters.lean` supplies a distinct restricted two-hypothesis prior/
posterior with supplied likelihood. The Clojure `machine_parameters.clj`
constructs finite registered hypothesis kernels for a separately supplied
machine model; neither this nor the Lean toy feeds learned concentrations into
the reference cascade's theta. The cascade scoring path is separate from the
explicit experimental `:observation-model` branch in `rank-cascade-actions`.
`DirichletLearning.accumulate` is positive-array accumulation, not an existing
runtime bridge from tick records to `ForwardModel.B`.

The paper's broad “no path from tick o or mu into R17” needs qualification
against current source: `war_machine.clj/accumulation-step-for-tick` does pass
observation and one entity's posterior into `machine_accumulation.clj/step`,
which adds their outer product. `machine-accumulation-config.edn` declares
entity `arxana/stack/futon-v1/leaf/2/2`, prior 1.0 and model `wm-status-v1`.
These are observation-channel × entity-status concentrations, not the token
state/action transition counts above, and no normalization-to-cascade-B/A
consumer is present. The three September 21 reference trace forms have no
`:accumulation-state`; the replay also inventories those forms. Thus there is
an existing status recurrence in source, but no demonstrated learned cascade
ForwardModel in these records. Its carrier cannot be silently relabeled as
pattern-effect evidence.

## 2. Retained evidence census

[improve_1_replay.clj](improve_1_replay.clj) inventories both canonical
`futon2/data/wm-d-task-enactment` and `futon3c/data/wm-d-task-enactment`, and all
29 EDN files in `futon2/data/wm-runs`. File hashes, per-row evidence/meaning
hashes and exact results are in [improve-1-results.edn](improve-1-results.edn).
The census is a snapshot of those local stores, not every possible external
archive. No `token-outcome.edn` file was found under either canonical data tree.
None of the 29 run records contains a retained fix-10a comparison, v2 signed
observation projection, or v3 consuming initialization receipt.

Three D-task files exist in those two stores:

| Run | Raw post measurements | Fresh local v2 replay |
|---|---:|---|
| 1789964661, action-a5d2326d… | 11 | admitted |
| 1789952479, action-357124fc… | 0 | refused: `:deferred-artifact-not-fresh-execution` |
| `close-retention-success`, action-0bca9db5… | 0 | test residue; refused: `:independent-jobs-unestablished` |

The replay calls the unchanged `verify-observations-v2` with retained author/
reviewer job snapshots as its read-job port. It rechecks source evidence and
identity; it does **not** claim fresh Agency-owned job retrieval. Fix-10d now
allows the legacy reference identity to verify without changing its digest.
“Signed” here means Boolean polarity, **not a cryptographic signature**.

For the admitted occurrence, artifact revision is
`7a9daa0f2ef9850aff01f1631dbc93c59625b586`; the before revision is retained but
`:before-evidence :not-measured`. All 11 schedules are held with
`:observation-placement-not-declared`. Historical-locator observations and
artifact observations are separate; they must not become duplicate trials.

| Target | Verified artifact true / false | Wanted-token observations | Selected effect trials at attempt grain | Complete per-firing B tuples / A truth-label pairs |
|---|---:|---:|---:|---:|
| M-aif-policy-conditioned-eig | 1 / 3 | 3 false | 1 potential failure | 0 / 0 |
| M-f11-find-production-successor | 1 / 2 | 2 false | 0: not selected | 0 / 0 |
| M-wm-08-external-f2 | 4 / 0 | 1 true, already present | 0: not selected/already present | 0 / 0 |
| M-expressions-of-interest | 0 / 0 | 0 | 0: refused execution | 0 / 0 |
| Other targets in inventoried runs | 0 / 0 in admitted D-task records | 0 | 0 | 0 / 0 |

The AIF trial candidate is `:apparatus/one-authority-per-question` producing
`["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]`. D lacks that token,
the recorded interpreted guard is enabled, both predicted horizon marginals
are 1, and the artifact's completion declaration is observed false. The other
two AIF wants are predicted absent and observed false: they are **not two more
failures of this pattern**. F11's two negatives are not trials of an unchosen
pattern. F2's already-present positives are not newly successful effects.

What the stored tuple actually provides is `(model D, selected cascade u,
post-artifact Boolean o, occurrence, revisions, meaning/evidence pins)`, plus
predicted intermediate states. It does not provide observed intermediate s,
a timestamp assigning the post observation to a particular model tau, or proof
that the selected pattern was enacted as an individual firing. The authority
explicitly retains `:causal-attribution :independent-check-required`. Hence:

- One occurrence-bound endpoint mismatch is available for an **end-to-end
  selected-task delivery** calibration, if that trial grain is declared. It
  estimates production of the checked declaration after this dispatch route,
  including executor behavior; it is not intrinsic pattern causality.
- Zero already-admissible rows support an automatic **per-firing** Beta B
  update under the stronger contract proposed below. No production learned
  parameter update occurred in these records.
- Zero labeled observation-error trials calibrate A. A false completion
  declaration after work is evidence about completion/delivery, not evidence
  that a sensor missed a completion known independently to be true. Learning
  false negatives from this mismatch would confound B with A.

Fix-10a retains exact predicted wanted-token marginals, intended outputs and
raw comparisons. Fix-10b/d gives meaning/revision/occurrence-bound signed
observations. Fix-10e can initialize next D only under the enabled, unchanged
declaration; the source switches currently explicitly default OFF. Those are
necessary pieces, but none records a performed model step or authorizes B
learning. C3/C4 observe presence/declaration text, not correctness of the work.

## 3. Smallest honest model and replay

Use a versioned Beta prior per **target + interpreted pattern + singleton
produced token + guard/interpretation digest + observation meaning + execution
route and trial grain**. Do not pool by bare pattern name across targets.
For a fully observed admitted trial `y∈{false,true}`:

```
alpha' = alpha + 1[y=true]
beta'  = beta  + 1[y=false]
theta' = alpha' / (alpha' + beta')
```

Require positive declared concentrations. Missing/refused/unscheduled data is
not y=false. Deduplicate by occurrence, performed step, effect and evidence
identity; do not count observation replays twice. Require independently checked
preconditions and initial effect absence, actual firing/execution binding and
a declared observation placement. For a multi-effect bundle either observe the
whole bundle once (one shared theta) or commission a new joint transition
model; multiplying separate Beta means is an extra independence assumption.

A future admitted posterior mean can enter existing `pattern-kernel` through
`:theta`, with parameter provenance and model identity carried forward. No new
simulator is needed. An end-to-end attempt-only alternative needs a matching
one-attempt action transition/horizon contract before feeding that theta into
planning. The existing T=2 cascade can retry a failed pattern; one terminal
artifact observation cannot identify how many individual trials occurred.

For sensitivity, explicitly assume the per-step trial contract and a
**hypothetical declared Beta(9,1)** prior on all three patterns. This prior is
illustrative, not fitted or recommended as a default. It changes the old theta=1
model even before any data. A proper positive Beta cannot have mean exactly 1.
Hold recorded C, D, A, T=2, E, F and beta fixed. The hypothetical updater-failure
record gives Beta(9,2), theta=9/11; changing its Boolean to true gives
Beta(10,1), theta=10/11. These are counterfactual input rows, not modified or
re-signed historical evidence. The script performs the Boolean count update,
then calls the actual ranker, sparse scorer, posterior, Bayes choice, and fix-7
comparison implementation.

| Scenario | AIF theta | AIF terminal Q(effect) | G AIF | G F11 | G F2 | Action |
|---|---:|---:|---:|---:|---:|---|
| Recorded | 1 | 1 | 15.170069253170 | 15.171332462691 | 15.171375235023 | AIF |
| Shared illustrative prior | 9/10 | 99/100 | 14.788997805242 | 14.790248382668 | 15.171375235023 | AIF |
| One hypothetical failure | 9/11 | 117/121 | 14.550757177517 | 14.790248382668 | 15.171375235023 | AIF |
| Same hypothetical row true | 10/11 | 120/121 | 14.817579098783 | 14.790248382668 | 15.171375235023 | F11 |

The reference menu is replayed literally, including F2; fix-16 now excludes
that no-new-want candidate at admission. Removing it leaves the AIF/F11 odds
and choice unchanged, but changes their normalized probabilities. Thus these
three-way masses are not claimed as a fresh post-fix-16 tick.

For a singleton new effect with terminal weight w, let
`p1=theta`, `p2=1-(1-theta)^2`, and h be binary entropy. The actual identity-A
risk formula, also checked via the real scorer, is:

```
G(theta) = B - h(p1) - h(p2) - w*p2
B = 15.171375235022659
w_AIF = 229/175347
w_F11 = 5/116898
```

All ambiguity terms remain zero. From prior to failure, total outcome entropy
increases from 0.381084507746 to 0.619355248607 nats. Expected terminal utility
decreases by only about 0.0000301131 nats. Net AIF G **falls by
0.238240627725 nats**. The failure has not improved expected completion;
its uncertainty is closer to the nearly uniform preference distribution.
The sparse calculation is exactly KL, not a loss of faithfulness to KL.

The kernel uses a fixed posterior mean for each predictive firing. It does not
integrate a persistent Beta latent parameter across future steps: for example,
Beta(9,1)'s fully integrated two-attempt success probability is
`1-E[(1-theta)^2]=54/55`, rather than the plug-in `99/100`. Nor does it simulate
future learning of theta. That distinction needs an explicit model declaration;
calling the plug-in rollout full Bayesian parameter planning would be wrong.

## 4. Requested witness and its limits

The replay reproduces all three recorded Gs and posterior probabilities within
1e-6. Prior→failure changes Q at both steps (9/10→9/11 and 99/100→117/121),
changes the AIF/F11 log-odds G contribution from **0.001250577426 to
0.239491205151**, and changes `:near-tie?` from true to false under an explicitly
supplied illustrative threshold 0.01. No default threshold was invented.

For the failure counterfactual, the real fix-7 action record has
`:decided-by #{:G}`: neutralizing G changes AIF to F11, while neutralizing E or
F does not. AIF posterior rises from **0.372996860634 to 0.430174087334**.
The success control selects F11, whose G lead is 0.027330716115. Its action
record says **`:robust`**, because the declared tie-break also selects F11
when G is neutralized. That label does not assert that G was unchanged or
irrelevant. The output retains each neutralization's choice and mass.

This is a computed **model sensitivity witness**, including opposite Boolean
controls and an actual changed action. It is not the stronger production
witness of an *admitted real trial* updating a live parameter: the retained
reference lacks the per-step execution/observation contract. The production
witness cannot honestly be claimed today. The obstacle is not that the C scale
makes any G change invisible: uncertainty creates a much larger response than
the tiny desired-outcome utility. Preference scaling alone also does not
identify an epistemic benefit from learning a reusable parameter.

## 5. Lean correspondence and proposed slices

Read-only inspection at mathlib4
`77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`:
`PolicyRollout.lean` (ForwardModel and rolloutState), `CascadeTransition.lean`
(patternKernel), `PolicyHorizon.lean`, `OutcomeRiskKL.lean`,
`DirichletLearning.lean:19,60`, and the existing MAP-rnode-to-lean analysis.

Positive Beta concentrations are the two-outcome special case of the positive
Dirichlet carrier. Success/failure counts are one-hot outer-product updates.
For B, the conditioning index must additionally identify action/guard or
state-action context; the existing outcome×state accumulation theorem alone
does not establish that operational join. Normalizing concentrations and
routing their means into B needs a correspondence/receipt of its own. The
existing guarded theta kernel is normalized and already supports the proposed
plug-in value. Fixed B remains a valid ForwardModel input; no current theorem
proves learned theta calibrated, per-step trials actually performed, or the
resulting action useful. A different joint model for a persistent uncertain
parameter would require a larger state/model and new correspondence work.

1. **Record only, no change to selection.** Add a prospective learning-trial
   receipt naming trial grain, before/after observations, occurrence/performed
   step, guard/effect/meaning and route digests, placement, admission or typed
   refusal, and deduplication identity. Retain the Beta calculation and two
   shadow rollouts under an explicit prior; keep all production theta values.
   The historical updater receipt must say why it is held, not silently count
   it. Pin missing-as-unknown, unselected target, already-present effect,
   duplicate replay and revised meaning negatives.
2. Establish the execution/clock contract. Either bind actual individual
   firings with independently observed preconditions and scheduled outcomes,
   or declare/rebuild an end-to-end attempt action model. Collect genuinely
   admissible trials; preserve the distinction between checked declarations
   and substantive task success. Do not reinterpret old held records merely
   to obtain counts.
3. Route versioned means to existing theta behind a **declared switch,
   default OFF**. Receipt must show prior/counts, normalized parameter,
   consumed model identity, changed Q/G and fix-7 comparisons. Reuse both
   failure and success controls above; require no unsupported A calibration.
4. Before enabling, review the measured uncertainty incentive with improve-2's
   C/disposition work. If information gain about reusable parameters is wanted,
   represent it explicitly and distinguish it from irreducible execution noise.
   Do not clip entropy, tweak weights to force the desired winner, or claim
   stronger sensor evidence than C3/C4 supplies.

## Reproduction and validation

From `/home/joe/code/futon2-improve-1`, OpenJDK 21.0.11 / Clojure 1.11.1:

```sh
clojure -M holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_1_replay.clj > holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-1-results.edn
clj-kondo --lint holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_1_replay.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- holes/labs/wm-contract/runs/fixlist-2026-09-21/improve_1_replay.clj
git diff --check
```

The optional first script argument replaces `/home/joe/code`, the root of the
two canonical read-only data stores. Script prints EDN to stdout only. Fresh
replay exits 0 with `:validation
:recorded-G-and-posteriors-match-and-record-change-alters-Q-and-G-flip-witness`.
Kondo: 0 errors / 0 warnings. Parens: OK. No production/test namespace changed;
no failing-on-main regression is claimed for this discovery. The test registry
only warrants its supported namespace-test/Lean-build commands, not this
standalone discovery script; exact commands, input hashes, assertions and
fresh results are supplied instead of a fabricated test warrant.
