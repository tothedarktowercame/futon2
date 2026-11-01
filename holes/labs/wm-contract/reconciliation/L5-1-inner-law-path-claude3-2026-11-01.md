# L5-1: the inner selection law's code path (claude-3, 2026-11-01)

Discovery for L5-2, serving Joe's rulings of 11:48Z
(`RULING-inner-selection-enacted-object-2026-11-01.md`). No code changed, no `data/` written. Base:
futon2 main `cdb4ba6fa`. Census scripts are in `~/notes/l4b/` (`census5.clj`, `stop.clj`). `SC` =
`[:decision :selection-certificate]`. Paths under `src/futon2/aif/` unless written otherwise.

## Five things L5-2 needs first

**1. In 8 of the 13 multi-policy runs the enacted policy was chosen by list order.** Every policy in
those runs shares one first action, so the marginal is about 1.0 and decides nothing.
`chosen-entry` (`policy.clj:524`) then takes the first ranked entry with that action. The chosen
index is 0 in all 8, and ranked order is family construction order, not G
(`wm/family_selection.clj:339-345`). The record reads `:decided-by :robust`.

**2. Ruling 3 and Q11 collide once ruling 1 is in.** Under argmin, the previous run's choice is the
minimiser. On a Q6 repeat the inputs are unchanged, so the G values are too, and the minimiser is
exactly the policy Q6 forbids. Keeping it in the comparison means anything else chosen fails Q11's
"strictly below every other compared policy". So every Q6 repeat with a remaining policy violates
Q11. Q11 needs "every other compared policy Q6 does not forbid". Lean can derive that set from
`previousDecision`, `previousOutcome` and the two digests, so ruling 3's "no typed-exclusion
carrier" still holds. This is a `Requirements.lean` change, so it needs a ruling or your packet.

**3. Q6 must forbid the prediction class, not the key.** It keys on `:policy-key`
(`previous_run.clj:230`, `:excluded-keys [(:policy-key choice)]`). e71c7380 chose `policy-e77a…`;
it was refused; d5b9b513 excluded that key and chose `policy-bd8f…`, a different key in the same
prediction class at the same G. Same for f4b95abf then c5590f95. Under the quotient these are one
policy, so both repeats were a refused choice taken again.

**4. Ruling 1 says G minimiser; the posterior mode is only that when E and F are flat.** The ruling
record notes this ("with a flat E"). All 25 scored era-2 runs have uniform `:habit` and no F, so
they cannot tell the two apart. Once the enactment fold moves E, the mode can differ from argmin.
L5-2 should select by G directly and leave the posterior as a record, not take the posterior's
mode. Q11 is stated on G.

**5. Terminal receipts have been reporting the G of a policy that did not run.**
`wm/terminal_receipt.clj:64` takes `:per-policy-argmax :action` as "selected". In 12 of 13 runs that
is not `[:decision :chosen]`. The incident record's inner G values (1.4034, 2.2930, 1.6825, 1.6996…)
are these argmax values. Ruling 1 fixes this going forward, because the two then coincide. Those
values in `INCIDENT-outer-selection-Q4-reach` describe a policy other than the enacted one in the
multi-policy runs.

## 1. From posterior to enacted policy

The path is 14 hops, so past ten. Per the packet it stops in detail where G is consumed (hop 5); the
rest are named because L5-2 must change several.

