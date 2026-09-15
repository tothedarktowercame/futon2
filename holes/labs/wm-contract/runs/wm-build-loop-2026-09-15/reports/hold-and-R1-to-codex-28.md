# Build loop on hold (Joe), and the R1 review result

From claude-2 to codex-28, 2026-09-15. This is a report. There is nothing
for you to decide.

## Joe's instruction: hold new build work

Joe (emacs-repl, 2026-09-15) said he will not greenlight anything new until
checklist TODO items actually start closing, or until he is convinced that
the new work items are exactly what is needed to close them.

My assessment, which I gave him:

- **P1 would not close WM-02.** Even fully built and rolled out, P1 would not
  tick WM-02 or R1.
  - No production predictor consumes the belief. Cascade G has no runtime
    form, and WM-05 is unwired.
  - With no admitted observations, the target belief stays at the declared
    initial distribution.
- **The persistence work grew one requirement at a time.** Each step was
  correct: first the store, then crash safety, caller authority and
  activation. Together they are persistence machinery built ahead of any
  consumer.
- **The limiting inputs for the first WM row are elsewhere.** They are WM-04,
  which has zero accepted independent observations, and WM-03, which has no
  interpretation (B) of the live actions.

**What is held:**

- P1b-2b and P1b-2c
- the activation/recovery procedure
- production genesis

P1b-2a (codex-2) was already running and will be recorded when it lands, but
nothing will build on it.

I have offered Joe a closability map: for every unchecked item, its specific
missing evidence and dependencies, ranked by what can be ticked soonest. The
next packet would target the first box that can genuinely be closed, and I
would bring it to Joe before dispatching. I am waiting for his answer.

## R1 independent review (codex-3, futon2 `4a2a1b20`)

codex-3 rejected R1 against the full contract I had requested, pending one
case (its F1). It changed no store source.

**Where R1 holds.** When valid `.writer.lock` metadata survives, R1 is
correct:
- no temp is adopted;
- no committed damage is masked;
- initialization-incomplete keeps precedence;
- no committed record can end up named as a temp.

**The failing case (F1).**
- The case: only temp files present, **no** lock file.
- What happens: `read-store` creates an empty lock, and strict parsing then
  reports `:damaged :parse-failure` rather than `:pending-recovery`.
- Where it comes from: codex-3's original lock bootstrap. It predates R1.
- Consequence: both states refuse, so nothing is adopted or lost.

**What resolving it would take.** Either the contract classifies absent or
malformed coordination metadata as damage with a specific reason, or the
code distinguishes a newly created lock from corruption.

**Rechecks.** codex-3's reruns match mine: gates exit 0 (16 tests, 292
assertions), and 17/17 mutants behave as predicted (14 killed, 3 known
survivors).

**Status.** F1 is recorded as open in the ledger. It is **not being fixed
while the store is held.**
