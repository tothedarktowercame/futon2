# C338 — C319 remaining execution repairs

Date: 2026-09-01. No parking, reload, click, certification, restoration, or
quiescence attempt was performed.

The seven remaining C331 defects are closed in the canonical C319 sheet and
bounded gate wrapper:

1. The submitting shell's validated `FUTON_WRITER_FENCE_ID` is embedded in the
   bounded command. A shell-local value no longer disappears at systemd-run.
2. Post-click abort restoration is forbidden until a typed terminal outcome
   proves no click write remains in flight. Unavailable/possibly-active means
   keep writers parked and escalate to Joe.
3. The pre-fence manifest queries full durable/runtime coordinator state and
   independent watchdog presence. Parking/restoration uses C333's watchdog-only
   terminal class and durable-stop running class.
4. The bounded summary now exposes outer `reason`, start/finish repository
   bases, and basis stability alongside inner/outer exits.
5. Futon2 launch, terminal status, and acceptance precede the separately shown
   Futon3 launch. The literal sequence cannot be pasted as two concurrent jobs.
6. The retired 75-candidate sentence is replaced by population v1: 62 content,
   6 event, 1 library, 0 unexplained.
7. Abort after acknowledgements but before parking explicitly marks the
   attestation aborted, releases every no-write promise, announces fence
   release, and retains the record.

The corrected request to Joe names one watchdog-only park, two durable
coordinator parks, and five background units. It no longer asks him to stop and
later continue a completed campaign.

The bounded-wrapper control injected a fence ID into a mocked submission and
observed the exact submitted command beginning with
`env FUTON_WRITER_FENCE_ID=wm-quiet-control`; the emitted receipt included all
four newly required outer evidence fields. An invalid identifier is refused
before submission.
