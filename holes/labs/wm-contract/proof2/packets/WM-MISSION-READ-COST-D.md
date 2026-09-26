# WM-MISSION-READ-COST-D — how many 9-to-42-second mission reads a selection makes

Follow-on to `WM-REGISTRY-READ-D.md` J6. Read at futon2 `a41f4c31` and futon1b `ac3f83b`; read-only —
no code change, no write under `data/`, no flight, nothing loaded into :6768, :7070 or futon1b's JVM.
Executed: the cache-behaviour count of §2 in a `clojure -M` JVM of my own, and the futon1b timings of §3
by curl. Sizing for two owners; nothing proposed as a change here.

## 1. Every path from the click to a substrate mission read

| caller | form | cached? | reads |
|---|---|---|---|
| `war_machine.clj:7219`, inside `declared-sources` (guarded `(when-not (:cascade-sources judge-opts) …)`, `:7213`) | `(mission-registry/load-missions)` | **no** | 1 per selection |
| `cascade_problems.clj:45` `substrate-targets`, called from `war_machine.clj:7241` | `(registry/open-missions)` zero-arg → `load-missions` `:458-465` | **no** | 1 per selection |
| `full_loop_runner.clj:1451-1453` `mission-entry` ← `mission-for-decision` `:1735` ← `:4943`, in the **`:construction`** phase (skipped when `:mission-fn` is supplied) | `(missions/open-missions)` zero-arg | **no** | 1 per click, not in selection |
| `controller_authority.clj:34`, the per-action guardrail predicate | `(missions/mission-status …)` → `load-missions-cached` `:536` | cached path | 1 per call — see §2 |
| `mission_registry.clj:646` `work-target-status`; `actuator_a3.clj:822` | `mission-status` | cached path | 1 per call |
| `interpretation_request.clj:29` `resolve-target` | `(registry/load-missions-cached)` | cached path | 1 per call |
| `target_field.clj:100` | `(mr/open-missions {:missions missions …})` — **one-arg**, over already-loaded missions | — | **0** (a false positive in the J6 list) |
| `scripts/gate_f2_supersession.clj:22`; `mission_substrate_ingest.clj:33` | `load-missions` / `load-missions-from-files` | — | not on the click path |

**Counted statically, and I say so.** I did not drive the selection phase. There is no seam that runs it
without the tick: the phase body is `(run-phase! … :selection #(selection-judge window-days))`
(`full_loop_runner.clj:4676`), `selection-judge` is `wm/generate-war-machine`, and driving that outside the
suite fixture reads the real repair store and writes real trip reports. What I did count dynamically is the
cache, in §2, which is where the per-action number is decided.

**So: 2 uncached reads per selection** — `:7219` and `:7241` — **before any per-action work**, and **3 per
click**, the third being `mission-entry` in the construction phase. On top of that, one read per
`mission-status` / `resolve-target` call, for the reason in §2. The guardrail calls `mission-status` once per
ranked mission action (`mission_registry.clj:472` calls it "dozens per selection"); I did not run the judge,
so the number of ranked actions per selection is `{:absent :not-counted}`.

## 2. What the cache buys: nothing, and the reason is one line of it

`load-missions-cached` (`mission_registry.clj:477-491`) captures `now` at `:483`, **before** the read, and
stamps the entry with it at `:490`. An entry is therefore already as old as the read took when it is stored.
With a 5 s TTL (`:469-475`) and a read that takes longer than 5 s, every entry is stored expired.

Counted, 5 calls per trial, `load-missions` stubbed with a counter and a sleep, cache reset between trials:

| read latency | calls | substrate reads |
|---|---|---|
| 0 ms | 5 | **1** |
| 1 s (under the TTL) | 5 | **1** |
| 6 s (over the TTL) | 5 | **5** |

**Reads saved per click at today's latency: zero**, at any number of calls. Across the clicks of one flight:
also zero — the entry is expired before the next click starts, and would have been anyway at a 5 s TTL.

