# U40 — the first retrospective, replayed over recorded history

claude (wm-build-loop), 2026-09-03. Row U40, class RUN, `:covers-key :none`.
Builds on U39 (`runs/U39-selection-retrospective/`), which designed the three
shapes this row uses and left the retrospective itself to this row.

Every number below comes from `u40_first_retrospective.bb`, run from the futon2
root as `bb holes/labs/wm-contract/u40_first_retrospective.bb`. Read-only: no
tick, no run lock, no substrate call, no network, nothing written under `data/`,
no weight or constant moved. The artifacts are `u40-measurements.edn` (typed),
`tension-records.edn` (the two minted records alone) and
`U40-FIRST-RETROSPECTIVE.txt` (the same census in prose); all three are
byte-identical on two consecutive runs.

**The shapes are U39's, and control C1 is what keeps them so.** U39 is
`:done-unreviewed`, so its producer is left untouched for its reviewer; the (a),
(b) and (c) projections are re-implemented here and C1 requires that they
reproduce U39's committed worked claim, its three measured legs and its verdict
exactly, and that the keys which differ are exactly the three this row declares
it changes. C1 passes: the claim is equal outright, the overtake, C_mis and
non-progress legs are equal outright, and the three differing keys are
`:verdict/legs` (the `:receipts` leg alone), `:verdict/verdict-rule` (an
`:owner` key naming U39) and `:verdict/basis`. Consolidating the two copies into
one file is follow-up work for whoever signs U39.

## 0. Before any rationale can be scored: did the ranking decide anything?

The row asks for three rationales to be scored. The first measurement this pass
made was whether the ranking that carries them is the thing that chose — and on
the recorded corpus it mostly is not.

| measured over the 94 ranking-era ticks | result |
|---|---|
| `[:decision :selected-policy-id]` | `stub:first-ranked-authorized-mission` 88, `pi-s-9dbc2ceb3317bc38050c41ce` 4, `stub:controller-head` 2 |
| enacted mission = controller rank 1 | **2 of 94** |
| enacted mission ∈ a hardcoded three-element set | **92 of 94** |
| `[… :strategic-memory :counterfactuals :scheduler-habit]` = the controller order restricted to those three | **94 of 94** |
| enacted mission = the head of that three-element list | 88 of 94 |

The set is literal in the source: `strategic-candidate-ids` at
`scripts/futon2/report/war_machine.clj:6235-6238` is
`#{"M-aif-policy-conditioned-eig" "M-shared-memory-control-build-test"
"M-wm-aif-policy-grain-compliance"}`. `wm-admissible` is filtered to it
(`:6239-6245`), that list and the controller ranking are handed to
`invoke-strategic-selection` (`:6246-6251`), and the enacted action is the first
admissible action matching the returned `:selected-mission-ids` (`:6253-6260`).
The set has stood since futon2 `fa61e98` (2026-07-24, `git log -S
strategic-candidate-ids`).

So on 92 of 94 ticks the controller computed 146 candidates with a seven-term G
decomposition each, recorded all of it, and did not decide. **A rationale
recorded on one of those ticks is a rationale for a choice that was not made.**
The two exceptions are the 14:00 and 14:03 records of 2026-09-02, after the
selector id changes to `stub:controller-head`; both carry
`:git-dirty? true`, so the code that produced them matches no committed sha.

This is not a finding about the three missions. It is the reason case 1 reads
the way it does, and it bounds what any retrospective on this corpus can say.

## 1. Case 1 — run `0a18c4f7`, 2026-09-02T13:47:22Z

