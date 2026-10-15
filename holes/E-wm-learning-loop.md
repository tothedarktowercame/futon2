# E-wm-learning-loop — close outcome learning around outer target selection

**Date:** 2026-10-14  
**Status:** LIVE — durable outcome publication, posterior consumption, and
live selection are wired.  The 2026-10-15 supervised demonstration validated
the runtime Lean certificate and a corrected production preview consumed three
applied observations; one later supervised click must still consume that exact
post-demonstration revision to close the final temporal-loop exit condition.
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

- [x] Promote the useful projection in `meta-outcome-dataset` into a strict
  production reader over an explicitly pinned run-record manifest.
- [x] Add selected-want counts, grounded artifact evidence, typed failure, and
  collateral-progress evidence to each row.
- [x] Refuse duplicate run/click identities, source drift, missing selected
  identity, and outcome claims lacking their required evidence.
- [x] Define deterministic chronology and target-local sufficient statistics.
- [x] Record exclusions rather than silently dropping legacy or incomplete runs.

**Acceptance:** replaying the manifest containing the 2026-10-14 run yields one
row for `M-interim-director-proxy-metric-inventory` with terminal class
`:grounded-progress`, four untouched selected wants, a pinned grounded artifact,
and measured time/token use.

### Packet B — define the outer target posterior

- [x] Introduce a versioned `:wm/outer-target-belief-v1` state.
- [x] Start with an explicit operator-authorized prior over the observation
  vocabulary; never derive preferences from frequencies.
- [x] Update the selected target from each admissible observation row.
- [x] Preserve counts, posterior parameters, last observation, accumulated
  resource use, and source pins.
- [x] Make deterministic rebuild idempotent and conflicting duplicate identity
  a refusal.

**Acceptance:** a controlled observation changes the chosen target's posterior;
an identical replay does not; an unchosen target is byte-identical before and
after.

### Packet C — repair the success/learning vocabulary seam

- [x] Define `:grounded-progress` as a structured class with an explicit
  collateral-progress subtype and evidence, not just a terminal keyword.
- [x] Keep selected-want progress as a separate observation.
- [x] Require report cards to display both dimensions together.
- [x] Refuse “grounded progress” from becoming pattern reinforcement when no
  selected want moved; retain the current correct no-want-effect behavior.
- [x] Keep collateral progress as its own posterior outcome and preference cost;
  do not alias it to future selected-want success. Any transition model beyond
  this categorical assumption still requires separate authority.

**Acceptance:** the demonstration run updates the target-level posterior while
leaving pattern reinforcement at zero.

### Packet D — connect learned state to live outer selection

- [x] Load and pin the outer target posterior before shadow scoring.
- [x] Give every supported candidate a comparable learned-outcome model input;
  absence uses the same declared prior, not a missing channel.
- [x] Compute per-candidate predicted outcomes and G terms at the outer grain.
- [x] Bind scoring rows to full target identities and the posterior revision.
- [x] Retain structural centrality, declared priority, feasibility, and
  freshness as separately visible inputs rather than hiding them in one scalar.
- [x] Replace pairwise shared-channel omission in compare mode with a declared
  union-channel rule and explicit missing-evidence cost, so missing channels
  cannot erase another candidate's evidence.

**Acceptance:** the production selector certificate shows the posterior input,
predicted observation distribution, risk/ambiguity/EIG, structural terms, and
the nearest losing alternative for its selected target.

### Packet E — continuation and cadence without an ad hoc ban

- [x] Define an explicit continuation observation: the previous artifact names
  a concrete next step, acceptance condition, and evidence pointer.
- [x] Compare continuation against switching using predicted marginal outcomes
  and resource cost.
- [x] Add a declared recent-selection opportunity cost or scheduling constraint
  only if needed after posterior learning is live.
- [x] Provide an explicit target exemption carrier for stop-the-line repair or
  an authorized active flight; require the exemption on the certificate.
- [x] Retire the CLI's unconditional “repeat then stop” behavior: retain it as
  a safety backstop unless a consumed live certificate explicitly authorizes
  continuation.

**Acceptance:** consecutive selection is possible when warranted, but static
centrality alone cannot monopolize repeated clicks.

### Packet F — certificates, debugger, and operational visibility

- [x] Add an outer-learning certificate to each selection containing prior revision,
  observation source, update verdict, posterior revision, and consumption path.
- [x] Make absent, refused, shadow-only, and consumed states distinct.
- [x] Teach the debugger to stop when an admissible terminal outcome fails to
  produce its required outer update, or when selection ignores a present learned
  state.
- [x] Add report-card rows for learning update and learned-state consumption.
- [x] Add a source-pinned Lean runtime certificate for the temporal loop join:
  incoming posterior consumption, exact selected identity, and publication of
  the current run as evidence available to a future click.
- [x] State the re-arm condition beside every disabled learning mechanism.

**Acceptance:** “the machine learned from this run” is mechanically false unless
the run can point both to the posterior update and to a later selection that
consumed its revision.

## 5. Test and demonstration plan

Tests are scoped and warrantable; no whole-CI rerun is required for each packet.

### Unit properties

- [x] Observation projection preserves terminal, want-effect, collateral
  progress, failure, and resource dimensions independently.
- [x] Posterior update is deterministic, idempotent, and chosen-target-local.
- [x] Missing evidence refuses or remains typed absent; it never becomes zero.
- [x] Preferences remain unchanged under outcome updates.
- [x] Every scoring row consumes the posterior revision it certifies.

