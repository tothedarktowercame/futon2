# War Machine producer/consumer handoff audit — 2026-10-16

## Scope and question

This audit covers the live War Machine boundaries at which one component or
agent produces a message, reply, file, receipt, or record which another
component accepts under a stricter contract.  It includes the production
runner, Agency transport, interpretation/reading jobs, author/reviewer turns,
and the run-record/report boundary.  It excludes pure in-process function
calls with no independently represented message, and controlled SUTs which
the production runner does not call.

The question at every boundary is not merely "is the consumer strict?".  It
is:

1. Does the producer receive the complete acceptance contract?
2. Is that contract carried as data, or copied into prose independently of
   the validator?
3. Are identity, source bytes, and the exact request pinned?
4. Does `done` mean only transport completion, with acceptance checked
   separately?
5. Is rejection typed, retained in the run record, and visible in the report?
6. Does a test exercise the real producer-shaped response through the real
   consumer, including a negative response?
7. Is retry/re-arm explicit, bounded, and attached to the failed contract?

## Evidence used

- Live click `wm-click-63d1c654-8fde-4a17-83d1-a756b29031b0`, run
  `2026-10-16-2a81e7f3-bef3-4c61-bdec-1cd7f031a017`.
- Agency job `invoke-1792126855136-15146-3707ed76`.
- The immutable request and returned answer under
  `storage/wm-target-reading-requests/17f82be101d56ab45fe6f5affb6dfd78afbca48f348f457e28050afa0f7cf7e5/`.
- Production composition in `full_loop_runtime.clj`, dispatch and phase logic
  in `full_loop_runner.clj`, and the dedicated request/validator namespaces.
- Focused tests named in the inventory below.  This was a source and recorded-
  run audit; it did not issue another click or rerun the full suite.

## Rating

- **A — bound:** the request carries the relevant schema/identity/pins, the
  validator is authoritative, rejection is retained, and producer-shaped
  positive and negative tests cross the boundary.
- **B — duplicated:** the consumer is strict and failures are typed, but all
  or part of its contract is independently copied into prompt prose.
- **C — hidden or unwired:** a producer can reasonably comply with what it was
  sent and still fail a hidden condition, the failure is lost, or production
  has no producer for the consumer at all.

## Complete live-boundary inventory

