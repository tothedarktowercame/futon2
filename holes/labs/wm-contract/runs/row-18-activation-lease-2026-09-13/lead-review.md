# Activation lease source review — codex-26

Reviewed bc1b4856/fdd99673/8d09b84a. All six source/test/readback pins match; retained16tests49assertions, kondo0 and actual parensOK inspected without rerun. Real current-status output refuses activation-lease-unavailable.

Accepted narrowly: own-file group/world write checks, stable identity/size/time around same-buffer strict receipt parsing, embedded census-byte digest/equality, lease enclosing resolution and capture, and revalidation before/after logical capture are now implemented. The isolated real reader exercises permission corruption and mutation. One injected generation-change check exists. This is conditional reader/protocol acceptance, not deployed writer coherence.

Remaining activation gap: controller artifacts are only declared paths and syntactically valid digests. No controller content is read or matched to independently configured expected source, nor is mechanically enforced direct in-JVM reload present. The captured process vector is authenticated by the root host receipt under its trust premise; digest equality alone is not census completeness evidence. The next packet must supply concrete controller/source resolution and a scoped launch/reload policy, or retain an exact unavailable-controller refusal. Do not invent :lease-enforced status from metadata.

The requested interval controls are not yet comprehensively exercised: add isolated actual protected-capture controls for expiry, changed inode/generation, and competing reload/launch while the lease is held, preserving logical refusal and no production writes. Production access must open preprovisioned lock files without creating missing ones. Current generic lock helper includes CREATE; it cannot be used as evidence that the production reader is strictly read-only once /run/futon2 exists but a lock is absent.

No activation, filesystem provisioning, or node admission was performed. Historical open->resolved repair remains a separate logical failure even after deployment coordination closes.
