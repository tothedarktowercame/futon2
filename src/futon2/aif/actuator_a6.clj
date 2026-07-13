(ns futon2.aif.actuator-a6
  "A6: substrate discharge -> capability-star status -> status-aware re-ranking.

   A6 owns only :capability-star-status. A4a owns :capability-star identity."
  (:require [clojure.string :as str]
            [futon2.aif.a4a-substrate :as a4a-substrate]
            [futon2.aif.actuator-a3 :as a3]
            [futon2.aif.efe :as efe]
            [futon2.aif.operational-witness :as ow]
            [futon2.aif.substrate :as substrate])
  (:import (java.time Instant)
           (java.util Date)))

(def ^:dynamic *pattern-grain-model-uncertainty?*
  "Operator arm for the pattern-grain posterior-spread controller bonus.
   This is model uncertainty, not expected information gain. Explicit
   :pattern-grain-model-uncertainty? opts override this var."
  true)

(defn status-id
  [star-id]
  (str "capability-star-status/"
       (str/replace (str star-id) #"[^A-Za-z0-9._:-]" "_")))

(defn status-doc
  ([star-id] (status-doc star-id {}))
  ([star-id {:keys [at] :or {at (Date/from (Instant/now))}}]
   {:xt/id (status-id star-id)
    :entity/type :capability-star-status
    :status/star star-id
    :status/status :satisfied
    :status/updated-by :a6-discharge
    :status/at at}))

(defn discharge-capability-rows
  [opts]
  (let [rows (for [raw (substrate/entities-by-type :discharge opts)
                   :let [doc (merge raw (:entity/props raw))]
                   :when (= :capability (:discharge/type doc))]
               [(or (:entity/id doc) (:xt/id doc) (:id doc))
                (:discharge/endpoint doc)])
        allowed (:capability-filter opts)]
    (->> rows
         (map (fn [[d cap]] {:discharge d :capability cap}))
         (filter (fn [{:keys [capability]}]
                   (or (nil? allowed) (contains? allowed capability))))
         vec)))

(defn stars-for-capability
  [capability opts]
  (->> (substrate/entities-by-type :capability-star opts)
       (map #(merge % (:entity/props %)))
       (filter #(= capability (:star/capability %)))
       (map #(or (:entity/id %) (:xt/id %) (:id %)))
       vec))

(defn write-status!
  [star-id opts]
  (let [doc (status-doc star-id opts)]
    {:doc doc
     :tx (a3/drawbridge-submit-tx! [[:xtdb.api/put doc]] opts)}))

(defn flip-star-status-on-discharge!
  "Read capability discharges, resolve each to A4a's capability-star, and write
   the locked A6 :capability-star-status entity. No star means no-op."
  ([] (flip-star-status-on-discharge! {}))
  ([opts]
   (reduce
    (fn [acc {:keys [discharge capability]}]
      (let [stars (stars-for-capability capability opts)]
        (if (seq stars)
          (reduce (fn [acc' star]
                    (update acc' :written conj
                            (assoc (write-status! star opts)
                                   :discharge discharge
                                   :capability capability
                                   :star star)))
                  acc
                  stars)
          (update acc :no-star conj {:discharge discharge
                                     :capability capability
                                     :reason :no-capability-star}))))
    {:written [] :no-star []}
    (discharge-capability-rows opts))))

(defn status-rows
  [opts]
  (let [allowed (:capability-filter opts)
        stars (into {}
                    (map (fn [raw]
                           (let [doc (merge raw (:entity/props raw))]
                             [(or (:entity/id doc) (:xt/id doc) (:id doc))
                              (:star/capability doc)])))
                    (substrate/entities-by-type :capability-star opts))]
    (->> (substrate/entities-by-type :capability-star-status opts)
         (map #(merge % (:entity/props %)))
         (filter #(= :satisfied (:status/status %)))
         (keep (fn [doc]
                 (let [status (or (:entity/id doc) (:xt/id doc) (:id doc))
                       star (:status/star doc)
                       cap (get stars star)]
                   (when cap {:status status :star star :capability cap}))))
         (filter (fn [{:keys [capability]}]
                   (or (nil? allowed) (contains? allowed capability))))
         vec)))

(defn apply-star-status
  "Bridge substrate status into the in-memory capability graph. Touches only
   [:capabilities cap-id :status]."
  ([capability-graph] (apply-star-status capability-graph {}))
  ([capability-graph opts]
   (reduce (fn [graph {:keys [capability]}]
             (assoc-in graph [:capabilities capability :status] :satisfied))
           capability-graph
           (status-rows opts))))

(defn- pattern-grain-model-uncertainty-enabled?
  [opts]
  (if (contains? (or opts {}) :pattern-grain-model-uncertainty?)
    (:pattern-grain-model-uncertainty? opts)
    *pattern-grain-model-uncertainty?*))

(defn- with-pattern-grain-model-uncertainty
  [rank-opts opts]
  (if (and (pattern-grain-model-uncertainty-enabled? opts)
           (not (:model-uncertainty-fn rank-opts)))
    (assoc rank-opts :model-uncertainty-fn (:model-uncertainty-fn (a4a-substrate/load-pattern-grain-model-uncertainty opts)))
    rank-opts))

(defn rank-with-star-status
  [state candidate-actions capability-graph opts]
  (efe/rank-star-map-actions
   state
   candidate-actions
   (with-pattern-grain-model-uncertainty
     (merge {:capability-graph (apply-star-status capability-graph opts)
             :graph-ascent-status-aware? true}
            opts)
     opts)))

(defn- open-mission-actions
  [graph]
  (mapv (fn [mission-id] {:type :open-mission :target mission-id})
        (sort (keys (:missions graph)))))

(def default-rank-opts
  {:pre-registered-goal :goal
   :graph-ascent-status-aware? true
   :graph-applicability-penalty 0.0
   :graph-body-weight 3.0
   :graph-ascent-weight 20.0})

(defn rank-graph
  ([graph] (rank-graph graph {}))
  ([graph opts]
   (let [rank-opts (merge default-rank-opts opts)]
     (efe/rank-star-map-actions
      (or (:state opts) {:observation {} :belief {}})
      (or (:candidate-actions opts) (open-mission-actions graph))
      (with-pattern-grain-model-uncertainty
        (assoc rank-opts :capability-graph graph)
        opts)))))

(defn closure-falsifier
  "E-pur-si-muove gate: a discharge must move the status-aware ranking."
  ([capability-graph] (closure-falsifier capability-graph {}))
  ([capability-graph opts]
   (let [before (rank-graph capability-graph opts)
         flip (flip-star-status-on-discharge! opts)
         after-graph (apply-star-status capability-graph opts)
         after (rank-graph after-graph opts)
         before-order (mapv #(get-in % [:action :target]) before)
         after-order (mapv #(get-in % [:action :target]) after)]
     {:before before
      :after after
      :before-order before-order
      :after-order after-order
      :moved? (not= before-order after-order)
      :flip flip
      :graph after-graph})))

(defn- ranked-targets
  [ranked]
  (mapv #(get-in % [:action :target]) ranked))

(defn- produces-facts
  [capability-graph]
  (->> (:missions capability-graph)
       (mapcat (fn [[mission {:keys [produces]}]]
                 (map (fn [cap] [mission cap]) produces)))
       (sort-by (juxt first second))
       vec))

(defn- discharge-facts
  [opts]
  (->> (discharge-capability-rows opts)
       (mapv (juxt :discharge :capability))
       (sort-by first)
       vec))

(defn witness-live
  "Read-only operational witness driver. Gathers the live substrate discharge
   facts and current A6 status rows, then runs the pure operational witness
   register over the observed before/discharges/after transition."
  ([capability-graph] (witness-live capability-graph {}))
  ([capability-graph opts]
   (let [before-order (ranked-targets (rank-graph capability-graph opts))
         after-graph (apply-star-status capability-graph opts)
         after-order (ranked-targets (rank-graph after-graph opts))
         observation {:discharges (discharge-facts opts)
                      :produces (produces-facts capability-graph)
                      :before-order before-order
                      :after-order after-order}]
     {:observation observation
      :witness (ow/run-register observation)})))
