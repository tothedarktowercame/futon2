# M-wm-substrate-1a-to-1b-port — port futon2 WM substrate reads off the retired futon1a onto live futon1b

**Status:** CLOSED — IDENTIFY → handoff to codex-3, reviewed by claude-6. Acceptance implementation and hermetic tests landed in `b6e70912a`.
**Owner mission:** `~/code/futon7/holes/M-war-machine-aif-completion.md`
**Date:** 2026-07-17. Operator: Joe. Ground Control: claude-6.

## Why (confirmed live)

futon1a (XTDB1, :7071) was **retired** 2026-07-14 (futon3c I-0 unification);
futon1b is the live substrate. futon1b serves `application/edn`, NOT JSON, and
does **not** serve the targeted `?end=<endpoint>` hyperedge route (it hangs) —
only `?type=<hx-type>`.

`war_machine.clj`'s `substrate-get-json` (scripts/futon2/report/war_machine.clj:654)
sends `Accept: application/json` and `(json/parse-string body true)`. Against 1b's
EDN body the parse throws → caught → nil → `fetch-hyperedges-by-type` returns `[]`
→ **`mission-doc-index` is empty (0 of 335 live mission-docs)** → the strategic
mission-value enrichment can't resolve a mission's endpoint → it skips delta-t →
`:mission-value-factor` stays `nil` → **strategic G is flat and the landed
strategic fix is inert live.** Verified: enriching 3 real missions yields
identical G-efe = 5.195, all `:tension`/`:centrality`/`:mvf` nil.

The delta-t half is already ported (see reference below) and verified
(`:mission-T` complete→0.0, head→1.0, derive→0.7 live) — but it is never
exercised because the endpoint mapping upstream is broken.

## Reference pattern (WORKING — copy this)

`futon3c/src/futon3c/aif/mission_delta_t.clj` (working tree, claude-6, 2026-07-17)
ports the same class of read. Key moves:
- Body is **EDN** → `clojure.edn/read-string`, not `json/parse-string`. Send
  `Accept: application/edn`.
- `:hx/props` arrives as an **EDN-encoded string on some records, a map on
  others** → normalize to a map before reading props.
- `:hx/ends` is `[{:entity-id "…"}]`; but 1b **also** provides `:hx/endpoints`
  as a plain string list, so endpoint extraction can stay on `:hx/endpoints`.
- The `?end=` route hangs → fetch by `?type=` and filter by endpoint.
- Per-type fetches are **best-effort** (catch timeout/failure → `[]`), because
  some families (`mission-cross-ref`, `file→mission`, `sorry-doc`) are slow/
  oversized in 1b; the essential `mission-doc` family is reliable.

## Scope

1. **Port `substrate-get-json` (and any sibling substrate reader) to EDN.**
   Parse 1b's EDN body; handle the `:hx/props` string-or-map and endpoint
   shapes. Keep the `?type=` route (it works); never call the hanging `?end=`.
2. **Sweep** futon2 for other consumers of the deprecated 1a contract —
   JSON-parsing of :7073 substrate responses, `?end=` usage, `:hx/props`-as-map
   or `:hx/endpoints` assumptions — across `scripts/futon2/report/war_machine.clj`,
   `src/futon2/aif/*`, `src/futon2/aif/full_loop_runner.clj`, and any script.
   **Do NOT change** the futon3c API reads on :7070 (`fetch-missions` etc.) —
   that host returns JSON and works; only the :7073 substrate reads are EDN.
3. **Verify the enrichment key-schemes align** so `:mission-value-factor`
   actually populates for REAL candidates: tension looks up `mission-doc-index`
   by `(normalize-mission-id target)` (M- stripped); centrality looks up by
   `(action-target-key target)` (keys are `M-…`). Confirm real candidate
   `:target` values resolve in BOTH maps; fix any mismatch.
4. **Do NOT touch** the strategic scoring logic — `forward_model.clj`,
   the `:mission-value-factor` formula, or `enrich-candidates-with-mission-value`'s
   math (all landed + reviewed in commit `8361512`). This task is purely the
   substrate-read porting so that logic ENGAGES.

## Acceptance bar (concrete, testable)

- `mission-doc-index` returns non-empty (~335) against live 1b.
- Enriching real `:advance-mission` candidates for missions of DIFFERING phase
  yields non-nil, DISTINCT `:tension`, `:centrality`, `:mission-value-factor`.
- `efe/compute-efe` gives DISTINCT `:G-efe` across those candidates (strategic
  fix engages — no longer flat).
- A **hermetic** test (mocked EDN substrate response, e.g. `with-redefs` on the
  http fetch) asserting: the reader parses EDN, `mission-doc-index` populates,
  and the enrichment resolves keys to a non-nil `:mission-value-factor`. Must
  not depend on the live substrate.
- Existing futon2 tests stay green.

## Secondary (note, don't block)

The full judgement (`clojure -M:wm-judgement-only`) runs >200s. If the port
surfaces an obvious cause (e.g. repeated 5s-timeout reads against slow families),
note it; performance is a follow-on, correctness is the deliverable.

## Gates (required before bell-back)

- `clj-kondo` clean on changed `.clj`.
- `futon4/dev/check-parens.el` clean on changed `.clj`.
- `clojure -X:test` green for the affected futon2 nses incl. the new hermetic test.

## Bell-back

Bell **claude-6** with: summary, commit sha(s), gate results, new-test names, and
the **live enrichment probe output** showing non-nil distinct `:mission-value-factor`
and distinct `:G-efe` across real missions. Do NOT touch cron or run the live loop.
