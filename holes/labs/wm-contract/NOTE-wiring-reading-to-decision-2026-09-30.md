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

## 7. Inside cascade-decision

This section re-reads the replacement boundary at futon2 `490026fbe`.  The
replacement must be in place: the interpreted-construction route and its
`efe/rank-actions` call do not remain behind an option or fallback.

### 7.1 Consumers between construction and the recorded decision

The present values have these consumers:

| Value / consumer | Current read | Disposition under reading policy families |
|---|---|---|
| `joint-candidates` construction | `cascade_decision.clj:729-754` replaces declared precedence IDs with target-specific `:interpretations`, locators, construction receipts and interpretation receipts. | **The old route itself; goes.** A reading policy is already an arranged co-application candidate from `cascade-shape-g`; requiring target-specific operators here would restore interpretation-before-selection. |
| per-problem `:interpretations` | Read while constructing `joint-candidates` (`cascade_decision.clj:736-752`), by the theta pass (`:917-953`), and by admission/progress prediction (`:1230-1281`). | **No meaning before interpretation; goes from selection admission.** `score-policy` obtains theta by library pattern ID and constructs progress operators. Interpretation receipts become a post-selection debt, not candidate admission evidence. |
| live-C freshness and scales | Sources, derivation and freshness guard are at `cascade_decision.clj:681-726`; reachability/spec is derived from `joint-q0`, wants and candidates at `:790-839`. | **Does not feed the new scorer unchanged.** Freshness may remain as separately recorded evidence, but `family-selection` supplies neither `joint-reachable`, live-C scales, nor a live-C receipt. Treating it as scored C would create a second C; see §7.3. |
| token-belief stage and carry | `token-carry/stage`, predecessor inspection and `joint-q0` are at `cascade_decision.clj:756-772`; the stage and input are attached to the certificate at `:1108-1111`. | **Carrier can remain recorded, but `joint-q0` has no scoring role.** Shape G starts from its own progress-token belief. The old target-token universe cannot initialize occurrence-specific done tokens without a new, warranted projection. |
| `joint-q0` | Supplies the old `efe/rank-actions` belief at `cascade_decision.clj:1008-1011`, reachable-domain calculation at `:814-815`, and precision model identity indirectly at `:1067-1076`. | **Old scoring input; goes.** `family-selection` does not expose one shared q0 because each policy has its own progress-token domain and scorer receipt. |
| diagnostic lanes | Each old interpreted problem runs `cascade-lane` at `cascade_decision.clj:773-789`; their token-rate scoring is copied at `:1106-1107`, used for measured A at `:1156-1175`, and returned at `:1185-1189`. | **No meaning for a pre-interpretation policy.** These lanes score target-specific interpreted token operators. They must be removed from selection output or explicitly retained as unavailable diagnostics; they cannot certify reading policies. |
| theta consumption | The old candidate patterns are read and rewritten at `cascade_decision.clj:904-953`, then summarized on the decision at `:1127-1136`. | **Old duplicate; goes.** `cascade-shape-g/arranged->candidate` already calls `learning-ledger/pattern-theta` per library pattern and leaves provenance on every occurrence (`cascade_shape_g.clj:108-142`). Family summaries must carry that provenance if the decision needs the aggregate receipt. |
| `rank-opts` | Defines the class observation model, upstream conditioning, prediction clock and old C at `cascade_decision.clj:954-999`; only `efe/rank-actions` consumes it at `:1008-1011`. | **Old route; goes.** `score-policy` already ran the existing scorer with co-application precedence, progress observation model, horizon C, fit F and all G terms. Re-scoring would be a second scorer. |
| class-unknown declines | The retry loop removes targets refused by the class scorer at `cascade_decision.clj:1000-1066`. | **No meaning for the progress model; goes.** A reading policy does not ask the focus classifier for a scalar class G. Its typed failures come from reading, graph, retraction and policy scoring and are already counted by `family-selection` (`family_selection.clj:70-112`). |
| precision carry model identity and beta | Precision model metadata is taken from the old ranked vector; candidate identities and observation schedules form `model-id`, then `advance` chooses beta (`cascade_decision.clj:1067-1076`). | **Needs fields family selection lacks:** `:cascade-scoring :precision-model`, old schedules and one joint q0. Beta still must be declared, but its model identity must be restated over the reading-family scorer configuration, graph/analysis pins, progress model and policy identities. Reusing the old identity would claim the wrong model was continuous. |
| prefix admission / `production-ranked` | Flight steps are joined to `joint-candidates` at `cascade_decision.clj:1077-1089`; `production-ranked` supplies historical prefix F immediately before selection at `:1090-1094`. | **Has no present join to reading policies.** `family-selection` already supplies circumstance-fit F. Old flight steps name interpreted candidate policies and token beliefs, whereas reading policy IDs name arranged cascades. Until a new same-policy occurrence join exists, applying prefix F would attach another policy's history. |
| ticket queue | Queue validation happens at `cascade_decision.clj:1301-1305`; selector options carry queue and refusals at `:1095-1101`; abstention retains its plan at `:1306-1311`. | **Keeps working with a field adjustment.** Reading candidates carry `:target`, so the existing selector can stratify them. The refusal input must include family failures instead of old admission declines. |
| novelty | `novelty/read-inputs` enters the existing selector at `cascade_decision.clj:1099-1101`. | **Keeps working unchanged.** `family-selection` calls that same selector once; it accepts `:novelty-inputs` in its opts (`family_selection.clj:100-106`). No second novelty calculation is needed. |
| joint selection | Old ranked entries pass through `production-ranked` to `policy/select-action-cascades` at `cascade_decision.clj:1090-1101`. | **Replaced by the one call already inside `family-selection/select-over-families`** (`family_selection.clj:64-112`). Calling the selector again in `cascade-decision` would be two decisions. |
| selection certificate fields | Precision family, token lanes/stage/input, candidate derivations, schema, preference audit, theta and focus receipts are attached at `cascade_decision.clj:1104-1153`. | The base selector certificate, novelty, law and ticket receipt **keep working**. `:target-policy-families` and counted failures must be attached. Precision, prefix, candidate derivations, theta, token-rate lanes and preference audit each need the replacement evidence named above; copying the old fields would be false. |
| authority and gate | `controller-authority/authorize` and `decision-gate/emit!` consume the selected decision and ranked entries at `cascade_decision.clj:1154-1155`. | **Keeps working** if family-ranked entries retain finite `:controller-score`, action, applied selection law and authorization fields. S12 provides those entries; the gate remains the one gate. |
| `select-and-record-cascade!` | The public wrapper calls `cascade-decision` at `cascade_decision.clj:1341-1346`; the caller stores its returned decision and lanes. | **Keeps the same public seam.** Its recorded decision must add family summaries/failures and must not claim old lanes or interpreted candidate derivations were used. |

