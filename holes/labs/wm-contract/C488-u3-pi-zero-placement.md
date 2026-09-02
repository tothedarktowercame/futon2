# C488 — U3: the π₀ placement, measured at the seam where it could decide

Date: 2026-09-02. Seat: claude-cli (wm-edge worklist loop, any-lane).
Item: worklist `:U3` (class `:RUN`), from `SPEC-dormant-wiring.md` §U3.
Script: `holes/labs/wm-contract/u3_pi_zero_placement.clj` (clj-kondo 0/0,
check-parens OK). Artifact: `holes/labs/wm-contract/U3-PI-ZERO-PLACEMENT.txt`,
byte-identical on two consecutive runs. **Replay only — no live run, no run
lock taken, nothing written under `data/`.**

## 0. The question this row was given, and the one it could answer

`:choices :pi-zero-form` holds three placements of ln E in the eq. 2.7 β solve:
`:none` (what `converge-beta` computes by default and what S2 recorded),
`:pi-only` (friston2017.txt:684 as printed) and `:both`
(`spm_MDP_VB_X.m:963-964`, the authors' own code). Its `:values` says the arms
are *separated but not adjudicated*, and its `:measurement`
`:what-would-adjudicate` names exactly what would settle it:

> a difference in what the machine SELECTS under each arm.

SPEC U3 names a different discriminator — "if the in-π-only arm brackets the
H1b bound better across the recorded fields, flip and record why."

**That discriminator does not exist on any committed field, and this report
says so before it reports anything else.** Of the 24 records this study reads
(S2's 20, S4's 4), **0 carry `:act-gate-verdicts` and 0 carry
`:realized-outcome`** — see CONTROL 3 in the artifact, both fields. H1b's own
restated bound (`enactedEqualsSelectedWhenRankOneGated`) has an antecedent that
occurs in **0 of the 50** records that *do* carry gates (worklist `:H1b`
`:owner-did`). So "brackets the H1b bound better" is not a comparison anything
on disk can make, for either arm, at any β.

This row therefore ran the registry's own named discriminator instead. §6 says
what that substitution does and does not license.

## 1. The chain, stated before any number was read

The three arms differ in the β solve. β reaches selection by exactly one route
— `:tau-mode :variational-beta-gamma`, where τ_eff **is** β
(`policy.clj:135-137`) — so the arms can only reach a decision through τ:

    arm a  →  β_a(t)              eq. 2.7 root, carried tick to tick
           →  τ_a(t) = β_a(t)     policy/effective-temperature (policy.clj:135-137)
           →  P_a(t) = softmax(ln E − G/τ_a − F_π)   policy/softmax-weights (policy.clj:148)
           →  argmax P_a(t)

Every step below the arm uses the shipped function, not a re-implementation.
The β solve is the one place a local implementation is unavoidable (`src`'s
`converge-beta` exposes `:none` only through this entry point), and CONTROL 1
checks the local `:none` arm against `src` on every tick before any arm is read.

**The arithmetic floor, stated first.** Under the ln-E-free, F_π-free score the
expression is −G/τ, and argmax(−G/τ) = argmin G for *every* τ > 0. On that law
no β whatever can move the choice. That is a fact about the expression, not
about our field; CONTROL 4 checks it numerically at nine τ from 1e−3 to 1e3
(24 of 24 ticks) rather than asserting it.

## 2. The four score expressions, and where each one is in the code

Read off the call sites, not inferred:

| law | expression | where |
|---|---|---|
| `:G` | −G/τ | `policy.clj:678` — the actuation boundary with every ln E zero. Posterior only; the CHOICE there is `(first ranked-actions)`, so τ decides nothing. |
| `:E+G` | ln E − G/τ | **`policy.clj:693-696` — the one site in the code where τ decides**: `chosen-idx = argmax(ln E − G/τ)`. Also the strategic boundary's counterfactual ordering (`policy.clj:494-496`). |
| `:G+F` | −G/τ − F_π | **Not reachable.** See below. |
| `:E+G+F` | ln E − G/τ − F_π | `policy.clj:553-556` — the strategic boundary's recorded posterior under `f-pi-opts` (what S4's applied ticks wrote), and U1's flip target. Recorded; never read for the choice. |

