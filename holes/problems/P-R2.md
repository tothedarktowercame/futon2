# P-R2 — The observation vector: declared channels, and the one channel it does not read

Problem record (delivery-lifecycle v2). Node R2 (PERCEIVE). Lane 2 of the R-node build.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. Source: `futon2/holes/labs/wm-contract/R2-glossary-formalisation.md` (carry its §3–§5 forward; do not re-derive).

## S1

**problem.** R2's noun is stack-defined: a 14-channel vector (`observation.clj:18–32`) whose docstring says
13, with two trace records carrying 13 keys — schema drift on 05-18 with no receipt (worksheet §4 e1). Eight
channels have a likelihood; six are delivered to no consumer (`:n-a-by-design`) and the record cannot tell
present-and-unused from absent. The ring finding (worksheet §0): the one channel the machine cannot
fabricate — a typed operator turn (✘ ✓ 💡) — is the one channel it does not read; `:acknowledged?` has no
producer and `nag?` is a 4-term AND with one input unwired (e6). PREREG invariant: the vector's key set
is a type, not a docstring.

**now.** Exactly-once turn storage is witnessed (e3, C1 20/20) — storage, not reading. `gen_turn_chain.py`
joins turns to patterns for the *paper* (e4), not for the vector (e5 missing). `promotion-tests.edn:36–50`
states the requirement `readsTurns` (an inference over a ≥111-item window must differ when the turn channel
is held constant) with two refusing witnesses named and no accepting witness — no tick has ever carried the
channel.

**solved:**
1. `Channel : Type` enumerated (14), `Observation := Channel → [0,1]`, `likelihood : Channel → Option …`
   (None for the six, as a type), and the record contract `∀ tick, keys tick.observation = Channel` checked
   over `futon2/data/wm-trace/` with the **reader loop over all top-level forms** (never `edn/read-string`).
   **Falsifier:** the two 13-key records of 05-18 must fire; if the checker passes the whole corpus, it is
   reading the first form only (the error of 08-27, lifecycle log row 1).
2. The turn channel: **blocked on a design decision** (what content of a turn→pattern association
   normalises to [0,1]; what hidden state a ✘/✓/💡 bears on). The deliverable for this half is the refusal
   with the two candidate answers laid out for Joe — not `:operator-turn-count`, which the excursion already
   names as the defect recreated (presence, not content).

**facades:** "13 harmonized channels. Real." (a count, and the wrong one); a docstring as a schema; a
turn *count* channel; `:morning-brief-*` present-and-empty read as observed; the C1 20/20 offered as
evidence of reading.

**status.** open.
**holder.** claude-20 (tech lead) → codex-8 (R2-D1)  
**parent.** BUILD  *(fifth precept, §0.10 — added 2026-08-30)*

## Edges (overlap points)
`R2→R3` observe (drawn), `R16→R2` re-observe (drawn), `R2→R8`, `R2→R7`, `R10→R2`, `R9→R2` (derived).
Deliveries e1–e7 as in worksheet §4, each with the undeclared field named there; the edge schemas are fixed
in `P-control-map-lint.md`'s fixtures. Payload for `R2→R3` and `R2→R8`: `Observation` with its `Channel`
key set as the schema and `receipt = {tick, key-set-ok?, consumed-by}`.

## deliveries
- **R2-D1 — discovery, no code.** Census over the trace with the reader loop: per record, schema version,
  key set, which channels carry a likelihood, which are `:n-a-by-design`; the two 13-key records identified
  by tick; `:acknowledged?` producers (expected: none); `:morning-brief-*` present/empty/absent counts.
  ≤ 200 lines, file:line and tick ids. Refusal permitted.
- **R2-D2 — build (after review).** The `Channel` type + record-contract checker as a check script
  (`futon2/checks/` or where review says), acceptance = the falsifier fires on the two records and nothing
  else; kondo/parens; run twice, deterministic.
- **R2-D3 — decision, not a build.** The turn-channel design question written up for Joe from D1's data.

## log
- 2026-08-30 record written (claude-15).

**Lean interface (2026-08-30):** `mathlib4/DarkTower/WarMachine/Holes.lean@6ee2d0db` — `r2WellFormed` (closed) and `r2ContractCensus` (hole; expected illFormed = 2 — the refuting run is the fixture). Build packets quote these; the lane closes when the hole moves.

**Lean interface @`6fd8a33f4d`:** `R2TickLit := R2Tick (Fin 14) Unit` (declaration order), `wmTraceR2` (hole: transcribed corpus), `r2ContractCensusWmTrace : r2ContractCensus wmTraceR2 (all-14-present) = 2`.

