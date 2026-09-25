# H-VALUE-CAL-D — C over a calendar index, separate from rollout step

claude-11, 2026-09-25. PROOF-2 packet on claude-8's requisition (H-value hole,
PROOF-2a). Read-only design plus one measured check. Script: the
`H-VALUE-CAL-D` section appended to `h_value_c_d.clj` (babashka, its own
process, no repo writes, nothing loaded into a shared JVM). The sections above
it are unchanged: their output is byte-identical to the pre-change script
apart from one blank line.

**Verdict up front: negative.** A calendar index removes the harm H-VALUE-C-D
found. Rollout-step decay pushed 4 > 5 > 7 down to 0.3–7.7%; on the calendar
index it holds on **3.9–29.6%** of the space (single-pick ranking, full sweep,
depending on λ), and on **25.9%** under the sequence ranking. That is back
inside H-VALUE-D's static range (2.2–21.8%, text reading up to 27.9%), not
above it. 4 > 5 > 7 is a majority in only one sub-reading: the one where the
VS Code implementation arrives before any instance can land. That reading
contradicts the text (§5.1). Where the deadline on 7 is still reachable,
which is the case the text describes, the fraction *drops*, to 18–23%.

## 1. Inputs read

- `H-VALUE-D.md` (kimi-7): a static V per instance; 4 > 5 > 7 holds on 2.2–21.8%.
- `H-VALUE-C-D.md` + `h_value_c_d.clj` (claude-4, 9b5437d0): rollout-step decay
  penalises 4 first, because 4's cascade is the deepest (want steps: 4 at 5.0,
  5 at 4.0, 7 at 3.0).
- `H-VALUE-G-D.md` (claude-6): the lane-G universe bug. It concerns the lane's
  `horizon-g-sparse` partition function. It does not touch this script's V,
  which never computes a lane G, so no correction carries over.
- Mission text `futon3c/holes/missions/M-futon-seams.md`, sha256
  `d13c5cfe…c6f6fd`, which equals mission-C's `:mission-sha`. Spans below are
  zero-based, end-exclusive **character** offsets (mission-C's
  `:offset-unit`). Read as bytes, they land elsewhere.
- The four cascades `proto/instance-{4,5,6,7}.edn`, plus `item6/mission-C.edn`
  and `item6/target-cost.edn`. Their sha256s have changed since H-VALUE-C-D
  pinned them (instance files and mission-C). The unchanged sections still
  reproduce H-VALUE-C-D's numbers exactly (λ = 0: no timing (0.1996, 0.2970),
  full sweep (0.0771, 0.5019)), so the changes do not reach this computation.
  Current shas are in §4.

## 2. The mission's ordering and timing sentences

| # | text | span | kind |
|---|---|---|---|
| O0 | "The candidates in cost order" | [18030 18058] | **the list's own key: cost** (§5.3) |
| O1 | 4 — "smallest surface, 51 literals plus three sites…" | [18061 18160] | cost (surface) |
| O2 | 4 — "unblocks provider substitution for Rob immediately" | [18227 18277] | timing: value starts at completion |
| O3 | 5 — "larger, but it retires `matrix-ircd`" | [18337 18358] | cost; degree unstated |
| O4 | 7 — "do before a VS Code implementation exists, not after" | [18462 18514] | **deadline predicate on an event** |
| O5 | "the cheapest *remaining* moment is before the VS Code version exists" | [16272 16340] | same event; the deadline has not passed |
| O6 | "extracting now means three existing callers move at once, or a fourth implementation appears" | [16149 16241] | the same event, named as "a fourth implementation" |
| O7 | "He is **not** asking for those adapters to be built now" | [14618 14667] | V's value is deferred to that event |
| O8 | "Divergence has already started here" | [15502 15537] | drift origin is in the past |
| O9 | "Pick one instance and declare its interface — not all eight." | [17969 18029] | one pick |
| O10 | "For whichever is chosen, the exit is: the interface is declared, **at least one existing caller is converted to it**, and there is a test…" | [18517 18654] | the exit is a conjunction |
| O11 | "Each was checked against the working tree on 2026-09-24" | [4714 4769] | the only date: t = 0 |

