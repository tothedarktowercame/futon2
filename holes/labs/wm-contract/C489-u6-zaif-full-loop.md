# C489 — U6: one recorded zaif v0 decision through the WM's R nodes

**Row:** `worklist.edn :U6` (`:class :I`, `:owner :any`, `:covers-key :none`).
**By:** claude-cli (wm-edge loop, any-lane), 2026-09-02.
**Statement:** "Full-AIF-loop unit test on ZAIF — a practical mission driven
through the system, per component."
**Artifacts:** `test/futon2/aif/zaif_full_loop_test.clj` (the test),
`u6_zaif_decision.clj` (the probe), `U6-ZAIF-DECISION.txt` (its output,
byte-identical on two runs).
**No ruling.** Nothing here is written to `aif-equations.edn :choices` or to
`control-map-edges.edn :decisions`. The row's `:covers-key` is `:none`.

## 1. What was built

A test namespace that drives ONE real recorded zaif v0 decision through the War
Machine's R-node functions, one `deftest` per node, so a regression names the
node that broke. 15 tests, 136 assertions, green.

The subject is zaif v0: `futon3c/src/futon3c/agents/zaif_controller.clj` (270
lines) with its hydrator `zaif_inputs.clj` (209 lines) — a pure per-decision
argmax over four arms (`:retrieve :act :ask :yield`) scored by seven fixed
documented constants.

**The cross-repo problem, and how it is closed.** futon3c is not on futon2's
classpath (`futon2/deps.edn :deps`), so a futon2 test cannot call `decide`.
The acceptance allows "a planted fixture shaped exactly like one, with the
shape pinned against zaif_controller.clj", and this row does better than that
in one respect and exactly that in the other:

- the fixture's NUMBERS were produced by the shipped controller, not
  reconstructed — `u6_zaif_decision.clj` runs in the futon3c JVM against
  futon3c `0252995` and freezes the output;
- the fixture's SHAPE is re-derived from the controller SOURCE on every test
  run: `controller-forms` reads the file, `source-constants` pulls the
  `constants` map, `source-arm-order` pulls the arm vector `choose-arm` ranges
  over, and the fixture is checked against both.

Verified by mutation, twice: changing the fixture's
`:operator-attention-cost` from 0.65 to 0.5 turns the suite red (3 failures,
naming `zaif-fixture-shape-is-pinned...` and `r17-constants-do-not-drift...`),
and changing the SOURCE constant from 0.65 to 0.55 turns it red the same way.
The pin catches drift in both directions, which a frozen copy would not.

This matters because the existing replay harness, `z2_calibrate.clj:56-58`,
says of itself "We inline the controller arithmetic (matching
zaif_controller.clj exactly) so this script is self-contained and auditable" —
a copy with nothing checking that it still matches.

## 2. The frozen decision

`e-ce907fcf-c7e1-4272-a4a2-13def7aaaa50`, 2026-06-08T15:26:51.274850972Z, from
`holes/labs/M-zaif-harness/calibration-sessions.edn`. A real recorded operator
turn: gold-judged, labelled `:is_correction true`, attributed to
`M-futon-forward-model` — the ONE mission whose B1 γ cell is burned in
(γ = 0.7071067811865476, not the uniform 1.0 prior), so the fixture is
non-degenerate on the γ channel.

```
inputs   {:mission "M-futon-forward-model"
          :gamma {"M-futon-forward-model" {:policy-precision 0.7071067811865476}}
          :task-belief {}
          :c-belief {:operator-c-uncertainty 1.0}
          :observations {:posting-stats {:total-docs 80 :dfs [1 …] :estimated-tokens 160}}}
decision {:arm :retrieve
          :g-terms {:retrieve 0.7622675616071356 :act 0.0 :ask 0.35 :yield 0.0}
          :gamma-used 0.7071067811865476 :operator-attention-cost 0.65}
```

## 3. What was MEASURED, over all 114 recorded sessions

All from the shipped controller and the live hydrator, in `U6-ZAIF-DECISION.txt`:

| quantity | value |
|---|---|
| sessions | 114 |
| arm distribution | `{:retrieve 31 :act 83}` |
| chosen by TIE-BREAK, not by a score | **83**, all `:act` |
| `:act` G-term non-zero on | **0** of 114 |
| `:task-belief` non-empty on | **0** of 114 |
| distinct γ used | `[0.7071067811865476 1.0]` |
| distinct `:ask` G-terms | `[-0.35 0.35]` |
| `:retrieve` G-term range | `[-0.4, 0.7738964064459891]` |
| arm distribution at the Z3a sweep cost 0.15 | `{:retrieve 15 :ask 99}` |

