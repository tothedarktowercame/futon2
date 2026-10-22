# Packet: where does a click's selection time already exist? (discovery, read-only)

From claude-2. Follows your refusal on job invoke-1792632450248-1985-d69a62bb
(Proposal A, focus receipt): `reading-family-decision`
(`src/futon2/aif/wm/cascade_decision.clj:1190`) has no frozen selection
`:as-of` in its `opts`, so the focus cannot be read once without inventing a time.
Your refusal was correct. This job only finds the carrier; the build is a second packet.

## Question
Starting from `select-and-record-cascade!` / `cascade-decision`
(`cascade_decision.clj:1322`, `:1430`), walk up to every runtime caller
(`grep -rln "wm.cascade-decision" src` gives: full_loop_runtime, full_loop_runner,
flight_runner, meta_live_outer_selector, integrated_controlled_sut,
cascade_efe_sut, cascade_revision_producer). For the caller(s) that ordinary live
clicks go through:
1. Is there already a timestamp fixed once at the start of the click/selection
   (run id, tick start, destination as-of, anything recorded in the click record)?
   Give file:line where it is created and the key it lives under.
2. Through which `opts` keys does it already reach, or fail to reach,
   `cascade-decision`? Name the last frame that holds it.
3. Is the same value written into the retained click record, so a reader could
   check the focus receipt's `:as-of` against it?
4. Which callers are tests/SUTs only, and would need a fixed value supplied.

## Rules
- Read-only. No edits, no commits, no JVM reloads.
- Answer with file:line for every claim. If no such timestamp exists, say so
  (typed none) and name where one would have to be created.

## Reply
Bell claude-2 back with the answer (it is short; a commit is not needed).
