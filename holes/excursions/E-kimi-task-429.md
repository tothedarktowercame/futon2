# E-kimi-task-429 — Docstring index for the ~4400 Mathlib lemmas cited by generated APM proofs (coverage measurement, no prose)

**Requisition:** in-progress — dispatched 2026-11-10T02:11:27Z to kimi-1 as invoke-1794276687376-16506-39d054ce

Clocked in by claude-21 for kimi-1 on 2026-11-10 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Docstring index for cited Mathlib lemmas (discovery; no prose writing)

## Goal

Generated APM proofs cite library lemmas as "a library lemma". We want to replace that phrase with
a gloss derived mechanically from the lemma's Mathlib docstring. Before designing the rule we need to
know how many cited lemmas HAVE a docstring, and what the docstrings look like. Your job is the
measurement: a script that maps every cited name to its declaration in the Lean sources and extracts
the docstring and statement, plus a coverage summary. Do not write glosses or prose yourself.

## Inputs (read-only)

- Names: `/home/joe/notes/claude-21/census/cited-lemmas-all.tsv` (columns: name, citations, theorems;
  about 4400 distinct names, 22k citations). Names are as Lean prints them, e.g. `le_of_not_gt`,
  `Filter.mp_mem`, `LT.lt.le`, `Continuous.comp`, `Quotient.sound`.
- Lean sources, in this order: Mathlib at `/home/joe/code/apm-lean/.lake/packages/mathlib/Mathlib/`
  (lake-manifest rev 5ee5050), then the other packages under `/home/joe/code/apm-lean/.lake/packages/*/`
  (batteries, aesop, …), then Lean core: resolve the toolchain from `/home/joe/code/apm-lean/lean-toolchain`
  and read `~/.elan/toolchains/<toolchain>/src/lean/` (Init, Std, Lean).

## Deliverables (all under `/home/joe/notes/claude-21/census/kimi/`)

1. `docstring_index.py` — stdlib Python only. Given the names TSV and the source roots, writes:
2. `lemma-docstrings.tsv` — one row per name, columns:
   `name  status  file  line  docstring  statement`
   - status ∈ `docstring`, `no-docstring`, `alias-of:<target>`, `to_additive-of:<multiplicative name>`,
     `deprecated-alias-of:<target>`, `not-found`, `ambiguous:<n>` (several candidates, list the first file).
   - docstring: the `/-- … -/` text immediately above the declaration, flattened to one line (collapse
     whitespace; escape tabs/newlines); empty when none.
   - statement: from the declaration keyword to the `:=` (or `where`/`|`), flattened to one line.
   Resolution must handle: `namespace X … end X` and `section` nesting; `theorem/lemma/def/abbrev/instance/
   structure field (projection like `Subtype.property`)`; `protected`/`private`; `@[…]` attribute lines
   (including `@[to_additive]`, `@[deprecated …]`, `@[simp]`) between the docstring and the declaration;
   `alias foo := bar` and `alias ⟨a, b⟩ := iff`; `export`; dotted names where the prefix is a namespace or a
   structure (`LT.lt.le` is `theorem LT.lt.le` or `theorem le` inside `namespace LT.lt`).
3. `SUMMARY.md` — counts per status; citation-weighted coverage (sum of `citations` over names with
   status `docstring` ÷ total citations); the same for `theorems`; the 20 most-cited names with status
   `not-found` or `no-docstring`; and five example rows for the top names
   (`le_of_not_gt`, `lt_of_not_ge`, `LT.lt.le`, `Filter.mp_mem`, `Continuous.comp`) quoted in full so the
   extraction can be checked by eye. State the command line that produced the TSV and its wall time.

## Acceptance

- The script runs from the names TSV to the output TSV in one command, in minutes, with no Lean process.
- Every name in the input appears exactly once in the output.
- The five example rows name the right declaration (check `le_of_not_gt` especially: it may be a
  deprecated alias of `le_of_not_lt` or similar in this Mathlib rev; record what you find, do not guess).

## Constraints

- Read-only over `/home/joe/code/apm-lean` and everything under `.lake`. No `lake`, no Lean builds,
  no edits there, no git commits or pushes anywhere. Write only under `/home/joe/notes/claude-21/census/kimi/`.
- Nothing persistent in `/tmp`.
- If a resolution case is too hard, record the name as `not-found` with the reason in a `note` column
  rather than inventing a match. Accurate coverage numbers matter more than a high score.

When done, bell claude-21 back with: the status counts, the citation-weighted coverage, the five
example rows, and the paths of the three deliverables.
