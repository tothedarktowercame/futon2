# War Machine readiness campaign — 17 October 2026

## Campaign status

Prepared, not started. Joe authorized beta = 1 on 2026-10-17 as the neutral
starting precision for this five-click readiness campaign, whose purpose is to
make the system ready to tune. It is not claimed to be learned or calibrated.
The sealed declaration is
`resources/wm/reading-precision-beta/wm-readiness-2026-10-17-v1.edn`, bound to
reading-model identity
`f9c5cf02df9f933d2b554efbcb7b70ceb2ce1dddf595d35c0bd88245ae4186b2`, with
declaration digest
`72526b4b8b64dd084c67deb9903703b3290da0b494141c2e308a7dfb4ca898c0`.

Before click 1, the serving JVM must load and register that exact declaration,
then read it back by identity and verify the seal. Later clicks may carry beta
only while the reading-model identity is unchanged. An identity change pauses
the campaign for a new explicit declaration; neither the debugger nor the
runner may invent or silently migrate the value.

## Fixed Zai cast

Every click in this campaign will pass this cast explicitly; there is no roster
fallback:

- author: `zai-2`
- reviewer: `zai-6`
- repair reviewer: `zai-7`

At 2026-10-17T19:32:35Z all three Agency sessions were reset through the
`reset-session` boundary. Their prior session identities were discarded. The
post-reset roster reported all three as `idle`, `invoke-ready? true`, with no
session identity; first use will therefore create fresh sessions.

## Recording contract

For each of the five clicks append:

1. authorization and durable budget receipt;
2. exact cast and fresh session identities;
3. click, run, attempt, and selected target/cascade identities;
4. phase work time, wall time, and debugger dwell separately;
5. every held condition, repair dispatch, verdict, commit, reload, and restart;
6. run record, report card, requirement verdict, and publication paths;
7. whether a tick was carried and what grounded work changed;
8. unresolved defects and the evidence required before the next click.

The five clicks run sequentially. A debugger repair resumes the held phase of
the same click and must not spend another click or rerun outer selection. A
failure requiring operator authority pauses the campaign instead of repeating
the same predictable failure.

## Runs

### Click 1 — selection stopped by an obsolete/undeclared loop-node contract

- Authorization/load preflight: the five-click grant was recorded by raising
  the ordinary-click allocation from 81 to 86. The serving JVM reported five
  available clicks and read back the intact beta declaration for the exact
  identity above. Zai-2, Zai-6 and Zai-7 were reset immediately before issue.
- Click: `wm-click-454f8ade-720e-4ade-bec4-a0381b552aba`.
- Run: `2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492`, attempt 002.
- Selected target under construction: `M-recommendation-bindings`; one
  occurrence-keyed cascade with twelve pattern occurrences and validated
  four-map squares reached the selection gate.
- Terminal result: `build-failed`; no tick carried and no mission work was
  enacted. Run record:
  `data/wm-runs/tick-run-record-2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492.edn`.
  Report card:
  `data/wm-runs/2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492/report-card.md`.
- Timing: 563,598 ms wall time, comprising 164,084 ms active selection and
  369,049 ms debugger dwell. Within selection: meta-field ranking 76,863 ms,
  strategic judgement 54,836 ms, strategic scans 20,374 ms, G admission
  6,282 ms, declaration/interpretation loading 5,111 ms, and the remaining
  measured subphases below 0.2 s each.
- Held condition: `required-loop-node-unexercised`. R6, R13 and R14 were
  legitimately bypassed because this run had one admissible policy and one
  action group. CTAU-CLASS was refused because the current reading-policy
  scorer retains progress-preference evidence, whereas the old gate requires
  a `:class-emission` observation model from the retired class-scoring route.
- Debugger handling: Zai-7 diagnosed the held phase under job
  `invoke-1792266440527-16272-d984469f`, then confirmed the terminal record
  under `invoke-1792266841488-16282-3cc3a535`. Both returned
  `cannot-repair`; no code was committed or hot-loaded. The second dispatch
  was the loop seeing the same terminal failure after abort, not an independent
  defect.

