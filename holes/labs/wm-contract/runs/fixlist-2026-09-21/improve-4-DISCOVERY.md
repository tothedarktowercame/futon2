# improve-4 — attested results and institutional routes in C

2026-09-21, codex-13; branch `fix/narrative-improve-4`, futon2 base
`0de21aa4`. Discovery only. No production changes, clicks, serving-JVM evals,
or Lean builds. Source hashes and a dated, read-only warrant census accompany
this report in `improve-4-evidence/`.

**Finding:** Joe has specified both attested incremental results and preferences
about the route. Neither requires choosing between outcomes and process again.
The missing join is an authorised, versioned mapping from a wanted condition to
its admissible evidence, including when and for which occurrence that evidence
must hold. The test registry supplies execution warrants, not that semantic
mapping. Existing checkpoints supply some route evidence, not a generic
institution monitor. An IAD profile must use all design principles; turning its
eleven entries into weighted scores would contradict Joe's correction.

**Correction to the handoff's historical premise:** production does pass `:spec`,
not `:c-fn-pointwise`, but `:spec` is no longer necessarily constant in time.
`live_c.clj:324–344` validates terminal placement and uniform earlier C;
`war_machine.clj:6127–6128,6213–6244` threads the family schedule;
`efe.clj:1089–1090,1152–1161` passes it to the scorer;
`cascade_model_manifest.clj:678–699,770–782` constructs the member at each τ.
The earlier uniform member and terminal wanted-token member are different.
The improve-2 discovery already reports this for the reference runs (T=2).
There is still **no process-stamp schedule**, and arbitrary pointwise C remains
unused by production callers. Unscheduled legacy specs explicitly stay constant.
The 09-18 consolidated note's “zero production requirers” for live-C is likewise
historical: `war_machine.clj:6096–6098,6213` now derives and consumes it.

## 1. Specification inventory and current implementation

Line numbers below are at the hashed inputs, not moving main. Short names:

- **N** = `holes/NOTE-joes-view-of-C.md` (explicitly distinguishes quotes,
  records and inferences).
- **A** = `holes/labs/wm-contract/ANSWERS-C-questions-2026-09-21.md`.
- **P** = `holes/labs/wm-contract/PLAN-c-replacement-2026-09-20.md`.
- **I** = `holes/labs/wm-contract/SESSION-C-institutional-constraints-2026-09-09.md`.
- **D** = `holes/labs/wm-contract/SESSION-C-pattern-institution-duality-2026-09-09.md`.
- **X** = `holes/labs/wm-contract/DRAFT-apex-institutions.md`.
- **R** = `holes/labs/wm-contract/NOTE-test-registry.md`.

Classification: **P-space** = impossible in the represented action envelope;
**I-rule** = a rule with an observed check/typed denial or a proposed such rule;
**C-soft** = valued observation/route, needing a declared preference law, not an
execution ban. **Design** denotes an instruction about building the model,
not a new C weight; its relevant enforcement category is stated. Classifying
an agent's proposal does not ratify it. This inventories the concrete relevant
statements in the requested corpus, including repeated statements with distinct
content, rather than attributing every sentence of the agent-written notes to Joe.

