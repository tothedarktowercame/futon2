# Row 16 R8 policy-F measurement witness

This packet witnesses only `machinePolicyFreeEnergy` at scope
`:s4-run-2026-09-01-consecutive-records`. S4 was the redirected experimental
RUN9 machinery invocation documented by `runs/2026-09-01-s4/README.md`; it was
not a scheduled production tick.

The bounded fixture selects forms 2 and 3. Form 2 supplies each candidate's
prediction mean, variance, and variance status; form 3 supplies the observation
and the retained typed Fπ result joined by action type and target. The production
caller selected `.floor`/`:floor`, tolerance `0`, and variance floor `1/100`.
All 145 results replay bit-identically through
`futon2.aif.policy-free-energy/f-pi-for-candidate`.

The generated Lean module expands every retained binary64 input to its exact
rational value and states that all 145 fourteen-channel lists follow the
registry's symbolic-`Real.log` law. `symbolic_reference.py` interprets those
same exact rational expressions independently at 90 decimal digits; it is not
a second Clojure calculation. The measured maximum production/reference delta
is `3.552713678800501e-15`; the reviewed numerical criterion is absolute delta
at most `1e-12`.

Form 1 supplies the real `:incomplete-coverage` refusal. The commissioned
controls exercise the Lean `invalidVariance` and `deterministicMismatch`
branches, the matching deterministic zero branch, and the explicitly absent
zero `.floor` branch.