| # | Producer → consumer | Production entry | Contract and test evidence | Rating |
|---|---|---|---|---|
| 1 | Runner → Agency transport | `full_loop_runner/dispatch!` | Requires click id and the identical run-local job ledger before creating a job; registers every accepted job. `full-loop-runtime-test/selected-target-answer-is-built-with-the-run-local-job-ledger` crosses the production composition root. | A |
| 2 | Outer choice → selected-target reading author | `selected_target_reading/repair!` via `full_loop_runtime/production-defaults` | Target, source path, source digest, excerpt digest and immutable request bytes are pinned. The author is designated, not chosen from a fallback roster. The reply contract is prose copied from the Python validator. `selected-target-reading-test` and `full-loop-runtime-test` cover the live port. | B, repaired but structurally duplicated |
| 3 | Selected-target author reply → Python validator → reading registry | `selected_target_reading/repair!` | A `done` Agency job is parsed, independently validated, then published. The live run proved that the prompt and validator had diverged: the author used `evidence`, `sequence`, `elaboration`, and `authority`; the validator accepts only `context`, `condition`, `contrast`, `action`, `rationale`, `goal`, `dependency`. Commit `c231a882d` added the list and a prompt test, but the list remains duplicated. | C before `c231a882d`; B now |
| 4 | Selected-target repair outcome → final run record/report | `selected-target-reading-step` then selection checkpoint persistence | The step attaches the refusal to the judgment, but the cited live terminal record contains the completed author job only under model usage; it contains no `:wm/selected-target-reading-request-v1` refusal and still reports only `:no-current-target-reading`. The validator's exact stderr is therefore absent from the operator-facing report. | C |
| 5 | Selected reading policy → occurrence-reading responder | `full_loop_runner/selected-reading-phase` | The request is strongly self-describing: immutable policy key, debt, occurrences, library pins, response shape and responder identity. The validator seals receipts and checks all bindings. `wm.selected-reading-runner-test` has substantial positive and adversarial coverage. However production `full_loop_runtime/production-defaults` installs no `:selected-reading-ask-fn`; the live path always records `:selected-reading-ask-not-configured` when debt is reached. | C (consumer tested, producer unwired) |
| 6 | Mission want → interpretation responder | `wm.click-ask` → `flight_runner/ask-fn` → `want_interpretation` | The issued request is persisted and content-addressed. The prompt gives an exact fenced-EDN grammar and the response is rebound to the issued request before publication. The validator additionally runs source, constraint, construction and admission checks which are not fully enumerable from the advertised reply grammar. Tests cover parsing, binding, validation, revalidation and publication. | B |
| 7 | Selected legacy action → interpretation job | `interpretation_job/run!` | Sources and identity are pinned as files. The prompt requires a receipt, but tells the agent to read `interpretation_evidence.clj` and `fact_measurement.clj` for strict schemas instead of embedding or attaching a versioned contract. A namespace reload can therefore change the acceptance contract independently of the immutable request. Tests cover unavailable agents and invalid receipts, not schema drift between issuance and validation. | C |
| 8 | Selected action → implementation author | `full_loop_runner/author-prompt` | Prompt names target repository/base head, selected action, acceptance locators, evidence shape, gates, response markers, and refusal form. Artifact binding observes the actual repository and commit rather than trusting prose. Numerous runner tests cover missing cards, false commits, refusal, artifact mismatch and acceptance criteria. The contract is still spread across prompt text, regex parsers and artifact validators, but the independent artifact observation prevents most prose mistakes becoming success. | B |
| 9 | Author reply → build/artifact gates | `fresh-artifact-binding`, `resolve-target-build`, feature-card and acceptance gates | Transport completion is distinct from a fresh commit, correct repository, substantive files, feature card, and declared effects. Negative controls are extensive. Rejection is durable and can open a repair obligation. | A |
| 10 | Mechanical author failure → same-author build cure | `build-cure-loop` / `build-cure-prompt` | Bounded retries. The cure prompt repeats the exact feature-card shape, durable-prefix constraint, original commit and exact validation error. Tests explicitly assert the cure prompt repeats the required schema and that unrelated reviewer failures do not enter the cure loop. | A for supported cure kinds; typed terminal failure otherwise |
| 11 | Authored commit → independent reviewer | `reviewer-prompt` → review parser/gate | Exact verdict grammar is at the durable response prefix; reviewer must inspect the bound commit and execute tools. Approval without execution evidence fails. Author and reviewer must be distinct and invoke-ready. Tests cover misleading prose, missing tool evidence, request changes and rejection. | A |
| 12 | Negative review → revision author → re-reviewer | `revision-author-prompt`, `revision-reviewer-prompt` | The complete negative review, construction, acceptance criteria, prior commits, exact pattern-use claim and re-review claim are repeated. The amendment is rebound to an observed new commit. Tests cover malformed/absent claims, wrong action digest, unresolved commits and second rejection. | A/B: strong binding, but two independently copied EDN marker schemas |
| 13 | Missing standing decision → reviewer follow-up | `ensure-standing-decision!` / `standing-completion-prompt` | The consumer requires an exact eight-key standing-decision record, identity separation, evidence vector, timestamp and an 80-character explanation. The follow-up prompt says only “Deposit the standing decision … Decision content is yours”; it does not include or attach that schema. The detailed shape appeared in the earlier review prompt, but the follow-up is a new Agency job and cannot rely on conversational memory. It catches failure only after spending the job. | C |
| 14 | Job reply → reply extraction | `flight_runner/reply-text`, retained job-text ports | Uses `:result` once, falling back to text events, specifically avoiding the former double-copy fenced-block failure. Full author/reviewer text is retained beside attempt evidence. Tests cover result versus event text. | A |
| 15 | Runner decision → sealed Q/report facts → Lean/report card | Q2/Q4/Q6/Q8/Q9/Q10 carrier producers and `wm_run_facts` | Recent requirement carriers are sealed and recomputed from the record rather than accepted by shape. Missing or edited carriers become not-recomputable. The live click nevertheless ended with `requirements NOT CHECKED: HTTP Error 403`, so the publication/check service boundary can fail after a local report card is launched. The report says this, but publication status alone does not imply requirements were checked. | A for record-to-facts; C operationally for remote check availability |

