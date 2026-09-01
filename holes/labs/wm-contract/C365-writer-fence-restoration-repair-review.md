# C365 — independent review of the C357 restoration repairs

Date: 2026-09-01. Scope: fixture-only review of
`scripts/writer_fence_restore.py` at `adfa555`; no live manifest, journal,
coordinator, or systemd unit was touched.

## Verdict

The authentication and ordinary reverse-prefix retry controls hold, but the
tool is not yet a fail-safe restoration boundary. Two material findings remain.

1. **The HMAC secret has no ownership or mode requirement.** `read_key` accepts
   any readable 32-byte file, including mode `0644`; `--key-file` is optional
   and silently selects a fixed default path. A fixture using a world-readable
   key produced a valid HMAC and passed manifest authentication. Empty/short,
   missing, and wrong keys do reject, so the cryptography is functioning; the
   missing property is exclusive possession of the secret. HMAC with a
   world-readable or repository-carried key authenticates no actor.

2. **A successful inverse followed by an outcome-append failure is not
   retryable.** Forced `append_record` failure after `execute` and the restored
   postcondition left the target restored, no outcome file, and an error to the
   caller. The next invocation did not repeat or reconcile the completed
   inverse; it failed `journal-action-not-observed:1` because the target was no
   longer parked. Thus disk evidence and reality diverge at the exact boundary
   the outcome ledger is meant to close. The postcondition proves the action
   landed before the append failure, but that proof is not durably retained.

There is also a stated residual, not a third demonstrated corruption: the
re-observe/act pair is not atomic. Another writer can change a target after the
parked observation and before the inverse. A final restored postcondition can
still make the operation report success, so the tool proves final state, not
that no intervening event occurred. Closing that requires a target-side CAS,
revision token, or the independently held writer fence; millisecond adjacency
does not establish event-freedom.

## Controls that held

- A wrong 32-byte key rejects as `manifest-authentication-invalid`; a missing
  key exits 1; a short or empty key rejects.
- Missing journal, empty journal, malformed journal, and foreign-fence journal
  remain distinguishable: `NOTHING-RECORDED:journal-missing`, generic
  `NOTHING-RECORDED`, `journal-invalid-line:1`, and `journal-row-invalid:1`.
- A three-inverse sequence survived two staged failures. Run 1 restored ordinal
  3 then failed 2; run 2 skipped 3, restored 2, then failed 1; run 3 skipped 3
  and 2 and restored 1. The durable outcome ordinals were `[3, 2, 1]`.
- The committed focused suite passed: 8 tests, exit 0.

## Reproduction commands

The review imported `scripts.writer_fence_restore` with `FakeBackend` from
`test_writer_fence_restore.py`, used temporary directories, and injected:

- a mode-`0644` 32-byte key into `read_key` and `validate_manifest`;
- an `append_record` function raising `OSError` after a successful inverse;
- failures on inverse attempts 2 and 4 of a three-target restoration;
- missing, zero-byte, malformed, and foreign-fence JSONL journals.

The committed suite was run with:

```sh
python3 -m unittest -v test_writer_fence_restore.py
```

This is a report only. No restoration implementation or live state was changed.
