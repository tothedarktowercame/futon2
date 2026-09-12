# TN: Row 17 discovery — Agency work-lifecycle credit

Date: 2026-09-12  
Scope: `WORK-REMAINING.md` row 17 discovery only. No production or canonical
accounting change is made here.

## 1. Authorities and observed deployment

The assurance-band names are `R10 Scheduled entrypoint`, `R20 Interoceptive
tripwires`, `TRACE WM trace store (route ledger)`, and `R9 No
self-certification` (`p4ng/empirics-futon/control-stages.edn:30-35`). The
equation registry classifies these as plumbing rather than equation hosts
(`holes/labs/wm-contract/aif-equations.edn:520-521`).

Source pins read for this census:

- futon3c `3682c693a7e379dc8d86cf90716d34da5c478544`
- p4ng `d7df9406e371459ac4ffe2f92c38899b6009c7c7`
- futon2 pre-note HEAD `7ae631ad12e25259334920185b01bc3675d0f9ff`

The live default invoke ledger was observed at 2026-09-12 20:12 UTC as
`/tmp/futon3c-invoke-jobs.edn`, SHA-256
`0992e0f277ad86da29fca8cb0c88e582a7c968380a84fddb43339e3fac10f9c8`,
with 8,340 jobs. Two immediately preceding completed records,
`invoke-1789243606256-20511-7ec8a521` and
`invoke-1789243788806-20512-c7406299`, retain caller `claude-15`, target,
surface, accepted/started/finished times, execution counts, terminal outcome,
and delivered destination/time. This is a mutable `/tmp` authority: its hash
is a discovery-time pin, not yet retained evidence suitable for admission.

The live park store was simultaneously observed at
`/tmp/futon3c-parked-on.edn`, SHA-256
`dda2a7e5ff966b6361aca18d6cd11eda1359a946fae23429326eea72f9423118`,
with 20 outstanding records, 19 dependency-index entries, 48 ready-inbox
queues, and no leases. It has the same mutable-record limitation.

## 2. Machinery census

### Invoke/job lifecycle and receipts

Bell, whistle, and streaming whistle share the invoke/job engine
(`futon3c/README-bells-and-whistles.md:3-21`). A bell returns an accepted job
identity and status URL (`:80-86`); streaming exposes accepted, running,
heartbeat and terminal events (`:108-130`); delivery is written back to the
job ledger (`:133-138`).

The durable ledger is explicitly the authority and its in-memory active index
is only a derived projection (`futon3c/src/futon3c/transport/http.clj:255-266`).
It declares active and terminal vocabularies (`:268-308`), defaults to
`/tmp/futon3c-invoke-jobs.edn` (`:355-366`), and serializes updates under one
writer lock (`:619-632`). Accepted records retain job, agent, caller, surface,
request digest, reply relation, mode, timestamps, model, delivery state and
execution counters (`:1312-1366`). Running is timestamped (`:1559-1568`). The
first terminal transition wins and records terminal code/message, result,
summary, artifact reference, execution evidence and trace identity
(`:1571-1626`). Delivery success or failure is separately recorded with
surface, destination, time and note (`:1682-1737`).

Restart recovery converts only activated/running orphaned work into the typed
`worker-lost-on-restart` failure while queued work remains activatable
(`transport/http.clj:570-590`). Task-mode success is also refused when the
result has no execution evidence (`:4375-4399`). Detailed terminal transcripts
remain for 24 hours, then compact to identity/outcome/evidence and first/last
events; unreferenced tombstones expire after seven days, while active and
park-referenced jobs are protected (`:370-453`). Thus this is durable lifecycle
evidence with a stated retention window, not an append-only historical archive.

### Parking and deadlines

The park engine is disk-backed at `FUTON3C_PARKED_ON_PATH` or
`/tmp/futon3c-parked-on.edn`; its state contains continuation records, reverse
dependency index, coalescing keys, ready inboxes and leases
(`futon3c/src/futon3c/agency/parked_on.clj:35-52,82-94`). `park!` records a
continuation and reconciles already-terminal dependencies (`:332-399`),
`note-completion!` atomically folds arrivals and releases a completed join once
(`:305-329`), `rehydrate!` reconciles recovered jobs and requeues stale leases
(`:401-440`), and `sweep-deadlines!` supplies the deadline backstop
(`:443-465`). HTTP integration is flag-gated and defaults off; the implemented
path includes finalize notification, headless/buffer resume and inspection
endpoints (`futon3c/README-park.md:97-111`). Polling is the working Emacs path;
the WS fast path is presently latent (`:127-132`).

The park protocol has a material referential gap: the API accepts an invented
dependency id without checking the job registry, leaving it to wake at its
deadline (`README-park.md:167-179`). Exact agent/session validation is also a
documented caller obligation (`:136-151`), not evidence here of a server-side
refusal.

### Bell/whistle coordination and durable dispatch records

Busy-agent bells wait in a durable FIFO turn queue; holds apply between turns,
survive restart, and do not cancel in-flight work
(`README-bells-and-whistles.md:140-168`). The coordination ledger records an
invoke edge and its result (including failures) without changing invocation
semantics (`futon3c/src/futon3c/social/coordination_ledger.clj:49-82,138-164`).
Its evidence body retains from/to, surface, kind, time, edge identity, outcome
and error. It does not retain a shell command; the invoke job retains a request
digest and execution event counts, while terminal text/artifact references are
subject to the lifecycle retention policy above.

