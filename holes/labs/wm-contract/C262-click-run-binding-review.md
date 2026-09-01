# C262 — adversarial click → run-id binding review

Date: 2026-09-01

Tested against futon3c `1ff8450564d895584a28f908d19f509ba990db19`
(C256 binding plus C259 run-record identity validation), with a clean diff for
the three tested runner-service files. All bindings and run records were
isolated temporary fixtures; no production click, run record, or live ledger
was touched.

## Verdict

The binding faithfully records the value returned by the runner, and the
single-flight and typed-absence boundaries work. It is **not atomic with the
terminal status transition**, does not enforce run-id uniqueness, and exposes
a mismatched run id as `:present` alongside the mismatch. It is not yet a safe
certificate load authority without a consumer that refuses every non-present
run-record status and duplicate identity.

## Findings

### Duplicate run ids are accepted

Two sequential public clicks received distinct click ids and both returned
`same-public-run`. Both binding files were written with
`:run-id-status :present`. A slow first click correctly caused an overlapping
click to return `{:rejected :already-running}`; after it closed, the second
click was accepted. Single-flight prevents overlap, not duplicate identity
across time.

No uniqueness index or collision check exists at the binding boundary. A
duplicate producer id can therefore bind two clicks to one run identity. A
later run-record search may detect ambiguity, but the binding itself reports
both as valid present IDs.

### Run-record mismatch is typed but the wrong id stays present

A runner returned `returned-run` while its supplied run record contained
`record-run`. The terminal result was:

```
:outcome              :grounded-change
:run/id               "returned-run"
:run-id-status        :present
:run-record-status    :identity-mismatch
:run-record-absence   :run-record-identity-mismatch
```

The mismatch is loud, which is an improvement from C259, but there is no
single binding verdict. A consumer reading only `:run/id` and
`:run-id-status` will accept the contradicted identity. Certificate loading
must require the conjunction of present run id, present matching run record,
matching click id, and uniqueness; the current shape permits partial reading.

### Binding and terminal status are ordered, not atomic

`close-click!` persists/renames the binding and only then swaps the terminal
status. An injected exception immediately after the real binding rename and
before the status swap produced:

```
binding on disk  {:click/id click-post-persist,
                  :run/id run-post-persist,
                  :run-id-status :present}
status           {:running? true, :last-result nil}
```

Through the public worker catch path, the same interleaving produced a binding
on disk for `run-public-window` while terminal status became
`:outcome :service-failed` with no run binding. The two authorities disagree.
This is the C258 post-commit window in the click layer.

The reverse crash outcome also remains structurally possible under power loss:
the binding uses `spit` plus atomic rename but does not force the temporary file
or parent directory before publishing terminal status. Atomic visibility is
not durable atomicity; a status acknowledged before the rename is stable can
survive in process while the binding disappears on restart.

### Typed absence and timestamp refusal hold

A result containing a timestamp but no run id produced exactly:

```
:run-id-status :absent
:run-id-absence :runner-did-not-return-run-id
```

No timestamp-derived identity appeared. A runner that did return a run id but
then hit a pre-persistence filesystem failure became `:service-failed`; it did
**not** falsely claim `:runner-did-not-return-run-id`. The absence reason is
therefore honest on the exercised paths.

## Focused checks

The public service fixture exercised slow overlap, sequential clicks,
duplicate IDs, missing IDs with a timestamp, run-record mismatch,
pre-persistence failure, and post-rename/pre-status failure.

The focused committed runner-service suite also passed:

```
clojure -M:test -n futon3c.wm.runner-service-test
9 tests, 73 assertions, 0 failures, 0 errors
```

No repository-wide suite or gate was run.

## Required falsifiers before certificate loading trusts the binding

1. Reject or type a duplicate run id across click bindings.
2. Expose one compound validity verdict; a mismatched/unavailable run record
   must not coexist with an independently consumable `:run-id-status :present`.
3. Give binding persistence and terminal publication a recoverable commit
   protocol, including file and directory force, and reconcile startup after a
   crash between the two authorities.
4. Preserve the demonstrated rule that persistence failure is service failure,
   not fabricated run-id absence, and that timestamps never infer identity.
