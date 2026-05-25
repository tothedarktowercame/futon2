(ns futon2.aif.free-energy
  "Free-energy computation and mode inference for the War Machine.

   The inference step between observation and render. Reads observation
   vectors from `futon2.aif.observation/observe` and preferences from
   `futon2.aif.preferences`, produces pragmatic / epistemic / total
   free energy plus a mode classification.

   cf. cyberants `ants/aif/policy.clj` — EFE computation
   cf. portfolio/policy.clj — action ranking
   cf. M-aif-head: the war machine integrates all heads, not replaces them

   Invariants:
   - WM-I1 (read-only — judge produces data, never writes)
   - WM-I4 (sovereignty — priorities are informational, not commands)"
  (:require [futon2.aif.preferences :as pref]))

(defn- channel-gap
  "Distance of observation from preferred range.
   Returns 0.0 if within [lo, hi], positive distance otherwise."
  [obs-val [lo hi]]
  (let [v (double (or obs-val 0.0))]
    (cond (< v lo) (- lo v)
          (> v hi) (- v hi)
          :else 0.0)))

(defn- in-avoided?
  "True if observation value falls within an avoided range."
  [obs-val [lo hi]]
  (let [v (double (or obs-val 0.0))]
    (and (>= v lo) (<= v hi))))

(defn compute-free-energy
  "Compute strategic free energy from observation vector.

   Returns {:G-total :G-pragmatic :G-epistemic :per-channel-gaps :avoided-active}.

   G-pragmatic: weighted distance from preferences (dominated by workstream balance).
   G-epistemic: uncertainty from dark arrows and unaddressed claims.
   G-total: 0.65 * pragmatic + 0.35 * epistemic.

   cf. war-machine-terminal-vocabulary.edn :G/pragmatic-fn, :G/epistemic-fn"
  [obs]
  (let [;; Pragmatic: gap between observations and preferences
        per-channel (into {}
                          (for [[ch pref-range] pref/preferences
                                :let [v (get obs ch 0.0)
                                      gap (channel-gap v pref-range)]]
                            [ch {:value v
                                 :preferred pref-range
                                 :gap gap
                                 :in-range? (zero? gap)}]))
        g-pragmatic (reduce-kv (fn [acc ch weight]
                                 (+ acc (* weight (get-in per-channel [ch :gap] 0.0))))
                               0.0
                               pref/pragmatic-weights)
        ;; Epistemic: uncertainty from dark areas
        g-epistemic (+ (* 0.4 (- 1.0 (:loop-health obs 0.0)))
                       (* 0.3 (- 1.0 (:attack-coverage obs 0.0)))
                       (* 0.3 (- 1.0 (:support-coverage obs 0.0))))
        ;; Total
        g-total (+ (* 0.65 g-pragmatic) (* 0.35 g-epistemic))
        ;; Avoided states currently active
        avoided (vec (for [[k v] pref/avoided-states
                           :when (not= k :strategic-mode)
                           :when (vector? v)
                           :when (in-avoided? (get obs k 0.0) v)]
                       k))]
    {:G-total g-total
     :G-pragmatic g-pragmatic
     :G-epistemic g-epistemic
     :per-channel per-channel
     :avoided-active avoided}))

;; ---------------------------------------------------------------------------
;; v0.10: R3a prediction-error computation against a likelihood model.
;; ---------------------------------------------------------------------------

(defn compute-prediction-error
  "R3a + R3b: prediction error (and precision-weighted error) for one channel
   given the observed value and a likelihood-model output `{:mean :variance}`.

   Returns:
     {:observed         <number>
      :predicted-mean   <number>
      :predicted-variance <number>
      :error            <observed − predicted-mean>      ; R3a
      :weighted-error   <error * precision>              ; R3b (precision = 1 / max(variance, ε))
      :precision        <1 / max(variance, ε)>}

   The ε floor (`min-variance`) prevents division by zero when the
   likelihood reports certainty (variance ≈ 0). Default min-variance 0.01."
  ([observed prediction] (compute-prediction-error observed prediction {}))
  ([observed prediction {:keys [min-variance] :or {min-variance 0.01}}]
   (let [pm (double (:mean prediction 0.0))
         pv (double (:variance prediction 0.0))
         o (double (or observed 0.0))
         err (- o pm)
         precision (/ 1.0 (max pv min-variance))]
     {:observed o
      :predicted-mean pm
      :predicted-variance pv
      :error err
      :precision precision
      :weighted-error (* err precision)})))

(defn infer-mode
  "Infer strategic mode from observation vector.
   Returns keyword: :multiplied, :depositing, :foraging-trapped, :hermit, :stagnant, :dark."
  [obs]
  (let [stack (get obs :stack-pct 0.0)
        consulting (get obs :consulting-pct 0.0)
        portfolio (get obs :portfolio-pct 0.0)
        loop-h (get obs :loop-health 0.0)
        active (get obs :active-repo-ratio 0.0)
        ticks (get obs :ticks-firing-ratio 0.0)
        depositing (get obs :depositing-signal 0.0)]
    (cond
      ;; Dark: nothing happening
      (and (< active 0.2) (< loop-h 0.3))
      :dark

      ;; Depositing: consulting active (commit-based or frame-based)
      (or (> consulting 0.2) (> depositing 0.15))
      :depositing

      ;; Hermit: stack-dominated, no consulting AND no depositing signal
      (and (> stack 0.7) (< consulting 0.05) (< depositing 0.05))
      :hermit

      ;; Scanning: stack-dominated but daily scans active (transitional)
      (and (> stack 0.7) (> depositing 0.0))
      :scanning

      ;; Foraging-trapped: stuck on stack under math/portfolio pressure
      (and (> stack 0.5) (> ticks 0.5))
      :foraging-trapped

      ;; Stagnant: surfaces used but not improving
      (and (> active 0.3) (< loop-h 0.5))
      :stagnant

      ;; Multiplied: healthy balance
      :else
      :multiplied)))
