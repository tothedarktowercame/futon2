# Foundations that can change selection — discovery, 2026-09-22

**A, B, C and parameter novelty fail the selection witnesses below. Outcome-learned E and enabled observation initialization pass their consumer witnesses.** It would be inaccurate to make all six fail: two of the requested mechanisms already affect the production selector under the stated inputs. Neither passing fixture establishes that today's closed attempts supplied those inputs. The admission precondition fails for click 2 and for the current declared-source census.

Code examined and executed: futon2 main `6c8e722b713b47947f23831566b9ab72c63aef35`, branch `fix/foundations-witnesses`. Main advanced during discovery; the branch was rebased and the probe rerun. No production changes, WM clicks, serving-JVM evaluation, or store writes. This is a discovery packet, not authorization to resume the line.

## Runnable witnesses and scope

[probe.clj](probe.clj) runs from the repository root:

```sh
clojure -M:test holes/labs/wm-contract/runs/foundations-2026-09-22/probe.clj
```

It prints `:PASS` / `:FAIL` records for the requested behavioral contracts. Expected failures are data rather than failing tests installed in `test/`. An invalid fixture/control throws. A zero exit code means the probe completed, **not that the foundations all passed**.

The main fixture reconstructs assembled problems from click 1's retained domain declarations, facts, candidate orders, locators and interpretation/construction receipts. It reconstructs live-C mission masses from the consumed preference specification. It calls `cascade-problems/assemble` → `war-machine/cascade-decision` → actual joint `efe/rank-actions` → `policy/select-action-cascades` → decision gate. Two hard assertions require two admitted candidates and exactly the recorded G values. No manually assigned G scores or mocked selector are used.

The external habit snapshot and predecessor-authority read ports are supplied in memory; the scorer wrapper only captures inputs/results and calls the original function. The fixture declares an empty ticket queue to compare the two historical candidates, rather than claim they are eligible under today's front-ticket queue. It replaces external reads, not admission, arithmetic, validators or selection. E uses the real outcome rule and pure count accumulator; persistence is intentionally outside this packet. Close initialization uses a typed fixture at the verified-observation read boundary, including a tampering negative control; it does not generate or independently verify a real closed attempt.

Default retained-record root is `/home/joe/code/futon2`; `FUTON2_RECORD_ROOT` can point at a copy. Inputs:

- `data/wm-runs/tick-run-record-2026-09-21-1790033693.edn`: `:decision/:selection-certificate` candidates, token-belief-stage domain inputs, and precision-family model.
- `data/wm-runs/tick-run-record-2026-09-22-1790037762.edn`: candidate count.
- Current `resources/wm/cascade-sources/*.edn`, read-only mechanical observations, and the production source/admission functions for the separate census. The output retains declaration paths and SHA256 values; this census is time-sensitive.

The two [stage](../click-r4-1-2026-09-21/STAGES.md) [accounts](../click-r4-2-2026-09-22/STAGES.md) establish the actual recent click outcomes; illustrative fixture records in this probe are not additional WM outcomes.

## Results and smallest missing changes

File:line references below are at the examined main revision. Thresholds are declared by this probe, not inserted into production.

