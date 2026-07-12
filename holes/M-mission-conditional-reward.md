# M-mission-conditional-reward

**Status: DRAFT 2026-07-12 (claude, on Joe's request — the TN queued "drafted
on request" at the batch-7 flip-threshold verdict). Not armed. Arming =
operator decision; slices then run through the zai lane per CONTROL-LAYER.md,
reviewed per RUNNER-CONTRACT.md coding-mission form.**

**Compute note (asked at drafting): NO GPU. Every slice is counts, cosine
similarities over a few hundred MiniLM embeddings, and a ≤6-feature ridge-
logistic fit on n≈39 labels — CPU-seconds. The GFN sampler is the tabular
conditional-logZ TB core over 24-pattern pools (CPU-minutes per batch).
GPU only enters at R3 (neural GFN over wirings) or a fine-tuned reward
encoder, both out of scope and pointless below hundreds of labels.**

## HEAD

R̂'s reliability posteriors are GLOBAL per-pattern by construction
(reward_v0.fit_reliability: one Beta(a,b) per pattern stem over all records,
regardless of mission). Four consecutive LOMO-below-null results (0.722 →
0.655 → 0.642 → 0.666 vs null 0.667 at n=39/22 groups) reached the
preregistered flip threshold: the label-poverty hypothesis is retired and the
mission-conditional-features diagnosis is official (TN-gflownets-fable-review,
batch-7 checkpoint). R̂ learns mission signatures it has labels for and does
not transfer.

THE TRAP THIS DRAFT EXISTS TO AVOID: the obvious fix — condition counts on
mission group (pattern-mission affinity tables, per-mission wiring priors) —
is undefined on a mission with no labels, which is exactly what LOMO tests
and exactly what every future mission is on arrival. Identity-conditioned
features can lift LOO while doing NOTHING for the decisive curve. Any v2
feature must be computable for a label-free mission.

WANT: a reward revision R̂ v2 whose mission-conditioning runs through mission
SIMILARITY, not mission identity — (a) reliability pseudo-counts weighted by
kernel similarity between the target psi and each labeled record's want,
(b) optionally the same kernel over the wiring corpus, (c) exact reduction to
v1.2 at the uniform-kernel limit, (d) judged on the LOMO transfer curve,
which is the research question — reported pass or fail.

## IDENTIFY (evidence)

- Transfer verdict: TN batch-7 checkpoint — LOMO 0.666 vs null 0.667,
  n=39/22 groups, fourth consecutive below-null; gap sequence 0.047 → 0.059
  → 0.047 → 0.001 (labels buy transfer too slowly to wait on).
- Mechanism: reward_v0.fit_reliability pools every record into one global
  Beta per pattern; features_v1 adds coverage/wireability/size — none
  conditions on which mission the want came from. Within-mission skill is
  memorized via the label set; cross-mission structure has nowhere to live.
- Composition point (TN): minting continues regardless; label breadth and
  this feature change COMPOSE — this mission does not pause the batch loop.
- Instrument already in hand: MATCHER.model (MiniLM) embeds psis for
  build_pool; the labeled records carry their want texts (fold_ground_truth);
  kernel-weighting is a loop over ≤40 records.

## DERIVE (slice contracts — small, testable, in order)

### Slice M1 — the mission kernel + a cheap kill-test
`mission_kernel.py` (futon3a/holes/labs/M-memes-arrows/): embed every labeled
record's want text with MATCHER.model (normalized); kernel
k(q, r) = softmax-over-records of cos(q, e_r)/τ, one DOCUMENTED bandwidth
knob τ; τ→∞ ⇒ exactly uniform weights. KILL-TEST (run before any reward
work): within-mission-group want pairs must be more similar than
cross-group pairs (report the two distributions + a rank statistic). If the
kernel cannot even separate the missions we HAVE, similarity-conditioning is
dead on arrival — report and stop; that is a finding, not a failure.
TESTS: uniform-limit exactness; determinism; runs from any cwd.

### Slice M2 — reward_v2 = v1.2 with kernel-weighted evidence
`reward_v2.py` EXTENDING reward_v1 (never edits it; v0/v1 untouched):
(a) reliability: per-pattern Beta pseudo-counts become kernel-weighted sums —
    a_p = PRIOR_A + Σ_r k(q, r)·N·[p ∈ used_r ∧ success_r], b_p symmetric,
    where N keeps total evidence mass comparable to v1.2 (document the
    normalization; do not tune it);
(b) wireability (optional sub-slice, only if (a) alone moves LOMO): pair
    counts kernel-weighted the same way;
(c) weights refit with the same fit_weights_v1 shape (same features, new
    evidence) — no new fitted feature axes;
(d) τ selected INSIDE each LOMO training fold (small grid, e.g. 4 values,
    picked by within-fold LOO) — never on the held-out mission. τ is the
    only knob; log the per-fold choices.
TESTS: τ→∞ recovers v1.2 scores to numerical tolerance on all records (the
regression guard); all v0/v1 hard checks pass under v2 incl. the permanent
degeneracy gate.

### Slice M3 — the transfer verdict, preregistered
Rerun the exact TN evaluation battery on the UNCHANGED label set:
LOMO AUC under v2 vs its shuffle null, AND paired against v1.2's LOMO on the
identical records/groups; LOO S3 under v2 reported alongside (it may DROP —
trading memorization for transfer is the intended trade and is reported, not
hidden). No label additions inside this mission (label minting continues in
the batch lane; the comparison here is same-data, feature-change-only).

### NOT in this mission (recorded follow-ons)
Pattern-mission affinity via pattern-text × psi embeddings (a second
similarity channel); mission-group hierarchical shrinkage (needs more groups
with ≥3 labels); R3 GFN-over-wirings per CONTROL-LAYER roadmap.

## INSTANTIATE — acceptance (preregistered, mechanical)

- **A1 (THE POINT):** LOMO AUC under v2 > LOMO AUC under v1.2 on the
  identical label set and grouping — both numbers pasted verbatim, with the
  null. The research question is answered only if v2's LOMO ALSO clears its
  shuffle null; below-null-but-improved is progress reported honestly, not a
  pass.
- **A2:** M1 kill-test distributions reported (pass required to proceed to
  M2 — a stopped mission with a clean negative is a valid outcome).
- **A3:** τ→∞ regression: v2 ≡ v1.2 at the uniform limit, tolerance stated.
- **A4:** all hard anti-gaming checks green under v2, incl. degeneracy gate.
- **A5:** v0/v1 files untouched; scoreboard keeps reading v1.2 and gfn_live
  keeps consuming v1.2 until the operator flips (flip recorded in
  CONTROL-LAYER.md; on flip, both R̂ arms inherit v2 automatically per the
  system-grain claim — zero incumbent changes).

## Risks (named up front)

- Effective-n shrinkage: kernel weighting concentrates evidence on similar
  missions; at n=39 most groups carry 1–2 labels, so variance may eat the
  bias win. That outcome is the label-count answer and gets reported — no
  tuning past the preregistered τ grid.
- Kernel leakage: the kernel may only be re-encoding token overlap the
  coverage feature already carries; the M1 kill-test plus the A1 paired
  comparison bound this cheaply before any reward code is written.

## Gates (per RUNNER-CONTRACT.md, coding-mission form)

Artifact-only (no JVM, no meme.db, no substrate-2); tests green from a
DIFFERENT cwd, summary line pasted verbatim; file-relative paths;
stop-on-blocker (if adjudications won't load or the kernel kill-test fails,
report — never synthesize); prediction-error line in the bell-back.

## Checkpoint — cross-analysis from the lucy zaif line (claude-5, 2026-07-12)

The lucy session (M-zaif-harness, M-text-sidecar — all pushed) closed
PZ1/PZ2/PZ3 and built D1+U1. Four findings bear directly on this draft;
a laptop agent picking this up should read M-zaif-harness in full, but
the load-bearing intersections are:

1. **PZ2 measured the same sparsity this mission's Risk names, from the
   other side.** 38 operator-gold correction events spread over 17
   mission cells: 4 cells ≥3 events, one ≥5. "At n=39 most groups carry
   1–2 labels" is the identical structure. Consequence both ways: zaif's
   v0 γ(mission) cells start at the uniform prior (PZ2 verdict), and
   **this mission's kernel move is exactly the principled upgrade to
   that cold start** — kernel-weighted borrowing from similar missions
   instead of uniform ignorance. If M1/M2 pass, zaif inherits the
   backoff for free.
2. **M1's kill-test doubles as the test of zaif's backoff tier 2.** The
   zaif design's backoff ladder for sparse cells is (1) sorry terminal
   overlap → (2) shared prose-embedding space → (3) learned structural
   on demonstrated residual. The mission kernel (MiniLM over want texts)
   IS tier 2. One experiment, two missions: report the M1 distributions
   with both consumers in mind.
3. **The label-rate constraint has a measured relief valve.** PZ1: ~24%
   of operator turns carry corrections (est. 342 in a 40-day window vs
   2/21 flown folds); detection by lexicon is disqualified (recall .186)
   but the ✘/✓ declared-mark channel is live (precision 1.0 by
   construction, hydra on C-c .), and U1 gives agent-side acts as typed
   evidence for marks to reference. Mission attribution for these events
   should use the **autoclock witness, not text tokens** — PZ2 measured
   ~⅓ attribution loss from token-grep. A future R̂ evidence channel
   from mission-attributed marks composes with (never replaces) the
   flown-fold channel.
4. **Method transfer:** PZ1's shape — independent blind labels, gold
   pass decides, detector measured before trusted — is the same
   discipline as the M1 kill-test ("measure the guesser first"); and the
   PZ1→PZ2→PZ3 probe sequence (measure the instrument → census the
   grain → check the port) is a reusable template for reward-lane
   feature work.

Infrastructure now available on lucy that this mission could query if it
ever wants the operator-turn corpus: the D1 FTS5 sidecar
(`/api/alpha/evidence/text-search?q=…`, BM25, composes with
author/since), and `memory-search :text` through the standard envelope.

**Laptop-side follow-up (claude-5 laptop, 2026-07-12):** port recon done —
see M-zaif-harness.md §"Checkpoint — laptop-side port recon" for the B0–B2
slice contracts. B2 is this mission's triangulation lane: kernel-geometry
(M1) vs operator-behavior (marks→γ(mission)) as two independent estimates
of mission difference. One footgun found en route: ✘/✓ are stripped by the
D1 tokenizer — the marks channel needs B0's typed tags to be queryable.

## INSTANTIATE — OUTCOME (2026-07-12, futon3a e0b7972)

Execution: code authored by codex-2 (the dispatched handoff hit the
harness's 30-minute job cap mid-M3, before any run of the battery); review,
battery run, and this report by claude-5 laptop (operator had paused Agency
— no re-dispatch). All gates re-run from a different cwd; verbatim lines in
`reward_v2_battery_2026-07-12.log` (committed beside the code).

- **A2 MET — kill-test PASS:** within-mission want-pair cosine mean 0.3741
  vs cross 0.1648 (medians 0.2710/0.1581), rank AUC 0.7758 over 22/719
  pairs. The kernel separates the missions we have labels for.
- **A3 MET, exactly:** τ→∞ max |v2 − v1.2| = 0.000e+00 across all 39
  records (tolerance 1e-9). N-normalization = len(train) makes the uniform
  limit algebraically identical, not approximately so.
- **A4 MET:** trivial/bloated/degeneracy all PASS under v2.
- **A5 MET:** reward_v0/v1, wiring_corpus, gfn_live, scoreboard untouched
  (git-verified; only new files added).
- **A1 NOT MET — THE HONEST NEGATIVE, and the mission's real product:
  LOMO v2 = 0.645 (null95 0.701) vs v1.2 = 0.675 (null95 0.695), same
  records/groups.** Kernel-conditioning did not buy transfer; it cost
  ~0.03. LOO S3 v2 = 0.698 vs null 0.683 (within-mission still passes).
  The mechanism is visible in the per-fold τ choices: **21/22 folds chose
  τ = 0.15, the narrowest kernel in the predeclared grid.** Within-fold
  record-grain LOO rewards concentrating evidence on same-mission records
  — memorization — so selection drives τ narrow, and a narrow kernel has
  nothing to concentrate on when the held-out mission arrives. The named
  Risk ("effective-n shrinkage... variance may eat the bias win") landed:
  at n=39 the kernel move trades away the pooled evidence v1.2 uses.
  Per the preregistration this is reported verbatim, not tuned past.

VERDICT: mission similarity is REAL in the embedding (A2) but exploiting
it at n=39 makes transfer worse, not better. Both triangulation routes now
agree: the binding constraint is label mass/breadth, not features — which
is the zaif marks lane's case (M-zaif-harness B-slices) stated from the
reward side. Candidate follow-on IF ever armed (a new mission, not a quiet
fix): align the inner selection criterion with the outer objective (inner
leave-one-GROUP-out instead of record-grain LOO — 21/22 τ=0.15 says the
criterion, not the kernel, drove the narrowing). Scoreboard keeps reading
v1.2; nothing flips.
