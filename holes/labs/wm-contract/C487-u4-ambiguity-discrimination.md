# C487 — U4: does the ambiguity term discriminate? Sweep over every recorded field

Date: 2026-09-02. Seat: claude-cli (wm-edge worklist loop, any-lane).
Item: worklist `:U4` (class `:RUN`), from `SPEC-dormant-wiring.md` §U4.
Script: `holes/labs/wm-contract/u4_ambiguity_sweep.clj` (clj-kondo 0 errors /
0 warnings, `check-parens` OK). Artifact:
`holes/labs/wm-contract/U4-AMBIGUITY-SWEEP.txt`, byte-identical on two
consecutive runs. **Replay only — no live run, no run lock taken, nothing
written under `data/`.**

## 0. The question and the answer

`aif-equations.edn :ambiguity` is theory-defined (dacosta2020 eq. 45–48) and
Lean-closed, and the badge beside it says its influence was "MEASURED 0% flips
/ 674 ticks" (`R5-glossary-formalisation.md:29`, `facts-R5.md:458`). SPEC U4
asked for that zero to be either overturned or typed.

**Typed outcome: (a) — the term discriminates.** Over the 882 archived ticks,
dropping the ambiguity leg from G = risk + ambiguity moves the EFE-best
candidate on **51 ticks**, every one of them a strict overturn rather than a
tie-break, and moves **62 478 candidate ranks**. In the era where the entropy
form is the recorded one (2026-07-03 →) it does this on 8 of 220 ticks, and at
the selection grain it moves the score-ordered winner on 9 of the 122 ticks
where the record licenses the subtraction.

**And the current field's zero is now typed too, and it is neither (b) nor
(c).** On S2 (20 ticks), S4 (4 ticks) and all 79 archived ticks of 2026-09-01
the count is 0 at every grain — but the term is not far from mattering: the
scale factor at which it *would* move the winner is **λ_break = 1.0238 on every
one of those ticks.** Ambiguity is 2.4 % of its own size short of changing the
choice. That is the number the untyped zero was hiding.

## 1. What "drop the term" means, stated before any number was read

The recorded per-candidate score is `:controller-score` (from 2026-07-14) or
`:G-total` (through 2026-07-09). `:G-core` = `:G-risk` + `:G-ambiguity`
(`src/futon2/aif/efe.clj:842-849`) and the augmentation legs enter additively
(`efe.clj:852-872`), so ambiguity enters with coefficient exactly 1 and "drop
it" is "subtract it". Lower is more preferred (`efe.clj:904-919`), so the
statement's *argmax* is an **argmin** here; it is written arg-best below.

Two grains, because only one is checkable on every record.

- **Grain A (R5, the theory grain).** arg-best over `G-risk + G-ambiguity`
  against arg-best over `G-risk` alone. Both legs are recorded per candidate in
  every era, so this needs no reconstruction of the controller blend. It is
  what eq. 42 asks. Available on all 882 ticks.
- **Grain B (selection grain).** The live rule is "first entry of the
  score-ordered list whose action type is not `:no-op`"
  (`src/futon2/aif/policy.clj:497-499`), re-ordered by `score − G-ambiguity`.
  Licensed only where the record carries `:G-core` beside `:controller-score`,
  i.e. **122 of 882 ticks**. The earlier records are counted as not measurable
  at this grain rather than reconstructed at guessed weights: the legacy leg
  weights drifted inside the corpus, and the four-leg identity at the weights
  the source declares today (`efe.clj:83-84`) leaves residuals up to 1.398 on
  2026-06-10 records.

**Ties.** Recorded candidate sets are full of exact ties — the risk-only argmin
is tied on **642 of 882** archived ticks — so "the index moved" is not "the
term discriminated". Every count is reported twice: as a change of the chosen
*action*, and as STRICT, meaning the with-ambiguity winner is strictly worse on
the ambiguity-free score. On these fields the two counts coincide: all 51
grain-A changes and all 9 grain-B changes are strict.