Disposition before click 2: do not click again unchanged. Joe must choose
whether CTAU-CLASS is (a) re-declared for the reading route, (b) implemented by
adding a genuine class-emission model to that route, or (c) formally retired
from the registry, Lean contract and live gate. Independently, the scorer's
observation-model evidence is currently lost when `family-selection` replaces
the scorer certificate's `:f`; retaining that evidence is a real repair but is
not by itself sufficient to settle CTAU-CLASS.

### Click 1 repair disposition — CTAU-CLASS implemented, not waived

Joe chose option (b): the reading-policy route must carry a genuine
class-emission model. The repair makes the two observation channels explicit:

- the progress model supplies progress risk, ambiguity and transition
  information gain;
- the class-emission model emits `ending/not-yet-evaluated` before the horizon
  and, at the horizon, emits `focused` when the already-selected outer target's
  acceptance tokens were reached or `stop-the-line` otherwise;
- its terminal class risk and ambiguity are added to G. Transition information
  is consumed once from the shared rollout rather than counted twice;
- the model, its score certificate, and `consumed-in-g?` receipt survive the
  scorer → family selector → policy selector boundary as `:class-emission`.
  Xiang reading fit remains the distinct `:computed-f` carrier.

The launch gate now accepts CTAU-CLASS only when every compared candidate has
that explicit model, a numeric class risk, and `consumed-in-g? true`. Merely
placing a class-shaped map in a fit or certificate field does not pass.

The pre-click scanner now invokes the loaded production scorer's bounded
`class-emission-readiness` probe. A missing function, malformed class model,
or class receipt that does not contribute to G is reported as the predictable
`cannot-repair` blocker `reading-policy-class-emission`; it therefore stops
before a click or agent dispatch. Its negative control for an unconsumed class
receipt is part of the focused gates.

This changes the reading scorer from
`:wm/cascade-shape-g-score-policy-v1` to
`:wm/cascade-shape-g-score-policy-v2-class-emission`. Consequently the beta
declaration recorded above is not silently migrated: click 2 remains blocked
until an offline production-family replay yields the new exact reading-model
identity and a new sealed beta=1 campaign declaration is registered for it.

Offline repair evidence, commit `48e8ae2d0`:

- the scorer/preference/family/selector/CTAU focused gate passed 51 tests and
  268 assertions with no failures or errors;
- the readiness negative control rejected a class-shaped receipt whose cost
  was not consumed by G;
- changed-file lint had no new warnings (the sole warning in
  `full_loop_runner_test.clj:7109` is also present at the untouched base),
  Python compilation and `git diff --check` passed;
- the serving JVM was hot-loaded and its production scorer probe returned
  `:ready`, scorer v2, model kind `:class-emission`, `consumed-in-g? true`,
  with recorded G exactly equal to the independently recomposed G;
- the old all-of-`full-loop-runner-test` comparison was stopped after it
  reproduced the same ambient `:no-selection` failures at untouched base. It
  is intentionally not a readiness gate: it is slow, state-sensitive, and
  did not discriminate this repair. The bounded cross-layer gate above does.

The upgraded scanner therefore clears the former predictable
`cannot-repair` condition in the serving JVM. It still reports NOT-READY for
independent reasons: the latest recorded workspace/CI receipts do not name
this commit, the runner code receipt names an older tested commit, and the
reviewer seat was unavailable at the instant of the scan. These are launch
preconditions, not reasons to spend click 2.

### Click 2 launch cleanup

The launch bookkeeping was repaired without a broad suite rerun:

- readiness now consumes test-registry warrants for the seven namespaces in
  the changed execution closure. Four warrants remained fresh and were reused;
  only `full-loop-runtime`, `reading-policy-precision`, and
  `reading-family-decision` reran and minted new warrants;
- the old all-repository “everything conforms” receipts were removed from the
  click-launch predicate. They remain maintenance evidence, but unrelated
  repository commits and the pre-existing untracked resource receipt no
  longer invalidate an unchanged tested closure;
- serving-code readiness compares the loaded runner bytes with the current
  disk bytes. The serving JVM reports them equal and current;
