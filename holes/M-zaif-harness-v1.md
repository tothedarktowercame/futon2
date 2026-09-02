# Mission: M-zaif-harness-v1 — the harness edition, lifecycle-aligned and WM-fed

**Date:** 2026-09-02
**Status:** OPEN — HEAD through VERIFY drafted; INSTANTIATE runs as wm-contract U rows.
**Owner:** claude-1. Driver: Joe.
**Home:** futon2/holes/ (successor edition to `M-zaif-harness.md`, 2026-07-11, which stays
the v0 record; this document does not rewrite it).
**Lifecycle:** `futon4/holes/mission-lifecycle.md` — followed phase by phase, which is
the point: a conforming mission is WM-legible (see the phase→node table in IDENTIFY).

Cross-refs: `SPEC-zaif-harness-v1.md` and `SPEC-dormant-wiring.md` and
`NOTE-chipwits-iconography.md` (all `holes/labs/wm-contract/`); `p4ng/app-zaif.tex`
(the transplant route map); `aif-equations.edn` (the node registry);
wm-contract worklist rows U1–U6 (minted) and U7–U9 (stubs pending Joe).

## HEAD — operator anchor (2026-09-02, dictated, reported)

> "The kind of cool outcome is basically we're going to get the crew for the war
> machine by building it ourselves, because ultimately we can use these zaif agents
> as the crew." · "We can test individual tools one at a time. And we can test the
> reporting workflow through the WM edge facilities that we've already built." ·
> "How each of the R number nodes map to individual harness components… that was
> already spelled out in an earlier appendix for the PLoP paper." · (And, for the
> iconic language:) "pull the ChipWits FORTH repository… make a little fun analogy
> between ChipWits and these agentic models and their harnesses."

**Already felt to be true:** the wiring exists and has staged evidence (S2–S4); the
transplant route map is landed to its last row; the analogy carries real design
content (the Workshop/constructor identification is exact, not decorative).
**Anti-glibness discipline:** every mapping row in the spec carries a unit test; a
mapping without a test is decoration and does not ship. The R17 stub is named, not
papered over. **Working-economy position:** this mission underwrites the WM's crew
(agents whose competence is an inspectable board) and is underwritten by the WM's
edge facilities (Z1 ledgers, typed events) and the library's policy-grain work
(§1c). **Carried tensions:** (1) arms→cascades is deliberately deferred to LA2/LA3 —
the U5 opaque-candidate seam is the promise that deferral is cheap; (2) the ask
arm is structurally unreachable at shipped constants (Z3's A/B is the sanctioned
resolution, preregistration-gated); (3) whether the lifecycle→node mapping below
is a formal claim or a fertile analogy is itself carried forward, deliberately.
**Provenance:** operator dictation in session, 2026-09-02, three exchanges.

## 1. IDENTIFY

**Gap:** zaif v0 decides but is not live, the WM's theory-aligned score is built
but off by default, and neither has per-component tests. Nothing today lets us
say "this node's behaviour is ready to bring online" about either system from
evidence rather than hope.

**Theoretical anchoring:** the PLoP paper's inversion (declare, don't guess;
measure the guesser); §1c's unification (pattern ≡ policy at every grain);
active inference at mission grain — S3's own result that reward discrimination
clears its null *within* missions is why the mission is the clocking unit.

**The phase→node alignment (why lifecycle conformance = WM legibility):**

| Lifecycle phase | WM node(s) | What the phase deposits, in node terms |
|---|---|---|
| HEAD | R2 | operator voice as highest-precision typed observations — the declared-marks channel, at the point of authorship |
| IDENTIFY | R1/R3 | the belief state: gap named, scope bounded — what the mission holds to be true of its field |
| MAP | R2→R3 | survey as observation-gathering: facts folded into belief before any design; "produces facts, not decisions" is the R3 discipline verbatim |
| DERIVE | R4/R5 | design alternatives as forward models; IF/HOWEVER/THEN/BECAUSE is a G-comparison over candidate designs, risk and ambiguity named |
| ARGUE | R6 | pattern cross-references are recall warrants entering the posterior — the library speaks for or against the chosen design |
| VERIFY | R9 | independent witness before action: the BoM and structural checks adjudicate the design externally, never by the designer's report |
| INSTANTIATE | R16 | the action, witnessed: build rows with receipts |
| DOCUMENT | R17 | the learning deposit: docbook/library accumulation — what later missions inherit |

