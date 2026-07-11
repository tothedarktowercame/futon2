# M-composition-aware-reward

**Status: IDENTIFY complete (evidence below) → DERIVE/INSTANTIATE chartered
2026-07-11. Operator-armed (Joe): build R1+R2 as mission work through the
zai lane, reviewed per CONTROL-LAYER.md; then batch 5 runs under the result
and we compare.**

## HEAD

The learned proposal reward R̂ (reward_v0) is compositionally blind: it scores
pattern SETS with mean-pooled per-pattern features, so its argmax is a single
high-reliability pattern. The batch-4 canary (greedy-rhat, sealed-unflown arm)
exposed this on all three missions: single-pattern argmax at R̂ 2.77–2.82,
crushing every composition either flown arm produced. The GFN's low-β sampling
MASKS the degeneracy; raise β or train to convergence and proposals collapse
to safe singles. Meanwhile everything the fold lane values — multi-pattern
wiring, copar meets, obligation coverage — lives in structure the reward
cannot see. A single pattern is one PSR/PUR arrow: boundedly complex by
construction. Composition is what a cascade IS.

WANT: a reward whose argmax is an honest composition — R̂ v1 that (a) draws a
wireability signal from the fold corpus (the loop's only compositional ground
truth), (b) matches proposal size to want complexity instead of monotonically
penalizing size, (c) keeps all existing anti-gaming checks passing, and
(d) applies symmetrically to both R̂-consuming arms (gfn sampler + greedy
canary) with zero incumbent changes — per the system-grain S2 claim.

## IDENTIFY (evidence, all reviewer-verified 2026-07-11)

- Canary finding: TN-gflownets-fable-review.md checkpoint "BATCH 4 folding
  complete + THE COUNTERFACTUAL STUDY". Greedy = R̂ at temperature zero; its
  degenerate argmax is a faithful report of mean-pooled reliability dominance
  (fitted w_rel ≈ 2.2 at n=15) with a coverage term too weak to reward joint
  structure.
- Wiring corpus exists: 132 realized edges over 118 distinct pattern-pairs in
  21 deposits (futon6/data/fold-turns/ft-*.edn), incl. recurring pairs (AIF
  cluster wired 4×). Negatives exist too: overlap-holes and named
  non-contributions are anti-wiring evidence (Run 19's h1/h2; Run 5's h6...).
- Interaction risk: if psi-grain labels ever flow BEFORE this fix, narrow
  single-pattern folds mint true labels, reliability posteriors rise, and the
  loop entrenches the degeneracy (TN, same checkpoint).

## DERIVE (the slice contracts — small, testable, in order)

### Slice R1 — wireability prior from the fold corpus
Build `wiring_corpus.py` (futon3a/holes/labs/M-memes-arrows/): parse ALL
deposits' wiring (boxes' :fits-pattern + hyperedges; reuse the parsing shape
of futon2/holes/labs/slush-demo/render_cascades.py, but as a clean function,
not a copy) → per-pair counts {seq, copar} AND negative counts (pairs
co-proposed in a deposit's :pattern-ids where at least one member ended as a
non-contribution/overlap policy-hole rather than a wired box). Output:
`wiring-corpus.json` + loader. TESTS: hand-verified counts for one known
deposit (ft-autoclock-in-002: b1→b2 seq etc.); the 4× AIF pairs present;
runs from any cwd.

### Slice R2 — reward_v1 = reward_v0 + wireability + size-match
`reward_v1.py` extending (not editing) reward_v0: adds
(a) wireability term: expected pairwise wiring affinity of the proposed set
    under wiring-corpus.json (Laplace-smoothed; pairs never seen = neutral,
    negative-evidenced pairs < neutral);
(b) size-match term: |proposal| vs want-obligation count. Obligation count =
    a simple, DOCUMENTED, mechanical rule over the psi (e.g., count of
    distinct HUNGRY-FOR clauses after salient-token filtering) — UNCALIBRATED
    v0, one knob, honestly labeled (warrant-threshold precedent);
(c) weights fit on the same 15 ground-truth labels, LOO;
(d) a NEW hard anti-gaming check: single-pattern argmax on a multi-obligation
    want = FAIL (codifies the canary finding as a permanent gate).
TESTS: all reward_v0 hard checks still pass under v1; LOO S3 under v1 ≥ its
shuffle null on n=15 (report the number — if it degrades below null, that is
a FINDING to report, not to hide; do not tune until it passes).

### Slice R3 (NOT in this mission — recorded as the follow-on)
GFN over wiring sketches (attach-actions building the DAG directly). Only if
R1+R2 residuals argue for it. See CONTROL-LAYER.md roadmap.

## INSTANTIATE — acceptance (preregistered, mechanical)

- **A1 (THE CANARY RE-RUN, the mission's point):** greedy-argmax under
  reward_v1 on the three batch-4 missions (M-operational-vocabulary,
  M-pattern-mining, M-legacy-sorry-cleanup): argmax size > 1 on every mission
  whose obligation count ≥ 2, sizes within ±2 of obligation count. Paste the
  three argmax sets + scores verbatim.
- **A2:** LOO S3 under v1 vs null on n=15 — reported with the number, pass or
  not.
- **A3:** all hard checks green, incl. the new degeneracy check.
- **A4:** reward_v0 untouched (v1 is additive; the scoreboard keeps reading
  v0 until the operator flips it — the flip is an operator decision recorded
  in CONTROL-LAYER.md).

## Follow-on (after this mission lands, separate arming)

Batch 5 on fresh missions under v1: GFN + incumbent + greedy canary all
regenerate; compare canary behavior, fold quality, and (when labels exist)
S-curves against the batch-1..4 record. "Put in another distinct mission and
see if results improve" — Joe, 2026-07-11.

## Gates (per RUNNER-CONTRACT.md, coding-mission form)

Artifact-only (no JVM, no meme.db, no substrate-2); tests green from a
DIFFERENT cwd, summary line pasted verbatim; file-relative paths;
stop-on-blocker (if the fold corpus is unparseable or the labels won't load,
report — never synthesize); prediction-error line in the bell-back.
