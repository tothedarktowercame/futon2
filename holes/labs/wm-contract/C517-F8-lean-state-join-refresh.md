# C517 — :F8 leg 1, slice 0: the U35 Lean-state join, refreshed

**Row:** `:F8` (`:loop-mode :one-slice-per-invocation`), leg 1's own
pre-step: "refresh the stale U35 lean-state join FIRST (pin STALE-PIN,
predates F1's MachineQ.lean) so the spec legs read true"
(`holes/labs/wm-contract/worklist.edn:1304`).

**What this slice is not.** No Lean statement was written, so leg 1's
`Lean elaborates 0/0 + #print axioms clean` bar has no new subject here;
what it has is the whole corpus, re-elaborated. No registry was written
and no ruling: `aif-equations.edn` `:choices` and `control-map-edges.edn`
`:decisions` are byte-unchanged.

## 1. The refresh

`bb lean_state_probe.bb` re-ran the probe against the live corpus and
`bb empirics-futon/gen_lean_state.bb` re-rendered the section from it.

| | receipt of 2026-09-03 | receipt of 2026-09-05 |
|---|---|---|
| corpus tree | `f16b389596` | `824f67b5e2` (`F1 slice 1: construct Q(o\|pi) in Lean`), worktree clean |
| pin as rendered | **STALE-PIN** | **CURRENT** |
| modules / lines / declarations | 68 / 10469 / 635 | 71 / 11762 / 725 |
| type-check | 68/68 exit 0, 0 error diagnostics, 101 s | 71/71 exit 0, 0 error diagnostics, 164 s |
| `sorry` / `axiom` | 10 / 0, exact-by-location | 10 / 0, exact-by-location |
| contract pin | `a3ae5084be`, 10 holes = 10 source holes | `69721b1268`, 10 holes = 10 source holes |
| glossary join | 48 of 56 resolve | 48 of 56 resolve |
| equation join | 8 of 18 carriers declared, 8 resolve | 8 of 18 carriers declared, 8 resolve |

Three modules were added and one changed since the old receipt, derived
from the two reports' per-module sha256 sets: `MachineQ`, `MachineQWitness`,
`Run4Preregistration` added; `Holes` changed. The ten `sorry` tokens are
the same ten declarations — the last four moved from `Holes.lean:7181,7184,7187,7190`
to `:7676,7679,7682,7685` because `Holes.lean` grew by 497 lines, and the
source/Lean-warning agreement is `:exact-by-location` at both shas.

**The rendered file moved by three lines and no more**
(`p4ng/sec-lean-state-generated.tex:1-5`): both tables are byte-identical
before and after, and the file is 51 lines at both shas. That is the
substantive result and not a triviality — see §2.

## 2. The corpus grew by 1293 lines and the join did not move

`824f67b5e2` landed `MachineQ.lean` (287 lines) and `MachineQWitness.lean`
(302 lines), F1 slice 1's construction of Q(o|π). The equation join reports
the same 8 of 18 carriers resolving as it did two days ago, because the join
resolves the carrier **the registry declares**, never a name it finds:
`aif-equations.edn:114` still declares `:lean "PredictiveOutcomeKernel"`
`:lean-status :carrier-only` for `:forward-model`, and
`mathlib4/DarkTower/WarMachine/MachineQ.lean:187` declares
`machinePredictiveOutcomeKernel`, which no registry row names.

So the refreshed join reads true **about the registry** and understates the
Lean corpus for one row. Moving that declaration is a registry write and is
not this slice's; it is named here as leg-1 input.

## 3. The ten rows with no declared carrier — leg 1's worklist, verbatim

From the refreshed report's `:equation-lean-join :no-carrier-declared`
(`p4ng/sec-lean-state-generated.tex:13-26` renders them as **not in Lean**):

`:observe` (o, R2), `:prediction-error` (eps, R8), `:precision` (Pi, R7),
`:policy-free-energy` (F-pi, R8), `:belief-update` (mu-next, R3),
`:belief-state` (mu, R1), `:policy-set` (pi, R6), `:depth` (T, R13),
`:temperature` (tau, R14), `:action` (u, R16).

That is the convergence ruling's nine class-(a) quantities plus `:policy-set`
(π, the row F7 inhabited on the runtime leg). Four of the ten carry
`:lean-status :missing` in the registry and six carry no `:lean-status` field
at all — a distinction the rendered table prints (`missing` vs `--`) and no
checker consumes.

