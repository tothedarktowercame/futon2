# WM-REGISTRY-READ-D — the eighth flight's selection failure, read from source

Discovery only. Read at futon2 `c95c6453` (the runtime the flight ran) and futon3c `74e0235a`; no
`src/futon2/aif` file has changed between `c95c6453` and futon2 HEAD `0d4ebdea`, so the working tree is
that commit's bytes for every file cited. No code change, no load into :6768 or :7070, no write under
`data/`, no flight. One thing was executed: the substrate read of Q2, five times, in a JVM of my own
(`clojure -M` in futon2), which is the read the packet asked for.

Flight `flight-ada87008`, 2026-09-26 03:50–03:56Z; cast author `claude-6`, reviewer `claude-13`;
finding `repair-occ-036463620c0c032c9e46aa44b6a6d6b35ed3e0ceb27dc58030f07d8fc2e6c747`.

## 1. The kind is on the finding as data because nothing on this path reads `:kind`

Confirmed, and the path is this:

| hop | site | what it does |
|---|---|---|
| throw | `mission_registry.clj:443-448` | `(ex-info "substrate-2 mission registry unreachable" {:kind :substrate-unreachable} t)` — the cause `t` is attached, the ex-data carries only `:kind` |
| the selection phase's catch | `full_loop_runner.clj:4692-4706` | catches `ExceptionInfo` around the **whole** `:selection` phase; asks `judge-refusal` (`:519-542`, requires `(= "cascade decision refused" (ex-message e))`) then `gate-refusal` (`:550-566`, requires `(= :inadmissible-decision (:error d))`). Neither matches, so `(throw e)` at `:4706` — untouched |
| classify | `failure-kind-from` `:3637-3642` → `explicit-failure-kind` `:3601-3608` (reads `:failure-kind`/`:outcome` at any chain depth) → `transport-failure-kind` `:3628-3635` → `:untyped-failure` | `:kind` is read by neither |
| write | the close catch `:5666-5695` | `:failure-kind (failure-kind-from e)` `:5680`; `:failure-stage (or … (last-error-phase @phase-events))` `:5686`; `:error (.getMessage e)` `:5688` — the **outer** message; `:error-class` `:5689`; `:error-data failure` `:5690`, the outer ex-data |
| land on the finding | `:3991` | `:failure-data (:error-data data)` — which is why `{:kind :substrate-unreachable}` appears on the finding as data |

**The walk.** Scanning `src/futon2/aif/*.clj` and `scripts/futon2/report/war_machine.clj` for `ex-info`
throws whose ex-data carries `:kind` and neither `:failure-kind` nor `:outcome`: **48 sites**. Most are not
on the selection path. The ones that are, and that the register already carries, would each close
`:untyped-failure` for exactly this reason:

- row 23 `:substrate-unreachable` — `mission_registry.clj:446` (**reached this flight**)
- row 218 `:invalid-policy-prefix` — `policy.clj:240`, message "Invalid policy prefix"
- row 219 `:precision-consumption-mismatch` — `policy.clj:401`, "precision carry consumption mismatch"
- row 220 `:no-acting-cascade-candidate` — `policy.clj:423`, "Cascade selection refused"
- row 221's five (`:invalid-temperature` `:invalid-habit` `:invalid-free-energy` `:no-admissible-candidate`
  `:unmapped-candidate`) — `cascade_selection.clj:36`, "Cascade selection refused"

None of those messages is `"cascade decision refused"`, so `judge-refusal` does not recognise them.
War-machine's own five (`war_machine.clj:6237 :6240 :6301 :6323 :6626`) **are** covered, because they all
carry that exact message. One more is not on the register at all: `:substrate-mission-registry-empty`,
`mission_registry.clj:454`, "substrate-2 mission registry returned no missions" — the sibling of row 23,
same shape, same outcome.

**Can the phase catch share `8c396c3a`'s fix site? Yes — it is the same site.** `8c396c3a` added
`judge-refusal-abstention`, a re-throw carrying `{:outcome :abstained …}` with the original as cause, and
its call site is `:4705`, inside this very catch. The substrate exception passed through the same `cond` and
fell to `:4706`. A third branch there — for ex-data carrying a bare `:kind` and neither typed key — covers
all 48 throwers at once, instead of one recogniser per thrower.

## 2. The cause beneath is on no record, and it was not a timeout

**The five reads**, `futon2.aif.substrate/entities-by-type "mission" {:limit 1000}` against
`http://127.0.0.1:7073` (the url `configured-url` resolves), default `:timeout` 60000
(`substrate.clj:13`), in my own JVM:

| read | latency | outcome |
|---|---|---|
| 1 | 34,697 ms | 331 missions |
| 2 | 40,356 ms | 331 missions |
| 3 | 41,133 ms | 331 missions |
| 4 | 35,899 ms | 331 missions |
| 5 | 38,644 ms | 331 missions |

