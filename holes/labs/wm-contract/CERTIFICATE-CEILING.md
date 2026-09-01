# What the operational certificate can and cannot establish

Date: 2026-09-01. Assembled by `claude-20` (owner) from the C379–C419 campaign.
**This is a ceiling, not a status report**: it states what the certificate could
establish if every open repair landed, so the run can be authorised against a
known claim rather than an assumed one.

Joe's standing ask was *an operational certificate that proves the correct
topology was used when the machine runs*. Three independent findings bound how
much of that is achievable on this machine.

## Established, and how

**Producer binding for the tested phase** (C383 `8d4bd99`, C402 `d704401`,
C410 `1d0cc90`). Quiescence and fence evidence are produced by direct
invocation of their checks. Bounded job records resolve through the Futon3c
registry, must carry the fence id as attempt identity, and their gate start
freshness is measured at state-machine ingestion rather than trusted from
receipt JSON. The available systemd monotonic start is not claimed because the
fence supplies no comparable boot-relative observation. Click
evidence is checked against live serving-JVM status over HTTP rather than a
caller-authored receipt alone, and `certified` invokes the certifier itself. C395's
handwritten no-run chain is now a passing regression test that refuses at
`click-issued`.

**Serving program identity** (C398 `dd08e09`, C410). The certificate carries
`serving-runner-code`, derives the tested commit from a durable bounded-job
receipt, requires the loaded runner identity to equal it, and fails as
`:serving-program-differs-from-tested-program`. It retains `:tested-job-id`,
`:tested-attempt` and `:tested-commit`, so the derivation can be re-checked
rather than only its verdict surviving.

## Not establishable here, with the reason

**1. The topology-bearing code is not a closed set** (C399 `85d7244`). Clojure's
runtime closure *"is not mechanically derivable from the namespace graph due to
injected functions, `requiring-resolve`, mutable Vars, and the HTTP boundary."*
One runner digest is insufficient, and no digest reaches an injected function or
a rebound Var. **The certificate can pin a declared boundary; it cannot pin the
program.** C399's additional requirement stands: the production run must show
the judgment seam was not replaced — a runtime observation, not a pin.

**2. History length is unprovable, and a copy is indistinguishable from its
original** (C395 `3c69b02`, C409 `a843c45`, C415 `ddfc37e`). Per-row and
predecessor hashes prove the integrity of the rows that remain. They do not
prove that rows were not discarded. Closing that needs an authority the
evidence writer cannot rewrite, and **no such authority exists here**: Agency
ledgers, bounded receipts, git reflogs, refs, timestamps and local state are all
mutable by the account that writes the evidence. The one asymmetry — the
root-owned journal, which `joe` can append to but cannot unlink (C416) — was
assessed and rejected: it *"cannot distinguish an unchanged copy from its
original"*, expires under retention, and yields to root. **Best-effort anchoring
would be misleading**, so none is wired.

**3. The observed program is bound to terminal observation, not to the run**
(C404 `5c7a125`, still open). The click observer records `:serving-runner-code`
from *terminal* service status; the run record does not carry program identity,
and the certificate does not bind the observation to the instant the run began.
So *"a reload or substituted click receipt between execution and terminal
observation is not distinguishable from a single stable serving program by this
artifact alone."* C410 bound the tested *receipt* to the fence attempt; this is
the **time** binding of the one identity that is observed, and it is a different
question. Unlike (1) and (2) this looks repairable — the run record could carry
the identity at start — and it is not yet dispatched.

**4. Attempt membership across producers is unprovable** (C423 `987bd1a`, C429
`4a58e90`, C432 `33aac56`). `agent-id` reaches the bounded receipt from
`bg.py --agent`: producer-persisted and caller-authored at once. C410's check
**detects a mismatch but does not prove shared attempt identity**. C429 surveyed
the alternatives and found none: the bounded receipt carries no run identity,
the fence id is caller-selected, click and run ids join to each other rather
than to the job, and *"adding the current attempt label to the run record would
only create another copy of a caller-authored assertion."* Systemd's unit
identity and monotonic start **are** genuine and unforgeable by the caller, and
neither is carried into the run — the nearest available material, recorded so a
future attempt starts there. Clearing condition: an independently issued,
verify-at-use capability bound by both producers.

**5. Adapter pins establish occurrence, not correspondence** (C391 `a6e1c24`).
A receipt whose `:expected` is a four-row table pinned by one source line has
not been shown to match the Lean definition. 31/31 holds under the stated
meaning and is not a claim of identity-preserving adapters.

## The shape of the honest claim

The certificate can say: *this run was produced by the serving JVM, over
bounded jobs sharing one fence attempt, against a tested commit equal to the
loaded runner identity, with a declared topology boundary of stated
completeness.*

It cannot say: *this is the whole program*, *this is the only history*,
*this ledger is not a copy*, *this program ran every stage*, or
*this tested job belongs to this attempt*.

**Both halves belong in the artifact.** Class 10 (C414 `1a075c0`) is the reason:
evidence anchored by its own producer can verify perfectly while the principal
that writes it controls its canonical head. A certificate stating only the first
half would be an instance of the class it was built to avoid.

## What this means for authorising the run

The demonstrated shape-only certificate paths are closed by the producer
bindings above. The remaining risk is that a valid certificate or valid ledger
prefix is selected as canonical by the same principal that writes it, or that
the certificate is read as proving more than it does. The limits are now
machine-readable using C417 (`b185982`) authority vocabulary, rather than prose
alone, so a consumer cannot silently inherit the stronger reading.
