# C501 — H3/H4: the falsifier fields corrected, and four pointers re-read at source

Date: 2026-09-03 · worklist row `:U47` · executes Joe's J10 ruling
(`worklist.edn :J10`), which dispositions `policyPrecisionIsGammaFromBeta` (H3)
and `policyPosteriorImportsPolicyF` (H4) by name and authorises the correction
of both declarations' falsifier fields.

Author ≠ reviewer: this needs a second read. Every claim below carries a
`file:line` pointer, a commit, or the words "not found".

Commits: mathlib4 `a3ae5084be` (the declarations), `f16b389596` (contract
re-emitted); futon2 and p4ng as listed in §6.

## 1. What moved — and what did not

**No count moves.** Both holes stay open and stay `:run-gated`, so contract
holes remain 10, closed 114, declaration population 124, accounting
`:open-hole` rows 11, declaration fence 4 `:pre-run-closable` / 6 `:run-gated`.
Every generated artifact moved only its contract pin.

What did move is one axis value and two docstrings:

| | was | now |
|---|---|---|
| `policyPosteriorImportsPolicyF` readiness | `:contested` | `:witnessed-under-flag` |
| `policyPrecisionIsGammaFromBeta` readiness | `:not-ready` | `:not-ready` (ruled, and it cannot yet take the shape it was given — §3) |
| accounting `:readiness` axis | 3 values | 4 (`:witnessed-under-flag` added) |
| U27 findings `:F3`, `:F4` | open | `:resolved` |

## 2. The falsifier fields (J10 item 1)

