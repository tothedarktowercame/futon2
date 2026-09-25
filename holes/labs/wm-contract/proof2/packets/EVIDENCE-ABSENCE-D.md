# EVIDENCE-ABSENCE-D — http-backend answers a failed read with `[]` and `0` (absence read as a value)

Author: kimi-7 (E-kimi-task-7), for claude-8 / PROOF-2. Read-only analysis of
futon3c @ 61ab13eb working tree. No code edits. All findings settled by code
and records; no operator testimony cited as authority (Joe's 2026-09-24
principle is noted as the stated requirement, not as proof).

## 1. The two substitutions

File: `futon3c/src/futon3c/evidence/http_backend.clj`.

`-query`, lines 98–100:

```clojure
resp @(http/get url {:timeout 10000})
parsed (parse-response resp)]
(or (:entries parsed) [])))
```

Introduced by commit `28f5dec5f862fd2db8840c7d15d9c8a112d31133` (Joe Corneli,
2026-02-20, "Add make tickle launcher and HTTP evidence backend"). The commit
message cites no ruling for the `[]` fallback; the surrounding design note in
the ns docstring says only "queries a remote Agency's evidence API".

`-count`, lines 113–115:

```clojure
resp @(http/get url {:timeout 10000})
parsed (parse-response resp)]
(long (or (:count parsed) 0))))
```

Introduced by commit `10d8145afb4d22ad624079d25cafa7c6a957a5fb` (Joseph Corneli,
2026-07-10, "Sync local working-tree changes (2026-07-10)"). No ruling cited.

Why `(or (:entries parsed) [])` is a substitution and not a parse: `parsed` is
nil whenever the HTTP call throws (httpkit dereferences a failed request into
an `:error` response whose body is nil), whenever the status is non-200 with a
non-JSON body, and whenever the 10 s client timeout fires — `parse-response`
(lines 18–21) swallows all parse exceptions to nil. So *every* failure mode
collapses to the same value as a 200 with `{"entries":[]}`. The same shape
recurs at line 129 in `-all` (`(or (:entries parsed) [])`). `-forks-of` (line
118–120) returns `[]` by design ("Not exposed via HTTP API — return empty for
now"), a third absence-as-value site, though not a failed-read substitution.

Timing evidence on record (test_registry.clj docstring, lines 931–939):
"that client times out at 10s, and a page large enough to be slow comes back
EMPTY rather than as an error (2026-09-25: limit 2000 over 3108 entries took
18.7s and returned nothing)."

## 2. Consumers of `-query` / `-count` on HttpBackend

`-query`/`-count` are reached through the wrappers `futon3c.evidence.store/query*`
(store.clj:130) and `count*` (store.clj:141), which pass straight through to
whatever backend the caller resolved. Only call sites whose resolved backend is
an `HttpBackend` are exposed to the substitution; in-JVM callers resolve the
XTDB store and are unaffected. Grepping protocol callers through wrappers:

| # | Consumer | Reaches via | Backend | What it does with `[]` / `0` |
|---|----------|-------------|---------|------------------------------|
| C1 | `futon3c.test-registry/latest-run-for-namespace` (test_registry.clj:947 `-query`, :974 `-count`; entry point `-main "latest-for-namespace"`, :1081) | `store/query*`, `store/count*` | `make-http-backend` (:1081, validation.clj `resolve-options` :44) | **Guards, but the guard is defeated by the `0` substitution.** It refuses `:scan-window-exhausted` unless `(and (pos? scanned) (>= scanned held))`. A failed `-query` gives scanned=0 → correctly refuses. But a *windowed or partial* `-query` (scanned>0) plus a *failed* `-count` (held=0, substituted) satisfies the guard and yields `(none :no-run-for-namespace)` — the false conclusion "these tests were never registered". This is the live AR-42 correction case. |
| C2 | `futon3c.test-registry.validation/resolve-options` (validation.clj:38–48) | binds the backend for `check-record!` / `register-run!` / `latest-run-for-namespace` | `make-http-backend` | Propagates C1's behaviour to every validation-warrant caller; no guard of its own. Inherits the false-absence conclusion. |
| C3 | `futon3c.nlp.classical-pipeline/fetch-turns` (classical_pipeline.clj:205–209) | `backend/-query` direct | `make-http-backend` | **Treats as empty.** `(filter turn-entry?)` over `[]` → zero turns → `run-pipeline` reports zero affect detections / zero resolutions and (unless `:dry-run?`) appends nothing — the false conclusion "no chat turns occurred since SINCE", silently, in a written report. |
| C4 | `run-pipeline` append path (classical_pipeline.clj:351) | `backend/-append` | `make-http-backend` | Not a `-query`/`-count` consumer; noted because `-append` *already* returns a typed error map (see §4). |
| C5 | Tests: `test/futon3c/evidence/http_backend_test.clj` (:126, :140, :154, :171) | direct | `->HttpBackend` | Assert the substitution: the tests currently *pin* `[]`/`0` as the failure answer and would need updating with the fix. |

Consumer count (production, affected): **3** (C1, C2, C3). C1/C2 are one code
path with two entry points.

Other `store/query*`/`store/count*` call sites (portfolio/observe.clj:110–152,
aif/stack_generator.clj:325, evidence/threads.clj:48, live_efe_map.clj:330,
agents/tickle*.clj, agents/zai_api.clj:1485, transport/http.clj:2929/2971/etc.,
agency/clock_decision.clj:49–58, logic/tracer.clj, logic/ratchet.clj:175,
logic/archaeology.clj:231, peripheral/mission_control_backend.clj:1081) resolve
the in-process store in the dev-server JVM and never hold an HttpBackend; they
are listed for completeness and are not exposed to this substitution today —
but any fix at the protocol level must keep their contract (a vector of
entries / a long) intact or type the failure so they can propagate it.

## 3. The silent 48h window

`futon3c/transport/http.clj`, `handle-evidence-query`, lines 2906–2918:

```clojure
broad-page? (and (nil? explicit-since)
                 (nil? explicit-before)
                 (nil? session-id)
                 (nil? pattern-id))
query (cond-> {}
        ...
        (or explicit-since broad-page?)
        (assoc :query/since (or explicit-since (default-evidence-since)))
```

with `default-evidence-since` (lines 2425–2430):

```clojure
(defn- default-evidence-since
  "Default lower bound for broad evidence pages.
   Session/pattern-specific views keep exact history; unfiltered latest pages
   should never force the backing store into a full chronological scan."
  []
  (str (.minusSeconds (Instant/now) (* 48 60 60))))
```

A caller that passes `type`/`author`/`tags`/`limit` but no `since`/`before`/
`session-id`/`pattern-id` gets only the newest 48h, with no marker in the 200
response that a window was applied (`{:ok true :count n :entries [...]}` —
`count` is the post-window count).

Callers of the endpoint, via HttpBackend's param map (http_backend.clj:87–94 —
`since`/`before` are sent only when the caller supplies them):

