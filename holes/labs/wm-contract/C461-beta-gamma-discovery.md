# C461 discovery: Friston beta/gamma dynamics in the War Machine

This is a read-only design audit for worklist item I1(a). It describes existing
quantities and possible state placement; it does not treat the current selection
gain as variational policy precision. The selection-gain namespace itself says
that its scalar is engineering commitment control learned from realized versus
expected controller performance, not Friston's precision learned from expected
free energy under a policy posterior
(`src/futon2/aif/selection_gain.clj:1-11`).

## 1. The paper equations and symbols

Friston et al. 2017 prints the inference part of equation 2.7 across printed
pages 13–14 as:

> `π = σ(−F − γ · G)`<br>
> `β = β + (π − π₀) · G`

and immediately defines `γ = 1/β` and `π₀ = σ(−γ · G)` (Friston et al. 2017,
printed pp. 13–14, eq. 2.7 and the paragraph immediately following it). The
paper says that the equalities in 2.7 are normally iterated until convergence
(Friston et al. 2017, printed p. 14, paragraph following eq. 2.7).

At the grain relevant here:

- `π` is the posterior probability vector over policies; the displayed update
  includes both policy-conditioned variational free energy `F` and expected
  free energy `G` (Friston et al. 2017, printed p. 13, eq. 2.7).
- `π₀` is the expected-free-energy-only policy distribution
  `σ(−γ · G)`, before the policy-conditioned evidence term `F` is included
  (Friston et al. 2017, printed p. 14, definition following eq. 2.7).
- `G` is the vector of expected free energies, one entry per policy (Friston
  et al. 2017, printed p. 13, eq. 2.7; the paper introduces `G(π)` as expected
  free energy on printed p. 7, eq. 2.1).
- The updated `β` is the posterior rate/temperature statistic for the gamma
  belief over precision; the right-hand `β` is its prior rate plus the scalar
  prediction-error term `(π − π₀) · G` (Friston et al. 2017, printed pp. 6 and
  13–14, eqs. 2.1 and 2.7).
- `γ` is expected policy precision, the inverse of the updated temperature/rate
  statistic: `γ = 1/β` (Friston et al. 2017, printed p. 14, definition following
  eq. 2.7).

Da Costa et al. 2020 Appendix A.2 does **not** state the beta update from
Friston equation 2.7. Its exact claim is that the simple policy distribution
`σ(−G(π))` can be extended to `σ(−γG(π))`, where `γ` is an inverse-temperature
parameter expressing confidence in policy selection; it refers the reader to
another source for the associated belief update (Da Costa et al. 2020, printed
p. 24, Appendix A.2, unnumbered paragraph). Thus it supports the role of
`γ` in policy selection, but it is not an independent statement of
`β ← β + (π − π₀) · G` (Da Costa et al. 2020, printed p. 24, Appendix A.2,
unnumbered paragraph).

## 2. Mapping the symbols to quantities that exist today

### `π`: partial confirmation — `:softmax-weights`

`policy/softmax-weights` returns a normalized probability vector over the
candidate-aligned `g-totals`; its three-argument form scores each candidate as
`−G/τ + log-prior` (`src/futon2/aif/policy.clj:82-104`).
`strategic-recommendation` records that vector as a map from each action to its
weight under the exact key `:softmax-weights`
(`src/futon2/aif/policy.clj:234-246`, `src/futon2/aif/policy.clj:296-298`).
This is the existing same-grain candidate for `π`.

It is not yet Friston's `π` exactly: the WM uses engineering `τ` and an
unscaled habit log-prior, while equation 2.7 uses `−F − γG`; the
`effective-temperature` documentation explicitly says both current modes are
engineering calibration and that selection gain is not variational precision
(`src/futon2/aif/policy.clj:46-70`). In particular, the WM does not provide a
policy-conditioned `F` vector at this selection site
(`src/futon2/aif/policy.clj:240-246`).

### `π₀`: refutation — `log-priors` is not this quantity

The `log-priors` argument is the existing habit-prior seam: it aligns with
`g-totals`, is added to the selection score unscaled by `τ`, and is documented
as the future site for a real `ln E(π)`
(`src/futon2/aif/policy.clj:82-100`). That makes it a log prior over policies,
not Friston's probability vector `π₀ = σ(−γG)` (Friston et al. 2017, printed
p. 14, definition following eq. 2.7).

Therefore the proposed `π₀ → log-priors` mapping is false. No distinct WM field
currently stores the expected-free-energy-only probability vector at the same
candidate grain. Calling `softmax-weights` without habit priors would still use
the engineering `τ` path, not a persisted formal `γ = 1/β`
(`src/futon2/aif/policy.clj:93-104`; `src/futon2/aif/policy.clj:46-80`).

### `G`: confirmation at the candidate grain — `:controller-score`

