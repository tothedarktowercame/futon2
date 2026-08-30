# P-R8 — Two nouns under one number: F (present fit) and g (gain reading)

Problem record (delivery-lifecycle v2). Node R8 (BELIEVE, support). Lane 3 of the R-node build.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. Source: `futon2/holes/labs/wm-contract/R8-glossary-formalisation.md` (carry §3–§5 forward).

## S1

**problem.** "R8" names two things (worksheet §0): **F**, the per-tick present-fit
`½ · mean_k (Π_k · (o_k − μ_k)²)` — theory-defined as a formula over stack-defined μ, o — and **g**, the gain
reading `clamp(2^(gain·mean perf), 0.5, 2.0)` with `τ_eff = 1/g` — an engineering controller that was
called a precision and fed from L1 pairs only. Every trace record examined in the census holds G under the key
`:free-energy` and no F (the census counted 88 *realized outcomes*; the record count is the number of
top-level forms across the 54 files of `futon2/data/wm-trace/` and is for D1 to establish — amended
2026-08-30 after claude-20 checked the corpus: 54 files by `ls`, not 88 of anything). g never moved: pinned at 1.0 with 0 samples through five deliveries whose
guarantees were each defensible and whose composition was seven weeks of silence (§4 e1–e5: corpus-wide
throw on one rejected deposit; `nil` outside a 4-entry map; `(catch Throwable _ judgement)`; no-op on
absence; idem-key from a different clock). R8 is the node where the July facade was most complete: the
number was present, named, and inert.

**now.** `GainChain.lean` states threadedIdentity, inhabitedHandle, durableBeforeFold, declaredDomain,
typedAbsence — and lacks the L2 constraint the glossary demands. The record schema ≥ 7 carries
`:prediction-errors` and `:precision-state`, so F is recomputable from the record. The outcome space for g
does not exist (spine: `P-validated-R5` §2a).

**Fact added 2026-08-30 (claude-20, self-corrected; reproduced by claude-15 — 53 files / 792 forms,
`:variational-free-energy` on 32, `:selection-gain` on 32, intersection 32, difference 0 both ways):** F
and g are instrumented in exactly the same records. Every form that stores an F also carries a
`:selection-gain` state, and every such state is `{:selection-gain 1.0 :perf-history [] :mean-perf nil
:samples 0}`. The trace began recording both nouns at the same moment (07-14 per R8-D1) and, across every
record where g is present, g never moved. For "two nouns under one number" this is evidence about how the
delivery went — the schema change that introduced F introduced g's state alongside it, already inert —
not a coincidence to design around. R8-D2 does not depend on it; R8-D3 (the five deliveries typed) does:
e5's `idem-key`/clock question starts from the tick these 32 records share.

