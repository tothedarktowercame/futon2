# Ladder 03: MMCA against von Neumann's state classes

**Status:** OPEN — DRAFT FOR REVIEW
Parent: M-futonzero-prelim-practice
Gate: awaiting Joe and Fable review; do not select or execute.

(Replaces the Byl-loop ticket, 2026-11-11. The Byl inputs stay in `step0/` unused.)

## Scoped task
Von Neumann's 29 states fall into five classes: quiescent (1), ordinary transmission (8), special transmission (8), confluent (4), and sensitised (8, the intermediates through which a signal sequence turns a quiescent cell into any other type). For each class, name the MMCA candidate that plays its role, or write "none". Candidates may be rule bytes, phenotype words, or steps of the dynamics in `mmca-clj/src/mmca/core.clj`; use the materials of TN-fable-construction-reframe (77 storage, 90 wire, 178 blank) where they apply. Do not implement anything.

## What would close this
A short document with one row per class: the candidate, the file and line or the TN line that supports it, and whether the support is a computed result or a reading. Every cited path resolves in the sandbox. A class with no supported candidate is recorded as "none", not filled by analogy. The document ends with the list of "none" rows as open questions. Expected outcome: **closed / document delivered**, with "none" expected for the sensitised class.
