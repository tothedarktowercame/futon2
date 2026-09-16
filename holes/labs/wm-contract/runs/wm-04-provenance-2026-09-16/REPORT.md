# WM-04 provenance and occurrence return

2026-09-16, codex-8, on Joe's explicit instruction to carry out the proposed
DEP1/DEP2 provenance work and narrow the observation-contract work with WM-04.
Target: `M-shared-memory-control-build-test`.
Revised after independent review `1b1e96b6`: DEP1 retrieval accepted with wording
corrections; the initial DEP2 account was rejected and is corrected below using
the reviewer's execution join. Revision acceptance pending. No observation,
label, model or production action.

## DEP1: original records acquired

The live evidence service returned these records by exact evidence identity.
Raw response bytes, SHA-256, retrieval time and URL are retained in the matching
`*-response.json` and `*-fetch.json` files here.

| Record | Identity | What it establishes |
|---|---|---|
| Recorded decision | `6e6f56a1-b9d7-4f83-928f-3a211ef890a0` | Exact mission subject; claude-4 records Joe's instruction to remove operator-level strategy/enactment gating, retaining machine gates and 1–2 runs before a larger campaign |
| Original retained user turn | `emacs-d200387dbebb4ff30653e9c308c4bf00` | `evidence/author` joe, role user, Emacs Claude REPL transport, session `8b297865-a47b-4b91-bb40-e5ae59ff4f70`; literal operator instruction agrees with removal of enactment gating, delivery QA and 1–2 first |
| User turn's reply parent | `emacs-b8fe23c2bc8f99bbeb6c1729e1339496` | Preceding assistant discussion names this selected mission and explicitly asks Joe to decide the enactment rung |

The user turn was located by a session-scoped evidence query, then fetched
separately by identity. The query returned 79 records under a requested limit
of 1000 in the July 24 20:00Z–July 25 00:00Z window; its raw response is retained.
The exact session filename was not found in `/home/joe/.claude/projects` by
`rg --files -g '*8b297865-a47b-4b91-bb40-e5ae59ff4f70*'`. This is only that local
filename search, not a claim that no original session log exists elsewhere.

**Provenance level:** these are source records retrieved from the stack's evidence
store, including an original retained operator chat turn and its reply context;
they are stronger evidence than a source-code citation to the decision UUID.
The retrieval API returns stored entries (`handle-evidence-entry`); it does not
authenticate authorship cryptographically. The transcript's author/role/transport
fields and chain are the available attribution, but they do not identify who
typed the input: the same session has 17 user-role turns attributed to joe,
including eight agent-written park-wake payloads. The quoted turn reads as
operator speech rather than a wake payload; that distinction relies on content.
This packet does not manufacture
a new authenticated commission or insert records into the observation resolver.
Independent review must decide whether this provenance meets the applicable
authority requirement; any stronger required origin proof remains explicit.

**Time discrepancy:** the decision body's `decided-at` is `2026-07-24T21:10Z`;
the parent assistant question is timestamped `2026-07-24T21:11:16.58Z`;
the retained user turn's `evidence/at` is `2026-07-24T21:16:27.178777083Z`;
the decision was recorded at `2026-07-24T21:17:39.210131443Z`. Preserve all four.
The 21:10 summary time precedes the parent question and cannot establish the
decision instant; treat it as an approximate summary time. These records
support the July 24 sequence before the September target selection, but
do not establish a single exact effective instant. No earlier cutoff may be
justified by silently choosing the summary's time. The decision's `quoted`
field is an edited paraphrase; cite the literal user turn, including its
explicit 1–2-before-10–20 instruction.

This instruction is not a categorical observation commission, a retrospective
sampling rule, a label, execution evidence or current permission to bypass any
later machine/operator constraint. Its scope must remain attached to its content.

## DEP2: the selection was displaced, and F11 was enacted

