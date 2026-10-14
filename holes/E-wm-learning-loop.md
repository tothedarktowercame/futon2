# E-wm-learning-loop — close outcome learning around outer target selection

**Date:** 2026-10-14  
**Status:** DERIVE — investigation complete; implementation packets open.  
**Driver:** War Machine. **Operator:** Joe. **Repository:** `futon2`.  
**Relates:** [[E-outer-loop-improvement]] · [[E-close-the-loop]] ·
`src/futon2/aif/meta_live_outer_selector.clj` ·
`src/futon2/aif/meta_outcome_dataset.clj` ·
`src/futon2/aif/meta_preference_authority.clj`.

## HEAD

The War Machine records outcomes and contains several local learning mechanisms,
but the live outer target selector does not consume learned outcome state. The
loop currently has the shape

```text
select target → construct cascade → enact → observe → retain records
       ↑                                                   │
       └──────────── no production learning edge ──────────┘
```

This was demonstrated after run
`2026-10-14-6a64c83f-4732-4af1-b286-561f22109667` made grounded progress on
`M-interim-director-proxy-metric-inventory`. A fresh production-equivalent,
read-only selector preview immediately selected the same target again:

- 106 supported tasks and 588 exclusions;
- rank 1 with zero pairwise losses;
- the same sole effective channel, pipeline structural centrality;
- 96 structural occurrences, hence cost `1 / (1 + 96) = 0.010309278…`;
- no declared-priority or pipeline-freshness value for the selected row.

The run was remembered as evidence, but did not change the outer policy's state.
This excursion plans an AIF-theory-compliant upgrade: observed consequences of
target selection become a pinned posterior state that the next outer decision
actually consumes. The repair is not an arbitrary anti-repeat rule. It is the
missing perception–learning–selection edge.

## 1. Investigation findings

### 1.1 One run produced three distinct, valid descriptions

The successful run recorded:

1. **Terminal outcome:** `:grounded-progress`, because commit
   `278cae92dc1191a129b57a2943df4bae06b9034e` produced a real Arm A processing
   artifact.
2. **Selected-want measurement:** all four selected mission wants remained
   `:untouched` after the commit.
3. **Pattern feedback:** every selected pattern was classified
   `:no-want-effect`, with no positive reinforcement.

These statements need not conflict. Grounded collateral progress can be real
without closing or advancing the selected checkbox surface. The defect is that
no target-level learner represents this distinction for the next outer choice.

### 1.2 The live outer selector is memoryless with respect to outcomes

The current `:meta-live-pipeline-task-state` policy consumes:

- pipeline structural centrality;
- ticket mtime freshness, when available;
- declared priority, when available;
- automated feasibility;
- ownership and last-touch observations.

It consumes no prior target outcome, repetition history, achieved progress,
failure history, elapsed cost, token cost, or target posterior. Pairwise ranking
uses only channels shared by both candidates. A target missing freshness and
priority can therefore defeat richer candidates using centrality alone.

Ownership and last-touch are observed but do not alter the ranking. Continuous
CLI mode detects consecutive repetition only after selection and stops the
loop; it does not teach the selector or choose a different target.

### 1.3 The intended META learning pieces are disconnected scaffolding

`futon2.aif.meta-outcome-dataset` can reconstruct from pinned run records:

- selected target and task kind;
- terminal outcome class;
- chronology and prior occurrence count;
- elapsed wall time and token use;
- evidence quality and exclusions.

`futon2.aif.meta-preference-authority` can read an operator-authorized
preference declaration and normalize outcome/resource observations. Neither
namespace has a production consumer. Outside their own tests they are not
required by the live selection path.

### 1.4 Existing learners operate at different grains

The implementation contains learning or accumulation at several narrower
grains:

| mechanism | grain | state observed in the demonstration | outer effect |
|---|---|---|---|
| cascade feedback | pattern application | durable event recorded; all four patterns had no selected-want effect | none |
| enactment habit | pattern cascade | fold read seven flight files and obtained zero usable receipts/samples | inner policy only |
| strategic habit | first acting pattern | no retained state on this run | no outer-target effect |
| learning-trial ledger | token transition/theta | store had not advanced since 2026-09-23 | no outer-target effect |
| scan learning | scan-channel outcome | not retained on this run record | no outer-target effect |