`efe/rank-actions` applies `compute-efe` to each included candidate, sorts by
`:controller-score`, and returns every scored candidate with a rank
(`src/futon2/aif/efe.clj:903-921`). `strategic-recommendation` constructs its
`g-totals` from those scores and records the chosen value under the exact key
`:controller-score` (`src/futon2/aif/policy.clj:240-255`,
`src/futon2/aif/policy.clj:259-264`). Thus `:controller-score` is the existing
per-candidate quantity to place in the `G` vector. This confirms the mapping at
the WM's action-candidate grain; it does not by itself make the controller's
engineering approximation identical to every term in the paper's generative
model.

### `β` and `γ`: no present formal counterparts

There is no persisted Friston `β` rate statistic and no derived formal
`γ = 1/β` today. The closest scalar, `:selection-gain`, is explicitly described
as learned from realized-vs-expected performance rather than
`(π − π₀) · G` (`src/futon2/aif/selection_gain.clj:6-22`). Its state contains
`:selection-gain`, performance history, mean performance, and sample count,
not `β`, `π`, or `π₀` (`src/futon2/aif/selection_gain.clj:59-64`,
`src/futon2/aif/selection_gain.clj:99-107`).

## 3. Where beta could persist across ticks

The smallest change is a sibling trace-state field following the existing
selection-gain lifecycle. At the start of a tick, the judge reads
`:selection-gain` from `prev-trace-record`, defaults it, and schema-coerces it
(`scripts/futon2/report/war_machine.clj:4332-4343`). It folds the previous
tick's realized outcome and extracts the scalar used this tick
(`scripts/futon2/report/war_machine.clj:4344-4361`). The updated state is then
written into the new trace record under `:selection-gain`, explicitly so the
next tick continues it (`scripts/futon2/report/war_machine.clj:4773-4776`). A
new, separately named beta state could use the same read/coerce/update/write
shape without changing the meaning of `:selection-gain`.

The anticipation snapshot is a worse home. It is freshly obtained from
`anticipation/anticipation-snapshot` during the tick and inserted into the WM
state (`scripts/futon2/report/war_machine.clj:4453-4462`), then copied into the
trace record as `:anticipation` (`scripts/futon2/report/war_machine.clj:4782-4784`).
This site exposes upcoming typed events; it does not read the previous trace's
anticipation value as inference state (`scripts/futon2/report/war_machine.clj:4453-4456`).
Putting beta there would require changing the anticipation subsystem's meaning
and adding a previous-tick feedback path, whereas a trace-state sibling needs
only the persistence pattern already used by selection gain. This comparison
is about storage mechanics only: the beta update signal and formal quantities
listed in section 5 still have to be implemented honestly.

## 4. Naming collision

`FUTON_WM_TAU_MODE` already has two effective values: the literal environment
value `spread` selects `:spread`; every other value selects
`:selection-gain-only` (`scripts/futon2/report/war_machine.clj:238-248`). The
temperature function then uses a closed two-branch `case` and throws for any
mode other than `:spread` or `:selection-gain-only`
(`src/futon2/aif/policy.clj:72-80`). Consequently, adding a new mode requires
changes at both parsing and dispatch; merely setting a new environment value
currently still produces `:selection-gain-only` at the parser.

There is also a semantic collision with `:gamma-only`: the shadow script still
uses that stale label for the existing `1 / selection-gain` mode
(`scripts/dark_mode_shadow.bb:9-13`, `scripts/dark_mode_shadow.bb:144-151`).
The live name for that behavior is now `:selection-gain-only`, and both are
engineering calibration, not Friston gamma
(`src/futon2/aif/policy.clj:58-70`). Reusing `:gamma-only` for formal
`γ = 1/β` would make old reports and the new dynamics read as the same mode.

Proposed unambiguous mode name: **`:variational-beta-gamma`**, with environment
value `variational-beta-gamma`. It names the coupled update rather than the old
selection-gain reciprocal. Any implementation should also correct the stale
shadow labels instead of accepting them as aliases; today those labels occur at
`scripts/dark_mode_shadow.bb:9-13` and `scripts/dark_mode_shadow.bb:149-151`.

## 5. Quantities and operations missing today

Equation 2.7 cannot currently be evaluated from WM state because all of the
following are absent:

1. A formal persisted `β` prior/posterior rate statistic at policy-selection
   grain. The persisted scalar state today is the differently learned
   `:selection-gain` (`src/futon2/aif/selection_gain.clj:6-22`,
   `scripts/futon2/report/war_machine.clj:4773-4776`).
2. A formal `γ = 1/β` derived from that beta and supplied to policy selection.
   Current `effective-temperature` instead divides an engineering temperature
   by selection gain (`src/futon2/aif/policy.clj:46-80`).
3. A distinct candidate-aligned `π₀ = σ(−γG)` probability vector. The existing
   `log-priors` are `ln E(π)` habit-prior inputs, not `π₀`
   (`src/futon2/aif/policy.clj:82-100`; Friston et al. 2017, printed p. 14,
   definition following eq. 2.7).
4. The formal posterior `π = σ(−F − γG)`. `:softmax-weights` is the same-grain
   observable candidate, but its implemented score is `−G/τ + log-prior`
   (`src/futon2/aif/policy.clj:82-104`).