| # | Where | What it decides |
|---|---|---|
| 1 | `cascade_shape_g.clj:548` `score-policy` (via `:274` `score-arranged`), called at `wm/family_selection.clj:319` | G = progress G + class risk + class ambiguity + fit-ambiguity |
| 2 | `wm/family_selection.clj:339` `construct-ranked`, `:45` `scored->ranked` | Ranked list, in family order. `:rank` is position, not G order. `:controller-score` carries G |
| 3 | `wm/family_selection.clj:349` `select-over-families` | Attach F, certify β, Q6 exclusion (`:399` `menu`), re-certify on the reduced menu (`:402`) |
| 4 | `policy.clj:351` `prepare-selection-inputs`, `:254` `selection-candidate` | Attaches E and F; the candidate row carrying `:g` that `SC :candidates` records |
| 5 | `policy.clj:477` → `cascade_selection.clj:54` `selection-posterior` | **G is consumed here**: σ(log E − F − G/β) |
| 6 | `policy.clj:493` `acting?`, `:507` `action-of` | Drops empty cascades; maps each policy to `cascade-first-action` (`:154`) |
| 7 | `policy.clj:509` → `cascade_selection.clj:179` `bayes-choice` | Sums mass by first action; ties by `:action-name-ascending` |
| 8 | `policy.clj:524` `chosen-entry` | First entry in ranked order with that first action. **This picks the policy** |
| 9 | `policy.clj:545` → `cascade_selection.clj:316` `selection-comparisons` | Recording only; writes `:policy-comparison` and `:action-comparison` |
| 10 | `policy.clj:590` | Returns `{:action (:action chosen-entry) …}`, `:chosen-action-mass` (`:647`) |
| 11 | `wm/family_selection.clj:485-500` | `verify-selected-identity` against the menu |
| 12 | `wm/cascade_decision.clj:1217` `reading-family-decision` | Assembles the certificate |
| 13 | `wm/cascade_decision.clj:1354` → `decision_gate.clj:339` `emit!`, `:262` `check-cascade-decision!` | **Independently recomputes the marginal** and refuses `:chosen-not-bayes-action`, `:chosen-mass-not-marginal` |
| 14 | `full_loop_runner.clj:914` `chosen-summary`, called at `:1520` | Writes `[:decision :chosen :id]` from `(:action decision)` |

`:decided-by :G` is written at `cascade_selection.clj:337-346`. It compares the top two policies by
posterior mass and names the term that favoured the winner. That is a true statement about the
posterior mode, but hop 8 does not choose the mode, so it does not describe what ran. The function's
docstring says "Recording only" (`:316`). `:action-comparison :decided-by` beside it (`:robust`,
`:tie-break`) is what described the marginal.

A second caller exists: `wm/cascade_decision.clj:350`, the R14 lane. ac5bc477 reads
`:selection-route :wm/reading-policy-family-v1`, the route above. `wm/family_selection.clj` also has
a `:canonical-coapply` G law with `canonical-exclusion` (`previous_run.clj:277`). No key from it
appears in ac5bc477's certificate.

## 2. What "first action" is

`policy.clj:154` `cascade-first-action`, duplicated on purpose in `decision_gate.clj:118`
`first-acting-pattern`:
- Vector precedence: `(first precedence)`.
- Co-apply precedence: `co-apply-roots` (`policy.clj:149`), the vector of pattern maps for units that
  nothing in `:descent` points at. So it is the set of arrangement roots, not one unit. Cascades with
  the same roots pool.
- Non-cascade: `(:type action)`.

Its docstring says it is the chain head and not necessarily the enacted step.

If the enacted object is the whole cascade:
- **Disappear:** hop 7 (`bayes-choice`) and the first-action map at hop 6.
- **Change:** hop 8 becomes minimum G over the menu, excluding what Q6 forbids, refusing on a tie at
  the minimum. `policy.clj:536` `weights` and `:647` `:chosen-action-mass` stop being a marginal.
  Hop 13's marginal checks become "chosen has the strict minimum G"; `:chosen-mass-not-marginal` has
  no remaining meaning.
- **Stay:** hop 5 as a record; `acting?` (empty cascades still cannot be chosen, per the
  `2026-09-21-1789951020` note at `policy.clj:479`), with stopping scored separately (section 6).
- **Ticket queue:** `policy.clj:518-523` selects within a stratum by the same law, and
  `decision_gate.clj:244-257` recomputes it. That needs argmin within the stratum.

`bayes-choice` has a legitimate meaning for the grain the ruling keeps open. Leaving the function
with no caller is a small cost, but no production path should reach it.