There is therefore no safe one-line substitution at old line 1010.  The
replacement boundary begins before interpretation admission and ends at the
single selector call.  Post-selection authority, gating and the public
`select-and-record-cascade!` seam remain.

### 7.2 Producing families inside the decision

The outer report already reads mission records once.  Each record includes
`:id` and `:path` (`mission_registry.clj:13-17,409-416`), and the target vector
is built from those records plus declared/proposal/ticket targets at
`war_machine.clj:6532-6568`.  However, only the assembled result is passed to
`select-and-record-cascade!` (`war_machine.clj:6591-6603`).  Assembly retains
targets only as `:problems[*].target` and `:refusals[*].target`
(`cascade_problems.clj:343-369`); **no existing argument passed into
`cascade-decision` carries the mission file path**.  The path is available in
`loaded-missions` outside the call, not inside it.  Declared targets, tickets
and proposals may have no mission path at all.

The integration must therefore carry an explicit target-source declaration
into the existing `assembled` value (preferred, because it belongs to the
enumerated field), for example one row per target with `:target-id`,
`:source-path`, and source kind.  It must not scan the registry again.
For every row with a mission path, the decision then:

1. computes `target-reading-registry/excerpt-digest` from exactly the HEAD or
   opening (`target_reading_registry.clj:35-55`);
2. calls `current-reading` with target and digest
   (`target_reading_registry.clj:148-161`);