## 2. Controls, run before the measurements were read

| control | what it forecloses | result |
|---|---|---|
| **C1** coefficient | subtracting a term that does not enter the score with weight 1 | max \|`G-core` − (`G-risk`+`G-ambiguity`)\| over the 122 grain-B ticks: **0.000e+00** |
| **C2** src reproduction | reading a second implementation of ambiguity rather than the machine's | recompute `:G-ambiguity` from the record's own `:prediction-variance` with `efe.clj`'s own private `ambiguity` fn: **13 622 entropy-carrying candidates, max \|Δ\| 0.0000e+00**; **588 `:learn-action-class` candidates, max \|Δ\| 1.2545e+02** — see §5 |
| **C3** decision reproduction | claiming grain B measures the *enacted* action | the live rule replayed on the recorded order reproduces the record's own `:decision` on **1 of 122** ticks; grain B is therefore a statement about the score ordering, not the enactment, and the enactment question is answered by B′ instead |
| **C4** positive control | a detector that reports 0 because it is broken | drop **risk** instead of ambiguity: winner changes on **461 / 882** ticks (**200 / 220** in the entropy era, **100 %** of the 2026-09-01 and S2/S4 ticks) |
| **B′** ordering-independent bound | grain B's dependence on a selection filter it cannot evaluate | count candidates that overtake the *recorded decision's* candidate when ambiguity is dropped — an upper bound on the enacted change under any order-independent filter: **18 / 122** archived, **0 / 20** on S2, **0 / 4** on S4, **0 / 79** on 2026-09-01 |

## 3. Fields

| field | ticks | note |
|---|---|---|
| ARCHIVE `data/wm-trace/wm-trace-YYYY-MM-DD.edn` | 882 records, 882 usable | 55 files, 2026-05-18 → 2026-09-01 |
| S2 `runs/2026-09-01-s2/wm-trace-s2.edn` | 20 | all 20 timestamps also present in ARCHIVE |
| S3 | **0 — not found** | `runs/2026-09-01-s3/` holds `ARMS.txt` and `README.md` only; its README's first line says it is a replay, not a 20-tick stage run. The same absence C486 §3 recorded. |
| S4 `runs/2026-09-01-s4/wm-trace-s4.edn` | 4 | all 4 timestamps also present in ARCHIVE |

**The 674 was not reproduced and is not the denominator here.** The archive
holds 882 records carrying `:ranked-actions` with numeric `:G-risk` and
`:G-ambiguity`. Whatever population the badge counted, it is not this one, and
its provenance is **not found** in the lab directory — the badge's own
`:note` (`facts-R5.md:458`) carries the number with no pointer to a script or a
record set. Every count below names its own denominator.

**Era boundary 2026-07-03.** That is the first archived date whose recorded
`:G-ambiguity` is on the gaussian-entropy scale: the within-tick range over
entropy-carrying candidates is 0.0300 on 2026-07-02 and 16.4647 on 2026-07-03.
The `:ambiguity-mode` *key* only appears from 2026-07-14, so splitting on the
key would misfile 2026-07-03…07-09.

## 4. The measurements

Grain A′ = grain A restricted to entropy-carrying candidates (§5 says why).

| group | ticks | A′ changed (all strict) | grain B changed | B′ decision overtaken | λ_break min / median |
|---|---|---|---|---|---|
| ARCHIVE, all | 882 | **51** (5.78 %) | 9 / 122 | 18 / 122 | 1.0109 / 16.9167 |
| ARCHIVE ≤ 2026-07-02 | 662 | 43 (6.50 %) | not measurable | — | 1.5454 / 16.9167 |
| ARCHIVE ≥ 2026-07-03 | 220 | 8 (3.64 %) | 9 / 122 | 18 / 122 | 1.0109 / 1.1001 |
| ARCHIVE 2026-09-01 | 79 | **0** | 0 / 79 | 0 / 79 | 1.0238 / 1.0238 |
| S2 | 20 | **0** | 0 / 20 | 0 / 20 | 1.0238 / 1.0238 |
| S4 | 4 | **0** | 0 / 4 | 0 / 4 | 1.0238 / 1.0238 |

