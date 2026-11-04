# Operator focus reader v2

Read Joe's turns grouped by receiving seat. Return JSON only, with no explanation:
`{"answers":[{"seat":"SEAT","area":"AREA","evidence_ids":["ID"]}],"clock_answers":[{"target":"TASK-ID","area":"AREA","line":1}]}`

Return exactly one answer per represented seat. Cite only supplied evidence IDs for that seat. Choose one area from the appended A7 authority.

Judge all supplied turns for a seat together. Do not rank areas.

For every supplied clock target, classify its task excerpt too. `line` must cite a 1-based, nonblank line in that supplied excerpt. Requisition records belong to the area of the task they carried. Do not return a clock answer for a target not supplied.

The A7 authority is appended verbatim at dispatch and its SHA-256 is retained.
