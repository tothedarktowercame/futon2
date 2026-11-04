#!/usr/bin/env python3
"""Emit the Phase 1 design-note record set for T-car3-queue-routing-design-note.

Deterministic: verifies each quoted fragment against the pinned ticket bytes
(sha256 e539d69d...) and writes the same EDN bytes for the same input. No
timestamps, no environment dependence; running twice leaves no diff.
"""
import hashlib, sys
from pathlib import Path

TICKET = Path("/home/joe/code/futon3c/holes/tickets/T-car3-queue-routing-design-note.md")
PIN = "e539d69d02ab28760e704dbfa825e015ba49a813d933e1f086471c5e4808282c"
OUT = Path("/home/joe/code/futon2/data/wm-d-task-enactment-artifacts/T-car3-queue-routing-design-note/phase-1-records.edn")

STEPS = [
    (["agent/scope-before-action", 0], ":declare-phase-1-design-scope", ":phase-1-scope-record",
     "# T-car3-queue-routing — Phase 1 design note", ":heading-names-design-note"),
    (["agent/provisional-claims-ledger", 1], ":record-dated-partial-status", ":partial-status-ledger-entry",
     "**Status (triaged 2026-09-01): PARTIAL -- mechanism landed, capture-gap fallback still tolerated.**",
     ":status-dated-partial"),
    (["contracts/holder-states-the-claim", 2], ":state-exact-closure-route-claim", ":closure-route-claim",
     "Close after either proving all accept sites supply stable :from/:surface/:msg-id or accepting the fallback as the contract.",
     ":closure-route-exactly-stated"),
    (["agent/evidence-over-assertion", 2], ":anchor-accept-site-claim-to-source", ":anchored-accept-site-claim",
     "turn_queue.clj:213,241,523", ":accept-sites-cited-by-line"),
    (["agent/handoff-preserves-context", 3], ":emit-ratification-handoff-status", ":ratification-handoff-record",
     "Status: model-only proposal for ratification.", ":status-reads-model-only"),
    (["agent/scope-before-action", 4], ":fence-implementation-out-of-phase", ":phase-scope-fence",
     "No implementation is included in", ":implementation-excluded-this-phase"),
]

def main():
    ticket = TICKET.read_text()
    digest = hashlib.sha256(ticket.encode()).hexdigest()
    if digest != PIN:
        sys.exit(f"ticket sha256 {digest} != pin {PIN}; refusing to emit records")
    lines = [
        "{:schema :wm/design-note-record-set-v1",
        " :ticket-id \"T-car3-queue-routing-design-note\"",
        f" :ticket-source {{:path \"{TICKET}\" :sha256 \"{digest}\"}}",
        " :cascade {:selected-digest \"ee265eb24a6847d330826a8fec9f47aa10e770eb6ddb68e29bf23bf95878f9cb\"",
        "           :compiled-digest \"38af15fe15b6ae3243aaf8bb6b3ab72902f2a1850dd207c9a7ed0853a891c583\"",
        "           :phase :compile}",
        " :scope \"Phase 1 textual records only: the design note's scope declaration, dated",
        "  partial-status ledger entry, closure-route claim, source anchors, ratification",
        "  handoff status and phase fence, mechanically verified against the pinned ticket",
        "  bytes. Queue-routing implementation is explicitly out of scope of Phase 1.\"",
        " :records ["
    ]
    for hole, op, produces, quote, check in STEPS:
        seen = ticket.count(quote)
        if seen != 1:
            sys.exit(f"quote {quote[:40]!r} occurs {seen}x in pinned ticket; refusing")
        import json
        lines.append(f'  {{:hole ["{hole[0]}" {hole[1]}]')
        lines.append(f"   :operation {op}")
        lines.append("   :capability :wm/pattern-apply")
        lines.append(f"   :produces {produces}")
        lines.append(f"   :completion-check {check}")
        lines.append(f"   :locator \"data/wm-d-task-enactment-artifacts/T-car3-queue-routing-design-note/phase-1-records.edn#record={check[1:]}\"")
        lines.append(f"   :quote {json.dumps(quote, ensure_ascii=False)}")
        lines.append("   :verified-against-ticket-sha256 true}")
    lines.append("]}")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n")
    print(OUT)

if __name__ == "__main__":
    main()
