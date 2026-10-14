(ns futon2.aif.outer-target-score
  "Transparent outer-target score derived from a pinned target posterior and
  an explicit preference-cost authority."
  (:require [futon2.aif.outer-target-belief :as belief]))

(def schema :wm/outer-target-score-v1)

(defn- entropy [probabilities]
  (- (reduce + 0.0
             (map (fn [p] (if (pos? p) (* p (Math/log p)) 0.0))
                  probabilities))))

(defn score
  [{:keys [belief target preference-costs preference-authority]}]
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
            normalized-g (/ (+ risk ambiguity (- eig)) 2.0)]
        {:schema schema :status :scored :target target
         :prediction prediction
         :terms {:risk risk :ambiguity ambiguity
                 :expected-information-gain eig
                 :combination :risk-plus-ambiguity-minus-information-gain
                 :normalization :divide-by-two}
         :g normalized-g
         :belief-source-pin (:source-pin belief)
         :preference-authority preference-authority}))))
