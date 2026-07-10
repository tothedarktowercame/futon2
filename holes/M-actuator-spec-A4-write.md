# Spec: A4 minimal — the write + discharge record (short-term memory)

**Owner/reviewer:** claude-4 (Codex builds, claude-4 reviews). **Extends:**
`futon2/src/futon2/aif/actuator_a3.clj`. **Pays down:** R16-EXEC-REACH (the byte the
loop never wrote). **Explicitly NOT in scope:** capability-star `:status`, EFE
re-ranking, mission-ascent-progress — that is the deferred long-term-memory gap
(R12+R13); do not touch it.

## What A4 is

A3 already dispatches the builder (durable endpoint write via `submit-tx`) and
proves inhabitation. A4 adds the two things that make the write a remembered event:
1. capture the **before** endpoint-snapshot at dispatch, so a later return can tell
   what THIS run newly inhabited;
2. on the builder's return, record a **durable discharge record** for each endpoint
   that went uninhabited → inhabited — the short-term memory that "mission M /
   endpoint E was discharged at T, proven by query Q".

The endpoint itself is already durable in substrate-2 (that's the write); the
discharge record is the durable note that the *actuator did this work*.

## Build (extend actuator_a3.clj)

1. **`capture-before-snapshot!`** — at live dispatch (in `run-a3!`/dispatch path),
   compute `endpoint-snapshot` for the mission's bindings and persist it to
   `futon2/logs/a4-before/<mission-key>.edn` (survives the async gap until the
   builder bells back). Include the bindings + inhabited? per endpoint.

2. **`record-discharge!`** — write a durable `:discharge` entity via the authed
   Drawbridge write path (`submit-tx` + `await-tx`, `x-admin-token`):
   ```clojure
   {:xt/id (keyword "discharge" "<mission-key>/<endpoint>/<iso>")
    :entity/type :discharge
    :discharge/mission <mission-id>
    :discharge/endpoint <endpoint-name>
    :discharge/type <bound substrate type>
    :discharge/proof-query <the derived proof query, pr-str>
    :discharge/at <iso>}       ; (str (Instant/now)) — Instant is already imported
   ```

3. **`finalize-discharge!`** `[mission-id opts]` — the on-return step:
   - load the before-snapshot; take an AFTER `endpoint-snapshot`;
   - `endpoint-dial-review {:before before :after after}` → `:newly-inhabited`;
   - for each newly-inhabited endpoint NOT already recorded (query substrate-2 for
     an existing `:discharge` of that mission+endpoint), `record-discharge!`;
   - return `{:dial-review … :recorded [...] :already-recorded [...]}`.
   - **Idempotent**: a second call records nothing new. A pre-existing inhabited
     endpoint (inhabited in the before-snapshot) is NOT recorded — only what this
     run moved.

## Acceptance (tests — NO live builder dispatch, use authed Drawbridge for the record; evict test writes)

- `record-discharge!` writes a `:discharge` entity that is then provable via
  `xtdb.api/q`; evict after.
- `finalize-discharge!`: before=uninhabited + after=inhabited (drive the "after" by
  writing a temp endpoint like the smoke test) → records exactly one discharge;
  before=inhabited already → records nothing; second call → records nothing
  (idempotent).
- No before-snapshot present → clear error, not a silent pass.

## Gates

clj-kondo clean; `futon4/dev/check-parens.el` clean; tests green (none live-dispatch
a builder). Authed Drawbridge (`x-admin-token` from `futon3c/.admintoken`). Do NOT
restart :7071. Do NOT touch capability `:status` / EFE. Bell **claude-4** back with a
summary + commit SHAs.
