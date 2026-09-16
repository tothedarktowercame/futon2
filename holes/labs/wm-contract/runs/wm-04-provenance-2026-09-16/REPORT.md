# WM-04 provenance and occurrence return

2026-09-16, codex-8, on Joe's explicit instruction to carry out the proposed
DEP1/DEP2 provenance work and narrow the observation-contract work with WM-04.
Target: `M-shared-memory-control-build-test`.
Independent review pending. No observation, label, model or production action.

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
fields and chain are the available attribution. This packet does not manufacture
a new authenticated commission or insert records into the observation resolver.
Independent review must decide whether this provenance meets the applicable
authority requirement; any stronger required origin proof remains explicit.

**Time discrepancy:** the decision body's `decided-at` is `2026-07-24T21:10Z`;
the retained user turn's `evidence/at` is `2026-07-24T21:16:27.178777083Z`;
the decision was recorded at `2026-07-24T21:17:39.210131443Z`. Preserve all three.
They support the July 24 sequence before the September target selection, but
do not establish a single exact effective instant. No earlier cutoff may be
justified by silently choosing the summary's time.

This instruction is not a categorical observation commission, a retrospective
sampling rule, a label, execution evidence or current permission to bypass any
later machine/operator constraint. Its scope must remain attached to its content.

## DEP2: available occurrence context

`readback.clj` reads every EDN form in the pinned September 12 trace, verifies
four records and the fourth record's timestamp and target, and writes
`trace-readback.edn`. The selected action and policy match WM-05's D1 account.
Both exact-target pre/post belief lookups return nil. The record's producer
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
additional linkage evidence. No candidate match is promoted to a close.

| Required field | Result for the September 12 design anchor |
|---|---|
| Target, selected action/policy, selection timestamp | Retained and independently readable |
| Exact target pre/post belief | Missing at the trace's target lookup |
| Run/cohort/attempt/checkpoint binding | Not established by this trace or literal correlation search |
| Action start/completion, evidence cutoff, disposition time | Not established; selection time is not a substitute |
| Externally authorized expected context and observation configuration | Not supplied by the acquired decision or selection record |
| Predeclared eligibility/sampling and blinded observer commission | Not supplied; neither is retroactively constructed here |

## Narrowed observation-contract work

The returned records remove the need to repeat the original-authorization lookup.
They do **not** make this historical selection ready for a qualifying observation.
Keep it as a design anchor unless a producer supplies its exact execution join.

The next bounded contract review should use WM-05's existing
`U1-OBSERVATION-SPEC.md` and ask WM-04 to resolve these specific matters:

1. Assess the acquired user-turn/decision provenance and timestamp discrepancy;
   name any missing authority evidence rather than treating a reference as proof.
2. Determine the exact producer of the execution/close context. If the historical
   join remains unavailable, specify the fields a prospectively commissioned
   occurrence must retain. Do not mint a substitute occurrence or sample now.
3. Review the target's lifecycle rubric applicability at that point, including
   ambiguity and the distinction between whole-mission and subphase evidence.
4. With the existing WM-12 and outcome-domain owners, settle the state/observation
   relationship and timing required for sampling. A close-point validator alone
   cannot license arbitrary intermediate observations; lifecycle labels alone
   do not estimate state-conditioned terminal-disposition probabilities.
5. Only after those choices are settled, narrow the prospective eligibility,
   stopping, blinding and distinct exact-subject review contract. Existing
   immutable evidence admission and resolver checks remain unchanged.

Items 3–5 are proposed remaining contract work, not claimed completed semantics.
No preference construction or WM-06 changes are commissioned by this packet.
No new observation, accepted label, eligible pair, denominator, measured A,
model admission, serving use or checklist/DAG state change is claimed.

## Validation and coordination

The readback executes against actual retained EDN with assertions on the exact
record count, timestamp and target. Its output is retained. clj-kondo reports
0 errors/0 warnings; check-parens reports OK. No runtime source was changed.

Codex-28 reported an overlapping request to WM-04 (DEP1/DEP2/DEP4,
`invoke-1789573365035-21479-c734e9be`, p4ng `0876046`) and instructed that this
acquisition be its shared input, with the owner assessing applicability instead
of repeating the lookup. His WM-12/WM-08 requests are acknowledgment-only.
The earlier WM-04 discussion job `invoke-1789573233140-21472-0950686b` is still
running as this report is prepared; no owner acceptance is inferred.
