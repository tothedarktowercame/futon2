# L5-9 — The selection record carries its execution identity and its refusal-history carrier (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` (≥ this packet). Alert: click 5 (`wm-click-1d0f3570`), selection run
**Q5 unverifiable** ("selection interpretation order needs [:runner-execution/identity :id]"),
**Q6 and Q11 unverifiable** (refusalHistory, refusalHistorySource, refusalHistoryUnsealed,
refusalHistorySealedBeforeR18: "no carrier on the selection path"; Lean's `forbidden` is derived
from refusalHistory, so Q11 cannot be read either). Evidence: `clojure -M -m wm-run-facts --run
data/wm-runs/selection-record-2026-11-01-wm-click-1d0f3570-….edn --kind selection`.

## Cause

The tick record gets `:runner-execution/identity` (`full_loop_runtime.clj` ~1610) and its
selection certificate gets `[:q6 :refusal-history …]` from the inner path; the selection record
written by `retain-selection!` (~1306) gets neither. The exporter's `selection-path`/Q5 reader
needs `[:runner-execution/identity :id]` to join the phase log (`wm_run_facts.clj` ~874), and the
Q6 reader needs the `:q6` carrier (~759–782), which for the selection kind is "no carrier".

## Behaviour

1. The runtime writes on the selection record, at decision time: `:runner-execution/identity`
   (same value as the tick's) and `[:outer/selection :q6]` with `:refusal-history` (vector of
   `{:cascade :input-digest :run-id :target}` as `previous-run/refusal-history` returns,
   as-of the click's start), `:refusal-history-unsealed`, `:refusal-history-sealed-before-r18`.
   Refusals before scoring (no digest) still write identity and the carrier it can compute.
2. The selection export reads them: `interpretationOrder` from the phase log joined by that id;
   `refusalHistory*` from `[:outer/selection :q6]` with the same semantics as the work reader
   (retained / predecessor-only / unsealed count). Records without the carrier export as today.
3. Nothing in selection changes; no new default.

## Tests

- A selection record written through `run-opportunity!` on a fixture (as
  `not_exercised_selection_test` does) carries both; the export reads Q5 order and refusal history
  from them; with a planted refusal on the same digest, `forbidden` contains that cascade.
- A pre-change record exports exactly as before.
- Named, outer, projector namespaces vs `main`; hermetic root; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex5/l5-9`. Bell claude-2 with sha,
counts, and the export of click 5's record on the branch (read-only) showing Q5/Q6/Q11.