The text gives no date for any future event and no duration for any work.

## 3. The proposal, as data

```edn
{:schema :wm/c-calendar-index-draft-v0
 :clock {:unit :expected-attempt          ; declared; the mission states no durations
         :origin {:event :e/now :date "2026-09-24" :span [4714 4769]}}
 :events
 {:e/now {:at 0}
  :e/vs-code-exists {:at {:status :unstated :swept [5 15 25 40 :never]}
                     :stated-as ["a VS Code implementation exists" [18462 18514]
                                 "before the VS Code version exists" [16272 16340]
                                 "a fourth implementation appears" [16149 16241]]
                     :already-happened? false}   ; O5: "cheapest remaining moment"
  :e/drift-began {:at {:status :past :date :unstated} :span [15502 15537]}}
 :step->calendar
 {:rule :all-wants-at-exit              ; O10: the exit is a conjunction
  :completion {:instance-time :expected-attempts-theta-0.8
               :source "item6/target-cost.edn"}
  :rollout-step-contributes :nothing}   ; a step count is not a date
 :attainment                            ; the degree-of-attainment term
 {:form "A(i,o,t) = d_i(tau,o) * 1[t >= T_i] * knee_i(T_i)"
  :d {:d5-rob {:status :unstated :swept [0.25 0.5 0.75 1.0] :span [18337 18358]}
      :else 1.0}
  :knee {"7" {:predicate [:before :e/vs-code-exists] :span [18462 18514]
              :if-missed {:kappa {:status :unstated :swept [0 0.5]}}}}}
 :value-flow                            ; what an attained outcome is worth over [0, H]
 {:R :from-completion :S :from-completion :D :from-completion :C :from-completion
  :V {:from :e/vs-code-exists :requires [:before :e/vs-code-exists] :span [14618 14667]}
  :horizon {:status :unstated :swept [30 60 120]}}}
```

This does three things the rollout-step schedule could not:

1. **Calendar time is a property of the flight, not of token depth.** Every
   want of instance i is attained together at T_i = E[attempts_i]. This is
   declared, not derived from the rollout. The text supports it: the exit is a
   conjunction (O10), so a token part-way down a cascade earns nothing on its
   own. With this rule, 4's six-deep cascade (H-VALUE-C-D §3) no longer costs
   it anything.
2. **The deferral and the deadline are the same event.** H-VALUE-D §5.1
   found them pulling in opposite directions ("the term must pick one"). Here
   V's value flows only from E_vs onward (O7). 7's services keep their degree
   only if 7 lands before E_vs (O4, O5). The swept u_V is gone, replaced by
   the event's position.
3. **Deadlines are predicates over a named event**, so "not after" can be
   checked against a record of when a non-Emacs client appeared, rather than
   fitted as a rate.

Two rankings are measured:

- **single-pick**: V(i) = Σ_o w_o · Σ_τ A(i,o,·) flowed to H / H − λ·E[attempts_i],
  one instance done now. This is O9, and the same comparison the earlier
  packets used.
- **sequence**: the order of {4, 5, 7} worked one after another. Each
  instance's value is taken at its cumulative completion time. λ·cost is the
  same for every order and drops out, so this row is λ-free. "4 > 5 > 7" means
  (4 5 7) is the strict best of the six orders. "7-first" means the best order
  starts with 7. Instance 6 is unranked (as before) and is not in the sequence.

## 4. Computed results

`bb h_value_c_d.clj`. Inputs (sha256) as printed by the run:

