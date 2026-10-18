# Recorded flight: deterministic War Machine continuation

## Purpose

A recorded flight resumes an ordinary War Machine run from immutable evidence
without spending a click and without repeating outer selection.  It is a
development and readiness instrument: it may expose and repair the next
downstream defect, but it cannot create tick evidence or retroactively change
the outcome of the source click.

## Non-negotiable boundary

The replay restores data, not conclusions.  Every restored value is named by
source path and digest.  Every newly executed phase is marked `:executed`.
Nothing may silently fall back to a live selector, live agent, click budget,
or mutable “current” registry entry.

The replay therefore has three classes of input:

1. `:restored` — a sealed checkpoint, selected outer receipt, author answer,
   published reading/square, or repair artifact from the source run;
2. `:revalidated` — restored bytes accepted by the current validator, with
   both the original and current validator identities retained;
3. `:missing` — required evidence that the source run never produced.  This is
   a replay frontier, not permission to manufacture a fixture.

## Entry and fast-forward

The first implementation should accept a source run record plus its machinery
checkpoint directory.  It verifies that their run, click, attempt, target,
source, and selection identities agree, then constructs a replay manifest.

Outer selection is never invoked.  The retained `:outer-task-selection-v1`
receipt and chosen target are restored.  Selection counters remain zero and a
call to a selector is a typed `:recorded-flight-selector-called` error.

Earlier phases are fast-forwarded only while their complete inputs and outputs
are present and digest-valid.  The first phase whose output is absent is run
against stubbed ports.  A port may return only a response named in the replay
manifest.  An unrecorded agent call is a typed
`:recorded-flight-unrecorded-agent-call` error.

The run writes an append-only replay ledger containing, for each phase:

- source artifact and digest;
- disposition (`:restored`, `:revalidated`, `:executed`, or `:missing`);
- current code identity;
- start/end times for executed work;
- typed result or exception;
- the next replay frontier.

The replay uses its own output root and run id.  It must not write ordinary
click records, consume ordinary-click authorization, update habit stores,
publish report cards as live runs, or mutate the source artifacts.

## Click 5 seed and present frontier

The initial seed is click
`wm-click-0b708330-60fa-4b40-b00d-014340ea471d`, run
`2026-10-17-173c5a65-fed9-4bc1-8aa2-b3b4df08dba5`, attempt `attempt-002`.
Its selection checkpoint and target are retained.  Its saved Zai-2 answer is:

`/home/joe/code/storage/wm-target-reading-requests/b13efa403ece5a5deb42cba77f48ed305fe18f6c356eb7e169a70b07886c4e94/analysis-input-attempt-1.json`

That answer has nine fragments, zero fragments with `pattern_refs`, and zero
total references.  There is no `analysis-input-attempt-2.json` and no current
published reading for `M-aif-a-matrix-faithfulness` at the selected excerpt
digest.  Consequently the first faithful replay can prove that repaired code
issues the bounded correction request, but it cannot pass publication using
recorded evidence alone.

The frontier is therefore:

`selected target restored -> saved answer decoded -> Python validation passes
-> cascade validation requests correction -> corrected answer missing`

A corrected answer may later enter the manifest only as a separately authored
and validated offline artifact, with its authoring job and digest recorded.  It
must not be described as part of click 5.  Once present, the same recorded
flight resumes from this frontier, publishes into its isolated replay store,
and continues through cascade construction until the next typed error.

## Acceptance controls

- selector stub is planted to throw and its call count remains zero;
- click-budget state is snapshotted before and after and remains byte-equal;
- all agent ports throw unless their response digest is in the manifest;
- a changed checkpoint, answer, target, excerpt, or repair digest refuses;
- source-run stores are read-only and unchanged after replay;
- rerunning the same manifest produces the same restored/revalidated phase
  sequence up to the first newly executed nondeterministic boundary;
- the replay outcome is never reported as a click outcome or tick.

## Initial implementation and observed replay (2026-10-18)

`futon2.aif.wm.recorded-flight` implements the first boundary above.  A sealed
manifest pins the source selection checkpoint and agent answer by SHA-256.  The
runner restores the durable judgment/sorry cell, uses isolated request and
reading roots, supplies only the pinned first answer, and records the attempted
correction as the frontier.  There is no selector port in the API.  An optional
click-budget atom is compared before and after execution.

The CLI is `scripts/wm_recorded_flight.clj`:

`clojure -M -m wm-recorded-flight MANIFEST.edn OUTPUT_ROOT`

The actual click-5 replay produced the append-only ledger:

`data/wm-recorded-flights/click5/ledger/518d952f7de7c801be25d909b691599634249b282d9bbc7b07f694fdc47338b6.edn`

It records zero selector calls, zero clicks spent, zero ticks carried, two
agent-boundary calls (the restored initial answer and the missing correction),
and frontier `:corrected-answer-missing`.  The boundary result reproduces all
nine `:fragment-without-validated-pattern-ref` failures.  Thus the first replay
did not merely predict the frontier; it reached it through current production
validation using the retained live artifacts.

### Continued repair flight

An offline correction adds one grounded library citation to each of the nine
fragments.  This is explicitly attributed to `codex-68-offline-repair`, not to
click 5.  Current validation accepts it and publishes one cascade into the
isolated replay store.

Occurrence-square authoring then classifies eight citations as complete
occurrences and one as a typed void: the fragment-7 citation's library pattern
lacks the complete IF/HOWEVER/BECAUSE/THEN carrier, so supplying a ninth square
was correctly rejected.  The corrected eight-entry square artifact publishes,
and inner construction selects policy
`M-aif-a-matrix-faithfulness/policy-4b34f579f00abd4fa758` using the restored
outer receipt exactly once.  It spends zero clicks and invokes no live outer
selector.

The canonical carrier is retained at the documented Packet-B seam,
`[:decision :selection-certificate :selected-canonical-cascade]`.  The first
version of the replay incorrectly searched the entire judgment tree for a
nested `:canonical-cascade`; it now reads only that authoritative field and
refuses a digest mismatch.  Replay ledger
`data/wm-recorded-flights/click5-through-cascade-5/ledger/bf7364e3a88c0c47ec38137880eb7ed45148c1ad562843aac2a0e75fdaa21507.edn`
therefore reaches eight explicit operational holes with zero selector calls
and zero clicks.

The pinned, separately attributed offline authoring artifact
`data/wm-recorded-flights/click5-through-cascade/offline-operational-authoring.edn`
closes those eight holes as `:apply-pattern` operations.  It deliberately
supplies no application-order edges: reading order is not execution order.
The compiler derives each completion check from its validated four-map square,
certifies the refined cascade, and exposes all eight nodes as an enactable
frontier.  Replay ledger
`data/wm-recorded-flights/click5-through-enactment/ledger/936b6559aee824f89c8f63305ea0f379bc5dca56a048c5e80ec1ba67e8c1befd.edn`
records zero open holes and frontier `:flight-boundary-not-wired`.

That frontier is not another format or handoff failure.  The current runner
explicitly stops when a certified cascade has enactable nodes because no
executor yet turns `:apply-pattern` into a verified substrate effect.  A
further replay must not manufacture such receipts.  The next implementation
unit is an operation dispatch table for declared cascade capabilities, with a
real `:wm/pattern-apply` executor that emits verified-effect journal entries;
only then may the recorded flight advance to replay/tick evaluation.
