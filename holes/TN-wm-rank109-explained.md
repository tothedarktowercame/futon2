# TN-wm-rank109-explained

**Why the rank-109 `:learn-action-class` beat every mission in attempt-023.**
Author: claude-9. Date: 2026-07-18. Answers acceptance item 1 of
`TN-wm-failure-to-launch` F1. Reproducible from
`futon2/data/wm-trace/wm-trace-2026-07-17.edn` (repro snippet at bottom).

## The mechanism, in one line

With a learned-frequency habit prior attached, `policy/select-action` picks
**argmax(−G/τ_eff + lnE)** (`policy.clj:242-266`), and in attempt-023 the habit
prior's span (~4.0 nats) was ~2.5× the *entire* G span of all 114 candidates
(~1.58), so the pick was decided almost entirely by lnE — i.e. by what the WM
has selected most often in the past — not by G, and therefore not by the
three-factor mission value inside G.

## The numbers (from the trace)

- τ_eff = 1.0: tau-mode is `:selection-gain-only` (the live arena default), so
  τ_eff = 1/g with selection-gain g = 1.0 (burn-in prior). The recorded
  `:tau-spread 0.3165` = range(G)/5 = 1.58/5 — consistent.
- G across all 114 ranked candidates: **31.754 → 33.337, range ≈ 1.58**.
- Habit prior lnE (state: 787 samples over 13 classes, α = 1.0, no recency
  decay): floor **−5.2204** for never-selected classes; **−1.2130** for
  `:learn-action-class :fire-pattern` (the historical favourite); **−2.3300**
  for `:advance-mission "M-learning-loop"` (the only mission ever selected).
- Scores (−G + lnE):

  | candidate | rank(G) | G | lnE | score |
  |---|---|---|---|---|
  | `:learn-action-class :fire-pattern` (winner) | 109 | 32.837 | −1.213 | **−34.050** |
  | `:advance-mission M-learning-loop` | 40 | 32.166 | −2.330 | −34.496 |
  | `:advance-mission M-expressions-of-interest` (top mission-value, mv 0.786) | 1 | 31.754 | −5.220 | −36.975 |
  | `:no-op` | 114 | 33.337 | −5.220 | −38.558 |

  The top mission-value candidate has the **best G of all 114** — 1.08 better
  than the winner's — and still loses by ~2.9 nats, all of it habit prior.

## What this establishes

1. **The three-factor model works and reaches G.** The top-value mission is
   rank 1 by G. The enrichment is not inert.
2. **But its authority is bounded by the G span.** Mission-value moves G by
   ≤ ~0.6 between missions; the frequency prior moves selection scores by up
   to ~4.0. At τ_eff = 1 the prior wins whenever it disagrees.
3. **The dark-room loop is structural, not incidental.** lnE is a smoothed
   frequency over past selections; a mission never selected sits at the −5.22
   floor forever, so the prior can only entrench historical behaviour. This is
   the same habit-prior surface as the original flat-G dark-room finding.
4. **The "repair the broken sense" story is post-hoc.** The selector did not
   know `:fire-pattern` was broken; it picked `:fire-pattern` because
   `:fire-pattern` is what it habitually picks. The pick was *lucky*, not
   *wise* — confirming the TN's "we cannot yet tell" caveat, in the
   unflattering direction.
5. **Two dials govern who wins**: the prior is deliberately unscaled by τ
   (D-1d seam: temperature modulates G, never lnE), so the balance is set by
   (a) selection-gain g — at g ≳ 2.5–3 the G term would dominate this trace's
   spread — and (b) the prior's span (α, decay). g was at its burn-in value
   1.0 for this click.

## Repro

```bash
cd ~/code/futon2 && clojure -Sdeps '{}' -M -e '
(require (quote [clojure.edn :as edn]))
(def t (edn/read-string {:default (fn [_ v] v)}
         (slurp "data/wm-trace/wm-trace-2026-07-17.edn")))
(->> (:ranked-actions t)
     (map (fn [e] {:rank (:rank e)
                   :type (get-in e [:action :type])
                   :target (or (get-in e [:action :target])
                               (get-in e [:action :target-class]))
                   :G (:controller-score e)
                   :lnE (:habit-prior-bias e)
                   :mv (get-in e [:action :mission-value-factor])
                   :score (+ (- (:controller-score e)) (:habit-prior-bias e))}))
     (sort-by (comp - :score)) (take 5) (run! prn))'
```