**λ_break** is the smallest scale factor λ > 1 at which arg-best over
`risk + λ·ambiguity` stops being the recorded arg-best. Because the winner
wins at λ = 1, only candidates with *lower* ambiguity can overtake, each at the
threshold (risk_w − risk_j)/(amb_j − amb_w), which is ≥ 1 by that same fact.
It is reported because a bare "0 flips" says nothing about how close the term
came, and here the answer is: very close.

**The size of what ambiguity overturned, when it did.** Pre-2026-07-03 the
risk gaps overturned are 0.0075 to 0.0130 — hair-thin, on a field where the
whole ambiguity range is 0.0300. From 2026-07-03 they are 0.5075 to **7.8208
nats**.

**A worked instance** (six ticks on 2026-07-16, run records
`2026-07-16T10:17:45.295574234Z`, `…T11:00:52.576211347Z`,
`…T13:53:42.810954707Z`, `…T14:17:49.951440902Z`, `…T15:18:40.575143608Z`,
`…T16:16:26.896894340Z`). Ambiguity promotes a `:fire-pattern` candidate
(`musn/plan-before-tool`, ambiguity −116.79209997800035) over an
`:advance-mission` candidate (`M-stack-geometry`, ambiguity
−108.87689318874708) which sits at rank 4 in the recorded order. The risk
disadvantage overturned is **7.820835 nats**, against an ambiguity advantage of
**7.915207 nats** — a margin of 0.094 nats. 7.915207 = ½·ln(0.0075 / 1e-9): one
channel's worth of variance, the difference between a channel the
`:fire-pattern` action model treats as deterministic and one it does not.

## 5. Two different quantities share the `:G-ambiguity` key

`efe.clj:680-682`:

    g-ambig (if learn-action?
              (:predictive-variance zone-evidence)
              (reduce + 0.0 (vals ambiguity-terms)))

For `:learn-action-class` candidates the key holds the capability zone's Beta
**predictive variance**, not E_Q(s|π)[H(P(o|s))], and `ambiguity-terms` is `{}`
(`efe.clj:678-679`). Every one of the 588 such candidates that C2 could check
records the same value, **0.25** — the maximum variance of a Bernoulli, i.e.
the uninformative value. Entropy-carrying candidates on the same ticks record
−125.1977213937595 … −108.87689318874708. The two live in the same
`:G-ambiguity` key, are summed into the same `:G-core`, and sit **125 nats**
apart on a quantity whose whole entropy spread is 16.5.

This is why every measurement is reported twice. Scoring on the recorded key as
it stands gives **371 / 882** grain-A changes; restricted to the candidates
whose `:G-ambiguity` is the ambiguity term, **51 / 882**. Seven-eighths of the
apparent discrimination is the unit collision, not the term. The 51 is the
honest number.

Recorded as a finding, not a ruling: this looks like a row of its own — either
a second key for the zone term or a declared alias — and minting it is Joe's.

## 6. Why the current field's zero, typed

The SPEC offered (b) "H(P(o|s)) near-constant across candidates → the inertness
lives in our A, and the row says so with the variance" and (c) "C flat over the
outcomes ambiguity distinguishes → the fix belongs to the outcome carrier".
**Both fail as stated, and the measurement says what replaces them.**

**(b) is false on today's field.** The within-tick spread of the entropy term
over entropy-carrying candidates on S2/S4 is **16.3208 nats** (max sd 1.78464),
and on the archive as a whole 16.4647. It is not near-constant. It *was*
near-constant before 2026-07-03, when the `:variance-sum` proxy gave a
within-tick range of 0.0300 — so (b) describes the era the badge was written
in and not the machine that runs now.

