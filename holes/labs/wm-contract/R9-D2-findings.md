# R9-D2 — independence checker run

Status: observed build output, 2026-08-30. Corpus: `p4ng` commit `6c288174`,
`vetting/OBLIGATIONS.md`.

## Instruments

- **Observed.** `checks/r9_independence.clj` invokes `git -C /home/joe/code/p4ng
  show 6c288174:vetting/OBLIGATIONS.md`; it never reads the mutable working-tree
  file (`checks/r9_independence.clj:100-106`). It splits all `## O…` sections
  and takes only the first bold status marker (`checks/r9_independence.clj:27-35`).
- **Observed.** The run emitted both EDN and Lean from the same data structure
  (`checks/r9_independence.clj:67-96,131-142`). Regenerating both twice produced
  byte-identical files; regenerating followed by `git diff --exit-code` is the
  divergence gate.

## Corpus and verdicts

- **Observed.** The pinned corpus has 22 sections: 13 fixed, 7 open, and 2
  unmarked. The closed ids are O1 O2 O3 O5 O6 O7 O8 O9 O14 O15 O16 O17 O20
  (`holes/labs/wm-contract/R9-D2-report.edn:1`). O1, O2, and O5 contain later
  residual statuses, but only their first `Status` is classified
  (`p4ng/vetting/OBLIGATIONS.md@6c288174:17-45,46-67,86-105`).
- **Observed, tautological.** Ledger-only is 13 `unknown`; it exercises the nil
  declaration arm but is not evidence of checker quality
  (`holes/labs/wm-contract/R9-D2-report.edn:1`).
- **Observed.** Declared run is 13 `self`, reported per row. O7, O14, O15 use
  `.rowText`; the other ten use `.paperSentence`. The named commissioned agents
  are inside the author part by the existing ruling, not a builder judgement
  (`holes/problems/P-R9.md:66-68`; `R9-D2-report.edn:1`).
- **Observed.** Prose attribution tokens occur in 8/13 rows: O1
  (author, reviewer), O3 (reviewer), O5 (reviewer), O7 (author, reviewer,
  codex-1), O8 (author), O14 (author, reviewer, codex-1, codex-7), O15
  (zai, reviewer, “fixed by”), O20 (reviewer). O2, O6, O9, O16, O17 have none
  under the stated token scan (`R9-D2-report.edn:1`). These tokens are reported
  but never supplied to the checker (`checks/r9_independence.clj:38-44,48-60`).

- **Observed (added by claude-20 at review, from this note's own per-row data; codex-8's count and rows
  unchanged).** The 8 conflates two findings, and only one of them bites. Splitting the same rows by
  whether the token names a **specific agent identity** or only a **generic role word**:

      specific agent   3 of 13   O7 (codex-1) · O14 (codex-1, codex-7) · O15 (zai)
      generic only     5 of 13   O1 · O3 · O5 · O8 · O20
      neither          5 of 13   O2 · O6 · O9 · O16 · O17

  "author" and "reviewer" are role vocabulary carrying no producer identity, so counting them inflates the
  result. **The 3 is the finding**: the paper's blanket *"the author has since closed thirteen"*
  (`sec-discussion.tex:404-407`) is too strong in three rows, where the ledger's own prose names a
  commissioned agent as closer. Those three are exactly the rows carrying `rowText` declarations, and they
  are `self` only because `P-R9.md:66-68` places commissioned agents inside the author's producing part —
  the falsifier being that a named closer who was *not* commissioned would flip that row to `independent`.
  The tokens still never reach the checker; this is a reading of the note's data, not a change to it.
- **Observed.** The badge file headline says four derived badges, while parsing
  its quantities gives five; the self-verdict is recorded beside both numbers
  (`data/r18-badges.edn:13-15`; `R9-D2-report.edn:1`).

## Falsifier and Lean fixtures

- **Observed.** The test makes a correct checker return `self` inside and
  `independent` outside, then shows a false-only checker judges an inside
  producer independent and fails soundness (`test/r9_independence_test.clj:6-17`).
- **Observed.** The generated Lean file supplies literal tables and proves the
  exact fixture predicates: checker consultation, recorded soundness, per-row
  declaration sources, and the two-run census (`R9-D2-report.lean:5-59`). It
  compiled against `DarkTower.WarMachine.Holes` with standard axioms only.

## Refusal / packet correction

The later acceptance paragraph describes `declarationSource` as a string and
mentions a transcribed `inDeclaredPart` field. That contradicts the quoted Lean
interface: `DeclarationSource` is a sum and membership is derived from
`producer` and `declaredPart` (`mathlib4/DarkTower/WarMachine/Holes.lean:244-268`).
The generator follows the interface and does not emit the stale fields. The
immutable holes themselves are not edited; the generated declarations are
proof terms with the same predicates over this run's named fixtures.
