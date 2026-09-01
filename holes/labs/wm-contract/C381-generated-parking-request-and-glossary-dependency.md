# C381 — generated parking request and pending glossary dependency

Date: 2026-09-01. No writer was parked and no paper artifact was edited.

`scripts/wm_quiet_run_state.py parking-request --fence-id ID` is now the sole
parking-request producer. It renders writer IDs, exact operator commands,
required observations, acknowledgers, and counts from the restoration and
fence-evidence authorities used by the state machine. The initial ledger row
pins the rendered specification by SHA-256. A population control requires exact
agreement across all three authorities (three coordinator writers and eight
systemd units), so the former prose phrase “five background units” cannot
silently return.

## Pending generated-paper dependency

The live glossary-assurance paragraph currently reports “31/31 positive
sources pinned.” C376 established that its upstream receipt validator measured
reproducibility rather than successful elaboration: 11 of 30 receipts recorded
elaboration exit 1 while passing. C379 owns the validator/receipt repair and
will establish the true verifying fraction.

Dependency disposition: **watch C379; regenerate from its corrected live split**.
Do not edit the generated paragraph or its count directly. Until C379 lands,
the current paragraph is reproducible from its declared inputs but its
“positive source” interpretation is not authoritative.

Focused controls:

```sh
python3 -m unittest -v test_wm_quiet_run_state.py
python3 scripts/wm_quiet_run_state.py parking-request --fence-id fixture
```
