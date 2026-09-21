# Improve-5 — parameter novelty in cascade G

2026-09-21; codex-13; base e675673b; discovery only. No production change,
click, serving-JVM evaluation, or Lean build. Learning is a stated preference;
the open question is its model and relative strength, not whether Joe wants it.

## 1. What is missing, and the precise quantity

The serving cascade branch is `efe/rank-actions` → `rank-cascade-actions`
(`src/futon2/aif/efe.clj:1357–1389`) →
`cascade-model-manifest/horizon-g-sparse[-cert]` (same-named source:1019–1098).
It computes risk + ambiguity at the common horizon. It has transition
probabilities, not a posterior distribution over those probabilities. Its
frozen default deterministic B does not supply an uncertain parameter carrier.
Therefore the historical runs do **not** contain a computable real-model
parameter EIG. A missing model is a typed absence, not evidence of zero learning.
The numbers below are explicitly declared counterfactuals using real frozen
candidate, C, habit, F, and action-projection records.

For one eligible effect endpoint in one attempt, declare
θ ~ Beta(a,b), Y | θ,π ~ Bernoulli(θ), n=a+b, p=a/n. Success updates to
Beta(a+1,b), failure to Beta(a,b+1). In nats, with ψ the digamma function:

```
KLsuccess = log(n/a) + ψ(a+1) − ψ(n+1)
KLfailure = log(n/b) + ψ(b+1) − ψ(n+1)
Iπ = p KLsuccess + (1−p) KLfailure
   = h(p) + p ψ(a+1) + (1−p) ψ(b+1) − ψ(n+1)
   = H(Y|π) − Eθ H(Y|θ,π).
```

