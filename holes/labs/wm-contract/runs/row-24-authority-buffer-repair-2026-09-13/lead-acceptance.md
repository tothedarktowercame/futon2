# Authority reader repair review — 20650

Subject23614af7/c5f1ab84. Both source/test pins match final subject and retained receipt. Read raw5tests20assertions, clean kondo and explicit-path parens; missing-databind attempt retained. Passing gates not rerun. Added separate lead-date-control.clj (exit0): mutating a returned Date does not alter captured text or a subsequent reparse. This closes the untested mutable-value regression.

Accepted as a cooperative isolated byte acquisition helper. JSON scanner requires one root and rejects nested duplicate keys; EDN sentinel is unforgeable; pointer traversal advances by index including nil/false keys. Each pointer read rechecks retained text against the recorded hash and reparses, so parsed mutable values do not alias capture storage. Canonical value encoding matches the existing F11 recursive encoding; raw-source digest remains separate.

Trust limits: the capture is still a public map. Callers can replace both text and digest or format, so downstream must retain the independently configured expected digest/format and must not treat arbitrary supplied capture maps as authenticated authority. The value encoding inherits F11 conventions (sets/sequences become vectors), not an injective type-preserving universal EDN encoding. Schema-specific validation is required. No production authority, universe completeness, payload chronology, or emitter integration is established.

Codex24 reader packet is reviewed; next integration remains F11-owned. Retain this helper and exact pins for a later reviewed consumer packet. Do not duplicate work or wire the emitter as routine bookkeeping. All full-certificate/production acquisition obligations remain open.
