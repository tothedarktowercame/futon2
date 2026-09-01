# C409 — append-only and external-authority census

Date: 2026-09-01. Discovery only; no scheme or implementation change.

## Answer

There is **no permanent, cryptographically sealed append-only authority** in
the assessed build paths on this machine.

There is one narrower facility: the root-owned systemd journal is an
**externally administered, retention-bounded append log** relative to the
unprivileged `joe` processes that write the War Machine artifacts. Those
processes can submit new journal messages but cannot edit the accepted journal
files or their parent directory. It is not presently used to anchor the quiet
run ledger, certificate, Agency registry, or bounded receipts.

That distinction matters:

- if “external authority” means only that the artifact writer cannot rewrite
  an accepted entry while it is retained, journald qualifies;
- if it means permanent, sealed, independently verifiable history, **nothing
  assessed qualifies**.

## Candidate assessment

| candidate | durable? | can the artifact writer rewrite/delete the authority? | disposition |
|---|---|---|---|
| Agency invoke-job registry | yes across process restart | **yes** — `/tmp/futon3c-invoke-jobs.edn` is mode `0600`, owned by `joe`; the serving process and operator account replace it | durable mutable state, not an authority |
| Agency in-memory registry and roster snapshots | partly | **yes** — in-memory atoms and `joe`-owned EDN/SQLite state are controlled by the same account/process family | neither append-only nor external |
| bounded-job registry and receipt store | yes while `/tmp` survives | **yes** — `/tmp/futon-bounded-tests`, `jobs.json`, and individual receipts are `joe`-owned and owner-writable; `bg.py:74–79` atomically replaces the registry | durable receipts, not an authority |
| Git commits and object hashes | yes while objects remain | object contents are tamper-evident for a known hash, but **yes** for the canonical-history claim — the same account controls refs, object retention, and reflogs | content addressing without an external head |
| Git reflog | normally retained for a configured period | **yes** — `.git/logs/HEAD` and its parent are `joe`-writable; reflogs can be expired, deleted, or replaced | local recovery log, not append-only authority |
| Git author/committer timestamps | stored in commit objects | **yes at creation**, and history selection remains caller-controlled; timestamps are supplied data, not a machine clock attestation | provenance, not temporal authority |
| systemd unit state / job metadata | service-manager durable only for the unit lifecycle | the ordinary writer cannot rewrite a past manager observation, but transient unit state is not an append-only history and can disappear/reset | external observation, not an append ledger |
| systemd journal | persistent, with configured rotation/retention | **no for `joe` after ingestion** — journal files and directories are `root:systemd-journal`, not writable by uid 1000 | externally anchored while retained; not permanent/sealed |

## Evidence for the journald boundary

Observed on this machine:

```text
uid=1000(joe), groups=joe,users
/var/log/journal                         root:systemd-journal drwxr-sr-x
/var/log/journal/<machine>/system.journal root:systemd-journal -rw-r-----
joe write access to directory: no
joe write access to journal file: no
journal storage in use: 2.5G
one retained boot: 2026-08-14 through the current observation
```

Journal headers carry boot IDs, file IDs, sequential-number ranges, realtime
and monotonic timestamps assigned by journald. This provides an external
ordering/ingestion observation relative to a `joe` process.

It is not a sealed authority here. The inspected archived user-journal headers
reported `Tag objects: 0`, and no forward-secure-sealing key file was found
under `/var/log/journal`. The files are subject to root administration,
rotation, vacuuming, retention limits, disk loss, and machine compromise. A
writer can also submit false *message content*; only journald's trusted fields
and the fact/order of ingestion are outside that writer's direct control.

## Consequence for the current findings

C395's quiet-run chain and C404's serving/test receipt binding have no external
head today. Their hashes and durable receipts can be internally consistent
after rollback, copying, or caller selection because the same authority that
chooses the artifact controls the mutable stores that name it.

Journald's existence does not retroactively close either finding: no current
producer records the relevant ledger head or run/program binding into the
journal, and no consumer resolves one from it. This census states only the
facility and its actual trust boundary; it deliberately does not propose a
protocol.

## Durable is not append-only

Atomic rename, fsync, locks, schema validation, HMACs, and hash chains improve
durability or internal integrity. None prevents the same filesystem authority
from replacing a complete artifact with another complete artifact. That is the
specific distinction this census preserves.

## Delivery inventory

```sh
bb -cp . -e "(require '[checks.wm-workspace-gate :as g]) (prn (g/inventory-result))"
```

Result: `{:name :check-inventory, :exit 0, :unknown (), :missing ()}`.