```
d76e38791455a26245892720f877005b9a58f8dae41290dffebc7725ade03b0a  proto/instance-4.edn
311a61e7c3513c6e63853ca59525ad65685847aff3ad43dbae7f56fa61f6121e  proto/instance-5.edn
826de016723cc9a0c7fddfc8235e9b611345ff7b39d6abdd7c29a2146ef18f93  proto/instance-6.edn
036f85666d72cc13e9bde31c82e5323e7336c6e228f748a9dbfedef719fe4937  proto/instance-7.edn
c7d2c5cba4a3091dbfa274f9f9291e1984978dd0a93b9743725aaa2e8105b8f7  item6/mission-C.edn
7d77ef0299ede3427035ee88c3bbf4f7b4fadca64954214540380b3769d64e3d  item6/target-cost.edn
```

**Cross-check of the new assembly.** With every attainment share forced to 1,
the calendar code rebuilds the static token-grain cells at u_V = 1, ρ = δ = 0:
(0.1441, 0.3849) against (0.1441, 0.3849), `:equal true`.

Cells are (share 4>5>7, share 7-first); single-pick / sequence. There are
1001 weight points per cell. Full sweep = 120 cells (H × s_vs × κ × d₅ᴿ).

| λ | full sweep | deadline reachable (s_vs > T₇) | E_vs never in horizon | E_vs after all three (40) | E_vs mid-sequence (15, 25) | E_vs before anything lands (5) |
|---|---|---|---|---|---|---|
| 0 | (**0.2960**, 0.2576) / (0.2591, 0.4431) | (0.2294, 0.3143) / (0.1805, 0.5208) | (0.3116, 0.2020) / (0.2750, 0.4061) | (0.2380, 0.2940) / (0.2750, 0.4061) | (0.1840, 0.3806) / (0.0861, 0.6356) | (0.5626, 0.0309) / (0.5735, 0.1323) |
| 0.02 | (0.2746, 0.2708) / same | (0.2025, 0.3306) / same | (0.2842, 0.2090) / same | (0.2074, 0.3123) / same | (0.1591, 0.4005) / same | (0.5632, 0.0320) / same |
| 0.05 | (0.2421, 0.2884) | (0.1622, 0.3513) | (0.2248, 0.2290) | (0.1636, 0.3413) | (0.1302, 0.4175) | (0.5616, 0.0370) |
| 0.1 | (0.1942, 0.3237) | (0.1091, 0.3929) | (0.1468, 0.2809) | (0.1098, 0.3808) | (0.0900, 0.4549) | (0.5347, 0.0472) |
| 0.2 | (0.1206, 0.3701) | (0.0477, 0.4449) | (0.0617, 0.3430) | (0.0483, 0.4329) | (0.0405, 0.5018) | (0.4120, 0.0711) |
| 0.4 | (0.0389, 0.4191) | (0.0066, 0.4924) | (0.0080, 0.3959) | (0.0065, 0.4825) | (0.0060, 0.5456) | (0.1680, 0.1257) |

The sequence values do not depend on λ (see above); rows after λ = 0.02 omit them.

**Headline, against the earlier packets.**

| | 4 > 5 > 7 holds on | 7 leads on |
|---|---|---|
| H-VALUE-D, static V | 2.2% – 21.8% | 26.9% – 58.0% |
| H-VALUE-C-D, rollout-step decay (full sweep) | 0.3% – 7.7% | 50.2% – 66.8% |
| **H-VALUE-CAL-D, calendar, single-pick (full sweep)** | **3.9% – 29.6%** | **25.8% – 41.9%** |
| **H-VALUE-CAL-D, calendar, sequence (full sweep)** | **25.9%** | **44.3%** |
| H-VALUE-CAL-D, single-pick, deadline reachable | 0.7% – 22.9% | 31.4% – 49.2% |

## 5. Reading

**5.1 The calendar index stops penalising 4, but that only undoes the harm.**
All of H-VALUE-C-D's anti-correlation came from indexing C by token depth.
Once every want lands at the flight's completion time, 4 and 7 land at the
same time (8.75 attempts each) and 5 lands later (10). The timing differences
between instances are small, so single-pick V is close to the static V with
u_V derived from E_vs. Where E_vs is not within the horizon, V's weight is
effectively zeroed, and 4 > 5 > 7 rises to 31.2% at λ = 0. H-VALUE-D already
reached the same neighbourhood (27.3%) by sweeping u_V ≤ 0.5. The calendar
index gives that deferral a textual basis (O7 tied to E_vs); it does not add
reach.