**solved:**
1. **F, definable now:** `F : Belief → Observation → Precision → ℝ` and the record contract
   `∀ tick, tick.variationalFreeEnergy = F tick.muPre tick.observation tick.precision`, checked over the
   trace with the reader loop. **Falsifier (amended 2026-08-30 after R8-D1 — codex-12 refused the one-direction form and claude-20
   reproduced the refusal; claude-15 reproduced the counts independently):** the checker assigns every
   top-level form one of three dispositions and the acceptance is the *census*, not a uniform fire —
   (a) `:missing-F-computable`: no stored F, inputs present → the checker recomputes F and fires;
   (b) `:stored-F`: `:variational-free-energy` present → the checker recomputes and reports the identity
   **as a consistency check only, never as evidence** — the trace serialises `observed`, `predicted-mean`,
   `error`, `precision`, `weighted-error` at full precision and each is the arithmetic of its neighbours,
   so recompute = stored exactly (32/32, max |diff| 0.0: claude-13, claude-20, claude-15, three loops).
   *Amended 2026-08-30: the earlier "equal within ε or fires" clause was empty — it could not fail for
   any ε ≥ 0 — caught by claude-13's pre-dispatch read of R8-D2 (charter 6b, first use).*
   (c) `:insufficient-inputs`: no `:precision-state` (or no `:prediction-errors`) → typed absence, never
   a pass and never a fire.
   **Where the evidence sits (amended 2026-08-30, twice):** on population (a), the 755 forms whose F nobody
   has computed. The checker must (i) produce a finite F for every computable form and report the
   distribution (min/max/quantiles, non-finite count — expected 0, not guaranteed); (ii) reproduce the
   **two-population split** — stored F in [0.190, 0.522] (32), recomputed F up to 10.64 (755–758 by input
   filter, stated); (iii) **report the split BY ERA, not by field** (claude-20's finding, reproduced by
   claude-15 over 792 forms): partition on the `:free-energy` shape — `:G-total` map (760 forms, files
   05-18…07-09) vs `:controller-score` map (32 forms, files 07-14…07-21 and 08-30); no file holds both —
   and show that **four things move at one boundary, 2026-07-14**: `:variational-free-energy` appears,
   `:selection-gain` appears (pinned `1.0`, `:samples 0`, never moves), `:free-energy` is reshaped from G to
   controller, and mean per-channel `:precision` drops ~94.6 → ~9.5. Precision is the *proximate* arithmetic
   driver of the F gap (F = ½·mean(Π ε²); |error| 0.27 vs 0.31 and channels 8.0 vs 7.3 do not explain it) and
   the checker says so — but the record carries the era, not "a precision-scale effect", because a single dated
   schema change moved all four. `:unexplained-regime` stays a permitted outcome (a form that fits neither
   era, or a boundary that is not 07-14, is a finding). **Cause stays `inferred, untested`** — a
   precision-state reset, a channel recalibration, a change in which ticks were instrumented: D2 asserts none
   of these; it reports the boundary and the four co-moving facts with tick dates, and stops.
   **Expected values, stated before D2:** 755 / 32 / 5 over 792 forms in 53 files (D1, codex-12; claude-20
   reproduced); claude-15's own loop first gave 793 forms in 54 `.edn` files with 6 lacking `:precision-state`
   — the extra one is the dotfile `.lane-futility-index.edn` (one form, not a trace). **Filter, stated:**
   `data/wm-trace/wm-trace-*.edn`, dotfiles and `wm-shadow-step.json` excluded → 53 files, 792 forms.
   D2 uses that filter and reports the file and form counts; a checker that fires on all 792 or passes all 792 is
   wrong in one of two directions.
   "operational alarm" and no more — no claim that F is the variational free energy of a generative model.
2. **g, honestly named:** `expectedCoverage`, `realized`, `perf`, `g`, `τ_eff` as an engineering controller
   with its five deliveries typed (`Delivery` fields §4: `atomic-with`, `receipt`, `guarantee`, `idem-key`),
   and the **L2 refusal** stated as a type: g may be updated only from pairs whose `realized` the model did
   not produce. Refusing witness: the whole corpus (all L1). **Blocked** for anything that calls g a
   precision or π a policy — that half waits for the spine (Outcome, Policy).

**facades:** a key named `:free-energy` that holds G; the 18/18 fixture test (−0.6 → −0.6); 88 outcomes on
one policy offered for "any mission"; tests on `with-redefs` producers; τ "repaired" by renaming γ→g.

**status.** open.
**holder.** claude-20 (tech lead) → codex-12 (R8-D1)  
**parent.** BUILD  *(fifth precept, §0.10 — added 2026-08-30)*

## Edges (overlap points)
`R7→R8` (drawn), `R8→R5` (drawn), `R10→R8` (drawn), `R1→R8`, `R2→R8`, `R8→R14` (derived — the gain chain,
from the glossary). Payload `R8→R5`: `{tick, F}`; `R8→R14`: `{tick, g, τ_eff, n-samples, layer}` — the
`layer` field is what R9 checks. Schemas fixed in `P-control-map-lint.md`'s fixtures.

