# P-supersede-stack-logic-model — the old logic model is the goal we are now building, not an input

holder: claude-15 · parent: R2 (the operator channel) / `sec-operator.tex` · status: OPEN (recorded 2026-08-30 18:40Z, Joe)

## Problem
AUD-D1 found `war_machine.clj:2075,2089,2150,2211` and `joe_hud.clj:427-428` reading
`futon5a/data/stack-logic-model.edn` and `alignment.edn` — planned inputs (`M-war-machine.md`, 05-03:
"Logic model, pocketwatch" / "Sorry topology") that were never produced. The first reading was "dead branch, delete".
Joe (18:40Z): *"it seems as though maybe we are actually in the process of reinventing that. So not that we need to
include it, but we need to supersede it."*

## What the old model was trying to do (from the reader's expected shape, the only spec that exists)
`{:workstreams [{:id :label :jsdq-mode :constraint}] :pocketwatch {:ticks [...]}}` and `{:sorry-topology [{:id :layer
:severity :status :closes-by}]}` — evaluated against commit ratios per workstream (`war_machine.clj:2211-2230`:
stack / consulting / portfolio / mathematics %). That is a **hand-written model of what the operator does**, read
as a fixed file, with ticks firing when the operator's observed behaviour departs from it.

## What supersedes it
The same function, but learned from the live store rather than written by hand and never produced:
- the paper's claim: `p4ng/sec-operator.tex` §"Closing the Loop Over the Operator" — one node outside the frame
  (the operator) with edges crossing the boundary, so the loop does not grade itself;
- the machine side: R2, the only ring whose content originates outside the Markov blanket
  (`M-formal-war-machine.md:807-814`, Joe 08-27: *"every operator turn is stored in the Evidence Landscape, processed
  with Air, decorated with metadata, forming part of a candidate cascade, and used in problem selection by the WM"*;
  `:1962-1964`: R2 is "the plug, and the socket is Tier 1");
- the currency rule: `I_data_current` — the operator model is a read of the store's operator turns at a basis, not a
  `.edn` under `futon5a/data`.
So "workstreams + jsdq-mode + pocketwatch ticks over commit ratios" becomes "the operator's turns as evidence, with
the WM's expectation of them as a generative-model prior and departures as prediction error" — same intent, on
Active Inference's terms, with the operator's feedback actually reaching the loop.

## Consequences
- AUD-D3 deletes the dead reads and **cites this record** in the commit, so the deletion reads as supersession.
- Nothing from the old shape is imported. If a future R2 design wants workstream-level ticks, it derives them from
  stored operator turns; the field names above are a record of intent, not a schema to satisfy.
- Big picture (Joe): the build is not only fixing the war machine's problems but **extending it as we understand what it
  is** — this record is the first case where a repair item turned out to be an extension item.

## Facades this record refuses
- "Delete the dead branch" as a closure: removes the symptom and loses the intent.
- "Restore the file": would reinstate a hand-written operator model, which is exactly what `sec-operator.tex` argues
  against (`:207`: a self-description is "not a model of the operator").
