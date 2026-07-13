(ns futon2.aif.substrate-fixture)

(defn- doc-id [doc]
  (or (:xt/id doc) (:entity/id doc) (:hx/id doc)))

(defn make-opts
  ([] (make-opts []))
  ([initial-docs]
   (let [store (atom (into {} (map (juxt doc-id identity)) initial-docs))
         entities (fn [type]
                    (->> (vals @store)
                         (filter #(= type (:entity/type %)))
                         vec))
         hyperedges (fn [type]
                      (->> (vals @store)
                           (filter #(= type (:hx/type %)))
                           vec))
         inhabitation
         (fn [bindings]
           (mapv (fn [{:keys [kind type endpoint-types] :as binding}]
                   (let [inhabited?
                         (case kind
                           :entity (boolean (seq (entities type)))
                           :hyperedge
                           (let [required (mapv #(set (map doc-id (entities %)))
                                                endpoint-types)]
                             (boolean
                              (some (fn [hx]
                                      (let [ends (set (:hx/endpoints hx))]
                                        (every? #(some ends %) required)))
                                    (hyperedges type))))
                           false)]
                     (assoc binding :inhabited? inhabited?)))
                 bindings))]
     {:store store
      :inhabitation-fn inhabitation
      :entities-by-type-fn entities
      :hyperedges-by-type-fn hyperedges
      :relations-fn (fn [{:keys [type]}]
                      (->> (vals @store)
                           (filter #(= type (:relation/type %)))
                           vec))
      :put-doc-fn (fn [doc]
                    (swap! store assoc (doc-id doc) doc)
                    {:ok true :id (doc-id doc)})})))