| Part / runnable function | Required behavioral test | Fresh main result | Smallest honest change and status |
|---|---|---|---|
| **A** — `a-witness` | On the two reassembled real candidates, supply a non-identity observation model; the maximum posterior change must exceed **0.0001**. | **FAIL:** delta **0.0** through `cascade-decision`; declared rates do not reach the joint scorer. Positive control uses the real scorer and selector with the same rates: delta **0.0003162189212701505**, so the failure is the top-level input path, not an insensitive fixture. | Thread a validated declared `:adjudication-rates` map and provenance into the joint scoring options at `scripts/futon2/report/war_machine.clj:6240`. The receiving scorer already supports nonzero rates at `src/futon2/aif/efe.clj:1095`; the top-level forwarding/declaration is **not written**, rather than switched off. A learned/calibrated rate source remains separate required work. |
| **B** — `b-witness` | A matching recorded failure count must change planned Q, then G and the selected action, while the candidate menu and habit remain fixed. | **FAIL:** count consumer admits the row (endpoint beta goes from 1 to 2), and the learning receipt changes; rollout traces, G and choice are identical. G stays `[9.704757850639144 9.706022726492865]`. | Introduce an admitted attempt-grain forward model from the count snapshot, and consume it before rollout at `war_machine.clj:6150` / `:6240` and `cascade_model_manifest.clj:378`. Binding learned attempt success to individual pattern causality would be unjustified. `attempt_learning.clj:1` explicitly says production consumes no counts; `parameter_novelty.clj:197` records `:production-B-consumption :none`. The learning-to-planned-Q connection is **not written**. No existing enable flag makes it happen. |
| **C** — `c-witness` | On the two real candidates, declared preferences must overcome an opposing 2:1 habit, so the preference winner remains selected when habit alone favors the other candidate. A future class-domain version must obtain Q over attested run-ending classes, then consume the declared class preference. | **FAIL:** G spread **0.001264875853721037**, versus habit log-odds **0.6931471805599453**. Uniform habit picks M-aif-policy-conditioned-eig; opposing habit picks M-f11-find-production-successor. The consumed C is still over target-qualified wanted tokens. | Supply the missing predictive bridge to run-ending classes and score that declared domain at `war_machine.clj:6216–6244` / `efe.clj:1006`. `focus_receipt.clj:100–101` explicitly holds the predictive kernel/local C; `run_ending_classification.clj:67` classifies an observed close, not a planned outcome. Class-C consumption is **not written**. The classifier is merged, but does not itself supply P(class\|policy). See the domain caveat below. |
| **G / parameter novelty** — `g-witness` | Two matched policies with the same rollout semantics, C, E and parameter mean, but different expected learning, must select the more informative policy. | **FAIL:** both serving G values are **4.158883083818247**. Means are both **9/10**; information gains are **0.004915823711360012** and **0.042186147994622836** nats. Selection keeps the less informative tie winner; both receipts say novelty is not consumed. | Bind an admitted parameter model into the scorer before selection, evaluating theta-marginal risk plus theta-conditional ambiguity. `parameter_novelty.clj:104` already computes an illustrative theta-latent decomposition; `:210` labels it unconsumed, and `policy.clj:209` attaches it only in the certificate after scoring. Production consumption is **not written**, not disabled by a flag. Do not subtract another novelty bonus from risk that already includes predictive entropy. |
| **E** — `e-witness` | After **N = 3** qualifying observed-success outcomes for the initially losing policy, the outcome-learned snapshot changes the selected action from the uniform-habit choice. | **PASS:** M-aif-policy-conditioned-eig → M-f11-find-production-successor; habit becomes **4:1**. The actual `cascade-habit-observed-want-v1` rule authorizes each increment. | **No code change needed for this consumer witness.** `cascade_habit_reinforcement.clj:57` / `:85`, `cascade_prior.clj:108`, `policy.clj:313`, and the runner close call at `full_loop_runner.clj:3866` are already wired. This is count accumulation gated by an observed predicted want, not a separately learned success-rate prior. Actual qualifying outcomes, not selection events, are needed. |
| **Update at close** — `update-witness` | With initialization on, a retained verified-close observation changes the next selection's consumed q0; switching it off must leave q0 unchanged, and a tampered meaning must be refused. | **PASS at the consumer boundary:** off unchanged; on changed; tampered digest refused; status `:observed-initialization`. The scorer's precision-family q0 changes, not merely a diagnostic receipt. | **Merged but disabled in all four source declarations.** Enable the supported `:token-initialization` policy in the relevant `resources/wm/cascade-sources/<target>.edn` only with qualifying evidence. Consumption is `token_belief_predecessor.clj:109`, `token_initialization_policy.clj:62`, `war_machine.clj:6176–6189`. No additional consumer code is needed for this witness. This does not prove the real close writer/signature/transport path end to end. |

### What these tests do and do not prove

A's illustrative false-positive/false-negative rates are both 1/2 for each token. That makes the observation uninformative: it proves that the existing scorer can respond to non-identity A and that the top-level option is dropped. It is **not** a proposed production calibration, nor evidence that adding noise is progress. It removes the tiny preference distinction rather than overpowering a 2:1 habit.

B supplies an in-memory snapshot with the production ledger row shape and matching family, meaning and contract. The count is demonstrably consumed by the existing endpoint-prior reader. It is not a newly signed learning trial, and no new causal authority is claimed. The first required assertion, “changed record changes planned Q”, already fails; choice sensitivity is downstream work after that connection exists. A particular single trial is not guaranteed to flip every future model, so a future implementation must retain an explicit witness with enough evidence to cross the decision boundary.

C's class-domain test cannot currently be expressed by passing a valid class-C input: no such consumed input exists. The runnable failing test quantifies the actual declared token-C failure; it does **not** pretend that putting 55/35/5/5 onto token weights tests Joe's preference. The [improve-7a receipt](../fixlist-2026-09-21/improve-7a-IMPLEMENTATION.md) records those numbers as a global long-run estimate and holds local C until the conditional outcome kernel exists. Even with that kernel, 55:35 gives only `log(55/35) = 0.451985…`, which cannot beat 2:1 habit at beta 1 with other terms equal. A focus-versus-known-failure contrast gives `log(55/5) = 2.397895…`, which can. Candidate outcomes and their probabilities matter; the ratio alone does not guarantee a flip. No class assignments to these two historical candidates were fabricated.

G uses one reassembled real problem and a clearly marked synthetic twin policy with the same guard and produced token. The matched policies have the same serving G and 0.9 parameter mean. One has prior 9/1; the other reaches 90/10 through 81 success and 9 failure count rows. Assertions require equal serving G/means and unequal computed information. This separates parameter learning from ordinary differences in outcomes. The fixture does not assert that the synthetic twin is available live. Its information advantage is only **0.0372703243** nats: even correct consumption would not overcome a 2:1 habit on these parameters, though it would break the uniform-habit tie.

