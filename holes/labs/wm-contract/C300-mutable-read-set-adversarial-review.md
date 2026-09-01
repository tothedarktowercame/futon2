# C300 — adversarial review of the mutable read-set

Date: 2026-09-01. Reviewer: `wm-evidence`. Review target: futon2 `0c88124`.
No substrate or consumer was edited.

## Verdict

The substrate is sound for the narrower claim **“these valid UTF-8 bytes were
captured once and the same content was observed again at comparison.”** It does
not establish the broader claims **“text and digest are interchangeable”** or
**“the input did not move.”** Two material limits and one outcome-typing defect
were demonstrated.

## Adversarial results

### 1. One capture really supplies bytes, text, size, and digest

A file changed in `after-capture` produced `:moved/:changed`. The snapshot's
text still equalled `String(snapshot.bytes, UTF-8)`, and its SHA equalled
`sha256(snapshot.bytes)`. Mutation after capture cannot split those fields;
they are derived synchronously from the retained byte array.

**Limit found — invalid UTF-8 is lossy.** A three-byte fixture `ff fe 41`
captured size 3 and SHA
`e338b52c1bba42031362180fb1465d6e8b382881cb2f2601e30e971f21e4901c`.
Its decoded text was `��A`; re-encoding that text produced seven bytes and SHA
`e95a7363204eb40353cb0c8e7dde3aa6a23aafa95f59a9d0c7e490271e236d05`.
Thus text and SHA come from one capture but do not necessarily denote the same
reconstructable content. For the present Clojure/EDN consumers, valid UTF-8 is
an unstated precondition. Without validation, a consumer may parse replacement
characters while provenance names different raw bytes.

### 2. Movement and ABA

- `before → after` returned `:moved`, named the path, and supplied captured and
  current SHA values.
- Of two inputs, changing only the second returned one `:unchanged` and one
  `:changed`, correctly localizing the movement.
- `before → middle → before` inside `after-capture` returned `:stable` and
  `:unchanged` because the comparison is content equality at two instants.

The ABA outcome is defensible for content-snapshot checks: the verdict consumes
the same bytes that exist again at comparison. It is not evidence that no
writer ran, no transient invalid state existed, or the observation window was
quiescent. C297's prose says movement becomes explicit without stating this
limit. Consumers needing no-concurrent-mutation require a filesystem/version
token; this digest sandwich cannot supply it.

### 3. Reasons and typed outcomes

A changed input preserves a strong reason: `:changed`, exact path, and both
digests. The unavailable path is weaker:

- deleting the file after capture returned top-level `:moved`, comparison
  `:status :unavailable`, and `:cause` equal only to the path string;
- replacing it with a directory returned the same status pair with cause
  `"Is a directory"`.

Vanished and unreadable are therefore not typed outcomes. They may sometimes
be guessed from free-text exception messages, but callers cannot reliably
distinguish them. At initial capture, `regular-file?` also labels every
non-regular input as “absent,” so a directory and a nonexistent path collapse
in the other direction. The top-level `:moved` is accurate as a refusal to
certify stability, but it is not a complete account of why comparison failed.

### 4. Partial capture

Capturing `[valid-file, absent-file]` threw
`ExceptionInfo("mutable read-set input is absent")` with the missing path and
returned no snapshot or observation. No partial read-set can reach a verdict.
This is fail-closed and not a vacuous pass. It does mean initial capture failure
is an exception rather than the same typed result vocabulary used after
capture.

### 5. Existing focused tests

```sh
clojure -X:test :nses '[mutable-read-set-test]'
```

Result: 2 tests, 6 assertions, 0 failures/errors. These establish the ordinary
valid-UTF-8 stable and changed cases; they do not cover invalid encoding, ABA,
vanished/unreadable distinction, or multi-input partial capture.

## Findings to route

1. **UTF-8 validity is neither checked nor typed.** Either reject invalid UTF-8
   before exposing `:text`, or state and enforce the input contract at every
   text consumer.
2. **`:stable` means equal bytes at two observations, not no movement.** The
   substrate/output documentation must not support quiescence or no-writer
   claims without a revision token.
3. **Post-capture `:unavailable` and initial “absent” collapse vanished,
   unreadable, and wrong-kind inputs.** The comparison identifies the path but
   not a machine-readable cause.

The single-capture derivation, per-input movement localization, and fail-closed
partial-capture boundary held under attack.