## 3. Tests that pin the marginal

61 files mention `select-action-cascades`, so any fixture with same-root cascades is an implicit pin
I have not audited one by one. Below are the tests that assert the behaviour directly.

**Must change under ruling 1:**

| Test | Why |
|---|---|
| `selection-law-candidate-test/the-candidate-is-the-chosen-action-not-the-posterior-mode` | Asserts the chosen differs from the mode; the ruling makes them equal |
| `decision-gate-test/non-bayes-choice-is-refused` | Pins `:chosen-not-bayes-action`; replace with a non-minimum refusal |
| `selection-certificate-test/co-application-marginal-pools-cascades-with-the-same-roots` | Pins pooling |
| `ranked-candidates-test/ranking-retains-more-than-ten-and-uses-action-tie-rule` | 12 equal-G actions; asserts `:action-name-ascending`. Under strict minimum this is a tie at the minimum and refuses |
| `wm01-bindings-test/decision-law-and-revision-bindings` | Asserts `:tie-break-rule :action-name-ascending` |
| `enacted-step-test/enacted-steps-recorded-on-the-selection-law` | Reads `:action-marginal` |
| `ticket-queue-test/front-ticket-wins-with-full-family-evidence` | Reads the marginal and `:action-comparison` |
| `selection-certificate-test/decisions-byte-identical`, `g-term-decomposition-test/existing-selection-decisions-unchanged` | Golden decisions (`test/fixtures/selection-certificate/before*.edn`) with `:action-comparison` |

**Depends on whether `:action-comparison` is kept as a record:**
`selection-discrimination-test` (`recorded-runs`, `exact-ties-and-single-action`,
`free-energy-sign-and-status`, `exclusions-and-pooled-single-action`),
`run-narrative-test/recorded-comparisons-are-rendered-without-inference`.

**Already consistent with argmin, and better off after it:** tests that write
`(assoc d :action (get-in d [:selection-law :per-policy-argmax :action]))`:
`already-satisfied-review-test/frozen-run001-drives-review-before-ordinary-refusal`,
`verified-answer-delivery-test/frozen-run001-reaches-construction`, `preference_audit_test`
(helper), and readers `terminal_receipt_test`, `meta_outcome_dataset_test`,
`construction_receipt_lean_adapter_test/aqc-certificate-refuses-identity-and-correspondence-mutations`.
They assume the mode is what ran, which the ruling makes true.

**Unaffected if the function stays:** `cascade-selection-test/bayes-choice-aggregates`.
`novelty_discovery_test` calls `bayes-choice` directly. `decision-gate-test/gate-and-selector-agree-on-what-counts-as-an-action`
still applies to `acting?`. `zaif_full_loop_test` matches by grep only on a recorded corpus and a
comment (`r16-action-the-three-laws-choose-three-different-arms-test`); grep suggests it is not on
this path. `trace_test/redirected-trace-retains-the-whole-cascade-decision-test` uses the keyword as
inert data.

## 4. Extensionally distinct

`policyCount` is not counted by prediction, and not by arrangement digest. It is the distinct
structural policy key:
- `target_policy_family.clj:105` `deduplicate` on `cascade_shape_g.clj:606` `structural-identity`:
  mission, node occurrence sequence, edges.
- `q2_construction.clj:45-52` `census-entry`: `(distinct (filter keyed? ks))` over policy keys.
- `wm_run_facts.clj`: `(count policies)`.

So same-prediction variants **are** counted as distinct. Across the 13 multi-policy runs
(`census5.clj`), using the L4c-1 digest:

| Result | Count |
|---|---|
| Runs with exactly 2 prediction classes | 13 of 13 |
| Runs where a class holds more than one G | 0 |
| Runs where the minimum-G class has exactly one policy | 13 of 13 |

