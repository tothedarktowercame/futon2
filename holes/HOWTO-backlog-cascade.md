# HOWTO — build (and rebuild) the backlog-cascade

**2026-07-12, claude-2 — the method behind
`futon7/holes/M-futon-forward-model.sequencing-v1.md` (waves) and
`…backlog-cascade-v0.edn` (the typed prior), written for reuse: this is a
RECIPE, each step with its inputs, its command-shape, and the pitfall we
actually hit. Design rationale lives in [[N-backlog-as-cascade]]; this doc
is the how. A rebuild is ~1–2 hours of agent time; v(n+1) is a NEW deposit,
never an edit.**

## Inputs (gather first; note each one's date)

| input | where | freshness rule |
|---|---|---|
| open-mission census | live sweep (step 1) | must be same-day |
| centrality (c_joint / c_mission / c_eig) | `futon7/holes/M-futon-forward-model.centrality.json` | staleness OK for scores, NOT for statuses — statuses come from the census |
| operator interest (anchors + per-mission) | `…interest.json` | re-elicit if the operator says his anchors moved |
| market/generative pass | `…roadmap.edn` | flagged :speculative by design |
| operating facts | ask the operator | dated externalities (deadlines, windows, in-flight batches) |
| held-out seeds | `…heldout-seeds.md` | READ THE RULES, not the seeds' details, before authoring anything |

## Step 1 — census the open missions

Sweep `M-*.md` across the futon repos' `holes/` + `holes/missions/`
(agent fan-out works; one line per mission: name | repo | status | mtime |
cross-refs `[[M-…]]`/`:enables`).

**PITFALL (hit twice in one day):** never classify status by substring —
"ARGUE (DERIVE **complete**)" is OPEN. Use **leading-token** classification
(first token of the status line; closed iff ∈ {CLOSED COMPLETE DONE
ARCHIVED SUPERSEDED}). This is the mission-registry's own fixed bug
(`2cd7445`); every ad-hoc grep reintroduces it. Prefer querying the
registry itself where it's live.

Also collect: which statuses name a BLOCKER (gate, Codex dispatch,
operator hold) — these are sequencing edges.

## Step 2 — correct the axes' statuses against the census

The centrality/interest files carry statuses from their own generation
date. Join on mission name; census wins on status; axes win on scores.
Missions in the census but absent from the axes get NO score (do not
invent one — they rank by dependency position only).

## Step 3 — cluster by coupling, not by theme first

Group by (a) cross-ref density (the mutually-linked recent cluster IS the
active front — this found the live campaign independently, which was the
method's first self-validation), (b) shared repo/infrastructure, (c) theme
last. Same-move mission pairs (mutual ⇄ refs doing one job) get NOTED as
absorption candidates, not merged.

## Step 4 — sequence clusters into waves

Ordering criteria, in priority order:
1. **dependency edges** (X feeds Y ⇒ X's wave ≤ Y's);
2. **dated externalities** (deadlines/windows override everything —
   put them where the calendar says, not where the theme fits);
3. **centrality** (c_joint) within a wave;
4. **interest** only to break remaining ties — and where centrality and
   interest DISAGREE, write the disagreement down as operator information
   (combining-methods-as-diagnostic). Never average the axes.

**Every wave gets a REVISION CONDITION** — the concrete observation that
would reorder it ("if gate L3 fails → …", "any revenue signal → 2c
preempts"). A wave without a falsifier is a wish, not a plan. Add the
global falsifier too (ours: waves 1–2 ≈ the operator's ~25-of-125 hunch;
three misses ⇒ rewrite).

## Step 5 — the held-out discipline (do this BEFORE authoring holes)

- Seeds stay IN the candidate pool (a census that can't see a seed makes
  the rediscovery test unpassable — the seal binds the ANSWER KEY, not the
  pool).
- Seeds get NO wave assignment, no queue seeding, no weight, no steering
  prompt — they rank by mechanism or not at all.
- **Hole-authoring rule:** holes in the cascade must be DERIVED from
  in-plan evidence (a ladder rung nothing grounds; a verdict demanding an
  unminted mission). Authoring a hole that paraphrases a sealed seed =
  steering. Cite each hole's derivation in the artifact.

## Step 6 — express as the cascade (fold-turn form)

One EDN deposit (`backlog-cascade-vN.edn`), gated by check-parens +
single-form read:

- `:psi` — the strategic want, honest prose (this is what terminals
  discharge);
- `:boxes` — one per wave-1/2 mission: `{:id :mission :role :satiety
  :via}`. **Satiety = how much of its wave-ROLE the mission's CURRENT
  phase already delivers** (not how finished the mission is);
- `:wires` — `[from to justification]`, dependency edges only, each
  justified in words (wires are earned, exactly as in tactical folds);
- `:terminals` — the boxes whose completion discharges the psi directly,
  with a `:terminal-note` saying why;
- `:holes` — missing MISSIONS, each with `:derived-from` (step 5 rule);
- `:hole-discipline` + `:revision` fields — the protocol travels inside
  the artifact;
- waves 3–4 stay OUT of the cascade (low-satiety frontier); the shelf
  (parked/stubs) stays out entirely — pool, not plan.

## Step 7 — render + register

Optional but worth it: a human-readable brief (SVG lane map + box-by-box
cross-reference) — a second agent rendered v0's in one pass. Register the
deposit's existence where consumers look (the forward-model mission doc;
the campaign log if a lane consumes it). The render binds to its version;
a new deposit gets a new render.

## Step 8 — revision protocol

- v(n+1) = NEW deposit citing WHICH revision condition fired; never edit
  v(n) (the prior's evolution is itself an append-only record).
- Update the waves doc in place (it's prose) with a one-line changelog;
  the cascade EDN is the immutable half of the pair.
- Re-run from step 1 — the census is always same-day; everything else
  can be inherited if unchanged.

## What this bought us, concretely (why reuse it)

The v0 pass: (1) independently rediscovered the active campaign from
coupling structure alone; (2) surfaced the centrality-vs-interest tension
as a named operator decision instead of a silent average; (3) made batch-8
mission selection a read off the rendered artifact (the prereg's
"strategic level selects a mission," operationally true for the first
time); (4) produced 4 evidence-derived holes of which one (h3, the
WM-rank→queue seam) was VALIDATED as real within hours — the strategic
field went blind and the seam's absence was exactly where it hurt.