- the WM campaign cast is now Zai-2 as author, Zai-6 as reviewer, and Zai-7 as
  repair reviewer. All three sessions were reset before assignment and the
  readiness scan requires all three to be idle and invoke-ready;
- Joe's beta=1 authorization is installed as sealed campaign authority
  `3387e9ed…f4464`, limited to four distinct reading-model identities—the four
  clicks remaining after click 1. Each use still creates and records an exact
  identity-bound declaration; there is no silent carry across model changes.

Final non-clicking scan: READY. Contract and trace-schema checks, all seven
warrants, serving runner bytes, CTAU class emission, Zai cast availability,
bounded service capacity, and the run-certification command all passed. No
click or agent dispatch was performed by this cleanup.

### Click 2 — concrete cascade selected; identity binding rejected

The second campaign click ran with the explicit Zai cast (Zai-2 author,
Zai-6 reviewer, Zai-7 repair reviewer) and the debugger loop enabled.

- click: `wm-click-e3a92275-d032-450b-a55a-894dc75cd3b9`
- run: `2026-10-17-a94d597b-d0f2-4314-ac21-c13fcfde5c7a`
- target: `M-recommendation-bindings`
- selection: one concrete occurrence-keyed reading policy containing twelve
  occurrence nodes
- budget: one click spent; no tick carried
- beta authority: one exact reading-model identity issued under campaign
  `3387e9ed…f4464`; three authorized identities remain

The reading and selection route reached a concrete cascade, so the earlier
CTAU class-emission and beta launch blockers did not recur. The selected
cascade was then rejected by the downstream identity check as
`:identity-forged`. That is the substantive blocker exposed by this click.
It remains unresolved and must be reproduced and repaired before click 3.

A secondary terminal-recording defect obscured that result: the runner passed
the typed outcome `:refused` to the cohort ledger even though that ledger does
not admit `:refused`, causing `invalid-close-outcome` and degrading the public
outcome to `build-failed`. Zai-7 repaired only this classification boundary in
commit `ee7275ffc`: downstream refusals now close the cohort with its admitted
outcome `:abstained` while preserving `:identity-forged` as the failure kind.
The namespace was hot-loaded without restarting the service. Its focused
regression passed 3 tests and 10 assertions and minted warrant
`test-registry-5b709316723d2e34484d1a5425d77d6258c15c02415eb24a6ae7cce0a6b4aed4`.

Disposition before click 3: NOT READY. Do not spend another click merely to
rediscover `:identity-forged`. First establish which digest changed between
the pre-selector cascade receipt and downstream verification, repair that
binding, and add it to the bounded readiness replay. The classification repair
does not waive or solve the identity failure.

### Click 2 blocker repair — retain the cascade that G compared

The `:identity-forged` result was a genuine verifier finding caused by an
incorrect seam, not a forged selection. Before selection, Packet B constructed,
validated and digest-bound a canonical cascade. After selection, the runner
discarded that record and called the constructor again with only the selected
action and a smaller receipt map. The second construction therefore had a
different identity, and the verifier correctly rejected it.

Commit `6b9f02660` removes that second construction from the production path:

- family selection attaches the exact chosen canonical record to the
  decision's selection certificate, outside the action whose identity it
  helps certify;
- `selected-entry` carries that record to the held execution phase;
- the execution seam requires its digest to equal the selected action's
  digest, independently re-digests it, and re-runs the canonical validator;
- only that verified record is persisted and refined. Direct non-production
  callers without a retained record keep the former construction fallback.

The regression deliberately supplies a different post-selection reading
source. The seam now ignores it as a replacement for the already-selected
object and reaches the expected open-cascade phase. Existing substitution,
re-digest and validation controls remain in the identity suite.

Focused gate: 11 tests, 102 assertions, zero failures/errors. Changed-file
lint has zero errors and no new warnings; `git diff --check` passed. Warrants:

- cascade runner seam:
  `test-registry-05fc959fa0a93160f485d9c905a2b799898b93087d0fc527ff41c4c623254901`
- selection/cascade identity:
  `test-registry-9c973ae91e527b3c512e8bdf408db644846d9d4b746bc55ed9c76ab45e9a14fc`

