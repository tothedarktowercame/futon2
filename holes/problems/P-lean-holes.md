# P-lean-holes — The Lean model states its holes, so the sorry count is a measure

Problem record (delivery-lifecycle v2). Opened 2026-08-30 by claude-15 on Joe's remark: *"I hope the
Lean model is being kept up to date. In fact, we might put in known holes so we know how many sorries
are in there!"* Owner: claude-15. **holder.** claude-15 → codex-22 (LH-D1). **parent.** P-validated-R5 §2b (the Lean audit).

## S1

**problem.** `mathlib4/DarkTower/WarMachine/` (six modules; last change 2026-08-28, `CascadeOrder`)
contains **0 `sorry`s** — and none of the terms decided on 2026-08-30 exist in it: `Pattern`, `Cascade`
(DAG over patterns, §2.1d), `Policy : InformationState → Action`, `Outcome := Σ v, Obs v` with
`C` per pragmatic vertex, `G = risk − EIG`, `nonDegenerate`, the `find` laws F1–F4 and `organise` laws
O1–O4 (§3e), `independent`/`Layer` (P-R9), `Delivery` (lifecycle §0.6), `Handoff`/`Workflow` (§0.10).
Only `Producer` (`GainChain.lean:71`) is declared. Zero sorries in a model that does not state its holes
is the typed-absence facade at the Lean layer: the count says "complete" about a model that has not been
asked the questions. Nothing today was kept in step with Lean; the typing lives in Clojure checks and
records only.

**now.** `CascadeOrder` carries `Reach` and `acyclicDescent` (generic in α — the O3 fast-forward is a
restriction of it); `CommitmentTemperature.lean:236–292` has the finite Markov-category theorems;
`GainChain` has `Producer` and the record contracts minus the L2 constraint; `PolicyGrade` has the
Snatch fixtures; `Mathlib.Probability.Kernel.Category.Stoch` elaborates. Packet C (the two-readings
theorem) is held.

**solved.** A module `DarkTower/WarMachine/Holes.lean` in which **every decided-but-unformalised term is a
declaration whose body is `sorry`**, each with a doc comment naming the owning record and section
(`-- P-validated-R5 §3e O3`) and the holder; the module builds (`lake build DarkTower.WarMachine.Holes`
— sorries warn, they do not fail); and a script `scripts/count-holes.sh` that prints, per owning record,
**two lines: declared-with-body / declared-with-sorry**, never a percentage of "done". **Registered
expectation, before the packet runs:** the first count is 0 / N with N = the number of terms in the list
above (claude-15 counts 14: Pattern, Cascade, InformationState, Policy, Vertex/Obs/Outcome, C, G,
nonDegenerate, find+F1–F4, organise+O1–O4, independent+Layer, Delivery, Handoff, Workflow — the builder
reports the actual N and does not adjust the list to hit 14). **Falsifier:** if any hole can be closed by
a one-line definition that merely renames an existing Lean term, it is not a hole and must be declared
with a body, not a sorry (the rename facade in reverse).

**facades:** a `sorry` count of 0 on a model that omits the terms; a hole "closed" by `axiom`; a body
that types but does not state the law (e.g. `nonDegenerate := True`); closing holes inside this packet
(this packet declares, it does not prove — proving is a later packet per hole with the record as
acceptance); percentages.

**status.** open.

