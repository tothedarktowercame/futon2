# CASCADE-RUBRIC-SCORING-2026-09-12 — WM fold wirings scored against the turn-as-structured-proof rubric

Discovery packet, step 1 of `NOTE-cascade-structured-proof-validation.md` (a2144859).
WM side scored by zai-5, 2026-09-12. Read-only: no schema, gate, or loop changes.

## 0. Finding #1 first, because it dominates everything below

**Persisted fold wirings in the WM record are essentially ONE full exemplar.**
The last ~10 days (RUN4 era, 2026-09-10 → 09-12) contain **zero**: all twelve
construction checkpoints across the six RUN4 cohorts
(`/home/joe/run4/*/cohort/*/attempt-*/003-construction.edn` — U88-codex20 v1/v2,
initialization38690, ea1-admission v1/v2, U88-zai-successor v1/v3/v4) carry
**`:wiring nil`** (pointer: any of those files, e.g.
`/home/joe/run4/U88-zai-successor-20260912-v4/cohort/run4-u88-zai-successor-20260912-v4/attempt-001/003-construction.edn`,
judgment key `:wiring nil`). The wm-trace files
(`futon2/data/wm-trace/wm-trace-2026-09-{01,02,04,07,11,12}.edn`) contain zero
occurrences of `fits-pattern`/`:boxes`. The gate passed every one of those turns
with no wiring at all — the numeric gate (`cascade-score>0`,
`coverage-score-delta<0`) cannot see the absence. That is the loudest single
fact in this report.

The one full exemplar is
`futon2/holes/labs/M-evaluate-policies/exhibit/fold-turn.edn` (2026-07-03,
Fable/claude-code turn over the ARGUE-exhibit cascade-3, 61 lines). Partial
precedents in prose exhibits: `holes/E-live-loop-3.md`,
`holes/ground-control-test-2026-07-06.md`,
`holes/labs/slush-demo/RUNNER-CONTRACT.md` (+ `render_cascades.py`,
`review_deposit.py`). No synthetic reconstructions were made; the sample is
what it is: **1 full wiring + 12 wiring-less RUN4-era turns.** Fewer than 8,
stated plainly.

## 1. Cascade length distribution

**RUN4 era (12 turns, 2026-09-11/12):** every cascade is the SAME two-pattern
halo — `iching/hexagram-43-guai` and `iching/hexagram-44-gou`, semilattice
descent both directions, `:co_app []`, `:cascade-score -0.703`,
`:construction-kind :selected-policy` (pointer: any 003-construction.edn
above). Patterns: **2. Boxes: 0** (`:wiring nil`). Wires: 0. Longest path: 0.
Policy-holes at the wiring level: 0 (the `:open-hole-count 2` in the selection
rationale is mission-level, not wiring-level). Joe's "two patterns long" is
not a metaphor — it is the exact, uniform number, eleven-plus times in a row.
Two later checkpoints (ea1-admission v2, zai-successor v4) add evidence
hyperedge ids (`e-7782c5f4…`, `e-d13a4076…`) to the descent list — references,
not additional patterns.

**The exemplar (`fold-turn.edn`):** cascade **6 patterns**
(`coordination/task-shape-validation`, `coordination/session-durability-check`,
`coordination/par-as-obligation`, `agent/budget-bounds-exploration`,
`aif/structured-observation-vector`, `aif/candidate-pattern-action-space`),
**6 boxes** (`:s1`–`:s6`), **5 wires** — a pure linear chain, so longest path
= 6 nodes, **2 `:policy-holes`** plus **2 in-box `:hole`** entries (4 holes
total). Evaluation block records `:coverage 0.75`, `:delta-g -0.75` with
method stated (`fold-turn.edn:58-61`).

## 2. Per-mark scoring

### The exemplar (the only scoreable object)

- **M1 goal and decomposition — PARTIAL.** The circumstance states have→want
  (`fold-turn.edn:7`); the six boxes decompose the work; but obligations are
  not named or numbered, and no box attaches to an obligation id — attachment
  is implicit in the `:role` prose. No obligation is discharged by prose
  alone (the `:produces` artifacts are named), so it half-clears the bar.
- **M2 warrant per step — PASS with a gap.** Every box carries
  `:fits-pattern` (pattern id) and `:addresses-however` (why-for-THIS-
  circumstance prose) — e.g. `:s1` at `fold-turn.edn:16-20`. The gap: no
  pattern **revision**, and no warrant **kind** (pattern vs worked example vs
  ordinary deduction is never recorded).
- **M3 condition status — FAIL.** No warrant's applicability conditions
  appear with status established/absent/unchecked. `:addresses-however` is an
  argument, not a statused condition list; there is no witness or obstruction
  pointer anywhere.
