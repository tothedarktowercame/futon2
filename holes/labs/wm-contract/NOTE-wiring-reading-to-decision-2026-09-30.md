# Wiring mission readings to the real cascade decision (2026-09-30)

**Scope.** This is a read-only wiring discovery at futon2 `17d572fb0` and
mathlib4 `4d76a76ce`. It proposes no second decision or scorer. I did not fire a
War Machine click, query the serving JVM, or load code into futon3c.

## 1. The current decision seam

The production composition root calls `wm/generate-war-machine` through its
selection judge (`src/futon2/aif/full_loop_runtime.clj:10-19`), and the runner
calls that judge once in its selection phase
(`src/futon2/aif/full_loop_runner.clj:5222-5239`). The report reads the mission
registry once, merges declared cascade sources and mission-hole wants, and
forms the target union at
`scripts/futon2/report/war_machine.clj:6532-6568`. It then calls the real
decision at the single outer seam
`scripts/futon2/report/war_machine.clj:6595-6631`:
`cd/select-and-record-cascade!`.

Inside that decision, candidates currently come from each assembled problem's
`:constructed-candidates`. Their precedence IDs are replaced by operators from
the target's already-published `:interpretations`
(`src/futon2/aif/wm/cascade_decision.clj:729-754`). Admission still requires a
nonempty precedence, an interpretation for every pattern, and interpretation
receipts (`cascade_decision.clj:1230-1281`). The joint family gets G from the
one `efe/rank-actions` call at `cascade_decision.clj:1008-1037`; selection then
passes those ranked entries to the existing policy selector at
`cascade_decision.clj:1090-1103`.

The smallest production change is to keep the outer call and the one joint
selector, but replace the current **admit interpreted constructions -> build
`joint-candidates` -> `efe/rank-actions`** provider inside `cascade-decision`
with one target-policy-family provider. For each target it reads a pinned
analysis, materialises structurally distinct reading/retraction cascades, and
calls `cascade-shape-g/score-policy`; it then supplies those scored carriers to
the same joint `policy/select-action-cascades` call. `score-policy` already
computes circumstance fit F and the complete G carrier
(`src/futon2/aif/cascade_shape_g.clj:254-271`), while `score-arranged` already
uses the co-application transition and records the preference schedule and G
terms (`cascade_shape_g.clj:135-190`). The outer
`select-and-record-cascade!` call at `war_machine.clj:6595` remains the public
seam. Replacing that call with a parallel “reading decision” would create the
second path forbidden by the team contract.

This is more than swapping only line 1010: the present admission check would
reject precisely the pre-interpretation policies that `score-policy` accepts.
The provider therefore has to replace that admission/construction segment as
one coherent internal seam, while leaving decision gating, joint selection,
ticket planning, authority, and recording in place.

## 2. What exists for an arbitrary target

There is no production reading registry. `wm_task_reading.py` can extract a
mission HEAD (or its opening when HEAD is absent) and writes a request whose
`:task` metadata contains `target_id`, file path, content digest, offsets, and
source kind (`scripts/wm_task_reading.py:56-72,93-115`). The script stops after
writing the request (`wm_task_reading.py:118-137`); an 象 seat and
`session_turn_analysis.py` produce and validate the analysis later. No code in
`generate-war-machine`, `cascade-decision`, or the runner calls this producer
or resolves an analysis from a target ID.

The only mission-HEAD results are the eight fixed experiment files under
`holes/labs/wm-contract/mission-head-cascades-2026-09-30/`. Their analysis
filenames encode a mission name, but the analysis bodies do not carry a
top-level target ID. The lab materialiser derives `:target` from the filename
stem (`src/futon2/aif/cascade_shape_g.clj:288-324`); that is an experiment
convention, not a runtime lookup contract.

I counted the canonical registry locally, without a :7073 query: the current
mission registry contains 335 mission records and
`cascade-problems/substrate-targets` returns 255 current targets. Seven of
those 255 names correspond to the eight lab analyses by filename;
`M-xiang-2000` is not in the current substrate-target set. Thus:

* **7/255** current targets have a corresponding lab analysis under the
  filename convention.
* **0/255** have an analysis discoverable by target ID through a production
  index or API today.

The dynamic target union may also include declared-source, proposal, and
ticket targets (`war_machine.clj:6555-6568`), so 255 is the measured substrate
population, not a promise that every future tick has exactly 255 targets.

Runtime needs a durable record keyed by target ID and source content digest,
containing the validated analysis, request identity, status, validator
version, analysis digest, and library pins. A mismatched source digest is a
missing current reading, not permission to use the old analysis.

## 3. Retractions at runtime

The current Python tool loads or builds a graph and computes connected
retractions (`/home/joe/code/futon3c/scripts/pattern_retraction.py:194-252,
269-299`). Its graph is reproducible from authored `why`/`how` relations and
validated analysis records
(`/home/joe/code/futon3c/scripts/mined_pattern_graph.py:83-133,153-194`). None
of that Python code is callable as a Clojure function in the futon2 JVM.

Three options preserve one decision and one scorer:

1. Invoke the Python tool as a subprocess for every target, then feed its data
   to `score-policy`. This retains one scorer but adds per-click process cost,
   filesystem assumptions, and a new typed subprocess failure for every
   target.
2. Publish retractions beside each durable reading. The decision validates
   the reading digest, graph digest, parameters, and retraction receipt before
   scoring. This is deterministic and cheap, but every graph revision requires
   refreshing all affected materialisations.
3. Publish one versioned graph artifact and implement the already-specified
   retraction calculation as a pure Clojure namespace used by the existing
   decision. The provider reads the durable analysis and pinned graph, derives
   the policy family in-process, and passes every policy to `score-policy`.

