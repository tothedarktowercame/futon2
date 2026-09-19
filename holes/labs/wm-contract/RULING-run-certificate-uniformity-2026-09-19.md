# Ruling: uniform certificate requirement for every run — Joe, 2026-09-19

Recorded by claude-12 from Joe's ruling (operator, emacs-repl, 2026-09-19),
settling the realness-criterion question raised by the reproduction run
(`holes/labs/wm-contract/runs/reproduction-2026-09-19/README.md`).

## The ruling

> All runs of the machine should carry the same certificate burden,
> including self-repair. Obviously external repairs that aren't based on a
> run do not have to produce these witnesses, because that would be a
> fabrication. I don't see any reason why we wouldn't treat every run of
> the machine as "a run of the machine" and require that it be valid.

And on cost: the "burden" phrasing is only meaningful if certificates are
heavy to produce; if they are, that is an independent problem — not a
reason to exempt runs. (Measured today they are not heavy: the fields are
values the tick already computes; writing them is a per-decision EDN
append. The heavy step, Lean re-elaboration, is off-run checking.)

## What follows

1. **Every run — ordinary click, self-repair click, any gated run — writes
   the same validity record.** The current state where a repair click
   leaves `traceWritten false` and its quantities exist only in scattered
   checkpoint files is a defect against this ruling, not a category
   difference. (The separate exclusion of repair actions from HABIT/
   learning accounting is untouched by this ruling — that is about what
   the machine learns from, not about whether the run was valid.)
2. **External repairs carry no run-witnesses.** Work done outside a run
   (an agent editing code directly) must not produce run-shaped evidence;
   that is the discharge contract's existing no-fabrication line applied
   in reverse.
3. **The realness v2 criterion** therefore reads the per-run record every
   run writes, and requires on it: C source (:derived vs fallback),
   outcome-rate provenance (measured vs identity), the cascade posterior
   (with per-policy F when WIRE-f-on-tick lands), the U37 verdict with
   membership diff, and the recorded G-term decomposition (so the
   AGG-single-kl-reduction degeneracy measurement can read the corpus).
   Evidence, not a gate: a run missing fields is reported invalid, never
   halted (per the addendum to T-wm-excessive-guardrails-19092026.md).

Sequencing of the implementation is claude-4's as build lead.
