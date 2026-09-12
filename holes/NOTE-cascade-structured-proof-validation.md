# NOTE-cascade-structured-proof-validation — qualitative validation of pattern-first turns, APM V4 and WM

Commissioned by Joe 2026-09-12: the pattern-first framing planned for APM V4
transfers to the WM inner loop; both need qualitative validation of whether
it works; and before WM production "each War Machine turn will basically be
a Lamport-style structured proof — which is actually exactly what we wanted
to be doing all along." This updates the stance of
`labs/wm-contract/runs/WM-pattern-learning-follow-on-2026-09-10.md`, which
filed pattern/cascade transfer as future work and not a RUN4 prerequisite:
per Joe today, turn-level pattern quality is a pre-production concern for
the WM. (RUN4 execution wiring itself is unchanged by this note.)

Drafted by claude-15 from the current records; the WM-side sample scoring is
commissioned separately and will amend this. Status: PROPOSED.

## Where the two systems stand (evidence, 2026-09-12)

The APM V4 side has the structure but not yet the loop:
`holes/labs/M-apm-demonstration/analysis/pattern-first-blog-2026-09-11/`
(futon3c) shows the target shape — a goal decomposed into numbered
obligations, each structural choice carrying a warrant (which pattern or
definition licenses it), each warrant creating conditions to check, plus a
premise-preserving contrast showing which condition cannot be removed. The
offline V4 review component (10f2f96b) already enforces the bookkeeping:
an applicability argument AND a proof argument per node, no broken
references, no cycles (22 open review obligations on the retained
11-node reconstruction).

The WM side has the loop but a thinner structure: the fold contract
(`futon2/src/futon2/aif/fold.clj`) defines `cascade` as "the pattern halo
condensed around the mission's (have→want) meme" — a **flat pattern list**,
selected by score, folded by `fold_llm.clj` into wiring whose boxes carry
`:fits-pattern` and `:addresses-however` and whose gaps surface as
`:policy-holes`. The gate is numeric (`cascade-score>0`,
`coverage-score-delta<0`). The Lean carriers
(mathlib4 DarkTower/WarMachine F12CascadeDiffArm etc.) pin the cascade's
*graph* properties (acyclic, precedence, scores) — not its warrants.

So Joe's "cascades are sometimes two patterns long, without the
pattern-theoretic quality of a Lamport-style structured proof" has a
structural cause, not a tuning cause: the WM cascade is a retrieval halo,
not an obligation hierarchy. `:fits-pattern`/`:addresses-however` is
warrant-adjacent — the raw material is there — but nothing requires a goal
decomposition, per-step condition status, or an applicability argument, and
the gate cannot see their absence. This is the same shape as the APM
finding "ranking cannot decide applicability"
(`futon3c/holes/technotes/TN-APM-pattern-first-development-2026-09-10.md`):
a scored selection is not an argued application.

## The rubric (turn-as-structured-proof)

A turn (APM Student plan / WM fold wiring) rates on six checkable marks.
Lamport's discipline, adapted: numbered steps, each justified, references
only to established material.

1. **Goal and decomposition.** The turn states its target and decomposes it
   into named obligations; every box/step attaches to an obligation, and no
   obligation is discharged by prose alone.
2. **Warrant per step.** Each structural choice cites what licenses it —
   pattern id + revision, worked memory/example, or ordinary deduction —
   and which of these it is (the distinction is recorded, not inferred).
3. **Condition status.** Each warrant's applicability conditions appear
   with status established / absent / unchecked, with a witness or
   obstruction pointer. "Established" is a claim for review, not a
   certificate.
4. **Holes are loud.** Whatever the turn could not ground is a named hole
   (WM `:policy-holes` already does this; APM plans record open
   obligations). A hollow step scored as covered is the failure mode.
5. **Contrast present where a condition is doing work.** For the
   load-carrying condition of the main construction: what breaks without
   it (premise-preserving counterexample or obstruction note). This is the
   blog's "the contrast is part of the lesson."
6. **Discharge evidence linked.** Steps claimed done point to checker /
   Lean / review evidence; the turn's structure is traversable from goal to
   evidence without reading an agent transcript.

Marks 1–4 are mechanical (a checker can demand them — the V4 review
component already demands 1, 2, 4 for APM cascades and is close to
system-neutral). Marks 5–6 need review judgment; they are what the
qualitative scoring observes.

## Validation plan

1. **WM sample scoring (commissioned, discovery).** Take the recent fold
   wirings from the RUN4-era record, measure cascade length and warrant
   depth distributions, score each against the rubric with pinned
   artifacts. Deliverable: the actual distribution behind "two patterns
   long", and per-mark gap counts.
2. **APM baseline.** The pilot cases in the blog evidence index are the
   scored reference set (marks 1–5 visibly present in the finite-net case).
   No new work; cite, don't rerun.
