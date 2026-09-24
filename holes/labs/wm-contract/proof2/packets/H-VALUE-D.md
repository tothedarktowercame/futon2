# H-VALUE-D — a value term with timing and degree, tested against the mission's own ranking

kimi-7, 2026-09-24. PROOF-2 packet, claude-8's requisition (H-value hole,
PROOF-2a item 6). Read-only. Script: `h_value_d.py` (this directory),
deterministic, stdlib only; its output is §4. Inputs: `futon3c/holes/labs/
M-futon-seams/item6/{NOTES.md,mission-C.edn,target_value.py,target-cost.edn}`
and the mission text `futon3c/holes/missions/M-futon-seams.md` at HEAD
(sha matches mission-C's `:mission-sha d13c5cfe…`, verified by claude-10's
`scripts/mission_c_check.py` per item6/NOTES.md).

## 1. The proposed term

**V(i) = Σ_{o ∈ served(i)} w_o · d_i(o) · u_o − λ · E[attempts_i]**

| parameter | meaning | authority |
|---|---|---|
| served(i) | outcomes instance i's wants serve | mission-C.edn `:served-by` (owner-authored, binary links) |
| E[attempts_i] | expected attempts to completion at θ 0.8 | measured: target-cost.edn — 4: 8.75, 5: 10, 6: 10, 7: 8.75 |
| w_o ≥ 0, Σw = 1 | outcome weights | mission states none (`:preference :unstated`, span [17969 18029] "Pick one instance and declare its interface — not all eight.") — **declared-with-no-ruling, swept** over the simplex, step 1/10 (1001 points, claude-10's grid) |
| d_i(o) ∈ [0,1] | degree of attainment of o by i, with evidence | stated for one link (below), unstated elsewhere — unstated is a **typed absence, swept**, never a guessed constant |
| u_o ∈ (0,1] | timing factor on outcome o (deferred value) | stated for one outcome (below), u = 1 elsewhere: "immediately" (4's rob service) means no delay; elsewhere u = 1 is the absence-of-timing reading |
| λ ≥ 0 | cost weight vs outcome value | no ruling exists — swept {0, .02, .05, .1, .2, .4}, claude-10's grid |

No invented constant is presented as design: every number that decides the
ranking is either measured (E[attempts]) or swept with its unstated status
named.

## 2. Degree and timing FROM THE MISSION TEXT, with spans

What the text actually states (offsets into the mission at HEAD):

| instance | claim | kind | span |
|---|---|---|---|
| 4 | "unblocks provider substitution for Rob immediately" | timing: immediate (no delay); degree: unqualified "unblocks" → d_4(rob) = 1 is the text's reading | [18227 18277] |
| 4 | "smallest surface, 51 literals plus three sites that route on the provider" | cost (counted, not E[attempts]) | [18061 18160] |
| 5 | "larger, but it retires `matrix-ircd` and the \"don't start an IRC server\" flag together" | degree: retires one named shim process; **how fully that serves rob-can-run-the-stack (whose outcome statement spans provider, shell, transport, editor) is NOT stated → typed absence, swept as d_5(rob)** | [18337 18358] |
| 7 | "do before a VS Code implementation exists, not after" | timing: a **deadline** on doing 7 — see §5.1 | [18462 18514] |
| 7 | "The cheap moment has passed; the cheapest *remaining* moment is before the VS Code version exists." | timing: cost rising with calendar time | [16247 16340] |
| vs-code outcome | "He is **not** asking for those adapters to be built now" | timing: outcome value **deferred** → u_V < 1, swept | [14618 14667] |
| 6 | "Instance 6 is the warning" (under "**Do not** mint the abstraction…") | **6 carries no rank. The mission's order is over {4, 5, 7}; 6's position is a typed absence** — 4>5>7 is counted irrespective of where 6 lands | [18901 18926] |
| (drift) | "Two implementations that have already drifted cost more to unify than one implementation plus a stub" and the copy-paste that "now evolve[s] separately" ([14822]) | drift cost is already accruing — interacts with *other* instances' delay; see §5.2 | [3227 3327] |

No other degree or timing is stated. In particular the text gives no
numeric degree anywhere; every d other than d_4(rob) = 1 is swept.

## 3. Readings of the unstated parts

- **baseline** (claude-10's): all d = 1, all u = 1 — binary served-by, no timing.
- **degree reading**: d_5(rob) ≤ 0.5 — "5 retires one shim" read as partial
  service of Rob's four-facet outcome against 4's unqualified "unblocks".
- **timing reading**: u_V ≤ 0.5 — the vs-code outcome's value deferred,
  per "not asking for those adapters to be built now".
- **text reading**: both. (u_V, d_5(rob) each also swept over
  {0.1,0.25,0.5,0.75,1.0} / {0.25,0.5,0.75,1.0} so the conditioned
  fractions are not point guesses.)

Analytic region for 4 > 5 > 7 (from served(4) ⊂ served(5) and the costs):

- 4 > 5 ⇔ w_drift < (1 − d_5(rob))·w_rob + 1.25λ
- 5 > 7 ⇔ d_5(rob)·w_rob > u_V·w_V + 1.25λ

i.e. the mission's order lives where **rob outweighs drift, the vs-code
outcome is deferred, and cost is lightly weighted** — the "Rob-first"
sensitivity reading mission-C's own `:preference :note` names. High λ kills
it: 7 is the cheapest cascade, so cost-dominance puts 7 first.

## 4. Computed results (`h_value_d.py` output, fractions of the swept space)

share = fraction of grid points; cells are (share 4>5>7, share 7-first).
W-simplex is 1001 points per (λ, u_V, d_5(rob)) cell.

| λ | baseline d=1,u=1 | full sweep | degree only d5R≤.5 | timing only uV≤.5 | text reading (both ≤.5) |
|---|---|---|---|---|---|
| 0 | (0.000, 0.341) | (0.166, 0.269) | (0.233, 0.291) | (0.192, 0.198) | **(0.273, 0.216)** |
| 0.02 | (0.125, 0.448) | (0.218, 0.332) | (0.239, 0.357) | (0.251, 0.259) | **(0.279, 0.287)** |
| 0.05 | (0.125, 0.448) | (0.199, 0.382) | (0.196, 0.417) | (0.226, 0.321) | (0.227, 0.359) |
| 0.1 | (0.165, 0.531) | (0.166, 0.445) | (0.112, 0.471) | (0.188, 0.389) | (0.128, 0.419) |
| 0.2 | (0.154, 0.587) | (0.097, 0.515) | (0.026, 0.522) | (0.109, 0.466) | (0.029, 0.475) |
| 0.4 | (0.046, 0.639) | (0.022, 0.580) | (0.000, 0.580) | (0.024, 0.544) | (0.000, 0.544) |

Cross-check: baseline at λ = 0.1 gives 16.5% for 4>5>7 — claude-10's "at
most 17%" reproduced by construction.

**Headline fractions.** Overall full sweep: 4>5>7 holds on **2.2%–21.8%**
of the space depending on λ (max at λ = 0.02); 7 leads on **26.9%–58.0%**.
Under the text-anchored reading (u_V ≤ 0.5 ∧ d_5(rob) ≤ 0.5): 4>5>7 peaks
at **27.9%** (λ = 0.02) and 7-first bottoms at **21.6%** (λ = 0). The
mission's order is a *minority* of every swept space, and it requires
small λ — a value term whose cost side dominates cannot reproduce it at
all, because 7 is the cheapest target.

## 5. What the term still cannot express

1. **The deadline on 7 is not a discount.** "Do before a VS Code
   implementation exists, not after" makes 7 *more* urgent early, and the
   mission ranks 7 *last of the three anyway*. A scalar V(i) cannot hold
   both; the text's claim is a scheduling constraint (a hazard on the cost
   of delay, one-sided in calendar time), not a value term. Modelling it as
   u_V < 1 captures "deferred outcome value" (span [14618 14667]) but is
   the opposite sign of the deadline claim — both are in the text, and the
   term must pick one.
2. **Path dependence.** The drift cost is already accruing from copy-paste
   that exists *today* (span [14822]) and grows with other instances'
   delay, so the value of 6 and 7 depends on what is done first and when.
   V(i) is additive over outcomes and independent across instances; an
   order-aware term (value of a *sequence*, with drift interest compounding
   on the undone ones) is the missing generalisation.
3. **"Smallest surface" is a counted cost, not E[attempts].** The mission's
   first reason for 4 is "51 literals plus three sites" (span
   [18061 18160]) — surface area. E[attempts] at θ 0.8 is a proxy whose
   authority is the cost model, not the mission's count; the two are
   different cost concepts and the mission cites the one the model doesn't
   measure.
4. **Degree evidence.** d_5(rob) is swept because the text states no
   number. The mission's ranking is consistent with d_5(rob) ≤ 0.5, but no
   span says so; asserting it would be inventing the preference the
   exercise exists to discover.

## 6. Verdict against the falsifier

The term's free parameters cannot produce *any* order at fixed reading:
the region for 4>5>7 is analytically characterised (§3) and occupies at
most ~28% of the swept space, and that region coincides with the mission's
own Rob-first, defer-VS-Code reading. Timing and degree each move the
fraction in the direction the mission's reasons point (degree: 0% → 23–28%
at λ ≤ 0.02; timing: lowers 7-first from 53% to 22–39%). But the mission's
order remains a minority of every honest sweep, and the strongest
single reading of the text (§2) still leaves 7-first nearly as likely as
4>5>7 at the best λ (27.9% vs 28.7%). The residual disagreement is not
rescuable by better parameters: it lives in §5.1–5.2, the deadline and the
path dependence, which are properties of a *sequence of instances over
calendar time* — exactly the structure a per-target scalar V does not have.
Proposed amendment (definition currently missing): the value term PROOF-2a
wants is over **ordered prefixes of the instance set with a drift-interest
clock**, not over instances; V(i) as above is its static projection.
