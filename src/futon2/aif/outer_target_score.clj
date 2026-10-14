(ns futon2.aif.outer-target-score
  "Transparent outer-target score derived from a pinned target posterior and
  an explicit preference-cost authority."
  (:require [futon2.aif.outer-target-belief :as belief]))

(def schema :wm/outer-target-score-v1)

(defn- entropy [probabilities]
  (- (reduce + 0.0
             (map (fn [p] (if (pos? p) (* p (Math/log p)) 0.0))
                  probabilities))))

(defn- pinned-continuation? [continuation]
  (and (= :present (:status continuation))
       (string? (:next-step continuation))
       (string? (:acceptance-condition continuation))
       (string? (get-in continuation [:evidence :path]))
       (boolean (re-matches #"[0-9a-f]{64}"
                            (or (get-in continuation [:evidence :sha256]) "")))))

(defn score
  [{:keys [belief target preference-costs preference-authority controller]}]
  (let [cost-keys (set (keys preference-costs))
        vocabulary (set belief/outcome-vocabulary)]
    (cond
      (not= :produced (:status belief))
      {:schema schema :status :refused :reason :belief-not-produced}

      (not= vocabulary cost-keys)
      {:schema schema :status :refused :reason :preference-vocabulary-mismatch}

      (or (nil? preference-authority)
          (not-every? #(and (number? %) (<= 0.0 (double %) 1.0))
                      (vals preference-costs)))
      {:schema schema :status :refused :reason :preference-authority-invalid}

      :else
      (let [alpha (or (get-in belief [:targets target :alpha])
                      (get-in belief [:prior :alpha]))
            total (reduce + (vals alpha))
            prediction (into (sorted-map)
                             (map (fn [outcome]
                                    [outcome (/ (get alpha outcome) total)]))
                             belief/outcome-vocabulary)
            risk (reduce + (map (fn [[outcome p]]
                                  (* p (get preference-costs outcome)))
                                prediction))
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
            normalized-g (-> (/ (+ risk ambiguity (- eig)) 2.0)
                             (+ weighted-resource-cost)
                             (+ cadence-cost)
                             (- continuation-credit)
                             (max 0.0) (min 1.0))]
        {:schema schema :status :scored :target target
         :prediction prediction
         :terms {:risk risk :ambiguity ambiguity
                 :expected-information-gain eig
                 :combination :risk-plus-ambiguity-minus-information-gain
                 :normalization :divide-by-two
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
                 :weighted-resource-cost weighted-resource-cost}
         :g normalized-g
         :belief-source-pin (:source-pin belief)
         :preference-authority preference-authority}))))
