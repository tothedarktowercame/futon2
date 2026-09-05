# C516 — :F1 slice 2, the machine-grain Q(o|π): the rows coincide, and that is the measurement

**Row:** `:F1` slice 2 — the machine-grain `Q(o|π)` instance over the cascades
`:F7` constructed.
**Gate that opened it:** `aif-equations.edn` `:choices` `:policy-grain`
(Joe, 2026-09-05 — the grain is the cascade, the question closed) plus `:F7`
(futon2 `73b6616a`, two distinct constructed-and-scored cascades for one
recorded decision). Slice 2's `:blocker` — "needs π, the machine produces no
admissible one" — was answered by `:F7`, not by a ruling written here.

## 1. What was actually new, stated narrowly

Slice 1 proved the composition in Lean and slice 3 transcribed it into
`src/futon2/aif/machine_q.clj`. Both ran on carriers **declared for the
demonstration**, which under `FUNDAMENTALS.edn`'s criterion makes them fixtures
and inhabits nothing. This slice ran the same composition on values the shipped
code computes from machine data:

| carrier | provenance | where |
|---|---|---|
| states | machine data | `futon2.aif.belief/status-set`, `src/futon2/aif/belief.clj:42` — 7 lifecycle statuses |
| outcome alphabet | machine data | the 7 observed events A has rows for, `belief.clj:199-206` |
| A | machine data | `belief/observation-model-v1` (`belief.clj:199-206`), transposed to `{s {o mass}}` |
| belief | machine data | the `:mu-post` of recorded run `c149f9de-669c-4817-9b0e-ed4aad77db79` (`data/wm-trace/wm-trace-2026-09-04.edn`) |
| π | machine data | the two cascades of `runs/F7-cascade-policy/f7-cascade-policy-decision.edn` |
| **B** | **machine data, lifted** | `belief/transition-model-v1` (`belief.clj:216-231`) under the constant lift `B(s,u) = B(s)` |

**No entity was chosen for the belief and none had to be.** All 417 entities in
that record carry ONE distribution; the producer refuses rather than picks if
that ever stops being true
(`f1_machine_grain_q.clj` `machine-belief`).

**The policy identities are recomputed, not copied.** `cascade-prior/policy-key`
is run over each candidate and compared with the key `:F7` recorded; a mismatch
throws. That is the defect `:F7`'s control 12g showed was single-point.

## 2. The finding: Q(o|π) is constant in π, and two arms say why

`runs/F1-machine-q/03-machine-grain-q.edn`:

| arm | plans differ | rows coincide | max abs row difference |
|---|---|---|---|
| `:machine-grain/machine-plan` | no | **yes** | 0.0 |
| `:machine-grain/cascade-plan` | yes | **yes** | 0.0 |
| `:machine-grain/declared-B` (declared control) | yes | no | 0.021891808923863332 |

Arm 1: under the machine's own action identity
(`habit-prior/policy-key`, `src/futon2/aif/habit_prior.clj:29-38`) the two
cascades plan the **same** step — the action grain excludes cascade payloads by
design, and the docstring says so. Rows must coincide, and do, by
`rowsEqualOfEqualPlans`.

