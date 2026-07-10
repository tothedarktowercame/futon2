# Spec: A6 — re-observe → recommendation moves (direct re-ranking) + A7 falsifier

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **New ns:**
`futon2/src/futon2/aif/actuator_a6.clj`. **Pays down:** R13 (a discharge visibly re-orders the
queue). **Built against the LOCKED contract** with claude-5: claude-4/A6 owns
`:capability-star-status`; claude-5/A4a owns `:capability-star`. **Stays OFF the live enact
path** — like A5, this is the re-rank *mechanism* + its falsifier, not a `*live-wire?*` flip.

## The plug-in point (verified)
`efe.clj/mission-ascent-progress` with `status-aware? true` skips capabilities where
`[:capabilities cap-id :status]` is `:satisfied` — so a satisfied capability stops earning ascent
credit and its mission re-ranks. The `capability-graph` is passed *into* `rank-star-map-actions`;
nothing reads substrate status today. A6 supplies both missing halves.

## Pieces

1. **`flip-star-status-on-discharge! [opts]`** — read A4 `:discharge` records
   (`:discharge/type :capability`); for each, resolve its `:capability-star` (match the discharged
   capability to `:star/capability`) and write the **locked (a) entity**:
   ```clojure
   {:xt/id <status-id — deterministic fn of star-id>   ; idempotent flip
    :entity/type :capability-star-status
    :status/star <star-id>  :status/status :satisfied
    :status/updated-by :a6-discharge  :status/at <inst>}
   ```
   via authed Drawbridge `submit-tx`. **No-op (logged) if no star exists yet** (A4a hasn't minted
   it — expected on the sparse/demo map). **NEVER write `:capability-star`** (A4a owns identity).

2. **`apply-star-status [capability-graph opts]`** — the substrate→graph bridge: read
   `:capability-star-status` entities, join to `:capability-star`, and set
   `[:capabilities cap-id :status] :satisfied` for satisfied stars. Returns the enriched graph.
   Reads substrate, touches only `:status` (never `:capability-star`).

3. **Re-rank** — `rank-star-map-actions` over the status-applied graph with
   `mission-ascent-progress … status-aware? true`. Levers exist; A6 just wires the enriched graph
   + the flag.

4. **A7 falsifier `closure-falsifier`** (the E-pur-si-muove gate) — seed a `capability-graph`
   fixture (a `:capability-star` + a mission whose `:produces`/`:scope` includes that capability) →
   `rank-star-map-actions` **BEFORE** → flip the star's status (piece 1 + 2) → rank **AFTER** →
   **assert the ranked order MOVED** (the satisfied mission's ascent dropped). *"N mana spent, no
   dial moved ⇒ still a mirror"* — a discharge that does not move the recommendation FAILS.

## Acceptance (tests — seeded fixture, independent of A4a)
- **Falsifier positive:** seed → discharge/flip → the mission's rank **changes**. PASS = closure.
- **Negative control:** no discharge → rank **stable** (the gate doesn't fire spuriously).
- **Contract:** the write is exactly the `(a)` shape (deterministic `:xt/id`, `:status/*` fields,
  `:status/updated-by :a6-discharge`); no-op when no star; **never** writes `:capability-star`.
- **Bridge:** `apply-star-status` joins the two entities and sets `:status` only.
- Runs on a seeded fixture now; the real signal fills in as A4a mints stars.

## Gates
clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green. Reuse `efe/rank-star-map-actions`
+ `mission-ascent-progress`. Authed Drawbridge. **Stay off the live enact path** (no `*live-wire?*`
flip — that's a separate joint arm). No `:7071` restart. Bell **claude-4** back with commit sha +
the falsifier's before/after ranking (showing the order moved).