This table is how "the plan feeds into the War Machine as a whole": each phase's
exit artifact is typed the way the corresponding node consumes. When the build
reaches a node, the mission's matching phase already holds that node's test
fixture. (Carried tension 3 applies: treat as fertile until a row cashes it.)

**Completion criteria:** U6–U9 acceptances green; the reporting gate holds on one
real zaif decision; the honest gap list (which R nodes zaif exercises vs stubs)
published in the registry.
**Depends on / enables:** depends on WM edge facilities (landed) and U1's test
harness (queued); enables the Z3 A/B and, later, cascade-grain arms (LA2/LA3).
**Owner/repos:** futon2 (WM, this doc), futon3c (zaif code), p4ng (paper).

## 2. MAP — ready vs missing

Ready (no new code): the route map's landed rows — memory seam (closed learning
loop over Lean-checked analysis), checked handoffs mechanism, declared marks,
transcript persistence, text sidecar, ledger library Z1 (shipped, test-covered),
R14 fold ported (PZ3, has run over the live corpus), controller Z2 (built,
opt-in, replay-calibrated). Recorded fields S2–S4. The node registry with per-eq
sources. ChipWits source and manuals (`~/code/chipwits-forth`).

Missing (the actual work): per-node test suite (U6), per-tool R2/R16 pairs (U7),
reporting gate (U8), R7 precision table as data (U9), the R14 fold *consumed* by
zaif rather than just ported, live defaults (J-gated), Z3 (preregistration).

Surprises already recorded: the dormant units were built-not-unwired
(SPEC-dormant-wiring's finding); the appendix's handoff tallies were withdrawn
by its own audit — the mechanism is evidenced, its tally is not.

## 3. DERIVE

The design is `SPEC-zaif-harness-v1.md`'s table — R node → component → icon →
unit test — not repeated here. The three load-marked decisions, in lifecycle
form:

- IF arms will become cascades (LA2/LA3), HOWEVER building tests against arm
  internals would make that migration a rewrite, THEN candidates are opaque at
  the R6 seam (U5 invariant), BECAUSE a test that names the node, not the
  candidate's insides, survives the type change.
- IF operator signal is the densest evidence channel, HOWEVER lexical detection
  measured 0.42/0.19, THEN declared marks are the instrument and the lexicon is
  a seeder only, BECAUSE "measure the guesser first" already ruled.
- IF v0's constants are fixed by design, HOWEVER learning must eventually move
  them, THEN R17 ships as a named stub with a drift-pin test and Z3 as the only
  sanctioned change vector, BECAUSE silent tuning is the facade the paper
  refuses.

Wiring diagram: the futon5 exotype question is live for v1 (components, loops,
multi-repo interfaces — three "yes" answers); sketch during this phase or record
the priced skip in VERIFY.

## 4. ARGUE

Pattern cross-reference (the library speaks): `snatch/have-a-temperament` — the
crew member's board is authored, not driven; `snatch/probe-before-committing` —
U7's per-tool tests before U6's integration; `snatch/an-unmodelled-response-
stops-the-line` — the typed-absence discipline at R2/R14;
`statement-ladder-before-proof-text` — spec rows before test code. Rationale:
§1c (pattern ≡ policy) is the ruling this design rests on; the crew model is
its practical reading.

## 5. VERIFY

The BoM is the spec's table itself: one row per node, each naming its formalism
(typed record shape + test) or its priced decline (R17: stub named, drift
pinned; R13: horizon pinned). Structural check: U6's per-node planted
expectations are the completeness/orphan check in executable form. Spike: U6 on
a single recorded decision is the risk-reduction spike before any live default
moves.

## 6. INSTANTIATE — feeding the build

Runs as wm-contract rows, in order: U2/U4 (record-only, in flight), U1 (seam
tests), U6 (end-to-end fitting), then U7/U8/U9 on Joe's mint. Each row's
delivery feeds the node its phase-fixture already typed: tool pairs → R2/R16,
precision table → R7, fold consumption → R14, reporting gate → the Z1 edge.
Default flips and Z3 remain J-gated acts, not row side-effects.

## 7. DOCUMENT

On completion: update `app-zaif.tex` (the route map gains its test column and
loses nothing silently), registry rows for what U-tests pinned, a docbook entry
for the harness edition, and the honest gap list published where the crew's
future briefings will read it.
