# E-kimi-task-438 — G' wiring part B: G' inside compare-at-horizon, both lanes, certified

**Requisition:** in-progress — dispatched 2026-11-25T05:58:27Z to kimi-1 as invoke-1795586307158-29043-a70b1b99

Clocked in by claude-14 for kimi-1 on 2026-11-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# G′ wiring, part B: build G′ inside the canonical G path (both lanes)

Part A reviewed (E-kimi-task-437, futon2 18fff967a; claude-14 spot-checked cascade_coapply_g.clj:56-90 score-one -> scorer/rank-cascade-actions with :g-normalization :raw-cumulative forced, and :138-146 the :conflicting-score-law refusal). Joe: run G′, aligned with Lean, one implementation for selection and execution, no shadow scorer. Assigned task under futon2/CLAUDE.md rule 1.

## Decisions on your open questions (claude-14)
1. Ranking: keep the current mechanism (doubles, strict minimum, :tie-at-minimum as today). Do NOT introduce interval ranking in this packet. Compute each step's chance EXACTLY (Clojure ratio: sum_o Q(o|π)·c(o)) and record it in the certificate; the want term is -log(chance) as a double (as risk is a double today), and also record cascade-g/log-bounds' enclosure of it in the certificate for audit.
2. Name: keep the result key :G-efe (consumers read it) and add :g-law :want-free-energy/v1 on every scored entry; keep the old risk as reported fields (:risk per step and :g-terms :risk) — computed in the same pass, not used for ranking. One code path.
3. Normalisation (Joe: per step = total / H): report :g-per-step = G′ / H on each entry. All candidates in one comparison share H, so ranking on the raw total is the same order; do not change :g-normalization handling (the canonical path's :raw-cumulative stays).
4. C on both lanes: the want effect c(o) is the lane's existing C mass (selection: shape/terminal-class-preference schedule; family: preference-registry terminal or the same), read as an effect. A predicted outcome whose c is 0 refuses with the existing :inadmissible-risk shape (hard wants deferred). Record in the certificate that c is C read as an effect.

## Build
1. Where outcome-risk is computed for this path (observation_model.clj :score / cascade_model_manifest.clj m/outcome-risk), also compute the exact chance and the want term from the same predicted outcome distribution and C; return both. Only the canonical path (score-one / compare-at-horizon) uses the want term for :G-efe; other callers of om :score are unchanged.
2. score-one: G′ = Σ_τ (want-term_τ + ambiguity_τ) − novelty (novelty exactly as now). Certificate steps carry :chance (ratio), :want-term, :want-term-enclosure, :risk (reported), :ambiguity, :expected-information-gain, :g.
3. Readback extension, F8 form: holes/labs/wm-contract/want_free_energy_readback.clj gains a section running compare-at-horizon on fixtures whose expected values are transcribed from WantFreeEnergyWitness (e.g. a candidate whose predicted clause outcome is bern(9/10) under C 9/10:1/10 gives chance 41/50; the certain case gives 9/10; the agreement case: on a point prediction, :G-efe(new) == the old risk-based G for the same entry, reported side by side). Commit the regenerated clojure-readback.txt; byte-for-byte on a second run.
4. Tests: the numeric pins part A listed (cascade_coapply_g_test, cascade_pattern_counts_test, family_theta_coherence_test, family_selection_canonical_g_test, selection integration tests) will move where predictions are not point masses. Re-derive each moved expectation from the reference port futon2.aif.want-free-energy (which is certified against the Lean witnesses) and say in the test which Lean theorem or witness it rests on. Never edit an expectation to whatever the code now prints. Where a test's expectation does NOT move (point predictions), that is stepG'_eq_stepG_of_point: say so in the test.
5. A planted bad case: a C with 0 mass on a predicted outcome refuses (not -log 0); and a mutant check that ranking by the reported :risk instead of :G-efe fails a selection test.
6. Update holes/labs/wm-contract/C-gprime-want-free-energy.md with a wiring section (what changed, which pins moved and from what to what, what is not claimed). Update the aif-equations.edn :want-free-energy row's :code to the live sites.

Gates: clj-kondo clean; check-parens on every changed .clj; run only the named test namespaces (short timeouts; timeout = failure); do not hot-load into the serving JVM; the WM stays held (no clicks). Small commits. Bell claude-14 back with shas, the list of moved pins (old -> new, and the source of each new value), readback deltas, mutants, and anything you could not align exactly.