**5.2 The deadline works against the ranking unless it is far away.** The
text says 7 must land before E_vs (O4) and that this moment has not yet
passed (O5). When E_vs falls mid-sequence (15 or 25 attempts), the only way to
meet it is to do 7 early. The sequence ranking then puts 7 first on 63.6% of
the space, and 4 > 5 > 7 falls to 8.6%. The mission's order and its deadline
are consistent only if the author expects E_vs after 4 and 5 are both done.
In that case the deadline is met in every order, V is the same across orders,
and the calendar index has no effect on the ranking. The sequence share is
then 27.5%: the same number whether E_vs is at 40 or never.

**5.3 The one majority (s_vs = 5) contradicts the text.** If VS Code exists
before any instance can land, 7 always misses its deadline and carries κ. 4 >
5 > 7 then holds on 56–57% for λ ≤ 0.05 (single-pick) and 57.4% (sequence). But O5 says the cheapest remaining moment is
*before* the VS Code version exists, so the author treats it as reachable.
This cell is reported, not used.

**5.4 What decides the ranking is still the unstated weights.** With timing
neutralised, the region is H-VALUE-D §3's: rob outweighs drift, and d₅ᴿ < 1.
The mission states no weights (`:preference :unstated`, [17969 18029]), so
every figure here is a share of a simplex, as before.

**5.5 The list is headed "in cost order".** O0 is the list's own heading: "The
candidates in cost order" [18030 18058]. The reasons it gives are one
surface count (O1), one "larger" (O3), and one timing clause (O4). By its
text, 4 > 5 > 7 is ordered by cost, where cost is surface size. It is not
stated as an order of value. H-value's test ("no weighting reproduces the
mission's own ranking") treats it as a value ranking. The script's cost is
E[attempts] at θ 0.8, which ranks 7 as cheap as 4 (8.75), so it does not
reproduce the stated cost order either. H-VALUE-D §5.3 already noted that the
mission cites a counted surface and the model measures attempts. I am
recording this as a question about the target, not as a verdict on it.

## 6. What is missing, proposed as amendments

1. **A calendar index for C** (§3), separate from `:c-schedule`'s rollout-step
   `:placement`. It has a declared clock, named events with typed absent
   dates, and the step→calendar rule `:all-wants-at-exit`. On this mission it
   removes the anti-correlation and does no more. It is still the right place
   for O4–O7 to live, because they are statements about events, not about
   token depth.
2. **The H-value target's order key.** Before a value term is tested against a
   mission's ranking, the ranking's stated key must be recorded. M-futon-seams
   keys its list on cost (O0). A value-term test needs either a mission that
   ranks by value, or a restatement of H-value as "reproduces the stated
   order under the stated key". The second would make the cost model's
   surface-vs-attempts mismatch (H-VALUE-D §5.3) the thing under test.
3. **E_vs's expected date, or a record that none is expected.** §5.2 shows the
   mission's order presupposes E_vs after 4 and 5 are both done. If the
   mission's author states that, the deadline cells collapse to the
   "after all three" column (single-pick 0.6–23.8%, sequence 27.5%), and the
   result stays negative.

The index and events are declarations for the owners of `live_c.clj` and
`preference-member`. No lane code is edited here.

## 7. Does the mission state a value ordering over instances?

*claude-8 asked for this as "§5". The packet already has a §5 (Reading) and a
§6, so it is appended as §7.*

I read the whole mission: `futon3c/holes/missions/M-futon-seams.md`, 65,070
bytes, sha256 `d13c5cfe9e9b19b445bd5bb73507f286a9e5ff3b478a1c5bc6a2250d70c6f6fd`
(last commit to the file: futon3c `3f5f44dd`). Spans are character offsets as
before. I looked for every sentence that ranks, prefers or prioritises
instances, and for every per-instance benefit or consequence claim.

