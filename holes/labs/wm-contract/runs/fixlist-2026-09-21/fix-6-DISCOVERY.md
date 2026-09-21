# Fix-6 discovery: the candidate differences are small terminal utilities

Codex-11, 2026-09-21. Discovery only; no production change.
Branch `fix/narrative-6`, based on `main` at `4354668b` (the requested
`master` ref does not exist in futon2). Worktree: `/home/joe/code/futon2-fix-6`.

All **51 recorded G values reproduce exactly** in a separate Clojure process,
through both `efe/rank-cascade-actions` and
`cascade-model-manifest/horizon-g-sparse`. The largest recorded/recomputed
absolute difference is **0.0**. An independent token-sum decomposition agrees
within 1e-6. This is replay of frozen inputs, not a fresh scan or WM click.

The small spread comes from globally normalized preference weights, followed
by division among each mission's wants, and a menu that can satisfy very few
new tokens. It is **not** numerical loss caused by a large offset. A common
offset cancels in posterior odds. In the latest run, the alleged 463 unreachable
wants are not in the scoring universe at all: it has **11 tokens**. In the
earlier runs, **116 scored wanted tokens remain absent under every candidate**;
those do account for most of the much larger offset.

## Evidence and replay boundaries

Inputs are the three `data/wm-runs/tick-run-record-2026-09-21-<suffix>.edn`
files in the canonical futon2 checkout, read without alteration:

| Suffix | SHA256 |
|---|---|
| 1789964661 | `8264021ac5f5bc6c251720917c69375795fcc9e1a4a5f263694b6efe51142686` |
| 1789951020 | `5c68dee93b36bde383ab4ecca5ec96856114361534a02ffdedd800fc06f09501` |
| 1789952479 | `031ec7431af2939bdfa15ca941e28cc23e6ec3b2264d99dc8b65b00c82b2e298` |

[fix_6_replay.clj](fix_6_replay.clj) reads candidate identities/precedences from
`:decision :selection-certificate :candidates`, and the actual consumed A, C,
D and Q from `:decision :g-term-decomposition :policies`. It reconstructs the
extensional preference spec from the recorded terminal log weights and
schedule, asserting equality with **every recorded C member**. This avoids
rederiving preferences from today's mission files. It asserts common A/C/D,
identity rates, point-mass beliefs, and empty zero exclusions; recomputes
trajectories from D and each precedence; and compares them with recorded Q.
It does not feed recorded future Q into the scorer.

[fix-6-results.edn](fix-6-results.edn) contains all candidate rows, all token
contributions, exact rational weights, per-step risk/ambiguity, source
projection, counterfactual scores and posterior masses. Candidate identity is
the full action, not bare `:C1`/`:C0`, which repeat across missions.

One correction to the census interpretation: **C is common across candidates
and varies across horizon steps; Q differs across candidates**. “Open-loop”
does not mean identical Q. All three runs have T=2, identity A, point-mass D,
uniform C at step 1 and weighted C only at step 2. Ambiguity is identically
zero. F consumed by selection is zero; for non-finite identity-A F this is
`:computed-not-attached`, not evidence that computed F was zero. E is uniform
in 1789964661, but is nonuniform in the two earlier runs.

## Exact decomposition

For token universe V, terminal weights w, and deterministic predicted state
S_pi(t), with no zeroed outcomes:

```
C_t(S) = exp(sum_{v in S} w_t(v)) / Z_t
log Z_t = sum_{v in V} log(1 + exp(w_t(v)))
risk_pi(t) = log Z_t - sum_{v in S_pi(t)} w_t(v)
ambiguity_pi(t) = 0
w_1 = 0; w_2 = w
G_pi = B - sum_{v in S_pi(2) \ S0} w(v)
B = |V| log 2 + log Z_2 - sum_{v in S0} w(v)
```

The last expression uses the verified monotone union transitions of these
records. B is the score of holding the initial state, computed whether or not
such a candidate is admitted. Every token contributes `log 2` at step 1 and
`softplus(w) - 1_present*w` at step 2. The results file lists the latter for
every candidate/token, so even tokens with no differential effect are visible.

### 1789964661: three candidates

`|V|=11`; 6 tokens have positive terminal weight. B = **15.171375235022659**,
split as **7.6246189861593985 + 7.546756248863261**.

| Target (all have local id C1) | Step-1 risk | Step-2 risk | G | G minus B |
|---|---:|---:|---:|---:|
| M-aif-policy-conditioned-eig | 7.6246189861593985 | 7.5454502670101355 | 15.170069253169533 | -0.00130598185312556 |
| M-f11-find-production-successor | 7.6246189861593985 | 7.546713476531827 | 15.171332462691225 | -0.0000427723314333864 |
| M-wm-08-external-f2 | 7.6246189861593985 | 7.546756248863261 | 15.171375235022659 | 0 |

