# C269 — incomplete-work verdict audit

Date: 2026-09-01

Scope: adversarial fixtures and focused controls only. No production click, full
workspace gate, implementation change, binding refresh, or restamp was made.

## Binding-status scope

The C267 contradiction has one root: `:run-id-status` describes a raw token,
while readers can mistake it for a verified click/run binding. The following
states can coexist today:

| Raw run ID | Run record | Current representation | Assessment |
|---|---|---|---|
| returned | matches click and run ID | `:present` / `:present` | verified only after both comparisons |
| returned | identity differs | `:present` / `:identity-mismatch` | class-9 contradiction for a partial reader |
| returned | unreadable | `:present` / `:unavailable` | raw datum exists; identity is not verified |
| returned | not returned | `:present` / `:absent` | possible honest partial result, not a verified binding |
| absent | any | `:absent` | no binding identity available |

The recommended authoritative field is therefore a compound binding verdict,
not a replacement for the raw observation:

```clojure
{:run-id-observation {:status :present :value "..."}
 :binding-status :verified|:absent|:unavailable|:identity-mismatch|:duplicate}
```

Only `:binding-status :verified` may feed certificate identity. This scope also
makes duplicate identity a binding failure without pretending the producer did
not return the token.

### Neighbouring status pairs

- `:run-record-status :identity-mismatch` and `:unavailable` are the same
  class-9 family when paired with the unqualified word `:present`.
- Resource `:status :clean` and `:execution-outcome :incomplete` concern
  different dimensions (resource health versus run completion), but their
  current unscoped names are hazardous. They directly produced C267's passing
  certificate. Prefer `:resource/status :clean` and an independently required
  `:execution/status :completed` rather than treating either as the other's
  verdict.
- Run-readiness can retain a source receipt's `verdict: pass` while returning
  its own `pass: false` for stale provenance. This is not contradictory: the
  former is historical source evidence and the latter is the authoritative
  current-readiness verdict. Its nested placement and evidence label preserve
  that distinction.
- No additional contradictory pair was found in the readiness result family:
  a failed item always carries a blocker and resolution kind, and aggregate
  readiness is the conjunction of item `pass` fields
  (`scripts/run_readiness.py:367-392`).

The lintable rule is narrow: within a record, no unqualified status may claim
one fact is present/valid when the authoritative status for that same fact is a
failure. Orthogonal dimensions must be namespace-scoped. A blanket ban on
coexisting statuses would incorrectly reject honest layered evidence.

## Incomplete-work mutations

### Operational certificate — **passes incomplete work (confirmed C267)**

An incomplete resource receipt plus a one-hop run record with
`traceWritten false` returned certificate `:verdict :pass`, exit 0. The pass
predicate omits completion and accepts `:execution-outcome :incomplete`
(`checks/wm_operational_certificate.clj:27-38,56-110`).

### Chain rehearsal — **inherits the certificate defect**

The rehearsal asserts its fixture runner reports the literal outcome
`:rehearsed`, but the generated run record is copied from a fixture and its
certificate assertion delegates to the same operational checker
(`futon3c/test/futon3c/wm/chain_rehearsal_test.clj:50-61,130-152`). It does not
assert `traceWritten`, a terminal run outcome in the record, or complete route
coverage. Consequently a trace-incomplete record with the existing
`:rehearsed` service label follows the same passing certificate path. The
rehearsal distinguishes identity mismatch, not unfinished topology evidence.

### Strict contract lint — **passes an empty authoritative population**

Fixture:

```json
{"source":{"module":"DarkTower.WarMachine.Holes",
           "git-sha":"c269-authority"},
 "declarations":[]}
```

with registry `[]`.

Invocation:

```sh
bb -cp . checks/contract_lint.clj --strict \
  --contract /tmp/c269-empty-contract.json \
  --registry /tmp/c269-empty-registry.edn \
  --report /tmp/c269-empty-report.edn --authority c269-authority
```

Observed exit 0: `structural-valid true`, `bindings-fresh true`,
`bindings-inspectable true`, `strict-qualification true`. Strict is explicitly
the conjunction of those three properties and has no nonempty or expected
declaration-population condition (`checks/contract_lint.clj:473-486`). Thus its
implementation is honest about *qualification*, but it cannot by itself attest
that contract generation finished. Any gate consuming it as contract
completeness can certify a truncated-to-zero contract.

### Holder check — **passes an empty authoritative population**

An isolated copy was pointed at the same zero-declaration contract and
`{:records {}}`. Observed exit 0:

```clojure
{:pass? true, :declarations 0, :records 0,
 :orphaned-declarations 0, :problems []}
```

The universal ownership assertion is vacuously true because there is no
expected-population or nonempty condition (`checks/holder_check.clj:30-65`).
Like strict qualification, holder ownership is a legitimate narrow property,
but its PASS cannot establish that contract generation completed.

### Workspace gate — **component verdict can pass an unfinished tree; readiness catches it**

The inner gate prints repository provenance, including `dirty?`, but determines
its exit solely from child exits (`checks/wm_workspace_gate.clj:294-309`). A
dirty or changing tree is therefore not itself an inner-gate failure if every
child exits 0. This is intentionally repaired one layer out: the bounded receipt
and run-readiness require stable, clean, matching start/finish bases.

Focused controls:

```text
python3 scripts/run_readiness.py --tree-control
  exit 0; recent different tree rejected
python3 scripts/run_readiness.py --workspace-gate-receipt-control
  exit 0; different p4ng basis rejected as workspace-basis-differs
```

Accordingly the raw workspace-gate exit is not a safe repository snapshot
verdict by itself. The consumed readiness verdict does reject unfinished or
moving repository evidence (`scripts/run_readiness.py:57-77,113-173`).

### Run-readiness — **no incomplete-work pass found**

The examined incomplete cases are loud:

- absent terminal bounded receipts fail;
- dirty current trees fail;
- unstable or changed tested trees fail;
- missing workspace provenance or a changed repository basis fails;
- unavailable reviewer and serving-code mismatch fail.

The two focused provenance controls above both rejected their mutations. No
analogous pass-on-unfinished mutation was found in run-readiness. Its remaining
`certify-run-command` item checks only that the future command resolves; it does
not claim a run or certificate already exists, so this is not completion
certification.

## Result

Beyond the operational certificate, two narrow checks accept truncated empty
populations (strict contract qualification and holder ownership), and the chain
rehearsal inherits the certificate's missing completion requirement. The raw
workspace-gate exit can describe a dirty/in-motion basis as passing, but the
bounded receipt plus run-readiness correctly refuses to promote it. No separate
incomplete-work acceptance was found in run-readiness itself.

