From claude-11. Discovery only: read code, run records and the serving JVM's state; no edits, no reloads, no clicks. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Joe (2026-10-28): get the War Machine's inner loop working again on its own,
with the outer loop set aside. The outer selector was deleted this morning
(futon2 `a04b157d2` "Remove outer selection machinery; require a
caller-supplied task", then `5429f0b2c`). Since then
`futon2.aif.full-loop-runtime/run-ordinary-opportunity!`
(`src/futon2/aif/full_loop_runtime.clj:503-513`) returns
`{:absent :no-task-selection-policy}` unless the caller supplies
`{:task {:id …}}`, and otherwise runs `run-ordinary-opportunity-with-task!`.

Find out what stands between today's machine and one working click on a
caller-supplied task. Do not fire one.

## Questions

1. **Entry.** Trace `POST /api/alpha/wm/click`
   (`futon3c/src/futon3c/transport/http.clj`, `handle-wm-click-start`) to
   `run-ordinary-opportunity!`. Can a caller pass a task id through that
   route today? If yes, the exact request body; if no, the exact place it is
   dropped (file:line).
2. **Loaded code.** Is the serving JVM running master's post-deletion code?
   Ask the JVM, read-only (e.g. via `futon3c/scripts/proof-eval.sh`, forms
   that only read: `(find-ns 'futon2.aif.meta-live-outer-selector)`, the
   source/arity of `run-ordinary-opportunity!`, namespace load times). List
   any deleted namespace still loaded and any changed namespace whose loaded
   version predates `a04b157d2`. (Memory: committed is not loaded.)
3. **Inputs per task.** For a supplied task, list what the with-task path
   requires before it can choose a tactical cascade (target reading, pinned
   pattern graph, occurrence squares, cascade feedback, ledgers ...), as
   file:line, and for each whether it is per-task or global.
4. **A first task.** From the live Giraffe ready set (run
   `/home/joe/code/storage/giraffe/20261028T145255.009Z-cab4c7c6/pass.edn`,
   13 ready tickets), name the tickets for which every per-task input in (3)
   already exists on disk (give paths), or say none do and which input is
   missing for each.
5. **Last clicks.** When was the last click that reached the with-task path,
   and how did it end (run record path, terminal state)? Any click since
   `a04b157d2`?

## Output

`/home/joe/code/futon2/holes/labs/wm-contract/DISCOVERY-inner-loop-supplied-task-2026-10-28.md`,
one section per question, every claim with file:line, command output or path.
Commit only that file by explicit path (futon2 is shared; never
`git commit -a`).
