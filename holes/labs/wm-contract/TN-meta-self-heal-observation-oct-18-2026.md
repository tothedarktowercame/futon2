# TN — META self-heal observation, 2026-10-18

## Question

When a production click identifies the War Machine as broken, does META select
an `:algorithm` task and execute `:run-algorithm`, without an operator or repair
agent fixing the machine from outside?

## Protocol

- Three ordinary production clicks, with `wm-author`, `wm-reviewer`, and
  `wm-repair-reviewer` reset to fresh conversations before each click.
- The ordinary debugger remained attached so that the exact stopping phase and
  condition were retained.
- `wm_click_repair_loop.py --observe-only` prohibited external repair dispatch.
  A debugger stop was aborted after capture so the single-flight boundary could
  advance to the next observation.
- No Codex-side repair was made between the three clicks.

The observation-only mode was introduced at futon2 `62e29c87b`. Its first
trial exposed a second repair-dispatch branch at a debugger-held stop. That
trial (`wm-click-91c3f069-…`) is excluded: the mistakenly dispatched job was
cancelled, the held phase was aborted, and the defect was closed with a planted
test at `8028c54a4`. Futon3c `3b91ad40b` records the one replacement click.

## Clean observations

| run | click | wall clock | META field ranking | terminal observation | external repair |
|---|---|---:|---:|---|---|
| `2026-10-18-0b816790-ff10-4381-9d0d-321027d2b7f2` | `wm-click-db3e7eec-f742-40e7-bc27-8a8c3c788011` | 201,756 ms | 72,009 ms | `:abstained`, selection, `:no-acting-cascade-candidate` | skipped |
| `2026-10-18-5f024334-c330-40d8-9e2e-126e1377083c` | `wm-click-5744ba2b-17f0-4c3a-a3ad-341650b95874` | 146,848 ms | 14,532 ms | `:abstained`, selection, `:no-acting-cascade-candidate` | skipped |
| `2026-10-18-e2d6d367-f7df-4c63-ae1a-78cc092d2ec4` | `wm-click-b22320fd-ab1d-47b5-915e-f5834a58a9e9` | 115,163 ms | 14,017 ms | `:abstained`, selection, `:no-acting-cascade-candidate` | skipped |

The click ledger, not the wrapper's display counter, is authoritative here.
The ledger contains the three distinct click IDs above and moved the durable
budget from 92/95 to 95/95. The wrapper displayed `2/3` because one shell call
observed two asynchronously accepted clicks across the single-flight boundary;
it then made a harmless, unspent attempt after the budget was exhausted.

## What self-repair did do

Every failure was converted automatically into a durable open repair ticket:

- `54d1deffd` — `T-repair-occ-ad7f485f…`
- `0ab3970ca` — `T-repair-occ-41cfef2f…`
- `b04e2df1c` — `T-repair-occ-3d71c1a3…`

Those tickets accurately record selection failure, but only at the generic
grain “Verify or restore abstained”; their target is `not retained`. This is
failure detection and obligation publication, not execution of the META
self-heal Algorithm.

## Verdict

**META Algorithm selection and execution are not proved working.** All three
runs spent measurable time in `:meta-field-ranking`, so the META field code was
entered. But each terminal run record says:

```clojure
{:outer-task-selection
 {:schema :wm/outer-task-selection-v1
  :status :absent
  :reason :receipt-not-retained}}
```

No run retained a chosen `:algorithm`, emitted `:run-algorithm`, or executed an
algorithm action. All three instead stopped in ordinary selection with
`:no-acting-cascade-candidate`. Therefore this is the case Joe identified:
outside implementation is warranted, not to repair the selected task, but to
repair the META route so that (1) its selection receipt survives a failed inner
selection and (2) a detected injury can actually choose and run an admitted
algorithm.

## Primary evidence

- Run records:
  `data/wm-runs/tick-run-record-2026-10-18-{0b816790…,5f024334…,e2d6d367…}.edn`
- Debugger stops:
  `data/wm-click-loop/debugger/held-2026-10-18-{0b816790…,5f024334…,e2d6d367…}.md`
- Campaign ledger: `data/wm-click-loop/ledger.jsonl`
- Durable click ledger: `data/wm-ordinary-clicks/consumption.jsonl`
- Failure tickets: the three `holes/tickets/T-repair-occ-*.md` paths named
  above.

