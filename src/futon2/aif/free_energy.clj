(ns futon2.aif.free-energy
  "Controller diagnostics and variational free-energy computation for the War Machine.

   The inference step between observation and render. Reads observation
   vectors from `futon2.aif.observation/observe` and preferences from
   `futon2.aif.preferences`, producing explicitly named engineering diagnostics.
   The distinct `compute-variational-free-energy` function reports the Gaussian
   prediction-error objective.

   cf. cyberants `ants/aif/policy.clj` — EFE computation
   cf. portfolio/policy.clj — action ranking
   cf. M-aif-head: the war machine integrates all heads, not replaces them

   Invariants:
   - WM-I1 (read-only — judge produces data, never writes)
   - WM-I4 (sovereignty — priorities are informational, not commands)"
  (:require [futon2.aif.observation :as observation]
            [futon2.aif.preferences :as pref]))

(defn- channel-gap
  "Distance of observation from preferred range.
   Returns 0.0 if within [lo, hi], positive distance otherwise."
  [obs-val [lo hi]]
  (let [v (double obs-val)]
    (cond (< v lo) (- lo v)
          (> v hi) (- v hi)
          :else 0.0)))

(defn- channel-reading
  "Return a reason-bearing reading for CHANNEL. Plain explicit maps are a
   supported legacy boundary; metadata-bearing observations retain absence."
  [obs channel]
  (let [source-status (observation/observation-status obs channel)
        explicit-legacy-value? (and (= :status-metadata-missing
                                       (:reason source-status))
                                    (contains? obs channel)
                                    (some? (get obs channel)))]
    (if (or (= :observed (:variant source-status)) explicit-legacy-value?)
      {:status :present :value (double (get obs channel))}
      (merge {:status :absent}
             (select-keys source-status [:reason :paths])))))

(defn- avoidance-verdict
  "Tri-state avoided-range diagnostic. An absent source is unknown; it is
   never converted to the numeric observation zero. Plain explicit maps remain
   supported for library callers that predate observation metadata."
  [obs channel [lo hi :as avoided-range]]
  (let [source-status (observation/observation-status obs channel)
        explicit-legacy-value? (and (= :status-metadata-missing
                                       (:reason source-status))
                                    (contains? obs channel)
                                    (some? (get obs channel)))]
    (if (or (= :observed (:variant source-status)) explicit-legacy-value?)
      (let [v (double (get obs channel))]
        {:status (if (and (>= v lo) (<= v hi)) :violated :satisfied)
         :observation {:variant :observed :value v}
         :avoided-range avoided-range})
      {:status :unknown
       :observation (select-keys source-status [:variant :reason :paths])
       :avoided-range avoided-range})))