## deliveries
- **R8-D1 — discovery, no code.** Census over the trace: schema versions; which records carry
  `:prediction-errors`/`:precision-state`; recompute F for each and compare with whatever sits under
  `:free-energy`/`:variational-free-energy`; the `:selection-gain` state across ticks (expected: 1.0
  throughout, 0 samples). ≤ 200 lines, tick ids and file:line. Refusal permitted.
- **R8-D2 — build (after review).** The F checker whose acceptance is (i) the three-disposition
  census (755 / 32 / 5 over 792 in 53 files, filter stated), (ii) the recomputed distribution over the
  missing-F population with non-finite count, (iii) the two-population split reproduced and attributed
  to a named per-channel field (expected: precision, ~10×) or reported `:unexplained-regime` with the
  per-field comparison, (iv) the stored-F identity reported as a consistency check labelled tautological.
  Fixture requirements from claude-13's read: a synthetic form with `:precision-state` present and
  `:prediction-errors` absent (the corpus's 2 are a subset of the 5, so a one-test implementation looks
  correct); the reader carries a `:default` tagged-literal handler (edn/read throws on this corpus
  without it); the "32 = schema ≥ 7" claim is untested (`:schema-version` reads nil on all 792) and is
  not to be asserted. kondo/parens; deterministic. *(Rewritten 2026-08-30 after claude-13 refused the
  recompute-vs-stored bar as empty; claude-20 and claude-15 verified.)*
- **R8-D3 — build (after D2).** The five deliveries e1–e5 written as `Delivery` records with schema +
  fixture each, and the L2 constraint as a stated type with its refusing witness — no g update code.
- **R8-D4 (blocked on spine).** g over the Outcome carrier.

## log
- 2026-08-30 record written (claude-15).
- 2026-08-30 amended (claude-15): '88 trace records' was the outcome count, not the record count.
- 2026-08-30 acceptance (iii) amended again: split reported BY ERA (07-14 schema boundary; four facts co-move), precision named as proximate driver only — claude-20's finding on completing R8-D1 checklist item 4, reproduced by claude-15 (760 G-shape / 32 controller-shape, no mixed file).
- 2026-08-30 **R8-D2 refused by claude-13 at its pre-dispatch read (charter 6b, first use) — correct.** Disposition (b) was empty (identity, 32/32 exact zeros); evidence moved to the 755 recomputed values and the two-population split, attributable to precision scale (~10×, claude-15 probe). Falsifier and D2 bullet rewritten; cause left untested.
- 2026-08-30 F-and-g-co-instrumented fact added to *now* (claude-20 corrected its own inferred 'different sets' within one turn by running the probe; claude-15 reproduced).
- 2026-08-30 R8-D2 delivery bullet rewritten to match the amended falsifier (claude-20 caught the stale text and quoted it as superseded rather than reconciling it).
- 2026-08-30 amended (claude-15): falsifier rewritten as three dispositions after R8-D1's refusal (the packet's one-direction form was wrong in two directions: 32 forms already carry F; 5–6 cannot compute it). Owner gate on R8-D1: PASSED.

**Lean interface (2026-08-30):** `mathlib4/DarkTower/WarMachine/Holes.lean@6ee2d0db` — `R8Tick`, `r8Disposition` (closed), `r8Census` and `r8EraBoundary` (holes). Build packets quote these; the lane closes when the hole moves.

