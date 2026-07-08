# Bindings grounding: single-entry-point (`futon3c-d/mission/single-entry-point`)

**Author:** zai-10 (reviewed lane — claude-4 reviews + runs build-match).
**CLean source:** `futon6/holes/clean/single-entry-point.clean.edn` (5 boxes, audit-derive-then-discharge, holes at s3/s4, terminal at s5).
**Method:** spec-derived from box `:produces` meaning + `:text`. NOT reverse-engineered.

## Key finding: ALL boxes unbound — this mission has zero substrate-2 manifestation

Single-entry-point is an **operational/process-level mission**: it concerns JVM
lifecycle, process-group ownership, shutdown hooks, and cyder registration. None
of its `:produces` types name substrate-2 entities or hyperedges. The cyder
process registry (`futon3c.cyder`) is an in-JVM atom (`defonce !processes`),
not a substrate-2 store. The `dev-laptop-env` launcher is a shell script. The
process-group escape prevention is a UNIX-level invariant (setsid, Process.toHandle).

This is the third data point in the diverse candidate set, and it is the most
informative for build-match design: **a CLean can be entirely honest and entirely
unbound** — not because the mission is incomplete, but because its domain
(process management) doesn't touch substrate-2 at all.

## Per-box derivation

### s1 — `:produces :orphan-jvm-audit`

- **Box text:** "three long-running JVMs... None owned by the futon3c lifecycle"
- **Binding:** **unbound: abstract**
- **Why:** This is an audit FINDING — a report about observed process state
  (orphaned JVMs, their RSS, ports). It is an observation, not a substrate
  entity or hyperedge. No typed node is created by auditing.

### s2 — `:produces :single-launcher-claim`

- **Box text:** "a single ./scripts/dev-laptop-env invocation is the SOLE launcher of every long-running futon-stack dev process"
- **Binding:** **unbound: abstract**
- **Why:** This is a DESIGN CLAIM — a structural assertion about process
  ownership. The `:produces :single-launcher-claim` names an invariant ("one
  launcher owns all dev processes"), not a substrate entity. The claim is
  enforced by process-level mechanisms (port collision, shutdown hooks), not
  by substrate-2 writes.

### s3 — `:produces :integration-strategy`

- **Box text:** "DERIVE fork, UNDECIDED: (A) in-JVM embedding... (B) managed sub-processes"
- **Binding:** **unbound: abstract**
- **Why:** This is a GENUINELY OPEN design fork (`:sorry` hole, `:satiety :payoff`).
  The `:produces :integration-strategy` names a decision that hasn't been made.
  No substrate entity represents an undecided integration strategy.

### s4 — `:produces :escape-proof-process-group`

- **Box text:** "child processes must be captured in a process group (setsid / Process.toHandle) so they CANNOT survive the launcher"
- **Binding:** **unbound: abstract**
- **Why:** This is a PROCESS-LEVEL INVARIANT — a property of the UNIX process
  tree (no child reparents to systemd on launcher death). The `:produces
  :escape-proof-process-group` names a behavioral property enforced by OS-level
  mechanisms (setsid, process groups, signal handling), not a substrate entity.

### s5 — `:produces :owned-dev-stack` (TERMINAL, `:discharges`)

- **Box text:** "shutdown hook SIGTERMs every managed child; each subsystem registers with cyder so it shows in (dev/status); double-launch guard"
- **Binding:** **unbound: abstract**
- **Why:** The terminal discharges to completion criteria via JVM-level
  mechanisms: shutdown hooks, cyder registration (in-JVM atom), port-collision
  guards. None of these write to substrate-2. The cyder registry is
  `defonce !processes (atom {})` — RAM-only, dies on JVM teardown. The
  `:produces :owned-dev-stack` names a state of the process tree, not a
  substrate entity.

## Summary

| Box | `:produces` | Binding | Why |
|-----|-------------|---------|-----|
| s1 | :orphan-jvm-audit | unbound: abstract | audit finding (observation, not entity) |
| s2 | :single-launcher-claim | unbound: abstract | design claim (process invariant) |
| s3 | :integration-strategy | unbound: abstract | undecided design fork (:sorry hole) |
| s4 | :escape-proof-process-group | unbound: abstract | UNIX process invariant (setsid, not substrate) |
| s5 | :owned-dev-stack | unbound: abstract | terminal — JVM lifecycle + cyder (in-JVM atom) |

**0 bound, 5 unbound (all abstract).** No generic-type coarseness issue — there
are simply no boxes whose `:produces` has a substrate-2 manifestation.

## Expected drift prediction

**build-match will report 0/0** — zero bound boxes, zero inhabited, zero drift.
The `:discharged?` dial will be false (no bound boxes = no structure to verify),
but this is not a failure of the CLean or the mission. It is the honest result:
this mission's domain (process management) does not intersect substrate-2.

**Contrast with the other two candidates:**

| candidate | bound | result | what it revealed |
|---|---|---|---|
| autoclock-in | 3 (specific hyperedges) | 2/3, discriminating | real drift — agents aren't typed |
| state-snapshot-witness | 3 (generic :evidence) | 3/3, coarse | blind spot — generic types pass trivially |
| **single-entry-point** | **0** | **0/0, N/A** | **a mission can be entirely outside substrate-2** |

The third data point completes the picture: build-match's discrimination ranges
from **discriminating** (specific types catch real drift) through **coarse**
(generic types pass trivially) to **inapplicable** (the mission has no substrate
manifestation at all). This is the full design space for build-match v2.
