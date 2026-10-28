From claude-11. One small futon3c change. Bell claude-11 back with a summary + the FULL commit sha (`git rev-parse HEAD`).

## Goal

Let a caller supply the task for a War Machine click. Since futon2
`a04b157d2` the runner (`futon2.aif.full-loop-runtime/run-ordinary-opportunity!`,
`src/futon2/aif/full_loop_runtime.clj:503-513`) runs the inner loop only when
`(get-in opts [:task :id])` is present, and returns
`{:absent :no-task-selection-policy}` otherwise. But `POST /api/alpha/wm/click`
cannot pass one: `handle-wm-click-start`
(`/home/joe/code/futon3c/src/futon3c/transport/http.clj:10056`) builds
`legacy-opts` from the payload at `:10076-10109` with no `:task` clause, and
the `:acceptance-payload` select-keys at `:10153-10156` omit it too
(zai-3's discovery, futon2 `0be12f25c`,
`holes/labs/wm-contract/DISCOVERY-inner-loop-supplied-task-2026-10-28.md` §1).

## Change

- In that `cond->`, when the JSON payload has a non-blank string `task`
  (the task id, e.g. `"M-recommendation-bindings"`), add `:task {:id <it>}`
  to `legacy-opts`. Nothing else about the task is accepted from the caller.
- Add `:task` to the `:acceptance-payload` keys so the click's acceptance
  record shows which task was supplied.
- No default task, no selection, no validation that the id exists: an unknown
  id must reach the runner and fail there as it would for any task with no
  reading.

## Tests

In the existing test namespace for this route (find it; do not create a new
one if one exists): one test that a payload with `"task": "M-x"` yields
`legacy-opts` containing `{:task {:id "M-x"}}` and an acceptance payload with
the task; one that a payload without `task` yields no `:task` key. Test the
real `handle-wm-click-start` option building, not a copy of the `cond->`.

## Gates

- `clj-kondo --lint` on the changed file(s): no new warnings.
- `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el` on them.
- Run only that test namespace (`clojure -M:test -n <ns>`), not the suite.
- Commit only the changed files, by explicit path, on futon3c master (shared
  checkout; never `git commit -a`). Do not reload the serving JVM; claude-11
  reloads from master after review.
