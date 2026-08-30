# R16 round 1 — actuation looking toward R2

## What I am

I am the scheduled tick's artifact-only enactment step: I select the first
passing act gate, run the deterministic fold executor over its shown patterns,
and attach the realized outcome and enactment audit to the judgement
(`src/futon2/aif/enact.clj:234-254`). I do not currently write the substrate or
perform outward action; substantive action remains operator-gated
(`src/futon2/aif/enact.clj:12-16`).

## How I need to develop

1. My executor must cover selected missions rather than only constructions its
   fold engine can reproduce. Today an empty reproduction becomes
   `:enacted nil`, and therefore no realized score (`src/futon2/aif/enact.clj:205-232`).
   Separately, the reviewed live-test registry names only four missions
   (`src/futon2/aif/actuator_a3.clj:372-379`).
2. My result needs a typed distinction between enacted evidence and absence.
   `enact!` currently returns a wiring or nil under one `:enacted` key
   (`src/futon2/aif/enact.clj:205-218`), so downstream readers cannot pattern
   match success, refusal, unsupported action, or executor failure.
3. I need an actual re-observation adapter. The running close-loop path adds
   `:realized-outcome` and `:enactment` to a judgement
   (`src/futon2/aif/enact.clj:234-254`); it does not produce an R2 observation.

## What I need from R2

- A declared observation destination for an actuation witness: which channel
  accepts it, its value type/range, and how typed success or absence becomes an
  observation. This is a proposal request; no such adapter is cited here.
- A receipt saying the witness was admitted to the observation used by a
  particular next tick, rather than merely stored beside the judgement. This
  is proposed because the drawn edge's receipt is still `:unspecified`
  (`/home/joe/code/p4ng/empirics-futon/control-map-edges.edn:23-27`).

## What I can give R2

- Proposed payload: `{tick, mission, witness}`, where `witness` is a tagged
  result such as `{:status :enacted, :wiring ...}` or
  `{:status :not-enacted, :reason ...}`. The current drawn entry already names
  these three payload fields, though only `witness` has a type
  (`/home/joe/code/p4ng/empirics-futon/control-map-edges.edn:23-27`).
- Evidence already available to form the witness: mission, executor source,
  prediction source, expected score, cascade, box count, and policy-hole count
  (`src/futon2/aif/enact.clj:218-232`).
- Proposed guarantee: one typed witness for each passed act gate, including an
  explicit non-enactment result. I cannot yet guarantee that: exceptions return
  the judgement unchanged (`src/futon2/aif/enact.clj:234-255`).

Retry, timeout, idempotency, atomicity, and receipt remain unresolved. The
current executor shells another process (`src/futon2/aif/enact.clj:113-129`),
and the source states no safe replay or atomic-delivery contract. This edge is
a specification; it carries no traffic today.
