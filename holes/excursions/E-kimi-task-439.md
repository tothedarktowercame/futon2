# E-kimi-task-439 — R1ArguedRecords port to r1-belief (Pearl, argued records) + readback + bb load

**Requisition:** in-progress — dispatched 2026-11-25T16:40:24Z to kimi-1 as invoke-1795624825071-29113-5fa0cc1c

Clocked in by claude-14 for kimi-1 on 2026-11-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Port R1ArguedRecords to futon2.aif.r1-belief (argued records, Pearl's rule) + readback

Assigned by Joe via claude-14 (M-cascade-live-causal; record log design agreed with claude-5 2026-11-25): Cascade Live and the WM run ONE R1 implementation, futon2.aif.r1-belief (your b389f2fe2), loaded from babashka by Cascade Live. It lacks the argued-record update.

Specification: mathlib4 worktree /home/joe/worktrees/mathlib4/c14-cascade-outcome (branch c14/cascade-outcome-type), DarkTower/WarMachine/R1ArguedRecords.lean (b2d012575a): readingChannel k0 r (reader claims rung k0 with reliability r: likelihood r at k0, 1-r elsewhere, for "says" true; complement for false), coin_reader_moves_nothing (r = 1/2), perfect_reader_pins (r = 1), reading_raises_claimed (r > 1/2), witness_three_quarters_realised (uniform prior, r = 3/4 claiming :realised -> 3/7).

Build in futon2 (small commits):
1. src/futon2/aif/r1_belief.clj: (reading-channel k0 r) returning a likelihood usable by the existing update-belief; refuse r outside [0,1] and a missing r (no default). Keep exact ratios.
2. Tests named after the Lean theorems above (coin reader leaves the belief equal; perfect reader pins; r>1/2 raises the claimed rung when 0<p<1; witness 3/7 exactly). Planted: missing reliability refused; r=1.2 refused. Mutants you try, and which test catches each.
3. The code must load in babashka (bb -cp src -e "(require 'futon2.aif.r1-belief)") — check it does; if a JVM-only construct blocks it, say which and replace it with a portable one.
4. Readback in the F8 form: extend holes/labs/wm-contract/want_free_energy_readback.clj or add r1_belief_readback.clj, transcribing R1DeepTemporalB17.noTrace_realised (3/34) and R1ArguedRecords.witness_three_quarters_realised (3/7) by theorem name; exact ratio comparison; commit the output under holes/labs/wm-contract/runs/R1-belief/clojure-readback.txt; byte-for-byte on a second run.
Gates: clj-kondo; check-parens; run only the r1-belief test namespace (short timeout); no hot-load; no wiring. Bell claude-14 back with shas, results, mutants, and the bb load check.
