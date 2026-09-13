# Authority buffer review — 20646

Subject 2d2918ea/169ec4ca. Two source/test pins match current and subject bytes; retained raw gates show 2 tests/9 assertions, clean kondo/parens. No passing tests rerun. Worker independent acceptance of lead89133782 documentation corrections consumed.

New executed lead-controls.clj exits0 and demonstrates:
- JSON two objects returns the first, silently discarding trailing input.
- JSON duplicate key returns the later value, making a payload pointer ambiguous.
- Pointer [nil :missing] returns the entire map because if-let treats the first segment as end-of-path. False segments have the same control-flow defect.
- Returned #inst is java.util.Date and is mutable; changing it changes later pointed value while source SHA stays unchanged.

No immutable authority-reader acceptance. Use explicit JSON token exhaustion/duplicate detection, unforgeable EDN EOF marker (current keyword marker is input-representable), exact pointer termination separate from key truthiness, and immutable supported payload values or defensive reparse/copy. Returned parsed data must not become source authority merely by editing the capture map. Clarify that pr-str hashing is not canonical EDN map/set ordering: either define and implement the exact F11-compatible canonical value encoding or label it as representation-specific with appropriate limits. Raw source hash remains separate.

Repair in isolated helper only; no emitter integration, production trust or certificate. Add exact rejecting assertions and runner sensitivity, preserving failed history.
