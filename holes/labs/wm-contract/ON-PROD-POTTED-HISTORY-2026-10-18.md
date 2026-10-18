# On-production potted history

Date: 2026-10-18

Status: design and stack-freeze gate; implementation not yet claimed.

## Purpose

This project advances a retained live War Machine history through the actual
production machinery without spending another click or repeating work whose
result is already durably recorded. It exists to expose several downstream
production defects per real click, instead of discovering only the first one.

This supersedes the old recorded-flight instrument as evidence of *live
readiness*. The old instrument remains useful as a deterministic unit of
downstream conformance, but its isolated roots, direct function calls and
fixture executors are not production evidence.

## Non-negotiable meaning of “on prod”

An on-production replay runs in the serving Futon3c JVM and uses:

- the canonical Futon2 checkout and production data roots;
- the same registered-run, `run-phase!`, held-condition and retry machinery as
  an ordinary click;
- the same production handoff decoder, validators, registries, persistence,
  cascade compiler, capability routing, journal, trace and close path;
- the same debugger and repair-dispatch boundary;
- the serving process's recorded code identity.

It must not bind a test data root, supply an alternate output root, call a
phase implementation directly around the runner, substitute the recording
pattern executor, manufacture effect receipts, or translate a production
error into a replay success.

The one permitted substitution is a transcript response at a named
nondeterministic port. The retained raw response bytes, job identity, author,
reviewer, schema and digest are supplied to the *production port*. All parsing,
normalization, validation, publication and downstream work still execute in
production. A missing transcript response stops at a typed frontier. It never
falls back silently to a fixture or a live agent call.

## It is not a click

The replay has a distinct `:wm/on-prod-potted-history-v1` run identity. It
consumes no ordinary-click authorization, performs no outer selection, and
cannot certify a carried tick. Any production writes are append-only and name
the replay identity and source run. A later real click may use repairs learned
from it, but the replay itself is never relabelled as that click.

Skipping outer selection is a recorded restoration, not a new decision. The
source selection receipt and selected canonical cascade must revalidate byte
for-byte under current code. Equal-G discrimination remains the recorded Lean
FAIL; replay does not cure or conceal it.

## Initial source and frontier

Use the furthest live October 18 source:

- click `wm-click-ea4e52f7-ee16-4409-b7f4-8f52faa35ebe`;
- run `2026-10-18-9c3a318d-a329-4dca-a19b-17976a0acfde`;
- terminal attempt `attempt-002`;
- last genuine phase: `cascade-execution`;
- retained failure: production executable-cascade persistence attempted a
  process-relative missing directory.

The first execution must restore and revalidate every durable predecessor
receipt, enter the ordinary held `cascade-execution` phase through its
production retry port, and exercise the repaired canonical store. It then runs
until the next production condition or ordinary close. It does not reconstruct
the cascade in a separate namespace and copy the result into the runner.

## Rollback pin, not a development gate

The Git pin is a rollback marker. It is not a prerequisite attestation for
each repair iteration: this instrument intentionally changes and hot-loads
production code while advancing. Each ledger generation records the code it
actually ran, and a repair opens a new linked generation.

The following remain required:

1. Futon2, Futon3c and every loaded code dependency have pinned commit and tree
   identities.
2. Their tracked worktrees are clean and stable before and after reload.
3. The serving JVM records each targeted reload and the replay ledger names
   that loaded identity; a mismatch is evidence, not a reason to postpone the
   first repair run.
4. The transcript manifest binds the source run record, every restored
   artifact, the production configuration and the stack pin by digest.
5. Pre-existing untracked artifacts are preserved and censused. They are not
   silently deleted, committed, or admitted as code/config authority.
6. Hot repair creates a new explicitly linked stack generation without
   pretending that it was the initial pin.

The Futon2 pin for this project is the annotated Git tag
`wm-on-prod-replay-base-20261018`, applied to the commit containing this design.
Companion repository pins observed when the design was created are:

- Futon3c `3fa9be7bd94ab3a412ab97659f343c2c441f5dd2`, tree
  `e419ddc4b3293ce91f30adc23f6da3a6da431f81`;
- Futon1 `f3cc717d3a2cd3d2909cf3a95e540225a5679ce6`, tree
  `47ec7520673d28ec66ebcd0367d2e733ccd92a4b`;
- Mathlib4 `2c21e66ec452d16e5dad11d8673024d195da06b0`, tree
  `a0e396bda61474a7eb96dd89a69c734948b6db65`.

These pins are a proposed stack until the serving-JVM identity check passes.
They are not yet a readiness certificate.

## Required ledger

Each replay generation records:

- stack tag, repository commits/trees, tracked-dirty state and serving identity;
- source click/run/attempt and every restored artifact path plus digest;
- every phase as `:restored`, `:revalidated`, `:executed`, or `:frontier`;
- production path and port used by each executed phase;
- start/end/duration, writes, handoff calls, debugger holds and repairs;
- click-budget and selection-counter snapshots proving both stayed unchanged;
- the exact next typed production condition.

## Falsifiers

The project fails, rather than degrades, if:

- a test binding or alternate root makes the replay pass;
- a direct helper call reaches farther than the serving runner;
- a transcript answer bypasses production decoding or validation;
- the selected receipt or cascade is recomputed instead of restored and
  revalidated;
- production writes cannot be attributed to the replay identity;
- loaded code differs from the pinned clean stack;
- selection or click budget changes;
- a failure is classified as success merely because replay continued.

## Implementation sequence

1. Produce the stack/transcript manifest and obtain the serving identity
   attestation.
2. Add a serving endpoint that opens an on-prod replay as a registered run and
   restores the named held phase without selection.
3. Route pinned transcript responses through the existing production ports.
4. Resume the source run's `cascade-execution` phase and ledger the next
   frontier.
5. Repair that frontier under the ordinary debugger protocol, create the next
   stack generation, and resume again until production close or an unresolved
   design decision.

No new ordinary click is warranted by this project until the on-production
history reaches ordinary close or names a genuinely nondeterministic condition
that retained evidence cannot exercise.
