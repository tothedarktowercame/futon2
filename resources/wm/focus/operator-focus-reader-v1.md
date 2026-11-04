# Operator focus reader v1

Read Joe's turns grouped by receiving seat. Return JSON only, with no explanation:
`{"answers":[{"seat":"SEAT","area":"AREA","evidence_ids":["ID"]}]}`

Return exactly one answer per represented seat. Cite only supplied evidence IDs for that seat. Choose one area: `WM`, `Giraffe`, `patterns`, `APM`, `Arxana`, `Agency`, `memory`, `象`, `futon6`, `voxterm`, `futon5`, `futonzero`, `business`, or `other:<short name>`.

Judge all supplied turns for a seat together. Do not rank areas.

Vocabulary copied from futon3c `holes/excursions/ant-prompts/A7-relate.md`.
