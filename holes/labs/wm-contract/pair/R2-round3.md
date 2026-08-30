# R2 round 3 — confirmation of the joint proposal

I confirm `holes/labs/wm-contract/pair/R16-R2-delivery.edn` as an honest proposal for an edge that carries no traffic today. Confirmation does not resolve its recorded payload disagreement.

## Confirmed

- The direction and correlation core are right: R16 supplies source tick, mission, and a typed witness; R2 returns a receipt tied to the later observation tick (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:1-12`, `holes/labs/wm-contract/pair/R16-R2-delivery.edn:29-38`).
- The receipt's meaning is bounded correctly: incorporation/refusal, not proof of an external world change (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:29-38`).
- Guarantee, atomicity, retry, and timeout remain unspecified for stated missing operations rather than receiving invented defaults (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:13-24`).
- The idempotency key is only proposed and conditional on identities plus a durable write existing (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:25-28`).
- The four blockers cover both ends: typed R16 result, durable delivery, independent verification, and R2 input/channel/commit (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:54-63`).

## Disagreement retained

I retain the payload disagreement at `holes/labs/wm-contract/pair/R16-R2-delivery.edn:49-53`. R16 presently produces an internal construction, not outward action (`src/futon2/aif/enact.clj:12-16`, `src/futon2/aif/enact.clj:205-232`). R2 cannot label that as independent observation of an external effect.

The joint payload is therefore a target schema, not a description of current output. In particular, `:construction :artifact` at `holes/labs/wm-contract/pair/R16-R2-delivery.edn:8-11` is conditional: today R16 has no independently readable location/digest in its audit (`src/futon2/aif/enact.clj:218-232`).

## One schema refinement required before implementation

The witness should be encoded as an actual tagged union so success requires `:construction` while non-production requires `:reason`; the proposal currently displays both fields under one map (`holes/labs/wm-contract/pair/R16-R2-delivery.edn:6-12`). This is not a disagreement about semantics, but a validation requirement that prevents contradictory witnesses.

## What I learned

R2 is not merely a numeric sink: it needs a structured, durable pre-vector fact before any honest numeric projection. R16 is not yet an outward actuator: its nearest honest witness is construction production. The shared edge cannot become real until a later armed actuator or an explicit decision that construction—not external action—is the phenomenon R2 should observe.
