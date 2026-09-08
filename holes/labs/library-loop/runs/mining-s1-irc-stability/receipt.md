# Receipt: mining-s1-irc-stability (M1, cascade 1 of 6)

Target: the 1-th mapped item of runs/W1-witness-survey.md's mapped set in
survey order — **S1** (`runs/W1-witness-survey.md:37-51`): "reliable transport
must not silently lose coordination context", problem `leaf-5` scene
`irc-stability`, mapped to pattern `agent/handoff-preserves-context`.

## Why the cascade nodes differ from the survey's mapping

The survey's mapping (`agent/handoff-preserves-context`, grounded on
`problems/process-conduct-is-unassured`) maps the PROBLEM. This record mines
the SOLUTION, and its nodes are the patterns actually involved in what
happened, per the role spec. The mission's own plan
(futon3c/holes/missions/M-IRC-stability.md:91-98, committed at b1208d65,
2026-02-20) names five patterns; **three exist as library nodes at the futon3
pin** (liveness-heartbeats, listener-leases, loop-failure-signals) and two
(`realtime/connection-state-machine`, `realtime/reconnect-with-backoff`,
M-IRC-stability.md:94-95) **have no library file** — `grep -r` over
`futon3/library/` at pin ab277bdd returns nothing for either id. They are
recorded in `:admission-note`, not minted as nodes.

## What the witnesses say

- Problem: futon5a/holes/stories/leaf-5.md:31-35 (verbatim in the cascade).
- Closure bar left open: leaf-5.md:44-45 ("sustained use without drop-out
  incidents") — the record's `:scope` states it is NOT claimed.
- Solution witness: M-IRC-stability.md:302-310 (Completion 2026-02-23, all 6
  failure modes, 16 stability tests, irc.clj at 652 lines then), closed at
  futon3c 0f55ac8d.
- Code witnesses (all re-read at futon3c b0c3648965d3e75acf199b09967609353d9023ad,
  where irc.clj is 706 lines): keepalive loop `irc.clj:629-643`, `ping-idle!`
  `irc.clj:415-425`, reap `irc.clj:391-410` (keepalive-reap evidence at :404),
  `nick-ghost?`/`kill-ghost!` `irc.clj:219-235` and reclaim `:274-275`,
  `emit-error-evidence!` `irc.clj:45`, relay deadline `(deref f relay-timeout-ms ::timeout)`
  `irc.clj:509`, `SO_TIMEOUT` `irc.clj:652`; tests `irc_test.clj` section 11
  (`:409+`), relay-timeout tests `:580+,:610+`, 35 deftests total.

## Gate reproduction

```sh
bb -e '(clojure.edn/read-string (slurp "cascade.edn"))'   # EDN_OK, exit 0
grep -n "+ THEN:" library/realtime/{liveness-heartbeats,listener-leases,loop-failure-signals}.flexiarg
# all three at :18; rule :then-source cites :19 -- the content line ONLY,
# marker line excluded, per the exemplar-review convention
```

Every rule's `:interpretation-class` is `:attested`, each with the mission
span that attests the reading (F1 at :37-40, cross-ref rows at :93-97). No
interpretation is `:documented`-only: the mission's plan itself states how
each pattern constrains the fix.

## Honest limits

- Admission: zero admitted nodes; all three patterns appear in the plan
  BEFORE implementation (mission committed 2026-02-20, completion 2026-02-23),
  so `:admitted []` with the note, exactly as the exemplar did.
- The library flexiargs post-date the mission (committed 2026-05-04,
  futon3 86b57cb): the mission is the historical witness of involvement; the
  flexiargs are the library's later formalization the record cites for THEN
  spans. This is stated here, not smoothed.
- futon5a pin (corrected on reopen): leaf-5.md last touched at futon5a
  9fc6f96b11ed61749fe09cc8a5e07724c2e0f428 (2026-05-03); pinned exactly in
  :source-pins.
- Trail status: the searches above are the complete commands run; no
  truncation anywhere (`grep -rn` over library/ returned the three hits and
  the two misses shown; `grep -n` over irc.clj/irc_test.clj returned every
  line cited).
