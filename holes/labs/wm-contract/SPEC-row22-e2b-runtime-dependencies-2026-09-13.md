# Row 22 E2b remaining runtime dependencies

The pure verifier establishes only a retained post-event equality: one selected
approved occurrence and its exact action bytes equal the enacted occurrence and
action bytes. It does not make either event happen.

Before runtime integration can qualify E2b, the live tick must retain:

1. independently configured production E1/E2a authority and the complete
   approved occurrence domain;
2. an R6 scoring/posterior witness over exactly that full ordered domain, joined
   to the selected occurrence—this packet keeps it explicitly
   `:external-dependency` and does not waive the remaining R6 law;
3. an R9 independent-review authorization for the exact selected occurrence,
   consumed before enactment—post-event equality cannot supply it afterward;
4. an R16 enactment record carrying the selected occurrence and exact enacted
   occurrence/action under the same model, run, tick, and cohort; and
5. a refusal at the live first-passing boundary when its candidate differs from
   the selected approved occurrence, even if the semantic action maps compare
   equal.

No generic divergence is admitted. A future typed divergence requires its own
adopted reason vocabulary and independently pinned authority tied to the exact
subject. Until then mismatch refuses. Production witness authority is currently
unavailable, and this packet makes no selector or enactment source change.
