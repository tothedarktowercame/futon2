# U55-stepper — the stepper's controls (worklist `:U55`)

Account: `../../C510-stepper.md`. Tools: `../../wm_step.sh`,
`../../wm_step_records.bb`, `../../wm_step_evidence.bb`. The step work directory
itself lives under untracked `data/wm-step/w1` (a 300 MB trace corpus per pin);
`steps.edn` is its ledger and these files are the controls a reviewer reads.

The one accepted step's run store is a sibling: `../2026-09-04-010-accepted`.

## controls/

| file | what it shows |
|---|---|
| `determinism-011-vs-012.edn` | two steps from one pin, **identical** (`98bdc350…`), exclusion list stated in the file |
| `world-drift-011-vs-012.edn` | 0 of 17 hashed inputs moved; commit census 3762 unchanged; mana age both sides of nothing |
| `determinism-008-vs-009.edn` | the same on the generation-0 pin (`d8c444bf…`), with the mana snapshot's content moving between them |
| `world-drift-008-vs-009.edn` | that mana movement, named |
| `cassette-faithful-001-vs-002.edn` | a step that FILLED the cassette from the live store and a step that replayed it FROZEN agree — the replay is faithful, not merely repeatable |
| `cassette-filling-001.edn` | 7 entries filled, 3 hits |
| `cassette-frozen-011.edn`, `-012.edn` | frozen: 10 hits, **0 fills, 0 misses, 0 non-GET** — the evidence input is completely pinned, and the tick POSTed nothing, measured at the server |
| `plant-mu-uniform.edn` | the planted input: 417 entities' `:mu-post` made uniform, before/after shas |
| `planted-input-002-vs-005.edn` | the planted step **diverges** and the difference is localised to the controller and selection scores |
| `world-moved-002-vs-007.edn` + `world-drift-002-vs-007.edn` | the divergence that was NOT the machine: four `:observation` percentage channels, tracked to a `git log --since` census that lost one commit |

The first detection of the planted input is not a file: `wm_step.sh step` exits
4 and runs no tick at all, naming the changed file. `--allow-pin-drift` is what
produced `005-planted`.

## defect-loop/

`PLANTED-DEFECT.diff` is the exact edit to `src/futon2/aif/efe.clj:909`
(quantise `:controller-score` to 0.5). `006-defect/01-decisions.edn` is RE7's
verdict against the step taken with it — `:defect`, the chosen candidate at rank
1 inside a 136-wide tie — and `007-fixed/01-decisions.edn` is RE7 against the
step taken after reverting it: `:green`, tie 1. `03-controls.edn` on each side
is RE7's own nine controls, four negative, all passing in both directions.