There is one already explicit node binding. `run-scheduled-dispatch!` names
R10, requires a commission id and dispatch function, requires the returned
receipt to echo R10 and the commission and name a dispatch id, then records the
joined commission and receipt. Invalid, unlinked, or unrecordable dispatches
refuse (`coordination_ledger.clj:84-136`).

## 3. Candidate credits

| Node | What it declares | What Agency demonstrably supplies | Credit/evidence shape | Existing record usable today |
|---|---|---|---|---|
| R10 | Scheduled entrypoint | A mechanically enforced R10 commission-to-dispatch-receipt join, with typed refusal if identity/linkage/recording is absent. | A working-evidence row plus a `by-record` claim over the complete stored commission and dispatch receipt; retain the three refusal controls. | Coordination evidence tagged `[:coordination :scheduled-dispatch :R10]` queried by commission id; unlike generic jobs, this record already names R10. |
| R20 | Interoceptive tripwires: the catalogue asks for phase-boundary wires and `freeze, record, park, summon`, calibrated against incidents (`VERIFY-r-nodes.edn:1430-1451`). | Generic work-lifecycle wires: missing execution refuses success; lost workers become recorded failures; deadlines wake continuations; terminalization is first-writer-only; delivery failure remains visible. Parking supplies record/park/resume, but not the complete catalogue action or calibration. | Separate narrow working-evidence rows for **work execution tripwires** and **continuation/deadline handling**, each with induced failure records and explicit feature-flag/deployment pins. Do not claim the catalogue-wide R20 node. | Invoke jobs with `worker-lost-on-restart`, `invoke-no-execution-evidence`, timeout/cancel and delivery failure; parked records plus their joined terminal jobs. The live files above show the record populations but must be copied into bounded retained fixtures before admission. |
| R9 | No self-certification | Caller, target, request digest, execution, result and delivery provenance that a distinct-reviewer checker can consume. Agency itself does not compare producer/author with reviewer and does not refuse self-certification. | Input evidence only. No R9 credit until a checker consumes the identities and mechanically refuses equality. | Completed invoke records can pin producer/caller/target and later review jobs, but a pair is not proof of enforced separation. Existing narrower enforcement remains `full_loop_runner.clj:2598-2611`, as recorded in `VERIFY-r-nodes.edn:1888-1908`. |
| TRACE | WM trace store (route ledger) | An Agency job ledger and a coordination evidence ledger, each for dispatch/lifecycle facts. | No direct credit. A future cross-ledger claim would need an explicit identity-preserving join from an Agency job/trace id to a WM run/route record. | `:trace-id` and `:trace->job` are possible join inputs, but neither makes the Agency ledger the WM TRACE store. |

R12 (two-layer calibration) and R17 (structure learning/BMR) receive no
candidate credit: this machinery neither calibrates two layers nor learns a
model structure. R16 receives no new credit merely because work eventually
finishes; Agency delivery is not grounded machine actuation.

## 4. Boundary of the credit

The coding-handoff convention asks for different author and reviewer agents,
but the surveyed Agency boundary does not enforce that inequality. Generic
coordination rows normalize a missing caller to `unknown`; they establish that
an edge was recorded, not that its author was independent. Therefore row 17
may credit provenance to the evidence supply for R9, but must not mark R9
implemented. The node census already records this exact limitation: two
narrower checks exist, zero R9 route records carry their verdict, and there is
no universal admission layer (`VERIFY-r-nodes.edn:1895-1908`). Row 19 owns the
missing mechanical self-certification refusal.

Likewise, Agency's timeout, restart and execution-evidence checks are genuine
tripwires, but they do not establish R20's full weave, retro-trip calibration,
blind-spot map, or universally enabled `freeze, record, park, summon` action.
The existing R20 verification reports zero R20-linked Agency surfaces
(`VERIFY-r-nodes.edn:1443-1455`). Crediting must name the narrower lifecycle
properties and add the node/evidence link; it cannot rewrite that zero into a
full R20 admission. Row 18/19 must build missing assurance rather than borrow a
generic lifecycle event as its proof.

Finally, the live `/tmp` hashes demonstrate present records, not stable pins.
Admission evidence must retain bounded selected records before ledger
compaction or mutation, record their source ledger hash and extraction time,
and avoid copying unrelated prompts/results.

## 5. Proposed Row 17 packet split

1. **R10 scheduled-entrypoint credit.** Extract one real R10 commission,
   dispatch receipt and evidence row; verify the commission/dispatch/node join;
   commission the invalid-commission, unlinked-receipt and recording-failure
   controls already enforced at the boundary. Add only the resulting
   working-evidence/by-record credit to R10.
2. **R20 execution-tripwire credit.** Retain minimal redacted job rows for
   execution-evidence refusal, worker loss/recovery, timeout or cancellation,
   and delivery failure, plus a successful control. Check terminal chronology,
   first-terminal-wins and retained execution/outcome fields. Credit precisely
   “Agency work execution tripwires,” not all interoceptive tripwires.
3. **R20 continuation credit.** Retain a real park record, its dependency job,
   release/deadline outcome, deployment flag, and delivery path. Include an
   invented-dependency negative control which currently demonstrates the known
   gap. Credit record/park/resume only where the configured path actually ran.
4. **R9/TRACE non-credit record.** Retain the discovered provenance fields and
   join candidates as prerequisites for rows 19 and a future cross-ledger
   witness. Assert that author=reviewer is not refused and that an Agency
   `trace-id` alone is not a WM TRACE record. This packet prevents later
   accounting from silently upgrading available evidence into enforced
   assurance.

Each packet cites `WORK-REMAINING.md` row 17. The first three can be reviewed
independently; the fourth records why no R9 or TRACE status changes with them.