What is true, and is the sharper statement: the term takes only **4 distinct
values** across the ~139 candidates of a tick. It is a coarse step function,
not a constant, and the step is 2.4 % smaller than it needs to be.

**Where the step comes from.** Over the 98 decomposable ticks (records carrying
`:prediction-variance`), the per-channel decomposition
½·ln(2πe·σ²_ch) differs across candidates on exactly **3 of 14 channels**:

| channel | ticks where candidates differ | max spread | distinct values |
|---|---|---|---|
| `:mission-health` | 98 / 98 | 8.261780380 | 3 |
| `:sorry-count-norm` | 98 / 98 | 8.059047825 | 2 |
| `:ticks-firing-ratio` | 98 / 98 | 8.405621416 | 2 |
| the other 11 | 0 / 98 | 0.000000000 | 1 |

The other eleven are `{:status :absent :reason :deterministic-by-action-model}`
and are floored at 1e-9 (`efe.clj:56-59`), contributing an identical −8.9435
nats to every candidate.

**(c) is false for two of those three channels.** C is the channel-grain
`[lo hi]` floor at `src/futon2/aif/preferences.clj:9-23`, read through
`current-C` (`preferences.clj:60-71`). It is not flat or absent on the channels
ambiguity distinguishes: `:mission-health [0.5 1.0]`, `:sorry-count-norm
[0.0 0.3]`. It *is* degenerate on the third: `:ticks-firing-ratio [0.0 0.0]`, a
point preference. (`:depositing-signal` is the one observation channel C does
not name at all, and no candidate varies its ambiguity through it.)

**What replaces (b) and (c): the forward model declares variance where the
world is still.** On S2 and S4 the observation is *exactly constant* across
every tick on all three channels ambiguity varies through —
`:mission-health`, `:sorry-count-norm`, `:ticks-firing-ratio` each show 1
distinct value and range 0.00000 over 20 and over 4 ticks. The channels that do
move are `:loop-health` (14 distinct on S2, range 0.0233811),
`:mathematics-pct`, `:portfolio-pct`, `:stack-pct` (8 distinct each) — and no
candidate declares a predicted variance for any of them, so they enter every
candidate's ambiguity at the same floor. This is the same structure C486 §5
found on the F_π side and reached independently here from the C-vector and the
observation series.

So the deficiency is not C's flatness and not A's constancy. It is the
**coverage of the action models' declared variances**: ambiguity can only speak
about the three coordinates the world is not moving in. The fix belongs with
whatever gives the action models a predicted variance on the channels that
actually vary — which is upstream of G, and is the outcome-carrier direction
(c) pointed at, arrived at for a different reason than (c) gave.

## 7. What this does not claim

- No ruling. Nothing here is written to `:choices` or `:decisions`.
- **Grain B is not the enacted action.** C3 measured that: the live-rule replay
  reproduces the record's own `:decision` on 1 of 122 ticks, because the modern
  path applies a further filter (`:selection-boundary
  :reason-bearing-strategic-policy` on the S2 records) that the recorded
  ranking alone does not determine. B′ is the claim that survives: on S2, S4
  and 2026-09-01, **no** candidate overtakes the recorded decision's candidate
  when ambiguity is dropped, so under any order-independent filter the enacted
  action is unchanged there.
- The 51 archived grain-A changes are a statement about the recorded EFE
  ordering. Whether the machine would have *acted* differently on those
  2026-05/06 ticks is not measurable from the record, for the reason §1 gives.
- 674 is neither reproduced nor refuted; its population is **not found**.

## 8. Reproduce

```
cd /home/joe/code/futon2
clojure -M holes/labs/wm-contract/u4_ambiguity_sweep.clj \
  > holes/labs/wm-contract/U4-AMBIGUITY-SWEEP.txt
```

No timestamp is written into the artifact, so "re-run it and diff" is a check
anyone can make — the standard `run7_beta_arms.clj` / `ARMS.txt` and
`u2_retrodiction.clj` / `U2-RETRODICTION.txt` set.
