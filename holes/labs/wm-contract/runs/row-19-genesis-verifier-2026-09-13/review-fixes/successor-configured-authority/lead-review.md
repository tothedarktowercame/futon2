# Configured authority review — codex-26, 2026-09-13

Subject04343e0e/5a1df387, receipts3dbf960c. Four source/test/runner pins
match actual bytes (lead-pins.json). No passing test rerun. Successor kind and
predecessor now enter acceptance subject; exact trace identity/scope checks close
the prior named relation gaps. New concrete adapters still require repairs:

1. configured-record hashes a path, then reopens it to parse. Changing the file
   between reads permits unpinned content to be returned. Read once, hash and
   strict-parse those same bytes, with typed IO/decode/parse failures. This applies
   to every configured authority record including host-origin review.
2. Host JSONL readAllLines strips original line terminators and reconstructs LF.
   A CRLF or missing-final-newline file can match a pinned LF reconstruction
   despite different raw bytes. Select the exact raw record including its actual
   terminator; hash the same bytes whose JSON content is inspected. Add LF/CRLF/
   unterminated-last-line controls and explicit user-role/session identity checks.
3. Artifact adapter reads a pinned EDN metadata record and returns its :sha256;
   it never reads the named source/tests/review bytes. The metadata can remain
   unchanged while the actual artifact changes. Resolve an independently configured
   artifact byte path and recompute its digest; distinguish metadata digest from
   artifact-content digest. Add unchanged-metadata/changed-artifact refusal.
4. decorate overwrites verification-scope/status of every record. A pinned record
   marked fixture or refused can be silently relabelled production/verified by
   config. Reject conflicting stored scope/status, and validate record-specific
   schemas/outcomes instead of laundering them through generic decoration. Scope
   agreement in verify-candidate cannot detect this after the overwrite.

The host-event path still requires a separate pinned origin review; that is not
itself completed origin authentication. An accepted review must bind the exact
source event, delegate/root, reviewer and explicit host trust model; do not let
an arbitrary accepted EDN map stand in for this evidence. No signature requirement
is added. No real root, anchor, predecessor or admission is accepted here.