**Corrections to the era finding (claude-13's fourth read via claude-20; verified by claude-15 at source,
2026-08-30):** (1) `:free-energy`, `:variational-free-energy` and `:selection-gain` are three keys of **one
unconditional map literal** (`scripts/futon2/report/war_machine.clj:4664, :4665, :4687` — no `when`/`if`
guards any of them), so "F appears ↔ selection-gain appears ↔ controller shape" is a write-site identity,
not three co-moving facts; zero mismatches confirm the source, not the world. (2) The only *contingent*
conjunct is date-contiguity — the stored-F forms are exactly a contiguous date suffix (non-interleaving,
which rollback, replay or backfill could have broken) — and because the boundary 2026-07-14 was read off the
data, "0 violations at 20260714" tests contiguity, not the date. Acceptance (iii) reads accordingly:
report the write site, the contiguity, the precision ratio, the boundary as observed; cause untested.
`r8EraBoundary`'s docstring corrected in the same terms.
**The corpus is live and cannot be sha-pinned:** `data/` is gitignored (`futon2/.gitignore:46`) and
`wm-trace-2026-08-30.edn` (1 form, 523 KB) was written **today at 10:54 by a runner nobody in this build
has identified** — no cron entry, user timer or live process found; the 792/53 counts include it. R8-D2
therefore carries a **content pin** — 53 files / 792 forms / digest `c434950f2e6a7e9b` over the sorted forms
(claude-20's computation; method to be stated in the packet so it can be re-derived) — so a later
disagreement reads as corpus drift, not checker defect. **Finding for Joe:** a War Machine tick with no
known holder (fifth precept) was written this morning.

**Lean interface @`6fd8a33f4d`:** `R8TickLit`, `wmTraceR8` (hole: the transcribed corpus, digest c434950f2e6a7e9b), `r8CensusWmTrace : r8Census wmTraceR8 = (755,32,5)`, `r8EraBoundary` over `wmTraceR8`. R8-D2 (codex-12, running) must be told: its quoted block changed.

**Content-pin method (claude-20, 2026-08-30; in R8-D2 and R2-D2):** reader-loop every top-level form of the
53 `wm-trace-*.edn` files (dotfiles and `wm-shadow-step.json` excluded); `(hash form)` per form, rendered as a
string; **sort**; feed in that order to SHA-256 as UTF-8; first 16 hex chars → `c434950f2e6a7e9b`. Sorting
makes the pin order-independent: appending a file changes it, re-reading the same corpus never does. Both
lanes read the same live, gitignored corpus, so both carry it.

**R8-D2 owner gate PASSED 16:16Z** (codex-12, futon2 `be3a77d`: `checks/r8_f_contract.clj`, test 5/17/0,
report EDN, findings note; claude-20 first-line review). claude-15 re-ran the tests and reproduced the
missing-F distribution with an independent loop: 755 computable, min 1.847, median 5.963, max 10.638,
non-finite 0; 755/32/5; era 760/32 with zero boundary violations; precision 94.58 → 9.49. The three
quantities carry their labels in the deliverable: stored-F delta *tautological*; boundary *contingent
non-interleaving*; the gain/shape biconditionals *source consistency from one write site*.
**Two refusals, both upheld:** (1) the ∀-bound holes admit no honest body (codex-12 built against the
superseded forms — the analysis half stands, the hole-moving half is R8-D3); (2) the packet's content pin
did not reproduce (`c434950f…` vs the checker's `c9add16a…`) with the counts equal, and codex-12 refused to
call that drift *or* to adopt the packet's number, because the packet never fixed serialisation or
delimiters. **Ruling:** the pin is the checker's *published* method — `:sha256-over-newline-joined-sorted-form-sha256`,
value `c9add16a…` — from here; a pin whose method is ambiguous is worse than none, because the one
distinction it exists to make (drift vs method) is the one it cannot make.
**R8-D3 (next):** the hole-moving half against `Holes.lean@e3f65c5c`+: a generator that emits `wmTraceR8 :
List R8TickLit` from the 792 forms (facts only: predictionErrors?/precisionState?/storedF?/selectionGain?/
shape/fileDate), gated by `regenerate && git diff --exit-code`; `r8CensusWmTrace` and `r8EraBoundary`
discharged by `decide` or reported failing with the row; the pin value in the literal's header. Through
claude-13.

**16:25Z — `freeEnergyShape` was a verdict (claude-20's question at R8-D3; ratified `Holes.lean@32b92969`, JSON
`53c5e466`):** the generator decided `gMap`/`controllerMap` by inspecting keys and `r8EraBoundary` tested that
decision — the R9 `inDeclaredPart` defect one level subtler. `R8Tick` now carries the **facts** `hasControllerScore`
and `hasGTotal` (key presence), and `R8Tick.freeEnergyShape` is **derived** in Lean, with `unknown` (neither or
both keys) as a possible outcome that would be a finding. R8-D3's generator writes the two Bools from the record's
key set and nothing the era law tests.

**R8-D3 PASSED the owner gate 16:44Z — the holes moved** (codex-12, futon2 `639ca75`; claude-20 first-line).
`R8-D3-report.lean` (5,572 lines: `wmTraceR8` as 792 `R8TickLit` literals carrying only presence facts,
`hasControllerScore`/`hasGTotal`, `fileDate`) elaborates under the owner: exit 0; `generatedCensus` and
`generatedEraBoundary` discharged by `native_decide` — **axioms, now printed in the artefact:** `[propext,
Classical.choice, Quot.sound, <theorem>._native.native_decide.ax_1_1]` for both — a stated trust in the
compiler (charter 3a), not `sorryAx`. Regeneration reproduces both artefacts byte-identical. Numbers: census
(755, 32, 5); shapes gMap 760 / controllerMap 32 / unknown 0 (both-keys 0, neither-key 0); era violations
0/0/0 as three counts; date margin 20260709 → 20260714; pin `c9add16a…` under the ruled method. Bound in the
witness registry against contract `1b09974a`.
**First genuine `wrong-shape`:** the contract lint judges `r8EraBoundary` `:wrong-shape` — the report's era
section (`:era-counts`, `:era-metrics`, `:conjunct-violations`, `:date-margin`) does not inhabit the declared
evidence type `EraTable := {boundary, perEra : Era → {count, storedF?, selectionGain?, shape, meanPrecision}}`.
The numbers are right and the shape is not the one the theory said to gather — the apex judgement, mechanical.
**R8-D4 (owner's call on direction):** either the generator emits an `EraTable`-shaped section (the fields all
exist in the report already) or the Lean evidence type is loosened to what the run naturally produces; the
adapter record's rule says the type is derived from the law, so the default is the generator conforms.

**16:56Z — `EraSummary` rewritten (claude-13's two findings via claude-20; ratified `Holes.lean@2f68318a`, JSON
`f910a405`; 34 bodies / 32 holes):** (1) `count` beside `meanPrecision` made the type assert a false denominator —
three defensible populations gave three means (all 760 forms 94.4826; with `:prediction-errors` 758 → 94.4826;
with both keys 755 → 94.5845, the report's figure); *a mean is not a fact*: `precisionSum` and `precisionRecords`
are the facts, `EraSummary.meanPrecision` is derived (0 on an empty population — a typed absence). (2) single
`storedF : Bool`, `selectionGain : Bool`, `shape : FreeEnergyShape` per era presupposed the uniformity
`r8EraBoundary` tests — the evidence type could not express its own falsifier, so `:conformant` would have been a
certainty; now `storedFCount`, `selectionGainCount` and a `ShapeTally {gMap, controllerMap, unknown}`, with
`EraSummary.uniform` as the derived property the *law* rules on — a non-uniform era is representable, and a
"neither" form appears as a count, not a forced verdict. **R8-D4** conforms the generator to this type (the
denominator trap becomes structural: the population is a field). claude-13's advice taken: tell codex-12 the
answer, do not spend its box rediscovering it.

**17:01Z — units, the eighth member (claude-20's row-11 dry-run against the amended declarations):** the reported
mean 94.5845 is `520403.9349 / 5502` — per channel VALUE — while the docstring paired it with the per-FORM population
755 (which would give 689.28). A generator reading `precisionRecords` as forms would have derived a confidently wrong
mean; the fix that made the denominator a carried fact is what made this findable. **Ruling (option 1 with units in
the names):** `precisionValues : Nat` (values summed — the mean's denominator by construction) and `precisionForms :
Nat` (forms contributing — a fact a reader wants, never a denominator); docstring pair corrected to *5502 values from
755 forms → 94.5845*. `Holes.lean@HEAD`; R8-D4 conforms to it. Family rule gains a clause: **a denominator carries
its unit in its name.**

