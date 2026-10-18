(ns futon2.aif.meta-algorithm-evidence
  "Pure evidence model for the META Algorithm arm.

  Ticket publication is an observation, never evidence that an algorithm ran.
  A successful receipt requires the same selected algorithm to be started,
  terminated, and independently verified."
  (:require [futon2.aif.meta-field-observation :as field]))

(def schema :wm/meta-algorithm-execution-v1)

(defn receipt
  [{:keys [selection algorithm-id started terminal verification
           repair-ticket-publication]}]
  (let [selected-id (when (= :run-algorithm (get-in selection [:action :type]))
                      (get-in selection [:action :target]))
        status (cond
                 (nil? selected-id) :not-selected
                 (not= selected-id algorithm-id) :identity-mismatch
                 (not= :started (:status started)) :selected-not-started
                 (not= :completed (:status terminal)) :started-not-completed
                 (not= :verified (:status verification)) :completed-not-verified
                 :else :verified)
        body {:schema schema
              :status status
              :algorithm-id algorithm-id
              :selected-id selected-id
              :selection-source (select-keys selection [:schema :action :chosen])
              :started started
              :terminal terminal
              :verification verification
              :repair-ticket-publication repair-ticket-publication}]
    (assoc body :evidence-digest (field/digest body))))

(defn verify [value authority]
  (let [expected (receipt authority)]
    (if (= expected value)
      {:schema schema :status :verified
       :algorithm-id (:algorithm-id value)
       :evidence-digest (:evidence-digest value)}
      {:schema schema :status :refused
       :reason :algorithm-execution-evidence-mismatch
       :expected expected :actual value})))