### Production-shaped fixture

- [x] Use the shape of run
  `2026-10-14-6a64c83f-4732-4af1-b286-561f22109667`: one target, four selected
  wants, all untouched, grounded collateral artifact, approximately 722 seconds
  and 4.22 million tokens.
- [x] Verify that this shape updates target-level beliefs without positively
  reinforcing its four patterns.

### Counterfactual selection tests

- [x] With no history, reproduce the current centrality-driven choice.
- [x] Add the grounded-progress observation and prove that posterior/G changes.
- [x] Provide no continuation evidence and show another target can win.
- [x] Provide strong, pinned continuation evidence and show the same target can
  legitimately remain first.
- [x] Remove the learned-state input and require a typed
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

### 2026-10-15 supervised evidence

Two authorized clicks were observed under the debugger.

- Click `wm-click-05a95278-0524-4241-9aa4-fe50224313b9`, run
  `2026-10-14-d8fcde03-a588-4fc7-8658-06e56a4dbde8`, completed
  `:grounded-progress` while the learner was in shadow mode.
- Click `wm-click-fdaba692-8108-4536-8fbd-237054a981b4`, run
  `2026-10-14-0b92b122-0c1c-4fde-ac1c-4e632242648c`, completed
  `:grounded-progress` with a `:live-consumed` certificate.  It selected
  `M-categorical-code` instead of the structural baseline
  `M-interim-director-proxy-metric-inventory`.
- The exact second run record has SHA-256
  `31752324d65a4b19a48c8f65c9d980c4ab43d5c9709b3eb6b269538e7698edc4`;
  the publication manifest used for validation has SHA-256
  `aaf19eb9ead51db7324c623c1a16789915cdd70a7f8016b1a0c8cddadf65f47d`.
  The generated Lean runtime certificate compiled with exit 0, and its four
  theorems reported no axiom dependencies.
- The debugger exposed two integration defects during the run: redecision
  could change the outer target after buying target-specific evidence, and the
  serving JVM retained an old outcome projector.  The former is repaired by
  pinning one outer receipt across in-click redecision.  Reloading the current
  projector repaired the latter without rewriting any run record.
- Replay after that reload produced three rows, three applied updates, and zero
  unresolved rows.  All three were classified as grounded collateral progress,
  producing posterior revision
  `b895dd4c602f302d550ffa574a3c2c1aecd26dd9920629e24ed1080ac66e9552`.
- A read-only production preview then issued a `:live-consumed` receipt over
  that exact revision: 104 candidates, baseline
  `M-interim-director-proxy-metric-inventory`, live selection
  `M-futonzero-mvp`, and `:changed? true`.

The second click's immutable certificate correctly records the older serving
projector's `2 rows / 0 applied / 2 unresolved` view.  It must not be rewritten
after the fact.  Therefore the preview proves that the repaired live selector
can consume the updated posterior, but the last exit condition remains open
until a future authorized click consumes revision `b895dd4c…` in its own
runtime certificate.

### 2026-10-15 R5/R8 semantic repair for future runs

Review of the working loop found two theory-to-runtime gaps which are now
repaired in code, without rewriting either supervised run above.

- R5 risk is categorical `KL(Q(o|π) || C(o))`, not expected authorized cost
  presented under a risk label.  The pinned authority still declares bounded
  outcome costs, and now also declares the conversion
  `C = softmax(-β cost)`, with `β = 1`.  Expected cost remains visible as a
  separate diagnostic.  Ambiguity is normalized entropy of the target's
  categorical one-step outcome law, under the explicit modelling assumption
  that the selected target state is observed and this outcome law is the
  likelihood `A(.|s)`.
- R8 is no longer a disconnected definition.  Each admitted observation stores
  its pre-update categorical surprise `-log P(o)`, and the outer target scorer
  consumes it as a named model-fit diagnostic.  It is deliberately not added
  to the future-policy expected-free-energy score: that would double-count
  present evidence as future policy value.
- The runtime Lean adapter now imports the existing outcome-risk and policy-EFE
  theory modules.  Before emitting a certificate it independently recomputes
  KL risk, entropy, canonical `G`, and (when present) R8 surprise from the
  recorded distributions.  Correct-looking labels with false numbers, or an
  R8 producer without its declared consumer, are typed refusals.

These changes govern future clicks only.  A later supervised run is needed to
produce empirical evidence bearing the new R5/R8 runtime certificate; the
October records remain evidence for the semantics they actually executed.

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

- [x] The selected run's outcome becomes a replayable target-level observation
  with pinned authority.
- [x] A versioned posterior updates from that observation at the outer target
  grain.
- [x] The next production selector consumes that exact posterior revision in
  shadow/compare mode.
- [x] Its certificate exposes risk, ambiguity, EIG, resource terms, structural
  inputs, missingness, and the comparison that selected the winner.
- [x] Grounded collateral progress can change target belief without falsely
  reinforcing patterns or claiming checkbox progress.
- [x] Repeating a target requires explicit continuation value or another visible
  preference term; unchanged centrality alone is insufficient.
- [x] A production-shaped shadow end-to-end test proves
  `observe → update → changed next decision`.
- [ ] A supervised two-click demonstration produces both update and consumption
  certificates.

**Excursion exit:** the War Machine may claim an outer learning loop only when a
recorded outcome changes a durable target belief and a later target decision is
mechanically proven to have consumed that changed belief.
