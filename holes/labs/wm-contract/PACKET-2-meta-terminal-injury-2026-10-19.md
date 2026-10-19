# Dedicated META self-heal — Packet 2 receipt

Lineage: Packet 1 repair `cff70f9fb77727abbefd9fd2d1a7027f4ad23251`.

Scope is terminal failure classification and durable append integration only.
There is no coordinator, Agency dispatch, loop-node, construction fold, D-task,
serving-click, or live-production-ledger change.

## Added schemas and API

`:wm/meta-terminal-classification-v1` contains the immutable run/click IDs,
full run-record pin, typed injury/non-injury verdict, failure kind and observed
stage, trace/output presence, stable cause/injury IDs for injuries, and a
recomputed classification pin.

Public classification/integration API:

- `meta-terminal-injury/classify`
- `meta-terminal-injury/verify-classification`
- `meta-terminal-injury/integrate`
- `meta-terminal-injury/replay`

Public durable API:

- `meta-injury-store/read-one-form`
- `meta-injury-store/load-ledger`
- `meta-injury-store/transact-classification!`
- `meta-injury-store/ingest-record-file!`

The file ingestion boundary hashes the exact bytes and requires exactly one EDN
form. The store takes an exclusive sibling-file lock, validates the complete
ledger, writes and fsyncs a same-directory temporary file, atomically replaces
the ledger, and fsyncs the parent directory. Duplicate ingestion is detected
by the pinned record and performs no rewrite. Active state is always derived
from the sealed append history; no HEAD/current-status authority exists.

## Classification law

Terminal failure defaults to injury. The complete explicit non-injury kind map
is:

```clojure
{:abstained :ordinary-selection-abstention
 :operator-cancelled :operator-cancelled
 :declared-no-work :declared-no-work
 :diagnostic-only :diagnostic-only}
```

A record with no failure is also explicitly non-injury only when it declares a
successful terminal outcome, or is an absent terminal observation. Unknown
stages and unknown failure kinds remain injuries. `traceWritten` and
`:run-output` are recorded diagnostics with no classification authority.

A first cause opens a stable injury. A repeated attributable failure after
`:enacted` reopens through the Packet-1 law and increments the attempt. A
repeat while already active is appended as an inert repeat audit and cannot
downgrade state. Different cause keys create distinct active injury IDs.

## Five-click campaign replay census

The discoverable self-heal repair chain is the five chronologically related
records from first `A-self-heal` admission through the post-dispatch
abstention. The fixture records their production-shaped projections and exact
original byte pins:

| Run suffix | Classification | Reason / kind | Transition |
|---|---|---|---|
| `8227be68…` | non-injury | ordinary selection abstention | observation |
| `bd1f106d…` | injury | fold-output-invalid / construction | open |
| `b9b2b9bb…` | non-injury | ordinary selection abstention | observation |
| `be38a3a9…` | injury | carry-identity-unavailable / author-dispatch | open |
| `e86d38df…` | non-injury | ordinary selection abstention | observation |

Census: **5 inputs; 2 injuries; 3 non-injuries; 2 open; 0 reopened; 3
ordinary-selection-abstention reasons; 2 distinct active injuries**. The ticket
names `bd1f106d…`, `be38a3a9…`, and `e86d38df…`; all are present. The first and
third abstentions in the chain are also retained. Other 2026-10-19 run files
precede or interleave unrelated selection activity and are not silently called
members of this five-click repair chain.

Controls cover trace-written, run-output-present, absent observation,
unrelated success, unknown stage/kind, author/review/reload failures, duplicate
ingestion, classification tampering, same-cause reopen, distinct causes,
multiple EDN forms, ledger tampering, and byte-stable duplicate replay.

## Remaining work

Packet 3 still owes the dedicated coordinator and sealed work order. Later
packets owe Agency session dispatch, review/enactment/retry transport, serving
integration, and removal/quarantine of ordinary-route bypasses. This packet
does not claim end-to-end self-heal.

No click and no full suite were run.
