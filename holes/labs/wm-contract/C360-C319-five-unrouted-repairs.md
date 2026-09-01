# C360 — five unrouted C319 execution repairs

Date: 2026-09-01. Documentation and read-only checker controls only; no fence,
parking, bounded job, reload, or click was performed.

The canonical C319 sheet now makes five previously prose-only boundaries
executable:

1. services must reach inactive and remain byte-identical across two samples
   five seconds apart; a ten-minute settle timeout reports the named unit;
2. the prose acknowledgement population exactly equals the structured named
   principals. Additional sessions must enter the `sessions` array first;
3. each bounded launch parses its returned ID, polls that exact ID to a terminal
   receipt, and reports `BOUNDED-JOB-TIMEOUT ID` after 45 minutes;
4. a non-reporting click has a read-only, identity-bound status loop with typed
   `CONTINUE-WAITING`, `TERMINAL`, and `ABORT-WITH-WRITERS-PARKED` decisions;
5. release requires a second interval observation of the parked writer
   population after certification.

The post-click observation is intentionally narrower than pre-click
quiescence. Production creates authorised evidence files, so it reports
`WRITERS-STILL-PARKED` for coordinators, systemd units, and writable handles; it
does not falsely reassert clean repositories or zero jobs. The checker control
shows authorised dirty output does not defeat that scoped observation, while a
live parked unit still rejects it.

Focused controls:

```sh
python3 checks/writer_fence_evidence.py --self-test
python3 -m py_compile checks/writer_fence_evidence.py
```
