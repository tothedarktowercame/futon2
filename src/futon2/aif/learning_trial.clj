(ns futon2.aif.learning-trial
  "Prospective, record-only trials. No production parameter or learning ledger writes."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def prior-resource "wm/learning-trial-prior.edn")
(defn declared-prior [] (edn/read-string (slurp (io/resource prior-resource))))

(defn- shadow [prediction pattern theta]
  (let [precedence (mapv #(if (= (:id pattern) (:id %)) (assoc % :theta theta) %)
                         (get-in prediction [:action :precedence]))]
    (if (and (= :frozen (:status prediction)) (seq (:initial-belief prediction)))
      {:theta theta :rollout (model/rollout-evaluation (constantly precedence)
                                                      (:initial-belief prediction) (:horizon prediction))}
      {:status :held :reason :prediction-input-unavailable})))

(defn receipt
  "Retain endpoint evidence and hypothetical Beta means. Every trial remains
   held: no currently declared execution-clock contract licenses per-firing
   counts. seen-identities/previous-meanings are explicit replay inputs only;
   neither is a mutable learning store. No inferred state is called observed."
  [{:keys [comparison occurrence route source-record prior seen-identities previous-meanings]
    :or {seen-identities #{} previous-meanings {}}}]
  (let [prior (or prior (declared-prior))
        prediction (:prediction comparison)
        action (:action prediction)
        rows (into {} (map (juxt :token identity)) (:tokens comparison))
        valid-prior? (and (= :illustrative (:authority prior)) (= :record-only (:mode prior))
                          (every? #(and (integer? %) (pos? %)) [(:alpha prior) (:beta prior)]))
        trials
        (mapv
         (fn [[pattern token]]
           (let [row (get rows token)
                 observed (:observed row)
                 measurement (:measurement row)
                 meaning {:token token :locator (:declared-locator measurement)
                          :declaration-sha256 (:declaration-sha256 measurement)}
                 meaning-digest (evidence/value-digest meaning)
                 identity-data {:occurrence (select-keys occurrence [:action/id :action/value-sha256 :transition/id])
                                :pattern (:id pattern) :effect token :grain (:trial-grain prior)}
                 identity (evidence/value-digest identity-data)
                 declarations (filter #(= (first token) (get-in % [:snapshot :target]))
                                      (get-in source-record [:dispatch :declarations]))
                 placement (if (= 1 (count declarations))
                             (sources/observation-schedule (:snapshot (first declarations)))
                             {:status :held :reason :observation-placement-not-declared})
                 initial-prob (reduce-kv (fn [p s mass] (+ p (if (contains? s token) mass 0)))
                                        0 (or (:initial-belief prediction) {}))
                 reason (cond
                          (or (not= (first token) (:target action))
                              (and occurrence (not= (first token) (get-in occurrence [:action/value :target]))))
                          :unselected-target
                          (pos? initial-prob) :effect-already-present
                          (contains? seen-identities identity) :duplicate-replay
                          (and (contains? previous-meanings token)
                               (not= meaning-digest (get previous-meanings token))) :revised-meaning
                          (not (boolean? observed)) :observation-missing
                          (not valid-prior?) :illustrative-prior-invalid
                          :else (or (:reason placement) :execution-clock-contract-missing))
                 a (:alpha prior) b (:beta prior)
                 theta (when valid-prior? (/ a (+ a b)))
                 after (when (and valid-prior? (boolean? observed))
                         {:alpha (+ a (if observed 1 0)) :beta (+ b (if observed 0 1))})
                 next-theta (when after (/ (:alpha after) (+ (:alpha after) (:beta after))))]
             {:pattern (:id pattern) :effect token :trial-grain (:trial-grain prior)
              :status :held :reason reason :counted? false
              :occurrence occurrence :performed-step {:status :missing :reason :execution-clock-contract-missing}
              :before-observation {:status :missing :reason :before-observation-not-bound
                                   :retained-evidence (get-in source-record [:dispatch :before-evidence])}
              :initial-model-marginal initial-prob
              :after-observation (if (boolean? observed) observed {:status :unknown :reason :observation-missing})
              :measurement measurement :measurement-verification (:measurement-verification comparison)
              :placement placement :meaning meaning :meaning-sha256 meaning-digest
              :guard (:guard pattern) :guard-sha256 (evidence/value-digest (:guard pattern))
              :effect-sha256 (evidence/value-digest (:produces pattern))
              :interpretation-sha256 (evidence/value-digest (get-in action [:interpretation-receipts (:id pattern)]))
              :route route :route-sha256 (evidence/value-digest route)
              :deduplication {:identity identity :inputs identity-data}
              :shadow (if (and next-theta (#{:observation-placement-not-declared :execution-clock-contract-missing} reason))
                        {:status :hypothetical :counted? false :prior (select-keys prior [:alpha :beta])
                         :if-counted after :prior-rollout (shadow prediction pattern theta)
                         :if-counted-rollout (shadow prediction pattern next-theta)}
                        {:status :held :reason reason})}))
         (for [pattern (:precedence action) token (sort-by pr-str (:produces pattern))] [pattern token]))]
    {:schema :wm/learning-trial-receipt-v1 :mode :record-only
     :prior prior :prior-sha256 (evidence/value-digest prior)
     :status :held :reason (if (seq trials) :execution-clock-contract-missing :prediction-input-unavailable)
     :production-parameters :unchanged :trials trials}))