Two findings follow, both code-backed.

**(a) γ is read on every decision and enters none of them.**
`gamma-for-mission` (`zaif_controller.clj:34-44`) is consulted every time, and
its only consumer is `act-value` (`:76-85`), whose belief term falls back to
0.0 when `:task-belief` supplies no `:act-value`/`:pragmatic-value`/
`:expected-utility`. The live hydrator sets `:task-belief {}` unconditionally
(`zaif_inputs.clj:190-194`). So on 114 of 114 sessions the `:act` term is
exactly 0.0 and γ multiplies zero. This is Joe's "recorded but never consulted"
one level down: the quantity is not merely unread, it is read into a product
with a structurally absent factor.

**(b) 83 of 114 live decisions were settled by the tie order, not by a score.**
With `:act` = 0.0 and `:yield` = `:yield-baseline` 0.0, those two arms tie at
the maximum whenever `:retrieve` and `:ask` are both ≤ 0. `choose-arm`
(`:98-103`) sorts by `[(- value) position]` and takes the first, and `:act`
comes first in the source vector `[:retrieve :act :ask :yield]` — no: `:act` is
given tie-rank 0 by the explicit `(case arm :act 0 :retrieve 1 :ask 2 :yield 3)`
key. So the shipped `{:act 83}` is 83 tie-breaks. The 31 `:retrieve` choices
are the only ones a score settled.

The Z3a sweep constant is what makes the arithmetic move at all: at 0.15 the
distribution becomes `{:ask 99 :retrieve 15}` — `:act` disappears entirely.

## 4. What was PLANTED, and how much it carries

zaif v0 declares NO observation model. Its arm values are dimensionless
preference scores, so ANY placement of them into the WM's observation space is
a declared choice. The test makes that choice in exactly one function,
`plant-per-arm-prediction`, under a stated rule: the arm's own recorded G-term
is added to a base value on one WM channel, clamped to [0,1], and paired with a
single shared variance. Two things are planted and nothing else: the per-arm
Q(o|π) at R4, and the realised observation at R8 (the base value, because the
recorded turn is labelled a correction — no gain).

**The negative control measures how much the plant carries.**
`plant-dependence-negative-control-test` runs the same three arms on three
different plant channels and gets three different R5 orderings:

| channel | C range | R5 arm order (best first) | head law chooses |
|---|---|---|---|
| `:mission-health` | [0.5 1.0] | `[:ask :act :retrieve]` | `:ask` |
| `:support-coverage` | [0.8 1.0] | `[:act :retrieve :ask]` | `:act` |
| `:sorry-count-norm` | [0.0 0.3] | `[:act :ask :retrieve]` | `:act` |

So **which arm the WM prefers is a property of the plant, not of the arms**,
and nothing in this row may be read as a measurement of which arm is right.
What survives the control is the structure: the pipeline runs end to end on
zaif-shaped candidates, and the nodes that cannot run are named.

