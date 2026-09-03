# U27 — every open hole in the fresh contract, against the work that now exists

Date: 2026-09-03. Item `:U27` (CLOSE THE RED, part 2). Author ≠ reviewer: this
needs a second read. Machine-readable companion: `audit.edn` (16 rows, 4
findings, every claim carrying a `file:line` pointer or a run-record id).

Contract audited: `mathlib4/DarkTower/WarMachine/holes-contract.json` at the
U26 pin `6de47bd0503dd4e6090c420ca32d8e7474438d7e` — 124 declarations, 109
closed, **15 holes**. The registry the paper reads,
`variable-situation-accounting.edn`, carries **16** `:open-hole` rows: those 15
plus one glossary-side hole (`Strategic mission selection`, promoted by U14 and
reported outside the declaration columns).

## 1. The result, first

**Holes closed by this audit: zero.** That is not a shortfall against the row's
"close the READY ones" — it is what the evidence says, and three findings say
it. The row also asked for the fence typing (Joe's 2026-09-03 amendment), and
that is delivered for all 16 rows.

| | pre-run-closable | run-gated |
|---|---|---|
| contract declarations (15) | 9 | 6 |
| + glossary-side (1) | 9 | 7 |

Readiness, separately from the fence: 13 `:not-ready`, 2 `:contested`
(`nonDegenerateAblationLaw`, `policyPosteriorImportsPolicyF`), 1
`:witnessed-and-held-open` (`wmRunsOnce`).

**Every number above is this audit's own, as of 2026-09-03 against contract
`6de47bd050`.** They are a record of what the audit found, and are not re-derived
when a row later takes a verdict; the live counts are in
`variable-situation-accounting.edn`. What has moved since is listed in
`:counts :verdicts-since` in `audit.edn` and in §6 below.

## 1a. Disposition since the audit (2026-09-03)

Five rows have taken verdicts: `nonDegenerateAblationLaw` (`:U45`, below) and
the four `find` rows (`:U46`, §1b). `nonDegenerateAblationLaw` moves `:contested` →
`:closed-under-criterion`. The audit could not close it because no criterion for
`hole → closed` was written down (F1) and the declaration's author had
deliberately demoted it (F2); Joe's J9 ruling the same day recorded the criterion
in `RUNBOOK.md` (futon2 `a2641b3`) and dispositioned this hole by name, and
worklist row `:U45` executed the close.

Which legs of the criterion carried it, since "meets the criterion" is three
different claims:

- **(3) Lean transcription — met.** `wmRecordedAblationNonDegenerate`
  (`Holes.lean:229`) proves the predicate over the pinned exact-dyadic table, no
  `sorry`. Unchanged since `86186c3744`.
- **(2) Rejecting witness — met, and at the current contract sha.**
  `checks/ablation_exact_dyadic_witness.clj` exits 0 over the fixture, and
  `--negative` — which removes the minimizer separation — is rejected. Re-run for
  `:U45` and re-stamped to `4bc55c5968`.
- **(1) Persisted record — inapplicable, not met.** The declared observation is a
  recorded score table, not a run observation. Said explicitly rather than
  counted as satisfied. The fixture is an exact-dyadic transcription of the
  `snatcher-dominant`/`g1` case of the persisted record
  `futon3:checks/ablation-snatch.edn`; all ten scores were decoded and compared
  against that record for `:U45` and are equal.

Effect on the counts a reader of Box 2 sees: contract holes 15 → 14, accounting
`:open-hole` rows 16 → 15, declaration fence `:pre-run-closable` 9 → 8. No other
row moved and no other declaration was edited.

## 1b. The four `find` rows (`:U46`, 2026-09-03)

`findF1Containment`, `findF2Receipted`, `findF3NonSelfCertifying` and
`findF4Falsifiable` move `:not-ready` → `:closed-under-criterion`. The same J9
ruling dispositions all four by name; what §4 recorded as remaining — "the pinned
`FindReceiptRow` is not in Lean" — is what `:U46` supplied.

- **(3) Lean transcription — met, one commit per declaration.** The pinned record
  `futon3:checks/find-snatch.edn` (sha256 `839897ef…`) is transcribed as
  `SnatchPattern` (18 authored ids), `snatchRepository`, `FindSnatchScenario`
  (6), `findSnatchZeroMass`, and `FindSnatchRowLit` with two tables —
  `findSnatchRounds` (34 rows, the grain `find_snatch.clj:157-171` iterates) and
  `findSnatchScenarios` (6 rows carrying `:selected-union`, the grain
  `find_snatch.clj:172-177` iterates for F4). It is generated from the fixture by
  `futon2:holes/labs/wm-contract/u46_find_transcribe.bb`, not hand-typed.
  `wmFindSnatchF1Containment` (`Holes.lean:776`), `wmFindSnatchF2Receipted`
  (`:788`), `wmFindSnatchF3NonSelfCertifying` (`:803`) and
  `wmFindSnatchF4Falsifiable` (`:817`) each discharge by `decide`, no `sorry`,
  each with a soundness lemma carrying the Boolean check to the set-level
  declaration so the decision is about the recorded rows and not about lists.
- **(2) Rejecting witness — met, at the current contract sha.**
  `futon3:checks/find_snatch.clj` exits 0 with drift mismatches 0, and all five
  controls are rejected: `--negative-f1`, `--negative-f2`, `--negative-f3`,
  `--negative-f4`, `--negative`. Re-run for `:U46` and re-stamped to
  `0bab8f813f`. Two pins in that fragment were stale and were corrected from the
  re-run rather than carried (`:run-sha`, and a `:check-sha` predating futon3
  `4e1c410446`, which moved the check onto the generic find/organise path).
- **(1) Persisted record — inapplicable, not met.** The declared observation is a
  recorded find-receipt table over authored library text, not a run observation.

**One thing the close does not give, stated rather than smoothed.** The positive
path of `find_snatch.clj` REWRITES `checks/find-snatch.edn` from the current
library, so the fixture is regenerated rather than frozen: a re-run today writes
sha256 `08a0c3e7…`, not the pinned `839897ef…`. The `:scenarios`, `:laws` and
`:drift` are identical — only `:as-of` and the repository move, which grew from
18 to 24 as `library/snatch` gained six patterns. The recorded rows are therefore
stable and the pinned 18-member repository is the *stronger* F1 statement, so
transcribing the pin understates nothing; but the docstring pin names a snapshot
the check no longer reproduces, and that is a defect in the pinning, not in the
proof. `C500` §3 carries it.

**A second bound, on F3.** On this record the recorded non-self-certifying set
equals the recorded receipted set in all 34 rows — every receipt is
`:structured-antecedent` with a warrant file — so F3's proof discriminates
nothing that F2's does not. What separates them here is the `--negative-f3`
control, not the evidence.

Effect on the counts: contract holes 14 → 10 and closed 110 → 114 with the
declaration population unchanged at 124; accounting `:open-hole` rows 15 → 11 and
`:proven-against-pinned-source` 11 → 15; declaration fence `:pre-run-closable`
8 → 4.

## 2. Why nothing closed

**F1 — there is no checked criterion for hole → closed.** C141 read the
checkers: `contract_lint.clj` types every `kind = "closed"` declaration
`:closed-by-record` *before* authority, binding freshness, fixture shape or
result are considered, and `holder_check.clj` only reduces the owner's final
segment to a live registry key. No check reads a record
(`C141-closed-by-record-audit.md:5-30`). So the transition is written by hand,
and nothing downstream distinguishes a supported close from an unsupported one.

**F2 — a proven pinned obligation was demoted to a hole in the same commit that
proved it.** This is the fact that decides the audit. mathlib4 `86186c3744`
("Prove exact recorded ablation minimizers", 2026-08-31) adds `theorem
wmRecordedAblationNonDegenerate` — a complete Lean proof of
`nonDegenerateAblationLaw` over the pinned exact-dyadic table, no `sorry`
(`Holes.lean:217`) — and in the same diff moves the declaration from `mkClosed`
to `mkHole`. Its Clojure witness passes with a rejecting negative control and a
fresh contract-sha (`witness-registry.edn:727-740`). So the one hole whose
declared evidence is *fully discharged, twice over* was deliberately left open
by the declaration's author. "The pinned instance is proven" is demonstrably
not this contract's close criterion, and no document says what is.

**F3 — the `falsifier` field means two different things, and for H3/H4 it names
the confirming observation.** On twelve holes it states a refutation
(`wmRunsOnce`: "no tick-entry invocation completes with a TickRunRecord";
`dirichletAccumulationImportAbsent`, whose docstring says outright "the claim
held open is the ABSENCE"). On `policyPrecisionIsGammaFromBeta` it reads "a run
record in which tau is set by the beta update from G and pi" and on
`policyPosteriorImportsPolicyF` "a run record in which Q(pi) is computed with an
F_pi term" — each of which would make the named claim *true*. Both docstrings
say "this is the claim held open", so the claims are positive and the fields
describe their discharge. This is not a wording quibble: H4's named observation
is already on persisted record, so the two readings give opposite dispositions.

Taken together: closing any hole here would be an authored decision wearing a
record's clothes. Two `:needs-joe` rows are raised instead — `:J9` (F1/F2:
what the close criterion is, and whether `nonDegenerateAblationLaw` closes) and
`:J10` (F3: what the S4 record does to H4).

## 3. What the audit found on the way — F4, drifted pointers inside Holes.lean

Four pointers inside the H3/H4 docstrings no longer resolve to what they claim.
This is U29's defect class occurring *inside the Lean contract*, which U29's
scope (`variable-situation-accounting.edn`) does not cover.

| cited | claimed to be | what is there now | current location |
|---|---|---|---|
| `policy.clj:242-245` / `:242-246` (H3 **and** H4) | the policy score seam | `gap-report`, an unrelated helper | `policy.clj:157` `selection-scores`, "THE ONE PLACE THE SCORE EXPRESSION IS WRITTEN" |
| `war_machine.clj:4450-4451` (H4) | where the Laplace channel F is bound | unrelated report prose | `war_machine.clj:5543` |
| `war_machine.clj:4452` (H4) | the `:R8` route tag | unrelated report prose | `war_machine.clj:5857`, and it now names `compute-prediction-error`, not the `compute-variational-free-energy` the 2026-08-30 receipt recorded |
| `war_machine.clj:4753` (H4) | the F's one downstream appearance | an unrelated manifest read | not re-resolved by this audit |
| `selection_gain.clj:187-193` (H3) | "the field is ABSENT today, sim-only" | **still exactly that** | unchanged |

The drift is *not uniform* — one pointer of the family still holds — so this
family has to be checked pointer by pointer, not re-resolved by offset.

And one claim moved, not just its address. H4's docstring says of the policy
score "there is no F term in it, and no free-energy symbol anywhere in the
scoring path". `policy.clj:158` now writes the score as `ln E(a) − G(a)/τ [−
F_pi(a)]` and `:184` takes `:f-pi-policy-posterior?` and `:f-pi-values` as
options — landed by RUN9/I2 after the docstring was written. The term is
default-off, so the sentence is true of the default path and false of the seam,
and the docstring draws no such distinction.

Not repaired here: these are Joe-ruled declarations (J1/J2) and editing a
declaration's content is what U26's review was at pains to verify had *not*
happened. Carried on `:J10`.

## 4. The fence typing

The axis, stated so it can be applied again:

- **`:pre-run-closable`** — closing needs no evidence from a live War Machine
  tick. Lean work, a checker, a fixture transcription, or an owner's scope
  amendment are all pre-run, however much work they are.
- **`:run-gated`** — the claim is about what a run does, so only a run record
  can verify it. Each such row carries `:runtime-evidence` naming *which* record
  field or run shape.

**The axis is not a difficulty ordering and not a readiness ordering.** Two rows
make that concrete and a reader of Box 2 needs both: `wmRunsOnce` is
`:run-gated` and its run evidence already exists, repeatedly (the pinned
2026-08-30 receipt, three 2026-09-02 receipts, four more from S4) — it stays
open because the Lean proposition is world-level, not because a run is awaited.
`C` is `:pre-run-closable` and blocked on a scope amendment nobody has made.

### pre-run-closable (9 as audited; 8 after `:U45`, 4 after `:U46` — §1a, §1b)

| hole | what remains |
|---|---|
| `C` | the `DESIGN-c-vector.md §5` split (`C_int`/`C_mis`), an owner amendment. U12 measured the refusal's ground still holding: one `risk_mis` value across all 133 mission actions, all three 2026-09-02 records `:absent`, `FUTON_WM_MISSION_C` off. |
| `nonDegenerateAblationLaw` | ~~nothing on the evidence side — **contested**, see F2~~ **CLOSED 2026-09-03** under the J9 criterion by `:U45` (§1a); it is no longer a hole and no longer in this count, which now stands at 8 |
| `find` | an owner ruling overturning the "implementation, not a law" refusal |
| `findF1Containment` | ~~the pinned `FindReceiptRow` is not in Lean: `Holes.lean` mentions it at exactly two places, its definition (`:267`) and its `mkHole` row (`:6850`). Closing means transcribing the row as a concrete finite-typed value, as `wmTraceR2`/`wmTraceR8` were, and proving by `decide`. Its binding is also one pin behind (`f812795ca6`).~~ **CLOSED 2026-09-03** by `:U46` (§1b): the row is transcribed and `wmFindSnatchF1Containment` proves it by `decide`; the binding is re-run and re-stamped |
| `findF2Receipted` | ~~as F1 (`:272`, `:6851`)~~ **CLOSED 2026-09-03** by `:U46`, `wmFindSnatchF2Receipted` |
| `findF3NonSelfCertifying` | ~~as F1 (`:276`, `:6852`)~~ **CLOSED 2026-09-03** by `:U46`, `wmFindSnatchF3NonSelfCertifying` — with the bound in §1b: over this record F3's set is F2's set |
| `findF4Falsifiable` | ~~as F1 (`:280`, `:6853`)~~ **CLOSED 2026-09-03** by `:U46`, `wmFindSnatchF4Falsifiable`, at the `:selected-union` grain the check uses |
| `organise` | LA2's decision, named in the declaration itself |
| `dirichletAccumulationImportAbsent` | the provenance walk is **done and written down** (TN §9a, with the feeder at `a4a_substrate.clj:46-60` and the writer at `actuator_a3.clj:31,68,486-487`); what does not exist is an executable binding of it. The name does not occur in `checks/witness-registry.edn` — no check, no fixture, no negative control. An absence claim without a rejecting control is the shape this lab treats as unproven. |

### run-gated (7)

| hole | the runtime evidence, named |
|---|---|
| `preferenceStackLiveRecorded` | a trace record carrying mission-c criteria records beside its existing `:preference-stack`. The C_int half is there on all three 2026-09-02 records (five layers, each with `:source`/`:author`/`:basis`/`:site`); the C_mis half is not — `mission-c-readback` occurs zero times in `wm-trace-2026-09-02.edn`. **Two documents disagree about whether that would close it**: the Lean docstring says PERMANENT and cites C114's declining to narrow; `DESIGN-c-vector.md:105-112` (door 7) says "close-by-record once U11's records exist on a real tick". U13 noted the path and did not take it, and it is not written at the Lean hole. |
| `wmRunsOnce` | a completed tick leaving a `TickRunRecord`. **It exists.** Held open for the C114 reason. |
| `wmRunConformsToWiring` | TN §11 R2's run — ~20 shadow ticks, current code, judged by R3 against the re-synced topology. `:U29` and `:U31` are both still open, so the topology to judge against has not been re-synced; the 2026-09-02 receipts still read 6 of 9 hops unmapped against 21 drawn edges, the same as 2026-08-30. |
| `enactedEqualsSelectedWhenRankOneGated` | a record joining a rank-1 selection that passes its **own** act gate to the enacted action. No record since the pin carries the enactment half at all: `:realized-outcome` occurs 0 times in `wm-trace-2026-09-01.edn` (158 `:decision`), 0 in `wm-trace-2026-09-02.edn` (6), 0 in `wm-trace-s4.edn` (8). The S-stage runs are shadow runs, so they cannot produce the antecedent by construction. |
| `policyPrecisionIsGammaFromBeta` | a persisted record carrying `:tau` with `:tau-source` naming `carry-beta`'s `:beta-source`. **The wiring exists and the record does not**: S3 is "a REPLAY, not a 20-tick stage run" (`runs/2026-09-01-s3/README.md:3`), its one live τ = β tick came from `run8_s3_preflight.clj`, which suppresses every write but the run lock (`:28-54`). Checked: `wm-trace-2026-09-01.edn` carries 18 `:tau-source` values, all `:selection-gain-only`, and the reported τ `1.0364669814843985` occurs 0 times. |
| `policyPosteriorImportsPolicyF` | **the one hole whose named observation is already on persisted record.** `runs/2026-09-01-s4/wm-trace-s4.edn`: 3 of 4 records carry `:f-pi-posterior {:coverage :complete … :applied? true}` with per-candidate `:f-pi-by-candidate-id`; the 4th records `{:status :absent :reason :incomplete-coverage :applied? false}` rather than imputing. **Contested** — see F3, and note `FUTON_WM_FPI_POSTERIOR` is default-off, which neither contract field mentions. |
| `Strategic mission selection` | a record in which the mission value carried is the principled one (G_S over forward-model predicted mission outcomes, with its own habit E_S) rather than the three-factor additive surrogate. The glossary paragraph's claim is about what the implementation represents on the live path, so a unit test of the layer would not discharge it. |

## 5. What a reviewer should check

1. **F2 is the load of the argument** — verify `git show 86186c3744` in mathlib4
   really adds the theorem and demotes `mkClosed` → `mkHole` in one diff. If it
   does not, the "close nothing" conclusion has to be re-argued.
2. The three `:contested` / `:witnessed-and-held-open` calls, which are the only
   judgement in the artifact: `nonDegenerateAblationLaw`,
   `policyPosteriorImportsPolicyF`, `wmRunsOnce`.
3. The negative facts, each of which is a `grep` you can repeat: 0
   `:realized-outcome` in the three recent trace files; 0
   `mission-c-readback` in `wm-trace-2026-09-02.edn`; 18/18
   `:tau-source :selection-gain-only` in `wm-trace-2026-09-01.edn`;
   `dirichletAccumulationImportAbsent` absent from `witness-registry.edn`.
4. That the fence typing is binary and total (16/16 rows typed), since `:U34`
   fails its render on an untyped hole.
5. That no hole's `:content-status` moved and no contract declaration was
   edited: the open-hole count in Figure 3 and Box 2 must be unchanged at 15.
   (True of the audit as delivered. `:U45` subsequently moved it to 14 — §1a —
   and `:U46` to 10 — §1b — so check this against the audit's own commit, not
   against HEAD.)

## 6. Superseding record

| what | when | where |
|---|---|---|
| F1's gap (no stated criterion for `hole → closed`) closed | 2026-09-03 | J9 ruling, `RUNBOOK.md` §"What ends a `closed-by-record` evidence obligation" (futon2 `a2641b3`) |
| F2's subject (`nonDegenerateAblationLaw`) closed | 2026-09-03 | `:U45`; mathlib4 `4bc55c5968` (hole → closed), `e4a8ac81cb` (contract re-emitted) |
| The four `find` rows closed | 2026-09-03 | `:U46`; mathlib4 `65ec7e4c89`, `357b8d0a08`, `ddef5448ab`, `0bab8f813f` (one declaration per commit), `057e5eccbd` (contract re-emitted) |
| F3/F4 (the H3/H4 falsifier fields and the four drifted docstring pointers) | ruled, not yet executed | J10 ruling; worklist row `:U47` |

F2 stands as recorded: `86186c3744` really did add the theorem and demote the
declaration in one diff. The demotion is superseded, not amended — it stays in
history as the pre-criterion state.
