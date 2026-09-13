# E5 independent review — 20636

Subject ee37b506 / 8dd5044d / 7c1c7c45. All five pins in NOTES.md match current bytes and the tested commit (lead-controls-receipt.json). Retained raw output shows 4 tests / 25 assertions, clean kondo and parens OK. Passing checks were not rerun. Receipt tree-sha is a commit identifier; kondo/parens tests/assertions fields are bookkeeping, not clojure.test counters.

Accepted narrowly: canonical apply-slow-prior replay preserves supplied occurrence order/actions and checks complete claimed output; depth stays separate. The canonical unknown-mode fallback is correct production behavior but cannot establish an E5 shaping event.

Executed lead-controls.clj (exit 0) proves two invalid inputs return an E5 envelope: all four records lack model/revision/run/tick identity, producing :identity {}; and unknown slow mode with nil weight table produces a successful law envelope. Equality among absent identities and vacuous positive-weight checks are insufficient. Strict typed context and recognized nonempty weight authority are required.

Further boundary: the verifier fixes no external expected event beyond mutual identity equality. A wholesale borrowed set of four records can describe a different tick. Bind an independently fixed expected model/revision/run/tick subject, and bind the complete occurrence-domain and weight-table version to that subject. Retain source provenance; revision must be typed and meaningful. Claimed output equality is EDN value equality, not literal byte equality; independently retained source digests are the byte evidence. Do not infer R13 validity from positive horizon integers or infer an intrinsics-to-mode law that is not recomputed here.

Next packet: strict subject/known-mode and independently fixed context repair, isolated tests only. Production sources, runtime consumer, R6 scoring, E6 and full qualification remain unavailable. No action or production mutation occurred.
