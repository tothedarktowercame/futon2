# Packet: the R20 receipt gains the interoception row, as a typed none (2026-10-22)

From claude-2. Recording/schema change only; selection does not change.

## Why
The Lean certificate now has ten rows, not nine. mathlib4 `darktower`
`89f424b274` added `Node.interoception` (`DarkTower/WarMachine/R20Certificate.lean`,
`InteroceptionRow` = `size`, `tick : R20Interoception.InteroceptiveTickRecord (Fin size)`,
`observed : Fin size`; cases `inRange` / `outOfRange`). Current Lean head is
`de33cabb48`. The Clojure receipt (`src/futon2/aif/wm/apparatus_certificates.clj`)
still emits nine rows and pins Lean `8fda677deb`. No runtime code records a
machine-internal reading as an observation modality, so the honest row today
is a typed none. Because absence is a hole, a missing row must keep
`:certified?` false, exactly as in the Lean.

## Goal (one behaviour)
- `node-order` gains `:interoception` after `:cost` (Lean order).
- `receipt` emits an interoception row: `(absent :interoception [:size :tick :observed]
  :no-interoceptive-modality-recorded)` (use the existing `absent` helper).
- `lean-authority` :mathlib4 becomes `"de33cabb48"`.

## Acceptance (in `test/futon2/aif/apparatus_certificates_test.clj`)
1. The receipt has ten rows whose `:node`s equal `node-order`, in order.
2. The interoception row is `:not-recorded` with the three missing fields.
3. Bad case: take a receipt and force every other row to `:agrees`; it is still
   not `:certified?` because of the interoception row. Then remove the row from
   `:rows` entirely and show that whatever validates receipts (if one exists;
   grep for `r20-certificate-v1` readers, e.g. tick-record validation or the
   futon3c witness) does not accept a nine-row v1 receipt as complete. If no
   such validator exists, say so in your bell rather than inventing one.
4. Old fixtures (`apparatus_certificates_test.execution-receipt.edn` and any
   others) are updated, not the rule.

## Do not
- add a reading, a modality, or any G term — that is later work;
- touch selection, policy.clj, meta_live_outer_selector.clj (codex-4 works there).

## Gates
clj-kondo on changed files; `emacs --batch -l ~/code/futon4/dev/check-parens.el` on
changed Clojure; `clojure -M:test` for the apparatus-certificates namespace and any
namespace whose fixtures you touch (set `FUTON3C_INVOKE_JOBS_FILE` to a temp path).
Rebase onto current master before committing. No live clicks; no JVM hot-load.

Bell claude-2 back with a summary + commit shas.
