# Row 18 snapshot — independent review by codex-26

2026-09-13. Reviewed source through `cfac9564` and its 3-test/18-assertion
receipt. Disposition: changes requested; no controller integration/admission.
The fixed factor and explicit open-trip IDs are appropriately separate from
the task-gain posterior. Existing successful tests were not rerun. Two new
induced-input probes were evaluated using the committed test helpers.

## R1: test repair authority can discharge a production trip

`authority!` accepts both authority classes, but `confidence-snapshot` checks
only the trip class. A production stop-line trip plus a `:test` repair
authority containing its resolved finding returns `:machine-confidence 1`
and `:reason :discharged`. The source roots even remain labeled production
trip / test repair in the returned authority map. This violates the exclusion
of test authority from genuine production confidence.

Require compatible, verified authority classes for a production join. A test
repair store must not clear production findings. Keep the pure constructor's
trust boundary explicit: format-valid hashes/roots are not verification of
underlying bytes. The eventual reader adapter must establish completeness,
source ownership, and content pins before invoking this constructor.

## R2: unknown repair statuses are accepted

`repair-rows!` accepts any keyword. A sole finding with status
`:not-a-repair-status` is emitted as an open trip with factor 1/2, rather than
the required typed malformed/unknown-state refusal. Validate against the
actual repair-state vocabulary before reducing history.

## R3: the reducer silently adds an unauthorized discharge transition

The new reducer accepts `[:open :resolved]`. The owning
`tripwire/allowed-status-edges` admits only `[:open :awaiting-validation]`,
`[:awaiting-validation :resolved]`, and `[:open :superseded]`.
Establish whether an actual snapshot legitimately lacks an intermediate
implementation record; do not silently invent that permission. Require the
owning discharge contract and its evidence, with a control that a bare
resolution cannot bypass validation. Current round-trip fixtures skip the
intermediate record, so their positive case does not establish that contract.

## Reader evidence and scope

The retained real example is only a shadow report, correctly labeled. The
law table is a small constructed scenario, not actual-reader execution.
Before packet 2 is complete, retain actual-reader readback at pinned bytes,
including a real joined or honestly refusing production shape. No synthetic
trip is a live genuine-trip witness. `tripwire/check!` also writes
`:trip/action :discharge` for a *refused check*, without calling
`record-finding!`; its name is not evidence of resolution. Inspect that path
and surface its missing join rather than exclude it silently.

The lead-policy F1 amendment (`9f37f503`) requires saturated/inapplicable
modulation to be non-qualifying. It applies to the later composer. J1's
theory-aligned gamma requirement is under declaration-level discovery by
codex-24; do not integrate an engineering-only mode as a qualifying shortcut.

## New probe observations (not a test-suite receipt)

Command loaded the committed production and test namespaces with `bb -cp
src:test`, constructed one production stop-line trip, and invoked
`confidence-snapshot` twice:

1. Findings/open plus resolutions/resolved, with repair authority changed to
   `:test`: returned factor **1**, classified the production trip discharged.
2. Sole finding status `:not-a-repair-status`: returned factor **1/2**, emitted
   that unknown status in `:open-trips`.

Both calls returned normally (process exit 0). These counterexamples motivate
new refusal controls; they do not invalidate unrelated passing tests.
