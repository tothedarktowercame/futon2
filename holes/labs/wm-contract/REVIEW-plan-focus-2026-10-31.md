# Review: is PLAN-criteria-completion-2026-10-31 focused on what Joe asked for?

External reviewer (Claude cloud session `01DcURqCM4rXHzJMaHCQ8AhG`), reading
futon2 `main` at `26b2e26` and futon3c `master` at `1a4ffbc66`. Under the
2026-10-18 ground rules this is a review for codex-11 and Joe, not a plan
of its own. Joe's stated want, from the handoff: "a criteria-compliant
machine doing useful work, with repairs between individual runs, not an
interminable preflight/repair chain."

## What landed since the handoff (about 30 hours)

| Kind | Count | Examples |
|---|---|---|
| Charged clicks | 2 | 3415 grounded-progress (futon7 `de7bf835`), eaa9 incomplete (new injury adf04df0) |
| Machine fixes merged and reloaded | 4 | standing authority recovery `89caae4`; preflight no-verifier fix futon3c `657aa7d`; descriptor typing `f9d40b6`; one-attempt cap `5197d9f` |
| Accounting and planning commits | about 20 | CHECKPOINT (3 revisions in one hour), PLAN, P3 disposition, REPORT updates |
| Tracker rows | 22 → 24, 9 closed | two rows added, three closed on existing evidence |
| Evidence bytes added to Git | 151 MB, 79 files | `resources/wm/standing-authority/` |
| Agents committing | 7 | codex-11 23, zai-4 19, others 1 to 3 |

The recovery itself was focused and finished: the blocker named in the
handoff is gone, the ledger validates 130/0 under the standing verifier in
the serving JVM, and one effective injury remains. That is the right
outcome. The question is what the plan does next with 14 credits.

## Where the plan loses focus

1. **The spine is the accounting, not the loop.** Of eight packets, three
   that are dispatched now (P0 plan, P2 twenty-row matrices for two past
   runs, P3 six-row stale audit) produce documents, and the only packet
   that can satisfy Joe's criterion ("successor consumption without
   corrective intervention") is P6, last and conditional on P4 and P5.
   Every useful click so far (46, 49, 3415) was followed by a merge and a
   reload, so the streak has never been attempted. No audit of eaa9 or
   3415 can produce that witness; only a frozen sequence can.
2. **P4 spends the next credit on a self-heal of a cause already fixed
   directly.** adf04df0's cause is the abbreviated commit descriptor;
   `c96bd98` typed it and the namespace is reloaded. The 2026-10-29 rule
   is one self-heal attempt, then fix upstream and log; here upstream is
   already fixed, so the remaining need is discharge, which the medic
   route did in minutes for 9c622038, ad038c48 and 2aef7e0c on 10-29. A
   self-heal author/review/enact cycle for an already-fixed cause costs the
   same credit and hours, and its claimed criteria coverage (A1 to E4
   minus B and D1/D2) overstates what a repair run exercises.
3. **Row count grows as fast as closures.** Two rows were added while
   three closed. The plan's rule that a new defect "enters the existing
   queue" with six fields guarantees the tracker keeps growing; it has no
   rule for what does not get a row.
4. **Accounting churn.** The checkpoint was corrected three times in one
   hour (`4774d86`, `fffe320`, `e6d33ef`) as parallel packets reported.
   Each correction is honest, but a per-packet cadence produces more
   reconciliation than evidence.
5. **151 MB of evidence blobs in Git** was a reviewed decision for
   portability and is not wrong, but it is a permanent cost on every
   clone and should not become the pattern for future adoptions; the
   future-adoption contract in the recovery branch should keep bundles
   small by construction (pins plus bytes only for receipts, not whole
   run records).

## A tighter order for the same 14 credits

1. **Discharge adf04df0 by medic with the fixing commit on record**
   (`c96bd98`), as on 10-29. One credit or none, depending on route; no
   author contact. If codex-11 judges that the self-heal route must be
   exercised for its own witness, do it once but do not credit it to task
   criteria.
2. **Freeze and run a three-click ordinary sequence now** (P5 and P6
   merged into one pre-registration): no merge, no reload, no state edit
   between clicks; a stop ends the sequence and is recorded as such;
   repairs happen after, not inside. This is the only experiment that
   can answer E2, E3, E4 and T12 together, and it is three credits.
3. **Extract the twenty-row matrix from that sequence**, once, instead
   of from eaa9 and 3415 now. P2 as dispatched will be superseded by the
   sequence's runs within a day.
4. **Gate new tracker rows on the sequence's path.** A defect gets a row
   only if it names a first divergence on a run of the frozen sequence;
   everything else goes to a residual list without owner or packet. P3's
   remaining audits belong there.
5. **One accounting commit per click**, after its receipt, instead of
   one per packet. Parallel doc packets should write their own files and
   let the per-click commit reconcile.

That spends four credits, leaves ten, and would give Joe within a day the
one thing the campaign has not yet produced: a declared sequence where
the machine either chained two useful runs or stopped for a reason it
wrote down itself.

## Two smaller things

- 象-2's report names a Lean gap: the cap's two typed stop paths are not
  anchored. That should be a small standalone module in the shape of
  `AlreadySatisfiedReview.lean`, not a reason to delay the sequence.
- The plan's "at most 5 parent charges, leaving at least 9" is a good
  ceiling. Keeping it is more important than finishing P7.
