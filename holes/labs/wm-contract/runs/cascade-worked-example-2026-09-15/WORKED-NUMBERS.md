# Worked cascade computations — draft for Joe

Synthetic pedagogical example, agreed with zai-7 (coordination.json). No blog publication is authorized by this artifact. This section is for the wip draft. The model is deliberately smaller than Snatch: its transition and observation tables can be fully specified without inventing distributions for the Snatch record. These are executable floating-point calculations, not a Lean proof or measured War Machine outcomes.

## The board and its model

State is `(x, completed)`, initially `(0, empty)`. Every pattern has a guard requiring that it has not already fired. SET writes x=1; FLIP writes x=1-x. SEAL requires both SET and FLIP completed; PUBLISH requires SEAL. ARCHIVE requires PUBLISH, and VERIFY requires ARCHIVE. The last four preserve x but change their own completion bits. SET and FLIP initially have **overlapping enabled guards and shared write scope**; they are not independent or disjoint-scope patterns. The other prerequisite edges are identical for all candidates.

At each step, apply the first enabled pattern in precedence, then observe. No enabled pattern means identity, still followed by an observation. All guards and completion bits are explicit in compute.py. These model assumptions ensure each pattern fires once; they are not general laws for every cascade.

| Policy | Precedence | First four observed x values |
|---|---|---|
| bad4 | SET, FLIP, SEAL, PUBLISH | 1, 0, 0, 0 |
| good4 | FLIP, SET, SEAL, PUBLISH | 1, 1, 1, 1 |
| good6 | FLIP, SET, SEAL, PUBLISH, ARCHIVE, VERIFY | 1, 1, 1, 1 |

Both four-pattern candidates have exactly the same nodes and descent edges, but different legal precedence. “Good” and “bad” describe alignment in this example, not a general judgement about a pattern. FLIP is beneficial first and harmful after SET: its quality cannot be read from an independent per-pattern scalar.

The book observation is `(x, coin)`, where coin is independently fair at every observation. Thus A((x,0)|s)=A((x,1)|s)=1/2, with zero mass on the other x. Preferences are C(x=1)=0.99 and an independent fair coin; in observation order [(0,0),(0,1),(1,0),(1,1)], C=[0.005,0.005,0.495,0.495]. q0 is a point mass. Every reachable q remains a point mass. Therefore I=0 here: this example illustrates composition and irreducible ambiguity, not active information seeking.

## Equations and the two horns

Prediction and J/Q follow spec §2: q_next(s′)=sum_s q(s)B_pi(s′|s), J(s,o)=q(s)A(o|s), Q(o)=sum_s J(s,o). Book B uses G_tau=KL(Q||C)+sum_s q(s)H(A(.|s)), and G=sum_tau G_tau (book 4.9/4.10; spec §3.3–4). The code independently computes L and I and checks G=L-I at every finite-scoring step.

**Horn A is underdefined as a unique formula.** For illustration only, A here scores the task outcome x, pushing both A and C onto x and excluding the known irrelevant coin. The resulting likelihood is deterministic, ambiguity is zero, and G_A is task-alignment KL. This is a changed observation model/objective, not the book G on the original alphabet and not an adopted full-A implementation. Equivalently in this particular factorised example, G_A=G_B-T ln2. This does not justify subtracting entropy in arbitrary models. It neither measures attestation quality nor proves that perfectly specified stochastic patterns have zero book ambiguity.

| Reachable x | Risk, both horns | Ambiguity A illustration | Ambiguity B | G_B |
|---|---:|---:|---:|---:|
| 1 | 0.010050335854 | 0.000000000000 | 0.693147180560 | 0.703197516413 |
| 0 | 4.605170185988 | 0.000000000000 | 0.693147180560 | 5.298317366548 |

The risks are -ln(0.99) and -ln(0.01). Ambiguity B is ln2=0.693147180560. All quantities are derived from the retained joint and preference distributions; there is no precedence-indicator score.

## Same horizon: composition and selection

For selection we predeclare T=6 for every candidate. Completion remains observed with its actual likelihood; no free zero-cost padding is invented. This application-step schedule is a toy convention, not a proof of equal physical work or time in the War Machine. In particular the model does not encode an extra preference reward for ARCHIVE/VERIFY.

| Policy | Fired applications | G_A illustration | G_B book |
|---|---:|---:|---:|
| bad4 | 4 | 23.035901265794 | 27.194784349154 |
| good4 | 4 | 0.060302015121 | 4.219185098481 |
| good6 | 6 | 0.060302015121 | 4.219185098481 |

