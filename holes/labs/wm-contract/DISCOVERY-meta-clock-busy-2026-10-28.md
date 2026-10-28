# Discovery: META producer clock read refuses on busy session resolution

Date: 2026-10-28  
Scope: read-only inspection of the retained repair-iteration-1 receipt and the
current canonical futon2/futon3c/futon1b sources. No reload or click was run.

## Finding

The failing read is the `:session-resolved` clock decision at the end of the
META diagnosis producer's agent turn, not the admission decision at its start.
Admission already completed: the retained `worker-observation.edn`, captured at
20:43:07Z, shows `wm-author` inside its model HTTP call. The final clock update
then failed at 20:45:43Z, after the agent returned a session identity different
from the one known when the turn began.

That final update calls `record!` directly and therefore does not use the
busy-store retry loop. A futon1b HTTP 504 is correctly classified as
`:clock/store-busy`, but this call propagates it immediately. Commit
`3617cc30c05df369634280655ec1f5fcd3fd2dc5` fixed a different route: a busy
read performed inside `boundary/append!` was wrapped as top-level
`:error/code :exception`; the commit taught `record!` to recognize the nested
`:futon1b-read-unavailable` as a busy write. It did not change the direct call
from `finish!`.

## Exact path

1. futon2 prepares the diagnosis in
   `src/futon2/aif/meta_self_heal_production.clj:1262-1293`.
   `prepare-diagnosis` calls `produce-diagnosis!`, whose first attempt reaches
   the injected producer at `meta_self_heal_production.clj:268-273` as
   `(diagnose request)`.

2. The serving port is installed by
   `/home/joe/code/futon3c/src/futon3c/wm/meta_self_heal_adapters.clj:839-840`:
   `:meta-diagnosis-fn #(producer-invoke! ... :diagnosis %)`. `producer-invoke!`
   invokes `wm-author` through `invoke-with-rate-limit-retry!` at lines 469-480;
   the latter calls `reg/invoke-agent!` at lines 263-280. Its sole retry is for
   provider HTTP 429 after 90,000 ms, not for the clock store.

3. `registry/invoke-agent!` starts clock accounting before delivering the agent
   turn at
   `/home/joe/code/futon3c/src/futon3c/agency/registry.clj:1203-1211`:
   `(start! aid-val current-session prompt invoke-options)`. `start!` calls
   `record-waiting-out-busy-store!` at
   `agency/clock_decision.clj:387-408`. This admission call already waits on
   `:clock/store-busy`.

4. Once the agent returns, `registry/invoke-agent!` calls `finish!` at
   `agency/registry.clj:1260-1268`. If the returned session differs from the
   session at admission, `finish!` executes the failing call at
   `agency/clock_decision.clj:477-478`:

   ```clojure
   (when (and session-id (not= session-id (:session-id *turn*)))
     (record! (assoc *turn* :session-id session-id :phase :session-resolved)))
   ```

5. In that `record!`, the decision-id lookup is
   `agency/clock_decision.clj:294-299`:
   `(busy-read #(store/get-entry* backend eid))`. For a cold session there can
   first also be `restore!` at lines 295-296; both reads have the same
   classification. `store/get-entry*` delegates to the futon1b backend. Its
   HTTP read is bounded in
   `/home/joe/code/futon3c/src/futon3c/evidence/futon1b_backend.clj:225-266`.
   The default requested timeout is 120,000 ms (lines 53-59); a substrate
   read-health context caps it at 30,000 ms
   (`src/futon3c/substrate/read_health.clj:4-11`). Futon1b itself reports a
   query deadline as HTTP 504 with `:error :query-deadline-exceeded`
   (`/home/joe/code/futon1b/futon1b_server.clj:617-626`); exhausted query permits
   throw after 5 seconds and reach the same timeout response
   (`futon1b_xt.clj:33,56-60`).

The META adapter supplies no `:timeout-ms` in its invocation options
(`meta_self_heal_adapters.clj:474-480`), so Registry does not impose a separate
agent-invoke deadline (`agency/registry.clj:1113-1114,1231-1258`). The relevant
bound is consequently the individual evidence-read bound above, followed—on
the affected direct finalization call—by immediate refusal rather than a retry.

## Classification and retry comparison

`futon1b_backend.clj:253-265` maps every HTTP 504 (and 429/502/503) to
`:error/code :futon1b-read-unavailable`; it does not depend on whether the body
names `:query-deadline-exceeded` or the server message names an unavailable
query permit. `busy-read` recognizes `:futon1b-read-unavailable` at
`agency/clock_decision.clj:252-284` and rethrows `:clock/store-busy`.

The behavior then depends on the caller:

* Admission (`start!`) and deferred edit activity (`finish!` lines 469-473)
  call `record-waiting-out-busy-store!`. That loop catches only
  `:clock/store-busy`, sleeps according to `*store-busy-waits-ms*`, and retries
  the idempotent stable decision (`agency/clock_decision.clj:344-385`). The
  schedule is 2s, 5s, 10s, 20s, 30s, then nine 60s sleeps: 607 seconds of
  bounded backoff, followed by one final attempt. A non-busy exception is
  rethrown immediately. The wall-clock bound additionally includes each
  evidence-read attempt (normally at most 120s, or 30s under read-health
  context).
* Session resolution (`finish!` lines 477-478) calls `record!` directly. It
  therefore classifies the observed 504 correctly but refuses at once.
* `3617cc30c` changed `busy-write?` at
  `agency/clock_decision.clj:258-265` and its use at lines 318-325, so a busy
  read hidden inside the append receipt also becomes `:clock/store-busy` and
  can be retried by a retrying caller. It did not put the session-resolution
  caller inside that retry loop.

The retained receipt is consistent with this distinction: its final error is
`:clock/store-busy` with text "while reading this turn's clock decision". That
proves classification succeeded; the absence of waiting is at the caller.

## Smallest repair (not made)

At `/home/joe/code/futon3c/src/futon3c/agency/clock_decision.clj:478`, replace
the direct `record!` call with `record-waiting-out-busy-store!`:

```clojure
(record-waiting-out-busy-store!
 (assoc *turn* :session-id session-id :phase :session-resolved))
```

This reuses the existing stable decision identity and the existing bounded
607-second backoff schedule. It changes only transient busy handling. A real
read rejection (`:futon1b-read-rejected`), invalid evidence, persistence
failure, or any other non-`:clock/store-busy` exception still escapes on its
first attempt; a store that remains busy through the schedule still refuses on
the final attempt. The regression should construct this exact
session-identity-change path, make the real backend boundary return a 504-shaped
busy read once, and show that `finish!` retries and then persists; a companion
non-busy failure should show immediate refusal.
