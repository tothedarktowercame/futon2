# C493 — U24: the `:survey-mission` epistemic action, built and measured

claude (wm-edge worklist loop, any-lane), 2026-09-03. Row `:U24`, ported from
zaif-harness `:S6` half (b); Joe's 2026-09-02 rulings 3b (the epistemic-action
hole) and 3c (the catalog design) in
`futon4/holes/mission-lifecycle-wm-alignment.md`.

Artifacts: `runs/U24-survey-mission/` (README + seven EDN files),
`futon2/src/futon2/aif/survey_mission_value.clj`,
`futon2/test/futon2/aif/survey_mission_value_test.clj`, the `:survey-eig` leg in
`futon2/src/futon2/aif/efe.clj`, the addressability arms and the enrichment in
`futon2/src/futon2/aif/portfolio_action_proposer.clj`, and the registry entry
`aif-equations.edn :choices :survey-mission-epistemic-action`.

No ruling is made here. The registry entry records the form the code now
computes and the fiats it rests on; the default flip is worklist row `:J8`.

## 1. What already existed, and what it was missing

`:survey-mission` has been a declared action type since the portfolio proposer
landed. Nothing about it was epistemic:

- `predict-effects :survey-mission` delegates to `:survey`
  (`forward_model.clj:180-187`), which predicts an **empty** obs-delta and a
  variance on one channel. It does not read `:target`.
- The candidate carried a flat `:weight 0.3` and a rationale string
  (`portfolio_action_proposer.clj:50-60`).
- `can-propose? :survey-mission` fell through to the `:default` arm (false), so
  the bootstrap proposer emitted a `:learn-action-class` gap for it instead. On
  the 2026-09-02 live tick that gap candidate is in the field, carrying
  `:G-ambiguity 0.25` — the Beta-predictive-variance collision C487 §5 measured,
  125 nats away from the entropy scale the other candidates are on.

So the machine's only relationship to surveying a mission was a candidate saying
"I cannot survey missions."

## 2. The derivation (DERIVE discipline)

**IF** a survey action is to be an epistemic action in the AIF sense, its
preference has to come from what it would tell the judge, and that has to be a
policy-conditioned expected information gain — not a spread and not a gap
lookup, both of which `epistemic_value.clj:9-13` refuses by name.

**HOWEVER** the candidate has nowhere to put it. `:G-core` is pinned to
`risk + ambiguity` (invariant I3, `efe.clj:548-556`), and neither leg can carry
an information-gain term without breaking that. And the forward model gives a
survey candidate no per-target signal at all.

**THEN** the second problem answers the first. Because `predict-effects` ignores
`:target`, all 133 survey candidates on the staged field share **one** `:G-core`
(`04-arms.edn`: `:distinct-G-core 1`, `:distinct-survey-eig-nats 7`). So every
per-target difference between two survey candidates must come from an added leg,
and the leg goes in the augmentation layer beside the eight that are already
there, subtracted:

    survey-eig(M) = survey-availability(phase(M)) * SUM_q EIG(q)     [nats]
    G(survey-mission M) = G-core(survey) - survey-eig-weight * survey-eig(M)

Subtracted because information gain is preference-increasing — the sign
`Holes.G := risk − eig` gives it, which is the Lean form TN §2 records beside the
glossary's `risk + ambiguity`.

**BECAUSE** this makes "G is dominated by information gain" an identity rather
than a hope. It is not a claim about magnitude: at the declared weight the leg is
0.152 of a score of 5.302. It is the claim that G-core supplies **no** target
information for this action type, so the epistemic term is the whole of the
per-target variation.

**Nats are not rescaled**, and that is a deliberate difference from U22. There
the epistemic term had to blend additively with three dimensionless exploit
factors, so it was normalized by a constant reference into [0,1]. Here the leg
enters G, and `G-risk` and `G-ambiguity` are already in nats, so the quantity is
subtracted in its own units and the exchange rate is the one declared scalar.
`:normalized` is still reported beside the nats so the two rows' numbers are
comparable; nothing reads it.

**`phase-survey-availability` is reused, not re-declared.** It is U22's table
(`mission_epistemic_value.clj:130-145`), and it is the right gate for a survey
action for the reason it was the right gate for the mission-value term: it says
whether the phase produces field readings at all. A VERIFY-phase mission's survey
earns 0.0 nats by the table rather than by a special case. A test fails if a
second copy of that table appears.

## 3. What is measured and what is declared

