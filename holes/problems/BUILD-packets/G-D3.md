# G-D3 — BUILD (rev 3: rev 2's Policy gate was unsatisfiable — `decided` is the same date on all 48 declarations; now a two-number gate by name against the baseline JSON): the thirteen theory-defined glossary entries into Holes.lean — bodies where the glossary gives a formula, holes where it states a law

Owner lane (claude-15). Builder: codex-22 (the Lean seat). Pre-dispatch read: claude-13. One behaviour: the AIF core
mathematics enters Holes.lean as declarations bound to the glossary, exported through the registry. Time box ~40 min.
Refusal is a valid deliverable — per entry.

READ FIRST: /home/joe/code/futon2/holes/labs/wm-contract/glossary-formal-lines.md (G-D1; the 13 `[class: theory-defined]`
entries and their Formal/Markov/Lean lines are the spec — quote them); /home/joe/code/futon2/holes/problems/P-glossary-mathematics.md
(S1 solved 2–4, facades); /home/joe/code/p4ng/sec-glossary.tex (the anchors — every formula cites its line);
/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean @ HEAD (one holder: you, for this packet's duration — the owner will
not edit it until you bell back).

SCOPE: ONLY the 13 theory-defined entries. For each, exactly one of:
 (a) CLOSED-BY-RECORD body — when the glossary GIVES the formula: `variationalFreeEnergy (Π ε : Channel → ℝ) : ℝ := ½ · mean_k (Π_k · ε_k²)`
     (sec-glossary.tex:19; mean over Channel.all); `deltaFReduction` via `logMultivariateBeta` (the glossary's ΔF = ln B(A) + ln B(a') − ln B(a) − ln B(A'),
     :58 — take `logMultivariateBeta : List ℝ → ℝ` as a HOLE if Mathlib's Beta is not usable Mathlib-free, and say so); `bayesFactorThreshold := ΔF ≤ −3`
     (:60); `softmax`: **quote :39 in full — `Q(π) ∝ exp(ln E(π) − G(π)/τ)` — and the body carries BOTH terms**, the log habit prior ln E(π) and −G(π)/τ (rev 1 of this packet wrote "softmax over −G/τ", dropping E(π): a prior-weighted softmax is not a Boltzmann over G, and E(π) is what step ⑩ derives and ⑯ applies — claude-13, 2026-08-30); `predictionError ε := o − μ` per channel; `PrecisionMap := Channel → ℝ≥0`.
 (b) HOLE with evidence + falsifier — when the glossary states a LAW or names an object it does not define: `GenerativeModel` (P(o, s, π)),
     `observationKernel` A : S ⇝ O, `BeliefState`, `expectedFreeEnergy` as G(π) = risk + ambiguity with `risk`/`ambiguity` as functionals of a kernel
     Q(o∣π) (the Markov rendering from G-D1: state the kernel type; the Kleisli composite stays a hole), `expectedInformationGain`, `bayesianModelReduction`.
 (c) REFUSAL in the doc tag — quoting G-D1's refusal text — for `Model uncertainty and EIG` (needs Outcome/Q) and for anything whose formal line
     you cannot state without a decision (the two π's: DO NOT touch `Policy`; leave the glossary's cascade-grain π as a refused entry pointing at
     P-validated-R5 §3 and G-D1's note — that decision is Joe's).
THE THREE G's: `Holes.G := risk − eig` stays as is (P-validated-R5 §2a′ owns it). The new `expectedFreeEnergy` HOLE carries in its doc tag the grain
mismatch sentence from G-D1 verbatim; do not reconcile them.
REGISTRY: every new declaration in `closedDeclarations` or `holeDeclarations` with owner = the glossary anchor (e.g. "sec-glossary.tex:19 · P-glossary-mathematics")
and evidence/falsifier for holes. Re-emit (`scripts/emit-contract.sh`), commit Holes.lean, then the JSON alone; re-emit byte-identical.
ACCEPTANCE (report actuals, write the equation): **T0 is pinned to the STARTING sha 32b92969** (Holes.lean; the baseline JSON is 53c5e466) — `git show 32b92969:DarkTower/WarMachine/Holes.lean | grep -c '^/-- HOLE'` (= 24; bare HEAD would contain your additions and make the equation self-satisfying — AD-D2's fix, regressed in rev 1); after:
T0 + N_added_holes = :kind hole in JSON = `lake env lean … | grep -c 'uses .sorry.'`; zero error lines; `git diff --check`; bodies count rises by
N_added_bodies (state it). Registered expectation (claude-15, before the run): bodies +5 to +7, holes +5 to +7, refusals 2; report the actuals.
Every new body's formula must match its cited glossary line character-for-character in the mathematics — the reader will diff, specifically: mean vs sum at :19 (a sum over 8 channels is 8× the value), the ½, ε² vs |ε|, the prime placement at :58 (a′/A′ swapped flips the sign of ΔF and the ≤ −3 test), and the TWO terms at :39.
**Policy gate (checked, not requested; two numbers, by NAME):** the additions are the declarations whose `name` is in the new
holes-contract.json and absent from the baseline `git show 53c5e466:DarkTower/WarMachine/holes-contract.json`. (1) `N_selected` = that
count — must equal N_added_bodies + N_added_holes (so an empty selection fails loudly); (2) `grep -c Policy` over THOSE signatures only
must be 0 — the pre-existing `Policy` declaration (P-validated-R5 §2a′/§3 owns it) is untouched by construction. Quote both numbers and
the jq/comm command in the bell. (rev 2 selected on `decided`, which is "2026-08-30" on all 48 declarations — claude-13: unsatisfiable
when the filter is too wide, vacuous when empty.)
Note for the bell: the glossary calls G(π) "expected free energy", so `expectedFreeEnergy` and `Holes.G` collide by name by design; the verbatim grain-mismatch sentence in the doc tag is what keeps them apart.
COMMIT in mathlib4 on explicit paths. Do NOT push. Do NOT touch any other DarkTower file.
BELL claude-15 back with: sha(s); the equation; bodies/holes/refusals added by name; any entry whose formula you could not state as written and why.
