# C514 — :F5, the contract-fidelity gate

**Row:** `worklist.edn :F5`. **Date:** 2026-09-05. **Status:** done-unreviewed.

## What the row asked for

A checker that refuses (1) a worklist mint whose row lacks ancestry and
(2) a build plan that drops clauses of its commissioning ancestor without a
`:decision/not-done` record; jurisdiction forward from the gate's landing, with
the grandfathered count reported; wired into the loop's `worklist_check` path.

## What exists now

- `ancestry_check.bb` — the gate. Two parts, both refusing.
- `plan-ancestry.edn` — the plan registry, schema from
  `N-process-trap-recording-conventions.md:273` (§3) field for field.
- `worklist.edn :ancestry-gate` — the jurisdiction pin.
- `worklist_check.bb` — calls the gate and dies on its refusal, so
  `wm-build-loop.sh`'s pre-work `ledger_ok` is where a violating mint stops.
- `negative_controls.sh` §10 — nine negative controls and two positive.

## The two refusals, and where they come from

**(1) A mint with no ancestry.** `DRIFT-2026-09-05-account.md:78`: the genesis
ledger's "schema offers no parent-plan field", so row D5d consumed an A-structure
noun as a bound proxy and nothing refused — because there was nothing to refuse
*with*. A row under jurisdiction must name `:basis`, `:epic`, or a `:covers-key`
into a named registry, and the pointer must resolve.

**(2) A plan that drops an ancestor clause silently.** §3: a dropped clause
carries author, date and rationale. A clause of the ancestor that is neither in
`:clauses/inherited` nor recorded in `:clauses/dropped` has not been dropped, it
has been lost. §3's two other named refusals are here too: a plan with no
ancestor, and a plan that orders a consumer before its producer.

## Jurisdiction, stated exactly

`:ancestry-gate {:landed-after "e8b89211"}`. Every row id present in
`worklist.edn` at that sha is grandfathered: **171 of 171 rows today, 0 under
jurisdiction.** So on the current board the gate refuses no absence, and its
whole force on absences is prospective. That is what the row specified, and it
is stated rather than dressed up.

What the gate *does* refuse today is falsehood: **grandfathering excuses an
absence, never a falsehood.** Ancestry a grandfathered row does declare is
resolved like any other — 28 rows carry `:epic`, 136 carry `:covers-key` — and
that population found a real defect in the first run: five rows
(`:I2`, `:D61c`, `:U45`, `:U48`, `:U50`) sign registries in *other repositories*
(`:registry-repo "../p4ng"`, `"../mathlib4"`), and resolving `:registry-path`
against futon2 reported "file not found" for five valid trails. Fixed in the
checker, not in the ledger: a pointer the checker cannot resolve is not a pointer
that does not resolve — the same defect class `pointer_check.bb`'s roots list has
now hit eight times.

If the pinned sha cannot be read, the gate exits 1. A gate that cannot determine
its own jurisdiction refuses rather than waving the board through; "the checker
could not tell" is exactly the region T2a says drift goes to. Control 10i.

## The plan registry, and what it reports rather than rules

Six plans: the tech-lead charter, `P-validated-R5.md`, the TN edge-review note,
`worklist.edn` itself, `P-assured-process.md`, and `plan-ancestry.edn`.

Two carry **unaccounted ancestor clauses**, reported and not refused because they
are grandfathered:

| plan | ancestor | unaccounted |
|---|---|---|
| `P-validated-R5.md` | charter | all four charter clauses |
| `worklist.edn` | TN note | 5 of the TN's 9 (it inherits the four loop clauses) |

**No `:clauses/dropped` record is written for the Structure-A → Structure-B
displacement**, and that is deliberate. A dropped-clause record names an author,
a date and a rationale; `DRIFT-2026-09-05-account.md:125` searched
`BUILD-ledger.md`, `BUILD-status.md`, `DECISIONS-REGISTER.md`,
`DECISIONS-PENDING.md`, the charter, all BUILD packets, the two REPL archives and
git subjects 2026-08-30..09-05, and found no actor-authored decision. Writing one
would fabricate the decision the account could not find. The registry carries the
finding and the search trail in `:ancestor/note` instead. **No ruling was written:
`aif-equations.edn :choices` and `control-map-edges.edn :decisions` are untouched.**

Reflexivity (T5). `plan-ancestry.edn` is itself a registered plan, ancestor
`N-process-trap-recording-conventions.md:267`, and it is the one plan
**not** grandfathered — so it is the first artifact the full check applies to,
and every clause of its ancestor is accounted for: three inherited, one dropped
with author, date and a `:decision/not-done` rationale. The dropped one is §3's
surfacing rule (`:notify/required?` → `:notify/discharged-at`), which is declared
in the field list and **not enforced**: no plan registered today sets
`:notify/required? true`, so a checker for that path would have no population and
no control that could tell it from a no-op. Recorded as dropped rather than
quietly unimplemented; aspirational in the sense of
`N-process-trap-recording-conventions.md:30`.

## Controls (negative_controls.sh §10)

Nine negative — 10a ancestry-less mint; 10b ancestry naming a file that does not
exist; 10c a real file at a line it does not have; 10d a plan that drops an
ancestor clause with no record; 10e a dropped clause with no author; 10f an
inheritance the ancestor does not declare; 10g a plan with no ancestor; 10h a
consumer ordered before its producer; 10i an unreadable jurisdiction sha. Two
positive — 10j the board and registry as they stand, with the grandfathered count
and plan population pinned; **10k the wiring**: `worklist_check.bb` must itself
fail on a ledger the gate refuses, because checking `ancestry_check.bb` alone
would prove a checker exists and prove nothing about whether a violating mint can
enter a seat.

Three were mutation-tested: neutering the ancestry-less refusal reddens 10a,
neutering the silent-drop refusal reddens 10d, and neutering the call from
`worklist_check.bb` reddens 10k. 10h plants the reversal of `P-validated-R5.md:90`
— the R5 definition order, the very clause the DRIFT account finds lost —
so the historical defect is now expressed as data a checker reads.

## Gates

clj-kondo 0 errors / 0 warnings on `ancestry_check.bb` and `worklist_check.bb`
(linted per file: `bb` scripts share the `user` namespace, so linting the two
together reports collisions between them and not defects in either);
`futon4/dev/check-parens.sh` OK on both plus `plan-ancestry.edn` and
`worklist.edn`; `negative_controls.sh` PASS 69 negative / 40 positive;
`pointer_check.bb` 1466 pointers / 0 unresolved; `worklist_check.bb` exit 0.

## Not done, stated

No ruling — nothing written to `aif-equations.edn :choices` or
`control-map-edges.edn :decisions`. `gen_aif_dag.bb` not run into a publish
(TN §9a); the p4ng build was not run and nothing was regenerated or published.
No `src/` change in futon2, so no test run is claimed. No tick, no run lock,
nothing under `data/`. The §3 surfacing rule is registered as a dropped clause,
not implemented (above). The gate's clause-completeness check applies only where
the ancestor's clauses are enumerable — a registered plan, or an
`:ancestor-clauses` entry; where the ancestor is a passage of the commissioner's
words, only its resolution is checked, and the registry says so per row rather
than leaving it to be assumed.