I recommend **durable background readings plus option 3**. 象 reading is an
asynchronous agent operation and should finish before a tick; retraction is
pure bounded calculation and belongs in the existing decision process. The
Python implementation remains the cross-language parity oracle. This does not
create a thin path: all policies still enter the same decision gate and joint
selector, and all G values still come from `score-policy` and the existing
scorer.

## 4. A target with no current reading

It produces a typed target failure, proposed as `:no-current-target-reading`,
with target ID, expected source path/digest, lookup source, and stage
`:policy-family`. It contributes no fallback policy and is never silently
removed. Existing assembly/admission failures are already copied into
`:dropped-candidates` with target and stage
(`cascade_decision.clj:1283-1300`), and an all-refused field becomes a gated
abstention (`cascade_decision.clj:1039-1066`). The new failure belongs in that
same collection and in the decision's per-target policy-family receipt.

The report retains `:decision`, `:cascade-problems`, and `:cascade-lanes` in
the judgement (`war_machine.clj:6775-6781`). The runner copies the controller
decision and ranked candidates into a successful selection cell
(`full_loop_runner.clj:5404-5429`), and preserves dropped candidates on an
abstention (`full_loop_runner.clj:5434-5442`). The run-fact exporter must count
`:no-current-target-reading` in `pathAbsenceCount`, as required by Q7, and must
also retain it in a per-target failure count. “Zero” is the acceptance value;
the failure remains visible until every target has a current reading.

## 5. Evidence the run record must carry

The selection certificate needs one `:target-policy-families` entry per
enumerated target, written before selection:

* target ID; mission file and content digest; request/analysis IDs and digests;
  validator status/version; pattern-library pins and graph digest;
* query-time slice IDs, slice size, library size, and proof that the slice came
  from the whole pinned library;
* reported and structurally distinct policy counts; for each policy, stable
  ID, nodes, directed/overlap edges, precedence/descent, adjustment provenance,
  and transition kernel (`:co-apply` plus kernel version);
* for every policy, F value/status/evidence, risk, ambiguity, expected
  information gain, raw and normalised G, horizon, and the preference
  distribution at every step;
* accepted/rejected/not-read fit evidence per node and the
  `:interpretation-owed` set; for the chosen policy, subsequent interpretation
  receipts and timestamps so selection-before-interpretation is observable;
* typed target failures and counts, including no reading, stale reading,
  disconnected/refused seeds, no policies, and non-finite G.

These fields let the exporter construct Q2's target/pool/policy-count facts
(`DarkTower/WarMachine/Requirements.lean:143-152`), Q5's event order
(`Requirements.lean:168-170`), Q9's completion and earlier-progress controls
(`Requirements.lean:205-211`), and Q10's same-pattern/different-arrangement
pairs and separate G values (`Requirements.lean:213-218`). The current checker
already expects `targetConstruction`, `interpretationOrder`, completion pair
counts, and arrangement pair counts
(`scripts/wm_check_run_facts.clj:30-45,61-81`). It must be extended for the
newer Q4/Q9 fields: its Q4 dependencies omit graded steps and per-policy term
counts, and its Q9 dependencies omit `earlierProgressPairs` and
`earlierProgressNoGreaterRisk`, although the current Lean definitions require
them. The record should contain the detailed receipts above; the exporter may
derive the aggregate checker fields, rather than writing unverifiable counts
at selection time.

## 6. Proposed implementation handoffs

1. **Reading registry.** Persist and retrieve a validated mission reading by
   target ID plus current source digest. Bad case: a reading for the same
   target at an older digest is returned as current.
2. **Background coverage producer.** Enqueue one 象 reading for each current
   target lacking a current record, deduplicated by target/digest, and publish
   the validated result. Bad case: two ticks enqueue duplicate work or an
   invalid analysis becomes current.
3. **Pinned graph provider.** Publish one versioned mined graph with its input
   count and digest; refuse a digest mismatch. Bad case: graph bytes change
   while the recorded digest remains old.
4. **Clojure retraction parity.** Implement the Python algorithm as a pure
   calculation and prove parity on connected, disconnected, isolated-seed,
   and alternative-tree fixtures. Bad case: an isolated or disconnected seed
   silently yields a singleton/fallback cascade.
5. **Target policy-family provider.** From one current reading and graph,
   materialise and structurally deduplicate reading cascades, real rejected
   substitutions/additions, and retractions, with provenance. Bad case: an
   annotation-only duplicate counts as another policy, or an outside pattern
   appears without an adjustment receipt.
6. **Decision integration.** Replace the interpreted-construction/scoring
   segment described in section 1, score every policy with `score-policy`, and
   pass the joint scored family to the existing selector. Bad case: one target
   lacks a reading while another is ready; the latter is selected and the
   former is counted in the decision and run record, with no throw and no
   fallback.
7. **Post-selection interpretation.** Dispatch readings only for the chosen
   policy's `:interpretation-owed` nodes, retain their receipts, and prohibit
   enactment until the debt is discharged in the same run. Bad case: enactment
   begins with an owed node or an invented operator presented as attested.
8. **Run-fact export.** Export the policy-family receipts and derive Q2, Q5,
   Q9, and Q10 inputs; update the checker's dependency lists to match current
   Lean. Bad case: deleting one policy's G terms, one horizon preference, or
   one arrangement edge makes the named requirement non-recomputable or fail.

The first runtime blocker is not scoring. It is the absence of a durable,
target-keyed, freshness-checked 象 reading for 248 of the 255 measured current
substrate targets—and the absence of any production lookup even for the seven
lab examples.
