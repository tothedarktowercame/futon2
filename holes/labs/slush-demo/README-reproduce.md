# Reproduce & extend the GFN reward-gate line (as of 2026-07-10)

Everything below runs CPU-only, no live JVM required. Written for a fresh
machine (e.g. lucy); on the laptop it already runs.

## Repos + revisions (all on the default branch)

| repo | branch | rev | provides |
|---|---|---|---|
| futon2 | main | ≥ `410caf7` | TN + findings + `slice2/gfn_core.py` (trusted trainer) |
| futon3a | main | ≥ `32dcb09` | lab code (gates, `obligation_accuracy.py`), `meme.ch2` fold events |
| futon3 | (default) | any recent | the flexiarg pattern library (`library/**.flexiarg`) |
| futon6 | master | ≥ `ffa6f85` | closure-folds ground truth; phylogeny + label corpora (`data/`) |

Path layout assumed by the code: all four repos as siblings under `~/code/`.

## External data (NOT in git — by design, futon3a commit `6b6d9ad`)

1. `~/code/data/notions/` must contain at least:
   - `minilm_pattern_embeddings.json` (loaded at import by `cascade_construct.py`)
   - `pattern_posteriors.self_graded.json` (+ `.drop_log.`) — lazy fallback only;
     the tracked `futon6/data/pattern_posteriors.grounded.json` is preferred.
2. Symlink (per futon3a `README-data.md`):
   `ln -s ~/code/data/notions ~/code/futon3a/resources/notions`
3. Python env: `futon3a/.venv` with `sentence-transformers` + `numpy`
   (`python3 -m venv .venv && .venv/bin/pip install sentence-transformers numpy`).
   First run downloads `all-MiniLM-L6-v2` from HF (needs network once).
   `gfn_core.py` needs numpy only.

## Reproduce (each takes seconds–minutes, deterministic given seed)

```bash
cd ~/code/futon2/holes/labs/slush-demo
python3 -m pytest slice2/test_gfn_core.py -q          # 9 pass
python3 slice2/gfn_core.py --iters 700                 # G0a/G0b PASS,
        # conditional max TV 0.0125, logZ err 0.0289, shared-arm TV 0.9865

cd ~/code/futon3a
.venv/bin/python3 holes/labs/M-memes-arrows/aliveness_v3_gate2.py
        # fold-grain validity: 9 measurable, 6/7 positives > 0, anti-1=1 PASS
.venv/bin/python3 holes/labs/M-memes-arrows/aliveness_v3_corpus_gate2.py
        # mission grain: 83 scored, dense (83/83), AUC ~ chance (the grain-
        # mismatch proof); writes findings/ground_truth_mission_grain.json

clj-kondo --lint src/meme/ch2.clj test/meme/ch2_test.clj   # 0 / 0
clojure -M:test -n meme.ch2-test                            # 9 tests green
```

Headline numbers to expect are in `findings/slice1_5_validity_findings.md`,
`findings/ground_truth_expansion_findings.md`, and the checkpoints in
`futon2/holes/TN-gflownets-fable-review.md`.

## Extend (the approved next bit)

Goal: fold-grain ground truth at n≈60+ so the reward's discrimination test
(AUC vs shuffle null) is powered. Two lanes, independent:

1. **Prospective — CH2 fold events (schema DONE, wiring open).**
   `meme.ch2/fold-event` + `emit-fold-event!` (futon3a `32dcb09`) now represent
   failed folds; negatives require attempt evidence (`:used` and/or `:note`).
   OPEN: emit at the live-loop adjudication seam —
   `futon2/src/futon2/aif/fold_escrow.clj` produces the pass/reject verdicts
   (E-live-loop-2 step 2d); on each verdict, emit a fold event with the
   deposit's patterns as `:used`. Not wired here because verifying it needs
   the dev JVM (quiet window) + a golden-runner pass; it is a small, seam-local
   change once the JVM is available.
2. **Retrospective — curation (claude-1's method).** Mine mission/git history
   for hole-closures; each record needs judgment (hole, `:used`, outcome,
   resolver-blind problem text). Append to `futon6/holes/closure-folds.edn`
   honoring its header discipline (record failures; `:used []` records move no
   pattern posterior). GROUND in
   `futon3a/holes/labs/M-memes-arrows/discharge_experiment.py` lifts from there.

Acceptance for the reward (unchanged, v3 spec + TN):
distribution spreads; AUC(success>fail) beats the shuffle null on fold-grain
records; anti-`1=1` and bloated-shell controls score dead. Then — and only
then — Slice 2 trains `gfn_core.py` on aliveness_v3 with greedy-submodular +
matched-budget-annealing baselines.
