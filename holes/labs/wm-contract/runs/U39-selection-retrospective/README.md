# U39 — the selection-rationale retrospective, DERIVE pass

claude (wm-build-loop), 2026-09-03. Row U39, class I, `:covers-key
[:choices :selection-retrospective]`.

Joe's ask (2026-09-03): *"when the machine refuses to work on a given piece of
work... are we logging that rationale and learning whether the rationale holds
up... does it maybe regret that choice... we need to really focus on that
learning loop aspect"* — with no strong prior on the right form, so this derives
one and registers the choice-set. **No ruling is written here.** The registry
entry `aif-equations.edn :choices :selection-retrospective` records what the
machine is observed to do and what the free hands are; it does not decide them.

Every number below comes from `u39_selection_retrospective.bb`, run from the
futon2 root as `bb holes/labs/wm-contract/u39_selection_retrospective.bb`.
Read-only: no tick, no run lock, no substrate call, no network, nothing written
under `data/`. The artifacts are `u39-measurements.edn` (typed) and
`U39-SELECTION-RETROSPECTIVE.txt` (the same census in prose); both are
byte-identical on two consecutive runs, because neither carries a wall-clock
field.

## 1. What already exists, counted

The corpus is `data/wm-trace/`: **56 files, 885 records**. The retrospective
needs a rationale side and an outcome side, so both were counted rather than
assumed.

| side | carrier | records carrying it |
|---|---|---|
| rationale | `[:decision :controller-ranking]` | 94 |
| rationale | `[:decision :selection-law]` | 94 |
| rationale | `[:ranked-actions]` (per-candidate G decomposition) | 885 |
| rationale | `[:policy-support-exclusions]` | 125 |
| outcome | `[:realized-outcome]` | 88 |
| outcome | any of the three places `trace-outcome` looks (`war_machine.clj:2328-2332`) | **0** |
| outcome | `[:selection-gain :samples]` > 0 | **0** (all 125 read 0) |

**Records carrying both sides: 0.** The 94 ranking-bearing records are
2026-08-30 → 09-02; the 88 outcome-bearing records are 2026-07-02 → 07-06. The
two halves of a retrospective have never co-occurred on one record. That is the
single fact that shapes the design: **the evaluator cannot be a same-record
read; it has to be a join keyed by a claim id.**

Two further measurements about the outcome side, because "the carrier exists"
and "the carrier is readable" are different claims:

- All 88 `:realized-outcome` maps have keys `(:expected-G :policy :realized-G
  :tick)`. `selection-gain/fold-realized-outcome` (`selection_gain.clj:196-206`)
  admits a sample only when `:expected-score` **and** `:realized-score` are
  numbers. **0 of 88 are foldable**, and `:samples` is 0 on every record that
  carries a gain state. The correctly-shaped producer exists
  (`fold_realized.clj:96-100` emits exactly those two keys); it is not what
  wrote these 88 records.
- No live tick record carries an outcome at all, so
  `previous-selection-non-progress?` (`war_machine.clj:2312-2326`) can never
  read "progress happened" on the tick path — its `:grounded-change` test is
  against a field a different producer writes.

## 2. The machine refuses in two places, and only one of them says why

**Admission refusal** — `[:policy-support-exclusions]`: 376 refusals over the 94
ranking-era records, 4 per tick, every one typed
`:mission-absent-from-capability-graph`, each carrying the whole rejected action
including its proposer provenance. The candidate is never scored.

**Selection rejection** — everything in `[:decision :controller-ranking]` below
the chosen entry: 144–146 candidates per tick, so ~145 rejections per tick, and
**no reason field at all**. What the record does carry, per candidate, is the
full G decomposition (`:G-risk`, `:G-ambiguity`, `:G-goal-outcome`,
`:augmentation-terms`, `:habit-prior-bias`, and the mission factors `:central`
`:strategic` `:doable` `:mission-value-factor` `:non-progress-decay` on the
action).

