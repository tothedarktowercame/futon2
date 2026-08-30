# R2 round 1 — structured observation

## What I am

I turn heterogeneous scan data into a fixed, ordered fourteen-channel numeric observation used by the War Machine loop. The channel vocabulary is declared at `src/futon2/aif/observation.clj:11-32`; `observe` projects scan inputs into a channel map at `src/futon2/aif/observation.clj:34-74`, and `sense->vector` fixes the order at `src/futon2/aif/observation.clj:76-80`.

Packet correction: `:acknowledged?` is not one of my fourteen observation channels (`src/futon2/aif/observation.clj:18-31`). It is a field hard-coded on lane-futility nags (`src/futon2/aif/lane_futility.clj:321-334`). It cannot stand in for observing an act.

## How I need to develop

1. I need an explicit input carrier for completed actuation witnesses. Today `observe` destructures only loop health, support/attack, mission triage, graph, frames, and annotations (`src/futon2/aif/observation.clj:44-49`). No actuation input can reach a channel.
2. I need a declared observation semantics for an act witness: presence alone, verified external effect, and failed/absent actuation are different facts. Today every channel is coerced to a number and missing values commonly default to `0.0` (`src/futon2/aif/observation.clj:48-74`), which would erase typed absence.
3. I need durable correlation from an observed act back to its tick and mission. R16's current audit carries mission and construction diagnostics but no external witness identifier (`src/futon2/aif/enact.clj:218-232`).
4. I need a test that a witness changes the next observation and that no witness produces a typed absence rather than a plausible zero. The current vector conversion defaults any absent channel to `0.0` (`src/futon2/aif/observation.clj:76-80`).

## What I need from R16

Proposal, not current traffic:

- Payload: `{:tick <stable-id> :mission <mission-id> :witness {:id <stable-id> :kind <external-kind> :location <substrate-address> :digest <content-digest>} :disposition :enacted|:not-enacted}`.
- I need `:disposition` because an absent or failed act must not masquerade as a successful witness. The current `:enacted nil` explicitly means reproduction did not happen (`src/futon2/aif/enact.clj:205-209`).
- I need the witness to name an external, independently readable artifact; R16's present audit describes its own executor output only (`src/futon2/aif/enact.clj:222-232`).
- Proposed idempotency key: `[tick mission witness.id]`. This is a proposal; no current producer establishes those identities.

## What I can give R16

Proposal, not current traffic:

- A receipt can state `{:tick ... :mission ... :witness-id ... :observation-tick ... :channel ... :value ... :status :incorporated|:refused}`.
- I can promise only that a validated witness was incorporated into a named later observation, not that the world change succeeded; R16 must establish the act's external effect.
- Guarantee, atomicity, retry, and timeout remain unspecified until there is a durable inbound queue/write and a defined observation commit. Inventing them before those operations exist would describe no running mechanism.

Traffic today is false: `observe` has no enactment input (`src/futon2/aif/observation.clj:44-49`), while R16 only attaches `:enactment` to the judgement/trace (`src/futon2/aif/enact.clj:234-254`).
