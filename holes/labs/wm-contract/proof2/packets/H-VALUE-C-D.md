# H-VALUE-C-D — a time-indexed C-schedule with drift interest, tested against the mission's ranking

claude-4, 2026-09-24. PROOF-2 packet, claude-8's requisition (H-value hole,
PROOF-2a item 6). Read-only. Script: `h_value_c_d.clj` (this directory,
babashka, deterministic, no repo writes); its output is §4 verbatim and pins
every input by sha256. Successor to `H-VALUE-D.md` (kimi-7), which found that
no static scalar V per instance reproduces M-futon-seams' own ranking
4 > 5 > 7.

**Verdict up front: the narrower fix fails, and fails in the wrong direction.**
A per-step decay on C makes the mission's order *less* reproducible than the
static scalar it was meant to repair — 4 > 5 > 7 falls from H-VALUE-D's
2.2–21.8% of the swept space to **0.3–7.7%**, and 7-first rises from 26.9–58.0%
to **50.2–66.8%**. The mechanism is §5.1 and it is structural, not a bad rate.

## 1. Where `:c-schedule` is built and consumed today

| role | site |
|---|---|
| built | `futon2/src/futon2/aif/live_c.clj:324` `preference-schedule` — reads `:c-schedule` off the declaration |
| built | `live_c.clj:337` `family-schedule` — one schedule per candidate family, else `:incommensurable-family` |
| built | `live_c.clj:359` `cascade-spec` (`:c-schedule` attached at :429) — onto the live C spec |
| built | `futon2/src/futon2/aif/cascade_problems.clj:160,163` — onto the cascade-problem and its `:cascade-spec` |
| passed | `futon2/src/futon2/aif/efe.clj:1089-1090` — copied into the scoring spec when present |
| **consumed** | `futon2/src/futon2/aif/cascade_model_manifest.clj:688` `preference-member [spec universe horizon tau]` — the only step-indexing site |
| recorded | `cascade_model_manifest.clj:1074,1114` and `cascade_observation_scoring.clj:125` — `:c-form :step-indexed` when a `:c-schedule` or `:c-fn-pointwise` is present, `:constant-spec` otherwise |

**How C is indexed by step today: a two-valued switch, not a schedule.**
`preference-member` computes `uniform? = (and (= :terminal placement) (not= tau horizon))`.
At a step that is not the horizon under `:terminal` placement, the weights map
is `{}` — C is flat. At `tau = horizon`, the full weights apply. Placement
`:every-step` (or absent) applies C at every step. Placement outside
`#{nil :every-step :terminal}` is the typed refusal `:invalid-preference-schedule`.

`preference-schedule` is stricter still: it accepts a declared `:c-schedule`
**only** when it equals
`{:placement {:value :terminal :status :declared} :elsewhere {:value :uniform-over-non-ruled-zero :status :declared}}`,
and throws `:invalid-preference-schedule` otherwise. Absent, it returns the
typed default `{:placement {:value :every-step :status :defaulted :reason :schedule-not-declared}}`.

So there is a slot named for time-indexed preference and **no rate in it**. A
decaying C_t cannot be declared today without amending both the validator in
`live_c.clj` and the placement whitelist in `preference-member`. This packet
models the schedule; it does not implement it, and no lane code is edited.

**Where a graded preference over a token enters the risk term.** It already
does, by a different door: `cascade_model_manifest.clj:588` `utility-weights`
takes an optional `:weights {token w}` map that *replaces the uniform
`lam/|want|` share* for the tokens it names. `log-preference-fn` (:608)
validates it — positive exact rationals, every key in `:want`, else
`:invalid-preference-spec :field :weights` — and produces `ln c(o) = u(o) − ln Z`,
which `outcome-risk-pointwise` (:676) turns into the risk term
`Σ_{q(o)>0} q(o)·(ln q(o) − ln c(o))`. `efe.clj:1082-1086` carries `:weights`
through when the caller derived a live C.

Two limits of that door: weights must be **strictly positive** (a zero-degree
want is expressible only through `:zeroed`, which is at outcome-subset grain,
not token grain), and they are **not step-indexed** — one weight per token for
the whole rollout. Degree is expressible; degree-that-changes-with-time is not.

## 2. The schedule under test

For instance i, want token τ reached at rollout step `step_i(τ)`, outcome o:

**V(i) = Σ_{τ ∈ want(i)} Σ_{o ∈ outcomes(τ)} w_o · d_i(τ,o) · u_o · e^{−ρ·step_i(τ)} · e^{+δ·step_i(τ)}[o = drift] − λ·E[attempts_i]**

The lane minimises G, so the mission's 4 > 5 > 7 is V₄ > V₅ > V₇, i.e.
G₄ < G₅ < G₇.

