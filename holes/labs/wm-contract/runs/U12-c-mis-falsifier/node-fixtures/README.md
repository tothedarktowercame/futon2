# U12 node fixtures

One fixture per (run-id, node), harvested from `data/wm-trace/` tick records by
`holes/labs/wm-contract/u12_c_mis_falsifier.clj`. Never planted: every value is a
record field, and a node the record cannot answer is a typed absence with a reason
rather than a missing file.

| fixture | node | record field | producer (`:via`) | basis | value |
|---|---|---|---|---|---|
| `node-fixtures/0a18c4f7-R2.edn` | R2 | `[:observation]` | `futon2.aif.observation/observe` | record :route R12->R2 | map, 14 keys |
| `node-fixtures/0a18c4f7-R7.edn` | R7 | `[:precision-state]` | `futon2.aif.precision/update-precision-state` | record :route R2->R7 | map, 8 keys |
| `node-fixtures/0a18c4f7-R3.edn` | R3 | `[:mu-post]` | `futon2.report.war-machine/apply-arena-belief-events` | record :route R7->R3 | map, 417 keys |
| `node-fixtures/0a18c4f7-R1.edn` | R1 | `[:mu-pre]` | `belief prior carried into the tick` | record field; not a route hop | map, 417 keys |
| `node-fixtures/0a18c4f7-R8.edn` | R8 | `[:prediction-errors]` | `futon2.aif.free-energy/compute-prediction-error` | record :route R3->R8 | map, 7 keys |
| `node-fixtures/0a18c4f7-R5.edn` | R5 | `[:ranked-actions]` | `futon2.aif.efe/rank-actions` | record :route R8->R5 | seq, 146 entries |
| `node-fixtures/0a18c4f7-R6.edn` | R6 | `[:decision]` | `futon2.aif.policy/select-action` | record :route R5->R6 | map, 25 keys |
| `node-fixtures/0a18c4f7-R14.edn` | R14 | `[:decision :tau]` | `futon2.report.war-machine/invoke-strategic-selection` | record :route R6->R14 | 1.0 |
| `node-fixtures/0a18c4f7-R9.edn` | R9 | `[:preference-stack]` | `futon2.aif.preferences/preferences` | record field; C_int, the half this row does NOT measure | map, 4 keys |
| `node-fixtures/0a18c4f7-S4.edn` | S4 | `[:active-mission]` | `futon2.report.war-machine/carry-active-mission` | war_machine.clj:1524-1526 | map, 2 keys |
| `node-fixtures/0a18c4f7-R12.edn` | R12 | -- | `futon2.report.war-machine/scan-r12-apparatus` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/0a18c4f7-R20.edn` | R20 | -- | `the apparatus scan's input` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/0a18c4f7-R16.edn` | R16 | -- | `enactment` | S7: R16 enactment data does not exist until a flight flies | absent :no-enactment-yet |
| `node-fixtures/4abad68c-R2.edn` | R2 | `[:observation]` | `futon2.aif.observation/observe` | record :route R12->R2 | map, 14 keys |
| `node-fixtures/4abad68c-R7.edn` | R7 | `[:precision-state]` | `futon2.aif.precision/update-precision-state` | record :route R2->R7 | map, 8 keys |
| `node-fixtures/4abad68c-R3.edn` | R3 | `[:mu-post]` | `futon2.report.war-machine/apply-arena-belief-events` | record :route R7->R3 | map, 417 keys |
| `node-fixtures/4abad68c-R1.edn` | R1 | `[:mu-pre]` | `belief prior carried into the tick` | record field; not a route hop | map, 417 keys |
| `node-fixtures/4abad68c-R8.edn` | R8 | `[:prediction-errors]` | `futon2.aif.free-energy/compute-prediction-error` | record :route R3->R8 | map, 7 keys |
| `node-fixtures/4abad68c-R5.edn` | R5 | `[:ranked-actions]` | `futon2.aif.efe/rank-actions` | record :route R8->R5 | seq, 146 entries |
| `node-fixtures/4abad68c-R6.edn` | R6 | `[:decision]` | `futon2.aif.policy/select-action` | record :route R5->R6 | map, 25 keys |
| `node-fixtures/4abad68c-R14.edn` | R14 | `[:decision :tau]` | `futon2.report.war-machine/invoke-strategic-selection` | record :route R6->R14 | 1.0 |
| `node-fixtures/4abad68c-R9.edn` | R9 | `[:preference-stack]` | `futon2.aif.preferences/preferences` | record field; C_int, the half this row does NOT measure | map, 4 keys |
| `node-fixtures/4abad68c-S4.edn` | S4 | `[:active-mission]` | `futon2.report.war-machine/carry-active-mission` | war_machine.clj:1524-1526 | map, 4 keys |
| `node-fixtures/4abad68c-R12.edn` | R12 | -- | `futon2.report.war-machine/scan-r12-apparatus` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/4abad68c-R20.edn` | R20 | -- | `the apparatus scan's input` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/4abad68c-R16.edn` | R16 | -- | `enactment` | S7: R16 enactment data does not exist until a flight flies | absent :no-enactment-yet |
| `node-fixtures/801976e7-R2.edn` | R2 | `[:observation]` | `futon2.aif.observation/observe` | record :route R12->R2 | map, 14 keys |
| `node-fixtures/801976e7-R7.edn` | R7 | `[:precision-state]` | `futon2.aif.precision/update-precision-state` | record :route R2->R7 | map, 8 keys |
| `node-fixtures/801976e7-R3.edn` | R3 | `[:mu-post]` | `futon2.report.war-machine/apply-arena-belief-events` | record :route R7->R3 | map, 417 keys |
| `node-fixtures/801976e7-R1.edn` | R1 | `[:mu-pre]` | `belief prior carried into the tick` | record field; not a route hop | map, 417 keys |
| `node-fixtures/801976e7-R8.edn` | R8 | `[:prediction-errors]` | `futon2.aif.free-energy/compute-prediction-error` | record :route R3->R8 | map, 7 keys |
| `node-fixtures/801976e7-R5.edn` | R5 | `[:ranked-actions]` | `futon2.aif.efe/rank-actions` | record :route R8->R5 | seq, 146 entries |
| `node-fixtures/801976e7-R6.edn` | R6 | `[:decision]` | `futon2.aif.policy/select-action` | record :route R5->R6 | map, 25 keys |
| `node-fixtures/801976e7-R14.edn` | R14 | `[:decision :tau]` | `futon2.report.war-machine/invoke-strategic-selection` | record :route R6->R14 | 1.0 |
| `node-fixtures/801976e7-R9.edn` | R9 | `[:preference-stack]` | `futon2.aif.preferences/preferences` | record field; C_int, the half this row does NOT measure | map, 4 keys |
| `node-fixtures/801976e7-S4.edn` | S4 | `[:active-mission]` | `futon2.report.war-machine/carry-active-mission` | war_machine.clj:1524-1526 | map, 4 keys |
| `node-fixtures/801976e7-R12.edn` | R12 | -- | `futon2.report.war-machine/scan-r12-apparatus` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/801976e7-R20.edn` | R20 | -- | `the apparatus scan's input` | record :route R20->R12 | absent :no-record-field |
| `node-fixtures/801976e7-R16.edn` | R16 | -- | `enactment` | S7: R16 enactment data does not exist until a flight flies | absent :no-enactment-yet |