`:G+F` is not a state the machine can be in. F_π enters the posterior at the
strategic boundary and nowhere else — `select-action` throws on any other
boundary (`policy.clj:634-638`) — and that site always passes the full
`log-priors` vector (`policy.clj:553-556`), which is built as zeros rather than
nil (`policy.clj:656-657`). So F_π without ln E requires an ln E that is
identically zero, and in that case `:G+F` *is* `:E+G+F`. On our fields ln E is
non-zero on every aligned candidate (§3). It is kept as an arm for one reason,
and that reason is §5.

## 3. Fields, and the precondition the arms need

- **S2** — `runs/2026-09-01-s2/wm-trace-s2.edn`, 20 records, 20 usable aligned
  fields, 143 candidates on tick 1 and 145 thereafter (2 dropped on tick 1:
  F_π absent, matching the record's own `:f-pi-absent-count 2`).
- **S4** — `runs/2026-09-01-s4/wm-trace-s4.edn`, 4 records, 4 usable, 144–145
  candidates (1 dropped on tick 2).
- **S3 — not found**, as C486 §3 and the U1 readiness note both record;
  `runs/2026-09-01-s3/` holds `ARMS.txt` and `README.md` and its README's first
  line says the directory is a replay. It contributes 0 ticks.

The join is by `:candidate-identity`, not by the F_π envelope's `rank/N` key —
U1's defect: those keys are the *producing* tick's ranks, and a rank-key join
transposes F_π between candidates whose ordering moved.

**The precondition holds.** The arms separate only where ln E is non-uniform
over the aligned field (`:pi-zero-form :measurement :withdrawal`). Measured
here rather than carried over: **3 distinct ln E values, spread 3.0445224377
nats, 0 candidates at ln E = 0**, on all 24 ticks of both fields.

## 4. Controls, run before the arms were read

| control | what it forecloses | result |
|---|---|---|
| 1. solver | the three arms resting on an unchecked reimplementation | local `:none` arm vs `src` `converge-beta` at β_prior 1.0: max \|Δ\| **0.000e+00**, 20 ticks (S2) and 4 (S4) |
| 2. recorded β | measuring a β the run did not solve | S2's own `:beta-posterior` reproduced from its own `:beta-prior`: max \|Δ\| **0.000e+00**, 20 ticks. S4 recorded no `:policy-precision-state`, reported as NOT APPLICABLE rather than passed |
| 3. enactment | reading the H1b bracket where it does not exist | `:act-gate-verdicts` **0 of 24**; `:realized-outcome` **0 of 24** |
| 4. τ-invariance | asserting the `:G` floor instead of checking it | argmax = argmin G at all nine τ in {1e−3 … 1e3}, **24 of 24** ticks |
| 5. detector teeth | a null from a detector that cannot fire | a τ stepped across the closed-form window edge changes the argmax on **24 of 24** ticks *that have an edge*, on the one law that has edges at every tick |

Control 5 is the one that makes §5's zeros mean something. On `:G`, `:E+G` and
`:E+G+F` **no tick has a finite window edge at all** — no positive τ changes
those laws' argmax on these fields, so there is nothing for a detector to find
and the detector correctly reports 0 of 0. On `:G+F` every tick has an edge and
the detector fires on all 24. The apparatus can see an argmax change; it does
not see one where the arms put τ.

## 5. What the arms did

The arms reproduce RUN7's γ at S2 tick 20 exactly and independently — `:none`
0.9667979198, `:pi-only` 0.9040600731, `:both` 0.9712374387 — which is the
`:run-result` figure the `:interim` was argued from, now re-derived by a second
script from the same records.

**How far apart the arms put τ**, as a ratio max/min across the three arms
(1.0 = the arms hand selection the same temperature):

| β₀ | S2 mean ρ | S2 max ρ | S4 mean ρ | S4 max ρ |
|---|---|---|---|---|
| 0.5 | 1.090114 | 1.166361 | 1.022557 | 1.035897 |
| 1.0 | 1.040052 | 1.074306 | 1.009931 | 1.015773 |
| 2.0 | 1.015648 | 1.029012 | 1.003867 | 1.006113 |
| 5.0 | 1.004357 | 1.007991 | 1.001080 | 1.001686 |

**What that does to the choice.** λ_break is the factor by which τ would have to
move to change *this tick's* argmax — computed in closed form, not by scanning:
the score is linear in u = 1/τ, so the argmax as a function of u is the upper
envelope of straight lines and the window edges are the nearest line crossings.
"Headroom" is (λ_break − 1)/(ρ − 1): how many times further the choice can
survive than the arms actually move.

### At β₀ = 1.0, the value both stage runs used

| field | law | argmax differs | rank moves (π-only vs both) | λ_break min | ρ max | headroom |
|---|---|---|---|---|---|---|
| S2 | `:G` | **0 of 20** | 0 | — τ-invariant | 1.074306 | ∞ |
| S2 | `:E+G` | **0 of 20** | 0 | 13.599688 | 1.074306 | **169.6×** |
| S2 | `:G+F` *(unreachable)* | 0 of 20 | 45 (max 4) | 1.782538 | 1.074306 | 10.5× |
| S2 | `:E+G+F` | **0 of 20** | 36 (max 3) | 13.702105 | 1.074306 | **170.9×** |
| S4 | `:E+G` | **0 of 4** | 0 | 13.588240 | 1.015773 | **798.1×** |
| S4 | `:E+G+F` | **0 of 4** | 0 | 13.690571 | 1.015773 | **804.6×** |

### Across the β₀ sweep, on `:E+G` — the site where τ decides

| β₀ | S2 argmax differs | λ_break min | ρ max | headroom |
|---|---|---|---|---|
| 0.5 | 0 of 20 | 6.813187 | 1.166361 | 34.9× |
| 1.0 | 0 of 20 | 13.599688 | 1.074306 | 169.6× |
| 2.0 | 0 of 20 | 27.166105 | 1.029012 | 901.9× |
| 5.0 | 0 of 20 | 67.674669 | 1.007991 | 8343.3× |

β₀ was swept rather than fixed at the runs' 1.0 for C486's reason: C468 found
the J4 rank movement concentrated at β₀ = 0.5, and a result at one initial
condition is a result about that condition. Here the sweep is monotone in the
safe direction — the arms converge as β₀ grows while the choice's tolerance
widens — so 1.0 is neither the best nor the worst case, and the worst case
(β₀ = 0.5) still leaves 35×.

**Read it in this order.**

1. **On every score expression the machine can be in, at every β₀, on both
   fields: 0 argmax differences between the arms — 0 of 288** (3 reachable
   laws × 4 β₀ × 24 ticks). At
   β₀ = 1.0 the *whole ordering* is identical too on `:G` and `:E+G` — not one
   candidate of 145 moves one place.
2. **The zero is typed, not bare.** At the one site where τ decides, the choice
   survives a 13.6-fold change in τ and the arms move it by 1.07-fold. The
   field is 170 times further from a difference than the arms travel.
3. **The one expression where the placement separates is one the machine cannot
   be in.** `:G+F` at β₀ = 0.5 moves the argmax on **11 of 20** S2 ticks, with
   λ_break 1.0437 against ρ 1.1664 — the arms move τ *further than the choice
   survives*. Drop ln E from the selection score and the π₀ placement starts
   deciding things. But F_π and ln E arrive at that call site together or not
   at all (§2), so this is what the null looks like from the other side, not an
   alternative the register could choose.
4. **What ln E does to τ's authority is the mechanism.** ln E's 3.04-nat spread
   across 3 groups makes the top of the score robust: within one ln E group the
   ordering is τ-invariant (the shift is monotone in G), so only cross-group
   pairs can flip, and the nearest cross-group crossing sits 13.6× away. This
   is the same structure RUN8's S3 arms found from the other direction — its
   `ARMS.txt` reports a smallest cross-group adjacent margin of 0.7355590795
   with 0 argmax moves.
5. **On the path the runs actually took, β reaches nothing that selects at
   all** — and this is stronger than the headroom. All 24 records carry
   `:selection-boundary :reason-bearing-strategic-policy`, which is not one of
   `policy/select-action`'s boundaries: the controller decision's `:action` is
   *replaced* downstream at `war_machine.clj:5241-5244` by the mission the R14
   strategic selector returned. That selector is called
   (`war_machine.clj:5211-5214`) with a vector of three mission-id strings and a
   trace-id — the strategic candidates in `wm-admissible` order
   (`war_machine.clj:5203-5209`), where `wm-admissible` is the G-ordered ranked
   list filtered by executability (`war_machine.clj:5169`). No τ, no posterior,
   no score crosses that call. Measured from the records to match: the
   `:decision :action` equals the argmax of ln E − G/τ on **0 of 24** ticks.

## 6. Recommendation, and what it does not rest on

**Recommend `:habit-prior-in-both`. The entry stays `:open-branches`; the
ruling is Joe's.** Three grounds, in decreasing order of what the numbers carry:

1. **The adjudicating measurement the entry specified has now been run at the
   seam the entry named, and returns a typed no-discrimination result.** 0 of
   288 argmax differences — every reachable law, every β₀, both fields — with 35×–8400×
   headroom and a detector shown able to fire. This does not say `:both` is
   better. It says nothing the machine does distinguishes the arms, by a margin
   the report states rather than leaves implicit.
2. **Under SPEC U2's own fallback rule** — "if the arms do not discriminate on
   retrodiction, choose by source fidelity" — the source is the authors' own
   implementation, which puts ln E in both (`spm_MDP_VB_X.m:963-964`, verified
   in clone by C463). That is already the `:interim`, so this changes nothing;
   it removes the reason the entry was open.
3. **One ground that is measured and is about the arms rather than the field.**
   `:both` leaves γ 14× closer to the ln-E-free arm than `:pi-only` does
   (|Δγ| at S2 tick 20: 0.004440 against 0.062738), reproduced here
   independently of RUN7. That near-cancellation is what the SPM rationale
   predicts: with ln E in both, (π − π₀) isolates what F_π alone does, which is
   what the β update is meant to measure. With ln E in π only, the update is
   driven by the habit prior about as much as by F_π.

**What U3 must not be read as answering.** The `:none`-against-the-other-two
separation is the question of whether ln E enters π at all — U3's, not U2's,
per C486's `:reviewer-should-check (5)`. These numbers do not settle it either:
it produced 0 argmax differences too, and both sources answer it *yes* on their
own authority. It is settled by the sources, not here.

**What would make the placement decidable by measurement.** Not a longer
replay. Two things have to change, and neither is in this row's gift:

- **A selector that reads the posterior.** While `war_machine.clj:5241-5244`
  replaces the decision's action with R14's mission, no β, no τ and no F_π can
  change what the machine does, whatever U1 flips. U1's readiness note point 4
  says the same thing about `:strategic-recommendation`; §5.5 is that
  observation carried one call further, to the site that actually overwrites
  the action.
- **A field where the top of the score is close.** The 13.6× margin is not
  noise — it is ln E's 3.04-nat group structure holding the argmax down. A
  placement question can only be decided on a field where the leaders are
  within one ln E group of each other.

**No ruling is written by this row.** §6 is a recommendation with its grounds;
`:pi-zero-form` stays `:open-branches`.

## 7. Bounds

- **24 ticks, one route, one apparatus** — the same bound C486 §7 states. S2's
  20 ticks are one path (RUN3: 180 hops, 9 distinct); S4 is four ticks.
- **The λ_break window is exact for the argmax and says nothing about the rest
  of the ordering.** Rank movement is reported separately, and it is not zero:
  36–45 candidates move at β₀ = 1.0 on the F_π-bearing laws. Only the top is
  13.6× away.
- **`:G+F` is a synthetic arm.** §2 argues it is unreachable from the call
  sites; that is a reading of the code, and a reviewer who finds a caller that
  passes a zero ln E vector alongside live F_π should say so — it would turn
  §5.3 from a control into a live case.
- **The R14 selector itself was not chased into its own repo.** What is
  established here is the *shape of its input* at the futon2 call site
  (`war_machine.clj:5203-5214`) — three mission-id strings and a trace-id —
  and that the decision's action is replaced with its answer. What that
  selector does with them is outside this row.
- **τ = β is the only route from the arms to selection.** Under the other two
  `:tau-mode` values β does not reach τ at all, so the whole comparison is
  conditional on the variational mode being the one that runs. S2 solved β; S4
  did not (`:tau-source :selection-gain-only`, τ = 1.0 on all 24 recorded
  ticks), so S4's arms are computed from β series this study solves, not from
  temperatures S4 used.