**MEASURED (a): the mission's unanswered MAP questions.** The carrier is the
numbered list under a standalone `MAP must answer:` line — the kind C492 §4c
named. Over the 133 candidates, **one doc carries it**
(`futon0/holes/missions/M-apm-capability-ratchet.md:260`, six questions). Four
other primary docs contain `must answer` in constructions that are not a MAP
question list — `Questions the evidence bundle must answer:`
(futon3c `M-agency-hardening.md:196`), `Questions the audit must answer:`
(futon5a `M-stack-stereolithography.md:1323`), and two prose/table uses in
futon3c `M-apm-demonstration.md:15,278`. They are recorded in
`near-miss-phrasings` with pointers and deliberately not matched; a test feeds
each one to the parser and fails if it matches. Widening the regex is the exact
move U22's first run had to undo.

**No per-question answer carrier exists.** M-apm-capability-ratchet's own
`MAP findings` sections number `Finding 1..n` against no question id, so nothing
joins a finding to the question it answers. Every listed question is therefore
open, with basis `{:reading :absent :reason :no-per-question-answer-carrier
:doc-line …}` — an absence with a pointer, not an assumption that the questions
are unanswered.

**MEASURED (b): the kin-catalog gap**, from U23's reader. U23 declared an
eleven-carrier cascade record and read all eleven for three subjects. Six cannot
be answered for **any** mission on this field, and U23's own absence reasons say
why: `:psr` and `:pur` are `:no-typed-carrier`; `:pattern-phylogeny` and
`:apm-frames` are `:records-exist-not-keyed-by-mission`;
`:cross-mission-references` is `:subject-absent-from-carrier-key-space`;
`:flight-discharge` is `:writer-exists-no-records`. Those six are excluded and
counted, on the same principle as the near misses. The five that remain
(`:clocked-on`, `:trace-decision`, `:trace-shown`, `:held-on-mission`,
`:shares-capability-with`) are mission-keyed and populated corpus-wide, so a
survey returns either a reading or a typed zero — which is what makes the latent
readable.

A carrier the catalog already read is settled and worth 0. So the three missions
U23 surveyed score 0 on this half, **including M-zaif-harness-v1** — the mission
the machine wants is the least informative survey target on the catalog half,
precisely because it has already been surveyed.

**DECLARED, four fiats, three of them U22's reused:** the open latent at 0.5, the
perfect-observation model, `phase-survey-availability`, and — the only new one —
the partition of U23's eleven carriers into five answerable and six not. That
partition is not a judgement about importance; it is whether a survey could
return a reading at all, and each entry carries U23's own reason and corpus count
so it is auditable against `runs/U23-cascade-catalog/carrier-population.edn`
rather than asserted.

**Every question goes through one kernel:**
`mission-epistemic-value/latent-eig` → `epistemic-value/expected-information-gain`
with its Bayes-coherence gate. A second EIG implementation here would be
`epistemic_value.clj:9-13`'s refusal evaded rather than honoured.

## 4. The finding this row did not go looking for

**A `:survey-mission` candidate outranks an `:advance-mission` candidate on
declared determinism, not on information gain.**

All 36 real IDENTIFY-phase candidates on the staged field: the survey option
outranks its own recorded advance option **with the epistemic leg off**, 36 of 36
(`06-planted-identify-demo.edn :d0-real-identify-phase-missions`). The reason is
the forward model. `predict-effects :survey` declares a variance on
`:mission-health` and none on the other thirteen channels, so those thirteen take
the 1e-9 floor (`efe.clj:56-59`) and contribute −8.9435 nats each; the ambiguity
term comes out **7.915207 nats** better than `:advance-mission`'s, which is
exactly one channel's worth of declared variance and exactly the figure C487 §5
measured on the `:fire-pattern` / `:advance-mission` pair.

