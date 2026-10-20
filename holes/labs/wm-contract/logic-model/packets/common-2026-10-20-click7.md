Context (claude-5, 2026-10-20, after click 7 of the self-heal grant).

Click 7 (`wm-click-25cd3682-60f4-4be3-81f3-91c993882b47`, 14:46Z-15:44Z) was the
first click to reach the author phase of the dedicated META self-heal route. The
acceptance gate worked: the proposed test was persisted, admitted, run before
repair (4 tests, 4 failures, 1 error, `:ran-before-repair`), the reviewer (zai-3)
answered `:accepted` and admission accepted it (F24). Then the author port
invoked zai-2 (session `zai-5d88ef02-8503-4035-9ef6-93adb416beee`) with the
sealed work order. The seat worked for 57 minutes and 30 rounds, made the repair
(files changed 14:49Z, uncommitted in `/home/joe/code/futon2`: `deps.edn`,
`src/futon2/aif/wm/loop_node_exercise.clj`, `src/futon2/aif/wm/terminal_receipt.clj`,
`test/futon2/aif/wm/terminal_receipt_test.clj`, new
`test/futon2/aif/meta_loop_node_injury_acceptance_test.clj`), then spent the
remaining rounds running whole test namespaces that hit the live stack. At its
round cap the seat's harness forced a final report: prose followed by a fenced
EDN map whose `:status` was `:incomplete-blocking-commit-and-baseline` and whose
payload had no `:patch-sha` (nothing was committed). The report is saved
verbatim at `~/notes/wm-click-sources/click-7-25cd3682-author-final-report.md`.

The port (futon3c `src/futon3c/wm/meta_self_heal_adapters.clj`, `agency-invoke!`,
line ~236 at `0e8562cc6`) refused it as `:agency-response-not-one-edn-map`
(`one-edn-map` reads a fence only when the whole text is the fence), so the
author receipt is `:failed` with nothing of the author's answer on it. The
coordinator (futon2 `src/futon2/aif/meta_repair_coordinator.clj` at `63995798a`)
then `held! :author-failed` (C:320-321). `transaction.edn` is `:active` with
receipts `[author :failed]`; the ledger's latest entry for
`injury/d755c886a22d6fc8c04db7fada64089e07157f62` is ordinal 6, `:claimed`,
attempt 1 of 3.

Verified by claude-5 after the click: the pinned acceptance command
`clojure -M:test -n futon2.aif.meta-loop-node-injury-acceptance-test` passes in
the author's tree (4 tests, 15 assertions, 0 failures; `bg-1792511526206-16`),
and clj-kondo is clean on the four Clojure files. The repair exists; the record
does not know it.

Rules for this packet:
- Work in a worktree under `~/worktrees/<repo>/<name>` (never `/tmp`). Do not
  touch, stash, or commit the author's uncommitted files in
  `/home/joe/code/futon2`; `git stash` is forbidden in the shared trees.
- Commit on your branch, then fast-forward the shared checkout's branch
  (futon3c `master`, futon2 `main`) to it. Never amend.
- Gates: `clj-kondo --lint <files>` clean; `futon4/dev/check-parens.el`; the
  named test namespace run once through the bounded runner
  `futon3c/scripts/bounded_test_job.py` (it wrote
  `~/notes/test-receipts/f24-meta-self-heal-production.json`; read its header
  for the arguments) with the receipt saved under `~/notes/test-receipts/`.
  Run tests in your own JVM (`clojure -X:test :nses '[ns]'` in futon2,
  `clojure -M:test -n ns` in futon3c); never load a worktree file into the
  shared JVMs on :6768/:7073.
- One pin per test must be captured from a live record named below, not an
  authored constant.
- Bell claude-5 back with: commit sha(s), the receipt path, the tests you added
  with one sentence each on what bad case they catch, and the before/after line
  numbers of every function you changed (the Lean model cites these files by
  line).
