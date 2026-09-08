# Receipt: mining-s1-irc-stability (M1, cascade 1 of 6 — second pass under the restated edge term)

Target: the 1-th mapped item of runs/W1-witness-survey.md's mapped set in
survey order — **S1** (`runs/W1-witness-survey.md:37-51`): "reliable transport
must not silently lose coordination context", problem `leaf-5` scene
`irc-stability`.

## What changed on the second pass

The first pass was reopened (finding 1: edges not @why/@how-backed; finding 2:
the loop-failure-signals rule reading unattested; finding 3: futon5a pin —
already fixed at futon2 5cff84a4). The blocker is now RESOLVED from the
commission's own text (claude-1, worklist :blocker-history 2026-09-08): the
commission provides the attested-or-documented class
(MINING-historical-cascades.md:29-30) and the ratified L2 exemplar already
used mission-span-cited edges (runs/mining-exemplar/receipt.md:63-66). Under
the restated statement this pass:

- **Edges carry `:interpretation-class :documented` in-band**, each with a
  `:documentation` field naming the mission spans that author the reading and
  the measured annotation-absence it rests on (grep over library/realtime at
  pin ab277bdd: no @why/@how on any of the three nodes).
- **Finding 2 honoured, not overridden**: the `surface-loop-failures-as-evidence`
  rule is reclassified `:attested -> :documented`, with the rule text itself
  stating the narrower implemented reading (exception-surfacing) versus the
  THEN's own terms (overload/dropped-lines with tuning/backpressure), and the
  bridge named as the mission's reading, not the THEN's claim.
- **`:transcript` added** (finding 3 of the M3 review, generalised): the
  committed history futon3c b1208d65 (2026-02-15, plan with the pattern
  cross-reference table) -> F1-F6 implementation -> 0f55ac8d (2026-02-23,
  close), plus the post-close transport commits 1530126f (2026-02-24) and
  29d18a9a (2026-03-05), dated AFTER the close and recorded as follow-on
  work, not predecessors (transcript corrected per reopen review 2026-09-08). All shas/dates from git at pin
  f3534b93, re-run this invocation.
- THEN spans remain content lines only (`:19`; marker `+ THEN:` at `:18`
  excluded) — verified by grep at pin ab277bdd.

## What did not change

3 selected nodes (the mission-named realtime trio that exists in the library),
`:admitted []` with the honest note, the two pattern references with no library
node recorded rather than minted, all code witnesses, and the leaf-5 closure
bar still not claimed.

## Gate reproduction

```sh
bb -e '(clojure.edn/read-string (slurp "cascade.edn"))'   # exit 0
grep -n "+ THEN:" library/realtime/{liveness-heartbeats,listener-leases,loop-failure-signals}.flexiarg   # all :18
git log --oneline -- holes/missions/M-IRC-stability.md src/futon3c/transport/irc.clj   # at f3534b93
```

Trail status: the searches above are the complete commands run; no truncation
(`grep -rn` over library/ for the two absent ids returned the three hits and
two misses shown in the first-pass receipt; this pass re-ran the THEN grep and
the git log only, both complete).
