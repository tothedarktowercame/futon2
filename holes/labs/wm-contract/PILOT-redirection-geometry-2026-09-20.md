# PILOT — redirection geometry of two sessions (2026-09-20)

Commissioned by Joe (operator, verbatim): "I'd like to commission a
pilot comparing the claude-12 (d158cebc-06aa-4763-8704-e216a5a39f5c)
and claude-4 (sessions af24caa1-54d3-4f19-9d73-c8183eb9cb65) sessions
in terms of their redirections (a time series of cosine distances
relative to the pattern embedding)."

PILOT STATUS per the standing ruling: informs the design of the real
measurement; carries no decisions.

## Method and provenance

- Source: futon1b evidence store (:7073), full keyset paging per
  session. context-retrieval events: 214 (claude-12 session), 655
  (claude-4 session). Each event: turn number, timestamp, query prefix,
  top-3 retrieved patterns with scores.
- Embeddings: futon3a/resources/notions/minilm_pattern_embeddings.json
  (1,551 patterns, 384-dim). Coverage: ZERO retrieved patterns missing
  a vector, both sessions (retrieval draws from the library corpus by
  construction).
- Two representations per the robustness ruling (no centroid):
  top1-cos = cosine distance between consecutive events' rank-1
  pattern vectors; wset-dist = 1 - weighted-Jaccard over the top-3
  id->score maps. Series files: /tmp/pilot/{claude-12,claude-4}-series.json.

## Numbers

| measure | session | n | median | mean | p10 | p90 | frac=0 | frac>=0.5 |
|---|---|---|---|---|---|---|---|---|
| top1-cos | claude-12 | 213 | 0.651 | 0.586 | 0.000 | 0.801 | 0.12 | 0.77 |
| top1-cos | claude-4 | 654 | 0.668 | 0.650 | 0.506 | 0.801 | 0.02 | 0.91 |
| wset-dist | claude-12 | 213 | 1.000 | 0.932 | 0.794 | 1.000 | 0.00 | 0.97 |
| wset-dist | claude-4 | 654 | 1.000 | 0.982 | 1.000 | 1.000 | 0.00 | 1.00 |

Representation agreement (spearman): 0.489 (claude-12), 0.106
(claude-4).

## Findings (pilot-grade)

1. **The set representation saturates at k=3.** Consecutive turns'
   top-3 retrievals are almost always disjoint, so wset-dist sits at
   its ceiling (median 1.0 both sessions) and measures retrieval
   instability, not redirection. The predicted failure mode arrived
   inverted: mass at one, not near zero.
2. **The top-1 series carries signal and separates the sessions.**
   claude-12's session has a real mass of identical consecutive top-1
   patterns (12% zero-distance pairs; p10 = 0) — near-identity
   operations: sustained threads, wakes returning to the same topic.
   claude-4's session almost never repeats (2%; p10 = 0.506) — nearly
   every incoming turn lands somewhere new, consistent with a
   many-lane coordination stream. In the operations framing: the
   claude-12 stream contains many near-identity operations; the
   claude-4 stream almost none.
3. **The representations disagree (spearman 0.106 on claude-4), and
   the disagreement is explained**: one of them is at ceiling. Per the
   standing ruling this indicts the representation, not the algorithm.

## Design lessons for the real study

- Retain the QUERY'S OWN embedding vector in the retrieval event (or
  deepen k substantially): top-3 ids are too coarse for set measures,
  and the turn's own vector makes the turn its own position rather
  than a shadow of its nearest patterns.
- Check what text the retrieval embeds — the stored query field is a
  100-char prefix; if retrieval itself embeds a truncation, that is an
  instrument limit to fix at the embedding layer (embedding strategy
  over algorithm).
- Scope honestly: this series measures displacement between successive
  INCOMING turns. The B->A half of the off-by-one structure (agent
  responses as positions) has no retrieval events; embedding responses
  is a design item, not a computation over existing data.

Filesystem/relocation questions explicitly deferred by Joe to tomorrow.

## Confound (claude-4 review of this pilot, 2026-09-20 — appended)

The retrieval event carries no ORIGIN field, and the incoming-turn
streams are mixed: sampled query prefixes in the claude-4 session show
genuine operator turns alongside the agent's OWN park payloads echoed
back at resume, and harness housekeeping. Park payloads summarise
different lanes on different subsystems by construction, so a
displacement series over that stream measures how scattered the
agent's dispatch portfolio was — no operator redirection required.
Finding 2's between-session difference (12% vs 2% near-identity) may
therefore measure STREAM COMPOSITION, not operations: the two sessions
plausibly differ mostly in their park/bell/operator mix. (Sample
behind this: 4 events in one window, fragile parse — indicative, the
mechanism structural.)

Design lesson four, compounding with the truncation limit: record the
incoming turn's origin (operator / agent-bell / harness-resume) in the
context-retrieval event at emission. The turn envelope already carries
Origin:, so emission copies a field it can see — no prefix heuristics.
Only the operator substream is the operation series under the standing
framing; today it cannot be typed out of the data.

The numbers above stand as computed; this section changes what finding
2 is allowed to mean.