3. builds `target-policy-family/policy-family` with that reading and the one
   loaded graph (`target_policy_family.clj:55-97`).

`pattern-graph-pin/load-pinned` must run **once per decision**, before the
per-target map, and its typed refusal must become one counted provider failure
affecting every target rather than N filesystem reads
(`pattern_graph_pin.clj:120-150`).  The graph path and reading root can be
composition-root configuration, but cannot enable the old route.  A target
without a source path receives a counted `:target-source-path-absent`; it does
not borrow a guessed file.

### 7.3 There are currently two different C values

The old decision derives live-C and its scales at
`cascade_decision.clj:681-726`, but its current class-scoring branch explicitly
records live-C without scoring it (`cascade_decision.clj:972-985`).  The shape
scorer uses another C: at every horizon step it prefers a larger completed
pattern count and then want-met (`cascade_shape_g.clj:157-169,196-217`).
These are different outcome spaces and different preference distributions.

There is one C only if the live preference producer emits a step-indexed
distribution over the shape scorer's actual observation space
`[completed-pattern-count want-met?]`, pinned to the same policy horizon and
want definition, and `score-policy` consumes that distribution instead of its
synthetic `progress-preference`.  Alternatively the project can designate the
shape progress C as the decision's sole C and retire live-C from selection.
Merely attaching both receipts does not make them one preference.

### 7.4 Tests at the replacement boundary

A source search at this HEAD finds **26 test namespaces** that directly call
`wm-cd/cascade-decision` or `select-and-record-cascade!`.  Eighteen assert the
old interpreted-candidate, q0/class scoring, lane, prefix, or admission route
and must be rewritten around reading families (or deleted when they test a
removed behavior):

`futon2.aif.h4-production-stage-test`,
`policy-precision-carry-test`, `policy-prefix-admission-test`,
`policy-prefix-f-test`, `scoring-input-receipts-test`, `temporal-consume-test`,
`token-belief-carry-test`, `token-belief-predecessor-test`,
`token-observation-initialization-test`, `uniform-run-record-test`,
`futon2.report.cascade-decision-test`, `coverage-account-test`,
`eig-source-remaining-test`, `measured-a-decision-test`,
`new-wanted-token-admission-test`, `observation-labels-consume-test`,
`preference-schedule-decision-test`, and `wm01-bindings-test`.

Eight exercise boundaries that should retain their behavioral claim after
their fixtures acquire readings: `futon2.aif.cascade-proposals-test`,
`gate-refusal-abstention-test`, `judge-refusal-abstention-test`,
`repair-proposals-test`, `ticket-queue-test`,
`futon2.report.cascade-habit-accumulation-test`,
`cascade-habit-selection-test`, and `habit-fold-call-test`.  Their exact bytes
may gain family receipts, but proposal failure accounting, gate refusal,
ticket planning, no reinforcement during selection, recording identity and
fold delivery remain valid.  This count is by namespace, not by test form;
the source command was `rg -l 'wm-cd/(cascade-decision|select-and-record-cascade!)' test/futon2`.

### 7.5 Implementation handoffs

1. **Carry the enumerated source declaration.** Add the already-read mission
   paths beside every assembled target, including typed absence for non-mission
   targets. Bad case: the decision re-reads the registry or guesses a path for
   a ticket.
2. **Load one pinned graph and form every family.** Compute excerpt digests,
   query the reading registry and call `policy-family` once per target, with
   one graph load for the decision. Bad case: stale reading bytes are returned
   current, or N targets cause N graph reads.
3. **Replace admission and scoring in place.** Delete interpreted candidate
   admission, joint-q0/class `efe/rank-actions`, retry declines and diagnostic
   lanes from the selection route; call `select-over-families` once. Bad case:
   any option still reaches the old scorer, or an interpretation receipt is
   required before a reading policy can be scored.
4. **Restate beta/model continuity.** Define precision identity from the
   pinned reading-family scoring model and preserve beta only when that exact
   identity matches. Bad case: an old class/q0 identity carries beta into the
   progress/co-application model.
5. **Reconcile C explicitly.** Supply one step-indexed C in the progress
   observation space or retire live-C from selection, with one recorded
   authority. Bad case: the certificate contains two different preferences
   both labelled as the C that selected the policy.
