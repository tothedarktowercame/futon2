# Row 22: canonical R6 scoring/posterior correspondence

Date: 2026-09-13. Status: read-only source specification and blocking finding.
No production claim, scoring run, selector invocation, or runtime change.

## 1. Actual production path

The WM builds `wm-efe-opts`, then calls `efe/rank-actions` on `wm-state` and
`wm-enriched-candidates` (`scripts/futon2/report/war_machine.clj:6411-6442`).
`rank-actions` partitions policy support, calls `compute-efe` for every included
action, stable-sorts ascending by `:controller-score`, assigns ranks, and
retains exclusions in metadata (`src/futon2/aif/efe.clj:982-1019`). The WM may
then attach independently learned habit priors and apply its anamnesis/open-
mission filters (`war_machine.clj:6443-6454`). It resolves tau mode and selection
law once, computes/aligned-carries F_pi and beta over `wm-ranked`, filters to
`wm-admissible`, and calls `policy/select-action` at the strategic boundary
(`war_machine.clj:6473-6550`).

`compute-efe` returns a multi-objective `:controller-score`, not bare canonical
G. Its exact decomposition includes risk, ambiguity, prediction, preference
and model authority, horizon/belief options, and every named controller
augmentation (`efe.clj:475-590,780-980`). `rank-actions` orders that complete
score. A verifier cannot reconstruct it from action, prior, and one scalar.

At the strategic selector, the ordered `:controller-score` vector is G for the
selection law. `policy/selection-scores` computes
`lnE - G/tau [- F_pi]` with F_pi either unscaled or divided by tau
(`policy.clj:157-219`); `softmax-weights` performs the one floating log-sum-exp
normalization (`:221-241`). `strategic-recommendation` computes both the
habit-only counterfactual scores and full F_pi scores. The requested
`:controller-head` law selects the first non-no-op G-ranked candidate and does
not consume the posterior; `:full-score-posterior` selects the first maximum of
the full score only when F_pi entered (`:544-676`). The alternative actuation
branches and abstain comparison have different behavior (`:790-874`) and must
not be mixed into one witness.

## 2. E5 does not currently enter this path

The reviewed E5 function produces `:prior`, shaped `:step-score-delta`, and
`:step-score-delta-base`. Exhaustive searches of `efe.clj`, `policy.clj`, and
the WM caller find no reads of those three fields. `:prior` and
`:step-score-delta` are instead inputs to the separate rollout implementation
(`src/futon2/aif/rollout.clj:215-262,294-318,382-430,455-538`). The live WM R6
path derives `:habit-prior-bias` independently—from the EFE structural-pressure
mode and/or `habit-prior/attach-log-priors`—and reads that as ln E
(`efe.clj:963-967`; `policy.clj:796-814`). E5 `:prior` is not this field, and
E5 delta is not `:controller-score`.

Therefore there is currently no source-to-field transformation from E5 output
to live R6 scoring. A recomputation verifier cannot demonstrate E6a causal
influence until a separately reviewed behavior defines and implements that
transformation. Mapping E5 prior to ln E, adding E5 delta to G, or routing a
convenient subset through rollout would each change the model and is not
authorized by this specification.

## 3. Immutable input authority required after that repair

One independently configured fixed context must pin these complete byte
sources for one run/tick/model/cohort/event and one ordered occurrence domain:

| Source | Fields that must be retained |
|---|---|
| Candidate authority | Every occurrence id, complete action bytes, E5 unshaped and shaped rows, move class, base/shaped prior and delta; duplicates remain distinct. |
| E5 authority | Exact five-source E5 resolver pins, slow mode/intrinsics/table authority, full replay output and independent unchanged depth. |
| R6 state/model | Complete `wm-state`, observation and previous observation/prediction, forward-model and belief A/B/D modes/revisions, preference C/module, capability graph, mission/goal data, and all data-dependent lookup records read by `compute-efe`. |
| EFE options | Every option accepted at `efe.clj:581-590` and continued destructuring, including horizon, belief update, risk/ambiguity/goal-outcome/structural/control modes and weights. Absence is retained explicitly only where the actual branch defines absence. |
| Support | Included and excluded occurrence ids with the exact partition premises, followed by the complete sorted ranked output and all score-decomposition fields. |
| Habit | Exact learned-prior state/revision, policy-key projection, source choice, span-cap option, aligned per-occurrence `:habit-prior-bias`, and stats. E5 prior cannot substitute. |
| F_pi | Previous prediction inputs, full per-occurrence status, rank/policy-key join, values, scaling, coverage/refusal envelope and requested/applied placement. |
| Temperature/precision | Tau mode, selection gain and source, adaptive-temperature options, beta prior/posterior/carry source, solver status/bracket/residual/tolerance, and the exact effective tau. Engineering gain is not variational beta. |
| Selection policy | Boundary, requested/applied selection law, abstain epsilon, admissibility inputs/results, no-op identity, and fallback/refusal status. |
| Output | Full ranked rows, controller/habit/full score vectors, posterior masses in occurrence order, normalization, selected occurrence/action, explanation and every provenance envelope. |

