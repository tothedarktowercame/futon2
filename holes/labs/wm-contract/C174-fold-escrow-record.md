# C174 — reconstructible Fold escrow records

Date: 2026-08-31

`FoldEscrowRecord` binds the recorded prompt inputs, stored digest, authored
turn, arming payload, and resulting Fold output in one envelope. Its
`reconstructible` property states that digesting the prompt rebuilt solely from
the recorded inputs equals the stored digest.

The concrete checker applies that property through the production
reconstructor and SHA-256 implementation. All **10** C24 entries fail prompt
reconstruction with `:prompt-not-reconstructable`; none is accepted merely
because it appears in the list. The packet's “11 quarantined lines” was an
output-line count; the authoritative quarantine has ten entries.

The negative control adds an already-valid, reconstructible fold turn as a
purported quarantine member. Validation keeps it in active deposits and does
not admit it to quarantine. Thus list membership cannot turn a reconstructible
record into a quarantined one.

This is supporting formal vocabulary for the existing Fold glossary entry, not
a new glossary paragraph. The glossary bound count therefore remains 8/33.