## Findings, ordered by repair priority

### H1 — A failed selected-target exchange is not retained in the terminal record

This is the most immediate defect. `selected-target-reading-step` attaches its
record to the judgment, including typed validation refusal and stderr. In the
live run, recursive inspection of the terminal record found no selected-target
request/refusal map; only the Agency job survived under
`:registered-run/model-usage`. The final decision reverted to the original
`:no-current-target-reading` failure. An operator cannot distinguish “the
author was never contacted” from “the author replied and validation rejected
this exact field”.

Required repair: persist the phase record independently of whether redecision
succeeds, and render `contacted`, `answered`, `validated`, `published`, and the
typed refusal as distinct states. Add a production-runner test in which the
author returns a syntactically valid but validator-invalid response and assert
that the final record and report show the validator reason.

### H2 — Selected-only post-selection interpretation is not wired in production

`selected-reading-phase` correctly refuses when `:selected-reading-ask-fn` is
nil. Its request and receipt discipline are among the strongest in this audit,
but `full_loop_runtime/production-defaults` installs no such function. Thus a
large tested subsystem cannot run on the serving route. This is the same class
of defect as the earlier selected-target function being available dynamically
but absent from the actual runner option map.

Required repair: install a designated, readiness-checked responder port in the
production option map and add a test entering through
`full-loop-runtime/run-opportunity!`, not directly through
`selected-reading-phase`.

### H3 — The standing-decision follow-up withholds the schema it enforces

The validator expects a highly specific record. The follow-up prompt does not
state it. Reliance on an earlier prompt is not a contract: jobs can be retried,
restored, delivered to a fresh process, or inspected independently.

Required repair: make the standing-decision schema a data declaration used by
both prompt rendering and validation. Pin a test which gives the exact bad
shape a reasonable reader of the current follow-up would produce.

### H4 — The selected-target contract is repaired by duplication, not unified

The newly added role list prevents the observed failure, but the authoritative
set still lives in Python while Clojure contains a second literal copy. The
next validator change can recreate the same defect.

Required repair: have the request builder emit a versioned
`acceptance-contract` containing allowed roles and field constraints, or have
the validator expose that contract in a machine-readable command which Clojure
embeds verbatim. Test equality with the validator's authority, not merely that
the prompt contains today's words.

### H5 — Interpretation jobs depend on reading live validator source

Telling an agent to inspect Clojure source is useful context but not a frozen
contract. The source is mutable, may be hot-loaded, and is not among the
pinned `.source` companions. This permits the request and validator to refer
to different schema versions.

Required repair: issue the complete schema (or its content-addressed manifest)
as an immutable companion and have validation check that schema digest.

### H6 — Want-interpretation advertises syntax more completely than semantics

The response grammar is explicit, and semantic rejection is honestly typed.
But a responder is not told the complete set of source/admission conditions
which can reject a grammatically correct response. This wastes jobs and makes
rejection look like author error when the machine withheld relevant rules.

Required repair: attach a request-specific checklist of every enabled
validator, its version, and the data it will read. Do not attempt to encode
the constructor itself in prose; carry validator identities and pins.

### H7 — Remote requirement checking can disappear behind “publication launched”

The run produced local artifacts and launched report publication, while the
requirement check returned HTTP 403. The alert is honest, but callers and UI
must not collapse publication success and requirement-check success.

Required repair: represent report rendering, publication, and requirement
checking as three separately terminal receipts. A published report with an
unchecked requirements section must remain visibly incomplete.

## Cross-cutting cause