The TTL's own justification says the zero-arg load is "a bounded substrate-2 query (cheap)"
(`:470-474`, written 2026-09-17). At `TN-futon1b-cost-profile-2026-09-08.md`'s p50 of 2.3 s that premise
held and the cache worked. It stopped holding when the read passed 5 s.

## 3. Where the time goes in futon1b, and what the source says

**Timings, curl against 127.0.0.1:7073.** Five each, plus probes:

| request | runs | time |
|---|---|---|
| `entities?type=mission&limit=1` | 5 (04:30Z) | 30.36, 28.79, 26.90, 25.53, 26.05 s |
| `entities/latest?type=mission&limit=1` | 5 (04:30Z) | 27.96, 24.77, 24.68, 23.46, 26.75 s |
| `entities?type=mission&limit=1` | 4 (later, quiet) | 10.26, 9.53, 9.42, 9.22 s |
| `entities?type=zzz-nonexistent-type&limit=1` | 1 | **0.097 s**, `:count 0` |
| `census?type=mission` | 1 | 3.43 s — and it answers `{:type "mission", :kind :hyperedge, :count 0}`: it counts **hyperedges**, not entities, so it is not a cheap substitute for this read |
| `entities/census?type=mission` | 1 | 35.50 s, 116 KB |

The response carries `:count 331` and a `:next-cursor`. So the route costs ~9.4 s at rest and 23–42 s under
load (claude-8's 04:3xZ curls read 36.3–41.8 s; my five futon2-JVM reads of `limit=1000` at 04:0xZ read
34.7–41.1 s). **An empty type answers in 97 ms**, so the predicate is not scanning the table — the cost
tracks the number of rows of that type.

**The query path is the one the note describes, plus one more query.** `entities-query`
(`futon1b_graph.clj:591`) builds a narrow ordered window over `[xt/id entity/type]` with the caller's
`limit` (`:609-620`) and hydrates it by `_id IN` (`:621`, `fxt/hydrate-by-ids`) — exactly what
`futon1b_xt.clj:210-222` documents, and `:215-222` records why: a wide `[*]` projection under a type
predicate cost ~13 s, the narrow window ~0.7 s, the `IN` hydration ~1 s.

Then `:622-626`:

```clojure
total (count (query-fn node
                       (fxt/pq '[p-type]
                               '(-> (from :entities [xt/id entity/type])
                                    (where (= entity/type p-type)))
                               t)))
```

**No `limit`.** Every `/entities?type=…` call materialises every row of that type and counts them in
Clojure, whatever the caller asked for — which is why a `limit=1` read costs what a `limit=1000` read costs.
(The file is `futon1b_graph.clj`, not `futon1b_xt.clj` as the request had it; `futon1b_xt.clj:210-222` is
`hydrate-by-ids`' docstring.)

**Where it came from.** `bb3c3c5`, **Joseph Corneli**, 2026-08-23 11:36, "entities: deadline route reads".
That is **2½ hours before** the window-and-hydrate rebuild it now sits beside — `081e34e`, same author,
2026-08-23 14:03, "Typed entity reads: ordered id window, then hydrate by `_id IN`". The rebuild made the
read it was about ~20× cheaper and left the count query next to it untouched.

What I have **not** established: that the count query is where the 9.4 s goes. Both queries use the same
`(from :entities [xt/id entity/type]) (where (= entity/type p-type))` shape, so a per-matching-row cost
would be paid twice per call; separating them needs timing inside futon1b's JVM, which this lane may not do.
`{:absent :per-query-split-not-measured}`.

## 4. One line each

**futon2 — switch no caller; fix the stamp.** Moving `now` in `load-missions-cached` to after the read
(`mission_registry.clj:483` → `:490`) is what turns the guardrail's dozens of `mission-status` calls into one
read per selection; switching `cascade_problems.clj:45` and `war_machine.clj:7219` to the cached read only
helps once that stamp is fixed, and buys nothing before it.

**futon1b — for its owner:** the unlimited `total` count at `futon1b_graph.clj:622-626` (`bb3c3c5`, Joseph
Corneli, 2026-08-23 11:36) materialises every row of the type on every call regardless of `limit`, and is the
reason a one-row read of `type=mission` costs what a thousand-row read costs.
