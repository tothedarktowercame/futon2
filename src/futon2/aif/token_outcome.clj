(ns futon2.aif.token-outcome
  "Selection-time token prediction and post-build comparison. No belief update."
  (:require [futon2.aif.load-identity :as load-identity]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.interpretation-evidence :as evidence]))

(load-identity/register! *ns* *file*)

(defn freeze-prediction
  "Freeze the scorer's terminal rollout for ALL wants of the selected target.
   Verdicts use positive marginal support, not a hidden probability threshold."
  [decision]
  (let [action (:action decision)
        target (:target action)
        certificate (:selection-certificate decision)
        family (:precision-family certificate)
        {:keys [q0 horizon]} (:model family)
        domains (filter #(= target (:target %))
                        (get-in certificate [:token-belief-stage :domain-inputs]))
        want (get-in (first domains) [:declaration :want])
        base {:schema :wm/token-outcome-prediction-v1 :target target
              :action action :model-id (:model-id family)
              :prediction-rule :positive-marginal-support}]
    (if-not (and (= :cascade-candidate (:kind action))
                 (= 1 (count domains)) (seq want)
                 (map? q0) (seq q0) (model/normalized-exact? q0)
                 (every? set? (keys q0))
                 (integer? horizon) (<= 0 horizon)
                 (vector? (:precedence action)))
      (assoc base :status :refused :reason :prediction-input-unavailable)
      (let [result (model/rollout-evaluation (constantly (:precedence action)) q0 horizon)
            belief (:belief result)]
        (if (contains? belief :status)
          (assoc base :status :refused :reason :model-rollout-refused :finding belief)
          (assoc base :status :frozen :initial-belief q0 :horizon horizon
                 :model (:model family) :rollout result
                 :observation-locators (:observation-locators action)
                 :intended-outputs (into #{} (mapcat :produces) (:precedence action))
                 :wanted (mapv (fn [token]
                                 (let [qualified [target token]]
                                   {:token qualified
                                    :predicted (reduce-kv
                                                (fn [p state mass]
                                                  (+ p (if (contains? state qualified) mass 0)))
                                                0 belief)}))
                               (sort-by pr-str want))))))))

(defn compare-outcomes
  "Compare raw measurements, never the positive-only D-task carry projection.
   Missing, duplicate, or wrong-artifact evidence remains typed missing."
  [prediction measurements artifact-sha]
  (let [base {:schema :wm/token-outcome-comparison-v1
              :prediction prediction :artifact-sha artifact-sha
              :measurements (vec measurements)
              :evidence-sha256 (evidence/value-digest (vec measurements))}]
    (if-not (= :frozen (:status prediction))
      (assoc base :status :refused :reason :prediction-unavailable)
      (assoc base :status :compared
             :tokens
             (mapv
              (fn [{:keys [token predicted]}]
                (let [rows (filter #(= token (:token %)) measurements)
                      row (first rows)
                      result (:result row)
                      observed (:observed result)
                      missing (cond
                                (empty? rows) :measurement-unavailable
                                (not= 1 (count rows)) :ambiguous-measurement
                                (not (boolean? observed)) (or (:kind result) :measurement-unavailable)
                                (or (nil? artifact-sha)
                                    (not= artifact-sha (get-in result [:evidence :resolved-sha])))
                                :artifact-revision-mismatch)
                      verdict (cond missing :observation-missing
                                    (pos? predicted) (if observed :predicted-and-observed
                                                        :predicted-not-observed)
                                    observed :not-predicted-observed
                                    :else :neither)]
                  (cond-> {:token token :predicted predicted
                           :observed (if missing {:status :missing :kind missing} observed)
                           :verdict verdict :measurement row}
                    missing (assoc :reason missing))))
              (:wanted prediction))))))
