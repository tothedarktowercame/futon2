# Correct invalid close outcome

**Status:** OPEN

## Observed failure

Target: not retained; stage: :close.

Context: {:phase :close}.

invalid close outcome

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance criteria

- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**
- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-2613ce611247ef5502fa693d1d7d8a0fbfcd85214a91d2ba2cd7fd18cc34a8ef](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-2613ce611247ef5502fa693d1d7d8a0fbfcd85214a91d2ba2cd7fd18cc34a8ef.edn)

Finding SHA-256: `9a336272a4a001af679321ae98ea50b7dc513f5dca1ab2ea9b6a67f1fc16e314`
