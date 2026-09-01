# C321 — runnable writer-fence evidence bundle

Date: 2026-09-01. Owner: `wm-evidence`.

## Canonical command and exit contract

```sh
cd /home/joe/code/futon2
python3 checks/writer_fence_evidence.py \
  --fence-id FENCE-ID --attestations /path/to/fence-attestations.json
```

- exit `0`: `FENCE-VERIFIABLE` — every named observed condition is parked and
  the complete attestation set was supplied;
- exit `1`: `FENCE-BREACH` — at least one observed condition is not parked;
- exit `3`: `FENCE-INDETERMINATE` — an observation was unavailable or moved,
  or the observed world is clean but attestations are incomplete;
- exit `2`: a self-test mutation escaped.

The command is read-only. It neither stops nor resumes a unit, coordinator, or
job. An observed breach dominates missing attestations: an unfenced world is a
breach, not merely indeterminate because nobody attested to it.

The attestation file is structured JSON. Its content, rather than its filename,
binds the acknowledgement to one named, expiring fence window:

```json
{
  "schema": "wm-writer-fence-attestation-v1",
  "fence-id": "FENCE-ID",
  "issued-at": "2026-09-01T01:00:00Z",
  "expires-at": "2026-09-01T02:00:00Z",
  "acknowledged-by": {
    "operator": "joe",
    "dispatch-coordinator": "claude-20",
    "publisher": "claude-1",
    "sessions": ["wm-nouns", "wm-verbs", "wm-organization", "wm-evidence"]
  },
  "writer-population": {
    "coordinators": [
      "jit-queue:jit-m94A03-retry-v3",
      "jit-queue:jit-all-open-v2",
      "ftriangle-live-smoke-v1"
    ],
    "units": [
      "apm-campaign-babysit-jit-all-open-v2.service",
      "apm-watchdog.timer", "apm-watchdog.service", "apm-closer.service",
      "apm-axiom-audit.timer", "apm-axiom-audit.service",
      "futon-pattern-index.timer", "futon-pattern-index.service"
    ]
  },
  "intended-state": {
    "coordinators": {
      "jit-queue:jit-m94A03-retry-v3": "terminal-complete-watchdog-stopped",
      "jit-queue:jit-all-open-v2": "durably-stopped",
      "ftriangle-live-smoke-v1": "durably-stopped"
    },
    "units": "inactive",
    "writable-handles": "none",
    "c292": "QUIESCENT"
  }
}
```

The interval must contain the observation time and may span at most two hours.
A foreign, stale, incomplete, or differently scoped attestation is invalid.
The report records the canonical attestation content SHA-256. This is a content
binding, not a digital signature: human identity remains attested, while the
machine independently observes the named world state twice.

The output keeps `observed`, `attested`, and `unverifiable` as separate data.
The last category always names the limits that the first two do not turn into
machine facts: future manual/editor starts, future dispatch, a writer with no
currently open handle, an unenumerated in-JVM writer, and cross-authority ABA.
Thus `FENCE-VERIFIABLE` means the named observations and attestations support
opening the window; it does not mean the command proved metaphysical absence
of every possible writer.

## Captured observations

Each endpoint captures:

- the three durable coordinators through the serving JVM, including registry
  enabled/lifecycle state, runtime-scheduler presence, durable status/epoch,
  tick claim, and same-coordinator/same-epoch quiescence witness;
- SHA-256 of the durable coordinator registry;
- timer and paired-service active/sub states and invocation IDs for babysitter,
  watchdog, closer, audit, and pattern-index units;
- every observable writable `/proc` file descriptor beneath the five certified
  repositories, including PID, FD, flags, path, and command;
- C292's complete quiescence verdict.

The entire capture is repeated. A clean but unequal pair is
`FENCE-INDETERMINATE`; it is not averaged into success. Any active coordinator,
unit, writable handle, or C292 failure at either endpoint is `FENCE-BREACH`.

## Controls and unfenced dry run

```sh
python3 checks/writer_fence_evidence.py --self-test
```

The controls demonstrate all three verdicts: a parked fixture plus complete
attestations is verifiable; the same observations without attestations are
indeterminate; an active unit is a breach; and movement between clean
observations is indeterminate. They also reject foreign and stale attestations.

The required read-only dry run against the unfenced live world returned exit
`1`, `FENCE-BREACH`. At both endpoints it reported:

- all three named coordinators not durably quiescent;
- active babysitter, watchdog timer, closer, audit timer/service, and pattern
  timer;
- writable handles inside a certified repository;
- C292 not quiescent.

The dry run had no attestation file (`:status absent` in the emitted JSON), and
still returned breach because multiple observed writers were present. Nothing
was parked to produce this control.
