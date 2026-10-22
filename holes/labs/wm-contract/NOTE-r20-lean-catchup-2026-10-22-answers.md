# R20 Clojure data catch-up answers (2026-10-22)

Read-only extraction by codex-4. This is not a claim that either click is R20 certified.

## Pair selected

| click | record | started | predecessor evidence |
| --- | --- | --- | --- |
| A | `data/wm-runs/tick-run-record-2026-10-21-fcf06c3f-8c08-4c55-a322-632090fa7220.edn` | `2026-10-21T01:27:07.240171605Z` | `:accumulation :previous-id` is `2026-10-21-35176c9e-…` |
| B | `data/wm-runs/tick-run-record-2026-10-21-752b48a7-6d82-4a0e-b888-9966e8c46116.edn` | `2026-10-21T02:43:48.771382846Z` | `[:registered-run/chronology :prior-opportunity :run/id]` and `[:decision :accumulation :previous-id]` both name A |

Both contain a machine-produced `:apparatus-certificates` receipt with schema
`:wm/r20-certificate-v1`, but it identifies Lean revision `8fda677deb` and says
`:certified? false`. The current R20 modules describe the target carrier; this
does not equate an older receipt pin with the current Lean theorem.

## 1. Nine rows

The source for each row is `[:apparatus-certificates :rows]`. “None” is a typed
absence reported by that producer, not an inferred zero or pass.

| R20 row | Retained source and actual value(s) | Carrier not recorded |
| --- | --- | --- |
| precision | Both: `:verdict :not-recorded`, `:reason :precision-update-not-retained`; checked source `[:selection-certificate :policy-precision-state]`, carry `{}` | `PrecisionFromObservation.TickRecord` update, hence no extractable E/G/F/prior/posterior relation |
| evidence | Both: `:verdict :not-recorded`, missing `[:likelihood]`. Raw policy evidence has six `prior-alpha`/`posterior-alpha` vectors, each `[1 1 1 1 1]`; these are not the Lean two-hidden-state belief carrier. | `EvidenceRow.likelihood` and observation; no valid mapping of the five-element alpha vectors to `EvidenceRow.prior`/`posterior` |
| habit | Both: missing `[:counts-after :next-consumed]`; root `:habit-reads` is `{:status :absent :reason :never-read :occurrences []}` and `:habit-reinforcement` is `{:absent :legacy-habit-store-retired}` | `R15TemporalHierarchy.TwoTickRecord` before/after/outcome/nextConsumed |
| class preference | Both: `:reason :no-scored-class-emission-candidate`, missing `[:class-emission-scoring]`, source `[:selection-certificate :scoring]` | `ClassPreferenceRow` horizon, tau, predicted class distribution, and reported risk from the scored outer candidate |
| strategic focus | Both: `:reason :focus-record-not-retained`, missing `[:previous-focus :focus :scored-class]`; expected source `[:selection-certificate :focus-receipt]` | `StrategicFocusRow.previousFocus` and strategic tick (focus, target class, score) |
| liveness | Both: missing `[:before-state :after-state :reported-live]` | `LiveRow` transition and reported liveness; terminal failure/absence is not a substitute |
| independence | Both: `:verdict :agrees`, `:case :author-is-not-reviewer`; author `zai-2`, reviewer-of-record `zai-3` | None for this inequality carrier; it alone does not certify a click. |
| joint action | Both: missing `[:ownership]` | `JointRow` per-agent ownership/write receipt |
| cost | A: agrees, `:mostly-acting`, 0/224196/224196 through/after/total tokens; input/output 222070/2126; 214824/53305 ms; 0/2 jobs. B: agrees, `:mostly-acting`, 0/612130/612130; 609636/2494; 213959/105459 ms; 0/2 jobs. Sources `[:registered-run/model-usage]`, `[:registered-run/timing :phase-timings-ms]`. | None for the record's cost extraction. |

Exact compact values are in `NOTE-r20-lean-catchup-2026-10-22-values.edn`.

## 2. `R20Run.Carries` across A → B

The run identity advances and B explicitly links to A, but none of the four semantic
carry obligations can be established from retained values:

| quantity | A end | B start | result |
| --- | --- | --- | --- |
| habit counts | absent (`:never-read`, retired store) | absent (`:never-read`, retired store) | typed none; not comparable |
| initial-state belief/counts | no R20 evidence carrier; only non-substitutable per-policy `[1 1 1 1 1]` alpha vectors | same | typed none; no `d` or posterior |
| prior precision | `:precision-update-not-retained`, checked carry `{}` | same | typed none; no prior beta |
| focus | `:focus-record-not-retained` | same | typed none; no A focus or B `previousFocus` |

This is chronological adjacency, not an R20 `Carries` witness. The retained
`:accumulation` hashes are generic status accumulation, not habit state, belief,
precision, or focus.

## 3. Proposal A

**Change, then build as a separate bounded packet.** I endorse its direction, but
not an unqualified scalar addition to `outer_target_score`.

1. `decision-focus-info` is the right semantic focus producer: it retains the previous
declared focus when no discovery window covers the decision time, as the ask classifier
already does. The outer selector must supply one frozen selection `:as-of` and retain the
resulting focus receipt with the scores; it must not independently call it at wall-clock
`now` after selection starts.
2. `outer-target-score`'s `:preference-distribution` is over outcome vocabulary, while
R15's class preference is over target classes. They cannot be silently merged as one sample
space. The packet should add an explicit target-class/C term with declared weight or
product-model, retain both distributions, and show the combination in the receipt. Folding
it into outcome C requires an explicit outcome×class model first.
3. I see no principled reason to preserve the injury branch indefinitely, but do not remove
it in A. First replay a focus-sensitive ordinary comparison; only then perform C's
stuck/finishable and G-versus-branch analysis. These records cannot provide that replay:
class and focus rows are not recorded.

The bounded A packet should freeze `decision-focus-info` at the outer-selection seam,
classify each admitted candidate in that exact context, carry the class receipt into scoring,
add a named calibrated term, and test swapped retained focuses. Unknown remains fail-closed;
the injury branch remains untouched.
