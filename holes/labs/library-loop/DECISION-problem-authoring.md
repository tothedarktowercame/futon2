# Decision sheet: commission the problem-author role? (for Joe)

Date: 2026-09-06. Basis: annotation rounds A1-A3 (receipts in `runs/`),
which completed the 24-pattern zaif-cascade refusal frontier: **4 served,
20 adjudicated no-committed-source**, every refusal naming the missing
problem in the annotator's own words plus the rejected near-miss candidates.
Gate: 24/53 -> 20/53 refused (each count independently reproduced).

## The 20 named missing problems, by family

- **equity (5):** confident-adaptation-as-published, confident-adaptation-merged,
  contribution-pathways, contribution-pathways-merged,
  protected-interaction-as-published
- **ai4ci (3):** ai-core-function, funding-justification, pilot-feasibility
- **math-formalization (3):** CA/measure-integration-api,
  CV/frontier-bound-from-arc-hypotheses, CV/holomorphic-disk-api
- **gauntlet (2):** modeline-persists-across-worlds, teaching-inversion
- **singletons (7):** agent/trail-enables-return, ants/pheromone-trail-tuner,
  cascades/declared-skeleton, control/effort-estimation, hdm/non-capture,
  math-strategy/missing-dependency-protocol, ukrns/publication-cadence

## The decision

**Q1 — commission the problem-author role at all?** The role MINES committed
history (excursion logs, project documents, dossiers) for text that states
each problem, and commits a problem node citing it — same evidence rule as
annotation, one level up. It does NOT invent problems: a pattern whose
problem was never written down anywhere stays honestly refused, and that
result is informative too (it marks patterns whose rationale lives only in
someone's head).

**Q2 — if yes, which families first?** Ranked recommendation:

1. **cascades + control + agent + ants singletons** — closest to the
   harness/cascade work now resuming; their problems almost certainly
   exist in committed WM/loop history.
2. **equity + gauntlet + ukrns** — project families; sources would be the
   project documents themselves.
3. **math-formalization + hdm + math-strategy** — likely sourced from the
   Lean campaign's own records; may deserve authoring by that lane instead.

**Q3 — runtime/timing?** The role reads and writes prose (the role-fidelity
case for a zai seat); zai quota resets 2026-09-08 02:04Z. Codex (98%) can
start batch 1 now if waiting is worse than fidelity.

**Answerable in one reply,** e.g.: "yes, batches 1-2, codex now" or "yes,
batch 1 only, zai after reset" or "hold".

## RULED (Joe, 2026-09-06, verbatim)

"Let's run these on Codex only. Batches 1 and 2 both."

Execution: A4 (batch 1) and A5 (batch 2, incl. ai4ci) minted; codex-18;
sequential with claude-1 review between batches; batch 3 NOT commissioned.

## A4 review adjudication: self-derived problem nodes (claude-1, 2026-09-06)

All four A4 sources are the patterns' OWN committed HOWEVER/BECAUSE text,
promoted into the problems namespace (the receipt says so plainly). That is
within the letter of the commission ("committed history" included futon3's
corpus) but it changes what gate reachability MEANS: a pattern reachable
only through its own promoted text is the pattern vouching for itself --
the F3/R9 non-self-certification concern at graph grain.

ADJUDICATION -- accepted with a typed distinction, not reverted:
- The four nodes stand: the statements are committed, dated (originating
  commits verified by blame), and genuinely problem-shaped. Deleting real
  information to protect a metric would be backwards.
- Each A4 node gains an in-band `source-class: self-derived` line (A5
  packet carries the edit) so a future STRICT gate reading can exclude
  self-derived roots mechanically -- the certified-cascade reading should
  report both numbers: reachable, and reachable-through-independent-problems.
- Batch 2 rule: EXTERNAL-FIRST. Project documents are the mining grounds;
  a self-derived promotion is a marked fallback, never silent.
- Gate counts henceforth report the split. Current: 16/53 refused; of the
  37 served, 4 reach only through self-derived nodes.

Overridable by Joe; recorded here rather than blocking on him.

## Refinement (Joe, 2026-09-06): witnessed problems, not just stated ones

Verbatim (compressed): problems ARE supposed to be encoded inside patterns,
but the in-pattern HOWEVER/BECAUSE statements are "conceptual, or let's say
theoretical... we don't necessarily know that we've actually solved that
problem." For the what-problems-are-we-solving question, "take some
patterns and look back and see, was there an actual solution?" -- at the
level of MISSION CLUSTERS and historical work, "even if the specific
patterns we used weren't cited in missions." Witnessing sources named:
(1) the top half of the live cascade (mission clusters -- what problems
were we solving there, and do the design patterns evidence them);
(2) the what-problems-are-we-solving paper's clustered areas of endeavor;
(3) vsat arcs -- "a live updating documentation surface for the whole
futon stack" with contemporaneous summaries of what problems we felt we
were solving (located: futon4/README-vsatarcs.md, reader over ~102
scene-form stories in futon5a/holes/stories/, each with an .aif.edn
annotation overlay).

CONSEQUENCE. The problem graph gains a SECOND typed dimension, orthogonal
to source-class: witness-class. A problem node is *stated* (its text
exists, committed) vs *witnessed* (historical work evidences an actual
solution, with pointers). This is the readiness ladder's formula-vs-
witnessed distinction applied at problem grain, and it serves Joe's
theory-track ruling ("mining the evidence landscape... what problems we
have been solving historically"). Role: **solution-witness** (roles note
section 7). Discovery round dispatched read-only; witnessing annotations
only after review.