Composition control (§3.6): bad4−good4 = 5 ln(99) = 22.975599250673 under either horn at T=6. At T=4 it is 3 ln(99). This difference arises from the changed predicted states after the same two interacting operators. No interaction term has been discarded.

For the illustrative choice distribution set E_i=1, gamma=1 and F_i=0. Without observed-data F this is a policy prior, not an inferred posterior about live observations. Spec §3.5 gives softmax(ln E−gamma G). The **pending, unadopted** fuel proposal used here is softmax(ln E−gamma G−lambda expected-fired). lambda=0.25 is an arbitrary displayed exchange rate, not a ruling. Idle observations cost no fired-application fuel.

| lambda | P(bad4) | P(good4) | P(good6) |
|---|---:|---:|---:|
| 0 | 5.25767856379e-11 | 0.499999999974 | 0.499999999974 |
| 0.25 | 6.5453821649e-11 | 0.622459331161 | 0.377540668773 |

The table is the same for B to floating-point tolerance: adding common 6 ln2 cancels in softmax. Fuel shifts weight toward good4 because this model gives both good candidates identical task outcomes. This is a visible design consequence, not a recommendation. These numbers do not compare unequal-horizon raw G sums.

## Length diagnostic, not a selection table

| Policy | Own application horizon | G_A illustration | G_B book |
|---|---:|---:|---:|
| bad4 | 4 | 13.825560893818 | 16.598149616058 |
| good4 | 4 | 0.040201343414 | 2.812790065654 |
| good6 | 6 | 0.060302015121 | 4.219185098481 |

The six-pattern good language scores only 0.060302 under the A illustration with 99% alignment. This is small, not length-invariant: positive epsilon still accumulates. A separate exact-alignment control changes preferences to C(x=1)=1, keeping the coin fair. Then both good languages have A=0; book B is 4 ln2=2.772588722240 or 6 ln2=4.158883083360 at their respective own horizons. Bad4 reaches x=0 and refuses a finite score because C=0 there (riskAdmissible); no smoothing is performed. These exact-preference results are a separate experiment and are not mixed into the .99 selection table.

## What the one-action projection misses

proxy.clj executes the repository’s existing Gaussian risk and ambiguity helpers on an explicitly synthetic, one-channel first-step projection Q=N(1,0.01), C=the normalized [0.9,1] range at temperature 0.1. Both SET-first and FLIP-first produce x=1 from x=0, so this projection is identical for all three candidates. It has no later cascade prediction.

Computed old-component risk = 0.133925052246; Gaussian differential ambiguity = -0.883646559789; sum = -0.749721507543, for all candidates.

This is the action-grain risk-plus-ambiguity component, **not a replay of the entire historical controller score**: engineering bonuses, additional channels and their authorities are not computed or asserted zero. These are current pinned implementations of the retained old-regime components, not measured historical tick values. A Gaussian differential entropy can legitimately be negative. Its sign is not evidence of cheating; it is also not numerically interchangeable with the discrete entropy above. The demonstrated defect is that identical one-step projections conceal the later composition difference.

## Reproduction and evidence

From futon2:

```sh
clojure -M holes/labs/wm-contract/runs/cascade-worked-example-2026-09-15/proxy.clj > holes/labs/wm-contract/runs/cascade-worked-example-2026-09-15/proxy.json
python3 holes/labs/wm-contract/runs/cascade-worked-example-2026-09-15/compute.py
```

results.json retains q, J, Q, C, L, I, both risks/ambiguities, transitions and totals for every step. compute.py checks normalization, decomposition, composition inequality, exact-alignment zero, zero-preference refusal and softmax normalization/cancellation. Natural logarithms; Python binary64 and JVM doubles; these tolerance checks are not formal proofs.

Source references (sha256 pins also in results.json):
- `holes/labs/wm-contract/SPEC-cascade-policy-semantics-2026-09-15.md:24-31,35-55,65-79` — `047a857c26b50679fb489f0ba5cc09a517ff1f1c1cccc89c8835d7697cb62fb0`
- `src/futon2/aif/efe.clj:40-64,475-500,631-648,687-703` — `1dd3b83d0c78a8388889d35c32a6bde12cd9ee55eed5ce8c0b570f3005fd1b83`
- `src/futon2/aif/preferences.clj:235-264,506-565` — `f1a0b5c0a05a3d20bc09a03b03de0af9798cad9f0626bc78b83f140c78417023`

No spec, Lean definition, runtime objective, admission record or blog page was changed. Publication remains pending Joe’s inspection of the assembled wip draft.