**The row's premise, checked and corroborated.** The controller ranked
M-zaif-harness-v1 first and M-expressions-of-interest second. The enacted
mission was M-wm-aif-policy-grain-compliance, at **controller rank 124 of 146**.
The record's own `:selection-law` says `:chosen-rank 1,
:moved-from-controller-head? false`. The recorded scheduler-habit list is
`["M-wm-aif-policy-grain-compliance" "M-shared-memory-control-build-test"
"M-aif-policy-conditioned-eig"]` and the enacted mission is its **head** —
so "the stub took the scheduler-habit head" is exactly right, with
`[:decision :strategic-memory :counterfactuals :scheduler-habit]` as the pointer
and `war_machine.clj:6239-6245` as the producer.

**One correction, code-backed, recorded and not ruled.** Two different channels
are called *scheduler-habit* on this record and they disagree:

| field | produced by | basis | its head |
|---|---|---|---|
| `[:decision :counterfactual]` (`:kind :scheduler-habit-authoritative`) | `src/futon2/aif/policy.clj:646` | `:habit-prior-bias` | **M-learning-loop** |
| `[… :strategic-memory :counterfactuals :scheduler-habit]` | `war_machine.clj:6239-6245` | controller order restricted to the hardcoded three | **M-wm-aif-policy-grain-compliance** |

The enacted mission is rank 125 of the habit-adjusted ranking the first channel
orders. The row's phrase is true of the second channel and false of the first.
Recorded as a measured collision of one name over two channels; no `:decisions`
entry is written by this row.

**The claim, and the verdict.** `:claim/soundness-at-mint` is
`{:status :unsound, :reason :law-field-contradicts-ranking}` — the projection
refuses to mint, exactly as U39 specified, and the corroboration from the trace
matches the false stamp already recorded at
`holes/labs/zaif-harness/runs/S4-identify-ingest.edn:40`. Verdict
**`:rationale-untestable`**, reason `:claim-unsound-at-mint`. **No tension is
minted**: U39 section 6 mints only on a refuted verdict, and a claim refused at
mint is not scored.

**What the machine would have had to know to choose differently: nothing about
the missions.** M-zaif-harness-v1 is not in `strategic-candidate-ids`, so no G
value would have selected it through that selector. What would have had to
change is the candidate set or the selector, and both are code.

## 1b. The 14:00 record — run `4abad68c`, U39's worked example

The middle of the three 2026-09-02 rationales, re-derived here (this is the C1
pin) and scored because the row asks for the day's rationales. M-zaif-harness-v1
chosen at controller rank 1; M-expressions-of-interest rejected at rank 2 on a
`G-risk` margin of 0.004028678 nats. One tick later M-eoi out-ranks it, and
M-eoi's own G-core has fallen by 0.005237161 — more than the margin it lost by.
Verdict **`:rationale-refuted`**, reason
`:a-rejected-candidate-closed-the-recorded-margin-on-its-own-movement`.
**Tension record 1 of 2 mints here**, through the U39 shape.

## 2. Case 2 — run `801976e7`, 2026-09-02T14:03:09Z

M-expressions-of-interest at controller rank 1; M-zaif-harness-v1 rejected at
rank 5 on a `G-risk` margin of 0.085989 nats. This is one of only two ticks in
the corpus where the controller's preference was the enacted choice, so it is
the first rationale on record that is scoreable at all.

**Under the declared rule it is not scoreable yet.** U39's rule scores a claim
against the *next* ranking record; `801976e7` is the last record the corpus
carries, and the machine has not ticked since. Verdict
**`:rationale-untestable`**, reason `:no-later-ranking-record-in-corpus`. No
mint. What the machine would have had to know is one more tick.

**The leg that is measurable is the one U39 typed absent.** U39 typed receipts
`:no-mission-to-receipt-carrier` because no *trace field* joins a receipt to a
mission. The row names a carrier that is not a trace field — the boards'
ledgers — and one of them declares the mission it serves in its own `:mission`
key, which is a declared binding and not an inference.

24h window, 2026-09-02T14:03:09Z → 2026-09-03T14:03:09Z, ledgers read at the
last commit before each boundary:

| carrier | declares | rows minted | rows reaching reviewed `:done` | ledger commits |
|---|---|---|---|---|
| `holes/labs/zaif-harness/worklist.edn` | `futon2/holes/missions/M-zaif-harness-v1.md` | 27 | **27** | 109 |
| `holes/labs/wm-contract/worklist.edn` | *(no `:mission` key; its `:source` is TN-edge-review)* | 34 | 21 | 68 |

M-expressions-of-interest has **no declared board**. Its document is
`futon5a/holes/missions/M-expressions-of-interest.md`
(`futon2/holes/core-mission-gaps.edn:607-609`), and that repository took **0
commits in the window** — its last commit is `e1a8c52`, 2026-08-26T17:10:22Z,
seven days before the selection. The gauge census the row calls "the criteria"
(U28, `C494-u28-eoi-gauge-census.md`) landed at futon2 `6ca58f3`,
2026-09-03T16:51Z — **2h48m after the window closed**, and it found all six
criteria bind nothing.

**The row's "30+", checked.** 27 under the declared-carrier rule. 45 if the 18
wm-contract rows that mention *zaif* somewhere in their own text are added — but
that is a lexical reading and is typed as one (`:reading
:lexical-not-declared`); 4 of the newly-done wm rows mention M-eoi and all 4 of
them also mention zaif, so 0 rows mention M-eoi alone. The row's figure is
reproduced only under the lexical reading.

**The free hand, named and not exercised.** Whether a receipts asymmetry may
decide a verdict is not decided here. U39's rule has no receipts leg; this row
measures the leg, types it `:measured-outside-the-trace`, and writes no ruling.
`aif-equations.edn :choices :selection-retrospective` is left exactly as U39
registered it.

## 3. Case 3 — the non-progress decay events

**94 decay events over 94 ticks, one per tick**, 93 of them on the mission the
machine had selected on the previous tick (the exception is the corpus's first
tick, which has no previous). Values `{0.5 91, 0.333 2, 0.25 1}`.

A decay event is not a rejection rationale, so U39's rule does not reach it. The
rule below is **declared here in the same shape and with the same status** —
`:declared-not-ruled`, no tunable scalar — and `:choices` is not written:

> A decay event asserts that the mission selected on the previous tick made no
> grounded change and is worth less for it. **REFUTED** iff the machine itself
> re-selects that mission on the very next tick **and** every recorded field that
> changed on the mission in between belongs to the decay mechanism — so the
> reversal is the decay's own expiry and nothing learned about the mission.
> **UNTESTABLE** where the corpus carries no reading of the mission's grounded
> change either way. **UPHELD** requires a receipt showing no change in the decay
> window on a declared carrier.

Alternatives named and not taken: *decay-is-a-rotation-schedule* (read it as a
rotation policy that asserts nothing, in which case nothing can refute it) —
not taken because the field is named `:non-progress?` and is computed by
`previous-selection-non-progress?` (`war_machine.clj:2312-2326`), a predicate
about change; *receipts-decide* — not taken because the recorded decay windows
are 3 and 13 minutes wide and only one of the three missions the corpus selects
has a declared carrier.

**Measured:** 93 of the 94 events have a next tick. On **85** of those 93 the
machine re-selected the decayed mission on that very next tick. On **85 of those
85**, the only recorded fields that changed on the mission in between were the
decay mechanism's own — `:non-progress-decay`, `:mission-value-factor`,
`:non-progress-count`, `:non-progress?` (84 events change all four, 1 changes
three). Nothing else about the mission moved.

The 8 events not followed by re-selection are listed in the artifact and are not
counterexamples to the mechanism: 6 fall on 2026-08-30 and 08-31, the two days
on which the machine cycled over three missions rather than alternating between
two, and 2 fall on 2026-09-02 at 13:47 and 14:00, the ticks across the selector
change. One of the six is the sharper case — 2026-08-31T14:30, where the decayed
mission was chosen *on that same tick*, so the decay did not deter at all.

**Its own test has no readable input.** The assertion's `:grounded-change` arm
(`war_machine.clj:2321-2326`) reads the same three places `trace-outcome`
(`:2328-2332`) looks, and **0 of the 94
ranking-era records** — 0 of all 885, per U39 §1 — reach any of the three places
it looks. The machine asserted non-progress 94 times with no capacity to observe
progress.

Verdict **`:rationale-refuted`**, reason
`:every-recorded-reversal-is-the-decays-own-expiry-and-nothing-else`. **Tension
record 2 of 2 mints here** — one record for the family, not 94, per U39 §6:
a decay refutation is one fact.

**What the machine would have had to know: whether anything had happened.** What
would have had to change is a producer writing an outcome onto the tick record.

## 4. The two tension records

`tension-records.edn`. Both carry `:tension/born-of :refuted-rationale`, poles,
a statement whose every number is from records, provenance naming the records,
and `:written-to :nothing`. The record *shape* is
`DESIGN-tensions-as-patterns.md` section 3 and row U41's to implement; the
library home (`futon3/library`, a `.flexiarg`, `pattern_registry.clj:48-53`,
resolved at `:158-161`) is **named, not written to** — the birth rule needs two
typed receipts and a person.

1. **From the 14:00 claim.** *"select by the G-risk margin as scored this tick"*
   vs *"select by a preference that survives to the next tick"*. Carried by
   M-wm-aif-policy-grain-compliance, because the tick's durable clock is the
   previous tick's selection (the U43 lag); the provenance names both run-ids so
   a reader can tell.
2. **From the decay family.** *"penalise the mission just selected, so attention
   rotates off it"* vs *"return to the mission just selected, because nothing
   recorded about it changed"*. Carried by `:the-selection-mechanism-itself`.

## 5. The summary for Joe, typed

| case | verdict | what the machine would have had to know | what would have had to change |
|---|---|---|---|
| 1 — `0a18c4f7` | `:rationale-untestable` (`:claim-unsound-at-mint`) | **nothing about the missions** — the preferred candidate was not in the selector's candidate set | the candidate set or the selector, `war_machine.clj:6235-6238` — both are code |
| 2 — `801976e7` | `:rationale-untestable` (`:no-later-ranking-record-in-corpus`) | **one more tick** — the declared rule scores against the next ranking record and the machine has not ticked since | nothing about the rule; the corpus has to grow |
| 3 — decay family | `:rationale-refuted` | **whether anything had happened** — the assertion's own test reads a field no record carries | a producer writing an outcome onto the tick record |
| across all three | — | **that the ranking was not the thing choosing** — 92 of 94 ticks | the enactment path |

## 6. Controls

| control | asks | result |
|---|---|---|
| C1 U39 shape pin | the (a)/(b) projections reproduce U39's committed claim, measured legs and verdict, differing only where declared | PASS |
| C2 margin identity | every `:margin/total` equals the recorded controller-score difference on the claim's own record | PASS, max abs delta 0.0 over 11 margins across 3 claims |
| C3 fabricated subject | a mission id in no record yields no claim, no verdict, no decay event | PASS |
| C4 decay rule negative | the reversal leg counts only events where the decayed mission is re-selected on the very next tick | PASS, 8 non-re-selected events counted by nothing |
| C5 case 1 premise | the record contradicts itself on its own ranking | PASS, law says rank 1, ranking says 124 |

## 7. What this row did not do

No ruling: `aif-equations.edn :choices` and `control-map-edges.edn :decisions`
are untouched. No live tick, no run lock, no substrate call, no network. Nothing
under `src/` or `scripts/futon2/` changed; no weight, threshold or decay constant
moved. No tension record written to `futon3/library`. `gen_aif_dag.bb` not run
into a publish (TN §9a).
