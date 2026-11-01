From claude-11. Discovery only: read code; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

The War Machine's outer (task-selection) loop is now specified in Lean:
`/home/joe/code/mathlib4/DarkTower/WarMachine/R20Step.lean` at `d845c84fb`
(slices 3a–3f: `SelectionCascade`, `GiraffeFacts`, `outerModel`,
`outerScore` = `CascadeEFE.stepG` − `outerNovelty`, `advanceStateCounts`,
`advanceGainCounts`, `step`). Design:
`/home/joe/code/futon2/holes/labs/wm-contract/NOTE-outer-G-computation-2026-10-28.md`.
The Clojure side follows the Lean (Joe: Lean first). Today the runner has no
task-selection policy (`futon2/src/futon2/aif/full_loop_runtime.clj:503-513`)
and the click route accepts a caller-supplied task (futon3c `86bde441a`).

Map what exists in Clojure that the Lean definitions correspond to, so the
build packets reuse it instead of re-writing it.

## Questions

1. **G.** Which futon2 namespace implements `CascadeEFE.stepG` (risk +
   ambiguity over finite kernels) for the inner loop, and is it a pure
   function we can call with an outer model (states doable/blocked, outcomes
   (ending class, gain))? file:line of the function and its argument shape.
   Same for any novelty term (`PatternThetaNovelty` analogue).
2. **Giraffe facts.** How does a Clojure caller get, per open item, status
   (ready / not-ready / undetermined), one-step unblocks and cone from
   `futon3c.giraffe.pass` / `futon3c.giraffe.run` (missions source, run
   `~/code/storage/giraffe/20261028T154151.285Z-b916cd4e`)? Is the pass
   callable in-process from the serving JVM (futon2 depends on futon3c?) or
   only via its output files?
3. **Relation to focus and reach.** Where does the runtime already compute a
   task's relation to the current focus (focus/associated/elsewhere) and
   anything usable as reach? file:line, or "absent".
4. **Learned C.** Where does the runtime keep the learned ending-class
   preferences (the 55/35/5/5 kit and counts), if anywhere after the
   deletion? file:line or "absent".
5. **Carried state.** Where could the outer state (state counts, gain counts)
   be persisted between clicks; is there an existing per-run state ledger the
   inner loop uses (e.g. `outer-learning-manifest`)? Was it deleted with the
   outer selector (futon2 `a04b157d2`)?
6. **Entry.** Where, in `run-ordinary-opportunity!`, would a task-selection
   step return `{:task {:id …}}` when the caller supplies none, so the inner
   path then runs unchanged?

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-outer-loop-clojure-2026-10-28.md`,
one section per question, every claim with file:line. Commit only that file by
explicit path (futon2 is shared; never `git commit -a`).
