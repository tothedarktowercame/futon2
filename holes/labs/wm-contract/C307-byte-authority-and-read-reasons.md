# C307 — byte authority and typed read reasons

Date: 2026-09-01

## Contract

Captured bytes are authoritative. Size and SHA-256 derive from that exact byte
array. UTF-8 text is exposed only when a decoder configured with malformed and
unmappable input set to `REPORT` accepts the bytes.

- Text input: `:text-status {:status :present}` and `:text` is available.
- Binary/non-UTF-8 input: `:text-status {:status :absent :reason :non-utf8}`;
  no `:text` key is emitted. Bytes and digest remain available and stable.

This contract supports the captured PDF consumer without claiming that a lossy
Unicode view can reproduce binary bytes. Text-consuming checks fail normally
if they attempt to parse a binary entry.

## Typed file reasons

Capture and comparison now distinguish:

- `:absent` — the path does not exist;
- `:wrong-kind` — the path exists but is not a regular file;
- `:unreadable` — a regular file could not be read.

Capture failures carry `:read-set/reason` in exception data. Post-capture
movement entries carry `:reason`; callers never need to parse exception prose.

## Focused verification

`mutable-read-set-test` uses an actual invalid byte pair (`c3 28`), not a
Unicode string surrogate. It also removes a captured file, replaces one with a
directory, and injects an access-denied read after capture.

```text
clojure -X:test :nses '[mutable-read-set-test]'
  4 tests, 14 assertions, 0 failures, 0 errors

clj-kondo --lint checks/mutable_read_set.clj test/mutable_read_set_test.clj
  0 errors, 0 warnings
```

Focused positive runs of the five C304 consumers and R17 remained green,
including figure agreement extracting text from a temporary copy of captured
PDF bytes. C306's content-current/event-free interface remains out of scope.