6. **Attach the replacement certificate.** Record every target family,
   failures, F/G terms, kernel, pins, preference schedule and selected policy;
   retain novelty, ticket, authority and gate receipts. Bad case: deleting one
   target's no-reading failure leaves the aggregate count unchanged.
7. **Reconnect post-selection interpretation and execution evidence.** The
   chosen policy's owed nodes are interpreted before enactment; token carry,
   prefix and measured-A receipts return only when they join that same policy
   and observation model. Bad case: an old candidate's flight prefix or theta
   receipt is attached to the chosen reading policy by position or pattern
   label alone.
8. **End-to-end recording.** Through `select-and-record-cascade!`, give one
   target a current reading and one no reading. The first is selected, the
   second is counted in the recorded decision, and nothing throws. Plant a
   placeholder/fallback for the missing target and show the test fails.

### 7.6 Reason not to perform a narrow in-place splice

The replacement should be done in the existing decision, but not as a narrow
splice around `efe/rank-actions`.  The code before that call constructs the
wrong policy grain and the code after it attributes old-model precision,
prefixes, lanes, theta and candidate derivations to the ranked entries.  A
splice that preserves those receipts would produce a decision that selects a
reading cascade while certifying that an interpreted token cascade was scored.
That is worse than an explicit abstention because the record would name the
wrong evidence.  The coherent in-place replacement is the whole
interpretation-admission/scoring/certificate segment, retaining the one outer
decision, selector, authority gate and recording seam.

## 8. Sources for non-mission targets

This section reads the field at futon2 `5d8c01f6b`.  The bounded inventory
used one substrate mission query (limit 1000, the bound in
`mission_registry.clj:443-445`) and local ticket, proposal, repair and declared
source reads.  It made no WM click.

### 8.1 Ticket targets

There are two ticket inputs at enumeration.  The primary-checkout ticket
registry returns records with `:id`, `:kind`, `:path`, `:title`, status text and
class, and optional parent (`mission_registry.clj:606-631`).  The queue has a
much narrower contract: every entry has exactly `:ticket` and `:inserted-at`,
and no path, line, body or foreign-store identity (`ticket_queue.clj:20-35`).
The report has the full registry records in `loaded-tickets`, but reduces them
to IDs in `substrate-tickets` and passes only those IDs to the source
declaration (`war_machine.clj:6537,6556-6574`).  Thus the field currently loses
paths it has already read.

Two real examples establish both shapes:

* `T-car3-phase2-impl` is a registry ticket with `:kind :ticket`, path
  `/home/joe/code/futon3c/holes/tickets/T-car3-phase2-impl.md`, title, live
  status text/class, and parent `M-agency-hardening`.
* `T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`
  is a queue entry with only that `:ticket` and
  `:inserted-at "2026-09-22T05:13:31.542488602Z"`.  Independently, its
  declared cascade source records the path and byte hash of
  `resources/wm/cascade-sources/T-repair-occ-444fb018.edn`; `load-declared`
  already retains `{:path :sha256 :target}` for every declaration
  (`cascade_sources.clj:254-295`).

For the 55 primary-checkout tickets, the smallest source declaration is the
already-read ticket `:path` plus an item line when the ticket has an open
checkbox. `wm_task_reading.py` then reads the containing section and records
the exact item offsets (`wm_task_reading.py:76-91`).  A ticket without an open
checkbox should use its whole opening as inline text; it must not be assigned a
fabricated line.  The one queue-only target above can use the exact declared
EDN bytes as inline text.  All **56/56** current ticket targets have a source
under this rule; **0** have no text anywhere.

### 8.2 Proposal targets

Persisted retrieval proposals live in a `proposal.edn` bundle beside captured
evidence bytes.  The bundle contains the request and target, and each derived
proposal carries target, pattern, retrieval evidence and a proposal digest
(`cascade_proposals.clj:50-70`).  Loading checks the captured byte pins and
re-derives proposals (`cascade_proposals.clj:72-102`), but returns only the
proposal rows and a top-level list of bundle files (`:104-123`); it does not
join a bundle path back onto each proposal.