So after the quotient every one of the 13 has a unique strict minimiser, with no tie and no need for
a representative rule at the minimum. The L4c-1 digest is the identity that would do it: it is what
the law scored and the projector already computes it. L5-2 needs the same function on the Clojure
selection path, applied after hop 2 (predictions exist only after scoring), so the quotient cannot
live in `q2_construction`, which reads the field before scoring. `cascade_decision.clj:1403` can
pass the quotient to it.

Two things to weigh:
- **The classes merge real alternatives.** Within a class, `:pattern-count` varies (6-7 in ac5bc477,
  8-10 in 0be4fc7d). Those arrangements use different patterns. They predict alike because the
  progress-count model counts tokens, and that model is the uncalibrated synthetic one. The ruling
  accepts this ("one policy under the model"), and it is worth knowing that the coarseness of the
  model sets the size of the policy space.
- **A stricter identity exists and is unused.** `cascade_equivalence.clj:148`
  `distinct-with-differing-effects?` compares kernel rows at s₀; grep finds only its test callers.
  It compares state transitions, not predicted outcomes, so it would split classes the ruling
  merges. I recommend the prediction digest, which is the ruling's wording.

The representative of a class, when a non-minimum class has several: lowest G, then policy key.
Inside a class G is equal in every observed run, so this is only a stable name.

## 5. Q6 exclusion before comparison

Removal: `previous_run.clj:249` `exclusion`, `kept` = ranked minus `:excluded-keys`, wired in at
`wm/cascade_decision.clj:1265` as `:exclude`, applied at `wm/family_selection.clj:399`. The selector
sees `menu`. Precision is then recomputed on the reduced menu (`:402`), and hop 5 onward never sees
the policy. That is why `SC :candidates` has 4 rows and `:q2-construction` 5 in d5b9b513 and
c5590f95. `SC :target-policy-families` keeps the 5th, so its G exists on the record.

To keep it compared and forbid choosing it:
1. `select-over-families` passes the whole `ranked` to the selector and passes the forbidden keys
   beside it; it no longer re-certifies, since the domain did not shrink.
2. `select-action-cascades` scores everything and takes the minimum over the non-forbidden.
3. `exclusion` returns `:forbidden` in place of a reduced `:ranked`; `:refusal` when nothing is left
   stands, and hop 12's `:repeat-refused` route is unchanged.
4. `decision_gate` checks "chosen is the strict minimum among non-forbidden".

The record then carries every policy in `SC :candidates`, `:scoring`, `:posterior`, with `SC :q6`
`{:status :excluded :excluded-keys […] :menu 5 :removed 0}`, or better a renamed `:forbidden` count,
because `:removed` would be false. Q3 and Q11's equalities then hold, as the ruling says. See point 2
at the top for the Q11 clause this requires, and point 3 for class-level forbidding.

The digest is safe: `selection-inputs-digest` is taken over `ranked` before exclusion (`previous_run.clj:255`).

## 6. Stopped kernel on the inner path

The transition rollout is in `cascade_observation_scoring.clj:195` `score-candidate`, which calls
`m/rollout-evaluation` (`cascade_model_manifest`) and queries `observation_model.clj`:
- Progress channel: `:343`, outcomes `[completed-count want-met?]`.
- Class channel: `:293`, deterministic emission; `cascade_shape_g.clj:40` `class-emission-model`.
- C: `cascade_shape_g.clj:222` `progress-preference n horizon`, built **per policy** from that
  policy's own token count.

**The observation model can express stopping, but the empty arrangement as scored today gives the
wrong number.** I called `(score-arranged "T" "stop" {:nodes [] :edges []})` in a tooling JVM:

```
status :computed, horizon 1, g 2.3440824595252776, progress-g 0.18311841208159615
steps [{[0 true] 1}]  class [{:stop-the-line 1}]
declaration {:horizon-steps 1 :outcome-count 2 :pattern-count 1}
```

