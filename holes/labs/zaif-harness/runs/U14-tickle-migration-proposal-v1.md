# U14e — tickle-side checked-handoff migration proposal v1

Date: 2026-09-03
Author: codex-22
Status: proposal only; no source change is authorized here

This is the deferred packet from `U14-r9-ingress-design-v1.md:151-156` and
`:179-181`. It relies on the checked-handoff event and validator introduced by
futon3c `bf634fab` (reviewed in `ef3ff249`), persisted APM seats from
`b3d770a4`, additive guide typing from `f7c373f4` (reviewed in `8a090c66`), and
the grading shim from `b4604c0c`.

## Enumeration and homonym exclusion

These searches were run from `/home/joe/code/futon3c`. Each complete result was
written to `/tmp/u14e` before inspection; none was truncated.

```sh
rg -n --glob '*.clj' 'tickle' src/futon3c/apm > /tmp/u14e/tickle-apm.txt
rg -n --glob '*.clj' 'independent-review|independence|verdict|adjudicat|reviewer|depositor' src/futon3c/apm > /tmp/u14e/r9-apm.txt
rg -n --glob '*.clj' 'tickle' src | rg -v '/futon3c/apm/' > /tmp/u14e/tickle-non-apm.txt
rg -n --glob '*.clj' ':verdict|parse-verdict' src/futon3c/agents > /tmp/u14e/verdict-agents.txt
```

The outputs had 0, 362, 169, and 12 lines. The first two commands establish
the boundary: the legacy `:receipt/independent-review?` vocabulary is confined
to `apm/*`; no tickle orchestration site reads or writes it. The last command
then restricts the proposed migration to verdict-bearing code under
`futon3c/agents`, excluding WM outing verdicts and APM promotion verdicts that
share the words but are different state machines. Blackboard projection at
`tickle_orchestrate.clj:161-162` merely renders an already-produced verdict; it
does not mint or persist one. Four mint/copy boundaries remain.

## Site 1 — review completion is the authority boundary

Current site: `src/futon3c/agents/tickle_orchestrate.clj:309-317` parses review
prose into `:approve`, `:request-changes`, or `:unclear`;
`request-review!` invokes the fixed author seat `claude-1` at `:371-397`, emits
`:review-complete` with `:ok`, `:verdict`, preview, error, and elapsed time at
`:398-410`, and returns the same verdict at `:411-415`. The worker result
currently returns no worker-seat field (`:365-369`).

IF this is where a separate reviewer first authors a verdict, HOWEVER the
worker seat is discarded by `assign-issue!`, THEN the first migration packet
must retain `:agent-id` in the assignment result and have `request-review!`
construct and validate `:checked-handoff/verdict` with worker seat, author seat
`claude-1`, the issue/result proposal, parsed verdict, and an explicitly absent
rerun witness, BECAUSE seat distinctness must be computed from persisted inputs
rather than inferred from the function name. During a dated additive window,
the old `:verdict` remains and the checked event plus computed grade is added to
the emitted body and return map. A validation refusal prevents the review from
being represented as accepted.

SEAM FLAG: **NO**. This writes tickle coordination evidence, not an APM frame
receipt, and changes neither frame-cycle receipt shapes nor promotion gate
vocabulary.

## Site 2 — workflow completion copies the verdict

Current site: `src/futon3c/agents/tickle_orchestrate.clj:491-500` copies the
review result into a summary containing issue number, workflow status, verdict,
and elapsed time; `:503-509` persists that summary as `:workflow-complete` and
`:510-511` projects it for display.

IF workflow completion republishes the authority decision, HOWEVER a bare
keyword loses its seats and adjudication, THEN a second packet must copy the
already-validated checked event and grade from site 1 without recomputing or
re-authoring them, BECAUSE a projection is not a new adjudication. The old
`:verdict` remains through the same dated window; status, timing, IRC reporting,
and blackboard display stay unchanged.

SEAM FLAG: **NO**. The record remains under tickle orchestration tags and no
APM/WM consumer reads this shape.

## Site 3 — CT extraction evidence conditionally copies a verdict

Current site: `src/futon3c/agents/tickle_work_queue.clj:257-283` accepts entity
id/type, session, event tag, ground truth, extraction result, and `verdict`; it
persists task/type/claim metadata, forces author `tickle-1` at `:273`, and
conditionally adds only `:verdict` at `:283`.

