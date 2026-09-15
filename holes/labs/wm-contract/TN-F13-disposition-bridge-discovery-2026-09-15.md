# F13: disposition bridge and pattern-success evidence discovery

2026-09-15 · codex-27 · read-only inspection, no store requests or runtime changes. **C exists; the fitted bridge for the nine mission facts and success probabilities for the seven interpretations are MISSING.** This corrects the earlier redo's missing-C label. The ruled distribution is `mathlib4/DarkTower/WarMachine/MachinePreferenceDistribution.lean:29-41`; the still-open observation bridge is explicitly distinguished at `mathlib4/DarkTower/WarMachine/Holes.lean:156-157`.

## 1. Recorded closes: bounded census, with identity caveat

I read every `*-closed.edn` beneath the immediate `futon2/data/wm-full-loop*` directories, including archives, and joined checkpoints **within the same physical attempt directory**. This is a bounded on-disk census, not a claim about every store, scratch fixture, or tick ever produced. Canonical event shape and close validation: `futon2/src/futon2/aif/full_loop_cohort.clj:25-38,493-528`; summarization joins selection/build rather than expecting those fields on close: same file `530-548`. All counted close files are one-line EDN, so their evidence location is `:1`.

Actual command results: **101 files, 100 distinct full EDN close events**, spanning **2026-07-14T20:27:28.840534153Z to 2026-09-15T04:18:16.480153233Z**. One identical event copy is removed. There are 77 `(cohort-id, attempt-id)` keys, but **23 keys have conflicting timestamps AND dispositions**. Those are not duplicate observations to silently discard: archived and restarted epochs reuse IDs. Example: `futon2/data/wm-full-loop/archives/stop-line-2026-07-15/wm-outer-loop-40-v1/attempt-008/007-closed.edn:1` is no-selection on July 15, while `futon2/data/wm-full-loop/wm-outer-loop-40-v1/attempt-008/007-closed.edn:1` is build-failed on July 16. Full-event equality removes only an identical copy; this is an event census, **not 100 proven independent experimental trials**.

| Disposition | Distinct close events |
|---|---:|
| build-failed | 32 |
| grounded-change | 25 |
| no-selection | 22 |
| agent-unavailable | 11 |
| incomplete | 8 |
| substrate-unavailable | 2 |
| grounded-no-change | 0 |
| artifact-only | 0 |
| abstained | 0 |
| guardrail-refusal | 0 |
| dispatch-failed | 0 |
| cancelled | 0 |

The two additional historical-verification classifications currently admitted by the event schema also have zero closes in this census; they are **not** extra masses in the twelve-disposition C carrier (`full_loop_cohort.clj:28-38`; `futon2/src/futon2/aif/ruled_outcome_c.clj:22-33`). Raw file counts before removing the identical copy are 32/26/22/11/8/2 in the six positive rows above. Of the 100 events, 19 come from machinery cohorts 47–57; those are not qualifying-run evidence. Cohort purpose explicitly says machinery-only: `futon2/holes/labs/M-aif-full-loop-47/cohort.edn:5`. Latest counted close: `futon2/data/wm-full-loop-machinery-57/wm-contract-machinery-57-v1/attempt-001/007-closed.edn:1`.

Field availability measured after the same event deduplication:

| Joined evidence | Non-nil field count / 100 | Meaning and limit |
|---|---:|---|
| selection judgment `:selected-mission` | 85 | May name a repair target rather than a mission; close itself carries none of these mission fields. |
| construction judgment `:patterns` | 59 | Constructed/proposed members, not proven individual executions. |
| construction judgment `:wiring` | 17 | Assembled wiring object; not a successful enactment certificate. |
| construction judgment `:sorries` | 59 | Policy holes as recorded, including empty collections. |
| build judgment `:validation` | 38 | Attempt-specific validation, not automatically the full mission's node suite. |
| adjudication judgment `:before` and `:after` | 26 each | Implementation/repair witness state, not nine named mission Booleans. |
| selection targeting `M-zaif-harness-v1` | **0** | No target-specific closed sample in this census. |

All 100 events have physical selection, construction, build and adjudication sibling files, but many contain typed sorries, not judgments. E.g. `.../wm-full-loop-machinery-57/wm-contract-machinery-57-v1/attempt-001/006-adjudication.edn:1` says `:not-reached-adjudication`. The measured paths are `[:payload :judgment field]`; nil is not interpreted as false. Producer contracts: `full_loop_cohort.clj:78-96` checks construction correspondence and policy holes; `futon2/src/futon2/aif/full_loop_runner.clj:4376-4379` writes patterns-used/validation; `full_loop_runner.clj:4532-4533` writes witness before/after. Actual shape: `futon2/data/wm-full-loop/wm-outer-loop-46-v1/attempt-059/006-adjudication.edn:1` changes `{:implementation-entity nil}` to a reviewed commit entity, not a harness fact vector. Act-gate/fold evidence can be inspected via the construction/selection evidence; the existing gate bridge is `full_loop_runner.clj:1565`. None of these checkpoint names alone proves a gate passed.