| parameter | authority |
|---|---|
| `step_i(τ)` | **measured from the cascades**, not transcribed: the script reads `instance-{4,5,6,7}.edn` and computes each want token's rollout level from the `:guard/:needs → :produces` DAG (initial at 0; a pattern fires at 1 + max level of its needs). Same DAG `target_cost.clj` counts patterns over, read for depth instead of count. |
| `outcomes(τ)` | `mission-C.edn` `:served-by` **at token grain** — each entry is (instance, want token, outcome). Finer than H-VALUE-D's instance→outcome set, and it is the grain the graded-preference fix asks for. Instance 8 is `:prospective` with `:want nil`: dropped, not scored. |
| `E[attempts_i]` | measured, `target-cost.edn` θ 0.8: 4 = 8.75, 5 = 10, 6 = 10, 7 = 8.75 |
| `w_o` | mission states none (`:preference :status :unstated`, span [17969 18029]) — swept over the simplex at step 1/10, 1001 points, H-VALUE-D's grid unchanged so the fractions compare |
| `d_i(τ,o)` | text states one degree: 4 "unblocks provider substitution for Rob immediately", unqualified → d = 1. 5 only "retires `matrix-ircd`" (span [18337 18358]) without stating how fully that serves Rob's four-facet outcome → **typed absence, swept** {0.25,0.5,0.75,1.0} on d₅(`:impersonation-retired`, rob). No other degree is stated; those terms carry 1, the absence-of-degree reading, not a claim. |
| `u_o` | text states one deferral: "He is **not** asking for those adapters to be built now" (span [14618 14667]) → u_V swept {0.1,0.25,0.5,0.75,1.0}; all other u = 1 |
| `λ` | no ruling — swept {0,.02,.05,.1,.2,.4}, H-VALUE-D's grid |
| **`ρ` (decay rate)** | **no authority found for a C-schedule decay.** See below. Swept {0, 0.105, 0.2, 0.4, 0.8}. |
| **`δ` (drift interest)** | **no authority found.** No rate is stated anywhere; the mission's drift claim is qualitative ("Two implementations that have already drifted cost more to unify than one implementation plus a stub", span [3227 3327]). Swept on ρ's grid so the two compare. |

**The decay rate's authority, stated as the requisition asks.** No ruling sets
a decay rate for C, and none sets one for the C-schedule slot. What exists is a
*different* per-step rate at a *different* seam: `futon2/src/futon2/aif/rollout.clj:481-485`
`rollout-discount` (option `:temporal-discount`, aliased `:gamma`, default 0.9)
discounts the **rollout accumulator** `S(π) = Σ γ^t g(s_t)` (`project-policy`,
:487-488) — not C. Its provenance is already censused: `C535-U64-rollout-parameter-provenance.md`
finds the *shape* commissioned (`holes/E-policy-rollout-engine.md`) and the
*value* **not found** — 0.9 restates a library default carried into the
production lane from a smoke-test witness with no accompanying argument, and
C535 writes no ruling. So ρ = 0.105 = −ln 0.9 is swept and reported separately
as *the lane's inherited rate*, which is what it is; it is not an authorised
one, and this packet does not make it one.

## 3. What the steps actually are

Computed, not assumed (§4 prints them):

| instance | want tokens → step | mean step | cascade depth |
|---|---|---|---|
| **4** | caller-converted 4, redirect-test 5, prefix-routing-retired 6 | **5.0** | 6 |
| **5** | protocol-declared 3, adapter-conformance-test 4, impersonation-retired 5 | **4.0** | 5 |
| 6 | one-authority 4, flag-retired 5 | 4.5 | 5 |
| **7** | record-schema-declared 2, writers-converted 3, divergence-test 4 | **3.0** | 4 |

The mission's order is 4 > 5 > 7. The mean step at which each reaches its wants
is 5.0 > 4.0 > 3.0 — **monotone in exactly the reverse**. Any decay in step
therefore penalises the instances in the mission's own order of preference.
This is the whole result, and §5.1 says why it is not a fixable rate.

Token grain also sharpens the outcome map: 7 serves drift through **two**
tokens (`:writers-converted`, `:divergence-test`) and serves both second-impl
and vs-code through **one** shared token (`:record-schema-declared`); 6 serves
both drift and coupling through `:one-authority`. H-VALUE-D's instance→outcome
sets could not see either.

## 4. Computed results

`bb h_value_c_d.clj`, verbatim. Cells are (share 4>5>7, share 7-first) over the
sweep; `:points` is the number of (w, u_V, d₅ᴿ, ρ, δ) points in that reading.

