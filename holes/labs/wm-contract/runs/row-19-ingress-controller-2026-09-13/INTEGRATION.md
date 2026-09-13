# Prospective invoke ingress/drain integration

Status: isolated construction only. Futon3c commit 1f742f6f is not wired,
loaded, installed, or evidence that the current server is protected.

## Lock and lifecycle rule

The controller has one private lock and generation. Every creator calls
begin-creation! before attempting invoke-jobs-writer-lock; its opaque ticket
contributes to waiting-writer. Inside or after the ledger transaction it must
call creation-finished! exactly once in finally, supplying a job id only if
durable acceptance succeeded. Lock ordering is controller briefly, release,
ledger writer, controller briefly: code must never acquire the ledger lock
while holding the controller lock. This avoids inverse lock ordering and
counts entrants already blocked on the writer.

Execution integration moves each accepted id through distinct sets:
accepted-queued to executing to final-delivery to absent. drained? requires
the waiting count and all three sets to be empty. Closing rejects new entrants
but does not cancel anything in those sets.

All seven lexical callers in transport/http.clj must use the wrapper: auto
bellback 1164, parked resume 1293, direct invoke 4532, bell 5222, announce
5350, whistle stream 5697, whistle async 5858. The latter two make the eighth
surface distinction. Job worker start/completion and each surface's final
delivery callback must advance the same id; errors also need a terminal
delivery completion path.

Parked completion and deadline sweeps (http.clj:1320-1365) must call
defer-resume! while closed instead of creating. The required file adapter
atomically replaces a schema-and-digest envelope and forces both file and
parent directory before the transition becomes visible. Recovery parses the
same byte buffer it hashes and restores only the serializable ordered deferred
projection; opaque entrant tickets never reach disk. Reopen yields stable
resume-id/payload pairs. The
runner uses the resume id as requested job id, so retry is idempotent, and
calls acknowledge-resume! only after durable acceptance. A crash before ACK
replays the same identity; after ACK it cannot replay. Conflict on identical id
with changed payload refuses.

Waiting, accepted, executing, and delivery counters deliberately do not recover
from this store. Startup must reconcile those lifecycle identities against the
hot invoke ledger, durable turn queues, and delivery records before opening.
This packet does not fabricate that reconciliation and must not report a fresh
empty controller as proof that pre-restart accepted work drained.

## Verification lane

verification-snapshot requires both a loopback address and an operator token
owned by the controller. Integration requires a separate loopback-only listener
with its own reviewed authentication; it must not be routed through Agency's
public handler or accept request-supplied controller state. Its bounded API is
status, close, reopen, and one explicit retention/archive probe after terminal
joins. Only status exists in this packet; actuator endpoints and serving
integration are later reviewed packets. Every status says restart-authorized
false, including an open empty controller.

## Initial transition gap

The old JVM has none of these checks. The controller cannot protect its own
first installation, and selective hot-loading remains prohibited. A later
controlled restart must therefore use an external ingress fence for the first
transition. After a fresh prospective source/classpath snapshot is reviewed,
post-start evidence must measure that all callers share the installed
controller. Historical loaded-byte identity remains unknown and is not
reconstructed. No restart readiness follows from the isolated tests.

## Next bounded packet

Wire only the common creator wrapper and lifecycle callbacks in http.clj, with
injected temporary ledger/deferred stores in tests. Commission/archive logic
and D13 remain unchanged. A separate packet wires parked persistence and the
local verification listener. This split prevents a broad handler rewrite from
being hidden in the controller boundary.
