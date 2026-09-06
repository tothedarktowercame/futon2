# C542 — F12 gate-repair slice: closing the two reds F12's own commits left

Date: 2026-09-06. Row `:F12` (`worklist.edn`), slice named `gate-repair`.
Slices 1–3 are `C539`, `C540`, `C541`.

## 0. Why this slice and not the next one C541 named

C541 §6 leaves the row with no startable slice: slice 4 (the carrier
reconciliation and the `sorry` at `Holes.lean:861`) waits on D1, and O4 over the
library waits on D2, D3 and on the zaif rule table's then-correspondence. What
C541 did **not** say is that the row's own two previous commits had left the
repository gate red, and that the worklist protocol's pre-commit step
(`negative_controls.sh`, `pointer_check.bb`) therefore fails for every lane, not
only this one. Measured at the start of this slice:

```
negative_controls: FAIL -- pointer_check: a registry pointer does not resolve
pointer_check: 2419 pointers in 6 files, 1 unresolved
  UNRESOLVED zaif_cascade_gate.clj:435 (file not found)
contract-authority-current: FAIL :failures [:contract-authority-not-last-source-change
                                            :contract-source-not-current]
flip_readiness_check: FAIL (2 problems)   ;; 7/7 flips BLOCKED-ON contract-pin
```

Both are this row's. Neither needs a ruling from anyone.

## 1. Red one — a pointer this row's own ledger entry made unresolvable

`C541` cites the zaif gate's primary-round predicate as
`futon3c:scripts/zaif_cascade_gate.clj:435`, and slice 3 copied that pointer into
the `:F12` `:progress` field. `worklist.edn` is one of `pointer_check.bb`'s six
scanned registries, and its roots list carried five futon3c directories
(`src/futon3c/transport/`, `/agency/`, `/agents/`, `/aif/`, `dev/futon3c/`) and
not `scripts/`. The pointer is **true**: the file is 49044 bytes at that path and
line 435 is

```clojure
primary? (fn [s] (and (paired? s) (not (oracle-uncertain? s))))
```

which is exactly the three-conjunct primary predicate `C541` §1 describes, and
`:559-562` are the `acting-order-after` / `score-before` / `score-after` lines of
the precedence exchange. So the repair is the twelfth instance of the defect
class that file already records eleven of: **a pointer the checker cannot resolve
is not a pointer that does not resolve.** `$HOME/code/futon3c/scripts/` is
appended to `roots` — appended, not inserted, so no existing first-match
resolution can change (`resolve-file` is `(first (filter exists ...))` over
`roots` in order). After: `pointer_check: 2419 pointers in 6 files, 0 unresolved`.

**A second finding, recorded rather than repaired.** Slice 3's `C541` §5 reports
`pointer_check 2416/0` and that is honest: the check ran before the ledger row
carrying the pointer was written, because the ledger row is written *after* the
work commit by protocol step 5. The row's own text is the last thing written and
the only thing not gated. Every slice of every row is in that position; this is
the first time it produced a red. Not fixed here — the fix is a change to the
loop's step order or to the row-writing step, which is not this row's.

## 2. Red two — C176 phase two, owed by the lane that changed the Lean

`C541` §5 called this one "the contract regeneration workflow (`C176`), not this
row's acceptance." On the text of `C176` that disposition is wrong: it says a
Lean lane "may not report delivery complete until it has (1) committed the
`Holes.lean` change; (2) regenerated the contract against that committed source
authority; (3) rebound affected witness fragments and merged the registry; (4)
passed `contract_authority_current`, strict contract lint, and the workspace
gate." The lane that committed the `Holes.lean` change is F12 slice 2
(mathlib4 `61c4825dc3`, the transcription block at `Holes.lean:924-1048`). Phase
two is this row's, and `C176`'s own wording — "manual in sequencing, not optional
in acceptance" — is what makes the red bounded rather than permanent.

Ran `mathlib4/scripts/emit-contract.sh` on the clean tree at `61c4825dc3`
(`lake build` 2704 jobs, 0 errors). `DarkTower/Contract/Emit.lean:52-62` derives
the authority as `git log -1 --format=%H -- DarkTower/WarMachine/Holes.lean`,
which is the C177 comparand the checker uses, so the regenerated authority is
`61c4825dc3…` by construction rather than by hand.

**The regeneration changed the authority and nothing else.** Structural diff of
the committed contract against the regenerated one: `source` differs, and
`declarations` compares **equal** as a value (124 declarations before and after).
Slice 2 added four theorem/def declarations to `Holes.lean` and registered none
of them in `registry`, so `C176` step 3 — rebind affected witness fragments — has
no affected fragment to rebind *from this change*. That is a measurement, not an
assumption; see §4 for the fragments that were already stale before it.

After:

```
contract-authority-current: PASS-CONTENT-ONLY (event-free unverified) :failures []
contract-authority-current: negative-control PASS
flip_readiness_check: PASS   ;; contract-pin off all 7 flips' BLOCKED-ON lists
negative_controls: PASS (133 negative, 53 positive; shared registries untouched)
```

