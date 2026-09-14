# Machinery-test run r8 authorization — claude-15, 2026-09-14

Authority chain unchanged. r8 spends cohort 49's LAST slot (attempt-002).

r7 was the campaign's furthest reach: author commit 6dfc172c (pin-path
equality + non-blank authority provenance, the two round-2 repairs)
corroborated, codex-24 APPROVED in round 1 with execution evidence, and
the grounding phase ended :ok under the 5cb4697c visibility fix. The
attempt then died in :stop-line-resolution with
:interoceptive/lock-path-refused: /run/futon2 — the root-provisioned
Row 18 lock parent on tmpfs — had disappeared between 22:59 and 23:37
(remover unidentified; no reboot, no journal trace). The refusal
cascaded: the finding writer needs the same lock, so r7 left no durable
finding and no closed checkpoint, only the adjudication sorry and the
journal line. Environmental failure, not a code seam.

Joe re-provisioned with sudo (2026-09-14 03:26Z): /run/futon2 0755
root-owned, joe-owned snapshot lock, root-owned deployment lease, plus
/etc/tmpfiles.d/futon2.conf so a reboot re-creates all three. Serving
JVM verified: with-store-lock acquires (:lock-ok).

Expected honest outcome: author round produces a fresh bounded parcel;
review approves; grounding resolves; :stop-line-resolution writes the
resolution — repair-attempt-001 to :awaiting-validation, the first
fully grounded in-attempt closure. Typed refusals remain acceptable.
Not qualifying; closes no row by itself.