One more thing is forced rather than measured, and the test says so at the
assertion: R8's F_π ordering. Every planted mean is `base + a non-negative
G-term` and the realised value IS `base`, so F_π is monotone in the planted
gain by construction. That `:act` fits best (−1.3836) and `:retrieve` — the arm
zaif chose — fits worst (11.1164) is arithmetic, not evidence.

## 5. What the pipeline does when it runs

On the `:mission-health` plant, with the flags on
(`:selection-law :full-score-posterior`, `:f-pi-policy-posterior? true`):

- **R5**: G = risk + ambiguity with residual exactly 0.0 on all three arms.
  Risk discriminates (3 distinct values); **ambiguity does not** — one value,
  −0.883646559789373, on all three, because zaif declares no per-arm variance
  anywhere in its source. So G's ordering IS the risk ordering exactly.
  Structurally the same shape C487 found on the WM's own side: the deficiency
  is the coverage of declared variances, not the ambiguity functional.
- **R14**: τ = 0.10396979632561867 from the arm G spread at unit selection gain.
- **R6 (ln E)**: from the recorded arm history, folded by the WM's own
  `habit-prior/observe-action` — 83 `:act`, 31 `:retrieve`, 0 `:ask`, so
  `:ask` sits at the α-only floor, ln E = −4.762173934797756. This is the one
  input at R6 that is measured rather than planted.
- **R16**: **three laws, three arms, from one recorded decision.** zaif chose
  `:retrieve`; the WM's controller-head law chooses `:ask`; the full-score law
  (ln E − G/τ − F_π) chooses `:act`, with
  `:moved-from-controller-head? true`. Under this plant the arm zaif chose is
  the WM's LEAST preferred at G.

## 6. The honest gap list

Recorded as data in the test (`r-node-coverage`) so it is asserted rather than
narrated: every entry is typed, carries a basis pointer, and the node set is
checked against the nodes U6's statement names.

| node | quantity | status | basis |
|---|---|---|---|
| R2 | o | **stub** | `zaif_inputs.clj:153-167` emits `:posting-stats`; `observation.clj:11-33` lists the WM's 14 channels; the two sets are disjoint |
| R1 | μ | **stub** | `zaif_inputs.clj:190-194` sets `:task-belief {}`; 0 of 114 sessions carry one |
| R3 | μ_next | **stub** | no belief update exists; `decide` is stateless across turns |
| R4 | Q(o\|π) | **stub** | `forward_model.clj:25-31` excludes all four arms; `fm/predict` throws on each (pinned) |
| R5 | G | **partial** | `zaif_controller.clj:129-132` builds a scalar yield−cost per arm, not risk+ambiguity; no per-arm variance declared, so R5b cannot discriminate |
| R7 | Π | **stub** | no prediction-error history and no variance to take a precision of |
| R8 | F_π | **stub** | nothing scores a previous prediction against a later observation |
| R14 | τ, γ | **partial** | γ is read every decision and multiplies 0.0 (§3a); there is no τ at all |
| R6 | Q(π) | **stub** | `choose-arm` is a raw argmax; no softmax, no ln E |
| R16 | u | **exercises** | `choose-arm` is a deterministic argmax with a fixed tie order — the same class of rule as dacosta2020 eq. 11 — but 83 of 114 choices were settled by the tie order and 31 by a score |
| R17 | a_conc, ΔF | **stub by design** | constants are a literal map (`:11-20`) with no update path; `dual-constants` (`:143-158`) records a second cost for paired comparison and never folds it back |

Exactly one node is exercised. R17 is on the list and typed, which is what U6's
statement predicted.

## 7. U5 held

`r16-candidates-stay-opaque-at-the-seam-test`: the same scores, ln E and F_π
under `{:type :retrieve}` candidates and under `{:type :apply-cascade :target …
:payload {…}}` candidates give the same chosen rank and the same
`:selection-law :chosen-rank`, while the candidate identity does change (so the
test is not vacuous). The suite survives arms becoming cascades.

## 8. Not claimed

- No ruling, no registry write. `:covers-key :none`.
- Replay only: no live run, no run lock taken, nothing written under `data/`,
  and `persist-decision!` is never called.
- The R4 plant is not a derivation and §4's control is the evidence that it is
  not treated as one.
- The 114-session corpus is the calibration corpus
  (`calibration-sessions.edn`), replayed through the LIVE hydrator. It is not a
  log of decisions zaif actually took in production; no such log was found
  (`grep -rl zaif-arm-choice` over futon2 and futon3c returns one technote and
  no artifact).
- `pointer_check.bb`'s root list has no futon3c root, so this row's central
  file cannot be cited in `file.clj:NN` form inside `worklist.edn` without the
  checker reporting "file not found". The row's `:evidence` writes futon3c
  pointers as `… .clj L11-L20` to avoid a false red. Appending a futon3c root
  is a p4ng change and outside this acceptance — recorded here as the same
  defect class C472/C473/AC7 named, one repo over.

## 9. Gates

- clj-kondo on `test/futon2/aif/zaif_full_loop_test.clj`: 0 errors, 0 warnings.
- `futon4/dev/check-parens.el`: OK.
- `clojure -X:test :patterns ["futon2\..*-test$"]`: see the ledger row.
- `p4ng/empirics-futon/negative_controls.sh` and `pointer_check.bb`: see the
  ledger row.
- `U6-ZAIF-DECISION.txt` byte-identical on two consecutive runs
  (md5 bc7764596e6769a5928763850db93e7b).