Both fields named the **confirming** observation — word for word what the same
declaration's evidence field asks for — so one field called the observation the
discharge and the other called it the refutation. Twelve of the fifteen holes
use the field as a genuine refutation, and two of those are the model the
correction follows: `wmRunsOnce` ("no invocation of the tick entry point
completes end-to-end with a `TickRunRecord`") and
`dirichletAccumulationImportAbsent`, whose docstring says outright "the claim
held open is the ABSENCE".

| | before | after |
|---|---|---|
| H3 | "a run record in which τ is set by the β update from G and π" | "no run record carries τ together with the β it was derived from — every persisted tick's τ produced by an engineering calibration law and none by `carry-β`" |
| H4 | "a run record in which Q(π) is computed with an F_π term from the observations under each policy" | "no default-path run record's Q(π) carries a per-policy F term — F_π reaching the posterior only under `FUTON_WM_FPI_POSTERIOR=1`" |

Corrected in **two places each**, because the contract does not read the
docstring: the human-readable `falsifier:` annotation
(`Holes.lean:7186`, `:7189`) and the `mkHole` registry row the emitter actually
serialises (`:7422`, `:7423`). Each corrected string carries the correction
dated inside the field, which is the `wmRunsOnce` precedent for an amended
falsifier (`Holes.lean:7417`, "amended 2026-08-31: …").

**H4's is scoped to the default path** and H3's is not, because the two holes
are in different states: the flagged-path record for H4 already exists, so an
unscoped absence form would have been false on the day it was written.

The emitted contract confirms the blast radius: of 124 declarations, exactly two
fields differ from `057e5eccbd` — the two falsifier strings — plus the
`source.git-sha` pin. Every other field is byte-identical.

## 3. The dispositions (J10 item 4)

**H4 → `:witnessed-under-flag`.** The contest U27 could not resolve was a
*wording* defect, not an evidential one. With the falsifier corrected, the S4
record is a witness and no longer also a refutation, and the two opposite
dispositions collapse to one. What it witnesses is the **flagged** path:
`*f-pi-posterior?*` is read from `FUTON_WM_FPI_POSTERIOR` and is default-off
(`futon2/scripts/futon2/report/war_machine.clj:199-219`). The record is
`runs/2026-09-01-s4/wm-trace-s4.edn` — 3 of 4 ticks carry `:f-pi-posterior
{:status :present, :applied? true}` with per-candidate `:f-pi-by-candidate-id`;
the 4th records `{:status :absent, :reason :incomplete-coverage, :applied?
false}` rather than imputing a value. It closes on a default-path persisted
record carrying the term, or if the flag is ruled default-on, which is its own
ruling.

**H3 stays `:not-ready`, and the contrast is why J10 ruled the pair together.**
It was given H4's disposition *shape* and cannot yet take it: there is no
persisted record to be witnessed under, flagged or otherwise.
`data/wm-trace/wm-trace-2026-09-01.edn` carries 18 `:tau-source` values and
every one is `:selection-gain-only`; S3's one live τ = β tick ran under a
write-suppressing preflight (`run8_s3_preflight.clj:28-54`), and
`runs/2026-09-01-s3/README.md:3` says the stage was "a REPLAY, not a 20-tick
stage run". Recorded in `:verdicts-since` anyway, with the no-move stated,
because a row J10 dispositioned by name that shows no trace would read as an
unruled one.

`:witnessed-under-flag` is a new value on the `:readiness` axis
(`generate_variable_situation_accounting.bb:521`). It is declared with what it
means: the declared observation *is* on persisted record, but only from a run
under a default-off flag, so it records what the machine does under a flag and
not what it does.

## 4. The pointers (J10 item 2) — three corrections to J10's own re-resolutions

The row's discipline is that a pointer is verified at source before it is
written. Doing that changed three of the four targets J10 named.

| cited in the docstring | J10 said | **verified at source, 2026-09-03** | what is there |
|---|---|---|---|
| `policy.clj:242-245` (H3) | `policy.clj:157` | **`policy.clj:77-145`** | `effective-temperature` — the τ *law*, not the score |
| `policy.clj:242-246` (H4) | `policy.clj:157` | `policy.clj:157-215` | `selection-scores`, "THE ONE PLACE THE SCORE EXPRESSION IS WRITTEN", normalised by `softmax-weights` at `:215` |
| `war_machine.clj:4450-4451` (H4) | `:5543` | **`:5773`** | `free-energy (fe/compute-controller-diagnostics observation)` |
| `war_machine.clj:4452` (H4) | `:5857` | **`:6087`** | `(route-tag route2 :R8 "futon2.aif.free-energy/compute-prediction-error")` |
| `war_machine.clj:4753` (H4) | not re-resolved | `:6472` | `:free-energy free-energy` in the report map |
| `selection_gain.clj:187-193` (H3) | still holds | still holds | unchanged, verified |

**(a) Two of J10's targets had drifted again, by +230 lines**, because
`:U28` (futon2 `6ca58f3`) grew `war_machine.clj` between the audit and the
repair. This is U27's F4 lesson recurring inside F4's own repair, and it is the
argument for the discipline: had the numbers been copied out of the ruling, two
of the four "corrected" pointers would have landed on unrelated code.

**(b) The subject of the `war_machine.clj` family is gone.** The Laplace-channel
scalar F was **retired** on 2026-09-01 by worklist I5 slice (c) under Joe's J2
ruling — futon2 `5a66411`; `src/futon2/aif/free_energy.clj:7-12` records why
("the Laplace-channel F was a shortcut that no equation in the registry
imported"). So `:5773` binds the controller-diagnostics map that used to carry
the scalar, `:6087`'s `:R8` tag now names ε rather than the removed
`compute-variational-free-energy`, and `:6472` puts that same diagnostics map on
the report, not the scalar.

**The 2026-08-30 receipt discrepancy, stated rather than smoothed.**
`p4ng/empirics-futon/control-map-edges.edn:140-144` still records the R3→R8
hop `:via "futon2.aif.free-energy/compute-variational-free-energy"` (`:142`),
carried derivatively at futon2 `edge-census.edn:82`. (C473 §1 cites that row as
`:120-124`; it is at `:140-144` today — the same drift class again.) That row is **correct and is not
rewritten**: it records what WM-RUN2 measured against
`tick-run-record-2026-08-30.edn`, and that measurement is still true of that
receipt. So the registry row and the code disagree by construction, and C473 §1
already reports it unrepaired ("Repairing a signed registry row is the supersede
dance, not this slice"). `:U47` does not repair it either — no `:decisions`
entry is written by this row.

**(c) H3's pointer does not go where J10 sent it.** J10 sent both H3 and H4 to
the `selection-scores` seam at `policy.clj:157`. That is right for H4, whose
sentence is about the policy *score*, and wrong for H3, whose sentence is about
the τ *law* — "τ from the score spread and an engineering selection gain". That
is `effective-temperature` (`policy.clj:77-145`), whose default `:spread` mode
computes τ_eff = τ_spread / g at `:133`. Sending it to `:157` would have
produced a resolving pointer that does not support the sentence around it, which
is the F4 defect class in a new guise.

## 5. The scoping (J10 item 3), and the same defect found in H3 and left alone

**H4's sentence is scoped, as ruled.** "there is no F term in it, and no
free-energy symbol anywhere in the scoring path" is true of the **default** path
and false of the seam: `policy.clj:158` writes the score as
`ln E(a) − G(a)/τ [− F_pi(a)]` and `:184-186` takes `:f-pi-policy-posterior?`,
`:f-pi-values` and `:f-pi-scaling` as options, both landed by RUN9/I2 after the
docstring was written. The original sentence stands and the scope is added
dated, which is this file's convention (`wmRunsOnce`: "Original claim retained
here as history, not current state").

**NEWLY FOUND AND NOT REPAIRED: H3 has the same defect.** "No β appears anywhere
in the policy path" is false of the current tree. `effective-temperature`'s
`:variational-beta-gamma` mode sets τ = β (`policy.clj:101-105`, `:135-144`,
"THE VARIATIONAL MODE NEVER FALLS BACK TO THE SELECTION-GAIN LAW"), and
`policy_precision/carry-beta` supplies it with its provenance
(`src/futon2/aif/policy_precision.clj:544-560`) — both landed by RUN8/I1 after
the docstring was written. The sentence is true of the default `:spread` path.

It is **stated in the docstring and the sentence left standing**, not scoped.
J10 enumerated the scoping for H4's sentence and not for H3's; scoping a
declaration's prose without the owner having ruled on it is what U26's review
was at pains to verify had not happened. A reader of H3 now finds the fact next
to the sentence. **This wants a ruling** — it is one sentence, of exactly the
shape J10 already ruled on once.

## 6. What was regenerated, and what was not

| repo | commit | what |
|---|---|---|
| mathlib4 | `a3ae5084be` | both docstrings, both `mkHole` rows |
| mathlib4 | `f16b389596` | `holes-contract.json` re-emitted at that authority |
| futon2 | this row's commit | generator (`:readiness` axis + both `hole-closability` entries), regenerated accounting, U27 `audit.edn` + `AUDIT.md`, re-run lean-state probe, this note |
| p4ng | this row's commit | `sec-model-coverage-generated.tex`, `sec-lean-holes-generated.tex`, `sec-variable-situation-generated.tex`, `war-room-tetrahedron.svg` — every one moving only its contract pin |

**NOT DONE, stated rather than implied.**

- **No ruling written.** `aif-equations.edn :choices` and
  `control-map-edges.edn :decisions` are untouched; `:covers-key :none`.
- **`gen_aif_dag.bb` not run into a publish** (TN §9a gate rule).
- **The `control-map-edges.edn:140-144` / `edge-census.edn:82` `:via` strings
  are not repaired** — §4(b): they are a measurement, and repairing a signed
  registry row is the supersede dance.
- **H3's "No β" sentence is not scoped** — §5.
- **`p4ng/wm-status.pdf` was already dirty in the shared checkout** when this
  row started and is not touched; no status receipt or infographic was
  regenerated, which is not this row's acceptance.
- **Three stale `Holes.lean` line citations in a futon2 comment**
  (`generate_variable_situation_accounting.bb:26`, `:32`, `:41-42`, citing
  `6645`/`6648`/`6651` and `6636`/`6642`) were **found and left**: they are
  outside J10's four, no checker reads them, and the same +230-style drift will
  keep moving them until something generates them. Reported here so the next row
  has the list.

## 7. Gates

- `lake build DarkTower.WarMachine.Holes` — 2704 jobs, 0 errors, at both
  mathlib4 commits; 10 `sorry`, unchanged.
- `lean_state_probe.bb` — 68/68 modules exit 0, 0 error diagnostics,
  sorry 10 source / 10 Lean-reported, **axioms 0**.
- `generate_variable_situation_accounting.bb` — WROTE; `--check` PASS;
  `--negative-empty`, `--negative-untyped`, `--negative-drift` all PASS.
- `gen_model_coverage.py` — 133 variables → 114 closed, 10 open, 0 unclassified;
  fence 4/6.
- `clj-kondo` — 0 errors / 0 warnings; `check-parens` — OK, on the one changed
  Clojure file.
- `negative_controls.sh` — PASS (33 negative, 16 positive; shared registries
  untouched).
- `pointer_check.bb` — 1204 pointers in 3 files, 0 unresolved.
- `worklist_check.bb` — re-run after the ledger commit.
