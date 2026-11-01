# L5-16 — Stopping stays a competitor on the outer, at a ruled altitude (claude-2 → codex-5, 2026-11-01)

Supersedes L5-16-L (withdrawn; its Lean half `2c8877d31` on `l5-16-outer-stopping` is not merged and
must not be). Authority: `reconciliation/RULING-outer-stopping-is-not-a-competitor-2026-11-01.md`,
Refinement section (Joe, verbatim). Base futon2 `main` (≥ this packet). Worktree only. **No Lean
change.** Q11 keeps the abstention comparison; the outer never prefers stopping because stopping is
expensive, not because it is absent.

## Behaviour

1. **Altitude resource.** `resources/wm/outer-abstention-altitude.edn`:
   `{:schema :wm/outer-abstention-altitude-v1 :nats 10 :authority "Joe, 2026-11-01: the machine
   runs at 10,000 feet or so above stopping level; lowering this to 0 is 'let\'s pause'"
   :authorized-at "2026-11-01T20:55Z"}`. The number is Joe's empirical parameter; 10 nats is the
   first value (work G today 2.8–3.4 nats, stopping's risk 3.28; the exact figure is his to change
   by editing the file). `resources/wm/*.edn` is matched by an ignore rule: `git add -f`.
2. **Abstention score.** `outer_score/abstention-score` adds the altitude: `G = risk + ambiguity −
   novelty + altitude`, with `:terms` gaining `:altitude` and `:altitude-source {:path :sha256
   :nats}` pinned on the abstention carrier. A missing or malformed resource refuses typed
   (`:abstention-altitude-unconfigured`), never defaults to 0: a zero altitude is a decision
   (pause), not an absence.
3. **Stopping-equivalent classes** (L5-11): a class whose prediction equals the abstention's is
   stopping under another name and carries the same altitude in its G, so it is compared (undo
   L5-11's exclusion from the comparison; keep `:stopping-equivalent` as the diagnostic list) and
   loses provably. Lean's `predictsOtherThanStopping` then needs the class's prediction to differ
   from the abstention's: say in the report whether the altitude should instead be modelled as a
   distinct predicted outcome (e.g. the abstention predicting `[:stop-the-line :none]` while a
   stopping-equivalent class predicts the same) and, if Lean's gate would still fail, keep L5-11's
   exclusion and say so; do not change Lean.
4. **Refusal kept.** `:pick-not-below-abstention` stays on the outer: with the altitude it fires
   only when every class's G exceeds risk-of-stopping + altitude, which is a real alarm. The loop's
   repair-9 rule (wait once, then pause) stays as is.
5. **Record and export.** The selection record's `:abstention` carries the breakdown and the
   altitude pin; the exporter's `abstentionG` is the total (what Lean compares); a new diagnostic
   `:abstentionTerms` carries the breakdown. Records before this change export as before.
6. **Click 12's record re-selected** on its retained observation picks
   M-interim-director-proxy-metric-inventory (G 3.3896) against abstention 13.2782; Q11 holds in
   the checker.

## Tests

- Altitude read, pinned, added; missing resource refuses typed; altitude 0 in a fixture makes
  the abstention competitive again (the pause case) and the refusal fires honestly.
- Click 12 re-selection as in 6; a fixture where every class predicts stopping compares them with
  the altitude and refuses `:pick-not-below-abstention` only if their G exceeds the abstention's.
- Named, outer, projector namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex5/l5-16`. Bell claude-2 with sha,
counts, click 12's re-selection and export.
