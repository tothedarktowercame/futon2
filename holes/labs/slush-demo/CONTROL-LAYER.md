# CONTROL-LAYER.md — the review/orchestration protocol, cached

Written 2026-07-11 because the operator observed that this layer lived only in
one session's context and would be lost at close (the process-level version of
"announced is not sent"). RUNNER-CONTRACT.md is the runner's half; THIS is the
reviewer/orchestrator's half. A future session resumes the loop from these two
files + `review_deposit.py` + the TN's checkpoints, without re-deriving
anything.

## Roles and separations (violate none)

- **Proposer arms**: gfn (samples exp(β·R̂)), incumbent (`construct_cascade`,
  frozen legacy), greedy-rhat (argmax R̂ — the CANARY, sealed, never flown;
  KEEP verdict from the batch-4 pilot: it exposes reward degeneracy the
  sampler masks). All arms generated per mission by `slice2/gfn_live.py` from
  the same pool + reward snapshot; identities in *.SEALED.json.
- **Folder** (zai-N): blind to proposer, folds worklist entries only, per
  RUNNER-CONTRACT.md. Fresh runner per batch has worked well; the contract is
  the transferable asset (3 successive runners improved on it).
- **Reviewer** (claude owner): runs `review_deposit.py` on EVERY deposit +
  re-runs every claimed test gate from a DIFFERENT cwd + reads the analysis.
  Author ≠ reviewer always. Fix-don't-re-bell for mechanical defects;
  re-dispatch to the author when the fix is judgment work on their analysis
  (Run 19 precedent).
- **Adjudicator**: mechanical acceptance preregistered in FLIGHTS-*.md BEFORE
  execution; label grain recorded per record (:grain :flight-slice vs
  want-grain true/false). Ground truth: futon6/holes/fold-turn-adjudications.edn
  (the loader ingests only true/false; :flight-slice-pass is deliberately
  skipped — no subsumption).
- **Operator** (Joe): arming per batch; owns the open decisions below.

## Batch design invariants

1. Selection rule pre-committed and auditable (first unflown per arm in the
   sealed shuffle order + the mission's incumbent); worklist committed before
   any fold.
2. Mission BREADTH over depth (LOMO transfer ablation: R̂ does not yet
   transfer across missions; label breadth is what moves the real curve).
3. Dischargeable wants preferred (futonzero lesson: gate-blocked missions can
   only mint negatives).
4. ARM SYMMETRY: improvements to R̂ flow to both R̂-consuming arms
   automatically; the incumbent stays frozen under the SYSTEM-grain S2 claim
   (labeled amendment, TN 2026-07-11). Never improve one arm's inputs by hand.
5. Same-mission arm pairs: consider folding the two arms on DIFFERENT runners
   (Run 19's cross-fold contamination was mechanical and is now checked, but
   :via-prose independence between same-mission folds is NOT checkable).

## Pre-fold doc-freshness check (operator-endorsed, 2026-07-11)

Before dispatching a fold: probe the mission doc against landed reality —
(a) does the doc's latest phase log cite artifacts? spot-check they exist;
(b) is the doc's IDENTIFY the ONLY phase? then the attested want is from the
mission's FIRST phase and may be long-stale — check for later work in the
repo (lab dirs, src, tests named for the mission). If divergent, the fold
assignment must say so ("landed state is HAVE; fold against what REMAINS") —
batch-3's two doc-drift discoveries (autoclock's 4th rule; snapshot's 4/4
siblings) were found post-hoc; this makes it pre-fold. Stale-doc psis corrupt
any future psi-grain label.

## The NOT-CHECKED register (audit the auditor)

Known gaps in the battery — review these when something feels off, and ADD to
this list whenever a new gap is found (twice the reviewer was wrong: the sha
checker at Run 10; sixteen deposits never box-checked until Run 19):
- :via prose quality and independence across same-mission folds (soft leakage)
- satiety-call correctness (convention compliance is checked socially, not
  mechanically)
- wiring judgment (seq/copar correctness is the folder's claim; nothing
  adjudicates it — see wiring-feedback roadmap)
- the runner's test gates are re-run by the reviewer, but on the runner's
  fixtures — fixture quality itself is unaudited
- gallery/metrics parsing is regex-based, not an EDN reader

## Open operator decisions (blocking, in leverage order)

1. **R̂ aggregation fix** — mean-pooled reliability makes single patterns the
   argmax (batch-4 canary finding). Must land BEFORE the next retrain and
   BEFORE psi-grain labels flow (else the loop entrenches the degeneracy).
   Candidate: composition-aware term learned from the wiring corpus (below).
2. **Label grain** — deposit-psi vs mission-want. Interacts with (1).
3. **Run-off stopping rule** — proposed: paired sign-test p<0.05, practical
   equivalence at 30 undecided pairs. Pending confirmation.

## Wiring-feedback roadmap (design sketch, 2026-07-11 conversation)

Why cascades over single patterns: a single pattern is one PSR/PUR arrow —
boundedly complex by construction. Composition is what a cascade IS; wants
with multiple obligations need meets. The fold's wiring diagram (seq chains,
copar meets, terminals, non-contribution holes) is the loop's only
COMPOSITIONAL ground truth, and it is currently thrown away by the reward
(set-level features only).

Three rungs, cheapest first:
- **R1 — wireability prior from the fold corpus**: per pattern-pair, count
  realized edges across deposits (seq/copar separately), plus negatives
  (co-proposed but non-contributing/overlap-holed). Reward term = expected
  wiring density of the proposed set. Uses data we already have (21 deposits,
  grows every batch); symmetric across R̂ arms by construction.
- **R2 — want-complexity-matched size prior**: count distinct obligations in
  the psi; the size prior peaks near the obligation count instead of
  monotonically penalizing size. Kills the single-pattern argmax without
  hand-rewarding bloat (the 16-pattern giant stays penalized).
- **R3 — GFN over WIRINGS, not sets**: trajectories build (pattern,
  attachment) actions — attach p via seq/copar to the frontier — i.e., the
  GFN samples wiring sketches directly; reward evaluates the sketch. This is
  the natural GFlowNet formulation (DAG construction is what GFNs are for)
  and makes "cascade = sketch of the eventual wiring diagram" literal. Bigger
  lift; do R1/R2 first and let their residuals argue for R3.
