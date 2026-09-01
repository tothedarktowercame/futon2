# C371 — executable quiet-run state machine

Date: 2026-09-01. Canonical execution authority:
`scripts/wm_quiet_run_state.py`. C319 remains the narrative/provenance record;
it is not an execution surface.

The append-only, fsynced, hash-chained ledger permits only this order:

```text
initial → quiescence → fence-held → tested-commit → reload-recorded
        → click-issued → click-terminal → certified → restored → released
```

Every transition except `released` stores the absolute paths and SHA-256 hashes
of its evidence. Every resume re-reads every prior artifact, re-hashes it, and
validates the complete state order and fence identity. Quiescence and fence
observations are run by the machine itself; bounded evidence is resolved by
durable job ID through `bg.py` and systemd. `released`
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

Advance using producer observations and durable job identities:

```sh
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to quiescence

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to fence-held \
  --attestations /tmp/$FENCE_ID-attestations.json

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to tested-commit \
  --job-id "$WORKSPACE_GATE_JOB_ID" \
  --job-id "$FUTON2_SUITE_JOB_ID" \
  --job-id "$FUTON3_SUITE_JOB_ID"

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to reload-recorded --evidence /tmp/run-readiness.json

# The external observer writes one receipt after terminal status. Both
# transitions resolve the serving JVM's terminal status, durable click/run
# binding, and run record; the receipt alone is insufficient.
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to click-issued --evidence "$CLICK_RECEIPT"
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to click-terminal --evidence "$CLICK_RECEIPT"

# The machine invokes certify_live_run.clj itself, using the recorded click
# receipt and producer-bound Futon2 test job. It persists the generated output.
python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to certified

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" \
  --to restored --evidence /tmp/restore-command.json \
  --manifest "/tmp/$FENCE_ID-restore-manifest.json" \
  --journal "/tmp/$FENCE_ID-restore.actions.jsonl" \
  --outcomes "/tmp/$FENCE_ID-restore.outcomes.jsonl" \
  --key-file /home/joe/.config/futon/writer-fence-restore.key

python3 scripts/wm_quiet_run_state.py advance --ledger "$STATE" --to released
```

Launch all three bounded jobs with `--agent "$FENCE_ID"`. The transition
requires that producer-recorded attempt identity on every job, exactly one of
each canonical command, terminal systemd units, and the same Futon2 commit for
the workspace gate and Futon2 suite.

The `restored` transition uses the restoration tool's owner-only key, HMAC,
fence-ID, journal, and outcome validators. It then independently derives the
changed target population from the authenticated manifest and requires exactly
that population in both the park journal and successful outcome ledger. A
successful restore command with incomplete rows cannot release the fence. It
also observes every target through the live backend; records alone do not
establish restoration.

## Interval policy

The machine chooses refusal, not implicit refresh. The fence observation must
still be at most 300 seconds old at machine ingestion; caller-authored receipt
times cannot freshen it. The attestation must remain valid through gate finish.
Systemd's boot-relative process-start field is deliberately not claimed: the
fence observation is wall-clock based and records no boot identity or monotonic
sample with which to compare it. If either enforced condition fails, obtain a
new observation and new bounded runs; the existing transition does not fire.

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

They reject synthesized mid-chain ledgers, changed evidence, handwritten
bounded evidence, stale evidence paired with an asserted fresh timestamp,
mismatched gate/suite commits, early release, and incomplete restoration.
