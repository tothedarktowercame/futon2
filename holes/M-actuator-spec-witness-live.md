# Spec: the live driver for the operational witness (`witness-live`)

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Extends:**
`futon2/src/futon2/aif/actuator_a6.clj` (it already holds `rank-graph` / `apply-star-status`
/ the `:discharge` read). **Requires** `futon2.aif.operational-witness` (the PURE relations).

## Goal
Turn `closure-witnesso` from a constructed-observation demo into a **live** witness: gather the
real `(before, discharges, after)` facts from the actual A6/substrate flow and run the pure
core.logic witness against them. **`operational-witness` stays PURE** (relations only — portable
to Reazon/`kanren`); this driver does all the I/O.

## Build — `witness-live [capability-graph opts]` (in `actuator_a6`)

READ-ONLY (A6's `flip-star-status-on-discharge!` already wrote the statuses; the driver *observes*
the counterfactual transition, it never writes):

1. **`before-order`** = the ranked mission targets of `(rank-graph capability-graph opts)` —
   WITHOUT status enrichment (the pre-discharge counterfactual).
2. **`after-graph`** = `(apply-star-status capability-graph opts)` — enrich with the CURRENT
   `:capability-star-status` rows (whatever A6 flipped).
3. **`after-order`** = the ranked mission targets of `(rank-graph after-graph opts)`.
4. **`discharges`** = read `:discharge` records with `:discharge/type :capability` via authed
   Drawbridge → `[[<discharge-xt-id> <capability-endpoint>] …]` (reuse A6's discharge read).
5. **`produces`** = from `(:missions capability-graph)`: for each `[M {:produces [caps…]}]`, emit
   `[M cap]` per cap → `[[M cap] …]`.
6. **`observation`** = `{:discharges … :produces … :before-order … :after-order …}`.
7. Return `{:observation observation
             :witness (operational-witness/run-register observation)}`.

Extract the mission target the same way the A6 test does (`(get-in row [:action :target])`).

## Acceptance (tests — seeded fixture, same style as `actuator_a6_test`)
- **Live positive:** with a `:capability-star` + a `:discharge` + the flipped
  `:capability-star-status` in substrate, `witness-live` returns
  `{:witness {:closure {:witnessed? true :bindings [["…" cap "M-a" "M-b"]]}}}`, and
  `before-order ≠ after-order`.
- **Live negative (mirror):** no discharge status → `:witnessed? false`, `before-order == after-order`.
- **Facts are gathered from the REAL substrate + ranking**, not a constructed map (a test that
  asserts the `:observation` was read from substrate, e.g. the discharge id matches the seeded doc).
- **READ-ONLY:** the driver performs no substrate writes (the test seeds/evicts; the driver only reads).
- `operational-witness` gains NO I/O deps (stays pure).

## Gates
clj-kondo clean (the `.clj-kondo/config.edn` now handles `fresh`/`run*`); `check-parens` clean;
tests green. Reuse `actuator_a6` (`rank-graph`, `apply-star-status`, discharge read) +
`operational-witness/run-register`. Authed Drawbridge (READ only). No `:7071` restart. Bell
**claude-4** back with commit sha + a real `witness-live` result (the witnessed bindings).
