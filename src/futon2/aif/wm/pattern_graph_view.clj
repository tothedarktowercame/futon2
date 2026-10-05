(ns futon2.aif.wm.pattern-graph-view
  "Observational view of a selected cascade against the mined pattern graph."
  (:require [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.pattern-retraction :as retraction]))

(def schema :wm/pattern-graph-view-v1)

(defn- graph-id [x]
  (cond
    (keyword? x) (if-let [n (namespace x)] (str n "/" (name x)) (name x))
    (symbol? x) (str x)
    :else (str x)))

(defn view
  "Describe ordered chosen PATTERNS without changing any decision."
  [graph patterns]
  (let [ids (set (:pattern-ids graph))
        edges (:edges graph)
        neighbours (reduce (fn [m {:keys [a b]}]
                             (-> m (update a (fnil conj #{}) b)
                                 (update b (fnil conj #{}) a))) {} edges)
        positioned (mapv vector (range 1 (inc (count patterns))) patterns)
        rows (mapv (fn [[position pattern]]
                     {:pattern pattern :position position :in-graph? (contains? ids pattern)
                      :linked-patterns (count (get neighbours pattern #{}))}) positioned)
        pair-kinds (reduce (fn [m {:keys [a b kind]}]
                             (update m (vec (sort [a b])) (fnil conj #{}) kind)) {} edges)
        pairs (vec (for [left positioned right positioned
                         :when (< (first left) (first right))
                         :let [[i a] left [j b] right [x y] (sort [a b])]]
                     {:a x :b y :positions [i j]
                      :kinds (vec (sort (get pair-kinds [x y] #{})))}))
        unknown (vec (filter #(not (contains? ids %)) patterns))
        without-links (vec (filter #(and (contains? ids %)
                                         (empty? (get neighbours % #{}))) patterns))
        seeds (vec (sort (remove (set (concat unknown without-links)) patterns)))
        result (when (>= (count seeds) 2)
                 (retraction/retractions graph {:seeds seeds :k 3}))
        cascades (mapv (fn [{:keys [rank cost nodes edges]}]
                         {:rank rank :cost cost :nodes nodes
                          :added (vec (sort (remove (set seeds) nodes)))
                          :edges (mapv (fn [{:keys [a b kind-used]}]
                                         {:a a :b b :kind kind-used}) edges)})
                       (:retractions result))]
    {:schema schema :patterns rows :pairs pairs :unknown unknown
     :without-links without-links :seeds seeds
     :connecting-cascades cascades
     :failures (if (< (count seeds) 2)
                 [{:kind :fewer-than-two-linked-patterns :seeds seeds}]
                 (vec (:failures result)))}))

(defn for-action
  "Load GRAPH-PATH without a pin and view ACTION's ordered precedence. Never throws."
  [graph-path action]
  (try
    (let [patterns (mapv (comp graph-id :id) (:precedence action))]
      (if (empty? patterns)
        {:status :absent :reason :selected-action-without-precedence}
        (let [loaded (graph-pin/load-unpinned graph-path)]
          (if (= :loaded (:status loaded))
            (assoc (view (:graph loaded) patterns) :graph (:graph-ref loaded))
            {:status :absent :reason (:kind loaded) :graph {:path (str graph-path)}}))))
    (catch Exception e
      {:status :absent :reason :pattern-graph-view-failed :message (ex-message e)})))