| Specification excerpt and file:line | Class and present implementation evidence |
|---|---|
| “C has to do with outcomes” (N:34–36) | C-soft, **live**: token-set outcome preferences in `cascade_model_manifest.clj:678–706`, `efe.clj:1152`; Lean `PolicyHorizon.lean:49–63` computes risk over observations. Does not decide what a completion token attests. |
| “my preference is also about how the outcome is pursued” / “Ostrom's institutional theory with its deontics” (N:45–48) | C-soft plus I-rule distinction, **doc only as a general institutional model**. Particular review gates exist (runner:3956), but no institutional route observation enters C. |
| “not just 'harmony' either” / “design requirements / institutions” / “priority at some given moment” (N:49–52) | C-soft temporal/context preference; **partial**: terminal schedule exists, institutional/context priorities do not follow from it. `contextual_preferences.clj:1–3` explicitly describes fixture binding; not live Joe-derived institutional content. |
| IRC reply “cannot not land on IRC” (N:53–55) | P-space **example**, not a request for a C penalty. Futon3c peripheral registry constructs distinct runners (`peripheral/registry.clj:1–10`); no claim here that this audit commissioned the IRC invariant. |
| institution “checks for duplication and denies such duplicated submissions” (N:55–56) | I-rule **example**, not evidence of a WM duplication policy. No such generic institution is passed to cascade C; a future guard must name duplicate identity and the refusal boundary. |
| “prefer to focus on the WM topics” rather than interspersed infrastructure (N:57–58) | C-soft **doc only** as this explicit route criterion. Current wanted-token projection is not a measured interruption/focus observation. No ban on infrastructure inferred. |
| “dispreference for a highly chaotic unfocused workstate” / “it is carried state” (N:59–64) | C-soft with history; **partial** live aliveness source, no attested focus/history monitor. A one-step stamp without carried state cannot measure this. |
| “M- E- and T- files all describe optative conditions” (N:65–66) | C-soft source content, **partly live** via live-C and mission-hole wants. Neither generic document parsing nor a checkbox proves the condition. |
| “harmony/aliveness, completed missions, and capability stars” (N:106–109) | C-soft **live starter sources**, `live_c.clj` derivation and report assembly above. Not exhaustive; raw source weight is not evidence of achievement. Dark-room guard uses L, not H alone (N:113–121; implementation in `live_c.clj`). |
| “preferences are to be READ OFF the recorded evidence, with ruling as the fixing act” (N:88–101, a dated RECORD, not newly recovered verbatim Joe) | Design/I-rule for preference authority: `preference_module.clj:57` requires exact strength and authority at instantiation; no automated extraction may silently rewrite C. Corpus-derived utility remains separate from learned E. |
| “one correct, best-of, validated model” / “not happy to kick the can” (N:80–84) | Design, validation obligation/I-rule; existing tests and correspondence contracts help, but a Lean type alone does not implement the requested observations. |
| “It's really all of those” / “Attested work results” (A:11–16) | C-soft valued content with I-rule for admissible attestation; **missing live warrant→want join**. Do not recast Q1 as an unresolved choice among completion, attestation and disposition. |
| “an incremental work result is okay, and that exists” (A:16–17) | C-soft **not implemented as general warranted incremental credit**. `test_registry.clj:693–774` registers partial-work test scope; it does not imply whole-mission closure. |
| “preferences about what happened along the way” / “stamp several places … on your route” (A:18–23) | C-soft route plus possible I-rule completion condition; **doc only generically**. The metaphor does not specify which stamps, ordering, repetition, or mandatory completion gate. Concrete existing checkpoints are below. |
| “prefer if my specification was incorporated into the build result” (A:24–28) | Design implementation instruction; this discovery must lead to observable slices, not another indefinite deferral or demand to restate the specification. |
| “institutions as constraints on behavior … embodying preferences” (I:8, Joe verbatim, exploratory) | P-space/I-rule/C-soft decomposition proposed, **not a universal hard constraint adopted**. I:3–4 explicitly denies a schema ruling. Runner gates implement particular constraints; no generic institution-to-model compiler. |
| institutions “could consume design patterns” / cascade “just an informational object” / “interpreter for Cascades” (D:7) | Design/I-rule hypothesis; not a proven mathematical duality. **Partial live interpretation**: `cascade_policy` guard/transition interpretation and runner dispatch/fold; no generic institutional interpreter with roles/obligations. `cascade_structure` explicitly records unavailable authority evidence, not institutions. |
| “Which mission should come from an AIF outer loop” / “use my adaptation of Ostrom's principles from Table 2” (P:50–56) | Design plus I-rule profile; **dark mission-value enrichment**: `war_machine.clj:2361,2378` only definition/self-arity call across src/scripts. No IAD profile hits in futon2 src/scripts or futon3c/src (case-insensitive `iad`, excluding word-substring noise). Do not revive the removed judge path. |
| “all design principles … use them all and see if they work” / “not sure ratings can or should be used” (P:138–143) | Design/I-rule arrangements; **doc only as eleven-item profile**. P:145–153 supersedes P:80–104's proposed scoring. No IAD score, uniform weighting, or scalar readiness inferred. |
| reviewer “reruns the tests — strictly a waste” (R:3–6, recorded observation); “VERIFIES THE RECORD” (R:15, specification prose) | I-rule monitoring, **live registry**: `test_registry.clj:776–875` record checking; `915–954` separate review/adequacy/sample. Not called from WM token observation. |
| scope “wherever tests and handoffs are run” (R:78); environment “toolchain, JVM, dependency hashes” (R:79–80); “spot-check one test per review by default” (R:81–84) | I-rule **implemented registry facilities**: fingerprint:222–300, review!:915–954; R9 author check:923. Later futon2 `AGENTS.md:33–55` says check before run, refusal means run; do not read the earlier sampling prose as permission to rerun all suites. |