(defn compute-controller-diagnostics
  "Compute legacy controller diagnostics from an observation vector.

   Returns {:controller-score :preference-gap-score :coverage-uncertainty-pressure
            :per-channel-gaps :avoidance-by-channel :avoided-active}.

   preference-gap-score: weighted distance from preferences (dominated by workstream balance).
   coverage-uncertainty-pressure: uncertainty from dark arrows and unaddressed claims.
   controller-score: 0.65 * preference gap + 0.35 * coverage uncertainty.

   cf. war-machine-terminal-vocabulary.edn :G/pragmatic-fn, :G/epistemic-fn"
  ([obs] (compute-controller-diagnostics obs {:support-aware? true}))
  ([obs {:keys [support-aware?] :or {support-aware? true}}]
  (let [;; Pragmatic: gap between observations and preferences. Read through the
        ;; current-C seam (E-C-vector-live §4.5) so the channel preferences can
        ;; go live without touching this consumer; today identical to the static map.
        per-channel (into {}
                          (for [[ch pref-range] (pref/current-C)
                                :let [reading (if support-aware?
                                                (channel-reading obs ch)
                                                {:status :present
                                                 :value (double (get obs ch 0.0))})]]
                            [ch (if (= :present (:status reading))
                                  (let [v (:value reading)
                                        gap (channel-gap v pref-range)]
                                    {:status :present
                                     :value v
                                     :preferred pref-range
                                     :gap gap
                                     :in-range? (zero? gap)})
                                  (assoc reading :preferred pref-range))]))
        g-pragmatic (reduce-kv (fn [acc ch weight]
                                 (if-let [gap (get-in per-channel [ch :gap])]
                                   (+ acc (* weight gap))
                                   acc))
                               0.0
                               pref/pragmatic-weights)
        ;; Epistemic: uncertainty from dark areas
        epistemic-weights {:loop-health 0.4
                           :attack-coverage 0.3
                           :support-coverage 0.3}
        epistemic-terms
        (into {}
              (for [[ch weight] epistemic-weights
                    :let [reading (if support-aware?
                                    (channel-reading obs ch)
                                    {:status :present
                                     :value (double (get obs ch 0.0))})]]
                [ch (if (= :present (:status reading))
                      (assoc reading :weighted-term
                             (* weight (- 1.0 (:value reading))))
                      reading)]))
        g-epistemic (reduce + 0.0 (keep :weighted-term (vals epistemic-terms)))
        support {:pragmatic
                 {:present (->> per-channel (keep (fn [[ch x]]
                                                    (when (= :present (:status x)) ch))) set)
                  :absent (into {} (keep (fn [[ch x]]
                                           (when (= :absent (:status x))
                                             [ch (select-keys x [:reason :paths])]))
                                         per-channel))}
                 :epistemic
                 {:present (->> epistemic-terms (keep (fn [[ch x]]
                                                        (when (= :present (:status x)) ch))) set)
                  :absent (into {} (keep (fn [[ch x]]
                                           (when (= :absent (:status x))
                                             [ch (select-keys x [:reason :paths])]))
                                         epistemic-terms))}}
        ;; Total
        g-total (+ (* 0.65 g-pragmatic) (* 0.35 g-epistemic))
        avoidance-by-channel
        (into {}
              (for [[k v] pref/avoided-states
                    :when (not= k :strategic-mode)
                    :when (vector? v)]
                [k (avoidance-verdict obs k v)]))
        ;; Compatibility view: only measured violations are active. Unknown is
        ;; retained in :avoidance-by-channel and never silently counted either way.
        avoided (vec (for [[k verdict] avoidance-by-channel
                           :when (= :violated (:status verdict))]
                       k))]
    {:controller-score g-total
     :preference-gap-score g-pragmatic
     :coverage-uncertainty-pressure g-epistemic
     :score-support support
     :epistemic-terms epistemic-terms
     :per-channel per-channel
     :avoidance-by-channel avoidance-by-channel
     :avoided-active avoided})))

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
      :producer-contract :prediction-error/v1
      :precision precision
      :weighted-error (* err precision)})))

(defn compute-variational-free-energy
  "Compute the per-tick Gaussian prediction-error diagnostic

     F = 1/2 mean_k(precision_k * error_k^2).

   `prediction-errors` must be a non-empty map whose values carry finite,
   numeric `:error` and non-negative `:precision`. This is deliberately a
   distinct quantity from the strategic controller's historical
   `compute-controller-diagnostics` map and from cascade model-selection scores."
  [prediction-errors]
  (when-not (seq prediction-errors)
    (throw (ex-info "variational free energy requires prediction errors" {})))
  (let [terms
        (mapv (fn [[channel {:keys [error precision]}]]
                (when-not (and (number? error) (Double/isFinite (double error))
                               (number? precision) (Double/isFinite (double precision))
                               (not (neg? (double precision))))
                  (throw (ex-info "invalid prediction-error term for variational F"
                                  {:channel channel :error error
                                   :precision precision})))
                (* (double precision) (double error) (double error)))
              prediction-errors)]
    (* 0.5 (/ (reduce + terms) (double (count terms))))))

(defn infer-mode
  "Infer strategic mode from observation vector.
   Returns keyword: :multiplied, :depositing, :foraging-trapped, :hermit, :stagnant, :dark."
  [obs]
  (let [stack (get obs :stack-pct 0.0)
        consulting (get obs :consulting-pct 0.0)
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