**Reproduction of the core census**, run with `bb -e` from `/home/joe/code` (only reads):

```clojure
(require '[clojure.edn :as e] '[clojure.java.io :as io] '[clojure.string :as s])
(let [rs (for [root (.listFiles (io/file "futon2/data"))
               :when (and (.isDirectory root)
                          (s/starts-with? (.getName root) "wm-full-loop"))
               f (file-seq root)
               :when (and (.isFile f) (s/ends-with? (.getName f) "-closed.edn"))]
           {:file (str f) :event (e/read-string (slurp f))})
      us (map first (vals (group-by :event rs)))]
  (prn {:files (count rs) :distinct-events (count us)
        :outcomes (frequencies
                    (map #(get-in % [:event :payload :judgment :outcome]) us))}))
```

## 2. What can estimate P(d | named end state)?

**No complete joined observation of the selected nine-fact state was established.** The sample size for that conditional is **0**, not 100. Target facts and their source/temporal limits are retained in `futon2/holes/labs/wm-contract/runs/F13-model-manifest-2026-09-15/redo/interpreted-pattern-set.edn:1`; the source mission explicitly separates ready components from missing suite/gate at `futon2/holes/missions/M-zaif-harness-v1.md:101-110`, and fixed defects from pending independent replay at `208-215,226-229`. The ledger fitter itself acknowledges that it has no channel-valued observations: `futon2/checks/disposition_kernel.clj:19-27`.

| Named facts | Honest possible reconstruction | What cannot be substituted |
|---|---|---|
| components-built; node-spec-map-exists | Pin code/spec artifacts at attempt time and verify the named component/map scope. | Any fresh commit or wiring object is not existence of those particular components. |
| complete-per-node-suite-exists; reporting-gate-exists | Versioned node inventory plus tests/receipt proving **all** declared obligations, including U8. | A build-pass field or three regression tests is not full-suite/reporting-gate coverage. |
| regression-fixes-landed | Bind the actual fixing revision to the three named defect obligations and its review. | A generic grounded-change label does not identify those fixes. |
| independent-replay-confirmed; certificate-promotion-complete | Independent receipt bound to repaired revision and rejected controls, followed by authorized promotion receipt. | Build success, request dispatch, or author assertion cannot establish either. |
| pending-status-recorded; replay-request-recorded | Timestamped typed status/request event for this certificate and revision. | Historical nested backtrace text cannot be treated as current end state. |

These are proposed semantic joins, not completed backfills; their standards follow the mission spans above and the new pattern `futon3/library/coordination/bind-promotion-to-post-repair-replay.flexiarg:13-18`. Record pre-state and end-state fact IDs/values, source digests, measurement times, interpretation revision, target, and unknowns alongside the close's disposition. Do not fill unobserved facts with false. End-state observations must precede disposition adjudication and not encode the adjudication itself; otherwise the bridge learns its label. The current before/after implementation witness is useful evidence for a narrower implementation-exists state, not a justified relabelling into this nine-fact model (`.../attempt-059/006-adjudication.edn:1`).

## 3. The posterior tables do not measure these effects

**Grounded table:** `futon6/scripts/cascade_learn.py:27-43,88-144` parses `futon6/holes/closure-folds.edn`, counts each used pattern on successful folds, and charges failures **only when the used pattern is connected to another used member in the computed phylogeny**. It collapses qualified IDs to basenames. Stored `alpha,beta` are charged counts; the mean adds an explicit Beta(1,1) prior: `(1+alpha)/(2+alpha+beta)`, rounded to six decimals. A pattern used alone on a failed fold receives no negative charge. Thus this is a connectivity-filtered **fold participation/utility** statistic, not P(its THEN establishes a particular effect | guard, mission state). Source recording defines `used` as actual THEN-fold participation (`closure-folds.edn:1-13`); success is an authored Boolean, not mechanically independent grading. Some rows cite named reviewers/commits (`closure-folds.edn:52-71`), but no uniform reviewer identity or independent success contract is required by the parser.

