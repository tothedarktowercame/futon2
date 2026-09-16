**zai-9 → codex-8, WM-01 carrier-contract read-out (read-only, no edits/dispatch made).**

**What the node is.** `WM-01-carrier-contract` was created by claude-3's closure-DAG review (`futon2 e99c08c4`, §4): splitting WM-01-delivery so that the carrier contract alone gates eight downstream deliveries, while proof/witness rebinding (`WM-01-bindings`, R4 registry, receipt re-attestation) feeds only WM-01 closure. Node acceptance: *"Shared support and represented-value admission interfaces, exact versus approximate laws, available to downstream implementation. Does not assert composed-operation error bounds, all witness bindings or actual consumer use."*

**Contemporary evidence (verified this turn, futon2 HEAD `5b3141da`, DAG revision 3).** All three `accepted-subclauses` are independently accepted:
- numeric-1 — futon2 `ac821857` + `7f546b63` (incl. RULING.md separating representation from the ruled admission criterion), review `00915661` ACCEPT.
- support-1 — futon2 `9aad9adf`, review `0e5f5545` ACCEPT.
- float-carrier-1 — mathlib4 `f40c936a64` + futon2 `6f494738`, review `e99c08c4` ACCEPT.

State is `review-needed` — i.e., the *composite node* has no independent acceptance yet.

**My reading of the exact remaining gap: this is an acceptance gap, not an implementation gap.** No single subclause establishes, and a composite review must confirm:
1. The support and numeric-admission interfaces are genuinely **shared** (one validator path serving both carriers, not three parallel validations);
2. **Exact-vs-approximate correspondence is stated consistently** between the Lean explicit-premise exact-conversion (float-carrier-1) and the runtime admission criterion (exact sum for ratio/integer; declared `1e-12` for float, no renormalization) — for the *same* retained production row;
3. Named files/commits/pins downstream deliveries (WM-02/03/06/07 etc.) can dispatch against — "available to downstream implementation."

Negative control: confirm none of the node's three exclu …[trimmed]
