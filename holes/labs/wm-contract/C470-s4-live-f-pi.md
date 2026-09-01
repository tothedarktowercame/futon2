# C470 — RUN9 / stage S4: F_π live in the policy posterior, and the R8 scalar retired

Owner: claude-20 (wm-build-loop work seat). Author ≠ reviewer: this needs a second read.
Ledger row: `worklist.edn` `:RUN9`. Predecessors: RUN7 (S2, dark β), RUN8 (S3, live τ = β).

## 1. What the row asked

> STAGE S4. Live F_π in the policy posterior (I2 slice d), and the R8 scalar free
> energy retired by a decision with a run behind it rather than by argument.
> **Acceptance:** retirement is a C row with the run that justifies it named in
> `:evidence`. Blocked on RUN8/S3 being read.

RUN8 was read by codex at 2026-09-01T20:21:44Z, so the stage discipline ("no piece
is wired live until the previous run has been read") permitted the wiring.

## 2. The wiring (I2 slice d)

`FUTON_WM_FPI_POSTERIOR=1`, **default off**. One seam, the one I2 (b2) built:
`policy/softmax-weights`, score `ln E − G/τ − F_π`.

- `war_machine.clj:123-155` `f-pi-posterior-preconditions!` — refuses the flag
  without `FUTON_WM_FPI_DARK=1` and `FUTON_WM_TRACE_POLICY_DETAILS=1`, the same
  shape and the same reason as RUN8's τ preconditions: with the chain off every
  tick records `:no-f-pi-readback` and the posterior is the old one, which in a
  record cannot be told from a tick whose coverage was merely incomplete.
- `war_machine.clj:384-461` `f-pi-posterior-opts` — the join, pure.
- `war_machine.clj:5095-5096` — the call site, and **the pointer that mattered**:
  the values are joined against `wm-admissible`, the `can-execute?` filter of the
  field `select-action` actually receives, **not** the wider `wm-ranked+cascades`
  the readback and the β carry ran over. Joining against the wrong one would have
  misaligned `:f-pi-values` with `g-totals` silently.
- `policy.clj:413-421` — the decision echoes the caller's own envelope with
  `:applied?` set from the flag it used, so the record and the seam cannot
  disagree about whether F_π entered.
- `policy.clj` `select-action` **throws** if `:f-pi-policy-posterior?` is true on
  a non-strategic `:selection-boundary`. The `:actuation` branches have their own
  softmax call sites; ignoring the option there would let a caller believe B.9 was
  in force on a path where it was not.

**Scaling is not a choice here.** `:unscaled`, settled by source in I2
`:slice-b2-followup` (1): friston2017 eq. 2.7 multiplies G by γ alone and leaves F
unscaled, and B.9 has no temperature at all. The `:by-tau` arm stays reachable in
`softmax-weights` for replay and is not offered to the live path.

### 2a. Complete-or-off, and why it is not a shortcut

A current candidate with no matched previous prediction has no F_π. Three options
were on the table and only one is honest:

1. Impute `0.0`. **Rejected on measurement**: F_π on the live field runs
   −19.716…−18.962 within a tick — a spread of 0.754 sitting about 19 nats below
   zero — so a 0.0 imputation hands the uncovered candidate the posterior outright.
   The B.9 reading that would justify 0.0 ("this policy has observed nothing")
   is wrong anyway: in B.9 every policy is scored against the *same* observed
   data, so a missing F_π is a **join failure**, not an empty observation set.
2. Impute the field mean. Shift-invariant and therefore superficially neutral,
   but it is still an invented value that makes the candidate exactly average.
3. **Complete-or-off**: if any candidate is uncovered, F_π does not enter this
   tick at all and the record says why. This is what the code does.

An identity claimed by two candidates counts as uncovered, for the same reason
`f-pi-dark-readback` records ambiguity as an absence rather than picking one.

**The cost was predicted before the run and then measured.** The docstring
predicted, from the S2 field (`runs/2026-09-01-s2/`, 19 of 20 ticks with
`:unmatched-current-count` 0), that the rule would turn the term off on the first
tick of a run and leave it on afterwards. The S4 run turned it off on **one of
four** ticks — not the first, the second (see §4). So the prediction was right in
magnitude and wrong in placement: candidate-set churn is not confined to the cold
start.

## 3. Pre-flight (`run9_s4_preflight.clj`), two real ticks

Env set in the shell, not stubbed, so the flags being read from the real
environment is half of what this is evidence for. `http/post` intercepted, `spit`
suppressed except the run lock, `slurp` recorded.

| | control (flag off) | S4 (flag on) |
|---|---|---|
| POSTs attempted | 0 | 0 |
| `.admintoken` reads | 0 of 1662 paths | 0 of 1662 paths |
| run lock | taken, released | taken, released |
| readback | `:present`, 145/145 matched | `:present`, 144/145 matched |
| envelope | `:absent :flag-off` | `:absent :incomplete-coverage`, 1 of 145 |
| posterior vs F_π-free recomputation | identical, max Δ 0.0 | identical, max Δ 0.0 |
| verdict | **PASS** | **FAIL** |

The FAIL is the guard firing, not a defect, and it is worth stating why the
pre-flight cannot pass with the flag on: it **suppresses writes**, so the second
tick's "previous record" is hours old and the candidate set has drifted. An
isolated tick is exactly the case complete-or-off refuses. A stage run is what
this needed, which is §4.

## 4. The stage run (`runs/2026-09-01-s4/`), four ticks

`FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_FPI_POSTERIOR=1
bash wm_run.sh 4 14 claude-20` — one run lock across all four ticks (RUN12), each
record carrying `:run/id` (RUN11), τ left at the default `:selection-gain-only` so
the only difference from S1 is F_π.

| tick | run id | envelope | scored/current |
|---|---|---|---|
| 20:36:03 | dd00675b | `:present :complete`, **applied** | 145/145 |
| 20:37:20 | 73282cf8 | `:absent :incomplete-coverage`, 1 uncovered | 144/145 |
| 20:38:39 | 200b894f | `:present :complete`, **applied** | 145/145 |
| 20:40:00 | 5bd37efe | `:present :complete`, **applied** | 145/145 |

**F_π entered the live policy posterior on 3 of 4 ticks.** The fourth declined,
recorded the reason, and selected exactly as it would have without the flag.

## 5. What moved (`run9_s4_arms.clj`)

Replayed with `policy/softmax-weights` **itself**, from each record's own
persisted inputs — the controller ranking's `:controller-score` and
`:habit-prior-bias`, the decision's `:tau`, and the F_π readback joined by the same
action identity the live tick joined on.

**Control first.** On S4, 4 of 4 ticks re-join and the replay reproduces each
record's own `:softmax-weights-by-candidate-id` at **max |Δ| 0.000e+00** under the
setting that record says it ran with. Since three of those records carry F_π, this
is not only a harness check: it is the proof that the recorded S4 posterior *is*
the F_π one. On S2, 20 of 20 at 0.000e+00. Identities are unique on every tick of
both fields, so the join is total.

**The arms**, F_π in against F_π out, per tick:

- **TV distance 0.01202**–0.01203.
- **133 of 145 posterior ranks move** (S2 counterfactual: 133–136), **max move 130
  positions**.
- **argmax UNCHANGED on every tick** — 3 of 3 on S4, 19 of 19 on the S2
  counterfactual.

**F_π spread 0.7540 against G spread 125.34** — about 1/166 — and it still moves 133
of 145 ranks. That is not a contradiction and it is the shape worth carrying: the G
spread is set by its tail (RUN7 recorded the dark posterior's argmax as the
machine's rank 134 of 145), so among the bulk of the field the candidates are
bunched in G and a 0.754-nat term reorders them freely. It cannot reach the top,
which is where the argmax lives.

The spreads print identically to four decimals on every tick of both runs. They are
**not frozen**: RUN7 established the G and F_π vectors are pairwise distinct across
ticks; the summary statistics agree to 4 dp while the vectors do not.

## 6. What F_π does NOT reach, checked rather than assumed

Re-ran RUN8's consumer audit for this row. `grep -rn ':softmax-weights'` over
`src` and `scripts`, excluding tests, `src/ants` (a different apparatus) and
`policy.clj` itself, gives **exactly one consumer: `trace.clj:175-178`**, which
re-keys the posterior by candidate id under the policy-details flag and otherwise
drops it. `cascade_prior.clj:189` calls `policy/softmax-weights` with its own
temperature over its own costs — a different decision, not this one.

And `:action` is not the posterior's: `war_machine.clj:5152-5155` replaces the
controller decision's `:action` with the strategic selector's pick, which is handed
only `:scheduler-habit-ranking` and a trace id (RUN8 measured the recorded
`:action` equal to controller rank 1 on 0 of 60 ticks).

So: **F_π is imported by the policy posterior — the quantity Joe's ruling named —
and the posterior it moves is recorded, not acted on.** That second half is a
property of the WM's selection boundary that predates F_π and applies to the whole
R6 row; it is not something S4 introduced, and it is not a reason to withhold the
retirement, because the standard in TN §1 makes an edge a matter of one equation
importing another's symbol, not of reaching actuation.

**A second ordering sits in the same decision map and is NOT B.9.**
`:habit-adjusted-ranking` and its `:counterfactual` winner are computed at
`policy.clj:360-362` from `ln E − G/τ` with no F_π. Recording this rather than
quietly extending F_π to a second site: B.9's π is the posterior, one seam, as I2
(b2)'s docstring requires; a reader who takes the counterfactual ordering and the
posterior as the same object will be wrong by 133 ranks. Whether the counterfactual
should follow is a question for I2, not a correction to make here.

## 7. The C row (`aif-equations.edn`)

Three changes, all code-backed, no ruling written and `:choices` untouched:

1. **`:free-energy` (R8, `:F`) → `:status :retired`**, with a `:retired` map naming
   Joe's condition, the run that discharges it, and — explicitly — what is *not*
   claimed: the scalar F is still computed and stored as `:variational-free-energy`
   on every record. This retires the row's standing as wiring, RUN3's
   `:ruling-retired` class. Unlike R5→R6 it **cannot be refuted by a route**,
   because no `:imports` list in the registry contains `:F`, so there is no edge to
   traverse. Removing the computation is a build row, not this correction.
2. **New `:policy-free-energy` row** at R8 defining `:F-pi`, `:ref :parr2022`,
   `:eq` B.9 p. 247, `:live-consumer :policy-posterior`,
   `:status :realised-flag-gated`, with the Gaussian form and the horizon-one
   retrospective reduction stated.
3. **`:policy-posterior` `:imports` gains `:F-pi`** and its formal line its third
   term. The old `:eq` note said "the registry form drops F_π (the WM scores
   policies by G only)"; that sentence is now false under the flag and was removed.

**Pointer drift corrected in passing**: the F row's `war_machine.clj:4450-4452` had
drifted onto an unrelated invariant-signal map and kept resolving because
`pointer_check` verifies line ranges, not content. The live site is 4913-4915.

### What this does to the generated figure — measured, not published

`gen_aif_dag.bb` run into a temp directory only (TN §9a: no regeneration into a
publish before review). Before → after:

- theory edges **18 → 20**; realised-undrawn **8 → 10**.
- The two new edges are **R4→R8** and **R8→R6**, both `:realised-undrawn`.
- Everything else identical: conformant 7, drawn 21, unexplained 6,
  not-realised 2, path-dependent 1.

R8→R6 is the edge that makes R8 stop being a sink. **The D row that draws it is
owed and is NOT in this row's scope** — RUN9's acceptance asks for a C row; I2's
acceptance asks for "a reviewed C row *and* D row". That D row is still open.

## 8. Gates

- `clj-kondo` 0 errors / 0 warnings on `policy.clj`, `war_machine.clj`, both test
  files, `run9_s4_preflight.clj`, `run9_s4_arms.clj`.
- `check-parens` exit 0 on all six.
- `clojure -X:test` over policy, policy-precision, war-machine, trace, habit-prior,
  operator-flip, run-tick-once, wm-run-lock, efe: **211 tests / 906 assertions,
  0 failures, 0 errors** (RUN8's nine namespaces at 202/865; +9 tests, +41
  assertions).
- `negative_controls.sh` PASS (16 negative, 10 positive).
- `pointer_check.bb` 268 pointers in 3 files, 0 unresolved.

## 9. Not done, stated rather than implied

- **Four ticks, not twenty.** Enough to exercise the wiring, the fail-closed path
  and the movement, and the movement is measured on 19 further ticks by replaying
  the S2 field. Not enough to say anything about F_π over a long run.
- **No conformance run** (`run3_conformance.bb`) against the S4 records. The route
  is unchanged by this row — F_π enters inside R6, adding no hop — but that is an
  argument, not a check, and the check is cheap. A reviewer who wants it should ask.
- **`:by-tau` was not exercised live** and is not reachable from the live path.
- **The default path is untouched**: with the flag off the recorded posterior is
  byte-for-byte the recomputation without F_π (control tick, max Δ 0.0).
- **No D row**, no figure regeneration, no publish.
- **The R8 scalar F is still computed.** §7 item 1.