- **M4 holes are loud — PASS.** The best mark. Two `:policy-holes` each with
  a `:why` naming what grounds the gap instead (`fold-turn.edn:50-57`: D3
  "warranted by IHTB-1, not by a pattern"; D5a/D5b "outside this pattern
  halo"), plus two in-box `:hole` entries with `:wanted` (`:s3`, `:s6`). A
  hollow step is never scored as covered — coverage 6/8 is honest arithmetic.
- **M5 contrast — FAIL** (reviewed judgment). No load-bearing condition
  carries a premise-preserving counterexample or obstruction note. The
  closest thing is `:s2`'s "the 0.0 placeholder competed in (and structured)
  every 5th tick" — a retrospective incident, not a contrast for a condition
  of THIS construction.
- **M6 discharge evidence linked — FAIL** (reviewed judgment). `:produces`
  names artifact kinds (`:typed-score-vector`, `:legible-arena`) but no
  checker/Lean/review pointers; the evaluation block cites its method string
  but no run record. The turn is not traversable from goal to evidence
  without the surrounding mission transcript.

**Exemplar total: 2 pass + 1 partial + 3 fail ≈ 2.5/6.**

### RUN4-era turns (12)

All six marks **FAIL mechanically** — there is no wiring to score; the marks
apply to the fold artifact the loop was supposed to produce and did not
persist. The one mitigating observation: selection surfaces mission-level
open holes honestly (`:open-hole-count 2` in the rationale,
`…/002-selection.edn`), so M4's *spirit* survives at mission level even
while the wiring-level expression is absent.

## 3. Warrant reality check (within the exemplar — it contains both the best and worst available)

**Real warrant — `:s3` (task-shape-validation).** Box
(`fold-turn.edn:26-33`): "Admission gate (invariant I2): a row enters the
argmin only with a full typed decomposition OR an explicit provenance
marker; malformed rows rejected with gate attribution." The pattern's THEN
licenses exactly an input-admission gate rejecting malformed shapes; the
`:addresses-however` names the specific incident class this gates ("accepted
bare 0.0s made later evidence unverifiable"). The pattern's content does
work in THIS circumstance — real.

**Real warrant — `:s1` (structured-observation-vector).** Box
(`:16-20`): "persist EVERY term entering G-total … plus G-core as a
normalized typed map per candidate per tick." The pattern licenses typed
observation vectors; the circumstance (heterogeneous lanes arriving
incomparable) is the pattern's applicability condition almost verbatim; the
"prose-grade 0.0s" incident is the concrete failure it prevents. Real.

**Stretched — `:s5` (session-durability-check).** Box (`:36-40`): "no
flag-flip is acknowledged without archived before/after census +
counterfactual artifacts." The pattern is about coordination state surviving
session boundaries; the box uses it to license *evidence persistence for
config changes*. The THEN covers "persist before/after evidence" only at
reading-generous width; the specific census+counterfactual demand is the
circumstance's, not the pattern's. Plausible but stretched — the citation
would survive with the pattern replaced by any "durability" pattern.

**Decorative — `:s6` (par-as-obligation).** Box (`:41-47`): "every D-stage
lands with a PUR; mission close is gated on QA + the paper sentence." The
pattern licenses "have a retrospective obligation"; any close-out step
satisfies that, so the citation does no licensing work for THIS step's
specific content (the three candidate meta-patterns are the step's real
substance, and they are warranted by the mission, not the pattern). Citation
as vocabulary.

## 4. Mapping notes for the V4 checker probe (schema delta, not implemented)

Chosen wiring: the exemplar. The mapping:

- **boxes → nodes**: `:s1`–`:s6` map 1:1; node id = `:id`, node body = `:role`.
- **`:fits-pattern` → warrant**: id-only today. The checker needs, per box:
  `:pattern-id` (have), `:pattern-revision` (MISSING — pattern texts are
  mutable), `:warrant-kind` (MISSING — enum pattern/worked-example/deduction),
  and the **applicability argument** (raw material in `:addresses-however`,
  but unstatused prose — the checker needs condition triples
  `{:condition :status :witness|:obstruction}`; NOTHING carries these).
- **`:policy-holes` → open obligations**: have `:free`/`:why` (good — the
  "why no pattern grounds this" is exactly an obstruction note); MISSING:
  obligation id, owner, discharge gate — without them the checker cannot
  demand re-attachment on the next turn.
- **`:wires`/`:consumes`/`:produces`** → the DAG the Lean carriers already
  pin (acyclic, precedence); usable as-is.
- **MISSING entirely for marks 5–6**: any `:contrast` field (what breaks
  without the load-bearing condition) and any `:evidence` links (checker /
  Lean / review artifact refs per claimed-done step). The exemplar's
  `:produces` names kinds, not pointers.
- **And the demand-side gap that is bigger than any field**: the RUN4
  construction contract records `:wiring` but no gate requires it non-nil —
  the checker would demand persistence before it could demand structure.

## Judgment (one paragraph, as asked)

Joe's "minor change" reading is right, with one caveat. The gap is
demand-side, not capability-side: the fold interface already elicits the
warrant raw material (`:fits-pattern` + circumstance-specific
`:addresses-however` — in the one exemplar, 2 of 4 sampled warrants are
genuine licenses), `:policy-holes` is already the loudest hole mechanism in
the whole record, and nothing about marks 1–4 requires new capability —
they require the gate to *demand* wiring persistence and structure, which it
today does not (twelve straight turns passed with `:wiring nil`). That is
schema/gate work. The caveat is marks 5–6: contrast and
goal-to-evidence traversability were NOT produced even in the hand-run
exemplar (0/2), so "agents can produce them" is untested, not shown — I
would commission marks 1–4 as gate demands first and treat 5–6 as a reviewed
capability question the scoring will answer empirically. Capability is
unproven; obstruction is undemonstrated; the demand gap is demonstrated.