Both ambiguity terms are zero for every row. Exactly two tokens distinguish
the candidates:

| Target-qualified token | Exact weight | Who newly satisfies it |
|---|---:|---|
| `["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]` | 229/175347 | AIF candidate, at steps 1 and 2 |
| `["M-f11-find-production-successor" :hole/h9ab212b3281d]` | 5/116898 | F11 candidate, at steps 1 and 2 |

Their terminal risk contributions, in table candidate order, are respectively
`[0.6924944028319425, 0.693800384685068, 0.693800384685068]` and
`[0.6931685669543465, 0.6931257946229122, 0.6931685669543465]`.
Thus the **winner/runner-up gap is 443/350694 = 0.00126320952169217**,
and the full spread is **229/175347 = 0.00130598185312556**.

The weights can be followed to source: AIF's alive mass `224/58449` and
closed mass `5/58449` are each divided among its three declared wants;
F11 has closed mass `5/58449`, divided among two wants. These are outputs of
`live-c/normalise-weights` and `project-want`, not transition probabilities.

The external-F2 route completion token has weight **1/6**, much larger than
either distinguishing weight, but is **already present in D**, together with
its prerequisite tokens. Its candidate adds no new state. This weight comes
from `merge-live-cascade-spec` retaining the joint want and
`cascade-model-manifest/utility-weights` giving unnamed wants `lam/|want|`;
it is absent from `:weights-echo`. Looking only at the five echoed live weights
would miss it and fail to reproduce G.

The neutral-universe term is `2*11*log(2) = 15.249237972318797`; preferences
and already-present wants shift the holding baseline by -0.077862737296138.
The nine tokens fixed across all candidates contribute 12.398111922263352.
Only **three** fixed-absent wanted tokens contribute 4.160210878004318
(about 27% of G), so unreachable scored wants do **not** dominate this run.
The provenance counts 468 live entries, five projected outcome tokens and
465 unreached source tokens. These are different units: one source can project
to multiple outcomes and alive/closed sources can share outcomes; subtracting
5 from 468 is not a valid count of excluded sources.

### 1789951020 and 1789952479: the same 24 G values

Both have `|V|=123`, 120 positive terminal weights, 113 projected live outcome
tokens and 442 unreached live source tokens. B = **170.56199895748495**:
step 1 = **85.25710320887337**, step 2 = **85.30489574861159**.

| Candidate group | Count | Step-2 risk | G | G minus B |
|---|---:|---:|---:|---:|
| M-expressions-of-interest C1 and C2 | 2 | 85.30176481395061 | 170.558868022824 | -61/19483 |
| Empty-precedence C0 candidates, one per target | 21 | 85.30489574861159 | 170.56199895748495 | 0 |
| M-wm-08-external-f2 C1 | 1 | 85.30489574861159 | 170.56199895748495 | 0 |

Every row has the same step-1 risk and zero ambiguity at both steps. The full
spread is **0.0031309346609589284**, slightly greater than 0.003.

The three differentiating tokens all belong to M-expressions-of-interest and
all have weight **61/38966 = 0.00156546733049325**:

| Token | C1 at step 1 / 2 | C2 at step 1 / 2 | Other 22 at step 2 |
|---|---|---|---|
| :change-authored-and-bound | present / present | absent / absent | absent |
| :premise-refused-before-work | absent / present | present / present | absent |
| :obligation-resolved-through-the-account | absent / absent | absent / present | absent |

Each EOI candidate satisfies two equal-weight tokens at the only weighted
step. Their terminal states are different, but **C assigns the same utility**.
The terminal risk of each such token is 0.6923647532306627 when present and
0.6939302205611559 when absent. This accounts for the entire spread.

`2*123*log(2) = 170.51420641774655`: the large magnitude is predominantly
the common observation-universe normalization, with a +0.0477925397384
preference correction to B. The 120 tokens fixed across candidates contribute
166.40076675412175. Of these, **116 wanted tokens are absent under all
candidates**, contributing **160.85974729577842** (about 94% of G). These are
represented wants that this menu cannot newly satisfy, distinct from live
source wants excluded before scoring. Removing fixed dimensions under a
proper shared marginal would remove a constant, not improve the spread.

Habit decides C1 versus C2 because their Gs are exactly equal. In 1789951020
their E ratio is 4:1 and posteriors are 0.14322491827350511 versus
0.03580622956837615; in 1789952479 it is 5:1, with E=1/6 versus 1/30.
Even against an ordinary C0, the G-only odds factor is merely
`exp(61/19483) ≈ 1.003136`, while habit ratios are order-one. A numerical
guard can detect unequal doubles here without demonstrating a meaningful
G-based choice.

## Proposal for review: calibrate the declared preference scale

