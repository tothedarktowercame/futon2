# Correct store busy

**Status:** OPEN

Parent: M-peradam-grounding

## Observed failure

Target: M-peradam-grounding; stage: :cascade-resume.

:clock/store-busy

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance criteria

- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**
- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-injury-5ca30d9d7509ec6d70181a5041df37c972fe2e5f](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-injury-5ca30d9d7509ec6d70181a5041df37c972fe2e5f.edn)

Finding SHA-256: `432fd8c4f41961699eded9fe23b78f4722328d630181de35643f43521b56f7be`

## Injury provenance

```edn
{:injury-id "injury/5ca30d9d7509ec6d70181a5041df37c972fe2e5f", :cause-record-pin {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-d78842b4-2dc8-4aca-bab7-c90d982f467f.edn", :sha256 "781a049aff13d55610ef4e9996361fddaba98d74bb10a94013c1db26032d230e"}, :origin-run/id "2026-11-05-d78842b4-2dc8-4aca-bab7-c90d982f467f", :finding-sha256 "432fd8c4f41961699eded9fe23b78f4722328d630181de35643f43521b56f7be"}
```