- **Pass `since`:** `classical_pipeline/fetch-turns` (`:query/since since`),
  `agents/tickle_logic.clj:259`. These escape the window but not the timeout
  substitution.
- **Do not pass `since`/`before`:** `test_registry/latest-run-for-namespace`
  (`{:query/tags [:test-registry] :query/limit limit}`, test_registry.clj:947),
  `tickle_work_queue.clj:209`, `apm_work_queue.clj:834`,
  `arse_work_queue.clj:112`, `tickle_orchestrate.clj:81`, `zai_api.clj:1485`,
  `threads.clj:48` (subject query — subject is not in `broad-page?`'s
  exemption list, so a subject-only HTTP query is also windowed; in-JVM it is
  not). Of these only the first runs over HttpBackend in production today; the
  rest would inherit the trap the moment they run standalone.

## 4. Smallest fix, per codebase convention

**Convention cited.** The codebase already has a typed-failure shape for this
exact backend: `-append` (http_backend.clj:63–70) does not throw and does not
substitute; it returns

```clojure
{:error/component :E-store
 :error/code :http-error
 :error/message "HTTP append failed: <status>"
 :error/at <instant>}
```

— the `social-error` shape defined in `futon3c.evidence.store` (store.clj:37–43).
test_registry layers its own typed results on top: `{:status :none :reason ...}`
(test_registry.clj:41) and refusal maps `{:record/type :test-registry/refusal
:reason ... :details ...}` (:42–44); validation.clj:16 throws `ex-info` with a
`:record/type :test-registry.validation/refusal` payload. The codebase's
convention for *backend-level* failures is therefore a returned typed map, not
an exception; exceptions are used one layer up to make refusals un-ignorable.

**Proposed change (smallest):**

1. `http-backend`: when the response carries `:error`, a non-200 `:status`, or
   an unparseable/absent body, return the `social-error` map with
   `:error/code :read-failed` and an `:error/kind` of `:timeout | :http |
   :parse`, instead of `[]` / `0`. Same for `-all`. `-forks-of`'s designed
   `[]` should gain `:error/code :unsupported` or a protocol-level
   `:supported?` flag so designed-empty and failed-empty stay distinct.
2. `store/query*` / `store/count*`: pass the map through unchanged (they
   already return whatever the backend returns; no wrapper change needed if
   consumers check).
3. Consumers:
   - **C1 latest-run-for-namespace**: check for `:error/code` on both calls
     before the guard; a failed read returns/refuses as
     `(refusal :registry-read-failed {:kind ...})`. *Bad case closed:* a
     timed-out or windowed scan can no longer satisfy
     `(pos? scanned) (>= scanned held)` with a substituted `0`, so "never
     registered" is only asserted when the scan is known whole. It must also
     pass an explicit `:query/since` (epoch) or the server must stamp the
     window — otherwise a *successful* 48h-windowed read still passes the
     guard legitimately.
   - **C2 validation**: inherits C1's refusal; `record-refusals!` already
     persists refusals, so the absence lands on the record typed.
   - **C3 fetch-turns**: on `:error/code`, throw `ex-info` (CLI boundary,
     matching test_registry's `fail!`) so the pipeline exits non-zero rather
     than emitting a zero-turn report. *Bad case closed:* a timed-out read
     aborts the report instead of fabricating "no chat turns".
4. Server side (separate, optional but the real root for C1): stamp applied
   defaults in the response — `{:window {:since <ts> :defaulted? true}}` in
   `handle-evidence-query` — so "newest 48h" is on the record rather than
   silent. This is the typed-absence reading of the window itself.

Throw-vs-return: returning the typed map is what `-append` already does, so
returning is the smaller, convention-matching change at the backend; throwing
(`ex-info` with the refusal map) is the right shape at CLI boundaries
(`fail!`, `refuse!` precedents) where a refusal must not flow into arithmetic
or `filter`.

## 5. Definition gap

`broad-page?`'s exemption list (`session-id`, `pattern-id`, `since`, `before`)
does not include `subject` or `tag`, so a *filtered* page is windowed while a
*session* page is not — the rule for which filters make a page "broad" is
nowhere defined. If PROOF-2 needs a statement of when a server may silently
bound a read, that definition is missing; proposed amendment: *any default
bound applied to a read must be stamped on the response, and a stamped
default may never be removed by adding an unrelated filter.*