Even when these mechanisms work, they rank patterns, cascades, transition
parameters, or scan models after a target has been chosen. None can dislodge an
outer target selected from static graph centrality.

### 1.5 Progress does not currently alter the task representation

The grounded artifact did not close a selected checkbox, change the target's
96 graph occurrences, or write a target-state transition consumed by the
selector. The target remains open and the next selector sees essentially the
same state. “Grounded progress” is therefore a terminal report class, not yet a
state transition in the outer controller.

## 2. AIF account of the missing loop

The outer decision is its own AIF policy grain. It must remain separate from
the inner tactical cascade grain.

### 2.1 State

Let outer state `sᵗ` contain, for every currently enumerated M/E/T target:

- current standing: open, closed, blocked, or conflicted;
- declared priority and structural observations;
- ownership and actionable support;
- most recent selected-want observation;
- grounded collateral-progress observations;
- recent typed failures and blockers;
- accumulated elapsed/token cost;
- selection and outcome chronology;
- posterior sufficient statistics, with their evidence pins.

The state is not an unbounded dump of run history. It is a deterministic fold
whose sufficient statistics can be replayed from an externally pinned manifest.
The fold retains its manifest and exclusions so every posterior value is
auditable.

### 2.2 Actions and policies

An outer action is exactly one supported task identity plus action kind, for
example `{:type :advance-mission :target "M-…"}`. Tactical patterns,
precedences, constructed cascades, and tactical G remain forbidden inputs until
after this action has been selected.

An outer policy is a distribution or deterministic ordering over the complete
supported task population. Its receipt must bind every score to the task
identity and the state revision it consumed.

### 2.3 Observations

The post-enactment observation vector must preserve distinct facts rather than
collapse them into one success boolean:

```clojure
{:target                         "M-…"
 :terminal-class                 :closure | :grounded-progress |
                                 :typed-blocker | :abstention-or-failure
 :selected-wants                 {:reached n :progressed n :blocked n
                                  :untouched n}
 :grounded-artifact              {:status :present|:absent :repo … :commit …}
 :collateral-progress            {:status :present|:absent :kind … :evidence …}
 :resource-use                   {:wall-clock-ms n :tokens n}
 :failure                        {:status :present|:absent :kind … :stage …}
 :operator-observation           {:status :present|:absent :kind … :source …}}
```

Operator turns enter only through an explicit typed observation with a source
and interpretation rule. Silence is absence, not approval; an instruction is
not silently converted into reward.

### 2.4 Generative model and prediction error

For each candidate target, the outer model predicts a distribution over the
declared observation vocabulary, conditioned on current target state and the
resource envelope. At minimum the model distinguishes:

- closure;
- selected-want progress;
- grounded collateral progress;
- no measured progress;
- typed blocker/failure;
- time and token bands.

The realized observation updates only the chosen target's likelihood evidence.
Unchosen targets provide no counterfactual outcome. Prediction error is retained
as a receipt, not inferred from whether the operator liked the result.

The first production model may be a transparent conjugate categorical model
with explicit priors. It need not begin with a complex learned model. It must,
however, update from outcomes and feed the next decision.

### 2.5 Preferences and expected free energy

The operator-authorized preference authority supplies `C`, including the
relative preference for closure, selected-want progress, grounded collateral
progress, blockers/failures, and normalized resource use. Empirical outcomes
update beliefs about consequences; they do not silently rewrite preferences.

For candidate policy `π`, the outer selector records the familiar decomposition:

```text
G(π) = risk(π) + ambiguity(π) − expected_information_gain(π)
```

- **Risk** compares predicted outcomes with authorized preferences.
- **Ambiguity** records uncertainty in the observation model.
- **Expected information gain** rewards a choice only when its possible
  observation is expected to reduce relevant target-state uncertainty.

Recent progress should affect the next selection through the posterior and
predicted marginal outcome, not through an unexplained diversity bonus.
Selection cadence may appear as an explicit opportunity/resource cost, but must
not masquerade as learned task value.

### 2.6 Habits

Habit `E(π)` may encode enacted outer-task frequency only after the outer action
grain is established. It must be reported separately from G and must not turn
frequent historical selection into evidence that a target is currently useful.
A cadence or anti-monopoly policy belongs in an explicit controller term unless
Joe separately authorizes an outer habit prior.

## 3. Required invariants

1. **One grain per learner.** Outer target learning never consumes tactical
   pattern identity; pattern learning never claims to update target value.
