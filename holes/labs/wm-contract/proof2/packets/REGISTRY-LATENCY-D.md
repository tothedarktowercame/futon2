# REGISTRY-LATENCY-D — why a test-registry namespace lookup takes 5.7–7.8 s

Read at futon3c `5e388bda` and futon2 `594ebce8`; read-only, nothing changed, nothing proposed.
The owner is the registry's (futon3c); this sizes the question claude-8 raised after the second
and third flights, whose C8 reads refused `:registry-unreadable` against a 5 s timeout
(`observation_checks.clj:198`, "5000 ms since C8's reader was written; not raised by
WM-SPIKE-FIX-II D").

**Finding, in two sentences.** A lookup does not scan the registry: the namespace ledger is
already the index and already the lookup's source, and the `scanned 3200, registry-entries 3200`
in the response is echoed from the ledger's stored build marker, not counted per query. What each
lookup does pay is one `store/query*` round-trip to the evidence store before it will answer —
and on this deployment that is futon1b over HTTP, where claude-8 measured `/api/alpha/evidence?limit=1`
at 3.5–9.4 s, which is the whole of the 5.7–7.8 s.

## 1. What one lookup actually does

`latest-run-for-namespace` (`test_registry.clj:1423`) with a ledger configured — and the handler
always configures one: `transport/http.clj:2554` assocs `:namespace-ledger-file` unconditionally
from the server's `:test-registry-root`, so the live endpoint is never on the no-ledger branch.

1. build the ledger if the file is absent — one full scan, once (`:1499-1500`);
2. read and fold the ledger file (`namespace-ledger`, `:1501`): `data/test-registry/namespace-ledger.edn`,
   782 KB / 1,779 lines as of 2026-09-26 00:25;
3. **`fill-namespace-ledger-forward!` (`:1502`, defn `:1269`) — one `store/query*` per lookup**,
   `{:query/tags [:test-registry] :query/limit 200 :query/since <ledger-whole-through>}`
   (`:1292-1294`). `:since` is the newest `:whole-through` over the build and fill markers
   (`ledger-whole-through`, `:1246-1257`), not the newest run entry, for a reason that file records:
   on 2026-09-25 six runs registered 03:27–03:31Z sat in the store while the ledger's newest run
   read 03:59Z;
4. re-fold the ledger if the fill appended (`:1513-1515`);
5. on a hit, one `read-chain!` for that entry (`:1519-1520`).

The fill is not optional and not cached away: a failed fill read answers `(none :registry-read-failed)`
(`:1506-1510`) rather than answering over a gap — AR-43's rule, stated at `:1504-1505`.

## 2. What the `scanned` numbers are not

`:1526-1528`: the `:no-run-for-namespace` answer carries `:scanned (:scanned built)` and
`:registry-entries (:registry-entries built)` — both read off the **build marker**. They say the
one-time build scanned 3,200 entries against a store holding 3,200; they do not say this query did.
A per-query scan happens only with no ledger configured (`:1541-1544`), which the HTTP handler never
does. So "the lookup scans the whole ledger per query" is not what the numbers show.

## 3. Where the seconds are

Step 3 is a network read. The evidence store is whatever the server's `:evidence-store` config holds
(`transport/http.clj:2444-2448`); in this deployment that is futon1b, and its backend
(`evidence/futon1b_backend.clj`) calls exactly four paths — `/api/alpha/evidence`,
`/api/alpha/evidence/<id>`, `/api/alpha/evidence/count`, `/health` (`:309, :436, :455, :536, :576, :605`).
claude-8's `?limit=1` measurement of 3.5–9.4 s against the lookup's 5.7–7.8 s puts essentially the
whole lookup in that one call. Steps 2 and 4 parse 782 KB of local EDN, which is not where seconds go.

**futon1b's hung entities query is not on this path**: `/api/alpha/entities` appears nowhere in the
futon1b backend. It is the same JVM, though, so a saturated futon1b shows up here as a slow evidence
read without sharing a route.

There is a 60 s query cache (`futon1b_backend.clj:93-101`), keyed on the exact `[base-url params]`
(`:392`) and dropped whole on any successful append through the backend (`:130-134`). Both misses
are structural for this caller: the fill's `:query/since` advances whenever a fill appends, so
consecutive lookups are different keys, and a registration — the thing that happens while clicks are
running — invalidates everything.

## 4. The fixes, sized; the choice is the registry's

| fix | where | cost | what it does not do |
|---|---|---|---|
| raise C8's read timeout past futon1b's tail | one constant, futon2 `observation_checks.clj:198` | a line | nothing gets faster; a click still stalls for the full read, and the tail is 9.4 s against a 5 s limit, so it is a guess at a moving number |
| let a lookup answer from the ledger when a fill ran within N seconds | `test_registry.clj:1502`, plus a marker read | small code, **a rule change**: AR-43's refusal at `:1504-1507` exists so the lookup never answers over a gap, and this trades that for a bounded staleness window | does not make futon1b faster; moves who bears the risk |
| make futon1b's evidence read fast | futon1b, not the registry | unsized here — needs the query plan behind `/api/alpha/evidence`, which this note did not open | the registry's per-lookup round-trip stays, it just costs less |
| index by namespace | **already done** — the ledger is that index and is already the source | — | the index was never the missing piece; the freshness check in front of it is what costs |

**Not established here:** the runtime value of the server's `:evidence-store`, read from config rather
than probed; the query plan behind futon1b's `/api/alpha/evidence`; and whether the 3.5–9.4 s spread
is load-dependent — three measurements at 23:30Z, 23:50Z and 00:02Z are claude-8's, not re-run.