The occurrence id must be carried beside each action throughout. Action-keyed
maps alone are insufficient when two occurrences have equal semantic actions.
Every intermediate domain must equal the fixed ordered support: no filtering,
drop, insertion, reorder or deduplication may go unrecorded.

## 4. Proposed pure verifier interface

After the missing E5-to-R6 behavior is ruled and implemented:

```clojure
(verify-r6-correspondence
 {:mode :isolated-test|:production
  :authority-root <independently configured root>
  :sources {:fixed-context <pin>
            :candidate-and-e5 <pin>
            :state-observation-model <pin>
            :efe-options <pin>
            :support-decision <pin>
            :habit-authority <pin>
            :f-pi-authority <pin>
            :temperature-precision <pin>
            :selection-policy <pin>
            :claimed-ranked-output <pin>
            :claimed-posterior-output <pin>}})
```

Each source is read once as strict UTF-8/single-form EDN; its digest is made
from the same buffer. The verifier validates typed identity/scope and the full
ordered occurrence domain, calls the actual E5 transform, the ruled adapter,
`efe/rank-actions`, the configured habit attachment/alignment, F_pi alignment,
`policy/selection-scores`, `policy/softmax-weights`, and the exact selected
branch. It compares every retained intermediate and output coordinate. It
reports IEEE floating equality/deltas for production functions. This is not an
exact-real theorem about exp/log; any analytic theorem needs a separate
floating-to-real error statement and its own assumptions.

Required refusals include missing/unreadable/mutated pins; absent identity or
authority; cross model/run/tick/cohort/event; borrowed state, observation,
model, habit, F_pi, temperature or policy choice; missing/duplicate/reordered
occurrences; equal-action occurrence substitution; support drop; incomplete
score decomposition; F_pi misalignment or typed absence relabelled present;
unknown selection/tau/scaling modes; nonfinite/nonpositive temperature;
posterior/order/normalization mismatch; and any E5 shaped change that produces
no ruled R6 input change.

## 5. Current retained evidence and limits

The retained Row 18 field remains exactly the previously reviewed field: 147
ranked candidates, 145 aligned F_pi values and two typed absences. It recorded
`:log-prior-placement :none`, not the canonical theorem's `:both`; c=1 and c=2
both fail the sufficient `c > 3R/2` condition, so uniqueness remains unknown
(`TN-row18-field-applicability-2026-09-13.md:1-45`). This packet does not search
for another field, change priors/G, clip values, alter temperature, or upgrade
that record.

Unavailable today: a retained unshaped→E5-shaped→live-R6 consumer hop; a ruled
E5-to-R6 field transformation; complete state/model/option snapshots at that
hop; full occurrence-indexed score and posterior outputs for both arms; and a
production-owned fixed comparison protocol. Consequently there is no positive
production scoring or E6a influence claim.

## 6. Bounded next work

1. Authority decision: choose and state the mathematical E5-to-R6 field
   transformation, or explicitly decide that E5 belongs only to rollout and
   cannot feed this R6 edge. Do not infer it from existing names.
2. One behavior: implement that decision at a source seam while retaining
   occurrence identity and before/after fields. This requires separate review.
3. One capture behavior: retain the complete table in section 3 at the actual
   consumer; no selector or action qualification follows merely from capture.
4. Implement the pure verifier interface and commission all refusals.
5. Only then extend E6a through E2a/E3/E2b and require a changed selected and
   enacted approved occurrence. An unchanged choice remains nonqualifying.