`readback.clj` reads every EDN form in the pinned September 12 trace, verifies
four records and the fourth record's timestamp and target, and writes
`trace-readback.edn`. The selected action and policy match WM-05's D1 account.
Both exact-target pre/post belief lookups return nil. Each belief map has 417
keys: 414 stack-coordinate strings and three keywords, rather than mission-level
coordinates. This is a domain mismatch, not evidence of a dropped target row.
The record's producer
contract is `:r8/retired-f-controller-v1`; the observation envelope has `:type`
and `:channels`, not an externally supplied close-observation authority context.

The required run/cohort/attempt/checkpoint identities and four observation times
are absent at the record root and its `:point` location. These inspections do not
assert that every nested historical record lacks similarly named fields; such
fields would need their own exact-occurrence join.

`correlation-search.json` retains the exact commands and results for all existing
`futon2/data/wm-full-loop*` roots, searched with ignored files included:

- Exact selection timestamp `2026-09-12T17:28:09.498448087Z`: no literal matches
  (rg exit 1, no errors).
- Policy `pi-s-9dbc2ceb3317bc38050c41ce`: 65 matching files. `policy-occurrences.edn`
  reads all forms in those files and lists their event identities and recorded
  times. Matches span multiple cohorts and July–September records, including
  later machinery records. Policy equality therefore cannot identify this one
  occurrence; a match may be a nested historical reference, not enactment.

`source-pins.json` pins the trace, all 65 matching files, validator/resolver,
transport source and upstream specification/handoff. This is a bounded local
correlation search, not a global absence census or proof the selected action
never ran. Different timestamp encodings or an external ledger would require
additional linkage evidence. This initial search was insufficient for the
provenance task: independent review found the join through the phases log and
morning-brief item, which is outside those globbed roots.

**Verified join:** opportunity
`duree-click-on-demand/2026-09-12T17:23:07.717766103Z/ed3826f3-e2bc-4bf4-8285-d98ea30d71b8`,
execution `ea1-418bb56e1f0d7ad8986839e45f5268a98e62dcbf9c8ad3634bd998f421a293ae--attempt-001`.
The matching file under `data/wm-morning-brief/items/` retains the ordinary
selector decision inside `:selection-review :selection-reasons`. Its decision
agrees with the trace after removing the differently keyed weight maps; those maps have
different keys but equal 148-value multisets. The same record says
`:source :authenticated-operator-task-pin` and names
`M-f11-find-production-successor` as the enacted candidate and selected target.
This is the record's authority classification, not new authentication by us.

The phases log lines 6833–6869 identify this opportunity/execution. Selection is
17:26:16–17:27:26, construction 17:27:30–17:27:34, author-dispatch starts at
17:28:10.105, and delivery QA occurs at 17:34:21. These times belong to the F11
click. They cannot be borrowed as action/close times for the displaced target.
The F11 author/outcome record is not an observation of shared-memory mission
progress. The revised `readback.clj` asserts and retains this distinction in
`execution-join.edn`; the reviewer independently discovered and checked it.

The current trace is the post-migration file (mtime 19:55), with an existing
pre-migration backup (mtime 17:28). The fourth decision is identical in both;
the later file adds accumulation state/initialization fields. Additional source
pins are retained in `revision-source-pins.json` and the review's
`review-claude-3/join-source-pins.txt`.

| Required field | Result for the September 12 design anchor |
|---|---|
| Target, selected action/policy, selection timestamp | Retained and independently readable |
| Exact target pre/post belief | No mission-level coordinates in these belief maps; not a dropped target row |
| Execution binding | Identified F11 click and brief item; shared-memory recommendation displaced |
| Shared-memory action start/completion, cutoff, disposition | Not supplied by this click because that target was not enacted; F11 times cannot substitute |
| Externally authorized expected context and observation configuration | Not supplied by the acquired decision or selection record |
| Predeclared eligibility/sampling and blinded observer commission | Not supplied; neither is retroactively constructed here |