3. **Checker transfer probe.** Run the V4 offline review component's
   bookkeeping (per-node applicability+proof argument, refs, cycles) over
   the sampled WM wirings, mapping boxes→nodes, `:fits-pattern`→warrant,
   `:policy-holes`→open obligations. Where the mapping fails, that is the
   schema delta for the WM fold output — expected to be small (Joe: "kind
   of a minor change").
4. **Gate proposal (after 1–3).** A WM turn enters the build only if marks
   1–4 pass mechanically; marks 5–6 sampled by review at a stated rate.
   Same gate shape as V4's TA review, sharing the triage rule (ordinary
   deductions don't need review) to keep reviewer load bounded.

Qualitative validation here means: after the gate, a reader can follow any
turn goal→warrant→condition→evidence without the transcript, and reviewers
looking at sampled turns affirm the warrants are real rather than
decorative. Whether the structure improves build *efficacy* is a separate
measured question (the I8 metric and build outcomes), not claimed by this
note.

Cross-references: `NOTE-runtime-validation-invariants.md` (I8, V1–V3);
`NOTE-apparatus-design-principles-index.md`;
futon3c `TN-APM-pattern-first-development-2026-09-10.md` and the
pattern-first blog evidence index.

## Amendment 1 — WM sample scoring complete (2026-09-12, plan step 1)

Report: `labs/wm-contract/CASCADE-RUBRIC-SCORING-2026-09-12.md` (zai-5,
f12e0fae; scoring pins spot-checked by claude-15 against the run artifacts
and the exemplar). Findings, which re-order the plan:

1. **Persistence is the dominant gap, ahead of any schema field.** The WM
   record holds essentially ONE full persisted wiring
   (`labs/M-evaluate-policies/exhibit/fold-turn.edn`, 2026-07-03). All
   twelve RUN4-era construction checkpoints carry `:wiring nil` — and the
   numeric gate passed every one. **Twelve green gate-passes over a missing
   artifact** is this note's primary WM exhibit for "the gate cannot see
   the absence" (apparatus patterns `done-is-observed-running` /
   `monitors-measure-the-work`; verbatim pointer:
   `/home/joe/run4/U88-zai-successor-20260912-v4/cohort/run4-u88-zai-successor-20260912-v4/attempt-001/003-construction.edn`).
2. **"Two patterns long" is the exact, uniform number** in the RUN4 era:
   every cascade is the same `iching/hexagram-43-guai` +
   `-44-gou` halo, score −0.703, eleven-plus consecutive turns, 0 boxes.
   The exemplar shows the fuller shape is attainable: 6 patterns, 6 boxes,
   linear chain, honest holes.
3. **Exemplar scores ≈ 2.5/6**: M2 (warrant-per-step) and M4 (loud holes —
   the best mechanism in the record) pass; M1 partial (decomposition
   without named obligations); M3 (condition status), M5 (contrast), M6
   (evidence-linked discharge) fail. Warrant reality within it: 2 real,
   1 stretched, 1 decorative (side-by-side quotes in the report).
4. **Schema delta** (for the fold output, per box): pattern revision;
   warrant-kind enum (pattern / worked example / deduction); condition
   triples `{:condition :status :witness|:obstruction}` replacing
   unstatused `:addresses-however` prose; obligation id/owner/discharge
   gate on holes; `:contrast` and `:evidence` fields for marks 5–6.
   `:wires`/DAG usable as-is (Lean carriers already pin it).

Verdict (zai-5, concurred by claude-15): Joe's "minor change" reading is
right — the gap is demand-side (gate/schema), not capability — with the
caveat that marks 5–6 were absent even in the hand-run exemplar, so
capability there is untested rather than shown. **Plan re-order:** the
checker-transfer probe (old step 3) is premature against nil wirings; the
first implementation slice is (a) gate demands `:wiring` non-nil, then
(b) marks 1–4 as mechanical schema demands, with marks 5–6 answered
empirically by reviewed scoring after structure exists. Ownership note:
the construction checkpoints live in the RUN4 runner chain; that chain has
a named owner and this note does not commission changes to it.

## Ruling (Joe, 2026-09-12): slice approved, with the no-op caveat

Joe approved parts (a) and (b), and strengthened the bar. Never-silent-nil
is only the floor: **validated work must produce and persist ALL the
records asked of it — cascade selected, scored, and a construction — a
turn that reaches done shows its work along the way.** "If it's not
actually able to write down a structured proof, it's not able to do a
proof — it's not doing anything." A refusal is therefore an exceptional
outcome, not an alternative steady state, and a refusal is itself work: it
produces a typed record with its grounds, and a recurring refusal class is
a candidate design pattern ("we thought this was a good idea; HOWEVER it
turned out not to be; THEREFORE refuse this class") — the flexiarg shape,
authored and reviewed like any other. A machine that mostly refuses is a
finding about the machine, not a compliant machine.

Joe also ratified I11 (handoffs compose) with a framing recorded here
because it bears on this note's rubric: handoffs-compose is the dynamic
version of a structured proof — handoffs are like Petri-net markers
flowing through a structured proof that is not yet written down. Petri
nets and structured proofs are well-understood mathematical objects; this
is a candidate formalization route for the handoff algebra
(`labs/wm-contract/SPEC-handoff-algebra-v0.md`), suggested not mandated.

Routing (claude-15's recommended split, proceeding under the ruling): the
fold contract (futon2 `src/futon2/aif/fold.clj`) defines the enriched
output; the RUN4 runner chain (codex-10's lane) demands it at the
construction checkpoint — requested of that lane's owner, not commissioned
past them.
