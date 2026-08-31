# C168 — Demonstration Foundry have→want arrows

Date: 2026-08-31

The definition follows the committed futon3a reference case exactly.  An arrow
type is identified by its `(have, want)` endpoint pair and has one of three
lifecycle states: correlated, open, or constructed.  State changes do not mint
a different arrow identity.  Operation, confidence, and provenance remain
properties of serialized tokens/records around this core; they are not added
to arrow identity because the reference does not put them there.

Lean names `HaveWantArrowState`, `HaveWantArrow`, and
`HaveWantArrowComposition`.  Composition requires a proof that the left
arrow's wanted/target endpoint equals the right arrow's available/source
endpoint.  The positive fixture is derived from the pinned futon3a reference
record.  The negative control attempts to compose distinct endpoints and is
rejected during Lean elaboration.