All 31 targets whose field source kind is currently `:proposal` instead come
from open repair findings.  A repair proposal carries `:repair/id`, failure
kind/stage, discharge contract, backtrace status, and a `:finding-source`
containing the exact EDN path and sha256 (`repair_proposals.clj:34-68`).  For
example:

* `T-repair-attempt-047-review-execution-evidence-missing` points to
  `data/wm-repair-obligations/findings/repair-attempt-047-review-execution-evidence-missing.edn`
  and records failure stage `:reviewer-wait`, failure kind
  `:review-execution-evidence-missing`, its discharge contract, and retained
  backtrace.
* `T-repair-attempt-048-agent-unavailable` points to the corresponding finding
  EDN and records stage `:agent-readiness`, kind `:agent-unavailable`, its
  discharge contract, and retained backtrace.

These structured findings are not Markdown item sections.  Their smallest
honest reading source is inline text equal to the exact finding-file bytes,
with the already-recorded `:finding-source :sha256` as its byte identity.  A
retrieval proposal would use the exact captured request target/query text and
its proposal-bundle pin.  All **31/31** current proposal-kind targets have an
existing finding-source file; **0** have no text anywhere.

### 8.3 The declared target and pathless mission

The sole `:declared` row is `M-wm-08-external-f2`.  It is not an open mission
record, but `load-declared` has already retained its exact source path and
sha256:
`resources/wm/cascade-sources/M-wm-08-external-f2.edn`.  S15 passed only
declared target IDs, so that known path was lost.  As with a repair finding,
its source is inline text equal to the exact EDN bytes, identified by the
loader's sha256.

The pathless mission is `M-ukrns-wp`.  Its substrate entity has name
`mission|M-ukrns-wp` but no stored `:provenance/path`, status, or retained open
items.  `substrate-entity->entry` reads the path only from
`:provenance/path` (`mission_registry.clj:407-431`); it correctly returns nil
rather than scanning for a similarly named file.  No current
`M-ukrns-wp.md` exists in the canonical checkouts or under `/home/joe/npt`.
Mentions in futon7 describe a historical npt mission but do not supply its
bytes.  This target therefore remains a counted `:target-source-path-absent`
failure until its authoritative text or provenance path is restored.

### 8.4 Source identity supplied to the reading registry

For a Markdown ticket item the declaration is `(path, item-line)`, and the
reading digest is the sha256 of the exact containing-section UTF-8 text handed
to 象 (`wm_task_reading.py:56-91`).  A mission remains `(path, HEAD)` via
`build_mission_request` (`:94-116`).  For declared sources and repair findings
the declaration is `(inline-text, source-byte-sha256, source-description)`.
The inline reading's stable registry identity is the sha256 of the exact UTF-8
`source_text`, matching the request's `excerpt_sha256`
(`wm_task_reading.py:56-72` and `target_reading_registry.clj:52-55`); the
source-file sha256 is retained separately so a reserialization cannot pretend
to be the same source.

This yields the present source census:

| Field kind | Targets | Resolvable text | No text found |
|---|---:|---:|---:|
| ticket | 56 | 56 | 0 |
| proposal | 31 | 31 | 0 |
| declared | 1 | 1 | 0 |
| mission without path | 1 | 0 | 1 |

“Resolvable” means the exact referenced file exists now.  It does not mean a
reading has been run.  The one unresolved mission remains a counted failure;
the system must not substitute one of the documents that merely mentions it.

### 8.5 Implementation handoffs

1. **Retain already-read file sources in the field.** Join live ticket records,
   declared `:files`, and proposal `:finding-source`/bundle provenance into
   `:target-sources`, with source kind (`:item-section` or `:inline-bytes`),
   path, optional item line, and byte pin. Bad case: two records claim the same
   target with different paths or hashes; assembly returns a typed source
   conflict rather than choosing one by enumeration order.
2. **Build and register both source forms.** Extend the request builder and
   registry lookup to accept the declared item-section or byte-pinned inline
   source, compute `excerpt_sha256` over exactly what 象 sees, and reject a
   changed file or inline digest. Bad case: editing one byte of a repair
   finding leaves its prior reading current, or a ticket without a checkbox is
   assigned a guessed item line.
