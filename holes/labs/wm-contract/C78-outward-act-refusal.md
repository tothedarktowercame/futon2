# C78 — outward-act boundary refusal

Recorded 2026-08-31 by `wm-verbs`. No actuator was added and `P-R16` was not
changed.

## Decision

No currently authored binding supplies all three required ports: an armed
external effect, an independently operated read-back query derived from that
binding, and an observation that feeds the verified result into the next
belief. Implementing C78 now would invent at least one of those ports. The
delivery is therefore refused rather than representing another model-local
receipt as an outward act.

## Evidence

- `src/futon2/aif/enact.clj:130-147` shells the fold engine and returns a
  `:constructed-wiring` map. This is the already-classified in-model
  construction.
- `src/futon2/aif/enact.clj:243-285` turns that result into the typed union
  `:constructed-wiring` / `:no-construction` / `:enactment-failed`; none is an
  external-effect witness.
- `src/futon2/aif/trace.clj:317-330` persists the machine's own outcome and
  enactment audit. That is useful evidence of what the machine reported, but
  the writer and claimant are the same boundary.
- `src/futon2/aif/observation.clj:18-31,41-75` declares the fourteen current
  observation channels and their source-status records. There is no external
  act-witness channel.
- `src/futon2/aif/belief.clj:970-1015` maps only the current eight likelihood
  channels into the belief-update driver. No independent act-witness verdict
  enters the next belief.

Agency job records are genuinely outside the generative model and independently
queryable, as C75 demonstrates. But R16 has no authored binding naming a
specific Agency operation as its act. Adding one would build a dispatcher and
an authority-bearing external mutation, contrary to C75's boundary and without
an armed target. A subprocess-created marker file is smaller code but fails the
independence criterion: R16 would choose the path, write the bytes, and supply
the expected digest. A second file-reading function would be a separate reader,
not an independent witness.

## What would make the unit buildable

An owner-approved authored binding must name: (1) one bounded external
operation and its authority/arming rule; (2) an independently owned query that
derives its target from that binding and returns a typed effect/absence verdict;
and (3) the observation-channel semantics by which that verdict is committed
to the next belief, including the agreed `:independent` weighting. At that
point the existing enactment result union can be extended without replacing
its construction/no-construction/failure variants.

No test gate is claimed for an actuator that was not built. The falsifier for
this refusal is concrete: locate an existing authored binding and current
consumer satisfying all three ports above. This census found none.