**Queued Lean change (16:09Z, claude-13's R2-D2 read via claude-20; held until AD-D2 releases `Holes.lean`):**
`R2TickLit := R2Tick (Fin 14) Unit` pins the channel *arity*, not the channels' identity or declaration
order — `Fin 14` cannot say "these fourteen names in this order", so discharging `r2ContractCensusWmTrace`
is NOT evidence that `Channel` was source-keyed; that rests on the fixture-corpus test and the
declaration-order vector in the Clojure report, and the packet now says so. Proposal to ratify at G-D3/after
AD-D2: `inductive Channel` with the fourteen named constructors in the order of `observation.clj:18–32`
(evidence: the names themselves; falsifier: a fifteenth key in any tick), and `R2TickLit := R2Tick Channel Unit`.

**Ratified 16:13Z (`Holes.lean@e3f65c5c`):** `inductive Channel` with the fourteen named constructors in declaration order; `Channel.all`; `R2TickLit := R2Tick Channel Unit`. R2-D2's Lean quote at `6fd8a33f` is now commentary-and-signature stale for `R2TickLit` — the builder's next bell gets the new name (the census law's type is otherwise unchanged).

**R2-D2 PASSED the owner gate 16:31Z — analysis half; hole not moved** (codex-1, futon2 `a74ac42`). Tests 3/19
re-run; the discriminating fixture corpus is real (five records each carrying `:undeclared-fifteenth`, each
asserted to fire — the test a trace-derived `Channel` cannot pass); `:channel` reports the ordered vector from
`observation.clj:11` `observation-channels`; census 790 conforming / 2 firing. The content pin is published
under the *superseded* method (`:sha256-over-concatenated-sorted-clojure-form-hashes`, `c434950f…`) — honest,
since the packet stated it and the newline-joined ruling landed after dispatch; two published methods differing
only in delimiter is exactly the ambiguity the ruling closed. **R2-D3:** re-derive the pin under the ruled
method; `wmTraceR2 : List R2TickLit` over the named `Channel` inductive by a generator gated by `git diff
--exit-code`; `r2ContractCensusWmTrace` by `decide` (expected 2). The turn-channel refusal stands.

**16:45Z — CORRECTION to the correction:** the "does not elaborate" finding above was made on the
**working tree**, which codex-1 was editing for R2-D3 at that moment; the committed `R2-D2-report.lean@a74ac42`
**elaborates (exit 0)** and proves its `example` by `native_decide` (generated axiom
`…r2CensusCheck._native.native_decide.ax_1_1`) — claude-20 re-ran the committed blob; claude-15 reproduced.
So R2-D2's hole did move, with a stated-after-the-fact trust in the compiler; the remaining R2-D3 work is the
naming gap (wire the literal to `wmTraceR2` over the named `Channel`), the pin under the ruled method, a named
theorem, and `#print axioms` in the file. **Rule from this:** a hole-moving artefact is gated *at its sha*, never
at the working-tree path (charter 3a(i) amended).

**R2-D3 PASSED the owner gate 16:53Z — the holes moved** (codex-1, futon2 `4eb4f58`; claude-20 first-line;
gated at the sha). `wmTraceR2` emitted as the 792-entry literal over the named `Channel` inductive;
`r2ContractCensusWmTrace` (= 2) discharged by `native_decide` — **axioms** `[…r2CensusWmTrace._native.native_decide.ax_1_1]`,
no `sorryAx`; the trust choice is *measured*, not asserted: kernel `decide` hit `maxRecDepth`, then exhausted
200,000 heartbeats at raised depth; `native_decide` compiled the same proposition in 1.87 s (codex-1). Pin
`c9add16ac96c973b…` under the ruled method — identical to R8-D3's — and codex-1 stated its limit unprompted:
*the matching digest establishes that R2 and R8 enumerated identically; it cannot establish that their shared
filter selected the correct corpus.* Undeclared keys 0; regeneration reproduces both artefacts (owner re-ran).
Bound in the witness registry against contract `1b09974a`. **Open on this node:** the turn channel (R2-D3's
design decision, Joe's), and R2-D4 = the generator emits a named theorem and `#print axioms` (charter 3a(iii)).
Lean for R2: 2 bodies / 2 holes, both holes witnessed.

**16:59Z:** the `observation.clj` docstring's 13-vs-14 drift — confirmed from a third lane (R16-D1) — fixed by the owner (one word); the instance is preserved here, in the R2 worksheet, and in `a74ac42`'s report (`:docstring-count 13`). It was a facade instance, not evidence R2 depends on.

