# E-kimi-task-436 — G' want free energy: aligned Clojure + F8-form readback certificate

**Requisition:** in-progress — dispatched 2026-11-25T05:34:27Z to kimi-1 as invoke-1795584867616-29022-45ae2c8c

Clocked in by claude-14 for kimi-1 on 2026-11-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# G′ (plan by the chance the wants are met): aligned Clojure + F8-form runtime certificate

Assigned by Joe (2026-11-25, claude-14 REPL): "get Kimi to build an aligned Clojure implementation so that we could just get started running this model ... we have a very well-established way of developing aligned Clojure that produces runtime certificates that prove that the runtime matches the Lean, and we should follow that same pattern in a dispatch for this system." No shadow implementation. Normalisation: per-step = horizon total / H (Joe: "sensible"). Hard wants (value 0): deferred; every want value is in (0, 1].

## The pattern to follow (READ these first; copy their shape, not just their idea)
- futon2 holes/labs/wm-contract/C521-F8-policy-free-energy-lean.md (the slice report form).
- futon2 holes/labs/wm-contract/f8_policy_free_energy_readback.clj (readback: every -expected TRANSCRIBED from a named Lean theorem, header lists the transcriptions by name; log evaluated only where Lean keeps it symbolic; reports deltas; states which arm is production).
- futon2 holes/labs/wm-contract/runs/F8-policy-free-energy/clojure-readback.txt (committed output).
- futon2 holes/labs/wm-contract/aif-equations.edn row :policy-free-energy (its :lean, :code, readback and note fields) — add ONE row :want-free-energy in the same shape.

## The specification (Lean, mathlib4 worktree /home/joe/worktrees/mathlib4/c14-cascade-outcome, branch c14/cascade-outcome-type)
- DarkTower/WarMachine/WantFreeEnergy.lean: WantEffect, chance, wantTerm, stepG' (= wantTerm of pushed prediction + CascadeEFE.ambiguity), totalG', scoreCascadeCo', scoreCascadeCoN' (novelty subtracted as scoreCascadeCoN), stepG'_eq_stepG_of_point (agreement with G on certain predictions).
- DarkTower/WarMachine/WantFreeEnergyBenefit.lean: mixK, chance_mix, climb_raises_chance, delivery_lowers_wantTerm.
- DarkTower/WarMachine/WantFreeEnergyWitness.lean: THE numerals to transcribe: chance_nine_in_ten 41/50, chance_certain 9/10, chance_mix_half 11/20, chance_uniform 12/25, chance_uniform_climb 57/100, b17_noTrace_realised 3/34, perStep.
- Existing Clojure to reuse, not reimplement: futon2.aif.r1-belief (b389f2fe2: rungs, climb, predict, update), and the canonical cascade G (futon2 src/futon2/aif/selection_canonical_g.clj `score`, and whatever it calls for prediction, ambiguity and novelty). Find the Clojure counterpart of CascadeEFE.stepG/totalG and of CascadeCoApplyGNovelty.scoreCascadeCoN there; G′ replaces ONLY the risk term.

## Build (one small commit per item is fine; all in futon2)
1. src/futon2/aif/want_free_energy.clj (ns futon2.aif.want-free-energy): want-effect validation (values in (0,1], refuse otherwise), chance, want-term, step-g', total-g', per-step, score-cascade-co' and score-cascade-co-n' that take the SAME prediction/ambiguity/novelty the canonical G computes and replace its risk by want-term. Pure functions; no wiring into the click in this packet.
2. test/futon2/aif/want_free_energy_test.clj: tests named after the Lean theorems (wantTerm_nonneg, wantTerm_zero_of_certain, wantTerm_point_eq_risk / stepG'_eq_stepG_of_point against the existing risk function, wantTerm_prod, reliability_helps, chance_mix, climb_raises_chance, delivery_lowers_wantTerm), planted bad cases (a want value 0 or >1 refused; a non-normalised prediction refused), and mutants you try (report which test catches each).
3. holes/labs/wm-contract/want_free_energy_readback.clj in the f8 readback form: header listing each transcription by Lean theorem name; production calls named; rational chances compared EXACTLY (Clojure ratios), want-term = -log chance compared at 1e-12 with log evaluated in Clojure; the agreement case (point prediction: G′ step == canonical G step) measured on one real-shaped candidate. Output committed to holes/labs/wm-contract/runs/G-prime-want-free-energy/clojure-readback.txt.
4. aif-equations.edn: one :want-free-energy row (:latex "G'_\pi = -\log \sum_o Q(o\mid\pi)\,c(o) + \text{ambiguity}", :lean pointing at WantFreeEnergy.lean + the Witness, :code at the new namespace, readback path, :node :R5, and a note citing Tull et al. 2023 Cor. 38/Prop. 37, Parr & Friston 2019 (GFE), Millidge et al. 2021 (epistemic term kept by statement), and that it changes no :choices). Do not edit Requirements.lean or :choices.
5. holes/labs/wm-contract/C-gprime-want-free-energy.md: the slice report in C521's form (what was in Lean, what the readback measures, deltas, what is NOT claimed).

Gates: clj-kondo clean; futon4/dev/check-parens.el on every .clj; run only `clojure -M:test -n futon2.aif.want-free-energy-test` (short timeout; timeout = failure); the readback reproduces its committed output byte for byte on a second run. Do not hot-load into the serving JVM. Bell claude-14 back with shas, test results, readback deltas, mutants, and anything you could not transcribe exactly (say so; never approximate silently).
