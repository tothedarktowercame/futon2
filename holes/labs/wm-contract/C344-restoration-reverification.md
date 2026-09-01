# C344 — restoration re-verification after C333

Date: 2026-09-01. Discovery only. No coordinator or watchdog was stopped,
resumed, re-armed, or reconfigured. C319 was not dry-run.

## Verdict

The documented verb mapping is now semantically correct, but the restoration
path is **not yet executable evidence**. It relies on an operator to create and
interpret records that no command currently writes or validates.

## Coordinator classes

- `jit-queue:jit-m94A03-retry-v3`: the journal vocabulary has the distinct
  action `rearm-terminal-coordinator`. C319's emergency section requires both
  captured and current durable `:complete`, then uses `start-registered!` and
  explicitly forbids `resume!`. `live-regulator/start!` returns a recovered
  terminal state without scheduling a regulator; `start-registered!` can re-arm
  the independent watchdog.
- `jit-queue:jit-all-open-v2` and `ftriangle-live-smoke-v1`: the journal
  vocabulary is `resume-coordinator`. C319 requires captured pre-state
  `:running`; after durable `stop!`, `resume!` consumes the quiescence witness
  through `resume-stopped!` and starts the registered runtime.

These action names plus coordinator ID are sufficient to distinguish the
classes **if they are recorded faithfully**.

## Refusal status

C329 said restoration “refuses coordinator resume unless its captured durable
pre-state was genuinely `:running`.” Today that refusal exists only as prose.
Step 0 prints status to the terminal rather than saving a named manifest. Step
1 tells the operator to append after success, but supplies no literal append
command. Step 9 reads a free-form text file and asks the operator to substitute
an ID into one of three placeholder commands. No parser joins action, ID,
captured pre-state, current state, and allowed verb.

There is one useful lower-level safety net: the displayed two-argument
`resume!` supplies no continuation reason, so on a durable `:complete` state
`continue-complete!` returns
`:live-regulator-continuation-evidence-invalid` before enabling or starting it.
That is not C329's promised pre-state refusal, however; a caller supplying a
reason can explicitly continue completed work.

Likewise, the terminal `start-registered!` path is safe only while the durable
state remains `:complete`. C319 says to reconfirm that state, but does not
machine-enforce the reconfirmation.

## Partial abort and emergency operator path

The intended partial-abort algorithm is sound: append only after a successful
park and restore journalled actions in reverse order. It handles mixed verbs in
principle. It is not currently demonstrable because the displayed parking
commands never append their successful action and no restoration dispatcher
exists. A half-parked window can therefore leave an empty/incomplete journal or
accept a hand-edited wrong action.

The standalone emergency section contains no instruction to `resume!` the
completed coordinator; it correctly names `start-registered!`. Its dangerous
remaining property is manual interpretation: Joe must recover scrollback for
the pre-state, inspect a free-form journal, and select placeholders correctly.
If the coordinating session disappears, the named manifest authority may not
exist as a file at all.

## Required routed repair

Before asking Joe to park anything, supply one fail-closed restoration tool (or
equivalent literal command sequence) that:

1. atomically saves the structured pre-state manifest;
2. appends an allowlisted typed action only after each successful mutation;
3. on restore, validates manifest identity, action class, coordinator ID,
   captured pre-state, and current state;
4. permits `start-registered!` only for captured/current terminal `:complete`;
5. permits `resume!` only for captured `:running` now durably witnessed
   `:stopped`;
6. restores any journal prefix in reverse order and records each outcome.

Controls must reject swapped verbs for both classes and demonstrate restoration
planning for an empty, watchdog-only, running-only, and mixed partial journal.