**Answer: (b).** No value ordering over instances is stated. The only
ordering is by cost, plus the deadline event on 7. (c) does not apply either:
the outcomes are stated, but nothing orders or weights them.

**The ordering the mission does state, and its key:**

| span | text | what it orders by |
|---|---|---|
| [4361 4402] | "## Cost ordering (use this to prioritise)" | the mission's only instruction on how to prioritise. Its three tiers are cost tiers: declare first (free), retrofit in code (expensive), retrofit in prompt text (worst) |
| [18030 18058] | "The candidates in cost order" | heads the IDENTIFY list 4, 5, 7 |
| [44631 44669] | "IDENTIFY's anchor is the cost ordering" | the mission's own account, written after the fact, of what the IDENTIFY choice rested on |
| [44454 44563] | a war-room pattern "bears on the IDENTIFY exit's choice of one instance over eight, but the mission did not cite it when choosing" | a second possible basis for the choice, which the text says was **not** used |

**Per-instance benefit and consequence claims.** None of them is ranked
against another instance:

| instance | span | text | value or cost-justification? |
|---|---|---|---|
| 4 | [18227 18277] | "unblocks provider substitution for Rob immediately" | **A per-instance benefit with a timing, not a ranking.** It is a clause in item 1 of a list whose stated key is cost [18030 18058]. It compares 4 with nothing, and 5 and 7 carry no Rob-unblocking clause to compare it against. In the list it answers "why is the cheapest one worth doing", not "why is this worth more than 5". Its outcome is `:rob-can-run-the-stack`, already stated at [2931 3026]. |
| 5 | [18322 18408] | "larger, but it retires `matrix-ircd` and the \"don't start an IRC server\" flag together" | A benefit offered **against** a higher cost ("larger, but"). This is the one place the list weighs benefit against cost, for one instance. It does not say the benefit places 5 above or below anything. |
| 7 | [18462 18514] | "do before a VS Code implementation exists, not after" | A deadline on an event (§2, O4). It says **when**, not how much. |
| 7 | [14434 14484] | "Editor coupling — the one still compounding" | A consequence claim: cost grows while it waits. It is a claim about the rate of cost, and it would argue for doing 7 **earlier**, while the list puts 7 last. |
| 6 | [12358 12416], [13480 13511] | "the most painful retrofit", "Why this is the worst case" | Cost (the retrofit tier). 6 is not in the candidate list. |

**Outcomes.** The mission states the value it wants per outcome: Rob can run
the stack, a second implementation is cheap, no drifting forks, and so on.
mission-C records six outcomes, each with its span. No sentence weights one
outcome above another, and mission-C records `:preference :status :unstated`
for that reason. So value is stated per outcome but not ordered. The outcome
list gives H-value the things to value, not an order to reproduce. That is why
the answer is (b) and not (c).

**H-value's next measurable question.** With no value ordering stated, the
value term has no ranking on this mission to reproduce. The next question has
to be one the record can decide:

> Given the mission's stated outcomes, the stated links from instances to
> outcomes, and its stated timing (Rob "immediately"; V deferred to the
> VS Code event; the deadline on 7), does the value term leave instance 4, the
> one the cost ordering chose, undominated?

"Undominated" means no admissible weighting of the unweighted outcomes puts
another candidate above 4 **on every** weighting, and the answer is reported
as the fraction of the simplex on which 4 is the argmax of V. This is the
"4 first" share, not the full 4 > 5 > 7 share. The calendar sweep already
contains its inputs.

The claim under test becomes that value and the declared cost ordering do not
**conflict** at the first pick. They do not have to agree on the whole order.
It stays falsifiable: if some candidate beats 4 on every weighting, or 4 is
first on only a sliver, value contradicts the cost choice, and that finding
would belong in PROOF-2a. The hold-out stays as in H-VALUE-C-D §7, and needs a
mission that states an order by value.