## The holes are the interfaces (Joe, 2026-08-30)
`Holes.lean` is the registry of contact points between the Lean and Clojure children of every node
tetrahedron (charter, "Interfaces are Lean declarations"). Each hole's doc comment therefore also names
the **Clojure artefact expected to witness it** (e.g. `futon3/checks/find_snatch.clj` for F1–F4;
`futon2/checks/…` for R8's F checker) — added at LH-D1's review if the packet does not carry it. A lane
closes when its hole moves; the two-line count is the gate.

## deliveries
- **LH-D1 — build, one module + one script** (codex-22). As in *solved*. Gates: `lake build` of the
  module succeeds with exactly N sorry warnings; the script's two lines match; nothing else in
  `DarkTower/` changes; commit on explicit paths in `mathlib4`; no push.
- **LH-D2+ — one hole per packet**, each closed against its record's acceptance, dispatched by the owner
  (spine holes) or the tech lead (R-node holes), and the count re-run as the gate.

## log
- 2026-08-30 record written (claude-15); LH-D1 dispatched (status line below).
- 2026-08-30 **LH-D1b reviewed by claude-15 — passes with four fixes made directly** (mathlib4 `3b8e2ceb` +
  review fixes `b98b2500`). Elaborated with the corrected instrument: 15 sorry warnings = 15 HOLE
  declarations; script totals match. Laws read one by one — F1–F4, O1–O3, the ablation law, L2, the R2
  and R8 contracts all state what the records state; `fastForward` is an inductive `ReachOutside` (through
  unselected intermediates) — right. Fixes: `Delivery`/`Handoff`/`Workflow` bodies were **not the record
  text** under a `CLOSED-BY-RECORD` tag (invented fields; restored to §0.6/§0.10 — the one thing the tag
  must never do); O4/S-G4 quantified over *all* `actingOrder`/`score` functions, refuted by a constant score
  — now parameters (the run's functions); `C` had lost its pragmatic-vertex index (§2a); `Tension` had only
  `context` (§3e fixes want/however). Codex's three extra holes beyond my 12 (`C`, `find`, `organise` as
  implementations) are right to keep. **Two lines, 2026-08-30:**
```
P-validated-R5 declared-with-body: 9
P-validated-R5 declared-with-sorry: 12
P-R9 declared-with-body: 1
P-R9 declared-with-sorry: 1
P-R2 declared-with-body: 0
P-R2 declared-with-sorry: 1
P-R8 declared-with-body: 1
P-R8 declared-with-sorry: 1
delivery-lifecycle declared-with-body: 3
delivery-lifecycle declared-with-sorry: 0
total declared-with-body: 14
total declared-with-sorry: 15
```
  **The D2 hold on the build is lifted**; build packets quote these declarations.
- 2026-08-30 **LH-D1 reviewed by claude-15 — returned as LH-D1b** (mathlib4 `c6b9a90b`; builds; script
  prints 0/11, 0/1, 0/3, total 0/15; no axiom; `git diff --check` clean; only the two files). Gates pass;
  the *shape* is wrong, and the packet's term list is part of the cause. The module declares types and
  functions as `sorry` and declares no laws. Of the 15: **seven are not holes** — the record fixes their
  bodies in one line: `Policy := InformationState → Action` (§3), `Outcome := (v : Vertex) × Obs v` (§2a),
  `G := fun π => risk π − eig π` (§2a′), `independent c w := w.producer ∉ c.producingPart` (P-R9),
  and `Delivery`/`Handoff`/`Workflow`, each declared `sorry` beside a `…Shape` structure that already
  carries every field (a rename-hole — the record's own falsifier). `InformationState` is a fixed field
  list (§3d) and belongs as a structure. **The genuine holes are absent:** F1–F4 and O1–O4 as `Prop`s
  about `find`/`organise`; `nonDegenerate`'s ablation property; the L2 rule `valueEvidence w → w.layer =
  L2`; S-G4 (score depends on precedence); the record contracts for R2 (`keys = Channel`) and R8 (F
  recomputed = stored). Counting types-as-holes inflates N with things that are decided and hides the
  things that are not. LH-D1b: bodies for the decided, one declared `Prop`/theorem per law with `sorry`,
  the script distinguishing `HOLE` from `CLOSED` doc tags. **Instrument note:** the sorry-warning count is `lake env lean DarkTower/WarMachine/Holes.lean 2>&1 | grep -c 'uses .sorry.'` — Lean prints `` `sorry` `` with backticks; a grep for straight quotes returns 0 (claude-15 got 0 three times before reading the raw output; 15 confirmed). `lake build` re-elaborates only on a hash change, so `touch` does not help. Registered expectation revised: bodies ≥ 8,
  law-holes ≈ 12 (F1–F4, O1–O4, nonDegenerate-ablation, L2, S-G4, R2/R8 contracts) — reported, not fitted.

**STATUS 2026-08-30 15:13Z:** LH-D1 dispatched to codex-22 — job `invoke-1788102830497-4297-e6cec2d9`, park `park-9c5f9325-19ac-4a3a-946a-cf8bf8d6ba54` (deadline +45 min).

**STATUS 2026-08-30 15:20Z:** LH-D1 returned as LH-D1b (shape: laws not types) — job `invoke-1788103238044-4308-6cf3bc5b`, park `park-3ce1e57f-04ab-4402-bf71-d84ca1e096bf`. D2 hold on the build stays until D1b is reviewed.
- 2026-08-30 **Second pass on `Holes.lean` from claude-20's reading against the artefacts** (mathlib4
  `99c5009b` + `6ee2d0db`; zero elaboration errors, **16 sorries / 15 bodies**): (1) `r2ObservationKeysAreChannels`
  was vacuous or refuted depending on where `Channel` came from — the same defect claude-13 caught in the
  R2-D2 packet, one level up; now `r2WellFormed` against a *declared* list plus `r2ContractCensus` whose
  fixture is the refuting run (illFormed = 2). (2) `r8StoredFRecomputes` encoded the clause retired that
  hour; replaced by `r8Census` and `r8EraBoundary` (four facts co-move at 2026-07-14), with `R8Tick` fields
  named as the artefact's keys so the clause-2 signature diff is literal. Lesson kept: a hole must be checked
  against the artefact it is about, not only against the record — the record can be behind the artefact.
  Also: my first fix commit carried a type error (the gate checked the script, not the elaboration); the
  gate now asserts zero `error` lines before committing.

## Evidence shape per hole (Joe, 2026-08-30)
A HOLE names a law nobody has proved; from 2026-08-30 it also names **what evidence would move it** — an
`evidence : Type` (the artefact kind a witness must inhabit) and a `falsifier : evidence → Prop` (the
zero-mass outcome), derived from the AIF reading of the law rather than from whatever artefact exists.
This is the lifecycle's evidence apex stated in Lean before any run: the theory predicts the observation;
the Clojure run is the observation; `conformant`/`wrong-shape` is the likelihood being non-zero or zero.
Convention (to be carried by the shared emitter, `P-lean-clojure-adapter` solved 4): the doc tag gains
`· evidence: <type name> · falsifier: <one line>`, and the type is declared beside the hole. First two:
`nonDegenerateAblationLaw` — evidence `AblationTable := Prior → {argminG, argminRisk : List Policy, moved? :
Bool}`, falsifier "no prior moves"; `r8EraBoundary` — evidence `EraTable := {boundary : Nat, perEra : Era →
{count, storedF?, selectionGain?, shape, meanPrecision}}`, falsifier "a form in neither era". Owner adds
these at AD-D2; the two-line count gains nothing (evidence types are scaffolding, not holes).
- 2026-08-30 **Third pass — a family of declarations that do not say what their docstring says** (codex-1's
  refusal of R2-D2 via claude-20; mathlib4 `c131af37`, zero errors, 18 sorries). `r2ContractCensus`
  universally quantified `illFormed` and then asserted equality — false for every instantiation (codex-1's
  counterexample: `Channel := Empty`, empty corpus, `illFormed := 1`); `r8Census` had the same shape over its
  triple; `r9VerdictSound` assumed the checker's soundness as a hypothesis and so could never fail — the
  interesting case never reached the conclusion. Fixed as codex-1 proposed: the censuses are COMPUTED values
  (`r2ContractCensus : … → Nat`, `r8Census : … → Nat × Nat × Nat`, closed) and the holes assert the run's
  number (`r2ContractCensusWmTrace = 2`, `r8CensusWmTrace = (755, 32, 5)`) — statements about *wm-trace* that
  can be false; `r9CheckerSound` is a predicate on a *given* checker with no soundness hypothesis, and
  `r9WmCheckerSound` the hole. **Standing rule (claude-20's wording, kept):** a HOLE's docstring states the
  expected value, and the *type* must be able to be false when that value is wrong. Clause-2's signature
  diff catches packet/file drift; it cannot catch a wrong declaration — that took a reader and a builder each
  asked to attack the interface. Also restored the `R8Disposition` inductive that a slice edit of mine had
  dropped (the zero-error gate caught it).
- **Process finding (claude-20):** a builder's refusal comes back from the job API as `state: failed`; a
  lane that did exactly what the charter asks reads as an infrastructure failure to anyone triaging by state.
  Wants a field on the job (`:refused`) — futon3c, not this record; noted here so nobody re-diagnoses it.
- 2026-08-30 **Fourth pass — the family was every "Wm" hole I wrote** (mathlib4 `6fd8a33f4d`; zero errors;
  23 sorries). claude-13 refuted `r9WmCheckerSound` with `fun _ _ => false` (it quantified over every
  checker); the same shape was in `r2ContractCensusWmTrace` (∀ corpus), `r8CensusWmTrace` (∀ 792-list) and
  `r9TwoRunCensus` (∀ 13 rows) — my fixes of the morning's family were members of it. **Rule, final form:
  a hole about a run is a decidable proposition over a NAMED FIXTURE CONSTANT transcribed from the run**
  (`wmTraceR2`, `wmTraceR8`, `wmVerdictsLedgerAlone`, `wmVerdictsDeclared` — themselves holes whose evidence
  is the corpus and whose falsifier is a digest mismatch); the law then moves by `decide` once the constant
  is filled, and is false exactly when the run disagrees. This is the CommitmentTemperature finite style and
  precisely what the adapter (AD-D2/D3) exists to emit. Counts now: P-R2 2/3, P-R8 2/4, P-R9 5/6.
- 2026-08-30 16:05Z **Fault, mine (fifth precept):** AD-D2 handed `Holes.lean` to codex-22 ("additions only")
  and I went on editing it — two holders of one file in one checkout. My `6fd8a33f` swept codex-22's
  uncommitted `import DarkTower.Contract.Emit` into history while `Emit.lean` is untracked, so that commit
  builds only once AD-D2 commits the module; caught by the elaboration error on my next edit, not by any
  gate. Worktree restored to HEAD; my pending fix (one HOLE tag per fixture constant — the script counts 22,
  Lean 23, which is the three-way count's independent leg working as designed) waits for AD-D2 to close.
  **Rule:** while a packet holds a file, the owner does not touch it; signature changes queue as proposals
  until the packet closes, and the packets that quote the file quote the sha the builder started from.
- 2026-08-30 16:13Z **AD-D2 landed; `Holes.lean` is exported as data** (`holes-contract.json`, source sha =
  the module's last commit). The deferred one-tag-per-constant fix was made moot by codex-22's registry
  (tags = sorries = 23). Then the queued `Channel` change ratified on release: fourteen named constructors in
  `observation.clj:18–32` order, `Channel.all` as the declaration-order list, `R2TickLit := R2Tick Channel
  Unit` (`e3f65c5c`; JSON `5e7b4c2a`). **Two lines now: 22 bodies / 23 holes.**
- 2026-08-30 16:34Z **G-D3 landed** (`1b09974a`): the glossary's theory core is in Lean — 7 bodies (one over a hole) + 8 holes + 2 refusals. **Two lines: 31 bodies / 32 holes.** The file returns to the owner.
- 2026-08-30 16:39Z **Axiom gate (charter 3a).** claude-13 raised it on R2-D3; claude-20 ran it on both delivered
  reports: R9-D2's four theorems depend on `[propext]`, `[propext, Classical.choice, Quot.sound]` ×2,
  `[propext, Quot.sound]` — kernel-checked (claude-15 reproduced); R2-D2's proof, when named and retried with
  `native_decide`, depends on a generated axiom `…_native.native_decide.ax_1_1` — not `sorryAx`, not a known
  name, invisible to the standing check. **And as committed, R2-D2's `.lean` does not elaborate at all** (plain
  `decide`, maximum recursion depth at 792 entries): the artefact carries a failing proof. Neither delivered file
  contained `#print axioms`; "standard only" was a bell claim, true for R9 and not carried by its artefact. Rule:
  elaborate at the gate; named theorems; `#print axioms` in the file; non-standard axioms named with reason.

