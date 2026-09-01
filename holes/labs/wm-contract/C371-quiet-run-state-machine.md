# C371 — executable quiet-run state machine

Date: 2026-09-01. Canonical execution authority:
`scripts/wm_quiet_run_state.py`. C319 remains the narrative/provenance record;
it is not an execution surface.

The append-only, fsynced, hash-chained ledger permits only this order:

```text
initial → quiescence → fence-held → tested-commit → reload-recorded
        → click-issued → click-terminal → certified → restored → released
```

Every transition except `released` consumes named files, stores their absolute
paths and SHA-256 hashes, and validates their verdict and identity. `released`
has no operator shortcut: it is available only from `restored`, and is the sole
transition that emits `FENCE-RELEASE`.

## Canonical commands

Choose one ledger and never edit it:

```sh
STATE=/tmp/$FENCE_ID-quiet-run-state.jsonl
python3 scripts/wm_quiet_run_state.py parking-request --fence-id "$FENCE_ID" \
  > "/tmp/$FENCE_ID-parking-request.json"
python3 scripts/wm_quiet_run_state.py init --ledger "$STATE" --fence-id "$FENCE_ID"
python3 scripts/wm_quiet_run_state.py status --ledger "$STATE"
```

The generated parking request is the only current request to Joe. Its writer
IDs, exact commands, required observations, acknowledgers, and counts are
rendered from the same restoration/fence authorities the transitions consume;
they are not copied from C319 prose.

Advance by supplying the receipt produced by the named external operation:

```sh
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to quiescence --evidence /tmp/quiescence.json

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to fence-held --evidence /tmp/fence.json \
  --attestations /tmp/$FENCE_ID-attestations.json

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to tested-commit --evidence /tmp/workspace-gate.receipt.json \
  --suite-receipt /tmp/futon2.receipt.json \
  --suite-receipt /tmp/futon3.receipt.json \
  --fence-evidence /tmp/fence-at-gate-start.json \
  --attestations /tmp/$FENCE_ID-attestations.json

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to reload-recorded --evidence /tmp/run-readiness.json

# The external observer writes one receipt after terminal status. Its accepted
# click identity retrospectively establishes click-issued; the next transition
# consumes the same immutable receipt for terminal identity/outcome.
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to click-issued --evidence "$CLICK_RECEIPT"
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to click-terminal --evidence "$CLICK_RECEIPT"

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to certified --evidence "$CERTIFICATE"

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to restored --evidence /tmp/restore-command.json \
  --manifest "/tmp/$FENCE_ID-restore-manifest.json" \
  --journal "/tmp/$FENCE_ID-restore.actions.jsonl" \
  --outcomes "/tmp/$FENCE_ID-restore.outcomes.jsonl" \
  --key-file /home/joe/.config/futon/writer-fence-restore.key

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" --to released
```

The `restored` transition uses the restoration tool's owner-only key, HMAC,
fence-ID, journal, and outcome validators. It then independently derives the
changed target population from the authenticated manifest and requires exactly
that population in both the park journal and successful outcome ledger. A
successful restore command with incomplete rows cannot release the fence.

## Interval policy

The machine chooses refusal, not implicit refresh. The fence observation must
be at most 300 seconds old when the bounded gate actually starts, and the
attestation must remain valid through gate finish. Admission delay is included
because the comparison uses the gate receipt's `started-at`, not submission
time. If either condition fails, obtain a new observation and a new gate run;
the existing transition does not fire.

The attestation claim deliberately ends at `tested-commit`. Reload and live
author/reviewer latency are unbounded, so later receipts carry
`attestation-coverage: not-claimed`. The machine still enforces their identity,
terminal, certificate, restoration, and ordering predicates; it does not claim
the original 60-minute attestation covered them.

## Operator notes (not machine claims)

Joe still owns parking, reload, and click; the dispatcher still owns writer
acknowledgements and the decision to abort. Those are operations that produce
the receipts above, not states inferred from someone reading prose. Recovery
guidance and reasons remain in C319/C305/C313, explicitly outside this
machine's claims.

Focused falsifiers:

```sh
python3 -m unittest -v test_wm_quiet_run_state.py
```

They reject skipped states, early release, stale fence evidence both before
fence entry and at gate start, and incomplete restoration populations.
