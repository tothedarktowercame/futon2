# C473 — I5 slice (c): the R8 scalar F removed, and the run that shows the key absent

Owner: claude-20 (wm-edge worklist seat). Author ≠ reviewer: this needs a second read.
Ledger row: `worklist.edn` `:I5`, slice **(c) the removal and the run**.
Slice (a) is `C471-f-scalar-readers.md` (futon2 `b5eebb5`); slice (b) is
`C472-f-scalar-disposition.md` (futon2 `e25b1ae`). C472 §6 listed six things
(c) owed, in order; this note answers them in that order. Every claim carries a
`file:line` pointer, a command that was run, or the words "not found".

Commits: futon2 `5a66411`, p4ng `25c7303`.

## 1. The retag: option 1, and what it costs

C472 §2 tabled four options for the `:R8` route tag once
`compute-variational-free-energy` goes. **Option 1 is taken**: the tag stays
where it is (`war_machine.clj:4918`) and names `fe/compute-prediction-error`.

The reason is narrow and I would rather state it than dress it up. Option 2 —
C472 called it "the honest order" — puts the `:R8` tag before the `:R7` tag, in
the order the values actually flow. It produces two `:unmapped` hops, and
`run3_conformance.bb` exits 1 on any unmapped hop (`run3_conformance.bb:196-200`,
`:verdict :not-conformant` when `(or (seq refutations) (seq unmapped))`). One of
the two, `R8→R7`, is unmapped only because run3's `topology` (`:52-62`) does not
read `:decisions :adds` while `gen_live_topology.bb:190-200` does — the checker
gap C472 §2 measured. C472 assigned that repair elsewhere: "it is a RUN-class
repair, not this row's". So option 2 cannot be completed inside this row's
acceptance without doing work the row was told not to do, and the acceptance's
own bar ("a run shows the key absent", and RUN3 green on it) would fail.

**What option 1 costs, said plainly.** Two things, neither new but both moved
onto a quantity where they read worse:

