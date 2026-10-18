# WM code review 2026-10-18 — common brief for every reviewer

You are one of five reviewers doing a read-only code review of the War Machine (WM) in /home/joe/code/futon2, commits since 2026-10-11 (`git log --since=2026-10-11`). About 395 commits, mostly by codex-68, with no independent reviewer. Joe flies to a conference tomorrow and wants to know: **is the machine ready for the next live click, or will it hit another "predictable" error right away?**

## Hard rules (futon2/CLAUDE.md "Claude sessions on the War Machine: ground rules" — read it)
- READ ONLY. Do not edit any file in ~/code. Do not hot-load, do not POST to :7070 (except GET requests and the read-only test-registry `check` curl in futon2/CLAUDE.md), do not click, do not touch data/ or storage/. Do not run git commands that change state (no checkout, stash, pull, commit, reset).
- codex-68 is actively working in this checkout. Expect HEAD to move; review at the sha you start at and state it.
- You may run a NARROW test namespace (`clojure -M:test -m cognitect.test-runner -n <ns>`) only if needed to confirm a specific finding, wrapped as `FUTON3C_INVOKE_JOBS_FILE=$(mktemp) timeout 300 ...`. No full suites. Prefer reading.
- Do not use `pkill -f` / `pgrep -f`.
- The specification is mathlib4/DarkTower/WarMachine/Requirements.lean plus holes/labs/wm-contract/. Measure code against it; do not propose redesigns of it.

## Context you need
- holes/labs/wm-contract/TN-war-machine-readiness-oct-17-2026.md: five live clicks yesterday, each stopped by a new defect, none carried a tick. All five defects were the same kind: two layers each holding their own copy of one contract that had drifted (old gate vs new route; cascade constructed twice so identity changed; strict decoder vs real agent output, twice; Python validator vs registry validation).
- holes/labs/wm-contract/RECORDED-FLIGHT-DESIGN-2026-10-17.md: codex-68's offline replay ("recorded flight") of click 5 pushed past those to `:production-close-evidence-missing`, using offline-authored artifacts and an offline-scoped pattern-apply executor. Its last section says live readiness requires wiring the capability dispatcher into the production runner with a real `:wm/pattern-apply` executor and exercising the full close/trace path. The newest commits (e.g. 0975c7df7, ccf6d30d8, 311689b8b, 74c35e797) are working on that.

## What to look for (in priority order)
1. **The next predictable failure**: walk the production click path in your area as a live click would hit it. Where will real (Zai) agent output, real store state, or a real downstream consumer meet a check that it will fail? Name the input, the check, file:line.
2. **Two copies of one contract**: validators, constructors, digests, schemas, enums, or decoders defined in two places that can disagree (Clojure vs Python, producer vs consumer, test fixture vs production). Fallback paths that keep an old construction for "non-production callers".
3. **Test adequacy**: do the regressions pin what the commit claims? Do they stub the very dependency the bug was in? Construct the bad case in your head (or narrowly in a test) and ask whether it would be caught.
4. **Offline/replay vs live divergence**: anything that passes in the recorded flight only because the replay stubs or isolates it.
5. Ordinary correctness bugs.

Do NOT report style, naming, or speculative refactors. Every finding needs: file:line (at the sha you reviewed), the concrete failure scenario (input/state -> wrong result), severity (blocks-next-click / likely-later / minor), and confidence (confirmed by reading the code path end to end / plausible). Say what you checked and what you did not get to.

## Output
Write your findings to the file named in your task as Markdown, as you go (so partial work survives). End with a short "Readiness verdict for my area" paragraph. Your final message should be a 10-line summary of the file.