So the answer to *"are we logging that rationale"* is: for the admission
refusals, yes, with a typed reason; for the selection rejections, the ingredients
are all logged and the reason is not. **Shape (a) is the projection that turns
the ingredients into a reason** — and it needs no new logging.

## 3. (a) The RATIONALE-AS-CLAIM record

    {:claim/id                 "rc-<run-id>"
     :claim/at                 <trace :timestamp>
     :claim/source             {:file … :line … :run-id …}
     :claim/law                <[:decision :selection-law] verbatim>
     :claim/soundness-at-mint  {:status :sound|:unsound …}
     :claim/chosen             {:action … :controller-rank … :G-core …
                                :terms {…} :mission-value-factor …}
     :claim/rejected           [{:action … :controller-rank …
                                 :margin/total   <G-core difference>
                                 :margin/by-term {<term> <difference> …}
                                 :lost-on        <term carrying most of it>
                                 :lost-on-share  <its fraction>
                                 :term-source    {…the factors behind that term…}
                                 :counter-channel {:channel :habit-prior
                                                   :favours … :by …
                                                   :consulted? …}} …]
     :claim/refused            [{:action … :reason … :stage :admission
                                 :scored? false} …]
     :claim/assertion          <the proposition the verdict scores>
     :claim/derived-from       [<the seven recorded fields>]
     :claim/new-logging-required :none}

Three things make it a *claim* rather than a summary.

**It asserts something falsifiable.** `:claim/assertion` says the chosen action's
`:G-core` is below each listed rejected one's by `:margin/total`, and that the
margin is accounted for by `:margin/by-term`, with `:lost-on` naming the term
carrying most of it. Later evidence can contradict that.

**It carries its own counter-channel.** A rejected candidate may be preferred by
a channel the applied law did not consult. `:counter-channel` records which, by
how much, and whether it was consulted — `lnE` enters the score the selection
takes its argmax of only under `:full-score-posterior` (`policy.clj:582`,
`:593`; the same fact is recorded as `:habit-authority` at `policy.clj:619-621`).

**It refuses to mint on a record that contradicts itself.** `:claim/soundness-at-mint`
compares the record's `:selection-law` against the record's own ranking. Worked
counter-example from the corpus, run `0a18c4f7-758e-400a-8223-9c52edf07450`
(2026-09-02T13:47:22Z): the law field says `:chosen-rank 1,
:moved-from-controller-head? false`, and the chosen action sits at **controller
rank 124**. The projection returns `{:status :unsound, :reason
:law-field-contradicts-ranking}`. This corroborates, from the trace rather than
from prose, the false stamp already recorded at
`zaif-harness runs/S4-identify-ingest.edn:40`. A claim minted here is refused at
mint; it is not scored later and then found wanting.

### Worked example — `rc-4abad68c-5481-4402-8f0e-252add62c54b`

2026-09-02T14:00:29Z, 146 candidates, chosen `:advance-mission
M-zaif-harness-v1` at controller rank 1, `:selection-law` sound.

| rank | rejected | `:margin/total` (nats) | `:lost-on` | share |
|---|---|---|---|---|
| 2 | M-expressions-of-interest | 0.004028678 | `:G-risk` | 1.000 |
| 3 | M-distributed-frontiermath | 0.051982917 | `:G-risk` | 1.000 |
| 4 | M-symbol-grounding | 0.065287366 | `:G-risk` | 1.000 |

The share is exactly 1.000 on all three because `:G-ambiguity` is byte-identical
across every `:advance-mission` candidate on this tick and every augmentation
term is 0.0 — so on this corpus a mission rejection is *always* a risk
rejection, and `:term-source` is where the reason becomes legible: the rank-2
loss traces to `:mission-value-factor` 0.5769701 against the winner's 0.59.

`:counter-channel` on that same row: the habit prior favoured the **rejected**
candidate by 0.693147 nats and was not consulted (`:applied :controller-head`).

Control C2 pins that the claim invents no number: every `:margin/total`
reproduces the recorded `:controller-score` difference, max absolute deviation
0.0.