IF this generic emitter receives a verdict produced elsewhere, HOWEVER forcing
the envelope author to `tickle-1` cannot establish who adjudicated whose work,
THEN its packet must replace the verdict argument at the authority seam with an
already-validated checked event (or reject an unvalidated event), persist that
event and its grade, and retain the old verdict additively until the dated flip,
BECAUSE the evidence emitter must not manufacture authorship. Entity metadata,
preview truncation, tags, and claim-type selection remain unchanged.

SEAM FLAG: **NO**. CT extraction evidence is not consumed as an APM frame or
promotion receipt.

## Site 4 — ArSE generation evidence conditionally copies a verdict

Current site: `src/futon3c/agents/arse_work_queue.clj:154-179` accepts entity,
problem, node, session, event tag, generation result, and `verdict`; it persists
the corresponding coordination envelope, forces author `tickle-1` at `:169`,
and conditionally adds only `:verdict` at `:179`.

IF ArSE repeats the same bare-verdict copy as CT extraction, HOWEVER its
producer and reviewer are likewise absent, THEN a separate one-file packet must
make the same validated-event substitution and additive persistence, BECAUSE
sharing a proposed schema does not justify changing two workflow files in one
behavioral packet. Problem/node metadata, previews, tags, and claim types stay
unchanged.

SEAM FLAG: **NO**. ArSE coordination evidence is outside the APM frame receipt
and promotion-gate path.

## Proposed sequence and acceptance bars

1. **U14e-1 — `tickle_orchestrate.clj`, review authority.** Retain worker seat,
   validate the event, persist seats/adjudication/grade at `:review-complete`.
   Acceptance: worker=author is refused; fixed `claude-1` distinct from the
   actual assigned worker grades `:seat-string-distinctness`; dangling witness
   never earns the top grade; old verdict remains byte-for-byte equal.
2. **U14e-2 — `tickle_orchestrate.clj`, workflow projection.** Copy the exact
   validated event from review result to workflow completion. Acceptance: event
   identity and grade are unchanged across the copy; failed review cannot mint
   an accepted checked event; display/status behavior is unchanged.
3. **U14e-3 — `tickle_work_queue.clj`.** Require or accept the validated checked
   event at CT verdict-bearing calls and persist it additively. Acceptance: a
   forged worker-authored event is refused before append; envelope author
   `tickle-1` cannot alter the event author; legacy verdict remains during the
   window.
4. **U14e-4 — `arse_work_queue.clj`.** Apply the same rule independently.
   Acceptance: the CT properties hold for ArSE fixtures and its problem/node
   fields are unchanged.
5. **U14e-flip — dated retirement, only after all four land.** Readers switch
   from bare verdicts to checked events, then legacy fields can be retired in a
   separately reviewed change. Acceptance: a census finds no authority decision
   based solely on bare `:verdict`; historical evidence remains readable and is
   never upgraded beyond `:ungradeable-legacy` without persisted witnesses.

All four proposed implementation packets require claude-1 review because they
are claude-1-side orchestration tooling, despite carrying zero APM/WM seam
flags under the narrower receipt/gate definition above.

## Amendment 2026-09-03 (post-U14e-1 stop; claude-1 ruling on record)

There is NO legacy record population at any of the four sites: the evidence
store returns `{:count 0 :checked 0}` for tickle-scoped AND single-tag
queries across all three families (verified independently by codex-22,
claude-2, claude-1; mechanism: the work queues take `evidence-store` as a
caller-supplied parameter with no HTTP wiring anywhere, and `emit!` no-ops
silently on nil — the fourth silent-degrade instance this campaign). The
dated-additive window at these sites therefore protects code-path
compatibility only, and the first-ever persisted records from these flows
will be born speaking the checked-handoff vocabulary. Stated here so nobody
later wonders where the legacy population went: there never was one, and the
absence queries prove it. Condition 2 (live pin) is amended per site to the
PAIR: (a) the dated absence queries verbatim (both tag forms), (b) an
in-test byte-capture of the unmodified emission on a fixture. Wiring the
store at the callers is wm-side row U38, independent of this migration.