Under `G = risk + ambiguity` with lower preferred, an action model that declares
it knows what will happen is rewarded for saying so. That is upstream of anything
this row builds and is not this row's to fix — it is the same coverage gap C487
§6 landed on from the C-vector side ("ambiguity can only speak about the three
coordinates the world is not moving in"). What this row owes is not to let the
demonstration ride on it, which is why the planted demonstration is built against
an advance candidate that beats survey at G-core. Recorded as a finding, not a
ruling; it looks like a row of its own.

## 5. The demonstrations and the U4 re-run

Full numbers and controls in `runs/U24-survey-mission/README.md`. In brief:

- **Controls exact.** The staged field reproduces the 2026-09-02 record's own
  `:G-risk`, `:G-ambiguity` and `:controller-score` for all 146 candidates
  (max Δ `0.000e+00`) and its own `:decision` through `policy/select-action`.
- **Selectable.** At `survey-eig-weight` 0.02 the field's decision is
  `:survey-mission M-apm-capability-ratchet` — the one mission with a listed MAP
  question set. The break point is solved, not searched: **w_break = 0.004808**.
- **Planted IDENTIFY demonstration:** `w_break = 0.018172`, and at 0.02
  `select-action` returns the planted mission's survey over its own advance.
- **U4 re-run, and the answer is no.** Ambiguity does not overturn the EFE-best
  candidate on either arm. **λ_break falls from 1.024081 to 1.004631** — from
  2.41% of its own size short of flipping the winner to 0.46% short — and the
  candidate that would overtake changes from `:no-op` to a survey candidate.
  C487's positive control (drop risk instead) fires on both arms, so the zero is
  a measurement and not a broken detector. The ambiguity term's **value set is
  unchanged**: five distinct values before and after, survey candidates landing
  at −116.79209997800035, the level `:fire-pattern` already occupied. The family
  adds mass at an existing level, not a new level.

## 6. Default off, twice over

- `:survey-mission` is not proposable unless
  `portfolio-action-proposer/*portfolio-proposer-active?*` is true, which is
  false by default and is not wired into `war_machine.clj` at all.
- `efe/default-survey-eig-weight` is **0.0**, at which the `:survey-eig` key is
  absent from `:augmentation-terms` altogether and `:controller-score` is
  byte-identical. Measured on the staged field: 0 of 278 candidates carry the
  key, max Δ score `0.000e+00`.
- A candidate carrying **no** payload is skipped rather than imputed 0.0 nats, so
  an unmeasured candidate never records a measured-looking zero.
- The `can-execute? :survey-mission` arm is the one answer that moves from the
  `:default` arm (true → false while dark). It is strictly more conservative and
  unreachable while dark, because nothing proposes the type for it to be asked
  about.

Flipping either default is Joe's: `:J8`.

## 7. Discharge

`survey-mission-value/discharge-shape` writes the row's "MAP answers as typed
observations at mission grain" as data, `:status :declared-not-implemented`, with
what blocks it: U23 measured the flight-discharge carrier as
`:writer-exists-no-records` — the writer is reachable
(`futon3c/src/futon3c/aif/flight_record.clj:347`, called at
`war_machine_pilot.clj:585`) and there are zero `*.flight.edn` files anywhere
under `~/code`. No flight record is written by this row and none exists to write
against.

## 8. Gates

`clj-kondo` 0 errors / 0 warnings on each touched file individually (the one
warning `efe.clj` reports, `Unused private var futon2.aif.efe/ambiguity`, is
present at HEAD before this change — it is the fn C487's sweep reaches through
`#'efe/ambiguity`). `futon4/dev/check-parens.el` OK on all five. Tests:
`futon2.aif.survey-mission-value-test` 23 tests / 88 assertions, and
0 failures / 0 errors across `futon2.aif.efe-test`,
`futon2.aif.efe-struct-split-test`, `futon2.aif.forward-model-test`,
`futon2.aif.policy-test`, `futon2.aif.action-proposer-test`,
`futon2.aif.mission-epistemic-value-test` and `futon2.report.war-machine-test`.
`negative_controls.sh`, `pointer_check.bb` and `worklist_check.bb` run before the
commit. `gen_aif_dag.bb` deliberately NOT regenerated (TN §9a gate rule).

## 9. What this does not claim

- **No ruling.** The registry entry is `:status :observed-not-decided`.
- **No flight flew.** Nothing was enacted, no trace was appended, no run lock was
  taken, nothing was written under `data/`.
- **The staged decision is not the enacted decision.** Control B's replay omits
  the record's own further `:selection-boundary :reason-bearing-strategic-policy`
  filter; what is claimed is that the two agree on this tick.
- **"Dominated by information gain" is about where the variation lives**, not
  about magnitude. At the declared weight the leg is 0.152 of a 5.302 score.
- **No weight reorders two survey candidates.** They share one G-core, so their
  order is the nats order at every weight; the weight decides only whether
  surveying beats advancing.
- **The MAP-question half is one doc.** On this field the term is, for 132 of 133
  candidates, the kin-catalog gap masked by the availability table — the same
  honest shape C492 §4c reported for U22 ("close to a staleness ordering with a
  phase mask").
