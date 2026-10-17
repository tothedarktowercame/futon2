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