`FLIP-READINESS.md` and its sidecar needed **no edit**, and that is a check
rather than a convenience: the committed document listed `contract-pin` on no
flip, i.e. it recorded the pre-slice-2 green state, and the checker's two
`PROBLEM` lines were the derivation disagreeing with it. Restoring the pin
restored the agreement. Had the repair been wrong in either direction the
document would have had to move.

## 3. Controls

- **The new root makes the pointer checked, not merely reachable.** A copy of
  `worklist.edn` with `zaif_cascade_gate.clj:435` rewritten to `:99999` (plant
  verified present, 1 occurrence) run through `AIF_EXTRA` reports
  `UNRESOLVED zaif_cascade_gate.clj:99999 (end beyond file)`. Without this, a
  root that resolved the name would look identical to a root that also validated
  the line.
- **The contract-authority negative control passes** — it substitutes a
  zero-sha authority and the checker rejects it (`recorded-authority
  "000…0"`, negative-control PASS), so the green in §2 is not a checker that
  stopped checking.
- **`negative_controls.sh` 133/53 with `shared registries untouched`**, and its
  own stale-pin control (`negative_controls.sh:533`) still requires a planted
  stale pin to block the flip gate, so the repair did not disarm the instrument
  that reports it.

## 4. What is still red, and the evidence that this slice did not cause it

Strict contract lint and the workspace gate both fail, and both failed **the same
way before the regeneration**:

- `contract_lint.clj --strict` against the OLD contract at authority `69721b12…`
  and against the NEW one at `61c4825d…` produce byte-identical verdict blocks:
  the same 28 `strict-stale-declarations` (`modelReductionFreeEnergyChange` …
  `Cohort`) and the same `strict-stale-remediation {:rerun-and-rebind 37}`.
- `checks/ambiguity_witness.clj`, one of the 56 checks the workspace gate reports
  failing, returns `FAIL` under the old contract and `FAIL` under the new one
  (run both ways by checking the contract out and restoring it).
- The workspace gate run in this slice additionally carries
  `:verdict-qualification :repository-basis-moved` and `:basis-status :moved`,
  because it ran against a tree this slice was editing. It is reported here as a
  measurement of the failing set, not as a verdict.

So the 37 rebinds are an older condition that predates slice 2 and is not
discharged by regenerating the authority. Naming it is this slice's whole
contribution to it.

Also left alone: `p4ng`'s seven `sec-*-generated.tex`, `war-room-tetrahedron.svg`
and `wm-status.pdf` are modified in the working tree from a publish run at
23:15 that predates this slice's first write, recording
`authority 69721b1268…  (checkout HEAD 61c4825dc3)`. Regeneration into a publish
is the gate rule's (TN §9a) post-review step, not a slice's.

## 5. Where the row stands

Unchanged on its acceptance: no O-law was stated, witnessed or discharged here,
and the `sorry` at `Holes.lean:861` is untouched. Slice 4 still waits on D1; O4
over the library still waits on D2, D3 and the rule table. What has changed is
that the row no longer holds the repository's pre-commit gate red while it waits.

**One thing for the reviewer to weigh, which is not a ruling.** All three of the
row's open questions are being held as advance rulings for Joe. The ledger's own
checker embeds his rule of 2026-09-01 against exactly that
(`worklist_check.bb:17-45`): "a choice the theory does not settle gets BRANCHES
BUILT AND RUN, not an advance ruling. Only a choice whose arms cannot be run
reaches him as a question." D1's three arms — widen `Cascade`, retype
`organise`'s codomain, state the laws of a `Cascade`-plus-`Repository` pair — are
each a Lean elaboration against the fixture slice 2 already built
(`Holes.lean:974`), so they are runnable, and whether they *are* is the question
the reviewer should settle before the row waits any longer. D3 may be the
residue the rule describes, since its arms differ in what the apparatus is
allowed to become rather than in a number; D2's three denominators are already
measured (`C541` §2).

## 6. What was checked

- `pointer_check.bb` before (1 unresolved) and after (0), plus the plant control.
- `contract_authority_current.clj` before (2 failures), after (PASS), and its
  `--negative-control`.
- `flip_readiness_check.bb` before (FAIL, 2 problems), after (PASS).
- `negative_controls.sh` before (FAIL at the pointer control), after (PASS
  133/53).
- Old-vs-new comparison for `contract_lint.clj --strict` and
  `ambiguity_witness.clj`; structural diff of the two contract JSONs.
- `clj-kondo --lint pointer_check.bb`: 0 errors, 0 warnings. `check-parens` on
  the same file: exit 0.
- `lake build` 2704 jobs, 0 errors, on the unmodified `Holes.lean`. No Lean was
  written by this slice.
- Not run: `gen_aif_dag.bb` and the publish generators (TN §9a).