2. **Observation before update.** No posterior update occurs from selection,
   author claims, or predicted effects alone.
3. **Preferences are authorized, not learned accidentally.** Outcome frequency
   changes likelihoods, not the desired-outcome ordering.
4. **Missing is not zero.** Missing outcome, cost, or selected-want evidence
   produces a typed absence or excludes the update.
5. **Only chosen-target likelihood updates.** No synthetic negative evidence is
   assigned to unchosen targets.
6. **Collateral progress stays distinct.** A useful artifact can improve a
   target's state without being called selected-want success.
7. **No self-certification.** The terminal label alone cannot warrant its own
   learning update; grounded artifact and after-state evidence must bind it.
8. **Replay determinism.** The same pinned manifest and authority produce the
   same folded state and selection inputs.
9. **The posterior is consumed.** A learned state that is merely retained or
   displayed does not satisfy this excursion.
10. **Every switched-off learner names its re-arm condition.** Disabled,
    shadow-only, or accumulation-only mechanisms remain visible in the run
    certificate.

## 4. Implementation plan

### Packet A — establish the authoritative outer observation fold

- [ ] Promote the useful projection in `meta-outcome-dataset` into a strict
  production reader over an explicitly pinned run-record manifest.
- [ ] Add selected-want counts, grounded artifact evidence, typed failure, and
  collateral-progress evidence to each row.
- [ ] Refuse duplicate run/click identities, source drift, missing selected
  identity, and outcome claims lacking their required evidence.
- [ ] Define deterministic chronology and target-local sufficient statistics.
- [ ] Record exclusions rather than silently dropping legacy or incomplete runs.

**Acceptance:** replaying the manifest containing the 2026-10-14 run yields one
row for `M-interim-director-proxy-metric-inventory` with terminal class
`:grounded-progress`, four untouched selected wants, a pinned grounded artifact,
and measured time/token use.

### Packet B — define the outer target posterior

- [ ] Introduce a versioned `:wm/outer-target-belief-v1` state.
- [ ] Start with an explicit operator-authorized prior over the observation
  vocabulary; never derive preferences from frequencies.
- [ ] Update the selected target from each admissible observation row.
- [ ] Preserve counts, posterior parameters, last observation, accumulated
  resource use, and source pins.
- [ ] Make duplicate replay idempotent and conflicting replay a refusal.

**Acceptance:** a controlled observation changes the chosen target's posterior;
an identical replay does not; an unchosen target is byte-identical before and
after.

### Packet C — repair the success/learning vocabulary seam

- [ ] Define `:grounded-progress` as a structured class with an explicit
  collateral-progress subtype and evidence, not just a terminal keyword.
- [ ] Keep selected-want progress as a separate observation.
- [ ] Require report cards to display both dimensions together.
- [ ] Refuse “grounded progress” from becoming pattern reinforcement when no
  selected want moved; retain the current correct no-want-effect behavior.
- [ ] Decide how collateral progress predicts future selected-want progress and
  encode that as a model assumption with an authority, not as a success alias.

**Acceptance:** the demonstration run updates the target-level posterior while
leaving pattern reinforcement at zero.

### Packet D — connect learned state to live outer selection

- [ ] Load and pin the outer target posterior before `task-state-candidates`.
- [ ] Give every supported candidate a comparable learned-outcome model input;
  absence uses the same declared prior, not a missing channel.
- [ ] Compute per-candidate predicted outcomes and G terms at the outer grain.
- [ ] Bind scoring rows to full target identities and the posterior revision.
- [ ] Retain structural centrality, declared priority, feasibility, and
  freshness as separately visible inputs rather than hiding them in one scalar.
- [ ] Replace pairwise shared-channel omission with a comparison rule that
  cannot let missing channels erase another candidate's evidence.

**Acceptance:** the production selector certificate shows the posterior input,
predicted observation distribution, risk/ambiguity/EIG, structural terms, and
the nearest losing alternative for its selected target.

### Packet E — continuation and cadence without an ad hoc ban

- [ ] Define an explicit continuation observation: the previous artifact names
  a concrete next step, acceptance condition, and evidence pointer.
- [ ] Compare continuation against switching using predicted marginal outcomes
  and resource cost.
- [ ] Add a declared recent-selection opportunity cost or scheduling constraint
  only if needed after posterior learning is live.
