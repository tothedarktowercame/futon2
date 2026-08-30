# R14 → R5, round 1

## What I am

I am the policy-outcome commitment calibration in SELECT. I carry a bounded
gain, divide the spread temperature by it, and attach both to the explanation
(`src/futon2/aif/policy.clj:46`, `src/futon2/aif/policy.clj:193`). The live arena
passes R5's admitted ranked entries and my gain directly into
`policy/select-action` (`scripts/futon2/report/war_machine.clj:4513`).

I do **not** currently make the live strategic choice temperature-sensitive.
That branch calculates `scores`, then chooses the first non-no-op entry from the
already-ranked R5 vector; its temperature-dependent order is counterfactual
(`src/futon2/aif/policy.clj:242`, `src/futon2/aif/policy.clj:247`).

## How I need to develop

1. Specify my R5 input as a disjoint delivery: either a non-empty admitted
   ranked vector with controller-score provenance, or an explicit no-support /
   scoring-failure reason. What prevents this: today I receive a bare vector;
   empty input silently becomes `:abstain :no-candidates`
   (`src/futon2/aif/policy.clj:360`).
2. State which observable is commitment. If it is the chosen action, the live
   branch must make choice depend on temperature; if it is distribution
   sharpness, that distribution needs a downstream consumer and receipt. What
   prevents this: the live branch chooses before its counterfactual ordering
   (`src/futon2/aif/policy.clj:249`).
3. Emit a sensitivity witness at birth: the same delivered score field under
   at least two admissible gains, naming whether winner, abstention, entropy, or
   nothing changes. What prevents this: the decision explanation records spans
   and `:governed-by`, but no gain counterfactual
   (`src/futon2/aif/policy.clj:209`).
4. Preserve score identity through outcome feedback. What prevents this: my
   learner accepts expected and realized scalars plus a policy id, while the
   R5→R14 call has no delivery id or receipt tying the score field to the later
   outcome (`src/futon2/aif/selection_gain.clj:173`).

## What I need from R5

- A tagged score-field payload: success carries stable candidate identity,
  rank, controller score, decomposition/provenance, and support exclusions;
  failure carries one reason and no score. R5 already orders ascending and
  retains exclusions as vector metadata (`src/futon2/aif/efe.clj:844`).
- A guarantee that every delivered numeric controller score was computed under
  one named scoring configuration and is finite. I see the aggregate, but I
  cannot certify R5's inputs or scoring authority from my consumer boundary.
- An idempotency key binding tick, candidate field, and scoring configuration.
  This is a proposal; the current direct function call supplies no such key.

## What I can give R5

- The exact gain, `tau-spread`, `tau-effective`, scaled score, and whether G or
  a habit prior governed the counterfactual order
  (`src/futon2/aif/policy.clj:209`).
- A bounded learner: gain stays `1.0` through burn-in and then derives from a
  rolling mean, clamped to `[0.5, 2.0]`
  (`src/futon2/aif/selection_gain.clj:124`).
- An honest receipt saying whether R5's ordering selected the live action or
  was only evaluated counterfactually. Today the honest value is
  `:habit-authority :counterfactual-only` (`src/futon2/aif/policy.clj:270`).