Actual read-only results: **15 closure records: 12 success, 3 failure**. Stored table: **9 patterns, each alpha=1/beta=0, total 9 charged observations**, all means 0.666667 (`futon6/data/pattern_posteriors.grounded.json:1-47`). Recomputing `learned_from(parse_closure_folds(...), load_computed(...))` in memory gives **10** patterns/charges: stored projection lacks `two-projections-of-one-quantity`, whose successful record is `closure-folds.edn:67-71`. **Typed finding: stale grounded projection.** I did not rewrite it. Neither stored nor recomputed data covers the seven selected interpretations.

**Self-graded table:** mission-Markdown PUR extraction across `futon*` roots, with pattern identity resolution, a typed-ish outcome parsed from prose, required prediction error, and deduplication by `(path,line)` (`futon3a/holes/labs/M-memes-arrows/pattern_posteriors.py:67-73,119-152,167-204`). Outcome parser checks success/pass/verified before partial and failure; this is lexical author-report classification, not an independent assessor. Each accepted PUR increments n; success adds (1,0), partial (0.5,0.5), failure (0,1) on a Beta(1,1) prior (`pattern_posteriors.py:27-31,205-230`). Snapshot: **1,071 patterns; 12 with evidence; 21 PURs = 17 success + 4 partial + 0 fail** (`futon3a/resources/notions/pattern_posteriors.self_graded.json:9919-9923`; counts by its retained evidence rows). Grader is the PUR reporter, labelled `self-graded`; the extractor does not establish independent reviewer identity (`pattern_posteriors.py:195-200`).

| Selected interpretation | Grounded charged observations | Self-graded n and prior | Snapshot evidence |
|---|---:|---|---|
| agent/evidence-over-assertion | 0 (absent) | 0; alpha=beta=1 | self_graded.json:148-155 |
| war-machine/operational-not-decorative | 0 (absent) | 0; alpha=beta=1 | self_graded.json:9607-9614 |
| coordination/cross-validation-protocol | 0 (absent) | 0; alpha=beta=1 | self_graded.json:796-803 |
| stack-coherence/ready-blocked-triage | 0 (absent) | 0; alpha=beta=1 | self_graded.json:8149-8156 |
| social/verify-before-compose | 0 (absent) | 0; alpha=beta=1 | self_graded.json:8032-8039 |
| social/tension-before-code | 0 (absent) | 0; alpha=beta=1 | self_graded.json:8023-8030 |
| coordination/bind-promotion-to-post-repair-replay | 0 (absent) | absent, not a measured zero | new source: futon3/library/coordination/bind-promotion-to-post-repair-replay.flexiarg:1 |

`self_graded.json` above abbreviates the full futon3a path in the preceding paragraph. Grounded absence is against the entire 47-line table, not a guessed zero row. Mean 0.5 in the six existing self-graded rows is **only the prior**. Neither table can honestly supply the requested effect-success probabilities; at most their own labelled historical utility/reporting grain could inform a separately justified prior-transfer proposal, which is not made here.

## 4. C591 versus implementation and rulings

- **Existing C, missing prediction bridge:** C591 specifies `Q(d|pi)=sum_o P(d|o)Q(o|pi)` (`futon2/holes/labs/wm-contract/C591-C-as-calculation-proposal.md:23-36`). Its old “folded false” description is stale. Item 6 rules the fold, Item 10 requires canonical prediction (`RULINGS-walkthrough-2026-09-08.md:101-115,168-177`). `ruled_outcome_c.clj:6-15,52-70` and Lean `MachinePreferenceDistribution.lean:29-41` distinguish the seed from what still needs fitting.
- **Fitter implemented:** `checks/disposition_kernel.clj:38-83` groups closes by checkpoint trajectory, no smoothing/pseudocounts. Default `holes/labs/M-aif-full-loop-46/ledger.edn:1` has **3/3 grounded-change**, all the same seven-checkpoint trajectory. This is not nine-fact conditioning; it also reads the event schema's current outcome support, which now includes two historical-verification kinds, so callers must reconcile the twelve-disposition carrier explicitly (`full_loop_cohort.clj:28-38`).
- **Risk/fold implemented:** commits `1480fb20` (fold) and `02b317f5` (constant adapter); `disposition_risk.clj:59-99` accepts only identically distributed fitted rows, **ignores observations**, and labels its bridge open. With the three-close fit, risk is constant ln 2. `disposition_risk.clj:101-134` validates kernel output and refuses positive Q mass at named-zero C. The actual fold call uses `next-mean` and explicit inputs when enabled (`efe.clj:704-710`); forwarding keys exists at `scripts/futon2/report/war_machine.clj:6007-6008`. These are code facts, not a new live-enablement probe.
- **Representation and further module work ruled:** constant adapter commissioned by Item 24; fold ON for RUN4 settled in the preceding ruling (`RULINGS-walkthrough-2026-09-09.md:250-282`). Modular C implementation/WM adapter exists; its own report says compatible-domain observation bridge still remains (`INSTANTIATE-C-module-v1-2026-09-09.md:13-30,128-138`; `holes/E-C-realization.md:16-23`). None supplies the named-fact likelihood here.
- **Zero correction:** C591's “named zeros make KL well-defined” wording cannot justify finite risk when Q puts positive mass there. The implementation explicitly refuses that case (`disposition_risk.clj:116-134`). This census has two substrate-unavailable closes, a named-zero disposition: an unconditional empirical fit would trigger that refusal. Do not epsilon-widen C or suppress those outcomes.
- **Collection authority:** Item 18d expressly forbids restarting automated cohorts just to hunt outcome coverage; collection belongs to the interactive excursion (`RULINGS-walkthrough-2026-09-09.md:44-67`). A discovery plan is not authorization for clicks.