- [ ] Exempt stop-the-line repair and an explicitly authorized active flight;
  record the exemption on the certificate.
- [ ] Retire the CLI's “repeat then stop” behavior once selection itself can
  reason about continuation; retain it as a safety backstop until then.

**Acceptance:** consecutive selection is possible when warranted, but static
centrality alone cannot monopolize repeated clicks.

### Packet F — certificates, debugger, and operational visibility

- [ ] Add an outer-learning certificate to each run containing prior revision,
  observation source, update verdict, posterior revision, and consumption path.
- [ ] Make absent, refused, shadow-only, and consumed states distinct.
- [ ] Teach the debugger to stop when an admissible terminal outcome fails to
  produce its required outer update, or when selection ignores a present learned
  state.
- [ ] Add report-card rows for learning update and learned-state consumption.
- [ ] State the re-arm condition beside every disabled learning mechanism.

**Acceptance:** “the machine learned from this run” is mechanically false unless
the run can point both to the posterior update and to a later selection that
consumed its revision.

## 5. Test and demonstration plan

Tests are scoped and warrantable; no whole-CI rerun is required for each packet.

### Unit properties

- [ ] Observation projection preserves terminal, want-effect, collateral
  progress, failure, and resource dimensions independently.
- [ ] Posterior update is deterministic, idempotent, and chosen-target-local.
- [ ] Missing evidence refuses or remains typed absent; it never becomes zero.
- [ ] Preferences remain unchanged under outcome updates.
- [ ] Every scoring row consumes the posterior revision it certifies.

### Production-shaped fixture

- [ ] Use the shape of run
  `2026-10-14-6a64c83f-4732-4af1-b286-561f22109667`: one target, four selected
  wants, all untouched, grounded collateral artifact, approximately 722 seconds
  and 4.22 million tokens.
- [ ] Verify that this shape updates target-level beliefs without positively
  reinforcing its four patterns.

### Counterfactual selection tests

- [ ] With no history, reproduce the current centrality-driven choice.
- [ ] Add the grounded-progress observation and prove that posterior/G changes.
- [ ] Provide no continuation evidence and show another target can win.
- [ ] Provide strong, pinned continuation evidence and show the same target can
  legitimately remain first.
- [ ] Remove the learned-state input and require a typed
  `:outer-learning-state-unavailable` refusal rather than silent fallback in
  production mode.

### Live demonstration

1. Capture a before-selection certificate.
2. Run one supervised click.
3. Verify the target observation and posterior update.
4. Run the read-only production selector preview.
5. Demonstrate that the new ranking differs for a stated evidential reason—or
   that continuation wins from explicit predicted marginal value.
6. Run the next supervised click and verify that it consumes the certified
   posterior revision.

## 6. Migration and safety

The upgrade should be introduced in four modes:

1. **record-only:** produce observation rows and posterior candidates;
2. **shadow:** calculate learned outer G beside the existing decision;
3. **compare:** registered A/B replay over pinned historical manifests;
4. **live:** switch authority only after the production-shaped counterfactual
   tests and a supervised demonstration pass.

Every mode is written on the receipt. Shadow output must never be described as
having influenced selection. The live switch carries its rollback and re-arm
conditions in writing.

Do not mutate historical run records. Fold them through versioned readers with
typed exclusions. Do not infer positive want effects from terminal labels. Do
not import tactical cascades into outer target selection. Do not tune against
the single demonstration target and call the result learning.

## 7. Exit conditions

- [ ] The selected run's outcome becomes a replayable target-level observation
  with pinned authority.
- [ ] A versioned posterior updates from that observation at the outer target
  grain.
- [ ] The next production selector consumes that exact posterior revision.
- [ ] Its certificate exposes risk, ambiguity, EIG, resource terms, structural
  inputs, missingness, and the comparison that selected the winner.
- [ ] Grounded collateral progress can change target belief without falsely
  reinforcing patterns or claiming checkbox progress.
- [ ] Repeating a target requires explicit continuation value or another visible
  preference term; unchanged centrality alone is insufficient.
- [ ] A production-shaped end-to-end test proves
  `observe → update → changed next decision`.
- [ ] A supervised two-click demonstration produces both update and consumption
  certificates.

**Excursion exit:** the War Machine may claim an outer learning loop only when a
recorded outcome changes a durable target belief and a later target decision is
mechanically proven to have consumed that changed belief.
