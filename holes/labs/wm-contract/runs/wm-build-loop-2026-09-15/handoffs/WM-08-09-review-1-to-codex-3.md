# WM-08-09-review-1: independent acceptance assessment of three owner fixes

From claude-3 to codex-3. **Assessment author: codex-3. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's September 16 breadth-first direction (`p4ng/wm-walkthroughs/build-loop/closure/DEADLINE-PLAN.md`, sha256 `ee2e2128…`). Dispatch: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-08-09-review-1.md` (sha256 `0ff615f9fba6021eaf5b04c5c52fe4df1e74188aa1109121411aae131af2c1ee`). **Read it first; it governs.** Cascades WM08 C1–C6 and WM09.

This is a **read-only source assessment**. It runs **concurrently** with codex-4 (WM-04 resolver, writing `src/futon2/aif/observation_authority_resolver.clj` and tests) — you write only your receipt directory, so the scopes are disjoint.

It is **not** serving activation, an ordinary-run claim, or a repair. Accepting a historical diff does not mean it is deployed.

## The three subjects, as I verified them (canonical futon2 `4fec3894`)

| Commit | When | Subject | Files |
|---|---|---|---|
| `c4130ba5` | 09-15 15:21 | Interpretation failures open an environmental hold, not a machine repair | `src/futon2/aif/full_loop_runner.clj` (+7), `test/futon2/aif/interpretation_job_test.clj` (+11) |
| `706ca3e8` | 09-15 16:09 | Legacy predecessor resets with provenance, not a refusal | `src/futon2/aif/receipt_construction.clj` (+34/−5), `test/futon2/aif/receipt_construction_test.clj` (+26) |
| `983d4c49` | 09-15 16:15 | Before-arm reuses the recorded acting order | `src/futon2/aif/receipt_construction.clj` (+13/−1), `test/futon2/aif/receipt_construction_test.clj` (+16) |

Two facts that bear directly on attribution:
- **`full_loop_runner.clj` has one later commit, `cae5f6d9`** ("Retain preregistered pre/end fact measurements and move exposures"). Before crediting current runner behaviour to `c4130ba5`, check whether `cae5f6d9` touches the interpretation-failure path. If it does, assess the fix as it stood and state separately what the current code does.
- **`receipt_construction.clj` has no later commits**, so current behaviour there is attributable to those two fixes plus what preceded them.
- Git records Joe's identity as author on all three; the attribution to claude-20 comes from the dispatch and the TODO, not from git metadata. Don't assert it from git.

## What each verdict must decide

**`c4130ba5` — interpretation failure.** It must remain a **typed failure** that opens an environmental hold. It must not be converted into a machine repair or into a successful construction.

**`706ca3e8` — legacy predecessor reset.** Explicit legacy-bootstrap handling must **not** become permission to reset established model state when history is merely missing or corrupt. Establish exactly what distinguishes "legacy predecessor" from "missing" and "corrupt" in the code, and whether a test exercises each. If only the legacy case is tested, that is a finding.

**`983d4c49` — recorded acting order.** The retained before-arm order must be **the same occurrence's actual order**, and must preserve the distinctions between selected, interpreted and enacted. A test that only checks that *some* recorded order is reused does not establish that it is the same occurrence's.

For each, give **accept**, **limited acceptance** (name the concrete missing case), or **reject**. Do not change any specification or contract to rescue a fix.

## Controls

- Run the affected namespaces in isolated processes, retaining command, cwd and exit:
  - `clojure -X:test :nses '[futon2.aif.interpretation-job-test]'`
  - `clojure -X:test :nses '[futon2.aif.receipt-construction-test]'`
- Keep realistic **positive** controls, and add targeted negatives where the existing tests do not already distinguish the property: **wrong-order**, **wrong-predecessor** (missing and corrupt, as distinct from legacy), and **interpretation-failure**.
- Additional disposable tests go **only** in your receipt directory, never in canonical `src/` or `test/`. Run them against canonical source (e.g. with the receipt directory added to the classpath) and record exactly how.
- clj-kondo and check-parens (files after `--`) on any Clojure probe you author.
- **No** serving reload, Lean rebuild, click, or full test suite.

If the existing tests cannot distinguish a required property, report limited acceptance or rejection with the concrete missing case. Do not broaden the review into writing the repair.

## Scope

Write only `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-08-09-review-1/`.

Not allowed: canonical source or test edits, checkbox edits, reloads into the shared JVM, pushes, choosing successor work. futon2 is shared (codex-4 commits concurrently): stage explicit paths only, never `commit -a`, never amend, and check `git log -1` immediately before committing.

## Receipt: `wm-08-09-review-1/RECEIPT.md`

Record each fix's verdict in its own section as you finish it (so partial work survives), with: the exact diff read, the current corresponding source and any later change, the property required, the evidence for or against, commands with cwd and exit, and the exact remaining WM-08/09 obligations. State plainly that acceptance of a historical diff is not deployment.

## Stop and bellback

Stop once the receipt is committed. **Bell claude-3 back** with the commit sha, the three per-fix verdicts with their reasons, the test and control results with exits, and the remaining WM-08/09 obligations. A discovery beyond scope is a question in the bellback, not a task.
