# E-kimi-task-55 — PROOF-2a-PLAN ⟨2⟩3: first-layer wire tests for the read-step block of the matrix (5 wires), calibration lane

**Requisition:** in-progress — dispatched 2026-09-26T12:44:10Z to kimi-1 as invoke-1790426650678-24830-22e4fc80

Clocked in by claude-8 for kimi-1 on 2026-09-26 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Wire tests for one block of the matrix: lane 3, the read step (⟨2⟩3 of PROOF-2a-PLAN)

You are writing first-layer wire tests for the wires listed below, in futon3c. Read first, in this order: `futon2/holes/labs/wm-contract/PROOF-2a-PLAN.md` step ⟨2⟩3 (what this is for); `futon3c/test/futon3c/diagramprover/wm_wire_ledger_test.clj` (its docstring is THE definition of a verified wire: the reader's value under the field is present, is not a typed absence `{:absent ..}` or `{:status :absent ..}`, and is the value the writer wrote; VERIFIED when a live record carries both ends, WITNESSED-HERMETICALLY when a test drives writer through reader in a hermetic run, UNVERIFIED otherwise); `futon3c/test/futon3c/diagramprover/wm_wire.clj` (the helpers: `received?`, `sha256-file`, `read-record`); and the two existing wire tests `wm_wire_r7_fold_selection_test.clj` and `wm_wire_r9_candidate_enact_test.clj` as the pattern: each namespace defines a `wire` map with the `[writer reader field]`, its kind, its test var, a `:check` fn returning `{:writer .. :reader ..}`, and the live records it read with sha256, and registers itself by being added to `wire-test-nses` in the ledger test.

The wires (from `futon3c/holes/labs/M-wm-wiring/wm-adjacency.edn`, map 5927020d; the boxes' sites are in `futon3c/holes/labs/M-wm-wiring/wm-flight-wiring.edn` under `:boxes`, each with `:site` file and var, `:writes`, `:reads`):
- [:r2-served-by-reading :r2-flight-read :text-sha256]
- [:r2-served-by-reading :r2-test :text-sha256]
- [:r2-served-by-reading :r2-test :want-span]
- [:r2-served-by-reading :r2-verifier :text-sha256]
- [:r2-served-by-reading :r2-verifier :want-span]

For each wire: one namespace `futon3c.diagramprover.wm-wire-<writer>-<reader>-<field>-test` (shorten sensibly, keep it unique), one `wire` map, one deftest that observes the writer's value and the reader's value and asserts `received?`, and one bad case per wire (a typed absence at the reader fails; a different value fails). Live records to search for both ends: `futon3c/holes/labs/M-wm-wiring/spike/` (flight and run records, click records) and `futon2/holes/labs/M-futon-seams/exemplar/`; if a record carries both ends, pin it verbatim (path + sha256 asserted before reading) and the wire is VERIFIED; if none does, say which records you read and why each lacks an end, and witness the wire hermetically by calling the writer's var and the reader's var from the sites. Never invent a value: a constant that is not from a live record or from a real call is not a pin.

Then add each namespace's symbol to `wire-test-nses` in `wm_wire_ledger_test.clj`, run the ledger test and your tests once in futon3c's JVM (`clojure -M:test -n <ns> ...` from `/home/joe/code/futon3c`; the ledger writes `wm-wire-ledger.edn`, commit it), and register warrants for each namespace and the ledger: `AUTHOR=<your-id> /home/joe/code/futon2/scripts/wm/register-warrant.sh --pinned <sha> <ns>` run from `/home/joe/code/futon3c`. Gates: clj-kondo 0/0 on your files, `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` OK, commit by explicit path (`git add <paths>`; never `git add -A`, never `--amend`, never `git stash`), nothing under `data/` except the warrant ledger, no load into the :6768 JVM, no flight, no click, no edit to the map `wm-flight-wiring.edn` (if a wire cannot be tested because the map is wrong, say so; do not fix the map). Report: for each wire its status and the record or call that witnessed it; the ledger's counts before and after; the shas and warrant ids.
