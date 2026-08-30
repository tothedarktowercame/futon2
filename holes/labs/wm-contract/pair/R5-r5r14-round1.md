# R5 round 1 — scoring toward R14

## What I am

I score each admitted candidate with a multi-objective controller score whose
core is expected-free-energy risk plus ambiguity, then sort candidates by that
score and assign ranks (`src/futon2/aif/efe.clj:726-735`,
`src/futon2/aif/efe.clj:844-862`). My risk includes the channel-risk and
capability-zone-risk fold (`src/futon2/aif/efe.clj:645-655`).

## How I need to develop

1. My output needs an explicit versioned payload contract. I currently return
   full `compute-efe` maps with metadata and ranks, but the public contract says
   only that entries carry `:rank` and are ordered by `:controller-score`
   (`src/futon2/aif/efe.clj:844-862`). A consumer cannot distinguish score
   schema changes from ordinary score movement.
2. I need the consumer to preserve my admitted candidate set and ordering.
   Policy extracts `g-totals` from `:controller-score` and separately finds
   `:no-op` (`src/futon2/aif/policy.clj:377-382`); dropping either silently
   changes temperature or abstention semantics.
3. I need the selection boundary named in every receipt. In the production
   `:strategic-recommendation` branch, R14's temperature affects an inspectable
   ranking, while `chosen` is the first non-no-op entry in my existing order
   (`src/futon2/aif/policy.clj:234-280`). That is not the same behaviour as the
   historical `:actuation` branch.

## What I need from R14

- The selection-gain value and temperature mode actually applied to my score
  vector. `effective-temperature` divides spread-derived temperature by gain,
  or uses `1/g`, depending on mode (`src/futon2/aif/policy.clj:46-80`).
- A receipt containing the source score-vector identity, effective temperature,
  selection gain, boundary, chosen action, and whether my scores governed the
  live choice or only a counterfactual ordering. This is a proposal; no
  cross-node receipt type exists in the cited code.
- Explicit refusal when the score vector is empty or malformed, rather than a
  plausible temperature. Empty `g-totals` currently yields `tau-min`
  (`src/futon2/aif/policy.clj:39-44`).

## What I can give R14

- Observed payload: the ordered `ranked-actions` vector. Each entry contains an
  action, `:controller-score`, and `:rank`; excluded candidates remain only in
  vector metadata (`src/futon2/aif/efe.clj:844-862`).
- Observed guarantee: within one call, every included candidate is scored with
  the same state and opts, then sorted ascending by controller score
  (`src/futon2/aif/efe.clj:853-862`). I do not guarantee cross-run stability.
- Proposed identifier: a digest over the ordered action identities, scores,
  score-component schema/version, and selection boundary. No such digest is
  currently produced.

The production runner passes ranked entries and prior selection gain to `select-action`
(`scripts/futon2/report/war_machine.clj:4509-4518`). Retry, timeout, atomicity,
and idempotency are not observable properties of this in-process pure call and
must not be invented as transport behaviour.

Packet correction: τ-adjusted scores order only the counterfactual in the strategic
branch, but R5 has already sorted `ranked-actions`; `chosen`
is the first non-no-op in that R5 order (`efe.clj:844-862`,
`policy.clj:247-253`). Thus τ does not choose there, but R5 scoring still does.
