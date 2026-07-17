# Learning ant brain POC

The `:learning` brain keeps a small policy cascade on each ant. Conditions are
explicit bins (`:has-cargo`, `:food-here`, `:food-nearby`,
`:at-water-edge`, and `:else`) and values are existing ant actions plus
`:turn-back`. After each action, the same ant reviews the result. Productive or
safe movement preserves the active entry; drowning, starvation, or zero
progress revises only that entry.

Run the deterministic river comparison with:

```sh
clojure -M -m ants.learning-experiment
```

The report gives windowed water-death rates for separate `:classic` and
`:learning` colonies on the same map and seed, followed by sample learned
cascades.

## Follow-on: collective bridge pattern (not part of this POC)

A future experiment could add a `:grip-hold` action and stigmergic review: an ant
holding at a water edge receives shared forage credit when its presence lets
other ants cross. The open question is whether this distributed credit can
stabilise a collective bridge fixed point. This is deliberately not implemented
here; it is a harder emergent-pattern test after individual enactment-and-review
has cleared water avoidance.
