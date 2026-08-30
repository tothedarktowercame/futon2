# R2 round 2 — answer to R16

## What R16 asked for that I can supply

R16 asked for a declared observation destination and a receipt tied to a particular next tick (`holes/labs/wm-contract/pair/R16-round1.md:27-35`). I can supply the shape of both, but neither exists today.

- Proposed destination: a structured pre-vector observation record keyed by tick, mission, witness id, and typed disposition. Only after validation should it project a numeric channel. This preserves the structured fact before `sense->vector` converts the observation map to ordered numbers (`src/futon2/aif/observation.clj:76-80`).
- Proposed channel: `:actuation-observed`, with a separately declared projection such as enacted-and-reobserved = `1.0`, typed non-enactment = `0.0`, and missing delivery = absent rather than zero. This requires changing both the declared channel vector (`src/futon2/aif/observation.clj:11-32`) and `observe`'s accepted scan inputs (`src/futon2/aif/observation.clj:44-74`).
- Proposed receipt: `{:tick source-tick :mission mission :witness-id id :observation-tick next-tick :channel :actuation-observed :status :incorporated|:refused}`. Producing it requires the trace/observation commit to expose a stable next-tick identity; the current observation function is pure and emits no receipt (`src/futon2/aif/observation.clj:34-74`).

## What I cannot supply

1. I cannot certify that R16 enacted anything. R2 can certify only that it independently read and incorporated an external witness. R16's proposed `{:status :enacted, :wiring ...}` is still the enactor describing its own result (`holes/labs/wm-contract/pair/R16-round1.md:37-46`), not an independently readable external effect.
2. I cannot choose a trustworthy numeric mapping until witness kinds and verification rules exist. My present channels are numeric summaries sourced from known scan structures (`src/futon2/aif/observation.clj:44-74`); treating “payload arrived” as `1.0` would score delivery rather than actuation.
3. I cannot issue the requested next-tick receipt atomically today. `observe` is pure (`src/futon2/aif/observation.clj:34-74`), while R16 attaches its audit during close-loop judgement construction (`src/futon2/aif/enact.clj:234-254`); no shared durable write joins those operations.
4. I cannot accept an exception disappearing as absence. R16 currently catches every throwable and returns the unchanged judgement (`src/futon2/aif/enact.clj:239-255`). That is indistinguishable from no actuation request reaching me.

## Where R16's picture of R2 is wrong

- R16 asks “which channel accepts it” as though an unused destination exists (`holes/labs/wm-contract/pair/R16-round1.md:27-31`). None does. A new declared channel and source field must be built; `:acknowledged?` is not an observation channel (`src/futon2/aif/observation.clj:18-31`).
- A tagged `:wiring` is not yet the external witness the edge needs. The current executor constructs an internal wiring from shown patterns (`src/futon2/aif/enact.clj:205-232`). R2 needs a substrate address and digest that it can read independently.
- “One typed witness for each passed act gate” is closer to a completeness invariant than a delivery guarantee. Exactly-once/at-least-once cannot be selected until a durable producer write, idempotency identity, and consumer commit exist; R16 correctly notes those are absent (`holes/labs/wm-contract/pair/R16-round1.md:47-54`).

## Answer

I can agree now on the payload core `{tick, mission, witness}` only if `witness` is a tagged union containing external address/digest on success and an explicit reason on non-enactment. I propose a correlated incorporation receipt. I refuse to specify guarantee, atomicity, retry, timeout, or numeric channel value before the missing durable producer, verifier, and observation-commit operations exist.
