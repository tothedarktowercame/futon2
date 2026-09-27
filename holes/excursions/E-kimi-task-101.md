# E-kimi-task-101 — WIRE-2L-D: second-layer census of the 175 wires and the ledger :second-layer field (discovery)

**Requisition:** completed — 2026-09-27T03:43:48Z, job invoke-1790480333591-25278-e848d7b6, state failed

Clocked in by claude-8 for kimi-2 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# WIRE-2L-D: the second layer — which of the 175 wires already have a value-varying test, and how the ledger records `:second-layer` (⟨2⟩3b of PROOF-2a-PLAN; discovery, no edits)

WIRE-2L-D (claude-8 → kimi-2), discovery under the coding-handoff protocol: you find out and propose, claude-8 reads; bell claude-8 back with the report below. NO commit, no edit to any tracked file, no test run; work read-only in `/home/joe/code/futon3c` and `/home/joe/code/futon2` at HEAD (other lanes' dirty files: leave them). ⟨2⟩3 is Completed at ledger 9040e7fc (175 wires, all witnessed at the first layer). ⟨2⟩3b (PROOF-2a-PLAN.md:257) is the next register: "Every wire has a second-layer test: changing the value the writer supplies changes the reader's calculation, or produces the specified refusal. ACCEPT: every matrix entry has a registered test that varies the writer's value and asserts the reader's output or refusal changes accordingly; the ledger gains a `:second-layer` status per wire." Exemplar defect it exists for: measured A reaching a consumer that returns early. This packet defines the second layer operationally and censuses what exists.

Read first: `test/futon3c/diagramprover/wm_wire_ledger_test.clj` (how the first layer is defined and how the ledger is generated: each wire test namespace exports a `wire` map `{:wire :kind :test :check :live-records-read :note}`, `received?` in `wm_wire.clj:22-26`, the ledger written from the map + adjacency at pinned revs, `wire-test-nses`); three wire test namespaces of different shapes — `wm_wire_construction_assemble_one_r4_kernel_cascade_spec_test.clj` (its `bad-carriers-before-the-real-reader` and the `:different` mutation), `wm_wire_ask_merge_published_construction_construct_interpretations_test.clj` (its `different-carrier-changes-the-reader-product`), and one of the rates-lane tests (`wm_wire_rates_support.clj` users) — and the ledger `holes/labs/M-wm-wiring/wm-wire-ledger.edn`.

Establish:
1. **The operational definition.** For a wire [writer reader field], a second-layer witness is a test that (a) drives the real reader with the writer's value V and with a different value V′ (or the specified typed absence) and (b) asserts the reader's RECORDED product differs (or the specified refusal appears) — not merely that the value arrived (that is the first layer). Say what "the reader's calculation" is for the three shapes you read (a ranked list, a spec receipt, a rate). Distinguish: (i) value-varying with a product assertion; (ii) refusal-at-the-door only (the bad case exists but no product-change assertion); (iii) first layer only.
2. **The census.** For each of the 175 wires (from the ledger's `:test` var, open its namespace): classify (i)/(ii)/(iii) by reading the deftests, naming the deftest that witnesses (i) or (ii). Counts per class and per lane; the full list for (iii) and (ii) (wire → namespace → what is missing). Do not run anything.
3. **The ledger field.** Propose how `:second-layer` enters the ledger without a second generation path: e.g. the wire map exports `:second-layer {:test `deftest-var :kind :value-varying|:refusal-only}` and the ledger test records it beside `:test`, defaulting to `{:absent :no-second-layer-test}` (typed, never a status); the coverage join unchanged. Say what changes in `wm_wire_ledger_test.clj` (~lines) and whether the map/adjacency pins move (they should not).
4. **The packets.** Group the (ii)+(iii) wires into implementation packets by lane and support namespace, each under ~250 lines, in an order; estimate the total against the plan's 2,000–6,000 for ⟨2⟩3b.

Report (bell claude-8): the definition in one paragraph; the census table (counts) and the two lists; the ledger-field proposal with lines; the packet list with estimates; anything not yours, unfixed; what you read and what you did not.
