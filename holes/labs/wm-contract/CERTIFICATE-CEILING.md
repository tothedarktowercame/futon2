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
evidence is checked against the serving JVM over HTTP — a channel the
presenter does not own — and `certified` invokes the certifier itself. C395's
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

**3. Adapter pins establish occurrence, not correspondence** (C391 `a6e1c24`).
A receipt whose `:expected` is a four-row table pinned by one source line has
not been shown to match the Lean definition. 31/31 holds under the stated
meaning and is not a claim of identity-preserving adapters.

## The shape of the honest claim

The certificate can say: *this run was produced by the serving JVM, over
bounded jobs sharing one fence attempt, against a tested commit equal to the
loaded runner identity, with a declared topology boundary of stated
completeness.*

It cannot say: *this is the whole program*, *this is the only history*, or
*this ledger is not a copy*.

**Both halves belong in the artifact.** Class 10 (C414 `1a075c0`) is the reason:
evidence anchored by its own producer can verify perfectly while the principal
that writes it controls its canonical head. A certificate stating only the first
half would be an instance of the class it was built to avoid.

## What this means for authorising the run

The residual risk is **not** that a forged certificate slips past — the producer
bindings above close the paths that were demonstrated. It is that a *true*
certificate is read as proving more than it does. The mitigation is that the
limits are machine-readable (C405 `771885b`, C417 pending) rather than prose, so
a consumer cannot silently inherit the stronger reading.