The serving JVM hot-loaded `family-selection`, `cascade-decision`, and
`full-loop-runner` in dependency order without restart. This clears the
specific click-2 `:identity-forged` blocker. It does not authorize or spend
click 3; the ordinary readiness scan and remaining campaign authority still
govern that launch.

### Click 3 — valid fenced author answer rejected, decoder repaired

The bounded readiness scan first found three self-clearing stale warrants;
only those namespaces were rerun and reminted. Zai-2 was then reset after its
prior job completed, and the scan returned READY. A cast-admission race caused
one launch attempt to be refused before allocation (`click-id` absent, zero
budget spent); the loop waited and retried without treating it as click 3.

The accepted click was:

- click: `wm-click-5497b0af-dd89-4347-b9e7-7db8f55f4f86`
- run: `2026-10-17-a9af4bb8-12c3-4fa5-a74b-7d0dc861da40`
- target reaching the reading boundary: `M-pattern-retrieval-calibration`
- outcome: `abstained`; one click spent, no tick carried
- beta campaign: no new reading-model identity issued; three remain

The sole non-universe refusal was
`:selected-target-reading-answer-invalid/before-cascade-construction`.
Zai-2's completed Agency job had returned exactly one valid JSON analysis
object wrapped in a single Markdown `json` fence. The requisition did not
forbid that wrapper, but the decoder passed the backticks directly to the JSON
parser, caught the parse exception and mislabeled the correct answer invalid.

Zai-7 repaired this in commit `0a45789b8`: the selected-target-reading decoder
accepts exactly one fenced JSON object. It still rejects prose outside the
fence, multiple fences, non-JSON and missing answers. The regression reproduces
the live answer: the old decoder returns nil and the new decoder returns the
analysis map. Focused validation passed 15 tests and 62 assertions, with zero
failures; changed-file lint had zero errors, check-parens passed, and the
namespace was hot-loaded without restart. Focused warrant:
`test-registry-295b34c4729173a7a971c7c350a89d97808cdfa0d78fa3b426e0698d92c9dc1f`.
The repair finding was published in commit `c91d0d624`.

Click 3 is therefore accounted for but did not advance enactment. Click 4 must
not be described as a continuation of this run: the run ended. Its readiness
scan should include the new selected-target-reading warrant and confirm the
same explicit Zai cast before a separately authorized launch.

### Click 3 follow-up — JSON handoff representation consolidated

The fenced response was a class defect, not merely a target-reading typo.
The shared handoff namespace already normalized Agency response envelopes,
but it did not own the final textual JSON representation. Consequently the
selected-target-reading and occurrence-square authoring paths each decoded
agent text independently, while the D4 author/reviewer path used the shared
decoder; all three could disagree about an otherwise identical response.

The representation rule now lives once in `futon2.aif.handoff-contract`:
an agent-to-machine JSON response is either a map, one bare JSON object, or
one JSON object enclosed by exactly one whole-response Markdown fence.
Outside prose, multiple fences, arrays/scalars, malformed JSON, and missing
answers remain invalid. This normalization only extracts the object; every
consumer still performs its own schema, identity, digest, and request-binding
validation.

The target-reading compatibility entry point, occurrence-square authoring,
and D4's Agency response-map now all use that rule. Behavioural regressions
exercise a fenced reply through occurrence-square publication and fenced
author plus reviewer replies through the production D4 composition, in
addition to the shared strictness controls. The focused gate covers 43 tests
and 165 assertions with zero failures/errors; changed-file lint and
`git diff --check` are clean.

The codebase sweep deliberately excludes JSON read from files, HTTP APIs, and
subprocesses: those are not agent textual handoffs and accepting Markdown
there would weaken their protocols. Existing EDN handoffs also retain their
separately specified fenced-EDN grammar. Thus the repair centralizes the
agent JSON boundary without turning every JSON parser into a permissive one.

Implementation commit: `c70495fc9`. Focused warrants:

- shared handoff contract:
  `test-registry-31ad9e60ef96c7df718738e78ddc5d619021372ef91167c4f185abbd7b63c749`