## Narrowed observation-contract work

The returned records remove the need to repeat the original-authorization lookup.
They do **not** make this historical selection ready for a qualifying observation.
It is a displaced selector recommendation, not a shared-memory execution awaiting
its missing join. Keep it as that scoped design anchor.

The next bounded contract review should use WM-05's existing
`U1-OBSERVATION-SPEC.md` and ask WM-04 to resolve these specific matters:

1. Assess the acquired user-turn/decision provenance and timestamp discrepancy;
   name any missing authority evidence rather than treating a reference as proof.
2. Decide whether a displaced selector recommendation belongs in the declared
   observation population at all. Under an enacted-target population, exclude
   this case as not enacted rather than record it as an unobserved execution.
   Specify the exact context a prospectively commissioned enacted occurrence
   must retain. Do not mint that occurrence or borrow the F11 action's label.
3. Review the target's lifecycle rubric applicability at that point, including
   ambiguity and the distinction between whole-mission and subphase evidence.
4. With the existing WM-12 and outcome-domain owners, settle the state/observation
   relationship and timing required for sampling. The current belief's stack
   coordinates do not supply the proposed mission-level state distribution.
   A close-point validator alone
   cannot license arbitrary intermediate observations; lifecycle labels alone
   do not estimate state-conditioned terminal-disposition probabilities.
5. Only after those choices are settled, narrow the prospective eligibility,
   stopping, blinding and distinct exact-subject review contract. Existing
   immutable evidence admission and resolver checks remain unchanged.

Items 3–5 are proposed remaining contract work, not claimed completed semantics.
No preference construction or WM-06 changes are commissioned by this packet.
No new observation, accepted label, eligible pair, denominator, measured A,
model admission, serving use or checklist/DAG state change is claimed.

### WM-04 owner's returned assessment

`OWNER-ASSESSMENT.md` retains the completed owner response to
`invoke-1789574000734-21494-4b95d23b`. The owner accepts the corrected displaced-
recommendation account and treats the original-record provenance as adequate
at source-record/design-context scope, not admitted-evidence-window scope.
This resolves the missing-close question for this recommendation: it was not
the enacted target, and there is no mission-level q0 in the trace to recover.

The owner agrees that an enacted-target population would exclude this case as
not enacted, without assigning an F11 label. This remains a design conclusion;
the population/eligibility rule still requires prospective reviewed authority.
Any future evidence use must admit literal bytes before its cutoff through the
existing manifest/resolver. The owner also preserves WM-12 schedule and outcome-
domain dependencies and identifies prospective sample adequacy/stopping authority
as outstanding. This assessment narrows the next contract packet without
claiming those missing inputs have been delivered.

## Validation and coordination

The readback executes against actual retained EDN with assertions on the exact
record count, timestamp and target. Its output is retained. clj-kondo reports
0 errors/0 warnings; check-parens reports OK. No runtime source was changed.

Codex-28 reported an overlapping request to WM-04 (DEP1/DEP2/DEP4,
`invoke-1789573365035-21479-c734e9be`, p4ng `0876046`) and instructed that this
acquisition be its shared input, with the owner assessing applicability instead
of repeating the lookup. His WM-12/WM-08 requests are acknowledgment-only.
The earlier WM-04 discussion job `invoke-1789573233140-21472-0950686b` completed
after the initial packet. Its full reply is retained in `OWNER-PLANNING.md` and
`owner-planning-response.json`. The owner recommends exactly the bounded DEP1
acquisition plus DEP2 missing-field inventory and separate review pursued here.
That is planning agreement, not acceptance of this packet's findings; the latter
request is queued as `invoke-1789574000734-21494-4b95d23b`.

The owner's reply also says zero authorized observations/eligible pairs/measured
A rows exist. This packet does not adopt that global claim: its cited 0/7 and
0/86 figures are scoped historical findings, not a contemporary exhaustive census.