This is expected posterior/prior KL, equivalently parameter/outcome mutual
information, the standard Bayesian experimental-design quantity
([primary derivation context](https://pmc.ncbi.nlm.nih.gov/articles/PMC7514425/)).
For positive integer a,b, ψ(k+1)=H_k−γ: the probe uses exact rational harmonic
sums before conversion to double and separately checks the weighted posterior
KL expression. Values per one observable endpoint:

| Beta | Mean | Novelty, nats | Predictive entropy, nats |
|---|---:|---:|---:|
| (1,1) | .5 | .193147180560 | .693147180560 |
| (9,1), illustrative prior | .9 | .042186147995 | .325082973391 |
| (90,10) | .9 | .004915823712 | .325082973391 |

For independent parameters and independent endpoint observations, the policy
term is the sum over **distinct eligible effect endpoints**. Without this
factorization use the joint predictive observation and joint posterior KL;
summing marginal gains is not generally justified. A masked/unobservable
endpoint has zero information only when the declared observation model proves
it: missing observation-model evidence is an absence. Perfect observation of
Y is assumed in these numbers; noisy tests require A and generally lose Beta
conjugacy after observing their result.

### Trial grain and horizon

The checked-in illustrative prior (`resources/wm/learning-trial-prior.edn`)
is record-only, per-pattern-effect-firing, with execution clock undeclared.
Improve-1b's inspected draft deliberately replaces this with a
`:selected-cascade-effect-attempt` contract: initially absent, predicted-positive
effect; post-build artifact revision; authenticated occurrence and matching
observation/meaning/route; one selected cascade/effect trial, not one per
simulated firing. Family identity includes target, cascade/ordered patterns,
effect and route. It establishes delivery calibration, **not individual pattern
causality**. Do not feed attempt counts into a per-firing B parameter.

For a declared terminal observation at τ=T, use
`Gκ(π)=Στ [riskτ + ambiguityτ] − κ I(θ;Y_T|π)` once. Do not repeat the same
endpoint novelty at both simulated steps of T=2. If multiple real observations
are available, use the chain rule `Στ I(θ;Yτ | Y<τ,π)`, with posterior updates
inside the expectation, rather than repeatedly charging the initial prior.
For two firings sharing θ, even predictive terminal success is
`1−E[(1−θ)^2]`, not `1−(1−Eθ)^2`; success at that endpoint does not give the
single-Bernoulli Beta(a+1,b) posterior. A new endpoint transition model or a
proper per-firing likelihood is required before production use.

Draft provenance (not assumed merged/serving): worktree
`/home/joe/code/futon2-improve-1b`, files
`src/futon2/aif/attempt_learning.clj` SHA256
`d66352ef40a1c86853cfd45f40ce00d01c1ddc630f53f1031a30c0fec081665b`,
`resources/wm/attempt-learning-contract.edn` SHA256
`d100064bc58956df61a5dd566a9fdc4b13dca434a76e8b3a5228abbd2b415e17`.

### Lean correspondence and needed theorem

`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:7089–7095`
defines parameter prior and outcome-conditioned posterior kernels;
`:7258` is posterior/prior KL and `:7267` averages it over the predictive
outcome kernel. `Outcome Obs` is the dependent sum of vertex observations,
not a policy probability. A policy-indexed endpoint bit can occupy the relevant
observation vertex. The actual kernel carrier has finite listed support;
continuous Beta on θ∈[0,1] is **not** a direct instantiation of that finite sum.
`ExpectedInformationGainWitness` and the one-point parameter counterexample
at :7339–7368 do not connect real cascade B to a parameter posterior.

Needed: a measure/density version (or explicitly proved discretization limit),
Beta/Bernoulli conjugacy, normalized predictive distribution, the displayed
closed-form equality/nonnegativity, and an interface theorem binding each
counted attempt observation to the same θ that parameterizes the declared B.
For independent endpoints prove additivity; otherwise prove the joint formula.
Then prove exactly one novelty contribution per declared observation time.
`EpistemicValue.lean:16–44` proves the **state** MI decomposition and introduces
action-conditioned A for checks; its comments explicitly exclude parameter
novelty. No theorem there supplies these parameter/model bindings.

## 2. Frozen menus and numerical results

Reproducible inputs: [compressed extracted records](improve-5-evidence/),
[all 324 candidate/case rows](improve-5-evidence/candidates.csv),
[complete results including fix-7 comparisons](improve-5-evidence/results.edn),
and `test/futon2/report/novelty_discovery_test.clj`.
The test replays all 27 original G values through the real sparse model and
all 27 original posterior entries through `selection-posterior`. It preserves
F, habits, β=1, candidate identities, action marginalization, and tie-breaks.
C0 policies participate in normalization but have nil first actions and are
excluded from enactable action choice, exactly as in the selection projection.

Two separate arms prevent a model change being attributed to novelty:

* **fixed-recorded-G**: subtract κI from frozen G. Diagnostic overlay only:
  the old deterministic B and the hypothetical uncertain Beta are not a coherent
  joint generative model. κ=0 exactly reproduces the historical posterior.
* **factorized-attempt-endpoint**: a declared sensitivity model, not today's B.
  Start with the historical deterministic terminal state; randomize each newly
  produced wanted token independently with p=a/n, once at the endpoint. Keep
  the earlier uniform C and terminal weighted C. Recompute terminal KL using
  the real preference-member/log-probability functions; A is identity.
  Then compare κ=0 and κ=1 on that **same** model. The earlier risk is |U|ln2;
  terminal G differs by `−m h(p)+(1−p)Σeffect w` from frozen G.

The second arm is internally normalized but does **not** establish that the
independent joint outcomes are realizable by today's ordered cascade. In
particular EOI's two effects can have guard dependencies. Their true joint
likelihood must be declared before the two marginal gains can be summed in
production. These are sensitivity values, not retrospectively learned models.
Likewise effect eligibility here is a predictive opportunity, not proof of an
authenticated future occurrence or that the historical post-build observer
could see every effect. No missing scheduling/observability receipt is forged.

### Reference 1789964661

A = `M-aif-policy-conditioned-eig/C1`, F =
`M-f11-find-production-successor/C1`, W = `M-wm-08-external-f2/C1`.
A and F each have one initially absent, predicted-positive wanted effect
(`h6378c65a4012`, `h9ab212b3281d`); W has none: its produced tokens are already
present. Frozen G=(15.170069253170,15.171332462691,15.171375235023),
posterior=(.333618860083,.333197695627,.333183444290).

Here and below κ=1; CSV also gives κ=0 and G-base for every row.

| Arm; prior | G A / F / W | Posterior A / F / W |
|---|---|---|
| fixed; 1,1 | 14.976922073 / 14.978185282 / 15.171375235 | .354355807 / .353908464 / .291735728 |
| fixed; 9,1 | 15.127883105 / 15.129146315 / 15.171375235 | .338274660 / .337847618 / .323877721 |
| fixed; 90,10 | 15.165153429 / 15.166416639 / 15.171375235 | .334164836 / .333742982 / .332092182 |
| endpoint; 1,1 | 14.284427883 / 14.285059488 / 15.171375235 | .414717520 / .414455665 / .170826814 |
| endpoint; 9,1 | 14.802930730 / 14.804067619 / 15.171375235 | .371654180 / .371231891 / .257113930 |
| endpoint; 90,10 | 14.840201054 / 14.841337943 / 15.171375235 | .368060607 / .367642401 / .264296991 |

Per-candidate novelty is (I,I,0), with I in §1. Every shared-prior arm still
chooses A. Fix-7 policy `:decided-by :G`, action `:decided-by #{:G}`; habit/F
contributions zero. Policy log-odds advantage A/F is .001263209522 in the fixed
arm; .000631604761 at endpoint p=.5 and .001136888570 at p=.9. These report the
combined G contribution; they do **not** identify novelty separately.

A distinguishing control holds p=.9 for every candidate but uses A=(90,10),
F=(9,1), W=(90,10). At κ=0 A wins, mass .367581855546; at κ=1 F wins,
mass .376349002853. Parameter novelty advantage .037270324282 exceeds F's
pragmatic/entropy disadvantage .001136888570. The switch occurs above
κ≈.03050. This is a synthetic concentration control, not evidence these
historical families actually have different learned concentrations. It shows
an action can change **because of novelty**, not just because B was changed.

### Earlier 1789952479

24 policies: EOI C1 and C2 each expose two new predicted wanted effects;
21 C0 policies and W/C1 expose zero. EOI C1 orders obligation, bounded-execution,
stop-the-line; C2 orders obligation, stop-the-line, bounded-execution. Their
endpoint pairs differ, but both have m=2. Each EOI novelty is 2I; every other
policy's is zero. The CSV explicitly names all 24 candidates, including all
21 C0s (not an omitted top-k ranking).

Frozen EOI G=170.558868022824 for both; other G=170.561998957485.
Habits: EOI C1=1/6, dionysus C0=.1, every other policy=1/30. Frozen posterior
EOI C1=.167084516759, C2=.033416903352, dionysus=.099937322486,
each remaining policy=.033312440829.

| Arm; prior | Novelty each EOI | G each EOI | p C1 / C2 / dionysus / each other |
|---|---:|---:|---|
| fixed; 1,1 | .386294361120 | 170.172573662 | .224631205 / .044926241 / .091305319 / .030435106 |
| fixed; 9,1 | .084372295989 | 170.474495727 | .178640448 / .035728090 / .098203933 / .032734644 |
| fixed; 90,10 | .009831647425 | 170.549036375 | .168401734 / .033680347 / .099739740 / .033246580 |
| endpoint; 1,1 | .386294361120 | 168.787844768 | .496472825 / .099294565 / .050529076 / .016843025 |
| endpoint; 9,1 | .084372295989 | 169.824642874 | .286024028 / .057204806 / .082096396 / .027365465 |
| endpoint; 90,10 | .009831647425 | 169.899183522 | .272189467 / .054437893 / .084171580 / .028057193 |

Non-EOI G remains 170.561998957485 (roundoff <1e-12). Same selected action
in every κ=0/1 pair: EOI obligation. Both EOI policies marginalize to that same
first action. Actual fix-7 output: policy `:decided-by :habit`, comparing equal-G
EOI C1/C2 with log habit ratio ln5=1.609437912434; action `:decided-by :robust`.
No near-tie threshold was supplied (`:threshold-undeclared`). Thus policy
habit discrimination must not be reported as an enacted-action change.

### Failure/entropy interaction and double counting

Improve-1's per-firing experiment gave a failure-induced G drop .238240627725,
mostly increased predictive entropy, under nearly uniform C. This discovery's
**different attempt endpoint** model also exhibits the effect:

| A prior (others 9,1) | G before novelty | Novelty | G after novelty | p(A) |
|---|---:|---:|---:|---:|
| 9,1 | 14.845116877963 | .042186147995 | 14.802930729969 | .371654179821 |
| 9,2, hypothetical failure | 14.696167391358 | .041599630518 | 14.654567760840 | .406907637409 |
| 10,1, hypothetical success | 14.865551881443 | .038366256079 | 14.827185625364 | .366007852271 |

Failure makes novelty slightly **smaller**, yet makes A more attractive:
the entropy reward dominates. Adding novelty does not repair the pragmatic
scale problem. These hypothetical updates are sensitivity controls, not valid
historical learning receipts inferred from a build disposition.

For this endpoint model, `H(Y)=Eθ h(θ)+I(θ;Y)`. The first part is outcome noise
remaining even if θ were known; the second is reducible parameter uncertainty.
Beta(9,1) and Beta(90,10) have equal predictive entropy but very different
novelty. At fixed p and concentration→∞, novelty→0 while h(p) remains positive.
A known fair coin has maximal outcome entropy but zero parameter novelty.

There is an accounting choice: identity-A predictive risk contains `−H(Y)`.
After marginalizing uncertain θ it already rewards BOTH components of that
entropy. Subtracting κI additionally makes the epistemic coefficient 1+κ in
this particular predictive-risk objective. It is an explicit extra preference,
not automatically the canonical decomposition. An alternative generative
objective using θ as latent and endpoint likelihood Y|θ has
`risk + Eθ H(Y|θ) = expected preference cost − I(θ;Y)`; joint state/parameter
models require the corresponding conditional MI decomposition. Decide which
latent carrier and ambiguity term are intended before claiming “novelty was
missing, so subtracting it cannot double-count.” Current fixed-parameter B
contains no posterior over θ; that fact does not remove the accounting issue
once uncertain B is introduced.

## 3. Existing heuristic audit

Bounded search: futon2 `src/`, `scripts/`, `test/` and futon3c `src/` for
`predictability-bonus`, `model-uncertainty-fn`, `rank-star-map-actions`,
`rank-with-star-status`, `rank-graph`; followed scoring branches.

| Mechanism | Reachability and meaning | Recommendation |
|---|---|---|
| `efe.clj:431–448` predictability-bonus | Σ max(0,1−predicted variance), invoked by `compute-efe:737`; negative weighted term only in controller-augmentation mode (:805–811). Rewards low variance, including predictable no-op, not posterior learning. Library default is legacy augmentation (:628), but WM arena explicitly defaults telemetry-only (`war_machine.clj:946–955`); env override restores old channel ranking. Cascade branch never calls it. | Keep named predictability telemetry; retire its interpretation as epistemic value. Do not port it to cascade novelty. Any retained channel control is a declared predictability preference. |
| `efe.clj:236–255,754,840,854` model-uncertainty-fn | On graph/open-mission channel scoring, subtracts weighted supplied uncertainty. Default constantly zero. No observation-conditioned posterior or expected reduction. `actuator_a6.clj:125–157` can inject it into star-map rankings. Internal counterfactual/rank helpers use those functions; no serving WM cascade caller found in bounded search. | Relabel as cooccurrence-spread heuristic and keep only explicitly declared legacy diagnostics/control; retire as evidence of EIG. Never substitute its magnitude for nats of parameter KL. |
| `a4a.clj:213–255,277–311`; `a4a_substrate.clj:274–301` | Dirichlet moments over pattern/mission cooccurrence; concept stddev sums, constellation RMS stddev, summed distinct constellations per mission. Reads EDN substrate; absent/read failure returns zero. This is uncertainty in a different model, not delivery B, and it can stay high for uninformative actions. | If retained, record model identity and missing substrate separately from calibrated certainty. Learning value needs a predictive observation likelihood and Bayesian update for that model too. |

`Holes.lean:7339–7368` already provides a counterexample: the model-uncertainty
bonus can be 1 when canonical parameter EIG is 0. Neither heuristic reaches the
live all-cascade branch. Callable library paths are not evidence that WM uses
them. The arena's telemetry default also means “compute-efe calculates it” is
not sufficient evidence that it changes even the legacy channel choice.

## 4. Concrete decision and slices

Joe's strength question: **holding route, habit, cost and predicted delivery
rate equal, should .0372703 nats more expected parameter learning justify
.00113689 nats worse pragmatic score?** That is the A/F control above: yes at
κ=1; no below κ≈.03050. More generally, name a tolerated expected loss of a
specified warranted result for that amount of learning. The pragmatic weights
must represent Joe's results/process preferences first (improve-2 Q3); changing
β scales G relative to habit/F and is not a substitute for κ's relative scale.
Also decide whether this is extra learning preference on predictive risk or
a joint state/parameter EFE with conditional ambiguity. The model designers
must supply the observation/joint-effect contract; it is not Joe's job to invent
missing endpoint likelihoods.

1. **Record only:** versioned per-policy novelty receipt. Bind prior ledger
   revision, family (target/cascade/effect/route), observation contract/version,
   τ=T, trial grain, units=nats, eligible endpoints, joint/factorization witness,
   expected KL, and typed absence reasons. Keep illustrative and learned priors
   distinct. Record shadow κ and separated pragmatic/ambiguity/novelty terms;
   selection bytes unchanged. No retroactive learning from bare outcome labels.
   Tests: Beta identities above; same mean/different concentration; known-noise
   control; one attempt counted once across T=2; no double count of duplicate
   produces; missing/unsupported observation held; correlated effects cannot
   silently enter additive mode; frozen G/posterior byte-identical. Historical
   insufficient bindings must remain absent, with sensitivity numbers separate.
2. **Declared switch, default off:** connect a declared parameterized attempt B
   and observation likelihood, consume only validated improve-1b trials; decide
   the entropy accounting explicitly. Enter `−κI` only with all model bindings
   satisfied. Receipt records switch, κ, decomposition and counterfactual κ=0
   choice. Extend fix-7 to isolate novelty removal from risk/ambiguity removal;
   otherwise it can only say `:G`. Test the equal-mean concentration action flip
   plus unchanged decisions with switch off. A malformed model gives typed
   refusal/absence under the declared contract, never a heuristic replacement.
3. **Update and generalize:** calibrated/scope-matched priors, route-sensitive
   learning families, missing/noisy observations, and dependence among effects.
   Prove normalized joint predictive likelihood and posterior consistency;
   assess actual observation coverage before crediting learning opportunities.
4. **Sequential experiments:** only after real multiple-observation/action clocks
   exist, use conditional information increments and belief-conditioned planning.
   Repeated hypothetical firings alone do not create experimental evidence.

## 5. Validation and limits

Own worktree/CLI only. `clojure -M:test -m cognitect.test-runner -n
futon2.report.novelty-discovery-test`: **2 tests, 346 assertions, 0 failures,
0 errors**. Replays frozen numeric G and posterior, all candidate normalization,
zero eligible-effect novelty, shared-prior invariance, and the separating
concentration control. No real data/store writes. `clj-kondo --lint
test/futon2/report/novelty_discovery_test.clj`: errors 0, warnings 0.
`emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
'(arxana-check-parens-cli)' -- --no-defaults
test/futon2/report/novelty_discovery_test.clj`: OK. Java 21.0.11, Clojure 1.11.1,
CLI 1.12.5.1664. Registry evidence is retained beside the numeric results.
Discovery controls run on existing production functions; no claim of a
pre-fix failure or implemented live novelty. The missing model/observation
bindings and factorization assumptions above are substantive limits.
