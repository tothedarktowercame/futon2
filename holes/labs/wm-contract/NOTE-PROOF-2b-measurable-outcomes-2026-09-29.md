# PROOF-2b measurable outcomes (2026-09-29)

This is a measurement design for the draft at `PROOF-2b.md`, not evidence
that its requirements have been met.  I read one actual record of every
named carrier before using its keys: the latest run record
`data/wm-runs/tick-run-record-2026-09-29-1790701955.edn`, the tail returned by
`futon2.aif.trace/read-history-strict` from `data/wm-trace`, every JSON object
in `data/wm-click-loop/ledger.jsonl`, and the EDN bindings under
`../futon3c/data/wm-click-run-bindings/`.  A metric below is an acceptance
metric only when its population and repository basis are pinned.

## Summary table

| RQ | Metric (record, formula, unit) | Today's value (2026-09-29 UTC) | Proposed threshold and window | Instrumentation | Verdict |
|---|---|---|---|---|---|
| RQ-1 | On each click, persist a HEAD-pinned `:target-field` manifest containing the four sets `:missions`, `:excursions`, `:tickets`, `:algorithms`, plus `:enumerator-sha`. Compute `missing = enumerated_HEAD − decision`, `extra = decision − enumerated_HEAD`, and equality between the decision and report digests. Units: target IDs and set mismatches. The present run has only outcome summaries such as `[:mission-hole-coverage :targets-added]`, not the decision set. | **Not computable as stated.** Independent read-only census in `NOTE-target-field-coverage-2026-09-29.md:101-118` found 221 live missions, 373 live excursions, 43 live tickets, while the decision substrate was only 264 M/T targets and omitted excursions. Latest run has `[:mission-hole-coverage :status] = :absent`, reason `:coverage-not-recorded`; its decision is absent. | Per click: `missing=0`, `extra=0`, decision digest = report digest, and all are at the same HEAD SHA. Maintain 100% over a campaign of 20 clicks. | New manifest written by the common enumerator and copied verbatim to run record and target-field report. | **measurable-after-instrumentation** |
| RQ-2 | Join binding `[:click/id]`, `[:outcome]`, `[:run-record]` to the run's terminal action receipt, or join a typed failure ID to click-ledger `repair-dispatched` and terminal `repair-judged`. `completion_rate=(action receipts + routed typed failures)/clicks`; unit %. | Six bindings were recorded today: outcomes `{:abstained 5, :build-failed 1}`; zero grounded actions. The checked ledger contains four click events, all abstained; one has `repair-dispatched` and `repair-judged`, three terminate at the stall stop without a repair event. Thus grounded-action rate is **0/6**, and ledger-visible route coverage is **1/4** for that ledger population. This is not yet a valid RQ-2 pass/fail because the binding has an outcome keyword, not a typed failure ID or action receipt. | Per click, exactly one terminal action receipt or typed failure; every failure has a matching dispatch within 5 minutes and terminal judgment. `completion_rate=100%` for each 20-click campaign; additionally at least one enacted action so a campaign cannot pass entirely by failing. | Add `:action-receipt` or `:failure {:id :kind ...}` to the run; copy failure ID into ledger dispatch/judgment. | **measurable-after-instrumentation** |
| RQ-3 | For every candidate persist `:G {:risk r :ambiguity a :information-gain i :novelty n :total g}` and check the declared equation within `1e-12`. For every target classified unknown, count admitted gathering actions and require `i>0`. A paired control holds risk/ambiguity equal and compares unread vs known. Units: nats and target/action counts. | **No current value.** Latest run `[:decision :chosen]` is `{:status :absent :reason :no-chosen-action}` and carries no candidate decomposition. Existing `:g-term-decomposition` only appears when a choice is reached; there is no per-target unknown-state/gathering-action manifest. | Every click: 100% of candidates have finite decompositions; every unknown target has at least one gathering action with IG > 0. Once per release, the paired control has `G_unread < G_known` (or higher posterior selection probability) at equal non-epistemic terms. | Add target knowledge-state, action kind, and complete candidate decomposition to the selection certificate. | **measurable-after-instrumentation** |
| RQ-4 | Replace the phrase with a versioned model contract. Record `:self-model {:factors {factor-id {:kind :states :prior :posterior}} :basis-sha ...}`. Measure coverage against the RQ-1 target manifest and a pinned stack-claim manifest; normalization error `abs(1-Σq)`; and whether an action-touched factor has a posterior transition or typed no-update receipt. Units: covered IDs, probability error, transitions. | The latest trace has `[:scan-learn-state :q]` over seven strategic statuses and `:mu-pre`/`:mu-post` over stack entities. It does **not** have task factors or a self-action factor manifest. Therefore “a generative model of itself” has no present observable truth condition. | Per click: factor coverage 100% of pinned tasks and stack claims plus every declared self-action/failure kind; all distributions normalized within `1e-12`; every enacted action names touched factors and yields a transition/no-update receipt. Campaign: at least one nonzero posterior transition in each exercised factor family. | New factor/model manifest and touched-factor receipt. | **not-measurable-as-written** |
| RQ-5 | Diff consecutive `:self-model` manifests. Expansion event must name triggering task/observation and new factor/state IDs; reduction event must carry BMR `:delta-f`, threshold, and before/after IDs. Units: factors/states added or removed and nats. | **No current value.** Scan learning changes concentrations but does not record task-triggered factor creation; current BMR records reduce parameters, not the proposed self-model's factor/state space. | For a planted new task, next click adds exactly its factor with declared unread prior. For a planted observation whose maximum old-model likelihood is below a declared floor (propose `1e-6`), next click emits one expansion event. Remove/merge only with `delta-f <= -3`. Run each control once per release. | Add `:model-change` receipts and old-model likelihood to the run/trace; writer is the future self-model updater. | **measurable-after-instrumentation** |
| RQ-6 | In the RQ-1 manifest, compare algorithm IDs to a HEAD-pinned algorithm registry. For an algorithm action join run receipt to artifact path, commit SHA, and independent review. Units: missing algorithm IDs and completed algorithm runs. | **No algorithm registry/field carrier exists today.** The latest run has no A-target/action receipt. Git can count commits only after an action records which commit it caused; commit-message inference is not an admissible join. | Per click: algorithm-set difference = 0. Per acceptance campaign: one successful `tornhill` and one `write-algorithm` action, each with artifact, commit, and reviewer; every algorithm remains eligible even when another task wins. | Add versioned algorithm registry, include it in target manifest, and emit an algorithm action receipt. | **measurable-after-instrumentation** |
| RQ-7 | Reword “forward motion” as campaign deltas joined to receipts: enacted actions; wants flipped false→true; task lifecycle advances; algorithm runs; reviewed commits; repairs closed. Do not count wiring commits unless a later action receipt cites the wire. Units: counts per 20-click campaign. | Today's six bindings: **0 grounded actions** (`5 :abstained`, `1 :build-failed`). Checked ledger: **0 carried ticks**, **0 commits** in its only repair judgment. Want/task deltas and commit-to-action attribution are not recorded. There is no observable by which “forward motion” itself can currently be decided. | Each 20-click campaign: RQ-2 completion 100%, enacted actions > 0, and at least one of `{want closed, task advanced, reviewed action commit}` > 0; report all component counts without collapsing them to a score. | Campaign ID on bindings/runs plus want/task before-after receipt and commit/review join. | **not-measurable-as-written** |
| RQ-8 | For every enacted action require one receipt with `:click-id`, target ID/kind, chosen action, complete G decomposition, outcome, and (when mutating) commit SHA and review identity. `paper_trail_rate=complete receipts/enacted actions`; validate commit by `git cat-file -e SHA^{commit}`. Unit %. | Binding records provide click ID, outcome, and run path, but the latest run has no chosen action; neither carrier provides a causal commit/reviewer join. Present completeness for today's five abstentions is not applicable, and there are zero enacted actions to test. | 100% per click and per 20-click campaign; mutating actions additionally have one reachable commit and reviewer distinct from author. Zero enacted actions cannot discharge this requirement. | Add the unified action receipt, preferably to run record and binding by digest. | **measurable-after-instrumentation** |
| RQ-9 | Every failure receipt has `:id`, `:kind`, `:source`, `:at`; ledger events cite that ID. `route_rate=failures with dispatch/failures`; `closure_rate=dispatched with terminal judgment/dispatched`; latency in seconds. | Checked ledger's four click events are all `outcome:"abstained"`; one is followed by dispatch and judgment (route **1/4**, closure **1/1**, dispatch latency 2 s), while the prior three lead to a stall stop. Latest run says `:no-chosen-action` and coverage absence, but neither is a typed failure joined to the repair. | Per click/campaign: untyped failures = 0, route and closure rates = 100%, dispatch latency ≤ 5 minutes. A `no-defect` judgment is allowed but must close the same failure ID. | Add failure IDs and kinds to run and ledger; require ledger referential integrity. | **measurable-after-instrumentation** |