1. The tag now names ε, and ε ran at `war_machine.clj:4840` — *before*
   `update-precision-state` (`:4849`) and `apply-arena-belief-events` (`:4896`),
   whose `:R7` and `:R3` tags are emitted at `:4914-4915`, on the line above it.
   Retroactive tagging is the existing convention, recorded at
   `control-map-edges.edn:186` (D6: "the tag ORDER R7, R3, R8 records where the
   tags were placed, not where the values flowed"). What is new is that the
   retroactive tag now names something that ran before the two tags it follows,
   rather than after them.
2. The `:R3 → :R8` row in `:route-measured-drawn` (`control-map-edges.edn:120-124`)
   has `:via "futon2.aif.free-energy/compute-variational-free-energy"` and a
   `:basis` — "belief-update prediction errors feed the present-fit free-energy
   calculation" — that is backwards for ε. **Reported, not repaired.** That row
   records what WM-RUN2 measured on 2026-08-30 against the receipt it names
   (`tick-run-record-2026-08-30.edn`), and that measurement is still true of that
   receipt. Repairing a signed registry row is the supersede dance, not this
   slice. The same string is carried derivatively at
   `futon2/holes/labs/wm-contract/edge-census.edn:82`.
3. And the one C472 §2 named that option 1 does not fix: the retagged `:R8` goes
   on standing between the `:R3` and `:R5` tags, so the `R3→R5` pair — which no
   decision adds, and which the registry routes the long way round
   (`R3→R1→R4→R5`: `aif-equations.edn:108-110`, `:111-115`, `:122-124`) while the
   code hands this tick's belief straight to `rank-actions`
   (`war_machine.clj:4924-4932` → `:5002`) — **stays covered**. Removing F did not
   uncover it; retagging R8 kept it covered. C472's sentence still stands and is
   still unacted: the F computation was concealing a gap between the drawing and
   the route, and after this slice the retagged ε does the concealing instead.

No `:decisions` entry is written by this slice. Under option 1 there is no
`[:R3 :R5]` hop to record, and C472 §6's step 2 was explicitly conditional on
option 2.

## 2. What was removed (futon2 `5a66411`)

| what | was | now |
|---|---|---|
| the function | `free_energy.clj:184-205` | gone; ns docstring records why (`free_energy.clj:7-12`) |
| the per-tick call | `war_machine.clj:4913-4914` | gone |
| the `:R8` route tag | `war_machine.clj:4915`, naming the removed fn | `war_machine.clj:4918`, naming `compute-prediction-error` |
| the judgement key | `war_machine.clj:5241` | gone |
| the record key | `trace.clj:482` | gone |
| the producer contract | `:r8/stored-f-controller-v1` (`trace.clj:247`) | `:r8/retired-f-controller-v1` (`trace.clj:257`) |
| the trace schema version | 20 | 21, with its ledger entry (`trace.clj:241-247`) |

## 3. The era, which is the part that needed design

C472 §4 established that the r8 contract is three conjuncts, not one:
`record-conforms?` required `(= stored? gain?)`, `(= stored? controller?)` and
`(= stored? current?)`. F retired while the selection gain and the controller-map
free-energy shape stayed, so an F-less record failed all three. **Bumping the
contract constant would not have helped** — it would have moved the record from
failing one conjunct to failing the other two.

What landed instead (`checks/r8_f_contract.clj:31-46`): each era declares what it
requires, and the conjuncts are read off the era rather than off each other.

```
:pre-stored-f         {:f false :gain false :controller false}
:stored-f-controller  {:f true  :gain true  :controller true}
:retired-f-controller {:f false :gain true  :controller true}
```

`contract-era` (`:186-201`) resolves a declared contract through `contract-eras`
(`:27-31`); an unrecognised contract is `:malformed`, not silently legacy, and
`unknown-producer-contract-is-malformed-not-legacy` (`test/r8_f_contract_test.clj`)
holds that. Two violation classes are new — `:era-requires-gain` and
`:era-requires-controller` — because they are the case no earlier era could
produce: a field the era demands, absent, while F's absence is correct. They are
summed into `:conjunct-violations :eraRequiredFieldAbsent`.

The era **table** is unchanged at two sides: `:retired-f-controller` is a
post-boundary contract, not a third side of the 20260714 date boundary
(`r8_f_contract.clj:322-326`).

**The generated Lean.** `lean-fixture-text` emitted
`∀ t ∈ wmTraceR8Generated, (storedF.isSome ↔ selectionGain.isSome) ∧ … ∧
(storedF.isSome ↔ 20260714 ≤ fileDate)`. All three conjuncts are false for a
retired-era record, and `R8TickLit`
(`mathlib4/DarkTower/WarMachine/Holes.lean:589-597, 626`) carries no contract
field — so the discriminator the Clojure side uses is not expressible there. The
retirement day is also **mixed**: records written earlier on 2026-09-01 carry F
and later ones do not, in the same `fileDate`. The theorem is therefore stated
over `wmTraceR8StoredEra`, the records that predate the retirement day, with the
exclusion named in the generated text rather than left for a reader to notice.
It is weaker and true; the artefact that enforces the contract over *every*
record is `:r8EraBoundary :violations`, which reads the contract.
`R8-D2-report.lean` is on no lakefile — searched; the only references to it are
`R8-D2-findings.md`, `C471`, `worklist.edn` and the generator itself — so this is
about not writing a false theorem, not about a build.

**Not moved: the pinned census** `{:missing-F-computable 755 :stored-F 32
:insufficient-inputs 5}` (`r8_f_contract.clj:16-18`). It was already 85 apart from
the live corpus before this slice (measured at HEAD: `{:insufficient-inputs 5,
:missing-F-computable 755, :stored-F 117}`), it feeds `:recorded-census-delta`
which is evidence rather than a check, and no `:checks` entry reads it
(`:snapshot-expected-census` is nil in `lint-paths`). C472 §4 said it "moves with
it"; it does not need to, and moving a pin nothing enforces would be a change
that looks like maintenance and is not.

## 4. Three byte-identity controls, and why their anchors did not move

`trace_test.clj` holds three controls that compare today's record against a
*pinned historical implementation*, loaded out of git: `f-pi-dark-off-…`,
`policy-trace-details-flag-off-…` (against a golden resource file) and
`beta-dark-off-…`. Each backs a claim of the form "this flag adds nothing to the
record". Each broke on this commit, correctly: the record legitimately lost a key
and changed its producer contract.

Re-pinning the anchors to HEAD would have made every one of them pass, and would
have replaced the claim with "the last commit changed nothing" — the rot
`pre-beta-dark-trace-sha`'s own docstring (`trace_test.clj:29-38`) was written to
prevent. So the anchors stay and the comparison drops exactly the two keys this
slice retired (`i5-retired-keys`, `trace_test.clj:66-84`). Everything else is
still under the control, byte for byte.

**A fourth control broke for a different reason, and it is the more interesting
one.** `war_machine_test.clj`'s `previous-portfolio-step-fn` compiles a
*historical* `war_machine.clj` (pinned `e7a9bb6^`) against **today's** libraries.
Every revision that predates this slice calls `fe/compute-variational-free-energy`,
so removing a public var from `futon2.aif.free-energy` makes all of them fail to
compile — on a line the control does not exercise (the failure was
`Syntax error compiling at (4594:9)`, which is the call site in the historical
file). Confirmed by running that namespace at HEAD (44 tests, 0 failures) and with
the change (1 error). The loader now neutralises that one call in the source it
compiles, next to the ns rename it already performed. **The general point:
"load a past revision and compare" couples every past revision to today's library
API, and a deletion is what finds that out.**

## 5. The paper (p4ng `25c7303`)

`detect_drift.py:117-119` pinned `compute-variational-free-energy` by name, and
`named_form` (`:246-251`) raises `ValueError` when a pinned form is absent — which
takes the whole drift run down, not one unit. C471 §4 measured that by renaming
the defn; this slice would have triggered it. The pin follows
`compute-prediction-error` in the same file, and a second pin covers
`f-pi-for-candidate` (`policy_free_energy.clj:41`).

`app-eqtutorial.tex`'s T-F entry no longer claims a per-tick implementation. The
dated 2026-08-31 paragraph in `sec-vetting-corrections.tex` is **left as written**
— it records what was true when it was made — and a 2026-09-01 paragraph records
the retirement.

**Not re-stamped.** `detect_drift.py` now reports `free_energy.clj` DRIFTED and
`policy_free_energy.clj` NEW, so the sentences citing them re-enter vetting. That
is the signal working. (`policy.clj` and `futon2/holes/` were already drifted
before this slice.)

Prose that asserted the field exists: `data/r18-badges.edn:218` and the block it
generates at `holes/aif-wiring-explainer.html:196`, regenerated with
`bb scripts/r18_badges_to_js.bb --write` and re-checked with `--check`.

## 6. The run

Stage **S5**, `runs/2026-09-01-s5/`. Four ticks, one RUN12 lock, at futon2
`5a66411`:

    clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj   # PASS, 0 POSTs, 0 .admintoken reads
    bash holes/labs/wm-contract/wm_run.sh 4 14 claude-20               # done rc=0

**The key is absent, not nil**, on all four records — checked with
`contains?`, not `nil?`, because a `nil` value would satisfy the acceptance's
words and not its point. Each carries `:producer-contract
:r8/retired-f-controller-v1` and `:trace-schema-version` 21; `:selection-gain`
is present and the `:free-energy` map still carries `:controller-score` on all
four, which is the era's other two requirements holding while F's is inverted.

**RUN3, which is the acceptance bar and the test of §1:**

    bb run3_conformance.bb runs/2026-09-01-s5
    run3: selection by-run-id (4 receipts, 4 run ids, dates 2026-09-01)
    run3: 4 records, 4 routes, 36 hops (9 distinct)
    run3: ruling-unrealised 1 (R5->R6); excluded-dependency-grain 1 (R2->R7)
    run3: drawn 2 (R7->R3, R8->R5); route-measured 5 (R12->R2, R14->TRACE,
          R20->R12, R3->R8, R6->R14)
    run3: CONFORMANT

`conformance.edn`: `:verdict :conformant`, `:unmapped nil`, `:refutations nil`,
`:sha "5a66411"`, `:selection :by-run-id`. **Zero unmapped hops** — which is
option 1 doing exactly what §1 said it would, and is not independent evidence
that option 1 was right. `R3→R8` classifies `:route-measured` with the same
`:from`/`:to` pair as before, because run3 classifies pairs and does not read
`:via`; the `:via` in the receipts is now
`futon2.aif.free-energy/compute-prediction-error`.

**The r8 contract over the whole corpus, after the run:**

    bb checks/r8_f_contract.clj --report /tmp/r8-after-run.edn
    trace files: 55 · trace forms: 882
    dispositions: {:insufficient-inputs 5, :missing-F-computable 760, :stored-F 117}
    r8-f-contract: PASS
    bb checks/r8_f_contract.clj --negative …   # PASS, era mutation rejected

The five new records land in `:missing-F-computable`, the same disposition as
the pre-boundary era — retiring F did not add a fourth arm to the Lean
`r8Disposition` (`Holes.lean:604-611`), which is why mathlib4 needs nothing.

**One record from this slice is outside S5.** The first launch of `wm_run.sh`
was killed by a harness timeout partway through its first tick. The script's
EXIT trap released the lock (verified: no `.run-lock` afterwards), and one
record survives at `2026-09-01T22:48:41Z`, `:run/id 84ca8231-…`, with the same
contract and the same absence. run3 selects by `:run/id`, so it is out of S5 by
construction. Recorded here rather than tidied away.

**The in-tick `:route-verdict` disagrees with run3 and always has.** Each
receipt carries `{:hops 9, :conformant 3, :unmapped 6}` from war_machine's own
route check, which calls `R20→R12`, `R12→R2`, `R2→R7`, `R3→R8`, `R6→R14` and
`R14→TRACE` unmapped — every pair run3 classifies `:route-measured` or
`:excluded-dependency-grain`. So the in-tick checker reads `:edges` and not
`:route-measured-drawn` or `:decisions`. Pre-existing: the 2026-09-01 receipts
from before this slice (e.g. `25f17ffb`) carry the identical six. **Reported,
not repaired** — it is the same family as C472 §2's run3-vs-gen_live_topology
gap (three checkers, three different readings of one registry), and it is a
RUN-class repair, not this row's.

## 7. Gates

futon2 (`5a66411`): clj-kondo 0 errors / 0 warnings on the seven changed
Clojure files; `check-parens` exit 0 on the same seven; `clojure -T:build ci`
exit 0; `clojure -M:test -m cognitect.test-runner -d test/futon2` — 965 tests,
5813 assertions, 0 failures, 0 errors; `bb test/r8_f_contract_test.clj` 13
tests / 65 assertions; `r8_f_contract` PASS and its negative control PASS;
`contract_lint_test`, `control_map_lint_test`, `r2_channel_contract_test`,
`r9_independence_test`, `preemptive_repair_lint_test`,
`strict_contract_qualification_test`, `positive_proof_receipt_test` all pass;
`variational_free_energy_witness`, `bayes_factor_threshold_witness`,
`model_reduction_free_energy_change_witness` all PASS.

p4ng (`25c7303`): `negative_controls.sh` PASS (16 negative, 10 positive);
`pointer_check.bb` 411 pointers, 0 unresolved; `detect_drift.py` exit 0.

**Two things found red that are NOT mine, verified by running them at HEAD
with the change stashed:**

1. `clojure -T:build ci` — the repository's own hermetic gate — discovers
   **three tests**. `cognitect.test-runner` under `-T:build ci` finds only
   `positive-proof-receipt-test`; the 965 tests under `test/futon2` run only
   when the directory is named (`-d test/futon2`). So "CI is green" has been
   carrying almost none of this repository's test weight. Found because this
   slice's changes were invisible to it.
2. `checks/trace-schema-compatibility` FAILs, at HEAD and with the change
   alike, for a reason that is a date: its range and `latest-trace-record`
   calls are hard-coded to end 2026-08-31 (`:15-16`), so with 2026-09-01
   records present `read-range` is 803 of 877 and `:latest-agrees?` is false.
   The workspace gate invokes only its `--negative` arm
   (`wm_workspace_gate.clj:433`), which passes.

## 8. What a reviewer should attack

- **§1 is the soft spot.** Option 1 was chosen partly because option 2 needed
  work C472 put outside this row. A reviewer who thinks the honest tag order is
  worth a red RUN3 and a run3 repair should say so; the retag is one string at
  `war_machine.clj:4918`.
- **The generated Lean theorem got weaker** (§3). Restricting it to
  pre-retirement records is honest but it is a smaller claim than the one it
  replaced, and the alternative — a contract field on `R8TickLit` — is a
  mathlib4 change nobody has asked for.
- **The pinned census was not moved** (§3), against C472 §4's expectation.
- **Four historical-comparison controls were amended rather than re-anchored**
  (§4). The amendments are load-bearing in one direction: if a future change
  removes a key it should not, the `i5-retired-keys` list is where it would be
  hidden. It has two entries and they are named in the commit message.
