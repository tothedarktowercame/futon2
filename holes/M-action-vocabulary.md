# M-action-vocabulary — price all the move classes the operator actually values

**Status: IDENTIFY (2026-07-05, operator-directed). Owner: TBD.
Ratification: Joe. Lineage: futonzero-alphazero.md §5 (contest v1 +
golden round 2 — the finding this mission discharges);
E-cascade-sampler-sampler (parked on exactly this precondition).**

## HEAD

The deepest measured negative in the FutonZero record (2026-06-12,
golden round 2, sealed deck): **the operator's picks were mis-ranked
not just by the proxy but by the realized-G floor itself** — operator
gold is *portfolio-shaped* ("quick wins and some deeper dives" +
close-outs of saturated missions), while **every score in the stack
prices only the ADVANCE move class.** Four independent
single-best-holds results trace to this: the contest could not be won
because the scoreboard could not see what winning meant.

## IDENTIFY — the tension

The WM's action vocabulary is richer than its value function: advance,
close, survey, (and since 2026-07-05: apply-cascade with escrowed ΔG)
— but g(s) prices advancement only. The named fix from the contest
record: **move-class-conditional intensity** — advance ×
(1−resolvedness); close × resolvedness; survey for stale-early targets
— **plus a composition-aware bundle score** (a portfolio of picks
valued as a portfolio, not a sum of solo scores).

## Completion criteria

- P1: the move-class value spec — per-class intensity formulas over
  the substrate-metric quantities (resolvedness, staleness, κ/W1),
  written as a logic model before code (house discipline).
- P2: implemented in the ranking lane BEHIND A FLAG, dark, with the
  Flight-1/S1 regression controls re-run flag-off (byte-stage-named
  identical per the ledger lesson).
- P3: the golden-selections re-test — rounds 1–2's sealed operator
  picks re-scored under the new vocabulary. Falsifiable target: the
  operator's picks rank materially higher (top-quartile median) than
  under the advance-only score. If they don't, the portfolio structure
  is not where we think it is — that negative goes in the record.
- E1: E-cascade-sampler-sampler contest v2 is UNBLOCKED (its verdict
  file updated to point here as the discharged precondition).

## Scope out

- No sampler training in this mission (that's the contest's job once
  unblocked); no live flag-flip without operator word.

## P2 dark implementation log — move-class value spec (codex-1, 2026-07-05)

**Status: P2 LANDED DARK.** Plan of record: deposit
`futon6/data/fold-turns/ft-action-vocabulary-005.edn`; its wiring boxes
b1-b17 are the P1 logic model. Implementation flag:
`FUTON_WM_MOVE_CLASS_INTENSITY=v1`. Default is `:off`; no live flag flip
was performed.

Chosen v1 forms, deliberately simple:

- `resolvedness`: explicit `:resolvedness` /
  `:resolution-state/resolvedness` when present; otherwise mission-doc
  `:open-hole-count` maps to `1/(1+N)`. Zero visible holes is saturated
  (`1.0`); many holes is early/open. Missing signal = `0.0`.
- `staleness`: explicit `:staleness` when present; otherwise
  `:staleness-days` or mtime age maps to `days/(days+7)`. Seven days is
  the half-life; no stale signal = `0.0`.
- `kappa` / `W1`: explicit `:kappa`, `:min-kappa`,
  `:curvature/min-incident-kappa`, and `:W1` / `:w1`; today's mission
  action `:weight` is the W1 carrier. Missing kappa/W1 default to `1.0`
  so the dark path remains observable before the richer substrate lands.
- `advance-intensity = kappa * W1 * (1 - resolvedness)`.
- `close-intensity = kappa * W1 * resolvedness`.
- `survey-intensity = kappa * W1 * staleness * (1 - resolvedness)`.
- `apply-cascade-intensity = max(0, -escrowed-dG)`, reading the pinned
  fold/deposit coverage-dG from action metadata.

Bundle merge, v1: a portfolio is not a plain sum. Within each class,
same-class repeats get diminishing returns (`v/(rank+1)` after sorting
by value); mixed portfolios earn a small diversity multiplier
(`+0.1` per extra class, capped at `+0.3`); optional budget overrun is
subtracted. This keeps currency discipline visible without fitting an
exchange-rate table before P3.

Code path: `futon2.aif.move-class-intensity` holds the pure formulas and
bundle scorer. `efe/compute-efe` subtracts the signed contribution from
`:G-total` only when `:move-class-intensity-mode :v1` is explicitly
threaded; flag-off uses the historical `G-total` expression and emits
no move-class keys. `war_machine.clj` resolves the environment flag and
records the mode in `arena-mode-flags`. Trace whitelisting includes the
dark contribution at birth so any future v1 run is auditable.

Regression notes: gamma feed was not touched. The close-loop test suite
had a stale pre-L4 expectation for classical fold dG fallback; the test
now binds `*classical-fold-dG?*` when it wants the old fallback and
asserts the L4 default abstention otherwise. Production close-loop code
was not changed.

## Kill criteria

- P3 negative twice (two vocabulary revisions, sealed picks still
  mis-ranked) → the operator's value structure is not expressible at
  this grain; stop, and the finding feeds the Joe-HUD/Bayesian-model
  thread instead.
