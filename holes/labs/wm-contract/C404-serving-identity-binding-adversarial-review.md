# C404 — serving-identity certificate binding adversarial review

Date: 2026-09-01. Review only; no implementation was changed. The declared
closed-dependency-set limitation is excluded from the findings.

## Verdict

The head comparison and unavailable handling are fail-closed, but the new
binding does not identify *which test run* supplies the tested head. A caller
can select an unrelated durable Futon2-CI-shaped job whose head matches the
serving observation and obtain `:program-identity-status :match` and
`:verdict :pass`, even when the quiet-run tested job named a different head.

The binding therefore establishes:

> the serving observation equals the head of a caller-selected passing job

not:

> the program that produced this run equals the program tested for this
> quiet-run attempt.

## Successful substitution

An adversarial durable-job record was constructed with:

```clojure
{:id "older-different-run"
 :agent-id "different-fence"
 :systemd {:ActiveState "inactive"}
 :receipt {:command "clojure -T:build ci"
           :outer-exit 0
           :verdict "pass"
           :repository-basis-stable true
           :repository-basis-start {:dirty false :head "serving-head"}
           :repository-basis-finish {:dirty false :head "serving-head"}}}
```

The intended quiet-run tested head was independently set to
`actual-quiet-run-tested-head`; it has no input position in
`tested-commit-from-job!`. Supplying the unrelated job id derived
`serving-head`. A production resource whose terminal serving observation also
said `serving-head` then produced:

```clojure
{:selected-job-agent "different-fence"
 :derived-head "serving-head"
 :serving-head "serving-head"
 :identity-status :match
 :verdict :pass}
```

`checks/certify_live_run.clj:121–141` validates terminal status, command,
exits, cleanliness, basis stability, and a nonblank finish head. It does not
check the job's `:agent-id`/fence id, the job ids recorded by the state
machine's `tested-commit`, its working directory/repository identity, or an
attempt identity shared with the click. It then discards the selected job id
and carries only the head into the certificate.

This permits the exact suppression requested by the attack: if the intended
tested job says `T` and the serving observation says `S`, selecting any valid
job that says `S` prevents
`:serving-program-differs-from-tested-program` from firing.

## Program-that-ran limit

The click observer records `:serving-runner-code` from terminal service status
(`checks/wm_click_resource_observer.clj:91–105`). The run record itself does not
carry that program identity, and the certificate does not bind the observation
to the instant the run began. Consequently the certificate compares the
selected test head with the program reported by the serving JVM at terminal
observation; it cannot independently establish that this was the program that
executed every stage of the run. A reload or substituted click receipt between
execution and terminal observation is not distinguishable from a single stable
serving program by this artifact alone.

This is separate from the closed-dependency-set limitation: it concerns the
time/attempt binding of the one identity the implementation does observe.

## What resisted attack

- A directly mismatched tested and serving head produces `:status :mismatch`,
  reason `:serving-program-differs-from-tested-program`, and certificate
  verdict `:fail`.
- Missing serving identity and missing tested commit both produce
  `:status :unavailable`. `program-identity-valid?` admits only `:match` or the
  deliberately non-serving `:not-applicable`; a production `:unavailable`
  therefore makes `base-valid?` false and the persisted verdict `:fail`.
- Dirty or unstable loaded identities do not match.
- A missing/blank job id, a nonterminal job, wrong command, nonpassing exit or
  verdict, dirty/unstable basis, or absent finish head is rejected before
  certificate construction.
- `:unavailable` was not found to be non-blocking in a downstream acceptance
  path. `certify_live_run` returns failure for the written failing certificate,
  and the quiet-run state machine accepts only certificate verdict `pass`.
- The focused author suite remains green: 11 tests / 41 assertions, zero
  failures and errors.

## Classification

This is a selector-authority defect, not a comparison defect. The durable job
receipt is real and its head is really compared; the caller is free to choose a
different valid receipt from the one whose result is being certified. Fixing
the equality predicate would not close it. The consumer needs the tested job
identity produced by the quiet-run chain, rather than a second caller-supplied
selection.

## Delivery inventory

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