## Commands and observed carriers

Commands were read-only and run from `/home/joe/code/futon2`:

```sh
# Inspect the real latest run, rather than infer keys from prose.
clojure -M -e '(require (quote [clojure.edn :as edn]))
 (let [x (edn/read-string {:default tagged-literal}
               (slurp "data/wm-runs/tick-run-record-2026-09-29-1790701955.edn"))]
   (prn (select-keys x [:run/id :click/id :decision
                        :mission-hole-coverage :live-c-coverage])))'

# Count the actual binding population and today's outcomes.
clojure -M -e '(require (quote [clojure.edn :as edn])
                         (quote [clojure.java.io :as io])
                         (quote [clojure.string :as str]))
 (let [xs (map #(edn/read-string {:default tagged-literal} (slurp %))
               (filter #(.endsWith (.getName %) ".edn")
                 (file-seq (io/file "../futon3c/data/wm-click-run-bindings"))))
       t (filter #(str/starts-with? (str (:recorded-at %)) "2026-09-29") xs)]
   (prn {:all (count xs) :today (count t)
         :today-outcomes (frequencies (map :outcome t))}))'
# => {:all 84, :today 6,
#     :today-outcomes {:abstained 5, :build-failed 1}}

# Read every event in the current click-loop campaign ledger.
jq -c . data/wm-click-loop/ledger.jsonl

# Read the actual trace tail and selected keys.
clojure -M -e '(require (quote [futon2.aif.trace :as t]))
 (let [r (last (:records (t/read-history-strict 1 {:dir "data/wm-trace"})))]
   (prn (select-keys r [:run/id :scan-learn-state :mu-pre :mu-post])))'

# Commit reachability check proposed for RQ-8.
git cat-file -e <sha>^{commit}
```

