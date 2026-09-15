# WM-01-acceptance-1: independent acceptance review of mathlib4 480a666ad2

From claude-3 to codex-3. **Author of this review: codex-3. Reviewer of your evidence and verdict: claude-3.** The repair author is codex-27; you are independent of it. Bell claude-3 back when done (see the end).

Authority: codex-28 dispatch `invoke-1789507763981-21248-b8bddfbb`, under Joe's 2026-09-15 authority for staged, bounded WM work. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-acceptance-1.md` (sha256 `f61d3606031660fa231975a0a746fc5df5671b9316f36b70826f337d6b5728ae`). Read it; it governs where this packet is silent.

## The TODO clause this serves (quoted from p4ng/CHECKLIST-fundamentals.md)

> - [ ] **WM-01 — Sound, shared probability and model carriers.** **PARTIAL:** bind dependent proofs and admitted witnesses to the repaired source; verify repair acceptance and avoid inheriting old admissions by filename. Record State/Action/Outcome, model revision and numeric semantics consistently. [K,L] Built evidence: mathlib4 `480a666ad2` adds duplicate-free support, zero mass off support and nonnegative PrecisionMap; author reports committed rejection controls and successful build. Independent acceptance receipt not yet supplied.

This dispatch covers only "verify repair acceptance" / "Independent acceptance receipt not yet supplied". Numeric correspondence, dependent proof/admission bindings and shared model identity remain open and are NOT in scope.

## Read first

- Cascade: `/home/joe/code/p4ng/wm-walkthroughs/cascades/wm-01/README.md`, steps 1, 2 and the independent repair-acceptance part of step 4 (K1–K3, K6). `cascade.json` sha256 `27841342cef13fcf6f7b9f6434f0f349f57abf73d7b6d0f0f42bf95ebe717495`.
- Repair: `git -C /home/joe/code/mathlib4 show 480a666ad2` (38 files), especially `DarkTower/WarMachine/Holes.lean` and `DarkTower/WarMachine/ProbabilityKernelRepairNegative.lean`.
- The repair author's own report, which is author validation and not independent evidence: `/home/joe/code/futon2/holes/labs/wm-contract/runs/fundamentals-checklist-2026-09-15/source-response-codex-27.json` (job `invoke-1789491575339-21070-498e7b46`).

## Goal

A reasoned, independent ACCEPT or REJECT verdict on repair `480a666ad2`, with retained evidence. A build alone is not acceptance. REJECT is a legitimate, useful outcome; report it plainly.

## What to assess, per obligation

1. Duplicate-free support.
2. Zero mass off support.
3. Exact-table / constructor obligations: what a constructor of the repaired kernel must now supply (nonnegative mass, exact normalization, the two new fields), and whether the dependent files in the repair discharge the new fields honestly (no `sorry`, no new axiom, no vacuous or trivially-true field).
4. Nonnegative precision (`PrecisionMap`): nonnegative, and not silently strengthened to strictly positive.

For each: quote the source statement; say whether it enforces the intended law; show a **valid control accepted** and a **negative control rejected for the intended reason**. The rejecting error must name that obligation. Rejection because of a missing unrelated field, a syntax error or an unknown identifier does not count. The cascade notes that the committed controls "deliberately supply all fields"; verify that. If a committed valid control is missing, you may write one (see "Output").

Axioms: run `#print axioms` on the relevant theorems and witnesses. Report anything beyond `propext`, `Classical.choice`, `Quot.sound`, and flag any `sorryAx`.

## Existing evidence first

Before rebuilding, check whether a complete independent acceptance receipt already exists; if so, validate and bind it rather than duplicate its execution. What I found (verify, do not trust): none located. `runs/wm-build-loop-2026-09-15/reports/plan-clearance-to-codex-28.md` lines 59–65 is an unexecuted plan by claude-2, and codex-27's response above is the author's own validation. A quick look in /tmp found no retained codex-27 kernel/axiom logs. Unavailable old logs stay unavailable: new evidence is new evidence, not the missing historical receipt.

## Environment facts I checked (verify before relying on them)

- Canonical `/home/joe/code/mathlib4` is on branch `darktower` at `2b22eeef31`, a descendant of `480a666ad2`. **Do not switch, reset, check out in, or build in the canonical checkout.** Its `.lake` is shared (packages 723M, build 9.7G).
- Toolchain is identical at both commits: `leanprover/lean4:v4.31.0-rc1`. `lakefile.lean` and `lake-manifest.json` are unchanged between `480a666ad2` and HEAD.
- `480a666ad2..HEAD` touches only 3 files: added `CascadeEFE.lean` and `CascadeEFEPolicies.lean`, modified `GOverCascades.lean` (all under `DarkTower/WarMachine/`). None of the 38 repair files changed, and none of them imports those three directly. Confirm the transitive import closure of what you check before leaning on that; either way the subject of record is `480a666ad2`.
- `/` has about 78G free (96% used). Keep temporary build output bounded; delete it when done, keeping logs.

Build from an isolated snapshot of `480a666ad2`: for example `git -C /home/joe/code/mathlib4 archive 480a666ad2 | tar -x -C /tmp/wm01-acc-480a`, or a /tmp worktree whose registration you remove afterwards. Prebuilt Mathlib oleans may be reused only as a copy or read-only; no build may write into the canonical `.lake`. With the same toolchain there is no cross-toolchain package sharing; if that turns out to be false, stop and report. If the subject cannot be verified in this environment, report the concrete obstacle; no environment repair campaign is authorized.

## Output: receipts only, here

`/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/`

- `RECEIPT.md` should contain:
  - the subject: full commit sha plus sha256 of each file you reviewed;
  - the toolchain: `lean --version` and `lake --version`;
  - the exact commands and their exit statuses;
  - findings per obligation;
  - control outcomes, each with its quoted rejecting error line;
  - the axiom reports;
  - the ACCEPT/REJECT verdict with reasons;
  - limits: what you did not check.
- `logs/`: raw build, axiom and control output.
- Any control you write yourself goes in this directory, not in mathlib4.
- Keep historical `480a666ad2` separate from current descendants. Do not claim current production correspondence from this acceptance.
- Commit the receipt directory in futon2 with explicit paths only (`git add <that dir>`). futon2 is a shared worktree: never `commit -a`, never amend.

## Do not

Edit production source, proof statements, tests, the checklist or dependencies; switch the canonical mathlib4 checkout; repair findings; extend the audit to all proofs; design persistence; dispatch any other work; reload serving code; run a click; check any checkbox.

## Gates

No source edits are authorized, so clj-kondo and check-parens only apply to any helper script you write in .clj/.el. The build, axiom and control runs above are the tests.

## Stopping boundary and bellback

Stop when `RECEIPT.md` and logs are committed. **Bell claude-3 back** with:
- the receipt path and futon2 commit sha;
- your verdict;
- the checks you actually executed, with exit statuses;
- the limits of those checks.

A discovery outside this scope goes in the bellback as a question, not as a task.
