# Handoff tool consolidation — 2026-10-16

Production agent replies now cross one transport boundary:
`futon2.aif.handoff-contract`.

`response-payload` is the sole general Agency payload rule: full `:result`,
then legacy `:text`, then text events; it never consumes `:result-summary`.
`exchange` owns contact availability, exceptions, terminal job state, payload
decoding, validation exceptions, job identity, and typed outcomes.

## Migrated live consumers

- selected-target reading: `handoff/exchange`
- selected-policy reading: `handoff/exchange`
- want interpretation, criteria, constraints and locator readings:
  `handoff/response-payload`
- enactment replies: `handoff/response-payload` through `reply-text`
- author/reviewer reply text: `flight-runner/reply-text`, backed by
  `handoff/response-payload`

## Deliberate specialized readers

- Feature-card extraction examines `:result-summary`, `:result`, and individual
  events separately because line boundaries and truncation are evidence in that
  protocol. It is not a general response decoder.
- `task-execution-evidence/job-text` combines summary, terminal message, result,
  and events to search for review/commit attestations. It is an evidence census,
  not a payload parser.
- `job-text-retention` retains the exact `:result` bytes and separately records
  their absence. Substituting event text would weaken its retention claim.
- `prefix-free-energy-cold-start` compares a sealed authority payload exactly;
  it does not decode an Agency reply.

## Verified failure modes

Unavailable contact, designated agent unavailable, contact exception, non-done
job, current `:result`, legacy `:text`, direct injected map/string, text-event
fallback, malformed payload, decoder exception, validator exception, blank
payload, and preservation of job id are covered by focused tests. Both reading
paths and the older flight reading/enactment adapters run in that gate.