The actual binding schema uses `:click/id`, `:outcome`, `:run-record`,
`:binding-status`, and `:run-id-observation`.  The click ledger uses JSON keys
`event`, `click-id`, `outcome`, `run-id`; repair events additionally carry
`tag`, `job`, `verdict`, and `commits`.  The latest run uses `:click/id` and
`:run/id`, `[:decision :chosen]`, and coverage maps.  These namespaces are
similar enough to invite accidental joins but are not interchangeable.

## Measurable rewordings for the three especially fluffy phrases

### RQ-3: standing curiosity

**Observable difference:** with non-epistemic terms held equal, an unread
target has an admitted information-gathering action with positive expected
information gain and ranks ahead of the otherwise identical known target.
If the selection certificate does not show that counterfactual difference,
“standing curiosity” adds no observable requirement.

Proposed text: “For every target whose knowledge state is `:unknown`, the
candidate set contains at least one gathering action with finite IG > 0; every
candidate records risk, ambiguity, IG, novelty and total G, and the paired
unread/known control ranks unread ahead when all other terms are equal.”

### RQ-4: a generative model of itself

**Observable difference:** a versioned factor graph explicitly includes all
pinned task IDs, stack claim IDs, and the machine's declared action/failure
kinds; an action changes (or explicitly declines to change) the posterior of
the factors it names.  Merely carrying `:mu-post` or the seven-state scan
learner is not this claim.

Proposed text: “Each click records a normalized posterior over a versioned
factor manifest covering the pinned task, stack-claim and self-action
populations, and each action records the factor IDs it observes and their
prior/posterior transition.”  The metaphysical phrase should be dropped once
this contract replaces it.

### RQ-7: forward motion

**Observable difference:** within a fixed campaign, at least one externally
checkable state changes: a want closes, a task advances lifecycle, or an
action produces a reviewed commit.  Actions and repairs are reported too, but
they are activity, not automatically progress.  With none of these deltas,
“forward motion” has no observable content.