The update control changes a wanted token from unknown-to-the-fresh-reader/absent-in-initialization to an admitted true observation at the same artifact revision. It asserts the actual q0 used by selection and rejects changed meaning. It does not rename this deterministic initialization as learned B or Bayesian model learning. If the fresh observation already supplies the same value, enabling the policy changes nothing. Old observations at an unordered revision must still be refused. The existing stronger fixture `test/futon2/aif/token_observation_initialization_test.clj:two-ticks-use-signed-false-in-actual-selection` tests the artifact production/reader path but creates temporary artifacts; it was inspected, not run under this packet's no-store-write restriction.

## Admission precondition and observed frequency

At least **two supported policies with distinct first actions in the eligible ticket stratum** must survive. Two policy ids that marginalize onto the same first action do not create an action choice. Both require:

1. Nonempty admitted facts/universe, explicit wants, valid interpreted patterns and C3/C4 locators for every token (`cascade_problems.clj:103`).
2. Nonempty executable orders, construction receipts, and per-pattern interpretation receipts; a newly wanted token must be reachable within the declared horizon (`war_machine.clj:6349`).
3. Common declared horizon, beta, preference schedule/scales; valid live-C freshness and model support.
4. Positive selection support and eligibility under the current front-ticket declaration (`ticket_queue.clj:plan`). A single eligible front target can remove inter-target choice even when the unrestricted family has multiple candidates.

Observed retained clicks:

| Run | Targets considered (stage account) | Scored candidates | At least two? |
|---|---:|---:|---|
| 2026-09-21-1790033693 | 283 | 2 | yes |
| 2026-09-22-1790037762 | 283 | 1 | no |

That is **1 of 2 observed clicks**, not an estimate that “half of future clicks can choose.” No new click was made. Click 2's account reports 282 refused targets and no admitted proposal among 90 supply items.

The fresh read-only declared-source census found **4 declared targets → 3 assembled targets → 1 admitted target / 1 candidate**. One assembly refusal is `:universe-not-admitted`; two assembled targets have `:no-new-wanted-token` and consequently `:no-admissible-candidate`. No declaration enables observation initialization. The probe prints declaration digests, so this census is attributable. It excludes generated wants, proposal/repair supply and actual front-ticket eligibility; it is not a fabricated full `judge` execution. Generated mission-hole wants do not establish pattern applicability by themselves (`mission_hole_wants.clj:87`), and supplying more target names is not enough.

## Priority for making a subsequent choice depend on more than habit

**Admission comes first.** With one eligible first action, no A/B/C/G change can flip it. Admit a second genuinely executable, scoped candidate in the eligible stratum; do not relax the receipts, locators or stop-the-line rule to manufacture competition. Because of this precondition, no single parameter change can honestly be promised to change the next click today.

Conditional on that precondition, rank work by prospective non-habit effect:

1. **C on the correct run-ending domain.** It can produce a preference contrast larger than the observed habit contrast, but only with a declared predictive class kernel and genuinely different candidate outcome distributions. Implement the missing class-domain boundary, not a larger arbitrary token-weight multiplier. This is a construction, not a one-line flag.
2. **B from admitted attempt outcomes into planned Q.** This addresses the actual “predict success despite observed failure” problem and gives repeated evidence a route to change G. The minimal useful delivery is the changed-record → changed-Q witness before claiming useful choices; preserve attempt/pattern causal grain.
3. **A forwarding plus an evidenced calibration.** The forwarding gap is the smallest code edit here and has a positive control. Without justified rates and candidate-dependent observations it need not change the winner or improve the choice.
4. **Parameter novelty in a coherent latent model.** Its matched-policy witness is ready, and receipt arithmetic exists. Consume the coherent risk/conditional-ambiguity model once; do not double-count entropy. This fixture's information contrast is smaller than the observed habit advantage.
5. **Enable close observation initialization where evidence qualifies.** This is the smallest configuration change, already passes, and can change q0. Freshly observed facts often make it redundant; it is not itself A/B learning and cannot be promised to change choice.
6. **E needs qualifying outcomes, not another selection implementation.** It already changes choices. It cannot satisfy the specific goal of making choice depend on something other than habit.

## Gates and fresh output

```text
clj-kondo --lint holes/labs/wm-contract/runs/foundations-2026-09-22/probe.clj
errors: 0, warnings: 0

emacs --batch -l /home/joe/code/futon4/dev/check-parens.el \
  --eval '(arxana-check-parens-cli)' -- --files \
  holes/labs/wm-contract/runs/foundations-2026-09-22/probe.clj
OK

clojure -M:test holes/labs/wm-contract/runs/foundations-2026-09-22/probe.clj
A FAIL; B FAIL; C FAIL; G FAIL; E PASS; update-at-close PASS; admission FAIL
read-only-store-check :passed

 git diff --check
 git diff --cached --check
(no output)
```

The probe also asserts unchanged file names, lengths and modification times under the canonical repair-obligation, habit and learning-trial stores during its witness execution. This checks the observed run; the source contains no store-writing call. No suite warrant or full runner validation is claimed. Only this report and the standalone probe are committed.
