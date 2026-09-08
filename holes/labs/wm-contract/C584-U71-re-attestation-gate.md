# C584 — U71 positive-receipt re-attestation gate

U71 adds a declaration-grain comparison for every standard positive witness
receipt and both Lean proof receipts.  The checker extracts the live declaration
text and compares its sha256 with the receipt pin
(`positive_receipt_reattestation_check.bb:20-63`).  A mismatch fails unless its
receipt, declaration, pinned hash, and current live hash all equal one U72
backlog entry (`positive_receipt_reattestation_check.bb:65-80`).  Consequently a
second edit to an already stale declaration is a new mismatch, not an exemption.

The live census is 33 receipt files, 33 known-stale declaration pins, and zero
unexpected mismatches (`runs/U71-re-attestation-gate.edn:5`).  The debt is
reported as `KNOWN-STALE` on every invocation rather than silently accepted;
U72 removes those exact entries as it re-emits receipts.

The gate runs from the worklist check every seat already invokes
(`worklist_check.bb:231-241`).  Its commissioned control copies a fresh receipt
and its sources, observes a pass, changes the pinned theorem text without
changing the receipt, and requires `UNATTESTED-DRIFT` plus the re-emission
diagnostic (`positive_receipt_reattestation_controls.sh:6-20`).  The publish
negative-control path invokes that control at
`p4ng/empirics-futon/negative_controls.sh:18-21`.  The exact control result and
paths are recorded at `runs/U71-re-attestation-gate.edn:9-16`.
