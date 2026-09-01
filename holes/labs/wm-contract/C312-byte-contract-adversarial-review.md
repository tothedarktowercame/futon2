# C312 — adversarial review of the C307 byte contract

Date: 2026-09-01. Reviewer: `wm-evidence`. Review authority: detached futon2
commit `e9bfaa1`. No substrate or consumer was edited.

## Verdict

**No C307 contract defect found.** Authoritative bytes, size, SHA-256, strict
UTF-8 exposure, binary absence, and the three read-failure reasons held under
the tested encoding and path-race fixtures.

The reasons are necessarily **observation-stage-relative**, not claims about a
path's final state. A deletion after the regular-file precheck but before the
read is correctly `:unreadable` with a `NoSuchFileException` cause, whereas a
path absent when classified is `:absent`. Consumers must not reinterpret the
reason as a durable terminal path state.

The shared checkout acquired C310 changes during review. Tests were therefore
run against an isolated detached `e9bfaa1` worktree rather than mixing the
requested C307 authority with in-flight work.

## Encoding boundary

| Fixture | Text outcome | Byte/digest outcome |
|---|---|---|
| UTF-8 BOM + `A` | `:present`, text retains BOM | exact round-trip; size 4 |
| valid `A` prefix + malformed `c3 28` tail | `:absent :non-utf8`; no text | exact bytes/SHA; size 3 |
| UTF-16LE BOM + `A` | `:absent :non-utf8`; no text | exact bytes/SHA; size 4 |
| bytes `00 41` | `:present` as NUL + `A` | exact round-trip; size 2 |
| UTF-8 encoding of a lone surrogate | `:absent :non-utf8`; no text | exact bytes/SHA; size 3 |
| empty file | `:present`, empty text | empty bytes SHA; size 0 |

The `00 41` fixture can be described externally as UTF-16BE without a BOM, but
it is also a valid two-byte UTF-8 sequence. Bytes do not carry an intended
encoding label, so exposing its valid UTF-8 view is correct under C307's byte
contract and is not a false `:present`.

For every text-present case, UTF-8 re-encoding reproduced the captured byte
array exactly. For every case, `:size == alength(:bytes)` and
`:sha256 == sha256-bytes(:bytes)`. No false `:non-utf8` was observed.

## Path and kind races

- Initially nonexistent path: capture exception reason `:absent`.
- Existing directory: capture exception reason `:wrong-kind`.
- File deleted after the regular-file precheck but inside `read-bytes`:
  capture exception reason `:unreadable`, cause class
  `java.nio.file.NoSuchFileException`.
- File replaced by a directory after capture: observation `:moved`, comparison
  `:unavailable/:wrong-kind`.
- Symlink target changed from one regular file to another: `:moved/:changed`
  with both target-content digests.
- Symlink target changed from a regular file to a directory:
  `:moved/:unavailable/:wrong-kind`.

This establishes the path contract follows the symlink target. It is a content
check, not a symlink-identity check; swapping to a target with identical bytes
would be endpoint-equal under C306 and would not establish event freedom.

No partial capture verdict was introduced: the C300 valid-plus-missing case
still fails before returning an observation.

## Focused verification

The committed C307 tests cover ordinary text, movement, binary bytes, typed
reasons, and same-byte size/SHA derivation. The encoding and symlink fixtures
above extend them without editing the test suite.

The live checkout was also checked after C310 settled; the namespace loaded
successfully. That is not substituted for the pinned C307 review.

## Inventory in the delivering commit

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

The exact result is reported after this record is committed. Inventory is a
classification check, not evidence for the byte-contract verdict above.