```
;; inputs (sha256):
;;   78eb7ed0c18398b00540799ed388ed3495e28c8501660de2eccd986437906a68  instance-4.edn
;;   e1c7edb6a8c57eb2bf73fda9bb2a7c45ccc191445f583204e858af66bcaa6667  instance-5.edn
;;   4eb6d91999fe304c6924038f46a6b380e05cd30ee0972c55f667224b17836439  instance-6.edn
;;   8152622d36aa589c391da0bdde4aa9a0fcb836b5f5e63a9aba01e639f034de78  instance-7.edn
;;   cff8c325598c1fb965af03370393a442f4385f29859203968e5a2fc7fc7362c3  mission-C.edn
;;   7d77ef0299ede3427035ee88c3bbf4f7b4fadca64954214540380b3769d64e3d  target-cost.edn
```

| λ | no timing (ρ=δ=0) | decay only (ρ>0, δ=0) | drift only (ρ=0, δ>0) | both > 0 | full sweep | text reading | lane rate ρ=0.105 |
|---|---|---|---|---|---|---|---|
| 0 | (0.1996, 0.2970) | (0.0646, **0.7020**) | (0.1535, 0.1425) | (0.0535, 0.5545) | (**0.0771**, 0.5019) | (0.0846, 0.4669) | (0.1200, 0.2997) |
| 0.02 | (**0.2044**, 0.3283) | (0.0540, 0.7354) | (0.1539, 0.1524) | (0.0449, 0.5905) | (0.0701, 0.5331) | (0.0612, 0.4996) | (0.1046, 0.3191) |
| 0.05 | (0.1704, 0.3480) | (0.0399, 0.7614) | (0.1318, 0.1678) | (0.0321, 0.6150) | (0.0548, 0.5562) | (0.0374, 0.5250) | (0.0824, 0.3454) |
| 0.1 | (0.1247, 0.3856) | (0.0223, 0.7871) | (0.0933, 0.1932) | (0.0173, 0.6449) | (0.0346, 0.5850) | (0.0168, 0.5552) | (0.0515, 0.3857) |
| 0.2 | (0.0655, 0.4336) | (0.0061, 0.8113) | (0.0501, 0.2458) | (0.0050, 0.6825) | (0.0148, 0.6233) | (0.0034, 0.5957) | (0.0183, 0.4492) |
| 0.4 | (0.0136, 0.4921) | (0.0002, 0.8239) | (0.0121, 0.3340) | (0.0002, 0.7241) | (0.0026, 0.6684) | (0.0000, 0.6446) | (0.0008, 0.5179) |

**Headline, side by side with H-VALUE-D.**

| | 4 > 5 > 7 holds on | 7 leads on |
|---|---|---|
| H-VALUE-D (static scalar V per instance) | **2.2% – 21.8%** | **26.9% – 58.0%** |
| H-VALUE-C-D (time-indexed schedule, full sweep) | **0.3% – 7.7%** | **50.2% – 66.8%** |
| H-VALUE-C-D, decay only | 0.02% – 6.5% | 70.2% – 82.4% |
| H-VALUE-C-D, token grain only (ρ = δ = 0) | 1.4% – 20.4% | 29.7% – 49.2% |

## 5. Reading, against the falsifier

The falsifier was: *if the fractions do not move materially from H-VALUE-D's,
say so; a fitted schedule that reproduces the order only in a sliver is a
negative result.* They moved materially — **downward**. This is a negative
result of a stronger kind than the falsifier anticipated: not a schedule that
fails to help, a schedule that hurts.

**5.1 The decay's index is the wrong clock, and on this mission it is
anti-correlated with the mission's reasons.** The lane's `:c-schedule` indexes
**rollout steps inside one flight**; every timing claim in the mission is about
**calendar time across flights** — "unblocks … for Rob *immediately*", "do
*before* a VS Code implementation exists, not after", "the cheap moment has
passed". A decay in rollout step does not measure when an outcome lands in
calendar time. It measures **how deep in its own cascade a token sits**, which
is cascade length by another name. And 4's cascade is the *longest* of the
three (6 steps) precisely because retiring prefix routing needs
enumerate → grain → bind → single-producer → convert → test → retire, while 7's
is the shortest (4). So decay rewards exactly the instance the mission ranks
last: 7-first goes from 29.7% to **70.2%** at λ = 0. The rate is not the
problem — every positive ρ moves it the same way, and the lane's own inherited
0.105 is the mildest point of a uniformly wrong direction.

**5.2 Drift interest compounds in the wrong index too, and helps the unranked
instance.** "Compounding from today" is a calendar claim: the copy-paste that
is drifting exists now, and the cost of unifying grows while it sits. Indexed
on rollout step, `e^{+δ·t}` instead rewards an instance whose drift tokens sit
*deep in its own cascade*, which has nothing to do with how long the drift has
been accruing. It does move 7-first down hard (29.7% → 14.3% at λ = 0), but the
leader it installs instead is measured, not guessed: over the drift-only cells
at λ = 0 the first-place counts are **6: 47,440 · 4: 19,457 · 7: 11,892 ·
5: 1,291**. Instance **6** takes first place on 59% of that space — the
instance the mission explicitly does not rank ("Instance 6 is the warning") —
because its drift tokens sit at steps 4 and 5, the latest of any. Meanwhile
4 > 5 > 7 barely moves (0.1996 → 0.1535). A term that suppresses the symptom
by promoting the unranked instance has not found the mission's reason.

