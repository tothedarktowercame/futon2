# G1 observer boundary review — 2026-09-10

Independent review of Codex12 job 88027733. The loaded serving vars and the
separate Babashka recording process are different evidence sources. Required
environment values alone cannot attest effective loaded values.

The proposed post-accept observer invocation is not a complete RUN4 recording
adapter. `wm_step_observe.bb:122-156` selects the previous accepted step from
`pin/pin.edn`; its first-step arm emits unmarked typed absence. Its trace reader
uses tick receipts and wm-trace files in the canonical lab runs directory,
not the RUN4 full-loop terminal bundle. `wm_step.sh:544-565` invokes it after
deposition but BEFORE the battery and pin advancement. A hook after acceptance
would reverse that dependency.

Executed in a disposable work directory, with print-only enabled:

```
FUTON_WM_RECORDING_CONTRACT=1 bb holes/labs/wm-contract/wm_step_observe.bb <temp-dir> RUN4-first-step-review --print
```

Exit 0; result `:observation/status :typed-absence`, reason
`:no-previous-accepted-step`, no `:recording-contract` field. No record was
written. Therefore flag-on plus process success is insufficient, and demanding
a marked paired observation on the first trial is not the existing contract.

Implementation split: independently implement and test pinned required/current/
effective serving-value attestation before click, persisting provenance without
claiming recording completion. Recording integration must consume actual joined
trial evidence using the existing realized-recording constructor/validator and
explicit unknowns, or supply the observer's actual supported input carrier.
It must precede the battery that checks it, use configured isolated roots, and
must not fabricate a previous accepted step or turn terminal success into
operator acceptance. Codex10 owns that serving/battery integration. Codex12's
absence-of-hook finding is accepted; its command alone is not an executable
solution for U88's first trial.
