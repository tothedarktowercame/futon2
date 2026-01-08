(ns futon2.aif.adapters.fulab
  "Adapter stub for fulab (fucodex/fuclaude) AIF integration."
  (:require [clojure.string :as str]
            [futon2.aif.adapter :as adapter]))

(defn- text-score [value]
  (cond
    (string? value) (count (str/split value #"\s+"))
    (coll? value) (count value)
    :else 1))

(defn- compute-g [candidate context]
  (let [base (text-score candidate)
        anchors (text-score (:anchors context))
        forecast (text-score (:forecast context))]
    (+ 0.1 base (* 0.05 anchors) (* 0.02 forecast))))

(defn- compute-tau [context]
  (let [uncertainty (double (max 1 (text-score (:uncertainty context))))]
    (double (/ 1.0 uncertainty))))

(defrecord FulabAdapter []
  adapter/AifAdapter
  (select-pattern [_ _state context]
    (let [candidates (vec (:candidates context))
          scored (into {}
                       (for [c candidates]
                         [c (compute-g c context)]))
          chosen (or (:chosen context)
                     (when (seq candidates)
                       (first (sort-by scored candidates))))
          tau (compute-tau context)]
      {:decision/id (:decision/id context)
       :candidates candidates
       :chosen chosen
       :aif {:G-chosen (get scored chosen)
             :G-rejected (apply dissoc scored [chosen])
             :tau tau
             :belief-id (or (:belief-id context) (:decision/id context))}}))
  (update-beliefs [_ _state observation]
    (let [error (double (max 0.0 (dec (text-score (:outcome observation)))))
          tau (compute-tau observation)]
      {:aif/state {:belief-updated true}
       :aif {:prediction-error error
             :tau-updated tau
             :belief-delta {:decision/id (:decision/id observation)
                            :status (or (:status observation) :unknown)}}})))

(defn new-adapter []
  (->FulabAdapter))