The eleven IAD entries are a design checklist, not eleven soft preferences to
sum. The requested plan transcribes/adapts Table 2; the original paper's Table 2
is on PDF page 3, and relates tests to producers' interactions with their place.
That grounds using evidence about practice rather than scoring principle names.
[Original paper](https://metameso.org/~joe/papers/corneli2016institutional.pdf).

| Plan specification excerpt (P line) | Enforcement reading; current concrete evidence / missing part |
|---|---|
| “Producers … clearly defined” (87) | I-rule roles/eligibility. Runner author/reviewer casting and refusal:3956–3970 live; no all-mission IAD ownership profile. |
| “boundaries of the Place well defined” (88) | P-space when tools structurally restrict scope; I-rule when checked scope denies effects. Registry code/test manifests:206 and runner target repository provide evidence, not an all-path scope proof. |
| “Process related to local conditions” (89) | I-rule admission/source binding: `interpretation_evidence.clj:54–121` joins occurrence, target and sources. Interpretation admission is not subsequent evidence of applying a pattern. |
| “benefits proportional to efforts” (90) | C-soft payoff **proposal, not calibrated**. Live-C projection exists; corpus weight is not measured effort or a demonstrated proportional payoff. |
| affected producers “modify operational rules” (91) | I-rule amendment authority, **profile/doc only**. Editable flexiargs show a proposal channel, not authorized acceptance or actual participant voice. |
| “tests document Producer-Place interaction” (92) | I-rule monitoring evidence: registry manifests/results live; semantic adequacy remains a distinct review. |
| “tests modifiable by Producers” (93) | I-rule amendment rights; in-tree sources show editability, not permission. No generic rights receipt. |
| “graduated sanctions” (94) | I-rule response policy: typed refusals exist, but **no inferred graded sanction schedule**. A denial alone is not graduated sanctions. |
| “rapid low-cost conflict-resolution arenas” (95) | I-rule resolution mechanism: bells/Agency are live infrastructure, not a measured latency/access guarantee for each mission. |
| “rights to devise institutions unchallenged” (96) | I-rule rights/authority, **doc only as profile criterion**; library authorship cannot certify absence of external challenge. |
| “nested enterprises” (97) | I-rule organization/profile: mission/scope hierarchies supply placement evidence; no generic nested authority or sanction propagation. |

The apex draft must not be promoted to Joe's law by quoting its v1 tables:

| Draft statement / correction | Classification and code status |
|---|---|
| “claims without a witness verdict MAY NOT be counted” (X:22); disposition ≠ progress (X:96–102) | I-rule acceptance/credit proposal; attested refusal licenses a named knowledge transition, not the refused claim. Current `token_outcome.clj:48–85` distinguishes false/missing observations from build disposition; registry separately distinguishes a successful run. Neither is a universal credit institution. |
| detector “exists … wired … commissioned” (X:41); “bookkeeping … NOT established” (X:68–82) | I-rule proposed gate, **not proved live by the signature audit**. Earlier “monitors already exist” withdrawn. Do not use the audit's count as a commissioning stamp. |
| “SAME CLAIM AND ITS PRODUCING PART” (X:118–125) | I-rule independence; existing runner unequal IDs and registry reviewer≠author are partial checks, not proof of causal independence. |
| “execution start, acceptance, or credit?” (X:127–140) | I-rule **pending Joe**; draft recommendation gates acceptance/credit, permits detector-building. No exception invented here. |
| cue “is never evidence” (X:150–158); “carriage alone is not evidence of use” (160–170); agent cues confer “no authority” (172–177) | I-rule proposed entry/obligation monitors; **doc/provisional** at X:189–193. A card or cue is not an observed application, commissioning or success stamp. |

Lean today supplies the formal place for time-indexed preferences, not these
institutional predicates. `PolicyHorizon.lean:49–63` defines step risk and the sum
at τ=1…T; its fixture at :306 demonstrates changing the second member changes
ranking. `ZeroPreferenceExclusion.lean:40–56` connects infinite risk to selection
exclusion. Neither theorem stops an actuator or authorizes a new zero. N:71–79
and N:211–214 explicitly mark the three-way institutional decomposition as an
inference/open question; this report uses it as requested analysis, not a ruling.

## 2. Attested incremental work: the missing semantic join

A registered run attests that the named tests/command ran successfully against
recorded code, tests, environment and loaded dependencies, with stable inputs
and a pinned log. A current check adds freshness relative to the supplied review
scope/environment. Hash integrity is **not authentication** (`test_registry.clj:3`).
It does not prove test adequacy, whole-mission completion, a particular runtime
observation, independence of the test author, or every claim in the agent reply.
The reviewer provides adequacy judgment; merely re-running supplies none of it.

The live observation boundary is narrow:
`observation_checks.clj:143–165` dispatches C3 path, C4 declaration, C5 contract
bundle entry, C6 witness-reference existence. Its docstring says “No warrant
service is consulted.” C5 is **not** the test registry. C6 resolves repository
and revision, not the referenced claim's validity. `token_outcome.clj:48` consumes
signed measurements; `full_loop_runner.clj:2848` records the comparison. Thus
fix-10a's receipt is the right downstream carrier once a warrant-backed observer
exists, not proof that it exists now. Old C1/C2 scripts under `scripts/wm04/`
are historical examples, not current observation-class dispatch.

Proposed versioned binding (record-only in slice 1):

```
{:target "M-..." :want ["M-..." :increment/...]
 :criterion {:id ... :version ... :source-sha256 ...}
 :scope {:repo ... :revision ... :paths [...] :tests [...]}
 :warrant-id "test-registry-..." :claim ...
 :adequacy {:reviewer ... :receipt-id ... :verdict ...}
 :occurrence {:run-id ... :attempt-id ... :role ...}
 :applicability {:kind :persistent-result-or-this-attempt :cutoff ...}
 :authority {:declaration ... :version ...}}
```

Do not infer this from a test namespace, `:origin`, target name in a path, or a
similarity search. Start with a mission's explicit sub-result criterion, retain
the test's positive and bad-case witnesses and its adequacy review, bind to the
registered run, check registry integrity/freshness and applicable scope, then
emit `{:observed true :evidence ...}` only for that named token. Keep the original
want meaning: a declaration token cannot silently become a correctness token.
For the updater, a small warrant for the typed comparison helper may satisfy
“comparison receipt implemented and checked”; it does not satisfy “the shared
posterior updater is working” or “mission closed”.

Incremental credit is a distinct declared token/state transition, deduplicated
by criterion/version and result identity. Re-checking or re-registering identical
work is not new progress. A persistent capability can remain true at later ticks;
if Joe values novelty, that requires a *newly established* transition observation,
not paying each tick for a persistent Boolean. A correctly detected refusal can
satisfy a separately declared negative-case knowledge criterion, never the
positive claim it refused. Whole-mission closure requires its own acceptance
conditions; no arithmetic percentage or new weight is inferred here.

Refuse/retain typed missingness for absent binding, unknown criterion/version,
wrong target or occurrence, wrong revision/scope, unsupported tests, stale
SHA/closure/environment, changed inputs, missing/mismatched log, failed/zero-test
run, unknown adequacy, ineligible reviewer, post-cutoff evidence, or duplicate
credit. Registry's existing reasons include `:missing-or-mismatched-run-intent`,
`:not-a-warrant`, `:unsupported-results`, `:stale-sha`, `:environment-mismatch`,
`:log-object-missing`, `:log-mismatch`, `:results-log-mismatch`, and
`:parser-superseded` (776–875). `check-record!` returns `:outside-closure`; a
consumer must inspect it, not read `:warrant? true` as coverage of that diff.
A missing/stale measurement is **unknown**, not observed false or zero preference.

### Today's census (UTC; bounded and reproducible)

Read-only Agency GET, explicit since `2026-09-21T00:00:00Z`, snapshot at about
16:38 UTC: **218 entries = 109 intents + 109 runs; 102 runs declare `:warrant? true`**.
These are registration-time warrants, **not 102 freshly revalidated warrants**.
No review-kind entry occurs in this dated tag query; this does not mean nobody
reviewed the work outside the registry.

Of those 102, **0 carry any structured mission/want/token key**, while **6 mention
mission IDs in payload strings (including file/loaded-source paths)**. The six
IDs and exact mentions are in `census-summary.edn`; they are discovery candidates,
not six semantic mappings. All registry subjects are session/run subjects by
construction (`test_registry.clj:535`). The append-only subject index has **144
records, 88 latest distinct subjects, 0 mission-named subjects**. One lexical
want match is `proposal-supply-b1/wants`; it names a test subject, not a qualified
wanted token. Consequently **0 explicit wanted-token bindings were found** in
these two inspected interfaces. This is not a claim that no external handwritten
mapping exists anywhere in the workspace.

The broader count endpoint reported 667 all-date tagged entries, but the default
list returned 412 because `http.clj:2805–2820` imposes a recent-window default on
broad list requests. I did not misreport 412 as the all-time total. An older-window
read timed out at 55 seconds; the dated census instead uses an explicit since
query and a separately retained count. No all-time completeness claim is made.
The compressed original dated response and subject index are retained; the census
test parses **all** payloads, checks unique IDs/date/envelope counts, and includes
negative controls distinguishing a path mention from a semantic key. It does not
call a server, run reviewed tests, or assert semantic sufficiency from a key alone.

## 3. What route stamps could observe, and which τ

A stamp is a **checked occurrence of a declared predicate**, with subject, actor,
role/authority, rule version, evidence digest, event time/order, and verdict
(satisfied / violated / pending / observation-missing). A planned action, prompt,
card or checkpoint's mere presence is not a satisfaction stamp. A stamp can
observe an institutional guard, yet the guard must remain at the execution
boundary; making compliance preferred never substitutes for enforcing it.

| Candidate stamp | Existing observable evidence and limit | Proposed placement; not current model semantics |
|---|---|---|
| Test/negative-case specification registered before dispatch | Registry intent/run time plus scope, independent adequacy/commissioning receipt if required. A post-build passing test does not establish pre-dispatch registration. | τ=0 admission/input state if already required at dispatch; first route transition if registering it is itself planned work. Distinguish specification-before-work from test-execution-before-work. |
| Local interpretation admitted | Construction admission/source receipts bound to target/occurrence; `interpretation_evidence` source and identity checks. | At construction/admission boundary; carries evidence already available at τ=0 for a later build policy, rather than rewarding it as a future event again. |
| Author produced an independently reviewed incremental result | Dispatch/build jobs, exact retained prompts/replies, commit/readback and reviewer result. A distinct agent ID alone is partial independence evidence. | Observed at review completion, before acceptance/closure; model a review transition and its possible rejection, not deterministic success. |
| Specified pattern was actually applied | Selected precedence and interpretation receipts are **not use evidence**. Need event binding pattern/version to actual action, affected artifact and observed effect; agent text alone has its declared evidence class. | At the corresponding enactment transition; cannot derive from the selected plan or shape receipt. |
| Feedback was received / conflict resolved | Agency delivery and reply can provide narrower transport evidence; an author revision or standing decision can show response. Neither merely naming a channel nor sending a prompt proves participant receipt/understanding. | At feedback/revision boundary; separate sent, received, resolved predicates and missingness. |
| Work retained WM focus | Ordered task transitions with mission/topic and authorized scope, including interruption causes; no current complete focus observer found. | Carried monitor state over transitions; preference over interruption/focus observations at chosen steps, not an endpoint mission ID alone. |
| Attested result credited | Verified warrant→criterion binding and checked adequacy; signed token measurement. | At result acceptance; separate persistent achieved state from one-time new credit. |

The cohort already records `:cohort/id`, `:attempt/id`, `:event/sequence`,
`:checkpoint/type`, `:recorded-at` and payload (`full_loop_cohort.clj:427–494`).
`append-checkpoint!` at :576 validates cells and order; closed retention also
checks admitted evidence cutoff. The stages time-step, selection, construction,
dispatch, build, adjudication, closed are useful observation locations, but are
**not** the scorer's τ=1…T. Today's T=2 token rollout is not a seven-checkpoint
institutional trajectory. Timestamp order alone cannot bridge those grains.

For a later declared model, let state carry relevant history/obligations and let
O expose `(result tokens, process stamps, pending/violated/missing status)` on a
common carrier for every compared policy. Declare the mapping from execution
boundary to model step before scoring: e.g. action1=author/sub-result,
action2=review, action3=accept, with stamp observations after each action.
This is an example schema, **not** a choice of T=3 for production. Preexisting
warrants belong in initial state; a prerequisite missing at dispatch triggers
an I-rule denial if the rule says mandatory, not a fake future stamp.

Cτ then prefers particular **observable stamp/result configurations** at the
specified step. B must predict the actions and monitor updates; A must distinguish
evidence from assertion. If a process condition depends on an earlier event,
carry it in state so the declared model remains adequate. Preference over a
history can be expressed on that augmented state/observation; arbitrary marginal
Cτ on endpoint-only observations cannot recover order. A repeated satisfied stamp
at every later step can multiply its cost/reward, so the schedule must declare
once-at-boundary versus persistent preference. Deadline violations need a monitor,
not only a local permission check.

The existing sparse schedule only supports terminal/every-step placement, not
arbitrary milestone laws. The callable pointwise alternative avoids neither this
model work nor normalization: `cascade_model_manifest.clj:789–791` notes no domain
declaration there, and :860 refuses it on the nonidentity-rate path. Extending
serializable preference members/schedules with explicit domains is preferable to
passing an opaque function and losing the certificate. Lean's generic
`horizonEFE` already accommodates declared Cτ; its type supplies no institutional
content, normalized masses, timeline mapping, or observation rates for free.

Hard action-space exclusions, institutional denials and soft preferences must
stay distinct in records. A missing action cannot be selected; a denied attempt
is an observed rule response with authority; an unfavored but allowed route may
still be selected. Zero C gives infinite risk for positively predicted outcomes
under the relevant assumptions, not physical impossibility. Do not infer zeros,
penalties, reward sizes, or sanction severity from a principle's name.

## 4. Slices — one observable behavior each

1. **Record-only attestation/stamp account.** At close, join supplied evidence to
   versioned criterion declarations; record each binding's applicability,
   matching evidence or typed gap, and which existing checkpoint supplied it.
   Preserve all eleven IAD profile entries as artifact/gap statements, never a
   score. Default behavior/scoring unchanged. Acceptance: frozen reference run
   keeps updater-negative despite grounded-change; a scope-matched incremental
   warrant is distinguished from a mission closure; absent binding stays missing;
   prompt/selected-pattern/post-dispatch registration cannot masquerade as the
   named stamp. Assert byte-identical decisions and no real store writes. This
   incorporates the specification into retained build evidence immediately.
2. **A single warrant-backed observation class, declared-switch default off.**
   For one explicitly declared incremental token, check registry + scope +
   adequacy/applicability, emit signed measurement consumed by existing token
   comparison. Acceptance: real registry positive control and planted stale SHA,
   uncovered diff, wrong mission/revision, false adequacy, future evidence and
   duplicate IDs; unknown remains unknown. No change to existing token meaning.
3. **One institutional ordering monitor, record-only first.** Pick the already
   specified author/reviewer/result sequence; produce satisfied/violated/pending
   receipts with event/rule versions. Pin the exact before/after requirement;
   distinguish admission, pattern use and commissioning. Acceptance includes
   late, missing and wrong-actor evidence, plus a real admissible case so an
   always-denying monitor cannot qualify.
4. **One authorised acceptance guard.** After Joe settles the blocked boundary
   for the chosen rule, enforce it at that boundary using the monitor receipt.
   Acceptance: induced violation gives typed denial; good case proceeds; all
   relevant execution entry points checked. Do not substitute a C penalty or
   invent a signature-builder exception.
5. **One process preference experiment, declared-switch default off.** Introduce
   a common augmented observation domain and serializable Cτ schedule for a
   soft criterion Joe has specified, after its strength/composition is declared.
   Acceptance: two routes with identical terminal results but different observed
   stamp order have different intended risk; swapping stamps swaps that effect;
   identical routes tie; missing measurement is not noncompliance; normalization
   holds each step; disabled path is byte-identical. Trace B/A/C/domain/schedule
   provenance and held-out observation support. Preserve improve-2's outcome
   odds/source budget account and avoid double-counting outcome and process.
6. **Incremental credit without repeat payment.** Once its rule is declared,
   record newly established criterion/version once, preserving persistent truth
   separately. Acceptance: same warrant/result replay yields no new credit;
   genuinely distinct sub-result does; negative-case knowledge never closes the
   positive claim. Do not reset habit identities or override fix-8's observed
   outcome reinforcement rule incidentally.

Concrete decisions reserved to Joe (not requests to restate the whole theory):

- For violation-signature-before-work, does missing commissioning block **dispatch,
  acceptance, or credit**? For the bounded work that builds the detector itself,
  which named acceptance conditions apply? These are X's existing open rulings.
- For a route stamp used to complete a work unit, which first condition is mandatory:
  test specification registered before dispatch, independent adequacy review before
  acceptance, or both? These have different evidence timestamps. The existing
  general desire for route stamps does not choose their sequence for a mission.
- For WM focus, is an explicitly authorized infrastructure interruption merely
  less preferred, neutral, or outside the task's permitted scope? This determines
  C-soft versus guard semantics; no numeric odds proposed by the text.
- After a concrete incremental criterion and route pair are prepared, what relative
  preference strength/composition should distinguish them? Improve-2's unanswered
  budget-scope/odds questions remain; its “what is valued” question is answered.
- If a graded sanction is to be implemented, who adjudicates the named breach and
  what response applies? Typed denial alone is not that ruling.

None of these questions blocks slice 1. No permission to weaken existing guards,
weight the eleven principles, or reinterpret completion claims is implied.

## Reproduction and validation

`improve-4-evidence/` retains compressed read-only API/index snapshots, the census,
source hashes, exact query URLs, and fresh check output. The source survey used
`rg` over futon2 src/scripts/test and futon3c/src, then read each relevant call
site; external Lean files were only read. The original paper was read, not built.
The census namespace reads only retained snapshots in its own CLI process.
This is discovery, so no failing-on-main implementation regression is claimed.
The negative control checks the specific analytical error: a mission name in a
loaded-source path must not count as a structured semantic binding.

```sh
# /home/joe/code/futon2-narrative-improve-4
clojure -M:test -m cognitect.test-runner -n futon2.report.c-process-discovery-test
clj-kondo --lint test/futon2/report/c_process_discovery_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults test/futon2/report/c_process_discovery_test.clj
```

Environment: Linux, Java 21.0.11; Clojure CLI 1.12.5.1664 / Clojure 1.11.1.
Registry warrant scope is snapshot census integrity/classification, not freshness
of the surveyed warrants, commissioned institutions, or an implemented C change.

Fresh checks: **2 tests / 7 assertions, zero failures/errors**; clj-kondo zero
errors/warnings; check-parens OK. Explicit dated list count and separate dated
count endpoint both returned 218. No existing production suite was rerun.