5. A policy-conditioned variational-free-energy vector `F`, aligned one-to-one
   with candidates. The WM computes one aggregate
   `variational-free-energy` from prediction errors before action ranking
   (`scripts/futon2/report/war_machine.clj:4450-4452`), while the selection
   score consumes only `g-totals` and `log-priors`
   (`src/futon2/aif/policy.clj:240-246`).
6. The scalar dot product `(π − π₀) · G` and the associated beta update. The
   present selection-gain learner instead folds a realized-performance ratio
   (`src/futon2/aif/selection_gain.clj:17-22`,
   `src/futon2/aif/selection_gain.clj:124-160`).
7. An iteration/convergence rule for the mutually dependent `π`, `π₀`, `β`,
   and `γ`. Friston says equation 2.7 is normally iterated until convergence
   (Friston et al. 2017, printed p. 14, paragraph following eq. 2.7); the WM's
   current softmax is a single pure calculation from fixed `τ`, scores, and
   log priors (`src/futon2/aif/policy.clj:93-104`).

These absences mean beta/gamma dynamics require new formal state and update
inputs. They cannot be obtained by renaming `:selection-gain`, `log-priors`, or
the current `:softmax-weights`.

## 6. Owner review (claude-20, 2026-09-01)

Checked against the sources rather than taken on trust.

**§1 is verbatim correct.** Eq. 2.7's inference block reads
`π = σ(−F − γ · G)` and `β = β + (π − π₀) · G` at `friston2017.txt:660-666`,
and the paragraph immediately after (`:683-685`) gives `γ = 1/β`,
`π₀ = σ(−γ · G)`, and "usually one would iterate the equalities in equation 2.7
until convergence". The page break at 13/14 is where the report says it is.

**The Da Costa refutation is correct and matters for the packet that sent it.**
A.2 (`dacosta2020.txt:1262-1271`) says only that `σ(−G(π))` can be extended to
`σ(−γG(π))` with γ an inverse temperature, and closes "We refer the reader to
[44] for a discussion of the associated belief updating scheme." So A.2 supports
γ's role in selection and explicitly does NOT state the β update. I1's framing
cited Da Costa A.2 alongside Friston eq. 2.7 as if both gave the update; one
does.

**The π₀ refutation is right, and it refutes my own packet.** I proposed
`π₀ → log-priors`. π₀ is `σ(−γ · G)`, the EFE-only policy distribution
(`friston2017.txt:684`); `log-priors` is the habit seam `ln E(π)`
(`policy.clj:82-100`). They are different objects at the same grain, and no WM
field holds π₀ today.

### The finding neither the report nor the packet drew: I1 depends on I2

The `F` in `π = σ(−F − γ · G)` is not the WM's aggregate channel scalar. Friston's
notation table defines it as `F : F_π = F(π) = Σ_τ F(π,τ) ∈ ℝ`, "Variational free
energy for each policy" (`friston2017.txt:291`). That is the same object as Parr
2022 B.4 — the per-policy F_π that item **I2** exists to compute (C462 §1).

So the β update needs π; π needs F_π; F_π is I2. I1 as written cannot be
implemented faithfully before I2 lands. The report names the absence (§5 item 5)
but treats it as one missing input among seven; it is the one that orders the two
items.

Three ways forward, and the choice is Joe's, not mine — opened as **J3**:
(a) sequence I2 before I1 and implement 2.7 whole;
(b) implement β against an F-free π (`σ(ln E − γG)`), a declared deviation that
    makes the β update measure something eq. 2.7 does not define;
(c) land the shared trace write seam first as one handoff, then I2, then I1.

### Both items need the same trace write, at two sites

C462 §2 found that `strip-ranked-action` (`trace.clj:76-127`) drops
`:prediction`, so no per-candidate prediction survives a tick. The sibling
`strip-decision` (`trace.clj:128-137`) drops `:softmax-weights`, so π does not
survive either. §3 of this report picks the right home for β — the selection-gain
read/coerce/write lifecycle at `war_machine.clj:4332-4361` and `:4773-4776` — but
β is not the only thing that has to persist: the update also needs π and π₀ at
the tick they were formed. That is a second write at the decision-strip site.
Section 3's comparison against the anticipation snapshot is sound as far as it
goes; it answers where β lives, not where π does.

**§4 accepted.** The collision is real: `arena-tau-mode`
(`war_machine.clj:238-248`) maps every non-`spread` value to
`:selection-gain-only`, so a new environment value silently produces the old mode
at the parser, and `effective-temperature` (`policy.clj:72-80`) throws on any
third keyword — both parser and dispatch must change together. The stale
`:gamma-only` labels are at `dark_mode_shadow.bb:9-13` and `:144-151`.
`:variational-beta-gamma` is a good name; adopting it, and correcting the stale
shadow labels in the same slice rather than aliasing them.

`pointer_check.bb`: 180 pointers in 3 files, 0 unresolved. No `src/` or
`scripts/` modification from this job.
