# C559 — the F2 falsifier, reconciled against the current library state

Date: 2026-09-07 · worklist row `:F11` slice 1 · run record
`runs/F11-find/02-reconciliation.edn`

`:F11`'s acceptance puts the F2 falsifier reconciliation first, because
`P-validated-R5.md:490-497` — the C61 amendment of 2026-08-31 — still records
the four `findF*` bindings as stale, "until the representations are reconciled
and the strengthened check passes". This slice reads that antecedent against the
library as it is today rather than as it was when the amendment was written.

## 1. The amendment's antecedent is discharged

Both conditions the amendment names were met, and neither by this slice:

| condition | where it was met |
|---|---|
| the two antecedent representations reconciled | `C73-find-strict-rebinding.md` (2026-08-31): the checker reports zero antecedent-representation mismatches, with `--negative-f1`, `--negative-f2`, `--negative-f3` each rejected |
| the strengthened check passes, bindings renewed | `C500-find-rows-close.md` (2026-09-03): four theorems, `wmFindSnatchF1Containment` (`Holes.lean:776`), `wmFindSnatchF2Receipted` (`Holes.lean:788`), `wmFindSnatchF3NonSelfCertifying` (`Holes.lean:803`), `wmFindSnatchF4Falsifiable` (`Holes.lean:817`) |

What the amendment diagnosed was that a receipt's presence could not establish
that the receipt receipted *what fired*: the runner evaluated a separately
hand-written antecedent while the receipt cited the authored clause, over 21
exact-text mismatches (`C60-find-snatch-acceptance-decision-2026-08-31.md`).
That gap is closed in the code rather than in prose — `find_snatch.clj:47-71`
evaluates the authored antecedent through `find-organise`, so there is one
carried object, and `find_snatch.clj:148-155` (`require-zero-drift!`) throws on
any residual mismatch instead of printing a count.

## 2. Re-measured today

futon3 `7c653bb9912f504b51d0b2ef8da4c32158ceb9ac`, `library/snatch` at 24
authored patterns:

```
clojure -Sdeps '{:paths ["checks"]}' -M -m find-snatch
  → F4 6/6; drift mismatches 0; wrote checks/find-snatch.edn
    find-snatch: PASS exit-convention=0-pass/1-fail
```

So the reconciliation holds at the current library, not only at the library it
was made against. Run twice, the check writes a byte-identical report
(`c11673ea71…` both times).

## 3. What moved since C500 measured it — the pin's line coordinates

C500 §3 recorded the pinned fixture as a snapshot the check no longer
reproduces: `:repository` had grown 18 → 24 while `:scenarios`, `:laws` and
`:drift` stayed identical. **The scenarios are no longer identical.** Against
the pin `839897ef…`, the live re-run `c11673ea…` differs in **96 of 96
receipts, across all 33 rounds that carry a receipt** (34 rounds are recorded;
`g2/snatcher` round 11 records `:selected []` with
`:absence :no-pattern-addresses-this-tension` and no receipt).

Every one of those differences is in `:if-lines` or `:however-lines`. **No
`:if-text`, `:however-text`, `:warrant :file` or `:route` differs anywhere** —
`runs/F11-find/02-reconciliation.edn`
`:difference-is-line-coordinates-only? true`, `:differing-fields
#:warrant{:however-lines 96, :if-lines 96}`.

The cause is two library commits since the pin's `:as-of` — futon3 `5c0b737`
(L7 rationale backfill) and `e58576c` (L18 v2b edging) — which inserted one or
two lines above the clauses of every snatch pattern. The shift is +0 to +2 per
file, and the pinned coordinates now land off the clause: `probe-before-committing`
is pinned at `:if-lines [15 16]`, but line 15 of
`library/snatch/probe-before-committing.flexiarg` is blank and line 16 is the
`+ IF:` label; the clause is at lines 17-18, which is what the live run records.

**What this does and does not reach.** F2 asks that a receipt cite the tension
clause it acknowledges. The live receipts do; the pinned ones cite the right
file and the right text at coordinates that have moved. No line number reaches
Lean: `FindSnatchRowLit` (`Holes.lean:354-361`) carries `scenario`, `round`,
`selected`, `receipted`, `nonSelfCertifying` and `absence` — the membership
lists the receipts *imply*, not the receipts' coordinates — and the string
`flexiarg` does not occur in `Holes.lean` at all. So the four theorems are
untouched by the shift: what they decide over is unchanged, which the run
record states refutably rather than by inspection (no `:route` and no
`:warrant/file` difference in 96 receipts). What the shift does reach is the
`fixture-sha256` the four docstrings pin: a live re-run now writes a different
file. That is the pinning defect C500 §3 reported and deferred to its own row,
observed here actually biting rather than predicted.

## 4. Method, and why the generator does not run the check

`find_snatch.clj`'s positive path recomputes its report from the live library
and **overwrites** `futon3:checks/find-snatch.edn`, the fixture the four Lean
docstrings name. A generator that invoked it would move the thing it measures,
which is why `u46_find_transcribe.bb` does not either. So the live re-run was
taken once by hand and committed as `runs/F11-find/01-find-snatch-live.edn`:

```
cp checks/find-snatch.edn /tmp/pin.edn
clojure -Sdeps '{:paths ["checks"]}' -M -m find-snatch
cp checks/find-snatch.edn <...>/runs/F11-find/01-find-snatch-live.edn
git checkout -- checks/find-snatch.edn      # tree left as found
```

futon3's working tree is left as it was found (`checks/find-snatch.edn` back at
`839897ef…`; the two untracked math-formalization flexiargs and the modified
`resources/sigils/patterns-index.tsv` were there before this slice and are
untouched).

`f11_f2_reconcile.bb` is then a pure function of that file and the pin, and
reproduces: two runs write a byte-identical `02-reconciliation.edn`
(`667b3421…`). Its control, `--negative`, rewrites one live `:if-text` to a
clause the library does not carry and confirms the classifier stops reporting
`:difference-is-line-coordinates-only?` — without it, "lines only" would be a
claim that could not fail.

## 5. Not done, stated

- **No ruling.** Nothing was written to `aif-equations.edn :choices` or
  `control-map-edges.edn :decisions`.
- **The pin was not repaired and the fixture was not re-pinned.** Re-pinning
  means choosing what the fixture's contract is — freeze it with a `--check`
  mode, or re-pin the four docstrings to a regenerated record and
  re-transcribe. C500 §3 already says that wants its own row; this slice adds
  the measurement that says which repair is now needed and how much it covers.
- **`gen_aif_dag.bb` not run into a publish** (TN §9a).
- The remaining slices of `:F11` — the conformant implementation at the s3e
  interface, the laws stated in Lean and witnessed on a real `find` over the
  committed library, and the `sorry` at `Holes.lean:264` — are untouched.
