(ns futon2.aif.durable-hydrate
  "Reading side of futon2.aif.durable-intern, kept free of deftype so that
  babashka scripts can hydrate interned records too.

  An interned value is a map carrying {:durable/interned {id subtree}}, with
  each repeated subtree replaced by {:durable/ref id}. hydrate returns the
  value as written, sharing each interned subtree instead of copying it."
  (:import [java.util HashMap HashSet]))

(def table-key :durable/interned)
(def ref-key :durable/ref)

(defn interned? [x]
  (and (map? x) (contains? x table-key)))

(defn- ref-id [v]
  (when (and (map? v) (= 1 (count v)))
    (get v ref-key)))

(defn hydrate
  "Inverse of durable-intern/encode. Returns X unchanged when it carries no
  table. Refuses a reference with no table entry, and a reference cycle."
  [x]
  (if-not (interned? x)
    x
    (let [table (get x table-key)
          done (HashMap.)
          resolving (HashSet.)]
      (letfn [(resolve-id [id]
                (or (.get done id)
                    (let [entry (get table id ::missing)]
                      (when (identical? entry ::missing)
                        (throw (ex-info "Durable reference has no table entry"
                                        {:durable/ref id})))
                      (when-not (.add resolving id)
                        (throw (ex-info "Durable references contain a cycle"
                                        {:durable/ref id})))
                      (try
                        (let [v (walk entry)]
                          (.put done id v)
                          v)
                        (finally (.remove resolving id))))))
              (walk [v]
                (if-let [id (ref-id v)]
                  (resolve-id id)
                  (cond
                    (map? v) (persistent!
                              (reduce-kv (fn [acc k y] (assoc! acc k (walk y)))
                                         (transient (empty v)) v))
                    (vector? v) (mapv walk v)
                    (set? v) (into (empty v) (map walk) v)
                    (seq? v) (doall (map walk v))
                    :else v)))]
        (walk (dissoc x table-key))))))
