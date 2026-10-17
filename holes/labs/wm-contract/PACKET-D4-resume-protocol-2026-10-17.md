# Packet D4 receipt — debugger and same-click resume protocol

**Date:** 2026-10-17
**Excursion:** [[E-implementing-G-over-cascades]] Packet D4
**Branch:** `packet-d4-resume-protocol` (worktree `/home/joe/code/futon2-packet-d4`, base exactly main `9a76c4302`)
**Status:** reusable protocol delivered against a documented fixture/port.  No production wiring guessed; no live click run; not merged.  Scope: one new namespace + tests + this receipt at the debugger/repair-loop/runner-service boundary.  Selection, canonical cascade/compiler/journal, full-loop runner/runtime, exporter, and Lean untouched.

## The port, and the ONE adapter field D3 must supply

D3's exact production carrier is pending, so the protocol is implemented
against a documented error shape:

```
{:kind :wm/open-cascade-execution-error
 :cascade-digest <64-hex>
 :holes [{:hole-id :owner :question} …]
 :phase <keyword in #{:materialise :validate :compile :enact :evidence}>
 :attempts […]                     ;; prior failed attempts, preserved
 :resume-port {:retry (fn [ctx] …)}
```

**`:resume-port {:retry fn}` is the single adapter field D3 must supply**:
a retry entrypoint taking the resume context and re-performing the held
phase against the same cascade digest.  Everything else (recognition,
requisition, answer contracts, hot-load, click/selection accounting) is
protocol-managed.  The runner-service boundary supplies two
fixture-visible controls to `resume!`/`handle-error!`: `:click-budget`
(an atom of clicks spent) and `:selection-counter` (an atom of selector
calls), which the protocol asserts unchanged across every retry.

## What was built (`futon2.aif.wm.open-cascade-resume`)

- `recognize` — typed recognition of the open-cascade execution error;
  unknown class, malformed digest/holes/phase, or a missing `:resume-port`
  are typed errors (`:resume-error-unknown-class`,
  `:resume-cascade-digest-missing`, `:resume-phase-not-held`,
  `:resume-port-missing`), never abstention/refusal.
- `authoring-requisition` — the explicit requisition, identity-bound
  (value-digest over cascade-digest+phase+holes, stable), naming every
  hole (id/owner/question), the retained phase, and the author/reviewer
  pair; author = reviewer is refused (`:requisition-author-equals-reviewer`).
- `submit-answer!`/`validate-answer` — shared handoff contracts: answer
  names THIS requisition id and cascade digest, is authored by the
  requisition's author and accepted by its reviewer (distinct), resolves
  exactly the requisitioned holes (no foreign hole, none unresolved), and
  carries a loadable patch fn.  Invalid answers return typed errors and
  are preserved RAW in the ledger; the requisition stays open.
- `hot-load!` — applies the accepted patch under the digest+phase binding;
  a throwing patch is `:hot-load-failed` with the attempt preserved.
- `resume!` — retries the held phase in the SAME click: asserts
  `:clicks-spent 0` and `:selections-run 0` by comparing the runner's
  click-budget and selection-counter atoms before/after; a retry that
  secretly runs selection is caught as `:resume-protocol-violation` with
  both counts recorded.  Throwing retries are `:resume-retry-failed`.
  Every terminal state is `{:status :error :kind …}` — never
  `:abstained`/`:refused`/`:no-selection`.
- `handle-error!` — the end-to-end one-click protocol; ledger
  (append-only) keeps requisitions, answers (incl. raw failures), hot-load
  attempts, and retries.

## Gates run

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.wm.open-cascade-resume-test`
  — **9 tests, 56 assertions, 0 failures, 0 errors.**
- `clj-kondo --lint` both files — 0 errors, 0 warnings.

## Planted controls and what catches them

| planted defect | caught by |
|---|---|
| error of an unknown class | `:resume-error-unknown-class` |
| error without the D3 `:resume-port` adapter | `:resume-port-missing` |
| phase outside the seam family (`:select`) | `:resume-phase-not-held` |
| author = reviewer on the requisition | `:requisition-author-equals-reviewer` |
| answer naming another requisition/digest | `:answer-requisition-mismatch` / `:answer-cascade-digest-mismatch` |
| answer by/accepted-by the wrong identities | `:answer-author-mismatch` / `:answer-reviewer-mismatch` |
| holes unresolved or foreign hole invented | `:answer-holes-unresolved` (exact sets compared) |
| answer without a loadable patch | `:answer-patch-missing` |
| malformed answer discarding evidence | raw answer preserved in ledger; requisition stays open |
| patch that throws on hot-load | `:hot-load-failed`, attempt preserved |
| retry that runs selection again | `:resume-protocol-violation` (selection-counter observed), both counts recorded |
| retry that throws | `:resume-retry-failed` (:status :error) |
| new click spent on retry | click-budget asserted unchanged; `:clicks-spent 0` |
| terminal error relabeled as abstention/refusal | explicit negative assertion: no outcome is `:abstained`/`:refused`/`:no-selection` |
| retry history lost | failing retry then succeeding retry: both preserved, first never discarded |
| digest binding drift | requisition identity and answer contracts bind the exact cascade digest; retry ctx carries the same digest |

## Honest gaps

- No production wiring: the protocol is not yet invoked by the live
  debugger (`futon2.aif.wm.debugger`) or any runner service; D3 must supply
  `:resume-port {:retry fn}` and the runner boundary must pass its click
  budget and selection counter.  Guessing that wiring was out of scope by
  requisition.
- The requisition "dispatch" is a ledger record; actual bell/whistle
  delivery to the author agent is the dispatching boundary's job (the
  requisition map is bell-ready).
- `:attempts` on the error and `:prior-attempts` in the resume context are
  carried but not deeply validated (their shape belongs to D3's carrier).
