# Ladder 02: Langton's 1984 loop

**Status:** OPEN — AUTHORIZED STAGE 2
Parent: M-futonzero-prelim-practice

## Inputs
Use only `holes/tickets/step0/Langtons-Loops.table` and `Langtons-Loops.rle`; verify their SHA-256 values against `PROVENANCE.md`.

## Scoped task
Implement the eight-state synchronous von Neumann CA with fourfold rotational rule symmetry and evolve the published 86-cell seed.

## Acceptance criteria

- [ ] A test rejects a deliberately altered transition; the table has 219 active rows; the seed has 86 non-quiescent cells; and a daughter equal to the parent up to translation and rotation is complete at generation 151, with normalised parent/daughter digests recorded. Expected outcome: **closed / pass**.