The class channel is already right: `:stop-the-line`. The rest is not comparable with a cascade's G:
- **`[0 true]`: the want reads as met,** because an empty arrangement has empty acceptance.
- **C is different:** `n = 0` gives a 2-outcome C. Cascades use 2(n+1) outcomes.
- **The horizon is 1**, not the compared cascade's.
- **No fit-ambiguity term.** Cascades carry 0.4251 in every observed run (`score-policy`,
  `cascade_shape_g.clj:575`). Whether stopping owes it is a modelling choice: `fit-evidence` is fit
  of nodes to the reading, and stopping has no nodes.

**Smallest change:** score the empty precedence under the *minimising cascade's own* model: its
universe, `:want`, horizon and C, starting from the same `{#{} 1}`, with information gain 0. No new
query or kernel is needed, because `rank-cascade-actions` already takes the model and candidate as
arguments, and the belief just stays put. That predicts `[0 false]` at every step and `:stop-the-line`
at the terminal step, and it is digested exactly as the cascades are. `pattern-count` already floors
at 1 (`cascade_observation_scoring.clj:40`). The record needs `abstentionG` and
`abstentionPrediction` on `SC`; Lean has a single `abstentionG`, so one reference model is required,
and the minimiser's is the natural one.

Hand estimate for ac5bc477 (n = 9, horizon 9), not run: progress risk `ln Σ / ln 20` ≈ 2.38, class
risk ≈ 0.24, total ≈ 2.62 with no fit term, against cascades at 1.64 to 1.80. So stopping would lose
there, which is what you want from a working comparator, though one estimate says little.

An unresolved point: because C is per-policy, "stop" has a different G against each cascade's C. Q11's
`abstainOnTask` clause needs stopping below *every* compared cascade with one number. Using the
minimiser's model is defensible for a pick; for abstention the comparison is slightly less clean.

## 7. Blast radius

**Depend on the marginal or tie rule and must change:**

| Reader | Reads |
|---|---|
| `decision_gate.clj:211-262`, `:300-325` | Recomputes marginal, `bayes-choice` for the queue, `:chosen-action-mass` |
| `full_loop_runner.clj:2963-2973` `ranked-candidates` | `:tie-break-rule`; **throws** `:unsupported-ranking-tie-rule` on a posterior tie when the rule is not `:action-name-ascending` |
| `wm/cascade_decision.clj:364-372` | Re-keys `:softmax-weights` (R14 lane) |
| `run_narrative.clj:406`, `:467-470` | `:action-comparison :decided-by`, argmax, both comparisons |
| `selection_rationale.clj:154-188`, `:314` | `:chosen-action-mass`, margin against the rival |
| `evidence_emit.clj:115`, `:161` | `:chosen-action-mass` as "posterior mass" |
| `trace.clj:33,145,303` | Retains `:chosen-action-mass`, `:softmax-weights` |
| `scripts/futon2/report/war_machine.clj:5006` | Prints `:chosen-action-mass` |
| `futon3c/src/futon3c/aif/live_recommendation.clj:81`, `futon3c/src/futon3c/peripheral/war_machine_pilot.clj:278` | `:softmax-weights` from the selection law. **Another repo** |

**Read `:per-policy-argmax` as "the selected": corrected by the ruling, no change needed, though
the key name stays misleading:** `wm/terminal_receipt.clj:64,110`,
`construction_receipt_lean_adapter.clj:345,459,750,826`, `full_loop_runner.clj:1493`
(pattern-graph view), `meta_outcome_dataset.clj:94,182,445`.

**Comparison record only:** `wm/loop_node_exercise.clj:59`,
`construction_receipt_lean_adapter.clj:460,492-494` (pins
`:policy-printed-identity-ascending` and `{:status :undeclared}`).

**Flat selector, not this path:** `machine_q.clj:7,125-131` reads `policy/select-action`'s
`:softmax-weights`.

`futon3c/src/futon3c/aif/loop_learning.clj` and `stack_generator.clj` mention `selection-law`; I did
not read them. L5-2 should check them, and keep `:softmax-weights` present until the two futon3c
readers move, since a served JVM reads them.

Separately, the records stay readable: old records keep the marginal. Readers that branch on it
should not require it to be absent.