The eight glossary rows that do not resolve are all `:row-source
:glossary-paragraph` — prose paragraphs with no Lean declaration of that name
(`Observation vector o`, `Embedding space`, `Active Inference Framework`,
`EDN`, `Substrate and Drawbridge`, `No self-certification`,
`Revision boundary`, `A shared experimental substrate`). Every
`:contract-declaration` row resolves, 48 of 48.

## 4. Three findings recorded, none repaired

**(a) `CONVERGENCE-draft.edn`'s spec-leg pointers are off by one, 14 of 18.**
Leg 3 adopts this draft. Its rows cite `p4ng/sec-lean-state-generated.tex:<line>`
as the spec-leg basis; for `:observe` through `:temperature` the cited line is
the row **above** the one named — `:observe` cites `:12`, which is `\hline`,
and the `observe` row is at `:13`; `:forward-model` cites `:19`, which is the
`belief-state` row, and `forward-model` is at `:20`. The last four rows
(`:policy-posterior`, `:action`, `:dirichlet-accumulation`,
`:model-reduction`, cited `:27`-`:30`) are correct. The defect is
pre-existing and not caused by this refresh: the file is 51 lines and the
table rows sit at the same line numbers at both shas, checked against
`git show HEAD:sec-lean-state-generated.tex`. It matters because the wrong
pointer lands on a **neighbouring table row of the same shape**, so it
resolves and reads plausibly rather than failing.

**(b) `CONVERGENCE-draft.edn:10`'s caveat is now false.** It reads "The Box-5
join is explicitly stale ... These rows reproduce that join and do not refresh
it." The join is refreshed as of this commit; the draft's row *contents* are
unaffected (§1: both tables byte-identical), so the caveat is stale prose
rather than a wrong rung. Left as written, for the leg-3 slice that adopts the
file. Two further staleness points in the same draft, both post-dating it:
`:forward-model`'s spec-leg (§2) and `:policy-set`'s `:impl-leg {:rung :absent}`
citing `FUNDAMENTALS.edn` "no runtime decision constructs and scores two
admissible cascade policies" — the entry `:F7` moved.

**(c) The accounting's `:as-of` cannot move.**
`futon2/scripts/generate_variable_situation_accounting.bb:842` writes
`:as-of "2026-09-03"` as a literal, so `variable-situation-accounting.edn:2`
still reads `2026-09-03` after `:F6` regenerated the whole file at `20381c6c`
on 2026-09-05. The lean-state report copies that field into
`:glossary-lean-join :source-as-of`, and the generator prints it in the
glossary-join caption, so `tab:lean-glossary-join` now dates the registry two
days behind its last change. The registry's `:authority` pin
(`variable-situation-accounting.edn:4`, contract `69721b1268`) is current; it
is only the date label that is frozen.

## 5. Gates

| gate | result |
|---|---|
| `bb lean_state_probe.bb` | 71 modules, 725 declarations, 11762 source lines; 71/71 exit 0, 0 error diagnostics, 163749 ms; `sorry` 10 source / 10 Lean-reported (exact-by-location); axioms 0 |
| `bb empirics-futon/gen_lean_state.bb` | bare exit 0, `pin CURRENT` |
| `bash p4ng/empirics-futon/negative_controls.sh` | see the ledger row |
| `bb p4ng/empirics-futon/pointer_check.bb` | see the ledger row |
| `futon4/dev/check-parens.sh` | OK on the report EDN and `worklist.edn` |
| PDF, `latexmk` on the TeXLive 2026 tree | `futon-2026.tex` builds to 60 pages with **2 errors**, both `! Misplaced \noalign` at `sec-case-study-vetting.tex:331` and `:380` |

**The two LaTeX errors are pre-existing, and that was checked rather than
assumed.** A baseline build of the same document with the committed
`sec-lean-state-generated.tex` (shadowed via `TEXINPUTS`, the shared checkout
untouched) gives the same 2 errors at the same two lines and the same 60
pages. Both are an `\hline` immediately after an `\input{...-generated}`
whose generated block ends in `\hline`; `:F6`'s evidence line reports them as
warnings, and in this build they are errors that `latexmk` exits 12 on while
still emitting the PDF. Not repaired here — the file is `:F6`'s and the
repair is not this row's acceptance.

No clj-kondo run is claimed: no Clojure or `.bb` source was changed. No
`src/` change in futon2, so no test run is claimed. No live tick, no run
lock, nothing written under `data/`. `gen_aif_dag.bb` was not run and nothing
was regenerated into a publish beyond this section (TN §9a).
