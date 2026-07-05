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

## Kill criteria

- P3 negative twice (two vocabulary revisions, sealed picks still
  mis-ranked) → the operator's value structure is not expressible at
  this grain; stop, and the finding feeds the Joe-HUD/Bayesian-model
  thread instead.