The recurring mistake is not weak validation. Validation is generally strict.
The mistake is that validation rules are often authored a second time as
English prompt text, or not sent at all. Tests then supply ideal hand-built
responses directly to the consumer. They prove the consumer rejects bad data
without proving that the real producer was given enough information to create
good data.

The live selected-target failure is a precise example: the fixture used
`["context"]`, already knowing the validator vocabulary. The real author had
never been told that vocabulary and chose reasonable but forbidden synonyms.

## Required design rule

Every external handoff must have one versioned contract value from which both
the producer-facing request and consumer validation are derived. At minimum it
must carry:

- schema id and version;
- exact allowed enums and required fields;
- request identity and immutable source pins;
- validator identities/versions and any enabled semantic gates;
- success, decline, refusal and retry/re-arm shapes;
- a place in the terminal run record for every outcome.

Prompt prose may explain this value but must not independently redefine it.
`done` means only that the producer stopped; accepted, validated, published,
and consumed remain separate recorded states.

## Repair packets

1. **Retain failures:** preserve selected-target phase records through the
   final record and report; pin the observed live invalid-role case.
2. **Wire tested consumers:** install the selected-reading production responder
   through the real composition root with designated identity and ledger.
3. **Unify contracts:** begin with selected-target roles and standing decision;
   render prompts from the same declarations validators consume.
4. **Freeze interpretation schemas:** attach content-addressed schema manifests
   to interpretation jobs and want requests.
5. **Separate report terminals:** distinguish rendered, published, and
   requirements-checked outcomes.
6. **Add producer-shaped tests:** for every row rated B or C, generate the
   response through the actual prompt/adapter and pass it through validation,
   publication and terminal-record persistence. Each packet needs a planted
   omission showing the test fails when one required constraint is withheld.

## Disposition

The author/reviewer implementation loop is substantially better defended than
the reading and interpretation paths: it binds real commits, independently
reviews them, and has many adversarial tests. The immediate unsafe area is the
front half of the click, where readings and interpretations are commissioned.
The correct next work is packets 1–3 above, in that order. More live clicks
before those are repaired would mostly pay to rediscover contract omissions.

## Repair update — validator-owned contracts

Commits `bf2cf7ee2` (futon2) and `fbef8ca40` (futon3c) close H1--H4:
selected-target outcomes survive terminal persistence; both reading responders
are production-wired to the designated author; selected-target and standing
decision prompts render the validator-owned contract instead of copying it.

The follow-up interpretation packet closes H5 and H6. Legacy interpretation
jobs now issue `interpretation-contract.source` before dispatch. It is captured
and hashed like every other request companion, and the returned receipt must
cite it. Want-interpretation requests now carry their request-specific semantic
gate list (source pin, token constraints, three-halves square, construction and
admission) inside the content-addressed issued request; the prompt renders that
same value.

H7 is not an agent handoff that can guarantee remote service availability.
Its acceptable guarantee is fail-closed separation: the local controlled report
card is valid only with a sealed checker terminal, and `wm_click.sh` treats a
missing run record, failed checker, or missing verdict as terminal failure. An
HTTP 403 therefore cannot be represented as a checked report. Publication and
checking remain separate facts; availability remains an operational dependency,
not evidence that the requirement check occurred.

### Legacy interpretation test disposition

`interpretation-job-test` formerly sent an interpreted-action fixture through
the whole runner. That selector route has been retired; the fixture now stops
before interpretation because it supplies neither a reading family nor its
Prefix-F evidence. Its 236 failures and 5 errors were therefore repetitions of
one unreachable setup, not 241 interpretation regressions.

The namespace now retains only contracts owned by `interpretation-job`: the
immutable acceptance manifest, mandatory transport classifier, closed failure
schema, and failure disposition. The still-live laws formerly mixed into the
fixture remain independently covered by `interpretation-pin-stress-test`
(captured-source identity) and `fact-measurement-test` (pre/end measurement and
frozen files). The selected-only production seam remains covered by
`wm.selected-reading-runner-test`.
