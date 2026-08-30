# WM-RUN2 — the route the tick actually took, judged against the wiring (organisation evidence)

Owner: claude-15. Builder: codex-5 (holds war_machine.clj; DISPATCHED ONLY AFTER WM-RUN1 GATES — same file, same
entry point). Mode: work.

## The declarations you are witnessing (fixed; do not touch Lean)
mathlib4 `eec8279cf7` (amended after the WM-RUN1 gate): `RouteHop` {fromNode, toNode, via, at_} and `wmRunConformsToWiring` — "a completed tick's
reassembled route is non-empty and every hop is an edge of the wiring specification". Falsifier: empty route, or a
hop absent from `p4ng/empirics-futon/control-map-edges.edn` (Figure 4 as data; the conformance reference).
Joe's design (from his words; the original note was not located — do not hunt for it): a tracer tag on the flowing
Clojure map — as the tick routes through the wiring, each node boundary conj's an entry; afterwards the entries
assemble and join into the route, "which may or may not be a simple cycle".

## Amendments from the WM-RUN1 gate (now binding)
- The entry point exists: `scripts/futon2/run_tick_once.clj` (`clojure -M -m futon2.run-tick-once 14`); instrument it.
- The receipt's basis fields are DISAMBIGUATED per the amended Lean `TickRunRecord`: `storeBasisCount`/`storeBasisMaxAt`
  = the STORE's count/max-at at tick time (the I_data_current pin — from the count route or the fetch result's total;
  say which), and `entriesRead`/`entriesLimit` = the sample the tick consumed under its cap. `evidence-basis` in
  run_tick_once.clj currently reports the sample as if it were the pin — fix it as part of adding `:route`, and
  update `wm_runs_once_witness.clj`'s field checks to the new names (its registry row re-binds with your run).

## Goal (one behaviour: the tick narrates its own route, and the narration is checked)
1. A `:wm/route` vector on the tick's flowing state map. At each node boundary that exists in code, conj
   `{:node :Rn :via "<fn-name>" :at <inst>}` — tag at least: R2 (observation assembly), R3 (belief update loop
   entry/exit — the micro-step loop is INSIDE R3, one tag not per-step), R8 (compute-variational-free-energy),
   R7 (precision), R5 (compute-efe per the batch, one tag), R6/R14 (policy/select-action with the seam), R12/R20
   where the code visibly passes through them, and the trace write (the consumer edge). Tag where the code IS, not
   where the diagram wishes it were — a node with no code site gets no tag, and that absence is a finding.
2. `assemble-route`: consecutive tags → hops `[{:from :to :via :at}]`. The receipt
   (`tick-run-record-<date>.edn` from WM-RUN1) gains `:route` (the hops) and `:route-verdict`.
3. `futon2/checks/wm_route_conformance.clj` (bb): reads the receipt + `control-map-edges.edn`; verdict line:
   `route: hops=N conformant=K unmapped=M | drawn-edges-fired=F/21`; a hop whose {from,to} is not a drawn edge is
   `unmapped` — NOT auto-failed: report it (the diagram may be wrong, per the R16 findings; an unmapped hop is
   evidence about the WIRING). Exit 1 only on empty route. `--negative` control: a synthetic receipt with an empty
   route → exit 1.
4. Registry row binds `wmRunConformsToWiring` ONLY IF hops are non-empty AND every hop is drawn; if unmapped hops
   exist, do NOT bind — record the unmapped list in the findings as proposed wiring amendments instead (they go to
   the CML lane). Honest non-binding is an acceptable outcome.

## Acceptance (row-11 first; bare runs)
- One tick via the WM-RUN1 entry produces a receipt with `:route`; show it verbatim. The conformance check runs
  bare with the verdict line; negative control exits 1.
- No behavioural change: the tag conj is the ONLY addition to the flow (diff shows tags + assembly only); the six
  efe/preferences suites stay green (state the command).
- kondo + parens; commit only your paths; refusals honest.

## Report
Bell claude-15 back with: sha(s), the route verbatim, the verdict line, drawn-edges-fired count, unmapped hops
(if any) as proposed amendments, whether you bound the registry row and why, diffstat.
