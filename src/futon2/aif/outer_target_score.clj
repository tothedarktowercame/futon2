(ns futon2.aif.outer-target-score
  "Transparent outer-target score derived from a pinned target posterior and
  an explicit preference-cost authority."
  (:require [futon2.aif.outer-target-belief :as belief]))

(def schema :wm/outer-target-score-v1)

(defn- entropy [probabilities]
  (- (reduce + 0.0
             (map (fn [p] (if (pos? p) (* p (Math/log p)) 0.0))
                  probabilities))))

(defn- finite-number? [x]
  (and (number? x) (Double/isFinite (double x))))

(defn- preference-distribution [costs transform]
  (when (and (= :softmax-negative-cost (:kind transform))
             (finite-number? (:inverse-temperature transform))
             (pos? (double (:inverse-temperature transform))))
    (let [beta (double (:inverse-temperature transform))
          weights (into {} (map (fn [[outcome cost]]
                                  [outcome (Math/exp (- (* beta (double cost))))]))
                        costs)
          z (reduce + (vals weights))]
      (when (and (finite-number? z) (pos? z))
        (into (sorted-map) (map (fn [[outcome weight]]
                                  [outcome (/ weight z)])) weights)))))

(defn- categorical-kl [q c]
  (reduce + 0.0
          (map (fn [[outcome p]]
                 (if (zero? p) 0.0
                     (* p (Math/log (/ p (get c outcome))))))
               q)))

(defn- pinned-continuation? [continuation]
  (and (= :present (:status continuation))
       (string? (:next-step continuation))
       (string? (:acceptance-condition continuation))
       (string? (get-in continuation [:evidence :path]))
       (boolean (re-matches #"[0-9a-f]{64}"
                            (or (get-in continuation [:evidence :sha256]) "")))))

(defn score
  [{:keys [belief target preference-costs preference-transform
           preference-authority controller]}]
  (let [cost-keys (set (keys preference-costs))
        vocabulary (set belief/outcome-vocabulary)
        preference-distribution (preference-distribution preference-costs
                                                         preference-transform)]
    (cond
      (not= :produced (:status belief))
      {:schema schema :status :refused :reason :belief-not-produced}

      (not= vocabulary cost-keys)
      {:schema schema :status :refused :reason :preference-vocabulary-mismatch}

      (or (nil? preference-authority)
          (not-every? #(and (number? %) (<= 0.0 (double %) 1.0))
                      (vals preference-costs)))
      {:schema schema :status :refused :reason :preference-authority-invalid}

      (or (nil? preference-distribution)
          (not= vocabulary (set (keys preference-distribution)))
          (not-every? #(and (finite-number? %) (pos? (double %)))
                      (vals preference-distribution)))
      {:schema schema :status :refused :reason :preference-transform-invalid}

      :else
      (let [alpha (or (get-in belief [:targets target :alpha])
                      (get-in belief [:prior :alpha]))
            total (reduce + (vals alpha))
            prediction (into (sorted-map)
                             (map (fn [outcome]
                                    [outcome (/ (get alpha outcome) total)]))
                             belief/outcome-vocabulary)
            expected-authorized-cost
            (reduce + (map (fn [[outcome p]]
                             (* p (get preference-costs outcome)))
                           prediction))
            risk (categorical-kl prediction preference-distribution)
            ambiguity (/ (entropy (vals prediction))
                         (Math/log (double (count prediction))))
            ;; No likelihood-parameter posterior beyond the categorical
            ;; Dirichlet is claimed here. Keep epistemic value explicit zero.
            eig 0.0
            observations (get-in belief [:targets target :observations] 0)
            continuation (get-in controller [:continuations target])
            continuation? (pinned-continuation? continuation)
            exempt? (contains? (set (:exempt-targets controller)) target)
            cadence-cost (if (and (pos? observations)
                                  (not continuation?) (not exempt?))
                           (double (or (:recent-selection-cost controller) 0.0))
                           0.0)
            continuation-credit (if continuation?
                                  (double (or (:continuation-credit controller) 0.0))
                                  0.0)
            resource-history (get-in belief [:targets target :resource-use])
            resource-fractions
            (cond-> []
              (pos? (get resource-history :elapsed-observations 0))
              (conj (min 1.0 (/ (double (:wall-clock-ms resource-history))
                                (* (double (get controller :wall-clock-budget-ms 1))
                                   (:elapsed-observations resource-history)))))
              (pos? (get resource-history :token-observations 0))
              (conj (min 1.0 (/ (double (:tokens resource-history))
                                (* (double (get controller :token-budget 1))
                                   (:token-observations resource-history))))))
            resource-cost (if (seq resource-fractions)
                            (/ (reduce + resource-fractions)
                               (count resource-fractions))
                            (double (or (:unobserved-resource-cost controller) 0.5)))
            weighted-resource-cost (* resource-cost
                                      (double (or (:resource-weight controller) 0.0)))
            canonical-g (+ risk ambiguity (- eig))
            ;; This is the former injury-comparison value.  It remains visible
            ;; only to make the unresolved conversion auditable: it is neither
            ;; canonical G nor an authorised policy prior.
            legacy-clamped-comparison
            (-> (/ canonical-g 2.0)
                (+ weighted-resource-cost)
                (+ cadence-cost)
                (- continuation-credit)
                (max 0.0) (min 1.0))]
        {:schema schema :status :scored :target target
         :prediction prediction
         :preference-distribution preference-distribution
         :preference-transform preference-transform
         :terms {:risk risk
                 :risk-kind :categorical-kl-q-predicted-outcomes-to-c
                 :ambiguity ambiguity
                 :ambiguity-kind :categorical-likelihood-entropy
                 :model-assumption :observed-target-one-step-outcome-likelihood
                 :expected-authorized-cost expected-authorized-cost
                 :expected-information-gain eig
                 :combination :risk-plus-ambiguity-minus-information-gain
                 :canonical-g canonical-g
                 :normalization :divide-by-two
                 :r8-present-fit
                 (or (get-in belief [:targets target :model-fit :last])
                     {:schema :wm/outer-present-fit-v1 :status :absent
                      :reason :target-unobserved})
                 :r8-consumer :outer-target-score-diagnostic
                 :recent-selection-opportunity-cost cadence-cost
                 :continuation-credit continuation-credit
                 :continuation-evidence (if continuation?
                                          continuation
                                          {:status :absent
                                           :reason :no-pinned-continuation})
                 :controller-exemption (when exempt? :declared)
                 :resource-history (or resource-history
                                       {:status :absent :reason :target-unobserved})
                 :resource-cost resource-cost
                 :weighted-resource-cost weighted-resource-cost
                 :engineering-influence
                 {:resource-cost weighted-resource-cost
                  :cadence-cost cadence-cost
                  :continuation-credit continuation-credit
                  :status :declared-not-converted-to-policy-prior}
                 :legacy-comparison
                 {:status :unresolved
                  :reason :policy-prior-conversion-not-declared
                  :value legacy-clamped-comparison
                  :former-combination
                  :clamped-canonical-g-plus-engineering}}
         ;; R5's canonical quantity; callers must not read a legacy bounded
         ;; comparison receipt as a replacement G.
         :g canonical-g
         :belief-source-pin (:source-pin belief)
         :preference-authority preference-authority}))))
