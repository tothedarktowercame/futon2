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

## Operator decisions — RESOLVED 2026-07-11 (Joe)

1. **R̂ aggregation: FIXED** — M-composition-aware-reward closed; reward_v1.2
   (top-k, wireability, fitted size-mismatch, permanent degeneracy gate).
   Scoreboard S3 primary = v1.2 (v0 kept as reference column). Proposal
   generation (gfn_live) consumes v1.2 — both R̂ arms symmetric.
2. **Label grain: DEPOSIT-PSI is the primary unit** (mission-want kept as a
   slower parallel ledger where determinable). Safeguards REQUIRED:
   (a) psi-author ≠ flight-flyer (different runners — batch 3 collapsed this;
   restored as a hard rule); (b) pre-fold doc-freshness check (stale psi =
   corrupted label); (c) v1.2's size-match + degeneracy gate guard the
   narrow-psi Goodhart. Adjudication records carry :grain :deposit-psi with
   success true/false; the loader ingests them.
3. **Run-off stopping rule: CONFIRMED** — paired per-mission outcomes at
   deposit-psi grain; decisive at two-sided sign-test p<0.05; practical
   equivalence declared at 30 undecided pairs; ties count as ties.

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

## Side-channel quarantine (operator design point, 2026-07-11)

Folders may record `:library-alternatives` (existing patterns that would fit
better) and `:mint-candidate` holes (patterns that do not exist). QUARANTINE
RULE: this channel never influences the batch that produced it — not the
proposals, not the adjudication, not R̂'s features — because a folder feeding
proposals is a third, unblinded proposer. Downstream uses only:
(a) alternatives accumulate as RETRIEVAL-RECALL evidence (each one is a
measured build_pool miss that cosine metrics cannot see); (b) mint candidates
feed the pattern-authoring queue; (c) if alternatives ever look strong enough
to act on, the action is a NEW preregistered proposer arm, not a quiet edit.

## Flight-design notes (batch-6 doctrine)

- The bar is HIGHER on expected-TRUE flights than expected-FALSE ones: FALSE
  flights produce evidence toward a psi they cannot discharge; TRUE flights
  must produce the psi's EXACT expression (zai-7, B6-F8). Design acceptance
  accordingly: TRUE flights get correctness bars, FALSE flights get
  evidence bars.
- Psi-ambition confound (OPEN operator decision, batch 7): richer cascades
  draw more conjunctive psis from blind folders, which discharge less at
  flight scale — S1/S2 partly measures psi ambition. Mitigations on the
  table: obligation-count covariate in the paired test; fixed psi-scope
  budget in the folder contract; per-obligation discharge rates.
- Instrument flights (probes/monitors) must CALIBRATE against known truth
  before reporting (B6-F3: a port probe declared dead the very port carrying
  its own bell traffic). A monitoring artifact's acceptance includes one
  cross-check against an independently verified fact.
