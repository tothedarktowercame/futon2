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