Five of five succeeded; mean ≈ 38.1 s; slowest 41.1 s, which is under the flight's 47.3 s phase and well
under the 60 s budget. The route is slow and, right now, reliable.

**What the classifier would have done with each candidate cause:**

- **A client timeout.** `babashka.http-client` raises `java.net.http.HttpTimeoutException` on its request
  timeout. That class is the **first entry** in `transport-failure-classes` (`:3555` → `:transport-timeout`),
  and `transport-failure-kind` walks the cause chain (`:3628-3635`), so it would have been found beneath the
  wrapper. The finding says `:untyped-failure`. **So the 03:55 cause was not a client timeout** — this is a
  deduction from the record plus the source, not a guess.
- **A non-2xx status.** `substrate.clj:69-73` throws `(ex-info "authoritative substrate request failed"
  {:method :url :status :body})` — no `:kind`, no `:failure-kind`, no `:outcome`. It would also close
  `:untyped-failure`, and its `:status` would be invisible on the finding, because only the **outer**
  ex-data is recorded (`:5690`).
- **Anything else** — a decode failure after the body arrived, a second read inside the phase — would behave
  the same way.

**The cause itself: `{:absent :cause-not-on-record}`.** The finding carries `:error-class
"clojure.lang.ExceptionInfo"`, which is the wrapper's class, and `:failure-data {:kind
:substrate-unreachable}`, which is the wrapper's ex-data. Nothing on any record names what `t` was.

**Where the chain would have to be read.** `flight/throwable-summary` (`flight.clj:256`) already produces
exactly this — `{:class :message}` plus `:ex-kind` plus `:cause`, the chain beneath at most five deep — and
the flight's own paths use it (`flight_runner.clj:264`, `:278`, the WM-SPIKE-FIX-II E fix). **Nothing
equivalent exists on the phase-error path.** The runner's own `cause-chain` (`:3593-3599`) is called from
exactly two places, `explicit-failure-kind` (`:3608`) and `transport-failure-kind` (`:3635`) — both for
classification, neither for the record. The place it would go is the close map's `:5688-5690`, where
`(.getMessage e)` and `(.getName (class e))` are written today.

## 3. What the preflight's ok vouches for

Phase timings are from the flight's own timeline (claude-8's); the run record carries no phase latencies.

| phase | site | route / call | timeout | measured |
|---|---|---|---|---|
| `agent-readiness` | — | Agency roster | 10 s | 6,196 ms ok |
| `code-state` | `:3805` | — | — | 6,198 ms ok |
| `substrate-preflight` | `:4644-4646` → `substrate-readiness!` `:961` → `substrate-preflight!` `:910-928` | `entity-by-id` on one random sentinel, `/api/alpha/entities/<id>` | 15 s first attempt, then 30 s, 60 s (`:113`) | **14,078 ms ok** — passed on the first attempt with 922 ms to spare |
| `preference-refresh` | `:4647-4649` → `c_vector/maybe-refresh!` `:308-314` | a signature fetch, and a re-derivation if stale; `ensure-belly-fresh!` `:322-330` "reads substrate-2" | none set here | **85,894 ms "ok"** — and the phase body is wrapped in `(catch Throwable _ nil)` at `:4649`, so it reports ok whatever happened inside it |
| `stop-line-memory` | `:4650-4652` → `repair/open-obligations` | the local repair store | — | 12,182 ms ok |
| `selection` | `:4676` → `generate-war-machine` → … → `open-missions` `mission_registry.clj:500-505` → `load-missions` `:458-465` → `load-missions-from-substrate` `:433-448` | `entities-by-type "mission" {:limit 1000}`, `/api/alpha/entities?type=mission&limit=1000` | 60 s (`substrate.clj:13`) | **47,266 ms error** |

**Plainly, what the preflight's ok vouches for:** that one `GET /api/alpha/entities/<a uuid that does not
exist>` returned — a 404, which the client turns into nil — within 15 s. It does not touch the
`entities?type=…&limit=1000` route, does not read a single mission, and says nothing about a bulk read that
measures ~38 s against a different budget. On this flight it came within 922 ms of failing its own first
attempt, and the phase that follows it took six times its budget without being able to fail.

One more thing the timings show: `open-missions` (`:500-505`) calls the **uncached** `load-missions`.
`load-missions-cached` (`:477-491`) exists with a 5 s TTL, and its docstring (`:470-474`) justifies that TTL
on the ground that "the zero-arg load is a bounded substrate-2 query (cheap)" — written 2026-09-17, when
presumably it was. It is now ~38 s per call.

## 4. Where the click entry's reason drops

`record-click` is **not** the hop any more. At `c95c6453` it takes the whole click map (`flight.clj:215`,
`:as click`) and passes four keys through when present — `:chosen` `:235`, `:outcome` `:236`, `:cast`
`:237`, `:displacement` `:240`. It carries whatever it is given.

The hop is **`record-summary`** (`flight_runner.clj:403-431`). It reads exactly four things out of the run
record: `[:decision :chosen]` `:412`, `[:decision :abstention]` `:413`, `[:runner/source
:loaded-displacement]` `:422`, and `:route` `:425`. `:outcome` is the `:via` of the route edge into
`FULL_LOOP_CLOSE` (`:425-429`) — which is where the eighth flight's `:incomplete` came from — else
`{:absent :no-terminal-outcome-on-run-record}`. **It never reads the run record's failure kind or failure
error.** WM-CAST-I gave the click entry an outcome; it did not give it a reason. `http-click-fn`'s started
branch (`:516-522`) adds only `:server-click-id` and `:cast` on top.

**Is it the same hop as the sixth flight's defect (iii)?** Same function, different branch, and the
characterisations differ. The mission records (iii) as the click entry carrying only `:abstention {:kind
:run-record-missing :missing :run-record}` — that is `record-summary`'s `(nil? record)` fallback at `:431`,
i.e. *there was no record to read*. This is the opposite case: the record was read and these fields are
never extracted from it. And on the reading in the request — "service-failed reason dropped,
`http-click-fn`" — that branch is already fixed: the not-started path at `:502-515` carries the server's
`:status` and a typed `:detail` (WM-SPIKE-FIX-I A). So: not the same hop, and the fix for one does not
reach the other.

## 5. Defects

| # | site | reader | writer | fix shape | test box |
|---|---|---|---|---|---|
| J1 | `full_loop_runner.clj:4692-4706` (the selection phase catch) | `explicit-failure-kind` `:3601-3608` | every `ex-info` whose ex-data has `:kind` and neither typed key — **48 sites**, of which 9 registered kinds are on the selection path | a third branch beside `judge-refusal`/`gate-refusal`: ex-data carries a keyword `:kind` and neither `:failure-kind` nor `:outcome` → re-throw carrying a typed key, original as cause, exactly as `judge-refusal-abstention` (`8c396c3a`) does | a throw of each shape through the phase catch; assert the finding's `:failure-kind` is the thrower's `:kind`, not `:untyped-failure` |
| J2 | the close map `:5688-5690` | the finding's `:failure-error` / `:failure-data` | `close!` | write a cause summary, not just `(.getMessage e)` — `flight/throwable-summary` (`flight.clj:256`) is the existing shape and already produces `:cause` | a nested ex-info with a known cause; assert the cause's class and message reach the finding |
| J3 | `flight_runner.clj:403-431` `record-summary` | the flight record's click entry | the run record | read the failure kind and error the same way `:outcome` is read at `:425-429`, with a typed absence when the record has none | a run record with a failure; assert the click entry names it, and a record without one; assert the typed absence |
| J4 | `mission_registry.clj:454` | the register | — | `:substrate-mission-registry-empty` is a reachable kind with no register row | — (register work) |
| J5 | `full_loop_runner.clj:4649` | the phase log | `preference-refresh` | `(catch Throwable _ nil)` makes the phase incapable of reporting a failure; it recorded "ok" for 85.9 s | a throwing refresh-fn; assert the phase records it |
| J6 | `mission_registry.clj:500-505` | `open-missions`' callers | — | the uncached `load-missions` on a path that calls it per action, against a read that now costs ~38 s; `load-missions-cached` exists with a 5 s TTL whose "cheap" premise (`:470-474`) no longer holds | a counter on the substrate fn; assert one read per selection, not one per action |

## 6. Proposed packet split — one behaviour each

1. **WM-PHASE-KIND-I** — J1 only. The third branch in the selection catch; nothing else moves. This is the
   one that makes row 23 and the eight other registered kinds land typed.
2. **WM-CAUSE-ON-RECORD-I** — J2 only. `close!` records the cause chain through the existing
   `throwable-summary`. Independent of J1: it is what would have told us what `t` was.
3. **WM-CLICK-REASON-I** — J3 only. `record-summary` reads the failure kind and error, typed absence when
   absent. Needs J2 first if the reason is to include a cause.
4. **WM-REGISTER-EMPTY-I** — J4, register row only, no code.
5. **WM-PHASE-SWALLOW-I** — J5 only. Whether `preference-refresh` should be able to fail is a decision, not
   an edit: today a substrate outage there is invisible.
6. **WM-MISSION-READ-COST-I** — J6 only, and it wants a measurement before a change: how many
   `open-missions` calls one selection makes. Sizing, not a fix.

J1 and J2 are independent and could run in parallel; 3 follows 2; 4 is register-only; 5 and 6 are decisions
that should not ride along with a typing fix.