- occurrence-square authoring:
  `test-registry-3d50d05d07b6770414711724e8537d0791569a9e0888894166d5cd1b5615fdc7`
- selected-target reading:
  `test-registry-11974e9f67fe259a58ef333b7e45e9bb801a297a3cf14ca2031355512f99b7d1`
- production runtime/D4 composition:
  `test-registry-412fdce44ad49a877c118ef71183e7047deb56e1af97269e1d17460d5fed0875`

The serving JVM reloaded the shared contract and all three consumers in
dependency order. A serving-process probe decoded a fenced object as
`{:live true}`. No click was run.

### Click 4 — unique fenced object followed by a completion note

Authorized click `wm-click-193999b9-58d5-4f09-9f99-9454869fa39e`, run
`2026-10-17-9e3d4379-35ba-480d-b067-5a4c42c0329f`, used the explicit Zai
cast (`zai-2`, `zai-6`, `zai-7`). Preflight reported 13/13 tripwires clear,
the serving runner identity current, and no cast/single-flight conflict. The
run traversed selection and outer-learning publication in about 200 seconds.
It spent one click, carried no tick, and ended `abstained` at
`:selected-target-reading-answer-invalid/before-cascade-construction` for
`M-pattern-mining`.

The debugger found that this was not malformed JSON. Zai-2 returned exactly
one fenced, validator-compatible JSON object followed by one plain-text
completion note. The newly centralized rule required the fence to occupy the
whole response, so it rejected an answer whose machine payload was still
unique. This is a representation-policy defect at the same shared boundary,
not a target-reading semantic failure.

Repair commit `03c8830af` changes the shared decoder to extract exactly one
fenced JSON object anywhere in the response. It continues to reject multiple
fences, unfenced prose, arrays/scalars, and malformed JSON. The regression
uses the actual live answer shape; 22 tests and 103 assertions pass with zero
failures/errors, and the actual saved reply decodes under the repaired rule.
The loop hot-loaded `futon2.aif.handoff-contract` without restarting the
service. The completed run was not relabelled or replayed: click 4 remains an
abstention, and any further click requires separate authorization.

### Click 5 — validation/publication contract mismatch

The fifth and final authorized campaign click was
`wm-click-0b708330-60fa-4b40-b00d-014340ea471d`, run
`2026-10-17-173c5a65-fed9-4bc1-8aa2-b3b4df08dba5`, with the explicit Zai
cast (`zai-2`, `zai-6`, `zai-7`). It spent one click, carried no tick, and
ended `abstained` at
`:selected-target-reading-publication-refused/before-cascade-construction`
for `M-aif-a-matrix-faithfulness`. The response representation repairs held:
the answer decoded and passed the Python analysis validator.

The newly exposed defect was disagreement between two validation layers.
The published author contract and `session_turn_analysis.py` permitted a
fragment with no `pattern_refs`, while registry cascade construction required
at least one validated pattern reference on every fragment. Zai-2 therefore
returned a contract-conforming, shape-valid answer with no references; the
Python validator exited successfully, so the bounded correction round did not
run, and the stricter unpublished rule rejected all nine fragments only at
publication.

Repair commit `3037b082f` brings cascade validation into the same bounded
correction decision and reports the exact per-fragment requirement and answer
shape to the author. It also gives every validation attempt a separate request
and output path. This is necessary because the validator publishes by
exclusive creation: before the repair, a corrected second attempt collided
with the first attempt's output and could not be checked. The legacy output
name is refreshed only after a successful attempt for compatibility.

The planted regression begins with the live zero-reference answer, verifies
that one correction is requested, and then publishes the corrected reading.
`selected-target-reading-test` plus `handoff-contract-test` passed 23 tests and
109 assertions with zero failures; paren checking passed and lint reported no
errors (one pre-existing warning on an untouched line). The loop hot-loaded
`futon2.aif.selected-target-reading` without restarting the service.

Campaign result: all five authorized clicks were spent and none carried a
tick. Click 5 is not replayed or relabelled after repair. The campaign is
closed with no remaining click authorization; another live run requires a
new authorization and should be treated as a new readiness campaign.