Preserve the common universe, horizon, guards, D, A, zero exclusions and
relative token weights. Declare a **single family preference scale k**, and
apply `w'_v = k*w_v` to all terminal log weights (including default weights),
recomputing normalized C. This is a C-model change, not multiplying a reported
G or subtracting a baseline while continuing to call it G. The existing
`lam` declaration is the intended place to express the scale; the replay
operates on the consumed weights to make its counterfactual unambiguous.

The measured sensitivity points k=100 and 1000 are **not learned values** and
are not proposed as an unattended default. The follow-on task should obtain
and record an independently justified preference scale (e.g. operator-declared
outcome odds), test its whole-family effect, and expose the habit/G tradeoff.
Choosing k merely to force a desired winner would have no evidential warrant.

| Scale | Latest Gs (AIF / F11 / F2) | Latest spread | Earlier EOI / other G | Earlier spread |
|---:|---|---:|---|---:|
| 1 | 15.170069253170 / 15.171332462691 / 15.171375235023 | 0.001305981853 | 170.558868022824 / 170.561998957485 | 0.003130934661 |
| 100 | 14.632063166064 / 14.758384118234 / 14.762661351377 | 0.130598185313 | 175.680288877430 / 175.993382343529 | 0.313093466099 |
| 1000 | 15.851033694383 / 17.114243216074 / 17.157015547509 | 1.305981853126 | 247.564135307968 / 250.695069968955 | 3.130934660986 |

At k=1000 the latest posterior, retaining E and beta=1, is approximately
**[0.643645, 0.181987, 0.174368]**. EOI C1/C2 still tie in G at every scale;
no rescaling can distinguish equal-utility states. The recorded different
orders need an independently justified order-sensitive completion preference
or outcome model if their distinction matters. That is additional modeling,
not supplied by this proposal.

Costs and limits relative to the spec:

- `DarkTower/WarMachine/OutcomeRiskKL.lean` defines normalized outcome KL,
  with positive Q on zero C giving infinity. Renormalizing positive C after
  rescaling its utility preserves that definition and all support conditions.
  Omitting log Z would not preserve the reported KL.
- `PolicyHorizon.lean` defines `stepRisk`, `stepAmbiguity`, `stepTerm` and
  `horizonEFE` for a common model and step-indexed C. The proposal changes C,
  which is an explicit input, and keeps the horizon sum. It loses fidelity
  to the **current declared lam=1 preference**, not to the KL/horizon law.
- `CascadeEFE.lean` supplies guarded policy prediction, joint prediction,
  `risk`, `ambiguity` and `stepG`. It explicitly calls itself a mathematical
  model, not a correspondence claim about the live machine. No theorem in
  these files establishes an empirical k, promises discrimination, or makes
  these deterministic interpreted transitions true of implementation outcomes.
  These are source correspondences, not runtime Lean execution. Lean sources
  inspected at mathlib4 `77fdbda5b5629b3c8f6c7f9bbb027da0436ba1b3`; no Lake run.
- Global normalization over the live source inventory still makes an absolute
  task weight depend on other sources. A fixed calibrated scale does not cure
  that modeling choice, identity A, unattached non-finite F, or weak candidates.
  Nor does a sharper posterior establish that the favored task is more useful.

Two tempting changes are insufficient: subtracting common terms leaves odds
unchanged; merely extending T with the **recorded terminal schedule** leaves
the latest spread unchanged (T=3 and T=10 both 0.00130598185312). The earlier
EOI cascades reach their third token at T=3, raising the spread only to
0.00469640199145; T=10 adds no further separation. Switching to every-step C
would be a different declared objective. A non-identity A needs measured
observation error; adding noise to make numbers differ is not such evidence.

## Reproduction and validation

From `/home/joe/code/futon2-fix-6`, with OpenJDK 21.0.11 and the repository's
Clojure 1.11.1 dependencies:

```sh
clojure -M holes/labs/wm-contract/runs/fixlist-2026-09-21/fix_6_replay.clj > holes/labs/wm-contract/runs/fixlist-2026-09-21/fix-6-results.edn
clj-kondo --lint holes/labs/wm-contract/runs/fixlist-2026-09-21/fix_6_replay.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- holes/labs/wm-contract/runs/fixlist-2026-09-21/fix_6_replay.clj
```

The replay exits 0 and ends with this validation field:

```
All 51 candidates: recorded, rank-cascade-actions, sparse scalar, certificate and token decomposition agree within 1e-6; predicted states match recorded states.
```

Static validation: `linting ... errors: 0, warnings: 0`; paren check: `OK`.
No production code or test namespace changed, so no failing-on-main regression
test is claimed: this is the commissioned discovery, not a fix. The registry
exists but cannot warrant this script command: `test_registry.clj`'s
`validate-command!` accepts only a namespace-bound test command or one Lean
module build, and `successful?` requires parsed nonzero test counts. The exact
process command, frozen input hashes, assertions and fresh output above are
the evidence; no registry rule was weakened to accept a discovery script.