## 4. (b) The RETROSPECTIVE-VERDICT record

    {:verdict/claim-id         "rc-…"
     :verdict/evaluated-against {:run-id … :at …}
     :verdict/window           {:from … :to … :ticks-between …}
     :verdict/legs             {:overtake … :c-mis … :non-progress … :receipts …}
     :verdict/overtake-readings {<reading> <n> …}
     :verdict/verdict          :rationale-upheld | :rationale-refuted
                               | :rationale-untestable
     :verdict/verdict-reason   <typed>
     :verdict/verdict-rule     {:name … :status :declared-not-ruled
                                :scalars :none :statement …
                                :alternatives-not-taken {…}}
     :verdict/basis            [<pointers>]}

### The four legs, and which of them can be read today

| leg | status on the recorded corpus |
|---|---|
| **overtake** | MEASURED. Both halves are on the records. |
| **C_mis movement** (U18 gauges) | UNTESTABLE as posed. The readback is keyed to the tick's *selected* mission (`runs/U42-producers/measurements.edn`), so two ticks that select different missions have two different subjects and there is no movement to read. On 2026-09-02 the readback is `:measured` for exactly one of three runs (M-zaif-harness-v1, `risk_mis` 6.666712) and `:absent :no-measurable-criteria` for the other two. |
| **non-progress** | MEASURED, and see §5 — it measures a decay, not progress. |
| **receipts attributable to the hold** | ABSENT, `:no-mission-to-receipt-carrier`. U23 measured both candidate carriers: flight-discharge is `:writer-exists-no-records` (zero `*.flight.edn` anywhere under `~/code`) and clocked-on records *who* clocked on, not *what* landed (`runs/U23-cascade-catalog/carrier-population.edn`). |

### The overtake test needs an attribution leg, and that is this pass's main finding

The row's statement poses the overtake test as *"did a rejected candidate's own
channels later re-rank it above the chosen"*. Run naively on the recorded pair,
it fires on **all four** top rejected candidates — and the reason is the same for
all four and has nothing to do with any of them:

    chosen mission-value-factor  0.59  ->  0.295     (halved)
    chosen non-progress-decay    1.0   ->  0.5
    every rival's factor         unchanged, byte-identical

The chosen mission was penalised *for having been chosen* (`non-progress-decay-k
1.0`, `war_machine.clj:2290`), its `:G-core` rose by 0.084781 nats, and
everything behind it moved up. A test that fires on that is measuring the decay,
not the rationale.

So the verdict rule declared here **attributes the swing**, using two booleans
read off the records and no tunable scalar:

> **REFUTED** iff some candidate this claim rejected later out-ranked the chosen
> one **and** its own recorded movement alone would have closed the margin the
> claim asserted (`rival-delta-G > margin-at-claim`). An overtake that happens
> only because the chosen candidate was decayed is **UNTESTABLE**
> (`:overtake-attributable-to-the-chosen-candidates-own-non-progress-decay`).
> **UPHELD** requires an outcome leg, and no record in this corpus carries one.

Alternatives named and not taken, so the choice-set is on the record:
*overtake-dominant* (any overtake refutes) — rejected because on the recorded
pair it refutes every claim after one tick; *weighted legs* — rejected because
three of the four legs are typed absences here, so the weights would be
unmeasurable.

### Worked example — verdict on `rc-4abad68c…`, evaluated against `801976e7…`