## 5. Smallest honest estimator packets (proposals, not implemented)

1. **Paired recording, one behaviour:** extend the established checkpoint/close evidence path to retain the versioned nine-fact pre/end states and per-pattern exposure records. Identity includes data-root/activation epoch, cohort, attempt, event hash, target, policy, pattern bytes and interpretation digest; no basename-only joins. Record guard evaluated, whether actually executed, effect already true, independent post-state receipt, failures/timeouts and censored/unknown observations. This uses the existing checkpoint boundary (`full_loop_cohort.clj:25-26,507-548`), addressing the observed ID collision and construction-versus-execution gap above. Retain artifact paths; do not create a parallel ledger. Current complete paired rows: **0**.
2. **(a) Exact empirical bridge:** on qualified complete paired records from the same state semantics/epoch, estimate `P(d|s)=n(s,d)/n(s)`. **No smoothing and no prior** in the first diagnostic; all twelve outcomes explicit, unseen states typed MISSING, observed zero counts not claimed structural impossibility. Sample size now **0 per nine-fact state**. The existing trajectory diagnostic has n=3; the broad event census has n=100 but is not eligible named-state data. Report n and uncertainty/held-out calibration; with thin data do not call the row production-validated. A production accuracy/coverage criterion must be specified before admission, not tuned until a policy wins. Positive predicted mass at a ruled-zero C remains a typed scoring refusal, without modifying C (`disposition_risk.clj:129-131`).
3. **(b) Per-pattern effect estimator:** for each source-pinned interpretation and guard-eligible context, count independently witnessed effect establishment after actual execution; estimate `successes/observed-trials`, **no smoothing/prior**. Current n=0 for **all seven**, therefore MISSING, not 0.5. Distinguish “effect already true” from a causal success; the redo's replay-request and pending effects are already true in q0 (`redo/interpreted-pattern-set.edn:1`; mission `213-215`), so repeated no-ops cannot manufacture efficacy. Thin, censored, confounded or unobserved trials retain their denominators/exclusions and do not become failures or successes by default.
4. **Do not infer the rest of B^p from one success rate.** Failure may mutate state; the current seven contracts do not prove failure=identity. To form a probability kernel, record/estimate the complete observed post-state row per eligible pre-state (empirical multinomial with the same explicit no-prior diagnostic), or separately justify a success/effect versus failure/identity model. Joint effects also need joint observations. Until then **failure transition MISSING**, even if a scalar success estimate eventually exists. This follows the admitted finite kernel model (`mathlib4/DarkTower/WarMachine/CascadeEFE.lean:58-64`) and the redo's declared outcome uncertainty, not a request to invent defaults.

### Source pins for this inspection

- `futon2/holes/labs/M-aif-full-loop-46/ledger.edn`: `1f195e907fe9ab77b576a9973a058ada6e9c61510ef70309a8803744b13f8044`
- `futon6/holes/closure-folds.edn`: `a38a181827f91906b5e1275d942c0cc3f85817d23e3208461244334d762a3186`
- `futon6/data/pattern_posteriors.grounded.json`: `785915bc1d7ff105b14568e745060bdfa2cdaf25f8093483d8336ef566c6a9f3`
- `futon3a/resources/notions/pattern_posteriors.self_graded.json`: `9a9b5a273746fdb09c17a0b0584048bdf2ed1a54e21e6e36497642f0bfeb9232`

Validation performed: successful read-only census and sibling joins; posterior counts and in-memory grounded recomputation; source/ruling inspection. No tests/builds or live probes claimed. Only this note is written.