Arm 2 is the isolation, and it is the arm that matters. Each cascade plans
**itself** (u = its cascade-grain policy key), so the plans differ — and the
rows still coincide, because `transition-model-v1` is a 7×7 **identity with no
action index at all** (`belief.clj:216-231`: "no explicit dynamics; the prior is
the previous posterior"). `Q(s|π)` came back equal to the belief in every arm.
So what is missing is **B's dependence on u**, not the plan's dependence on π.
That is `FUNDAMENTALS.edn` `:fundamental/controlled-transition-kernel` — until
now read off the code, now measured on a run.

Arm 3 is a **declared control and an inhabitant of nothing**: give B an action
index and the rows separate. Its purpose is to keep arms 1 and 2 from being read
as a fact about `machine_q` — the composition separates policies as soon as B
reads u.

The constant lift is not one option among several. It is the only lift that adds
no information to what `belief.clj` holds; any other is a modelling ruling, and
this lane writes none.

## 3. A defect in slice 3's boundary, found by feeding it machine objects

`predictive-outcome-row!` promises typed refusals. It threw
`ClassCastException` instead, for **any** row carrying a non-numeric value: the
row sum was computed eagerly in the `let`, so `double` ran before the
`:refusal/non-probability-mass` branch could fire. Slice 3's own controls never
hit it because every row they fed had numeric values.

Two of this slice's controls did: the recorded `:mu-post`
(entity → distribution) and an `:F7` cascade candidate both came back
`:refusal/untyped-throw`.

- **Fix:** `src/futon2/aif/machine_q.clj:204` — the sum is a `delay`, forced only
  in the `:refusal/unnormalised` branch at `:221-223`. Both objects now refuse as
  `:refusal/non-probability-mass`.
- **Test:** `test/futon2/aif/machine_q_test.clj:159` — two assertions added to
  `malformed-rows-are-refused-with-typed-reasons-test` (a map mass, a string
  mass).

This is inside `:F1`'s own acceptance ("boundary rejection shown by negative
control (proxy fed in → **typed** refusal)"), which is why it was repaired here
rather than reported onward.

## 4. A limit of the boundary, recorded rather than dressed up

The recorded belief posterior — a genuine distribution over the **states** —
is **ACCEPTED** as a row over the **outcomes**. The machine names its 7 observed
events with the same 7 keywords as its lifecycle statuses (`belief.clj:42` and
`:199-206` have equal key sets), so `predictive-outcome-row!` cannot tell the two
carriers apart. It checks shape, support and normalisation; carrier identity is
not among them.

Six controls, with the verdict each actually produced
(`03-machine-grain-q.edn` `:boundary-refusals`):

| fed | verdict |
|---|---|
| `:F7`'s own cascade-selection weights — Q(π) at the cascade grain | `:refusal/outcome-outside-declared-alphabet` |
| one `:F7` cascade candidate map | `:refusal/non-probability-mass` |
| the recorded `:mu-post` | `:refusal/non-probability-mass` |
| the recorded 14-channel `:observation` | `:refusal/outcome-outside-declared-alphabet` |
| the recorded belief's single posterior, offered as an outcome row | **`:accepted`** — §4 |
| the machine-grain row this slice computed (positive control) | `:accepted` |

**Negative control:** `:F7`'s single-cascade record
(`runs/F7-cascade-policy/f7-single-cascade-decision.edn`, `:candidate-count 1`,
`:menu-status :no-policy-choice`) — one policy is not a policy-conditioned
anything. The receipt classifies it `:unavailable-single-policy-menu`; that is
the receipt's own classification, not a `machine_q` refusal.

## 5. Gates

- `clj-kondo` 0 errors / 0 warnings on `machine_q.clj`, `machine_q_test.clj`,
  `f1_machine_grain_q.clj`.
- `check-parens` OK on the same three.
- `futon2.aif.machine-q-test`: 12 tests / 58 assertions, **0 failures 0 errors**.
- `q_interface_completeness_check`: **PASS**, with all three negative controls
  (`--negative-control`, `--negative-pin-behind`, `--negative-empty`) still
  rejecting.
- **The `test/futon2/aif` directory is RED, and it was red before this slice.**
  1155 tests / 10343 assertions, 6 failures / 9 errors — all of them in
  `actuator-a3-test` (9 errors, all at `src/futon2/aif/actuator_a3.clj:150`) and
  `fold-escrow-test` (6 failures), all from `futon6` fold-turn deposits failing
  pin-1B prompt reconstruction (`:prompt-not-reconstructable`), the failure class
  `C24` and `C174` already describe. Checked, not assumed: the same two
  namespaces run in a clean detached worktree at `HEAD` give the **identical**
  6 failures / 6 errors, and the full-directory run at `HEAD` reports the same
  9 `actuator_a3.clj:150` errors and the same 6 `fold_escrow_test` failures.
  Slice 3 recorded this directory green on 2026-09-05; the red arrived between
  then and now from other work, and repairing it is outside `:F1`'s bar.
- `negative_controls.sh` and `pointer_check.bb`: see the commit message.

## 6. What this slice did not do

- **No ruling.** `aif-equations.edn` `:choices` and `control-map-edges.edn`
  `:decisions` are untouched. Arm 3's kernel is labelled `:declared-control`
  everywhere it appears.
- **No `FUNDAMENTALS.edn` verdict moves.** `:fundamental/controlled-transition-kernel`
  stays `:in`; this run is evidence **for** that entry.
- **No Lean.** The Lean leg's carriers are still declared for the witness.
- **Nothing selected, scored or enacted.** The cascade prior stays cold; no run
  lock, no tick, nothing written under `data/`; `data/wm-trace` read only.
- **`src/` still binds `*seam-reading*` nowhere.** The caller here is a lab
  script, so the R4 seam stays dark and no recorded number moves.
- `gen_aif_dag.bb` and `gen_q_interface_table.bb` not run; nothing regenerated
  into a publish (TN §9a).