Window 2026-09-02T14:00:29Z → 14:03:09Z, one tick. Swing 0.090018 nats on all
four rows (chosen +0.084781, rival's own movement the remainder).

| rejected | rank 14:00 → 14:03 | margin → margin | rival's own ΔG | reading |
|---|---|---|---|---|
| M-expressions-of-interest | 2 → 1 | +0.004029 → −0.085989 | 0.005237 | **`:rival-improved-enough-on-its-own`** |
| M-distributed-frontiermath | 3 → 2 | +0.051983 → −0.038035 | 0.005237 | `:chosen-decayed` |
| M-symbol-grounding | 4 → 3 | +0.065287 → −0.024730 | 0.005237 | `:chosen-decayed` |
| M-superpod-mark2 | 5 → 4 | +0.068510 → −0.021508 | 0.005237 | `:chosen-decayed` |

One of four survives the attribution: M-expressions-of-interest improved by
0.005237 nats on its own, which exceeds the 0.004029 it was rejected by. So
**`:rationale-refuted`**, reason
`:a-rejected-candidate-closed-the-recorded-margin-on-its-own-movement` — and the
other three are correctly withheld. The test discriminates on the recorded
corpus; it does not fire on everything.

Controls: C3 (the detector does not fire on a record scored against itself),
C4 (it does fire on the 09-02 pair), C1 (a fabricated mission id appears in no
record and in no claim).

## 5. What the existing regret signal actually measures

`previous-selection-non-progress?` (`war_machine.clj:2312-2326`) and
`recent-non-progress-count` (`:2339-2356`) are the primitive regret signal the
row names. Measured over the ranking era:

| day | ticks | candidate rows | `:non-progress-decay` values | chosen action's own `:non-progress-count` | distinct missions selected | records with an outcome |
|---|---|---|---|---|---|---|
| 2026-08-30 | 7 | 1008 | `{1.0 910, 0.5 6, 0.333 1}` | `{0 7}` | 3 | 0 |
| 2026-08-31 | 5 | 720 | `{1.0 650, 0.5 3, 0.333 1, 0.25 1}` | `{0 3, 1 1, 2 1}` | 3 | 0 |
| 2026-09-01 | 79 | 11455 | `{1.0 10349, 0.5 79}` | `{0 79}` | **2** | 0 |
| 2026-09-02 | 3 | 438 | `{1.0 396, 0.5 3}` | `{0 3}` | 3 | 0 |

Two things follow, and both are structural rather than incidental.

**The count is forgiven by one intervening selection.** `recent-non-progress-count`
walks back at most 12 records and terminates at the first record whose action
names a different target (`:else count`). Measured on 2026-09-02:
M-wm-aif-policy-grain-compliance is selected at 13:47, carries
`:non-progress-count 1, :non-progress-decay 0.5` at 14:00 — and is back to
`0, 1.0` at 14:03, because one tick chose something else in between.

**Under alternation it never fires at all.** On 2026-09-01 the machine ran 79
ticks over exactly two missions in strict alternation. Of 11,455 candidate rows,
exactly 79 carry a decay — one per tick, always the immediately previous
selection — and the count reaches 2 on no row. The chosen action's own
`:non-progress-count` is 0 on all 79 ticks. A machine that alternates between two
missions forever accrues no regret by this signal.

That is the argument for the claim-keyed design in §3–4: a claim carries its own
id and window, so a retrospective on it is not defeated by what the machine
happened to select in between.

## 6. (c) The tension mint

The tension *record* is not minted here. `DESIGN-tensions-as-patterns.md`
section 3 already proposes it and worklist row U41 implements it; that note
already names `:tension/born-of :refuted-rationale` as one of its three sources
and cites U39 for it. This pass specifies only the **mint edge** — what a
refuted rationale contributes:

    {:mint/from        :refuted-rationale
     :mint/verdict-id  <the verdict's claim id>
     :tension/born-of  :refuted-rationale
     :tension/poles    ["select by the <lost-on> margin as scored this tick"
                        "select by a preference that survives to the next tick"]
     :tension/statement <one sentence, every number from the two records>
     :tension/carried-by <[:active-mission :mission-id] at claim time>
     :tension/resolution-path :none-named-at-mint
     :tension/status   :carried
     :tension/provenance {:claim … :verdict-against … :records […]
                          :minted-by … :written-to :nothing}
     :mint/record-shape-owner "DESIGN-tensions-as-patterns.md section 3 (U41 implements)"
     :mint/library-home {:carrier :flexiarg
                         :root "futon3/library (pattern_registry.clj:48-53)"
                         :addressed-by "pattern_registry.clj:158-161"
                         :status :named-not-written}}

**Only a REFUTED verdict mints.** `:rationale-untestable` mints nothing —
otherwise every tick would mint a tension about the non-progress decay, which is
one fact, not N.

**The library home is named, not written to.** The WM's own addressable pattern
artifact is a `.flexiarg` under `futon3/library` (1256 files there today), and
`pattern_registry.clj:158-161` is the function that resolves an id to one. That
is the futon4 pattern class the design note's birth rule graduates into. Nothing
is written to it by this row or by the mint: the birth rule
(`DESIGN-tensions-as-patterns.md` section 2) requires ≥2 typed receipts *and* a
person. The mint feeds the nursery, not the library.

### Worked example, from the §4 verdict

> M-zaif-harness-v1 was preferred over M-expressions-of-interest at
> 2026-09-02T14:00:29.357627605Z on a `G-risk` margin of 0.004028678 nats; one
> tick later M-expressions-of-interest out-ranked it, and its own G-core had
> fallen by 0.005237161 — enough to close that margin without any help from the
> chosen candidate's non-progress decay.

`:tension/carried-by` reads `M-wm-aif-policy-grain-compliance` and not
M-zaif-harness-v1, because the tick's durable clock is the *previous* tick's
selection (the U43 lag, `:choices :focus-clock-reconciliation`). Recorded as it
reads rather than corrected here; the mint takes whatever the clock says and the
provenance names both run-ids, so a reader can tell.

## 7. (d) The learning-feed rule, and its J-gate

**Verdicts accumulate as evidence. No verdict moves a live weight, ever, without
a J-gate.** Concretely:

1. A verdict is appended to a ledger under `runs/`. It is not written into any
   trace record, any registry `:choices` entry, or any store.
2. Nothing in the tick may read the ledger. The tick's ranking, temperature,
   admissibility verdicts and selector must be byte-identical whether the
   ledger exists or not — the same containment `mission_gauges.clj` states for
   its own observables ("`war-machine` merges `:observables` into the
   observation IT HANDS THE READBACK ONLY").
3. The three consumers are all offline: the Z3-style pre-registered A/B, arm C's
   playout precedent (`:choices :task-belief-actand-source` arm C
   `:cascade-catalog-playout`, U33), and Joe's review.
4. **The J-gate.** Any change that lets a verdict influence a live number — a
   weight, `non-progress-decay-k`, a τ feed, an admissibility rule — is a
   worklist `:J` row and Joe's call. This row does not mint that J row; it names
   the boundary the row would have to cross.

**The convergence, named as the row asks.** R14's selection gain
(`selection_gain.clj:40-70`) consumes a signed (expected, realized) pair and has
never had a foldable sample: `:samples` is 0 on all 125 records that carry the
state, and 0 of the 88 `:realized-outcome` records carry the keys the fold
requires. The upheld/refuted stream *is* a claimed-vs-verified stream in that
namespace's own vocabulary, at policy grain. **That is a convergence, not a
wiring proposal**: feeding it to R14 would be exactly the live-weight move rule 4
gates. It is recorded so the option is visible when someone rules on it.

## 8. What this pass does not claim

- No verdict rule is ruled on. `:verdict/verdict-rule :status
  :declared-not-ruled`, alternatives named.
- No shape is implemented on the live path. Nothing under `src/` or
  `scripts/futon2/` changed; the three shapes exist as projections in a
  read-only script.
- No retrospective is run over history beyond the one worked pair. That is
  worklist row U40 (`:blocked`), which asks for the three 2026-09-02 cases with
  their receipts and explicitly builds on this shape.
- The tension record is not implemented — that is U41.
- No number here is a prediction. The four legs are typed as measured or absent
  with the reason; three of four are absent on this corpus.

## 9. Review

**claude-2 review invited** (arm-C consumer side, per the row's acceptance):
the specific question is whether the verdict stream in §4, as shaped, is what
`:cascade-catalog-playout` would want as precedent data — in particular whether
the `:attribution` leg's two booleans survive at cascade grain, where the
"chosen candidate decays for having been chosen" mechanism may not exist.
