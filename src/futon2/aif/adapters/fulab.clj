(ns futon2.aif.adapters.fulab
  "Adapter stub for fulab (fucodex/fuclaude) AIF integration."
  (:require [clojure.string :as str]
            [futon2.aif.adapter :as adapter]))

(defn- text-score [value]
  (cond
    (string? value) (count (str/split value #"\s+"))
    (coll? value) (count value)
    :else 1))

(def default-config
  {:g/weights {:base 0.1
               :anchors 0.05
               :forecast 0.02}
   :tau/scale 1.0
   :tau/min 0.1
   :tau/max 2.0})

(defn- clamp [value min-val max-val]
  (-> value (max min-val) (min max-val)))

(defn- compute-g [candidate context config]
  (let [{:keys [base anchors forecast]} (:g/weights config)
        base-score (text-score candidate)
        anchor-score (text-score (:anchors context))
        forecast-score (text-score (:forecast context))]
    (+ (* base base-score)
       (* anchors anchor-score)
       (* forecast forecast-score))))

(defn- compute-tau [context config]
  (let [uncertainty (double (max 1 (text-score (:uncertainty context))))
        tau (double (/ (:tau/scale config) uncertainty))]
    (clamp tau (:tau/min config) (:tau/max config))))

(defrecord FulabAdapter [config]
  adapter/AifAdapter
  (select-pattern [_ _state context]
    (let [candidates (vec (:candidates context))
          scored (into {}
                       (for [c candidates]
                         [c (compute-g c context config)]))
          chosen (or (:chosen context)
                     (when (seq candidates)
                       (first (sort-by scored candidates))))
          tau (compute-tau context config)]
      (let [result {:decision/id (:decision/id context)
                    :candidates candidates
                    :chosen chosen
                    :aif {:G-chosen (get scored chosen)
                          :G-rejected (apply dissoc scored [chosen])
                          :tau tau
                          :belief-id (or (:belief-id context) (:decision/id context))}}]
        (tap> (merge {:type :aif/fulab
                      :event :select
                      :session/id (:session/id context)}
                     result))
        result)))
  (update-beliefs [_ _state observation]
    (let [error (double (max 0.0 (dec (text-score (:outcome observation)))))
          tau (compute-tau observation config)]
      (let [result {:aif/state {:belief-updated true}
                    :aif {:prediction-error error
                          :tau-updated tau
                          :belief-delta {:decision/id (:decision/id observation)
                                         :status (or (:status observation) :unknown)}}}]
        (tap> (merge {:type :aif/fulab
                      :event :update
                      :session/id (:session/id observation)}
                     result))
        result))))

(defn new-adapter
  ([] (->FulabAdapter default-config))
  ([config] (->FulabAdapter (merge default-config config))))