**5.3 A deadline is still not a rate.** H-VALUE-D §5.1 named this and it
survives the repair: "do before a VS Code implementation exists, not after"
(span [18462 18514]) is a step function in calendar time whose knee is an
**exogenous event**, not any `e^{−ρt}`. No (ρ, δ) pair in any sweep encodes it,
because the schedule's domain contains no event the mission can name.

**5.4 Graded token preference is the one thing that helped, and it is small.**
At ρ = δ = 0 the token-grain decomposition alone lifts 4 > 5 > 7 from
H-VALUE-D's 16.6% to **20.0%** at λ = 0 (20.4% at λ = 0.02, its best cell
anywhere in this packet), because the token grain finally sees that 7 buys its
drift service twice through two tokens and its second-impl and vs-code service
once through one shared token. This is the half of claude-10's fix that is both
already implementable (`:weights`, §1) and directionally right. It is also not
enough: 20.4% is still a minority, and it is inside H-VALUE-D's own range.

**5.5 What the schedule cannot express, in one line each.**
1. **Calendar time.** There is no index for it. `:c-schedule` has rollout steps and a horizon; the mission has dates, an interview, and a VS Code implementation that does not exist yet.
2. **An exogenous deadline.** No event vocabulary, so no knee.
3. **Drift accrual since a start date.** `δ` compounds from step 0 of a rollout, not from when the fork happened.
4. **Degree that varies with time.** `:weights` is one positive rational per token for the whole rollout (§1); it cannot fall as an outcome is partially served, and it cannot be zero.
5. **Cross-instance path dependence.** V is still additive over tokens and independent across instances, so "doing 4 first makes 7 cheaper" is inexpressible — H-VALUE-D §5.2 unchanged.

## 6. Proposed amendment (the definition that is missing)

The requisition's rule is to settle by definitions and records or say the
definition is missing. It is missing, and it is not the decay rate.

**Missing definition: a calendar index for C, distinct from rollout step.**
`:c-schedule` today conflates two clocks under one name. What the mission's
reasons need is C indexed by *when the outcome lands in calendar time*, which
in the lane is a property of the **flight** (when it is run) and not of the
**rollout** (how deep the token sits). The two are independent, and on
M-futon-seams they are anti-correlated. Proposed shape, for the owners of
`live_c.clj` and `preference-member` rather than for this packet to build:

- `:c-schedule` keeps `:placement` as the rollout-step switch it is, and its
  validator stops being the single-value equality check it is today (§1) so a
  schedule can carry more than one admissible form;
- a **separate** declared field carries calendar deferral per outcome, with its
  own typed absence — the vs-code outcome's "not now" is a value about the
  outcome, stated once by the mission, not a property of any cascade that
  serves it;
- deadlines are declared as **predicates over named events**, not rates, so
  "before a VS Code implementation exists" is checkable rather than fitted;
- the decay rate, if one is ever declared for C, cites an authority. Today none
  exists: the only per-step rate in the lane is `rollout-discount`'s γ, whose
  value C535 records as **not found**. Reusing it for C would inherit an
  uncommissioned number into a second seam.

## 7. What a second mission must state for the hold-out test

H-value's test is "reproduces 4 > 5 > 7 on M-futon-seams **and** holds on a
second mission". The first conjunct fails here, so the hold-out is not yet
reachable; but the second mission's requirements are now decidable, and they
are what this packet found missing:

1. **Outcomes with relative weight**, or an explicit statement that they are unweighted — so a sweep is a finding rather than a gap. M-futon-seams states none, which is why every fraction in this packet is a share of a simplex.
2. **Per-token degree of attainment** — "this token serves that outcome fully / partly, and here is the evidence". One degree is stated in M-futon-seams (`4`'s "unblocks … immediately"); one is conspicuously absent (`5`'s "retires matrix-ircd"); the rest are silent.
3. **Deadlines as calendar predicates naming their event**, not as urgency adjectives, so §5.3's knee has somewhere to live.
4. **The date drift began accruing, per outcome**, if drift interest is to compound "from today" — otherwise δ has no origin and compounds from an arbitrary zero.
5. **The author's ranking recorded before any model is fitted**, as M-futon-seams' IDENTIFY exit was, so the test stays a test.

A second mission stating (1)–(4) would also settle whether the failure here is
M-futon-seams-specific. The anti-correlation in §3 — mission order monotone in
reverse of cascade depth — is one mission's accident until a second is checked;
the *conflation of two clocks* in §6 is not, and does not need a second mission
to be true.