Proposed text: “For each 20-click campaign report action, want-transition,
task-transition, algorithm-run, reviewed-action-commit and closed-repair
counts.  Acceptance requires RQ-2 for every click, at least one enacted
action, and at least one want/task/reviewed-commit delta causally joined to an
action receipt.”

## Audit of the plan's ACCEPT lines

| Step | Checkability as written | Checkable replacement |
|---|---|---|
| ⟨0⟩1 | `:targets-added > 0` is readable at `[:mission-hole-coverage :targets-added]`; “selection over >5 targets” lacks a persisted admitted-support count on the current absent decision. | Require one run with `targets-added>0` and selection certificate `:eligible-target-count>5`, both under the same enumerator SHA. |
| ⟨0⟩2 | A planted refusal can be observed, but “repair dispatch” is not referentially joined to it. | Failure receipt and ledger dispatch share `:failure-id`; verify kind `:schedule-disagreement`, dispatch within 5 minutes, terminal judgment. |
| ⟨1⟩1 | “field count = enumerators' count” can pass with the wrong members and moving HEAD. | Persist both ID sets and HEAD SHA; require exact set equality, not count equality. |
| ⟨1⟩2 | “live unread excursion” and “following click” have no persisted unread classification or action-to-commit join. | Fixture/real excursion ID appears as `:unknown`; chosen `read-criteria` receipt names it; next click manifest contains its committed criteria at recorded SHA. |
| ⟨1⟩3 | Report/cleanup/review can be checked only if the action names its artifact and commit. | One Tornhill receipt records report digest, proposed cleanup ID, commit SHA, reviewer and review verdict; all resolve in git. |
| ⟨1⟩4 | “one live instance with its paper trail” inherits RQ-8's missing schema. | `write-algorithm` receipt records new algorithm ID/path/commit/review; next click's algorithm manifest contains the exact ID. |
| ⟨1⟩5 | Typed empty-field error is checkable; “no click ends without action or error” is not until terminal receipts are exhaustive. | On the planted click require one `:empty-target-field` failure; across campaign require exactly one of action/failure on 100% of click IDs. |
| ⟨2⟩1 | “record carries posteriors” needs a declared factor population; “belief moves” needs prior, posterior and action-factor join. | Require normalized prior/posterior for every factor-manifest ID and a named task factor with `L1(q_post,q_pre)>1e-12` after its joined action. |
| ⟨2⟩2 | Candidate epistemic/novelty values are checkable once recorded; “scores better” needs controlled equality of other terms. | Record all G terms and use a paired fixture with identical risk/ambiguity/habit; require `G_unread<G_known` within stated tolerance. |
| ⟨2⟩3 | New mission appearance can be checked only with consecutive repository bases; “unexplained observation opens a state” has no threshold. | Pin pre/post SHAs and manifests; require exact new task factor. Define unexplained as max old-model likelihood `<1e-6`, then require a model-change receipt naming the new state. |
| ⟨2⟩4 | No ACCEPT line is stated. “Lean correspondence” is otherwise susceptible to existence-only evidence. | Name the declarations and registry rows, require compiled tests on fixtures exported from the running Clojure model, and exact schema/version binding. |
| ⟨3⟩1 | Counts are reportable, but N is unset and “wants/tasks moved” lacks transition receipts. | Set `N=20`; use the RQ-7 campaign contract and report denominators as well as counts. |
| ⟨3⟩2 | “flight ... to completion” and “same trail” do not name a terminal state or join. | Require a flight ID with terminal mission criterion met, progress-check verdict per click, action receipts joined by click ID, and final mission state/commit at pinned SHA. |

## Bottom line

None of RQ-1 through RQ-9 is fully measurable from today's records exactly as
written.  Seven become mechanical with narrowly specified instrumentation.
RQ-4 and RQ-7 should be reworded because their present nouns do not themselves
denote observables.  RQ-3 is meaningful only through the counterfactual rank
difference above; absent that difference, “standing curiosity” should be
treated as prose, not an acceptance condition.  RQ-8 is the enabling carrier:
once its action receipt exists, RQ-2, RQ-6, RQ-7 and RQ-9 can share joins
rather than inventing separate campaign narratives.
